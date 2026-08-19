# Session record — review-4 fix 1/5: closure gates (G) and harness truthfulness (A)

**Date:** authored 2026-08-18, executed 2026-08-19.
**Mode:** R30 (commit and push at each checkpoint, unattended).
**ADR:** [ADR-0155](../../notes/adr/0155-closure-gates-and-harness-truthfulness.md).
**Prompt:** [2026-08-18-review-4-fix-1-closure-and-harness.md](../prompts/2026-08-18-review-4-fix-1-closure-and-harness.md).
**Range:** `7d998f0..` — `7a6bf04`, `bf0b381`, `c6eddb9`, `f704c91`, and this close.

## Step 0

`bin/preflight`, the **COMMITTED** script at `7d998f0` (this session
changes it, so the before/after is the point):

```
-- 1. Last five CI runs on main --   five green
-- 2. Edit-root confirmation --      OK: not under /mnt/
-- 3. Tree-clean check --            OK: clean, untracked included
-- 4. HEAD-vs-remote tip match --    OK: matches origin/main
-- 5. Last stable-* tag --           stable-20260818-repo-review-4 (0a07195)
                                     DISCLOSED: HEAD is not currently tagged
exit 0
```

Baseline `make test`, unpiped, `MAKE_EXIT` captured:
**MAKE_EXIT=0, 348 blocks / 3,960 tests / 17,758 assertions**, 0
`FAIL/ERROR`. **Reconciles exactly with ADR-0154.** `poly check` OK.

Reading sets at Step 0, all under budget and every budget at its
baseline: `:onboarding` 1401/1530, `:corpus` 1801/2045, `:sim`
1247/1405, `:judge` 895/1000, `:docs` 708/785.

### The five measurements, taken before editing

| | measured | outcome |
|---|---|---|
| (a) docsgen write set, one run in a scratch worktree | **52 tracked files**, tree already byte-fresh | vs the 19 diff-list paths: **0 uncovered**, **1 list entry covering nothing written** |
| (b) palgebra: 5 equations files, 3 `.mermaid` | `lemon-pie`, `decision-monad` unrendered | **intentional** — the recipe comment already said so; gate asserts a DECLARED exception list |
| (c) pages vs case ids | **22 == 22** | prediction confirmed; L3-9 latent, not live |
| (d) "tees" comment sites | **6 sites in 4 files** | matches the plan exactly; 3 further `tee` hits correctly untouched |
| (e) preflight with `gh` failing | `FAIL:` then **`OK: last five runs all green`**, **exit 0** | the red witness, verbatim |

(a)'s single reverse hit, `components/sim/docs/sim-theory-diagram.md`,
is neither of the two readings offered: `Makefile:146` copies it only
`cmp -s ... || cp`, so a fresh file keeps its mtime. **The diff list
needed no change at all.**

(c) was measured wrong first — 21, from an extraction regex that missed
the first case's `[{:id` (bracket before brace). Corrected before use.

## Checkpoints

| commit | what |
|---|---|
| `7a6bf04` | G red — 4 failures / 32 assertions |
| `bf0b381` | G green — closure gate, declared edge, prune, exception list |
| `c6eddb9` | A red — 7 failures / 24 assertions |
| `f704c91` | A green — exit truthfulness on every surface |

Two pushes, each preceded by a full unpiped `make test`:

- push 1 `7d998f0..bf0b381` — **MAKE_EXIT=0, 350 / 3,972 / 17,828**
- push 2 `bf0b381..f704c91` — **MAKE_EXIT=0, 352 / 3,990 / 17,876**

Per-namespace reconciliation against Step 0's 348 / 3,960 / 17,758:
`+2` blocks and `+12` tests at push 1 = `docsgen-closure-test`'s 6
deftests, once per project (conformance, ehrt-cli); `+70` assertions =
34 × 2, plus `io-vocabulary-lint-test` scanning one more `src` file × 2.
`+2 / +18 / +48` at push 2 = `exit-truthfulness-test`'s 9 deftests × 2
and 24 assertions × 2. No other namespace moved.

## Gates that fired on this session's own work

Both were caught by a full unpiped run reporting **MAKE_EXIT=2** — the
law this session lands, paying for itself twice inside it.

1. **`io-vocabulary-lint-test`** on the use-cases prune's first draft
   (bare `.listFiles`). Rewritten through
   `ehrt.kernel.interface/list-files`, result-or-loud.
2. **`state-residue-test`** on the new MAKE_EXIT clause taking
   `.agents/state.md` to 124 lines over its 120 cap. Compacted, not
   raised: the clause folded into the existing paragraph and the dated
   anecdote it carried retired VERBATIM to
   `.agents/plans/state-history-2026-08.md`. 119 lines.

A third, in-session design decision: the taught-idiom lint flagged this
session's own HISTORY.md negative example. Resolved with a **declared**
`ANTI-PATTERN` marker rather than by weakening the rule or dropping the
example.

## Plant-and-withdraw — six plants, none remaining

Run against the GREEN baseline rather than inside the red commits, so
each planted red is unambiguous (disclosed reordering of Step 1).

| # | plant | red | withdrawn |
|---|---|---|---|
| 1 | drop `operators-doc` from `docsgen:` | reverse closure names `docs/operators.md` | byte-identical |
| 2 | drop `docs/operators.md` from the diff list | forward closure, same file | byte-identical |
| 3 | `event-schema-freeze` onto `docsgen:` | **two** gates | byte-identical |
| 4 | orphan page | page-set closure | removed, 22 pages |
| 5 | pre-fix taught idiom | lint names both mirrors | byte-identical |
| 6 | strip the `ANTI-PATTERN` marker | lint names both mirrors | byte-identical |

`git status --porcelain` carries only intended edits; a tree-wide grep
for the plant markers returns nothing.

## Close

`bin/preflight`, the **NEW** script, with `gh` failing:

```
FAIL: gh run list failed: / HTTP 401: Bad credentials
UNKNOWN: CI status could not be determined -- the query above FAILED.
         This is NOT a green report...
exit 1
```

Before → after: `OK: last five runs all green` + exit 0 becomes
`UNKNOWN:` + exit 1.

`bin/post-push-verify` — the **NEW** script — after push 2: remote tip
matches, per-commit ASCII clean, CI reported once (AR-CI-4), exit 0.

CI, both verified by this session's own `gh run view`
(`R-session-verifies-ci-via-gh`): run **32250959906** at `bf0b381`
concluded **success**; run **32253127894** at `f704c91` concluded
**success**.

Final full `make test` before the close push: **MAKE_EXIT=0, 352 /
3,990 / 17,876**, 0 failures — identical to push 2, as expected, since
the close touches records and generated docs only.

Reading sets at close: `:onboarding` 1412/1530, `:corpus` 1807/2045,
`:sim` 1253/1405, `:judge` 901/1000, `:docs` 714/785 — all under
budget, no budget moved (`R-budget-stop` untouched).

Registers: 12 rows carry `; **FIXED ADR-0155 (2026-08-19)**` as a DATED
APPEND with the original disposition token still visible; plan sessions
A and G marked landed; `roadmap.md#repo-review-4` stays OPEN, compacted
to hold its six-line cap; `rulings.md` gains
`R-preflight-fail-closed` and `R-full-suite-before-push` gains the
wrapper clause.

## An observation, out of fence

At both pushes, `post-push-verify` check 3 printed `status=
conclusion=<pending>` with an EMPTY status and no URL — the run was not
yet indexed and `gh` returned a shape the `[ "$run_line" = "null" ]`
guard does not catch. Not a regression, not touched (the fence limits
check 3 to the `UNKNOWN:` rendering), recorded for a later session.
