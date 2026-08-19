# 2026-08-19 -- Review-4 fix 3/5: environment residue at its root and result-or-loud widened to the class it names

**ADR:** [`ADR-0157`](../../notes/adr/0157-environment-residue-and-result-or-loud.md).
**Prompt:** [`2026-08-19-review-4-fix-3-environment-and-result-or-loud.md`](../prompts/2026-08-19-review-4-fix-3-environment-and-result-or-loud.md).
**Mode:** R30, autonomous. **Pair:** plan Sessions **B + D**, plus
`roadmap.md#commit-msg-ascii-hook` folded in from ADR-0156's second
addendum.

## Step 0

- `bin/preflight` in the edit root: **exit 0, no findings.** Five green
  CI runs, tree clean, HEAD == `origin/main` at `c80b558`, last tag
  `stable-20260819-review-4-fix-2-oracle-and-guards` @`841fb75`, HEAD
  untagged, **no tag owed**.
- Baseline `make test` in a fresh clone at `c80b558`, unpiped, wrapper
  ending `exit "$MAKE_EXIT"`: **MAKE_EXIT=0, 358 blocks / 4,012 tests /
  18,008 assertions / 0 failures / 0 errors.** Reconciles exactly with
  ADR-0156's close figures. `clojure -M:poly check`: **OK**.
- (a) edit root `core.fileMode=false`, `core.ignorecase=true`,
  `core.hooksPath=.githooks`; fresh clone `true` / unset / unset.
- (b) no case-colliding tracked paths; all 32 tracked `bin/` and
  `.githooks/` files `100755`; zero tracked paths carry a non-ASCII byte.
- (c) 13 `.mkdirs` (10 in `components/*/src` + 3 in `bases/*/src` --
  the plan's 13 and the prompt's 10 are the SAME population under
  different grep shapes, no site removed) and 2 `.delete`. Zero
  `.createNewFile` / `.setExecutable` / `.mkdir` / `.deleteOnExit` /
  `.setReadable` / `.setWritable` / `.setLastModified`, so no residue row
  was owed.
- (d) Hooks install via `git config core.hooksPath .githooks`, pointing
  at the tracked directory; **nothing enumerates them by name**.
- (e) Exactly one non-ASCII commit message in the last 40: `04b6f66`.
  Predicted exactly, found exactly.

## What landed

Five commits, `c80b558..18076c8`, red pushed with green
(`rulings.md#R-red-pushed-with-green`):

| sha | commit |
|---|---|
| `80b3a62` | test: red -- B's six tests |
| `6069e40` | test: red -- kernel helpers + widened lint |
| `31a2152` | fix: preflight residue checks, commit-msg hook, the STOP |
| `992c580` | fix: 15 sites routed through the kernel |
| `18076c8` | docs: regenerate state-derived |

**B.** `bin/preflight` check 2 gains two residue checks beside the
`/mnt/*` check, each printing its own remedy. `.githooks/commit-msg`
refuses a non-ASCII message pre-commit. The byte scan is **extracted**
to `bin/ascii-scan`; `bin/post-push-verify` check 2 invokes the same
script, and both callers are fail-closed on a scanner they cannot run.
`executable_bits_test` kept, as the plan directed.

**D.** `ehrt.kernel.io` gains `mkdirs!`, `delete!` and the declared
exception `delete-quietly!`; 15 sites across 10 files routed; the lint
goes to five patterns and gains the population assertion it never had.

## STOP-AND-REPORT -- the edit-root config flip

D3-1's **"verified safe -- zero churn"** is false, and it is false in
this review's own signature way: it measured a **fresh clone** and
concluded about the **edit root**. The flip surfaces **360**
`mode change 100644 => 100755` on ordinary text files, zero content
change -- worktree bits `core.fileMode=false` had been hiding. Index is
sound and identical in both clones. **Edit root restored to as-found and
`git status` re-verified byte-identical to the pre-flip snapshot.**
Remedy (flip AND `chmod -x` sweep) is AUTHOR ACTION, rowed as
`roadmap.md#edit-root-worktree-residue`.

**Consequence, disclosed:** `bin/preflight` now exits 1 in this edit
root with two `FINDING:` lines until that remedy runs. True report of a
real condition, but every session's Step 0 will meet it.

## Deviations, disclosed

- **Staging hygiene missed two files.** `.githooks/commit-msg` and
  `bin/ascii-scan` were still staged from Step 2 when the D-red commit
  was made. `git diff --cached --stat` was printed and not READ, which is
  the point of the ritual (R26e). Corrected the only permitted way: the
  commit was unpushed and `rulings.md#R-amend-unpushed-message-only`
  allows a **message-only** amend, so the record was made true rather
  than the content rewritten.
- **The roadmap Done move happened at the close, not at Step 2** as the
  prompt ordered. `done_pointer_adr_test` requires a Done pointer's ADR
  to exist in the generated index, and ADR-0157 did not exist at Step 2;
  moving it there would have made `make test` red for a records reason.
  Same act, later step. Fix-forward with disclosure, one defensible
  reading (`rulings.md#R-stop-only-on-two-defensible-readings`).
- **Two fixtures outside the prompt's file list** were edited:
  `post_push_verify_range_test.clj` and `exit_truthfulness_test.clj`'s
  `build-pushed-fixture!` now copy `bin/ascii-scan` alongside
  `bin/post-push-verify`. Forced by the extraction the fence permits --
  the script is fail-closed on a missing scanner, so a fixture without it
  stops at check 2. Caught by the suite, not by inspection.
- **The commit-msg fold-in was judged INSIDE B's fence** and carried
  rather than stopped: it is `.githooks/` work on B's own ceremony
  surface, ADR-0156's addendum names it as the remedy, and "Q3 pair small
  ones" is the standing instruction.

## Verification

- **Full `make test`** at `18076c8`, unpiped, `MAKE_EXIT=0`:
  **358 blocks / 4,040 tests / 18,110 assertions / 0 failures / 0
  errors.** Against Step 0's baseline: blocks unchanged, **+28 tests,
  +102 assertions** -- 14 new tests (7 in `exit_truthfulness_test`, 7 in
  `kernel/io_test`), each counted in the two projects that run them.
- **`clojure -M:poly check`: OK**, including the new oracle -> kernel
  require edge (brick deps are wired at the project level, ADR-0011).
- **Oracle.** Predicted an abort on an undeclared digest-source diff,
  then `IDENTICAL` when declared. Both observed exactly:
  `bin/regression-oracle c80b558 HEAD` exit 1, *"DIFFERS outside the
  leading docstring -- STOP"*, showing the `:require` hunk **and** the
  `-main` hunk (that the require is visible at all is ADR-0156's L1-4 fix
  working); then `--declared-digest-change` exit 0,
  **`IDENTICAL: every root's digest matches`**, all 35 roots.
- **`bin/post-push-verify c80b558 HEAD`: all three checks green** --
  remote tip matches, every message in range pure ASCII (check 2 running
  through the extracted `bin/ascii-scan`), CI run reported once. And
  check 2 now has a **pre-commit twin**: `.githooks/commit-msg` gated
  every commit in this range as it was written.
- **Reading sets**, all under budget, none compacted:
  `:corpus` 1821/2045, `:docs` 728/785, `:judge` 915/1000,
  `:onboarding` 1438/1530, `:sim` 1267/1405.
- **CI:** see the close addendum on ADR-0157.

## Registers

D3-1 -> **PARTLY FIXED ADR-0157** (gate landed, flip re-rowed); D4-1 ->
**FIXED ADR-0157**. D4-4 read: already FIXED by ADR-0156 and about the
zero-population rubric sentence, not the `.delete` half -- unchanged.
Plan Sessions B and D marked landed. `roadmap.md#repo-review-4` carries
the fix 3/5 line; `#commit-msg-ascii-hook` CLOSED under `## Done`;
`#edit-root-worktree-residue` opened at the vacated PRIORITY 7.
`rulings.md#R-io-result-or-loud` carries a dated append naming the two
patterns and the three helpers -- still one row, still within the
three-line cap.
