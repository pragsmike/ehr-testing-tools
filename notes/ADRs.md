# Architecture Decision Records — ehr-testing (workspace)

Numbered, append-only, starting fresh at ADR-0001 for this workspace
— not a continuation of `ehr-testing-sim`'s or (later) `ehr-testing-tools`'
own numbering. Never silently revert an Accepted ADR; supersede it
with a new numbered record.

Legacy ADRs move into this workspace intact as provenance
(`notes/sim/ADRs.md`, `notes/tools/ADRs.md`, frozen, not rewritten for
new paths/namespaces) and are cited here origin-qualified, e.g.
`sim/ADR-0008`, `tools/ADR-0017`.

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
  (`tools/ADR-0013`) and needed no repointing at all — the "sanctioned
  adaptation" R15 anticipated turned out to be a non-event; ADR-0013's
  own subprocess-only rule is *why* `poly/sim` does **not** appear in
  `projects/conformance/deps.edn` (a `poly check` warning 207 initially
  flagged this correctly; see deviation record).
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
`projects/conformance/deps.edn` per R15/ADR-0013). `clojure -M:poly
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

## ADR-0005 — The `ehr sim` mount: ADR-0012 fulfilled, ADR-0013 decision 1 retired

**Status:** Accepted (author-directed, amending the session's own R20
in-session), 2026-07-28.

### Context

ADR-0004's own R19 restructuring left five `sim_*_test.clj` suites (plus
`smoke_test.clj`'s sim half) in `projects/conformance`, still resolving
sim through a sibling-checkout discovery order (`ehrt.tools.sim`'s
`:sim-dir` → `EHR_TESTING_SIM_DIR` → `../ehr-testing-sim`, ADR-0013)
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
lockstep."* And ADR-0013 decision 5, verbatim: *"The `ehr sim` mount
remains DEFERRED (ADR-0012, unchanged)."* `notes/tools/ADRs.md`
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

**ADR-0013's direction invariant preserved, poly-enforced.** The rule
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
(`stub-key`, `known-dispatch-pairs`) updated to match, per ADR-0012
property 4's own correction about what "mounting sim" does and doesn't
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

### ADR-0012's five properties, exercised

Each of the five interface commitments ADR-0012 recorded (before any
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
`bases/ehr-cli`'s own CLI tests hermetic, exactly the property ADR-0012
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
disagreeing with reality is a finding (leave it red, ADR-0013's own
precedent); a check misencoding its own invariant is an escalation
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
