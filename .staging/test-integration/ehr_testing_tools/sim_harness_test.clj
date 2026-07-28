(ns ehr-testing-tools.sim-harness-test
  "Coverage of sim-harness's own remaining logic ONLY (Step 3, ruling
  5): the full run! behavior matrix (ok-payload unwrap, nonzero exit,
  rejected status, spawn failure, the :config absolute-path rewrite,
  discovery) moved to test/ehr_testing_tools/sim_test.clj when that
  logic moved to the src/ adapter -- re-testing it here would be
  exactly the duplication ruling 5 says dies at this delegation; this
  namespace now proves only that sim-harness/run! actually delegates
  (defaulting :out-dir to this suite's own \"target/sim-harness\" log
  convention, never overriding a caller's own :out-dir) and that
  available?/absence-message still work. Lives on the test-integration
  path only because sim-harness.clj itself does (it is not required
  from test/); needs no sibling checkout to run."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.sim :as sim]
            [ehr-testing-tools.sim-harness :as sim-harness]))

(defn- fake-invocation
  [exit-code stdout]
  (fn [{:keys [stdout-path stderr-path]}]
    (spit stdout-path stdout)
    (spit stderr-path "")
    (result/ok {:exit-code exit-code})))

(deftest run-defaults-out-dir-to-target-sim-harness-test
  (let [captured (atom nil)
        underlying (fake-invocation 0 (pr-str {:status :ok :payload {}}))
        fake (fn [opts] (reset! captured opts) (underlying opts))]
    (sim-harness/run! {:seed 42 :sim-dir "." :run-invocation fake})
    (is (re-find #"^target/sim-harness/" (:stdout-path @captured)))))

(deftest run-caller-out-dir-override-wins-test
  (let [captured (atom nil)
        underlying (fake-invocation 0 (pr-str {:status :ok :payload {}}))
        fake (fn [opts] (reset! captured opts) (underlying opts))]
    (sim-harness/run! {:seed 42 :sim-dir "." :out-dir "target/somewhere-else" :run-invocation fake})
    (is (re-find #"^target/somewhere-else/" (:stdout-path @captured)))))

(deftest run-delegates-to-sim-run-unchanged-test
  (let [fake (fake-invocation 0 (pr-str {:status :ok :payload {:manifest {:stage :simulated}}}))
        r (sim-harness/run! {:seed 42 :sim-dir "." :run-invocation fake})]
    (is (result/ok? r))
    (is (= {:stage :simulated} (:manifest (:payload r))))))

(deftest available-delegates-to-sim-available-test
  (is (= (sim/available?) (sim-harness/available?))))

(deftest absence-message-names-the-discovery-paths-test
  (is (str/includes? sim-harness/absence-message sim/sim-dir-env-var))
  (is (str/includes? sim-harness/absence-message sim/default-sim-repo-dir)))
