# Experiment Backlog

Experiments this project needs run before committing to architecture,
adopted from the component-selection research behind
`docs/ehr-testing-tools-problem-statement.md`. Each gates a specific
decision — an experiment that doesn't gate anything doesn't belong here.

| Id | Objective | Gates | Priority |
|---|---|---|---|
| EXP-SBOM | Resolve NIST v2-validation / IGAMT / CDC-wrapper licensing and artifact provenance (incl. direct inquiry to NIST if needed); produce a dependency/license SBOM against the Apache-2.0 target | The v2 gate architecture: NIST-engine-based "full gate" vs HAPI-light + local rules; also resolves facts-register [F1](../notes/facts-register.md) and guide F3 | 1 — first; cheap, and a potential adoption blocker |
| EXP-A4 ([protocol](experiments/EXP-A4.md), [results](experiments/EXP-A4-results.md)) — **executed 2026-07-24** | Verify Synthea determinism: identical seed + version + config ⇒ identical bytes, including across thread counts; enumerate everything that must be pinned | The reproducibility-manifest schema — resolved: manifest v1 (`corpus.manifest`), clinician-seed pinning, forced locale/timezone, two registered canonicalizers | 2 — cheap |
| EXP-B2 | Measure parse→serialise round-trip fidelity of HAPI HL7v2 (note: PipeParser canonicalises trailing delimiters) and HAPI FHIR parsers | The mutation-layer design: mutate parsed trees vs encoded strings; the intended-diff-only invariant | 2 — cheap |
| EXP-A3 | Spike a Synthea `PatientExporter` plugin projecting to HL7 v2: ADT^A01 with MSH/EVN/PID/PV1, deterministic per seed, validating against a trivial profile | The v2 generation approach — the largest build item; its output eventually feeds the guide's Experiment 3 | 3 |
| EXP-C5 | Characterise official FHIR validator behaviour offline with pinned IGs and no terminology server (open upstream bug: locally packaged ValueSets still fail); determine the verdict-classification policy (assert on severity/code/expression; royalty-free vocab bundled; SNOMED/CPT checks classified license-blocked-indeterminate) | The FHIR gate wrapper's verdict policy | 4 |
| EXP-D3 | Build and run the CDC lib-hl7v2-nist-validator fully offline; mirror the NIST-Nexus artifacts locally (upstream build disables SSL verification — do not inherit that) | The full-gate runtime, contingent on EXP-SBOM | 5 — after EXP-SBOM |

EXP-C3 (OperationOutcome stability across validator versions) and EXP-E6
(performance envelopes) trail the list above — they inform tuning once a
gate exists, not the architecture decisions the six experiments above
gate.
