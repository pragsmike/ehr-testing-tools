(ns ehr-testing-sim.run
  "The `sim run` capability: config -> {simulation output + manifest},
  wrapped in the Result vocabulary. Never prints (only the CLI shell
  prints); never throws for expected failures.

  v0 returns the ground-truth log directly in the payload. Message
  emission (HL7v2 via the ER7 emitter over org.clojars.cmiles74/
  clojure-hl7-parser structures, ehr-testing-sim.emit-hl7) attaches
  here as an output stage consuming the log -- the log is primary,
  messages are a rendering, and emission is opt-in (:emit \"hl7\").
  Milestone M1: engine/run echoes back the :facility and materialized
  :providers it actually allocated against (docs/operational-models.md);
  both check/check-all (the capacity/surge-ladder invariants) and
  emit-hl7/emit (PV1-3/6/7) are threaded that SAME config, not a fresh
  default that might not even share ward names.

  M2a (ADR-0011): :utc-offset is a rendering/manifest-only concern (the
  engine's ground-truth log never needs it, ADR-0011 -- it never
  shifts the underlying arithmetic, only labels which fixed offset the
  naive wall clock is asserted to be in) so it's threaded here and into
  emit-hl7, not into engine/run. :warm-up-seconds IS an engine concern
  (each event's :warm-up mark is set as the log is generated) so it
  threads into engine/run AND into check/check-all's warm-up-mark
  invariant, so a run always self-checks against the SAME window it
  was actually generated with."
  (:require [clojure.edn :as edn]
            [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.check :as check]
            [ehr-testing-sim.churn :as churn]
            [ehr-testing-sim.emit-hl7 :as emit-hl7]
            [ehr-testing-sim.manifest :as manifest]
            [ehr-testing-sim.result :as result]))

(defn- merge-config-file
  "M4 Task 0: `:config` (a path to an EDN file) supplies the data-heavy
  engine keys that have no CLI flag of their own (`:pathway`/
  `:pathways`/`:order-profiles`, a full `:churn-profile` map) -- read
  once, merged UNDER the caller's own opts (an explicit opt wins over
  anything the file also names; the file exists to supply what flags
  can't express, not to override them). Absent `:config` -- the
  default -- this is the identity function on opts, byte for byte."
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

  M2b: `:churn` (bare boolean, \"turn churn on with sensible defaults\"
  -- ehr-testing-sim.churn/sample-profile) or `:churn-profile` (an
  explicit ehr-testing-sim.churn/ChurnProfile map, merged OVER
  churn/default-churn-profile so a caller only needs to name the rates
  they want to change) activates InjectChurn between IR and execution
  (ehr-testing-sim.engine/run's own :churn-profile wiring). Neither
  key present -- the default -- means no :churn-profile reaches
  engine/run at all, the opt-in path Task 0's pinned-fixture
  expectation depends on.

  M4 Task 0: `:config` (a path to an EDN file) is a passthrough vehicle
  for the data-heavy engine keys that have no CLI flag of their own
  (`:pathway`/`:pathways`/`:order-profiles`, and `:churn-profile` when a
  caller wants the full map rather than the bare `--churn` toggle) --
  read once, merged UNDER the rest of opts (explicit flag-driven keys
  win on any overlap; the file supplies what flags can't express) --
  see `merge-config-file`. Every key in `ehr-testing-sim.engine/config-
  keys` reaches `engine/run` unconditionally, whether it arrived via a
  flag or via `:config` -- that completeness is this function's own
  plumbing-completeness test's whole point (M3's `:pathways` shipped
  CLI-invisible despite reaching `engine/run` from a direct API caller;
  never again silently, per that test).

  Milestone site-profiles: `:site-profile` (ehr-testing-sim.site-profile/
  SiteProfile) is threaded straight to `emit-hl7/emit`, the SAME
  rendering/manifest-only treatment `:utc-offset` already gets -- it is
  NOT a member of `engine/config-keys` and never reaches `engine/run` at
  all (docs/site-profiles.md's own binds-at-emit-time-only law), so it
  rides `:config` the same passthrough way `:pathway`/`:order-profiles`
  do (no `--site-profile` flag exists), never the plumbing-completeness
  test's own engine-facing set.

  `opts`'s second, injectable arity follows the SAME -fn convention
  `ehr-testing-sim.cli/dispatch-action` already uses (`:engine-run-fn`,
  defaulting to the real `ehr-testing-sim.engine/run`) -- the seam the
  plumbing-completeness test uses to capture exactly what reaches the
  engine without running a real simulation against sentinel data."
  ([opts] (run-command opts {}))
  ([raw-opts {:keys [engine-run-fn] :or {engine-run-fn engine/run}}]
   (let [opts (merge-config-file raw-opts)
         {:keys [seed patients emit reference-date utc-offset warm-up-seconds churn churn-profile site-profile]} opts]
     (if (nil? seed)
       (result/error :missing-required-opt
                     {:message "--seed is required (determinism is a feature, not a default)"
                      :opt :seed})
       (let [reference-date (or reference-date emit-hl7/default-reference-date)
             utc-offset (or utc-offset emit-hl7/default-utc-offset)
             warm-up-seconds (or warm-up-seconds 0)
             effective-churn-profile (cond
                                       churn-profile (merge churn/default-churn-profile churn-profile)
                                       churn churn/sample-profile
                                       :else nil)
             engine-params (-> (select-keys opts [:patients :arrival-gap :warm-up-seconds])
                                (assoc :reference-date reference-date :utc-offset utc-offset))
             {:keys [ground-truth facility providers exhausted]}
             (engine-run-fn (merge (select-keys opts engine/config-keys)
                                   {:seed seed :churn-profile effective-churn-profile}))
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
              (= "hl7" emit) (assoc :messages (emit-hl7/emit ground-truth reference-date utc-offset facility providers site-profile))))))))))
