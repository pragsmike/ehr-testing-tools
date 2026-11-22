# Session: a medication order carries its prescription -- dose, duration, refills, as-needed (ADR, next free number; event schema 1.14.0, additive)

Context. A `:medication-order` carries its codes, a citation and an optional `:chronic`. The vendored modules author more, in a `prescription` block the loader keeps and nothing reads (gmf.clj 1249 says so); and dense-7500's own four authored orders state no regimen at all, which is where nearly every order in a dense log comes from (59 of some 20,000 orders at 28,024 are module-written). This session carries the block to the event in the modules' own layout, and authors a regimen on each of dense's four orders, correcting two drugs that do not fit their pathway. NO behaviour changes: no dispense reads the prescription yet, no draw is added, no population moves. The 2026-11-17 disclosure session left "the dropped prescription fields" as undecided, declared limitation or owed; this settles it as owed and paid. Stability across revisions is relaxed by standing ruling (ADR-0189); determinism at one revision is not. Base `19ba99df` (verify HEAD == origin/main, clean tree, `bin/preflight`), event schema 1.13.0. HALT only on a premise mismatch that changes what is built; report every other difference and carry on.

Read first (ranges at `19ba99df`):
- .agents/handoffs/handoff-2026-11-22.md (the owed list); notes/adr/0191-*.md with its addendum (how `:chronic` was declared, carried and typed -- the pattern to repeat)
- components/patient-simulator/src/ehrt/patient_simulator/gmf.clj 1240-1255; the interpreter's medication-order event; compile_trajectory.clj 300-310, 645-655; components/patient-simulator/docs/limitations.md 28-48
- components/sim-model/src/ehrt/sim_model/pathway.clj 224-247; components/sim-engine/src/ehrt/sim_engine/event_schema.clj 1100-1122; decide.clj `decide :medication-order`; state.clj (MedicationOrderRecord)
- demos/scenarios/dense-7500/config.edn 141-185 (`dense-inpatient`), 252-290 (the two clinic pathways that write an order); the scenario README's pathway descriptions
- components/sim-emit-fhir/src/ehrt/sim_emit_fhir/emit_fhir.clj 220-240 (MedicationRequest; read only)
- the last figures drill: `.agents/session-records/2026-11-22-refill-tail-off-by-default.md`

Author ruling, verbatim, 2026-11-22: "Module's layout." -- to: "whether the field shape follows the modules' own layout, which is the cheaper option, or a FHIR-like one ... I'd take the modules' layout and map it at the emitter." Given in reply to a proposal of two sessions, of which this is the first: "Session 1: parts 1 and 2. One additive schema bump; dense moves only by added fields and corrected drugs", with "Orders with no authored detail would stay without it. I would not invent defaults for them." and regimens "from general clinical knowledge", labelled provisional, with "codes ... from ones already in the tree".

Channel facts (measured 2026-11-22 by parsing the 66 module files; verify):
- 134 MedicationOrder states. 83 hold a `prescription`: 32 with `dosage` (amount, frequency, period, unit) and `duration` (quantity, unit), 9 of those with `instructions`; 51 with `as_needed` only (30 true); 33 with `refills` (32 are 0, one is 6). 51 hold none.
- `dense-inpatient` admits for "Acute abdominal pain" with a fever, then orders Penicillin V 500 MG Oral Tablet at `:antibiotic-start` and Alendronic acid 10 MG Oral Tablet at `:analgesia-start`. Neither fits. `dense-clinic-acute` (reason "Sinusitis") orders Amoxicillin 250 MG / Clavulanate 125 MG; `dense-clinic-chronic` (reason "Diabetes review") orders extended-release metformin 500 MG, chronic.

The channel's regimens (provisional; the channel is not a clinician; each code below was read from a vendored module today -- confirm code and display against the module file before writing it):
- `dense-inpatient` `:antibiotic-start`: ceftriaxone 1000 MG Injection (1665021, uti/abx_tx), 1 dose once a day, 5 days.
- `dense-inpatient` `:analgesia-start`: Acetaminophen 325 MG Oral Tablet (313782, medications/otc_pain_reliever), 2 tablets four times a day, as needed.
- `dense-clinic-acute`: amoxicillin 875 MG / clavulanate 125 MG Oral Tablet (562508, uti/abx_tx), 1 tablet twice a day, 7 days, no refills.
- `dense-clinic-chronic`: the metformin it has (860975), 1 tablet once a day, 30 days, 5 refills.

Channel expectations (not rulings; correct from the tree and report):
- One optional `:prescription` map on the IR step and on the `:medication-order` event, keys kebab-cased from the module's (`:dosage {:amount :frequency :period :unit}`, `:duration {:quantity :unit}`, `:refills`, `:as-needed`, `:instructions`), each key optional, the map absent when the author wrote none. `classify-change` should call this additive; if it does not, quote it and say what a 1.13.0 log does against 1.14.0.
- Carried exactly as authored: no unit conversion, no derived quantity, no default.
- No draw, no new event, no change to any dispense or refill. Swapping two drug codes changes those orders' `:codes` and nothing downstream of them.

Steps:
1. Measure and predict, before any edit: confirm the module census above; for each oracle root and the dense 750 cell, how many in-log orders and pre-horizon order facts will gain a `:prescription`; the roots that will move and those that will not.
   Invariant: tree untouched. Gate: the census and both lists, committed ahead of step 2.
2. Red tests. (a) a vendored module's order with dosage and duration reaches the event with the same values; one with `as_needed` only carries only that; one with no block carries no `:prescription` key. (b) an authored pathway order's `:prescription` reaches the event. (c) the schema refuses a malformed dosage and an unknown key. (d) pre-horizon order facts carry it too, or the record says why they cannot. (e) no dispense, fill count or draw differs between a run with the block authored and one without.
   Invariant: red at the base for the stated reasons only. Gate: the bricks.
3. Build it: loader declaration, interpreter, compile, IR, decide, event schema 1.13.0 -> 1.14.0 with `classify-change` quoted, baseline re-frozen, export and examples regenerated. Gate: step 2 green; `poly check`.
4. dense-7500: the four regimens and the two corrected drugs in `config.edn`, comments saying provisional and where each code came from; siblings by their derivation rules.
   Invariant: step counts, delays, citations and weights unchanged; at 750, the event count, every dispense and every `:t` equal the base's -- only `:codes` and `:prescription` on medication orders differ. Gate: that comparison, stated.
5. Whatever the suite shows moved, each in its own commit: oracle roots against step 1's prediction, dense's figures by the usual drill (28,024 included; keep `37582a56...` labelled as the value before this ADR), pins, the README's pathway descriptions.
   Invariant: no figure in a consumer doc is one this session did not measure. Gate: the figures gate.
6. Docs: formats.md for the field; the consumer docs say an order may carry a prescription, that absence means the author stated none, and that nothing yet computes a quantity or a days-supply from it; the limitations register and gmf.clj's comment say what is now true.
7. ADR (Accepted, next free number): the field and its layout; carried as authored; why nothing is defaulted; dense's regimens as provisional. Owed next, named: a quantity and a days-supply on a dispense; the refill interval taken from the supply and the chain bounded by authorised refills (both unruled, they replace ruled defaults); dosage on the FHIR MedicationRequest; `dense-inpatient`'s care plan, procedure and report codes, which still describe a fracture in an abdominal-pain stay.
8. Not docs-only: full `make test` unpiped with MAKE_EXIT recorded, then `make integration`; nothing written into the tree while either runs. Archive this prompt under the same slug; record; `make docsgen`; push; `bin/post-push-verify 19ba99df <tip>`; `gh run watch --exit-status`; CI green is the close marker. The record carries the downstream line. Report: the shas, the CI run id, step 1 against what moved, step 4's comparison, the new figures, and every premise this prompt got wrong.
