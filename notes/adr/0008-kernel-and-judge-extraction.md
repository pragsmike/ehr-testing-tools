<!-- Attic file: notes/adr/0008-kernel-and-judge-extraction.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0008 — Kernel and judge extraction: ADR-0002 R14 (named hole H4) closed

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-29.

### Context

ADR-0002 R14 left `components/tools`' judge and corpus code sharing an
unnamed foundation layer (`result`/`digest`/`canonical`/`artifact`/
`lineage`/`locator`/`invocation`), deliberately not extracted at H2
landing time, naming a future foundation component as a design
decision for a later, ruled session. This session's own prompt (R31,
R36) is that ruling: name the foundation component `kernel`, extract
`judge` alongside it, and determine kernel's actual membership by
census against the live require graph rather than by assuming the
prompt's own "expected kernel set" is correct.

### Decision

**R36's rule, applied.** A root-layer `ehrt.tools.*` namespace (not
already inside `corpus/` or `judge/`) moves to **kernel** if the
census shows it required by two or more of {judge, corpus, cli,
conformance} (cli/conformance counted transitively, via what
`ehrt.tools.interface` actually re-exports and what callers of that
re-export actually invoke -- the only way either brick touches this
component at all); it moves to **judge** if used only by judge; it
stays in **tools** otherwise. `judge`'s own five `ehrt.tools.judge.*`
namespaces move to `ehrt.judge.*` wholesale, per the prompt's own
explicit instruction, not by census.

**Census table** (every root-layer namespace, its real callers, and
its disposition):

| Namespace | Callers found (real `:require`, not prose) | Disposition |
|---|---|---|
| `result` | judge (fhir, v2), corpus (8 files), cli (via interface) | **kernel** |
| `digest` | judge (fhir, v2, verdict-cache), corpus (5 files), cli (via interface) | **kernel** |
| `artifact` | judge (fhir), corpus (generate), cli (via interface) | **kernel** |
| `invocation` | judge (fhir), corpus (generate) | **kernel** |
| `canonical` | corpus (canonicalizers), tools' own `check.clj` + `lint.clj` (→ cli via interface) | **kernel** |
| `locator` | corpus (mutate), tools' own `check.clj` + `interface.clj` (→ cli) | **kernel** |
| `lineage` | corpus (`mutate.clj`) only -- **census surprise**, see below | **tools** (stays) |
| `check` / `check.schemas` | cli only (via interface), 1 of 4 | **tools** (stays) |
| `diff` | `check.clj` only (tools-internal) | **tools** (stays) |
| `lint` | tools-internal (deps-lint tool) | **tools** (stays) |
| `pipeline`, `docsgen`, `usecases`, `quickstart-fresh` | tools-internal / cli (docsgen only) | **tools** (stays) |
| `sim` | corpus (generators) + cli (via interface) -- **meets the 2-of-4 test** but R37 names it explicitly as staying in the residual tools component (the sim adapter) | **tools** (stays, R37 override) |
| `judge.{fhir,v2,report,finding,verdict-cache}` | named by the prompt, not census | **judge** |

**The "expected kernel set" named seven members
(`result`/`digest`/`canonical`/`artifact`/`lineage`/`locator`/
`invocation`); the census confirms six and contradicts one.**
`lineage` has exactly one real caller, `corpus/mutate.clj` -- not
judge, not a second corpus consumer, not cli. R36's own fallback rule
("used only by judge → judge; otherwise it stays in tools") applies
literally: `lineage` is used only by corpus, so it stays in tools.
This is the census doing the job the prompt's own "(verify, don't
assume)" parenthetical asked of it -- recorded here as a finding, not
silently reconciled to match the expected list.

**Interfaces sized by the H2 method (grep of actual external
callers), not copied wholesale.** `ehrt.kernel.interface` exports 23
names -- the full public API of `result` and `digest` (every function
in both is called externally), a proper subset of `artifact` (5 of
8 -- `env-override`/`cache-dir`/`default-downloader!`/`extracted-dir`/
`default-extractor!` have no caller outside `ehrt.kernel.artifact`
itself and stay unexported), a proper subset of `canonical` (3 of 6),
all of `locator`'s externally-called surface, and one of
`invocation`'s three functions. `ehrt.judge.interface` exports the
same eleven names `ehrt.tools.interface` already re-exported
pre-extraction, plus `finding-valid?` (a genuine external caller this
session's own census turned up in `check_test.clj` that ADR-0002's
original interface sizing missed, since that census only grepped
`bases/ehr-cli` and `projects/conformance/test`, not `components/tools`'
own test tree against namespaces about to leave the component).

**Collisions, resolved the same way ADR-0002 resolved them the first
time -- qualify both, pick no silent winner.** `artifact/resolve`
shadows `clojure.core/resolve`; exported as `resolve-artifact`
(matching `ehrt.tools.interface`'s own pre-extraction precedent
exactly). `invocation/run!` shadows `clojure.core/run!` -- caught only
by this session's own verification run (a `WARNING` on namespace load,
not a static-review finding), exported as `run-invocation!`, borrowing
`corpus.generate`'s own existing `:run-invocation` injection-seam name
rather than inventing a new one. `judge.report/valid?` and
`judge.finding/valid?` collide with each other now that `result/valid?`
(their old collision partner, pre-extraction) has left this component
entirely -- both qualified from the start (`report-valid?`/
`finding-valid?`), since there is no unqualified winner to pick between
two siblings that both moved together.

**A cycle the census's own file scope initially missed.**
`components/tools/test/ehrt/tools/locators_doc_test.clj` pins example
locators from `docs/locators.md` against BOTH `ehrt.kernel.locator`'s
grammar and `ehrt.tools.corpus.er7`'s resolution, in one suite. Moving
it into `components/kernel/test/` (its first-pass destination, since
it tests `locator`) would have made kernel's own test suite depend on
`corpus.er7`, a tools-owned namespace -- backwards, kernel→tools,
against the very direction this extraction exists to enforce. Resolved
per the prompt's own instruction for exactly this situation ("resolve
by moving the offending namespace per R36's rule"): the test stays in
`components/tools/test/`, its own namespace unchanged
(`ehrt.tools.locators-doc-test`), consuming `ehrt.kernel.interface`
for the locator half like any other tools-side caller. Not a namespace
R36 governs directly, but the same principle -- direction over
convenience.

**HAPI FHIR/HL7v2 Maven coordinates moved to `components/judge/deps.edn`.**
Verified (grep, whole tree) that no `ca.uhn.*` class is `:import`ed
anywhere outside `judge/v2.clj` -- `corpus/operators.clj` and
`lint.clj`'s own mentions are prose/string-literal only, not real
imports. `hapi-fhir-base`/`hapi-fhir-structures-r4` are carried to
judge alongside `hapi-base`/`hapi-structures-v24` on the strength of
what they're *for* (judge.fhir's own EXP-B2 comment), even though
grep found no current `:import` of the FHIR half anywhere in this
workspace -- a pre-existing, undisturbed fact about this dependency's
own liveness, not something this session introduces or resolves.
`lint.clj`'s `verify-target-2` (the deps-lint mechanism checking a
catalytic resource's `deps.edn` coordinate actually resolves) had
`"components/tools/deps.edn"` hardcoded as the one file it would ever
check -- updated to search kernel's, judge's, and tools' own
`deps.edn` files, since the one target-2 entry that exists
(`hapi-hl7v2-dep`) just changed which of them is true.

**Dependency wiring lives at the project level, not the component
level, in this workspace's own established convention** (confirmed by
reading every existing `deps.edn` before touching any of them: no
component `deps.edn` anywhere in this workspace carries a `poly/X
:local/root` entry for a sibling brick -- only external Maven
coordinates; brick-to-brick wiring is done once per project/dev-alias,
flat, explicit). `poly/kernel` and `poly/judge` were added everywhere
`poly/tools` already appears (root `deps.edn` `:dev`/`:test`/`:ehr`,
`projects/tools-cli`, `projects/conformance`, `projects/integration`),
matching exactly how `poly/sim` was added in ADR-0005 for the same
structural reason.

**Verification.** `clojure -M:poly check`: green. `clojure -M:poly
deps`: `judge`→`kernel` only; `kernel`→nothing; `tools`→`{kernel,
judge, palgebra, sim}`; no arrow from judge or kernel back into tools,
none from kernel to judge; `sim`/`palgebra`/`ehr-cli`/`sim-cli`
unchanged -- the exact shape this record's own decision names.
`clojure -M:poly test :all skip:integration`: 0 failures/0 errors
(full run, ~12.5 minutes, HAPI FHIR/v2 classpath cold). Targeted
re-run of all eleven new kernel/judge test namespaces directly (not
just as part of `:all`): 169 tests, 431 assertions, 0 failures/0
errors, run twice -- once before and once after the `run!` →
`run-invocation!` rename, confirming the rename fixed the shadow
warning without changing behavior.

### Deviation record

**A commit-boundary-adjacent slip, self-caught before it reached a
commit.** ADR-0007's own edit (this same file) appended its new
section immediately after ADR-0006's closing deviation-record text,
which pushed a trailing "Self-archived to ..." line -- belonging to
the *prior* session's own record, not ADR-0007's -- to the end of the
file, after ADR-0007, reading as though ADR-0007 too had been
self-archived to a session prompt from the day before it was written.
Caught by re-reading the file's own tail before starting this record;
fixed by moving the line back to immediately follow ADR-0006's own
deviation record, before ADR-0007's heading. That commit had already
been pushed (`82e9154`) by the time this was caught -- fixed forward
in this same commit, not amended, per this repo's own no-rewrite
discipline.

**The lineage census surprise**, already recorded above as part of the
decision itself rather than deferred to this section, since it's load-
bearing to the disposition table, not incidental to it.

**The `run!`/`clojure.core/run!` shadow**, likewise recorded above --
a real, if cosmetic, finding this extraction's own verification step
caught that a purely-static review of the pre-extraction interface
would not have (the collision didn't exist before extraction, since
`invocation/run!` was never re-exported unqualified at the
`ehrt.tools.interface` layer).

**Prose staleness, named and left, not chased.** Several docstrings
across `components/tools/src` still say `ehrt.tools.locator`/
`ehrt.tools.canonical`/etc. in prose (not code) after this extraction
-- `corpus/er7.clj` and `corpus/operators.clj` had a few sed
false-positive corruptions where a blind pattern substitution touched
a *prose* mention using the exact `namespace/symbol` shape (not just a
free-text namespace reference), which were caught and fixed in this
same session (both now correctly cite `ehrt.kernel.locator`); broader
prose that never used the slash-qualified form (e.g. "ehrt.tools.locator's
v2 grammar supports...") was not swept -- same disclosed-gap posture
ADR-0005's own deviation record took for its five sim-consuming test
namespaces' stale prose, not a new precedent.

---

