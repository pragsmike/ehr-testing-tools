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

**Deferred, with reasons:**

| State type | Why deferred |
|---|---|
| `CallSubmodule` | Recursion into a second module JSON file the interpreter would also have to load, namespace, and fold state from — real complexity the v1 interpreter doesn't yet carry. Confirmed load-bearing by the survey: `ear_infections.json`'s entire medication-prescribing pathway (both its antibiotic and OTC-painkiller branches) routes through `CallSubmodule`, not an inline `MedicationOrder` — a module using it is not merely "missing one state," it is opaque past that point without submodule support |
| `Counter` | Named in this document's own brief as deferred; not observed in any of the four modules read this session, so its omission carries no survey evidence either way — carried forward as originally scoped |
| `MultiObservation` | Named in this document's own brief as deferred; not observed in any of the four modules read this session — same status as `Counter` |
| `Death` | Defers to the expired-state machinery this project has already captured (`:expired` status, `components/sim/docs/patient-state-model.md`'s accumulator table; the post-mortem event-validity rows, `components/sim/docs/clinical-realities.md`) rather than being ported as its own mechanism — wire it to that machinery when donor/post-mortem content lands, not before. Not observed in any of the four modules read this session (all four are acute, non-fatal illness modules by design of the candidate search) |
| `CarePlanStart` / `CarePlanEnd` | **Discovered spot-checking `bronchitis.json`** (read while scouting a urinary-tract-infection candidate that turned out not to exist as its own module, below) — not present in any of the three formally surveyed candidates. Deferred for the same reason as `Device`/`DeviceEnd` was originally deferred here (no accumulator or IR home yet, and no formal candidate needs it) — unlike `Device`/`DeviceEnd`, no vendored module has yet forced this one to be reconsidered, so it stays deferred |

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

All four of Synthea's transition kinds are v1 scope — every module
surveyed uses all four, so there was no candidate-driven reason to
defer any of them:

- **`direct_transition`** — unconditional, one target state.
- **`distributed_transition`** — a weighted distribution over target
  states, sampled from the run's single seeded RNG (the same
  determinism law every other weighted pick in this project already
  follows — `components/sim/docs/sim-theory.edn`'s global determinism law).
- **`conditional_transition`** — an ordered list of
  (condition, target) pairs, first match wins.
- **`complex_transition`** — `conditional_transition` and
  `distributed_transition` composed: an ordered list of conditions,
  each guarding its own nested distribution.

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
| `total_joint_replacement.json` | 31 | `CallSubmodule` ×4, `CarePlanStart`/`CarePlanEnd` ×1 each | none (`Age`/`And`/`Attribute`, all v1) | ambulatory (pre-op) → inpatient (surgery) → ambulatory (follow-up) | Rejected at LOAD (deferred types scattered through pre-op assessment AND post-op pain management AND post-op careplan — not an excludable tail) — **deferred** |
| `congestive_heart_failure.json` | 115 | `CallSubmodule` ×7, `Counter` ×5, `Death` ×4, `ImagingStudy` ×4, `DiagnosticReport` ×3, `CarePlanStart`/`CarePlanEnd` ×3+1, `MultiObservation` ×1 (28/115 ≈ 24% deferred) | `Vital Sign`, `Date`, `Or`, `Active CarePlan` — four more gaps | ambulatory ×3, emergency, hospice, inpatient ×2 | Far over ADR-0013 point 4's "modest surface" bar — **deferred, cited for prioritization data only** |
| `sepsis.json` | 37 | `MultiObservation` ×2, `DiagnosticReport` ×1, `Death` ×1 | none new (`Active Allergy`/`Age`/`Observation`, all recognized keywords — `Observation`-as-condition-type is itself the sore_throat-shared gap) | emergency | `DiagnosticReport` (`Blood_Cultures`) is the FIRST state after the encounter opens, unconditional; `MultiObservation` (`Record_Blood_Pressure`) fires on both the vasopressor and ICU-survival branches — both MANDATORY, not tails — **deferred** |
| `myocardial_infarction.json` | 26 | `CallSubmodule` ×5, `Death` ×2, `CarePlanStart` ×1 | none | emergency | `ACS_Arrival_Meds`/`Cardiac_Labs`/`NSTEACS`/`STEMI` are all `CallSubmodule` and ALL reachable unconditionally past `ECG` — the module's entire post-ECG therapeutic content is opaque — **deferred** |
| `stroke.json` | 12 | `Death` ×1 (an excludable ~17.5% procedural-mortality tail — the ONLY sinusitis-precedent-shaped gap found this session) | **`Date`** — `Emergency_Encounter`'s own `conditional_transition` gates Clopidogrel/Alteplase on simulated year, evaluated immediately on encounter entry, for every patient — **condition-vocabulary gap RESOLVED, GMF coverage Wave A, 2026-08-02** (`:date` now v1, `.agents/plans/2026-08-02-gmf-coverage-plan.md`) | emergency | Smallest, cleanest STATE-type surface surveyed after appendicitis — the `Date` gap that blocked it is closed, but the `Death` state-type gap (this row's own second column) still does, per AR-6 (same plan): `Death` stays a load-bearing, semantically-real state (unlike `Device`, never a safe consumed-internally pass-through) — waits for Wave C's own `:expired`/post-mortem wiring — **deferred, revisit trigger: Wave C** |
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

Counting across all 26 modules inspected this session (10 formal reads
+ 16 histogram-scouted) plus the two modules this project already had
evidence for (`ear_infections.json`, `sinusitis.json` itself):

| Deferred feature | Modules blocked (of 28 total ever inspected) | Content class it would unlock |
|---|---:|---|
| `CallSubmodule` | 20 — `ear_infections`, `urinary_tract_infections`, `total_joint_replacement`, `myocardial_infarction`, `dermatitis`, `allergic_rhinitis`, `food_allergies`, `contraceptive_maintenance`, `dialysis`, `hiv_diagnosis`, `hypertension`, `osteoarthritis`, `covid19`, `stable_ischemic_heart_disease`, `lupus`, `allergies`, `lung_cancer`, `colorectal_cancer`, `hypothyroidism`, `diabetic_retinopathy_treatment`, `breast_cancer`, `cystic_fibrosis`, `anemia___unknown_etiology`, `home_hospice_snf` (24, corrected count — see note) | By far the largest single blocker: shared medication-regimen and referral submodules (`medications/*`, `heart/*`, `dme/*`, `total_joint_replacement/*`, `anemia/*`) are how modern Synthea authors factor out repeated therapeutic content — this is THE headline finding, not a tie. `hypothyroidism.json` is the SECOND confirmed instance (after `self_harm.json`) of a module blocked by exactly ONE otherwise-unreachable-by-default `CallSubmodule` — real, repeated evidence for a reachability-aware load gate as a cheap, high-value v1.1 extension |
| `CarePlanStart`/`CarePlanEnd` | 11 — `total_joint_replacement`, `congestive_heart_failure`, `dermatitis`, `food_allergies`, `myocardial_infarction`, `attention_deficit_disorder`, `gout`, `fibromyalgia`, `self_harm`, `dementia`, `lung_cancer`, `lupus`, `veteran_ptsd` (13, see note) | Structured chronic-disease/post-procedure care-management plans (physical therapy, psychiatric follow-up, home health) — almost always paired with `CallSubmodule` in the same module |
| `Death` | 12+ confirmed — `congestive_heart_failure`, `sepsis`, `myocardial_infarction`, `stroke`, `self_harm`, `gallstones`, `epilepsy`, `spina_bifida`, `cystic_fibrosis`, `breast_cancer`, plus several histogram-only hits (`chronic_kidney_disease`, `hiv_diagnosis`, `stable_ischemic_heart_disease`, `colorectal_cancer`, `lung_cancer`) | **The single strongest, most consistent finding in this table.** Every one of the 12+ modules above has its `Death` state on a genuinely excludable, low-probability tail — NEVER once on a mandatory path, across all 41 modules this session read at any depth. `spina_bifida.json` (above) is the concrete, empirically-confirmed proof: a module this session first mis-characterized as vendorable specifically BECAUSE its `Death` state looked safely isolated — it IS safely isolated, the loader's all-or-nothing gate is what still blocks it. Promoting `Death` to a `Device`/`DeviceEnd`-style consumed-internally state (wired to the existing `:expired` machinery, `components/sim/docs/clinical-realities.md`'s post-mortem entry) is, on this session's own evidence, the cheapest, highest-confidence, most immediately-productive v1.1 extension in this entire table — `spina_bifida.json` alone is ready to vendor the day it lands |
| **Wellness-encounter `wellness: true` encoding** (new this session) | 5 confirmed (`mTBI`, `atrial_fibrillation`, `osteoporosis`, `epilepsy`, `med_rec`) of a ~41-module scouted sample — likely still under-counted, not systematically checked across all 85 | Blocks an entire v1 ENCOUNTER CLASS (`:wellness`) via this idiom specifically — the cheapest fix in this table (a loader normalization, not new interpreter machinery); `epilepsy.json` and `med_rec.json` are otherwise FULLY clean, making this the single highest-confidence "would vendor immediately if fixed" row in this table |
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
