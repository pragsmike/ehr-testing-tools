(ns ehr-testing-sim.churn
  "InjectChurn (docs/sim-theory.edn): the pathway-ir x churn-profile ->
  operational-pathway transform. M2b's second half -- Task 1 landed the
  churn STEP TYPES the engine can execute; this namespace decides WHERE
  to insert them into an authored pathway, before the engine ever sees
  it, per the theory's own stage laws:

  - IR endomorphism: the output validates as pathway IR, same as the
    input -- the engine needs no knowledge that churn exists at all.
  - clinical-steps invariant: only INSERTS operational-noise steps
    (docs/patient-state-model.md's churn family); the original steps
    are never removed, reordered, or altered. `strip` is the converse
    made executable: filtering every churn-insertable step type back
    out of a churned pathway recovers the original exactly.
  - all stochastic choices draw from the run's single seeded RNG:
    `inject` takes the run's own `java.util.Random` (the SAME instance
    ehr-testing-sim.engine/run already threads through decide, not a
    derived or isolated stream) -- the same reasoning ADR-0009 gives
    for NPI generation, extended here.

  Applicability oracle: docs/patient-state-model.md's event-validity
  table is what decides whether a given churn step type is legal at a
  given point in the pathway (e.g. no :cancel-discharge before any
  :discharge exists yet). Since InjectChurn works at the IR level, with
  no patient state or facility to consult, this is tracked here as a
  small type-level state machine over the step TYPES seen so far --
  admitted?/has-uncancelled-{admission,transfer,discharge}?/
  last-location -- the same predicate shape check.clj's own invariants
  ask of a real log, asked instead of a pathway's step-type sequence.

  RNG consumption is fixed regardless of the profile's VALUES or the
  pathway's applicability state at any point (ADR-0009's own rejected-
  alternative reasoning: making consumption depend on content is worse
  than a fixed, occasionally-discarded draw) -- every churn step type
  draws exactly one `.nextDouble` at every insertion point (`gap`),
  always, in a fixed order; the draw is discarded (no insertion) when
  either the probability isn't cleared or the type isn't currently
  applicable. A zero-probability profile therefore never inserts
  anything, for ANY sequence of draws -- the identity-transform
  property this stage's `zero-profile-is-the-identity-transform`
  property test confirms directly, not merely by inspection."
  (:require [ehr-testing-sim.pathway :as pathway]
            [malli.core :as m])
  (:import [java.util Random]))

(def ^:private churn-step-types
  #{:cancel-admit :cancel-transfer :cancel-discharge :transfer-in-error :bed-swap :merge})

(def ChurnProfile
  "Step type -> per-insertion-point probability in [0.0, 1.0]. Task 0's
  durations rule doesn't apply here -- v1's churn step types carry no
  authored duration of their own (cancel-*/transfer-in-error/bed-swap/
  merge all fire instantaneously in `decide`), so this profile is
  probabilities only, no minutes-authored fields."
  (into [:map]
        (map (fn [step-type] [step-type {:optional true} [:double {:min 0.0 :max 1.0}]]))
        churn-step-types))

(defn valid-churn-profile? [profile] (m/validate ChurnProfile profile))

(def default-churn-profile
  "All-zero: churn OFF. The merge base for a caller-supplied partial
  profile (docs/patient-state-model.md's `run`-wiring note) and the
  profile the zero-probability-identity property is stated against."
  (into {} (map (fn [step-type] [step-type 0.0])) churn-step-types))

(def sample-profile
  "A modest, illustrative nonzero profile -- what a bare `--churn` flag
  turns on when the caller wants churn without hand-tuning every rate.
  Each probability is per GAP (an insertion point around an authored
  step), not per patient or per run, so these are deliberately small."
  {:cancel-admit 0.01 :cancel-transfer 0.02 :cancel-discharge 0.01
   :transfer-in-error 0.02 :bed-swap 0.03 :merge 0.01})

(def ^:private step-type-order
  "Fixed roll order at every gap -- see the namespace docstring's RNG-
  consumption note. Order itself is arbitrary; FIXEDNESS is the law."
  [:cancel-admit :cancel-transfer :cancel-discharge :transfer-in-error :bed-swap :merge])

(def ^:private initial-applicability-state
  {:admitted? false
   :has-uncancelled-admission? false
   :has-uncancelled-transfer? false
   :has-uncancelled-discharge? false
   :last-location nil})

(defn- applicable?
  "docs/patient-state-model.md's event-validity table, asked of the
  type-level state accumulated over the pathway's steps so far (the
  applicability-oracle role that table promises InjectChurn).

  `at-end?` (no original steps remain after this gap) additionally
  gates :cancel-admit specifically: reverting a patient to :new
  strands any ORIGINAL step still ahead that assumes they stay
  admitted (v0/M1's :discharge and :transfer decide methods don't
  themselves re-check current status -- that's check.clj's job, by
  design, for the core step types) -- InjectChurn must never insert a
  step that makes an EXISTING, untouched authored step incoherent, so
  :cancel-admit is only ever inserted after the pathway's own steps are
  already exhausted. The other churn types never strand a later
  original step this way (cancel-transfer/cancel-discharge reinstate a
  still-legal prior location; bed-swap/merge/transfer-in-error don't
  change :admitted? at all), so only :cancel-admit needs this extra
  gate."
  [state step-type at-end?]
  (case step-type
    :cancel-admit (and at-end? (:has-uncancelled-admission? state))
    :cancel-transfer (:has-uncancelled-transfer? state)
    :cancel-discharge (:has-uncancelled-discharge? state)
    :transfer-in-error (and (:admitted? state) (some? (:last-location state)))
    :bed-swap (:admitted? state)
    :merge (:admitted? state)))

(defn- advance-state
  "Folds one step (authored OR churn-inserted -- both are ordinary
  pathway steps once emitted) into the applicability state.

  :cancel-discharge is deliberately NOT modeled as reliably restoring
  :admitted? true here, even though a SUCCESSFUL one really does
  (engine/evolve's own :cancel-discharge sets :status :admitted).
  Unlike every other churn type, :cancel-discharge can be REJECTED at
  decide-time for a reason InjectChurn has no way to predict statically
  -- the bed it would reinstate into may have been legitimately
  reclaimed by someone else's admission by the time it actually runs
  (ehr-testing-sim.engine's bed-reoccupied-by-someone-else? guard).
  Optimistically assuming success and chaining a further :admitted?-
  requiring insertion (a :transfer-in-error/:bed-swap/:merge) after it
  would produce an ILLEGAL step whenever that rejection actually fires
  at runtime -- conservatively treating the patient as no-longer-
  reliably-admitted after a :cancel-discharge is what keeps InjectChurn
  a true applicability oracle rather than an optimistic guess."
  [state {:keys [type location]}]
  (case type
    :admission (assoc state :admitted? true :has-uncancelled-admission? true :last-location location)
    :transfer (assoc state :has-uncancelled-transfer? true :last-location location)
    :discharge (assoc state :admitted? false :has-uncancelled-discharge? true)
    :cancel-admit (assoc state :admitted? false :has-uncancelled-admission? false)
    :cancel-transfer (assoc state :has-uncancelled-transfer? false)
    :cancel-discharge (assoc state :has-uncancelled-discharge? false)
    ;; :delay, :transfer-in-error (self-cancelling, nets to no change),
    ;; :bed-swap, :merge: no applicability-relevant change.
    state))

(defn- churn-step
  [step-type last-location]
  (case step-type
    :cancel-admit {:type :cancel-admit}
    :cancel-transfer {:type :cancel-transfer}
    :cancel-discharge {:type :cancel-discharge}
    :transfer-in-error {:type :transfer-in-error :location last-location}
    :bed-swap {:type :bed-swap}
    :merge {:type :merge}))

(defn- roll-gap
  "Rolls every step type in `step-type-order`, in order, against
  `churn-profile`, ALWAYS drawing one `.nextDouble` per type regardless
  of applicability (the fixed-consumption law) -- returns [state'
  inserted-steps], folding each accepted insertion into the
  applicability state immediately (so two churn steps landing in the
  SAME gap see each other, not a stale pre-gap snapshot)."
  [^Random rng churn-profile state at-end?]
  (reduce (fn [[state acc] step-type]
            (let [draw (.nextDouble rng)
                  probability (get churn-profile step-type 0.0)]
              (if (and (< draw probability) (applicable? state step-type at-end?))
                (let [step (churn-step step-type (:last-location state))]
                  [(advance-state state step) (conj acc step)])
                [state acc])))
          [state []]
          step-type-order))

(defn inject
  "The IR->IR transform: `pathway` (valid pathway IR) x `churn-profile`
  (step-type -> probability, see ChurnProfile) x `rng` (the run's own
  seeded java.util.Random) -> a new, still-valid pathway with churn
  steps spliced in at legal points. n original steps have n+1 gaps
  (before the first, between each pair, after the last); every gap
  rolls the full `step-type-order` (see `roll-gap`). Pure given `rng`;
  never removes, reorders, or alters the input's own steps."
  [pathway churn-profile rng]
  {:pre [(pathway/valid? pathway)]}
  (loop [steps (:steps pathway) state initial-applicability-state out (transient [])]
    (let [[state' inserted] (roll-gap rng churn-profile state (empty? steps))
          out' (reduce conj! out inserted)]
      (if (empty? steps)
        (assoc pathway :steps (persistent! out'))
        (let [[step & remaining] steps]
          (recur remaining (advance-state state' step) (conj! out' step)))))))

(defn strip
  "The clinical-steps invariant's converse, executable: removing every
  churn-insertable step type from a churned pathway recovers the
  ORIGINAL exactly, because `inject` never removes, reorders, or
  alters the steps it was given -- a pure filter, not a
  reconstruction."
  [pathway]
  (update pathway :steps (fn [steps] (into [] (remove (comp churn-step-types :type)) steps))))
