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

;; ---- v2 grammar (P7, arrives with mutation): segment / segment+repeat
;; / field / field+repeat / component / subcomponent, over the
;; delimiter-split ER7 substrate (corpus.er7) -- see locator.clj's v2
;; section docstring for the MSH-1/MSH-2 off-by-one convention this
;; grammar deliberately does NOT special-case (it lives in corpus.er7's
;; field-to-split-index mapping instead). ----

(deftest v2-data-path-parses-a-bare-segment-test
  (let [r (locator/v2-data-path "PID")]
    (is (result/ok? r))
    (is (= {:segment "PID"} (:payload r)))))

(deftest v2-data-path-parses-a-segment-with-repeat-test
  (let [r (locator/v2-data-path "OBX[2]")]
    (is (result/ok? r))
    (is (= {:segment "OBX" :segment-repeat 2} (:payload r)))))

(deftest v2-data-path-parses-a-field-test
  (let [r (locator/v2-data-path "PID-3")]
    (is (result/ok? r))
    (is (= {:segment "PID" :field 3} (:payload r)))))

(deftest v2-data-path-parses-a-field-with-repeat-test
  (let [r (locator/v2-data-path "PID-3[2]")]
    (is (result/ok? r))
    (is (= {:segment "PID" :field 3 :field-repeat 2} (:payload r)))))

(deftest v2-data-path-parses-a-component-test
  (let [r (locator/v2-data-path "PID-3.1")]
    (is (result/ok? r))
    (is (= {:segment "PID" :field 3 :component 1} (:payload r)))))

(deftest v2-data-path-parses-a-subcomponent-test
  (let [r (locator/v2-data-path "PID-3.1.2")]
    (is (result/ok? r))
    (is (= {:segment "PID" :field 3 :component 1 :subcomponent 2} (:payload r)))))

;; ---- MSH-1/MSH-2: syntactically ordinary field locators at the
;; grammar level -- the off-by-one convention (MSH-1 has no split-array
;; slot; MSH-2 lands at split-index 1) is a corpus.er7 concern, not
;; this parser's, per the module docstring. ----

(deftest v2-data-path-parses-msh-1-like-any-other-field-test
  (let [r (locator/v2-data-path "MSH-1")]
    (is (result/ok? r))
    (is (= {:segment "MSH" :field 1} (:payload r)))))

(deftest v2-data-path-parses-msh-2-like-any-other-field-test
  (let [r (locator/v2-data-path "MSH-2")]
    (is (result/ok? r))
    (is (= {:segment "MSH" :field 2} (:payload r)))))

;; ---- rejects: unknown segment-name shapes, zero/negative indices
;; (inexpressible in the grammar -- no digit class admits 0 or a sign),
;; trailing separators (the regex is fully anchored) ----

(deftest v2-data-path-rejects-malformed-strings-test
  (doseq [bad ["" "PI" "PIDD" "PI-3" "pid-3" "9ID" "PID-" "PID-3-"
               "PID-3." "PID-3.1." "PID[" "PID[2" "PID[x]" "PID[0]"
               "PID-0" "PID-3[0]" "PID-3.0" "PID--3" "PID.1"]]
    (let [r (locator/v2-data-path bad)]
      (is (result/rejected? r) (str "expected rejection for " (pr-str bad)))
      (is (= :invalid-v2-path (:category r))))))

(deftest fhir-data-path-navigates-real-data-with-get-in-test
  ;; The whole point: the parsed path must actually work with
  ;; get-in/assoc-in/update-in against plain-data (data.json-shaped)
  ;; FHIR JSON -- string keys, integer indices.
  (let [data {"entry" [{"resource" {"resourceType" "Patient" "gender" "female"}}]}
        r (locator/fhir-data-path "entry[0].resource.gender")]
    (is (result/ok? r))
    (is (= "female" (get-in data (:payload r))))))
