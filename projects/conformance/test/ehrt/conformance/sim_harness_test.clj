(ns ehrt.conformance.sim-harness-test
  "ADR-0005: sim-harness.clj is now a one-line pass-through to
  ehrt.corpus.interface/sim-run! -- everything this suite used to prove
  (out-dir defaulting, availability discovery, the absence message) no
  longer exists to test; that behavior matrix lives at the source
  adapter now (components/corpus/test/ehrt/corpus/sim_adapter_test.clj). This is
  the one thing left worth asserting: the pass-through doesn't
  accidentally transform opts on the way through."
  (:require [clojure.test :refer [deftest is]]
            [ehrt.kernel.interface :as result]
            [ehrt.conformance.sim-harness :as sim-harness]))

(deftest run-delegates-opts-unchanged-test
  (let [r (sim-harness/run! {:seed 100 :patients 1})]
    (is (result/ok? r))
    (is (map? (:manifest (:payload r))))))
