# Prompts

The home for session prompts (charter R-A, `notes/ADRs.md` ADR-0023,
2026-08-01) — a session archives the prompt that drove it here, paired
by date-slug with its own
[`.agents/session-records/`](../session-records/README.md) entry.
`notes/prompts/` stays the historical archive for everything through
2026-08-01; see its own README for the forward pointer.

**The listing is [`INDEX.md`](INDEX.md)**, generated from this
directory by `make state-derived` and gated both ways by
`ehrt.docs-tooling.index-completeness-test`. This file states the
convention; it does not list files (ADR-0147).

## Filename convention

`YYYY-MM-DD-short-slug.md`, one file per session, the SAME slug as its
paired session record — `ehrt.docs-tooling.prompt-record-pairing-test`
fails the build in either direction if the two drift apart.

## What a prompt archive contains

The shape `notes/prompts/*.md` already used: a short repo/clone/HEAD
context paragraph, the original prompt verbatim, and a deviation record
naming anything the session did differently and why.

## When

Every non-trivial session, as its last pre-push act, archives its own
driving prompt here alongside its record — R-A's other half.
Both files are written by hand and `make state-derived` regenerates the
two indexes; `bin/close-scaffold` did this until it was deleted
2026-08-25.

Annotations that used to ride the old per-file rows here are verbatim,
dated, in [`../plans/state-history-2026-08.md`](../plans/state-history-2026-08.md).
