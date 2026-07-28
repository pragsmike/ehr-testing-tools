# Session records

**One dated markdown record per session, started here.** This closes
the one process gap the retiring design channel's own reflection pass
named: session summaries used to live only in that chat conversation —
nowhere in the repo. From this session forward, each session that does
non-trivial work writes its own record here as its **last act before
the ceremony** (per `AUTHORS-GUIDE.md`'s ceremony section: write record
→ commit → push). The design channel, while it existed, got a copy of
this same summary; the record in this directory is the original, not a
copy of something that lives somewhere else — see
[`docs/way-of-working.md`](../../docs/way-of-working.md) for the fuller
account of why that distinction matters now that the channel is gone.

## Filename convention

`YYYY-MM-DD-short-slug.md`, one file per session (a session spanning
multiple calendar days uses its start date). If two sessions land the
same day, append `-2`, `-3`, etc. to the slug.

## What a record contains

Each record is short — this is a durable index entry, not a transcript.
Four parts, in this order:

1. **Scope.** What the session was asked to do and what it actually
   did, in a sentence or two. Name the milestone or task if one exists
   (`.agents/plans/roadmap.md`'s own naming).
2. **Red→green evidence highlights.** Not every test — the headline
   count/delta (tests, assertions, coverage, against the prior
   session's own baseline) and any test that caught something real
   worth naming, the same way `.agents/plans/roadmap.md`'s own
   milestone write-ups already do. If no code changed, say so plainly
   (a docs-only session's proof is the suite staying green and
   untouched, not a red→green cycle).
3. **Judgment calls and their ratification status.** Every place the
   session made a call the author didn't hand it verbatim, and whether
   the author has ratified it yet (ADR-0013's own closing "Ratification
   record" is the model this follows). Ratified elsewhere (an ADR, a
   roadmap entry) can be a pointer rather than a repeat.
4. **Findings and HEAD landed.** Anything the session discovered that
   wasn't the point of the session (a real bug, a stale doc, a gap
   named for a future session) plus the commit HEAD this session's
   ceremony lands on.

## Where this sits relative to everything else

This directory is the durable home for **what happened in a session**.
It does not replace or outrank:

- `notes/ADRs.md` — the reasoning-of-record for *why* a structural
  decision was made. A session record may narrate that an ADR was
  written or ratified; it never substitutes for the ADR itself.
- `.agents/plans/roadmap.md` — what's landed and what's next, at
  milestone grain. A session record is finer-grained (one session, not
  one milestone) and may sit underneath several roadmap entries, or
  none, if the session was documentation-only.
- `.agents/handoffs/` — for **mid-flight, multi-session work that isn't
  done yet**. A handoff is written when a session ends with work still
  open, to brief whichever session picks it up next. A session record
  is written when a session ends, full stop, whether or not the work it
  did is part of something larger still in progress.

See [`docs/way-of-working.md`](../../docs/way-of-working.md) for the
full meta-process this convention is one piece of.
