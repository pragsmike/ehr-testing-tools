# 2026-08-04 — GMF coverage Wave VS session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target. Preflight: the
`/mnt/c` Windows checkout was confirmed stale by design (read-only,
ADR-0030 J4) at `c0cdb3a`, well behind the ext4 clone of record; ext4
clean at `origin/main`'s own HEAD (`b396c2c`), no uncommitted changes;
ADR-0038 confirmed the latest ADR, next ADR 0039. A pre-existing
Synthea checkout was found at `/home/mg/synthea-checkout`, confirmed
via `git log -1` to already equal the pin
(`7e08387c68a7f0e21d13076609a159fd473fc902`) exactly — no fresh clone
needed.

## Prompt, verbatim

> 2026-08-03 — Build session: Wave VS — the vital-sign channel
> Context
> Post-LC census: 73/85 walk; the vital-sign family is the largest remaining (4 blocked: `covid19`, `congestive-heart-failure` on the `VitalSign` state; `contraceptives` on the `:vital-sign` condition; `wellness-encounters` on an observation-vocabulary name — plus `metabolic-syndrome-care` as a LATENT consumer whose Glucose/HDL/ Triglycerides conditions are currently unreached). The author ruled (2026-08-03): this wave carries ONLY the baseline content its consumers actually test — the broader calibration register remains re-scoped Wave E, separate and on demand — and Wave H moves LAST, after full parity (a re-ordering of the captured F0→F→G→H→I sequence: record with a dated note, per fix-forward). Semantics pinned at `7e08387c68a7f0e21d13076609a159fd473fc902`: `State.java` `VitalSign` (~1855–1920), `Logic.java` `VitalSign` (~639–651). Two things the design channel did NOT pre-verify are named session reads (AR-6).
> Read first
> 1. `AGENTS.md`; `.agents/skills/build-session/SKILL.md`
> 2. The sim's existing observation-family vital-sign name vocabulary — locate via the census walk error text ("unrecognized vital-sign name") — and the clinical-state channel (ADR-0027) where the vital register will live
> 3. Synthea at the pin: `State.java` `VitalSign.process` (legacy exact/range vs. distribution/expression branches), `Logic.java` `VitalSign.test`, `Person.getVitalSign` (absence behavior — AR-6), and the four consumers' actual states: `congestive_heart_failure.json` (LVEF states + LVEF/SBP/O2-sat conditions), `covid19/infection.json` (O2-sat states), `contraceptives` closure (its `:vital-sign` condition site), `wellness_encounters.json` (BMI condition + the Height observation), `metabolic_syndrome_care.json` (latent conditions)
> 4. `notes/ADRs.md` — ADR-0027 (clinical-state channel), ADR-0036 (honest-absence), ADR-0038; next ADR expected 0039
> 5. Post-LC census artifact header
> Author rulings (design channel, 2026-08-03; record in ADR-0039)
> * AR-1 (the register). A per-patient vital-sign register in the clinical-state channel: `vital name → current value` (doubles). Written by the `VitalSign` state; read by the `:vital-sign` condition. Name vocabulary (keywordized, mapped alongside the observation family's existing names): Left ventricular Ejection fraction, Oxygen Saturation, Systolic Blood Pressure, BMI, Glucose, HDL, Triglycerides, Height — the consumers' full set including the latent ones; nothing more.
> * AR-2 (sample-once, one draw — disclosed divergence). Upstream `setVitalSign` stores a GENERATOR that re-samples on every read; the sim samples ONCE at VitalSign-state execution (exact → the value, zero draws; range/distribution → one draw via the existing samplers from Wave F0) and stores the VALUE. Conditions read stored values — no draws at read time (a read-time draw would put RNG consumption inside condition evaluation, breaking the fixed-consumption law's shape). Disclose: upstream's per-read jitter is realism noise; stable values are the fitness-for-purpose choice. The `expression` (CQL) branch is NOT supported: a VitalSign state carrying an expression is a clean load rejection naming the feature (no catalog module at the pin uses it in the consumers' closures — verify during Step 1 and record).
> * AR-3 (baselines — authored knobs, not ported curves). Vitals TESTED but never SET need baseline values: at minimum BMI, Systolic Blood Pressure, Oxygen Saturation, Glucose, HDL, Triglycerides (verify the exact set against the consumers during Step 1; LVEF is module-set before tested — confirm, and if any module tests an unset, non-baseline vital, honest-absence fires, which is correct). Upstream computes these via LifecycleModule growth/physiology curves; porting that is the Framingham anti-pattern (ADR-0031's Tier-3 ruling). Instead: FLAT AUTHORED CONSTANTS in a register table — clinically-unremarkable adult values, documented as CALIBRATION KNOBS with the register discipline (the table is the provenance: authored, dated, ADR-cited; it makes no clinical- fidelity claim). Config overridability is a named future knob, not built. Initialize the register from the baseline table at patient creation with ZERO draws (constants), so no rng stream moves.
> * AR-4 (honest absence). `:vital-sign` condition against a vital with no stored value and no baseline → recorded walk error (ADR-0036's precedent). The observation family's vocabulary gains the same names (Height unblocks `wellness-encounters`), following whatever shape that vocabulary already uses — read it first.
> * AR-5 (oracle bracket — pure identity). No vendored root contains a VitalSign state, `:vital-sign` condition, or the Height observation name (verify with the recursive scan pattern before Step 1 — the F-era scan predates this vocabulary; re-run it, don't reuse its conclusion). Baseline initialization is draw-free (AR-3). Every oracle batch byte-identical; any change STOP-AND-ESCALATE.
> * AR-6 (named session reads). Two pins the design channel did not make: (a) `Person.getVitalSign`'s behavior on an unset vital (throw? null?) — read it and record it in the ADR as the upstream contrast to AR-4's honest absence; (b) covid19's O2-sat VitalSign states' exact encoding (legacy range vs. gmf-v2 distribution) — read before implementing, since it selects which sampler path AR-2 exercises.
> * AR-7 (census re-run + H re-ordering note). Same params, disambiguated filename (overwrite bug open — workaround). Expected movement: the four blocked modules resolve or unmask (classify); `metabolic-syndrome-care` classified explicitly (unchanged if its VS paths stay unreached; changed-with-explanation if they open); vendored roots unmoved (escalate). Records step adds the dated re-ordering note to the parity plan §4 and roadmap: H runs LAST, after the schema-invalid family and the Wave I tail, by author ruling 2026-08-03 — H's history-phase design should exercise against the complete walking catalog.
> Steps
> Step 0 — Preflight. Standard; ADR-0038 at origin; next ADR 0039; Synthea checkout at pin; AR-5's fresh recursive vendored-root scan recorded.
> Step 1 — Register + baselines + vocabulary. Clinical-state vital register; AR-3 baseline table (register-documented); name vocabulary in both stores (new register + observation family). Verify AR-3's tested-but-never-set list and AR-2's no-expression claim against the consumers' closures; record both in the ADR. Tests: register read/write, baseline initialization zero-draw, vocabulary lookups. Commit: `feat(sim): per-patient vital-sign register with authored baselines (ADR-0039 AR-1/AR-3)`
> Step 2 — VitalSign state. Loader (schema: exact/range/ distribution; expression → clean rejection) + interpreter (sample-once per AR-2, store) + AR-6(b)'s covid19 encoding read. Tests: each shape, one-draw consumption for sampled shapes, zero for exact. Commit: `feat(sim-trajectory): VitalSign state samples once into the register (ADR-0039 AR-2)`
> Step 3 — `:vital-sign` condition. Evaluate against register (stored value, else baseline, else honest-absence walk error); operator set per `Utilities.compare` semantics at the pin. Tests: stored, baseline-fallback, absence error, each operator. Commit: `feat(sim-trajectory): :vital-sign condition reads the register, honest absence (ADR-0039 AR-4)`
> Step 4 — Oracle bracket. Pure identity per AR-5; record; escalate on any change.
> Step 5 — Census re-run. Per AR-7; artifact + classification. Commit: `docs(sim-trajectory): census after Wave VS -- vital-sign family closed (ADR-0039)`
> Step 6 — Records. ADR-0039 (rulings verbatim + AR-6's two recorded reads; execution note: oracle table, classification). The H re-ordering dated notes (plan §4 + roadmap). Roadmap: VS → Done; schema-invalid family enters Next. Session record + prompt self-archive + budget check. Commit: `docs: wave VS records -- vital channel landed, H re-ordered last (archives prompt)`
> Fences
> * No LifecycleModule curve porting in any form — AR-3's flat constants are the ruling; growth curves, BP-by-age, BMI trajectories are all out.
> * No schema-invalid family work, no Wave H mechanics, no Wave E register content beyond AR-3's minimal baseline set.
> * Red→green per step required.
> * Deviations: dated deviation-record appendix on the archived prompt.

## Deviation record

- **Step 5's own suggested commit-message framing ("vital-sign family
  closed") does not match what the census re-run actually found** —
  two of the four originally-blocked modules resolve fully (`covid19`,
  `contraceptives`); `congestive-heart-failure` unmasks a wholly
  different, unrelated gap (a `SetAttribute` `:value`/`:distribution`
  conflict); `wellness-encounters` advances one state (`Height`
  resolves, `Weight` — the immediate next state on its mandatory path —
  does not). The actual commit message and ADR-0039's own title/Fence
  state this honestly rather than following the prompt's own suggested
  wording verbatim (`docs/dev/way-of-working.md`'s own fix-forward-with-
  disclosure discipline).
- **The register design resolved a genuine ambiguity in AR-3's own
  wording** ("no stored value and no baseline → recorded walk error"
  reads like a two-tier fallback checked at CONDITION-read time; "seed
  the register from the baseline table at patient creation" reads like
  baselines become the register's own initial content) by taking the
  literal seed-at-init reading — both produce identical observable
  behavior, and the seed-at-init reading is the simpler implementation.
  Not escalated; recorded in ADR-0039 AR-3's own text and this
  session's own record.
- **`normalize-set-attribute-distribution` was renamed
  `normalize-value-distribution` and reused for VitalSign's own
  `:distribution`** rather than left untouched with a parallel function
  added — the prompt's own AR-2 named a shared five-kind gate ("the
  same five-kind gate `:set-attribute` already has"), which this
  session read as licensing the reuse, not merely a similarity to
  document alongside a duplicate. Not escalated; a same-shape reuse.
- **`Weight` was found live (via the census re-run, not the Step 1
  characterization pass) but deliberately NOT added** to
  `sim-trajectory/vital-signs.edn` — AR-1's own ratified list is eight
  names, "nothing more"; a ninth name discovered mid-session is a named
  next-step (roadmap Next section), not silently folded into this
  session's own scope.
- **Two pre-existing tests used `VitalSign`/`:vital-sign` as their own
  "still genuinely deferred/unsupported" placeholder fixtures** (a
  pattern already swapped four times across prior waves, per each
  fixture's own dated comment history) — both had to move once this
  wave built the real thing (`AllergyOnset` for the state-type
  placeholder, `:true` for the condition-type placeholder). Not named
  in the prompt's own Steps list; treated as a direct, necessary
  consequence of Steps 2/3, folded into those same commits rather than
  given separate checkpoints (the same "checkpoints stay buildable,
  red→green per step" discipline the Wave LC prompt's own deviation
  record already established as precedent).
