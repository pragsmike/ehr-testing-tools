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
