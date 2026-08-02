# 2026-08-02 — GMF coverage Wave D stage D1b: observation family implementation

## Scope

Implementation half of Wave D stage D1 (`notes/ADRs.md` ADR-0029; D1a
characterized and halted for a ruling per E1, landed `de5bf51..dce2086`
a prior session same day). The design channel ruled the D1a schema
PROPOSAL (P1–P6) ACCEPTED AS DRAFTED with Q1–Q4 resolved (Step 0, this
session): `:category` added now (Q1); one curated vital-sign reference
table answers both the `VitalSign`-code gap and the `vital_sign`-field
value gap and supplies the OBX reference-range/abnormal-flag inputs
(Q2+Q3); ruled on this session's own engine-source evidence, no second
closure fetched, confirmation duty carried forward (Q4). A governing
principle was recorded: never override what the vendored artifact
specifies, freely supply what it delegates to the engine (stroke's own
`default: 0` stays blocked; sepsis's own `vital_sign` values, delegated
to an unported `LifecycleModule.java`, may be supplied in-project under
full provenance discipline, the same pattern Persona already
establishes for Synthea's own demographics engine).

Payoff: `sepsis.json`, whose closure D1a already surveyed clean (zero
D3-scoped transition kinds, D7 clean) — no characterization gate stood
before Step 3.

## Red→green evidence highlights

- `poly check`: clean throughout, checked after every step.
- Full non-integration suite (`poly test :all skip:integration`): 188
  `Testing ehrt.*` namespace announcements at this session's own final
  HEAD, 0 failures/0 errors. Every checkpoint's own affected-tests run
  (`poly test`, changed-since-`stable-*` scope) was also green before
  that checkpoint's commit, one real red→green cycle along the way (see
  Findings, below).
- Byte-identical oracle, the strongest form this codebase has: a
  disposable `git worktree` at this session's own pre-Step-0 HEAD
  (`dce2086`) ran a fixed-seed engine emission (or, for the one real
  `CallSubmodule` closure with no full-engine emission path yet,
  `ear_infections`, a fixed-seed interpreter-trajectory digest) for all
  five pre-existing vendored roots (sinusitis/appendicitis/sore_throat/
  ear_infections-closure/death-fixture), SHA-256-digested; the identical
  script re-run against this session's own post-Step-3 HEAD (`870a1ab`)
  produced byte-identical digests on all five. New bytes came only from
  the new sepsis.json walks, exactly as the session prompt's own
  regression-oracle requirement demanded.
- `sepsis.json` end-to-end: interpreter-layer proof
  (`ehrt.sim-trajectory.vendored-sepsis-test`, 5 tests/49 assertions) —
  load-clean, a well-mixed seed reaching the `:diagnostic-report`
  emission with its embedded `value_code` child, the mandatory-path
  `Capillary_Refill`/`Pulse_Oximetry` pair (value_code and table-sourced
  `vital_sign`, units/range/flag present), both `MultiObservation` value
  mechanisms side by side (`Record_Blood_Pressure`/`_2`, range vs.
  vital_sign), and determinism. Full engine/check/emit round trip
  (`ehrt.sim-emit-hl7.vendored-sepsis-test`, 3 tests/12 assertions) — a
  real 500-patient, 100-year-horizon population (empirically sized
  against the fixed-registration-anchor interaction sepsis's own
  Age_Guard/Delay onset gate creates, the same measure-don't-guess
  discipline `death_fixture_test.clj`'s own docstring already
  establishes): 31 `:diagnostic-report` events, 14 value-code-carrying,
  97 ORU messages; the full invariant catalog holds; the emitted ORU
  for a real event checks out structurally (ORC present, OBR-4 the
  report codes, one OBX per child, CWE segment with the correct SNOMED
  CT coding-system abbreviation for the coded value).

## Judgment calls and their ratification status

- The D1a schema PROPOSAL's own pseudocode (P1/P2) mixed two distinct
  schema layers in its sketch — `ehrt.sim-trajectory.gmf`'s raw
  module-JSON `GmfState` schema (using its own `with-transitions`
  helper) and `ehrt.sim-model.pathway`'s compiled IR `Step` schema —
  without always distinguishing which layer a given field belonged to.
  Resolved by grounding directly against the real, fetched
  `sepsis.json` (Blood_Cultures/Record_Blood_Pressure/_2's own raw
  JSON, captured verbatim in this session's own gmf_test.clj fixture):
  embedded children carry NO `:type`/transitions of their own (a bare
  `ObservationChild` map, gmf.clj's own new schema), while the compiled
  IR's `ObservationEntry` (pathway.clj) is the amended `:observation`
  step shape, exactly as P1/P2's own prose (not its pseudocode) states.
  Not a deviation from the ruling — a concretization of an ambiguous
  sketch against the same evidence D1a itself was grounded in.
- The Q2+Q3 ruling's own "supplies the OBX reference-range/abnormal-flag
  inputs" text went beyond P6's own literal base sketch ("no reference-
  range/abnormal-flag" on the new OBX builder). Resolved by extending
  `observation-obx-segment` itself (reused directly, not duplicated
  into a separate `report-obx-segment` builder P6's own text
  suggested) to append OBX-7/OBX-8 ONLY when the observation actually
  carries them — additive, byte-identical to every pre-existing call
  when absent (confirmed by explicit field-count assertion), applying
  uniformly to both a standalone table-sourced `:observation` event AND
  a `:diagnostic-report` child, since the ruling's own text names no
  distinction between the two. Judged as executing the ruling faithfully,
  not as a live design decision needing separate ratification.
- The vital-sign reference table's own abnormal-flag consequence (the
  sampled value is drawn FROM `:reference-range` by construction, so
  the flag is always `:normal`) was not spelled out in the session
  prompt's own F2/ruling text. Named explicitly in the table's own
  header comment and this session's docs (section 12) as an honest,
  disclosed consequence of the documented simplification — not a
  silent limitation.

## Findings and HEAD landed

- Real, live gate trip (not a synthetic proof): Step 2b's own loader
  changes made two pre-existing `gmf_test.clj` fixtures
  (`deferred-state-type-json`, `calls-deferred-leaf-json`) stale —
  both used `MultiObservation` as their own "still deferred" negative-
  test example, now false. Caught live by the affected-tests run (4
  failures, all the same root cause), fixed forward to `ImagingStudy`
  (R5, genuinely still deferred), reverified green — the same "swapped
  again, for the same reason" pattern those fixtures' own docstrings
  already carried from an earlier CallSubmodule-era swap.
- Real, live gate trip: Step 0's own roadmap.md edit (the D1 Now-row)
  grew `:onboarding`'s real line count past its own reading-set budget
  (932 → 935) — the same self-catching gate two prior sessions already
  hit on this exact file, fixed forward in its own small commit before
  continuing, per staging hygiene (not folded into an unrelated
  checkpoint's commit).
- One test assertion of this session's own authoring was itself wrong,
  not the code: `diagnostic-report-with-no-report-level-codes-emits-a-
  blank-obr4` expected OBR-4 to render as a genuinely empty field for a
  `nil` report concept; the real (and correct) output is `"^^LN"` — a
  degenerate-but-legal CWE field, `cwe-field`'s own existing behavior
  for a `nil` concept, unchanged by this session. Test fixed, renamed
  to `...emits-a-degenerate-obr4`, its own docstring stating why.
- No open findings carried forward beyond what ADR-0029's own D1b
  execution note and Q4's confirmation duty already name (F4: the next
  `MultiObservation`/`DiagnosticReport`-bearing module vendored must
  note whether this design held against it).
- HEAD at session end: this session ran under R30 (the standing default
  per ADR-0007/ADR-0023) — every checkpoint committed and pushed by
  this session itself. Final push lands this session's own records
  commit.
