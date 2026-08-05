(ns ehrt.sim.run
  "The `sim run` capability: config -> {simulation output + manifest},
  wrapped in the Result vocabulary. Never prints (only the CLI shell
  prints); never throws for expected failures.

  v0 returns the ground-truth log directly in the payload. Message
  emission (HL7v2 via the ER7 emitter over org.clojars.cmiles74/
  clojure-hl7-parser structures, ehrt.sim-emit-hl7.emit-hl7) attaches
  here as an output stage consuming the log -- the log is primary,
  messages are a rendering, and emission is opt-in (:emit \"hl7\").
  Milestone M1: engine/run echoes back the :facility and materialized
  :providers it actually allocated against (docs/operational-models.md);
  both check/check-all (the capacity/surge-ladder invariants) and
  emit-hl7/emit (PV1-3/6/7) are threaded that SAME config, not a fresh
  default that might not even share ward names.

  M2a (sim/ADR-0011): :utc-offset is a rendering/manifest-only concern (the
  engine's ground-truth log never needs it, sim/ADR-0011 -- it never
  shifts the underlying arithmetic, only labels which fixed offset the
  naive wall clock is asserted to be in) so it's threaded here and into
  emit-hl7, not into engine/run. :warm-up-seconds IS an engine concern
  (each event's :warm-up mark is set as the log is generated) so it
  threads into engine/run AND into check/check-all's warm-up-mark
  invariant, so a run always self-checks against the SAME window it
  was actually generated with."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-check.interface :as check]
            [ehrt.sim-engine.interface :as churn]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]
            [ehrt.sim-emit-fhir.interface :as emit-fhir]
            [ehrt.sim-trajectory.interface :as sim-trajectory]
            [ehrt.sim.manifest :as manifest]
            [ehrt.sim-model.interface :as sim-model]))

(defn- module-resolve-fn
  "D3's own real caller shape -- a thin `io/resource` wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(defn- module-table-resolve-fn
  "D3a/H2's own real caller shape -- a thin `io/resource` wrapper over
  `sim/modules/lookup_tables/<table-name>` (the table name already
  carries its own `.csv` extension, unlike a module call-path)."
  [table-name]
  (some-> (io/resource (str "sim/modules/lookup_tables/" table-name)) slurp))

(defn resolve-modules
  "M5b: `:modules` at the config/CLI-facing layer is a vector of NAME
  STRINGS (resolving to `resources/modules/<name>.json` -- test code may
  point at other fixture paths via a lower-level API, per this
  function's own callers, but `run-command`'s own surface only ever
  resolves the real vendored directory); `ehrt.sim-engine.engine/run`'s OWN
  `:modules` key wants already-loaded, CLOSURE-shaped entries instead
  (ADR-0033 AR-2 -- `sim-trajectory/load-closure`'s own `:ok` payload,
  `{:root :modules :tables}`, engine.clj does no file I/O of its own,
  the same layering `:facility`/`:providers` already follow). This is
  THIS namespace's own translation step, the same role
  `:churn`/`:churn-profile`'s own translation already plays.
  Result-not-throw: a missing or invalid module name in a caller's own
  config is an operational error (`:module-not-found`/
  `:module-load-failed`), never a thrown exception.

  ADR-0033 AR-1: `initial-attributes-by-name` (optional, default `{}`)
  -- `{module-name {attr value}}`, this namespace's own `:module-
  initial-attributes` opts key -- attaches each name's own seed map
  onto its resolved closure as `:initial-attributes`, when non-empty
  (absent-means-untouched, the same opt-in law every other engine-
  facing key here already follows). A scenario-authoring knob, not
  engine machinery: the engine only ever threads what it's handed."
  ([names] (resolve-modules names {}))
  ([names initial-attributes-by-name]
   (loop [names names acc []]
     (if (empty? names)
       (result/ok acc)
       (let [module-name (first names)
             res (io/resource (str "sim/modules/" module-name ".json"))]
         (if (nil? res)
           (result/error :module-not-found {:module module-name})
           (let [loaded (sim-trajectory/load-closure module-name (slurp res) module-resolve-fn module-table-resolve-fn)]
             (if (result/ok? loaded)
               (let [closure (:payload loaded)
                     ia (get initial-attributes-by-name module-name)]
                 (recur (rest names)
                        (conj acc (cond-> closure (seq ia) (assoc :initial-attributes ia)))))
               (result/error :module-load-failed
                             {:module module-name :category (:category loaded) :payload (:payload loaded)})))))))))

;; --- M6 Task 0: config-reachable :self-check-failed, recategorized -------
;; The tools full-capability session (`tools/ADR-0015`, this project's
;; own frozen pre-merge history, notes/tools/ADRs.md) found that
;; assigning one patient ordinal BOTH an
;; authored encounter-opening pathway (:admission/:outpatient-visit
;; somewhere in its steps) AND a GMF module reaches engine/run and blows
;; up as :self-check-failed -- a config-reachable outcome wearing the
;; "bug in us" category. The module's own compiled trajectory is
;; PREPENDED ahead of whatever pathway already queued
;; (ehrt.sim-engine.engine/run's own :registered decide method), so the
;; pathway's own encounter-opening step finds an already-non-:new
;; patient -- illegal under this project's single-encounter-horizon
;; scope (sim/ADR-0007 point 3). The combination stays illegal; this makes
;; the refusal honest and early: :rejected :incompatible-assignment,
;; naming the patient ordinal and the two conflicting sources, BEFORE
;; engine/run (and its RNG) ever starts. Purely structural -- no RNG, no
;; module content resolved (every vendored module is encounter-bearing
;; by sim/ADR-0013's own curation criterion, so ANY module assignment
;; conflicts with ANY encounter-opening pathway, regardless of which
;; specific module).

(defn- admission-bearing-pathway?
  [pathway]
  (boolean (some #(#{:admission :outpatient-visit} (:type %)) (:steps pathway))))

(defn- ordinal-guaranteed-admission-bearing?
  "Whether ordinal `i` is CERTAIN to receive an encounter-opening
  pathway, given only the config's own structure -- never the RNG. No
  `:pathways` override means every ordinal gets the plain `:pathway`
  (engine/run's own default when absent, sim-model/
  sample-admission-discharge, mirrored here since this check runs
  before engine/run supplies it). With `:pathways` present: an explicit
  `:patient-ordinal` entry decides outright; otherwise a non-empty
  weighted pool is 'guaranteed admission-bearing' only when EVERY pool
  member is -- a pool mixing admission-bearing and empty pathways
  can't be called either way without the RNG, so it is NOT reported
  (this check only ever reports certain conflicts, never possible
  ones)."
  [pathway pathways i]
  (if (nil? pathways)
    (admission-bearing-pathway? pathway)
    (let [explicit (first (filter #(= i (:patient-ordinal %)) pathways))
          pool (filterv :weight pathways)]
      (cond
        explicit (admission-bearing-pathway? (:pathway explicit))
        (seq pool) (every? #(admission-bearing-pathway? (:pathway %)) pool)
        :else false))))

(defn- ordinal-guaranteed-module?
  "Whether ordinal `i` is CERTAIN to receive SOME module. An explicit
  `:patient-ordinal` entry decides outright; otherwise
  `ehrt.sim-engine.engine/assign-module` always resolves a NON-EMPTY
  pool to some pool member for any ordinal no explicit entry covers --
  there is no 'opt out' of a present pool -- so a non-empty pool alone
  is already certain, independent of which module the RNG eventually
  picks."
  [module-assignment i]
  (boolean
   (or (first (filter #(= i (:patient-ordinal %)) module-assignment))
       (seq (filterv :weight module-assignment)))))

(defn incompatible-assignments
  "The full check: every ordinal (0-indexed, `patients` of them,
  default 1) certain to receive BOTH an encounter-opening pathway and a
  module. Guarded by each config's OWN schema validity
  (`sim-model/valid-pathways-config?`/`valid?`,
  `sim-trajectory/valid-modules-config?`) so a structurally
  malformed config (this namespace's own plumbing-completeness test's
  sentinel opts) is silently skipped here -- never misdiagnosed as a
  conflict, never thrown on -- rather than validated twice; a
  malformed config's OWN failure mode belongs to whatever consumes it
  for real (`engine/run`'s own `:pre` assertions), not this check."
  [{:keys [pathway pathways patients module-assignment]}]
  (when (and module-assignment
             (sim-trajectory/valid-modules-config? module-assignment)
             (or (nil? pathways) (sim-model/valid-pathways-config? pathways))
             (sim-model/valid? (or pathway sim-model/sample-admission-discharge)))
    (into []
          (keep (fn [i]
                  (when (and (ordinal-guaranteed-module? module-assignment i)
                             (ordinal-guaranteed-admission-bearing?
                              (or pathway sim-model/sample-admission-discharge) pathways i))
                    {:patient-ordinal i
                     :pathway-source (if pathways :pathways :pathway)
                     :module-source :module-assignment})))
          (range (or patients 1)))))

(defn effective-churn-profile
  "M2b: resolves opts' `:churn`/`:churn-profile` into the profile
  `engine/run` actually wants -- a bare `:churn` toggle expands to
  `churn/sample-profile`; an explicit `:churn-profile` is merged OVER
  `churn/default-churn-profile` (a caller only needs to name the rates
  they want to change); neither key present means nil, the opt-in path
  the pinned fixture depends on. Public: `ehrt.sim.identifiers/
  identifiers-command` reuses this SAME resolution (a projection over
  the same run `run-command` executes), rather than re-deriving it."
  [{:keys [churn churn-profile]}]
  (cond
    churn-profile (merge churn/default-churn-profile churn-profile)
    churn churn/sample-profile
    :else nil))

(defn merge-config-file
  "M4 Task 0: `:config` (a path to an EDN file) supplies the data-heavy
  engine keys that have no CLI flag of their own (`:pathway`/
  `:pathways`/`:order-profiles`, a full `:churn-profile` map) -- read
  once, merged UNDER the caller's own opts (an explicit opt wins over
  anything the file also names; the file exists to supply what flags
  can't express, not to override them). Absent `:config` -- the
  default -- this is the identity function on opts, byte for byte.
  Public: `ehrt.sim.identifiers/identifiers-command` reuses this
  SAME config-merging step (a projection over the same run this
  namespace's own `run-command` executes), rather than re-deriving it."
  [opts]
  (if-let [path (:config opts)]
    (merge (edn/read-string (slurp path)) (dissoc opts :config))
    opts))

(defn run-command
  "opts: :seed (required, long), :patients (long), :reference-date
  (ISO date string, pinned input for HL7 timestamp anchoring; defaults
  to emit-hl7/default-reference-date), :utc-offset (ISO-style fixed
  offset, pinned rendering/manifest input; defaults to emit-hl7/
  default-utc-offset), :warm-up-seconds (engine input, default 0),
  :emit (\"hl7\" to render messages into the payload), plus engine
  options. Runs the simulation, self-checks the invariant catalog over
  its own output (a run that violates its own invariants is an :error
  -- a bug in us, not a legitimate rejection), and returns
  {:ground-truth [...] :manifest {...} :summary {...}
   :messages [...]}} (:messages present only when :emit is \"hl7\").

  M6 Task 1: `:emit \"fhir\"` renders `:fhir-bundles`
  ({patient-id -> Bundle}, ehrt.sim-emit-fhir.interface/bundle-run)
  instead of `:messages` -- end-of-run snapshots by default, or the
  instant `:at` (seconds from run start) names. `:site-profile` has no
  bearing here (FHIR has no dialect-config surface yet, docs/sim-
  theory.edn's own format-dispatch note).

  M6 Task 0: one patient ordinal assigned BOTH an encounter-opening
  authored pathway AND a GMF module is `:rejected :incompatible-
  assignment` (payload {:conflicts [{:patient-ordinal :pathway-source
  :module-source} ...]}), checked BEFORE engine/run is ever called --
  `incompatible-assignments`' own docstring has the full reasoning
  (single-encounter-horizon, sim/ADR-0007; this used to reach engine/run
  and surface only as :self-check-failed once the invariant catalog
  caught the resulting double encounter).

  M2b: `:churn` (bare boolean, \"turn churn on with sensible defaults\"
  -- ehrt.sim-engine.churn/sample-profile) or `:churn-profile` (an
  explicit ehrt.sim-engine.churn/ChurnProfile map, merged OVER
  churn/default-churn-profile so a caller only needs to name the rates
  they want to change) activates InjectChurn between IR and execution
  (ehrt.sim-engine.engine/run's own :churn-profile wiring). Neither
  key present -- the default -- means no :churn-profile reaches
  engine/run at all, the opt-in path Task 0's pinned-fixture
  expectation depends on.

  M4 Task 0: `:config` (a path to an EDN file) is a passthrough vehicle
  for the data-heavy engine keys that have no CLI flag of their own
  (`:pathway`/`:pathways`/`:order-profiles`, and `:churn-profile` when a
  caller wants the full map rather than the bare `--churn` toggle) --
  read once, merged UNDER the rest of opts (explicit flag-driven keys
  win on any overlap; the file supplies what flags can't express) --
  see `merge-config-file`. Every key in `ehrt.sim-engine.engine/config-
  keys` reaches `engine/run` unconditionally, whether it arrived via a
  flag or via `:config` -- that completeness is this function's own
  plumbing-completeness test's whole point (M3's `:pathways` shipped
  CLI-invisible despite reaching `engine/run` from a direct API caller;
  never again silently, per that test).

  Milestone site-profiles: `:site-profile` (ehrt.sim-emit-hl7.site-profile/
  SiteProfile) is threaded straight to `emit-hl7/emit`, the SAME
  rendering/manifest-only treatment `:utc-offset` already gets -- it is
  NOT a member of `engine/config-keys` and never reaches `engine/run` at
  all (docs/site-profiles.md's own binds-at-emit-time-only law), so it
  rides `:config` the same passthrough way `:pathway`/`:order-profiles`
  do (no `--site-profile` flag exists), never the plumbing-completeness
  test's own engine-facing set.

  M5b: `:modules` (a vector of NAME STRINGS, resolving against
  `resources/modules/<name>.json` -- `resolve-modules`'s own docstring)
  and `:module-assignment`/`:module-horizon-days` (forwarded verbatim,
  no translation needed) ride `:config` the same passthrough way
  `:pathway`/`:order-profiles` already do (no `--modules` flag exists;
  this is data-heavy config, not a bare toggle). A name that fails to
  resolve or load is `:module-not-found`/`:module-load-failed` --
  surfaced BEFORE `engine/run` is ever called, the same
  fail-fast-on-a-bad-config posture `--seed` missing already gets.

  ADR-0033 AR-1: `:module-initial-attributes` (optional,
  `{module-name {attr value}}`) rides `:config` the same passthrough
  way -- a scenario-authoring knob for a module walk-entry seed
  (`resolve-modules`'s own docstring has the full attachment rule).
  Absent entirely -- the default -- every resolved closure carries no
  `:initial-attributes` at all, byte-identical to a run that never
  named this key.

  ADR-0042 AR-3: `:history` (optional boolean) rides `:config` the same
  passthrough way -- forwarded verbatim, no translation, no flag of its
  own (`ehrt.sim-engine.engine/run`'s own docstring has the mechanism this
  gates).

  `opts`'s second, injectable arity follows the SAME -fn convention
  `ehrt.sim-cli.core/dispatch-action` already uses (`:engine-run-fn`,
  defaulting to the real `ehrt.sim-engine.engine/run`) -- the seam the
  plumbing-completeness test uses to capture exactly what reaches the
  engine without running a real simulation against sentinel data."
  ([opts] (run-command opts {}))
  ([raw-opts {:keys [engine-run-fn] :or {engine-run-fn engine/run}}]
   (let [opts (merge-config-file raw-opts)
         {:keys [seed patients emit at reference-date utc-offset warm-up-seconds churn churn-profile site-profile
                 modules module-initial-attributes]} opts
         conflicts (incompatible-assignments opts)
         resolved-modules (when modules (resolve-modules modules (or module-initial-attributes {})))]
     (cond
       (nil? seed)
       (result/error :missing-required-opt
                     {:message "--seed is required (determinism is a feature, not a default)"
                      :opt :seed})

       (seq conflicts)
       (result/rejected :incompatible-assignment {:conflicts conflicts})

       (and resolved-modules (not (result/ok? resolved-modules)))
       resolved-modules

       :else
       (let [reference-date (or reference-date emit-hl7/default-reference-date)
             utc-offset (or utc-offset emit-hl7/default-utc-offset)
             warm-up-seconds (or warm-up-seconds 0)
             effective-churn-profile (effective-churn-profile opts)
             engine-params (-> (select-keys opts [:patients :arrival-gap :warm-up-seconds])
                                (assoc :reference-date reference-date :utc-offset utc-offset))
             engine-opts (cond-> (merge (select-keys opts engine/config-keys)
                                        {:seed seed :churn-profile effective-churn-profile})
                           resolved-modules (assoc :modules (:payload resolved-modules)))
             {:keys [ground-truth facility providers exhausted]} (engine-run-fn engine-opts)
             checked (when-not exhausted (check/check-all ground-truth facility warm-up-seconds))]
         (cond
           exhausted (result/error :capacity-exhausted exhausted)
           (not (result/ok? checked)) (result/error :self-check-failed (:payload checked))
           :else
           (result/ok
            (cond-> {:ground-truth ground-truth
                     :manifest (manifest/build {:seed seed
                                                :engine-params engine-params
                                                :config {:path "(inline)"
                                                         :sha256 (apply str (repeat 64 "0"))}
                                                :invocation {:verb "run" :opts opts}})
                     :summary {:patients (or patients 1)
                               :events (count ground-truth)}}
              (= "hl7" emit) (assoc :messages (emit-hl7/emit ground-truth reference-date utc-offset facility providers site-profile))
              (= "fhir" emit) (assoc :fhir-bundles
                                     (emit-fhir/bundle-run ground-truth reference-date utc-offset seed (or at :end)))))))))))
