(ns ehrt.tools.corpus.operators-test
  "Loading this namespace registers the seed FHIR defect operators as a
  side effect (same registration-at-load-time convention as
  corpus.canonicalizers) -- requiring it above is enough."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as kernel]
            [ehrt.tools.corpus.operators :as operators]))

;; ---- registry mechanics ----

(deftest register-and-lookup-test
  (let [r (operators/register! {:id :test-op :version "1" :format :fhir
                                 :contract {:type :violates :target "test constraint"}
                                 :locator-required? true
                                 :fn (fn [data _path] data)})]
    (is (kernel/ok? r))
    (is (some? (operators/lookup :test-op "1")))
    (is (nil? (operators/lookup :nope "1")))))

(deftest register-rejects-invalid-entry-test
  (let [r (operators/register! {:id :bad :version "1"})]
    (is (kernel/rejected? r))
    (is (= :invalid-operator (:category r)))))

(deftest register-rejects-bad-contract-type-test
  (let [r (operators/register! {:id :bad :version "1" :format :fhir
                                 :contract {:type :not-violates-or-preserves :target "x"}
                                 :locator-required? true
                                 :fn (fn [data _path] data)})]
    (is (kernel/rejected? r))))

(deftest entries-lists-all-registered-test
  (operators/register! {:id :e1 :version "1" :format :fhir
                         :contract {:type :violates :target "t"}
                         :locator-required? true :fn (fn [d _p] d)})
  (is (some #(= [:e1 "1"] [(:id %) (:version %)]) (operators/entries))))

;; ---- seed catalog: 4-6 FHIR defect operators spanning the taxonomy
;; named in the P4 prompt: remove required element; cardinality
;; violation via duplication; invalid code value; malformed date;
;; wrong-type value. Each declares {:type :violates :target ...}. ----

(def seed-ids
  #{:remove-required-element :duplicate-element :invalid-code-value
    :malformed-date :wrong-type-value})

(deftest all-seed-operators-registered-at-version-1-test
  (doseq [id seed-ids]
    (is (some? (operators/lookup id "1")) (str id " must be registered"))))

(deftest seed-catalog-spans-four-to-six-operators-test
  (is (<= 4 (count seed-ids) 6)))

(deftest every-seed-operator-declares-a-violates-contract-test
  (doseq [id seed-ids]
    (let [entry (operators/lookup id "1")]
      (is (= :violates (:type (:contract entry))) (str id " must declare :violates"))
      (is (string? (not-empty (:target (:contract entry)))) (str id " contract :target must be non-empty"))
      (is (= :fhir (:format entry)))
      (is (true? (:locator-required? entry))))))

;; ---- individual operator semantics ----

(deftest remove-required-element-removes-the-value-at-path-test
  (let [entry (operators/lookup :remove-required-element "1")
        data {"resourceType" "Patient" "gender" "female" "id" "abc"}
        mutated ((:fn entry) data ["gender"])]
    (is (not (contains? mutated "gender")))
    (is (= "abc" (get mutated "id")) "unrelated fields must be untouched")))

(deftest duplicate-element-wraps-value-in-a-two-element-array-test
  (let [entry (operators/lookup :duplicate-element "1")
        data {"gender" "female"}
        mutated ((:fn entry) data ["gender"])]
    (is (= ["female" "female"] (get mutated "gender")))))

(deftest invalid-code-value-replaces-with-a-value-outside-any-valueset-test
  (let [entry (operators/lookup :invalid-code-value "1")
        data {"gender" "female"}
        mutated ((:fn entry) data ["gender"])]
    (is (not= "female" (get mutated "gender")))
    (is (string? (get mutated "gender")))))

(deftest malformed-date-replaces-with-a-non-conformant-date-string-test
  (let [entry (operators/lookup :malformed-date "1")
        data {"birthDate" "1985-03-12"}
        mutated ((:fn entry) data ["birthDate"])]
    (is (not= "1985-03-12" (get mutated "birthDate")))
    ;; must not accidentally still satisfy the FHIR date regex
    (is (not (re-matches #"([0-9]([0-9]([0-9][1-9]|[1-9]0)|[1-9]00)|[1-9]000)(-(0[1-9]|1[0-2])(-(0[1-9]|[1-2][0-9]|3[0-1]))?)?"
                          (get mutated "birthDate"))))))

(deftest wrong-type-value-replaces-with-a-different-json-type-test
  (let [entry (operators/lookup :wrong-type-value "1")
        data {"active" true}
        mutated ((:fn entry) data ["active"])]
    (is (not (boolean? (get mutated "active"))))))

;; ---- operators navigate nested paths (the realistic locator shape,
;; e.g. entry[0].resource.gender parsed to ["entry" 0 "resource"
;; "gender"]) ----

(deftest operators-navigate-nested-paths-test
  (let [entry (operators/lookup :remove-required-element "1")
        data {"entry" [{"resource" {"gender" "female" "id" "x"}}]}
        mutated ((:fn entry) data ["entry" 0 "resource" "gender"])]
    (is (not (contains? (get-in mutated ["entry" 0 "resource"]) "gender")))
    (is (= "x" (get-in mutated ["entry" 0 "resource" "id"])))))

;; ---- v2 seed catalog (P7): every entry convicts under judge.v2's
;; base-structural tier (verified empirically -- see this suite's twin,
;; test/ehr_testing_tools/corpus/mutate_test.clj and the v2 contract
;; pairing suite, for the judge-facing proof; this suite covers the
;; operator :fn's own mechanical semantics against synthetic data, same
;; split as the FHIR section above). ----

(def v2-seed-ids
  #{:blank-required-field :corrupt-encoding-characters
    :malformed-datetime-value :truncate-segment-fields :corrupt-segment-name})

(deftest all-v2-seed-operators-registered-at-version-1-test
  (doseq [id v2-seed-ids]
    (is (some? (operators/lookup id "1")) (str id " must be registered"))))

(deftest every-v2-seed-operator-declares-a-violates-contract-test
  (doseq [id v2-seed-ids]
    (let [entry (operators/lookup id "1")]
      (is (= :violates (:type (:contract entry))) (str id " must declare :violates"))
      (is (string? (not-empty (:target (:contract entry)))) (str id " contract :target must be non-empty"))
      (is (= :v2 (:format entry)))
      (is (true? (:locator-required? entry))))))

;; ---- :doc (DOC-3): every seed entry carries the one-sentence
;; user-register description docs/operators.md renders. Asserted over
;; the two seed-id sets rather than over `entries`, because this
;; suite's own registry-mechanics tests register throwaway entries
;; (:test-op, :e1) into the same shared registry -- those legitimately
;; have no :doc, since the key is optional. ----

(deftest every-seed-operator-carries-a-doc-sentence-test
  (doseq [id (concat (sort seed-ids) (sort v2-seed-ids))]
    (let [doc (:doc (operators/lookup id "1"))]
      (is (string? (not-empty doc)) (str id " must carry a non-empty :doc"))
      (is (re-find #"\.$" (or doc "")) (str id " :doc must be a sentence, ending in a period")))))

(deftest doc-is-optional-in-the-schema-test
  (let [r (operators/register! {:id :no-doc-op :version "1" :format :fhir
                                 :contract {:type :violates :target "t"}
                                 :locator-required? true :fn (fn [d _p] d)})]
    (is (kernel/ok? r) "an entry without :doc is still a valid operator")))

;; MSH-1=field sep (no split slot), MSH-2="^~\&", MSH-9=message type at
;; split-index 8; PID-7=birth date at split-index 7 (see corpus.er7's
;; own field-index docstring for the MSH-vs-other-segment convention).
(def ^:private sample-parsed
  {:delimiters {:field "|" :component "^" :repetition "~" :escape "\\" :subcomponent "&"}
   :segments [["MSH" "^~\\&" "SND" "FAC" "RCV" "FAC" "20260101" "" "ADT^A01^ADT_A01" "MSG1" "P" "2.4"]
              ["PID" "1" "" "12345" "" "Doe^John" "" "19800101"]]})

(deftest v2-blank-field-blanks-the-located-field-test
  (let [entry (operators/lookup :blank-required-field "1")
        mutated ((:fn entry) sample-parsed {:segment "MSH" :field 9})]
    (is (= "" (get-in mutated [:segments 0 8])))
    (is (= "MSG1" (get-in mutated [:segments 0 9])) "unrelated fields must be untouched")))

(deftest v2-corrupt-encoding-characters-replaces-msh-2-test
  (let [entry (operators/lookup :corrupt-encoding-characters "1")
        mutated ((:fn entry) sample-parsed {:segment "MSH" :field 2})]
    (is (= "^~&" (get-in mutated [:segments 0 1])))))

(deftest v2-malformed-datetime-value-replaces-with-a-non-conformant-value-test
  (let [entry (operators/lookup :malformed-datetime-value "1")
        mutated ((:fn entry) sample-parsed {:segment "PID" :field 7})]
    (is (= "notadate" (get-in mutated [:segments 1 7])))
    (is (not= "19800101" (get-in mutated [:segments 1 7])))))

(deftest v2-truncate-segment-fields-drops-the-field-and-everything-after-test
  (let [entry (operators/lookup :truncate-segment-fields "1")
        mutated ((:fn entry) sample-parsed {:segment "MSH" :field 9})]
    (is (= 8 (count (get-in mutated [:segments 0]))) "MSH-9 (split-index 8) and everything after it must be gone")
    (is (= "MSH" (get-in mutated [:segments 0 0])) "fields before the truncation point are untouched")))

(deftest v2-corrupt-segment-name-corrupts-the-last-character-test
  (let [entry (operators/lookup :corrupt-segment-name "1")
        mutated ((:fn entry) sample-parsed {:segment "PID"})]
    (is (= "PIX" (get-in mutated [:segments 1 0])))
    (is (= "12345" (get-in mutated [:segments 1 3])) "unrelated fields must be untouched")))
