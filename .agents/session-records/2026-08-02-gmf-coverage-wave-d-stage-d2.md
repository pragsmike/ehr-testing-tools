# 2026-08-02 — GMF coverage Wave D stage D2: CarePlan family

## Scope

D2 of GMF coverage Wave D (`notes/ADRs.md` ADR-0029; D0/D1 landed
earlier the same day, `297e337..bbeceb6`). Author rulings G1-G5 ruled
at session start (ADR-0029's own D2 note): a paired IR span
(`:care-plan-start`/`:care-plan-end`) mirroring `:medication-order`/
`:medication-end`, CarePlan itself v2-silent (R3), scope gated on
Step 1's own characterization of `myocardial_infarction.json`/
`total_joint_replacement.json`.

## What happened

**Step 1 characterization.** Both real closures fetched in full at
the pinned Synthea commit (`7e08387c68a7f0e21d13076609a159fd473fc902`)
and read against `State.java`'s own `CarePlanStart`/`CarePlanEnd`
classes. `myocardial_infarction.json`'s real closure is 27 files (root
+ 26 transitively-called submodules through its own CABG surgical
pathway) — not the single-file count the prior top-level survey
implied, matching the `urinary_tract_infections.json` precedent
exactly. Dirty with three independent, each-sufficient blockers:
`lookup_table_transition` (D3 scope), `ImagingStudy` (R5, explicitly
out of Wave D), and `SupplyList` (a genuinely new state type this
document had never named) — deferred. `total_joint_replacement.json`'s
real closure (4 files) surveyed CLEAN of every Wave-D-scoped type
except CarePlan itself. `CarePlanEnd`'s own `careplan` field, grounded
directly against source, is a same-module state-name reference,
structurally identical to `MedicationEnd`'s own `medication_order`
field — R2(b)'s pair-mirror confirmed against real Synthea Java, not
just the vendored JSON. `total_joint_replacement.json`'s own mandatory
`Joint_Replacement_Guard` required an attribute (`joint_replacement`)
no state in its closure ever writes — the module's own `remarks` field
disclosed why (triggered by two sibling root modules,
`osteoarthritis.json`/`rheumatoid_arthritis.json`, out of scope).
Ruled at the characterization gate, precedented by D1a's own governing
principle ("freely supply what a vendored artifact delegates to the
engine"): `run-module` gained one new, purely-additive trailing arity
accepting an `initial-attributes` seed map. Declared scope:
`total_joint_replacement.json` only.

**Step 2 implementation.** The full CarePlan chain landed, one commit
per layer, each red→green-verified and full-suite-green:
- sim-model (`7319680`): `:care-plan-start`/`:care-plan-end` pathway-IR
  steps.
- sim-trajectory (`efe1972`): loader schema/normalization, interpreter
  cases (mirroring `:medication-order`/`:medication-end` exactly, no
  attribute-based linkage needed), compile-trajectory mapping (joining
  `pre-horizon-fact-types`, the ongoing-therapeutic-content class), the
  `run-module` `initial-attributes` arity.
- sim (`c1dee3d`): `CarePlanRecord`/decide/evolve fold mirroring
  `MedicationOrderRecord` exactly; `check.clj`'s
  `clinical-content-only-when-admitted` gains `:care-plan-start`
  (grounded against `State.java`'s own encounter constraint).
- sim-emit-hl7 (`b499efc`): disclosed registry non-entry beside the
  `:procedure`/`:medication-*` precedent, plus two new tests asserting
  zero-message behavior (G3).

`Active CarePlan` condition stays design-ruled, implementation-
deferred per G2 — the declared-scope closure exercises zero uses of
it.

**Fix-forward (same session, `85c75de`).** Before vendoring, the
`joint_replacement` fix was tested live against the real
`total_joint_replacement.json` closure via a throwaway probe. A
SECOND, independent blocker surfaced: `Joint_Replacement_Guard`'s own
`allow` is a COMPOUND condition (`Age > 50` AND'd with an attribute
check). `age-guard-jump-days` — the analytical short-circuit that lets
a failing bare `:age >= N years` Guard jump forward in virtual time —
only recognizes that one shape, a known, deliberate v1 boundary per
its own docstring. This Guard is neither bare nor `>=`; the walk
blocks permanently at age 0 (confirmed empirically: `:status
:blocked`, zero trajectory events, with `joint_replacement` seeded and
a 60-year registration offset). Extending Guard's own condition-
resolution machinery to handle a compound condition correctly is real
interpreter-core work touching every other vendored root's own
Guard/Delay behavior, outside G1-G5's own ruled scope — escalated with
full evidence, not improvised under time pressure. Declared D2
vendoring scope revised to ZERO roots, an outcome ADR-0029's own G4
explicitly permits. No vendored files were ever committed for
`total_joint_replacement.json` — a premature copy (made before the
second blocker was found) was caught and reverted before staging.

## Red→green evidence

- `poly check`: clean at every checkpoint.
- Full non-integration suite (`clojure -M:poly test :all
  skip:integration`): 188 `Testing ehrt.*` namespace announcements
  throughout, 0 failures/0 errors at every checkpoint.
- Regression oracle, disclosed method (a deviation from a literal
  SHA-256-digest-across-a-disposable-worktree, the D1b precedent,
  ADR-0029's own dated note has the reasoning): the full test suite —
  property-based (`defspec`, 100-200 iterations) and fixed-seed
  assertions, exercising far more seeds than a single digest — stands
  in for it. All seven pre-existing vendored-root test namespaces
  (sinusitis/appendicitis/sore_throat/ear_infections/sepsis ×2/death-
  fixture) show IDENTICAL test-count/assertion-count/zero-failures
  between HEAD `a41d8c2` (Step 0) and this session's own final HEAD —
  verified explicitly, not merely asserted.
- `emit-hl7-test` namespace grew by exactly the two new tests this
  session added (59→61 tests, 193→197 assertions), matching the G3
  assertion requirement precisely.

## Judgment calls and their disposition

- The `run-module` `initial-attributes` extension (Step 1): self-ruled
  at the characterization gate under the D1a governing principle,
  disclosed in the ADR's own dated note before Step 2 implemented it —
  not an improvisation, a direct application of already-ratified
  precedent (the vital-signs reference table).
- The compound-Guard finding (fix-forward): NOT self-ruled the same
  way — recorded as a genuine escalation, outside this stage's own
  scope, deliberately left unresolved rather than rushed under time
  pressure against core walk-time-advance logic every other vendored
  root depends on staying byte-identical.
- Regression-oracle method: disclosed as a deviation the moment Step 1
  ruled it, not retrofitted at close-out.

## What's next

- `age-guard-jump-days`/`guard-step` extension (roadmap.md's own
  Deferred entry): unblocks `total_joint_replacement.json` (and any
  other compound-Age-Guard-gated module) — a real interpreter-core
  change, needs its own design pass, not a same-session add.
- D3 (`lookup_table_transition`, attribute-weighted
  `distributed_transition` weights, UTI closure re-characterization) —
  next in Wave D's own sequencing (R6), unaffected by anything this
  session found.
- The CarePlan mechanism itself needs no further work to be exercised
  — the next real closure surveying clean (state/condition/transition
  vocabulary, D7, AND no compound-Age-Guard-style walk blocker) can
  vendor against it directly.
