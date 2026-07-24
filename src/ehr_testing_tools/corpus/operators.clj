(ns ehr-testing-tools.corpus.operators
  "The mutation operator catalog (ADR-0004): operators as data, a
  registry like ehr-testing-tools.canonical (pattern nursery #3), and
  a seed catalog of FHIR defect operators. Pattern nursery #14
  (operators carry contracts): mutation and metamorphic perturbation
  are one operation shape wearing two hats -- what distinguishes them
  is only the :contract each declares (:violates a base-spec
  constraint, for a defect operator; :preserves a relation, for a
  metamorphic one). Only :violates entries are populated this session
  -- no metamorphic (:preserves) operator has been authored yet.

  Informed by EXP-B2's applied decision rule: every :fn here operates
  on plain Clojure data (data.json-shaped FHIR JSON, string keys and
  integer indices), never a HAPI-parsed tree -- HAPI FHIR's round-trip
  was found to silently drop resource.id, disqualifying it as a
  mutation substrate.

  Registry and validation live together here (unlike canonicalizers,
  split across canonical.clj + corpus/canonicalizers.clj) because this
  catalog has exactly one consumer so far (corpus.mutate) -- a generic
  top-level registry namespace would be premature abstraction until a
  second, non-FHIR catalog actually needs the same shape."
  (:require [malli.core :as m]
            [ehr-testing-tools.result :as result]))

(def Contract
  [:map
   [:type [:enum :violates :preserves]]
   [:target [:string {:min 1}]]])

(def Operator
  [:map
   [:id :keyword]
   [:version :string]
   [:format :keyword]
   [:contract Contract]
   [:locator-required? :boolean]
   [:fn [:fn fn?]]])

(defonce ^:private registry (atom {}))

(defn register!
  "Registers an operator entry, keyed by [id version]. Returns
  result/ok {:id :version} or result/rejected :invalid-operator."
  [entry]
  (if (m/validate Operator entry)
    (do (swap! registry assoc [(:id entry) (:version entry)] entry)
        (result/ok (select-keys entry [:id :version])))
    (result/rejected :invalid-operator {:entry entry})))

(defn lookup
  [id version]
  (get @registry [id version]))

(defn entries
  []
  (vals @registry))

(defn registry-snapshot
  "Test/dev support: the full registry map, keyed by [id version] --
  for saving and later restoring exact state, same convention as
  ehr-testing-tools.canonical."
  []
  @registry)

(defn reset-registry!
  ([] (reset-registry! {}))
  ([snapshot] (reset! registry snapshot)))

;; ---- seed catalog: FHIR defect operators spanning the defect
;; taxonomy named in the P4 prompt. Each :fn is (data path) -> data,
;; a pure transform assuming path already resolves in data --
;; corpus.mutate validates the locator resolves before calling in. ----

(defn- remove-required-element
  [data path]
  (if (= 1 (count path))
    (dissoc data (first path))
    (update-in data (butlast path) dissoc (last path))))

(register!
 {:id :remove-required-element :version "1" :format :fhir
  :contract {:type :violates
             :target "removes the element at the locator path, violating that element's minimum-cardinality constraint (Element.min >= 1 per the base FHIR StructureDefinition for whichever element the locator names)"}
  :locator-required? true
  :fn remove-required-element})

(defn- duplicate-element
  [data path]
  (update-in data path (fn [v] [v v])))

(register!
 {:id :duplicate-element :version "1" :format :fhir
  :contract {:type :violates
             :target "wraps the value at the locator into a two-element JSON array, violating the FHIR JSON representation rule that singular (max-cardinality-1) elements must be represented as a single value, never an array"}
  :locator-required? true
  :fn duplicate-element})

(def ^:private not-a-real-code "not-a-valid-code-9f3a1c")

(defn- invalid-code-value
  [data path]
  (assoc-in data path not-a-real-code))

(register!
 {:id :invalid-code-value :version "1" :format :fhir
  :contract {:type :violates
             :target "replaces the value at the locator with a string outside any plausible bound ValueSet, violating the FHIR requirement that a code-type element's value be drawn from its bound ValueSet (e.g. Patient.gender is bound to http://hl7.org/fhir/ValueSet/administrative-gender)"}
  :locator-required? true
  :fn invalid-code-value})

(defn- malformed-date
  [data path]
  (assoc-in data path "2026-13-45"))

(register!
 {:id :malformed-date :version "1" :format :fhir
  :contract {:type :violates
             :target "replaces the value at the locator with a string failing the FHIR date/dateTime/instant regex (base FHIR spec's own YYYY[-MM[-DD]] / full dateTime pattern), violating that element's required lexical format"}
  :locator-required? true
  :fn malformed-date})

(defn- wrong-type-value
  [data path]
  (let [current (get-in data path)
        replacement (cond
                      (string? current) 12345
                      (number? current) "not-a-number"
                      (boolean? current) "not-a-boolean"
                      (map? current) "not-an-object"
                      (vector? current) "not-an-array"
                      :else "wrong-type")]
    (assoc-in data path replacement)))

(register!
 {:id :wrong-type-value :version "1" :format :fhir
  :contract {:type :violates
             :target "replaces the value at the locator with a value of a structurally different JSON type than its FHIR data type requires (e.g. a string where a boolean or number is required), violating the base FHIR type constraint for whichever element the locator names"}
  :locator-required? true
  :fn wrong-type-value})
