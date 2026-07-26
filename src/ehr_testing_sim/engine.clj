(ns ehr-testing-sim.engine
  "The discrete-event simulation core: a priority queue of pending
  events ordered by simulated time, a world of patient states (plus,
  from Milestone M1 on, the static facility/provider config decide
  needs to read), and the decide/evolve pair (ADR-0008) that replaces
  a single fused transition function. Architecture mined from Google's
  Simulated Hospital (pkg/state WrappedQueue + pkg/hospital
  RunNextEventIfDue).

  Event-sourcing doctrine (ADR-0008): the ground-truth log is the only
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

  Identity doctrine (ADR-0010, M2a): `:patient-id` -- not `:mrn` -- is
  the fold key and the work-queue key. `:mrn` moves into state as
  {:mrns #{...} :active-mrn ...}, because a real hospital's MRN is
  exactly the identifier merge (M2b) changes; patient-id never
  reassigns and never rebinds. Every event carries `:participants`, a
  vector of {:patient-id :role} -- single-element with role :subject
  for every event type this project has today (the degenerate case);
  a patient's state folds exactly the events they participate in.
  `patient-id-for` is a PURE function of this run's seed and a
  patient's arrival ordinal -- deliberately off the seeded RNG stream,
  so identity generation adds no new stochastic draws for ADR-0009's
  seed-stability accounting to track (unlike NPI generation, which IS
  an RNG draw, ADR-0007).

  Time doctrine (ADR-0011, M2a): the engine clock (every event's :t) is
  now integer SECONDS from run start, not minutes. The pathway IR is
  NOT changed -- :delay's :from/:to stay minutes, authoring ergonomics
  -- the engine converts minutes -> seconds itself, at the one place a
  minute-denominated draw becomes a clock advance. A warm-up window
  (:warm-up-seconds, default 0) marks every event with `:t <
  warm-up-seconds` as `:warm-up true`; the log stays complete (no
  trimming here -- ADR-0011 leaves trimming, if any, to Package).

  Determinism doctrine: ALL randomness flows from the single
  java.util.Random seeded in `run`. No other entropy source (wall
  clock, hash ordering, nondeterministic seq realization) may
  influence output. Same config + seed => identical output, byte for
  byte once serialized -- WITHIN a version; see notes/ADRs.md ADR-0009
  for the cross-version seed-stability policy Milestone M1's new RNG
  draws (bed choice, attending sampling) triggered, and M2a's identity/
  time changes triggered again (documented once, per the M2a session
  plan, not per-commit).

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
  is the full design spec). `:patient-id` is the fold/queue key
  (ADR-0010); `:mrns`/`:active-mrn` are what `:mrn` became once MRN
  moved into state -- a singleton set until M2b's merge exists to grow
  it. As of Milestone M1: :location is the {:ward :bed :placement} map
  (upgraded from v0's bare ward-name string, alongside the allocation
  ladder that populates it for real); :class/:attending/:payer/
  :admitted-at are populated at admission; :attributes remains
  reserved, unused until M5."
  [:map
   [:patient-id :string]
   [:mrns [:set :string]]
   [:active-mrn :string]
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
  property) start from the same place the engine itself does.
  ADR-0010: keyed by `patient-id`, not `mrn` -- `mrn` is the patient's
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
  "The internal, deterministic patient-id (ADR-0010): a PURE function
  of this run's seed and the patient's arrival ordinal (0-indexed) --
  never reassigned, never re-derived elsewhere, and deliberately OFF
  the seeded RNG stream (identity needs no stochastic behavior, only
  spread across seeds -- keeping it off the RNG means identity
  generation adds no new draws for ADR-0009's accounting to track).
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
  to mean before ADR-0010's :participants existed. An event with more
  than one participant (M2b's bed-swap, merge) appears in every
  participant's own sequence, not just one."
  [ground-truth patient-id]
  (filterv (fn [event] (some #(= patient-id (:patient-id %)) (:participants event)))
           ground-truth))

(defmulti decide
  "Decides what happens when patient `patient-id` is due to execute
  `step` at simulated time t (SECONDS from the run's epoch, ADR-0011).
  Consults `world` ({:patients {patient-id -> patient-state} :facility
  .. :providers ..} -- read-only) and the seeded RNG to make stochastic
  and cross-patient choices; returns {:events [<ground-truth
  event>...] :advance <seconds>}. NEVER returns or implies a new
  patient state -- state changes only by folding the returned events
  through `evolve` (ADR-0008). Pure given the RNG (the RNG is the only
  stateful argument, and its consumption order is fixed by the
  deterministic event ordering)."
  (fn [_rng _t _world _patient-id step] (:type step)))

(defmethod decide :admission
  [rng t world patient-id {:keys [location reason force-placement]}]
  (let [{:keys [facility providers patients]} world
        board (facility/occupancy-board patients)
        {:keys [home-ward] :as alloc} (facility/allocate rng facility board location force-placement)
        ward-id (:id (facility/ward-by-name facility home-ward))
        attending (facility/choose-attending rng providers ward-id)
        active-mrn (get-in patients [patient-id :active-mrn])]
    {:events [(merge {:event :admission :t t :active-mrn active-mrn :reason reason :attending attending
                      :participants [{:patient-id patient-id :role :subject}]}
                     alloc)]
     :advance 0}))

(defmethod decide :delay
  [rng _t _world _patient-id {:keys [from to]}]
  ;; :from/:to are authored in MINUTES (pathway.clj IR, unchanged --
  ;; ADR-0011 decision 1's authoring-ergonomics carve-out); the engine
  ;; converts to SECONDS here, the one place a minute-denominated draw
  ;; becomes a clock advance.
  {:events []
   :advance (* 60 (rand-int-in rng from to))})

(defmethod decide :transfer
  [rng t world patient-id {:keys [location force-placement]}]
  (let [{:keys [facility patients]} world
        board (facility/occupancy-board patients)
        patient (get patients patient-id)
        alloc (facility/allocate rng facility board location force-placement)]
    {:events [(merge {:event :transfer :t t :active-mrn (:active-mrn patient) :from (:location patient)
                      :attending (:attending patient) :bed-ready false
                      :participants [{:patient-id patient-id :role :subject}]}
                     alloc)]
     :advance 0}))

(defmethod decide :discharge
  [_rng t world patient-id _step]
  (let [patient (get-in world [:patients patient-id])
        discharge-event {:event :discharge :t t :active-mrn (:active-mrn patient)
                          :location (:location patient) :attending (:attending patient)
                          :participants [{:patient-id patient-id :role :subject}]}
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

(defmulti evolve
  "Folds one ground-truth event into ONE patient it names:
  (patient-state, event) -> patient-state'. Pure and total: no RNG, no
  knowledge of the step or decision that produced the event, no
  knowledge of `world` or any other patient. This is the ONLY function
  that ever produces a new patient state (ADR-0008) -- the run loop is
  what maps an event to every participant's slice of `world` (via the
  event's own :participants, ADR-0010) and folds `evolve` in there,
  once per participant."
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
  {:event :patient-id :before :after :world-before :world-after} --
  `:patient-id` is a convenience view of the event's PRIMARY (first)
  participant, since every check.clj invariant needs at most one
  patient's pre/post state even once M2b's bed-swap/merge span two
  (cross-participant invariants read world-before/world-after
  directly instead). Every participant in :participants folds via
  `evolve`, not just the primary one -- ADR-0010: a patient's state
  folds exactly the events they participate in. `world-before`/
  `world-after` are the full {patient-id -> patient-state} map
  immediately before/after this event (ADR-0008: state-history is
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

(defn- pop-min
  "Removes and returns the earliest queue entry. Queue is a sorted-map
  keyed by [t seq-no] -- the seq-no tiebreak makes ordering total, so
  RNG consumption order (and thus output) is fully determined."
  [queue]
  (let [[k v] (first queue)]
    [k v (dissoc queue k)]))

(defn run
  "Runs the simulation. config:
    :seed             long (required)
    :patients         number of patients (default 1)
    :pathway          a pathway IR map (default pathway/sample-admission-discharge)
    :arrival-gap      max MINUTES between successive patient arrivals
                      (default 60; actual gaps sampled from the seeded
                      RNG). Stays minutes, converted to seconds at the
                      point arrivals are computed -- symmetric to
                      :delay's own minutes-authored/seconds-internal
                      split (ADR-0011), and empirically necessary: an
                      earlier draft left this in raw seconds while
                      :delay's dwell times (minutes*60) stayed
                      comparatively huge, so arrivals clustered far
                      faster than patients discharged and blew past
                      config/default-facility's real usable capacity
                      (16 concurrent, not its nominal 18 -- Cardiology's
                      surge sits unused when every patient's home-ward
                      is Renal) at patient counts the property tests
                      already exercised. Keeping both minutes-scaled
                      preserves the calibration that made that headroom
                      real.
    :warm-up-seconds  events with :t less than this get :warm-up true
                      (default 0; ADR-0011 -- the log stays complete,
                      no trimming here)
    :facility         facility config (default config/default-facility)
    :providers        provider templates (default config/default-provider-templates;
                       NPIs are generated from THIS run's seed -- ADR-0007)

  Returns {:ground-truth [event ...] :state-history {patient-id [state
  ...]} :facility .. :providers [materialized-provider ...]}. The
  facility and MATERIALIZED providers (real NPIs, not just templates)
  are echoed back so a caller rendering this run's log
  (ehr-testing-sim.emit-hl7/emit needs facility + providers for PV1)
  uses the EXACT config this run allocated against, not a fresh default
  that might not even share ward names. ground-truth is format-free,
  ordered by [t seq-no]; emitters consume it and test assertions target
  it directly (a first-class output, per the problem statement).
  state-history is DERIVED (ADR-0008; sim-theory.md open question #3)
  -- (get state-history patient-id) is exactly (rest (reductions evolve
  (initial-patient patient-id mrn) (events for patient-id))), proven as
  a property test (engine-test/patient-state-is-a-fold-of-the-log)
  rather than assumed; the engine computes it as a byproduct of the
  loop below because decide needs live world state to make its next
  decision, not because it's a second source of truth."
  [{:keys [seed patients pathway arrival-gap warm-up-seconds facility providers]
    :or {patients 1
         pathway pathway/sample-admission-discharge
         arrival-gap 60
         warm-up-seconds 0
         facility config/default-facility
         providers config/default-provider-templates}}]
  {:pre [(some? seed) (pathway/valid? pathway)]}
  (let [rng (Random. ^long seed)
        ;; Provider NPIs are generated from this run's seed (ADR-0007),
        ;; drawn once up front -- before arrival staggering -- so
        ;; provider identity is as deterministic and as fixed-order as
        ;; everything else this RNG produces.
        materialized-providers (config/materialize-providers rng providers)
        ;; Stagger arrivals: :arrival-gap is authored in MINUTES (same
        ;; carve-out as :delay's IR, and for the same calibration
        ;; reason -- see `run`'s docstring); the engine converts to
        ;; SECONDS here. Consume RNG in patient order (fixed).
        arrivals (vec (reductions + 0 (repeatedly (dec patients)
                                                  #(* 60 (rand-int-in rng 0 arrival-gap)))))
        mrn-for (fn [i] (format "MRN%06d" (inc i)))
        pid-for (fn [i] (patient-id-for seed i))
        init-queue (into (sorted-map)
                         (map-indexed
                          (fn [i arrival-t]
                            [[arrival-t i]
                             {:patient-id (pid-for i) :steps (:steps pathway)}])
                          arrivals))
        init-world {:patients (into {} (map-indexed (fn [i _] [(pid-for i) (initial-patient (pid-for i) (mrn-for i))]))
                                    arrivals)
                    :facility facility
                    :providers materialized-providers}
        mark-warmup (fn [ev] (assoc ev :warm-up (< (:t ev) warm-up-seconds)))]
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
        (let [[[t _] {:keys [patient-id steps]} queue'] (pop-min queue)
              [step & remaining] steps
              {:keys [events advance]} (decide rng t world patient-id step)
              events (mapv mark-warmup events)
              world' (reduce (fn [w ev]
                                (reduce (fn [w2 {:keys [patient-id]}]
                                          (update-in w2 [:patients patient-id] evolve ev))
                                        w (:participants ev)))
                              world events)
              ground-truth' (reduce conj! ground-truth events)
              state-history' (reduce (fn [sh ev]
                                        (reduce (fn [sh2 {:keys [patient-id]}]
                                                  (update sh2 patient-id (fnil conj [])
                                                          (get-in world' [:patients patient-id])))
                                                sh (:participants ev)))
                                      state-history events)]
          (if (seq remaining)
            (recur (assoc queue' [(+ t advance) seq-no] {:patient-id patient-id :steps (vec remaining)})
                   (inc seq-no) world' ground-truth' state-history')
            (recur queue' seq-no world' ground-truth' state-history')))))))
