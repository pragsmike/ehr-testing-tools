(ns ehrt.patient-simulator.emittable-events
  "WHICH GROUND-TRUTH EVENT TYPES A VENDORED CLOSURE CAN DRIVE -- the
  declared table behind the generator-side event-type coverage gate
  (ADR-0165, P1(a)).

  ADR-0160 gave the JUDGE side a coverage gate: every oracle root is
  exercised. Nothing measured the GENERATOR side. That gap is exactly
  what let ADR-0163's defect sit invisible: the invariant catalog was
  correct, but the population it ran over produced zero-to-one
  `:medication-end` events, so `medication-end-references-existing-
  order-and-follows-it-in-time` had almost nothing to judge. A gate
  that asserts the gated runs COLLECTIVELY exercise every event type
  the vendored closures can emit is the meter that was missing.

  This table is that gate's input. One row per state type
  `ehrt.patient-simulator.gmf-interpreter/step` dispatches on, carrying
  two facts:

  - `:trajectory-event` -- the trajectory event type the interpreter
    emits for that state type, or `nil` for a state consumed internally
    (no trajectory event at all: scratch writes, transitions, waits).
  - `:ground-truth` -- the ground-truth event types that trajectory
    event can reach through `ehrt.patient-simulator.compile-trajectory`
    and `ehrt.sim-engine.engine`'s own `decide`. `#{}` means the
    compile layer never turns it into an IR step, so it never reaches a
    log at all -- a REAL trajectory fact with no operational
    projection, not an oversight.

  BOTH columns are gated, not asserted:
  `ehrt.patient-simulator.emittable-events-test` reads
  `gmf_interpreter.clj` with the Clojure reader (the
  `ehrt.docs-tooling.sim-purity-lint-test` discipline -- never a regex
  over raw text) and fails when this table's key set diverges from
  `step`'s own `case` dispatch constants, or when its `:trajectory-
  event` value set diverges from the event-type keywords the
  interpreter's own `emit-and-advance`/`trajectory-event` call sites
  pass. A state type added to the interpreter without a row here is a
  test failure, by construction.

  The `:ground-truth` column is DECLARED against `compile-trajectory`'s
  own clause set, each row cited below, and its own proof obligation is
  discharged empirically rather than syntactically: the coverage gate
  reads real corpora, so a row claiming a type the pipeline cannot
  actually produce shows up as a permanently-unsatisfiable gate, and a
  row claiming `#{}` for a type that DOES appear shows up in the
  measured matrix. ADR-0165 records that matrix as landed.")

(def state-type->emittable
  "state type -> {:trajectory-event ... :ground-truth #{...}}. See this
  namespace's own docstring for the contract and for what gates it.

  Line citations are into `gmf_interpreter.clj` (the `:trajectory-
  event` column) and `compile_trajectory.clj` (the `:ground-truth`
  one), both at ADR-0165."
  {;; --- consumed internally: no trajectory event at all -------------
   ;; `pass-through-outcome` with an empty events vector (or, for
   ;; :terminal, a literal outcome map). Scratch writes, transitions,
   ;; guards and waits -- real interpreter work, never a log fact.
   :initial        {:trajectory-event nil :ground-truth #{}}
   :terminal       {:trajectory-event nil :ground-truth #{}}
   :simple         {:trajectory-event nil :ground-truth #{}}
   :delay          {:trajectory-event nil :ground-truth #{}}
   :guard          {:trajectory-event nil :ground-truth #{}}
   :set-attribute  {:trajectory-event nil :ground-truth #{}}
   :symptom        {:trajectory-event nil :ground-truth #{}}
   :counter        {:trajectory-event nil :ground-truth #{}}
   ;; Device/DeviceEnd: consumed-internally by ruling, not by accident
   ;; -- no equipment-tracking home exists (gmf.clj's own
   ;; `gmf-type->keyword` M5b note).
   :device         {:trajectory-event nil :ground-truth #{}}
   :device-end     {:trajectory-event nil :ground-truth #{}}
   ;; VitalSign writes ctx's own :vital-signs register, never the
   ;; trajectory (ADR-0039 AR-1/AR-2).
   :vital-sign     {:trajectory-event nil :ground-truth #{}}
   ;; CallSubmodule emits no event OF ITS OWN: `call-submodule-step`
   ;; returns the CALLEE's events, each already attributed to the
   ;; callee's own state type by the callee's own dispatch. Giving this
   ;; row the callee's types would double-count them.
   :call-submodule {:trajectory-event nil :ground-truth #{}}

   ;; --- a trajectory event that never becomes an IR step ------------
   ;; ConditionOnset/ConditionEnd compile to an ANNOTATION on an
   ;; already-compiled encounter step (`annotate-condition`), never to
   ;; a standalone step -- so they ride a ground-truth event's own
   ;; `:conditions` vector and are never an event themselves. ADR-0163
   ;; Step 2 turns on exactly this fact.
   :condition-onset {:trajectory-event :condition-onset :ground-truth #{}}
   :condition-end   {:trajectory-event :condition-end   :ground-truth #{}}
   ;; SupplyList: a log-only trajectory fact, compiled to NO IR step in
   ;; any phase (`compile-trajectory`'s own explicit clause, ADR-0036
   ;; AR-3).
   :supply-list     {:trajectory-event :supply-list     :ground-truth #{}}
   ;; AllergyOnset/Vaccine reach `compile-trajectory`'s `:else` -- real
   ;; trajectory events (ADR-0040 AR-5) with no compile clause of their
   ;; own, so no IR step and no log event. Named here rather than left
   ;; to be rediscovered.
   :allergy-onset   {:trajectory-event :allergy-onset   :ground-truth #{}}
   :vaccine         {:trajectory-event :vaccine         :ground-truth #{}}

   ;; --- reaches the log ---------------------------------------------
   ;; Encounter -> `encounter->step`: :emergency/:inpatient compile to
   ;; an :admission step, :wellness/:ambulatory/:virtual to an
   ;; :outpatient-visit one (ADR-0133). Both step types name their own
   ;; ground-truth event.
   :encounter       {:trajectory-event :encounter
                     :ground-truth #{:admission :outpatient-visit}}
   ;; WellnessWait mints the SAME raw :encounter event, always with
   ;; `:encounter-class :wellness` -- so only the outpatient half of
   ;; the pair above is reachable from it (`wellness-wait-step`).
   :wellness-wait   {:trajectory-event :encounter
                     :ground-truth #{:outpatient-visit}}
   ;; EncounterEnd -> `encounter-end->step`, which reads the OPENING
   ;; encounter's own class: outpatient-family opens pair with
   ;; :outpatient-visit-end, everything else with :discharge.
   :encounter-end   {:trajectory-event :encounter-end
                     :ground-truth #{:discharge :outpatient-visit-end}}
   :procedure       {:trajectory-event :procedure       :ground-truth #{:procedure}}
   ;; ImagingStudy compiles through `procedure->step` -- upstream's own
   ;; companion-procedure move (ADR-0036 AR-2). It is therefore
   ;; INDISTINGUISHABLE from a Procedure in the log: coverage of
   ;; :procedure is all this row can ever ask for.
   :imaging-study   {:trajectory-event :imaging-study   :ground-truth #{:procedure}}
   :observation     {:trajectory-event :observation     :ground-truth #{:observation}}
   ;; MultiObservation and DiagnosticReport are two loadable state
   ;; types sharing ONE trajectory event type (ADR-0029 R2(a)).
   :multi-observation {:trajectory-event :diagnostic-report :ground-truth #{:diagnostic-report}}
   :diagnostic-report {:trajectory-event :diagnostic-report :ground-truth #{:diagnostic-report}}
   :medication-order  {:trajectory-event :medication-order  :ground-truth #{:medication-order}}
   :medication-end    {:trajectory-event :medication-end    :ground-truth #{:medication-end}}
   :care-plan-start   {:trajectory-event :care-plan-start   :ground-truth #{:care-plan-start}}
   :care-plan-end     {:trajectory-event :care-plan-end     :ground-truth #{:care-plan-end}}
   ;; Death -> `death->step`, which is a :discharge step carrying
   ;; `:disposition :expired` (ADR-0028 C4). There is no `:death`
   ;; ground-truth event kind at all -- the log's own 23-kind closed
   ;; vocabulary (`ehrt.sim-engine.event-schema/Event`) has none.
   :death             {:trajectory-event :death            :ground-truth #{:discharge}}})

(defn emittable-ground-truth-events
  "Every ground-truth event type the loaded `closures` can drive --
  the union of `state-type->emittable`'s `:ground-truth` over every
  state of every module of every closure.

  `closures` is a sequence of `ehrt.patient-simulator.gmf/load-closure`
  payloads (`ehrt.sim.run/resolve-modules`' own return shape). Reads
  what a closure ACTUALLY declares, so the answer narrows with the
  scenario rather than describing the interpreter in the abstract."
  [closures]
  (into #{}
        (mapcat (fn [closure]
                  (for [[_ module] (:modules closure)
                        [_ state] (:states module)
                        event-type (:ground-truth (get state-type->emittable (:type state)))]
                    event-type)))
        closures))
