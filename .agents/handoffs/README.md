# .agents/handoffs/ — session handoffs

Where the [`handoff`](../skills/handoff/SKILL.md) skill writes. Exactly
one live handoff sits in this directory, `handoff-YYYY-MM-DD.md`, the
most recent; [`archive/`](archive/) holds every handoff it superseded.
The skill writes both: it saves the new handoff here and moves the
previous one into `archive/` in the same step. A successor reads the
live one and never needs `archive/`.

Layout by author ruling, 2026-10-01 (one live, the rest archived), with
the skill restored the same day from its 2026-08-25 de-scaffold
deletion; the session that did both is
[`2026-10-01-handoff-relocation.md`](../session-records/2026-10-01-handoff-relocation.md).

The two July handoffs under `notes/tools/agents/handoffs/` (2026-07-25,
2026-07-26) are frozen pre-merge provenance of the tools repo, not this
directory's history; they stay there, read-only, and are not archived
here.
