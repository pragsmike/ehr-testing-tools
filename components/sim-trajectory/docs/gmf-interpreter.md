# The GMF interpreter: v1 semantics

The design doc for Milestone M5's `RunModules` (`components/sim/docs/sim-theory.edn`)
— the Synthea Generic Module Framework (GMF) interpreter port. This
document specifies what `RunModules` actually does: which GMF state
and transition types v1 executes, how it turns a module walk into
`clinical-trajectory` events, the history/horizon split that lets a
persona's full pre-run life compress into a manageable simulated
fast-forward, how a GMF encounter becomes an ADT admission of the
right stripe, and the attribute-namespacing discipline that keeps
vendored modules from stepping on each other or on this project's own
engine-reserved state. [`notes/ADRs.md`](../notes/ADRs.md) ADR-0013
records the sibling decision (module vendoring: target, provenance,
curation criterion) this document's own candidate-module survey (the
appendix) feeds directly. This document stays the as-built spec of
*this project's own* interpreter — for how a real Synthea module is
structured and run upstream, why so much of the current module
catalog still fails to clear the bar below, and the ordered ladder of
future extensions that would change that, see its companion,
[`gmf-source-model.md`](gmf-source-model.md).

**Provenance discipline for this document.** Per `AGENTS.md`'s
standing rule ("do not invent facts about upstream sources"), every
claim below about what a real Synthea module contains is sourced from
reading real module JSON at a pinned commit, not from memory or
inference about the Generic Module Framework in general. Commit
`7e08387c68a7f0e21d13076609a159fd473fc902` of
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea)
(`master`, fetched 2026-07-27) is the pin for every module citation in
this document and its appendix; three modules were read in full against
it —
[`sinusitis.json`](https://raw.githubusercontent.com/synthetichealth/synthea/7e08387c68a7f0e21d13076609a159fd473fc902/src/main/resources/modules/sinusitis.json),
[`ear_infections.json`](https://raw.githubusercontent.com/synthetichealth/synthea/7e08387c68a7f0e21d13076609a159fd473fc902/src/main/resources/modules/ear_infections.json),
and
[`sore_throat.json`](https://raw.githubusercontent.com/synthetichealth/synthea/7e08387c68a7f0e21d13076609a159fd473fc902/src/main/resources/modules/sore_throat.json)
— with a fourth, `bronchitis.json`, spot-checked while scouting for a
urinary-tract-infection candidate (below). The appendix carries the
full survey; this section's state-type inventory is informed by all
four reads, not just the three formally tabled.

## 1. State types, v1

Synthea's own Generic Module Framework has more state types than this
project's v1 interpreter executes. The table below is v1's scope —
what a module author's state compiles to, or why it's consumed without
producing a trajectory event. "Deferred" state types are read and
skipped over structurally (a module using one fails this project's own
curation criterion, ADR-0013 point 4, until a later milestone extends
the interpreter) rather than causing a load error — the interpreter
walks past what it doesn't yet execute the same way a parser tolerant
of unknown fields does, so a deferred-type module can still be *read*
for survey purposes (the appendix below does exactly that) even though
it can't yet be *vendored*.

| State type | v1 disposition | Trajectory-event mapping |
|---|---|---|
| `Initial` | v1, consumed internally | none — module entry point, no clinical or administrative fact |
| `Terminal` | v1, consumed internally | none — ends this module's own walk for the patient; **not** `Death` (below) |
| `Simple` | v1, consumed internally | none — pure branching/logic state (e.g. `sinusitis.json`'s `Penicillin_Allergy_Check`, `sore_throat.json`'s `Determine_if_Bacterial`); carries no clinical content of its own |
| `Delay` | v1, consumed internally | none — advances simulated time only. Compiles to the same `:advance N` shape every other step type's `decide` already returns (`ehrt.sim.engine`); the interpreter samples the delay's own authored range from the run's single RNG, same determinism law every other stochastic draw already follows |
| `Guard` | v1, consumed internally | none — blocks until its condition holds, re-checked each time its module is next due; no clinical or administrative fact of its own |
| `SetAttribute` | v1, consumed internally | none — writes into `:attributes` (§5, module-namespaced) |
| `ConditionOnset` | v1, trajectory event | a clinical fact — code triplet carried verbatim (code-passthrough law). Compiles at `CompileTrajectory` to a diagnosis annotation on the enclosing (or most recently opened) `Encounter`-mapped IR step, **not** a standalone IR step type — this project's pathway IR has no diagnosis-list step today, and DG1/billing rendering is separately gated on `snomed-icd10-map` landing (`components/sim/docs/sim-theory.md`'s Catalytic resolution table, still target 1/not built) |
| `ConditionEnd` | v1, trajectory event | references its `ConditionOnset` trajectory event (the same "references an existing prior event" shape `:cancel-*`/`:result-available` already establish, `components/sim/docs/patient-state-model.md`). If no encounter is open when it fires, it is still a real trajectory event (glass-box traceability doesn't lapse just because nothing is rendering to wire right now) but compiles to no IR step — a log-only fact, the same shape `:step-rejected` already established for "real, worth keeping, not worth a message" |
| `Encounter` | v1, trajectory event | the state type that drives the encounter mapping (§4) — becomes `:admission` (A01), the new `:outpatient-visit` (A04), or an ED-admission via the existing ladder, depending on the module's own encounter class |
| `EncounterEnd` | v1, trajectory event | mirrors its opening `Encounter` — `:discharge` (A03) for inpatient/ED-class, the new `:outpatient-visit-end` (no message, §4) for outpatient-class |
| `Procedure` | v1, trajectory event | compiles to a new `:procedure` IR step (does not exist in `ehrt.sim.pathway` today — M5b build scope) |
| `Observation` | v1, trajectory event | compiles to a new, lighter-weight `:observation` IR step — **not** `:order`/`:result-followup` (order-profiles' panel/turnaround-time semantics don't fit a same-encounter vitals or exam finding, `sore_throat.json`'s `Take_Temperature_High`/`Take_Temperature_Low` being the read example); M5b build scope |
| `MedicationOrder` | v1, trajectory event | compiles to a new `:medication-order` IR step (M5b build scope, distinct from `:order`'s lab-panel semantics) |
| `MedicationEnd` | v1, trajectory event | references its `MedicationOrder` trajectory event; compiles to a new `:medication-end` IR step |
| `Device` / `DeviceEnd` | v1, consumed internally **(M5b finding, moved here from the Deferred table below)** | none — structurally identical to `Simple`: no equipment-tracking home exists anywhere in `components/sim/docs/patient-state-model.md`'s accumulator or the pathway IR, so these states pass through (ordinary transition resolution, no attribute write, no trajectory event) rather than mint anything. Discovered load-bearing, not merely convenient: the ratified vendored module (`sinusitis.json`) uses them for its Nebulizer content, exactly where this document's own appendix already predicted (confined to the module's rare chronic-surgical tail) — but the M5a loader's own all-or-nothing gate ("any deferred-type use fails it, full stop") rejected the WHOLE module for two states, not merely that tail, since the loader has no partial-compile mechanism to "simply not compile that one branch's terminal states" the way the appendix's prose imagined. Consumed-internally is the minimal, disciplined resolution — see the M5b findings section below for the full account |
| `CallSubmodule` | v1, trajectory-adjacent **(GMF coverage Wave B, ADR-0027, moved here from the Deferred table below — the same "move, not duplicate" treatment `Device`/`DeviceEnd` already got at M5b)** | none of its own — recursion into a second module JSON file, loaded/namespaced/gated as part of the SAME closure (§1's own loader gate now extends to it, `ehrt.sim-trajectory.gmf/load-closure`, D3) and run via descend-run-return (`ehrt.sim-trajectory.gmf-interpreter`'s own D1-D4 order contract, ns docstring). Every trajectory event a called submodule itself emits carries the normal event-type mapping this table already assigns it (a `MedicationOrder` inside a callee is still a `:medication-order` event) PLUS a `:call-path` citation (D2) — see §9 for the full account |
| `Death` | v1, terminal trajectory event **(GMF coverage Wave C, ADR-0028, moved here from the Deferred table below — the same "move, not duplicate" treatment `Device`/`DeviceEnd`/`CallSubmodule` already got)** | ends the walk (`:terminal? true`, `:next nil` — the module's own declared post-Death transition is never resolved, a disclosed departure from real Synthea's own continue-past-Death semantics). Compiles at `CompileTrajectory` to the EXISTING `:discharge` IR step, no new step type — death inside a still-open encounter attaches as that encounter's own terminal disposition (`:disposition :expired`, `:codes` the cause of death verbatim); death outside any encounter closes the pathway without fabricating a discharge from an admission that never happened. `ehrt.sim.engine`'s own `:discharge` decide/evolve fold this to `:status :expired` (`components/sim/docs/patient-state-model.md`'s accumulator table, real for the first time) — see §10 for the full account |
| `MultiObservation` / `DiagnosticReport` | v1, trajectory event **(GMF coverage Wave D stage D1, ADR-0029, moved here from the Deferred table below — the same "move, not duplicate" treatment `Device`/`DeviceEnd`/`CallSubmodule`/`Death` already got; `DiagnosticReport` enters this table for the first time — it was never in this document's own original brief at all)** | both extend Synthea's own private `ObservationGroup` class and compile to the SAME new `:diagnostic-report` IR step (embedded, inline `Observation`-shaped children, never a reference — §11's own D1a-2 grounding against `State.java`). `:observation`'s own IR step gains `:value-code`/`:category`/`:reference-range`/`:interpretation` alongside it, closing the `value_code`/`vital_sign` value-sourcing gap a standalone Observation state can also carry — see §12 for the full per-layer account |
| `CarePlanStart` / `CarePlanEnd` | v1, trajectory event **(GMF coverage Wave D stage D2, ADR-0029, moved here from the Deferred table below — the same "move, not duplicate" treatment every prior wave's own state-type additions already got)** | a paired span structurally identical to `MedicationOrder`/`MedicationEnd` (State.java's own `CarePlanStart extends AttributeAssignableState`/`CarePlanEnd` classes, §13's own grounding) — compiles to new `:care-plan-start`/`:care-plan-end` IR steps, `:care-plan-citation` resolved the same `:references`-index way `:order-citation` already is. CarePlan itself stays v2-silent (R3) — no HL7v2 message shape, its natural rendering is a future FHIR CarePlan resource. The mechanism is built and tested; NO real vendored module exercises it yet as of this table's own writing (§13's own fix-forward: both D2 candidates deferred for unrelated reasons, one a compound-Guard interpreter gap) |

**Deferred, with reasons:**

| State type | Why deferred |
|---|---|
| `Counter` | Named in this document's own brief as deferred; not observed in any of the four modules read this session, so its omission carries no survey evidence either way — carried forward as originally scoped |

**A recommendation, flagged for author review: add `Symptom` to v1 as
a write-only, consumed-internally state — not deferred.** `Symptom` is
not in this document's original brief at all, but the survey found it
pervasive: 19 of `sinusitis.json`'s 44 states, 23 of `sore_throat.json`'s
44, and it would very likely recur in almost any acute-illness module
this project vendors next, since it's how Synthea modules track
symptom severity as the module runs. In every state read this session,
`Symptom` is a **leaf write** — it sets a severity value and is never
itself the target of a `Symptom`-typed guard condition anywhere in the
three formally surveyed modules (confirmed by reading every transition,
not inferred). That makes it structurally identical to `SetAttribute`
for this project's purposes: compile a `Symptom` state to a write into
a module-namespaced `:attributes` key (§5) holding the sampled
severity, no trajectory event, and — the one deliberate v1
simplification — **no `Symptom`-typed guard condition exists in v1's
own condition vocabulary** (§2), so nothing downstream ever reads the
value back through the interpreter itself; it is recorded (available
to a future Z-segment template or state-document render, the way any
other `:attributes` entry is) but not load-bearing to any v1 module's
own control flow. Excluding `Symptom` from v1 would fail almost every
short acute-illness module surveyed on a technicality that carries no
real interpreter complexity to support — the opposite of this
project's own curation discipline (ADR-0013), which exists to keep
real complexity out, not to reject cheap wins.

*Ratified 2026-07-26 (item 1 of 8, this document's closing list, below).*

**Dated note, GMF coverage Wave A (2026-08-02, AR-3,
`.agents/plans/2026-08-02-gmf-coverage-plan.md`): reconfirmed, and
disambiguated from a same-named but distinct addition.** This flag's own
"no `Symptom`-typed guard condition exists in v1's own condition
vocabulary" line (above) was true when written and stayed true through
M5a/M5b — but Wave A's own characterization step found a real, mandatory-
path need for exactly that: `sore_throat.json`'s `Determine_if_Bacterial`
(§ appendix, below) wraps `At Least`-compound sub-conditions of type
`Symptom` (reading the severity a `Symptom` STATE already writes). Wave A
therefore adds `Symptom` to §2's own condition vocabulary too — a
distinct v1.1 addition to the ALREADY-RATIFIED `Symptom` STATE this flag
covers, not a reopening of this flag's own recommendation. See §2, below,
for the condition-side addition and its own citation.

## 2. Transitions, v1

The original four of Synthea's transition kinds were v1 scope from the
start — every module surveyed at M5-prep used all four, so there was
no candidate-driven reason to defer any of them. **GMF coverage Wave B
(2026-08-02, ADR-0027, D5) adds a fifth: `type_of_care_transition`**
(below) — Synthea's own care-setting dispatcher, characterized against
real source before implementation and built once `urinary_tract_
infections.json`'s own survey named it (§9 has the full dispatch-rule
account, including the documented simplification this project's own
lack of a payer/insurance concept requires).

- **`direct_transition`** — unconditional, one target state.
- **`distributed_transition`** — a weighted distribution over target
  states, sampled from the run's single seeded RNG (the same
  determinism law every other weighted pick in this project already
  follows — `components/sim/docs/sim-theory.edn`'s global determinism law).
  **GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3b, H3):** an
  entry's own `:distribution` may ALSO be a NamedDistribution map
  (`{:attribute name :default n}`, an attribute-sourced weight with a
  JSON-specified fallback, `Transition.java`'s own field names
  verbatim) — `stroke.json`'s own `Chance_of_Stroke` gate, ADR-0028;
  the mechanism is built, but stroke itself stays deferred (§10's own
  dated note: `stroke_risk` is SPECIFIED, unsourceable content, not
  resolved by the mechanism landing).
- **`conditional_transition`** — an ordered list of
  (condition, target) pairs, first match wins; if NO condition matches
  and none is condition-less, the LAST entry is used unconditionally
  (§14's own D3f finding — this project's own port did not implement
  this fallback until `urinary_tract_infections.json`'s real closure
  exercised the gap).
- **`complex_transition`** — `conditional_transition` and
  `distributed_transition` composed: an ordered list of conditions,
  each guarding EITHER a direct `:transition` OR its own nested
  `:distributions` list (§14's own D3f finding — real Synthea's
  `ComplexTransitionOption` allows either per branch; this loader's own
  schema previously required `:distributions` unconditionally), same
  first-match/last-entry-fallback rule as `conditional_transition`.
- **`type_of_care_transition`** (Wave B) — a fixed
  `{ambulatory, emergency, telemedicine}` map of target states, no
  weights of its own in the module JSON; this interpreter resolves it
  via the SAME weighted-pick mechanism `distributed_transition` already
  uses (one `.nextDouble` draw), against a year-gated weight table
  (§9's own D5 account has the full characterization and citation).
- **`lookup_table_transition`** (GMF coverage Wave D stage D3,
  2026-08-02, ADR-0029, D3a, H2) — the SIXTH kind, discovered on
  `urinary_tract_infections.json`'s own entry path (§9) and NAMED, not
  built, at Wave B (it needed an external lookup-table CSV mechanism
  this project had no analog for). Landed this session: closures may
  carry DATA-FILE members (lookup-table CSVs) alongside JSON modules
  (R4, `ehrt.sim-trajectory.gmf/load-closure`'s own `table-resolve-fn`);
  a zero-rng row lookup (age range + a curated set of other recognized
  attribute columns, H2's own specify-vs-delegate audit) against the
  resolved table, then ONE weighted-pick draw over the matched row's
  own weights, falling back to each entry's own JSON-declared
  `default_probability` on no match (real Synthea's own
  `defaultTransitions` mirror). Full characterization and both
  vendored tables (`uti.csv`/`uti_recurrence.csv`): §14.

**Condition predicates, v1 as originally scoped: age, sex, attribute,
and `PriorState`** — plus, **M5b finding**, `Active Condition`/`Active
Medication`/`And`, joined once the ratified vendored module
(`sinusitis.json`) turned out to need them on its own MANDATORY
post-encounter path (`Wait_for_condition_to_resolve`, reached by every
patient who ever completes a Doctor_Visit encounter, not an excludable
tail the way `Device`/`DeviceEnd` is), plus `Active Allergy` as a
documented, always-false simplification (see the M5b findings section
below for the full account of all four, including why `Active Allergy`
could NOT be built the same log-query way the other two were). Synthea's
own condition vocabulary is still larger than this (the three formally
surveyed modules alone also use compound `At Least`-N-of wrappers — see
the appendix's own gap notes, still deferred at M5b); the original four were
chosen because they cover every module's *entry* logic (age/sex-gated
onset) and the one compilation decision worth making carefully up front:

**`PriorState` compiles to a ground-truth-log query — the log IS
`person.history`, done right.** `components/sim/docs/patient-state-model.md`'s own
mining section already stated this as the plan: Synthea's GMF guards
conditional transitions like "prior state X, within window" by walking
`Person.history`, a mutable-world approximation of an event log built
because the GMF interpreter had no primitive log to query instead
(`.agents/memory/architecture.md`'s Synthea entry; `components/sim/docs/event-sourcing.md`'s
own retelling). This project has the log already, typed, timestamped,
and authoritative (ADR-0008) — so `PriorState` compiles directly to a
query over it, never to a second history structure the interpreter
would have to build and keep in sync, the same "one authoritative
record, everything else a projection" discipline this project applies
everywhere else (`components/sim/docs/event-sourcing.md`'s three-deep authority
hierarchy).

**This fixes the interpreter's own guard-evaluation shape, and — the
good news — no engine signature change is required to support it.**
`decide`'s real signature (`(rng, t, world, patient-id, step)`,
`engine.clj`) already carries what a `PriorState` guard needs: `world`
has carried a `:ground-truth` key — "a persistent mirror of the
log-so-far threaded through `world` specifically so `decide` can query
it directly" — since M2b (`components/sim/docs/patient-state-model.md`'s "deterministic
event id" section), landed for the cancel family and
`:transfer-in-error`'s own prior-location lookups. `engine.clj` already
ships the query primitive, too: `events-for-patient` (`ground-truth,
patient-id -> events`, ADR-0010-era) is exactly the log-view a
`PriorState` guard needs to walk looking for its target state. This is
the concrete fulfillment of an anticipation `engine.clj`'s own
top-of-file doctrine comment made at ADR-0008's landing (M1-era): decide
"consults `world`... this is where cross-patient coupling lives" —
`world` was always going to need to carry more than bare patient
states once a consumer needed to ask "what happened," not just "what
is true now," and M2b's `:ground-truth` key is that growth, landed
two milestones before M5 needed it. **The interpreter's own guard
evaluator, informally: `(evaluate-condition condition patient-state
(:ground-truth world) step)`** — reading `world`'s existing
`:ground-truth` mirror through `events-for-patient`, not a new
parameter engine.clj has to grow. `PriorState`'s own "target state,
within window" semantics become a `filter` over that patient's own
event subsequence for the target module/state citation (§6), most
recent first, optionally bounded by a time window computed from `t`.

**GMF coverage Wave A (2026-08-02,
`.agents/plans/2026-08-02-gmf-coverage-plan.md`) adds five more
predicates, each ruled in because its data source already exists —
Step 1's own membership bar (AR-2) — with no new state home:**
`Or`/`At Least` (boolean-disjunction and N-of-M compound wrappers, the
same recursive shape `And` already establishes — Synthea's own
Logic.java `Or`/`AtLeast` classes); `Date` (a calendar-year comparison
against the interpreter's own virtual clock, `ctx`'s `:t`, already
threaded since M5a); `Observation`-as-a-condition-type (a log query over
already-emitted `:observation` trajectory events by concept, the same
shape `Active Condition`/`Active Medication` already establish — the
value itself was already sampled and carried by the already-built
`Observation` STATE type); and `Symptom`-as-a-condition-type (an
emergent finding, not one of AR-2's five NAMED candidates, but required
for `At Least`'s only real vendored use — see the flag's own dated note,
§1, above, for the disambiguation from the already-ratified `Symptom`
STATE). `Active Allergy` was ALSO one of AR-2's five named candidates,
but needed no new work: it already joined v1 at M5b (above), and
`sore_throat.json`'s own `Active Allergy` checks (appendix, below) use
the identical RxNorm-7984-Penicillin-V shape `sinusitis.json`'s already
does. `Vital Sign`/`Active CarePlan` stay OUT (AR-2, pre-ruled — no
accumulator or IR home exists for either yet); real, confirmed by this
session's own characterization: neither appears anywhere in
`sore_throat.json`. Semantics for all five grounded against Synthea's
own `Logic.java`/`Person.java` at this document's own pinned commit
(`ehrt.sim-trajectory.gmf-interpreter`'s own per-predicate docstrings
carry the full citation); `Observation`'s own v1 scope omits the "is
nil"/"is not nil" operators real Synthea also supports, and `Date`'s own
v1 scope omits the `:month`/`:date` variants — neither needed by any
candidate module this session read.

## 3. The history/horizon design

**This is the hard decision.** A GMF module runs from birth in real
Synthea; this project's own scope is an encounter horizon
(ADR-0007 point 3: "hospital-operations traffic across a single
encounter... not a patient's lifelong longitudinal history"). Persona
already assigns every patient a DOB (`components/sim/docs/patient-state-model.md`,
Milestone M4) — which means a patient can arrive at age 45, and a
GMF-driven trajectory needs *some* answer for what 45 years of module
activity produced by the time they walk in, without this project
actually simulating 45 years of operational traffic to get there. The
answer is a two-phase run per patient.

**"Run start," precisely.** Because this project has no single
wall-clock moment shared by every patient — arrivals are scheduled
individually (a fixed count or, per ADR-0011 point 3, a seeded arrival
process) — "run start" for a given patient's own history/horizon split
means **that patient's own `:registered` event time**, the moment
their operational encounter horizon begins (`ehrt.sim.engine`'s
existing `:registered` event, Milestone M4), not a single instant
shared across the whole run.

**History phase: birth to registration, fast-forwarded, no operational
simulation.** Every module a patient's persona makes eligible walks
its own state graph from `Initial` starting at the patient's DOB,
advancing through `Delay`/`Guard`/transition logic exactly as it would
during the horizon phase — same RNG draws, same distributions, same
condition evaluation — up to the patient's own registration instant.
The difference is what happens to `Encounter`/`Procedure`/
`MedicationOrder`/`Observation` states crossed along the way: their
*state effects* fold normally (a `ConditionOnset` crossed during
history leaves the patient with that condition at registration time; a
`MedicationEnd` crossed during history means that medication is no
longer active at registration time), but **no operational trajectory
event is minted for the encounter machinery itself** — there is no
value in a compiled IR admission step for a doctor's visit that
happened fourteen years before this run's own registration event.

**Horizon phase: registration onward, full trajectory emission.**
From the patient's registration instant, every module resumes exactly
where the history phase left its state cursor, and now every state
crossed — including `Encounter`/`Procedure`/`MedicationOrder`/
`Observation` — emits real `clinical-trajectory` events, per §1's
table, that `CompileTrajectory` turns into real IR steps. This is the
phase this project's existing laws (code passthrough, glass-box
traceability, clinical-content-preserving compilation,
`components/sim/docs/sim-theory.edn`'s `:trajectory`/`:compile` laws) actually govern
day to day.

**Determinism.** Both phases draw from the run's single seeded RNG, in
a fixed order: for a given patient, persona sampling (13 draws, M4)
happens first, then the history phase's own module-walk draws, then
the horizon phase's own draws — the same per-patient, arrival-ordered
sequencing `engine/run` already establishes for every other stochastic
source. Landing `RunModules` will perturb the pinned-seed fixture
again, exactly as ADR-0009 already anticipates for every milestone
that grows the engine's stochastic surface — expected, not a
regression to chase.

**Two open sub-questions, each with a recommendation — both ratified
2026-07-26 (items 2 and 3 of 8, this document's closing list, below):**

1. **Granularity of the history-phase fast-forward.** Real Synthea
   advances in fixed weekly ticks because its own horizon is a
   lifetime (`components/sim/docs/patient-state-model.md`'s mining section already
   notes this, "for completeness rather than as a design constraint").
   **Recommendation: no fixed tick at all.** The history phase should
   reuse the exact same per-state transition-sampling logic the
   horizon phase uses — each state's own authored `Delay` range or
   transition determines how much simulated time passes, chained
   state to state until the walk crosses the patient's registration
   instant — rather than looping at an artificial fixed interval. This
   is not a new idea for this project: `engine.clj`'s own header
   already states "this project's encounter-horizon discrete-event
   engine has no equivalent tick loop" (`components/sim/docs/patient-state-model.md`'s
   mining section), and a fixed-tick history phase would be exactly
   the tick loop this project's own discrete-event architecture was
   built to avoid, reintroduced only for the part of a patient's life
   this project doesn't otherwise care about. The cost is bounded RNG
   consumption per patient (proportional to the number of state
   transitions a module actually makes between birth and registration,
   not to elapsed calendar time), which is also the cheaper failure
   mode to reason about if ADR-0009's fixture-regeneration policy ever
   needs to explain a perturbation.
2. **Do pre-horizon facts enter the log, or live only as attributes?**
   **Recommendation: mark, don't trim — the same choice ADR-0011
   already made for warm-up traffic, for the identical reason.** A
   condensed set of `:pre-horizon true` events — one per
   `ConditionOnset`/`ConditionEnd`/`MedicationOrder`/`MedicationEnd`
   the history-phase walk actually crosses (not one per state visited;
   `Delay`/`Guard`/`Simple`/`Initial`/`Terminal`/`SetAttribute`/
   `Symptom` never produce trajectory events regardless of phase, §1)
   — enters `ground-truth-log`, each still citing `{:module :state}`
   per the glass-box law, so a patient's initial attribute/condition
   state at registration is always auditable back to *why*, not merely
   asserted. This is a direct application of the same lesson this
   project already drew from Synthea's own export-window confusion
   (issues #1465/#1040, `components/sim/docs/research/SimHospital-Synthea-limitations-
   considered.md` §4.1/§4.2, already cited in `components/sim/docs/patient-state-
   model.md`'s warm-up section): content that silently doesn't appear,
   with no marker explaining why, is a documented failure mode this
   project has already committed not to reproduce. `check.clj` and any
   consumer can filter `:pre-horizon` events the same way `:warm-up`
   events are already filterable today.

## 4. Encounter mapping

GMF's own encounter classes — `wellness`, `ambulatory`, `emergency`,
`inpatient` (the vocabulary the three surveyed modules' `Encounter`
states use directly, e.g. `sinusitis.json`'s `Doctor_Visit` is
`ambulatory`, `ear_infections.json`'s `Next_Wellness_Encounter` is
`wellness`) — map onto this project's existing ADT vocabulary and one
new step-type pair:

| GMF encounter class | Maps to | Notes |
|---|---|---|
| `wellness` / `ambulatory` | **new `:outpatient-visit` step, A04** | Activates `:class :outpatient`, already a value in `components/sim/docs/patient-state-model.md`'s `:class` enum but with no allocation path or message type wired to it yet — this milestone is that wiring |
| `emergency` | ED admission via the existing allocation ladder | No new step type — an `:admission` targeting a `:class :ed` ward is already this project's existing shape (`components/sim/docs/operational-models.md`) |
| `inpatient` | `:admission`, A01 | No new step type — this project's existing, `:built` shape |

**Dated note (2026-08-03, `notes/ADRs.md` ADR-0031 AR-5(c)).** This
row's `wellness` / `ambulatory` grouping CONFLATES two distinct
upstream constructs that happen to share the word "wellness," found
live-probing Synthea source at this document's own pin
(`7e08387c68a7f0e21d13076609a159fd473fc902`) for the parity plan's Q2
ruling: (1) a module-authored `Encounter` state carrying the class
STRING `"wellness"` — checked against every vendored root at the same
pin, NONE actually use it (`sinusitis.json`/`sore_throat.json` are
`"ambulatory"`; `ear_infections.json`'s other encounter is
`"outpatient"`); and (2) the `"wellness": true` BOOLEAN idiom
(`gmf.clj`'s own normalization clause, below), which upstream does not
treat as a same-session class string at all — `State.java`'s
`Encounter.process` wellness branch creates nothing and BLOCKS until
the engine's hardcoded `EncounterModule` opens its own
separately-scheduled next wellness encounter. This table's row
predates that finding and still describes only the STRING case (1);
case (2)'s real semantics, and the loader's own disclosed timing
substitution for it, are captured where the loader clause lives
(`gmf.clj`'s own dated comment, ADR-0031 AR-5(b)) rather than rewritten
into this table.

**The new step types, sketched for M5b build scope (not built this
session):** `:outpatient-visit` / `:outpatient-visit-end`, paired
directly (like `:admission`/`:discharge`, not auto-paired like
`:order`/`:result-followup` — a GMF module's own `Encounter`/
`EncounterEnd` pair already brackets start and end explicitly, so there
is no turnaround-time to sample and no ergonomic reason to auto-pair).

**Sketch, ratified 2026-07-26 (items 5–8 of 8, this document's closing
list, below; item 4 is `sinusitis.json`'s own vendoring recommendation,
appendix):**

- **(Item 5.)** `:outpatient-visit`'s `decide` does **not** call
  `ehrt.sim.facility/allocate` — an outpatient encounter
  occupies no bed, so there is no ladder to consult. `:status`
  transitions `:new -> :admitted` (reusing the existing value — an
  outpatient patient genuinely is "mid-encounter," which is what
  `:status` actually tracks) and `:class` is set to `:outpatient`
  (already a legal value).
- **(Item 6.)** `:location` stays **deliberately nil** for
  the visit's duration — a **named exception**, gated on `:class
  :outpatient`, to `components/sim/docs/patient-state-model.md`'s existing "never
  nil-bed, even while boarding" rule for `:location`, which was written
  before this project had a class of patient with no bed to be nil
  *about*. **Ratification note:** this exception is not merely prose —
  it lands, M5b, as a genuine row in `components/sim/docs/patient-state-model.md`'s
  event-validity table (the same status × event-class ×
  attribute-conditions shape that table's post-mortem/donor rows
  already use, not a special-cased sentence bolted alongside it):
  `:location = nil` is legal exactly when `:class = :outpatient`, for
  events of the encounter/therapeutic-intent classes an outpatient
  visit spans, illegal otherwise — a *conditional* validity row, keyed
  on status × event-class × the `:class :outpatient` attribute
  condition, the mechanism the table's own donor row already
  establishes for a gated exception.
- **(Item 7.)** `:outpatient-visit-end` transitions `:status` `:admitted ->
  :discharged`, the same terminal value inpatient discharge already
  uses (no new `:status` value invented — `:class :outpatient` is
  already the distinguishing fact, the same way `:class` already
  distinguishes inpatient sub-cases without a parallel `:status`
  value). It emits a real ground-truth event but — **deliberately, by
  the same precedent ADR-0012 already established for
  `:step-rejected`** — gets **no `message-type-registry` entry** in
  v1: many real ambulatory feeds send a single A04 and no closing
  message for a same-day visit, so inventing a discharge-shaped message
  here would be manufacturing wire traffic no real interface sends,
  the opposite of this project's realism goal. The ground-truth event
  still exists because `state-history` (the fold, ADR-0008) needs a
  real event to transition on, independent of whether anything renders
  it.
- **(Item 8.) New invariants this implies (co-landing scope, `check.clj`, M5b):**
  `outpatient-visit-only-when-new` (the `:admission`-row's own
  treatment, applied to the new step); a structural
  `outpatient-patients-occupy-no-bed` check (`:class :outpatient =>
  :location nil`, for the visit's duration — the same fact item 6's
  validity-table row states, checked mechanically here); and the
  occupancy board's own consistency law (`components/sim/docs/operational-models.md`'s
  "board ≡ fold over patient locations") gains an explicit scope
  qualifier — *inpatient/ED* patient locations, not every patient's —
  since an outpatient patient was never a candidate for the board to
  include in the first place, not an exception being carved out of an
  otherwise universal law.

This sketch touches an existing "never nil" invariant (`:location`),
even though only under a new, named, narrowly-gated condition — see
this document's closing list, below, for the full ratification
record.

## 5. Attributes registry

`components/sim/docs/patient-state-model.md`'s reserved `:attributes` map
(`[:map-of :keyword :any]`, unused until M5) is where every module's
`SetAttribute`/`Symptom` writes land. **v1 discipline: every write is
auto-namespaced by the writing module's own id.** A module's raw
Synthea attribute key (a bare string, e.g. `sinusitis.json`'s
`bacterial_infection`) compiles to a keyword namespaced under the
module's own id (e.g. `:sinusitis/bacterial-infection`) in this
project's `:attributes` map, never a bare keyword. This is a deliberate
departure from Synthea's own flat `Person.attributes` map, where every
module shares one namespace by convention rather than by any enforced
boundary — and it is this project's own no-hidden-modules corollary
(`components/sim/docs/sim-theory.md`'s IR-transforms section, restated at the M5
roadmap entry) applied to *data* coupling, not just *execution*
coupling: a module silently reading another module's bare attribute
key is exactly the kind of invisible cross-module coordination that
corollary already rules out for always-on execution, and there is no
principled reason data coupling should get a pass execution coupling
doesn't.

**Dated note, GMF coverage Wave B (2026-08-02, ADR-0027, D1): this
section's own "namespaced by the writing module's own id" claim now
has two distinct senses, not one — disambiguated here, not silently
narrowed.** LOAD-TIME declared-write namespacing (`ehrt.sim-
trajectory.gmf/declared-attributes`, the reserved-key collision check
this section's own next paragraph describes) is UNCHANGED — a static
property of each module's own JSON, computed per-module regardless of
whether that module is ever called as a submodule. RUNTIME attribute
namespacing DURING A WALK (`gmf-interpreter`'s own `step`, the
SetAttribute/Symptom write path and the Attribute/Symptom condition
read path) is now root-scoped, not module-scoped: a `CallSubmodule`
callee and its caller share one namespace (the walk's own root module
id), by design (D1's own three-compartment person record, `docs/gmf-
interpreter.md`'s own §9). This is intentional sharing, not a
regression of the collision-freedom property below — but it DOES open
a distinct risk the load-time check does not cover: two different
closure members writing the SAME bare attribute name for UNRELATED
reasons would now collide at RUNTIME (not caught at load), whereas two
STANDALONE top-level modules never could. Not observed in either Wave
B closure this session (`ear_infections.json`'s own two called
submodules write disjoint bare keys) — named here as a real,
considered gap a future closure-wide write-collision check could close
(the same shape the existing reserved-key check already establishes,
widened from "one module vs. the engine's own reserved keys" to "every
closure member vs. every other"), not built this session since no
candidate closure has yet needed it.

**Because every write is namespaced by its own module, cross-module
collisions are structurally impossible in v1** — two different modules
can never produce the same namespaced key, since each module's own id
is part of the key. **"Collisions are a validation error at module
load," per this milestone's own scope, therefore names a narrower,
real check: a vendored module writing a bare (non-namespaced) key that
collides with an engine-reserved attribute.** `:donor`
(`components/sim/docs/patient-state-model.md`'s post-mortem entry,
`components/sim/docs/clinical-realities.md`) is the one such reserved key that exists
today, written by engine-internal logic rather than any module — a
future vendored module whose own raw Synthea key happens to be
`donor` (none of the three surveyed modules use this key; noted as a
hypothetical the load-time check exists to catch, not an observed
collision) is rejected at load time, before any of its states ever
run. This is a cheap, mechanical check (compare a module's declared
write-set, scanned once from its JSON, against a small fixed list of
engine-reserved keys) — no schema-registry machinery is implied or
needed for it.

**No hidden modules, restated for attributes specifically:** lifecycle
behavior this project runs is explicit and listable
(`components/sim/docs/sim-theory.md`'s corollary, restated at the M5 roadmap entry) —
namespacing makes that listability concrete for state, not just for
execution: `grep`-ing `:attributes` for a module's own namespace
segment is how a reader finds everything that module ever wrote,
without reading its JSON.

## 6. Trajectory event shape

Restated here as the build session's own test obligations — each is
already a stated law on `components/sim/docs/sim-theory.edn`'s `:trajectory`/
`:compile` stages; this section exists so a future red-test author has
them in one place, phrased as obligations rather than as prose laws:

1. **Every trajectory event cites `{:module :state}`.** Glass-box
   traceability: any compiled IR step, and any rendered message
   downstream of it, must be auditable back to the exact module id and
   state name that produced it — a test asserting this citation exists
   on every trajectory event a run produces is the mechanical form of
   the law, not merely inspection.
2. **Every trajectory event's concept triplets are carried verbatim
   from the source module.** Code passthrough: the interpreter never
   invents, translates, or normalizes a `{:system :code :display}`
   value it reads from a module's own JSON — a property test comparing
   every trajectory event's concept fields against the literal values
   in the vendored module JSON they came from is this law's executable
   form.
3. **Every `CompileTrajectory`-produced IR step cites the trajectory
   event it realizes.** Provenance: `pathway-ir`'s own steps, once
   compiled from `clinical-trajectory`, must carry enough of a back-
   reference (at minimum, the `{:module :state}` citation from
   obligation 1, riding along through compilation) that a reader
   holding only a compiled IR step can still answer "which module state
   produced this," without needing the trajectory intermediate kept
   around separately.
4. **Clinical-content-preserving compilation.** Every trajectory event
   maps to at least one IR step (§1's table names which — some map to
   an annotation on an existing step rather than a new step of their
   own, which still counts; none are silently dropped); none is
   reordered against the clinical causality the module's own transition
   graph already encodes.

## 7. Implementation status (M5a, as built)

`RunModules` the *library* is `:built` as of Milestone M5a
(`ehrt.sim-trajectory.gmf` — the loader, §1/§5; `ehrt.sim-trajectory.gmf-
interpreter` — `step`/`walk-module`/`run-module`, §1–§3): every v1 state
type and all four transition kinds land exactly as specified above,
tested against `test/ehrt/sim/fixtures/fixture-clinic.json`
(ADR-0013 point 6's own hand-written fixture — placed there, not
`resources/modules/`, per that point's own reasoning: this project's
authored test content carries no NOTICE obligation and is not vendored
upstream data). `components/sim/docs/sim-theory.edn`'s own `:trajectory` stage stays
`:planned` regardless — see that file's own updated `:contract` note —
because a library existing is not the same as the pipeline actually
wiring persona → modules → trajectory inside a run; that wiring is
M5b's job.

Four implementation decisions this session made, each a concrete filling-
in of a design choice §1–§3 above left unspecified rather than a silent
divergence from anything ratified:

1. **A blocked Guard's own re-check mechanism.** Neither §2 nor §3 states
   *how* a Guard whose condition currently fails ever makes progress
   under the "no fixed tick" design — real Synthea's own answer (a global
   simulation tick re-checking every blocked module) is exactly the
   mechanism §3 rejects reproducing. This session's own resolution,
   `ehrt.sim-trajectory.gmf-interpreter/guard-step`: a failing `:age`
   condition with operator `>=` resolves ANALYTICALLY — the interpreter
   computes the exact virtual-clock advance (a `java.time.LocalDate`
   computation, zero rng draws) until the persona's age reaches the
   guard's own threshold, then proceeds — while a Guard failing on any
   other condition (a different operator, `:gender`, `:attribute`, or
   `:prior-state`) simply BLOCKS, the same way real Synthea's own
   Delay-then-Guard authoring idiom already requires a module author to
   route around a Guard that needs elapsed time to pass. This keeps
   "wait until old enough" Guards working without reintroducing a tick
   loop for every OTHER kind of block, at the cost of scoping the
   analytic resolution to the one condition kind (age) whose "how much
   longer" is a closed-form computation rather than an open-ended wait
   on some other actor.
2. **`Observation`'s own sampled value.** §1's table does not specify how
   (or whether) an `Observation` state's own value is sampled at the
   interpreter layer — a defensible reading is "M5b/`CompileTrajectory`'s
   concern." This session's own choice: when a state carries a `:range`,
   the interpreter samples one value uniformly within it (one rng draw,
   the same fixed-consumption law every other stochastic choice in this
   project follows) and carries it on the trajectory event; a state with
   no `:range` emits no `:value`. A future module needing something
   richer (a categorical/abnormal-tail distribution, `order-profiles`'
   own shape) is out of this session's scope, not precluded by it.
3. **`EncounterEnd`'s own reference is "the most recently opened
   Encounter for this module," not tracked open/closed.** Real GMF
   modules occasionally have more than one Encounter conceptually
   "pending" at once (the same gap `components/sim/docs/patient-state-model.md`'s own
   mining section already names for `VisitID`/pending encounters,
   pre-existing and unrelated to this session). v1's interpreter does
   not track which encounters are still open versus already closed — it
   simply finds the LAST `:encounter` event this module emitted, which
   is exactly correct for every module this session's own fixture and
   property tests exercise (encounters do not nest or overlap in v1's
   own state/transition scope) but would need real open/closed tracking
   before a future module with genuinely overlapping encounters could
   trust it.
4. **Virtual time is an interpreter-internal `epoch-day`
   (`java.time.LocalDate/toEpochDay`), not the engine's own seconds-
   from-run-start clock (ADR-0011).** M5a is engine-free by design (the
   roadmap's own M5a/M5b split) — mapping a module's own epoch-day
   virtual clock onto a real run's seconds-from-registration clock is
   exactly the kind of persona → modules → trajectory wiring M5b's own
   session does, not a gap in this one.

---

## 8. M5b findings: vendoring `sinusitis.json` against the REAL loader

Section 1's appendix recommendation (`sinusitis.json` as the first
module to vendor) was made from a SURVEY read of the module's JSON —
state types and condition types, read by eye, against v1's own scope as
this document defined it. M5b is the first session to actually run that
module through the real M5a loader and interpreter, and doing so
surfaced six gaps the survey did not (and structurally could not)
catch by reading alone, each resolved this session per this document's
own "extend v1 with a documented reason, or defer the module" standing
option (`docs/gmf-interpreter.md`'s own M5b task brief) -- the first
five surfaced by the loader/interpreter, the sixth by CompileTrajectory
itself (Task 3):

1. **The loader's all-or-nothing gate is stricter than the appendix's
   own prose imagined.** The appendix's recommendation reasoned that
   `sinusitis.json`'s `Device`/`DeviceEnd` gap was safe because it is
   "reachable by simply not compiling that one branch's terminal
   states" — but `ehrt.sim-trajectory.gmf/load-module` has no partial-
   compile mechanism at all: ANY deferred-type state anywhere in a
   module rejects the WHOLE module, by design (section 1's own
   docstring, ADR-0013 point 4). Resolved by extending v1: `Device`/
   `DeviceEnd` join as consumed-internally states, structurally
   identical to `Simple` (§1's table, above) — no trajectory event, no
   attribute write, ordinary transition resolution. This is the minimal
   treatment the appendix's own reasoning already justified; only the
   MECHANISM (a real state-type entry, not a load-time exemption) needed
   building.
2. **A real, previously-unexercised loader bug: a module with no
   top-level `:remarks` field failed schema validation.**
   `ehrt.sim-trajectory.gmf`'s own module constructor always assigned an
   explicit `:remarks` key (nil when absent from the source JSON); an
   `{:optional true}` schema key permits the key's ABSENCE, not an
   explicit nil value, so this fails `[:vector :string]` — never
   surfaced by M5a because the hand-written fixture always carries a
   top-level `:remarks` array. `sinusitis.json` has none (only
   PER-STATE remarks, a separate, already-supported field). Fixed: the
   loader now only assocs `:remarks` when the source module actually has
   one — a bug fix, not a v1-scope decision, but recorded here since it
   was this vendoring session's own discovery.
3. **A vendored module can carry a code value that isn't a JSON
   string.** `sinusitis.json`'s `Prescribe_Alternative_Antibiotic` state
   carries its RxNorm code as an unquoted JSON number
   (`"code": 1649987`), and `ehrt.sim.pathway/Concept` requires a
   string. Fixed at the normalization layer (`ehrt.sim-trajectory.gmf/
   normalize-code` now coerces `:code` to a string unconditionally) —
   the code's own digits pass through unchanged (code passthrough law
   unweakened), only their Clojure type changes, the same kind of
   representation normalization this loader already applies to every
   other GMF field (kebab-casing, keywordizing systems).
4. **The condition-vocabulary gap on `sinusitis.json` is on the
   module's MANDATORY path, not an excludable branch — bigger than the
   appendix's own survey characterized it.** The appendix's condition-
   vocabulary-gap note named `Penicillin_Allergy_Check`'s own `Active
   Allergy` condition (reached by only 20% of encounters, Doctor_Visit's
   own distributed split) as `sinusitis.json`'s share of the common,
   cross-module gap — implicitly contrasting it with `sore_throat.json`'s
   OWN gap, which sits on that module's mandatory path. What the survey
   missed: `Wait_for_condition_to_resolve` (reached by EVERY patient who
   completes ANY Doctor_Visit encounter, not merely the 20% who reach the
   allergy check) uses an `And` compound over `Active Medication`/`Active
   Condition` sub-conditions — entirely outside the original v1 four
   predicates. Left unresolved, this condition-vocabulary gap — not
   `Device`/`DeviceEnd` — would have been the real blocker: `evaluate-
   condition` throws `ex-info` for any unrecognized condition type
   (a programmer-error signal, not a result-not-throw outcome), and
   virtually every patient who ever onsets sinusitis over a realistic
   history-phase horizon reaches this state. Resolved by extending v1's
   condition vocabulary, per this document's own §2 prediction ("Active
   Allergy`/`Active Medication` are architecturally the same log-query
   mechanism `PriorState` already establishes"): `:active-condition`/
   `:active-medication` are now log queries over the trajectory-so-far
   ("does an onset event for this concept exist with no later end event
   referencing it"), the same index-based reference shape `ConditionEnd`/
   `MedicationEnd` already carry; `:and` is a recursive conjunction over
   its own `:conditions`. `Active Allergy` (used only by
   `Penicillin_Allergy_Check`, the excludable 20% branch) is NOT built
   the same log-query way — this project's `persona/Persona` schema has
   no allergy concept anywhere, so there is no onset event any query
   could ever find. It resolves to a documented, always-`false` constant
   instead: the conservative default (never wrongly blocks the module's
   own MANDATORY path, since the only place it's consulted is the
   excludable branch), recorded honestly as a real simplification rather
   than a silent gap.
5. **`sinusitis.json` has no `Terminal` state at all — confirmed a
   structural property the appendix's own survey table never checked.**
   Real Synthea modules of this shape run for a patient's entire
   lifetime under Synthea's own fixed-tick engine and are not authored
   to "finish" the way this project's hand-written fixture (purpose-
   built to demonstrate every v1 state type, including `Terminal`) does.
   `ehrt.sim-trajectory.gmf-interpreter/walk-module`/`run-module` loop until
   `:terminal?` or `:blocked?` (or, for `run-module`, an optional
   `horizon-end-t` bound); a module that never reaches Terminal and never
   blocks on a Guard (this module has none) would otherwise run until
   the `max-steps` runaway-loop backstop fires and THROWS, treating it as
   a module-authoring bug. This is resolved not by any interpreter change
   but by a now-load-bearing REQUIREMENT on M5b's own real engine wiring
   (Task 4): every real call to `run-module` against a vendored module
   MUST supply a bounded `horizon-end-t` (this project's own encounter-
   horizon scope, ADR-0007 point 3, makes this the correct choice
   anyway, not merely a workaround) — an UNBOUNDED `run-module` call
   is safe only for a module (like the hand-written fixture) known to
   reach Terminal or block. Recorded here so a future module vendored
   without this same property doesn't silently reproduce the risk.

6. **`ConditionEnd` can reference its own onset via `referenced_by_attribute`
   rather than a direct `condition_onset` state citation — harmless to
   the interpreter's own control flow, but leaves CompileTrajectory's own
   annotation codeless.** `sinusitis.json`'s `Sinusitis_Ends` state uses
   `referenced_by_attribute` (an attribute-tracked condition, not a fixed
   state name) — a reference shape v1's interpreter does not resolve, so
   the trajectory event's own `:references` (and therefore its `:codes`,
   `ehrt.sim-trajectory.compile-trajectory`'s own resolution path) comes back
   nil. This turned out NOT to break the interpreter's own walk for this
   module (every place this state's condition is actually EVALUATED
   happens before this specific end event ever fires, so the missing
   reference is never consulted at decide time) — but CompileTrajectory's
   own condition annotation for it is genuinely codeless. Resolved by
   making `ehrt.sim.pathway/ConditionAnnotation`'s own `:codes`
   field `{:optional true}` rather than required: a real annotation can
   legitimately carry no concept when its source event named none, the
   same "don't fabricate what was never actually said" discipline this
   whole session already applies to the vendored file's own idiosyncrasies.

None of these six findings changes `resources/modules/sinusitis.json`
itself — the vendored file on disk is byte-verbatim against upstream
(`resources/modules/NOTICE`'s own hash record); every resolution above
lives in the interpreter/loader (`ehrt.sim-trajectory.gmf`/`ehrt.sim.
gmf-interpreter`) or in how M5b's own engine wiring calls it, never in
the vendored data.

---

## Appendix: candidate-module survey

Three modules read in full at commit
`7e08387c68a7f0e21d13076609a159fd473fc902` (§ header). **A fourth
candidate, urinary tract infection, does not exist as its own module
in Synthea's current module set** — checked directly against the
directory listing at this commit (no `uti.json`, `cystitis.json`, or
similarly named file; GitHub's code-search API requires
authentication this session didn't have, so the check is a directory
listing, not a full-text search, but the directory listing is
authoritative for file *existence*, which is the question that
matters here). `bronchitis.json` was spot-checked as a substitute
while confirming this, and `sore_throat.json` was chosen as the third
formal candidate in UTI's place — both encounter-bearing, modest acute
illness modules in the same family the original brief's UTI suggestion
was reaching for.

### State-type expressibility

"Expressible" means the state's *type* is in §1's v1-or-recommended
list (the v1 table plus the `Symptom` recommendation); it does not by
itself mean every *condition* the state's transitions use is in §2's
four — condition-vocabulary gaps are called out separately below the
table, since they cut across all three modules rather than
distinguishing them.

| Module | Total states | Expressible (v1 + `Symptom`) | Not expressible | Gap detail |
|---|---:|---:|---:|---|
| `sinusitis.json` | 44 | 42 | 2 | `Device`/`DeviceEnd` (`Nebulizer`/`End Nebulizer`) — confined to the chronic-surgical tail, reachable only via the module's own low-probability chronic branch |
| `ear_infections.json` | 16 | 14 | 2 | `CallSubmodule` ×2 (`Ear_Infection_Prescribed_Antibiotic`, `Ear_Infection_Prescribed_OTC_Painkiller`) — structurally worse than the count suggests: **both** of the module's medication-prescribing branches route through a submodule, so the entire therapeutic content of this module is opaque to v1, not just 2 of 16 states |
| `sore_throat.json` | 44 | 44 | 0 | None — every state's *type* is in v1 (with the `Symptom` recommendation). Three states (`Determine_if_Bacterial`, `Pediatric_Allergy_Check`, `Adult_Allergy_Check`) use condition types outside v1's four predicates (below) — a condition-vocabulary gap, not a state-type gap |

### The condition-vocabulary gap, common to all three

`sinusitis.json` (`Penicillin_Allergy_Check`) and `sore_throat.json`
(`Pediatric_Allergy_Check`, `Adult_Allergy_Check`, both keyed on
Penicillin V) each use an **`Active Allergy`** condition; `sore_throat.json`'s
`Determine_if_Bacterial` additionally uses an **`At Least`**-N-of
compound wrapper (modified Centor criteria) over several sub-conditions.
Neither is in v1's four predicates (age, sex, attribute, `PriorState`,
§2). Worth recording as a near-term extension rather than a dead end:
**`Active Allergy`/`Active Medication` are architecturally the same
log-query mechanism `PriorState` already establishes** — "does an
onset event for concept X exist in this patient's log with no matching
end event before now" is the identical shape to `PriorState`'s own
"most recent target state, optionally within a window" query, just
keyed on a medication/allergy concept rather than a module state name.
Recommended as a natural v1.1 extension of §2's `PriorState` compilation
once a vendored module's own control flow actually needs it — not
built or scoped into v1 by this document, since neither formal recommendation
below strictly requires it (§ recommendation, next).

### Recommendation: vendor `sinusitis.json` first

**Ratified 2026-07-26 (item 4 of 8, this document's closing list,
below), per ADR-0013 point 4's own criterion.** `sinusitis.json` over `sore_throat.json` — despite
`sore_throat.json`'s clean 44/44 state-type score — because
`sinusitis.json`'s one gap (`Device`/`DeviceEnd`, 2 states) is
**structurally isolated**: confined to the module's rare chronic-
surgical tail, downstream of a low-probability branch
(`Chronic_Sinusitis_Continues`'s own distribution gives the
resolve/continue/worsen split, and only the worsen tail ever reaches
surgery), reachable by simply not compiling that one branch's terminal
states rather than by compromising anything on the module's acute-care
spine. `sore_throat.json`'s own gap — the `Active Allergy` condition on
its allergy-check states — sits **on the module's main diagnostic
branch** (every patient who reaches the doctor-visit encounter passes
through an allergy check before an antibiotic is selected), so an
interpreter without `Active Allergy` support would have to guess or
default that branch's outcome for every patient the module runs, not
just the rare chronic case `sinusitis.json` risks skipping. A gap
confined to an excludable tail is a smaller, easier-to-reason-about
risk for a *first* vendored fixture than a gap sitting on the common
path, even though it scores worse on a raw state-type percentage.
`sinusitis.json` is also the richest single fixture among the three —
`Procedure` (`Sinus_Surgery`), `MedicationOrder`/`MedicationEnd` (both
the primary and the allergy-alternate antibiotic), `ConditionOnset`/
`ConditionEnd` (three parallel onset paths: viral/bacterial/
inflammatory), and all four transition kinds all appear in one module,
exercising more of §1/§2's v1 surface in a single vendored file than
either alternative.

`ear_infections.json` is not recommended for v1 despite being the
smallest and best README-fit ("encounter-rich, modest state-type
surface," ADR-0013 point 4's own phrasing): its `CallSubmodule` gap
disqualifies its actual clinical content, not merely 2 of its 16
states, per the gap-detail note above — vendoring it in v1 would ship
a module whose two therapeutic branches both silently do nothing.

**Dated note, GMF coverage Wave B (2026-08-02, ADR-0027): superseded,
not reopened.** `CallSubmodule` support is exactly what Wave B built —
`ear_infections.json`'s own real closure (root plus its two called
submodules) surveyed clean of every Wave-D-scoped deferred type once
read in full (section 9, below), and is now vendored
(`resources/modules/NOTICE`'s own Wave B table rows). This entry's own
"silently does nothing" risk was the correct call AT THE TIME it was
written — no CallSubmodule support existed yet — left standing per this
document's own append-don't-erase convention, not silently corrected.

---

### M7 survey (this session, 2026-07-27): ten formal candidates plus an
extended scouting pass, spanning emergency/inpatient/ambulatory/
observation — one new module vendors

Read at the SAME pinned commit as the M5-prep survey above,
`7e08387c68a7f0e21d13076609a159fd473fc902` of
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea)
(`master`) — no re-fetch against a later `master`, per this project's
own "same commit, same evidence" discipline for a survey that revisits
a prior one. Ten candidates were read in full, chosen to span the axes
the current one-module set (`sinusitis.json` alone) can't exercise:
`sore_throat.json`, `urinary_tract_infections.json`,
`appendicitis.json`, `total_joint_replacement.json`,
`congestive_heart_failure.json`, `sepsis.json`,
`myocardial_infarction.json`, `stroke.json`, `self_harm.json`,
`gallstones.json`. A further ~16 modules were histogram-scouted
(state-type and condition-type counts only, the same lighter-weight
technique this document's own prior session used for `bronchitis.json`)
hunting specifically for a clean, `Observation`-bearing candidate once
the ten formal reads turned up none — see "The Observation-bearing gap"
below. **Total real modules inspected this session: 26.**

**Correction to this document's own prior claim.** The M5-prep
appendix (above) stated UTI "does not exist as its own module in
Synthea's current module set," checked via directory listing at this
same commit. That check was accurate for its own method (no
`uti.json` existed) but the conclusion was incomplete: at this SAME
commit, `urinary_tract_infections.json` exists (evidently added,
renamed, or the listing method missed it — not independently
resolved this session, since it doesn't change the practical
conclusion, below). Corrected here per this project's append-don't-
erase convention, not silently edited above.

#### State-type and condition-vocabulary expressibility

| Module | States | State-type gap | Condition-vocab gap | Encounter classes | Verdict |
|---|---:|---|---|---|---|
| `appendicitis.json` | 35 | none — all 35 states are v1 types | none — only `Gender`/`PriorState`, both in v1's original four | emergency → inpatient (ED admission, then transfer to an inpatient surgical encounter) | **Expressible, VENDORED this session** |
| `sore_throat.json` | 44 | none — all 44 states are v1 types (with the `Symptom` recommendation) | **`At Least`, `Symptom`, `Observation`** — an `At Least`-5-of compound on `Determine_if_Bacterial`, reached by EVERY patient who completes `Doctor_Visit` (not the excludable `Active Allergy` tail this document's own prior survey named as sore_throat's only gap) | ambulatory | State-type clean; blocked by a MANDATORY-path condition-vocabulary gap bigger than previously characterized — **RESOLVED and VENDORED, GMF coverage Wave A, 2026-08-02** (`.agents/plans/2026-08-02-gmf-coverage-plan.md`; `resources/modules/sore_throat.json`'s own NOTICE entry) |
| `urinary_tract_infections.json` | 29 | `CallSubmodule` ×3 | n/a (module has no `Encounter` state of its own) | none directly — delegates via `type_of_care_transition` (a FIFTH transition kind, outside this document's four, §2) to `uti/ambulatory_path`\|`ed_path`\|`telemed_path` submodules | Not encounter-bearing at its own top level AND `CallSubmodule`-blocked — **deferred** |
| `total_joint_replacement.json` | 31 | `CallSubmodule` ×4, `CarePlanStart`/`CarePlanEnd` ×1 each | none (`Age`/`And`/`Attribute`, all v1) | ambulatory (pre-op) → inpatient (surgery) → ambulatory (follow-up) | Rejected at LOAD (deferred types scattered through pre-op assessment AND post-op pain management AND post-op careplan — not an excludable tail) — **deferred**. **Dated note, GMF coverage Wave B (2026-08-02, ADR-0027): `CallSubmodule` is REMOVED from this module's own blocker list** — Wave B built the mechanism this row's own gap column names, though this module's OWN four call-paths were never fetched or characterized this session (unlike `ear_infections.json`/`urinary_tract_infections.json`, §9) — this row's own `CarePlanStart`/`CarePlanEnd` gap alone still blocks it, Wave D's own scope, and this module's real closure could still hide further gaps a real characterization pass would need to surface before any vendoring claim |
| `congestive_heart_failure.json` | 115 | `CallSubmodule` ×7, `Counter` ×5, `Death` ×4, `ImagingStudy` ×4, `DiagnosticReport` ×3, `CarePlanStart`/`CarePlanEnd` ×3+1, `MultiObservation` ×1 (28/115 ≈ 24% deferred) | `Vital Sign`, `Date`, `Or`, `Active CarePlan` — four more gaps | ambulatory ×3, emergency, hospice, inpatient ×2 | Far over ADR-0013 point 4's "modest surface" bar — **deferred, cited for prioritization data only** |
| `sepsis.json` | 37 | `MultiObservation` ×2, `DiagnosticReport` ×1, `Death` ×1 | none new (`Active Allergy`/`Age`/`Observation`, all recognized keywords — `Observation`-as-condition-type is itself the sore_throat-shared gap) | emergency | `DiagnosticReport` (`Blood_Cultures`) is the FIRST state after the encounter opens, unconditional; `MultiObservation` (`Record_Blood_Pressure`) fires on both the vasopressor and ICU-survival branches — both MANDATORY, not tails — deferred at M7 time. **VENDORED, GMF coverage Wave D stage D1 (2026-08-02, ADR-0029) — `resources/modules/NOTICE`'s own table; `Death` was already v1 by Wave C, `MultiObservation`/`DiagnosticReport` closed this wave, §11/§12 for the full account** |
| `myocardial_infarction.json` | 26 | `CallSubmodule` ×5, `Death` ×2, `CarePlanStart` ×1 | none | emergency | `ACS_Arrival_Meds`/`Cardiac_Labs`/`NSTEACS`/`STEMI` are all `CallSubmodule` and ALL reachable unconditionally past `ECG` — the module's entire post-ECG therapeutic content is opaque — **deferred**. **Dated note, GMF coverage Wave B (2026-08-02, ADR-0027): `CallSubmodule` is REMOVED from this module's own blocker list**, same caveat as `total_joint_replacement.json`'s own note above — the mechanism landed, but this module's own five call-paths were never fetched or characterized this session. `Death`/`CarePlanStart` still block it (Wave C/Wave D respectively) — matching the wave plan's own "B+C → MI" sequencing (`.agents/plans/2026-08-02-gmf-coverage-plan.md`), not reopened or accelerated by this note |
| `stroke.json` | 12 | `Death` ×1 (an excludable ~17.5% procedural-mortality tail — the ONLY sinusitis-precedent-shaped gap found this session) | **`Date`** — `Emergency_Encounter`'s own `conditional_transition` gates Clopidogrel/Alteplase on simulated year, evaluated immediately on encounter entry, for every patient — **condition-vocabulary gap RESOLVED, GMF coverage Wave A, 2026-08-02** (`:date` now v1, `.agents/plans/2026-08-02-gmf-coverage-plan.md`) | emergency | Smallest, cleanest STATE-type surface surveyed after appendicitis — the `Date` gap that blocked it is closed, but the `Death` state-type gap (this row's own second column) still does, per AR-6 (same plan): `Death` stays a load-bearing, semantically-real state (unlike `Device`, never a safe consumed-internally pass-through) — waits for Wave C's own `:expired`/post-mortem wiring — **deferred, revisit trigger: Wave C**. **Dated note, GMF coverage Wave C (2026-08-02, ADR-0028): `Death` is REMOVED from this module's own blocker list, but a NEW, worse gap replaces it — still deferred, revisit trigger changed.** `Death` itself landed this wave (section 10) and this module's own `Death` state (the `range`+`codes` form) is now fully expressible. But real-closure characterization (section 10's own C5 survey) found `Chance_of_Stroke`'s own `distributed_transition` gates the "Stroke" branch on `{"attribute": "stroke_risk", "default": 0}` — a real Synthea engine attribute (`CardiovascularDiseaseModule`'s own Framingham risk score) this project has no source for, whose own JSON default is exactly 0, making onset (and therefore `Death`) structurally unreachable if honored literally. Escalated and ruled (design channel, 2026-08-02): `stroke.json` stays deferred — revisit trigger is now an attribute-sourced `distributed_transition` weight mechanism (unbuilt) AND a stroke-risk-equivalent data source (out of this project's own persona model), both landing together, not scoped this session |
| `self_harm.json` | 35 | `Death` ×1 (excludable ~1.6–5.5% fatal-attempt tail), `CarePlanStart` ×1 (in the SECOND, ambulatory follow-up encounter — moot regardless, see "Multi-encounter" below) | none (`And`/`Attribute`/`Gender`/`Race`, all v1) | ambulatory ×2, emergency | Both deferred states are structurally isolated exactly like `sinusitis.json`'s own `Device`/`DeviceEnd` precedent — but the loader's all-or-nothing gate rejects on PRESENCE, not reachability, so isolation doesn't save it under the loader AS BUILT — **deferred, but the strongest evidence yet for a reachability-aware load gate (prioritization table, below)** |
| `gallstones.json` | ~50 | `CallSubmodule` ×2, `Death` ×2, `CarePlanStart`/`CarePlanEnd` ×1 each, `DiagnosticReport` ×2, `ImagingStudy` ×1, **`Physiology`** ×1 (a NEW deferred type — an ECG-waveform physiological model, nested `type: line`/`type: Attribute` chart config that is NOT itself a state-type gap, verified by direct inspection so it isn't miscounted) | `Race` (already v1) | ambulatory ×2, emergency | Six distinct deferred-type families in one module — **deferred, cited for prioritization data only** |

#### A second extended pass: a near-miss caught before commit; the Observation-bearing hunt continues, still empty

Fifteen more modules were histogram-scouted after the seam checkpoint
(author direction: spend more session budget before finalizing the
vendor set), bringing the session total to **41 real modules
inspected** — very close to half of Synthea's own 85-module catalog
(`notes/facts-register.md` F2). Two were read in full on promising
histograms (zero or one deferred-type state):

- **`spina_bifida.json` — a real, caught-before-commit self-correction.**
  First characterized (below, in this subsection's own initial draft)
  as clean and vendored: 39 states, all v1 types EXCEPT one `Death`,
  condition vocabulary `Age`/`And` only (both v1), its `Encounter_NICU`
  (`:inpatient`) reached DIRECTLY off `Myelomeningocele`'s own onset —
  no preceding encounter, so immune to the multi-encounter truncation
  `appendicitis.json` hits. **That characterization was wrong in exactly
  the way this document's own established rule already warns against:**
  `Death` fires on genuinely rare, realistic tails here too (6.1%
  day-1-survival, 2% post-op, 1% under-age-5, 0.5% living-with-SB) —
  but `Death` is NOT `Device`/`DeviceEnd` (which M5b actually promoted
  into v1's consumed-internally set); it remains a `gmf-type->keyword`-
  unrecognized, load-REJECTED type, same as `CallSubmodule`/
  `CarePlanStart` — the loader's all-or-nothing gate does not
  distinguish "isolated tail" from "mandatory," it rejects on bare
  PRESENCE, and this document's own §1 table and every other Death-
  bearing module's own row above (`stroke`, `sepsis`,
  `myocardial_infarction`, `self_harm`, `gallstones`,
  `congestive_heart_failure`) already say so. Attempting to vendor it
  test-first (Task 2's own red-first discipline) caught this
  immediately: `gmf/load-module` returns `:rejected
  :unsupported-state-type {:state :death ...}`, confirmed empirically,
  before any test or file made it into a commit. **Corrected here,
  not silently — `spina_bifida.json` is DEFERRED, not vendored**, and
  the mistake itself is left visible rather than rewritten away, per
  this project's own append-don't-erase convention for a survey
  correcting itself. This sharpens rather than weakens the `Death` row
  in the prioritization table, below: EVERY module this session found
  with a `Death` state found it on a genuinely excludable tail, never
  once on a mandatory path, across all 41 modules read — the strongest,
  most consistent evidence in this entire survey for a specific,
  cheap v1.1 extension (promoting `Death` to a consumed-internally
  state exactly the way `Device`/`DeviceEnd` already were, wired to
  the existing `:expired` machinery per `components/sim/docs/clinical-realities.md`'s
  post-mortem entry) — `spina_bifida.json` itself becomes the FIRST
  module ready to vendor the moment that lands.
- **`epilepsy.json` — clean but for the wellness-encoding gap, DEFERRED.**
  16 states, all v1 types, condition vocabulary `Gender`/`PriorState`
  only (both v1) — its real emergency-encounter content
  (`Seizure_Encounter`) is fully expressible, but its SECOND encounter
  (`Medicine_Encounter`, an ongoing-medication follow-up) uses the SAME
  `"wellness": true` boolean idiom `osteoporosis.json`/`mTBI.json`/
  `atrial_fibrillation.json` already exhibited — now FOUR confirmed
  instances, plus `med_rec.json` (histogram-scouted, six states, every
  one a v1 type, blocked ONLY by this same idiom) makes FIVE. This is
  no longer an edge case; see the prioritization table's revised count,
  below.
- **`hypothyroidism.json` — clean but for one `CallSubmodule`, DEFERRED.**
  A THIRD confirmed instance (after `self_harm.json`) of a module whose
  entire encounter-bearing content is v1-expressible except for one
  `CallSubmodule` state (`Anemia_Submodule`) — reached only when
  `Check_Anemia_Exist`'s own `Attribute`/`is nil` guard passes, which it
  always would in this project (no module or persona field ever sets an
  `anemia` attribute) — real evidence, not hypothetical, for the
  reachability-aware load-gate recommendation the prioritization table
  already names.

Thirteen more (`asthma`, `copd`, `cystic_fibrosis`, `pregnancy`,
`cerebral_palsy`, `anemia___unknown_etiology`, `home_health_treatment`,
`home_hospice_snf`, `veteran_hyperlipidemia`, `kidney_transplant`,
`diabetic_retinopathy_treatment`, `breast_cancer`,
`bone_marrow_transplant`) were histogram-scouted only and confirmed
blocked by the same recurring families (`CallSubmodule`/`CarePlanStart`/
`Counter`/`DiagnosticReport`/`Death` combinations, several with 2+ at
once) — no full read needed given the established gap vocabulary
already accounts for what's visible. `home_health_treatment.json` adds
ONE more new finding worth naming: it uses `encounter_class` values
`"home"` and `"urgentcare"`, TWO MORE classes outside this document's
own four (§4) — cited here for completeness, not pursued further (the
module is independently `Counter`-blocked).

**The Observation-bearing hunt is now closed for this session, not
because of insufficient search but because of consistent, repeated
evidence.** Every Observation-bearing module found across 41 real
modules read — `sore_throat`, `sepsis`, `osteoporosis`,
`hypothyroidism`, `myocardial_infarction`, `wellness_encounters`,
`veteran_ptsd`, `breast_cancer`, `pregnancy`, `copd` — is blocked by
something else. This is recorded as the session's real, well-evidenced
finding, not an artifact of limited budget: Task 3's demo proceeds
without a real module-sourced OBX (below).

#### The Observation-bearing gap: no vendorable candidate found

Every module read this session that fires an `Observation` state
(`sore_throat.json`, `sepsis.json`, `osteoporosis.json`,
`myocardial_infarction.json` among the histogram-scouted set,
`opioid_addiction.json`, `hypertension.json`) is blocked by something
else — most tellingly, `osteoporosis.json` (histogram-scouted: 18
states, ALL v1 types including exactly one `Observation`
(`Bone_Density`, a DXA bone-density LOINC panel), zero `CallSubmodule`/
`Death`/`CarePlanStart`/`Counter`/`MultiObservation` — the cleanest
STATE-type surface found this session after `appendicitis.json`) is
rejected not by a state-type or condition gap at all but by a THIRD,
newly-discovered format issue:

**A second GMF wellness-encounter encoding this loader doesn't
recognize.** `docs/gmf-interpreter.md`'s (this document's) own §1/§4
assume every `Encounter` state names its class via a string field,
`"encounter_class": "wellness"` — the encoding `sinusitis.json`/
`sore_throat.json`/`ear_infections.json` all use, and the only one
`ehrt.sim-trajectory.gmf`'s `GmfState` schema accepts (`:encounter-class`
is a REQUIRED key for the `:encounter` variant, no `{:optional true}`).
`osteoporosis.json`'s `Wellness_Encounter` state instead carries a bare
`"wellness": true` boolean and NO `encounter_class` key at all — this
is confirmed a real, recurring upstream idiom, not a one-off: the SAME
`wellness: true` shape appears in `mTBI.json` and
`atrial_fibrillation.json` (both histogram-scouted this session), three
independent modules. A module using it fails `ehrt.sim-trajectory.gmf`'s
schema validation outright (`:schema-invalid`) — a DIFFERENT rejection
category than `:unsupported-state-type`, surfacing only once a real
`Encounter` state is checked against the v1 schema, not caught by the
state-type gate at all. Even setting that aside, `osteoporosis.json`'s
own `Consider_Medication` state gates `Prescribe_Bisphosphate` on a
mandatory `Date` condition (year ≥ 1995) — the SAME gap `stroke.json`
and `atrial_fibrillation.json` (below) independently exhibit — so
fixing the encoding gap alone would not unblock it either. **Net
result: this session found no module that is genuinely vendorable AND
carries an `Observation` state; Task 3's demo cannot show a real
module-sourced OBX this session** (recorded as an unmet goal, not
papered over — see Task 3, below, and the prioritization table's own
`Date`/wellness-encoding rows).

`atrial_fibrillation.json` (histogram-scouted: 11 states, ALL v1 types
including `Device` — otherwise as clean as `appendicitis.json`) is a
second independent instance of BOTH new gaps at once (`wellness: true`
encoding on `Next Wellness Visit`, AND a mandatory `Date` condition on
that same state gating Verapamil/Warfarin by year) — cited here because
two unrelated cardiology-family modules hitting the identical pair of
gaps is evidence this is a systemic idiom, not a fluke of one module's
authoring style.

#### Multi-encounter-per-episode: a compile-time gap, not a state-type or condition gap

**A new, load-bearing finding, verified by reading
`ehrt.sim-trajectory.compile-trajectory` directly (`compile-trajectory`,
lines ~194–262), not merely inferred from the JSON.** M5b's own
`encounter-closed?` mechanism (docs/gmf-interpreter.md §8, "M5b
findings," item 7) was built to stop `sinusitis.json`'s
`Potential_Onset` loop from minting a second admission for an
already-discharged patient-id when a module recurs across a patient's
WHOLE LIFE. Its actual implementation is coarser than that one case:
`encounter-closed?` is a single boolean that, once set by the FIRST
`:encounter-end` event, drops EVERY subsequent trajectory event —
including a SECOND `:encounter` immediately following, within the SAME
clinical episode, not a later lifetime recurrence.

`appendicitis.json` is the first vendorable module to actually exercise
this: `Appendicitis_Encounter` (`:emergency`) opens, `Transfer_To_Inpatient`
(`:encounter-end`, zero elapsed simulated time later) closes it and sets
`encounter-closed?` — after which `Appendectomy_Encounter` (`:inpatient`),
the `Appendectomy` procedure itself, and the recovery delay are ALL
silently dropped by `compile-trajectory`, never reaching
`ehrt.sim.pathway` IR. This is confirmed empirically in Task 2,
below (a real compiled-step assertion), not just reasoned about here.
`total_joint_replacement.json` exhibits the identical shape (ambulatory
pre-op encounter closes BEFORE the real inpatient surgical encounter
opens) — moot for that module since it's independently load-rejected,
but a second confirming data point that this is the NORM for staged
surgical/transfer content, not appendicitis's own quirk.

**This does not violate the clinical-content-preserving compilation law
(§6 obligation 4)** — the underlying (uncompiled) `clinical-trajectory`
still carries every event in full, and the drop is the SAME "real, not
worth compiling, once we're past this run's own scope" shape
`compile-trajectory`'s own docstring already establishes for pre-horizon
drops. But it means a vendored module's real inpatient (or any
post-transfer) content can be invisible in practice even when every
state and condition it uses is fully v1-expressible — a gap orthogonal
to the named deferred STATE types, worth its own row in the
prioritization table below. `ehrt.sim.pathway` already has a
`:transfer` step (ADT^A02, ward-to-ward) that is architecturally the
right primitive for "encounter-end immediately followed by a
same-episode encounter of a different class" — named here as the
shape a future extension would most plausibly take, not designed or
built this session.

#### Prioritization table: which deferred feature blocks the most content

**Superseded, 2026-08-03 (ADR-0034 — the GMF census tool, `.agents/
plans/2026-08-02-gmf-parity-plan.md` §3).** This table was built by
hand-reading a scouted sample (26–41 of 85 modules, depending on the
row) — exactly the survey method the parity plan's own §3 named as
twice-overturned by fetched evidence (`urinary_tract_infections.json`'s
"×3 closure" was really 12 files; `myocardial_infarction.json`'s was
27). §15, below, is the mechanical census that walks and digests the
FULL catalog at this document's own pin: it is now the frontier of
record. This table stays as read (not deleted — a real, dated snapshot
of what a hand survey found and got partly wrong, itself useful
provenance), but any future prioritization call reads §15 first.

Counting across all 26 modules inspected this session (10 formal reads
+ 16 histogram-scouted) plus the two modules this project already had
evidence for (`ear_infections.json`, `sinusitis.json` itself):

| Deferred feature | Modules blocked (of 28 total ever inspected) | Content class it would unlock |
|---|---:|---|
| `CallSubmodule` | 20 — `ear_infections`, `urinary_tract_infections`, `total_joint_replacement`, `myocardial_infarction`, `dermatitis`, `allergic_rhinitis`, `food_allergies`, `contraceptive_maintenance`, `dialysis`, `hiv_diagnosis`, `hypertension`, `osteoarthritis`, `covid19`, `stable_ischemic_heart_disease`, `lupus`, `allergies`, `lung_cancer`, `colorectal_cancer`, `hypothyroidism`, `diabetic_retinopathy_treatment`, `breast_cancer`, `cystic_fibrosis`, `anemia___unknown_etiology`, `home_hospice_snf` (24, corrected count — see note) | By far the largest single blocker: shared medication-regimen and referral submodules (`medications/*`, `heart/*`, `dme/*`, `total_joint_replacement/*`, `anemia/*`) are how modern Synthea authors factor out repeated therapeutic content — this is THE headline finding, not a tie. `hypothyroidism.json` is the SECOND confirmed instance (after `self_harm.json`) of a module blocked by exactly ONE otherwise-unreachable-by-default `CallSubmodule` — real, repeated evidence for a reachability-aware load gate as a cheap, high-value v1.1 extension. **Dated note, GMF coverage Wave B (2026-08-02, ADR-0027): the MECHANISM this row names is now built** (`ehrt.sim-trajectory.gmf/load-closure`, `ehrt.sim-trajectory.gmf-interpreter`'s own call/return, §9) — `ear_infections.json` is REMOVED from this list, vendored (`resources/modules/NOTICE`). `urinary_tract_infections.json` stays on it, but for a DIFFERENT reason now: its own real closure (twelve files, not the four this table's own count assumed) is dirty with `DiagnosticReport`/`MultiObservation`, both Wave D's own scope (§9's own full account) — `CallSubmodule` itself is no longer what blocks it. Every other module on this list was NOT re-characterized this session; each still needs its own real closure read before any vendoring claim, the same caveat this document's own MI/`total_joint_replacement.json` rows (above) now carry |
| `CarePlanStart`/`CarePlanEnd` | 11 — `total_joint_replacement`, `congestive_heart_failure`, `dermatitis`, `food_allergies`, `myocardial_infarction`, `attention_deficit_disorder`, `gout`, `fibromyalgia`, `self_harm`, `dementia`, `lung_cancer`, `lupus`, `veteran_ptsd` (13, see note) | Structured chronic-disease/post-procedure care-management plans (physical therapy, psychiatric follow-up, home health) — almost always paired with `CallSubmodule` in the same module |
| `Death` | 12+ confirmed — `congestive_heart_failure`, `sepsis`, `myocardial_infarction`, `stroke`, `self_harm`, `gallstones`, `epilepsy`, `spina_bifida`, `cystic_fibrosis`, `breast_cancer`, plus several histogram-only hits (`chronic_kidney_disease`, `hiv_diagnosis`, `stable_ischemic_heart_disease`, `colorectal_cancer`, `lung_cancer`) | **The single strongest, most consistent finding in this table.** Every one of the 12+ modules above has its `Death` state on a genuinely excludable, low-probability tail — NEVER once on a mandatory path, across all 41 modules this session read at any depth. `spina_bifida.json` (above) is the concrete, empirically-confirmed proof: a module this session first mis-characterized as vendorable specifically BECAUSE its `Death` state looked safely isolated — it IS safely isolated, the loader's all-or-nothing gate is what still blocks it. Promoting `Death` to a `Device`/`DeviceEnd`-style consumed-internally state (wired to the existing `:expired` machinery, `components/sim/docs/clinical-realities.md`'s post-mortem entry) is, on this session's own evidence, the cheapest, highest-confidence, most immediately-productive v1.1 extension in this entire table — `spina_bifida.json` alone is ready to vendor the day it lands |
| **Wellness-encounter `wellness: true` encoding** (new this session) | 5 confirmed (`mTBI`, `atrial_fibrillation`, `osteoporosis`, `epilepsy`, `med_rec`) of a ~41-module scouted sample — likely still under-counted, not systematically checked across all 85 | ~~Blocks an entire v1 ENCOUNTER CLASS (`:wellness`) via this idiom specifically — the cheapest fix in this table (a loader normalization, not new interpreter machinery); `epilepsy.json` and `med_rec.json` are otherwise FULLY clean, making this the single highest-confidence "would vendor immediately if fixed" row in this table~~ **OVERTURNED (2026-08-03, `notes/ADRs.md` ADR-0031 AR-5).** A live probe against Synthea source at this document's own pin (`7e08387c68a7f0e21d13076609a159fd473fc902`, `State.java`'s `Encounter.process` wellness branch) found `wellness: true` creates NOTHING and BLOCKS until the engine's hardcoded `EncounterModule` opens its own next separately-scheduled wellness encounter — not a loader-normalization gap at all, but the absence of a synthesized wellness CYCLE this sim has no equivalent for. The "cheapest fix" characterization above is RETIRED; the five modules move into Wave G's own unlock ledger (`.agents/plans/2026-08-02-gmf-parity-plan.md`), gated on the wellness-cycle design ADR-0031 AR-2 rules |
| **Mandatory-path `Date` condition** (new this session) | 3 confirmed (`stroke`, `atrial_fibrillation`, `osteoporosis`), 1 more histogram-only (`attention_deficit_disorder`) | Calendar-year-gated treatment-protocol logic (older vs. newer drug availability) — narrow, mechanical (a numeric year comparison against `sim-config`'s own `reference-date`), likely the SECOND-cheapest fix in this table |
| `MultiObservation` | 3 — `congestive_heart_failure`, `sepsis`, `wellness_encounters` | Combined multi-panel vitals reads (blood pressure systolic+diastolic in one panel) — clusters with `DiagnosticReport` |
| `DiagnosticReport` | 5 — `congestive_heart_failure`, `sepsis`, `gallstones`, `wellness_encounters`, `dialysis`, `lung_cancer`, `colorectal_cancer` (7, see note) | Structured lab-panel reporting (blood cultures, imaging read-outs) |
| `Counter` | 3 — `congestive_heart_failure`, `dental_and_oral_examination`, `homelessness`, `hypertension`, `lung_cancer`, `colorectal_cancer` (6, see note) | Named in this document's original brief as deferred; this session's evidence is the first confirming it's genuinely used, not just theoretically possible |
| `ImagingStudy` | 4 — `congestive_heart_failure`, `gallstones`, `dental_and_oral_examination`, `lung_cancer` | Radiology-order/result content |
| **`At Least`/`Symptom`-condition/`Observation`-condition compound** (new this session, sharper than the prior document's `Active Allergy`-only framing) — **BUILT, GMF coverage Wave A, 2026-08-02** (`.agents/plans/2026-08-02-gmf-coverage-plan.md`; `sore_throat.json` vendored the same session, section 8's own successor findings section names any further gaps) | 1 confirmed on a MANDATORY path (`sore_throat.json`'s `Determine_if_Bacterial`, the modified Centor-criteria gate) | Unlocked `sore_throat.json` outright, exactly as predicted — the single highest-value TARGETED fix in this table |
| `Physiology` | 1 — `gallstones` | Waveform/physiological simulation (ECG traces) — clearly out of scope for a long time, cited only for completeness |
| **Multi-encounter-per-episode compile-time truncation** (new this session, a `compile-trajectory` gap, not a loader gap) | 2 confirmed content-relevant (`appendicitis`, `total_joint_replacement`), likely affects most staged-care/surgical modules | Same-episode transfers (ED→inpatient, ambulatory pre-op→inpatient→ambulatory follow-up) — the SPECIFIC gap standing between this session's own vendored `appendicitis.json` and a demo that shows its actual surgery |

*Note on the `CallSubmodule`/`CarePlanStart`/`Death`/`DiagnosticReport`/
`Counter` row counts: the FIRST number in each cell is this session's
own conservative count from the 10 formally-read modules plus
`ear_infections.json`; the parenthetical, where present, is the fuller
count including the 16 histogram-scouted modules, listed for
transparency but not double-verified by a full read the way the
formal ten were — treat the parenthetical as directional, not as
precise as the formal-ten figures.*

**Dated note, GMF coverage Wave D stage D2 (2026-08-02, ADR-0029, §13
has the full account): both `myocardial_infarction.json` and
`total_joint_replacement.json` real closures now fetched and read in
full, undercounted by this table the same way `urinary_tract_infections.json`
already was.** `myocardial_infarction.json`'s real closure is 27 files
(not the single-file top-level count this table's own histogram-scout
implied) and is dirty with THREE separate deferred/out-of-scope
findings at once — `lookup_table_transition` (D3), `ImagingStudy` (R5),
and a genuinely NEW state type this document has never named,
`SupplyList` — deferred, not vendored. `total_joint_replacement.json`'s
real closure is only 4 files and surveys CLEAN of every Wave-D-scoped
type except `CarePlanStart`/`CarePlanEnd` itself — its own
`joint_replacement` attribute gap was resolved this session (a small,
disclosed `run-module` extension, §13), but a SECOND, independent
blocker surfaced testing that fix against the real closure: a compound
`Joint_Replacement_Guard` (`Age > 50` AND'd with an attribute check)
this interpreter's own `age-guard-jump-days` cannot analytically
resolve (bare `:age >= N years` only) — the walk blocks permanently at
age 0, confirmed empirically. **NOT vendored this session either** —
named as this closure's own next prerequisite (§13's own fix-forward
finding), unowned until a future session extends Guard's own
condition-resolution machinery. Both rows' own `CarePlanStart`/
`CarePlanEnd` cell entries stay accurate as prioritization data; the
CarePlan MECHANISM itself is real, built, and tested regardless (§13),
awaiting a clean closure to prove it against.

**Dated note, GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, §14
has the full account): both `urinary_tract_infections.json` and
`total_joint_replacement.json` are now VENDORED (`resources/modules/
NOTICE`'s own new rows), closing this table's own last two open Wave D
payoff rows.** `urinary_tract_infections.json` is REMOVED from the
`CallSubmodule` row above — its own real blocker (`DiagnosticReport`/
`MultiObservation`, D6) is now v1 (Wave D stage D1), and the genuinely
new sixth transition kind this table's own D2 note first flagged,
`lookup_table_transition`, is now built (H2) and no longer names an
unowned gap; the two lookup tables it needs (`uti.csv`/
`uti_recurrence.csv`) are this project's own first closure DATA-FILE
members (R4). `total_joint_replacement.json` is REMOVED from the
`CarePlanStart`/`CarePlanEnd` row above — its own compound-Guard
blocker (D2's own fix-forward finding) is resolved (H4:
`age-guard-jump-days` extended under a sound-jump-or-escalate rule).
`myocardial_infarction.json` stays deferred (its own three independent
blockers — `lookup_table_transition` landing does not by itself unblock
it, since `ImagingStudy`/R5 and the genuinely-new `SupplyList` state
type are still unowned). `stroke.json` stays deferred too: D3 also
lands the attribute-weighted `distributed_transition` mechanism (H3,
`stroke.json`'s own `Chance_of_Stroke` shape byte-confirmed against
source) but this does NOT unblock stroke — `stroke_risk` stays
SPECIFIED, unsourceable content (§10's own dated note, unchanged);
Wave D's own three named stages (D1/D2/D3) are now all closed.

**Headline: `CallSubmodule` blocks more real content than every other
deferred feature combined**, confirming and sharpening
`docs/gmf-interpreter.md`'s own prior finding on `ear_infections.json`
("not merely 2 of 16 states... the entire therapeutic content") — this
session's evidence is that this is the NORM for Synthea's current
module library, not `ear_infections.json`'s own quirk. The
CallSubmodule/Death/Counter/MultiObservation roadmap lines
(`.agents/plans/roadmap.md`'s deferred ledger) should read this table
before any future prioritization call: `CallSubmodule` support would
unlock roughly 2–3× as many modules as any other single deferred
feature, `Death` is the safest to wire (its own instances are
consistently excludable tails, never mandatory, across every module
this session read in full), and the two NEW findings this session
adds (`wellness: true` encoding, mandatory `Date` conditions) are
cheaper, narrower fixes than any named GMF state type — genuinely
"quick wins" once someone is looking at the loader/interpreter again,
each independently worth more vendored content per line of code than
`CallSubmodule` support would cost to build.

#### Recommendation: vendor `appendicitis.json`; defer the rest

**Ratified by this session's own execution, not merely proposed** (Task
2 is `appendicitis.json` vendored test-first against the REAL
loader/interpreter/compiler, per this document's own M5b-established
"survey proposes, the real loader/compiler disposes" discipline — and,
this session, that same discipline caught its OWN survey being wrong
about a second module, `spina_bifida.json`, before that mistake ever
reached a commit; see that module's own corrected write-up above).
Of 41 modules read this session (10 formal + 1 extended-pass formal +
14 more histogram-scouted in two rounds), exactly ONE clears the full
bar — zero state-type gap AND zero condition-vocabulary gap, confirmed
against the real loader, not just read by eye: `appendicitis.json`, a
cleaner survey score than `sinusitis.json` itself needed at its own
M5a/M5b vendoring (which required the M5b condition-vocabulary and
`Device`/`DeviceEnd` extensions `appendicitis.json` needs neither of).

Every other candidate this session read is deferred, each for a
reason tabled above — none is a coin flip; each hits either a
mandatory-path state-type gap (`total_joint_replacement`,
`congestive_heart_failure`, `sepsis`, `myocardial_infarction`,
`gallstones`, `hypothyroidism`), a mandatory-path condition-vocabulary
gap (`sore_throat`, `stroke`), a non-encounter-bearing structural
disqualification (`urinary_tract_infections`), the `Death`-blocks-
otherwise-clean-modules pattern (`spina_bifida`, and a real near-miss
this session specifically caught itself getting wrong about), or the
newly-discovered wellness-encoding/`Date` pair (`osteoporosis`,
`atrial_fibrillation`, `mTBI`, `epilepsy`, `med_rec`). The vendored set
after this session is `{sinusitis, appendicitis}` — two modules, well
under ADR-0013 point 5's ~10-module lockfile-revisit threshold, and
under this session's own "~4–6" target even after author-directed
extended scouting nearly doubled the modules read (26 → 41). **This is
reported as the session's real result, not under-delivered against
quietly**: the target assumed a hit rate Synthea's actual current
module library does not support at this project's own real curation
bar. The prioritization table above is the concrete evidence for why,
and names exactly which future extensions would change the count —
promoting `Death` to a consumed-internally state (unlocking
`spina_bifida.json` immediately, on this session's own evidence) and
the `wellness: true` encoding fix (unlocking `epilepsy.json`/
`med_rec.json` immediately) are, on this session's own evidence, the
two cheapest, highest-confidence next moves — each backed by a named,
already-identified, ready-to-revisit module the moment it lands.

**Dated note, GMF coverage Wave A (2026-08-02,
`.agents/plans/2026-08-02-gmf-coverage-plan.md`): the vendored set above
("`{sinusitis, appendicitis}`") is this document's own historical M7
result, superseded but left standing per the append-don't-erase
convention this note itself follows.** Wave A closed the `At Least`/
`Symptom`-condition/`Observation`-condition gap this table's own row
named as the single highest-value targeted fix, and vendored
`sore_throat.json` the same session — the vendored set is now
`{sinusitis, appendicitis, sore_throat}`, three modules
(`resources/modules/NOTICE`'s own table). `stroke.json`'s own `Date` gap
is ALSO now resolved at the interpreter level, but that module stays
deferred: its `Death` state-type gap (this table's own `Death` row)
still blocks the loader's all-or-nothing gate, per AR-6 — Wave C's own
trigger, not reopened here.

---

## Ratification record

**All eight author-review items from this document are ACCEPTED,
2026-07-26** (author-ratified ahead of M5a's build session, per this
roadmap's own M5a-opens-by-confirming-these convention,
`.agents/plans/roadmap.md`'s M5 entry):

1. `Symptom` joins v1 as a write-only, consumed-internally state (§1).
2. History-phase fast-forward: no fixed tick — reuses the exact
   per-state transition-sampling logic the horizon phase uses (§3,
   sub-question 1).
3. Pre-horizon facts enter `ground-truth-log` marked `:pre-horizon
   true`, never trimmed (§3, sub-question 2).
4. `sinusitis.json` is the first module recommended to vendor, M5b
   (Appendix).
5. `:outpatient-visit`'s `decide` calls no allocation ladder; `:status`
   `:new -> :admitted`, `:class -> :outpatient` (§4 sketch).
6. `:outpatient-visit`'s `:location` stays deliberately `nil` for the
   visit's duration — a named, narrowly-gated exception to
   `components/sim/docs/patient-state-model.md`'s "never nil-bed" rule for
   `:location`. **This item does not stay prose-only:** it is scheduled
   to land, M5b, as a genuine conditional row in that document's
   event-validity table (mechanism: status × event-class ×
   attribute-conditions — the same shape the table's post-mortem/donor
   rows already use for a gated exception, not a special-cased sentence
   bolted on beside the table), per this session's own annotation at
   item 6's own place in §4, above (§4 sketch).
7. `:outpatient-visit-end`'s `:status` `:admitted -> :discharged`; no
   `message-type-registry` entry, by design, the same precedent
   ADR-0012 already established for `:step-rejected` (§4 sketch).
8. New invariants implied by items 5–7:
   `outpatient-visit-only-when-new`, `outpatient-patients-occupy-no-bed`,
   and the occupancy board's inpatient/ED-scoped consistency-law
   qualifier (§4 sketch).

None of the eight requires code or resource changes to *this* session
(M5a) — items 1–3 are interpreter-core design this session's own build
implements directly (Tasks 1–3, below the roadmap's M5a scope); items
4–8 are M5b scope (the vendored module, the outpatient step-type pair,
and item 6's validity-table row specifically), recorded here as
ratified so M5b's own session opens against decided design, not a
still-open recommendation.

---

## 9. GMF coverage Wave B: closure survey and characterization findings (2026-08-02)

Step 1 of the Wave B session (ADR-0027; `.agents/plans/2026-08-02-gmf-
coverage-plan.md`) — real closures fetched and read at the SAME pinned
commit every prior GMF citation in this document uses,
`7e08387c68a7f0e21d13076609a159fd473fc902` of
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea)
(`master`). D6's own bar ("modest deferred-type surface" per ADR-0013
point 4) applies to each candidate closure as a unit; D7's hidden-
import check is run per closure; D5's `type_of_care_transition`
dispatch rule is characterized against real Synthea source, below —
this section is the fix-forward fill-in ADR-0027's own D5 placeholder
points to.

### `ear_infections.json` closure survey — VENDORED this session

| Module | States | State-type gap | Condition-vocab gap | Other findings |
|---|---:|---|---|---|
| `ear_infections.json` (root) | 16 | `CallSubmodule` ×2 (Wave B's own headline target) | none | mandatory-path `Attribute` `is not nil` operator (`End_Ear_Infection_Medications`); mandatory-path `encounter_class: "outpatient"` (`Ear_Infection_Encounter`, the module's OWN primary encounter); mandatory-path `wellness: true` boolean idiom with no `codes` key (`Next_Wellness_Encounter`, the module's own unconditional post-medication fallback) |
| `medications/ear_infection_antibiotic.json` | 19 | none | none (`Age`/`Date`/`Active Allergy`, all v1) | mandatory-path `Attribute` `is nil` operator (`Initial`, an idempotency gate); every `MedicationOrder` (11 of them) carries `assign_to_attribute: antibiotic_prescription` |
| `medications/otc_pain_reliever.json` | 12 | none | none (`Age`/`Date`/`Active Allergy`, all v1) | same shape as the antibiotic submodule: mandatory `is nil` gate on `Initial`, every `MedicationOrder` (6 of them) carries `assign_to_attribute: otc_pain_reliever` |

**Verdict: closure surveys clean of Wave-D-scoped deferred types** (no
`DiagnosticReport`/`MultiObservation`/`CarePlanStart`/`ImagingStudy`
anywhere in the closure) — the ONLY state-type gap is `CallSubmodule`
itself, Wave B's own reason for existing. Four real, previously-
uncharacterized findings surfaced by reading the actual closure rather
than the top-level module alone (the same "survey proposes, the real
loader disposes" pattern section 8's own M5b findings already
established, repeated here for a NEW module):

1. **`MedicationOrder`'s own `assign_to_attribute` field, and
   `MedicationEnd`'s own `referenced_by_attribute` field, are both
   unbuilt.** `GmfState`'s `:medication-order` schema variant carries
   no attribute-assignment field at all (a module using it still loads
   — Malli's `:map` is open by default, so the extra key is silently
   ignored — but the interpreter never WRITES the attribute, so a
   downstream `is not nil` check on it would always read nil). This is
   the SAME reference shape M5b's own finding 6 already named for
   `ConditionEnd`'s `referenced_by_attribute` ("a reference shape v1's
   interpreter does not resolve") — that finding judged it harmless
   for `sinusitis.json` because nothing on that module's own control
   flow ever consulted the missing reference; here it is load-bearing:
   every patient who is ever prescribed a medication through EITHER
   called submodule reaches `End_Ear_Infection_Medications`, which
   gates on exactly this attribute.
2. **The `Attribute` condition type supports only `!=`/`=` operators
   today — `is nil`/`is not nil` are unbuilt.** Confirmed mandatory-
   path: the root module's own post-medication cleanup state and both
   submodules' own idempotency-gating `Initial` states all use one of
   these two operators, never `!=`/`=`.
3. **`encounter_class: "outpatient"` is a real GMF value this loader's
   4-entry `encounter-class->keyword` map (section 1) doesn't
   recognize** — `Ear_Infection_Encounter`, the module's own PRIMARY
   encounter (every patient who ever gets an ear infection reaches
   it), uses it. Falls through to `(keyword (slug c))` at
   normalization (no throw there) but then fails `GmfState`'s
   `:encounter` schema variant, whose `:encounter-class` enum is
   closed to `#{:wellness :ambulatory :emergency :inpatient}` —
   `:schema-invalid` at load. Resolution (Step 2, disclosed as a
   loader-normalization commit, not a new interpreter mechanism):
   `encounter-class->keyword` gains `"outpatient" -> :ambulatory` —
   `ehrt.sim-trajectory.compile-trajectory`'s own `encounter->step`
   (confirmed by direct read, lines ~97–102) already treats `:wellness`
   and `:ambulatory` identically (both compile to `:outpatient-visit`),
   so this is a genuine same-concept vocabulary alias, not an invented
   mapping — and it needs no `compile-trajectory` change, keeping this
   ADR's own fence (interpreter/loader only) intact.
4. **The already-documented `wellness: true` boolean idiom (section 8,
   "M7 survey," "A second GMF wellness-encounter encoding this loader
   doesn't recognize") is confirmed MANDATORY-path here, not an
   excludable tail the way that survey's own five prior instances
   (`mTBI`/`atrial_fibrillation`/`osteoporosis`/`epilepsy`/`med_rec`)
   were.** `Next_Wellness_Encounter` is `End_Ear_Infection_Medications`'s
   own unconditional fallback arm (reached by every patient once both
   medication attributes are cleared) — every ear-infection walk
   reaches it. A second wrinkle beyond the boolean-vs-string encoding
   itself: this state carries NO `codes` key at all (Synthea's own real
   wellness-encounter concept is auto-filled by engine machinery this
   project's own GMF port never carried, not authored per-module) —
   `GmfState`'s `:encounter` variant currently requires `:codes`.
   Resolution (Step 2): the SAME loader normalization the M7 finding
   already named (`:wellness true` + absent `:encounter-class` ->
   `:encounter-class :wellness`), plus `:codes` becomes
   `{:optional true}` on `:encounter` — the identical "don't fabricate
   what was never actually said" disposition M5b's finding 6 already
   established for `ConditionAnnotation`'s own `:codes` field.
   `compile-trajectory`'s own `encounter->step` never reads `:codes`
   for an encounter event (confirmed by direct read) — safe to loosen
   at the schema layer with zero downstream impact.

**D7 hidden-import check, `ear_infections` closure: empty (clean),
confirmed by exhaustive scan of every `Attribute` condition and every
`SetAttribute`/`assign_to_attribute` write across all three files.**
`antibiotic_prescription` and `otc_pain_reliever` are each READ in a
DIFFERENT module than at least one of their own WRITES — the root
module (`ear_infections.json`) reads what its own called submodule
writes (`assign_to_attribute`), and the root module later WRITES the
same key back to nil (`Unset_Antibiotic_Prescription_Attribute`/
`Unset_Non_Opioid_Prescription_Attribute`) that the submodule's own
`Initial` state later reads again on any subsequent call. This is
concrete, load-bearing evidence for D1's own root-scoping design — not
a hypothetical the closure happens not to exercise.

### `urinary_tract_infections.json` closure survey — DEFERRED this session (D6)

The design session's own framing named this module's payoff as
"contingent on its closure surveying clean" — Step 1's own real-closure
read found it does not. The top-level module's own survey row (this
document's own M7 appendix, "`CallSubmodule` ×3... deferred") already
undercounted the closure's real depth: three named path submodules
(`uti/telemed_path`, `uti/ambulatory_path`, `uti/ed_path`) themselves
transitively call EIGHT more (`uti/hpi`, `uti/gu_pregnancy_check`,
`uti/abx_tx`, `uti/labs`, `uti/lab_follow_up`, `uti/ambulatory_eval`,
`uti/ed_eval`, `uti/ed_bundle`) — twelve real files, not four.

| Module | States | State-type gap | Other findings |
|---|---:|---|---|
| `urinary_tract_infections.json` (root) | 29 | `CallSubmodule` ×3 | **TWO transition kinds outside this document's own four (§2), both new findings**: `type_of_care_transition` (`Care Pathways`, D5, characterized below and built this session) and `lookup_table_transition` — a SIXTH real GMF transition kind this document's own brief never named, on the module's own entry path (`Urinary Tract Infection`, selecting Cystitis vs. Pyelonephritis; `Recurrent UTI`, its own recurrence analogue) |
| `uti/telemed_path.json` | 31 | `CallSubmodule` ×7 | none beyond the calls themselves |
| `uti/ambulatory_path.json` | 18 | `CallSubmodule` ×4 | none beyond the calls themselves |
| `uti/ed_path.json` | 7 | `CallSubmodule` ×2 | none beyond the calls themselves |
| `uti/hpi.json` | 12 | none (`Symptom` ×8) | none |
| `uti/gu_pregnancy_check.json` | 6 | none (`Observation` ×2) | none |
| `uti/abx_tx.json` | 30 | none (`MedicationOrder` ×19) | none |
| `uti/labs.json` | 19 | **`DiagnosticReport` ×2 (Wave-D-scoped)** | none |
| `uti/lab_follow_up.json` | 10 | `CallSubmodule` ×1 (-> `uti/abx_tx`) | none |
| `uti/ambulatory_eval.json` | 7 | `CallSubmodule` ×3 (-> `uti/gu_pregnancy_check`, `uti/labs`, `uti/abx_tx`) | confirmed by direct transition-graph read: its own `complex_transition` sends 100% of patients to `uti/labs` (20% direct, 80% via `uti/gu_pregnancy_check` first) — `DiagnosticReport` is unconditionally reached on this path, not a tail |
| `uti/ed_eval.json` | 7 | `CallSubmodule` ×2 (-> `uti/ed_bundle`, `uti/abx_tx`) | |
| `uti/ed_bundle.json` | 56 | **`DiagnosticReport` ×6, `MultiObservation` ×4 (both Wave-D-scoped)**, `CallSubmodule` ×1 (-> `uti/labs`, a second path into the same deferred type) | |

**Verdict: DEFERRED (D6) — dirty in every branch, not an excludable
tail.** All three of the top-level module's own care pathways
(`Telemedicine`/`Ambulatory`/`ED`) route into either `uti/ambulatory_eval`
(confirmed-mandatory `uti/labs`, `DiagnosticReport`) or `uti/ed_eval`
(`uti/ed_bundle`, `DiagnosticReport` + `MultiObservation`) — both
deferred types this project's own wave plan already scopes to Wave D
("state types needing IR + emitter homes"), not a cheap mechanical
extension the way `ear_infections`' own four findings (above) are. Per
D6: this closure member is dirty, so the WHOLE root module drops from
this wave's vendoring — the payoff shrinks from two closures to one,
honestly, exactly as the session prompt's own "contingent on its
closure surveying clean" framing anticipated. `type_of_care_transition`
(D5) is still characterized and built this session regardless (Wave
B's own structural scope, not conditioned on any one module vendoring)
— `lookup_table_transition` is a genuinely new, unplanned finding and
is NOT built this session: it would need real design (an external
lookup-table CSV mechanism this project has no analog for, `uti.csv`/
`uti_recurrence.csv` are never fetched or vendored here) and nothing
in this wave's own scope depends on it, since `urinary_tract_infections.json`
is deferred regardless of whether it's ever resolved. Named here as a
finding for a future wave, not an escalation this session blocks on
(the outcome — UTI deferred — does not change either way it's
eventually resolved).

**D7 hidden-import check: not run to completion for this closure** —
moot once the closure itself is deferred under D6 (D7 exists to
falsify D1's OWN scoping design against a closure this session is
about to vendor; a closure that never vendors this wave carries no
such obligation).

**Encounter-derivation wrinkle (Step 1(f)), read-only, no engine
change:** confirmed directly against the fetched closure —
`urinary_tract_infections.json` itself has NO `Encounter` state; every
`Encounter`/`EncounterEnd` pair in this closure lives inside the THREE
path submodules (`uti/telemed_path.json` ×6, `uti/ambulatory_path.json`
×3, `uti/ed_path.json` ×1), exactly the cross-boundary-encounter shape
D2's own provenance citation exists to cover. Where the residual sim's
own encounter handling would consume such an event: `ehrt.sim-
trajectory.compile-trajectory`'s `encounter->step`/`encounter-end->step`
(section 8's own "Multi-encounter-per-episode" finding already reads
this code path directly, lines ~97–117) — no new consumption point
would be needed, the same `:encounter`/`:encounter-end` trajectory
event shape a root-level `Encounter` state already produces, just
carrying a longer `:module`-citation call path (D2) once emitted from
inside a call. Not exercised by any vendored test this session, since
`urinary_tract_infections.json` itself is deferred (D6) — recorded here
so a future Wave-D session revisiting UTI does not have to re-derive
it.

### D5 — `type_of_care_transition` dispatch-rule characterization

Grounded against `Transition.java`'s own `TypeOfCareTransition` class
and `TypeOfCareTransitionOptions` (Synthea source, same pinned commit),
plus the external `telemedicine_config.json` resource that class reads
at construction time (`TypeOfCareTransition(TypeOfCareTransitionOptions
options)` calls `TelemedicineConfig.fromJSON()`).

**Real Synthea's own dispatch, as read:** `Care Pathways`'s own JSON
carries only three target STATE NAMES (`ambulatory`/`emergency`/
`telemedicine` -> `"Ambulatory"`/`"ED"`/`"Telemedicine"` in
`urinary_tract_infections.json` — no weights of any kind in the module
JSON itself). The actual selection weights live entirely in
`telemedicine_config.json`, keyed on: (a) whether `time` (the simulated
instant) is before or after `start_year: 2020`
(`config.getTelemedicineStartTime()`), and (b) whether the person's
CURRENT insurance payer's name is in `high_emergency_use_insurance_
names` (`["Medicaid", "Dual Eligible", "NO_INSURANCE"]`) — four
`EnumeratedDistribution`s total (pre/during-telemedicine ×
high/typical-emergency), one `person.randLong()`-reseeded sample drawn
from whichever one applies.

**This project's own persona (`ehrt.sim-model.interface/persona`) has
no insurance/payer concept anywhere** — the payer-name half of real
Synthea's own dispatch cannot be evaluated against this project's own
data, the identical shape of gap `Active Allergy`'s own documented
simplification already established (no allergy concept exists to
query either). **Simplification, documented here per D5's own
instruction, not left as a silent assumption:** this interpreter always
uses the `typical_emergency_distribution` branch (never
`high_emergency_distribution`) — the majority-case default, since this
project cannot determine which synthetic patients would fall into the
three named high-emergency-use payer categories. The pre/during-2020
split, by contrast, IS honestly implementable — this project's own
virtual clock (`ctx`'s `:t`) already answers "what calendar year is
it," the identical mechanism `:date` condition (Wave A) already uses —
so that half of the real dispatch rule is NOT simplified away:

- `t`'s own calendar year `< 2020`: `{:ambulatory 0.75 :emergency 0.25}`
  (`pre_telemedicine.typical_emergency_distribution` — no telemedicine
  option exists pre-2020 in Synthea's own real data either, consistent
  with `:ambulatory`/`:emergency` being the module's only two targets
  reachable before that year).
- `t`'s own calendar year `>= 2020`:
  `{:ambulatory 0.56 :emergency 0.2 :telemedicine 0.24}`
  (`during_telemedicine.typical_emergency_distribution`).

**RNG consumption: exactly one `.nextDouble` draw, the SAME fixed-
consumption weighted-pick mechanism `distributed_transition` already
uses** (`weighted-pick-transition`) — `type_of_care_transition` is
implemented as a transition KIND (a 5th sibling of `direct`/
`distributed`/`conditional`/`complex`), not a new state type, since
real Synthea's own `Care Pathways` state is itself a plain `Simple`
carrying this field, exactly like `distributed_transition` already
attaches to any state type. Joins the interpreter ns docstring's own
order contract (Step 2) as: descend into the year-gated weight table
(zero rng, a pure function of `ctx`'s own `:t`), then one weighted-pick
draw — same position in the per-state draw order every other
transition-resolving draw already occupies.

### Regression baseline (Step 1(e))

Fixed-seed walks of the three currently-vendored modules
(`sinusitis.json`, `appendicitis.json`, `sore_throat.json`) — 6 seeds
(`1 2 3 42 20260802 999999`) × 2 sexes each, `run-module` to a 10-year-
bounded horizon from a 25-year history offset (the same shape
`vendored_sore_throat_test.clj`'s own property test already uses),
hashing `(:status result)` and `(hash (:trajectory result))` —
captured before any Wave B code change. Re-run and diffed at every
Step 2/3 checkpoint per ADR-0027's own verification-baselines section;
byte-identical at every one (confirmed one final time at session
close, Step 4).

---

## 10. GMF coverage Wave C: `Death` characterization (2026-08-02)

Step 1 of the Wave C session prompt (`notes/ADRs.md` ADR-0028; C1/C3/C5
of that ADR's own decision record). Read at the SAME pinned commit
every prior GMF citation in this document uses,
`7e08387c68a7f0e21d13076609a159fd473fc902` of
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea)
(`master`) — `stroke.json` itself, and `State.java`'s own `Death` inner
class (the real Synthea engine source, not inferred from the module
JSON alone).

### C1 — Death forms, grounded against `State.java`

Real Synthea's `Death` state (`State.java`, class `Death`) declares five
fields: `codes` (cause-of-death concept list), `conditionOnset` (a
state-name citation), `referencedByAttribute` (an attribute-name
citation), `range`, and `exact` (both time-delay shapes, the SAME
`{low high unit}` / `{quantity unit}` shapes `Delay`/`Procedure`'s own
duration already use). `process(person, time)`, quoted verbatim:

```java
long deathTime = time;
if (exact != null) {
  deathTime = time + Utilities.convertTime(exact.unit, exact.quantity);
} else if (range != null) {
  double delayInDays = person.rand(range.low, range.high);
  deathTime = time + Utilities.convertTime(range.unit, delayInDays);
}
Code causeOfDeath = null;
if (conditionOnset != null) { /* resolve via person.record's own open condition named conditionOnset */ }
else if (referencedByAttribute != null && person.attributes.containsKey(referencedByAttribute)) { /* resolve via the attribute */ }
else if (codes != null && codes.size() > 0) { causeOfDeath = codes.get(0); }
person.recordDeath(deathTime, causeOfDeath);
return true;
```

Three time forms (immediate — neither `range` nor `exact` present, no
rng draw; `exact` — deterministic, no rng draw; `range` — exactly ONE
`person.rand` draw, the SAME fixed-consumption law this project's own
`resolve-time-advance` already implements for `Delay`/`Procedure`
duration — `{low high unit}` is literally the same shape, so `Death`'s
own time resolution needs no new helper, only a new case dispatching
into the existing one). Three cause-of-death resolution forms, in
priority order: `conditionOnset` (a log query against the state that
onset the condition), `referencedByAttribute` (an attribute read), and
`codes` (verbatim, code-passthrough law) — Synthea's own docstring
explicitly notes real Synthea's module CONTINUES past a `Death` state to
whatever it `direct_transition`s to next ("the module will continue to
progress to the next state(s)... typically... to a Terminal state") —
this project's own C2 ruling (ADR-0028) deliberately departs from that:
the walk terminates AT `:death`, so `Death`'s own declared transition is
never followed. **This is a disclosed, ruled simplification, not an
oversight** — donor/post-mortem content (what a real module's own
post-Death states like `stroke.json`'s own `End_Encounter`/
`End_Stroke_Condition` would otherwise represent) is exactly the
"remains future work" the deferred-table's own instruction and C2 both
already name.

**`stroke.json`'s own Death state uses exactly one of each family** —
the `range` time form (`{low: 1, high: 30, unit: "days"}`) and the
`codes` cause form (one SNOMED concept, the stroke diagnosis itself,
repeated verbatim from the module's own `Stroke` ConditionOnset state).
Neither `exact`, `conditionOnset`, nor `referencedByAttribute` is used
by this module. Per C1's own instruction, only the range time-form and
the `codes` cause-form are built this wave; `conditionOnset`/
`referencedByAttribute` are UNBUILT, named here (not silently
mishandled) — the interpreter throws a `programmer-error` ex-info if a
future module's own `Death` state uses either, the same disposition
`evaluate-condition`'s own unsupported-condition-type case already
establishes. `exact`/immediate are built anyway (the SAME code path
`resolve-time-advance` already provides for `range`, at zero marginal
cost — not "speculative" in ADR-0013 point 4's sense, since no new
mechanism is added, only an existing one's `:death` case wires to it).

### C5 — `stroke.json`'s own closure survey

| Module | States | State-type gap | Condition-vocab gap | Transition-kind sweep | Other findings |
|---|---:|---|---|---|---|
| `stroke.json` (root, no CallSubmodule — trivial one-file closure) | 12 | `Death` ×1 (this wave's own reason for existing) | none (`Date`, resolved Wave A) | `direct_transition`, `distributed_transition` ×1, `conditional_transition` ×1 — 3 of the now-six known kinds; no `complex_transition`, `type_of_care_transition`, or `lookup_table_transition` | **NEW, mandatory-path gap** (below): `Chance_of_Stroke`'s own `distributed_transition` entry for `"Stroke"` carries `"distribution": {"attribute": "stroke_risk", "default": 0}` — an ATTRIBUTE-SOURCED weight, not a literal number. `GmfState`'s own `TransitionFields` schema requires `[:distribution number?]`; unmodified, this module fails `:schema-invalid` at load, before `Death` is ever reached |

**D7 hidden-import check: vacuous (clean by construction) — a one-file
closure has no cross-module attribute reference to check**, the D7
falsifier's own precondition (D1's root-scoping design) never
applies to a non-calling walk.

### The `stroke_risk` finding — escalated, ruled

`stroke_risk` is set by real Synthea's `CardiovascularDiseaseModule`
(`calculateStrokeRisk`, `src/main/java/org/mitre/synthea/modules/
CardiovascularDiseaseModule.java`, same pin) — a hard-coded ENGINE
module (not a GMF JSON, not part of `stroke.json`'s own closure),
recalculated every simulated timestep from a Framingham 10-year stroke-
risk score (age, sex, smoking status, and comorbidities including
diabetes/coronary heart disease/atrial fibrillation). This project's
persona carries none of that — the same gap SHAPE `type_of_care_
transition`'s payer attribute and `Active Allergy` already established
(a real Synthea data source this project has no analog for), but a
materially WORSE consequence: those two precedents both left a real,
useful DEFAULT branch reachable (the "typical" emergency-use
distribution; a conservative always-false allergy check on an
excludable tail). Here the JSON's own specified fallback is **`default:
0`** — honored literally (this project's own code-passthrough/no-
fabrication discipline leaves no principled alternative), `Chance_of_
Stroke`'s own monthly gate would pick the "keep waiting" branch with
probability 1, every seed, forever: stroke onset — and the `Death`
branch this wave exists to unlock — becomes STRUCTURALLY unreachable,
not merely rare, under a bare vendored run.

**Escalated to the author (design channel, 2026-08-02) rather than
silently resolved, per C3's own escalate-with-evidence spirit
generalized to a gap this consequential to the wave's own payoff
claim.** Three options were named (defer `stroke.json` this wave;
substitute a documented nonzero constant, breaking no-fabrication
discipline; port a minimal, disclosed stroke-risk approximation as its
own scoped design). **Ruled: defer `stroke.json` this wave**, the same
D6 treatment ADR-0027 already gave `urinary_tract_infections.json` — a
module whose own mandatory path can't be honestly resolved within this
wave's own scope drops from vendoring, and the payoff shrinks honestly
rather than being papered over. `Death` itself is built fully this wave
regardless (C1/C2/C3/C4 are not conditioned on any one module vendoring
— the same "the mechanism landed either way" framing ADR-0027's own D5
account already used for `type_of_care_transition`) and proven against
the project's own hand-authored test fixture instead of a real vendored
module's own death branch (Step 3's own revised scope, ADR-0028's
deviation record). `stroke.json`'s own survey row (the M7 appendix
table, above) is updated fix-forward: **deferred, revisit trigger — an
attribute-sourced `distributed_transition` weight mechanism AND a
stroke-risk-equivalent data source both landing together**, not decided
or scoped this session.

> **Dated note, GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, H3):
> the attribute-sourced `distributed_transition` weight mechanism named
> above is now BUILT** (`ehrt.sim-trajectory.gmf-interpreter/resolve-
> distribution-value`, a `{:attribute name :default n}` NamedDistribution
> map, byte-confirmed against `stroke.json`'s own `Chance_of_Stroke`
> shape) — but this does NOT unblock `stroke.json`: the revisit trigger
> was always BOTH halves landing together, and the second half
> (a stroke-risk-equivalent data source) remains entirely unowned. The
> mechanism is proven instead against a hand-authored fixture (§14's
> own D3b account), the same "build the mechanism, prove it against a
> fixture, defer the vendoring target" shape `VitalSign` (D1a) and the
> CarePlan mechanism (D2) already established.

### C3 — the `:expired` gap table

Every claim `components/sim/docs/patient-state-model.md`'s `:status`
accumulator row and `components/sim/docs/clinical-realities.md`'s
post-mortem entry make, checked against the LIVE code (not the docs'
own prose) as of this session's own starting commit (`d8447e6`):

| Captured claim | Implemented where | Status |
|---|---|---|
| `:status` includes `:expired` as a real value | `ehrt.sim.engine/PatientState`'s `:status` enum, `[:enum :new :admitted :discharged :merged]` | **Docs-only.** `:expired` appears NOWHERE in `components/sim/src` except three lines of PROSE inside `ehrt.sim.check`'s own comments (`check.clj` lines 340-349) explaining why `order-only-when-admitted` is written as a strict generalization — no code path can produce, read, or check this value today |
| `:expired` reached via a death event OR an expired discharge disposition | — | **Docs-only.** No `:death`/disposition-carrying event type exists in `ehrt.sim.engine`'s `decide`/`evolve` multimethods |
| Therapeutic-intent event classes illegal when `:expired` | `ehrt.sim.check/order-only-when-admitted`, `/clinical-content-only-when-admitted` | **Partially implemented, by construction rather than by design.** Both invariants already read `(not= :admitted (:status before))` — the STRICT generalization patient-state-model.md's own note anticipates ("already covers it once `:expired` lands, without inventing an unfalsifiable invariant"). Once `:expired` is a real, distinct value, these two invariants automatically extend to cover it with ZERO code change — confirmed by direct read, not merely asserted |
| Morgue/funeral-home transfer, autopsy/specimen, donor-management/procurement legal while `:expired` | — | **Docs-only, and explicitly OUT of this wave's own minimal-path scope** (C3: "donor pathways... named finding, not built") |
| A final disposition-20 discharge later moves `:expired` -> `:discharged` | — | **Docs-only, out of scope** — the same donor/post-mortem administrative content C2 already defers |

**Declared minimal coherent path (C3):** (1) `:expired` joins
`PatientState`'s `:status` enum for real. (2) `Death` maps into the
compiled pathway via the EXISTING `:discharge` IR step (C4 — no new IR
step type), carrying two new optional fields: `:disposition [:enum
:expired]` and `:codes` (cause of death, verbatim). (3) `ehrt.sim.
engine`'s `:discharge` `decide`/`evolve` branch on `:disposition`: an
`:expired`-disposition discharge sets `:status :expired` (never
`:discharged`), leaves `:location`/`:attending` UNCHANGED (the
"clinically absorbing but operationally alive" fact — patient-state-
model.md's own words — requires the bed to stay occupied, not vacated),
and — a genuinely NEW piece the gap table above didn't name, found by
tracing `:discharge`'s own decide method directly — the existing
bed-ready-transfer coupling (a `:discharge`'s own decide call searches
for a boarding patient at the same vacated ward and conjoins a
`:transfer` relieving them) MUST NOT fire for an `:expired`-disposition
discharge, since no bed is actually vacated; unguarded, it would double-
occupy a bed no-double-occupancy already forbids. (4) One new,
directly-named structural invariant (mirroring `no-events-after-merged-
terminal`'s own existing shape): a location co-occupancy check specific
to the expired case (`expired-patient-retains-location` — never nil
immediately after an expired-disposition discharge, the converse of
`admitted-occupies-one-slot`'s own "never nil while admitted" rule,
stated explicitly rather than left to fall out of the generalization
above by accident). No IR/`sim-model` schema change beyond `:discharge`'s
two new optional fields is needed — both are additive, and `:discharge`'s
existing zero-field call sites (every hand-authored pathway that predates
this wave) are unaffected by construction (Malli's open-map + `{:optional
true}` fields, the same backward-compatibility shape `citation-fields`
already establishes elsewhere in `engine.clj`). **No escalation triggered
by this table** — the minimal path touches no pathway-IR step-TYPE and no
`sim-model` schema beyond two optional fields on an existing step.

---

## 11. GMF coverage Wave D stage D1a: observation-family characterization (2026-08-02)

Step 1 of the D1a session prompt (`notes/ADRs.md` ADR-0029 R2(a)/(c),
R6). **This session is characterization only (E1) — no schema, compile-
mapping, or engine code lands here; the PROPOSAL this characterization
feeds is recorded separately in ADR-0029's own D1 placeholder, marked
PROPOSED, awaiting a design-channel ruling.** Read at the SAME pinned
commit every prior GMF citation in this document uses,
`7e08387c68a7f0e21d13076609a159fd473fc902` of
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea)
(`master`) — `sepsis.json` itself, plus four real engine source files:
`State.java` (`MultiObservation`/`DiagnosticReport`/`ObservationGroup`/
`Observation`/`VitalSign` inner classes), `HealthRecord.java`
(`multiObservation`/`report`/`Observation`/`Report`), `Person.java`
(`getVitalSign`/`setVitalSign`), and `LifecycleModule.java` (the
built-in, non-GMF module that actually populates the vital signs
`sepsis.json` reads).

### D1a-1 — `sepsis.json` closure: single file, no `CallSubmodule`

`sepsis.json` has **zero `CallSubmodule` states** (confirmed by direct
state-type census, below) — unlike Wave B/C's own closures
(`ear_infections`/`urinary_tract_infections`), there is no second file
to fetch or characterize. The existing survey row (Appendix, above,
M7 survey) already named this module's disposition; this session adds
the closure-level and source-level detail that row's own format didn't
carry.

| Module | States | State-type gap | Condition-vocab gap | Other findings |
|---|---:|---|---|---|
| `sepsis.json` (root, no `CallSubmodule` — trivial one-file closure) | 37 | `MultiObservation` ×2, `DiagnosticReport` ×1 (Wave D's own reason for existing) | none (`Active Allergy`/`Age`/`Observation`, all v1 — `Observation`-as-condition-type resolved Wave A) | see D1a-3 through D1a-7, below, for the full account |

**State-type census** (direct count against every state's own `type`
key): `ConditionOnset` ×3, `Death` ×1, `Delay` ×3, `DiagnosticReport`
×1, `Encounter` ×1, `EncounterEnd` ×2, `Guard` ×1, `Initial` ×1,
`MedicationOrder` ×4, `MultiObservation` ×2, `Observation` ×6,
`Procedure` ×6, `Simple` ×5, `Terminal` ×1 (sums to the survey row's
own 37). Every type OTHER than `MultiObservation`/`DiagnosticReport`
is already v1 — `Death` (Wave C), the four original trajectory-event
types (`ConditionOnset`/`MedicationOrder`/`Observation`/`Procedure`),
and the five consumed-internally types (`Initial`/`Terminal`/`Simple`/
`Delay`/`Guard`) — confirming this closure is EXACTLY as clean as the
prioritization table already claimed: the two Wave-D-scoped types are
the module's only real blocker.

**Transition-kind sweep, against all seven known kinds (§2):**
`direct_transition` ×28, `distributed_transition` ×6,
`conditional_transition` ×2 — **zero** `complex_transition`, **zero**
`type_of_care_transition`, **zero** `lookup_table_transition`, and —
checked directly against every one of the six `distributed_transition`
states' own distribution values (`0.06`/`0.94`/`0.6`/`0.4`/`0.125`/
`0.875`/`0.2`/`0.8`/`0.18`/`0.82`/`0.5`/`0.5`) — **zero** attribute-
sourced weights (every value is a literal number, unlike `stroke.json`'s
own `stroke_risk` finding, ADR-0028). **This is a real, honest finding
against the prompt's own anticipated shrinkage:** sepsis needs NONE of
the three D3-scoped transition kinds — D1 carries no D3 dependency via
transitions, and D1's own payoff is not resequenced or shrunk on this
axis.

### D1a-2 — `MultiObservation`/`DiagnosticReport`: one shared parent, embedded-only children, no cross-state coupling

Grounded against `State.java`'s own class hierarchy, not the module
JSON alone. `MultiObservation` and `DiagnosticReport` both extend a
private abstract class, `ObservationGroup`:

```java
private abstract static class ObservationGroup extends State {
  protected List<Code> codes;
  protected List<Observation> observations;
  ...
}
public static class MultiObservation extends ObservationGroup {
  private String category;
  public boolean process(Person person, long time) {
    for (Observation o : observations) { o.process(person, time); }
    ...
    HealthRecord.Observation observation =
        person.record.multiObservation(time, primaryCode, observations.size());
    ...
    observation.category = category;
    return true;
  }
}
public static class DiagnosticReport extends ObservationGroup {
  public boolean process(Person person, long time) {
    for (Observation o : observations) { o.process(person, time); }
    ...
    Report report = person.record.report(time, primaryCode, observations.size());
    ...
    return true;
  }
}
```

**Children are EMBEDDED, never referenced.** `observations` is a
Gson-bound `List<Observation>` — the JSON `"observations"` array is a
list of FULL, INLINE `Observation` STATE definitions (the same shape
`sepsis.json`'s own standalone `Lactate_Level`/`Pulse_Oximetry` states
use), not a count or a set of names pointing at other states. There is
no field on `ObservationGroup`, `MultiObservation`, or
`DiagnosticReport` that names a preceding state — the Java type itself
forecloses a reference-based authoring shape, this is not merely what
`sepsis.json` happens to do. `sepsis.json`'s own `Record_Blood_Pressure`
(two embedded children, `range`-sourced) and `Blood_Cultures` (one
embedded child, `value_code`-sourced) both confirm this directly.

**`number_of_observations` (JSON) is DEAD — grepped for across the
whole of `State.java`, zero hits under any camelCase binding.** The
`int numberOfObservations` parameter `multiObservation`/`report` (below)
actually consume is the JAVA LIST's own `.size()` at the `process()`
call site (`observations.size()`, quoted above) — NOT anything read
from the module's own JSON. `sepsis.json`'s `Record_Blood_Pressure`
carries `"number_of_observations": 0` and it is never read; the real
count is 2, the length of its own `"observations"` array. This project's
own schema needs no `number_of_observations`-equivalent field at all —
the children VECTOR's own count already is the count.

**`category` is `MultiObservation`-only at the Java level —
`DiagnosticReport` (and `ObservationGroup` itself) declare no
`category` field.** Confirmed both by the class bodies above and by
direct inspection: `sepsis.json`'s `Blood_Cultures` (`DiagnosticReport`)
carries no top-level `category` key; `Record_Blood_Pressure`/
`Record_Blood_Pressure_2` (`MultiObservation`) both carry
`"category": "vital-signs"`. Each CHILD observation carries its own
`category` regardless (`Observation`'s own field, per-child) — the
top-level `category` is a MultiObservation-only convenience, not a
report-level concept `DiagnosticReport` shares.

**No coupling between the two state TYPES exists anywhere — structural
or referential.** In `sepsis.json`: `Blood_Cultures` (`DiagnosticReport`)
fires exactly once, unconditionally, the FIRST state after
`Sepsis_ED_Encounter` opens — reached by 100% of sepsis-onset patients.
`Record_Blood_Pressure`/`Record_Blood_Pressure_2` (`MultiObservation`)
fire 0, 1, or 2 times depending on path (patients who reach `Normal_MAP`
then `Discharge_to_Home` directly — the majority path — never reach
either) — confirmed by full transition-graph trace, below. **Neither
ever names or reads the other; they are two independent state types
that share an implementation parent and a JSON authoring shape, nothing
more.** Consistent with this document's own existing prioritization
table (Appendix): of the modules already read across Waves A–C,
`gallstones`/`dialysis`/`lung_cancer`/`colorectal_cancer` all carry
`DiagnosticReport` with no `MultiObservation` anywhere — the reverse
(a `MultiObservation`-only module) was not found in the 41-module
corpus already surveyed, though that survey was never run looking
specifically for this combination and this session did not re-run it —
named as a gap in the existing evidence, not claimed as a negative
proof.

**Full reachability trace (for the record):** `Sepsis_ED_Encounter` ->
`Blood_Cultures` -> `Administer_Broad_Spectrum_Abx` -> {`Aztreonam` |
`Piperacillin_Tazobactam`} -> `Vancomycin` -> `Vitals_and_Labs` ->
`Capillary_Refill` -> `Pulse_Oximetry` -> `Lactate_Level1` ->
`Fluid_Resuscitation` -> `Check_Septic_Shock` -> {0.2 `Low_MAP` ->
`Septic_Shock` -> `Admit_to_ICU` | 0.8 `Normal_MAP` ->
`Admit_to_Inpatient`} -> ... -> `Fluid_Resuscitation2` -> `Lactate_Level`
-> {value>=2: `Administer_Vasopressors` -> `Norepinephrine` ->
`Record_Blood_Pressure` | else: `Record_Blood_Pressure` directly} ->
`Check_ARDS` -> {0.18 ARDS branch -> `Ventilator`/`Ventilator_Weaning`
-> `Delay_in_ICU` or `Death` | 0.82 `Delay_in_ICU` directly} ->
`Delay_in_ICU` -> {0.125 `Death` | 0.875 `Record_Blood_Pressure_2`} ->
`Admit_to_Inpatient` -> `Delay_3-10_days` -> {0.6 `Discharge_to_Home` |
0.4 `Admit_to_ICU`, a genuine relapse CYCLE back into the ICU branch —
noted for completeness, not itself a GMF anomaly; Synthea's own
`distributed_transition` graphs are not required to be acyclic the way
D3's own closure call-graph is}. `Record_Blood_Pressure` is reached on
the `Low_MAP`/vasopressor branch AND on the ICU-relapse branch;
`Record_Blood_Pressure_2` is reached only via `Delay_in_ICU`'s own
0.875 branch — both MANDATORY once on their own branch (the existing
survey row's own characterization stands, confirmed rather than merely
repeated).

### D1a-3 — Observation value-sourcing: three real mechanisms in this one closure, only one built today

`State.java`'s `Observation.process()` (non-legacy branch, quoted in
full) tries, in order: `distribution`, `attribute` (a person-attribute
read), `vitalSign` (`person.getVitalSign(vitalSign, time)`), `valueCode`,
`expression` (CQL), `sampledData`, `attachment`. The LEGACY branch
(`isLegacyGmf()` — true whenever `exact`/`range` is present on THAT
STATE, a per-state check, not a module-wide `gmf_version` flag despite
`sepsis.json`'s own top-level `"gmf_version": 2` key) supports only
`exact`/`range`. `sepsis.json` exercises exactly THREE of these:

1. **`range`** (legacy path, ALREADY BUILT) — `Lactate_Level`,
   `Lactate_Level1`, `Low_MAP`, `Normal_MAP`, plus `Record_Blood_
   Pressure`'s two embedded children. Matches this project's own
   `sample-observation-extra` (`gmf_interpreter.clj` lines 535-540)
   exactly: `(rand-double-in rng low high)`, one draw, unit carried
   verbatim.
2. **`value_code`** (a coded/qualitative finding, e.g. `Capillary_
   Refill`'s "Increased capillary filling time" and `Blood_Cultures`'
   own embedded child, "Positive (qualifier value)") — **UNBUILT.**
   `sample-observation-extra` has no branch for it; a `value_code`-only
   Observation (no `range`) compiles today to `{:codes codes}` with no
   value at all — the qualitative finding is silently dropped, not
   fabricated, but also not carried.
3. **`vital_sign`** (a named-vital-sign lookup, e.g. `Pulse_Oximetry`'s
   `"vital_sign": "Oxygen Saturation"` and `Record_Blood_Pressure_2`'s
   two embedded children, `"Systolic Blood Pressure"`/`"Diastolic Blood
   Pressure"`) — **UNBUILT, and its real upstream source has NO analog
   anywhere in this project (D1a-4, below).** Same silent-drop
   consequence as `value_code`.

**Real texture, worth naming plainly: the SAME clinical concept (blood
pressure) is authored via BOTH mechanisms side by side in one module** —
`Record_Blood_Pressure`'s children use `range` (a flat 40-120 mm[Hg]
sample), `Record_Blood_Pressure_2`'s children use `vital_sign` (reads a
continuously-trending physiological value). Real Synthea authors mix
idioms even within a single module; a `:diagnostic-report`/`:observation`
child schema that only accepts ONE of the three value-sourcing shapes
would reject real, mandatory-path content this closure already proves
exists.

### D1a-4 — `vital_sign`'s real source: `LifecycleModule.java`, a hardcoded Java module this project has never ported

`Person.getVitalSign` THROWS if the named vital sign was never set
(`vitalSigns.get(vitalSign)` returns null -> `IllegalStateException`,
`Person.java` lines ~551-556) — there is no silent default at the
`Person` level. Real Synthea supplies `SYSTOLIC_BLOOD_PRESSURE`/
`DIASTOLIC_BLOOD_PRESSURE`/`OXYGEN_SATURATION` (among many others) via
`LifecycleModule.java`, a Java class registered as one of Synthea's
CORE modules (run for every patient, same as any GMF module, but
authored in Java, not JSON — entirely outside this project's GMF
loader/interpreter and outside its own module-vendoring surface):

```java
person.setVitalSign(VitalSign.SYSTOLIC_BLOOD_PRESSURE,
    new BloodPressureValueGenerator(person, SysDias.SYSTOLIC));
person.setVitalSign(VitalSign.DIASTOLIC_BLOOD_PRESSURE,
    new BloodPressureValueGenerator(person, SysDias.DIASTOLIC));
...
person.setVitalSign(VitalSign.OXYGEN_SATURATION, oxygenSaturation); // age/hypoxemia-conditioned
```

**This project's persona/clinical-state model has NO equivalent
continuous physiology simulation for blood pressure, oxygen saturation,
or any other `LifecycleModule`-sourced vital** — the identical shape of
gap `Active Allergy`'s own documented simplification and D5's own
payer-name gap already established (a real upstream mechanism this
project's own data model has nothing to evaluate against). **A `vital_
sign`-typed Observation value therefore cannot be honestly computed
from this project's own state today, by construction, not by omission**
— this is the concrete, evidence-grounded finding the session prompt's
own E4(b) asked for, though it lands on the `vital_sign` FIELD (an
`Observation`/`MultiObservation`-child property), not on the `VitalSign`
STATE TYPE or the `Vital Sign` CONDITION TYPE R2(c) actually names
(D1a-5, next).

### D1a-5 — `VitalSign` (state type) and `Vital Sign` (condition type): both ABSENT from `sepsis.json`'s own closure

Checked directly, exhaustively: `sepsis.json` has zero states of type
`VitalSign` and zero conditions of `condition_type: "Vital Sign"`
anywhere in its closure. **R2(c)'s own dissolution design
(`VitalSign` state -> observation-flavored events, `Vital Sign`
condition -> a log query) is therefore NEITHER confirmed NOR
contradicted by sepsis — this closure simply does not exercise either
mechanism, a negative result, not evidence either way, recorded
plainly per E4(b)'s own instruction rather than silently treated as a
pass.** The RELATED-but-distinct `vital_sign` FIELD gap (D1a-4) is real
and load-bearing for D1's own scope (it blocks two of this closure's
own `Observation`/`MultiObservation`-child values); R2(c)'s own STATE-
and CONDITION-type claims remain untested by any vendored-candidate
evidence gathered to date, across Waves A–D. `VitalSign`'s own real
semantics (`State.java`, class `VitalSign`, quoted for the record):
`process` calls `person.setVitalSign(vitalSign, ...)` with either a
constant (`exact`), a uniform range (`range`, legacy), or a
`Distribution`/CQL expression (non-legacy) — establishing a
CONTINUOUSLY-READABLE generator, not a one-time point value, over
however long until a later state (or another `VitalSign` state)
overwrites it. This is a materially different shape than this project's
existing `:observation` step (a single point-in-time sample) — worth
flagging for whichever future session actually builds `VitalSign`
support, not resolved here.

### D1a-6 — D7 hidden-import check and encounter-bearing check

**D7 (ADR-0027's own falsifier), applied to this single-file closure:**
zero `Attribute`-condition reads, zero `Observation.attribute`-sourced
reads, anywhere in `sepsis.json` — grepped exhaustively. The module's
ONE `assign_to_attribute` write (`Acute_Respiratory_Distress_Syndrome_
ARDS` -> `ARDS`, a `ConditionOnset` state — note this field is not even
in this project's own `:condition-onset` schema variant today,
`gmf.clj` line 365, only `:medication-order`'s carries `:assign-to-
attribute`; harmless here since nothing reads it back) is never read
anywhere in this closure. **Clean, but trivially so — a single-file
closure has no cross-module read/write pair to falsify at all**, a
different reason than `ear_infections`' own real cross-module D7 pass
(§9).

**Encounter-bearing check:** `sepsis.json` HAS its own root-level
`Encounter` state (`Sepsis_ED_Encounter`, `encounter_class: "emergency"`)
— unlike `urinary_tract_infections.json`'s own root module (§9), sepsis
does not depend on any called submodule to open an encounter.

### D1a-7 — Emission-side inventory: what today's ORU rendering carries, and the gap a `:diagnostic-report` step must close

Read directly against `ehrt.sim-emit-hl7.emit-hl7` (the message-type
registry and both existing ORU builders). **`:result-available` ->
`oru-message`** (MSH, PID, PV1, ORC, ONE `obr-segment` keyed on
`:concept`, one `obx-segment` PER entry in `:results`) — `obx-segment`
REQUIRES `:reference-range`/`:abnormal-flag` (`order-profiles`' own
computed truth, OBX-7/OBX-8) and hardcodes OBX-2 `"NM"` (numeric) —
neither field exists anywhere on a GMF-derived observation, and OBX-2
`"NM"` is wrong for a `value_code`-sourced result (needs `"CWE"`, HL7's
coded-value-with-exceptions type). **`:observation` -> `observation-
message`** (MSH, PID, PV1, ONE `observation-obx-segment`, NO ORC/OBR) —
closer in shape (OBX-3 from `:codes`, OBX-5 the numeric `:value` when
present, OBX-6 `:unit`) but (a) it is SINGLE-OBX only, no panel/group
shape, and (b) it shares the same `"NM"`-only, numeric-only limitation
— no path renders a `value_code`-sourced qualitative finding today,
in EITHER builder.

**What a `:diagnostic-report` step's real, legal ORU^R01-with-OBR
rendering needs, concretely:** (1) `obr-segment` reused as-is for OBR-4
— it is already generic on `concept`, and the report-level `:codes` this
step's own top-level carries (`Blood_Cultures`' own panel LOINC, D1a-2)
is exactly what it wants. (2) A NEW OBX-builder — `observation-obx-
segment`'s simpler field set (codes/value/unit, no reference-range/
abnormal-flag — GMF data has neither), but able to render EITHER a
numeric value (OBX-2 `"NM"`) OR a `value_code` (OBX-2 `"CWE"`, OBX-5 the
code's own display/code, the same `cwe-field` helper `obr-segment`
already uses for OBR-4) — today's code has no branch for the second
case in either builder. (3) One OBX per embedded child (D1a-2's own
embedding finding — never a reference to resolve), `set-id` from
position, the same `map-indexed` shape `oru-message` already uses for
`:results`. (4) ORC-1/ORC-2 reused unchanged (`orc-segment`, already
generic on `control-id`) — a `:diagnostic-report`'s own control-id needs
no new `control-id-for` case, the existing single-subject fallback
already covers it. **No `message-type-registry` change beyond a new
`:diagnostic-report -> {:type "ORU" :trigger "R01"}` entry** — the same
trigger `:result-available`/`:observation` already use, since a real
DiagnosticReport panel IS an ORU^R01, ORC+OBR present (unlike the
order-less `:observation` shape).

## 12. GMF coverage Wave D stage D1b: observation family implementation (2026-08-02)

D1a's own schema PROPOSAL (P1–P6) was RULED (`notes/ADRs.md` ADR-0029's
own dated ruling note, Q1–Q4 resolved) and landed this session, per-layer,
each co-landing schema + compile-trajectory mapping + engine handling +
emission decision, this project's own standing co-landing convention.

**Loader (`ehrt.sim-trajectory.gmf`).** `MultiObservation`/
`DiagnosticReport` join v1 as two distinct loadable state types
(`gmf-type->keyword`), both extending Synthea's own private
`ObservationGroup` — grounded directly against the real, fetched
`sepsis.json` (D1a-2's own citations): embedded children carry NO
`:type`/transitions of their own, only `:category`/`:unit`/`:codes` plus
exactly one of `:range`/`:value-code`/`:vital-sign` (`ObservationChild`,
new). `:observation`'s own GmfState gains the same two new optional
fields (`:value-code`/`:vital-sign`) a standalone Observation state can
carry (Capillary_Refill/Pulse_Oximetry, D1a-3). `:vital-sign`'s raw
string value is left untouched at load time — a lookup key into the
reference table below, never a module-authored identifier to
slug/namespace the way `:attribute`/`:symptom` are.

**Interpreter (`ehrt.sim-trajectory.gmf-interpreter`).**
`sample-observation-extra` gains `value_code`/`vital_sign` branches
alongside the pre-existing `range` branch (D1a-3's three mechanisms,
side by side), plus `:category` pass-through (Q1's own ruling). The
`vital_sign` branch (`vital-sign-extra`) draws ONE uniform value from
`sim-trajectory/vital-signs.edn`'s own `:reference-range` for the named
vital sign — the SAME plain-range mechanism `range` already uses (P4's
own Option A, RULED); because the value is drawn FROM that range by
construction, the OBX abnormal-flag this same range supports (Q2+Q3)
is always `:normal`, an honest computed consequence of the
simplification. An unrecognized vital-sign name throws
(`:unrecognized-vital-sign`) — a real, visible rejection, the table's
own "grows by evidence" rule. `MultiObservation`/`DiagnosticReport`
both compile to ONE trajectory event type, `:diagnostic-report`,
carrying the report-level `:codes` (optional, D1a-2) and the full
`:observations` vector — each child sampled via the SAME
`sample-observation-extra`, reused verbatim, never a parallel
child-sampling implementation, consuming RNG per child in vector order.

**Compile-trajectory.** `observation-fields` extracted (value/unit/
value-code/category/reference-range/interpretation) and shared by both
the top-level `:observation` step compile and each `:diagnostic-report`
child (`diagnostic-report->step`) — P1/P2's own "no third type" applied
literally in code, not merely in schema. `:diagnostic-report` joins
`pre-horizon-dropped-types` (the same treatment `:observation` already
gets).

**Engine (`ehrt.sim.engine`).** `decide`/`evolve :diagnostic-report`
follow P5's own recommendation exactly: `decide` emits ONE
`:diagnostic-report` ground-truth event (codes + the full observations
vector, mirroring how the compiled IR step itself bundles children);
`evolve` FLATTENS each child into its own `ObservationRecord` appended
to `:observations` — the identical pattern `:result-available`'s own
per-analyte flattening already establishes, reused rather than a third
accumulator shape invented. `ObservationRecord` gains `:value-code`/
`:category`; `:reference-range`/`:interpretation` already existed
(`:result-available`'s own fields, now shared). `check.clj`'s
`clinical-content-only-when-admitted` invariant extends its own event
set to include `:diagnostic-report` — the same therapeutic-intent-class
scoping `:procedure`/`:observation`/`:medication-order` already get.

**Emission (`ehrt.sim-emit-hl7.emit-hl7`).** `observation-obx-segment`
itself is EXTENDED in place, then reused directly at both call sites
(`observation-message` AND the new `diagnostic-report-message`) rather
than duplicated into a separate `report-obx-segment` builder — P6's own
"sharing observation-obx-segment's field set" is realized here as
literal reuse, one function, not a near-duplicate sibling. It now
branches OBX-2 `"CWE"`/`"NM"` on whether the observation carries
`:value-code`, rendering a SNOMED CT-coded finding via a new system-
aware `coded-value-field` (`cwe-field` itself stays LOINC-hardcoded and
untouched — every pre-existing call site is a LOINC panel/analyte
concept) — plus, beyond P6's own base sketch, OPTIONAL reference-range/
abnormal-flag rendering when the observation carries them (Q2+Q3's own
ruling, so a table-sourced STANDALONE `:observation` event — e.g.
Pulse_Oximetry — renders them too, not only a `:diagnostic-report`
child), appended ONLY when present (a variadic trailing-field build,
never a positional pad) — byte-identical to every pre-existing call
when absent, confirmed by field-count assertion. `diagnostic-report-
message` (new): MSH/PID/PV1/`orc-segment` (reused unchanged)/
`obr-segment` (reused unchanged, report-level codes)/one
`observation-obx-segment` per child via `map-indexed`, the same shape
`oru-message` already establishes for `:results`. One new
`message-type-registry` entry, `:diagnostic-report -> {:type "ORU"
:trigger "R01"}`; `control-id-for` needed no change (P6's own finding).

**`VitalSign` (state type) / `Vital Sign` (condition type) — design-
ruled (R2(c)), implementation-deferred, unchanged by this stage.**
Neither is built this session, per F3's own explicit fence (D1b's own
session prompt): no vendored module across Waves A–D exercises either
mechanism (D1a-5's own negative result, §11, still the current
evidence), and this workspace does not build unexercised machinery.
The vital-sign reference table (D1 F2) already carries what a future
`VitalSign` implementation will need (LOINC code/units/reference-range
per named vital sign); the design itself (`VitalSign` state ->
observation-flavored event, `Vital Sign` condition -> a log query,
R2(c)'s own ruling) stands as recorded, untested against real evidence.
The next session vendoring a real `VitalSign`-bearing or `Vital Sign`-
condition-bearing candidate should re-derive against source at that
point, not treat D1a-5/P3's own sketch as settled (D1a-5's own closing
note, restated here since D1b's own implementation pass is where a
reader would otherwise expect this gap to have closed).

Full field-by-field diffs, test coverage, and the sepsis.json vendoring
payoff are in the D1b session record and the commits it names (ADR-0029
execution note).

---

## 13. GMF coverage Wave D stage D2: closure survey and characterization findings (2026-08-02)

Step 1 of the D2 session (ADR-0029 R6; the CarePlan family). Both
candidate closures (`myocardial_infarction.json`,
`total_joint_replacement.json`) fetched IN FULL — every transitively
`CallSubmodule`-reachable file, not the top-level module alone — at the
SAME pinned commit every prior GMF citation in this document uses,
`7e08387c68a7f0e21d13076609a159fd473fc902` of
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea)
(`master`).

### `myocardial_infarction.json` closure survey — DEFERRED this session

The top-level survey (section 8, "`total_joint_replacement`... 4
transition kinds all appear in one module") and the prioritization
table's own MI row both undercounted this closure's real depth, the
SAME shape the UTI finding (ADR-0028) already established: MI's own
five root-level `CallSubmodule` targets (`heart/stemi_pathway`,
`heart/nsteacs_pathway`, `heart/acs_discharge_meds`,
`heart/acs_arrival_medications`, `heart/cardiac_labs`) expand
transitively — through the CABG (coronary artery bypass graft) surgical
pathway alone — into **27 real files total** (root + 26 submodules;
full closure fetched and read, not sampled): `heart/cabg_sequence` ->
`heart/cabg/{outcomes,operation,cabg_referral,postop}` and
`heart/operative_status`; `heart/cabg/operation` itself calls
`surgery/general_anesthesia`, `heart/cabg/{or_labs_meds,or_intraop,details}`;
`heart/cabg/or_labs_meds` calls `heart/or_blood`;
`heart/cabg/{cabg_referral,postop,preoperative}` chain into
`heart/cabg/{preoperative,labs_common,icu_meds_devices,postop_blood}`;
`heart/acs_discharge_meds` calls `medications/{beta_blocker,statin,ace_arb}`
(each 1000+ lines, real drug-class submodules). D7 hidden-import check
not pursued to completion — moot once the state-type census (below)
disqualifies the closure outright, the same "not run to completion,
moot once deferred" disposition UTI's own D7 entry (section 9) already
uses.

**State-type census, full 27-file closure (direct count against every
state's own `type` key):** `MedicationOrder` ×185, `Simple` ×107,
`Procedure` ×68, `SetAttribute` ×42, `CallSubmodule` ×33, `Observation`
×29, `Terminal` ×27, `Initial` ×27, `Delay` ×21, `DiagnosticReport` ×17,
`ConditionOnset` ×15, `MedicationEnd` ×14, `EncounterEnd` ×10,
`Encounter` ×10, **`SupplyList` ×6** (a state type this document has
never named — genuinely new, entirely unbuilt, no schema/loader entry
exists), **`ImagingStudy` ×5** (R5 — explicitly OUT of Wave D, a named
CHF-triggered hole, unowned by D0–D3), `Death` ×5, `Device` ×4,
`DeviceEnd` ×3, **`Counter` ×2** (named in this document's own original
brief as deferred, still unowned by any wave), `ConditionEnd` ×2,
`CarePlanStart` ×2 (this stage's own scope — present, but moot),
`MultiObservation` ×1. `DiagnosticReport`/`MultiObservation` are D1
scope, already landed — not a blocker on their own.

**Transition-kind sweep, against the six real GMF kinds this
document's own §2 names:** `direct_transition` ×473,
**`lookup_table_transition` ×39** (D3's own scope, ADR-0027's D6
finding — a SIXTH kind this loader does not build), `conditional_transition`
×37, `distributed_transition` ×32, `complex_transition` ×27, ZERO
`type_of_care_transition`.

**Verdict: MI is disqualified from D2's own vendoring scope by THREE
independent, each-individually-sufficient reasons** — `lookup_table_transition`
(D3-scoped, not this stage's), `ImagingStudy` (R5, explicitly out of
Wave D), and `SupplyList`/`Counter` (never-owned deferred types, one of
them genuinely new). The loader's own all-or-nothing gate means the
closure fails to LOAD at all regardless of where any of these states
sit in the module's own probability distribution (the same disposition
`spina_bifida.json`'s low-probability `Death` state already
established — reachability is irrelevant to a load-time schema gate).
**MI stays deferred, not vendored, resequenced honestly per G4** — its
own real blocker set (D3 kind + R5 type + two never-owned deferred
types) is a DIFFERENT, and larger, set than the CarePlan gap D2 itself
was framed around; CarePlan's own presence in this closure (×2) is
real but moot.

### `total_joint_replacement.json` closure survey — DEFERRED this session (a second blocker found at Step 2, below)

Four files total, fully resolved (root + `medications/moderate_opioid_pain_reliever`,
`total_joint_replacement/functional_status_assessments`,
`dme/wheelchair_end`) — no further `CallSubmodule` targets found at any
depth, confirmed by exhaustive cross-check of every `"submodule"` key
against every fetched file.

| Module | States | State-type gap | Condition-vocab gap | Other findings |
|---|---:|---|---|---|
| `total_joint_replacement.json` (root) | 33 | `CallSubmodule` ×4 (Wave B, already built), `CarePlanStart` ×1, `CarePlanEnd` ×1 (D2's own reason for existing) | none (`Age`/`And`/`Attribute`/`Date`, all v1) | `Joint_Replacement_Guard`'s own mandatory `joint_replacement is not nil` gate has no in-closure source (below) |
| `total_joint_replacement/functional_status_assessments.json` | 20 | none | none (`Attribute` only) | idempotency-gating `assessment_done` attribute, root-scoped, same shape ear_infections' own `Initial` gates |
| `medications/moderate_opioid_pain_reliever.json` | 12 | none | none | `assign_to_attribute: opioid_prescription`, read via a plain `Attribute` condition elsewhere in the root (not a `MedicationEnd` citation) — already-built v1 mechanism, no gap |
| `dme/wheelchair_end.json` | 4 | none | none | two `DeviceEnd` states, `referenced_by_attribute` pointing at attributes never written anywhere in this closure — harmless: `:device-end`'s own interpreter case is unconditional pass-through (`gmf_interpreter.clj` line 767), never resolves the reference |

**State-type census (full 4-file closure):** `SetAttribute` ×10,
`Delay` ×8, `Simple` ×6, `MultiObservation` ×6 (D1 scope, already
landed), `Terminal` ×4, `Initial` ×4, `CallSubmodule` ×4,
`MedicationOrder` ×3, `EncounterEnd` ×3, `Encounter` ×3, `DeviceEnd` ×3
(no matching `Device` state anywhere in the closure — confirmed
harmless, `:device-end` never resolves its own reference either way),
`Procedure` ×2, `Guard` ×1 (already v1, `gmf.clj` line 89),
`ConditionOnset` ×1, `CarePlanStart` ×1, `CarePlanEnd` ×1. Every type
OTHER than `CarePlanStart`/`CarePlanEnd` is already v1 — **the closure
surveys clean of every Wave-D-scoped type except the one D2 itself
exists to build.**

**Transition-kind sweep:** `direct_transition` ×42,
`conditional_transition` ×8, `distributed_transition` ×5,
`complex_transition` ×1 — ZERO `type_of_care_transition`, ZERO
`lookup_table_transition`. No D3 dependency; D2's own payoff is not
resequenced or shrunk on this axis.

**D7 hidden-import check: clean.** Every `SetAttribute`/`assign_to_attribute`
write and every `Attribute`-condition/`referenced_by_attribute` read
across all four files resolves within the closure's own root-scoped
namespace (`joint_replacement`'s own gap, below, is a MISSING source,
not a hidden cross-module wiring bug — the attribute name itself is
consistent everywhere it appears). `assessment_done` (submodule-local
idempotency gate) and `opioid_prescription`
(`assign_to_attribute`/plain-`Attribute`-read pair) both resolve
correctly under this project's existing root-scoping contract (Wave B,
D1), no new interpreter mechanism needed.

**`Guard`'s own mandatory `joint_replacement is not nil` gate — a
real, load-bearing, specify-vs-delegate finding, grounded directly
against the module's own text.** `Joint_Replacement_Guard` (the
closure's own second state, reached unconditionally from `Initial`)
requires `joint_replacement is not nil` before ANY further content —
including the `CarePlanStart`/`CarePlanEnd` pair D2 exists to build —
is ever reached. No state anywhere in this four-file closure ever
WRITES `joint_replacement` (confirmed by exhaustive `SetAttribute`
scan, above). The module's OWN top-level `remarks` field states the
reason plainly: *"This is not a standalone module. Currently joint
replacements are triggered by the 'joint_replacement' attribute set by
the osteoarthritis and rheumatoid arthritis modules. Possible values...
are 'hip' and 'knee'."* Real Synthea runs `osteoarthritis.json`/
`rheumatoid_arthritis.json` as INDEPENDENT sibling root modules per
patient, sharing one un-namespaced `person.attributes` map —
architecturally different from this project's own root-scoped
CallSubmodule contract (Wave B, D1), and neither sibling module is any
part of this closure or this session's own scope to vendor.

**Ruling (self-ruled at this characterization gate, precedented
directly by D1a's own Q2+Q3 governing principle: "never override what
the vendored artifact specifies; freely supply what it delegates to
the engine" — the SAME pattern Persona already establishes for
Synthea's own demographics engine, and the vital-signs reference table
already establishes for `LifecycleModule.java`'s own unported
physiology).** `joint_replacement` is DELEGATED content (no default
value anywhere in the module JSON, unlike `stroke_risk`'s own
specified `default: 0`) — sourced from two sibling modules this
project has no reason to vendor just to unblock one attribute. Rather
than defer `total_joint_replacement.json` outright (the `stroke.json`
disposition), this session supplies the gap the SAME documented-
simplification way: `ehrt.sim-trajectory.gmf-interpreter/run-module`
gains one new, purely-additive, backward-compatible trailing arity
accepting an `initial-attributes` map (defaults `{}` on every existing
call site — zero behavior change for appendicitis/sinusitis/sore_throat/
ear_infections/sepsis/death-fixture), and the vendored TJR test
supplies `joint_replacement` (`"knee"`/`"hip"`, both walked) as an
authored, provenance-cited starting attribute — citing this module's
own `remarks` block as the source of truth for why no in-project origin
exists, the same disclosure discipline the vital-signs table's own
citation already models. This is a narrower, more surgical fix than
building `osteoarthritis.json`/`rheumatoid_arthritis.json` themselves
(out of scope, unowned by any wave).

> **Dated note (2026-08-03, ADR-0033): the ENGINE-layer half of this
> rider (H7) is now resolved too.** At this stage (D2), `initial-
> attributes` reached only `run-module`'s own interpreter-layer arity —
> `engine.clj` had no config surface to seed it at all, a gap this
> paragraph did not yet own (it was found and pinned two sessions later,
> ADR-0030 J3). ADR-0033's own AR-1 adds that config surface
> (`:module-initial-attributes`, or a closure's own `:initial-
> attributes` for a direct API caller) and AR-3 threads it through
> `:registered`'s decide method — the SAME `"knee"` value this
> paragraph's own citation supplies, reused verbatim, now also proven
> through a real engine run (`vendored_tjr_test.clj`'s own seeded
> round-trip test, `components/sim-emit-hl7/test/`).

**The multi-encounter-per-episode compile-time truncation gap (section
9's own prioritization table row) still applies to this module exactly
as that row already documents** — `Pre_Procedure_Encounter_End` (the
module's own FIRST `:encounter-end`) sets `compile-trajectory`'s own
`encounter-closed?` before `Joint_Replacement_Encounter` (the real
surgical encounter, and everything inside it including
`CarePlanStart`/`CarePlanEnd`) ever opens — confirmed by direct
transition-graph read this session, not merely re-cited. **This does
NOT block D2's own vendoring**, for the same reason it never blocked
ear_infections/sepsis: this project's vendored-CLOSURE tests prove the
INTERPRETER's own raw trajectory (`run-module`), never the compiled
`compile-trajectory` -> pathway-IR -> engine -> emit-hl7 pipeline — that
fuller pipeline is proven only for single-file, non-closure modules
(appendicitis, sinusitis) to date, and remains a real, still-open,
already-named gap standing between ANY closure-having module and a
full engine/emission demonstration of its own actual surgical content.

> **Dated note (2026-08-03, ADR-0031 AR-6's second defect-fix session,
> `notes/ADRs.md` ADR-0033): CLOSED.** `engine.clj`'s `:registered`
> decide method now threads a closure's own `:modules`/`:tables` maps
> and this section's own `initial-attributes` seed through to
> `run-module`'s full arity (ADR-0033 AR-2/AR-3) — the full compile-
> trajectory/engine/emit round trip this paragraph names is proven for
> TJR (and the other two closure roots) by
> `components/sim-emit-hl7/test/vendored_tjr_test.clj`'s own seeded
> round-trip test, and by `bin/oracle-src/ehrt/oracle/digest.clj`'s own
> `total-joint-replacement-engine` first baseline (AR-4b). The multi-
> encounter-per-episode compile-time truncation gap itself (this
> paragraph's own first half) is UNCHANGED by that fix — a separate,
> still-open `compile-trajectory` gap, not touched by ADR-0033's own
> registration-wiring scope.

**Fix-forward finding (dated note, filled Step 2, 2026-08-02): a
SECOND, independent blocker found empirically AFTER Step 1's own
characterization closed, testing the `initial-attributes` fix against
the real closure.** `Joint_Replacement_Guard`'s own `allow` is `{:and
[Attribute joint_replacement is-not-nil, Age > 50 years]}` — a
COMPOUND condition. `ehrt.sim-trajectory.gmf-interpreter/age-guard-
jump-days` (the analytical short-circuit that lets a failing bare
`:age >= N years` Guard jump forward in virtual time rather than block
forever) only recognizes a BARE `:age` condition with operator `>=` —
its own docstring states this plainly ("any other failing condition...
returns nil... the walk blocks instead"), a KNOWN, deliberate v1
boundary, not an accidental gap. `Joint_Replacement_Guard`'s condition
is neither bare (`:and`-wrapped) nor `>=` (`>`) — `guard-step` cannot
resolve it, and this interpreter has no periodic re-tick mechanism (a
walk is one continuous recursive descent, not Synthea's own ticked
simulation clock): the walk BLOCKS PERMANENTLY at `:t` = DOB (age 0),
before `Joint_Replacement_Guard`'s own second child (Attribute) is
ever even reached, confirmed empirically (a throwaway probe test:
`joint_replacement` seeded via the new `initial-attributes` arg,
registration 60 years post-DOB, 20-year horizon — `:status :blocked`,
zero trajectory events). This is a genuinely new, previously-
uncharacterized interpreter gap (compound/non-`>=`-operator Age
Guards), outside G1–G5's own ruled scope (the CarePlan pair's schema/
interpreter/engine/emit chain, not Guard/condition analytical-
resolution machinery) — extending `age-guard-jump-days` to handle a
compound condition and a strict `>` correctly (verifying every OTHER
`:and` sibling already holds, plus getting the day-vs-year integer-
age-flooring boundary right for a strict inequality) is real
interpreter-core work, not a data-seeding simplification the D1a
governing principle already covers — an ESCALATION with evidence
(G1's own instruction), not improvised this session under time
pressure against core walk-time-advance logic that every other
vendored root's own Guard/Delay behavior also depends on staying
byte-identical.

**Declared D2 vendoring scope, REVISED: ZERO roots vendored this
session** — an outcome G4 explicitly names as acceptable.
`myocardial_infarction.json` deferred (three independent, each-
sufficient blockers, none of them CarePlan-shaped — named above, own
row added to section 9's own prioritization table). `total_joint_
replacement.json` ALSO deferred — the `joint_replacement` attribute
gap alone was resolved (the `run-module` `initial-attributes`
extension, real and kept), but the newly-found compound-Guard
blocker stands unresolved, named here as this closure's own next
prerequisite, unowned by any wave until a future session extends
`age-guard-jump-days`/`guard-step`. The CarePlan mechanism itself
(sim-model schema, sim-trajectory loader/interpreter/compile mapping,
sim engine fold, sim-emit-hl7 disclosed silence — all four layers,
Steps 2a–2d) is real, fully co-landed, GREEN infrastructure,
independent of whether any real module exercises it yet — the SAME
"build the mechanism, defer the vendoring target" shape D1a's own
`VitalSign` disposition and this wave's own `ImagingStudy`/`lookup_
table_transition` rows already establish, not a partial or half-
finished implementation.

**`Active CarePlan` (condition type) — design-ruled, implementation-
deferred per G2, unchanged by this stage.** Not built this session: no
vendored module across Waves A–D exercises it (`total_joint_
replacement.json`'s own closure — the only D2 candidate whose
condition vocabulary was fully surveyed — uses `Age`/`And`/`Attribute`/
`Date` only, zero `Active CarePlan`, confirmed by exhaustive scan), and
this workspace does not build unexercised machinery (the SAME fence
D1's own `Vital Sign` condition disposition already states). The
design itself (a log-query over `:care-plan-start`/`:care-plan-end`
trajectory events, architecturally the same shape `active-onset-
condition-holds?` already establishes for `Active Medication` — one
function, a different event-type pair) is straightforward to build the
day a real candidate needs it; the next session vendoring one should
build it fresh against that module's own real usage, not treat this
sketch as settled.

## 14. GMF coverage Wave D stage D3: closure survey and characterization findings (2026-08-02)

Step 1 of the D3 session (ADR-0029 R6, H1/H5: `lookup_table_transition`,
attribute-weighted `distributed_transition`, TJR's own compound-Guard
blocker, the UTI closure re-characterization). Per H5, UTI's own closure
is FRESH-fetched in full (not trusting the D2-era file list — there was
none anyway, UTI was last characterized at Wave B, ADR-0027, section 9)
and TJR's own D2 fetch is re-verified by hash, all at the SAME pinned
commit every prior GMF citation in this document uses,
`7e08387c68a7f0e21d13076609a159fd473fc902` of
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea)
(`master`).

### D3a — `LookupTableTransition` dispatch-rule characterization

Grounded against `Transition.java`'s own `LookupTableTransition`,
`LookupTableTransitionOption`, `LookupTableKey`, `DistributedTransition-
Option`, and `NamedDistribution` classes (Synthea source, same pinned
commit).

**JSON shape, read directly off `urinary_tract_infections.json`'s own
`Urinary Tract Infection`/`Recurrent UTI` states:** `lookup_table_
transition` is a vector of `{transition, default_probability, lookup_
table_name}` entries — a SIXTH transition kind, sibling to `direct`/
`distributed`/`conditional`/`complex`/`type_of_care`. `lookup_table_name`
is a relative CSV filename (`"uti.csv"`/`"uti_recurrence.csv"`), never
slugged — the same "file reference, not a semantic identifier"
disposition `:submodule` already established (Wave B, D3).

**Real Synthea's own dispatch (`LookupTableTransition.follow`):** the
CSV's own column headers, minus its LAST N columns (N = the JSON's own
`lookup_table_transition` entry count), are the table's ATTRIBUTE
columns; the remaining N columns are the transition-name columns, one
per declared entry (`createDistributedTransitionOptions` throws if a
CSV column name doesn't match a declared JSON transition — a load-time
consistency check, not a runtime one). Per attribute column: `"age"` is
special-cased — parsed as an inclusive `low-high` integer range
(`Range.between`, Apache Commons, BOTH ends inclusive) and matched
against `person.ageInYears(time)` (an Integer falling inside the row's
own range); `"time"` is special-cased the same way for a date range
(unused by either UTI table — no `"time"` column in either CSV, NOT
built this session, named unbuilt/unneeded); every OTHER column name is
matched by exact STRING equality against `person.attributes.get(name).
toString()`. A row match is looked up in a `HashMap<LookupTableKey,
List<DistributedTransitionOption>>` (`LookupTableKey`'s own overridden
`equals`, lines ~505-570: an Integer age matches a stored Range via
`Range.contains`, never Range-to-Range unless both sides are ranges);
NO row match falls back to `defaultTransitions` — a `Distributed
TransitionOption` list built directly from each entry's own JSON
`default_probability` (`loadDefaultTransitions`). Either way, the
FINAL pick is `pickDistributedTransition`: one `person.rand()` draw
(`[0,1)`), a cumulative-weight walk over whichever option list applies
— the SAME fixed-consumption weighted-pick shape this project's own
`weighted-pick-transition` already implements (row lookup itself is a
pure, zero-rng key match; exactly one draw follows, win or fallback).

**Key-column audit (H2's own specify-vs-delegate bar), both tables:**
`uti.csv`/`uti_recurrence.csv` (below) each declare exactly TWO
attribute columns, `age`/`gender` — both fields this project's own
Persona genuinely supplies (`age-years-at`, already built; `:sex`,
already read by `gender-condition-holds?`). BUILDABLE, no escalation:
neither table names a field outside this project's persona model, the
same audit `stroke_risk`/`joint_replacement` FAILED (ADR-0028/ADR-0029's
own D2 note) and vital-sign names PASSED (D1a). No table this session
fetched declares a `"time"` column — that half of real Synthea's own
mechanism stays NAMED, UNBUILT (installed ≠ used, H1's own instruction),
not silently assumed general.

**Vendored content (fetched, hashed, this session — Step 3's own
vendoring target):**

| Table | Columns | Rows | SHA-256 |
|---|---|---|---|
| `lookup_tables/uti.csv` | `age,gender,Cystitis,Pyelonephritis,Wait_for_UTI` | 10 (5 age bands × 2 sexes) | `c3eec06429961a4484d1ab5f14973b778e59b0b80bbdad3fa2629329c8dbf231` |
| `lookup_tables/uti_recurrence.csv` | `age,gender,Cystitis,Pyelonephritis` | 10 | `baf597d27a7c139f962b7a100ff02abfcdc616c540478c7867e888305965aeda` |

Both tables' own rows sum to 1.0 per row (confirmed by direct read) —
this project's own `weighted-pick-transition` (normalizes by the
entries' own summed total before drawing) and real Synthea's own
`pickDistributedTransition` (an unnormalized cumulative walk against a
raw `[0,1)` draw, DistributedTransition's own docstring: values not
summing to 1.0 shift weight onto the last entry) are behaviorally
IDENTICAL whenever weights already sum to 1.0, as both vendored tables'
own rows do — no divergence for this vendoring, the existing helper is
reused unchanged.

**Design (H1, pinned before implementation): closure DATA-FILE members
(R4/H2).** `ehrt.sim-trajectory.gmf/load-closure` gains a SEPARATE,
optional trailing `table-resolve-fn` argument (purely additive — every
existing 3-arg call site is unaffected, the same "optional trailing
arity" shape `run-module`'s own `initial-attributes` addition already
established, D2) and a parallel `lookup-table-names` collector (mirrors
`call-submodule-paths` exactly: every distinct `lookup_table_name` any
state's own `:lookup-table-transition` entries name). The all-or-
nothing gate (D3, ADR-0027) extends to table members: an unresolvable
name, an unparseable CSV, or a table declaring an attribute column
outside `#{"age" "gender"}` rejects the WHOLE closure (a NEW rejection
reason, `:unrecognized-lookup-table-column`, the same "REJECTED, never
silently skipped" disposition `:unsupported-state-type` already
establishes) — never a silent partial table. `load-closure`'s own
return shape gains `:tables` (call-path-shaped table name -> parsed
table), parallel to `:modules`, empty when a closure names none. CSV
parsing is a small, in-house line/comma splitter (both vendored tables
are header-plus-plain-numeric-rows, no quoting/escaping needed) — the
same "SimpleCSV, a lightweight ad hoc parser" shape real Synthea's own
`Utilities.readResource`/`SimpleCSV.parse` pairing already uses, not a
new external dependency for two trivial files.

`tables` threads through the interpreter as a SEPARATE, parallel
optional trailing argument to `step`/`walk-module`/`run-module`
alongside `modules` (not folded into it — `modules` keeps its own
existing documented shape, call-path -> loaded module, unchanged),
defaulting to `{}` (no tables available) at every existing call site —
zero behavior change for every one of the six already-vendored roots,
none of which ever declare a `lookup_table_transition`. `resolve-
transition` (and therefore `pass-through-outcome`, threaded through
every state-type case in `step`, not only `:call-submodule`) gains the
new kind: a zero-rng row lookup (`age-years-at` + persona `:sex` mapped
to `"F"`/`"M"`, the same map `gender-condition-holds?` already uses,
inverted) against the resolved table, falling back to the entries' own
`default-probability` list on no match, then ONE `weighted-pick-
transition` draw — joining the interpreter's own descend-run-return
order contract at the same position every other transition-resolving
draw already occupies.

### D3b — attribute-weighted `distributed_transition` (`NamedDistribution`)

Grounded against `Transition.java`'s own `DistributedTransitionOption`/
`NamedDistribution`/`processDistributedTransition` (lines ~79-106,
~707-773). A `distributed_transition` entry's own `distribution` field
is `Object` at the Java level: a plain `Double` (this project's own
already-built v1 case) OR a JSON object `{"attribute": name, "default":
n}, ` deserialized into `NamedDistribution` (`this.attribute`/`this.
defaultDistribution`, field names verbatim). `pickDistributedTransition`
resolves it at DRAW time: `dist = person.attributes.containsKey(nd.
attribute) ? (Double) person.attributes.get(nd.attribute) :
nd.defaultDistribution` — an attribute-sourced weight with a JSON-
specified fallback, read from the SAME flat `person.attributes` bag
every other attribute reference resolves against (this project's own
root-scoped equivalent, `attribute-condition-holds?`'s own `root-id`
mechanism). This is EXACTLY `stroke.json`'s own `Chance_of_Stroke` gap,
byte-confirmed here against source rather than re-derived from ADR-0028's
own prose: `{"attribute": "stroke_risk", "default": 0}` (`docs/gmf-
interpreter.md` section 10, this document, unchanged).

**H3's own disposition, restated for the record: this mechanism landing
does NOT unblock stroke.** `stroke_risk` is SPECIFIED content (the
JSON's own literal `default: 0`) whose real upstream source
(`CardiovascularDiseaseModule.calculateStrokeRisk`, an engine module,
not a GMF JSON) this project has no persona equivalent for — honoring
`default: 0` literally (this project's own no-fabrication discipline
leaves no other principled reading) still makes `Chance_of_Stroke`'s own
`"Stroke"` branch structurally unreachable. Building the MECHANISM does
not change that; `stroke.json` stays deferred until BOTH the mechanism
(this session) AND a stroke-risk-equivalent data source (unowned, ADR-
0028/ADR-0029 R7) land together, exactly as section 10's own dated note
already named. Proven instead against a hand-authored fixture (Step 2,
H3), the same "build the mechanism, prove it against a fixture" shape
`Death`'s own C1/C2 build already used before `death-fixture.json`
existed.

**Design (H1): a `:distributed-transition` entry's own `:distribution`
field becomes `[:or number? [:map [:attribute :string] [:default
number?]]]` (schema); `weighted-pick-transition` resolves EITHER shape
per entry — a plain number unchanged, or `(if (contains? (:attributes
ctx) k) (get (:attributes ctx) k) default)` where `k` is the SAME root-
scoped, slugged keyword every other attribute read already computes.
Zero new rng draw beyond the existing one-per-pick; the attribute READ
itself is a pure lookup, the same "zero-rng" property every other
attribute-condition read already has.**

### D3c — UTI closure re-survey (full 12-file, H5's own fresh fetch)

Every file re-fetched this session (not trusted from Wave B's own
prose) at the pinned commit; SHA-256 recorded per file, below. State-
type census, transition-kind sweep, and condition-vocabulary sweep
confirm Wave B's own headline finding (section 9) still holds — the
closure's ONLY state-type gap, once `DiagnosticReport`/`MultiObservation`
(D1) and `CarePlanStart`/`CarePlanEnd` (D2, present in this closure only
incidentally via shared call structure — confirmed NOT present, see
below) are counted as v1, is `lookup_table_transition` itself, this
stage's own reason for existing — plus several new, MECHANICAL
findings this session's fuller (field-level, not just type-level) read
surfaces that section 9's own type-census-only survey could not have
caught.

| File | States | SHA-256 |
|---|---:|---|
| `urinary_tract_infections.json` (root) | 29 | `18de2b8e30d41ef1770fcb10aaf5912bfdb15dbe459cec18730029a37cf9ef7b` |
| `uti/telemed_path.json` | 31 | `5f176628fb9209291dcae24ae78b76337cc5768dd9743975926c300026dde78c` |
| `uti/ambulatory_path.json` | 18 | `c6a65a8da02f240f41fab61300594e0be04eb2a9b077b579daf0b8c6d47adddb` |
| `uti/ed_path.json` | 7 | `184ce09969461a6409e990cac2e688ef84699daa1a2120a76afa4c59d0846c92` |
| `uti/hpi.json` | 12 | `eb789dabf6ad72896be1c089775d282b73a4653cff8736013bbac954f98b6242` |
| `uti/gu_pregnancy_check.json` | 6 | `945e01345cf9c32d8ed5740fa3ca1de81303c94bf0f75d7ea8dcd9d359506f77` |
| `uti/abx_tx.json` | 30 | `4a67624e9bb7a755022f758efff1f9240d22cd2149d19e18c79f79fc9f9a0d05` |
| `uti/labs.json` | 19 | `a85e6f7d81e2fe673f08d39253b4de4de600a50a1f829c85a43abbc3723b2b04` |
| `uti/lab_follow_up.json` | 10 | `ba89f931feaa0bc89bdf37909661c93d55fd7ef7fe87d5e37b65414890c3d58a` |
| `uti/ambulatory_eval.json` | 7 | `117c7462c39549af328d0d17889058c09fc1002b9516fb6a71f95cd0976cae93` |
| `uti/ed_eval.json` | 7 | `24b7e6173b8c4da9782f2e04c50d3c098121def9efb1d933b9d729d93cc767a1` |
| `uti/ed_bundle.json` | 56 | `d9b85e669c1ac1f49bb04ed00d5e2a73260284c01a743fca23290680a61f7927` |

**State-type census (232 states total): every type is ALREADY v1
(`SetAttribute`, `Terminal`, `ConditionEnd`, `ConditionOnset`, `Delay`,
`MedicationEnd`, `Simple`, `CallSubmodule`, `Initial`, `Guard`,
`MedicationOrder`, `Procedure`, `Encounter`, `EncounterEnd`,
`Observation`, `DiagnosticReport`, `MultiObservation`, `Symptom`) — ZERO
`CarePlanStart`/`CarePlanEnd`/`ImagingStudy`/`SupplyList`/`Counter`/any
other deferred type anywhere in the closure.**

**Transition-kind sweep, all seven known kinds:** `direct_transition`
(every file), `conditional_transition` (16), `complex_transition` (21),
`distributed_transition` (7), `type_of_care_transition` (1, root's own
`Care Pathways`, already built Wave B), **`lookup_table_transition` (2,
both root: `Urinary Tract Infection` -> `uti.csv`, `Recurrent UTI` ->
`uti_recurrence.csv`)** — ZERO occurrences of any kind beyond these six;
no eighth kind found.

**Condition-vocabulary sweep:** `Age`, `Gender`, `And`, `Or`, `At Least`,
`Attribute`, `Active Condition` — ALL already v1 (Wave A/B). Zero new
condition types.

**D7 hidden-import check: clean.** Every cross-module attribute flow
confirmed root-scoped and correctly resolving under the existing Wave B
mechanism: `uti/abx_tx.json`'s own 18 `MedicationOrder` states all
`assign_to_attribute: "UTI_Tx"`; the ROOT's own `End UTI Tx` reads it
via `referenced_by_attribute: "UTI_Tx"` — write in a different closure
member than the read, exactly D7's own falsifier target, resolving
correctly by construction (root-scoped keys, D1).

**Specify-vs-delegate audit:** every attribute/value source found
(`uti.csv`/`uti_recurrence.csv`'s own age/gender columns, D3a above) is
persona-backed and buildable; no SPECIFIED-but-unsourceable value
(the `stroke_risk`/`joint_replacement` shape) appears anywhere in this
closure.

**New mechanical findings (field-level, not caught by section 9's own
type-only census) — each cheap, narrowly-scoped, the SAME "loader
normalization, not a new interpreter mechanism" disposition Wave B's own
D6 `encounter_class`/`wellness` findings already established (ADR-0027's
own Deviation record precedent for "characterization surfaces a real,
in-spirit-authorized finding," landed rather than dropped):**

1. **`gmf_version: 2` — every one of the twelve files carries this tag
   (`urinary_tract_infections.json` through `uti/ed_bundle.json`), the
   FIRST time this project's own vendored-module survey has encountered
   it.** It is additive, not a wholesale format replacement: the SAME
   file freely mixes the OLD per-field `range`/`exact`/`duration` shapes
   (already v1) alongside a NEW, uniform encoding — a top-level
   `"distribution": {"kind": "EXACT"|"UNIFORM", "parameters": {...}}`
   plus a sibling top-level `"unit"` — replacing `Delay`'s own top-level
   `range`/`exact` keys, `Procedure`'s own `duration` field, and
   `Symptom`'s own top-level `range`/`exact` severity keys, one state at
   a time, author's choice. 52 occurrences across the closure (34
   `EXACT`, 18 `UNIFORM`, confirmed by direct scan); ZERO third `kind`
   value found. Mechanical translation, confirmed field-by-field against
   both v1 and v2 examples of the SAME state type (`hpi.json`'s own
   `History Taking`/`Dysuria` states, `ambulatory_eval.json`'s own `Eval
   Procedure`): `UNIFORM` -> the existing `Range` shape (`{:low
   parameters.low :high parameters.high :unit unit}`); `EXACT` -> the
   existing `Exact` shape (`{:quantity parameters.value :unit unit}`,
   `unit` absent for unitless Symptom severity). Loader normalization
   (`gmf.clj`'s `normalize-state`), dispatched on the state's own
   already-normalized `:type`: `:delay`/`:symptom` write the translated
   shape to their own existing TOP-LEVEL `:range`/`:exact` keys (the
   exact keys `resolve-time-advance`/the `:symptom` interpreter case
   already read); `:procedure` writes it to its own existing `:duration`
   key, in the SAME flat `{:low :high :unit}` shape v1-authored
   `:duration` fields already use (confirmed against `appendicitis.json`/
   `sepsis.json`) — deliberately NOT a `{:range {...}}`-wrapped shape,
   matching the pre-existing v1 encoding exactly rather than inventing a
   new one. **FIXED (2026-08-03, `notes/ADRs.md` ADR-0032, commit
   `1ea1f4a`) — was disclosed, not fixed, pre-existing gap found along
   the way:** `ehrt.sim-trajectory.gmf-interpreter/resolve-time-advance`
   was called with `(:duration state)` as its own argument and
   destructured `{:keys [range exact]}` FROM that argument — but
   `:duration` (both v1- and, after this normalization, v2-authored) is
   a FLAT `{:low :high :unit}`/`{:quantity :unit}` map, never `{:range
   {...}}`/`{:exact {...}}`-wrapped, so `range`/`exact` were always nil
   and `resolve-time-advance` silently fell to `:else t` (zero advance)
   for EVERY `Procedure` state's own `:duration`, in every vendored
   module, v1 or v2, confirmed by direct read. Was out of D3's own ruled
   scope (H1-H8 name three mechanisms and a re-characterization, not a
   `Procedure`-timing fix) — named there as a live, unowned gap; fixed
   by ADR-0032 (ADR-0031 AR-6's first defect-fix session): the call site
   (`emit-and-advance`, gmf-interpreter.clj) now wraps the flat
   `:duration` as `{:range duration}` before calling `resolve-time-
   advance`. Oracle-bracketed against `bin/regression-oracle` — the
   census discovered by that same bracket that the hand-authored
   `death-fixture.json` ALSO carries a duration-bearing Procedure,
   correcting this session's own survey (ADR-0032's own AR-4 dated
   note has the full account).
2. **`SetAttribute`'s own `value_code` field (a Concept, the same shape
   `:observation`'s own `value_code`/`:diagnostic-report`'s own children
   already carry) is unbuilt** — confirmed on TJR's root file (below,
   D3f), not this closure; noted here for one combined build decision.
3. **An embedded `MultiObservation`/`DiagnosticReport` child's own
   `exact` value-sourcing mechanism (a fourth, alongside D1a-3's `range`/
   `value_code`/`vital_sign`) is unbuilt** — confirmed on TJR's own
   `functional_status_assessments.json` (below, D3f), not this closure;
   noted here for one combined build decision.
4. **Six new vital-sign names, unlisted in `sim-trajectory/vital-
   signs.edn`** (`uti/ed_bundle.json`'s own BMP/CMP `DiagnosticReport`
   children, 4 occurrences each across the file's own repeated lab
   panels): `Glucose`, `Urea Nitrogen`, `Calcium`, `Sodium`, `Potassium`,
   `Chloride`, `Carbon Dioxide` — SEVEN names, not six (corrected count:
   Glucose/Urea Nitrogen/Calcium/Sodium/Potassium/Chloride/Carbon
   Dioxide). LOINC codes verified this session against a live public FHIR
   terminology server (`r4.ontoserver.csiro.au`, CSIRO — a citable public
   $lookup endpoint, the same "real codes, cross-checked against a
   citable public source" discipline F21 already establishes, loinc.org
   itself still returning 403 to automated fetch): `Glucose` 2345-7,
   `Urea Nitrogen` 3094-0, `Calcium` 17861-6, `Sodium` 2951-2,
   `Potassium` 2823-3, `Chloride` 2075-0, `Carbon Dioxide` 2028-9 — all
   confirmed active. Reference ranges are author-curated typical-adult
   values (mg/dL for Glucose/Urea Nitrogen/Calcium, mmol/L for
   Sodium/Potassium/Chloride/Carbon Dioxide), the SAME "plausibility, not
   diagnostic precision" bar the table's own existing three rows already
   set — NOT independently source-verified beyond the LOINC code itself,
   disclosed the same way. Table grows by evidence (this closure's own
   real need), not speculation.

**Encounter-derivation wrinkle:** unlike Wave B's own UTI survey note
(section 9), THIS closure's own three path submodules
(`telemed_path`/`ambulatory_path`/`ed_path`) still carry every
`Encounter`/`EncounterEnd` pair themselves — confirmed unchanged,
exercised by this session's own vendored test (H6's own cross-boundary
encounter-event assertion, deferred at Wave B, closed here).

**Declared D3 vendoring scope for UTI (H5): BUILDABLE, pending H2's own
mechanism landing plus the four mechanical findings above** —
`lookup_table_transition` is the closure's only remaining gap once
`gmf_version 2`'s translation lands; every other prior blocker
(`CallSubmodule`, `DiagnosticReport`/`MultiObservation`, `type_of_care_
transition`) is already v1. Scope is the FULL twelve-file closure plus
its two lookup tables — no branch dropped.

### D3d — TJR fetch re-verification and new findings (H5)

`total_joint_replacement.json`'s own 4-file closure re-fetched fresh
this session (not trusted from D2's own prose — D2 never vendored it,
so no prior hash existed to diff against; this session's own fetch is
the first hash-anchored record). D2's own characterization (section 13)
holds exactly: 4 files, `CarePlanStart`/`CarePlanEnd` the only Wave-D-
scoped gap, D7 clean, `joint_replacement` delegated (`initial-
attributes`, already landed D2).

| File | States | SHA-256 |
|---|---:|---|
| `total_joint_replacement.json` (root) | 33 | `1666dfc39a3a2266ccc8c5937a0de317a4330489fb315e36b9d55c6dd6bb3d8b` |
| `medications/moderate_opioid_pain_reliever.json` | 12 | `e48546e9250ec9d76408d3aa76898d3f30b7e49ee81bed692dc830acb4a4efba` |
| `total_joint_replacement/functional_status_assessments.json` | 20 | `7fd8e6f27f75718628529448d078c87ddf0849b19014d4e4f854935a97c5fe6c` |
| `dme/wheelchair_end.json` | 4 | `e24ea11f2c9cdf830e06ce10eee411368194ab15890a1e0dec8ab5939be1aea9` |

Root and `functional_status_assessments.json` carry `gmf_version: 1`;
`moderate_opioid_pain_reliever.json` carries `gmf_version: 1`;
`wheelchair_end.json` carries `gmf_version: 2` but exercises none of
its new fields (confirmed by direct scan) — TJR's own exercised paths
need NO gmf_version-2 translation, unlike UTI's.

**Two new, previously-uncharacterized field-level findings (D2's own
state-TYPE census could not have caught either), byte-confirmed against
the fresh fetch:**

1. **`SetAttribute`'s own `value_code` field** (`Pre_Procedure_Encounter_
   Reason` sets `pre_procedure_encounter_reason`; `Home Health Reason
   Knee`/`Home Health Reason Hip` set `home_health_reason`, all three via
   `value_code`, never `value`) — unbuilt (`GmfState`'s own `:set-
   attribute` schema variant declares only `:value {:optional true}
   :any`; the extra key is silently tolerated by Malli's open-by-default
   `:map`, so the module still LOADS, but `:set-attribute`'s own
   interpreter case reads only `(:value state)`, so BOTH attributes would
   write `nil` today). **Confirmed harmless for THIS closure regardless**
   (direct read, all four files): `pre_procedure_encounter_reason` feeds
   only an `Encounter` state's own `:reason` field, which `compile-
   trajectory`'s own `encounter->step` never reads (already-dead JSON,
   the same disposition `:reason`/`number_of_observations` already have);
   `home_health_reason` is written twice and read NOWHERE in this
   closure's own four files. Building it anyway (cheap, mirrors `:value-
   code`'s own already-built `:observation`/`ObservationEntry` precedent
   exactly, avoids a silently-wrong `nil` for any FUTURE consumer) is
   this session's own choice, not a load-bearing requirement of TJR's own
   vendoring.
2. **An embedded `MultiObservation` child's own `exact` value-sourcing
   mechanism** (`functional_status_assessments.json`'s own `PROMIS29_
   Total_Assessment`, one child: `"exact": {"quantity": 1}`, no unit) —
   D1a-3 characterized THREE value-sourcing mechanisms (`range`/
   `value_code`/`vital_sign`); `exact` is a FOURTH, not previously seen
   on any embedded child. `ObservationChild`'s own schema has no `:exact`
   field at all — Malli's open-by-default `:map` tolerates the extra
   key, so the module still loads, but `sample-observation-extra`'s own
   `cond` chain falls to its `:else {:codes codes}` branch, SILENTLY
   dropping the value (not a load failure — a wrong-answer risk).
   **Load-bearing for TJR: `PROMIS29_Total_Assessment` is reached
   whenever `Perform_Functional_Status_Assessment_Hip`'s own `assessment_
   done` branch selects `"PROMIS-29"` (one of five `distributed_
   transition`-free, i.e. author-selected, arms) — a real, reachable
   path, not an excludable tail.** Built this session: `exact` is
   SPECIFIED content (a literal, `quantity: 1` here) — the SAME
   "specify, don't fabricate" governing principle D1a/D2 already applied
   to `stroke_risk`/`joint_replacement`'s own opposite case (DELEGATED
   content) makes this the easy branch, zero rng, mirroring `Delay`'s
   own `:exact` handling exactly: `sample-observation-extra` gains an
   `:exact` branch, `{:codes codes :value (:quantity exact) :unit (:unit
   state)}`, checked ahead of the existing `:else` fallback.

**Declared D3 vendoring scope for TJR (H5): BUILDABLE, pending H4's own
compound-Guard resolution landing plus the two findings above** — every
other D2-era finding stands unchanged (D7 clean, `joint_replacement`
delegated and already resolved).

### D3e — compound-Guard analytical resolution (H4)

`Joint_Replacement_Guard`'s own exact condition, re-confirmed byte-for-
byte against the fresh fetch (D3d): `{:and [{:condition-type :attribute
:attribute "joint_replacement" :operator "is not nil"} {:condition-type
:age :operator ">" :quantity 50 :unit "years"}]}`. `age-guard-jump-days`'s
existing v1 boundary (bare `:age` condition, operator `>=` only) does not
recognize this shape at all — neither the `:and` wrapper nor the strict
`>` operator.

**Design (H1, sound-jump-or-escalate, no Synthea source citation applies
here — real Synthea resolves this via a ticked simulation clock this
project's own no-fixed-tick design (section 3) deliberately does not
have; this is this project's OWN interpreter-core analytical extension,
not a ported mechanism):**

1. **Bare `:age` conditions gain the strict `>` operator**, alongside
   the existing `>=`: the exact day-vs-year integer-age-flooring
   boundary (`age-years-at`'s own `Period/between... .getYears()`,
   floored whole years) means `age > N` first becomes true on the SAME
   day `age >= (N+1)` first becomes true (a floored age of exactly `N`
   does not satisfy strict `>`; the jump target for `>` is therefore
   `dob.plusYears(N+1)`, computed by the SAME `age-guard-jump-days`
   arithmetic already used for `>=`, `quantity` bumped by one). Other
   operators (`<`, `<=`, `==`) stay OUT — a failing `<`/`<=`/`==` age
   condition is already true-then-permanently-false or a point condition
   the passage of time cannot make MORE true, so no sound forward jump
   exists (correctly blocks, unchanged, the SAME disposition a non-`>=`
   bare condition already has today).
2. **A compound `:and` condition may be jumped when it contains EXACTLY
   ONE `:age` sub-condition (operator `>=` or `>`, per (1)) and every
   OTHER sibling is a condition type that does not itself read `ctx`'s
   own `:t` (i.e. not `:age`/`:date` — the only two v1 condition types
   whose truth value can change merely from the passage of time) AND
   already holds, evaluated against the CURRENT ctx, before the jump.**
   Soundness: the ONLY thing time can change between now and the jump
   target is the age sub-condition itself (by construction, the jump
   advances `:t` alone — no attribute is cleared, no trajectory event is
   un-emitted, and a non-`:age`/`:date` sibling's truth value is a pure
   function of persona/attributes/trajectory, none of which the jump
   touches) — so a sibling true now is still true at the jump target,
   and the age sub-condition is true at the jump target BY the same
   construction `age-guard-jump-days` already proves for the bare case.
   `Joint_Replacement_Guard`'s own two siblings satisfy this exactly:
   `Attribute joint_replacement is-not-nil` (seeded via `initial-
   attributes` before the walk starts, never cleared anywhere in TJR's
   own closure, D2's own D7 scan) holds from `:t` = DOB onward; `Age >
   50 years` is the sole blocking condition. `guard-step`'s own existing
   "no second, still-blocked branch" comment (trust-by-construction,
   never a redundant re-check) extends unchanged to the compound case —
   the proof above is what licenses that trust, not a new runtime check.
3. **Named, NOT built (no sound bound, or genuinely not exercised by
   TJR) — an ESCALATION shape, not a heuristic jump, per H4's own
   instruction:** more than one `:age` sub-condition in one `:and`
   (ambiguous which bound governs, unexercised); an `:and` containing
   `:date` alongside `:age` (two time-dependent siblings interacting,
   unexercised); `:or`/`:at-least` wrapping an `:age` condition (a
   different logical combinator, unexercised); any sibling that does
   NOT already hold at evaluation time (correctly stays BLOCKED — no
   sound jump exists merely from advancing `:t`, unchanged from today).
   Installed ≠ used (H4's own words): only the form TJR actually
   exercises is built.

---

## 15. GMF census (2026-08-03, ADR-0034): the frontier converted to data

`.agents/plans/2026-08-02-gmf-parity-plan.md` §3, ADR-0031 AR-1/AR-4:
the census tool (`ehrt.sim-trajectory.census`, a `sim-trajectory` dev
entry point under `development/src`, not a CLI verb) walks and
smoke-digests the FULL upstream catalog at this document's own pin —
superseding the hand-scouted prioritization table above (its own
superseded note, §8) as the frontier of record. Full artifact:
[`components/sim-trajectory/docs/census/2026-08-03-synthea-7e08387.edn`](census/2026-08-03-synthea-7e08387.edn).

**Run parameters (re-runnable to the byte, all recorded in the
artifact's own `:header`):** pin `7e08387c68a7f0e21d13076609a159fd473fc902`,
verified via `git rev-parse HEAD` against the checkout (not the
sha256-content fallback — a real git checkout was available); 85
top-level modules discovered; 3 seeds/module, mixer-seed `20260803`
(the same `java.util.Random`-mixer derivation
`bin/oracle-src/ehrt/oracle/digest.clj` uses); registration at age 30,
horizon 50 further years (age 30→80), uniform persona sampling
(`{}` config) — one fixed, global choice, not tuned per module.

### Verdict counts

| Verdict | Count |
|---|---:|
| `:ok-walked` | 40 |
| `:load-failed` | 39 |
| `:walk-failed` | 6 |
| `:out-of-scope-by-ruling` | 0 (the category stays reserved, ADR-0031 AR-4 — empty is fine) |
| **Total** | **85** |

19 of 85 modules carry AR-3's `:disclosed-substitutions [:wellness-timing]`
tag — the mechanical `wellness: true`/no-`encounter_class` scan, applied
regardless of verdict. This is nearly 4× ADR-0031 AR-5(a)'s own hand-
survey count of five (`mTBI`, `atrial_fibrillation`, `osteoporosis`,
`epilepsy`, `med_rec`) — all five are among the 19 (confirmed
individually), so AR-5(a)'s finding stands, but was itself an
undercount by exactly the mechanism the parity plan's §3 predicted:
a scouted sample missing real instances a full mechanical sweep finds.
Of the 19: 12 `:ok-walked` (including the already-vendored
`ear_infections`, whose own timing-substitution disclosure is
`gmf.clj`'s own `normalize-state` docstring note, ADR-0031 AR-5(b)),
1 `:walk-failed` (`med_rec`, a max-steps runaway — see below), 6
`:load-failed` (a different, unrelated gap blocks each before the
wellness idiom itself would ever run).

### Sanity anchors (Step 2's own STOP-AND-ESCALATE gate)

**All SEVEN currently-vendored roots census `:ok-walked`** —
`appendicitis`, `ear-infections`, `sepsis`, `sinusitis`, `sore-throat`,
`total-joint-replacement`, `urinary-tract-infections`. (This session's
own driving prompt named "eight" vendored roots; the actual count, both
by direct listing of `components/sim/resources/sim/modules/*.json` and
by this document's own D3f regression-baseline prose above, is SEVEN —
a small premise correction, disclosed here and in the session record,
not blocking: no ruling anywhere in `notes/ADRs.md` or the parity plan
ever said eight.) No STOP-AND-ESCALATE fired.

**All five of ADR-0031 AR-5(a)'s named wellness modules carry the
AR-3 tag**, confirming the anchor: `epilepsy`/`mTBI`/
`atrial_fibrillation`/`osteoporosis` census `:ok-walked` (with the
tag), `med_rec` census `:walk-failed` (with the tag — AR-3 is
verdict-independent by design).

### Top gap mechanisms (`:load-failed`, by modules blocked)

| Mechanism | Modules blocked |
|---|---:|
| `Counter` (deferred state type) | 11 |
| `ImagingStudy` (deferred state type) | 10 |
| `gmf_version 2` timing, `EXPONENTIAL` distribution kind (loader THROWS — new finding, below) | 7 |
| `gmf_version 2` timing, `GAUSSIAN` distribution kind (loader THROWS — new finding, below) | 4 |
| `SupplyList` (deferred state type) | 3 |
| `AllergyOnset` / `VitalSign` / `Vaccine` (deferred state types) | 1 each |
| Unrecognized lookup-table column (`time`, real Synthea's own `LookupTableTransition` special case, D3a's own "unbuilt" note) | 1 |

**New finding, this session: `ehrt.sim-trajectory.gmf`'s own
`gmf-v2-timing->v1` (D3c finding 1's own translation function) is not
exception-free.** Its `case` over a `gmf_version 2` distribution `:kind`
has clauses for `UNIFORM`/`EXACT` only; a real `GAUSSIAN` or
`EXPONENTIAL` kind (11 modules combined) throws a raw
`IllegalArgumentException` at load time rather than a `:rejected`
Result — a genuine loader robustness gap the census's own full-catalog
sweep surfaced (no hand survey ever read enough `gmf_version 2` modules
to find it). Per this session's own fence, NOT fixed here (`gmf.clj` is
the thing being observed, not touched); `census.clj`'s own `census-one`
wraps `load-closure` in `try`/`catch` so this one finding does not abort
the whole run (`ehrt.sim-trajectory.census`'s own docstring on
`census-one` has the full account). Named for a future defect-fix or
Wave I session: extending `gmf-v2-timing->v1`'s vocabulary is a small,
mechanical, high-leverage fix (11 modules, tied for the single largest
census bucket after `Counter`/`ImagingStudy`).

### `:walk-failed` mechanisms

| Mechanism | Modules |
|---|---|
| Unrecognized condition type `Race` (real Synthea condition type, ADR-0031's §2 vocabulary never named it) | `anemia-unknown-etiology`, `cystic-fibrosis`, `self-harm` |
| Unrecognized condition type `Not` (boolean negation — real Synthea's `Logic.java` `Not` class, never in this project's v1 compound-wrapper set alongside `And`/`Or`/`At Least`) | `allergic-rhinitis` |
| `max-steps` runaway (a real zero-time-advance transition cycle this interpreter's own backstop catches, per its own docstring — either a module-authoring idiom this interpreter doesn't yet resolve, or a genuine authoring bug upstream) | `med-rec`, `veteran-substance-abuse-treatment` |

Two NEW condition-vocabulary gaps (`Race`, `Not`) neither §2 nor any
prior wave named — real, mechanically found, each blocking 1–3 modules;
smaller than the state-type gaps above but genuinely new information
this table did not have before.

### Reading this census

Zero `:load-failed` (minus `:out-of-scope-by-ruling`, currently empty)
is parity's own countable definition (parity plan §1/§3). This run: 39
non-out-of-scope `:load-failed` plus 6 `:walk-failed` stand between here
and that line — the ranked mechanisms above are the E/F/G/H/I sequencing
input the parity plan's own §4 deferred to "the census ranking," a
design-channel read this session does not make (fence, below).

### D3f — regression baseline (Step 1)

Fixed-seed walks of all SEVEN currently-vendored roots (`sinusitis`,
`appendicitis`, `sore_throat`, `ear_infections`-closure, `sepsis`,
`death-fixture`, plus the CarePlan mechanism's own fixture coverage) —
`poly check` clean, full non-integration suite green, captured at this
session's own pre-Step-2 HEAD, before any D3 code change; re-run and
diffed at every subsequent checkpoint per this document's own established
method, byte-identical/count-identical at every one (confirmed one final
time at session close, Step 4).
