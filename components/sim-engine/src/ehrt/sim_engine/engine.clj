(ns ehrt.sim-engine.engine
  "The discrete-event simulation core: a priority queue of pending
  events ordered by simulated time, a world of patient states (plus,
  from Milestone M1 on, the static facility/provider config decide
  needs to read), and the decide/evolve pair (`sim/ADR-0008`) that replaces
  a single fused transition function. Architecture mined from Google's
  Simulated Hospital (pkg/state WrappedQueue + pkg/hospital
  RunNextEventIfDue).

  Event-sourcing doctrine (`sim/ADR-0008`): the ground-truth log is the only
  primitive. `decide` (streams, t, world, patient-id, step) -> {:events
  :advance} consults the current world (every patient's state so far,
  plus facility/provider config -- read-only) and the run's seeded RNG
  streams (ADR-0171: one per patient, plus two run-scoped)
  to decide what happens, but never returns a new state -- this is
  where cross-patient coupling lives (a discharge's decide call may
  also emit a transfer event for a DIFFERENT patient, the bed-ready
  transfer for a boarding patient, docs/operational-models.md).
  `evolve` (patient-state, event) -> patient-state' is pure, total, and
  narrower: no RNG, no knowledge of world or of any patient but the one
  the event names. The ONLY path by which patient state changes is
  folding emitted events through `evolve`; docs/patient-state-model.md
  is PatientState's design spec, docs/sim-theory.md open question #3
  (state-history is derived, not primitive) is this ADR's resolution.

  Identity doctrine (`sim/ADR-0010`, M2a): `:patient-id` -- not `:mrn` -- is
  the fold key and the work-queue key. `:mrn` moves into state as
  {:mrns #{...} :active-mrn ...}, because a real hospital's MRN is
  exactly the identifier merge (M2b) changes; patient-id never
  reassigns and never rebinds. Every event carries `:participants`, a
  vector of {:patient-id :role} -- single-element with role :subject
  for every event type this project has today (the degenerate case);
  a patient's state folds exactly the events they participate in.
  `patient-id-for` is a PURE function of this run's seed and a
  patient's arrival ordinal -- deliberately off the seeded RNG stream,
  so identity generation adds no new stochastic draws for `sim/ADR-0009`'s
  seed-stability accounting to track (unlike NPI generation, which IS
  an RNG draw, `sim/ADR-0007`).

  Time doctrine (`sim/ADR-0011`, M2a): the engine clock (every event's :t) is
  now integer SECONDS from run start, not minutes. The pathway IR is
  NOT changed -- :delay's :from/:to stay minutes, authoring ergonomics
  -- the engine converts minutes -> seconds itself, at the one place a
  minute-denominated draw becomes a clock advance. A warm-up window
  (:warm-up-seconds, default 0) marks every event with `:t <
  warm-up-seconds` as `:warm-up true`; the log stays complete (no
  trimming here -- `sim/ADR-0011` leaves trimming, if any, to Package).

  Determinism doctrine: ALL randomness flows from java.util.Randoms
  DERIVED IN `run` FROM THE ONE SEED. No other entropy source (wall
  clock, hash ordering, nondeterministic seq realization) may
  influence output. Same config + seed => identical output, byte for
  byte once serialized -- WITHIN a version; see `sim/ADR-0009`
  for the cross-version seed-stability policy Milestone M1's new RNG
  draws (bed choice, attending sampling) triggered, and M2a's identity/
  time changes triggered again (documented once, per the M2a session
  plan, not per-commit).

  Stream partition (ADR-0171, arc 1): the path is now PLURAL. Until
  this arc there was exactly one Random and consumption order was
  GLOBAL EVENT ORDER, so adding, removing or reordering a single draw
  anywhere shifted every later draw for every later patient -- which is
  what made arcs 2-4 (newborns mid-run, new decide draws, emission
  chatter) each cost a corpus-wide reshuffle. `run` now derives five
  streams by family (`stream-family-tag`), keyed on a stable id rather
  than on construction order, and `decide` takes the stream MAP. A draw
  added to one patient's pathway now moves that patient. The run-scoped
  families (`:world`, `:facility`) are where cross-patient coupling
  still lives -- named and confined, not abolished: their draw counts
  are conditional on the population, so no per-patient stream can own
  them (ADR-0171 section 2(a)).

  The RNG-path law itself is unchanged: a measurement claiming to
  characterize the simulator's output must still draw from the real
  seeded, threaded path (`rulings.md#R-measure-claimed-population`).
  That path simply became plural.

  Step vocabulary: v0's :admission/:delay/:discharge, plus Milestone
  M1's :transfer (docs/operational-models.md's allocation ladder).
  Emission to HL7v2 is a separate namespace consuming the ground-truth
  log -- events here are format-free."
  (:require [ehrt.sim-model.interface :as sim-model]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.order-profiles :as order-profiles]
            [ehrt.sim-engine.person-fold :as person-fold]
            [ehrt.kernel.interface :as result]
            [malli.core :as m]
            [malli.util :as mu])
  (:import [java.util Random]))

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
   [:end-t {:optional true} :int]])

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
   [:interpretation {:optional true} [:enum :normal :low :high]]])

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
   [:ended-t {:optional true} :int]])

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
   [:ended-t {:optional true} :int]])

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
  standalone `:payer` field docs/operational-models.md described as an
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
   [:location {:optional true} [:maybe [:map
                                         [:ward :string]
                                         [:bed :string]
                                         [:placement [:enum :licensed :surge]]]]]
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

(defn- rand-int-in
  "Uniform integer in [lo, hi] from the seeded RNG."
  [^Random rng lo hi]
  (+ lo (.nextInt rng (inc (- hi lo)))))

(defn mix64
  "A fixed, fully-specified 64-bit mix of two longs (splitmix64-style
  constants) -- deliberately NOT an RNG draw. See `patient-id-for`.

  PUBLIC since ADR-0171 ruling A1 (\"reuse `engine.clj:225` `mix64` on
  `(family-tag, id-tag)`, unchanged, promoted from private to the
  sim-engine interface\"): the RNG stream partition derives every
  stream's seed with this same function on the same shape of key, so
  the partition adds no new numeric surface to specify or test. Its
  body is untouched by that promotion -- the constants are the ones
  `patient-id-for` has always used."
  ^long [^long a ^long b]
  (let [x (unchecked-add (unchecked-multiply a -7046029254386353131) b)
        x (unchecked-multiply (bit-xor x (unsigned-bit-shift-right x 30)) -4658895280553007687)
        x (unchecked-multiply (bit-xor x (unsigned-bit-shift-right x 27)) -7723592293110705685)]
    (bit-xor x (unsigned-bit-shift-right x 31))))

(defn patient-id-for
  "The internal, deterministic patient-id (sim/ADR-0010): a PURE function
  of this run's seed and the patient's arrival ordinal (0-indexed) --
  never reassigned, never re-derived elsewhere, and deliberately OFF
  the seeded RNG stream (identity needs no stochastic behavior, only
  spread across seeds -- keeping it off the RNG means identity
  generation adds no new draws for sim/ADR-0009's accounting to track).
  Distinct format from :mrn (\"PID-\" prefix, never \"MRN\") so the two
  id spaces are never visually confusable; the zero-padded ordinal
  leads so patient-id's lexical order matches arrival order exactly
  the way :mrn's already did -- load-bearing for the bed-ready
  tiebreak (docs/patient-state-model.md), which sorts on
  [:admitted-at patient-id]."
  [seed ordinal]
  (format "PID-%06d-%08x" ordinal (bit-and (mix64 seed ordinal) 0xffffffff)))

(def stream-scheme
  "The RNG stream partition's own version marker (ADR-0171 ruling D1),
  stamped top-level into every sim manifest as `:stream-scheme`,
  sibling of `:event-schema-version`.

  It is a DISCRIMINATOR, not a warranty. sim/ADR-0009 decision 1 states
  seed stability as a WITHIN-version guarantee and decision 2 names
  `:generator {:version ...}` as the cross-version key; this marker adds
  nothing to either. What it buys is legibility: two corpora with the
  same seed, config and generator version cannot differ, while two with
  the same seed and config and DIFFERENT stream schemes are expected to,
  and the marker says so on the artifact's face instead of making a
  reader resolve a generator version against a changelog.

  \"1.0\" is the partition itself -- the first scheme there has ever been.
  Everything generated before it carries no `:stream-scheme` key at all,
  which is exactly how a pre-migration corpus is told apart from a
  post-migration one."
  "1.0")

(def ^:private stream-family-tag
  "Family -> its fixed tag long (ADR-0171 section 2(b)). A compile-time
  constant table, deliberately NOT `(hash keyword)`: a hash this repo
  does not own would put the derivation's stability in someone else's
  hands, against `rulings.md#R-no-derivation-through-nondeterminism`'s
  spirit and against `gmf.clj`'s own hash-order caution.

  The five families are the census's five scopes (ADR-0171 section 1):

  * `:patient`  -- this patient's own clinical trajectory. Keyed by
                   arrival ordinal, the same key `patient-id-for` uses.
  * `:person`   -- arc 2's demographic/life-arc layer. ZERO draw sites
                   today; declared now so arc 2 adds rows rather than a
                   family, and so `newborn-id-tag` below has a family to
                   name.
  * `:world`    -- arrivals, and every cross-patient decision: all four
                   `allocate` calls, `bed-ready-location`, the bed-swap
                   and merge partner picks. Run-scoped (id-tag 0), because
                   their DRAW COUNTS are conditional on the population and
                   no per-patient stream can own them without making one
                   patient's consumption depend on another's state.
  * `:facility` -- `materialize-providers`, `choose-attending`, and
                   `:outpatient-visit`'s uniform provider pick: draws that
                   read no patient state at all. Run-scoped, and distinct
                   from `:world` (ruling E1) so adding a ward or a provider
                   template does not shift arrival gaps or bed choices.
  * `:emission` -- rendering-time latency planning (`ehrt.sim.run`), which
                   never enters ground truth. Ruling C1: it used to be
                   `(java.util.Random. seed)`, the master seed VERBATIM, so
                   the latency stream replayed the engine's own first draws."
  {:patient  1
   :person   2
   :world    3
   :facility 4
   :emission 5})

(defn stream-seed
  "The seed of one stream: `(mix64 (mix64 master family-tag) id-tag)`
  (ADR-0171 section 2(b), ruling A1). `id-tag` is the patient's arrival
  ordinal for `:patient`, and 0 for the run-scoped families.

  Collisions are cosmetic at this project's scale and are not engineered
  around: two patients sharing a stream seed share a draw sequence, which
  is a DUPLICATE trajectory, not a corrupt one, and at 10^6 ids over a
  64-bit mixed space the expected number of colliding pairs is ~2.7e-8.

  Order-free by construction, which is the whole point: a stream is keyed
  by a STABLE id, never by how many streams were built before it (the
  reason ADR-0171 rejected `SplittableRandom`'s split order)."
  ^long [^long master family ^long id-tag]
  (let [tag (get stream-family-tag family)]
    (when (nil? tag)
      (throw (ex-info "unknown RNG stream family"
                      {:family family :known (set (keys stream-family-tag))})))
    (mix64 (mix64 master (long tag)) id-tag)))

(defn stream
  "A fresh `java.util.Random` for one stream -- `stream-seed`'s value,
  handed to the one constructor the engine has ever used."
  ;; Deliberately UNHINTED, unlike `stream-seed` above: primitive-long
  ;; parameter hints compile callers to an IFn$LOLO call site, which a
  ;; plain `with-redefs` replacement cannot satisfy -- and the locality
  ;; test's whole mechanism is redefining this var. `run` calls it a
  ;; handful of times per run (twice, plus once per patient), so there
  ;; is no arithmetic here worth hinting.
  [master family id-tag]
  (Random. (stream-seed (long master) family (long id-tag))))

(defn newborn-id-tag
  "The `:person`-family id-tag for a newborn (ADR-0171 section 2(c),
  ruling B1): a birth's stream is derived from the PARENT's stable id and
  a birth ordinal, never from a global counter, so a birth occurring
  anywhere in the run perturbs no other person's stream.

  The ordinal is the PAIR `(parity-index, within-delivery-index)`, mixed
  in that order, with `within-delivery-index` pinned at 0 for as long as
  multiples are a named v1 limitation. Ruling B1 took the pair from the
  start deliberately: admitting twins later would otherwise have to widen
  a bare parity index, renumbering every existing singleton's stream and
  costing a full newborn-stream reshuffle.

  NO CALLER TODAY. Arc 2 owns the newborn path; this function exists now
  so arc 2 inherits the key rather than choosing it, and its only gate is
  `engine-test/the-stream-partition-derives-what-adr-0171-specifies`."
  ^long [^long parent-id-tag ^long parity-index ^long within-delivery-index]
  (mix64 (mix64 parent-id-tag parity-index) within-delivery-index))

(defn one-stream
  "Every family bound to ONE `Random` -- the degenerate stream map a
  caller with no `run` behind it needs (a single `decide` call in a
  test). Collapsing the families is EXACTLY the pre-partition behaviour,
  so a lone `decide` call's draw order is unchanged by ADR-0171; what
  moved is which stream `run` hands each family, and `run` builds that
  map itself."
  [^Random rng]
  {:patient rng :person rng :world rng :facility rng :emission rng})

(defn events-for-patient
  "Every event `patient-id` participates in, in log order -- the
  patient-phrased replacement for what a single :mrn-keyed lookup used
  to mean before sim/ADR-0010's :participants existed. An event with more
  than one participant (M2b's bed-swap, merge) appears in every
  participant's own sequence, not just one."
  [ground-truth patient-id]
  (filterv (fn [event] (some #(= patient-id (:patient-id %)) (:participants event)))
           ground-truth))

(defmulti decide
  "Decides what happens when patient `patient-id` is due to execute
  `step` at simulated time t (SECONDS from the run's epoch, sim/ADR-0011).
  Consults `world` ({:patients {patient-id -> patient-state} :facility
  .. :providers ..} -- read-only) and the seeded RNGs to make stochastic
  and cross-patient choices; returns {:events [<ground-truth
  event>...] :advance <seconds>}. NEVER returns or implies a new
  patient state -- state changes only by folding the returned events
  through `evolve` (sim/ADR-0008). Pure given the RNGs (they are the only
  stateful arguments, and their consumption order is fixed by the
  deterministic event ordering).

  ADR-0171: the first argument is a STREAM MAP, not one `Random` --
  `{:patient <this patient's stream> :world <the run's> :facility <the
  run's>}` -- and each method draws from the family its census row
  names (`stream-family-tag` above). `run` builds the real, partitioned
  map; a caller with no run behind it wraps its own `Random` in
  `one-stream`, which collapses the families back to the pre-partition
  single stream."
  (fn [_streams _t _world _patient-id step] (:type step)))

(defn- exhausted-outcome
  "Task 0: result-not-throw for allocation-ladder exhaustion --
  sim-model/allocate no longer throws, so decide translates its
  structured {:exhausted true} into a decide-level outcome the run loop
  halts on and run-command (ehrt.sim.run) surfaces as :error
  :capacity-exhausted, payload {:patient-id :ward :census}."
  [patient-id home-ward-name facility board]
  {:events [] :advance 0
   :exhausted {:patient-id patient-id :ward home-ward-name
               :census (sim-model/ward-census facility board)}})

;; --- M4: Persona (docs/sim-theory.edn's :persona stage) -------------------
;; :registered is engine-internal, never authorable pathway IR -- the same
;; treatment :result-followup already gets (pathway.clj's own docstring):
;; `run` prepends it to every patient's step queue itself, so no
;; sim-model/Step schema entry exists for it and it never
;; passes through sim-model/valid?. Its decide call is the ACTUAL Persona
;; stage boundary; folding it into Execute's own step-queue mechanism
;; rather than a separate pipeline stage is this milestone's own documented
;; theory-flip note (docs/sim-theory.edn, docs/sim-theory.md) -- the
;; stage's contract ("samples once, from the run's seeded RNG, in
;; fixed order" -- ADR-0171: from THIS patient's :patient-family stream) is satisfied by this event exactly, not merely gestured at.

;; M5b Task 4: persona -> run-module -> CompileTrajectory -> IR, the ACTUAL
;; RunModules/CompileTrajectory stage boundary, folded into THIS SAME
;; engine-internal step for the same reason Persona itself was (M4's own
;; documented theory-flip note): a patient's assigned module is consumed at
;; the same init moment this event already owns. `step`'s own :closure (set,
;; per patient, by `run`'s eager `registered-steps-for` -- mirroring how
;; :pathways' own per-patient resolution already happens eagerly, ahead of
;; the main loop) is nil for the (default, opt-in) case of no module
;; assignment -- byte-identical to pre-M5b :registered output, no new draw,
;; the same "absent means untouched" law :pathways/:churn-profile already
;; establish. `registration-t` is `sim-model/reference-today-epoch-day` --
;; components/patient-simulator/docs/gmf-interpreter.md section 3's own "that patient's own :registered
;; event time," expressed in the SAME calendar anchor every persona's own
;; DOB is already computed against (persona.clj's own docstring note).
;; `:module-horizon-days` bounds the walk (`run-module`'s own optional
;; `horizon-end-t`) -- REQUIRED for any real vendored module (M5b's own
;; finding, components/patient-simulator/docs/gmf-interpreter.md section 8 item 5: a module with no
;; Terminal state and no Guard to block on would otherwise run until the
;; interpreter's own max-steps backstop throws).

;; ADR-0033 AR-2/AR-3 (2026-08-03, J3 closed): `:closure` -- ALWAYS
;; closure-shaped when present (`ehrt.patient-simulator.gmf/load-closure`'s
;; own :ok payload, `{:root :modules :tables}`, plus an optional
;; :initial-attributes an authoring-time config may attach, AR-1) --
;; replaces the pre-ADR-0033 bare :module. `run-module` is now called at
;; its FULL 7-arity, threading the closure's own `:modules` (submodule
;; registry) and `:tables` (lookup-table members) straight through to the
;; interpreter -- the previous bare 5-arity call defaulted `modules` to
;; `{root root-module}` (the root alone) and `tables`/`initial-attributes`
;; to `{}`, which is EXACTLY the singleton-closure/no-seed case: this
;; change is draw-neutral and byte-neutral for every pre-ADR-0033 run
;; (AR-4), and only NEWLY reaches a closure's own called submodules/
;; tables/seed for a root that actually has them.

(defn compile-patient
  "One patient's Persona and compiled module trajectory, drawn from that
  patient's OWN `:patient` stream. Returns `{:persona p :compiled c}`;
  `:compiled` is nil for a patient with no assigned closure.

  ARRIVAL-TIME INDEPENDENT, and that is the whole reason this is a
  function rather than a `let` inside `decide :registered` (ADR-0173
  ruling C1: `ehrt.sim.run` must be able to obtain every patient's
  compiled death instant BEFORE the run, because the person component's
  own `persons` front door takes the whole population at once -- named
  in prose rather than by namespace, because ADR-0172 limitations row
  10's reverse-edge half is a bare token scan over this component's
  src). There is no `t` parameter here because nothing below could read
  one. Every input, enumerated:

  | input | why it cannot differ between run start and arrival |
  |---|---|
  | `rng` | ONE stream per patient (`run`'s `patient-rngs`), and exactly three `decide` methods draw from the `:patient` family -- `:registered`, `:delay`, `:order` -- all three on the ACTING patient's own stream. `:delay` and `:order` are steps that follow `:registered` in that patient's own queue, so at arrival the stream stands exactly where the pre-loop draws left it. See `run`'s docstring for the pinned pre-loop order. |
  | `(:persona-config world)` | set once in `init-world`; the run loop only ever `assoc`s `:ground-truth`/`:reinstate-index`/`:citation-index` and `update-in`s `[:patients pid]` |
  | `closure` | resolved pre-loop by `run`'s own `module-for` and carried on the `:registered` STEP -- immutable queue data |
  | `(:modules closure)` / `(:root closure)` / `(:initial-attributes closure)` / `(:tables closure)` | pure data inside that closure |
  | `reg-t` | `sim-model/reference-today-epoch-day` is `(LocalDate/of (inc reference-birth-year) 1 1)` -- a FIXED calendar anchor computed from a constant, not a clock and not the arrival instant |
  | `horizon-end-t` | `reg-t` plus `(:module-horizon-days world)`, run config |
  | `(:facility world)` | run config; never re-`assoc`ed by the loop (bed occupancy lives in `[:patients pid :location]`, not here) |
  | `history?` | `(:history world)`, run config |

  So the ONLY thing that moves when the call moves is WHEN it happens in
  wall-clock terms; the stream position it reads from is unchanged, which
  is what makes the move byte-identical. `the-registered-compile-is-
  arrival-time-independent` (engine-test) and `every-gated-run-compiles-
  the-same-persona-at-any-arrival-time` (ehrt.sim.run-test) are the
  gates, and `bin/regression-oracle` is the proof.

  `world` here is any map carrying the four config keys above -- the live
  `world` at `decide` time, or the equivalent map `run` builds before the
  loop exists.

  THE 4-ARITY (ADR-0173 section 1, arc 3a part 3) is the seam the
  `:patient` family loses. `supplied-persona`, when non-nil, IS this
  patient's Persona and NO `sim-model/persona` draw is made -- the
  arrival was bound to a person, and that person's Persona was drawn
  from the `:person` family instead. Thirteen draws (sixteen with
  demographic weights) leave the `:patient` stream, and every
  `:patient` draw that FOLLOWS the seam -- the module walk here,
  `decide :delay`, `decide :order` -- shifts by that much. That is the
  whole of arc 3a's predicted blast radius, and it is why `:persons`
  ABSENT ENTIRELY has to stay the byte-identical path: nil in, and this
  is the 3-arity verbatim."
  ([rng world closure] (compile-patient rng world closure nil))
  ([rng world closure supplied-persona]
   (let [persona (or supplied-persona (sim-model/persona rng (:persona-config world)))
         history? (boolean (:history world))
         compiled (when closure
                    (let [root-module (get (:modules closure) (:root closure))
                          reg-t (sim-model/reference-today-epoch-day)
                          horizon-end-t (+ reg-t (:module-horizon-days world))
                          {:keys [trajectory]} (patient-simulator/run-module
                                                 root-module rng persona reg-t horizon-end-t
                                                 (:modules closure)
                                                 (or (:initial-attributes closure) {})
                                                 (or (:tables closure) {})
                                                 history?)]
                      ;; ADR-0042 AR-1/AR-3: `history?` threads straight
                      ;; through to compile-trajectory's own new 4-arity --
                      ;; false stays the plain legacy path (byte-identical
                      ;; to every pre-H run, since that arity's own body is
                      ;; nothing but a call to the unchanged 3-arg one).
                      (patient-simulator/compile-trajectory trajectory (:facility world) reg-t history?)))]
     {:persona persona :compiled compiled})))

(defmethod decide :registered
  [{rng :patient} t world patient-id {:keys [closure]}]
  ;; :active-mrn is REQUIRED here, not merely conventional: :registered
  ;; is now every patient's FIRST event, and `replay` (below) bootstraps
  ;; a never-yet-seen participant's initial state via `(initial-patient
  ;; pid (:active-mrn event))` off the FIRST event naming them -- every
  ;; other event type already carries :active-mrn for exactly this
  ;; reason (a convention this event must honor, not just a rendering
  ;; nicety), or `replay`'s own bootstrap (and every check.clj invariant
  ;; built on it) silently seeds `:mrns #{nil}`.
  ;;
  ;; ADR-0173 C1: the persona draw and the module walk now happen at RUN
  ;; START, not here -- `run` calls `compile-patient` for every arrival
  ;; ordinal, in ordinal order, immediately after the pre-loop
  ;; `:patient`-family draws, and carries the result in
  ;; `:compiled-patients`. This method ATTACHES what was pre-compiled.
  ;; FALLS BACK to compiling in place when `world` carries no
  ;; `:compiled-patients` KEY -- a hand-built world, as most of
  ;; engine-test uses. Same fallback rule as `reinstated-state` and
  ;; `last-cited-index`, and for the same reason: on the KEY, never on a
  ;; missing entry, so a carrier `run` built but failed to populate shows
  ;; up as a changed corpus rather than as a silent recompile.
  ;; ADR-0173 section 2(b) (arc 3a part 3): `:residence` rides the event
  ;; ONLY for an arrival bound to a person who is not housed at their own
  ;; registration instant -- a nil-dropping `cond->`, the same shape
  ;; `citation-fields` and `reason-field` already use, so a run with no
  ;; `:persons` key emits the identical bytes it always has. It is a SUM
  ;; and not a nilable address because `sim-model/Persona`'s own
  ;; `:address` is required and non-nilable: the Persona keeps the row
  ;; last lived at, and this says whether anybody lives there (ruling E1
  ;; -- the wire renders PID-11 absent, ground truth keeps the
  ;; distinction between `:unhoused` and `:unknown`).
  ;;
  ;; ARC 3A PART 4 adds four optional keys, all of them ABSENT for every
  ;; run with no `:persons`, so this method's bytes are unchanged there.
  ;; `:person-id` is the provenance stamp
  ;; `identification-merge-survivor-is-the-persons-prior-patient` reads
  ;; on BOTH sides of an identification merge. The other three ride only
  ;; a PLACEHOLDER registration (ADR-0173 section 2(d)): the arrival
  ;; landed inside an open `:identity-unavailable` window, so the wire
  ;; gets the window's alias and an `:unknown` residence, and
  ;; `:window-close-t` is what lets
  ;; `every-placeholder-registration-is-resolved-or-still-open` tell a
  ;; dangling placeholder from one the horizon simply ended inside.
  ;;
  ;; `:persona` RIDES A PLACEHOLDER REGISTRATION UNCHANGED, and that is
  ;; deliberate. Ground truth knows who this patient is -- an
  ;; unidentified arrival is still somebody -- so the record stays
  ;; truthful and `registered-persona-is-schema-valid` keeps asserting
  ;; exactly what it asserts today. What `:identity :placeholder` buys
  ;; is that the FOLD (and therefore every message) renders the alias
  ;; instead: `evolve :registered` seeds `placeholder-demographics`
  ;; rather than `demographics-from-persona`.
  (let [{:keys [persona compiled residence person-id identity alias-name window-close-t
                mother-patient-id]}
        (if (contains? world :compiled-patients)
          (get (:compiled-patients world) patient-id)
          (compile-patient rng world closure))
        placeholder? (= :placeholder identity)]
    {:events [(cond-> {:event :registered :t t
                       :active-mrn (get-in world [:patients patient-id :active-mrn])
                       :persona persona
                       :participants [{:patient-id patient-id :role :subject}]}
                (seq (:registration-facts compiled)) (assoc :pre-horizon-facts (:registration-facts compiled))
                (and residence (not= :housed (:status residence))) (assoc :residence residence)
                person-id (assoc :person-id person-id)
                mother-patient-id (assoc :mother-patient-id mother-patient-id)
                placeholder? (assoc :identity :placeholder
                                    :alias-name alias-name
                                    :window-close-t window-close-t
                                    :residence {:status :unknown}))]
     :advance 0
     :prepend-steps (:steps compiled)}))

(defn- citation-fields
  "M5b: :citation/:conditions ride through onto the ground-truth event
  ONLY when the compiled step actually carries them (glass-box
  traceability, components/patient-simulator/docs/gmf-interpreter.md section 6 obligations 1/3) --
  `select-keys` + a nil-dropping `into {}` keeps a hand-authored step
  (never compiled, carries neither key) producing the EXACT same event
  shape it always has, byte-identical, no perturbation for any pathway
  that predates M5b."
  [step]
  (into {} (filter val) (select-keys step [:citation :conditions])))

(defn- reason-field
  "S-1 (ADR-0151): `:reason` rides onto the ground-truth event ONLY
  when the step actually carries one, the same nil-dropping shape
  `citation-fields` uses -- and a SIBLING of it rather than a widening
  of it, deliberately. `citation-fields` scopes itself to glass-box
  TRACEABILITY of what the compiler supplied
  (components/patient-simulator/docs/gmf-interpreter.md section 6
  obligations 1/3); `:reason` is clinical content a HAND-AUTHORED step
  supplies, and `compile_trajectory`'s own `encounter->step` never sets
  one. Same shape, different reason to exist, so two functions.

  Before this, both encounter decides merged `:reason` unconditionally,
  so every module-compiled encounter emitted `:reason nil` -- present
  and empty, which is the one shape that tells a consumer nothing
  (census S-1: `:outpatient-visit` 221/221, `:admission` 48/692, those
  48 exactly the 48 carrying a citation). Dropping the key made
  `:reason` `{:optional true}` on both kinds, which `classify-change`
  calls breaking, which is why this is the whole of what the event
  contract's 1.1.0 -> 1.2.0 bump buys."
  [step]
  (into {} (filter val) (select-keys step [:reason])))

(defn- person-stamp-field
  "Arc 3a part 4: `:person-event-id` rides onto an encounter event ONLY
  when the step that produced it came from a person-stream HOOK -- the
  same nil-dropping shape `reason-field` and `citation-fields` use, and
  a third sibling rather than a widening of either, for the same reason
  they are two: this one scopes itself to PERSON provenance, which is
  neither glass-box compiler traceability nor authored clinical content.

  It is a STAMP and never a log index -- `check.clj`'s
  `person-scoped-provenance-is-a-stamp-not-a-reference` is the gate --
  and it is what makes a hook-created encounter COUNTABLE in a corpus
  without joining it back to the person stream by guesswork."
  [step]
  (into {} (filter val) (select-keys step [:person-event-id])))

;; --- arc 3a part 3: the two kinds the person stream mints ----------------
;;
;; Both steps are QUEUE-SEEDED at their own absolute `:t` by `run` (the
;; queue is already a `sorted-map` keyed `[t seq-no]`, and
;; `schedule-followup` already inserts at an absolute instant, so the
;; main loop does not change at all). Both are engine-internal, never
;; authorable pathway IR -- the same treatment `:registered` and
;; `:result-followup` get, so neither has a `sim-model/Step` entry and
;; neither passes `sim-model/valid?`.
;;
;; THE PRIOR VALUE IS READ OFF THE PATIENT, NOT OFF THE PERSON EVENT.
;; The person event carries its own `:prior-address`/`:prior-value`, and
;; using it would make section 2(e) invariant 4 (`demographic-update-
;; reports-a-real-change`) a tautology over a field this method copied.
;; Reading `world` instead makes the wire's own claim true by
;; construction, and leaves the invariant guarding the day a future
;; decide stops doing it -- which is what an invariant is for.
;;
;; AN EVENT THAT REPORTS NO CHANGE IS NOT AN EVENT (`b4f1115`, promoted
;; from the person side to the wire side): when the value is already the
;; folded state's, nothing is emitted. The step is consumed either way,
;; and no RNG is touched by either method, so nothing shifts.

(defn- demographic-target
  "The patient a queue-seeded person step may write to, or nil when it
  may not write at all. `:expired` is the one status that refuses:
  section 2(e) invariant 5 forbids a demographic event after a patient
  expires, and the run loop's own `:merged` short-circuit already ends a
  merged patient's stream before any step of theirs is decided."
  [world patient-id]
  (let [patient (get-in world [:patients patient-id])]
    (when (and patient
               (some? (:demographics patient))
               (not= :expired (:status patient)))
      patient)))

(defmethod decide :demographic-update
  [_streams t world patient-id {:keys [cause field value person-event-id]}]
  (let [patient (demographic-target world patient-id)
        prior (get (:demographics patient) field)]
    (if (or (nil? patient) (= prior value))
      {:events [] :advance 0}
      {:events [{:event :demographic-update :t t
                 :active-mrn (:active-mrn patient)
                 :cause cause
                 :field field
                 :value value
                 :prior-value prior
                 :person-event-id person-event-id
                 :participants [{:patient-id patient-id :role :subject}]}]
       :advance 0})))

(defmethod decide :coverage-change
  [_streams t world patient-id {:keys [cause payer person-event-id]}]
  (let [patient (demographic-target world patient-id)
        prior (:payer (:demographics patient))]
    (if (or (nil? patient) (= prior payer))
      {:events [] :advance 0}
      {:events [{:event :coverage-change :t t
                 :active-mrn (:active-mrn patient)
                 :cause cause
                 :payer payer
                 :prior-payer prior
                 :person-event-id person-event-id
                 :participants [{:patient-id patient-id :role :subject}]}]
       :advance 0})))

;; --- arc 3a part 4: the two clinical hooks and the identification flow ----
;;
;; ADR-0173 sections 2(c) and 2(d). THREE new step types, all
;; engine-internal and all QUEUE-SEEDED at an absolute `:t` by `run`,
;; exactly like part 3's two. None of them adds an event KIND: a hook
;; produces the ordinary `:admission`/`:delay`/`:discharge` triple, a
;; fill produces a `:demographic-update`, and an identification merge
;; produces a `:merge` in churn's own shape. The vocabulary the fold
;; grew is still exactly two.
;;
;; NONE OF THE THREE DRAWS. `:person-encounter` prepends steps and
;; emits nothing itself; the fill and the merge read `world` and emit.
;; So the hooks change WHICH patients exist and what happens to them,
;; and change no stream's consumption for any patient that would have
;; existed anyway.

(def delivery-stay-minutes
  "How long a birth encounter lasts, in MINUTES -- two days, the
  ordinary post-partum stay. A CONSTANT and not a range, deliberately:
  `decide :delay` skips the draw entirely when `:from` = `:to`
  (ADR-0171 section 2(d)), so a hook-created encounter costs no
  `:patient`-family draw and cannot shift a stream that would have
  existed without it.

  BOUNDED AT ALL is the load-bearing part. ADR-0173 section 2(c) says
  `:delivery` mints an admission and stops there; an admission with no
  discharge holds a licensed bed for the REST OF THE RUN, and with
  `:persons` present a run's horizon is the person process's own -- ten
  years by default, not the hours a clinical pathway spans. One
  unclosed birth per delivery would exhaust any facility this repo
  ships. So the hook mints an ENCOUNTER, which is what a birth is."
  2880)

(def injury-stay-minutes
  "How long an occupational-injury ED encounter lasts, in MINUTES --
  four hours. Same constant-not-a-range reasoning as
  `delivery-stay-minutes` above, and the same bounded-encounter one."
  240)

(def unidentified-stay-minutes
  "How long an UNIDENTIFIED ED presentation lasts, in MINUTES -- twelve
  hours, longer than an ordinary injury visit because nobody can
  discharge a patient they cannot name. Same constant-not-a-range and
  bounded-encounter reasoning as the two above."
  720)

(defn- hook-ward
  "Which ward a hook-created encounter admits to, by CLASS rather than
  by name: an occupational injury is an ED presentation, a birth is an
  inpatient one. Read off this run's own facility, so a config that
  renames its wards -- `demos/scenarios/ed-tuesday` does -- still gets
  a real one, and a facility carrying neither class falls back to its
  first ward rather than to a literal no facility need contain."
  [facility want]
  (let [named (fn [c] (:name (first (filter #(= c (:class %)) (:wards facility)))))]
    (or (named want) (named :inpatient) (named :ed) (:name (first (:wards facility))))))

(defmethod decide :person-encounter
  ;; ADR-0173 section 2(c). The step carries WHAT the encounter is; this
  ;; method decides WHETHER it may happen at all, and prepends the
  ;; ordinary three-step encounter when it may.
  ;;
  ;; THE `:new` GUARD IS THIS PROJECT'S SINGLE-ENCOUNTER HORIZON, met a
  ;; second time. `check.clj`'s `admission-only-when-new` (sim/ADR-0007
  ;; point 3) means a patient gets ONE inpatient encounter, ever --
  ;; `evolve :discharge` leaves them `:discharged`, never back at
  ;; `:new`. So a hook landing on a patient who has already had their
  ;; encounter mints nothing, exactly as ADR-0173's own first tabled
  ;; deviation says a repeat arrival queues nothing. `run` also refuses
  ;; these statically, before the run, for a patient whose own queue
  ;; contains an encounter at all (`prelude`'s `encounter-free?`); this
  ;; guard is the runtime half, and the two are deliberately both
  ;; present -- a static analysis that turns out to be wrong shows up
  ;; here as a skipped encounter rather than as a red invariant.
  ;;
  ;; THE WHOLE TRIPLE IS PREPENDED OR NONE OF IT IS. A `:delay` and a
  ;; `:discharge` queued behind an admission that did not happen would
  ;; be a discharge with no admission, which
  ;; `discharge-follows-admission` correctly calls a defect -- so the
  ;; encounter is one decision, not three steps that each guard
  ;; themselves.
  [_streams _t world patient-id {:keys [reason ward-class stay-minutes person-event-id]}]
  (let [patient (get-in world [:patients patient-id])]
    (if (not= :new (:status patient))
      {:events [] :advance 0}
      {:events [] :advance 0
       :prepend-steps [{:type :admission
                        :location (hook-ward (:facility world) ward-class)
                        :reason reason
                        :person-event-id person-event-id}
                       {:type :delay :from stay-minutes :to stay-minutes}
                       {:type :discharge}]})))

(defmethod decide :identity-fill
  ;; ADR-0173 section 2(d), the `:fill` branch of `:identity-resolution`:
  ;; the placeholder patient KEEPS their patient-id and their MRN, and
  ;; every demographic field is filled in from the person's real
  ;; demographics at this instant.
  ;;
  ;; ONE EVENT, NOT SEVEN. A fill is not six independent field changes
  ;; that happen to coincide; it is one fact -- this record now belongs
  ;; to a known person -- so `:field` is `:identity` and `:value` is
  ;; `:known`, with the demographics themselves riding as the `:persona`
  ;; the record should have had all along. `evolve` below rebuilds the
  ;; whole state from it, and `demographic-update-reports-a-real-change`
  ;; still has something true to check: `:prior-value` is `:placeholder`,
  ;; which is exactly what the fold says it was.
  ;;
  ;; `:placeholder-event-id` IS A LOG INDEX -- the one referential key
  ;; this arc mints -- and it comes from `run`'s fold-carried
  ;; `:registration-index` rather than from a scan, the same shape
  ;; ADR-0169 gave `:citation-index`. A hand-built world carrying no
  ;; such KEY answers nil, which
  ;; `identity-fill-references-its-placeholder-registration` reports as
  ;; a dangling reference -- correctly, because it would be one.
  [_streams t world patient-id {:keys [persona residence person-event-id]}]
  (let [patient (demographic-target world patient-id)]
    (if (or (nil? patient) (not= :placeholder (:identity (:demographics patient))))
      {:events [] :advance 0}
      {:events [(cond-> {:event :demographic-update :t t
                         :active-mrn (:active-mrn patient)
                         :cause :identity-fill
                         :field :identity
                         :value :known
                         :prior-value :placeholder
                         :placeholder-event-id (get-in world [:registration-index patient-id])
                         :persona persona
                         :person-event-id person-event-id
                         :participants [{:patient-id patient-id :role :subject}]}
                  (and residence (not= :housed (:status residence)))
                  (assoc :residence residence))]
       :advance 0})))

(defmethod decide :identification-merge
  ;; ADR-0173 section 2(d), the `:merge` branch. The event is churn's own
  ;; `:merge` -- same kind, same `:survivor`/`:merged` roles, same
  ;; `:surviving-mrn`/`:merged-mrn`/`:merged-mrns` payload -- so
  ;; `merge-survivor-absorbs-merged-mrns`, `no-events-after-merged-
  ;; terminal`, the run loop's own `:merged` short-circuit and the whole
  ;; post-merge shadow surface apply verbatim. `:cause :identification`
  ;; is the ONLY thing that distinguishes it, and
  ;; `identification-merge-survivor-is-the-persons-prior-patient` is
  ;; what makes that marker mean something.
  ;;
  ;; A SEPARATE DECIDE, AND NOTHING ADDED TO CHURN'S LOTTERY. `decide
  ;; :merge`'s own `never-mergeable?` excludes `:new`, and a placeholder
  ;; patient who registered and was never admitted is exactly `:new`;
  ;; relaxing that would move every churn corpus for an unrelated
  ;; reason, which this arc has no licence to do. So this is one more
  ;; decide method with its own guard, and `churn/inject`'s step-type
  ;; set and roll order are untouched.
  ;;
  ;; The step is queued on the SURVIVOR, so `patient-id` here is the
  ;; person's prior patient and `:placeholder-patient-id` is the record
  ;; being absorbed.
  [_streams t world patient-id {:keys [placeholder-patient-id person-event-id]}]
  (let [{:keys [patients]} world
        survivor (get patients patient-id)
        merged (get patients placeholder-patient-id)]
    (if (or (nil? survivor) (nil? merged)
            (= patient-id placeholder-patient-id)
            (#{:merged :expired} (:status survivor))
            (#{:merged :expired} (:status merged))
            (not= :placeholder (:identity (:demographics merged))))
      {:events [] :advance 0}
      {:events [{:event :merge :t t
                 :cause :identification
                 :person-event-id person-event-id
                 :participants [{:patient-id patient-id :role :survivor}
                                {:patient-id placeholder-patient-id :role :merged}]
                 :surviving-mrn (:active-mrn survivor)
                 :merged-mrn (:active-mrn merged)
                 :merged-mrns (:mrns merged)}]
       :advance 0})))

(defmethod decide :admission
  [{world-rng :world facility-rng :facility} t world patient-id
   {:keys [location force-placement] :as step}]
  ;; ADR-0171: the bed choice is WORLD (its candidate set is `free`
  ;; against a board built from EVERY patient), the attending is
  ;; FACILITY (ward-eligible providers, no patient state read) -- ruling
  ;; E1's split is by what the draw READS, not by what it is named after.
  (let [{:keys [facility providers patients]} world
        board (sim-model/occupancy-board patients)
        alloc (sim-model/allocate world-rng facility board location force-placement)]
    (if (:exhausted alloc)
      (exhausted-outcome patient-id location facility board)
      (let [ward-id (:id (sim-model/ward-by-name facility (:home-ward alloc)))
            attending (sim-model/choose-attending facility-rng providers ward-id)
            active-mrn (get-in patients [patient-id :active-mrn])]
        {:events [(merge {:event :admission :t t :active-mrn active-mrn :attending attending
                          :participants [{:patient-id patient-id :role :subject}]}
                         alloc (reason-field step) (citation-fields step)
                         (person-stamp-field step))]
         :advance 0}))))

(defmethod decide :delay
  [{rng :patient} _t _world _patient-id {:keys [from to]}]
  ;; :from/:to are authored in MINUTES (pathway.clj IR, unchanged --
  ;; sim/ADR-0011 decision 1's authoring-ergonomics carve-out); the engine
  ;; converts to SECONDS here, the one place a minute-denominated draw
  ;; becomes a clock advance.
  ;;
  ;; ADR-0171 section 2(d): when :from = :to the draw is ARITHMETICALLY
  ;; DEAD -- `rand-int-in` evaluates `(.nextInt rng 1)`, which is always
  ;; 0 -- so it is skipped, and the step advances by the authored
  ;; constant. Free in outcome, costly in stream position, hence
  ;; draw-affecting, hence landed in the partition's own commit and
  ;; never before or after it (one reshuffle, ruling F1).
  ;;
  ;; This does NOT breach the fixed-consumption law `assign-pathway` and
  ;; `churn/roll-gap` state. That law exists so draw count never depends
  ;; on DATA; :from = :to is not data but the authored SHAPE of a step,
  ;; as visible as the step itself, and under a per-patient stream it
  ;; cannot reach any other patient.
  {:events []
   :advance (* 60 (if (= from to) from (rand-int-in rng from to)))})

(defmethod decide :transfer
  [{world-rng :world} t world patient-id {:keys [location force-placement]}]
  (let [{:keys [facility patients]} world
        board (sim-model/occupancy-board patients)
        patient (get patients patient-id)
        alloc (sim-model/allocate world-rng facility board location force-placement)]
    (if (:exhausted alloc)
      (exhausted-outcome patient-id location facility board)
      {:events [(merge {:event :transfer :t t :active-mrn (:active-mrn patient) :from (:location patient)
                        :attending (:attending patient) :bed-ready false
                        :participants [{:patient-id patient-id :role :subject}]}
                       alloc)]
       :advance 0})))

(defn- death-disposition-fields
  "Wave C (2026-08-02, ADR-0028, C3): :disposition/:codes ride onto the
  ground-truth :discharge event ONLY when the compiled step actually
  carries them (compile-trajectory.clj's own death->step, the two new
  optional fields sim-model/pathway.clj's :discharge schema gained) --
  the same nil-dropping merge `citation-fields` already establishes,
  applied to this step type's own two new fields."
  [step]
  (into {} (filter val) (select-keys step [:disposition :codes])))

(defn- bed-ready-location
  "Where a bed-ready transfer actually places `waiting-id`, once
  `patient-id`'s discharge has vacated `vacated-location`.

  Normally the just-vacated bed itself: that specific bed becoming ready
  IS the coupling (docs/operational-models.md's own \"patient B's
  discharge event is what makes patient A's boarding-to-transfer event
  schedulable\"). But the coupling names the bed WITHIN its rung -- it
  never licenses a rung the allocation ladder would not have reached.
  A vacated SURGE slot is rung 2, legal only \"once licensed beds are
  full\" (same document, ladder rung 2), and a licensed bed in the
  boarder's home ward -- which is this ward, since that is how
  `waiting-id` was chosen -- can be free at this instant: some OTHER
  coupling can vacate one with no boarder pulled into it (a bed-ready
  transfer's own origin bed triggers no second search), and under
  `--churn` a :cancel-admit or :cancel-transfer can vacate one outright.
  Handing over the surge slot then places on rung 2 with rung 1 free,
  which is exactly what `ehrt.sim-check.check/surge-only-when-earlier-
  rungs-exhausted` forbids (ADR-0153, seed 202 under `--churn` at
  `t 78480`). In that case the ladder decides, drawing its own seeded
  bed choice the way every other placement does.

  `allocate` can never come back `:exhausted` here: the vacated bed is
  in `waiting-id`'s own home ward, so rung 1 or rung 2 always has at
  least that one candidate -- and since rung 1 is free by the branch
  we are in, the result is always a licensed bed in that same ward."
  [world-rng world patient-id waiting-id vacated-location]
  (let [facility (:facility world)
        home-ward-name (get-in world [:patients waiting-id :home-ward])
        board (sim-model/occupancy-board (dissoc (:patients world) patient-id))
        home-ward (sim-model/ward-by-name facility home-ward-name)
        home-licensed-free? (boolean (seq (remove board (sim-model/licensed-bed-ids home-ward))))]
    (if (and (= :surge (:placement vacated-location)) home-licensed-free?)
      (:location (sim-model/allocate world-rng facility board home-ward-name nil))
      vacated-location)))

(defmethod decide :discharge
  [{world-rng :world} t world patient-id step]
  (let [patient (get-in world [:patients patient-id])
        ;; C3: an expired-disposition discharge vacates NO bed --
        ;; patient-state-model.md's own "clinically absorbing but
        ;; operationally alive" fact -- so the bed-ready-transfer
        ;; coupling below MUST NOT fire; unguarded, it would double-
        ;; occupy a bed no-double-occupancy already forbids.
        expired? (= :expired (:disposition step))
        discharge-event (merge {:event :discharge :t t :active-mrn (:active-mrn patient)
                                 :location (:location patient) :attending (:attending patient)
                                 :participants [{:patient-id patient-id :role :subject}]}
                                (citation-fields step)
                                (death-disposition-fields step))
        vacated-ward (get-in patient [:location :ward])
        vacated-location (:location patient)
        waiting-id (when-not expired?
                     (->> (:patients world)
                          (remove (fn [[pid _]] (= pid patient-id)))
                          (filter (fn [[_ p]] (and (= :admitted (:status p))
                                                    (not= (:home-ward p) (get-in p [:location :ward]))
                                                    (= vacated-ward (:home-ward p)))))
                          (sort-by (fn [[pid p]] [(:admitted-at p) pid]))
                          ffirst))]
    {:events (cond-> [discharge-event]
               waiting-id
               (conj (let [location (bed-ready-location world-rng world patient-id waiting-id vacated-location)]
                       {:event :transfer :t t
                        :active-mrn (:active-mrn (get-in world [:patients waiting-id]))
                        :from (:location (get-in world [:patients waiting-id]))
                        :attending (:attending (get-in world [:patients waiting-id]))
                        :home-ward (get-in world [:patients waiting-id :home-ward])
                        :location location
                        :placement (:placement location)
                        :forced false
                        :bed-ready true
                        :participants [{:patient-id waiting-id :role :subject}]})))
     :advance 0}))

;; --- M2b: churn family (docs/patient-state-model.md's event-validity
;; table; docs/event-sourcing.md's shadow-field dissolution) ---------------

(def ^:private reinstatable-event-types
  "The event classes a cancel decide reinstates state FROM, and therefore
  the only ones `run`'s `:reinstate-index` records (ADR-0169).

  `:cancel-transfer` restores `:home-ward`/`:location`; `:cancel-discharge`
  restores those plus `:attending`. `:cancel-admit` is deliberately
  ABSENT: its own decide reads nothing but the live patient's
  `:active-mrn`, so it never queried the log for prior state and has
  nothing to carry. `:transfer-in-error` is absent for the opposite
  reason -- it emits its transfer and that transfer's cancel in ONE
  decide, off the live pre-transfer patient -- there is no intervening
  event for anything to have queried yet, its own comment -- so it too
  never replayed. Both were checked rather than assumed: the arc's scope
  named them as candidates."
  #{:transfer :discharge})

(defn- last-uncancelled-index
  "Index into `ground-truth` of the most recent `event-type` event
  naming `patient-id` that is NOT already the target of an earlier
  `cancel-type` event -- the applicability query the event-validity
  table's cancel-* row asks ('the event class being cancelled must
  exist in this patient's log and not already be cancelled'). nil when
  no such event exists, which decide turns into a structured rejection
  rather than a throw."
  [ground-truth patient-id event-type cancel-type]
  (let [already-cancelled (into #{}
                                (comp (filter #(= cancel-type (:event %)))
                                      (map :cancels-event-id))
                                ground-truth)]
    (last (keep-indexed (fn [i ev]
                          (when (and (= event-type (:event ev))
                                     (some #(= patient-id (:patient-id %)) (:participants ev))
                                     (not (already-cancelled i)))
                            i))
                        ground-truth))))

(def documented-step-rejection-reasons
  "The closed enum every :step-rejected event's :reason must be drawn
  from (sim/ADR-0012's own invariant: 'every rejection's reason is from a
  documented enum') -- check.clj's step-rejected-reason-is-documented
  validates every log against exactly this set, so a new rejection path
  earns an entry here in the same change (the co-landing convention,
  extended to this event type)."
  #{:illegal-cancel-admit
    :illegal-cancel-transfer :illegal-cancel-transfer-bed-reoccupied
    :illegal-cancel-discharge :illegal-cancel-discharge-bed-reoccupied
    :illegal-bed-swap :illegal-merge})

(defn- rejected-outcome
  "sim/ADR-0012 (M3): a decide-time rejection is no longer a silent no-op --
  a :step-rejected ground-truth event now enters `:events` (folded via
  `evolve`'s own identity method for this type, below, and logged like
  any other event) alongside the pre-existing :rejected key callers
  already read directly off decide's return value. `:participants`
  names ONLY `patient-id`, the one attempting the step -- never a
  possibly-nonexistent :with target named in `step` itself (that stays
  in :attempted-step, plain data no invariant needs to resolve to a
  real patient), so participant-ids-exist-in-run stays sound for every
  rejection, including one that names a typo'd or never-admitted peer.
  No RNG is drawn here: decide already drew everything it was going to
  draw before discovering the rejection (determinism note, sim/ADR-0012)."
  [reason patient-id t step extra]
  (let [event {:event :step-rejected :t t
               :participants [{:patient-id patient-id :role :subject}]
               :attempted-step step
               :reason reason}]
    {:events [event] :advance 0 :rejected (merge {:reason reason :patient-id patient-id} extra)}))

(defmethod decide :cancel-admit
  [_streams t world patient-id step]
  (let [ground-truth (:ground-truth world)
        idx (last-uncancelled-index ground-truth patient-id :admission :cancel-admit)]
    (if (nil? idx)
      (rejected-outcome :illegal-cancel-admit patient-id t step nil)
      (let [patient (get-in world [:patients patient-id])]
        {:events [{:event :cancel-admit :t t :active-mrn (:active-mrn patient)
                   :cancels-event-id idx
                   :participants [{:patient-id patient-id :role :subject}]}]
         :advance 0}))))

(defmethod decide :transfer-in-error
  [{world-rng :world} t world patient-id {:keys [location force-placement]}]
  (let [{:keys [facility patients ground-truth]} world
        board (sim-model/occupancy-board patients)
        patient (get patients patient-id)
        alloc (sim-model/allocate world-rng facility board location force-placement)]
    (if (:exhausted alloc)
      (exhausted-outcome patient-id location facility board)
      ;; Both events are decided ATOMICALLY, in the same decide call --
      ;; the transfer, then its own immediate correction (A12, in-error).
      ;; The cancel's reinstated home-ward/location come straight off the
      ;; CURRENT (pre-transfer) patient state, not a log query: there is
      ;; no intervening event for anything to have queried yet.
      (let [transfer-idx (count ground-truth)
            transfer-event (merge {:event :transfer :t t :active-mrn (:active-mrn patient) :from (:location patient)
                                    :attending (:attending patient) :bed-ready false
                                    :participants [{:patient-id patient-id :role :subject}]}
                                   alloc)
            cancel-event {:event :cancel-transfer :t t :active-mrn (:active-mrn patient)
                          :cancels-event-id transfer-idx :in-error true
                          :home-ward (:home-ward patient) :location (:location patient)
                          :participants [{:patient-id patient-id :role :subject}]}]
        {:events [transfer-event cancel-event] :advance 0}))))

(defn- uniform-choice
  [^Random rng candidates]
  (nth candidates (.nextInt rng (count candidates))))

(defmethod decide :bed-swap
  [{world-rng :world} t world patient-id {:keys [with] :as step}]
  (let [{:keys [patients]} world
        self (get patients patient-id)
        eligible (->> patients
                     (remove (fn [[pid _]] (= pid patient-id)))
                     (filter (fn [[_ p]] (and (= :admitted (:status p)) (some? (:location p)))))
                     (mapv first))
        peer-id (cond
                  with with
                  (seq eligible) (uniform-choice world-rng eligible)
                  :else nil)
        peer (get patients peer-id)]
    (if (or (nil? peer-id) (nil? peer) (not= :admitted (:status peer)) (nil? (:location peer)))
      (rejected-outcome :illegal-bed-swap patient-id t step {:with with})
      {:events [{:event :bed-swap :t t
                 :participants [{:patient-id patient-id :role :subject}
                                {:patient-id peer-id :role :subject}]
                 :swap {patient-id {:active-mrn (:active-mrn self) :from (:location self)
                                    :to (:location peer) :attending (:attending self)}
                        peer-id {:active-mrn (:active-mrn peer) :from (:location peer)
                                :to (:location self) :attending (:attending peer)}}}]
       :advance 0})))

(defmethod decide :merge
  [{world-rng :world} t world patient-id {:keys [with] :as step}]
  (let [{:keys [patients ground-truth]} world
        survivor (get patients patient-id)
        ;; :new (never admitted -- no :admission event exists yet for
        ;; participant-ids-exist-in-run to find) and :merged (already
        ;; merged away) are never legal merge targets, dynamically
        ;; picked OR explicitly named via :with.
        never-mergeable? (fn [p] (#{:new :merged} (:status p)))
        eligible (->> patients
                     (remove (fn [[pid _]] (= pid patient-id)))
                     (remove (fn [[_ p]] (never-mergeable? p)))
                     (mapv first))
        merged-id (cond
                    with with
                    (seq eligible) (uniform-choice world-rng eligible)
                    :else nil)
        merged (get patients merged-id)
        already-merged? (some (fn [ev]
                                (and (= :merge (:event ev))
                                     (some #(and (= :merged (:role %)) (= merged-id (:patient-id %)))
                                           (:participants ev))))
                              ground-truth)]
    (if (or (nil? merged-id) (= patient-id merged-id) (nil? merged)
            (never-mergeable? merged) already-merged?)
      (rejected-outcome :illegal-merge patient-id t step {:with with})
      {:events [{:event :merge :t t
                 :participants [{:patient-id patient-id :role :survivor}
                                {:patient-id merged-id :role :merged}]
                 :surviving-mrn (:active-mrn survivor)
                 :merged-mrn (:active-mrn merged)
                 :merged-mrns (:mrns merged)}]
       :advance 0})))

;; --- M3: order/result (auto-paired, docs/sim-theory.edn's order-profiles
;; catalytic) ---------------------------------------------------------------

(defmethod decide :order
  [{rng :patient} t world patient-id {:keys [profile]}]
  (let [{:keys [patients ground-truth order-profiles]} world
        patient (get patients patient-id)
        prof (get order-profiles profile)
        order-idx (count ground-truth)
        order-event {:event :order-placed :t t :active-mrn (:active-mrn patient)
                     :profile profile :concept (:concept prof)
                     :location (:location patient) :attending (:attending patient)
                     :participants [{:patient-id patient-id :role :subject}]}
        ;; :turnaround-minutes is authored (in the profile) the same
        ;; minutes-authored way :delay's IR is (docs/patient-state-
        ;; model.md's durations rule); converted to seconds here, the
        ;; same one place :delay's own decide method already converts.
        turnaround-seconds (* 60 (rand-int-in rng (get-in prof [:turnaround-minutes :from])
                                              (get-in prof [:turnaround-minutes :to])))
        results (mapv (fn [analyte]
                        (let [value (order-profiles/sample-analyte-value rng analyte)]
                          ;; ADR-0150 (census S-6): the EVENT key is `:unit`,
                          ;; singular, matching :observation and a
                          ;; :diagnostic-report's children. The order-profile
                          ;; ANALYTE key stays `:units` -- it is a
                          ;; user-reachable `--config` surface (docs/cli.md,
                          ;; `:order-profiles`) and renaming it would break
                          ;; every config a user already wrote. Translated
                          ;; here, the same one-place translation `evolve
                          ;; :result-available` already performs downstream.
                          {:concept (:concept analyte) :unit (:units analyte) :value value
                           :reference-range (:reference-range analyte)
                           ;; Computed truth, not sampled (Task 4's mini-law):
                           ;; the flag is DERIVED from value vs range, here
                           ;; and nowhere else.
                           :abnormal-flag (order-profiles/abnormal-flag value (:reference-range analyte))}))
                      (:analytes prof))
        result-t (+ t turnaround-seconds)
        ;; :location/:attending are the patient's state AT ORDER TIME
        ;; (decide has no access to a FUTURE fold -- sim/ADR-0008 -- and the
        ;; result event's own values were already fully computed atomically
        ;; back here); PV1 context for both messages reflects where the
        ;; specimen was ordered, the same convention real order/result
        ;; pairs use when a patient's location changes between the two.
        result-event {:event :result-available :t result-t :active-mrn (:active-mrn patient)
                      :profile profile :order-event-id order-idx :concept (:concept prof)
                      :location (:location patient) :attending (:attending patient)
                      :results results
                      :participants [{:patient-id patient-id :role :subject}]}]
    ;; The result event is fully computed NOW (all its RNG draws happen
    ;; in this one decide call, same "decided atomically" precedent
    ;; transfer-in-error already sets) but is NEVER returned directly in
    ;; :events -- a future-t event spliced into THIS call's :events
    ;; would enter ground-truth at this call's OWN log position, ahead
    ;; of other patients' events with SMALLER :t that get processed
    ;; later in wall-loop order, breaking the log's global t-ordering
    ;; (engine-test's own `(apply <= (map :t ground-truth))` sanity
    ;; check, and the derivability law any emitter/consumer relies on).
    ;; Instead it rides `:schedule-followup`: the run loop enqueues it
    ;; as a genuine future queue entry, so it enters ground-truth at its
    ;; own correct global [t seq-no] position, the same way every other
    ;; scheduled event does.
    {:events [order-event] :advance 0
     :schedule-followup {:t result-t :patient-id patient-id
                         :steps [{:type :result-followup :result-event result-event}]}}))

(defmethod decide :result-followup
  [_streams _t _world _patient-id {:keys [result-event]}]
  {:events [result-event] :advance 0})

;; --- M5b: :outpatient-visit / :outpatient-visit-end (components/patient-simulator/docs/gmf-interpreter.md
;; section 4's sketch, items 5-7) --------------------------------------------

(defmethod decide :outpatient-visit
  [{facility-rng :facility} t world patient-id step]
  ;; Item 5: NO sim-model/allocate call -- an outpatient encounter occupies
  ;; no bed, so there is no ladder to consult. Still gets an attending
  ;; (real ambulatory visits have a treating provider) -- chosen uniformly
  ;; among ALL providers, not ward-filtered (there is no ward), the same
  ;; "no ward-scoping concept, choose uniformly among everyone" treatment
  ;; bed-swap/merge's own peer selection already establishes.
  (let [{:keys [providers patients]} world
        patient (get patients patient-id)
        attending (:id (uniform-choice facility-rng providers))]
    {:events [(merge {:event :outpatient-visit :t t :active-mrn (:active-mrn patient)
                      :attending attending
                      :participants [{:patient-id patient-id :role :subject}]}
                     (reason-field step) (citation-fields step))]
     :advance 0}))

(defmethod decide :outpatient-visit-end
  [_streams t world patient-id step]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :outpatient-visit-end :t t :active-mrn (:active-mrn patient)
                      :attending (:attending patient)
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

;; --- M5b: CompileTrajectory's new ground-truth event types (docs/gmf-
;; interpreter.md section 1's table) -- each is a real, glass-box-cited
;; ground-truth event; none carries or changes PatientState (the log
;; itself is the record, sim/ADR-0008, the same "no PatientState field for
;; it" treatment :order-placed/:result-available already get). None
;; consumes RNG -- their content was already fully sampled by the GMF
;; interpreter (M5a); CompileTrajectory/the engine only replay it.

(defmethod decide :procedure
  [_streams t world patient-id {:keys [codes] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :procedure :t t :active-mrn (:active-mrn patient) :codes codes
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defn- observation-value-fields
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

(defmethod decide :observation
  [_streams t world patient-id {:keys [codes] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :observation :t t :active-mrn (:active-mrn patient) :codes codes}
                     (observation-value-fields step)
                     {:participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

;; --- GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P5): both
;; MultiObservation and DiagnosticReport compile to this SAME step type
;; -- ONE ground-truth event for the whole state, carrying the full
;; :observations vector, mirroring how the compiled IR step itself
;; bundles children (never one event per child).

(defmethod decide :diagnostic-report
  [_streams t world patient-id {:keys [codes observations] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :diagnostic-report :t t :active-mrn (:active-mrn patient) :observations observations}
                     (when codes {:codes codes})
                     {:participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defmethod decide :medication-order
  [_streams t world patient-id {:keys [codes] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :medication-order :t t :active-mrn (:active-mrn patient) :codes codes
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(def ^:private cited-opening-event-types
  "The two event classes whose LAST citation-matching occurrence a
  terminal step resolves against: a `:medication-end` resolves its
  `:order-citation` to a `:medication-order`, a `:care-plan-end` its
  `:care-plan-citation` to a `:care-plan-start`. ADR-0169's carrier
  records these and nothing else."
  #{:medication-order :care-plan-start})

(defn- last-cited-index
  "Index into the log of the LAST `opening-type` event carrying
  `citation` and naming `patient-id` as a participant -- nil when there
  is none, and nil when `citation` itself is nil.

  Exactly what ADR-0164's two `keep-indexed` scans computed, and
  therefore exactly what `:medication-end`'s `:order-event-id` and
  `:care-plan-end`'s `:start-event-id` still are. ADR-0169 (arc 0)
  replaces the scan with a lookup: the two were 21.3% and 10.9% of the
  generate phase at 10^5 events, 32.2% combined, and each walked the
  WHOLE log once per terminal step. ADR-0164 scoped them by patient --
  it added the participant predicate INSIDE the same full-length
  `keep-indexed` -- which made them correct without making them shorter;
  this is the shortening, and it changes no answer.

  `run` carries `{[opening-type patient-id citation] last-index}`,
  written as events are appended, so a later occurrence simply
  overwrites an earlier one and the stored value IS `last`'s answer.
  Only events with a NON-NIL `:citation` are recorded: a nil citation
  could never be returned anyway, since both call sites are already
  guarded by `(when <citation> ...)`.

  FALLS BACK to the scan it replaces when `world` carries no
  `:citation-index` KEY -- a hand-built world, as most of engine-test
  uses. Same fallback rule as `reinstated-state`, and for the same
  reason: on the key, never on a missing entry, so a carrier that
  `run` built but failed to populate shows up as a changed corpus rather
  than as a silent replay.

  Proven post hoc against the scan itself by
  `ehrt.sim.run-test/citation-resolution-matches-the-whole-log-scan` on
  every gated corpus, seed 424242 (ADR-0163's own run) included."
  [world ground-truth opening-type patient-id citation]
  (when citation
    (if (contains? world :citation-index)
      (get (:citation-index world) [opening-type patient-id citation])
      (last (keep-indexed (fn [i ev] (when (and (= opening-type (:event ev))
                                                (= citation (:citation ev))
                                                (some #(= patient-id (:patient-id %))
                                                      (:participants ev)))
                                       i))
                          ground-truth)))))

(defn person-entry
  "What `world`'s `:person-index` holds for one person -- the patient a
  returning person resolves to, and what has been minted for them so far
  (`init-world`'s own comment carries the entry shape). nil when this
  person has not been seen before.

  ADR-0173 section 2(a) (arc 3a). Lands AHEAD of its caller,
  deliberately, and it is the reader that makes the carried index's
  hand-built-world contract real: FALLS BACK to nil when `world` carries
  no `:person-index` KEY -- on the KEY, never on a missing entry, the
  same rule `reinstated-state` and `last-cited-index` already follow, so
  a carrier `run` built but failed to populate shows up as a changed
  corpus rather than as a silent miss. Part 3's arrival selection is
  what writes it; part 4 grows each entry a `:placeholders` set -- every
  unidentified record minted for that person, before any of them is
  filled or merged. `run` still seeds the key EMPTY for a run with no
  `:persons`."
  [world person-id]
  (when (contains? world :person-index)
    (get (:person-index world) person-id)))

(defmethod decide :medication-end
  [_streams t world patient-id {:keys [order-citation] :as step}]
  ;; Resolved by CITATION match against ground-truth, never a pathway-
  ;; position index (pathway.clj's own :medication-end docstring) -- the
  ;; same glass-box, position-independent resolution ConditionEnd's own
  ;; trajectory-level :references already models, one level down at the
  ;; ground-truth log.
  (let [{:keys [ground-truth patients]} world
        patient (get patients patient-id)
        ;; ADR-0164: SAME PATIENT, too. A citation is `{:module :state}`
        ;; -- a module coordinate, not a patient-qualified one -- so two
        ;; patients walking the same module cite identically, and an
        ;; unfiltered `last` over the whole log hands this end whichever
        ;; patient's order came LAST. The participant predicate is the
        ;; one `last-uncancelled-index` (above) already uses for exactly
        ;; this reason, and the one check.clj's own medication-end
        ;; invariant tests the resolved target against.
        order-event-id (last-cited-index world ground-truth :medication-order
                                         patient-id order-citation)]
    ;; M6 Task 1: `:order-citation` now rides the event itself, alongside
    ;; the already-resolved `:order-event-id` -- `evolve`'s own fold-time
    ;; medication-orders match needs the CITATION (position-independent),
    ;; never the log-position index `:order-event-id` carries (meaningless
    ;; to a fold that never sees the whole log).
    {:events [(merge {:event :medication-end :t t :active-mrn (:active-mrn patient)
                      :order-event-id order-event-id :order-citation order-citation
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

;; GMF coverage Wave D stage D2 (2026-08-02, ADR-0029 R2(b)): the SAME
;; decide/evolve shape :medication-order/:medication-end establish,
;; two defmethod-pairs up.

(defmethod decide :care-plan-start
  [_streams t world patient-id {:keys [codes activities] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :care-plan-start :t t :active-mrn (:active-mrn patient) :codes codes}
                     (when activities {:activities activities})
                     {:participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defmethod decide :care-plan-end
  [_streams t world patient-id {:keys [care-plan-citation] :as step}]
  ;; Resolved by CITATION match against ground-truth, never a pathway-
  ;; position index -- the same glass-box, position-independent
  ;; resolution :medication-end already models.
  (let [{:keys [ground-truth patients]} world
        patient (get patients patient-id)
        ;; ADR-0164: SAME PATIENT, too -- the twin of the scan
        ;; :medication-end already carries, for the identical reason.
        start-event-id (last-cited-index world ground-truth :care-plan-start
                                         patient-id care-plan-citation)]
    {:events [(merge {:event :care-plan-end :t t :active-mrn (:active-mrn patient)
                      :start-event-id start-event-id :care-plan-citation care-plan-citation
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defn- fold-condition-annotation
  "One step in folding a compiled encounter step's own :conditions
  vector (sim-model/ConditionAnnotation) into `conditions`
  (a patient's own ConditionRecord vector) -- an onset OPENS a new
  record; an end CLOSES the most recent still-:active record with the
  SAME :codes (compile-trajectory's own annotate-condition already
  resolves a condition-end's :codes from its referenced onset, so codes
  match even though onset/end carry DIFFERENT citations -- one per
  module state). `t` is the enclosing encounter event's own :t: this
  project only ever compiles ONE encounter per patient (the single-
  encounter-horizon scope, sim/ADR-0007), so every condition annotation for
  a patient rides that SAME event -- onset and end share one instant
  rather than each carrying its own, a documented simplification of
  this scope, not a claim that a condition's real onset and resolution
  were simultaneous."
  [t conditions {:keys [event codes citation]}]
  (case event
    :condition-onset
    (conj (or conditions []) {:codes codes :citation citation :onset-t t :clinical-status :active})

    :condition-end
    (if-let [idx (last (keep-indexed (fn [i c] (when (and (= :active (:clinical-status c)) (= codes (:codes c))) i))
                                     conditions))]
      (update conditions idx assoc :clinical-status :resolved :end-t t)
      conditions)))

(defn- fold-conditions
  [patient t annotations]
  (cond-> patient
    (seq annotations) (update :conditions #(reduce (partial fold-condition-annotation t) % annotations))))

(defmulti evolve
  "Folds one ground-truth event into ONE patient it names:
  (patient-state, event) -> patient-state'. Pure and total: no RNG, no
  knowledge of the step or decision that produced the event, no
  knowledge of `world` or any other patient. This is the ONLY function
  that ever produces a new patient state (sim/ADR-0008) -- the run loop is
  what maps an event to every participant's slice of `world` (via the
  event's own :participants, sim/ADR-0010) and folds `evolve` in there,
  once per participant."
  (fn [_patient event] (:event event)))

(defmethod evolve :registered
  ;; ADR-0173 section 2(b): `:demographics` is seeded HERE, from the same
  ;; Persona, and `:persona` keeps its t0 meaning untouched -- so all
  ;; fourteen t0-only census sites read exactly what they always read.
  ;;
  ;; ARC 3A PART 3: `:residence` overrides the seed's own `:housed` arm
  ;; when the event carries one. It rides ONLY a registration bound to a
  ;; person who is not housed at that instant; absent -- every event in
  ;; every corpus that has no `:persons` behind it -- this is the seed
  ;; verbatim.
  ;;
  ;; ARC 3A PART 4: a PLACEHOLDER registration seeds
  ;; `placeholder-demographics` instead -- the window's alias name, an
  ;; `:unknown` residence and nothing else. `:persona` is written all
  ;; the same, because ground truth knows who this patient is even
  ;; while the modelled hospital does not, and every t0-only census
  ;; site keeps reading a Persona for every patient.
  [patient {:keys [persona residence alias-name identity]}]
  (assoc patient
         :persona persona
         :demographics (if (= :placeholder identity)
                         (placeholder-demographics alias-name)
                         (cond-> (demographics-from-persona persona)
                           (and residence (some? persona)) (assoc :residence residence)))))

;; --- arc 3a part 3: the two folding siblings ------------------------------
;;
;; `:field` is a key of `Demographics` verbatim (`person-fold/
;; demographic-effect` is what guarantees that), which is what lets the
;; fold be one `assoc` rather than a case per person-event kind. Both
;; are TOTAL over a hand-authored log the same way every other method
;; here is: a patient with no `:demographics` at all (no `:registered`
;; ever folded) is returned untouched rather than growing a map with one
;; field in it, which would be a demographic state claiming every other
;; field is unknown.

(defmethod evolve :demographic-update
  ;; ARC 3A PART 4: `:cause :identity-fill` is the one branch that writes
  ;; the WHOLE demographic state rather than one field of it, because
  ;; that is what the fact is -- the record now belongs to a known
  ;; person, and every field they have follows. `:persona` (the t0
  ;; record) is NOT rewritten: a placeholder registration already
  ;; carried the right one, and mutating it here would falsify the one
  ;; property fourteen census sites depend on.
  [patient {:keys [field value cause persona residence]}]
  (if (= :identity-fill cause)
    (cond-> patient
      (some? (:demographics patient))
      (assoc :demographics (cond-> (demographics-from-persona persona)
                             residence (assoc :residence residence))))
    (cond-> patient
      (some? (:demographics patient)) (assoc-in [:demographics field] value))))

(defmethod evolve :coverage-change
  [patient {:keys [payer]}]
  (cond-> patient
    (some? (:demographics patient)) (assoc-in [:demographics :payer] payer)))

(defmethod evolve :admission
  [patient {:keys [location home-ward attending t conditions]}]
  (-> patient
      (assoc :status :admitted
             :class :inpatient
             :home-ward home-ward
             :location location
             :attending attending
             :admitted-at t)
      (fold-conditions t conditions)))

(defmethod evolve :transfer
  [patient {:keys [location home-ward]}]
  (assoc patient :location location :home-ward home-ward))

(defmethod evolve :discharge
  ;; Wave C (2026-08-02, ADR-0028, C3): an expired-disposition discharge
  ;; sets :status :expired, never :discharged -- and, unlike an ordinary
  ;; discharge, leaves :location/:attending UNCHANGED: the body remains
  ;; wherever it was at the moment of death (patient-state-model.md's
  ;; own "clinically absorbing but operationally alive" fact), a LATER
  ;; morgue transfer or final disposition-20 discharge (donor/post-
  ;; mortem administrative content, out of this wave's own minimal
  ;; scope) is what would eventually move or discharge it, not this
  ;; event.
  [patient {:keys [t disposition]}]
  (if (= :expired disposition)
    (assoc patient :status :expired)
    (assoc patient :status :discharged :location nil :discharged-at t)))

;; --- M2b: churn family evolves -------------------------------------------

(defmethod evolve :cancel-admit
  [patient _event]
  (-> patient (assoc :status :new) (dissoc :class :home-ward :location :attending :admitted-at)))

(defmethod evolve :cancel-transfer
  [patient {:keys [home-ward location]}]
  (assoc patient :home-ward home-ward :location location))

(defmethod evolve :cancel-discharge
  ;; M6 Task 2 finding: :class must be part of the reinstatement, not
  ;; merely :home-ward/:location/:attending -- a degenerate but
  ;; structurally legal churn sequence (cancel-admit against an
  ;; ALREADY-DISCHARGED patient's original admission, `last-uncancelled-
  ;; index` doesn't gate on current status) strips :class via cancel-
  ;; admit's own dissoc; a following cancel-discharge that omitted :class
  ;; would leave an :admitted patient with no class at all, while the
  ;; wire (ehrt.sim-emit-hl7.emit-hl7's own single-subject-message) always
  ;; renders PV1-2 :inpatient for this event family regardless -- a real
  ;; wire/truth disagreement the emitter-coherence property surfaced.
  ;; :inpatient is the only value ever legal here: :discharge (unlike
  ;; :outpatient-visit-end) is reachable only from an :admission, which
  ;; always sets :class :inpatient (docs/patient-state-model.md).
  [patient {:keys [home-ward location attending]}]
  (-> patient
      (assoc :status :admitted :class :inpatient :home-ward home-ward :location location :attending attending)
      (dissoc :discharged-at)))

(defmethod evolve :bed-swap
  [patient {:keys [swap]}]
  (assoc patient :location (get-in swap [(:patient-id patient) :to])))

(defmethod evolve :merge
  [patient {:keys [participants surviving-mrn merged-mrns]}]
  (let [role (:role (first (filter #(= (:patient-id patient) (:patient-id %)) participants)))]
    (case role
      :survivor (-> patient (update :mrns into merged-mrns) (assoc :active-mrn surviving-mrn))
      :merged (assoc patient :status :merged))))

;; --- sim/ADR-0012: :step-rejected -- truth about the run, never a state
;; transition (the attempted step never actually happened) --------------

(defmethod evolve :step-rejected
  [patient _event]
  patient)

;; --- M3: order/result -- neither changes any PatientState field today
;; (docs/patient-state-model.md's accumulator has no order/result-history
;; field; the log itself is the history, queried directly, sim/ADR-0008) ------

(defmethod evolve :order-placed
  [patient _event]
  patient)

(defmethod evolve :result-available
  ;; M6 Task 1: EVERY analyte in :results becomes its own ObservationRecord
  ;; -- order-profiles' richer shape (reference-range/computed abnormal
  ;; flag) is exactly what Observation.referenceRange/interpretation
  ;; render from (EmitState, below); :concept (singular) wraps to a
  ;; single-element :codes vector, the same CodeableConcept shape every
  ;; other record here uses.
  [patient {:keys [t results]}]
  (update patient :observations (fnil into [])
          (mapv (fn [{:keys [concept unit value reference-range abnormal-flag]}]
                  {:codes [concept] :t t :value value :unit unit
                   :reference-range reference-range :interpretation abnormal-flag})
                results)))

;; --- M5b: :outpatient-visit / :outpatient-visit-end -----------------------
;; Item 5/7: :status re-uses the SAME values :admission/:discharge already
;; establish (:new -> :admitted -> :discharged) -- no new :status value
;; invented, :class :outpatient is already the distinguishing fact. Item 6:
;; :location and :home-ward are never set at all (stay absent/nil) -- the
;; named exception to "never nil-bed while admitted" (docs/patient-state-
;; model.md's event-validity table, the conditional row this milestone adds).

(defmethod evolve :outpatient-visit
  [patient {:keys [attending t conditions]}]
  (-> patient
      (assoc :status :admitted :class :outpatient :attending attending)
      (fold-conditions t conditions)))

(defmethod evolve :outpatient-visit-end
  [patient {:keys [t]}]
  (assoc patient :status :discharged :discharged-at t))

;; --- M5b: :procedure -- a log-only fact, no PatientState field change
;; (Procedure is deliberately outside EmitState's own rendered resource
;; set, M6 Task 1 -- "keep the resource set to what state actually
;; holds," applied by never accumulating what nothing renders).

(defmethod evolve :procedure [patient _event] patient)

;; --- M6 Task 1: :observation/:medication-order/:medication-end now land
;; in the clinical-content accumulator (this namespace's own header
;; comment above PatientState) -- EmitState's Observation/MedicationRequest
;; resources render from exactly these records, nothing re-derived from
;; the log.

(defmethod evolve :observation
  [patient {:keys [t codes] :as event}]
  (update patient :observations (fnil conj [])
          (merge {:codes codes :t t} (observation-value-fields event))))

;; --- GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P5): FLATTENS
;; each child into its own ObservationRecord -- the IDENTICAL pattern
;; :result-available's own per-analyte flattening already establishes
;; (below), reused rather than a third accumulator shape invented.

(defmethod evolve :diagnostic-report
  [patient {:keys [t observations]}]
  (update patient :observations (fnil into [])
          (mapv (fn [{:keys [codes] :as entry}] (merge {:codes codes :t t} (observation-value-fields entry)))
                observations)))

(defmethod evolve :medication-order
  [patient {:keys [t codes citation]}]
  (update patient :medication-orders (fnil conj [])
          {:codes codes :citation citation :ordered-t t :status :active}))

(defmethod evolve :medication-end
  ;; Citation-based, position-independent resolution (this project's
  ;; standing preference over a fragile index, docs/patient-state-
  ;; model.md's deterministic-event-id section) -- matches
  ;; `:order-citation` (the medication-end IR step's own field, riding
  ;; the ground-truth event unchanged since `decide`, below) against the
  ;; accumulator's own still-:active entry, never the ground-truth log's
  ;; :order-event-id (a log POSITION, meaningless at fold time without
  ;; the whole log in hand -- exactly what this fold is not supposed to
  ;; need, M6 Task 1's own snapshot-at-instant law).
  [patient {:keys [t order-citation]}]
  (if-let [idx (when order-citation
                 (last (keep-indexed (fn [i m] (when (and (= :active (:status m)) (= order-citation (:citation m))) i))
                                     (:medication-orders patient))))]
    (update-in patient [:medication-orders idx] assoc :status :completed :ended-t t)
    patient))

;; GMF coverage Wave D stage D2 (2026-08-02, ADR-0029 R2(b)): the SAME
;; fold shape :medication-order/:medication-end establish, one
;; defmethod-pair up -- CarePlan itself is v2-silent (R3), this record
;; exists for the fold and a future sim-emit-fhir consumer.

(defmethod evolve :care-plan-start
  [patient {:keys [t codes activities citation]}]
  (update patient :care-plans (fnil conj [])
          (cond-> {:codes codes :citation citation :started-t t :status :active}
            activities (assoc :activities activities))))

(defmethod evolve :care-plan-end
  [patient {:keys [t care-plan-citation]}]
  (if-let [idx (when care-plan-citation
                 (last (keep-indexed (fn [i m] (when (and (= :active (:status m)) (= care-plan-citation (:citation m))) i))
                                     (:care-plans patient))))]
    (update-in patient [:care-plans idx] assoc :status :completed :ended-t t)
    patient))

(defn replay
  "Replays `ground-truth` through `evolve`, returning a parallel seq of
  {:event :patient-id :before :after :world-before :world-after} --
  `:patient-id` is a convenience view of the event's PRIMARY (first)
  participant, since every check.clj invariant needs at most one
  patient's pre/post state even once M2b's bed-swap/merge span two
  (cross-participant invariants read world-before/world-after
  directly instead). Every participant in :participants folds via
  `evolve`, not just the primary one -- sim/ADR-0010: a patient's state
  folds exactly the events they participate in. `world-before`/
  `world-after` are the full {patient-id -> patient-state} map
  immediately before/after this event (sim/ADR-0008: state-history is
  derived -- this IS that derivation, generalized across patients)."
  [ground-truth]
  (loop [events ground-truth patients {} acc (transient [])]
    (if (empty? events)
      (persistent! acc)
      (let [event (first events)
            participant-ids (mapv :patient-id (:participants event))
            patients (reduce (fn [ps pid]
                                (if (contains? ps pid)
                                  ps
                                  (assoc ps pid (initial-patient pid (:active-mrn event)))))
                              patients participant-ids)
            patients' (reduce (fn [ps pid] (update ps pid evolve event)) patients participant-ids)
            subject-id (first participant-ids)]
        (recur (rest events) patients'
               (conj! acc {:event event :patient-id subject-id
                           :before (get patients subject-id) :after (get patients' subject-id)
                           :world-before patients :world-after patients'}))))))

;; --- M2b cancel-transfer/cancel-discharge: defined here, AFTER `replay`,
;; because their decide methods query it directly (docs/patient-state-
;; model.md's shadow-field dissolution: the reinstated prior state is
;; QUERIED FROM THE LOG at decide-time, never a field the accumulator
;; carries for this purpose alone).

(defn- bed-reoccupied-by-someone-else?
  "Whether `location`'s bed is CURRENTLY held by a patient other than
  `patient-id` -- the reinstatement guard cancel-transfer/cancel-
  discharge both need: the log-derived prior location was free WHEN it
  was vacated, but time has passed since, and another patient's own
  allocation (a later admission, a bed-ready transfer) may have
  legitimately claimed it in the meantime. Reinstating into an
  occupied bed would violate no-double-occupancy, so this is checked
  against the LIVE occupancy board (world, not the log) at decide-time
  -- the same board :admission/:transfer already consult."
  [world patient-id location]
  (when-let [bed (:bed location)]
    (let [occupant (get (sim-model/occupancy-board (:patients world)) bed)]
      (and (some? occupant) (not= occupant patient-id)))))

(defn- reinstated-state
  "The state patient `patient-id` was in immediately BEFORE the log event
  at `idx` -- the prior location/home-ward/attending a reinstating cancel
  restores. Exactly `(:before (nth (replay ground-truth) idx))`, and
  proven so post hoc, twice: `ehrt.sim.run-test/cancel-decides-reinstate-
  exactly-what-replay-would-hand-back` recomputes it against `replay`
  itself on every gated corpus, and `ehrt.sim-engine.engine-test/cancel-
  reinstatement-survives-the-fold-carried-index` does the same over
  churn-driven generated runs -- which it must, because only ONE of the
  four gated corpora carries a reinstating cancel at all.

  ADR-0169 (arc 0), the largest single generator-side cost the 2026-08-24
  throughput spike measured -- 35.3% of the generate phase at 10^5
  events, larger than both ADR-0164 citation scans combined. Both cancel
  decides used to evaluate `(nth (replay ground-truth) idx)` literally:
  a full `evolve` re-simulation of the ENTIRE log, materialising a vector
  of N maps carrying `:world-before`/`:world-after`, in order to read ONE
  element at an index the caller already held, and then discard the rest.
  Once per cancel event, so O(N) with allocation per cancel and quadratic
  in churn density.

  The run loop already computes that state: it is the patient's entry in
  `world` at the instant the event was appended, and `world`'s
  `:patients` is folded through the SAME `evolve` over the SAME events in
  the SAME order that `replay` folds. So `run` now records it, for
  `:transfer` and `:discharge` events only (the two reinstatable classes
  -- `:cancel-admit` reads no prior state at all, and
  `:transfer-in-error` decides its own cancel atomically off the live
  patient, neither of them touching the log), under the log index of the
  event itself. The read is a map lookup.

  FALLS BACK to the replay it replaces when `world` carries no
  `:reinstate-index` KEY -- a world built by hand rather than by `run`,
  which is how most of engine-test drives `decide` directly. The fallback
  is on the key's presence, never on a missing entry: a world that `run`
  built and an entry that is nevertheless absent is a DEFECT, and letting
  it read nil (which changes the emitted event, which the byte-identity
  gate then fails) is the behaviour that surfaces it. Silently replaying
  instead would hide it."
  [world ground-truth patient-id idx]
  (if (contains? world :reinstate-index)
    (get (:reinstate-index world) idx)
    (:before (nth (replay ground-truth) idx))))

(defmethod decide :cancel-transfer
  [_streams t world patient-id step]
  (let [ground-truth (:ground-truth world)
        idx (last-uncancelled-index ground-truth patient-id :transfer :cancel-transfer)]
    (if (nil? idx)
      (rejected-outcome :illegal-cancel-transfer patient-id t step nil)
      (let [patient (get-in world [:patients patient-id])
            {:keys [home-ward location]} (reinstated-state world ground-truth patient-id idx)]
        (if (bed-reoccupied-by-someone-else? world patient-id location)
          (rejected-outcome :illegal-cancel-transfer-bed-reoccupied patient-id t step {:location location})
          {:events [{:event :cancel-transfer :t t :active-mrn (:active-mrn patient)
                     :cancels-event-id idx :home-ward home-ward :location location
                     :participants [{:patient-id patient-id :role :subject}]}]
           :advance 0})))))

(defmethod decide :cancel-discharge
  [_streams t world patient-id step]
  (let [ground-truth (:ground-truth world)
        idx (last-uncancelled-index ground-truth patient-id :discharge :cancel-discharge)]
    (if (nil? idx)
      (rejected-outcome :illegal-cancel-discharge patient-id t step nil)
      (let [patient (get-in world [:patients patient-id])
            {:keys [home-ward location attending]} (reinstated-state world ground-truth patient-id idx)]
        (if (bed-reoccupied-by-someone-else? world patient-id location)
          (rejected-outcome :illegal-cancel-discharge-bed-reoccupied patient-id t step {:location location})
          {:events [{:event :cancel-discharge :t t :active-mrn (:active-mrn patient)
                     :cancels-event-id idx :home-ward home-ward :location location :attending attending
                     :participants [{:patient-id patient-id :role :subject}]}]
           :advance 0})))))

;; --- M3-adjacent: per-patient pathway assignment (roadmap.md's M3 entry,
;; SimHospital's percentage_of_patients analogue -- the distribution layer
;; M5's CompileTrajectory will also need, one pathway per patient) --------

(defn- weighted-pick
  "Which pool member `draw` (a uniform double in [0,1), already
  consumed by the caller) falls into, among `pool` ({value-key :weight}
  maps) -- cumulative-weight bucketing, falling through to the last
  member on any floating-point-boundary edge case rather than nil.
  `value-key` is which field names the resolved value -- :pathway for
  sim-model/PathwaysConfig, :module-id for M5b's own
  ehrt.patient-simulator.gmf/ModulesConfig -- the same pool shape, two resource
  kinds."
  [pool draw value-key]
  (let [total (reduce + (map :weight pool))
        target (* draw total)]
    (loop [members pool acc 0.0]
      (let [m (first members)
            more (rest members)
            acc' (+ acc (double (:weight m)))]
        (if (or (empty? more) (< target acc'))
          (get m value-key)
          (recur more acc'))))))

(defn assign-pathway
  "Resolves the pathway `pathways-config` (sim-model/
  PathwaysConfig) assigns to patient ordinal `i` (0-indexed arrival
  order): an explicit {:patient-ordinal i :pathway ...} entry when one
  names this ordinal, otherwise a weighted pick among the config's
  {:pathway :weight} pool entries. Today's single-:pathway `run` config
  is this function's degenerate case, expressed as a one-entry weighted
  pool with :weight 1 -- see `run`'s own docstring for why that case is
  NOT wired to skip the draw below (it would perturb the very law this
  paragraph states next).

  ALWAYS consumes exactly one `.nextDouble` from `rng`, whether the
  outcome is the explicit override or the weighted pick -- fixed RNG
  consumption per patient, sim/ADR-0009's own rejected-alternative reasoning
  extended here: making draw count depend on whether THIS patient
  happens to have an explicit override would mean adding one scripted
  override for patient N shifts every OTHER patient's downstream draws,
  the exact surprising coupling sim/ADR-0009 already rejected for bed
  choice (there: 'consumption changed once, for a documented reason' is
  the accepted property; making it depend on candidate count is not)."
  [^Random rng pathways-config i]
  (let [draw (.nextDouble rng)
        explicit (first (filter #(= i (:patient-ordinal %)) pathways-config))]
    (if explicit
      (:pathway explicit)
      (weighted-pick (filterv :weight pathways-config) draw :pathway))))

;; --- M5b: per-patient module assignment (ehrt.patient-simulator.gmf/
;; ModulesConfig) -- the SAME shape/law as assign-pathway just above,
;; extended to modules per components/patient-simulator/docs/gmf-interpreter.md's own Task 4 (module
;; assignment composes with :pathways -- both just IR entering the union).

(defn assign-module
  "Resolves the module id `modules-config` (ehrt.patient-simulator.gmf/
  ModulesConfig) assigns to patient ordinal `i` -- an explicit
  {:patient-ordinal i :module-id ...} entry when one names this ordinal,
  otherwise a weighted pick among the config's {:module-id :weight} pool
  entries, or nil when NEITHER covers this ordinal (unlike
  assign-pathway's own PathwaysConfig, a real population is expected to
  have patients with no assigned module at all -- most people don't have
  chronic sinusitis -- so an empty/non-covering pool is a legitimate,
  common case, not a caller error). ALWAYS consumes exactly one
  `.nextDouble` from `rng` regardless of outcome, the same fixed-
  consumption law `assign-pathway` already establishes, for the
  identical reason (sim/ADR-0009's own rejected-alternative reasoning)."
  [^Random rng modules-config i]
  (let [draw (.nextDouble rng)
        explicit (first (filter #(= i (:patient-ordinal %)) modules-config))
        pool (filterv :weight modules-config)]
    (cond
      explicit (:module-id explicit)
      (seq pool) (weighted-pick pool draw :module-id)
      :else nil)))

(defn- pop-min
  "Removes and returns the earliest queue entry. Queue is a sorted-map
  keyed by [t seq-no] -- the seq-no tiebreak makes ordering total, so
  RNG consumption order (and thus output) is fully determined."
  [queue]
  (let [[k v] (first queue)]
    [k v (dissoc queue k)]))

(def config-keys
  "The canonical, documented list of every key `run`'s config map
  accepts (this def IS the documentation the M4 Task 0 plumbing-
  completeness test checks against -- a new key earns an entry here in
  the SAME change that teaches `run` to read it, never after).
  `ehrt.sim.run/run-command` must forward every one of these
  from its own opts through to `run` -- its own completeness test
  asserts the full set, not just today's known gaps, so a future key
  added here without a matching `run-command` forwarding update fails
  loudly instead of shipping CLI-invisible the way M3's `:pathways` did
  (caught only by the tools consumer loop, after the fact)."
  [:seed :patients :pathway :pathways :arrival-gap :warm-up-seconds
   :facility :providers :churn-profile :order-profiles :persona-config
   :modules :module-assignment :module-horizon-days :history :persons])

(def Persons
  "`run`'s ENGINE-FACING `:persons` value (ADR-0173 section 2(a), arc 3a
  part 3). Not the config-facing one: `ehrt.sim.run`'s own `:persons` is
  the small authored map the ADR tables (`{:count :years :identification
  :unhoused}`), and that namespace translates it into this -- exactly
  the two-layer treatment `:modules` already has, where the config side
  is names and the engine side is already-loaded closures. This
  namespace does no I/O and calls nothing outside itself; person events
  arrive as DATA.

    :population  [{:person-id .. :id-tag ..} ...] -- the POOL, in a
                 fixed order. Carried explicitly rather than derived
                 from `:events`, because a person with no events at all
                 in the horizon is still a person who can walk into an
                 ED, and deriving the pool from the stream would make
                 them unselectable.
    :personas    person-id -> that person's own t0 Persona. The seam the
                 `:patient` family loses (`compile-patient`'s 4-arity).
    :alive       person-id -> that person's own death instant. A1's
                 arrival-candidate filter's whole input. It is DATA and
                 not something derived from `:events`, and the reason is
                 ruling C1's ordering: a person BOUND to an arrival gets
                 the compiled trajectory's death instead of their own
                 drawn one, so the stream handed here has already had
                 those `:person-death` events removed. Filtering on the
                 stream would therefore filter on a fact the binding
                 itself produced. `ehrt.sim.run`'s own docstring carries
                 the two-pass resolution this key exists for.
    :events      the t-ascending person-event vector, verbatim.

  ABSENT ENTIRELY -- not nil, not an empty population -- is the
  byte-identical path, the same opt-in law `:pathways`,
  `:churn-profile` and `:module-assignment` already establish."
  [:map
   [:population [:vector [:map [:person-id :string] [:id-tag :int]]]]
   [:personas [:map-of :string sim-model/Persona]]
   [:alive [:map-of :string :int]]
   [:events [:vector [:map [:event :keyword] [:t :int] [:person-id :string]]]]])

(defn valid-persons?
  "Whether `run`'s engine-facing `:persons` value is well-formed. Result-
  not-throw: `run` returns `result/error :invalid-persons` rather than
  blowing up inside the pre-loop, the same guard-clause-at-entry shape
  `:invalid-seed` already has (sim/ADR-0116 R9)."
  [persons]
  (m/validate Persons persons))

(defn- select-person
  "A1: ONE uniform from the `:world` stream picks this arrival's person
  out of the persons ALIVE at that instant.

  `:world` is the family whose own definition names this draw --
  *\"arrivals, and every cross-patient decision\"* (`stream-family-tag`)
  -- and the candidate set is the POOL, which is shared, so no
  per-patient stream could own it.

  FIXED CONSUMPTION: the draw is taken whether or not the filter
  removed anyone, and whether or not it removed EVERYONE. An arrival
  with no living candidate binds to nobody and is an ordinary,
  person-free arrival -- the same dual path a run with no `:persons` key
  takes, reached one arrival at a time instead of run-wide.

  Without the filter an arrival could land on a person whose own
  `:person-death` already fired, which is a defect with a wire face: a
  registration for somebody the ground truth says is dead."
  [^Random rng population alive t]
  (let [candidates (filterv (fn [{:keys [person-id]}]
                              (let [d (get alive person-id)]
                                (or (nil? d) (> d t))))
                            population)
        draw (.nextDouble rng)]
    (when (seq candidates)
      (:person-id (nth candidates
                       (min (dec (count candidates))
                            (long (* draw (count candidates)))))))))

(defn- prelude
  "Everything `run` computes before its loop starts, in ONE place because
  two callers must agree on it EXACTLY.

  `run` is the first. The second is ADR-0173 ruling C1's own ordering
  problem: the compiled trajectory's death instant is a t0 parameter of
  the person process, so a caller has to know every arrival's compiled
  death -- and which PERSON each arrival bound to -- BEFORE the person
  stream it will hand back to `run` can be built. Both facts are
  pre-loop facts, and `person-plan` (below) is the export that hands
  them out. Reimplementing the pre-loop outside this namespace was the
  alternative, and it would have put the pinned `:patient`-family draw
  order in two places.

  THE PINNED `:patient`-FAMILY DRAW ORDER is `run`'s own docstring's,
  unchanged: `module-for`, `pathway-for`, `churn/inject`, then
  `compile-patient`, per arrival ordinal in ordinal order. The `:world`
  family's order is arrival gaps first, then -- only with `:persons` --
  one selection uniform per arrival ordinal.

  BYTE-IDENTICAL WITH `:persons` ABSENT, by construction rather than by
  care: `bindings` is all-nil, no selection draw is taken, every arrival
  is its own patient's first, `compile-patient` gets a nil persona and
  so draws its own, and no person step is seeded. Every expression below
  is then the expression that was there before arc 3a part 3."
  [{:keys [seed patients pathway pathways arrival-gap facility churn-profile
           persona-config modules module-assignment module-horizon-days history persons]
    ;; The SAME defaults `run`'s own parameter list declares, restated
    ;; here because `person-plan` is a second entry point and a caller
    ;; that omitted `:patients` would otherwise reach `(range nil)`.
    ;; `run` passes its own already-defaulted values through, so these
    ;; only ever apply on the `person-plan` path.
    :or {patients 1
         pathway sim-model/sample-admission-discharge
         arrival-gap 60
         facility sim-model/default-facility
         persona-config {}
         modules []
         module-horizon-days 90
         history false}}]
  (let [;; ADR-0171: FIVE families, not one shared Random. Each stream
        ;; is derived from the master seed and a stable id -- never from
        ;; a counter, never from construction order -- so a draw added
        ;; to one patient's pathway moves that patient and no one else.
        ;; `stream-family-tag`'s own docstring carries which draw site
        ;; belongs to which family; the two run-scoped families take
        ;; id-tag 0.
        world-rng (stream seed :world 0)
        ;; One stream per arrival ordinal, built up front. A patient's
        ;; stream must PERSIST across their own decides (a `Random` is
        ;; stateful, and a patient's draws are one continuing sequence),
        ;; so these are constructed once here rather than per decide.
        patient-rngs (mapv (fn [i] (stream seed :patient i)) (range patients))
        ;; Stagger arrivals: :arrival-gap is authored in MINUTES (same
        ;; carve-out as :delay's IR, and for the same calibration
        ;; reason -- see `run`'s docstring); the engine converts to
        ;; SECONDS here. Consume RNG in patient order (fixed). WORLD:
        ;; who arrives when is a fact about the run, not about any one
        ;; patient, and these draws happen before any decide, so their
        ;; count and order are fixed by `:patients` alone.
        arrivals (vec (reductions + 0 (repeatedly (dec patients)
                                                  #(* 60 (rand-int-in world-rng 0 arrival-gap)))))
        mrn-for (fn [i] (format "MRN%06d" (inc i)))
        pid-for (fn [i] (patient-id-for seed i))
        ;; ADR-0173 section 2(a), ruling A1. AFTER the arrival gaps, so a
        ;; run with no `:persons` leaves this stream exactly where it has
        ;; always stood by the time the loop's own `:world` draws start.
        bindings (if persons
                   (mapv (fn [t] (select-person world-rng (:population persons) (:alive persons) t))
                         arrivals)
                   (vec (repeat patients nil)))
        ;; person-id -> the ordinal that person FIRST arrived at. A person
        ;; selected twice is the point of having a pool: the second
        ;; arrival resolves to the patient the first one minted.
        events-by-person (group-by :person-id (:events persons))
        ;; ADR-0173 section 2(d) (arc 3a part 4): an arrival landing
        ;; inside an open `:identity-unavailable` window is a PLACEHOLDER
        ;; arrival -- an unresponsive John Doe, in the author's own
        ;; words. It mints its OWN patient at its OWN ordinal even when
        ;; the person behind it already has a record, because the whole
        ;; point is that nobody yet knows the two are the same somebody;
        ;; the identification flow is what joins them afterwards, by a
        ;; fill or by a merge.
        windows-of (fn [p] (person-fold/identification-windows (get events-by-person p)))
        arrival-windows (mapv (fn [i]
                                (when-let [p (nth bindings i)]
                                  (person-fold/window-open-at (windows-of p) (nth arrivals i))))
                              (range patients))
        placeholder? (fn [i] (some? (nth arrival-windows i)))
        ;; A PLACEHOLDER ARRIVAL IS NEVER A PERSON'S CANONICAL PATIENT,
        ;; so it is skipped here rather than claiming the person: an
        ;; identified arrival that follows it still mints their real
        ;; record, and their later demographic events land on that one.
        first-ordinal (persistent!
                       (reduce (fn [acc i]
                                 (let [p (nth bindings i)]
                                   (if (or (nil? p) (placeholder? i) (contains? acc p))
                                     acc
                                     (assoc! acc p i))))
                               (transient {}) (range (count bindings))))
        ;; A patient id is minted from an ARRIVAL ORDINAL and always has
        ;; been (`patient-id-for`), so a repeat arrival is resolved by
        ;; minting from the FIRST ordinal rather than by inventing a
        ;; second id space. A run whose pool is larger than its arrival
        ;; count mints exactly the ids it mints today.
        ;;
        ;; The `(get first-ordinal p i)` DEFAULT is part 4's: a
        ;; placeholder arrival is bound to a person who has no entry
        ;; here, and it owns itself.
        owner-ordinal (fn [i]
                        (if-let [p (nth bindings i)]
                          (if (placeholder? i) i (get first-ordinal p i))
                          i))
        first-arrival? (fn [i] (= i (owner-ordinal i)))
        pid-of (fn [i] (pid-for (owner-ordinal i)))
        firsts (filterv first-arrival? (range patients))
        ;; ADR-0173 section 2(b): what a bound person brings to their own
        ;; first arrival. Their pre-arrival events are folded onto their
        ;; t0 Persona rather than replayed as log events, because
        ;; `registered-is-every-patients-first-event` is structural.
        registration-of (fn [i]
                          (when-let [p (nth bindings i)]
                            (person-fold/registration (get (:personas persons) p)
                                                      (get events-by-person p)
                                                      (nth arrivals i))))
        registrations (mapv registration-of (range patients))
        ;; M3-adjacent: :pathways ABSENT entirely -- the pinned-fixture
        ;; path -- means every patient gets the same plain :pathway, no
        ;; assign-pathway call, no new draw (see `run`'s docstring).
        pathway-for (if pathways
                      (fn [i] (assign-pathway (nth patient-rngs i) pathways i))
                      (fn [_i] pathway))
        ;; InjectChurn (M2b): ONLY when :churn-profile is actually
        ;; present does this stage run at all -- absent, `steps-for` is
        ;; a no-op and consumes no RNG (see the docstring's fixture note).
        steps-for (if churn-profile
                    (fn [i] (:steps (churn/inject (pathway-for i) churn-profile (nth patient-rngs i))))
                    (fn [i] (:steps (pathway-for i))))
        ;; M5b Task 4: module-assignment is resolved eagerly, the SAME
        ;; point :pathways' own assign-pathway draw already occupies (one
        ;; more fixed-consumption draw per patient, ONLY when
        ;; :module-assignment is actually present -- absent entirely,
        ;; `module-for` draws nothing, byte-identical to pre-M5b (see
        ;; `run`'s own docstring)). ADR-0173 ruling C1 (2026-08-26): the
        ;; MODULE WALK itself (persona-dependent -- an unbounded number of
        ;; draws) no longer waits for :registered decide-time; it happens
        ;; in `compiled-patients` below, together with the persona sampling
        ;; it depends on, immediately after this stage's own draws.
        ;; ADR-0033 AR-2: `:modules` entries are closure-shaped -- keyed
        ;; by each closure's own `:root`, not a bare module's `:id`
        ;; (byte-identical for a singleton closure, whose :root IS the
        ;; module's own :id).
        closures-by-root (into {} (map (fn [c] [(:root c) c])) modules)
        module-for (if module-assignment
                     (fn [i] (get closures-by-root (assign-module (nth patient-rngs i) module-assignment i)))
                     (fn [_i] nil))
        ;; M4: :registered is prepended to EVERY patient's step queue,
        ;; ahead of whatever InjectChurn produced -- engine-internal,
        ;; never seen by InjectChurn's own applicability oracle (it
        ;; operates on `pathway-for`'s output, before this prepend), the
        ;; same "not authorable IR" treatment :result-followup gets. M5b:
        ;; carries this patient's own resolved closure (nil, absent
        ;; :module-assignment). ADR-0173 C1: the closure still rides the
        ;; step (the hand-built-world fallback in `decide :registered`
        ;; reads it), but the walk + compile it feeds now happens in
        ;; `compiled-patients`, below.
        ;;
        ;; ARC 3A PART 3, DISCLOSED: a REPEAT arrival prepends no
        ;; `:registered` -- ADR-0173 section 2(a)'s own rule, and what
        ;; keeps `registered-is-every-patients-first-event` true by
        ;; construction rather than by luck -- and queues no steps
        ;; either. The ADR reads "the second encounter's steps simply
        ;; continue that patient's log"; the tree refuses, and the
        ;; refusal is one-sided. A second admission for a patient whose
        ;; status is `:discharged` violates `check.clj`'s own
        ;; `admission-only-when-new`, which is this project's
        ;; single-encounter horizon (sim/ADR-0007 point 3) expressed as
        ;; an invariant. Lifting that horizon is not an arc-3a change.
        ;; What a repeat arrival IS for stands untouched: the person
        ;; resolves to the patient they already are, and every later
        ;; demographic event of theirs lands on that one patient rather
        ;; than on a stranger.
        registered-entry-for (fn [i]
                               ;; `module-for` FIRST, then `steps-for` --
                               ;; the exact evaluation order the previous
                               ;; `(into [{... (module-for i)}] (steps-for
                               ;; i))` had, preserved deliberately: it is
                               ;; the pinned pre-loop order of this
                               ;; patient's own `:patient` stream (see
                               ;; `run`'s docstring). The resolved closure
                               ;; is returned ALONGSIDE the steps so
                               ;; `compiled-patients` below can read it
                               ;; without a second `module-for` call,
                               ;; which would draw again.
                               (let [closure (module-for i)
                                     steps (steps-for i)]
                                 {:closure closure
                                  :steps (if (first-arrival? i)
                                           (into [{:type :registered :closure closure}] steps)
                                           [])}))
        initial-entries (into [] (map-indexed (fn [i _] (registered-entry-for i))) arrivals)
        ;; ADR-0173 ruling C1: the four run-config values `compile-patient`
        ;; reads, gathered here because the compile now happens BEFORE
        ;; `init-world` exists. Identical to what `decide :registered`
        ;; would have read off `world` at arrival -- none of the four is
        ;; ever re-`assoc`ed by the run loop (`compile-patient`'s own
        ;; t-independence table says so key by key).
        compile-inputs {:persona-config persona-config
                        :module-horizon-days module-horizon-days
                        :facility facility
                        :history history}
        ;; ADR-0173 ruling C1: every patient's Persona and compiled module
        ;; trajectory, drawn HERE rather than at that patient's arrival,
        ;; in arrival-ordinal order, immediately after `initial-entries`
        ;; took this run's pre-loop `:patient`-family draws. Each patient
        ;; reads its OWN stream, so the move changes WHEN the draw happens
        ;; and not WHERE in any stream it lands -- the byte-identity
        ;; argument, in one line.
        ;;
        ;; A patient whose `:registered` is never decided (an `:exhausted`
        ;; run ends the loop early) is compiled here anyway. That costs a
        ;; walk and moves no byte: the draws land on that patient's own
        ;; stream, which nothing else ever reads. ARC 3A PART 3 adds one
        ;; more of those: a REPEAT arrival's own compile is drawn and
        ;; DISCARDED (the walk happens once, at first registration), and
        ;; the draw is taken either way so consumption stays a function
        ;; of `:patients` alone.
        compiled (mapv (fn [i]
                         (compile-patient (nth patient-rngs i) compile-inputs
                                          (:closure (nth initial-entries i))
                                          (:persona (nth registrations i))))
                       (range patients))
        ;; --- ADR-0173 section 2(c), arc 3a part 4: THE TWO HOOKS -------
        ;;
        ;; Both hooks CREATE traffic `:patients` does not count, said
        ;; plainly because it changes what a config means: with
        ;; `:persons` present, `:patients` is the number of SELECTED
        ;; arrivals, and the run's patient count is that plus the
        ;; newborns plus the injury arrivals.
        ;;
        ;; A hook may only put an encounter on a patient who is
        ;; CLINICALLY IDLE -- whose whole step queue, authored and
        ;; compiled, is their `:registered` and nothing else. This is
        ;; the single-encounter horizon again (`admission-only-when-
        ;; new`, sim/ADR-0007 point 3), and it has to be answered
        ;; STATICALLY as well as at decide time: a birth encounter
        ;; landing on a patient whose own module admits them LATER would
        ;; leave that later admission illegal, and the decide-time guard
        ;; cannot see the future. `decide :person-encounter` carries the
        ;; runtime half, so a wrong answer here costs a skipped
        ;; encounter rather than a red invariant.
        clinically-idle? (fn [i]
                           (and (empty? (:steps (:compiled (nth compiled i))))
                                (every? #(= :registered (:type %))
                                        (:steps (nth initial-entries i)))))
        hook-events (if persons (person-fold/hooks (:events persons)) [])
        newborn-personas (if persons (person-fold/newborn-personas (:events persons)) {})
        persona-of (fn [p] (or (get (:personas persons) p) (get newborn-personas p)))
        bound-persons (into #{} (remove nil?) bindings)
        ;; The SAME alive filter ruling A1 puts on arrival selection, put
        ;; on hook minting for the same reason: a patient minted for
        ;; somebody the ground truth already says is dead is a defect
        ;; with a wire face. Half-open on the left, exactly as
        ;; `select-person` is -- a person whose death instant IS the
        ;; hook's instant is dead at it.
        alive-at? (fn [p t] (let [d (get (:alive persons) p)] (or (nil? d) (> d t))))
        ;; One pass over the hooks, in the order the stream already
        ;; stands in (t-ascending -- the component's own front-door
        ;; contract). Each hook contributes at most one ADDITIONAL
        ;; patient and at most one encounter on an EXISTING one.
        ;;
        ;;   :delivery            -> the newborn is an additional patient
        ;;                           whose first encounter is the birth
        ;;                           (`traffic-model.md`), plus the
        ;;                           parent's own delivery admission when
        ;;                           the parent is clinically idle.
        ;;   :occupational-injury -> an ED encounter on the injured
        ;;                           person's own patient; or, for a
        ;;                           person NO arrival ever bound, an
        ;;                           additional patient of their own
        ;;                           ("or mints one if this is their
        ;;                           first contact", section 2(c)).
        hook-plan
        (reduce
         (fn [{:keys [mints encounters] :as acc} h]
           (let [p (:person-id h)
                 own (get first-ordinal p)
                 after-own-arrival? (and own (> (:t h) (nth arrivals own)))
                 encounter (fn [pid cause reason ward-class stay]
                             {:patient-id pid :t (:t h)
                              :steps [{:type :person-encounter :t (:t h) :cause cause
                                       :reason reason :ward-class ward-class
                                       :stay-minutes stay :person-event-id (:event-id h)}]})]
             (case (:event h)
               :delivery
               (let [nb (:newborn-person-id h)
                     nb-persona (get newborn-personas nb)
                     acc (if (and nb-persona after-own-arrival? (alive-at? nb (:t h)))
                           (update acc :mints conj
                                   {:person-id nb :t (:t h) :cause :delivery
                                    :reason "Live birth"
                                    :ward-class :inpatient
                                    :stay-minutes delivery-stay-minutes
                                    :person-event-id (:event-id h)
                                    :mother-patient-id (pid-for own)})
                           acc)]
                 (if (and after-own-arrival? (clinically-idle? own))
                   (update acc :encounters conj
                           (encounter (pid-for own) :delivery "Delivery"
                                      :inpatient delivery-stay-minutes))
                   acc))
               ;; ADR-0173 section 2(d), as an ARRIVAL rather than as a
               ;; coincidence -- `person-fold/hook-kinds` carries the
               ;; measurement that forced it. The window itself is the
               ;; unidentified presentation, so it mints a patient whether
               ;; or not this person already has one: that is the whole
               ;; case, and the resolution below is what joins the two
               ;; records back together (a merge) or fills this one in (a
               ;; fill).
               :identity-unavailable
               (if (alive-at? p (:t h))
                 (update acc :mints conj
                         {:person-id p :t (:t h) :cause :identity-unavailable
                          :reason "Unidentified patient"
                          :ward-class :ed
                          :stay-minutes unidentified-stay-minutes
                          :person-event-id (:event-id h)
                          :placeholder-window (person-fold/window-open-at
                                               (windows-of p) (:t h))})
                 acc)
               :occupational-injury
               (cond
                 (and after-own-arrival? (clinically-idle? own))
                 (update acc :encounters conj
                         (encounter (pid-for own) :occupational-injury
                                    (str "Occupational injury: " (name (or (:injury-class h) :unspecified)))
                                    :ed injury-stay-minutes))
                 ;; No arrival ever bound this person, so this injury IS
                 ;; their first contact with the system.
                 (and (nil? own) (not (bound-persons p)) (some? (persona-of p))
                      (alive-at? p (:t h)))
                 (update acc :mints conj
                         {:person-id p :t (:t h) :cause :occupational-injury
                          :reason (str "Occupational injury: " (name (or (:injury-class h) :unspecified)))
                          :ward-class :ed
                          :stay-minutes injury-stay-minutes
                          :person-event-id (:event-id h)})
                 :else acc)
               acc)))
         {:mints [] :encounters []}
         hook-events)
        ;; ADDITIONAL PATIENTS take ordinals `(+ patients k)` in hook-`:t`
        ;; order, so `patient-id-for` and `mrn-for` stay pure functions
        ;; of an ordinal (ADR-0173 section 2(c)) and the newborn's own
        ;; `:patient` stream is `(stream seed :patient (+ patients k))`
        ;; -- order-free, and disjoint from every t0 arrival's. A person
        ;; who ALREADY has a patient never mints a second one here, so
        ;; the list carries no silent duplicates.
        mints (into [] (map-indexed (fn [k m] (assoc m :ordinal (+ patients k))))
                    (:mints hook-plan))
        hook-rngs (mapv (fn [{:keys [ordinal]}] (stream seed :patient ordinal)) mints)
        ;; A hook patient draws NOTHING: their Persona comes from the
        ;; person side and they walk no module, so `compile-patient`'s
        ;; 4-arity with a nil closure is a pure construction. Their
        ;; `:patient` stream is built all the same, so every patient in
        ;; the run has one.
        mint-registrations (mapv (fn [{:keys [person-id t]}]
                                   (person-fold/registration (persona-of person-id)
                                                             (get events-by-person person-id)
                                                             t))
                                 mints)
        mint-patients (into {}
                            (map-indexed
                             (fn [k {:keys [ordinal person-id mother-patient-id placeholder-window]}]
                               (let [reg (nth mint-registrations k)]
                                 [(pid-for ordinal)
                                  (cond-> {:persona (:persona reg) :compiled nil
                                           :residence (:residence reg)
                                           :person-id person-id}
                                    mother-patient-id (assoc :mother-patient-id mother-patient-id)
                                    placeholder-window
                                    (assoc :identity :placeholder
                                           :alias-name (:alias-name placeholder-window)
                                           :window-close-t (:until-t placeholder-window)))])))
                            mints)
        ;; --- ADR-0173 section 2(d): THE IDENTIFICATION FLOW ------------
        ;;
        ;; `base-owner` is who a person's canonical patient is BEFORE any
        ;; placeholder resolves: their first identified arrival, or --
        ;; for a newborn, or somebody whose first contact was an injury
        ;; -- the additional patient a hook minted for them.
        base-owner (merge
                    (into {} (for [[p i] first-ordinal]
                               [p {:patient-id (pid-for i) :first-ordinal i
                                   :active-mrn (mrn-for i) :t (nth arrivals i)}]))
                    ;; A PLACEHOLDER mint is not a person's canonical
                    ;; patient any more than a placeholder ARRIVAL is:
                    ;; nobody yet knows whose record it is. The fill
                    ;; promotion below is what can make it one.
                    (into {} (for [{:keys [person-id ordinal t placeholder-window]} mints
                                   :when (nil? placeholder-window)]
                               [person-id {:patient-id (pid-for ordinal) :first-ordinal ordinal
                                           :active-mrn (mrn-for ordinal) :t t}])))
        ;; EVERY placeholder record this run mints, from BOTH sources:
        ;; a t0 arrival that coincided with an open window (section
        ;; 2(d) as written), and a window that minted its own
        ;; unidentified arrival (the same section, met at the rate the
        ;; process actually produces -- `person-fold/hook-kinds` carries
        ;; the measurement).
        placeholders
        (into (into [] (for [i (range patients) :when (placeholder? i)]
                         {:patient-id (pid-for i) :ordinal i :person-id (nth bindings i)
                          :window (nth arrival-windows i) :t (nth arrivals i)}))
              (for [{:keys [ordinal person-id t placeholder-window]} mints
                    :when placeholder-window]
                {:patient-id (pid-for ordinal) :ordinal ordinal :person-id person-id
                 :window placeholder-window :t t}))
        ;; One resolution per placeholder record. A `:merge` with NO
        ;; survivor DEGENERATES TO A FILL -- named in section 2(d) rather
        ;; than left implicit, because silently emitting a merge with a
        ;; null survivor is the defect that sentence exists to prevent.
        resolutions
        (into []
              (for [{:keys [patient-id ordinal person-id window]} placeholders
                    :let [survivor (get base-owner person-id)
                          mergeable? (and (= :merge (:branch window))
                                          survivor
                                          (not= (:patient-id survivor) patient-id)
                                          (<= (:t survivor) (:until-t window)))]
                    :when (some? (:branch window))]
                {:patient-id patient-id :ordinal ordinal :person-id person-id :window window
                 :branch (if mergeable? :merge :fill)
                 :survivor-patient-id (:patient-id survivor)}))
        ;; A person whose ONLY contact was an unidentified arrival gets
        ;; that record as their canonical patient once it is filled --
        ;; section 2(d)'s "the placeholder patient becomes theirs and the
        ;; fold index is updated". The fold starts at the FILL instant
        ;; and not at the arrival: a demographic change reported for
        ;; somebody nobody had identified yet would be a claim the
        ;; modelled hospital was in no position to make.
        person-owner (reduce (fn [acc {:keys [patient-id ordinal person-id window branch]}]
                               (if (or (not= :fill branch) (contains? acc person-id))
                                 acc
                                 (assoc acc person-id
                                        {:patient-id patient-id :first-ordinal ordinal
                                         :active-mrn (mrn-for ordinal) :t (:until-t window)})))
                             base-owner resolutions)
        placeholders-by-person (reduce (fn [acc {:keys [person-id patient-id]}]
                                         (update acc person-id (fnil conj #{}) patient-id))
                                       {} placeholders)
        person-index (into {}
                           (for [[p e] person-owner]
                             [p (assoc (dissoc e :t)
                                       :placeholders (get placeholders-by-person p #{}))]))
        compiled-patients (merge
                           (into {} (for [i firsts]
                                      [(pid-for i)
                                       (cond-> (nth compiled i)
                                         (nth registrations i)
                                         (assoc :residence (:residence (nth registrations i)))
                                         (nth bindings i)
                                         (assoc :person-id (nth bindings i))
                                         (placeholder? i)
                                         (assoc :identity :placeholder
                                                :alias-name (:alias-name (nth arrival-windows i))
                                                :window-close-t (:until-t (nth arrival-windows i))))]))
                           mint-patients)
        ;; ADR-0173 section 2(b): THE FOLD IS A QUEUE-SEEDING PASS, not a
        ;; change to the main loop. A person event strictly after its
        ;; person's own first arrival becomes an ordinary queue entry at
        ;; its own `:t`, exactly the way `schedule-followup` already
        ;; inserts one at an absolute instant.
        ;;
        ;; PART 4 widened WHAT "their own first arrival" means, and
        ;; nothing else: it is now `person-owner`'s instant, which for a
        ;; newborn is their birth, for an injury-first patient their
        ;; injury, and for a filled placeholder the fill.
        person-steps (vec (for [[p {:keys [patient-id t]}] (sort-by key person-owner)
                                step (person-fold/steps-after (get events-by-person p) t)]
                            {:patient-id patient-id :steps [step] :t (:t step)}))
        ;; The additional patients' own arrivals, and the encounters both
        ;; hooks put on existing ones.
        mint-steps (vec (for [{:keys [ordinal t cause reason ward-class stay-minutes person-event-id]} mints]
                          {:patient-id (pid-for ordinal) :t t
                           :steps [{:type :registered :closure nil}
                                   {:type :person-encounter :t t :cause cause :reason reason
                                    :ward-class ward-class :stay-minutes stay-minutes
                                    :person-event-id person-event-id}]}))
        resolution-steps
        (vec (for [{:keys [patient-id person-id window branch survivor-patient-id]} resolutions]
               (if (= :merge branch)
                 {:patient-id survivor-patient-id :t (:until-t window)
                  :steps [{:type :identification-merge :t (:until-t window)
                           :placeholder-patient-id patient-id
                           :person-event-id (:resolution-event-id window)}]}
                 (let [reg (person-fold/registration (persona-of person-id)
                                                     (get events-by-person person-id)
                                                     (:until-t window))]
                   {:patient-id patient-id :t (:until-t window)
                    :steps [{:type :identity-fill :t (:until-t window)
                             :persona (:persona reg) :residence (:residence reg)
                             :person-event-id (:resolution-event-id window)}]}))))
        ;; ONE list, in a fixed category order -- the demographic fold,
        ;; then the hooks' own arrivals, then the encounters they put on
        ;; EXISTING patients, then the identification resolutions.
        ;; `[t seq-no]` is the queue's key, so `:t` decides everything
        ;; that matters and this order only settles ties,
        ;; deterministically.
        seeded-steps (-> person-steps
                         (into mint-steps)
                         (into (:encounters hook-plan))
                         (into resolution-steps))]
    {:world-rng world-rng
     :patient-rngs (into patient-rngs hook-rngs)
     :arrivals arrivals
     :mrn-for mrn-for
     :pid-for pid-for
     :bindings bindings
     :first-ordinal first-ordinal
     :firsts firsts
     :pid-of pid-of
     :person-index person-index
     :registrations registrations
     :initial-entries initial-entries
     :compiled compiled
     :compiled-patients compiled-patients
     :mints mints
     :placeholders placeholders
     :resolutions resolutions
     :hook-patient-ordinals (mapv :ordinal mints)
     :person-steps person-steps
     :seeded-steps seeded-steps}))

(defn person-plan
  "ADR-0173 ruling C1's own resolution, exported. Returns, for a config
  `run` would accept:

    {:bindings   [person-id-or-nil ...]  -- by arrival ordinal
     :deaths     {person-id <compiled death instant>}
     :person-index {person-id {:patient-id .. :first-ordinal ..
                               :active-mrn ..}}}

  WHY IT EXISTS. Ruling C1 gives the person process the COMPILED
  trajectory's death instant as a t0 parameter, keyed by person. The
  engine mints a patient id from an arrival ordinal, and the binding
  from person to ordinal is a `:world`-family draw taken at a pinned
  position inside this run -- so a caller cannot key those deaths
  without asking the engine which person each arrival bound to. This is
  that question, answered by the same code `run` itself uses, so the two
  cannot disagree.

  THE DEATH INSTANT IS EXACTLY COMPUTABLE, which is the part that could
  have failed. `compile-trajectory` emits its bridging delays as
  `{:type :delay :from g :to g}`, and ADR-0171 section 2(d) made
  `:from` = `:to` DRAW-FREE -- so every advance in a compiled prefix is
  a constant and the death instant is the arrival instant plus sixty
  times the gap-minutes standing before the first `:discharge` carrying
  `:disposition :expired`. No simulation needed. Had compiled delays
  been ranges, this would have been structurally impossible.

  A person who binds to no arrival, or whose bound arrival compiles no
  expiring discharge, is ABSENT from `:deaths` and keeps their own drawn
  death.

  ARC 3A PART 4, DISCLOSED: so is a person whose only patient a HOOK
  minted -- a newborn, an unidentified presentation, or a first-contact
  injury arrival. Those patients exist only because the person STREAM
  produced them, and the stream is what `:deaths` is computed to
  produce, so feeding their compiled deaths back would re-open ruling
  C1's cycle at a second point and need a THIRD pass. Two passes is what
  C1 resolved to and what `ehrt.sim.run/engine-persons` implements; a
  third is not designed. The conservatism runs the safe way, as the
  alive filter's does: such a person keeps their own DRAWN death, which
  the stream already respects by emitting nothing after it."
  [config]
  (let [{:keys [arrivals bindings compiled person-index first-ordinal]} (prelude config)
        death-of (fn [i]
                   (loop [steps (:steps (:compiled (nth compiled i))) t (nth arrivals i)]
                     (when-let [step (first steps)]
                       (cond
                         (and (= :discharge (:type step)) (= :expired (:disposition step))) t
                         (= :delay (:type step)) (recur (rest steps) (+ t (* 60 (long (:from step)))))
                         :else (recur (rest steps) t)))))]
    {:bindings bindings
     :person-index person-index
     :deaths (into {} (for [[p i] first-ordinal
                            :let [d (death-of i)]
                            :when d]
                        [p d]))}))

(defn run
  "Runs the simulation. config:
    :seed             long (required, non-negative -- sim/ADR-0116: a
                      negative :seed returns result/error :invalid-seed
                      rather than running)
    :patients         number of patients (default 1)
    :pathway          a pathway IR map (default sim-model/sample-admission-discharge)
    :pathways         M3-adjacent: sim-model/PathwaysConfig
                      -- a vector of weighted-pool ({:pathway :weight})
                      and/or explicit ({:patient-ordinal :pathway})
                      entries, `assign-pathway`'s own input. When
                      present, EVERY patient's pathway comes from this
                      (not :pathway, which is then ignored) and consumes
                      one additional RNG draw per patient (fixed
                      consumption, see `assign-pathway`'s docstring).
                      ABSENT ENTIRELY (not merely nil) -- the default --
                      means every patient gets the plain :pathway
                      config, exactly as before this option existed: no
                      new draw, byte-identical output, the reason the
                      pinned fixture (no :pathways key) survives this
                      milestone untouched.
    :arrival-gap      max MINUTES between successive patient arrivals
                      (default 60; actual gaps sampled from the seeded
                      RNG). Stays minutes, converted to seconds at the
                      point arrivals are computed -- symmetric to
                      :delay's own minutes-authored/seconds-internal
                      split (sim/ADR-0011), and empirically necessary: an
                      earlier draft left this in raw seconds while
                      :delay's dwell times (minutes*60) stayed
                      comparatively huge, so arrivals clustered far
                      faster than patients discharged and blew past
                      sim-model/default-facility's real usable capacity
                      (16 concurrent, not its nominal 18 -- Cardiology's
                      surge sits unused when every patient's home-ward
                      is Renal) at patient counts the property tests
                      already exercised. Keeping both minutes-scaled
                      preserves the calibration that made that headroom
                      real.
    :warm-up-seconds  events with :t less than this get :warm-up true
                      (default 0; sim/ADR-0011 -- the log stays complete,
                      no trimming here)
    :facility         facility config (default sim-model/default-facility)
    :providers        provider templates (default sim-model/default-provider-templates;
                       NPIs are generated from THIS run's seed -- sim/ADR-0007)
    :order-profiles   M3: ehrt.sim-engine.order-profiles/OrderProfiles map
                      (default order-profiles/default-profiles) -- :order
                      steps look up their :profile key here.
    :churn-profile    ehrt.sim-engine.churn/ChurnProfile map (default nil
                       -- churn OFF). M2b: when present, InjectChurn runs
                       ONCE PER PATIENT (in arrival-ordinal order, a fixed
                       point in the draw sequence) against THIS PATIENT's
                       own `:patient`-family stream (ADR-0171) -- churn's
                       rows are PATIENT-scoped, so injecting churn into
                       patient N's pathway reaches no other patient --
                       between building each patient's step queue and the
                       main loop. Absent entirely (not merely all-zero),
                       this stage never runs and consumes no RNG: the
                       reason a config with no :churn-profile key
                       reproduces byte-identical pre-M2b output (the
                       pinned fixture; churn is opt-in, sim/ADR-0009's
                       accept-and-record policy doesn't even apply here
                       since nothing about this path changed).
    :persona-config   M4: sim-model/persona's own config
                      map ({:age-min :age-max :payers-under-65
                      :payers-65-plus}, all optional -- see that
                      function's docstring for defaults). EVERY
                      patient's step queue is prepended with an
                      engine-internal `:registered` step (never
                      authorable IR, the same treatment
                      :result-followup already gets) that samples
                      exactly one persona per patient, always -- this is
                      NOT opt-in the way :churn-profile/:pathways are:
                      Persona is a landed part of Execute's own step
                      vocabulary now (docs/sim-theory.edn), so this
                      milestone's own fixture regeneration is expected
                      and documented (sim/ADR-0009 policy), not guarded
                      against the way M2b/M3's opt-in additions were.
    :modules          M5b, hard-switched at ADR-0033 (AR-2): a vector of
                      ALREADY-LOADED, CLOSURE-SHAPED entries
                      (ehrt.patient-simulator.gmf/load-closure's own :ok
                      payload -- {:root :modules :tables}, plus an
                      optional :initial-attributes a caller's own config
                      may attach, AR-1) -- this namespace does no file
                      I/O of its own, ehrt.sim.run's job, the same
                      layering :facility/:providers/:order-profiles
                      already follow. Looked up by :root against
                      :module-assignment's own resolution. A standalone
                      module with no CallSubmodule embeds as the
                      singleton closure ({:root id :modules {id module}
                      :tables {}}, ehrt.patient-simulator.gmf/singleton-
                      closure) -- draw-neutral and byte-neutral versus
                      the pre-ADR-0033 bare-module shape (AR-4).
    :module-assignment M5b: ehrt.patient-simulator.gmf/ModulesConfig -- the
                      SAME weighted-pool/explicit-ordinal shape
                      :pathways already establishes, `assign-module`'s
                      own input, ONE additional fixed RNG draw per
                      patient when present (assign-pathway's own fixed-
                      consumption, sim/ADR-0009, law, extended). ABSENT
                      ENTIRELY (not merely nil or []) -- the default --
                      means no patient ever walks a module: no draw, no
                      :closure carried on any :registered step, BYTE-
                      IDENTICAL to pre-M5b output (the pinned fixture;
                      the same opt-in law :pathways/:churn-profile
                      already establish). A patient's own compiled
                      module content is PREPENDED onto whatever
                      :pathway/:pathways already queued for them, never
                      a replacement -- both are just IR entering the SAME
                      queue (the pathway-ir union, docs/sim-theory.edn).
                      A caller wanting MODULE-ONLY patients must pass an
                      explicit empty pathway (`{:name ... :steps []}`):
                      the DEFAULT :pathway (sample-admission-discharge)
                      otherwise still runs AFTER the module's own compiled
                      content, and usually conflicts with it (the module's
                      own encounter already admitted/discharged this
                      patient-id; the default pathway assumes a fresh
                      :new patient) -- exactly the same compose-don't-
                      second-guess-the-author posture InjectChurn already
                      takes toward whatever pathway it's handed.
    :module-horizon-days M5b: how many days past this run's own
                      registration instant (sim-model/reference-today-
                      epoch-day) an assigned module's own walk runs
                      before stopping (`ehrt.patient-simulator.gmf-interpreter/
                      run-module`'s own optional `horizon-end-t` bound)
                      -- REQUIRED to be finite for any real module walk
                      to terminate (components/patient-simulator/docs/gmf-interpreter.md section 8
                      item 5's own finding: a vendored module may have
                      no Terminal state and no Guard to block on).
                      Ignored entirely when no patient has an assigned
                      module.
    :history          Wave H pre-roll (2026-08-04, ADR-0042 AR-3): opt-
                      in gate for the interpreter's own `:phase` mint
                      and CompileTrajectory's new uniform-drop/straddle-
                      inheritance path (`ehrt.patient-simulator.gmf-
                      interpreter/run-module`'s and `ehrt.patient-simulator.
                      compile-trajectory/compile-trajectory`'s own
                      docstrings have the mechanism). Default `false` --
                      absent, the pre-H `:pre-horizon`/`:registration-
                      facts` mechanics run unchanged, byte-identical to
                      every pre-H run (ADR-0042 AR-5's own pure-identity
                      bracket). Ignored entirely when no patient has an
                      assigned module (there is no history phase to gate
                      without one).

  THE PRE-LOOP `:patient`-FAMILY DRAW ORDER, PINNED (ADR-0173 ruling C1
  requires this to be stated here, because it is the one thing about the
  compile's new position that is a CHOICE rather than a consequence).
  For each arrival ordinal i, in ordinal order, before the run loop
  starts, patient i's own `:patient` stream is drawn from in exactly
  this order:

    1. `module-for`   -- `assign-module`, 1 draw, only with :module-assignment
    2. `pathway-for`  -- `assign-pathway`, 1 draw, only with :pathways
    3. `steps-for`    -- `churn/inject`, only with :churn-profile
    4. `compile-patient` -- the Persona (13 draws, 16 with demographic
       weights) and then the module walk (unbounded), only ever in that
       order and only for a patient with an assigned closure

  Steps 1-3 are `initial-entries`; step 4 is `compiled-patients`. Steps
  1-3 are exactly where they have always been. Step 4 is where ADR-0173
  C1 MOVED the persona draw and the module walk to, out of `decide
  :registered` -- a move in TIME only: patient i's stream is read by
  nothing but patient i's own decides, so the sequence each stream sees
  is unchanged and every run stays byte-identical (`compile-patient`'s
  own docstring carries the input-by-input argument;
  `bin/regression-oracle` carries the proof). Everything after step 4 is
  loop-time: `decide :delay` and `decide :order`, the only other two
  `:patient`-family draw sites in this namespace.

  Returns {:ground-truth [event ...] :state-history {patient-id [state
  ...]} :facility .. :providers [materialized-provider ...]}. The
  facility and MATERIALIZED providers (real NPIs, not just templates)
  are echoed back so a caller rendering this run's log
  (ehrt.sim-emit-hl7.emit-hl7/emit needs facility + providers for PV1)
  uses the EXACT config this run allocated against, not a fresh default
  that might not even share ward names. ground-truth is format-free,
  ordered by [t seq-no]; emitters consume it and test assertions target
  it directly (a first-class output, per the problem statement).
  state-history is DERIVED (sim/ADR-0008; sim-theory.md open question #3)
  -- (get state-history patient-id) is exactly (rest (reductions evolve
  (initial-patient patient-id mrn) (events for patient-id))), proven as
  a property test (engine-test/patient-state-is-a-fold-of-the-log)
  rather than assumed; the engine computes it as a byproduct of the
  loop below because decide needs live world state to make its next
  decision, not because it's a second source of truth."
  [{:keys [seed patients pathway pathways arrival-gap warm-up-seconds facility providers churn-profile order-profiles
           persona-config modules module-assignment module-horizon-days history persons]
    :or {patients 1
         pathway sim-model/sample-admission-discharge
         arrival-gap 60
         warm-up-seconds 0
         facility sim-model/default-facility
         providers sim-model/default-provider-templates
         order-profiles order-profiles/default-profiles
         persona-config {}
         modules []
         module-horizon-days 90
         history false}}]
  {:pre [(some? seed) (sim-model/valid? pathway)
         (or (nil? pathways) (sim-model/valid-pathways-config? pathways))]}
  (if (neg? seed)
    ;; sim/ADR-0116 (R9): the seed contract is non-negative longs -- a
    ;; negative seed reaches the invariant catalog unvalidated otherwise
    ;; (the engine-test flake investigation's own shrunk counterexample,
    ;; R8/R9), so this is a guard clause at entry, not a throw.
    (result/error :invalid-seed {:key :seed :value seed :expected "a non-negative integer"})
    ;; ADR-0173 section 2(a) (arc 3a part 3): `:persons` is the ONE key
    ;; here whose value is a whole data structure a caller built rather
    ;; than a scalar or a schema'd config, so it is checked at entry the
    ;; same result-not-throw way `:seed` is -- a malformed pool would
    ;; otherwise surface as a nil-pointer somewhere inside the pre-loop,
    ;; which is the failure mode sim/ADR-0116 R9 was written against.
    (if (and (some? persons) (not (valid-persons? persons)))
      (result/error :invalid-persons
                    {:key :persons
                     :expected (str "{:population [{:person-id <string> :id-tag <int>} ...] "
                                    ":personas {person-id Persona} :alive {person-id <int>} "
                                    ":events [{:event <keyword> :t <int> :person-id <string>} ...]}")})
    (let [;; ADR-0173 section 2(a) (arc 3a part 3): the pre-loop lives in
        ;; `prelude`, one function, because `ehrt.sim.run` must be able to
        ;; ask the same question `run` answers -- which person each
        ;; arrival bound to, and what that arrival compiled -- BEFORE the
        ;; run (`person-plan`). `:persons` ABSENT makes every one of its
        ;; person-aware branches the expression that was there before.
        {:keys [world-rng patient-rngs arrivals mrn-for pid-for firsts pid-of
                person-index initial-entries compiled-patients seeded-steps
                hook-patient-ordinals]}
        (prelude {:seed seed :patients patients :pathway pathway :pathways pathways
                  :arrival-gap arrival-gap :facility facility :churn-profile churn-profile
                  :persona-config persona-config :modules modules
                  :module-assignment module-assignment
                  :module-horizon-days module-horizon-days :history history
                  :persons persons})
        facility-rng (stream seed :facility 0)
        ;; Provider NPIs are generated from this run's seed (sim/ADR-0007),
        ;; drawn once up front -- before arrival staggering -- so
        ;; provider identity is as deterministic and as fixed-order as
        ;; everything else this RNG produces. FACILITY (ADR-0171 ruling
        ;; E1): it reads no patient state, so adding a provider template
        ;; no longer shifts arrival gaps or bed choices.
        materialized-providers (sim-model/materialize-providers facility-rng providers)
        ;; An arrival with no steps at all is not queued: that is a REPEAT
        ;; arrival of a person already registered (ADR-0173 section 2(a)),
        ;; and an empty queue entry would reach `decide` with a nil step.
        arrival-queue (into (sorted-map)
                            (for [i (range patients)
                                  :when (seq (:steps (nth initial-entries i)))]
                              [[(nth arrivals i) i]
                               {:patient-id (pid-of i) :steps (:steps (nth initial-entries i))}]))
        ;; ADR-0173 section 2(b): the fold, seeded into the SAME sorted
        ;; queue at each person event's own absolute `:t`. Seq numbers
        ;; start where the arrivals' end, so an event landing on an
        ;; arrival instant still sorts after that arrival's own
        ;; `:registered`, and the loop's own counter starts past all of
        ;; them.
        ;;
        ;; PART 4 puts the two hooks' own entries -- an additional
        ;; patient's whole arrival, and an encounter on an existing one
        ;; -- and the identification resolutions into this SAME list, at
        ;; their own `:t`. `prelude` fixes their order; the queue's
        ;; `[t seq-no]` key is what actually decides.
        init-queue (into arrival-queue
                         (map-indexed (fn [k {:keys [patient-id steps t]}]
                                        [[t (+ patients k)] {:patient-id patient-id :steps steps}]))
                         seeded-steps)
        seq-start (+ patients (count seeded-steps))
        init-world {:patients (into {} (for [i (concat firsts hook-patient-ordinals)]
                                         [(pid-for i) (initial-patient (pid-for i) (mrn-for i))]))
                    :facility facility
                    :providers materialized-providers
                    :order-profiles order-profiles
                    :persona-config persona-config
                    :module-horizon-days module-horizon-days
                    :history history
                    ;; Task 1 (M2b): cancel-family/transfer-in-error decide
                    ;; methods query the log directly for the event they
                    ;; reinstate from (docs/patient-state-model.md's
                    ;; shadow-field dissolution) -- a PERSISTENT mirror of
                    ;; the log-so-far, kept alongside (not instead of) the
                    ;; transient `ground-truth` accumulator below so decide
                    ;; can `nth`/`filter`/`keep-indexed` over it (transients
                    ;; aren't seqable). Always a prefix of the final log.
                    :ground-truth []
                    ;; ADR-0169 (arc 0): log index -> the state that
                    ;; event's subject was in immediately BEFORE it, for
                    ;; the two REINSTATABLE classes only (:transfer,
                    ;; :discharge). What `reinstated-state` reads instead
                    ;; of replaying the whole log per cancel. Written
                    ;; below, inside the same fold that produces `world'`,
                    ;; because that fold is where the pre-event state
                    ;; exists -- a second pass could not see it.
                    ;;
                    ;; The KEY's presence is what tells `reinstated-state`
                    ;; this world came from `run`; a hand-built world has
                    ;; no such key and keeps the replay path.
                    :reinstate-index {}
                    ;; ADR-0169 (arc 0), the ADR-0164 scans' own carrier:
                    ;; [opening-type patient-id citation] -> the LAST log
                    ;; index carrying that combination. A later occurrence
                    ;; overwrites an earlier one, which is precisely what
                    ;; the `last` in the scan it replaces meant. Read by
                    ;; `last-cited-index`.
                    :citation-index {}
                    ;; ADR-0173 ruling C1 (arc 3a): patient-id -> that
                    ;; patient's `compile-patient` result, drawn at RUN
                    ;; START (in `prelude`) rather than at the patient's
                    ;; own arrival. Read by `decide :registered`, which
                    ;; falls back to compiling in place when this KEY is
                    ;; absent -- the hand-built-world tolerance
                    ;; `:reinstate-index` and `:citation-index` already
                    ;; establish.
                    :compiled-patients compiled-patients
                    ;; ADR-0173 section 2(a) (arc 3a): person-id -> the
                    ;; patient that person resolves to:
                    ;;
                    ;;   {person-id {:patient-id .. :first-ordinal ..
                    ;;               :active-mrn ..}}
                    ;;
                    ;; The engine mints a patient id from an ARRIVAL
                    ;; ORDINAL (`patient-id-for`), so nothing in the tree
                    ;; can make a returning PERSON resolve to the same
                    ;; patient without a carried index. This is that
                    ;; index. EMPTY with no `:persons` key, and part 4
                    ;; grows each entry a `:placeholders` set when the
                    ;; identification flow lands. Same carried-state
                    ;; precedent -- and the same hand-built-world
                    ;; tolerance, on the KEY and never on a missing entry
                    ;; -- as `:reinstate-index`, `:citation-index` and
                    ;; `:compiled-patients` above.
                    :person-index person-index
                    ;; ADR-0173 section 2(d) (arc 3a part 4): patient-id
                    ;; -> the LOG INDEX of that patient's own
                    ;; `:registered` event, written in the same fold that
                    ;; produces `world'`. `decide :identity-fill` reads it
                    ;; for `:placeholder-event-id`, which is the ONE
                    ;; referential key this arc mints -- and it is read
                    ;; from a carried index rather than found by a scan,
                    ;; the shape ADR-0169 gave `:citation-index` for
                    ;; exactly this reason. Same hand-built-world
                    ;; tolerance, on the KEY and never on a missing
                    ;; entry.
                    :registration-index {}}
        mark-warmup (fn [ev] (assoc ev :warm-up (< (:t ev) warm-up-seconds)))
        ;; ADR-0171: what `decide` receives. The two run-scoped families
        ;; are fixed for the whole loop; `:patient` is swapped per queue
        ;; entry below off this map, keyed by patient-id because that is
        ;; what a queue entry carries (including the :order follow-ups
        ;; scheduled mid-run).
        base-streams {:world world-rng :facility facility-rng}
        ;; ARC 3A PART 3: keyed by the FIRST arrival ordinal, so a
        ;; patient's draws stay one continuing sequence (`run`'s own
        ;; reason for building `patient-rngs` up front). The consequence,
        ;; stated: a repeat arrival's own ordinal has a `:patient` stream
        ;; that is never used. It is still CONSTRUCTED -- the `mapv` in
        ;; `prelude` is unconditional and draw-free -- so nothing shifts.
        ;; PART 4: hook-minted patients take ordinals `(+ patients k)`
        ;; and their streams sit at those indices, appended by `prelude`.
        ;; They draw nothing -- no persona (it comes from the person
        ;; side), no module walk, and a `:from` = `:to` delay is
        ;; draw-free -- so each one is constructed and never read.
        streams-by-pid (into {} (for [i (concat firsts hook-patient-ordinals)]
                                  [(pid-for i) (nth patient-rngs i)]))
        final-result (fn [ground-truth state-history extra]
                       (merge {:ground-truth (persistent! ground-truth)
                               :state-history state-history
                               :facility facility
                               :providers materialized-providers}
                              extra))]
    ;; Past every seq-no the queue was SEEDED with -- the arrivals' own
    ;; ordinals and then one per queue-seeded person step -- so a
    ;; mid-run `schedule-followup` can never collide with a person
    ;; event's key. With no `:persons` this is `patients`, verbatim.
    (loop [queue init-queue
           seq-no seq-start
           world init-world
           ground-truth (transient [])
           state-history {}]
      (if (empty? queue)
        (final-result ground-truth state-history nil)
        (let [[[t _] {:keys [patient-id steps]} queue'] (pop-min queue)
              [step & remaining] steps]
          (if (= :merged (get-in world [:patients patient-id :status]))
            ;; M2b: a merge (decided while processing a DIFFERENT
            ;; patient's step -- the survivor's) can end this patient-
            ;; id's stream mid-pathway, asynchronously to their own
            ;; queue. "The merged patient-id's stream ends with a
            ;; terminal merged-into event" (docs/patient-state-model.md)
            ;; means exactly this: their own remaining queued steps are
            ;; abandoned here, never decided, never emitting further
            ;; events -- the run loop's own enforcement of
            ;; no-events-after-merged-terminal, not just a check.clj
            ;; invariant asserted after the fact.
            (recur queue' seq-no world ground-truth state-history)
            (let [{:keys [events advance exhausted schedule-followup prepend-steps]}
                  (decide (assoc base-streams :patient (get streams-by-pid patient-id))
                          t world patient-id step)]
              ;; A :rejected decide outcome (an illegal cancel/bed-swap/
              ;; merge -- Task 1's validity-table enforcement) is NOT a
              ;; run-halting condition, unlike :exhausted: it means THIS
              ;; one step doesn't happen (already :events [] :advance 0),
              ;; not that the simulation can no longer proceed at all.
              ;; This matters for InjectChurn (M2b): a churned step can be
              ;; legal when INSERTED (per the applicability oracle, a
              ;; static analysis) yet collide with live world state by
              ;; the time it actually executes (e.g. a bed a cancel-
              ;; discharge would reinstate into has since been reclaimed
              ;; by someone else's admission) -- that step is simply
              ;; skipped, and the patient's OWN remaining steps proceed
              ;; normally. check.clj remains the independent safety net
              ;; for any log, authored or generated.
              (cond
                exhausted (final-result ground-truth state-history {:exhausted exhausted})
                :else
            (let [events (mapv mark-warmup events)
                  base-idx (count (:ground-truth world))
                  ;; ADR-0169: the patient-state fold and the reinstate
                  ;; index are built in ONE pass, because the index's
                  ;; value IS this fold's accumulator one step early --
                  ;; `w` before `ev` is applied. A `:discharge` decide can
                  ;; emit two events (the discharge, then a bed-ready
                  ;; :transfer for a DIFFERENT patient), so the subject is
                  ;; read off each event rather than assumed to be
                  ;; `patient-id`, and the state is captured per event
                  ;; rather than once for the batch.
                  [world' reinstate' citations' registrations']
                  (reduce (fn [[w ridx cidx gidx] [offset ev]]
                            (let [idx (+ base-idx offset)
                                  subject (:patient-id (first (:participants ev)))
                                  ridx' (if (reinstatable-event-types (:event ev))
                                          (assoc ridx idx (get-in w [:patients subject]))
                                          ridx)
                                  cidx' (if (and (cited-opening-event-types (:event ev))
                                                 (some? (:citation ev)))
                                          (reduce (fn [ci {:keys [patient-id]}]
                                                    (assoc ci [(:event ev) patient-id (:citation ev)] idx))
                                                  cidx (:participants ev))
                                          cidx)
                                  ;; ADR-0173 section 2(d): one more index
                                  ;; off the SAME fold, for the same
                                  ;; reason the two above are here -- the
                                  ;; log index exists at this point and
                                  ;; nowhere later.
                                  gidx' (if (= :registered (:event ev))
                                          (assoc gidx subject idx)
                                          gidx)]
                              [(reduce (fn [w2 {:keys [patient-id]}]
                                         (update-in w2 [:patients patient-id] evolve ev))
                                       w (:participants ev))
                               ridx' cidx' gidx']))
                          [world (:reinstate-index world) (:citation-index world)
                           (:registration-index world)]
                          (map-indexed vector events))
                  world'' (assoc world'
                                 :ground-truth (into (:ground-truth world) events)
                                 :reinstate-index reinstate'
                                 :citation-index citations'
                                 :registration-index registrations')
                  ground-truth' (reduce conj! ground-truth events)
                  state-history' (reduce (fn [sh ev]
                                            (reduce (fn [sh2 {:keys [patient-id]}]
                                                      (update sh2 patient-id (fnil conj [])
                                                              (get-in world' [:patients patient-id])))
                                                    sh (:participants ev)))
                                          state-history events)
                  ;; M3: :order's decide may ask for a follow-up queue
                  ;; entry (the auto-paired :result -- see engine's :order
                  ;; docstring for why it rides the REAL queue instead of
                  ;; being spliced into :events directly). Scheduled at
                  ;; its own [t seq-no], same as any other event -- this
                  ;; is what keeps ground-truth in true global time order
                  ;; even though the result's CONTENT was fully decided
                  ;; back when the order was placed.
                  [queue'' seq-no'] (if schedule-followup
                                      [(assoc queue' [(:t schedule-followup) seq-no]
                                             (select-keys schedule-followup [:patient-id :steps]))
                                       (inc seq-no)]
                                      [queue' seq-no])
                  ;; M5b Task 4: :registered's own decide call may ask for
                  ;; compiled module steps to run BEFORE whatever was
                  ;; already queued (this patient's own authored pathway,
                  ;; if any) -- spliced onto the FRONT of `remaining`,
                  ;; never replacing it: module-compiled and authored IR
                  ;; are both just steps entering the SAME queue (the
                  ;; pathway-ir union, docs/sim-theory.edn), not two
                  ;; competing sources.
                  remaining' (into (vec prepend-steps) remaining)]
              (if (seq remaining')
                (recur (assoc queue'' [(+ t advance) seq-no'] {:patient-id patient-id :steps remaining'})
                       (inc seq-no') world'' ground-truth' state-history')
                (recur queue'' seq-no' world'' ground-truth' state-history')))))))))))))
