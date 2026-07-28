(ns ehrt.tools.corpus.generators-test
  "Test-first (ruling 2, SS-2 Step 1; :sim entry added Step 3).
  Registry mechanics (shaped like corpus.operators's own test), then
  the :synthea seed entry: its pinned defaults are asserted equal to
  corpus.generate's OWN default-* vars (never re-derived copies that
  could drift), and its :execute-fn is proven to BE corpus.generate/
  generate! -- hermetically, via the same injected-fake shape
  generate_test.clj already uses, no real subprocess or network. The
  :sim entry (Step 3) is proven the same way, via ehrt.tools.
  sim/run!'s own injectable :run-invocation, plus an explicit :sim-dir
  so discovery succeeds without touching the real machine's own
  filesystem state."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [ehrt.tools.result :as result]
            [ehrt.tools.corpus.generate :as generate]
            [ehrt.tools.corpus.generators :as generators])
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

;; ---- :sim seed entry (Step 3): drives ehrt.tools.sim/run!,
;; then spools its own :messages/:manifest into out-dir -- tools
;; writes no manifest of its own for sim output (ruling 4: sim's own
;; ManifestV1_1-shaped payload IS the manifest). Hermetic throughout:
;; an injected :run-invocation fake, never a real subprocess, and an
;; explicit :sim-dir so discovery succeeds without depending on the
;; real machine's own filesystem state. ----

(defn- fake-sim-invocation
  [stdout]
  (fn [{:keys [stdout-path stderr-path]}]
    (spit stdout-path stdout)
    (spit stderr-path "")
    (result/ok {:exit-code 0})))

(deftest sim-registered-test
  (is (some? (generators/lookup :sim))))

(deftest sim-default-params-test
  (let [r (generators/resolve-params :sim {})]
    (is (result/ok? r))
    (is (= {:seed generate/default-seed :patients 1 :emit "hl7"} (:payload r)))))

(deftest sim-out-dir-fn-test
  (let [entry (generators/lookup :sim)]
    (is (= "target/corpus/sim-s7-p3" ((:out-dir-fn entry) {:seed 7 :patients 3})))))

(deftest sim-execute-fn-happy-path-spools-messages-and-manifest-test
  (let [out-dir (temp-dir)
        entry (generators/lookup :sim)
        sim-payload {:ground-truth [] :manifest {:stage :simulated} :messages ["MSH|1" "MSH|2"]}
        fake (fake-sim-invocation (pr-str {:status :ok :payload sim-payload}))
        params-result (generators/resolve-params :sim {:sim-dir (temp-dir) :run-invocation fake})]
    (is (result/ok? params-result))
    (let [r ((:execute-fn entry) (:payload params-result) out-dir)]
      (is (result/ok? r))
      (let [files (.listFiles (io/file out-dir))]
        (is (= 3 (count files)) "two message files plus manifest.edn")
        (is (some #(= "manifest.edn" (.getName %)) files))
        (is (= {:stage :simulated} (edn/read-string (slurp (io/file out-dir "manifest.edn")))))))))

(deftest sim-execute-fn-no-messages-is-its-own-rejection-test
  (let [out-dir (temp-dir)
        entry (generators/lookup :sim)
        sim-payload {:ground-truth [] :manifest {:stage :simulated} :messages []}
        fake (fake-sim-invocation (pr-str {:status :ok :payload sim-payload}))
        params-result (generators/resolve-params :sim {:sim-dir (temp-dir) :run-invocation fake})]
    (is (result/ok? params-result))
    (let [r ((:execute-fn entry) (:payload params-result) out-dir)]
      (is (result/error? r))
      (is (= :sim-produced-no-messages (:category r))))))

(deftest sim-execute-fn-propagates-sim-run-failures-unchanged-test
  (let [out-dir (temp-dir)
        entry (generators/lookup :sim)
        missing (let [f (File/createTempFile "generators-test-missing" "")]
                  (.delete f)
                  (.getAbsolutePath f))
        params-result (generators/resolve-params
                       :sim {:sim-dir nil :env-sim-dir-fn (fn [] nil) :default-dir missing})]
    (is (result/ok? params-result))
    (let [r ((:execute-fn entry) (:payload params-result) out-dir)]
      (is (result/error? r))
      (is (= :sim-not-available (:category r))))))
