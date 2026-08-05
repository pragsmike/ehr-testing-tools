<!-- Attic file: notes/adr/0002-land-ehr-testing-tools.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0002 — Land `ehr-testing-tools`: components/tools + components/palgebra + bases/ehr-cli, close H1/H2/H3

**Status:** Accepted, 2026-07-28 (author-ratified; AskUserQuestion checkpoints
on Step 1's git delegation, Makefile disposition, and skills disposition were
each answered before the corresponding work proceeded).

### Context

`ehr-testing-tools` was frozen at `stable-pre-monorepo` (`f848b67`,
post source-sink SS-4b) pending exactly this session (ADR-0001 H2).
This record closes H1 (source/sink component shape), H2 (the tools
landing itself), and H3 (the conformance project) — the last of which
turned out to need no prior judge extraction, contrary to H3's own
original phrasing.

### Decision

**Settled rulings (author-directed, verbatim from this session's own
prompt, continuing ADR-0001's numbering):**

- **R11.** Tools lands frozen at `stable-pre-monorepo`. Its open design
  ledger migrates into this workspace's registers as named entries
  with origin IDs (below), worked in no future session until named.
- **R12.** H1 CLOSED: one component, internal polymorphism. Recorded
  trigger for revisiting: SS-5 (blaze) landing with an HTTP client
  dependency enters as a `blaze-api` wrapper component corpus
  requires — a dependency-isolation component, not an alternate
  interface implementation.
- **R13.** Landing shape: thin `bases/ehr-cli` (`cli.clj` + `cli/help.clj`
  only, renamed `core.clj`/`help.clj`) + fat `components/tools`
  (everything else) + `components/palgebra` carved immediately — probe
  evidence (research-agent census, this session) showed it
  self-contained: no real `:require` of `ehr-testing-tools.*` anywhere
  under `palgebra/`, only text (docstrings, prose, a seeded-violation
  test fixture — 6 files total, not "enforcement strings and one
  docstring provenance note" as the prompt characterized it; see the
  deviation record).
- **R14.** Judge and corpus are NOT extracted this session — a shared
  root layer (`result`/`digest`/`canonical`/`artifact`/`lineage`/
  `locator`/`invocation`) sits beneath both, and naming a foundation
  component is a future, ruled design decision. The prompt's own
  premise that "corpus requires judge, never the reverse" does not
  hold against the live tree (see the deviation record) — the actual
  arrow is flat: neither requires the other as a code dependency,
  corpus just consumes a strictly larger subset of the shared
  foundation than judge does.
- **R15.** H3 CLOSED: `test-integration/` becomes `projects/conformance`
  — base-less, composing `components/sim` + `components/tools` (+
  `components/palgebra`, workspace.edn `:necessary` override — see
  below). The harness's sim invocation was already subprocess-only
  (`notes/tools/ADRs.md` ADR-0013) and needed no repointing at all — the
  "sanctioned adaptation" R15 anticipated turned out to be a non-event;
  `notes/tools/ADRs.md` ADR-0013's own subprocess-only rule is *why*
  `poly/sim` does **not** appear in `projects/conformance/deps.edn` (a
  `poly check` warning 207 initially flagged this correctly; see
  deviation record).
- **R16.** Palgebra's `deps-lint` enforces the renamed rule
  (`ehrt.palgebra.*` never requires `ehrt.tools.*` or `ehrt.sim.*`),
  KEPT (`components/palgebra/src/ehrt/palgebra/deps_lint.clj`).
- **R17.** Library version skew resolved by converging on the newer pin
  (Clojure 1.12.5, tools' own pin, over sim/root's 1.12.3) everywhere —
  root `deps.edn`, `projects/sim/deps.edn`, `projects/tools-cli/deps.edn`,
  `projects/conformance/deps.edn` — no test failed under 1.12.5.

**Landing shape, as built:**

```
components/tools/    -- fat: src/ehrt/tools/* (everything from
                        src/ehr_testing_tools except cli.clj/cli/),
                        test/ehrt/tools/*, docs/ (tools' docs tree,
                        intact), deps.edn (incl. HAPI FHIR/v2, R17),
                        interface.clj (NEW — wide delegation)
components/palgebra/ -- src/ehrt/palgebra/* (was palgebra/src/palgebra),
                        test/ehrt/palgebra/*, examples/, tools/,
                        HISTORY.md, deps.edn, interface.clj (NEW —
                        not named in R13's own layout, but required the
                        moment palgebra became its own brick; see
                        deviation record)
bases/ehr-cli/       -- src/ehrt/ehr_cli/{core.clj (was cli.clj),
                        help.clj (was cli/help.clj)}, test/, deps.edn,
                        README.md (was tools' own root README.md,
                        relocated here — see deviation record)
projects/tools-cli/  -- composes tools + palgebra + ehr-cli; :run,
                        :coverage aliases (ported, ADR-0004 posture:
                        measure and report, no --fail-threshold)
projects/conformance/ -- base-less; composes sim + tools + palgebra
                        (test-only); test/ (was test-integration/)
```

Mechanical rename, total: `ehr-testing-tools.X` → `ehrt.tools.X`,
`palgebra.X` → `ehrt.palgebra.X`, `ehr-testing-tools.cli`/`.cli.help` →
`ehrt.ehr-cli.core`/`.help`. Every `ns` form, every `require`, every
string reference (deps.edn paths, `docs/*` literal-path defaults,
`bin/*` scripts). Grep for both old roots afterward: zero hits outside
`notes/tools/` provenance, dated errata, and two deliberately-unchanged
literals (`~/.cache/ehr-testing-tools/artifacts` — a real, external,
user-visible cache-directory name and its `EHR_TESTING_TOOLS_CACHE`
env-var override, left unchanged since renaming it would silently
orphan any user's already-fetched cache for zero benefit; four
`{:name "ehr-testing-tools" ...}` test-fixture literals in
`corpus/{intake,operation_manifest,sink_composability,sink_write}_test.clj`,
arbitrary example data for schema validation, not tied to the real
`mutate-producer` identity function — which *was* renamed, to
`"ehrt.tools"`, matching sim's own `:generator :name` precedent, along
with the one test asserting against it).

`ehrt.tools.interface`: wide delegation, export set determined by grep
against `bases/ehr-cli` and `projects/conformance/test` — same
methodology as `ehrt.sim.interface` (ADR-0001 R5). Five short names
collided across two source namespaces each (`gate-file`/`gate-dir` in
both `judge.fhir` and `judge.v2`; `lookup`/`register!` in both
`corpus.operators` and `corpus.generators`; `valid?` in both
`judge.report` and `result`; `resolve!` in both
`corpus.generator-source` and `corpus.spool-source`) — each pair
qualified (`fhir-`/`v2-`, `generators-` prefix, `report-` prefix,
`spool-` prefix) rather than picking one winner silently, every caller
of the qualified half updated at its call site. Two more names
(`resolve`, `run!`) didn't collide with each other but each shadowed a
`clojure.core` name — loads with a `WARNING` printed on every
namespace load, which is cosmetic under `poly test` but became a real
`Syntax error compiling ... No such var: artifact/resolve` failure
under at least one real subprocess invocation of `bin/ehr`
(`stdin_intake_real_pipe_test.clj`, caught by `poly test :project`
before it caught by `poly test :all` — a difference in which specific
namespaces get compiled together first). Qualified to
`resolve-artifact`/`sim-run!` instead. `ehrt.palgebra.interface`:
same treatment, no collisions, no caller-name qualification needed.

**Verification method:** `clojure -M:poly check` — zero errors (two
warning-207 false-positives suppressed via `workspace.edn`'s
`:necessary`, one true-positive fixed by removing `poly/sim` from
`projects/conformance/deps.edn` per R15/`notes/tools/ADRs.md` ADR-0013).
`clojure -M:poly
test :all` and `clojure -M:poly test :project` — 131 test namespaces
each run, 0 failures/0 errors both times, including the real,
non-mocked `projects/conformance` suite against the actual
`../ehr-testing-sim` sibling checkout present on this machine (real
subprocess HL7 v2 generation and gating, zero baseline drift against
the committed `sim-v2-gate-baseline.edn`/`sim-v2-full-capability-baseline.edn`).
CLI smoke: `ehr help`, `ehr corpus operators`, and `ehr corpus
operators --format v2` (the one documented use-case strip), all
byte-identical against the same invocations run against the pre-merge
`ehr-testing-tools` clone at `stable-pre-monorepo`; `projects/sim`'s
own CLI smoke (`clojure -M:run run --seed 42 ...`) unchanged, exit 0.
`clojure -M:poly diff since:stable-bootstrap` recorded: tools-origin
additions plus the wiring edits this record names, satisfying P3.

**Migrated ledger, named with origin IDs (R11) — not worked this
session:**

- **tools/OPEN-4** — whether `corpus generate` grows an `--engine` flag
  now that the generator registry names more than one engine kind
  (`docs/source-sink-design.md`).
- **tools/OPEN-5** — whether a `dir:` Source ever grows
  framing-awareness for directories containing multi-item files
  (`docs/source-sink-design.md`).
- **tools/OPEN-6** — dir-sink `:append` catalog/manifest merge
  semantics, `:append-unsound` rejected unconditionally for now
  (`docs/source-sink-design.md`; D-d's 2026-07-28 partial resolution
  noted inline there).
- **tools/D-b** — whether the `blaze` sink lands before or after the
  IG-pinning blocker clears (`docs/source-sink-design.md`).
- **tools/SS-5** — the `blaze` sink build session itself, not yet run;
  recorded trigger for reopening H1 (R12, above).
- **tools/lineage-duplication** — `lineage/*.lineage.edn` and
  `operation-manifest.edn`'s own `:items` duplicate the same
  input-hash/parent facts; named as a finding, not consolidated
  (`.agents/plans/corpus-foundations.md`'s SS-4b row, ruling 8; now
  `notes/tools/agents/plans/corpus-foundations.md`).
- **tools/H5** — published-artifact coordinates (Clojars vs. Maven
  Central) — same item as this workspace's own H5, author's call.
- **tools/NIST-reply** — the residual NIST HL7 v2 validator licensing
  inquiry (`docs/experiments/EXP-SBOM-inquiry-draft.md`), narrowing in
  progress, nothing shipped depends on its answer.
- **tools/SETUP-unspoiled-walk** — a fresh-clone, no-prior-context
  walkthrough of `SETUP.md` has not been executed against the new
  Polylith layout; `bin/ehr`'s repo-root assumption and the artifact
  cache path are the two things most likely to need a doc fix once
  someone actually does this.
- **tools/sim-JDK-21-errand** — carried forward from `tools/ADR-0001`'s
  own deviation record (Temurin-vs-stock-OpenJDK-21 characterization);
  this session's own environment probe (below) hit the same class of
  finding independently, on a different axis.

**Status.** Accepted (author-directed), 2026-07-28.

### Deviation record

**Environment stanza (step 0).** Native Windows Git Bash has no JDK 21
(`java -version` → `1.8.0_311`, Oracle) and no SSH credentials for
either remote — `clojure -M:poly check` fails outright
(`Unrecognized option: --enable-native-access=ALL-UNNAMED`) and
`git ls-remote`/clone-based verification is impossible from that
shell. WSL2 Ubuntu (`wsl -e bash -lc "..."`) has the correct
toolchain (`openjdk 21.0.7`, `poly 0.3.32`, `git config core.hooksPath`
already `.githooks`) and matches `tools/ADR-0001`'s own environment
stanza exactly. All git operations and all `clojure`/`poly` invocations
this session ran through WSL for this reason, per AGENTS.md's own
WSL-only rule — not a new finding, but this session's own first-hand
confirmation of it.

**Step 1 delegation.** Per ADR-0001 R6, git commit/merge is the
author's ceremony by default. Asked explicitly, in this session's own
chat: author delegated the Step 1 history-merge git surgery (throwaway
clone, `.staging/` stage, `git merge --allow-unrelated-histories`) to
the session, for this session only — mirroring the bootstrap session's
own "session-level delegation note." File-list parity against
`stable-pre-monorepo` verified via `diff` of two sorted file listings:
exact match.

**R13's own "one docstring provenance note" claim was imprecise.** A
research-agent census (this session) found `palgebra/`'s actual
textual references to `ehr-testing-tools` were: two docstring
provenance notes in `signature.clj` (not one), three prose mentions in
`HISTORY.md`, and a seeded-violation test fixture in
`deps_lint_test.clj` spelling `ehr-testing-tools.lint` as a string to
prove the linter fires — six files, three categories (docstring,
prose, test fixture), not two. The underlying claim palgebra is safe
to extract day-one — *is* confirmed (zero real `:require` of
`ehr-testing-tools.*` anywhere under `palgebra/`), so R13's decision
stands; only its own supporting count was off, corrected here per
fix-forward-with-disclosure rather than silently repeated.

**R14's own "corpus requires judge, never the reverse" premise did not
hold.** The same census found **zero** cross-namespace `:require`
between `ehr-testing-tools.judge.*` and `ehr-testing-tools.corpus.*` in
either direction — the one textual hit (`corpus/operators.clj:25`, a
docstring comment naming `ehr-testing-tools.judge.v2`) is prose, not a
dependency. R14's own actionable content for this session ("not
extracted this session") is unaffected either way; the premise about
*which one would extract first* is recorded here as corrected, not
silently acted on, since a future foundation-extraction session should
not inherit a false premise about the current arrow.

**Palgebra needed an `interface.clj` R13 never named.** `poly check`
enforces (not merely documents) that a brick reaching into another
brick's non-interface namespace fails — and `components/tools`
genuinely requires `palgebra.lint`/`palgebra.signature` (two real
`:require` sites, `lint.clj`/`pipeline.clj`). R13's own target-layout
diagram for `components/palgebra` listed no `interface.clj`. Resolved
by adding one, same wide-delegation methodology as
`ehrt.tools.interface`, no collisions. Not a design decision requiring
a live ask — Polylith's own enforcement left no alternative once
palgebra became a separate brick.

**`docsgen.clj`'s cli.md renderer inverted the dependency direction.**
`components/tools/src/ehrt/tools/docsgen.clj` (pre-carve:
`src/ehr_testing_tools/docsgen.clj`) required `cli.help` directly (for
`cli-spec`, to render `docs/cli.md`) — legal when both lived in one
src tree, illegal once `cli.help` moved into `bases/ehr-cli` (Polylith:
bases depend on components, never the reverse). `render-cli-md` itself
was already pure (spec passed as a plain argument, no closure over
`help`); only the `-X`-invokable `write-cli-md!` wrapper and the test
file's fixture data closed over the real `help/cli-spec`. `write-cli-md!`
had no live caller left regardless (the Makefile's `cli-doc` target
did not survive, below) — parameterized to accept `spec` explicitly,
`help` require dropped from `docsgen.clj` entirely.
`docsgen_test.clj`'s seven cli.md tests were rewritten against a small,
locally-defined representative `test-cli-spec` fixture (same shape,
synthetic data) rather than moved into `bases/ehr-cli` — the tests'
own docstring already states the real spec's freshness against
`docs/cli.md` is CI's job (regenerate + `git diff --exit-code`), not
this unit suite's; a synthetic fixture proves the renderer doesn't
silently drop data just as well, without a cross-brick reference.
`ehrt.tools.check.Assertion`/`corpus.manifest.ManifestV1_1` and a
handful of other product-level path literals had matching cwd-relative
fixes (`docs/pipeline.edn` → `components/tools/docs/pipeline.edn`,
etc. — see below); this one alone required a real code restructuring,
not just a path-string edit, so it is called out on its own.

**`quickstart_fresh.clj` depended on tools' own root `README.md`,
which had already been deleted as "superseded."** Root docs
(`AGENTS.md`, `AUTHORS-GUIDE.md`, `CLAUDE.md`, `LICENSE`, `Makefile`,
`README.md`, `SETUP.md`) were dropped following the bootstrap session's
own sim precedent (superseded by the workspace's own versions,
reachable via the merge commit) — but unlike sim, tools shipped a real,
enforced test (`ehrt.tools.quickstart-fresh-test`'s
`committed-readme-and-script-agree-test`) asserting `README.md`'s
Quickstart fence and `bin/quickstart-demo` teach the identical commands
in the identical order. Deleting the README silently zeroed both sides
of that comparison (0 commands found either side) rather than failing
loudly — caught only by actually running `poly test :all`, not by any
static check. Resolved: tools' own root `README.md` (recovered from the
pre-merge clone) relocated to `bases/ehr-cli/README.md` — a natural
home, since the whole Quickstart is about invoking the `ehr` CLI —
internal doc links repointed at their new `components/tools/docs/*`
and `notes/tools/*` locations (best-effort; not every link was
independently re-verified to resolve, only the Quickstart fence itself,
which the test enforces). `quickstart_fresh.clj`'s own
`:readme-path` default updated to match. `bin/quickstart-demo`'s own
`make test` line updated to `clojure -M:poly test :all` (the Makefile
did not survive; see below) — this required updating the README's
matching taught line too, since the freshness test compares them
verbatim.

**cwd-relative literal paths, a real (not just cosmetic) tools-vs-sim
convention difference.** Unlike sim (which resolves fixtures via
`io/resource`, classpath-relative), tools resolves `artifacts.lock.edn`,
`config/synthea/synthea.properties`, `resources/synthea-default.properties`,
and every `test/fixtures/**`/`test-integration/fixtures/**` path via
plain `slurp`/`io/file` on a literal string — resolved against the JVM
process's actual working directory, confirmed empirically (a
throwaway probe test asserting `(System/getProperty "user.dir")`
during `clojure -M:poly test :project`: always the workspace root,
never a project or component subdirectory). Given that, the correct,
lowest-risk fix was **not** to convert these to `io/resource` (ADR-0005's
own artifact-registry/config design is legitimately about real,
user-editable filesystem paths, not bundled resources — converting
would be a wrong fix, not a conservative one) but to place the actual
files at the workspace root, unchanged relative paths, zero code
edits: `artifacts.lock.edn`, `config/synthea/synthea.properties`,
`resources/synthea-default.properties`, `test/fixtures/**`,
`test-integration/fixtures/**` (the last one keeping its
pre-carve directory name deliberately, since the 3 conformance test
files hardcoding it were not rewritten). `bin/ehr` was repointed to
`cd` to the workspace root (not a project subdirectory) before
`exec`ing, and root `deps.edn` gained an `:ehr` alias
(`poly/tools`/`poly/palgebra`/`poly/ehr-cli` local-roots, `-m
ehrt.ehr-cli.core`) so that cwd requirement and the classpath
requirement are satisfied by the same invocation. `docs/*`-referencing
literals (`pipeline.edn`, `use-cases.edn`, `signature.edn`,
`EXP-C5-results.md` citation) and `lint.clj`'s own `deps.edn`
dependency-coordinate check were each repointed to their real new
location (`components/tools/docs/*`, `components/tools/deps.edn`)
instead, since those *are* component-owned and moving with the
component was the correct fix.

**Three more census false positives, caught only by actually compiling
and running the suite.** `ehrt.tools.interface`'s first draft included
`mutate-to-stdout!` (actually a *private* helper defined inside
`bases/ehr-cli`'s own `core.clj`, matched by a docstring's prose
slash), `generate/invocation` and `generate/operators` (both docstring
prose, "corpus generate/operators don't" / "corpus.generate/invocation"),
`manifest/MirroredManifest` (a different repo's namespace,
`ehr-testing-sim.manifest/MirroredManifest`, named in a docstring),
and `mutate/mutate-v2` (genuinely private, `defn-`, never called
externally — the one census hit was prose too; Clojure's compiler
*does* reject a `def`-level alias of a private var, `IllegalStateException:
var ... is not public`, even though a plain function call through a
fully-qualified private-var symbol compiles fine — a distinction this
session learned the hard way). None of these five ever had a real
caller; all five were grep false positives from prose containing a
`namespace/symbol`-shaped substring. Removed from the interface
without loss — nothing required them.

**Makefile dropped, not ported (asked, not guessed).** Tools'
`Makefile` (pack/pack-skills/pack-push, pipeline/use-cases/
operators-doc/cli-doc docsgen targets, lint-pipeline/lint-deps,
check-palgebra-drift, test/coverage/integration/integration-smoke)
was superseded wholesale per the author's own choice (offered as an
explicit either/or): none of its targets are named in R11–R17, and
porting them as poly aliases would have been scope creep on this
session. `bin/ehr`/`bin/check-palgebra-drift`/`bin/quickstart-demo`
were kept and repointed (their own capability — the CLI wrapper, the
palgebra/sim drift check, the quickstart harness — is exercised by
real tests, unlike the Makefile's doc-regeneration targets, which
have no poly-era equivalent yet and are not blocking anything this
session's own gate checks).

**Skills: provenance only, not brought live (asked, not guessed).**
Tools' 8 skills not already in the workspace (`committee`,
`find-skills`, `probe`, `repo-adaptation`, `review`, `scenarios`,
`shared-skill-layout`, `wsl-windows-git-hygiene`) plus its own
differently-adapted `string-diagram`/`committee` copies moved to
`notes/tools/agents/skills/` as historical record — matching how
sim's own duplicate skills were dropped at bootstrap. The workspace's
live `.agents/skills/string-diagram/SKILL.md` carries an
`ehr-testing-sim`-era vendoring pin (commit `7ecce38c...`) that now
looks stale once palgebra's own authoritative copy lives in this same
workspace (`components/palgebra/tools/resource_equations_to_mermaid.py`)
— left as a named, disclosed gap for a future session's deliberate
call, not silently "fixed" by repinning it here. `handoff` is
byte-identical between the two repos; no action needed.

**`bin/check-palgebra-drift`'s own comparison target moved.** Its
hardcoded `palgebra/tools/...`/`palgebra/examples/...` paths (comparing
against `ehr-testing-sim`'s vendored copies) repointed to
`components/palgebra/tools/...`/`components/palgebra/examples/...`; the
sim-side comparison paths are unaffected (sim didn't move this
session).

### Erratum (2026-07-28, discipline-parity session — fix-forward, dated, not a revert)

**Correcting a mischaracterization of this ADR, not a mischaracterization
this ADR itself made.** The discipline-parity session's own prompt
described this record as having claimed fixture bytes "moved to brick
resources with `io/resource` conversion." Re-read directly: this ADR
never claimed that. Its own "cwd-relative literal paths, a real (not
just cosmetic) tools-vs-sim convention difference" section says the
opposite, explicitly and by name: *"the correct, lowest-risk fix was
**not** to convert these to `io/resource`... but to place the actual
files at the workspace root, unchanged relative paths, zero code
edits."* That decision was deliberate, disclosed, and — per this same
erratum — still correct in its core judgment (below). No correction to
this ADR's own factual content follows from that mischaracterization;
it's recorded here only so a future reader hitting the same claim
doesn't waste time hunting for a passage that isn't there.

**What *was* real, found by this same session's own audit
(`notes/discipline-parity-audit.md` row M24):** the fixture directories
this ADR placed at the workspace root (`test/`, `test-integration/`)
were never relocated under an owning brick — genuine top-level
untidiness, unrelated to the `io/resource` question. The
discipline-parity session relocated them
(`components/tools/test-fixtures/`, `projects/conformance/test-fixtures/`)
while keeping this ADR's own cwd-relative-literal-path design
unchanged — re-examined, not merely carried over: roughly a third of
the call sites across the 11 consuming test files (concentrated in
`bases/ehr-cli/test/ehrt/ehr_cli/core_test.clj`) pass the fixture path
to real CLI dispatch code (`gate/gate-file`, `cli/gate-v2-command`) as
the literal filesystem path a user would type — these are testing real
path-handling behavior, not fixture lookup, and would have broken under
an `io/resource` conversion regardless of brick placement. This ADR's
original choice holds for the reason it originally gave, confirmed
against a wider set of call sites than the original decision inspected.

**A second, unrelated gap the same relocation surfaced:** tools' own
pre-carve `.gitattributes` carried `-text` overrides protecting
`test/fixtures/v2/*.hl7` and `test/fixtures/v2/simhospital/messages.out`
from line-ending normalization (the same class of hazard ADR-0001's own
deviation record found and fixed for `components/sim/docs/demos/`).
Neither override was ported during this ADR's own carve. The vendored
corpus bytes were checked against their recorded sha256
(`fa9719a5f157391dcf78197e4239bce8af0382ae40b903d019a2773a1a9ff520`) at
relocation time and found intact — not corrupted in the interim — but
the gap was real and is now closed at the fixtures' new location
(`.gitattributes`, discipline-parity session).

---

