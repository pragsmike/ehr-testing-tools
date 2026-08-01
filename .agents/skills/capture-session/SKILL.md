---
name: capture-session
description: >
  Turn a ratified design decision or chat ruling into this repo's
  reasoning-of-record — an ADR entry or dated amendment, every doc that
  states the same rule updated in the same commit, and an explicit fence
  naming what the capture deliberately does not execute. Use when a
  design doc, charter, or chat ruling has been accepted and needs to
  land in `notes/ADRs.md` before build sessions implement it. Do not use
  this for the implementation work itself — that is a build session.
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

# Capture Session

Encodes how this workspace turns a ratified decision into its
reasoning-of-record, distilled from `AUTHORS-GUIDE.md` §3 (ADR rules),
`notes/ADRs.md` ADR-0007 (the provenance-tag convention and its own two
dated amendments), and the 2026-08-01 agent-ux capture session (worked
example: `.agents/session-records/2026-08-01-agent-ux-capture.md`,
`notes/ADRs.md` ADR-0023).

## Use this skill when

- A design doc, charter, or chat ruling has been accepted and needs to
  become `notes/ADRs.md` law before build sessions execute it.
- An existing ADR's own rule has been superseded or amended and every
  doc restating that rule needs to move together.

## Do not use this skill when

- The work is executing an already-captured decision (that's
  [`build-session`](../build-session/SKILL.md) or
  [`extraction-stage`](../extraction-stage/SKILL.md)).

## Procedure

1. **Home the decision in `notes/ADRs.md`.** Numbered sequentially,
   Status Accepted unless noted. Never silently revert an Accepted
   decision — supersede it with a new numbered record, or append a
   dated amendment to the existing one (`AUTHORS-GUIDE.md` §3).
2. **Tag provenance.** Mark each ruling `[A]` (author-ruled, verbatim or
   a direct paraphrase the author would recognize as their own) or `[C]`
   (channel-inferred: a reasonable default this workspace's own tooling
   or a prior session supplied, not something the author said in so
   many words) — ADR-0007's own convention, so a future audit can tell
   without re-deriving it from prose. A `[C]` ruling is vetoable
   post-hoc without that counting as reverting an Accepted decision; an
   `[A]` ruling is not.
3. **Prefer a dated amendment over a rewrite.** Append "### Amendments
   (`<date>`, `<session>` — fix-forward, dated, not a revert)" to the
   existing ADR record rather than editing its original Decision text
   in place — the original stands as the historical record of what was
   ruled and why; the amendment supersedes it going forward. Worked
   examples: ADR-0007's own two dated amendments.
4. **Land the same amendment in every doc that states the rule, same
   commit.** A rule captured in one place and left stale in three others
   is worse than not capturing it — `AUTHORS-GUIDE.md` §1 and
   `docs/dev/way-of-working.md` §1 both carry matching dated notes for
   each of ADR-0007's amendments; `AGENTS.md`'s own current-tense
   restatement is edited directly (no amendment note needed there — it
   is operational text, not a historical record).
5. **Name the fence.** State explicitly, in the same commit, what this
   capture deliberately does *not* execute — even work a broader plan
   (a charter, a migration report) already approved. Distinguishes
   "captured as decided" from "captured and also implemented this
   session." Worked example: the 2026-08-01 capture session's AR-7
   fence, naming six items ruled but left untouched.
6. **Handle the self-reference problem.** A commit's own message or tree
   cannot cite the sha it will produce. Use a self-referential phrase
   ("adopted; capture executed by this commit") rather than a `<sha>`
   placeholder that reads as an unfilled blank.
7. **Flag judgment calls as not-yet-ratified.** A phrasing choice or a
   scope reading made while capturing (not something the author stated
   verbatim) goes in the session record as a judgment call, distinct
   from what was actually ruled — ratified elsewhere (an ADR that
   already says so) can be a pointer rather than a repeat.

## Output

New or dated-amended ADR entries in `notes/ADRs.md`; every doc citing
the same rule updated in the same commit; an explicit fence of what was
deliberately not executed.

## Done when

- [ ] The decision is in `notes/ADRs.md`, provenance-tagged.
- [ ] Every other doc stating the same rule was updated in the same
      commit, not left to drift.
- [ ] What this capture does *not* do is named, not silently implied.
- [ ] Judgment calls are flagged as unratified where the author didn't
      speak verbatim.
