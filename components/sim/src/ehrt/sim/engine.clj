(ns ehrt.sim.engine
  "The discrete-event simulation core: a priority queue of pending
  events ordered by simulated time, a world of patient states (plus,
  from Milestone M1 on, the static facility/provider config decide
  needs to read), and the decide/evolve pair (`sim/ADR-0008`) that replaces
  a single fused transition function. Architecture mined from Google's
  Simulated Hospital (pkg/state WrappedQueue + pkg/hospital
  RunNextEventIfDue).

  Event-sourcing doctrine (`sim/ADR-0008`): the ground-truth log is the only
  primitive. `decide` (rng, t, world, patient-id, step) -> {:events
  :advance} consults the current world (every patient's state so far,
  plus facility/provider config -- read-only) and the run's single RNG
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

  Determinism doctrine: ALL randomness flows from the single
  java.util.Random seeded in `run`. No other entropy source (wall
  clock, hash ordering, nondeterministic seq realization) may
  influence output. Same config + seed => identical output, byte for
  byte once serialized -- WITHIN a version; see `sim/ADR-0009`
  for the cross-version seed-stability policy Milestone M1's new RNG
  draws (bed choice, attending sampling) triggered, and M2a's identity/
  time changes triggered again (documented once, per the M2a session
  plan, not per-commit).

  Step vocabulary: v0's :admission/:delay/:discharge, plus Milestone
  M1's :transfer (docs/operational-models.md's allocation ladder).
  Emission to HL7v2 is a separate namespace consuming the ground-truth
  log -- events here are format-free."
  (:require [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-trajectory.interface :as sim-trajectory]
            [ehrt.sim.churn :as churn]
            [ehrt.sim.order-profiles :as order-profiles]
            [malli.core :as m])
  (:import [java.util Random]))

;; --- M6 Task 1: the clinical-content accumulator -------------------------
;; EmitState's own snapshot-at-instant law (docs/sim-theory.edn) means the
;; FHIR emitter touches NOTHING but folded state, never the log directly
;; -- so Condition/Observation/MedicationRequest content has to actually
;; LAND in the fold, the same way :location/:persona already do, rather
;; than staying a log-only fact only ehrt.sim.check reads via
;; `replay`. Each record below is intentionally the smallest shape that
;; carries what the FHIR builders need, not a re-derivation of the whole
;; originating event.

(def ConditionRecord
  "One condition, folded from a compiled encounter step's own
  :conditions annotations (sim-model/ConditionAnnotation,
  ehrt.sim-trajectory.compile-trajectory's own annotate-condition). Scope
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
  "One observation -- either a GMF `:observation` event (:codes/:value/
  :unit only) or a single analyte flattened out of a `:result-available`
  event's own :results (order-profiles' richer shape: adds
  :reference-range/:interpretation, the computed abnormal flag).
  Optional fields absent rather than nil for the plain-GMF case -- 'no
  invented fields' (M6 Task 1)."
  [:map
   [:codes [:vector sim-model/Concept]]
   [:t :int]
   [:value {:optional true} number?]
   [:unit {:optional true} :string]
   [:reference-range {:optional true} [:map [:low number?] [:high number?]]]
   [:interpretation {:optional true} [:enum :normal :low :high]]])

(def MedicationOrderRecord
  "One medication order, folded from :medication-order and closed by a
  citation-matching :medication-end (the SAME position-independent,
  citation-based resolution ehrt.sim-trajectory.compile-trajectory already
  uses throughout, extended to fold time instead of compile time)."
  [:map
   [:codes {:optional true} [:maybe [:vector sim-model/Concept]]]
   [:citation sim-model/Citation]
   [:ordered-t :int]
   [:status [:enum :active :completed]]
   [:ended-t {:optional true} :int]])

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
   [:status [:enum :new :admitted :discharged :merged]]
   [:class {:optional true} [:enum :inpatient :emergency :outpatient
                              :preadmit :recurring :obstetrics]]
   [:home-ward {:optional true} [:maybe :string]]
   [:location {:optional true} [:maybe [:map
                                         [:ward :string]
                                         [:bed :string]
                                         [:placement [:enum :licensed :surge]]]]]
   [:attending {:optional true} [:maybe :string]]
   [:persona {:optional true} [:maybe sim-model/Persona]]
   [:admitted-at {:optional true} [:maybe :int]]
   [:discharged-at {:optional true} [:maybe :int]]
   [:conditions {:optional true} [:vector ConditionRecord]]
   [:observations {:optional true} [:vector ObservationRecord]]
   [:medication-orders {:optional true} [:vector MedicationOrderRecord]]
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

(defn- mix64
  "A fixed, fully-specified 64-bit mix of two longs (splitmix64-style
  constants) -- deliberately NOT an RNG draw. See `patient-id-for`."
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
  .. :providers ..} -- read-only) and the seeded RNG to make stochastic
  and cross-patient choices; returns {:events [<ground-truth
  event>...] :advance <seconds>}. NEVER returns or implies a new
  patient state -- state changes only by folding the returned events
  through `evolve` (sim/ADR-0008). Pure given the RNG (the RNG is the only
  stateful argument, and its consumption order is fixed by the
  deterministic event ordering)."
  (fn [_rng _t _world _patient-id step] (:type step)))

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
;; stage's contract ("samples once, from the run's single seeded RNG, in
;; fixed order") is satisfied by this event exactly, not merely gestured at.

;; M5b Task 4: persona -> run-module -> CompileTrajectory -> IR, the ACTUAL
;; RunModules/CompileTrajectory stage boundary, folded into THIS SAME
;; engine-internal step for the same reason Persona itself was (M4's own
;; documented theory-flip note): a patient's assigned module is consumed at
;; the same init moment this event already owns. `step`'s own :module (set,
;; per patient, by `run`'s eager `registered-steps-for` -- mirroring how
;; :pathways' own per-patient resolution already happens eagerly, ahead of
;; the main loop) is nil for the (default, opt-in) case of no module
;; assignment -- byte-identical to pre-M5b :registered output, no new draw,
;; the same "absent means untouched" law :pathways/:churn-profile already
;; establish. `registration-t` is `sim-model/reference-today-epoch-day` --
;; components/sim-trajectory/docs/gmf-interpreter.md section 3's own "that patient's own :registered
;; event time," expressed in the SAME calendar anchor every persona's own
;; DOB is already computed against (persona.clj's own docstring note).
;; `:module-horizon-days` bounds the walk (`run-module`'s own optional
;; `horizon-end-t`) -- REQUIRED for any real vendored module (M5b's own
;; finding, components/sim-trajectory/docs/gmf-interpreter.md section 8 item 5: a module with no
;; Terminal state and no Guard to block on would otherwise run until the
;; interpreter's own max-steps backstop throws).

(defmethod decide :registered
  [rng t world patient-id {:keys [module]}]
  ;; :active-mrn is REQUIRED here, not merely conventional: :registered
  ;; is now every patient's FIRST event, and `replay` (below) bootstraps
  ;; a never-yet-seen participant's initial state via `(initial-patient
  ;; pid (:active-mrn event))` off the FIRST event naming them -- every
  ;; other event type already carries :active-mrn for exactly this
  ;; reason (a convention this event must honor, not just a rendering
  ;; nicety), or `replay`'s own bootstrap (and every check.clj invariant
  ;; built on it) silently seeds `:mrns #{nil}`.
  (let [persona (sim-model/persona rng (:persona-config world))
        compiled (when module
                   (let [reg-t (sim-model/reference-today-epoch-day)
                         horizon-end-t (+ reg-t (:module-horizon-days world))
                         {:keys [trajectory]} (sim-trajectory/run-module module rng persona reg-t horizon-end-t)]
                     (sim-trajectory/compile-trajectory trajectory (:facility world) reg-t)))]
    {:events [(cond-> {:event :registered :t t
                       :active-mrn (get-in world [:patients patient-id :active-mrn])
                       :persona persona
                       :participants [{:patient-id patient-id :role :subject}]}
                (seq (:registration-facts compiled)) (assoc :pre-horizon-facts (:registration-facts compiled)))]
     :advance 0
     :prepend-steps (:steps compiled)}))

(defn- citation-fields
  "M5b: :citation/:conditions ride through onto the ground-truth event
  ONLY when the compiled step actually carries them (glass-box
  traceability, components/sim-trajectory/docs/gmf-interpreter.md section 6 obligations 1/3) --
  `select-keys` + a nil-dropping `into {}` keeps a hand-authored step
  (never compiled, carries neither key) producing the EXACT same event
  shape it always has, byte-identical, no perturbation for any pathway
  that predates M5b."
  [step]
  (into {} (filter val) (select-keys step [:citation :conditions])))

(defmethod decide :admission
  [rng t world patient-id {:keys [location reason force-placement] :as step}]
  (let [{:keys [facility providers patients]} world
        board (sim-model/occupancy-board patients)
        alloc (sim-model/allocate rng facility board location force-placement)]
    (if (:exhausted alloc)
      (exhausted-outcome patient-id location facility board)
      (let [ward-id (:id (sim-model/ward-by-name facility (:home-ward alloc)))
            attending (sim-model/choose-attending rng providers ward-id)
            active-mrn (get-in patients [patient-id :active-mrn])]
        {:events [(merge {:event :admission :t t :active-mrn active-mrn :reason reason :attending attending
                          :participants [{:patient-id patient-id :role :subject}]}
                         alloc (citation-fields step))]
         :advance 0}))))

(defmethod decide :delay
  [rng _t _world _patient-id {:keys [from to]}]
  ;; :from/:to are authored in MINUTES (pathway.clj IR, unchanged --
  ;; sim/ADR-0011 decision 1's authoring-ergonomics carve-out); the engine
  ;; converts to SECONDS here, the one place a minute-denominated draw
  ;; becomes a clock advance.
  {:events []
   :advance (* 60 (rand-int-in rng from to))})

(defmethod decide :transfer
  [rng t world patient-id {:keys [location force-placement]}]
  (let [{:keys [facility patients]} world
        board (sim-model/occupancy-board patients)
        patient (get patients patient-id)
        alloc (sim-model/allocate rng facility board location force-placement)]
    (if (:exhausted alloc)
      (exhausted-outcome patient-id location facility board)
      {:events [(merge {:event :transfer :t t :active-mrn (:active-mrn patient) :from (:location patient)
                        :attending (:attending patient) :bed-ready false
                        :participants [{:patient-id patient-id :role :subject}]}
                       alloc)]
       :advance 0})))

(defmethod decide :discharge
  [_rng t world patient-id step]
  (let [patient (get-in world [:patients patient-id])
        discharge-event (merge {:event :discharge :t t :active-mrn (:active-mrn patient)
                                 :location (:location patient) :attending (:attending patient)
                                 :participants [{:patient-id patient-id :role :subject}]}
                                (citation-fields step))
        vacated-ward (get-in patient [:location :ward])
        vacated-location (:location patient)
        waiting-id (->> (:patients world)
                        (remove (fn [[pid _]] (= pid patient-id)))
                        (filter (fn [[_ p]] (and (= :admitted (:status p))
                                                  (not= (:home-ward p) (get-in p [:location :ward]))
                                                  (= vacated-ward (:home-ward p)))))
                        (sort-by (fn [[pid p]] [(:admitted-at p) pid]))
                        ffirst)]
    {:events (cond-> [discharge-event]
               waiting-id
               (conj {:event :transfer :t t
                      :active-mrn (:active-mrn (get-in world [:patients waiting-id]))
                      :from (:location (get-in world [:patients waiting-id]))
                      :attending (:attending (get-in world [:patients waiting-id]))
                      :home-ward (get-in world [:patients waiting-id :home-ward])
                      :location vacated-location
                      :placement (:placement vacated-location)
                      :forced false
                      :bed-ready true
                      :participants [{:patient-id waiting-id :role :subject}]}))
     :advance 0}))

;; --- M2b: churn family (docs/patient-state-model.md's event-validity
;; table; docs/event-sourcing.md's shadow-field dissolution) ---------------

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
  [_rng t world patient-id step]
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
  [rng t world patient-id {:keys [location force-placement]}]
  (let [{:keys [facility patients ground-truth]} world
        board (sim-model/occupancy-board patients)
        patient (get patients patient-id)
        alloc (sim-model/allocate rng facility board location force-placement)]
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
  [rng t world patient-id {:keys [with] :as step}]
  (let [{:keys [patients]} world
        self (get patients patient-id)
        eligible (->> patients
                     (remove (fn [[pid _]] (= pid patient-id)))
                     (filter (fn [[_ p]] (and (= :admitted (:status p)) (some? (:location p)))))
                     (mapv first))
        peer-id (cond
                  with with
                  (seq eligible) (uniform-choice rng eligible)
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
  [rng t world patient-id {:keys [with] :as step}]
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
                    (seq eligible) (uniform-choice rng eligible)
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
  [rng t world patient-id {:keys [profile]}]
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
                          {:concept (:concept analyte) :units (:units analyte) :value value
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
  [_rng _t _world _patient-id {:keys [result-event]}]
  {:events [result-event] :advance 0})

;; --- M5b: :outpatient-visit / :outpatient-visit-end (components/sim-trajectory/docs/gmf-interpreter.md
;; section 4's sketch, items 5-7) --------------------------------------------

(defmethod decide :outpatient-visit
  [rng t world patient-id {:keys [reason] :as step}]
  ;; Item 5: NO sim-model/allocate call -- an outpatient encounter occupies
  ;; no bed, so there is no ladder to consult. Still gets an attending
  ;; (real ambulatory visits have a treating provider) -- chosen uniformly
  ;; among ALL providers, not ward-filtered (there is no ward), the same
  ;; "no ward-scoping concept, choose uniformly among everyone" treatment
  ;; bed-swap/merge's own peer selection already establishes.
  (let [{:keys [providers patients]} world
        patient (get patients patient-id)
        attending (:id (uniform-choice rng providers))]
    {:events [(merge {:event :outpatient-visit :t t :active-mrn (:active-mrn patient)
                      :reason reason :attending attending
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defmethod decide :outpatient-visit-end
  [_rng t world patient-id step]
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
  [_rng t world patient-id {:keys [codes] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :procedure :t t :active-mrn (:active-mrn patient) :codes codes
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defmethod decide :observation
  [_rng t world patient-id {:keys [codes value unit] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :observation :t t :active-mrn (:active-mrn patient) :codes codes}
                     (when (some? value) {:value value})
                     (when unit {:unit unit})
                     {:participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defmethod decide :medication-order
  [_rng t world patient-id {:keys [codes] :as step}]
  (let [patient (get-in world [:patients patient-id])]
    {:events [(merge {:event :medication-order :t t :active-mrn (:active-mrn patient) :codes codes
                      :participants [{:patient-id patient-id :role :subject}]}
                     (citation-fields step))]
     :advance 0}))

(defmethod decide :medication-end
  [_rng t world patient-id {:keys [order-citation] :as step}]
  ;; Resolved by CITATION match against ground-truth, never a pathway-
  ;; position index (pathway.clj's own :medication-end docstring) -- the
  ;; same glass-box, position-independent resolution ConditionEnd's own
  ;; trajectory-level :references already models, one level down at the
  ;; ground-truth log.
  (let [{:keys [ground-truth patients]} world
        patient (get patients patient-id)
        order-event-id (when order-citation
                         (last (keep-indexed (fn [i ev] (when (and (= :medication-order (:event ev))
                                                                   (= order-citation (:citation ev)))
                                                          i))
                                             ground-truth)))]
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
  [patient {:keys [persona]}]
  (assoc patient :persona persona))

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
  [patient {:keys [t]}]
  (assoc patient :status :discharged :location nil :discharged-at t))

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
  ;; wire (ehrt.sim.emit-hl7's own single-subject-message) always
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
          (mapv (fn [{:keys [concept units value reference-range abnormal-flag]}]
                  {:codes [concept] :t t :value value :unit units
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
  [patient {:keys [t codes value unit]}]
  (update patient :observations (fnil conj [])
          (cond-> {:codes codes :t t} (some? value) (assoc :value value) unit (assoc :unit unit))))

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

(defmethod decide :cancel-transfer
  [_rng t world patient-id step]
  (let [ground-truth (:ground-truth world)
        idx (last-uncancelled-index ground-truth patient-id :transfer :cancel-transfer)]
    (if (nil? idx)
      (rejected-outcome :illegal-cancel-transfer patient-id t step nil)
      (let [patient (get-in world [:patients patient-id])
            {:keys [home-ward location]} (:before (nth (replay ground-truth) idx))]
        (if (bed-reoccupied-by-someone-else? world patient-id location)
          (rejected-outcome :illegal-cancel-transfer-bed-reoccupied patient-id t step {:location location})
          {:events [{:event :cancel-transfer :t t :active-mrn (:active-mrn patient)
                     :cancels-event-id idx :home-ward home-ward :location location
                     :participants [{:patient-id patient-id :role :subject}]}]
           :advance 0})))))

(defmethod decide :cancel-discharge
  [_rng t world patient-id step]
  (let [ground-truth (:ground-truth world)
        idx (last-uncancelled-index ground-truth patient-id :discharge :cancel-discharge)]
    (if (nil? idx)
      (rejected-outcome :illegal-cancel-discharge patient-id t step nil)
      (let [patient (get-in world [:patients patient-id])
            {:keys [home-ward location attending]} (:before (nth (replay ground-truth) idx))]
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
  ehrt.sim-trajectory.gmf/ModulesConfig -- the same pool shape, two resource
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

;; --- M5b: per-patient module assignment (ehrt.sim-trajectory.gmf/
;; ModulesConfig) -- the SAME shape/law as assign-pathway just above,
;; extended to modules per components/sim-trajectory/docs/gmf-interpreter.md's own Task 4 (module
;; assignment composes with :pathways -- both just IR entering the union).

(defn assign-module
  "Resolves the module id `modules-config` (ehrt.sim-trajectory.gmf/
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
   :modules :module-assignment :module-horizon-days])

(defn run
  "Runs the simulation. config:
    :seed             long (required)
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
    :order-profiles   M3: ehrt.sim.order-profiles/OrderProfiles map
                      (default order-profiles/default-profiles) -- :order
                      steps look up their :profile key here.
    :churn-profile    ehrt.sim.churn/ChurnProfile map (default nil
                       -- churn OFF). M2b: when present, InjectChurn runs
                       ONCE PER PATIENT (in arrival-ordinal order, a fixed
                       point in the draw sequence) against THIS run's own
                       `rng` -- not a derived/isolated stream, same
                       reasoning sim/ADR-0009 gives for NPI generation --
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
    :modules          M5b: a vector of ALREADY-LOADED GMF module maps
                      (ehrt.sim-trajectory.gmf/load-module's own :payload
                      shape -- this namespace does no file I/O of its
                      own, ehrt.sim.run's job, the same layering
                      :facility/:providers/:order-profiles already
                      follow). Looked up by :id against
                      :module-assignment's own resolution.
    :module-assignment M5b: ehrt.sim-trajectory.gmf/ModulesConfig -- the
                      SAME weighted-pool/explicit-ordinal shape
                      :pathways already establishes, `assign-module`'s
                      own input, ONE additional fixed RNG draw per
                      patient when present (assign-pathway's own fixed-
                      consumption, sim/ADR-0009, law, extended). ABSENT
                      ENTIRELY (not merely nil or []) -- the default --
                      means no patient ever walks a module: no draw, no
                      :module carried on any :registered step, BYTE-
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
                      before stopping (`ehrt.sim-trajectory.gmf-interpreter/
                      run-module`'s own optional `horizon-end-t` bound)
                      -- REQUIRED to be finite for any real module walk
                      to terminate (components/sim-trajectory/docs/gmf-interpreter.md section 8
                      item 5's own finding: a vendored module may have
                      no Terminal state and no Guard to block on).
                      Ignored entirely when no patient has an assigned
                      module.

  Returns {:ground-truth [event ...] :state-history {patient-id [state
  ...]} :facility .. :providers [materialized-provider ...]}. The
  facility and MATERIALIZED providers (real NPIs, not just templates)
  are echoed back so a caller rendering this run's log
  (ehrt.sim.emit-hl7/emit needs facility + providers for PV1)
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
           persona-config modules module-assignment module-horizon-days]
    :or {patients 1
         pathway sim-model/sample-admission-discharge
         arrival-gap 60
         warm-up-seconds 0
         facility sim-model/default-facility
         providers sim-model/default-provider-templates
         order-profiles order-profiles/default-profiles
         persona-config {}
         modules []
         module-horizon-days 90}}]
  {:pre [(some? seed) (sim-model/valid? pathway)
         (or (nil? pathways) (sim-model/valid-pathways-config? pathways))]}
  (let [rng (Random. ^long seed)
        ;; Provider NPIs are generated from this run's seed (sim/ADR-0007),
        ;; drawn once up front -- before arrival staggering -- so
        ;; provider identity is as deterministic and as fixed-order as
        ;; everything else this RNG produces.
        materialized-providers (sim-model/materialize-providers rng providers)
        ;; Stagger arrivals: :arrival-gap is authored in MINUTES (same
        ;; carve-out as :delay's IR, and for the same calibration
        ;; reason -- see `run`'s docstring); the engine converts to
        ;; SECONDS here. Consume RNG in patient order (fixed).
        arrivals (vec (reductions + 0 (repeatedly (dec patients)
                                                  #(* 60 (rand-int-in rng 0 arrival-gap)))))
        mrn-for (fn [i] (format "MRN%06d" (inc i)))
        pid-for (fn [i] (patient-id-for seed i))
        ;; M3-adjacent: :pathways ABSENT entirely -- the pinned-fixture
        ;; path -- means every patient gets the same plain :pathway, no
        ;; assign-pathway call, no new draw (see `run`'s docstring).
        pathway-for (if pathways
                      (fn [i] (assign-pathway rng pathways i))
                      (fn [_i] pathway))
        ;; InjectChurn (M2b): ONLY when :churn-profile is actually
        ;; present does this stage run at all -- absent, `steps-for` is
        ;; a no-op and consumes no RNG (see the docstring's fixture note).
        steps-for (if churn-profile
                    (fn [i] (:steps (churn/inject (pathway-for i) churn-profile rng)))
                    (fn [i] (:steps (pathway-for i))))
        ;; M5b Task 4: module-assignment is resolved eagerly, the SAME
        ;; point :pathways' own assign-pathway draw already occupies (one
        ;; more fixed-consumption draw per patient, ONLY when
        ;; :module-assignment is actually present -- absent entirely,
        ;; `module-for` draws nothing, byte-identical to pre-M5b (see
        ;; `run`'s own docstring)). The MODULE WALK itself (persona-
        ;; dependent -- an unbounded number of draws) stays at :registered
        ;; decide-time, below, the same place persona sampling already is.
        modules-by-id (into {} (map (fn [m] [(:id m) m])) modules)
        module-for (if module-assignment
                     (fn [i] (get modules-by-id (assign-module rng module-assignment i)))
                     (fn [_i] nil))
        ;; M4: :registered is prepended to EVERY patient's step queue,
        ;; ahead of whatever InjectChurn produced -- engine-internal,
        ;; never seen by InjectChurn's own applicability oracle (it
        ;; operates on `pathway-for`'s output, before this prepend), the
        ;; same "not authorable IR" treatment :result-followup gets. M5b:
        ;; carries this patient's own resolved module (nil, absent
        ;; :module-assignment) -- :registered's own decide method is
        ;; where the actual walk + compile happens (this namespace's own
        ;; comment there).
        registered-steps-for (fn [i] (into [{:type :registered :module (module-for i)}] (steps-for i)))
        init-queue (into (sorted-map)
                         (map-indexed
                          (fn [i arrival-t]
                            [[arrival-t i]
                             {:patient-id (pid-for i) :steps (registered-steps-for i)}])
                          arrivals))
        init-world {:patients (into {} (map-indexed (fn [i _] [(pid-for i) (initial-patient (pid-for i) (mrn-for i))]))
                                    arrivals)
                    :facility facility
                    :providers materialized-providers
                    :order-profiles order-profiles
                    :persona-config persona-config
                    :module-horizon-days module-horizon-days
                    ;; Task 1 (M2b): cancel-family/transfer-in-error decide
                    ;; methods query the log directly for the event they
                    ;; reinstate from (docs/patient-state-model.md's
                    ;; shadow-field dissolution) -- a PERSISTENT mirror of
                    ;; the log-so-far, kept alongside (not instead of) the
                    ;; transient `ground-truth` accumulator below so decide
                    ;; can `nth`/`filter`/`keep-indexed` over it (transients
                    ;; aren't seqable). Always a prefix of the final log.
                    :ground-truth []}
        mark-warmup (fn [ev] (assoc ev :warm-up (< (:t ev) warm-up-seconds)))
        final-result (fn [ground-truth state-history extra]
                       (merge {:ground-truth (persistent! ground-truth)
                               :state-history state-history
                               :facility facility
                               :providers materialized-providers}
                              extra))]
    (loop [queue init-queue
           seq-no patients
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
            (let [{:keys [events advance exhausted schedule-followup prepend-steps]} (decide rng t world patient-id step)]
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
                  world' (reduce (fn [w ev]
                                    (reduce (fn [w2 {:keys [patient-id]}]
                                              (update-in w2 [:patients patient-id] evolve ev))
                                            w (:participants ev)))
                                  world events)
                  world'' (assoc world' :ground-truth (into (:ground-truth world) events))
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
                (recur queue'' seq-no' world'' ground-truth' state-history')))))))))))
