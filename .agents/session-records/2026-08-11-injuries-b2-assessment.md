# 2026-08-11 — Injuries B2 assessment: the nested-encounter gap characterized, vendoring still deferred (ADR-0106)

## Scope

B2 of the author-ruled injuries arc (2026-08-11, author verbatim "b"):
a WIDENED, assessment-first charter — attempt the injuries vendoring
batch under the standing vendoring ceremony, but if the nested-
encounter gap ADR-0105 found (pre-existing, unaffected by that
session's own fix) fires at the round-trip gate, the deliverable
BECOMES the full characterization of that gap, and nothing is
vendored. One commit: this close-phase record/ADR/prompt-archive
commit (no `src`/`resources`/`test` change — the fork resolved to
Branch C). This is ADR-0106.

## Red→green evidence highlights

Not a red→green session in the code sense — Branch C fired, so no
source change lands. The gate itself is the evidence. Fresh closure
re-enumeration at the pin found the SAME 8-file closure ADR-0070
named, zero lookup tables, but corrected the already-vendored count:
`dme/wheelchair.json` (named a "new" member by the driving prompt's
own inherited ADR-0070 count) is actually already vendored via a
SIBLING batch (ADR-0071) that landed after ADR-0070 was written — true
disposition 5 already-vendored/3-new, not 4/4.

**The round-trip probe, both legs run:**

- **Direct interpreter** (ADR-0070's own method: registration age 30,
  50-year horizon, mixer-seed 20260803, 120 well-mixed seeds): 2/120
  fail on the nested-`:encounter` assert, seeds
  `-576131918266266247`/`-5690589783821964774` — independently
  reproducing ADR-0105's own finding fresh, not merely citing it
  forward.
- **Engine-level population run** (the round-trip test's own standard
  parameters: seed 20260802, 300 patients, 36500-day horizon):
  `engine/run` THROWS THE SAME ASSERT, UNCAUGHT — a new severity
  finding, since `ehrt.sim-engine.engine/run`'s own per-patient walk
  call site carries no try/catch (unlike `census.clj`'s own isolated
  `walk-one`), so one unlucky patient among 300 kills the whole run.

Both legs confirm the driving prompt's own arithmetic
(`1-(1-2/120)^300 ≈ 99.35%`) — it fired on the first seed tried.

**Root cause, traced by name.** A scratch trace (`with-redefs`
intercepting `open-encounter-index`, captured trajectory-so-far,
restored after) walked both failing seeds to the IDENTICAL crash
point: `injuries.json`'s own `Spinal_Injury` branch opens
`ED_Visit_For_Spinal_Injury` (`Encounter`, emergency) and never closes
it with an `EncounterEnd` before later opening `Spinal_Injury_
Treatment_Encounter` (`Encounter`, ambulatory) on the SAME walk. A
full-graph DFS sweep (this session's own, all 11 injury-type entry
points plus both submodules) confirmed this is the ONLY reachable
double-open in the whole closure — every other injury type opens and
closes its own encounter cleanly; the hazard is reachable from every
entry point only because `Wait_For_Injury`'s own decades-long loop
eventually draws `Spinal_Injury` for any long-enough walk.

**Upstream semantics, source-grounded.** The pinned checkout's own
`State.java` (`Encounter.process`, line 930) shows upstream Synthea
tracks ONE current-encounter lock per Person: the SAME module
re-opening while its own earlier encounter is unreleased AUTO-CLOSES
it silently, then proceeds — exactly `injuries.json`'s own authored
pattern, and exactly Design Option (i) below. A DIFFERENT module
re-opening BLOCKS instead. Upstream never throws on this pattern and
never nests either.

**Four design options recorded, no recommendation** (per the driving
prompt's own explicit instruction): (i) auto-close on reopen
(upstream-faithful, narrowest blast radius); (ii) an encounter stack
(widest, requires generalizing `mark-phase`'s own single-open-phase
fold too); (iii) suppress-with-disclosed-event-loss (mirrors the
existing `:suppressed-encounter-ends` precedent, drops real content);
(iv) module-level exclusion (zero interpreter change, forecloses 10 of
11 otherwise-clean injury types).

## Judgment calls and their ratification status

- **The fork resolved to Branch C, as predicted.** Not a judgment
  call — the probe was run honestly per the driving prompt's own
  instruction, and it fired at both layers. No improvisation.
- **A missing ADR-0105 Done pointer, found and fixed.** `.agents/
  plans/roadmap.md`'s own Done section lacked the pointer ADR-0105's
  own execution record claims it added. One-line, disclosed fix,
  landed alongside this session's own new pointer — not itself
  licensed by the driving prompt, but directly adjacent, safe, and
  within the touched file's own fence.

## Findings and HEAD landed

**One real, pre-existing defect fully characterized, not fixed** (per
the driving prompt's own explicit fence: the fix is a FUTURE session's
own ruling): the nested-`:encounter` assert, root-caused to
`injuries.json`'s own `Spinal_Injury` branch, upstream semantics cited
from source, four options recorded with blast radius.

**A new severity finding**: `engine/run`'s own lack of per-patient
isolation, unlike the census tool's own `walk-one`.

**A closure-membership correction**: `dme/wheelchair.json` already
vendored via a sibling batch, true disposition 5/3 not 4/4.

**Oracle bracket held pure identity** — no `src`/`resources` change
this session touches at all.

**Tag paid forward:** `stable-20260811-interpreter-horizon-budget`
tagged at `1abee30` (Step 1, this session — the design channel's own
verified ADR-0105 landing, tag law case (i)), peeled ref verified
exact match, remote unmoved.

**Roadmap corrected:** the B2 row moves from "OPEN, now unblocked" to
"ASSESSED, RE-ANCHORED on a nested-encounter blocker," with a new
revisit trigger (a future session ruling on one of ADR-0106's own four
design options).

**HEAD landed:** the close-phase commit (this record's own commit),
pushed.
