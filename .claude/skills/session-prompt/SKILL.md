---
name: session-prompt
description: >
  Author a session prompt for `ehr-testing-tools` — the design channel's
  own preflight (re-read the repo before writing from memory), the
  canonical prompt anatomy (context, author rulings, checkpoints,
  close-out), and provenance citation for every "ruled" claim. Use when
  drafting a new session prompt for this repo's build/capture sessions.
  Do not use this to run a session that already has its prompt — that is
  build-session, capture-session, extraction-stage, or errata-sweep.
license: MIT
compatibility:
  - codex
  - claude-code
  - opencode
metadata:
  author: pragsmike
  version: 1.0.0
  tags:
    - documentation
    - session-mechanics
  tested-tools:
    - claude-code
---

# Session Prompt

Encodes the design channel's own preflight discipline (charter R-B,
`.agents/plans/2026-08-01-agent-ux-charter.md`) and the prompt anatomy
observed consistently across every session prompt from
`2026-08-01-migration-session-1.md` through
`2026-08-02-migration-session-4.md`.

## Use this skill when

- Drafting a new session prompt for this repo — a build, capture,
  extraction, or errata-sweep session.

## Do not use this skill when

- A prompt already exists and the task is to run the session it
  describes (use [`build-session`](../build-session/SKILL.md) and
  whichever of `capture-session`/`extraction-stage`/`errata-sweep` fits
  the session's own shape).

## Procedure

1. **Preflight before writing a word (R-B).** Re-read `AGENTS.md`'s head
   (the mode/ceremony section), the `.agents/` index READMEs (`skills/`,
   `plans/`, `session-records/`, `prompts/`), and the current mode
   rulings — is R30 or prepare-only standing; what does
   `.agents/plans/roadmap.md`'s "Now"/"Next" section say is next.
   Prompt authoring starts from the repo, not from memory — a prompt
   written from a stale mental model propagates its author's gaps at
   session rate (charter §2, diagnosis point 2).
2. **State the HEAD sha read.** The prompt's own context paragraph
   states the commit its conventions were read at — so a future reader
   can tell whether a since-changed rule was actually visible when the
   prompt was written.
3. **Follow the canonical anatomy:**
   - **Title/date line** — `YYYY-MM-DD — <repo>: <session name>`.
   - **Context** — which numbered build session this is, what roadmap
     or migration-report item(s) it executes, what stays fenced, and the
     ceremony reminder (standing ceremony or prepare-only; which clone;
     fast-forward and record HEAD; other clone untouched; roadmap rows
     land same-commit).
   - **Author rulings** — numbered `AR-1`, `AR-2`, ...; each one
     concrete and actionable, not a restatement of background. Tag `[A]`
     (author-ruled) or `[C]` (channel-inferred) per ADR-0007's own
     provenance convention when a future audit might need to tell which
     is which.
   - **Checkpoints** — numbered `C1`, `C2`, ...; each names its intended
     commit message *verbatim*, so the session doesn't improvise scope
     at commit time.
   - **Close-out** — what the final session record must contain (HEAD,
     shas, accounting tables, any budget deltas, post-push verification)
     and how much gets echoed to chat.
4. **Cite provenance for every "ruled" claim.** A claim that something
   was already decided should name the roadmap row, ADR number, or prior
   session record it comes from — "as discussed" is not a citation.
5. **Fence explicitly.** Name what this session will *not* do, even
   if a broader plan already approved it — this is what stops a later
   session reading silence as authorization.
6. **Archive it.** The prompt a session actually runs under gets
   archived verbatim to `.agents/prompts/<date>-<slug>.md` as part of
   that session's own close-out, paired with its session record, indexed
   in `.agents/prompts/README.md` in the same commit
   (`build-session`'s step 10 covers the mechanics).

## Output

A session prompt with a HEAD-cited preflight, numbered author rulings
(provenance-tagged where it matters), checkpoints with verbatim commit
messages, and a stated close-out and fence.

## Done when

- [ ] The prompt's context paragraph states the HEAD sha it was read at.
- [ ] Every ruling traces to a real roadmap row, ADR, or prior record.
- [ ] Checkpoints name their commit messages verbatim.
- [ ] The prompt states what it deliberately does not authorize.
