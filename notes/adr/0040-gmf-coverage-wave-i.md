<!-- Attic file: notes/adr/0040-gmf-coverage-wave-i.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0040 — GMF coverage Wave I: the singleton tail — six small mechanisms land; 7 of 9 blocked modules resolve, 2 unmask new gaps

**Status:** Accepted (author-ruled 2026-08-04, design channel, AR-1
through AR-7 below, recorded verbatim, attributed, per ADR-0007's own
provenance-tag convention — every ruling below is `[A]`); executed
2026-08-04.

### Context

Post-VS census (`2026-08-04-synthea-7e08387-wave-vs.edn`, ADR-0039):
75/85 walk, 1 out-of-scope (`gallstones`), 9 blocked across six small,
fully-pinned mechanisms — `.agents/plans/2026-08-02-gmf-parity-plan.md`
§4's own "Wave I: singleton tail" row (`AllergyOnset`, `Vaccine`,
`wellness-encounters`' own `Weight` gap, `congestive-heart-failure`'s
own newly-unmasked `SetAttribute` conflict, plus two mechanisms the
census's own error payloads named directly: `NamedDistribution` in
`complex_transition`, and an `Observation`-condition absence throw).
Named as the CLOSING unlock wave: if the classification went as
expected, the parity plan §1's countable definition would be met
(84/85 walking + `gallstones` out-of-scope) and only Wave H
(architectural, ruled last, ADR-0039 AR-7) would remain. Semantics
pinned at `7e08387c68a7f0e21d13076609a159fd473fc902`.

### Decision

**AR-1 (NamedDistribution in `complex_transition`) — corrected scope,
found live.** The session prompt named this "4 modules"
(`injuries`/`hospice-treatment`/`home-hospice-snf`/`home-health-
treatment`). Verified against the census's own malli error payloads
AND the vendored module JSON directly, BEFORE any edit: only
`injuries.json` (`Elderly_Incidence_Rates`'s own `complex_transition`)
actually carries a NamedDistribution map (`{"attribute":
"probability_of_fall_injury", "default": 0.06}`) in a `distributions`
entry that the schema's own `number?`-only field rejected —
`distributed_transition`'s own top-level `:distribution` already
accepted this shape (Wave D stage D3, H3); `complex_transition`'s
nested one never gained the same resolution (installed ≠ used, H1's
own deferral, now exercised for real). The other three modules'
`schema-invalid` rejection was `:encounter-class` values outside the
closed enum (`hospice`/`home`/`urgentcare`) — see AR-1b.

**AR-1b (encounter-class vocabulary completed) — a dated addendum,
found live, author-ruled mid-session (AskUserQuestion).** Not named in
any AR the session prompt states. `encounter-class->keyword`
completed to the FULL ten-value `HealthRecord.EncounterType` enum at
the pin (source-confirmed) — `urgentcare`→`:urgent-care`,
`hospice`→`:hospice`, `home`→`:home`, `snf`→`:snf` (the fourth,
`snf`, not yet exercised by any candidate closure this session — added
anyway, the same "complete the whole closed vocabulary in one step"
discipline AR-4 already follows). Author's ruling on the scope
question (folded into this wave rather than left disclosed-not-fixed):
same shape as AR-4's own vocabulary-completion pattern, not a new
mechanism.

**AR-2 (SetAttribute precedence — conflict rejection retired).**
Read against the pin (`State.java`'s own `SetAttribute.process`,
source-grounded): an explicit, ORDERED precedence chain over co-present
value sources — expression > range > seriesData > distribution >
valueCode > valueAttribute > literal value — not a mutual-exclusivity
rule. ADR-0035 AR-4's own load-time `set-attribute-value-conflict?`
rejection (distribution alongside value/value-code) is RETIRED, kept as
history per this project's fix-forward-with-disclosure discipline.
Implemented for the five sources this project supports: `:range` (one
draw, `Person.rand(low, high, decimals)` semantics — `:decimals`
optional, HALF_UP rounding when present), `:distribution` (ADR-0035,
unchanged), `:value-code` (unchanged), `:value-attribute` (an existing
root-scoped attribute's own current value — falls through to `:value`
when that attribute was never written, matching upstream's own
`containsKey` guard, never a silent nil write), `:value` (the literal
fallback). `:expression`/`:series-data` remain clean, named load-time
rejections (`:set-attribute-unsupported-source`, payload names which)
— no CQL evaluator or time-series mechanism exists here, the same
pattern `vital-sign-expression?` already established.
`congestive_heart_failure.json`'s own `Inpatient LOS` (`"value": 0`
alongside an `EXPONENTIAL` `:distribution`) and `hospice_treatment.
json`'s own `Eventual_Hospice_Reason` (`:value_attribute`) are the real
closures this unblocks.

**AR-3 (Observation-condition absence → false) — a corrected reading of
the pin, ruling unchanged.** `Logic.java`'s own `Observation.test`
(source-grounded): the "issue-774 band-aid" returning `false` on no
matching observation is gated behind `exporter.split_records=true`,
NOT Synthea's default (which throws — the SAME reading this project's
own prior throw already had right). This project deliberately ADOPTS
the band-aid's behavior UNCONDITIONALLY — it has no split-records/
lossOfCare concept of its own for the config axis to meaningfully gate,
the same "simplify an upstream config axis this project doesn't model"
disposition `:active-allergy`'s always-false condition and
`type-of-care-weights`'s always-typical-emergency branch already
establish. `is nil`/`is not nil` stay out of v1 scope, unchanged — no
candidate module needs them. `anemia___unknown_etiology.json`'s own
`anemia_sub` submodule (a Hematocrit condition reached before any
Hematocrit was recorded on some branch) is the real closure this
unblocks.

**AR-4 (vital observation vocabulary completed) — under-scoped on
first pass, found live, fixed same session.** First pass: mechanically
enumerated every Observation state at the pin carrying `category:
"vital-signs"` — 10 distinct names (not the session prompt's estimated
22, per its own "don't trust this count" instruction), 5 already
covered by the existing table; added the remaining 5 (`Weight`,
`Heart Rate`, `Respiration Rate`, `Head Circumference`, `Head
Circumference Percentile`), unblocking `wellness-encounters`' own real
found gap. Re-running the census (AR-7) unmasked that this first-pass
scoping was itself too narrow: `sample-observation-extra`'s own
`vital_sign` branch (`gmf-interpreter.clj`) does NOT gate on
`:category` at all — a `category: "laboratory"` Observation with a
`vital_sign` field routes through the identical mechanism.
`congestive_heart_failure.json`'s own Creatinine reader hit
`:unrecognized-vital-sign` the moment AR-2 let that closure's walk run
far enough to reach it. Fixed in the same session (a scope-completion
bug in this session's OWN prior work, not a new design question): a
SECOND, corrected enumeration — every `vital_sign` field value
anywhere in the catalog, any category — found six more rows
(`Creatinine`, `Blood Glucose`, `EGFR`, `LDL`, `Microalbumin
Creatinine Ratio`, `Total Cholesterol`, all `category: "laboratory"`
upstream). `sim-trajectory/vital-signs.edn`'s own header carries both
enumeration methods' dated notes; the SECOND is now the correct one to
re-run for any future addition.

**AR-5 (AllergyOnset + Vaccine — session reads).** Both read fully
against the pin before implementation, per the ruling's own
instruction:
- `AllergyOnset extends OnsetState` (the SAME base `ConditionOnset`
  already extends). This project's own `ConditionOnset` never modeled
  `OnsetState`'s real `diagnose`/target-encounter-deferral/assign-to-
  attribute machinery — an M5a simplification predating this wave
  (`step`'s own `:condition-onset` case emits unconditionally,
  ignoring `:target-encounter`). `AllergyOnset` follows the IDENTICAL,
  already-established simplification: `allergies.json`'s own
  `Allergy_Unspecified` (and the three called submodules —
  `drug_allergy_incidence.json`, `food_allergy_incidence.json`,
  `environmental_allergy_incidence.json`) all author `:target-
  encounter`/`:assign-to-attribute`, several also author non-empty
  `:reactions` — none read downstream anywhere on this closure's own
  mandatory path (confirmed by direct grep across all four files); no
  scope escalation, no engine-side channel needed.
- `Vaccine.process` is a genuinely simpler, unconditional leaf write —
  no target-encounter/diagnose distinction exists upstream AT ALL for
  this state (unlike `OnsetState`). `:series` (a primitive `int`)
  defaults to 0 when absent. `hiv_care.json`'s own five `Administer
  *` states are the real closure this unblocks.
- Found live during implementation (not a read-time finding): the
  loader's own pre-existing `(:series state) (update :series #(mapv
  normalize-imaging-series %))` clause (`ImagingStudy`'s own field,
  Wave F) was ungated on state type — Vaccine's bare `:series` int
  key collided with it, crashing `mapv` over a `Long`. Fixed with a
  `kw-type` guard; full sim-trajectory suite (325 tests) re-run green
  after the fix, not just the two touched namespaces.
- Two pre-existing loader tests used `AllergyOnset` as their own
  still-deferred-type fixture (the same "swap when it becomes
  supported" pattern this test's own history already follows across
  five prior waves); swapped to `Physiology` (State.java's own
  full physiology-simulation engine class — genuinely, not merely
  provisionally, deferred: no analog anywhere in this project's
  architecture).

**AR-6 (oracle bracket — pure identity).** A FRESH recursive scan of
every currently-vendored root (`components/sim/resources/sim/modules`
plus `components/sim-trajectory/test/.../fixtures/death-fixture.json`
— the regression-oracle's own 9 roots) for all six mechanisms, run
BEFORE any edit and again after the AR-4 follow-up fix: zero hits for
SetAttribute `:range`/`:value-attribute`, `complex_transition`
NamedDistribution, `AllergyOnset`/`Vaccine` state usage, the new
encounter-class values, and the new vital-sign names (including the
AR-4 follow-up's six). One category had content — an `Observation`
CONDITION exists in `sore_throat.json`'s own `Determine_if_Bacterial`
(2 occurrences) and `sepsis.json`'s own `Lactate_Level` — but
PROVABLY unaffected by construction: `digest.clj` catches no
exceptions, so the pre-existing baseline digests for these roots could
only have been produced if no sampled walk ever hit the (then-throwing)
absence path; AR-3's fix changes behavior ONLY at that exact point, so
these roots' outcomes are unchanged regardless. `bin/regression-oracle
3d85fa0 HEAD`, run twice (after Step 5, and again after the AR-4
follow-up fix): all 9 roots IDENTICAL both times (Verification
baselines, below).

**AR-7 (census re-run + parity determination) — NOT achieved, recorded
honestly.** Same params as the post-VS census, disambiguated filename
(`2026-08-04-synthea-7e08387-wave-i.edn`, the overwrite bug still open,
worked around by hand-copy, unfixed, named again). Result: 82
`:ok-walked` (up from 75), 1 `:out-of-scope-by-ruling`, 2
`:walk-failed` — NOT the 84/85 + zero-walk-failed the parity
declaration requires, so **no PARITY ACHIEVED note lands**. Classified
module-by-module against the post-VS artifact:
- **Resolved (7):** `anemia-unknown-etiology` (AR-3), `allergies` +
  `hiv-care` (AR-5), `home-health-treatment` + `home-hospice-snf` +
  `hospice-treatment` (AR-1b), `injuries` (AR-1).
- **Unmasked, NOT resolved (2)** — each module's own ORIGINAL blocker
  is gone, but the walk now runs far enough to hit a genuinely
  different, pre-existing gap outside this wave's six mechanisms:
  - `congestive-heart-failure`: AR-2 fixes `Inpatient LOS`; the walk
    then reaches `Dead_within_28_days`, a `Death` state using the
    `:condition-onset`/`:referenced-by-attribute` cause-of-death form
    — a NAMED, disclosed, UNBUILT limitation from Wave C (ADR-0028
    C1/C2's own docstring: "no vendored module needs them yet");
    `congestive_heart_failure.json` now does.
  - `wellness-encounters`: AR-4 (both passes) fixes every vocabulary
    gap on the path through `Record_Weight`; the walk then reaches a
    condition of type `Active CarePlan` (`:active-careplan`) — a
    log-query family member (the same shape `:active-condition`/
    `:active-medication` already establish) this project has never
    built at all.
  Neither finding is fixed here — real design/scope work, not a
  mechanical completion of an already-ruled mechanism (the AR-4
  follow-up's own distinguishing test: same mechanism, wider net,
  fixed same session; these two are DIFFERENT mechanisms entirely).

### Verification baselines

`bin/regression-oracle 3d85fa0 HEAD` (the tip before Step 1 → this
wave's own final tip, `d7f5003`, after the AR-4 follow-up), run twice
(once after `959b0bc`, once after `d7f5003`) — all 9 vendored root
batches IDENTICAL both times: `appendicitis`, `death-fixture`,
`ear-infections`, `ear-infections-engine`, `sepsis`, `sinusitis`,
`sore-throat`, `total-joint-replacement-engine`, `urinary-tract-
infections-engine`. AR-6's pure-identity claim holds, byte-verified.
`clojure -M:poly check` clean at every checkpoint.

### Execution record

**Step 1 (AR-1 + AR-1b, `d779cd6`).** `complex_transition`'s nested
`:distributions` schema gains the SAME `Distribution` type
`distributed_transition`'s own top-level field already has; interpreter
resolves each entry's own `:distribution` through the existing
`resolve-distribution-value` before the weighted pick.
`encounter-class->keyword`/the `:encounter` schema enum gain
`:urgent-care`/`:hospice`/`:home`/`:snf`. Red→green proven by git-stash
(10 failures/10 errors without the fix). 4 new tests + 3 new
encounter-class tests.

**Step 2 (AR-2, `93de2c0`).** `set-attribute-value-conflict?`/
`attribute-value-sources` retired (kept as a dated history comment);
`set-attribute-unsupported-source?` added for `:expression`/`:series-
data`. `SetAttributeRange` schema (`:low`/`:high`/`:decimals`),
`:value-attribute` schema field. Interpreter: `sample-set-attribute-
range` (one draw, HALF_UP rounding) + the full five-source precedence
cond in `step`'s own `:set-attribute` case. Red→green proven (13
failures/2 errors without the fix). 9 new tests.

**Step 3 (AR-3, `f99dff9`).** `observation-condition-holds?` signature
drops the now-unused `module-id` (condition-first, matching
`age-condition-holds?`/`date-condition-holds?`'s own convention);
absence returns `false`. One pre-existing test converted from
throw-expecting to false-expecting (a corrected premise, not a new
test). Red→green proven (1 error without the fix).

**Step 4 (AR-4, `24f0184`).** `vital-signs.edn` gains 5 rows (`Weight`,
`Heart Rate`, `Respiration Rate`, `Head Circumference`, `Head
Circumference Percentile`). 1 new test (5 names, `doseq`). Red→green
proven (1 error without the fix).

**Step 4 follow-up (AR-4, `d7f5003`).** Found re-running the census
(AR-7's own step): 6 more rows (`Creatinine`, `Blood Glucose`, `EGFR`,
`LDL`, `Microalbumin Creatinine Ratio`, `Total Cholesterol`). Test list
extended in place. Red→green proven (1 error without the fix). Oracle
re-verified IDENTICAL after this fix too (Verification baselines).

**Step 5 (AR-5, `959b0bc`).** `AllergyOnset`/`Vaccine` join
`gmf-type->keyword`, schema arms, `step` cases (both `emit-and-
advance`, the SAME shape `:condition-onset` already uses). The
`ImagingStudy`/Vaccine `:series` key collision fixed live (found by
this step's own red test). Two stale deferred-type test fixtures
swapped `AllergyOnset` → `Physiology`. Landed as ONE commit rather than
the two the session prompt named — disclosed in the commit message,
the two states share the same edit regions and a clean split would
cost more than it buys. Red→green proven (9 failures/1 error without
the fix); full 325-test sim-trajectory suite re-run green (not just
the two touched namespaces, given the shared-key-collision fix's own
blast radius).

**Step 6 (oracle bracket).** See Verification baselines, above.

**Step 7 (census re-run, `8ab71e7`).** See AR-7, above, for the full
classification. `clojure -M:poly check` clean.

`gitleaks git --staged -v`: clean, every commit.

### Fence

No Wave H mechanics (pre-roll, fold boundary — untouched). No Wave E
calibration content. No fix for either AR-7 finding
(`congestive-heart-failure`'s own Death cause-of-death forms;
`wellness-encounters`' own `:active-careplan` condition type) — both
are real, disclosed, UNFIXED gaps for the design channel to scope, not
improvised here. No census-tool overwrite-bug fix (worked around by
hand-copy again, per ADR-0035's own precedent). Deviation from the
session prompt's own framing in two places, both disclosed at the
point found rather than silently absorbed: AR-1's "4 modules" claim
(only 1 was actually NamedDistribution; AR-1b's addendum unblocks the
other 3 via a different, author-approved mechanism) and AR-7's own
"parity achieved" contingency (the actual result is 82/85 + 2
walk-failed, not 84/85 + zero — no declaration lands, and Wave H's own
scope may need to grow to absorb these two findings, or a short
follow-up wave may precede it — an author/design-channel decision,
named here, not made here).

---

