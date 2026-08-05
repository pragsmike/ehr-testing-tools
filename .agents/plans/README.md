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
  * 2026-08-02-sim-split-plan.md — staged extraction of components/sim into sim-model/sim-trajectory/sim-emit-hl7/sim-engine
  * 2026-08-02-gmf-coverage-plan.md — Wave A-D plan for GMF coverage expansion (condition vocabulary, CallSubmodule, Death, IR-homed state types)
  * 2026-08-02-gmf-parity-plan.md — APPROVED 2026-08-03 (ADR-0031): full Synthea module parity plan, J1 oracle gate cleared, §6 rulings folded
  * 2026-08-04-sim-split-b-plan.md — RULED 2026-08-04 (AR-1..AR-6): full decomposition of components/sim into provenance/sim-engine/sim-emit-fhir/sim-check/residual sim, sequenced M1-M4
  * roadmap-done-2026-07.md — attic: July's slice of the Done history (empty — no July-dated Done rows existed at rotation time), moved 2026-08-05 by scaffolding compaction B
  * roadmap-done-2026-08.md — attic: August's slice of the Done history, moved verbatim 2026-08-05 by scaffolding compaction B (`notes/ADRs.md` ADR-0046)
