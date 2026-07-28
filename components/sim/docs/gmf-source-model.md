# GMF: the source model, why modules block, and the unlock ladder

[`gmf-interpreter.md`](gmf-interpreter.md) is the as-built spec of
*this project's own* Generic Module Framework (GMF) interpreter — what
v1 executes, how, and its own implementation findings. This document
is its companion, not its replacement (link both ways; this file adds
nothing to that one's own scope): where the *source* content comes
from — how a Synthea module is actually structured and how Synthea
itself runs one — why so much of Synthea's current 85-module catalog
(`notes/facts-register.md` F2) still fails to clear this project's own
curation bar, with the receipts; and, because the author wants this
reasoning preserved even where the work itself is milestone-scale and
deferred, the ladder of extensions that would unblock more of it, each
rung's design sketch and honest cost class recorded now, for whichever
future session picks it up.

## Part A — the Synthea source model

### A.1 A Synthea GMF module, as JSON

One JSON file per disease (`src/main/resources/modules/` in
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea),
mined and re-verified against real module content at pinned commit
`7e08387c68a7f0e21d13076609a159fd473fc902`, `.agents/memory/architecture.md`
and [`gmf-interpreter.md`](gmf-interpreter.md)'s own provenance
discipline). Structurally, a module is a **map of named states**, each
carrying a `type` and exactly one transition. States fall into three
kinds by what they *do*, not by any grouping Synthea's own schema
names explicitly:

- **Control-flow** — `Initial`, `Terminal`, `Simple`, `Delay`, `Guard`.
  No clinical content of their own; they route the walk.
- **Record-writing** — `ConditionOnset`/`ConditionEnd`,
  `Encounter`/`EncounterEnd`, `Procedure`, `Observation`,
  `MedicationOrder`/`MedicationEnd`, and further types this project's
  own v1 doesn't yet execute (`CallSubmodule`, `Death`, `CarePlanStart`/
  `CarePlanEnd`, `Counter`, `MultiObservation`, `ImagingStudy`,
  `DiagnosticReport`, `Physiology` — Part B, below). These are where a
  module actually asserts a clinical or operational fact.
- **Bookkeeping** — `SetAttribute`, `Symptom`. Write into a shared
  fact store; no clinical event of their own.

Every non-terminal state carries **exactly one transition**, of four
kinds: `direct_transition` (unconditional, one target), a weighted
`distributed_transition`, a first-match `conditional_transition`
(an ordered list of `(condition, target)` pairs), and `complex_transition`
(`conditional_transition` and `distributed_transition` composed — an
ordered list of conditions, each guarding its own nested distribution).
Conditions are predicates over age, sex, attributes, and `PriorState`
(a query over the module's own visit history — A.2, below) in the
subset this project's own v1 interpreter recognizes
([`gmf-interpreter.md`](gmf-interpreter.md) section 2); Synthea's own
condition vocabulary is considerably larger (Part B item 4).

Clinical codes ride **inline**, as `{system, code, display}` triplets
on the state that asserts them — a `ConditionOnset`'s own SNOMED code,
an `Observation`'s own LOINC code, a `MedicationOrder`'s own RxNorm
code — never in a separate table a state merely references. This is
exactly what makes **code passthrough** (this project's own law,
[`sim-theory.edn`](sim-theory.edn)'s `:trajectory` stage) a
near-mechanical carry-forward rather than a translation: the source
data already arrives shaped the way this project's own
`{:system :code :display}` triplet wants it.

### A.2 How Synthea itself runs a module

Distinct from the JSON's own shape, and worth stating separately: how
the *real* Synthea Java engine executes this content, mined from
`Person.java`/`HealthRecord.java` and the Generic Module Framework wiki
(`.agents/memory/architecture.md`; independently re-verified via
[`docs/research/SimHospital-Synthea-limitations-considered.md`](research/SimHospital-Synthea-limitations-considered.md)'s
own source-level citations — cited here rather than re-fetched fresh,
per this document's own provenance discipline).

- **`Person.attributes` is an open, shared blackboard.** A single
  `Map<String,Object>` per patient, written and read by every module
  currently active for that patient — nothing enforces which module
  owns which key; convention is the only boundary.
- **Per-module cursors live in that same map.** Each module stores its
  own current-state pointer keyed by its own module name inside
  `Person.attributes` — there is no separate per-module state object.
- **A fixed tick loop, roughly seven days, from birth to death**
  ([`patient-state-model.md`](patient-state-model.md#design-inputs-mined-from-upstream)'s
  own mining record) — because Synthea's own horizon is a whole
  simulated lifetime, not one encounter, a polling loop is how *every*
  active module gets a chance to advance, whether or not anything
  relevant happened that week.
- **Guards query a mutable visit-history trail, `Person.history`,**
  rather than a durable, replayable log — the person's own recorded
  trail of visited module states, walked to answer "was I in state X
  recently" ([`event-sourcing.md`](event-sourcing.md#the-upstream-contrasts-what-happens-without-this-made-explicit)'s
  own retelling of the same mining record).
- **Every module a person's life makes eligible runs concurrently**
  against that same shared `Person`, coordinating only through
  attributes one module writes and another reads
  ([`patient-state-model.md`](patient-state-model.md)'s own mining
  section) — out of a real catalog large enough (85 modules,
  `notes/facts-register.md` F2) that many can plausibly be active for
  one person at once.

**Composition pain, documented by Synthea's own maintainers and users,
not inferred.**
[`docs/research/SimHospital-Synthea-limitations-considered.md`](research/SimHospital-Synthea-limitations-considered.md)
§4.2 mines three separate, citable instances: bare-string attribute
keys mean nothing stops one module from silently reading (or
colliding with) another's own convention-only namespace; hard-coded
Java lifecycle modules run always-on and invisibly even when a user
asks to run only their own custom module set — maintainer Jason
Walonoski's own answer, that doing so "might not behave the way you
expect," because those modules "still execute" regardless (discussion
#1126); and a practitioner asking whether adding vital signs to every
emergency encounter required finding and individually editing every
module that opens one (issue #780) — there was no cross-cutting
mechanism to answer "yes, but not that way."

### A.3 The contrast this project's own port already embodies

Not a claim that Synthea's own choices were wrong for Synthea's own
much larger problem (a whole simulated lifetime, not an encounter
horizon) — a contrast this project's own architecture already draws,
named here in one place rather than left scattered across the docs
that each made one piece of it:

| Synthea | This project |
|---|---|
| Fixed ~7-day tick, polling every active module | No fixed tick at all — the interpreter's history phase reuses the exact per-state transition-sampling logic the horizon phase uses, chaining state to state to the next relevant instant ([`gmf-interpreter.md`](gmf-interpreter.md) section 3); the engine's own discrete-event queue never ticks either — "quiet hours cost nothing" ([`GLOSSARY.md`](GLOSSARY.md)'s own DES entry) |
| A whole simulated lifetime | The history/horizon split: a fast-forwarded pre-registration history compresses a persona's full life into attributes and pre-horizon facts, then a bounded horizon phase emits real trajectory events inside this run's own encounter-horizon scope (ADR-0007 point 3) |
| `Person.history`, a mutable visit-trail approximation of a log | `PriorState` compiles directly to a query over this project's own ground-truth log — "the log IS `person.history`, done right" ([`gmf-interpreter.md`](gmf-interpreter.md) section 2) |
| `Person.attributes`, one flat, bare-string map shared by convention | A module-namespaced `:attributes` registry — every write auto-namespaced by its own module id, so cross-module collisions are structurally impossible, not merely avoided by discipline ([`gmf-interpreter.md`](gmf-interpreter.md) section 5) |
| Hidden, always-on Java lifecycle modules | No hidden modules: every module this project ever runs is an explicit, listable load (`ehrt.sim.gmf/loaded-modules`) — [`sim-theory.md`](sim-theory.md)'s own IR-transforms-as-composition-layer corollary, restated at the M5 roadmap entry |

## Part B — why so many modules block here, with the evidence

`docs/gmf-interpreter.md`'s own M7 survey — 41 real Synthea modules
read at the pinned commit above, 10 formally plus an extended
histogram-scouted pass — is the evidence base; this section
synthesizes its four stacked walls rather than repeating its own
per-module tables (cited by section below, not reproduced).

### Wall 1 — the loader's all-or-nothing gate

`ehrt.sim.gmf/load-module` rejects a module on the bare
**presence** of any state type outside v1's recognized set, full stop
— not on whether that state is actually *reachable* by a real patient.
This is not a hypothetical worst case; it is the single best-evidenced
finding in the whole M7 survey. `self_harm.json` carries two deferred
states, both on genuinely rare tails (a `Death` on a fatal-attempt
branch documented at roughly 1.6–5.5%, and a `CarePlanStart` inside a
second, ambulatory follow-up encounter that would be moot regardless
under Wall 4's own truncation gap) — structurally isolated exactly the
way `sinusitis.json`'s own `Device`/`DeviceEnd` gap was, the precedent
that *did* clear v1 by being promoted to a consumed-internally state.
The loader still rejects `self_harm.json` outright, because it gates
on presence, not reachability — the M7 survey's own words for it: "the
strongest evidence yet for a reachability-aware load gate." `stroke.json`
independently confirms the same shape: its own `Death` state is an
excludable ~17.5% procedural-mortality tail, the *only* sinusitis-precedent-shaped
gap the whole session found — and still blocks the module at load
time, before its own separate Wall 4 problem (below) is even reached.

### Wall 2 — `CallSubmodule` opacity

`CallSubmodule` is a state that hands control to a *second* module
JSON file — recursion this project's v1 interpreter does not yet
follow. The cost is not "one state out of many": every module the
survey found using it loses its **entire downstream therapeutic
content**, not a fraction proportional to state count. `ear_infections.json`
routes both of its medication-prescribing branches (antibiotic and
OTC-painkiller) through a submodule — the module's whole therapeutic
payload is opaque past that point. `myocardial_infarction.json` is
worse: every branch reachable immediately after its own `ECG` state —
`ACS_Arrival_Meds`, `Cardiac_Labs`, `NSTEACS`, `STEMI` — is a
`CallSubmodule`, unconditionally, so the module's entire post-ECG
content is invisible to this project. `congestive_heart_failure.json`
carries seven separate `CallSubmodule` states across its own 115-state
graph. This is, by the M7 survey's own headline finding, the single
largest blocker in the whole catalog — not a tie with any other
deferred feature (Part C rung 4, below, has the count).

### Wall 3 — Death as the tax on seriousness

Every module the survey read that models a genuinely life-threatening
condition carries a `Death` state — 12 or more confirmed instances
across the 41 modules read at any depth
(`congestive_heart_failure`, `sepsis`, `myocardial_infarction`,
`stroke`, `self_harm`, `gallstones`, `epilepsy`, `spina_bifida`,
`cystic_fibrosis`, `breast_cancer`, plus several histogram-only hits).
`Death` is the closest thing this survey found to a *tax levied on
authoring anything clinically serious* — and, on this session's own
evidence, a uniformly safe one to pay: not one of those 12+ instances
sits on a mandatory path. The cautionary record here is `spina_bifida.json`
itself: first mis-characterized, mid-session, as vendorable because
its one `Death` state (fired across several genuinely rare tails —
roughly 6.1% day-1-survival, 2% post-op, 1% under-age-5, 0.5%
living-with-SB) looked exactly like the `sinusitis.json` `Device`/
`DeviceEnd` precedent that *had* already cleared v1. It hadn't:
`Device`/`DeviceEnd` were promoted into
`ehrt.sim.gmf`'s own recognized state-type set at M5b; `Death`
never was — attempting to vendor `spina_bifida.json` test-first caught
the loader's own `:unsupported-state-type` rejection immediately,
before any commit. The design lesson this near-miss sharpens for
Part C rung 2, below: `Death` cannot be given the same *consumed-
internally, no trajectory event* treatment `Device`/`DeviceEnd`
received — a real death has to actually **mint** a transition into
this project's own `:expired` status (`patient-state-model.md`'s
accumulator, `clinical-realities.md`'s post-mortem entry), not pass
through inertly the way an untracked piece of equipment can. `Death`
is deliberately deferred to that captured expired/post-mortem
machinery rather than ported as its own standalone mechanism
([`gmf-interpreter.md`](gmf-interpreter.md)'s own deferred-type table).

### Wall 4 — predicate-vocabulary gaps, the sleeper

The cleanest **state-type** surface the whole M7 survey found, after
`appendicitis.json` itself, is `stroke.json` — 12 states, only one
deferred type (`Death`, an excludable ~17.5% tail, Wall 3's own
territory). And it is still deferred, because
`Emergency_Encounter`'s own `conditional_transition` gates
Clopidogrel/Alteplase prescribing on a **mandatory-path `Date`
condition** (simulated calendar year), evaluated for every patient
immediately on encounter entry — a condition type entirely outside
this project's own v1 vocabulary (age, sex, attribute, `PriorState`,
plus M5b's log-query additions, [`gmf-interpreter.md`](gmf-interpreter.md)
section 2). This is the sleeper wall precisely because a **state-type
histogram cannot see it at all** — every state `stroke.json` uses is a
recognized v1 type; the gap lives one layer down, inside a
transition's own condition, invisible to any survey technique that
only counts state *types*. This is exactly why the M7 survey's ten
formal candidates required full reads rather than histogram scouting
alone: a clean-looking state-type score is necessary but not
sufficient evidence a module will actually load and run. Two
compounders sit alongside this wall, neither a state-type or
condition-vocabulary gap of its own: the **multi-encounter-per-episode
compile-time truncation** — `ehrt.sim.compile-trajectory`'s own
`encounter-closed?` mechanism (built to stop a module recurring across
a whole lifetime from minting a second admission, ADR-0007 point 3's
own encounter-horizon scope) also silently drops a real, same-episode
second encounter, confirmed content-relevant in two modules this
session read (`appendicitis`, `total_joint_replacement`); and the
**Observation-bearing hunt**, which came back genuinely empty —
every module across all 41 read that fires an `Observation` state is
blocked by something else, confirmed rather than assumed after a
dedicated extended pass looking specifically for a counterexample.

## Part C — the unlock ladder

Ordered by what each rung unblocks, cheapest and most locally-scoped
first. Each rung names what it is, what it admits (citing the M7
survey's own counts), a design sketch with its open questions, and an
honest cost class — the reasoning the author wants preserved even
though every rung here is milestone-scale, deliberately-deferred work,
not this session's own scope.

### Rung 1 — a reachability-aware load gate (loader-only)

**What it is.** `ehrt.sim.gmf/load-module` stops rejecting a
module on the bare *presence* of a deferred-type state and starts
asking whether that state is actually reachable. **The semantics
question that has to be ruled on before any code:**

- **Load-with-neutralization (the recommended lean).** A deferred-type
  state still loads — as a **blocking terminal** for whichever branch
  reaches it. If a real patient's walk ever actually reaches it, it
  emits a real, visible, deterministic truth event (something in the
  spirit of `:step-rejected`, ADR-0012's own precedent for "an
  attempted thing this system doesn't support, made honest rather than
  silently dropped") rather than crashing or vanishing. The whole
  authored branch stays present in the graph, with its own
  distribution weight unchanged.
- **Prune-at-load.** Simpler to build — strip the unreachable branch
  out of the graph entirely before the interpreter ever sees it. But
  this silently rewrites the authored graph, against this project's
  own verbatim-vendoring grain (the same "inspectable by `git show`"
  argument ADR-0013 already makes for vendoring modules unmodified in
  the first place).

**The interplay worth naming before either is built:** pruning a
branch changes the *effective* probability mass of its siblings under
a `distributed_transition` or `complex_transition` — a pruned 2% tail
slightly reweights the remaining 98% toward each other, a real
(if small) statistical drift from the authored module's own intent.
Neutralization does not: the branch stays in the graph at its own
authored weight, and only what happens *if* it fires changes.

**What it admits.** Not a full vendor-readiness bar by itself
(`Death`'s own real meaning still waits on Rung 2) — but it is what
lets a module like `self_harm.json` or `spina_bifida.json` load and
run at all, rather than being flatly rejected the moment any deferred
type appears anywhere in the file. `self_harm.json`'s own two deferred
states (the `Death` tail, and a `CarePlanStart` inside a second
encounter — moot regardless under Wall 4's own truncation gap) are
exactly the shape this rung targets.

**Cost class:** loader-local. Touches `ehrt.sim.gmf`'s own
state-type gate and nothing else — no interpreter change, no engine
change, no provenance-chain change.

### Rung 2 — Death mints the transition into `:expired`

**What it is.** `Death` is promoted from "deferred, full stop" to a
real, interpreted state type — but not the same *way* `Device`/
`DeviceEnd` were (Wall 3's own cautionary point). `Device`/`DeviceEnd`
pass through inertly, consumed-internally, because this project has no
equipment-tracking concept for them to write into. `Death` cannot: a
real death has to **mint** the transition into this project's own
`:expired` status, activating the post-mortem event-validity rows
already captured and waiting
([`patient-state-model.md`](patient-state-model.md)'s event-validity
table — morgue/funeral-home transfer, autopsy/specimen events, and,
gated on a `:donor` attribute, donor-management/procurement) and
terminating that module's own walk for the patient (a death is not a
state a module resumes from). Two open design questions this rung
would have to settle before code: how the walk's own termination
interacts with the encounter horizon (death occurring *inside* the
currently-open encounter versus *outside* one entirely — different
shapes for what CompileTrajectory should emit), and how the resulting
`:expired` transition rides the same `:citation` provenance chain
every other trajectory event already carries, so a death event stays
just as auditable back to its own module state as everything else.

**What it admits.** On this session's own evidence, the single
cheapest, highest-confidence rung in this whole ladder: every one of
the 12+ `Death`-bearing modules the M7 survey found has it on a
genuinely excludable tail, never once on a mandatory path — and
`spina_bifida.json` (Wall 3's own near-miss) is ready to vendor the
day this rung lands, no further survey work needed. It is also the
rung that finally opens the door to the donor-management pathway
content `clinical-realities.md`'s own post-mortem entry already
describes in full but has never had a real module to source from.

**Cost class:** interpreter plus a real new step type. Touches
`ehrt.sim.gmf-interpreter` (a new `:death` case in `step`),
`ehrt.sim.compile-trajectory` (mapping a death trajectory event
to whatever ground-truth event mints `:expired`), and the engine's own
event-validity enforcement for the post-mortem rows that already exist
on paper but are not yet checkable in code (`patient-state-model.md`'s
own note that this row "is not yet checkable in code, since `:expired`
isn't a landed `:status` value yet").

### Rung 3 — predicate vocabulary, `Date` first

**What it is.** A new condition type, `Date`, joining v1's existing
four (age, sex, attribute, `PriorState`) plus M5b's own log-query
additions. Mechanics: a simulated-date predicate evaluated against the
run's own reference-date and the interpreter's own virtual clock — the
same `persona`-anchored calendar this project already uses for DOB and
age arithmetic
(`ehrt.sim.gmf-interpreter`'s own `epoch-day` clock,
[`gmf-interpreter.md`](gmf-interpreter.md)'s own time-model note), just
compared against a year threshold rather than an age one. Once `Date`
exists, the survey's own remaining observed condition-vocabulary gaps
are the natural next additions, in the order the survey actually
encountered them: **Vital Sign** and **Active CarePlan** (both
observed on `congestive_heart_failure.json`'s own transitions), **Or**
(a boolean compound, `congestive_heart_failure.json` again), and
**Observation-as-condition** (the same predicate gap `sore_throat.json`'s
own mandatory-path `At Least`-N-of compound and `sepsis.json`'s
condition vocabulary both stand on).

**What it admits.** By the M7 survey's own account, `Date` alone
admits `stroke.json` — the cleanest state-type surface surveyed after
`appendicitis.json` itself, whose only other gap (`Death`, an
excludable ~17.5% tail) is already handled by Rungs 1 or 2 landing
ahead of this one in the ladder.

**Cost class:** narrow and mechanical — a numeric year comparison
against `sim-config`'s own reference-date, one new case in
`evaluate-condition` and its own loader-side normalization entry. On
this session's own account, likely the second-cheapest fix in the
whole prioritization table, after `Date` itself.

### Rung 4 — `CallSubmodule` (an M-scale milestone, not a task)

**What it is.** Recursive module loading: a `CallSubmodule` state
loads a *second* module JSON file, whose own states the walk enters
and whose own transitions it follows until returning to the caller.
The scope sketch worth preserving, not built or estimated in detail
here: the called submodule joins the attribute registry under **its
own** namespace (`ehrt.sim.gmf`'s existing per-module namespacing
discipline, section 5, extended one level deeper); the interpreter's
own cursor/state-folding logic has to fold correctly across the call
boundary (entering a submodule, and returning from one back into the
calling module's own next state); the provenance chain gains a fourth
link — submodule state → calling state → trajectory event → IR step —
so a compiled step sourced from inside a submodule remains fully
auditable back through the module that called it, not just to the
submodule in isolation; the loader's own all-or-nothing gate has to be
evaluated over the **closure** of every module a `CallSubmodule` chain
can reach, not just the top-level file; and each submodule file needs
its own vendoring provenance record (`resources/modules/NOTICE`'s
existing per-file discipline, ADR-0013 point 3, extended to however
many submodule files a single vendored disease module actually pulls
in).

**What it admits.** By far the largest single unlock in this entire
table: roughly 24 of the 41 modules read at any depth this session are
`CallSubmodule`-blocked (20 confirmed from the ten formally-read
modules plus `ear_infections.json`; 24 counting the histogram-scouted
extension) — more than every other deferred feature in the
prioritization table combined. This is the rung that opens the
catalog's richest half: shared medication-regimen and referral
submodules (`medications/*`, `heart/*`, `dme/*`,
`total_joint_replacement/*`, `anemia/*`) are how most of Synthea's
*current* module library actually authors its own therapeutic content
— not a rare idiom a handful of modules happen to use.

**Cost class, and why it is last despite the biggest unlock.** Every
rung above this one is local to a single component — Rung 1 touches
only the loader, Rung 2 touches the interpreter plus one new engine
event type, Rung 3 touches one condition-evaluation case. `CallSubmodule`
touches the loader (the closure-evaluated gate), the interpreter (cursor
folding across a call boundary), the provenance chain (a fourth link),
*and* the vendoring discipline (per-submodule NOTICE records) — all at
once. It is an M-scale milestone in its own right, not a task that
fits alongside three narrower rungs, which is exactly why it is named
here, ordered, and left for its own future session rather than
estimated further.

## See also

[`gmf-interpreter.md`](gmf-interpreter.md) for this project's own
as-built v1 interpreter spec — the state-type table, the condition
vocabulary, the history/horizon design, and the M5b/M7 findings this
document's Part B synthesizes from without repeating.
[`trajectory-computation.md`](trajectory-computation.md) for how a
vendored module's own trajectory becomes pathway IR once it *does*
clear the bar this document's Part B describes. `notes/ADRs.md`
ADR-0013 for the vendoring decision (target, provenance, curation
criterion) this document's Part B measures every surveyed module
against.
