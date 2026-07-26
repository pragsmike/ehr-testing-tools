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
  reused rather than reimplemented (ADR-0008)."
  (:require [ehr-testing-sim.config :as config]
            [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.facility :as facility]
            [ehr-testing-sim.result :as result]))

(defn- by-patient [ground-truth]
  (group-by :mrn ground-truth))

(defn timestamps-monotone
  "Within a patient, event times never decrease (log order is emission
  order, which the engine guarantees is time order)."
  [ground-truth]
  (for [[mrn events] (by-patient ground-truth)
        [a b] (partition 2 1 events)
        :when (> (:t a) (:t b))]
    {:invariant :timestamps-monotone :mrn mrn :at [(:t a) (:t b)]}))

(defn discharge-follows-admission
  "No patient is discharged without a prior admission, and not twice."
  [ground-truth]
  (for [[mrn events] (by-patient ground-truth)
        :let [kinds (mapv :event events)
              first-admit (.indexOf ^java.util.List kinds :admission)
              discharges (keep-indexed #(when (= :discharge %2) %1) kinds)]
        d discharges
        :when (or (neg? first-admit) (< d first-admit))]
    {:invariant :discharge-follows-admission :mrn mrn :at d}))

;; --- M1 event-validity rows (docs/patient-state-model.md) ---------------

(defn admission-only-when-new
  "docs/patient-state-model.md's event-validity table: :admission is
  legal only when the patient's prior state is :new."
  [ground-truth]
  (for [{:keys [event before]} (engine/replay ground-truth)
        :when (and (= :admission (:event event)) (not= :new (:status before)))]
    {:invariant :admission-only-when-new :mrn (:mrn event) :at (:t event)}))

(defn transfer-only-when-admitted
  "docs/patient-state-model.md's event-validity table: :transfer
  (including bed-ready) is legal only when the patient's prior state
  is :admitted (Admitted or Boarding)."
  [ground-truth]
  (for [{:keys [event before]} (engine/replay ground-truth)
        :when (and (= :transfer (:event event)) (not= :admitted (:status before)))]
    {:invariant :transfer-only-when-admitted :mrn (:mrn event) :at (:t event)}))

(defn transfer-from-matches-state
  "A transfer event's declared :from matches the patient's actual
  location immediately beforehand (docs/operational-models.md)."
  [ground-truth]
  (for [{:keys [event before]} (engine/replay ground-truth)
        :when (and (= :transfer (:event event)) (not= (:from event) (:location before)))]
    {:invariant :transfer-from-matches-state :mrn (:mrn event) :at (:t event)}))

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
  physical slot -- location and its bed are never nil while admitted."
  [ground-truth]
  (for [{:keys [event world-after]} (engine/replay ground-truth)
        [mrn {:keys [status location]}] world-after
        :when (and (= status :admitted) (or (nil? location) (nil? (:bed location))))]
    {:invariant :admitted-occupies-one-slot :mrn mrn :at (:t event)}))

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
  (for [{:keys [event world-before]} (engine/replay ground-truth)
        :when (and (#{:admission :transfer} (:event event))
                   (= :surge (get-in event [:location :placement]))
                   (not (:forced event))
                   (not (earlier-rungs-exhausted? facility-config
                                                  (facility/occupancy-board world-before)
                                                  (:home-ward event)
                                                  (get-in event [:location :ward]))))]
    {:invariant :surge-only-when-earlier-rungs-exhausted :mrn (:mrn event) :at (:t event)}))

(def catalog
  "The full invariant catalog, in reporting order."
  [#'timestamps-monotone
   #'discharge-follows-admission
   #'admission-only-when-new
   #'transfer-only-when-admitted
   #'transfer-from-matches-state
   #'no-double-occupancy
   #'admitted-occupies-one-slot])

(def facility-catalog
  "Invariants that need the facility config, not just the log (checked
  separately from `catalog` because their function signature differs
  -- `check-all` runs both)."
  [#'occupancy-within-capacity
   #'surge-only-when-earlier-rungs-exhausted])

(defn check-all
  "Runs every invariant in the catalog over a ground-truth log.
  `facility-config` (default config/default-facility) is needed by the
  capacity/surge-ladder invariants; existing 1-arg call sites are
  unaffected."
  ([ground-truth] (check-all ground-truth config/default-facility))
  ([ground-truth facility-config]
   (let [base-violations (into [] (mapcat #(% ground-truth)) catalog)
         facility-violations (into [] (mapcat #(% ground-truth facility-config)) facility-catalog)
         violations (into base-violations facility-violations)]
     (if (empty? violations)
       (result/ok {:invariants-checked (into (mapv (comp :name meta) catalog)
                                              (mapv (comp :name meta) facility-catalog))
                   :events (count ground-truth)})
       (result/rejected :invariant-violation {:violations violations})))))
