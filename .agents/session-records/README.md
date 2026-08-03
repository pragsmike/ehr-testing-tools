# Session records

**One dated markdown record per session, started here** — adapted
directly from sim's own convention (frozen provenance:
`notes/sim/agents/session-records/README.md`), which closed the same
gap sim's own retiring design channel named: session summaries that used
to live only in a chat conversation, nowhere in the repo. From this
session forward, a session doing non-trivial work in this workspace
writes its own record here as its last act before the ceremony (commit
→ push, per `AUTHORS-GUIDE.md` §1 — the same ceremony sim used, this
workspace's own checkpoint/delegation model layered on top per
`docs/dev/way-of-working.md` §1).

## Filename convention

`YYYY-MM-DD-short-slug.md`, one file per session (a session spanning
multiple calendar days uses its start date). If two sessions land the
same day, append `-2`, `-3`, etc. to the slug.

## What a record contains

Four parts, in this order — unchanged from sim's own convention:

1. **Scope.** What the session was asked to do and what it actually
   did, in a sentence or two.
2. **Red→green evidence highlights.** Not every test — the headline
   count/delta (tests, assertions, coverage, against the prior baseline)
   and anything a test caught worth naming. A docs-only session's proof
   is the suite staying green and untouched, not a red→green cycle.
3. **Judgment calls and their ratification status.** Every place the
   session made a call the author didn't hand it verbatim, and whether
   the author has ratified it yet. Ratified elsewhere (an ADR) can be a
   pointer rather than a repeat.
4. **Findings and HEAD landed.** Anything discovered that wasn't the
   point of the session, plus the commit HEAD this session's ceremony
   lands on.

## Where this sits relative to everything else

Does not replace or outrank:

- `notes/ADRs.md` — the reasoning-of-record for *why* a structural
  decision was made.
- `.agents/plans/` — what's landed and what's next, at milestone grain.
  A session record is finer-grained and may sit underneath several
  plan entries, or none.
- `.agents/handoffs/` — not instantiated in this workspace
  (`notes/discipline-parity-audit.md` row M14): this workspace's own
  checkpoint model, author present throughout one session rather than
  sim's/tools' async multi-session handoff, structurally reduces the
  need for it. Add it the moment a session actually ends with open
  mid-flight work for a fresh session to pick up.

Empty at instantiation (2026-07-28, discipline-parity session, R25) —
this session's own record will be its first entry, written at close.

## Gap, 2026-07-29..2026-08-01 (recorded 2026-08-01, agent-ux capture session)

The ritual lapsed for eight sessions between `2026-07-29-wsl-clone-igamt-hygiene.md`
and this entry — nothing gated it, and it stopped the week velocity
spiked (`.agents/plans/2026-08-01-agent-ux-charter.md` §2.1, the
diagnosis this session's own charter-adoption work exists to close).
**No retroactive record is fabricated for that gap.** Each of those
eight sessions' own archived prompt under `notes/prompts/` carries a
deviation record documenting what actually happened and what judgment
calls it made; the design-channel conversation that drove them holds
whatever isn't in those deviation records. This entry exists so a
future reader doesn't mistake the gap for sessions that did nothing
notable, or search here first and stop looking.

## Records

Files in this directory:

  * 2026-07-28-discipline-parity.md
  * 2026-07-29-development-resumption.md
  * 2026-07-29-exp-d3-nist-validator.md
  * 2026-07-29-judge-engine-extraction.md
  * 2026-07-29-sim-sibling-errata-sweep.md
  * 2026-07-29-storefront-polish.md
  * 2026-07-29-wsl-clone-igamt-hygiene.md
  * 2026-08-01-agent-ux-capture.md
  * 2026-08-01-skill-adaptation.md
  * 2026-08-01-migration-session-1.md
  * 2026-08-02-migration-session-2.md
  * 2026-08-02-migration-session-3.md
  * 2026-08-02-migration-session-4.md
  * 2026-08-02-migration-session-5.md
  * 2026-08-02-migration-session-6.md
  * 2026-08-02-provenance-adoption-rider.md
  * 2026-08-02-sim-split-s1-s2.md
  * 2026-08-02-gmf-coverage-wave-a.md
  * 2026-08-02-gmf-coverage-wave-b.md
  * 2026-08-02-gmf-coverage-wave-c.md
  * 2026-08-02-sim-split-s3-wave-d-d0.md
  * 2026-08-02-gmf-coverage-wave-d-d1a.md
  * 2026-08-02-gmf-coverage-wave-d-stage-d1b.md
  * 2026-08-02-gmf-coverage-wave-d-stage-d2.md
  * 2026-08-02-gmf-coverage-wave-d-stage-d3.md
  * 2026-08-02-post-wave-d-cleanup.md
  * 2026-08-03-rulings-capture.md
  * 2026-08-03-procedure-duration-fix.md
  * 2026-08-03-engine-closure-context.md
  * 2026-08-03-gmf-census.md
