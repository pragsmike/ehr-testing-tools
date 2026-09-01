(ns ehrt.sim-engine.evolve
  "The fold: `evolve`, the multimethod that folds ONE ground-truth event
  into ONE patient it names, its twenty-seven methods, and the four
  private helpers they share -- `engine.clj`'s fourth extraction under
  `roadmap.md#engine-namespace-extraction-and-apply-unification` (the
  census's own dependency order, `.agents/plans/engine-extraction-
  census.md` section 3a: `evolve` lands after `streams`, `state` and
  `encounters`, and before `fold`, whose `replay` calls it).

  Extracted OUTPUT-IDENTICAL: every form below is `engine.clj`'s own
  text, moved and not rewritten -- including the four comment blocks
  that carry a stale `below` (`:result-available`'s `EmitState`,
  `:appointment`'s `keep-appointment`, `:diagnostic-report`'s
  `:result-available` and `:medication-end`'s `decide`), each of which
  was already false where it stood and is moved verbatim rather than
  corrected inside a commit whose whole claim is that the moved text is
  unchanged. The banner that opens this file's forms is `engine.clj`'s
  own too: `dd956b0` wrote it one session ago to say where the four
  encounter folds went, and its last sentence -- \"The methods below
  call them `encounters/`-qualified\" -- is a positional claim about the
  twenty-seven methods, so it travels with them.

  ONE VAR WAS PUBLIC in `engine.clj`, the `defmulti` itself, and it
  keeps a delegating `(def evolve evolve/evolve)` there under ruling
  C1(a). That def and this one are the SAME multifn object, so every
  method registered here is dispatched by `ehrt.sim-engine.evolve/
  evolve` too. `replay` and `run` called it unqualified through that
  def until the fifth and tenth extractions took them to
  `ehrt.sim-engine.fold` and `ehrt.sim-engine.run`, where each names
  `evolve/evolve` directly -- the same multifn, one hop shorter. The four
  helpers (`fold-condition-annotation`, `fold-conditions`,
  `resolve-appointment`, `keep-appointment`) were `defn-`, so under
  constraint 5 they become public HERE and get no def THERE.

  Three edges, all taken DIRECTLY into the namespace that owns them
  rather than back through `engine.clj`'s delegating defs:
  `state/demographics-from-persona` and `state/placeholder-demographics`
  (`:registered` and `:demographic-update`),
  `streams/next-appointment-ordinal` (`:appointment`), and
  `encounters/{open,close,cancel-open,reopen}-encounter`, which were
  already qualified before this move. It reaches nothing else -- not
  `sim-model`, not malli, not `clojure.*`."
  (:require [ehrt.sim-engine.encounters :as encounters]
            [ehrt.sim-engine.state :as state]
            [ehrt.sim-engine.streams :as streams]))

(defn fold-condition-annotation
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
  [t encounter-id conditions {:keys [event codes citation]}]
  (case event
    :condition-onset
    (conj (or conditions [])
          (cond-> {:codes codes :citation citation :onset-t t :clinical-status :active}
            ;; ADR-0174 section 2(a): which encounter recorded it. Absent
            ;; with no `:encounters` opt-in, so a legacy record's bytes
            ;; are the bytes it always had.
            encounter-id (assoc :encounter-id encounter-id)))

    :condition-end
    (if-let [idx (last (keep-indexed (fn [i c] (when (and (= :active (:clinical-status c)) (= codes (:codes c))) i))
                                     conditions))]
      (update conditions idx assoc :clinical-status :resolved :end-t t)
      conditions)))

(defn fold-conditions
  [patient t encounter-id annotations]
  (cond-> patient
    (seq annotations)
    (update :conditions #(reduce (partial fold-condition-annotation t encounter-id) % annotations))))

;; --- ADR-0174 section 2(a) (arc 3b sweep 1): the encounter's fold now
;; lives in `ehrt.sim-engine.encounters`, with the gate and the stamp it
;; belongs beside -- `open-encounter`, `close-encounter`,
;; `cancel-open-encounter` and `reopen-encounter`, moved verbatim along
;; with the header comment that introduced them. The methods below call
;; them `encounters/`-qualified; nothing else about this fold changed.

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
                         (state/placeholder-demographics alias-name)
                         (cond-> (state/demographics-from-persona persona)
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
      (assoc :demographics (cond-> (state/demographics-from-persona persona)
                             residence (assoc :residence residence))))
    (cond-> patient
      (some? (:demographics patient)) (assoc-in [:demographics field] value))))

(defmethod evolve :coverage-change
  [patient {:keys [payer]}]
  (cond-> patient
    (some? (:demographics patient)) (assoc-in [:demographics :payer] payer)))

(defn resolve-appointment
  "Write `outcome` onto whichever record carries `appointment-id`.

  IT LOOKS IN BOTH PLACES, and that is the correction the overlap defect
  forced (`next-appointment-ordinal`'s own docstring carries the failure).
  An appointment is normally resolved while it is the OPEN one, but a
  second booking can displace it into `:appointments` before its own
  visit comes round -- and the displaced record is still the one a later
  opener, cancel or no-show is talking about. Looking only at the open
  slot silently dropped those resolutions.

  Guarded on the outcome being ABSENT, so a record that already reached a
  terminal is never overwritten by a second one: the STATE cannot record
  two, and `appointment-reaches-at-most-one-terminal` is what reports the
  LOG that tried."
  [patient appointment-id outcome]
  (cond
    (nil? appointment-id) patient

    (= appointment-id (:appointment-id (:appointment patient)))
    (-> patient
        (update :appointments (fnil conj []) (assoc (:appointment patient) :outcome outcome))
        (assoc :appointment nil))

    :else
    (update patient :appointments
            (fn [as]
              (mapv (fn [a]
                      (if (and (= appointment-id (:appointment-id a)) (nil? (:outcome a)))
                        (assoc a :outcome outcome)
                        a))
                    (or as []))))))

(defn keep-appointment
  "What an ENCOUNTER OPENER does to the appointment it names: closes it
  `:kept`. \"Kept\" is not an event -- it IS the encounter happening -- so
  the opener's own fold is the only place it can be written."
  [patient appointment-id]
  (resolve-appointment patient appointment-id :kept))

(defmethod evolve :admission
  ;; ARC 3B SWEEP 1: opens the encounter. `open-encounter` reads the
  ;; PRE-admission patient, so the ordinal it takes counts what this
  ;; patient had before, never including the encounter being opened.
  ;; ARC 3B SWEEP 3: and closes the APPOINTMENT it names, `:kept`. The
  ;; kept terminal is not an event of its own -- it IS the encounter
  ;; happening -- so the opener's fold is the only place it can be
  ;; written. `keep-appointment` runs BEFORE `open-encounter` reads the
  ;; patient, which is deliberate and costs nothing: the encounter
  ;; ordinal counts encounters, never appointments.
  [patient {:keys [location home-ward attending t conditions encounter-id appointment-id] :as event}]
  (-> patient
      (keep-appointment appointment-id)
      (assoc :status :admitted
             :class :inpatient
             :home-ward home-ward
             :location location
             :attending attending
             :admitted-at t
             :encounter (encounters/open-encounter patient event))
      (fold-conditions t encounter-id conditions)))

(defmethod evolve :transfer
  [patient {:keys [location home-ward]}]
  (assoc patient :location location :home-ward home-ward))

(defmethod evolve :discharge
  ;; ARC 3B SWEEP 1: the non-expired arm CLOSES the encounter -- conj the
  ;; open record, stamped with the state the discharge itself leaves,
  ;; onto `:encounters` and drop `:encounter`. THE `:expired` ARM IS
  ;; UNTOUCHED and its encounter stays OPEN, because the body stays in
  ;; the bed, which is exactly what `expired-patient-retains-location`
  ;; asserts (ADR-0174 section 2(a) item 4).
  ;;
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
    (-> patient
        (assoc :status :discharged :location nil :discharged-at t)
        encounters/close-encounter)))

;; --- M2b: churn family evolves -------------------------------------------

(defmethod evolve :cancel-admit
  [patient _event]
  (-> patient
      (assoc :status :new)
      (dissoc :class :home-ward :location :attending :admitted-at)
      encounters/cancel-open-encounter))

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
      (dissoc :discharged-at)
      ;; ARC 3B SWEEP 1: and re-opens the encounter its own :discharge
      ;; closed, keeping that encounter's id -- a reinstated stay is ONE
      ;; encounter, not two.
      encounters/reopen-encounter))

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
  ;; ARC 3B SWEEP 3: the SECOND opener, and the one a follow-up produces
  ;; -- so this is where a scheduled return visit closes its own
  ;; appointment `:kept`.
  [patient {:keys [attending t conditions encounter-id appointment-id] :as event}]
  (-> patient
      (keep-appointment appointment-id)
      (assoc :status :admitted :class :outpatient :attending attending
             :encounter (encounters/open-encounter patient event))
      (fold-conditions t encounter-id conditions)))

(defmethod evolve :outpatient-visit-end
  [patient {:keys [t]}]
  (-> patient
      (assoc :status :discharged :discharged-at t)
      encounters/close-encounter))

;; --- the four kinds' own folds. An `:appointment` opens the record; a
;; `:reschedule` MOVES it and is not terminal; a cancel and a no-show
;; close it. The KEPT terminal is written by the opener's own evolve
;; (`keep-appointment`, below), because "kept" is not an event -- it is
;; the encounter happening.

(defmethod evolve :appointment
  ;; A SECOND BOOKING ARCHIVES THE OPEN ONE rather than dropping it --
  ;; appointments can overlap (see `next-appointment-ordinal`), and a
  ;; dropped record both loses its resolution and un-monotones the
  ;; ordinal. The archived record carries NO `:outcome`, because it has
  ;; not reached one; `resolve-appointment` can still find it there when
  ;; its own opener, cancel or no-show arrives.
  [patient {:keys [t appointment-id scheduled-t appointment-class reason]}]
  (let [ordinal (streams/next-appointment-ordinal patient)
        archived (if-let [open (:appointment patient)]
                   (update patient :appointments (fnil conj []) open)
                   patient)]
    (assoc archived :appointment
           (cond-> {:appointment-id appointment-id
                    :ordinal ordinal
                    :booked-at t
                    :scheduled-t scheduled-t
                    :appointment-class appointment-class}
             reason (assoc :reason reason)))))

(defmethod evolve :reschedule
  ;; NOT terminal: it moves `:scheduled-t` and leaves the record open,
  ;; which is why the id is kept rather than re-minted. Like the three
  ;; resolutions it must reach a DISPLACED record too, for
  ;; `resolve-appointment`'s own stated reason.
  [patient {:keys [appointment-id prior-scheduled-t scheduled-t]}]
  (let [move #(assoc % :prior-scheduled-t prior-scheduled-t :scheduled-t scheduled-t)]
    (if (= appointment-id (:appointment-id (:appointment patient)))
      (update patient :appointment move)
      (update patient :appointments
              (fn [as] (mapv #(if (= appointment-id (:appointment-id %)) (move %) %)
                             (or as [])))))))

(defmethod evolve :appointment-cancel
  [patient {:keys [appointment-id]}]
  (resolve-appointment patient appointment-id :cancelled))

(defmethod evolve :no-show
  [patient {:keys [appointment-id]}]
  (resolve-appointment patient appointment-id :no-show))

;; --- M5b: :procedure -- a log-only fact, no PatientState field change
;; (Procedure is deliberately outside EmitState's own rendered resource
;; set, M6 Task 1 -- "keep the resource set to what state actually
;; holds," applied by never accumulating what nothing renders).

(defmethod evolve :procedure [patient _event] patient)

;; --- M6 Task 1: :observation/:medication-order/:medication-end now land
;; in the clinical-content accumulator (`ehrt.sim-engine.state`'s own
;; header comment above `PatientState`) -- EmitState's Observation/
;; MedicationRequest resources render from exactly these records,
;; nothing re-derived from the log.

(defmethod evolve :observation
  [patient {:keys [t codes encounter-id] :as event}]
  (update patient :observations (fnil conj [])
          (cond-> (merge {:codes codes :t t} (state/observation-value-fields event))
            encounter-id (assoc :encounter-id encounter-id))))

;; --- GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P5): FLATTENS
;; each child into its own ObservationRecord -- the IDENTICAL pattern
;; :result-available's own per-analyte flattening already establishes
;; (below), reused rather than a third accumulator shape invented.

(defmethod evolve :diagnostic-report
  [patient {:keys [t observations encounter-id]}]
  (update patient :observations (fnil into [])
          (mapv (fn [{:keys [codes] :as entry}]
                  (cond-> (merge {:codes codes :t t} (state/observation-value-fields entry))
                    encounter-id (assoc :encounter-id encounter-id)))
                observations)))

(defmethod evolve :medication-order
  [patient {:keys [t codes citation encounter-id]}]
  (update patient :medication-orders (fnil conj [])
          (cond-> {:codes codes :citation citation :ordered-t t :status :active}
            encounter-id (assoc :encounter-id encounter-id))))

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
  [patient {:keys [t codes activities citation encounter-id]}]
  (update patient :care-plans (fnil conj [])
          (cond-> {:codes codes :citation citation :started-t t :status :active}
            activities (assoc :activities activities)
            encounter-id (assoc :encounter-id encounter-id))))

(defmethod evolve :care-plan-end
  [patient {:keys [t care-plan-citation]}]
  (if-let [idx (when care-plan-citation
                 (last (keep-indexed (fn [i m] (when (and (= :active (:status m)) (= care-plan-citation (:citation m))) i))
                                     (:care-plans patient))))]
    (update-in patient [:care-plans idx] assoc :status :completed :ended-t t)
    patient))
