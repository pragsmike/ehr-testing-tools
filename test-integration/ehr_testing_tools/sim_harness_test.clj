(ns ehr-testing-tools.sim-harness-test
  "Unit-level coverage of sim-harness/run!'s own logic, via a fake
  :run-invocation (never a real subprocess, never touching whether the
  sibling checkout exists -- AGENTS.md's hermetic-fake convention,
  applied here the same way judge.fhir_test.clj fakes :run-invocation).
  Lives on the test-integration path only because sim-harness.clj itself
  does (it is not required from test/); these tests need no sibling
  checkout and always run under `make integration`."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.sim-harness :as sim-harness]))

(defn- fake-invocation
  "A :run-invocation fake: writes stdout/stderr exactly like the real
  wrapper would (so sim-harness/run!'s own slurp calls succeed), then
  returns result/ok {:exit-code exit-code} -- invocation/run!'s own
  success shape, regardless of the wrapped command's exit code (a
  nonzero exit is a normal completed invocation, per invocation.clj's
  own docstring)."
  [exit-code stdout]
  (fn [{:keys [stdout-path stderr-path]}]
    (spit stdout-path stdout)
    (spit stderr-path "")
    (result/ok {:exit-code exit-code})))

(deftest run-unwraps-ok-payload-test
  (let [fake (fake-invocation 0 (pr-str {:status :ok
                                          :payload {:ground-truth []
                                                    :manifest {:stage :simulated}}}))
        r (sim-harness/run! {:seed 42 :run-invocation fake})]
    (is (result/ok? r))
    (is (= {:stage :simulated} (:manifest (:payload r))))))

(deftest run-nonzero-exit-is-sim-run-failed-test
  (let [fake (fake-invocation 2 "")
        r (sim-harness/run! {:seed 42 :run-invocation fake})]
    (is (result/error? r))
    (is (= :sim-run-failed (:category r)))
    (is (= 2 (:exit-code (:payload r))))))

(deftest run-sim-rejected-status-is-sim-run-rejected-test
  (let [fake (fake-invocation 0 (pr-str {:status :rejected :category :whatever :payload {}}))
        r (sim-harness/run! {:seed 42 :run-invocation fake})]
    (is (result/error? r))
    (is (= :sim-run-rejected (:category r)))))

(deftest run-spawn-failure-passes-through-test
  (let [fake (fn [_] (result/error :spawn-failed {:command "clojure"}))
        r (sim-harness/run! {:seed 42 :run-invocation fake})]
    (is (result/error? r))
    (is (= :spawn-failed (:category r)))))

(deftest run-config-opt-resolves-to-an-absolute-path-test
  ;; The full-capability gate loop's own --config passthrough
  ;; (test-integration/fixtures/sim-configs/full-capability.edn): a
  ;; relative fixture path must never reach argv verbatim, since the
  ;; subprocess's own working directory is the sibling checkout
  ;; (sim-harness/sim-repo-dir), not this repo's root -- captures the
  ;; :args a real invocation would receive without spawning one.
  (let [captured (atom nil)
        underlying (fake-invocation 0 (pr-str {:status :ok :payload {}}))
        fake (fn [opts] (reset! captured opts) (underlying opts))]
    (sim-harness/run! {:seed 42
                       :config "test-integration/fixtures/sim-configs/full-capability.edn"
                       :run-invocation fake})
    (let [args (:args @captured)
          config-idx (.indexOf ^java.util.List args "--config")]
      (is (pos? config-idx) "--config reached argv")
      (let [config-arg (nth args (inc config-idx))]
        (is (.isAbsolute (java.io.File. ^String config-arg)))
        (is (str/ends-with? config-arg "full-capability.edn"))))))
