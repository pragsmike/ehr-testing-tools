# 2026-08-03 — Rulings-capture session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target
(`feedback-dual-clone-edit-hazard`). The ext4 clone was already at
`origin/main`'s own HEAD (`0cff0d4`) at session start, confirmed by
`git fetch`; `/mnt/c` was confirmed read-only-guarded (post-Wave-D
cleanup's own J4 dual-clone guardrails, ADR-0030) and untouched this
session. Next ADR number confirmed 0031 before Step 1.

## Prompt, verbatim

> 2026-08-03 — Capture session: parity-plan rulings, wellness-semantics overturn, plan-status hygiene
>
> Context
>
> The design channel (2026-08-03) ruled the four open questions in `.agents/plans/2026-08-02-gmf-parity-plan.md` §6 and a live probe against Synthea source at the interpreter doc's own pin overturned a Wave-B-era survey claim about GMF wellness encounters — the third overturned survey row. Separately, the post-Wave-D cleanup session landed (`64e250f..0cff0d4`, ADR-0030): J1's byte-digest oracle verification returned IDENTICAL on both spans (clearing the parity plan's own gate), and J3 confirmed the closure engine round trip is broken in two distinct ways (see the three pinned tests under `components/sim-emit-hl7/test/`). This session CAPTURES all of that into the repo: an ADR, the parity plan's status flip and revisions, fix-forward status headers on two stale plan files, dated corrections to the wellness documentation, and roadmap rows. Docs-only: no code behavior changes anywhere. The single source-file touch (Step 4's `gmf.clj` edit) is docstring/comment text only; the full suite must stay green and no digest baseline moves.
>
> Read first
>
> 1. `AGENTS.md`
> 2. `.agents/plans/2026-08-02-gmf-parity-plan.md` (whole file)
> 3. `.agents/plans/README.md` and `.agents/plans/roadmap.md` (Now/Next/ Externals/Deferred sections)
> 4. `notes/ADRs.md` — ADR-0029's D2/D3 dated notes and ADR-0030 in full (the next ADR number is 0031)
> 5. `components/sim-trajectory/docs/gmf-interpreter.md` — §4 (the encounter-class mapping table) and the prioritization table's "Wellness-encounter `wellness: true` encoding" row
> 6. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` — the `encounter-class->keyword` map and its docstring, and the normalization clause matching `(and (= :encounter kw-type) (:wellness state) (not (:encounter-class state)))`
> 7. `.agents/session-records/2026-08-02-post-wave-d-cleanup.md`
> 8. Heads of `.agents/plans/2026-08-02-sim-split-plan.md` and `.agents/plans/2026-08-02-gmf-coverage-plan.md` (the stale status headers Step 3 fixes)
>
> Author rulings (record verbatim in ADR-0031; attributed, design channel 2026-08-03)
>
> * AR-1 (parity plan §6 Q1). The census tool is a `sim-trajectory` DEV ENTRY POINT, not a CLI verb. Promotable to a CLI verb later as a curation decision, once the census verdict vocabulary stabilizes — same walkable-vs-vendored logic the plan applies to modules. §3's "CLI verb (or `sim-trajectory` entry point)" hedge resolves to the latter.
> * AR-2 (parity plan §6 Q2). The synthesized wellness cycle is IN SCOPE for Wave G. Design shape ruled: (a) `wellness: true` becomes a genuine WAIT state — the interpreter parks the walk until the patient's next cycle visit, then attaches the module's downstream states to it — superseding the Wave B loader normalization that rewrites it to a created-on-the-spot `:outpatient-visit`; (b) the cycle's cadence is Synthea's own age-banded schedule (`EncounterModule.recommendedTimeBetweenWellnessVisits`, pin `7e08387c68a7f0e21d13076609a159fd473fc902`), ported as provenance-cited CONTENT under the vital-sign table's exact discipline (sha256, NOTICE, facts-register entry) — this is the register pattern, not the Framingham anti-pattern: its inputs (age, active-chronic-medications) exist in the sim with no input cascade; (c) before Wave H (pre-roll) exists, the cycle anchor is a seeded per-patient phase offset — an interim, disclosed answer superseded when H lands; (d) Wave G's remaining design questions (schedule-state home; multi-module attachment/churn composition — upstream, ALL waiting modules attach to the SAME visit; chronic-meds cap in v1 or a named register item) are the G design session's scope.
> * AR-3 (parity plan §6 Q3). Pre-roll REAFFIRMED as emit-nothing: the history phase folds state effects and mints no operational events, exactly as `docs/gmf-interpreter.md` §3 already ratifies. No backloaded-history mode in the sim. The backload need (pre-window messages for systems that ingest historical loads) is recorded as a NAMED TOOLS-SIDE FUTURE — a corpus construction over sim output, fault-injection's sibling — with revisit trigger: a real consumer for pre-window messages appears.
> * AR-4 (parity plan §6 Q4). Parity means WALKABLE, and walkable means WALK-VERIFIED: the census performs a seeded interpreter-layer smoke walk per module (N small) with digest recorded, not merely a load verdict — the three overturned survey rows were all semantic gaps loading alone would not catch. The verdict vocabulary keeps an `:out-of-scope-by-ruling` category even though AR-2 emptied its largest bucket. Boundary: census walk-verification is INTERPRETER-LAYER (a capability claim about the interpreter, and it surveys modules the engine will never see); engine round-trips remain per-vendored-root tests per ADR-0030 J3's established shape.
> * AR-5 (wellness overturn — the finding AR-2 rests on). The prioritization-table claim that the `wellness: true` encoding gap is "a loader normalization, not new interpreter machinery" is OVERTURNED by fetched Synthea source at the doc's own pin (`7e08387c68a7f0e21d13076609a159fd473fc902`): in `src/main/java/org/mitre/synthea/engine/State.java` (the `Encounter.process` method, wellness branch, ~lines 950–980), `wellness: true` creates NOTHING — it BLOCKS until the engine's hardcoded `EncounterModule` opens its next separately-scheduled wellness encounter, then attaches to it. Consequences, all recorded: (a) the five "would vendor immediately if fixed" modules (`epilepsy`, `med_rec`, `mTBI`, `atrial_fibrillation`, `osteoporosis`) are wellness-cycle modules and move into Wave G's unlock ledger — there is no cheap loader-fix win separate from G; (b) the vendored `ear_infections` root currently carries a live, previously-undisclosed TIMING SUBSTITUTION: its `Next_Wellness_Encounter` (upstream `wellness: true`, no `encounter_class` key) fires an immediate outpatient visit where upstream resolves the infection at the next SCHEDULED wellness visit, potentially months later on the cadence — legal under specify-vs-delegate (the artifact delegates timing; the sim supplied an answer) but documented as a vocabulary alias, which it is not; fix-forward disclosure, no behavior change this session, superseded by G-impl; (c) checked upstream at the same pin: NO vendored root uses a class-string `"wellness"` (`sinusitis`/`sore_throat` are `ambulatory`; `ear_infections`' other encounter is `outpatient`) — §4's mapping-table row conflates two upstream constructs sharing a word.
> * AR-6 (sequencing). Two DEFECT-FIX SESSIONS precede the census, in this order: (1) Procedure-duration fix — mechanical (`resolve-time-advance` destructures nested `:range`/`:exact` from a flat map; semantics pinned from Synthea source per H1 discipline before the fix commit), full oracle-bracketed re-baseline since virtual time shifts for every root; (2) engine closure-context fix — owns both J3 gaps (submodule-registry threading AND `initial-attributes` plumbing, including the design ruling on who supplies the seed at the engine layer), flips the three pinned round-trip tests, oracle-bracketed (the five non-closure roots must stay byte-identical; closure roots gain NEW engine-layer baselines). Duration-first is deliberate: it re-records the existing digest set once, and the engine fix then adds closure baselines on final timing semantics rather than recording them twice. Then census, then E/F/G with ordering left to the census ranking. G-impl follows the engine fix. The two fixes are NOT combined: one is mechanical, one has a design surface, and entangling them is churn.
> * AR-7 (approval act). The parity plan's own header names "census row to roadmap Now" as the approval trigger; AR-6 inserts two sessions before the census, so the approval act is AMENDED to: ADR-0031 landing + the plan's status flip (this session). The census row enters roadmap `Next` with its sequence position, not `Now`.
>
> Steps
>
> Step 0 — Preflight. Per the build-session skill's preflight rule: resolve both clone roots; every edit target resolves under the ext4 root. `git fetch` and confirm origin tip is `0cff0d4` or note and absorb anything newer. Confirm next ADR number is 0031.
>
> Step 1 — ADR-0031. Append to `notes/ADRs.md`: `## ADR-0031 — Parity-plan rulings (Q1–Q4), wellness-semantics overturn, defect-fix sequencing`. Status: Accepted (author-ruled 2026-08-03, design channel; recorded verbatim, attributed, per ADR-0007's provenance-tag convention). Body: AR-1 through AR-7 above, verbatim. Context paragraph names the evidence class: a fetched-source probe at the pin overturning a survey row (the third such), and ADR-0030 J3's confirmed-broken round trip as AR-6's motivation. Commit: `docs: ADR-0031 -- parity rulings Q1-Q4, wellness overturn, fix sequencing`
>
> Step 2 — Parity plan revision. `.agents/plans/2026-08-02-gmf-parity-plan.md`, annotate-not-rewrite except where a hedge is resolved:
>
> 1. Header: PROPOSED → APPROVED (2026-08-03, ADR-0031); note the gate paragraph's J1 condition is SATISFIED (IDENTICAL both spans, `56c7cef`); record AR-7's amended approval act.
> 2. §3: resolve the census hedge to the dev entry point (AR-1); add the smoke-walk and `:out-of-scope-by-ruling` requirements and the interpreter-layer boundary (AR-4).
> 3. §4 table: add the two defect-fix sessions as rows ABOVE Census (AR-6, one line each with their oracle-bracket requirement); mark the E/F/G sequence provisional pending census ranking; Wave G's row gains the five wellness modules in its unlock column and a pointer to AR-2's design shape including the pre-H anchor note; Wave H's row gains "ruled: emit-nothing reaffirmed (AR-3); backload = tools-side named future".
> 4. §6: each question annotated with its ruling and ADR-0031 pointer. Commit: `docs: parity plan APPROVED -- Q1-Q4 rulings folded, defect-fix sessions sequenced (ADR-0031)`
>
> Step 3 — Stale plan-status hygiene (fix-forward, annotate in place).
>
> 1. `.agents/plans/2026-08-02-sim-split-plan.md`: dated status paragraph at top — S1–S3 EXECUTED (cite the shas from roadmap Done entries), S4 deferred with its trigger, pointer to the roadmap row. The migration-report's own status-paragraph treatment is the pattern.
> 2. `.agents/plans/2026-08-02-gmf-coverage-plan.md`: one dated line at top — Waves A–D CLOSED 2026-08-02, superseded by the parity plan; the close-out detail already at the file's tail stays put.
> 3. `.agents/plans/README.md`: the parity-plan index line updated (approved 2026-08-03 per ADR-0031, gate cleared). Commit: `docs: fix-forward stale plan status headers (sim-split, gmf-coverage, plans index)`
>
> Step 4 — Wellness documentation corrections (dated, fix-forward; docstring/comment text ONLY — zero behavior change).
>
> 1. `components/sim-trajectory/docs/gmf-interpreter.md` §4: dated note on the mapping-table row distinguishing the two upstream constructs (module-created encounter with a class string vs. engine-scheduled wellness attachment), citing `State.java`'s `Encounter.process` wellness branch and `EncounterModule.recommendedTimeBetweenWellnessVisits` at pin `7e08387c68a7f0e21d13076609a159fd473fc902`, and AR-5(c)'s no-vendored-root-uses-the-class-string finding.
> 2. Same file, prioritization table's wellness-encoding row: dated OVERTURNED note per AR-5 — the five modules move to Wave G's ledger; the "cheapest fix" characterization is retired.
> 3. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj`: the normalization clause's comment/docstring gains the dated disclosure — this rewrite is a TIMING SUBSTITUTION (create-now where upstream waits for the scheduled cycle), live in the vendored `ear_infections` walk, disclosed per AR-5(b), superseded by Wave G's wait-semantics implementation (ADR-0031 AR-2). Do not touch the map, the clause, or any code form. Run the full suite and `bin/regression-oracle`'s smoke sanity if cheap — nothing may perturb; a digest change here is a STOP-AND-ESCALATE, not a fix. Commit: `docs(sim-trajectory): wellness semantics corrected from pinned Synthea source (ADR-0031 AR-5)`
>
> Step 5 — Roadmap. In `.agents/plans/roadmap.md`:
>
> 1. `Next`: three rows in AR-6 order — Procedure-duration fix session, engine closure-context fix session (each citing its Deferred row and the oracle-bracket requirement), census session (citing parity plan §3 + AR-1/AR-4). Update the existing Deferred rows for the two defects to point at their new Next rows (do not delete the Deferred provenance).
> 2. `Deferred`: the backload named future (AR-3) with its trigger.
> 3. The stroke-risk Deferred row: dated cross-reference — the data-source question is RULED by parity plan §2 (the risk-attribute register); the row's remaining substance is Wave E scheduling.
> 4. Run the reading-set budget test; bump budgets for any indexed file this session grew, per the established pattern. Commit: `docs: roadmap rows for defect-fix/census sequencing, backload named-future (ADR-0031)`
>
> Step 6 — Close out. Session record to `.agents/session-records/2026-08-03-rulings-capture.md` (include the verbatim rulings' provenance: design channel, 2026-08-03, with the Synthea probe evidence summarized); self-archive this prompt to `.agents/prompts/2026-08-03-rulings-capture.md`. Full suite + `poly check` + gitleaks per ceremony; push per R30. Commit: `docs: rulings-capture session records (archives prompt)`
>
> Fences
>
> * No code behavior changes. No test changes. No baseline/digest changes — any suite or oracle perturbation is an escalation.
> * Do not begin either defect-fix session's work here, however small it looks. Tests-only/docs-only boundaries have been load-bearing twice this week.
> * Deviations get a dated deviation-record appendix on the archived prompt, per convention.

## Deviation record

No deviations from the prompt's own steps or fences. Two clarifying
choices, both recorded as judgment calls (not deviations) in the
paired session record's own "Judgment calls" section rather than
repeated here: the overturned prioritization-table row's own dated
note placement (cell-embedded, this document's established
convention), and the §4 mapping-table note's narrow reading
(clarifying case (1)'s continued accuracy rather than retracting the
whole row). Step 4's fence ("any suite or oracle perturbation is an
escalation") was tested for real, not merely trusted: a literal
`bin/regression-oracle` run across the Step 4 commit and its parent
returned IDENTICAL on all six roots — see the session record's own
Red→green section for the digest citation.
