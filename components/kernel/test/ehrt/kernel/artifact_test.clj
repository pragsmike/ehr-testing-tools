(ns ehrt.kernel.artifact-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [malli.core :as malli]
            [ehrt.kernel.result :as result]
            [ehrt.kernel.digest :as digest]
            [ehrt.kernel.artifact :as artifact])
  (:import [java.io File]))

(defn- temp-dir []
  (let [f (File/createTempFile "artifact-cache" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

(defn- sample-artifact [content]
  {:kind :engine
   :name "sample"
   :version "1.0.0"
   :sha256 (digest/sha256-string content)
   :source "https://example.invalid/sample.jar"
   :acquired "2026-07-24"
   :license-status :verified})

;; ---- cache-dir ----

(deftest cache-dir-honors-explicit-override-test
  (is (= "/tmp/explicit-cache" (artifact/cache-dir "/tmp/explicit-cache"))))

(deftest cache-dir-honors-env-override-test
  (with-redefs [artifact/env-override (fn [] "/tmp/env-cache")]
    (is (= "/tmp/env-cache" (artifact/cache-dir)))))

(deftest cache-dir-defaults-under-home-test
  (with-redefs [artifact/env-override (fn [] nil)]
    (is (= (str (System/getProperty "user.home") "/.cache/ehr-testing-tools/artifacts")
           (artifact/cache-dir)))))

;; ---- read-lockfile ----

(deftest read-lockfile-valid-test
  (let [f (File/createTempFile "lockfile" ".edn")
        art (sample-artifact "abc")]
    (spit f (pr-str {:artifacts [art]}))
    (let [r (artifact/read-lockfile (.getAbsolutePath f))]
      (is (result/ok? r))
      (is (= [art] (:artifacts (:payload r)))))))

(deftest read-lockfile-parse-failure-test
  (let [f (File/createTempFile "lockfile" ".edn")]
    (spit f "{:artifacts [not valid edn")
    (let [r (artifact/read-lockfile (.getAbsolutePath f))]
      (is (result/error? r))
      (is (= :parse-failed (:category r))))))

(deftest read-lockfile-schema-violation-test
  (let [f (File/createTempFile "lockfile" ".edn")]
    (spit f (pr-str {:artifacts [{:kind :engine :name "x"}]})) ; missing required fields
    (let [r (artifact/read-lockfile (.getAbsolutePath f))]
      (is (result/error? r))
      (is (= :invalid-lockfile (:category r))))))

(deftest read-lockfile-not-found-test
  (let [r (artifact/read-lockfile "/no/such/path/artifacts.lock.edn")]
    (is (result/error? r))
    (is (= :not-found (:category r)))))

;; ---- fetch ----

(deftest fetch-cache-hit-short-circuits-no-network-test
  (let [dir (temp-dir)
        art (sample-artifact "cached content")
        dest (io/file dir (:sha256 art))]
    (spit dest "cached content")
    (let [downloader (fn [_source _dest] (throw (ex-info "network must not be called" {})))
          r (artifact/fetch art {:cache-dir-override dir :downloader downloader})]
      (is (result/ok? r))
      (is (true? (:cached (:payload r)))))))

(deftest fetch-downloads-and-verifies-on-miss-test
  (let [dir (temp-dir)
        art (sample-artifact "fresh content")
        downloader (fn [_source dest-path] (spit dest-path "fresh content"))
        r (artifact/fetch art {:cache-dir-override dir :downloader downloader})]
    (is (result/ok? r))
    (is (false? (:cached (:payload r))))
    (is (.exists (io/file dir (:sha256 art))))
    (is (= "fresh content" (slurp (io/file dir (:sha256 art)))))))

(deftest fetch-rejects-hash-mismatch-and-leaves-no-file-test
  (let [dir (temp-dir)
        art (sample-artifact "expected content")
        downloader (fn [_source dest-path] (spit dest-path "WRONG BYTES"))
        r (artifact/fetch art {:cache-dir-override dir :downloader downloader})]
    (is (result/rejected? r))
    (is (= :hash-mismatch (:category r)))
    (is (not (.exists (io/file dir (:sha256 art)))))))

(deftest fetch-hash-mismatch-property-test
  ;; For any downloaded content that doesn't match the claimed sha256,
  ;; fetch always rejects -- never silently accepts wrong bytes.
  (let [dir (temp-dir)
        art (sample-artifact "the one true content")
        prop-holds
        (prop/for-all [garbage (gen/such-that #(not= % "the one true content") gen/string-ascii)]
          (let [downloader (fn [_source dest-path] (spit dest-path garbage))
                r (artifact/fetch art {:cache-dir-override dir :downloader downloader})]
            (result/rejected? r)))]
    (is (:pass? (tc/quick-check 50 prop-holds)))))

(deftest fetch-download-failure-test
  (let [dir (temp-dir)
        art (sample-artifact "content")
        downloader (fn [_source _dest] (throw (ex-info "boom" {})))
        r (artifact/fetch art {:cache-dir-override dir :downloader downloader})]
    (is (result/error? r))
    (is (= :download-failed (:category r)))))

;; ---- resolve ----

(deftest resolve-unknown-artifact-test
  (let [r (artifact/resolve [] "nope" "1.0.0" {})]
    (is (result/rejected? r))
    (is (= :unknown-artifact (:category r)))))

(deftest resolve-known-but-not-yet-fetched-test
  (let [dir (temp-dir)
        art (sample-artifact "not fetched yet")
        r (artifact/resolve [art] "sample" "1.0.0" {:cache-dir-override dir})]
    (is (result/rejected? r))
    (is (= :not-cached (:category r)))))

(deftest resolve-known-and-cached-test
  (let [dir (temp-dir)
        art (sample-artifact "already fetched")
        _ (spit (io/file dir (:sha256 art)) "already fetched")
        r (artifact/resolve [art] "sample" "1.0.0" {:cache-dir-override dir})]
    (is (result/ok? r))
    (is (= (.getAbsolutePath (io/file dir (:sha256 art))) (:path (:payload r))))))

;; ---- :runtime kind (P4: JVM as a lockfile artifact) ----

(deftest runtime-is-a-valid-artifact-kind-test
  ;; A JVM distribution is an acquired, external, binary input exactly
  ;; like the Synthea engine jar -- it belongs in the same registry,
  ;; under a kind that says what it is (a runtime, not an :engine).
  (let [art (assoc (sample-artifact "jdk bytes") :kind :runtime)]
    (is (malli.core/validate artifact/Artifact art))))

;; ---- resolve-and-extract (archives, not single files, need
;; unpacking before their contents -- e.g. a JVM's bin/java -- are
;; reachable; extraction target is content-addressed by the archive's
;; own sha256, so it's a derived, idempotent side effect of a
;; verified cache hit, not new untracked state) ----

(deftest extracted-dir-is-content-addressed-test
  (is (clojure.string/ends-with? (artifact/extracted-dir "abc123" "/cache")
                                  "/cache/extracted/abc123")))

(deftest resolve-and-extract-extracts-on-first-resolve-test
  (let [dir (temp-dir)
        art (assoc (sample-artifact "archive bytes") :kind :runtime)
        _ (spit (io/file dir (:sha256 art)) "archive bytes")
        extract-calls (atom [])
        extractor (fn [archive-path dest-dir]
                    (swap! extract-calls conj [archive-path dest-dir])
                    (.mkdirs (io/file dest-dir "jdk-1.0.0" "bin"))
                    (spit (io/file dest-dir "jdk-1.0.0" "bin" "java") "#!fake")
                    (result/ok {:dest dest-dir}))
        r (artifact/resolve-and-extract [art] "sample" "1.0.0"
                                         {:cache-dir-override dir :extractor extractor})]
    (is (result/ok? r))
    (is (= 1 (count @extract-calls)))
    (is (clojure.string/ends-with? (:extracted-dir (:payload r)) (str "extracted/" (:sha256 art))))))

(deftest resolve-and-extract-skips-extraction-when-already-extracted-test
  (let [dir (temp-dir)
        art (assoc (sample-artifact "archive bytes") :kind :runtime)
        _ (spit (io/file dir (:sha256 art)) "archive bytes")
        dest (io/file (artifact/extracted-dir (:sha256 art) dir) "jdk-1.0.0" "bin")
        _ (.mkdirs dest)
        _ (spit (io/file dest "java") "#!fake")
        extract-calls (atom 0)
        extractor (fn [_archive-path _dest-dir] (swap! extract-calls inc) (result/ok {}))
        r (artifact/resolve-and-extract [art] "sample" "1.0.0"
                                         {:cache-dir-override dir :extractor extractor})]
    (is (result/ok? r))
    (is (zero? @extract-calls) "already-extracted, non-empty dest-dir must not re-extract")))

(deftest resolve-and-extract-propagates-resolve-rejection-test
  (let [r (artifact/resolve-and-extract [] "nope" "1.0.0" {})]
    (is (result/rejected? r))
    (is (= :unknown-artifact (:category r)))))

(deftest resolve-and-extract-propagates-extractor-failure-test
  (let [dir (temp-dir)
        art (assoc (sample-artifact "archive bytes") :kind :runtime)
        _ (spit (io/file dir (:sha256 art)) "archive bytes")
        extractor (fn [_archive-path _dest-dir] (result/error :extract-failed {:message "tar exploded"}))
        r (artifact/resolve-and-extract [art] "sample" "1.0.0"
                                         {:cache-dir-override dir :extractor extractor})]
    (is (result/error? r))
    (is (= :extract-failed (:category r)))))

(deftest find-executable-locates-file-at-any-depth-test
  (let [dir (temp-dir)
        _ (.mkdirs (io/file dir "jdk-17.0.19+10" "bin"))
        java-file (io/file dir "jdk-17.0.19+10" "bin" "java")
        _ (spit java-file "#!fake")
        r (artifact/find-executable dir "bin/java")]
    (is (result/ok? r))
    (is (= (.getAbsolutePath java-file) (:path (:payload r))))))

(deftest find-executable-rejects-when-not-found-test
  (let [dir (temp-dir)
        r (artifact/find-executable dir "bin/java")]
    (is (result/rejected? r))
    (is (= :executable-not-found (:category r)))))
