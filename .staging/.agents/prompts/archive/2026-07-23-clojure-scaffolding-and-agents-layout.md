Task: Clojure scaffolding + agent-facing layout for ehr-testing-tools
You are working in the `ehr-testing-tools` repository (bootstrapped last session: git, Makefile with `pack`, ADRs, facts register, positioning docs). This session adds two things:

1. Clojure project scaffolding — `deps.edn`, `src/`, `test/`, a working `make test`.
2. Agent-facing adaptation — `AGENTS.md` plus the standard `.agents/` layout, with the applicable skills copied from the sibling `../ehr-testing-guide` repo (especially `wsl-windows-git-hygiene` and `handoff`), the WSL-enforcing git hook, and a `.agents/prompts/` archive for executed Code prompts.

The guide repo is already adapted (root `AGENTS.md`, `CLAUDE.md` shim, `.agents/skills/`) and carries the repo-adaptation skill bundle at `../ehr-testing-guide/.agents/skills/repo-adaptation/`. Use that bundle's procedure and scripts rather than improvising: this repo is greenfield for agent-facing files, so you are in implementation mode, bootstrap path — this prompt is the approval. Read the bundle's `SKILL.md`, `references/target-structure.md`, and templates before creating anything.
Environment

* Commits from WSL only (hard rule; the hook you'll install enforces it).
* `../ehr-testing-guide` is readable; modify nothing in it.
* Read this repo's `AUTHORS-GUIDE.md`, `notes/ADRs.md`, and `docs/positioning.md` first — several constraints below come from them.

Step 0 — Reconcile the dirty Makefile
`git status` will likely show an uncommitted Makefile modification: `PACK_OUTPUT` redirected from the repo root to `$HOME` (the author's upload workflow). Keep the behavior as found in the working tree, fix the recipe's comment (it still says "at the repo root"), leave the `.gitignore` entry in place as belt-and-braces, and commit this alone: `Makefile: pack output to $HOME; fix stale comment`. If the tree is already clean, skip this step. Do not proceed with a dirty tree.
Step 1 — Clojure scaffolding
Read `../ehr-testing-guide/companion/deps.edn` (and its test setup) first; mirror its test tooling and alias names so `clojure -X:test` means the same thing in both repos.

1. `deps.edn`:
   * Exact-pinned versions only (`:mvn/version` with full versions — no ranges, no `RELEASE`). Dogfooding rule from `docs/positioning.md`: a reproducibility toolkit with unpinned deps is self-refuting.
   * Deps: `org.clojure/clojure` (latest stable 1.12.x you can verify) and the test runner the companion uses (same coordinates, same pinned version if current; if the companion's pin is outdated, pin the current stable and note the divergence in your report).
   * Aliases: `:test` (working via `clojure -X:test`); a `:dev`/`:repl` alias only if the companion has one to mirror — add nothing speculative.
2. Source tree: root namespace prefix `ehr-testing-tools`. Create `src/ehr_testing_tools/core.clj` with a small real function (not a TODO stub — e.g. something that returns the repo's name and version map read from `deps.edn` or a constant), and `test/ehr_testing_tools/core_test.clj` with a non-vacuous test of it. This is placeholder shape, deliberately minimal: the internal corpus/gate organization is an open decision (`docs/positioning.md`, Open decisions) — do not create `corpus/`, `gate/`, or any capability namespaces.
3. Makefile: add `test` target (`clojure -X:test`) and list it in `help`. Verify `make test` passes.

Commit: `Add Clojure scaffolding: deps.edn, src/test trees, make test`.
Step 2 — Agent-facing layout (repo-adaptation, bootstrap path)
Run the bundle's `scripts/inspect-repo.sh` against this repo to confirm greenfield classification, then create:

1. `AGENTS.md` at root. Use the bundle's template as the skeleton, the guide's own `AGENTS.md` as the house-style reference, and keep it concise and operational. Must cover:
   * One-paragraph project overview (operational tools for the EHR testing method; sibling of ehr-testing-guide; pre-release).
   * Commands: `make help` / `make pack` / `make test`; note pack output goes to `$HOME`.
   * Hard rules: WSL-only commits (hook enforced); exact-pinned deps; facts asserted in docs get an F-row in `notes/facts-register.md` with evidence and date; ADRs in `notes/ADRs.md`, supersede never revert; regenerate the pack after the final commit of a session.
   * Repo conventions: internal src structure pending (link the positioning doc's Open decisions); scope fence one-liner with pointer to README.
   * Pointers to `.agents/skills/` and the `.agents/prompts/` archive convention (below).
2. `CLAUDE.md` compatibility shim pointing to `AGENTS.md` as canonical, matching how the guide does it.
3. `.agents/` directories: `skills/`, `handoffs/`, `prompts/`. Create others (`plans/`, `memory/`, `templates/`, `logs/`) only if a copied skill requires them; empty-dir signaling isn't needed beyond these.

Step 3 — Copy skills from the guide
Copy rule: a skill comes over if its content is repo-agnostic; it stays behind if it's about authoring the manuscript. Apply the rule to every skill in `../ehr-testing-guide/.agents/skills/` and report the per-skill decision. Expected outcome (verify against content, don't trust this list blindly):

* Copy: `wsl-windows-git-hygiene` (with its `scripts/` and `references/`), `handoff`, `find-skills`, `shared-skill-layout`, `repo-adaptation` (the bundle itself — this repo will maintain its own layout with it).
* Leave: `committee`, `editorial-review` — manuscript-review machinery — unless reading them shows they're genuinely repo-agnostic; if borderline, leave and note it.

When copying, adapt: fix any guide-specific paths, repo names, or manuscript references inside copied skills (report each adaptation); copy verbatim where nothing is guide-specific. Preserve each skill's directory structure (`SKILL.md`, `scripts/`, `references/`, `agents/*.yaml` cross-tool shims).
Also copy the WSL enforcement: `../ehr-testing-guide/.githooks/` (pre-commit hook) into this repo, adapted if it references guide paths; run `git config core.hooksPath .githooks` in this clone; document that per-clone config step in `AGENTS.md` (config doesn't travel with the repo). Verify the hook actually fires by attempting a commit and observing it run.
Commit (steps 2+3 together or split sensibly): `Add AGENTS.md, .agents layout, skills from guide, WSL hook`.
Step 4 — ADR and conventions updates

1. ADR-0003 — Agent-facing layout and prompt archive. House format. Substance: Context — the guide repo standardized on AGENTS.md + `.agents/` (skills, handoffs) and both repos are driven by chat-designed, Code-executed prompt sessions whose prompts are the real provenance of changes but currently live outside the repo. Decision — adopt the same standard layout; executed Code prompts are archived in `.agents/prompts/` named `YYYY-MM-DD-<slug>.md`; `CLAUDE.md` kept as a pointer shim. Alternatives rejected — `notes/prompts/` (notes is for the repo's own knowledge — ADRs, registers; prompts are agent-facing operational artifacts and belong under `.agents/`); not archiving prompts (loses the provenance chain that caught real errors in the guide). Consequence — every Code session's prompt lands in `.agents/prompts/` as part of that session's commits; handoffs go to `.agents/handoffs/` via the handoff skill. Status: Accepted (author-directed).
2. Archive this prompt: save the file you are reading as `.agents/prompts/2026-07-23-clojure-scaffolding-and-agents-layout.md`, verbatim. (Earlier sessions' prompts may be backfilled by the author later; don't reconstruct them.)
3. AUTHORS-GUIDE.md amendments (pack ritual section): add — the pack is regenerated after the final commit of a session, so the header's clean-tree line is an invariant, not a hint; and pack markers are only valid at line start (the Makefile legitimately contains marker text mid-line — parsers must anchor).

Commit: `ADR-0003: agent layout + prompt archive; authors-guide pack notes`.
Step 5 — Verify, pack, report

* `make test` green; hook verified firing; `bash` syntax-check any copied scripts (`bash -n`).
* Regenerate the pack (after the final commit) and confirm its header shows the new HEAD and a clean tree.
* Summary must include: Step 0 disposition; pinned versions chosen (and any divergence from the companion's pins); per-skill copy/leave decision with one-line reason; every adaptation made inside copied files; hook verification evidence; files created; commits.

Out of scope
No capability code, no corpus/gate namespaces, no CI config, no Clojars/Maven publishing setup. Do not modify `../ehr-testing-guide`. Do not copy the guide's `notes/handoff-*.md` history or its `.agents/reviews/` — those are its session records, not conventions. Do not write new skills; copying and adapting only.
