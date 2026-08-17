## ADR-0106 — Injuries B2 assessment: the nested-encounter gap characterized, vendoring still deferred

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-11.

### Context

Prior: ADR-0070 (2026-08-07) deferred `injuries.json`'s own eight-file
closure WHOLE on a `run-submodule exceeded max-steps` defect
(`injuries/broken_jaw.json`'s own dangling `dental_referral` gate).
ADR-0105 (2026-08-11, this arc's own B1) closed that exact defect —
`run-submodule` now respects the horizon, `max-steps` now counts only
zero-advance steps — and its own real-content scratch probe found the
fix's own effect complete (0/120 `max-steps` failures post-fix, at
ADR-0070's own probe parameters) but ALSO found a SEPARATE,
pre-existing gap still firing: 2 of the SAME 120 walks fail on
`Assert failed:... nested :encounter -- this project's GMF subset
assumes encounters never nest`, the SAME two seeds failing both
before and after the B1 fix (confirmed by ADR-0105's own git-stash
seed-by-seed diff).

This session (B2, the author's own 2026-08-11 ruling "b," a WIDENED
charter: attempt the vendoring batch under the standing ceremony, and
if the nested-encounter assert fires at the round-trip gate, the
session's deliverable BECOMES the full characterization of that gap
under the ADR-0070 bail-out precedent, and nothing is vendored) ran
the fresh gate honestly. It fired. This ADR is the characterization.

### Tag ceremony

Design channel verified the ADR-0105 landing (`1abee30`) by fresh
public clone. `stable-20260811-interpreter-horizon-budget` tagged
annotated at `1abee30`, message "interpreter horizon/budget fix
landed, design-channel-verified 2026-08-11 (ADR-0105)"; pushed;
peeled ref confirmed `1abee30bb7be5f7b7d0d3b7fe5a4326556935cae` —
exact match; remote had not moved (`origin/main` was already at
`1abee30` at session start, confirmed by `git fetch` before tagging).

### Fresh gate at the pin (AR-VB4-1 discipline)

Closure re-enumerated fresh against the pin
(`/home/mg/synthea-checkout`,
`7e08387c68a7f0e21d13076609a159fd473fc902`, working tree clean,
re-confirmed) by grepping every `"submodule"` reference across
`injuries.json`, `injuries/broken_jaw.json`,
`snf/skilled_nursing_facility.json`, and `dme/wheelchair.json`: the
closure resolves to exactly the same 8 files ADR-0070 found (root plus
7 called submodules), zero lookup-table/CSV references anywhere in the
closure (checked by grepping for `lookup_table`/`transition_table` in
all 4 files — none found, so no undercount of the ADR-0070 AR-VB1-2
class this time).

**Disclosed correction to the driving prompt's own closure-membership
claim, verify-then-act (the fences' own explicit instruction).** The
prompt named 4 already-vendored members (`medications/
ear_infection_antibiotic.json`, `medications/otc_pain_reliever.json`,
`medications/moderate_opioid_pain_reliever.json`,
`dme/wheelchair_end.json`) and 4 new (`injuries.json`,
`injuries/broken_jaw.json`, `snf/skilled_nursing_facility.json`,
`dme/wheelchair.json`) — ADR-0070's own count, verbatim, carried
forward as a prior map. Re-verifying against the CURRENT vendored tree
found `dme/wheelchair.json` ALREADY VENDORED, byte-identical
(`diff` clean) — it landed 2026-08-07 via `osteoarthritis.json`'s own
closure in vendoring batch 2 (ADR-0071, same day as ADR-0070 but a
later session), a fact ADR-0070 could not have known at its own
writing time (`osteoarthritis.json` had not landed yet) and nothing
since has corrected. The closure's TRUE current disposition is **5
already vendored, byte-identical at the pin** (the 4 ADR-0070 named,
plus `dme/wheelchair.json`), **3 genuinely new**: `injuries.json`
itself, `injuries/broken_jaw.json`, `snf/skilled_nursing_facility.json`.
All 5 already-vendored members re-verified byte-identical this session
(`diff -q` against the pin, zero deltas); the 3 new members fetched to
SCRATCH only (`sha256sum` recorded, never committed, cleaned before
this record's own commit); `.gitattributes`' `components/sim/
resources/sim/modules/** -text` rule confirmed by grep to already
cover every path this batch would touch, new subdirectories included.

**Attribute-gate check (AR-VB4-2 discipline).** `injuries.json`'s own
`Initial` state direct-transitions straight to `Wait_For_Injury` — no
`Guard`, no attribute gate, matching ADR-0070's own "root only, no
seeding needed" finding and this session's own probe (real content
produced with `:initial-attributes {}`, no seeding). `broken_jaw.json`'s
own `dental_referral` never-cleared gate (ADR-0070's original bail-out
cause) is the exact never-cleared-attribute-gate class AR-VB4-2 flags
— now HANDLED by ADR-0105's horizon truncation (confirmed this
session: 0/120 `max-steps` failures at that exact branch, see below),
not a disqualifying finding for this session.

### The round-trip probe: BOTH legs fired the nested-encounter assert

**Leg 1 — direct interpreter, ADR-0070's own method (registration age
30, 50-year horizon, well-mixed seeds, 120 walks, mixer-seed
20260803).** A scratch script (never committed, cleaned before this
record's own commit) loaded `injuries.json`'s real closure directly
from the pinned checkout via `gmf/load-closure` (the same
`resolve-fn`/`table-resolve-fn` shape `census.clj`'s own
`make-resolve-fn`/`make-table-resolve-fn` use), then ran
`interp/run-module` directly at census parameters for the SAME
120-seed mixer derivation `census.clj`'s own `mixed-seeds` uses.
Result: **2 of 120 walks fail**, both on the SAME assert:

```
Assert failed: ehrt.sim-trajectory.gmf-interpreter: nested :encounter --
this project's GMF subset assumes encounters never nest (Wave H's own
fold discipline, ADR-0042)
(nil? (open-encounter-index (:trajectory ctx)))
```

Failing seeds (mixer-derived `.nextLong` values, mixer-seed 20260803):
`-576131918266266247` and `-5690589783821964774`. This independently
reproduces ADR-0105's own qualitative finding (2/120, same class) at
this session's own fresh run — not merely cited forward.

**Leg 2 — engine-level population run at the round-trip test's own
parameters** (seed 20260802, 300 patients, `:module-horizon-days`
36500 — the SAME convention every landed vendored root's own
`vendored-<module>-test` uses, e.g. `vendored_asthma_test.clj`).
`engine/run` was called directly (the same `run-config` shape a real
`vendored-injuries-test` would use, never written as a committed test
since Branch C lands no test) against the same freshly-loaded closure.
**Result: `engine/run` itself THROWS, uncaught, the SAME assert,
aborting the WHOLE 300-patient run** — not a per-patient isolated
failure. This is a materially WORSE severity than the direct-
interpreter leg's 2/120: `ehrt.sim-engine.engine/run`'s own per-patient
walk call site
(`components/sim-engine/src/ehrt/sim_engine/engine.clj` ~330-350,
the `sim-trajectory/run-module` call inside the patient-generation
`let`) carries NO `try`/`catch` around it — unlike `census.clj`'s own
`walk-one`, which explicitly catches per-seed throws so "the census
itself NEVER aborts on a module's failure" (that namespace's own
docstring, AR-2). At population scale, ANY one of 300 patients hitting
this branch kills the entire run for every other patient too.

Both legs confirm the prompt's own arithmetic: at a 300-patient
population, the interpreter-layer rate (~1.7%, 2/120) predicts firing
at `1 - (1 - 2/120)^300 ≈ 1 - (0.9833)^300 ≈ 99.35%` — and it fired,
on the very first seed tried.

### The assert site and the interpreter's encounter model

`components/sim-trajectory/src/ehrt/sim_trajectory/gmf_interpreter.clj`:

- **The assert itself** (line 1823-1827, the `:encounter` case of
  `step`'s state-type `case`, landed by the EncounterEnd fix,
  2026-08-08, ADR-0082): `(assert (nil? (open-encounter-index
  (:trajectory ctx))) "...nested :encounter -- this project's GMF
  subset assumes encounters never nest...")` before emitting the new
  `:encounter` event — "the brief's own 'one in-flight encounter'
  invariant made loud" (that line's own comment).
- **`open-encounter-index`** (line 1226-1245, `defn-`): the trajectory
  index of the currently-open `:encounter`, tracked at WALK level (not
  module-scoped, ADR-0082's own retirement of the older module-
  filtered `index-of-last-open-encounter`) — "one in-flight encounter
  is always enough for this project's own GMF subset," and this
  single-slot model is also what `:references` downstream consumption
  relies on (`compile-trajectory`'s own `referenced-event`, a plain
  trajectory position, never re-checked against which encounter opened
  it).
- **`mark-phase`** (line 2078-2109, the Wave H straddle-phase fold,
  ADR-0042/ADR-0086): its own docstring states the same assumption as
  a fold invariant — "`open-phase` clears back to nil once the
  matching `:encounter-end` has consumed it -- encounters never nest
  in this project's own GMF subset, so one in-flight phase is always
  enough."

Both the runtime assert and the compile-time phase fold assume the
SAME single-open-encounter invariant, independently — a change to one
without the other would silently reintroduce a fold defect the
straddle fix (ADR-0086) already closed once.

### The exact module states involved (both failing seeds walked)

A scratch trace (never committed; `with-redefs` intercepting
`open-encounter-index`'s own real implementation to capture the
trajectory-so-far on every call, restoring the original after each
walk) captured both failing seeds' full trajectories up to the crash.
**Both seeds fail at the IDENTICAL state pair**, reached by different
prior injury histories:

- Seed `-576131918266266247` (trajectory length 15 at failure): a
  laceration injury resolves cleanly first, THEN the module's own
  `Wait_For_Injury`/`*_Incidence_Rates` loop draws `Spinal_Injury` —
  `ED_Visit_For_Spinal_Injury` (`Encounter`, `emergency`) opens,
  `No_Spinal_Cord_Damage` (`ConditionOnset`) fires, `Spinal_Injury_
  CarePlan` (`CarePlanStart`) fires — then the SAME walk tries to open
  `Spinal_Injury_Treatment_Encounter` (`Encounter`, `ambulatory`)
  while `ED_Visit_For_Spinal_Injury`'s own encounter is STILL open —
  the assert fires here.
- Seed `-5690589783821964774` (trajectory length 32 at failure): a
  whiplash injury and an SNF submodule call resolve cleanly first,
  THEN the SAME `Spinal_Injury` branch is drawn and fails at the
  IDENTICAL state pair (`ED_Visit_For_Spinal_Injury` still open,
  `Spinal_Injury_Treatment_Encounter` tries to open a second one).

**Root cause, traced directly in `injuries.json`'s own state graph**
(pinned checkout, `src/main/resources/modules/injuries.json`):
`No_Spinal_Cord_Damage` → `Spinal_Injury_CarePlan` →
`Delay_After_Spinal_Surgery` → `Spinal_Injury_Treatment_Encounter` —
NO `EncounterEnd` state anywhere on this path closes
`ED_Visit_For_Spinal_Injury`'s own encounter before the second
`Encounter` state fires. The OTHER 25% branch,
`Spinal_Cord_Damage` → `Neurological_Damage` → `Wheelchair for Spine`
→ `Spinal_Surgery` → `Spinal_Injury_CarePlan`, converges onto the
SAME `Spinal_Injury_CarePlan` → ... → `Spinal_Injury_Treatment_
Encounter` tail — both of `ED_Visit_For_Spinal_Injury`'s own
sub-branches share the identical defect.

**A full-graph sweep, run this session** (a DFS from every one of the
closure's 11 injury-type entry points plus `broken_jaw.json`'s own
call, tracking encounter-open depth, cycle-guarded): the
`Spinal_Injury` branch is the ONLY reachable double-open in the whole
closure. Every OTHER injury type's own linear path (gunshot,
concussion, whiplash, broken bone, burn, laceration, sprain, knee,
shoulder, broken jaw, and both submodules' own internal `Encounter`/
`EncounterEnd` pairs in `broken_jaw.json`/`snf/
skilled_nursing_facility.json`) opens and closes its own encounter
cleanly, one at a time — the sweep only FINDS the spinal-injury hazard
downstream of every other branch because `injuries.json`'s own
`Wait_For_Injury` loop cycles back through the SAME 11-way
incidence-rate draw for as long as the patient survives inside the
horizon, so any walk long enough to draw `Spinal_Injury` at least once
inherits the hazard regardless of which injury type it saw first —
consistent with the measured 2/120 rate (spinal injury is 1 of 11
roughly-equal-weight draws, needing the multi-decade `Wait_For_Injury`
loop to land on it at least once by chance).

### Upstream semantics (source-grounded, not inferred)

Probed directly against the pinned checkout's own Java engine,
`src/main/java/org/mitre/synthea/engine/State.java`, the `Encounter`
state's own `process` method (line 930 onward — this project's GMF
subset does not implement or read this file; it is cited here purely
as evidence of what upstream Synthea itself does with the same
authored module, never as a claim about this project's own runtime).

Upstream tracks ONE current-encounter "lock" per `Person` (not a
stack, not per-module) — `person.hasCurrentEncounter()`. A
non-wellness `Encounter` state's own `process` (line 981-993), when
that lock is already held:

```java
if (person.hasCurrentEncounter()) {
  if (person.getCurrentEncounterModule().equals(module.name)) {
    // This module has the lock, but the previous encounter was not released...
    HealthRecord.Encounter encounter = person.record.currentEncounter(time);
    EncounterType encounterType = EncounterType.fromString(encounter.type);
    person.record.encounterEnd(time, encounterType);
    person.releaseCurrentEncounter(time, module.name);
  } else {
    // Block until the other module finishes their encounter...
    return false;
  }
}
```

Two real upstream branches, both source-confirmed, neither ever
"nests" an encounter the way this project's assert message frames the
hazard: **(a) the SAME module already holds an unreleased encounter**
(exactly `injuries.json`'s own `Spinal_Injury_Treatment_Encounter`
case — the SAME root module reopening while its own earlier encounter
never got an explicit `EncounterEnd`) — upstream AUTO-CLOSES the
stale encounter (`person.record.encounterEnd`), releases the lock,
THEN proceeds to open the new one, silently and without complaint.
**(b) a DIFFERENT module holds the lock** — upstream BLOCKS (`process`
returns `false`, the state does not advance) until the other module's
own encounter ends, then retries later. Upstream's own real behavior
for `injuries.json`'s exact authored pattern is case (a): a quiet
auto-close, never a thrown error and never a genuinely concurrent
open encounter either.

### Design options for a future fix (evidence only, no recommendation)

- **(i) Auto-close on reopen, matching upstream exactly.** The
  `:encounter` case's own assert becomes a conditional auto-close:
  when `open-encounter-index` is non-nil, synthesize an implicit
  `:encounter-end` for the stale one (referencing it, per this
  project's own citation law) before emitting the new `:encounter`.
  Blast radius: touches `step`'s `:encounter` case only; `mark-phase`'s
  own "one in-flight phase" fold invariant stays TRUE under this
  option (there is still never more than one encounter open at once,
  the auto-close guarantees it) — the narrowest option, and the ONLY
  one of the four that reproduces upstream's own real, source-
  confirmed behavior rather than approximating or diverging from it.
  Open design question this option does not answer on its own:
  upstream's own case (b), a DIFFERENT module holding the lock, has no
  analogue here under one-module-per-patient (Wave G's own standing
  scope) — irrelevant until multi-module assignment lands, disclosed
  not resolved.
- **(ii) An encounter stack, widening the subset.** Generalize
  `open-encounter-index` to a stack of open indices; the assert
  retires; `:encounter-end` pops the most recent. Blast radius:
  WIDER than (i) — `mark-phase`'s own fold (compile-time straddle-
  phase inheritance, ADR-0042/ADR-0086) assumes exactly one open
  phase and would need its own generalization to a stack too, or the
  two mechanisms drift out of sync (the same class of latent-defect
  risk this ADR's own "assert site" section names above); downstream
  `:references` resolution (`compile-trajectory`'s own
  `referenced-event`) would need to decide which of several
  concurrently-open encounters a given event belongs to, a genuinely
  new design question this project's `:references` shape does not
  answer today. Most upstream-general (matches Synthea's own
  multi-encounter reality for the cross-module case (b) too), also
  the most invasive.
- **(iii) Suppress the nested open, disclosed event loss.** Mirror the
  `:encounter-end`-with-nothing-open precedent already landed
  (`:suppressed-encounter-ends`, ADR-0082 R2): a second `:encounter`
  open while one is already open becomes a documented no-op, counted
  via a NEW `:suppressed-nested-encounters` counter, its own clinical
  content (the events between the suppressed open and its own
  eventual close) either dropped or re-attributed to the FIRST
  encounter. Blast radius: narrow at the assert site, but drops real
  authored content (`Spinal_Injury_Treatment_Encounter`'s own
  `Prescribe_Opioid` submodule call and its `EncounterEnd`) — the
  named "disclosed event loss" this option's own name states; not
  upstream-faithful (upstream never drops the treatment encounter's
  own content, it reassigns it to a fresh encounter after the
  auto-close).
- **(iv) Module-level exclusion.** Leave the assert as the standing
  backstop; exclude `injuries.json`'s own `Spinal_Injury` branch (or
  the whole module) from ever landing. Blast radius: zero interpreter/
  engine change, but forecloses vendoring THIS closure's real,
  otherwise-clean content (10 of 11 injury types walk clean, per this
  session's own full-graph sweep) over one branch's own authored gap —
  the AR-VB1-6/AR-VB2-5/AR-VB3-4 bail-out precedent's own default,
  restated as an explicit option rather than merely happening by
  default.

No recommendation is made here per the driving prompt's own explicit
instruction — the ruling among these four belongs to the author, in a
future session.

### [C] Dated finding, no action: the zero-advance budget's own residual boundary

Verified this session, design channel finding, recorded per the
driving prompt's own [C] instruction. ADR-0105's own `consume-step-
budget` counts only zero-advance steps and NEVER RESETS that count —
the "does not consume" semantic, chosen over "resets to 0 on any
advance" (both licensed by ADR-0105's own driving prompt). This means
the landed fix closes exactly the class it targeted (a loop whose
PER-CYCLE zero-advance step count, times the number of cycles needed
to cross the horizon, stays under 10000) but leaves a residual: a
LEGAL, always-advancing loop whose own zero-advance-steps-per-cycle
accumulate past 10000 across enough cycles WITHIN the horizon still
throws `max-steps`, exactly the same backstop, on a walk that never
had a bug. `broken_jaw.json`'s own 1-7 day `Wait for Dental Visit`
loop is safely under this new boundary (mean ~4562 cycles to cross a
50-year horizon at 1 zero-advance step/cycle, per ADR-0105's own
arithmetic, well under 10000) — unreachable for THIS closure by
concentration, not by design. A different, hypothetical module
authoring a FIXED 1-day wait cycle over a 50-year horizon (18250
cycles, each contributing exactly 1 to the never-resetting zero-
advance count) WOULD cross 10000 and throw, despite being a fully
legal, always-advancing loop with no authoring bug. No module in this
project's own vendored or candidate catalog is known to hit this
boundary today (confirmed by the same reasoning ADR-0105's own
pre-analysis used: no vendored or candidate closure's own Delay states
carry a bound anywhere near 1 day repeated for decades) — a dated
observation, not a fix, and no action taken this session.

### NOTICE update

`components/sim/resources/sim/modules/NOTICE`'s own injuries dated
section (inside the vendoring-batch-1 entry) gains a dated 2026-08-11
amendment: the max-steps leg is CLOSED (cites ADR-0105); the
nested-`:encounter` leg is now the SOLE named blocker (cites this ADR,
the exact state pair, the measured rates at both probe layers, the
`dme/wheelchair.json`-already-vendored correction); revisit trigger
restated as "a future session ruling on and implementing one of
ADR-0106's own four named design options."

### Oracle bracket

Branch C: pure identity, trivially — no `src/`, `resources/`, or
`test/` change lands this session (scratch fetches only, cleaned
before commit). Still run per the driving prompt's own instruction:
`bin/regression-oracle 1abee30 <this-record's-own-commit>` —
IDENTICAL, all 34 roots, matching the trivial-identity expectation
exactly.

### Full gate

`clojure -M:poly check`: OK. Full local suite (`clojure -M:poly test
:all`, unredirected capture, 7m35s): 822 occurrences of "0 failures, 0
errors" across the entire log, ZERO `FAIL`/`ERROR` report lines
anywhere — unchanged from baseline (no `src`/`test` file this session
touches any brick). `ehrt.cli.cli-parse-guard-lint-test` confirmed
green within that same run: 4 tests, 22 assertions, 0 failures/errors.
`bin/verify-nist-lock`: OK, 6 hit-nexus-sourced coordinates matched
(`nist-hl7-v2-parser`, `nist-hl7-v2-profile`, `nist-hl7-v2-validation`,
`nist-xml-util`, `nist-hl7-v2-schemas`, `nist-validation-report`).
`gitleaks detect --source . --no-git -v`: no leaks found.

### Fences

Touched exactly: `NOTICE`'s own dated section, `notes/adr/0106-*.md`
(this file), `notes/ADRs.md`, `notes/adr/README.md`, `.agents/
plans/roadmap.md`, `.agents/rulings.md`, `.agents/prompts/`,
`.agents/session-records/`. No `resources/sim/modules/` content
change (nothing vendored). No interpreter/engine/emitter change (the
nested-encounter fix is a FUTURE session's own ruling, per the
driving prompt's own explicit fence). No default-config change.
Scratch fetches (`injuries.json`, `injuries/broken_jaw.json`,
`snf/skilled_nursing_facility.json`, plus the two probe/trace scripts)
never committed, cleaned before this record's own commit.

### Deviations, disclosed

- **`.agents/plans/roadmap.md`'s own Done section was missing ADR-0105's
  own pointer** — ADR-0105's own execution record claims "appended to
  `.agents/plans/roadmap.md`'s own Done section," but the live file
  carried no such line (confirmed by direct grep before this session
  relied on it). A one-line, disclosed fix lands in this session's own
  close-phase commit alongside this ADR's own Done pointer — the same
  "transcript-witnessed is not repo-recorded" discipline this repo's
  own rulings register already names (`.agents/rulings.md`, "From
  ADR-0048"), applied here to a prior session's own written claim
  rather than a chat transcript.
- **The closure-membership count** (5 already vendored / 3 new, not
  the driving prompt's own inherited 4/4) — see the dedicated section
  above; `dme/wheelchair.json` joined the vendored tree via a SIBLING
  batch (ADR-0071, same day as ADR-0070) after ADR-0070's own count
  was written, and nothing since corrected it forward. Caught by this
  session's own fresh-gate-at-the-pin discipline (AR-VB4-1), not
  inherited uncritically.
- **The fork resolved to Branch C, as the driving prompt's own
  arithmetic predicted (~99.4% likely at 300 patients)** — disclosed
  as confirmation, not surprise: both probe legs (120-walk direct
  interpreter, 300-patient engine run) fired the assert, the engine
  leg on the very first seed tried.
- **The engine-layer severity finding** (a single patient's failure
  aborts the WHOLE 300-patient `engine/run`, no per-patient isolation
  the way `census.clj`'s own `walk-one` provides) — a genuine,
  disclosed finding this session's own probe surfaced, not previously
  named in ADR-0070 or ADR-0105 (both of which used the census's own
  per-seed-isolated method, or a horizon sweep, never the raw
  `engine/run` call this session made directly).

### Index line

```
- 2026-08-11 — injuries-b2-assessment — ADR-0106
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

`notes/adr/README.md`'s own file count corrects 103→104, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Injuries B2 assessment: the nested-encounter gap characterized, vendoring still deferred — B2's own widened, assessment-first charter fires as predicted (~99.4% likely at 300 patients): a fresh gate finds ADR-0105's own max-steps fix complete (0/120) but a SEPARATE, pre-existing gap still trips — `injuries.json`'s own `Spinal_Injury` branch opens `ED_Visit_For_Spinal_Injury` and never closes it before opening a second `Encounter` on the same walk, tripping `step`'s own nested-`:encounter` assert at 2/120 well-mixed seeds AND aborting a whole 300-patient `engine/run` uncaught (no per-patient isolation at the engine layer, a new severity finding); both failing seeds' trajectories walked to the identical state pair, upstream Synthea's own real Java semantics for the same authored pattern probed and cited (a quiet same-module auto-close, never a nest), four design options recorded with blast radius and no recommendation; a closure-membership correction (`dme/wheelchair.json` already vendored via a sibling batch, true count 5 already-vendored/3-new not 4/4) caught by fresh-gate discipline; nothing vendored, the oracle holds pure identity across all 34 roots

### Rulings-register history (moved verbatim from `.agents/rulings.md` by ADR-0145, 2026-08-17)

## From ADR-0106 (injuries B2 assessment; ruled 2026-08-11)

- **The widened, assessment-first charter** [A, ruled 2026-08-11,
  author verbatim "b"]: B2 (the injuries vendoring batch itself)
  ATTEMPTS the batch under the standing vendoring ceremony, but if the
  known pre-existing `nested :encounter` gap (ADR-0105's own finding,
  2/120 well-mixed seeds, unaffected by that session's own fix) fires
  at the round-trip gate, the session's own deliverable BECOMES the
  full characterization of that gap (root cause, upstream semantics,
  measured rate, design options with blast radius, no recommendation
  required) under the ADR-0070 bail-out precedent, and NOTHING is
  vendored. Either outcome — a landed batch or a full characterization
  — is a successful session; this ruling licenses both branches in
  advance, not only the one that actually fired. It fired: the fresh
  gate found the assert tripping at both probe layers (2/120 direct
  interpreter, and a full 300-patient `engine/run` throwing uncaught),
  matching this session's own pre-stated ~99.4%-likely arithmetic; the
  characterization landed in `notes/adr/0106-injuries-b2-assessment.
  md`, nothing vendored, `injuries.json` remains deferred, re-anchored
  on the nested-encounter blocker with a new revisit trigger (a future
  session ruling on one of the four named design options).
