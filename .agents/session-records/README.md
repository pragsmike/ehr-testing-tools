# Session records

One dated markdown record per session, written as its last pre-push act
(charter R-A, `notes/ADRs.md` ADR-0023) — adapted from sim's own
convention (frozen provenance:
`notes/sim/agents/session-records/README.md`), which closed the same
gap: summaries that lived only in a chat, nowhere in the repo.

**The listing is [`INDEX.md`](INDEX.md)**, generated from this directory
by `make state-derived` and gated both ways by
`ehrt.docs-tooling.index-completeness-test`. This file states the
convention; it does not list files (ADR-0147).

## Filename convention

`YYYY-MM-DD-short-slug.md`, one file per session (a session spanning
several calendar days uses its start date). Two the same day append
`-2`, `-3`. The slug MUST match its paired
[`.agents/prompts/`](../prompts/README.md) archive.

## What a record contains

Four parts, in this order, unchanged from sim's convention:

1. **Scope.** What the session was asked to do and what it did.
2. **Red→green evidence highlights.** Headline counts and deltas against
   the prior baseline, plus anything a test caught worth naming. A
   docs-only session's proof is the suite staying green and untouched.
3. **Judgment calls and their ratification status.** Every call the
   author did not hand over verbatim, and whether it is ratified yet.
4. **Findings and HEAD landed.**

## Where this sits

It does not outrank `notes/adr/` (why a decision was made — the sole
session narrative, `rulings.md#R-session-narrative-hierarchy`) or
`.agents/plans/` (what is next). `.agents/handoffs/` is deliberately not
instantiated here. The 2026-07-29..2026-08-01 gap, and the annotations
that used to ride the old per-file rows, are verbatim and dated in
[`../plans/state-history-2026-08.md`](../plans/state-history-2026-08.md).
