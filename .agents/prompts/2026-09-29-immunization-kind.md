# :immunization joins the log: the Vaccine fact becomes a ground-truth kind (ADR-0182)

> Archived verbatim (charter R-A). The session it drove is
> `.agents/session-records/2026-09-29-immunization-kind.md`, which
> records where the live tree differed from this prompt: step 3's
> producer could not land in clinic-decade (it authors no steps, and any
> added visit draws the shared `:facility` stream), and the author ruled
> it into a test-only oracle root exercising both routes, out of
> `gated-runs`; the gmf-interpreter.md section 1 table carried no Vaccine
> row to update, so one was added.

Context: the GMF interpreter emits `:vaccine {:codes :series}` (gmf_interpreter.clj:2046, Wave I,
ADR-0040 AR-5); compile-trajectory has no clause and drops it at `:else` (emittable_events.clj:93-98
names the pair with :allergy-onset). ZERO of 31 vendored modules carry a Vaccine state and vaccine is
not a pre-horizon fact kind, so no shipped corpus has ever contained it: promotion moves no existing
byte and needs a PRODUCER to be exercised. Downstream asked (2026-09-29) for subject, time, coded
product, series when present; nothing synthesized. Baseline 01a87e11, schema 1.9.0.

Read first:
- components/sim-model/src/ehrt/sim_model/pathway.clj:200-235
- components/patient-simulator/src/ehrt/patient_simulator/compile_trajectory.clj:255-300, 590-655
- components/patient-simulator/src/ehrt/patient_simulator/emittable_events.clj:85-112; gmf.clj:190-215; gmf_interpreter.clj:2040-2048
- components/sim-engine/src/ehrt/sim_engine/decide.clj:1648-1660; event_schema.clj:373 (version), 892-925
- components/sim-check/src/ehrt/sim_check/check.clj:1355-1385; components/corpus/src/ehrt/corpus/operators.clj:900-910
- components/sim-engine/test/ehrt/sim_engine/engine_test.clj (the 2026-09-29 outpatient-order tests: the run/run + check-all shape)
- docs/formats.md:388, 795-815; components/sim/docs/sim-theory.edn (pathway-ir union);
  components/sim/docs/patient-state-model.md (validity table);
  components/patient-simulator/docs/gmf-interpreter.md section 1 table; notes/adr/0178-*.md
- demos/scenarios/clinic-decade/config.edn; components/sim/test/ehrt/sim/run_test.clj:427-470 (gated-runs)

Author ruling 2026-09-29: "For B, before describe" -- the immunization slice lands now, describe after.
Channel expectations (not rulings; correct from the tree): kind name `:immunization`; `:series` carried
ONLY when the module JSON states it (absent, never 0-default, never nil -- ADR-0178); no HL7 registry
entry this slice (absent = no message, the shipped pattern); no FHIR resource this slice.

Steps:
1. Red tests. (a) IR: `{:type :immunization :codes [{:system :cvx ...}] :series 1 :citation ...}` validates
   as a pathway step, `:series`/`:citation` optional; (b) engine: `run/run` on a pathway authoring it
   emits exactly one `:immunization` inside the open encounter and `check-all` is `result/ok?` (the
   in-run self-check is the `sim` brick's; this is the same check); (c) module: a hand-built test module
   JSON with a Vaccine state (with and without "series") compiles to the step and the event carries
   `:series` only in the first; (d) law: an immunization with no open encounter fires
   `clinical-content-only-when-admitted`.  Invariant: all red at 01a87e11 for the stated reasons only.
   Gate: brick:sim-model, brick:patient-simulator, brick:sim-engine, brick:sim-check show these only.
2. The kind: pathway IR entry; `vaccine->step` at the compile dispatch (`:else` no longer sees
   :vaccine); interpreter emits `:series` only when the state carries it; `decide :immunization` (no
   draws, `_streams`); schema `kind :immunization` -- `[:active-mrn :string] [:codes [:vector Concept]]
   [:series {:optional true} :int] [:citation {:optional true} Citation]`, "no state change"; version
   1.9.0 -> 1.10.0 with changelog quoting `classify-change`, baseline re-frozen, export regenerated;
   emittable_events row `:vaccine -> #{:immunization}`; both kind sets (check.clj:1379,
   operators.clj:909) gain it.  Invariant: `git diff -- components/sim-engine/src/ehrt/sim_engine/
   run.clj` empty; no new draw site.  Gate: step 1 green; poly check OK; the five bricks green.
3. Producer: one authored `:immunization` step in a clinic-decade booked visit (a CVX code already in
   the tree, or added with its display; nothing invented). Measure which oracle roots read that config.
   Invariant: `bin/ground-truth-bracket 01a87e11 HEAD --declared-digest-change` DIFFERS only on roots
   whose config changed, by exactly the new events, IDENTICAL on every other; `bin/regression-oracle`
   the same split; both lists in the record.  Gate: banners.
4. Docs: formats.md vocabulary line + per-kind section (via the schema `:doc`, re-freeze if the baseline
   carries it); sim-theory.edn union; patient-state-model.md validity row; gmf-interpreter.md section 1
   Vaccine row now reaches the log; future-features.md moves immunization to landed if listed.
   Invariant: `make docsgen && git diff --exit-code` clean; exercised-sources gate green.
5. ADR-0182 (Accepted): the kind, its fields, "series when stated", the no-message decision, and the
   follow-ons owed (VXU^V04, FHIR Immunization, allergy by the same path). No roadmap row; the ADR is
   the record.  Gate: notes/ADRs.md index regenerated.
6. Not docs-only: full `make test` unpiped, MAKE_EXIT recorded, wrapper ends `exit "$MAKE_EXIT"`.
   Record + prompt archive. Push; gh run view the tip; CI green is the close marker. Record carries the
   downstream line: kind, fields, schema 1.10.0, "no existing corpus byte moves; the clinic-decade demo
   gains N events".
