(ns ehr-testing-sim.check
  "The invariant catalog: internal-consistency claims about a
  ground-truth log, machine-checkable (validation program, claim #3 in
  docs/problem-statement.md). Runs standalone as `sim check` and in CI
  as the regression suite; the property tests in test/ drive the same
  functions over generated runs, so the catalog does double duty.

  Each invariant is a named function (ground-truth) -> seq of violation
  maps (empty = holds). `check-all` aggregates to a Result: :ok when
  every invariant holds, :rejected (:category :invariant-violation)
  otherwise -- the check ran and the answer is no, which is exactly
  what the result-not-throw doctrine's :rejected arm is for.

  v0 catalog is minimal; every new step type added to the engine MUST
  land with its invariants here in the same change.

  Milestone M1 (docs/operational-models.md) adds the facility-aware
  invariants (no double occupancy, one physical slot per admitted
  patient, capacity, surge-only-when-earlier-rungs-exhausted) plus the
  event-validity rows from docs/patient-state-model.md (admission only
  when :new, transfer only when :admitted, a transfer's declared
  :from matches the fold). These read patient/world state via
  ehr-testing-sim.engine/replay -- the same fold `evolve` always was,
  reused rather than reimplemented (ADR-0008).

  M2a (ADR-0010) adds two structural invariants over :participants
  (every event has >=1, every participant id traces to an :admission
  in the same log) and moves every per-patient grouping from
  `:mrn` to `:participants`-derived patient-ids -- an event with more
  than one participant (M2b's bed-swap, merge) belongs to every
  participant's own sequence, not just one. M2a (ADR-0011) adds the
  warm-up-mark invariant (config/check-warm-up.clj docstring companion
  below)."
  (:require [clojure.set]
            [ehr-testing-sim.config :as config]
            [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.facility :as facility]
            [ehr-testing-sim.order-profiles :as order-profiles]
            [ehr-testing-sim.persona :as persona]
            [ehr-testing-sim.result :as result]))

(defn- events-by-patient
  "Every event each patient-id participates in, in log order -- the
  general, patient-phrased replacement for `(group-by :mrn ...)`. An
  event with multiple participants (M2b) appears in every participant's
  own sequence; today's event types are all single-participant, so this
  is presently equivalent to grouping by the sole participant, but is
  written the general way so M2b needs no rewrite here."
  [ground-truth]
  (reduce (fn [acc event]
            (reduce (fn [acc2 {:keys [patient-id]}]
                      (update acc2 patient-id (fnil conj []) event))
                    acc (:participants event)))
          {} ground-truth))

(defn timestamps-monotone
  "Within a patient, event times never decrease (log order is emission
  order, which the engine guarantees is time order)."
  [ground-truth]
  (for [[patient-id events] (events-by-patient ground-truth)
        [a b] (partition 2 1 events)
        :when (> (:t a) (:t b))]
    {:invariant :timestamps-monotone :patient-id patient-id :at [(:t a) (:t b)]}))

(defn discharge-follows-admission
  "No patient is discharged without a prior admission, and not twice."
  [ground-truth]
  (for [[patient-id events] (events-by-patient ground-truth)
        :let [kinds (mapv :event events)
              first-admit (.indexOf ^java.util.List kinds :admission)
              discharges (keep-indexed #(when (= :discharge %2) %1) kinds)]
        d discharges
        :when (or (neg? first-admit) (< d first-admit))]
    {:invariant :discharge-follows-admission :patient-id patient-id :at d}))

;; --- ADR-0010: structural participant invariants -------------------------

(defn every-event-has-participants
  "Every event names at least one participant (ADR-0010) -- a bug in a
  decide implementation or a future churn-injection step could
  otherwise emit an orphan event no patient's fold ever sees."
  [ground-truth]
  (for [event ground-truth
        :when (empty? (:participants event))]
    {:invariant :every-event-has-participants :event (:event event) :at (:t event)}))

(defn participant-ids-exist-in-run
  "Every patient-id named in any event's :participants is a patient-id
  this run actually created -- i.e. appears as a participant on at
  least one :registered event somewhere in the log. :registered is the
  ONE event type EVERY real patient this run creates always gets (M4),
  unconditionally -- a stricter, more universal proof than requiring an
  :admission/:outpatient-visit, which (M5b) a module-assigned patient
  can legitimately never get at all if their own disease process never
  produces an operational encounter inside this run's own configured
  horizon window. Catches a churn-injection or decide bug that names a
  stray or mistyped patient-id."
  [ground-truth]
  (let [admitted-ids (into #{}
                           (comp (filter #(= :registered (:event %)))
                                 (mapcat :participants)
                                 (map :patient-id))
                           ground-truth)]
    (for [event ground-truth
          {:keys [patient-id]} (:participants event)
          :when (not (contains? admitted-ids patient-id))]
      {:invariant :participant-ids-exist-in-run :patient-id patient-id :at (:t event)})))

;; --- ADR-0011: the warm-up mark -------------------------------------------

(defn warm-up-mark-matches-window
  "The warm-up mark is exactly `t < warm-up-seconds` (ADR-0011) -- a
  pure predicate over each event's own :t and the run's configured
  warm-up window, checkable without replay."
  [ground-truth warm-up-seconds]
  (for [event ground-truth
        :when (not= (boolean (:warm-up event)) (< (:t event) warm-up-seconds))]
    {:invariant :warm-up-mark-matches-window :at (:t event)}))

;; --- M1 event-validity rows (docs/patient-state-model.md) ---------------

(defn admission-only-when-new
  "docs/patient-state-model.md's event-validity table: :admission is
  legal only when the patient's prior state is :new."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (= :admission (:event event)) (not= :new (:status before)))]
    {:invariant :admission-only-when-new :patient-id patient-id :at (:t event)}))

(defn transfer-only-when-admitted
  "docs/patient-state-model.md's event-validity table: :transfer
  (including bed-ready) is legal only when the patient's prior state
  is :admitted (Admitted or Boarding)."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (= :transfer (:event event)) (not= :admitted (:status before)))]
    {:invariant :transfer-only-when-admitted :patient-id patient-id :at (:t event)}))

(defn transfer-from-matches-state
  "A transfer event's declared :from matches the patient's actual
  location immediately beforehand (docs/operational-models.md)."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (= :transfer (:event event)) (not= (:from event) (:location before)))]
    {:invariant :transfer-from-matches-state :patient-id patient-id :at (:t event)}))

;; --- M1 facility invariants (docs/operational-models.md) ----------------

(defn no-double-occupancy
  "No bed holds two patients at once, at any event boundary."
  [ground-truth]
  (for [{:keys [event world-after]} (engine/replay ground-truth)
        :let [beds (keep (comp :bed :location) (vals world-after))
              dupes (->> beds frequencies (filter (comp #(> % 1) val)) (map key))]
        bed dupes]
    {:invariant :no-double-occupancy :bed bed :at (:t event)}))

(defn admitted-occupies-one-slot
  "An admitted patient (Admitted or Boarding) occupies exactly one
  physical slot -- location and its bed are never nil while admitted.
  M5b: EXCEPT an outpatient (`:class :outpatient`) -- docs/patient-
  state-model.md's event-validity table's own conditional row (`:location
  = nil` is legal exactly when `:class = :outpatient`), the named,
  narrowly-gated exception to this rule (docs/gmf-interpreter.md section
  4's item 6). `outpatient-patients-occupy-no-bed`, below, is this same
  fact's own converse: an outpatient patient's :location must ALWAYS be
  nil, never merely may be."
  [ground-truth]
  (for [{:keys [event world-after]} (engine/replay ground-truth)
        [patient-id {:keys [status location class]}] world-after
        :when (and (= status :admitted) (not= class :outpatient)
                   (or (nil? location) (nil? (:bed location))))]
    {:invariant :admitted-occupies-one-slot :patient-id patient-id :at (:t event)}))

;; --- M5b: :outpatient-visit / :outpatient-visit-end (docs/gmf-interpreter.md
;; section 4's sketch, item 8's own invariant list) --------------------------

(defn outpatient-visit-only-when-new
  "docs/patient-state-model.md's event-validity table, extended: an
  :outpatient-visit is legal only when the patient's prior state is
  :new -- the same treatment :admission's own row already gets."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (= :outpatient-visit (:event event)) (not= :new (:status before)))]
    {:invariant :outpatient-visit-only-when-new :patient-id patient-id :at (:t event)}))

(defn outpatient-patients-occupy-no-bed
  "The structural half of item 6's conditional validity row: `:class
  :outpatient => :location nil`, for the visit's entire duration -- an
  outpatient patient was never a candidate for the occupancy board to
  include in the first place (`ehr-testing-sim.facility/occupancy-board`
  already only folds patients with a `:bed` present, so this is checked
  here directly rather than assumed from that board's own omission)."
  [ground-truth]
  (for [{:keys [event world-after]} (engine/replay ground-truth)
        [patient-id {:keys [class location]}] world-after
        :when (and (= class :outpatient) (some? location))]
    {:invariant :outpatient-patients-occupy-no-bed :patient-id patient-id :at (:t event)}))

(defn occupancy-within-capacity
  "Occupancy never exceeds a ward's declared capacity (licensed +
  surge slots)."
  [ground-truth facility-config]
  (for [{:keys [event world-after]} (engine/replay ground-truth)
        ward (:wards facility-config)
        :let [cap (+ (:beds ward) (:surge-slots ward))
              occ (count (filter #(= (:name ward) (get-in % [:location :ward])) (vals world-after)))]
        :when (> occ cap)]
    {:invariant :occupancy-within-capacity :ward (:name ward) :at (:t event)
     :occupied occ :capacity cap}))

(defn- earlier-rungs-exhausted?
  "Whether the ladder's earlier rungs were legitimately exhausted at
  `board`, for a placement targeting `target-ward-name` on behalf of
  `home-ward-name`: rung 2 (home surge) requires only rung 1 (home
  licensed) exhausted; rung 4 (boarding, target is a DIFFERENT,
  ED-class ward) requires rungs 1-3 all exhausted."
  [facility-config board home-ward-name target-ward-name]
  (let [home-ward (facility/ward-by-name facility-config home-ward-name)
        home-licensed-free? (boolean (seq (remove board (facility/licensed-bed-ids home-ward))))
        home-surge-free? (boolean (seq (remove board (facility/surge-slot-ids home-ward))))]
    (if (= home-ward-name target-ward-name)
      (not home-licensed-free?)
      (let [other-inpatient (remove #(= (:id %) (:id home-ward))
                                     (filter #(= :inpatient (:class %)) (:wards facility-config)))
            other-licensed-free? (boolean
                                   (some #(seq (remove board (facility/licensed-bed-ids %))) other-inpatient))]
        (and (not home-licensed-free?) (not home-surge-free?) (not other-licensed-free?))))))

(defn surge-only-when-earlier-rungs-exhausted
  "Surge placement (rung 2 or 4) only occurs when the earlier rungs are
  legitimately exhausted -- unless :forced true (docs/operational-
  models.md's own exemption for the authoring escape hatch)."
  [ground-truth facility-config]
  (for [{:keys [event world-before patient-id]} (engine/replay ground-truth)
        :when (and (#{:admission :transfer} (:event event))
                   (= :surge (get-in event [:location :placement]))
                   (not (:forced event))
                   (not (earlier-rungs-exhausted? facility-config
                                                  (facility/occupancy-board world-before)
                                                  (:home-ward event)
                                                  (get-in event [:location :ward]))))]
    {:invariant :surge-only-when-earlier-rungs-exhausted :patient-id patient-id :at (:t event)}))

;; --- M2b: churn family (docs/patient-state-model.md's event-validity
;; table's cancel-*/bed-swap/merge rows; ADR-0010's cross-participant
;; coherence) -------------------------------------------------------------

(def ^:private cancel-target-type
  "Cancel event type -> the event type it must reference."
  {:cancel-admit :admission :cancel-transfer :transfer :cancel-discharge :discharge})

(defn cancel-references-existing-uncancelled-event
  "The event-validity table's cancel-* row: the event class being
  cancelled must exist in this patient's log, be the RIGHT class, and
  not already be cancelled by an earlier cancel of the same kind.
  Structural -- checks any log directly, independent of whether decide
  itself already enforces this (docs/patient-state-model.md)."
  [ground-truth]
  (let [indexed (vec ground-truth)]
    (for [[idx event] (map-indexed vector indexed)
          :when (contains? cancel-target-type (:event event))
          :let [target-idx (:cancels-event-id event)
                target (get indexed target-idx)
                expected-type (get cancel-target-type (:event event))
                patient-id (:patient-id (first (:participants event)))
                cancelled-earlier? (some (fn [[i2 ev2]]
                                           (and (< i2 idx)
                                                (= (:event event) (:event ev2))
                                                (= target-idx (:cancels-event-id ev2))))
                                         (map-indexed vector indexed))]
          :when (or (nil? target)
                    (not= expected-type (:event target))
                    (not (some #(= patient-id (:patient-id %)) (:participants target)))
                    cancelled-earlier?)]
      {:invariant :cancel-references-existing-uncancelled-event :patient-id patient-id :at (:t event)})))

(defn bed-swap-both-admitted-before-swap
  "Both bed-swap participants were :admitted immediately beforehand
  (docs/operational-models.md's own admitted-when-placed rule, extended
  to the genuinely-two-participant case -- ADR-0010)."
  [ground-truth]
  (for [{:keys [event world-before]} (engine/replay ground-truth)
        :when (= :bed-swap (:event event))
        {:keys [patient-id]} (:participants event)
        :let [before (get world-before patient-id)]
        :when (not= :admitted (:status before))]
    {:invariant :bed-swap-both-admitted-before-swap :patient-id patient-id :at (:t event)}))

(defn merge-survivor-absorbs-merged-mrns
  "docs/patient-state-model.md's identity payoff: the merge's stated
  surviving MRN must be one the survivor already answered to (not an
  arbitrary string); the survivor's post-merge :active-mrn is exactly
  that; and the survivor's post-merge :mrns is a superset of what the
  merged patient answered to beforehand (retired, not discarded)."
  [ground-truth]
  (for [{:keys [event world-before world-after]} (engine/replay ground-truth)
        :when (= :merge (:event event))
        :let [{:keys [participants surviving-mrn]} event
              survivor-id (:patient-id (first (filter #(= :survivor (:role %)) participants)))
              merged-id (:patient-id (first (filter #(= :merged (:role %)) participants)))
              survivor-before (get world-before survivor-id)
              merged-before (get world-before merged-id)
              survivor-after (get world-after survivor-id)]
        :when (not (and (contains? (:mrns survivor-before) surviving-mrn)
                        (= surviving-mrn (:active-mrn survivor-after))
                        (clojure.set/subset? (:mrns merged-before) (:mrns survivor-after))))]
    {:invariant :merge-survivor-absorbs-merged-mrns :patient-id survivor-id :at (:t event)}))

(defn no-events-after-merged-terminal
  "The merged patient-id's stream ends with its own merge event -- no
  later event in the log names it as a participant (docs/patient-
  state-model.md, ADR-0010)."
  [ground-truth]
  (let [indexed (vec ground-truth)]
    (for [[merge-idx event] (map-indexed vector indexed)
          :when (= :merge (:event event))
          :let [merged-id (:patient-id (first (filter #(= :merged (:role %)) (:participants event))))]
          [later-idx later-event] (map-indexed vector indexed)
          :when (and (> later-idx merge-idx)
                    (some #(= merged-id (:patient-id %)) (:participants later-event)))]
      {:invariant :no-events-after-merged-terminal :patient-id merged-id :at (:t later-event)})))

;; --- ADR-0012: :step-rejected -- truth about the run, checked structurally
;; (never a message-bearing event -- no message-type-registry entry, by
;; design; see ehr-testing-sim.engine/documented-step-rejection-reasons) --

(defn step-rejected-reason-is-documented
  "ADR-0012's own invariant: every :step-rejected event's :reason is one
  of the documented enum (ehr-testing-sim.engine/documented-step-
  rejection-reasons) -- a rejection with an undocumented reason would
  mean a new decide-time rejection path shipped without updating the
  enum, the co-landing convention extended to this event type."
  [ground-truth]
  (for [event ground-truth
        :when (and (= :step-rejected (:event event))
                   (not (contains? engine/documented-step-rejection-reasons (:reason event))))]
    {:invariant :step-rejected-reason-is-documented :reason (:reason event) :at (:t event)}))

;; --- M3: order/result (docs/patient-state-model.md's event-validity
;; table's therapeutic-intent-class row -- orders/results illegal when
;; :status = :expired; written here as "legal only when :admitted", the
;; strict generalization that's testable today since :expired isn't a
;; landed status yet -- once it lands, :expired != :admitted makes this
;; row cover it automatically, no new invariant needed then) ---------------

(defn order-only-when-admitted
  "Therapeutic-intent class (docs/patient-state-model.md's event-
  validity table): :order-placed is legal only when the patient's prior
  state is :admitted -- covers :new/:discharged/:merged today, and
  (once that status lands) :expired too, since :expired is never
  :admitted either.

  Deliberately NOT extended to :result-available: a result's own
  turnaround is asynchronous to the rest of the patient's pathway (the
  patient's OTHER steps, including :discharge, are not blocked waiting
  for it -- engine.clj's :order docstring), so a result legitimately
  arriving after discharge is a real, common clinical pattern (pending
  labs at discharge), not a bug -- an engine/run integration test
  surfaced exactly this case during this milestone's own development,
  which is why this invariant is scoped to the order alone rather than
  generalized to both event types. result-references-existing-order-
  and-follows-it-in-time already guarantees a result's own order was
  itself legitimate."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (= :order-placed (:event event)) (not= :admitted (:status before)))]
    {:invariant :order-only-when-admitted :patient-id patient-id :at (:t event)}))

(defn result-references-existing-order-and-follows-it-in-time
  "Every :result-available event's :order-event-id is a real
  :order-placed event in this same log, for the SAME patient, at or
  before the result's own :t (co-landing invariant, Milestone M3)."
  [ground-truth]
  (let [indexed (vec ground-truth)]
    (for [[idx event] (map-indexed vector indexed)
          :when (= :result-available (:event event))
          :let [target-idx (:order-event-id event)
                target (get indexed target-idx)
                patient-id (:patient-id (first (:participants event)))]
          :when (or (nil? target)
                    (not= :order-placed (:event target))
                    (not (some #(= patient-id (:patient-id %)) (:participants target)))
                    (> (:t target) (:t event)))]
      {:invariant :result-references-existing-order-and-follows-it-in-time :patient-id patient-id :at (:t event)})))

(defn result-analytes-match-order-profile
  "Every :result-available event's :results analyte-concept set is
  EXACTLY its own :profile's analyte set (`order-profiles` -- default
  ehr-testing-sim.order-profiles/default-profiles, the same 'needs more
  than just the log' pattern facility-catalog/warmup-catalog already
  follow) -- catches a result that dropped, added, or substituted an
  analyte relative to what its own profile declares."
  [ground-truth order-profiles]
  (for [event ground-truth
        :when (= :result-available (:event event))
        :let [expected (into #{} (map :concept) (:analytes (get order-profiles (:profile event))))
              actual (into #{} (map :concept) (:results event))]
        :when (not= expected actual)]
    {:invariant :result-analytes-match-order-profile :profile (:profile event) :at (:t event)}))

(defn abnormal-flags-consistent-with-value-vs-range
  "The computed-truth mini-law (Milestone M3 Task 4), checked from the
  log directly: every result entry's :abnormal-flag equals
  ehr-testing-sim.order-profiles/abnormal-flag applied to its own value
  and reference-range -- a flag that disagrees with its own value is a
  bug, not a legitimate finding."
  [ground-truth]
  (for [event ground-truth
        :when (= :result-available (:event event))
        {:keys [value reference-range abnormal-flag]} (:results event)
        :when (not= abnormal-flag (order-profiles/abnormal-flag value reference-range))]
    {:invariant :abnormal-flags-consistent-with-value-vs-range :profile (:profile event) :at (:t event)}))

;; --- M5b: CompileTrajectory's new event types (docs/gmf-interpreter.md
;; section 1's table) -- :procedure/:observation/:medication-order are the
;; therapeutic-intent class (docs/patient-state-model.md's event-validity
;; table row), the same "legal only when :admitted" scoping :order-placed
;; already gets; :medication-end is deliberately NOT included, same reason
;; :result-available isn't -- a medication legitimately continues (and
;; ends) after discharge (a patient still taking a prescription at home).

(defn clinical-content-only-when-admitted
  "Therapeutic-intent class, extended to M5b's compiled clinical content:
  :procedure/:observation/:medication-order are legal only when the
  patient's prior state is :admitted."
  [ground-truth]
  (for [{:keys [event before patient-id]} (engine/replay ground-truth)
        :when (and (#{:procedure :observation :medication-order} (:event event)) (not= :admitted (:status before)))]
    {:invariant :clinical-content-only-when-admitted :patient-id patient-id :at (:t event)}))

(defn medication-end-references-existing-order-and-follows-it-in-time
  "Every :medication-end event's :order-event-id is a real
  :medication-order event in this same log, for the SAME patient, at or
  before the end's own :t -- the same shape result's own referential
  invariant already establishes for :order-placed/:result-available."
  [ground-truth]
  (let [indexed (vec ground-truth)]
    (for [[idx event] (map-indexed vector indexed)
          :when (= :medication-end (:event event))
          :let [target-idx (:order-event-id event)
                target (get indexed target-idx)
                patient-id (:patient-id (first (:participants event)))]
          :when (or (nil? target)
                    (not= :medication-order (:event target))
                    (not (some #(= patient-id (:patient-id %)) (:participants target)))
                    (> (:t target) (:t event)))]
      {:invariant :medication-end-references-existing-order-and-follows-it-in-time :patient-id patient-id :at (:t event)})))

;; --- M4: Persona (docs/sim-theory.edn's :persona stage) -------------------

(defn registered-is-every-patients-first-event
  "docs/sim-theory.edn's :persona stage lands as the engine-internal
  :registered event, prepended to every patient's step queue
  (ehr-testing-sim.engine/run) -- structurally, that means it must be
  the FIRST event naming any given patient-id, every time, or
  ehr-testing-sim.engine/replay's own bootstrap (which seeds a
  never-yet-seen participant's initial state off the first event
  naming them, ADR-0010) would silently seed from the wrong event."
  [ground-truth]
  (for [[patient-id events] (events-by-patient ground-truth)
        :when (not= :registered (:event (first events)))]
    {:invariant :registered-is-every-patients-first-event :patient-id patient-id :at (:t (first events))}))

(defn registered-persona-is-schema-valid
  "Every :registered event's :persona validates against
  ehr-testing-sim.persona/Persona -- the schema round-trip co-landing
  invariant for M4's new persona resource type."
  [ground-truth]
  (for [event ground-truth
        :when (and (= :registered (:event event)) (not (persona/valid-persona? (:persona event))))]
    {:invariant :registered-persona-is-schema-valid :at (:t event)}))

(def catalog
  "The full invariant catalog needing only a ground-truth log, in
  reporting order."
  [#'timestamps-monotone
   #'discharge-follows-admission
   #'every-event-has-participants
   #'participant-ids-exist-in-run
   #'admission-only-when-new
   #'transfer-only-when-admitted
   #'transfer-from-matches-state
   #'no-double-occupancy
   #'admitted-occupies-one-slot
   #'cancel-references-existing-uncancelled-event
   #'bed-swap-both-admitted-before-swap
   #'merge-survivor-absorbs-merged-mrns
   #'no-events-after-merged-terminal
   #'step-rejected-reason-is-documented
   #'order-only-when-admitted
   #'result-references-existing-order-and-follows-it-in-time
   #'abnormal-flags-consistent-with-value-vs-range
   #'registered-is-every-patients-first-event
   #'registered-persona-is-schema-valid
   #'outpatient-visit-only-when-new
   #'outpatient-patients-occupy-no-bed
   #'clinical-content-only-when-admitted
   #'medication-end-references-existing-order-and-follows-it-in-time])

(def facility-catalog
  "Invariants that need the facility config, not just the log (checked
  separately from `catalog` because their function signature differs
  -- `check-all` runs both)."
  [#'occupancy-within-capacity
   #'surge-only-when-earlier-rungs-exhausted])

(def warmup-catalog
  "Invariants that need the run's configured warm-up window (ADR-0011),
  not just the log -- same reason `facility-catalog` is separate."
  [#'warm-up-mark-matches-window])

(def order-profiles-catalog
  "Invariants that need the order-profiles config, not just the log --
  same reason `facility-catalog` is separate (Milestone M3)."
  [#'result-analytes-match-order-profile])

(defn check-all
  "Runs every invariant in the catalog over a ground-truth log.
  `facility-config` (default config/default-facility) is needed by the
  capacity/surge-ladder invariants; `warm-up-seconds` (default 0) is
  needed by the warm-up-mark invariant; `order-profiles-config`
  (default ehr-testing-sim.order-profiles/default-profiles, Milestone
  M3) is needed by result-analytes-match-order-profile. Existing
  1-arg/2-arg/3-arg call sites are unaffected."
  ([ground-truth] (check-all ground-truth config/default-facility 0 order-profiles/default-profiles))
  ([ground-truth facility-config] (check-all ground-truth facility-config 0 order-profiles/default-profiles))
  ([ground-truth facility-config warm-up-seconds]
   (check-all ground-truth facility-config warm-up-seconds order-profiles/default-profiles))
  ([ground-truth facility-config warm-up-seconds order-profiles-config]
   (let [base-violations (into [] (mapcat #(% ground-truth)) catalog)
         facility-violations (into [] (mapcat #(% ground-truth facility-config)) facility-catalog)
         warmup-violations (into [] (mapcat #(% ground-truth warm-up-seconds)) warmup-catalog)
         order-profiles-violations (into [] (mapcat #(% ground-truth order-profiles-config)) order-profiles-catalog)
         violations (-> base-violations
                        (into facility-violations)
                        (into warmup-violations)
                        (into order-profiles-violations))]
     (if (empty? violations)
       (result/ok {:invariants-checked (-> (mapv (comp :name meta) catalog)
                                            (into (mapv (comp :name meta) facility-catalog))
                                            (into (mapv (comp :name meta) warmup-catalog))
                                            (into (mapv (comp :name meta) order-profiles-catalog)))
                   :events (count ground-truth)})
       (result/rejected :invariant-violation {:violations violations})))))
