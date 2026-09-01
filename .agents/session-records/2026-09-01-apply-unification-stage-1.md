# 2026-09-01 — application-path unification: census and stage 1

Prompt archived at `.agents/prompts/2026-09-01-apply-unification-stage-1.md`.
Serves `roadmap.md#engine-namespace-extraction-and-apply-unification` (P5),
now solely this arc. Ceremony mode: R30 standing default (commit and push at
checkpoints, unattended) — the prompt states no prepare-only mode.

Commits, on `main`, parent `3e65ff9`:

| # | sha | what |
|---|---|---|
| C1 | `434e939` | docs: the apply-unification census — matrix, cones, choke point |
| C2 | `7ab7cdb` | refactor: one apply choke point, three projected call sites — output-identical |
| C3 | (this record) | docs: stage-1 records — the P5 row's stage-2 pair checklist |

## 0. Preflight

Both clone roots resolved at session start. The ext4 clone of record,
`~/src/ehr-testing-tools`, was at `3e65ff9`, the sha the prompt names; every
edit this session made resolved under it. The `/mnt/c` clone
(`C:\Users\prags\Documents\ehr-testing-tools`) was at `537f954` — far behind,
which is EXPECTED and correct since `bin/sync-mnt-c` was deleted at `e7646b5`
and the mirror retired. Nothing was written there.

## 1. Step 1 — the census

`.agents/plans/apply-unification-census.md`, 397 lines, re-derived at
`3e65ff9`. Its gate — every matrix cell cites `file:line` at this sha — is
met by construction: the 17 PRESENT cells cite the expression, and each of
the 22 OMITTED cells cites the site's own span, which is the evidence of
absence.

**The census's own findings, in the order they cost something.**

* **THIRTEEN concerns, not ten.** The prompt expected ~10 and section 4b
  lists 10; the matrix carries 13 because 4b folded the two decorations into
  one row and, being site-1-centric, never named the two concerns site 2 has
  and site 1 does not (`:patient-bootstrap`, `:replay-entries`).

* **Four of the thirteen are not per-event folds.** The census names each
  concern's GRAIN — decoration, per-event, per-batch, parameter — because
  generalizing a per-batch concern as per-event is how an output-identical
  claim gets lost, and one of them (`:state-history`) would have done exactly
  that. See correction C2 below.

* **22 omitted pairs, 3 predicted OUTPUT-MOVING and 19 INERT.** All three
  OUTPUT-MOVING pairs are decorations, which is section 4c's "replay cannot
  do them" restated as something stage 2 can check. The mechanism is named
  rather than asserted: `evolve` READS `:encounter-id` off the event
  (`evolve.clj:210`, `:220`, `:338`, `:343`, `:412-415`, `:423-427`,
  `:431-434`, `:459-463`) and folds it into conditions, observations,
  medication orders and care plans, so a re-stamp moves `:before`/`:after`
  too, not only `:event`.

## 2. The census's six corrections to section 4

**None disagrees with section 4c's divergence in KIND**, so the fence that
would have stopped this session before implementing did not fire. `replay`
still does strictly fewer things than site 1, and the six it does not do are
the six named.

* **C2 — `:state-history` appends the POST-BATCH state.** Section 4b says
  "post-event". `run.clj:1387-1392` reads `(get-in world' [:patients
  patient-id])` where `world'` is the accumulator after the WHOLE batch, so a
  batch of two events touching one patient appends that patient's FINAL state
  twice. This is the single correction an output-identical refactor turns on:
  the natural unification folds history per event, and that would have moved
  bytes. It stayed a post-pass.

* **C3 — `check.clj:527`'s comment says the opposite of what 4c says it
  says.** 4c: "the bed index `ehrt.sim-check.check` would want is unavailable
  on the replay path (`check.clj:527`'s own comment says as much)". The
  comment (`check.clj:522-528`) says the fold there is DELIBERATELY not
  `update-beds`, because "calling the engine's own index-builder here would
  prove only that the engine agrees with itself, which is the vacuous-gate
  shape this repository has already been bitten by twice". **Checked at the
  census's own sha rather than assumed** — `git show 517a96d:.../check.clj`
  carries the identical text. The P5 row repeated the mis-reading in its own
  closing sentence; this session's docs commit removes it. Consequence: the
  2 × `:bed-index` pair has no consumer waiting for it, and the arc must not
  be sold on delivering `check` a bed index.

* **C1** — the divergence is six wide at 4b's grain and ten at the matrix's.
* **C4** — two SUBJECT notions coexist and section 4 names neither. `subject`
  (`:reinstate-index`, `:registration-index`) is the FIRST participant's
  `:patient-id`, nil for a `:bed-status-change`; `subject-id` (a replay
  entry) is the first participant that HAS one. They diverge on any event
  whose first participant is not a patient, and a unification that collapsed
  them would be output-identical on today's corpora only by accident.
* **C5** — site 3 has no projection of its own to state: its stack is site
  2's by construction, so stage 1 declares it as an alias.
* **C6** — `check.clj` holds FOURTEEN `replay` call forms, not fifteen; the
  fifteenth occurrence is inside `fold-records`' docstring at `check.clj:395`.
  Fourteen is exactly `roadmap.md#performance-residual-sites`' own count, so
  4c's sentence reconciling a gap is reconciling one the docstring created.

## 3. Step 1(iv) — the choke point's home, CONFIRMED at one cost

`fold.clj` was the design channel's expectation and it is right, but the
derivation the channel did not have is the require graph. Sites 2 and 3 sit
BELOW any namespace that could require both `fold` and `log-index`, so:

* **`log_index.clj` as home** — site 2 would need `fold` → `log-index` while
  `log-index` → `fold` for `update-beds`. Paying it means moving `replay`,
  `update-beds` and `bed-correction-event-types` out of `fold`, gutting the
  namespace the fifth extraction created.
* **A new namespace above both** — works for site 1 and for nothing else;
  sites 2 and 3 cannot call up.
* **`fold.clj`** — costs moving TWO pure-data `def`s down.

So `reinstatable-event-types` and `cited-opening-event-types` moved out of
`log_index.clj` into `fold.clj`, each leaving a delegating def under ruling
C1(a). They are apply-site policy rather than log queries — `log_index.clj`'s
own docstring said so ("the only ones `run`'s `:reinstate-index` records") —
and the move was measured before it was taken: **zero test references**,
**one live code consumer each** (site 1), and no live prose repoint.

**One STALE-BEFORE-THE-MOVE citation, disclosed and not fixed** under
`rulings.md#R-move-not-improve`: `notes/adr/0174-arc-3b-encounter-horizon-
scheduling-and-bed-status.md:577` cites `reinstatable-event-types` at
`engine.clj`, where it has not lived since the sixth extraction. Already
false when this move arrived.

## 4. Step 2 — stage 1

`ehrt.sim-engine.fold/apply-events`, `acc x events x projection -> acc'`.
`fold.clj` 155 → 407 lines. Three call sites, each naming its projection:
`run.clj:1336-1342`, `fold.clj`'s own `replay`, `log_index.clj`'s
`reinstated-state` fallback.

**Output-identical by construction, not by assertion.** The choke point's
order is site 1's, unchanged — decorate off the pre-batch world, take the log
ordinal off it, one per-event reduce, then the per-batch post-pass off the
post-reduce world — and every concern is guarded by its own projection
membership and by nothing else. The three things that depended on the census:
C2 kept `:state-history` a post-pass; C4 kept the two subject notions
computed independently; and `:patient-bootstrap` runs FIRST of the per-event
concerns, which is `replay`'s own prior semantics (its `:before`/
`:world-before` were the bootstrapped map) and is inert today because no site
holds bootstrap and an index together.

**Three requires went dead in `run.clj`** — `encounters`, `evolve`,
`log-index` — because their only uses there were inside the fold that
travelled. Same class the extraction phase named. `mark-warmup` travelled
too, so `warm-up-seconds` is a parameter slot rather than a closed-over
binding.

**Docstrings corrected in the same commit**, not left to drift: `fold`'s ns
(it owns the apply PATH now, not one site), `run`'s ns (sole event producer,
no apply fold; edge list nine siblings, not twelve), `log-index`'s ns (site
3's fallback and its `fold` edge both re-stated).

**Red-before-green does not apply** (prompt S1(a)), and none was claimed. The
co-landed invariant is `ehrt.sim-engine.apply-projection-test`: it
transcribes the census's three matrix columns as literal sets and asserts the
vars against them — deliberately a TRANSCRIPTION rather than a read of the
markdown, for the same reason `check.clj` writes out its own bed arithmetic,
so the two can disagree loudly. It also gates the closure subset relation,
site 3's alias identity, the 17/22 arithmetic, and that both delegating defs
hold the same objects.

## 5. The gates

* **`clojure -M:poly check`** — `OK`.

* **`bin/regression-oracle 3e65ff9 1bfbf09`** — the script's own output:
  `IDENTICAL: every root's digest matches between 3e65ff9 and 1bfbf09`, 41
  roots, `declared-digest-change: no (soundness: yes outside the leading
  docstring)`. No declaration was owed and none was made. `1bfbf09` is C2
  before its amend; `git diff 1bfbf09 7ab7cdb` is one line of
  `.agents/state-derived.md`, which the oracle does not read.

* **`bin/ground-truth-bracket 3e65ff9 7ab7cdb`** — run against the FINAL sha.
  Result in §7.

* **The live `-M:dev` drive**, the log-index session's precedent, and the
  thing that actually covers site 3 — the coverage paragraph the extraction's
  Done entry left standing applies here unchanged: **neither bracket reaches
  a cancel decide and the gated corpora resolve no citation**, so an
  IDENTICAL bracket proves nothing about the reinstatement path. What
  witnesses sites 2 and 3 is the suite's cancel family
  (`engine_test.clj`'s cancel family, `ehrt.sim.run-test/cancel-decides-
  reinstate-exactly-what-replay-would-hand-back`,
  `citation-resolution-matches-the-whole-log-scan`) and this drive — NOT the
  oracle. Said here so no claim of IDENTICAL is read as covering them.

  * the closure is 13 and the three projections are 11/3/3, matching the
    matrix;
  * `fold/replay-projection` is `identical?` to `fold/reinstated-projection`
    — correction C5's alias, live;
  * both moved sets are `identical?` through `fold/` and `log-index/`;
    `engine/replay`, `iface/replay` and `fold/replay` are all one object;
  * site 2 driven: 3 records, entry keys in their original order, statuses
    `[:new :new] [:new :admitted] [:admitted :discharged]`, bootstrap fired;
  * site 1 driven under `run-loop-projection`: log mirror 3, `:warm-up`
    `[true true false]` at a 150-second window, `:reinstate-index` keyed at
    idx 2 ALONE (`:registered`/`:admission` are not reinstatable),
    `:registration-index {"P1" 0}`, bed `RENAL-1` `:occupied` since t=100,
    state-history 3, transient log 3, and **no `:entries` key** — the omitted
    concern stays omitted;
  * **SITE 3 DOWN BOTH BRANCHES**: with a carried `:reinstate-index` it
    returns the carried entry; with no such key it takes the choke-point
    fallback under `reinstated-projection`; both return `:status :admitted`,
    `:location {:ward "Renal" :bed "RENAL-1"}`, `:attending "DR-1"`, and the
    two maps are `=`. The pre-discharge state, both ways.
  * a real `iface/run` end to end: 9 events, 3 patients, every event carrying
    a boolean `:warm-up`.

  **One thing the drive found about ITSELF, recorded because the first
  reading looked like a defect.** Branch A and branch B disagreed on the
  first run. The cause was the drive, not the code: a hand-built site-1 world
  with an EMPTY `:patients` map is not site 1 — `prelude` seeds every patient
  with `initial-patient` before the loop (`run.clj:1129`), which is precisely
  WHY site 1 omits `:patient-bootstrap` and why the census predicts that
  omission INERT. Seeded as `prelude` seeds it, the two branches are equal.
  The 1 × A7 cone prediction is the thing that explains the discrepancy, which
  is the census working.

* **The C2 probe.** The drive also compares a real run's `:state-history`
  against a per-EVENT reconstruction from `replay`'s `:world-after`. It
  returns TRUE at seed 424242 / 3 patients — which does not contradict C2: the
  two agree exactly when no batch carries two events for one patient, and
  this run has none. C2 is a statement about the fold's grain, not about
  today's corpora, and it is the grain the refactor had to preserve.

## 6. Two premise mismatches, disclosed rather than adapted around

* **C14 does not exist.** The prompt's step 3 says "C14's two live-surface
  corrections ride the docs commit if the author has so ruled". There is no
  `C14` anywhere in the tree — `grep -rn C14` over `.agents/`, `notes/` and
  `docs/` returns nothing, and `.agents/rulings.md` has been FROZEN since
  2026-08-25. The two live-surface items the prompt is almost certainly
  pointing at are the ones the P5 row itself names as unpaid: the 23
  `docs/operational-models.md` citations inside `components/sim/docs/`, and
  the 8 live citations of a gate `e189418` deleted. The row says of both
  "Both need a ruling, not a session". **No ruling is recorded, so neither
  rode this session's docs commit.** They stay where the repoint pass left
  them.

* **The suite's baseline was not clean, and the reason was this session.**
  The pre-change `make test` was started before the census files were
  written, and `.agents/state-derived.md` line-counts `.agents/plans/
  README.md`; the index row appended mid-flight turned the run red at
  `state-derived-md-matches-a-fresh-render-test`. The same gate fired a
  second time on the post-change run, that time because the new test
  namespace moved the count 219 → 220. Both are the freshness gate working.
  **C2 was AMENDED ONCE before push** to carry its own regenerated
  `.agents/state-derived.md` — disclosed here rather than absorbed, the same
  way the extraction phase's eighteenth session disclosed its own amend. The
  amend is `1bfbf09` → `7ab7cdb` and its whole diff is that one generated
  line.

## 7. Ledger, and the delta explained in-clone

Counting definition (`rulings.md#R-ledger-counting-definition`): `deftest` and
`defspec` together, as `clojure.test`'s own "Ran N tests containing M
assertions" lines report them, summed across every namespace `make test`
prints -- the same definition the repoint pass used, so its 4,751/24,161/408
is comparable.

| | deftests | assertions | namespaces |
|---|---:|---:|---:|
| baseline, `3e65ff9` | 4,751 | 24,161 | 408 |
| final tree | **4,755** | **24,193** | **410** |
| delta | +4 | +32 | +2 |

**Every unit of the delta is accounted for, measured rather than reasoned.**

* `ehrt.sim-engine.apply-projection-test`, NEW -- 2 deftests / 15 assertions,
  and it runs in BOTH the `conformance` and `ehrt-cli` projects, so it
  contributes +2 namespaces, +4 deftests, +30 assertions.
* `ehrt.docs-tooling.test-source-live-path-lint-test`, 161 -> 162 assertions
  -- it asserts once per test SOURCE file and this session added one. Also
  counted twice, so +2 assertions. Located by running `clojure -M:poly test
  brick:docs-tooling` in this clone and in a disposable `git worktree` at
  `3e65ff9`, and diffing the per-namespace lines: 4,898 -> 4,900, one
  namespace moved, and that one.
* 30 + 2 = 32, and no other namespace's counts moved.

**The baseline row is derived, not copied, and it agrees.** This session's own
pre-change `make test` was truncated -- it aborted in the `conformance`
project at the `state-derived` freshness gate (see §6) and never reached
`ehrt-cli`, so its 3,196/17,353/281 is a partial run and is NOT the baseline.
The row above is the final tree's own measured counts minus the delta above,
and it reproduces the repoint pass's recorded 4,751/24,161/408 exactly.

**One budget self-catch and one found-not-caused.** The P5 row's first draft
put `:onboarding` 6 lines OVER its 1,530 ratchet (1,536). Under
`rulings.md#R-budget-stop` the row was compacted rather than pushed: it now
reads **1,529, headroom 1**. Separately, `:docs` reads **787 against a 785
budget, headroom -2**, and that is PRE-EXISTING -- present in this session's
first `.agents/state-derived.md` render before any edit of its inputs. Found,
not caused, not fixed here; it is one line in an owning session's scope, not
this one's.

## 8. Close

* **`make test`** -- EXIT 0, zero failures, zero errors, on the settled tree.
  The run was restarted once: an earlier invocation was killed mid-flight
  because the roadmap compaction above changed the tree under it, and a suite
  that reads a file it is also racing is not evidence.
* **`bin/regression-oracle 3e65ff9 1bfbf09`** -- IDENTICAL, 41 roots, no
  declaration.
* **`bin/ground-truth-bracket 3e65ff9 7ab7cdb`** -- the script's own output:
  `IDENTICAL: every digested root's :ground-truth matches between 3e65ff9 and
  7ab7cdb (38 roots)`, coverage line `38 roots carry :ground-truth and are
  digested; 3 skipped (no such key): appendicitis.edn, ear-infections.edn,
  sore-throat.edn`. No declaration.
* Push, post-push message verification and the CI close marker via `gh`
  follow this commit; they are recorded in the close-marker commit under
  `rulings.md#R-session-verifies-ci-via-gh`.

**What this session deliberately did NOT do.** No accumulator was enabled or
disabled at any site -- the 22 omitted pairs are all still omitted. No
`interface.clj` edit. No draw-order change. Site 3 was not deleted, though
census 4d predicts it can be. The three OUTPUT-MOVING pairs stay omitted. The
two live-surface corrections the P5 row carries as unpaid did not ride, for
want of a ruling (§6). Stage 2 is not this session.
