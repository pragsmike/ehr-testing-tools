# patient-simulator: scope and declared limitations

**Realistic EHR message traffic is the priority; patient-lifetime
simulation is relevant only inasmuch as it contributes to realistic
traffic.**

That sentence is this component's charter, and it is the front door
because every other document here answers a narrower question. It is
also the reason this component is named for what it *is* rather than
for what it computes: `patient-simulator` simulates patients so that
something downstream has realistic traffic to emit, and the quality bar
for anything it does is set by that downstream, not by fidelity to a
patient's biography for its own sake.

## Dependency direction

Traffic consumes patient simulation, never the reverse. This component
loads GMF modules (`gmf.clj`), interprets a lifetime out of them
(`gmf_interpreter.clj`), and compiles the horizon slice of that
lifetime into pathway IR (`compile_trajectory.clj`). It depends on
`sim-model` and `kernel` only. It does not know that `sim-engine`,
`sim-emit-hl7` or `sim-emit-fhir` exist, and none of those requires
anything from it -- the coupling runs one way, through the pathway-IR
data contract, wired by `sim`. So a limitation here is a limitation in
the *input* to traffic generation, and the honest question to ask of
each one below is not "is the biography wrong?" but "does the traffic
come out less realistic because of it?"

## Deliberate limitations

Each row is a gap this project declined ON PURPOSE, with the reason and
the citation that records the decision. A row's TRIGGER, where it has
one, is the condition under which the decline stops being defensible.
Most have none: the gap is permanent under the mission sentence above.

`ehrt.docs-tooling.patient-simulator-charter-test` gates this table two
ways: every citation below must resolve (the quoted text must occur
verbatim in the named file), and every deliberate-limitation marker in
this component's own `src` -- `UNDECLARED`, `DELIBERATELY`, `not
ported` -- must be covered by a citation into its own comment block. A
new marker with no row here is red.

| Limitation | Why declined | Citation | Trigger |
| --- | --- | --- | --- |
| **Care plans referenced by attribute never resolve their start.** `CarePlanStart`'s `assign_to_attribute` and `CarePlanEnd`'s `referenced_by_attribute` are real upstream fields this loader does not declare, so a `CarePlanEnd` that cites its start by attribute resolves neither `:care-plan-citation` nor `:start-event-id`, and its plan stays `:active` in ground truth forever. Observed: 7/7 `:care-plan-end` events in the event-log census, from 4 of 12 vendored `CarePlanEnd` states. | Care-plan state reaches NO emitter surface. HL7v2 has no CarePlan-equivalent segment and the emitter deliberately renders nothing; the FHIR emitter has no care-plan path at all. A defect no wire can show is not costing traffic realism today. The author ruled 2026-08-20: name the possibility, spend nothing on it. | `components/patient-simulator/src/ehrt/patient_simulator/gmf.clj` "stay UNDECLARED here"; `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` "no real CarePlan-equivalent segment"; `components/sim-check/src/ehrt/sim_check/check.clj` "a care plan legitimately continues (and" | **Two-part, either half sufficient.** Fix is owed when any emitter surface renders care-plan state: (a) a FHIR CarePlan resource, or (b) a render-time patient-context feature reachable by site-profile Z bindings. The realistic (b) is a facility Z segment fed by care-plan state -- ADT-driven care coordination: case-management enrollment, pathway membership on admission/discharge feeds. Note the order: without this fix, such a surface would render EVERY plan ever started as active -- a plausible-looking lie, worse than absence. Priced fix: port the `assign-to-attribute`/`referenced-by-attribute` pair at the loader and resolve it at the interpreter, the shape `:medication-order`/`:medication-end` already carries. Predicted contract-neutral (no event-shape change, additive fields only) but a DECLARED oracle change on every root drawing `bronchitis` or `injuries`. |
| **`MedicationOrder`/`CarePlanStart` `:reason` is validated and then dropped.** The field loads (a real GMF field the vendored closures author) and is never resolved past the loader; upstream resolves it three ways -- attribute, PriorState, ConditionOnset. | A resolved `reason` would change no message. `:reason` on a compiled encounter was itself a defect until ADR-0151 dropped the nil key; module-compiled encounters carry no reason by design, and no emitter renders a medication or care-plan reason. | `components/patient-simulator/src/ehrt/patient_simulator/gmf.clj` "resolution is not ported" | None. Fix owed only if an emitter grows a reason-bearing segment (an HL7v2 ORC-16 / RXE order-reason, or a FHIR `reasonReference`). |
| **`ConditionEnd` resolves only a direct `condition_onset` state citation.** The attribute-referenced form loads without schema failure (the state maps are open) and simply does not resolve, leaving CompileTrajectory's condition annotation codeless. Witnessed at `sinusitis.json`'s `Sinusitis_Ends`. | The event is still real and still in the ground-truth log; only its annotation's codes are missing, and a codeless annotation degrades one DG1 rather than losing a message. Cheap to live with, and the vendored set exercises the direct form overwhelmingly. | `components/patient-simulator/docs/gmf-interpreter.md` "a reference shape v1's interpreter does not resolve" | None declared. It becomes a real traffic defect the day diagnosis-list rendering (DG1/billing, gated on the SNOMED-ICD10 map) lands, because a codeless annotation then reaches the wire. |
| **`VitalSign` `expression` is rejected at load, not ignored.** A `VitalSign` state carrying an `expression` field fails the module load with `:vital-sign-expression-unsupported`; the field is deliberately absent from the schema so nothing can silently no-op it. | `expression` is upstream's CQL-evaluation branch and this project has no CQL evaluator. Rejecting loudly at load is the honest disposition: an authored expression is real intended clinical content, and silently emitting nothing for it would put a fabricated-by-omission vital sign on the wire. | `components/patient-simulator/src/ehrt/patient_simulator/gmf.clj` "DELIBERATELY UNDECLARED -- `vital-sign-expression?` (above) rejects"; `components/patient-simulator/src/ehrt/patient_simulator/gmf.clj` "no CQL expression evaluator" | None. A CQL evaluator is its own project, not a gap in this one. |
| **`Physiology` (waveform/ECG simulation) is unbuilt and unplanned.** One vendored-catalog module (`gallstones.json`) carries one such state. | Waveform physiology produces no messages. There is no ADT, ORU or FHIR shape in this project's emitters that a simulated ECG trace would render into, so the whole mechanism would sit at zero traffic value. | `components/patient-simulator/docs/gmf-interpreter.md` "Waveform/physiological simulation (ECG traces)" | None. Cited for completeness only. |
| **Lookup-table demographic columns are an honest absence, not a default.** A `lookup_table_transition` whose CSV keys on `race`, `state` or `socioeconomic_category` resolves only when the run's persona config asked for that draw; otherwise the walk stops at the boundary with a typed absence rather than substituting a value. | Those three persona fields are deliberately optional and config-gated -- a narrow, documented set this project draws only when a run asks for it. Inventing a demographic to keep a walk moving would put fabricated census data into ground truth, which is the one thing a testing corpus may not do. | `components/patient-simulator/src/ehrt/patient_simulator/gmf_interpreter.clj` "HONEST ABSENCE"; `components/sim-model/src/ehrt/sim_model/persona.clj` "a deliberate, narrow, documented" | None. The sibling `time`-column gap is separately registered as `roadmap.md#lookup-column-time-next`. |
| **A death mints no death-specific event kind.** `Death` states parse and walk; at compile time they add no IR step type. Death inside an encounter reuses the existing `:discharge` step with `:disposition :expired` and cause-of-death `:codes`; death outside any encounter closes the pathway and compiles to nothing. | This one is realism, not a shortfall. Real HL7v2 models a death as an ordinary `ADT^A03` whose PV1-36 carries an expired disposition code. A death-specific event kind would be an invention with no wire counterpart. | `components/patient-simulator/src/ehrt/patient_simulator/compile_trajectory.clj` "NO new IR step type" | None. |
| **A patient's pre-horizon lifetime is compressed, not emitted.** Everything the GMF walk crosses before the run's registration instant is either dropped (`:encounter`/`:encounter-end`/`:procedure`/`:observation`) or condensed into registration-time facts riding the engine's own `:registered` event (`:condition-onset`/`:condition-end`/`:medication-order`/`:medication-end`). The uncompiled trajectory keeps the full biography for glass-box traceability. | This is the mission sentence applied at its sharpest. A hospital does not emit messages for care delivered elsewhere years ago; it carries the resulting facts on the patient's record. Emitting pre-horizon encounters as traffic would be the realism defect, not the fix for one. | `components/patient-simulator/src/ehrt/patient_simulator/compile_trajectory.clj` "becomes a REGISTRATION-TIME FACT instead" | None. |

## What this table is not

It is not a coverage report. The GMF state types, transition kinds and
condition types this component *does* execute are tabled in
[`gmf-interpreter.md`](gmf-interpreter.md); what a survey found and did
not build is in
[`gmf-interpreter-findings.md`](gmf-interpreter-findings.md); the
loader's own source model is
[`gmf-source-model.md`](gmf-source-model.md); and the end-to-end walk
is [`trajectory-computation.md`](trajectory-computation.md). This table
holds only the gaps that were CHOSEN, with the reasoning that chose
them, so that a future session reads the decision instead of
re-deriving it -- and, where a decision has an expiry condition, reads
that too.
