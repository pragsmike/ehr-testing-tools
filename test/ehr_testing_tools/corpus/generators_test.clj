(ns ehr-testing-tools.corpus.generators-test
  "Test-first (ruling 2, SS-2 Step 1): written before ehr-testing-tools.
  corpus.generators existed. Registry mechanics (shaped like
  corpus.operators's own test), then the :synthea seed entry: its
  pinned defaults are asserted equal to corpus.generate's OWN default-*
  vars (never re-derived copies that could drift), and its
  :execute-fn is proven to BE corpus.generate/generate! -- hermetically,
  via the same injected-fake shape generate_test.clj already uses, no
  real subprocess or network."
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.corpus.generate :as generate]
            [ehr-testing-tools.corpus.generators :as generators])
  (:import [java.io File]))

(defn- temp-dir []
  (let [f (File/createTempFile "generators-test" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

;; ---- registry mechanics (same shape as corpus.operators-test) ----

(deftest register-and-lookup-test
  (let [r (generators/register! {:kind :test-gen
                                  :default-params {}
                                  :params-schema [:map]
                                  :out-dir-fn (fn [_] "target/x")
                                  :execute-fn (fn [_ _] (result/ok {}))})]
    (is (result/ok? r))
    (is (some? (generators/lookup :test-gen)))
    (is (nil? (generators/lookup :nope)))))

(deftest register-rejects-invalid-entry-test
  (let [r (generators/register! {:kind :bad})]
    (is (result/rejected? r))
    (is (= :invalid-generator-entry (:category r)))))

(deftest entries-lists-all-registered-test
  (generators/register! {:kind :another-test-gen
                          :default-params {} :params-schema [:map]
                          :out-dir-fn (fn [_] "target/y")
                          :execute-fn (fn [_ _] (result/ok {}))})
  (is (some #(= :another-test-gen (:kind %)) (generators/entries))))

;; ---- resolve-params ----

(deftest resolve-params-unknown-kind-test
  (let [r (generators/resolve-params :no-such-kind {})]
    (is (result/rejected? r))
    (is (= :unknown-generator-kind (:category r)))))

(deftest resolve-params-merges-given-onto-defaults-test
  (generators/register! {:kind :merge-test-gen
                          :default-params {:a 1 :b 2}
                          :params-schema [:map [:a :int] [:b :int]]
                          :out-dir-fn (fn [_] "target/z")
                          :execute-fn (fn [_ _] (result/ok {}))})
  (let [r (generators/resolve-params :merge-test-gen {:b 20})]
    (is (result/ok? r))
    (is (= {:a 1 :b 20} (:payload r)))))

(deftest resolve-params-invalid-merged-params-rejected-test
  (generators/register! {:kind :invalid-merge-test-gen
                          :default-params {:a 1}
                          :params-schema [:map [:a :int]]
                          :out-dir-fn (fn [_] "target/w")
                          :execute-fn (fn [_ _] (result/ok {}))})
  (let [r (generators/resolve-params :invalid-merge-test-gen {:a "not-an-int"})]
    (is (result/rejected? r))
    (is (= :invalid-generator-params (:category r)))))

;; ---- :synthea seed entry (D7): re-expresses corpus.generate's own
;; engine, not a re-implementation. Its pinned defaults must be the
;; SAME values corpus.generate's own zero-flag defaults already use --
;; imported vars, not re-typed constants that could silently drift. ----

(deftest synthea-registered-test
  (is (some? (generators/lookup :synthea))))

(deftest synthea-default-params-match-generates-own-defaults-test
  (testing "D9: the zero-param synthea: URL must mean exactly what zero-flag `ehr corpus generate` means"
    (let [r (generators/resolve-params :synthea {})]
      (is (result/ok? r))
      (is (= {:seed generate/default-seed
              :population generate/default-population
              :reference-date generate/default-reference-date
              :config-path generate/default-config-path}
             (:payload r))))))

(deftest synthea-resolve-params-given-seed-overrides-default-test
  (let [r (generators/resolve-params :synthea {:seed 42})]
    (is (result/ok? r))
    (is (= 42 (:seed (:payload r))))
    (is (= generate/default-population (:population (:payload r))))))

(deftest synthea-out-dir-fn-matches-generates-own-derivation-test
  (let [entry (generators/lookup :synthea)]
    (is (= (generate/default-out-dir 7 3) ((:out-dir-fn entry) {:seed 7 :population 3})))))

;; ---- :synthea execute-fn IS corpus.generate/generate! -- hermetic,
;; injected fakes, no real subprocess/network (same shape generate_
;; test.clj's own stub-deps uses). ----

(def ^:private synthea-artifact
  {:kind :engine :name "synthea" :version "4.0.0"
   :sha256 (apply str (repeat 64 "c"))
   :source "https://example.invalid/synthea.jar"
   :acquired "2026-07-24" :license-status :verified})

(defn- ok-invocation []
  (result/ok {:command "java" :args ["-jar" "/fake/synthea.jar"]
              :exit-code 0 :duration-ms 1 :started-at "2026-07-24T00:00:00Z"
              :stdout-path "/fake/out.log" :stderr-path "/fake/err.log"
              :stdout-sha256 (apply str (repeat 64 "0"))
              :stderr-sha256 (apply str (repeat 64 "0"))}))

(deftest synthea-execute-fn-runs-generate-bang-hermetically-test
  (let [out-dir (temp-dir)
        config-file (File/createTempFile "synthea" ".properties")
        _ (spit config-file "generate.thread_pool_size = 1\n")
        entry (generators/lookup :synthea)
        params-result (generators/resolve-params
                       :synthea
                       {:config-path (.getAbsolutePath config-file)
                        :seed 1 :population 1
                        :read-lockfile (fn [_] (result/ok {:artifacts [synthea-artifact]}))
                        :resolve-artifact (fn [_ _ _] (result/ok {:path "/fake/synthea.jar" :artifact synthea-artifact}))
                        :resolve-java-bin (fn [_ _] (result/ok {:path "/stub/java" :artifact nil}))
                        :run-invocation (fn [_] (ok-invocation))})]
    (is (result/ok? params-result))
    (let [r ((:execute-fn entry) (:payload params-result) out-dir)]
      (is (result/ok? r))
      (is (= out-dir (:out-dir (:payload r)))))))
