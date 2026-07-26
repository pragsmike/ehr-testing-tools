(ns ehr-testing-sim.engine
  "The discrete-event simulation core: a priority queue of pending
  events ordered by simulated time, a map of patient states, and a
  step-transition function. Architecture mined from Google's Simulated
  Hospital (pkg/state WrappedQueue + pkg/hospital RunNextEventIfDue),
  reduced to its functional essence: where the Go original mutates
  patient structs in place, this engine is a pure fold --
  (state, due-event) -> (state', emitted-events) -- so full state
  history per patient is retained, which is what will later make
  state-based emitters (FHIR/CDA snapshots at any simulated instant)
  a rendering concern rather than a redesign.

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
  (:require [ehr-testing-sim.pathway :as pathway])
  (:import [java.util Random]))

(defn- rand-int-in
  "Uniform integer in [lo, hi] from the seeded RNG."
  [^Random rng lo hi]
  (+ lo (.nextInt rng (inc (- hi lo)))))

(defmulti transition
  "Applies one pathway step to one patient's state at simulated time t
  (minutes from epoch of the run). Returns
  {:patient <patient'> :events [<ground-truth event>...] :advance <minutes>}.
  Pure given the RNG (the RNG is the only stateful argument, and its
  consumption order is fixed by the deterministic event ordering)."
  (fn [_rng _t _patient step] (:type step)))

(defmethod transition :admission
  [_rng t patient {:keys [location reason]}]
  {:patient (assoc patient :status :admitted :location location)
   :events [{:event :admission :t t :mrn (:mrn patient)
             :location location :reason reason}]
   :advance 0})

(defmethod transition :delay
  [rng _t patient {:keys [from to]}]
  {:patient patient
   :events []
   :advance (rand-int-in rng from to)})

(defmethod transition :discharge
  [_rng t patient _step]
  {:patient (assoc patient :status :discharged :location nil)
   :events [{:event :discharge :t t :mrn (:mrn patient)}]
   :advance 0})

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

  Returns {:ground-truth [event ...]} where events are format-free maps
  ordered by [t seq-no]. Emitters consume this log; test assertions
  target it directly (it is a first-class output, per the problem
  statement)."
  [{:keys [seed patients pathway arrival-gap]
    :or {patients 1
         pathway pathway/sample-admission-discharge
         arrival-gap 60}}]
  {:pre [(some? seed) (pathway/valid? pathway)]}
  (let [rng (Random. ^long seed)
        ;; Stagger arrivals; consume RNG in patient order (fixed).
        arrivals (vec (reductions + 0 (repeatedly (dec patients)
                                                  #(rand-int-in rng 0 arrival-gap))))
        init-queue (into (sorted-map)
                         (map-indexed
                          (fn [i arrival-t]
                            [[arrival-t i]
                             {:patient {:mrn (format "MRN%06d" (inc i)) :status :new}
                              :steps (:steps pathway)}])
                          arrivals))]
    (loop [queue init-queue
           seq-no patients
           ground-truth (transient [])]
      (if (empty? queue)
        {:ground-truth (persistent! ground-truth)}
        (let [[[t _] {:keys [patient steps]} queue'] (pop-min queue)
              [step & remaining] steps
              {p' :patient evs :events adv :advance} (transition rng t patient step)
              ground-truth' (reduce conj! ground-truth evs)]
          (if (seq remaining)
            (recur (assoc queue' [(+ t adv) seq-no] {:patient p' :steps (vec remaining)})
                   (inc seq-no)
                   ground-truth')
            (recur queue' seq-no ground-truth')))))))
