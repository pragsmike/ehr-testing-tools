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
  (:require [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.check :as check]
            [ehr-testing-sim.churn :as churn]
            [ehr-testing-sim.emit-hl7 :as emit-hl7]
            [ehr-testing-sim.manifest :as manifest]
            [ehr-testing-sim.result :as result]))

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
  expectation depends on."
  [{:keys [seed patients emit reference-date utc-offset warm-up-seconds churn churn-profile] :as opts}]
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
          (engine/run (merge {:seed seed :churn-profile effective-churn-profile}
                             (select-keys opts [:patients :arrival-gap :facility :providers :warm-up-seconds])))
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
           (= "hl7" emit) (assoc :messages (emit-hl7/emit ground-truth reference-date utc-offset facility providers))))))))
