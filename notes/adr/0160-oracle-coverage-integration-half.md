## ADR-0160 — the oracle-coverage gate's integration half runs green for the first time

**Status:** Accepted (author-directed, autonomous session per R30),
2026-08-20.

### Context

Review-4 finding **F-5**, rowed at the arc close and carried as
`roadmap.md#oracle-coverage-gate-integration-half`, PRIORITY 1. Quoted
from the row as it stood at `92d23bc`:

> the fresh-digest half of R4-Q6's coverage gate throws NPE before it
> asserts anything: `oracle_coverage_test.clj:95` searches
> `"(def <name>"` while `digest.clj` writes `(def ^:private <name>`, so
> `subs` gets nil. ADR-0156 refined that extractor in the docs-tooling
> half (which handles both) and not in this one. It has never once run
> green; nightly `Integration` 32344505291 is its first execution.
> ADR-0159 F-5.

Author ruling, 2026-08-20, verbatim: **"(a)."** — the minimal session:
fix the extraction, trigger the `Integration` workflow, watch it to
conclusion, fold in nothing else.

This is the review-4 watch-list class **W-1, born-red gates**, caught in
the act. ADR-0156 landed both halves of the coverage gate on the same
day. The docs-tooling half runs on every push and was green from its
first run. The integration half runs only in the scheduled lane, and its
first execution was the nightly of 2026-08-20 — eighteen hours and four
green sessions after it landed. Four sessions passed a full `make test`
over a namespace that could not survive its own first line of work,
because `make test` runs `poly test :all skip:integration` and that
namespace is exactly what `skip:integration` skips.

### Step 0

`bin/preflight` **exit 1**, every check disclosed. One `FINDING:` — a
red run among the last five on `main`: `Integration` at `e967fd7c`,
2026-08-20T07:32:42Z. That is the F-5 red itself, DISCLOSED and not a
stop, exactly as the prompt anticipated. The other four checks OK: edit
root `/home/mg/src/ehr-testing-tools`, not under `/mnt/`,
`core.fileMode` **true**, `core.ignorecase` unset; tree clean including
untracked; HEAD `92d23bc` == `origin/main`; last stable tag
`stable-20260820-review-4-arc-close` @ `0e72ed4`, HEAD untagged, **no
tag owed**.

Baseline `make test`, unpiped, `MAKE_EXIT` captured, wrapper ending
`exit "$MAKE_EXIT"`: **`MAKE_EXIT=0`, 364 zero-failure blocks / 4,070
tests / 18,304 assertions**, 20 minutes 25 seconds. Reconciles exactly
against **`.agents/session-records/2026-08-19-repo-review-4-arc-close.md`**,
whose own Close verification section carries `364 zero-failure blocks /
4,070 tests / 18,304 assertions` (ADR-0159's arc-close record; the ADR
itself carries the same figure at its Step 0). `clojure
-M:poly check` **OK**.

Budgets at Step 0: `:corpus` 1832/2045, `:docs` 735/785, `:judge`
922/1000, `:onboarding` 1502/1530, `:sim` 1274/1405 — all under.

### The NPE, captured locally

`integration.yml` runs `clojure -M:poly test :all project:integration`,
from the workspace root, through poly's clojure-test runner. The local
reproduction is that invocation narrowed to the one namespace: the
project's own `deps.edn` dep set and `:test` extra-path, with
`../../components` and `test` rewritten root-relative for the workspace
cwd, and `clojure.test/run-tests` as the runner. The digest subprocess
is built by the test itself, unchanged.

At `92d23bc`, unfixed:

```
Testing ehrt.integration.oracle-coverage-test

ERROR in (a-fresh-digest-witnesses-exactly-the-committed-coverage-claim-test) (RT.java:1241)
Uncaught exception, not in assertion.
expected: nil
  actual: java.lang.NullPointerException: Cannot invoke "java.lang.Character.charValue()" because "x" is null
 at clojure.lang.RT.intCast (RT.java:1241)
    clojure.core$subs.invokeStatic (core.clj:5038)
    ehrt.integration.oracle_coverage_test$committed.invokeStatic (oracle_coverage_test.clj:95)
    ehrt.integration.oracle_coverage_test$committed.invoke (oracle_coverage_test.clj:92)
    ehrt.integration.oracle_coverage_test$fn__201$fn__225$fn__226.invoke (oracle_coverage_test.clj:125)
    ...
Ran 1 tests containing 6 assertions.
0 failures, 1 errors.
```

Frame for frame the nightly's own stack (`:95` from `:92`, called at
`:125` inside `:122` inside `:121`), and assertion for assertion its
count: the nightly recorded **`Ran 1 tests containing 6 assertions. 0
failures, 1 errors.`** too. No new red test is owed and none was
written; run **32344505291** is the standing red witness and the local
capture is its confirmation, not its replacement.

What the 6 assertions mean is worth stating, because it is the reason
this gate looked alive for a day. Five of them PASSED: the digest
process exited 0, it wrote 35 `.edn`, 32 of them were engine-layer
roots, and both witnessed sets were non-empty. Everything up to and
including `rulings.md#R-empty-population-is-red` worked. The gate then
threw on the first assertion that compares the fresh digest to the
committed claim — that is, on every assertion the gate exists FOR. A
reader skimming the log sees five green assertions and one error, and
the shape flatters: it reads as a gate that mostly works. It was a gate
that had never once done its job.

### Step 1 — the fix, and the branch not taken

The prompt's preferred shape was ONE extractor, shared, if that needed
no composition change. **It needs one, so the inline branch was taken.**

`ehrt.docs-tooling.oracle-coverage-test`'s `def-form` is a private fn in
a docs-tooling TEST namespace. Reaching it from
`projects/integration` — whether by requiring it directly or by moving
it to a docs-tooling `src` ns — means adding `poly/docs-tooling` to
`projects/integration/deps.edn`. That project's own `deps.edn` refuses
that composition in writing, and gives the reason twice:

- on the `poly/palgebra` drop: "that edge moved to docs-tooling, which
  this project deliberately does not include -- AR-3 places
  docs-tooling's own tests in conformance only";
- on the `poly/judge-v2-nist` re-add: "poly runs a declared brick's
  tests in every composing project", which is precisely what composing
  docs-tooling here would do — pull docs-tooling's entire test tree into
  the nightly lane to share seven lines.

Moving the helper to `src` to dodge that would be worse in a second way:
it is test-scaffolding, and `src` in this workspace is shipped surface.

So the two-prefix `some` is inlined into `committed`, with the
docs-tooling sibling named in the docstring as the canonical twin and
the reason for the duplication recorded there. The dedup is rowed, not
forgotten: `roadmap.md#oracle-coverage-extractor-dedup`, new at PRIORITY
7 (the slot review-4's closes had left empty).

One thing was widened past a pure prefix fix, and deliberately. The
extractor now returns **nil** on a miss instead of throwing, and every
caller asserts on what comes back. The defect this ADR closes is not
only that a prefix was wrong — it is that a wrong prefix presented as an
uncaught NPE, which reads as a broken TEST. Read that way, the natural
response is to go looking at the test harness. The same miss as a failed
equality assertion reads as a broken CLAIM, which is what it is, and
points at `digest.clj`. A gate that cannot find its subject has to fail
loud in the vocabulary of the thing it gates.

### Local green — and what its assertions actually did

Same invocation, after the fix:

```
Testing ehrt.integration.oracle-coverage-test

Ran 1 tests containing 8 assertions.
0 failures, 0 errors.
```

**6 -> 8**, and the two extra are not new assertions — nothing was added
to the deftest. They are the two that had never been reached: the NPE
consumed the first equality assertion's slot and aborted the var before
the second equality assertion and the capacity-witness assertion ran at
all. The green run's eight are: exit 0; **35 `.edn` roots**; **32
engine-layer roots**; kinds non-empty; types non-empty; **witnessed
event kinds == committed**; **witnessed message types == committed**;
capacity witness == `["death-fixture"]`.

The two equality assertions are the point of the whole gate, so their
subjects are recorded here. Because they passed, the fresh 35-root
digest's witnessed sets ARE these, element for element:

- **witnessed event kinds, 13 of the closed 21** — `:admission`,
  `:care-plan-end`, `:care-plan-start`, `:diagnostic-report`,
  `:discharge`, `:medication-end`, `:medication-order`, `:observation`,
  `:outpatient-visit`, `:outpatient-visit-end`, `:procedure`,
  `:registered`, `:transfer`.
- **witnessed message types, 5** — `ADT^A01`, `ADT^A02`, `ADT^A03`,
  `ADT^A04`, `ORU^R01`.

Neither comparison is vacuous: the two `R-empty-population-is-red`
assertions guarding them passed in the RED run as well as the green, so
both sides were known non-empty before the equality was asked.

Full `make test` after the fix: **`MAKE_EXIT=0`, 364 blocks / 4,070
tests / 18,304 assertions** — byte-identical to Step 0, as it must be:
the changed namespace is the one `skip:integration` excludes, and the
roadmap row plus regenerated `state-derived.md` moved no counts.
`poly check` **OK**.

### Step 2 — the deliverable

`gh workflow run Integration --ref main`, dispatched at `8c53475`,
watched to conclusion with `gh run watch --exit-status`.

| run | trigger | sha | conclusion |
|---|---|---|---|
| **32344505291** | schedule, 2026-08-20T07:32:42Z | `e967fd7` | **failure** — the gate's first execution ever, `Ran 1 tests containing 6 assertions. 0 failures, 1 errors.` |
| **32402746494** | workflow_dispatch, 2026-08-20T18:21:33Z | `8c53475` | **success** — `Ran 1 tests containing 8 assertions. 0 failures, 0 errors.` |

The green run's own block, from its log: the digest ran 18:32:19 ->
18:33:20, **61 seconds**, and `Test results: 8 passes, 0 failures, 0
errors.` Whole lane 11 minutes 8 seconds. The count matches the local
green exactly, which is the check that the local narrowing reproduced
the lane rather than approximating it.

Push-lane CI at the fix commit, `test` run **32402730016** @ `8c53475`:
**success**.

### Fence

Files touched, against the prompt's list: the integration test
namespace; `.agents/plans/roadmap.md`; this ADR; the prompt archive; the
session record; `.agents/state-derived.md` and the two record
`INDEX.md`s (regenerated); `notes/ADRs.md` (generated index). NO
`digest.clj`, NO `bin/`, NO engine. The optional docs-tooling namespace
was NOT touched — the shared branch was not taken, so no private copy
was deleted and no test was deleted at all.

**Left in place, in scope of nothing here.** The deftest's bare
`.mkdirs` / `.listFiles` / `.delete` (now at `:127`, `:130`, `:165-166`
after the docstring grew) are outside the io-vocabulary lint, whose
scope is `components/*/src`, and outside this session's fence. They are
not rowed. The judgement: the lint's scope boundary is deliberate —
test scaffolding that manages its own temp directory in a `try/finally`
is the case the vocabulary rule exists to permit, not the case it exists
to catch. A row here would be a row against the lint's scope, which is a
different question from F-5 and does not belong to it.

### What this closes, and what it leaves

`roadmap.md#oracle-coverage-gate-integration-half` closes, cited to run
32402746494.

It does not discharge **W-1**. The review-4 watch-list carries born-red
gates as a CLASS, and this is one instance of it, found by the mechanism
W-1 predicts (a gate whose lane runs on a slower clock than the sessions
that land it). The general shape — a gate that lands green in `make
test` and is not executed by any lane for hours or days — is untouched
here and stays W-1's, for review 5.
