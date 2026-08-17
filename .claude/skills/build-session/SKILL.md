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
  version: 2.0.0
  tags:
    - git
    - ceremony
    - session-mechanics
  tested-tools:
    - claude-code
---

# Build Session

What to DO at each checkpoint of a build session in this repo. The standing rules
are rows in `.agents/rulings.md`; the incidents and near-misses that made each step
a step are in `HISTORY.md` beside this file (split out by ADR-0145).

## Use this skill when

- A session prompt for this repo names checkpoints and author rulings.
- A session is about to run `git commit`, `git push`, or anything else that
  mutates the working tree in `ehr-testing-tools`.

## Do not use this skill when

- Working in a repo without this ceremony — read that repo's own `AGENTS.md` first.

## Procedure

1. **Take ceremony mode from the prompt, never by assumption.** R30 — commit and
   push at each checkpoint, unattended — is the standing default; prepare-only must
   be stated explicitly and binds this session only (ADR-0007 R-F).
2. **Run `bin/preflight [--branch BRANCH]` before touching git**, and disclose every
   finding it prints — CI, edit-root, tree-clean, HEAD-vs-remote, tag state (ADR-0127).
3. **Run all git from WSL, never native Windows.** `.githooks/pre-commit`/`pre-push`
   enforce it once `git config core.hooksPath .githooks` is set per clone.
4. **Read `git diff --cached --stat` before every commit** and unstage anything
   outside the checkpoint in flight (`AUTHORS-GUIDE.md` §1, R26e).
5. **Scan for secrets at stage time**, not just at push: `gitleaks git --staged -v`.
6. **Write each commit message to a file and `git commit -F <path>`** — never an
   inline heredoc through a shell wrapper, which has silently mangled messages.
7. **Isolate a checkpoint that pairs a src fix with its own test**: disposable `git
   stash` the smaller half so the red run exercises exactly the unfixed code, `stash
   pop` before green. One cycle per independent fix (ADR-0129).
8. **Capture red for every enforcement test you add or edit**, and put the red run's
   own output in the session record — false positives included, not filtered.
9. **Back an "every occurrence of X" fix with a sweep census**: every hit with
   file:line, the ones correctly left untouched named too, and why (ADR-0117).
10. **Run `bin/post-push-verify [<base-sha>] [<tip-sha>]` after every push** —
    remote tip, per-commit ASCII over the pushed range, CI run reported once (AR-RL2-5).
11. **Take `stable-*` tags through `bin/tag-ceremony <tag> <sha> <msg-file>
    [--push]`, under licence** (`rulings.md#R-tag-law`). Release `v*` tags,
    repo-level `gh` mutations, git surgery and placing external documents stay
    AUTHOR ACTION — hand them to the author whatever the ceremony mode.
12. **Stop and report on a premise mismatch.** A checkpoint whose stated premise does
    not hold against the live tree is a finding, not something to adapt around
    (`docs/dev/way-of-working.md` §2).
13. **Run `bin/close-scaffold <YYYY-MM-DD> <slug> <description>` before the final
    push**, then fill in its stubs: it writes the session record, the prompt archive
    and both README index lines the two index gates check (R-A).
14. **Move this session's own closed rows to `Done` and re-measure every reading set
    at the close** (`rulings.md#R-register-hygiene-at-close`).
15. **Over a reading-set budget, compact or STOP-AND-REPORT — never bump**
    (`rulings.md#R-budget-stop`); the ratchet baseline makes the bump unavailable.
16. **Push a red-first commit together with its green successor, never alone**
    (`rulings.md#R-red-pushed-with-green`), and disclose it when it happens.
17. **Edit register files by anchored insertion or replacement, never by slicing
    between two anchors** (`rulings.md#R-anchored-register-edits`).

## Verification

- **A regression-oracle claim means `bin/regression-oracle <baseline> <target>` and
  that script's own output.** A test-count or assertion-count comparison is not an
  oracle and may not be reported as one (`rulings.md#R-oracle-script-contract`).
- **Every gate run goes to a full log with its exit code captured explicitly** —
  `make test > <log> 2>&1; MAKE_EXIT=$?`. A pipe or `tail` returns its own status
  and truncates the counts you reconcile against (review-3 D2-6).
- **Catching yourself drafting a justification for skipping an instructed step is
  the stop signal.** Do the step or STOP-AND-REPORT; the drafted excuse goes in the
  session record either way (ADR-0128).

## Output

Commits landed (or staged, in prepare-only mode) at each checkpoint, each push
verified, ending in a session record and an archived prompt.

## Done when

- [ ] Ceremony mode came from the prompt, and `bin/preflight` ran with every
      finding disclosed.
- [ ] `git diff --cached --stat` was read, and gitleaks run, before every commit.
- [ ] Every commit message came from a file; `clojure -M:poly check` is green.
- [ ] Every gate run went to a log with its exit code captured explicitly.
- [ ] A src-fix-plus-test checkpoint captured red under stash isolation, and every
      enforcement test proved red-before-green with its own real output.
- [ ] An "every occurrence" fix carries its sweep census.
- [ ] `bin/post-push-verify` ran after every push, its three checks recorded.
- [ ] Any `stable-*` tag went through `bin/tag-ceremony` under an explicit licence,
      ending in peeled-ref verification; AUTHOR ACTION items were left to the author.
- [ ] Any regression-oracle claim names `bin/regression-oracle`'s own output.
- [ ] `bin/close-scaffold` ran before the final push and its stubs are filled in.
- [ ] Closed rows moved to `Done`; all five reading sets re-measured and recorded.
