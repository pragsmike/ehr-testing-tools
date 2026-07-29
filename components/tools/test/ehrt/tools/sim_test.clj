(ns ehrt.tools.sim-test
  "Hermetic: run! is tested against an injected :run-command-fn, never
  a real simulation (ADR-0005, carve-loss recovery -- this namespace no
  longer subprocesses or discovers a sibling checkout at all, so there
  is no discovery order left to test the way the pre-mount version of
  this suite did). :run-command-fn rides the same single opts map as
  every other value here -- the convention
  ehrt.tools.corpus.generate/generate!'s own :run-invocation already
  uses, not a separate injection argument."
  (:require [clojure.test :refer [deftest is]]
            [ehrt.tools.result :as result]
            [ehrt.tools.sim :as sim]))

(deftest run-delegates-to-run-command-fn-test
  (let [captured (atom nil)
        fake (fn [opts] (reset! captured opts) (result/ok {:manifest {:stage :simulated}}))
        r (sim/run! {:seed 42 :patients 3 :run-command-fn fake})]
    (is (result/ok? r))
    (is (= {:stage :simulated} (:manifest (:payload r))))
    (is (= {:seed 42 :patients 3} @captured)
        "run-command-fn itself is stripped out before delegating, same as :out-dir")))

(deftest run-strips-out-dir-and-discovery-keys-before-delegating-test
  (let [captured (atom nil)
        fake (fn [opts] (reset! captured opts) (result/ok {}))]
    (sim/run! {:seed 42 :out-dir "target/sim-harness" :sim-dir "/whatever"
               :env-sim-dir-fn (fn [] nil) :default-dir "../ehr-testing-sim"
               :run-command-fn fake})
    (is (= {:seed 42} @captured)
        "the old subprocess-only opts never reach run-command -- it doesn't know them")))

(deftest run-passes-through-rejected-and-error-unchanged-test
  (let [rejected-fake (fn [_] (result/rejected :incompatible-assignment {:conflicts []}))
        error-fake (fn [_] (result/error :missing-required-opt {:opt :seed}))]
    (is (= :incompatible-assignment (:category (sim/run! {:run-command-fn rejected-fake}))))
    (is (= :missing-required-opt (:category (sim/run! {:run-command-fn error-fake}))))))

(deftest run-default-calls-the-real-run-command-test
  ;; The ONE non-hermetic case, deliberately: proves the default
  ;; (no :run-command-fn given) actually wires to the real
  ;; ehrt.sim.interface/run-command, not just to itself -- a real, fast,
  ;; deterministic 1-patient run (same fixed seed convention as
  ;; sim_manifest_contract_test.clj's own smallest-known-fast
  ;; invocation) rather than mocking the one seam meant to prove the
  ;; real wiring works.
  (let [r (sim/run! {:seed 100 :patients 1})]
    (is (result/ok? r))
    (is (map? (:manifest (:payload r))))))
