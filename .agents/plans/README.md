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
  * 2026-08-05-alignment-audit-brief.md — working brief for the alignment & cleanup audit arc: cognitive-load reduction, evolution-seam readiness, publication readiness; seeded findings S1-S6
  * 2026-08-05-alignment-audit-findings.md — the audit's findings register (`notes/ADRs.md` ADR-0049): probe/evidence/finding/recommendation/disposition rows for areas A-F, seeded rows S1-S7 updated with fresh evidence; findings-only, no fixes taken beyond Step 0's two pre-ruled acts
  * roadmap-done-2026-07.md — attic: July's slice of the Done history (empty — no July-dated Done rows existed at rotation time), moved 2026-08-05 by scaffolding compaction B
  * roadmap-done-2026-08.md — attic: August's slice of the Done history, moved verbatim 2026-08-05 by scaffolding compaction B (`notes/ADRs.md` ADR-0046)
  * 2026-08-06-ux-arc-brief.md — working brief for the UX arc: two-voices-two-homes principle, error-names-the-artifact, invocation-docs-gated; seeded findings U1-U5; residuals R1 (pending tags) and R2 (compaction-pointer rotation) folded into the opening session
  * 2026-08-06-ux-audit-findings.md — the UX audit's findings register (`notes/ADRs.md` ADR-0058): probe/evidence/finding/recommendation/disposition rows for areas A-D (invocation surfaces, help voice, error surfaces, first contact), seeded rows U1-U5 updated with fresh evidence, plus a complete 100-string classified appendix of every `help.clj` user-facing string; findings-only, no fixes taken
  * 2026-08-06-help-rewrite-draft.md — APPROVED 2026-08-06 (ADR-0062): the complete replacement text for every rendered string in `cli-spec` (register rows B-1/B-2/B-3/B-4(a)), every relocated ADR/milestone token moved to an adjacent `;;` source comment, plus the §5 voice-gate spec; applied verbatim by the landing session
  * 2026-08-07-repo-review-findings.md — the `repo-review` skill's own first-run findings register (`notes/ADRs.md` ADR-0077): 44 rows across all 8 rubric dimensions, the first-assessment scoreboard (4 green, 3 yellow, 1 red — error honesty, on a demonstrated silent-success defect), dispositions PROPOSED only; findings-only, nothing moved
  * 2026-08-08-encounterend-design.md — the EncounterEnd fidelity design brief (`notes/ADRs.md` ADR-0081): upstream's five-arm `EncounterEnd.process` semantics vs. this project's unconditional `:encounter-end` compile, the two-defect diagnosis, proposed openness-tracking fix, and the R1-R3 rulings that gate the fix session
  * 2026-08-09-repo-review-findings.md — the `repo-review` skill's own second-run findings register (`notes/ADRs.md` ADR-0092): 76 rows across all 8 rubric dimensions, review 1's own prior arithmetic re-derived and confirmed, the two-column scoreboard (3 green, 5 yellow, 0 red — error honesty closes RED to GREEN, continuity integrity and operator experience each regress to yellow), the step-5 mitigation-plan draft; dispositions PROPOSED only; findings-only, nothing moved
  * 2026-08-12-review-3-user-surface-findings.md — review-3's own findings register (`notes/ADRs.md` ADR-0114): a seven-battery (B1-B7) live probe of the `ehrt` CLI surface (verb/flag consistency, error quality, help surface, filesystem conventions, cross-doc agreement, output-shape consistency, the narration test), 48 tallied dispositions plus an 11-row UX-audit carry-forward (9 of 10 open items resolved on fresh evidence); findings-only, no fixes taken
