# Plans

What's landed and what's next, at milestone grain — distinct from
`notes/ADRs.md` (why a structural decision was made) and
`.agents/session-records/` (what one session did). Modeled on sim's own
`roadmap.md` (frozen provenance: `notes/sim/agents/plans/roadmap.md`): a
single rolling plan document, not a folder of one-off plan files.

**What goes in:** milestones, named holes and their trigger conditions
(the same convention `notes/ADRs.md` ADR-0001's own "named holes"
section already uses at the workspace level — a plan-level milestone and
an ADR-level named hole are the same kind of object at different
altitudes), and enough forward-looking structure that a session picking
this up cold knows what's next without re-reading every ADR.

**When:** updated as milestones land or the plan changes shape — not
necessarily every session.

**Format:** sim's own convention preferred per R24's default (a single
rolling `roadmap.md`, not tools' multiple-named-plans-plus-`archive/`
pattern) — adopted here for consistency, not because tools' pattern was
wrong; a workspace that later wants several concurrently-tracked plans
can still use tools' shape for that specific need without contradicting
this default.

`roadmap.md` landed 2026-08-01 (migration session 1, item 13), seeded
from the design channel's own chat-resident ledger (which it retires as
of that date) — see the file's own header for the update-in-same-commit
rule. Sim's and tools' own pre-merge plans stay frozen at
`notes/sim/agents/plans/` and `notes/tools/agents/plans/`.

## Plan list

Files in this directory:

  * roadmap.md — the rolling plan (milestone grain, updated same-commit as work)
  * 2026-08-01-agent-ux-charter.md
  * 2026-08-01-migration-report.md
