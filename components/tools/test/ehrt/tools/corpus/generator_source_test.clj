(ns ehrt.tools.corpus.generator-source-test
  "Test-first (ruling 3, SS-2 Step 2): written before ehrt.tools.
  corpus.generator-source existed. Hermetic throughout -- every
  registered test entry here is a fake (no real engine, no real
  subprocess); the real :synthea entry's own real-engine path is
  test-integration-tier, per corpus.generators-test's own hermetic
  execute-fn coverage. Covers the three distinct rejections ruling 3
  names by name: engine failure, empty output, and a pre-existing
  out-dir -- plus the two propagated-unchanged rejections from
  ehrt.tools.corpus.generators/resolve-params (unknown kind,
  invalid params)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehrt.tools.result :as result]
            [ehrt.tools.corpus.generators :as generators]
            [ehrt.tools.corpus.generator-source :as generator-source])
  (:import [java.io File]))

(defn- temp-dir-path
  "A fresh, not-yet-existing temp directory path -- unlike generate_
  test.clj's own temp-dir (which mkdirs immediately), the pre-existing-
  out-dir test below needs a path this function has NOT yet created."
  []
  (let [f (File/createTempFile "generator-source-test" "")]
    (.delete f)
    (.getAbsolutePath f)))

(defn- register-fake!
  "Registers a fake generator entry under a fresh, test-local :kind
  (never :synthea -- this suite never touches the real registered
  entry) with the given :out-dir-fn/:execute-fn, defaulting to an
  empty params-schema/default-params."
  [kind {:keys [out-dir-fn execute-fn]}]
  (generators/register! {:kind kind
                          :default-params {}
                          :params-schema [:map]
                          :out-dir-fn out-dir-fn
                          :execute-fn execute-fn}))

(deftest resolve-happy-path-returns-a-dir-source-test
  (let [out-dir (temp-dir-path)]
    (register-fake! :fake-gen-happy
                     {:out-dir-fn (fn [_] out-dir)
                      :execute-fn (fn [_ dir]
                                    (.mkdirs (io/file dir))
                                    (spit (io/file dir "message.hl7") "MSH|...")
                                    (result/ok {:out-dir dir}))})
    (let [r (generator-source/resolve! :fake-gen-happy {})]
      (is (result/ok? r))
      (is (= {:kind :dir :path out-dir} (:payload r))))))

(deftest resolve-unknown-kind-propagates-test
  (let [r (generator-source/resolve! :no-such-generator-kind {})]
    (is (result/rejected? r))
    (is (= :unknown-generator-kind (:category r)))))

(deftest resolve-invalid-params-propagates-test
  (generators/register! {:kind :fake-gen-strict-params
                          :default-params {}
                          :params-schema [:map [:seed :int]]
                          :out-dir-fn (fn [_] (temp-dir-path))
                          :execute-fn (fn [_ dir] (result/ok {:out-dir dir}))})
  (let [r (generator-source/resolve! :fake-gen-strict-params {:seed "not-an-int"})]
    (is (result/rejected? r))
    (is (= :invalid-generator-params (:category r)))))

(deftest resolve-pre-existing-out-dir-is-rejected-before-executing-test
  (testing "a non-empty out-dir is rejected up front; execute-fn is never called"
    (let [out-dir (temp-dir-path)
          _ (.mkdirs (io/file out-dir))
          _ (spit (io/file out-dir "leftover.txt") "from a previous run")
          executed? (atom false)]
      (register-fake! :fake-gen-collision
                       {:out-dir-fn (fn [_] out-dir)
                        :execute-fn (fn [_ dir]
                                      (reset! executed? true)
                                      (result/ok {:out-dir dir}))})
      (let [r (generator-source/resolve! :fake-gen-collision {})]
        (is (result/error? r))
        (is (= :out-dir-exists (:category r)))
        (is (false? @executed?))))))

(deftest resolve-engine-failure-propagates-unchanged-test
  (register-fake! :fake-gen-engine-failure
                   {:out-dir-fn (fn [_] (temp-dir-path))
                    :execute-fn (fn [_ _] (result/error :some-engine-failure {:detail "boom"}))})
  (let [r (generator-source/resolve! :fake-gen-engine-failure {})]
    (is (result/error? r))
    (is (= :some-engine-failure (:category r)))
    (is (= "boom" (:detail (:payload r))))))

(deftest resolve-empty-output-is-its-own-rejection-test
  (testing "execute-fn returns ok but writes nothing -- caught, not silently accepted"
    (let [out-dir (temp-dir-path)]
      (register-fake! :fake-gen-empty-output
                       {:out-dir-fn (fn [_] out-dir)
                        :execute-fn (fn [_ dir]
                                      (.mkdirs (io/file dir))
                                      (result/ok {:out-dir dir}))})
      (let [r (generator-source/resolve! :fake-gen-empty-output {})]
        (is (result/error? r))
        (is (= :generator-produced-no-output (:category r)))))))
