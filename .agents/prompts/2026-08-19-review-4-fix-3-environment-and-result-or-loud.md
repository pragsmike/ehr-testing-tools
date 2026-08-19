# Archived prompt: review-4-fix-3-environment-and-result-or-loud (2026-08-19)

Session prompt -- review-4 fix 3/5: environment residue at its root
(plan Session B, + `#commit-msg-ascii-hook` folded in) and result-or-
loud widened to the class it names (plan Session D) -- ADR-0157

## Context

Claude Code under R30 in ehr-testing-tools, third fix session of the
repo-review-4 arc. HEAD at handoff: c80b558 (ADR-0156 second addendum;
tree clean; CI green at 841fb75, 04b6f66, c80b558; last tag `stable-
20260819-review-4-fix-2-oracle-and-guards` @841fb75, no tag owed).
Author rulings on the plan 2026-08-18: "Q1 accept all recommendations.
Q2 that order ok. Q3 pair small ones." This prompt is B+D. Rows by id
are `.agents/plans/2026-08-18-repo-review-findings.md`; sessions by
letter are the plan's Part 2 (B at :354, D at :408). Quote the row.

Folded in from fix 2/5 (ADR-0156 second addendum,
`roadmap.md#commit-msg-ascii-hook` P7): the ASCII commit-message law is
gated only by `post-push-verify` check 2, which runs after the push --
fix 2/5's own addendum commit `04b6f66` carries a U+2026 and could not
be amended under `R-amend-unpushed-message-only`. A
`.githooks/commit-msg` hook refuses it pre-commit. Same surface as B
(`.githooks/`, `bin/preflight`), so it rides here with its row closed,
not fixed in passing.

## Channel anchors at c80b558 (re-derive every one)

* B. The edit root on `penny` carries `core.fileMode=false` and
  `core.ignorecase=true` (ADR-0154 D3-1; `/mnt/c`-era residue on an ext4
  clone); a fresh clone has `core.fileMode=true` and a byte-clean
  tracked tree (plan: "verified safe"); no tracked paths collide
  case-insensitively. The executable-bit class hit three times (ADR-0147
  S-7, ADR-0149, and the ADR-0154 preflight disclosure) and is gated at
  the symptom by `executable_bits_test` (index-mode read, keep it).
  `bin/preflight` has an environment section with the `/mnt/*` check
  (find its line; the prompt will not guess it -- fix 1/5 rewrote the
  file). `.githooks/` has `pre-commit` and `pre-push`; no `commit-msg`.
  `post-push-verify` check 2 is the ASCII scan -- reuse its byte test in
  the hook, do not write a second one.
* D. `io_vocabulary_lint_test.clj:31` `forbidden-patterns` = `.listFiles`
  / `.list` / `.renameTo`, kernel allowlisted; `kernel/io.clj` has
  `list-files` (:26) and `rename!` (:61) -- no `mkdirs!`/`delete!`. Live
  counts at c80b558 by a plain grep: `\.mkdirs\b` in `components/*/src`
  = 10 (the plan said 13 at 0a07195 -- re-derive both ways: the delta is
  either sites removed since, or a grep-shape difference; state which),
  `\.delete\b` = 2. `.mkdirs` false is ambiguous (existed vs failed):
  the helper is `(or (.mkdirs f) (.isDirectory f))` else throw with the
  path; `.delete` false is unambiguous on an existing path: `delete!`
  throws if the file still exists after.
* Do NOT widen to `.createNewFile`/`.setExecutable`/`.mkdir` in this
  session; if the grep shows them ignored, ROW them (one row, counts
  stated), keep D's fence at the two the plan names.

## Read first

1. Register rows D3-1, D3-2 (already FIXED ADR-0156 -- read for the
   ci-parity context only), D4-1, D4-4; plan Sessions B, D;
   `roadmap.md#commit-msg-ascii-hook`; ADR-0147 S-7, ADR-0149 finding 3,
   ADR-0154 SS D3 and SS D4, ADR-0156 second addendum.
2. `bin/preflight` whole (post-ADR-0155 shape; its exit is
   load-bearing); `bin/post-push-verify` check 2; `.githooks/pre-commit`,
   `pre-push`, and how they are installed (`core.hooksPath`? a `make`
   target? SETUP.md says -- find it; the new hook must install the same
   way); `executable_bits_test`; `exit_truthfulness_test` (the bin-
   script behavioral test shape).
3. `io_vocabulary_lint_test.clj` whole; `kernel/io.clj` whole (ADR-0078's
   result-or-loud contract, docstring conventions); every
   `.mkdirs`/`.delete` site (grep at Step 0, list them in the ADR with
   the brick each lives in -- note the kernel's own site).
4. `rulings.md#R-io-result-or-loud`, `#R-amend-unpushed-message-only`,
   `#R-full-suite-before-push`, `#R-red-pushed-with-green`,
   `#R-session-verifies-ci-via-gh`, `#R-empty-population-is-red`;
   build-session skill; `:sim` reading set (kernel is in it).

## Author rulings, verbatim

* "Q1 accept all recommendations. Q2 that order ok. Q3 pair small ones."
  (2026-08-18) -- B and D as the plan states them; the commit-msg hook is
  the channel's fold-in of a row the author has not separately ruled: if
  you judge it out of B's fence, STOP-AND-REPORT rather than skip (one
  defensible reading each way -- the hook is `.githooks/` work like B's
  preflight check, or it is a new gate that deserves its own ruling).
* Tag: no tag owed at Step 0. This session's own close tag: pay
  in-session if its tip run concludes success while open, else next Step
  0 -- say which.

## Step 0

Fresh clone, tip c80b558; `bin/preflight`; baseline `make test`
unpiped, MAKE_EXIT captured, wrapper ends `exit "$MAKE_EXIT"`,
reconcile vs ADR-0156's 358 blocks / 4,012 tests / 18,008 assertions;
`poly check`; reading sets vs baselines. Then: (a) in the EDIT ROOT
(not the fresh clone): `git config --local --get core.fileMode`,
`core.ignorecase`, `core.hooksPath`; in the fresh clone the same --
record both; (b) `git ls-files | tr A-Z a-z | sort | uniq -d` = empty
(no case collisions); `git ls-files -s bin/ .githooks/` modes -- all
100755 (the class is quiet today); (c) the `.mkdirs`/`.delete` site list
with bricks; (d) how hooks are installed (the exact mechanism -- the
commit-msg hook must ride it); (e) `git log --format=%B
c80b558~40..c80b558 | LC_ALL=C grep -nP '[^\x00-\x7F]'` -- every
non-ASCII commit message in the last 40: expect exactly `04b6f66`; that
is the hook's red witness.

## Step 1 -- B red

(i) `bin/preflight` environment section: a check that `core.fileMode` is
`true` (or unset on a filesystem that supports modes) and
`core.ignorecase` is unset/false, rendering `FINDING:` otherwise -- a
behavioral test in `exit_truthfulness_test`'s shape: run preflight in a
scratch clone with `core.fileMode=false` set, assert `FINDING:` and
non-zero exit; with the defaults, no such finding. (ii)
`.githooks/commit-msg`: test that a message file containing U+2026 is
refused (exit non-zero, names the byte/offset) and an ASCII one passes
-- invoke the hook script directly on a temp file. (iii) a test that the
hook installation mechanism from Step 0(d) installs `commit-msg`
(whatever form: `core.hooksPath` points at `.githooks/`, or the `make`
target copies all three). Commit: "test: red -- preflight flags
fileMode/ignorecase residue; commit-msg hook refuses non-ASCII; hook is
installed (ADR-0157, review-4 B, #commit-msg-ascii-hook)"

## Step 2 -- B green

`bin/preflight`: the two config checks beside the `/mnt/*` check, same
rendering shape, with the one-line remedy printed (`git config --local
core.fileMode true` / `--unset core.ignorecase`).
`.githooks/commit-msg`: reuse post-push-verify's byte scan (extract to a
tiny shared `bin/lib` fn iff both can source it without a second copy --
say which); exit 1 with the offending line and byte. Installation per
Step 0(d). `docs/SETUP.md` (or wherever hooks are documented) one line.
THEN, in the edit root on `penny` -- this is the one act outside the
repo the plan asks for -- `git config --local core.fileMode true && git
config --local --unset core.ignorecase`; run `git status` and assert it
is STILL clean (plan: zero churn); record the before/after config in the
ADR. If it is NOT clean, STOP: the plan's "verified safe" was measured
at 0a07195 and something moved. `executable_bits_test` stays. Roadmap
`#commit-msg-ascii-hook` -> CLOSED. Full `make test`; push red+green.
Commit: "fix: preflight catches fileMode/ignorecase residue at its root;
commit-msg hook gates the ASCII law pre-commit; edit-root config
corrected and recorded (ADR-0157, review-4 B, #commit-msg-ascii-hook)"

## Step 3 -- D red

(i) `kernel/io.clj`: `mkdirs!` and `delete!` per the contract above, with
`io_test` cases: `mkdirs!` on a new path, on an existing dir (ok), on a
path whose parent is a FILE (throws, names the path); `delete!` on an
existing file (ok), on a missing file (say: throw or ok? --
`java.io.File.delete` on a missing file returns false; the contract here
is "the path does not exist afterwards", so ok -- state it), on a
non-empty dir (throws). (ii) `io_vocabulary_lint_test`
`forbidden-patterns` + `\.mkdirs\b` and `\.delete\b`, kernel allowlisted
as for `.listFiles`; red on the 10+2 sites. `R-empty-population-is-red`:
the lint already asserts its population? -- read; if not, add it.
Commit: "test: red -- mkdirs!/delete! in the kernel; io vocabulary lint
forbids bare .mkdirs/.delete (ADR-0157, review-4 D)"

## Step 4 -- D green

Route every site through the kernel helpers -- one commit, mechanical,
but READ each site: where a `.mkdirs` return was already checked by
hand, replace the check, do not double it; where `.delete` was "best
effort cleanup" (e.g. a temp file in a `finally`), decide per site
whether loud is right (a `finally` that throws masks the original
exception -- for those, `delete!` inside `try`/swallow WITH a comment
naming why, or a `delete-quietly` kernel variant if >=2 sites need it;
say which and count). Oracle: the sites are filesystem plumbing, not
digest inputs -- predict `bin/regression-oracle c80b558 HEAD` IDENTICAL
and assert it (the oracle writes out-dirs via these helpers now; if the
digest moved, the helper changed behavior -- STOP). Full `make test`;
push red+green. Commit: "fix: result-or-loud widened to the class it
names -- mkdirs!/delete! route <n> sites through the kernel; lint gates
the two patterns (ADR-0157, review-4 D)"

## Step 5 -- register hygiene

Rows D3-1, D4-1 (+ D4-4 if its text is closed by the `.delete` half --
read it) -> `FIXED ADR-0157` dated APPEND; plan B, D marked landed;
roadmap `#repo-review-4` line -> "fix 3/5 (B+D) ADR-0157" (at cap,
compact); `#commit-msg-ascii-hook` CLOSED under `## Done`; any
`.createNewFile`/`.mkdir`/`.setExecutable` residue from Step 0(c) -> ONE
new row with counts. `rulings.md`: `R-io-result-or-loud` dated append
naming the two added patterns and the helpers (not a new row).

## Close (self-archive FIRST)

Archive to
`.agents/prompts/2026-08-19-review-4-fix-3-environment-and-result-or-loud.md`;
open the session record; then ADR-0157 (Step 0 a-e; the edit-root config
before/after and the clean-status proof; the site table with per-site
decision; oracle result), registers, session record with `gh run view`
id/conclusion, full `make test` reconciled per namespace vs Step 0,
`bin/post-push-verify` (all three checks -- and check 2 now has a
pre-commit twin; say both are green), tag per ruling. Commit: "docs:
ADR-0157 -- review-4 fix 3/5: environment residue and result-or-loud,
close"

## Fences

Files: `bin/preflight` (environment section ONLY), `.githooks/commit-msg`
(+ installer + SETUP line), `bin/post-push-verify` ONLY if the byte scan
is extracted (behavior unchanged), `kernel/io.clj` (two helpers +
tests), the `.mkdirs`/`.delete` call sites (routing only -- no other
logic in those lines), `io_vocabulary_lint_test`,
`exit_truthfulness_test`, registers; the ONE out-of-repo act is the
edit-root config change, recorded; NO other preflight check touched; NO
emitter/engine/check logic; oracle IDENTICAL (assert); every planted red
withdrawn; exit codes unpiped; anchored register edits, dated appends;
ASCII commit messages -- and this session's own messages pass the hook
it lands (the law applied to itself); R-RP. READ-BACK: files touched vs
this list; site count routed vs Step 0(c); preflight before/after on the
residue clone; the edit-root `git status` after the config change.
