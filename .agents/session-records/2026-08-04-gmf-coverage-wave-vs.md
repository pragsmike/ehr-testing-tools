# 2026-08-04 — GMF coverage Wave VS: the vital-sign channel

## Scope

Post-LC census (`2026-08-03-synthea-7e08387-wave-lc.edn`, ADR-0038):
73/85 walk; the vital-sign family was the largest remaining frontier —
four blocked modules (`covid19`/`congestive-heart-failure` on the
`VitalSign` state, `contraceptives` on the `:vital-sign` condition,
`wellness-encounters` on an observation-vocabulary name) plus
`metabolic-syndrome-care` as a latent consumer. The driving prompt
ratified seven author rulings (`notes/ADRs.md` ADR-0039, AR-1 through
AR-7): a per-patient vital-sign register (clinical-state channel,
GLOBAL not root-scoped), sample-once semantics for the new `VitalSign`
state (a disclosed divergence from upstream's own per-read
re-sampling), a minimal six-name authored baseline table (flat
constants, not ported physiology curves — the Framingham anti-pattern
ADR-0031 already forecloses), honest absence for a genuinely-unset
vital, an oracle bracket, a census re-run, and a re-ordering of the
Wave sequence (H now runs LAST, after the schema-invalid family and
the Wave I tail). Two named session reads (AR-6) were done directly
against the Synthea checkout at the pin before implementation began:
`Person.getVitalSign`'s own behavior on an unset vital (throws
`NullPointerException` — the upstream contrast to this project's own
honest-absence), and covid19's O2-sat `VitalSign` state's exact
encoding (legacy `range`, not a `gmf_version 2` distribution).
Executed in one pass, red→green per step, ending in the ADR, this
record, and the paired prompt archive.

## Red→green evidence

- **Step 1 (`60c8bb1`, register + baselines + vocabulary).**
  `gmf-interpreter-test`: 158/158, 421 assertions, 0 failures (up from
  152 pre-session) — 6 new tests (baseline seeding, the deliberately-
  absent LVEF name, register threading through an ordinary walk and a
  CallSubmodule round trip, vocabulary acceptance for all 5 new names).
- **Step 2 (`6141d6c`, `VitalSign` state).** `gmf-test` + `gmf-
  interpreter-test` combined: 221/221, 609 assertions, 0 failures — 11
  new tests (loader schema, v2-distribution normalization, unrecognized
  distribution kind, `:expression` rejection; interpreter exact/range/
  distribution sampling, one-draw consumption, register overwrite,
  unrecognized name, no-value-source throw). Two pre-existing tests
  needed fixing, not new content: `module-with-deferred-state-type-is-
  rejected`/`load-closure-all-or-nothing-gate-extends-to-a-transitively-
  called-submodule` both used `VitalSign` as their own "still genuinely
  deferred" placeholder fixture (a pattern already swapped four times
  before, per each fixture's own dated comment history) — moved to
  `AllergyOnset`, a real, still-unregistered Synthea state type.
- **Step 3 (`f04218d`, `:vital-sign` condition).** Combined: 227/227,
  617 assertions, 0 failures — 8 new tests (loader recognition; register
  read against baseline, both directions of the operator set, a value a
  `VitalSign` state just wrote, honest absence on the one baseline-less
  name, unrecognized name). Caught two of my own test bugs before
  green: two direct `interp/step` calls that never `assoc`'d `:current
  :check` onto the ctx, so the Guard state was never actually reached
  (the assertions passed vacuously against `:initial`'s own pass-
  through, not the condition logic under test) — fixed by adding the
  missing `:current` override, the SAME mistake `interp/step`'s own
  single-state-at-a-time contract makes easy to make once and hard to
  notice without running the suite. One pre-existing test
  (`evaluate-condition-throws-on-an-unrecognized-condition-type`) also
  used `:vital-sign` as its own placeholder — moved to `:true`
  (Logic.java's own trivial always-true constant, genuinely unbuilt).
- **Step 4 (oracle bracket, evidence only).** `bin/regression-oracle
  b396c2c f04218d` — all 9 vendored root batches IDENTICAL,
  byte-verified (table in the ADR's own Verification baselines). A
  fresh recursive scan of every vendored root confirmed zero real
  `VitalSign` state or `:vital-sign` condition usage anywhere — the
  one incidental hit (`appendicitis.json`'s own `:remarks` prose
  mentioning "Vital Signs") is free text, not module content.
- **Step 5 (`3e83390`, census).** `:ok-walked` 73→75, `:load-failed`
  8→7, `:walk-failed` 3→2, total 85→85 (unchanged).
- Throughout: `gitleaks git --staged -v` clean on every commit; each
  push verified against its own message file (every diff's only delta
  the `git log --format=%B` trailing-newline artifact); `clojure -M:poly
  check` green at every checkpoint.

## Judgment calls and their ratification status

- **The register key derivation (`(keyword (gmf/slug name))`) was read
  as satisfying AR-1's "keywordized, mapped alongside the observation
  family's existing names" literally** — one transform applied to the
  same raw string both mechanisms already share, rather than a
  separate hand-authored mapping table between two closed vocabularies.
  Not escalated; the ADR records the design choice explicitly (AR-1).
- **The baseline table is SEEDED INTO the register at patient creation
  (`initial-context`) rather than consulted as a separate fallback at
  every condition read** — both designs produce identical observable
  behavior for the six baseline-covered names and for the one
  deliberately-absent one (LVEF); the seed-at-init reading matches
  AR-3's own literal wording ("Initialize the register from the
  baseline table at patient creation") and is the simpler
  implementation (one map merge, one plain register lookup at read
  time, no two-tier fallback logic). Not escalated; disclosed in
  ADR-0039 AR-3.
- **`normalize-set-attribute-distribution` was renamed
  `normalize-value-distribution` and reused verbatim for `VitalSign`'s
  own `:distribution`**, rather than duplicating an identical function
  under a new name — the two state types share the exact same
  "no `:unit` folding" value-distribution shape; SetAttribute's own
  ADR-0035 AR-2/AR-4 citations in its docstring were widened to cover
  both callers. Not escalated; a same-shape reuse, not a new decision.
- **`Weight` was deliberately NOT added to `sim-trajectory/
  vital-signs.edn`** even though `wellness-encounters`' own census walk
  now fails on exactly that unrecognized name (the state immediately
  after `Height` on its mandatory path) — AR-1's own ruling named eight
  vitals and "nothing more"; adding a ninth name outside the ratified
  list, discovered mid-session via the census re-run rather than the
  Step 1 characterization pass, is scope creep the session's own fence
  forecloses. Disclosed as a named next-step in ADR-0039's own Fence
  and this roadmap's Next section, not fixed under this session's own
  authority.

## Findings and HEAD landed

- **A live finding: `congestive-heart-failure`'s own `VitalSign`
  blocker resolving unmasked a SECOND, wholly unrelated pre-existing
  gap** — `Inpatient LOS`, a `SetAttribute` state carrying both
  `"value": 0` and an `EXPONENTIAL` `:distribution` simultaneously
  (`:set-attribute-value-conflict`, ADR-0035 AR-4's own load-time
  rejection rule, confirmed byte-grounded against the real module
  JSON). Not anticipated by the driving prompt's own Context (which
  named `congestive-heart-failure` as blocked only "on the `VitalSign`
  state"); disclosed in ADR-0039's own Step 5 execution note and this
  roadmap's Next section, not fixed this session (out of fence).
- **A second live finding: `wellness-encounters`' own frontier moved,
  not closed** — `Height` resolving surfaces `Weight` as the immediate
  next blocker on the same mandatory path (`Record_Height` →
  `Record_Weight`, a `direct_transition` chain with no branch around
  it). Disclosed, not fixed (AR-1's own "nothing more" scope).
- This session ran under R30 (the standing default, ADR-0007/ADR-0023)
  — every checkpoint committed and pushed by this session itself.
- Commits, in order: `60c8bb1` (Step 1, register + baselines +
  vocabulary), `6141d6c` (Step 2, `VitalSign` state), `f04218d` (Step
  3, `:vital-sign` condition), `3e83390` (Step 5, census re-run —
  Step 4's oracle bracket needed no code change, folded into this
  record's own evidence instead), and this commit (Step 6 — ADR-0039,
  roadmap capture, `docs/gmf-interpreter.md` §1/§16, this record and
  its paired prompt archive, both indexed).
- **Fence, explicit:** this session did NOT touch the schema-invalid
  family, the `Weight` vocabulary gap, `congestive-heart-failure`'s own
  newly-unmasked `SetAttribute` conflict, Wave E calibration content
  beyond AR-3's minimal set, or Wave H — exactly the driving prompt's
  own Fences section. Deviation from the prompt's own suggested
  "vital-sign family closed" framing recorded honestly rather than
  silently adopted: the family is not fully closed this session.
