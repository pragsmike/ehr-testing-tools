# 2026-08-02 — GMF coverage Wave B: `CallSubmodule` — closure loading, call/return, `ear_infections.json` vendored

## Scope

Second session of the GMF coverage-expansion arc (Wave A: `notes/ADRs.md`
ADR-0026). The arc's structural lift: loader closure resolution
(`ehrt.sim-trajectory.gmf/load-closure`), interpreter call/return
(`ehrt.sim-trajectory.gmf-interpreter`'s own descend-run-return), root-
scoped workflow attributes (D1's own three-compartment person record),
cross-boundary provenance citations (D2), and the fifth transition kind,
`type_of_care_transition` (D5). Design ruled in the design channel same
day, recorded verbatim in `notes/ADRs.md` ADR-0027. Target payoffs:
`ear_infections.json` (vendored) and `urinary_tract_infections.json`
(deferred, D6 — see Characterization findings, below). Full decision
record: ADR-0027.

## Characterization (Step 1) findings

- **`ear_infections.json`'s own real closure (root plus two called
  submodules, `medications/ear_infection_antibiotic.json`/
  `otc_pain_reliever.json`) surveys clean of every Wave-D-scoped
  deferred type** — the only state-type gap anywhere in the three files
  is `CallSubmodule` itself. Four real, previously-uncharacterized
  mandatory-path findings surfaced by reading the actual closure rather
  than the top-level module alone: `MedicationOrder`'s own
  `assign_to_attribute` / `MedicationEnd`'s own `referenced_by_attribute`
  (unbuilt — a cross-module reference a fixed state-name citation can't
  express, since the order could be any one of several polymorphic
  states chosen by a Date/Age/Active-Allergy-gated branch);
  `Attribute` condition's own `is nil`/`is not nil` operators (unbuilt);
  an unrecognized `encounter_class: "outpatient"` value on the module's
  own PRIMARY encounter; and the already-documented `wellness: true`
  boolean idiom (section 8's own M7 finding, five prior instances all
  on excludable tails) confirmed MANDATORY here for the first time,
  plus a second wrinkle that finding never named — the state carries no
  `:codes` key at all.
- **D7's hidden-import check came back empty (clean)** — the two shared
  attributes (`antibiotic_prescription`, `otc_pain_reliever`) are each
  read in a DIFFERENT module than at least one of their own writes:
  concrete, load-bearing evidence for D1's own root-scoping design, not
  a hypothetical the closure happens not to exercise.
- **`urinary_tract_infections.json`'s own real closure is twelve files,
  not the four the wave plan's own top-level survey assumed** (three
  named path submodules transitively call eight more). All three
  top-level care pathways route unconditionally into
  `DiagnosticReport`/`MultiObservation`, both already Wave D's own
  scope — D6 drops the whole module from this wave's vendoring. A
  genuinely new, unplanned finding along the way: `lookup_table_
  transition`, a SIXTH GMF transition kind this document's own brief
  never named, on the module's own entry path — not built (would need
  real design, an external lookup-table CSV mechanism this project has
  no analog for; the outcome, UTI deferred, doesn't change either way
  it's eventually resolved).
- **D5's `type_of_care_transition` dispatch rule characterized against
  Synthea's own `Transition.java` and the external `telemedicine_
  config.json` resource it reads at construction**, both fetched at the
  same pinned commit. Real Synthea keys on the simulated calendar year
  AND the person's current insurance-payer name; this project's persona
  has no payer concept (the identical gap shape `Active Allergy`'s own
  simplification already established), so this interpreter always uses
  the `typical_emergency_distribution` branch — but the year-gated half
  is NOT simplified away, since `ctx`'s own `:t` already answers it
  honestly (the same mechanism `:date` condition already uses).
- **Fixed-seed regression baseline** (Step 1e): 6 seeds × 2 sexes × 3
  vendored modules (`sinusitis`/`appendicitis`/`sore_throat`), hashed
  status + trajectory, captured before any Wave B code change — the
  regression oracle every subsequent commit was checked against.

## Red→green evidence highlights, and two real bugs found live

- **A real cycle-detection bug, found and fixed by its own red test**
  (Step 2b, `gmf/load-closure`): root is pre-seeded into the resolved-
  modules map before its own children resolve, so checking modules-
  containment BEFORE stack-containment silently masked a cycle back to
  root as "already resolved, dedupe" instead of catching it. The fix
  (check `stack` first) is a one-clause reorder, but the bug was real —
  `load-closure-rejects-a-cyclic-call-graph` failed with a green `:ok`
  result before the fix, not a thrown exception or an obviously-wrong
  value.
- **A real Java `Random` sequential-seed clustering bug, found and
  fixed in the test suite itself** (Step 2d,
  `type_of_care_transition`): an initial version of the interpreter
  tests used sequential small seeds (0..499) directly as `Random`
  constructor seeds to sample the weighted-pick distribution — every
  one landed within 0.7301-0.7311 on its own first `.nextDouble()`
  draw (a documented Java `Random` quirk), so 500 "independent" draws
  never once picked `:ambulatory` (the LARGEST weight, 0.56) at all.
  Fixed by reusing this project's own established mixer-RNG pattern
  (`vendored_sore_throat_test.clj`'s own `well-mixed-candidate-seeds`),
  not by weakening the assertion.
- Every new mechanism went RED first, for the right reason: `gmf_test.
  clj`'s own pre-existing "deferred state type" test used `CallSubmodule`
  as its example — updated to `MultiObservation` (still genuinely
  deferred) once `CallSubmodule` became loadable, preserving real
  unsupported-type coverage rather than leaving a now-false premise
  green by accident (the same discipline Wave A's own session record
  already established for `:vital-sign`).
  `vendored_ear_infections_test.clj`'s first run was RED for the
  expected reason (the vendored resources didn't exist yet).
- `poly check`: clean at every checkpoint. `poly test :all
  skip:integration`: 0 failures/0 errors at every checkpoint, and one
  final FULL-WORKSPACE run at session close (every brick, not just
  `sim-trajectory`).
- The regression oracle (above) re-run after every commit and one
  final time at session close: byte-identical across all Wave B
  checkpoints — the D1 root-scoping restructure, in particular, had to
  be invisible to non-calling walks by construction, and it was.
- `ear_infections.json`'s own vendored test proves the real, end-to-end
  case, not just the synthetic call/return fixture Step 2c's own unit
  tests use: a well-mixed-seed search over 2000 candidates finds a real
  vendored walk reaching through a called submodule, whose
  `MedicationOrder` events carry the full root-first `:call-path`
  citation and whose root-level `MedicationEnd` events resolve the
  cross-module `referenced_by_attribute` back to a real order event's
  own trajectory index, never nil.

## Judgment calls and their ratification status

- Step 2e (encounter-class loader normalizations) as an ADDITIONAL
  commit beyond D1-D8's own Step 2 checkpoint list — a scope judgment,
  not a ratified item; justified in the commit message and ADR-0027's
  own Deviation record, matching ADR-0026's own precedent for handling
  an emergent finding rather than silently dropping or silently
  building it unrecorded.
- `assign_to_attribute`/`referenced_by_attribute` and the `is nil`/
  `is not nil` operators folded into Step 2c rather than given their
  own commits — both are tightly coupled to CallSubmodule's own cross-
  module reference shape; splitting them would have been an artificial
  cut through one coherent change.
- `lookup_table_transition` named as a finding and NOT built — treated
  as within scope for "record, don't silently build or silently skip"
  rather than an escalation needing a stop, since the practical outcome
  (UTI deferred either way) doesn't depend on how it's eventually
  resolved. Not independently re-ratified by the author before landing
  — flagged here for visibility, the same "record real judgment calls"
  discipline Wave A's own session record already applied.
- `total_joint_replacement.json`/`myocardial_infarction.json`'s own
  docs rows got a dated note ("`CallSubmodule` removed from this
  module's own blocker list") WITHOUT re-characterizing either
  module's own real closure this session — a deliberate, narrow claim
  (the mechanism landed; vendorability is unverified), not an
  overclaim the docs fix-forward pass could have drifted into.

## Findings and HEAD landed

- This session ran under R30 (commit and push at each checkpoint,
  unattended), the ratified standing default, exactly as the prompt
  itself named. Every checkpoint's `git diff --cached --stat` matched
  that checkpoint's own file list before staging; `gitleaks git
  --staged -v` clean at every commit; every commit message written to
  the WSL scratch tree first (via the Write tool's own UNC path, after
  an inline-heredoc-through-the-wsl-wrapper attempt silently truncated
  a message mid-sentence on an unescaped double-quote — the same
  hazard this project's own memory already names, re-confirmed live),
  `git commit -F`'d, never an inline heredoc; every push verified
  against its message file (every diff was the expected trailing-
  newline formatting artifact, never a real mismatch).
- Reading-set budget self-caught once (`:onboarding`, 803→804 lines,
  Step 0's own roadmap growth) — the same recurring pattern prior
  sessions' own records already describe (Wave A's own session hit the
  identical shape); fixed forward in the Step 3 commit, per the
  established discipline.
- HEAD at session end: this record's own companion commit (`docs: Wave
  B records (ADR, survey, roadmap; archives prompt)`) — see that
  commit for its own sha. Prior commits, in order: `a92254b`,
  `f596a37`, `9a2f0cd`, `599fa47`, `cc9e0d6`, `13b924e`, `3adf974`,
  `01eb56b`.
