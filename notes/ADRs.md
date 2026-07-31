# Architecture Decision Records — ehr-testing (workspace)

Numbered, append-only, starting fresh at ADR-0001 for this workspace
— not a continuation of `ehr-testing-sim`'s or (later) `ehr-testing-tools`'
own numbering. Never silently revert an Accepted ADR; supersede it
with a new numbered record.

Legacy ADRs move into this workspace intact as provenance
(`notes/sim/ADRs.md`, `notes/tools/ADRs.md`, frozen, not rewritten for
new paths/namespaces) and are cited here origin-qualified, e.g.
`sim/ADR-0008`, `tools/ADR-0017`.

**Citation rule (added 2026-07-30, judge-v2-nist follow-through
session): a bare `ADR-00XX` in this file, or in any other workspace
document, means this file's own record.** Frozen-era ADRs are always
cited origin-qualified (`tools/ADR-0012`, `sim/ADR-0008`, etc.) — never
bare. This is the rule the two paragraphs above already modeled; it is
restated as an explicit standing rule here because ADR-0012 below now
shares its number with the frozen `notes/tools/ADRs.md` ADR-0012 (the
`ehr sim` mount design), a genuine collision this session's own
citation-space audit found, unambiguous when ADR-0005 wrote its own
unqualified `ADR-0012` references (2026-07-28, before this workspace's
own ADR-0012 existed) but ambiguous since. Renumbering either record
was considered and rejected: ADR numbers are load-bearing in immutable
places this workspace cannot edit (commit messages, archived prompts,
docstrings) and this register's own append-only, never-reassigned
numbering rule exists for exactly this reason. Existing unqualified
frozen-era references are fixed forward, dated, as they're found — not
rewritten wholesale, and never by editing the frozen files themselves.

---

## ADR-0001 — Migration plan: bootstrap the workspace, land sim, freeze tools out

**Status:** Accepted, 2026-07-28 (author-ratified per the bootstrap
session's own checkpoint convention — every COMMIT and AUTHOR ACTION
line in that session's prompt was either executed or explicitly
confirmed with the author before the session continued).

### Context

`ehr-testing-sim` and `ehr-testing-tools` had drifted into the
duplicated-code-and-cross-repo-dependency-knot shape Polylith's own
docs diagnose (`doc/migration/polylith-brief.md` §2). This ADR records
the session that created the `ehr-testing` workspace, landed sim as
its first project, and deliberately left tools out.

### Decision

**Settled rulings (author-directed, verbatim from the bootstrap
session's own prompt):**

- **R1.** Workspace repo is `ehr-testing`. Top namespace is `ehrt`.
- **R2.** `ehr-testing-guide` stays out of the workspace entirely,
  permanently — not a deferred landing, don't plan for it.
- **R3.** `ehr-testing-tools` will be the only published artifact.
  Sim is a project that builds an app artifact or nothing; never
  published as a library. Resolves Polylith's one-library-per-
  workspace constraint (brief §11).
- **R4.** Sim's authoring discipline is canonical for this workspace.
  Where sim's and tools' conventions differ, sim's form wins; tools
  conforms on arrival, a later session.
- **R5.** Landing shape is thin base + fat component: `bases/sim-cli`
  holds only CLI dispatch; `components/sim` holds everything else
  behind one deliberately wide `ehrt.sim.interface`, narrowed only by
  a future, author-ruled extraction session (see H1).
- **R6.** All git commits and pushes are the author's, made manually.
  A session prepares working trees and commit messages; it does not
  itself run `git commit`, `git push`, `git merge`, or `gh` unless the
  author explicitly delegates that for the session it's said in — a
  live, scoped grant, not a standing rule change. Agents in this
  environment hold ambient authenticated `gh` credentials (the
  `pragsmike/packs` precedent, `sim/AUTHORS-GUIDE.md` §2); the
  standing rule is that they don't use them off their own initiative.
- **R7.** Hooks are installed and verified before the first
  workspace-era commit is pushed. `.githooks/pre-push` (`61a1573`)
  protects every commit after it, even though its own `poly
  check`/`poly test :project` gates couldn't pass until the workspace
  itself existed (`90e1b11`) and later sim (`c0b5b0a`) — verified by a
  real `git push --dry-run` right after the hook's own commit, which
  correctly refused (gitleaks not yet installed), and again after
  installing gitleaks (refused correctly on the still-missing `:poly`
  alias target, which didn't exist until the next commit).
- **R8.** Legacy ADRs and facts registers move intact as provenance
  (`notes/sim/ADRs.md`, `notes/sim/facts-register.md`), byte-identical
  except a one-line origin/date header — never rewritten for new
  paths or namespaces. The same treatment extends to sim's own
  `.agents/{memory,plans,prompts/archive,session-records}` (durable
  design lineage and session history), moved to `notes/sim/agents/`.
  Sim's own copy of `.agents/skills/` is dropped as a duplicate — the
  workspace's live `.agents/skills/` (step 5) already carries that
  content forward.
- **R9.** Poly version 0.3.32, via the `:poly` alias — no standalone
  install. JDK 21 (see the deviation record below for the precise
  local-vs-CI characterization).
- **R10.** Fix-forward: if a step's premise turns out false against
  the live tree, stop, record the finding, and ask — never silently
  adapt. See the deviation record.

**Landing shape, as built:**

```
components/sim/    -- fat: src/ehrt/sim/*, test/ehrt/sim/*,
                       resources/sim/* (nested per brick convention),
                       docs/* (sim's docs tree, intact), NOTICE
bases/sim-cli/      -- thin: src/ehrt/sim_cli/core.clj (was cli.clj),
                       test/ehrt/sim_cli/core_test.clj
projects/sim/       -- composes both bricks; :run and :coverage
                       aliases (ported from sim's own, ADR-0004
                       posture: measure and report, no threshold)
```

Mechanical rename, total: `ehr-testing-sim.X` → `ehrt.sim.X`,
`ehr-testing-sim.cli` → `ehrt.sim-cli.core`, `ehr_testing_sim/x.clj` →
`ehrt/sim/x.clj`. Classpath `io/resource` lookups updated for the
`resources/sim/` nesting (`version.edn`, `order-profiles.edn`,
`demographics/*`, `modules/*` all gained a `sim/` prefix). `ehrt.sim.interface`
re-exports exactly what `bases/sim-cli`'s own `src` calls from outside
its own namespace — `ok`/`rejected`/`error`/`ok?`/`rejected?`,
`check-all`, `identifiers-command`, `run-command`, `version`,
`git-sha` — determined by grep against the pre-merge repo, not
interface-design judgment (R5's own fat-component disclosure,
restated in `AGENTS.md`).

**Verification method:** property-law survival as the rename verifier
— sim's own determinism, invariant-catalog, emitter-derivability, and
schema-round-trip property suites all ran green under `ehrt.sim.*`
namespaces post-rename (`poly test :all`, `poly test :project`: 437
tests / 1124 assertions, 0 failures / 0 errors, coverage 96.87%
forms / 98.64% lines — `projects/sim`'s own `:coverage` alias,
measure-and-report per ADR-0004's posture), which is the load-bearing
evidence that the mechanical rename was faithful, not a byte-level
diff of the source. Additionally, the standalone CLI
smoke test compared `--format ground-truth` and `--format er7` output
between the pre-merge `ehr-testing-sim` clone and the landed
`projects/sim`, same seed/config: byte-identical. (The full EDN
output's `:manifest :generator` block legitimately differs — `:name`
now reads `ehrt.sim`, and `:sha256` differs because it's derived from
each repo's own distinct git HEAD — neither is a determinism
violation; ADR-0009's within-version guarantee covers ground-truth and
wire output, not cross-repo provenance identity.)

**Cross-references to legacy ADRs** (origin-qualified,
`notes/sim/ADRs.md`): `sim/ADR-0001` (dependency arrow tools→sim, now
enforced by `poly check` instead of being a separate-repo fact);
`sim/ADR-0003` (adopted sim's own authoring conventions from
`ehr-testing-tools`, the precedent this workspace's own R4 repeats one
level up); `sim/ADR-0009` (seed stability is a within-version
guarantee — the CLI smoke test above relies on this); `sim/ADR-0015`
(going public, `pack-push` dormant — this workspace hasn't ported the
pack ritual at all yet, `AUTHORS-GUIDE.md` §2).

### Deviation record

**Environment stanza (step 0, 2026-07-28, WSL2/Ubuntu):**

```
git --version    -> git version 2.50.1
java -version    -> openjdk version "21.0.7"
                     OpenJDK Runtime Environment (build 21.0.7+6-Ubuntu-0ubuntu120.04)
                     OpenJDK 64-Bit Server VM (build 21.0.7+6-Ubuntu-0ubuntu120.04, mixed mode, sharing)
clojure -M:poly version -> poly 0.3.32 (2025-12-29)
```

**JDK/Temurin premise (step 0).** The session prompt characterized the
environment as "JDK 21 (Temurin)." The actual `java -version` output
is Ubuntu's own OpenJDK 21 build, not Eclipse Temurin — a
`temurin-17-jdk` package was present but unused (version 17, not 21),
and no Temurin 21 build exists on this machine. Sim's own `SETUP.md`
tells WSL2/Ubuntu users to install the stock `openjdk-21-jdk` apt
package for local dev and reserves "Temurin" for its CI's
`distribution: temurin` pin — this machine's state matches that
convention exactly. Author's ruling (asked, not guessed): record
precisely as measured; "Temurin" is CI-only, not a local-dev claim.
See `SETUP.md` §1's own "JDK, precisely" subsection.

**Gitleaks premise (step 2).** The prompt asked to "port sim's hook
(WSL-commit enforcement, gitleaks)." Sim's actually-committed
`pre-push` hook does not run gitleaks at all — the one gitleaks scan
on record (`sim/facts-register.md` F15) was a one-time go-public
secrets audit, not a hook behavior. Flagged before implementing;
resolution: added gitleaks as a *new* pre-push gate (extending sim's
pattern, not porting a behavior that never existed), gitleaks v8.30.1
installed to `~/.local/bin` (checksum-verified against the published
release checksums, same method as F15), with the author's explicit
go-ahead for the download.

**Residual `.staging/` files, step 6→7 seam.** Step 6's git-history
merge staged sim's *entire* tree under `.staging/`; step 7's own
target layout named only `src/test/resources/docs` moving out of it,
leaving no stated disposition for `.staging/`'s own `AGENTS.md`,
`AUTHORS-GUIDE.md`, `CLAUDE.md`, `CONTRIBUTING.md`, `SETUP.md`,
`README.md`, `LICENSE`, `Makefile`, `NOTICE`, `.gitattributes`,
`.gitignore`, `.githooks/`, `deps.edn`, `.github/`, and `.agents/`.
Flagged before executing step 7; author-approved resolution: `NOTICE`
(load-bearing LOINC text) relocated with its content into
`components/sim/NOTICE` and the two directory-scoped
`resources/{demographics,modules}/NOTICE` files, all with their
internal path references updated to match the new locations (unlike
ADRs/facts-register, these are operative, not frozen). `.github/ISSUE_TEMPLATE/*`
moved to the workspace root (still useful, repo-wide).
`.agents/{memory,plans,prompts,session-records}` and
`notes/{ADRs,facts-register}` moved to `notes/sim/` as provenance
(R8). Everything else — the root config/doc files, all superseded by
the workspace's own versions written at step 5, or by sim no longer
being a standalone repo — deleted; still reachable in git history via
the merge commit (`a0534d0`). `.github/workflows/test.yml` deliberately
left in `.staging/` for step 11 to port.

**Step 7 commit-scoping mistake, self-caught.** The first `git commit
-- <pathspec>` for step 7 listed the *destination* paths
(`components/sim`, `bases/sim-cli`, ...) but omitted the matching
`.staging/{src,test,resources,docs,NOTICE}` *source* paths, so the
rename's deletion half never landed — `HEAD` briefly carried both the
old and new copies of every moved file. Caught by inspecting
`git ls-tree -r HEAD` directly rather than trusting `git status`;
fixed with an immediate follow-up commit
(`7c01a59`) removing the stale `.staging/` paths, no new commit
needed to the already-correct `components/sim`/`bases/sim-cli` content.
Same follow-up also ran `git add --renormalize .` for CRLF/LF
line-ending drift the first commit's own warnings flagged.

**Two latent test bugs, pre-existing, unmasked by the rename.**
`identifiers_test.clj` and `vendored_module_test.clj` both called
`clojure.set/intersection` (or `/subset?`) fully-qualified without
requiring `clojure.set` — this worked in the standalone sim repo only
because some other required namespace happened to load `clojure.set`
transitively first; under Polylith's different namespace-load order,
`poly test` surfaced a `ClassNotFoundException`. Fixed by adding the
missing `(:require [clojure.set ...])` — not a rename bug, a
correctness gap the migration's own classpath change exposed. Recorded
here per the fix-forward-with-disclosure rule rather than folded
silently into the rename commit's diff without comment.

**HL7 wire bytes and the workspace's own `eol=lf` rule.** Step 9's own
coverage-alias check led to inspecting `components/sim/docs/demos/`
more closely, which surfaced a real latent risk: `messages*.txt` under
that tree carry literal ER7 wire bytes (`\r` segment separators), and
the workspace's own `.gitattributes` (`* text=auto eol=lf`, step 1)
had no exemption for them — exactly the hazard `ehr-testing-tools`' own
`.gitattributes` already documents and defends against for its v2
fixtures. Not yet corrupted (verified byte-identical against the
pre-merge sim clone), but exposed for any future checkout on a
platform/config where normalization would actually fire. Fixed with a
`-text` override before it could bite (`bb8da0e`), not found by any
step's own explicit checklist item — a case for reading file content,
not just diffing paths, during verification.

### Named holes

Each recorded with its own trigger condition — not guessed at, not
silently resolved by whatever shape was locally convenient.

- **H1. Source/sink component shape** (one component with internal
  polymorphism vs. N components sharing an interface) — decided from
  the landed source-sink design after the running `ehr-testing-tools`
  session completes. Until then, `ehrt.sim.interface`'s width stays
  as-landed (R5); don't treat it as a decomposition hint.
- **H2. Tools landing plan** (thin `ehr-cli` base + fat `tools`
  component; first extractions judge → palgebra → corpus) — a
  separate prompt after tools is frozen and tagged stable.
- **H3. Conformance project** (base-less; composes sim + judge +
  corpus) — after H2's judge extraction.
- **H4. `hl7v2` wrapper component**, including the cmiles74 escaping
  workaround with a characterization test that fails if upstream
  fixes escaping — after H2.
- **H5. Published-artifact coordinates** (Clojars verified-group vs.
  Maven Central; group likely `io.github.pragsmike` or an owned
  domain) — author's call, unblocked by nothing in this repo.
- **H6. Workspace CI — CLOSED, 2026-07-28.** The `poly check` + `poly
  test :all` workflow landed this session (`53d76b0`,
  `.github/workflows/test.yml`), then all three of its own trigger
  conditions cleared the same session: author pushed to `origin`
  (`git push origin main`, `2a77fd6`), GitHub Actions ran it green
  (run `30379102956`, 1m15s), and the author tagged `stable-bootstrap`
  at that same commit (`git push origin stable-bootstrap`). The
  workflow's own `test.yml` comment names the next step: switch from
  `clojure -M:poly test :all` to incremental `clojure -M:poly test`
  (changed-or-affected since the most recent `stable-*` tag) — that
  edit itself is a small, low-risk follow-up, not reopening this hole.

### Amendments (2026-07-28, ADR-0002 session — fix-forward, dated, not a revert)

- **H1 CLOSED per ADR-0002 R12.** One component, internal
  polymorphism — the landed source-sink design (`docs/source-sink-design.md`
  D4, `ehrt.tools.corpus.source-sink`'s own docstring) is a canonical
  map schema with an open-set `:kind`, per-kind constructors in one
  namespace, runtime selection via URL designators. No current kind
  carries a distinct third-party dependency; SS-5 (blaze) is the
  recorded trigger for revisiting this (ADR-0002 R12).
- **H2 CLOSED, receipts in ADR-0002.** `ehr-testing-tools` landed as
  `components/tools` + `components/palgebra` + `bases/ehr-cli` +
  `projects/tools-cli`, `poly check`/`poly test :all`/`poly test
  :project` all green, CLI smoke byte-identical against the pre-merge
  clone.
- **H3 CLOSED per ADR-0002 R15.** `projects/conformance` — base-less,
  composes `components/sim` + `components/tools` + `components/palgebra`
  (test-only) — needed no prior judge extraction; the whole of
  `ehr-testing-tools`' `test-integration/` tree landed as one project's
  `test/`, `poly test :project` green including the real sim-sibling
  gate-loop and full-capability-gate suites.

---

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

## ADR-0003 — Pre-push gate doctrine: irreversibility-only

**Status:** Accepted (author-directed), 2026-07-28. **Written into its
own reserved slot** by the discipline-parity session, landing
chronologically after ADR-0004 and ADR-0005 — ADR-0004's own numbering
note already recorded why: the pending closeout-sweep session that owned
this record stopped at its own step-0 precondition (CI red) before
writing it, and no ADR number is ever reassigned once used, so ADR-0004
took the next number rather than block on this slot. This record is
that slot, filled in, not renumbered.

### Context

`.githooks/pre-push` originally ran `clojure -M:poly test :project`
alongside `gitleaks detect` and `clojure -M:poly check` (ADR-0001's own
R7, `61a1573`). The author removed it directly, terse commit message,
no session prompt: `1ebf4ce "Don't run tests on pre-push."`. This record
is the doctrine that commit was acting on, written down after the fact
so a future session doesn't reintroduce a test gate at push time without
understanding why it was removed.

**Honest evidence note.** The session prompt that requested this ADR
named "the connection-close incident" as the motivating event — a
long-running `poly test :project` invocation inside the push hook
plausibly timing out or dropping an SSH/terminal connection mid-push.
This workspace's own tree carries no record of that specific incident
beyond the terse commit message itself; per this workspace's
fix-forward-with-disclosure rule, this ADR states the doctrine the
author has directed and cites the one piece of repo evidence that
exists, rather than fabricating incident detail nothing in the tree
supports.

### Decision

**The pre-push hook gates on irreversibility, not correctness.** Three
checks, all fail-closed:

1. **WSL provenance** (`.githooks/pre-commit` and `.githooks/pre-push`
   both check `$WSL_DISTRO_NAME`/`/proc/version`) — a commit or push
   from native Windows is a mixed-platform-git mistake that's cheap to
   prevent and expensive to unwind (line-ending wars, executable-bit
   flips) once it's in history.
2. **`gitleaks detect`, fail-closed** — a secret pushed to a public
   remote is irreversible the instant it leaves the clone, regardless of
   whether it's reverted a commit later; history is treated as public
   from the moment of push (the same posture tools' own AUTHORS-GUIDE
   took once it went public).
3. **`clojure -M:poly check`, fail-closed** — a dependency-direction or
   interface violation compounds the longer it survives in shared
   history; catching it before it leaves the clone is cheap, catching it
   after is a revert.

**Tests are deliberately not in this list.** A failing test is
*reversible* — CI catches it on the very next push, the failure is
visible, and fixing it costs one more commit. It shares none of the
three properties above (public-secret exposure, cross-repo build
breakage, un-revertable history) that justify blocking a push
synchronously, in a hook a human is sitting in front of, rather than
async in CI. Running the full suite (or even `:project` alone, which by
this same day's own empirical findings, ADR-0004, pulls in every
artifact-fetch-dependent integration test unless structurally excluded)
inside a push hook trades a human's real time for a check that doesn't
need to happen synchronously at all.

**The trust boundary is the `stable-*` tag, not the push.** Ordinary
pushes to `main` may be red between commits — CI reports it, nothing
blocks it. A `stable-*` tag (`stable-bootstrap`, `stable-pre-monorepo`,
and this workspace's own future ones) is the point something is
asserted trustworthy, and that assertion rests on CI having run green at
that exact commit (`notes/ADRs.md` ADR-0001 H6, ADR-0002's own
verification sections) — not on the pre-push hook, which never ran the
suite at all by the time either tag was cut this session's own
lineage runs through.

### Consequence

`AGENTS.md`'s pre-push description (`4ed3ffa`, pre-dating this ADR)
already matches this doctrine in practice; this record is why, not a
change in mechanism. Any future session tempted to add a test gate back
into `.githooks/pre-push` should read this ADR first — the removal was
deliberate doctrine, not an oversight to quietly fix.

---

## ADR-0004 — Carve-loss audit; CI two-lane rule restored; local state is not clone state

**Status:** Accepted (author-directed), 2026-07-28.

**Note on numbering.** `ADR-0003` is a reserved, not-yet-written slot —
the pending `2026-07-28-ehr-testing-h2-closeout-sweep` session's own
pre-push hook-doctrine record, which that session's own prompt already
cites forward. That session stopped at its own step 0 precondition
(CI red) before writing it; this session and the executable-bits fix
session that preceded it needed to proceed regardless, so this record
takes the next number rather than block on a slot another session owns.
No ADR number is ever reassigned once used — see ADR-0005 below for why
that discipline mattered concretely this same day.

**Addendum (2026-07-28, discipline-parity session).** The slot is now
filled — see ADR-0003, above this record (inserted into its reserved
position, landing chronologically after this ADR per R27; no
renumbering).

### Context

Three post-H2-landing failures shared one root cause: material dropped
at carve time as "superseded," without a load-bearing inventory,
turned out to still be load-bearing. In order of discovery:

1. `bin/ehr` and ten other tracked scripts lost their executable bit in
   the index during the carve (`core.fileMode=false` made this
   invisible locally) — fixed same-day, its own session
   (`2026-07-28-ehr-testing-ci-red-executable-bits`), CI run
   `30405350913` red, `30408485074` green.
2. Fixing (1) let `poly test :all` run far enough to reach 13
   `^:integration`-tagged test namespaces requiring `ehr artifact
   fetch` (synthea, temurin-jdk, the FHIR validator) — machinery CI's
   cold clone has never had, because the pre-carve `deps.edn`
   `:test`/`:integration` alias split (and the Makefile `test`/
   `integration` target split enforcing it) that used to keep these
   off the per-push path was dropped wholesale as "superseded"
   (ADR-0002's own disclosure), without anyone identifying this
   specific consequence.
3. (Named but not yet hit in CI, found by this session's own audit:)
   `README.md` never existed at the workspace root — poly's generated
   placeholder `readme.md` sat there instead — and the workspace skill
   set only ever got half of what sim and tools each carried.

This session: audits the full loss set (not just these three) so the
next failure isn't a fourth surprise; restores the ENF-1 two-lane rule
in Polylith's own idiom; authors the missing README; restores a thin
Makefile. The sibling-checkout fix originally scoped here (R20) grew
into its own record — see ADR-0005 — once the author's own amended
ruling turned a repointing fix into an architecture fulfillment.

### Decision

**R18.** The ENF-1 two-lane rule stands unchanged in the workspace,
quoted verbatim from its origin
(`notes/tools/prompts/2026-07-25-enf1-enforcement-wave.md`): *"Fast
gates block, slow gates schedule — hooks and the per-push CI job carry
only the fast checks... the integration suite goes in a separate
scheduled/manual workflow, never in the per-push path."* And, on
fetching engines in CI: *"If any secret/licensing consideration
surfaces around fetching an engine in CI... stop and report rather
than fetch."* No such consideration surfaced this session — all three
artifacts `.github/workflows/integration.yml` fetches are already
license-verified in `artifacts.lock.edn` itself.

**R19.** The lane split is structural, not metadata: `projects/integration`
holds the artifact-fetch-dependent test namespaces; `projects/conformance`
keeps workspace-internal suites. The `^:integration` tag may remain as
documentation but is not the enforcement mechanism — poly's own
`skip:PROJECTS` (per-push: `poly test :all skip:integration`) and
`project:NAME` (nightly: `poly test :all project:integration`)
selectors are, pinned empirically this session (not assumed from
ADR-0002's own prior, correct-but-unverified claim that `:all` already
runs every project's test/ dir): `project:X` alone narrows an entire
run to just that project's own dependency closure, dropping every
other brick; `:all-bricks` gets silently overridden by a co-occurring
`project:X`, not merged with it. `:all skip:X` is the form that
actually unions "every brick, every project except X."

Membership in `projects/integration` was decided by actual dependency,
not the inherited tag list: of the 13 `^:integration`-tagged
namespaces, 5 (`baseline_gating_test`, `contract_pairing_test`,
`intake_source_golden_test`, `zero_flag_reproducibility_test`, and
`smoke_test`'s FHIR half) genuinely need `ehr artifact fetch`
machinery and moved; 2 (`mutate_stdout_stdin_loopback_test`,
`stdin_intake_real_pipe_test`) only need `bin/ehr`'s own executable
bit and pre-committed fixtures — hermetic once (1) above landed — and
stayed; 5 sim-consuming suites stayed too, on the expectation ADR-0005
confirmed (they needed a *different* fix, not artifact-fetch, and
became fully hermetic once that fix landed rather than merely staying
un-broken). `smoke_test.clj` itself split along a seam its own
docstring already drew ("FHIR half" / "sim-harness half") once the two
halves needed different lanes — a real, disclosed cost: the tier's
original point (both real-engine seams smoke-tested in one sub-2-minute
pass) no longer holds for the FHIR half on the per-push path. One
correction along the way: the "13 known" count itself was off by one —
`components/tools/test/ehrt/tools/corpus/golden_comparison_test.clj`
matched a grep for `^:integration` only inside a docstring's prose
(naming a *different*, real integration test), not as a real tag; 12
namespaces actually carried it.

**R20.** Superseded by its own amendment — see ADR-0005.

**R21.** Workspace skills are the union of sim's and tools' pre-carve
sets: `handoff` and `string-diagram` from sim (sim's form wins on
collision, ADR-0001 R4), eight tools-only skills (`committee`,
`find-skills`, `probe`, `repo-adaptation`, `review`, `scenarios`,
`shared-skill-layout`, `wsl-windows-git-hygiene`) added live. `handoff`
diffed byte-identical (no fix to fold in). `string-diagram` diffed
divergent: the live (sim) copy vendored its own stale, commit-pinned
copy of palgebra's converter+examples from before palgebra was ever in
this workspace; tools' own copy pointed at a live `palgebra/` location
instead of vendoring — the fix ADR-0002 already flagged as a "named,
disclosed gap for a future session's deliberate call." That session is
this one: folded tools' fix in, repointed at the *current* workspace
location (`components/palgebra/...`, one directory deeper than tools'
own old bare `palgebra/`), deleted the now-redundant vendored copy
(`.agents/skills/string-diagram/{tools,examples}/`).

**R22.** Root `README.md` authored; poly's generated `readme.md`
removed in the same working-tree change — `git rm` then a fresh
`Write`, not `git mv`, per the case-only-rename hazard this WSL/NTFS
checkout has for `README.md`/`readme.md` (confirmed hazard, not
theoretical: the `Write` tool itself refused to write `README.md`
until `readme.md` — the *same inode* on this filesystem — was read
first). Authoring the root README surfaced a real, second-order fix:
`ehrt.tools.quickstart-fresh`'s own `readme-path` default was
`bases/ehr-cli/README.md`, a stopgap from when tools' own README was
relocated there because no root README existed yet (ADR-0002). With a
real root README now the obvious canonical Quickstart-teaching surface,
the default moved to it, and `bases/ehr-cli/README.md`'s own duplicate
Quickstart section became a pointer to the root one instead of a second,
driftable copy — exactly the doc-rot shape DOC-5's own freshness check
exists to prevent, now pointed at the file that should own it.

**R23.** A thin Makefile returns: `test`, `integration`, `quickstart`,
`ci-parity` — named entry points to poly/CLI commands, not logic. The
full pre-carve Makefile (pack/pack-skills/pack-push, docsgen
regeneration, both lints, `check-palgebra-drift`, `coverage`,
`integration-smoke`) stays superseded per ADR-0002's own
author-approved decision, not reopened by this record.
`ci-parity` — fresh `git clone` to a scratch dir, artifact cache
repointed at an empty directory via `EHR_TESTING_TOOLS_CACHE`, then the
per-push lane — is this session's own answer to the generalized trap
below, made runnable rather than merely stated. Its full fresh-clone
form could not be exercised this session (git commit is the author's
own ceremony, ADR-0001 R6, and nothing existed to clone from beyond
this session's own starting commit); its constituent commands (cold
cache, `skip:integration`) were verified directly against the working
tree instead — `make ci-parity` itself is a commit away from its own
first real run, which the author's own post-session actions name.

### The carve-loss audit

Landed as `notes/carve-loss-audit.md`, committed before any restoration
in this record, so every fix below cites an audit row rather than
re-arguing its own premise. Method: every non-generated path at tools'
`stable-pre-monorepo` tag and sim's final pre-merge tree, diffed
against the current tree, with `src/`/`test/`/`docs/` subtrees excluded
from the row-by-row listing (already exhaustively verified elsewhere —
zero unaccounted paths found there when re-checked for this audit).
15 tools-side rows, 21 sim-side (sim: zero new findings, already fully
disclosed in ADR-0001), one row UNDECIDED at write time
(`.claude/settings.json`, a git-tracked shared permissions allowlist —
ruled live, in-session, by the author during this session's own step 3:
don't commit it, `.claude/` stays untracked).

### The generalized trap

Stated for citation, this record's own contribution to the workspace's
running doctrine: **local state is not clone state.** Index modes,
artifact caches, and sibling checkouts have each, independently, masked
a real CI failure behind a local green this same week — the executable
bit (working tree already had it set; the index didn't), the artifact
cache (this dev machine's own cache was warm from real prior use;
CI's is always cold), and the sibling checkout (a real `../ehr-testing-sim`
happens to sit next to this clone on this machine; CI has no sibling at
all — see ADR-0005 for how that one, specifically, stopped mattering).
`make ci-parity` is this doctrine's own local probe: the clone is the
ground truth, not the working tree.

### Deviation record

**The `poly test :all` mechanism, pinned empirically (step 0), not
assumed from ADR-0002's own prior claim.** `projects/conformance/deps.edn`'s
own `:test` alias (`cognitect-labs/test-runner`, `:dirs ["test"]`, no
`:excludes`) has no selector at all — `poly test :all`'s own "all brick
tests + all project tests" semantics is what pulled every namespace
under it in unconditionally, once the executable-bit fix let the run
get that far. `poly help test`'s own documented ARGS (`:all`,
`:all-bricks`, `project:NAME`, `skip:PROJECTS`) were read and tested
directly (three separate empirical passes: `:all-bricks project:conformance`
narrows instead of unions; `project:integration` alone runs bricks only,
not the project's own test/ tree; `:all skip:integration` is the form
that actually works) rather than guessed from the flag names' own
apparent meaning.

**`.github/workflows/test.yml`'s own third step, dropped, not
carried forward as `skip:integration`.** The pre-this-session workflow
ran `poly test :all` then a redundant `poly test :project` as
belt-and-suspenders against `:all` ever silently skipping project-level
tests. `:all skip:integration`'s own comprehensiveness (verified this
session, not assumed) makes that redundant step strictly weaker than
what it followed (`:project`'s own "changed since last stable tag"
scope is narrower than a full `:all`), so it was removed rather than
adapted — named here since removing a gate, even a redundant one,
deserves its own line.

**Named, disclosed, out of scope.** `.github/workflows/test.yml`'s
former `poly test :project` step carried a comment citing "AGENTS.md"
and ".githooks/pre-push's own gate" for its own rationale — stale in
two ways even before this session touched it (pre-push dropped its own
`poly test :project` gate in `1ebf4ce`, the same-day executable-bits
session's own prompt), and this session's own edit to that step
removed the comment along with the step. Not separately fixed as its
own row: it no longer exists to be stale.

---

## ADR-0005 — The `ehr sim` mount: `notes/tools/ADRs.md` ADR-0012 fulfilled, `notes/tools/ADRs.md` ADR-0013 decision 1 retired

**Status:** Accepted (author-directed, amending the session's own R20
in-session), 2026-07-28.

### Context

ADR-0004's own R19 restructuring left five `sim_*_test.clj` suites (plus
`smoke_test.clj`'s sim half) in `projects/conformance`, still resolving
sim through a sibling-checkout discovery order (`ehrt.tools.sim`'s
`:sim-dir` → `EHR_TESTING_SIM_DIR` → `../ehr-testing-sim`, `notes/tools/ADRs.md` ADR-0013)
that CI's cold clone never satisfies — they skip cleanly there rather
than fail, so they weren't part of ADR-0004's own red-CI fix, but they
also never ran for real anywhere CI could see.

This session's own initial proposal (documented in chat, not
committed) was a minimal repointing: keep the subprocess, retarget its
discovery default from the sibling checkout to `projects/sim` in this
same workspace, via a new `bin/sim` launcher mirroring `bin/ehr`'s own
shape. The author's own amended ruling rejected the subprocess
entirely: *"I'm ok with not using the subprocess at all, and simply
mounting sim's CLI tree as a subcommand of tools ehr... this was the
original design, and the subprocess technique was a way to get around
the fact that sim grew in a separate project that wasn't available to
all agents at the time."*

That recollection undersold its own case. `notes/tools/ADRs.md`
ADR-0013's own decision 1 gives the real, stronger reason: *"sim is a
private repo today and this repo is public (ADR-0008); a git or Maven
dependency from a public repo onto a private one breaks public CI
outright... and even once sim is public, a classpath dependency would
invert ADR-0012's own stated arrow... and tangle the two repos' version
lockstep."* [The quoted "ADR-0012" is `notes/tools/ADRs.md` ADR-0012 —
qualified fix-forward, 2026-07-30, not the workspace's own ADR-0012
below.] And `notes/tools/ADRs.md` ADR-0013 decision 5, verbatim: *"The
`ehr sim` mount remains DEFERRED (ADR-0012, unchanged)."* `notes/tools/ADRs.md`
ADR-0012 itself is the mount's own pre-existing design — five CLI
interface properties, verified against source, that a mount would rest
on, explicitly left unbuilt pending "the classpath question" resolving.
That question resolved the moment sim and tools became bricks in one
workspace (H2, this same week) — this record is that resolution,
exercised, not a new design.

### Decision

**Mount, in-process, via `ehrt.sim.interface` directly.**
`components/tools/src/ehrt/tools/sim.clj` (the adapter both
`ehrt.tools.corpus.generators`' `:sim` entry and the test harness
already drove) now calls `ehrt.sim.interface/run-command` directly —
no subprocess, no discovery order, no availability check. `bases/ehr-cli`
gains an `ehr sim run` group, dispatching through the same
`ehrt.tools.interface/sim-run!` re-export every other consumer already
used. `poly/sim` is a real `:local/root` dependency now, everywhere
`components/tools`' own compiled code loads (`projects/tools-cli`,
`projects/conformance`, `projects/integration` — transitively, since
`poly/tools` itself now requires `ehrt.sim.interface`; root `deps.edn`'s
own `:ehr`/`:dev` aliases).

**`notes/tools/ADRs.md` ADR-0013's direction invariant preserved, poly-enforced.** The rule
was never "sim and tools must never share a classpath" — AGENTS.md's
own constraints section already permitted `components/tools`/
`projects/conformance` depending on `components/sim`, the arrow
tools → sim, one-directional. `ehrt.tools.sim` requiring
`ehrt.sim.interface`, never the reverse, is exactly that arrow;
`poly check` enforces it structurally (a `sim` → `tools` require would
fail dependency-direction validation, not merely violate a convention).
What's retired is decision 1's *mechanism* (subprocess, because a
classpath dependency used to be structurally impossible across the
public/private, independently-versioned repo boundary) — the
motivating constraint, not the direction rule built on top of it.

**Sibling-discovery machinery removed as dead code, not left
permanently-true.** `available?`, `default-sim-repo-dir`,
`sim-dir-env-var`, and `sim-not-available` are gone from
`ehrt.tools.sim` and its `ehrt.tools.interface` re-export entirely —
not kept as a function that always returns `true`. `EHR_TESTING_SIM_DIR`
is no longer read anywhere in this workspace. `projects/conformance/test/ehrt/tools/sim_harness.clj`
(the project-local pass-through every `sim_*_test.clj` and `smoke_test.clj`
call through) lost its own `available?`/`absence-message` the same
way; its five consumers and `smoke_test.clj`'s sim half each lost their
own `(if-not (sim-harness/available?) (skip) (run-for-real))` wrapper,
now running unconditionally.

**One OS-level pipe test retained, deliberately, as the
consumer-fidelity witness.** Every in-process test proves the mount's
own *logic*; none of them prove `bin/ehr sim run --seed ... --patients ...`
— the actual invocation a human or script would type — still resolves,
parses its flags, and prints a real Result to stdout. `projects/conformance/test/ehrt/tools/sim_cli_real_invocation_test.clj`
is that proof, real `bin/ehr` subprocess and all, comment-marked as the
one deliberate exception to "everything else is in-process now" — same
real-subprocess discipline `mutate_stdout_stdin_loopback_test.clj` and
`stdin_intake_real_pipe_test.clj` already established, stdout and
stderr kept separate (a JVM/WSL warning on stderr must never corrupt
the EDN this test reads from stdout — caught once, by this test itself,
during this session; see the deviation record).

**Registered in help; docsgen deferred to its own owner.** `bases/ehr-cli/src/ehrt/ehr_cli/help.clj`'s
own `cli-spec` gained the `sim` group/`run` verb (flags matching
`run-command`'s own opts 1:1: `--seed`, `--patients`,
`--reference-date`, `--warm-up-seconds`, `--emit`, `--churn`,
`--config`); `help_test.clj`'s two hand-mirrored coverage structures
(`stub-key`, `known-dispatch-pairs`) updated to match, per `notes/tools/ADRs.md`
ADR-0012 property 4's own correction about what "mounting sim" does and doesn't
give for free. Regenerating `docs/cli.md` itself from this updated spec
is the pending closeout-sweep session's own step 4 (docsgen regen
tooling, a named row in this session's own carve-loss audit) — not
duplicated here; the spec data is ready for it.

**Sim stays out of any future published-library artifact (named,
not built).** `projects/tools-cli`'s own `poly/sim` dependency is for
the CLI mount only. ADR-0001 R3 already names `projects/tools-cli` as
this family's sole future publishable library, once H5 (Clojars/Maven
coordinates, still open) resolves; whatever that publishing mechanism
turns out to be must exclude sim's own code from the published
artifact's own coordinates when the time comes — recorded here as a
constraint on that future session, since this session is the one that
introduced the dependency it constrains.

**`bases/sim-cli` / `projects/sim` untouched, deliberately.** Sim's own
standalone CLI artifact and its composing project are exactly as this
session found them — confirmed via `git status` showing zero changes
under either path. Whether sim keeps a standalone CLI future
independent of the `ehr sim` mount is an explicitly deferred author
call, not decided by this record either way.

### `notes/tools/ADRs.md` ADR-0012's five properties, exercised

Each of the five interface commitments `notes/tools/ADRs.md` ADR-0012 recorded (before any
mount existed, so a later refactor couldn't silently break one without
noticing it was load-bearing) held, verified against source rather
than assumed: **(1)** `dispatch`'s own `[group action]`-in, Result-out
shape needed only a new `case` arm. **(2)** The single, host-side
`babashka.cli` spec (`core.clj`'s own `cli-spec`, distinct from
`help.clj`'s rendering spec of the same name) needed three new coerced
keys (`:patients`, `:warm-up-seconds`, `:churn`) and nothing else.
**(3)** Structural Result typing meant `run-command`'s own return value
needed no unwrapping, parsing, or reshaping at all — sim's Result maps
and tools' own are interchangeable by shape, not by shared code.
**(4)** The help-group data shape absorbed a new group with no changes
to its own renderers. **(5)** The `-fn` injection point
(`:sim-run-fn`, defaulting to the new `sim-run-command`) is what kept
`bases/ehr-cli`'s own CLI tests hermetic, exactly the property `notes/tools/ADRs.md` ADR-0012
named it for.

### Deviation record

**A genuine, previously-latent finding, surfaced by the mount, not
caused by it.** `sim_manifest_contract_test.clj` asserted
`(:generator :name)` equals `"ehr-testing-sim"` — sim's *pre-H2-rename*
self-identity. `components/sim/src/ehrt/sim/manifest.clj:77` has
reported `"ehrt.sim"` since ADR-0001's own mechanical rename. This test
path had never actually executed end to end before this session (always
skipped, local and CI both, for lack of a sibling checkout at the
moments it ran) — the in-process mount is what first let it run for
real, and it caught its own staleness on the first real run.
AUTHORS-GUIDE.md's two-failure-modes discipline: a sound check
disagreeing with reality is a finding (leave it red, `notes/tools/ADRs.md`
ADR-0013's own precedent); a check misencoding its own invariant is an escalation
(fix the check). This is the second kind — the rename was already
deliberate and ratified, so the test's own expectation was corrected
to `"ehrt.sim"`, not left red.

**The injection-seam convention, corrected mid-session to match this
codebase's own existing pattern, not invented fresh.** `ehrt.tools.sim/run!`'s
first draft took an injectable `:run-command-fn` as a *second*
argument (`(run! opts {:run-command-fn fake})`), modeled on no
particular precedent. `components/tools/test/ehrt/tools/corpus/generators_test.clj`'s
own `:sim` entry calls `(sim/run! params)` with one argument — the same
single-opts-map convention `ehrt.tools.corpus.generate/generate!`'s own
`:run-invocation`/`:resolve-artifact`/`:resolve-java-bin` already use,
proven by three tests that broke the moment `run!`'s real signature
diverged from what `generators.clj`'s already-committed `:execute-fn`
assumed. Fixed by moving `:run-command-fn` into the single opts map
(pulled out and `dissoc`'d before delegating, same shape as `:out-dir`)
— a real correction caught by the existing test suite, not a
hypothetical one.

**The witness test's own stderr-merge bug, caught by itself.**
`sim_cli_real_invocation_test.clj`'s first draft merged `bin/ehr`'s
stderr into stdout (`redirectErrorStream true`) before parsing stdout
as EDN — a JVM/WSL warning on stderr (this checkout's own documented
stale-fsmonitor-class warning, AUTHORS-GUIDE.md) corrupted the parse.
Fixed by keeping the streams separate and reading only stdout, the same
discipline `real-git-describe`'s own docstring already names for
exactly this reason.

**Named, disclosed, out of scope.** The five sim-consuming test
namespaces' own docstrings and prose still narrate a subprocess/
sibling-checkout world in places beyond the specific "Skips cleanly..."
sentences this session corrected (e.g. `sim_intake_test.clj`'s own
opening paragraph still frames itself as proving intake against "a
real *sim manifest*... something the unit-level fixtures... cannot
cover, since they build their own synthetic... values rather than
invoking sim" — still true, just no longer contingent on a sibling).
Not rewritten wholesale this session; the specific sentences asserting
skip-when-absent behavior that no longer exists were the correctness
bar, not full prose freshness.

---

## ADR-0006 — Discipline parity restored: guides, live registers, sweep completion

**Status:** Accepted (author-directed, commits delegated for this
session), 2026-07-28.

### Context

A review pass against the public `ehr-testing-tools` clone found the
workspace's discipline apparatus — guides, registers, `.agents/`
substrate — below sim's and tools' combined peak strength: several
mechanisms each parent carried were never unioned in, the reserved
`ADR-0003` slot sat empty, `.claude/` was untracked but not gitignored,
docsgen regen tooling was disconnected, and (found only once this
session's own audit looked) root `test/`/`test-integration/` fixtures
contradicted a claim about their own disposition that, on direct
re-reading, ADR-0002 never actually made. This record closes that pass.

### Decision

**R24–R29** (full text: this session's own prompt,
`notes/prompts/2026-07-28-ehr-testing-discipline-parity.md`), summarized
by what each produced:

- **R24** (union method) → `notes/discipline-parity-audit.md`: 24
  mechanism rows (M1–M24), every one dispositioned ADOPT/ADAPT/RETIRE,
  zero UNDECIDED at gate time.
- **R25** (live infrastructure) → `notes/facts-register.md` (fresh
  sequence, F1 carried forward from ADR-0005 with origin citation),
  `.agents/{memory,plans,session-records}/` (README-stub contracts,
  empty substrate — `.agents/handoffs/` deliberately NOT instantiated,
  audit row M14, this workspace's checkpoint model reduces the need
  sim's and tools' async multi-session model had for it).
- **R26** (doctrine promotion) → `AUTHORS-GUIDE.md` §7, five lessons
  (index-not-tree, local-state-not-clone-state, cwd=workspace-root,
  superseded-needs-inventory, poly-enforced-dependency-direction), each
  citing the ADR it was mined from. Landed inside the guide-union
  commit (`a48aae6`) rather than its own — see the deviation record.
- **R27** (pre-push doctrine) → ADR-0003, filled into its reserved
  slot; hook header rewritten to match; dry-run verified against the
  real hook.
- **R28** (fixture ownership) → fixtures relocated to
  `components/tools/test-fixtures/`/`projects/conformance/test-fixtures/`;
  cwd-relative literal paths **kept**, not converted to `io/resource` —
  re-examined against a wider call-site survey than ADR-0002's own
  (roughly a third of the 11 consuming test files exercise real
  CLI path-handling, not fixture lookup, and would break under a
  classpath-resource conversion regardless of brick placement); ADR-0002
  erratum written correcting a mischaracterization of what that ADR had
  claimed, not a mischaracterization the ADR itself made.
- **R29** (top-level tidy) → `doc/` merged into `docs/`;
  `notes/tools/agent/` (singular, one file) merged into
  `notes/tools/agents/` (plural); four deliberate root residents
  (`bin/`, `config/`, `resources/synthea-default.properties`,
  `artifacts.lock.edn`) recorded in `notes/carve-loss-audit.md` as
  accepted warts, three with a named exit plan and one likely
  permanent.

**The audit as method, restated for citation** (AUTHORS-GUIDE.md §7d):
this session's own step 1 is the second time this workspace has run a
full mechanism/path inventory before touching anything (the first,
`notes/carve-loss-audit.md`, ADR-0004) — both times, the inventory
surfaced real findings a narrower, targeted look would have missed
(this session: the scenario-roster live-operational gap, the missing
`.gitattributes` `-text` overrides, the broken `tools/ADR-0011` link,
the stale pre-carve namespace path in `PROVENANCE.md`). Recorded as a
repeatable method, not a one-off.

### The two-thirds-strength assessment, answered

The session that requested this record characterized the workspace's
discipline apparatus as running below sim's peak strength. What this
session actually closes: every artifact-level gap the audit found now
has a disposition and, where ADOPT/ADAPT, a landed fix. What it does
**not** close, honestly: parity of *artifacts* is not parity of
*practice*. A live facts register, a session-records directory, and a
staging-hygiene ritual are apparatus — whether they get used
correctly, unprompted, by the next session that doesn't have this
session's prompt telling it to, is a separate and harder claim this
record cannot make on its own evidence. The author's own post-session
action A4 (below) names the actual test.

### Deviation record

**Precondition stanza (step 0).** Clean tree, `HEAD == origin/main`
(`cc8f5e9`), per-push CI green on `HEAD` (run `30417940625`, verified
via `gh run list`), `clojure -M:poly check` green locally (WSL,
`openjdk 21.0.7`, `poly 0.3.32`) — all four confirmed before any file
was touched.

**Disposition-table counts.** 24 rows (M1–M24), 0 UNDECIDED at gate
time (step 2 passed without an author stop).

**Commit-boundary slip, self-caught (step 3→5 seam).** The R26 doctrine
section (step 5's own content) was written directly into the guide-union
commit (`a48aae6`) instead of its own later commit — noticed only after
the fact, while drafting this record, not caught by the staging-hygiene
ritual in real time despite that ritual being written in the very same
commit that violated it. Not corrected by amending `a48aae6` (this
repo's own no-rewrite discipline, `AUTHORS-GUIDE.md` §1's "create a NEW
commit" convention) — recorded here as the actual, honest account:
step 5 produced no commit of its own because there was nothing left to
commit by the time it was reached. The ritual's own real test is
whether the *next* session's commits stay in bounds, not whether this
one's retrospective diagnosis was instant.

**R27's "connection-close incident" — evidence gap, disclosed.** ADR-0003
cites this as R27's stated motive; this workspace's own tree carries no
record of it beyond the terse commit `1ebf4ce "Don't run tests on
pre-push."` itself. Recorded precisely as evidenced, not embellished —
see ADR-0003's own "Honest evidence note."

**The old sweep prompt could not be archived as written — it was never
written.** `notes/ADRs.md` ADR-0004 already disclosed this (the session
that would have authored it stopped at its own step 0). Step 6e's
instruction to archive it with a superseded-by note was executed as a
placeholder file explaining the gap
(`notes/prompts/2026-07-28-ehr-testing-h2-closeout-sweep.md`), not a
fabricated prompt body.

**ADR-0002 mischaracterization, corrected in the erratum, not in this
record.** This session's own prompt described ADR-0002 as having
claimed a completed `io/resource` conversion. Re-read directly, it
never did — the erratum (ADR-0002, above) corrects the *characterization*
of the record, not the record's own content, which needed no
correction on this point.

**Two operational gaps found and fixed, neither named in the session's
own prompt.** (1) `agent/scenario-roster.md` — the live path
`scenarios`/`probe` need — was missing from the entire tree even after
ADR-0005's skills union; restored from frozen provenance. (2)
`.gitattributes` never carried tools' own pre-carve `-text` overrides
for the v2 HL7 fixtures; added at the fixtures' new location, corpus
bytes verified intact (sha256 match) before the gap was closed.

**Verification, full chain.** `clojure -M:poly check`: green, twice
(after the sweep commit, after the tidy commit). `clojure -M:poly test
:all skip:integration`: 0 failures / 0 errors, twice (after the
interface/docsgen changes, after the fixture relocation) — the second
run is the empirical confirmation that cwd-relative fixture resolution
genuinely works across brick boundaries (`components/tools`,
`bases/ehr-cli`, `projects/conformance`), not merely assumed from
reading `deps.edn`. `make ci-parity`: green, fresh clone, cold artifact
cache, run against the committed fixture-relocation state specifically
(R28's own stated bar — this is exactly the class of change where
working-tree green can lie). Final grep sweep: zero stale
`test/fixtures`/`test-integration/fixtures`/`doc/`-path hits outside
frozen provenance, archived prompts, and this record's own erratum
text.

Self-archived to `notes/prompts/2026-07-28-ehr-testing-discipline-parity.md`.

---

## ADR-0007 — Commit/push restored to session ritual; ruling provenance tags adopted

**Status:** Accepted (author-directed), 2026-07-29.

### Context

`AGENTS.md` and `AUTHORS-GUIDE.md` §1 both state, as ADR-0001 R6: git
commit/push/merge/`gh` are the author's ceremony; a session prepares
the working tree and proposes commit messages but does not itself
commit or push, absent an explicit, in-session, in-chat delegation that
does not generalize to future sessions. That rule was itself a
channel-inferred default, not a verbatim author ruling — it encoded a
stale model of `ehr-testing-sim`'s own 40-session practice (an async,
multi-session culture where an out-of-band author review gated every
push) carried into this workspace's bootstrap prompt dressed as if it
were an author ruling, and it went unexamined by the discipline-parity
audit (ADR-0006) for exactly that reason: the audit inventoried
mechanisms against both parents' final states, and a rule that never
existed in either parent's own terms in the form R6 stated it has no
parent-side row to diff against.

The author's actual practice this session (2026-07-29, development
resumption: kernel/judge extraction, the `ehrt` rename, audience-forked
docs) is to commit and push at each checkpoint, unattended, watching
progress land on the remote — the opposite of R6's default. This
record supersedes R6 in place (ADR-0001 is not reverted; R6's own text
stands as the historical record of what was ruled and why it was
wrong) and adopts a provenance-tag convention so a future audit can
tell, without re-deriving it from a close prose reading, which rulings
came from the author directly and which are this workspace's own
inferred defaults.

### Decision

**R30** (supersedes R6). Committing at checkpoints, and pushing at
each checkpoint, are part of the session ritual for sessions the
author has told, explicitly, in that session's own chat, to operate
this way — the same scoping R6 always had for its one-off delegations,
now the *standing* mode rather than the exception, until a future
ruling changes it again. The staging-hygiene ritual (`AUTHORS-GUIDE.md`
§1, "Staging hygiene between checkpoints") is unchanged and still
applies before every commit, delegated or not: `git diff --cached
--stat` recorded, anything outside the checkpoint's own stated scope
unstaged first. Two classes of action stay the author's alone,
unaffected by R30: **tags** (ADR-0003's own trust boundary — the
`stable-*` tag, not the push, is what CI and a future clone actually
trust) and **repo-level `gh` mutations** — create/delete/settings/
visibility (the `pragsmike/packs` precedent, `AGENTS.md`'s own
citation of it, correctly scoped and left as-is here).

**Provenance tags, adopted this record forward.** Every author-ruling
list in a session prompt or ADR from this point on marks each ruling
`[A]` (author-ruled, verbatim or a direct paraphrase the author would
recognize as their own) or `[C]` (channel-inferred: a default this
workspace's own tooling or a prior session supplied, reasonable but
not something the author said in so many words, and vetoable post-hoc
without it counting as reverting an Accepted decision). R6 itself,
read again with this distinction available, was a `[C]` ruling
wearing `[A]`'s clothing — the tag exists so that mistake doesn't
recur silently. Tags are provenance metadata, not a quality signal:
a `[C]` ruling is not weaker or more provisional than an `[A]` one
once accepted; it is only *more revisable* by a later author veto
without that veto needing to clear the "supersede, don't revert" bar
this file otherwise holds every Accepted decision to.

### Deviation record

None — this record is itself the first act taken under R30 (its own
commit is also its own push), so there is nothing yet to disclose
about R30's application beyond this record's own existence.

---

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

## ADR-0009 — CLI renamed `ehrt` ("e-heart"); base `cli`, project `ehrt-cli`; `sim-cli` deprecated

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-29.

### Context

The CLI has been `ehr` since ADR-0002's own H2 landing. The author's
own session prompt (2026-07-29) renames it: **R32** [A] — the CLI is
renamed `ehr` → `ehrt`, pronounced "e-heart." Rationale on record:
memorable, and `ehr` stays reserved for future payload-EHR tooling (a
separate, not-yet-built capability this workspace may grow later,
distinct from the testing tooling `ehrt` names). **R35** [C] — the base
`bases/ehr-cli` renames to `bases/cli` (namespace `ehrt.cli.*`,
avoiding the `ehrt.ehrt-cli` stutter a literal rename would have
produced), and the project `projects/tools-cli` renames to
`projects/ehrt-cli` (projects name deployables; the deployable is now
`ehrt`). **R33** [A] — `bases/sim-cli` (sim's own standalone CLI) is
deprecated, not removed: it keeps working, its own tests keep running,
but no user-facing doc mentions it and `AGENTS.md`'s dev path marks it
deprecated, with a named (not scheduled) retirement trigger.

### Decision

**Every surface renamed, mechanically, in one pass:** `bin/ehr` →
`bin/ehrt` (`git mv`, preserving the index's `100755` mode --
`bases/cli/test/ehrt/cli/executable_bits_test.clj`'s own generic
`bin/`-prefix check proved this without needing a special case).
`bases/ehr-cli` → `bases/cli`, `ehrt.ehr-cli.core`/`.help` →
`ehrt.cli.core`/`.help`. `projects/tools-cli` → `projects/ehrt-cli`,
`workspace.edn`'s own project key and `:alias` renamed to match. Root
`deps.edn`'s `:ehr` alias → `:ehrt` (`poly/ehr-cli` → `poly/cli`, `-m
ehrt.ehr-cli.core` → `-m ehrt.cli.core`); every other `deps.edn`
touching the old base/project name (`projects/ehrt-cli`,
`projects/conformance`, `projects/integration`) updated to match.
`.githooks/`, `.github/workflows/integration.yml`'s own `clojure
-M:ehr artifact fetch` calls, `Makefile`'s `cli-doc` target
(`ehrt.ehr-cli.help/write-cli-md!` → `ehrt.cli.help/write-cli-md!`),
`README.md`'s Quickstart fence and project map, `bin/quickstart-demo`,
`AGENTS.md`, `AUTHORS-GUIDE.md`, `SETUP.md`, `CONTRIBUTING.md` --
every one of these carries the CLI's real invocable name, so every one
needed the same rename, not a representative sample.

**The CLI's own rendered surface, not just its file paths.**
`bases/cli/src/ehrt/cli/help.clj`'s `cli-spec` map has a `:program`
key (`"ehr"` → `"ehrt"`) that `render-top-level` already correctly
read from `spec` rather than hardcoding -- but `group-section` and
`verb-section` (the same file) hardcoded the literal string `` `ehr `` in
every section heading, independent of `:program`. Left alone, the
`:program` rename would have produced a `docs/cli.md` whose synopsis
said `ehrt <group>...` while every section heading still said `` `ehr
gate` `` -- a real, user-visible inconsistency a file-path grep would
never catch, only found by reading the renderer's own output shape.
Fixed by renaming both hardcoded literals directly (not by switching
them to read `(:program spec)`, which would be a design change beyond
this rename's own scope). The table-of-contents anchor links
(`#ehr-<group>`, built from the same hardcoded prefix) needed the
matching fix for the same reason -- a markdown anchor is derived from
its heading's own rendered text, so a heading rename without an anchor
rename breaks every internal cli.md link silently. `docs/cli.md`,
`docs/use-cases.md`, `docs/pipeline.md`, `docs/operators.md`
regenerated via `make docsgen` after the source fixes (`use-cases.edn`
hand-edited first, per this file's own generated-vs-authored
discipline) -- never hand-edited.

**Historical prose left alone, deliberately.** Every mention of `bin/ehr`
or `ehr-cli` describing a *past* event -- `bases/cli/test/ehrt/cli/executable_bits_test.clj`'s
own account of the H2 carve's executable-bit incident, ADR entries
prior to this one, archived session prompts, `.agents/session-records/`
-- was left exactly as written. Those records are accurate about what
was true when they were made; rewriting them to say `ehrt` would
misrepresent history, the same discipline `notes/sim/`/`notes/tools/`
frozen provenance already enforces for a different class of file. Only
citations of *current* state (what the CLI is called *now*) were
renamed.

**CLI smoke baseline note.** No committed byte-baseline file for `ehr
help`/`ehr corpus operators` output exists anywhere in this workspace
(ADR-0002's own "byte-identical" verification was a comparison against
a live `stable-pre-monorepo` clone at verification time, not a stored
fixture) -- there is nothing to regenerate. This record is the
disclosure the session prompt asked for: any *future* comparison
against a pre-rename invocation is moot, because the pre-rename
invocation (`bin/ehr ...`) no longer exists; `bin/ehrt help` and `bin/ehrt
corpus operators` are this rename's own fresh baseline, verified
below, not diffed against anything older.

**Sim-cli deprecation (R33).** `AGENTS.md`'s "Landed so far" section
now states the deprecation and its retirement trigger directly;
`notes/facts-register.md` F2 carries the same trigger as a dated row.
`SETUP.md` and `CONTRIBUTING.md` -- both user-facing, both found
during this session's own sweep to still present `sim-cli` as the
example of "something this workspace builds" (stale even before this
session: `SETUP.md`'s own text said "once `bases/sim-cli` has landed,"
long since true) -- now point at `bin/ehrt sim run` instead.
`bases/sim-cli`'s own code, deps.edn, and tests are untouched: `poly
check`/`poly test` still exercise them, unconditionally, exactly as
before.

### Verification

`clojure -M:poly check`: green. `clojure -M:poly test :all
skip:integration`: 0 failures/0 errors (full run). `bin/ehrt help`:
exit 0 from the workspace root, real invocation, prints the renamed
`ehrt`-branded usage text. `make ci-parity`: fresh clone, cold cache,
green (recorded below in the deviation record with the actual run
numbers). `make docsgen`: regenerated all four derived docs cleanly,
zero hand-edits to any `docs/*.md` output.

### Deviation record

**The `group-section`/`verb-section` hardcoded-heading finding**,
already recorded above as part of the decision, since it's load-bearing
to what "every surface" actually meant for this rename, not an
incidental aside.

**`docsgen_test.clj`'s own fixture assertions needed updating, not just
its fixture data.** `test-cli-spec`'s own `:program "ehr"` value is
synthetic and doesn't need to match the real CLI name -- but two
assertions independently hardcoded the expected `` `ehr <group>...` ``
heading text (mirroring `docsgen.clj`'s own pre-fix hardcoding), so
they broke the moment `group-section`/`verb-section` were fixed to
emit `ehrt`. Fixed in the same commit; a reminder that a test can
duplicate a hardcoded literal independently of the code under test,
not just inherit it.

**`quickstart_fresh_test.clj`, `usecases_test.clj`, and
`corpus/spool_source_test.clj`'s own `bin/ehr`-shaped fixture strings
left unchanged, deliberately.** These are synthetic example data fed
into a generic line-matching/rendering algorithm that treats them as
opaque text -- not assertions about this repo's own real invocation
name, and not read by anything outside their own test. Renaming them
would cost real review attention for zero behavioral or documentation
value; named here as a considered choice, not an oversight the next
session should "finish."

---

## ADR-0010 — Documentation doctrine: audience-forked, user path complete at root

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-29.

### Context

`notes/docs-audit.md` (this same session, Phase 3) dispositioned all 76
files under root `docs/`, `components/sim/docs/`, and
`components/tools/docs/`: which move to a complete, history-free user
path at root `docs/`; which move to a maintainer path at `docs/dev/`;
which stay component-adjacent (needed only by someone working on that
specific component's own code); which merge (two glossaries, two
problem statements); which retire to `notes/` as frozen provenance
(stale copies, pre-positioning scratch material, and the two
now-superseded per-directory NAV `README.md` pages). This record is
the doctrine that audit's dispositions implement, and the standing
instruction for every doc landed after it.

### Decision

**Three classes of doc, one rule each.**

1. **User path (`docs/`).** Complete at this level — a domain expert or
   informaticist never needs to descend into `components/`, never meets
   Polylith, and never learns sim and tools were separate repos before
   this workspace existed (R34). Every doc here either teaches a task
   (`use-cases.md`, `simulate-your-facility.md`, `site-profiles.md`),
   is a reference a task points at (`cli.md`, `operators.md`,
   `locators.md`, `formats.md`, `judge-calibration.md`, `glossary.md`),
   or orients a reader who doesn't have a task yet (`what-is-this.md`).
2. **Dev path (`docs/dev/`).** For maintaining or extending this
   workspace. `architecture.md` (new this session) is its own map:
   bricks, projects, where the theory docs live. `positioning.md`
   (moved and revised), `way-of-working.md`, the Polylith migration
   brief, `notation.md`, `pipeline.md`, `components.md`,
   `engine-onboarding.md`, `source-sink-design.md`, and deprecation
   notices (`bases/sim-cli`, R33) all live here.
3. **Component-adjacent (`components/*/docs/`).** Stays exactly where
   it is — not a residual bucket, each doc earns this on its own terms
   (`notes/docs-audit.md`'s own reason column): sim's engine internals
   and theory docs, tools' `palgebra-design.md` and evidence-trail
   experiments, both components' `research/`, and the hand-authored
   docsgen sources (`pipeline.edn`, `use-cases.edn`, `signature.edn`) —
   material a contributor to that specific component's own code needs,
   that a user or general workspace maintainer never does.

**R34's history rule, applied literally.** The user path names no
Polylith term, no `components/` path, no pre-merge repo name as
architecture (citations of `ehr-testing-sim`/`ehr-testing-tools` as
*origin provenance* in ADRs and `notes/` are unaffected — this rule
governs the user path's own voice, not this workspace's historical
record). `docs/what-is-this.md` states the problem and what this
workspace does in the present tense; the two pre-merge problem
statements it merges are themselves the historical record now, retired
to `notes/sim/docs/`/`notes/tools/docs/`.

**Generated docs moved with their write-paths, in the same commit,
never hand-edited afterward.** `cli.md`, `operators.md`, `use-cases.md`
(user path) and `pipeline.md` (dev path, since it names catalytic/stage
jargon the equation notation defines, not a task doc) are `make
docsgen` output; their hand-authored sources
(`components/tools/docs/{pipeline,use-cases}.edn`) stay component-
adjacent. `docsgen.clj`/`pipeline.clj`/`usecases.clj`'s own `:out`
write-paths (via the `Makefile`'s targets) point at the new locations;
their own banner text and internal cross-references were audited and
fixed in the same pass — a banner claiming to be "generated from
docs/pipeline.edn" when the real source is
`components/tools/docs/pipeline.edn` is exactly the kind of stale
self-reference a doc move must not leave behind.

**Every internal link, script-verified, not eyeballed.** A link-
resolution script (Python, this session's own scratch tool, not
committed) walked every `[text](target)` in every file under `docs/`
plus the root `README.md`, resolved `target` relative to its own
file's directory, and flagged anything that didn't exist on disk.
First pass: 34 broken links (files that moved out from under a same-
directory link that used to work; source `.edn` cross-references whose
depth changed; two doc-move-independent, genuinely pre-existing dead
links to `.agents/memory/patterns.md`, never live-populated after the
merge, fixed to point at their real content's frozen-provenance
location, `notes/tools/agents/memory/patterns.md`). Second pass: 0.
This is the cold-reader check the session's own prompt asked for, run
as a mechanical walk rather than a manual click-through — stronger,
not weaker: a script doesn't get tired of checking link 40.

**Standing instruction: a new doc declares its row before it's
written.** User path, dev path, or component-adjacent — stated in the
PR or commit that adds it, not inferred later. A doc that doesn't
obviously fit one of the three rows is a signal to ask, not to guess
into `docs/` by default.

### Deviation record

**A CLI-name rename gap, found and closed while moving files, not
originally scoped to this phase.** ADR-0009's own rename swept
generated docs and code but not every hand-authored doc under
`components/tools/docs/` and `components/sim/docs/` — `locators.md`,
`formats.md`, `judge-calibration.md`, `notation.md`,
`source-sink-design.md` still said `ehr corpus mutate`/`` `ehr` `` in
prose, untouched because ADR-0009's own sweep scope was code and
generated output, not every hand-authored prose doc in the tree. Found
only because this phase's own file-by-file link audit required reading
each moved doc anyway; fixed in the same commit as the move, not
deferred to a third rename pass.

**`docs/locators.md`'s own source citations were stale by more than
this session's rename** — `src/ehr_testing_tools/locator.clj` and
`test/ehr_testing_tools/locators_doc_test.clj`, pre-Polylith paths
that never existed in this workspace at all (the pre-carve tools repo's
own tree, `src/ehr_testing_tools/` not `src/ehrt/tools/`). Fixed to
the real current paths (`components/kernel/src/ehrt/kernel/locator.clj`
post-ADR-0008, `components/tools/test/ehrt/tools/locators_doc_test.clj`)
while this page was already open for the CLI-name and path-depth
fixes above -- not a new sweep, a repair made cheap by already being in
the file for other reasons.

**Prose (non-link) staleness named and left, matching this workspace's
own established posture (ADR-0005, ADR-0008).** A handful of plain-text
`docs/source-sink-design.md`/`docs/components.md` citations inside
`use-cases.edn` and elsewhere reference old paths in running prose,
not inside a markdown link the cold-reader script could catch —
lower-value to chase than the linked and generated surfaces this
record's own verification actually covers, disclosed rather than
silently incomplete.

---

## ADR-0011 — Per-engine judge split: `judge-v2-hapi` and `judge-fhir-official`; `judge` keeps the verdict vocabulary

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-29.

### Context

ADR-0008 extracted `components/judge` out of `components/tools`,
landing two gate engines (`ehrt.judge.v2`, the in-process HAPI HL7v2
base-structural judge; `ehrt.judge.fhir`, the official HL7 FHIR
validator, pinned subprocess) and a shared verdict vocabulary
(`finding`/`report`/`verdict-cache`) together in one brick. A second v2
engine (NIST `v2-validation`, profile-aware, via CDC's
`lib-hl7v2-nist-validator` wrapper) is on the board as its own future
session (EXP-D3) — this session's own charge (author, 2026-07-29:
*"Let's extract the validators now... I like judge-v2-hapi etc."*) is
to build the per-engine seam EXP-D3 lands into, and to name it for the
translator-under-test use case this workspace is heading toward:
gating the SAME input through multiple named engines and comparing
their verdicts, which reads far more naturally as
`judge-v2-hapi/gate-file` vs. `judge-v2-nist/gate-file` than as two
functions sharing one brick, disambiguated only by a naming prefix.

### Decision

**Landing shape:** `components/judge-v2-hapi` (from `ehrt.judge.v2`),
`components/judge-fhir-official` (from `ehrt.judge.fhir`).
`components/judge` keeps its name and the vocabulary trio
(`finding`/`report`/`verdict-cache`). `judge-v2-nist` is explicitly NOT
created this session — EXP-D3 lands into this seam later, a separate
session.

**Census (namespace -> real callers, whole-tree grep, not prose) —
confirms the session's own premise rather than contradicting it:**

| Namespace | Real callers | Disposition |
|---|---|---|
| `ehrt.judge.v2` | `ehrt.judge.interface` only | -> `ehrt.judge-v2-hapi.v2` |
| `ehrt.judge.fhir` | `ehrt.judge.interface` only | -> `ehrt.judge-fhir-official.fhir` |
| `ehrt.judge.finding` | `ehrt.judge.v2` (**turned out unused, see deviation record**), `ehrt.judge.fhir`, `ehrt.judge.report`, `ehrt.judge.interface`, both engines' own test namespaces | stays in `judge` |
| `ehrt.judge.report` | `ehrt.judge.interface` only | stays in `judge` |
| `ehrt.judge.verdict-cache` | `ehrt.judge.fhir` only | stays in `judge` (see disclosure below) |
| `ehrt.judge.interface` | `ehrt.tools.interface`, `ehrt.tools.check`, `v2_contract_pairing_test.clj`, `check_test.clj` | narrows to vocabulary-only |

No engine-to-engine `:require` in either direction — confirmed, not
merely assumed. Every downstream consumer of the gate functions
(`bases/cli/src/ehrt/cli/core.clj`,
`projects/integration/test/ehrt/tools/contract_pairing_test.clj`) goes
through `ehrt.tools.interface` only, never `ehrt.judge.interface`
directly, which is why the zero-behavior-change contract below rests
entirely on `ehrt.tools.interface`'s own re-exported names, not on
`ehrt.judge.interface`'s.

**verdict-cache placement — disclosed, not silently resolved.**
`ehrt.judge.verdict-cache` has exactly ONE real consumer today
(`ehrt.judge-fhir-official.fhir`), which on its face argues for moving
it there alongside its only caller. Kept in `judge` anyway, per this
session's own ruling: it keys generically on engine name/version +
input content hash + argv shape, nothing FHIR-specific, and the
planned NIST v2 engine (EXP-D3) is its expected second consumer.
Disclosed here for the author to veto post-hoc if the single-consumer
fact changes the calculus.

**Superseded 2026-07-31 (author ruling, P2-4, review finding 7).** The
expected-second-consumer justification above did not materialize:
`judge-v2-nist` landed (ADR-0012, 2026-07-30) without ever touching
`verdict-cache` — `ehrt.judge-fhir-official.fhir` remains the sole
consumer. Ruled anyway to leave `verdict-cache` in `judge` for now
(fix-forward, not a code move) — the generic key shape argument above
still holds on its own, independent of consumer count, and a single
extraction 2 for a still-single consumer is deferred until one of two
concrete triggers fires: (i) a second real consumer actually appears
(not merely planned), or (ii) `judge`'s own tools-split (this same
review's §5.1(a), stage 3, which narrows `tools` to its domain and
would touch every judge-adjacent boundary at once — the natural point
to re-derive this placement alongside everything else moving). No code
changed by this ruling; `verdict-cache`'s existing tests and consumers
are unaffected.

**The HAPI FHIR/HL7v2 Maven-coordinate pair, moved on again.** ADR-0008
moved `ca.uhn.hapi.fhir/hapi-fhir-base` and
`ca.uhn.hapi.fhir/hapi-fhir-structures-r4` into `components/judge/deps.edn`
alongside `judge.fhir`, disclosing at the time that nothing in this
workspace `:import`s either class directly. That disclosure is carried
forward unchanged: the pair moved on to
`components/judge-fhir-official/deps.edn` with `judge.fhir` itself,
still with no live `:import` anywhere (re-verified, whole-tree grep,
this session). NOT dropped — "superseded requires a load-bearing
inventory" (ADR-0008's own phrase) — whether to drop them is named
here as an OPEN AUTHOR DECISION, not resolved by this session.
`ca.uhn.hapi/hapi-base`/`hapi-structures-v24` (the HL7v2 pair,
genuinely `:import`ed in `judge-v2-hapi.v2`) moved to
`components/judge-v2-hapi/deps.edn`. `org.clojure/data.json` moved from
`components/judge/deps.edn` to `components/judge-fhir-official/deps.edn`
alongside its one real consumer (`judge.fhir`'s own JSON
parse/serialize) — `metosin/malli` stays in `judge` (both `finding` and
`report` still use it).

**Interface simplification: unqualified `gate-file`/`gate-dir`, not
carried-forward qualification.** The `v2-gate-file`/`v2-gate-dir`/
`fhir-gate-file`/`fhir-gate-dir`/`fhir-gate-batch` qualification
(ADR-0002, restated ADR-0008) existed only to disambiguate two engines
sharing ONE interface (`ehrt.tools.interface`, then
`ehrt.judge.interface`). Each engine now has its own interface with
nothing left to collide against, so
`ehrt.judge-v2-hapi.interface`/`ehrt.judge-fhir-official.interface`
export plain `gate-file`/`gate-dir`(`/gate-batch`).
`ehrt.tools.interface` re-applies its OWN `v2-`/`fhir-` qualification
at its own re-export layer (now sourced from the two new interfaces
directly instead of from `ehrt.judge.interface`), so every name it
re-exports is byte-identical to before this session — the
zero-behavior-change contract lives entirely at that layer.
`ehrt.judge.interface` narrows to the verdict vocabulary only
(`Report`/`build-report`/`diff-reports`/`baseline-relative-report`/
`report-valid?`/`finding-valid?`), plus two NEW re-exports found
necessary only by running `poly check` after the move (see the
deviation record): `worst-of` and four `verdict-cache-*` functions.

**Test namespaces moved with their engines** (`ehrt.judge-v2-hapi.v2-test`,
`ehrt.judge-fhir-official.fhir-test`); vocabulary tests
(`report-test`, `finding-test`, `verdict-cache-test`) stayed in
`judge`. Both moved test namespaces still `:require [ehrt.judge.finding
:as finding]` directly (calling `finding/valid?`/`finding/valid-cause-pairing?`)
rather than through `ehrt.judge.interface` — `poly check` does not flag
this (Polylith's brick-isolation enforcement applies to `:default`/src
profile namespaces, not `:test`), so left as-is rather than rewritten
for its own sake; disclosed here as an observed asymmetry, not a
violation.

**Dependency wiring** follows the established flat, project-level
convention (ADR-0008's own deviation record: no component `deps.edn`
anywhere carries a `poly/X :local/root` entry for a sibling brick).
`poly/judge-v2-hapi` and `poly/judge-fhir-official` added everywhere
`poly/judge` already appears: root `deps.edn` (`:dev`/`:test`/`:ehrt`),
`projects/ehrt-cli` (including its `:coverage` alias's `-p`/`-s` path
lists), `projects/conformance`, `projects/integration`.
`ehrt.tools.lint`'s `target-2-deps-edn-paths` (the deps-lint mechanism
verifying a catalytic-resource `deps.edn` coordinate actually resolves)
widened to include both new components' `deps.edn` files, same
rationale ADR-0008 already used when it first widened this list.

**Verification.** `clojure -M:poly check`: green (after the deviation
record's own fix, below). `clojure -M:poly deps`:
`judge-fhir-official` -> `{judge, kernel}`; `judge-v2-hapi` -> `{kernel}`
(real, src) and `{judge}` (test-only — see deviation record);
`judge` -> `{kernel}` only; no engine-to-engine arrow either direction;
`tools` -> `{kernel, judge, judge-v2-hapi, judge-fhir-official,
palgebra, sim}`. `clojure -M:poly test :all skip:integration`: exit
code and full log captured directly (`> file 2>&1; echo EXITCODE:$?`,
no pipe) per the sim-sibling errata session's own `tail`-masks-exit-code
lesson -- `EXITCODE:0`, 20m35s, three projects (`conformance`,
`ehrt-cli`, `sim`), zero `FAIL`/error markers anywhere in the 1416-line
log beyond the expected `0 failures, 0 errors` on every namespace;
`ehrt.judge.verdict-cache-test`/`report-test`/`finding-test` (vocabulary,
stayed in `judge`) and `ehrt.judge-v2-hapi.v2-test`/
`ehrt.judge-fhir-official.fhir-test` (moved with their engines) all ran,
each project pulling in the `conformance` project's own brick list
confirming the wiring directly: *"Running tests from the conformance
project, including 7 bricks and 1 project: judge, judge-fhir-official,
judge-v2-hapi, kernel, palgebra, sim, tools, conformance."*
`bin/ehrt gate v2`, `bin/ehrt gate fhir --report`, and `bin/ehrt check`
re-run against the exact fixture set and commands
`notes/judge-engine-extraction-characterization.md` recorded before the
move: all three `--report` EDN files and all three stdout logs (module
the process's own PID-independent `EXIT_*` line) byte-for-byte
IDENTICAL to that baseline (`diff`, zero output on every one of the six
comparisons).

### Deviation record

**`poly check` found two real Polylith interface violations the
ruling's own census didn't anticipate, because they were legal before
this session and illegal after.** `ehrt.judge-v2-hapi.v2` and
`ehrt.judge-fhir-official.fhir` both directly `:require`d
`ehrt.judge.finding` (and `fhir` additionally `ehrt.judge.verdict-cache`)
— fine while all three lived in one brick, `Error 101: Illegal
dependency` once `v2`/`fhir` moved to their own bricks and `finding`/
`verdict-cache` stayed behind, since Polylith requires cross-brick
access to go through the target brick's own `interface` namespace, not
its internals. Resolved two different ways, by what the census under
Step 2 couldn't show (nothing calls a namespace it doesn't use):

1. `judge-v2-hapi.v2`'s own `:require` of `ehrt.judge.finding` turned
   out to be dead code — grepped for `finding/` call sites inside the
   file and found none; the `raw->finding` local function builds plain
   maps, no schema validation call. Removed the require entirely,
   rather than routing a genuinely unused import through an interface.
   Consequence, visible in `poly deps`: `judge-v2-hapi`'s only REAL
   (src) dependency is `kernel`; `judge` shows up only as a **test**-alias
   dependency (its own test suite's `finding/valid?` assertions) — a
   cleaner graph than the ruling anticipated, not a violation of it.
2. `judge-fhir-official.fhir` genuinely calls `ehrt.judge.finding/worst-of`
   and four `ehrt.judge.verdict-cache` functions (`cache-key`, `lookup`,
   `store!`, `default-cache-dir`) — real, load-bearing cross-brick
   calls now that `fhir` lives apart from `finding`/`verdict-cache`.
   Fixed by widening `ehrt.judge.interface` to re-export all five
   (`worst-of`, `verdict-cache-key`, `verdict-cache-lookup`,
   `verdict-cache-store!`, `verdict-cache-default-dir` — no collision
   with any existing export, left unqualified) and routing
   `judge-fhir-official.fhir` through that interface instead of
   `ehrt.judge.finding`/`ehrt.judge.verdict-cache` directly. This is the
   same class of trap ADR-0008's own deviation record named (a problem
   invisible to static census, caught only by actually running the
   tool) — recorded here per the same fix-forward-with-disclosure
   discipline, not folded silently into the interface-sizing section
   above as though it were foreseen.

**Pre-existing prose staleness found, not chased (out of this
session's own narrow docs-sweep scope).**
`components/tools/docs/pipeline.edn` (component-adjacent, not the
user path, not one of the two files this session's own Step 7 named)
cites `ehrt.tools.judge.fhir/verdict-mapping-version` — stale from
before ADR-0008 even (the real namespace has been `ehrt.judge.fhir`,
now `ehrt.judge-fhir-official.fhir`, since ADR-0008 landed). Named here
per ADR-0010's "declare doc rows before writing" discipline rather than
silently fixed outside this session's own declared scope.

**Found: three user-path docs DO name judge internals, contrary to
this session's own working assumption that the grep would come back
clean.** `docs/formats.md`, `docs/glossary.md`, and
`docs/judge-calibration.md` (all user path per ADR-0010's own
disposition) cite `ehrt.judge.fhir`/`ehrt.judge.v2` directly (as
"Schema:"/vocabulary citations for `Report`/`Finding`/`Verdict`/`Cause`
and, in `glossary.md`, as prose naming which library backs which
judge). Two of these citations are now stale by this session's own
move (`ehrt.judge.fhir` -> `ehrt.judge-fhir-official.fhir`,
`ehrt.judge.v2` -> `ehrt.judge-v2-hapi.v2`); the `ehrt.judge.report`/
`ehrt.judge.finding` citations in the same files remain accurate
(unmoved). Per this session's own Step 7 instruction ("if one does,
record it — do not silently fix") and ADR-0010's own "declare doc rows
before writing" discipline, this is recorded rather than resolved:
whether these three docs' own namespace citations count as the kind of
Polylith/internal detail R34 excludes from the user path, or as
legitimate API/schema reference material a user path doc may cite, is
an open author call this session does not make unilaterally. If the
author rules they should be fixed, the two stale citations are the
only ones that actually changed.

---

## ADR-0012 — `judge-v2-nist` adopts the NIST engine directly: msg-id contract, Cause growth, fixture provenance

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-30. msg-id `ex-info` mechanism (below) ratified `[A]` (ADR-0007 provenance tag), 2026-07-30, judge-v2-nist follow-through session.

**Note (2026-07-30, added by the judge-v2-nist follow-through session):**
this record shares its number with the frozen `notes/tools/ADRs.md`
ADR-0012 (the `ehr sim` mount's own pre-existing design, cited
origin-qualified throughout `notes/ADRs.md` ADR-0005 above). Per this
file's own preamble citation rule: a bare `ADR-0012` anywhere in this
workspace's live documents means *this* record; the tools-era one is
always cited as `notes/tools/ADRs.md` ADR-0012 or `tools/ADR-0012`.

### Context

ADR-0011 built the per-engine judge seam and named `judge-v2-nist` (NIST
HL7 v2 `v2-validation`, profile-aware) as the third sibling, explicitly
not landed that session — pending EXP-D3. EXP-D3 (2026-07-29) recorded
the six NIST-origin Maven coordinates resolve cleanly from NIST's own
Nexus (`hit-nexus.nist.gov`) and characterized the CDC wrapper's own
report-filtering behavior (`docs/experiments/EXP-D3-results.md`). A
Cowork cloud session (2026-07-30) then spiked the engine directly from
Clojure — execution-verified, not read-verified — and found a
Java-friendly synchronous API (`hl7.v2.validation.SyncHL7Validator`)
this workspace's own platform research doc
(`components/tools/docs/research/NIST-HL7v2-dev-test-platform.md` §D.5)
had not surfaced; its own notes and script are archived verbatim at
`components/tools/docs/research/judge-v2-nist-spike-notes.md` and
`judge-v2-nist-spike.clj`. This record is the landing session that
followed, same day, and the decisions it had to make that the spike
itself left open.

### Decision

**Direct engine, not the CDC wrapper — supersedes the platform research
doc's own §D.5 recommendation.** `components/judge-v2-nist` depends on
the three `gov.nist` coordinates (`hl7-v2-validation`, `hl7-v2-parser`,
`hl7-v2-profile`, all 1.7.3) directly; `gov.cdc:lib-hl7v2-nist-validator`
is never a dependency, only a worked-example citation. Reasons, in order
of weight: **(1)** the wrapper's own `ProfileManager.filterAndConvert`
keeps only 4 of the underlying engine's 8 classification strings
(`Error`/`Warning`/`Alert`/`Informational`), silently discarding
`Affirmative`/`Informational`-adjacent/`Specification Error` signal —
EXP-D3's own Round 3 measured this at 75.6% of raw findings dropped
against the wrapper's shipped baseline message, and `Affirmative` is
exactly the classification meaning "this optional/RE field is empty,
and that's fine," load-bearing for this workspace's own verdict policy
and the mutate↔judge alignment loop the module docstring names. **(2)**
the wrapper's own documented version drift (README claims 1.6.3, last
published POM pins 1.6.10, unreleased `main` pins 1.7.3 — platform
research doc §D.4) is a supply-chain smell this workspace does not need
to inherit once the underlying coordinates are directly resolvable.
**(3)** hit-nexus resolving cleanly (EXP-D3) removes the platform
research doc's own stated reason for preferring the wrapper (§D.2: the
NIST coordinates aren't independently resolvable) — that premise no
longer holds, so the recommendation built on it no longer holds either.
The one Scala surface the direct path crosses
(`scala.jdk.javaapi.CollectionConverters/asJava`, once, at validator
construction) is not enough interop friction to outweigh (1)–(3).

**msg-id contract: explicit when a profile declares more than one,
never picked implicitly.** `ehrt.judge-v2-nist.v2/execute` refuses (via
`ex-info`, `{:type :ambiguous-msg-id :msg-ids [...]}`) when the profile
bundle's own `:msg-ids` has more than one entry and the caller passed no
explicit `:msg-id` — a single-id profile needs no `:msg-id` at all.
Sorting (or any other implicit tie-break) was considered and rejected:
picking a message-id ordering by convenience would silently validate
against the wrong message type on a plural profile, exactly the kind of
caller mistake `judge-v2-hapi`'s own `gate-file` docstring already
distinguishes from an operational condition. Neither sibling engine
(`judge-v2-hapi`, `judge-fhir-official`) has a genuine precedent for a
caller-contract violation of this shape (both are throw-free,
result-not-throw throughout, and neither has a concept requiring
caller disambiguation) — this is the *fallback* case named in this
session's own decision procedure: no precedent existed, so this
executes ex-info (a programming defect in the *call*, not an engine
verdict about the message under test, must not masquerade as
`:rejected` or `:no-verdict`), and it is flagged here for author
ratification rather than treated as settled convention.

**Ratification (2026-07-30, `[A]`, ADR-0007 provenance tag — direct
author ruling, judge-v2-nist follow-through session).** The msg-id
`ex-info` mechanism above is ratified. The doctrinal basis is
`AGENTS.md`'s own pre-existing Result-not-throw carve-out — "Exceptions
are for programmer error only" — which this session's own sibling-engine
grep looked one layer below: it searched the two sibling *engines* for a
caller-contract-violation precedent and found none, but the precedent
was never going to live in an engine; it lives in the workspace-wide
Result-not-throw rule itself. An ambiguous `:msg-id` — the caller
failing to disambiguate a plural-id profile — is a defect in the
*call*, not an engine or data outcome, so the mechanism was
doctrine-consistent all along. Standing boundary, stated once so no
future engine re-litigates it: engine and data outcomes are values
(findings, verdicts, `:check-exception` captures, `kernel/error`
results); caller-contract violations are programmer error and fail fast
via `ex-info` with descriptive data.

**`ehrt.judge.finding/Cause` grows its second specimen:
`:profile-spec-error`.** The enum is now
`[:enum :terminology-suppressed :profile-spec-error]`. The NIST
engine's own `Specification Error` classification means the
conformance profile (Π) itself is defective (e.g. references a value
set that doesn't exist) — the criterion could not be fully applied to
the message under test, distinct from `:terminology-suppressed` (the
criterion is sound, an external resource is merely absent).
`judge-v2-nist.v2/interpret` returns `:cause :profile-spec-error`
directly for Specification-Error captures; the spike's own
`:proposed-cause` rider (a placeholder for exactly this ADR) is deleted,
and its one specimen test renamed and updated to assert the real cause.
Co-landed with `judge-v2-nist` itself in one commit, per this
workspace's own co-landing discipline (a new engine step's invariants
ship with it, not after).

**Fixture provenance: a stand-in, not this project's own profile.**
`components/tools/test-fixtures/v2-nist/` vendors CDC's own
`COVID19_ELR-v2.3.1` Π bundle (`PROFILE.xml`, `CONSTRAINTS.xml`,
`VALUESETS-disabled.xml` — as shipped, not renamed) plus one companion
ER7 message, from the author's local
`~/Documents/NIST/lib-hl7v2-nist-validator` clone (HEAD
`eeac90c5f88dca3018992005232acdf3da644d88`), Apache-2.0, full
provenance and per-file sha256s in that directory's own `NOTICE.md`.
This is an explicit stand-in until a project-authored IGAMT export
replaces it — `notes/facts-register.md` F8 (the IGAMT registration
disclaimer, captured verbatim 2026-07-29) names the derived-from/
modified-notice obligation that *future* export will carry; F8 does not
attach to this vendored CDC test resource directly (it is CDC's own
fixture, not an IGAMT export this project produced), and `NOTICE.md`
says so, so a future replacement session knows where that obligation is
recorded rather than re-deriving it.

**No-vendor posture reaffirmed; mirror/fork deferred, dated.** Jars are
never vendored into this repo — they resolve from `hit-nexus.nist.gov`
via each affected `deps.edn`'s own `:mvn/repos` entry (root, plus every
project whose own `deps.edn` resolves independently:
`projects/conformance`, `projects/integration`, `projects/ehrt-cli` —
`poly test :all` does not inherit root's `:mvn/repos`, a real finding
this session hit directly rather than one the spike's own wiring notes
anticipated) into the local `~/.m2` cache, matching the ADR-0005
amendment's (2026-07-24, `notes/tools/ADRs.md`) no-redistribution
posture, reaffirmed rather than revisited by this session. Mirroring
the six resolved jars into a `file://` repo (CDC's own pattern, named in
the spike's own notes) is deferred to a future session, not built or
scheduled here — noted as a real future risk (hit-nexus has no stated
SLA and changed operators, NIST → Prometheus Computing, August 2026)
but out of this session's own scope.

**Engine version reads "unknown" for this engine — a real, disclosed
finding, not a bug.** Unlike `judge-v2-hapi`'s HAPI jars (Maven-built,
carry `META-INF/maven/.../pom.properties`),
`gov.nist:hl7-v2-validation:1.7.3` packages no Maven metadata at all
(confirmed by direct jar inspection) — `v2/engine-version` correctly
falls back to `"unknown"`, the same fallback path judge-v2-hapi's own
`hapi-version` already has for exactly this case, not a defect
introduced by this landing.

**Interface re-export, CLI expansion deferred.** `ehrt.tools.interface`
re-exports `v2-nist-make-validator`/`v2-nist-gate-file`/
`v2-nist-gate-dir`, same qualification discipline ADR-0011 established,
with one documented signature difference: this tier's `gate-file`/
`gate-dir` take a validator-state map (from `make-validator`, built once
per Π bundle and reused across files, since context construction
dominates cost), not a bare path — Π is an input at this tier, not a
fixed dependency. A real `bases/cli` `gate v2-nist` verb (a bundle-dir
flag, validator-state caching across a single invocation) is deliberately
NOT built this session — real design work, not a re-export — and is
named here as follow-on, not silently dropped.

**Verification.** `clojure -M:poly check`: green throughout (each
addition verified incrementally: component landing, tools-interface
requiring the new component, the three affected projects'
`:mvn/repos`/`poly/judge-v2-nist` wiring). `clojure -M:poly test :all
skip:integration`: full log captured directly (`> file 2>&1; echo
EXITCODE:$?`, no pipe, per this workspace's own tail-masks-exit-code
lesson) — `EXITCODE:0`, 181 test namespaces, zero `FAIL`/error markers
beyond the expected `0 failures, 0 errors` on every namespace, up from
this session's own 177-namespace baseline (`judge-v2-nist`'s own two
new test namespaces). All six NIST jar sha256s re-verified against
`artifacts.lock.edn`'s existing EXP-D3 entries (resolved fresh via
hit-nexus into `~/.m2`, byte-for-byte match, dated verification line
appended to each entry's own `:license-note`).

### Deviation record

**`:mvn/repos` is not inherited from root `deps.edn` by `poly test
:all`'s own per-project resolution — a real finding, not anticipated by
the spike's own wiring notes.** The spike's own `NOTES.md` named adding
`:mvn/repos` "to the ROOT deps.edn" as the one step needed; that is
sufficient for `clojure -M:dev:test`-style invocations (which resolve
against root `deps.edn` directly) but not for `poly test :all`, which
resolves each of `projects/conformance`, `projects/integration`, and
`projects/ehrt-cli`'s own `deps.edn` independently. Found by actually
running the full suite after the tools-interface wiring landed (`poly
check` passed; `poly test :all` failed on artifact resolution) — fixed
by repeating the same `:mvn/repos` entry in each of the three affected
project `deps.edn` files, same discipline `poly/judge-v2-nist`'s own
local-root entry already needed at that same layer (ADR-0011's own
"flat, project-level convention" — no component `deps.edn` carries a
sibling `poly/X` entry; each project names every brick and every
external repo it needs directly).

**Measured engine-in-the-loop numbers matched the spike's finding
counts exactly, but not its verdict/cause.** The spike's own `NOTES.md`
predicted `:no-verdict/:terminology-suppressed` for the COVID19_ELR
fixture (473 findings: structure 441, value-set 28, content 4 — this
session's own measurement matches every one of those counts exactly,
confirming the wiring is unchanged). The measured *cause* is
`:profile-spec-error`, not `:terminology-suppressed` — not a wiring
discrepancy from the spike, but a direct, expected consequence of this
same session's own Cause-growth decision above: the spike's code
returned `:terminology-suppressed` for Specification-Error captures
only because `:profile-spec-error` did not yet exist in the shared
enum at spike time; `interpret` returns the real cause now that it
does. Recorded here so a future reader comparing this session's pinned
test numbers against the spike's own prose doesn't mistake the cause
difference for an unexplained divergence.

**Fixture layout matched the spike's own description exactly.** CDC's
`COVID19_ELR-v2.3.1` bundle, as found in the author's local clone,
carries exactly `PROFILE.xml`, `CONSTRAINTS.xml`, and
`VALUESETS-disabled.xml` — no `VALUESETBINDINGS.xml`/
`COCONSTRAINTS.xml`/`SLICINGS.xml`, matching the spike notes' own
"Wiring into the workspace" step 5 description with no deviation to
record on this axis.

**`projects/ehrt-cli`'s own `:coverage` alias widened for consistency,
beyond this session's own literal step list.** The step list named only
root `deps.edn`'s `:dev`/`:ehrt`/`:test` and the three projects'
`:deps`/`:mvn/repos`; `:coverage`'s own `-p`/`-s` path lists (measure-
and-report, ADR-0004 posture, no enforcement gate) were widened to
include `components/judge-v2-nist/{src,test}` anyway, matching every
sibling engine's own existing entries there — mechanical, low-risk,
and leaving the new component invisible to coverage measurement seemed
a worse default than the small addition.

**CLI/help.clj gate-verb expansion, named but not built.** Per this
session's own step 7 permission ("if that expansion balloons... skip
it, note it as follow-on work"): a real `gate v2-nist` CLI verb needs a
profile-bundle-dir flag this tier doesn't share with `gate v2`/`gate
fhir` (both take a bare PATH) and validator-state reuse across a single
CLI invocation (building a fresh `SyncHL7Validator` per file would
defeat the whole point of `make-validator`'s own "build once per
bundle" discipline) — real design work belonging to a future session,
not a mechanical re-export.

### Ruling — 2026-07-31 (judge-family parity pass, P2-2)

The 2026-07-30 refactoring review (`notes/2026-07-30-refactoring-review.md`,
finding 6) found `gate-file`/`gate-dir` asymmetric between the two live
v2 engines: this ADR's own `gate-file` threw a raw
`FileNotFoundException` across the component interface on a missing
path (`judge-v2-hapi/gate-file` returned `kernel/error :file-not-found`
instead); `gate-dir` returned a bare `{filename result}` map with no
kernel envelope and walked recursively (`file-seq`), where
`judge-v2-hapi/gate-dir` returned `kernel/ok {:results [...]}` and
walked flat (`.listFiles`). Ruled (author, 2026-07-31, P2-2/AR-1):

- **Recursive is the shared rule for every engine's `gate-dir`.**
  `judge-v2-nist`'s own `file-seq` behavior is the standard;
  `judge-v2-hapi/gate-dir` changed to match (its `hl7-files-in` now
  walks `file-seq` instead of `.listFiles`) — a deliberate behavior
  change, not a bug fix, pinned by a cross-engine contract test
  (`projects/conformance/test/ehrt/tools/judge_engine_parity_test.clj`)
  against a fixture tree with one nested subdirectory.
- **Both engines return the kernel envelope from both functions.**
  `judge-v2-nist/gate-file` now returns `kernel/ok {:verdict :findings
  :path [:cause]}` or `kernel/error :file-not-found` (never throws);
  `judge-v2-nist/gate-dir` now returns `kernel/ok {:results [...]}` —
  the same shape `judge-v2-hapi` already produced.
- **The `bases/cli` compensating adapter simplified accordingly.**
  `v2-nist-gate-file*`/`v2-nist-gate-dir*` (`bases/cli/src/ehrt/cli/core.clj`)
  dropped their own `.isFile` pre-check and hand-rolled fail-fast
  directory composition — they now delegate straight to the engine,
  catching only the engine's own `:ambiguous-msg-id` ex-info. The CLI's
  missing-file exit code and message are unchanged from the user's
  perspective (pinned by
  `v2-nist-gate-command-missing-file-is-a-named-error-not-a-crash-test`,
  `bases/cli/test/ehrt/cli/core_test.clj`).
- Co-landed: `judge-v2-nist`'s own component test
  (`v2_engine_test.clj`) now validates its real-engine findings against
  `ehrt.judge.finding/Finding` and `valid-cause-pairing?`, giving it the
  test-tier dependency on `judge` finding 6c named as missing —
  mirroring what `judge-v2-hapi/v2_test.clj` already did.

### Ruling — 2026-07-31 (NIST artifact channel, P2-3)

The 2026-07-30 refactoring review (finding 8) found the six NIST jars
above resolving through *two* channels at once: real classpath loading
goes through `deps.edn`'s `:mvn/repos nist-hit` entry (into `~/.m2`,
confirmed working — the integration lane exercises the engine live from
there), while the same six coordinates also carry
`artifacts.lock.edn` rows that `ehrt doctor`'s `check-artifact-cache`
expected to be fetched into the content-addressed artifact cache —
failing on any machine where the engine itself ran fine, contradicting
this ADR's own engine-onboarding checklist item 4 ("resolves to exactly
one of" the three lockfile targets). Ruled (author, 2026-07-31, P2-3/AR-2,
option (a) of the two named in the review): the six rows **stay** in
`artifacts.lock.edn` as provenance/license records — the
`:use-permitted--unstated--confirmation-pending` posture and its
evidence trail live there, not scattered into `deps.edn` comments — and
each gains `:resolved-via :deps-edn`
(`components/kernel/src/ehrt/kernel/artifact.clj`'s `Artifact` schema,
optional key, default implied `:artifact-cache` when absent).
`check-artifact-cache` (`bases/cli/src/ehrt/cli/core.clj`) skips
cache-checking any row so marked, and says so in its `:detail` line —
the rows remain listed by name/version, just never asked whether
they're in `~/.m2`'s sibling cache. Recorded in
`docs/dev/engine-onboarding.md` checklist item 4 as a dated note.
The spike's own file://-mirror end-state (vendoring the six jars the
way CDC's own wrapper does, named as a future risk in this ADR's own
"Fixture layout" deviation-record entry above, given `hit-nexus`'s lack
of a stated SLA) remains open and unaffected by this ruling — it would,
if built, flip these rows back toward `:artifact-cache`.

---

## ADR-0013 — Output UX doctrine: single `out/` root, artifact-vs-display boundary (the TTY rule), the `show` verb, jet/`--json` surfacing

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-30.

**Note (2026-07-30, added by this same session, per ADR-0012's own
precedent):** this record shares its number with the frozen
`notes/tools/ADRs.md` ADR-0013 (the cross-repo consumer loop: sim
consumed by subprocess, findings not failures, baseline-delta drift
detection). Per this file's own preamble citation rule: a bare
`ADR-0013` anywhere in this workspace's live documents means *this*
record; the tools-era one is always cited as `notes/tools/ADRs.md`
ADR-0013 or `tools/ADR-0013`.

### Context

Two end-user complaints drove this session, both raised in chat,
2026-07-30. **(1)** The tool's output directories are unintuitive:
`target/` is where every zero-flag default writes
(`target/corpus/synthea-…`, `target/spool/…`, `target/gate-fhir`) and is
gitignored, while `out/` exists only as the path `docs/use-cases.md`
teaches users to type — created by no default, and NOT gitignored — so
following the quickstart with an explicit `--out-dir out/...` dirties
`git status` the moment a reader instead runs a zero-flag command and
gets `target/` back. The two names are one concept split by accident of
provenance (`target/` inherited from JVM build tooling convention,
`out/` invented for the docs): under the determinism doctrine
(everything derived is reproducible from seeds and inputs, `tools/ADR-0019`
via `docs/source-sink-design.md` D9) there is no second purpose either
name is protecting. **(2)** Users who run gates get a raw EDN envelope
on stdout, don't recognize EDN, and can't `jq` it — while `--json` has
existed on every command all along (`docs/formats.md`). A
discoverability defect, not a capability gap. A third, smaller finding
surfaced in the same session: ER7 content is unreadable in a terminal
because segment separators are bare CR, with no display verb to make it
legible.

One prior design constrains the shape of the fix to (2)/(3): the corpus
player named in the founding design chat (2026-07-27, unarchived —
cited here by the decisions it settled) decomposes into four separable
parts — an **input adapter** (something → a time-ordered event stream),
a **pacer** (stream-time → wallclock at rate R, with idle-skip), an
optional **accumulator** (the M6 v2-replay accumulator, already tested
code), and **sinks**, of which a **ticker** (one line per event) is the
primary visual one and paced file/MLLP emission is the non-visual
sleeper that makes the player a load/soak instrument. `ehrt show`,
below, is built as the ticker sink at infinite rate with no pacer — not
a rival mechanism the player will later have to absorb.

### Decision

**[A] Single tool-owned output root, named `out/`.** The docs-facing
name wins over the JVM-conventional one: it is what `docs/use-cases.md`
already trained users to type, it is self-explanatory to the
non-Clojure audiences `docs/dev/positioning.md` names, and it separates
`ehrt`'s own data from build tooling's own `target/` (poly/clojure
artifacts, lint caches). Doctrine sentence, verbatim, into this record
and into the user docs it touches:

> ehrt writes only under `out/` unless you pass `--out-dir`; `out/` is
> ignored and always safe to delete — everything in it is reproducible
> from seeds and inputs; `target/` belongs to the build.

Substructure: `out/corpus/` (generated corpora, `corpus generate` and
the `synthea:`/`sim:` generator-URL kinds), `out/spool/` (SS-3's spooled
stdin capture), `out/scratch/gate-fhir` (the FHIR validator's scratch
directory, renamed from a bare `gate-fhir` to name what it is now that
it sits under a shared root with siblings). `out/` enters
`.gitignore`. The mutate default (`<PATH>-mutants/…`, input-adjacent by
design, D12) is **not** moved — it derives from the input path, not
from a tool root, and the two are orthogonal: `out/` names where a tool
invents a fresh location; `<PATH>-mutants/` names a location relative
to something the caller already gave it. Nothing about this doctrine
touches the `--out-dir`/`--out` escape hatch on any command: an
explicit flag still wins outright, exactly as before.

**[A] The EDN envelope is no longer the unconditional stdout default.**
Rule: if stdout is a live terminal, default to human-readable rendering
(`--pretty` behavior, below); if stdout is a pipe or redirect, default
to the EDN envelope exactly as today. Doctrinal basis, stated here so
no future session mistakes this for a collision with `corpus.generate`'s
own refusal of ambient-dependent defaults (D9's determinism law): that
law governs **artifacts** — files, `--report` output, and any piped or
redirected bytes, because those are read by another program or kept as
a record, and a record must not vary with who's watching. A live
terminal is a human (or an assistant driving a shell for one), not an
artifact; interactive rendering is outside the doctrine's scope for the
same reason `ehrt help`'s own plain-text exception already sits outside
it (`bases/cli/src/ehrt/cli/core.clj`'s ns docstring). The sniff
therefore biases conservative: any doubt — either stream redirected, no
console attached at all — resolves to the machine format, so no
downstream consumer ever silently receives sniffed variance because of
where or how the process happened to run.

**[A] Flag precedence over the sniff.** `--pretty` forces human
rendering even into a pipe; `--edn` forces the raw envelope even at a
terminal; `--json` behaves exactly as it does today (a projection of
whichever envelope form was chosen — see the pretty/JSON interaction
below). Sniffing applies only when none of the three is given.
`--report` files are untouched by all of this: always EDN, always the
bare report (no `:status`/`:payload` wrapper), per `docs/formats.md`'s
existing, unchanged contract — the TTY rule governs stdout only.

**[A] Pretty means, by command class.** Envelope-emitting commands
(`gate`, `generate`, `mutate`, `intake`, and kin) render a compact
human summary, never a prettified EDN envelope — the envelope is the
machine form, full stop, and pretty is a different rendering entirely,
not indentation applied to the same data:

- **`gate`** (and `check`, same report shape): one verdict line per
  file, then aggregate finding counts by code, then any paths actually
  written (`--report`'s own path, when given) — a human scanning this
  can tell what happened without reading a single brace.
- **Every other envelope command** (`generate`, `mutate`, `intake`,
  `artifact fetch`/`resolve`, `version`, `doctor`): a brief generic
  rendering of `:status`/`:category` plus the payload's key counts and
  paths (whatever it already carries — a file count, an out-dir, a
  cached flag), plus one hint line naming both `--edn` and `--json` for
  the full envelope.

Tailoring beyond `gate` is a **permitted skip**: where a command's
payload resists a sane generic summary (deeply nested, no obvious
counts), the hint line plus a pretty-printed payload is the fallback,
recorded as this ruling's own named allowance rather than silently
shipped as a polish gap.

**[A] `ehrt show PATH`.** A new display verb, pretty-always regardless
of stdout's destination — its entire job is rendering for eyes, so
`ehrt show foo.hl7 | less` must work with no flag at all. It joins
`gate`'s own D11 sniff-format dispatch (`corpus.intake/sniff-format`):
ER7 renders one segment per line (CR → LF, trailing separator
stripped), with a blank line between messages; FHIR JSON renders
pretty-printed. **Display renderings are not wire format** — LF-joined
ER7 segments are nonconformant ER7 by construction, and that is
correct: the eyes/pipes split is structural (a distinct verb), never a
flag bolted onto a wire-emitting path that would tempt a caller into
piping `show`'s own output somewhere a real HL7 v2 consumer sits. `show`
never modifies the file it reads; it is read-only by construction, not
merely by convention.

`show` is designed, in its code shape, as the corpus player's ticker
sink at rate ∞ (Context, above) — not a mechanism the player will later
have to reimplement or absorb. The render function is **per-message**:
one ER7 message in, one rendered block out, with no knowledge of a
stream. The stream-level concerns — splitting the input into messages,
mapping the renderer over them, joining with blank lines — live in
`show`'s own thin CLI-adjacent layer, never inside the renderer. This
is the exact call shape a future pacer will need: call the renderer
once per event, at whatever cadence the pacer computes, with no
stream-splitting logic to route around. The player itself — the
pacer, the accumulator wiring, bed-board/census sinks, paced
file/MLLP emission — remains future work per the founding chat's own
ruling; this record only keeps `show`'s internals from foreclosing it
by accident.

**[A] jet and `--json` discoverability.** `docs/formats.md` gains a
"Reading these from a shell" section, sibling to its existing "Reading
these from Python": `--json | jq` as the zero-install route for the
EDN-projected-to-JSON path; `jet` (borkdude/jet) named as the
EDN-native equivalent — querying EDN directly, or converting an
existing `--report` EDN file to JSON for `jq` without a full rerun.
One-line mentions land in `README.md`'s Quickstart, the first gate strip
in `docs/use-cases.md`, and `ehrt help`'s own top-level doc line (which
flows into `docs/cli.md` via regeneration, `make cli-doc`).

**[A] One combined capture-and-build session, unattended (R30).**

**[C] The TTY probe is an injected seam, not a scattered ambient
call.** `bases/cli/src/ehrt/cli/core.clj`'s `main!` already injects
`:println-fn`/`:exit-fn`; this record adds `:tty?-fn` to the same map,
defaulting to a real check — `(some? (System/console))`, the classic
JVM idiom, chosen because its property of returning `nil` the moment
either stream is redirected is exactly the conservative bias the TTY
rule calls for. Tests pin both branches deterministically by injecting
the seam; the sniff is ambient only in real, un-instrumented use.

**[C] `docs/cli.md` is generated, never hand-edited.** Any doc claim
this session makes about a default or a flag comes from `make cli-doc`
regenerating it from `bases/cli/src/ehrt/cli/help.clj`'s own spec, not
from a manual edit to the rendered file.

**Frozen-era default strings, superseded in behavior, not in text.**
`tools/ADR-0019` (`notes/tools/ADRs.md`) and D9
(`docs/source-sink-design.md` Part IX.2) are the frozen/live records
that established `target/…` as the zero-flag default family this record
now moves to `out/…`. Every live citation of D9/`ADR-0019` in
`bases/cli/src/ehrt/cli/help.clj`, `core.clj`, and
`components/tools/src/ehrt/tools/corpus/generate.clj` names the
*determinism-of-defaults doctrine* those records state, which this
record does not revise — only the one concrete default-path family that
doctrine happened to produce, which this record supersedes in
behavior. Frozen `notes/tools/ADRs.md` itself is never edited; live
docstrings citing `ADR-0019` by bare number are left as-is (they remain
accurate about the doctrine) rather than swept for qualification as a
side effect of this session's own default-path change — a future
citation-hygiene pass, not this one, is the right place to qualify
every bare frozen-era reference workspace-wide.

### Alternatives rejected

*Naming the shared root `target/` instead of `out/`* — `target/` is
already the JVM/Clojure build-tooling convention (compiled classes,
poly/lint caches) and overloading it with `ehrt`'s own run output would
re-introduce exactly the ambiguity this record exists to remove, just
under the other name. *Making `out/` opt-in via a flag rather than the
default* — the whole complaint is that the zero-flag path and the
docs-taught path disagree; a flag a reader has to already know to pass
doesn't fix a discoverability gap, it relocates it. *Sniffing pretty-vs-EDN
inside `--report`/file-writing paths too* — the determinism doctrine
governs artifacts precisely because they're read by something other
than the terminal that produced them; making a `--report` file's shape
depend on how it happened to be invoked would reintroduce the sniffed-variance
hazard the conservative bias exists to prevent. *A flag instead of a verb
for `show`* — `docs/formats.md`'s own display-vs-wire doctrine (this
record) needs a structural boundary a caller cannot accidentally pipe
into a wire-format consumer; a `--pretty`-style flag on `gate`/`generate`
already exists and means something different (a summary of a Result
envelope), and reusing it for "render the file's own bytes for a human"
would conflate two unrelated meanings under one name.

### Consequence

Every command's zero-flag output moves under `out/`, `.gitignore` gains
one entry, and `docs/use-cases.md`'s existing strips (which already
spell explicit `--out-dir out/...` throughout) need no strip-content
edits — only the default-path prose describing what happens with no
flag at all. Every envelope command gains a human-facing rendering path
with no change to its EDN/JSON contract for anything already piping or
redirecting it. `ehrt show` is new surface, zero interaction with any
existing verb's exit-code or output contract. The corpus player
(`docs/dev/` design lineage, founding chat 2026-07-27) inherits a
tested, per-message renderer and a message-splitter it can reuse rather
than reinvent, once a future session builds the pacer around it.

**Status.** Accepted (author-directed, autonomous session per R30), 2026-07-30.

---

## ADR-0016 — `tools` split stage 1: `docs-tooling` extracted, `:necessary` re-derived

**Status:** Accepted (author-ruled 2026-07-31, session executed same day).

### Context

`notes/2026-07-30-refactoring-review.md` §5.1(a) proposed a staged
split of `components/tools` — a corpus domain component and the CLI's
universal façade wearing one name — into three independently-landable
extractions, using the method ADR-0011 already proved
(characterize → extract → verify byte-identical behavior → `poly
check` green). This record executes stage 1, the smallest and
safest: `docsgen`, `usecases`, `pipeline`, `quickstart-fresh`, `lint` —
dev-time-only doc/lint tooling, named by the review as "the sole
source of the `tools → palgebra` src edge" and the thing finding 14's
undocumented third `:necessary` entry (`integration`) hangs on.
Stages 2 (`corpus-io`) and 3 (narrow `tools` to its domain) are ruled
but not executed here.

Session start: the WSL ext4 clone was already at `origin/main`
(`9f7f57c`, no fast-forward needed). The 2026-07-31 "ruled P2 batch"
session (judge-family parity, NIST artifact channel, verdict-cache
note) had already landed as that same commit, `P2 batch.` — this
session builds on top of it, not around it.

### Decision

**Landing shape**, per author ruling on names (docs-tooling,
`ehrt.docs-tooling.*`) and on `tools` retiring after repoint rather
than surviving as a permanent façade:

```
components/docs-tooling/  -- src/ehrt/docs_tooling/{docsgen,usecases,
                             pipeline,quickstart_fresh,lint,interface}.clj,
                             test/ehrt/docs_tooling/{same + stale_path,
                             structure_currency}_test.clj, deps.edn
                             ({:deps {metosin/malli ...}})
components/tools/         -- loses the five namespaces; gains
                             operators_doc.clj (see split, below) and
                             operators_doc_test.clj
```

**Two escalations fired during Step 2, both resolved by author
decision, both documented here since AR-5 explicitly asked for
"stop and name the def" rather than silent resolution:**

1. **`ehrt.tools.interface`'s `write-cli-md!` genuinely delegates to
   `docsgen`** (AR-5's own anticipated check) — `bases/cli/help.clj`
   is a real, live caller, not a grep false positive. First ruling:
   keep the re-export, now delegating to
   `ehrt.docs-tooling.interface`.
2. **That ruling, combined with `docsgen.clj`'s own real dependency on
   `corpus.operators` (the operators.md-rendering half) and
   `lint.clj`'s real dependency on `corpus.canonicalizers`/
   `corpus.framing`/`corpus.operators`/`check.schemas`, produced a
   genuine circular *component* dependency** (`tools → docs-tooling →
   tools`) — `clojure -M:poly check` Error 104, a hard Polylith
   constraint (two bricks may not depend on each other, regardless of
   which specific namespaces create each edge), not a style
   preference discoverable by inspection alone. Second ruling (this
   is the one that landed): `bases/cli/help.clj` calls
   `ehrt.docs-tooling.interface/write-cli-md!` **directly**, bypassing
   `tools` entirely; `ehrt.tools.interface` drops `write-cli-md!`
   altogether. `docsgen.clj` itself split in two along its own
   pre-existing internal seam (it already had zero shared state
   between its cli.md half and its operators.md half, just shared
   pure helper functions): `render-cli-md`/`write-cli-md!` (pure, no
   tools dependency) moved whole to
   `ehrt.docs-tooling.docsgen`; `sorted-entries`/`render-operators-md`/
   `write-operators-md!` (needs the live `corpus.operators` registry)
   stayed in `components/tools`, renamed `ehrt.tools.operators-doc`.
   Both halves carry their own duplicate copy of the four small pure
   markdown-table helpers (`banner`/`escape-cell`/`table`/
   `exit-code-table`) the original file shared between them —
   duplicated rather than shared through an interface, since sharing
   them would reintroduce the same class of coupling the split exists
   to avoid, for four small pure functions. `lint.clj` keeps its own
   real need for tools' registries, routed through
   `ehrt.tools.interface` (three new exports: `check-schemas-lookup`,
   `framing-lookup`; `lookup`/`operators/entries` already existed) —
   this edge runs one direction only (`docs-tooling → tools`) and
   never touches `write-cli-md!`'s own removed edge, so it doesn't
   reintroduce the cycle.

**Test placement (AR-3).** All seven moved test files
(`docsgen_test` split the same way as its namespace; `usecases_test`,
`pipeline_test`, `quickstart_fresh_test`, `lint_test`, and the two
2026-07-31 doc-enforcement additions `stale_path_test`/
`structure_currency_test`, which carry no code dependency on any of
the five namespaces but move with their own docs-enforcement
machinery per this ruling) now live under
`components/docs-tooling/test/`. Default placement:
`projects/conformance` gains a real `poly/docs-tooling` dependency —
the only project that hosts docs-tooling's own tests. `bases/cli`
gains a real, direct dependency on `docs-tooling` (for
`write-cli-md!`); `projects/ehrt-cli` gains `poly/docs-tooling`
correspondingly. `projects/integration` does **not** gain
docs-tooling — nothing there needs it, and (see below) it lost its
own reason to need `palgebra` at all.

**`:necessary` re-derivation (AR-4, finding 14).** Method: `clojure
-M:poly deps` for the real brick-edge matrix, then `clojure -M:poly
check` with every `:necessary` entry in `workspace.edn` temporarily
cleared to `[]`, once, to see exactly what poly's own brick-reachability
check (real edges from a project's base, or between bricks the same
project declares — it does not see a project's own ad hoc test-tree
requires) considers unreachable without the override:

| Project | Before | After | Why |
|---|---|---|---|
| `ehrt-cli` | `["palgebra"]` | *(no `:necessary` key)* | `bases/cli` now directly requires both `ehrt.tools.interface` and `ehrt.docs-tooling.interface`; every other declared brick, palgebra included, is real-edge-reachable transitively through those two (confirmed: zero warnings with `:necessary []`). |
| `conformance` | `["tools" "palgebra"]` | `["docs-tooling"]` | `tools` and `palgebra` are now reachable via docs-tooling's own real edges (`docs-tooling → tools`, `docs-tooling → palgebra`) once docs-tooling itself is in the project; docs-tooling itself has no real incoming edge from anything else conformance declares (it's included solely to host its own moved tests), so it alone needs the override. |
| `integration` | `["tools" "palgebra"]` | `["tools"]` | `palgebra` dropped from the project's `:deps` entirely, not just the override — grep-confirmed nothing under `projects/integration/test/` requires `ehrt.palgebra`, and `tools` no longer requires it either (the edge moved to docs-tooling, which integration deliberately does not include). `tools` itself still needs the override: nothing else integration declares has a real edge to it, only its own test tree's direct requires (poly-invisible to this check, real at runtime). |

Confirmed: `clojure -M:poly check` — `OK`, zero warnings, with the
table above as the final `workspace.edn` state.

### Verification

Characterization baseline (before any Step 2 edit, HEAD `9f7f57c`):
fresh `make docsgen` exit 0, generated docs byte-identical to
committed (sha256 recorded); `make quickstart-fresh` exit 0 (15
commands agree); `make lint-pipeline` exit 0; per-push lane
(`skip:integration`) exit 0, 191 `Testing ehrt.*` namespaces, 0
failures/errors, all seven moving test namespaces present (each
running twice — once per composing project, `ehrt-cli` and
`conformance`). `make quickstart` exits 2 (`bin/quickstart-demo`
permission denied) — a **pre-existing, unrelated** exec-bit drop on
disk (git's index still says mode 100755; the working-tree file was
644), confirmed before any stage-1 edit and flagged separately
(spawned task, not fixed here — outside this stage's own fence).

Post-move verification, same commands: fresh `make docsgen` exit 0;
`docs/dev/pipeline.md` and `docs/use-cases.md` byte-identical to
baseline; `docs/operators.md` and `docs/cli.md` each differ by
exactly the renderer's own "GENERATED... edit this file instead"
banner line, now correctly citing the renderer's new true path — the
deliberate, correct consequence of the move itself, not drift (every
other line, byte-identical). `make quickstart-fresh`/`make
lint-pipeline`: same output modulo the invoked namespace's own name in
the echoed command line, same `OK` message, exit 0. `clojure -M:poly
check`: `OK`. Per-push lane: exit 0, **193** `Testing ehrt.*`
namespaces (191 baseline **+2**, exactly and only from the `docsgen`
split: `ehrt.docs-tooling.docsgen-test` and
`ehrt.tools.operators-doc-test` are two real namespaces where one
(`ehrt.tools.docsgen-test`) stood before, each running in the same two
composing projects — `ehrt-cli` and `conformance` — every other
existing namespace already ran in, so 1 namespace → 2 namespaces × 2
projects = a net +2 total occurrences, confirmed by grep count, not
inferred), 0 failures/0 errors, every moved test namespace present
under its new `ehrt.docs-tooling.*` name (each of the seven appearing
twice, once per composing project — same duplication pattern the
baseline already had for the un-split six).

**Follow-up, same day, before commit: `make quickstart` fixed and
verified green.** The exec-bit drop named above was pre-existing and
outside this stage's own fence, so it was left unfixed through Step 4;
the author separately authorized fixing it and committing this stage
in the same follow-up. `chmod +x bin/quickstart-demo` on the WSL ext4
clone restored the bit; `git status` showed zero change from this
(the index already recorded mode 100755 — it was always correct, only
the working-tree file had drifted). The `/mnt/c` clone's own copy was
checked and found already executable (drvfs reports it `-rwxrwxrwx`);
untouched. A full `make quickstart` re-run afterward: every taught
command passed with its expected exit code (`help`; both `corpus
generate` runs; all three `artifact fetch`es; `corpus mutate`; `gate
v2`; `gate fhir`, correctly rejecting, exit 1; `check`, 7/7 pass; `sim
run`; the closing `clojure -M:poly test :all`, 4m17s) — genuinely
green, functionally. The script's own final postcondition (`git
status --porcelain` must be empty) still exited the make target at 2,
because this stage's own not-yet-committed 36 files were sitting in
the tree at the time — confirmed, by diff, byte-identical to the
pre-run `git status`, so nothing the run itself wrote; a limitation of
running this postcondition against an intentionally uncommitted stage,
not a defect this stage introduced. Recorded here rather than
silently reclassified as unconditionally green.

### Deviation record

**AR-5's own anticipated escalation fired for real, and a second,
unanticipated one followed from it** — both resolved by author
ruling, both above (Decision, "Two escalations"). The first ruling
(keep `tools`'s re-export) was reversed by the second finding
(`poly check` Error 104) within the same session, before any test
ran against it — recorded as a genuine correction, not silently
dropped.

**A stale fully-qualified keyword literal, caught only by running the
suite, not by the rename sweep itself.**
`quickstart_fresh_test.clj:93` asserted
`(= :ehrt.tools.quickstart-fresh/missing (:script divergence))` — the
source's own `::missing` (an auto-namespaced keyword) resolves to
whatever namespace it's read in, so renaming `ehrt.tools.quickstart-fresh`
to `ehrt.docs-tooling.quickstart-fresh` silently changed the keyword
the test needed to assert against. `ns`/`:require` renames were swept
mechanically across all twelve moved files; this one hardcoded,
fully-qualified keyword literal in a test body was not caught by that
sweep — only by actually running the per-push lane, which is why
Step 4's own verification pass exists as a real command, not a
checklist item. Fixed to
`:ehrt.docs-tooling.quickstart-fresh/missing`; the same and one other
file's own docstring comments (`framing.clj:224,233`,
`usecases.clj:3`) citing the old qualified names by name were swept
in the same pass, found by a repo-wide grep after the fact rather
than anticipated in advance.

**`components/palgebra/test/ehrt/palgebra/deps_lint_test.clj`'s own
seeded-violation fixture string, `"ehrt.tools.lint"`, left
untouched.** It is arbitrary realistic-looking test data proving
palgebra's own `deps-lint` rule fires on a fake namespace string, not
a real citation of the namespace this session moved — renaming it
would not change what it tests and was outside AR-2's own "move,
don't improve" mandate.

**`notes/docs-audit.md`, a closed one-time disposition audit from an
earlier session, left untouched** despite citing the pre-split
`docsgen.clj`/`.usecases`/`.pipeline` paths — it is historical record
of a Phase-4 execution already completed, not a live "current state"
document this session's own read-first list named, and AGENTS.md's
own discipline-surface mapping does not list it among the "live,
current, edit freely" set. Named here per fix-forward-with-disclosure
rather than silently skipped.

**Named-future list, for stages 2/3 (not executed here):** the
duplicated pure markdown-table helpers between
`ehrt.tools.operators-doc` and `ehrt.docs-tooling.docsgen` (four small
functions, ~30 lines each copy) are a candidate for a shared
micro-namespace if a third consumer ever needs them, not before —
premature to extract for two. `components/tools`'s own interface
width (74 defs, unchanged by this stage) is untouched, per stage 1's
own fence. Stage 2 (`corpus-io`: source-sink/framing/player) and stage
3 (narrowing `tools` to its domain, retiring the façade) remain ruled,
unexecuted.

---

## ADR-0014 — Corpus player: pacer semantics, plan/execute time seam, cue rule extends artifact-vs-display; bed board and accumulator wiring deferred

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-30.

**Note (2026-07-30, added by this same session, per ADR-0012/ADR-0013's
own precedent):** this record shares its number with the frozen
`notes/tools/ADRs.md` ADR-0014 ("Intake learns optional manifest
sidecars, directory-scoped and generator-agnostic"). Per this file's
own preamble citation rule: a bare `ADR-0014` anywhere in this
workspace's live documents means *this* record; the tools-era one is
always cited as `notes/tools/ADRs.md` ADR-0014 or `tools/ADR-0014`.

### Context

The corpus player was named in the founding design chat (2026-07-27)
and given a four-part decomposition in ADR-0013's own player paragraph:
an **input adapter** (something → a time-ordered event stream), a
**pacer** (stream-time → wallclock at rate R, with idle-skip), an
optional **accumulator** (the M6 v2-replay state fold), and **sinks** —
a ticker (one line/block per event, the primary visual sink) and paced
file/MLLP emission (the non-visual sleeper that makes the player a
load/soak instrument). Three of the four parts already existed as
tested code by the time this session started: the message boundary is
`ehrt.tools.corpus.framing`'s own `:er7-multi` codec (the one splitter
in the codebase, ADR-0013); the ticker's rendering is literally
`ehrt.tools.display/render-er7-message`, built per-message for exactly
this reuse; and an accumulator already lives in the sim arc. This
session builds the one genuinely new mechanism — the pacer — plus the
two thin sinks that compose from already-landed parts: the ticker and
paced file emission. The bed board (a state-snapshot-at-intervals
surface), the accumulator's own wiring into the player, a sim
event-log input adapter, and where any of this should live relative to
a separate `ehr-testing-viz` repo are all explicitly deferred, named
here as the player's remaining work rather than built.

A useful identity anchors the design and is worth stating plainly:
**`ehrt play` at rate ∞, ticker sink, is `ehrt show`.** The player is
`show` plus time — nothing about `show`'s own rendering or dispatch
needed to change for this to be true; the pacer is purely additive.

### Decision

**Scope, ruled in chat, 2026-07-30: pacer + ticker sink + paced file
emission, this session.** Bed board, accumulator wiring, a sim
event-log input adapter, and the `ehr-testing-viz` placement question
are deferred, recorded, not built.

**Placement: `components/tools`** — the founding chat's own ruling
("start with the player living in tools"), matching `ehrt.tools.display`'s
own placement (ADR-0013) and this component's existing corpus/judge
machinery.

**Plan/execute split — the `:tty?-fn` seam pattern applied to time.** A
pure planning function (`ehrt.tools.player/plan`) takes a
time-ordered event sequence plus `{:rate :idle-cap-ms}` and returns an
emission plan — a seq of `[wait-ms event]` pairs, plus clamp/
unparseable/skip counts — with **no clock, no sleep, no IO**. A small
executor (`bases/cli`) folds the plan through an injected `:sleep-fn`
and a sink function. Every pacing computation is property-testable
without a wallclock; ambient time exists only in the production
executor's own default `:sleep-fn` (`Thread/sleep`). This is exactly
the injection discipline ADR-0013's `:tty?-fn` already established,
applied to a second ambient concern.

**Pacing semantics.**

- **`--rate R`**: stream-seconds per wallclock-second. Default `60` —
  one stream-hour of corpus time passes per wallclock-minute; `--rate
  1` is real time. Implemented as ordinary division
  (`wait-ms = delta-ms / R`), so `R` at (or near) infinity naturally
  yields all-zero waits with no special-cased sentinel — this is
  *why* the show identity above holds by construction, not by a
  separate code path.
- **Timestamps come from MSH-7**, parsed leniently by field-splitting
  the raw MSH segment text on the character MSH-1 itself declares (no
  HAPI dependency for reading one field) and accepting any
  `YYYY[MM[DD[HH[MM[SS]]]]]` prefix with an optional trailing
  fraction/zone ignored; missing trailing components default to the
  start of their unit (month 1, day 1, hour/min/sec 0).
- **Input order is preserved, never sorted.** Emission order is
  semantically load-bearing (an ADT message before the ORU it
  triggers) and the corpus's own order is the corpus's own statement —
  `plan` walks events in the order given, full stop.
- **Negative inter-event deltas are clamped to zero and counted**
  (`:clamped-count`) — an out-of-order or duplicate timestamp doesn't
  produce a negative wait, it produces an immediate emission, tallied
  so a caller can tell a well-ordered corpus from one that wasn't.
- **A message with a missing or unparseable MSH-7 paces at zero
  delta** (emitted immediately after its predecessor) **and is
  counted** (`:unparseable-count`); its own timestamp is treated as
  identical to its predecessor's for every *later* delta computation
  too, so one bad timestamp doesn't corrupt every subsequent gap in
  the run.
- **Idle-skip is a wallclock cap, not a stream-time threshold:**
  `--idle-cap SECONDS` (default `5`) caps any single computed wait —
  applied *after* dividing by rate, since "wait" means wallclock wait.
  When a wait is actually capped, a skip cue is emitted (see below) and
  tallied (`:skip-count`), distinct from `:clamped-count` (a capped
  wait is never also a clamped one — clamping is specifically the
  negative-delta case).

**The cue rule extends ADR-0013's artifact-vs-display boundary.** A
skip cue (showing that stream-time jumped) is emitted to the ticker's
own stream or to stderr — **never into a data sink.** Paced emission to
a file (or, later, a socket) writes bytes byte-identical to what
unpaced emission through the same sink designator would write; pacing
changes *when* bytes move, never *which* bytes. ADR-0013 drew its
artifact-vs-display line around content (files, `--report`, redirected
bytes are deterministic; a live terminal's rendering is not in scope of
that doctrine). This record extends that line explicitly: **artifact
content stays deterministic under the player too — timing is the
instrument's own concern, entirely outside the doctrine's scope.**
Stated here so a future session doesn't read paced-emission timing
variance as a doctrine violation; it isn't one, because content is
unaffected.

**Sinks.**

- **Ticker (the default):** full mode renders each message as a
  complete block via `render-er7-message` — the exact call shape
  ADR-0013's own display test already pinned — separated by blank
  lines, pretty-always (no TTY consultation, matching `show`'s own
  discipline). A `--ticker line` mode emits one compact line per event
  (MSH-7 timestamp, MSH-9 message type^trigger, first PID-3 patient
  identifier when the message carries a PID segment) via the same
  lenient field-splitting `plan` already does for MSH-7 — reusing the
  segment-splitting idea, not a second HL7 parser.
- **Data sinks reuse the existing source-sink designator vocabulary**
  (`ehrt.tools.corpus.source-sink-url`) rather than inventing a
  parallel flag scheme — `--sink dir:.../file:...` designators land
  exactly like `--out-dir`/`--out` do everywhere else in this CLI.
- **MLLP transport sink: deferred, per this session's own bail-out
  procedure.** `:mllp` already exists as a *framing* (byte-level
  0x0B/0x1C 0x0D envelope, `ehrt.tools.corpus.framing`) but there is no
  `:mllp` *sink kind* in `ehrt.tools.corpus.source-sink`'s own
  `known-sink-kinds` (`#{:dir :file :stdout :blaze}`) — a real network
  socket write. Building one properly touches three namespaces at
  once (a new canonical schema and constructor in `source-sink.clj`, a
  new scheme in `source-sink-url.clj`'s grammar, and a new write
  function in `sink-write.clj`), not a single isolated extension
  point — assessed against this session's own bail-out procedure and
  judged to balloon past "lands small." Deferred whole, not
  half-built: this session ships `--sink dir:`/`file:` only, and names
  the shape a future `:mllp` sink would need (the three-namespace
  surface above, plus connection lifecycle and ACK/retry policy,
  explicitly out of scope even when it does land).

**End-of-run summary.** The player emits a standard Result envelope —
events emitted, stream-time span, wallclock elapsed, rate, clamp
count, unparseable-timestamp count, skip count, sink designator —
through the existing TTY/`--pretty`/`--edn`/`--json` machinery
(ADR-0013): during a run the ticker (or the data sink) owns stdout: the
summary is the machine surface, printed once, at the end, the same way
every other command's result already is. Ctrl-C mid-run producing a
partial, graceful summary is out of scope this session — recorded as
deferred, not silently unhandled.

**One combined capture-and-build session, unattended (R30), matching
the 2026-07-30 output-UX session's own shape.**

### Alternatives rejected

*Sorting events by timestamp before pacing* — the corpus's own order is
part of what it says; an ADT-before-its-own-ORU corpus sorted by a
slightly-later ORU timestamp would silently reorder a causally
meaningful sequence. *A stream-time idle-skip threshold (skip whenever
the corpus itself has a large gap) instead of a wallclock cap* — this
would make skip behavior depend on the corpus's own timestamps
independent of the chosen rate, so the same corpus would skip
differently at `--rate 1` vs `--rate 3600` for no reason a user chose;
capping the actual wallclock wait is what "idle" means from the
sitting-at-a-terminal, watching-it-play perspective this sink serves.
*Treating a capped wait as also "clamped"* — conflating two different
findings (a corpus with out-of-order timestamps vs. a corpus with a
long real gap) into one counter would make the summary's own clamp
count lie about ordering when the real story was pacing, or vice
versa. *Building the MLLP sink today, minimally* — assessed directly
against the bail-out procedure (see Decision, above) and found to
cross three namespace boundaries rather than one; a half-built network
sink with no ACK handling and untested lifecycle is a worse outcome
than a clearly named deferral.

### Consequence

`ehrt.tools.player` is new, pure surface with no IO; `bases/cli` gains
`ehrt play` beside `ehrt show`, `:sleep-fn`/a clock seam in the
injection map, and `--rate`/`--idle-cap`/`--ticker`/`--sink` flags.
Nothing about any existing verb's behavior, exit code, or output
contract changes. A future session building the bed board, wiring the
sim accumulator into the player, or adding the `:mllp` sink inherits a
tested pacer and two working sinks to build around, and this record's
own three-namespace assessment of what an `:mllp` sink actually needs.

**Fulfillment note (2026-07-30, added by the CLI trial-UX session,
ADR-0015).** The directory half of this record's own `play-command`
input-scope deferral ("A single HL7 v2 (ER7) file is this session's
own input scope; a directory... is `:play-input-unsupported`") is
retired: `ehrt play` now accepts a directory of files sharing the
sniffed v2 format, concatenated in lexical filename order before
planning — fix-forward, not a revert of this record's own original
scoping, which was correct for what this session actually built. The
FHIR half of the same deferral, and every other item this record names
as future work (bed board, accumulator wiring, a sim event-log input
adapter, the `:mllp` sink), remain exactly as deferred here. See
ADR-0015 for the fulfillment's own design record.

**Status.** Accepted (author-directed, autonomous session per R30), 2026-07-30.

---

## ADR-0015 — CLI trial-UX: generate sources front door, play directories, gate v2-nist verb, breadcrumbs pretty-only

**Status:** Accepted (author-directed, autonomous session per R30), 2026-07-30.

**Note (2026-07-30, added by this same session, per ADR-0012/ADR-0013/
ADR-0014's own precedent):** this record shares its number with the
frozen `notes/tools/ADRs.md` ADR-0015 ("The gate loop maintains TWO
baselines: legacy-floor and full-capability"). Per this file's own
preamble citation rule: a bare `ADR-0015` anywhere in this workspace's
live documents means *this* record; the tools-era one is always cited
as `notes/tools/ADRs.md` ADR-0015 or `tools/ADR-0015`.

### Context

A design-channel UX audit (chat, 2026-07-30) walked the trial-user
journeys against `docs/cli.md` and found the consume side (`show`,
`gate`, playing a file, `--json`) genuinely simple — one verb, one
path, sniffing does the rest — with every friction point concentrated
on the produce side, none of it inherent to the underlying capability:

1. Sim generation is reachable only through `corpus intake 'sim:'` — a
   cataloging verb (SS-2) fronting for generation, with a URL spelling
   that needs shell quotes the moment params appear.
2. `play` rejects directories (`:play-input-unsupported`, the
   ADR-0014 deferral) while the sim generator emits one `.hl7` file
   per message — the natural sim→play pipeline needs a manual `cat`
   step the audit found nowhere documented as such.
3. The NIST profile-tier judge (`judge-v2-nist`, ADR-0012) — the
   headline capability of that arc — has no CLI verb at all (ADR-0012's
   own "CLI expansion deferred" note, never picked up), so a shell user
   cannot try it regardless of how the rest of the surface reads.
4. Produce commands (`generate`, `mutate`) end without telling the user
   what to do next — no bridge from "a corpus now exists" to "here is
   how to look at it."
5. The deliberate `:out-dir-exists` rerun rejection (D9's determinism
   law: a zero-flag command derives a stable path, so a second
   zero-flag run must not silently clobber the first) is doctrinally
   right but reads as a bare refusal unless its own `:hint` carries the
   literal remedy.

The audit's proposals were reviewed in chat and this session's own
prompt was commissioned on them. The `generate` restructuring carried
the one real design decision this record has to make (subcommands vs.
a `--source` flag); the recommendation — subcommands — stood
unobjected in that review and is ruled below.

### Decision

**[A] `ehrt corpus generate` grows source subcommands.** `corpus
generate synthea` and `corpus generate sim`, each with its own flag
spellings (`--seed`, `--patients`, …) mapped onto
`ehrt.tools.corpus.generators`' existing registry entries and their own
`:default-params` — the registry, not this CLI layer, remains the
single source of what each generator source *does*. Bare `corpus
generate` (no subcommand) stays exactly Synthea, byte-for-byte
unchanged, calling the same `generate!` function it always has —
compatibility with every existing doc, strip, and script that already
types the bare form, `bin/quickstart-demo` included. Both sources sit
under the same D9 zero-flag contract (frozen `tools/ADR-0019`): derived
`out/corpus/…` out-dirs, byte-reproducible, rejected-not-overwritten on
rerun (`:out-dir-exists`). The registry already shares
`generate/default-seed` across both entries, so `corpus generate sim`
with zero flags is a complete, deterministic command on its own.

*Why subcommands, not a `--source sim|synthea` flag* (the rejected
alternative): a subcommand is discoverable through `ehrt help corpus`
the same way every other multi-shape verb in this CLI already is (`gate
v2`/`gate fhir` is the in-house precedent this record follows, not a
new parsing mechanism), where a flag would need to already be known to
type before a reader could find it — the same discoverability argument
ADR-0013 already made for why `out/` had to be the *default*, not an
opt-in flag, applies here one level up.

**Amendment (2026-07-30, added by the cold-start UX session, ADR-0015
self-amendment).** This record's own compatibility sentence above
("Bare `corpus generate` (no subcommand) stays exactly Synthea,
byte-for-byte unchanged... compatibility with every existing doc,
strip, and script that already types the bare form, `bin/quickstart-demo`
included") is reversed here, fix-forward, not a revert of what this
record correctly decided for its own session: **bare `corpus generate`
now means `generate sim`**, not `generate synthea`. Ruled by the author
one session later, from a genuine cold-environment run of bare `bin/ehrt
corpus generate` (author's machine, Git Bash/Windows side, 2026-07-30):
a `run!` shadowing warning from `ehrt.tools.sim` was the first line of
output; the run then rejected `:not-cached` (the Temurin JDK archive
wasn't in the local artifact cache — Synthea's lane needs fetched
artifacts before it can run at all) with no remedy text; and the
rejected run left behind an empty `out/corpus/` directory. Rationale:
sim is this project's own engine, mounted in-process per ADR-0005 — zero
external artifacts, zero subprocess, zero network — so with sim as the
default, the first command a cold user types succeeds with nothing
fetched, where Synthea's default forced a fetch step before any success
was possible at all. `generate synthea` remains the explicit spelling
for the Synthea lane, unchanged in behavior; only the bare form's
routing flips — `generate sim` remains valid and identical to bare.
Consequence: `bin/quickstart-demo` (this record's own named beneficiary
of the old compatibility guarantee) now pins `generate synthea`
explicitly, since it deliberately exercises the Synthea→FHIR gate lane
the rest of the script depends on — the flip changes what it types, not
what it tests. Full ruling record, including the cold-run transcript in
full and every downstream doc/test site touched:
`notes/prompts/2026-07-30-ehr-testing-cold-start-ux.md`.

**[A] `corpus intake`'s generator-URL form (SS-2) and stdin form (SS-3)
are retained, unchanged in behavior.** They were designed as
composition features — generate-then-catalog, pipe-then-catalog — and
remain exactly that; no deprecation, no warning output, no behavior
change. What changes is positioning only: docs stop presenting `intake
'sim:'` as *the* way to run the simulator and present it as the
one-command compose it always was; `generate sim` is the front door for
"I just want a sim corpus."

**[A] `ehrt play` accepts a directory.** Files sharing the sniffed v2
format, concatenated in lexical filename order — which preserves the
sim generator's own `msg-%03d` emission order by construction — then
planned and paced as one stream, exactly as if the directory's content
had been `cat`-ed into one file first. The ordering rule is stated in
the verb's own help text: lexical order is the contract, deterministic
and disclosed, not an implementation detail a caller has to discover by
reading source — matching ADR-0014's own "the corpus's own order is
the corpus's own statement" doctrine, extended here to "the directory
listing's own order is the corpus's own statement" for exactly the same
reason (a caller who wants a different order names it in the
filenames). FHIR paths remain the named, disclosed unsupported input;
a bare directory of mixed or unclassifiable files is the same
`:play-input-unsupported` shape as before, not a new error family. This
half-retires ADR-0014's own `:play-input-unsupported` deferral — a
dated fulfillment note goes into that record, fix-forward, not a
revert.

**[A] `ehrt gate v2-nist PATH --profile BUNDLE_DIR` lands.** Picks up
ADR-0012's own skipped CLI-expansion step: builds the validator once
per invocation from the Π bundle (context construction dominates cost,
ADR-0012 — never a per-file rebuild), gates PATH (file or directory)
through the existing `ehrt.tools.interface` `v2-nist-*` re-exports, and
reports through the standard per-file verdict summary and envelope
machinery every sibling gate already uses (`gate-command`'s own
generic shape, unchanged). `--profile` is required — no default bundle
is silently assumed, since there is no project-owned profile yet
(ADR-0012's own "stand-in, not this project's own profile" disclosure)
— an absent `--profile` is a clear, named rejection, not a crash or a
silent no-op. The committed CDC fixture
(`components/tools/test-fixtures/v2-nist/COVID19_ELR-v2.3.1`) is the
documented try-it value, named in help text and the `docs/use-cases.md`
strip, never an implicit fallback a caller could stumble into
unknowingly. Bare `ehrt gate PATH` sniffing does NOT dispatch to
v2-nist — it structurally cannot, since sniffing has no bundle to build
a validator from — so `sniff-gate-command`'s own D11 dispatch table is
untouched by this record. A malformed `--profile` directory (missing
`PROFILE.xml`, or anything else the engine's own `make-validator`
throws on — `ehrt.judge-v2-nist.v2/make-validator` is one of this
workspace's few deliberate throw sites, a caller-contract violation
per ADR-0012's own Result-not-throw carve-out) is caught at this CLI
seam and surfaced as a named operational error, not an uncaught
stack trace.

**[A] Breadcrumbs.** The PRETTY summaries (`render-pretty`, ADR-0013 —
never the EDN/JSON envelope, whose shape is the machine contract and is
unaffected by this record) of produce commands end with one
copy-pasteable next command: `generate synthea`/`generate sim` →
`try: bin/ehrt show <out-dir>`; `corpus mutate` → `try: bin/ehrt gate
<mutants-dir>`. Two breadcrumbs, ruled here; more is permitted-skip
territory, named if added or explicitly declined during implementation.

**[A] The `:out-dir-exists` rejection's `:hint` carries the literal
remedy.** The exact `rm -rf <derived-dir>` for a fresh identical rerun,
and the `--out-dir` alternative for keeping the old run around; the
pretty rendering of this rejection presents it as the determinism
story ("same inputs, same directory, never silently overwritten"), not
as a bare refusal a reader has no way to act on. The envelope shape is
unchanged — `:hint` already existed as a key; only its text, and the
pretty rendering built around it, improve.

**[C] Subcommand grammar follows the cli-spec's existing positional
pattern for group verbs** (`gate v2`/`gate fhir` is the in-house
precedent) — no new parsing mechanism invented for `generate`'s own
subcommands.

**[C] One combined capture-and-build session, unattended (R30),**
matching this workspace's own 2026-07-30 session shape (ADR-0012,
ADR-0013, ADR-0014).

### Alternatives rejected

*A `--source sim|synthea` flag on `corpus generate` instead of
subcommands* — considered and rejected for the discoverability reason
stated above; recorded as the fallback this session's own decision
procedures name if the cli-spec's grammar genuinely cannot express
subcommands without new parsing machinery (it turned out it could; see
the deviation record if this alternative was ever actually taken).
*Sorting `play`'s directory input by MSH-7 timestamp instead of
filename* — rejected for the same reason ADR-0014 already rejected
sorting a single file's own message order: the corpus's own order (here,
the directory listing's own order) is part of what it says, and a
generator that names its files `msg-000.hl7`, `msg-001.hl7`, … is
already stating an intended order the way a single multi-message file's
internal sequence does.

### Consequence

`corpus generate` gains two named subcommands with no change to its
zero-flag/bare-command behavior; `intake`'s generator-URL and stdin
forms are unaffected in code, only in how docs introduce them; `play`
gains directory input via the existing `er7-multi` splitter, composed
rather than reimplemented; a fourth gate verb (`gate v2-nist`) joins
`gate v2`/`gate fhir` with no change to either sibling or to bare
`gate`'s own sniff table; every produce command's pretty rendering
gains one hint line with no change to its EDN/JSON contract, verified
by test. `notes/tools/ADRs.md` ADR-0015 (the two-baseline gate loop) is
untouched, frozen provenance, cited origin-qualified wherever this
record's own trial-UX work happens to reference gate-loop baselines at
all (it does not, directly).

**Status.** Accepted (author-directed, autonomous session per R30), 2026-07-30.
