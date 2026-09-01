# The repoint pass manifest

Derived at `9dffb2b`, before any edit of the pass it governs. Program:
`roadmap.md#engine-namespace-extraction-and-apply-unification` (P5),
backlogs (1) THE RULED REPOINT PASS, (2) C10 RESIDUAL SURFACES, (3) THE
RETIREMENT INVENTORY. Doctrine:
`roadmap.md#engine-emit-namespace-extraction` under `## Done`.

**Ruling C12(b), 2026-08-31**: C1(a)'s test-file fence LIFTS for the
pass and resumes at its close. The pass **repoints, retires and
corrects**; it does not improve
(`.agents/rulings.md#R-move-not-improve` still governs everything the
manifest does not name).

Every number below was derived from THIS tree by a reader-aware scanner
that separates CODE from PROSE -- string and `;`-comment spans blanked
in one pass and kept in the other -- never by `git grep` over raw lines,
which cannot tell a docstring citation from a call site. The scanner's
own facade def table is read out of the two files rather than
transcribed from the census, whose figures are at `517a96d`.

## The two facades, as they stand at `9dffb2b`

| facade | file | lines | top-level defs |
|---|---|---:|---:|
| `ehrt.sim-engine.engine` | `components/sim-engine/src/ehrt/sim_engine/engine.clj` | 741 | 43 |
| `ehrt.sim-emit-hl7.emit-hl7` | `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` | 383 | 26 |

All 69 defs delegate; neither file has an in-file caller of any of them.
The def-to-owner map the whole manifest resolves against is the two
files' own text, one owner per def, no def delegating to two.

## What the pass may not touch

* **`interface.clj`, either component, is FENCED** -- no edit of any
  kind, and every var either interface resolves through KEEPS its
  delegating def. That is 12 of `engine.clj`'s 43 and 16 of
  `emit_hl7.clj`'s 26, read out of the two interface files rather than
  assumed.
* **`engine_test.clj`'s `with-redefs [engine/stream ...]` site stays
  `engine/stream`.** Census constraint 1: `run.clj` resolves
  `'ehrt.sim-engine.engine/stream` through a lazy shim precisely so that
  redefinition is seen. Repointing it to `streams/stream` would redefine
  a var `run` does not call -- a silent behaviour change, and the one
  form in the whole pass where the obvious rewrite is wrong. The
  `engine/stream-seed` call INSIDE that `with-redefs` body is a plain
  read and does repoint.
* **Frozen surfaces are out of population**, not merely excluded:
  `notes/adr/`, `notes/prompts/`, `notes/sim/`, `notes/tools/`,
  `.agents/session-records/`, `.agents/prompts/`, `notes/ADRs.md`, and
  dated one-shot plan files.
* **No behaviour change, no rename, no improvement beyond this
  manifest.** `bin/regression-oracle` IDENTICAL at every commit, with no
  `--declared-digest-change`; a delta is a defect and stops the pass.

## (i-a) Prose citations naming a facade for a moved form

Every site is `<facade>/<name>` in a comment, docstring or markdown/EDN
prose, resolved against the facade's own def table above. **Frozen
surfaces are out of population** -- `notes/adr/`, `notes/prompts/`,
`notes/sim/`, `notes/tools/`, `.agents/session-records/`,
`.agents/prompts/`, `notes/ADRs.md` and dated one-shot plans
(`rulings.md#R-rename-frozen-records-keep-old-name`, and
`roadmap_lint_test`'s own `live-scan-roots` boundary: *dated one-shot
files and frozen archives are out of population*).

| file | sites | owning namespaces this file's citations move to |
|---|---:|---|
| `components/corpus-io/src/ehrt/corpus_io/er7_fields.clj` | 1 | `ehrt.sim-emit-hl7.segments` |
| `components/docs-tooling/src/ehrt/docs_tooling/trace_capture.clj` | 1 | `ehrt.sim-engine.streams` |
| `components/patient-simulator/docs/gmf-interpreter-findings.md` | 1 | `ehrt.sim-engine.state` |
| `components/patient-simulator/docs/trajectory-computation.md` | 6 | `ehrt.sim-engine.evolve`, `ehrt.sim-engine.run`, `ehrt.sim-engine.state` (+1 partial-token/possessive, resolved by hand) |
| `components/patient-simulator/src/ehrt/patient_simulator/gmf.clj` | 1 | `ehrt.sim-engine.assignment` |
| `components/patient-simulator/src/ehrt/patient_simulator/gmf_interpreter.clj` | 1 | `ehrt.sim-engine.assignment` |
| `components/person-simulator/src/ehrt/person_simulator/hazards.clj` | 1 | `ehrt.sim-engine.assignment` |
| `components/person-simulator/src/ehrt/person_simulator/persona.clj` | 1 | `ehrt.sim-engine.streams` |
| `components/person-simulator/test/ehrt/person_simulator/consumption_test.clj` | 1 | `ehrt.sim-engine.assignment` |
| `components/sim-check/src/ehrt/sim_check/check.clj` | 6 | `ehrt.sim-engine.decide`, `ehrt.sim-engine.fold`, `ehrt.sim-engine.run`, `ehrt.sim-engine.streams` (+2 partial-token/possessive, resolved by hand) |
| `components/sim-check/test/ehrt/sim_check/person_invariants_test.clj` | 1 | `ehrt.sim-engine.run` |
| `components/sim-emit-fhir/src/ehrt/sim_emit_fhir/emit_fhir.clj` | 6 | `ehrt.sim-emit-hl7.hl7-time`, `ehrt.sim-engine.fold`, `ehrt.sim-engine.state`, `ehrt.sim-engine.streams` (+2 partial-token/possessive, resolved by hand) |
| `components/sim-emit-fhir/test/ehrt/sim_emit_fhir/emit_fhir_test.clj` | 1 |  (+1 partial-token/possessive, resolved by hand) |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit.clj` | 2 | `ehrt.sim-engine.config` (+1 partial-token/possessive, resolved by hand) |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/messages.clj` | 1 | `ehrt.sim-engine.config` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/planners.clj` | 4 | `ehrt.sim-engine.assignment`, `ehrt.sim-engine.config` (+2 partial-token/possessive, resolved by hand) |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/segments.clj` | 1 | `ehrt.sim-engine.run` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/site_profile.clj` | 3 | `ehrt.sim-engine.config`, `ehrt.sim-engine.run` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/timelines.clj` | 2 | `ehrt.sim-engine.state` (+1 partial-token/possessive, resolved by hand) |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/v2_replay.clj` | 5 | `ehrt.sim-emit-hl7.er7` (+4 partial-token/possessive, resolved by hand) |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/charges_test.clj` | 1 |  (+1 partial-token/possessive, resolved by hand) |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/emit_hl7_test.clj` | 1 | `ehrt.sim-engine.config` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/siu_test.clj` | 2 |  (+2 partial-token/possessive, resolved by hand) |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/v2_replay_test.clj` | 3 | `ehrt.sim-emit-hl7.er7` (+2 partial-token/possessive, resolved by hand) |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_anemia_test.clj` | 1 | `ehrt.sim-engine.run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_colorectal_test.clj` | 3 | `ehrt.sim-engine.run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_ear_infections_test.clj` | 1 | `ehrt.sim-engine.run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_injuries_test.clj` | 2 | `ehrt.sim-engine.run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_sepsis_test.clj` | 1 |  (+1 partial-token/possessive, resolved by hand) |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_tjr_test.clj` | 1 | `ehrt.sim-engine.run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_uti_test.clj` | 1 | `ehrt.sim-engine.run` |
| `components/sim-engine/src/ehrt/sim_engine/churn.clj` | 1 | `ehrt.sim-engine.run` |
| `components/sim-engine/src/ehrt/sim_engine/engine.clj` | 2 | `ehrt.sim-engine.streams` |
| `components/sim-engine/src/ehrt/sim_engine/event_schema.clj` | 1 | `ehrt.sim-engine.state` |
| `components/sim-engine/src/ehrt/sim_engine/order_profiles.clj` | 1 | `ehrt.sim-engine.assignment` |
| `components/sim-engine/src/ehrt/sim_engine/run.clj` | 3 | `ehrt.sim-emit-hl7.emit`, `ehrt.sim-engine.streams` |
| `components/sim-engine/src/ehrt/sim_engine/streams.clj` | 1 | `ehrt.sim-engine.streams` |
| `components/sim-engine/test/ehrt/sim_engine/churn_scenarios_test.clj` | 5 | `ehrt.sim-engine.run`, `ehrt.sim-engine.streams` |
| `components/sim-engine/test/ehrt/sim_engine/engine_test.clj` | 10 | `ehrt.sim-engine.log-index`, `ehrt.sim-engine.run`, `ehrt.sim-engine.streams` (+5 partial-token/possessive, resolved by hand) |
| `components/sim-engine/test/ehrt/sim_engine/event_fleet.clj` | 4 | `ehrt.sim-engine.run` |
| `components/sim-engine/test/ehrt/sim_engine/persons_test.clj` | 1 | `ehrt.sim-engine.run` |
| `components/sim-model/src/ehrt/sim_model/config.clj` | 11 | `ehrt.sim-emit-hl7.planners`, `ehrt.sim-emit-hl7.registry`, `ehrt.sim-engine.config`, `ehrt.sim-engine.run` (+2 partial-token/possessive, resolved by hand) |
| `components/sim-model/src/ehrt/sim_model/pathway.clj` | 2 | `ehrt.sim-engine.run`, `ehrt.sim-engine.state` |
| `components/sim-model/src/ehrt/sim_model/persona.clj` | 3 | `ehrt.sim-engine.assignment` (+1 partial-token/possessive, resolved by hand) |
| `components/sim/docs/event-sourcing.md` | 1 | `ehrt.sim-engine.streams` |
| `components/sim/docs/patient-state-model.md` | 6 | `ehrt.sim-engine.decide`, `ehrt.sim-engine.evolve`, `ehrt.sim-engine.run`, `ehrt.sim-engine.state`, `ehrt.sim-engine.streams` |
| `components/sim/docs/sim-theory.edn` | 5 | `ehrt.sim-emit-hl7.hl7-time`, `ehrt.sim-engine.assignment`, `ehrt.sim-engine.config`, `ehrt.sim-engine.run` (+1 partial-token/possessive, resolved by hand) |
| `components/sim/docs/sim-theory.md` | 4 | `ehrt.sim-engine.config`, `ehrt.sim-engine.evolve`, `ehrt.sim-engine.run`, `ehrt.sim-engine.streams` |
| `components/sim/docs/third-party-sources.md` | 2 | `ehrt.sim-emit-hl7.er7`, `ehrt.sim-emit-hl7.registry` |
| `components/sim/src/ehrt/sim/identifiers.clj` | 2 | `ehrt.sim-engine.config`, `ehrt.sim-engine.run` |
| `components/sim/src/ehrt/sim/run.clj` | 9 | `ehrt.sim-engine.assignment`, `ehrt.sim-engine.run` (+3 partial-token/possessive, resolved by hand) |
| `components/sim/test/ehrt/sim/run_test.clj` | 17 | `ehrt.sim-emit-hl7.emit`, `ehrt.sim-engine.config`, `ehrt.sim-engine.run` (+1 partial-token/possessive, resolved by hand) |
| `demos/scenarios/clinic-decade/config.edn` | 4 | `ehrt.sim-emit-hl7.registry`, `ehrt.sim-engine.config` |
| `demos/scenarios/ed-tuesday/config-latency.edn` | 4 | `ehrt.sim-emit-hl7.registry`, `ehrt.sim-engine.config` |
| `demos/scenarios/ed-tuesday/config.edn` | 5 | `ehrt.sim-emit-hl7.registry`, `ehrt.sim-engine.assignment`, `ehrt.sim-engine.config` |
| `demos/traces/emit-state/README.md` | 1 | `ehrt.sim-engine.streams` |
| `demos/traces/order-result/README.md` | 1 | `ehrt.sim-emit-hl7.segments` |
| `demos/traces/persona-enriched/README.md` | 2 | `ehrt.sim-emit-hl7.er7`, `ehrt.sim-engine.run` |
| `docs/consuming-ground-truth.md` | 1 | `ehrt.sim-engine.streams` |
| `docs/dev/simulator-architecture.md` | 1 | `ehrt.sim-engine.fold` |
| `docs/site-profiles.md` | 2 | `ehrt.sim-engine.config`, `ehrt.sim-engine.run` |
| `projects/conformance/test/ehrt/conformance/v2_structure_resolution_test.clj` | 1 | `ehrt.sim-engine.run` |

**176 sites in 62 files.** 33 of the 176 are
possessive or line-wrapped fragments (`run's`, `documented-step-`) that
carry the same attribution and are repointed by hand with the whole
sentence read.

### Excluded from (i), each with its reason

| file | sites | why it is NOT repointed |
|---|---:|---|
| `.agents/plans/engine-extraction-census.md` | 2 | the census's own section 2 is an inventory OF engine.clj at 517a96d; historical by construction |
| `components/docs-tooling/test/ehrt/docs_tooling/sim_emit_hl7_dependency_test.clj` | 2 | same class -- the gate's own subject and its string fixture |
| `components/docs-tooling/test/ehrt/docs_tooling/stale_path_test.clj` | 3 | string fixtures the gate asserts over, plus prose whose subject IS this namespace |
| `components/patient-simulator/test/ehrt/sim/fixtures/pinned_seed_42_patients_5.edn` | 1 | capture-provenance header: what was actually invoked at capture time |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/interface.clj` | 12 | FENCED -- this session makes no interface.clj edit of any kind |

**20 sites excluded**, and
**70 more sites in frozen surfaces** are out of population
entirely rather than excluded.

## (i-a2) Bare alias-shaped prose: `engine/x`, `emit-hl7/y`

A second population the fully-qualified scan cannot see, and the one
this manifest most nearly missed: prose that writes the facade in ALIAS
shorthand. **150 such tokens exist on live surfaces.** They split three
ways, and the split is what makes them safe to act on:

* **66 name an INTERFACE, not a facade** -- the file binds
  `engine` to `ehrt.sim-engine.interface` (or `emit-hl7` to
  `ehrt.sim-emit-hl7.interface`), so the citation is TRUE and stays.
  `check.clj` is the type case. Out of population; naming them as such
  is the point, because a blind alias rewrite would have taken all
  66 of them.
* **25 are a MOVER'S OWN BANNER** in the owning namespace or
  the facade, counting what the test tree calls: *"`engine_test.clj`
  alone calls `engine/decide` at ninety sites"*. These are the
  RESIDUE-CLAIM class the doctrine names, and (ii) FALSIFIES them --
  the count stays true, the name does not. Each is corrected in
  whichever commit falsifies it: step 2 where a repoint does it, step 3
  where a retirement does.
* **59 are plain citations** on docs, demos, READMEs, the
  manual, a NOTICE, a shell script and three `src` files that never
  bound the alias at all.

**Form is preserved, not improved**: a bare citation is repointed to
the owner's own short name, a fully-qualified one to the owner
fully-qualified. That is the whole edit.

### The mover-banner half

| file | line | token | owner |
|---|---:|---|---|
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit.clj` | 41 | `emit-hl7/emit` | `ehrt.sim-emit-hl7.emit/emit` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit.clj` | 45 | `emit-hl7/emit` | `ehrt.sim-emit-hl7.emit/emit` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` | 47 | `emit-hl7/hl7-timestamp` | `ehrt.sim-emit-hl7.hl7-time/hl7-timestamp` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` | 79 | `emit-hl7/message-type-registry` | `ehrt.sim-emit-hl7.registry/message-type-registry` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` | 177 | `emit-hl7/escape-er7` | `ehrt.sim-emit-hl7.er7/escape-er7` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` | 233 | `emit-hl7/event->messages` | `ehrt.sim-emit-hl7.messages/event->messages` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/emit_hl7.clj` | 367 | `emit-hl7/emit` | `ehrt.sim-emit-hl7.emit/emit` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/fan_out.clj` | 7 | `emit-hl7/emit-wire` | `ehrt.sim-emit-hl7.emit/emit-wire` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/segments.clj` | 30 | `emit-hl7/control-id-for` | `ehrt.sim-emit-hl7.segments/control-id-for` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/site_profile.clj` | 17 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/src/ehrt/sim_engine/assignment.clj` | 45 | `engine/assign-module` | `ehrt.sim-engine.assignment/assign-module` |
| `components/sim-engine/src/ehrt/sim_engine/assignment.clj` | 45 | `engine/assign-pathway` | `ehrt.sim-engine.assignment/assign-pathway` |
| `components/sim-engine/src/ehrt/sim_engine/decide.clj` | 42 | `engine/decide` | `ehrt.sim-engine.decide/decide` |
| `components/sim-engine/src/ehrt/sim_engine/engine.clj` | 508 | `engine/replay` | `ehrt.sim-engine.fold/replay` |
| `components/sim-engine/src/ehrt/sim_engine/engine.clj` | 558 | `engine/assign-module` | `ehrt.sim-engine.assignment/assign-module` |
| `components/sim-engine/src/ehrt/sim_engine/engine.clj` | 558 | `engine/assign-pathway` | `ehrt.sim-engine.assignment/assign-pathway` |
| `components/sim-engine/src/ehrt/sim_engine/engine.clj` | 700 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/src/ehrt/sim_engine/event_schema.clj` | 501 | `engine/Demographics` | `ehrt.sim-engine.state/Demographics` |
| `components/sim-engine/src/ehrt/sim_engine/fold.clj` | 32 | `engine/replay` | `ehrt.sim-engine.fold/replay` |
| `components/sim-engine/src/ehrt/sim_engine/log_index.clj` | 35 | `engine/events-for-patient` | `ehrt.sim-engine.log-index/events-for-patient` |
| `components/sim-engine/src/ehrt/sim_engine/person_fold.clj` | 7 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/src/ehrt/sim_engine/person_fold.clj` | 27 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/src/ehrt/sim_engine/person_fold.clj` | 65 | `engine/Demographics` | `ehrt.sim-engine.state/Demographics` |
| `components/sim-engine/src/ehrt/sim_engine/person_fold.clj` | 89 | `engine/demographics-from-persona` | `ehrt.sim-engine.state/demographics-from-persona` |
| `components/sim-engine/src/ehrt/sim_engine/person_fold.clj` | 168 | `engine/run` | `ehrt.sim-engine.run/run` |

### The plain-citation half

| file | line | token | owner |
|---|---:|---|---|
| `bin/oracle-lib.sh` | 89 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/patient-simulator/docs/gmf-interpreter.md` | 380 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/person-simulator/README.md` | 38 | `engine/stream` | `ehrt.sim-engine.streams/stream` |
| `components/person-simulator/test/ehrt/person_simulator/limitations_test.clj` | 221 | `engine/stream-seed` | `ehrt.sim-engine.streams/stream-seed` |
| `components/person-simulator/test/ehrt/person_simulator/limitations_test.clj` | 233 | `engine/stream-seed` | `ehrt.sim-engine.streams/stream-seed` |
| `components/sim-check/test/ehrt/sim_check/person_invariants_test.clj` | 21 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/v2_replay_test.clj` | 242 | `emit-hl7/tn-field` | `ehrt.sim-emit-hl7.er7/tn-field` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_anemia_test.clj` | 88 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_colorectal_test.clj` | 33 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_colorectal_test.clj` | 35 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_colorectal_test.clj` | 95 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_ear_infections_test.clj` | 70 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_injuries_test.clj` | 47 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_injuries_test.clj` | 60 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_tjr_test.clj` | 15 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_uti_test.clj` | 83 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/resources/sim-engine/event-examples.edn` | 5 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/test/ehrt/sim_engine/churn_scenarios_test.clj` | 14 | `engine/patient-id-for` | `ehrt.sim-engine.streams/patient-id-for` |
| `components/sim-engine/test/ehrt/sim_engine/churn_scenarios_test.clj` | 16 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/test/ehrt/sim_engine/churn_scenarios_test.clj` | 30 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/test/ehrt/sim_engine/churn_scenarios_test.clj` | 75 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/test/ehrt/sim_engine/engine_test.clj` | 1017 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/test/ehrt/sim_engine/engine_test.clj` | 1091 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/test/ehrt/sim_engine/engine_test.clj` | 2411 | `engine/stream` | `ehrt.sim-engine.streams/stream` |
| `components/sim-engine/test/ehrt/sim_engine/engine_test.clj` | 2460 | `engine/events-for-patient` | `ehrt.sim-engine.log-index/events-for-patient` |
| `components/sim-engine/test/ehrt/sim_engine/engine_test.clj` | 2497 | `engine/stream` | `ehrt.sim-engine.streams/stream` |
| `components/sim-engine/test/ehrt/sim_engine/event_fleet.clj` | 203 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/test/ehrt/sim_engine/event_fleet.clj` | 268 | `engine/person-plan` | `ehrt.sim-engine.run/person-plan` |
| `components/sim-engine/test/ehrt/sim_engine/event_fleet.clj` | 391 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/test/ehrt/sim_engine/event_fleet.clj` | 406 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-engine/test/ehrt/sim_engine/persons_test.clj` | 8 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim-model/src/ehrt/sim_model/config.clj` | 342 | `emit-hl7/message-type-registry` | `ehrt.sim-emit-hl7.registry/message-type-registry` |
| `components/sim-model/src/ehrt/sim_model/facility.clj` | 166 | `engine/decide` | `ehrt.sim-engine.decide/decide` |
| `components/sim/docs/sim-theory.md` | 256 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim/resources/sim/modules/NOTICE` | 440 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim/resources/sim/modules/NOTICE` | 480 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim/resources/sim/modules/NOTICE` | 531 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim/test/ehrt/sim/run_test.clj` | 17 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim/test/ehrt/sim/run_test.clj` | 89 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim/test/ehrt/sim/run_test.clj` | 104 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim/test/ehrt/sim/run_test.clj` | 163 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim/test/ehrt/sim/run_test.clj` | 165 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim/test/ehrt/sim/run_test.clj` | 181 | `engine/run` | `ehrt.sim-engine.run/run` |
| `components/sim/test/ehrt/sim/run_test.clj` | 278 | `engine/run` | `ehrt.sim-engine.run/run` |
| `demos/scenarios/ed-tuesday/README.md` | 360 | `emit-hl7/plan-latency` | `ehrt.sim-emit-hl7.planners/plan-latency` |
| `demos/scenarios/ed-tuesday/README.md` | 361 | `emit-hl7/emit-wire` | `ehrt.sim-emit-hl7.emit/emit-wire` |
| `demos/scenarios/ed-tuesday/README.md` | 384 | `engine/config-keys` | `ehrt.sim-engine.config/config-keys` |
| `demos/scenarios/ed-tuesday/README.md` | 403 | `engine/run` | `ehrt.sim-engine.run/run` |
| `demos/scenarios/ed-tuesday/config-latency.edn` | 9 | `engine/config-keys` | `ehrt.sim-engine.config/config-keys` |
| `demos/scenarios/ed-tuesday/config-latency.edn` | 13 | `emit-hl7/emit-wire` | `ehrt.sim-emit-hl7.emit/emit-wire` |
| `demos/scenarios/ed-tuesday/config-latency.edn` | 153 | `engine/config-keys` | `ehrt.sim-engine.config/config-keys` |
| `demos/traces/order-result/README.md` | 17 | `engine/run` | `ehrt.sim-engine.run/run` |
| `demos/traces/order-result/config.edn` | 10 | `engine/run` | `ehrt.sim-engine.run/run` |
| `docs/dev/simulator-architecture.md` | 353 | `engine/run` | `ehrt.sim-engine.run/run` |
| `docs/dev/simulator-architecture.md` | 393 | `emit-hl7/plan-latency` | `ehrt.sim-emit-hl7.planners/plan-latency` |
| `docs/dev/simulator-architecture.md` | 397 | `emit-hl7/emit-wire` | `ehrt.sim-emit-hl7.emit/emit-wire` |
| `docs/manual/04-time-on-the-wire.md` | 99 | `engine/config-keys` | `ehrt.sim-engine.config/config-keys` |
| `projects/conformance/test/ehrt/conformance/v2_structure_resolution_test.clj` | 119 | `engine/run` | `ehrt.sim-engine.run/run` |
| `projects/integration/test/ehrt/integration/oracle_coverage_test.clj` | 65 | `engine/run` | `ehrt.sim-engine.run/run` |

**84 sites in 42 files**, and
66 out of population.


## (i-a3) WRAPPED citations, which no single-line grep can see

The eighteenth session recorded this class about the P5 slug itself --
ten of forty-one live citations wrap across a line break inside a
docstring, and a plain `git grep` finds only thirty-three. It applies to
the facades too, and a wrap-tolerant join of every adjacent line pair
(stripping the wrap indent and any `;;` marker) finds **fifteen**
straddling occurrences. Eight are already counted above, because the
fragment before the break was itself a token the line-local scan
matched; **seven are NEW and would otherwise have been missed
entirely**:

| site | citation | owner |
|---|---|---|
| `components/sim-engine/src/ehrt/sim_engine/evolve.clj:26` | `ehrt.sim-engine.engine/` + `evolve` | `ehrt.sim-engine.evolve/evolve` |
| `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/er7.clj:207` | `ehrt.sim-engine.engine/` + `replay` | `ehrt.sim-engine.fold/replay` |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/latency_test.clj:3` | `ehrt.sim-engine.engine/` + `assign-pathway` | `ehrt.sim-engine.assignment/assign-pathway` |
| `demos/scenarios/ed-tuesday/config.edn:317` | `ehrt.sim-emit-hl7.emit-hl7/` + `plan-charges` | `ehrt.sim-emit-hl7.planners/plan-charges` |
| `demos/scenarios/ed-tuesday/config-latency.edn:188` | `ehrt.sim-engine.engine/` + `config-keys` | `ehrt.sim-engine.config/config-keys` |
| `demos/scenarios/ed-tuesday/config-latency.edn:248` | `ehrt.sim-emit-hl7.emit-hl7/` + `plan-charges` | `ehrt.sim-emit-hl7.planners/plan-charges` |
| `demos/scenarios/clinic-decade/config.edn:187` | `ehrt.sim-emit-hl7.emit-hl7/` + `plan-charges` | `ehrt.sim-emit-hl7.planners/plan-charges` |

Two of the fifteen are joins that are not citations at all and are named
so they are not acted on: `v2_replay.clj:244` names `obx-segment`, which
is no facade def, and `gmf_interpreter.clj:306` sets two NAMESPACES
either side of a slash rather than citing a form.

**The prose population is therefore 176 + 84 + 7 = 267 sites**, and the
third of those three numbers is the one a session working from a plain
grep would have reported as zero.

## (i-b) Facade-BY-FILE citations: the fenced backlog, and what is NOT one

`engine.clj` and `emit_hl7.clj` are named by FILE at **178 sites** in
the tracked tree. The overwhelming majority are PROVENANCE -- a sibling
saying which file its text left, a residue banner saying what stayed --
and those are TRUE and stay. What (i) repoints is the class the records
enumerated: a citation that ATTRIBUTES A MOVED FORM to the facade file,
false the moment the form left.

Re-derived here rather than carried from the records, and two of the
record's own pointers moved:

| site | text | owner it moves to |
|---|---|---|
| `engine_test.clj:200` | "`engine`'s own `waiting-boarder` predicate" | `decide` (private mover, NO def -- nothing forwards this) |
| `engine_test.clj:1067` | "see engine.clj's :order docstring" | `decide.clj` |
| `engine_test.clj:1993` | "engine.clj's own :registered anchors registration-t" | `decide.clj` |
| `vendored_colorectal_test.clj:96` | "`engine.clj`'s own `:registered` decide method" | `decide.clj` |
| `vendored_injuries_test.clj:78` | "(`engine.clj`'s own ...)" | `decide.clj` |
| `vendored_ear_infections_test.clj:14` | "(`engine.clj`'s own `:registered` defmethod)" | `decide.clj` |
| `vendored_veteran_prostate_cancer_test.clj:59` | "`engine.clj`'s own `:registered`" | `decide.clj` |
| `vendored_sepsis_test.clj:12` | "engine.clj's own :registered event" | `decide.clj` |
| `vendored_dementia_test.clj:7` | "engine.clj anchors registration-t" | `decide.clj` |
| `vendored_allergic_rhinitis_test.clj:12` | "since `engine.clj` anchors `registration-t`" | `decide.clj` |
| `vendored_uti_test.clj:14` | "`engine.clj`'s bare 5-arity `run-module` call" | `decide.clj` |
| `vendored_uti_test.clj:22` | "`engine.clj`'s own fixed registration-t anchor" | `decide.clj` |
| `latency_test.clj:56` | "(assign-pathway/assign-module, engine.clj)" | `assignment.clj` -- the decide session's "plus one", stale since extraction 8 |
| `churn_scenarios_test.clj:56` | "(Persona, engine.clj's own docstring)" | `state.clj` -- NEW, found by this manifest |
| `pathway_test.clj:73` | "result auto-pairs ... see engine.clj" | `decide.clj` -- NEW, found by this manifest |
| `consumption_test.clj:17` | "`stream` is deliberately left unhinted in `engine.clj`" | `streams.clj` -- NEW; the hint census constraint 3 names is on `streams/stream` now |
| `emit_hl7_test.clj:1306` | "`emit_hl7.clj`'s own registry comment" | `registry.clj:41` -- the thirteenth session's row (A), test-side twin |
| `limitations_test.clj:152` | "`emit_hl7.clj`'s ..." | read in place; repointed only if it names a moved form |

**Fourteen were the records' twelve-plus-one; four are this manifest's
own additions**, which is what re-deriving rather than transcribing
buys. `engine_test.clj:2440`, `:2446` and `:2543` cite `engine.clj:480`
-- a LINE, stale for many sessions and not resolvable to an owner by
line at all; they are (v)'s business, not (i)'s, and are named there.

## (ii) Test CODE reaches through a facade

| test file | sites | owner namespace : names |
|---|---:|---|
| `components/sim-check/test/ehrt/sim_check/check_test.clj` | 14 | `decide`: documented-step-rejection-reasons; `fold`: replay; `run`: run |
| `components/sim-check/test/ehrt/sim_check/person_invariants_test.clj` | 7 | `run`: run; `streams`: stream |
| `components/sim-emit-fhir/test/ehrt/sim_emit_fhir/emit_fhir_test.clj` | 13 | `fold`: replay; `run`: run; `streams`: patient-id-for |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/charges_test.clj` | 20 | `emit`: emit, emit-wire; `planners`: plan-charges, plan-latency; `registry`: room-and-board-code |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/chatter_test.clj` | 26 | `emit`: emit, emit-wire; `planners`: plan-chatter, plan-latency; `registry`: chatter-event-kinds, message-type-registry; `segments`: control-id-for |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/emit_hl7_test.clj` | 186 | `emit`: emit; `er7`: escape-er7, unescape-er7; `hl7-time`: hl7-timestamp; `messages`: event->messages; `registry`: message-type-registry; `segments`: control-id-for, msh-segment, pid-segment; `config`: config-keys; `decide`: decide; `evolve`: evolve; `run`: run; `state`: initial-patient; `streams`: encounter-id-for, one-stream |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/emitter_order_independence_test.clj` | 2 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/event_conformance_test.clj` | 3 | `emit`: emit; `registry`: message-type-registry |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/ladders_test.clj` | 19 | `emit`: emit, emit-wire; `planners`: plan-chatter, plan-ladders; `registry`: message-type-registry, skeleton-message-types |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/latency_test.clj` | 25 | `emit`: emit, emit-wire; `hl7-time`: hl7-timestamp; `planners`: plan-latency; `segments`: control-id-for; `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/result_clock_test.clj` | 10 | `emit`: emit, emit-wire; `hl7-time`: hl7-timestamp; `segments`: control-id-for |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/siu_test.clj` | 32 | `emit`: emit, emit-wire; `registry`: message-type-registry, siu-event-kinds, siu-renders?, skeleton-message-types; `segments`: control-id-for |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/v2_replay_test.clj` | 21 | `emit`: emit; `er7`: tn-field; `registry`: message-type-registry; `fold`: replay; `run`: run; `streams`: patient-id-for |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_allergic_rhinitis_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_anemia_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_asthma_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_attention_deficit_disorder_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_bronchitis_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_colorectal_test.clj` | 2 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_dementia_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_dermatitis_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_ear_infections_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_fibromyalgia_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_hypothyroidism_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_injuries_test.clj` | 2 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_med_rec_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_metabolic_syndrome_care_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_osteoarthritis_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_osteoporosis_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_rheumatoid_arthritis_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_sepsis_test.clj` | 8 | `emit`: emit; `segments`: control-id-for; `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_sleep_apnea_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_tjr_test.clj` | 2 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_uti_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_veteran_lung_cancer_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_veteran_prostate_cancer_test.clj` | 2 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_veteran_ptsd_test.clj` | 2 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_veteran_self_harm_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_veteran_substance_abuse_treatment_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_vhd_pulmonic_test.clj` | 1 | `run`: run |
| `components/sim-emit-hl7/test/ehrt/sim_emit_hl7/vendored_vhd_tricuspid_test.clj` | 1 | `run`: run |
| `components/sim-engine/test/ehrt/sim_engine/bed_cycle_test.clj` | 7 | `messages`: event->messages; `run`: run; `streams`: stream |
| `components/sim-engine/test/ehrt/sim_engine/churn_scenarios_test.clj` | 5 | `run`: run; `streams`: patient-id-for |
| `components/sim-engine/test/ehrt/sim_engine/encounters_test.clj` | 34 | `run`: run; `streams`: encounter-id-for, mix64, patient-id-for, stream |
| `components/sim-engine/test/ehrt/sim_engine/engine_test.clj` | 394 | `assignment`: assign-module, assign-pathway; `config`: config-keys; `decide`: compile-patient, decide, person-entry; `evolve`: evolve; `fold`: replay; `log-index`: events-for-patient; `run`: run; `state`: Demographics, PatientState, demographics-from-persona, initial-patient, valid-patient?; `streams`: mix64, newborn-id-tag, one-stream, patient-id-for, stream, stream-scheme, stream-seed |
| `components/sim-engine/test/ehrt/sim_engine/event_fleet.clj` | 12 | `run`: run; `streams`: patient-id-for, stream |
| `components/sim-engine/test/ehrt/sim_engine/event_schema_test.clj` | 4 | `run`: run |
| `components/sim-engine/test/ehrt/sim_engine/persons_test.clj` | 41 | `config`: config-keys; `evolve`: evolve; `run`: person-plan, run; `state`: initial-patient; `streams`: patient-id-for, stream |
| `components/sim-engine/test/ehrt/sim_engine/scheduling_test.clj` | 18 | `config`: valid-scheduling?; `run`: run; `streams`: appointment-id-for, encounter-id-for |
| `components/sim/test/ehrt/sim/identifiers_test.clj` | 2 | `run`: run; `streams`: patient-id-for |
| `components/sim/test/ehrt/sim/run_test.clj` | 5 | `config`: config-keys; `fold`: replay; `streams`: patient-id-for |
| `projects/conformance/test/ehrt/conformance/v2_structure_resolution_test.clj` | 4 | `emit`: emit; `run`: run |

**944 code sites in 52 test files.** Every
owner alias is the owning namespace's own last segment; **an alias scan
of all 52 files finds ZERO collisions** -- the one file that already
binds `run` (`components/sim/test/ehrt/sim/run_test.clj`, to
`ehrt.sim.run`) needs `config`, `fold` and `streams` and does not need
`run`.

## (iii) Delegating defs that go caller-less AFTER (ii)

### `ehrt.sim-engine.engine` -- 43 defs

**KEEP, 12: `interface.clj` resolves through them.** `compile-patient`, `config-keys`, `documented-step-rejection-reasons`, `mix64`, `newborn-id-tag`, `person-plan`, `replay`, `run`, `stream`, `stream-scheme`, `stream-seed`, `valid-persons?`

**KEEP for a surviving SRC caller: none.**

**RETIRE, 31:**

| def | delegates to | status before (ii) |
|---|---|---|
| `ConditionRecord` | `ehrt.sim-engine.state/ConditionRecord` | already caller-less |
| `ObservationRecord` | `ehrt.sim-engine.state/ObservationRecord` | already caller-less |
| `MedicationOrderRecord` | `ehrt.sim-engine.state/MedicationOrderRecord` | already caller-less |
| `CarePlanRecord` | `ehrt.sim-engine.state/CarePlanRecord` | already caller-less |
| `Demographics` | `ehrt.sim-engine.state/Demographics` | 1 test file(s) reach it |
| `demographics-from-persona` | `ehrt.sim-engine.state/demographics-from-persona` | 1 test file(s) reach it |
| `placeholder-demographics` | `ehrt.sim-engine.state/placeholder-demographics` | already caller-less |
| `PatientLocation` | `ehrt.sim-engine.state/PatientLocation` | already caller-less |
| `EncounterRecord` | `ehrt.sim-engine.state/EncounterRecord` | already caller-less |
| `AppointmentRecord` | `ehrt.sim-engine.state/AppointmentRecord` | already caller-less |
| `PatientState` | `ehrt.sim-engine.state/PatientState` | 1 test file(s) reach it |
| `valid-patient?` | `ehrt.sim-engine.state/valid-patient?` | 1 test file(s) reach it |
| `initial-patient` | `ehrt.sim-engine.state/initial-patient` | 3 test file(s) reach it |
| `patient-id-for` | `ehrt.sim-engine.streams/patient-id-for` | 9 test file(s) reach it |
| `encounter-id-for` | `ehrt.sim-engine.streams/encounter-id-for` | 3 test file(s) reach it |
| `next-encounter-ordinal` | `ehrt.sim-engine.streams/next-encounter-ordinal` | already caller-less |
| `appointment-id-for` | `ehrt.sim-engine.streams/appointment-id-for` | 1 test file(s) reach it |
| `next-appointment-ordinal` | `ehrt.sim-engine.streams/next-appointment-ordinal` | already caller-less |
| `one-stream` | `ehrt.sim-engine.streams/one-stream` | 2 test file(s) reach it |
| `events-for-patient` | `ehrt.sim-engine.log-index/events-for-patient` | 1 test file(s) reach it |
| `decide` | `ehrt.sim-engine.decide/decide` | 2 test file(s) reach it |
| `delivery-stay-minutes` | `ehrt.sim-engine.decide/delivery-stay-minutes` | already caller-less |
| `injury-stay-minutes` | `ehrt.sim-engine.decide/injury-stay-minutes` | already caller-less |
| `unidentified-stay-minutes` | `ehrt.sim-engine.decide/unidentified-stay-minutes` | already caller-less |
| `person-entry` | `ehrt.sim-engine.decide/person-entry` | 1 test file(s) reach it |
| `evolve` | `ehrt.sim-engine.evolve/evolve` | 3 test file(s) reach it |
| `assign-pathway` | `ehrt.sim-engine.assignment/assign-pathway` | 1 test file(s) reach it |
| `assign-module` | `ehrt.sim-engine.assignment/assign-module` | 1 test file(s) reach it |
| `Persons` | `ehrt.sim-engine.config/Persons` | already caller-less |
| `Scheduling` | `ehrt.sim-engine.config/Scheduling` | already caller-less |
| `valid-scheduling?` | `ehrt.sim-engine.config/valid-scheduling?` | 1 test file(s) reach it |

Facade ends at **12 defs** (12 interface-borne).

### `ehrt.sim-emit-hl7.emit-hl7` -- 26 defs

**KEEP, 16: `interface.clj` resolves through them.** `add-on-message-types`, `chatter-event-kinds`, `control-id-for`, `default-reference-date`, `default-utc-offset`, `emit`, `emit-wire`, `emittable-message-types`, `plan-charges`, `plan-chatter`, `plan-ladders`, `plan-latency`, `room-and-board-code`, `siu-event-kinds`, `siu-renders?`, `skeleton-message-types`

**KEEP, 1: a SRC code caller survives (ii).** `unescape-er7` <- `components/sim-emit-hl7/src/ehrt/sim_emit_hl7/v2_replay.clj`.
That file is a sibling implementation reaching its own facade --
not a test, so (ii) does not touch it, and the def stays.

**RETIRE, 9:**

| def | delegates to | status before (ii) |
|---|---|---|
| `hl7-timestamp` | `ehrt.sim-emit-hl7.hl7-time/hl7-timestamp` | 3 test file(s) reach it |
| `message-type-registry` | `ehrt.sim-emit-hl7.registry/message-type-registry` | 6 test file(s) reach it |
| `order-status-ladder` | `ehrt.sim-emit-hl7.registry/order-status-ladder` | already caller-less |
| `result-status-ladder` | `ehrt.sim-emit-hl7.registry/result-status-ladder` | already caller-less |
| `^:private` `msh-segment` | `ehrt.sim-emit-hl7.segments/msh-segment` | 1 test file(s) reach it |
| `^:private` `pid-segment` | `ehrt.sim-emit-hl7.segments/pid-segment` | 1 test file(s) reach it |
| `escape-er7` | `ehrt.sim-emit-hl7.er7/escape-er7` | 1 test file(s) reach it |
| `^:private` `tn-field` | `ehrt.sim-emit-hl7.er7/tn-field` | 1 test file(s) reach it |
| `event->messages` | `ehrt.sim-emit-hl7.messages/event->messages` | 2 test file(s) reach it |

Facade ends at **17 defs** (16 interface-borne + 1 src-borne).

## (iv) Dead requires, and one dead form

### `engine.clj`

Nine were dead BEFORE this pass (the ninth and tenth extractions left
them, named in `2026-08-30-engine-extraction-run.md`); **(iii) makes
four more dead**, because `assignment`, `evolve`, `log-index` and
`state` exist only for defs the pass retires. The 12 surviving defs need
exactly five: `config`, `decide`, `fold`, `run`, `streams`.

| require | dead before the pass | dead because of (iii) |
|---|:--:|:--:|
| `ehrt.sim-model.interface` | yes | |
| `ehrt.patient-simulator.interface` | yes | |
| `ehrt.sim-engine.churn` | yes | |
| `ehrt.sim-engine.encounters` | yes | |
| `ehrt.sim-engine.order-profiles` | yes | |
| `ehrt.sim-engine.person-fold` | yes | |
| `ehrt.kernel.interface` | yes | |
| `malli.core` | yes | |
| `malli.util` | yes | |
| `ehrt.sim-engine.assignment` | | yes |
| `ehrt.sim-engine.evolve` | | yes |
| `ehrt.sim-engine.log-index` | | yes |
| `ehrt.sim-engine.state` | | yes |
| `(:import [java.util Random])` | yes | |

**Thirteen requires and one import.** Component-level dependencies do
NOT change and it is checked, not assumed: every external one is taken
by a sibling in the same component -- `sim-model` by seven of them,
`patient-simulator` by `decide.clj`, `kernel` by `run.clj`, `malli.core`
by five, `malli.util` by `state.clj` and `event_schema.clj`,
`java.util.Random` by five.

### `emit_hl7.clj`

`messages` becomes dead, and only `messages`: `event->messages` is its
one use and (iii) retires it. The other six requires all keep at least
one surviving def -- `er7` by `unescape-er7` alone, which is why the
src-borne keep in (iii) matters here.

### The dead form

`components/sim-emit-hl7/src/ehrt/sim_emit_hl7/registry.clj:321`,
`(def ^:private final-result-stage ...)` -- the form the `registry`
extraction moved and gave nothing, dead on arrival and named as dead in
that file's own `ns` docstring and in `emit_hl7.clj:88`. Retiring it
means retiring BOTH of those sentences too, or they become the very
class this pass exists to clear.

## (v) Residuals

### C10 residual surfaces -- five, lines re-derived at `9dffb2b`

C10(b) paid four of nine and ruled OUT restoring any gate; these five
still cite a gate `e189418` deleted (de-scaffold, 2026-08-25) in the
present tense. Two of them are in `state_derived.clj`'s own `src` half
-- the renderer whose TEST docstring C10(b) already corrected.

| site | gate cited | ruled line | actual line |
|---|---|---|---|
| `components/docs-tooling/src/ehrt/docs_tooling/state_derived.clj` | `reading-set-budget-test` | 77 | **77** |
| `components/docs-tooling/src/ehrt/docs_tooling/state_derived.clj` | `rulings-lint-test` | 173 | **174** |
| `Makefile` | `state-residue-test` | 256 | **256** |
| `notes/prompts/README.md` | `notes-prompts-frozen-test` | 14 | **15** |
| `components/docs-tooling/test/ehrt/docs_tooling/adr_index_test.clj` | `reading-set-budget-test` | 21 | **21** |

`notes/prompts/README.md` is the INDEX of a frozen directory, not a
frozen file, and C10(b)'s own precedent is `.agents/prompts/README.md`
-- corrected, line-neutral. `adr_index_test.clj` is a test file, so it
is C1(a)-fenced except that C12(b) lifts the fence for this session.

### Found-not-caused prose, carried by the records and re-derived here

| site | claim | truth |
|---|---|---|
| `segments.clj:250` | "`emit_hl7.clj`'s own registry comment" | the phrase lives at `registry.clj:41`; ADR-0174:313 pins it at the old `emit_hl7.clj:51`. The thirteenth session's row (A), which has now TRAVELLED twice |
| `emit_hl7_test.clj:1306` | same claim, test side | same fix; also listed in (i-b) |
| `timelines.clj:137` | `ehrt.sim-engine.engine/stamp-encounter` | **does not resolve** -- `engine.clj` defines nothing of that name; the form is `ehrt.sim-engine.encounters/stamp-encounter` (`encounters.clj:138`, public). Row (B), false since the THIRD engine session |
| `planners.clj:69`, `:318`; `messages.clj:223`, `:238`; `charges_test.clj:201` | same var | row (B) is WIDER than one site: **six sites in five files**, one of which travelled into `planners.clj`. Re-derived here; the records had four in three |
| `engine_test.clj:2440`, `:2446`, `:2543` | `engine.clj:480` | a LINE citation, stale for many sessions and unresolvable by line. Corrected to name the form and its owner, not a number |
| `docs/operational-models.md` | a path that has not existed since the sim merge | the file is `components/sim/docs/operational-models.md`; `c0b5b0a` relocated it two segments down without re-depthing the citations. Not a markdown LINK, so no gate sees it. **65 sites in 28 live files**, re-derived here against the records' twenty-one |

## Scope, honestly priced

| step | population | shape |
|---|---:|---|
| 2 (i-a) prose, ns-qualified | 176 sites / 63 files | mechanical, one owner per name |
| 2 (i-a2) prose, bare alias | 84 sites / 42 files | 25 of them mover banners this pass falsifies |
| 2 (i-a3) prose, wrapped | 7 sites / 6 files | invisible to a single-line grep |
| 2 (i-b) prose, by-file | 18 sites / 16 files | read in place, sentence by sentence |
| 2 (ii) test code | 976 sites / 52 files | mechanical; 1 site deliberately not moved |
| 3 (iii) retirement | 40 defs | 31 engine, 9 emit |
| 3 (iv) dead requires + form | 14 + 1 + 1 | engine 13+import, emit 1, `final-result-stage` |
| 4 (v) residuals | 5 + 76 sites | C10 five; found-not-caused six classes |

Steps 2 and 3 are this session's floor. If step 4 exceeds the session's
honest reach it is handed back with this table as its price, per the
pass's own fence.
