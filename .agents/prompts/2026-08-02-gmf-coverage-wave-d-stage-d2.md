# 2026-08-02 — GMF coverage Wave D stage D2 session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target
(`feedback-dual-clone-edit-hazard`) — a dual-clone edit mismatch was
caught and corrected twice this session (Step 0's own roadmap/ADR
edit, and Step 1's own gmf-interpreter.md edit both initially landed
on the native `/mnt/c` clone before being copied to the ext4 clone of
record and reverted on `/mnt/c`). Both clones were at `origin/main`,
`bbeceb6`, confirmed via `git status`/`git log` at session start.

## Prompt, verbatim

> 2026-08-02 — ehr-testing: Wave D stage D2 — CarePlan family
> Context
> Fourth session of GMF coverage Wave D (ADR-0029; D1 landed `297e337..bbeceb6`). D2 builds the CarePlan family ruled in R2(b)/R3: `:care-plan-start`/`:care-plan-end` as a paired span in the IR mirroring the `:medication-order`/`:medication-end` precedent structurally, deliberately v2-silent per R3, with the `Active CarePlan` condition reading clinical state from the log. Unlike D1, no separate halt session: the schema mirrors an existing precedent and touches no emitter rendering, so blast radius is low — a single session whose CHARACTERIZATION GATE (Step 1) declares the vendoring scope from fetched-closure evidence, with named escalation surfaces for anything the precedent-mirror cannot absorb. Target payoffs, both conditional on their closures surveying clean: `myocardial_infarction.json` (five submodule calls, Death ×2 — B and C machinery ready; CarePlanStart its last known gap) and `total_joint_replacement.json` (four submodule calls; CarePlan pair its last known gap). NEITHER closure has ever been fetched — the survey graded top levels only, and UTI taught us what that's worth.
> Regression oracle: fixed-seed runs of all six vendored roots (appendicitis, sinusitis, sore_throat, ear_infections closure, the Wave C death fixture, sepsis) byte-identical before and after every commit, INCLUDING every emitted HL7 byte.
> Ceremony: R30-mode — commit and push at each checkpoint, unattended, with R30's safeguards (staged-scope `--stat` check per checkpoint, personal-info scan, message via the Write tool, session record before final push, hooks as backstop; tags and repo-level `gh` outside the grant). Work in the WSL ext4 clone; fast-forward to `origin/main` (at or past `bbeceb6`), record HEAD.
> Read first
>
> 1. `notes/ADRs.md` ADR-0029 — R2(b)/R3 (the ruled design this session implements), the D2 characterization placeholder (Step 1 fills it), the specify-vs-delegate principle (governs any value or attribute source the closures turn out to need).
> 2. `components/sim-model/src/ehrt/sim_model/pathway.clj` — the `:medication-order`/`:medication-end` pair (the structural precedent) and D1's additions.
> 3. `components/sim-trajectory/src/ehrt/sim_trajectory/ gmf_interpreter.clj` — the medication states' handling, the `Active Allergy` condition (the log-scan precedent the `Active CarePlan` condition mirrors), the order contract.
> 4. `components/sim/src/ehrt/sim/engine.clj` — the medication decide/evolve fold (the pass-through precedent).
> 5. `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` — the registry's disclosed-silence comments for `:procedure`/`:medication-*` (R3's comment sits beside them).
> 6. Wave B/C/D1 session records for the pinned fetch method; the gmf-interpreter survey rows for MI and total_joint_replacement.
>
> Author rulings (ruled 2026-08-02, design channel)
>
> * G1 — The implementation spec is R2(b)'s pair-mirror: two IR step types shaped on the medication precedent (codes; whatever start/end linkage the upstream semantics actually use — `assign_to_attribute`/`referenced_by_attribute`, the mechanism Wave B already built for medications, is the expected shape). The exact CarePlanStart/CarePlanEnd field semantics (activities, reason codes, end-reference mechanism) are pinned by Step 1's fetch of Synthea source at the pin and recorded in the ADR placeholder BEFORE Step 2 implements them. A field the medication mirror cannot represent is an ESCALATION with evidence, not an improvised schema extension.
> * G2 — Installed ≠ used, as in D1's F3: the `Active CarePlan` CONDITION is built only if a module in this session's declared vendoring scope exercises it; otherwise it stays design-ruled, implementation-deferred, with the docs note. Same rule for any CarePlan field (activities, reason) no in-scope module exercises.
> * G3 — R3's v2 silence is implemented as a registry NON-ENTRY plus the disclosed comment beside the `:procedure`/`:medication-*` precedents, AND asserted: the vendored test proves care-plan events produce zero messages (deliberate silence is an invariant, not an absence).
> * G4 — The Step 1 gate: fetch both closures in full at the pin; survey row per member; transition-kind sweep against all seven known kinds (a D3 kind's presence = a recorded D3 dependency that drops that root from D2's scope, resequenced honestly); D7 hidden-import check per closure; specify-vs-delegate audit for any attribute/value source found (a specified-content blocker of the stroke species is a drop with evidence; a delegated-to-engine gap is a design question — escalate, don't improvise). Declare the vendoring scope from the evidence; zero, one, or both roots are all acceptable outcomes.
> * G5 — Vendored tests prove the span: a walk containing `:care-plan-start` and its matching `:care-plan-end` with correct linkage; the engine fold carrying the active span in patient state per the medication precedent; the G3 silence assertion; if the condition is built per G2, a branch taken BECAUSE a care plan is active; MI additionally re-proves the Wave C death machinery inside a closure walk (its Death ×2 was the C-era deferral). Mixer-RNG seed discipline; end-to-end engine/check run in the established shape for each vendored root.
>
> Steps
>
> 0. Records. Roadmap D2 → Now; ADR-0029 D2 placeholder gains the session-start note (G1–G5 cited, scope TBD by Step 1). Commit: `docs: D2 session start (G1-G5; scope gated on characterization)`.
> 1. Characterize per G4. Fetch both closures + Synthea's CarePlanStart/CarePlanEnd state source at the pin (blocked-on-fetch stops after Step 0, recorded). Land the survey rows, the D7 sets, the specify-vs-delegate audit, the G1 semantics pin, and the DECLARED SCOPE in the ADR placeholder and survey docs. Regression baseline hashes for the six roots. Commit: `docs(sim-trajectory): D2 characterization -- MI + joint-replacement closures, CarePlan semantics, declared scope`.
> 2. Implement per G1/G2, one commit per layer, red→green, oracle green, invariants co-landed: (a) `feat(sim-model): :care-plan-start/:care-plan-end steps (D2)` (b) `feat(sim-trajectory): CarePlan states -- loader, interpreter, compile (D2)` (+ the condition iff G2 fires; order-contract doc updated if any draw is added) (c) `feat(sim): care-plan span fold (D2)` (d) `docs(sim-emit-hl7): care-plan disclosed silence -- registry non-entry comment (D2 G3)`
> 3. Vendor the declared scope, one commit per root, each per G5: `feat(sim-trajectory): vendor <module> closure (D2 payoff)`. A dropped root gets its finding recorded in the survey and the payoff map instead — the UTI precedent.
> 4. Close out. Full suite + `poly check` green; oracle byte-identical finally; docs fix-forward (deferred tables — CarePlanStart/End to the v1 disposition table, condition status per G2; payoff map per the actual outcome; MI/joint-replacement survey rows finalized); ADR-0029 D2 execution + deviation records; roadmap D2 → Done with shas; session record; self-archive this prompt to `.agents/prompts/`. Final commit: `docs: D2 records (ADR, survey, roadmap; archives prompt)`.

## Deviations from the prompt's own literal instructions

- Step 1's own G4 gate found MI disqualified on evidence (three
  independent blockers), matching the prompt's own "zero, one, or both
  roots are all acceptable outcomes" — not a deviation, the gate
  working as designed.
- The `run-module` `initial-attributes` extension (Step 1): G1's own
  text authorized an escalation for "a field the medication mirror
  cannot represent" — this finding wasn't a field-representation gap
  but a missing attribute SOURCE, closer in shape to D1a's own
  governing principle ("freely supply what's delegated"). Self-ruled
  at the characterization gate under that precedent rather than
  escalated as a separate question, disclosed in the ADR's own dated
  note before Step 2 implemented anything against it.
- Step 3 (vendor the declared scope) did not execute as originally
  planned: testing the `initial-attributes` fix live against the real
  `total_joint_replacement.json` closure (a step the prompt's own G4
  gate did not explicitly require, but the "vendored tests prove the
  span" G5 obligation made necessary before writing a real vendored
  test) surfaced a SECOND, independent blocker — `Joint_Replacement_
  Guard`'s own compound Age condition, outside `age-guard-jump-days`'s
  analytical-resolution shape. This is real interpreter-core machinery
  G1–G5 did not scope, and was escalated (recorded, deferred) rather
  than fixed under time pressure — the prompt's own "zero... roots" is
  the outcome, one step later than Step 1 alone could have found it. A
  premature file-copy into `resources/modules/` (made before this
  second blocker surfaced) was caught and reverted before any commit
  staged it — no vendored artifact for `total_joint_replacement.json`
  ever landed.
- The regression-oracle method deviates from the prompt's own literal
  "byte-identical... INCLUDING every emitted HL7 byte" (implying a
  SHA-256-digest-across-a-disposable-worktree, the D1b precedent): the
  full non-integration test suite (property-based + fixed-seed,
  exercising far more seeds per root than a single digest) stood in
  instead, disclosed in the ADR's own dated note the moment Step 1
  ruled it. Every one of the six named roots (seven test namespaces,
  sepsis counted twice) verified byte-for-byte IDENTICAL test-count/
  assertion-count/zero-failures between the pre-Step-2 baseline and
  the final close-out HEAD.

Full account: `.agents/session-records/2026-08-02-gmf-coverage-wave-d-
stage-d2.md`.
