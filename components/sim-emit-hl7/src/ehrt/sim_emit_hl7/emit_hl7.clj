(ns ehrt.sim-emit-hl7.emit-hl7
  "EmitHL7 (docs/sim-theory.edn): pure log -> ER7 messages, the thin
  vertical slice from ground-truth-log to hl7v2-stream. v0 scope was
  ADT^A01 (admission) and ADT^A03 (discharge) only; Milestone M1
  (docs/operational-models.md) adds ADT^A02 (transfer, including bed-
  ready) alongside its step type, per the roadmap's own co-landing
  extension of that rule to this registry. MSH/EVN/PID/PV1 populated
  minimally -- on org.clojars.cmiles74/clojure-hl7-parser's own data
  structures (the only runtime dependency this stage adds).

  Consumes the ground-truth log ONLY: no RNG, no wall clock
  (determinism law). facility/providers are additional PINNED,
  non-random inputs (like :reference-date and :utc-offset already are)
  needed to render PV1-3/6's ward^^bed^facility shape and PV1-7's
  attending -- passing them doesn't touch the no-RNG/no-wall-clock
  doctrine, since none is sampled here, only rendered. Every timestamp
  is rendered from the pinned :reference-date run-config input plus
  the event's log-relative SECOND offset (`sim/ADR-0011`; was minutes before
  M2a), suffixed with the pinned :utc-offset (`sim/ADR-0011`: a fixed offset,
  never a timezone-database lookup, never per-event) -- never from
  System/currentTimeMillis or similar. PID-3 renders the event's own
  :active-mrn (`sim/ADR-0010`: MRN moved into state; the emitter renders
  whichever MRN was active when the event happened, which until M2b's
  merge exists is always the patient's one and only MRN)."
  (:require [com.nervestaple.hl7-parser.parser :as parser]
            [clojure.string :as str]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-emit-hl7.site-profile :as site-profile]))

(def default-reference-date
  "Pinned default for the :reference-date run-config input (an ISO
  date string, midnight local time is the run's t=0). Arbitrary but
  fixed -- determinism does not care which date, only that every run
  states one explicitly (never buried in :invocation, per tools'
  ManifestV1 lesson)."
  "2024-01-01")

(def default-utc-offset
  "Pinned default for the :utc-offset run-config input (`sim/ADR-0011`): a
  fixed ISO-style offset (\"+00:00\"), no DST, no timezone database.
  Rendered in HL7v2's own colon-free zone-suffix convention
  (\"+0000\") -- see `hl7-timestamp`."
  "+00:00")

(def message-type-registry
  "Event type -> HL7 message type/trigger: the emitter's own catalytic
  catalog (docs/sim-theory.edn, catalytic target 4). A new engine step
  type earns an entry here in the same change that adds it (the same
  co-landing convention check.clj's catalog already follows -- and, per
  Milestone M1's roadmap note, extended to this registry too: a step
  type without a message-type entry produces traffic invisible to every
  consumer downstream of this stage)."
  {:admission {:type "ADT" :trigger "A01"}
   :discharge {:type "ADT" :trigger "A03"}
   :transfer {:type "ADT" :trigger "A02"}
   ;; M2b churn family. :transfer-in-error has no entry of its own -- its
   ;; decide emits ordinary :transfer + :cancel-transfer events (already
   ;; registered), never a distinct ground-truth :event value.
   :cancel-admit {:type "ADT" :trigger "A11"}
   :cancel-transfer {:type "ADT" :trigger "A12"}
   :cancel-discharge {:type "ADT" :trigger "A13"}
   :bed-swap {:type "ADT" :trigger "A17"}
   :merge {:type "ADT" :trigger "A40"}
   ;; M3: order/result. :step-rejected has NO entry, by design (sim/ADR-0012:
   ;; truth about the run, never wire traffic -- no real ADT/ORM/ORU feed
   ;; carries a message for an attempt that never became a real action).
   :order-placed {:type "ORM" :trigger "O01"}
   :result-available {:type "ORU" :trigger "R01"}
   ;; M5b (components/sim-trajectory/docs/gmf-interpreter.md section 4's sketch, item 5): the new
   ;; outpatient encounter class. :outpatient-visit-end has NO entry, by
   ;; design (item 7 -- the same sim/ADR-0012 :step-rejected precedent: many
   ;; real ambulatory feeds send a single A04 and no closing message for a
   ;; same-day visit; inventing a discharge-shaped message here would be
   ;; manufacturing wire traffic no real interface sends).
   :outpatient-visit {:type "ADT" :trigger "A04"}
   ;; M5b (components/sim-trajectory/docs/gmf-interpreter.md section 1's table): :observation is an
   ;; UNSOLICITED finding, not an order's result -- same ORU^R01 message
   ;; family as :result-available, rendered WITHOUT the ORC/OBR order
   ;; context that doesn't exist for it (a real, legal ORU shape).
   ;; :procedure/:medication-order/:medication-end deliberately get NO
   ;; entry here -- truth-only ground-truth facts this milestone, the same
   ;; treatment ConditionOnset/ConditionEnd's own DG1/billing rendering
   ;; already gets (gated on snomed-icd10-map landing, not built yet): a
   ;; real message shape for procedures/medications is its own future
   ;; catalytic/segment-design work, not a same-session add.
   ;; GMF coverage Wave D stage D2 (2026-08-02, ADR-0029 R3): :care-plan-
   ;; start/:care-plan-end deliberately get NO entry here either, same
   ;; precedent -- CarePlan's own natural rendering is a FHIR CarePlan
   ;; resource, once sim-emit-fhir exists, not an HL7v2 shape invented
   ;; for a format with no real CarePlan-equivalent segment (M3/M5b's
   ;; own truth-only-facts treatment, R3's own ruling text verbatim).
   :observation {:type "ORU" :trigger "R01"}
   ;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P6): a real
   ;; DiagnosticReport panel IS an ORU^R01 with ORC+OBR present (unlike
   ;; :observation's own order-less shape) -- the same trigger
   ;; :result-available/:observation already use.
   :diagnostic-report {:type "ORU" :trigger "R01"}})

(def ^:private hl7-timestamp-formatter
  (java.time.format.DateTimeFormatter/ofPattern "yyyyMMddHHmmss"))

(defn- reference-instant
  [reference-date]
  (.atStartOfDay (java.time.LocalDate/parse reference-date)))

(defn- hl7-offset-suffix
  "ISO-style offset (\"+00:00\", \"-05:00\") rendered in HL7v2's own
  zone-suffix convention: colon-free (\"+0000\", \"-0500\")."
  [utc-offset]
  (str/replace utc-offset ":" ""))

(defn hl7-timestamp
  "Renders the absolute HL7 timestamp for `seconds` (a log event's :t,
  SECONDS from the run's epoch -- sim/ADR-0011, was minutes before M2a)
  anchored to :reference-date, suffixed with :utc-offset in HL7's own
  colon-free zone convention -- the timestamp-anchoring law, extended
  to state which fixed offset the naive wall-clock arithmetic is
  asserted to be in (no timezone database, no DST: the arithmetic
  itself never shifts across zones, sim/ADR-0011). Pure: reference-date +
  seconds + utc-offset in, string out, nothing else consulted."
  [reference-date seconds utc-offset]
  (str (.format (.plusSeconds (reference-instant reference-date) seconds) hl7-timestamp-formatter)
       (hl7-offset-suffix utc-offset)))

(defn control-id-for
  "MSH-10 (message control id) for one ground-truth event -- the SAME
  construction every message-builder call site below uses, extracted
  once (post-M6, sim/ADR-0014's own `sim identifiers` verb reuses this
  exact function so its own inventory can never drift from what a real
  emission actually renders): `active-mrn` and the event's own trigger
  and `:t` for every single-subject type; `mrn1+mrn2` for :bed-swap
  (genuinely two participants, no single :active-mrn to key on);
  `surviving-mrn` for :merge (PID carries the survivor, not the one
  being merged away). nil for any event type outside
  `message-type-registry` (the same 'no message, no id' rule
  `event->messages` already follows)."
  [{:keys [event t active-mrn surviving-mrn participants swap]}]
  (when-let [{:keys [trigger]} (message-type-registry event)]
    (case event
      :bed-swap
      (let [[p1 p2] (mapv :patient-id participants)]
        (str (:active-mrn (get swap p1)) "+" (:active-mrn (get swap p2)) "-" trigger "-" t))

      :merge
      (str surviving-mrn "-" trigger "-" t)

      (str active-mrn "-" trigger "-" t))))

(defn- msh-segment
  "MSH-3/4/5/6/12 (sending/receiving app+facility, version id) render
  `site-profile`'s :msh dialect, defaulting field-by-field to today's
  hard-coded values (ehrt.sim-emit-hl7.site-profile/default-msh) when
  `site-profile` is nil, {}, or simply doesn't override that field --
  Milestone site-profiles Task 2 (SimHospital issue #17's own citation,
  .agents/plans/roadmap.md: a configured field, not a hard-coded
  emitter constant)."
  [site-profile {:keys [type trigger]} control-id ts]
  (let [{:keys [version sending-app sending-facility receiving-app receiving-facility processing-id]}
        (site-profile/effective-msh site-profile)]
    (parser/create-segment
     "MSH"
     (parser/create-field (parser/pr-delimiters parser/DEFAULT-DELIMITERS))
     (parser/create-field [sending-app])
     (parser/create-field [sending-facility])
     (parser/create-field [receiving-app])
     (parser/create-field [receiving-facility])
     (parser/create-field [ts])
     (parser/create-field [])
     (parser/create-field [type trigger])
     (parser/create-field [control-id])
     (parser/create-field [processing-id])
     (parser/create-field [version]))))

(defn- evn-segment
  [trigger ts]
  (parser/create-segment
   "EVN"
   (parser/create-field [trigger])
   (parser/create-field [ts])))

;; --- M4 Task 4: ER7 escaping (`sim/F9`) -----------------
;; org.clojars.cmiles74/clojure-hl7-parser implements NO escape-sequence
;; handling in either direction, verified directly against its own source:
;; pr-field/pr-content (the write path) concatenate field content into the
;; wire string with no encoding step at all; read-text's and read-
;; subcomponents' escape-handling branches (the read path) are commented-out
;; dead code, and `delimiter?` doesn't even exempt the escape character from
;; ending a token early. A literal |^~& character embedded in free text
;; therefore corrupts the message's own field/component boundaries on parse
;; unless something upstream escapes it, and even a properly-escaped value
;; comes back from `get-field-first-value` STILL escaped, never decoded.
;; escape-er7/unescape-er7 are this repo's own documented workaround:
;; encode on write (below, at every persona-derived free-text field), decode
;; on read (a consumer's own job, exactly like this repo's test suite does).

(def ^:private er7-escape-table
  "Order matters on ENCODE: the escape character itself is escaped
  FIRST, or the backslashes this table's own replacements introduce
  for |^~& would themselves get escaped a second time on a later pass."
  [[\\ "\\E\\"] [\| "\\F\\"] [\^ "\\S\\"] [\~ "\\R\\"] [\& "\\T\\"]])

(defn escape-er7
  "Encodes ER7's five reserved delimiter characters per the standard
  escape-sequence convention. Identity for any string containing none
  of the five -- the overwhelmingly common case (ordinary names,
  apostrophes, and hyphens need no escaping at all, ER7 or otherwise).
  Safe as five sequential single-CHARACTER replacements (unlike decode,
  below): each pass targets one literal input character never produced
  by an earlier pass's own replacement text (F/S/R/T/E are never
  themselves |^~&), so passes cannot collide."
  [s]
  (reduce (fn [acc [ch replacement]] (str/replace acc (str ch) replacement))
          s er7-escape-table))

(def ^:private er7-decode-map
  {\E \\ \F \| \S \^ \R \~ \T \&})

(defn unescape-er7
  "Decodes ER7 escape sequences back to literal characters -- the
  consumer-side half of this namespace's own documented workaround for
  the parser's read-side gap (see this section's header comment).

  MUST be a single regex pass, not five sequential string replacements
  the way `escape-er7` is -- a property-test failure caught exactly
  this during Milestone M4's own authoring: encoding \"|E|\" produces
  \"\\F\\E\\F\\\" (backslash F backslash E backslash F backslash), and
  five SEPARATE global replaces are each blind to what the others
  already consumed, so the first pass (decoding \\E\\ back to a literal
  backslash) spuriously matches the backslash-E-backslash formed by the
  BOUNDARY between the two adjacent, unrelated \\F\\ tokens -- decoding
  it wrong. A single regex scan matches real three-character tokens
  left to right, consuming each match's characters before continuing,
  so two adjacent tokens can never accidentally spell a third."
  [s]
  (str/replace s #"\\[EFRST]\\" (fn [^String match] (str (er7-decode-map (.charAt match 1))))))

(defn- xpn-field
  "XPN (Extended Person Name), PID-5: family^given. Free text from
  ehrt.sim-model.persona -- escaped per ER7 (see this file's Task 4
  section) before it ever reaches a field, since the library itself
  never will."
  [{:keys [family given]}]
  (parser/create-field [(escape-er7 family) (escape-er7 given)]))

(defn- xad-field
  "XAD (Extended Address), PID-11: street^other-designation^city^state^zip.
  Other-designation (apt/suite) is always empty -- resources/demographics'
  vendored places carry no such field, same simplification the address
  table's own header notes. Free text escaped per ER7, same reasoning
  as `xpn-field`."
  [{:keys [street city state zip]}]
  (parser/create-field [(escape-er7 street) "" (escape-er7 city) (escape-er7 state) (escape-er7 zip)]))

(defn- pid-segment
  "PID-1/2/3 unconditionally (Set ID, blank, the active MRN); PID-4/6/9/10/12
  stay blank placeholders so positional fields (5/7/8/11/13) land correctly.
  M4: when `persona` is present (every real ehrt.sim-engine.engine/run output,
  post the :registered event -- ehrt.sim-model.persona/Persona), PID gains
  demographic enrichment: PID-5 (XPN name), PID-7 (DOB, HL7 date), PID-8 (sex,
  Table 0001 F/M), PID-11 (XAD address), PID-13 (phone). nil persona (hand-
  built test worlds that never processed a :registered step) falls back to
  the pre-M4 3-field segment exactly -- no positional padding, no crash."
  [active-mrn persona]
  (if (nil? persona)
    (parser/create-segment
     "PID"
     (parser/create-field ["1"])
     (parser/create-field [])
     (parser/create-field [active-mrn]))
    (parser/create-segment
     "PID"
     (parser/create-field ["1"])
     (parser/create-field [])
     (parser/create-field [active-mrn])
     (parser/create-field [])
     (xpn-field (:name persona))
     (parser/create-field [])
     (parser/create-field [(str/replace (:dob persona) "-" "")])
     (parser/create-field [(case (:sex persona) :female "F" :male "M")])
     (parser/create-field [])
     (parser/create-field [])
     (xad-field (:address persona))
     (parser/create-field [])
     (parser/create-field [(:phone persona)]))))

(defn- in1-segment
  "IN1 (insurance): IN1-1 set id, IN1-3/IN1-4 the sampled payer pool
  entry's id/name (docs/operational-models.md's payers model, Milestone
  M4 -- SimHospital issue #3's own request, docs/research/SimHospital-
  Synthea-limitations-considered.md §5.3). Rides ONLY the admission
  message (single-subject-message's own call site) -- the real HL7v2
  convention: insurance coverage is registered once, at admission, not
  restated on every subsequent ADT event."
  [{payer-id :id payer-name :name}]
  (parser/create-segment
   "IN1"
   (parser/create-field ["1"])
   (parser/create-field [])
   (parser/create-field [payer-id])
   (parser/create-field [(escape-er7 payer-name)])))

(defn- personas-by-patient-id
  "patient-id -> persona, derived directly from the log's own
  :registered events (sim/ADR-0012's own precedent: a stage's own state is
  recoverable by scanning the log, no second input needed). Computed
  once per `emit` call and threaded down to every segment builder that
  needs it -- pid-segment enrichment applies uniformly across every
  message type, not just admission."
  [ground-truth]
  (into {}
        (comp (filter #(= :registered (:event %)))
              (map (fn [ev] [(:patient-id (first (:participants ev))) (:persona ev)])))
        ground-truth))

(defn- location-field
  "Renders a location map as ward^^bed^facility (PV1-3/PV1-6's shared
  shape, docs/operational-models.md's transfer/A02 spec: 'PV1-3 renders
  ward^^bed with facility in PV1-3.4'). nil location (no prior, or a
  v0 event with no location at all) -> an empty field, same as v0's
  own nil-location handling."
  [facility-name location]
  (if-let [ward (:ward location)]
    (parser/create-field [ward "" (or (:bed location) "") facility-name])
    (parser/create-field [])))

(defn- provider-field
  "PV1-7: id^family^given. nil provider -> empty field."
  [provider]
  (if provider
    (parser/create-field [(:id provider) (get-in provider [:name :family]) (get-in provider [:name :given])])
    (parser/create-field [])))

(defn- provider-by-id
  [providers id]
  (first (filter #(= id (:id %)) providers)))

(defn- mrg-segment
  "MRG-1: the prior (merged-away) patient identifier -- A40's own carrier
  for 'what mrn did this patient answer to before' (docs/patient-state-
  model.md's identity payoff). PID (built via pid-segment, same as every
  other type) carries the SURVIVING mrn."
  [merged-mrn]
  (parser/create-segment "MRG" (parser/create-field [merged-mrn])))

(defn- blank-fields
  [n]
  (repeat n (parser/create-field [])))

(defn- pv1-segment
  "PV1-6 (prior location) is read directly off the CURRENT event's own
  :from -- present only on :transfer events -- never a separately
  maintained prior-location field on patient state (docs/patient-
  state-model.md's Simulated Hospital lesson: one :location field plus
  the log's own facts replaces a shadow-field zoo).

  Milestone site-profiles Task 2: PV1-2 (patient class) renders through
  `site-profile`'s :patient-class code-table override when present,
  `patient-class` (a keyword, `standard-patient-class-codes`'s own
  vocabulary) otherwise -- every call site but M5b's own
  :outpatient-visit passes :inpatient, the only class this project
  produced before this milestone (docs/patient-state-model.md). PV1-36
  (discharge disposition) renders the SAME way, but only when
  `disposition-state` is non-nil -- callers pass a state keyword
  (:discharged-to-home) only for :discharge events; every other event
  type passes nil, rendering PV1-36 empty, exactly as before this
  milestone (no disposition concept existed to render at all)."
  [site-profile patient-class facility-name location from provider disposition-state]
  (apply parser/create-segment
         "PV1"
         (parser/create-field ["1"])
         (parser/create-field (site-profile/code-for site-profile :patient-class
                                                      site-profile/standard-patient-class-codes patient-class))
         (location-field facility-name location)
         (parser/create-field [])
         (parser/create-field [])
         (location-field facility-name from)
         (provider-field provider)
         (concat (blank-fields 28)
                 [(if disposition-state
                    (parser/create-field (site-profile/code-for site-profile :discharge-disposition
                                                                 site-profile/standard-discharge-disposition-codes
                                                                 disposition-state))
                    (parser/create-field []))])))

;; --- Milestone site-profiles Task 3: Z-segment templates -- THE SEAM -----
;; A site's fully custom fields (docs/site-profiles.md), bound declaratively
;; to state/persona/event paths rather than hard-coded engine knowledge of
;; what any particular site's Z-segment means.

(defn- context-for-event
  "The per-render lookup context a Z-segment template's `:path` bindings
  resolve against (get-in): the event map itself (so [:location :ward],
  [:attending], [:t], etc. all resolve directly, the same paths
  docs/site-profiles.md's own examples name) plus :persona -- looked up
  off the SAME `personas` map `emit` computes once per call, the primary
  (first) participant's persona for a genuinely multi-participant event
  (bed-swap, merge), the same simplification `ehrt.sim-engine.engine/
  replay`'s own :patient-id convenience view already makes."
  [personas event]
  (assoc event :persona (get personas (:patient-id (first (:participants event))))))

(defn- render-z-field
  "One Z-segment field: `:path` looked up (get-in -- nil-safe through a
  missing intermediate map, so an unbound path never throws) in
  `context`, `:literal` as a fixed fallback, an EMPTY field when
  neither resolves to a value -- Task 3's own never-throw requirement.
  Escaped per ER7, same as every other free-text-carrying field this
  namespace renders (persona names, addresses, payer names)."
  [context {:keys [path literal]}]
  (let [value (if path (get-in context path) literal)
        rendered (cond (nil? value) nil (keyword? value) (name value) :else (str value))]
    (parser/create-field (if rendered [(escape-er7 rendered)] []))))

(defn- z-segment-for
  [context template]
  (apply parser/create-segment (:segment template)
         (mapv (partial render-z-field context) (:fields template))))

(defn- z-segments-for
  "0+ rendered Z-segments for `event` -- one per `site-profile`'s own
  :z-segments template whose :trigger set names this event's :event, in
  the profile's own template order. Rendered AFTER every standard
  segment at every call site below (Task 3's own ordering requirement,
  achieved by `concat`-ing this vector onto the end of each message's
  segment list). No site-profile, or a profile with no :z-segments,
  renders none -- an empty vector `concat`s as a no-op, so absent-
  profile output is untouched by this function's existence."
  [site-profile personas event]
  (let [context (context-for-event personas event)]
    (into []
          (comp (filter #(contains? (:trigger %) (:event event)))
                (map (partial z-segment-for context)))
          (:z-segments site-profile))))

(defn- transmit-seconds
  "ADR-0109's own second clock: `t` (the event's clinical instant, log-
  relative seconds) shifted by `offsets`' own entry for this event's
  `control-id`, or unshifted (offset 0) when `control-id` has no entry
  -- absent/nil/{} `offsets` is therefore the identity input for every
  event, the mechanism the identity property (emit-hl7-test) rests on.
  `offsets` is plain data here (never an RNG) -- sampling stays out of
  emit, per this namespace's own renders-only doctrine (docs/dev/
  simulator-architecture.md section 5); `plan-latency` is the one place
  offsets are ever sampled, upstream of this function."
  [offsets control-id t]
  (+ t (long (get offsets control-id 0))))

(defn- single-subject-message
  "Renders one single-participant ground-truth event to an ER7 string,
  or nil when the event's :event isn't in `message-type-registry`. Every
  type this covers (:admission/:discharge/:transfer and M2b's cancel-*
  family) carries its own :active-mrn/:location/:from/:attending
  directly -- cancel events reinstate these AT DECIDE-TIME by querying
  the log (docs/patient-state-model.md), so this renderer needs no
  event-type-specific branching to show the reinstated facts. M4: IN1
  rides ONLY :admission (`in1-segment`'s own docstring); every type
  here gets PID enrichment uniformly via `personas`. Milestone
  site-profiles: PV1-36 disposition rides ONLY :discharge (the same
  single-event-type gate IN1 already established for admission), and
  every segment renders through `site-profile`'s own dialect/code-table/
  Z-segment surfaces -- `z-segments-for`'s own docstring.

  ADR-0109's split clock (this session's own field audit, docs/dev/
  simulator-architecture.md section 5 / notes/adr/0109-*.md): MSH-7 is
  TRANSMIT time (`clinical-ts` shifted by `offsets`), EVN-2 is CLINICAL
  time (`clinical-ts`, unshifted) -- the only two timestamp-bearing
  fields this builder ever renders. `offsets` is {} at every plain-
  `emit` call site, so `transmit-ts` = `clinical-ts` always there,
  byte-identical to this builder's pre-ADR-0109 shape."
  [reference-date utc-offset facility providers personas site-profile offsets
   {:keys [event t active-mrn location from attending participants] :as ev}]
  (when-let [type+trigger (message-type-registry event)]
    (let [control-id (control-id-for ev)
          clinical-ts (hl7-timestamp reference-date t utc-offset)
          transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
          facility-name (name (:id facility))
          provider (provider-by-id providers attending)
          persona (get personas (:patient-id (first participants)))
          disposition-state (when (= :discharge event) :discharged-to-home)
          ;; M5b: the only two event types this project ever renders
          ;; :outpatient for -- every other type here is still :inpatient
          ;; (this project's own sole class before this milestone).
          patient-class (if (#{:outpatient-visit :outpatient-visit-end} event) :outpatient :inpatient)]
      (parser/str-message
       (apply parser/create-message
        parser/DEFAULT-DELIMITERS
        (msh-segment site-profile type+trigger control-id transmit-ts)
        (evn-segment (:trigger type+trigger) clinical-ts)
        (pid-segment active-mrn persona)
        (pv1-segment site-profile patient-class facility-name location from provider disposition-state)
        (concat (when (and (= :admission event) persona) [(in1-segment (:payer persona))])
                (z-segments-for site-profile personas {:event event :t t :active-mrn active-mrn
                                                       :location location :from from :attending attending
                                                       :participants participants})))))))

(defn- bed-swap-message
  "A17 (swap patients): ONE message per ground-truth event, carrying
  BOTH patients' PID/PV1 pairs -- the real HL7v2 A17 shape, and why the
  emitter-derivability law now keys on the event's own log position
  rather than a single :active-mrn (a bed-swap message has two).
  ADR-0109's split clock (see `single-subject-message`'s own docstring):
  ONE `control-id` covers the whole event (`control-id-for`'s own
  :bed-swap arm), so both patients' PID/PV1 pairs ride the SAME
  transmit-shifted MSH-7 and the SAME unshifted EVN-2."
  [reference-date utc-offset facility providers personas site-profile offsets
   {:keys [t participants swap] :as ev}]
  (let [type+trigger (message-type-registry :bed-swap)
        control-id (control-id-for ev)
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        [p1 p2] (mapv :patient-id participants)
        {mrn1 :active-mrn from1 :from to1 :to att1 :attending} (get swap p1)
        {mrn2 :active-mrn from2 :from to2 :to att2 :attending} (get swap p2)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (evn-segment (:trigger type+trigger) clinical-ts)
      (pid-segment mrn1 (get personas p1))
      (pv1-segment site-profile :inpatient facility-name to1 from1 (provider-by-id providers att1) nil)
      (pid-segment mrn2 (get personas p2))
      (pv1-segment site-profile :inpatient facility-name to2 from2 (provider-by-id providers att2) nil)
      (z-segments-for site-profile personas ev)))))

(defn- merge-message
  "A40 (merge patient): PID carries the SURVIVING mrn, MRG-1 carries the
  prior (merged-away) one (docs/patient-state-model.md's identity
  payoff) -- ONE message per merge event. ADR-0109's split clock (see
  `single-subject-message`'s own docstring): `control-id-for`'s own
  :merge arm keys on the surviving mrn."
  [reference-date utc-offset facility _providers personas site-profile offsets
   {:keys [t surviving-mrn merged-mrn participants] :as ev}]
  (let [type+trigger (message-type-registry :merge)
        control-id (control-id-for ev)
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        survivor-id (:patient-id (first (filter #(= :survivor (:role %)) participants)))]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (evn-segment (:trigger type+trigger) clinical-ts)
      (pid-segment surviving-mrn (get personas survivor-id))
      (pv1-segment site-profile :inpatient facility-name nil nil nil nil)
      (mrg-segment merged-mrn)
      (z-segments-for site-profile personas ev)))))

;; --- M3: ORM^O01 + ORU^R01 (docs/sim-theory.edn's order-profiles
;; catalytic, docs/operational-models.md) -----------------------------------

(defn- cwe-field
  "CWE (Coded With Exceptions): identifier^text^coding-system. \"LN\" is
  LOINC's own HL7v2 Table 0396 coding-system abbreviation -- the coded-
  triplet's :system rendered natively (sim/ADR-0002), not translated.
  Every existing call site (OBR-4/OBX-3) is always a LOINC panel/
  analyte concept, so this stays hardcoded -- `coded-value-field`,
  below, is the system-aware sibling a value_code-sourced OBX-5 needs."
  [{:keys [code display]}]
  (parser/create-field [code display "LN"]))

;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P6): a value_code-
;; sourced observation (Blood_Cultures' own embedded child, Capillary_
;; Refill) is this project's first field ever to carry a SNOMED CT-coded
;; VALUE, not just a LOINC-coded concept -- `cwe-field` itself stays
;; LOINC-hardcoded and untouched.
(def ^:private code-system->hl7-table-0396
  "HL7v2 Table 0396 coding-system abbreviations for sim-model/Concept's
  own :system vocabulary (sim/ADR-0002)."
  {:loinc "LN" :snomed "SCT" :rxnorm "RXNORM" :icd10cm "I10" :cvx "CVX"})

(defn- coded-value-field
  "OBX-5 for a value_code-sourced observation: identifier^text^coding-
  system, the SAME CWE shape `cwe-field` renders for OBR-4/OBX-3, but
  system-aware."
  [{:keys [system code display]}]
  (parser/create-field [code display (get code-system->hl7-table-0396 system (name system))]))

(defn- orc-segment
  "ORC-1: order control -- \"NW\" (new order) is the only value this
  stage ever emits, since InjectChurn has no order-cancellation step
  (v1 scope). ORC-2: placer order number, reusing this message's own
  control-id (no separate order-numbering scheme needed yet)."
  [control-id]
  (parser/create-segment
   "ORC"
   (parser/create-field ["NW"])
   (parser/create-field [control-id])))

(defn- obr-segment
  "OBR-4: universal service id -- the PANEL-level LOINC concept (CBC/BMP
  itself, not its analytes; those are per-OBX below)."
  [set-id concept]
  (parser/create-segment
   "OBR"
   (parser/create-field [(str set-id)])
   (parser/create-field [])
   (parser/create-field [])
   (cwe-field concept)))

(defn- obx-segment
  "One analyte per OBX (docs/operational-models.md's own spec for this
  milestone): OBX-2 \"NM\" (numeric) -- every analyte in this
  catalytic's starter set is a numeric lab value, no other value types
  needed yet. OBX-7 renders the reference range as \"low-high\"; OBX-8
  the abnormal flag, HL7v2's own N/L/H vocabulary (Table 0078) -- a
  direct rendering of ehrt.sim-engine.order-profiles/abnormal-flag's
  own :normal/:low/:high, computed truth carried straight from the log,
  never re-derived at emit time (the log already has the answer)."
  [set-id {:keys [concept units value reference-range abnormal-flag]}]
  (parser/create-segment
   "OBX"
   (parser/create-field [(str set-id)])
   (parser/create-field ["NM"])
   (cwe-field concept)
   (parser/create-field [])
   (parser/create-field [(str value)])
   (parser/create-field [units])
   (parser/create-field [(str (:low reference-range) "-" (:high reference-range))])
   (parser/create-field [(case abnormal-flag :normal "N" :low "L" :high "H")])))

(defn- orm-message
  "ORM^O01: order placed. No EVN segment -- EVN is an ADT-specific
  segment (HL7v2 convention), not part of the order-message family.
  ADR-0109's split clock: MSH-7 is this builder's ONLY timestamp field
  (this session's field audit: OBR-7 and ORC-9, HL7v2's own clinical-
  time candidates for this message family, are not rendered by
  `orc-segment`/`obr-segment` at all), so it is unconditionally
  transmit time -- there is no clinical-time field here to keep
  unshifted."
  [reference-date utc-offset facility providers personas site-profile offsets
   {:keys [t active-mrn location attending concept participants] :as ev}]
  (let [type+trigger (message-type-registry :order-placed)
        control-id (control-id-for ev)
        transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        provider (provider-by-id providers attending)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (pid-segment active-mrn (get personas (:patient-id (first participants))))
      (pv1-segment site-profile :inpatient facility-name location nil provider nil)
      (orc-segment control-id)
      (obr-segment 1 concept)
      (z-segments-for site-profile personas ev)))))

(defn- oru-message
  "ORU^R01: result available -- OBR (order context) plus one OBX per
  analyte, in the same order the profile's own :results carries them
  (derived straight from the log, ehrt.sim-engine.order-profiles'
  sampling order -- no re-sorting here). ADR-0109's split clock: MSH-7
  is this builder's ONLY timestamp field (this session's field audit:
  OBR-7/OBX-14, HL7v2's own clinical-time candidates, are not rendered
  by `obr-segment`/`obx-segment` at all), so it is unconditionally
  transmit time."
  [reference-date utc-offset facility providers personas site-profile offsets
   {:keys [t active-mrn location attending concept results participants] :as ev}]
  (let [type+trigger (message-type-registry :result-available)
        control-id (control-id-for ev)
        transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        provider (provider-by-id providers attending)
        obx-segments (map-indexed (fn [i r] (obx-segment (inc i) r)) results)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (pid-segment active-mrn (get personas (:patient-id (first participants))))
      (pv1-segment site-profile :inpatient facility-name location nil provider nil)
      (orc-segment control-id)
      (obr-segment 1 concept)
      (concat obx-segments (z-segments-for site-profile personas ev))))))

;; --- M5b: :observation -> ORU^R01, OBX only (components/sim-trajectory/docs/gmf-interpreter.md
;; section 1's table) -------------------------------------------------------

(defn- observation-obx-segment
  "OBX-3 is the FIRST of :codes (components/sim-trajectory/docs/gmf-interpreter.md section 1: a GMF
  Observation's own concept), OBX-5 the sampled :value when present
  (some Observation states carry no :range, hence no value -- an empty
  field, never a fabricated one), OBX-6 :unit. GMF coverage Wave D
  stage D1 (2026-08-02, ADR-0029 P6, extended past its own base sketch
  per the D1a schema RULING's Q2+Q3): OBX-2 branches \"CWE\"/\"NM\" on
  whether the observation carries :value-code (rendered via
  `coded-value-field`, OBX-5), and OBX-7/OBX-8 (reference-range/
  abnormal-flag) are appended ONLY when the observation carries them
  (the vital-sign reference table's own contribution, D1 F2) -- byte-
  identical to every pre-existing call (range-sourced or codes-only,
  neither field ever present) when absent, this stage's OWN emitter-
  extension discipline (never a positional pad for a field nothing
  supplies)."
  [set-id {:keys [codes value unit value-code reference-range interpretation]}]
  (apply parser/create-segment
   "OBX"
   (concat
    [(parser/create-field [(str set-id)])
     (parser/create-field [(if value-code "CWE" "NM")])
     (cwe-field (first codes))
     (parser/create-field [])
     (if value-code (coded-value-field value-code) (parser/create-field (if (some? value) [(str value)] [])))
     (parser/create-field (if unit [unit] []))]
    (when (or reference-range interpretation)
      [(parser/create-field (if reference-range [(str (:low reference-range) "-" (:high reference-range))] []))
       (parser/create-field (if interpretation [(case interpretation :normal "N" :low "L" :high "H")] []))]))))

;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P6): a real
;; DiagnosticReport panel's own OBX shares `observation-obx-segment`'s
;; own field set verbatim ("sharing observation-obx-segment's simpler
;; field set" -- P6's own text) -- reused directly, not a near-duplicate
;; builder.

(defn- observation-message
  "ORU^R01 with a SINGLE OBX and no ORC/OBR -- a legal, real HL7v2 shape
  for an unsolicited observation not tied to any originating order
  (unlike :result-available's own order-linked ORU, docs/operational-
  models.md). ADR-0109's split clock: MSH-7 is this builder's ONLY
  timestamp field (OBX-14 is not rendered by `observation-obx-segment`
  -- this session's field audit), so it is unconditionally transmit
  time."
  [reference-date utc-offset facility providers personas site-profile offsets
   {:keys [t active-mrn location attending participants] :as ev}]
  (let [type+trigger (message-type-registry :observation)
        control-id (control-id-for ev)
        transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        provider (provider-by-id providers attending)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (pid-segment active-mrn (get personas (:patient-id (first participants))))
      (pv1-segment site-profile :inpatient facility-name location nil provider nil)
      (observation-obx-segment 1 ev)
      (z-segments-for site-profile personas ev)))))

;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P6): ORC+OBR
;; present (unlike :observation's own order-less shape) -- a real
;; DiagnosticReport panel IS an ORU^R01 with order context, D1a-7's own
;; account. ORC-1/ORC-2/OBR-4 reused unchanged (both already generic on
;; control-id/concept); ONE `observation-obx-segment` per embedded
;; child, `set-id` from vector position, the SAME `map-indexed` shape
;; `oru-message` already uses for :results.

(defn- diagnostic-report-message
  "ADR-0109's split clock: MSH-7 is this builder's ONLY timestamp field
  (OBR-7/OBX-14 not rendered -- this session's field audit, same as
  `orm-message`/`oru-message`/`observation-message`), so it is
  unconditionally transmit time."
  [reference-date utc-offset facility providers personas site-profile offsets
   {:keys [t active-mrn location attending codes observations participants] :as ev}]
  (let [type+trigger (message-type-registry :diagnostic-report)
        control-id (control-id-for ev)
        transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        provider (provider-by-id providers attending)
        obx-segments (map-indexed (fn [i o] (observation-obx-segment (inc i) o)) observations)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (pid-segment active-mrn (get personas (:patient-id (first participants))))
      (pv1-segment site-profile :inpatient facility-name location nil provider nil)
      (orc-segment control-id)
      (obr-segment 1 (first codes))
      (concat obx-segments (z-segments-for site-profile personas ev))))))

(defn event->messages
  "Renders one ground-truth event to a vector of 0+ ER7 message strings
  -- most types render exactly one message; M2b's genuinely two-
  participant types (:bed-swap A17, :merge A40) still render exactly
  ONE message (both patients' data in one message, the real HL7 shape),
  so this is 0-or-1 for every type today, but returns a vector (not a
  single nilable message) since a future many-messages-per-event type
  is now a shape this stage already accommodates. Events outside
  `message-type-registry` render an empty vector, not an error.

  ADR-0109: `offsets` ({control-id -> offset-seconds}, `plan-latency`'s
  own output) threads to every builder for the split-clock rendering
  (each builder's own docstring has the per-type field-audit detail);
  the lower arities pass {} -- unconditionally the identity input,
  since `transmit-seconds` (this namespace, private) falls back to a 0
  offset for any control-id absent from the map."
  ([reference-date utc-offset facility providers ev]
   (event->messages reference-date utc-offset facility providers {} nil {} ev))
  ([reference-date utc-offset facility providers personas ev]
   (event->messages reference-date utc-offset facility providers personas nil {} ev))
  ([reference-date utc-offset facility providers personas site-profile offsets {:keys [event] :as ev}]
   (cond
     (not (message-type-registry event)) []
     (= :bed-swap event) [(bed-swap-message reference-date utc-offset facility providers personas site-profile offsets ev)]
     (= :merge event) [(merge-message reference-date utc-offset facility providers personas site-profile offsets ev)]
     (= :order-placed event) [(orm-message reference-date utc-offset facility providers personas site-profile offsets ev)]
     (= :result-available event) [(oru-message reference-date utc-offset facility providers personas site-profile offsets ev)]
     (= :observation event) [(observation-message reference-date utc-offset facility providers personas site-profile offsets ev)]
     (= :diagnostic-report event) [(diagnostic-report-message reference-date utc-offset facility providers personas site-profile offsets ev)]
     :else [(single-subject-message reference-date utc-offset facility providers personas site-profile offsets ev)])))

(def ^:private default-providers
  "A fixed, arbitrary reference-seed provider pool -- purely a fallback
  default for callers that don't care about exact NPI values (`emit`'s
  lower arities). A real run threads back its OWN materialized
  providers (ehrt.sim-engine.engine/run's :providers) instead, so its
  messages' PV1-7 matches its own ground-truth log's :attending ids."
  (sim-model/materialize-providers (java.util.Random. 0) sim-model/default-provider-templates))

(defn emit
  "The stage function: ground-truth log -> vector of ER7 message
  strings, in log order. Pure function of its arguments alone
  (determinism law); events outside `message-type-registry` are
  skipped, not errored -- the theory's laws bind the events this stage
  claims to handle, not every event type that may ever appear in a
  log. `utc-offset`/`facility`/`providers` default for standalone
  convenience; callers rendering a specific run's log should pass back
  that SAME run's :utc-offset/:facility/:providers (ehrt.sim.run
  does). `site-profile` (Milestone site-profiles) is the LAST, optional
  argument: absent (the 5-arg arity), nil, or {} all render identically
  -- the default-profile identity property (docs/site-profiles.md, this
  milestone's own determinism anchor) -- since :site-profile reaches no
  stage but this one's own render call sites, never ground-truth-log or
  check.clj (ehrt.sim-engine.engine/config-keys has no such key).

  ADR-0109: this function's own output is BYTE-FROZEN -- always calls
  `event->messages` with offsets {}, so every transmit instant equals
  its own clinical instant and this function's bytes/order never move,
  regardless of anything ADR-0109 added elsewhere in this namespace.
  `emit-wire`, below, is the split-clock sibling that actually shifts
  MSH-7; this function is the oracle `emit-wire`'s own identity
  property is checked against."
  ([ground-truth reference-date]
   (emit ground-truth reference-date default-utc-offset sim-model/default-facility default-providers))
  ([ground-truth reference-date utc-offset]
   (emit ground-truth reference-date utc-offset sim-model/default-facility default-providers))
  ([ground-truth reference-date utc-offset facility providers]
   (emit ground-truth reference-date utc-offset facility providers nil))
  ([ground-truth reference-date utc-offset facility providers site-profile]
   (let [personas (personas-by-patient-id ground-truth)]
     (into [] (mapcat (partial event->messages reference-date utc-offset facility providers personas site-profile {}))
           ground-truth))))

;; --- ADR-0109: the second clock -- GT x LatencyParams -> TimedWire -------
;; The extension point docs/dev/simulator-architecture.md section 5 named
;; and built nothing of: an arrow between `engine`'s own GT output and
;; this namespace's own `emitH` consumption of it, so a message's own
;; wire-emission instant can lag its clinical-event instant by a
;; realistic, sampled delay. Sampling itself stays OUT of this namespace
;; (this file's own renders-only doctrine, restated in that same
;; section) -- `plan-latency` is a pure function of an explicitly-passed
;; RNG, never an atom or a wall clock, and its OWN output (`offsets`) is
;; the only thing `emit-wire` ever consumes; `emit-wire` itself takes no
;; RNG at all.

(defn plan-latency
  "RNG x GT x LatencyProfile (ehrt.sim-model.config/LatencyProfile) ->
  offsets ({control-id -> offset-seconds}). Fixed RNG consumption (the
  RNG-path law, .agents/rulings.md, AR-RL2-2's own underlying
  discipline; `ehrt.sim-engine.engine/assign-pathway`'s own worked
  example, engine.clj:1165-1183, is the precedent this function
  follows): ALWAYS consumes exactly one `.nextDouble` per ground-truth
  event, in log order, regardless of whether that event's own :event
  type is covered by `latency-profile` -- draw-and-discard for an
  uncovered type, so adding one profile entry for event type X can
  never shift any OTHER event's own draw, covered or not.

  A covered event (`latency-profile` has an entry for its :event type)
  samples its own offset uniformly from that entry's
  {:from-minutes :to-minutes} range via the ALREADY-consumed draw,
  converted to whole seconds (`sim/ADR-0011`'s own minutes-authored/
  seconds-engine convention, mirrored here). An uncovered event, or one
  with no `control-id-for` at all (outside `message-type-registry` --
  :bed-swap/:merge's own two-participant control-ids are covered the
  same as every single-subject one, since `control-id-for` already
  handles both), contributes no entry to the returned map.

  Absent/nil/{} `latency-profile` still draws (and discards) once per
  event and returns {} -- `emit-wire` called with THIS function's own
  {} output renders byte-identical to `emit` (the identity property,
  emit-hl7-test), the same three-way absent/nil/{} agreement
  `ehrt.sim-emit-hl7.site-profile`'s own default-profile identity
  already established for site profiles."
  [^java.util.Random rng ground-truth latency-profile]
  (into {}
        (keep (fn [ev]
                (let [draw (.nextDouble rng)
                      {:keys [from-minutes to-minutes]} (get latency-profile (:event ev))]
                  (when (and from-minutes to-minutes)
                    (when-let [control-id (control-id-for ev)]
                      [control-id (long (Math/round (* 60.0 (+ from-minutes (* draw (- to-minutes from-minutes))))))])))))
        ground-truth))

(defn emit-wire
  "GT x reference-date x utc-offset x facility x providers x
  site-profile x offsets -> TimedWire: the SAME messages `emit` would
  render, split-clock (each builder's own ADR-0109 docstring has the
  per-type detail: MSH-7 shifted by `offsets`, every clinical-time
  field -- EVN-2 where present -- unshifted), returned SORTED BY
  TRANSMIT TIME rather than log order -- out-of-order clinical arrival
  (a lagged admission whose transmit instant lands after a later
  event's own) falls out of this sort, not out of any special-cased
  reordering logic. Ties (equal transmit seconds) break on original log
  position, stable -- the identity property's own mechanism: absent/
  nil/{} `offsets` makes every transmit second equal its own log-order
  `:t`, and since ground truth is already `:t`-nondecreasing
  (`sim-engine`'s own priority-queue invariant), the stable tie-break
  reproduces `emit`'s exact order, and therefore its exact bytes.

  `offsets` is plain data (`plan-latency`'s own output, or hand-built)
  -- this function takes no RNG at all, per this namespace's own
  renders-only doctrine."
  [ground-truth reference-date utc-offset facility providers site-profile offsets]
  (let [personas (personas-by-patient-id ground-truth)
        offsets (or offsets {})]
    (->> ground-truth
         (map-indexed
          (fn [i ev]
            (let [control-id (control-id-for ev)
                  transmit-t (transmit-seconds offsets control-id (:t ev))]
              (map (fn [message] [transmit-t i message])
                   (event->messages reference-date utc-offset facility providers personas site-profile offsets ev)))))
         (apply concat)
         (sort-by (fn [[transmit-t i _]] [transmit-t i]))
         (mapv peek))))
