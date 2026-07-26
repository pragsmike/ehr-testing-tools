(ns ehr-testing-tools.corpus.operators
  "The mutation operator catalog (ADR-0004): operators as data, a
  registry like ehr-testing-tools.canonical (pattern nursery #3), and
  seed catalogs of FHIR and v2 defect operators. Pattern nursery #14
  (operators carry contracts): mutation and metamorphic perturbation
  are one operation shape wearing two hats -- what distinguishes them
  is only the :contract each declares (:violates a base-spec
  constraint, for a defect operator; :preserves a relation, for a
  metamorphic one). Only :violates entries are populated this session
  -- no metamorphic (:preserves) operator has been authored yet.

  Informed by EXP-B2's applied decision rule, for both formats: every
  :fn here operates on plain, delimiter-split data, never a HAPI-parsed
  tree. For FHIR (P4): data.json-shaped JSON, string keys and integer
  indices -- HAPI FHIR's round-trip was found to silently drop
  resource.id, disqualifying it as a mutation substrate. For v2 (P7):
  ehr-testing-tools.corpus.er7's delimiter-split segments/fields --
  HAPI HL7v2's PipeParser round-trip was found to canonicalize away
  trailing empty fields, the same class of hazard for the same reason
  (docs/experiments/EXP-B2-results.md); PipeParser remains fine for
  *judging* (judge.v2 uses it unchanged), only disqualified as the
  mutation substrate.

  v2 operator scope (P7, author ruling): the seed catalog below
  contains only defects ehr-testing-tools.judge.v2's base-structural
  tier actually convicts, verified empirically against a real fixture
  before being registered here, not assumed from the defect's category
  -- several plausible candidates (dropping the PID segment entirely;
  corrupting PID's own segment-name; blanking a non-header field like
  PID-7) were probed and found to produce :pass, not :rejected, at this
  tier (HAPI's `defaultValidation` context does not enforce segment
  presence or field cardinality beyond primitive-type checking wired
  into parsing itself, per judge.v2's own docstring) -- they are
  recorded here as dropped, not shipped unconvictable, rather than
  silently omitted with no trace. See docs/judge-calibration.md's v2
  tier calibration section (CAL-1) for the consumer-facing read of
  this finding, the HAPI-source mechanism behind it (facts register
  F22), and this paragraph's cross-reference back as the catalog-side
  record.

  Registry and validation live together here (unlike canonicalizers,
  split across canonical.clj + corpus/canonicalizers.clj) because this
  catalog has few enough consumers so far (corpus.mutate) that a
  generic top-level registry namespace would be premature abstraction."
  (:require [malli.core :as m]
            [ehr-testing-tools.corpus.er7 :as er7]
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
   ;; :doc (DOC-3) -- one sentence in the user's register: what this
   ;; operator does to the input, not how it does it. Optional, so an
   ;; entry registered without one is still valid; the seed catalog
   ;; below carries one on every entry. This is what docs/operators.md
   ;; renders per operator, alongside the :contract's own target
   ;; sentence -- the two are deliberately different registers, not
   ;; duplicates: :contract/:target names the base-spec constraint the
   ;; mutation violates (a conformance claim, cited to the spec),
   ;; :doc names the edit itself (what changed in the file).
   [:doc {:optional true} [:string {:min 1}]]
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
  :doc "Deletes whatever the locator names, leaving the rest of the resource as it was."
  :contract {:type :violates
             :target "removes the element at the locator path, violating that element's minimum-cardinality constraint (Element.min >= 1 per the base FHIR StructureDefinition for whichever element the locator names)"}
  :locator-required? true
  :fn remove-required-element})

(defn- duplicate-element
  [data path]
  (update-in data path (fn [v] [v v])))

(register!
 {:id :duplicate-element :version "1" :format :fhir
  :doc "Replaces the value at the locator with a two-element JSON array holding that same value twice."
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
  :doc "Replaces the value at the locator with a code string no value set contains."
  :contract {:type :violates
             :target "replaces the value at the locator with a string outside any plausible bound ValueSet, violating the FHIR requirement that a code-type element's value be drawn from its bound ValueSet (e.g. Patient.gender is bound to http://hl7.org/fhir/ValueSet/administrative-gender)"}
  :locator-required? true
  :fn invalid-code-value})

(defn- malformed-date
  [data path]
  (assoc-in data path "2026-13-45"))

(register!
 {:id :malformed-date :version "1" :format :fhir
  :doc "Replaces the value at the locator with a date-shaped string that is not a real date (\"2026-13-45\")."
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
  :doc "Replaces the value at the locator with one of a different JSON type -- a number where a string was, a string where a boolean was."
  :contract {:type :violates
             :target "replaces the value at the locator with a value of a structurally different JSON type than its FHIR data type requires (e.g. a string where a boolean or number is required), violating the base FHIR type constraint for whichever element the locator names"}
  :locator-required? true
  :fn wrong-type-value})

;; ---- v2 seed catalog (P7): every :fn here is (parsed loc) -> parsed,
;; parsed being ehr-testing-tools.corpus.er7/parse's output and loc
;; being ehr-testing-tools.locator/v2-data-path's structured map --
;; corpus.mutate validates loc resolves (er7/resolve-locator) before
;; calling in, same trust boundary as the FHIR operators above. Every
;; entry below convicts under judge.v2's base-structural tier -- see
;; the module docstring for the candidates that did not and were
;; dropped. ----

(defn- v2-blank-field
  [parsed {:keys [segment segment-repeat field]}]
  (let [seg-idx (er7/segment-occurrence-index parsed segment segment-repeat)
        fld-idx (er7/field-index segment field)]
    (assoc-in parsed [:segments seg-idx fld-idx] "")))

(register!
 {:id :blank-required-field :version "1" :format :v2
  :doc "Empties the field the locator names, leaving its position in the segment intact."
  :contract {:type :violates
             :target "blanks the field at the locator, violating message-structure resolution's requirement that certain fields (e.g. MSH-9, the message type -- HAPI needs it to select which structure to parse the message into) be present with a value"}
  :locator-required? true
  :fn v2-blank-field})

(def ^:private corrupted-encoding-characters
  "A 3-character encoding-characters value missing the escape
  character -- the minimal corruption ca.uhn.hl7v2.parser.PipeParser
  is known to reject at parse time (mirrors the existing judge.v2 probe
  fixed in judge/v2_test.clj's execute-bad-delimiter-captures-parse-
  exception-test)."
  "^~&")

(defn- v2-corrupt-encoding-characters
  [parsed {:keys [segment segment-repeat field]}]
  (let [seg-idx (er7/segment-occurrence-index parsed segment segment-repeat)
        fld-idx (er7/field-index segment field)]
    (assoc-in parsed [:segments seg-idx fld-idx] corrupted-encoding-characters)))

(register!
 {:id :corrupt-encoding-characters :version "1" :format :v2
  :doc "Replaces the field the locator names -- MSH-2, where a message declares its own delimiters -- with a three-character value, one short of the four HL7 v2 requires."
  :contract {:type :violates
             :target "replaces the field at the locator (MSH-2, the encoding characters) with a malformed 3-character value missing the required escape character, violating HL7 v2's own encoding-characters well-formedness rule (MSH-2 must name exactly four characters: component, repetition, escape, subcomponent separators)"}
  :locator-required? true
  :fn v2-corrupt-encoding-characters})

(def ^:private not-a-real-datetime
  "notadate")

(defn- v2-malformed-datetime-value
  [parsed {:keys [segment segment-repeat field]}]
  (let [seg-idx (er7/segment-occurrence-index parsed segment segment-repeat)
        fld-idx (er7/field-index segment field)]
    (assoc-in parsed [:segments seg-idx fld-idx] not-a-real-datetime)))

(register!
 {:id :malformed-datetime-value :version "1" :format :v2
  :doc "Replaces the field the locator names with a string that is not a valid HL7 v2 timestamp (\"notadate\")."
  :contract {:type :violates
             :target "replaces the field at the locator with a string failing HL7 v2's DTM lexical format (YYYY[MM[DD[HHMM[SS[.S[S[S[S]]]]]]]][+/-ZZZZ]), violating that field's required primitive data type -- HAPI's defaultValidation context wires primitive-type checking into parsing itself, so this is a parse-time failure, not a post-parse one"}
  :locator-required? true
  :fn v2-malformed-datetime-value})

(defn- v2-truncate-segment-fields
  [parsed {:keys [segment segment-repeat field]}]
  (let [seg-idx (er7/segment-occurrence-index parsed segment segment-repeat)
        fld-idx (er7/field-index segment field)]
    (update-in parsed [:segments seg-idx] #(subvec % 0 fld-idx))))

(register!
 {:id :truncate-segment-fields :version "1" :format :v2
  :doc "Cuts the segment short at the locator's field, dropping that field and every field after it."
  :contract {:type :violates
             :target "truncates the segment named by the locator to end just before the locator's own field, dropping that field and every field after it -- violating message-structure resolution's requirement that certain fields exist positionally at all (distinct from :blank-required-field, which leaves the field's own slot present but empty; here the slot itself is gone)"}
  :locator-required? true
  :fn v2-truncate-segment-fields})

(defn- v2-corrupt-segment-name
  [parsed {:keys [segment segment-repeat]}]
  (let [seg-idx (er7/segment-occurrence-index parsed segment segment-repeat)
        original (get-in parsed [:segments seg-idx 0])
        corrupted (str (subs original 0 (dec (count original))) "X")]
    (assoc-in parsed [:segments seg-idx 0] corrupted)))

(register!
 {:id :corrupt-segment-name :version "1" :format :v2
  :doc "Changes the last character of the segment name the locator points at, so MSH becomes MSX."
  :contract {:type :violates
             :target "corrupts the last character of the segment name at the locator, violating HL7 v2's requirement that a message begin with a recognized MSH segment (verified only against MSH -- corrupting a non-header segment's own name, e.g. PID, was probed and found NOT to convict at this tier: HAPI's defaultValidation context tolerates an unrecognized segment identifier elsewhere in the message)"}
  :locator-required? true
  :fn v2-corrupt-segment-name})
