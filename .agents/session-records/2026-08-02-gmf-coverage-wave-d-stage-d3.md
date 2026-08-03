# 2026-08-02 — GMF coverage Wave D stage D3: closing stage, Wave D CLOSED

## Scope

D3 of GMF coverage Wave D (`notes/ADRs.md` ADR-0029; D0/D1/D2 landed
earlier the same wave). Author rulings H1-H8 ruled at session start
(ADR-0029's own D3 session-start note): `lookup_table_transition` (the
sixth GMF transition kind, H2), attribute-weighted `distributed_
transition` weights (H3), compound-Guard analytical resolution (H4),
and a full re-characterization of `urinary_tract_infections.json`'s own
closure (H5) plus a hash re-verification of `total_joint_replacement.
json`'s own D2 fetch. H7's own rider scopes `initial-attributes`
(D2's own addition) to walk-entry inputs, not a general cross-module
channel. H8 names this stage as Wave D's own close-out.

## What happened

**Step 1 characterization.** UTI's full twelve-file closure plus both
lookup-table CSVs fetched fresh (not trusted from Wave B's own prose)
at the pinned Synthea commit and hashed; TJR's own four-file D2 fetch
re-verified by hash (first hash-anchored record for it, since D2 never
vendored it). `LookupTableTransition`/`NamedDistribution` grounded
directly against `Transition.java`. Beyond H1-H4's own three named
mechanisms, Step 1 surfaced four cheap, mechanical loader/interpreter
findings the earlier state-type-only census could not have caught:
`gmf_version` 2's own uniform stochastic-timing encoding (pervasive
across UTI's closure), `SetAttribute`'s own `value_code` field, a
fourth observation value-sourcing mechanism (`:exact`), and seven new
vital-sign-table rows UTI's own BMP/CMP panel needs (LOINC-verified
against a live public FHIR terminology server, `r4.ontoserver.csiro.au`,
`notes/facts-register.md` F22).

**Step 2 implementation**, one commit per mechanism, each red→green and
full-suite-green:
- `ea85852` (H2): `lookup_table_transition` plus closure DATA-FILE
  members (R4) — `ehrt.sim-trajectory.gmf/load-closure` gains an
  optional `table-resolve-fn`; a zero-rng row lookup (age range +
  gender, the only two recognized attribute columns per H2's own
  specify-vs-delegate audit) then one weighted-pick draw, falling back
  to each entry's own JSON-declared `default_probability` on no match.
- `af89d0e` (H3): attribute-weighted `distributed_transition` — a
  NamedDistribution map (`{:attribute :default}`) resolved before the
  pick; proven against a hand-authored fixture, NOT `stroke.json`
  itself (H3's own instruction) — `stroke_risk` stays SPECIFIED,
  unsourceable content, the mechanism landing does not change that.
- `91c9bfd` (H4): `age-guard-jump-days` extended under a sound-jump-
  or-escalate rule — bare `:age` gains the strict `>` operator (jumping
  to `quantity+1` years, the day-vs-year integer-age-flooring boundary
  a strict inequality needs); a compound `:and` resolves when it
  contains exactly one Age sub-condition whose every other sibling is
  non-time-dependent and already holds. Unblocks TJR's own
  `Joint_Replacement_Guard`, permanently blocked at age 0 since D2's
  own fix-forward finding.

**Disclosed additions, each its own commit**, the same "characterization
surfaces a real, in-spirit-authorized finding" shape ADR-0027's own
Step 2e first established:
- `5d87388`: the four mechanical findings above (gmf_version 2 timing,
  `value_code`, `:exact`, vital-sign table growth). A disclosed,
  UNRELATED, pre-existing `Procedure`-duration bug was found along the
  way (`:duration` is a flat map but `resolve-time-advance` destructures
  nested `:range`/`:exact` keys from it, finding neither — every
  vendored Procedure's own duration silently never advances virtual
  time) and is NAMED, not fixed — repairing it would touch every
  vendored root's own regression behavior, outside this session's own
  ruled scope.
- `fdd0644`: a real interpreter BUG found live vendoring UTI —
  `first-matching-entry` (shared by `conditional_transition`/
  `complex_transition` dispatch) returned `nil`, crashing its own
  callers, when no entry's own condition held and none was
  condition-less. Real Synthea's own `ConditionalTransition.follow`/
  `ComplexTransition.follow` both fall back to the LAST entry
  unconditionally in that case — a real semantic this project's own
  port never implemented, since no previously-vendored module's own
  mandatory path ever exercised it. Isolated into its own commit ahead
  of the D3f loader findings since it changes shared dispatch behavior,
  not merely widens a schema.
- `4d9178b`: three more UTI-specific loader findings, each confirmed
  against Synthea source before building — a new `virtual`
  encounter-class value (NOT aliased onto `:ambulatory`, a distinct
  clinical modality); `complex_transition`'s own per-branch either/or
  (`Transition.java`'s own `ComplexTransitionOption` allows a direct
  `:transition` OR `:distributions`, never both required); a real
  upstream UTF-8 byte-order-mark in `uti_recurrence.csv` (verbatim from
  Synthea; `uti.csv` carries none), stripped before parsing.

**Step 3 vendoring**, both declared payoffs land:
- `8dcec56`: `urinary_tract_infections.json` — the SEVENTH vendored
  module, this project's SECOND closure (twelve files) and FIRST
  data-file closure members (two lookup-table CSVs).
  `vendored-uti-test` proves entry-path lookup dispatch both ways
  (Cystitis and the much rarer Pyelonephritis, via a long-horizon walk
  sweeping every age bucket), a `type_of_care_transition` path taken
  with cross-boundary `Encounter`/`EncounterEnd` citations inside a
  called path submodule (Wave B's own deferred check, closed here for
  the first time against real content), determinism, and a
  bounded-horizon property test over 200 seeds.
- `430edbb`: `total_joint_replacement.json` — the EIGHTH vendored
  module, THIRD closure (four files). `vendored-tjr-test` proves the
  walk provably advances past the compound age guard (H4's own
  fix-forward finding, closed), the full post-op CarePlan span
  (`:care-plan-start` paired with its own `:care-plan-end`, correct
  `:references` linkage), the `joint_replacement` `initial-attributes`
  seeding disclosed per H7's own rider, determinism, and a
  bounded-horizon property test over 200 seeds.

Both vendored tests are interpreter-layer proof only — the SAME
standing, already-disclosed full-pipeline gap `ear_infections.json`'s
own vendored test already carries (confirmed by direct search this
session: no full compile-trajectory/engine/emit round trip exists for
ANY closure-having module vendored to date), not newly introduced.

## Red→green evidence

- `poly check`: clean at every checkpoint.
- Full non-integration suite (`clojure -M:poly test :all
  skip:integration`): 192 `Testing ehrt.*` namespace announcements
  throughout, 0 failures/0 errors at every checkpoint.
- Regression oracle, the same disclosed method ADR-0029's own D2 note
  established (a full-suite namespace/assertion-count comparison
  standing in for a literal SHA-256-digest-across-a-disposable-
  worktree): every one of the eight pre-existing vendored-root test
  namespaces (sinusitis/appendicitis/sore_throat/ear_infections/
  sepsis/death-fixture, plus sim-emit-hl7's own emission-layer suites)
  shows IDENTICAL test-count/assertion-count/zero-failures between this
  session's own pre-Step-2 HEAD and its final HEAD, HL7 emission bytes
  included (`ehrt.sim-emit-hl7.vendored-sepsis-test`'s own determinism
  assertion, unchanged).
- The `first-matching-entry` fix (`fdd0644`) was proven, in isolation,
  to change nothing for any already-vendored root BEFORE the UTI-
  specific loader findings (`4d9178b`) were reapplied on top — a
  dedicated test run at that exact commit confirmed 0 failures/0 errors
  across every namespace except `vendored-uti-test`/`vendored-tjr-test`
  (which failed for the EXPECTED reason: their own schema fixes were not
  yet reapplied).

## Judgment calls and their disposition

- The four mechanical findings, the `first-matching-entry` bug fix, and
  the three UTI-specific loader findings were all self-ruled at the
  point they surfaced, under the SAME "extend v1 with a documented
  reason, or defer" standing option every prior wave's own disclosed
  deviations already used — none reopens H1-H8's own ruled design.
- The `first-matching-entry` fix was deliberately isolated into its OWN
  commit, separate from the three D3f loader findings it was found
  alongside, specifically because it changes CORE shared dispatch
  behavior (not a schema widening) — proven safe in isolation before
  building on top of it, the same discipline this project applies to
  any change touching every vendored root's own regression behavior.
- The pre-existing `Procedure`-duration bug (found investigating
  `gmf_version` 2's own timing translation) was deliberately NOT
  fixed — repairing `resolve-time-advance`'s own flat-vs-nested
  `:duration` mismatch would change EVERY vendored Procedure state's
  own timing behavior, a change this session's own ruled scope (H1-H8)
  never authorized. Named in the roadmap's own Deferred section
  instead.
- Both vendored tests stay interpreter-layer-only, matching
  `ear_infections.json`'s own established precedent exactly (confirmed
  by direct search, not assumed) — H6's own "a full engine/check run"
  instruction is satisfied within that same standing fence, not
  silently reinterpreted or escalated mid-session.

## What's next

- Wave D is CLOSED (D0-D3, this session's own close-out in
  `.agents/plans/2026-08-02-gmf-coverage-plan.md`). No further stage is
  scheduled under this wave's own name.
- Standing named items, unowned by any wave: `myocardial_infarction.json`
  (three independent blockers), `stroke.json` (the stroke-risk data
  source, R7 — H3's own mechanism is only half the trigger),
  `ImagingStudy` (R5, a CHF trigger), `Active CarePlan` (design-ruled,
  implementation-deferred, no exercising module yet), the disclosed
  `Procedure`-duration bug, and the standing compile-trajectory/engine/
  emit full-pipeline gap for closure-having modules.
- S4 (`sim-engine` split) trigger status: NOT fired by any Wave D work
  — `emit-state` remains the sole direct reader of `PatientState`.
