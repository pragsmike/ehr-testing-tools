(ns ehrt.sim-emit-hl7.segments
  "The emitter's segment layer: the thirteen HL7v2 segment builders --
  MSH, EVN, PID, IN1, MRG, PV1, NPU, SCH, ORC, OBR, OBX, the
  observation OBX and FT1 -- plus `control-id-for`, which mints MSH-10
  for every one of them, and `charge-concept`, which reads a chargeable
  fact off a log event for FT1.

  Extracted VERBATIM from `emit_hl7.clj`, the FIFTH cluster of that
  file's namespace extraction (`.agents/plans/engine-extraction-
  census.md` sections 2 and 2a, `roadmap.md#engine-namespace-extraction-
  and-apply-unification`). It is the first cluster of this file to
  depend on THREE landed siblings at once -- `er7` (eighteen edges),
  `registry` (two) and `hl7-time` (one) -- and, because of that, the
  first whose own MOVED TEXT had to be requalified: five bare names that
  resolved here through `emit_hl7.clj`'s delegating defs now name their
  real homes, `registry/message-type-registry`, `er7/escape-er7` twice,
  `er7/tn-field` and `hl7-time/hl7-timestamp`. `site-profile` and the
  HL7 parser come with it too, and `clojure.string` for one
  `str/replace`; no `:import` is owed.

  It has NO internal edge: not one of the fifteen forms calls another,
  which is why every private mover here is a widening and none stays
  private -- the exact opposite of `er7`, whose nine internal edges left
  six forms unwidened.

  `emit_hl7.clj` remains the namespace every existing requirer resolves
  against (author ruling C1(a)) and keeps THREE delegating defs. One is
  public: `control-id-for`, the cluster's only public mover, which
  `interface.clj` re-exports, which six test files call as
  `emit-hl7/control-id-for`, and which `corpus_io/er7_fields.clj`'s own
  docstring names by namespace. The
  other two are `^:private`, under the C7 extension the `tn-field`
  precedent established: `emit_hl7_test.clj` reaches `msh-segment` and
  `pid-segment` as `(#'emit-hl7/msh-segment ...)` and
  `(#'emit-hl7/pid-segment ...)`, var accesses on private vars that no
  move can carry and that C1(a) forbids editing, so both vars stay in
  `emit_hl7.clj` without widening its public surface by a name. Their
  twenty-two call sites there resolved through those defs unqualified
  until cluster 6 took every one of them; like `tn-field`'s, both defs
  now stand for that var access and a namespace claim alone.

  The other TWELVE private movers gain no def, because widening
  `emit_hl7.clj`'s own public surface is not what C1(a) asks for; they
  are public here and `defn-` no longer. Thirty-five call sites there
  named them `segments/...`; cluster 6 took thirty-four and cluster 7
  took the last, `plan-charges`' `segments/charge-concept`. None remains.

  Two sentences of the moved prose stopped being true at the seam and
  are corrected in the move commit rather than a commit later.
  `control-id-for` said its message-builder call sites were BELOW it and
  `sch-segment` said `siu-message` was; all thirteen builders and
  `siu-message` with them stayed behind, so both words are dropped.
  Nothing else differs, across 518 form-lines."
  (:require [com.nervestaple.hl7-parser.parser :as parser]
            [clojure.string :as str]
            [ehrt.sim-emit-hl7.hl7-time :as hl7-time]
            [ehrt.sim-emit-hl7.registry :as registry]
            [ehrt.sim-emit-hl7.er7 :as er7]
            [ehrt.sim-emit-hl7.site-profile :as site-profile]))

(defn control-id-for
  "MSH-10 (message control id) for one ground-truth event -- the SAME
  construction every message-builder call site uses, extracted
  once (post-M6, sim/ADR-0014's own `sim identifiers` verb reuses this
  exact function so its own inventory can never drift from what a real
  emission actually renders): `active-mrn` and the event's own trigger
  and `:t` for every single-subject type; `mrn1+mrn2` for :bed-swap
  (genuinely two participants, no single :active-mrn to key on);
  `surviving-mrn` for :merge (PID carries the survivor, not the one
  being merged away). nil for any event type outside
  `message-type-registry` (the same 'no message, no id' rule
  `event->messages` already follows).

  ARC 4 SWEEP 4: the SIU family keys on FOUR parts, `mrn-appointment-
  trigger-t`, from its first message. It is the only family here that
  did not have to LEARN that -- sweep 3 measured `:result-available`'s
  own three-part key NON-INJECTIVE and rowed the fix
  (`roadmap.md#oru-control-id-collision`), and a patient can hold more
  than one open appointment, so `mrn-S12-t` would collide the moment two
  bookings landed on one second. The appointment id is the discriminator
  the log already carries."
  [{:keys [event t active-mrn surviving-mrn participants swap bed to appointment-id]}]
  (when-let [{:keys [trigger]} (registry/message-type-registry event)]
    (case event
      (:appointment :reschedule :appointment-cancel :no-show)
      (str active-mrn "-" appointment-id "-" trigger "-" t)

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

(defn msh-segment
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

(defn evn-segment
  [trigger ts]
  (parser/create-segment
   "EVN"
   (parser/create-field [trigger])
   (parser/create-field [ts])))

(defn pid-segment
  "PID-1/2/3 unconditionally (Set ID, blank, the active MRN); PID-4/6/9/10/12
  stay blank placeholders so positional fields (5/7/8/11/13) land correctly.
  M4: when `persona` is present (every real ehrt.sim-engine.run/run output,
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
     (er7/xpn-field (:name persona))
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
       (er7/xad-field (:address persona))
       (parser/create-field []))
     (parser/create-field [])
     (if (:phone persona)
       (er7/tn-field (:phone persona))
       (parser/create-field [])))))

(defn in1-segment
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
   (parser/create-field [(er7/escape-er7 payer-name)])))

(defn mrg-segment
  "MRG-1: the prior (merged-away) patient identifier -- A40's own carrier
  for 'what mrn did this patient answer to before' (docs/patient-state-
  model.md's identity payoff). PID (built via pid-segment, same as every
  other type) carries the SURVIVING mrn."
  [merged-mrn]
  (parser/create-segment "MRG" (parser/create-field [merged-mrn])))


(defn pv1-segment
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
  project had ever produced -- `registry.clj`'s own comment
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
         (er7/location-field facility-name location)
         (parser/create-field [])
         (parser/create-field [])
         (er7/location-field facility-name from)
         (er7/provider-field provider)
         ;; PV1-8 .. PV1-18, then PV1-19 (visit number), then
         ;; PV1-20 .. PV1-35: 11 + 1 + 16 = the 28 fields that stood
         ;; between PV1-7 and PV1-36 before this sweep.
         (concat (er7/blank-fields 11)
                 [(if visit-number
                    (parser/create-field [visit-number])
                    (parser/create-field []))]
                 (er7/blank-fields 16)
                 [(if disposition-state
                    (parser/create-field (site-profile/code-for site-profile :discharge-disposition
                                                                 site-profile/standard-discharge-disposition-codes
                                                                 disposition-state))
                    (parser/create-field []))])))

(defn npu-segment
  "NPU (bed status update): NPU-1 the bed's PL -- the SAME datatype and
  the SAME `location-field` rendering PV1-3 uses -- and NPU-2 the bed
  status from HL7v2 Table 0116. Exactly two fields, which is the whole
  of the segment."
  [site-profile facility-name location status]
  (parser/create-segment
   "NPU"
   (er7/location-field facility-name location)
   (parser/create-field (site-profile/code-for site-profile :bed-status
                                               site-profile/standard-bed-status-codes status))))

(defn sch-segment
  "SCH, the scheduling segment SIU^S12 leads with.

  VERIFIED FROM THIS TREE'S OWN RESOLVED DEPENDENCIES, by reflection
  over `hapi-structures-v24` 2.6.0 rather than from memory: `SCH` has
  27 fields; SCH-1 and SCH-2 are `EI`, SCH-7 and SCH-25 are `CE`, and
  SCH-11 is `TQ`, whose 4th component is a `TS`. `SIU_S12`'s own
  segment names are `[MSH SCH NTE PATIENT RESOURCES]` and its `PATIENT`
  group is `[PID PD1 PV1 PV2 OBX DG1]` -- which is why `siu-message`
  renders PID and PV1 in that order and nothing between them.

  WHAT IS RENDERED, AND WHAT IS DELIBERATELY BLANK:

  * SCH-1 / SCH-2 -- placer and filler appointment ids, both the log's
    own `:appointment-id`. ONE id, rendered twice, because this project
    is both placer and filler; the point is that it is STABLE across an
    appointment's whole S12 -> S14/S15/S26 family, which is the reason
    `:reschedule` keeps the id rather than minting a new one
    (`event-schema`'s own `:reschedule` comment).
  * SCH-7 -- appointment reason, the log's `:reason`, escaped. Only
    `:appointment` carries one; the other three render it blank.
  * SCH-11 -- appointment timing, TQ-4 (start date/time) alone. FROM
    THE EVENT, never folded from the log: `:appointment` and
    `:reschedule` carry `:scheduled-t` and render it; `:appointment-
    cancel` and `:no-show` carry NO scheduled instant at all, so SCH-11
    is blank on both. That is a real limit of the event contract, not
    an omission here -- recovering it would mean folding an appointment
    timeline across the log, which is a second state derivation this
    renders-only namespace does not own.
  * SCH-25 -- filler status code, through the site profile's own
    `:appointment-status` table.
  * `:appointment-class` IS NOT RENDERED. It names the class of the
    FUTURE visit and its vocabulary is HL7 Table 0004 (the one PV1-2
    uses), not Table 0276/0277's appointment type; mapping one onto the
    other would be inventing a correspondence this tree cannot check.
    Stated rather than left as a blank field a reader has to explain."
  [site-profile {:keys [event appointment-id reason]} scheduled-ts]
  (apply parser/create-segment
         "SCH"
         (parser/create-field [appointment-id])
         (parser/create-field [appointment-id])
         (concat (er7/blank-fields 4)                          ; SCH-3 .. SCH-6
                 [(if reason
                    (parser/create-field [(er7/escape-er7 reason)])
                    (parser/create-field []))]             ; SCH-7
                 (er7/blank-fields 3)                          ; SCH-8 .. SCH-10
                 [(if scheduled-ts
                    (parser/create-field ["" "" "" scheduled-ts])
                    (parser/create-field []))]             ; SCH-11 (TQ-4)
                 (er7/blank-fields 13)                         ; SCH-12 .. SCH-24
                 [(parser/create-field
                   (site-profile/code-for site-profile :appointment-status
                                          site-profile/standard-appointment-status-codes
                                          (registry/siu-filler-status event)))])))

(defn orc-segment
  "ORC-1: order control -- \"NW\" (new order) is the only value this
  stage ever emits, since InjectChurn has no order-cancellation step
  (v1 scope). ORC-2: placer order number, reusing this message's own
  control-id (no separate order-numbering scheme needed yet).

  ARC 4 SWEEP 3 (ADR-0175 design (b)): the 3-arity additionally renders
  ORC-5 (Order Status) behind a two-field positional pad, from the
  site profile's own `:order-status` table. The 2-arity is unchanged and
  is what every pre-ladder call site still uses, so an order message
  with no ladder is byte-frozen -- ORC-2's own placer number, not a
  status, is what an un-laddered order has always said.

  ORC-2 IS THE RUNG'S OWN CONTROL ID, not the order's, because it is
  this message's own control id and that is what ORC-2 has always
  carried here. The link back to the order it restates is OBR-4 plus
  the rung's MSH-10, which encodes the basis instant -- stated because
  a reader could reasonably expect a placer number to be stable across
  a ladder, and in a real placer-numbered feed it would be."
  ([control-id]
   (parser/create-segment
    "ORC"
    (parser/create-field ["NW"])
    (parser/create-field [control-id])))
  ([control-id site-profile stage]
   (parser/create-segment
    "ORC"
    (parser/create-field ["NW"])
    (parser/create-field [control-id])
    (parser/create-field [])
    (parser/create-field [])
    (parser/create-field (site-profile/code-for site-profile :order-status
                                                site-profile/standard-order-status-codes
                                                stage)))))

(defn obr-segment
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
    (er7/cwe-field concept)))
  ([set-id concept clinical-ts]
   (parser/create-segment
    "OBR"
    (parser/create-field [(str set-id)])
    (parser/create-field [])
    (parser/create-field [])
    (er7/cwe-field concept)
    (parser/create-field [])
    (parser/create-field [])
    (parser/create-field [clinical-ts])))
  ;; ARC 4 SWEEP 3 (ADR-0175 design (b)): OBR-25 (Result Status), from
  ;; the site profile's own `:result-status` table, behind a 17-field
  ;; positional pad. THE PAD IS THE COST, and it is disclosed rather
  ;; than hidden: a result message that carries a ladder status grows
  ;; from 7 rendered OBR fields to 25. Every one of OBR-8..24 is a real
  ;; field this project does not populate, so the pad is the standard's
  ;; own positional arithmetic, not padding invented here.
  ([set-id concept clinical-ts site-profile stage]
   (apply parser/create-segment
          "OBR"
          (parser/create-field [(str set-id)])
          (parser/create-field [])
          (parser/create-field [])
          (er7/cwe-field concept)
          (parser/create-field [])
          (parser/create-field [])
          (parser/create-field [clinical-ts])
          (concat (er7/blank-fields 17)
                  [(parser/create-field
                    (site-profile/code-for site-profile :result-status
                                           site-profile/standard-result-status-codes
                                           stage))]))))

(defn obx-segment
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
  ([set-id clinical-ts result] (obx-segment set-id clinical-ts result nil nil))
  ([set-id clinical-ts {:keys [concept unit value reference-range abnormal-flag]}
    site-profile stage]
   (parser/create-segment
    "OBX"
    (parser/create-field [(str set-id)])
    (parser/create-field ["NM"])
    (er7/cwe-field concept)
    (parser/create-field [])
    (parser/create-field [(str value)])
    (parser/create-field [unit])
    (parser/create-field [(str (:low reference-range) "-" (:high reference-range))])
    (parser/create-field [(case abnormal-flag :normal "N" :low "L" :high "H")])
    (parser/create-field [])
    (parser/create-field [])
    ;; ARC 4 SWEEP 3 (ADR-0175 design (b)): OBX-11 (Observation Result
    ;; Status). It has been rendered as a positional blank since ADR-0142
    ;; put OBX-14 behind a pad, so the ladder FILLS a field that already
    ;; existed rather than widening the segment -- the OBR-25 half of the
    ;; same statement is a 17-field pad, and the asymmetry is worth
    ;; noticing.
    (if stage
      (parser/create-field (site-profile/code-for site-profile :observation-result-status
                                                  site-profile/standard-observation-result-status-codes
                                                  stage))
      (parser/create-field []))
    (parser/create-field [])
    (parser/create-field [])
    (parser/create-field [clinical-ts]))))

(defn observation-obx-segment
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
       (er7/cwe-field (first codes))
       (parser/create-field [])
       (if value-code (er7/coded-value-field value-code) (parser/create-field (if (some? value) [(str value)] [])))
       (parser/create-field (if unit [unit] []))]
      range-fields
      (repeatedly (- 13 rendered-so-far) #(parser/create-field []))
      [(parser/create-field [clinical-ts])]))))

(defn charge-concept
  "The coded fact a chargeable event carries, in `sim-model`'s own
  Concept shape, or nil for an event that is not chargeable.
  `:procedure` carries a `:codes` VECTOR and `:order-placed` a single
  `:concept`; the procedure's FIRST code is its primary one, and one
  line per code would bill a single procedure several times."
  [ev]
  (case (:event ev)
    :procedure (first (:codes ev))
    :order-placed (:concept ev)
    nil))


(defn ft1-segment
  "One FT1 charge line. Positions cited from `hapi-structures-v24` 2.6.0
  by instantiating the segment and asking it (ADR-0175 section 1(iv)):
  FT1-4 Transaction Date, FT1-6 Transaction Type, FT1-7 Transaction
  Code, FT1-10 Transaction Quantity, FT1-11 Transaction Amount -
  Extended, FT1-12 Transaction Amount - Unit, FT1-25 Procedure Code.

  FT1-7 IS RENDERED ON EVERY LINE, including procedure lines, and that
  is one place this differs from ADR-0175 section 2(c)'s own mapping
  (`:procedure` -> FT1-25, `:order-placed` -> FT1-7). FT1-7 is the
  TRANSACTION code -- what is being billed -- and a charge line with no
  FT1-7 is not a chargeable line at all; FT1-25 additionally names the
  procedure that gave rise to the charge, which is what section 2(c)
  was reaching for. A procedure line therefore carries both, an order
  or bed-day line only FT1-7.

  FT1-4 is the fact's own CLINICAL instant, never the DFT's transmit
  instant -- the same split-clock rule ADR-0109 gave EVN-2, OBR-7 and
  OBX-14: MSH-7 alone carries transmit time."
  [reference-date utc-offset set-id {:keys [at code display system quantity amount procedure?]}]
  (let [base [(parser/create-field [(str set-id)])
              (parser/create-field [])
              (parser/create-field [])
              (parser/create-field [(hl7-time/hl7-timestamp reference-date at utc-offset)])
              (parser/create-field [])
              (parser/create-field ["CG"])
              (er7/coded-value-field {:system system :code code :display display})
              (parser/create-field [])
              (parser/create-field [])
              (parser/create-field [(str quantity)])
              (parser/create-field [(er7/money (* quantity amount))])
              (parser/create-field [(er7/money amount)])]]
    (apply parser/create-segment
           "FT1"
           (if procedure?
             ;; FT1-13 .. FT1-24, then FT1-25.
             (concat base (er7/blank-fields 12)
                     [(er7/coded-value-field {:system system :code code :display display})])
             base))))
