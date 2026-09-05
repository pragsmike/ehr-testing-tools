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
   ;; :reference-field -- the log-index reference an event operator
   ;; corrupts, for the DERIVED referential family only (the structural
   ;; three carry none). The acceptance suite walks the live event
   ;; schema for every int-typed reference field and requires each to be
   ;; either covered here or recorded as a declared population gap, so
   ;; this slot is what makes a fifth reference field arriving without
   ;; operators turn a gate red instead of going silently uncovered.
   [:reference-field {:optional true} :keyword]
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
;; THE REFERENTIAL FAMILY IS DERIVED, NOT HAND-LISTED, and that is
;; ADR-0166's own error ledger applied one layer up: that arc gave
;; `:medication-end` a referential invariant, did not mirror it onto
;; its structural twin `:care-plan-end`, and the asymmetry sat
;; unnoticed for three weeks. A hand-listed operator catalog reproduces
;; exactly that failure mode. Here the catalog is the cross product of
;; `referential-columns` and `referential-shapes`, so adding a column
;; adds its whole family at once -- and the acceptance suite's own
;; `every-schema-reference-field-is-covered-or-declared-empty-test`
;; walks the LIVE event schema and turns red when a fifth reference
;; field arrives with neither operators nor a declared population gap
;; behind it.
;;
;; WHICH COLUMNS ARE HERE. ALL FIVE the event schema declares, as of
;; 2026-09-05 -- and until that day three of them were empty. The other
;; three (`:cancels-event-id`, `:order-event-id` on `:medication-end`,
;; and `:start-event-id`) were POPULATION GAPS: convictable in
;; principle, but no log this repository could generate carried a
;; single candidate site for them, measured over both opt-in demo
;; configs at their own documented invocations
;; (`.agents/plans/2026-09-01-event-mutation-population-ledger.md`
;; section 6, which named the invariant that would convict each).
;;
;; THE GAP CLOSED FROM THE CORPUS SIDE AND NOT FROM HERE, which is what
;; the ledger's own "each column that gains a population turns straight
;; into its cells' operators, because the contract, the shapes and the
;; convicting invariants are already fixed" was a bet on.
;; `demos/scenarios/dense-7500/config.edn` at seed 5,
;; `--patients 20 --churn`, carries 4 `:cancel-transfer`, 6
;; `:medication-end` and 8 `:care-plan-end`, so the three columns below
;; are three data rows and one helper (`target-kind`) -- no new shape,
;; no new contract, no new invariant. The bet paid.
;;
;; THE GAP KIND IS NOT RETIRED WITH THE GAPS. A population gap stays a
;; different object from the unconvictable-operator refusal above,
;; which is about a hole in `check` rather than a hole in the corpora;
;; there are simply none open today, and the acceptance suite's own
;; `declared-population-gaps` is empty rather than deleted.

(defn- subject-id
  "The patient a log event is about: its first participant's id."
  [event]
  (:patient-id (first (:participants event))))

(defn- first-index-where
  [pred events]
  (first (keep-indexed (fn [i e] (when (pred e) i)) events)))

;; ---- the referential family ----------------------------------------

(def ^:private cancel-target-type
  "Cancel event kind -> the event kind it must reference.

  MIRRORED from `ehrt.sim-check.check`'s own private def of the same
  name, and mirrored rather than shared because `components/corpus` has
  no edge to `components/sim-check` and Q11(a) keeps
  `ehrt.sim-check.interface` un-widened. Disclosed here rather than
  left to be discovered: this is exactly the ADR-0166 shape the
  referential family is otherwise built to avoid, so a FOURTH cancel
  kind arriving in `check` and not here would leave column A quietly
  under-populated. The one thing keeping the two halves of THIS file in
  step is that `:carrier?` and `:target` are both read off this map."
  {:cancel-admit :admission
   :cancel-transfer :transfer
   :cancel-discharge :discharge})

(defn- target-kind
  "The event kind THIS carrier's reference must resolve to.

  A column whose carriers all cite one kind declares `:target` as a
  bare keyword. Column A cannot: `:cancels-event-id` rides three cancel
  kinds that cancel three DIFFERENT event classes, and
  `referential-entry` derives one operator id per (shape, column), so
  those three carriers have to be ONE column. Its `:target` is
  therefore a map, and every shape that reads a target asks this helper
  instead of reading `:target` itself -- which is what makes the map
  form cost the shapes nothing (P7, 2026-09-05)."
  [column carrier]
  (let [t (:target column)]
    (if (keyword? t) t (get t (:event carrier)))))

(def ^:private referential-columns
  "One row per CARRIER of a log-index reference, not per field name.
  `:order-event-id` appears on two event kinds convicted by DIFFERENT
  invariants and typed differently, so the carrier is what a column is
  keyed by; ADR-0176 section 2(i)'s matrix arithmetic misses that and
  its own dated addendum (b) corrects it.

  `:target` is the event kind the reference must resolve to, EITHER as
  a bare keyword or as a map from carrier event kind to target kind --
  read through `target-kind` below, never directly, and see its
  docstring for why column A needs the map form.

  `:slug` is the stem of every derived operator id for this column,
  defaulting to the field name. It is DECLARED where two columns share
  a field: B1 and B2 both carry `:order-event-id`, and `register!` is a
  bare `swap! assoc`, so two columns deriving the same `[id version]`
  would SILENTLY REPLACE one another rather than being refused. B1
  keeps the bare stem because its four ids are already published in
  docs/operators.md and are what `--operator-id` takes; B2 declares its
  own. `every-registered-event-operator-has-a-loop-row-test` is the
  gate over that, since the overwrite has no other symptom.

  `:law` is the referential law in consumer-facing prose, rendered
  verbatim into docs/operators.md as part of each derived operator's
  contract target, so it is written for a reader who has never seen
  this file."
  [{:field :placeholder-event-id
    :carrier? (fn [e] (and (= :demographic-update (:event e))
                           (= :identity-fill (:cause e))))
    :carrier "identity fill"
    :target :registered
    :nilable? true
    :invariant :identity-fill-references-its-placeholder-registration
    :law (str "an identity fill cites a real `:registered` event in the same log, "
              "for the same patient, carrying `:identity :placeholder`, at or "
              "before the fill's own `:t`")}
   {:field :order-event-id
    :carrier? (fn [e] (= :result-available (:event e)))
    :carrier "result"
    :target :order-placed
    ;; The event schema types this one a plain `:int` (its
    ;; `:medication-end` cousin is `[:maybe :int]`), so the null shape
    ;; is schema-forbidden here and Q9(a) drops that cell: a
    ;; schema-invalid mutant is convicted by Malli rather than by
    ;; `check`, which would close the loop on the wrong instrument.
    :nilable? false
    :invariant :result-references-existing-order-and-follows-it-in-time
    :law (str "a result cites a real `:order-placed` event in the same log, "
              "for the same patient, at or before the result's own `:t`")}
   {:field :cancels-event-id
    :carrier? (fn [e] (contains? cancel-target-type (:event e)))
    :carrier "cancellation"
    ;; THE ONE MAP-FORM TARGET. Three carrier kinds cancelling three
    ;; different event classes, and one column because
    ;; `referential-entry` derives one operator id per (shape, column).
    :target cancel-target-type
    ;; `:int`, not `[:maybe :int]` -- so Q9(a) drops the null cell here
    ;; for the same reason it drops B1's.
    :nilable? false
    :invariant :cancel-references-existing-uncancelled-event
    :law (str "a cancellation cites a real event of the class it cancels in the "
              "same log, for the same patient, at or before the cancellation's "
              "own `:t`")}
   {:field :order-event-id
    ;; See `:slug` in this def's docstring: B1 above carries the same
    ;; field and its ids are already published, so this column takes the
    ;; qualified stem and B1 keeps the bare one.
    :slug :medication-end-order-event-id
    :carrier? (fn [e] (= :medication-end (:event e)))
    :carrier "medication end"
    :target :medication-order
    :nilable? true
    :invariant :medication-end-references-existing-order-and-follows-it-in-time
    :law (str "a medication end cites a real `:medication-order` event in the "
              "same log, for the same patient, at or before the end's own `:t`")}
   {:field :start-event-id
    :carrier? (fn [e] (= :care-plan-end (:event e)))
    :carrier "care-plan end"
    :target :care-plan-start
    :nilable? true
    :invariant :care-plan-end-references-existing-start-and-follows-it-in-time
    :law (str "a care-plan end cites a real `:care-plan-start` event in the "
              "same log, for the same patient, at or before the end's own `:t`")}])

(defn- resolving-sites
  "Every index whose event is one of this column's carriers and whose
  reference field holds an in-range log index -- the population every
  shape below narrows further.

  Purely structural, and that is what keeps it honest: it duplicates
  none of `check`'s own excusing logic. Where an invariant HAS such
  logic (`medication-end-...` and `care-plan-end-...` both excuse a nil
  reference when the patient's own `:registered` carries a matching
  `:pre-horizon-facts` citation) a mutation at an excused site would
  convict NOTHING, and the loop's own set equality would go red rather
  than silently pass.

  COLUMNS B2 AND C ARE THOSE TWO INVARIANTS AND STILL DO NOT MIRROR IT,
  because measurement says they do not have to: at the shipped
  population every one of their sites resolves to a real ground-truth
  event and none reaches the excusing branch, measured EXHAUSTIVELY
  rather than by sample -- 106 of 106 sites convict, 0 non-matching, at
  both 20 and 40 arrivals (P7 derivation, section 2). A structural
  predicate plus an exhaustive measurement is a narrower claim than a
  mirrored predicate, and it is the honest one: if a later population
  does reach that branch, the gate says so."
  [column events]
  (let [n (count events)
        field (:field column)]
    (vec (keep-indexed
          (fn [i e]
            (when (and ((:carrier? column) e)
                       (int? (get e field))
                       (< -1 (get e field) n))
              i))
          events))))

(defn- wrong-kind-index
  "The first event in the log that is NOT of the kind this carrier's
  reference promises -- the referent the `:wrong-kind` shape repoints
  to."
  [column events carrier]
  (first-index-where #(not= (target-kind column carrier) (:event %)) events))

(defn- cross-patient-index
  "The first event of the RIGHT kind belonging to some other patient
  than this carrier's own -- the referent the `:cross-patient` shape
  repoints to, or nil when the log holds none."
  [column events carrier]
  (let [p (subject-id carrier)]
    (first-index-where #(and (= (target-kind column carrier) (:event %))
                             (not= p (subject-id %)))
                       events)))

(def ^:private referential-shapes
  "The five defect shapes, one per disjunct of the referential
  invariants' shared form -- `(nil? target)` reached by both PHANTOM
  and NULL, the kind clause by WRONG-KIND, the participant clause by
  CROSS-PATIENT, and the time clause by INVERTED-SPAN. Covering the
  invariant rather than decorating it is the point of deriving them.

  `:sites` narrows this column's resolving sites to the ones this shape
  can actually convict at; `:apply` is the one-site edit. Both take the
  column, so a new column inherits all five for free.

  `:nilable-only?` marks the one shape the event schema can forbid."
  [{:shape :phantom
    :edit "at a log index that does not exist"
    :violation "past the end of the log, where nothing is"
    :sites (fn [column events] (resolving-sites column events))
    :apply (fn [column events site]
             ;; `(count events)` and not a drawn value: Q3(a) spends
             ;; the operator's ONE draw on the site, and a second draw
             ;; here would buy nothing a fixed out-of-range index does
             ;; not already give.
             (assoc-in events [site (:field column)] (count events)))}
   {:shape :null
    :nilable-only? true
    :edit "to nil, citing nothing at all"
    :violation "to nil, so it cites nothing at all"
    :sites (fn [column events] (resolving-sites column events))
    :apply (fn [column events site] (assoc-in events [site (:field column)] nil))}
   {:shape :cross-patient
    :edit "at a real event of the right kind belonging to a DIFFERENT patient"
    :violation "at an event of the right kind belonging to a different patient"
    :sites (fn [column events]
             (filterv #(some? (cross-patient-index column events (nth events %)))
                      (resolving-sites column events)))
    :apply (fn [column events site]
             (assoc-in events [site (:field column)]
                       (cross-patient-index column events (nth events site))))}
   {:shape :wrong-kind
    :edit "at a real event of the WRONG kind"
    :violation "at an event that is not of the kind the reference promises"
    ;; Both clauses are asked PER SITE rather than once for the whole
    ;; column, because `target-kind` is a function of the carrier: on
    ;; column A a `:cancel-transfer` and a `:cancel-discharge` at two
    ;; sites of the same column want two different wrong kinds. On a
    ;; keyword-target column the per-site answer is the column-wide one,
    ;; so D and B1 are unchanged by this.
    :sites (fn [column events]
             (filterv #(some? (wrong-kind-index column events (nth events %)))
                      (resolving-sites column events)))
    :apply (fn [column events site]
             (assoc-in events [site (:field column)]
                       (wrong-kind-index column events (nth events site))))}
   {:shape :inverted-span
    :edit "backwards in time, so it happens BEFORE the event it cites"
    :violation "before its own referent in time"
    ;; The one shape that edits `:t` rather than the reference, and so
    ;; the one whose declared finding set has two members: it trips the
    ;; span's own referential invariant AND `timestamps-monotone`.
    ;; ADR-0176 section 2(iv) predicted exactly this case when it chose
    ;; a SET over a singleton, and measurement confirmed it.
    :extra-findings #{:timestamps-monotone}
    :sites (fn [column events]
             (filterv (fn [i]
                        (let [e (nth events i)
                              target (nth events (get e (:field column)))]
                          ;; A positive referent time, so the moved
                          ;; clock stays non-negative, and a referent
                          ;; that really does precede its citer -- which
                          ;; is what makes the moved clock land behind
                          ;; the citer's own predecessor and trip
                          ;; monotonicity as well.
                          (and (pos? (:t target)) (<= (:t target) (:t e)))))
                      (resolving-sites column events)))
    :apply (fn [column events site]
             (let [target (nth events (get (nth events site) (:field column)))]
               (assoc-in events [site :t] (dec (:t target)))))}])

(defn- referential-entry
  [column shape]
  {:id (keyword (str (name (:shape shape)) "-"
                     (name (or (:slug column) (:field column)))))
   :version "1"
   :format :event
   :reference-field (:field column)
   :doc (str "Repoints one " (:carrier column) "'s `" (:field column) "` "
             (:edit shape)
             ", leaving every other field and every other event exactly as it was.")
   :contract {:type :violates
              ;; No internal register token in this sentence: it is
              ;; rendered verbatim into docs/operators.md, which is
              ;; consumer-facing prose, and the link-footnote gate
              ;; rejects a visible internal token there. The provenance
              ;; lives in this catalog's own comments instead.
              :target (str "points one " (:carrier column) "'s `" (:field column) "` "
                           (:violation shape)
                           ", violating this engine's own referential law that "
                           (:law column)
                           " (the invariant `ehrt.sim-check.check/"
                           (name (:invariant column)) "` states)")}
   :locator-required? false
   :seed-consuming? true
   :expected-findings (into #{(:invariant column)} (:extra-findings shape))
   :candidate-sites (fn [events] ((:sites shape) column (vec events)))
   :fn (fn [events site] ((:apply shape) column (vec events) site))})

(doseq [column referential-columns
        shape referential-shapes
        :when (or (:nilable? column) (not (:nilable-only? shape)))]
  (register! (referential-entry column shape)))

;; ---- the structural family -----------------------------------------
;;
;; ADR-0176 section 2(i) proposes three structural operators and gives
;; each ONE convicting invariant. ITS OWN DATED ADDENDUM (c) RECORDS
;; ALL THREE CLAIMS REFUTED BY MEASUREMENT: a structural edit is not a
;; content fault confined to one field, so it cascades through the
;; state machine, and as worded each produced between one and eight
;; DIFFERENT finding sets depending on which site the draw landed on.
;; A varying set cannot be declared, and Q5(a)'s equality is not
;; negotiable -- so what gives is the breadth of `:candidate-sites`,
;; which is exactly what a candidate-site predicate is for.
;;
;; The operators below are the NARROWED ones. Each narrowing is a
;; statement about what the operator MEANS, not a fudge to make a gate
;; pass, and each is argued at its own definition.
;;
;; Two mechanisms behind the cascades are properties of the LOG FORMAT
;; rather than of these operators, and any later structural operator
;; meets them too:
;;
;;   1. DROPPING AN EVENT RENUMBERS THE LOG. Every log-index reference
;;      past the drop point silently repoints one event earlier, so a
;;      drop injects referential faults it never declared unless the
;;      indices are repaired as part of the same edit. `drop-one-event`
;;      below repairs them.
;;   2. RENAMING A PARTICIPANT MOVES THE EVENT into a phantom patient's
;;      timeline, where every patient-scoped invariant convicts the
;;      phantom for having no `:registered` first event -- correct, and
;;      part of the declared class rather than noise around it.
;;      IT ALSO SPLITS A SPAN, and that half is a property of the LOG
;;      and not of the event kind: an end event's referential law reads
;;      the patient off BOTH ends, so a reattributed START convicts a
;;      FIFTH invariant exactly when some end cites it. See the three
;;      orphan operators below, which is the shape ADR-0176's addendum
;;      (c) took on 2026-09-05 once a log that closes its spans
;;      existed to measure against.

(def ^:private reference-fields
  "Every log-index reference field, for the two structural operators
  that must reason about references without caring which is which."
  [:cancels-event-id :order-event-id :start-event-id :placeholder-event-id])

(defn- referenced-indices
  [events]
  (into #{} (mapcat (fn [e] (keep #(get e %) reference-fields))) events))

(defn- carries-reference?
  [event]
  (boolean (some #(some? (get event %)) reference-fields)))

;; --- clock-skew

(defn- clock-skew-sites
  "Every event with a strictly-earlier predecessor in its own patient's
  log, EXCEPT those whose `:t` is load-bearing for something other than
  monotonicity.

  Three exclusions, and the third is the one measurement forced. An
  event that CARRIES a reference, or that IS referenced, has its `:t`
  read by a referential invariant's time clause, so moving it convicts
  that invariant too. An event carrying an `:appointment-id` or a
  `:scheduled-t` has its `:t` read against its appointment's -- two
  ed-tuesday sites tripped `scheduled-encounter-follows-its-appointment`
  before this clause existed. Excluding all three is what makes this
  operator mean *move a clock* rather than *move a clock, and sometimes
  break a schedule*."
  [events]
  (let [referenced (referenced-indices events)
        seen (volatile! {})]
    (vec (keep-indexed
          (fn [i e]
            (let [p (subject-id e)
                  prior (get @seen p)]
              (when p (vswap! seen assoc p (:t e)))
              (when (and p prior (< prior (:t e))
                         (not (carries-reference? e))
                         (not (contains? referenced i))
                         (nil? (:appointment-id e))
                         (nil? (:scheduled-t e)))
                i)))
          events))))

(defn- clock-skew
  [events site]
  (let [p (subject-id (nth events site))
        prior (last (keep-indexed (fn [i e] (when (and (< i site) (= p (subject-id e))) (:t e)))
                                  events))]
    (assoc-in events [site :t] (dec prior))))

(register!
 {:id :clock-skew :version "1" :format :event
  :doc "Moves one event's clock behind its own predecessor's, so that patient's log runs backwards across one step. No other field and no other event changes."
  :contract {:type :violates
             :target (str "sets one event's `:t` earlier than the `:t` of the event "
                          "that precedes it in the same patient's log, violating this "
                          "engine's own guarantee that log order is emission order and "
                          "emission order is time order, so within a patient event times "
                          "never decrease (the invariant "
                          "`ehrt.sim-check.check/timestamps-monotone` states)")}
  :locator-required? false
  :seed-consuming? true
  :expected-findings #{:timestamps-monotone}
  :candidate-sites clock-skew-sites
  :fn clock-skew})

;; --- drop-registration

(defn- drop-registration-sites
  "Every non-placeholder `:registered` whose removal convicts exactly
  the declared class, and nothing else.

  This operator REPLACES ADR-0176's `drop-event`, whose stated single
  finding was measured to be between four and eight depending on the
  site. Four clauses narrow it, and the first is the one that matters
  most:

  THE PATIENT MUST HAVE AT LEAST ONE OTHER EVENT. Dropping the lone
  `:registered` of a patient who does nothing else leaves a log that
  checks CLEAN -- 5 of 33 sampled drops did exactly that. A fault
  injector reporting success while injecting nothing is ADR-0165's own
  silence one layer up, and it is what the loop's step 7 exists to
  catch; excluding those sites is how this operator never reaches it.

  A PLACEHOLDER registration is excluded because it is cited by the
  identity-fill and merge machinery, which convicts separately; a
  registration NAMED BY A MERGE likewise; and one that is the TARGET of
  any log-index reference would have its citer convicted too, which is
  the referential family's job and not this one's."
  [events]
  (let [referenced (referenced-indices events)
        merged (into #{}
                     (mapcat (fn [e] (when (= :merge (:event e))
                                       (map :patient-id (:participants e)))))
                     events)
        per-patient (frequencies (keep subject-id events))]
    (vec (keep-indexed
          (fn [i e]
            (when (and (= :registered (:event e))
                       (not= :placeholder (:identity e))
                       (not (contains? referenced i))
                       (not (contains? merged (subject-id e)))
                       (< 1 (get per-patient (subject-id e) 0)))
              i))
          events))))

(defn- drop-one-event
  "Removes the site's event AND repairs every log-index reference past
  it. The repair is not tidiness: a log index is a position, so
  dropping event `s` silently repoints every reference greater than `s`
  one event earlier, and without the repair this operator injects a
  referential defect class it never declared."
  [events site]
  (let [shorter (into (subvec events 0 site) (subvec events (inc site)))]
    (mapv (fn [e]
            (reduce (fn [acc k]
                      (let [x (get acc k)]
                        (if (and (int? x) (> x site)) (assoc acc k (dec x)) acc)))
                    e
                    reference-fields))
          shorter)))

(register!
 {:id :drop-registration :version "1" :format :event
  :doc "Removes one patient's `:registered` event, leaving the rest of their log in place, and renumbers every log-index reference that pointed past it so no other defect class rides along."
  :contract {:type :violates
             :target (str "removes the `:registered` event that opens one patient's "
                          "record while leaving the rest of that patient's events in "
                          "place, violating this engine's own laws that every patient "
                          "id named anywhere in a log belongs to a patient that log "
                          "registered, and that a patient's first event is their "
                          "registration (the invariants "
                          "`ehrt.sim-check.check/participant-ids-exist-in-run` and "
                          "`ehrt.sim-check.check/registered-is-every-patients-first-event` "
                          "state)")}
  :locator-required? false
  :seed-consuming? true
  :expected-findings #{:participant-ids-exist-in-run
                       :registered-is-every-patients-first-event}
  :candidate-sites drop-registration-sites
  :fn drop-one-event})

;; --- orphan-participant

(def ^:private therapeutic-intent-kinds
  "The event kinds `check`'s own `clinical-content-only-when-admitted`
  scopes -- DERIVED from that invariant's subject rather than
  hand-picked, so a sixth clinical kind joining it joins this operator
  with it. That is ADR-0166's error ledger applied here too: the whole
  reason the referential family above is a cross product."
  #{:procedure :observation :medication-order :diagnostic-report :care-plan-start})

(def ^:private orphan-patient-id
  "A patient id no run can mint. Fixed rather than drawn, for the same
  reason the phantom index is: Q3(a) spends the one draw on the site."
  "PID-ORPHANED-BY-MUTATION")

(def ^:private orphan-four-findings
  "The four invariants the orphan edit convicts at EVERY site, whatever
  the log has done with the event it lands on. Written once and shared
  by all three orphan operators below, so the two that add a fifth are
  visibly this set PLUS one rather than a set retyped."
  #{:clinical-content-only-when-admitted
    :every-encounter-is-opened-and-closed-or-still-open
    :participant-ids-exist-in-run
    :registered-is-every-patients-first-event})

(def ^:private orphan-four-laws
  "Those same four in consumer-facing prose, shared for the same
  reason: three contracts rendered into docs/operators.md that agree in
  `:expected-findings` must not drift apart in words."
  (str "that clinical content happens only while a patient is admitted, that "
       "every encounter is opened and closed or still open, that every patient "
       "id named in a log belongs to a patient that log registered, and that a "
       "patient's first event is their registration"))

(def ^:private span-columns
  "The referential columns whose CARRIER is a span END and whose target
  is therefore a therapeutic-intent START -- a `:medication-end` citing
  its `:medication-order`, a `:care-plan-end` citing its
  `:care-plan-start`.

  DERIVED from `referential-columns` by that overlap rather than listed
  a second time here, for ADR-0166's reason and the same one
  `therapeutic-intent-kinds` is derived: a third span joining the
  referential family joins this split with it, instead of leaving one
  operator quietly dishonest on the next log that closes it."
  (filterv #(contains? therapeutic-intent-kinds (:target %)) referential-columns))

(defn- starts-cited-by
  "Every log index this span column's own end events cite as their
  start. `resolving-sites` is what \"an end citing an index\" means
  everywhere else in this file, so it is what it means here."
  [column events]
  (into #{}
        (map #(get (nth events %) (:field column)))
        (resolving-sites column events)))

(defn- orphan-site?
  "The orphan edit's own subject: a therapeutic-intent clinical event
  naming a patient. All three orphan operators share it, and differ
  ONLY in what the log has done with the event."
  [e]
  (and (contains? therapeutic-intent-kinds (:event e))
       (some :patient-id (:participants e))))

(defn- orphan-participant-sites
  "Every therapeutic-intent clinical event naming a patient THAT NO END
  EVENT CITES.

  Scoped to therapeutic intent because the wider operator ADR-0176
  proposes -- reattribute ANY event -- was measured to produce eight
  different finding sets across sampled sites, since which invariants a
  phantom patient trips depends entirely on what kind of event was
  moved into their timeline.

  Scoped further by a LOG FACT, 2026-09-05 (R-split), and the second
  clause is why the first was not enough. `clinical-content-only-when-
  admitted`'s kind list -- which is where `therapeutic-intent-kinds`
  comes from -- CONTAINS the span starts `:medication-order` and
  `:care-plan-start`. Reattribute one of those on a log that CLOSES its
  spans and the end's own referential law, which reads the patient off
  both ends, convicts as a fifth. Neither calibration log closes a
  span, so the four-set was measured identical at every sampled site of
  both and was still not a property of the operator: `demos/scenarios/
  dense-7500/config.edn` produced three distinct sets over its 48
  sites (34 / 6 / 8).

  The remedy is a predicate, not a wider declaration: Q5(a) is set
  equality, so a five-element declaration goes red on a log that leaves
  its spans open. This operator keeps its four-set by saying `no end
  cites me` out loud; the 14 sites it gives up are taken by the two
  operators below, one per span column, which declare the five."
  [events]
  (let [cited (into #{} (mapcat #(starts-cited-by % events)) span-columns)]
    (vec (keep-indexed
          (fn [i e] (when (and (orphan-site? e) (not (cited i))) i))
          events))))

(defn- closed-start-sites
  "Every orphan site that IS the start of a span THIS column's ends
  cite -- one column, so exactly one span invariant joins the four, and
  the declared set is the same at every site by construction.

  The kind clause is redundant on a well-formed log (a column's ends
  cite its own target kind) and is asked anyway: a mutant log reaching
  this predicate has no such guarantee, and a site whose kind does not
  match would convict the referential invariant for a different reason
  than the one this operator declares."
  [column events]
  (let [cited (starts-cited-by column events)]
    (vec (keep-indexed
          (fn [i e] (when (and (cited i)
                               (orphan-site? e)
                               (= (:target column) (:event e)))
                      i))
          events))))

(defn- orphan-participant
  [events site]
  (update-in events [site :participants]
             (fn [ps] (mapv (fn [p] (if (:patient-id p)
                                      (assoc p :patient-id orphan-patient-id)
                                      p))
                            ps))))

(register!
 {:id :orphan-participant :version "1" :format :event
  :doc (str "Reattributes one clinical event that opens no span a later event "
            "closes to a patient the run never registered, leaving every other "
            "field and every other event exactly as it was.")
  :contract {:type :violates
             :target (str "renames the patient on one clinical event to an id the run "
                          "never registered, so that content is attributed to an unknown "
                          "patient, sits in no encounter, and is not preceded by an "
                          "admission or a registration -- violating four of this engine's "
                          "own laws at once: " orphan-four-laws
                          ". The event is one no end event cites, so no span is split and "
                          "no span's own referential law is disturbed")}
  :locator-required? false
  :seed-consuming? true
  ;; Four members, and every one of them is part of the sentence the
  ;; contract states rather than a cascade tolerated: a clinical event
  ;; attributed to a patient the run never registered IS unadmitted
  ;; content, in no encounter, for an unknown patient whose first event
  ;; is not a registration. Q5(a) declares a SET precisely so an
  ;; operator whose defect class genuinely has four faces can say so.
  :expected-findings orphan-four-findings
  :candidate-sites orphan-participant-sites
  :fn orphan-participant})

;; THE TWO CLOSED-START OPERATORS, one per span column, derived rather
;; than written twice for the reason every derivation in this file is:
;; a third span column joining `referential-columns` mints its third
;; operator here with no edit. They ARE `:orphan-participant` -- same
;; edit, same phantom id, same one-draw-on-the-site discipline (Q3(a))
;; -- differing only in the site predicate and in the fifth invariant
;; that predicate makes certain.
(defn- closed-start-entry
  [column]
  {:id (keyword (str "orphan-closed-" (name (:target column))))
   :version "1"
   :format :event
   :doc (str "Reattributes one `" (name (:target column)) "` that a later "
             (:carrier column) " cites to a patient the run never registered, "
             "leaving every other field and every other event exactly as it was.")
   :contract {:type :violates
              :target (str "renames the patient on one `" (name (:target column))
                           "` that a later " (:carrier column) " cites, so that content "
                           "is attributed to an unknown patient, sits in no encounter, "
                           "and is not preceded by an admission or a registration, AND so "
                           "that the two ends of the span no longer name the same patient "
                           "-- violating five of this engine's own laws at once: "
                           orphan-four-laws ", and the referential law that "
                           (:law column) " (the invariant `ehrt.sim-check.check/"
                           (name (:invariant column)) "` states)")}
   :locator-required? false
   :seed-consuming? true
   :expected-findings (conj orphan-four-findings (:invariant column))
   :candidate-sites (fn [events] (closed-start-sites column (vec events)))
   :fn orphan-participant})

(doseq [column span-columns]
  (register! (closed-start-entry column)))
