(ns ehr-testing-tools.corpus.operators-test
  "Loading this namespace registers the seed FHIR defect operators as a
  side effect (same registration-at-load-time convention as
  corpus.canonicalizers) -- requiring it above is enough."
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.corpus.operators :as operators]))

;; ---- registry mechanics ----

(deftest register-and-lookup-test
  (let [r (operators/register! {:id :test-op :version "1" :format :fhir
                                 :contract {:type :violates :target "test constraint"}
                                 :locator-required? true
                                 :fn (fn [data _path] data)})]
    (is (result/ok? r))
    (is (some? (operators/lookup :test-op "1")))
    (is (nil? (operators/lookup :nope "1")))))

(deftest register-rejects-invalid-entry-test
  (let [r (operators/register! {:id :bad :version "1"})]
    (is (result/rejected? r))
    (is (= :invalid-operator (:category r)))))

(deftest register-rejects-bad-contract-type-test
  (let [r (operators/register! {:id :bad :version "1" :format :fhir
                                 :contract {:type :not-violates-or-preserves :target "x"}
                                 :locator-required? true
                                 :fn (fn [data _path] data)})]
    (is (result/rejected? r))))

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
