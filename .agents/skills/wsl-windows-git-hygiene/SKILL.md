---
name: wsl-windows-git-hygiene
description: Diagnose and fix Git worktree noise caused by Windows/WSL mixed checkouts, CRLF/LF churn, Windows-mounted repos, and local editor/tool artifacts. Use when a repo under /mnt/c, /mnt/d, or similar shows unexpected modified files, Git warns that CRLF will be replaced by LF, diffs look like line-ending-only rewrites, or the user mentions WSL, Windows editors, .gitattributes, autocrlf, or a dirty worktree that may be EOL noise.
license: MIT
allowed-tools:
  - codex
  - claude-code
  - opencode
metadata:
  author: pragsmike
  version: 1.0.0
  tags:
    - git
    - documentation
  tested-tools:
    - codex
---

# WSL Windows Git Hygiene

Use this skill to separate real content edits from Windows/WSL line-ending churn and to choose the least disruptive cleanup.

## Intended Scope

This skill is intentionally opinionated toward repositories that:

- are accessed from both Windows and WSL
- often live on Windows-mounted paths such as `/mnt/c` or `/mnt/d`
- show Git worktree noise caused by mixed line-ending behavior, local editor artifacts, or environment-specific Git configuration

It is not a universal Git style guide. It is a troubleshooting and cleanup workflow for mixed Windows/WSL repositories.

## Quick Start

1. Run `scripts/check-eol-noise.sh [repo-path]`.
2. Inspect `.gitattributes`, relevant Git config, and the script's EOL-only vs real-change classification.
3. Apply the standard actions below.

## Standard Actions

- Keep `.gitattributes` as the policy source. Do not weaken it just to hide noisy status output.
- Compare `git diff --stat` with `git diff --ignore-cr-at-eol --stat`.
- If `--ignore-cr-at-eol` removes the diff, treat it as line-ending noise unless other evidence says otherwise.
- Use `git ls-files --eol` on suspect files to compare:
  - index endings
  - working tree endings
  - declared attributes
- Check Git config with:
  - `git config --show-origin --get-regexp '^(core\.(autocrlf|eol|safecrlf|filemode)|merge\.renormalize)$'`
- Recommend:
  - `core.autocrlf=false` in WSL Git
  - `core.autocrlf=false` in Windows Git
  - optionally `core.safecrlf=warn`
- Put editor- or machine-local junk in `.git/info/exclude` when it should stay local to one checkout.
- Prefer `.git/info/exclude` over tracked `.gitignore` for `.vscode/`, `.codex`, `node_modules/`, and similar local artifacts.
- If the repo already has normalization debt, make a dedicated normalization commit.
- Keep normalization commits separate from semantic edits whenever possible.

## Portability Notes

- The examples in this skill use Windows-mounted WSL paths such as `/mnt/c/...` because that is the primary target environment.
- The diagnosis patterns are still broadly useful anywhere Windows and WSL both touch the same Git worktree.
- If a repository lives fully inside the Linux filesystem and is never edited from Windows tools, this skill may be unnecessary.

## Decision Rules

### EOL-only noise

Use this diagnosis when:

- `git diff --ignore-cr-at-eol --stat` goes empty or nearly empty
- Git warns `CRLF will be replaced by LF`
- `git ls-files --eol` shows the worktree as `crlf` or `mixed` while attributes require `eol=lf`

Recommended response:

- keep valid content edits
- normalize the noisy tracked files in a standalone commit
- fix local Git config to reduce recurrence

### Real content changes

Use this diagnosis when:

- `git diff --ignore-cr-at-eol` still shows real hunks
- large CSV or markdown files changed semantically, not just in line endings

Recommended response:

- review and keep or discard those edits on their merits
- do not label them as line-ending noise

### Mixed state

Use this diagnosis when both are present:

- some files disappear under `--ignore-cr-at-eol`
- others still contain real hunks

Recommended response:

- split the commit set
- first normalize EOL-only files
- then commit semantic changes separately

## Watch-Outs

- Windows-mounted repos under `/mnt/c`, `/mnt/d`, and similar can show locking or permission quirks.
- A strict `.gitattributes` file does not retroactively clean old index content; dirty status can still appear until normalization happens.
- Do not assume tracked `.gitignore` is the right home for local editor noise.
- Do not mix line-ending cleanup with unrelated content edits unless the user explicitly wants a single commit.

## Resources

- Diagnosis reference: `references/workflow.md`
- Helper script: `scripts/check-eol-noise.sh`
