# Prompts

The new home for session prompts (charter R-A, `notes/ADRs.md`
ADR-0023, 2026-08-01) — a session archives the prompt that drove it
here, paired by date-slug with its own
[`.agents/session-records/`](../session-records/README.md) entry. Not
a rewrite of the archival format: entries here follow the same shape
`notes/prompts/*.md` already used (a short repo/clone/HEAD context
paragraph, the original prompt verbatim, a deviation record) — only
the location changes.

**Why a new location, not more of `notes/prompts/`:** the agent-UX
charter's own diagnosis (§2) named signpost burial and a two-register
split as failure modes; consolidating session-driving archives under
`.agents/` alongside `.agents/session-records/`, `.agents/plans/`, and
`.agents/memory/` — the surfaces an agent picking up a session cold
actually needs to route through — closes one instance of that split.
`notes/prompts/` stays the historical archive for everything through
2026-08-01; see its own README for the forward pointer.

**When:** every non-trivial session, as its last pre-push act, archives
its own driving prompt here (paired with its session record) — the
same ritual `.agents/session-records/README.md` describes, R-A's other
half.

## Prompt list

Files in this directory:

  * 2026-08-01-agent-ux-capture.md
  * 2026-08-01-skill-adaptation.md
  * 2026-08-01-migration-session-1.md
  * 2026-08-02-migration-session-2.md
  * 2026-08-02-migration-session-3.md
  * 2026-08-02-migration-session-4.md
