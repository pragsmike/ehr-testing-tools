(ns ehrt.corpus.manifest-test
  (:require [clojure.test :refer [deftest is]]
            [ehrt.corpus.manifest :as manifest]))

(def sample-fields
  {:generator {:name "synthea" :version "4.0.0" :sha256 (apply str (repeat 64 "a"))}
   :seed 42
   :clinician-seed 43
   :config {:path "config/synthea/synthea.properties" :sha256 (apply str (repeat 64 "b"))}
   :invocation {:command "java" :args ["-jar" "synthea.jar"] :exit-code 0}
   :canonicalizers-applied [[:strip-timestamps "1"]]
   :environment {:locale "en-US" :timezone "UTC" :jvm-version "21"}})

(deftest build-produces-schema-version-0-test
  (let [m (manifest/build sample-fields)]
    (is (= 0 (:schema-version m)))
    (is (= 43 (:clinician-seed m)))
    (is (manifest/valid? m))))

(deftest build-defaults-canonicalizers-applied-to-empty-test
  (let [m (manifest/build (dissoc sample-fields :canonicalizers-applied))]
    (is (= [] (:canonicalizers-applied m)))
    (is (manifest/valid? m))))

(deftest valid-rejects-missing-required-fields-test
  (is (not (manifest/valid? (dissoc (manifest/build sample-fields) :seed))))
  (is (not (manifest/valid? (dissoc (manifest/build sample-fields) :clinician-seed))))
  (is (not (manifest/valid? (dissoc (manifest/build sample-fields) :generator))))
  (is (not (manifest/valid? {:schema-version 0}))))

(deftest valid-rejects-wrong-schema-version-test
  (is (not (manifest/valid? (assoc (manifest/build sample-fields) :schema-version 1)))))

;; ---- schema v1 (EXP-A4's upgrade: adds :reference-date, the one
;; pinned-input field the v0 hypothesis omitted as an explicit,
;; top-level field -- it was only recoverable indirectly via the
;; embedded invocation args) ----

(def sample-fields-v1
  (assoc sample-fields :reference-date "20260101"))

(deftest build-v1-produces-schema-version-1-test
  (let [m (manifest/build-v1 sample-fields-v1)]
    (is (= 1 (:schema-version m)))
    (is (= "20260101" (:reference-date m)))
    (is (= 43 (:clinician-seed m)))
    (is (manifest/valid-v1? m))))

(deftest valid-v1-rejects-missing-reference-date-test
  (is (not (manifest/valid-v1? (dissoc (manifest/build-v1 sample-fields-v1) :reference-date)))))

(deftest valid-v1-rejects-schema-version-0-test
  (is (not (manifest/valid-v1? (assoc (manifest/build-v1 sample-fields-v1) :schema-version 0)))))

(deftest v0-manifest-is-not-a-valid-v1-manifest-test
  ;; The schemas are genuinely distinct -- a v0 manifest (no
  ;; reference-date, schema-version 0) must not pass as v1.
  (is (not (manifest/valid-v1? (manifest/build sample-fields)))))

;; ---- schema v1.1 (P4: :stage, :seeds map, :engine-params map,
;; :runtime -- engine-shaped fields (seed, clinician-seed,
;; reference-date) leave the fixed top level; :schema-version becomes
;; the string "1.1", not the integer 2 -- this is an additive minor
;; revision, not a breaking rewrite, and the string says so directly
;; rather than implying a v2 that doesn't exist) ----

(def sample-fields-v1-1
  {:stage :generate
   :generator {:name "synthea" :version "4.0.0" :sha256 (apply str (repeat 64 "a"))}
   :runtime {:name "temurin-jdk" :version "17.0.19+10" :sha256 (apply str (repeat 64 "d"))}
   :seeds {:master 100 :clinician 555}
   :engine-params {:reference-date "20260101"}
   :config {:path "config/synthea/synthea.properties" :sha256 (apply str (repeat 64 "b"))}
   :invocation {:command "java" :args ["-jar" "synthea.jar"] :exit-code 0}
   :canonicalizers-applied [[:strip-timestamps "1"]]
   :environment {:locale "en-US" :timezone "UTC" :jvm-version "17.0.19"}})

(deftest build-v1-1-produces-schema-version-string-test
  (let [m (manifest/build-v1-1 sample-fields-v1-1)]
    (is (= "1.1" (:schema-version m)))
    (is (= :generate (:stage m)))
    (is (= {:master 100 :clinician 555} (:seeds m)))
    (is (= {:reference-date "20260101"} (:engine-params m)))
    (is (= "temurin-jdk" (:name (:runtime m))))
    (is (manifest/valid-v1-1? m))))

(deftest build-v1-1-runtime-is-optional-test
  ;; corpus.generate's :java-bin escape hatch (explicit override,
  ;; bypassing registry resolution) has no artifact to report -- the
  ;; manifest must not fabricate one; :runtime is simply absent.
  (let [m (manifest/build-v1-1 (dissoc sample-fields-v1-1 :runtime))]
    (is (manifest/valid-v1-1? m))
    (is (not (contains? m :runtime)))))

(deftest build-v1-1-defaults-canonicalizers-applied-to-empty-test
  (let [m (manifest/build-v1-1 (dissoc sample-fields-v1-1 :canonicalizers-applied))]
    (is (= [] (:canonicalizers-applied m)))
    (is (manifest/valid-v1-1? m))))

(deftest valid-v1-1-rejects-missing-required-fields-test
  (is (not (manifest/valid-v1-1? (dissoc (manifest/build-v1-1 sample-fields-v1-1) :stage))))
  (is (not (manifest/valid-v1-1? (dissoc (manifest/build-v1-1 sample-fields-v1-1) :seeds))))
  (is (not (manifest/valid-v1-1? (dissoc (manifest/build-v1-1 sample-fields-v1-1) :engine-params))))
  (is (not (manifest/valid-v1-1? (dissoc (manifest/build-v1-1 sample-fields-v1-1) :generator)))))

(deftest valid-v1-1-rejects-wrong-schema-version-test
  (is (not (manifest/valid-v1-1? (assoc (manifest/build-v1-1 sample-fields-v1-1) :schema-version 1))))
  (is (not (manifest/valid-v1-1? (assoc (manifest/build-v1-1 sample-fields-v1-1) :schema-version "2")))))

(deftest v1-manifest-is-not-a-valid-v1-1-manifest-test
  ;; Pre-v1.1 manifests remain valid *historical records* (schema
  ;; versioning, not migration) -- but they are genuinely a different
  ;; shape and must not pass as v1.1.
  (is (not (manifest/valid-v1-1? (manifest/build-v1 sample-fields-v1)))))

(deftest v1-1-manifest-is-not-a-valid-v1-manifest-test
  (is (not (manifest/valid-v1? (manifest/build-v1-1 sample-fields-v1-1)))))
