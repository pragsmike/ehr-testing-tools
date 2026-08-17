## ADR-0094 — Census closure-file-count fix: the lookup tables get counted too

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-09.

### Context

Prior: `notes/adr/0093-review-2-rulings-landing.md` scheduled, but did
not run, the fix ADR-0092's own register row D6-1 recommended. The
author ruled 2026-08-09 (ADR-0092 ruling 6, verbatim "6 a."), against
ADR-0092's own options text (D6-1): "(a) schedule a small census-tool
session now (the fix is well-understood: extend the JSON-module
resolver's counting to the CSV lookup-table resolver too); (b)
explicitly re-defer with a stated trigger... **Recommendation: (a)**."
This session is that small session.

The defect, confirmed by live read of `components/sim-trajectory/src/
ehrt/sim_trajectory/census.clj` at `77005de` before any edit: two
branches of `census-one` compute `:closure-file-count`, and both
counted JSON modules only, never lookup-table CSVs.

- **ok-walked branch:** `:closure-file-count (count modules)` — the
  destructuring on the same let-binding already pulls `{:keys [modules
  tables]}` from the closure payload; `tables` was simply never
  counted.
- **load-failed branch:** `:closure-file-count (count @fetched)` —
  `fetched` is populated only by `make-resolve-fn` (JSON module reads);
  `make-table-resolve-fn` never recorded its own reads, so tables read
  before a load failure were invisible to the count.

Repeat-cost record this fix closes: three real, disclosed undercounts
(asthma 3 vs 11, vhd-pulmonic and vhd-tricuspid 2 vs 4 each — register
D6-1, `notes/adr/0074-vendoring-arc-close.md`), plus review 1's own
unactioned "escalate priority" ask, restated unfixed across six window
ADRs (0081, 0083, 0087, 0088, 0089, 0091) before this session.

### Decision

**AR-CF-1 (the fix):** `:closure-file-count` means the number of
DISTINCT files read into the closure — root module + transitively-
called submodules + lookup-table CSVs (AR-D-6: the corrected count
states its definition where it is computed, a one-line comment at each
site). Both branches now converge on that definition:

- **ok-walked:** `(+ (count modules) (count tables))`.
- **load-failed:** `make-table-resolve-fn` now takes the SAME `fetched`
  atom `make-resolve-fn` already threads, and records a successful
  table read under the collision-proof key `(str "lookup_tables/"
  table-name)` — matching the on-disk relative path, disjoint by
  construction from a module call-path key (which never contains a
  `/`). `(count @fetched)` needed no arithmetic change once both
  resolvers write into it.

No other semantic change — `move-don't-improve` generalized: this fix
IS the session's one improvement, nothing else in `census.clj` moved.

**AR-CF-2 (co-landed gates):** three new deftests beside the existing
census tests, gated red before the fix, green after:

1. `ok-walked-module-with-submodule-and-table-counts-all-distinct-files`
   — a root with 1 submodule (`CallSubmodule`) and 1 lookup table
   (`lookup_table_transition`) must report `1+1+1 = 3`.
2. `load-failed-closure-counts-a-table-successfully-read-before-the-
   failure` — a root naming two tables, the first resolves, the second
   is missing (`:load-failed`); the table read before the failure must
   still be counted (`1` root `+ 1` table `= 2`).
3. `ok-walked-module-with-submodule-and-no-tables-still-counts-only-
   modules` — the regression guard: a closure naming no lookup tables
   reports exactly its module count (`1+1 = 2`), unchanged by the fix.

**AR-CF-3 (fix-forward, historical record untouched):** census numbers
already recorded in prior ADRs and in the two vendored-test docstrings
that say "UNDERCOUNTS" (`vendored_vhd_pulmonic_test.clj`,
`vendored_vhd_tricuspid_test.clj`) describe the artifacts as they WERE
— none of that prose is rewritten. Future census runs simply report
the corrected count.

**AR-CF-4 (real-module witness, bounded):** the census entry point was
pointed at the in-repo vendored module set
(`components/sim/resources/sim/modules/`) via a scratch-directory
symlink admitting the `<checkout-dir>/src/main/resources/modules`
contract cleanly (`ln -s .../components/sim/resources/sim/modules
.../src/main/resources/modules`, a few lines, no vendored bytes
touched) and run against `asthma`. Result: `:ok-walked`,
`:closure-file-count 11` — matching D6-1's own re-derivation exactly
(root `asthma.json` + 2 submodules, `medications/emergency_inhaler`
and `medications/maintenance_inhaler` + 8 distinct lookup tables the
two submodules name between them (3 + 5) = `1+2+8 = 11`).

**AR-CF-5 (roadmap closure):** the "Census tool: `:closure-file-count`
fix, scheduled" Next row (landed by ADR-0093) is done; it is removed
from Next and this ADR's Done pointer is added, per the standing
one-line-pointer contract (`.agents/rulings.md`, "the canonical
session-narrative hierarchy," AR-B-4 — the roadmap Done entry is a
pointer, the narrative above is this ADR's own).

### Red evidence (Step 1, against the unfixed tree)

```
Testing ehrt.sim-trajectory.census-test

FAIL in (load-failed-closure-counts-a-table-successfully-read-before-the-failure) (census_test.clj:384)
root names two tables -- the first resolves, the second is
            missing and fails the closure. The table read BEFORE the
            failure must still be counted...
1 root + the 1 table successfully read before the failure
expected: (= 2 (:closure-file-count (:gap entry)))
  actual: (not (= 2 1))

FAIL in (ok-walked-module-with-submodule-and-table-counts-all-distinct-files) (census_test.clj:359)
1 root + 1 submodule + 1 lookup table = 3 -- the ok-walked
            branch's own `:closure-file-count` must count `tables` too,
            not only `modules`
1 root + 1 submodule + 1 table
expected: (= 3 (:closure-file-count (:gap entry)))
  actual: (not (= 3 2))

Ran 16 tests containing 46 assertions.
2 failures, 0 errors.

Test results: 44 passes, 2 failures, 0 errors.
```

The regression-guard test (no-tables case) was GREEN in this same red
run, as expected — it exercises a code path the bug never touched.

### Green evidence (Step 2, after the fix)

```
Testing ehrt.sim-trajectory.census-test

Ran 16 tests containing 46 assertions.
0 failures, 0 errors.

Test results: 46 passes, 0 failures, 0 errors.
```

Full local suite (`clojure -M:poly test :all skip:integration`): 293
namespaces exercised, 0 failures, 0 errors, exit 0.

### Verification

- `clojure -M:poly check`: OK, Step 0.
- Oracle pre-digest (`bin/regression-oracle 77005de 77005de`): all
  THIRTY-FOUR roots IDENTICAL, the expected trivial tip-against-itself
  result.
- Oracle bracket over the fix itself (`bin/regression-oracle 77005de
  <fix-commit-object>`, the fix captured via `git stash create` into a
  dangling commit object ahead of this ADR's own commit so the bracket
  could run before landing, per this session's own no-commit-until-
  Step-4 ordering): all THIRTY-FOUR roots IDENTICAL — PURE IDENTITY, as
  predicted (census is tooling, not the sim/engine path; no digest
  movement to escalate).
- census-test namespace: 16 tests, 46 assertions, 0 failures, 0 errors
  (red-to-green evidence above).
- Full local suite: 293 namespaces, 0 failures, 0 errors.
- Witness: `asthma`, `:ok-walked`, `:closure-file-count 11` (AR-CF-4,
  above).
- `gitleaks git --staged -v`: clean at this commit.
- Tag verification: `stable-20260809-review-2-rulings-landing`
  (this session's own Step 0, the successor tag debt ADR-0093 named)
  tagged at `77005de`, pushed, peeled ref resolves to
  `77005de94c53bf940f303b3c6b55ff9a8fc8ff18` exactly.
- Last five `test`-lane runs (`gh run list --limit 5 --branch main`),
  checked at Step 0: all green (through ADR-0093's own close).
- Post-push message verification and the ASCII check (AR-RL2-5,
  `.agents/rulings.md`): ASCII check (`git log --format=%B -1 |
  LC_ALL=C grep -n '[^ -~]'`) run FIRST against `6dd7c80`, EMPTY.
  Message diff against the source file: only delta is the trailing-
  blank-line artifact `git log --format=%B` always adds, not a real
  mismatch.
- CI watched to conclusion (this session's own prompt named it):
  `test` lane run `31328209204`, green, 3m57s — `poly check`, `poly
  test :all skip:integration`, and generated-doc freshness all passed.
- `git status --porcelain`: clean before this session's first tool
  call, clean at the commit boundary.

### Fences

`census.clj` changed only at the two count sites, the
`make-table-resolve-fn` threading, and the AR-D-6 comments — no other
census logic, no `gmf.clj`, no `interface.clj`, no other `src` file
anywhere. Tests: `census_test.clj` additions only, no existing test
edited. No historical ADR or vendored-docstring rewrites (AR-CF-3). No
re-census of any module beyond the bounded `asthma` witness (AR-CF-4).
Roadmap: the one row move plus the Done pointer, nothing else.

### This session's own successor tag debt

The next session that opens fresh work tags
`stable-20260809-census-closure-file-count` at THIS session's own
closing tip, under standing ceremony — the tag-law case (ii) pattern.

### Index line

```
- 2026-08-09 — census-closure-file-count — ADR-0094
```

(appended to `.agents/plans/roadmap.md`'s own Done section, replacing
the Next row this ADR closes.)

`notes/adr/README.md`'s own file count corrects 91→92, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated

Untouched, carried forward from ADR-0093: fix clusters A and B in
full, D8-4's own unruled call, ruling 1's own unruled option (b), the
oracle's own blind-spot intake (H-3), the two remaining `defspec`
flake watch items (D3-2), the ADR-footnote-fork backlog row (D7-14),
`make quickstart`'s own untimed full run (D8-8), the two deferred
veteran modules under their true names, and publish-prep Externals.
What's new: this session's own successor tag debt (above); no new
horizon items open by this session's own narrow scope.

### Consequence

The 3x undercount that aged through six window ADRs unfixed, and
review 1's own unactioned "escalate priority" ask, both close on the
same evidence trail: a diagnosed, well-understood fix, gated red then
green, oracle-confirmed as pure tooling with zero sim/engine-path
effect, and witnessed against a real vendored module at exactly the
count D6-1's own re-derivation predicted (11, not 3). Nothing else in
`census.clj` moved.

### Index summary (moved verbatim from notes/ADRs.md by ADR-0143, 2026-08-16)

Census closure-file-count fix: the lookup tables get counted too — ruling 6 = D6-1 "a" executes: both `census-one` branches converge on one definition (root + submodules + lookup-table CSVs), the load-failed branch's `fetched` atom now threads through `make-table-resolve-fn` too; three new deftests gate red-to-green (1+N+M, a table read before a load failure, and the no-tables regression guard); the oracle bracket over the fix itself holds pure identity across all 34 roots; a bounded real-module witness against the in-repo `asthma` closure reports 11, matching D6-1's own re-derivation exactly
