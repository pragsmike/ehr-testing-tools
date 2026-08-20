# Session record: oracle-coverage gate, integration half -- first green run (2026-08-20)

**Prompt:** [`.agents/prompts/2026-08-20-oracle-coverage-integration-half.md`](../prompts/2026-08-20-oracle-coverage-integration-half.md)
**ADR:** [`notes/adr/0160-oracle-coverage-integration-half.md`](../../notes/adr/0160-oracle-coverage-integration-half.md)
**Mode:** R30, autonomous. **Base:** `92d23bc`.

Review-4 finding **F-5**, rowed at the arc close as
`roadmap.md#oracle-coverage-gate-integration-half` (PRIORITY 1). Author
ruling 2026-08-20, verbatim: **"(a)."** -- the minimal session: fix the
extraction, trigger the `Integration` workflow, watch it to conclusion,
fold in nothing else. Nothing else was folded in.

## Step 0

`bin/preflight` **exit 1**, all five checks disclosed. The one
`FINDING:` is a red among the last five runs on `main` -- `Integration`
at `e967fd7c`, 2026-08-20T07:32:42Z, run **32344505291**. That IS the
F-5 red this session came to fix: DISCLOSED, not a stop, as the prompt
anticipated. The other four: edit root `/home/mg/src/ehr-testing-tools`,
not under `/mnt/`, `core.fileMode` **true**, `core.ignorecase` unset;
tree clean including untracked; HEAD `92d23bc` == `origin/main`; last
stable tag `stable-20260820-review-4-arc-close` @ `0e72ed4`, HEAD
untagged, **no tag owed**.

Baseline `make test`, unpiped, wrapper ending `exit "$MAKE_EXIT"`:

    MAKE_EXIT=0
    364 zero-failure blocks / 4,070 tests / 18,304 assertions
    Execution time: 20 minutes 25 seconds

Reconciles exactly against
[`2026-08-19-repo-review-4-arc-close.md`](2026-08-19-repo-review-4-arc-close.md),
whose Close verification section carries the same `364 / 4,070 /
18,304`. `clojure -M:poly check` **OK**.

Budgets at Step 0: `:corpus` 1832/2045, `:docs` 735/785, `:judge`
922/1000, `:onboarding` 1502/1530, `:sim` 1274/1405.

**The NPE, reproduced locally at `92d23bc`.** `integration.yml` runs
`clojure -M:poly test :all project:integration` from the workspace root
through poly's clojure-test runner; the local reproduction is that
invocation narrowed to the one namespace (the project's own `deps.edn`
dep set and `:test` extra-path, rewritten root-relative for the
workspace cwd, `clojure.test/run-tests` as the runner). Result:

    ERROR in (a-fresh-digest-witnesses-exactly-the-committed-coverage-claim-test) (RT.java:1241)
    Uncaught exception, not in assertion.
      actual: java.lang.NullPointerException: Cannot invoke
              "java.lang.Character.charValue()" because "x" is null
        clojure.core$subs.invokeStatic (core.clj:5038)
        ...oracle_coverage_test$committed.invokeStatic (oracle_coverage_test.clj:95)
        ...oracle_coverage_test$committed.invoke (oracle_coverage_test.clj:92)
        ...oracle_coverage_test$fn__201$fn__225$fn__226.invoke (oracle_coverage_test.clj:125)

    Ran 1 tests containing 6 assertions.
    0 failures, 1 errors.

Frame for frame and count for count the nightly's own failure. No new
red test is owed and none was written -- run 32344505291 is the red
witness.

## Commits

| sha | what |
|---|---|
| `8c53475` | fix: integration half matches `^:private` defs; roadmap dedup row; state-derived |
| this | docs: ADR-0160, close |

No red-first commit this session, so `rulings.md#R-red-pushed-with-green`
had nothing to bind: the red is a CI run id, not a commit.

## Step 1 -- the branch taken

**Inline, not shared.** Sharing `def-form` out of
`ehrt.docs-tooling.oracle-coverage-test` requires adding
`poly/docs-tooling` to `projects/integration/deps.edn`, which that file
refuses in writing twice (AR-3: docs-tooling's tests live in conformance
only; and "poly runs a declared brick's tests in every composing
project" -- so composing it would pull docs-tooling's whole test tree
into the nightly lane to share seven lines). Moving the helper to a
docs-tooling `src` ns to dodge that would put test scaffolding on
shipped surface. So the two-prefix `some` is inlined, the docs-tooling
sibling is named in the docstring as the canonical twin, and the dedup
is rowed at `roadmap.md#oracle-coverage-extractor-dedup` (new, PRIORITY
7).

One deliberate widening past the prefix fix: the extractor returns
**nil** on a miss instead of throwing, so a gate that cannot find its
subject fails as a broken CLAIM (a failed equality assertion pointing at
`digest.clj`) rather than as a broken TEST (an uncaught NPE pointing at
the harness).

**Local green, same invocation:**

    Ran 1 tests containing 8 assertions.
    0 failures, 0 errors.

**6 -> 8, with nothing added to the deftest.** The NPE consumed the
first equality assertion's slot and aborted the var, so the second
equality assertion and the capacity-witness assertion had never run. The
eight: exit 0; 35 `.edn` roots; 32 engine-layer roots; kinds non-empty;
types non-empty; witnessed event kinds == committed; witnessed message
types == committed; capacity witness == `["death-fixture"]`.

Because the two equality assertions passed, the fresh digest's witnessed
sets are exactly the committed ones -- **13 event kinds** (`:admission`
`:care-plan-end` `:care-plan-start` `:diagnostic-report` `:discharge`
`:medication-end` `:medication-order` `:observation`
`:outpatient-visit` `:outpatient-visit-end` `:procedure` `:registered`
`:transfer`) and **5 message types** (`ADT^A01` `ADT^A02` `ADT^A03`
`ADT^A04` `ORU^R01`). Not vacuous: both `R-empty-population-is-red`
guards passed in the RED run too, so both sides were known non-empty
before the equality was asked.

## Step 2 -- the deliverable

`gh workflow run Integration --ref main` at `8c53475`, watched to
conclusion with `gh run watch --exit-status`.

| run | trigger | sha | conclusion |
|---|---|---|---|
| **32344505291** | schedule 07:32:42Z | `e967fd7` | **failure** -- the gate's first execution ever; `Ran 1 tests containing 6 assertions. 0 failures, 1 errors.` |
| **32402746494** | workflow_dispatch 18:21:33Z | `8c53475` | **success** -- `Ran 1 tests containing 8 assertions. 0 failures, 0 errors.` |

Green run's own block: digest 18:32:19 -> 18:33:20 (**61 s**), `Test
results: 8 passes, 0 failures, 0 errors.`, whole lane 11 minutes 8
seconds. The count matches the local green exactly, which is what shows
the local narrowing reproduced the lane rather than approximating it.

Push-lane `test` run **32402730016** @ `8c53475`: **success**.

## Close verification

Full `make test` at the close, unpiped, wrapper ending
`exit "$MAKE_EXIT"`:

    MAKE_EXIT=0
    364 zero-failure blocks / 4,070 tests / 18,304 assertions

**Delta against Step 0: ZERO in all three figures** -- the right answer
for a session whose only test change is in the namespace
`skip:integration` excludes, plus prose and the generated files that
follow from it. `clojure -M:poly check` **OK**.

`bin/post-push-verify` after each push: remote tip == local tip,
per-commit ASCII over the range OK, CI run at the tip reported once.

**Tag paid in session.** Push-lane CI at the close tip, run
**32405698519** @ `d5edf8a`, concluded **success** while this session
was still open, verified by its own `gh run view` -- which is
`rulings.md#R-session-verifies-ci-via-gh`'s condition, so under
`R-tag-law` the tag is paid now rather than deferred:
`bin/tag-ceremony stable-20260820-oracle-coverage-integration-half
d5edf8a ... --push`, remote peeled ref verified to match the target
exactly. **No tag owed at the next Step 0.** CI receipts, both pushes
green: `8c53475` run 32402730016, `d5edf8a` run 32405698519.

## Register hygiene at close

`roadmap.md#oracle-coverage-gate-integration-half` -> **CLOSED
2026-08-20 ADR-0160**, moved to `## Done`, citing green run 32402746494.
`roadmap.md#oracle-coverage-extractor-dedup` opened at PRIORITY 7 (the
slot review-4's closes left empty). Net roadmap delta: zero rows in
`## Next`.

Reading sets re-measured at the close:

| set | Step 0 | close | budget | headroom |
|---|---|---|---|---|
| `:corpus` | 1832 | 1832 | 2045 | 213 |
| `:docs` | 735 | 735 | 785 | 50 |
| `:judge` | 922 | 922 | 1000 | 78 |
| `:onboarding` | 1502 | 1508 | 1530 | 22 |
| `:sim` | 1274 | 1274 | 1405 | 131 |

`:onboarding` moved +6, the roadmap's own net line change (one row out
of `## Next` at six lines, one row in at six, one CLOSED row into
`## Done` at six). All five under budget; none at its baseline, so
`rulings.md#R-budget-stop` is not engaged.

## Fence

Files touched: the integration test namespace; `.agents/plans/
roadmap.md`; `notes/adr/0160-*.md`; the prompt archive; this record;
`.agents/state-derived.md` and the two record `INDEX.md`s and the two
README index lines (generated/scaffolded); `notes/ADRs.md` (generated).
NO `digest.clj`, NO `bin/`, NO engine, NO docs-tooling namespace, no
test deleted.

**Noted, not rowed.** The deftest's bare `.mkdirs` / `.listFiles` /
`.delete` at `:127`, `:130`, `:165-166` are outside the io-vocabulary
lint's `components/*/src` scope and outside this fence. Judgement
recorded in the ADR: a test managing its own temp directory in
`try/finally` is what that scope boundary deliberately permits, so a row
here would be a row against the lint's scope, a different question from
F-5.
