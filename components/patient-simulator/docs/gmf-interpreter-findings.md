# GMF interpreter: dated findings trail

This is the dated findings trail for [`gmf-interpreter.md`](gmf-interpreter.md)
(the living reference: sections 1-8, the appendix, and the ratification
record). Each section below is a closure survey, characterization
finding, or census re-run from one dated GMF-coverage session, appended
in the order it landed, 2026-08-02 through 2026-08-04. Content here is
historical record, moved verbatim from `gmf-interpreter.md` section 9-16
(docs coherence pass, 2026-08-05, ADR-0043 AR-D-1) -- narrating what a
session found and ruled at the time, not the current state of the
interpreter. For what the interpreter actually does today, read
`gmf-interpreter.md` itself; its per-wave pointer index links back
into the section here that established each finding.

---

## 9. GMF coverage Wave B: closure survey and characterization findings (2026-08-02)

Step 1 of the Wave B session (ADR-0027; `.agents/plans/2026-08-02-gmf-
coverage-plan.md`) — real closures fetched and read at the SAME pinned
commit every prior GMF citation in this document uses,
`7e08387c68a7f0e21d13076609a159fd473fc902` of
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea)
(`master`). D6's own bar ("modest deferred-type surface" per sim/ADR-0013
point 4) applies to each candidate closure as a unit; D7's hidden-
import check is run per closure; D5's `type_of_care_transition`
dispatch rule is characterized against real Synthea source, below —
this section is the fix-forward fill-in ADR-0027's own D5 placeholder
points to.

### `ear_infections.json` closure survey — VENDORED this session

| Module | States | State-type gap | Condition-vocab gap | Other findings |
|---|---:|---|---|---|
| `ear_infections.json` (root) | 16 | `CallSubmodule` ×2 (Wave B's own headline target) | none | mandatory-path `Attribute` `is not nil` operator (`End_Ear_Infection_Medications`); mandatory-path `encounter_class: "outpatient"` (`Ear_Infection_Encounter`, the module's OWN primary encounter); mandatory-path `wellness: true` boolean idiom with no `codes` key (`Next_Wellness_Encounter`, the module's own unconditional post-medication fallback) |
| `medications/ear_infection_antibiotic.json` | 19 | none | none (`Age`/`Date`/`Active Allergy`, all v1) | mandatory-path `Attribute` `is nil` operator (`Initial`, an idempotency gate); every `MedicationOrder` (11 of them) carries `assign_to_attribute: antibiotic_prescription` |
| `medications/otc_pain_reliever.json` | 12 | none | none (`Age`/`Date`/`Active Allergy`, all v1) | same shape as the antibiotic submodule: mandatory `is nil` gate on `Initial`, every `MedicationOrder` (6 of them) carries `assign_to_attribute: otc_pain_reliever` |

**Verdict: closure surveys clean of Wave-D-scoped deferred types** (no
`DiagnosticReport`/`MultiObservation`/`CarePlanStart`/`ImagingStudy`
anywhere in the closure) — the ONLY state-type gap is `CallSubmodule`
itself, Wave B's own reason for existing. Four real, previously-
uncharacterized findings surfaced by reading the actual closure rather
than the top-level module alone (the same "survey proposes, the real
loader disposes" pattern section 8's own M5b findings already
established, repeated here for a NEW module):

1. **`MedicationOrder`'s own `assign_to_attribute` field, and
   `MedicationEnd`'s own `referenced_by_attribute` field, are both
   unbuilt.** `GmfState`'s `:medication-order` schema variant carries
   no attribute-assignment field at all (a module using it still loads
   — Malli's `:map` is open by default, so the extra key is silently
   ignored — but the interpreter never WRITES the attribute, so a
   downstream `is not nil` check on it would always read nil). This is
   the SAME reference shape M5b's own finding 6 already named for
   `ConditionEnd`'s `referenced_by_attribute` ("a reference shape v1's
   interpreter does not resolve") — that finding judged it harmless
   for `sinusitis.json` because nothing on that module's own control
   flow ever consulted the missing reference; here it is load-bearing:
   every patient who is ever prescribed a medication through EITHER
   called submodule reaches `End_Ear_Infection_Medications`, which
   gates on exactly this attribute.
2. **The `Attribute` condition type supports only `!=`/`=` operators
   today — `is nil`/`is not nil` are unbuilt.** Confirmed mandatory-
   path: the root module's own post-medication cleanup state and both
   submodules' own idempotency-gating `Initial` states all use one of
   these two operators, never `!=`/`=`.
3. **`encounter_class: "outpatient"` is a real GMF value this loader's
   4-entry `encounter-class->keyword` map (section 1) doesn't
   recognize** — `Ear_Infection_Encounter`, the module's own PRIMARY
   encounter (every patient who ever gets an ear infection reaches
   it), uses it. Falls through to `(keyword (slug c))` at
   normalization (no throw there) but then fails `GmfState`'s
   `:encounter` schema variant, whose `:encounter-class` enum is
   closed to `#{:wellness :ambulatory :emergency :inpatient}` —
   `:schema-invalid` at load. Resolution (Step 2, disclosed as a
   loader-normalization commit, not a new interpreter mechanism):
   `encounter-class->keyword` gains `"outpatient" -> :ambulatory` —
   `ehrt.patient-simulator.compile-trajectory`'s own `encounter->step`
   (confirmed by direct read, lines ~97–102) already treats `:wellness`
   and `:ambulatory` identically (both compile to `:outpatient-visit`),
   so this is a genuine same-concept vocabulary alias, not an invented
   mapping — and it needs no `compile-trajectory` change, keeping this
   ADR's own fence (interpreter/loader only) intact.
4. **The already-documented `wellness: true` boolean idiom (section 8,
   "M7 survey," "A second GMF wellness-encounter encoding this loader
   doesn't recognize") is confirmed MANDATORY-path here, not an
   excludable tail the way that survey's own five prior instances
   (`mTBI`/`atrial_fibrillation`/`osteoporosis`/`epilepsy`/`med_rec`)
   were.** `Next_Wellness_Encounter` is `End_Ear_Infection_Medications`'s
   own unconditional fallback arm (reached by every patient once both
   medication attributes are cleared) — every ear-infection walk
   reaches it. A second wrinkle beyond the boolean-vs-string encoding
   itself: this state carries NO `codes` key at all (Synthea's own real
   wellness-encounter concept is auto-filled by engine machinery this
   project's own GMF port never carried, not authored per-module) —
   `GmfState`'s `:encounter` variant currently requires `:codes`.
   Resolution (Step 2): the SAME loader normalization the M7 finding
   already named (`:wellness true` + absent `:encounter-class` ->
   `:encounter-class :wellness`), plus `:codes` becomes
   `{:optional true}` on `:encounter` — the identical "don't fabricate
   what was never actually said" disposition M5b's finding 6 already
   established for `ConditionAnnotation`'s own `:codes` field.
   `compile-trajectory`'s own `encounter->step` never reads `:codes`
   for an encounter event (confirmed by direct read) — safe to loosen
   at the schema layer with zero downstream impact.

   **Dated note (2026-08-03, `notes/ADRs.md` ADR-0037 AR-3): this
   resolution is RETIRED.** `Next_Wellness_Encounter` no longer
   normalizes onto `:encounter-class :wellness` — it loads as its own
   `:wellness-wait` state type and the interpreter genuinely waits for
   the patient's own next cadence-scheduled visit before opening the
   encounter (`next-wellness-tick`, ADR-0037 AR-1/AR-2), retiring the
   create-now substitution ADR-0031 AR-5(b) disclosed. The ear-
   infections episode itself now resolves at the next cadence tick,
   not immediately after medications end.

**D7 hidden-import check, `ear_infections` closure: empty (clean),
confirmed by exhaustive scan of every `Attribute` condition and every
`SetAttribute`/`assign_to_attribute` write across all three files.**
`antibiotic_prescription` and `otc_pain_reliever` are each READ in a
DIFFERENT module than at least one of their own WRITES — the root
module (`ear_infections.json`) reads what its own called submodule
writes (`assign_to_attribute`), and the root module later WRITES the
same key back to nil (`Unset_Antibiotic_Prescription_Attribute`/
`Unset_Non_Opioid_Prescription_Attribute`) that the submodule's own
`Initial` state later reads again on any subsequent call. This is
concrete, load-bearing evidence for D1's own root-scoping design — not
a hypothetical the closure happens not to exercise.

### `urinary_tract_infections.json` closure survey — DEFERRED this session (D6)

The design session's own framing named this module's payoff as
"contingent on its closure surveying clean" — Step 1's own real-closure
read found it does not. The top-level module's own survey row (this
document's own M7 appendix, "`CallSubmodule` ×3... deferred") already
undercounted the closure's real depth: three named path submodules
(`uti/telemed_path`, `uti/ambulatory_path`, `uti/ed_path`) themselves
transitively call EIGHT more (`uti/hpi`, `uti/gu_pregnancy_check`,
`uti/abx_tx`, `uti/labs`, `uti/lab_follow_up`, `uti/ambulatory_eval`,
`uti/ed_eval`, `uti/ed_bundle`) — twelve real files, not four.

| Module | States | State-type gap | Other findings |
|---|---:|---|---|
| `urinary_tract_infections.json` (root) | 29 | `CallSubmodule` ×3 | **TWO transition kinds outside this document's own four (§2), both new findings**: `type_of_care_transition` (`Care Pathways`, D5, characterized below and built this session) and `lookup_table_transition` — a SIXTH real GMF transition kind this document's own brief never named, on the module's own entry path (`Urinary Tract Infection`, selecting Cystitis vs. Pyelonephritis; `Recurrent UTI`, its own recurrence analogue) |
| `uti/telemed_path.json` | 31 | `CallSubmodule` ×7 | none beyond the calls themselves |
| `uti/ambulatory_path.json` | 18 | `CallSubmodule` ×4 | none beyond the calls themselves |
| `uti/ed_path.json` | 7 | `CallSubmodule` ×2 | none beyond the calls themselves |
| `uti/hpi.json` | 12 | none (`Symptom` ×8) | none |
| `uti/gu_pregnancy_check.json` | 6 | none (`Observation` ×2) | none |
| `uti/abx_tx.json` | 30 | none (`MedicationOrder` ×19) | none |
| `uti/labs.json` | 19 | **`DiagnosticReport` ×2 (Wave-D-scoped)** | none |
| `uti/lab_follow_up.json` | 10 | `CallSubmodule` ×1 (-> `uti/abx_tx`) | none |
| `uti/ambulatory_eval.json` | 7 | `CallSubmodule` ×3 (-> `uti/gu_pregnancy_check`, `uti/labs`, `uti/abx_tx`) | confirmed by direct transition-graph read: its own `complex_transition` sends 100% of patients to `uti/labs` (20% direct, 80% via `uti/gu_pregnancy_check` first) — `DiagnosticReport` is unconditionally reached on this path, not a tail |
| `uti/ed_eval.json` | 7 | `CallSubmodule` ×2 (-> `uti/ed_bundle`, `uti/abx_tx`) | |
| `uti/ed_bundle.json` | 56 | **`DiagnosticReport` ×6, `MultiObservation` ×4 (both Wave-D-scoped)**, `CallSubmodule` ×1 (-> `uti/labs`, a second path into the same deferred type) | |

**Verdict: DEFERRED (D6) — dirty in every branch, not an excludable
tail.** All three of the top-level module's own care pathways
(`Telemedicine`/`Ambulatory`/`ED`) route into either `uti/ambulatory_eval`
(confirmed-mandatory `uti/labs`, `DiagnosticReport`) or `uti/ed_eval`
(`uti/ed_bundle`, `DiagnosticReport` + `MultiObservation`) — both
deferred types this project's own wave plan already scopes to Wave D
("state types needing IR + emitter homes"), not a cheap mechanical
extension the way `ear_infections`' own four findings (above) are. Per
D6: this closure member is dirty, so the WHOLE root module drops from
this wave's vendoring — the payoff shrinks from two closures to one,
honestly, exactly as the session prompt's own "contingent on its
closure surveying clean" framing anticipated. `type_of_care_transition`
(D5) is still characterized and built this session regardless (Wave
B's own structural scope, not conditioned on any one module vendoring)
— `lookup_table_transition` is a genuinely new, unplanned finding and
is NOT built this session: it would need real design (an external
lookup-table CSV mechanism this project has no analog for, `uti.csv`/
`uti_recurrence.csv` are never fetched or vendored here) and nothing
in this wave's own scope depends on it, since `urinary_tract_infections.json`
is deferred regardless of whether it's ever resolved. Named here as a
finding for a future wave, not an escalation this session blocks on
(the outcome — UTI deferred — does not change either way it's
eventually resolved).

**D7 hidden-import check: not run to completion for this closure** —
moot once the closure itself is deferred under D6 (D7 exists to
falsify D1's OWN scoping design against a closure this session is
about to vendor; a closure that never vendors this wave carries no
such obligation).

**Encounter-derivation wrinkle (Step 1(f)), read-only, no engine
change:** confirmed directly against the fetched closure —
`urinary_tract_infections.json` itself has NO `Encounter` state; every
`Encounter`/`EncounterEnd` pair in this closure lives inside the THREE
path submodules (`uti/telemed_path.json` ×6, `uti/ambulatory_path.json`
×3, `uti/ed_path.json` ×1), exactly the cross-boundary-encounter shape
D2's own provenance citation exists to cover. Where the residual sim's
own encounter handling would consume such an event: `ehrt.patient-
simulator.compile-trajectory`'s `encounter->step`/`encounter-end->step`
(section 8's own "Multi-encounter-per-episode" finding already reads
this code path directly, lines ~97–117) — no new consumption point
would be needed, the same `:encounter`/`:encounter-end` trajectory
event shape a root-level `Encounter` state already produces, just
carrying a longer `:module`-citation call path (D2) once emitted from
inside a call. Not exercised by any vendored test this session, since
`urinary_tract_infections.json` itself is deferred (D6) — recorded here
so a future Wave-D session revisiting UTI does not have to re-derive
it.

### D5 — `type_of_care_transition` dispatch-rule characterization

Grounded against `Transition.java`'s own `TypeOfCareTransition` class
and `TypeOfCareTransitionOptions` (Synthea source, same pinned commit),
plus the external `telemedicine_config.json` resource that class reads
at construction time (`TypeOfCareTransition(TypeOfCareTransitionOptions
options)` calls `TelemedicineConfig.fromJSON()`).

**Real Synthea's own dispatch, as read:** `Care Pathways`'s own JSON
carries only three target STATE NAMES (`ambulatory`/`emergency`/
`telemedicine` -> `"Ambulatory"`/`"ED"`/`"Telemedicine"` in
`urinary_tract_infections.json` — no weights of any kind in the module
JSON itself). The actual selection weights live entirely in
`telemedicine_config.json`, keyed on: (a) whether `time` (the simulated
instant) is before or after `start_year: 2020`
(`config.getTelemedicineStartTime()`), and (b) whether the person's
CURRENT insurance payer's name is in `high_emergency_use_insurance_
names` (`["Medicaid", "Dual Eligible", "NO_INSURANCE"]`) — four
`EnumeratedDistribution`s total (pre/during-telemedicine ×
high/typical-emergency), one `person.randLong()`-reseeded sample drawn
from whichever one applies.

**This project's own persona (`ehrt.sim-model.interface/persona`) has
no insurance/payer concept anywhere** — the payer-name half of real
Synthea's own dispatch cannot be evaluated against this project's own
data, the identical shape of gap `Active Allergy`'s own documented
simplification already established (no allergy concept exists to
query either). **Simplification, documented here per D5's own
instruction, not left as a silent assumption:** this interpreter always
uses the `typical_emergency_distribution` branch (never
`high_emergency_distribution`) — the majority-case default, since this
project cannot determine which synthetic patients would fall into the
three named high-emergency-use payer categories. The pre/during-2020
split, by contrast, IS honestly implementable — this project's own
virtual clock (`ctx`'s `:t`) already answers "what calendar year is
it," the identical mechanism `:date` condition (Wave A) already uses —
so that half of the real dispatch rule is NOT simplified away:

- `t`'s own calendar year `< 2020`: `{:ambulatory 0.75 :emergency 0.25}`
  (`pre_telemedicine.typical_emergency_distribution` — no telemedicine
  option exists pre-2020 in Synthea's own real data either, consistent
  with `:ambulatory`/`:emergency` being the module's only two targets
  reachable before that year).
- `t`'s own calendar year `>= 2020`:
  `{:ambulatory 0.56 :emergency 0.2 :telemedicine 0.24}`
  (`during_telemedicine.typical_emergency_distribution`).

**RNG consumption: exactly one `.nextDouble` draw, the SAME fixed-
consumption weighted-pick mechanism `distributed_transition` already
uses** (`weighted-pick-transition`) — `type_of_care_transition` is
implemented as a transition KIND (a 5th sibling of `direct`/
`distributed`/`conditional`/`complex`), not a new state type, since
real Synthea's own `Care Pathways` state is itself a plain `Simple`
carrying this field, exactly like `distributed_transition` already
attaches to any state type. Joins the interpreter ns docstring's own
order contract (Step 2) as: descend into the year-gated weight table
(zero rng, a pure function of `ctx`'s own `:t`), then one weighted-pick
draw — same position in the per-state draw order every other
transition-resolving draw already occupies.

### Regression baseline (Step 1(e))

Fixed-seed walks of the three currently-vendored modules
(`sinusitis.json`, `appendicitis.json`, `sore_throat.json`) — 6 seeds
(`1 2 3 42 20260802 999999`) × 2 sexes each, `run-module` to a 10-year-
bounded horizon from a 25-year history offset (the same shape
`vendored_sore_throat_test.clj`'s own property test already uses),
hashing `(:status result)` and `(hash (:trajectory result))` —
captured before any Wave B code change. Re-run and diffed at every
Step 2/3 checkpoint per ADR-0027's own verification-baselines section;
byte-identical at every one (confirmed one final time at session
close, Step 4).

---

## 10. GMF coverage Wave C: `Death` characterization (2026-08-02)

Step 1 of the Wave C session prompt (`notes/ADRs.md` ADR-0028; C1/C3/C5
of that ADR's own decision record). Read at the SAME pinned commit
every prior GMF citation in this document uses,
`7e08387c68a7f0e21d13076609a159fd473fc902` of
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea)
(`master`) — `stroke.json` itself, and `State.java`'s own `Death` inner
class (the real Synthea engine source, not inferred from the module
JSON alone).

### C1 — Death forms, grounded against `State.java`

Real Synthea's `Death` state (`State.java`, class `Death`) declares five
fields: `codes` (cause-of-death concept list), `conditionOnset` (a
state-name citation), `referencedByAttribute` (an attribute-name
citation), `range`, and `exact` (both time-delay shapes, the SAME
`{low high unit}` / `{quantity unit}` shapes `Delay`/`Procedure`'s own
duration already use). `process(person, time)`, quoted verbatim:

```java
long deathTime = time;
if (exact != null) {
  deathTime = time + Utilities.convertTime(exact.unit, exact.quantity);
} else if (range != null) {
  double delayInDays = person.rand(range.low, range.high);
  deathTime = time + Utilities.convertTime(range.unit, delayInDays);
}
Code causeOfDeath = null;
if (conditionOnset != null) { /* resolve via person.record's own open condition named conditionOnset */ }
else if (referencedByAttribute != null && person.attributes.containsKey(referencedByAttribute)) { /* resolve via the attribute */ }
else if (codes != null && codes.size() > 0) { causeOfDeath = codes.get(0); }
person.recordDeath(deathTime, causeOfDeath);
return true;
```

Three time forms (immediate — neither `range` nor `exact` present, no
rng draw; `exact` — deterministic, no rng draw; `range` — exactly ONE
`person.rand` draw, the SAME fixed-consumption law this project's own
`resolve-time-advance` already implements for `Delay`/`Procedure`
duration — `{low high unit}` is literally the same shape, so `Death`'s
own time resolution needs no new helper, only a new case dispatching
into the existing one). Three cause-of-death resolution forms, in
priority order: `conditionOnset` (a log query against the state that
onset the condition), `referencedByAttribute` (an attribute read), and
`codes` (verbatim, code-passthrough law) — Synthea's own docstring
explicitly notes real Synthea's module CONTINUES past a `Death` state to
whatever it `direct_transition`s to next ("the module will continue to
progress to the next state(s)... typically... to a Terminal state") —
this project's own C2 ruling (ADR-0028) deliberately departs from that:
the walk terminates AT `:death`, so `Death`'s own declared transition is
never followed. **This is a disclosed, ruled simplification, not an
oversight** — donor/post-mortem content (what a real module's own
post-Death states like `stroke.json`'s own `End_Encounter`/
`End_Stroke_Condition` would otherwise represent) is exactly the
"remains future work" the deferred-table's own instruction and C2 both
already name.

**`stroke.json`'s own Death state uses exactly one of each family** —
the `range` time form (`{low: 1, high: 30, unit: "days"}`) and the
`codes` cause form (one SNOMED concept, the stroke diagnosis itself,
repeated verbatim from the module's own `Stroke` ConditionOnset state).
Neither `exact`, `conditionOnset`, nor `referencedByAttribute` is used
by this module. Per C1's own instruction, only the range time-form and
the `codes` cause-form are built this wave; `conditionOnset`/
`referencedByAttribute` are UNBUILT, named here (not silently
mishandled) — the interpreter throws a `programmer-error` ex-info if a
future module's own `Death` state uses either, the same disposition
`evaluate-condition`'s own unsupported-condition-type case already
establishes. `exact`/immediate are built anyway (the SAME code path
`resolve-time-advance` already provides for `range`, at zero marginal
cost — not "speculative" in sim/ADR-0013 point 4's sense, since no new
mechanism is added, only an existing one's `:death` case wires to it).

> **Dated resolution note (2026-08-04, GMF coverage Wave I2, ADR-0041
> AR-1): both UNBUILT forms above are now BUILT** (`ehrt.patient-
> simulator.gmf-interpreter/death-cause-codes`) — `congestive_heart_
> failure.json`'s own four Death states all use `referencedByAttribute`.
> This ALSO corrects this section's own "priority order: conditionOnset,
> referencedByAttribute, codes" claim two paragraphs up, and the quoted
> pseudocode's own `if (conditionOnset != null) ... else if
> (referencedByAttribute...) ... else if (codes...)` shape — both were
> paraphrases, not a verbatim quote, and both had the real order
> backwards. `State.java`'s own `Death.process`, re-read fresh at the
> same pin for ADR-0041: `Code reason = null; if (codes != null) {
> reason = codes.get(0); } else if (conditionOnset != null) { ... }
> else if (referencedByAttribute != null) { ... }` — `codes` is checked
> FIRST. `conditionOnset`'s own real resolution is also more layered
> than the paraphrase above states: `person.hadPriorState(...)` gates a
> first branch (the condition's own PRESENT entry, matched by name);
> failing that, a SECOND fallback reads the named state's own
> JSON-declared codes directly off the module, regardless of whether it
> ever fired. This project's own port implements only the first branch
> (a trajectory citation query, ADR-0041's own `death-cause-codes`) —
> the second fallback is a disclosed, NOT-ported simplification, named
> in ADR-0041's own Fence.

### C5 — `stroke.json`'s own closure survey

| Module | States | State-type gap | Condition-vocab gap | Transition-kind sweep | Other findings |
|---|---:|---|---|---|---|
| `stroke.json` (root, no CallSubmodule — trivial one-file closure) | 12 | `Death` ×1 (this wave's own reason for existing) | none (`Date`, resolved Wave A) | `direct_transition`, `distributed_transition` ×1, `conditional_transition` ×1 — 3 of the now-six known kinds; no `complex_transition`, `type_of_care_transition`, or `lookup_table_transition` | **NEW, mandatory-path gap** (below): `Chance_of_Stroke`'s own `distributed_transition` entry for `"Stroke"` carries `"distribution": {"attribute": "stroke_risk", "default": 0}` — an ATTRIBUTE-SOURCED weight, not a literal number. `GmfState`'s own `TransitionFields` schema requires `[:distribution number?]`; unmodified, this module fails `:schema-invalid` at load, before `Death` is ever reached |

**D7 hidden-import check: vacuous (clean by construction) — a one-file
closure has no cross-module attribute reference to check**, the D7
falsifier's own precondition (D1's root-scoping design) never
applies to a non-calling walk.

### The `stroke_risk` finding — escalated, ruled

`stroke_risk` is set by real Synthea's `CardiovascularDiseaseModule`
(`calculateStrokeRisk`, `src/main/java/org/mitre/synthea/modules/
CardiovascularDiseaseModule.java`, same pin) — a hard-coded ENGINE
module (not a GMF JSON, not part of `stroke.json`'s own closure),
recalculated every simulated timestep from a Framingham 10-year stroke-
risk score (age, sex, smoking status, and comorbidities including
diabetes/coronary heart disease/atrial fibrillation). This project's
persona carries none of that — the same gap SHAPE `type_of_care_
transition`'s payer attribute and `Active Allergy` already established
(a real Synthea data source this project has no analog for), but a
materially WORSE consequence: those two precedents both left a real,
useful DEFAULT branch reachable (the "typical" emergency-use
distribution; a conservative always-false allergy check on an
excludable tail). Here the JSON's own specified fallback is **`default:
0`** — honored literally (this project's own code-passthrough/no-
fabrication discipline leaves no principled alternative), `Chance_of_
Stroke`'s own monthly gate would pick the "keep waiting" branch with
probability 1, every seed, forever: stroke onset — and the `Death`
branch this wave exists to unlock — becomes STRUCTURALLY unreachable,
not merely rare, under a bare vendored run.

**Escalated to the author (design channel, 2026-08-02) rather than
silently resolved, per C3's own escalate-with-evidence spirit
generalized to a gap this consequential to the wave's own payoff
claim.** Three options were named (defer `stroke.json` this wave;
substitute a documented nonzero constant, breaking no-fabrication
discipline; port a minimal, disclosed stroke-risk approximation as its
own scoped design). **Ruled: defer `stroke.json` this wave**, the same
D6 treatment ADR-0027 already gave `urinary_tract_infections.json` — a
module whose own mandatory path can't be honestly resolved within this
wave's own scope drops from vendoring, and the payoff shrinks honestly
rather than being papered over. `Death` itself is built fully this wave
regardless (C1/C2/C3/C4 are not conditioned on any one module vendoring
— the same "the mechanism landed either way" framing ADR-0027's own D5
account already used for `type_of_care_transition`) and proven against
the project's own hand-authored test fixture instead of a real vendored
module's own death branch (Step 3's own revised scope, ADR-0028's
deviation record). `stroke.json`'s own survey row (the M7 appendix
table, above) is updated fix-forward: **deferred, revisit trigger — an
attribute-sourced `distributed_transition` weight mechanism AND a
stroke-risk-equivalent data source both landing together**, not decided
or scoped this session.

> **Dated note, GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, H3):
> the attribute-sourced `distributed_transition` weight mechanism named
> above is now BUILT** (`ehrt.patient-simulator.gmf-interpreter/resolve-
> distribution-value`, a `{:attribute name :default n}` NamedDistribution
> map, byte-confirmed against `stroke.json`'s own `Chance_of_Stroke`
> shape) — but this does NOT unblock `stroke.json`: the revisit trigger
> was always BOTH halves landing together, and the second half
> (a stroke-risk-equivalent data source) remains entirely unowned. The
> mechanism is proven instead against a hand-authored fixture (§14's
> own D3b account), the same "build the mechanism, prove it against a
> fixture, defer the vendoring target" shape `VitalSign` (D1a) and the
> CarePlan mechanism (D2) already established.

### C3 — the `:expired` gap table

Every claim `components/sim/docs/patient-state-model.md`'s `:status`
accumulator row and `components/sim/docs/clinical-realities.md`'s
post-mortem entry make, checked against the LIVE code (not the docs'
own prose) as of this session's own starting commit (`d8447e6`):

| Captured claim | Implemented where | Status |
|---|---|---|
| `:status` includes `:expired` as a real value | `ehrt.sim-engine.engine/PatientState`'s `:status` enum, `[:enum :new :admitted :discharged :merged]` | **Docs-only.** `:expired` appears NOWHERE in `components/sim/src` except three lines of PROSE inside `ehrt.sim-check.check`'s own comments (`check.clj` lines 340-349) explaining why `order-only-when-admitted` is written as a strict generalization — no code path can produce, read, or check this value today |
| `:expired` reached via a death event OR an expired discharge disposition | — | **Docs-only.** No `:death`/disposition-carrying event type exists in `ehrt.sim-engine.engine`'s `decide`/`evolve` multimethods |
| Therapeutic-intent event classes illegal when `:expired` | `ehrt.sim-check.check/order-only-when-admitted`, `/clinical-content-only-when-admitted` | **Partially implemented, by construction rather than by design.** Both invariants already read `(not= :admitted (:status before))` — the STRICT generalization patient-state-model.md's own note anticipates ("already covers it once `:expired` lands, without inventing an unfalsifiable invariant"). Once `:expired` is a real, distinct value, these two invariants automatically extend to cover it with ZERO code change — confirmed by direct read, not merely asserted |
| Morgue/funeral-home transfer, autopsy/specimen, donor-management/procurement legal while `:expired` | — | **Docs-only, and explicitly OUT of this wave's own minimal-path scope** (C3: "donor pathways... named finding, not built") |
| A final disposition-20 discharge later moves `:expired` -> `:discharged` | — | **Docs-only, out of scope** — the same donor/post-mortem administrative content C2 already defers |

**Declared minimal coherent path (C3):** (1) `:expired` joins
`PatientState`'s `:status` enum for real. (2) `Death` maps into the
compiled pathway via the EXISTING `:discharge` IR step (C4 — no new IR
step type), carrying two new optional fields: `:disposition [:enum
:expired]` and `:codes` (cause of death, verbatim). (3) `ehrt.sim.
engine`'s `:discharge` `decide`/`evolve` branch on `:disposition`: an
`:expired`-disposition discharge sets `:status :expired` (never
`:discharged`), leaves `:location`/`:attending` UNCHANGED (the
"clinically absorbing but operationally alive" fact — patient-state-
model.md's own words — requires the bed to stay occupied, not vacated),
and — a genuinely NEW piece the gap table above didn't name, found by
tracing `:discharge`'s own decide method directly — the existing
bed-ready-transfer coupling (a `:discharge`'s own decide call searches
for a boarding patient at the same vacated ward and conjoins a
`:transfer` relieving them) MUST NOT fire for an `:expired`-disposition
discharge, since no bed is actually vacated; unguarded, it would double-
occupy a bed no-double-occupancy already forbids. (4) One new,
directly-named structural invariant (mirroring `no-events-after-merged-
terminal`'s own existing shape): a location co-occupancy check specific
to the expired case (`expired-patient-retains-location` — never nil
immediately after an expired-disposition discharge, the converse of
`admitted-occupies-one-slot`'s own "never nil while admitted" rule,
stated explicitly rather than left to fall out of the generalization
above by accident). No IR/`sim-model` schema change beyond `:discharge`'s
two new optional fields is needed — both are additive, and `:discharge`'s
existing zero-field call sites (every hand-authored pathway that predates
this wave) are unaffected by construction (Malli's open-map + `{:optional
true}` fields, the same backward-compatibility shape `citation-fields`
already establishes elsewhere in `engine.clj`). **No escalation triggered
by this table** — the minimal path touches no pathway-IR step-TYPE and no
`sim-model` schema beyond two optional fields on an existing step.

---

## 11. GMF coverage Wave D stage D1a: observation-family characterization (2026-08-02)

Step 1 of the D1a session prompt (`notes/ADRs.md` ADR-0029 R2(a)/(c),
R6). **This session is characterization only (E1) — no schema, compile-
mapping, or engine code lands here; the PROPOSAL this characterization
feeds is recorded separately in ADR-0029's own D1 placeholder, marked
PROPOSED, awaiting a design-channel ruling.** Read at the SAME pinned
commit every prior GMF citation in this document uses,
`7e08387c68a7f0e21d13076609a159fd473fc902` of
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea)
(`master`) — `sepsis.json` itself, plus four real engine source files:
`State.java` (`MultiObservation`/`DiagnosticReport`/`ObservationGroup`/
`Observation`/`VitalSign` inner classes), `HealthRecord.java`
(`multiObservation`/`report`/`Observation`/`Report`), `Person.java`
(`getVitalSign`/`setVitalSign`), and `LifecycleModule.java` (the
built-in, non-GMF module that actually populates the vital signs
`sepsis.json` reads).

### D1a-1 — `sepsis.json` closure: single file, no `CallSubmodule`

`sepsis.json` has **zero `CallSubmodule` states** (confirmed by direct
state-type census, below) — unlike Wave B/C's own closures
(`ear_infections`/`urinary_tract_infections`), there is no second file
to fetch or characterize. The existing survey row (Appendix, above,
M7 survey) already named this module's disposition; this session adds
the closure-level and source-level detail that row's own format didn't
carry.

| Module | States | State-type gap | Condition-vocab gap | Other findings |
|---|---:|---|---|---|
| `sepsis.json` (root, no `CallSubmodule` — trivial one-file closure) | 37 | `MultiObservation` ×2, `DiagnosticReport` ×1 (Wave D's own reason for existing) | none (`Active Allergy`/`Age`/`Observation`, all v1 — `Observation`-as-condition-type resolved Wave A) | see D1a-3 through D1a-7, below, for the full account |

**State-type census** (direct count against every state's own `type`
key): `ConditionOnset` ×3, `Death` ×1, `Delay` ×3, `DiagnosticReport`
×1, `Encounter` ×1, `EncounterEnd` ×2, `Guard` ×1, `Initial` ×1,
`MedicationOrder` ×4, `MultiObservation` ×2, `Observation` ×6,
`Procedure` ×6, `Simple` ×5, `Terminal` ×1 (sums to the survey row's
own 37). Every type OTHER than `MultiObservation`/`DiagnosticReport`
is already v1 — `Death` (Wave C), the four original trajectory-event
types (`ConditionOnset`/`MedicationOrder`/`Observation`/`Procedure`),
and the five consumed-internally types (`Initial`/`Terminal`/`Simple`/
`Delay`/`Guard`) — confirming this closure is EXACTLY as clean as the
prioritization table already claimed: the two Wave-D-scoped types are
the module's only real blocker.

**Transition-kind sweep, against all seven known kinds (§2):**
`direct_transition` ×28, `distributed_transition` ×6,
`conditional_transition` ×2 — **zero** `complex_transition`, **zero**
`type_of_care_transition`, **zero** `lookup_table_transition`, and —
checked directly against every one of the six `distributed_transition`
states' own distribution values (`0.06`/`0.94`/`0.6`/`0.4`/`0.125`/
`0.875`/`0.2`/`0.8`/`0.18`/`0.82`/`0.5`/`0.5`) — **zero** attribute-
sourced weights (every value is a literal number, unlike `stroke.json`'s
own `stroke_risk` finding, ADR-0028). **This is a real, honest finding
against the prompt's own anticipated shrinkage:** sepsis needs NONE of
the three D3-scoped transition kinds — D1 carries no D3 dependency via
transitions, and D1's own payoff is not resequenced or shrunk on this
axis.

### D1a-2 — `MultiObservation`/`DiagnosticReport`: one shared parent, embedded-only children, no cross-state coupling

Grounded against `State.java`'s own class hierarchy, not the module
JSON alone. `MultiObservation` and `DiagnosticReport` both extend a
private abstract class, `ObservationGroup`:

```java
private abstract static class ObservationGroup extends State {
  protected List<Code> codes;
  protected List<Observation> observations;
  ...
}
public static class MultiObservation extends ObservationGroup {
  private String category;
  public boolean process(Person person, long time) {
    for (Observation o : observations) { o.process(person, time); }
    ...
    HealthRecord.Observation observation =
        person.record.multiObservation(time, primaryCode, observations.size());
    ...
    observation.category = category;
    return true;
  }
}
public static class DiagnosticReport extends ObservationGroup {
  public boolean process(Person person, long time) {
    for (Observation o : observations) { o.process(person, time); }
    ...
    Report report = person.record.report(time, primaryCode, observations.size());
    ...
    return true;
  }
}
```

**Children are EMBEDDED, never referenced.** `observations` is a
Gson-bound `List<Observation>` — the JSON `"observations"` array is a
list of FULL, INLINE `Observation` STATE definitions (the same shape
`sepsis.json`'s own standalone `Lactate_Level`/`Pulse_Oximetry` states
use), not a count or a set of names pointing at other states. There is
no field on `ObservationGroup`, `MultiObservation`, or
`DiagnosticReport` that names a preceding state — the Java type itself
forecloses a reference-based authoring shape, this is not merely what
`sepsis.json` happens to do. `sepsis.json`'s own `Record_Blood_Pressure`
(two embedded children, `range`-sourced) and `Blood_Cultures` (one
embedded child, `value_code`-sourced) both confirm this directly.

**`number_of_observations` (JSON) is DEAD — grepped for across the
whole of `State.java`, zero hits under any camelCase binding.** The
`int numberOfObservations` parameter `multiObservation`/`report` (below)
actually consume is the JAVA LIST's own `.size()` at the `process()`
call site (`observations.size()`, quoted above) — NOT anything read
from the module's own JSON. `sepsis.json`'s `Record_Blood_Pressure`
carries `"number_of_observations": 0` and it is never read; the real
count is 2, the length of its own `"observations"` array. This project's
own schema needs no `number_of_observations`-equivalent field at all —
the children VECTOR's own count already is the count.

**`category` is `MultiObservation`-only at the Java level —
`DiagnosticReport` (and `ObservationGroup` itself) declare no
`category` field.** Confirmed both by the class bodies above and by
direct inspection: `sepsis.json`'s `Blood_Cultures` (`DiagnosticReport`)
carries no top-level `category` key; `Record_Blood_Pressure`/
`Record_Blood_Pressure_2` (`MultiObservation`) both carry
`"category": "vital-signs"`. Each CHILD observation carries its own
`category` regardless (`Observation`'s own field, per-child) — the
top-level `category` is a MultiObservation-only convenience, not a
report-level concept `DiagnosticReport` shares.

**No coupling between the two state TYPES exists anywhere — structural
or referential.** In `sepsis.json`: `Blood_Cultures` (`DiagnosticReport`)
fires exactly once, unconditionally, the FIRST state after
`Sepsis_ED_Encounter` opens — reached by 100% of sepsis-onset patients.
`Record_Blood_Pressure`/`Record_Blood_Pressure_2` (`MultiObservation`)
fire 0, 1, or 2 times depending on path (patients who reach `Normal_MAP`
then `Discharge_to_Home` directly — the majority path — never reach
either) — confirmed by full transition-graph trace, below. **Neither
ever names or reads the other; they are two independent state types
that share an implementation parent and a JSON authoring shape, nothing
more.** Consistent with this document's own existing prioritization
table (Appendix): of the modules already read across Waves A–C,
`gallstones`/`dialysis`/`lung_cancer`/`colorectal_cancer` all carry
`DiagnosticReport` with no `MultiObservation` anywhere — the reverse
(a `MultiObservation`-only module) was not found in the 41-module
corpus already surveyed, though that survey was never run looking
specifically for this combination and this session did not re-run it —
named as a gap in the existing evidence, not claimed as a negative
proof.

**Full reachability trace (for the record):** `Sepsis_ED_Encounter` ->
`Blood_Cultures` -> `Administer_Broad_Spectrum_Abx` -> {`Aztreonam` |
`Piperacillin_Tazobactam`} -> `Vancomycin` -> `Vitals_and_Labs` ->
`Capillary_Refill` -> `Pulse_Oximetry` -> `Lactate_Level1` ->
`Fluid_Resuscitation` -> `Check_Septic_Shock` -> {0.2 `Low_MAP` ->
`Septic_Shock` -> `Admit_to_ICU` | 0.8 `Normal_MAP` ->
`Admit_to_Inpatient`} -> ... -> `Fluid_Resuscitation2` -> `Lactate_Level`
-> {value>=2: `Administer_Vasopressors` -> `Norepinephrine` ->
`Record_Blood_Pressure` | else: `Record_Blood_Pressure` directly} ->
`Check_ARDS` -> {0.18 ARDS branch -> `Ventilator`/`Ventilator_Weaning`
-> `Delay_in_ICU` or `Death` | 0.82 `Delay_in_ICU` directly} ->
`Delay_in_ICU` -> {0.125 `Death` | 0.875 `Record_Blood_Pressure_2`} ->
`Admit_to_Inpatient` -> `Delay_3-10_days` -> {0.6 `Discharge_to_Home` |
0.4 `Admit_to_ICU`, a genuine relapse CYCLE back into the ICU branch —
noted for completeness, not itself a GMF anomaly; Synthea's own
`distributed_transition` graphs are not required to be acyclic the way
D3's own closure call-graph is}. `Record_Blood_Pressure` is reached on
the `Low_MAP`/vasopressor branch AND on the ICU-relapse branch;
`Record_Blood_Pressure_2` is reached only via `Delay_in_ICU`'s own
0.875 branch — both MANDATORY once on their own branch (the existing
survey row's own characterization stands, confirmed rather than merely
repeated).

### D1a-3 — Observation value-sourcing: three real mechanisms in this one closure, only one built today

`State.java`'s `Observation.process()` (non-legacy branch, quoted in
full) tries, in order: `distribution`, `attribute` (a person-attribute
read), `vitalSign` (`person.getVitalSign(vitalSign, time)`), `valueCode`,
`expression` (CQL), `sampledData`, `attachment`. The LEGACY branch
(`isLegacyGmf()` — true whenever `exact`/`range` is present on THAT
STATE, a per-state check, not a module-wide `gmf_version` flag despite
`sepsis.json`'s own top-level `"gmf_version": 2` key) supports only
`exact`/`range`. `sepsis.json` exercises exactly THREE of these:

1. **`range`** (legacy path, ALREADY BUILT) — `Lactate_Level`,
   `Lactate_Level1`, `Low_MAP`, `Normal_MAP`, plus `Record_Blood_
   Pressure`'s two embedded children. Matches this project's own
   `sample-observation-extra` (`gmf_interpreter.clj` lines 535-540)
   exactly: `(rand-double-in rng low high)`, one draw, unit carried
   verbatim.
2. **`value_code`** (a coded/qualitative finding, e.g. `Capillary_
   Refill`'s "Increased capillary filling time" and `Blood_Cultures`'
   own embedded child, "Positive (qualifier value)") — **UNBUILT.**
   `sample-observation-extra` has no branch for it; a `value_code`-only
   Observation (no `range`) compiles today to `{:codes codes}` with no
   value at all — the qualitative finding is silently dropped, not
   fabricated, but also not carried.
3. **`vital_sign`** (a named-vital-sign lookup, e.g. `Pulse_Oximetry`'s
   `"vital_sign": "Oxygen Saturation"` and `Record_Blood_Pressure_2`'s
   two embedded children, `"Systolic Blood Pressure"`/`"Diastolic Blood
   Pressure"`) — **UNBUILT, and its real upstream source has NO analog
   anywhere in this project (D1a-4, below).** Same silent-drop
   consequence as `value_code`.

**Real texture, worth naming plainly: the SAME clinical concept (blood
pressure) is authored via BOTH mechanisms side by side in one module** —
`Record_Blood_Pressure`'s children use `range` (a flat 40-120 mm[Hg]
sample), `Record_Blood_Pressure_2`'s children use `vital_sign` (reads a
continuously-trending physiological value). Real Synthea authors mix
idioms even within a single module; a `:diagnostic-report`/`:observation`
child schema that only accepts ONE of the three value-sourcing shapes
would reject real, mandatory-path content this closure already proves
exists.

### D1a-4 — `vital_sign`'s real source: `LifecycleModule.java`, a hardcoded Java module this project has never ported

`Person.getVitalSign` THROWS if the named vital sign was never set
(`vitalSigns.get(vitalSign)` returns null -> `IllegalStateException`,
`Person.java` lines ~551-556) — there is no silent default at the
`Person` level. Real Synthea supplies `SYSTOLIC_BLOOD_PRESSURE`/
`DIASTOLIC_BLOOD_PRESSURE`/`OXYGEN_SATURATION` (among many others) via
`LifecycleModule.java`, a Java class registered as one of Synthea's
CORE modules (run for every patient, same as any GMF module, but
authored in Java, not JSON — entirely outside this project's GMF
loader/interpreter and outside its own module-vendoring surface):

```java
person.setVitalSign(VitalSign.SYSTOLIC_BLOOD_PRESSURE,
    new BloodPressureValueGenerator(person, SysDias.SYSTOLIC));
person.setVitalSign(VitalSign.DIASTOLIC_BLOOD_PRESSURE,
    new BloodPressureValueGenerator(person, SysDias.DIASTOLIC));
...
person.setVitalSign(VitalSign.OXYGEN_SATURATION, oxygenSaturation); // age/hypoxemia-conditioned
```

**This project's persona/clinical-state model has NO equivalent
continuous physiology simulation for blood pressure, oxygen saturation,
or any other `LifecycleModule`-sourced vital** — the identical shape of
gap `Active Allergy`'s own documented simplification and D5's own
payer-name gap already established (a real upstream mechanism this
project's own data model has nothing to evaluate against). **A `vital_
sign`-typed Observation value therefore cannot be honestly computed
from this project's own state today, by construction, not by omission**
— this is the concrete, evidence-grounded finding the session prompt's
own E4(b) asked for, though it lands on the `vital_sign` FIELD (an
`Observation`/`MultiObservation`-child property), not on the `VitalSign`
STATE TYPE or the `Vital Sign` CONDITION TYPE R2(c) actually names
(D1a-5, next).

### D1a-5 — `VitalSign` (state type) and `Vital Sign` (condition type): both ABSENT from `sepsis.json`'s own closure

Checked directly, exhaustively: `sepsis.json` has zero states of type
`VitalSign` and zero conditions of `condition_type: "Vital Sign"`
anywhere in its closure. **R2(c)'s own dissolution design
(`VitalSign` state -> observation-flavored events, `Vital Sign`
condition -> a log query) is therefore NEITHER confirmed NOR
contradicted by sepsis — this closure simply does not exercise either
mechanism, a negative result, not evidence either way, recorded
plainly per E4(b)'s own instruction rather than silently treated as a
pass.** The RELATED-but-distinct `vital_sign` FIELD gap (D1a-4) is real
and load-bearing for D1's own scope (it blocks two of this closure's
own `Observation`/`MultiObservation`-child values); R2(c)'s own STATE-
and CONDITION-type claims remain untested by any vendored-candidate
evidence gathered to date, across Waves A–D. `VitalSign`'s own real
semantics (`State.java`, class `VitalSign`, quoted for the record):
`process` calls `person.setVitalSign(vitalSign, ...)` with either a
constant (`exact`), a uniform range (`range`, legacy), or a
`Distribution`/CQL expression (non-legacy) — establishing a
CONTINUOUSLY-READABLE generator, not a one-time point value, over
however long until a later state (or another `VitalSign` state)
overwrites it. This is a materially different shape than this project's
existing `:observation` step (a single point-in-time sample) — worth
flagging for whichever future session actually builds `VitalSign`
support, not resolved here.

### D1a-6 — D7 hidden-import check and encounter-bearing check

**D7 (ADR-0027's own falsifier), applied to this single-file closure:**
zero `Attribute`-condition reads, zero `Observation.attribute`-sourced
reads, anywhere in `sepsis.json` — grepped exhaustively. The module's
ONE `assign_to_attribute` write (`Acute_Respiratory_Distress_Syndrome_
ARDS` -> `ARDS`, a `ConditionOnset` state — note this field is not even
in this project's own `:condition-onset` schema variant today,
`gmf.clj` line 365, only `:medication-order`'s carries `:assign-to-
attribute`; harmless here since nothing reads it back) is never read
anywhere in this closure. **Clean, but trivially so — a single-file
closure has no cross-module read/write pair to falsify at all**, a
different reason than `ear_infections`' own real cross-module D7 pass
(§9).

**Encounter-bearing check:** `sepsis.json` HAS its own root-level
`Encounter` state (`Sepsis_ED_Encounter`, `encounter_class: "emergency"`)
— unlike `urinary_tract_infections.json`'s own root module (§9), sepsis
does not depend on any called submodule to open an encounter.

### D1a-7 — Emission-side inventory: what today's ORU rendering carries, and the gap a `:diagnostic-report` step must close

Read directly against `ehrt.sim-emit-hl7.emit-hl7` (the message-type
registry and both existing ORU builders). **`:result-available` ->
`oru-message`** (MSH, PID, PV1, ORC, ONE `obr-segment` keyed on
`:concept`, one `obx-segment` PER entry in `:results`) — `obx-segment`
REQUIRES `:reference-range`/`:abnormal-flag` (`order-profiles`' own
computed truth, OBX-7/OBX-8) and hardcodes OBX-2 `"NM"` (numeric) —
neither field exists anywhere on a GMF-derived observation, and OBX-2
`"NM"` is wrong for a `value_code`-sourced result (needs `"CWE"`, HL7's
coded-value-with-exceptions type). **`:observation` -> `observation-
message`** (MSH, PID, PV1, ONE `observation-obx-segment`, NO ORC/OBR) —
closer in shape (OBX-3 from `:codes`, OBX-5 the numeric `:value` when
present, OBX-6 `:unit`) but (a) it is SINGLE-OBX only, no panel/group
shape, and (b) it shares the same `"NM"`-only, numeric-only limitation
— no path renders a `value_code`-sourced qualitative finding today,
in EITHER builder.

**What a `:diagnostic-report` step's real, legal ORU^R01-with-OBR
rendering needs, concretely:** (1) `obr-segment` reused as-is for OBR-4
— it is already generic on `concept`, and the report-level `:codes` this
step's own top-level carries (`Blood_Cultures`' own panel LOINC, D1a-2)
is exactly what it wants. (2) A NEW OBX-builder — `observation-obx-
segment`'s simpler field set (codes/value/unit, no reference-range/
abnormal-flag — GMF data has neither), but able to render EITHER a
numeric value (OBX-2 `"NM"`) OR a `value_code` (OBX-2 `"CWE"`, OBX-5 the
code's own display/code, the same `cwe-field` helper `obr-segment`
already uses for OBR-4) — today's code has no branch for the second
case in either builder. (3) One OBX per embedded child (D1a-2's own
embedding finding — never a reference to resolve), `set-id` from
position, the same `map-indexed` shape `oru-message` already uses for
`:results`. (4) ORC-1/ORC-2 reused unchanged (`orc-segment`, already
generic on `control-id`) — a `:diagnostic-report`'s own control-id needs
no new `control-id-for` case, the existing single-subject fallback
already covers it. **No `message-type-registry` change beyond a new
`:diagnostic-report -> {:type "ORU" :trigger "R01"}` entry** — the same
trigger `:result-available`/`:observation` already use, since a real
DiagnosticReport panel IS an ORU^R01, ORC+OBR present (unlike the
order-less `:observation` shape).

## 12. GMF coverage Wave D stage D1b: observation family implementation (2026-08-02)

D1a's own schema PROPOSAL (P1–P6) was RULED (`notes/ADRs.md` ADR-0029's
own dated ruling note, Q1–Q4 resolved) and landed this session, per-layer,
each co-landing schema + compile-trajectory mapping + engine handling +
emission decision, this project's own standing co-landing convention.

**Loader (`ehrt.patient-simulator.gmf`).** `MultiObservation`/
`DiagnosticReport` join v1 as two distinct loadable state types
(`gmf-type->keyword`), both extending Synthea's own private
`ObservationGroup` — grounded directly against the real, fetched
`sepsis.json` (D1a-2's own citations): embedded children carry NO
`:type`/transitions of their own, only `:category`/`:unit`/`:codes` plus
exactly one of `:range`/`:value-code`/`:vital-sign` (`ObservationChild`,
new). `:observation`'s own GmfState gains the same two new optional
fields (`:value-code`/`:vital-sign`) a standalone Observation state can
carry (Capillary_Refill/Pulse_Oximetry, D1a-3). `:vital-sign`'s raw
string value is left untouched at load time — a lookup key into the
reference table below, never a module-authored identifier to
slug/namespace the way `:attribute`/`:symptom` are.

**Interpreter (`ehrt.patient-simulator.gmf-interpreter`).**
`sample-observation-extra` gains `value_code`/`vital_sign` branches
alongside the pre-existing `range` branch (D1a-3's three mechanisms,
side by side), plus `:category` pass-through (Q1's own ruling). The
`vital_sign` branch (`vital-sign-extra`) draws ONE uniform value from
`patient-simulator/vital-signs.edn`'s own `:reference-range` for the named
vital sign — the SAME plain-range mechanism `range` already uses (P4's
own Option A, RULED); because the value is drawn FROM that range by
construction, the OBX abnormal-flag this same range supports (Q2+Q3)
is always `:normal`, an honest computed consequence of the
simplification. An unrecognized vital-sign name throws
(`:unrecognized-vital-sign`) — a real, visible rejection, the table's
own "grows by evidence" rule. `MultiObservation`/`DiagnosticReport`
both compile to ONE trajectory event type, `:diagnostic-report`,
carrying the report-level `:codes` (optional, D1a-2) and the full
`:observations` vector — each child sampled via the SAME
`sample-observation-extra`, reused verbatim, never a parallel
child-sampling implementation, consuming RNG per child in vector order.

**Compile-trajectory.** `observation-fields` extracted (value/unit/
value-code/category/reference-range/interpretation) and shared by both
the top-level `:observation` step compile and each `:diagnostic-report`
child (`diagnostic-report->step`) — P1/P2's own "no third type" applied
literally in code, not merely in schema. `:diagnostic-report` joins
`pre-horizon-dropped-types` (the same treatment `:observation` already
gets).

**Engine (`ehrt.sim-engine.engine`).** `decide`/`evolve :diagnostic-report`
follow P5's own recommendation exactly: `decide` emits ONE
`:diagnostic-report` ground-truth event (codes + the full observations
vector, mirroring how the compiled IR step itself bundles children);
`evolve` FLATTENS each child into its own `ObservationRecord` appended
to `:observations` — the identical pattern `:result-available`'s own
per-analyte flattening already establishes, reused rather than a third
accumulator shape invented. `ObservationRecord` gains `:value-code`/
`:category`; `:reference-range`/`:interpretation` already existed
(`:result-available`'s own fields, now shared). `check.clj`'s
`clinical-content-only-when-admitted` invariant extends its own event
set to include `:diagnostic-report` — the same therapeutic-intent-class
scoping `:procedure`/`:observation`/`:medication-order` already get.

**Emission (`ehrt.sim-emit-hl7.emit-hl7`).** `observation-obx-segment`
itself is EXTENDED in place, then reused directly at both call sites
(`observation-message` AND the new `diagnostic-report-message`) rather
than duplicated into a separate `report-obx-segment` builder — P6's own
"sharing observation-obx-segment's field set" is realized here as
literal reuse, one function, not a near-duplicate sibling. It now
branches OBX-2 `"CWE"`/`"NM"` on whether the observation carries
`:value-code`, rendering a SNOMED CT-coded finding via a new system-
aware `coded-value-field` (`cwe-field` itself stays LOINC-hardcoded and
untouched — every pre-existing call site is a LOINC panel/analyte
concept) — plus, beyond P6's own base sketch, OPTIONAL reference-range/
abnormal-flag rendering when the observation carries them (Q2+Q3's own
ruling, so a table-sourced STANDALONE `:observation` event — e.g.
Pulse_Oximetry — renders them too, not only a `:diagnostic-report`
child), appended ONLY when present (a variadic trailing-field build,
never a positional pad) — byte-identical to every pre-existing call
when absent, confirmed by field-count assertion. `diagnostic-report-
message` (new): MSH/PID/PV1/`orc-segment` (reused unchanged)/
`obr-segment` (reused unchanged, report-level codes)/one
`observation-obx-segment` per child via `map-indexed`, the same shape
`oru-message` already establishes for `:results`. One new
`message-type-registry` entry, `:diagnostic-report -> {:type "ORU"
:trigger "R01"}`; `control-id-for` needed no change (P6's own finding).

**`VitalSign` (state type) / `Vital Sign` (condition type) — design-
ruled (R2(c)), implementation-deferred, unchanged by this stage.**
Neither is built this session, per F3's own explicit fence (D1b's own
session prompt): no vendored module across Waves A–D exercises either
mechanism (D1a-5's own negative result, §11, still the current
evidence), and this workspace does not build unexercised machinery.
The vital-sign reference table (D1 F2) already carries what a future
`VitalSign` implementation will need (LOINC code/units/reference-range
per named vital sign); the design itself (`VitalSign` state ->
observation-flavored event, `Vital Sign` condition -> a log query,
R2(c)'s own ruling) stands as recorded, untested against real evidence.
The next session vendoring a real `VitalSign`-bearing or `Vital Sign`-
condition-bearing candidate should re-derive against source at that
point, not treat D1a-5/P3's own sketch as settled (D1a-5's own closing
note, restated here since D1b's own implementation pass is where a
reader would otherwise expect this gap to have closed).

Full field-by-field diffs, test coverage, and the sepsis.json vendoring
payoff are in the D1b session record and the commits it names (ADR-0029
execution note).

---

## 13. GMF coverage Wave D stage D2: closure survey and characterization findings (2026-08-02)

Step 1 of the D2 session (ADR-0029 R6; the CarePlan family). Both
candidate closures (`myocardial_infarction.json`,
`total_joint_replacement.json`) fetched IN FULL — every transitively
`CallSubmodule`-reachable file, not the top-level module alone — at the
SAME pinned commit every prior GMF citation in this document uses,
`7e08387c68a7f0e21d13076609a159fd473fc902` of
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea)
(`master`).

### `myocardial_infarction.json` closure survey — DEFERRED this session

The top-level survey (section 8, "`total_joint_replacement`... 4
transition kinds all appear in one module") and the prioritization
table's own MI row both undercounted this closure's real depth, the
SAME shape the UTI finding (ADR-0028) already established: MI's own
five root-level `CallSubmodule` targets (`heart/stemi_pathway`,
`heart/nsteacs_pathway`, `heart/acs_discharge_meds`,
`heart/acs_arrival_medications`, `heart/cardiac_labs`) expand
transitively — through the CABG (coronary artery bypass graft) surgical
pathway alone — into **27 real files total** (root + 26 submodules;
full closure fetched and read, not sampled): `heart/cabg_sequence` ->
`heart/cabg/{outcomes,operation,cabg_referral,postop}` and
`heart/operative_status`; `heart/cabg/operation` itself calls
`surgery/general_anesthesia`, `heart/cabg/{or_labs_meds,or_intraop,details}`;
`heart/cabg/or_labs_meds` calls `heart/or_blood`;
`heart/cabg/{cabg_referral,postop,preoperative}` chain into
`heart/cabg/{preoperative,labs_common,icu_meds_devices,postop_blood}`;
`heart/acs_discharge_meds` calls `medications/{beta_blocker,statin,ace_arb}`
(each 1000+ lines, real drug-class submodules). D7 hidden-import check
not pursued to completion — moot once the state-type census (below)
disqualifies the closure outright, the same "not run to completion,
moot once deferred" disposition UTI's own D7 entry (section 9) already
uses.

**State-type census, full 27-file closure (direct count against every
state's own `type` key):** `MedicationOrder` ×185, `Simple` ×107,
`Procedure` ×68, `SetAttribute` ×42, `CallSubmodule` ×33, `Observation`
×29, `Terminal` ×27, `Initial` ×27, `Delay` ×21, `DiagnosticReport` ×17,
`ConditionOnset` ×15, `MedicationEnd` ×14, `EncounterEnd` ×10,
`Encounter` ×10, **`SupplyList` ×6** (a state type this document has
never named — genuinely new, entirely unbuilt, no schema/loader entry
exists), **`ImagingStudy` ×5** (R5 — explicitly OUT of Wave D, a named
CHF-triggered hole, unowned by D0–D3), `Death` ×5, `Device` ×4,
`DeviceEnd` ×3, **`Counter` ×2** (named in this document's own original
brief as deferred, still unowned by any wave), `ConditionEnd` ×2,
`CarePlanStart` ×2 (this stage's own scope — present, but moot),
`MultiObservation` ×1. `DiagnosticReport`/`MultiObservation` are D1
scope, already landed — not a blocker on their own.

**Transition-kind sweep, against the six real GMF kinds this
document's own §2 names:** `direct_transition` ×473,
**`lookup_table_transition` ×39** (D3's own scope, ADR-0027's D6
finding — a SIXTH kind this loader does not build), `conditional_transition`
×37, `distributed_transition` ×32, `complex_transition` ×27, ZERO
`type_of_care_transition`.

**Verdict: MI is disqualified from D2's own vendoring scope by THREE
independent, each-individually-sufficient reasons** — `lookup_table_transition`
(D3-scoped, not this stage's), `ImagingStudy` (R5, explicitly out of
Wave D), and `SupplyList`/`Counter` (never-owned deferred types, one of
them genuinely new). The loader's own all-or-nothing gate means the
closure fails to LOAD at all regardless of where any of these states
sit in the module's own probability distribution (the same disposition
`spina_bifida.json`'s low-probability `Death` state already
established — reachability is irrelevant to a load-time schema gate).
**MI stays deferred, not vendored, resequenced honestly per G4** — its
own real blocker set (D3 kind + R5 type + two never-owned deferred
types) is a DIFFERENT, and larger, set than the CarePlan gap D2 itself
was framed around; CarePlan's own presence in this closure (×2) is
real but moot.

### `total_joint_replacement.json` closure survey — DEFERRED this session (a second blocker found at Step 2, below)

Four files total, fully resolved (root + `medications/moderate_opioid_pain_reliever`,
`total_joint_replacement/functional_status_assessments`,
`dme/wheelchair_end`) — no further `CallSubmodule` targets found at any
depth, confirmed by exhaustive cross-check of every `"submodule"` key
against every fetched file.

| Module | States | State-type gap | Condition-vocab gap | Other findings |
|---|---:|---|---|---|
| `total_joint_replacement.json` (root) | 33 | `CallSubmodule` ×4 (Wave B, already built), `CarePlanStart` ×1, `CarePlanEnd` ×1 (D2's own reason for existing) | none (`Age`/`And`/`Attribute`/`Date`, all v1) | `Joint_Replacement_Guard`'s own mandatory `joint_replacement is not nil` gate has no in-closure source (below) |
| `total_joint_replacement/functional_status_assessments.json` | 20 | none | none (`Attribute` only) | idempotency-gating `assessment_done` attribute, root-scoped, same shape ear_infections' own `Initial` gates |
| `medications/moderate_opioid_pain_reliever.json` | 12 | none | none | `assign_to_attribute: opioid_prescription`, read via a plain `Attribute` condition elsewhere in the root (not a `MedicationEnd` citation) — already-built v1 mechanism, no gap |
| `dme/wheelchair_end.json` | 4 | none | none | two `DeviceEnd` states, `referenced_by_attribute` pointing at attributes never written anywhere in this closure — harmless: `:device-end`'s own interpreter case is unconditional pass-through (`gmf_interpreter.clj` line 767), never resolves the reference |

**State-type census (full 4-file closure):** `SetAttribute` ×10,
`Delay` ×8, `Simple` ×6, `MultiObservation` ×6 (D1 scope, already
landed), `Terminal` ×4, `Initial` ×4, `CallSubmodule` ×4,
`MedicationOrder` ×3, `EncounterEnd` ×3, `Encounter` ×3, `DeviceEnd` ×3
(no matching `Device` state anywhere in the closure — confirmed
harmless, `:device-end` never resolves its own reference either way),
`Procedure` ×2, `Guard` ×1 (already v1, `gmf.clj` line 89),
`ConditionOnset` ×1, `CarePlanStart` ×1, `CarePlanEnd` ×1. Every type
OTHER than `CarePlanStart`/`CarePlanEnd` is already v1 — **the closure
surveys clean of every Wave-D-scoped type except the one D2 itself
exists to build.**

**Transition-kind sweep:** `direct_transition` ×42,
`conditional_transition` ×8, `distributed_transition` ×5,
`complex_transition` ×1 — ZERO `type_of_care_transition`, ZERO
`lookup_table_transition`. No D3 dependency; D2's own payoff is not
resequenced or shrunk on this axis.

**D7 hidden-import check: clean.** Every `SetAttribute`/`assign_to_attribute`
write and every `Attribute`-condition/`referenced_by_attribute` read
across all four files resolves within the closure's own root-scoped
namespace (`joint_replacement`'s own gap, below, is a MISSING source,
not a hidden cross-module wiring bug — the attribute name itself is
consistent everywhere it appears). `assessment_done` (submodule-local
idempotency gate) and `opioid_prescription`
(`assign_to_attribute`/plain-`Attribute`-read pair) both resolve
correctly under this project's existing root-scoping contract (Wave B,
D1), no new interpreter mechanism needed.

**`Guard`'s own mandatory `joint_replacement is not nil` gate — a
real, load-bearing, specify-vs-delegate finding, grounded directly
against the module's own text.** `Joint_Replacement_Guard` (the
closure's own second state, reached unconditionally from `Initial`)
requires `joint_replacement is not nil` before ANY further content —
including the `CarePlanStart`/`CarePlanEnd` pair D2 exists to build —
is ever reached. No state anywhere in this four-file closure ever
WRITES `joint_replacement` (confirmed by exhaustive `SetAttribute`
scan, above). The module's OWN top-level `remarks` field states the
reason plainly: *"This is not a standalone module. Currently joint
replacements are triggered by the 'joint_replacement' attribute set by
the osteoarthritis and rheumatoid arthritis modules. Possible values...
are 'hip' and 'knee'."* Real Synthea runs `osteoarthritis.json`/
`rheumatoid_arthritis.json` as INDEPENDENT sibling root modules per
patient, sharing one un-namespaced `person.attributes` map —
architecturally different from this project's own root-scoped
CallSubmodule contract (Wave B, D1), and neither sibling module is any
part of this closure or this session's own scope to vendor.

**Ruling (self-ruled at this characterization gate, precedented
directly by D1a's own Q2+Q3 governing principle: "never override what
the vendored artifact specifies; freely supply what it delegates to
the engine" — the SAME pattern Persona already establishes for
Synthea's own demographics engine, and the vital-signs reference table
already establishes for `LifecycleModule.java`'s own unported
physiology).** `joint_replacement` is DELEGATED content (no default
value anywhere in the module JSON, unlike `stroke_risk`'s own
specified `default: 0`) — sourced from two sibling modules this
project has no reason to vendor just to unblock one attribute. Rather
than defer `total_joint_replacement.json` outright (the `stroke.json`
disposition), this session supplies the gap the SAME documented-
simplification way: `ehrt.patient-simulator.gmf-interpreter/run-module`
gains one new, purely-additive, backward-compatible trailing arity
accepting an `initial-attributes` map (defaults `{}` on every existing
call site — zero behavior change for appendicitis/sinusitis/sore_throat/
ear_infections/sepsis/death-fixture), and the vendored TJR test
supplies `joint_replacement` (`"knee"`/`"hip"`, both walked) as an
authored, provenance-cited starting attribute — citing this module's
own `remarks` block as the source of truth for why no in-project origin
exists, the same disclosure discipline the vital-signs table's own
citation already models. This is a narrower, more surgical fix than
building `osteoarthritis.json`/`rheumatoid_arthritis.json` themselves
(out of scope, unowned by any wave).

> **Dated note (2026-08-03, ADR-0033): the ENGINE-layer half of this
> rider (H7) is now resolved too.** At this stage (D2), `initial-
> attributes` reached only `run-module`'s own interpreter-layer arity —
> `engine.clj` had no config surface to seed it at all, a gap this
> paragraph did not yet own (it was found and pinned two sessions later,
> ADR-0030 J3). ADR-0033's own AR-1 adds that config surface
> (`:module-initial-attributes`, or a closure's own `:initial-
> attributes` for a direct API caller) and AR-3 threads it through
> `:registered`'s decide method — the SAME `"knee"` value this
> paragraph's own citation supplies, reused verbatim, now also proven
> through a real engine run (`vendored_tjr_test.clj`'s own seeded
> round-trip test, `components/sim-emit-hl7/test/`).

**The multi-encounter-per-episode compile-time truncation gap (section
9's own prioritization table row) still applies to this module exactly
as that row already documents** — `Pre_Procedure_Encounter_End` (the
module's own FIRST `:encounter-end`) sets `compile-trajectory`'s own
`encounter-closed?` before `Joint_Replacement_Encounter` (the real
surgical encounter, and everything inside it including
`CarePlanStart`/`CarePlanEnd`) ever opens — confirmed by direct
transition-graph read this session, not merely re-cited. **This does
NOT block D2's own vendoring**, for the same reason it never blocked
ear_infections/sepsis: this project's vendored-CLOSURE tests prove the
INTERPRETER's own raw trajectory (`run-module`), never the compiled
`compile-trajectory` -> pathway-IR -> engine -> emit-hl7 pipeline — that
fuller pipeline is proven only for single-file, non-closure modules
(appendicitis, sinusitis) to date, and remains a real, still-open,
already-named gap standing between ANY closure-having module and a
full engine/emission demonstration of its own actual surgical content.

> **Dated note (2026-08-03, ADR-0031 AR-6's second defect-fix session,
> `notes/ADRs.md` ADR-0033): CLOSED.** `engine.clj`'s `:registered`
> decide method now threads a closure's own `:modules`/`:tables` maps
> and this section's own `initial-attributes` seed through to
> `run-module`'s full arity (ADR-0033 AR-2/AR-3) — the full compile-
> trajectory/engine/emit round trip this paragraph names is proven for
> TJR (and the other two closure roots) by
> `components/sim-emit-hl7/test/vendored_tjr_test.clj`'s own seeded
> round-trip test, and by `bin/oracle-src/ehrt/oracle/digest.clj`'s own
> `total-joint-replacement-engine` first baseline (AR-4b). The multi-
> encounter-per-episode compile-time truncation gap itself (this
> paragraph's own first half) is UNCHANGED by that fix — a separate,
> still-open `compile-trajectory` gap, not touched by ADR-0033's own
> registration-wiring scope.

**Fix-forward finding (dated note, filled Step 2, 2026-08-02): a
SECOND, independent blocker found empirically AFTER Step 1's own
characterization closed, testing the `initial-attributes` fix against
the real closure.** `Joint_Replacement_Guard`'s own `allow` is `{:and
[Attribute joint_replacement is-not-nil, Age > 50 years]}` — a
COMPOUND condition. `ehrt.patient-simulator.gmf-interpreter/age-guard-
jump-days` (the analytical short-circuit that lets a failing bare
`:age >= N years` Guard jump forward in virtual time rather than block
forever) only recognizes a BARE `:age` condition with operator `>=` —
its own docstring states this plainly ("any other failing condition...
returns nil... the walk blocks instead"), a KNOWN, deliberate v1
boundary, not an accidental gap. `Joint_Replacement_Guard`'s condition
is neither bare (`:and`-wrapped) nor `>=` (`>`) — `guard-step` cannot
resolve it, and this interpreter has no periodic re-tick mechanism (a
walk is one continuous recursive descent, not Synthea's own ticked
simulation clock): the walk BLOCKS PERMANENTLY at `:t` = DOB (age 0),
before `Joint_Replacement_Guard`'s own second child (Attribute) is
ever even reached, confirmed empirically (a throwaway probe test:
`joint_replacement` seeded via the new `initial-attributes` arg,
registration 60 years post-DOB, 20-year horizon — `:status :blocked`,
zero trajectory events). This is a genuinely new, previously-
uncharacterized interpreter gap (compound/non-`>=`-operator Age
Guards), outside G1–G5's own ruled scope (the CarePlan pair's schema/
interpreter/engine/emit chain, not Guard/condition analytical-
resolution machinery) — extending `age-guard-jump-days` to handle a
compound condition and a strict `>` correctly (verifying every OTHER
`:and` sibling already holds, plus getting the day-vs-year integer-
age-flooring boundary right for a strict inequality) is real
interpreter-core work, not a data-seeding simplification the D1a
governing principle already covers — an ESCALATION with evidence
(G1's own instruction), not improvised this session under time
pressure against core walk-time-advance logic that every other
vendored root's own Guard/Delay behavior also depends on staying
byte-identical.

**Declared D2 vendoring scope, REVISED: ZERO roots vendored this
session** — an outcome G4 explicitly names as acceptable.
`myocardial_infarction.json` deferred (three independent, each-
sufficient blockers, none of them CarePlan-shaped — named above, own
row added to section 9's own prioritization table). `total_joint_
replacement.json` ALSO deferred — the `joint_replacement` attribute
gap alone was resolved (the `run-module` `initial-attributes`
extension, real and kept), but the newly-found compound-Guard
blocker stands unresolved, named here as this closure's own next
prerequisite, unowned by any wave until a future session extends
`age-guard-jump-days`/`guard-step`. The CarePlan mechanism itself
(sim-model schema, patient-simulator loader/interpreter/compile mapping,
sim engine fold, sim-emit-hl7 disclosed silence — all four layers,
Steps 2a–2d) is real, fully co-landed, GREEN infrastructure,
independent of whether any real module exercises it yet — the SAME
"build the mechanism, defer the vendoring target" shape D1a's own
`VitalSign` disposition and this wave's own `ImagingStudy`/`lookup_
table_transition` rows already establish, not a partial or half-
finished implementation.

**`Active CarePlan` (condition type) — design-ruled, implementation-
deferred per G2, unchanged by this stage.** Not built this session: no
vendored module across Waves A–D exercises it (`total_joint_
replacement.json`'s own closure — the only D2 candidate whose
condition vocabulary was fully surveyed — uses `Age`/`And`/`Attribute`/
`Date` only, zero `Active CarePlan`, confirmed by exhaustive scan), and
this workspace does not build unexercised machinery (the SAME fence
D1's own `Vital Sign` condition disposition already states). The
design itself (a log-query over `:care-plan-start`/`:care-plan-end`
trajectory events, architecturally the same shape `active-onset-
condition-holds?` already establishes for `Active Medication` — one
function, a different event-type pair) is straightforward to build the
day a real candidate needs it; the next session vendoring one should
build it fresh against that module's own real usage, not treat this
sketch as settled.

> **Dated resolution note (2026-08-04, GMF coverage Wave I2, ADR-0041
> AR-2): the sketch above is now BUILT, `active-careplan-condition-
> holds?`** (`ehrt.patient-simulator.gmf-interpreter`) — the real
> candidate this paragraph anticipated turned out to be
> `wellness-encounters.json`'s own closure member, `encounter/
> depression_screening.json` (`Check Eligibility`'s own At-Least
> guard), not `total_joint_replacement.json`. The design matches the
> sketch's own prediction almost exactly (`active-onset-condition-
> holds?` reused over `:care-plan-start`/`:care-plan-end`, one function,
> a different event-type pair) — grounded, this time, against
> `Logic.java`'s own `ActiveLogic` parent class (`ActiveCarePlan`'s own
> four-method override alone does not show the dispatch: `:codes`
> checked first, `:referenced-by-attribute` only when `:codes` is
> absent, re-testing the referenced entry's own active status, never
> merely "the attribute exists"). Only the `:codes` form is vendored-
> exercised; `:referenced-by-attribute` is installed, proven by a
> hand-built fixture, per ADR-0041's own execution record.

## 14. GMF coverage Wave D stage D3: closure survey and characterization findings (2026-08-02)

Step 1 of the D3 session (ADR-0029 R6, H1/H5: `lookup_table_transition`,
attribute-weighted `distributed_transition`, TJR's own compound-Guard
blocker, the UTI closure re-characterization). Per H5, UTI's own closure
is FRESH-fetched in full (not trusting the D2-era file list — there was
none anyway, UTI was last characterized at Wave B, ADR-0027, section 9)
and TJR's own D2 fetch is re-verified by hash, all at the SAME pinned
commit every prior GMF citation in this document uses,
`7e08387c68a7f0e21d13076609a159fd473fc902` of
[`synthetichealth/synthea`](https://github.com/synthetichealth/synthea)
(`master`).

### D3a — `LookupTableTransition` dispatch-rule characterization

Grounded against `Transition.java`'s own `LookupTableTransition`,
`LookupTableTransitionOption`, `LookupTableKey`, `DistributedTransition-
Option`, and `NamedDistribution` classes (Synthea source, same pinned
commit).

**JSON shape, read directly off `urinary_tract_infections.json`'s own
`Urinary Tract Infection`/`Recurrent UTI` states:** `lookup_table_
transition` is a vector of `{transition, default_probability, lookup_
table_name}` entries — a SIXTH transition kind, sibling to `direct`/
`distributed`/`conditional`/`complex`/`type_of_care`. `lookup_table_name`
is a relative CSV filename (`"uti.csv"`/`"uti_recurrence.csv"`), never
slugged — the same "file reference, not a semantic identifier"
disposition `:submodule` already established (Wave B, D3).

**Real Synthea's own dispatch (`LookupTableTransition.follow`):** the
CSV's own column headers, minus its LAST N columns (N = the JSON's own
`lookup_table_transition` entry count), are the table's ATTRIBUTE
columns; the remaining N columns are the transition-name columns, one
per declared entry (`createDistributedTransitionOptions` throws if a
CSV column name doesn't match a declared JSON transition — a load-time
consistency check, not a runtime one). Per attribute column: `"age"` is
special-cased — parsed as an inclusive `low-high` integer range
(`Range.between`, Apache Commons, BOTH ends inclusive) and matched
against `person.ageInYears(time)` (an Integer falling inside the row's
own range); `"time"` is special-cased the same way for a date range
(unused by either UTI table — no `"time"` column in either CSV, NOT
built this session, named unbuilt/unneeded); every OTHER column name is
matched by exact STRING equality against `person.attributes.get(name).
toString()`. A row match is looked up in a `HashMap<LookupTableKey,
List<DistributedTransitionOption>>` (`LookupTableKey`'s own overridden
`equals`, lines ~505-570: an Integer age matches a stored Range via
`Range.contains`, never Range-to-Range unless both sides are ranges);
NO row match falls back to `defaultTransitions` — a `Distributed
TransitionOption` list built directly from each entry's own JSON
`default_probability` (`loadDefaultTransitions`). Either way, the
FINAL pick is `pickDistributedTransition`: one `person.rand()` draw
(`[0,1)`), a cumulative-weight walk over whichever option list applies
— the SAME fixed-consumption weighted-pick shape this project's own
`weighted-pick-transition` already implements (row lookup itself is a
pure, zero-rng key match; exactly one draw follows, win or fallback).

**Key-column audit (H2's own specify-vs-delegate bar), both tables:**
`uti.csv`/`uti_recurrence.csv` (below) each declare exactly TWO
attribute columns, `age`/`gender` — both fields this project's own
Persona genuinely supplies (`age-years-at`, already built; `:sex`,
already read by `gender-condition-holds?`). BUILDABLE, no escalation:
neither table names a field outside this project's persona model, the
same audit `stroke_risk`/`joint_replacement` FAILED (ADR-0028/ADR-0029's
own D2 note) and vital-sign names PASSED (D1a). No table this session
fetched declares a `"time"` column — that half of real Synthea's own
mechanism stays NAMED, UNBUILT (installed ≠ used, H1's own instruction),
not silently assumed general.

**BUILT (2026-08-03, GMF coverage Wave LC, ADR-0038 AR-1(b)):** both
the `time` column and the age/gender-only whitelist above are retired
history — `time` is now special-cased exactly like `age` (both accepted
`Utilities.parseDateRange` forms, transcribed from the pin), and every
OTHER attribute column resolves generically (module attribute first,
then a persona-field mapping), never validated against a closed set.
The "H2's own specify-vs-delegate audit" language in this subsection
describes the ORIGINAL D3a scope decision, not current behavior.

**Vendored content (fetched, hashed, this session — Step 3's own
vendoring target):**

| Table | Columns | Rows | SHA-256 |
|---|---|---|---|
| `lookup_tables/uti.csv` | `age,gender,Cystitis,Pyelonephritis,Wait_for_UTI` | 10 (5 age bands × 2 sexes) | `c3eec06429961a4484d1ab5f14973b778e59b0b80bbdad3fa2629329c8dbf231` |
| `lookup_tables/uti_recurrence.csv` | `age,gender,Cystitis,Pyelonephritis` | 10 | `baf597d27a7c139f962b7a100ff02abfcdc616c540478c7867e888305965aeda` |

Both tables' own rows sum to 1.0 per row (confirmed by direct read) —
this project's own `weighted-pick-transition` (normalizes by the
entries' own summed total before drawing) and real Synthea's own
`pickDistributedTransition` (an unnormalized cumulative walk against a
raw `[0,1)` draw, DistributedTransition's own docstring: values not
summing to 1.0 shift weight onto the last entry) are behaviorally
IDENTICAL whenever weights already sum to 1.0, as both vendored tables'
own rows do — no divergence for this vendoring, the existing helper is
reused unchanged.

**Design (H1, pinned before implementation): closure DATA-FILE members
(R4/H2).** `ehrt.patient-simulator.gmf/load-closure` gains a SEPARATE,
optional trailing `table-resolve-fn` argument (purely additive — every
existing 3-arg call site is unaffected, the same "optional trailing
arity" shape `run-module`'s own `initial-attributes` addition already
established, D2) and a parallel `lookup-table-names` collector (mirrors
`call-submodule-paths` exactly: every distinct `lookup_table_name` any
state's own `:lookup-table-transition` entries name). The all-or-
nothing gate (D3, ADR-0027) extends to table members: an unresolvable
name, an unparseable CSV, or a table declaring an attribute column
outside `#{"age" "gender"}` rejects the WHOLE closure (a NEW rejection
reason, `:unrecognized-lookup-table-column`, the same "REJECTED, never
silently skipped" disposition `:unsupported-state-type` already
establishes) — never a silent partial table.

**RETIRED (2026-08-03, GMF coverage Wave LC, ADR-0038 AR-1):** this
whitelist never mirrored anything upstream does (read directly against
the pin, `LookupTableTransition`'s own `loadLookupTable`/`follow` has
no closed column vocabulary at all) and was blocking real attribute
columns. Any non-weight column other than `age`/`time` now loads
unconditionally; `:unrecognized-lookup-table-column` no longer exists
as a rejection reason (`:malformed-lookup-table-range` — a structurally
invalid `age`/`time` cell — is the only load-time rejection a table's
own content can still trigger). This section's own account of the
ORIGINAL D3a design stays below, unedited, as the historical record of
what was built then; do not read it as the current behavior.

`load-closure`'s own
return shape gains `:tables` (call-path-shaped table name -> parsed
table), parallel to `:modules`, empty when a closure names none. CSV
parsing is a small, in-house line/comma splitter (both vendored tables
are header-plus-plain-numeric-rows, no quoting/escaping needed) — the
same "SimpleCSV, a lightweight ad hoc parser" shape real Synthea's own
`Utilities.readResource`/`SimpleCSV.parse` pairing already uses, not a
new external dependency for two trivial files.

`tables` threads through the interpreter as a SEPARATE, parallel
optional trailing argument to `step`/`walk-module`/`run-module`
alongside `modules` (not folded into it — `modules` keeps its own
existing documented shape, call-path -> loaded module, unchanged),
defaulting to `{}` (no tables available) at every existing call site —
zero behavior change for every one of the six already-vendored roots,
none of which ever declare a `lookup_table_transition`. `resolve-
transition` (and therefore `pass-through-outcome`, threaded through
every state-type case in `step`, not only `:call-submodule`) gains the
new kind: a zero-rng row lookup (`age-years-at` + persona `:sex` mapped
to `"F"`/`"M"`, the same map `gender-condition-holds?` already uses,
inverted) against the resolved table, falling back to the entries' own
`default-probability` list on no match, then ONE `weighted-pick-
transition` draw — joining the interpreter's own descend-run-return
order contract at the same position every other transition-resolving
draw already occupies.

### D3b — attribute-weighted `distributed_transition` (`NamedDistribution`)

Grounded against `Transition.java`'s own `DistributedTransitionOption`/
`NamedDistribution`/`processDistributedTransition` (lines ~79-106,
~707-773). A `distributed_transition` entry's own `distribution` field
is `Object` at the Java level: a plain `Double` (this project's own
already-built v1 case) OR a JSON object `{"attribute": name, "default":
n}, ` deserialized into `NamedDistribution` (`this.attribute`/`this.
defaultDistribution`, field names verbatim). `pickDistributedTransition`
resolves it at DRAW time: `dist = person.attributes.containsKey(nd.
attribute) ? (Double) person.attributes.get(nd.attribute) :
nd.defaultDistribution` — an attribute-sourced weight with a JSON-
specified fallback, read from the SAME flat `person.attributes` bag
every other attribute reference resolves against (this project's own
root-scoped equivalent, `attribute-condition-holds?`'s own `root-id`
mechanism). This is EXACTLY `stroke.json`'s own `Chance_of_Stroke` gap,
byte-confirmed here against source rather than re-derived from ADR-0028's
own prose: `{"attribute": "stroke_risk", "default": 0}` (`docs/gmf-
interpreter.md` section 10, this document, unchanged).

**H3's own disposition, restated for the record: this mechanism landing
does NOT unblock stroke.** `stroke_risk` is SPECIFIED content (the
JSON's own literal `default: 0`) whose real upstream source
(`CardiovascularDiseaseModule.calculateStrokeRisk`, an engine module,
not a GMF JSON) this project has no persona equivalent for — honoring
`default: 0` literally (this project's own no-fabrication discipline
leaves no other principled reading) still makes `Chance_of_Stroke`'s own
`"Stroke"` branch structurally unreachable. Building the MECHANISM does
not change that; `stroke.json` stays deferred until BOTH the mechanism
(this session) AND a stroke-risk-equivalent data source (unowned, ADR-
0028/ADR-0029 R7) land together, exactly as section 10's own dated note
already named. Proven instead against a hand-authored fixture (Step 2,
H3), the same "build the mechanism, prove it against a fixture" shape
`Death`'s own C1/C2 build already used before `death-fixture.json`
existed.

**Design (H1): a `:distributed-transition` entry's own `:distribution`
field becomes `[:or number? [:map [:attribute :string] [:default
number?]]]` (schema); `weighted-pick-transition` resolves EITHER shape
per entry — a plain number unchanged, or `(if (contains? (:attributes
ctx) k) (get (:attributes ctx) k) default)` where `k` is the SAME root-
scoped, slugged keyword every other attribute read already computes.
Zero new rng draw beyond the existing one-per-pick; the attribute READ
itself is a pure lookup, the same "zero-rng" property every other
attribute-condition read already has.**

### D3c — UTI closure re-survey (full 12-file, H5's own fresh fetch)

Every file re-fetched this session (not trusted from Wave B's own
prose) at the pinned commit; SHA-256 recorded per file, below. State-
type census, transition-kind sweep, and condition-vocabulary sweep
confirm Wave B's own headline finding (section 9) still holds — the
closure's ONLY state-type gap, once `DiagnosticReport`/`MultiObservation`
(D1) and `CarePlanStart`/`CarePlanEnd` (D2, present in this closure only
incidentally via shared call structure — confirmed NOT present, see
below) are counted as v1, is `lookup_table_transition` itself, this
stage's own reason for existing — plus several new, MECHANICAL
findings this session's fuller (field-level, not just type-level) read
surfaces that section 9's own type-census-only survey could not have
caught.

| File | States | SHA-256 |
|---|---:|---|
| `urinary_tract_infections.json` (root) | 29 | `18de2b8e30d41ef1770fcb10aaf5912bfdb15dbe459cec18730029a37cf9ef7b` |
| `uti/telemed_path.json` | 31 | `5f176628fb9209291dcae24ae78b76337cc5768dd9743975926c300026dde78c` |
| `uti/ambulatory_path.json` | 18 | `c6a65a8da02f240f41fab61300594e0be04eb2a9b077b579daf0b8c6d47adddb` |
| `uti/ed_path.json` | 7 | `184ce09969461a6409e990cac2e688ef84699daa1a2120a76afa4c59d0846c92` |
| `uti/hpi.json` | 12 | `eb789dabf6ad72896be1c089775d282b73a4653cff8736013bbac954f98b6242` |
| `uti/gu_pregnancy_check.json` | 6 | `945e01345cf9c32d8ed5740fa3ca1de81303c94bf0f75d7ea8dcd9d359506f77` |
| `uti/abx_tx.json` | 30 | `4a67624e9bb7a755022f758efff1f9240d22cd2149d19e18c79f79fc9f9a0d05` |
| `uti/labs.json` | 19 | `a85e6f7d81e2fe673f08d39253b4de4de600a50a1f829c85a43abbc3723b2b04` |
| `uti/lab_follow_up.json` | 10 | `ba89f931feaa0bc89bdf37909661c93d55fd7ef7fe87d5e37b65414890c3d58a` |
| `uti/ambulatory_eval.json` | 7 | `117c7462c39549af328d0d17889058c09fc1002b9516fb6a71f95cd0976cae93` |
| `uti/ed_eval.json` | 7 | `24b7e6173b8c4da9782f2e04c50d3c098121def9efb1d933b9d729d93cc767a1` |
| `uti/ed_bundle.json` | 56 | `d9b85e669c1ac1f49bb04ed00d5e2a73260284c01a743fca23290680a61f7927` |

**State-type census (232 states total): every type is ALREADY v1
(`SetAttribute`, `Terminal`, `ConditionEnd`, `ConditionOnset`, `Delay`,
`MedicationEnd`, `Simple`, `CallSubmodule`, `Initial`, `Guard`,
`MedicationOrder`, `Procedure`, `Encounter`, `EncounterEnd`,
`Observation`, `DiagnosticReport`, `MultiObservation`, `Symptom`) — ZERO
`CarePlanStart`/`CarePlanEnd`/`ImagingStudy`/`SupplyList`/`Counter`/any
other deferred type anywhere in the closure.**

**Transition-kind sweep, all seven known kinds:** `direct_transition`
(every file), `conditional_transition` (16), `complex_transition` (21),
`distributed_transition` (7), `type_of_care_transition` (1, root's own
`Care Pathways`, already built Wave B), **`lookup_table_transition` (2,
both root: `Urinary Tract Infection` -> `uti.csv`, `Recurrent UTI` ->
`uti_recurrence.csv`)** — ZERO occurrences of any kind beyond these six;
no eighth kind found.

**Condition-vocabulary sweep:** `Age`, `Gender`, `And`, `Or`, `At Least`,
`Attribute`, `Active Condition` — ALL already v1 (Wave A/B). Zero new
condition types.

**D7 hidden-import check: clean.** Every cross-module attribute flow
confirmed root-scoped and correctly resolving under the existing Wave B
mechanism: `uti/abx_tx.json`'s own 18 `MedicationOrder` states all
`assign_to_attribute: "UTI_Tx"`; the ROOT's own `End UTI Tx` reads it
via `referenced_by_attribute: "UTI_Tx"` — write in a different closure
member than the read, exactly D7's own falsifier target, resolving
correctly by construction (root-scoped keys, D1).

**Specify-vs-delegate audit:** every attribute/value source found
(`uti.csv`/`uti_recurrence.csv`'s own age/gender columns, D3a above) is
persona-backed and buildable; no SPECIFIED-but-unsourceable value
(the `stroke_risk`/`joint_replacement` shape) appears anywhere in this
closure.

**New mechanical findings (field-level, not caught by section 9's own
type-only census) — each cheap, narrowly-scoped, the SAME "loader
normalization, not a new interpreter mechanism" disposition Wave B's own
D6 `encounter_class`/`wellness` findings already established (ADR-0027's
own Deviation record precedent for "characterization surfaces a real,
in-spirit-authorized finding," landed rather than dropped):**

1. **`gmf_version: 2` — every one of the twelve files carries this tag
   (`urinary_tract_infections.json` through `uti/ed_bundle.json`), the
   FIRST time this project's own vendored-module survey has encountered
   it.** It is additive, not a wholesale format replacement: the SAME
   file freely mixes the OLD per-field `range`/`exact`/`duration` shapes
   (already v1) alongside a NEW, uniform encoding — a top-level
   `"distribution": {"kind": "EXACT"|"UNIFORM", "parameters": {...}}`
   plus a sibling top-level `"unit"` — replacing `Delay`'s own top-level
   `range`/`exact` keys, `Procedure`'s own `duration` field, and
   `Symptom`'s own top-level `range`/`exact` severity keys, one state at
   a time, author's choice. 52 occurrences across the closure (34
   `EXACT`, 18 `UNIFORM`, confirmed by direct scan); ZERO third `kind`
   value found. Mechanical translation, confirmed field-by-field against
   both v1 and v2 examples of the SAME state type (`hpi.json`'s own
   `History Taking`/`Dysuria` states, `ambulatory_eval.json`'s own `Eval
   Procedure`): `UNIFORM` -> the existing `Range` shape (`{:low
   parameters.low :high parameters.high :unit unit}`); `EXACT` -> the
   existing `Exact` shape (`{:quantity parameters.value :unit unit}`,
   `unit` absent for unitless Symptom severity). Loader normalization
   (`gmf.clj`'s `normalize-state`), dispatched on the state's own
   already-normalized `:type`: `:delay`/`:symptom` write the translated
   shape to their own existing TOP-LEVEL `:range`/`:exact` keys (the
   exact keys `resolve-time-advance`/the `:symptom` interpreter case
   already read); `:procedure` writes it to its own existing `:duration`
   key, in the SAME flat `{:low :high :unit}` shape v1-authored
   `:duration` fields already use (confirmed against `appendicitis.json`/
   `sepsis.json`) — deliberately NOT a `{:range {...}}`-wrapped shape,
   matching the pre-existing v1 encoding exactly rather than inventing a
   new one. **FIXED (2026-08-03, `notes/ADRs.md` ADR-0032, commit
   `1ea1f4a`) — was disclosed, not fixed, pre-existing gap found along
   the way:** `ehrt.patient-simulator.gmf-interpreter/resolve-time-advance`
   was called with `(:duration state)` as its own argument and
   destructured `{:keys [range exact]}` FROM that argument — but
   `:duration` (both v1- and, after this normalization, v2-authored) is
   a FLAT `{:low :high :unit}`/`{:quantity :unit}` map, never `{:range
   {...}}`/`{:exact {...}}`-wrapped, so `range`/`exact` were always nil
   and `resolve-time-advance` silently fell to `:else t` (zero advance)
   for EVERY `Procedure` state's own `:duration`, in every vendored
   module, v1 or v2, confirmed by direct read. Was out of D3's own ruled
   scope (H1-H8 name three mechanisms and a re-characterization, not a
   `Procedure`-timing fix) — named there as a live, unowned gap; fixed
   by ADR-0032 (ADR-0031 AR-6's first defect-fix session): the call site
   (`emit-and-advance`, gmf-interpreter.clj) now wraps the flat
   `:duration` as `{:range duration}` before calling `resolve-time-
   advance`. Oracle-bracketed against `bin/regression-oracle` — the
   census discovered by that same bracket that the hand-authored
   `death-fixture.json` ALSO carries a duration-bearing Procedure,
   correcting this session's own survey (ADR-0032's own AR-4 dated
   note has the full account).
2. **`SetAttribute`'s own `value_code` field (a Concept, the same shape
   `:observation`'s own `value_code`/`:diagnostic-report`'s own children
   already carry) is unbuilt** — confirmed on TJR's root file (below,
   D3f), not this closure; noted here for one combined build decision.
3. **An embedded `MultiObservation`/`DiagnosticReport` child's own
   `exact` value-sourcing mechanism (a fourth, alongside D1a-3's `range`/
   `value_code`/`vital_sign`) is unbuilt** — confirmed on TJR's own
   `functional_status_assessments.json` (below, D3f), not this closure;
   noted here for one combined build decision.
4. **Six new vital-sign names, unlisted in `patient-simulator/vital-
   signs.edn`** (`uti/ed_bundle.json`'s own BMP/CMP `DiagnosticReport`
   children, 4 occurrences each across the file's own repeated lab
   panels): `Glucose`, `Urea Nitrogen`, `Calcium`, `Sodium`, `Potassium`,
   `Chloride`, `Carbon Dioxide` — SEVEN names, not six (corrected count:
   Glucose/Urea Nitrogen/Calcium/Sodium/Potassium/Chloride/Carbon
   Dioxide). LOINC codes verified this session against a live public FHIR
   terminology server (`r4.ontoserver.csiro.au`, CSIRO — a citable public
   $lookup endpoint, the same "real codes, cross-checked against a
   citable public source" discipline F21 already establishes, loinc.org
   itself still returning 403 to automated fetch): `Glucose` 2345-7,
   `Urea Nitrogen` 3094-0, `Calcium` 17861-6, `Sodium` 2951-2,
   `Potassium` 2823-3, `Chloride` 2075-0, `Carbon Dioxide` 2028-9 — all
   confirmed active. Reference ranges are author-curated typical-adult
   values (mg/dL for Glucose/Urea Nitrogen/Calcium, mmol/L for
   Sodium/Potassium/Chloride/Carbon Dioxide), the SAME "plausibility, not
   diagnostic precision" bar the table's own existing three rows already
   set — NOT independently source-verified beyond the LOINC code itself,
   disclosed the same way. Table grows by evidence (this closure's own
   real need), not speculation.

**Encounter-derivation wrinkle:** unlike Wave B's own UTI survey note
(section 9), THIS closure's own three path submodules
(`telemed_path`/`ambulatory_path`/`ed_path`) still carry every
`Encounter`/`EncounterEnd` pair themselves — confirmed unchanged,
exercised by this session's own vendored test (H6's own cross-boundary
encounter-event assertion, deferred at Wave B, closed here).

**Declared D3 vendoring scope for UTI (H5): BUILDABLE, pending H2's own
mechanism landing plus the four mechanical findings above** —
`lookup_table_transition` is the closure's only remaining gap once
`gmf_version 2`'s translation lands; every other prior blocker
(`CallSubmodule`, `DiagnosticReport`/`MultiObservation`, `type_of_care_
transition`) is already v1. Scope is the FULL twelve-file closure plus
its two lookup tables — no branch dropped.

### D3d — TJR fetch re-verification and new findings (H5)

`total_joint_replacement.json`'s own 4-file closure re-fetched fresh
this session (not trusted from D2's own prose — D2 never vendored it,
so no prior hash existed to diff against; this session's own fetch is
the first hash-anchored record). D2's own characterization (section 13)
holds exactly: 4 files, `CarePlanStart`/`CarePlanEnd` the only Wave-D-
scoped gap, D7 clean, `joint_replacement` delegated (`initial-
attributes`, already landed D2).

| File | States | SHA-256 |
|---|---:|---|
| `total_joint_replacement.json` (root) | 33 | `1666dfc39a3a2266ccc8c5937a0de317a4330489fb315e36b9d55c6dd6bb3d8b` |
| `medications/moderate_opioid_pain_reliever.json` | 12 | `e48546e9250ec9d76408d3aa76898d3f30b7e49ee81bed692dc830acb4a4efba` |
| `total_joint_replacement/functional_status_assessments.json` | 20 | `7fd8e6f27f75718628529448d078c87ddf0849b19014d4e4f854935a97c5fe6c` |
| `dme/wheelchair_end.json` | 4 | `e24ea11f2c9cdf830e06ce10eee411368194ab15890a1e0dec8ab5939be1aea9` |

Root and `functional_status_assessments.json` carry `gmf_version: 1`;
`moderate_opioid_pain_reliever.json` carries `gmf_version: 1`;
`wheelchair_end.json` carries `gmf_version: 2` but exercises none of
its new fields (confirmed by direct scan) — TJR's own exercised paths
need NO gmf_version-2 translation, unlike UTI's.

**Two new, previously-uncharacterized field-level findings (D2's own
state-TYPE census could not have caught either), byte-confirmed against
the fresh fetch:**

1. **`SetAttribute`'s own `value_code` field** (`Pre_Procedure_Encounter_
   Reason` sets `pre_procedure_encounter_reason`; `Home Health Reason
   Knee`/`Home Health Reason Hip` set `home_health_reason`, all three via
   `value_code`, never `value`) — unbuilt (`GmfState`'s own `:set-
   attribute` schema variant declares only `:value {:optional true}
   :any`; the extra key is silently tolerated by Malli's open-by-default
   `:map`, so the module still LOADS, but `:set-attribute`'s own
   interpreter case reads only `(:value state)`, so BOTH attributes would
   write `nil` today). **Confirmed harmless for THIS closure regardless**
   (direct read, all four files): `pre_procedure_encounter_reason` feeds
   only an `Encounter` state's own `:reason` field, which `compile-
   trajectory`'s own `encounter->step` never reads (already-dead JSON,
   the same disposition `:reason`/`number_of_observations` already have);
   `home_health_reason` is written twice and read NOWHERE in this
   closure's own four files. Building it anyway (cheap, mirrors `:value-
   code`'s own already-built `:observation`/`ObservationEntry` precedent
   exactly, avoids a silently-wrong `nil` for any FUTURE consumer) is
   this session's own choice, not a load-bearing requirement of TJR's own
   vendoring.
2. **An embedded `MultiObservation` child's own `exact` value-sourcing
   mechanism** (`functional_status_assessments.json`'s own `PROMIS29_
   Total_Assessment`, one child: `"exact": {"quantity": 1}`, no unit) —
   D1a-3 characterized THREE value-sourcing mechanisms (`range`/
   `value_code`/`vital_sign`); `exact` is a FOURTH, not previously seen
   on any embedded child. `ObservationChild`'s own schema has no `:exact`
   field at all — Malli's open-by-default `:map` tolerates the extra
   key, so the module still loads, but `sample-observation-extra`'s own
   `cond` chain falls to its `:else {:codes codes}` branch, SILENTLY
   dropping the value (not a load failure — a wrong-answer risk).
   **Load-bearing for TJR: `PROMIS29_Total_Assessment` is reached
   whenever `Perform_Functional_Status_Assessment_Hip`'s own `assessment_
   done` branch selects `"PROMIS-29"` (one of five `distributed_
   transition`-free, i.e. author-selected, arms) — a real, reachable
   path, not an excludable tail.** Built this session: `exact` is
   SPECIFIED content (a literal, `quantity: 1` here) — the SAME
   "specify, don't fabricate" governing principle D1a/D2 already applied
   to `stroke_risk`/`joint_replacement`'s own opposite case (DELEGATED
   content) makes this the easy branch, zero rng, mirroring `Delay`'s
   own `:exact` handling exactly: `sample-observation-extra` gains an
   `:exact` branch, `{:codes codes :value (:quantity exact) :unit (:unit
   state)}`, checked ahead of the existing `:else` fallback.

**Declared D3 vendoring scope for TJR (H5): BUILDABLE, pending H4's own
compound-Guard resolution landing plus the two findings above** — every
other D2-era finding stands unchanged (D7 clean, `joint_replacement`
delegated and already resolved).

### D3e — compound-Guard analytical resolution (H4)

`Joint_Replacement_Guard`'s own exact condition, re-confirmed byte-for-
byte against the fresh fetch (D3d): `{:and [{:condition-type :attribute
:attribute "joint_replacement" :operator "is not nil"} {:condition-type
:age :operator ">" :quantity 50 :unit "years"}]}`. `age-guard-jump-days`'s
existing v1 boundary (bare `:age` condition, operator `>=` only) does not
recognize this shape at all — neither the `:and` wrapper nor the strict
`>` operator.

**Design (H1, sound-jump-or-escalate, no Synthea source citation applies
here — real Synthea resolves this via a ticked simulation clock this
project's own no-fixed-tick design (section 3) deliberately does not
have; this is this project's OWN interpreter-core analytical extension,
not a ported mechanism):**

1. **Bare `:age` conditions gain the strict `>` operator**, alongside
   the existing `>=`: the exact day-vs-year integer-age-flooring
   boundary (`age-years-at`'s own `Period/between... .getYears()`,
   floored whole years) means `age > N` first becomes true on the SAME
   day `age >= (N+1)` first becomes true (a floored age of exactly `N`
   does not satisfy strict `>`; the jump target for `>` is therefore
   `dob.plusYears(N+1)`, computed by the SAME `age-guard-jump-days`
   arithmetic already used for `>=`, `quantity` bumped by one). Other
   operators (`<`, `<=`, `==`) stay OUT — a failing `<`/`<=`/`==` age
   condition is already true-then-permanently-false or a point condition
   the passage of time cannot make MORE true, so no sound forward jump
   exists (correctly blocks, unchanged, the SAME disposition a non-`>=`
   bare condition already has today).
2. **A compound `:and` condition may be jumped when it contains EXACTLY
   ONE `:age` sub-condition (operator `>=` or `>`, per (1)) and every
   OTHER sibling is a condition type that does not itself read `ctx`'s
   own `:t` (i.e. not `:age`/`:date` — the only two v1 condition types
   whose truth value can change merely from the passage of time) AND
   already holds, evaluated against the CURRENT ctx, before the jump.**
   Soundness: the ONLY thing time can change between now and the jump
   target is the age sub-condition itself (by construction, the jump
   advances `:t` alone — no attribute is cleared, no trajectory event is
   un-emitted, and a non-`:age`/`:date` sibling's truth value is a pure
   function of persona/attributes/trajectory, none of which the jump
   touches) — so a sibling true now is still true at the jump target,
   and the age sub-condition is true at the jump target BY the same
   construction `age-guard-jump-days` already proves for the bare case.
   `Joint_Replacement_Guard`'s own two siblings satisfy this exactly:
   `Attribute joint_replacement is-not-nil` (seeded via `initial-
   attributes` before the walk starts, never cleared anywhere in TJR's
   own closure, D2's own D7 scan) holds from `:t` = DOB onward; `Age >
   50 years` is the sole blocking condition. `guard-step`'s own existing
   "no second, still-blocked branch" comment (trust-by-construction,
   never a redundant re-check) extends unchanged to the compound case —
   the proof above is what licenses that trust, not a new runtime check.
3. **Named, NOT built (no sound bound, or genuinely not exercised by
   TJR) — an ESCALATION shape, not a heuristic jump, per H4's own
   instruction:** more than one `:age` sub-condition in one `:and`
   (ambiguous which bound governs, unexercised); an `:and` containing
   `:date` alongside `:age` (two time-dependent siblings interacting,
   unexercised); `:or`/`:at-least` wrapping an `:age` condition (a
   different logical combinator, unexercised); any sibling that does
   NOT already hold at evaluation time (correctly stays BLOCKED — no
   sound jump exists merely from advancing `:t`, unchanged from today).
   Installed ≠ used (H4's own words): only the form TJR actually
   exercises is built.

---

## 15. GMF census (2026-08-03, ADR-0034): the frontier converted to data

`.agents/plans/2026-08-02-gmf-parity-plan.md` §3, ADR-0031 AR-1/AR-4:
the census tool (`ehrt.patient-simulator.census`, a `patient-simulator` dev
entry point under `development/src`, not a CLI verb) walks and
smoke-digests the FULL upstream catalog at this document's own pin —
superseding the hand-scouted prioritization table above (its own
superseded note, §8) as the frontier of record. Full artifact:
[`components/patient-simulator/docs/census/2026-08-03-synthea-7e08387.edn`](census/2026-08-03-synthea-7e08387.edn).

**Run parameters (re-runnable to the byte, all recorded in the
artifact's own `:header`):** pin `7e08387c68a7f0e21d13076609a159fd473fc902`,
verified via `git rev-parse HEAD` against the checkout (not the
sha256-content fallback — a real git checkout was available); 85
top-level modules discovered; 3 seeds/module, mixer-seed `20260803`
(the same `java.util.Random`-mixer derivation
`bin/oracle-src/ehrt/oracle/digest.clj` uses); registration at age 30,
horizon 50 further years (age 30→80), uniform persona sampling
(`{}` config) — one fixed, global choice, not tuned per module.

### Verdict counts

| Verdict | Count |
|---|---:|
| `:ok-walked` | 40 |
| `:load-failed` | 39 |
| `:walk-failed` | 6 |
| `:out-of-scope-by-ruling` | 0 (the category stays reserved, ADR-0031 AR-4 — empty is fine) |
| **Total** | **85** |

19 of 85 modules carry AR-3's `:disclosed-substitutions [:wellness-timing]`
tag — the mechanical `wellness: true`/no-`encounter_class` scan, applied
regardless of verdict. This is nearly 4× ADR-0031 AR-5(a)'s own hand-
survey count of five (`mTBI`, `atrial_fibrillation`, `osteoporosis`,
`epilepsy`, `med_rec`) — all five are among the 19 (confirmed
individually), so AR-5(a)'s finding stands, but was itself an
undercount by exactly the mechanism the parity plan's §3 predicted:
a scouted sample missing real instances a full mechanical sweep finds.
Of the 19: 12 `:ok-walked` (including the already-vendored
`ear_infections`, whose own timing-substitution disclosure is
`gmf.clj`'s own `normalize-state` docstring note, ADR-0031 AR-5(b)),
1 `:walk-failed` (`med_rec`, a max-steps runaway — see below), 6
`:load-failed` (a different, unrelated gap blocks each before the
wellness idiom itself would ever run).

### Sanity anchors (Step 2's own STOP-AND-ESCALATE gate)

**All SEVEN currently-vendored roots census `:ok-walked`** —
`appendicitis`, `ear-infections`, `sepsis`, `sinusitis`, `sore-throat`,
`total-joint-replacement`, `urinary-tract-infections`. (This session's
own driving prompt named "eight" vendored roots; the actual count, both
by direct listing of `components/sim/resources/sim/modules/*.json` and
by this document's own D3f regression-baseline prose above, is SEVEN —
a small premise correction, disclosed here and in the session record,
not blocking: no ruling anywhere in `notes/ADRs.md` or the parity plan
ever said eight.) No STOP-AND-ESCALATE fired.

**All five of ADR-0031 AR-5(a)'s named wellness modules carry the
AR-3 tag**, confirming the anchor: `epilepsy`/`mTBI`/
`atrial_fibrillation`/`osteoporosis` census `:ok-walked` (with the
tag), `med_rec` census `:walk-failed` (with the tag — AR-3 is
verdict-independent by design).

### Top gap mechanisms (`:load-failed`, by modules blocked)

| Mechanism | Modules blocked |
|---|---:|
| `Counter` (deferred state type) | 11 |
| `ImagingStudy` (deferred state type) | 10 |
| `gmf_version 2` timing, `EXPONENTIAL` distribution kind (loader THROWS — new finding, below) | 7 |
| `gmf_version 2` timing, `GAUSSIAN` distribution kind (loader THROWS — new finding, below) | 4 |
| `SupplyList` (deferred state type) | 3 |
| `AllergyOnset` / `VitalSign` / `Vaccine` (deferred state types) | 1 each |
| Unrecognized lookup-table column (`time`, real Synthea's own `LookupTableTransition` special case, D3a's own "unbuilt" note) | 1 |

**New finding, this session: `ehrt.patient-simulator.gmf`'s own
`gmf-v2-timing->v1` (D3c finding 1's own translation function) is not
exception-free.** Its `case` over a `gmf_version 2` distribution `:kind`
has clauses for `UNIFORM`/`EXACT` only; a real `GAUSSIAN` or
`EXPONENTIAL` kind (11 modules combined) throws a raw
`IllegalArgumentException` at load time rather than a `:rejected`
Result — a genuine loader robustness gap the census's own full-catalog
sweep surfaced (no hand survey ever read enough `gmf_version 2` modules
to find it). Per this session's own fence, NOT fixed here (`gmf.clj` is
the thing being observed, not touched); `census.clj`'s own `census-one`
wraps `load-closure` in `try`/`catch` so this one finding does not abort
the whole run (`ehrt.patient-simulator.census`'s own docstring on
`census-one` has the full account). Named for a future defect-fix or
Wave I session: extending `gmf-v2-timing->v1`'s vocabulary is a small,
mechanical, high-leverage fix (11 modules, tied for the single largest
census bucket after `Counter`/`ImagingStudy`).

### `:walk-failed` mechanisms

| Mechanism | Modules |
|---|---|
| Unrecognized condition type `Race` (real Synthea condition type, ADR-0031's §2 vocabulary never named it) | `anemia-unknown-etiology`, `cystic-fibrosis`, `self-harm` |
| Unrecognized condition type `Not` (boolean negation — real Synthea's `Logic.java` `Not` class, never in this project's v1 compound-wrapper set alongside `And`/`Or`/`At Least`) | `allergic-rhinitis` |
| `max-steps` runaway (a real zero-time-advance transition cycle this interpreter's own backstop catches, per its own docstring — either a module-authoring idiom this interpreter doesn't yet resolve, or a genuine authoring bug upstream) | `med-rec`, `veteran-substance-abuse-treatment` |

Two NEW condition-vocabulary gaps (`Race`, `Not`) neither §2 nor any
prior wave named — real, mechanically found, each blocking 1–3 modules;
smaller than the state-type gaps above but genuinely new information
this table did not have before.

### Reading this census

Zero `:load-failed` (minus `:out-of-scope-by-ruling`, currently empty)
is parity's own countable definition (parity plan §1/§3). This run: 39
non-out-of-scope `:load-failed` plus 6 `:walk-failed` stand between here
and that line — the ranked mechanisms above are the E/F/G/H/I sequencing
input the parity plan's own §4 deferred to "the census ranking," a
design-channel read this session does not make (fence, below).

### D3f — regression baseline (Step 1)

Fixed-seed walks of all SEVEN currently-vendored roots (`sinusitis`,
`appendicitis`, `sore_throat`, `ear_infections`-closure, `sepsis`,
`death-fixture`, plus the CarePlan mechanism's own fixture coverage) —
`poly check` clean, full non-integration suite green, captured at this
session's own pre-Step-2 HEAD, before any D3 code change; re-run and
diffed at every subsequent checkpoint per this document's own established
method, byte-identical/count-identical at every one (confirmed one final
time at session close, Step 4).

### Census re-run (2026-08-03, ADR-0035 AR-7): the `gmf_version 2`
loader-exception class closes

Wave F0 (ADR-0035) ports GAUSSIAN/EXPONENTIAL/TRIANGULAR into the loader
and interpreter, structurally closing the `gmf_version 2` loader-
exception finding this section's own "New finding" paragraph (above)
named but did not fix. Re-run with the SAME header parameters (pin
`7e08387c68a7f0e21d13076609a159fd473fc902`, 3 seeds/module, mixer-seed
`20260803`, registration age 30, 50-year horizon, `{}` persona config) —
new artifact:
[`components/patient-simulator/docs/census/2026-08-03-synthea-7e08387-wave-f0.edn`](census/2026-08-03-synthea-7e08387-wave-f0.edn),
committed alongside the original rather than overwriting it (a same-
calendar-day re-run — see the tooling-gap disclosure below).

**Verdict counts, before → after:**

| Verdict | Before | After | Δ |
|---|---:|---:|---:|
| `:ok-walked` | 40 | 42 | +2 |
| `:load-failed` | 39 | 34 | −5 |
| `:walk-failed` | 6 | 9 | +3 |
| `:out-of-scope-by-ruling` | 0 | 0 | 0 |
| **Total** | **85** | **85** | 0 |

**Movement classification (AR-7).** All 11 modules the original
`gmf_version 2` loader-exception finding named were traced individually,
byte-confirmed against both census artifacts:

- **Resolved to `:ok-walked`** (2): `copd`, `opioid-addiction`.
- **Surfaced their NEXT blocker, now `:walk-failed`** (3):
  `contraceptives` and `dementia` (an unsupported condition type — the
  same `Race`/`Not` gap class this section's own `:walk-failed`
  mechanisms table already names), `wellness-encounters` (an
  unrecognized vital-sign name, `patient-simulator/vital-signs.edn`'s own
  curated-table boundary).
- **Stayed `:load-failed`, on a genuinely DIFFERENT gap** (6):
  `acute-myeloid-leukemia` (an unrecognized lookup-table column,
  `race` — H2's own `recognized-lookup-table-columns` boundary, a new
  finding this census run surfaced; **RESOLVED 2026-08-03, GMF coverage
  Wave LC, ADR-0038 — the boundary itself retired, this module now
  censuses `:ok-walked`**), `bone-marrow-transplant`/
  `colorectal-cancer`/`pregnancy` (`Counter`), `dental-and-oral-
  examination`/`metabolic-syndrome-care` (`SupplyList`) — each blocked
  by an EARLIER state in the module's own JSON key order than the
  distribution content, so the loader's deterministic first-found
  short-circuit (`normalize-states`' own docstring) never reached the
  now-fixed gap for these six at all; the fix did not regress them, it
  simply never touched them.

Net arithmetic: 11 moved out of the loader-exception category (2 + 3 +
6, all traced above); 6 of those 6 land BACK in `:load-failed` for an
unrelated reason, so the net `:load-failed` delta is −5, not −11 — the
6-module offset the raw verdict-count table alone could not explain
without this trace.

**SetAttribute digest movement (AR-4/AR-7): zero, and why.** AR-7
anticipated previously-`:ok-walked` modules with SetAttribute
distributions changing walk digests now that they sample real values.
Empirically: **zero** `:ok-walked`-in-both-runs module changed digest
(every one of the 40 modules `:ok-walked` in both censuses byte-matches
across all 3 seeds). Traced to source: `hypertension.json` — the
module this session's own driving prompt cited by name
(`Black_Onset_Age`'s GAUSSIAN onset-age SetAttribute) — census
`:load-failed` in BOTH runs, blocked by `Counter`, a state earlier in
its own JSON key order than `Black_Onset_Age`. The SetAttribute fix is
real and tested directly (`gmf_interpreter_test.clj`'s own
`set-attribute-gaussian-*` tests, `ADR-0035` AR-4) — this census's own
85-module top-level scope simply does not currently walk far enough
into any module that exercises it, an honest negative result, not a
gap in the fix.

**Sanity anchors held.** All SEVEN currently-vendored roots stayed
`:ok-walked` with byte-identical digests across both censuses (matching
Step 4's own oracle-bracket verdict, above); no module outside the 11
traced above moved at all.

**Tooling gap, disclosed not fixed:** the census tool's own artifact
filename (`<census-date>-synthea-<pin7>.edn`,
`ehrt.patient-simulator.census/-main`) has no same-calendar-day
disambiguation — a second run on the SAME date as a prior one collides
on the SAME path and silently overwrites it (found live, this session:
the first re-run attempt overwrote the original artifact before this
disclosure caught it via `git status`, restored from git before commit).
Worked around by hand-appending a `-wave-f0` suffix to this run's own
filename rather than the tool's own naming scheme; a same-day-safe
naming scheme (e.g. a run-sequence suffix or a full timestamp) is named
here for a future session, not built — out of this session's own
loader-plus-interpreter fence.

### Census re-run (2026-08-03, ADR-0036 AR-8): Counter/ImagingStudy/
SupplyList/condition-rider class closes

Wave F (ADR-0036) lands `Counter`/`ImagingStudy`/`SupplyList` plus the
`Not`/`Race`/`Socioeconomic Status` condition rider, closing the three
largest single-mechanism gaps this section's own F0 subsection (above)
left standing (`Counter` 14, `ImagingStudy` 10, `SupplyList` 5) and the
two condition-vocabulary gaps its own `:walk-failed` mechanisms table
named (`Race` 3 modules, `Not` 1 module). Re-run with the SAME header
parameters (pin, 3 seeds/module, mixer-seed `20260803`, registration
age 30, 50-year horizon) plus AR-8's own disclosed persona-config
delta — fixed, equal-weighted `:race-weights`/`:socioeconomic-weights`
pools (Synthea's own closed Race/SocioeconomicStatus vocabularies,
Logic.java-grounded), so the walks actually exercise the new guards.
New artifact:
[`components/patient-simulator/docs/census/2026-08-03-synthea-7e08387-wave-f.edn`](census/2026-08-03-synthea-7e08387-wave-f.edn),
committed alongside both prior artifacts (never overwriting), same
filename-disambiguation workaround F0 already used.

**Verdict counts, before → after:**

| Verdict | Before | After | Δ |
|---|---:|---:|---:|
| `:ok-walked` | 42 | 60 | +18 |
| `:load-failed` | 34 | 18 | −16 |
| `:walk-failed` | 9 | 7 | −2 |
| `:out-of-scope-by-ruling` | 0 | 0 | 0 |
| **Total** | **85** | **85** | 0 |

`Counter`/`ImagingStudy`/`SupplyList` are GONE from the top-gap-
mechanisms table entirely (were 14/10/5); the remaining mechanisms are
`VitalSign` ×2, `AllergyOnset` ×1, `Physiology` ×1, `Vaccine` ×1.

**Movement classification (AR-8), every one of the 20 verdict changes
traced individually, byte-confirmed against both artifacts:**

- **`Counter`-blocked, resolved fully to `:ok-walked`** (10):
  `bone-marrow-transplant`, `breast-cancer`, `colorectal-cancer`,
  `homelessness`, `hypertension`, `lung-cancer`,
  `metabolic-syndrome-disease`, `pregnancy`,
  `prescribing-opioids-for-chronic-pain-and-treatment-of-oud`,
  `veteran-lung-cancer`.
- **`Counter`-blocked, surfaced a NEXT blocker** (1): `mend-program` →
  `:walk-failed` (a `max-steps` runaway — a real zero-time-advance
  transition cycle, the same wellness-cycle-adjacent substitution
  artifact class the parity plan's own G row already names for
  `med-rec`/`veteran-substance-abuse-treatment`, now a third instance).
- **`SupplyList`-blocked, resolved fully to `:ok-walked`** (4):
  `dental-and-oral-examination`, `dentures`, `kidney-transplant`,
  `sleep-apnea`.
- **`SupplyList`-blocked, surfaced a NEXT blocker** (1):
  `metabolic-syndrome-care` → `:walk-failed` (the same `max-steps`
  mechanism as `mend-program`, above).
- **`ImagingStudy`-blocked, ALL 10 surfaced a next blocker, ZERO
  resolved fully and ZERO regressed** — `ImagingStudy` was never the
  ONLY gap on any of its 10 F0-blocked modules: `congestive-heart-
  failure` → `VitalSign` (ADR-0036 AR-7's own explicit deferral),
  `gallstones` → `Physiology` (a genuinely new, out-of-scope deferred
  type), seven modules (`diabetic-retinopathy-treatment`,
  `myocardial-infarction`, `stable-ischemic-heart-disease`,
  `vhd-aortic`, `vhd-mitral`, `vhd-pulmonic`, `vhd-tricuspid`) → each
  its own distinct unrecognized lookup-table column (`diabetic_retinopathy_stage`, `state`, `operative_status`,
  `cardiac_surgery`, `vhd_mr_risk`, `vhd_ps_risk`, `vhd_tr_risk` — H2's
  own `recognized-lookup-table-columns` boundary, AR-7's own deferred
  Wave-I item; **RESOLVED 2026-08-03, GMF coverage Wave LC, ADR-0038,
  pulled forward from Wave I — the boundary itself retired, all seven
  modules now census `:ok-walked`**), and `injuries` → a
  `:schema-invalid` rejection on a
  PRE-EXISTING, unrelated gap: a `complex_transition` entry's own
  nested `:distributions` carrying a NamedDistribution map
  (`{:attribute :default}`), which `TransitionFields`'s own
  `:complex-transition` schema declares `:distribution number?` only
  (D3b/H3's own documented scope: `distributed_transition` gained
  NamedDistribution resolution, `complex_transition`'s nested form did
  not — `resolve-transition`'s own docstring already names this
  exact gap as "no candidate module this session exercises a
  NamedDistribution there," now confirmed a real, if still out-of-
  scope, instance).
- **`Race`/`Not`-blocked (the `:walk-failed` mechanisms table above),
  ALL 4 resolved to `:ok-walked`**: `allergic-rhinitis` (`Not`),
  `cystic-fibrosis`, `dementia`, `self-harm` (`Race`).

Net arithmetic: 20 verdict changes total (10+1+4+1+4 = 20, all traced
above); the raw −16 `:load-failed` delta is NOT simply
`14+10+5=29` modules moving, because `ImagingStudy` never resolved a
module on its own and several `Counter`/`SupplyList` modules carried
more than one blocker — the same "fail-fast masking" the F0 subsection
above already documented, now demonstrated a second time.

**Sanity anchors held.** All seven currently-vendored roots stayed
`:ok-walked`, byte-identical across all three census artifacts
(F0/original and this run), matching the AR-6 oracle bracket above; no
module outside the 20 traced moved at all.

**Substance note (AR-8b): walk-verification attests determinism, not
richness, for a large slice of this catalog.** Of the 42 modules
`:ok-walked` in the PRE-Wave-F census, **26 produce zero trajectory
events on every one of their 3 smoke-walk seeds** — an immediate-
terminal on an absent persona attribute, a cross-module attribute
block, or an empty horizon-complete (byte-confirmed by direct query
against the F0 artifact's own `:walks` data): `ais-from-school-
screening-to-sosort-recommendations`, `atopy`, `atrial-fibrillation`,
`cerebral-palsy`, `chronic-kidney-disease`, `contraceptive-
maintenance`, `copd`, `dialysis`, `epilepsy`, `female-reproduction`,
`food-allergies`, `gout`, `lupus`, `mtbi`, `opioid-addiction`,
`sexual-activity`, `spina-bifida`, **`stroke`**, `total-joint-
replacement`, `trigger-bone-marrow-transplant`, `veteran`,
`veteran-mdd`, `veteran-prostate-cancer`, `veteran-ptsd`,
`veteran-self-harm`, `veteran-substance-abuse-conditions`. `stroke`'s
own presence on this list is a load-bearing confirmation of §10's
already-ratified E-rescoping account (`stroke_risk` falls back to its
JSON-declared `:default`, never sourced — the walk completes, but
touches no real content). This is not a Wave F finding about Wave F's
own additions — it is a standing property of walk-verification itself
(the census's own AR-2 definition: "loads AND every smoke-walk seed
completed without throwing," never a claim about event RICHNESS) that
Wave F's own re-run makes newly countable for the first time: for the
gated chronic-disease cluster specifically, `:ok-walked` currently
means "deterministically produces nothing," for over 60% of that
cluster's pre-F membership. Named for whichever future session ranks
Wave G/H's own priority — not a defect in this session's own scope,
a property of the frontier this session's own re-run happened to make
visible.

### Census re-run (2026-08-03, ADR-0037 AR-8): wellness cycle lands --
substitution retired, four loop modules resolve, Physiology out-of-scope

Wave G (ADR-0037) retires the create-now wellness substitution (AR-3)
and lands the real cadence-anchored wait (AR-1/AR-2), the design gap
this document's own §4 dated note and this section's own AR-3's
"5 confirmed... likely still under-counted" wellness row named since
ADR-0031. Re-run with the SAME header parameters as Wave F (pin, 3
seeds/module, mixer-seed `20260803`, registration age 30, 50-year
horizon, the same disclosed persona-config delta) — no NEW header
parameter this wave, since AR-2's own schedule function draws zero rng
and needs no config of its own. New artifact:
[`components/patient-simulator/docs/census/2026-08-03-synthea-7e08387-wave-g.edn`](census/2026-08-03-synthea-7e08387-wave-g.edn),
committed alongside all three prior artifacts (never overwriting), same
filename-disambiguation workaround F0 first used.

**Verdict counts, before → after:**

| Verdict | Before | After | Δ |
|---|---:|---:|---:|
| `:ok-walked` | 60 | 64 | +4 |
| `:load-failed` | 18 | 17 | −1 |
| `:walk-failed` | 7 | 3 | −4 |
| `:out-of-scope-by-ruling` | 0 | 1 | +1 |
| **Total** | **85** | **85** | 0 |

`Physiology` drops off the top-gap-mechanisms table entirely (was 1,
`gallstones`' own blocker) — reclassified, not resolved; the remaining
mechanisms are `VitalSign` ×2, `AllergyOnset` ×1, `Vaccine` ×1.

**Movement classification (AR-8), every verdict change and every
digest change among the 19 formerly `:wellness-timing`-tagged modules
traced individually, byte-confirmed against both artifacts:**

- **The four real upstream loop modules this Wave unblocks, ALL
  resolved fully to `:ok-walked`**: `med-rec`, `mend-program`,
  `metabolic-syndrome-care`, `veteran-substance-abuse-treatment` — each
  was the exact `max-steps` "zero-time-advance transition cycle"
  signature the retired substitution's own zero-advance wellness
  encounter produced (`mend-program`/`metabolic-syndrome-care` first
  surfaced this class at Wave F, `docs/gmf-interpreter.md`'s own AR-8
  account there; `med-rec`/`veteran-substance-abuse-treatment` were
  named in the parity plan's own unlock ledger from the start, ADR-0031
  AR-5(a)). Verified directly (not merely by census verdict): a
  standalone trace of `med_rec.json` through `run-module` at the
  census's own seed/registration/horizon parameters now completes at
  `:horizon-complete` with 269 real trajectory events, where it
  previously threw at `max-steps` inside its own `Wellness_Encounter`
  state.
- **`gallstones` reclassifies `:load-failed` → `:out-of-scope-by-
  ruling`** (AR-5): its own sole load gap, the `Physiology` state type,
  is now the census's first ruled exclusion, not a load gap still to
  close.
- **7 of the 19 formerly-tagged modules stay `:ok-walked` in both
  runs, but their own walk digest CHANGES** (a wait now times the
  encounter differently than the retired immediate-fire substitution
  did — expected, per AR-8's own prediction): `asthma`, `bronchitis`,
  `dementia`, `ear-infections`, `osteoporosis`, `sleep-apnea`,
  `veteran-hyperlipidemia`.
- **8 of the 19 formerly-tagged modules show NO observable difference**
  (still `:ok-walked`, byte-identical digest across both runs):
  `atrial-fibrillation`, `copd`, `epilepsy`, `hypertension`, `mtbi`,
  `stable-ischemic-heart-disease`, `veteran-prostate-cancer`,
  `wellness-encounters` — their own particular 3 seeds/50-year horizon
  never happen to cross the wellness-wait path differently between the
  two timing mechanisms (a module-specific gating fact, not a gap in
  the fix; `wellness-encounters` itself stays `:walk-failed` in BOTH
  runs regardless, for a wholly unrelated, pre-existing reason — see
  below).
- **No module OUTSIDE these 19 changed verdict or digest at all** —
  every one of the other 66 modules is byte-identical across both
  artifacts, confirmed by a full per-module diff, not merely a verdict-
  count comparison.

Net arithmetic: 5 verdict changes (4 loop modules + `gallstones`) plus
7 digest-only changes = 12 of the 19 formerly-tagged modules show SOME
observable movement; the remaining 7 stay entirely unmoved.

**Two `:walk-failed` modules stayed `:walk-failed`, for reasons wholly
unrelated to this Wave's own fence** (disclosed, not a regression):
`anemia-unknown-etiology` ("Observation condition has no matching prior
observation," a pre-existing observation-linkage gap) and `wellness-
encounters` ("unrecognized vital-sign name -- not in patient-simulator/
vital-signs.edn", `"Height"` — a vital-sign reference-table gap,
`docs/gmf-interpreter.md` section 11's own `vital_sign` line, unrelated
to the wellness CYCLE this Wave lands despite the module's own name).
Both errors are byte-identical to the Wave F census's own record for
the same two modules — genuinely untouched by this Wave, not silently
re-caused.

**Sanity anchors held.** All seven currently-vendored roots stayed
`:ok-walked` across every census artifact to date, matching the AR-6
oracle bracket above (`ear-infections`' own digest changes there are
the SAME timing change this census's own movement classification
names, not a second, independent divergence).

**Chronic-meds cadence cap: deferred, not implemented** (AR-1's own
ruling) — recorded as a named register item ("wellness cadence
chronic-meds cap"), not built this session.

**A live finding, disclosed (`notes/ADRs.md` ADR-0037's own deviation
record has the full account): `next-wellness-tick`'s own boundary
semantics were REFINED mid-session** from an inclusive "first tick >= t"
to a strict "first tick > t" after this census's own FIRST run (against
the inclusive version) showed `med-rec`/`mend-program`/`metabolic-
syndrome-care`/`veteran-substance-abuse-treatment` STILL `:walk-failed`
at `max-steps` — the real modules' own zero-delay wellness-wait loops
hit exactly the boundary case the inclusive design didn't cover. Fixed,
re-verified (the oracle bracket above and this section's own numbers
are the POST-fix, correct run), and its own dedicated commit's message
carries the full account."

### Census re-run (2026-08-04, ADR-0041 AR-4): PARITY ACHIEVED

Wave I2 (ADR-0041) closes the tail Wave I's own six-mechanism landing
left unmasked (ADR-0040 AR-7): `congestive-heart-failure`'s Death
states (`:condition-onset`/`:referenced-by-attribute` cause forms) and
`wellness-encounters`' `:active-careplan` condition. Re-run with the
SAME header parameters every wave since Wave F has used. New artifact:
[`components/patient-simulator/docs/census/2026-08-04-synthea-7e08387-wave-i2.edn`](census/2026-08-04-synthea-7e08387-wave-i2.edn).

| Verdict | Post-I | Post-I2 |
|---|---:|---:|
| `:ok-walked` | 82 | **84** |
| `:out-of-scope-by-ruling` | 1 | 1 |
| `:walk-failed` | 2 | **0** |
| `:load-failed` | 0 | 0 |
| **Total** | **85** | **85** |

Both `congestive-heart-failure` and `wellness-encounters` move to
`:ok-walked` (`:walk-errors []`); no other module's own verdict or
digest shifted — confirmed by direct comparison against the post-I
artifact, not merely a count match. Zero `:load-failed` and zero
`:walk-failed`, with every `:ok-walked` module's own smoke-walk digest
recorded: **this is parity plan §1/§3's own countable definition, MET.
PARITY ACHIEVED, at pin `7e08387c68a7f0e21d13076609a159fd473fc902`,
2026-08-04.** `notes/ADRs.md` ADR-0041 has the full mechanism account;
`.agents/plans/roadmap.md` retires this row and names Wave H the sole
remaining wave.

### Census re-run (2026-08-07, ADR-0069 AR-VC-4): the substance qualifier lands, the catalog ranked for curation

The vendoring arc opens (ratified `notes/adr/0066-player-fold.md`
AR-BB1-R, sequenced per `notes/adr/0068-player-arc-close.md`'s own
horizon note) with the census substance session: roadmap "Census tool
refinements" items (a) and (c) close (`ehrt.patient-simulator.census`'s own
`census-one`/`summarize`/`artifact-filename`, ADR-0069). Re-run with the
SAME header parameters every wave since Wave F has used (pin, 3
seeds/module, mixer-seed `20260803`, registration age 30, 50-year
horizon, the same disclosed race/socioeconomic/state persona-config
delta) — no NEW header parameter this session, since the substance
qualifier is derived entirely from each walk's own already-recorded
`:event-count`, no new sampling. New artifact, its own filename
DEMONSTRATING item (c)'s own fix (a real `-substance` label, not a
hand-appended wave suffix):
[`components/patient-simulator/docs/census/2026-08-07-synthea-7e08387-substance.edn`](census/2026-08-07-synthea-7e08387-substance.edn).

**Verdict counts, unchanged from Wave I2 — PARITY HELD, zero movement:**

| Verdict | Post-I2 | This run | Δ |
|---|---:|---:|---:|
| `:ok-walked` | 84 | 84 | 0 |
| `:out-of-scope-by-ruling` | 1 | 1 | 0 |
| `:walk-failed` | 0 | 0 | 0 |
| `:load-failed` | 0 | 0 | 0 |
| **Total** | **85** | **85** | **0** |

Every one of the 85 modules' own verdict AND every `:ok-walked`
module's own per-seed digest were compared directly against the
`2026-08-04-synthea-7e08387-wave-i2.edn` artifact, module by module —
zero verdict diffs, zero digest diffs. No STOP-AND-ESCALATE (AR-VC-4's
own gate): this run is evidence, not a regression.

**Substance tally (AR-VC-2, `summarize`'s new `:ok-walked-by-substance`
key): of the 84 `:ok-walked` modules, 51 produce zero trajectory events
on every one of their 3 smoke-walk seeds (`:zero-on-every-seed`), 33
produce real content (`:produces-content`)** — this run's own successor
to the pre-Wave-F "26 of 42" figure §15's own AR-8b substance note
recorded (above): the frontier has more than doubled since, and the
zero-content SHARE has grown alongside it (51/84 ≈ 61%, close to
AR-8b's own ~62% pre-Wave-F share), not shrunk — landing state types
and condition mechanisms closes LOAD gaps, it does not by itself give a
module richer content once loaded. The full `:zero-on-every-seed` list,
this run's own first-class successor to AR-8b's hand-curated 26-name
list (byte-confirmed by direct query against this artifact's own
`:modules`, not copied from the summary alone):

`acute-myeloid-leukemia`, `ais-from-school-screening-to-sosort-recommendations`,
`atopy`, `atrial-fibrillation`, `bone-marrow-transplant`, `breast-cancer`,
`cerebral-palsy`, `chronic-kidney-disease`, `contraceptive-maintenance`,
`copd`, `covid19`, `cystic-fibrosis`, `dental-and-oral-examination`,
`dentures`, `diabetic-retinopathy-treatment`, `dialysis`, `epilepsy`,
`female-reproduction`, `food-allergies`, `gout`, `hiv-care`,
`hiv-diagnosis`, `home-health-treatment`, `home-hospice-snf`,
`hospice-treatment`, `hypertension`, `kidney-transplant`, `lung-cancer`,
`lupus`, `metabolic-syndrome-disease`, `mtbi`, `myocardial-infarction`,
`opioid-addiction`, `pregnancy`,
`prescribing-opioids-for-chronic-pain-and-treatment-of-oud`, `self-harm`,
`sexual-activity`, `spina-bifida`, `stable-ischemic-heart-disease`,
`stroke`, `total-joint-replacement`, `trigger-bone-marrow-transplant`,
`veteran`, `veteran-lung-cancer`, `veteran-mdd`, `veteran-prostate-cancer`,
`veteran-ptsd`, `veteran-self-harm`, `veteran-substance-abuse-conditions`,
`vhd-aortic`, `vhd-mitral`.

`stroke`'s own presence is the same standing confirmation AR-8b already
recorded (§10's E-rescoping account: `stroke_risk` falls back to its
own JSON-declared default, never sourced). **New finding this run:
`total-joint-replacement` — one of the SEVEN currently-vendored roots —
is ALSO on this list.** Disclosed, not a regression: the vendored
module's own real content (`CarePlanStart`/`CarePlanEnd`,
`CallSubmodule` branches into pre-op/post-op content) simply never
fires under THIS census's own fixed persona/seed/horizon parameters for
any of its 3 seeds — the same "deterministic, not rich" property AR-8b
named as a standing fact about walk-verification itself, now confirmed
to reach even a shipped, vendored module. Named here for whichever
future session tunes census parameters or corpus generation for this
root specifically; not a defect in `total_joint_replacement.json`, the
census tool, or this session's own fence (no gmf/loader/interpreter
edit).

**Sanity anchors held.** All seven currently-vendored roots
(`appendicitis`, `ear-infections`, `sepsis`, `sinusitis`, `sore-throat`,
`total-joint-replacement`, `urinary-tract-infections`) stayed
`:ok-walked`, byte-identical digest across every 3 seeds, matching every
prior census artifact back through the original 2026-08-03 run.

**This artifact and this section are the curation pass's own input** —
the ranked, substance-tagged catalog the design channel's next pass
reads over (substance × closure-family × the recorded blockers: the
vital-sign trio, stroke pending Wave E's register) to produce the
batched vendoring plan named in ADR-0068's own horizon note. No
vendoring choice is made here.

## 16. GMF coverage Wave VS: the vital-sign channel (2026-08-04, ADR-0039)

The post-LC census (§15) ranked the vital-sign family as the largest
remaining frontier — `VitalSign` was §1's own last remaining Deferred-
table row (moved into the main table this Wave, §1 above). Full
ruling record: `notes/ADRs.md` ADR-0039 (AR-1 through AR-7, verbatim);
this section carries the two named session reads (AR-6) and points
back to the ADR for everything else, the same division of labor §9's
own D5 characterization note and §13's own closure survey already
establish.

**AR-6(a): `Person.getVitalSign` on an unset vital.** `Person.java`
(~line 551, pin `7e08387c68a7f0e21d13076609a159fd473fc902`):

```java
public Double getVitalSign(VitalSign vitalSign, long time) {
  ValueGenerator valueGenerator = vitalSigns.get(vitalSign);
  if (valueGenerator == null) {
    throw new NullPointerException(
        "Vital sign '" + vitalSign + "' not set. Valid vital signs: " + vitalSigns.keySet());
  }
  ...
}
```

Upstream THROWS — a crashed simulation run, not a recorded, continuable
outcome. This project's own honest-absence rule (ADR-0036 AR-4,
extended to `:vital-sign` by ADR-0039 AR-4) is a deliberate, disclosed
divergence: a genuinely-unset vital reading becomes a `:walk-error`
RESULT at the walk boundary (`walk-module`/`run-module`'s own catch),
never an escaping exception — the same result-not-throw discipline
this project's own `honest-absence` mechanism already applies to
`:race`/`:socioeconomic-status`/lookup-table columns.

**AR-6(b): covid19's O2-sat `VitalSign` encoding.**
`covid19/infection.json` (~line 1237, same pin):

```json
"Poor Oxygen Saturation": {
  "type": "VitalSign",
  "vital_sign": "Oxygen Saturation",
  "unit": "%",
  "range": { "low": 75, "high": 89 },
  "direct_transition": "Record Vitals"
}
```

LEGACY exact/range encoding (`isLegacyGmf()`'s own branch,
`State.java`'s `VitalSign.process`), not a `gmf_version 2`
`distribution` — this is what selects the range-sampling path (AR-2's
own `round1`'d uniform draw), not the distribution path, for covid19's
own real content. Confirmed by direct read before Step 2's own
implementation began, per AR-6's own instruction (not implemented
first, then checked).

**Register vs. Observation reader — two independently-existing
mechanisms, never conflated.** `sample-observation-extra`'s own
`vital_sign`-sourced branch (§11/§12, D1a-3) draws independently from
`patient-simulator/vital-signs.edn`'s own reference-range table — a
pre-existing, disclosed simplification (P4/Q3, D1a-4) this Wave does
NOT touch. The NEW register (`:vital-signs`, `gmf-interpreter.clj`'s
own `initial-context`) is a SEPARATE compartment the `VitalSign` state
writes and the `:vital-sign` condition reads; a `VitalSign`-state write
and an Observation's own independent `vital_sign`-sourced draw for the
SAME name can disagree in value — an accepted, disclosed consequence
of keeping the pre-existing Observation mechanism's own scope
untouched (ADR-0039's own fence), not a bug.

Full ruling record, module-by-module characterization, baseline table
provenance, and the post-Wave census movement classification: `notes/
ADRs.md` ADR-0039.
