(ns ehr-testing-tools.corpus.generate-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.corpus.generate :as generate])
  (:import [java.io File]))

(defn- temp-dir []
  (let [f (File/createTempFile "generate-test" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

(def synthea-artifact
  {:kind :engine :name "synthea" :version "4.0.0"
   :sha256 (apply str (repeat 64 "c"))
   :source "https://example.invalid/synthea.jar"
   :acquired "2026-07-24" :license-status :verified})

(defn- stub-deps
  "Fake read-lockfile/resolve-artifact/run-invocation for fast, hermetic
  tests -- no real jar, no real JVM subprocess."
  [{:keys [lockfile-result resolve-result invocation-result invocation-args-atom]}]
  {:read-lockfile (fn [_path] lockfile-result)
   :resolve-artifact (fn [_artifacts _name _version] resolve-result)
   :run-invocation (fn [invocation-opts]
                     (when invocation-args-atom (reset! invocation-args-atom invocation-opts))
                     invocation-result)})

(defn- ok-lockfile [] (result/ok {:artifacts [synthea-artifact]}))
(defn- ok-resolve [] (result/ok {:path "/fake/synthea.jar" :artifact synthea-artifact}))
(defn- ok-invocation []
  (result/ok {:command "java" :args ["-jar" "/fake/synthea.jar"]
              :exit-code 0 :duration-ms 42 :started-at "2026-07-24T00:00:00Z"
              :stdout-path "/fake/out.log" :stderr-path "/fake/err.log"
              :stdout-sha256 (apply str (repeat 64 "0"))
              :stderr-sha256 (apply str (repeat 64 "0"))}))

(deftest generate-happy-path-writes-manifest-test
  (let [out-dir (temp-dir)
        config-file (File/createTempFile "synthea" ".properties")
        _ (spit config-file "generate.thread_pool_size = 1\n")
        args-atom (atom nil)
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)
                          :invocation-args-atom args-atom})
        r (generate/generate! (merge deps
                                      {:config-path (.getAbsolutePath config-file)
                                       :seed 42
                                       :population 10
                                       :reference-date "20260101"
                                       :output-dir out-dir}))]
    (is (result/ok? r))
    (let [manifest (:manifest (:payload r))]
      (is (= 0 (:schema-version manifest)))
      (is (= 42 (:seed manifest)))
      (is (= "synthea" (:name (:generator manifest))))
      (is (= "4.0.0" (:version (:generator manifest))))
      (is (= (:sha256 synthea-artifact) (:sha256 (:generator manifest))))
      (is (= (.getAbsolutePath config-file) (:path (:config manifest))))
      (is (string? (:sha256 (:config manifest))))
      (is (= [] (:canonicalizers-applied manifest)))
      (is (contains? (:environment manifest) :locale))
      (is (contains? (:environment manifest) :timezone))
      (is (contains? (:environment manifest) :jvm-version)))
    ;; manifest.edn written alongside the (would-be) generated tree
    (let [written (edn/read-string (slurp (io/file out-dir "manifest.edn")))]
      (is (= (:manifest (:payload r)) written)))
    ;; invocation was constructed correctly: jar path, seed, population,
    ;; config path, and output directory all land in the args.
    (let [invoked @args-atom
          arg-str (clojure.string/join " " (:args invoked))]
      (is (clojure.string/includes? arg-str "/fake/synthea.jar"))
      (is (clojure.string/includes? arg-str "-s 42"))
      (is (clojure.string/includes? arg-str "-p 10"))
      (is (clojure.string/includes? arg-str (str "-c " (.getAbsolutePath config-file))))
      (is (clojure.string/includes? arg-str "-r 20260101"))
      (is (clojure.string/includes? arg-str (str "--exporter.baseDirectory=" out-dir))))))

(deftest generate-propagates-lockfile-read-failure-test
  (let [deps (stub-deps {:lockfile-result (result/error :not-found {:path "artifacts.lock.edn"})
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)})
        r (generate/generate! (merge deps {:config-path "x" :seed 1 :population 1
                                            :output-dir (temp-dir)}))]
    (is (result/error? r))
    (is (= :not-found (:category r)))))

(deftest generate-propagates-resolve-failure-without-fetching-test
  ;; generate! never auto-fetches -- if the artifact isn't already
  ;; resolvable, that's the caller's job (`ehr artifact fetch` first).
  (let [deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (result/rejected :not-cached {:name "synthea" :version "4.0.0"})
                          :invocation-result (ok-invocation)})
        r (generate/generate! (merge deps {:config-path "x" :seed 1 :population 1
                                            :output-dir (temp-dir)}))]
    (is (result/rejected? r))
    (is (= :not-cached (:category r)))))

(deftest generate-propagates-invocation-failure-test
  (let [deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (result/error :spawn-failed {:message "no java"})})
        r (generate/generate! (merge deps {:config-path "x" :seed 1 :population 1
                                            :output-dir (temp-dir)}))]
    (is (result/error? r))
    (is (= :spawn-failed (:category r)))))

(deftest generate-creates-output-dir-if-missing-test
  (let [parent (temp-dir)
        out-dir (str parent "/nested/does/not/exist/yet")
        config-file (File/createTempFile "synthea" ".properties")
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)})
        r (generate/generate! (merge deps {:config-path (.getAbsolutePath config-file)
                                            :seed 1 :population 1 :reference-date "20260101"
                                            :output-dir out-dir}))]
    (is (result/ok? r))
    (is (.isDirectory (io/file out-dir)))))
