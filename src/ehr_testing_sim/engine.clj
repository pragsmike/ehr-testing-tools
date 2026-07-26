(ns ehr-testing-sim.engine
  "The discrete-event simulation core: a priority queue of pending
  events ordered by simulated time, a world of patient states (plus,
  from Milestone M1 on, the static facility/provider config decide
  needs to read), and the decide/evolve pair (ADR-0008) that replaces
  a single fused transition function. Architecture mined from Google's
  Simulated Hospital (pkg/state WrappedQueue + pkg/hospital
  RunNextEventIfDue).

  Event-sourcing doctrine (ADR-0008): the ground-truth log is the only
  primitive. `decide` (rng, t, world, mrn, step) -> {:events :advance}
  consults the current world (every patient's state so far, plus
  facility/provider config -- read-only) and the run's single RNG to
  decide what happens, but never returns a new state -- this is where
  cross-patient coupling lives (a discharge's decide call may also
  emit a transfer event for a DIFFERENT patient, the bed-ready
  transfer for a boarding patient, docs/operational-models.md).
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
  byte once serialized -- WITHIN a version; see notes/ADRs.md ADR-0009
  for the cross-version seed-stability policy Milestone M1's new RNG
  draws (bed choice, attending sampling) triggered.

  Step vocabulary: v0's :admission/:delay/:discharge, plus Milestone
  M1's :transfer (docs/operational-models.md's allocation ladder).
  Emission to HL7v2 is a separate namespace consuming the ground-truth
  log -- events here are format-free."
  (:require [ehr-testing-sim.pathway :as pathway]
            [ehr-testing-sim.config :as config]
            [ehr-testing-sim.facility :as facility]
            [malli.core :as m])
  (:import [java.util Random]))

(def PatientState
  "The engine's per-patient accumulator -- what folding `evolve` over a
  patient's own event subsequence produces (docs/patient-state-model.md
  is the full design spec). As of Milestone M1: :location is the
  {:ward :bed :placement} map (upgraded from v0's bare ward-name
  string, alongside the allocation ladder that populates it for real);
  :class/:attending/:payer/:admitted-at are populated at admission;
  :attributes remains reserved, unused until M5."
  [:map
   [:mrn :string]
   [:status [:enum :new :admitted :discharged]]
   [:class {:optional true} [:enum :inpatient :emergency :outpatient
                              :preadmit :recurring :obstetrics]]
   [:home-ward {:optional true} [:maybe :string]]
   [:location {:optional true} [:maybe [:map
                                         [:ward :string]
                                         [:bed :string]
                                         [:placement [:enum :licensed :surge]]]]]
   [:attending {:optional true} [:maybe :string]]
   [:payer {:optional true} [:maybe :string]]
   [:admitted-at {:optional true} [:maybe :int]]
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
  ({:patients {mrn -> patient-state} :facility .. :providers ..} --
  read-only) and the seeded RNG to make stochastic and cross-patient
  choices; returns {:events [<ground-truth event>...] :advance
  <minutes>}. NEVER returns or implies a new patient state -- state
  changes only by folding the returned events through `evolve`
  (ADR-0008). Pure given the RNG (the RNG is the only stateful
  argument, and its consumption order is fixed by the deterministic
  event ordering)."
  (fn [_rng _t _world _mrn step] (:type step)))

(defmethod decide :admission
  [rng t world mrn {:keys [location reason force-placement]}]
  (let [{:keys [facility providers patients]} world
        board (facility/occupancy-board patients)
        {:keys [home-ward] :as alloc} (facility/allocate rng facility board location force-placement)
        ward-id (:id (facility/ward-by-name facility home-ward))
        attending (facility/choose-attending rng providers ward-id)]
    {:events [(merge {:event :admission :t t :mrn mrn :reason reason :attending attending}
                     alloc)]
     :advance 0}))

(defmethod decide :delay
  [rng _t _world _mrn {:keys [from to]}]
  {:events []
   :advance (rand-int-in rng from to)})

(defmethod decide :transfer
  [rng t world mrn {:keys [location force-placement]}]
  (let [{:keys [facility patients]} world
        board (facility/occupancy-board patients)
        patient (get patients mrn)
        alloc (facility/allocate rng facility board location force-placement)]
    {:events [(merge {:event :transfer :t t :mrn mrn :from (:location patient)
                      :attending (:attending patient) :bed-ready false}
                     alloc)]
     :advance 0}))

(defmethod decide :discharge
  [_rng t world mrn _step]
  (let [patient (get-in world [:patients mrn])
        discharge-event {:event :discharge :t t :mrn mrn
                          :location (:location patient) :attending (:attending patient)}
        vacated-ward (get-in patient [:location :ward])
        vacated-location (:location patient)
        waiting-mrn (->> (:patients world)
                          (remove (fn [[m _]] (= m mrn)))
                          (filter (fn [[_ p]] (and (= :admitted (:status p))
                                                    (not= (:home-ward p) (get-in p [:location :ward]))
                                                    (= vacated-ward (:home-ward p)))))
                          (sort-by (fn [[m p]] [(:admitted-at p) m]))
                          ffirst)]
    {:events (cond-> [discharge-event]
               waiting-mrn
               (conj {:event :transfer :t t :mrn waiting-mrn
                      :from (:location (get-in world [:patients waiting-mrn]))
                      :attending (:attending (get-in world [:patients waiting-mrn]))
                      :home-ward (get-in world [:patients waiting-mrn :home-ward])
                      :location vacated-location
                      :placement (:placement vacated-location)
                      :forced false
                      :bed-ready true}))
     :advance 0}))

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
  [patient {:keys [location home-ward attending t]}]
  (assoc patient
         :status :admitted
         :class :inpatient
         :home-ward home-ward
         :location location
         :attending attending
         :admitted-at t))

(defmethod evolve :transfer
  [patient {:keys [location home-ward]}]
  (assoc patient :location location :home-ward home-ward))

(defmethod evolve :discharge
  [patient _event]
  (assoc patient :status :discharged :location nil))

(defn replay
  "Replays `ground-truth` through `evolve`, returning a parallel seq of
  {:event :mrn :before :after :world-before :world-after} -- ONE place
  both check.clj's cross-patient invariants and emit-hl7's PV1
  derivation get 'patient/world state at any point in the log' from,
  rather than each reimplementing the fold. `world-before`/
  `world-after` are the full {mrn -> patient-state} map immediately
  before/after this event; `before`/`after` are that same event's own
  mrn's state, for convenience (ADR-0008: state-history is derived --
  this IS that derivation, generalized across patients)."
  [ground-truth]
  (loop [events ground-truth patients {} acc (transient [])]
    (if (empty? events)
      (persistent! acc)
      (let [event (first events)
            mrn (:mrn event)
            before (get patients mrn (initial-patient mrn))
            after (evolve before event)
            patients' (assoc patients mrn after)]
        (recur (rest events) patients'
               (conj! acc {:event event :mrn mrn :before before :after after
                           :world-before patients :world-after patients'}))))))

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
    :facility    facility config (default config/default-facility)
    :providers   provider templates (default config/default-provider-templates;
                 NPIs are generated from THIS run's seed -- ADR-0007)

  Returns {:ground-truth [event ...] :state-history {mrn [state ...]}
  :facility .. :providers [materialized-provider ...]}. The facility
  and MATERIALIZED providers (real NPIs, not just templates) are
  echoed back so a caller rendering this run's log (ehr-testing-sim
  .emit-hl7/emit needs facility + providers for PV1) uses the EXACT
  config this run allocated against, not a fresh default that might
  not even share ward names. ground-truth is format-free, ordered by
  [t seq-no]; emitters consume it and test assertions target it
  directly (a first-class output, per the problem statement).
  state-history is DERIVED (ADR-0008; sim-
  theory.md open question #3) -- (get state-history mrn) is exactly
  (rest (reductions evolve (initial-patient mrn) (events for mrn))),
  proven as a property test (engine-test/patient-state-is-a-fold-of-
  the-log) rather than assumed; the engine computes it as a byproduct
  of the loop below because decide needs live world state to make its
  next decision, not because it's a second source of truth."
  [{:keys [seed patients pathway arrival-gap facility providers]
    :or {patients 1
         pathway pathway/sample-admission-discharge
         arrival-gap 60
         facility config/default-facility
         providers config/default-provider-templates}}]
  {:pre [(some? seed) (pathway/valid? pathway)]}
  (let [rng (Random. ^long seed)
        ;; Provider NPIs are generated from this run's seed (ADR-0007),
        ;; drawn once up front -- before arrival staggering -- so
        ;; provider identity is as deterministic and as fixed-order as
        ;; everything else this RNG produces.
        materialized-providers (config/materialize-providers rng providers)
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
        init-world {:patients (into {} (map-indexed (fn [i _] [(mrn-for i) (initial-patient (mrn-for i))]))
                                    arrivals)
                    :facility facility
                    :providers materialized-providers}]
    (loop [queue init-queue
           seq-no patients
           world init-world
           ground-truth (transient [])
           state-history {}]
      (if (empty? queue)
        {:ground-truth (persistent! ground-truth)
         :state-history state-history
         :facility facility
         :providers materialized-providers}
        (let [[[t _] {:keys [mrn steps]} queue'] (pop-min queue)
              [step & remaining] steps
              {:keys [events advance]} (decide rng t world mrn step)
              world' (reduce (fn [w ev] (update-in w [:patients (:mrn ev)] evolve ev)) world events)
              ground-truth' (reduce conj! ground-truth events)
              state-history' (reduce (fn [sh ev]
                                        (update sh (:mrn ev) (fnil conj [])
                                                (get-in world' [:patients (:mrn ev)])))
                                      state-history events)]
          (if (seq remaining)
            (recur (assoc queue' [(+ t advance) seq-no] {:mrn mrn :steps (vec remaining)})
                   (inc seq-no) world' ground-truth' state-history')
            (recur queue' seq-no world' ground-truth' state-history')))))))
