(ns ehrt.kernel.locator-test
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.result :as result]
            [ehrt.kernel.locator :as locator]))

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
;; section docstring for the MSH-1/MSH-2 off-by-one convention, whose
;; field-to-split-index mapping lives in corpus.er7 and whose one
;; grammar-level consequence -- MSH-1 is not addressable at all -- is
;; enforced here, in the MSH-1 block further down. ----

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

;; ---- MSH-1: refused at parse (LOC-1, 2026-07-25). It used to parse
;; like any other field locator and then resolve onto MSH-2's slot,
;; because corpus.er7/field-index shifts only for N >= 2 -- so a
;; locator of MSH-1 silently addressed the encoding characters. The
;; refusal is MSH-specific and field-1-specific; every edge around it
;; is pinned below. The off-by-one convention itself still lives in
;; corpus.er7, unchanged. ----

(deftest v2-data-path-refuses-msh-1-test
  (let [r (locator/v2-data-path "MSH-1")]
    (is (result/rejected? r))
    (is (= :invalid-v2-path (:category r)))
    (is (= "MSH-1" (:path (:payload r))))
    (let [hint (:hint (:payload r))]
      (is (string? hint) "the refusal carries a teaching hint, DOC-1's house pattern")
      (is (re-find #"field separator" hint))
      (is (re-find #"MSH-2" hint) "and points at where the encoding characters actually live"))))

(deftest v2-data-path-refuses-every-form-that-names-msh-field-1-test
  (doseq [bad ["MSH-1" "MSH-1[2]" "MSH-1.1" "MSH-1.1.2" "MSH[1]-1"]]
    (let [r (locator/v2-data-path bad)]
      (is (result/rejected? r) (str "expected rejection for " (pr-str bad)))
      (is (= :invalid-v2-path (:category r)))
      (is (string? (:hint (:payload r))) (str (pr-str bad) " must teach, not just refuse")))))

;; The refusal's edges: it is about MSH's field 1 and nothing else.

(deftest v2-data-path-parses-msh-2-like-any-other-field-test
  (let [r (locator/v2-data-path "MSH-2")]
    (is (result/ok? r))
    (is (= {:segment "MSH" :field 2} (:payload r)))))

(deftest v2-data-path-parses-pid-1-because-field-1-is-ordinary-data-elsewhere-test
  (doseq [[path expected] [["PID-1" {:segment "PID" :field 1}]
                           ["ZZ1-1" {:segment "ZZ1" :field 1}]
                           ["OBX[2]-1" {:segment "OBX" :segment-repeat 2 :field 1}]]]
    (let [r (locator/v2-data-path path)]
      (is (result/ok? r) (str path " must parse -- only MSH's field 1 is special"))
      (is (= expected (:payload r))))))

(deftest v2-data-path-parses-msh-as-a-whole-segment-test
  (let [r (locator/v2-data-path "MSH")]
    (is (result/ok? r) "the segment-level MSH locator names no field and is untouched")
    (is (= {:segment "MSH"} (:payload r)))))

;; ---- categories: LOC-1 enriched two payloads and introduced no new
;; category, so callers dispatching on :category are unaffected. ----

(deftest loc-1-rejections-reuse-the-existing-categories-test
  (is (= :invalid-fhir-path (:category (locator/fhir-data-path "entry[0].resource.")))
      "the trailing-separator rejection is an ordinary :invalid-fhir-path")
  (is (= :invalid-v2-path (:category (locator/v2-data-path "MSH-1")))
      "the MSH-1 refusal is an ordinary :invalid-v2-path")
  (is (= #{:invalid-v2-path}
         (set (map #(:category (locator/v2-data-path %)) ["MSH-1" "PID-" "pid" ""])))
      "one category for every v2 grammar refusal, teaching hint or not"))

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

;; ---- cross-format parity (LOC-1, 2026-07-25): both grammars are
;; fully anchored, so neither accepts a separator with nothing after
;; it. The v2 side was always so (v2-path-re's ^...$); the FHIR side
;; got there when fhir-data-path started splitting with limit -1, which
;; keeps the trailing empty token alive long enough for the existing
;; (some empty? segments) guard to fire on it. Pinned side by side
;; because the parity is the point of the change, not a side effect. ----

(deftest both-grammars-reject-a-trailing-separator-test
  (doseq [bad ["entry[0].resource." "gender." "entry[0]."]]
    (let [r (locator/fhir-data-path bad)]
      (is (result/rejected? r) (str "expected rejection for " (pr-str bad)))
      (is (= :invalid-fhir-path (:category r)))))
  (doseq [bad ["PID-3." "PID-" "PID-3.1."]]
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
