(ns ehr-testing-sim.compile-trajectory
  "CompileTrajectory (docs/sim-theory.edn): clinical-trajectory ->
  compiled-pathway, per docs/gmf-interpreter.md section 1's own per-
  state-type mapping table. Pure and RNG-free: every value this stage
  ever touches (a Delay's own sampled advance, an Observation's own
  sampled value, which distributed_transition branch fired) was already
  decided by the GMF interpreter (M5a) -- CompileTrajectory only
  re-shapes already-decided content into pathway IR the engine can
  execute, the same 'the interpreter decides, this stage merely
  compiles' split ADR-0008's own decide/evolve separation established
  one layer down.

  The mapping (section 1's table):
  - `:encounter` -- `:wellness`/`:ambulatory` -> `:outpatient-visit`;
    `:emergency` -> `:admission` targeting this run's first `:class :ed`
    ward; `:inpatient` -> `:admission` targeting this run's first
    `:class :inpatient` ward (section 4). A GMF module names an
    encounter CLASS, never a specific hospital ward -- this run's own
    facility config is where a concrete ward name comes from, the same
    way `:reason`/attending are engine-config concerns, not module ones.
  - `:encounter-end` -- mirrors its own opening encounter's class
    (resolved via the trajectory's own `:references` index, the SAME
    'most recently opened Encounter for this module' citation the
    interpreter's own EncounterEnd handling already computes, section 7
    item 3): `:outpatient-visit-end` for outpatient-opened,
    `:discharge` for inpatient/ED-opened.
  - `:procedure`/`:observation`/`:medication-order`/`:medication-end` --
    each compiles to its own new standalone IR step (never an
    annotation), carrying `:codes` (and, for Observation, its own
    sampled `:value`/`:unit` when present) verbatim (code passthrough
    law) plus a `:citation` (provenance, section 6 obligation 3).
    `:medication-end` additionally carries `:order-citation` -- the
    CITATION (not a pathway-position index) of the `:medication-order`
    step it ends, resolved via the trajectory's own `:references`
    index -- glass-box, position-independent resolution, the same
    reason `docs/patient-state-model.md`'s deterministic-event-id
    section already prefers citing by identity over a fragile position.
  - `:condition-onset`/`:condition-end` -- compile to an ANNOTATION on
    the most recently compiled Encounter-mapped step (`:conditions`, a
    vector pathway.clj's Citation/ConditionAnnotation schemas define),
    never a standalone IR step (this project's pathway IR has no
    diagnosis-list step of its own yet, section 1's table). When no
    compiled encounter step exists to attach to -- no prior encounter in
    the trajectory at all, OR its own encounter was itself dropped as a
    pre-horizon fact, below -- the condition event compiles to NO IR
    step: a log-only fact, the same shape `:step-rejected` (ADR-0012)
    already established for 'real, worth keeping, not worth inventing an
    attachment point for.'

  Pre-horizon handling (docs/gmf-interpreter.md section 3, ratified item
  5; this session's own resolution of a real gap between that section's
  own prose -- 'no operational trajectory event is minted for the
  encounter machinery itself' during history -- and the M5a interpreter
  AS BUILT, which does not itself discriminate by phase when minting
  Encounter/Procedure/Observation events, only marks them afterward):
  enforced HERE, at compile time, since this is the layer that actually
  turns a trajectory event into something the engine executes. A
  pre-horizon `:encounter`/`:encounter-end`/`:procedure`/`:observation`
  event compiles to NOTHING (dropped -- no operational admission/visit/
  procedure/observation for something that happened years before this
  run's own registration instant). A pre-horizon `:condition-onset`/
  `:condition-end`/`:medication-order`/`:medication-end` event -- ONE
  per real onset/end this project's history-phase walk actually crossed
  -- becomes a REGISTRATION-TIME FACT instead: carried in this
  function's own `:registration-facts` output, never as a pathway IR
  step (an annotation's own encounter is necessarily unavailable this
  early, and a standalone step would misrepresent something that
  happened before this run's own clock started at all) -- ratified item
  5's own 'enters the ground-truth log... as registration-time facts' is
  realized by a caller (M5b Task 4's own engine wiring) riding these on
  the SAME engine-internal `:registered` event every patient already
  gets (M4), never inventing a new IR step type for it.

  The day -> minutes boundary (docs/patient-state-model.md's durations
  rule, extended): every trajectory event's own `:t` is an interpreter-
  internal EPOCH DAY (ehr-testing-sim.gmf-interpreter); pathway IR's own
  `:delay` is authored in MINUTES. This namespace is the ONE place that
  conversion happens for compiled content -- a `:delay {:from :to}` step
  (both bounds equal -- deterministic; the elapsed time was already
  decided by the interpreter, no fresh sampling) bridges the gap between
  `registration-t` and the first compiled step, and between each
  subsequent pair of compiled (non-annotation) steps, whenever that gap
  is nonzero."
  (:require [ehr-testing-sim.facility :as facility]))

(def ^:private minutes-per-day
  "The durations rule's own day clause: interpreter epoch-days -> engine
  minutes, at exactly this one conversion point."
  1440)

(defn- citation [event] {:module (:module event) :state (:state event)})

(defn- ward-name-for-class
  [facility class]
  (:name (first (filter #(= class (:class %)) (:wards facility)))))

(defn- encounter->step
  [facility event]
  (case (:encounter-class event)
    (:wellness :ambulatory) {:type :outpatient-visit :citation (citation event)}
    :emergency {:type :admission :location (ward-name-for-class facility :ed) :citation (citation event)}
    :inpatient {:type :admission :location (ward-name-for-class facility :inpatient) :citation (citation event)}))

(defn- referenced-event
  "The trajectory event `event`'s own `:references` index resolves to --
  the shared shape EncounterEnd/MedicationEnd's own back-references both
  use (docs/gmf-interpreter.md section 1)."
  [trajectory event]
  (when-let [idx (:references event)]
    (nth trajectory idx nil)))

(defn- encounter-end->step
  [trajectory event]
  (let [opening (referenced-event trajectory event)]
    (if (#{:wellness :ambulatory} (:encounter-class opening))
      {:type :outpatient-visit-end :citation (citation event)}
      {:type :discharge :citation (citation event)})))

(defn- procedure->step [event] {:type :procedure :codes (:codes event) :citation (citation event)})

(defn- observation->step
  [event]
  (cond-> {:type :observation :codes (:codes event) :citation (citation event)}
    (some? (:value event)) (assoc :value (:value event))
    (:unit event) (assoc :unit (:unit event))))

(defn- medication-order->step [event] {:type :medication-order :codes (:codes event) :citation (citation event)})

(defn- medication-end->step
  [trajectory event]
  (let [order-event (referenced-event trajectory event)]
    (cond-> {:type :medication-end :citation (citation event)}
      order-event (assoc :order-citation (citation order-event)))))

(def ^:private pre-horizon-dropped-types
  "docs/gmf-interpreter.md section 3's own 'no operational trajectory
  event... during history' -- enforced here (see this namespace's own
  docstring for why the interpreter itself doesn't already do this)."
  #{:encounter :encounter-end :procedure :observation})

(def ^:private pre-horizon-fact-types
  "The ratified item 5 condensed set: ConditionOnset/ConditionEnd/
  MedicationOrder/MedicationEnd, the only pre-horizon events that ever
  become a REGISTRATION-TIME fact rather than being dropped outright."
  #{:condition-onset :condition-end :medication-order :medication-end})

(defn- annotate-condition
  "Finds the most recently COMPILED encounter-mapped step whose citation
  matches the trajectory's own 'most recent prior :encounter for this
  module' (the same temporal heuristic the interpreter's own
  EncounterEnd citation already uses, section 7 item 3) and appends this
  condition event as one more entry in that step's own `:conditions`
  vector. No such compiled step (never opened, or opened but itself
  dropped as a pre-horizon fact) -> `steps` unchanged: a log-only fact,
  never a fabricated attachment point.

  ConditionEnd's own trajectory event carries NO `:codes` of its own
  (the interpreter's `step` only attaches `:references` -- ending the
  SAME condition its onset already named, never repeating the concept);
  its annotation's own `:codes` are resolved from that referenced onset
  event instead, so a reader never sees a conditions entry with no
  concept at all."
  [steps trajectory idx event]
  (let [enc-idx (last (keep-indexed (fn [i ev] (when (and (< i idx) (= :encounter (:event ev))) i)) trajectory))]
    (if-let [enc-citation (some-> enc-idx (->> (nth trajectory)) citation)]
      (let [target-idx (last (keep-indexed (fn [i s] (when (= enc-citation (:citation s)) i)) steps))
            codes (or (:codes event) (:codes (referenced-event trajectory event)))]
        (if target-idx
          (update-in steps [target-idx :conditions] (fnil conj [])
                     {:event (:event event) :codes codes :citation (citation event)
                      :references (:references event)})
          steps))
      steps)))

(defn- emit-with-delay
  [steps last-t event new-step]
  (let [gap-minutes (* minutes-per-day (- (:t event) last-t))]
    (cond-> steps
      (pos? gap-minutes) (conj {:type :delay :from gap-minutes :to gap-minutes})
      true (conj new-step))))

(defn compile-trajectory
  "clinical-trajectory (a vector of GMF-interpreter trajectory events,
  ehr-testing-sim.gmf-interpreter/run-module's own `:trajectory`) x
  `facility` (this run's own facility config -- where a concrete ward
  name for an emergency/inpatient encounter class comes from) x
  `registration-t` (the same epoch-day instant `run-module` was called
  with, the anchor the FIRST compiled step's own bridging delay is
  measured from) -> {:steps [pathway-ir-step ...] :registration-facts
  [...]}. `:steps` is real pathway IR (`ehr-testing-sim.pathway/valid?`
  holds for `{:name ... :steps steps}`, any real facility); `:registration-
  facts` is this namespace's own resolution of ratified item 5 -- see
  this namespace's own docstring."
  [trajectory facility registration-t]
  (loop [events (map-indexed vector trajectory)
         steps []
         registration-facts []
         last-t registration-t
         encounter-closed? false]
    (if (empty? events)
      {:steps steps :registration-facts registration-facts}
      (let [[idx event] (first events)
            more (rest events)
            event-type (:event event)]
        (cond
          ;; This project's own encounter-horizon scope (ADR-0007 point 3:
          ;; "hospital-operations traffic across a single encounter... not
          ;; a patient's lifelong longitudinal history") -- a real vendored
          ;; module can be authored to recur across a patient's WHOLE life
          ;; (docs/gmf-interpreter.md section 8 item 5's own finding, about
          ;; sinusitis.json specifically: Potential_Onset loops forever).
          ;; Compiling every recurrence would mint a SECOND :admission/
          ;; :outpatient-visit for an already-:discharged patient-id --
          ;; illegal by this project's own event-validity table, not a
          ;; churn/engine bug. CompileTrajectory's own resolution: compile
          ;; through the end of the FIRST horizon-phase encounter, then
          ;; stop -- everything after is out of THIS run's own scope, a
          ;; log-only fact the underlying (uncompiled) clinical-trajectory
          ;; still carries in full, the same "real, not worth compiling"
          ;; shape this namespace's own docstring already establishes for
          ;; other drop cases.
          encounter-closed?
          (recur more steps registration-facts last-t encounter-closed?)

          (and (:pre-horizon event) (pre-horizon-dropped-types event-type))
          (recur more steps registration-facts last-t encounter-closed?)

          (and (:pre-horizon event) (pre-horizon-fact-types event-type))
          (recur more steps
                 (conj registration-facts {:event event-type :codes (:codes event)
                                           :citation (citation event) :references (:references event)})
                 last-t encounter-closed?)

          (= :encounter event-type)
          (recur more (emit-with-delay steps last-t event (encounter->step facility event))
                 registration-facts (:t event) encounter-closed?)

          (= :encounter-end event-type)
          (recur more (emit-with-delay steps last-t event (encounter-end->step trajectory event))
                 registration-facts (:t event) true)

          (= :procedure event-type)
          (recur more (emit-with-delay steps last-t event (procedure->step event))
                 registration-facts (:t event) encounter-closed?)

          (= :observation event-type)
          (recur more (emit-with-delay steps last-t event (observation->step event))
                 registration-facts (:t event) encounter-closed?)

          (= :medication-order event-type)
          (recur more (emit-with-delay steps last-t event (medication-order->step event))
                 registration-facts (:t event) encounter-closed?)

          (= :medication-end event-type)
          (recur more (emit-with-delay steps last-t event (medication-end->step trajectory event))
                 registration-facts (:t event) encounter-closed?)

          (#{:condition-onset :condition-end} event-type)
          (recur more (annotate-condition steps trajectory idx event) registration-facts last-t encounter-closed?)

          :else
          (recur more steps registration-facts last-t encounter-closed?))))))
