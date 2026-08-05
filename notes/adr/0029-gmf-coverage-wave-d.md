<!-- Attic file: notes/adr/0029-gmf-coverage-wave-d.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0029 — GMF coverage Wave D: design (R1–R7) — IR additions, CarePlan v2-silence, closure data files, D0–D3 sequencing

**Status:** Accepted (author-ruled 2026-08-02, design channel, R1–R7
below; recorded verbatim, attributed, per `notes/ADRs.md` ADR-0007's
own provenance-tag convention). D0 (this ADR's own executing session)
runs same day; D1–D3 are named here as placeholders, not yet run.

### Context

Wave D is the GMF coverage arc's fourth wave (`.agents/plans/2026-08-02-
gmf-coverage-plan.md`), the wave named to hold every state type this
project's own module survey found needing BOTH a new IR shape and a new
emitter/emission-decision home — `DiagnosticReport`, `MultiObservation`,
`CarePlanStart`/`CarePlanEnd`, `ImagingStudy`, plus Wave A's own two
named-but-excluded drops (`Vital Sign`, `Active CarePlan`). Two Wave C
dated fix-forward notes (ADR-0028) already redirected work into this
wave: `urinary_tract_infections.json`'s own real closure (UTI, twelve
files, `DiagnosticReport`/`MultiObservation`-dirty) and the newly-found
sixth GMF transition kind, `lookup_table_transition`. Separately, the
live sim-split plan (`.agents/plans/2026-08-02-sim-split-plan.md`) names
S3 (`sim-emit-hl7` extraction) as triggered by "starting any second
state-based emitter" or, per the coverage plan's own cross-reference, by
Wave D's own `DiagnosticReport` emission decision. This session's design
pass (R1–R7 below) rules Wave D's own shape and, per R1, fires the S3
trigger NOW rather than waiting — front-running deliberately, not
accidentally, since emitter growth inside the still-fat `sim` component
is exactly the anti-pattern S3 exists to prevent.

### Decision

Ruled 2026-08-02, design channel, recorded verbatim:

**R1 — S3 executes now, as Wave D's own stage D0**, front-running its
own named trigger deliberately: emitter growth inside fat `sim` is the
anti-pattern the split exists to prevent, and the move is oracle-guarded
(fixed-seed byte-identity, the strongest form this codebase has). This
ADR's own executing session IS D0. Component name `sim-emit-hl7` (AR-1
family, ADR-0025's own naming convention).

**R2 — Wave D's IR additions**, ruled now for the record, built in D1/D2
not D0: (a) ONE new pathway-IR step type, `:diagnostic-report`, for the
observation family — optional report codes plus a vector of
observation-shaped children; both `MultiObservation` and
`DiagnosticReport` compile into it, the exact upstream coupling pinned
by D1's own characterization against Synthea source at the pin; (b)
`:care-plan-start`/`:care-plan-end` as a paired span, mirroring the
existing `:medication-order`/`:medication-end` precedent; (c)
`VitalSign` dissolves into observation-flavored events (a vital-sign
category) — vitals live in the ground-truth log, and the `Vital Sign`
condition type reads clinical state from there; NO new persona
compartment is added for it. Every IR addition co-lands its full chain
in the same change: schema + compile-trajectory mapping + engine
handling (or an explicit pass-through ruling) + an emission decision +
invariants — the same co-landing convention this project's every prior
wave has followed.

**R3 — CarePlan is deliberately v2-silent**: a registry non-entry with a
disclosed comment, the same precedent `:procedure`/`:medication-*`
already set (M3/M5b's own truth-only-facts treatment). Its natural
rendering is a FHIR CarePlan resource, once `sim-emit-fhir` exists — not
a same-session HL7v2 shape invented for a format that has no real
CarePlan-equivalent segment.

**R4 — Closures may carry DATA-FILE members**, not only module JSON:
lookup-table CSVs (the `lookup_table_transition` finding's own
prerequisite). The loader closure, `resources/modules/NOTICE`, its
provenance-header convention, and the content-hash lineage this project
already carries for vendored modules all extend to cover them. Built in
D3.

**R5 — `ImagingStudy` is OUT of Wave D**: named in the coverage plan
with a CHF trigger, not built this wave. A named hole, not a silent
omission — CHF's own prioritization-table row (`components/sim-
trajectory/docs/gmf-interpreter.md`) is where it is revisited.

**R6 — Wave D sequencing**: **D0** (this session — the sim-split S3
emitter extraction, front-run per R1). **D1** (observation family:
`DiagnosticReport`/`MultiObservation`, the `:diagnostic-report` IR step,
ORU^R01-with-OBR emission — unlocks sepsis, closures permitting). **D2**
(CarePlan family: the paired IR span, the `Active CarePlan` condition
type — unlocks MI and `total_joint_replacement`, closures permitting).
**D3** (`lookup_table_transition`, attribute-weighted
`distributed_transition` weights, the UTI closure re-characterization —
unlocks UTI). Each stage is its own session, its own characterization
gate, its own payoff declared from fetched evidence — the same
discipline every prior wave (A/B/C) has run under.

**R7 — The stroke-risk data source is NOT Wave D scope**: a
calibration/content-provenance item (ADR-0028's own escalated finding —
`Chance_of_Stroke`'s `stroke_risk` attribute has no source in this
project), named in the coverage plan with its own row, unowned by any
wave until a future session rules it.

### Fence

This ADR covers the DESIGN pass (R1–R7) only. D0's own execution record
(caller map, golden baseline, extraction accounting, verification) is
recorded in this same document as a dated addendum once D0's own Step 4
lands (see below), not restated in this Decision section. D1, D2, and
D3 are not started — each gets its own characterization note filled
into `.agents/plans/2026-08-02-gmf-coverage-plan.md` and its own session
record when it runs. R2's IR additions are ruled in shape only here; no
schema, compile-trajectory mapping, or engine code exists yet for any of
them.

> **D0 execution note (filled Step 4, 2026-08-02).** D0 executed same
> day as ruled: `components/sim-emit-hl7` extracted from `sim`
> (`emit-hl7`/`v2-replay`/`site-profile`), `poly check` clean, `poly
> test :all skip:integration` 0 failures/0 errors, golden run
> byte-identical, deftest+defspec parity (281 = 206 + 75) held. Full
> caller map, interface design, dependency directions, and verification
> baselines are recorded in `notes/ADRs.md` ADR-0025's own dated
> "S3 executed" note (this ADR's own sibling extraction record) rather
> than restated here — this ADR is Wave D's design record, ADR-0025 is
> the sim-split arc's own execution record, and D0 is the point where
> the two meet. Commits, in order: `7935b71`/`7a3dd58` (Step 0, this
> ADR + plan restructure, plus a same-session fix-forward for an ADR
> insertion-order mistake caught before Step 1), `ccce1fc` (Step 1,
> characterization), `e38e232` (Step 2, extraction), this commit (Step
> 4, records). Session record:
> `.agents/session-records/2026-08-02-sim-split-s3-wave-d-d0.md`.

> **D1a characterization note (filled Step 1, 2026-08-02, stage D1a —
> characterization only, E1: no schema/compile-mapping/engine code lands
> this session).** Full account, source-cited against `sepsis.json` and
> four real Synthea engine files (`State.java`/`HealthRecord.java`/
> `Person.java`/`LifecycleModule.java`) at the same pinned commit every
> prior GMF citation in this document uses:
> `components/sim-trajectory/docs/gmf-interpreter.md` §11. Headline
> findings, for the record: (a) `sepsis.json` is a single-file closure
> (zero `CallSubmodule`) exercising only 3 of the 7 known transition
> kinds, none D3-scoped — D1 carries no D3 dependency via transitions,
> shrinking nothing; (b) `MultiObservation`/`DiagnosticReport` share one
> Java parent (`ObservationGroup`) and take children ONLY as embedded,
> inline `Observation`-shaped definitions — never a reference to a
> preceding state, never coupled to each other — grounding R2(a)'s own
> "one step type, both compile into it" directly against source, not
> inference; the module JSON's own `number_of_observations` field is
> DEAD (never read — the real count is the children vector's own
> length); (c) sepsis's own `Observation`/`MultiObservation`-children use
> THREE value-sourcing mechanisms side by side (`range`, ALREADY BUILT;
> `value_code`, a coded/qualitative finding, UNBUILT; `vital_sign`, a
> named-vital-sign lookup, UNBUILT) — the `vital_sign` case's own real
> upstream source, `LifecycleModule.java`, is a hardcoded Java module
> this project has never ported and has no persona/clinical-state
> equivalent for, a genuine, load-bearing gap distinct from (and not
> resolved by) R2(c)'s own dissolution design; (d) the `VitalSign` STATE
> TYPE and `Vital Sign` CONDITION TYPE R2(c) actually names are BOTH
> absent from sepsis's own closure — R2(c) is neither confirmed nor
> contradicted by this session's evidence, a negative result recorded
> plainly, not silently treated as a pass; (e) neither existing ORU
> builder (`oru-message`/`observation-message`) can render a
> `value_code`-sourced qualitative finding today (both hardcode OBX-2
> `"NM"`), and `oru-message`'s own `obx-segment` requires reference-
> range/abnormal-flag fields no GMF-derived observation carries — a real
> gap the emission design must close, detailed with a concrete field-by-
> field account in §11's own D1a-7. **A schema PROPOSAL drawn from this
> evidence is recorded immediately below, marked PROPOSED — awaiting a
> design-channel ruling, not yet decided.**

> **D1a schema PROPOSAL (drafted Step 2, 2026-08-02) — RULED (design
> channel, 2026-08-02, stage D1b's own Step 0; see the dated ruling note
> immediately after Q4, below, for the resolution). Every claim below is
> drawn from D1a's own characterization (§11); left standing verbatim as
> drafted (append-don't-erase, this document's own convention for
> resolved questions) — the ruling note is what moved this from PROPOSED
> into R2(a)/(c)'s own Decision section, not an edit to the proposal
> text itself.**
>
> **P1 — one new IR step, `:diagnostic-report`, children reuse the
> EXISTING `:observation` step shape verbatim (R2(a)'s own "observation-
> shaped children," now concrete):**
> ```clojure
> [:diagnostic-report
>  (with-transitions [:type [:= :diagnostic-report]]
>    [:codes {:optional true} [:vector sim-model/Concept]]
>    [:observations [:vector ObservationEntry]]
>    [:citation {:optional true} Citation])]
> ```
> `:codes` optional (D1a-2: `DiagnosticReport` always carries report-level
> codes in practice, but `MultiObservation` compiles into the SAME step
> and real Synthea's own `ObservationGroup.codes` is itself just a
> `List<Code>`, not required to be non-empty at the Java level — optional
> here follows the same "don't require what source doesn't" discipline
> `:observation`'s own optional `:category`/`:unit` already set). NO
> `:category`/`:number-of-observations`-equivalent field — D1a-2 found
> `category` is `MultiObservation`-only at the Java level and consumed
> by nothing this project's own emission survey (D1a-7) found a use for,
> and `number_of_observations` is dead JSON (the children vector's own
> count already is the count) — proposed OMITTED under this project's
> own minimal-schema discipline, flagged as Q1 below in case the author
> weighs it differently.
>
> **P2 — `:observation`'s own shape (and `ehrt.sim.engine/ObservationRecord`)
> gain ONE new optional field, `:value-code`, closing the value_code gap
> for BOTH standalone `Observation` states (`sepsis.json`'s own
> `Capillary_Refill`) and `:diagnostic-report`'s embedded children —
> one field, two consumers, not a bespoke child-only shape:**
> ```clojure
> ;; pathway.clj, :observation step (existing step, ONE field added):
> [:observation (with-transitions [:type [:= :observation]] [:codes [:vector sim-model/Concept]]
>                 [:category {:optional true} :string] [:unit {:optional true} :string]
>                 [:value {:optional true} number?]
>                 [:value-code {:optional true} sim-model/Concept]   ;; NEW
>                 [:range {:optional true} Range])]
> ;; engine.clj, ObservationRecord (existing accumulator shape, same field added):
> [:value-code {:optional true} sim-model/Concept]                   ;; NEW
> ```
> `ObservationEntry` (P1's own children shape) IS this same amended
> `:observation` step shape — no third type. `sample-observation-extra`
> (`gmf_interpreter.clj`) gains a `value_code` branch (`{:codes codes
> :value-code (normalize-code value_code)}`, the same `normalize-code`
> helper `:codes` itself already uses) alongside its existing `:range`
> branch.
>
> **P3 — compile-mapping sketch, all three GMF states, confidence levels
> stated plainly (not uniform — VitalSign is NOT sepsis-grounded):**
> - **`MultiObservation`/`DiagnosticReport` -> `:diagnostic-report`**
>   (HIGH confidence, D1a-2/D1a-3 direct source grounding): one compile
>   function, shared — `:codes` from the state's own top-level `codes`
>   (absent on `MultiObservation` states with no report-level code,
>   present on `DiagnosticReport`'s own `Blood_Cultures`-shaped states);
>   `:observations` = one compiled `ObservationEntry` PER embedded child,
>   via the SAME per-child sampling `sample-observation-extra` already
>   does for a standalone `Observation` state (reuse, not a parallel
>   implementation) — `range`/`value_code` both resolve; `vital_sign`
>   resolves per P4's own open question, below.
> - **`VitalSign` -> observation-flavored event (R2(c)'s own ruling)**
>   (LOW confidence — sketched from R2(c)'s own text alone, NOT grounded
>   in any fetched real module this session, since `sepsis.json` doesn't
>   exercise it; the first session vendoring a real VitalSign-bearing
>   candidate should re-derive this against source, not treat this
>   sketch as settled). A real `VitalSign` state (`State.java`) has NO
>   `codes` field at all — it is an internal generator-setter, not an
>   observable clinical event, so "dissolves into an observation-flavored
>   event" has no source-given LOINC/SNOMED code to carry verbatim (code-
>   passthrough law has nothing to pass through). Sketch: sample ONE
>   point value at entry time from the state's own `exact`/`range` (this
>   project's existing sampling infra already covers both forms), category
>   `"vital-signs"`, and — the open gap — an AUTHOR-SUPPLIED or convention-
>   derived code (e.g. a fixed LOINC keyed off the named vital sign,
>   `"Systolic Blood Pressure"` -> `8480-6`, the same code `sepsis.json`'s
>   own `vital_sign`-typed Observations already use) rather than one read
>   from the module JSON, since none exists there. **Flagged as Q2,
>   below — this is a real gap R2(c)'s own ruling text left unstated,
>   surfaced by trying to actually draft the mapping, not assumed away.**
>
> **P4 — the `vital_sign`-field gap (D1a-4): engine has no physiology
> source, so NO real value can be computed from this project's own
> state today. Two options, RECOMMENDATION marked:**
> - **Option A (RECOMMENDED): documented simplification, the SAME shape
>   `Active Allergy`/D5's payer gap already established.** Author a
>   small static default-range table for the handful of named vital
>   signs real candidate closures are known to need so far (`sepsis.json`
>   alone needs `"Systolic Blood Pressure"`/`"Diastolic Blood Pressure"`/
>   `"Oxygen Saturation"`) — `sample-observation-extra` gains a
>   `vital_sign` branch that looks up the name in this table and samples
>   a plain uniform range, same mechanism `range` already uses, clearly
>   commented as a documented approximation of a real continuous
>   physiology model this project does not have. Table grows by
>   evidence (a future candidate needing an unlisted vital sign is a
>   real, visible `:unrecognized-vital-sign`-shaped rejection, not a
>   silent wrong answer) — the same "grows by evidence, not
>   speculation" discipline this project's own condition/state
>   vocabularies already follow.
> - **Option B: leave unbuilt, matching TODAY's accidental behavior,
>   made deliberate.** `vital_sign`-sourced children compile with no
>   `:value`/`:value-code` at all (an `ObservationEntry` carrying only
>   `:codes`) — real, honestly incomplete, but SILENT (no gap surfaces
>   without reading the output closely). `Pulse_Oximetry`/`Record_Blood_
>   Pressure_2` would vendor with a real fidelity hole, undisclosed at
>   the wire level.
>
> **P5 — engine handling: RECOMMEND pass-through, no new accumulator
> field.** Grounded directly against `ehrt.sim.engine`'s own existing
> cases: `:observation`'s `decide` emits one `:event :observation`
> ground-truth event and its `evolve` appends an `ObservationRecord` to
> `patient`'s own `:observations` accumulator (a pure historical list,
> no status/location impact); `:procedure`'s `evolve` is a bare no-op.
> Neither pattern needs inventing for `:diagnostic-report`: `decide`
> emits ONE `:event :diagnostic-report` event carrying `:codes` and the
> full `:observations` vector (mirroring how the compiled IR step itself
> bundles children — one event, not N); `evolve` FLATTENS each child
> into its own `ObservationRecord` appended to `:observations` — the
> IDENTICAL pattern `ObservationRecord`'s own docstring already
> describes for `:result-available`'s per-analyte flattening ("a single
> analyte flattened out of a `:result-available` event's own
> `:results`"), reused rather than a third accumulator shape invented.
> No `PatientState` schema change beyond `ObservationRecord`'s own P2
> `:value-code` field addition.
>
> **P6 — emission sketch (D1a-7's own field-by-field account, condensed
> to the concrete diff):** `message-type-registry` gains
> `:diagnostic-report {:type "ORU" :trigger "R01"}` (the same trigger
> `:result-available`/`:observation` already use). ONE new OBX-builder —
> call it `report-obx-segment` — sharing `observation-obx-segment`'s
> field set (codes/value/unit, no reference-range/abnormal-flag) but
> branching on `:value-code` vs. `:value` for OBX-2 (`"CWE"` + `cwe-field`
> vs. `"NM"` + the numeric string, the SAME `cwe-field` helper
> `obr-segment` already uses for OBR-4). A new `diagnostic-report-
> message` function: MSH/PID/PV1/`orc-segment` (control-id, reused
> unchanged)/`obr-segment` (`:codes`, reused unchanged)/one
> `report-obx-segment` per `:observations` entry via `map-indexed`, the
> SAME shape `oru-message` already uses for `:results`. No change to
> `control-id-for` — the existing single-subject fallback already
> covers a `:diagnostic-report` event.
>
> **Open questions (Q), enumerated per E5, none decided here:**
> - **Q1 — keep or drop `:category`/report-level metadata on
>   `:diagnostic-report`?** P1 recommends OMIT (unused by anything this
>   session's own emission survey found); the author may know a future
>   FHIR `DiagnosticReport.category`/`Observation.category` consumer
>   (`sim-emit-fhir`, not yet built) that would want it carried through
>   now rather than added later.
> - **Q2 — `VitalSign`'s own compile-mapping (P3) has no source-given
>   code to pass through.** Author-supplied code table (P3's own
>   sketch), a required-`:codes`-on-authoring-time convention (the
>   author who VENDORS a VitalSign-bearing module supplies the code,
>   not the interpreter), or something else — genuinely open, and
>   arguably not even THIS wave's decision if no VitalSign-bearing
>   candidate is vendored before some future wave forces the question.
> - **Q3 — P4's own Option A vs. B** (documented simplification vs.
>   leave unbuilt) for the `vital_sign` FIELD gap — P4 recommends A: a
>   disclosed, evidence-grown default-range table beats a silent fidelity
>   hole, but it is new authored content (a data table) this project has
>   not needed anywhere else in the GMF interpreter to date, worth the
>   author's own judgment on whether it's worth building now versus
>   deferring `vital_sign`-sourced children specifically (independent of
>   whether `:diagnostic-report` itself lands) until a second closure
>   needs the same table, proving it's not a one-module special case.
> - **Q4 — is D1a's own evidence (one closure, `sepsis.json`) sufficient
>   to rule P1/P2/P5/P6 (the MultiObservation/DiagnosticReport-proper
>   design, HIGH confidence per P3), or should D1b's own implementation
>   session first re-confirm against the OTHER corpus-known
>   `MultiObservation`/`DiagnosticReport` modules (`congestive_heart_
>   failure`/`gallstones`/`wellness_encounters`/`dialysis`/`lung_cancer`/
>   `colorectal_cancer`, all cited but none closure-read this session) —
>   this session's own budget did not extend to a second closure fetch,
>   named here rather than silently assumed adequate.**

> **D1a schema RULING (design channel, 2026-08-02, stage D1b's own Step
> 0).** The PROPOSAL above (P1–P6) is ACCEPTED AS DRAFTED. Q1–Q4
> resolved:
>
> - **Q1 — RULED IN.** `:category` is added now, optional, on
>   `:observation`/`ObservationEntry` entries — P1's own OMIT
>   recommendation is not followed; the author judged a future
>   `sim-emit-fhir` `Observation.category`/`DiagnosticReport.category`
>   consumer worth carrying through now rather than reconstructing later
>   from a field this session would otherwise have dropped on the floor.
> - **Q2 + Q3 — ONE mechanism answers both.** A single curated
>   vital-sign reference table (vital-sign name -> LOINC code, units,
>   reference range) closes Q2 (`VitalSign`'s own compile-mapping has no
>   source-given code — the table supplies one, author-curated, per P3's
>   own sketch) AND Q3 (P4's own Option A, the documented-simplification
>   default-range sample) AT ONCE, and additionally supplies the
>   reference-range/abnormal-flag inputs D1a-7 found no GMF-derived
>   observation carries — one table, three consumers, not three separate
>   mechanisms. Built as content (F2, below), not code.
> - **Q4 — ruled on THIS session's own engine-source evidence, no second
>   closure fetched.** D1a-2's grounding against `State.java`'s own
>   `ObservationGroup` class hierarchy (both `MultiObservation` and
>   `DiagnosticReport` extend it identically, embedded-only children,
>   confirmed structurally, not merely by reading `sepsis.json` alone) is
>   sufficient to rule P1/P2/P5/P6 now. The confirmation duty Q4 itself
>   named does not lapse: the first future `MultiObservation`/
>   `DiagnosticReport`-bearing module vendored after this session must
>   note, in its own closure survey, whether this design held against it
>   — recorded here so a future session knows the check is owed (F4).
>
> **Governing principle (recorded here for the record, applied
> throughout stage D1b's own implementation):** never override what the
> vendored artifact specifies; freely supply what it delegates to the
> engine. Stroke's own `default: 0` (ADR-0028's own escalated
> `stroke_risk` finding) is SPECIFIED content — it stays blocked, no
> supplied replacement, since overriding a real authored value would
> silently misrepresent the vendored module itself. Sepsis's own
> `vital_sign` values are DELEGATED to an unported engine module
> (`LifecycleModule.java`, D1a-4) that supplies them at runtime rather
> than specifying them in the module's own JSON — supplying an
> in-project, provenance-cited replacement for a delegated mechanism is
> this simulator's own already-established pattern (Persona replacing
> Synthea's own demographics-generation engine), carried out here under
> the same full content-provenance discipline (F2's own hashed,
> source-cited table), not a new kind of liberty.

> **D1b execution note (filled Step 4, 2026-08-02).** D1b executed same
> day as ruled: the vital-sign reference table (D1 F2, `components/
> sim-trajectory/resources/sim-trajectory/vital-signs.edn`, LOINC codes
> cross-checked against HL7 FHIR's own public examples, `notes/facts-
> register.md` F21); `ehrt.sim-model.pathway`'s new `:diagnostic-report`
> step and `ObservationEntry` schema; `ehrt.sim-trajectory.gmf`'s two new
> loadable state types and `:observation`'s own `:value-code`/
> `:vital-sign` fields; `ehrt.sim-trajectory.gmf-interpreter`'s
> `value_code`/`vital_sign` sampling branches and the shared
> `:diagnostic-report` trajectory-event compile for both state types;
> `ehrt.sim-trajectory.compile-trajectory`'s `observation-fields`
> extraction and `diagnostic-report->step`; `ehrt.sim.engine`'s
> decide/evolve pass-through (P5's own per-child flattening, reused from
> `:result-available`); `ehrt.sim.check`'s therapeutic-intent-class
> extension; `ehrt.sim-emit-hl7.emit-hl7`'s extended `observation-obx-
> segment` (reused directly at both call sites, never duplicated) and
> new `diagnostic-report-message`; `sepsis.json` vendored
> (`resources/modules/NOTICE`). `poly check` clean; the full non-
> integration suite green throughout (188 `Testing ehrt.*` namespace
> announcements at this session's own HEAD, 0 failures/0 errors); the
> byte-identical oracle held for all five pre-existing vendored roots
> (sinusitis/appendicitis/sore_throat/ear_infections-closure/death-
> fixture) — SHA-256 digests of a fixed-seed engine run's own emitted
> HL7 (interpreter-trajectory digest for the closure, no full-engine
> emission path existing for a real CallSubmodule closure yet) compared
> byte-for-byte between this session's own pre-Step-0 HEAD (`dce2086`)
> and its post-Step-3 HEAD (`870a1ab`) in a disposable worktree: all
> five identical. Commits, in order: `297e337` (Step 0, RULED + roadmap),
> `917e9cf` (a same-session fix-forward: Step 0's own roadmap.md growth
> self-tripped the reading-set budget gate, caught and bumped the same
> way twice before), `5974fd2` (Step 1, vital-sign table), `f4a4e99`
> (Step 2a, sim-model), `acd49f5` (Step 2b, sim-trajectory),
> `7a13de5` (Step 2c, sim), `e345f13` (Step 2d, sim-emit-hl7), `870a1ab`
> (Step 3, sepsis vendored), this commit (Step 4, records). Session
> record: `.agents/session-records/2026-08-02-gmf-coverage-wave-d-stage-
> d1b.md`.
>
> **Deviation record.** Two pre-existing `gmf_test.clj` fixtures
> (`deferred-state-type-json`, `calls-deferred-leaf-json`) used
> `MultiObservation` as their own "still deferred" negative-test
> example — this session's own change made that premise false, caught
> live by the affected-test run (4 failures, all citing the same stale
> assumption), fixed forward to `ImagingStudy` (R5, genuinely still
> deferred) rather than left to silently test something it no longer
> tested — the same "swapped again, for the same reason" disclosure
> those fixtures' own docstrings already carry from an earlier such
> swap (CallSubmodule, GMF coverage Wave B). No schema or design
> deviation from the RULED proposal (P1–P6) occurred — F1's own "no
> silent additions beyond it" held throughout; the one addition beyond
> P6's own literal text (reference-range/abnormal-flag rendering on
> `observation-obx-segment`, plain-value-observation determinism
> unaffected) is the Q2+Q3 ruling's own explicit instruction, not an
> unruled deviation.

> **D2 session start (design channel, 2026-08-02).** Stage D2 (the
> CarePlan family) begins. Author rulings for this stage, recorded
> verbatim:
>
> - **G1** — the implementation spec is R2(b)'s pair-mirror:
>   `:care-plan-start`/`:care-plan-end` shaped on the
>   `:medication-order`/`:medication-end` precedent (`pathway.clj`) —
>   codes plus whatever start/end linkage the upstream semantics
>   actually use (`assign_to_attribute`/`referenced_by_attribute`, the
>   mechanism Wave B already built for medications, is the expected
>   shape). Exact CarePlanStart/CarePlanEnd field semantics (activities,
>   reason codes, end-reference mechanism) are pinned by Step 1's fetch
>   of Synthea source at the pin and recorded here BEFORE Step 2
>   implements them. A field the medication mirror cannot represent is
>   an ESCALATION with evidence, not an improvised schema extension.
> - **G2** — installed ≠ used, as D1's F3 already established: the
>   `Active CarePlan` CONDITION is built only if a module in this
>   session's declared vendoring scope exercises it; otherwise it stays
>   design-ruled, implementation-deferred, with a docs note. Same rule
>   for any CarePlan field (activities, reason) no in-scope module
>   exercises.
> - **G3** — R3's v2 silence is implemented as a `message-type-registry`
>   NON-ENTRY plus the disclosed comment beside the
>   `:procedure`/`:medication-*` precedent (`emit_hl7.clj`), AND
>   asserted: the vendored test proves care-plan events produce zero
>   messages.
> - **G4** — the Step 1 characterization gate: fetch both closures in
>   full at the pin; survey row per member; transition-kind sweep
>   against all seven known kinds (a D3 kind's presence drops that root
>   from D2's scope, resequenced honestly); D7 hidden-import check per
>   closure; specify-vs-delegate audit for any attribute/value source
>   found. Declare the vendoring scope from the evidence — zero, one, or
>   both roots are acceptable outcomes.
> - **G5** — vendored tests prove the span: a walk containing
>   `:care-plan-start` and its matching `:care-plan-end` with correct
>   linkage; the engine fold carrying the active span in patient state;
>   the G3 silence assertion; if the condition is built per G2, a branch
>   taken BECAUSE a care plan is active; MI additionally re-proves the
>   Wave C death machinery inside a closure walk.
>
> **D2 characterization (filled Step 1, 2026-08-02).** Full account,
> source-cited against both real closures at the same pin and
> Synthea's own `State.java` (`CarePlanStart`/`CarePlanEnd` classes):
> `components/sim-trajectory/docs/gmf-interpreter.md` §13. Headline
> findings: (a) `myocardial_infarction.json`'s real closure is 27 files
> (root + 26 transitively-called submodules through its own CABG
> surgical pathway), not the single-file top-level count the prior
> survey implied — dirty with THREE independent, each-sufficient
> blockers (`lookup_table_transition` ×39, D3's own scope;
> `ImagingStudy` ×5, R5, explicitly out of Wave D; `SupplyList` ×6, a
> genuinely new, never-before-named state type) — **deferred, not
> vendored**, same disposition `urinary_tract_infections.json` already
> has; (b) `total_joint_replacement.json`'s real closure is only 4
> files and surveys CLEAN of every Wave-D-scoped type except
> `CarePlanStart`/`CarePlanEnd` itself (zero D3-scoped transition
> kinds, D7 hidden-import check clean); (c) `CarePlanEnd`'s own
> `careplan` field (grounded against `State.java`) is a same-module
> state-NAME reference, structurally identical to `MedicationEnd`'s own
> `medication_order` field — R2(b)'s pair-mirror confirmed directly
> against source, no attribute-based cross-module linkage needed for
> this closure (real Synthea's own `assign_to_attribute`/
> `referenced_by_attribute` mechanism exists on both CarePlan states
> too, per source, but stays unbuilt this session per G2 — TJR doesn't
> exercise it); (d) `total_joint_replacement.json`'s own mandatory
> `Joint_Replacement_Guard` requires an attribute, `joint_replacement`,
> that no state in the closure ever writes — the module's own `remarks`
> field discloses why: it is triggered by two SIBLING root modules
> (`osteoarthritis.json`/`rheumatoid_arthritis.json`), architecturally
> outside this project's root-scoped CallSubmodule contract and outside
> this session's own scope to vendor.
>
> **Ruling (self-ruled at the characterization gate, precedented by
> D1a's own governing principle: "never override what the vendored
> artifact specifies; freely supply what it delegates to the engine").**
> `joint_replacement` is DELEGATED content (no default value anywhere
> in the module JSON, unlike `stroke_risk`'s own specified `default:
> 0`) — `ehrt.sim-trajectory.gmf-interpreter/run-module` gains one new,
> purely-additive, backward-compatible trailing arity accepting an
> `initial-attributes` map (every existing call site unaffected, `{}`
> implied); the vendored test supplies `joint_replacement` as an
> authored, provenance-cited starting attribute, citing the module's
> own `remarks` block. This is narrower than vendoring
> `osteoarthritis`/`rheumatoid_arthritis` themselves (out of scope,
> unowned).
>
> **Fix-forward finding (dated note, filled Step 2, 2026-08-02): a
> SECOND, independent `total_joint_replacement.json` blocker, found
> empirically testing the `joint_replacement` fix against the real
> closure.** `Joint_Replacement_Guard`'s own `allow` is a COMPOUND
> condition (`{:and [Attribute is-not-nil, Age > 50 years]}`);
> `age-guard-jump-days` (the analytical jump that resolves a failing
> BARE `:age >= N years` Guard) only recognizes that one shape, a
> KNOWN, deliberate v1 boundary per its own docstring — this Guard is
> neither bare nor `>=`, so the walk blocks PERMANENTLY at age 0
> (confirmed empirically: `joint_replacement` seeded, 60-year
> registration offset, `:status :blocked`, zero trajectory events; full
> account: `components/sim-trajectory/docs/gmf-interpreter.md` §13).
> Extending Guard's own analytical-resolution machinery to handle a
> compound condition correctly is real interpreter-core work outside
> G1–G5's own ruled scope (the CarePlan chain, not Guard/condition
> resolution) touching every other vendored root's own Guard/Delay
> behavior — an ESCALATION with evidence (G1's own instruction), not
> improvised this session.
>
> **Declared D2 vendoring scope, REVISED: ZERO roots vendored this
> session** — an outcome G4 explicitly names as acceptable.
> `myocardial_infarction.json` deferred (three independent blockers,
> unrelated to CarePlan); `total_joint_replacement.json` ALSO deferred
> (the compound-Guard blocker, above) despite the `joint_replacement`
> attribute gap being genuinely resolved. Both findings recorded in
> `components/sim-trajectory/docs/gmf-interpreter.md` §9's own
> prioritization table (dated note) and the coverage plan's own payoff
> map (Step 4 fix-forward), the same disclosure discipline
> `urinary_tract_infections.json`'s own D6 finding already established.
> The CarePlan mechanism itself (sim-model/sim-trajectory/sim/sim-
> emit-hl7, all four layers) is real, fully co-landed, tested
> infrastructure regardless — the same "build the mechanism, defer the
> vendoring target" shape `VitalSign` (D1a) already established.
>
> **Regression-oracle method, disclosed (a deviation from a literal
> SHA-256-digest-across-a-disposable-worktree, the D1b precedent):**
> this stage's own regression proof is the full non-integration test
> suite (`clojure -M:poly test :all skip:integration`) run at this
> commit's own pre-Step-2 HEAD and compared, namespace-by-namespace,
> test-count-and-assertion-count-and-zero-failures, against the same
> suite re-run at Step 4's own close-out HEAD, for every one of the six
> PRE-EXISTING vendored-root test namespaces (sinusitis, appendicitis,
> sore_throat, ear_infections, sepsis, death-fixture) — each of those
> namespaces already carries property-based (`defspec`, 100-200
> iterations) and fixed-seed determinism assertions exercising far more
> seeds than a single digest would, the same practical regression
> guarantee through a stronger, already-built mechanism. Full baseline
> captured at HEAD `a41d8c2` (Step 0's own commit), before any D2 code
> change: `poly check` clean; full suite 0 failures/0 errors across
> every namespace (log retained in this session's own scratch, not
> committed).

> **D2 execution note (filled Step 4, 2026-08-02).** D2 executed same
> day as ruled: `pathway.clj`'s new `:care-plan-start`/`:care-plan-end`
> steps (mirroring `:medication-order`/`:medication-end` exactly);
> `gmf.clj`'s `CarePlanStart`/`CarePlanEnd` loader entries and
> `:careplan` state-name normalization; `gmf_interpreter.clj`'s
> `:care-plan-start`/`:care-plan-end` interpreter cases and
> `run-module`'s new backward-compatible `initial-attributes` trailing
> arity; `compile_trajectory.clj`'s `care-plan-start->step`/`care-plan-
> end->step` (joining `pre-horizon-fact-types`, the ongoing-therapeutic-
> content class); `engine.clj`'s `CarePlanRecord`/decide/evolve fold
> (mirroring `MedicationOrderRecord` exactly) and `check.clj`'s
> `clinical-content-only-when-admitted` extension (`:care-plan-start`
> joins, grounded against `State.java`'s own encounter constraint);
> `emit_hl7.clj`'s disclosed registry non-entry plus two asserting
> tests (G3). `poly check` clean throughout; full non-integration suite
> green at every checkpoint (188 `Testing ehrt.*` namespace
> announcements at this session's own final HEAD, 0 failures/0 errors);
> the regression-oracle method (this ADR's own disclosed note, above)
> held exactly — all seven pre-existing vendored-root test namespaces
> (sinusitis/appendicitis/sore_throat/ear_infections/sepsis ×2/death-
> fixture) show IDENTICAL test-count/assertion-count/zero-failures
> between HEAD `a41d8c2` (Step 0) and this session's own final HEAD.
> Commits, in order: `a41d8c2` (Step 0, RULED + roadmap), `0131985`
> (Step 1, characterization), `7319680` (Step 2a, sim-model),
> `efe1972` (Step 2b, sim-trajectory), `c1dee3d` (Step 2c, sim),
> `b499efc` (Step 2d, sim-emit-hl7), `85c75de` (a same-session fix-
> forward: a second, independent `total_joint_replacement.json`
> blocker found live testing the first fix, revising the declared
> vendoring scope to zero), this commit (Step 4, records). Session
> record: `.agents/session-records/2026-08-02-gmf-coverage-wave-d-
> stage-d2.md`.
>
> **Deviation record.** Two deviations from the ruled plan, both
> disclosed at the point they occurred, not silently absorbed: (1) the
> regression-oracle METHOD (a full-suite namespace/assertion-count
> comparison standing in for a literal SHA-256-digest-across-a-
> disposable-worktree) — recorded in this ADR's own dated note as soon
> as Step 1 ruled it, not retrofitted here; (2) the declared vendoring
> SCOPE itself moved twice: Step 1 declared `total_joint_replacement.
> json` (TJR is clean, `myocardial_infarction.json` is not); Step 2's
> own live test of the `joint_replacement` fix against the real TJR
> closure found a SECOND, independent blocker (the compound-Guard gap,
> above) neither G1–G5 nor Step 1's own characterization anticipated —
> revised to zero vendored roots, fully disclosed in a dedicated fix-
> forward commit (`85c75de`) the moment it was found, not smoothed over
> at close-out. No schema or design deviation from G1/G2/G3 occurred —
> the CarePlan mechanism itself matches the ruled pair-mirror exactly;
> the ONLY deviation is in what got vendored, not in what got built.

> **Oracle byte-verification (dated note, ruled 2026-08-02, post-Wave-D
> cleanup session — ADR-0030 J1):** the disclosed full-suite comparison
> method (above) is UPGRADED to byte-verified for this stage's own
> span. `bin/regression-oracle bbeceb6 d23fa9b` (a disposable worktree
> per commit, `bin/oracle-src/ehrt/oracle/digest.clj`'s own fixed-seed
> golden runs for all six pre-existing vendored roots — appendicitis/
> sinusitis/sore_throat/ear_infections-closure/death-fixture/sepsis,
> HL7 emission bytes included for the three engine-layer roots):
> IDENTICAL SHA-256 digests on every root between `bbeceb6` (D1b's own
> close-out commit) and `d23fa9b` (this stage's own close-out) — this
> was the optional D2-span extension J1 itself named ("if
> `d23fa9b`->`d8447e6`-era baselines are cheaply reproducible"), run
> because it was: same harness, same two-worktree diff, no separate
> mechanism needed. Digest table in this session's own session record
> (`.agents/session-records/2026-08-02-post-wave-d-cleanup.md`). D2's
> own regression-oracle claim is therefore byte-verified, not merely
> count-verified — the deviation this ADR's own dated note above
> disclosed is now closed.

> **D2 rider (dated note, ruled 2026-08-02, D3's own Step 0 — H7):** the
> `initial-attributes` arity D2 added to `ehrt.sim-trajectory.gmf-
> interpreter/run-module` (above) is SCOPED here, in the record, before
> a second session reaches for it: it is for WALK-ENTRY inputs standing
> in for an out-of-closure writer (`total_joint_replacement.json`'s own
> `joint_replacement`, sourced from two sibling root modules this
> project does not vendor) — an authored, provenance-cited starting
> value the vendored TEST supplies and discloses per-use, never a
> general cross-module communication channel. Cross-module facts still
> travel through clinical state, exactly as ADR-0027's own D1 already
> established (root-scoped workflow attributes share a call tree; they
> do not reach across separate top-level walks) — `initial-attributes`
> does not change that, it only seeds one walk's own starting point.
>
> **D3 session start (design channel, 2026-08-02).** Stage D3 (the
> arc's own closing stage: `lookup_table_transition`, attribute-weighted
> `distributed_transition` weights, the UTI closure re-characterization,
> and TJR's own compound-Guard blocker) begins. Author rulings for this
> stage, recorded verbatim:
>
> - **H1** — each mechanism's semantics are pinned from Synthea source
>   at the pin BEFORE its implementation commit, recorded here: the
>   dispatch rule for `LookupTableTransition` (key columns, row
>   matching, the probability draw), the attribute-weight resolution
>   rule (`NamedDistribution`: when the attribute is read, fallback
>   semantics), and the compound-Guard forms TJR actually exercises.
>   Any rng draw any mechanism adds joins the documented order contract.
>   One commit per mechanism.
> - **H2** — lookup-table CSVs are closure DATA MEMBERS per R4: resolved
>   by `load-closure` from the module's own table references, vendored
>   beside the modules with sha256 lineage recorded in the NOTICE and a
>   facts-register entry. The specify-vs-delegate audit applies to table
>   KEY COLUMNS: a table keyed on person fields the persona genuinely
>   supplies is buildable; one keyed on fields the persona lacks is an
>   ESCALATION with the column named.
> - **H3** — the attribute-weight mechanism landing does NOT unblock
>   stroke; the survey note must say so in so many words: stroke's own
>   artifact SPECIFIES `default: 0` with no in-project source for
>   `stroke_risk` — per the specify-vs-delegate principle that stays
>   blocked until the risk-source item is ruled. The mechanism is built
>   for schema honesty and for whatever module exercises it legitimately,
>   proven with a fixture test, not stroke.
> - **H4** — compound-Guard resolution extends `age-guard-jump-days`
>   under a sound-jump-or-escalate rule: a compound containing an Age
>   condition may be jumped only to a bound provably no later than the
>   earliest time the compound could become true (then re-evaluated);
>   any form where no sound bound exists is an escalation with the form
>   quoted, not a heuristic jump. Installed ≠ used: build the forms TJR
>   exercises; name the rest.
> - **H5** — gates. UTI: FULL re-characterization — a fresh fetch of the
>   complete closure at the pin (not trusting the D2-era file list),
>   survey rows, an all-seven-kind sweep, D7, the specify-vs-delegate
>   audit including every lookup table's own key columns; declared scope
>   from the evidence. TJR: re-verify the D2 fetch by hash (re-fetch only
>   on mismatch); its gate is H4 landing plus its D2 survey standing.
> - **H6** — vendored tests. UTI: entry-path lookup dispatch proven BOTH
>   ways (a seed reaching Cystitis, a seed reaching Pyelonephritis), a
>   `type_of_care_transition` path taken, cross-boundary encounter events
>   asserted, a full engine/check run. TJR: a walk that provably ADVANCES
>   past the compound age guard, the care-plan span with G3's silence
>   assertion, the `initial-attributes` seeding disclosed in the test's
>   own docstring, a full engine/check run. Mixer-RNG seed discipline
>   throughout.
> - **H7** — the D2 rider, above: `initial-attributes` scoping.
> - **H8** — D3 closes the wave: a Wave D retrospective note lands in
>   the coverage plan (the payoff tally as it actually happened, the
>   standing named items, an S4-trigger status line).
>
> **D3 characterization note (filled Step 1, 2026-08-02).** Full account,
> source-cited against `Transition.java`'s own `LookupTableTransition`/
> `NamedDistribution` classes, the fresh-fetched UTI closure (all twelve
> files plus both lookup-table CSVs, hashed), and TJR's own re-verified
> fetch: `components/sim-trajectory/docs/gmf-interpreter.md` section 14.
> Headline findings: (a) both `LookupTableTransition` mechanisms (H2) and
> `NamedDistribution` (H3) grounded directly against source, both key-
> column audits pass (age/gender, persona-backed); (b) UTI's full closure
> re-survey confirms Wave B's own headline finding still holds
> (`lookup_table_transition` the only remaining gap) but surfaces FOUR
> new mechanical findings section 9's own type-only census could not
> have caught — a `gmf_version: 2` distribution-wrapper encoding
> (pervasive, mechanical, a pre-existing `Procedure`-duration bug found
> and DISCLOSED not fixed along the way), `SetAttribute`'s own `value_code`
> field, an embedded-observation-child `exact` value mechanism, and seven
> new vital-sign-table rows (LOINC-verified against a live public FHIR
> terminology server, `notes/facts-register.md` F22); (c) TJR's D2 fetch
> re-verified by hash, its own two additional findings (`value_code`,
> `exact`) confirmed harmless-but-buildable; (d) H4's compound-Guard
> resolution designed (sound-jump-or-escalate), no Synthea source citation
> applies (this project's own analytical extension, not a ported
> mechanism) — both UTI and TJR declared BUILDABLE, pending the named
> mechanisms/findings landing in Step 2.

> **D3 execution note (filled Step 4, 2026-08-02).** D3 executed same
> day as ruled: `ehrt.sim-trajectory.gmf/load-closure`'s own closure
> DATA-FILE member extension (H2, R4) plus `lookup_table_transition`
> (the sixth transition kind, `resolve-lookup-table-transition`);
> `resolve-distribution-value` (H3, attribute-weighted `distributed_
> transition`, proven against a fixture, stroke stays blocked);
> `age-guard-jump-days` extended under a sound-jump-or-escalate rule
> (H4, unblocking TJR's own compound `Joint_Replacement_Guard`). Both
> declared payoffs vendored: `urinary_tract_infections.json` (twelve
> files, this project's SECOND closure and FIRST data-file closure
> members) and `total_joint_replacement.json` (four files, THIRD
> closure). `poly check` clean throughout; the full non-integration
> suite green at every checkpoint (192 `Testing ehrt.*` namespace
> announcements at this session's own final HEAD, 0 failures/0 errors);
> the regression-oracle method (ADR-0029's own D2 dated note, a full-
> suite namespace/assertion-count comparison, disclosed there as a
> standing alternative to a literal SHA-256-digest-across-a-disposable-
> worktree) held exactly — every one of the eight pre-existing vendored-
> root test namespaces (sinusitis/appendicitis/sore_throat/
> ear_infections/sepsis/death-fixture, plus sim-emit-hl7's own emission-
> layer suites) shows IDENTICAL test-count/assertion-count/zero-failures
> between this session's own pre-Step-2 HEAD and its final HEAD, HL7
> emission bytes included (`ehrt.sim-emit-hl7.vendored-sepsis-test`'s
> own determinism assertion, unchanged, held throughout). Commits, in
> order: `07ff1d5` (Step 0, H1-H8 + roadmap), `074d4d7` (Step 1,
> characterization), `ea85852` (Step 2a, H2), `af89d0e` (Step 2b, H3),
> `91c9bfd` (Step 2c, H4), `5d87388` (a same-session disclosed addition:
> four mechanical findings from Step 1's own fuller characterization —
> `gmf_version` 2 timing encoding, `SetAttribute` `value_code`, an
> observation-child `exact` mechanism, vital-sign table growth),
> `fdd0644` (a same-session disclosed bug fix found live vendoring UTI:
> `first-matching-entry`'s own missing fallback-to-last-entry
> semantic), `4d9178b` (a same-session disclosed addition: three more
> UTI-specific loader findings — a new `virtual` encounter-class value,
> `complex_transition`'s own either/or, a real upstream CSV
> byte-order-mark), `8dcec56` (Step 3, UTI vendored), `430edbb` (Step 3,
> TJR vendored), this commit (Step 4, records). Session record:
> `.agents/session-records/2026-08-02-gmf-coverage-wave-d-stage-d3.md`.
>
> **Deviation record.** Three disclosed deviations from H1-H8's own
> literal text, each the same "characterization/vendoring surfaces a
> real, in-spirit-authorized finding" shape ADR-0027's own deviation
> record first established a precedent for (and ADR-0029's own D1/D2
> dated notes have each repeated since):
>
> 1. **Four mechanical loader/interpreter additions (`5d87388`), named
>    in Step 1's own characterization but not among H1-H8's own three
>    named mechanisms** — `gmf_version` 2's uniform stochastic-timing
>    encoding (a loader normalization, not a new interpreter mechanism,
>    the same disposition Wave B's own encounter-class/wellness findings
>    already established; a disclosed, unrelated, PRE-EXISTING
>    `Procedure`-duration bug was found along the way and is NAMED, not
>    fixed, since repairing it would touch every vendored root's own
>    regression behavior, outside this session's own ruled scope),
>    `SetAttribute`'s own `value_code` field, a fourth observation
>    value-sourcing mechanism (`exact`), and seven new vital-sign
>    reference-table rows (LOINC-verified against a live public FHIR
>    terminology server, `notes/facts-register.md` F22) — all four cheap,
>    narrowly-scoped, and load-bearing for the declared payoffs.
> 2. **A real interpreter BUG found live vendoring UTI, fixed in its own
>    commit (`fdd0644`), outside H1-H8's own named scope**:
>    `first-matching-entry` (shared by `conditional_transition`/
>    `complex_transition` dispatch) returned `nil` when no entry's own
>    condition held and none was condition-less, crashing its own
>    callers — real Synthea's own `ConditionalTransition.follow`/
>    `ComplexTransition.follow` both fall back to the LAST entry
>    unconditionally in that case, a real semantic this project's own
>    port never implemented because no previously-vendored module's own
>    mandatory path ever exercised it. Isolated into its own commit,
>    ahead of the three D3f loader findings, since it is a genuine
>    behavior-changing bug fix touching shared dispatch logic, not a
>    schema-widening addition — the regression oracle (above) confirms
>    it changes nothing for any already-vendored root.
> 3. **Three more UTI-specific loader findings (`4d9178b`)**, each
>    confirmed against Synthea source before building, the same
>    discipline H1 names for the three named mechanisms: a new,
>    genuinely distinct `virtual` encounter-class value (NOT aliased
>    onto `:ambulatory`, unlike `outpatient`'s own Wave B precedent — a
>    remote encounter is a different clinical modality, and this
>    session's own interpreter-layer-only fence means `compile-
>    trajectory`'s own encounter mapping is never exercised for this
>    closure anyway); `complex_transition`'s own per-branch either/or
>    (a direct `:transition` OR a weighted `:distributions` list,
>    confirmed against `Transition.java`'s own `ComplexTransitionOption`
>    — this loader's schema previously required `:distributions`
>    unconditionally); and a real upstream UTF-8 byte-order-mark in
>    `uti_recurrence.csv` (verbatim from Synthea; `uti.csv` carries
>    none), stripped before parsing.
>
> None of the three changes H1-H8's own ruled DESIGN — each is an
> IMPLEMENTATION-level finding Step 2/3's own build surfaced, resolved
> per this project's own standing "extend v1 with a documented reason,
> or defer" option, not a design reopening. **Interpreter-layer proof
> only for both vendored payoffs** (`ehrt.sim-trajectory.vendored-uti-
> test`/`vendored-tjr-test`) is NOT a deviation — it is the SAME
> standing, already-disclosed limitation `ear_infections.json`'s own
> vendored test already carries (confirmed by direct search this
> session: no full compile-trajectory/engine/emit round-trip test
> exists for ANY closure-having module vendored to date), named here
> for completeness, not newly introduced.
>
> **Oracle byte-verification (dated note, ruled 2026-08-02, post-Wave-D
> cleanup session — ADR-0030 J1):** the disclosed full-suite comparison
> method (ADR-0029's own D2 dated note, reused for D3 above) is
> UPGRADED to byte-verified for this stage's own span, the session
> prompt's own required check. `bin/regression-oracle d23fa9b 7257775`
> (a disposable worktree per commit, `bin/oracle-src/ehrt/oracle/
> digest.clj`'s own fixed-seed golden runs for all six pre-existing
> vendored roots — appendicitis/sinusitis/sore_throat/ear_infections-
> closure/death-fixture/sepsis, HL7 emission bytes included for the
> three engine-layer roots): IDENTICAL SHA-256 digests on every root
> between `d23fa9b` (pre-D3, D2's own close-out) and `7257775` (post-D3,
> Wave D's own close-out) — H1-H8's own three named mechanisms and all
> three disclosed additions, including `fdd0644`'s own shared-dispatch
> bug fix, change nothing observable for any of the six pre-existing
> roots at these seeds/populations. Digest table in this session's own
> session record (`.agents/session-records/2026-08-02-post-wave-d-
> cleanup.md`). D3's own regression-oracle claim is therefore
> byte-verified, not merely count-verified — combined with the dated
> note on ADR-0029's own D2 section (above), the byte-verified chain
> now runs unbroken from D1b's own literal digest (`dce2086`-
> `870a1ab`) through `7257775`.
>
> **H8 — Wave D close-out.** This session closes GMF coverage Wave D in
> full: **D0** (sim-split S3, `sim-emit-hl7` extraction, ADR-0025/
> ADR-0029), **D1** (observation family, `sepsis.json` vendored),
> **D2** (CarePlan family mechanism landed, zero roots vendored that
> stage), **D3** (this session — three mechanisms, two roots vendored).
> Payoff tally, as it actually happened against the wave's own original
> plan (`.agents/plans/2026-08-02-gmf-coverage-plan.md`): **landed** —
> `sepsis.json` (D1), `urinary_tract_infections.json` and
> `total_joint_replacement.json` (D3); the CarePlan mechanism itself
> (D2) landed as real, tested infrastructure with no vendored root to
> show for it yet. **Standing named items, unowned by any wave**:
> `myocardial_infarction.json` (three independent blockers —
> `ImagingStudy`/R5, a genuinely new `SupplyList` state type, and
> `Counter`, none touched by D0-D3); `stroke.json` (the stroke-risk data
> source, R7 — the attribute-weighted `distributed_transition` mechanism
> landed this session, H3, but the revisit trigger was always both
> halves together); `ImagingStudy` itself (R5, a named CHF trigger);
> `Active CarePlan` (the condition type, design-ruled/implementation-
> deferred per D2's own G2, no exercising module yet); the pre-existing
> `Procedure`-duration bug (D3c finding 1, disclosed not fixed). **S4
> trigger status**: NOT fired by any Wave D work — `emit-state`
> (`ehrt.sim.engine`) remains the sole direct reader of `PatientState`;
> no Wave D stage introduced a second consumer of the engine/order-
> profiles boundary the sim-split plan's own S4 row names as its
> trigger. S4 stays deferred, unchanged.

---

