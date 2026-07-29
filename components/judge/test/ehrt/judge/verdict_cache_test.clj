(ns ehrt.judge.verdict-cache-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [ehrt.judge.verdict-cache :as verdict-cache])
  (:import [java.io File]))

(defn- temp-dir []
  (let [f (File/createTempFile "verdict-cache-test" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

(def ^:private base-key-args
  {:content-sha256 (apply str (repeat 64 "a"))
   :validator-artifact {:name "fhir-validator-cli" :version "6.9.12"
                        :sha256 (apply str (repeat 64 "b"))}
   :ig-artifacts []
   :argv-shape ["-version" "4.0" "-tx" "n/a"]
   :verdict-mapping-version "v2"})

;; ---- cache-key: sensitivity to every component (session ruling 3 --
;; any omission that could alias two distinct judgments is a
;; correctness bug, not a tuning knob) ----

(deftest cache-key-is-deterministic-for-identical-inputs-test
  (is (= (verdict-cache/cache-key base-key-args)
         (verdict-cache/cache-key base-key-args))))

(deftest cache-key-differs-on-content-hash-test
  (is (not= (verdict-cache/cache-key base-key-args)
            (verdict-cache/cache-key (assoc base-key-args :content-sha256 (apply str (repeat 64 "z")))))))

(deftest cache-key-differs-on-validator-name-test
  (is (not= (verdict-cache/cache-key base-key-args)
            (verdict-cache/cache-key (assoc-in base-key-args [:validator-artifact :name] "other-validator")))))

(deftest cache-key-differs-on-validator-version-test
  (is (not= (verdict-cache/cache-key base-key-args)
            (verdict-cache/cache-key (assoc-in base-key-args [:validator-artifact :version] "9.9.9")))))

(deftest cache-key-differs-on-validator-sha256-test
  ;; The same claimed name+version with a DIFFERENT sha256 is exactly
  ;; the "a claim vs. a fact" distinction ADR-0005/digest.clj's own
  ;; docstring draws -- the key must not trust the claim alone.
  (is (not= (verdict-cache/cache-key base-key-args)
            (verdict-cache/cache-key (assoc-in base-key-args [:validator-artifact :sha256] (apply str (repeat 64 "9")))))))

(deftest cache-key-differs-on-ig-artifacts-test
  (is (not= (verdict-cache/cache-key base-key-args)
            (verdict-cache/cache-key (assoc base-key-args :ig-artifacts
                                             [{:name "us-core" :version "6.1.0" :sha256 (apply str (repeat 64 "e"))}])))))

(deftest cache-key-differs-on-argv-shape-test
  (is (not= (verdict-cache/cache-key base-key-args)
            (verdict-cache/cache-key (assoc base-key-args :argv-shape ["-version" "4.0" "-tx" "n/a" "-ig" "us-core@6.1.0"])))))

(deftest cache-key-differs-on-verdict-mapping-version-test
  (is (not= (verdict-cache/cache-key base-key-args)
            (verdict-cache/cache-key (assoc base-key-args :verdict-mapping-version "v3")))))

(deftest cache-key-ignores-extraneous-artifact-fields-test
  ;; Only :name/:version/:sha256 are load-bearing for identity -- an
  ;; artifact record's other fields (:source, :acquired, :license-
  ;; status, ...) changing must not spuriously invalidate every cached
  ;; verdict.
  (is (= (verdict-cache/cache-key base-key-args)
         (verdict-cache/cache-key (assoc-in base-key-args [:validator-artifact :source] "https://example.invalid/new-mirror")))))

;; ---- lookup/store: hit/miss behavior ----

(deftest lookup-on-empty-cache-dir-is-a-miss-test
  (is (nil? (verdict-cache/lookup (temp-dir) "deadbeef"))))

(deftest store-then-lookup-is-a-hit-test
  (let [dir (temp-dir)
        key (verdict-cache/cache-key base-key-args)
        value {:verdict :pass :findings []}]
    (verdict-cache/store! dir key value)
    (is (= value (verdict-cache/lookup dir key)))))

(deftest lookup-a-different-key-after-a-store-is-still-a-miss-test
  (let [dir (temp-dir)]
    (verdict-cache/store! dir (verdict-cache/cache-key base-key-args) {:verdict :pass :findings []})
    (is (nil? (verdict-cache/lookup dir "0000000000000000000000000000000000000000000000000000000000000000")))))

(deftest lookup-a-corrupt-cache-entry-degrades-to-a-miss-not-a-crash-test
  (let [dir (temp-dir)
        key "corrupt-entry"]
    (io/make-parents (io/file dir (str key ".edn")))
    (spit (io/file dir (str key ".edn")) "{:verdict :pass, :findings [")
    (is (nil? (verdict-cache/lookup dir key)))))

(deftest store-round-trips-a-no-verdict-outcome-with-cause-test
  (let [dir (temp-dir)
        key (verdict-cache/cache-key base-key-args)
        value {:verdict :no-verdict :cause :terminology-suppressed
               :findings [{:severity :warning :code "code-invalid"
                           :locator {:format :fhir :path "entry[0].resource.gender"}
                           :message "..." :engine {:name "fhir-validator-cli" :version "6.9.12"}
                           :disposition :no-verdict :cause :terminology-suppressed}]}]
    (verdict-cache/store! dir key value)
    (is (= value (verdict-cache/lookup dir key)))))
