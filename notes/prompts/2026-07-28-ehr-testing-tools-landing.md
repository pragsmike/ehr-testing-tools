# 2026-07-28 — H2: land ehr-testing-tools; close H1, H2, H3

Archived per this session's own step 7: the originating session prompt,
verbatim, with the deviation-record appendix appended below it. Per
`docs/way-of-working.md` §3, "prompts are provenance" — this workspace's
second entry in that convention (the first, the bootstrap session's own
prompt, lives at `notes/prompts/2026-07-28-ehr-testing-bootstrap-sim-landing.md`).

---

## Original prompt

### Context

The `ehr-testing` workspace stands at `stable-bootstrap`+2 (`8d1a49b`), with
`components/sim`, `bases/sim-cli`, `projects/sim` landed, verified, and
building green. This session lands `ehr-testing-tools` (frozen at
`f848b67`, post source-sink SS-4b) and closes three of ADR-0001's named
holes: H1 (source/sink component shape — resolved from the landed design,
see R12), H2 (the tools landing itself), and H3 (the conformance project,
which turns out to need no prior judge extraction).

The bootstrap session's prompt
(`notes/prompts/2026-07-28-ehr-testing-bootstrap-sim-landing.md`) is
precedent for all conventions not restated here: checkpoint/COMMIT
discipline, AUTHOR ACTION boundaries, deviation record, R6 (no agent git
operations), R10 (fix-forward: stop and ask on false premises).

### Preconditions (verify before step 0; stop if unmet)

P1. AUTHOR ACTION, done before session start: `ehr-testing-tools` tagged
`stable-pre-monorepo` at `f848b67` and the tag pushed. No tools sessions in
flight. P2. Workspace `main` == origin, clean tree, `clojure -M:poly check`
OK. P3. `clojure -M:poly diff` against `stable-bootstrap` shows only known
post-tag commits (H6 closure); after this session it must show only
tools-origin additions plus the wiring edits this prompt names.

### Read first

1. This prompt, fully.
2. Workspace `AGENTS.md`, `AUTHORS-GUIDE.md`, `notes/ADRs.md` (ADR-0001).
3. Tools' `AGENTS.md`, `notes/ADRs.md` (esp. ADR-0017, ADR-0020),
   `docs/source-sink-design.md`, `palgebra/` layout and
   `palgebra/src/palgebra/deps_lint.clj`.
4. `src/ehr_testing_tools/corpus/source_sink.clj` docstring — the H1
   evidence.
5. The bootstrap prompt (precedent, cited above).

### Author rulings (continuing ADR-0001's numbering)

R11. Tools lands frozen at `stable-pre-monorepo`. Its open design ledger
(lineage-duplication reconciliation, OPEN-4/5/6, SS-5/D-b, and the
author-only items: Clojars/H5, NIST reply, SETUP unspoiled walk, sim
JDK-21 errand) migrates into the workspace's registers as named entries
with their origin IDs. No ledger item is worked in this session. R12. H1
CLOSED: one component, internal polymorphism. The landed source/sink
design is a canonical map schema with open-set `:kind`, per-kind
constructors, and runtime selection via URL designators — all kinds must
coexist on one classpath, which composition-time interface swapping
cannot provide. No current kind carries a distinct third-party dependency.
Recorded trigger: when SS-5 (blaze) lands with an HTTP client dependency,
it enters as a `blaze-api` wrapper component that corpus requires — a
dependency-isolation component, not an alternate interface implementation.
R13. Landing shape: thin `bases/ehr-cli` (`src/ehr_testing_tools/cli/` and
`cli.clj` only) + fat `components/tools` (everything else) +
`components/palgebra` carved immediately. Palgebra qualifies for day-one
extraction because probe evidence shows it self-contained: its only
textual references to tools namespaces are its own direction-linter's
enforcement strings and one docstring provenance note. R14. Judge and
corpus are NOT extracted this session. The require census shows a shared
root layer (result, digest, canonical, artifact, lineage, locator,
invocation, ...) beneath both; extracting them requires naming a
foundation component — a ruled design decision, queued behind this
landing. Confirmed direction for that future session: corpus requires
judge, never the reverse; judge extracts first. R15. H3 CLOSED this
session: `test-integration/` becomes `projects/conformance` — a base-less
project composing `components/sim` + `components/tools`, running the
sim-harness, gate-loop, generator-source, manifest-contract, intake, and
loopback suites. The harness's sim invocation is repointed from an
external sim checkout to the workspace's own sim (in-process via
`ehrt.sim.interface` where the harness already calls a CLI-shaped entry,
else the project-classpath equivalent). This is the one sanctioned
adaptation beyond mechanical rename; it gets its own deviation-record
entry describing exactly what changed. R16. Palgebra's `deps-lint` is
updated to enforce the renamed rule (`ehrt.palgebra.*` never requires
`ehrt.tools.*` or `ehrt.sim.*`) and KEPT. Its retirement in favor of `poly
check`'s brick-level enforcement is a future ADR once equivalence is
demonstrated, not a this-session cleanup. R17. Library version skew is
resolved by the workspace's `inconsistent-lib-versions :error` validation:
where sim and tools pin differently, converge on the newer pin unless a
test fails, in which case stop and ask (R10). Expected collision: Clojure
1.12.x. HAPI FHIR/v2 deps land in `components/tools/deps.edn`.

### Steps

0. Environment + precondition probe.
1. Tools history merge (AUTHOR ACTION, delegated to the session for this
   session only — see deviation record).
2. Carve into components/tools, components/palgebra, bases/ehr-cli,
   projects/tools-cli, projects/conformance.
3. Provenance and ledger migration.
4. Wiring (root deps.edn, workspace.edn).
5. Verification gate: poly check, poly test :all, poly test :project, CLI
   smoke, poly diff since:stable-bootstrap.
6. ADR-0002 and hole closures.
7. CI, sweep, archive.
   AUTHOR ACTION after green CI: tag `stable-tools-landing`.

(Full step detail — target directory layout, namespace-rename rules,
verification-gate sub-items — matched what actually got built; see
`notes/ADRs.md` ADR-0002 for the landing shape as executed, which is the
authoritative record, not this summary.)

### Deviation record

(P1–P3 stanza, then dated entries. Empty valid; absent not.)

---

## Appendix: deviation record (as executed)

This session's own findings, in the order they surfaced. The full,
narrative version of each lives in `notes/ADRs.md` ADR-0002's own
"Deviation record" section — summarized here so this archived prompt
carries the shape of what happened, without duplicating that record's
full text.

**Preconditions.** P1 (tag pushed, `f848b67`), P2 (clean tree, `poly
check` OK), P3 (`poly diff since:stable-bootstrap` shows only the H6 docs
commit) — all verified via WSL; native Windows Git Bash lacks JDK 21 and
SSH credentials for this repo's remotes, confirmed a genuine environment
mismatch, not a false alarm.

**Step 1 delegation.** Git commit/merge is the author's ceremony by
default (ADR-0001 R6); asked explicitly in this session's own chat,
delegated for this session only. File-list parity against
`stable-pre-monorepo` verified exact.

**R13's "one docstring provenance note" and R14's "corpus requires judge"
were both imprecise against the live tree** — corrected, disclosed, in
ADR-0002; neither changes this session's actual decisions.

**Palgebra needed an `interface.clj` R13 never named** — `poly check`'s
own enforcement left no alternative once palgebra became a separate
brick; added, same wide-delegation methodology as `ehrt.tools.interface`.

**`docsgen.clj`'s cli.md renderer inverted the dependency direction**
(a component requiring a base) — `render-cli-md` was already pure;
`write-cli-md!` parameterized, `docsgen_test.clj`'s cli.md tests rewritten
against a local representative fixture instead of the real `cli-spec`.

**`quickstart_fresh.clj` depended on tools' own root `README.md`, already
dropped as "superseded"** — recovered from the pre-merge clone, relocated
to `bases/ehr-cli/README.md`, internal doc links repointed.

**cwd-relative literal paths (`artifacts.lock.edn`, `config/synthea/*`,
`resources/synthea-default.properties`, `test/fixtures/**`,
`test-integration/fixtures/**`) are a real tools-vs-sim convention
difference**, confirmed empirically (`poly test` always runs with cwd =
workspace root) — resolved by placing these files at the workspace root
unchanged, not by converting to `io/resource` (which would have been
wrong for ADR-0005's own real-filesystem artifact-registry design).
`bin/ehr` repointed to `cd` to the workspace root; root `deps.edn` gained
an `:ehr` alias.

**Five census false positives** (`mutate-to-stdout!`, `generate/invocation`,
`generate/operators`, `manifest/MirroredManifest`, `mutate/mutate-v2`) —
all docstring-prose slash-strings, not real calls; none had a caller,
none needed exporting.

**Two `clojure.core`-shadowing names** (`resolve`, `run!`) — cosmetic
under `poly test`, but a real `Syntax error compiling` failure under a
real `bin/ehr` subprocess invocation (`stdin_intake_real_pipe_test.clj`,
caught by `poly test :project`, not `:all`). Qualified to
`resolve-artifact`/`sim-run!`.

**Makefile dropped, not ported; skills moved to provenance only** — both
asked explicitly (AskUserQuestion), not guessed.

**R15's "sanctioned adaptation" turned out to be a non-event** — the
sim-harness was already subprocess-only (`tools/ADR-0013`), and needed no
repointing; `poly/sim` correctly does not appear in
`projects/conformance/deps.edn`, confirmed by a `poly check` warning 207
this session resolved (not suppressed) by actually removing the
dependency.

**Ledger migrated** (R11): tools/OPEN-4, tools/OPEN-5, tools/OPEN-6,
tools/D-b, tools/SS-5, tools/lineage-duplication, tools/H5,
tools/NIST-reply, tools/SETUP-unspoiled-walk, tools/sim-JDK-21-errand —
full detail in ADR-0002.

**Generated docs (`docs/pipeline.edn`, `docs/use-cases.edn`,
`docs/signature.edn`) still cite old `ehr-testing-tools.*` paths in their
own generated prose** — their regeneration tooling (Makefile targets) did
not survive this session; hand-editing generated content would violate
their own "wholly generated, never hand-edited" contract. Left as a
disclosed, known gap, not silently patched.

**Verification, final state:** `poly check` — OK, zero warnings (two
false-positive warning-207s suppressed via `:necessary`, one true
positive fixed by removing `poly/sim`). `poly test :all` and `poly test
:project` — 131 test namespaces each, 0 failures / 0 errors, including
the real, non-mocked `projects/conformance` suite against the actual
`../ehr-testing-sim` sibling checkout present on this machine. CLI smoke
— `ehr help`, `ehr corpus operators`, `ehr corpus operators --format v2`
all byte-identical against the pre-merge clone; sim's own CLI smoke
unchanged.
