# 2026-08-12 — Engine seed contract: non-negative, validated at entry

## Scope

Session prompt resolving the R8-chartered engine-test flake
(`ehrt.sim-engine.engine-test`'s
`mixed-authored-and-compiled-run-satisfies-the-full-invariant-catalog`).
Author ruling R9 (verbatim "a") classified the seed contract as
non-negative longs, engine-validated: the generator drew out-of-
contract inputs, the engine accepted them unvalidated. R30 standing
ceremony (commit and push at each checkpoint) — the prompt named no
prepare-only override.

## Step 0 — Preflight + tag

Working directory confirmed the ext4 clone. `git fetch` confirmed
`origin/main` at `c6deb5a` (`c6deb5a233477e562c3edcd2833798fab4f2719c`,
ADR-0115 close) — matched the driving prompt's own stated premise
exactly. Last five `test`-lane runs on `main`: all `success`, no red.
Tagged `stable-20260812-review-3-rulings` at `c6deb5a`, annotated,
message citing tag-law case (i); pushed; peeled ref verified via `git
ls-remote --tags origin` — exact match.

## Read-first STOP-AND-REPORT — no existing engine invalid-option convention

Read-first item 2 named this exact STOP condition in advance.
`engine.clj`'s only config validation was a `{:pre [...]}` clause that
throws `AssertionError` — no `result`-shaped envelope anywhere in the
file (confirmed by grep and by `components/sim/src/ehrt/sim/run.clj:164`'s
own comment naming `:pre` assertions as the mechanism). Stopped before
any git action; asked the author. **Ruling: adopt `ehrt.kernel.
interface`'s result-not-throw doctrine** (R10, `.agents/rulings.md`
"From ADR-0116") — `engine.clj` gains its first dependency on it.

## Step 1 — Repro and evidence capture

1. **Shrunk witness** (`{:seed -3377439408979484 ...}`, the defspec's
   exact config): did not throw, ran to completion, `check/check-all`
   returned `:rejected`/`:invariant-violation`/
   `:medication-end-references-existing-order-and-follows-it-in-time`
   at `t=178620` for patient `PID-000003-1a0eb69f`. Confirms the
   classification.
2. **Two recorded seeds, pinned quick-check, 150 trials each.**
   `1786546687672` — fails, shrinks to `[-3377439408979484]` exactly
   as recorded, confirmed deterministic on a second run. `7844068501`
   — **passed clean, 150/150, both runs.** STOP-AND-REPORT: the
   driving prompt's own evidence base treated both seeds as equally-
   confirmed repros; direct evidence in `notes/adr/
   0112-batch-straddle-recording.md` shows the "cleared on re-run"
   disclosure there was against a FRESH unpinned seed, never
   `7844068501` itself. **Ruling: proceed on `1786546687672` alone**,
   without re-litigating R9.
3. **CLI reachability, pre-fix.** `bin/ehrt sim run --seed -1
   --patients 1` — exit 0, `:status :ok`, a full (silently invalid-
   input-tolerant) run. Recorded per the prompt's own "record it,
   including if it surprisingly succeeds" instruction.
4. **Red regression deftest,** `run-rejects-negative-seed-with-clean-
   error`: 3 assertion failures against the pre-fix tree (expected
   `result/error?`, got a raw run map), 314/317 other assertions in
   the namespace unaffected.

## Step 2 — The fix, three parts plus two mid-session widenings

1. **Engine entry validation.** `engine.clj`'s `run` gains `(if (neg?
   seed) (result/error :invalid-seed {:key :seed :value seed :expected
   "a non-negative integer"}) (let [rng ...] ...))` — a single guard
   wrapping the existing body, nothing else in the run path touched.
2. **Contract statement.** `run`'s own `:seed` docstring; the `sim
   run`/`sim identifiers` `--seed` doc strings in `bases/cli/src/ehrt/
   cli/help.clj`; `make cli-doc` regenerated `docs/cli.md`, delta
   exactly those two rows. A third `--seed` row (`corpus generate`)
   was found and deliberately left untouched — verified dual-source
   (`:sim` delegates to the same fixed `run-command`; `:synthea` is an
   unrelated external contract) — disclosed rather than guessed at.
3. **Generator to contract, widened.** `engine_test.clj:1172`'s
   `gen/large-integer` → `(gen/large-integer* {:min 0})`. Running the
   full namespace afterward surfaced a NEW failure in an untouched
   sibling defspec at a freshly-drawn negative seed — direct evidence
   the single-site fix was insufficient. **STOP-AND-REPORT, ruling:
   widen the fence to a full repo-wide sweep** (verbatim ruling text in
   `notes/adr/0116-*.md`'s own Decision section). Extension-blind grep
   plus per-site verification (confirmed each generated value actually
   reaches `engine/run`'s `:seed`, not guessed) found **24 sites across
   7 files**; all 24 swept with the identical mechanical edit. Sites
   verified NOT affected (churn/order-profiles tests feeding a raw
   `Random` directly, `patient-id-for-differs-by-seed`, every
   sim-model/sim-trajectory generative test, one unrelated corpus-io
   generator) left untouched. Full inventory in `notes/adr/
   0116-engine-seed-contract.md`.
4. **Caller-propagation gap, found and fixed.** Witnessing the CLI's
   post-fix behavior (the author's own explicit instruction) found
   `bin/ehrt sim run --seed -1` still returning `:status :ok`/exit 0 —
   `ehrt.sim.run/run-command` and `ehrt.sim.identifiers/
   identifiers-command` both blindly destructured `engine/run`'s
   return, silently swallowing the new error map. **Second widening,
   ruled**: both functions now check `(result/error? engine-result)`
   first and propagate. Post-fix: both CLI verbs return `{:status
   :error, :category :invalid-seed, ...}` at exit 2; `--seed 42`
   sanity-checked unaffected.

**Green evidence:** new deftest passes; both quick-checks re-run green
at their recorded seeds; the full affected namespace set (engine-test,
churn-test/order-profiles-test as an untouched control group,
identifiers-test, latency-test, emit-hl7-test, v2-replay-test,
emit-fhir-test, check-test, run-test) — 265+113 tests, 919+421
assertions, 0 failures/0 errors across both verification passes. Full
`make test` (`clojure -M:poly check` + `clojure -M:poly test :all
skip:integration` + `bin/verify-nist-lock`): green throughout, 0
FAIL/ERROR anywhere; NIST lock OK.

`git diff --cached --stat` reviewed before staging: exactly the 12
touched files matching the widened fence. `gitleaks git --staged -v`:
clean. Committed `fc72f54` ("fix: sim engine seed contract --
non-negative, validated at entry (ADR-0116)"); `gitleaks detect`:
clean; pushed. Post-push verification: pushed message diffed against
the source file — only the trailing-blank-line `git log --format=%B`
artifact; ASCII byte-check clean. CI confirmed `completed`/`success`
on commit 1's own push (`31626435804`), `headSha` matching exactly —
watched to conclusion given this session's own subject is CI
flakiness in exactly this test suite.

## Step 3 — Oracle, ADR + ceremony surfaces, commit 2

`bin/regression-oracle c6deb5a fc72f54` → `IDENTICAL: every root's
digest matches between c6deb5a and fc72f54`, all 35 roots, matching
the pre-analysis (positive-seed roots, guard clause unreachable from
any of them).

`notes/adr/0116-engine-seed-contract.md` landed: context, tag
ceremony, R9/R10 rulings, the full Step 1 evidence verbatim, the
three-part fix plus both widenings with the full 24-site sweep
inventory, green evidence, oracle bracket, full gate, four disclosed
deviations, fences, index line. `notes/ADRs.md` gained its index line;
`notes/adr/README.md`'s own file count corrected 113→114. The
roadmap's R8 row moved to RESOLVED with the classification recorded;
the roadmap's Done section gained one pointer line. `.agents/
rulings.md` gained "From ADR-0116": R9 (the seed contract), R10 (the
kernel-adoption error convention), R11 (the caller-auditing
generalizable lesson). This session record and its prompt archive land
in the same commit, both READMEs updated.

## Deviations, disclosed

Four STOP-AND-REPORT findings, each resolved by an explicit author
ruling before proceeding:

1. No existing engine invalid-option convention (Read-first's own
   named STOP condition) — resolved by R10.
2. Seed `7844068501` did not reproduce when pinned (the driving
   prompt's own named STOP condition) — resolved by proceeding on the
   other seed alone.
3. The engine-side fix broke ~20 unfenced defspecs repo-wide — not
   named verbatim in the driving prompt but squarely within its
   STOP-AND-REPORT spirit — resolved by the fence-widening ruling and
   the 24-site sweep.
4. `run-command`/`identifiers-command` silently swallowed the new
   error, surfaced while executing the author's own explicit
   instruction to witness the CLI's post-fix behavior — resolved by a
   second fence-widening ruling.

No other deviation: `git status --porcelain` confirmed clean before
this session's first tool call; every other Read-first document
matched this session's own characterization of it once these four
findings were resolved.

## Close-out echo

**R9, verbatim** (`.agents/rulings.md`, "From ADR-0116"): "is the seed
contract (a) non-negative longs ... or (b) all longs legal" — RULED
"a". **R10:** adopt `ehrt.kernel.interface`'s result-not-throw
doctrine for the engine's first invalid-option convention. **R11:**
auditing every caller of a function whose return contract gains a new
Result-typed branch is not optional.

**The fix:** engine entry guard (`result/error :invalid-seed`);
contract docstrings + two `docs/cli.md` rows; 24-site generator sweep
across 7 files; two caller-propagation fixes
(`run-command`/`identifiers-command`).

**`bin/regression-oracle c6deb5a fc72f54`:** IDENTICAL, all 35 roots.

**`bin/verify-nist-lock`:** OK, all 6 hit-nexus-sourced coordinates
match `artifacts.lock.edn` exactly.

**SHAs:** Step 0 tag `stable-20260812-review-3-rulings` at `c6deb5a`.
Commit 1 `fc72f54`. Commit 2: this record's own landing commit.

**CI status:** `test` lane green on commit 1's push, watched to
conclusion (`headSha` match confirmed); commit 2's own run
recorded/disclosed at push.

## HEAD landed

`fc72f54` (commit 1) — commit 2 (this record's own commit) lands
after this record, in the same push as the prompt archive.
