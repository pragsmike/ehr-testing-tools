(ns ehrt.sim.facility
  "The occupancy board and the four-rung allocation ladder
  (docs/operational-models.md). The board is NEVER an independent
  structure the engine writes to -- it is a pure fold over patient
  states (`occupancy-board`), recomputed on demand every time `decide`
  needs it (`sim/ADR-0008`'s own pattern, applied to beds: one authoritative
  record -- patient state -- everything else a projection with a
  proven consistency law).

  Ward matching throughout is by NAME (the string a pathway names,
  e.g. \"Renal\") -- the same string `:home-ward` and `:location`'s
  `:ward` carry on patient state and ground-truth events
  (docs/patient-state-model.md). Provider-ward eligibility, by
  contrast, is keyed by ward ID (a keyword, e.g. :renal) -- config's
  own `:wards` vectors on providers -- so `choose-attending` takes an
  id, not a name; callers cross the boundary via `ward-by-name`."
  (:require [clojure.string :as str]))

(defn ward-by-name
  [facility ward-name]
  (first (filter #(= ward-name (:name %)) (:wards facility))))

(defn ward-by-id
  [facility ward-id]
  (first (filter #(= ward-id (:id %)) (:wards facility))))

(defn- ward-tag
  [ward]
  (str/upper-case (name (:id ward))))

(defn licensed-bed-ids
  "Derived, never enumerated (docs/operational-models.md): ward tag +
  2-digit index, e.g. a ward :id :renal with :beds 3 derives
  [\"RENAL-01\" \"RENAL-02\" \"RENAL-03\"]."
  [ward]
  (mapv #(format "%s-%02d" (ward-tag ward) %) (range 1 (inc (:beds ward)))))

(defn surge-slot-ids
  "Derived using the ward's own :surge-format -- surge naming is site-
  idiosyncratic config, not code (docs/operational-models.md)."
  [ward]
  (mapv #(format (:surge-format ward) (ward-tag ward) %) (range 1 (inc (:surge-slots ward)))))

(defn occupancy-board
  "The derived index: bed-id -> patient-id (ADR-0010; was bed-id -> mrn
  before identity moved off :mrn), folded from patient states. This IS
  the consistency law stated as code: recomputing from `patients` from
  scratch always equals this."
  [patients]
  (into {}
        (keep (fn [[patient-id patient]]
                (when-let [bed (get-in patient [:location :bed])]
                  [bed patient-id])))
        patients))

(defn- free
  "Candidate ids from `ids` not present as a key in `board`."
  [ids board]
  (remove board ids))

(defn- choose
  "Uniform seeded choice among `candidates` (a non-empty vector),
  consuming exactly one RNG draw regardless of candidate count --
  same determinism law as every other stochastic choice in the theory."
  [rng candidates]
  (nth candidates (.nextInt rng (count candidates))))

(defn- placement-of
  [ward bed]
  (cond
    (some #{bed} (licensed-bed-ids ward)) :licensed
    (some #{bed} (surge-slot-ids ward)) :surge
    :else (throw (ex-info "force-placement bed is not a valid id for this ward"
                          {:ward (:name ward) :bed bed}))))

(defn ward-census
  "A snapshot of every ward's occupancy against its declared capacity
  (licensed + surge), keyed by ward name -- the diagnostic payload
  Task 0's capacity-exhausted outcome carries (result-not-throw:
  ehr-testing-tools' allocate no longer throws when every ladder rung
  is exhausted, docs/clinical-realities.md's ED-diversion stub)."
  [facility board]
  (into {}
        (map (fn [ward]
               (let [slots (into #{} (concat (licensed-bed-ids ward) (surge-slot-ids ward)))]
                 [(:name ward)
                  {:occupied (count (filter slots (keys board)))
                   :capacity (+ (:beds ward) (:surge-slots ward))}])))
        (:wards facility)))

(defn allocate
  "The four-rung allocation ladder (docs/operational-models.md), seeded
  within each rung. `force-placement` (optional `{:ward :bed}`)
  overrides the ladder outright and draws no RNG. Returns
  {:home-ward :location {:ward :bed :placement} :forced}, or, when
  every rung is legitimately exhausted, {:exhausted true :home-ward
  home-ward-name} -- result-not-throw (Task 0): callers (engine/decide)
  turn this into a structured outcome rather than catching an
  exception. No RNG draw occurs on the exhausted path, same as the
  exception it replaces -- exhaustion is discovered before any `choose`
  call, so this change does not alter RNG consumption for any run that
  doesn't actually exhaust the facility."
  [rng facility board home-ward-name force-placement]
  (if force-placement
    (let [{:keys [ward bed]} force-placement
          w (ward-by-name facility ward)]
      {:home-ward home-ward-name
       :location {:ward ward :bed bed :placement (placement-of w bed)}
       :forced true})
    (let [home-ward (ward-by-name facility home-ward-name)
          home-licensed (free (licensed-bed-ids home-ward) board)
          home-surge (free (surge-slot-ids home-ward) board)]
      (cond
        (seq home-licensed)
        {:home-ward home-ward-name
         :location {:ward home-ward-name :bed (choose rng home-licensed) :placement :licensed}
         :forced false}

        (seq home-surge)
        {:home-ward home-ward-name
         :location {:ward home-ward-name :bed (choose rng home-surge) :placement :surge}
         :forced false}

        :else
        (let [other-inpatient (remove #(= (:id %) (:id home-ward))
                                       (filter #(= :inpatient (:class %)) (:wards facility)))
              other-candidates (mapcat (fn [w] (map (fn [b] [w b]) (free (licensed-bed-ids w) board)))
                                       other-inpatient)]
          (if (seq other-candidates)
            (let [[w b] (choose rng other-candidates)]
              {:home-ward home-ward-name
               :location {:ward (:name w) :bed b :placement :licensed}
               :forced false})
            (let [ed-wards (filter #(= :ed (:class %)) (:wards facility))
                  ed-candidates (mapcat (fn [w] (map (fn [b] [w b]) (free (surge-slot-ids w) board)))
                                        ed-wards)]
              (if (seq ed-candidates)
                (let [[w b] (choose rng ed-candidates)]
                  {:home-ward home-ward-name
                   :location {:ward (:name w) :bed b :placement :surge}
                   :forced false})
                {:exhausted true :home-ward home-ward-name}))))))))

(defn choose-attending
  "Seeded uniform sample among providers eligible for `ward-id` (a
  keyword, matching a provider's :wards vector) -- docs/operational-
  models.md's 'ward-eligible providers' rule. Returns the provider id."
  [rng providers ward-id]
  (let [eligible (filterv #(some #{ward-id} (:wards %)) providers)]
    (:id (choose rng eligible))))
