(ns ehrt.sim-trajectory.compile-trajectory
  "CompileTrajectory (docs/sim-theory.edn): clinical-trajectory ->
  compiled-pathway, per docs/gmf-interpreter.md section 1's own per-
  state-type mapping table. Pure and RNG-free: every value this stage
  ever touches (a Delay's own sampled advance, an Observation's own
  sampled value, which distributed_transition branch fired) was already
  decided by the GMF interpreter (M5a) -- CompileTrajectory only
  re-shapes already-decided content into pathway IR the engine can
  execute, the same 'the interpreter decides, this stage merely
  compiles' split sim/ADR-0008's own decide/evolve separation established
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
  - `:care-plan-start`/`:care-plan-end` -- GMF coverage Wave D stage D2
    (ADR-0029 R2(b)): the SAME standalone-IR-step/citation-resolution
    shape as `:medication-order`/`:medication-end`, one function-pair
    down (`care-plan-start->step`/`care-plan-end->step`) -- `:codes`/
    `:activities` verbatim, `:care-plan-citation` resolved the same
    `:references`-index way `:order-citation` already is.
  - `:condition-onset`/`:condition-end` -- compile to an ANNOTATION on
    the most recently compiled Encounter-mapped step (`:conditions`, a
    vector pathway.clj's Citation/ConditionAnnotation schemas define),
    never a standalone IR step (this project's pathway IR has no
    diagnosis-list step of its own yet, section 1's table). When no
    compiled encounter step exists to attach to -- no prior encounter in
    the trajectory at all, OR its own encounter was itself dropped as a
    pre-horizon fact, below -- the condition event compiles to NO IR
    step: a log-only fact, the same shape `:step-rejected` (sim/ADR-0012)
    already established for 'real, worth keeping, not worth inventing an
    attachment point for.'
  - `:death` -- GMF coverage Wave C (ADR-0028, C4): NO new IR step type.
    Death inside an encounter (a compiled `:admission`/`:outpatient-visit`
    step already exists, no terminal disposition compiled for it yet)
    reuses the EXISTING `:discharge` step, carrying two new optional
    fields (`:disposition :expired`, `:codes` -- cause of death, verbatim)
    -- real HL7v2 already models a death this way (an ordinary ADT^A03
    whose PV1-36 carries an expired disposition code, `docs/clinical-
    realities.md`'s own wire-truth section). Death outside any encounter
    closes the pathway at that timestamp without fabricating a discharge
    from an admission that never happened -- the same 'no attachment
    point, don't invent one' precedent the condition-annotation case
    above already establishes.

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
  gets (M4), never inventing a new IR step type for it. THIS PARAGRAPH
  DESCRIBES THE LEGACY PATH ONLY -- `history?` false (the default, every
  pre-H caller), byte-identical to every pre-H run. See the next
  paragraph for `history?` true.

  Wave H pre-roll (2026-08-04, ADR-0042 AR-1/AR-2): `history?` true
  swaps the paragraph above for a SINGLE uniform rule -- any event whose
  own `:phase` (`ehrt.sim-trajectory.gmf-interpreter/run-module`'s own
  AR-2 encounter-anchored mark, minted only when `history?` was ALSO
  true at the interpreter) is `:history` compiles to NOTHING, no
  dropped-types/fact-types bucketing, no `:registration-facts` entry --
  the ConditionEnd-with-no-open-encounter precedent this namespace
  already establishes elsewhere ('real, worth keeping, not worth a
  message'), generalized to a whole phase. Glass-box traceability keeps
  every history-phase event inspectable on the raw, uncompiled
  `trajectory` this function's own caller still has in full (AR-1's own
  'no second interpreter, no fold-only mode' ruling) -- there is nothing
  left for a condensed registration-time-fact summary to add, so
  `:registration-facts` stays empty under `history?` true (AR-6's own
  reconciliation: `:pre-horizon-facts`, riding the SAME engine-internal
  `:registered` event, is simply never populated in this mode, not
  retired as a mechanism -- `history?` false still lands there exactly
  as before). Because `:phase` is INHERITED from an encounter's own
  opening phase (interpreter-level, not re-derived here), a straddling
  encounter's own `:encounter-end` drops together with its own
  `:encounter` -- `encounter-closed?` never becomes true for it, so this
  loop's own 'first encounter, then stop' scoping naturally continues
  past a fully-dropped straddling encounter to the next one, exactly as
  it already skips past any other pre-horizon-dropped encounter today.
  `check.clj`'s own `:clinical-content-only-when-admitted` invariant
  needs no change for this: it reads compiled IR step types
  (`:procedure`/`:observation`/`:medication-order`/`:diagnostic-report`/
  `:care-plan-start`) replayed through folded engine state, never
  `:pre-horizon-facts` or the raw trajectory directly, and AR-2
  guarantees a straddling encounter's own contents never reach `:steps`
  at all, in either mode.

  Step 3 finding (2026-08-04, running the real UTI closure under an
  ordinary seed, AR-4's own proof obligation): open-encounter
  inheritance alone is NOT sufficient. `:medication-end`/`:care-plan-
  end`/`:condition-end` can legitimately fire OUTSIDE any encounter (a
  medication started during a dropped history-phase encounter,
  ended after discharge, in horizon, with nothing open to inherit
  from) -- `history-phase?` (below) closes this the same way AR-2
  closes the encounter case: an event whose own `:references`
  antecedent was itself dropped is dropped too, one hop along the
  SAME back-edge `referenced-event` already resolves for citation
  purposes. This is not a new rule, only AR-2's own 'no orphaned
  reference to something dropped' principle applied to the other kind
  of back-reference this namespace already has.

  The straddle fix (2026-08-08, ADR-0085/0086, AR-SF-1): the LEGACY
  (`history?` false) path had no analogue of AR-2's own inheritance at
  all -- its drop clauses (below) tested only an event's own raw
  `:pre-horizon` flag, with no back-reference check against the
  encounter it belongs to. A real, clinically ordinary shape (an
  encounter admitted before 'today' and still open through it) then
  compiled orphaned clinical content and a terminal step with no
  matching opening, tripping `check.clj`'s own
  `:clinical-content-only-when-admitted` (ADR-0085's own diagnosis).
  Shape (b), the ruled fix (AR-SF-1): generalize AR-2's own principle
  to the legacy path -- a `straddle-open?` fold state (mirroring
  `gmf-interpreter/mark-phase`'s own `open-phase`, the SAME 'one
  in-flight span, encounters never nest in this project's own GMF
  subset' invariant, at compile time instead of interpreter time)
  opens the moment a raw-pre-horizon `:encounter` is dropped, and every
  subsequent event -- regardless of ITS OWN raw `:pre-horizon` -- gets
  the EXISTING pre-horizon disposition (`pre-horizon-dropped-types`
  drops, `pre-horizon-fact-types` becomes a registration fact) until
  the matching `:encounter-end` closes the span. `:supply-list` needs
  no change (already unconditional, any phase). `encounter-closed?`
  stays untouched by a straddling `:encounter-end`'s own drop -- the
  SAME 'existing disposition' a fully-pre-horizon `:encounter-end`
  already got, letting a genuinely later horizon-phase encounter still
  become 'the first' for this run's own single-encounter scope (the
  post-straddle proof this session's own test suite adds, mirroring
  the pre-existing `history-mode-post-straddle-horizon-encounter-
  still-compiles-normally` test). `:suppressed-straddle-spans` (AR-SF-7)
  counts SPANS, not events -- incremented once per span whose own
  closing `:encounter-end` has raw `:pre-horizon` false (a genuine
  straddle, not a fully-pre-horizon span that happens to close inside
  history too), a purely additive key on this function's own return
  map (every caller confirmed `:keys`-selective, AR-SF-7's own
  friction test).

  The day -> minutes boundary (docs/patient-state-model.md's durations
  rule, extended): every trajectory event's own `:t` is an interpreter-
  internal EPOCH DAY (ehrt.sim-trajectory.gmf-interpreter); pathway IR's own
  `:delay` is authored in MINUTES. This namespace is the ONE place that
  conversion happens for compiled content -- a `:delay {:from :to}` step
  (both bounds equal -- deterministic; the elapsed time was already
  decided by the interpreter, no fresh sampling) bridges the gap between
  `registration-t` and the first compiled step, and between each
  subsequent pair of compiled (non-annotation) steps, whenever that gap
  is nonzero."
  (:require [ehrt.sim-model.interface :as sim-model]))

(def ^:private minutes-per-day
  "The durations rule's own day clause: interpreter epoch-days -> engine
  minutes, at exactly this one conversion point."
  1440)

(defn- citation [event] {:module (:module event) :state (:state event)})

(defn- ward-name-for-class
  [facility class]
  (:name (first (filter #(= class (:class %)) (:wards facility)))))

(defn- encounter->step
  "ADR-0133 (restoration cascade, resolving the deferred decision
  `ehrt.sim-trajectory.gmf/encounter-class->keyword`'s own docstring
  named): `:virtual` joins `:wellness`/`:ambulatory` here -- the SAME
  compile-layer same-concept alias Wave B's own `\"outpatient\"` ->
  `:ambulatory` precedent already established (a phone/remote
  encounter compiles to the SAME :outpatient-visit IR shape a
  wellness/ambulatory one does; the trajectory event itself keeps
  :encounter-class :virtual, so no modality information is lost -- a
  distinct IR treatment remains available to any future session with
  an actual consumer for that distinction)."
  [facility event]
  (case (:encounter-class event)
    (:wellness :ambulatory :virtual) {:type :outpatient-visit :citation (citation event)}
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
  "ADR-0133: `:virtual` joins the outpatient-pairing set here TOO --
  patching only `encounter->step` would pair a :virtual encounter's own
  :outpatient-visit start with a :discharge end (this set falling
  through to the `:else` branch), silently wrong with no exception."
  [trajectory event]
  (let [opening (referenced-event trajectory event)]
    (if (#{:wellness :ambulatory :virtual} (:encounter-class opening))
      {:type :outpatient-visit-end :citation (citation event)}
      {:type :discharge :citation (citation event)})))

(defn- procedure->step [event] {:type :procedure :codes (:codes event) :citation (citation event)})

(defn- observation-fields
  "Value/unit/value-code/category/reference-range/interpretation,
  verbatim (code passthrough law) -- GMF coverage Wave D stage D1
  (ADR-0029 P1/P2, D1a schema RULING): shared by the top-level
  :observation step compile (its own :citation attached separately,
  below) and each :diagnostic-report child (`diagnostic-report->step`,
  below) -- no citation of its own, embedded, never a separately-cited
  state (D1a-2)."
  [entry]
  (cond-> {:codes (:codes entry)}
    (some? (:value entry)) (assoc :value (:value entry))
    (:unit entry) (assoc :unit (:unit entry))
    (:value-code entry) (assoc :value-code (:value-code entry))
    (:category entry) (assoc :category (:category entry))
    (:reference-range entry) (assoc :reference-range (:reference-range entry))
    (:interpretation entry) (assoc :interpretation (:interpretation entry))))

(defn- observation->step
  [event]
  (assoc (observation-fields event) :type :observation :citation (citation event)))

;; --- GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 R2(a)): both
;; MultiObservation and DiagnosticReport interpret to the SAME
;; trajectory event type (:diagnostic-report, gmf-interpreter.clj), so
;; ONE compile function covers both -- D1a-2's own "one step type, both
;; compile into it."

(defn- diagnostic-report->step
  [event]
  (cond-> {:type :diagnostic-report :observations (mapv observation-fields (:observations event))
           :citation (citation event)}
    (:codes event) (assoc :codes (:codes event))))

(defn- medication-order->step [event] {:type :medication-order :codes (:codes event) :citation (citation event)})

(defn- medication-end->step
  [trajectory event]
  (let [order-event (referenced-event trajectory event)]
    (cond-> {:type :medication-end :citation (citation event)}
      order-event (assoc :order-citation (citation order-event)))))

;; --- GMF coverage Wave D stage D2 (2026-08-02, ADR-0029 R2(b)): the
;; paired CarePlan span -- SAME shape :medication-order/:medication-end
;; already establish one function-pair up (standalone IR step,
;; :references-based back-citation via referenced-event).

(defn- care-plan-start->step
  [event]
  (cond-> {:type :care-plan-start :codes (:codes event) :citation (citation event)}
    (:activities event) (assoc :activities (:activities event))))

(defn- care-plan-end->step
  [trajectory event]
  (let [start-event (referenced-event trajectory event)]
    (cond-> {:type :care-plan-end :citation (citation event)}
      start-event (assoc :care-plan-citation (citation start-event)))))

;; --- GMF coverage Wave C (2026-08-02, ADR-0028, C4): :death ----------------
;; No new IR step type (C4's own rebuttable default) -- reuses :discharge,
;; the existing "close this encounter" primitive, carrying two new optional
;; fields (`ehrt.sim-model.pathway`'s own :discharge schema). Real HL7v2
;; already models a death this way (an ordinary ADT^A03 whose PV1-36
;; carries an expired disposition code, docs/clinical-realities.md's own
;; wire-truth section) -- this is not a workaround, it's the wire shape.

(defn- death->step
  [event]
  (cond-> {:type :discharge :disposition :expired :citation (citation event)}
    (:codes event) (assoc :codes (:codes event))))

(defn- encounter-currently-open?
  "Whether `steps` already carries a compiled encounter-mapped step
  (:admission/:outpatient-visit) with no compiled terminal disposition
  of its own yet -- C4's own 'death inside an encounter' test. Since
  `compile-trajectory`'s own loop already short-circuits everything
  once `encounter-closed?` is true (the cond's own first clause,
  below), reaching this check at all already means no :encounter-end
  has fired -- this only needs to ask whether an encounter was ever
  OPENED, which `encounter-closed?` alone can't answer (C2's own
  synthetic Death-only fixture opens none at all)."
  [steps]
  (boolean (some #(#{:admission :outpatient-visit} (:type %)) steps)))

(def ^:private pre-horizon-dropped-types
  "docs/gmf-interpreter.md section 3's own 'no operational trajectory
  event... during history' -- enforced here (see this namespace's own
  docstring for why the interpreter itself doesn't already do this).
  GMF coverage Wave C (ADR-0028): `:death` joins this set -- a patient
  whose module-driven death fell entirely before this run's own
  registration instant is exactly as irrelevant to THIS run's own
  operational content as a pre-horizon procedure already is; the
  engine-level 'this patient never actually registers' consequence is
  out of this wave's own minimal-path scope (C3), named here rather
  than silently mishandled. GMF coverage Wave D stage D1 (ADR-0029):
  `:diagnostic-report` joins this set -- a pre-horizon MultiObservation/
  DiagnosticReport is exactly as irrelevant to THIS run's own
  operational content as a pre-horizon :observation already is. GMF
  coverage Wave F (2026-08-03, ADR-0036): `:imaging-study` joins this
  set -- it compiles to the SAME IR step family as :procedure (above),
  so a pre-horizon one is exactly as irrelevant as a pre-horizon
  :procedure already is. `:supply-list` does NOT join this set -- it
  never compiles to a step at all, any phase (its own explicit
  unconditional clause, above), so there is nothing for pre-horizon
  dropping to add."
  #{:encounter :encounter-end :procedure :observation :death :diagnostic-report :imaging-study})

(def ^:private pre-horizon-fact-types
  "The ratified item 5 condensed set: ConditionOnset/ConditionEnd/
  MedicationOrder/MedicationEnd, the only pre-horizon events that ever
  become a REGISTRATION-TIME fact rather than being dropped outright.
  GMF coverage Wave D stage D2 (ADR-0029): :care-plan-start/
  :care-plan-end join this set -- a care plan prescribed years before
  registration and still open is exactly as clinically relevant as an
  active medication already is, the same 'ongoing therapeutic content'
  class :medication-order/:medication-end already establish (never the
  ephemeral-clinical-event class :observation/:procedure/:encounter
  sit in)."
  #{:condition-onset :condition-end :medication-order :medication-end
    :care-plan-start :care-plan-end})

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

(defn- history-phase?
  "Wave H pre-roll, Step 3 finding (2026-08-04, ADR-0042): AR-2's own
  encounter-anchored inheritance covers an event INSIDE an open
  encounter, but `:medication-end`/`:care-plan-end`/`:condition-end`
  can legitimately fire OUTSIDE any encounter -- a medication or care
  plan started during a (dropped) history-phase encounter but ended
  after that encounter closed, in horizon, with no encounter open at
  all at the moment it ends (this namespace's own docstring already
  states the reason such spans exist: 'a medication legitimately
  continues... after discharge'). Left unchecked, THAT event's own
  `:phase` reads `:horizon` (nothing open to inherit from) while its
  own `:medication-order`/`:care-plan-start`/`:condition-onset` was
  dropped as history -- an orphaned `:medication-end` etc., found live
  running the real UTI closure under an ordinary seed (AR-4's own
  proof obligation), tripping `check.clj`'s own
  `medication-end-references-existing-order-and-follows-it-in-time`.
  Generalizes AR-2's own 'no orphaned reference to something dropped'
  principle one hop further, along the SAME `:references` back-edge
  `referenced-event` (below) already resolves: an event whose own
  antecedent was itself history-phase is ALSO history-phase, regardless
  of open-encounter state. A no-op for `:encounter-end` (already
  correctly phased by interpreter-level inheritance, so `(:phase
  event)` alone already agrees) and for anything with no `:references`
  at all."
  [trajectory event]
  (or (= :history (:phase event))
      (= :history (:phase (referenced-event trajectory event)))))

(defn- emit-with-delay
  [steps last-t event new-step]
  (let [gap-minutes (* minutes-per-day (- (:t event) last-t))]
    (cond-> steps
      (pos? gap-minutes) (conj {:type :delay :from gap-minutes :to gap-minutes})
      true (conj new-step))))

(defn compile-trajectory
  "clinical-trajectory (a vector of GMF-interpreter trajectory events,
  ehrt.sim-trajectory.gmf-interpreter/run-module's own `:trajectory`) x
  `facility` (this run's own facility config -- where a concrete ward
  name for an emergency/inpatient encounter class comes from) x
  `registration-t` (the same epoch-day instant `run-module` was called
  with, the anchor the FIRST compiled step's own bridging delay is
  measured from) -> {:steps [pathway-ir-step ...] :registration-facts
  [...]}. `:steps` is real pathway IR (`ehrt.sim-model.pathway/valid?`
  holds for `{:name ... :steps steps}`, any real facility); `:registration-
  facts` is this namespace's own resolution of ratified item 5 -- see
  this namespace's own docstring."
  ([trajectory facility registration-t] (compile-trajectory trajectory facility registration-t false))
  ([trajectory facility registration-t history?]
  (loop [events (map-indexed vector trajectory)
         steps []
         registration-facts []
         last-t registration-t
         encounter-closed? false
         straddle-open? false
         suppressed-straddle-spans 0]
    (if (empty? events)
      {:steps steps :registration-facts registration-facts
       :suppressed-straddle-spans suppressed-straddle-spans}
      (let [[idx event] (first events)
            more (rest events)
            event-type (:event event)
            ;; The straddle fix (AR-SF-1): an event's EFFECTIVE
            ;; pre-horizon status is its own raw flag OR "a pre-horizon-
            ;; opened span is still in flight" -- `straddle-open?` is the
            ;; compile-time mirror of `mark-phase`'s own `open-phase`.
            effective-pre-horizon? (or (:pre-horizon event) (and (not history?) straddle-open?))]
        (cond
          ;; This project's own encounter-horizon scope (sim/ADR-0007 point 3:
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
          (recur more steps registration-facts last-t encounter-closed? straddle-open? suppressed-straddle-spans)

          ;; Wave H pre-roll (ADR-0042 AR-1/AR-2, Step 3 finding): history?
          ;; true -- a single uniform drop by `history-phase?` (above):
          ;; the interpreter's own AR-2 phase mark, generalized one
          ;; :references hop further so a :medication-end/:care-plan-end/
          ;; :condition-end whose own antecedent was dropped drops too,
          ;; even when it fires outside any open encounter. No dropped-
          ;; types/fact-types bucketing, no :registration-facts entry.
          ;; See this namespace's own docstring, above.
          (and history? (history-phase? trajectory event))
          (recur more steps registration-facts last-t encounter-closed? straddle-open? suppressed-straddle-spans)

          ;; LEGACY (history? false -- the default, every pre-H caller):
          ;; the straddle fix (above) -- `effective-pre-horizon?` replaces
          ;; the event's own raw `:pre-horizon`, so a span opened by a
          ;; genuinely pre-horizon `:encounter` claims every event up to
          ;; and including its own `:encounter-end`, whatever THEIR own
          ;; raw flags say. `:encounter` opens the span (only reachable
          ;; here with its own raw flag true, since `effective-pre-
          ;; horizon?` for `:encounter` can otherwise only be true via an
          ;; already-open span, and encounters never nest); the matching
          ;; `:encounter-end` closes it and, if its OWN raw flag was
          ;; false, counts one genuine suppressed straddle (AR-SF-7) --
          ;; a fully pre-horizon span (open AND close both raw-true)
          ;; closes the same way, uncounted, byte-identical to every
          ;; pre-H run for that case.
          (and (not history?) effective-pre-horizon? (pre-horizon-dropped-types event-type))
          (recur more steps registration-facts last-t encounter-closed?
                 (case event-type
                   :encounter true
                   :encounter-end false
                   straddle-open?)
                 (if (and (= :encounter-end event-type) (not (:pre-horizon event)))
                   (inc suppressed-straddle-spans)
                   suppressed-straddle-spans))

          (and (not history?) effective-pre-horizon? (pre-horizon-fact-types event-type))
          (recur more steps
                 (conj registration-facts {:event event-type :codes (:codes event)
                                           :citation (citation event) :references (:references event)})
                 last-t encounter-closed? straddle-open? suppressed-straddle-spans)

          (= :encounter event-type)
          (recur more (emit-with-delay steps last-t event (encounter->step facility event))
                 registration-facts (:t event) encounter-closed? straddle-open? suppressed-straddle-spans)

          (= :encounter-end event-type)
          (recur more (emit-with-delay steps last-t event (encounter-end->step trajectory event))
                 registration-facts (:t event) true straddle-open? suppressed-straddle-spans)

          ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-2): ImagingStudy's
          ;; own trajectory event compiles to the SAME IR step family a
          ;; Procedure does -- `procedure->step` reads only :codes/citation,
          ;; both of which an :imaging-study event already carries the
          ;; identical shape of (upstream's own companion-procedure move).
          (#{:procedure :imaging-study} event-type)
          (recur more (emit-with-delay steps last-t event (procedure->step event))
                 registration-facts (:t event) encounter-closed? straddle-open? suppressed-straddle-spans)

          ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-3): SupplyList --
          ;; a log-only trajectory fact, unconditionally (never an IR step,
          ;; any phase) -- the ConditionEnd no-open-encounter precedent
          ;; verbatim, without that precedent's own encounter-open gate.
          (= :supply-list event-type)
          (recur more steps registration-facts last-t encounter-closed? straddle-open? suppressed-straddle-spans)

          (= :observation event-type)
          (recur more (emit-with-delay steps last-t event (observation->step event))
                 registration-facts (:t event) encounter-closed? straddle-open? suppressed-straddle-spans)

          (= :diagnostic-report event-type)
          (recur more (emit-with-delay steps last-t event (diagnostic-report->step event))
                 registration-facts (:t event) encounter-closed? straddle-open? suppressed-straddle-spans)

          (= :medication-order event-type)
          (recur more (emit-with-delay steps last-t event (medication-order->step event))
                 registration-facts (:t event) encounter-closed? straddle-open? suppressed-straddle-spans)

          (= :medication-end event-type)
          (recur more (emit-with-delay steps last-t event (medication-end->step trajectory event))
                 registration-facts (:t event) encounter-closed? straddle-open? suppressed-straddle-spans)

          (= :care-plan-start event-type)
          (recur more (emit-with-delay steps last-t event (care-plan-start->step event))
                 registration-facts (:t event) encounter-closed? straddle-open? suppressed-straddle-spans)

          (= :care-plan-end event-type)
          (recur more (emit-with-delay steps last-t event (care-plan-end->step trajectory event))
                 registration-facts (:t event) encounter-closed? straddle-open? suppressed-straddle-spans)

          (#{:condition-onset :condition-end} event-type)
          (recur more (annotate-condition steps trajectory idx event) registration-facts last-t encounter-closed? straddle-open? suppressed-straddle-spans)

          ;; GMF coverage Wave C (2026-08-02, ADR-0028, C4): death inside
          ;; an encounter attaches as that encounter's own terminal
          ;; disposition (death->step, above); death outside any
          ;; encounter closes the pathway at that timestamp WITHOUT
          ;; fabricating a discharge from an admission that never
          ;; happened (compile-trajectory's own established "no
          ;; attachment point, don't invent one" precedent, the same
          ;; treatment a condition event with no open encounter already
          ;; gets, above). Either way `encounter-closed?` becomes true --
          ;; nothing legitimately follows a death (C2's own terminal
          ;; contract, already enforced one layer down; this is belt-
          ;; and-suspenders, not a new leniency).
          (= :death event-type)
          (if (encounter-currently-open? steps)
            (recur more (emit-with-delay steps last-t event (death->step event)) registration-facts (:t event) true straddle-open? suppressed-straddle-spans)
            (recur more steps registration-facts last-t true straddle-open? suppressed-straddle-spans))

          :else
          (recur more steps registration-facts last-t encounter-closed? straddle-open? suppressed-straddle-spans)))))))
