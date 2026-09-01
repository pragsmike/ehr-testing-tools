(ns ehrt.sim-model.facility
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
  "The derived index: bed-id -> patient-id (sim/ADR-0010; was bed-id -> mrn
  before identity moved off :mrn), folded from patient states. This IS
  the consistency law stated as code: recomputing from `patients` from
  scratch always equals this."
  [patients]
  (into {}
        (keep (fn [[patient-id patient]]
                (when-let [bed (get-in patient [:location :bed])]
                  [bed patient-id])))
        patients))

(defn free
  "Candidate ids from `ids` that are AVAILABLE to allocate.

  ARC 3B SWEEP 2 (ADR-0174 section 2(c)): what \"available\" means
  depends on whether this run carries a bed-status index. With `beds`
  ABSENT (nil -- no `:bed-cycle` opt-in, or a hand-built world), it is
  what it has always been: not a key in `board`, i.e. nobody is in it.
  With `beds` PRESENT, it is `:ready`, which is strictly narrower --
  a bed a patient left an hour ago is not free until housekeeping has
  turned it.

  ONE function, one branch, both readings -- not two. `allocate`'s
  four rungs, `bed-ready-location`'s own rung-1 probe and
  `ehrt.sim-check.check`'s `earlier-rungs-exhausted?` all ask this
  question, and if they asked it in three copies the cycle would have
  to be taught to three places. PUBLIC for exactly that reason: the two
  callers outside this namespace need the SAME predicate, not a
  same-looking one.

  `board` is still consulted on the index path, and deliberately: the
  index's `:occupied` half is derivable from `patients` (`occupancy-
  board`'s own consistency law), so the two agree by construction --
  and an `and` of two readings that agree is the cheapest place to
  notice if they ever stop."
  [ids board beds]
  (if (nil? beds)
    (remove board ids)
    (remove board (filter #(= :ready (:status (get beds %))) ids))))

(defn initial-beds
  "The bed-status index a `:bed-cycle` run starts from (ADR-0174 section
  2(c)): EVERY licensed bed and EVERY surge slot the facility declares,
  born `:ready` at t 0. bed-id -> {:status :since-t :last-patient-id}.

  Enumerated from `licensed-bed-ids`/`surge-slot-ids`, never from a
  list a config author maintains by hand -- the same derived-never-
  enumerated rule those two functions already state.

  `:last-patient-id` is ABSENT at run start rather than nil: no patient
  has left this bed, and a key that is not there says that more
  honestly than a key holding nothing."
  [facility]
  (into {}
        (for [ward (:wards facility)
              bed (concat (licensed-bed-ids ward) (surge-slot-ids ward))]
          [bed {:status :ready :since-t 0}])))

(defn ward-of-bed
  "The ward NAME whose licensed beds or surge slots include `bed`, or
  nil. Derived from the same two id functions the index is built from,
  so a bed id and its ward can never disagree."
  [facility bed]
  (:name (first (filter (fn [ward]
                          (or (some #{bed} (licensed-bed-ids ward))
                              (some #{bed} (surge-slot-ids ward))))
                        (:wards facility)))))

(defn- choose
  "Uniform seeded choice among `candidates` (a non-empty vector),
  consuming exactly one RNG draw regardless of candidate count --
  same determinism law as every other stochastic choice in the theory."
  [rng candidates]
  (nth candidates (.nextInt rng (count candidates))))

(defn bed-placement
  "Which ladder rung a bed id belongs to -- `:licensed` or `:surge` --
  found by searching the facility's own derived id lists rather than by
  parsing the id string. nil for a bed no ward declares.

  ARC 3B SWEEP 2 (ADR-0174 section 2(c)): `decide :bed-ready` needs a
  full `Location` for the bed it is handing over, and `:placement` is
  the third of its three fields. Deriving it here is what keeps
  `bed-ready-location`'s surge/licensed reasoning reading the same
  placement the original allocation wrote."
  [facility bed]
  (some (fn [ward]
          (cond
            (some #{bed} (licensed-bed-ids ward)) :licensed
            (some #{bed} (surge-slot-ids ward)) :surge))
        (:wards facility)))

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
  home-ward-name} -- result-not-throw (Task 0): callers
  (ehrt.sim-engine.decide/decide)
  turn this into a structured outcome rather than catching an
  exception. No RNG draw occurs on the exhausted path, same as the
  exception it replaces -- exhaustion is discovered before any `choose`
  call, so this change does not alter RNG consumption for any run that
  doesn't actually exhaust the facility.

  ARC 3B SWEEP 2 (ADR-0174 section 2(c)): the 6-arity takes the run's
  bed-status index and every rung inherits the `:ready` gate AT ONCE,
  through `free` -- the ladder's shape, its rung order and its per-rung
  `choose` draw are all untouched. The 5-arity is the whole of the
  no-`:bed-cycle` path and passes `beds` nil, which is exactly what a
  hand-built world looks like; it is a delegation and not a copy, so
  the two readings can never drift."
  ([rng facility board home-ward-name force-placement]
   (allocate rng facility board nil home-ward-name force-placement))
  ([rng facility board beds home-ward-name force-placement]
   (if force-placement
     (let [{:keys [ward bed]} force-placement
           w (ward-by-name facility ward)]
       {:home-ward home-ward-name
        :location {:ward ward :bed bed :placement (placement-of w bed)}
        :forced true})
     (let [home-ward (ward-by-name facility home-ward-name)
           home-licensed (free (licensed-bed-ids home-ward) board beds)
           home-surge (free (surge-slot-ids home-ward) board beds)]
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
               other-candidates (mapcat (fn [w] (map (fn [b] [w b]) (free (licensed-bed-ids w) board beds)))
                                        other-inpatient)]
           (if (seq other-candidates)
             (let [[w b] (choose rng other-candidates)]
               {:home-ward home-ward-name
                :location {:ward (:name w) :bed b :placement :licensed}
                :forced false})
             (let [ed-wards (filter #(= :ed (:class %)) (:wards facility))
                   ed-candidates (mapcat (fn [w] (map (fn [b] [w b]) (free (surge-slot-ids w) board beds)))
                                         ed-wards)]
               (if (seq ed-candidates)
                 (let [[w b] (choose rng ed-candidates)]
                   {:home-ward home-ward-name
                    :location {:ward (:name w) :bed b :placement :surge}
                    :forced false})
                 {:exhausted true :home-ward home-ward-name})))))))))

(defn choose-attending
  "Seeded uniform sample among providers eligible for `ward-id` (a
  keyword, matching a provider's :wards vector) -- docs/operational-
  models.md's 'ward-eligible providers' rule. Returns the provider id."
  [rng providers ward-id]
  (let [eligible (filterv #(some #{ward-id} (:wards %)) providers)]
    (:id (choose rng eligible))))
