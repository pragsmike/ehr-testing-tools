(ns ehrt.tools.sim-test
  "Test-first (ruling 4, SS-2 Step 3): written before ehrt.tools.
  sim existed. Hermetic throughout -- every subprocess call goes through
  an injected :run-invocation fake (never a real subprocess), and
  discovery itself is hermetic via injectable :env-sim-dir-fn/:default-dir
  overrides -- these tests must pass identically whether or not a real
  ../ehr-testing-sim sibling checkout happens to exist on the machine
  running them, unlike test-integration/ehr_testing_tools/sim_harness_
  test.clj's own real-subprocess-adjacent suite (which this namespace's
  own real discovery order feeds, unchanged in behavior -- ruling 5)."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [ehrt.tools.result :as result]
            [ehrt.tools.sim :as sim])
  (:import [java.io File]))

(defn- temp-dir []
  (let [f (File/createTempFile "sim-test" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

(defn- missing-path []
  (let [f (File/createTempFile "sim-test-missing" "")]
    (.delete f)
    (.getAbsolutePath f)))

(defn- fake-invocation
  [exit-code stdout]
  (fn [{:keys [stdout-path stderr-path]}]
    (spit stdout-path stdout)
    (spit stderr-path "")
    (result/ok {:exit-code exit-code})))

;; ---- discovery order (ruling 4): explicit :sim-dir, then
;; :env-sim-dir-fn, then :default-dir -- all three injectable so this
;; suite never depends on the real machine's own filesystem state ----

(deftest available-with-explicit-sim-dir-test
  (is (true? (sim/available? {:sim-dir (temp-dir)
                               :env-sim-dir-fn (fn [] nil)
                               :default-dir (missing-path)}))))

(deftest available-false-when-nothing-resolves-test
  (is (false? (sim/available? {:sim-dir nil
                                :env-sim-dir-fn (fn [] nil)
                                :default-dir (missing-path)}))))

(deftest available-falls-through-to-env-then-default-test
  (let [env-dir (temp-dir)]
    (is (true? (sim/available? {:sim-dir nil
                                 :env-sim-dir-fn (fn [] env-dir)
                                 :default-dir (missing-path)})))))

(deftest run-not-available-names-all-three-tried-paths-test
  (let [missing (missing-path)
        r (sim/run! {:seed 42
                      :sim-dir nil
                      :env-sim-dir-fn (fn [] nil)
                      :default-dir missing})]
    (is (result/error? r))
    (is (= :sim-not-available (:category r)))
    (is (= missing (:sibling-checkout (:payload r))))
    (is (nil? (:sim-dir (:payload r))))
    (is (nil? (:env-var (:payload r))))))

(deftest run-explicit-sim-dir-wins-over-env-and-default-test
  (let [explicit-dir (temp-dir)
        env-dir (temp-dir)
        captured (atom nil)
        fake (fn [opts] (reset! captured opts) ((fake-invocation 0 (pr-str {:status :ok :payload {}})) opts))]
    (sim/run! {:seed 42 :sim-dir explicit-dir :env-sim-dir-fn (fn [] env-dir)
               :default-dir (missing-path) :run-invocation fake})
    (is (= explicit-dir (:dir @captured)))))

;; ---- the four sim_harness_test.clj cases, unchanged in intent, now
;; exercised directly against this src/ adapter ----

(deftest run-unwraps-ok-payload-test
  (let [fake (fake-invocation 0 (pr-str {:status :ok
                                          :payload {:ground-truth []
                                                    :manifest {:stage :simulated}}}))
        r (sim/run! {:seed 42 :sim-dir (temp-dir) :run-invocation fake})]
    (is (result/ok? r))
    (is (= {:stage :simulated} (:manifest (:payload r))))))

(deftest run-nonzero-exit-is-sim-run-failed-test
  (let [fake (fake-invocation 2 "")
        r (sim/run! {:seed 42 :sim-dir (temp-dir) :run-invocation fake})]
    (is (result/error? r))
    (is (= :sim-run-failed (:category r)))
    (is (= 2 (:exit-code (:payload r))))))

(deftest run-sim-rejected-status-is-sim-run-rejected-test
  (let [fake (fake-invocation 0 (pr-str {:status :rejected :category :whatever :payload {}}))
        r (sim/run! {:seed 42 :sim-dir (temp-dir) :run-invocation fake})]
    (is (result/error? r))
    (is (= :sim-run-rejected (:category r)))))

(deftest run-spawn-failure-passes-through-test
  (let [fake (fn [_] (result/error :spawn-failed {:command "clojure"}))
        r (sim/run! {:seed 42 :sim-dir (temp-dir) :run-invocation fake})]
    (is (result/error? r))
    (is (= :spawn-failed (:category r)))))

(deftest run-config-opt-resolves-to-an-absolute-path-test
  (let [captured (atom nil)
        underlying (fake-invocation 0 (pr-str {:status :ok :payload {}}))
        fake (fn [opts] (reset! captured opts) (underlying opts))]
    (sim/run! {:seed 42 :sim-dir (temp-dir)
               :config "test-integration/fixtures/sim-configs/full-capability.edn"
               :run-invocation fake})
    (let [args (:args @captured)
          config-idx (.indexOf ^java.util.List args "--config")]
      (is (pos? config-idx) "--config reached argv")
      (let [config-arg (nth args (inc config-idx))]
        (is (.isAbsolute (java.io.File. ^String config-arg)))
        (is (str/ends-with? config-arg "full-capability.edn"))))))
