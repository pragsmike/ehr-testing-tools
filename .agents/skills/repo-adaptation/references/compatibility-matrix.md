# Compatibility Matrix

How the target `AGENTS.md` + `.agents/` structure relates to current AI coding tools.

**Read `## Verified findings (2026-08-01)` below before trusting the Claude
Code row of the table that follows** — it was wrong in a way that mattered
(discovery path, not just phrasing) until this pass, and the fix was made by
empirical test, not by re-reading vendor docs.

## Tool support overview

| Tool | Primary instruction file | Reads `AGENTS.md`? | Reads `.agents/skills/`? | Legacy files |
|---|---|---|---|---|
| **OpenAI Codex** | `AGENTS.md` | Yes (native) | Yes (native) | N/A — unverified this pass, carried from original authoring |
| **OpenCode** | `AGENTS.md` | Yes (preferred) | Project rules supported | Falls back to `CLAUDE.md` — unverified this pass, carried from original authoring |
| **Claude Code** | `CLAUDE.md` (auto-loaded); `AGENTS.md` only if a human points to it | No — see findings below | **No.** Reads `.claude/skills/<slug>/SKILL.md` (project) or `~/.claude/skills/<slug>/` (personal), or a plugin/marketplace install. See findings below. | `CLAUDE.md`, `.claude/` |
| **Cursor** | `.cursorrules` | No (manual) | No | `.cursorrules`, `.cursorignore` — unverified this pass, carried from original authoring |
| **GitHub Copilot** | `.github/copilot-instructions.md` | No | No | Own format — unverified this pass, carried from original authoring |
| **Windsurf** | `.windsurfrules` | No | No | Own format — unverified this pass, carried from original authoring |
| **Aider** | `.aider.conf.yml` | No | No | Own format — unverified this pass, carried from original authoring |

## Verified findings (2026-08-01)

Tested tool: **Claude Code 2.1.63**, inside `ehr-testing-tools` (a repo whose
`.agents/skills/` holds 11 skill directories, including this one). Everything
else in this file's rows was left as originally authored — plausible, but not
independently re-tested this pass; only the Claude Code row above was
actually exercised, because that was the tool available to test with.

**1. Direct observation, this session.** The running session's own
Skill-tool listing did not contain any of `.agents/skills/`'s 11 entries
under their own names — not `committee`, `find-skills`, `handoff`, `probe`,
`review`, `scenarios`, `shared-skill-layout`, `string-diagram`,
`wsl-windows-git-hygiene`, nor a bare `repo-adaptation`. One entry,
`anthropic-skills:repo-adaptation`, did appear, with a description
byte-for-byte identical to this file's own `SKILL.md` frontmatter
description — but a full filename search of `~/.claude` (all of
`plugins/`, `plugins/marketplaces/claude-plugins-official/`, `cache/`,
`downloads/`, etc.) turned up zero files or directories named
`repo-adaptation` or `anthropic-skills` anywhere on disk, and the one
registered marketplace (`claude-plugins-official`) has no plugin by either
name. Conclusion: `anthropic-skills:repo-adaptation` is a fixed, built-in
skill the product ships regardless of repo content — its match to this
file's wording is a notable coincidence (or convergent authorship on the
same well-known problem), not evidence that `.agents/skills/` was read.

**2. Live probe.** Created `.claude/skills/zzz-discovery-probe-20260801/SKILL.md`
(unique name, thrown away immediately after) in the same repo. Neither this
session's own listing nor a freshly spawned subagent's listing picked it up.
Inconclusive by itself — a subagent may inherit a catalog fixed at session
start rather than rescanning the filesystem live — but consistent with (3)
and (4) below, and it rules out "just add the file and it works mid-session."

**3. Primary-source corroboration.** Cloned `pragsmike/skills` (HEAD
`311b022`, the sole commit) this session. Its own `README.md` and
`docs/adoption-guide.md` state Claude Code's supported install paths as
project `.claude/skills/<name>/` and personal `~/.claude/skills/<name>/`,
plus `/plugin marketplace add pragsmike/skills` — never `.agents/skills/`.
The same docs give paths for Cursor (`.cursor/skills/`), Gemini CLI
(`.gemini/skills/`), GitHub Copilot (`.github/skills/`), and Windsurf
(`.windsurf/skills/`) that are newer than this file's own "No/No/No" rows for
those tools — plausible (skill-style conventions have been spreading), but
sourced from one library maintainer's adoption guide, not those vendors'
own docs, so not folded into the table above as verified fact.

**4. Cross-repo corroboration.** `pragsmike/cyberneutics` (HEAD `c9aef26`),
a repo actively used day-to-day with Claude Code, keeps its 8 skills —
several sharing this repo's own names (`committee`, `handoff`, `probe`,
`review`, `scenarios`, `string-diagram`) — at `.claude/skills/`, and uses
`.agents/` for something else entirely (five persona files: `frankie.md`,
`joe.md`, `maya.md`, `tammy.md`, `vic.md` — not skills). One namespace,
two unrelated live conventions across pragsmike's own repos.

**Bottom line:** `.agents/skills/` is not a Claude Code discovery path in
this environment. `ehr-testing-tools`'s own 11-skill directory there is
invisible to Claude Code today — a fact inherited unmodified from sim's
original bootstrap (`notes/ADRs.md`, the workspace-formation ADR's R8:
"Sim's own copy of `.agents/skills/` is dropped as a duplicate — the
workspace's live `.agents/skills/` already carries that content forward"),
never independently checked against real tool behavior until this pass.
Whether/how to fix that for this repo (dual registration, a `.claude/skills`
compat layer, or something else) is a migration-report decision, not this
skill file's to make — see `.agents/plans/2026-08-01-migration-report.md`.

## Compatibility strategies

### Teams using Codex + OpenCode

Best case. Both tools natively read `AGENTS.md`. Migration is straightforward — consolidate everything into `AGENTS.md` and `.agents/skills/`. OpenCode will also fall back to `CLAUDE.md` if it exists, so keeping it as a shim is safe but optional.

### Teams using Claude Code

Claude Code reads `CLAUDE.md` and `.claude/` natively. Until Claude Code adds native `AGENTS.md` support:

- Keep `CLAUDE.md` as a compatibility shim that either duplicates or points to `AGENTS.md`.
- Map `.claude/commands/` to `.agents/skills/` and keep the old commands as thin wrappers if needed.
- Document this dual-maintenance requirement in the migration report.

### Teams using Cursor

Cursor reads `.cursorrules` for code style guidance. Since Cursor does not read `AGENTS.md`:

- Keep `.cursorrules` in place for Cursor users.
- Consolidate the same rules into the code conventions section of `AGENTS.md`.
- Note in the migration report that `.cursorrules` and `AGENTS.md` contain overlapping content and should be kept in sync.

### Teams using GitHub Copilot

Copilot reads `.github/copilot-instructions.md`. Similar to Cursor:

- Keep the Copilot file for Copilot users.
- Merge content into `AGENTS.md`.
- Document the overlap.

### Multi-tool teams

When a team uses 3+ tools, the migration report should explicitly list which files serve which tools and recommend a sync strategy. Common approaches:

1. **Single source with shims**: `AGENTS.md` is authoritative; tool-specific files are generated or manually synced from it.
2. **Parallel maintenance**: Each file is maintained independently. Workable for small files but error-prone at scale.
3. **Gradual convergence**: Start with parallel maintenance, migrate tools to `AGENTS.md` as they add support.

Recommend option 1 for new setups and option 3 for existing teams with entrenched workflows.

## What NOT to assume

- Do not assume all tools can read `AGENTS.md`. Check the matrix above.
- Do not assume legacy files are safe to delete. They may serve tools that are actively in use.
- Do not hardcode vendor-exclusive features into `AGENTS.md` (e.g., Claude-specific XML tags or Cursor-specific ignore patterns). Keep the content tool-neutral and add tool-specific notes in a clearly labeled compatibility section.
