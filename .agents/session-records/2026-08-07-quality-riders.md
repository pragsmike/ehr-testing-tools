# 2026-08-07 — Quality riders: the review arc opens

## Scope

Session prompt naming AR-QR-0 through AR-QR-4, opening the quality-
review arc: the `repo-review` skill lands, the ADR-0075-named flaky
test gets a mechanism fix, and preflight's own CI check widens from
one run to five. Full root-cause narrative and every ruling:
`notes/adr/0076-quality-riders.md`.

**A premise mismatch surfaced at preflight, before any git operation.**
The prompt's own premise: the `repo-review` `SKILL.md` pair sits
untracked, awaiting this session's first commit. The live tree said
otherwise — the pair was already committed directly to `main`
(`74ebc6b`, "Added repo-review skill."), off-ceremony, no session
record, no ADR entry. CI at that exact tip was red: `readme-presence-
test` failed because `.agents/skills/repo-review` had no `README.md`.
Traced to the exact root cause before touching anything (every sibling
skill directory in both `.agents/skills/` and `.claude/skills/` has a
README; this one didn't), then disclosed to the author via
`AskUserQuestion` with three options (fix forward now / stop and
report only / fix the README gap only and stop). Ruled: fix forward
now, disclose the ceremony bypass, continue the arc as planned — which
is what this record and ADR-0076 do.

This session's own preflight: working directory confirmed the ext4
clone; tip `74ebc6b` (one commit past the prompt's stated `9acb79b`,
the mismatch above). `clojure -M:poly check` OK. Full suite baseline:
RED, one failure, reproducing CI's own finding exactly. Last-five CI
conclusions on main disclosed (the very check AR-QR-3 widens,
exercised early): `74ebc6b` failure, four consecutive successes before
it. `stable-20260807-ci-current` tagged at `9acb79b` and pushed
(AR-QR-0).

## Red→green evidence highlights

- Step 1 (AR-QR-1): full suite red at baseline (1 failure,
  `readme-presence-test`) → green after adding the two missing
  READMEs and their index-entry mirrors (511 passes, 0 failures, 0
  errors). `index-completeness-test` also went red once the README
  existed but the top-level index didn't cite it yet — a second,
  cascading gate caught in the same pass, fixed in the same commit.
- Step 2 (AR-QR-2): no local red-first proof exists for either change
  (disclosed, not fabricated — see ADR-0076's own AR-QR-2 entry for
  why). Full suite green before and after (511/0/0 both). The fix's
  own proof is a soak across future CI runs, not this session's local
  green.
- Step 3 (AR-QR-3): a full-suite verification run surfaced a THIRD,
  previously unnamed intermittent failure —
  `ehrt.sim-engine.engine-test/every-churned-run-satisfies-the-
  invariant-catalog` (a `defspec` property test, unpinned generator
  seed) — failed once (seed `-60645`, 12 patients), passed clean on an
  immediate re-run with an identical tree (511/0/0). Confirmed
  intermittent and unrelated to this session's own touches (none of
  which reach `sim-engine`). Named for next-arc/review intake, not
  fixed — see Findings, below, and ADR-0076's own AR-QR-4 entry.

## Judgment calls and their ratification status

- **The ceremony-bypass fix-forward, above** — author-ruled live, via
  `AskUserQuestion`, before any git operation. Recorded verbatim in
  ADR-0076's own Context section.
- **The third flake (engine-test) was disclosed and deferred, not
  fixed**, matching the exact restraint ADR-0075 showed for the flake
  this session fixed — AR-QR-4's own fence ("no other flake-hardening
  sweeps") already ruled this in advance; applying it to a finding
  the prompt itself couldn't have named (it didn't exist as a known
  finding until this session's own Step 3 run surfaced it) is a
  live extension of that fence, not a deviation from it. Not yet
  separately ratified beyond the fence's own text — the assessment
  session's probe battery is where it's designed to land next.
- **No red-first test was written for AR-QR-2's two behavior changes**
  (the atomic temp-dir helper, the nil-vs-empty `.listFiles`
  distinction) — a deliberate choice over inventing a permission-based
  test whose own portability across CI runners (root vs. non-root)
  would risk manufacturing a fourth flake in the course of fixing the
  first. Disclosed in the AR-QR-2 commit message and ADR-0076, not
  silently skipped.

## Findings and HEAD landed

Two intermittent test failures on record after this session, neither
newly fixed: `merge-config-file-suggests-a-same-stem-sibling-file`
(ADR-0075's own finding — this session gives it a mechanism fix and a
soak plan, not a witnessed-red-then-green close) and
`every-churned-run-satisfies-the-invariant-catalog` (new this session,
named for next-arc/review intake, not investigated further). Also: the
ceremony-bypass gap itself (Context, above) — a commit landed directly
to `main` outside any session, found and fixed forward.

Commits, in order (this session): `d0129b9` (Step 1, the README
fix-forward), `9cc3563` (Step 2, the flake fix), `9a34409` (Step 3, the
preflight widening + Externals row), and this record's own closing
commit (Step 4).

## Verification

- `clojure -M:poly check`: OK, every step this session.
- Full suite: red at Step 0 baseline (matching CI); green after Steps
  1 and 2; one intermittent failure surfaced during Step 3's own
  verification run, green on immediate re-run — see above.
- `gitleaks git --staged -v`: clean, every commit; also clean on every
  push (pre-push hook), 729→732 commits scanned across three pushes.
- Post-push message verification, every commit: one delta each, the
  known harmless trailing-blank-line artifact.
- `bin/regression-oracle 9acb79b 9a34409`: all twenty-seven
  vendored-root batches IDENTICAL, soundness "yes outside ns form."
- Tag verification: `stable-20260807-ci-current` peeled ref resolves
  to `9acb79b` exactly.
- CI, this session's own three pushes, checked directly (not assumed):
  `d0129b9` success, `9cc3563` success, `9a34409` success.

## Deviations, disclosed

- **The prompt's own premise (untracked skill files) didn't hold** —
  already committed, off-ceremony, CI-red. Disclosed via
  `AskUserQuestion` before any git action; ruled fix-forward. Full
  account: ADR-0076's own Context section.
- **AR-QR-2 carries no local red-first proof** for either behavioral
  change, disclosed rather than manufactured — see Judgment calls,
  above.
- **A third, previously unnamed intermittent test failure surfaced
  mid-session**, outside this session's own named scope (AR-QR-4's own
  fence), disclosed and deferred to the assessment session rather than
  fixed or silently absorbed.
