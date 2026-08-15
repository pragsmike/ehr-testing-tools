# Carve-loss audit

Produced 2026-07-28, `2026-07-28-ehr-testing-carve-loss-recovery` session,
step 1. Compares every non-generated path at `ehr-testing-tools`'
`stable-pre-monorepo` tag and `ehr-testing-sim`'s final pre-merge tree
(merge commit `a0534d0`, staged under `.staging/`) against the current
workspace tree. `src/`, `test/`, `test-integration/`, `resources/`, and
`docs/demos/` are excluded from the row-by-row listing below: their
mechanical rename is already exhaustively verified elsewhere (ADR-0001's
property-law/CLI-smoke verification, ADR-0002's `poly test` counts and
CLI-smoke bytes) and a direct file-by-file diff of both trees against
the current tree, run for this audit, found **zero** unaccounted-for
paths in either of those subtrees. This audit's own value is in the
non-source, easy-to-wave-off-as-"superseded" material — exactly the
class of loss the three post-landing failures (Makefile, README.md,
skills) all came from.

## Method

`git ls-tree -r stable-pre-monorepo` and `git ls-tree -r a0534d0` (the
latter's `.staging/` prefix stripped) diffed by path against `git
ls-files` on the current tree, with known rename patterns applied
(`.agents/*` → `notes/{tools,sim}/agents/*`, `docs/*` →
`components/{tools,sim}/docs/*`, `palgebra/*` → `components/palgebra/*`,
etc.) before anything is called missing. Every row below was checked by
reading the actual file (pre-carve and, where applicable, current), not
by path-matching alone.

## Disposition rows

### Tools-side (`ehr-testing-tools` @ `stable-pre-monorepo`)

| Path (pre-carve) | Disposition | Detail |
|---|---|---|
| `.agents/handoffs/*`, `.agents/memory/*`, `.agents/plans/*`, `.agents/prompts/archive/*` (49→41 files, see note), `agent/scenario-roster.md` | SUPERSEDED-CORRECTLY | All present verbatim under `notes/tools/agents/{handoffs,memory,plans}/` and `notes/tools/prompts/` (archive's own directory segment dropped, files flattened one level) and `notes/tools/agent/scenario-roster.md`. Filename-diffed: 41/41 archived prompts present, zero missing (an initial hand-count of the pre-carve listing suggested 49; recount by script gives 41 on both sides — the higher number was miscounted, not a real gap). |
| `.agents/skills/{committee,find-skills,probe,repo-adaptation,review,scenarios,shared-skill-layout,wsl-windows-git-hygiene}/*`, plus tools' own divergent `string-diagram`/`committee` copies | SUPERSEDED-CORRECTLY, as provenance only | Present under `notes/tools/agents/skills/*` (ADR-0002's own disclosed decision: "provenance only, not brought live"). **Not live** in workspace `.agents/skills/` — this is exactly what step 5 (R21, skills union) of this session corrects; that step's own row supersedes this one going forward. |
| `docs/*` (23 files: GLOSSARY.md, README.md, cli.md, components.md, experiments*, formats.md, judge-calibration.md, locators.md, notation.md, operators.md, palgebra-design.md, pipeline.{edn,md}, positioning.md, research/*, signature.edn, source-sink-design.md, use-cases.{edn,md}) | SUPERSEDED-CORRECTLY | Diffed file-by-file against `components/tools/docs/*`: zero missing. Full 1:1 mapping. |
| `artifacts.lock.edn`, `config/synthea/synthea.properties`, `bin/{check-palgebra-drift,ehr,quickstart-demo}`, `palgebra/HISTORY.md`, `palgebra/tools/resource_equations_to_mermaid.py` | SUPERSEDED-CORRECTLY | Moved intact to workspace root / `components/palgebra/`, per ADR-0002's own disclosed cwd-relative-path fix. Executable bits on the `bin/*` + palgebra tool script were themselves lost in transit and only just repaired (`2026-07-28-ehr-testing-ci-red-executable-bits` session, same day) — the *content* move was correct; the *mode* loss is that session's own finding, not a new one here. |
| `.gitattributes`, `.githooks/{pre-commit,pre-push}`, `.gitignore`, `AGENTS.md`, `AUTHORS-GUIDE.md`, `LICENSE`, `SETUP.md`, `deps.edn` | SUPERSEDED-CORRECTLY | Workspace has its own versions of every one of these, per ADR-0001 R4/R8's own explicit disclosure ("root config/doc files... superseded by the workspace's own versions written at step 5"). Confirmed present and current. |
| `notes/ADRs.md`, `notes/facts-register.md`, `notes/ehr-testing-sim-mounting-note.md` | SUPERSEDED-CORRECTLY | Present verbatim at `notes/tools/ADRs.md`, `notes/tools/facts-register.md`, `notes/tools/ehr-testing-sim-mounting-note.md`. |
| `.github/workflows/integration.yml` (nightly, `workflow_dispatch`, artifact-cache-primed integration lane) | **DROPPED-WRONGLY** | No workspace equivalent exists at all. Direct cause of today's CI failure: with no structural lane separating artifact-dependent tests, `poly test :all`/`:project` picked up all 13 `^:integration` namespaces unconditionally (see step 0's pinned mechanism, `notes/prompts/2026-07-28-ehr-testing-carve-loss-recovery.md`). **Restore: this session, step 3** (R18/R19) — not a byte-for-byte port (the fetch/cache-priming shape carries over; the invocation becomes `poly test project:integration` per R19's structural split, not `make integration`'s old alias-selector mechanism). |
| `.github/workflows/ci.yml`'s test-lane exclusion of `^:integration` (via `deps.edn`'s pre-carve `:test`/`:integration` alias split, metadata-based `:excludes [:integration]`) | **DROPPED-WRONGLY** | The mechanism, not just the file, is gone: `projects/conformance/deps.edn`'s own `:test` alias has no exclusion at all. **Restore: this session, step 3**, but per R19 as a *structural* project split (`projects/integration`), not a metadata selector — R19 explicitly supersedes the old mechanism rather than reinstating it verbatim. |
| `.github/workflows/ci.yml`'s other per-push gates: `make lint-pipeline`, `make lint-deps`, coverage floor (85% forms via cloverage `--fail-threshold`), generated-doc freshness (`pipeline.md`/`use-cases.md`/`operators.md`/`cli.md` regenerate-and-diff), `bin/ehr` CLI smoke, `quickstart-fresh` | SUPERSEDED-CORRECTLY, not reopened here | These were part of ADR-0002's own disclosed, author-approved "Makefile dropped, not ported" decision ("none of its targets are named in R11–R17... offered as an explicit either/or"). R18–R23 do not name any of these gates, so this session does not restore them. Named here so the gap stays visible rather than silently re-absorbed into the Makefile/integration-lane restoration this session *does* make — a future session's call, not this one's. |
| `Makefile` (pack/pack-skills/pack-push, pipeline/use-cases/operators-doc/cli-doc docsgen targets, lint-pipeline/lint-deps, check-palgebra-drift, `coverage`, `integration-smoke`) | SUPERSEDED-CORRECTLY, not reopened here | Same ADR-0002 disclosed decision as the row above. `pack-push` was already "Dormant" per the pre-carve Makefile's own comment before the carve even happened. |
| `Makefile`'s `test`/`integration`/`quickstart-demo` targets specifically | **DROPPED-WRONGLY** | These three (plus a new `ci-parity`) are exactly what R23 asks this session to restore, thin, as poly-command entry points — step 6. |
| `docs/*` regeneration tooling (`make pipeline`/`use-cases`/`operators-doc`/`cli-doc`, `docsgen.clj`'s live callers) | **DROPPED-WRONGLY**, named-future | Already disclosed in ADR-0002's own deviation record (the `docsgen.clj` dependency-direction fix) and again in the pending closeout-sweep prompt's own step 4, which owns this fix. Not duplicated here — this row exists so the audit's inventory is complete, not to re-scope the work. |
| `CLAUDE.md` | **DROPPED-WRONGLY** | Genuinely absent from the entire current tree (`git ls-files | grep -i claude` returns nothing) — not moved, not renamed, just never written during bootstrap, despite the workspace's own `AGENTS.md` already promising it ("Claude Code users: see `CLAUDE.md`, which points here"). Both tools' and sim's pre-carve copies were simple pointer files to their own `AGENTS.md`; sim's was the shorter, cleaner of the two. **Restore: this session, step 6** (bundled with the README/Makefile commit — same "public/agent-facing entry surface" theme as R22, sim's form per R4 since the two pre-carve versions differ only in verbosity). This wasn't named in R18–R23 directly; flagged here as a genuine finding this audit surfaced, disposed per the audit's own DROPPED-WRONGLY/this-session option rather than a fresh stop-and-ask, since it's low-risk and directly serves R22's own stated intent. |
| `.claude/settings.json` (a git-*tracked*, repo-shared Claude Code permissions allowlist — distinct from the untracked `.claude/settings.local.json` the closeout-sweep session's own step 3 is about to `.gitignore`) | RULED: SUPERSEDED-CORRECTLY (author, live, this session) | Author's ruling, given directly in-session: don't commit `.claude/settings.json` in this workspace. `.claude/` stays entirely untracked, matching the closeout-sweep session's own pending `.gitignore` step; nothing under `.claude/` was touched this session. |

### Sim-side (`ehr-testing-sim` @ final pre-merge tree, merge commit `a0534d0`)

Clean. Every non-source path diffed (`.agents/*`, `.editorconfig`,
`.github/ISSUE_TEMPLATE/*`, `.gitattributes`, `.githooks/*`,
`.gitignore`, `AGENTS.md`, `AUTHORS-GUIDE.md`, `CONTRIBUTING.md`,
`LICENSE`, `Makefile`, `NOTICE`, `README.md`, `SETUP.md`, `deps.edn`,
`docs/*` — 21 files — `notes/ADRs.md`, `notes/facts-register.md`) and
every one resolves to SUPERSEDED-CORRECTLY, already disclosed in
ADR-0001's own deviation record (the "Residual `.staging/` files, step
6→7 seam" section) or trivially present at its expected workspace
location. One specific check worth naming since it looked, at first
glance, like a possible silent loss: sim's own pre-carve
`docs/way-of-working.md` (a ~forty-session meta-process narrative,
distinct in content from the workspace's own current
`docs/way-of-working.md`, which is a new document about *this*
workspace's bootstrap conventions) is not preserved anywhere as
provenance under `notes/sim/`. This is not a new finding — ADR-0001's
own text already discloses it: sim's root docs, `docs/way-of-working.md`
included, are named as "superseded by the workspace's own versions...
still reachable in git history via the merge commit." Restated here
only to confirm the audit checked it and found the existing disclosure
sufficient, not silently skipped it.

## UNDECIDED — gate for step 2

One row was UNDECIDED at audit time: `.claude/settings.json`
(git-tracked shared permissions allowlist). Ruled live, in-session, by
the author during step 3: don't commit it — `.claude/` stays entirely
untracked. Zero rows remain UNDECIDED; every row in this audit now has
a disposition.

## Summary counts

- Tools-side rows: 15. SUPERSEDED-CORRECTLY: 8 (5 fully so, 3 "not
  reopened here" — disclosed gaps left for a future session).
  DROPPED-WRONGLY: 6 (2 restored this session's step 3 — integration
  lane + its structural mechanism; 1 restored this session's step 6 —
  Makefile's three targets plus `ci-parity`; 1 restored this session's
  step 6 — `CLAUDE.md`; 1 named-future — docsgen regen, owned by the
  pending closeout sweep; 1 is really "skills," disposed by step 5
  rather than left as a bare row). UNDECIDED at audit time: 1
  (`.claude/settings.json`), ruled SUPERSEDED-CORRECTLY live, in-session,
  before step 3 completed. Zero rows remain open.
- Sim-side rows: 0 open findings — fully accounted for by ADR-0001's
  own prior disclosure.

## Accepted warts (R29, discipline-parity session, 2026-07-28)

Four root residents are deliberate, not oversight, and are recorded
here — not as a disposition row (none of them are carve *losses*; all
four are things that legitimately never had a brick to live in) but so
"ambient at the workspace root" reads as a named, accepted state with
its own exit plan rather than unexamined clutter:

| Path | Why it's at root | Exit plan |
|---|---|---|
| `bin/` (`ehr`, `quickstart-demo`; `check-palgebra-drift` until 2026-08-15, see the retirement row below) | Entry-point scripts; `bin/ehr` itself must `cd` to the workspace root before `exec`ing (ADR-0002's own cwd-relative-path finding) — a script whose whole job is establishing "workspace root as cwd" can't itself live inside a brick without begging the question. | None currently planned — this is likely a permanent root resident, not a staging state; named here for completeness, not because a move is expected. |
| `config/synthea/synthea.properties` | Synthea's own properties-file input, read cwd-relative by `ehrt.tools.corpus.generate` (ADR-0002). | Brick-owned resources, future session — once/if `components/tools` grows a `resources/tools/` tree (this session added `test-fixtures/`, a sibling, not this), config could move there with a matching cwd-relative-path update, same treatment fixtures just got. |
| `resources/synthea-default.properties` | Same reason and same consumer as the row above; historically ungrouped from `config/synthea/` for reasons not recorded. | Same exit plan as above — likely belongs alongside `config/synthea/synthea.properties`, not treated as a separate question, when that future session runs. |
| `artifacts.lock.edn` | The artifact registry `ehrt.tools.artifact` reads cwd-relative (ADR-0002); genuinely workspace-wide (both `projects/sim` and `projects/tools-cli` artifact-fetch through it), so no single brick is the obvious owner even under the resources-migration plan above. | No exit plan named — this one may be a permanent root resident on its own merits (a workspace-wide registry isn't naturally brick-scoped the way fixtures or one component's config is); revisit only if a future session identifies a real owning brick. |

None of these four block anything; they're recorded so a future
top-level-tidy pass doesn't have to re-derive from scratch why they're
still there.

## Later dispositions

Rows added after the 2026-07-28 audit proper, for paths whose
disposition changed once the merged workspace made the original
judgement obsolete. Same discipline: what was decided, on what
evidence, and by whom.

| Path | Disposition | Detail |
|---|---|---|
| `bin/check-palgebra-drift` | **RETIRED-AS-INERT** (author ruling "accept all.", 2026-08-15; repo review 3 finding D1-5, R-1; ADR-0136) | Deleted. Its own header called it a "Nightly drift check"; nothing scheduled it and nothing invoked it. Zero-caller inventory re-derived at deletion, not inherited from the register: `Makefile` — 1 hit, and it is the comment listing the script among pre-carve targets that "stay superseded", not an invocation; `.github/workflows/test.yml` — 0; `.github/workflows/integration.yml` — 0; all of `bin/` excluding the script itself — 0; all of `.agents/skills/` — 0. Every other tracked hit is prose (this audit, the review registers, ADR-0002/0004/0127, archived prompts, and `bases/cli/test/ehrt/cli/executable_bits_test.clj`'s docstring, which cites it as the historical first instance of the mode-loss bug class). It could not have fired in any case: it diffs this repo's palgebra files against copies vendored into a sibling `../ehr-testing-sim` checkout, and that repo was consolidated INTO this workspace at the merge (`a0534d0`) — the premise is gone, so the check clean-skipped by construction, not merely by absence. A guard that cannot fire is worse than no guard, because its presence reads as coverage: review 3's D5-4 found three stale `.mermaid` outputs sitting inside exactly the directory this script nominally watched, and the script was never going to see them (it pairs the `.txt` sources and the `.py`, never the outputs). That coverage is now real and mechanized — `make palgebra-examples` plus CI's freshness diff, same commit family. Same shape as review 2's D2-4 (`verify-nist-lock`'s false enforcement claim), found, ruled and fixed; this was its unfound sibling. `bases/cli/test/ehrt/cli/executable_bits_test.clj` enumerates tracked files dynamically, so the deletion shrinks its population and needs no edit there. If an `ehr-testing-sim` checkout is ever maintained again, the check is recoverable from history at `fca52ec`. |
