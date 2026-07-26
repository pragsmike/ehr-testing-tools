(ns ehr-testing-sim.run
  "The `sim run` capability: config -> {simulation output + manifest},
  wrapped in the Result vocabulary. Never prints (only the CLI shell
  prints); never throws for expected failures.

  v0 returns the ground-truth log directly in the payload. Message
  emission (HL7v2 via the ER7 emitter over org.clojars.cmiles74/
  clojure-hl7-parser structures) attaches here as an output stage
  consuming the log -- the log is primary, messages are a rendering."
  (:require [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.check :as check]
            [ehr-testing-sim.manifest :as manifest]
            [ehr-testing-sim.result :as result]))

(defn run-command
  "opts: :seed (required, long), :patients (long), plus engine options.
  Runs the simulation, self-checks the invariant catalog over its own
  output (a run that violates its own invariants is an :error -- a bug
  in us, not a legitimate rejection), and returns
  {:ground-truth [...] :manifest {...} :summary {...}}."
  [{:keys [seed patients] :as opts}]
  (if (nil? seed)
    (result/error :missing-required-opt
                  {:message "--seed is required (determinism is a feature, not a default)"
                   :opt :seed})
    (let [engine-params (select-keys opts [:patients :arrival-gap])
          {:keys [ground-truth]} (engine/run (merge {:seed seed} engine-params))
          checked (check/check-all ground-truth)]
      (if-not (result/ok? checked)
        (result/error :self-check-failed (:payload checked))
        (result/ok
         {:ground-truth ground-truth
          :manifest (manifest/build {:seed seed
                                     :engine-params engine-params
                                     :config {:path "(inline)"
                                              :sha256 (apply str (repeat 64 "0"))}
                                     :invocation {:verb "run" :opts opts}})
          :summary {:patients (or patients 1)
                    :events (count ground-truth)}})))))
