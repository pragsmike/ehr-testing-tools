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

  * roadmap.md — the rolling plan (milestone grain, updated same-commit as work; rows follow the ADR-0144 contract: status token, `**[slug]**`, `PRIORITY n` under `## Next`, six lines a row, cited `roadmap.md#<slug>`)
  * 2026-08-01-agent-ux-charter.md
  * 2026-08-01-migration-report.md
  * 2026-08-02-sim-split-plan.md — staged extraction of components/sim into sim-model/sim-trajectory/sim-emit-hl7/sim-engine
  * 2026-08-02-gmf-coverage-plan.md — Wave A-D plan for GMF coverage expansion (condition vocabulary, CallSubmodule, Death, IR-homed state types)
  * 2026-08-02-gmf-parity-plan.md — APPROVED 2026-08-03 (ADR-0031): full Synthea module parity plan, J1 oracle gate cleared, §6 rulings folded
  * 2026-08-04-sim-split-b-plan.md — RULED 2026-08-04 (AR-1..AR-6): full decomposition of components/sim into provenance/sim-engine/sim-emit-fhir/sim-check/residual sim, sequenced M1-M4
  * 2026-08-05-alignment-audit-brief.md — working brief for the alignment & cleanup audit arc: cognitive-load reduction, evolution-seam readiness, publication readiness; seeded findings S1-S6
  * 2026-08-05-alignment-audit-findings.md — the audit's findings register (`notes/ADRs.md` ADR-0049): probe/evidence/finding/recommendation/disposition rows for areas A-F, seeded rows S1-S7 updated with fresh evidence; findings-only, no fixes taken beyond Step 0's two pre-ruled acts
  * roadmap-done-2026-07.md — attic: July's slice of the Done history (empty — no July-dated Done rows existed at rotation time), moved 2026-08-05 by scaffolding compaction B
  * roadmap-done-2026-08.md — attic: August's slice of the Done history, moved verbatim 2026-08-05 by scaffolding compaction B (`notes/ADRs.md` ADR-0046) and again 2026-08-17 by the row-contract migration (ADR-0144), which retired 29 closed rows here verbatim
  * 2026-08-06-ux-arc-brief.md — working brief for the UX arc: two-voices-two-homes principle, error-names-the-artifact, invocation-docs-gated; seeded findings U1-U5; residuals R1 (pending tags) and R2 (compaction-pointer rotation) folded into the opening session
  * 2026-08-06-ux-audit-findings.md — the UX audit's findings register (`notes/ADRs.md` ADR-0058): probe/evidence/finding/recommendation/disposition rows for areas A-D (invocation surfaces, help voice, error surfaces, first contact), seeded rows U1-U5 updated with fresh evidence, plus a complete 100-string classified appendix of every `help.clj` user-facing string; findings-only, no fixes taken
  * 2026-08-06-help-rewrite-draft.md — APPROVED 2026-08-06 (ADR-0062): the complete replacement text for every rendered string in `cli-spec` (register rows B-1/B-2/B-3/B-4(a)), every relocated ADR/milestone token moved to an adjacent `;;` source comment, plus the §5 voice-gate spec; applied verbatim by the landing session
  * 2026-08-07-repo-review-findings.md — the `repo-review` skill's own first-run findings register (`notes/ADRs.md` ADR-0077): 44 rows across all 8 rubric dimensions, the first-assessment scoreboard (4 green, 3 yellow, 1 red — error honesty, on a demonstrated silent-success defect), dispositions PROPOSED only; findings-only, nothing moved
  * 2026-08-08-encounterend-design.md — the EncounterEnd fidelity design brief (`notes/ADRs.md` ADR-0081): upstream's five-arm `EncounterEnd.process` semantics vs. this project's unconditional `:encounter-end` compile, the two-defect diagnosis, proposed openness-tracking fix, and the R1-R3 rulings that gate the fix session
  * 2026-08-09-repo-review-findings.md — the `repo-review` skill's own second-run findings register (`notes/ADRs.md` ADR-0092): 76 rows across all 8 rubric dimensions, review 1's own prior arithmetic re-derived and confirmed, the two-column scoreboard (3 green, 5 yellow, 0 red — error honesty closes RED to GREEN, continuity integrity and operator experience each regress to yellow), the step-5 mitigation-plan draft; dispositions PROPOSED only; findings-only, nothing moved
  * 2026-08-12-review-3-user-surface-findings.md — review-3's own findings register (`notes/ADRs.md` ADR-0114): a seven-battery (B1-B7) live probe of the `ehrt` CLI surface (verb/flag consistency, error quality, help surface, filesystem conventions, cross-doc agreement, output-shape consistency, the narration test), 48 tallied dispositions plus an 11-row UX-audit carry-forward (9 of 10 open items resolved on fresh evidence); findings-only, no fixes taken
  * 2026-08-13-manual-review-1.md — the `manual-review` skill's own first scored run (`notes/ADRs.md` ADR-0125): eight-dimension rubric scored against the finished manual, overall verdict FAIL (dimensions 1, strip executability, and 4, glossary linkage, both fail on repeat-pattern evidence across multiple chapters); findings-only, no fixes taken
  * 2026-08-14-manual-review-2.md — the second scored run, authored by the design channel (Fable) against a fresh public clone at `46b82ba` rather than by a session invoking the skill, a disclosed runner deviation the author chartered verbatim: all eight dimensions scored, both run-1 FAILs (1, strip executability; 4, glossary linkage) verified remediated, overall PASS with warns, plus four beyond-rubric findings (F1 erratum, F2 warn, F3 cosmetic, F4 affirmative record); nothing re-executed (channel sandbox limit, disclosed in the report's own preamble)
  * 2026-08-15-repo-review-findings.md — the `repo-review` skill's own third-run findings register, and the first run under the amended rubric (the population-closure law, landed this session's own Step 0): 40 rows across all 8 rubric dimensions, review 2's own prior arithmetic re-derived and confirmed exactly, the three-column scoreboard (2 green, 5 yellow, 1 red — sampling adequacy closes to GREEN as the aged census undercount is confirmed fixed; claim-reality and derivation drift each regress as the amended probes reach their real populations for the first time); three probes recorded as blocked or partial rather than skipped; dispositions PROPOSED only; findings-only, nothing moved
  * 2026-08-15-repo-review-3-plan.md — repo review 3's own step-5 mitigation plan, for the author's ruling: four proposed fix sessions (derivation registration, scan-root widening, ceremony-script correctness, trivial ride-alongs), three rulings needed (the orphaned palgebra drift check, two unregistered standing requests, and D5's own RED-vs-YELLOW score as an explicit judgement call), what is deliberately fine, and the three unrun probes named with their cost; proposes only, executes nothing
  * 2026-08-16-fence-battery-findings.md — register row D8-5 executed at last, chartered standalone by ruling Q2 a after lapsing across two consecutive reviews: the population enumerated from the TREE by the co-landed `bin/fence-census` (102 files, 202 fenced blocks, closed — 18 command/exercised, **58 command/bare**, 29 output, 97 other), `make quickstart` and `make integration` both green with `MAKE_EXIT` captured, and all 58 bare fences run one by one — **GREEN 42, RED 7, YELLOW 5, SKIPPED-WITH-REASON 4**. Headline: **zero RED on README, SETUP, or the manual**; all seven REDs are one root cause in one developer-facing file (`polylith-brief.md` teaches bare `poly`, not on PATH here), and the manual's two YELLOWs are both sequencing. Records its own near-miss: the manual's most emphatic reproducibility claim first read RED against an `out/` the exercisers had populated, and holds on a clean re-probe — the battery's own instance of rule 9. Dispositions RECOMMENDED only; nothing fixed but the two chartered riders
  * 2026-08-16-event-log-census.md — the event-log contract arc's own Step-1 evidence (author rulings *"Ok, add it, and make EDN be primary"* and *"Choose a."*, both 2026-08-16): the ground-truth event log's vocabulary and per-kind key population derived FROM THE TREE by the co-landed `bin/event-census` — the 21 `{:event ...}` construction sites in `engine.clj` reconciled against **4,997 events across eleven corpora**, the two populations agreeing exactly. Corrects two claims the driving prompt carried (`replay`'s `:before`/`:after`/`:world-*` are a derived trace record, not event keys; the universal key set is four, not five — `:active-mrn` is absent from `:bed-swap`, `:merge`, `:step-rejected`). Cross-checks all four built-in consumers' own reads: no read turned out dead, though `check.clj`'s `:disposition` read needed a purpose-built death corpus to clear. Names the **nested-`:event` collision** (four `:pre-horizon-facts` fact names collide with top-level log kinds), one reproduced consumer defect (the ADT-family Z-segment context is a seven-key subset while every other message family gets the whole event), and six shape defects (S-1..S-6). Describes only — every finding is a register row, nothing fixed
  * 2026-08-17-rulings-census.md — the ADR-0145 census, generated by `bin/rulings-migrate-0145 --markdown` and regenerated rather than edited: `.agents/rulings.md`'s 55 `## From ...` blocks with the ADR each one moves to, and all 181 bullets classified STANDING | ARC-LOCAL | SUPERSEDED against the register's own header test (*"ongoing rules a future session must still follow, not one-off execution choices"*), each with the sentence that decides it. 91 STANDING, 89 ARC-LOCAL, 6 SUPERSEDED
  * state-history-2026-08.md — attic: `.agents/state.md` as it stood at `0b15e87` (all 724 lines, 11 dated preamble blocks and 10 sections) plus both record READMEs' 291 per-file index rows with their hand annotations, moved VERBATIM by `bin/state-migrate-0147` at ADR-0147. The move is proved by that script's own `--verify` read-back, not by a diffstat. What is true NOW is `.agents/state.md` (hand-owned, capped) and `.agents/state-derived.md` (generated); this file is in no reading set and is not maintained
  * reading-sets-history.md — `.agents/reading-sets.edn`'s own header and per-set rationale, moved VERBATIM by ADR-0145: the seed note, the composition principle, the ratchet, and NINETEEN dated budget re-derivations 2026-08-05 to 2026-08-17. That file is now data with a 20-line header (gated); this is where its history went, and where a future re-derivation is appended. Undated name deliberately: it is a rolling attic, not a one-shot record
