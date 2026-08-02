(ns ehrt.sim.gmf-interpreter
  "The GMF interpreter core (Milestone M5a Task 2, docs/gmf-interpreter.md
  sections 1, 2, and 6). Pure, seeded, engine-free: one module instance
  per patient. `step` advances exactly one state -- evaluates that
  state's own effect (an attribute write, a sampled time advance, or a
  cited trajectory event) and resolves its own transition (direct,
  distributed, conditional, or complex) -- consuming the passed `rng`
  only in the documented order this namespace's own functions state.
  `walk-module` drives `step` repeatedly from a patient's current state
  until the module reaches a Terminal state or BLOCKS on a Guard whose
  condition does not (yet) hold; Milestone M5a's own history/horizon
  two-phase run (Task 3, `ehrt.sim.gmf-interpreter.horizon` or
  this namespace's own `run-module`) is what resumes a blocked walk
  across a phase boundary.

  Condition vocabulary, v1 (section 2): age, sex (:gender), :attribute,
  :prior-state. `:prior-state` compiles to a query over the
  ACCUMULATING TRAJECTORY threaded through `ctx` -- in M5a this trajectory
  IS the log-view the design doc names (\"the log-view IS the
  accumulating trajectory; the engine's real log arrives M5b\"); the
  query helper (`prior-state-condition-holds?`) is written against the
  same event shape (`{:module :state :t ...}`) so M5b can swap the
  source (a real ground-truth log) without touching this logic.

  Time model: virtual time (`ctx`'s `:t`) is an EPOCH DAY (java.time.
  LocalDate/toEpochDay) -- a plain long, so Delay/Guard-age arithmetic is
  ordinary integer comparison, and java.time.LocalDate carries the
  calendar-correct month/year math GMF's own Delay/Age units need
  (`.plusMonths`/`.plusYears` are NOT fixed day-counts). This is an
  interpreter-internal representation; mapping it onto the engine's own
  seconds-from-run-start clock (sim/ADR-0011) is M5b's concern (RunModules
  meeting the real engine), not this session's.

  A documented v1 simplification, recorded here rather than left as a
  silent assumption: a Guard whose condition currently fails BLOCKS the
  walk (no progress) UNLESS the condition is `:age` with operator `>=` --
  in that one case, `step` computes the exact virtual-clock advance
  needed to satisfy it (a deterministic date computation, consuming NO
  rng draw) and proceeds, rather than blocking. This is the mechanism
  that lets 'wait until old enough' Guards make progress under this
  project's own 'no fixed tick' design (docs/gmf-interpreter.md section 3)
  without reintroducing the tick loop that design deliberately rejects:
  the jump is exactly as much virtual time as the one age threshold
  needs, not a polling interval. A Guard blocked on any other condition
  (or a non-`>=` age comparison) simply halts progress -- a module
  author's own responsibility to route around (the same responsibility
  real Synthea's own Delay-then-Guard idiom already carries), not
  something this interpreter resolves for them."
  (:require [ehrt.sim.gmf :as gmf])
  (:import [java.time LocalDate Period]
           [java.util Random]))

;; --- Time -------------------------------------------------------------------

(defn- parse-dob ^LocalDate [persona] (LocalDate/parse (:dob persona)))

(defn dob-epoch-day
  "The epoch-day (java.time.LocalDate/toEpochDay) of `persona`'s own DOB --
  the virtual-time origin a fresh module walk starts from (history phase,
  Task 3: 'from Initial starting at the patient's DOB')."
  [persona]
  (.toEpochDay (parse-dob persona)))

(defn initial-context
  "The patient-ctx a fresh module walk starts from: current state
  `:initial` (every GMF module's own entry-point convention), virtual
  time at the persona's own DOB, an empty attributes map, and an empty
  accumulating trajectory."
  [persona]
  {:current :initial :t (dob-epoch-day persona) :attributes {} :persona persona :trajectory []})

(defn- advance-date
  "epoch-day `t` advanced by `n` `unit`s -- day/week arithmetic is a plain
  day-count; month/year arithmetic goes through java.time.LocalDate
  (calendar-correct, NOT a fixed day-count)."
  ^long [^long t unit ^long n]
  (case unit
    "weeks" (+ t (* 7 n))
    "months" (.toEpochDay (.plusMonths (LocalDate/ofEpochDay t) n))
    "years" (.toEpochDay (.plusYears (LocalDate/ofEpochDay t) n))
    (+ t n)))

(defn- age-years-at
  [persona ^long t]
  (.getYears (Period/between (parse-dob persona) (LocalDate/ofEpochDay t))))

;; --- RNG primitives (fixed-consumption law, per ehrt.sim.engine/
;; ehrt.sim-model.persona's own precedent) ----------------------------------

(defn- rand-int-in [^Random rng lo hi] (+ lo (.nextInt rng (inc (- hi lo)))))
(defn- rand-double-in [^Random rng lo hi] (+ lo (* (.nextDouble rng) (- hi lo))))

(defn- resolve-time-advance
  "How much virtual time a Delay (or a Procedure's own :duration) advances
  from `t`: `:exact` is deterministic, NO rng draw; `:range` samples
  exactly one uniform integer draw, the same fixed-consumption law every
  other stochastic choice in this project already follows. Neither
  present -> no advance, no draw (a state with no timing info of its
  own)."
  [^Random rng ^long t {:keys [range exact]}]
  (cond
    exact (advance-date t (:unit exact) (long (:quantity exact)))
    range (advance-date t (:unit range) (rand-int-in rng (long (:low range)) (long (:high range))))
    :else t))

;; --- Condition evaluation (v1's four predicates, section 2) ----------------

(defn- compare-op
  [op a b]
  (case op ">=" (>= a b) ">" (> a b) "<=" (<= a b) "<" (< a b) "==" (= a b) (= a b)))

(defn- age-condition-holds?
  [{:keys [operator quantity unit]} persona t]
  (compare-op operator (if (= unit "years") (age-years-at persona t) (age-years-at persona t)) quantity))

(defn- gender-condition-holds?
  [{:keys [gender]} persona]
  (= (:sex persona) (case gender "F" :female "M" :male gender)))

(defn- attribute-condition-holds?
  [module-id ctx {:keys [attribute operator value]}]
  (let [k (keyword module-id (gmf/slug attribute))
        actual (get (:attributes ctx) k)]
    (case operator
      "!=" (not= actual value)
      (= actual value))))

(defn- window-days
  [{:keys [quantity unit]}]
  (case unit "weeks" (* 7 quantity) "months" (* 30 quantity) "years" (* 365 quantity) quantity))

(defn- prior-state-condition-holds?
  "PriorState (section 2): a query over `ctx`'s own accumulating
  trajectory for the target module/state citation, most recent first,
  optionally bounded by a time window -- the interpreter-local instance
  of the SAME event shape a real ground-truth-log query (M5b) will use."
  [module-id ctx {:keys [name window]}]
  (let [max-age (when window (window-days window))]
    (boolean (some (fn [event]
                     (and (= module-id (:module event))
                          (= name (:state event))
                          (or (nil? max-age) (<= (- (:t ctx) (:t event)) max-age))))
                   (:trajectory ctx)))))

;; --- M5b: Active Condition / Active Medication -- the log-query family
;; docs/gmf-interpreter.md's own condition-vocabulary-gap note predicted
;; ("architecturally the same log-query mechanism PriorState already
;; establishes... just keyed on a medication/allergy concept rather than
;; a module state name"), now built because the ratified vendored module
;; (sinusitis.json) genuinely needs it on its own mandatory post-encounter
;; path (Wait_for_condition_to_resolve), not merely a hypothetical
;; extension. `Active Allergy` is NOT built the same way: this project's
;; persona/Persona carries no allergy concept anywhere (unlike a
;; condition/medication onset, there is no v1 state type that ever WRITES
;; an allergy fact for this query to find), so it is a documented, always-
;; false simplification -- the conservative default (never wrongly
;; blocks a module's OWN main path, since sinusitis.json's only Active
;; Allergy check is confined to `Penicillin_Allergy_Check`, one arm of
;; Doctor_Visit's own 20% branch, not the 100%-reached path
;; Active Condition/Active Medication sit on) rather than a silent guess.

(defn- code-matches?
  [event-codes condition-codes]
  (boolean (some (fn [ec] (some (fn [cc] (and (= (:system ec) (:system cc)) (= (:code ec) (:code cc))))
                                condition-codes))
                 event-codes)))

(defn- active-onset-condition-holds?
  "Does `ctx`'s own trajectory contain an `onset-event-type` event whose
  :codes match `condition`'s own :codes, with no LATER `end-event-type`
  event referencing that onset's own trajectory index (the same
  index-based reference ConditionEnd/MedicationEnd's own :references
  field already carries, gmf-interpreter's `index-of-citation`)? Most
  recent matching onset, same as PriorState's own 'most recent' rule."
  [onset-event-type end-event-type ctx condition]
  (let [trajectory (vec (:trajectory ctx))
        onset-idx (last (keep-indexed (fn [i ev] (when (and (= onset-event-type (:event ev))
                                                             (code-matches? (:codes ev) (:codes condition)))
                                                    i))
                                      trajectory))]
    (boolean (and onset-idx
                  (not (some (fn [ev] (and (= end-event-type (:event ev)) (= onset-idx (:references ev))))
                            trajectory))))))

;; Mutual recursion with evaluate-condition (And's own sub-conditions are
;; evaluated through the SAME dispatcher, below) -- forward-declared so
;; this namespace reads top-to-bottom without reordering evaluate-condition
;; ahead of the condition-type helpers that already precede it.
(declare evaluate-condition)

(defn- and-condition-holds?
  [module-id ctx {:keys [conditions]}]
  (every? #(evaluate-condition module-id ctx %) conditions))

(defn evaluate-condition
  "The interpreter's own guard evaluator (docs/gmf-interpreter.md section 2:
  '(evaluate-condition condition patient-state (:ground-truth world)
  step)', instantiated here over `ctx`'s own persona/attributes/
  trajectory -- the M5a stand-in for `world`'s :ground-truth mirror).
  M5b adds :active-condition/:active-medication (log query by concept,
  architecturally the same shape :prior-state already establishes),
  :and (recursive compound), and :active-allergy (always false -- this
  namespace's own docstring note on why: no allergy concept exists
  anywhere in this project's Persona for a query to find)."
  [module-id ctx condition]
  (case (:condition-type condition)
    :age (age-condition-holds? condition (:persona ctx) (:t ctx))
    :gender (gender-condition-holds? condition (:persona ctx))
    :attribute (attribute-condition-holds? module-id ctx condition)
    :prior-state (prior-state-condition-holds? module-id ctx condition)
    :active-condition (active-onset-condition-holds? :condition-onset :condition-end ctx condition)
    :active-medication (active-onset-condition-holds? :medication-order :medication-end ctx condition)
    :active-allergy false
    :and (and-condition-holds? module-id ctx condition)
    (throw (ex-info "ehrt.sim.gmf-interpreter: unsupported condition type"
                     {:condition-type (:condition-type condition)}))))

(defn- age-guard-jump-days
  "The v1 simplification this namespace's own docstring names: a FAILING
  `:age` condition with operator `>=` resolves analytically -- the exact
  number of days until the persona's age reaches :quantity `:unit`s, a
  deterministic java.time computation, NO rng draw. Any other failing
  condition (a different operator, a non-age condition type) returns nil
  -- 'not analytically resolvable', the walk blocks instead (`guard-step`,
  below)."
  [{:keys [condition-type operator quantity unit]} persona ^long t]
  (when (and (= condition-type :age) (= operator ">=") (= unit "years"))
    (let [target-day (.toEpochDay (.plusYears (parse-dob persona) (long quantity)))]
      (when (> target-day t) (- target-day t)))))

;; --- Transition resolution (direct, distributed, conditional, complex) ----

(defn- weighted-pick-transition
  "distributed_transition (and complex_transition's own nested
  distributions): a cumulative-weight pick over `entries`
  ({:transition :distribution}), consuming EXACTLY one `.nextDouble` --
  fixed consumption regardless of which member is chosen, the same law
  ehrt.sim.engine/assign-pathway and
  ehrt.sim.order-profiles/sample-analyte-value already establish."
  [^Random rng entries]
  (let [total (reduce + (map :distribution entries))
        target (* (.nextDouble rng) total)]
    (loop [es entries acc 0.0]
      (let [e (first es) more (rest es) acc' (+ acc (double (:distribution e)))]
        (if (or (empty? more) (< target acc')) (:transition e) (recur more acc'))))))

(defn- first-matching-entry
  "conditional_transition's own first-match-wins semantics, and
  complex_transition's own first-matching-condition's distribution list --
  an entry with NO :condition at all is the trailing 'else' arm. Consumes
  NO rng: purely a walk over already-known state (persona/attributes/
  trajectory), never a stochastic choice of its own."
  [module-id ctx entries]
  (first (filter (fn [{:keys [condition]}] (or (nil? condition) (evaluate-condition module-id ctx condition)))
                 entries)))

(defn- resolve-transition
  "The shared 4-kind transition dispatcher every non-Terminal v1 state
  type resolves its own :next through -- one mechanism, reused by every
  state type's own `step` handling below, rather than duplicated per
  type."
  [module-id ctx ^Random rng state]
  (cond
    (:direct-transition state) (:direct-transition state)
    (:distributed-transition state) (weighted-pick-transition rng (:distributed-transition state))
    (:conditional-transition state) (:transition (first-matching-entry module-id ctx (:conditional-transition state)))
    (:complex-transition state) (weighted-pick-transition
                                  rng (:distributions (first-matching-entry module-id ctx (:complex-transition state))))
    :else nil))

;; --- step --------------------------------------------------------------

(defn- pass-through-outcome
  [module-id ctx rng state advance events]
  {:events events
   :attributes (:attributes ctx)
   :advance advance
   :next (resolve-transition module-id ctx rng state)
   :terminal? false
   :blocked? false})

(defn- blocked-outcome
  [ctx]
  {:events [] :attributes (:attributes ctx) :advance 0 :next nil :terminal? false :blocked? true})

(defn- guard-step
  [module-id ctx ^Random rng state]
  (let [condition (:allow state)]
    (if (evaluate-condition module-id ctx condition)
      (pass-through-outcome module-id ctx rng state 0 [])
      (if-let [jump (age-guard-jump-days condition (:persona ctx) (:t ctx))]
        ;; `age-guard-jump-days` computes the EXACT day the condition
        ;; starts holding -- re-evaluating after the jump would always
        ;; pass by construction, so there is no second, still-blocked
        ;; branch to handle here (a scenario that can't happen gets no
        ;; defensive code for it, this project's own convention).
        (let [ctx' (update ctx :t + jump)]
          (update (pass-through-outcome module-id ctx' rng state 0 []) :advance + jump))
        (blocked-outcome ctx)))))

(defn- trajectory-event
  [module-id ctx event-type extra]
  (merge {:module module-id :state (:current ctx) :t (:t ctx) :event event-type} extra))

(defn- index-of-citation
  "Where in `trajectory` the event citing `{:module :state target-state}`
  and of the given `event-type` sits -- the shape ConditionEnd/
  MedicationEnd's own reference to its opening event uses (docs/gmf-
  interpreter.md section 1: 'the same 'references an existing prior
  event' shape :cancel-*/:result-available already establish')."
  [trajectory module-id event-type target-state]
  (when target-state
    (some (fn [[i event]]
            (when (and (= module-id (:module event)) (= event-type (:event event)) (= target-state (:state event)))
              i))
          (map-indexed vector trajectory))))

(defn- index-of-last-open-encounter
  [trajectory module-id]
  (last (keep (fn [[i event]] (when (and (= module-id (:module event)) (= :encounter (:event event))) i))
              (map-indexed vector trajectory))))

(defn- emit-and-advance
  "Every v1 trajectory-event-producing state type shares this shape: cite
  `{:module :state :t}` (glass-box law), carry `extra` (typically :codes,
  verbatim -- code passthrough law) into the event, append it to the
  accumulating trajectory, then resolve the ORDINARY transition
  (optionally after its own sampled `:duration`, Procedure's own case)."
  [module-id ctx ^Random rng state event-type extra]
  (let [event (trajectory-event module-id ctx event-type extra)
        ctx' (update ctx :trajectory conj event)
        advance (if-let [duration (:duration state)] (- (resolve-time-advance rng (:t ctx) duration) (:t ctx)) 0)]
    (pass-through-outcome module-id ctx' rng state advance [event])))

(defn- round1 [^double v] (/ (Math/round (* v 10.0)) 10.0))

(defn- sample-observation-extra
  [^Random rng state]
  (let [codes (:codes state)]
    (if-let [{:keys [low high]} (:range state)]
      {:codes codes :value (round1 (rand-double-in rng low high)) :unit (:unit state)}
      {:codes codes})))

(defn step
  "Advances ONE state from `ctx`'s own `:current` -- consuming `rng` only
  in this function's own documented order (per-state-type below). Returns
  {:events [...] :attributes {...} :advance seconds :next state-or-nil
  :terminal? bool :blocked? bool}; NEVER mutates `ctx` -- the caller
  (`walk-module`, below) is what folds an outcome back into a new ctx,
  the same decide/evolve-style separation ehrt.sim.engine already
  establishes (sim/ADR-0008), scaled down to this interpreter's own single-
  function `step`, since a GMF state's own effect and its own transition
  are never independently interesting the way decide/evolve's
  cross-patient split is."
  [module rng ctx]
  (let [module-id (:id module)
        state (get-in module [:states (:current ctx)])]
    (case (:type state)
      :terminal {:events [] :attributes (:attributes ctx) :advance 0 :next nil :terminal? true :blocked? false}
      :initial (pass-through-outcome module-id ctx rng state 0 [])
      :simple (pass-through-outcome module-id ctx rng state 0 [])
      ;; M5b: consumed-internally, like :simple -- gmf/gmf-type->keyword's
      ;; own docstring note (no equipment-tracking home yet, no trajectory
      ;; event, no attribute write).
      :device (pass-through-outcome module-id ctx rng state 0 [])
      :device-end (pass-through-outcome module-id ctx rng state 0 [])
      :delay (let [t' (resolve-time-advance rng (:t ctx) state)]
               (pass-through-outcome module-id ctx rng state (- t' (:t ctx)) []))
      :guard (guard-step module-id ctx rng state)
      :set-attribute (let [k (keyword module-id (gmf/slug (:attribute state)))
                           ctx' (update ctx :attributes assoc k (:value state))]
                       (pass-through-outcome module-id ctx' rng state 0 []))
      :symptom (let [severity (cond (:exact state) (:quantity (:exact state))
                                     (:range state) (rand-int-in rng (:low (:range state)) (:high (:range state)))
                                     :else nil)
                     k (keyword module-id (gmf/slug (:symptom state)))
                     ctx' (update ctx :attributes assoc k severity)]
                 (pass-through-outcome module-id ctx' rng state 0 []))
      :condition-onset (emit-and-advance module-id ctx rng state :condition-onset {:codes (:codes state)})
      :condition-end (emit-and-advance module-id ctx rng state :condition-end
                                        {:references (index-of-citation (:trajectory ctx) module-id
                                                                         :condition-onset (:condition-onset state))})
      :encounter (emit-and-advance module-id ctx rng state :encounter
                                    {:codes (:codes state) :encounter-class (:encounter-class state)})
      :encounter-end (emit-and-advance module-id ctx rng state :encounter-end
                                        {:references (index-of-last-open-encounter (:trajectory ctx) module-id)})
      :procedure (emit-and-advance module-id ctx rng state :procedure {:codes (:codes state)})
      :observation (emit-and-advance module-id ctx rng state :observation (sample-observation-extra rng state))
      :medication-order (emit-and-advance module-id ctx rng state :medication-order {:codes (:codes state)})
      :medication-end (emit-and-advance module-id ctx rng state :medication-end
                                         {:references (index-of-citation (:trajectory ctx) module-id
                                                                          :medication-order (:medication-order state))}))))

;; --- walk-module: drives `step` from :initial to Terminal or blocked ------

(def ^:private max-steps
  "A runaway-loop backstop, not a design limit: a real v1 module always
  terminates or blocks in far fewer steps than this. Exceeding it means a
  module authoring bug (a zero-time-advance transition cycle), a
  programmer error this project's own conventions reserve exceptions
  for -- never a result-not-throw outcome, since no legitimate module
  should ever reach it."
  10000)

(defn walk-module
  "Drives `step` from `ctx`'s own `:current` until the module reaches a
  Terminal state (`:status :terminal`) or BLOCKS on a Guard whose
  condition does not hold (`:status :blocked`, `ctx`'s own `:current`
  left AT the blocked Guard, ready for a caller -- Task 3's history/
  horizon two-phase run -- to resume the SAME walk later with more
  virtual time or more attributes available)."
  [module rng ctx]
  (loop [ctx ctx n 0]
    (when (>= n max-steps)
      (throw (ex-info "ehrt.sim.gmf-interpreter: walk-module exceeded max-steps -- likely a module authoring bug (a zero-time-advance transition cycle)"
                       {:module (:id module) :current (:current ctx)})))
    (let [outcome (step module rng ctx)
          ctx' (-> ctx
                   (assoc :attributes (:attributes outcome))
                   (update :trajectory into (:events outcome))
                   (update :t + (:advance outcome)))]
      (cond
        (:terminal? outcome) (assoc ctx' :status :terminal)
        (:blocked? outcome) (assoc ctx' :status :blocked)
        :else (recur (assoc ctx' :current (:next outcome)) (inc n))))))

;; --- run-module: the history/horizon two-phase run (Task 3, docs/gmf-
;; interpreter.md section 3) -------------------------------------------------

(defn run-module
  "The ratified history/horizon design (section 3): ONE continuous walk
  from `persona`'s own DOB (Task 2's `initial-context`) -- no fixed tick,
  no separately-invoked phase pass. Every trajectory event this walk
  emits is marked `:pre-horizon` by the exact pure predicate `(< t
  registration-t)` -- the same shape sim/ADR-0011's own warm-up mark already
  uses (`warm-up-mark-matches-window`), applied here to the history/
  horizon boundary instead of a run's warm-up window. `registration-t`
  is the caller-supplied virtual instant (an epoch-day, `dob-epoch-day`'s
  own unit) this patient's history phase ends and horizon phase begins
  at -- 'that patient's own :registered event time' (section 3), passed
  in explicitly here since M5a has no real engine `:registered` event
  yet to read it from (M5b's own integration point).

  `horizon-end-t` (optional, an epoch-day) bounds the horizon phase --
  omitted, the walk runs to Terminal or blocked, same as `walk-module`;
  supplied, the walk also stops (status `:horizon-complete`) once `:t`
  reaches it, without emitting whatever state it stopped at.

  Because this is genuinely ONE walk (not two independently driven
  passes), 'the phases genuinely share state' (section 3's own property)
  holds by construction: the SAME accumulating `:attributes` map and
  `:trajectory` are threaded across the registration boundary, so a Guard
  blocked on an attribute set earlier in the SAME walk sees it, whichever
  side of `registration-t` each state happens to fall on."
  ([module rng persona registration-t] (run-module module rng persona registration-t nil))
  ([module rng persona registration-t horizon-end-t]
   (loop [ctx (initial-context persona) n 0]
     (when (>= n max-steps)
       (throw (ex-info "ehrt.sim.gmf-interpreter: run-module exceeded max-steps -- likely a module authoring bug (a zero-time-advance transition cycle)"
                        {:module (:id module) :current (:current ctx)})))
     (if (and horizon-end-t (>= (:t ctx) horizon-end-t))
       (assoc ctx :status :horizon-complete)
       (let [outcome (step module rng ctx)
             marked-events (mapv #(assoc % :pre-horizon (< (:t %) registration-t)) (:events outcome))
             ctx' (-> ctx
                      (assoc :attributes (:attributes outcome))
                      (update :trajectory into marked-events)
                      (update :t + (:advance outcome)))]
         (cond
           (:terminal? outcome) (assoc ctx' :status :terminal)
           (:blocked? outcome) (assoc ctx' :status :blocked)
           :else (recur (assoc ctx' :current (:next outcome)) (inc n))))))))
