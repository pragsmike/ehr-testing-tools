# 2026-07-28 — ehr-testing workspace bootstrap and sim landing

Archived per this session's own step 12: the originating session prompt,
verbatim, with the deviation-record appendix appended below it. Per
`docs/way-of-working.md` §3, "prompts are provenance" — this is the
workspace's first entry in that convention (see that document's own honest
gap note about sim's `.agents/prompts/archive/` never having been
populated with real prompts; this workspace starts the habit for real from
its own first session).

---

## Original prompt

### Context

`pragsmike/ehr-testing` is the new public monorepo that will consolidate
`ehr-testing-sim` and `ehr-testing-tools` into a single Polylith workspace.
It exists on GitHub with one commit (MIT LICENSE, Leiningen-flavored
.gitignore) and is cloned under WSL alongside `ehr-testing-tools`.

This session bootstraps the workspace and lands sim only. Tools is frozen
out of this session: a source-sink formalization session is running
against it, and nothing that originates in tools crosses into the
workspace until that session lands and tools is tagged stable. That
exclusion covers code you might be tempted to pre-extract (judge,
palgebra, corpus) and empty scaffolding for future components — do not
create stub components. Installed-but-unused machinery is a known
confabulation hazard in this project family.

Environment: WSL2/Ubuntu, recently upgraded git, JDK 21 (Temurin). Sim's
own CI already pins Temurin 21 and its SETUP.md claims JDK 8+/21, so no
sim-side JDK doc fix is expected — but step 11 greps to confirm across
everything landed.

### Read first

1. This prompt, fully, before any action.
2. `../ehr-testing-sim/AGENTS.md` and `../ehr-testing-sim/AUTHORS-GUIDE.md`
   — the discipline being promoted to workspace canon.
3. `../ehr-testing-sim/docs/way-of-working.md`, `CONTRIBUTING.md`,
   `SETUP.md`.
4. `../ehr-testing-sim/notes/ADRs.md` and `notes/facts-register.md` —
   these move as provenance in step 8.
5. The Polylith consolidation brief (author will place it at
   `doc/migration/polylith-brief.md` in this repo; if absent, ask — do not
   proceed on memory of it).
6. `poly doc page:workspace` and `page:component` once poly is installed,
   or the cljdoc pages for workspace/component/base/project.

### Author rulings

R1. Workspace repo is `ehr-testing` (rename possible later; do not
optimize naming elsewhere for it). Top namespace is `ehrt`.
R2. `ehr-testing-guide` stays out of the workspace entirely.
R3. `ehr-testing-tools` will be the only published artifact. Sim is a
project that builds an app artifact or nothing; it is never published as
a library. (Resolves the one-library-per-workspace constraint.)
R4. Sim's discipline is canonical. Where sim's and tools' conventions
differ, sim's form wins; tools conforms on arrival (future session).
R5. Landing shape is thin base + fat component: `bases/sim-cli` holds
only CLI dispatch (`ehr-testing-sim.cli` and nothing else); `components/sim`
holds everything else behind one deliberately wide `ehrt.sim.interface`,
to be narrowed by later extraction sessions.
R6. All git commits and pushes are the author's, made manually in WSL.
This session prepares working trees and provides commit messages; it
never runs `git commit`, `git push`, `git merge`, or `gh`. Agents in this
environment hold ambient authenticated `gh` credentials (the packs-repo
precedent); the standing rule is that they do not use them. This rule
gets written into the root AGENTS.md in step 5.
R7. Hooks are installed and verified before the first workspace-era
commit is pushed. A hook installed at commit five protects commit six.
R8. Legacy ADRs and facts registers move intact as provenance; the
workspace starts a fresh ADR sequence whose ADR-0001 is the migration
plan, with unresolved items recorded as named holes, not guesses.
R9. Poly version 0.3.32, via the `:poly` alias (no standalone install
required). JDK 21 Temurin.
R10. Fix-forward: if any step's premise turns out false against the live
tree, stop, record the finding in the deviation appendix, and ask — do
not silently adapt.

### Steps

Checkpoint convention: each COMMIT line is a point where the author
commits with the given message before the session continues. AUTHOR
ACTION lines are performed by the author, not the agent.

**0. Environment probe.** In the `ehr-testing` clone: record `git
--version`, `java -version` (expect Temurin 21), and — after step 3 wires
the alias — `clojure -M:poly version` (expect 0.3.32). Record outputs
verbatim in the deviation appendix header as the session's environment
stanza. If poly does not run on this JDK, stop (R10).

**1. Windows/WSL friction and repo hygiene.** Create `.gitattributes`
(`* text=auto eol=lf`, `*.png binary`, `*.jar binary`). Create
`.editorconfig` (utf-8, lf, final newline, 2-space indent for `*.clj`
`*.edn`). Replace the Leiningen-flavored `.gitignore` with a
deps.edn/Polylith one: keep `.cpcache/`, `.nrepl-port`, `target/`; add
`.calva/output-window/`, `.lsp/.cache/`, `.clj-kondo/.cache/`,
`.portal/`, `development/src/dev/scratch*`.
COMMIT `chore: gitattributes, editorconfig, deps-era gitignore`

**2. Pre-push hook.** Port sim's hook (WSL-commit enforcement,
gitleaks), extend the test gate to `clojure -M:poly check` and `clojure
-M:poly test :project`. Note `:project` deliberately: project-level
tests (the future conformance project) do not run under bare `poly
test`; the gate must include them from day one so their omission can
never be silent. Verify the hook fires on a dry-run push. Document
installation in `SETUP.md` (created in step 5).
COMMIT `chore: pre-push hook — WSL enforcement, gitleaks, poly gates`

**3. Workspace skeleton.** From the repo root (existing-git-repo mode —
no `name:`, no `:commit`): `clojure -Sdeps '{:deps {polylith/clj-poly
{:mvn/version "0.3.32"}}}' -M -m polylith.clj.core.poly-cli.core create
workspace top-ns:ehrt dialects:clj`. Then edit the generated root
`deps.edn` to add the `:poly` alias per the brief §4, and `workspace.edn`
to set `:top-namespace "ehrt"`, `:interface-ns "interface"`, `:dialects
["clj"]`, `:vcs {:name "git" :auto-add false}` (author commits manually,
R6), `:tag-patterns {:stable "^stable-.*" :release "^v[0-9].*"}`,
`:validations {:inconsistent-lib-versions {:type :error :exclude []}}`,
`:projects {"development" {:alias "dev"}}`. `clojure -M:poly check` must
pass on the empty workspace.
COMMIT `feat: polylith workspace skeleton, top-ns ehrt`

**4. Migration brief lands.** AUTHOR ACTION: place the consolidation
brief at `doc/migration/polylith-brief.md`. Agent verifies it is
readable and matches the Read-first understanding.
COMMIT `docs: polylith consolidation brief (migration reference)`

**5. Workspace discipline, seeded from sim.** Author these at the
workspace root, adapted (not copied blind) from sim's versions:
`AGENTS.md`, `AUTHORS-GUIDE.md`, `CONTRIBUTING.md`, `SETUP.md`,
`docs/way-of-working.md`. Adaptations required: workspace vocabulary
(brick/component/base/project; the `poly ws get:` interface as the
agent's primary source of workspace truth); R6 verbatim (the
git-operations boundary and the gh-credential disclosure — converts the
packs incident from folklore to a documented boundary); the fat-component
disclosure (`ehrt.sim.interface` is deliberately wide during migration;
agents must not treat its width as design intent, extraction happens
only by author-ruled session); SETUP.md environment table (JDK 21
Temurin, verified this session step 0, cite step-0 outputs not memory).
Create the workspace skills directory (location per sim's convention) and
move/copy sim's skills into it, diffing against tools' counterparts where
both exist is DEFERRED to the tools landing — this session takes sim's
versions as-is.
COMMIT `docs: workspace discipline seeded from ehr-testing-sim (canonical per R4)`

**6. Sim history merge.** AUTHOR ACTION (git surgery is the author's): in
a throwaway clone of ehr-testing-sim, `mkdir -p .staging && git mv $(ls
-A | grep -v '^\.git$\|^\.staging$') .staging/`, commit "chore: stage for
monorepo merge"; then in ehr-testing, `git remote add sim <path-or-url>`,
`git fetch sim`, `git merge --allow-unrelated-histories sim/main`.
Agent's role: verify post-merge that `.staging/` contains sim's full tree
and that `git log --oneline | wc -l` reflects both histories.

**7. Carve sim into brick shape.** All moves via `git mv` (author
commits; agent stages the tree and hands over). Target layout:
`components/sim/{deps.edn, src/ehrt/sim/ (everything from
src/ehr_testing_sim EXCEPT cli.clj, plus interface.clj — NEW, wide
delegation), test/ehrt/sim/, resources/sim/, docs/ (intact)}`;
`bases/sim-cli/{deps.edn, src/ehrt/sim_cli/core.clj (was cli.clj),
test/ehrt/sim_cli/}`; `projects/sim/{deps.edn (components/sim +
bases/sim-cli + third-party)}`. Namespace rename, mechanical and total:
`ehr-testing-sim.X` → `ehrt.sim.X` (files `ehr_testing_sim/x.clj` →
`ehrt/sim/x.clj`); `ehr-testing-sim.cli` → `ehrt.sim-cli.core`. Every `ns`
form, every `require`, every string reference (deps.edn `:main-opts`,
Makefile, docs code blocks). Grep for the old root afterward; zero hits
outside provenance files (`notes/`, ADRs) which keep their historical
text. `interface.clj`: delegation-only, re-exporting the fns that sim's
CLI, Makefile targets, and tests actually call from outside their own
namespace — determined by grep, not judgment. Mark it with a header
comment: "Deliberately wide (migration ruling R5). Narrowing is an
extraction decision, not a cleanup." Root `deps.edn`: add `poly/sim` and
`poly/sim-cli` under `:dev`, both test paths under `:test`.
`workspace.edn`: add `"sim" {:alias "sim"}`.
COMMIT `feat: land sim as components/sim + bases/sim-cli (thin base, fat component)`

**8. Provenance moves.** `notes/sim/ADRs.md` (was ehr-testing-sim
notes/ADRs.md, byte-identical), `notes/sim/facts-register.md`
(likewise). Workspace-level `notes/ADRs.md` is created in step 10. Sim's
historical ADR text is NOT updated for new paths/namespaces — provenance
is frozen; a one-line header notes the origin repo and merge date.
COMMIT `docs: sim ADRs and facts register preserved as provenance`

**9. Verification gate.** In order, all must pass: (1) `clojure -M:poly
check` — zero errors. (2) `clojure -M:poly test :all` — sim's full suite
green, including the determinism, invariant-coherence,
message-derivability, and emitter-coherence property suites. These are
the rename verifier: if the laws hold under `ehrt.sim.*`, the mechanical
rename was faithful. (3) CLI smoke: `clojure -M:poly` shell or a
project-classpath run of `ehrt.sim-cli.core` reproducing sim's documented
quickstart invocation (`run --seed 42 ...` per sim's `:cli` alias docs);
output compared against the same invocation in the pre-merge sim clone.
Determinism makes this a byte-comparison, not an eyeball. (4) Coverage
alias still runs (`:coverage` ported per sim's ADR-0004 posture: measure
and report, no threshold). Any failure: stop, record, ask (R10). Do not
patch tests to pass.

**10. ADR-0001 — the migration plan.** `notes/ADRs.md`, fresh sequence.
ADR-0001 records as DECIDED: R1–R9 above, the landing shape, the
verification-by-property-law method, and cross-references to legacy ADRs
by origin-qualified ID (e.g. "sim/ADR-0008"). It records as NAMED HOLES,
each with its trigger condition: H1 (source/sink component shape — one
component with internal polymorphism vs. N components sharing an
interface — decided from the landed source-sink design after the running
tools session completes); H2 (tools landing plan — thin `ehr-cli` base +
fat `tools` component; first extractions judge → palgebra → corpus — a
separate prompt after tools is frozen and tagged); H3 (conformance
project — base-less; composes sim + judge + corpus — after H2's judge
extraction); H4 (`hl7v2` wrapper component, including the cmiles74
escaping workaround with a characterization test that fails if upstream
fixes escaping — after H2); H5 (published-artifact coordinates — Clojars
verified-group vs. Maven Central; group likely `io.github.pragsmike` or
an owned domain — author's call, unblocked by nothing in this repo); H6
(workspace CI: port sim's `test.yml` to poly-based incremental testing
(`poly test` + `changed-or-affected-projects since:release`) — may land
this session as step 11 if time allows, else queued).
COMMIT `docs: ADR-0001 migration plan — settled rulings and named holes`

**11. Sweep and CI (if session budget allows).** Grep the workspace for
`jdk.?17|java 17|temurin.?17` and for the old namespace root; fix
stragglers. (Pre-session probe found no 17-claims in sim; expect zero,
verify anyway.) Port `.github/workflows/test.yml`: Temurin 21, `clojure
-M:poly check`, `clojure -M:poly test :all` (switch to incremental once a
`stable-*` tag exists), tag `stable-<sha>` guidance in the workflow
comments.
COMMIT `ci: poly-gated workflow, temurin 21`
AUTHOR ACTION: after green CI, tag `stable-bootstrap` so incremental
testing has a baseline.

**12. Self-archive.** Copy this prompt to
`notes/prompts/2026-07-28-ehr-testing-bootstrap-sim-landing.md` with the
deviation-record appendix appended.
COMMIT `docs: archive bootstrap session prompt with deviation record`

### Deviation record

(Environment stanza from step 0 here, then dated entries for any
mid-session rulings or premise failures. Empty is a valid state; absent
is not.)

---

## Appendix: deviation record (as executed)

This session's own findings, in the order they surfaced. The full,
narrative version of each lives in `notes/ADRs.md` ADR-0001's own
"Deviation record" section — reproduced here so this archived prompt is
self-contained, per the prompts-are-provenance convention.

### Environment stanza (step 0, 2026-07-28, WSL2/Ubuntu)

```
git --version    -> git version 2.50.1
java -version    -> openjdk version "21.0.7"
                     OpenJDK Runtime Environment (build 21.0.7+6-Ubuntu-0ubuntu120.04)
                     OpenJDK 64-Bit Server VM (build 21.0.7+6-Ubuntu-0ubuntu120.04, mixed mode, sharing)
clojure -M:poly version -> poly 0.3.32 (2025-12-29)
```

### JDK/Temurin premise (step 0)

The prompt characterized the environment as "JDK 21 (Temurin)." The
actual `java -version` output is Ubuntu's own OpenJDK 21 build, not
Eclipse Temurin — a `temurin-17-jdk` package was present but unused
(version 17, not 21), and no Temurin 21 build exists on this machine.
Sim's own `SETUP.md` tells WSL2/Ubuntu users to install the stock
`openjdk-21-jdk` apt package for local dev and reserves "Temurin" for its
CI's `distribution: temurin` pin — this machine's state matches that
convention exactly. Flagged before proceeding; author's ruling (asked,
not guessed): record precisely as measured; "Temurin" is CI-only, not a
local-dev claim.

### Gitleaks premise (step 2)

The prompt asked to "port sim's hook (WSL-commit enforcement,
gitleaks)." Sim's actually-committed `pre-push` hook does not run
gitleaks at all — the one gitleaks scan on record (`sim/facts-register.md`
F15) was a one-time go-public secrets audit, not a hook behavior.
Flagged before implementing; resolution: added gitleaks as a *new*
pre-push gate (extending sim's pattern, not porting a behavior that never
existed). gitleaks v8.30.1 installed to `~/.local/bin`, checksum-verified
against the published release checksums (same method as F15), with the
author's explicit go-ahead for the download.

### Residual `.staging/` files, step 6→7 seam

Step 6's git-history merge staged sim's *entire* tree under `.staging/`;
step 7's own target layout named only `src/test/resources/docs` moving
out of it, leaving no stated disposition for `.staging/`'s own
`AGENTS.md`, `AUTHORS-GUIDE.md`, `CLAUDE.md`, `CONTRIBUTING.md`,
`SETUP.md`, `README.md`, `LICENSE`, `Makefile`, `NOTICE`,
`.gitattributes`, `.gitignore`, `.githooks/`, `deps.edn`, `.github/`, and
`.agents/`. Flagged before executing step 7; author-approved resolution:
`NOTICE` (load-bearing LOINC text) relocated with its content into
`components/sim/NOTICE` and the two directory-scoped
`resources/{demographics,modules}/NOTICE` files, with internal path
references updated to match (unlike ADRs/facts-register, these are
operative, not frozen). `.github/ISSUE_TEMPLATE/*` moved to the
workspace root. `.agents/{memory,plans,prompts,session-records}` and
`notes/{ADRs,facts-register}` moved to `notes/sim/` as provenance (R8).
Everything else deleted as superseded — still reachable in git history
via the merge commit. `.github/workflows/test.yml` deliberately left for
step 11 to port.

### Step 7 commit-scoping mistake, self-caught

The first `git commit -- <pathspec>` for step 7 listed the *destination*
paths but omitted the matching `.staging/{src,test,resources,docs,NOTICE}`
*source* paths, so the rename's deletion half never landed — `HEAD`
briefly carried both the old and new copies of every moved file. Caught
by inspecting `git ls-tree -r HEAD` directly rather than trusting `git
status`; fixed with an immediate follow-up commit removing the stale
`.staging/` paths. The same follow-up also ran `git add --renormalize .`
for CRLF/LF line-ending drift the first commit's own warnings had
flagged.

### Two latent test bugs, pre-existing, unmasked by the rename

`identifiers_test.clj` and `vendored_module_test.clj` both called
`clojure.set/intersection` (or `/subset?`) fully-qualified without
requiring `clojure.set` — this worked in the standalone sim repo only
because some other required namespace happened to load `clojure.set`
transitively first; under Polylith's different namespace-load order,
`poly test` surfaced a `ClassNotFoundException`. Fixed by adding the
missing `(:require [clojure.set ...])` — not a rename bug, a correctness
gap the migration's own classpath change exposed.

### HL7 wire bytes and the workspace's own `eol=lf` rule

Step 9's own coverage-alias check led to inspecting
`components/sim/docs/demos/` more closely, which surfaced a real latent
risk: `messages*.txt` under that tree carry literal ER7 wire bytes (`\r`
segment separators), and the workspace's own `.gitattributes` (`* text=auto
eol=lf`, step 1) had no exemption for them — exactly the hazard
`ehr-testing-tools`' own `.gitattributes` already documents for its v2
fixtures. Not yet corrupted (verified byte-identical against the
pre-merge sim clone), but exposed for any future checkout where
normalization would actually fire. Fixed with a `-text` override before
it could bite.

### Session-level delegation note

Per R6, this session defaulted to agent-prepares/author-commits. After
step 1's own commit, the author explicitly told the session, in chat, to
execute commits itself for the remainder of the session — a live,
scoped grant (`AUTHORS-GUIDE.md` §1), not a rewrite of R6 for future
sessions. Git surgery proper (step 6's merge) was asked about
separately and explicitly delegated too, rather than assumed to be
covered by the earlier, narrower grant. Nothing was pushed to `origin`
this session; that, and tagging `stable-bootstrap` once CI is green
(step 11's own AUTHOR ACTION), remain the author's next steps.

### Step ordering note

Step 10 (ADR-0001) was drafted before step 11 (sweep and CI) per the
prompt's own numbering, but committed after it, so ADR-0001's own H6
entry could record what step 11 actually shipped rather than a
placeholder ("may land this session... else queued"). A disclosed,
deliberate reordering of commit sequence relative to step numbering, not
a silent one — nothing in step 11 depended on ADR-0001 existing first.
