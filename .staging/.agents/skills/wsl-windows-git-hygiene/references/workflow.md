# Workflow Reference

This reference assumes a mixed Windows/WSL workflow and is optimized for repos on Windows-mounted paths. That is an intended target environment, not a claim that every repository should be managed this way.

## Symptoms That Should Trigger This Skill

- Repo is on a Windows mount such as `/mnt/c/...` or `/mnt/d/...`
- `git status` suddenly shows many modified files
- `git diff --stat` shows huge rewrites for files that should not have changed
- Git warns `CRLF will be replaced by LF the next time Git touches it`
- The user mentions WSL, Windows editors, `.gitattributes`, `autocrlf`, or "line ending noise"

## Core Commands

Inspect policy and config:

```bash
sed -n '1,220p' .gitattributes
git config --show-origin --get-regexp '^(core\.(autocrlf|eol|safecrlf|filemode)|merge\.renormalize)$'
```

Compare noisy vs EOL-ignored diffs:

```bash
git diff --stat
git diff --ignore-cr-at-eol --stat
git diff --ignore-cr-at-eol --name-only
```

Inspect line-ending state for suspect files:

```bash
git ls-files --eol -- path/to/file1 path/to/file2
```

## Interpretation

- If raw diff is large but `--ignore-cr-at-eol` goes empty, the worktree is mostly dirty from line endings.
- If `.gitattributes` says `eol=lf` but `git ls-files --eol` shows `w/crlf`, `w/mixed`, `i/crlf`, or `i/mixed`, the repo has normalization debt.
- If untracked files are clearly editor-local or machine-local, prefer `.git/info/exclude`.

## Recommended Outcome

1. Preserve real content edits.
2. Exclude local-only noise locally.
3. Normalize tracked EOL-only files in a dedicated commit.
4. Keep semantic edits in a separate commit.
5. Set `core.autocrlf=false` in both WSL Git and Windows Git.

## Local Exclude Guidance

Prefer `.git/info/exclude` when the artifact is specific to one checkout or one machine:

- `.vscode/`
- `.codex`
- `node_modules/`
- scratch files
- temporary package manifests created by local experiments

Use tracked `.gitignore` only when the ignore rule should apply to every clone.
