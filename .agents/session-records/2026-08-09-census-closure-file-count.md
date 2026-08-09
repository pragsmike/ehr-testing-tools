# 2026-08-09 — Census closure-file-count fix

## Scope

Session prompt naming AR-CF-0 through AR-CF-5, executing ADR-0092
ruling 6 = D6-1 ("6 a."): the Next row ADR-0093 scheduled. Extends
`ehrt.sim-trajectory.census`'s `:closure-file-count` from counting
JSON modules only to counting lookup-table CSVs too, in both the
ok-walked and load-failed branches, gated red-to-green, closing a
3x-undercount repeat-cost record open since ADR-0074.

## Step 0 — Preflight + tag (AR-CF-0)

Working directory confirmed the ext4 clone, HEAD `77005de` (review-2
rulings landing, ADR-0093), branch up to date with `origin/main`,
working tree clean, `core.hooksPath` confirmed `.githooks`. `clojure
-M:poly check`: OK. Oracle pre-digest (`bin/regression-oracle 77005de
77005de`): all THIRTY-FOUR roots IDENTICAL, soundness "yes outside ns
form" — the expected trivial tip-against-itself result. Last five
`test`-lane runs on main all green.

Tagged `stable-20260809-review-2-rulings-landing` at `77005de`,
annotated, message "review 2's rulings landed, design-channel-verified
2026-08-09 (ADR-0093)" — the successor tag debt ADR-0093's own
"This session's own successor tag debt" section named; pushed; peeled
ref verified (`git ls-remote --tags origin` resolves the tag to
`77005de94c53bf940f303b3c6b55ff9a8fc8ff18` exactly).

## Step 1 — Red evidence (AR-CF-2)

Three deftests added to `census_test.clj`, beside the existing
ok-walked/load-failed fixtures, using two new fixture helpers
(`write-module-file!`, `write-table-file!`) that write into the
`<checkout-dir>/src/main/resources/modules/...` shape `make-resolve-
fn`/`make-table-resolve-fn` actually read from real Synthea checkouts:

1. `ok-walked-module-with-submodule-and-table-counts-all-distinct-
   files` — root + 1 `CallSubmodule` + 1 `lookup_table_transition`,
   expects `1+1+1 = 3`.
2. `load-failed-closure-counts-a-table-successfully-read-before-the-
   failure` — root names two tables, the first resolves, the second is
   missing; expects `1+1 = 2` (the successfully-read table still
   counted).
3. `ok-walked-module-with-submodule-and-no-tables-still-counts-only-
   modules` — the regression guard, expects `1+1 = 2`, unaffected by
   the bug.

Run against the unfixed tree:

```
FAIL in (load-failed-closure-counts-a-table-successfully-read-before-the-failure)
expected: (= 2 (:closure-file-count (:gap entry)))
  actual: (not (= 2 1))

FAIL in (ok-walked-module-with-submodule-and-table-counts-all-distinct-files)
expected: (= 3 (:closure-file-count (:gap entry)))
  actual: (not (= 3 2))

Ran 16 tests containing 46 assertions.
2 failures, 0 errors.
```

Test 3 (the regression guard) was green in this same run, as expected
— it never touches the buggy path. No commit.

## Step 2 — The fix (AR-CF-1)

`census.clj`: `make-table-resolve-fn` now takes the same `fetched`
atom `make-resolve-fn` already threads, recording a successful table
read under the collision-proof key `(str "lookup_tables/"
table-name)`. The ok-walked branch's count becomes `(+ (count modules)
(count tables))`; the load-failed branch's `(count @fetched)` needed
no arithmetic change once both resolvers write into it. A one-line
AR-D-6 comment states the counting definition at each site.

census-test namespace: 16 tests, 46 assertions, 0 failures, 0 errors
(green). Full local suite (`clojure -M:poly test :all
skip:integration`), re-run with output redirected to a controlled log
after the harness's own rolling capture truncated an earlier run's
output: 293 namespace blocks, 0 failures, 0 errors, exit 0.

Oracle bracket: `bin/regression-oracle` only accepts git refs, so the
uncommitted fix was captured via `git stash create` (a dangling commit
object, no working-tree or stash-list effect) and that object
(`6abe2a6...`) stood in for the target ref. `bin/regression-oracle
77005de 6abe2a6...`: all THIRTY-FOUR roots IDENTICAL — PURE IDENTITY,
as predicted (census is tooling, not the sim/engine path).

`clojure -M:poly check` and `clojure -M:poly test :docs-tooling`
(covers `done-pointer-adr-test`/`index-completeness-test` ahead of
Step 4's own doc edits, run pre-emptively): both clean, 0 failures.

## Step 3 — Witness (AR-CF-4)

The census entry point was pointed at the in-repo vendored module set
via a scratch-directory symlink (`<scratch>/src/main/resources/
modules` → `components/sim/resources/sim/modules`, no vendored bytes
touched, removed after use) — the checkout-dir contract admitted this
cleanly, no forcing needed. Run against `asthma`: `:ok-walked`,
`:closure-file-count 11` — root `asthma.json` + 2 submodules
(`medications/emergency_inhaler`, `medications/maintenance_inhaler`) +
8 distinct lookup tables (3 + 5) the two submodules name between them
= `1+2+8 = 11`, matching D6-1's own re-derivation exactly.

## Step 4 — ADR + ceremony surfaces + commit

`notes/adr/0094-census-closure-file-count.md` landed: the ruling
quoted, the defect's two-branch anatomy, red→green evidence pasted,
the witness result, the fix-forward sentence (AR-CF-3, historical
ADR/docstring counts untouched), the oracle-bracket identity, this
session's own successor tag debt. `notes/ADRs.md` gained its index
line; `notes/adr/README.md`'s own file count corrected 91→92
(`ls`-verified). `roadmap.md`'s Next row (landed by ADR-0093) removed,
one Done pointer added.

Staged: `.agents/plans/roadmap.md`, `census.clj`, `census_test.clj`,
`notes/ADRs.md`, `notes/adr/README.md`, `notes/adr/0094-census-
closure-file-count.md` — nothing else. `gitleaks git --staged -v`:
clean. Committed `6dd7c80` ("fix: census closure-file-count counts
lookup tables too -- both branches, gated red-to-green (ADR-0094,
ruling 6)"); pushed. ASCII check run FIRST on the landed message:
EMPTY. Message diff against the source file: only the trailing-blank-
line artifact, not a real mismatch. CI watched to conclusion (this
session's own prompt named it): `test` lane run `31328209204`, green,
3m57s — `poly check`, `poly test :all skip:integration`, and
generated-doc freshness all passed. The ADR's own post-push/CI
verification lines, left as placeholders at commit time (ADR-0093's
own precedent), were filled in afterward as a normal dated ADR append
landing in this session's Step 5 commit rather than by amending
`6dd7c80` — this repo never amends a landed commit.

## Step 5 — Ceremony (this record)

Session record and prompt archive land together, both READMEs
updated, plus the ADR-0094 verification fill-in from Step 4, same
commit.

## Deviations, disclosed

See the prompt archive's own deviation record
(`.agents/prompts/2026-08-09-census-closure-file-count.md`): a stale
read-first line reference corrected live (AR-D-6 read from
`.agents/rulings.md` instead of a non-existent `census.clj` comment
region), the oracle bracket run against a `git stash create` object
standing in for the uncommitted fix (the script only accepts refs),
the witness path admitting cleanly via a scratch symlink (no bounded-
deferral fallback needed), and the ADR verification section's post-
push fill-in landing in this Step 5 commit rather than amending the
Step 4 commit. All fences held otherwise: `census.clj` touched only at
the two count sites, the `make-table-resolve-fn` threading, and the
AR-D-6 comments; `census_test.clj` gained only the three new deftests;
no historical ADR or vendored-docstring rewrite; the roadmap gained
exactly one row move plus the Done pointer. `git status --porcelain`
clean before this session's first tool call, clean at each commit
boundary.

## Close-out echo

**Red evidence** (Step 1, against the unfixed tree):

```
FAIL in (load-failed-closure-counts-a-table-successfully-read-before-the-failure)
expected: (= 2 (:closure-file-count (:gap entry)))
  actual: (not (= 2 1))

FAIL in (ok-walked-module-with-submodule-and-table-counts-all-distinct-files)
expected: (= 3 (:closure-file-count (:gap entry)))
  actual: (not (= 3 2))

Ran 16 tests containing 46 assertions.
2 failures, 0 errors.
```

**The fix, in brief:** `make-table-resolve-fn` now records every table
it reads into the same `fetched` atom the module resolver already
uses (key `"lookup_tables/<name>"`); the ok-walked branch counts
`(+ (count modules) (count tables))` instead of `(count modules)`
alone.

**All three gates green** after the fix: 16 tests, 46 assertions, 0
failures, 0 errors.

**Witness:** `asthma` — `:ok-walked`, `:closure-file-count 11`
(matching D6-1's own re-derivation exactly; old buggy count was 3).

**Oracle bracket:** `77005de` vs the fix — 34/34 roots IDENTICAL, pure
tooling change, zero sim/engine-path effect.

**Shas:** tag `stable-20260809-review-2-rulings-landing` at `77005de`;
fix commit `6dd7c80`; this record's own commit follows.

**CI:** `test` lane run `31328209204`, green, 3m57s, watched to
conclusion.
