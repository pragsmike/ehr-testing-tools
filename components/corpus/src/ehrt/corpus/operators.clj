(ns ehrt.corpus.operators
  "The mutation operator catalog (ADR-0004): operators as data, a
  registry like ehrt.kernel.canonical (pattern nursery #3), and
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
  ehrt.corpus-io.er7's delimiter-split segments/fields --
  HAPI HL7v2's PipeParser round-trip was found to canonicalize away
  trailing empty fields, the same class of hazard for the same reason
  (docs/experiments/EXP-B2-results.md); PipeParser remains fine for
  *judging* (judge.v2 uses it unchanged), only disqualified as the
  mutation substrate.

  v2 operator scope (P7, author ruling): the seed catalog below
  contains only defects ehrt.judge-v2-hapi.v2's base-structural
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
            [ehrt.corpus-io.interface :as corpus-io]
            [ehrt.kernel.interface :as kernel]))

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
   ;; :default-locator (D12, docs/source-sink-design.md Part IX.5,
   ;; ADR-0019) -- an operator's own canonical conviction target,
   ;; consulted by the CLI when --locator-path is omitted. Optional:
   ;; declaring one is calibration work against docs/judge-calibration.md,
   ;; done per operator when its default is actually authored, not
   ;; invented speculatively by this session (D12's own text) -- no seed
   ;; catalog entry below declares one yet.
   [:default-locator {:optional true} [:string {:min 1}]]
   ;; The three event-operator slots (ADR-0176 section 2(i), ruled
   ;; 2026-09-01). Optional HERE so the ten file-level entries above
   ;; stay valid unchanged; REQUIRED, and required together, by
   ;; `EventOperator` below for :format :event.
   [:expected-findings {:optional true} [:set :keyword]]
   [:seed-consuming? {:optional true} :boolean]
   [:candidate-sites {:optional true} [:fn fn?]]
   [:fn [:fn fn?]]])

(def EventOperator
  "The extra shape :format :event carries, on top of `Operator`.

  :expected-findings is the event analogue of :contract/:target, and
  the difference is the point: a target sentence is prose about a
  third-party specification, while a finding set names invariants in
  `ehrt.sim-check.check`'s OWN closed vocabulary, so the closed oracle
  loop can assert observed = declared (Q5(a), set EQUALITY).

  :seed-consuming? true and :locator-required? false are asserted as
  literals rather than merely typed: an event operator selects its own
  site by ONE draw over the candidate sites the log offers (Q3(a)/
  Q4(a)), so an entry asking to be handed a locator instead is not a
  differently-configured event operator -- it is a mis-declared one."
  [:map
   [:format [:= :event]]
   [:expected-findings [:set :keyword]]
   [:seed-consuming? [:= true]]
   [:locator-required? [:= false]]
   [:candidate-sites [:fn fn?]]])

(defonce ^:private registry (atom {}))

;; ---- catalog gaps (ADR-0176 Q6(a), ruled 2026-09-01) --------------
;;
;; A candidate operator this repository's own catalog cannot convict is
;; REFUSED registration and recorded here, rather than shipped
;; unconvictable or dropped silently. That is the v2 seed catalog's own
;; precedent (this namespace's docstring: three plausible v2 defects
;; were probed, found to produce :pass, and "recorded as dropped, not
;; shipped unconvictable") with the one difference that matters --
;; HAPI is a third party this repository cannot extend, while `check`'s
;; catalog is its OWN. An unconvictable EVENT operator is therefore
;; evidence of a hole in `check`, not a property of the operator, and
;; ADR-0166's error ledger (a referential invariant left unmirrored
;; onto its structural twin for three weeks) is the standing proof
;; such holes sit unnoticed. Executable rather than prose, so the
;; knowledge cannot rot out of a comment.

(defonce ^:private gaps (atom []))

(defn catalog-gaps
  "Every candidate refused registration for want of a finding this
  repository's own invariant catalog can convict, in refusal order."
  []
  @gaps)

(defn reset-catalog-gaps!
  ([] (reset-catalog-gaps! []))
  ([snapshot] (reset! gaps (vec snapshot))))

(defn- record-gap!
  [entry reason]
  (swap! gaps conj {:id (:id entry)
                    :version (:version entry)
                    :format (:format entry)
                    :reason reason
                    :contract (:contract entry)}))

(defn register!
  "Registers an operator entry, keyed by [id version]. Returns
  kernel/ok {:id :version}, or:
    - kernel/rejected :invalid-operator, if the entry fails `Operator`
      or (for :format :event) `EventOperator`
    - kernel/rejected :unconvictable-operator, if an event operator
      declares no finding this repository's own catalog can convict --
      the entry is NOT registered and IS recorded as a catalog gap
      (`catalog-gaps`, ADR-0176 Q6(a))

  The three checks run in that order deliberately: a mis-declared
  entry is a shape error and says nothing about the catalog, so it must
  not be recorded as evidence of a hole in `check`."
  [entry]
  (cond
    (not (m/validate Operator entry))
    (kernel/rejected :invalid-operator {:entry entry})

    (and (= :event (:format entry)) (not (m/validate EventOperator entry)))
    (kernel/rejected :invalid-operator {:entry entry})

    (and (= :event (:format entry)) (empty? (:expected-findings entry)))
    (do (record-gap! entry :no-declared-finding)
        (kernel/rejected :unconvictable-operator
                         {:id (:id entry) :version (:version entry)
                          :reason :no-declared-finding}))

    :else
    (do (swap! registry assoc [(:id entry) (:version entry)] entry)
        (kernel/ok (select-keys entry [:id :version])))))

(defn lookup
  [id version]
  (get @registry [id version]))

(defn entries
  []
  (vals @registry))

(defn registry-snapshot
  "Test/dev support: the full registry map, keyed by [id version] --
  for saving and later restoring exact state, same convention as
  ehrt.kernel.canonical."
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
;; parsed being ehrt.corpus-io.er7/parse's output and loc
;; being ehrt.kernel.locator/v2-data-path's structured map --
;; corpus.mutate validates loc resolves (corpus-io/resolve-locator) before
;; calling in, same trust boundary as the FHIR operators above. Every
;; entry below convicts under judge.v2's base-structural tier -- see
;; the module docstring for the candidates that did not and were
;; dropped. ----

(defn- v2-blank-field
  [parsed {:keys [segment segment-repeat field]}]
  (let [seg-idx (corpus-io/segment-occurrence-index parsed segment segment-repeat)
        fld-idx (corpus-io/field-index segment field)]
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
  (let [seg-idx (corpus-io/segment-occurrence-index parsed segment segment-repeat)
        fld-idx (corpus-io/field-index segment field)]
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
  (let [seg-idx (corpus-io/segment-occurrence-index parsed segment segment-repeat)
        fld-idx (corpus-io/field-index segment field)]
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
  (let [seg-idx (corpus-io/segment-occurrence-index parsed segment segment-repeat)
        fld-idx (corpus-io/field-index segment field)]
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
  (let [seg-idx (corpus-io/segment-occurrence-index parsed segment segment-repeat)
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

;; ---- event seed catalog (ADR-0176, ruled 2026-09-01) --------------
;;
;; The THIRD format, and the first one whose substrate is not a file.
;; An event operator is a pure function over a ground-truth event log
;; -- the vector `ehrt sim run --format ground-truth` prints and `ehrt
;; sim check` reads -- and it carries a NAMED DEFECT CLASS: the set of
;; `check` invariants it is built to trip. Where the ten file-level
;; entries above are LOWERING-LAYER faults (a blanked MSH-9, a
;; truncated segment: defects that come into existence only when a log
;; is lowered to bytes), these are CONTENT faults in the log itself.
;; The two catalogs are two layers, not competitors.
;;
;; Each entry declares two functions rather than one:
;;   :candidate-sites  (events) -> vector of log indices this operator
;;                     can convict at. The population-closure surface:
;;                     an empty vector is a REJECTION at application
;;                     time, never a silent no-op
;;                     (rulings.md#R-empty-population-is-red).
;;   :fn               (events site) -> events'. One site, already
;;                     drawn by `corpus.mutate` (Q3(a): exactly one
;;                     site per application, chosen by one draw).
;;
;; THIS SESSION LANDS ONE, deliberately. ADR-0176 Q8(a)'s v1 catalog is
;; the DERIVED referential family (four log-index reference fields x
;; five defect shapes, minus the cells the schema forbids) plus three
;; structural operators; this is the SPINE session, which proves the
;; whole contract end to end on one operator so the breadth session has
;; a contract to fill in rather than a design to discover. The
;; derivation itself -- and the gate that turns red when a fifth
;; reference field arrives without an operator for it, which is
;; ADR-0166's error ledger applied one layer up -- is that session's.

(defn- identity-fill-sites
  "Every `:demographic-update` with `:cause :identity-fill` whose
  `:placeholder-event-id` is an in-range log index.

  Purely structural, and that is what keeps it honest: it duplicates
  none of `check`'s own excusing logic, because
  `identity-fill-references-its-placeholder-registration` HAS none --
  unlike its `:medication-end` and `:care-plan-end` cousins, it carries
  no pre-horizon-fact escape hatch, so a resolving reference made
  dangling convicts unconditionally. That is precisely why this field
  is the spine's operator and one of those two is not."
  [events]
  (let [v (vec events)
        n (count v)]
    (vec (keep-indexed
          (fn [i e]
            (when (and (= :demographic-update (:event e))
                       (= :identity-fill (:cause e))
                       (int? (:placeholder-event-id e))
                       (< -1 (:placeholder-event-id e) n))
              i))
          v))))

(defn- phantom-placeholder-event-id
  "Repoints the site's `:placeholder-event-id` at `(count events)` --
  one past the last index, so it resolves nowhere. `(count events)` and
  not a drawn value: Q3(a) spends the operator's ONE draw on the site,
  and a second draw here would buy nothing a fixed out-of-range index
  does not already give."
  [events site]
  (let [v (vec events)]
    (assoc-in v [site :placeholder-event-id] (count v))))

(register!
 {:id :phantom-placeholder-event-id :version "1" :format :event
  :doc "Repoints one identity-fill's :placeholder-event-id at a log index that does not exist, leaving every other field and every other event exactly as it was."
  :contract {:type :violates
             ;; No ADR-NNNN token in this sentence: it is rendered
             ;; verbatim into docs/operators.md, which is
             ;; consumer-facing prose, and the link-footnote gate
             ;; (ehrt.docs-tooling.link-footnote-gate-test) rejects a
             ;; visible internal register token there. The provenance
             ;; lives in this catalog's own comments instead.
             :target "repoints a `:demographic-update`'s `:placeholder-event-id` at an index past the end of the log, violating this engine's own referential law that an identity fill cites a real `:registered` event in the same log, for the same patient, carrying `:identity :placeholder`, at or before the fill's own `:t` (the invariant `ehrt.sim-check.check/identity-fill-references-its-placeholder-registration` states)"}
  :locator-required? false
  :seed-consuming? true
  :expected-findings #{:identity-fill-references-its-placeholder-registration}
  :candidate-sites identity-fill-sites
  :fn phantom-placeholder-event-id})
