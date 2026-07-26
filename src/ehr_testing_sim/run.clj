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
   :messages [...]}} (:messages present only when :emit is \"hl7\")."
  [{:keys [seed patients emit reference-date utc-offset warm-up-seconds] :as opts}]
  (if (nil? seed)
    (result/error :missing-required-opt
                  {:message "--seed is required (determinism is a feature, not a default)"
                   :opt :seed})
    (let [reference-date (or reference-date emit-hl7/default-reference-date)
          utc-offset (or utc-offset emit-hl7/default-utc-offset)
          warm-up-seconds (or warm-up-seconds 0)
          engine-params (-> (select-keys opts [:patients :arrival-gap :warm-up-seconds])
                             (assoc :reference-date reference-date :utc-offset utc-offset))
          {:keys [ground-truth facility providers]}
          (engine/run (merge {:seed seed}
                             (select-keys opts [:patients :arrival-gap :facility :providers :warm-up-seconds])))
          checked (check/check-all ground-truth facility warm-up-seconds)]
      (if-not (result/ok? checked)
        (result/error :self-check-failed (:payload checked))
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
