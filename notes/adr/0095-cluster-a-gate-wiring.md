## ADR-0095 — Cluster A: the classpath static gate lands, verify-nist-lock joins the push lane

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-09.

### Context

The author ruled 2026-08-09: "Let's do cluster A" — ADR-0092's fix
cluster A (`.agents/plans/2026-08-09-repo-review-findings.md`, rows
D2-4 and D2-18). ADR-0092's own charter paragraph, quoted verbatim:

> **Cluster A — CI/gate wiring (D2-18, D2-4).** Two "a check exists (or
> should exist) but doesn't run where it matters" gaps, same shape, same
> file family (`test.yml`/`Makefile`). (i) A static docs-tooling gate,
> reader-based like its siblings (`sim_emit_hl7_dependency_test.clj`'s
> own extraction method): for every project in `workspace.edn`, parse
> each composed brick's TEST-tree requires, resolve to the owning brick,
> assert it's in the composing project's own `deps.edn` — closes the
> `2088763` classpath-break class structurally, not just this one
> instance. (ii) Add `bin/verify-nist-lock` as an explicit `test.yml`
> step (or fold it into the canonical "Full suite" command every session
> already runs), restoring the enforcement surface its own header
> comment has claimed for three arcs without actually having it.

The two register rows, quoted in full:

> **D2-18** | Does any gate on the push lane (or anywhere routinely
> watched) catch a test file's `:require` naming a component absent
> from its containing project's own classpath composition — the
> `2088763` incident class (H-4, above)? | `git show 2088763` read in
> full: `judge`'s test tree (composed into EVERY project including
> `integration`) required `ehrt.judge-v2-nist.interface` directly after
> `integration`'s own `deps.edn` had dropped that dependency; `poly
> check` did NOT catch it (ADR-0088's own Verification: "poly check:
> OK" against the very tree with the gap) — only the next scheduled
> `Integration` run did, up to a day later. Searched all 26
> `docs-tooling` test files for anything checking
> project-deps-vs-test-tree-requires composition: none. Root cause is
> structural — this repo's own docstrings explicitly disclose
> test-context cross-brick requires as "deliberate and precedented,"
> which is exactly what makes Polylith's native src-level dependency
> graph blind to this hazard class. | **Confirmed live gap.** Two
> closely related incidents (this one and H-5/`cd08b20`) inside one
> 24-hour session window, zero mechanical gate added for either —
> precisely "a check that runs only where nobody looks." | A static
> gate, docs-tooling-shaped like its siblings: for every project in
> `workspace.edn`, parse each composed brick's TEST-tree `ns` requires
> (reader-based, matching `sim_emit_hl7_dependency_test.clj`'s own
> extraction method), resolve back to the owning brick, assert it's in
> the composing project's own `deps.edn`. | fix-session-candidate

> **D2-4** | **New.** `bin/verify-nist-lock`'s own header claims it is
> "wired into `make test`... right after `poly test :all
> skip:integration`," so "not yet resolved" should never fire in that
> lane. Does the actual push lane, or routine session practice, ever
> invoke it? | `test.yml` never calls `make test` — it inlines `poly
> check`/`poly test` as separate steps, never the Makefile's own
> `test:` target (which DOES include `bin/verify-nist-lock`, confirmed
> by reading the Makefile). Grepped all 17 ADRs from 0075 through 0091
> for `make test`/`verify-nist-lock` in their own Verification
> sections: **zero hits**. `.agents/skills/build-session/SKILL.md`'s
> own standing ceremony names no step running it either. | A
> supply-chain integrity check (NIST-jar sha256 vs. `artifacts.lock.edn`)
> is currently exercised NOWHERE in routine practice — not CI, not the
> last 17 sessions' own verification, not the standing skill. Its own
> header's claimed enforcement surface is false against the live tree,
> drift this dimension's probe exists to catch. | Add
> `bin/verify-nist-lock` as an explicit `test.yml` step, or fold it
> into every session's canonical "Full suite" command. |
> fix-session-candidate

### Decision

**AR-CA-1 (D2-18, the static gate):** new docs-tooling test,
`ehrt.docs-tooling.project-classpath-test`
(`components/docs-tooling/test/ehrt/docs_tooling/
project_classpath_test.clj`), reader-based like its sibling
(`sim_emit_hl7_dependency_test.clj`'s own extraction method): for
EVERY project named in `workspace.edn` (`development` included — it
composes everything and passes trivially, cheap generality), parses
each composed brick's own TEST-tree `:require` forms, resolves each
`ehrt.<name>.*` namespace to its owning brick, and asserts that brick
appears in the composing project's own `deps.edn` (or, for
`development` — which has no `deps.edn` of its own — the root
`deps.edn`'s own `:dev` alias; `:necessary` overrides folded in too).
Failure message names the test file, the required namespace, the
owning brick, and the project whose composition lacks it — the
`2088763` commit message is the model of the disclosure this gate
automates. The gate self-wires into the push lane by being an ordinary
docs-tooling test; no workflow edit needed for (i).

**AR-CA-2 (the gate's witness pair):** the gate's own extraction logic
run against a disposable worktree at `2088763~1` (the commit before
the fix) reports EXACTLY the judge / `ehrt.judge-v2-nist` /
`integration` violation and nothing else; against HEAD, it passes
clean. Both outputs below.

**AR-CA-3 (D2-4, the wiring):** `bin/verify-nist-lock` added as an
explicit named step in `test.yml`, immediately AFTER `poly test :all
skip:integration` — the suite's own dependency resolution has by then
populated the Maven repo the check reads; placed before it, exit 2
"not yet resolved" would fail the lane spuriously.

**AR-CA-4 (the check can actually fire):** before landing, `bin/
verify-nist-lock --repo <empty scratch dir>` run locally (exit 2, "not
yet resolved," all six coordinates named missing) and against the real
local repo (exit 0, six coordinates matched) — proof the check trips
when reality diverges, not just passes when it doesn't. Both outputs
below.

**AR-CA-5 (the header):** `bin/verify-nist-lock`'s own header
corrected to name the ACTUAL surfaces truthfully post-wiring: `make
test` (local convenience target) and the `test.yml` push lane, with a
dated note explaining the prior claim was false against the live tree
(D2-4's own finding). No other script changes — the check's own logic
(the three exit codes, the coordinate enumeration, the sha256
comparison) is byte-for-byte unchanged.

**AR-CA-6 (Makefile):** already correctly wired (`test:` target, lines
38-41) — verified, disclosed below, changed nothing.

### The witness pair (AR-CA-2)

Trip at `2088763~1` (the commit immediately before the fix — judge's
own `pairing_conviction_test.clj` requires `ehrt.judge-v2-nist.interface`
directly, but `projects/integration/deps.edn` had dropped
`poly/judge-v2-nist` on 2026-07-31), a disposable `git worktree` with
the new gate test file copied in at the same relative path:

```
Testing ehrt.docs-tooling.project-classpath-test

FAIL in (every-project-composed-test-tree-requires-only-documented-bricks-test) (project_classpath_test.clj:154)
test-tree require(s) naming a brick absent from the composing project's own deps.edn/:necessary list -- the `2088763` classpath-break class:
  components/judge/test/ehrt/judge/pairing_conviction_test.clj requires ehrt.judge-v2-nist.interface (brick judge-v2-nist), but project integration does not compose it
expected: (empty? violations)
  actual: (not (empty? ({:project "integration", :test-file "components/judge/test/ehrt/judge/pairing_conviction_test.clj", :required-ns "ehrt.judge-v2-nist.interface", :owning-brick "judge-v2-nist"})))

Ran 5 tests containing 8 assertions.
1 failures, 0 errors.
```

Exactly the one predicted violation — judge / `ehrt.judge-v2-nist` /
`integration` — and nothing else; the historical window held no
undisclosed second break.

Clean at HEAD (`7234f8c`, this session's own opening tip):

```
Testing ehrt.docs-tooling.project-classpath-test

Ran 5 tests containing 8 assertions.
0 failures, 0 errors.
```

### The exit-2/exit-0 proofs (AR-CA-4)

Scratch scenario (`--repo` pointed at a freshly-created empty
directory — no jars resolved yet):

```
not yet resolved -- run a full build first (missing from <scratch-dir>):
  nist-hl7-v2-parser (gov/nist/hl7-v2-parser/1.7.3/hl7-v2-parser-1.7.3.jar)
  nist-hl7-v2-profile (gov/nist/hl7-v2-profile/1.7.3/hl7-v2-profile-1.7.3.jar)
  nist-hl7-v2-validation (gov/nist/hl7-v2-validation/1.7.3/hl7-v2-validation-1.7.3.jar)
  nist-xml-util (gov/nist/xml-util/2.1.0/xml-util-2.1.0.jar)
  nist-hl7-v2-schemas (gov/nist/hit/hl7-v2-schemas/1.7.2/hl7-v2-schemas-1.7.2.jar)
  nist-validation-report (com/github/hl7-tools/validation-report/1.2.0/validation-report-1.2.0.jar)
```

Exit code: 2.

Real local repo (`~/.m2/repository`, default):

```
OK: 6 hit-nexus-sourced coordinate(s) match artifacts.lock.edn exactly
  nist-hl7-v2-parser
  nist-hl7-v2-profile
  nist-hl7-v2-validation
  nist-xml-util
  nist-hl7-v2-schemas
  nist-validation-report
```

Exit code: 0.

The check genuinely trips when reality diverges from
`artifacts.lock.edn` (missing coordinates, exit 2) and genuinely
passes when it doesn't (all six matched, exit 0) — not merely
plausible from reading the script.

### The header correction (AR-CA-5)

Before (line 24, the false claim D2-4 caught):

```
# Wired into `make test` (the per-push lane) right after
```

After:

```
# Wired into two surfaces, both right after
# `clojure -M:poly test :all skip:integration`, ...
#   - `make test`, the local convenience target (Makefile)
#   - `test.yml`, the per-push lane every push and PR actually runs
#     (ADR-0095, cluster A / D2-4: this script's own header previously
#     claimed the `make test` wiring alone was the per-push lane, which
#     was false -- `test.yml` never calls `make test`, it inlines the
#     `poly` steps directly, so this check ran nowhere routinely
#     watched until this step was added)
```

The check's own logic (three exit codes, coordinate enumeration,
sha256 comparison) is byte-for-byte unchanged — only the header
comment lines moved, per this session's own fence.

### The Makefile (AR-CA-6, disclosure only)

`Makefile` lines 38-41, unchanged:

```
test:
	clojure -M:poly check
	clojure -M:poly test :all skip:integration
	bin/verify-nist-lock
```

Already correctly wired since before this session opened — verified
live, changed nothing.

### Verification

- `clojure -M:poly check`: OK, both at Step 0 and after the new gate
  test file landed.
- Oracle pre-digest (`bin/regression-oracle 7234f8c 7234f8c`): all
  THIRTY-FOUR roots IDENTICAL, the expected trivial tip-against-itself
  result.
- Oracle bracket over this session's own in-flight changes (`bin/
  regression-oracle 7234f8c <wip-commit-object>`, captured via `git
  stash create` into a dangling commit object ahead of this ADR's own
  commit, per this session's own no-commit-until-Step-4 ordering): all
  THIRTY-FOUR roots IDENTICAL — PURE IDENTITY, as predicted (this
  session touches CI wiring, a docs-tooling gate, and a script header
  comment only — no sim/engine-path code).
- `ehrt.docs-tooling.project-classpath-test`: 5 tests, 8 assertions, 0
  failures, 0 errors, at HEAD (witness pair above).
- Full local suite (`clojure -M:poly test :all skip:integration`): 295
  namespace test blocks, 0 failures, 0 errors anywhere (grepped the
  entire log for any nonzero failure/error count — none found); the
  new gate confirmed running twice in this run, once per project that
  composes `docs-tooling` (`development`, `conformance`).
- Last five `test`-lane runs (`gh run list --limit 5 --branch main`),
  checked at Step 0: all green.
- `gitleaks git --staged -v`: clean at this commit.
- Tag verification: `stable-20260809-census-closure-file-count` (this
  session's own Step 0, the successor tag debt ADR-0094 named) tagged
  at `7234f8c`, pushed, peeled ref resolves to
  `7234f8c853885bd5118447e2c261f862d1b24c0a` exactly.
- Post-push message verification and the ASCII check: run against the
  landing commit before push completion is claimed (see this session's
  own record for the exact SHA and transcript).
- CI watched to conclusion: this session's own prompt named the push
  lane run as AR-CA-3's own first live witness — the new step's
  presence and conclusion are quoted in the session record.
- `git status --porcelain`: clean before this session's first tool
  call.

### Fences

No CLI src anywhere. No census, sim, judge, or engine src touched.
Workflow edits: the one `test.yml` step, nothing else in any workflow.
No Makefile changes. `bin/verify-nist-lock`: the header comment lines
only, no logic change. New test file + the one workflow step + the
header lines + ceremony surfaces are this session's entire footprint.
No live violation found at HEAD (only the historical `2088763~1`
witness tripped) — nothing to fix forward under this session's own
charter.

### This session's own successor tag debt

The next session that opens fresh work tags
`stable-20260809-cluster-a-gate-wiring` at THIS session's own closing
tip, under standing ceremony — the tag-law case (ii) pattern.

### Index line

```
- 2026-08-09 — cluster-a-gate-wiring — ADR-0095
```

(appended to `.agents/plans/roadmap.md`'s own Done section; cluster A
was carried only in horizon notes, never its own `Next` row, so there
is no Next-row removal to pair with this pointer.)

`notes/adr/README.md`'s own file count corrects 92→93, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.

### The horizon, restated

Untouched, carried forward from ADR-0094: fix cluster B in full
(D4-5/D4-6/D4-7/D8-3, the CLI parse-guard family), cluster C's
ride-along fixes (D8-7, D8-4, D7-7/D7-8), ruling 1's own unruled option
(b), the oracle's own blind-spot intake (H-3), the two remaining
`defspec` flake watch items (D3-2), the ADR-footnote-fork backlog row
(D7-14), `make quickstart`'s own untimed full run (D8-8), the two
deferred veteran modules under their true names, and publish-prep
Externals. What's new: this session's own successor tag debt (above);
cluster A itself is now CLOSED, not carried further. No other new
horizon items open by this session's own narrow scope.

### Consequence

Two "a check exists (or should exist) but doesn't run where it
matters" gaps close on the same evidence trail: a new static gate that
reproduces the exact historical incident it was built to catch (and
only that incident, confirmed by a clean run at both the trip point
and at HEAD), and a supply-chain integrity check restored to the lane
every push actually runs, proven to trip when reality diverges
(scratch exit 2) and pass when it doesn't (real repo exit 0). The
oracle holds pure identity across all 34 roots — this is tooling and
CI wiring, not sim/engine-path work.
