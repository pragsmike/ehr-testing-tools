(ns ehr-testing-tools.corpus.manifest-test
  (:require [clojure.test :refer [deftest is]]
            [ehr-testing-tools.corpus.manifest :as manifest]))

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
