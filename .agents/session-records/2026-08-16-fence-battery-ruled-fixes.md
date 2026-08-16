# 2026-08-16 -- the D8-5 battery's ruled fixes: six pages fixed-or-disclosed, intake made diagnosable, D8-5 discharged

Micro-session, autonomous (R30). Baseline `30cc335`; ADR-0140. Author
ruling: **"Accept recommendations."** (2026-08-16), binding the
battery register's per-finding recommendations and the session shape
(the fixes get their own ADR, so D8-5's lapse closes before the
event-log-contract arc starts).

## Step 0 -- preflight, disclosed in full

`bin/preflight` (plain), every line disclosed rather than summarized:

- last five CI runs on `main`: **all green** (`30cc335`, `abb0239` x2,
  `f91ca13`, `b96c246`).
- edit root: `/home/mg/src/ehr-testing-tools`, not under `/mnt/` -- OK.
- tree clean including untracked -- OK.
- local HEAD `30cc3358…` matched `origin/main` -- OK.
- last `stable-*` tag `stable-20260815-review-3-fixes`;
  **DISCLOSED: HEAD not tagged.** No tag owed by this session -- its
  own close tag is deferred to the next session's Step 0 under the
  standing conditional license (the ADR-0133/0135 pattern).

Both standing tags verified peeled, as the prompt required:

```
b139de589083c6b4967c1a4769b2c6a8d17feac4  stable-20260815-result-nodes^{}
b96c246430038b4d38aa60a391de5e376e61cd24  stable-20260815-review-3-fixes^{}
```

`out/` was removed (`rm -rf out`) before any fence re-run and treated
as a precondition throughout -- the battery's own near-miss (a stale
`sim-s1-p1-first-run` dir producing a false RED against the manual's
most emphatic claim) is why. Artifact cache is **outside** `out/`
(`~/.cache/ehr-testing-tools/artifacts`, confirmed via `bin/ehrt
artifact resolve`), so clearing `out/` cost no re-download.

## Step 1 -- four page fixes, `14c9348`

Every re-run below is from the cleared `out/`.

| finding | fix | verification | exit |
|---|---|---|---|
| R-F1 | one parenthetical before `04-time-on-the-wire.md`'s play fence, naming the two generate fences below it and the exerciser | ran both `corpus generate sim` fences, then the play fence | `GEN_BASE_EXIT=0`, `GEN_LATENCY_EXIT=0`, **`PLAY_EXIT=0`** |
| R-F1 (2nd half) | the sentence's alternative, `bin/demo-exerciser-ed-tuesday` | ran it | **`EXERCISER_EXIT=0`** (see the near-miss below) |
| R-F2 | one parenthetical naming the Quickstart prerequisite | `artifact fetch` x2, `corpus generate synthea`, then the check fence | all 0; **`CHECK_EXIT=0`**, output `:totals {:pass 7, :rejected 0, :indeterminate 0, :no-verdict 0}` matching the page's own printed block |
| R-F3 | one provenance note at the head of `polylith-brief.md`; the seven fences untouched | both section references derived against the file's OWN headings by GitHub's slug algorithm | `#phase-0--prerequisites-and-inventory` **RESOLVES**, `#9-command-cheat-sheet` **RESOLVES** |
| R-F5 | `curl -o out/simhospital/messages.out`, `sha256sum` updated, `mkdir -p` added | ran the fence | `CURL_EXIT=0`, `SHA_EXIT=0`, digest `fa9719a5f157…`; `ls messages.out` -> no such file; `git status --porcelain` showed only this session's own edits |

**A false red worth recording.** The first `bin/demo-exerciser-ed-tuesday`
run exited **1**. Every named invariant in it passed; the failure was
its own ADR-0005 tree-clean postcondition, tripped by this session's
four uncommitted doc edits -- the same class the battery itself hit in
Step 2 of its register (`make quickstart` failing on postconditions,
not on fences). Re-run after the Step-1 commit: **exit 0**, "every
command asserted, every named invariant held, tree clean." Recorded
rather than filtered: read without looking at the log, that exit 1
would have been filed as an R-F1 fix failure.

## Step 2 -- two disclosures, `43aec70`

- **R-F4**: lead-in sentence declaring both `formats.md` strips
  illustrative and `<…>` the placeholder marker; `some/corpus` ->
  `<your-corpus-dir>`, `some/report.edn` -> `<your-report>.edn`; `jet`
  named as an optional external tool, not vendored and not on PATH
  here. Verified: `command -v jet` -> not found; no `jet` entry in the
  lockfile.
- **R-F6**: one sentence before `simulate-your-facility.md`'s fence.
  Both of its claims verified live, not assumed:
  `MISSING_CONFIG_EXIT=2` with
  `{:status :error, :category :config-not-found, :payload {:path "stmarys.edn"}}`,
  and the named alternative
  (`--config demos/scenarios/ed-tuesday/config.edn`) `DEMO_CONFIG_EXIT=0`.

## Step 3 -- R-F7, the tool fix, red-first, `07a9566`

Fixed at `ehrt.corpus-io.spool/spool!`, the intake seam -- distinguish
BEFORE parsing (the D4-3 pattern). `framing/decode` and every engine
untouched. No STOP-AND-REPORT was needed: the fix never left the
stdin path, and `mutate`'s stdout contract and the pipe convention are
both unchanged.

**Red, against exactly the unfixed code** (`clojure -M:poly test
brick:corpus-io`, run before any `src` edit -- no stash isolation
needed, since the tests were written and run first):

```
Ran 11 tests containing 44 assertions.
7 failures, 0 errors.
Test results: 37 passes, 7 failures, 0 errors.
```

Green after the fix: `44 passes, 0 failures, 0 errors.`

**Before/after, the use-case page's own fence with `in/v2-corpus`
absent:**

```
BEFORE  PIPELINE_EXIT=1
{:status :rejected, :category :malformed-mllp-frame, :payload {:pos 0, :hint "expected 0x0B start-of-block"}}

AFTER   PIPELINE_EXIT=2
{:status :error, :category :upstream-error, :payload {:origin "stdin", :upstream {:status :error, :category :file-not-found, :payload {:path "in/v2-corpus"}}, :hint "the command feeding this one failed; its own result envelope arrived here instead of corpus bytes"}}
```

`mutate`'s own envelope survives the pipe verbatim, and the pipeline's
exit code now matches the exit code `mutate` actually returned.

**The register understated its own finding.** It predicted
`:malformed-mllp-frame` for empty stdin. Probed live, empty stdin
**succeeded**:

```
BEFORE  EMPTY_STDIN_EXIT=0
{:status :ok, :payload {:catalog [{… :path "capture-manifest.edn" …}], :intake-record {… :file-count 1 …}, :out "out/empty-intake"}}

AFTER   EMPTY_STDIN_EXIT=1
{:status :rejected, :category :empty-input, :payload {:origin "stdin", :framing :mllp, :hint "nothing arrived to spool -- check that the producer on the other side of the pipe actually ran"}}
```

`decode-mllp` on zero bytes returns `ok []`, so intake cataloged a
corpus whose only member was the capture manifest the spool had just
written. Found only because the test was written from the behaviour
rather than from the register's row.

Over-reach fenced: `spool-does-not-mistake-corpus-bytes-for-an-upstream-envelope-test`
(a FHIR Bundle, which also starts with `{`) passes in BOTH runs by
design. Happy path re-verified through the same real pipe with
`test-fixtures/v2`: exit 0, catalog written. `clojure -M:poly check`:
OK.

**Generated docs: a checked no-op, not a skipped step.** `intake`'s
category list is rendered into neither `docs/cli.md` (intake section is
flags only) nor `docs/formats.md` (envelope shape, not a per-command
category vocabulary), so nothing `cli-md-is-current-test` gates was
forced.

## Step 4 -- census, budget, records

**Post-fix census** (`bin/fence-census`, re-run):

| class | before `30cc335` | after |
|---|---|---|
| command / exercised | 18 | 18 |
| command / bare | **58** | **56** |
| output | 29 | 29 |
| other | 97 | 99 |
| total | 202 | **202 (closed)** |
| files | 102 | 102 |

The two movers were **named, not guessed**: the baseline census was
re-run in a disposable `git worktree` at `30cc335` and diffed
per-fence. Exactly two rows changed, both `docs/formats.md`,
`command/bare -> other/-`. Cause: the enumerator's own pre-existing
`NOT_A_COMMAND_RE` ("shapes that look command-ish at the head but are
not runnable text") matches a `<placeholder>` token -- the census
agreeing independently with R-F4's ruled disclosure. No census edit was
made. Worktree removed.

**`:onboarding` re-derived** by the gate's own method (`line-seq` over
`.agents/reading-sets.edn`'s `:onboarding` `:paths`) at the pre-close
tip: **2660 / 2690, 30 lines headroom**. The battery's register claimed
**2657**; three lines unaccounted for. Re-derived rather than repeated,
disclosed rather than reconciled by assumption. Re-derived again AT the
close, after every records edit: **2662 / 2690, 28 lines headroom**.
**No bump forced** -- closing D8-5's fourteen-line backlog row to an
eleven-line CLOSED row paid for most, not all, of this session's own
Done pointer, watch-list edit and two README index lines (net +2).

## Deviations, all disclosed

1. **R-F2's premise was false.** The prompt and the register both said
   Chapter 2 generates the Synthea corpus. It does not -- Chapter 2
   generates `sim-s1-p1` and explicitly defers Synthea to later
   chapters (`02-setup-first-corpus.md:67`); `git grep synthea-s1-p5`
   over `docs/manual/` returns Chapter 8 alone. Fixed forward to the
   register's ruled substance ("one line naming the prerequisite
   command"), pointing at the root `README.md#quickstart` (anchor
   verified) instead of a working link to a false claim.
2. **R-F5 took one line beyond the ruled minimum.** `mkdir -p
   out/simhospital` -- without it, `curl -o` fails from a cleared
   `out/` and the fence would have stayed RED after its own fix, a
   STOP condition. `out/` is gitignored (`.gitignore:8`).
3. **R-F1's sentence names the commands that exist.** The prompt's
   draft said "the two `sim run` invocations below"; they are
   `bin/ehrt corpus generate sim`. Written to match the tree.
4. **`:onboarding` disagrees with the register by 3 lines** (above).

## Fences honoured

R-F8 not implemented (no exerciser added, no `exercised-sources.edn`
row, no census gate armed). The seven `poly` fences not rewritten.
Chapter 4 not reordered. `jet` not vendored. No converter, generator,
or engine changed; zero vendored bytes. No regression-oracle claim is
made or owed.

## Close -- full suite, unpiped, `MAKE_EXIT` captured

```
MAKE_EXIT=0
0-failures-0-errors occurrences: 640
FAIL in / ERROR in lines: 0
total passes: 16408
Test results: blocks: 320
log lines: 2485
```

An earlier attempt at this same run was killed at the harness's 10-minute
cap (exit 143) and is NOT the run above -- recorded because a truncated
run must never be read as a result. The run above completed in the
background, ~25 minutes, matching the battery's own measured baseline.

**Block-count reconciliation, predicted BEFORE the run.** Prediction:
**640, unchanged.** R-F7's three new deftests land in the EXISTING
namespace `ehrt.corpus-io.spool-test`, so they raise assertions and not
blocks -- the prompt's "new namespace, +2 per project context" branch
did not apply. **Outcome: 640, exactly as predicted**; `Test results:`
blocks held at 320.

**Passes reconcile with no residue.** Baseline 16,384 (`30cc335`'s own
recorded figure, not the 16,382 in the prompt's memory -- that was the
figure BEFORE the battery session's own C-4 deftest). The three new
deftests carry 12 assertions (6 + 4 + 2, the first being three `is`
across a two-framing `doseq`), and `corpus-io` sits in two of the
project contexts `:all skip:integration` runs (`conformance`,
`ehrt-cli`; `integration` is skipped). `16,384 + 12 x 2 = 16,408`.
**Observed: 16,408.**

`clojure -M:poly check` OK (it is `make test`'s own first step), and
`bin/verify-nist-lock` ran and printed its six NIST coordinates.

## Push receipts

Pushed `30cc335..79788c4` (five commits). `bin/post-push-verify` (no
arguments), all three checks:

```
-- 1. Remote tip vs HEAD --
OK: origin/main (79788c4807641ebec154d1d8dd503b351552f9e3) matches tip (79788c4807641ebec154d1d8dd503b351552f9e3)

-- 2. Per-commit ASCII check, 30cc3358..79788c48 --
OK: every commit message in range is pure ASCII

-- 3. CI run at tip (79788c4807641ebec154d1d8dd503b351552f9e3) --
CI run for 79788c48…: status=queued conclusion=<pending>
  https://github.com/pragsmike/ehr-testing-tools/actions/runs/31961270263
DISCLOSED: reported once, not awaited to conclusion (AR-CI-4)
```

`gitleaks detect` ran as the pre-push hook over 942 commits: no leaks.

The commits, in order:

| sha | what |
|---|---|
| `14c9348` | R-F1/R-F2/R-F3/R-F5 -- four page fixes |
| `43aec70` | R-F4/R-F6 -- two disclosures |
| `07a9566` | R-F7 -- the intake diagnosability fix, red-first |
| `c177000` | ADR-0140, register close note, roadmap, rulings, record, prompt archive |
| `79788c4` | full-suite reconciliation |

**No tag owed or taken** by this session; its own close tag is deferred
to the next session's Step 0 under the standing conditional license.
