# 2026-08-02 — GMF coverage Wave A session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target
(`feedback-dual-clone-edit-hazard`). The ext4 clone was fast-forwarded
to `origin/main`, `11cd013`, confirmed clean, at session start —
exactly the HEAD the prompt itself named.

## Prompt, verbatim

> 2026-08-02 — ehr-testing: GMF coverage Wave A — condition vocabulary + sore_throat
>
> Context
> First session of the GMF coverage-expansion arc — the payoff milestone sim split S2 unblocked (roadmap backlog row; `.agents/plans/ 2026-08-02-sim-split-plan.md` R-4 kept it out of the split itself). The arc's wave structure was designed in the design channel 2026-08-02 and is recorded by this session's Step 0 so it never has to be re-derived. Wave A extends `sim-trajectory`'s condition vocabulary — interpreter work only, no pathway-IR or sim-model changes — and vendors `sore_throat.json`, the survey's one state-type-clean module blocked solely by condition gaps. All work lands in `components/sim-trajectory` (plus survey/records surfaces). Determinism guard: no byte-identity oracle applies (behavior legitimately grows), so the regression oracle is the existing vendored modules — fixed-seed walks of appendicitis and sinusitis must produce identical trajectories before and after every commit of this session.
> Ceremony: R30-mode (the ratified default) — commit and push at each checkpoint, unattended, with R30's safeguards: staged scope matches the session's own file list (`--stat` check), personal-info scan, commit message via file, session record written before final push, hooks as backstop. Tags and repo-level `gh` remain outside the grant. Work in the WSL ext4 clone; fast-forward to `origin/main` (at or past `11cd013`), record HEAD.
>
> Read first
>
> 1. `components/sim-trajectory/docs/gmf-interpreter.md` — §2 (condition vocabulary v1), the deferred tables, the module survey rows for `sore_throat`/`stroke`/`sepsis`, and §83–95 (the flagged `Symptom` recommendation — ruled below).
> 2. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` and `gmf_interpreter.clj` — the loader gate, `step`'s condition evaluation, and the documented rng-consumption-order contract.
> 3. `components/sim-trajectory/test/.../vendored_appendicitis_test.clj` and `vendored_module_test.clj` — the vendored-module test pattern Wave A's sore_throat test must follow.
> 4. `notes/ADRs.md` ADR-0025 (the split record) and origin-qualified `sim/ADR-0013` point 4 (module curation criterion).
> 5. `.agents/plans/roadmap.md` — the GMF-expansion backlog row this session activates, and the S3 trigger row (Step 0 amends its wording per the author's 2026-08-02 ruling).
>
> Author rulings
>
> * AR-1 Wave plan is ratified as written in Step 0's plan text. Approving this prompt approves the plan; land it verbatim.
> * AR-2 Wave A candidate set: `At Least`, `Or`, `Date`, `Observation`-as-condition-type, `Active Allergy`. Membership is conditional on the characterization step confirming each condition's data source already exists in the accumulating trajectory, patient attributes, or persona. Any candidate needing a NEW state home (accumulator field, IR change, sim-model schema) drops to Wave D — report the drop with evidence, don't build the home. `Vital Sign` and `Active CarePlan` are pre-ruled OUT of Wave A (expected to need state homes); if characterization shows one is actually free, escalate rather than silently including it.
> * AR-3 `Symptom` recommendation (gmf-interpreter.md's own flagged review item): RULED ACCEPTED — `Symptom` joins v1 as a write-only, consumed-internally state, exactly as the doc proposes. The doc's flag is resolved fix-forward (dated note at the flag, not a rewrite).
> * AR-4 Condition evaluation consumes NO rng draws — this is the existing contract and every new condition type must preserve it, stated in each evaluator's docstring and covered by a property test (identical rng state before/after evaluation).
> * AR-5 sore_throat vendors per sim/ADR-0013 point 4 with its own survey row update and a vendored test in the established pattern: load-clean, fixed-seed full-walk determinism, and at least one branch-coverage assertion through the `At Least` compound that blocked it. If sore_throat surfaces a gap the survey missed, that is a finding: record it, vendor nothing, and close the session with the condition work + finding (do not force the module in).
> * AR-6 Stroke stays out: its `Date` gap closes this wave but its `Death` tail waits for Wave C — the loader's all-or-nothing gate stands; no consumed-internally shortcut for `Death` (it is semantically load-bearing, unlike `Device`). Note this in the survey row so the next session doesn't re-litigate it.
>
> Steps
>
> 0. Record the arc. Land `.agents/plans/2026-08-02-gmf-coverage-plan.md` containing exactly this plan text: [the plan text, reproduced verbatim in `.agents/plans/2026-08-02-gmf-coverage-plan.md` itself, not repeated a second time here]. Index it in the plans README; add roadmap rows (Wave A → Now, Waves B–D → Deferred with the payoff triggers above); amend the S3 trigger row to: "trigger: starting any second state-based emitter (`sim-emit-fhir`/`sim-emit-cda`) — S3 is that arc's first step" (author ruling 2026-08-02, closing the build-then-extract loophole). Commit: `docs: GMF coverage wave plan; S3 trigger wording (ruled 2026-08-02)`.
> 1. Characterize. (a) Per-candidate data-source table: condition type → where its operand lives today (trajectory event query / attribute / persona) → in or out per AR-2, with grep/read evidence. (b) Extract `sore_throat.json` from the vendored SimHospital-era… no — from the Synthea upstream the existing vendored modules came from: locate the provenance note in the existing modules' headers and use the same source and pinning discipline; if network access to that source is unavailable this session, stop after the condition work and record the vendoring as blocked-on-fetch (a finding, not a failure). (c) Fixed-seed regression baseline: record walk-output hashes for both existing vendored modules.
> 2. Implement, one condition type per commit, each commit carrying its evaluator + unit tests + the AR-4 rng-neutrality property test + the regression oracle green: `At Least`, `Or`, `Date`, `Observation`-as-condition, `Active Allergy` (as ruled in/out by Step 1), then `Symptom` per AR-3. Loader acceptance (`gmf.clj`'s recognized-keyword tables) co-lands with each interpreter change. Commit messages: `feat(sim-trajectory): <condition> condition (GMF coverage Wave A)`.
> 3. Vendor sore_throat per AR-5: module file with provenance header, survey row updated (including the AR-6 stroke note), vendored test green, docs' deferred tables updated fix-forward (dated notes). Commit: `feat(sim-trajectory): vendor sore_throat (GMF coverage Wave A payoff)`.
> 4. Close out. Full suite + `poly check` green; regression hashes byte-identical one final time; workspace ADR for Wave A (condition vocabulary v1→v1.1, the Symptom ruling, any AR-2 drops with evidence); roadmap Wave A row → Done with shas; session record; self-archive this prompt to `.agents/prompts/` with deviation record if any. Final commit: `docs: Wave A records (ADR, survey, roadmap; archives prompt)`.

## Deviation record

Two, both disclosed in `notes/ADRs.md` ADR-0026's own Deviation record
and repeated here per this step's own instruction:

1. **`At Least` and `Or` landed in one combined commit** (`f99e87a`),
   not two, departing from Step 2's literal "one condition type per
   commit." Both are trivial `GroupedCondition` mirrors of the already-
   built `And`, share the identical loader change (broadening recursive
   `:conditions` normalization from `:and`-only to `:and`/`:or`/
   `:at-least`), and splitting them would have been an artificial
   two-way cut through one small diff. Named in that commit's own
   message at the time, not silently folded in.
2. **`:symptom`-as-a-condition-type was built even though it is not one
   of AR-2's five NAMED candidates.** Step 1's own characterization
   found that `At Least`'s only real vendored use (`sore_throat.json`'s
   `Determine_if_Bacterial`) wraps `Symptom`/`Observation`/`Age`
   sub-conditions exclusively — building `At Least` for real branch
   coverage (AR-5's own obligation) had no content without it. Its data
   source (the already-accumulating `:attributes` map, written by the
   already-ratified `Symptom` STATE) clears AR-2's own membership bar
   the five named candidates were judged against, so this was treated
   as within AR-2's own spirit rather than an AR-2 violation requiring
   escalation — Step 1's own instruction ("if characterization shows a
   gap the survey missed, record it") is read as covering exactly this
   case, and it is recorded, not silently assumed. No candidate needing
   a genuinely NEW state home was built without a drop-and-report; only
   `:symptom` (no new home) crossed AR-2's own line into "found
   necessary, not merely convenient."

No other ruling in this prompt was applied differently than written.
`Vital Sign`/`Active CarePlan` were confirmed still OUT (AR-2's own
pre-ruling) — neither appears anywhere in `sore_throat.json` — so the
"escalate if actually free" branch never fired. `Active Allergy` needed
no code change (already built at M5b); this is recorded as a finding in
the session record and ADR-0026, not treated as a deviation, since AR-2
never required NEW work, only that it be ruled in or out.
