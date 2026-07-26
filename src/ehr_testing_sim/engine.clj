(ns ehr-testing-sim.engine
  "The discrete-event simulation core: a priority queue of pending
  events ordered by simulated time, a world of patient states, and the
  decide/evolve pair (ADR-0008) that replaces a single fused
  transition function. Architecture mined from Google's Simulated
  Hospital (pkg/state WrappedQueue + pkg/hospital RunNextEventIfDue).

  Event-sourcing doctrine (ADR-0008): the ground-truth log is the only
  primitive. `decide` (rng, t, world, mrn, step) -> {:events :advance}
  consults the current world (every patient's state so far) and the
  run's single RNG to decide what happens, but never returns a new
  state -- this is where cross-patient coupling lives (a discharge's
  decide call may also emit a transfer event for a DIFFERENT patient).
  `evolve` (patient-state, event) -> patient-state' is pure, total, and
  narrower: no RNG, no knowledge of world or of any patient but the one
  the event names. The ONLY path by which patient state changes is
  folding emitted events through `evolve`; docs/patient-state-model.md
  is PatientState's design spec, docs/sim-theory.md open question #3
  (state-history is derived, not primitive) is this ADR's resolution.

  Determinism doctrine: ALL randomness flows from the single
  java.util.Random seeded in `run`. No other entropy source (wall
  clock, hash ordering, nondeterministic seq realization) may
  influence output. Same config + seed => identical output, byte for
  byte once serialized. This is guarantee #1 of the problem statement
  and is enforced by property tests, not by review alone.

  The v0 skeleton executes :admission/:delay/:discharge pathways for a
  configured number of patients with staggered arrivals, producing the
  ground-truth trajectory log. Emission to HL7v2 is a separate
  namespace consuming this log -- events here are format-free."
  (:require [ehr-testing-sim.pathway :as pathway]
            [malli.core :as m])
  (:import [java.util Random]))

(def PatientState
  "The engine's per-patient accumulator -- what folding `evolve` over a
  patient's own event subsequence produces (docs/patient-state-model.md
  is the full design spec). :class/:home-ward/:attending/:payer/
  :attributes are reserved fields that land for real with Milestone M1
  (docs/operational-models.md) and beyond (:attributes waits on M5);
  the v0 step set (:admission/:delay/:discharge) populates only :mrn,
  :status, and :location (still a bare ward-name string here --
  :location's {:ward :bed :placement} shape is Milestone M1's own
  change, landing with the allocation ladder, not this one)."
  [:map
   [:mrn :string]
   [:status [:enum :new :admitted :discharged]]
   [:class {:optional true} [:enum :inpatient :emergency :outpatient
                              :preadmit :recurring :obstetrics]]
   [:home-ward {:optional true} [:maybe :string]]
   [:location {:optional true} [:maybe :string]]
   [:attending {:optional true} [:maybe :string]]
   [:payer {:optional true} [:maybe :string]]
   [:attributes {:optional true} [:map-of :keyword :any]]])

(defn valid-patient?
  "Validates a patient accumulator against PatientState -- the same
  valid?/explain convention ehr-testing-sim.pathway already uses."
  [patient]
  (m/validate PatientState patient))

(defn initial-patient
  "The state a patient starts in when its arrival is scheduled --
  `evolve`'s fold origin. The single place this shape is constructed,
  so tests reconstructing state independently (the fold-consistency
  property) start from the same place the engine itself does."
  [mrn]
  {:mrn mrn :status :new})

(defn- rand-int-in
  "Uniform integer in [lo, hi] from the seeded RNG."
  [^Random rng lo hi]
  (+ lo (.nextInt rng (inc (- hi lo)))))

(defmulti decide
  "Decides what happens when patient `mrn` is due to execute `step` at
  simulated time t (minutes from epoch of the run). Consults `world`
  (every patient's current state, keyed by mrn -- read-only) and the
  seeded RNG to make stochastic and cross-patient choices; returns
  {:events [<ground-truth event>...] :advance <minutes>}. NEVER returns
  or implies a new patient state -- state changes only by folding the
  returned events through `evolve` (ADR-0008). Pure given the RNG (the
  RNG is the only stateful argument, and its consumption order is
  fixed by the deterministic event ordering)."
  (fn [_rng _t _world _mrn step] (:type step)))

(defmethod decide :admission
  [_rng t _world mrn {:keys [location reason]}]
  {:events [{:event :admission :t t :mrn mrn
             :location location :reason reason}]
   :advance 0})

(defmethod decide :delay
  [rng _t _world _mrn {:keys [from to]}]
  {:events []
   :advance (rand-int-in rng from to)})

(defmethod decide :discharge
  [_rng t _world mrn _step]
  {:events [{:event :discharge :t t :mrn mrn}]
   :advance 0})

(defmulti evolve
  "Folds one ground-truth event into the one patient it names:
  (patient-state, event) -> patient-state'. Pure and total: no RNG, no
  knowledge of the step or decision that produced the event, no
  knowledge of `world` or any other patient. This is the ONLY function
  that ever produces a new patient state (ADR-0008) -- the run loop is
  what maps an event to the right patient's slice of `world` via the
  event's own :mrn and folds `evolve` in there."
  (fn [_patient event] (:event event)))

(defmethod evolve :admission
  [patient {:keys [location]}]
  (assoc patient :status :admitted :location location))

(defmethod evolve :discharge
  [patient _event]
  (assoc patient :status :discharged :location nil))

(defn- pop-min
  "Removes and returns the earliest queue entry. Queue is a sorted-map
  keyed by [t seq-no] -- the seq-no tiebreak makes ordering total, so
  RNG consumption order (and thus output) is fully determined."
  [queue]
  (let [[k v] (first queue)]
    [k v (dissoc queue k)]))

(defn run
  "Runs the simulation. config:
    :seed        long (required)
    :patients    number of patients (default 1)
    :pathway     a pathway IR map (default pathway/sample-admission-discharge)
    :arrival-gap max minutes between successive patient arrivals
                 (default 60; actual gaps sampled from the seeded RNG)

  Returns {:ground-truth [event ...] :state-history {mrn [state ...]}}.
  ground-truth is format-free, ordered by [t seq-no]; emitters consume
  it and test assertions target it directly (a first-class output, per
  the problem statement). state-history is DERIVED (ADR-0008; sim-
  theory.md open question #3) -- (get state-history mrn) is exactly
  (rest (reductions evolve (initial-patient mrn) (events for mrn))),
  proven as a property test (engine-test/patient-state-is-a-fold-of-
  the-log) rather than assumed; the engine computes it as a byproduct
  of the loop below because decide needs live world state to make its
  next decision, not because it's a second source of truth."
  [{:keys [seed patients pathway arrival-gap]
    :or {patients 1
         pathway pathway/sample-admission-discharge
         arrival-gap 60}}]
  {:pre [(some? seed) (pathway/valid? pathway)]}
  (let [rng (Random. ^long seed)
        ;; Stagger arrivals; consume RNG in patient order (fixed).
        arrivals (vec (reductions + 0 (repeatedly (dec patients)
                                                  #(rand-int-in rng 0 arrival-gap))))
        mrn-for (fn [i] (format "MRN%06d" (inc i)))
        init-queue (into (sorted-map)
                         (map-indexed
                          (fn [i arrival-t]
                            [[arrival-t i]
                             {:mrn (mrn-for i) :steps (:steps pathway)}])
                          arrivals))
        init-world (into {} (map-indexed (fn [i _] [(mrn-for i) (initial-patient (mrn-for i))]))
                         arrivals)]
    (loop [queue init-queue
           seq-no patients
           world init-world
           ground-truth (transient [])
           state-history {}]
      (if (empty? queue)
        {:ground-truth (persistent! ground-truth)
         :state-history state-history}
        (let [[[t _] {:keys [mrn steps]} queue'] (pop-min queue)
              [step & remaining] steps
              {:keys [events advance]} (decide rng t world mrn step)
              world' (reduce (fn [w ev] (update w (:mrn ev) evolve ev)) world events)
              ground-truth' (reduce conj! ground-truth events)
              state-history' (reduce (fn [sh ev]
                                        (update sh (:mrn ev) (fnil conj []) (get world' (:mrn ev))))
                                      state-history events)]
          (if (seq remaining)
            (recur (assoc queue' [(+ t advance) seq-no] {:mrn mrn :steps (vec remaining)})
                   (inc seq-no) world' ground-truth' state-history')
            (recur queue' seq-no world' ground-truth' state-history')))))))
