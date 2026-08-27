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
            [clojure.string :as str]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-check.interface :as check]
            [ehrt.sim-engine.interface :as churn]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]
            [ehrt.sim-emit-fhir.interface :as emit-fhir]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.person-simulator.interface :as person-simulator]
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
  (ADR-0033 AR-2 -- `patient-simulator/load-closure`'s own `:ok` payload,
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
           (let [loaded (patient-simulator/load-closure module-name (slurp res) module-resolve-fn module-table-resolve-fn)]
             (if (result/ok? loaded)
               (let [closure (:payload loaded)
                     ia (get initial-attributes-by-name module-name)]
                 (recur (rest names)
                        (conj acc (cond-> closure (seq ia) (assoc :initial-attributes ia)))))
               (result/error :module-load-failed
                             {:module module-name :category (:category loaded) :payload (:payload loaded)})))))))))

;; --- arc 3a part 3: `:persons`, config-facing -> engine-facing ------------
;;
;; ADR-0173 section 2(a). `ehrt.sim-engine.engine/run` does not require
;; the person component and CANNOT: that component depends on
;; sim-engine's stream-partition surface, and the reverse edge would be
;; a cycle `clojure -M:poly check` refuses (ADR-0172 limitations row
;; 10). So the person events reach the engine as DATA, and THIS
;; namespace -- which may require both -- is where they are built. It is
;; exactly the layering `:modules` already follows: names here, loaded
;; closures there, one translation step in between.
;;
;; ADR-0172 ruling F1 ("the component lands ALONE: nothing in this
;; workspace calls it, and nothing may until arc 3's fold") is LIFTED
;; here, by a caller that is not a sim-engine namespace. Row 10 stays
;; green verbatim, both halves.

(def default-person-years
  "How many whole years the person walk covers when `:persons` names no
  `:years`. Ten, so a default run's people actually move house, change
  jobs and change payers at the rates the process draws -- a one-year
  walk over a handful of people produces almost nothing, and a knob
  whose default produces nothing is a knob that ships untested."
  10)

(defn- person-id-for
  "The person pool's own id space, and deliberately NOT the patient id
  space: a person is not a patient until an arrival binds them to one,
  and two id spaces that look alike would invite a reader to join them
  by string equality."
  [i]
  (format "PERSON-%06d" i))

(defn- person-walk-config
  "The person process's own config, built from `:persons`' authored map
  plus whatever `:persona-config` this run already carries. The payer
  pools are read off `:persona-config` rather than named twice: a run's
  people and its patients draw coverage from ONE pool set, and letting
  them diverge would produce a corpus whose registrations and whose
  coverage changes disagree about which payers exist.

  nil-valued keys are dropped, not passed: the process reads
  `:unhoused` through a defaulting `get-in`, so an explicit nil would
  override its own default with zero.

  ARC 3A PART 4: the payer pools FALL BACK to `sim-model`'s own, and
  that closes a gap arc 2b recorded rather than worked around.
  `ehrt.person-simulator.process` emits no `:coverage-change` at all
  for a run that names neither pool -- the variates are drawn and the
  event is not -- so a scenario with no `:persona-config` produced ZERO
  of a kind the 1.3.0 contract declares, measured across all four gated
  corpora. Defaulting here rather than restating the pools in a
  scenario file is deliberate: a forked payer pool is the same class of
  drift that component's own docstring forbids for addresses. The run's
  patients already draw from these defaults (`sim-model/persona`'s own
  `:or`), so this makes the two agree instead of making them differ."
  [{:keys [persons persona-config]} population]
  (let [{:keys [years identification unhoused]} persons
        pc (or persona-config {})]
    (into {} (remove (comp nil? val))
          {:t0 0
           :years (or years default-person-years)
           :population population
           :persona pc
           :payers-under-65 (or (:payers-under-65 pc) sim-model/under-65-payers)
           :payers-65-plus (or (:payers-65-plus pc) sim-model/sixty-five-plus-payers)
           :identification identification
           :unhoused unhoused})))

(defn valid-persons-config?
  "Whether `:persons`' authored value is well-formed: a map with a
  positive `:count`, and a positive `:years` if it names one."
  [persons]
  (and (map? persons)
       (pos-int? (:count persons))
       (or (nil? (:years persons)) (pos-int? (:years persons)))))

(defn engine-persons
  "`:persons`, translated: the authored `{:count :years :identification
  :unhoused}` map becomes the `{:population :personas :alive :events}`
  value `ehrt.sim-engine.engine/run` takes.

  TWO CALLS TO `persons`, AND THE REASON IS AN ORDERING ONE. ADR-0173
  ruling C1 gives the person process the COMPILED trajectory's death
  instant as a t0 parameter, keyed by person -- so the deaths cannot be
  computed until the person-to-arrival binding is known, and the binding
  is a `:world`-family draw taken inside the run's own pre-loop over the
  persons ALIVE at each arrival instant (ruling A1). Written naively
  that is a cycle: aliveness depends on deaths, deaths depend on the
  binding, the binding depends on aliveness.

  It is broken at the ALIVE FILTER, and the break is disclosed rather
  than hidden. Pass 1 runs with no compiled deaths at all, and its
  `:person-death` events are what `:alive` carries -- each person's OWN
  drawn death, which depends on nothing but their own `:person` stream.
  The binding is then a function of fixed data, `engine/person-plan`
  answers it, the compiled deaths follow, and pass 2 produces the stream
  the engine actually folds. Both passes draw from freshly derived
  streams, so the second is not a continuation of the first and nothing
  is consumed twice.

  What the filter is CONSERVATIVE about, said plainly: a person whose
  own drawn death precedes an arrival is not selectable for it, even
  though binding them would have replaced that death with a later
  compiled one. That direction is the safe one. The direction the filter
  exists to forbid -- an arrival landing on somebody the ground truth
  already says is dead -- is closed absolutely, because a compiled death
  is by construction at or after the arrival that produced it."
  [engine-opts]
  (let [{:keys [seed persons persona-config]} engine-opts
        population (vec (for [i (range (:count persons))]
                          {:person-id (person-id-for i) :id-tag (inc i)}))
        personas (into {} (for [{:keys [person-id id-tag]} population]
                            [person-id (person-simulator/initial-persona
                                        person-id
                                        {:rng (engine/stream seed :person id-tag)
                                         :t 0 :master seed :id-tag id-tag
                                         :persona (or persona-config {})})]))
        walk (person-walk-config engine-opts population)
        pass-1 (person-simulator/persons (assoc walk :deaths {}) {:master seed})
        alive (engine/person-deaths pass-1)
        provisional {:population population :personas personas :alive alive :events pass-1}
        plan (engine/person-plan (assoc engine-opts :persons provisional))
        pass-2 (person-simulator/persons (assoc walk :deaths (:deaths plan)) {:master seed})]
    (assoc provisional :events pass-2)))

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
  `patient-simulator/valid-modules-config?`) so a structurally
  malformed config (this namespace's own plumbing-completeness test's
  sentinel opts) is silently skipped here -- never misdiagnosed as a
  conflict, never thrown on -- rather than validated twice; a
  malformed config's OWN failure mode belongs to whatever consumes it
  for real (`engine/run`'s own `:pre` assertions), not this check."
  [{:keys [pathway pathways patients module-assignment]}]
  (when (and module-assignment
             (patient-simulator/valid-modules-config? module-assignment)
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

(defn- config-file-stem
  [^String filename]
  (str/replace filename #"\.[^./]+$" ""))

(defn- similar-sibling-config
  "U4 (ux fixes 2, `notes/adr/0060-ux-fixes-2.md`): when `:config` names
  a path that doesn't exist, a same-directory file sharing its own
  filename stem but a different extension is this arc's own founding
  incident, made structural -- a `.md` sitting where a `.edn` was
  named. `ehrt.kernel` has no existing similar-file/did-you-mean helper
  (checked, confirmed absent this session) -- this is a small, local
  fn, not a reusable one, since C-1's own fix is scoped to this
  namespace. Returns the sibling's own path string, or nil.

  Quality riders (AR-QR-2, ADR-0076): `.listFiles` returns nil on an
  I/O failure (permission denied, directory removed mid-read) -- NOT
  for an empty directory, which yields an empty array. Silently
  treating both as 'no sibling' conflated a real failure with a
  negative result. Retried once (a transient filesystem hiccup on a
  CI runner self-heals); a still-nil result after the retry returns
  nil exactly as before -- a did-you-mean is decoration and its
  absence must never fail the error path it decorates -- but the
  failure mode is now named here rather than absorbed silently."
  [path]
  (let [f (io/file path)
        dir (or (.getParentFile f) (io/file "."))
        stem (config-file-stem (.getName f))]
    (when (.isDirectory dir)
      (when-let [files (or (.listFiles dir) (.listFiles dir))]
        (some-> (->> files
                     (filter #(.isFile ^java.io.File %))
                     (remove #(= (.getName ^java.io.File %) (.getName f)))
                     (filter #(= stem (config-file-stem (.getName ^java.io.File %))))
                     first)
                .getPath)))))

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
  namespace's own `run-command` executes), rather than re-deriving it.

  C-1 (ux fixes 2, `notes/adr/0060-ux-fixes-2.md`): Result-wrapped, the
  same pattern this codebase's every other file-reading operational
  boundary already uses (`ehrt.kernel.artifact/read-lockfile`'s own
  positive control) -- a missing path is `result/error :config-not-found
  {:path path}` (U4: plus `:did-you-mean` when a same-stem sibling
  exists), and a present-but-unparseable file is `result/error
  :config-unreadable {:path path :message ...}`, never a raw JVM
  exception reaching the CLI shell. Callers must unwrap the Result."
  [opts]
  (if-let [path (:config opts)]
    (if (.exists (io/file path))
      (try
        (result/ok (merge (edn/read-string (slurp path)) (dissoc opts :config)))
        (catch Exception e
          (result/error :config-unreadable {:path path :message (.getMessage e)})))
      (let [sibling (similar-sibling-config path)]
        (result/error :config-not-found
                       (cond-> {:path path} sibling (assoc :did-you-mean sibling)))))
    (result/ok opts)))

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

  ADR-0109: `:latency` (ehrt.sim-model.config/LatencyProfile, optional)
  is the SAME rendering-only treatment `:site-profile` already gets --
  not a member of `engine/config-keys`, never reaches `engine/run`.
  Present: this run's own ground-truth log and a FRESH `java.util.Random`
  seeded from this same run's `:seed` (a second, independently-seeded
  stream -- never the engine's own sealed RNG) feed `emit-hl7/plan-
  latency`, and `:messages` renders via `emit-hl7/emit-wire` (transmit-
  time order) instead of `emit-hl7/emit` (log order). Absent (the
  default): `:messages` renders via `emit-hl7/emit` exactly as before
  this ADR, byte-identical -- `:latency` rides `:config` the same
  passthrough way `:site-profile` does (no `--latency` flag exists).

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
   (let [config-result (merge-config-file raw-opts)]
     (if-not (result/ok? config-result)
       config-result
       (let [opts (:payload config-result)
             {:keys [seed patients emit at reference-date utc-offset warm-up-seconds churn churn-profile site-profile
                     modules module-initial-attributes latency]} opts
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

           ;; ADR-0173 section 2(a): rejected BEFORE the engine (and its
           ;; RNG) ever starts, the same fail-fast-on-a-bad-config
           ;; posture a missing `--seed` already gets.
           (and (contains? opts :persons) (not (valid-persons-config? (:persons opts))))
           (result/error :invalid-persons
                         {:key :persons
                          :value (:persons opts)
                          :expected "{:count <positive int> :years <positive int, optional> ...}"})

           :else
           (let [reference-date (or reference-date emit-hl7/default-reference-date)
                 utc-offset (or utc-offset emit-hl7/default-utc-offset)
                 warm-up-seconds (or warm-up-seconds 0)
                 effective-churn-profile (effective-churn-profile opts)
                 ;; ADR-0173 section 2(f): the two keys a demographic
                 ;; fold's configuration lives in, stamped so the
                 ;; artifact's own face says a fold ran and under what
                 ;; settings. `:persons` here is the AUTHORED map, not
                 ;; the translated payload -- a manifest describes a
                 ;; configuration, and the population itself is the
                 ;; corpus. `ManifestV1_1` is an open map, so this is
                 ;; additive at the same seam `:event-schema-version` and
                 ;; `:stream-scheme` already ride, and no schema moves.
                 ;; ARC 3B SWEEP 1 (ADR-0174 section 2(a)): `:encounters`
                 ;; joins for the SAME reason `:persons` did -- a corpus
                 ;; whose patients can have more than one visit is a
                 ;; different artifact from one whose patients cannot,
                 ;; and its own face should say so rather than leaving a
                 ;; reader to infer it from the presence of a second
                 ;; `:admission`. Absent from `opts`, `select-keys`
                 ;; leaves it absent here, so no legacy manifest moves.
                 engine-params (-> (select-keys opts [:patients :arrival-gap :warm-up-seconds
                                                      :persons :persona-config :encounters])
                                    (assoc :reference-date reference-date :utc-offset utc-offset))
                 engine-opts (cond-> (merge (select-keys opts engine/config-keys)
                                            {:seed seed :churn-profile effective-churn-profile})
                               resolved-modules (assoc :modules (:payload resolved-modules)))
                 ;; `:persons` reaches `engine/run` TRANSLATED or not at
                 ;; all -- absent entirely is the byte-identical path, and
                 ;; the authored map would be a malformed engine value.
                 engine-opts (if (:persons engine-opts)
                               (assoc engine-opts :persons (engine-persons engine-opts))
                               engine-opts)
                 engine-result (engine-run-fn engine-opts)
                 {:keys [ground-truth facility providers exhausted]} engine-result
                 checked (when (and (not exhausted) (not (result/error? engine-result)))
                           (check/check-all ground-truth facility warm-up-seconds))]
             (cond
               ;; sim/ADR-0116: engine/run now returns result/error :invalid-seed
               ;; (rather than throwing or running) for an out-of-contract seed --
               ;; propagate it as-is, the same error/result convention every other
               ;; branch here already returns.
               (result/error? engine-result) engine-result
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
                  (= "hl7" emit)
                  (assoc :messages
                        (if latency
                          (emit-hl7/emit-wire ground-truth reference-date utc-offset facility providers site-profile
                                              ;; ADR-0171 ruling C1: the emission latency
                          ;; stream is the :emission FAMILY, derived like
                          ;; every other. It used to be `(java.util.Random.
                          ;; seed)` -- the master seed VERBATIM, so this
                          ;; stream replayed the engine's own first draws.
                          ;; The correlation is invisible while emission is
                          ;; one draw per event and the two streams are
                          ;; consumed for unrelated purposes; arc 4 adds
                          ;; chatter, fan-out and status ladders to this
                          ;; side, and one `mix64` decorrelates them before
                          ;; that lands.
                          (emit-hl7/plan-latency (engine/stream seed :emission 0) ground-truth latency))
                          (emit-hl7/emit ground-truth reference-date utc-offset facility providers site-profile)))
                  (= "fhir" emit) (assoc :fhir-bundles
                                         (emit-fhir/bundle-run ground-truth reference-date utc-offset seed (or at :end)))))))))))))
