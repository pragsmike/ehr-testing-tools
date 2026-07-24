(ns ehr-testing-tools.locator-test
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.locator :as locator]))

(deftest make-valid-locator-test
  (let [r (locator/make :fhir "Patient.name")]
    (is (result/ok? r))
    (is (= {:format :fhir :path "Patient.name"} (:payload r)))
    (is (locator/valid? (:payload r)))))

(deftest make-rejects-unknown-format-test
  (let [r (locator/make :bogus "x")]
    (is (result/rejected? r))
    (is (= :invalid-locator (:category r)))))

(deftest make-rejects-empty-path-test
  (let [r (locator/make :fhir "")]
    (is (result/rejected? r))
    (is (= :invalid-locator (:category r)))))

(deftest valid-schema-check-test
  (is (locator/valid? {:format :v2 :path "PID-3"}))
  (is (not (locator/valid? {:format :v2 :path ""})))
  (is (not (locator/valid? {:format :not-a-format :path "x"})))
  (is (not (locator/valid? "garbage"))))

(deftest known-formats-test
  (is (contains? locator/known-formats :fhir))
  (is (contains? locator/known-formats :v2))
  (is (contains? locator/known-formats :table))
  (is (contains? locator/known-formats :xpath)))

;; ---- FHIR data-path grammar (P4): the operational subset mutation
;; needs -- dotted field access plus bracketed integer indices, into
;; parsed plain-data JSON. Full FHIRPath is future work; this is not
;; it. ----

(deftest fhir-data-path-parses-a-bare-field-test
  (let [r (locator/fhir-data-path "gender")]
    (is (result/ok? r))
    (is (= ["gender"] (:payload r)))))

(deftest fhir-data-path-parses-dotted-fields-test
  (let [r (locator/fhir-data-path "resource.gender")]
    (is (result/ok? r))
    (is (= ["resource" "gender"] (:payload r)))))

(deftest fhir-data-path-parses-a-bracketed-index-test
  (let [r (locator/fhir-data-path "entry[0]")]
    (is (result/ok? r))
    (is (= ["entry" 0] (:payload r)))))

(deftest fhir-data-path-parses-a-realistic-full-path-test
  (let [r (locator/fhir-data-path "entry[0].resource.name[0].given[0]")]
    (is (result/ok? r))
    (is (= ["entry" 0 "resource" "name" 0 "given" 0] (:payload r)))))

(deftest fhir-data-path-rejects-empty-string-test
  (let [r (locator/fhir-data-path "")]
    (is (result/rejected? r))
    (is (= :invalid-fhir-path (:category r)))))

(deftest fhir-data-path-rejects-malformed-segment-test
  (doseq [bad ["entry[" "entry]" "entry[abc]" "." "entry..resource" "entry[0][1]" "9bad"]]
    (let [r (locator/fhir-data-path bad)]
      (is (result/rejected? r) (str "expected rejection for " (pr-str bad)))
      (is (= :invalid-fhir-path (:category r))))))

(deftest fhir-data-path-navigates-real-data-with-get-in-test
  ;; The whole point: the parsed path must actually work with
  ;; get-in/assoc-in/update-in against plain-data (data.json-shaped)
  ;; FHIR JSON -- string keys, integer indices.
  (let [data {"entry" [{"resource" {"resourceType" "Patient" "gender" "female"}}]}
        r (locator/fhir-data-path "entry[0].resource.gender")]
    (is (result/ok? r))
    (is (= "female" (get-in data (:payload r))))))
