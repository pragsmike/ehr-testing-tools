# 2026-08-02 — GMF coverage Wave A: condition vocabulary v1→v1.1, `sore_throat.json` vendored

## Scope

First session of the GMF coverage-expansion arc, sim split S2's own
payoff milestone (`notes/ADRs.md` ADR-0025). Landed the arc's wave plan
(`.agents/plans/2026-08-02-gmf-coverage-plan.md`), extended
`ehrt.sim-trajectory.gmf-interpreter`'s condition vocabulary with five
new predicates (`:or`, `:at-least`, `:date`, `:observation`-as-condition,
`:symptom`-as-condition), and vendored `sore_throat.json` — the module
survey's one state-type-clean candidate blocked solely by the
condition-vocabulary gap this session closed. Interpreter-only work, no
pathway-IR or `sim-model` changes, exactly as scoped. Full decision
record: `notes/ADRs.md` ADR-0026.

## Characterization (Step 1) findings

- **All five of AR-2's named candidates cleared its own membership
  bar** (data source already exists, no new state home): `At Least`/
  `Or` (compound wrappers, delegate to sub-conditions), `Date`
  (`ctx`'s own virtual clock `:t`, already threaded since M5a),
  `Observation`-as-condition (the accumulating `:trajectory`, already
  carrying `:observation` events since M5a). `Active Allergy` was
  ALREADY BUILT at M5b — a real finding, not a no-op: `gmf.clj`'s
  `condition-type->keyword` and `gmf-interpreter.clj`'s
  `evaluate-condition` both already carried it, and
  `sore_throat.json`'s own `Active Allergy` checks
  (`Pediatric_Allergy_Check`/`Adult_Allergy_Check`) use the identical
  RxNorm-7984-Penicillin-V shape `sinusitis.json`'s own
  `Penicillin_Allergy_Check` already does — confirmed by fetching and
  reading the real module JSON, not inferred.
- **`Vital Sign`/`Active CarePlan` confirmed still OUT** (AR-2's own
  pre-ruling) — grepped directly against the fetched `sore_throat.json`:
  zero occurrences of either condition type. The "escalate if actually
  free" branch never fired.
- **Emergent finding: `:symptom`-as-a-condition-type**, not one of
  AR-2's five named candidates. `sore_throat.json`'s only real use of
  `At Least` (`Determine_if_Bacterial`, a modified-Centor-criteria gate)
  wraps `Symptom`/`Observation`/`Age` sub-conditions exclusively —
  confirmed by fetching and reading the real module JSON (both of its
  two `At Least` entries, verbatim, at the pinned commit). Built anyway:
  its data source (the already-accumulating `:attributes` map, written
  by the already-ratified `Symptom` STATE, M5a) clears AR-2's own
  membership bar. Recorded as a finding, not silently folded in — see
  the prompt archive's own deviation record.
- **Semantics for all five grounded against Synthea's own source**, not
  invented: fetched `Logic.java`/`Person.java` at
  `docs/gmf-interpreter.md`'s pinned commit
  (`7e08387c68a7f0e21d13076609a159fd473fc902`) and confirmed each
  predicate's real upstream behavior — `Person.getSymptom`'s own
  default-to-0 (not a missing-key error), `Observation`'s own
  required-precondition throw (mirrored, not softened), `Date`'s own
  `currentyear = Utilities.getYear(time)` comparison, `And`/`Or`/
  `AtLeast`'s own `allMatch`/`anyMatch`/`count>=minimum` semantics.
- **Fixed-seed regression baseline** (Step 1c): 10 seeds × 2 sexes ×
  2 vendored modules (`sinusitis.json`, `appendicitis.json`), each
  walk's own status/`:t`/trajectory-count/trajectory-hash/attributes-
  hash recorded before any interpreter change — the regression oracle
  every subsequent commit was checked against.

## Red→green evidence highlights

- Every new condition type went RED first, for the right reason
  (`unsupported condition type` `ex-info`), confirmed via a targeted
  `clojure.test/run-tests` call before implementation, then GREEN after
  — `:symptom`, `:at-least`+`:or`, `:date`, `:observation`, and
  `sore_throat.json`'s own vendored test file (RED for "resource not
  found" before vendoring, per the established vendored-module test
  pattern).
- The pre-existing `evaluate-condition-throws-on-an-unrecognized-
  condition-type` test specifically asserted `:at-least` was
  unsupported — updated to assert `:vital-sign` instead (AR-2's own
  still-OUT candidate), preserving real unsupported-type coverage
  rather than leaving a now-false assertion green by accident.
- `poly check`: clean at every checkpoint. `poly test :all
  skip:integration`: 0 failures/0 errors at every checkpoint (seven
  commits, each independently verified, not just at session end).
- The regression oracle (above) re-run after every interpreter commit
  and one final time at session close: byte-identical trajectory-hash/
  attributes-hash across all 20 (seed × sex × module) combinations,
  every time — the determinism guard this session's own prompt named
  (no byte-identity oracle applies workspace-wide, since interpreter
  behavior legitimately grows; the vendored-module walks are the real
  oracle).
- The AR-4 rng-neutrality property (identical rng state before/after
  condition evaluation) is trivially true by construction —
  `evaluate-condition`'s own signature never accepted an `rng`
  parameter, for any condition type, before or after this session — but
  proven empirically anyway per AR-4's own instruction: a `defspec` per
  new condition type using a call-counting `proxy [Random]`, matching
  the pattern this test file's own pre-existing rng-consumption
  properties (`delay-consumes-a-fixed-single-rng-draw`,
  `complex-transition-always-consumes-exactly-one-draw`) already use.
- `sore_throat.json`'s own vendored test (AR-5's own obligation): a
  well-mixed-seed search (the same technique
  `vendored_appendicitis_test.clj` already established, since
  sequential/nearby `java.util.Random` seeds avalanche poorly) found
  real vendored walks crossing BOTH of `Determine_if_Bacterial`'s own
  `At Least` thresholds — `>=5` (a real downstream `MedicationOrder`
  event, Penicillin V or its allergy-alternate) and `>=3`-but-not-`>=5`
  (a real `Procedure` event, `Throat_Culture`, with no antibiotic
  ordered). An earlier draft of this search checked for
  `Determine_if_Bacterial` itself appearing in the trajectory and found
  ZERO hits across thousands of seeds — a false negative, not a real
  bug: `Determine_if_Bacterial` is a `Simple` state (consumed
  internally, no trajectory event of its own, `docs/gmf-
  interpreter.md`'s §1 table), so branch coverage has to be observed
  through each branch's own real DOWNSTREAM event instead. Caught by
  directly testing the underlying weighted-transition mechanism in
  isolation before concluding anything was broken.

## Judgment calls and their ratification status

- Combining `:at-least`+`:or` into one commit (deviation 1, prompt
  archive) — a scope judgment, not a ratified item; justified in the
  commit message and ADR-0026's own Deviation record at the time, not
  silently folded in.
- Building `:symptom`-as-condition (deviation 2, prompt archive) — the
  larger judgment call this session made. Treated as within AR-2's own
  spirit (same membership test, same evidence standard) rather than an
  AR-2 violation needing a stop-and-escalate, since Step 1's own
  instruction ("if characterization shows a gap the survey missed,
  record it") reads as covering exactly this shape of finding. Not
  independently re-ratified by the author before landing — flagged here
  for visibility, matching this repo's own "record real judgment calls,
  don't silently exercise them" discipline (`docs/dev/way-of-working.md`
  §2).
- `Observation`'s own throw-on-missing-observation behavior (mirroring
  Synthea's own `Logic.java` design rather than defaulting to `false`
  the way `Active Allergy` does) — a semantics judgment grounded in
  upstream source, not this project's own invention; recorded in the
  evaluator's own docstring and this record for visibility, not
  separately escalated, since it matches the established "throw for a
  module-authoring-shape bug, result-not-throw for a legitimate
  never-happened-yet case" split this interpreter already draws
  (unsupported condition types vs. `Active Allergy`'s always-false
  default).

## Findings and HEAD landed

- `Active Allergy` needed no code change (already M5b) — a finding, not
  a deviation, since AR-2 never required NEW work for a candidate, only
  that it be ruled in or out with evidence.
- `stroke.json`'s own `Date` gap is now resolved at the interpreter
  level, but the module stays deferred per AR-6: its `Death`
  state-type gap is load-bearing (unlike `Device`/`DeviceEnd`, never a
  safe consumed-internally pass-through), so the loader's all-or-
  nothing gate still rejects it. Recorded in `docs/gmf-interpreter.md`'s
  own survey row, per AR-6's own instruction, so a future session
  doesn't re-litigate why `stroke.json` isn't vendored despite its
  `Date` gap being gone.
- This session ran under R30 (commit and push at each checkpoint,
  unattended), the ratified standing default, exactly as the prompt
  itself named. Every checkpoint's `git diff --cached --stat` matched
  that checkpoint's own file list before staging; `gitleaks git
  --staged -v` clean at every commit; every commit message written to a
  scratchpad file first, `git commit -F`'d, never an inline heredoc;
  every push verified against its message file (`git log --format=%B
  -1` diffed against the source file — every diff was the expected
  trailing-newline formatting artifact, never a real mismatch).
- Reading-set budget self-caught once (`:onboarding`, 760→784 lines,
  Step 0's own roadmap growth) — the same recurring pattern prior
  sessions' own records already describe; fixed forward in the same
  commit, per the established discipline.
- HEAD at session end: `docs: Wave A records (ADR, survey, roadmap;
  archives prompt)`, the session's own final commit — see this record's
  own companion commit for its sha. Prior commits, in order: `0b2c1b2`,
  `9176250`, `f99e87a`, `5e3e72c`, `6a35492`, `6a3e11b`, `a2cf68d`.
