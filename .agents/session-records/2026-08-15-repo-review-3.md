# 2026-08-15 — Repo review 3, with the rubric amendment as Step 0

Session record (charter R-A, `notes/ADRs.md` ADR-0023). Driving prompt
archived at `.agents/prompts/2026-08-15-repo-review-3.md`.

**Shape:** a `repo-review` run whose own Step 0 amended the instrument
it then executed (author's choice (b), channel post-mortem 2026-08-14).
Ends at the skill's own STOP: register and plan land, nothing is fixed,
rulings are the author's.

## What landed

| commit | contents |
|---|---|
| `dbbeb1f` | The rubric amendment: the population-closure law plus its three dimension patches (D5, D1, D7), `.agents/skills/repo-review/SKILL.md` and its `.claude/` mirror in the same commit |
| `bc6f46c` | `.agents/plans/2026-08-15-repo-review-findings.md` (40-row register), `.agents/plans/2026-08-15-repo-review-3-plan.md` (step-5 mitigation plan), plans index rows for both |
| this commit | Session record and archived prompt |

Tag paid at Step 0: `stable-20260815-result-nodes` at
`b139de58…`, annotated, pushed, remote peeled ref verified equal to
target — ADR-0135's deferred close tag, under license case (i) with
both conditions present (design-channel fresh-clone verification
relayed with the prompt, plus the author-side CI relay).

## Step 0 — the amendment

All four anchor texts named by the prompt were confirmed present in the
live skill file before any edit, and the mirror was confirmed
byte-identical before the amendment. The four edits landed verbatim as
prompted; the mirror was byte-copied in the same commit, which is the
commit its own descended gate (`skill_mirror_currency_test`) then had
to survive. CI green at `dbbeb1f`.

## Step 1 — the review

Baseline: `make test` from the verified-clean tree, **unpiped, with the
exit code captured explicitly** (`MAKE_EXIT=0`) — deliberately, because
the pipe-masking incident ADR-0135 caught is one of this review's own
named history-scan items. 636 `0 failures, 0 errors` occurrences,
16,315 passes, zero `FAIL in`/`ERROR in` lines. That 636 reconciles
**exactly** with ADR-0135's own recorded figure, by the same metric the
record names.

Prior arithmetic re-derivation (the skill's own step-4 standing
correction): review 2's summary line re-derives exactly in every figure
— 76 rows, 57/8/5/5 plus one non-tallied cross-reference. The first
review whose predecessor needed no fix-forward correction.

**Findings: 40 rows, 2 green / 5 yellow / 1 red.** Headlines:

- **D5 RED** — tree-first enumeration finds 10 derived artifacts where
  the make graph registers 5. Three of the unregistered five (the
  string-diagram skill's own `components/palgebra/examples/*.mermaid`
  teaching examples) are stale against their converter, byte-verified:
  0 `_out` nodes committed vs 6 regenerated, the delta being exactly
  ADR-0135's result-node feature. They now demonstrate the defect
  ADR-0135 was chartered to fix.
- **D1 YELLOW** — 25 dead markdown links on live reader-facing
  surfaces, all 25 inside `components/*/docs/`, zero outside it. The
  gate that should catch them (`stale_path_test.clj`) is scoped to
  `docs/` by its own docstring, and its own origin (P1-1, 2026-07-31)
  was this exact link family.
- **D6 GREEN (up)** — the census `:closure-file-count` undercount,
  carried since ADR-0071 and escalated without effect at review 2, is
  fixed (ADR-0094), verified by reading the live counting expression.
- Every review-1/review-2 finding open in D4, D7 and D8 is closed,
  verified against live code rather than the fix ADRs. Several fixes
  cite the finding id that caused them.

## Deviations, disclosed

1. **`bin/preflight --expect-tag` does not exist.** The driving prompt
   specified it; the script takes only `--branch` and exits 2 on
   unknown arguments. Preflight was run plain and the substance
   verified directly (`git rev-parse stable-20260814-exact-name^{}` =
   `46b82babf1e109f6a5748f175f8a687419a3ea3e`, exactly the commit the
   prompt named). Recorded as a prompt-vs-tree premise mismatch rather
   than silently adapted.

2. **This register's own first-draft arithmetic was wrong**, caught by
   applying the skill's own re-derivation law to itself before
   committing: the summary claimed 35 rows and 24/7/2/3, which did not
   sum. The mechanical recount gives 40 rows and 26/8/2/3 plus one
   cross-reference. Corrected fix-forward in the same commit, with the
   error disclosed in the register's own arithmetic note rather than
   quietly overwritten.

3. **Three probes did not run, and are recorded as blocked or partial
   rather than skipped** — the live fence battery (D8-5), the local
   cold-clone probe (D3-1, substituted with CI's own cold runner), and
   the full 44-ADR deviation read (D6-4). This session ran without
   sub-agents over a window four times review 2's. D8 is consequently
   held at YELLOW on an unrun probe rather than scored GREEN on the
   evidence actually gathered — scoring green on a battery that did not
   execute would be precisely the error this session's own amendment
   exists to prevent.

4. **The path sweep's first two passes were wrong and were discarded**,
   not reported. A naive extraction flagged 675 "unresolved" citations;
   reading hits in context identified four false-positive classes (this
   repo's shorthand citation convention, generator template sources
   whose links resolve at the generated output's location,
   `polylith-brief.md`'s external Polylith tutorial examples, and a
   `%20`-encoded filename that exists with literal spaces). Only after
   excluding all four does the defensible figure of 25 remain. The
   exclusion list is recorded at register row D1-8 so the proposed gate
   widening does not land noisy.

## Fences honoured

Step 0 touched only the skill file, its mirror, and the tag ceremony's
artifacts. Step 1 landed only the register, the plan, their index rows,
and these close artifacts. Zero fixes, zero `src`, zero regeneration
beyond what probes required read-only — `make docsgen` was run as D5's
own probe and produced zero bytes of diff; the three stale example
diagrams were regenerated to a scratch path for comparison and left
untouched in the tree.

## Horizon

The plan's four proposed fix sessions and three rulings, none executed.
Carried items unchanged and named in the register: the loopback flake
(still `state.md`-only, no roadmap anchor, 18 days), publish-prep
Externals (correctly parked), Wave E / vital-signs (anchored, aging).
