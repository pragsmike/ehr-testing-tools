<!-- Attic file: notes/adr/0039-wave-vs.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0039 — Wave VS: the vital-sign channel — register, `VitalSign` state, `:vital-sign` condition land; two of four blocked modules clear

**Status:** Accepted (author-ruled 2026-08-03, design channel, AR-1
through AR-7 below, recorded verbatim, attributed, per ADR-0007's own
provenance-tag convention — every ruling below is `[A]`); executed
2026-08-04.

### Context

Post-LC census (`2026-08-03-synthea-7e08387-wave-lc.edn`, ADR-0038):
73/85 walk; the vital-sign family was the largest remaining frontier —
four `:load-failed`/`:walk-failed` modules (`covid19`,
`congestive-heart-failure` on the `VitalSign` state; `contraceptives`
on the `:vital-sign` condition; `wellness-encounters` on an
observation-vocabulary name) plus `metabolic-syndrome-care` as a
LATENT consumer whose `Glucose`/`HDL`/`Triglycerides` conditions were
unreached. The author ruled this wave carries ONLY the baseline
content its consumers actually test — the broader calibration register
stays re-scoped Wave E, on demand — and that Wave H moves LAST, after
full parity, re-ordering the captured F0→F→G→H→I sequence
(`.agents/plans/2026-08-02-gmf-parity-plan.md` §4's own dated note,
below). Semantics pinned at
`7e08387c68a7f0e21d13076609a159fd473fc902`: `State.java`'s `VitalSign`
class (~1855–1920), `Logic.java`'s `VitalSign` class (~639–651).

### Decision

**AR-1 (the register).** A per-patient vital-sign register in the
clinical-state channel: `vital name → current value` (doubles).
Written by the `VitalSign` state; read by the `:vital-sign` condition.
Name vocabulary (keywordized via `gmf/slug`, mapped alongside the
observation family's existing string-keyed vocabulary in
`sim-trajectory/vital-signs.edn` — one transform, not a hand-maintained
second table): Left ventricular Ejection fraction, Oxygen Saturation,
Systolic Blood Pressure, BMI, Glucose, HDL, Triglycerides, Height — the
consumers' full set including the latent ones; nothing more.

**AR-2 (sample-once, one draw — disclosed divergence).** Upstream's
own `setVitalSign` stores a GENERATOR that re-samples on every read
(`Person.getVitalSign`); this project samples ONCE at `VitalSign`-state
execution (exact → the value, zero draws; range/distribution → one
draw via the existing samplers from Wave F0) and stores the VALUE.
Conditions read stored values — no draws at read time (a read-time
draw would put RNG consumption inside condition evaluation, breaking
the fixed-consumption law's shape). Disclosed: upstream's per-read
jitter is realism noise; stable values are this project's own
fitness-for-purpose choice. The `expression` (CQL) branch is NOT
supported: a `VitalSign` state carrying an expression is a clean load
rejection naming the feature (`:vital-sign-expression-unsupported`) —
no candidate module at the pin uses it in the consumers' closures
(verified: all six real `VitalSign` states found — five in
`congestive_heart_failure.json`'s own `LVEF *` states, one in
`covid19/infection.json`'s own `Poor Oxygen Saturation` — use `range`
only, confirmed by direct read, not merely anticipated).

**AR-3 (baselines — authored knobs, not ported curves).** Vitals
TESTED but never SET need baseline values. Verified against the
consumers directly (session characterization step): `BMI` (
`wellness_encounters.json`'s `Obese_Check`), `Systolic Blood Pressure`
(`congestive_heart_failure.json`'s `Admit_Discharge Transition`),
`Oxygen Saturation` (same module, `Oxygen_Saturation`/
`Oxygen_Saturation_Admission`), `Glucose`/`HDL`/`Triglycerides`
(`metabolic_syndrome_care.json`'s `Blood_Sugar_Check`/
`Metabolic_Check`/`Triglyceride_Check`) — exactly the minimum set
named, no more, no fewer. `Left ventricular Ejection fraction` is
DELIBERATELY ABSENT: `congestive_heart_failure.json`'s own five `LVEF
*` `VitalSign` states always precede the one `:vital-sign` condition
that reads it (`Maintaining CHF`) — every path into that state passes
through exactly one LVEF write first (traced against the closure's own
state graph) — confirmed, not assumed; a baseline here would silently
mask a real gap if that ever stopped being true. Upstream computes
these via `LifecycleModule.java`'s own growth/physiology curves
(age/sex-banded, continuously reassessed) — porting that is the
Framingham anti-pattern ADR-0031's own Tier-3 ruling already
forecloses. Instead: FLAT AUTHORED CONSTANTS in a register table
(`sim-trajectory/vital-sign-baselines.edn`), clinically-unremarkable
adult values, documented as CALIBRATION KNOBS with the register
discipline — the table is the provenance (authored, dated, ADR-cited),
making no clinical-fidelity claim. Config overridability is a named
future knob, not built. `initial-context` seeds the register from the
baseline table at patient creation with ZERO draws (constants) — the
register starts as the baseline map itself, so a `VitalSign` state's
later write simply overwrites its own name's entry (the same
"later write wins" semantics `SetAttribute` already establishes for
`:attributes`), and only a name the baseline table omits can ever be
genuinely unset.

**AR-4 (honest absence).** `:vital-sign` condition against a vital
with no stored value (i.e. a name the baseline table omits, unset by
any `VitalSign` state on the walk so far) → recorded walk error
(ADR-0036's own precedent, `honest-absence`, reused verbatim). The
observation family's vocabulary (`sim-trajectory/vital-signs.edn`)
gains the same five new names, following that table's own pre-existing
shape (LOINC-coded rows, string-keyed) — Height unblocks
`wellness-encounters`' own `Record_Height` Observation state.

**AR-5 (oracle bracket — pure identity).** A fresh recursive scan of
every currently-vendored root (`components/sim/resources/sim/modules`,
23 files, 9 root modules) confirmed — the F-era scan predates this
vocabulary and was re-run, not reused — found only one incidental hit:
`appendicitis.json`'s own free-text `:remarks` field ("the GMF does not
include Vital Signs..."), prose, not a real `VitalSign` state or
`:vital-sign` condition. Baseline initialization is draw-free (AR-3).
Every oracle batch byte-identical (Step 4, below); no change,
no escalation needed.

**AR-6 (named session reads).** Two pins the design channel did not
make, both read this session:
(a) `Person.getVitalSign`'s behavior on an unset vital — reads
`vitalSigns.get(vitalSign)`, and when the generator is absent (never
set) THROWS a `NullPointerException` ("Vital sign 'X' not set. Valid
vital signs: ...", `Person.java` ~line 551) — the upstream contrast to
this project's own honest-absence (AR-4): upstream crashes the whole
simulation run; this project records a walk error and continues past
it at the walk boundary, never propagating past `walk-module`/
`run-module`'s own catch.
(b) covid19's O2-sat `VitalSign` state (`Poor Oxygen Saturation`,
`covid19/infection.json` ~line 1237) encodes `"range": {"low": 75,
"high": 89}` — the LEGACY exact/range form, not a `gmf_version 2`
`distribution` — so this wave's own range-sampling path (AR-2) is what
it exercises, not the distribution path.

**AR-7 (census re-run + H re-ordering note).** Same params,
disambiguated filename (`2026-08-04-synthea-7e08387-wave-vs.edn`, the
overwrite bug still open, worked around by hand-renaming — unfixed,
named again). Movement, traced module-by-module against the wave-LC
artifact (Step 5, below): the four blocked modules resolve or unmask,
classified; `metabolic-syndrome-care` classified explicitly (unchanged
— its own VS paths stay unreached by the sampled seeds); vendored
roots unmoved (AR-5). Records step adds this dated re-ordering note to
the parity plan §4 and roadmap: **H runs LAST, after the schema-invalid
family and the Wave I tail, by author ruling 2026-08-03** — H's own
history-phase design should exercise against the complete walking
catalog.

### Verification baselines

`bin/regression-oracle b396c2c f04218d` (the tip before Step 1 → the
Step 3 tip) — all 9 vendored root batches IDENTICAL: `appendicitis`,
`death-fixture`, `ear-infections`, `ear-infections-engine`, `sepsis`,
`sinusitis`, `sore-throat`, `total-joint-replacement-engine`,
`urinary-tract-infections-engine`. AR-5's pure-identity claim holds,
byte-verified. `clojure -M:poly check` clean at every checkpoint.

### Execution record

**Step 1 (register + baselines + vocabulary, `60c8bb1`).** Ctx's own
new `:vital-signs` compartment, GLOBAL over the whole walk (never
root-scoped the way workflow `:attributes` is, ADR-0027 D1's own third
compartment) — threaded through every outcome-producing site
`gmf_interpreter.clj` already threads `:attributes` through (
`pass-through-outcome`, `blocked-outcome`, `call-submodule-step`,
`death-step`, `step`'s own `:terminal` case, `step-safely`'s walk-error
outcome, and the `run-submodule`/`walk-module`/`run-module` driving
loops). `initial-context` seeds it from the new
`vital-sign-baselines.edn` resource. `vital-signs.edn` gains five rows
(LVEF, BMI, HDL, Triglycerides, Height), LOINC codes copied verbatim
from the real candidate closures. 6 new tests
(`gmf-interpreter-test`).

**Step 2 (VitalSign state, `6141d6c`).** Loader: `"VitalSign"` joins
`gmf-type->keyword`; schema declares `:vital-sign`/`:unit`/`:range`/
`:exact`/`:distribution`, `:expression` deliberately undeclared
(`vital-sign-expression?` rejects it at load time before the schema is
ever checked); `:distribution` shares SetAttribute's existing five-kind
gate and normalization (`normalize-set-attribute-distribution` renamed
`normalize-value-distribution`, no `:unit` folding). Interpreter: the
new `:vital-sign` case in `step` samples once (AR-2) and stores into
the register, never emitting a trajectory event. VitalSign was the
last row in `docs/gmf-interpreter.md`'s own deferred-type table; its
test-fixture placeholder (a still-genuinely-unsupported example) moves
to `AllergyOnset`. 11 new tests across `gmf-test`/`gmf-interpreter-test`.

**Step 3 (`:vital-sign` condition, `f04218d`).** Loader: `"Vital
Sign"` joins `condition-type->keyword`. Interpreter:
`vital-sign-condition-holds?` reads the register by the same
slug-derived key the state writes, reusing `compare-op` unchanged (
already matches every operator the real candidates use — `<`, `<=`,
`>=`, `>` — the same dispatch `Utilities.compare`'s own `Double` branch
defines for them; `!=`/`is nil`/`is not nil` stay unbuilt,
installed ≠ used, no vendored candidate needs them). Honest absence
(AR-4) on a genuinely-unset name. `vital-sign-reference-table`/
`validate-vital-sign-name` relocate earlier in the namespace (both new
consumers need them before the file's own top-to-bottom order would
otherwise reach them). A second pre-existing test-fixture placeholder
(the generic "unsupported condition type" test) moves from
`:vital-sign` to `:true` (Logic.java's own trivial always-true
constant, genuinely unbuilt). 8 new tests.

**Step 4 (oracle bracket).** See Verification baselines, above.

**Step 5 (census re-run, `3e83390`).** `:ok-walked` 73→75,
`:load-failed` 8→7, `:walk-failed` 3→2, `:out-of-scope-by-ruling` 1→1
(unchanged), total 85→85. Traced module-by-module against the wave-LC
artifact:
- **`covid19`**: `:load-failed` (unrecognized state type `VitalSign`)
  → `:ok-walked`. Fully resolved.
- **`contraceptives`**: `:walk-failed` (unsupported condition type
  `:vital-sign`, one sampled seed) → `:ok-walked`. Fully resolved.
- **`congestive-heart-failure`**: `:load-failed` (unrecognized state
  type `VitalSign`) → STILL `:load-failed`, but on a wholly different,
  UNMASKED, pre-existing gap: `Inpatient LOS`, a `SetAttribute` state
  carrying both `"value": 0` and an `EXPONENTIAL` `:distribution`
  (`:set-attribute-value-conflict`, ADR-0035 AR-4's own load-time
  rejection rule, confirmed byte-grounded against the real module JSON)
  — the `VitalSign` blocker itself is gone; this is a different,
  unrelated finding, disclosed, not fixed (out of this wave's own
  fence).
- **`wellness-encounters`**: `:walk-failed` (unrecognized vital-sign
  name `Height`, three sampled seeds) → STILL `:walk-failed`, now on
  `Weight` (the very next Observation state on the same mandatory
  path, `Record_Weight`, immediately after `Record_Height`) — `Weight`
  is outside AR-1's own ratified 8-name scope ("nothing more");
  disclosed as the next incremental gap, not fixed here.
- **`metabolic-syndrome-care`**: `:ok-walked` → `:ok-walked`,
  unchanged — its own `Glucose`/`HDL`/`Triglycerides` `:vital-sign`
  conditions remain latent, unreached by the sampled seeds; the LATENT
  classification from the session's own Context holds.

`clojure -M:dev:test` per namespace, final run: `gmf-test` and
`gmf-interpreter-test` both 0 failures/0 errors (227 tests, 617
assertions combined with every pre-existing test in both namespaces).
`gitleaks git --staged -v`: clean, every commit.

### Fence

No schema-invalid family work (`congestive-heart-failure`'s own newly
unmasked `SetAttribute` value/distribution conflict, `injuries`/
hospice's own pre-existing complex-transition gap); no `Weight`
vocabulary addition (`wellness-encounters`' own next incremental gap,
named not fixed, AR-1's own scope discipline); no Wave E calibration
content beyond AR-3's minimal baseline set; no Wave H. Deviation from
the session prompt's own suggested "vital-sign family closed" framing:
recorded honestly in Step 5's own commit and here — two of four
originally-blocked modules resolve fully, one unmasks an unrelated
gap, one advances its own frontier by one state; the family is not
fully closed this session.

---

