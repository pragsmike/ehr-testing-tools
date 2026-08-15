# 2026-08-15 -- review-3 fix session C: post-push-verify range derivation, build-session pipe discipline, gate category honesty

Repo review 3's third fix session
(`.agents/plans/2026-08-15-repo-review-3-plan.md` Session C), on the arc
ruling *"accept all."* (2026-08-15). Lands ADR-0138 and closes register
rows **D1-6, D2-6, D4-3**.

Ceremony mode: **R30 standing default** (commit and push at each
checkpoint, unattended). The prompt states no prepare-only scope, so the
default applies; determined from the prompt, not assumed.

## Step 0 -- Preflight

`bin/preflight`, plain. Every finding it printed, disclosed:

- **Last five CI runs on `main`: all five green.** `7544f7c`, `2db2dee`,
  `15f5943`, `043305b`, `fca52ec`. No red, no PENDING.
- **Edit root OK** -- `/home/mg/src/ehr-testing-tools`, not under
  `/mnt/`. No fresh instance of the retired `/mnt/c` hazard class.
- **Tree clean**, untracked files included.
- **HEAD matches `origin/main`** at
  `7544f7c712db4e1540dbe92efa16dbf8cdb238f2`.
- **`DISCLOSED: HEAD is not currently tagged stable-*`** -- expected and
  correct. The last `stable-*` tag is `stable-20260815-result-nodes`;
  the arc tags at its own step 7, and this session was licensed no tag.

Tag verification, as the prompt required: `git rev-parse
stable-20260815-result-nodes^{}` =
**`b139de589083c6b4967c1a4769b2c6a8d17feac4`**, matching the prompt's
stated value exactly. Baseline tip **`7544f7c`**, as stated.

## Step 1 -- D1-6: the range derivation

### The premise that did not survive the tree

The prompt's Step 1.2 instructed: *"record `origin/<branch>`'s SHA
BEFORE the fetch (the script currently fetches first -- order
matters)."* **Probed before any code was written**, in a throwaway repo
with a bare origin:

```
HEAD=ecaf9ca
origin/main after push = ecaf9ca
origin/main@{1} = 9d7dd6d
ecaf9ca refs/remotes/origin/main@{0}: update by push
9d7dd6d refs/remotes/origin/main@{1}: update by push
```

`git push` **itself** fast-forwards `refs/remotes/origin/<branch>`. The
script's own `git fetch` is not what advances that ref -- the push
already did -- so `origin/<branch>` read at *any* point inside a
post-push run is the **post**-push tip. Using it as the base would have
produced an **empty range**: strictly worse than the `tip^1` defect
being fixed.

The pre-push tip is recoverable only from that ref's reflog, and that
is the mechanism **register row D1-6 itself names** as its own
alternative (*"or the reflog's `origin/<branch>@{1}`"*). So the ruled
OUTCOME -- derive from origin's pre-push tip, fail loud when
underivable -- landed exactly as ruled; only the prompt's stated means
of reading it needed correcting, from a source the register supplied.
The before-the-fetch ordering is kept anyway (the derivation runs above
check 1), so a fetch cannot shift the range underneath the check about
to run.

Recorded rather than silently absorbed, per `build-session` step 12 and
`docs/dev/way-of-working.md` §2. This did not fire STOP-AND-REPORT: the
ruled outcome was achievable as ruled, and the correction came from the
register rather than from session judgment.

### Headless harness

The script resolves its own repo root from `${BASH_SOURCE[0]}/..` and
`cd`s there, so running it from a throwaway repo would operate on *this*
repo. The test therefore copies the real script into
`<fixture>/bin/post-push-verify` -- the real bytes, rooted at the
fixture. STOP-AND-REPORT did not fire; the script exercises headlessly.

### Red, with the fix stashed (checkpoint isolation)

`git stash push -- bin/post-push-verify`, leaving the new test in place,
so the red run exercised exactly the unfixed script and nothing else in
flight:

```
FAIL in (default-range-covers-every-pushed-commit-test)
a non-ASCII message anywhere in the pushed range fails the check
expected non-zero exit; the pushed range carries a non-ASCII commit message. Output was:
== bin/post-push-verify (main, range 338410e4..55dc4e10) ==
-- 2. Per-commit ASCII check, 338410e4..55dc4e10 --
OK: every commit message in range is pure ASCII
== bin/post-push-verify complete ==
expected: (not (zero? exit))
  actual: (not (not true))
```

A **one-commit range over a three-commit push**, reported OK, exit 0 --
D1-6 exactly as the register describes it. The fixture's own
precondition assertion (that three commits really were pushed) **passed
in the same run**, so the red is the script's, not the harness's.

### Green

```
Ran 1 tests containing 4 assertions.
0 failures, 0 errors.
```

### No regression on the explicit-range path

`bin/post-push-verify 0027a6e` against the live tree: `range
0027a6e8..7544f7c7`, four commits, `OK: every commit message in range is
pure ASCII`, CI reported once, exit 0. `--help` renders the rewritten
usage text.

## Step 2 -- D2-6: the pipe-discipline law

One law added to `build-session`'s VERIFICATION section in its
siblings' style, with a matching `Done when` checkbox. It names the
mechanism (`cmd | tail -40` reports `tail`'s exit status, which is 0
whatever `cmd` did) and both consequences (masked exit code, truncated
countable signature).

`.claude/` mirror byte-copied; both files sha256
`663e7331daeada812deba2fd02c3ea9e71fadbd32a2b1e9e862bda0a2f5211b5`.

Corroboration found in the tree while working: ADR-0135's own session
record documents this happening to it **twice**, including a run killed
mid-flight whose log ends `Error 143` while the harness reported exit 0.

## Step 3 -- D4-3: gate reports the true category

Fence check first. The prompt located `gate`'s `:file-not-found` at
`core.clj:698-703` and `:984-997`; those are in fact
`mutate-command` and `batch-command`. `gate`'s category is produced
**in the engines** (`judge-v2-hapi`, `judge-v2-nist`,
`judge-fhir-official`, each `(not (.canRead f))` -> `:file-not-found
{:reason :permission-denied}`), which the fence puts out of reach.

The fence resolves cleanly rather than pressing: the fix goes at the
**CLI seam** in `gate-command`, which is where the fence points ("gate's
read path in `bases/cli/src/ehrt/cli/core.clj`, the minimum try/catch
routing"), and which covers all three gate formats at once. One private
guard function plus one `or` in the single-file branch; no refactor of
surrounding code. STOP-AND-REPORT did not fire.

**ADR-0098's ruled engine-level shape is deliberately untouched** (one
category plus a distinguishing `:reason :permission-denied` payload key,
author ruling Q2 "a."), and its three engine tests still assert it.
D4-3's defect was measured at the CLI surface, which is what `show` is a
sibling of.

### Red, with the fix stashed

`git stash push -- bases/cli/src/ehrt/cli/core.clj`:

```
FAIL in (gate-on-an-existing-unreadable-file-reports-path-unreadable-test)
the file exists -- the honest category names the read failure, not absence
expected: (= :path-unreadable (:category r))
  actual: (not (= :path-unreadable :file-not-found))
```

**Precondition held live:** the session ran as **uid 1000**, so `chmod
000` really did remove read access and the root-skip branch never fired.
The skip guard is retained anyway, matching ADR-0098's three engine
tests.

The companion test (a missing path still gets `:file-not-found`) passed
in **both** states, as it should -- it pins the half the fix must not
over-reach into.

### Green

Both deftests pass; no failures reported.

### Docs

`grep` over `docs/**` for `:file-not-found` / `:path-unreadable`:
**zero hits**. Neither category is documented on any doc surface, so
nothing needed regenerating and nothing was touched. `docs/cli.md` is
generated from help text, which this change does not alter --
confirmed by `cli-md-is-current-test` passing in the full run.

## Step 4 -- Gate run, commits, close

### `make test`, unpiped, exit code captured explicitly

Run as `make test > <log> 2>&1; MAKE_EXIT=$?` -- the discipline this
session just wrote into the skill, applied to itself.

```
MAKE_EXIT=0
0-failures-0-errors occurrences: 640
FAIL in / ERROR in lines: 0
total passes: 16382
Test results: blocks: 320
log lines: 2485
```

**Block-count reconciliation, predicted BEFORE the run.** Prediction:
**640**, from `636` plus one new test namespace
(`ehrt.docs-tooling.post-push-verify-range-test`) running in two project
contexts at two matching lines each = `+4`. The two new `gate` deftests
join the **existing** `ehrt.cli.core-test` namespace, so they raise
assertions and not blocks -- so the prompt's "possibly a new CLI test
namespace" branch did not apply.

**Outcome: 640, exactly as predicted.** `Test results:` blocks moved
318 -> 320, consistent at one per context. The derivation is the same
one ADR-0135's record used for its own `+4`.

Passes reconcile too, against the correct baseline. The prompt named
636 blocks / the review-3 baseline's 16,315 passes, but Session B
(`stale-path-gate-widening`) already carried passes to **16,369** while
holding blocks at 636 -- it widened an existing namespace. So
`16,369 + 13 = 16,382`: the range test's 4 assertions in 2 contexts
(8) plus the two CLI deftests' 5 assertions in 1 context (5). No
residue.

`bin/verify-nist-lock` ran as part of `make test` and printed its six
NIST coordinates.

### Commits

Three fix commits, each message via file, each ASCII-verified before
committing, each preceded by `git diff --cached --stat` and `gitleaks
git --staged -v` (no leaks found, all three):

1. `fd0d277` -- `fix: post-push-verify derives the pushed range from
   origin's pre-push tip; fails loud when underivable (review-3 D1-6,
   three live sightings)` (script + test co-landed)
2. `4fbfd37` -- `docs: build-session names explicit exit-code capture
   for gate runs (review-3 D2-6, ADR-0135 incident class)` (skill +
   mirror)
3. `ef6b10c` -- `fix: gate reports :path-unreadable for unreadable
   files, matching show (review-3 D4-3)` (src + test co-landed)

Plus a fourth records commit: ADR-0138, its `notes/ADRs.md` index line,
the three register disposition cells, this record, and the prompt
archive.

Nothing outside the checkpoint in flight was ever staged; no unstaging
was needed.

### Register

Rows **D1-6**, **D2-6**, **D4-3** -> `**FIXED 2026-08-15 (ADR-0138)**`
with an account each. Disposition cells only -- dimension summaries and
verdicts left as the review took them, matching what Sessions A and B
did to this file.

## Post-push receipts

Recorded in the follow-up commit, per the prompt's Step 4: the fix's own
first live use, `bin/post-push-verify` with **no arguments**, over a
multi-commit pushed range, alongside the by-hand full-range check as its
independent witness.

## Deviations and disclosures, collected

1. **The prompt's fetch-ordering premise is wrong** (Step 1 above). Fix
   implements the register's own named alternative; ruled outcome
   unchanged. Recorded in ADR-0138 under its own heading and in the
   D1-6 disposition cell.
2. **The prompt's line citations for `gate`'s `:file-not-found` point
   at `mutate`/`batch`**, not `gate`; the real sites are in the three
   judge engines. Resolved inside the fence by fixing at the CLI seam,
   which is what the fence names.
3. **Two deftests, not one**, for D4-3 -- the second pins that a missing
   path still gets `:file-not-found`, so the fix cannot over-reach.
4. **A `Done when` checkbox accompanies D2-6's one line**, because every
   other law in that skill carries one.
5. **No tag paid.** None licensed; the arc tags at its own step 7.
6. No oracle claim is made or owed: zero corpus `src`, zero vendored
   bytes, zero module JSON.
