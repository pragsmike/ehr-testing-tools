(ns ehrt.sim-emit-hl7.registry
  "The emitter's message-type catalog and the kind-sets and status
  ladders that select from it: `message-type-registry` itself, the
  three MSH-9 vocabularies derived from it, scheduling's four kinds
  and their SCH-25 states, the charge tables, chatter's kind map, and
  the two order/result status ladders.

  Extracted VERBATIM from `emit_hl7.clj`, the SECOND cluster of that
  file's namespace extraction (`.agents/plans/engine-extraction-
  census.md` sections 2 and 2a, `roadmap.md#engine-namespace-extraction-
  and-apply-unification`). It is a LEAF, and a stricter one than
  `hl7-time` was: not one form here calls anything outside this
  namespace, and none reaches a Java class, so it needs NO `:require`
  and no `:import` at all. The only cross-form references are the four
  INSIDE the cluster -- `skeleton-message-types` and `siu-event-kinds`
  read the registry, `emittable-message-types` reads the two
  vocabularies, and `siu-renders?` reads `siu-event-kinds`.

  `emit_hl7.clj` remains the namespace every existing requirer resolves
  against (author ruling C1(a)) and keeps a delegating def of each of
  the TEN public forms below -- seven of which `interface.clj`
  re-exports, the heaviest such cluster in the file. `siu-filler-status`
  and `charge-closing-kinds` are the two widenings this move forced:
  `sch-segment`, `event->messages` and `plan-charges` stayed behind and
  call them, so they are public here and `^:private` no longer, and
  they gain NO delegating def, because widening `emit_hl7.clj`'s own
  public surface is not what C1(a) asks for. `final-result-stage` arrived
  here DEAD -- no caller anywhere in the tree, then or since -- and the
  ruled repoint pass deleted it rather than carry it further.

  `message-type-registry`'s own comment text is CITED BY PATH from
  `components/patient-simulator/docs/limitations.md`'s care-plan row,
  which `ehrt.docs-tooling.patient-simulator-charter-test` resolves
  against the file named there. That citation points HERE now.")

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
   ;; SCHEDULING'S FOUR KINDS EARN THEIR ENTRIES HERE (arc 4 sweep 4,
   ;; 2026-08-28, ADR-0175 ruling B1's third tranche). The version
   ;; blocker went in sweep 1 -- `site-profile/default-msh` declares
   ;; "2.4" and the SIU structures are v2.4 -- and this is the sweep
   ;; ruling B1 put AFTER (A) for exactly that reason.
   ;;
   ;; THEY ARE THE FIRST ENTRIES IN THIS REGISTRY THAT DO NOT RENDER ON
   ;; THEIR OWN. An entry here has always meant "this kind renders";
   ;; these four mean "this kind renders WHEN `:siu` is on". The gate is
   ;; in `event->messages`, not here, and the reason it is not here is
   ;; that this map has three other readers -- `control-id-for`,
   ;; `skeleton-message-types` and the conformance gate's own vocabulary
   ;; check -- and every one of them wants the SIU families present
   ;; unconditionally. `skeleton-message-types` in particular: derived
   ;; from this map, so an SIU message is gated in FULL by `gate v2`'s
   ;; sampler from the moment it can exist, with no list to widen.
   ;;
   ;; THE TRIGGER MAPPING, AND EXACTLY WHAT THE JAR SETTLES. Verified
   ;; against `hapi-structures-v24` 2.6.0's own
   ;; `ca/uhn/hl7v2/parser/eventmap/2.4.properties`, which maps
   ;; `SIU_S13` .. `SIU_S24` and `SIU_S26` onto the structure `SIU_S12`.
   ;; So S12 (the structure itself), S14, S15 and S26 are all real v2.4
   ;; triggers resolving to one structure -- that much IS jar-verified,
   ;; and it is the whole of what the jar can say. NEITHER JAR CARRIES
   ;; HL7 TABLE 0003, measured rather than assumed: no resource in
   ;; `hapi-base` or `hapi-structures-v24` contains a trigger-event
   ;; DESCRIPTION at all. So S14-vs-S13 for a reschedule notification is
   ;; settled by the EVENT CONTRACT, not by the jar:
   ;; `event-schema.clj`'s own `:reschedule` doc says SIU^S14 at
   ;; contract 1.7.0, `docs/formats.md` renders it, and the frozen
   ;; baseline carries it. `notes/adr/0174-*.md`:697 enumerates
   ;; "S12/S13/S15/S26" and is the lone surface that disagrees; it is
   ;; not the contract, and this sweep's own fences forbid a schema
   ;; diff. The contract wins, S14 stands, and the disagreement is
   ;; recorded here rather than silently resolved.
   ;;
   ;; THE FOLD ARM THE OLD COMMENT OWED IS PAID, and not as an
   ;; `evolve-entry` arm. An unhandled MSH-9 throws out of that `case`,
   ;; which kills the emitter-coherence property rather than failing it
   ;; softly (ADR-0175 section 1(iii)) -- but SIU is not an ADT, and an
   ;; appointment asserts nothing about a visit. `v2-replay/fold-message`
   ;; skips the whole SIU FAMILY before dispatch, so the trigger never
   ;; reaches `evolve-entry` and no accumulator entry is bootstrapped
   ;; from an appointment's PID for a patient who has not arrived.
   :appointment {:type "SIU" :trigger "S12"}
   :reschedule {:type "SIU" :trigger "S14"}
   :appointment-cancel {:type "SIU" :trigger "S15"}
   :no-show {:type "SIU" :trigger "S26"}
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

(def skeleton-message-types
  "Every MSH-9 this emitter's own registry produces, as `TYPE^TRIGGER`.

  ARC 4 SWEEP 2 (ADR-0175 design (h), ruling D1). `gate v2`'s sampling
  policy splits the wire into SKELETON families -- gated in full,
  always -- and ADD-ON families, which are stratified and capped. This
  is the skeleton half, DERIVED from `message-type-registry` rather
  than listed anywhere, so a registry entry a later sweep adds is gated
  in full from the moment it exists rather than from the moment
  somebody remembers to widen a set.

  What is NOT here is the whole of what arc 4 puts on the wire:
  chatter's ADT^A08/A31/A28 and charges' DFT^P03 have no registry entry
  by design (each is derivable restatement, `rulings.md#R-skeleton-or-
  emission`), so they are exactly the add-on half."
  (into #{} (map (fn [{:keys [type trigger]}] (str type "^" trigger)))
        (vals message-type-registry)))

(def add-on-message-types
  "Every MSH-9 arc 4's emission ADD-ONS put on the wire, and the exact
  complement of `skeleton-message-types` above. Each is DERIVABLE
  RESTATEMENT with no registry entry by design
  (`rulings.md#R-skeleton-or-emission`): chatter's three ADT triggers
  (`chatter-trigger`'s own rule picks between A08 and A31 per
  event; `:registered` is always A28) and charges' DFT^P03.

  DECLARED here rather than derived, because there is nothing to derive
  it FROM -- an add-on message is built by a builder, not looked up in a
  map. `fan_out_run_test/the-emitter-produces-nothing-outside-the-
  declared-vocabulary` is what keeps this set honest: over a run with
  every add-on on, the MSH-9s actually emitted must be a subset of
  `emittable-message-types`, and each of the four strings here must be
  witnessed. A fifth add-on family that forgets this set turns that gate
  red rather than shipping a fan-out filter that cannot name it."
  #{"ADT^A08" "ADT^A31" "ADT^A28" "DFT^P03"})

(def emittable-message-types
  "The WHOLE vocabulary this emitter can put on a wire: skeleton plus
  add-on. It is exactly the allow-list a `:fan-out` filter's own
  `:message-types` may name (ADR-0175 section 2(f)) -- THE ALLOW-LIST
  LAW: naming a `TYPE^TRIGGER` this emitter cannot produce is a
  configuration ERROR rejected before the engine runs
  (`ehrt.sim.run`'s own `:unknown-fan-out-message-type` branch, the
  `:invalid-siu` precedent), never a subscriber feed that is silently
  empty because of a typo."
  (into skeleton-message-types add-on-message-types))

(def siu-event-kinds
  "The four ground-truth kinds this family renders -- the SAME four
  `ehrt.sim-engine.event-schema` declared at contract 1.7.0 and left
  unrendered, and the exact key set of the registry's own SIU block.
  Derived from the registry rather than listed twice, so the two cannot
  disagree."
  (into #{} (keep (fn [[kind {:keys [type]}]] (when (= "SIU" type) kind)))
        message-type-registry))

(defn siu-renders?
  "Does `siu` (the `:siu` emission profile: nil/absent = off, a map =
  on) ask for a message for `event`?

  THE SHAPE IS AN ON/OFF PLUS AN OPTIONAL ALLOW-LIST, and the defaults
  are stated rather than implied: `{}` (or any map with no `:triggers`)
  means ALL FOUR kinds, and `{:triggers [...]}` means exactly the kinds
  named. Absent or nil is off, which is what makes a run that never
  names the key byte-identical to one from before this sweep -- the
  same three-way absent/nil/{} agreement `:site-profile` has, except
  that here `{}` is ON, because the key's presence IS the opt-in and a
  key that meant nothing when empty would be a knob with a silent
  no-op setting.

  The allow-list is EVENT KINDS, not HL7 trigger strings, for the
  reason every other emission profile in the emitter takes engine
  vocabulary: a config author names what happened, and the trigger is
  this registry's business."
  [siu event]
  (boolean (and (map? siu)
                (contains? siu-event-kinds event)
                (let [triggers (:triggers siu)]
                  (or (nil? triggers) (contains? (set triggers) event))))))

(def siu-filler-status
  "Event kind -> the SCH-25 state
  (`site-profile/standard-appointment-status-codes`' own vocabulary).

  A RESCHEDULE IS STILL `:booked`. SIU^S14 is what says the appointment
  moved; SCH-25 says what state it is in once it has, and a rescheduled
  appointment is a booked one at a new instant."
  {:appointment :booked
   :reschedule :booked
   :appointment-cancel :cancelled
   :no-show :no-show})

(def room-and-board-code
  "The reserved price-table key for the per-inpatient-day room-and-board
  line. It is a billing code this project mints for ITSELF: `:procedure`
  and `:order-placed` lines carry codes the log already holds, and an
  occupied bed-day carries none, so the table has to name one. Absent
  from the table, every bed-day line is a counted skip like any other
  unpriced code -- there is no default price anywhere in the
  emitter, deliberately."
  "ROOM-BOARD")

(def charge-closing-kinds
  "The two kinds that CLOSE an encounter, and therefore the two instants
  a DFT is emitted at (ADR-0175 section 2(c)). `:outpatient-visit-end`
  is one of them even though it renders no ADT of its own -- its
  `message-type-registry` silence is a statement about ADT traffic
  (many real ambulatory feeds send an A04 and no closing message), not
  about billing, and a same-day visit is still billed."
  #{:discharge :outpatient-visit-end})

(def chatter-event-kinds
  "The three ground-truth kinds an event-driven chatter rule may cover,
  mapped to what their restatement carries. The TRIGGER is derived, not
  configured (ADR-0175 section 2(a)): `:registered` is always A28 (a
  registration is person-scoped by definition), and the other two are
  A08 when the basis event happened inside an open encounter -- which
  the event says on its own face, via `:encounter-id` -- and A31 when
  it did not.

  A `:coverage-change` restatement is the IN1-ONLY update: its PID is
  the same PID any other message for that patient at that instant would
  carry (nothing about a payer change moves it), and the IN1 is what
  actually changed."
  {:demographic-update {:in1? false}
   :coverage-change    {:in1? true}
   :registered         {:in1? false :trigger "A28"}})

(def order-status-ladder
  "The ORC-5 stage an ORM^O01 rung carries, by rung index, SATURATING at
  the last entry. Two stages, then the ladder holds: a third and fourth
  order rung both say `:in-progress`, which is what a real order-status
  feed says when nothing has changed but time.

  THE STAGES ARE KEYWORDS, NOT CODES. Every code string this ladder
  renders comes from `ehrt.sim-emit-hl7.site-profile`'s own
  `:order-status` table at render time, so a site overrides the
  vocabulary without touching the ladder's shape (ADR-0175 section
  2(b): tables 0038/0123/0085 are in no jar and no resource in this
  tree, so they ship as declared, overridable data and are never
  asserted as an HL7 citation)."
  [:scheduled :in-progress])

(def result-status-ladder
  "The OBR-25/OBX-11 stage an ORU^R01 rung carries, by rung index,
  saturating the same way. ONE stage, deliberately: every rung this
  project can render carries the order's own analyte values (the log
  holds one result per order), and `:preliminary` -- HL7's \"a verified
  early result is available, final not yet obtained\" -- is what that
  is. A second stage would have to mean something the log does not
  distinguish."
  [:preliminary])
