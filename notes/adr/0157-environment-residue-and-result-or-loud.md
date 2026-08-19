## ADR-0157 — review-4 fix 3/5: environment residue at its root, and result-or-loud widened to the class it names

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-19.

### Context

Fix 3 of 5 in the repo-review-4 arc, the pair **B + D**, under the
author's standing ruling of 2026-08-18 — *"Q1 accept all
recommendations. Q2 that order ok. Q3 pair small ones."*

Folded in from ADR-0156's second addendum:
`roadmap.md#commit-msg-ascii-hook`, PRIORITY 7. That row is `.githooks/`
work on the same ceremony surface Session B touches, ADR-0156's own
addendum names the `commit-msg` hook as its remedy, and "pair small
ones" is the standing instruction. Judged inside B's fence and carried
here rather than skipped; the session's prompt asked for a
STOP-AND-REPORT if it were judged outside, and it was not.

The two halves are the same defect at two altitudes.

- **B** — the executable-bit class bit three times in one review window
  (ADR-0147 S-7, ADR-0149's CI red at `76b4e20`, ADR-0154's Step-0
  disclosure) and was gated at the SYMPTOM every time.
  `executable_bits_test` reads the git INDEX precisely because
  `core.fileMode=false` makes the worktree bit lie. Nobody read the
  cause, which is retired `/mnt/c`-era git config surviving in an ext4
  clone.
- **D** — `R-io-result-or-loud`'s lint forbids
  `.listFiles`/`.list`/`.renameTo` and misses `.mkdirs`/`.delete`: a
  gate whose population is narrower than the rule it enforces. Third
  instance of that shape in review 4.

And both are gates about *failures that do not announce themselves*. A
`.mkdirs` returning false and a `core.fileMode=false` are the same kind
of fact: true, recorded, and never read.

### Step 0 — the measurements everything below rests on

`bin/preflight` in the edit root: **exit 0, no findings.** Five green CI
runs on main (`c80b558`, `04b6f66`, `841fb75`, `1e20c63`, `660b7bf`),
tree clean, HEAD == `origin/main` at `c80b558`, last tag
`stable-20260819-review-4-fix-2-oracle-and-guards` @`841fb75`, HEAD not
tagged — disclosed, no tag owed.

Baseline `make test` in a fresh clone at `c80b558`, unpiped, `MAKE_EXIT`
captured, wrapper ending `exit "$MAKE_EXIT"`: **MAKE_EXIT=0, 358 blocks,
4,012 tests, 18,008 assertions, 0 failures, 0 errors.** Reconciles
exactly with ADR-0156's close figures — nothing moved in between.
`clojure -M:poly check` in that clone: **OK**.

**(a) The config, both clones.**

| setting | edit root | fresh clone |
|---|---|---|
| `core.fileMode` | `false` | `true` |
| `core.ignorecase` | `true` | *unset* |
| `core.hooksPath` | `.githooks` | *unset* |

**(b) The class is quiet today.** `git ls-files | tr A-Z a-z | sort |
uniq -d` → empty, so no tracked paths collide case-insensitively.
`git ls-files -s bin/ .githooks/` → **32 files, all `100755`.** Also
measured, because the commit-msg hook needed it: **zero tracked paths
contain a non-ASCII byte**, which is what makes it safe for the hook to
scan a message file whole rather than trying to guess which lines git
will strip.

**(c) The `.mkdirs`/`.delete` census, and the grep-shape reconciliation
the prompt asked for.** The plan recorded 13 `.mkdirs` at `0a07195`;
the prompt's own re-derivation found 10 in `components/*/src`. Both
numbers are right and neither site was removed: **10 in
`components/*/src` plus 3 in `bases/*/src` is 13.** It is a
grep-shape difference, not a delta. `.delete` is 2, both in
`kernel/artifact.clj`. Full table under D below.

**Residue census, so the fence is drawn from the tree and not from
imagination:** `(.createNewFile`, `(.setExecutable`, `(.mkdir` (without
the `s`), `(.setReadable`, `(.setWritable`, `(.deleteOnExit`,
`(.setLastModified` in production `src` → **zero occurrences of each.**
The plan's fence at two patterns is the whole live population; there is
no third pattern to row.

**(d) Hook installation.** `git config core.hooksPath .githooks` — a
per-clone opt-in pointing git at the *tracked* directory, documented in
`AGENTS.md` and checked by `ehrt doctor`'s own `check-hooks-path`.
**Nothing anywhere enumerates the hooks by name**, so a third file in
`.githooks/` is installed by the same act that installed the first two.
That is a claim, so it is tested rather than asserted (below).

**(e) The hook's red witness.** `git log --format=%B c80b558~40..c80b558
| LC_ALL=C grep -nP '[^\x00-\x7F]'` over the last 40 commits →
**exactly one**, `04b6f66`, the U+2026 ADR-0156's own addendum
disclosed. Predicted exactly, found exactly.

### Decision — B

**`bin/preflight` check 2 gains the two residue checks**, beside the
`/mnt/*` check and in the same rendering shape. The path check catches a
checkout in the wrong PLACE; these catch a checkout in the right place
still CONFIGURED for the wrong one. Each `FINDING:` prints its own
one-line remedy.

Two deliberate choices. Only `false` is a finding for `core.fileMode` —
*unset* is correct, because git detects mode support from the filesystem
itself, and on a filesystem that genuinely has no executable bit `false`
is the right answer (and that case is already a `FAIL:` above, where
`/mnt/*` is refused outright). And the EFFECTIVE value is read, not just
`--local`, because a global setting bites identically; the printed
remedy is `--local` either way, since a local setting wins.

`.git/config` is not tracked, so no repo test can assert this for
someone else's clone. That is exactly why it belongs in the script every
session runs at Step 0 rather than in a test.

**`.githooks/commit-msg`** refuses a non-ASCII commit message before the
commit exists. **`executable_bits_test` stays**, as the plan directed:
it protects clones this config change cannot reach.

**The scan is extracted, not copied.** `bin/ascii-scan` is the one
implementation and both `.githooks/commit-msg` and `bin/post-push-verify`
check 2 invoke it. The prompt offered extraction *iff* both callers can
use it without a second copy; a shared *executable* rather than a
sourced library is what makes that true here, because the hook is
`/bin/sh` and post-push-verify is `bash`, and an invoked script is
indifferent to both. Check 2's own output is unchanged — `--bytes` is
the hook's flag, not its. A `bin/lib/` sourced function would have
coupled two shell dialects for one `grep`.

Both callers are **fail-closed on a scanner they cannot run**: an
unmeasurable check is not a passing one (ADR-0155's law, applied to its
own descendants). Without that, the failure mode of a missing
`bin/ascii-scan` is a silent green — which is the shape this entire arc
keeps finding.

### STOP-AND-REPORT — the edit-root config flip did not happen

The plan's D3-1 says the flip is **"verified safe"** with **"zero
churn"**. It is not, and the way it is wrong is this review's own
signature defect turned on the review itself.

D3-1 measured a **fresh clone** at this tip — `git status --porcelain
-uno` empty with `core.fileMode=true` — and concluded about the **edit
root**. Those are different populations. Run in the edit root, with
before/after snapshots kept:

```
git config --local core.fileMode true
git config --local --unset core.ignorecase
-> 360 tracked files appear modified, every one of them
   `mode change 100644 => 100755`, and zero content change.
```

They are ordinary text files — `.md`, `.edn`, `LICENSE`,
`.editorconfig`, brick `deps.edn` — carrying the executable bit in the
**worktree** while the index correctly says `100644`.
`core.fileMode=false` has been hiding all 360 for the life of this
clone.

The index is sound and the two clones agree on it exactly: **1382 files
at `100644`, 45 at `100755`** in both. A fresh clone's worktree is clean
and writes `0644`. So the residue is strictly larger than the two config
lines the plan priced: it is **two config lines plus 360 worktree bits**,
and flipping the config without normalizing them converts a hidden
inconsistency into 360 staged mode changes on the author's next
`git add -A`.

**The edit root was restored to exactly as found** —
`core.fileMode=false`, `core.ignorecase=true` — and `git status
--porcelain=v1 --untracked-files=all` re-verified byte-identical to the
pre-flip snapshot. Nothing outside the repo was left changed.

**The remedy is AUTHOR ACTION** because it is materially larger than the
act the plan authorized: flip the two settings *and* `chmod -x` the 360
files whose index mode is `100644`. Rowed as
`roadmap.md#edit-root-mode-residue`, not executed.

**Consequence, disclosed: `bin/preflight` now exits 1 in this edit
root**, with two `FINDING:` lines, until that remedy runs. That is a
true report of a real condition and it names its own fix — but every
session's Step 0 will meet it, and a check that is permanently red gets
read as noise. It should be paid, not lived with. This is precisely the
failure mode the session's own control test
(`preflight-reports-a-clean-environment-as-clean-test`) exists to
prevent in the general case; here the redness is the machine's, not the
script's.

**Surfaced in passing, not fixed:** three files
(`.agents/skills/shared-skill-layout/agents/openai.yaml` and its two
mirrors) carry CRLF in the working copy. Same era, same class, folded
into the same row.

**A second thing surfaced by Step 0(d)**, unrelated to the residue and
rowed separately as `roadmap.md#setup-md-hook-citations`: the prompt
asked for the new hook to be documented "wherever hooks are
documented", and finding that place turned up **three live citations
pointing at `SETUP.md` for hook and gitleaks instructions it does not
contain** — `.githooks/pre-push:14` and `:39`, and `cli/core.clj:360`'s
`check-hooks-path` docstring. Hooks are documented in `AGENTS.md` and
`AUTHORS-GUIDE.md` §1, which is where this session's line went. Errata,
not behavior, and out of this session's fence.

### Decision — D

`ehrt.kernel.io` gains three functions, re-exported through
`ehrt.kernel.interface`:

| fn | contract |
|---|---|
| `mkdirs!` | **Loud.** Afterwards the path is a directory, or `ex-info :mkdirs-failed` naming it. |
| `delete!` | **Loud.** Afterwards the path does not exist, so deleting a missing file is a SUCCESS; a non-empty directory throws `:delete-failed`. |
| `delete-quietly!` | **The declared exception.** Never throws; returns whether the path is gone. |

Both booleans are ambiguous in the same way — `false` means either
"already so" or "refused" — and that ambiguity is exactly why every one
of the 15 sites discarded them. So the contract is stated on the
**postcondition**, never on the return.

**Loud rather than result-returning is a deliberate departure** from
`list-files`/`rename!` beside them. Those sit in code that already
threads results; the `.mkdirs` sites do not — they are single statements
inside a `let` or a `->`. Handing each a result to check would have been
13 new branches written by a session whose fence was routing. A throw
keeps each site one expression and still makes the failure impossible to
ignore, which is what "result OR loud" names.

**The site table.** Every site was read before it was routed, and **not
one of the 13 `.mkdirs` sites checked the boolean by hand** — every one
was a bare statement or an `_` binding. So no check was doubled and none
was removed.

| # | site (at `c80b558`) | brick | shape | routed to |
|---|---|---|---|---|
| 1-2 | `judge_fhir_official/fhir.clj:193,252` | judge-fhir-official | `_ (.mkdirs out)` | `kernel/mkdirs!` |
| 3 | `corpus/intake.clj:376` | corpus | bare statement | `kernel/mkdirs!` |
| 4 | `corpus/generate.clj:282` | corpus | `_` binding | `kernel/mkdirs!` |
| 5 | `corpus/generators.clj:162` | corpus | bare statement | `kernel/mkdirs!` |
| 6 | `corpus_io/spool.clj:203` | corpus-io | bare statement | `kernel/mkdirs!` |
| 7 | `docs_tooling/usecases.clj:299` | docs-tooling | bare statement | `kernel/mkdirs!` |
| 8 | `sim_trajectory/census.clj:527` | sim-trajectory | bare statement | `kernel/mkdirs!` (+ alias) |
| 9 | `oracle/digest.clj:673` | oracle | bare statement | `kernel/mkdirs!` (+ require; declared oracle change) |
| 10 | `kernel/artifact.clj:191` | kernel | bare statement | `kernel-io/mkdirs!` |
| 11-13 | `cli/core.clj:725,726,1030` | bases/cli | bare statements | `kernel-io/mkdirs!` (+ alias) |
| 14 | `kernel/artifact.clj:138` | kernel | hash-mismatch arm | `kernel-io/delete-quietly!` + comment |
| 15 | `kernel/artifact.clj:141` | kernel | inside `catch` | `kernel-io/delete-quietly!` + comment |

**Both `.delete` sites needed quiet, which is what earned the variant.**
The session's threshold was two sites; the count is exactly two. Site 14
returns `result/rejected :hash-mismatch` carrying the expected and actual
digests — a throw from the cleanup would replace that diagnosis with a
filesystem complaint the caller cannot act on. Site 15 is inside a
`catch`, one degree sharper: a throw there would MASK the download
exception entirely, and its message is the only account anyone gets of
why the download failed. A failed cleanup is survivable at both: `tmp`
is named `<sha>.tmp-<nanos>`, which `cached-and-verified?` never
matches, so it litters the cache rather than poisoning it, and the
docstring's "nothing is left in the cache" holds for the cache ENTRY.
Each site carries a comment naming why it is quiet — **declared, not
inferred from a bare `.delete`**.

**Two alias notes.** `sim_trajectory/census.clj` and `bases/cli/core.clj`
alias `ehrt.kernel.interface` only under result-vocabulary role names
(`result`, and `result`/`artifact`/`locator`), which read wrong on
`mkdirs!`; each gains one more role alias for the io vocabulary, which is
`bases/cli/core.clj`'s own documented convention. `oracle/digest.clj`
had no kernel require at all and gains one — brick deps here are wired at
the **project** level (ADR-0011's flat convention), so no `deps.edn`
moved and `clojure -M:poly check` is **OK**.

**The lint** now forbids five patterns. `\bdelete\b` does not match
inside `.deleteOnExit` for the same reason `\blist\b` does not match
inside `.listFiles`. Only `kernel/io.clj` retains bare calls, which is
the allowlisted namespace whose whole job is making them.

**And the lint gains the population assertion it never had.**
`R-empty-population-is-red` already bound tests when this lint was
written, and its `doseq` over a scan meant a scan that found nothing was
a pass that proved nothing, silently, forever.

### The oracle

**Predicted:** `bin/regression-oracle c80b558 HEAD` aborts on an
undeclared digest-source diff, because `digest.clj`'s `-main` is inside
the soundness body; re-run declared, `IDENTICAL`.

**Observed, both halves exactly:**

```
== soundness check: digest.clj, whole file minus its leading docstring ==
DIFFERS outside the leading docstring -- STOP: pass --declared-digest-change
```
exit 1, showing both hunks — the `:require` line **and** the `-main`
line. That the `:require` is visible at all is ADR-0156's own L1-4 fix
working: before it, the soundness body began at the first `^(defn` and a
require change was invisible to the check that exists to catch exactly
that class.

```
bin/regression-oracle c80b558 HEAD --declared-digest-change
--- declared-digest-change: yes (soundness: no outside the leading docstring) ---
IDENTICAL: every root's digest matches between c80b558 and HEAD
```
exit 0, all 35 roots. The sites are filesystem plumbing, not digest
inputs, and the digests say so.

### Read-back against the fence

**Files.** 33, and four sit outside the prompt's own list, each forced
rather than chosen:

- `components/kernel/src/ehrt/kernel/interface.clj` — the three helpers
  have to be re-exported or no other brick can reach them; cross-brick
  access here goes through the interface, never the implementation
  namespace.
- `components/docs-tooling/test/.../post_push_verify_range_test.clj` —
  its fixture copies the real script into a scratch `bin/`, and
  post-push-verify is now fail-closed on a scanner that is not there.
  Caught by the suite, not by inspection. `exit_truthfulness_test`'s own
  fixture needed the same.
- `AGENTS.md` and `AUTHORS-GUIDE.md` — the prompt asked for a "SETUP.md
  (or wherever hooks are documented) one line". `SETUP.md` has no hook
  content at all; these two are where hooks are documented. See the
  citation row above.
- `bin/ascii-scan` — new, and the extraction the fence explicitly
  permits.

**Sites.** Step 0(c) counted 13 `.mkdirs` + 2 `.delete` = 15. Routed:
**15**. Bare calls remaining: **3, all in `kernel/io.clj`**, the
allowlisted namespace.

**Preflight before/after.** In the residue fixture it prints two
`FINDING:` lines and exits non-zero; with `core.fileMode=true` and
`core.ignorecase` unset it prints `OK:` for both and exits **0** — both
asserted behaviorally, in a fixture whose other four checks are
satisfied so the exit code means what it says.

**Edit-root `git status` after the config change.** The flip was made,
measured, and reverted; `git status --porcelain=v1 --untracked-files=all`
after the revert is **byte-identical** to the pre-flip snapshot.

### Consequences

The three-hit class now has a check at its cause rather than only at its
symptom — and running that check immediately found the cause is bigger
than the plan believed. That is the check earning its place on its first
execution, and it is why the flip is a row rather than a fait accompli.

The ASCII law is gated where a violation is still fixable. ADR-0156's
addendum could only record its own defect because `R-amend-unpushed-
message-only` had put it out of reach within the hour; the same message
today never becomes a commit. And because the scan is one script with
two callers, the pre-commit twin and the post-push check cannot drift
into disagreeing about what ASCII means.

`R-io-result-or-loud`'s lint now covers the class its own rule names.
Fifteen sites that could turn an I/O failure into a confusing downstream
`FileNotFoundException` now name the directory or file that actually
failed.

Three times in this arc a gate has turned out to be narrower than the
rule it is read as enforcing, and twice a claim has turned out to be
wider than what it measures. This session found one more of each — the
lint's population, and D3-1's own "verified safe" — which suggests the
pattern is not yet exhausted and the remaining fix sessions should
expect it.

### Receipts

- **Baseline** (fresh clone at `c80b558`): `MAKE_EXIT=0`, **358 blocks /
  4,012 tests / 18,008 assertions**, 0 failures, 0 errors. `poly check`
  OK.
- **Close** (this tree): `MAKE_EXIT=0`, **358 blocks / 4,040 tests /
  18,110 assertions**, 0 failures, 0 errors. Delta **+28 tests / +102
  assertions** — 14 new tests (7 in `exit_truthfulness_test`, 7 in
  `kernel/io_test`), each counted in the two projects that run it. Block
  count unchanged: no namespace was added or removed.
- **`clojure -M:poly check`: OK**, including the new `oracle -> kernel`
  require edge.
- **Oracle:** abort undeclared (exit 1), `IDENTICAL` declared (exit 0),
  all 35 roots.
- **`bin/post-push-verify c80b558 HEAD`:** all three checks green, check
  2 running through the extracted `bin/ascii-scan`. **Check 2 now has a
  pre-commit twin and both are green:** `.githooks/commit-msg` gated
  every commit in that range as it was written, and check 2 confirmed
  the range afterwards.
- **CI at `18076c8`** (the B+D tip): run **32280873910**, conclusion
  **`success`**, verified by this session via `gh run view`
  (`rulings.md#R-session-verifies-ci-via-gh`).
- **Reading sets**, all under budget, none compacted: `:corpus`
  1821/2045, `:docs` 728/785, `:judge` 915/1000, `:onboarding`
  1438/1530, `:sim` 1267/1405.
