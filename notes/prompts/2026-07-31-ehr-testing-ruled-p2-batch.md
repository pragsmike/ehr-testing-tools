# 2026-07-31 — ehr-testing-tools: ruled P2 batch (judge parity, NIST channel, verdict-cache note)

Repo: `github.com/pragsmike/ehr-testing-tools`, WSL ext4 clone
(`~/src/ehr-testing-tools`). HEAD at session start: `879c299` — already
equal to `origin/main` (no fast-forward needed; `git fetch` + `git log
--oneline origin/main -3` confirmed identical logs before any edit).
No commit or push run by this session; the tree is left uncommitted,
coherent, with each step's proposed commit message printed below, per
the prompt's own instruction. `/mnt/c` clone not touched.

## Original prompt (verbatim)

Context
You are executing three tasks from the 2026-07-30 review that were awaiting author rulings, now ruled (2026-07-31): P2-2 judge-family parity, P2-3 NIST artifact channel, P2-4 verdict-cache placement. The rulings are stated in Author rulings below — they are decisions, not suggestions; do not re-litigate or "improve" them. P2-5 (intake staging-dir) is explicitly deferred — do not touch it.
Work in the WSL ext4 clone; fast-forward to `origin/main` (at or past `879c299`) before any edit and record the HEAD sha. Do not commit or push; leave the tree coherent per step, print each step's proposed commit message, run the per-push lane (`clojure -M:poly test :all skip:integration color-mode:none`, exit code checked directly, never through a pipe) between steps. Do not touch the `/mnt/c` clone.

Read first
1. `notes/2026-07-30-refactoring-review.md` — findings 6, 7, 8 and tasks P2-2/P2-3/P2-4.
2. `components/judge-v2-hapi/src/ehrt/judge_v2_hapi/v2.clj` and `components/judge-v2-nist/src/ehrt/judge_v2_nist/v2.clj` — the asymmetries at the cited lines (nist `gate-file` slurp ~:218-223, nist `gate-dir` ~:225-232, hapi `gate-dir` ~:205-215, hapi error value ~:196-203).
3. `components/judge/src/ehrt/judge/finding.clj` — `Finding`, `Verdict`, `valid-cause-pairing?`; and `ehrt.kernel.result` — the `:ok/:rejected/:error` envelope.
4. `bases/cli/src/ehrt/cli/core.clj` ~:891-930 — the compensating adapter that simplifies once the component behaves.
5. `docs/dev/engine-onboarding.md` checklist item 4, `artifacts.lock.edn`, and the doctor implementation in `bases/cli` — for Step 2.
6. `notes/ADRs.md` ADR-0010/0011/0012 — for the notes Steps 1–3 append.

Author rulings (2026-07-31)
* AR-1 (P2-2): recursive directory walk is the rule for all engines. nist's `file-seq` behavior is the standard; hapi's `gate-dir` changes from flat `.listFiles` to recursive. This is a deliberate behavior change — pin it in the contract test and record it in the ADR note. Both engines return the kernel envelope; nist's `gate-file` returns `kernel/error :file-not-found` instead of throwing.
* AR-2 (P2-3): option (a). The six NIST rows stay in `artifacts.lock.edn` as provenance/license records (the `:use-permitted--unstated--confirmation-pending` posture lives there); they gain a marker that doctor's cache check respects, since the engine is consumed via `deps.edn` (engine-onboarding target 2). Suggested marker: `:resolved-via :deps-edn` on those rows (default `:artifact-cache` implied when absent) — if the lockfile schema suggests a better-fitting key, use it and record the choice in the deviation record.
* AR-3 (P2-4): verdict-cache stays in `judge` for now, revisit later. Docs-only: supersede ADR-0011's placement justification (which cited the NIST-consumer expectation that did not materialize — finding 7) with a dated note: kept by author ruling 2026-07-31; revisit triggers are (i) a second consumer appearing or (ii) the tools split's stage 3. No code moves.
* AR-4 Scope fence. Untouched: intake staging (P2-5 deferred), everything in P3-1..P3-6, all component boundaries (the split is a separate session), and `artifacts.lock.edn` rows other than adding the AR-2 marker.
* AR-5 Test-first (red→green evidence per change), fix-forward, no history rewrites.

Steps 1–3 and Close-out as delivered in chat — reproduced in condensed form below rather than duplicating the full chat transcript.

## What landed, per step

### Step 1 — Judge-family parity (P2-2, per AR-1)

Files: `components/judge-v2-nist/src/ehrt/judge_v2_nist/v2.clj`,
`components/judge-v2-hapi/src/ehrt/judge_v2_hapi/v2.clj`,
`bases/cli/src/ehrt/cli/core.clj`,
`components/judge-v2-nist/test/ehrt/judge_v2_nist/v2_engine_test.clj`,
`bases/cli/test/ehrt/cli/core_test.clj`, `notes/ADRs.md` (ADR-0012
ruling note), new
`projects/conformance/test/ehrt/tools/judge_engine_parity_test.clj` +
new fixture tree `projects/conformance/test-fixtures/gate-dir-nested/`
(`top.hl7`, `subdir/nested.hl7`).

- `judge-v2-nist/gate-file` now returns `kernel/ok {:verdict :findings
  :path [:cause]}` or `kernel/error :file-not-found {:path ...}` —
  never throws a raw `FileNotFoundException` across the component
  interface.
- `judge-v2-nist/gate-dir` now returns `kernel/ok {:results [...]}` —
  the same envelope shape `judge-v2-hapi/gate-dir` already produced.
- `judge-v2-hapi/gate-dir`'s `hl7-files-in` now walks `file-seq`
  (recursive) instead of `.listFiles` (flat) — matching nist's
  pre-existing behavior, per AR-1's ruling that recursive is the
  shared rule.
- `bases/cli`'s `v2-nist-gate-file*`/`v2-nist-gate-dir*` dropped their
  own `.isFile` pre-check and hand-rolled fail-fast directory
  composition; they now delegate straight to the engine, only catching
  the engine's own `:ambiguous-msg-id` ex-info.
- New cross-engine contract test (`judge_engine_parity_test.clj`)
  asserts, per engine (hapi, nist): missing-file → kernel error;
  gate-dir walks recursively (nested fixture); findings validate
  against `ehrt.judge.finding/Finding`; verdict/cause pairing is
  `valid-cause-pairing?`-valid.
- `judge-v2-nist`'s own `v2_engine_test.clj` (real-engine test) now
  unwraps the new envelope and additionally asserts `finding/valid?`
  and `valid-cause-pairing?` on its real 473-finding fixture result —
  giving `judge-v2-nist` the test-tier dependency on `judge` finding
  6c named as missing, mirroring `judge-v2-hapi/v2_test.clj:61`.
- New CLI test
  `v2-nist-gate-command-missing-file-is-a-named-error-not-a-crash-test`
  pins the CLI's missing-file behavior.
- Docs sweep: grepped `docs/` and component docstrings for
  flat-walk/throws-on-missing claims — none found beyond the source
  files themselves (the review's own findings text and the historical
  ADR-0012 landing entry are dated records, not current-state prose,
  so left as-is per fix-forward-for-current-claims-only).

**Red → green (AR-5).** Two genuine red runs, both captured before any
source fix landed:
1. Full per-push lane (`poly test :all skip:integration`) against the
   stashed-out source fix: `ehrt.judge-v2-nist.v2-engine-test` failed
   at `kernel/ok?`/`finding/valid?` assertions (log: `/tmp/step1-red.log`,
   exit 1) — confirmed the poly test runner is fail-fast per namespace
   within a project, so later namespaces (including the new contract
   test) never ran in that pass.
2. To reach the new contract test at the correct cwd (repo root — an
   isolated `clojure -X:test` run from inside `projects/conformance`
   was tried first and rejected: cwd there resolves fixture paths
   wrongly, a false signal, not real evidence), the nist engine-test
   update was also temporarily stashed alongside the source fix and
   the full lane re-run: `nist-missing-file-returns-kernel-error-test`
   ERRORed (real `FileNotFoundException`, uncaught); both
   `*-gate-dir-walks-recursively-test`s and
   `nist-findings-validate-against-shared-schema-test` FAILed (log:
   `/tmp/step1-red2.log`, exit 1). `hapi-missing-file-...` and
   `hapi-findings-validate-...` passed already, as expected (hapi was
   already compliant on those two axes pre-fix).
Both temporary stashes were popped (restoring all Step 1 edits) and
the full lane re-run: **green**, exit 0, 191 test namespaces, 0
failures/errors (log: `/tmp/step1-green.log`), including the new
`ehrt.tools.judge-engine-parity-test` namespace and the CLI pinning
test.

**CLI missing-file behavior: confirmed byte-identical.** Before this
session, a missing path to `ehrt gate v2-nist` produced
`result/error :file-not-found {:path path}` via the adapter's own
`.isFile` pre-check (exit 2). After: the same envelope now comes
directly from the engine's own `gate-file`, same category, same
`:path` value, same exit code — pinned by the new CLI test, which
passed in the green run above. No diff in observable CLI behavior.

Proposed commit message:
`fix: judge-family parity -- nist gate-file returns error value, both gate-dirs kernel-enveloped and recursive (ruled 2026-07-31); cross-engine contract test pins the shared shape`

### Step 2 — NIST artifact channel (P2-3, per AR-2)

Files: `artifacts.lock.edn` (six NIST rows + header comment),
`components/kernel/src/ehrt/kernel/artifact.clj` (`Artifact` schema),
`bases/cli/src/ehrt/cli/core.clj` (`check-artifact-cache`),
`bases/cli/test/ehrt/cli/core_test.clj` (new doctor test),
`docs/dev/engine-onboarding.md` (checklist item 4 note), `notes/ADRs.md`
(ADR-0012, second ruling subsection).

**Marker key chosen: `:resolved-via :deps-edn`**, exactly AR-2's
suggested name — no better-fitting existing key was found in the
lockfile schema, so no deviation from the suggestion. Added as an
`{:optional true}` Malli key on `ehrt.kernel.artifact/Artifact`
(new `ResolvedVia` enum `[:enum :artifact-cache :deps-edn]`, default
`:artifact-cache` implied by absence, matching AR-2's own phrasing).
The six NIST rows in `artifacts.lock.edn` each gained
`:resolved-via :deps-edn`; the header comment above them now narrates
the ruling instead of the pre-ruling "resolves via :mvn/repos" framing
alone.

`check-artifact-cache` (doctor's cache check) now partitions lockfile
artifacts into `:resolved-via :deps-edn` rows (skipped) and everything
else (cache-checked as before); the `:detail` line on a passing check
now reads e.g. `"3 artifact(s) cached; 6 resolved via deps.edn (not
cache-checked): nist-hl7-v2-parser, ..."` — distinguishing the two
resolution stories explicitly, per the ruling's own requirement.

**Red → green (AR-5).** `check-artifact-cache` was temporarily reverted
in place (Edit, not stash — the file also carries Step 1's already-
landed change, which needed to stay); full lane run:
`doctor-command-deps-edn-resolved-artifact-skips-the-cache-check-test`
FAILed (three assertions, `:pass` expected got `:fail`/wrong detail
string) — nothing else failed (log: `/tmp/step2-red-full.log`, exit
1). Fix restored; full lane re-run: green, exit 0 (log:
`/tmp/step2-green.log`).

**Real-tree sanity check (beyond the unit test).** Ran `bin/ehrt
doctor` against the real, now-edited `artifacts.lock.edn` on this
machine (whose artifact cache does **not** have the six NIST jars
fetched into it, only resolved via `~/.m2`): **exit 0** — the exact
defect the review's finding 8 named (doctor exiting 1 while the
engine itself works) is closed. `:detail` read `"3 artifact(s)
cached; 6 resolved via deps.edn (not cache-checked): nist-hl7-v2-parser,
nist-hl7-v2-profile, nist-hl7-v2-validation, nist-xml-util,
nist-hl7-v2-schemas, nist-validation-report"`.

Proposed commit message:
`fix: doctor respects resolved-via marker -- NIST lockfile rows are provenance, deps.edn is the channel (ruled 2026-07-31); doctor test pins exit 0 on maven-resolved tree`

### Step 3 — Verdict-cache placement note (P2-4, per AR-3)

Files: `notes/ADRs.md` only (ADR-0011, the existing "verdict-cache
placement — disclosed, not silently resolved" paragraph gains a
"Superseded 2026-07-31" paragraph immediately after it, naming both
revisit triggers verbatim from AR-3). No code touched;
`components/judge/src/ehrt/judge/verdict_cache.clj` and its tests are
byte-for-byte unchanged.

**No test impact, confirmed by the lane, not just asserted.** Full
per-push lane re-run after this docs-only edit: green, exit 0 (log:
`/tmp/step3-green.log`) — same result as Step 2's green run, as
expected for a documentation-only change.

Proposed commit message:
`docs: verdict-cache stays in judge by ruling 2026-07-31 -- ADR-0011 justification superseded; revisit on second consumer or split stage 3`

## Close-out

Per-push lane green after all three steps (last confirmed in Step 3's
own run). Integration lane (`poly test :all project:integration`) run
once after all three steps: **green**, exit 0, 80 test namespaces, 0
failures/errors (log: `/tmp/integration.log`) — unaffected by Step 1's
conformance-project-only test additions, as anticipated by the prompt
(nothing to report/fix).

## Deviation record (2026-07-31)

**Isolated `clojure -X:test` runs needed correction, not just
caution.** Two ad hoc single-namespace runs were attempted to get
tighter red evidence than the full lane's fail-fast-per-namespace
behavior allows: (1) `cd projects/conformance && clojure -X:test :nses
'[ehrt.tools.judge-engine-parity-test]'` — ran, but from the wrong cwd
for this repo's own path convention (every conformance test's fixture
paths are repo-root-relative, confirmed by reading sibling test files
first), producing misleading `ERROR`s from a `make-validator` call
that couldn't find `PROFILE.xml` at all, not from the behavior under
test. Discarded as a false signal rather than reported as red
evidence. (2) `cd projects/ehrt-cli && clojure -X:test :nses [...]` —
failed outright (`No function found on command line or in :exec-fn`):
neither `projects/ehrt-cli/deps.edn` nor `bases/cli/deps.edn` declares
an `:exec-fn` on their `:test` alias (unlike `projects/conformance`'s,
which does) — poly's own test runner invokes cognitect's runner some
other way for those projects. Resolved by using the full
`clojure -M:poly test :all skip:integration` lane for every genuine
red/green pair in this session instead, and by temporarily stashing
(or, once one already-landed step's fix lived in the same file,
Editing-then-restoring) exactly the source-side change under test each
time — slower per iteration than a targeted namespace run would have
been, but every red/green claim above is backed by an actual command
run, not inferred.

**Two temporary `git stash` operations, both cleanly popped, one
pre-existing unrelated stash left untouched.** Step 1's red evidence
needed the source fix reverted while keeping the new/updated tests in
place; `git stash push -- <3 source files>` did this. To reach the new
contract test at the correct cwd, a second, nested stash
(`git stash push -- v2_engine_test.clj`) was added on top, then both
were popped in order (`stash@{0}` twice) once evidence was captured.
`git stash list` showed a **pre-existing, unrelated stash**
(`WIP on main: d3edbd8 ...`, not created this session) throughout —
left exactly as found; only the two stashes this session created were
touched.

**`v2-nist`'s `gate-dir` fail-fast composition was dropped, not
preserved — a deliberate reading of "the adapter simplifies," not an
oversight.** The old `v2-nist-gate-dir*` composed per-file gates with
`reduce`/`reduced`, stopping at the first non-ok result; the new one
delegates straight to the engine's own (non-fail-fast, like
`judge-v2-hapi`'s) `gate-dir`. This can only change observable
behavior if a directory gate hits an operational error mid-walk, which
cannot happen for `judge-v2-nist` (missing files never enter a
`file-seq` walk of an existing directory) except via the
`:ambiguous-msg-id` throw, which both the old and new code catch
identically at the whole-`gate-dir` level. No test gap was found for
this; flagged here since the prompt asked specifically about
missing-file behavior and this is an adjacent, silently-implied
consequence of "the adapter simplifies."

**`notes/2026-07-30-refactoring-review.md` left untracked, as found.**
Present in the working tree (both clones, per its own memory record)
but not part of any of the three steps' own file lists, and not
committed by this session.

**No unexpected `git status` dirt at any step boundary.** Every check
across all three steps showed exactly that step's own files, aside
from the pre-existing untracked review-report file disclosed above.
