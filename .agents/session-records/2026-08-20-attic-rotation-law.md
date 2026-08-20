# Session record: the attic rotation law lands mechanical (2026-08-20)

**Prompt:** [`.agents/prompts/2026-08-20-attic-rotation-law.md`](../prompts/2026-08-20-attic-rotation-law.md)
**ADR:** [`notes/adr/0161-attic-rotation-law.md`](../../notes/adr/0161-attic-rotation-law.md)
**Mode:** R30, autonomous. **Base:** `891e57e`.

`roadmap.md#attic-rotation-law` (PRIORITY 5), ADR-0139 finding C-3.
Author ruling 2026-08-20 settles the judgement the row said was owed:
the law is MECHANICAL, no arc boundaries. `## Done` holds at most 30
LINES, rotation is an act of the close ceremony, oldest whole rows
first, attic append-only.

## Step 0

`bin/preflight` **exit 0**, no findings. Last five runs on `main` all
green; edit root `/home/mg/src/ehr-testing-tools`, not under `/mnt/`,
`core.fileMode` **true**, `core.ignorecase` unset; tree clean including
untracked; HEAD `891e57e` == `origin/main`; last stable tag
`stable-20260820-oracle-coverage-integration-half` @ `d5edf8a`, HEAD
untagged, DISCLOSED, **no tag owed**.

Baseline `make test`, unpiped, wrapper ending `exit "$MAKE_EXIT"`:

    MAKE_EXIT=0
    364 zero-failure blocks / 4,070 tests / 18,304 assertions
    Execution time: 14 minutes 15 seconds

Reconciles exactly against
[`2026-08-20-oracle-coverage-integration-half.md`](2026-08-20-oracle-coverage-integration-half.md),
whose close carries the same `364 / 4,070 / 18,304`. `clojure -M:poly
check` **OK** (it is `make test`'s own first line).

Budgets at Step 0: `:corpus` 1832/2045, `:docs` 735/785, `:judge`
922/1000, `:onboarding` **1508/1530** (22 lines of headroom, tightest of
five), `:sim` 1274/1405.

**The Done section, measured.** 71 CLOSED rows / 134 lines counting the
header, 2026-08-08 to 2026-08-20.

**The partition, predicted before acting and then executed.** Survivors
are the newest whole rows fitting under 30 lines with the header
counted: ADR-0160 `#oracle-coverage-gate-integration-half`, ADR-0159
`#repo-review-4`, ADR-0158 `#edit-root-worktree-residue`, ADR-0158
`#intake-staging-dir` -- **4 rows / 25 lines**. Rotated: **67 rows / 109
lines**, the live file's own lines 285-393, a contiguous tail cut.
ADR-0157 `#commit-msg-ascii-hook` is also dated 2026-08-19 and rotates:
oldest of its own day by file order, which is age order at the head of
this section.

**The reader census.** Six consumers of `## Done` or of `roadmap.md`'s
line count, each checked: `done_pointer_adr_test` (derives its
population from the file at read time -- no edit owed, and the prompt's
allowed one-line re-read was not needed), `roadmap_lint_test` (the real
risk was `every-cited-slug-resolves-test`; the three live slug cites all
address OPEN rows outside the rotate set, checked before acting),
`state_derived/parse-roadmap-rows` (regenerated), `reading_set_budget_
test` (the point of the exercise), `stale_path_test` (the attic is
explicitly outside its include-list, and a move into an equal-or-
narrower population cannot open a violation), and
`hand-owned-assets.edn`'s `:stale-row` anchor (an OPEN row, untouched).
**No reader assumes the full history. Zero findings; the STOP never
armed.**

Two facts re-derived here that decided the gate's shape, both in
ADR-0161: the 2026-08 attic has **twelve committed revisions, two of
which insert into the middle of the file**, so a byte-prefix test would
be red on unrewritable history; and **none of the twelve deletes a
line**, which is the enforceable form of the same law.

## Step 1 -- red

New namespace `components/docs-tooling/test/ehrt/docs_tooling/attic_
rotation_test.clj`, beside the ADR-0144 row-contract suite. Red run, in
full, nothing filtered:

    Testing ehrt.docs-tooling.attic-rotation-test

    FAIL in (done-section-is-within-its-line-cap-test) (attic_rotation_test.clj:102)
    the close ceremony rotates oldest whole rows to the current month's attic until ## Done is at or under 30 lines
    .agents/plans/roadmap.md's ## Done section is 134 lines, over its 30-line cap by 104 -- rotate oldest WHOLE rows verbatim into .agents/plans/roadmap-done-<yyyy-mm>.md, oldest first, until it fits (ADR-0161; the law is mechanical, no arc boundaries)
    expected: (<= (count section) done-line-cap)
      actual: (not (<= 134 30))

    Ran 7 tests containing 16 assertions.
    1 failures, 0 errors.

The other six tests -- including the whole append-only history walk over
both attic files and the pinned real-git deletion detector -- were green
at the red commit. Commit `f834286`, pushed with its green successor
(`rulings.md#R-red-pushed-with-green`).

## Step 2 -- green

The rotation, with three read-backs and no diffstat standing in for any
of them:

    READ-BACK OK: attic tail == pre-rotation roadmap.md lines 285-393, byte for byte
    READ-BACK OK: the pre-rotation attic is an unchanged byte prefix of the new one
    READ-BACK OK: sorted union of (live Done body + rotated block) == sorted pre-rotation Done body (133 lines)

`## Done` 134 -> 25 lines. Order preserved, NOT sorted: sorting would be
an edit, and it would have dissolved review 5's watch row W-10 specimen
(the ADR-0159 F-1 spliced row pair) in passing. The pair moves to the
attic in the broken shape it has, and the attic section header says so.

Surfaces, per `rulings.md#R-law-surface-propagation`: the `## Done`
header itself, `build-session` SKILL.md step 15 (+ HISTORY.md, + both
`.claude/skills` mirrors byte-equal), `.agents/rulings.md`'s new
`R-done-attic-rotation` row, `.agents/plans/README.md`'s attic index
line. Commit `b38adad`.

## Step 3 -- register hygiene, and the law's first ordinary act

`roadmap.md#attic-rotation-law` OPEN -> CLOSED. Its six-line CLOSED row
took `## Done` from 25 to **31**, so the rotation step fired against the
session that wrote it: ADR-0158 `#intake-staging-dir`, the oldest
survivor, rotated verbatim with its own three read-backs green, and the
section returned to **25 lines / 4 rows**. Recorded cheerfully -- the
cheapest possible demonstration that the law exempts no close, not even
its own.

**Review 3 finding C-3 is DISCHARGED.** ADR-0159 carried it as "ROWED,
OPEN, and worse each close" and did NOT place it on review 5's
thirteen-row watch-list; the roadmap row was its only register home, and
closing the row closes the finding.

## Budgets at close, re-measured

| set | actual | budget | baseline | headroom |
|---|---|---|---|---|
| `:corpus` | 1836 | 2045 | 2045 | 209 |
| `:docs` | 739 | 785 | 785 | 46 |
| `:judge` | 926 | 1000 | 1000 | 74 |
| `:onboarding` | **1400** | 1530 | 1530 | **130** |
| `:sim` | 1278 | 1405 | 1405 | 127 |

**The ratchet does not move, and this is the session's own disclosure
rather than the result its prompt predicted.** `:onboarding` 1,508 ->
1,400. ADR-0147's standing formula -- post-compaction actual x 1.15,
rounded up to the nearest 5 -- gives **1,610**, which is ABOVE the
committed baseline of **1,530**, and the ratchet forbids up. So the
budget HOLDS, exactly as ADR-0145 disclosed for this same set. The set
grew 180 lines between ADR-0147 (actual 1,328) and this session; the
rotation returns 109 of them. The baseline moves again at an actual of
**1,326 or below** -- 74 more lines. What was bought is headroom 22 ->
130, paid by compaction and not by a bump, which is what review 5's
watch row **W-13** asked for. The other four sets each gain the same
four lines of `build-session` SKILL.md and each holds.

## Close verification

Full `make test`, unpiped, wrapper ending `exit "$MAKE_EXIT"`:

    MAKE_EXIT=0
    366 zero-failure blocks / 4,084 tests / 18,336 assertions
    Execution time: 14 minutes 3 seconds

**Delta against Step 0 reconciled per namespace, not by subtraction.**
`diff` of the two runs' own `<namespace> <tests> <assertions>` tallies
reports exactly two added lines and no changed ones:

    > ehrt.docs-tooling.attic-rotation-test 7 16
    > ehrt.docs-tooling.attic-rotation-test 7 16

So `364 / 4,070 / 18,304` -> `366 / 4,084 / 18,336` is **the new lint
namespace and nothing else**. The prediction was right in kind and
under-counted the multiplicity: every docs-tooling namespace is executed
TWICE by `poly test :all` (once in the component's own run, once in the
run that carries it downstream) -- visible in the Step 0 log too, where
`done-pointer-adr-test` appears twice for the same reason. `clojure -M:poly
check` OK; `bin/verify-nist-lock` OK, 6 coordinates.

## Fences

Files touched: `roadmap.md`, `roadmap-done-2026-08.md`, one new test
namespace, `build-session` SKILL + HISTORY + both mirrors, `rulings.md`
(one row), `.agents/plans/README.md` (one line), the generated
`state-derived.md` / both record INDEXes / `notes/ADRs.md`, the ADR, this
record, the archived prompt.

`done_pointer_adr_test` NOT edited -- its population is derived, so the
allowed one-line re-read was unnecessary. `.agents/reading-sets.edn` and
`.agents/reading-sets-baseline.edn` NOT edited, for the arithmetic given
above. Zero `src`. Zero test deletions. No roadmap section touched beyond
the retired OPEN row and the new CLOSED one. No row text edited during
either rotation -- six read-backs prove it rather than assert it. The
oracle was neither touched nor run; no oracle claim is made or owed.
