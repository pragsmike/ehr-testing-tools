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
            [ehr-testing-sim.churn :as churn]
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
   [:status [:enum :new :admitted :discharged :merged]]
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

(defn- exhausted-outcome
  "Task 0: result-not-throw for allocation-ladder exhaustion --
  facility/allocate no longer throws, so decide translates its
  structured {:exhausted true} into a decide-level outcome the run loop
  halts on and run-command (ehr-testing-sim.run) surfaces as :error
  :capacity-exhausted, payload {:patient-id :ward :census}."
  [patient-id home-ward-name facility board]
  {:events [] :advance 0
   :exhausted {:patient-id patient-id :ward home-ward-name
               :census (facility/ward-census facility board)}})

(defmethod decide :admission
  [rng t world patient-id {:keys [location reason force-placement]}]
  (let [{:keys [facility providers patients]} world
        board (facility/occupancy-board patients)
        alloc (facility/allocate rng facility board location force-placement)]
    (if (:exhausted alloc)
      (exhausted-outcome patient-id location facility board)
      (let [ward-id (:id (facility/ward-by-name facility (:home-ward alloc)))
            attending (facility/choose-attending rng providers ward-id)
            active-mrn (get-in patients [patient-id :active-mrn])]
        {:events [(merge {:event :admission :t t :active-mrn active-mrn :reason reason :attending attending
                          :participants [{:patient-id patient-id :role :subject}]}
                         alloc)]
         :advance 0}))))

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
    (if (:exhausted alloc)
      (exhausted-outcome patient-id location facility board)
      {:events [(merge {:event :transfer :t t :active-mrn (:active-mrn patient) :from (:location patient)
                        :attending (:attending patient) :bed-ready false
                        :participants [{:patient-id patient-id :role :subject}]}
                       alloc)]
       :advance 0})))

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

(defn- rejected-outcome
  [reason patient-id extra]
  {:events [] :advance 0 :rejected (merge {:reason reason :patient-id patient-id} extra)})

(defmethod decide :cancel-admit
  [_rng t world patient-id _step]
  (let [ground-truth (:ground-truth world)
        idx (last-uncancelled-index ground-truth patient-id :admission :cancel-admit)]
    (if (nil? idx)
      (rejected-outcome :illegal-cancel-admit patient-id nil)
      (let [patient (get-in world [:patients patient-id])]
        {:events [{:event :cancel-admit :t t :active-mrn (:active-mrn patient)
                   :cancels-event-id idx
                   :participants [{:patient-id patient-id :role :subject}]}]
         :advance 0}))))

(defmethod decide :transfer-in-error
  [rng t world patient-id {:keys [location force-placement]}]
  (let [{:keys [facility patients ground-truth]} world
        board (facility/occupancy-board patients)
        patient (get patients patient-id)
        alloc (facility/allocate rng facility board location force-placement)]
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
  [rng t world patient-id {:keys [with]}]
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
      (rejected-outcome :illegal-bed-swap patient-id {:with with})
      {:events [{:event :bed-swap :t t
                 :participants [{:patient-id patient-id :role :subject}
                                {:patient-id peer-id :role :subject}]
                 :swap {patient-id {:active-mrn (:active-mrn self) :from (:location self)
                                    :to (:location peer) :attending (:attending self)}
                        peer-id {:active-mrn (:active-mrn peer) :from (:location peer)
                                :to (:location self) :attending (:attending peer)}}}]
       :advance 0})))

(defmethod decide :merge
  [rng t world patient-id {:keys [with]}]
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
      (rejected-outcome :illegal-merge patient-id {:with with})
      {:events [{:event :merge :t t
                 :participants [{:patient-id patient-id :role :survivor}
                                {:patient-id merged-id :role :merged}]
                 :surviving-mrn (:active-mrn survivor)
                 :merged-mrn (:active-mrn merged)
                 :merged-mrns (:mrns merged)}]
       :advance 0})))

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

;; --- M2b: churn family evolves -------------------------------------------

(defmethod evolve :cancel-admit
  [patient _event]
  (-> patient (assoc :status :new) (dissoc :class :home-ward :location :attending :admitted-at)))

(defmethod evolve :cancel-transfer
  [patient {:keys [home-ward location]}]
  (assoc patient :home-ward home-ward :location location))

(defmethod evolve :cancel-discharge
  [patient {:keys [home-ward location attending]}]
  (assoc patient :status :admitted :home-ward home-ward :location location :attending attending))

(defmethod evolve :bed-swap
  [patient {:keys [swap]}]
  (assoc patient :location (get-in swap [(:patient-id patient) :to])))

(defmethod evolve :merge
  [patient {:keys [participants surviving-mrn merged-mrns]}]
  (let [role (:role (first (filter #(= (:patient-id patient) (:patient-id %)) participants)))]
    (case role
      :survivor (-> patient (update :mrns into merged-mrns) (assoc :active-mrn surviving-mrn))
      :merged (assoc patient :status :merged))))

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
    (let [occupant (get (facility/occupancy-board (:patients world)) bed)]
      (and (some? occupant) (not= occupant patient-id)))))

(defmethod decide :cancel-transfer
  [_rng t world patient-id _step]
  (let [ground-truth (:ground-truth world)
        idx (last-uncancelled-index ground-truth patient-id :transfer :cancel-transfer)]
    (if (nil? idx)
      (rejected-outcome :illegal-cancel-transfer patient-id nil)
      (let [patient (get-in world [:patients patient-id])
            {:keys [home-ward location]} (:before (nth (replay ground-truth) idx))]
        (if (bed-reoccupied-by-someone-else? world patient-id location)
          (rejected-outcome :illegal-cancel-transfer-bed-reoccupied patient-id {:location location})
          {:events [{:event :cancel-transfer :t t :active-mrn (:active-mrn patient)
                     :cancels-event-id idx :home-ward home-ward :location location
                     :participants [{:patient-id patient-id :role :subject}]}]
           :advance 0})))))

(defmethod decide :cancel-discharge
  [_rng t world patient-id _step]
  (let [ground-truth (:ground-truth world)
        idx (last-uncancelled-index ground-truth patient-id :discharge :cancel-discharge)]
    (if (nil? idx)
      (rejected-outcome :illegal-cancel-discharge patient-id nil)
      (let [patient (get-in world [:patients patient-id])
            {:keys [home-ward location attending]} (:before (nth (replay ground-truth) idx))]
        (if (bed-reoccupied-by-someone-else? world patient-id location)
          (rejected-outcome :illegal-cancel-discharge-bed-reoccupied patient-id {:location location})
          {:events [{:event :cancel-discharge :t t :active-mrn (:active-mrn patient)
                     :cancels-event-id idx :home-ward home-ward :location location :attending attending
                     :participants [{:patient-id patient-id :role :subject}]}]
           :advance 0})))))

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
    :churn-profile    ehr-testing-sim.churn/ChurnProfile map (default nil
                       -- churn OFF). M2b: when present, InjectChurn runs
                       ONCE PER PATIENT (in arrival-ordinal order, a fixed
                       point in the draw sequence) against THIS run's own
                       `rng` -- not a derived/isolated stream, same
                       reasoning ADR-0009 gives for NPI generation --
                       between building each patient's step queue and the
                       main loop. Absent entirely (not merely all-zero),
                       this stage never runs and consumes no RNG: the
                       reason a config with no :churn-profile key
                       reproduces byte-identical pre-M2b output (the
                       pinned fixture; churn is opt-in, ADR-0009's
                       accept-and-record policy doesn't even apply here
                       since nothing about this path changed).

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
  [{:keys [seed patients pathway arrival-gap warm-up-seconds facility providers churn-profile]
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
        ;; InjectChurn (M2b): ONLY when :churn-profile is actually
        ;; present does this stage run at all -- absent, `steps-for` is
        ;; a no-op and consumes no RNG (see the docstring's fixture note).
        steps-for (if churn-profile
                    (fn [_i] (:steps (churn/inject pathway churn-profile rng)))
                    (fn [_i] (:steps pathway)))
        init-queue (into (sorted-map)
                         (map-indexed
                          (fn [i arrival-t]
                            [[arrival-t i]
                             {:patient-id (pid-for i) :steps (steps-for i)}])
                          arrivals))
        init-world {:patients (into {} (map-indexed (fn [i _] [(pid-for i) (initial-patient (pid-for i) (mrn-for i))]))
                                    arrivals)
                    :facility facility
                    :providers materialized-providers
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
            (let [{:keys [events advance exhausted]} (decide rng t world patient-id step)]
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
                                          state-history events)]
              (if (seq remaining)
                (recur (assoc queue' [(+ t advance) seq-no] {:patient-id patient-id :steps (vec remaining)})
                       (inc seq-no) world'' ground-truth' state-history')
                (recur queue' seq-no world'' ground-truth' state-history')))))))))))
