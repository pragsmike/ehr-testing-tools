# :allergy-onset joins the log and the registration facts: the AllergyOnset fact becomes ground truth (ADR-0184)

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-09-30-allergy-onset-kind.md`, which
> records where the live tree differed from this prompt: the loader did
> NOT drop `allergy_type`/`category`/`reactions` (its state schema is an
> open map, so they passed through -- undeclared, the reaction Codes
> un-normalized), so step 1's loader test asserts the normalized shape;
> and `classify-change` called 1.11.0 BREAKING (the nested
> `:pre-horizon-facts` enum widening), so the bump was owed, not merely
> taken.

Context: the interpreter emits `:allergy-onset {:codes}` (gmf_interpreter.clj:2040, Wave I, ADR-0040
AR-5) and compile-trajectory drops it at `:else`, the pair of :vaccine that ADR-0182 promoted. The loader
reads only `codes` and `target_encounter` (gmf.clj:1335); `allergy_type`, `category`, `reactions` are
declared upstream and dead past the loader. ZERO of 31 vendored modules carry AllergyOnset or AllergyEnd,
so nothing shipped moves and the producer is a fixture (the ADR-0182 pattern: oracle root `immunization`
via engine-pair + an engine test). Downstream's first-priority domain fact (2026-09-18): allergy /
substance / reaction, nothing synthesized. Baseline 6641c017, schema 1.10.0, describe 1.1.0.

Read first:
- notes/adr/0182-*.md (the whole precedent: producer, series-when-stated, no-message decision)
- components/patient-simulator/src/ehrt/patient_simulator/gmf.clj:170-200, 1330-1340 (loader); gmf_interpreter.clj:2036-2048
- components/patient-simulator/src/ehrt/patient_simulator/compile_trajectory.clj:255-300, 360-380 (pre-horizon-fact-types), 590-655 (dispatch, the :vaccine clause)
- components/patient-simulator/src/ehrt/patient_simulator/emittable_events.clj:85-112
- components/sim-model/src/ehrt/sim_model/pathway.clj (the :immunization IR entry); components/sim-engine/src/ehrt/sim_engine/decide.clj (decide :immunization), event_schema.clj:401-440 (PreHorizonFact), the :immunization kind, :405 (version)
- components/sim-check/src/ehrt/sim_check/check.clj:1355-1385; components/corpus/src/ehrt/corpus/operators.clj:900-910
- components/sim-check/src/ehrt/sim_check/describe_catalog.clj (the immunization opt-in row); components/oracle/src/ehrt/oracle/digest.clj (the immunization root)
- the 2026-09-29 immunization session record and its fixture under patient-simulator test fixtures

Author ruling 2026-09-30: "Allergy". Channel expectations (not rulings; correct from the tree): kind name
`:allergy-onset` (mirrors the trajectory event; leaves `:allergy-end` for AllergyEnd later); fields
`:codes` (the substance), `:category` and `:allergy-type` ONLY when the module states them, `:reactions`
as `[{:codes [...]}]` ONLY when stated and WITHOUT severity (no draw), `:citation`; pre-horizon onsets
become registration facts; no HL7 registry entry, no FHIR resource this slice.

Steps:
1. Red tests. (a) loader: an AllergyOnset with allergy_type, category and two reactions loads with all
   three; one with none loads with none -- red because the loader drops them. (b) IR: the authored step
   validates, optional keys optional. (c) engine: `run/run` on a pathway authoring it emits one
   `:allergy-onset` inside the open encounter, `check-all` `result/ok?`. (d) module fixture: two
   AllergyOnset states (full, bare) inside a wellness encounter compile to steps; a third BEFORE the
   horizon becomes a `:pre-horizon-facts` entry with `:event :allergy-onset` and never a log event.
   (e) law: an onset with no open encounter fires `clinical-content-only-when-admitted`.
   Invariant: red at 6641c017 for the stated reasons only.  Gate: the five bricks show these only.
2. The kind: loader reads the three fields (optional); interpreter emits them when present (no draw --
   `_streams`/no rng use, asserted by a test that the patient stream position is unchanged across the
   step); `allergy-onset->step`; `:allergy-onset` joins `pre-horizon-fact-types` AND PreHorizonFact's
   `:event` enum; IR entry; `decide :allergy-onset` (no draws); `evolve` no-op; schema kind + version
   1.10.0 -> 1.11.0 with `classify-change` quoted, baseline re-frozen, export and examples regenerated,
   fleet fixture gains one state; emittable_events row; both kind sets gain it.
   Invariant: `git diff -- components/sim-engine/src/ehrt/sim_engine/run.clj` empty.  Gate: step 1 green; poly check.
3. Producer: fixture module (SNOMED substance codes already in the tree or added with display; one full
   state, one bare, one pre-horizon) + a weighted authored pathway step; oracle root `allergy` via
   engine-pair as a FIRST BASELINE; engine test through `run/run` + `check-all` asserting the counts and
   which carry `:reactions`/`:category`.  Invariant: `bin/ground-truth-bracket 6641c017 HEAD
   --declared-digest-change` and `bin/regression-oracle` IDENTICAL on every existing root, the new root
   target-side only; root-count pins move.  Gate: banners.
4. Describe: an `opt-in allergy-onset` family row beside immunization's; `:describe-version` 1.1.0 ->
   1.2.0 (additive row); golden regenerated, diff shown.  Gate: brick:sim-check.
5. Docs: formats.md kind section + the PreHorizonFact enum text (the nested-`:event` hazard paragraph
   now lists seven values); sim-theory contract; validity table; gmf-interpreter.md §1 AllergyOnset row;
   the three "additive does not bump" copies (formats.md:330, glossary.md:487, event_schema.clj:37)
   corrected while the schema section is open.  Invariant: `make docsgen && git diff --exit-code` clean.
6. ADR-0184 (Accepted): fields, "when stated", reactions without severity and why, pre-horizon routing,
   no-message; follow-ons named: severity sampling (a draw site, its own ruling), `:allergy-end`,
   `Active Allergy` guard reading a register, AL1/IAM and FHIR AllergyIntolerance rendering.
7. Not docs-only: full `make test` unpiped, MAKE_EXIT recorded, wrapper ends `exit "$MAKE_EXIT"`.
   Record + prompt archive. Push; gh run view the tip; CI green is the close marker. Record carries the
   downstream line: kind, fields, schema 1.11.0, "no existing corpus byte moves; author the step in your
   own pathway to produce it today; a pre-horizon allergy rides `:registered`'s `:pre-horizon-facts`."
