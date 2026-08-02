# 2026-08-02 — GMF coverage Wave D stage D1b session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl$\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they land on the
same clone the git/build commands target (`feedback-dual-clone-edit-
hazard`). Both clones were at `origin/main`, `dce2086`, confirmed via
`git status`/`git log` at session start.

## Prompt, verbatim

> 2026-08-02 — ehr-testing: Wave D stage D1b — observation family implementation + sepsis
>
> Context
> Implementation half of Wave D stage D1 (ADR-0029; D1a characterized and halted per E1, landing `de5bf51..dce2086`). The design channel ruled 2026-08-02: the D1a PROPOSED section is ACCEPTED AS DRAFTED, with Q1–Q4 resolved — Q1: `:category` added now, optional, on observation entries; Q2+Q3: one curated vital-sign reference table (vital-sign type → LOINC code, units, reference range) answers both the VitalSign code question and `vital_sign` value sourcing, and supplies the OBX reference-range/abnormal-flag inputs; Q4: ruled on the engine-source evidence now, no second module — the first future `MultiObservation`/`DiagnosticReport`-bearing module vendored serves as confirmation, noted as such. Step 0 converts the PROPOSED section to RULED and records the governing principle:
> Never override what the vendored artifact specifies; freely supply what it delegates to the engine. Stroke's `default: 0` is specified content — it stays blocked. Sepsis's `vital_sign` values are delegated to an unported engine module (`LifecycleModule.java`) — supplying an in-project, provenance-cited replacement is the simulator's own established pattern (persona replacing Synthea's demographics engine), under full content discipline.
> Payoff: `sepsis.json`, whose closure D1a already surveyed clean (no D3 kinds, D7 empty) — no characterization gate stands before Step 3.
> Regression oracle: fixed-seed runs of all currently vendored roots (appendicitis, sinusitis, sore_throat, ear_infections closure, the Wave C death fixture) byte-identical before and after every commit — INCLUDING every emitted HL7 byte: the new OBX builder and registry entry must not alter one byte of any existing message. New bytes come only from new-module walks.
> Ceremony: R30-mode — commit and push at each checkpoint, unattended, with R30's safeguards (staged-scope `--stat` check per checkpoint, personal-info scan, message via the Write tool, session record before final push, hooks as backstop; tags and repo-level `gh` outside the grant). Work in the WSL ext4 clone; fast-forward to `origin/main` (at or past `dce2086`), record HEAD.
> Read first
>
> 1. `notes/ADRs.md` ADR-0029 — the D1a characterization note and the PROPOSED section (the spec this session implements verbatim; Step 0 marks it RULED).
> 2. `components/sim-trajectory/docs/gmf-interpreter.md` §11 (the D1a findings account: ObservationGroup embedding, the three value mechanisms, the dead `number_of_observations`) and the sepsis survey rows.
> 3. `components/sim-model/src/ehrt/sim_model/pathway.clj` — the `:observation` step shape being extended and reused as the child shape; the C4 optional-fields precedent.
> 4. `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` — the OBX/OBR builders, `oru-message`, the message-type registry, and the unsolicited-`:observation` comment the new entry sits beside.
> 5. `components/sim/src/ehrt/sim/engine.clj` — the `:result-available` per-analyte flattening precedent the pass-through reuses.
> 6. The D1a session record for the pinned fetch paths (the vendored `sepsis.json` content and its provenance header come from the same pin).
>
> Author rulings (ruled 2026-08-02, design channel)
>
> * F1 — The ADR's PROPOSED section is the implementation spec: `:diagnostic-report` step (optional report codes; children as a vector of the EXISTING observation shape, verbatim reuse); `:observation`/`ObservationRecord` gain optional `:value-code` (Concept) and optional `:category`; engine pass-through on the flattening precedent; one new OBX builder branching CWE-vs-numeric; one new message function; one registry entry. No silent additions beyond it — anything the spec missed is a deviation-record entry with evidence, or an escalation if it changes the schema.
> * F2 — The vital-sign reference table is CONTENT, not code: a resource in `components/sim-trajectory/resources/` with a provenance header naming the reference-range sources, hashed like all vendored content, its home noted as named-not-final in the docs. Sampling from it is deterministic; its rng draws join the documented order contract. Observation events carry value, units, reference range, and abnormal flag so the emitter stays dumb (formats-as-emitters).
> * F3 — Installed ≠ used, applied forward: the `VitalSign` STATE and the `Vital Sign` CONDITION have their design ruled (R2(c): dissolution into categorized observation events read from clinical state) but are NOT implemented this session — no vendored module exercises either, and this workspace does not build unexercised machinery. The table already carries what they will need; the docs record them as design-ruled, implementation-deferred-until-a- bearing-module. If sepsis's closure turns out to exercise either after all (contradicting D1a), that is a finding: implement per the ruled design and record the survey correction.
> * F4 — Q4's confirmation duty is recorded in the survey: the next `MultiObservation`/`DiagnosticReport`-bearing module vendored must note whether the embedding evidence held.
> * F5 — Vendored test (sepsis) proves, beyond load-clean and fixed-seed determinism: a walk reaching a `:diagnostic-report` emission with embedded children; at least one `:value-code` observation and one table-sourced `vital_sign` observation with units, range, and flag present; and the end-to-end engine run in the Wave C precedent's shape (compile → engine → invariant catalog), with the emitted ORU for the report checked structurally (OBR present, one OBX per child, CWE segment for the coded value). Mixer-RNG seed discipline throughout.
>
> Steps
>
> 0. Records. ADR-0029: PROPOSED → RULED (dated note: accepted as drafted, Q1–Q4 resolutions, F3's deferral); the specify-vs- delegate principle recorded in the ADR body (quoted above); roadmap D1 → implementation phase. Commit: `docs: D1 schema RULED (Q1-Q4); specify-vs-delegate principle`.
> 1. The reference table per F2 — content commit with provenance header, hash recorded, a short doc note on sources and the sampling contract. Commit: `feat(sim-trajectory): vital-sign reference table (D1 F2, content)`.
> 2. Implement per F1, one commit per layer, red→green, oracle green, invariants co-landed: (a) `feat(sim-model): :diagnostic-report step; :value-code/:category fields (D1)` (b) `feat(sim-trajectory): observation-family states -- loader, interpreter, compile (D1)` (MultiObservation/DiagnosticReport with embedded children; `value_code` and table-sourced `vital_sign` value mechanisms; rng order-contract doc updated) (c) `feat(sim): diagnostic-report pass-through (D1)` (d) `feat(sim-emit-hl7): ORU rendering for :diagnostic-report (D1)` (OBX builder CWE branch, message function, registry entry beside the disclosed-silence comments)
> 3. Vendor sepsis per F5: module file with provenance header, survey row finalized (F4's confirmation-duty note), NOTICE, vendored test. Commit: `feat(sim-trajectory): vendor sepsis closure (D1 payoff)`.
> 4. Close out. Full suite + `poly check` green; oracle byte-identical finally; docs fix-forward (deferred tables — MultiObservation/ DiagnosticReport to the v1 disposition table, VitalSign/Vital Sign condition marked per F3; payoff map — sepsis real); ADR execution + deviation records; roadmap D1 → Done with shas; session record; self-archive this prompt to `.agents/prompts/`. Final commit: `docs: D1 records (ADR, survey, roadmap; archives prompt)`.

## Deviations from the prompt's own literal instructions

- Q2+Q3's own "supplies the OBX reference-range/abnormal-flag inputs"
  ruling is a real addition beyond P6's own base sketch ("no reference-
  range/abnormal-flag" on the new OBX builder) — executed as the
  ruling's own explicit text requires, not as an unruled deviation; see
  the session record's own judgment-calls section for the concrete
  implementation choice (extending `observation-obx-segment` in place
  rather than a separate `report-obx-segment` builder).
- One small fix-forward commit (`917e9cf`, the reading-set budget
  self-catch) landed between Step 0 and Step 1, not named in the
  prompt's own step list — a real, live gate trip this session's own
  Step 0 edit caused, fixed forward per staging hygiene rather than
  folded into an unrelated checkpoint.
- Two pre-existing `gmf_test.clj` fixtures needed a fix-forward swap
  (`MultiObservation` → `ImagingStudy`, their own "still deferred"
  example) as a direct, expected consequence of Step 2b — landed inside
  Step 2b's own commit (the same file, same checkpoint), not a
  separate commit.

Full account: `.agents/session-records/2026-08-02-gmf-coverage-wave-d-
stage-d1b.md`.
