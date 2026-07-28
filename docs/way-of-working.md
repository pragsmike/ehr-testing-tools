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
- **Default: agent prepares, author commits.** `notes/ADRs.md`
  ADR-0001 (R6) states this as the durable rule. Within a single
  session, the author may explicitly delegate commit (and, separately
  and more cautiously, push) execution to the agent — a live, scoped
  grant, not a rewrite of the rule for the next session. `AUTHORS-GUIDE.md`
  §1 has the exact boundary.

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
