(ns ehrt.sim-trajectory.gmf-interpreter
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
  two-phase run (Task 3, `ehrt.sim-trajectory.gmf-interpreter.horizon` or
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
  walk (no progress) UNLESS the condition is analytically resolvable --
  in that case, `step` computes the exact virtual-clock advance needed
  to satisfy it (a deterministic date computation, consuming NO rng
  draw) and proceeds, rather than blocking. This is the mechanism that
  lets 'wait until old enough' Guards make progress under this
  project's own 'no fixed tick' design (docs/gmf-interpreter.md section 3)
  without reintroducing the tick loop that design deliberately rejects:
  the jump is exactly as much virtual time as the one age threshold
  needs, not a polling interval. A Guard blocked on any other condition
  simply halts progress -- a module author's own responsibility to
  route around (the same responsibility real Synthea's own Delay-then-
  Guard idiom already carries), not something this interpreter resolves
  for them. v1 (M5a): a bare `:age` condition, operator `>=` only. GMF
  coverage Wave D stage D3 (2026-08-02, ADR-0029, D3e, H4, sound-jump-
  or-escalate): extended to bare `:age` with operator `>` (the day-vs-
  year integer-age-flooring boundary a strict inequality needs), and to
  an `:and` compound containing exactly one Age sub-condition whose
  every OTHER sibling is non-time-dependent and already holds (`age-
  guard-jump-days`'s own docstring has the full soundness argument) --
  any other compound shape (two Age conditions, `:date` alongside
  `:age`, `:or`/`:at-least` wrapping one) is a named, unbuilt
  escalation, not a heuristic jump.

  GMF coverage Wave B (2026-08-02, ADR-0027): CallSubmodule call/return
  and D4's own determinism-threading order contract. ONE clock (`:t`)
  and ONE rng stream are shared across an entire walk, callee included
  -- `step`'s own optional 4th argument, `modules` (call-path -> loaded
  module, `ehrt.sim-trajectory.gmf/load-closure`'s own return shape),
  is consulted ONLY by the `:call-submodule` case. Consumption order is
  DESCEND-RUN-RETURN: reaching a CallSubmodule state descends into the
  callee's own `:initial` state immediately (`run-submodule`), runs it
  to ITS OWN Terminal -- consuming rng in exactly the order the
  callee's own states would consume it standalone, since it IS run
  standalone, just with a shared clock/rng/attributes/trajectory
  (D1's own three-compartment ctx, section 5's own dated note) --  then
  RETURNS control to the caller's own CallSubmodule state, which
  resolves ITS OWN transition using the POST-CALL ctx (attributes as
  the callee left them). A called submodule that itself BLOCKS on a
  Guard is a disclosed, out-of-scope limitation this session (throws,
  loudly, rather than silently mishandling a resume this interpreter
  has no mechanism for yet) -- neither vendored Wave B closure ever
  exercises it (confirmed: zero Guard states in either of
  `ear_infections.json`'s own two called submodules, Step 1's own
  characterization). D2's own cross-boundary citation: every trajectory
  event emitted while `ctx`'s own `:call-stack` is active (root-first,
  the callee always last) carries `:call-path` in addition to `:module`
  -- absent entirely for a non-calling walk, the same trajectory shape
  this project's pre-Wave-B regression oracle already fixes (backward-
  compatible representation, D2's own latitude). A defensive
  `max-call-depth` backstop (D3) throws if the call stack ever exceeds
  it -- a bug signal (e.g. a static-acyclicity gap `gmf/load-closure`'s
  own D3 check should have caught), never a legitimate result.

  GMF coverage Wave D stage D1 (2026-08-02, ADR-0029, D1a schema
  RULING): `:multi-observation`/`:diagnostic-report` both compile to
  ONE trajectory event type, `:diagnostic-report` (D1a-2's own shared-
  ObservationGroup-parent grounding, R2(a)) -- `sample-observation-
  extra` gains `value_code`/`vital_sign` branches (D1a-3's other two
  value-sourcing mechanisms) alongside the pre-existing `range` branch,
  applied identically whether sampling a standalone `:observation`
  state or one embedded `:diagnostic-report` child. RNG order contract,
  extended: a `:multi-observation`/`:diagnostic-report` state consumes
  its children's own draws, IN VECTOR ORDER, each exactly as
  `sample-observation-extra`'s own per-branch rule already states
  (`range`/`vital_sign`: one draw; `value_code`/neither: zero) -- no
  draw of the parent state's own, beyond what its children need."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.sim-trajectory.gmf :as gmf])
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

;; GMF coverage Wave VS (2026-08-04, ADR-0039 AR-1/AR-3): the vital-sign
;; register -- a per-patient `:vital-signs` compartment, `{keyword
;; current-value}`, GLOBAL over the whole walk (never root-scoped the
;; way workflow `:attributes` is, ADR-0027 D1's own third compartment --
;; a vital-sign reading is a clinical fact, not CallSubmodule workflow
;; scratch). Written once per name by the `VitalSign` state (sample-
;; once, AR-2); read by the `:vital-sign` condition. Seeded at patient
;; creation from `vital-sign-baselines.edn`'s own flat authored
;; constants (AR-3) -- ZERO rng draws, so this seeding never touches the
;; rng stream (a fixed-consumption-law no-op, same as every other
;; constant-only initialization in this namespace). Keys are the SAME
;; `gmf/slug` transform every other free-form GMF name already goes
;; through (:attribute/:symptom's own keying), applied to the vital-
;; sign's own raw name string -- "mapped alongside" `sim-trajectory/
;; vital-signs.edn`'s own string-keyed vocabulary by construction (one
;; transform, not a hand-maintained second mapping table).
(def ^:private vital-sign-baseline-table
  (edn/read-string (slurp (io/resource "sim-trajectory/vital-sign-baselines.edn"))))

(defn- vital-sign-key
  [name] (keyword (gmf/slug name)))

(defn- baseline-vital-signs
  []
  (into {} (map (fn [[name value]] [(vital-sign-key name) value])) vital-sign-baseline-table))

;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029, D1a-4, D1a schema
;; RULING Q2+Q3): the vital-sign reference table -- this project's own
;; documented simplification for a real upstream mechanism (Synthea's
;; `LifecycleModule.java`) it has never ported, D1a-4's own finding.
;; Loaded once at namespace load time, the same "small, hand-curated,
;; hashed content" treatment ehrt.sim-engine.order-profiles' own resources/
;; order-profiles.edn already establishes for the analogous lab-analyte
;; table. Relocated here (2026-08-04, ADR-0039) from beside its own
;; original sole consumer, `vital-sign-extra` (below) -- the NEW
;; `VitalSign` state and `:vital-sign` condition (both Steps this same
;; wave) need it BEFORE this file's own top-to-bottom definition order
;; would otherwise reach it (evaluate-condition, above initial-context
;; even, is defined well before `vital-sign-extra`'s own prior home)."
(def ^:private vital-sign-reference-table
  (edn/read-string (slurp (io/resource "sim-trajectory/vital-signs.edn"))))

;; GMF coverage Wave VS (2026-08-04, ADR-0039 AR-1): the closed-
;; vocabulary check, shared by THREE consumers -- the pre-existing
;; Observation-family reader (`vital-sign-extra`, below), the NEW
;; `VitalSign` state, and the NEW `:vital-sign` condition -- one table,
;; one disposition (a real, visible rejection, never a silent nil).
(defn- validate-vital-sign-name
  [vital-sign-name]
  (when-not (contains? vital-sign-reference-table vital-sign-name)
    (throw (ex-info "ehrt.sim-trajectory.gmf-interpreter: unrecognized vital-sign name -- not in sim-trajectory/vital-signs.edn"
                     {:unrecognized-vital-sign vital-sign-name}))))

(defn initial-context
  "The patient-ctx a fresh module walk starts from: current state
  `:initial` (every GMF module's own entry-point convention), virtual
  time at the persona's own DOB, an empty attributes map, and an empty
  accumulating trajectory.

  GMF coverage Wave VS (2026-08-04, ADR-0039 AR-3): `:vital-signs`
  seeds from `vital-sign-baseline-table` (above) -- every OTHER name
  (e.g. `Left ventricular Ejection fraction`, deliberately absent from
  that table) starts genuinely unset, honest-absence territory until a
  `VitalSign` state writes it (AR-4).

  EncounterEnd fix (2026-08-08, ADR-0082, R2): `:suppressed-encounter-
  ends` counts A5-arm `:encounter-end` states that fired with nothing
  open (`step`'s own `:encounter-end` case, below) -- a legal upstream
  no-op (docs/gmf-interpreter.md's own dated section 4 note), never an
  error, but zero-cost-countable per the standing error-honesty lesson.
  Threaded as a full-value passthrough exactly like `:attributes`/
  `:vital-signs` (every outcome-constructing site in this namespace
  carries it forward unchanged unless the `:encounter-end` A5 branch
  bumps it) -- open-encounter STATE itself is NOT threaded here at all;
  `open-encounter-index` (below, retiring `index-of-last-open-
  encounter`) recomputes it by a pure fold over `ctx`'s own trajectory
  on demand, cheap at this project's own walk sizes and simpler than
  keeping a second piece of derived state in sync."
  [persona]
  {:current :initial :t (dob-epoch-day persona) :attributes {} :persona persona :trajectory []
   :vital-signs (baseline-vital-signs) :suppressed-encounter-ends 0})

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

(defn- age-months-at
  "Total whole months since DOB (years*12 + months, remaining days
  ignored) -- `java.time.Period/toTotalMonths`, the SAME `Period/between`
  this namespace's own `age-years-at` already computes, just read a
  different way. Only the wellness cadence table's own months-tier
  (below) needs this; every other age check in this namespace is
  years-only."
  ^long [persona ^long t]
  (.toTotalMonths (Period/between (parse-dob persona) (LocalDate/ofEpochDay t))))

;; --- GMF coverage Wave G (2026-08-03, ADR-0037 AR-1/AR-2): wellness
;; cadence -- a PURE, ZERO-DRAW schedule function synthesizing Synthea's
;; own periodic-tick wellness cycle (`EncounterModule.process`'s own
;; daily re-check against `recommendedTimeBetweenWellnessVisits`, source-
;; grounded, `resources/sim-trajectory/wellness-cadence.edn`'s own
;; header), since this interpreter has no fixed tick of its own
;; (docs/gmf-interpreter.md section 3's own no-fixed-tick design). -------

(def ^:private wellness-cadence-table
  (edn/read-string (slurp (io/resource "sim-trajectory/wellness-cadence.edn"))))

(defn- band-lookup
  "The first row (table order) whose own `:max-age` the query `age`
  satisfies, or a trailing `nil`-`:max-age` row (the source's own final
  `else`) -- never falls through to nil, `wellness-cadence.edn`'s own
  two vectors both end in one."
  [rows ^long age]
  (some (fn [{:keys [max-age quantity unit]}]
          (when (or (nil? max-age) (<= age max-age))
            {:quantity quantity :unit unit}))
        rows))

(defn- wellness-cadence-band
  "AR-1's own table: age <= 3 years dispatches on MONTHS
  (`:under-3-by-age-months`), otherwise on YEARS
  (`:three-plus-by-age-years`) -- the source's own two-tier `if
  (ageInYears <= 3)`, EXCLUDING that method's own chronic-medications
  cap (AR-1's own named deferral, the calibration register)."
  [persona ^long t]
  (if (<= (age-years-at persona t) 3)
    (band-lookup (:under-3-by-age-months wellness-cadence-table) (age-months-at persona t))
    (band-lookup (:three-plus-by-age-years wellness-cadence-table) (age-years-at persona t))))

(defn next-wellness-tick
  "AR-2: the first Synthea-cadence wellness-visit tick STRICTLY AFTER
  `t`, a PURE function of `persona`'s own DOB and `t` -- ZERO rng draws
  (load-bearing for AR-6: every non-wellness walk's own rng stream is
  untouched by this function's own existence). Anchored at DOB (the
  recurrence's own tick0 is DOB itself, upstream's own very first
  wellness check passing immediately since `person.record.
  timeSinceLastWellnessEncounter` starts effectively infinite); each
  subsequent tick is the PREVIOUS tick plus THAT tick's own age-banded
  interval (`wellness-cadence-band`, above) -- AR-2's own ratified
  recurrence, 'next = previous + band(age)', re-expressing upstream's
  own daily re-check as a closed iteration since this interpreter has no
  fixed tick to re-check on.

  STRICT, not `>=` -- a live finding (found running this Wave's own
  census against the real `med_rec.json`, not merely anticipated): its
  own wellness-wait loop has ZERO delay anywhere in the loop body
  (Wellness_Encounter -> ... -> EncounterEnd -> Initial ->
  ConditionOnset -> Wellness_Encounter again, all zero-time states). An
  inclusive `>=` first-call at `t` = DOB returns DOB itself (zero
  advance); re-entering the SAME wellness-wait state at that same
  unchanged `t` then returns the SAME tick again, forever -- an
  infinite zero-advance spin into `max-steps`, exactly upstream's own
  never-fires-twice-at-the-same-instant guarantee (its own
  `timeSinceLastWellnessEncounter` resets to zero the moment an
  encounter fires, so the NEXT check always needs a genuinely NEW
  interval to elapse). Strict `>` guarantees every call returns a tick
  strictly later than its own `t` argument, so even a zero-delay loop
  body genuinely advances on every iteration -- the property AR-7's own
  loop-bounding acceptance evidence depends on."
  ^long [persona ^long t]
  (loop [tick (dob-epoch-day persona)]
    (if (> tick t)
      tick
      (let [{:keys [quantity unit]} (wellness-cadence-band persona tick)]
        (recur (advance-date tick unit quantity))))))

;; --- RNG primitives (fixed-consumption law, per ehrt.sim-engine.engine/
;; ehrt.sim-model.persona's own precedent) ----------------------------------

(defn- rand-int-in [^Random rng lo hi] (+ lo (.nextInt rng (inc (- hi lo)))))
(defn- rand-double-in [^Random rng lo hi] (+ lo (* (.nextDouble rng) (- hi lo))))

;; ADR-0040 AR-2: SetAttribute's own :range value source -- `Person.rand
;; (low, high, decimals)` semantics (`RandomNumberGenerator.java`, source-
;; grounded): one uniform draw, then HALF-UP rounding to :decimals places
;; when present (BigDecimal.setScale's own rounding mode, mirrored with
;; plain double arithmetic -- this project's fixed-consumption law only
;; constrains the DRAW count, never the post-draw arithmetic used to
;; shape it, the same latitude `sample-distribution`'s own `:round`
;; handling already takes).
(defn- round-half-up ^double [^double v ^long decimals]
  (let [factor (Math/pow 10.0 decimals)]
    (/ (double (Math/round (* v factor))) factor)))

(defn- sample-set-attribute-range
  ^double [^Random rng {:keys [low high decimals]}]
  (let [v (rand-double-in rng low high)]
    (if decimals (round-half-up v decimals) v)))

;; --- ADR-0035 (Wave F0) AR-1/AR-3: GAUSSIAN/EXPONENTIAL/TRIANGULAR
;; sampling -- ehrt.sim-trajectory.gmf's own normalized SampledDistribution
;; shape, sampled here with EXACTLY ONE rng draw per kind (EXACT: zero),
;; the fixed-consumption law every other stochastic choice in this
;; project already follows. -------------------------------------------------

(def ^:private probit-a
  [-3.969683028665376e+01 2.209460984245205e+02 -2.759285104469687e+02
   1.383577518672690e+02 -3.066479806614716e+01 2.506628277459239e+00])
(def ^:private probit-b
  [-5.447609879822406e+01 1.615858368580409e+02 -1.556989798598866e+02
   6.680131188771972e+01 -1.328068155288572e+01])
(def ^:private probit-c
  [-7.784894002430293e-03 -3.223964580411365e-01 -2.400758277161838e+00
   -2.549732539343734e+00 4.374664141464968e+00 2.938163982698783e+00])
(def ^:private probit-d
  [7.784695709041462e-03 3.224671290700398e-01 2.445134137142996e+00
   3.754408661907416e+00])

(defn- horner ^double [coeffs ^double x]
  (reduce (fn [^double acc ^double c] (+ (* acc x) c)) 0.0 coeffs))

(defn- probit-approx
  "ADR-0035 AR-3: a single-draw substitute for `java.util.Random/
  nextGaussian` -- `nextGaussian` consumes a VARIABLE number of draws
  (the polar Box-Muller method, retrying on rejection) and caches a
  spare value across calls, both incompatible with this project's
  fixed-consumption law. Peter Acklam's rational approximation of the
  standard-normal inverse CDF (public domain; source: https://
  web.archive.org/web/20151030215612/http://home.online.no/~pjacklam/
  notes/invnorm/), claimed accuracy ~1.15e-9 absolute error, no
  refinement step needed. `p` in (0, 1) -> the z such that Phi(z) = p;
  GAUSSIAN sampling below calls this with exactly one `.nextDouble`
  draw as `p`."
  ^double [^double p]
  (let [p-low 0.02425 p-high (- 1.0 p-low)]
    (cond
      (< p p-low)
      (let [q (Math/sqrt (* -2.0 (Math/log p)))]
        (/ (horner probit-c q) (inc (* q (horner probit-d q)))))

      (<= p p-high)
      (let [q (- p 0.5) r (* q q)]
        (/ (* q (horner probit-a r)) (inc (* r (horner probit-b r)))))

      :else
      (let [q (Math/sqrt (* -2.0 (Math/log (- 1.0 p))))]
        (- (/ (horner probit-c q) (inc (* q (horner probit-d q)))))))))

(defn- sample-distribution
  "ADR-0035 AR-1/AR-3: samples `ehrt.sim-trajectory.gmf`'s own normalized
  SampledDistribution shape -- the values Distribution.java's own
  `generate` computes (fetched-source pin
  7e08387c68a7f0e21d13076609a159fd473fc902), ported verbatim except
  GAUSSIAN's own draw (`probit-approx`, above -- a DISCLOSED numeric
  divergence from `nextGaussian`, fitness-for-purpose not bit-parity,
  AR-3's own ruling). EXACT: zero draws (mirrors Distribution.java's own
  EXACT branch, which never calls `person.rand()` either). UNIFORM/
  GAUSSIAN/EXPONENTIAL/TRIANGULAR: exactly one `.nextDouble` draw,
  regardless of which branch, the same fixed-consumption law
  `weighted-pick-transition`/`rand-int-in` already establish. `:round`
  (when true) rounds the SAMPLED VALUE to the nearest integer, per
  Distribution.java's own trailing `Math.round` -- applied AFTER any
  GAUSSIAN clamp, matching source order."
  ^double [^Random rng {:keys [kind parameters round]}]
  (let [value
        (case kind
          :exact (double (:value parameters))
          :uniform (rand-double-in rng (:low parameters) (:high parameters))
          :gaussian
          (let [{:keys [mean standard-deviation min max]} parameters
                raw (+ mean (* standard-deviation (probit-approx (.nextDouble rng))))]
            (cond-> raw
              (some? min) (clojure.core/max min)
              (some? max) (clojure.core/min max)))
          :exponential
          (let [mean (:mean parameters) lambda (/ -1.0 mean)]
            (+ 1.0 (/ (Math/log (- 1.0 (.nextDouble rng))) lambda)))
          :triangular
          (let [{:keys [min mode max]} parameters
                f (/ (- mode min) (- max min))
                r (.nextDouble rng)]
            (if (< r f)
              (+ min (Math/sqrt (* r (- max min) (- mode min))))
              (- max (Math/sqrt (* (- 1.0 r) (- max min) (- max mode)))))))]
    (if round (double (Math/round ^double value)) value)))

(defn- resolve-time-advance
  "How much virtual time a Delay (or a Procedure's own :duration) advances
  from `t`: `:exact` is deterministic, NO rng draw; `:range` samples
  exactly one uniform integer draw, the same fixed-consumption law every
  other stochastic choice in this project already follows. Neither
  present -> no advance, no draw (a state with no timing info of its
  own).

  ADR-0035 AR-3: a `:distribution` (GAUSSIAN/EXPONENTIAL/TRIANGULAR,
  `sample-distribution` above -- UNIFORM/EXACT never reach here as
  :distribution, D3c's own v1-collapse already turns those into :range/
  :exact before this function ever sees them) samples a DOUBLE in unit
  space, then converts to a whole unit-count via `Math/round` (round-
  half-up) AT THIS conversion boundary -- `advance-date` needs a `long`
  day/week/month/year count regardless of the distribution's own :round
  flag (which governs the SAMPLED VALUE for non-timing consumers, e.g.
  SetAttribute's :round true -- a double duration always needs a long
  day-count here, independent of whether the state's own author asked
  for value-level rounding too). The deterministic rounding-to-
  granularity choice this ADR's own AR-3 names."
  [^Random rng ^long t {:keys [range exact distribution]}]
  (cond
    exact (advance-date t (:unit exact) (long (:quantity exact)))
    range (advance-date t (:unit range) (rand-int-in rng (long (:low range)) (long (:high range))))
    distribution (advance-date t (:unit distribution) (Math/round (sample-distribution rng distribution)))
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

(defn- root-id
  "GMF coverage Wave B (2026-08-02, ADR-0027, D1): the walk's own ROOT
  module id -- workflow attributes (SetAttribute/Symptom writes,
  Attribute/Symptom condition reads -- ctx's own THIRD compartment, D1's
  own three-compartment person record) are namespaced under THIS,
  never `module-id` (the module CURRENTLY executing, which
  `CallSubmodule`, once built, can differ from) -- so a callee and its
  caller resolve the same bare attribute name in one shared namespace.
  Defaults to `module-id` when `ctx` carries no explicit `:root` --
  correct for every non-calling walk (root = self, byte-identical to
  this project's pre-Wave-B behavior by construction) and for a `ctx`
  built directly against `step` without going through
  `walk-module`/`run-module`'s own one-time `:root` normalization (both
  test fixtures and this namespace's own docstring examples do this)."
  [ctx module-id]
  (or (:root ctx) module-id))

(defn- attribute-condition-holds?
  "GMF coverage Wave B (2026-08-02, ADR-0027): `is nil`/`is not nil`
  join `!=`/`=` -- confirmed mandatory-path by Step 1's own
  characterization (`ear_infections.json`'s own `End_Ear_Infection_
  Medications`, and both called submodules' own idempotency-gating
  `Initial` states, docs/gmf-interpreter.md section 9)."
  [module-id ctx {:keys [attribute operator value]}]
  (let [k (keyword (root-id ctx module-id) (gmf/slug attribute))
        actual (get (:attributes ctx) k)]
    (case operator
      "!=" (not= actual value)
      "is nil" (nil? actual)
      "is not nil" (some? actual)
      (= actual value))))

(defn- date-condition-holds?
  "GMF coverage Wave A (2026-08-02): Synthea's own Logic.java Date class
  (`currentyear = Utilities.getYear(time); compare(currentyear, year,
  operator)`) -- this project's own equivalent of 'the simulated calendar
  year' is `ctx`'s own virtual clock (:t, an epoch-day anchored to the
  persona's real DOB since M5a), already-threaded data, no new state
  home. v1 scope: :year only (real Synthea's own Date condition also
  supports :month/:date variants -- not observed on any candidate module's
  own mandatory path this session, so out of scope per this project's own
  narrow-per-need curation discipline, sim/ADR-0013 point 4)."
  [{:keys [operator year]} ^long t]
  (compare-op operator (.getYear (LocalDate/ofEpochDay t)) year))

(defn- symptom-condition-holds?
  "GMF coverage Wave A (2026-08-02): the log-query family's own attribute-
  read shape, applied to a Symptom's OWN severity write -- reads the SAME
  module-namespaced key the already-built :symptom STATE type writes
  (gmf-interpreter's own `step`), defaulting to 0 when never sampled,
  exactly mirroring Synthea's own `Person.getSymptom` default (grounded
  against Logic.java/Person.java at docs/gmf-interpreter.md's own pinned
  commit) -- a module may check a symptom before ever writing it on some
  branch, and 0 (never happened yet) is the correct default, not a missing-
  key error. GMF coverage Wave B (2026-08-02, ADR-0027, D1): reads the
  ROOT-namespaced key (`root-id`), not `module-id` -- symptom severity
  is workflow scratch, the same third compartment SetAttribute shares."
  [module-id ctx {:keys [symptom operator value]}]
  (let [k (keyword (root-id ctx module-id) (gmf/slug symptom))]
    (compare-op operator (get (:attributes ctx) k 0) value)))

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

(defn- latest-observation-value
  "The :value of the most recent :observation trajectory event whose own
  :codes match `condition-codes` (`code-matches?`, above -- already shared
  with :active-condition/:active-medication) -- most-recent-first, the
  same 'most recent' rule PriorState/Active Condition already establish."
  [ctx condition-codes]
  (some (fn [event] (when (and (= :observation (:event event)) (code-matches? (:codes event) condition-codes))
                       (:value event)))
        (rseq (vec (:trajectory ctx)))))

(defn- observation-condition-holds?
  "GMF coverage Wave A (2026-08-02): Synthea's own Logic.java Observation
  class -- the most recent matching-code observation's value, compared via
  :operator. Already-existing data: the accumulating :trajectory (the
  value itself was sampled and carried by the already-built :observation
  STATE type, M5a).

  FIXED (2026-08-04, ADR-0040 AR-3): used to THROW when no matching
  observation was ever recorded, the 'module-authoring-shape bug'
  disposition -- corrected against the pin (`Logic.java`'s own
  `Observation.test`, source-grounded): upstream's DEFAULT path
  (`exporter.split_records` unset, the standing config value in every
  context this project's own census runs under) also throws a
  NullPointerException here for a comparison operator with no matching
  observation, the SAME 'required precondition, module-authoring bug'
  reading this project's own prior throw already had right. What
  changes is the RULING, not the source: upstream's `exporter.
  split_records=true` branch instead returns `false` (its own
  documented issue-774 band-aid) -- this project deliberately ADOPTS
  that band-aid's behavior unconditionally, since this project has no
  split-records/lossOfCare concept of its own for the config to
  meaningfully gate (the same 'simplify a upstream config axis this
  project doesn't model' disposition `:active-allergy`'s own
  always-false simplification and `type-of-care-weights`'s own
  always-typical-emergency branch already establish) -- a deliberate
  simplification the ADR discloses, not a misreading of the band-aid as
  upstream's only behavior. `anemia___unknown_etiology.json`'s own
  `anemia_sub` submodule is the real, found gap this session's AR-3
  closes (a Hematocrit Observation condition reached before any
  Hematocrit was ever recorded on some branch).

  Absence is what THIS CONDITION TESTS -- 'no matching observation yet'
  is real, expected module state on some branches, not a programmer
  error, so `false` is the correct semantics here, not a silent
  default: distinguished from this namespace's own `honest-absence`
  doctrine (`race`/`socioeconomic-status`/`vital-sign`/lookup-table-
  column), which reserves a WALK-ERROR result for a genuinely
  unconfigured field a module's author had every reason to expect
  present. `is nil`/`is not nil` (upstream's own explicit absence
  tests, handled BEFORE this null-check on the pin) stay OUT of v1
  scope, unchanged -- no candidate module this session needs them
  either (the same omission this function's own docstring already
  named, pre-dating this fix)."
  [{:keys [operator codes value]} ctx]
  (if-let [obs-value (latest-observation-value ctx codes)]
    (compare-op operator obs-value value)
    false))

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

;; GMF coverage Wave I2 (2026-08-04, ADR-0041 AR-2): :active-careplan --
;; Synthea's own Logic.java ActiveCarePlan class (ActiveLogic's own
;; parent `test`, source-grounded at the pin: :codes checked first,
;; :referenced-by-attribute only when :codes is absent, a bare condition
;; with neither throwing upstream -- READ the parent, not merely the
;; ActiveCarePlan subclass, since the dispatch itself lives there).

(defn- careplan-active-by-reference?
  "Does ctx's own trajectory carry NO :care-plan-end event whose own
  :references cites `idx` (a trajectory index, the SAME index-based
  reference :care-plan-end's own :references field already carries,
  `index-of-citation`'s shape) -- the referenced-by-attribute form's own
  'is THIS specific entry still active' check (ActiveLogic's own
  `checkAttribute`, which re-tests the referenced entry, never merely
  'the attribute exists'). `idx` nil (the attribute was never written)
  is FALSE -- nothing to check, the same 'no active careplan' answer the
  :codes form gives when it finds no matching onset at all."
  [ctx idx]
  (boolean (and idx
                (not (some (fn [ev] (and (= :care-plan-end (:event ev)) (= idx (:references ev))))
                          (:trajectory ctx))))))

(defn- active-careplan-condition-holds?
  "GMF coverage Wave I2 (2026-08-04, ADR-0041 AR-2): :codes dispatches
  through the SAME onset/end log-query `active-onset-condition-holds?`
  already establishes for :active-condition/:active-medication
  (:care-plan-start/:care-plan-end, the SAME paired-span shape ADR-0029
  R2(b) built) -- confirmed the real vendored use, `depression_
  screening.json`'s own `Check Eligibility` At-Least guard, uses only
  this form. :referenced-by-attribute reads the attribute's own stored
  trajectory index (the SAME index-based indirection :medication-order/
  :medication-end's own :assign-to-attribute/:referenced-by-attribute
  pair already establishes) and checks THAT entry's own active status
  (`careplan-active-by-reference?`, above) -- installed, not yet used by
  any vendored candidate (no closure this session writes a careplan-
  index attribute for this form to read; proven by a hand-built ctx in
  this namespace's own tests, the same 'mechanism built, fixture-proven,
  target not-yet-vendored' shape stroke.json's own NamedDistribution
  mechanism already established, ADR-0028/ADR-0029 H3). Neither form
  present: FALSE, not upstream's own throw ('must be specified by code
  or attribute') -- 'is a careplan currently active' is this
  condition's own question, and a malformed condition map missing both
  is a module-authoring-shape concern this project's own evaluate-
  condition already has a dedicated unsupported-condition-type throw
  for, one layer up; a second, narrower throw here would be defensive
  code for a shape no real closure authors."
  [module-id ctx {:keys [codes referenced-by-attribute] :as condition}]
  (cond
    codes (active-onset-condition-holds? :care-plan-start :care-plan-end ctx condition)
    referenced-by-attribute
    (careplan-active-by-reference? ctx (get (:attributes ctx) (keyword (root-id ctx module-id) (gmf/slug referenced-by-attribute))))
    :else false))

;; Mutual recursion with evaluate-condition (And's own sub-conditions are
;; evaluated through the SAME dispatcher, below) -- forward-declared so
;; this namespace reads top-to-bottom without reordering evaluate-condition
;; ahead of the condition-type helpers that already precede it.
(declare evaluate-condition)

(defn- and-condition-holds?
  [module-id ctx {:keys [conditions]}]
  (every? #(evaluate-condition module-id ctx %) conditions))

(defn- or-condition-holds?
  "GMF coverage Wave A (2026-08-02): the boolean-disjunction mirror of
  :and, Synthea's own Logic.java Or class -- true iff ANY sub-condition
  holds. Same recursive shape, same zero-rng property."
  [module-id ctx {:keys [conditions]}]
  (boolean (some #(evaluate-condition module-id ctx %) conditions)))

(defn- at-least-condition-holds?
  "GMF coverage Wave A (2026-08-02): Synthea's own Logic.java AtLeast
  class -- true iff at least :minimum of :conditions evaluate true (the
  N-of-M generalization of :and (minimum = count) and :or (minimum = 1)).
  Real use: sore_throat.json's Determine_if_Bacterial (Step 3), a
  modified-Centor-criteria gate whose own sub-conditions are :symptom/
  :observation/:age -- confirmed by reading the vendored file directly,
  not inferred."
  [module-id ctx {:keys [minimum conditions]}]
  (>= (count (filter #(evaluate-condition module-id ctx %) conditions)) minimum))

(defn- not-condition-holds?
  "GMF coverage Wave F (2026-08-03, ADR-0036 AR-4): Synthea's own
  Logic.java Not class -- recursive negation of a single nested
  condition, the same dispatcher every other compound already goes
  through (`evaluate-condition`, below)."
  [module-id ctx {:keys [condition]}]
  (not (evaluate-condition module-id ctx condition)))

(defn- honest-absence
  "GMF coverage Wave F (2026-08-03, ADR-0036 AR-4): the exception
  `race-condition-holds?`/`socioeconomic-status-condition-holds?` throw
  when the persona carries no `field` at all -- a distinct marker
  (`::honest-absence` in ex-data) `walk-module`/`run-module`'s own loop
  catches AND ONLY THIS, converting it into a `:walk-error` RESULT
  (never propagating as an exception past the walk boundary, result-
  not-throw at the layer a caller actually observes) -- deliberately
  NOT the same disposition an unsupported condition type/vital-sign
  name/observation-precondition gap already get (a genuine, expected,
  worth-recording outcome, not a module-authoring-shape bug this
  interpreter refuses to run at all). Every OTHER throw in this
  namespace stays an uncaught, loud exception -- this catch is scoped to
  exactly this marker, never a blanket try/catch that would silently
  downgrade a real programmer-error throw into a soft result."
  [condition-type missing-field]
  (ex-info (str "ehrt.sim-trajectory.gmf-interpreter: " (name condition-type)
                " condition evaluated against a persona missing " (name missing-field))
           {::honest-absence true :condition-type condition-type :missing-field missing-field}))

(defn- race-condition-holds?
  "GMF coverage Wave F (2026-08-03, ADR-0036 AR-4): Synthea's own
  Logic.java Race class -- case-insensitive match (source-grounded:
  `race.equalsIgnoreCase(...)`) against the persona's own :race
  (`ehrt.sim-model.persona`'s own optional field, AR-4/AR-5). A persona
  carrying no :race at all is a WALK ERROR (`honest-absence`, above),
  never a silent false -- the honest-absence rule this ADR's own ruling
  states, distinguishing 'this persona was never configured with race
  data' from 'this persona's race genuinely does not match.'"
  [{:keys [race]} persona]
  (if (contains? persona :race)
    (.equalsIgnoreCase ^String race (:race persona))
    (throw (honest-absence :race :race))))

(defn- socioeconomic-status-condition-holds?
  "GMF coverage Wave F (2026-08-03, ADR-0036 AR-4): Synthea's own
  Logic.java SocioeconomicStatus class -- case-SENSITIVE equality
  (source-grounded: `category.equals(...)`, unlike Race's own
  case-insensitive match) against the persona's own
  :socioeconomic-category. Honest-absence, the same rule
  `race-condition-holds?` (above) already establishes."
  [{:keys [category]} persona]
  (if (contains? persona :socioeconomic-category)
    (= category (:socioeconomic-category persona))
    (throw (honest-absence :socioeconomic-status :socioeconomic-category))))

(defn- vital-sign-condition-holds?
  "GMF coverage Wave VS (2026-08-04, ADR-0039 AR-1/AR-4): Synthea's own
  Logic.java VitalSign class (`Utilities.compare(person.getVitalSign(...),
  value, operator)`, source-grounded) -- reads ctx's own `:vital-signs`
  register (a plain, GLOBAL lookup, never root-namespaced the way
  `:attribute`/`:symptom`'s workflow-scratch keys are). A recognized
  name with a stored value uses it; `initial-context`'s own baseline
  seeding (AR-3) already means every baseline-covered name IS a stored
  value from patient creation onward, so no separate baseline-table
  fallback is needed here -- only a name this session's own baseline
  table deliberately omits (`Left ventricular Ejection fraction`) can
  ever be genuinely unset, and THAT is `honest-absence` (ADR-0036 AR-4's
  own rule, extended here), never a silent false. `compare-op` (above)
  already matches every operator this wave's real candidate closures use
  (`<` `<=` `>=` `>`) -- the SAME dispatch Utilities.compare's own Double
  branch defines for them; `!=`/`is nil`/`is not nil` are unbuilt,
  installed ≠ used (no vendored candidate needs them, grows-by-evidence
  discipline)."
  [ctx {:keys [vital-sign operator value]}]
  (validate-vital-sign-name vital-sign)
  (let [k (vital-sign-key vital-sign)]
    (if (contains? (:vital-signs ctx) k)
      (compare-op operator (get (:vital-signs ctx) k) value)
      (throw (honest-absence :vital-sign k)))))

(defn evaluate-condition
  "The interpreter's own guard evaluator (docs/gmf-interpreter.md section 2:
  '(evaluate-condition condition patient-state (:ground-truth world)
  step)', instantiated here over `ctx`'s own persona/attributes/
  trajectory -- the M5a stand-in for `world`'s :ground-truth mirror).
  M5b adds :active-condition/:active-medication (log query by concept,
  architecturally the same shape :prior-state already establishes),
  :and (recursive compound), and :active-allergy (always false -- this
  namespace's own docstring note on why: no allergy concept exists
  anywhere in this project's Persona for a query to find). GMF coverage
  Wave A (2026-08-02, `.agents/plans/2026-08-02-gmf-coverage-plan.md`)
  adds :symptom (an emergent finding, not one of that session's own named
  candidates -- required for :at-least's only real vendored use,
  sore_throat.json's Determine_if_Bacterial, whose sub-conditions are
  Symptom/Observation/Age; see `symptom-condition-holds?`'s own
  docstring), :at-least/:or (compound wrappers, the same recursive shape
  :and already establishes), :date (a calendar-year comparison against
  `ctx`'s own `:t`, no new state needed), and :observation (a log query
  over already-emitted :observation trajectory events by concept, the
  same shape :active-condition/:active-medication already establish).
  GMF coverage Wave F (2026-08-03, ADR-0036 AR-4) adds :not (recursive
  negation, `not-condition-holds?`), :race, and :socioeconomic-status
  (persona demographic predicates, `race-condition-holds?`/
  `socioeconomic-status-condition-holds?`) -- the latter two THROW
  `honest-absence` (above) when the persona carries no matching field
  at all, a distinct marker `walk-module`/`run-module`'s own loop
  catches and converts into a `:walk-error` RESULT, never a silent
  false and never an escaping exception. GMF coverage Wave VS
  (2026-08-04, ADR-0039 AR-1/AR-4) adds :vital-sign (`vital-sign-
  condition-holds?`, above) -- reads ctx's own new `:vital-signs`
  register, honest-absence again when a name is genuinely unset (the
  ONE deliberately baseline-less name, `Left ventricular Ejection
  fraction`). GMF coverage Wave I2 (2026-08-04, ADR-0041 AR-2) adds
  :active-careplan (`active-careplan-condition-holds?`, above) -- the
  log-query family's third member, keyed on a careplan concept."
  [module-id ctx condition]
  (case (:condition-type condition)
    :age (age-condition-holds? condition (:persona ctx) (:t ctx))
    :gender (gender-condition-holds? condition (:persona ctx))
    :attribute (attribute-condition-holds? module-id ctx condition)
    :prior-state (prior-state-condition-holds? module-id ctx condition)
    :active-condition (active-onset-condition-holds? :condition-onset :condition-end ctx condition)
    :active-medication (active-onset-condition-holds? :medication-order :medication-end ctx condition)
    :active-careplan (active-careplan-condition-holds? module-id ctx condition)
    :active-allergy false
    :and (and-condition-holds? module-id ctx condition)
    :or (or-condition-holds? module-id ctx condition)
    :at-least (at-least-condition-holds? module-id ctx condition)
    :date (date-condition-holds? condition (:t ctx))
    :observation (observation-condition-holds? condition ctx)
    :symptom (symptom-condition-holds? module-id ctx condition)
    :not (not-condition-holds? module-id ctx condition)
    :race (race-condition-holds? condition (:persona ctx))
    :socioeconomic-status (socioeconomic-status-condition-holds? condition (:persona ctx))
    :vital-sign (vital-sign-condition-holds? ctx condition)
    (throw (ex-info "ehrt.sim-trajectory.gmf-interpreter: unsupported condition type"
                     {:condition-type (:condition-type condition)}))))

(defn- bare-age-jump-days
  "GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3e, H4): a
  FAILING bare `:age` condition with operator `>=` OR `>` resolves
  analytically -- the exact number of days until the persona's age
  reaches :quantity `:unit`s, a deterministic java.time computation, NO
  rng draw. `>`'s own jump target is `quantity + 1` years, not
  `quantity` -- the day-vs-year integer-age-flooring boundary a strict
  inequality needs: a floored age of exactly `quantity` does not
  satisfy `>`, so the exact day floored-age first becomes STRICTLY
  greater than `quantity` is the same `>=`-style boundary one year
  later. Any other operator (`<`/`<=`/`==`) returns nil -- age only
  ever increases, so a condition that is already true, or already
  permanently false, is never 'about to become true'; no sound forward
  jump exists (D3e's own account)."
  [{:keys [operator quantity unit]} persona ^long t]
  (when (and (#{">=" ">"} operator) (= unit "years"))
    (let [target-years (if (= operator ">") (inc (long quantity)) (long quantity))
          target-day (.toEpochDay (.plusYears (parse-dob persona) target-years))]
      (when (> target-day t) (- target-day t)))))

(defn- age-guard-jump-days
  "The v1 simplification this namespace's own docstring names: a FAILING
  `:age` condition resolves analytically via `bare-age-jump-days`
  (above) -- the exact virtual-clock advance needed, NO rng draw.

  GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3e, H4,
  sound-jump-or-escalate): a compound `:and` condition ALSO resolves,
  when -- and only when -- it contains EXACTLY ONE `:age` sub-condition
  (bare-resolvable per above) and every OTHER sibling is a condition
  type that does not itself read `ctx`'s own `:t` (i.e. not `:age`/
  `:date`, the only two v1 condition types whose truth value the mere
  passage of time can change) AND already holds, evaluated against the
  CURRENT ctx, before the jump. Soundness: the jump advances `:t` alone
  -- no attribute is cleared, no trajectory event is un-emitted -- so a
  non-`:age`/`:date` sibling true now stays true at the jump target
  (a pure function of persona/attributes/trajectory, none of which the
  jump touches), and the age sub-condition becomes true at the jump
  target by the SAME construction the bare case already proves;
  `guard-step`'s own 'no second, still-blocked branch' trust-by-
  construction (below) extends unchanged to this compound case, since
  this proof is what licenses it. Any OTHER form -- more than one Age
  sub-condition, an `:and` also containing `:date`, `:or`/`:at-least`
  wrapping an Age condition, or a sibling that does NOT already hold --
  is an ESCALATION with no sound bound, correctly returning nil (the
  walk blocks, unchanged); installed ≠ used (H1/H4): only the form
  `total_joint_replacement.json`'s own `Joint_Replacement_Guard`
  exercises (`{:and [Attribute is-not-nil, Age > 50 years]}`, ADR-0029's
  own D2/D3d finding) is built, the rest named, not guessed at."
  [module-id ctx condition]
  (let [persona (:persona ctx) t (:t ctx)]
    (case (:condition-type condition)
      :age (bare-age-jump-days condition persona t)
      :and (let [subs (:conditions condition)
                 age-subs (filter #(= :age (:condition-type %)) subs)
                 other-subs (remove #(= :age (:condition-type %)) subs)]
             (when (and (= 1 (count age-subs))
                        (every? #(not (#{:age :date} (:condition-type %))) other-subs)
                        (every? #(evaluate-condition module-id ctx %) other-subs))
               (bare-age-jump-days (first age-subs) persona t)))
      nil)))

;; --- Transition resolution (direct, distributed, conditional, complex) ----

(defn- resolve-distribution-value
  "GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3b, H3): a
  distributed_transition entry's own :distribution may be a plain
  number (v1, unchanged, passed through) or a NamedDistribution map
  (`{:attribute name :default n}`, Transition.java's own field names
  verbatim, D3b) -- real Synthea's own attribute-sourced weight, read
  from the SAME root-scoped, slugged key `attribute-condition-holds?`
  already resolves, falling back to the JSON-declared :default when the
  attribute is absent (stroke.json's own Chance_of_Stroke gate,
  ADR-0028 -- the mechanism landing does NOT unblock stroke itself,
  D3b's own disclosed note: stroke_risk stays SPECIFIED, unsourceable
  content). Zero rng: a pure lookup, the same property every other
  attribute-condition read already has."
  [module-id ctx d]
  (if (map? d)
    (let [k (keyword (root-id ctx module-id) (gmf/slug (:attribute d)))]
      (get (:attributes ctx) k (:default d)))
    d))

(defn- weighted-pick-transition
  "distributed_transition (and complex_transition's own nested
  distributions): a cumulative-weight pick over `entries`
  ({:transition :distribution}), consuming EXACTLY one `.nextDouble` --
  fixed consumption regardless of which member is chosen, the same law
  ehrt.sim-engine.engine/assign-pathway and
  ehrt.sim-engine.order-profiles/sample-analyte-value already establish."
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
  trajectory), never a stochastic choice of its own.

  GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3f finding,
  found vendoring `uti/ambulatory_path.json`'s own `risk-check` state):
  when NO entry's own condition holds (and none is condition-less), the
  LAST entry is returned UNCONDITIONALLY -- Transition.java's own
  `ConditionalTransition.follow`/`ComplexTransition.follow` both share
  this exact fallback ('if none of the conditions evaluated to true...
  the module will transition to the last transition defined'), a real
  semantic this function's own prior form never implemented (it simply
  returned nil, which `resolve-transition`'s own callers then crashed
  on -- no previously-vendored module's own mandatory path ever hit a
  branch where every condition failed with no explicit trailing
  else-arm, until this closure's real content did)."
  [module-id ctx entries]
  (or (first (filter (fn [{:keys [condition]}] (or (nil? condition) (evaluate-condition module-id ctx condition)))
                      entries))
      (last entries)))

(defn- type-of-care-weights
  "GMF coverage Wave B (2026-08-02, ADR-0027, D5): Synthea's own
  TypeOfCareTransition dispatch, characterized against Transition.java
  and the external telemedicine_config.json resource it reads at
  construction (docs/gmf-interpreter.md section 9's own D5 account,
  full source citations there). Real Synthea keys on BOTH the
  simulated calendar year (before/from telemedicine_config.json's own
  `start_year: 2020`) and the person's current insurance-payer name
  (`high_emergency_use_insurance_names`) -- this project's persona has
  no payer concept, the identical gap shape `:active-allergy`'s own
  documented simplification already establishes. Simplification:
  always the `typical_emergency_distribution` branch (never
  `high_emergency_distribution`), since no data exists to tell which
  synthetic patients would qualify; the year-gated half is NOT
  simplified away -- `ctx`'s own `:t`, the same mechanism `:date`
  condition already uses, answers it honestly. Weights cited verbatim
  from `telemedicine_config.json`'s own `typical_emergency_distribution`
  rows at both branches."
  [^long year]
  (if (< year 2020)
    {:ambulatory 0.75 :emergency 0.25}
    {:ambulatory 0.56 :emergency 0.2 :telemedicine 0.24}))

(defn- type-of-care-entries
  "Pairs each year-gated weight (`type-of-care-weights`) with the
  module's OWN declared target state for that key (`toc-targets`,
  `state`'s own `:type-of-care-transition` map) -- a key the weight
  table names but the module doesn't declare (pre-2020, a module that
  happens to still carry a `:telemedicine` target -- moot, since
  `type-of-care-weights` itself omits `:telemedicine` before 2020) is
  simply not offered."
  [toc-targets weights]
  (keep (fn [[k w]] (when-let [target (get toc-targets k)] {:transition target :distribution w})) weights))

(defn- lookup-column-value
  "GMF coverage Wave LC (2026-08-03, ADR-0038 AR-1(c)): resolves the
  CURRENT string value a lookup-table ATTRIBUTE column (`column`, the
  CSV's own raw header name -- never `age`/`time`, those are the two
  specials `lookup-table-row-matches?` handles separately, below) is
  compared against -- mirrors Transition.java's own `follow` loop body
  (`person.attributes.get(currentAttribute)`) plus this project's own
  persona-field extensions, MODULE-ATTRIBUTE-FIRST (AR-1(c)'s own
  ruling: a module-set column, e.g. `operative_status`, and a persona
  column, e.g. `race`, resolve from DIFFERENT stores -- the module
  store wins whenever both could apply, checked first, always; this is
  a real, disclosed divergence from upstream's own single flat
  attribute namespace, this project's own module-namespacing tradeoff).
  `gender` is this project's own PRE-EXISTING special (persona `:sex`,
  F/M-translated -- upstream never special-cases it at all, it is
  simply always a real Person attribute; this project's persona has no
  attribute-map entry for sex, only a top-level field, so a translation
  is needed here the same way it already was before this wave, now
  folded into this one dispatcher rather than hardcoded at the row-
  match call site). HONEST ABSENCE (`honest-absence`, above) when
  NEITHER store has the column -- upstream throws unconditionally here
  (`!person.attributes.containsKey(currentAttribute)`); this project
  returns the error as a value at the walk boundary, the SAME rule
  ADR-0036 AR-4 established for :race/:socioeconomic-status CONDITIONS,
  extended here to lookup-table ATTRIBUTE columns (the SAME
  `honest-absence` helper, reused verbatim -- `:condition-type
  :lookup-table-column`, `:missing-field` the slugged column name)."
  [module-id ctx column]
  (let [k (keyword (root-id ctx module-id) (gmf/slug column))
        persona (:persona ctx)]
    (cond
      (contains? (:attributes ctx) k) (get (:attributes ctx) k)
      (= column "gender") (case (:sex persona) :female "F" :male "M" (name (:sex persona)))
      (= column "race") (if (contains? persona :race) (:race persona)
                             (throw (honest-absence :lookup-table-column :race)))
      (= column "state") (if (contains? persona :state) (:state persona)
                              (throw (honest-absence :lookup-table-column :state)))
      (= column "socioeconomic_category")
      (if (contains? persona :socioeconomic-category) (:socioeconomic-category persona)
          (throw (honest-absence :lookup-table-column :socioeconomic-category)))
      :else (throw (honest-absence :lookup-table-column (keyword (gmf/slug column)))))))

(defn- lookup-table-row-matches?
  "GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3a, H2): does
  `row` (`ehrt.sim-trajectory.gmf/parse-lookup-table`'s own shape) match
  `ctx`'s own persona/attributes at virtual time `t`? Mirrors
  `LookupTableKey.equals`'s own age-range-contains-age rule (Synthea
  source, D3a) -- an ABSENT `:age-range`/`:time-range` on the row
  matches vacuously, the same 'no constraint on that axis' semantics
  Java's own key carries when one side supplies no range.

  GMF coverage Wave LC (2026-08-03, ADR-0038 AR-1(b)/AR-2): adds
  `:time-range` containment (the SAME inclusive-both-ends check
  `:age-range` already performs, `ehrt.sim-trajectory.gmf/parse-time-
  range`'s own epoch-day pair, compared against `ctx`'s own epoch-day
  `:t`) and generalizes EVERY remaining `:attributes` column (formerly
  only `gender`, hardcoded) through `lookup-column-value` (above) --
  CASE-SENSITIVE string equality against the row's own raw cell value,
  mirroring `LookupTableKey.equals`'s own `this.attributes.equals(that.
  attributes)` (`List<String>.equals`, plain `.equals()` per element --
  READ at the pin, not assumed: this is NOT the same case-INsensitive
  match `race-condition-holds?`'s own `:race` CONDITION type performs;
  a lookup-table `race` COLUMN is an ordinary attribute cell to
  upstream, sharing no special-case code with the `Race` Logic class at
  all)."
  [module-id ctx {:keys [age-range time-range attributes]}]
  (let [persona (:persona ctx) t (:t ctx)]
    (and (or (nil? age-range)
             (let [age (age-years-at persona t)] (<= (long (first age-range)) age (long (second age-range)))))
         (or (nil? time-range)
             (<= (long (first time-range)) t (long (second time-range))))
         (every? (fn [[column value]] (= value (lookup-column-value module-id ctx column))) attributes))))

(defn- lookup-table-weights
  "GMF coverage Wave D stage D3 (D3a, H2): the FIRST row (table order,
  deterministic) matching `ctx` (above), or nil (no match --
  `resolve-lookup-table-transition` falls back to each entry's own
  `:default-probability`, Java's own `defaultTransitions` mirror)."
  [module-id ctx table]
  (some #(when (lookup-table-row-matches? module-id ctx %) %) table))

(defn- resolve-lookup-table-transition
  "GMF coverage Wave D stage D3 (D3a, H2): the sixth transition kind.
  Zero-rng row lookup against `tables`' own resolved entry for this
  state's own declared `:lookup-table-name` (`ehrt.sim-trajectory.gmf/
  load-closure`'s own `:tables` return shape), then ONE `weighted-pick-
  transition` draw over each entry's own weight -- the matched row's
  `:weights` value for that entry's own `:transition` keyword when
  present, else the entry's own JSON-declared `:default-probability`
  (real Synthea's own `defaultTransitions` mirror, D3a) -- the SAME
  fixed-consumption weighted pick `:distributed-transition`/`:complex-
  transition`/`:type-of-care-transition` already share, joining this
  namespace's own descend-run-return order contract at the position
  every other transition-resolving draw already occupies. Zero rng in
  the row lookup itself (`lookup-table-weights`, above) -- a possible
  `honest-absence` throw during that lookup (GMF coverage Wave LC,
  ADR-0038 AR-1(c)) happens BEFORE the one draw, so a walk-error never
  leaves a partial/inconsistent rng-consumption count."
  [module-id ^Random rng ctx tables entries]
  (let [table (get tables (:lookup-table-name (first entries)))
        row (lookup-table-weights module-id ctx table)]
    (weighted-pick-transition
     rng (mapv (fn [{:keys [transition default-probability]}]
                 {:transition transition
                  :distribution (get (:weights row) transition default-probability)})
               entries))))

(defn- resolve-transition
  "The shared 6-kind transition dispatcher every non-Terminal v1 state
  type resolves its own :next through -- one mechanism, reused by every
  state type's own `step` handling below, rather than duplicated per
  type. GMF coverage Wave B (D5) adds `:type-of-care-transition` as a
  fifth kind, the same `weighted-pick-transition` mechanism
  `:distributed-transition`/`:complex-transition` already share (one
  `.nextDouble` draw, fixed consumption regardless of which member is
  chosen) -- a zero-rng weight lookup (`type-of-care-weights`, a pure
  function of `ctx`'s own `:t`) followed by the SAME one-draw pick,
  joining this namespace's own descend-run-return order contract at
  the position every other transition-resolving draw already
  occupies. GMF coverage Wave D stage D3 (D3a, H2) adds `:lookup-table-
  transition` as a sixth kind (`resolve-lookup-table-transition`,
  above) -- the only kind that consults `tables` (`ehrt.sim-trajectory.
  gmf/load-closure`'s own `:tables` return shape, threaded through the
  SAME way `modules` already is for `:call-submodule`). GMF coverage
  Wave LC (2026-08-03, ADR-0038 AR-1) generalizes the row-matching this
  kind performs from a two-column (`age`/`gender`) whitelist to ANY
  attribute column (`lookup-table-row-matches?`/`lookup-column-value`,
  above) -- a purely internal change to HOW a row is chosen, this
  dispatcher's own call shape unchanged except threading `module-id`
  through (module-namespaced attribute resolution needs it, the same
  way `resolve-distribution-value`/`attribute-condition-holds?` already
  do). D3b (H3):
  `:distributed-transition`'s own entries resolve each :distribution
  through `resolve-distribution-value` BEFORE the pick -- a plain
  number passes through unchanged, a NamedDistribution map resolves to
  an attribute-sourced weight.

  GMF coverage Wave I (2026-08-04, ADR-0040 AR-1): `:complex-transition`'s
  own nested :distributions now gain the SAME resolution -- Transition.
  java's own ComplexTransitionOption shares one `List<NamedDistribution>
  distributions` field type with DistributedTransition (source-
  confirmed), so this was always the identical mechanism, never a
  distinct one H1 correctly deferred until a real candidate needed it
  (injuries.json's own Elderly_Incidence_Rates, this session's own
  found gap)."
  [module-id ctx ^Random rng state tables]
  (cond
    (:direct-transition state) (:direct-transition state)
    (:distributed-transition state)
    (weighted-pick-transition
     rng (mapv #(update % :distribution (partial resolve-distribution-value module-id ctx)) (:distributed-transition state)))
    (:conditional-transition state) (:transition (first-matching-entry module-id ctx (:conditional-transition state)))
    ;; GMF coverage Wave D stage D3 (D3f finding, found vendoring
    ;; uti/ambulatory_path.json): a matched complex_transition entry is
    ;; EITHER a direct :transition OR a weighted :distributions list,
    ;; never both required -- Transition.java's own ComplexTransition.
    ;; follow mirrored exactly (`option.transition != null ? ... :
    ;; option.distributions`).
    (:complex-transition state)
    (let [entry (first-matching-entry module-id ctx (:complex-transition state))]
      (if (:transition entry)
        (:transition entry)
        (weighted-pick-transition
         rng (mapv #(update % :distribution (partial resolve-distribution-value module-id ctx)) (:distributions entry)))))
    (:type-of-care-transition state)
    (weighted-pick-transition rng (type-of-care-entries (:type-of-care-transition state)
                                                          (type-of-care-weights (.getYear (LocalDate/ofEpochDay (:t ctx))))))
    (:lookup-table-transition state)
    (resolve-lookup-table-transition module-id rng ctx tables (:lookup-table-transition state))
    :else nil))

;; --- step --------------------------------------------------------------

(defn- pass-through-outcome
  [module-id ctx rng state advance events tables]
  {:events events
   :attributes (:attributes ctx)
   :vital-signs (:vital-signs ctx)
   :suppressed-encounter-ends (:suppressed-encounter-ends ctx)
   :advance advance
   :next (resolve-transition module-id ctx rng state tables)
   :terminal? false
   :blocked? false})

(defn- blocked-outcome
  [ctx]
  {:events [] :attributes (:attributes ctx) :vital-signs (:vital-signs ctx)
   :suppressed-encounter-ends (:suppressed-encounter-ends ctx)
   :advance 0 :next nil :terminal? false :blocked? true})

(defn- guard-step
  [module-id ctx ^Random rng state tables]
  (let [condition (:allow state)]
    (if (evaluate-condition module-id ctx condition)
      (pass-through-outcome module-id ctx rng state 0 [] tables)
      (if-let [jump (age-guard-jump-days module-id ctx condition)]
        ;; `age-guard-jump-days` computes the EXACT day the condition
        ;; starts holding -- re-evaluating after the jump would always
        ;; pass by construction, so there is no second, still-blocked
        ;; branch to handle here (a scenario that can't happen gets no
        ;; defensive code for it, this project's own convention).
        (let [ctx' (update ctx :t + jump)]
          (update (pass-through-outcome module-id ctx' rng state 0 [] tables) :advance + jump))
        (blocked-outcome ctx)))))

(defn- trajectory-event
  "GMF coverage Wave B (2026-08-02, ADR-0027, D2): `:call-path` (root-
  first, the currently-executing module last) is added ONLY while
  `ctx`'s own `:call-stack` is active (length > 1 -- root plus at least
  one callee) -- absent entirely for a non-calling walk, so this
  project's pre-Wave-B trajectory shape (and its own regression oracle)
  stays byte-identical (D2's own 'representation may stay backward-
  compatible' latitude for the one-element-path case)."
  [module-id ctx event-type extra]
  (let [call-stack (:call-stack ctx)]
    (cond-> (merge {:module module-id :state (:current ctx) :t (:t ctx) :event event-type} extra)
      (> (count call-stack) 1) (assoc :call-path call-stack))))

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

(defn- open-encounter-index
  "EncounterEnd fix (2026-08-08, ADR-0082): the trajectory index of the
  currently-open `:encounter`, walk-level -- NOT module-scoped, RETIRING
  `index-of-last-open-encounter`'s own openness-blind, module-filtered
  guess (ADR-0081's own Context section has the full diagnosis). One
  in-flight encounter is always enough for this project's own GMF
  subset (Wave H's own `mark-phase` fold, above, already proves it by
  construction: 'encounters never nest'), and module-agnostic matches
  how `:references` is already CONSUMED downstream -- a plain trajectory
  position (`ehrt.sim-trajectory.compile-trajectory`'s own
  `referenced-event`), never re-checked against the citing event's own
  module, so a CallSubmodule callee's own EncounterEnd correctly closes
  an encounter its caller opened (or vice versa), exactly the cross-
  module case the retired function's module-id filter silently
  mishandled. A pure fold over `trajectory` -- no ctx state to keep in
  sync (`initial-context`'s own dated note, above, has the full
  rationale): `:encounter` opens (sets the index), any later
  `:encounter-end` closes it (clears to nil), everything else passes
  the running value through unchanged."
  [trajectory]
  (reduce (fn [open [i event]]
            (case (:event event)
              :encounter i
              :encounter-end nil
              open))
          nil
          (map-indexed vector trajectory)))

(defn- emit-and-advance
  "Every v1 trajectory-event-producing state type shares this shape: cite
  `{:module :state :t}` (glass-box law), carry `extra` (typically :codes,
  verbatim -- code passthrough law) into the event, append it to the
  accumulating trajectory, then resolve the ORDINARY transition
  (optionally after its own sampled `:duration`, Procedure's own case).

  FIXED (2026-08-03, notes/ADRs.md ADR-0032 AR-2, D3c finding 1): a
  Procedure's own `:duration` is the FLAT `{:low :high :unit}` shape
  (the loader's own Range schema, not a nested `{:range {...}}`/
  `{:exact {...}}` wrapper) -- `resolve-time-advance` destructures
  `:range`/`:exact` KEYS, so passing `duration` straight through always
  missed both, silently never advancing time for ANY Procedure. Wrapped
  here, at the call site, as `{:range duration}` -- `resolve-time-
  advance`'s own contract, Delay's nested shape, and the loader schema
  all stay unchanged (AR-2's own ruling: the flat map IS Procedure's
  canonical shape, upstream GMF 1.0's own encoding; the mismatch was
  never a shape this function's argument needed translating, only a
  wrapper this call site was missing).

  ADR-0035 AR-2/AR-3: a GAUSSIAN/EXPONENTIAL/TRIANGULAR Procedure
  duration normalizes to a state-level `:distribution` instead of
  `:duration` (`ehrt.sim-trajectory.gmf`'s own `apply-new-timing-
  distribution` -- no Range/Exact collapse exists for these three
  kinds), so this call site checks BOTH: `:duration` first (the v1/
  UNIFORM/EXACT path, untouched), `:distribution` second (wrapped as
  `{:distribution duration}`, the SAME 'wrap at the call site' shape
  AR-2 above already established for `:duration`) -- GATED on `(= :procedure
  (:type state))`, since `emit-and-advance` is the shared helper EVERY
  trajectory-event-producing state type calls, and this project's own
  loader leaves an Observation state's own v2 :distribution field
  (a pre-existing, out-of-scope encoding this session's own fence does
  not cover) completely unnormalized -- found live, full-catalog UTI
  closure sweep (`uti/ed_bundle.json`'s own O2-saturation Observation
  states): an ungated check here would hand that RAW, still-string-
  keyed map to `sample-distribution`, crashing. Only `:procedure`
  states ever carry a real, normalized timing `:distribution`."
  [module-id ctx ^Random rng state event-type extra tables]
  (let [event (trajectory-event module-id ctx event-type extra)
        ctx' (update ctx :trajectory conj event)
        advance (cond
                  (:duration state) (- (resolve-time-advance rng (:t ctx) {:range (:duration state)}) (:t ctx))
                  (and (= :procedure (:type state)) (:distribution state))
                  (- (resolve-time-advance rng (:t ctx) {:distribution (:distribution state)}) (:t ctx))
                  :else 0)]
    (pass-through-outcome module-id ctx' rng state advance [event] tables)))

(defn- round1 [^double v] (/ (Math/round (* v 10.0)) 10.0))

;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029, D1a-4, D1a schema
;; RULING Q2+Q3): the observation-family value-sourcing reader --
;; `vital-sign-reference-table`/`validate-vital-sign-name` now live near
;; `initial-context` (2026-08-04, ADR-0039's own relocation note there).
(defn- vital-sign-extra
  "One uniform draw within the named vital-sign's own :reference-range
  (the SAME plain-range mechanism the pre-existing `range` branch
  already uses, P4's own 'documented approximation of a real continuous
  physiology model this project does not have') -- because the value is
  drawn FROM :reference-range by construction, the abnormal-flag this
  same range also supports (Q2+Q3's own 'supplies the OBX reference-
  range/abnormal-flag inputs' ruling) is always :normal, an honest
  computed consequence of the simplification, never a fabricated
  excursion. An unrecognized name is a real, visible rejection
  (:unrecognized-vital-sign, `validate-vital-sign-name` above) -- this
  table's own header comment's 'grows by evidence, not speculation'
  rule -- never a silent nil."
  [^Random rng codes vital-sign-name]
  (validate-vital-sign-name vital-sign-name)
  (let [{:keys [reference-range units]} (get vital-sign-reference-table vital-sign-name)
        {:keys [low high]} reference-range]
    {:codes codes :value (round1 (rand-double-in rng low high)) :unit units
     :reference-range reference-range :interpretation :normal}))

(defn- sample-observation-extra
  "The value-sourcing mechanisms D1a-3/D3d found side by side in one
  closure: `range` (legacy, M5a), `value_code` (a coded/qualitative
  finding, verbatim -- code passthrough law), `vital_sign` (`vital-
  sign-extra`, above), and `exact` (D3d finding 2, GMF coverage Wave D
  stage D3, ADR-0029 -- a literal, SPECIFIED value, TJR's own
  `PROMIS29_Total_Assessment`, zero rng, mirroring `Delay`'s own
  `:exact` handling). Neither present -> codes only, unchanged M5a
  behavior. `:category` (Q1's own ruling: added now) rides along
  whenever the state carries one, independent of which value mechanism
  (or none) fired -- reused identically whether `state` is a full
  top-level GmfState or one bare ObservationChild map (`gmf/
  ObservationChild`'s own shape), since both carry exactly the same
  field names."
  [^Random rng state]
  (let [codes (:codes state)
        base (cond
               (:range state)
               (let [{:keys [low high]} (:range state)]
                 {:codes codes :value (round1 (rand-double-in rng low high)) :unit (:unit state)})

               (:exact state)
               {:codes codes :value (:quantity (:exact state)) :unit (:unit state)}

               (:value-code state)
               {:codes codes :value-code (:value-code state)}

               (:vital-sign state)
               (vital-sign-extra rng codes (:vital-sign state))

               :else {:codes codes})]
    (cond-> base (:category state) (assoc :category (:category state)))))

(defn- diagnostic-report-extra
  "R2(a)/P5: ONE trajectory event for the whole state, carrying the
  report-level :codes (when present, D1a-2: optional) and the full
  :observations vector -- each child sampled the SAME way a standalone
  Observation state is (`sample-observation-extra`, reused verbatim,
  never a parallel child-sampling implementation), in the state's own
  vector order (this namespace's own docstring RNG order-contract
  note)."
  [^Random rng state]
  (cond-> {:observations (mapv (partial sample-observation-extra rng) (:observations state))}
    (:codes state) (assoc :codes (:codes state))))

;; --- GMF coverage Wave F (2026-08-03, ADR-0036 AR-2): ImagingStudy --------
;; State.java's own `process`/`duplicateSeries`/`duplicateInstances`
;; (source-grounded, pin 7e08387c68a7f0e21d13076609a159fd473fc902):
;; series count is drawn ONCE for the whole study when :min-number-series/
;; :max-number-series are both present (a single `rand-int-in`, equivalent
;; to upstream's own `(int) rand(min, max+1)`); each MATERIALIZED series
;; then draws its own instance count independently, when THAT series (the
;; authored one, or the shared reference series every materialized copy
;; clones) carries :min-number-instances/:max-number-instances -- one
;; draw per materialized series, upstream's own `duplicateInstances` loop
;; verbatim. Neither bound present on a given series/study level means
;; zero draws at that level (the authored :series/:instances vector is
;; used as-is) -- consumption is fully deterministic GIVEN a module's own
;; authored bounds, the same branching-consumption family distributed
;; transitions already establish (a module-authoring-time-fixed branch,
;; never a runtime-outcome-dependent one).

(defn- imaging-series-instance-count
  [^Random rng {:keys [instances min-number-instances max-number-instances]}]
  (if (and min-number-instances max-number-instances
           (>= max-number-instances min-number-instances) (seq instances))
    (rand-int-in rng min-number-instances max-number-instances)
    (count instances)))

(defn- imaging-study-extra
  "One trajectory event's own :codes/:modality/:series (glass-box: the
  drawn counts, never full per-instance content -- AR-2's own ruling).
  `:codes` wraps the single :procedure-code Concept in a vector, the
  SAME shape a standalone Procedure's own :codes already is, so
  `compile-trajectory`'s own `procedure->step` (unchanged) compiles this
  event exactly as it would a Procedure's."
  [^Random rng state]
  (let [{:keys [series min-number-series max-number-series procedure-code]} state
        materialized (if (and min-number-series max-number-series
                              (>= max-number-series min-number-series) (seq series))
                       (repeat (rand-int-in rng min-number-series max-number-series) (first series))
                       series)
        series-out (mapv (fn [s] {:modality (:modality s)
                                  :instance-count (imaging-series-instance-count rng s)})
                         materialized)]
    {:codes [procedure-code]
     :modality (:modality (first series-out))
     :series series-out}))

;; --- GMF coverage Wave B (2026-08-02, ADR-0027, D1-D4): CallSubmodule
;; call/return -- descend-run-return, ns docstring's own order contract --

(def ^:private max-steps
  "A runaway-loop backstop, not a design limit: a real v1 module always
  terminates or blocks in far fewer ZERO-TIME-ADVANCE steps than this.
  Exceeding it means a module authoring bug (a zero-time-advance
  transition cycle), a programmer error this project's own conventions
  reserve exceptions for -- never a result-not-throw outcome, since no
  legitimate module should ever reach it.

  ADR-0105 fix (2026-08-11): the budget counts ONLY steps whose own
  `:advance` is zero -- a step that genuinely moves the module clock
  forward does not consume it. Before this fix, EVERY step counted
  regardless of advance, so a LEGAL time-advancing loop (e.g. a 1-7-day
  Delay paired with a zero-advance re-check, `injuries/broken_jaw.
  json`'s own Dental Referral shape, ADR-0070's own bail-out finding)
  could trip this backstop purely by iterating enough times within a
  long horizon, with no bug present -- `max-steps`'s own name and this
  docstring's own prior wording already promised 'a zero-time-advance
  transition cycle' as the target; the implementation now matches that
  promise instead of merely aspiring to it."
  10000)

(defn- consume-step-budget
  "ADR-0105: `n`, the runaway-loop step counter every `max-steps`-
  policed loop in this namespace threads through its own `recur` --
  incremented only when `outcome`'s own `:advance` is zero (this is the
  budget `max-steps` polices, per that var's own docstring), left
  unchanged on any step that genuinely moves the module clock forward.
  One shared rule, three call sites (`walk-module`/`run-submodule`/
  `run-module`) -- see `max-steps`'s own docstring for the arithmetic
  this fixes."
  ^long [^long n outcome]
  (if (zero? (long (:advance outcome))) (inc n) n))

(def ^:private max-call-depth
  "D3's own defensive runtime call-depth invariant -- a real v1 closure
  never nests anywhere close to this deep; exceeding it is a bug signal
  (e.g. a static-acyclicity gap `ehrt.sim-trajectory.gmf/load-closure`'s
  own D3 check should have caught at LOAD time), never a legitimate
  result this interpreter silently absorbs at RUN time."
  100)

;; Mutual recursion with `step` (a CallSubmodule state's own handling
;; calls `run-submodule`, which drives the callee via `step` itself,
;; the SAME function -- one interpreter, not two) -- forward-declared so
;; this namespace reads top-to-bottom, the same shape `evaluate-
;; condition`/`and-condition-holds?` already establish above.
(declare step)

(defn- run-submodule
  "Drives `callee-module` from ITS OWN `:initial` state to Terminal,
  sharing `rng`/`:t`/`:attributes`/`:trajectory` with the CALLER's own
  `ctx` (D1: one clock, one rng stream, descend-run-return) -- `call-
  stack` is the FULL root-first path INCLUDING the callee itself (D2),
  threaded into the callee's own nested ctx so every event it emits
  carries `:call-path` (`trajectory-event`, above). A called submodule
  that itself BLOCKS on a Guard is a disclosed, out-of-scope limitation
  this session -- throws, loudly, rather than silently mishandling a
  resume-across-a-call mechanism this interpreter does not have yet
  (ns docstring's own note; neither vendored Wave B closure exercises
  this path).

  ADR-0105 fix (2026-08-11): `horizon-end-t` (optional, an epoch-day)
  is now threaded in from `run-module`'s own call chain and re-checked
  before EVERY step, mirroring `run-module`'s own top-of-loop check
  exactly (`run-module`'s own docstring) -- previously this loop never
  received it at all (horizon-BLIND, ADR-0070's own bail-out finding: a
  time-advancing Delay loop inside a submodule iterated past the
  horizon forever, tripping `max-steps` at ANY horizon). Crossing the
  horizon here ends the WHOLE WALK in the SAME `:status :horizon-
  complete` truncation the top-level Delay-overshoot path uses --
  'parking past the horizon ends the walk in the same status Delay
  uses' (`wellness-wait-step`'s own docstring, the mirror-site
  contract this fix keeps) -- never a silent per-submodule status the
  caller would have to notice and translate; `call-submodule-step`
  (below) propagates it straight up to `run-module`'s own loop."
  [modules tables ^Random rng ctx callee-module call-stack horizon-end-t]
  (when (> (count call-stack) max-call-depth)
    (throw (ex-info "ehrt.sim-trajectory.gmf-interpreter: CallSubmodule call depth exceeded max-call-depth -- likely a bug (a static-acyclicity gap gmf/load-closure's own D3 check should have caught)"
                     {:call-stack call-stack})))
  (loop [callee-ctx (assoc ctx :current :initial :call-stack call-stack) n 0]
    (if (and horizon-end-t (>= (:t callee-ctx) horizon-end-t))
      (assoc callee-ctx :status :horizon-complete)
      (do
        (when (>= n max-steps)
          (throw (ex-info "ehrt.sim-trajectory.gmf-interpreter: run-submodule exceeded max-steps -- likely a module authoring bug (a zero-time-advance transition cycle)"
                           {:call-stack call-stack :current (:current callee-ctx)})))
        (let [outcome (step callee-module rng callee-ctx modules tables horizon-end-t)
              ctx' (-> callee-ctx
                       (assoc :attributes (:attributes outcome))
                       (assoc :vital-signs (:vital-signs outcome))
                       (assoc :suppressed-encounter-ends (:suppressed-encounter-ends outcome))
                       (update :trajectory into (:events outcome))
                       (update :t + (:advance outcome)))]
          (cond
            (:horizon-complete? outcome) (assoc ctx' :status :horizon-complete)
            (:terminal? outcome) (assoc ctx' :status :terminal)
            (:blocked? outcome)
            (throw (ex-info "ehrt.sim-trajectory.gmf-interpreter: a called submodule blocked on a Guard -- no resume-across-a-call mechanism this session (Wave B's own disclosed scope limitation, ADR-0027)"
                             {:call-stack call-stack :current (:current ctx')}))
            :else (recur (assoc ctx' :current (:next outcome)) (consume-step-budget n outcome))))))))

(defn- call-submodule-step
  "The CallSubmodule state's own `step` handling: looks up `modules`
  (the resolved closure, `ehrt.sim-trajectory.gmf/load-closure`'s own
  return shape) for the call-path `state`'s own `:submodule` names,
  descends via `run-submodule`, then resolves the CALLER's OWN
  transition using the POST-CALL ctx (attributes as the callee left
  them -- exactly what lets `ear_infections.json`'s own `End_Ear_
  Infection_Medications`, reached AFTER the call returns, see what the
  callee wrote). A `nil` lookup is a loader/interpreter mismatch (`gmf/
  load-closure`'s own all-or-nothing gate should already have rejected
  a closure with a missing call-path) -- a programmer-error throw, not
  a result-not-throw outcome, the same disposition `evaluate-condition`
  already establishes for a genuinely unsupported condition type.

  ADR-0105 fix (2026-08-11): `horizon-end-t` threads straight through
  to `run-submodule` (consulted only there). When the callee's own walk
  parks on the horizon (`run-submodule`'s own `:status :horizon-
  complete`), the caller's own transition is NEVER resolved -- the
  events the callee emitted before parking still land (`new-events`,
  unchanged), but this outcome carries `:horizon-complete? true` and
  `:next nil` instead, which `run-module`'s own loop (the only caller
  that ever supplies a non-nil `horizon-end-t`) recognizes and ends the
  WHOLE walk on, the same `:status :horizon-complete` a top-level Delay
  overshoot produces -- never resolving `state`'s own post-call
  transition against a ctx whose clock has already run past the
  horizon."
  [modules tables module-id ctx ^Random rng state horizon-end-t]
  (let [callee-id (:submodule state)
        callee-module (get modules callee-id)]
    (when (nil? callee-module)
      (throw (ex-info "ehrt.sim-trajectory.gmf-interpreter: CallSubmodule names a call-path missing from the resolved closure -- loader/interpreter mismatch (gmf/load-closure should have caught this at load time)"
                       {:call-path callee-id :caller module-id})))
    (let [call-stack (conj (or (:call-stack ctx) [(or (:root ctx) module-id)]) callee-id)
          pre-call-trajectory-count (count (:trajectory ctx))
          result (run-submodule modules tables rng ctx callee-module call-stack horizon-end-t)
          new-events (vec (drop pre-call-trajectory-count (:trajectory result)))
          post-call-ctx (assoc result :call-stack (:call-stack ctx))
          base {:events new-events
                :attributes (:attributes post-call-ctx)
                :vital-signs (:vital-signs post-call-ctx)
                :suppressed-encounter-ends (:suppressed-encounter-ends post-call-ctx)
                :advance (- (:t post-call-ctx) (:t ctx))
                :terminal? false
                :blocked? false}]
      (if (= :horizon-complete (:status result))
        (assoc base :next nil :horizon-complete? true)
        (assoc base :next (resolve-transition module-id post-call-ctx rng state tables))))))

(defn- wellness-wait-step
  "GMF coverage Wave G (2026-08-03, ADR-0037 AR-3): advances the module
  clock to `next-wellness-tick` (a PURE function of persona+t, zero rng
  draws -- AR-2), opens a wellness `:outpatient-visit`-family `:encounter`
  event AT the tick -- `death-step`'s own 'the event cites the COMPUTED
  time, never entry time' precedent is the shape this borrows -- attaches
  `state`'s own `:reason` when present (a NEW thread: unlike every other
  Encounter-shaped state, whose own `:reason` field is validation-only
  dead weight, `gmf.clj`'s own D2 disclosure), then resolves the
  ORDINARY transition via `pass-through-outcome`, the same helper
  `emit-and-advance` already reuses for every other event-then-advance
  state.

  Bounded by `horizon-end-t` exactly as Delay is -- NOT by anything in
  THIS function, which never receives it: `run-module`'s own loop
  already re-checks `:t` against `horizon-end-t` before every `step`
  call, the SAME mechanism that already lets a Delay's own advance
  overshoot the horizon in one step and get caught on the NEXT loop
  iteration ('parking past the horizon ends the walk in the same status
  Delay uses'). This is also the loop-bounding mechanism AR-7's own
  four upstream loop modules rely on: each `:wellness-wait` -> ... ->
  Guard -> `:wellness-wait` cycle now advances a FULL cadence interval
  per iteration (never zero, unlike the retired create-now
  substitution), so the horizon bounds iterations the same way it
  already bounds every other module's own walk.

  EncounterEnd fix (2026-08-08, ADR-0082, R1): this is the OTHER site
  that mints a raw `:encounter` event (the `:encounter` case in `step`'s
  own dispatch, below, is the other) -- carries the SAME nesting assert
  `open-encounter-index` gates there, since a wellness encounter closes
  by openness alone, no separate wellness-vs-ordinary arm (R1's own
  openness-only ruling)."
  [module-id ctx ^Random rng state tables]
  (assert (nil? (open-encounter-index (:trajectory ctx)))
          "ehrt.sim-trajectory.gmf-interpreter: nested :encounter (wellness) -- this project's GMF subset assumes encounters never nest (Wave H's own fold discipline, ADR-0042)")
  (let [t' (next-wellness-tick (:persona ctx) (:t ctx))
        event (trajectory-event module-id (assoc ctx :t t') :encounter
                                 (cond-> {:encounter-class :wellness}
                                   (:codes state) (assoc :codes (:codes state))
                                   (:reason state) (assoc :reason (:reason state))))
        ctx' (update ctx :trajectory conj event)]
    (pass-through-outcome module-id ctx' rng state (- t' (:t ctx)) [event] tables)))

(defn- death-cause-codes
  "ADR-0041 AR-1: `State.java`'s own `Death.process` (source re-read
  fresh this session, at the pin) resolves cause-of-death through an
  ordered if/else-if chain -- :codes FIRST, then :condition-onset, then
  :referenced-by-attribute. This CORRECTS docs/gmf-interpreter.md
  section 10's own C1 account, which paraphrased the chain in the
  OPPOSITE order (condition-onset, referenced-by-attribute, codes) --
  a dated resolution note lands on section 10 itself. No vendored Death
  state this project has ever combines more than one form
  (`stroke.json`'s own Death: :codes alone; `congestive_heart_
  failure.json`'s own four Death states: :referenced-by-attribute
  alone), so this ordering is proven by the fixture tests below, not by
  any real closure's own co-presence.

  :condition-onset -- a citation query over ctx's own trajectory for the
  named ConditionOnset event (`index-of-citation`, the SAME shape
  ConditionEnd's own :condition-onset field already resolves), that
  event's own :codes when found. Absent (the named state never onset on
  this walk) resolves to nil, NOT upstream's own second fallback
  (`person.hadPriorState` false -> read the named state's own JSON-
  declared codes directly off the module, regardless of whether it ever
  fired) -- a disclosed, NOT-ported simplification: no vendored module
  needs it, and this project's own trajectory-query idiom (PriorState/
  Active Condition/Active Medication) has no existing 'read a state's
  own declared content without having walked it' mechanism to reuse.

  :referenced-by-attribute -- the SAME index-based indirection
  :medication-order/:medication-end's own :assign-to-attribute/
  :referenced-by-attribute pair already establishes (this session ALSO
  ports that mechanism onto :condition-onset, `step`'s own
  :condition-onset case, above), read back the identical way
  :medication-end already does: the attribute's own stored trajectory
  index, that event's own :codes. Absent (the attribute was never
  written) resolves to nil -- a disclosed departure from upstream's own
  behavior (a RuntimeException, 'referenced but not set') -- chosen
  because this project's own SetAttribute/assign-to-attribute family
  already treats a not-yet-written attribute as an honest, non-fatal
  absence everywhere else it reads one (`:value-attribute`'s own
  containsKey guard, ADR-0040 AR-2), and Death's own cause is
  supplementary content (feeds a terminal event's own :codes field,
  never gates the walk's own progress the way a Guard's condition
  does)."
  [module-id ctx {:keys [codes condition-onset referenced-by-attribute]}]
  (cond
    codes codes
    condition-onset
    (when-let [idx (index-of-citation (:trajectory ctx) module-id :condition-onset condition-onset)]
      (:codes (nth (:trajectory ctx) idx)))
    referenced-by-attribute
    (when-let [idx (get (:attributes ctx) (keyword (root-id ctx module-id) (gmf/slug referenced-by-attribute)))]
      (:codes (nth (:trajectory ctx) idx)))
    :else nil))

(defn- death-step
  "GMF coverage Wave C (2026-08-02, ADR-0028, C1/C2): `state`'s own
  :range/:exact resolve the SAME way `resolve-time-advance` already
  resolves a `:delay`'s -- Death's own real Synthea fields (`State.
  java`'s `range`/`exact`, docs/gmf-interpreter.md section 10) are
  literally the same `{low high unit}`/`{quantity unit}` shape. The
  emitted event cites the COMPUTED death time (`death-t`, not the
  state's own entry time -- a `:range` death is genuinely delayed); the
  outcome is `:terminal? true`/`:next nil` -- C2's own terminal
  contract, Death's own declared transition is never resolved.

  ADR-0041 AR-1: all THREE of State.java's own cause-of-death forms now
  resolve (`death-cause-codes`, above) -- `:condition-onset`/
  `:referenced-by-attribute` no longer throw (Wave C's own disclosed
  UNBUILT limitation, retired here: `congestive_heart_failure.json`'s
  own four Death states all use `:referenced-by-attribute`)."
  [module-id ctx ^Random rng state]
  (let [death-t (resolve-time-advance rng (:t ctx) state)
        event (trajectory-event module-id (assoc ctx :t death-t) :death {:codes (death-cause-codes module-id ctx state)})]
    {:events [event] :attributes (:attributes ctx) :vital-signs (:vital-signs ctx)
     :suppressed-encounter-ends (:suppressed-encounter-ends ctx)
     :advance (- death-t (:t ctx))
     :next nil :terminal? true :blocked? false}))

(defn step
  "Advances ONE state from `ctx`'s own `:current` -- consuming `rng` only
  in this function's own documented order (per-state-type below). Returns
  {:events [...] :attributes {...} :advance seconds :next state-or-nil
  :terminal? bool :blocked? bool}; NEVER mutates `ctx` -- the caller
  (`walk-module`, below) is what folds an outcome back into a new ctx,
  the same decide/evolve-style separation ehrt.sim-engine.engine already
  establishes (sim/ADR-0008), scaled down to this interpreter's own single-
  function `step`, since a GMF state's own effect and its own transition
  are never independently interesting the way decide/evolve's
  cross-patient split is.

  GMF coverage Wave B (2026-08-02, ADR-0027, D1/D3): the optional 4th
  argument, `modules` (`ehrt.sim-trajectory.gmf/load-closure`'s own
  return shape, call-path -> loaded module), is consulted ONLY by the
  `:call-submodule` case (`call-submodule-step`, above) -- every other
  case is unaffected, and every pre-Wave-B 3-argument call site (this
  namespace's own `walk-module`/`run-module`, and every test that calls
  `step` directly) keeps working unchanged: a single module IS its own
  one-module closure.

  GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3a, H2): the
  optional 5th argument, `tables` (`ehrt.sim-trajectory.gmf/load-
  closure`'s own `:tables` return shape), is a SEPARATE, parallel
  closure-wide argument alongside `modules` (not folded into it --
  `modules` keeps its own existing documented shape unchanged) --
  consulted ONLY by `resolve-transition`'s own `:lookup-table-
  transition` case, reached from every state type's own transition
  resolution (not only `:call-submodule`), since a lookup-table
  transition can attach to any simple-transitioning state, the same way
  `:distributed-transition`/`:complex-transition` already can. Defaults
  to `{}` (no tables available) at every existing arity -- zero
  behavior change for every state type and every pre-D3 call site,
  none of which ever declare a `lookup_table_transition`.

  ADR-0105 fix (2026-08-11): the optional 6th argument, `horizon-end-t`
  (an epoch-day, `run-module`'s own shape), is consulted ONLY by the
  `:call-submodule` case, threaded straight through to `call-
  submodule-step` -- `run-submodule`'s own horizon-awareness fix.
  Defaults to `nil` (no horizon) at every existing arity -- zero
  behavior change for `walk-module` and every pre-ADR-0105 call site,
  none of which ever pass one."
  ([module rng ctx] (step module rng ctx {(:id module) module} {} nil))
  ([module rng ctx modules] (step module rng ctx modules {} nil))
  ([module rng ctx modules tables] (step module rng ctx modules tables nil))
  ([module rng ctx modules tables horizon-end-t]
   (let [module-id (:id module)
         state (get-in module [:states (:current ctx)])]
    (case (:type state)
      :terminal {:events [] :attributes (:attributes ctx) :vital-signs (:vital-signs ctx)
                 :suppressed-encounter-ends (:suppressed-encounter-ends ctx)
                 :advance 0 :next nil :terminal? true :blocked? false}
      :initial (pass-through-outcome module-id ctx rng state 0 [] tables)
      :simple (pass-through-outcome module-id ctx rng state 0 [] tables)
      ;; M5b: consumed-internally, like :simple -- gmf/gmf-type->keyword's
      ;; own docstring note (no equipment-tracking home yet, no trajectory
      ;; event, no attribute write).
      :device (pass-through-outcome module-id ctx rng state 0 [] tables)
      :device-end (pass-through-outcome module-id ctx rng state 0 [] tables)
      :delay (let [t' (resolve-time-advance rng (:t ctx) state)]
               (pass-through-outcome module-id ctx rng state (- t' (:t ctx)) [] tables))
      :guard (guard-step module-id ctx rng state tables)
      ;; GMF coverage Wave B (2026-08-02, ADR-0027, D1): both writes are
      ;; ROOT-namespaced (`root-id`), not `module-id` -- workflow scratch
      ;; is shared across a CallSubmodule call tree by construction.
      ;;
      ;; ADR-0040 AR-2: the FULL upstream precedence chain (`State.java`'s
      ;; SetAttribute.process, source-grounded) -- `ehrt.sim-trajectory.gmf`'s
      ;; own `set-attribute-unsupported-source?` already rejects
      ;; :expression/:series-data at LOAD time (this project has neither a
      ;; CQL evaluator nor a time-series mechanism), so this `cond` covers
      ;; exactly the five sources that reach here, in upstream's own
      ;; order: :range (one draw, `sample-set-attribute-range`) >
      ;; :distribution (`sample-distribution`, ADR-0035) > :value-code (a
      ;; Concept, TJR's own Pre_Procedure_Encounter_Reason/Home_Health_
      ;; Reason_Knee/Hip states) > :value-attribute (an existing root-
      ;; scoped attribute's own current value, read the SAME way
      ;; `:attribute`/`:symptom` conditions already do -- upstream's own
      ;; `if (person.attributes.containsKey(valueAttribute))`: when the
      ;; attribute was never written yet, this source does NOT fire,
      ;; falling through to :value rather than writing nil) > :value (the
      ;; literal GMF 1.0 fallback, congestive_heart_failure.json's own
      ;; `Inpatient LOS` shape -- `\"value\": 0` alongside a real
      ;; :distribution, now a legal co-present default the distribution
      ;; draw overrides, RETIRED load-time-conflict dated note above).
      ;; Retains the silent-nil fix ADR-0035 AR-4 already made: a state
      ;; whose only value source is :distribution/:range never falls
      ;; through to a bare, absent `:value`.
      :set-attribute
      (let [k (keyword (root-id ctx module-id) (gmf/slug (:attribute state)))
            va-key (when (:value-attribute state) (keyword (root-id ctx module-id) (gmf/slug (:value-attribute state))))
            v (cond
                (:range state) (sample-set-attribute-range rng (:range state))
                (:distribution state) (sample-distribution rng (:distribution state))
                (:value-code state) (:value-code state)
                (and va-key (contains? (:attributes ctx) va-key)) (get (:attributes ctx) va-key)
                :else (:value state))
            ctx' (update ctx :attributes assoc k v)]
        (pass-through-outcome module-id ctx' rng state 0 [] tables))
      ;; ADR-0035 AR-2/AR-5: a GAUSSIAN/EXPONENTIAL/TRIANGULAR Symptom
      ;; severity normalizes to the SAME state-level :distribution key
      ;; Delay/Procedure's new kinds do (gmf/apply-new-timing-
      ;; distribution groups Delay+Symptom together, D3c's own original
      ;; :range/:exact collapse precedent) -- sampled directly via
      ;; `sample-distribution` (no unit conversion: severity is
      ;; unitless, `gmf-v2-timing->v1`'s own docstring note).
      :symptom (let [severity (cond (:exact state) (:quantity (:exact state))
                                     (:range state) (rand-int-in rng (:low (:range state)) (:high (:range state)))
                                     (:distribution state) (sample-distribution rng (:distribution state))
                                     :else nil)
                     k (keyword (root-id ctx module-id) (gmf/slug (:symptom state)))
                     ctx' (update ctx :attributes assoc k severity)]
                 (pass-through-outcome module-id ctx' rng state 0 [] tables))
      ;; ADR-0041 AR-1: :assign-to-attribute -- the SAME index-based
      ;; indirection :medication-order's own case already establishes
      ;; (found live, necessary: `congestive_heart_failure.json`'s own
      ;; `CHF Condition Start` authors it, and Death's own :referenced-
      ;; by-attribute cause form -- `death-cause-codes`, below -- has
      ;; nothing to resolve without it).
      :condition-onset
      (let [event-idx (count (:trajectory ctx))
            outcome (emit-and-advance module-id ctx rng state :condition-onset {:codes (:codes state)} tables)]
        (if-let [attr (:assign-to-attribute state)]
          (update outcome :attributes assoc (keyword (root-id ctx module-id) (gmf/slug attr)) event-idx)
          outcome))
      :condition-end (emit-and-advance module-id ctx rng state :condition-end
                                        {:references (index-of-citation (:trajectory ctx) module-id
                                                                         :condition-onset (:condition-onset state))}
                                        tables)
      ;; EncounterEnd fix (2026-08-08, ADR-0082, R1/R2/R3, ADR-0081's own
      ;; rulings executed): the assert is the brief's own "one in-flight
      ;; encounter" invariant made loud (Wave H's own fold already
      ;; proves it holds for every vendored closure; a violation here is
      ;; a genuinely new module shape, not a scenario to route around
      ;; silently).
      :encounter
      (do (assert (nil? (open-encounter-index (:trajectory ctx)))
                   "ehrt.sim-trajectory.gmf-interpreter: nested :encounter -- this project's GMF subset assumes encounters never nest (Wave H's own fold discipline, ADR-0042)")
          (emit-and-advance module-id ctx rng state :encounter
                             {:codes (:codes state) :encounter-class (:encounter-class state)} tables))
      ;; A1 (open, ours): emit, referencing the TRACKED open index --
      ;; unchanged observable behavior for every module whose walks
      ;; never hit A5 below (every currently-vendored module but the
      ;; hypothyroidism/anemia_sub case ADR-0082 catalogs). A5 (nothing
      ;; open): upstream's own legal no-op -- NO EVENT, the ordinary
      ;; transition taken instead, `:suppressed-encounter-ends`
      ;; incremented (R2). A2/A3 (wellness arms) and A4 (blocked on
      ;; another module's encounter) have no separate arm here by
      ;; openness-only design (R1): a wellness encounter this module's
      ;; own walk opened closes exactly like A1 above (its own emit
      ;; site is `wellness-wait-step`, which shares this SAME openness
      ;; state); A4 is unreachable under one-module-per-patient (Wave G's
      ;; own standing scope) and disclosed, not dispatched.
      :encounter-end
      (if-let [open-idx (open-encounter-index (:trajectory ctx))]
        (emit-and-advance module-id ctx rng state :encounter-end {:references open-idx} tables)
        (pass-through-outcome module-id (update ctx :suppressed-encounter-ends inc) rng state 0 [] tables))
      :procedure (emit-and-advance module-id ctx rng state :procedure {:codes (:codes state)} tables)
      :observation (emit-and-advance module-id ctx rng state :observation (sample-observation-extra rng state) tables)
      ;; GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 R2(a)): both
      ;; state TYPES compile to the SAME trajectory event type,
      ;; :diagnostic-report -- D1a-2's own shared-ObservationGroup-parent
      ;; grounding, "one step type, both compile into it."
      (:multi-observation :diagnostic-report)
      (emit-and-advance module-id ctx rng state :diagnostic-report (diagnostic-report-extra rng state) tables)
      ;; GMF coverage Wave B (2026-08-02, ADR-0027): assign-to-attribute
      ;; / referenced-by-attribute -- a mandatory-path finding, Step 1's
      ;; own characterization (docs/gmf-interpreter.md section 9):
      ;; ear_infections.json's own called submodules assign EVERY
      ;; MedicationOrder to a root-scoped attribute holding its own
      ;; trajectory index (the same index-based reference `:references`
      ;; already carries elsewhere), and the ROOT module's own
      ;; MedicationEnd states resolve that attribute back to the SAME
      ;; index -- cross-module by construction, exactly D1's own root-
      ;; scoping contract.
      :medication-order
      (let [event-idx (count (:trajectory ctx))
            outcome (emit-and-advance module-id ctx rng state :medication-order {:codes (:codes state)} tables)]
        (if-let [attr (:assign-to-attribute state)]
          (update outcome :attributes assoc (keyword (root-id ctx module-id) (gmf/slug attr)) event-idx)
          outcome))
      :medication-end
      (let [references (if-let [attr (:referenced-by-attribute state)]
                          (get (:attributes ctx) (keyword (root-id ctx module-id) (gmf/slug attr)))
                          (index-of-citation (:trajectory ctx) module-id
                                              :medication-order (:medication-order state)))]
        (emit-and-advance module-id ctx rng state :medication-end {:references references} tables))
      ;; GMF coverage Wave D stage D2 (2026-08-02, ADR-0029 R2(b)): the
      ;; SAME shape :medication-order/:medication-end establish, two
      ;; cases up -- no attribute-based cross-module linkage this
      ;; session (the declared vendoring scope, total_joint_replacement.json,
      ;; exercises only the fixed state-name citation, gmf-interpreter.md
      ;; section 13's own G1 finding).
      :care-plan-start
      (emit-and-advance module-id ctx rng state :care-plan-start
                         (cond-> {:codes (:codes state)}
                           (:activities state) (assoc :activities (:activities state)))
                         tables)
      :care-plan-end
      (emit-and-advance module-id ctx rng state :care-plan-end
                         {:references (index-of-citation (:trajectory ctx) module-id
                                                          :care-plan-start (:careplan state))}
                         tables)
      ;; GMF coverage Wave B (D1-D4): CallSubmodule's own handling is
      ;; `call-submodule-step`, above -- it needs `modules`/`tables`
      ;; (this arity's own 4th/5th arguments), the ONE case that does.
      :call-submodule (call-submodule-step modules tables module-id ctx rng state horizon-end-t)
      ;; GMF coverage Wave C (2026-08-02, ADR-0028, C1/C2): Death's own
      ;; time resolution reuses `resolve-time-advance` unchanged --
      ;; :range/:exact are the SAME shape :delay/:procedure duration
      ;; already use, so no new time-sampling helper is needed, only a
      ;; new case wiring to the existing one (:range costs exactly one
      ;; rng draw, the same fixed-consumption law every other stochastic
      ;; choice in this project follows; :exact/neither cost none).
      ;; C2's own terminal contract: the event cites the COMPUTED death
      ;; time (not the state's own entry time -- a `:range` death is
      ;; genuinely delayed), then the walk ends here, `:terminal? true`,
      ;; `:next nil` -- Death's own declared transition (real Synthea
      ;; continues past it) is never resolved, by design (ns docstring's
      ;; own note, docs/gmf-interpreter.md section 10's own C1 account).
      :death (death-step module-id ctx rng state)
      ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-1): Counter --
      ;; SetAttribute-shaped attribute arithmetic (State.java's own
      ;; Counter class, source-grounded). :amount absent OR authored as
      ;; 0 both mean "default to 1" -- upstream's own legacy-compat
      ;; default (a Java primitive `int` field left at 0 either way, so
      ;; the two cases are indistinguishable at the source and stay
      ;; indistinguishable here). Reads/writes go through the SAME
      ;; root-namespaced key SetAttribute/Symptom already use -- Counter
      ;; is a third workflow-scratch writer, not a new namespace. Zero
      ;; draws, zero advance, no trajectory event (`pass-through-outcome`
      ;; with an empty events vector, identical to :set-attribute).
      :counter
      (let [k (keyword (root-id ctx module-id) (gmf/slug (:attribute state)))
            current (or (get (:attributes ctx) k) 0)
            amount (:amount state)
            delta (if (or (nil? amount) (zero? amount)) 1 amount)
            v (if (= :increment (:action state)) (+ current delta) (- current delta))
            ctx' (update ctx :attributes assoc k v)]
        (pass-through-outcome module-id ctx' rng state 0 [] tables))
      ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-2): ImagingStudy --
      ;; one trajectory event carrying the procedure code, primary
      ;; modality, and drawn series/instance counts (glass-box,
      ;; `imaging-study-extra`, above `step`) -- compiles to the SAME IR
      ;; step family a :procedure produces (`compile-trajectory`'s own
      ;; :imaging-study clause, upstream's own companion-procedure move,
      ;; the 30-minute stop left as record metadata, never a clock
      ;; advance: `emit-and-advance`'s own advance `cond` only fires on
      ;; :duration or a :procedure-typed :distribution, neither of which
      ;; ImagingStudy ever carries, so `advance` is 0 here with zero
      ;; extra code).
      :imaging-study (emit-and-advance module-id ctx rng state :imaging-study (imaging-study-extra rng state) tables)
      ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-3): SupplyList --
      ;; a log-only trajectory fact (State.java's own SupplyList class,
      ;; source-grounded) -- a REAL event, glass-box traceable, that
      ;; `compile-trajectory`'s own explicit :supply-list clause compiles
      ;; to NO IR step (the ConditionEnd no-open-encounter precedent
      ;; verbatim, unconditional here rather than encounter-gated).
      :supply-list (emit-and-advance module-id ctx rng state :supply-list {:components (:supplies state)} tables)
      ;; GMF coverage Wave G (2026-08-03, ADR-0037 AR-3): wellness-wait
      ;; -- loader-distinct from :encounter (gmf.clj's own
      ;; `effective-state-type` override) since it is a genuine
      ;; BLOCK-then-attach cycle, not an immediate creation -- retiring
      ;; Wave B's own create-now substitution (ADR-0031 AR-5(b)).
      :wellness-wait (wellness-wait-step module-id ctx rng state tables)
      ;; GMF coverage Wave VS (2026-08-04, ADR-0039 AR-1/AR-2): VitalSign
      ;; -- sample ONCE (exact: zero draws; range: one uniform draw,
      ;; round1'd the SAME way vital-sign-extra's own independent
      ;; Observation draw already is; distribution: one draw via
      ;; sample-distribution, ADR-0035's own general-purpose value
      ;; sampler, honoring the module's own :round when present) and
      ;; STORE the value in ctx's own :vital-signs register, keyed the
      ;; SAME `gmf/slug` way :attribute/:symptom already are -- never a
      ;; trajectory event of its own (no vendored consumer this wave
      ;; reads a VitalSign write off the log, only off the register).
      ;; `validate-vital-sign-name` (above) rejects a name outside
      ;; sim-trajectory/vital-signs.edn's own closed vocabulary, the
      ;; SAME disposition the pre-existing Observation reader already
      ;; gets for the identical gap.
      :vital-sign
      (let [name (:vital-sign state)]
        (validate-vital-sign-name name)
        (let [v (cond
                  (:exact state) (double (:quantity (:exact state)))
                  (:range state) (round1 (rand-double-in rng (:low (:range state)) (:high (:range state))))
                  (:distribution state) (sample-distribution rng (:distribution state))
                  :else (throw (ex-info "ehrt.sim-trajectory.gmf-interpreter: VitalSign state has no exact/range/distribution"
                                         {:module-id module-id :state (:current ctx)})))
              ctx' (update ctx :vital-signs assoc (vital-sign-key name) v)]
          (pass-through-outcome module-id ctx' rng state 0 [] tables)))
      ;; GMF coverage Wave I (2026-08-04, ADR-0040 AR-5): AllergyOnset --
      ;; the SAME unconditional emit `:condition-onset` already performs
      ;; (`gmf/gmf-type->keyword`'s own dated note has the full
      ;; simplification rationale: :target-encounter/:assign-to-attribute/
      ;; :reactions are declared, dead past the loader, the SAME
      ;; treatment several other v1 fields already establish).
      :allergy-onset (emit-and-advance module-id ctx rng state :allergy-onset {:codes (:codes state)} tables)
      ;; GMF coverage Wave I (2026-08-04, ADR-0040 AR-5): Vaccine -- an
      ;; unconditional leaf write, State.java's own Vaccine.process
      ;; verbatim (no target-encounter/diagnose distinction exists
      ;; upstream at all). :series defaults to 0, the SAME zero-default
      ;; a genuinely absent Java primitive int carries.
      :vaccine (emit-and-advance module-id ctx rng state :vaccine
                                  {:codes (:codes state) :series (or (:series state) 0)} tables)))))

;; --- GMF coverage Wave F (2026-08-03, ADR-0036 AR-4): honest-absence, at
;; the walk boundary -- `step` itself still THROWS `honest-absence` (the
;; internal signal that propagates unchanged through and/or/at-least/
;; guard/transition recursion, exactly like every other exception in this
;; namespace); `step-safely` is the ONE place that narrow exception gets
;; caught and turned into a recorded `:walk-error` outcome, at the SAME
;; layer `:terminal?`/`:blocked?` already surface to a caller -- result-
;; not-throw at the boundary a walk's own caller actually observes,
;; without touching `step`'s own existing throw-based contract for every
;; other exception.

(defn- honest-absence?
  [e]
  (::honest-absence (ex-data e)))

(defn- step-safely
  ([module rng ctx modules tables] (step-safely module rng ctx modules tables nil))
  ([module rng ctx modules tables horizon-end-t]
   (try
     (step module rng ctx modules tables horizon-end-t)
     (catch clojure.lang.ExceptionInfo e
       (if (honest-absence? e)
         {:events [] :attributes (:attributes ctx) :vital-signs (:vital-signs ctx)
          :suppressed-encounter-ends (:suppressed-encounter-ends ctx)
          :advance 0 :next nil
          :terminal? false :blocked? false :walk-error (ex-data e)}
         (throw e))))))

;; --- walk-module: drives `step` from :initial to Terminal or blocked ------

(defn walk-module
  "Drives `step` from `ctx`'s own `:current` until the module reaches a
  Terminal state (`:status :terminal`) or BLOCKS on a Guard whose
  condition does not hold (`:status :blocked`, `ctx`'s own `:current`
  left AT the blocked Guard, ready for a caller -- Task 3's history/
  horizon two-phase run -- to resume the SAME walk later with more
  virtual time or more attributes available).

  GMF coverage Wave B (2026-08-02, ADR-0027, D1): normalizes `ctx`'s own
  `:root` ONCE, here, before the loop starts -- `assoc`/`update` never
  touch unrelated keys, so once set it survives every fold this loop's
  own `ctx'` construction performs, all the way to `walk-module`'s next
  call (a future `CallSubmodule` recursion, once built, descends with
  the SAME `:root` still in place). Defaults to `(:id module)` when
  `ctx` doesn't already carry one -- correct for a fresh, non-calling
  walk (root = self) and a no-op when `ctx` already has one (a resumed
  blocked walk, or a submodule ctx a future recursive call passes in).

  GMF coverage Wave B (2026-08-02, ADR-0027, D3): the optional 4th
  argument, `modules` (`ehrt.sim-trajectory.gmf/load-closure`'s own
  return shape), is threaded straight through to `step` -- needed only
  if `module` itself (or anything it transitively calls) contains a
  CallSubmodule state. Omitted, `step`'s own 3-arity default (a
  single-module closure) applies, unchanged from every pre-Wave-B call
  site.

  GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3a, H2): the
  optional 5th argument, `tables` (`ehrt.sim-trajectory.gmf/load-
  closure`'s own `:tables` return shape), threads straight through to
  `step` the same way -- needed only if `module` (or anything it
  transitively calls) contains a `lookup_table_transition`. Omitted,
  `{}` applies, unchanged from every pre-D3 call site."
  ([module rng ctx] (walk-module module rng ctx {(:id module) module} {}))
  ([module rng ctx modules] (walk-module module rng ctx modules {}))
  ([module rng ctx modules tables]
   (loop [ctx (update ctx :root #(or % (:id module))) n 0]
     (when (>= n max-steps)
       (throw (ex-info "ehrt.sim-trajectory.gmf-interpreter: walk-module exceeded max-steps -- likely a module authoring bug (a zero-time-advance transition cycle)"
                        {:module (:id module) :current (:current ctx)})))
     (let [outcome (step-safely module rng ctx modules tables)
           ctx' (-> ctx
                    (assoc :attributes (:attributes outcome))
                    (assoc :vital-signs (:vital-signs outcome))
                    (assoc :suppressed-encounter-ends (:suppressed-encounter-ends outcome))
                    (update :trajectory into (:events outcome))
                    (update :t + (:advance outcome)))]
       (cond
         (:walk-error outcome) (assoc ctx' :status :walk-error :walk-error (:walk-error outcome))
         (:terminal? outcome) (assoc ctx' :status :terminal)
         (:blocked? outcome) (assoc ctx' :status :blocked)
         :else (recur (assoc ctx' :current (:next outcome)) (consume-step-budget n outcome)))))))

;; --- run-module: the history/horizon two-phase run (Task 3, docs/gmf-
;; interpreter.md section 3) -------------------------------------------------

(defn- mark-phase
  "ADR-0042 AR-1/AR-2 (2026-08-04, Wave H pre-roll): folds ONE
  trajectory event onto `[open-encounter-phase accumulated-events]`,
  returning the pair updated for the next event. `:pre-horizon` -- the
  event's own raw `t < registration-t` -- is attached unconditionally,
  unchanged from every pre-H run (byte-identical when `history?` is
  false, since no OTHER key is ever added -- load-bearing for
  ADR-0042 AR-5's pure-identity bracket). `:phase` (`:history`/
  `:horizon`) is attached only `when history?`: AR-2's own encounter-
  anchored inheritance -- an `:encounter` event's phase is its own raw
  one (the first event of a span has nothing to inherit from); every
  event between an `:encounter` and its matching `:encounter-end` (the
  `:encounter-end` itself included) inherits THAT encounter's own
  opening phase instead of its own raw timestamp, so a straddling
  encounter's close never reads as horizon content on its own account
  (ADR-0042's own worked example, the UTI Care Pathways straddle,
  ADR-0033/0034 dated notes). `open-phase` clears back to nil once the
  matching `:encounter-end` has consumed it -- encounters never nest in
  this project's own GMF subset, so one in-flight phase is always
  enough. Events outside any open encounter (before the first, between
  two, or after the last) fall back to their own raw phase, per AR-2's
  own closing clause."
  [registration-t history? open-phase acc event]
  (let [pre-horizon? (< (:t event) registration-t)
        own-phase (if pre-horizon? :history :horizon)
        inherited-phase (or open-phase own-phase)
        event' (cond-> (assoc event :pre-horizon pre-horizon?)
                 history? (assoc :phase inherited-phase))]
    (case (:event event)
      :encounter [own-phase (conj acc event')]
      :encounter-end [nil (conj acc event')]
      [open-phase (conj acc event')])))

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
  side of `registration-t` each state happens to fall on.

  GMF coverage Wave B (2026-08-02, ADR-0027, D1): `ctx`'s own `:root`
  is set to `(:id module)` here, once -- see `walk-module`'s own
  docstring note for why one normalization at the walk's true entry
  point is enough for the whole walk. The optional trailing `modules`
  argument (D3) is threaded straight through to `step`, same as
  `walk-module`'s own -- omitted, a single-module closure applies.

  GMF coverage Wave D stage D2 (2026-08-02, ADR-0029): the optional
  8th argument, `initial-attributes`, seeds `ctx`'s own `:attributes`
  map before the walk starts -- omitted, `{}` applies (`initial-
  context`'s own default, unchanged for every pre-D2 call site). Real,
  narrow need: `total_joint_replacement.json`'s own mandatory
  `Joint_Replacement_Guard` reads an attribute (`joint_replacement`)
  no state in its own closure ever writes -- delegated to two sibling
  root modules this project does not vendor (gmf-interpreter.md
  section 13's own G1 finding, D1a's governing principle: freely
  supply what a vendored artifact delegates to the engine). A purely
  additive arity, not a change to any existing one.

  GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3a, H2): the
  optional 9th argument, `tables` (`ehrt.sim-trajectory.gmf/load-
  closure`'s own `:tables` return shape), threads straight through to
  `step` the same way `modules` already does -- omitted, `{}` applies,
  unchanged for every pre-D3 call site.

  Wave H pre-roll (2026-08-04, ADR-0042 AR-1/AR-3): the optional 10th
  argument, `history?` (default `false`, every pre-H call site
  unchanged), gates a SECOND per-event mark, `:phase`, minted
  alongside the existing `:pre-horizon` boolean by `mark-phase`
  (above) -- see that function's own docstring for AR-2's encounter-
  anchored inheritance rule. `history?` false stays byte-identical to
  every pre-H run: `:pre-horizon` is still unconditional and unchanged,
  `:phase` is never attached, and `ctx`'s own new `:open-encounter-
  phase` bookkeeping never reaches `:trajectory` (an oracle digest
  never serializes bare `ctx`, only `:trajectory`, per every existing
  batch)."
  ([module rng persona registration-t] (run-module module rng persona registration-t nil {(:id module) module}))
  ([module rng persona registration-t horizon-end-t] (run-module module rng persona registration-t horizon-end-t {(:id module) module}))
  ([module rng persona registration-t horizon-end-t modules]
   (run-module module rng persona registration-t horizon-end-t modules {}))
  ([module rng persona registration-t horizon-end-t modules initial-attributes]
   (run-module module rng persona registration-t horizon-end-t modules initial-attributes {}))
  ([module rng persona registration-t horizon-end-t modules initial-attributes tables]
   (run-module module rng persona registration-t horizon-end-t modules initial-attributes tables false))
  ([module rng persona registration-t horizon-end-t modules initial-attributes tables history?]
   (loop [ctx (-> (initial-context persona) (assoc :root (:id module)) (update :attributes into initial-attributes)) n 0]
     (when (>= n max-steps)
       (throw (ex-info "ehrt.sim-trajectory.gmf-interpreter: run-module exceeded max-steps -- likely a module authoring bug (a zero-time-advance transition cycle)"
                        {:module (:id module) :current (:current ctx)})))
     (if (and horizon-end-t (>= (:t ctx) horizon-end-t))
       (assoc ctx :status :horizon-complete)
       (let [outcome (step-safely module rng ctx modules tables horizon-end-t)
             [open-encounter-phase' marked-events]
             (reduce (fn [[open-phase acc] event] (mark-phase registration-t history? open-phase acc event))
                     [(:open-encounter-phase ctx) []]
                     (:events outcome))
             ctx' (-> ctx
                      (assoc :attributes (:attributes outcome))
                      (assoc :vital-signs (:vital-signs outcome))
                      (assoc :suppressed-encounter-ends (:suppressed-encounter-ends outcome))
                      (assoc :open-encounter-phase open-encounter-phase')
                      (update :trajectory into marked-events)
                      (update :t + (:advance outcome)))]
         (cond
           (:walk-error outcome) (assoc ctx' :status :walk-error :walk-error (:walk-error outcome))
           ;; ADR-0105: a called submodule's own walk crossed the horizon
           ;; (run-submodule's own horizon-awareness fix) -- end the WHOLE
           ;; walk here, the SAME :status :horizon-complete a top-level
           ;; Delay overshoot produces (call-submodule-step's own docstring).
           (:horizon-complete? outcome) (assoc ctx' :status :horizon-complete)
           (:terminal? outcome) (assoc ctx' :status :terminal)
           (:blocked? outcome) (assoc ctx' :status :blocked)
           :else (recur (assoc ctx' :current (:next outcome)) (consume-step-budget n outcome))))))))
