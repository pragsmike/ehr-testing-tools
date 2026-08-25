# 2026-08-25 — de-scaffold

Author ruling of 2026-08-25, quoted by the driving prompt: **"go as
written."** One session, one commit, **no ADR, no tag** — which is
itself the first thing the new rules say. Prompt archived at
[`../prompts/2026-08-25-de-scaffold.md`](../prompts/2026-08-25-de-scaffold.md).
Base `d6ad63a`.

## What was deleted

| what | count |
|---|---|
| `bin/` ceremony scripts (`tag-ceremony`, `close-scaffold`) | 2 files, 322 lines |
| docs-tooling gates (9 namespaces) | 9 files, 1,382 lines |
| `roadmap_lint_test/no-row-exceeds-six-lines-test` | 1 deftest |
| skills, both trees (`capture-session`, `committee`, `errata-sweep`, `extraction-stage`, `find-skills`, `handoff`, `manual-review`, `repo-adaptation`, `repo-review`, `review`, `shared-skill-layout`) | 11 skills × 2 trees |
| `rulings.md` rows | 13 of 126 → 113 |
| `roadmap.md` OPEN rows reduced to one line each | 25 of 31 → 6 payload rows |
| `state.md` | 119 lines → 83, rewritten from scratch |

`.agents/` + `notes/adr/` went **126,335 → 119,921 lines**, −6,414.
Clojure is 66,721. The ratio went from 1.90 to 1.80 lines of scaffolding
per line of code — a real cut, and nowhere near enough on its own; what
actually changes the slope is the moratorium, not this commit.

Deleted gates, with what they gated: `tag_law_test` (the retired tag
formulations), `prompt_record_pairing_test` (record↔prompt one-to-one),
`notes_prompts_frozen_test` (the frozen prompt archive), `attic_rotation_test`
(the `## Done` rotation law), `done_pointer_adr_test` (`## Done` rows pointing
at ADRs), `state_staleness_tripwire_test` (state.md currency),
`reading_set_budget_test` (the reading-set line-count ratchet),
`state_residue_test` (state.md's cap and residue lint), `rulings_lint_test`
(the rulings row contract). Every one of them gated a rule about how the
repo writes about itself. Not one gated behaviour.

## What was kept, and why

**Every gate over payload.** All 45 surviving docs-tooling namespaces,
every sim/judge/corpus test, `poly check`, `verify-nist-lock`. The delete
list was chosen so that no gate over code, vendored bytes, generated
surfaces, the CLI or the operator path was touched.

**Six skills**: `build-session`, `probe`, `scenarios`, `session-prompt`,
`string-diagram`, `wsl-windows-git-hygiene`, plus `README.md`. The mirror
is `diff -r`-identical, 18 files each side.

**113 rulings rows**, frozen. Rows stay when a live surface cites them —
that is now the only reason a row exists. Six were marked RETIRED in
place rather than deleted: the three tag rulings, `R-arc-closes-in-own-session`,
`R-review-cadence-in-adrs`, and `R-session-verifies-ci-via-gh` (whose
`gh run view` half survives as the close marker, and which
`process_law_citation_test` still vouches for).

**Six roadmap rows**: `#performance-residual-sites` and traffic-scale
arcs 1–4, plus `#corpus-player-slices`. The other 25 keep one line each
so the slugs that cite them still resolve — `hand-owned-assets.edn`'s
`:stale` row depends on exactly that.

## The one gate this session ADDED (step 1, Q-C)

`R-witness-population-is-counted` landed as a gate, not a row.
`components/sim/test/ehrt/sim/run_test.clj`'s two ADR-0163 gates asserted
`(empty? (unpaired-ends ...))` over corpora measured at **zero**
`:medication-end` and **zero** `:care-plan-end` — vacuous, exactly as
review 5's L1-1 found. Each now also pins the counted witness from the
one gated run that produces both, `:adhd-seed-2`: one cited
`:medication-end`, one cited `:care-plan-end`, `unpaired-ends` run over
that non-empty population, and each gate's own zero disclosed as a
pinned count so a drift either way is loud.

**Red before green**, witnessed: the two pinned counts were set to 2 and
the brick run — `2 failures, 0 errors`, each failure printing the actual
witness event. Restored to 1 — `132 passes, 0 failures, 0 errors`,
31 seconds.

## Premises of the prompt that were wrong

1. **"OPEN rows reduced to payload: … NIST engine wiring, guide
   chapters."** Neither names a row. `grep -i nist .agents/plans/roadmap.md`
   finds one row, `#nist-licensing`, an EXTERNAL author action about a
   licensing gist, not engine wiring; "guide chapters" is
   `#guide-ch24-notes`, also EXTERNAL. Both are explained by reading
   "every other row" as "every other OPEN row" — which is how step 5 was
   executed. No substitute was invented.
2. **"`.agents/`+`notes/adr/` is 125,551 lines."** Measured at `d6ad63a`
   it is **126,335** (all files) or 119,667 (`*.md` only). The direction
   and the argument are right; the figure is not reproducible as stated.
3. **The skills keep-list keeps `probe` and deletes `committee`.**
   `probe` IS the scenarios→committee pipeline run N times: its SKILL.md
   reads `.agents/skills/committee/roster.md` and calls `/committee` as
   its funnel half. `probe` and `scenarios` now both cite a skill that no
   longer exists. Executed as written; flagged rather than fixed, because
   restoring `committee` would defy the explicit list and no fence
   permits it.
4. **"`.agents/memory/`: correct what ADR-0170's L-3 found stale."**
   `.agents/memory/` holds one file, `README.md`, and has been empty of
   content since instantiation; L-3 found nothing in it. The finding is
   **L3-12**, and it names the agent's *out-of-repo* memory files
   (`reference_make_test_runtime.md`, `project_arc_0_performance_closed.md`).
   Those were corrected; nothing in `.agents/memory/` needed to change,
   and no maintenance promise was added anywhere.
5. **"Remove their source-side helpers only if nothing else calls
   them."** There were none. Of the nine deleted namespaces only
   `reading_set_budget_test` required anything from `src` —
   `state-derived/line-count` and `total-lines` — and both are used by
   `state_derived.clj` itself and by `state_derived_test`.
6. **"`Makefile` targets that only served these go too."** None existed.
   `grep -n 'close-scaffold\|tag-ceremony' Makefile` is empty; the only
   Makefile mention of a deleted gate is a comment citing
   `state-residue-test`, left as history in a comment block about why
   `state-derived` exists.

The line numbers the prompt cites (`run_test.clj:809`, `:825`,
`unpaired-ends` at `:772`) were all correct at `d6ad63a`.

## Fences

No `src/` change outside step 1's test — held; the only non-test,
non-doc file touched is `bin/preflight`, a comment naming two deleted
scripts. No ADR. No tag. No new rulings rows. No new skills.

Four kept surfaces were edited past the letter of their step, each
because this session's own deletions made them false:
`build-session/SKILL.md` steps 11/13/15 and its "Done when" boxes,
`session-prompt/SKILL.md`'s close-out bullet, `bin/preflight`'s
fail-closed comment, and `state_derived_test.clj`'s failure hint. Each
told a session to run `bin/close-scaffold` or `bin/tag-ceremony`. No
deleted gate was re-added.

## Verification

**Full `make test`, unpiped, `MAKE_EXIT` captured, wrapper ending in
`exit "$MAKE_EXIT"`.** Windows-side `LoadPercentage` sampled at **4%**
immediately before the run began — quiet penny.

| | |
|---|---|
| `MAKE_EXIT` | **0** |
| wall | **863 s (14m23s)** |
| poly `Execution time` | **824 s (13m44s)** |
| namespace-runs / tests / assertions | **352 / 4,046 / 18,088** |

Both clocks named, because mixing them is what flipped a recorded delta's
sign in this window (ADR-0170, L3-1).

**Reconciled against `d6ad63a`'s 370 / 4,166 / 18,690, exactly:**

- namespace-runs **−18** = the 9 deleted namespaces × 2 projects.
- tests **−120** = 59 deftests in those 9 namespaces + 1
  (`no-row-exceeds-six-lines-test`) = 60, × 2 projects.
- assertions **−602** = 618 deleted (309 per project) less the **16**
  added by step 1 (4 assertions × 2 gates × 2 projects). The sim brick
  alone moved 124 → 132 across the same edit.

**A first run was RED and is disclosed rather than dropped.**
`MAKE_EXIT=2` at 484 s, one failure:
`state-derived-md-matches-a-fresh-render-test`, `:onboarding` 1345 vs
1340. Cause: this session re-wrapped four long lines in `rulings.md`
**while that run was in flight**, which moved a line-counted input. That
is hazard 2's neighbour and it is now hazard-adjacent text in
`state.md`: never edit a tracked file mid-run. `make state-derived` was
re-run and the suite re-run clean over the final tree with no edits
during it; only the second run is the pre-push gate.

## Landing

Filled after push.
