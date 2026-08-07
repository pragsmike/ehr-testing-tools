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
2. **Preflight: confirm the session's edit root is the ext4 clone.**
   **Retired 2026-08-05** (scaffolding compaction C, `notes/ADRs.md`
   ADR-0047 AR-C-3): the second, Windows-mounted `/mnt/c` clone this
   step used to guard against — read-only, reject-all-hooks, synced
   only via the now-deleted `bin/sync-mnt-c` (ADR-0030 J4,
   `feedback-dual-clone-edit-hazard`) — no longer exists as a live
   working tree. The hazard class it guarded against is closed
   structurally, not procedurally: Claude Code's own project root now
   points at the ext4 clone by its UNC path
   (`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools`) directly,
   so there is no second clone root left to resolve or mistarget. At
   session start, simply confirm the working directory IS that ext4
   clone (`~/src/ehr-testing-tools`) — if any Read/Edit/Write call ever
   resolves somewhere else again, that is a NEW regression, not a
   known, guarded-against hazard: STOP-AND-REPORT rather than treating
   it as routine vigilance.
3. **All git operations from WSL, never native Windows** —
   `.githooks/pre-commit`/`pre-push` enforce this once `git config
   core.hooksPath .githooks` is set per clone. If working from a
   Windows-launched session, route git through `wsl -e bash -lc "cd
   <repo-path> && <command>"`, one command per invocation — not an
   inline `wsl.exe` call with untrusted interpolation.
4. **Staging hygiene, before every commit.** Run `git diff --cached
   --stat`, record its output. Anything staged outside the checkpoint
   currently in flight gets unstaged (`git restore --staged <path>`)
   before committing — never folded in silently because it happened to
   already be there (`AUTHORS-GUIDE.md` §1, "Staging hygiene between
   checkpoints", R26e).
5. **Personal-info/secrets scan before each commit** — the same
   discipline the pre-push hook's `gitleaks detect` applies, run earlier
   at stage time (`gitleaks git --staged -v`, or `protect --staged`).
6. **Commit message via file, never an inline heredoc through the WSL
   wrapper.** Nested quoting and backticks have silently mangled
   messages crossing Bash-tool → `wsl.exe` → bash — write the message to
   a plain file with a non-shell tool, then `git commit -F <path>` as
   its own simple call.
7. **Push, then verify.** After every push: `git log --format=%B -1`
   against the pushed commit, diffed against the message file that
   produced it. A diff whose only delta is one trailing blank line is
   `git log --format=%B`'s own formatting artifact, not a failure. Any
   other mismatch is never fixed by amending a pushed commit — add a
   fix-forward note to this session's own session record naming what
   the wrapper dropped.
8. **`stable-*` tags are a session act, under license; everything
   else below stays author-only regardless of ceremony mode.** A
   session creates and pushes a `stable-*` continuity tag when (i) its
   own prompt licenses a SPECIFIC tag at a SPECIFIC commit, or (ii) for
   its own predecessor's design-channel-verified stable point, as
   standing ceremony — deferring a licensed tag is now the deviation,
   disclose why if you do (tag law, `notes/ADRs.md` ADR-0057 AR-T-1,
   superseding ADR-0003's author-only scope for this one class of tag
   only; ADR-0003's trust-boundary reasoning for every other AUTHOR
   ACTION item below is otherwise unchanged). A tag already present at
   the exact commit and message is verified and disclosed, never
   re-created. **Release `v*` tags, repo-level `gh` mutations
   (create/delete/settings/visibility), git surgery, and placing
   external documents remain AUTHOR ACTION** — stop and hand these to
   the author regardless of ceremony mode.
9. **Fix-forward with disclosure on premise mismatch.** When a
   checkpoint's stated premise doesn't hold against the live tree, stop,
   record the finding, and ask — don't silently adapt or guess
   (`docs/dev/way-of-working.md` §2; worked examples: the JDK/Temurin
   premise, the gitleaks-hook premise, both in that document).
10. **Red→green for every gate touched.** If a checkpoint adds or edits
   an enforcement test, prove it fails before the fix and passes after
   — don't just assert green.
11. **Session record before the final push (R-A).** The last checkpoint
    of any non-trivial session writes `.agents/session-records/<date>-
    <slug>.md` and archives its own driving prompt to
    `.agents/prompts/<date>-<slug>.md`, both indexed in their own
    README in the same commit — the index-completeness gate
    (`ehrt.docs-tooling.index-completeness-test`) fails the build on a
    missing or ghost entry.

## VERIFICATION

**A regression-oracle claim means SHA-256 digests of output files
across a disposable worktree at the baseline commit — a test-count or
assertion-count comparison is NOT an oracle and may not be reported as
one** (`notes/ADRs.md` ADR-0030, J2, ratified 2026-08-02 after finding
that exact substitution had gone uncaught through two prior sessions'
own dated notes, ADR-0029's D2/D3). `bin/regression-oracle
<baseline-ref> <target-ref>` is the standing harness — two disposable
`git worktree`s, a synthetic from-scratch classpath per worktree
(`:local/root` pointed at that worktree, never a historical commit's
own `deps.edn`), `bin/oracle-src/ehrt/oracle/digest.clj`'s own
fixed-seed golden runs for the vendored-root set current at the time
it runs. A session whose own prompt or ADR entry asserts "the
regression oracle held" or "byte-identical" without naming this
script's own output is making a claim it has not actually verified —
fix the claim (run the script) or fix the wording (name the weaker
method actually used, disclosed as a deviation the way ADR-0029's own
D2 dated note did), never leave it unlabeled.

## Output

Commits landed (or staged, in prepare-only mode) at each checkpoint,
pushed with post-push verification recorded, ending in a session record
and prompt archive.

## Done when

- [ ] Ceremony mode was determined from the session's own prompt, not
      assumed.
- [ ] The session's working directory was confirmed as the ext4
      clone at session start (the `/mnt/c` mirror retired 2026-08-05,
      ADR-0047 AR-C-3 — no second clone root to resolve anymore).
- [ ] The LAST FIVE runs' conclusions for main were checked at session
      start (e.g. `gh run list --limit 5 --branch main`), not just the
      latest — a probabilistic red (an intermittent failure) hides
      behind any single green. All five are disclosed in the session
      record; a red anywhere among them is a finding to report before
      proceeding, never silently passed (ci current, `notes/ADRs.md`
      ADR-0075 AR-CI-3: CI ran red across many commits with nobody —
      neither a build session nor the design channel's own
      verification loop — watching, until a session finally checked;
      widened to five runs, quality riders AR-QR-3, after a session
      found the flaky-test red hiding one run behind an all-green
      "latest run" check). Watching a run TO CONCLUSION stays reserved
      for a session whose own claim is about CI (the AR-CI-4
      precedent) — an ordinary session discloses the five and
      proceeds, never blocks waiting on one still running.
- [ ] `git diff --cached --stat` was reviewed before every commit.
- [ ] Every commit message came from a file, not an inline heredoc.
- [ ] `gitleaks` and `clojure -M:poly check` are green before every push.
- [ ] Every push was verified against its message file.
- [ ] AUTHOR ACTION items were named and left to the author, not taken.
- [ ] Any regression-oracle claim this session made names
      `bin/regression-oracle`'s own output, not a count comparison.
- [ ] The session record and prompt archive land before the final push.
