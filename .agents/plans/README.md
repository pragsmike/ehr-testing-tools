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

Empty at instantiation (2026-07-28, discipline-parity session, R25) —
this workspace's own roadmap doesn't exist yet; `notes/ADRs.md`'s own
"named holes" sections currently carry that function. The first session
that wants a rolling plan distinct from the ADR file's own holes should
create `roadmap.md` here rather than growing it inside an ADR. Sim's and
tools' own pre-merge plans stay frozen at `notes/sim/agents/plans/` and
`notes/tools/agents/plans/`.

## Plan list

Files in this directory:

  * 2026-08-01-agent-ux-charter.md
  * 2026-08-01-migration-report.md
