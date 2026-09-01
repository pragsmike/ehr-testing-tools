(ns ehrt.sim-engine.state
  "The patient accumulator: `PatientState`, the five records it nests,
  its two constructors, and the state-at-t demographics they carry --
  `engine.clj`'s second extraction under
  `roadmap.md#engine-namespace-extraction-and-apply-unification`
  (the census's own dependency order, `.agents/plans/engine-extraction-
  census.md` section 3a: `state` must land before `evolve`, and `evolve`
  before `fold`).

  Extracted OUTPUT-IDENTICAL: every form below is `engine.clj`'s own
  text, moved and not rewritten -- including the `M6 Task 1` header
  comment block above `ConditionRecord`, which is part of the cluster
  because `PatientState`'s own docstring cites it by position. Under
  ruling C1(a) `engine.clj` requires this namespace and keeps a
  delegating def for each of the thirteen vars that were public there,
  so every existing requirer -- and `ehrt.sim-engine.interface`, which
  re-exports none of them -- still resolves against `engine.clj`, and no
  test file changed.

  ONE var is public HERE and gets no delegating def:
  `observation-value-fields`, which was `defn-` in `engine.clj`. It is
  the census's single CYCLE BREAKER. `decide` -> `log-index` -> `fold`
  -> `evolve` -> `decide` closed only through this one form, which
  `decide :observation` and `evolve :observation`/`:diagnostic-report`
  all call; moving it down below every one of them is what makes the
  rest of the engine's cluster graph a DAG. Its natural home is here
  because what it computes IS `ObservationRecord`'s own value fields.
  It gets no delegating def per constraint 5 -- that would widen the
  engine's public surface, which C1(a) does not ask for.

  This namespace is a LEAF within `sim-engine`: it reaches only
  `sim-model` and malli, and nothing in `sim-engine` below it."
  (:require [ehrt.sim-model.interface :as sim-model]
            [malli.core :as m]
            [malli.util :as mu]))

;; --- M6 Task 1: the clinical-content accumulator -------------------------
;; EmitState's own snapshot-at-instant law (docs/sim-theory.edn) means the
;; FHIR emitter touches NOTHING but folded state, never the log directly
;; -- so Condition/Observation/MedicationRequest content has to actually
;; LAND in the fold, the same way :location/:persona already do, rather
;; than staying a log-only fact only ehrt.sim-check.check reads via
;; `replay`. Each record below is intentionally the smallest shape that
;; carries what the FHIR builders need, not a re-derivation of the whole
;; originating event.

(def ConditionRecord
  "One condition, folded from a compiled encounter step's own
  :conditions annotations (sim-model/ConditionAnnotation,
  ehrt.patient-simulator.compile-trajectory's own annotate-condition). Scope
  note: only conditions attached to an OPERATIONAL encounter step
  (:admission/:outpatient-visit) are folded here -- :registered's own
  :pre-horizon-facts (registration-time, pre-run history) are a
  documented v1 scope boundary, not yet accumulated (CDA-style: deferred
  with a contract note, not silently dropped)."
  [:map
   [:codes {:optional true} [:maybe [:vector sim-model/Concept]]]
   [:citation sim-model/Citation]
   [:onset-t :int]
   [:clinical-status [:enum :active :resolved]]
   [:end-t {:optional true} :int]
   ;; ADR-0174 section 2(a) (arc 3b sweep 1): WHICH encounter this
   ;; condition was recorded during. Absent on every record of every run
   ;; that did not opt into `:encounters`, which is what keeps
   ;; `sim-emit-fhir`'s Condition.encounter reference byte-identical
   ;; there; present, it is what stops a condition recorded during
   ;; visit 2 being attributed to visit 1.
   [:encounter-id {:optional true} :string]])

(def ObservationRecord
  "One observation -- a GMF `:observation` event, a single analyte
  flattened out of a `:result-available` event's own :results
  (order-profiles' richer shape: adds :reference-range/:interpretation,
  the computed abnormal flag), or a single child flattened out of a
  GMF coverage Wave D `:diagnostic-report` event's own :observations
  (ADR-0029 P5 -- the SAME per-analyte flattening pattern, reused, not
  a third accumulator shape). :value-code/:category are new this wave
  (D1a schema RULING P2/Q1); :reference-range/:interpretation, already
  present for :result-available, are now also populated for a
  vital-sign-reference-table-sourced :diagnostic-report child (Q2+Q3).
  Optional fields absent rather than nil for the plain-GMF case -- 'no
  invented fields' (M6 Task 1)."
  [:map
   [:codes [:vector sim-model/Concept]]
   [:t :int]
   [:value {:optional true} number?]
   [:unit {:optional true} :string]
   [:value-code {:optional true} sim-model/Concept]
   [:category {:optional true} :string]
   [:reference-range {:optional true} [:map [:low number?] [:high number?]]]
   [:interpretation {:optional true} [:enum :normal :low :high]]
   ;; ADR-0174 section 2(a) -- see ConditionRecord's own entry.
   [:encounter-id {:optional true} :string]])

(def MedicationOrderRecord
  "One medication order, folded from :medication-order and closed by a
  citation-matching :medication-end (the SAME position-independent,
  citation-based resolution ehrt.patient-simulator.compile-trajectory already
  uses throughout, extended to fold time instead of compile time)."
  [:map
   [:codes {:optional true} [:maybe [:vector sim-model/Concept]]]
   [:citation sim-model/Citation]
   [:ordered-t :int]
   [:status [:enum :active :completed]]
   [:ended-t {:optional true} :int]
   ;; ADR-0174 section 2(a) -- see ConditionRecord's own entry.
   [:encounter-id {:optional true} :string]])

(def CarePlanRecord
  "GMF coverage Wave D stage D2 (2026-08-02, ADR-0029 R2(b)): one care
  plan, folded from :care-plan-start and closed by a citation-matching
  :care-plan-end -- the SAME position-independent, citation-based
  resolution MedicationOrderRecord already establishes one step up.
  CarePlan itself is v2-silent (R3) -- this record exists for the fold
  and a future sim-emit-fhir consumer, no HL7v2 rendering reads it."
  [:map
   [:codes {:optional true} [:maybe [:vector sim-model/Concept]]]
   [:activities {:optional true} [:maybe [:vector sim-model/Concept]]]
   [:citation sim-model/Citation]
   [:started-t :int]
   [:status [:enum :active :completed]]
   [:ended-t {:optional true} :int]
   ;; ADR-0174 section 2(a) -- see ConditionRecord's own entry.
   [:encounter-id {:optional true} :string]])

(def Demographics
  "STATE-AT-T demographics -- what a patient's demographic facts are AT
  ONE INSTANT, as opposed to `:persona`, which is and stays the t0
  sample (ADR-0173 section 2(b)). Seeded at `:registered` from that
  patient's own Persona; from arc 3a part 3 on, the person stream's
  `:demographic-update` and `:coverage-change` siblings fold onto it.

  Persona-shaped, field for field, and the field schemas are taken FROM
  `sim-model/Persona` rather than restated, so the two cannot drift.
  Three deliberate differences, each from ADR-0173 section 2(b):

  * `:address` becomes `:residence`, a SUM. A places row cannot express
    the absence of a residence at all, and `sim-model/Persona`'s own
    `:address` is required and non-nilable -- widening it would move every `:registered` event
    in every corpus for a fact that belongs to state-at-t and not to a
    t0 sample. `:unknown` is the placeholder-registration case (section
    2(d)), distinct from `:unhoused`: not knowing where somebody lives
    is not the same fact as their having nowhere to live, and ruling E1
    keeps that distinction in ground truth even though both render
    PID-11 absent on the wire.
  * `:identity` marks a placeholder registration -- an arrival landing
    inside an open `:identity-unavailable` window.
  * `:age` is NOT carried. It is a t0 derivation of `:dob` against a
    fixed calendar anchor, so it is not state-at-t at all.

  ARC 3A PART 4: FIVE FIELDS BECAME `{:optional true}`, and that is the
  placeholder registration and nothing else. A patient who arrived
  unidentified has a `:name` -- the window's own `:alias-name`, a John
  Doe -- and genuinely has no known sex, DOB, phone, SSN or payer. The
  alternative was to carry the person's REAL values under an alias
  name, which is a fabrication with a wire face: a John Doe rendering a
  correct date of birth tells a consumer's MPI something the modelled
  hospital does not know. `:name`, `:residence` and `:identity` stay
  REQUIRED: every one of the three has a true value in the placeholder
  case (the alias, `:unknown`, and `:placeholder`), so making them
  optional would buy nothing and lose a constraint."
  [:map
   [:name (mu/get sim-model/Persona :name)]
   [:sex {:optional true} (mu/get sim-model/Persona :sex)]
   [:dob {:optional true} (mu/get sim-model/Persona :dob)]
   [:phone {:optional true} (mu/get sim-model/Persona :phone)]
   [:ssn {:optional true} (mu/get sim-model/Persona :ssn)]
   [:payer {:optional true} (mu/get sim-model/Persona :payer)]
   [:residence [:multi {:dispatch :status}
                [:housed [:map
                          [:status [:= :housed]]
                          [:address (mu/get sim-model/Persona :address)]]]
                [:unhoused [:map
                            [:status [:= :unhoused]]
                            [:last-known-address {:optional true}
                             (mu/get sim-model/Persona :address)]]]
                [:unknown [:map [:status [:= :unknown]]]]]]
   [:identity [:enum :known :placeholder]]])

(defn demographics-from-persona
  "A patient's INITIAL state-at-t demographics, read off their t0
  Persona. Every housed field copies straight across; `:address` becomes
  a `:housed` residence; `:identity` is `:known`, because a patient with
  a Persona is by definition not a placeholder (ADR-0173 section 2(d) is
  what mints the `:placeholder` ones, in part 3).

  nil in, nil out: `evolve :registered` is total over hand-authored logs
  too, and a `:registered` event with no `:persona` at all already folds
  to `:persona nil` today."
  [persona]
  (when persona
    {:name (:name persona)
     :sex (:sex persona)
     :dob (:dob persona)
     :phone (:phone persona)
     :ssn (:ssn persona)
     :payer (:payer persona)
     :residence {:status :housed :address (:address persona)}
     :identity :known}))

(defn placeholder-demographics
  "The state-at-t demographics of a patient who arrived inside an open
  `:identity-unavailable` window (ADR-0173 section 2(d), arc 3a part 4).

  The window's `:alias-name` and NOTHING ELSE. No address -- `:unknown`
  rather than `:unhoused`, because not knowing where somebody lives is
  a different fact from their having nowhere to live, and ruling E1
  keeps the two apart in ground truth even though both render PID-11
  absent. No DOB, no sex, no payer: the person behind this record is
  somebody, and the run's own `:persona` on the registration event says
  who, but the modelled hospital does not know it yet and the wire may
  not claim otherwise."
  [alias-name]
  {:name alias-name :residence {:status :unknown} :identity :placeholder})

(def PatientLocation
  "The {:ward :bed :placement} map an admitted patient's `:location`
  holds -- factored out of `PatientState` (where it was inline) so
  `EncounterRecord` below can name the SAME shape rather than restate
  it. No value moves: this is the identical schema, given a name."
  [:map
   [:ward :string]
   [:bed :string]
   [:placement [:enum :licensed :surge]]])

(def EncounterRecord
  "ONE encounter (ADR-0174 section 2(a), arc 3b sweep 1). `PatientState`
  holds the OPEN one under `:encounter` and every CLOSED one under
  `:encounters`, accumulating exactly the way `:conditions` and
  `:care-plans` already do.

  THE OPEN RECORD IS DELIBERATELY THIN -- `:encounter-id`, `:ordinal`
  and the opener's own instant, and nothing else. The seven
  single-encounter-assumed fields of `PatientState` (`:status`,
  `:class`, `:home-ward`, `:location`, `:attending`, `:admitted-at`,
  `:discharged-at`) STAY where they are and ARE the open encounter's
  projection, unchanged in shape and unchanged in value while an
  encounter is open, which is why every reader in the emitters, the
  checks and the board is untouched by this field's arrival. Duplicating
  them onto the open record would create a second place for a transfer
  to have to update.

  A CLOSED record is that projection, SNAPSHOT at the closing event --
  taken after the closer's own field changes, so a discharged
  encounter's `:location` is nil exactly as the discharged patient's is.
  That snapshot is what `evolve :discharge` now has somewhere to put
  instead of throwing away.

  `:cancelled` marks an encounter a `:cancel-admit` un-did. It is kept
  in `:encounters` rather than dropped so `:ordinal` can never be
  REUSED -- an id minted for an admission that was cancelled must not be
  minted again for the patient's next one, or
  `every-encounter-is-opened-and-closed-or-still-open` would see two
  openers carrying one id."
  [:map
   ;; Absent on every record of every run with no `:encounters` key: the
   ;; records are folded either way (so the invariants below are never
   ;; vacuous), but nothing MINTS an id unless the run opted in.
   [:encounter-id {:optional true} :string]
   [:ordinal :int]
   [:class {:optional true} [:maybe [:enum :inpatient :emergency :outpatient
                                     :preadmit :recurring :obstetrics]]]
   [:home-ward {:optional true} [:maybe :string]]
   [:location {:optional true} [:maybe PatientLocation]]
   [:attending {:optional true} [:maybe :string]]
   [:status {:optional true} [:maybe [:enum :new :admitted :discharged :merged :expired]]]
   [:admitted-at {:optional true} [:maybe :int]]
   [:discharged-at {:optional true} [:maybe :int]]
   [:cancelled {:optional true} :boolean]
   ;; ARC 3B SWEEP 3 (ADR-0174 section 2(b)): the appointment this
   ;; encounter was KEPT against, present only on an encounter a
   ;; scheduled arrival or a follow-up opened. It is the single field
   ;; that makes `scheduled-encounter-follows-its-appointment`
   ;; non-vacuous -- and it is non-vacuous only because sweep 1 landed,
   ;; since without a SECOND encounter every appointment would trivially
   ;; precede its patient's first and only visit.
   [:appointment-id {:optional true} :string]])

(def AppointmentRecord
  "ONE appointment (ADR-0174 section 2(b), arc 3b sweep 3). `PatientState`
  holds the OPEN one under `:appointment` and every TERMINAL one under
  `:appointments` -- deliberately the same two-field shape
  `:encounter`/`:encounters` establishes, because an appointment has the
  same life cycle as an encounter: exactly one open at a time, and a
  closed one that must never have its ordinal reused.

  TERMINAL MEANS ONE OF THREE, AND ONLY EVER ONE. `:outcome` is `:kept`,
  `:cancelled` or `:no-show`; `appointment-reaches-at-most-one-terminal`
  is what asserts the exclusivity over the LOG, and this field is what
  makes it exclusive in the STATE. A `:reschedule` is NOT terminal -- it
  moves `:scheduled-t` and leaves the record open, which is exactly why
  it keeps its own id rather than minting a second.

  `:prior-scheduled-t` is present only on a record a reschedule moved,
  and is the shape `demographic-update-reports-a-real-change` already
  established for a change: prior and current on ONE record, never two."
  [:map
   [:appointment-id :string]
   [:ordinal :int]
   [:booked-at :int]
   [:scheduled-t :int]
   [:appointment-class [:enum :inpatient :emergency :outpatient
                        :preadmit :recurring :obstetrics]]
   [:reason {:optional true} [:maybe :string]]
   [:prior-scheduled-t {:optional true} :int]
   [:outcome {:optional true} [:enum :kept :cancelled :no-show]]])

(def PatientState
  "The engine's per-patient accumulator -- what folding `evolve` over a
  patient's own event subsequence produces (docs/patient-state-model.md
  is the full design spec). `:patient-id` is the fold/queue key
  (sim/ADR-0010); `:mrns`/`:active-mrn` are what `:mrn` became once MRN
  moved into state -- a singleton set until M2b's merge exists to grow
  it. As of Milestone M1: :location is the {:ward :bed :placement} map
  (upgraded from v0's bare ward-name string, alongside the allocation
  ladder that populates it for real); :class/:attending/
  :admitted-at are populated at admission; :attributes remains
  reserved, unused until M5.

  Milestone M4: `:persona` (sim-model/Persona -- name,
  DOB, sex, address, phone, SSN-shaped id, and payer, ALL of it,
  including payer) is populated once, by the `:registered` event every
  patient's step queue is now prepended with (`run`'s own docstring),
  never resampled after (the attribute-pool contract). This RETIRES the
  standalone `:payer` field components/sim/docs/operational-models.md described as an
  engine-patient-init stand-in: there was no code actually setting it
  (always nil), so retiring it means removing the now-redundant field
  from this schema, not deleting behavior -- payer now lives at
  `(:payer (:persona patient))`, sampled by Persona alongside every
  other demographic fact, per that document's own 'Persona subsumes it'
  note.

  Milestone M6: `:discharged-at` mirrors `:admitted-at` (Encounter.period's
  own end instant); `:conditions`/`:observations`/`:medication-orders`
  (ConditionRecord/ObservationRecord/MedicationOrderRecord, above) are
  the clinical-content accumulator EmitState renders from -- see this
  namespace's own header comment just above `PatientState` for why this
  content had to actually enter the fold rather than stay log-only."
  [:map
   [:patient-id :string]
   [:mrns [:set :string]]
   [:active-mrn :string]
   ;; GMF coverage Wave C (2026-08-02, ADR-0028, C3): :expired lands for
   ;; real -- docs/patient-state-model.md's own accumulator table has
   ;; named this value since M2b-era design, but no code path could
   ;; produce, read, or check it until now (this wave's own gap table,
   ;; components/patient-simulator/docs/gmf-interpreter.md section 10).
   [:status [:enum :new :admitted :discharged :merged :expired]]
   [:class {:optional true} [:enum :inpatient :emergency :outpatient
                              :preadmit :recurring :obstetrics]]
   [:home-ward {:optional true} [:maybe :string]]
   [:location {:optional true} [:maybe PatientLocation]]
   [:attending {:optional true} [:maybe :string]]
   [:persona {:optional true} [:maybe sim-model/Persona]]
   ;; ADR-0173 section 2(b) (arc 3a): state-at-t, seeded at :registered
   ;; from :persona and folded on by the person stream's own siblings
   ;; from part 3 on. `:persona` above is NOT mutated -- it stays the t0
   ;; record, so all fourteen t0-only census sites are untouched and
   ;; `registered-persona-is-schema-valid` keeps asserting exactly what
   ;; it asserts today. Nothing READS this field yet.
   [:demographics {:optional true} [:maybe Demographics]]
   [:admitted-at {:optional true} [:maybe :int]]
   [:discharged-at {:optional true} [:maybe :int]]
   [:conditions {:optional true} [:vector ConditionRecord]]
   [:observations {:optional true} [:vector ObservationRecord]]
   [:medication-orders {:optional true} [:vector MedicationOrderRecord]]
   [:care-plans {:optional true} [:vector CarePlanRecord]]
   ;; ADR-0174 section 2(a) (arc 3b sweep 1): the encounter, made
   ;; explicit. `:encounter` is the OPEN one (nil between encounters),
   ;; `:encounters` every CLOSED one in the order they closed. BOTH are
   ;; folded whether or not the run opted into `:encounters` -- the
   ;; records cost no emitted byte without an id (nothing renders them,
   ;; `sim-emit-fhir`'s own legacy arm), and folding them
   ;; unconditionally is what keeps `admission-only-when-no-open-
   ;; encounter` a real predicate on a legacy log instead of a
   ;; vacuously-true one.
   [:encounter {:optional true} [:maybe EncounterRecord]]
   [:encounters {:optional true} [:vector EncounterRecord]]
   ;; ARC 3B SWEEP 3 (ADR-0174 section 2(b)): appointments, the same two
   ;; layers for the same reason. `:appointment` is the OPEN one (nil
   ;; between appointments), `:appointments` every TERMINAL one in the
   ;; order it went terminal.
   ;;
   ;; UNLIKE `:encounter`/`:encounters` THESE ARE FOLDED ONLY WHERE THE
   ;; EVENTS EXIST, and that asymmetry is not an oversight. The encounter
   ;; records are folded unconditionally because their openers
   ;; (`:admission`, `:outpatient-visit`) exist in EVERY run, so folding
   ;; them is what keeps `admission-only-when-no-open-encounter` a real
   ;; predicate on a legacy log. An `:appointment` event exists in no run
   ;; that did not opt in, so there is nothing to fold and no invariant
   ;; that could go vacuous by not folding it.
   [:appointment {:optional true} [:maybe AppointmentRecord]]
   [:appointments {:optional true} [:vector AppointmentRecord]]
   [:attributes {:optional true} [:map-of :keyword :any]]])

(defn valid-patient?
  "Validates a patient accumulator against PatientState -- the same
  valid?/explain convention ehrt.sim-model.pathway already uses."
  [patient]
  (m/validate PatientState patient))

(defn initial-patient
  "The state a patient starts in when its arrival is scheduled --
  `evolve`'s fold origin. The single place this shape is constructed,
  so tests reconstructing state independently (the fold-consistency
  property) start from the same place the engine itself does.
  sim/ADR-0010: keyed by `patient-id`, not `mrn` -- `mrn` is the patient's
  starting (and, until M2b's merge, only) MRN."
  [patient-id mrn]
  {:patient-id patient-id :mrns #{mrn} :active-mrn mrn :status :new})

;; --- the cycle breaker ----------------------------------------------------
;;
;; `observation-value-fields` was `defn-` in `engine.clj` and is public
;; here for the reason the ns docstring gives: it is the ONE form the
;; census's `decide` -> `log-index` -> `fold` -> `evolve` -> `decide`
;; cycle closes through. Its text is `engine.clj`'s own, verbatim; the
;; "shared by `decide :observation` and `decide :diagnostic-report`"
;; clause below is inherited AS IT STOOD and is imprecise -- the live
;; sharers are `decide :observation` and `evolve :observation`/
;; `:diagnostic-report`, `decide :diagnostic-report` passing its
;; `:observations` through whole. Named in this session's record rather
;; than silently corrected, because the extraction's whole claim is that
;; the moved text is unchanged.

(defn observation-value-fields
  "value/unit/value-code/category/reference-range/interpretation,
  verbatim (code passthrough law) -- GMF coverage Wave D stage D1
  (ADR-0029 P2, D1a schema RULING Q1/Q2+Q3): shared by `decide
  :observation` and `decide :diagnostic-report` (one per child, below),
  the SAME reuse `ehrt.patient-simulator.compile-trajectory/observation-
  fields` already establishes one layer down -- never re-derived, only
  carried through."
  [{:keys [value unit value-code category reference-range interpretation]}]
  (cond-> {}
    (some? value) (assoc :value value)
    unit (assoc :unit unit)
    value-code (assoc :value-code value-code)
    category (assoc :category category)
    reference-range (assoc :reference-range reference-range)
    interpretation (assoc :interpretation interpretation)))
