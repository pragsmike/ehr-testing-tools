# Architecture Decision Records — ehr-testing (workspace)

Numbered, append-only, starting fresh at ADR-0001 for this workspace
— not a continuation of `ehr-testing-sim`'s or (later) `ehr-testing-tools`'
own numbering. Never silently revert an Accepted ADR; supersede it
with a new numbered record.

Legacy ADRs move into this workspace intact as provenance
(`notes/sim/ADRs.md`, frozen, not rewritten for new paths/namespaces)
and are cited here origin-qualified, e.g. `sim/ADR-0008`.

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
- **H6. Workspace CI, mostly landed, one trigger remaining.** The
  `poly check` + `poly test :all` workflow landed this session
  (`53d76b0`, `.github/workflows/test.yml`). What's still open: the
  workflow hasn't run for real yet (nothing has been pushed to
  `origin` this session without the author's own explicit go-ahead —
  see R6 above and `AUTHORS-GUIDE.md` §1), so switching the test step from
  `:all` to incremental `poly test` waits on the author pushing,
  confirming green, and tagging `stable-<sha>` — the workflow's own
  comment names this explicitly so it isn't forgotten.
