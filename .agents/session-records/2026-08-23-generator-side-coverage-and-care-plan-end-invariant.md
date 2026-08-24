# 2026-08-23 -- generator-side event-type coverage gate plus the care-plan-end referential invariant

Autonomous build session (R30), penny / WSL / JDK 21. HEAD at start
`68af03b`, tree clean. Two commits as instructed, plus a third for the
close (disclosed below). Local commits only -- no push, no tag.

- `cb93bb3` -- `test(sim)`: the generator-side coverage gate (ADR-0165)
- `828a744` -- `feat(sim-check)`: the `:care-plan-end` invariant (ADR-0166)
- close commit -- this record, the archived prompt, the regenerated
  indexes and the roadmap row

## Preflight

`bin/preflight`, exit 0, no findings. One disclosure printed and carried
here: **HEAD is not currently tagged `stable-*`** (last tag
`stable-20260821-patient-simulator-charter` at `6ce2160`). Last five CI
runs on main all green; edit root not under `/mnt/`; tree clean
including untracked; local HEAD matched `origin/main`.

## Suite figures

| run | namespaces | tests | assertions | result | wall |
| --- | --- | --- | --- | --- | --- |
| baseline, `68af03b` | 368 | 4,116 | 18,414 | `MAKE_EXIT=0` | **1,702s (28m22s)**; poly execution 27m09s |
| final, completed tree | 370 | 4,142 | 18,450 | `MAKE_EXIT=0` | **1,662s (27m42s)**; poly execution 26m41s |

The namespace column counts namespace RUNS, this file's own inherited
convention -- every namespace runs in two projects, so 370 runs are 195
DISTINCT namespaces (194 at baseline).

Delta: **+2 runs / +1 distinct namespace, +26 tests, +36 assertions.**
Reconciled exactly, per namespace, rather than attributed by
subtraction:

| namespace | baseline | final | x runs | delta |
| --- | --- | --- | --- | --- |
| `patient-simulator.emittable-events-test` | absent | 3 / 5 | 2 | +6 tests, +10 assertions |
| `sim.run-test` | 26 / 87 | 27 / 89 | 2 | +2 tests, +4 assertions |
| `sim-check.check-test` | 65 / 70 | 74 / 79 | 2 | +18 tests, +18 assertions |
| `docs-tooling.io-vocabulary-lint-test` | 3 / 111 | 3 / 112 | 2 | +0 tests, +2 assertions |
| `docs-tooling.test-source-live-path-lint-test` | 5 / 138 | 5 / 139 | 2 | +0 tests, +2 assertions |

The last two rows are worth naming: they are repo-wide lint gates that
assert once per scanned file, so the new test namespace joined their
population automatically and passed both. Nothing else in the tree
moved by a single assertion.

The final run is FASTER than the baseline (27m42s vs 28m22s) despite
doing more, so the four gated corpora cost nothing measurable at suite
scale -- the run the hunt added is ~19ms.

**Q2 evidence (the open "make test doubled" question).** The baseline
sits at 28m22s, matching the post-2026-08-21 figure rather than the
earlier one, so the slowdown persists and is not this session's.

One locating observation this session can add, directly measured rather
than inferred: during the baseline run the log file went **unmodified
from 19:48 to 19:59-04:00 -- over eleven minutes -- with
`Testing ehrt.sim-emit-hl7.vendored-veteran-ptsd-test` as its last
line**, then moved through the remaining ~230 namespaces briskly. The
independent 32-namespace re-judgement run below was observed sitting on
that same namespace at the end of its own run, consistent with the same
stall, though it was not separately timed. That is a specific place to
look, not a diagnosis, and it does not by itself explain a doubling.

## Commit 1 -- ADR-0165, the coverage gate

### The measurement of record (step 3)

Cell = cited (compiled-content) events of that type in that run; `-` =
that run's own closure cannot emit it; `0` = emittable there, produced
zero times. Fourth column is the run the hunt added.

| event type | seed-202 ed-tuesday | seed-424242 clinic-decade | seed-5 clinic-decade | adhd seed-2 | covered |
| --- | --- | --- | --- | --- | --- |
| `:admission` | 0 | 0 | **1** | 0 | YES |
| `:care-plan-end` | 0 | 0 | 0 | **1** | YES |
| `:care-plan-start` | 0 | **9** | **5** | 0 | YES |
| `:diagnostic-report` | - | 0 | **1** | - | YES |
| `:discharge` | 0 | 0 | **1** | 0 | YES |
| `:medication-end` | 0 | 0 | 0 | **1** | YES |
| `:medication-order` | 0 | **20** | **21** | 0 | YES |
| `:observation` | 0 | **10** | **25** | - | YES |
| `:outpatient-visit` | 0 | **38** | **46** | 0 | YES |
| `:outpatient-visit-end` | 0 | **38** | **46** | 0 | YES |
| `:procedure` | 0 | **28** | **17** | 0 | YES |

| run | wall | ok | total events | cited events |
| --- | --- | --- | --- | --- |
| seed-202 ed-tuesday | 1,040ms | true | 407 | **0** |
| seed-424242 clinic-decade | 2,025ms | true | 343 | 143 |
| seed-5 clinic-decade | 1,352ms | true | 363 | 163 |
| adhd seed-2 | 19ms | true | 12 | 2 |

### RED, before any hunt

```
emittable by the gated scenarios' own modules but produced by NO gated corpus:
[:care-plan-end :medication-end]
Produced: [:admission :care-plan-start :diagnostic-report :discharge
           :medication-order :observation :outpatient-visit
           :outpatient-visit-end :procedure]
```

The two missing types are exactly the two ADR-0163's drop rule removes.

### The INVARIANT demonstration (P1)

`:temporary-mutation-for-adr-0165` added to `step`'s `case`, gate run,
reverted:

```
FAIL in (table-covers-every-state-type-the-interpreter-dispatches-on)
state types in `step` but not in the table: (:temporary-mutation-for-adr-0165);
in the table but not in `step`: ()
```

`git status` for `gmf_interpreter.clj` verified clean after revert
before any further work.

### The P2 relocation, proven verbatim

The ENTIRE deletion side of `run_test.clj`'s diff:

```
-  (:require [clojure.string :as str]
-            [clojure.test :refer [deftest is testing]]
-    (let [r (run/run-command {:seed 202 :patients 100 :churn true
-                              :config "demos/scenarios/ed-tuesday/config.edn"})]
-    (let [r (run/run-command {:seed 424242 ...})]
-    (let [r (run/run-command {:seed 5 ...})]
```

The `ns` require line and three `let` bindings. No assertion and no
`testing` docstring changed.

### Hunt log (step 5)

| # | population | runs | wall | outcome |
| --- | --- | --- | --- | --- |
| 1 | `clinic-decade`, seeds 1-60 x {25,50,100} patients | 180 | 84s | `:medication-end` 9 hits (smallest seed 35 @ 25); `:care-plan-end` **zero** |
| 2 | single-module, 7 CarePlanEnd-bearing modules x seeds 1-40 @ 100 | 280 | 69s | `:care-plan-end` in `dermatitis`, `rheumatoid_arthritis`, `attention_deficit_disorder` |
| 3 | `attention_deficit_disorder`, seeds 1-60 x 7 patient counts | 420 | 17s | minimum found: **seed 2 @ 10 patients**, both types, ~19ms |

Total hunt wall ~170s, well inside the ~30min/type budget.

Why hunt 1 found nothing, read off the modules rather than left as bad
luck: of `clinic-decade`'s twelve, only `asthma`, `bronchitis` and
`total_joint_replacement` carry a CarePlanEnd at all, and
asthma/bronchitis reach theirs ONLY via `referenced_by_attribute`
(`asthma_careplan`, `bronchitis_careplan`) -- the never-written-attribute
route ADR-0163 now drops. Seed 5's two historical `:care-plan-end`
events were exactly that kind.

**Waivers: NONE.** All 11 emittable types covered; `coverage-waivers`
lands empty with its contract documented.

### Two holes the green gate sits over

Rowed, not hidden, because a green gate is where a weakness hides:
`roadmap.md#ed-tuesday-module-tail-inert` (that scenario declares 10
emittable types and covers zero -- 407 events, none cited) and
`roadmap.md#generator-coverage-depth` (`:admission`, `:discharge`,
`:diagnostic-report` are each one cited event deep, in one run).

## Commit 2 -- ADR-0166, the `:care-plan-end` invariant

### Step 7's probe, file:line

| read | says |
| --- | --- |
| `compile_trajectory.clj:367-379` | `pre-horizon-fact-types` carries `:care-plan-start`/`:care-plan-end` in the same class as the medication pair (ADR-0029 D2) |
| `compile_trajectory.clj:531-534` | ONE shared clause promotes any member to a registration fact -- no care-plan-specific branch |
| `event_schema.clj:186-191` | `PreHorizonFact`'s `:event` enum declares all six; names the nested-`:event` hazard |
| `engine.clj:887-907` | `decide :care-plan-end` emits `:start-event-id` + `:care-plan-citation`, the exact twin |

The twin's suggestion turned out correct -- but it was probed, not
assumed, which is what the step asked for.

### Red, in both directions

- Red 1 (trivial, recorded as non-evidence): `No such var:
  check/care-plan-end-references-existing-start-and-follows-it-in-time`.
- Red 2A -- reporting guard neutered: **all 7 rejection tests fail,
  both acceptance tests pass**, `7 failures, 0 errors`.
- Red 2B -- pre-horizon escape neutered: the scripted straddle test
  fails AND the real gated corpus fails,
  `{:invariant :care-plan-end-references-existing-start-and-follows-it-in-time,
  :patient-id "PID-000005-939736f8", :at 19016340}`.

Both mutations reverted; restoration verified green before staging.

### Declared oracle change and its outcome

Population enumerated by grep (44 referencing files), then the 32 test
namespaces that build and judge a corpus re-run as one batch: all 28
`sim-emit-hl7` `vendored_*_test` namespaces plus `engine_test`,
`corpus.sim-adapter-test`, `event-conformance-test`,
`sim.interface-surface-test`.

```
Ran 138 tests containing 591 assertions.
0 failures, 0 errors.   RECHECK_EXIT=0
```

**No gated corpus newly fails**, so there is no step-9 FINDING to
report. `check_test` (74/79, including the 300-run defspec) and
`run_test` (27/89) clean separately.

### Fences

`check.clj`'s staged diff has **zero deleted lines** -- purely additive:
one invariant, one helper, one catalog entry. No vendored module
touched, in either commit.

## Oracle sweeps

FIVE `make docsgen` runs, all exit 0. Every artifact that moved was a
generated COUNT, and nothing else moved at any point:

| after | artifact | change |
| --- | --- | --- |
| commit 1 content (2 runs) | `.agents/state-derived.md` | test namespaces 198 -> 199; ADR files 162 -> 163; roadmap rows 53 -> 55; Next 21 -> 23; `:onboarding` 1,406 -> 1,418 tokens |
| commit 2 content | `.agents/state-derived.md` | ADR files 163 -> 164 |
| close scaffold, then the `## Done` rotation (2 runs) | `.agents/state-derived.md`, both record INDEXes | session records 167 -> 168; archived prompts 160 -> 161; roadmap row count NET UNCHANGED -- one CLOSED row in, ADR-0160's rotated out |

No `demos/traces/**` ground truth, no
`components/sim-engine/resources/sim-engine/event-examples.edn`, no
event-schema export moved at any point. Predicted both times: commit 1
adds a test, a data table and a fixture relocation; commit 2 adds an
invariant, and an invariant judges a log rather than producing one.

## Reading sets, re-measured at close

Read off the regenerated `.agents/state-derived.md` at the close -- all
five sets, which is the whole population (there is no `:cli` set).

| set | paths | actual | budget | baseline | headroom |
| --- | --- | --- | --- | --- | --- |
| `:corpus` | 7 | 1,836 | 2,045 | 2,045 | 209 |
| `:docs` | 5 | 739 | 785 | 785 | 46 |
| `:judge` | 8 | 926 | 1,000 | 1,000 | 74 |
| `:onboarding` | 10 | 1,418 | 1,530 | 1,530 | 112 |
| `:sim` | 6 | 1,278 | 1,405 | 1,405 | 127 |

All under budget. `:onboarding` moved 1,406 -> 1,418 (this session's two
OPEN rows and one CLOSED row, less the rotated one); no compaction was
needed and no bump was considered (`rulings.md#R-budget-stop`).

## Register hygiene at close

`## Done` reached 31 lines against its 30 cap once this session's CLOSED
row was added, so the ceremony's rotation was owed and paid: ADR-0160's
row -- the oldest whole row -- moved VERBATIM into
`.agents/plans/roadmap-done-2026-08.md` (append-only), leaving `## Done`
at 25 lines. `ehrt.docs-tooling.attic-rotation-test` was red on the cap
before the rotation and green after, which is how the overflow was
found rather than assumed.

## Deviations, disclosed

1. **The P1 table carries TWO columns, not one.** P1 named a
   state-type→event-type table gated against the interpreter's dispatch.
   The interpreter's dispatch produces TRAJECTORY event types, but the
   trajectory is discarded at `engine.clj:346` and only ground-truth
   events are observable from a gated run -- six trajectory types can
   never reach a log by construction, so gating the raw vocabulary would
   need six STRUCTURAL waivers, which is not what P3(a) describes. Each
   row therefore carries both the trajectory event and the ground-truth
   types it reaches. Full reasoning in ADR-0165.
2. **The gate counts only citation-bearing events.** Not named in the
   prompt; added because measurement showed it load-bearing -- seed-202
   emits 92 `:admission` and 90 `:discharge` events of which ZERO are
   cited, so without the filter the gate would have measured the
   scenario author instead of the generator.
3. **The `:ground-truth` column is declared-and-cited, not
   machine-derived.** Four state types delegate to helpers outside
   `step`'s own case (`death-step`, `wellness-wait-step`, `guard-step`,
   `call-submodule-step`), so a syntactic per-branch walk would
   under-report `:death` and `:wellness-wait`. Its check is empirical
   (the gate reads real corpora) rather than syntactic.
4. **The covering run comes from a module the gated scenarios do not
   name.** `attention_deficit_disorder` is not in `clinic-decade` or
   `ed-tuesday`. Forced: `:care-plan-end` is unreachable-when-paired in
   both, for the module reason recorded above.
5. **Step 11's "close this row" had no row to close.** No OPEN roadmap
   row existed for this session's work. Fixed forward: a CLOSED row was
   added at close, the shape ADR-0163's row takes, rather than
   inventing an OPEN row to close.
6. **A third commit the prompt did not name.** The prompt specified two
   commits, but `bin/close-scaffold`'s record, the archived prompt, the
   regenerated indexes and the roadmap row cannot ride either of them
   (R-A pairs the record with the close, and both index gates need the
   regeneration). Same disposition the ADR-0163 session disclosed for
   the same reason.
7. **The record's own figures were written after the green run.** The
   final `make test` ran over the completed tree with this record's
   suite row held as fill-in markers, because a record cannot carry a
   run's result before the run exists. The only edits after the green
   run are this record's figures, this line, and the close commit that
   carries them -- no source, test, register or generated artifact
   changed after the gate. Stated rather than left for a reader to
   infer from timestamps.
8. **A stale memory corrected.** A carried note said the ADR-0163/0164
   arc was "closed but NEVER PUSHED". `bin/preflight` showed
   `origin/main` at `68af03b` with CI green, so it had since been
   pushed. Recorded here because the note was load-bearing for this
   session's starting premise.
