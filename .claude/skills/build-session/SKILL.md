---
name: build-session
description: >
  Run a checkpointed build session against this repo's own commit/push
  ceremony — WSL-only git, staging hygiene, commit-message-via-file,
  gitleaks, post-push message verification, and the COMMIT/AUTHOR-ACTION
  checkpoint model. Use whenever a session prompt for `ehr-testing-tools`
  names checkpoints (C1, C2, ...) and author rulings (AR-1, AR-2, ...),
  or whenever a session is about to run `git commit`/`git push` in this
  repo. Do not use for repos without this ceremony.
license: MIT
compatibility:
  - codex
  - claude-code
  - opencode
metadata:
  author: pragsmike
  version: 1.0.0
  tags:
    - git
    - ceremony
    - session-mechanics
  tested-tools:
    - claude-code
---

# Build Session

Encodes the checkpoint/COMMIT/AUTHOR-ACTION ceremony this workspace runs
build sessions under, distilled from `AGENTS.md` ("Session mode and
ceremony"), `AUTHORS-GUIDE.md` §1, `docs/dev/way-of-working.md`, and
`notes/ADRs.md` ADR-0007 (R6, R30, and its two dated amendments — R-F,
2026-08-01; post-push message verification, 2026-08-01). Those documents
are the narrative; this skill is the operational checklist a session
actually runs.

## Use this skill when

- A session prompt for this repo names checkpoints and author rulings.
- A session is about to run `git commit`, `git push`, or any command
  that mutates the working tree in `ehr-testing-tools`.

## Do not use this skill when

- Working in a repo without this ceremony (WSL-only git, checkpoint
  model) — check that repo's own `AGENTS.md`/`AUTHORS-GUIDE.md` first.

## Procedure

1. **Determine ceremony mode before touching git.** R30 (commit and push
   at each checkpoint, unattended) is the *standing default* since R-F
   (ADR-0007's 2026-08-01 dated amendment) — a session runs under it
   unless its own prompt states, explicitly, at the start, that this
   session is prepare-only (agent stages and proposes messages, never
   itself commits/pushes/merges/`gh`s). Whichever mode applies, it is
   scoped to this session only — the next session starts back at
   whichever mode its own prompt states.
2. **All git operations from WSL, never native Windows** —
   `.githooks/pre-commit`/`pre-push` enforce this once `git config
   core.hooksPath .githooks` is set per clone. If working from a
   Windows-launched session, route git through `wsl -e bash -lc "cd
   <repo-path> && <command>"`, one command per invocation — not an
   inline `wsl.exe` call with untrusted interpolation.
3. **Staging hygiene, before every commit.** Run `git diff --cached
   --stat`, record its output. Anything staged outside the checkpoint
   currently in flight gets unstaged (`git restore --staged <path>`)
   before committing — never folded in silently because it happened to
   already be there (`AUTHORS-GUIDE.md` §1, "Staging hygiene between
   checkpoints", R26e).
4. **Personal-info/secrets scan before each commit** — the same
   discipline the pre-push hook's `gitleaks detect` applies, run earlier
   at stage time (`gitleaks git --staged -v`, or `protect --staged`).
5. **Commit message via file, never an inline heredoc through the WSL
   wrapper.** Nested quoting and backticks have silently mangled
   messages crossing Bash-tool → `wsl.exe` → bash — write the message to
   a plain file with a non-shell tool, then `git commit -F <path>` as
   its own simple call.
6. **Push, then verify.** After every push: `git log --format=%B -1`
   against the pushed commit, diffed against the message file that
   produced it. A diff whose only delta is one trailing blank line is
   `git log --format=%B`'s own formatting artifact, not a failure. Any
   other mismatch is never fixed by amending a pushed commit — add a
   fix-forward note to this session's own session record naming what
   the wrapper dropped.
7. **AUTHOR ACTION checkpoints stay author-only in every mode** — tags
   (the `stable-*` tag is the actual trust boundary, ADR-0003), and
   repo-level `gh` mutations (create/delete/settings/visibility). Git
   surgery and placing external documents are AUTHOR ACTION too. Stop
   and hand these to the author regardless of ceremony mode.
8. **Fix-forward with disclosure on premise mismatch.** When a
   checkpoint's stated premise doesn't hold against the live tree, stop,
   record the finding, and ask — don't silently adapt or guess
   (`docs/dev/way-of-working.md` §2; worked examples: the JDK/Temurin
   premise, the gitleaks-hook premise, both in that document).
9. **Red→green for every gate touched.** If a checkpoint adds or edits
   an enforcement test, prove it fails before the fix and passes after
   — don't just assert green.
10. **Session record before the final push (R-A).** The last checkpoint
    of any non-trivial session writes `.agents/session-records/<date>-
    <slug>.md` and archives its own driving prompt to
    `.agents/prompts/<date>-<slug>.md`, both indexed in their own
    README in the same commit — the index-completeness gate
    (`ehrt.docs-tooling.index-completeness-test`) fails the build on a
    missing or ghost entry.

## Output

Commits landed (or staged, in prepare-only mode) at each checkpoint,
pushed with post-push verification recorded, ending in a session record
and prompt archive.

## Done when

- [ ] Ceremony mode was determined from the session's own prompt, not
      assumed.
- [ ] `git diff --cached --stat` was reviewed before every commit.
- [ ] Every commit message came from a file, not an inline heredoc.
- [ ] `gitleaks` and `clojure -M:poly check` are green before every push.
- [ ] Every push was verified against its message file.
- [ ] AUTHOR ACTION items were named and left to the author, not taken.
- [ ] The session record and prompt archive land before the final push.
