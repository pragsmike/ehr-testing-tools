# Way of working

This describes the *meta-process* for this workspace: not what was
decided (`notes/ADRs.md` is that record), but how a bootstrap-and-
landing session is run here. It is adapted from
[`ehr-testing-sim`'s own `docs/way-of-working.md`](https://github.com/pragsmike/ehr-testing-sim/blob/main/docs/way-of-working.md)
— read that document for the fuller, 40-session culture that produced
sim itself; sim's discipline is canonical here per `notes/ADRs.md`
ADR-0001 (R4), but this workspace's own *process* is genuinely
different from sim's, and this document is about the difference, not a
re-statement of sim's own history.

**Audience:** a future maintainer of this workspace — human or AI —
picking it up cold.

## 1. What's different here from sim's own process

Sim was built across roughly forty stateless sessions, none of which
saw another's transcript, orchestrated from one long-running design
conversation. This workspace's bootstrap-and-landing work runs
differently: **one session prompt, author present throughout, real-
time delegation possible.** Concretely:

- The session prompt itself carries the rulings (numbered R1, R2, ...)
  that sim's design channel would have argued out separately and
  recorded as ADRs before a build session ever started. Here, capture
  and build happen inside the same session, against a prompt the
  author already ratified before the session began.
- **Checkpoints, not full statelessness.** The prompt marks COMMIT
  points (a discrete unit of work the author reviews and commits) and
  AUTHOR ACTION points (git surgery, placing external documents —
  things only the author does, by design, regardless of how the
  session's git-delegation is currently set for that session). A
  session does not blow through either kind of checkpoint
  unsupervised.
- **Default: agent prepares, author commits — unless the session's own
  prompt says otherwise.** `notes/ADRs.md` ADR-0001 (R6) states the
  default; `notes/ADRs.md` ADR-0007 (R30) names the standing
  alternative mode, commit-and-push-at-each-checkpoint, that a session
  runs under when its own prompt says so at the start, not a per-push
  ask. Either way it is a live, scoped grant for that session, not a
  rewrite of the default for the next one. `AUTHORS-GUIDE.md` §1 has
  the exact boundary.

  **Amendment, 2026-08-01 (R-F ratified, `notes/ADRs.md` ADR-0007's
  own dated amendment):** the default named above is superseded in
  place — R30 (commit-and-push-at-each-checkpoint, unattended) is now
  the standing default; prepare-only is the exception a session's own
  prompt must state. `AUTHORS-GUIDE.md` §1 and ADR-0007's own amendment
  carry the full detail.

  **Amendment, 2026-08-01 (skill-adaptation session, ADR-0007's own
  second dated amendment):** the ceremony gains a post-push check —
  verify the pushed commit message against the message file that
  produced it, every push. A mismatch is a fix-forward note in the
  session record, never an amend. `AUTHORS-GUIDE.md` §1 has the full
  rationale.

## 2. Fix-forward with disclosure (ADR-0001, R10)

**When a step's stated premise doesn't hold against the live tree, the
session stops, records the finding, and asks — it does not silently
adapt or guess.** This is the single most load-bearing rule in how
this workspace's bootstrap session was actually run, and it has real
receipts from that session, not just the stated policy:

- **The JDK/Temurin premise.** The session prompt characterized the
  environment as "JDK 21 (Temurin)." The bootstrap session's own
  `java -version` probe showed Ubuntu's stock OpenJDK 21 build, not
  Temurin — Temurin was present only at version 17, unused. Rather
  than silently writing "Temurin 21" into `SETUP.md` to match the
  prompt's wording, the session stopped, surfaced the discrepancy, and
  asked the author how to characterize it. Resolution: recorded
  precisely as measured (`SETUP.md` §1's own "JDK, precisely"
  subsection is the result), with "Temurin" scoped to CI, matching how
  sim's own `SETUP.md` already treated the same distinction.
- **The gitleaks premise.** The prompt asked the session to "port
  sim's hook (WSL-commit enforcement, gitleaks)." Sim's actually-
  committed `pre-push` hook does not run gitleaks at all — the one
  gitleaks scan on record (sim's `notes/facts-register.md` F15) was a
  one-time secrets audit, not a hook behavior. The session flagged
  this as a deviation from the stated premise rather than silently
  either adding or omitting the gate, gave its planned interpretation
  (add a new gitleaks-on-push gate, extending sim's pattern rather
  than porting a behavior that didn't exist), and proceeded once that
  was visible.

Both are permanent entries in this workspace's own deviation record —
see `notes/ADRs.md` ADR-0001's own appendix, and the archived session
prompt under `notes/prompts/` once step 12 lands it.

## 3. Continuity lives in the repo

Same principle as sim, same reason: nothing in one session's own
working memory can keep a multi-session migration coherent — the repo
has to be that thing.

- `notes/ADRs.md` outranks inference about why the workspace is
  organized a certain way.
- `AGENTS.md` is read every session — contribution discipline,
  workspace vocabulary, the fat-component disclosure, what's landed
  and what's deliberately still out.
- Sim's own `notes/sim/ADRs.md` and `notes/sim/facts-register.md`
  (moved intact as provenance, `AUTHORS-GUIDE.md` §3) are the
  reasoning-of-record for decisions made *before* the merge; this
  workspace's own `notes/ADRs.md` doesn't restate them, it cites them,
  origin-qualified (`sim/ADR-0008`).

## 4. Named holes are recorded, not guessed at

Where a decision genuinely isn't made yet — the source/sink component
shape, the tools landing plan, published-artifact coordinates — it's
written down as a named hole with its own trigger condition
(`notes/ADRs.md` ADR-0001's own "named holes" section), not silently
resolved by whichever shape was locally convenient at build time. A
hole gets closed by a future, author-ruled session addressing it
directly, the same way sim's own capture-then-build rhythm kept
judgment concentrated where context was richest.

## 5. The failure modes this defends against

- **Silent premise drift** — a session quietly treating a stale or
  wrong assumption as true because correcting it felt like friction.
  Defense: §2 above, fix-forward with disclosure.
- **Confabulated specifics** — asserting a path, a version, or a fact
  that sounds right but wasn't checked. Defense: the facts-register
  discipline inherited from sim (`AUTHORS-GUIDE.md` §4) and this
  document's own citations, each checked against the live tree at
  time of writing rather than assumed.
- **Scope creep past a checkpoint** — a session doing more than the
  checkpoint it just passed authorized. Defense: the COMMIT/AUTHOR
  ACTION convention (§1), and the fact that a delegated git-permission
  grant is read as scoped to its own session, not a standing change.

## 6. Operationally encoded in skills (migration session 5, 2026-08-02)

This document stays the human-readable narrative; the mechanics above
are also encoded as repo-local skills a session loads on demand instead
of re-deriving them from a close prose reading each time —
`.agents/skills/build-session/` (§1, the checkpoint/COMMIT/AUTHOR-ACTION
ceremony), `.agents/skills/capture-session/` (turning a ratified
decision into `notes/ADRs.md` law), `.agents/skills/extraction-stage/`
(the characterize→extract→verify→records split-stage discipline),
`.agents/skills/errata-sweep/` (§2's fix-forward-with-disclosure pattern
applied to stale doc claims), and `.agents/skills/session-prompt/`
(charter R-B's design-channel preflight and the canonical prompt
anatomy). Skills cite this document and `notes/ADRs.md`; they do not
restate them — this document remains what outranks a skill's own prose
if the two ever read differently.

**Amendment, 2026-08-02 (migration session 5) — practice-vs-narrative
divergence, named not resolved.** `notes/ADRs.md` ADR-0007 states that
"every author-ruling list in a session prompt or ADR... marks each
ruling `[A]` or `[C]`" (the provenance-tag convention). Checking every
session prompt written since that convention's adoption
(`2026-08-01-migration-session-1.md` through this session's own prompt,
five prompts total) found **none** of their Author-rulings lists
actually carry an `[A]`/`[C]` tag on any ruling — the convention has
been silently unused since the day it was adopted, not merely applied
inconsistently. `.agents/skills/session-prompt/SKILL.md` documents the
convention as ADR-0007 states it (encoding the rule, not the five-prompt
lapse) rather than quietly dropping it, since dropping it silently would
defeat the audit-recoverability the tags exist for. Not resolved here —
named for the author to rule on: retroactively tag the five prompts,
rule the convention retired, or simply start applying it going forward.
Matching note: `notes/ADRs.md` ADR-0007's own third dated amendment.

**Resolved, 2026-08-02 (provenance-adoption rider session).** Ruled
`[A]`: adopted, applying going forward, not retroactively tagged — the
five prompts named above stay untagged. `.agents/skills/session-prompt/SKILL.md`
now states tagging as a required step of its Author-rulings anatomy
(mirrored to `.claude/skills/session-prompt/SKILL.md`), with escalation
semantics stated for the first time: a `[C]` ruling conflicting with the
live tree is a default to fix-forward and reconcile; an `[A]` ruling
conflicting with the live tree always escalates. Full ruling and its
precedent citation: `notes/ADRs.md` ADR-0007's own fourth dated
amendment.
