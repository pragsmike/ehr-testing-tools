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
   ;; M5b (components/patient-simulator/docs/gmf-interpreter.md section 4's sketch, item 5): the new
   ;; outpatient encounter class. :outpatient-visit-end has NO entry, by
   ;; design (item 7 -- the same sim/ADR-0012 :step-rejected precedent: many
   ;; real ambulatory feeds send a single A04 and no closing message for a
   ;; same-day visit; inventing a discharge-shaped message here would be
   ;; manufacturing wire traffic no real interface sends).
   :outpatient-visit {:type "ADT" :trigger "A04"}
   ;; ARC 3B SWEEP 2 (ADR-0174 ruling C, 2026-08-26): the AUTHOR'S OWN
   ;; ADDITION -- the ADR had recommended nothing new reach the wire in
   ;; arc 3b, and the author overrode that for the bed cycle so
   ;; `ehrt play --board` can see a dirty bed. It is the only entry in
   ;; this registry whose event names NO PATIENT, and therefore the only
   ;; one that does not go through `single-subject-message`:
   ;; `ADT_A20`'s segments are `[MSH EVN NPU]`, with no PID and no PV1
   ;; at all. `bed-status-message` is its sibling, added rather than
   ;; widening the single-subject builder with a patient-less branch.
   ;;
   ;; MSH-12 IS NOW "2.4" (arc 4 sweep 1, ADR-0175 ruling A1, commit 2 of
   ;; 2). This entry did not change it and does not depend on it: ADT^A20
   ;; exists in both versions, which is why sweep 2 could add it while
   ;; the version question was still open. It is settled now -- see the
   ;; block below.
   :bed-status-change {:type "ADT" :trigger "A20"}
   ;; SCHEDULING'S FOUR KINDS STILL GET NO ENTRY HERE, and the reason
   ;; CHANGED on 2026-08-27 (arc 4 sweep 1, ADR-0175 ruling A1). Read
   ;; this block before adding one.
   ;;
   ;; THE VERSION BLOCKER IS GONE. `:appointment`, `:reschedule`,
   ;; `:appointment-cancel` and `:no-show` map onto the SIU family --
   ;; S12, S14, S15, S26 -- which is v2.4 structure. Arc 3b sweep 3
   ;; recorded, correctly for its day, that "every message this emitter
   ;; produces carries MSH-12 \"2.3\", while the SIU structures are
   ;; v2.4", so an entry "would emit a structure the version field
   ;; disclaims". `site-profile/default-msh` now declares "2.4", and
   ;; that sentence no longer holds: SIU is exactly as declarable as
   ;; ADT^A20.
   ;;
   ;; WHAT THIS SWEEP CHANGED, AND WHAT IT DID NOT. It changed the
   ;; version field and PID-13's rendering, nothing else. It did NOT add
   ;; a message family -- ADR-0175 ruling B1 puts SIU after (A), not in
   ;; it, and its own fences forbid a new family in this sweep. So the
   ;; four stay GROUND TRUTH ONLY: a consumer reading the log sees
   ;; appointments, a consumer reading the wire does not, and the gap is
   ;; still REAL. What is different is that it is now a SCHEDULING-WORK
   ;; gap and not a version gap, and it is arc 4 sweep 4's to close.
   ;;
   ;; ANY LATER SWEEP ADDING AN SIU ENTRY OWES a `v2-replay/evolve-entry`
   ;; arm with it (ADR-0175 section 1(iii)): an unhandled MSH-9 THROWS
   ;; there, which kills the emitter-coherence property rather than
   ;; failing it softly.
   ;;
   ;; The silence is still stated in all three places the conformance
   ;; gate demands: each kind's own `:doc`, here, and
   ;; `event-conformance-test`'s silent set.
   ;; M5b (components/patient-simulator/docs/gmf-interpreter.md section 1's table): :observation is an
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
   :diagnostic-report {:type "ORU" :trigger "R01"}
   ;; ARC 3A PART 3 (ADR-0173, contract 1.3.0): `:demographic-update`
   ;; and `:coverage-change` deliberately get NO entry here, the same
   ;; truth-only treatment `:procedure`/`:medication-order`/the CarePlan
   ;; pair already get -- and, unlike those, with the change still
   ;; REACHING the wire: `demographics-timeline` folds both, so every
   ;; message the patient receives after one renders the new PID (or the
   ;; new IN1). That is what lifted ADR-0172 limitations row 6.
   ;;
   ;; The A08 (and the A31, and an IN1-only update) that would give them
   ;; messages of their OWN is real work this arc does not do and does
   ;; not sketch: a registry entry is also a control-id derivation, a
   ;; derivability-property row, and a `witnessed-message-types` claim.
   ;; ADR-0173's own Consequences name it as a candidate for a later
   ;; arc rather than leaving it as a silence a reader has to notice.
   })

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
  [{:keys [event t active-mrn surviving-mrn participants swap bed to]}]
  (when-let [{:keys [trigger]} (message-type-registry event)]
    (case event
      ;; ARC 3B SWEEP 2: a bed event has no `:active-mrn` to key on --
      ;; it has no patient. The BED plus the status it is moving TO is
      ;; what makes it unique, and the status is in the key rather than
      ;; only the bed because a ward tuned to a zero-minute leg would
      ;; otherwise put two legs of one bed's cycle at the same `t`.
      :bed-status-change
      (str bed "-" (name to) "-" trigger "-" t)

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

(defn- tn-field
  "TN (Telephone Number), PID-13. The persona's own `:phone` is
  `NNN-NNN-NNNN` (`ehrt.sim-model.persona`'s contract regex,
  `^\\d{3}-\\d{3}-\\d{4}$`); the WIRE carries `(NNN)NNN-NNNN`.

  ARC 4 SWEEP 1 (`notes/adr/0175-arc-4-emission-add-ons.md` ruling A1,
  commit 1 of 2). This is a conformance fact, not a formatting
  preference. HAPI's v2.4 TN primitive rule wants the parenthesised
  area code, and `PipeParser` enforces primitives DURING the parse
  rather than after -- so at MSH-12 \"2.4\" the persona's own shape does
  not produce a warning, it throws, and the message resolves to no
  structure at all. ADR-0175 section 2(e) measured it: 346 of the probe
  corpus's 747 messages died exactly here, and with this one field
  reformatted all 747 resolve into real v2.4 structures. Probed
  directly against the vendored jar: `(303)292-0567` OK,
  `(303)292-0567X1234` OK, `\"\"` OK; `492-292-0567`, `3032920567` and
  `(303) 292-0567` all FAIL.

  GROUND TRUTH DOES NOT MOVE. `persona`'s regex and its three phone
  draws are untouched, and `bin/ground-truth-bracket` proves that
  per commit rather than this docstring asserting it. Rendering is
  where a wire convention belongs -- the same seam PID-11's XAD and
  PID-7's date already use.

  A phone that does NOT match the contract shape renders VERBATIM
  rather than being mangled into a guess. Every persona this emitter
  has ever been handed matches, so this branch moves no existing
  message; it exists because a silent reformat of an unrecognised
  value would be a worse failure than passing it through."
  [phone]
  (parser/create-field
   [(str/replace phone #"^(\d{3})-(\d{3})-(\d{4})$" "($1)$2-$3")]))

(defn- pid-segment
  "PID-1/2/3 unconditionally (Set ID, blank, the active MRN); PID-4/6/9/10/12
  stay blank placeholders so positional fields (5/7/8/11/13) land correctly.
  M4: when `persona` is present (every real ehrt.sim-engine.engine/run output,
  post the :registered event -- ehrt.sim-model.persona/Persona), PID gains
  demographic enrichment: PID-5 (XPN name), PID-7 (DOB, HL7 date), PID-8 (sex,
  Table 0001 F/M), PID-11 (XAD address), PID-13 (TN phone, `tn-field`). nil persona (hand-
  built test worlds that never processed a :registered step) falls back to
  the pre-M4 3-field segment exactly -- no positional padding, no crash.

  ARC 3A PART 4: PID-7, PID-8 and PID-13 render EMPTY when the field is
  absent, the same way PID-11 already does. That is the PLACEHOLDER
  registration and nothing else -- a patient who arrived unidentified
  has an alias name and no known date of birth, sex or phone -- and it
  is a rendering rule, not a permission: every persona this emitter has
  ever been handed carries all three, so no existing message moves. The
  alternative was to render the person's REAL values under an alias
  name, which would tell a consumer's MPI something the modelled
  hospital does not know."
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
     (if (:dob persona)
       (parser/create-field [(str/replace (:dob persona) "-" "")])
       (parser/create-field []))
     (if (:sex persona)
       (parser/create-field [(case (:sex persona) :female "F" :male "M")])
       (parser/create-field []))
     (parser/create-field [])
     (parser/create-field [])
     ;; ADR-0173 ruling E1: an ABSENT `:address` renders an EMPTY
     ;; PID-11, not a five-empty-component XAD and not a sentinel. It
     ;; reaches here only through `demographics-timeline`'s own fold,
     ;; for a patient with nowhere to live; every persona this emitter
     ;; has ever been handed carries one, so no existing message moves.
     ;; A literal -- HOMELESS, UNDOMICILED, a shelter row -- is one
     ;; site's local convention and belongs in a site profile, which is
     ;; a seam this project already has.
     (if (:address persona)
       (xad-field (:address persona))
       (parser/create-field []))
     (parser/create-field [])
     (if (:phone persona)
       (tn-field (:phone persona))
       (parser/create-field [])))))

(defn- in1-segment
  "IN1 (insurance): IN1-1 set id, IN1-3/IN1-4 the sampled payer pool
  entry's id/name (docs/operational-models.md's payers model, Milestone
  M4 -- SimHospital issue #3's own request, docs/research/SimHospital-
  Synthea-limitations-considered.md §5.3). Rides ONLY the admission
  message (single-subject-message's own call site) -- the real HL7v2
  convention: insurance coverage is registered once, at admission, not
  restated on every subsequent ADT event.

  ARC 3A PART 4: the call site skips this segment ENTIRELY when the
  patient's demographic state carries no payer, which is the
  PLACEHOLDER registration and nothing else -- an unidentified arrival
  has no known coverage, and an IN1 with two empty fields would be a
  claim that they have none rather than that nobody has asked yet.
  Every persona this emitter has ever been handed carries a payer, so
  no existing message moves."
  [{payer-id :id payer-name :name}]
  (parser/create-segment
   "IN1"
   (parser/create-field ["1"])
   (parser/create-field [])
   (parser/create-field [payer-id])
   (parser/create-field [(escape-er7 payer-name)])))

(defn- demographics-timeline
  "The demographic state this emitter renders from, derived directly
  from the log's own events (sim/ADR-0012's own precedent: a stage's own
  state is recoverable by scanning the log, no second input needed).
  Computed once per `emit` call and threaded down to every segment
  builder that needs it, so PID enrichment applies uniformly across
  every message type, not just admission. Read ONLY through
  `demographics-at`.

  `{patient-id [[t state] ...]}`, t-ascending, one entry per event that
  MOVED that patient's demographics. `:registered` seeds it; ADR-0173
  section 2(b)'s two kinds fold onto it.

  THE VALUE IS PERSONA-SHAPED, deliberately, and this is the one design
  choice here worth stating. `ehrt.sim-engine.engine/Demographics` is the
  ENGINE's state-at-t shape, and it carries a residence SUM where a
  Persona carries an `:address`. This namespace may not depend on
  sim-engine at all (`components/sim-emit-hl7` depends on
  `components/sim-model` and nothing else, AGENTS.md's own dependency
  constraint), and -- more to the point -- a site profile's Z-segment
  templates bind `[:persona ...]` paths against this exact value
  (`context-for-event`), so changing its SHAPE would silently break
  every authored site profile in the field. So the fold writes back into
  a Persona: `:address` is ABSENT, not nil-valued and not sentinel-
  valued, for a patient who has nowhere to live. `pid-segment` renders
  an absent address as an empty PID-11, which is ruling E1 on the wire.

  ARC 3A PART 3 IS WHERE THE FOLD ARRIVED. Before it, this function
  returned `{patient-id persona}` and every `t` answered with the t0
  sample -- the shape ADR-0172 limitations row 6 was written about. That
  row is STRUCK by this change, not repaired, and its gate is deleted:
  a delta folded onto patient state is no longer invisible to a message.

  ARC 3A PART 4 ADDS THE PLACEHOLDER AND ITS FILL. A `:registered`
  carrying `:identity :placeholder` seeds the window's ALIAS NAME and
  nothing else -- no DOB, no sex, no phone, no address -- even though
  the event's own `:persona` says who the patient really is. That gap
  between what ground truth knows and what the wire may claim is the
  whole of the identification flow's point (ADR-0173 section 2(d)), and
  this is the one function that enforces it. The `:identity-fill` then
  RE-SEEDS from the persona the fill carries, so every message after it
  renders the identified patient and every message before it renders
  the John Doe."
  [ground-truth]
  (letfn [(hide-address [state residence]
            (cond-> state
              (and residence (not= :housed (:status residence))) (dissoc :address)))
          (seed [ev]
            (if (= :placeholder (:identity ev))
              ;; PERSONA-SHAPED, with one field in it. `pid-segment`
              ;; renders every absent field empty, so this is a PID
              ;; carrying an MRN and a John Doe name and nothing else.
              {:name (:alias-name ev)}
              (when-let [persona (:persona ev)]
                (hide-address persona (:residence ev)))))
          (fold [state ev]
            (case (:event ev)
              :demographic-update
              (if (= :identity-fill (:cause ev))
                (hide-address (:persona ev) (:residence ev))
                (case (:field ev)
                  :residence (let [address (:address (:value ev))]
                               (if address (assoc state :address address) (dissoc state :address)))
                  :name (assoc state :name (:value ev))
                  :dob (assoc state :dob (:value ev))
                  state))
              :coverage-change (assoc state :payer (:payer ev))
              state))]
    (reduce (fn [acc ev]
              (let [patient-id (:patient-id (first (:participants ev)))]
                (case (:event ev)
                  :registered (assoc acc patient-id [[(:t ev) (seed ev)]])
                  (:demographic-update :coverage-change)
                  (if-let [timeline (get acc patient-id)]
                    (assoc acc patient-id
                           (conj timeline [(:t ev) (fold (second (peek timeline)) ev)]))
                    acc)
                  acc)))
            {}
            ground-truth)))

(defn- demographics-at
  "One patient's demographic state AS IT STOOD AT `t` -- the single
  lookup shape every PID-rendering site in this namespace goes through.

  The LAST entry at or before `t`, which is what makes a message render
  the demographics the patient had when the event happened rather than
  the ones they ended the run with. A patient with no `:registered` in
  this log at all -- a hand-built fixture, a sliced log -- answers nil,
  and `pid-segment` falls back to its pre-M4 three-field segment."
  [demographics patient-id t]
  (when-let [timeline (get demographics patient-id)]
    (loop [entries timeline state nil]
      (if-let [[et estate] (first entries)]
        (if (<= et t) (recur (rest entries) estate) state)
        state))))

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
  milestone (no disposition concept existed to render at all).

  ARC 3B SWEEP 1 (ADR-0174 ruling C1): PV1-19, the VISIT NUMBER, is the
  encounter's one wire face and the only field this sweep adds to any
  message. It was one of the 28 blanks below, on every message this
  project had ever produced -- `emit_hl7.clj`'s own registry comment
  calls traffic invisible to every consumer a failure mode, and an
  encounter with no visit number was exactly that. `visit-number` is
  nil for every event of every run that did not opt into `:encounters`,
  and nil renders the SAME empty field that stood here before, so the
  blank count moves 28 -> 27 while the byte count does not."
  [site-profile patient-class facility-name location from provider disposition-state visit-number]
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
         ;; PV1-8 .. PV1-18, then PV1-19 (visit number), then
         ;; PV1-20 .. PV1-35: 11 + 1 + 16 = the 28 fields that stood
         ;; between PV1-7 and PV1-36 before this sweep.
         (concat (blank-fields 11)
                 [(if visit-number
                    (parser/create-field [visit-number])
                    (parser/create-field []))]
                 (blank-fields 16)
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
  off the SAME `demographics` map `emit` computes once per call, the primary
  (first) participant's persona for a genuinely multi-participant event
  (bed-swap, merge), the same simplification `ehrt.sim-engine.engine/
  replay`'s own :patient-id convenience view already makes."
  [demographics event]
  (assoc event :persona (demographics-at demographics
                                         (:patient-id (first (:participants event)))
                                         (:t event))))

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
  profile output is untouched by this function's existence.

  ADR-0150 (2026-08-18): every call site hands this the WHOLE event.
  `single-subject-message` used to synthesize a seven-key subset
  ({:event :t :active-mrn :location :from :attending :participants})
  while the six other families passed `ev`, so an ADT-family template
  bound to `:reason`, `:home-ward`, `:disposition`, `:warm-up` or any
  other key rendered empty -- and silently, `render-z-field` treating
  an unbound path and an unreachable one identically. The context a
  template resolves against is now the same map on every family, which
  is the only shape `docs/site-profiles.md`'s own path examples can be
  read literally against."
  [site-profile demographics event]
  (let [context (context-for-event demographics event)]
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
  here gets PID enrichment uniformly via `demographics`. Milestone
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
  [reference-date utc-offset facility providers demographics site-profile offsets
   {:keys [event t active-mrn location from attending participants] :as ev}]
  (when-let [type+trigger (message-type-registry event)]
    (let [control-id (control-id-for ev)
          clinical-ts (hl7-timestamp reference-date t utc-offset)
          transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
          facility-name (name (:id facility))
          provider (provider-by-id providers attending)
          persona (demographics-at demographics (:patient-id (first participants)) t)
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
        (pv1-segment site-profile patient-class facility-name location from provider disposition-state
                     (:encounter-id ev))
        (concat (when (and (= :admission event) (:payer persona))
                  [(in1-segment (:payer persona))])
                (z-segments-for site-profile demographics ev)))))))

(defn- bed-swap-message
  "A17 (swap patients): ONE message per ground-truth event, carrying
  BOTH patients' PID/PV1 pairs -- the real HL7v2 A17 shape, and why the
  emitter-derivability law now keys on the event's own log position
  rather than a single :active-mrn (a bed-swap message has two).
  ADR-0109's split clock (see `single-subject-message`'s own docstring):
  ONE `control-id` covers the whole event (`control-id-for`'s own
  :bed-swap arm), so both patients' PID/PV1 pairs ride the SAME
  transmit-shifted MSH-7 and the SAME unshifted EVN-2."
  [reference-date utc-offset facility providers demographics site-profile offsets
   {:keys [t participants swap] :as ev}]
  (let [type+trigger (message-type-registry :bed-swap)
        control-id (control-id-for ev)
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        [p1 p2] (mapv :patient-id participants)
        ;; ARC 3B SWEEP 1: PV1-19 comes from the same per-side entry
        ;; PV1-3 does -- a two-patient event names two encounters and
        ;; carries neither at top level (`BedSwapSide`'s own docstring).
        {mrn1 :active-mrn from1 :from to1 :to att1 :attending enc1 :encounter-id} (get swap p1)
        {mrn2 :active-mrn from2 :from to2 :to att2 :attending enc2 :encounter-id} (get swap p2)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (evn-segment (:trigger type+trigger) clinical-ts)
      (pid-segment mrn1 (demographics-at demographics p1 t))
      (pv1-segment site-profile :inpatient facility-name to1 from1 (provider-by-id providers att1) nil enc1)
      (pid-segment mrn2 (demographics-at demographics p2 t))
      (pv1-segment site-profile :inpatient facility-name to2 from2 (provider-by-id providers att2) nil enc2)
      (z-segments-for site-profile demographics ev)))))

(defn- npu-segment
  "NPU (bed status update): NPU-1 the bed's PL -- the SAME datatype and
  the SAME `location-field` rendering PV1-3 uses -- and NPU-2 the bed
  status from HL7v2 Table 0116. Exactly two fields, which is the whole
  of the segment."
  [site-profile facility-name location status]
  (parser/create-segment
   "NPU"
   (location-field facility-name location)
   (parser/create-field (site-profile/code-for site-profile :bed-status
                                               site-profile/standard-bed-status-codes status))))

(defn- bed-status-message
  "A20 (bed status update): `[MSH EVN NPU]`, and NOTHING ELSE -- no PID,
  no PV1. This is the first message this project emits that names no
  patient, which is why it is a sibling of `single-subject-message`
  rather than a branch inside it: that builder's contract is a PID/PV1
  pair per subject, and an A20 has no subject to pair.

  THE STRUCTURE IS VERIFIED FROM THIS TREE'S OWN RESOLVED DEPENDENCIES,
  not from memory. `components/judge-v2-hapi/deps.edn` pulls
  `ca.uhn.hapi/hapi-structures-v24` 2.6.0; that jar carries
  `ca/uhn/hl7v2/model/v24/message/ADT_A20.class` and
  `ca/uhn/hl7v2/model/v24/segment/NPU.class`, and A20 does NOT appear in
  `ca/uhn/hl7v2/parser/eventmap/2.4.properties` -- i.e. it is not
  aliased onto another structure, it has its own. NPU's two fields are
  NPU-1 `PL` (the datatype PV1-3 already renders) and NPU-2 `IS`.

  AND THE VERSION QUESTION, NOW SETTLED -- this paragraph is kept and
  amended rather than deleted, because what it could NOT check is the
  reason the question was worth asking.

  It used to read: MSH-12 stays `\"2.3\"`, and A20-in-2.3 is the
  AUTHOR'S RULING (ADR-0174 ruling C) rather than something this clone
  can verify, because there is NO 2.3 trigger table here -- the only
  structure library on any classpath is v2.4, `~/.m2` holds
  `hapi-structures-v24` alone, and neither `hapi-base` nor any resource
  in this repository carries a 2.3 eventmap. All of that is still true
  of 2.3.

  WHAT CHANGED (arc 4 sweep 1, ADR-0175 ruling A1, 2026-08-27):
  `site-profile/default-msh` declares `\"2.4\"`. The unverifiable claim
  is retired rather than answered -- this message family no longer
  needs A20-in-2.3 to be legal, because it no longer says 2.3. What was
  checkable all along (A20 in 2.4, own structure, not aliased in
  `2.4.properties`) is now what the version field actually declares,
  and `ehrt.conformance.v2-structure-resolution-test` asserts the
  resolution over a whole corpus rather than this docstring asserting
  it. A site that must speak 2.3 keeps `{:msh {:version \"2.3\"}}` and
  inherits the same unverifiability, knowingly."
  [reference-date utc-offset facility _providers _demographics site-profile offsets
   {:keys [t bed ward to] :as ev}]
  (let [type+trigger (message-type-registry :bed-status-change)
        control-id (control-id-for ev)
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))]
    (parser/str-message
     (parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (evn-segment (:trigger type+trigger) clinical-ts)
      (npu-segment site-profile facility-name {:ward ward :bed bed} to)))))

(defn- merge-message
  "A40 (merge patient): PID carries the SURVIVING mrn, MRG-1 carries the
  prior (merged-away) one (docs/patient-state-model.md's identity
  payoff) -- ONE message per merge event. ADR-0109's split clock (see
  `single-subject-message`'s own docstring): `control-id-for`'s own
  :merge arm keys on the surviving mrn."
  [reference-date utc-offset facility _providers demographics site-profile offsets
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
      (pid-segment surviving-mrn (demographics-at demographics survivor-id t))
      (pv1-segment site-profile :inpatient facility-name nil nil nil nil (:encounter-id ev))
      (mrg-segment merged-mrn)
      (z-segments-for site-profile demographics ev)))))

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
  itself, not its analytes; those are per-OBX below).

  ADR-0142 (2026-08-16), clinical time on the RESULT wire: the 3-arity
  additionally renders OBR-7 (Observation Date/Time) = `clinical-ts`,
  the event's OWN `:t`, rendered by `hl7-timestamp` exactly as EVN-2's
  clinical-ts is (author ruling Q1 \"a\") -- with OBR-5/OBR-6 as empty
  positional fields. It NEVER shifts under `emit-wire`: MSH-7 alone
  carries transmit time (`single-subject-message`'s own split-clock
  docstring, extended to results).

  The 2-arity renders OBR-1/4 only, exactly as before. It exists so
  `orm-message` stays BYTE-FROZEN: `obr-segment` also renders on
  ORM^O01, whose clinical-time story (OBR-7 = specimen/observation time
  versus ORC-9 = transaction time on an order that has not been
  observed yet) is a different question from a result's, and one
  ADR-0142's own fence holds shut. Author ruling Q3, 2026-08-16, on a
  scope collision reported rather than resolved in silence: \"Results
  only; ORM byte-frozen.\""
  ([set-id concept]
   (parser/create-segment
    "OBR"
    (parser/create-field [(str set-id)])
    (parser/create-field [])
    (parser/create-field [])
    (cwe-field concept)))
  ([set-id concept clinical-ts]
   (parser/create-segment
    "OBR"
    (parser/create-field [(str set-id)])
    (parser/create-field [])
    (parser/create-field [])
    (cwe-field concept)
    (parser/create-field [])
    (parser/create-field [])
    (parser/create-field [clinical-ts]))))

(defn- obx-segment
  "One analyte per OBX (docs/operational-models.md's own spec for this
  milestone): OBX-2 \"NM\" (numeric) -- every analyte in this
  catalytic's starter set is a numeric lab value, no other value types
  needed yet. OBX-7 renders the reference range as \"low-high\"; OBX-8
  the abnormal flag, HL7v2's own N/L/H vocabulary (Table 0078) -- a
  direct rendering of ehrt.sim-engine.order-profiles/abnormal-flag's
  own :normal/:low/:high, computed truth carried straight from the log,
  never re-derived at emit time (the log already has the answer).

  ADR-0142 (2026-08-16), clinical time on the RESULT wire: OBX-14
  (Date/Time of the Observation) = `clinical-ts`, the event's OWN `:t`,
  behind a positional pad at OBX-9..13 (author ruling Q2 \"a\"). Like
  OBR-7 it NEVER shifts under `emit-wire` -- MSH-7 alone carries
  transmit time. Every analyte OBX of one result event carries the SAME
  clinical instant, since the log records one `:t` for the result, not
  one per analyte."
  [set-id clinical-ts {:keys [concept unit value reference-range abnormal-flag]}]
  (parser/create-segment
   "OBX"
   (parser/create-field [(str set-id)])
   (parser/create-field ["NM"])
   (cwe-field concept)
   (parser/create-field [])
   (parser/create-field [(str value)])
   (parser/create-field [unit])
   (parser/create-field [(str (:low reference-range) "-" (:high reference-range))])
   (parser/create-field [(case abnormal-flag :normal "N" :low "L" :high "H")])
   (parser/create-field [])
   (parser/create-field [])
   (parser/create-field [])
   (parser/create-field [])
   (parser/create-field [])
   (parser/create-field [clinical-ts])))

(defn- orm-message
  "ORM^O01: order placed. No EVN segment -- EVN is an ADT-specific
  segment (HL7v2 convention), not part of the order-message family.
  ADR-0109's split clock: MSH-7 is this builder's ONLY timestamp field
  (ADR-0109's field audit: OBR-7 and ORC-9, HL7v2's own clinical-time
  candidates for this message family, are not rendered by
  `orc-segment`/`obr-segment` at all), so it is unconditionally
  transmit time -- there is no clinical-time field here to keep
  unshifted.

  STILL TRUE after ADR-0142 (2026-08-16), and deliberately so. That
  session gave `obr-segment` a 3-arity rendering OBR-7 and put it on
  all three ORU shapes; this builder keeps calling the 2-arity, so
  ORM^O01 stays BYTE-FROZEN. An order's clinical-time story is a
  different question from a result's -- OBR-7 on an order would mean
  specimen/observation time for an observation that has not happened
  yet, and ORC-9 (transaction time) is the field that would actually
  be owed -- and it is a named revisit, not a silent ride-along.
  Author ruling Q3, 2026-08-16: \"Results only; ORM byte-frozen.\""
  [reference-date utc-offset facility providers demographics site-profile offsets
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
      (pid-segment active-mrn (demographics-at demographics (:patient-id (first participants)) t))
      (pv1-segment site-profile :inpatient facility-name location nil provider nil (:encounter-id ev))
      (orc-segment control-id)
      (obr-segment 1 concept)
      (z-segments-for site-profile demographics ev)))))

(defn- oru-message
  "ORU^R01: result available -- OBR (order context) plus one OBX per
  analyte, in the same order the profile's own :results carries them
  (derived straight from the log, ehrt.sim-engine.order-profiles'
  sampling order -- no re-sorting here).

  ADR-0109's split clock, CORRECTED IN PLACE 2026-08-16 (ADR-0142):
  ADR-0109's field audit recorded OBR-7 and OBX-14 as not rendered by
  `obr-segment`/`obx-segment` at all, which made MSH-7 this builder's
  only timestamp field and therefore unconditionally transmit time.
  ADR-0142 renders both: MSH-7 is TRANSMIT time (shifted by `offsets`),
  OBR-7 and every OBX-14 are CLINICAL time (`clinical-ts`, this event's
  own `:t`, never shifted) -- the same two-clock split
  `single-subject-message` has always had via EVN-2, now on the result
  wire, so a downstream receiver handed a late result can back-date it."
  [reference-date utc-offset facility providers demographics site-profile offsets
   {:keys [t active-mrn location attending concept results participants] :as ev}]
  (let [type+trigger (message-type-registry :result-available)
        control-id (control-id-for ev)
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        provider (provider-by-id providers attending)
        obx-segments (map-indexed (fn [i r] (obx-segment (inc i) clinical-ts r)) results)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (pid-segment active-mrn (demographics-at demographics (:patient-id (first participants)) t))
      (pv1-segment site-profile :inpatient facility-name location nil provider nil (:encounter-id ev))
      (orc-segment control-id)
      (obr-segment 1 concept clinical-ts)
      (concat obx-segments (z-segments-for site-profile demographics ev))))))

;; --- M5b: :observation -> ORU^R01, OBX only (components/patient-simulator/docs/gmf-interpreter.md
;; section 1's table) -------------------------------------------------------

(defn- observation-obx-segment
  "OBX-3 is the FIRST of :codes (components/patient-simulator/docs/gmf-interpreter.md section 1: a GMF
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
  supplies) -- **AMENDED 2026-08-16 (ADR-0142), for OBX-14 ONLY.**

  ADR-0142, clinical time on the RESULT wire: OBX-14 (Date/Time of the
  Observation) = `clinical-ts`, the event's OWN `:t`, is now rendered
  unconditionally on every OBX this builder produces (author ruling Q2
  \"a\"). HL7v2 field positions being ordinal, that requires a
  POSITIONAL PAD -- OBX-9..13 always, and OBX-7/8 as well when the
  observation carries neither a reference-range nor an interpretation.
  The \"never a positional pad\" sentence above is superseded for this
  one field and stands for every other: OBX-7/8 are still content-
  conditional, and no field is padded to except on the way to OBX-14.
  The distinction that makes this consistent rather than merely
  excepted: OBX-7/8 pad for a value the observation MIGHT NOT HAVE,
  whereas `clinical-ts` is derived from `:t`, which every event in the
  log carries by construction -- there is no case where the pad leads
  to nothing.

  Like OBR-7, OBX-14 never shifts under `emit-wire`: MSH-7 alone
  carries transmit time."
  [set-id clinical-ts {:keys [codes value unit value-code reference-range interpretation]}]
  (let [range-fields (when (or reference-range interpretation)
                       [(parser/create-field (if reference-range [(str (:low reference-range) "-" (:high reference-range))] []))
                        (parser/create-field (if interpretation [(case interpretation :normal "N" :low "L" :high "H")] []))])
        ;; OBX-1..6 always, plus OBX-7/8 when present: pad from there up
        ;; to OBX-13, so `clinical-ts` lands at OBX-14 exactly.
        rendered-so-far (+ 6 (count range-fields))]
    (apply parser/create-segment
     "OBX"
     (concat
      [(parser/create-field [(str set-id)])
       (parser/create-field [(if value-code "CWE" "NM")])
       (cwe-field (first codes))
       (parser/create-field [])
       (if value-code (coded-value-field value-code) (parser/create-field (if (some? value) [(str value)] [])))
       (parser/create-field (if unit [unit] []))]
      range-fields
      (repeatedly (- 13 rendered-so-far) #(parser/create-field []))
      [(parser/create-field [clinical-ts])]))))

;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P6): a real
;; DiagnosticReport panel's own OBX shares `observation-obx-segment`'s
;; own field set verbatim ("sharing observation-obx-segment's simpler
;; field set" -- P6's own text) -- reused directly, not a near-duplicate
;; builder.

(defn- observation-message
  "ORU^R01 with a SINGLE OBX and no ORC/OBR -- a legal, real HL7v2 shape
  for an unsolicited observation not tied to any originating order
  (unlike :result-available's own order-linked ORU, docs/operational-
  models.md).

  ADR-0109's split clock, CORRECTED IN PLACE 2026-08-16 (ADR-0142):
  ADR-0109 recorded OBX-14 as not rendered by
  `observation-obx-segment`, making MSH-7 this builder's only timestamp
  field. ADR-0142 renders it: MSH-7 is TRANSMIT time, OBX-14 is
  CLINICAL time (this event's own `:t`, never shifted). There is no
  OBR here at all -- this shape carries no ORC/OBR -- so OBR-7 is not
  owed and not rendered."
  [reference-date utc-offset facility providers demographics site-profile offsets
   {:keys [t active-mrn location attending participants] :as ev}]
  (let [type+trigger (message-type-registry :observation)
        control-id (control-id-for ev)
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        provider (provider-by-id providers attending)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (pid-segment active-mrn (demographics-at demographics (:patient-id (first participants)) t))
      (pv1-segment site-profile :inpatient facility-name location nil provider nil (:encounter-id ev))
      (observation-obx-segment 1 clinical-ts ev)
      (z-segments-for site-profile demographics ev)))))

;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P6): ORC+OBR
;; present (unlike :observation's own order-less shape) -- a real
;; DiagnosticReport panel IS an ORU^R01 with order context, D1a-7's own
;; account. ORC-1/ORC-2/OBR-4 reused unchanged (both already generic on
;; control-id/concept); ONE `observation-obx-segment` per embedded
;; child, `set-id` from vector position, the SAME `map-indexed` shape
;; `oru-message` already uses for :results.

(defn- diagnostic-report-message
  "ADR-0109's split clock, CORRECTED IN PLACE 2026-08-16 (ADR-0142):
  ADR-0109 recorded OBR-7/OBX-14 as not rendered, making MSH-7 this
  builder's only timestamp field, the same as
  `orm-message`/`oru-message`/`observation-message`. ADR-0142 renders
  both on this ORU shape: MSH-7 is TRANSMIT time, OBR-7 and every
  embedded child's own OBX-14 are CLINICAL time (this event's own `:t`,
  never shifted). `orm-message` is the one of that list that does NOT
  change -- author ruling Q3, \"Results only; ORM byte-frozen\"."
  [reference-date utc-offset facility providers demographics site-profile offsets
   {:keys [t active-mrn location attending codes observations participants] :as ev}]
  (let [type+trigger (message-type-registry :diagnostic-report)
        control-id (control-id-for ev)
        clinical-ts (hl7-timestamp reference-date t utc-offset)
        transmit-ts (hl7-timestamp reference-date (transmit-seconds offsets control-id t) utc-offset)
        facility-name (name (:id facility))
        provider (provider-by-id providers attending)
        obx-segments (map-indexed (fn [i o] (observation-obx-segment (inc i) clinical-ts o)) observations)]
    (parser/str-message
     (apply parser/create-message
      parser/DEFAULT-DELIMITERS
      (msh-segment site-profile type+trigger control-id transmit-ts)
      (pid-segment active-mrn (demographics-at demographics (:patient-id (first participants)) t))
      (pv1-segment site-profile :inpatient facility-name location nil provider nil (:encounter-id ev))
      (orc-segment control-id)
      (obr-segment 1 (first codes) clinical-ts)
      (concat obx-segments (z-segments-for site-profile demographics ev))))))

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
  ([reference-date utc-offset facility providers demographics ev]
   (event->messages reference-date utc-offset facility providers demographics nil {} ev))
  ([reference-date utc-offset facility providers demographics site-profile offsets {:keys [event] :as ev}]
   (cond
     (not (message-type-registry event)) []
     (= :bed-status-change event) [(bed-status-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
     (= :bed-swap event) [(bed-swap-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
     (= :merge event) [(merge-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
     (= :order-placed event) [(orm-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
     (= :result-available event) [(oru-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
     (= :observation event) [(observation-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
     (= :diagnostic-report event) [(diagnostic-report-message reference-date utc-offset facility providers demographics site-profile offsets ev)]
     :else [(single-subject-message reference-date utc-offset facility providers demographics site-profile offsets ev)])))

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
   (let [demographics (demographics-timeline ground-truth)]
     (into [] (mapcat (partial event->messages reference-date utc-offset facility providers demographics site-profile {}))
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
  RNG-path law, `rulings.md#R-measure-claimed-population`'s own
  underlying discipline; `ehrt.sim-engine.engine/assign-pathway`'s own
  worked example -- cited BY NAME, never by line: that line moved twice
  already, once under arc 0's refactor and once under ADR-0171's, which
  is exactly the species ADR-0170 named -- is the precedent this
  function follows): ALWAYS consumes exactly one `.nextDouble` per ground-truth
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
  (let [demographics (demographics-timeline ground-truth)
        offsets (or offsets {})]
    (->> ground-truth
         (map-indexed
          (fn [i ev]
            (let [control-id (control-id-for ev)
                  transmit-t (transmit-seconds offsets control-id (:t ev))]
              (map (fn [message] [transmit-t i message])
                   (event->messages reference-date utc-offset facility providers demographics site-profile offsets ev)))))
         (apply concat)
         (sort-by (fn [[transmit-t i _]] [transmit-t i]))
         (mapv peek))))
