# Experiment Backlog

Experiments are how this repo verifies a pipeline stage's law
(`docs/notation.md`) actually holds, and how it pins design decisions
that would otherwise rest on assertion — a protocol is written and
committed before execution, and the results file that follows is
classification only; interpreting what a result means for the
architecture happens in the design record (an ADR or the plan file),
not in the results file itself.

Experiments this project needs run before committing to architecture,
adopted from the component-selection research behind
`docs/ehr-testing-tools-problem-statement.md`. Each gates a specific
decision — an experiment that doesn't gate anything doesn't belong here.

| Id | Objective | Gates | Priority |
|---|---|---|---|
| EXP-SBOM ([protocol](experiments/EXP-SBOM.md), [results](experiments/EXP-SBOM-results.md)) — **executed 2026-07-23**; deep-research extension 2026-07-24 | Resolve NIST v2-validation / IGAMT / CDC-wrapper licensing and artifact provenance (incl. direct inquiry to NIST if needed); produce a dependency/license SBOM against the Apache-2.0 target (the license target current at execution time; superseded prospectively by MIT, ADR-0007) | The v2 gate architecture: NIST-engine-based "full gate" vs HAPI-light + local rules; also resolves facts-register [F1](../notes/facts-register.md) and guide F3 — inventory complete, both classified `license-unstated` on primary evidence. The 2026-07-24 deep-research pass ([`docs/research/License Status of NIST HL7 v2 Validation Software  Evidence-Based Classification.md`](research/License%20Status%20of%20NIST%20HL7%20v2%20Validation%20Software%20%20Evidence-Based%20Classification.md)) found a live official distribution channel (`hit-nexus.nist.gov`, facts register [F14](../notes/facts-register.md)) enabling a fetch-at-build adoption path, and narrowed the residual NIST inquiry to 4 questions with a named recipient (facts register [F15](../notes/facts-register.md)/[F16](../notes/facts-register.md), `docs/experiments/EXP-SBOM-inquiry-draft.md`) — still unsent | 1 — first; cheap, and a potential adoption blocker |
| EXP-A4 ([protocol](experiments/EXP-A4.md), [results](experiments/EXP-A4-results.md)) — **executed 2026-07-24** | Verify Synthea determinism: identical seed + version + config ⇒ identical bytes, including across thread counts; enumerate everything that must be pinned | The reproducibility-manifest schema — resolved: manifest v1 (`corpus.manifest`), clinician-seed pinning, forced locale/timezone, two registered canonicalizers | 2 — cheap |
| EXP-B2 ([protocol](experiments/EXP-B2.md), [results](experiments/EXP-B2-results.md)) — **executed 2026-07-24** | Measure parse→serialise round-trip fidelity of HAPI HL7v2 (note: PipeParser canonicalises trailing delimiters) and HAPI FHIR parsers | The mutation-layer design: mutate parsed trees vs encoded strings; the intended-diff-only invariant — resolved: mutation operates on FHIR JSON as plain Clojure data (faithful modulo whitespace); HAPI FHIR's round-trip silently drops `resource.id` on every Bundle entry, disqualifying it as a mutation substrate; PipeParser confirmed to canonicalize trailing delimiters, out of scope this session (v2 mutation deferred to post-EXP-A3) | 2 — cheap |
| EXP-A3 | Spike a Synthea `PatientExporter` plugin projecting to HL7 v2: ADT^A01 with MSH/EVN/PID/PV1, deterministic per seed, validating against a trivial profile | The v2 generation approach — the largest build item; its output eventually feeds the guide's Experiment 3 | 3 |
| EXP-C5 | Characterise official FHIR validator behaviour offline with pinned IGs and no terminology server (open upstream bug: locally packaged ValueSets still fail); determine the verdict-classification policy (assert on severity/code/expression; royalty-free vocab bundled; SNOMED/CPT checks classified license-blocked-indeterminate) | The FHIR gate wrapper's verdict policy | 4 |
| EXP-D3 | Build and run the CDC lib-hl7v2-nist-validator fully offline. The artifact-mirroring problem this experiment was originally scoped around is dissolved (2026-07-24 deep research, facts register [F14](../notes/facts-register.md)): the six vendored NIST-origin coordinates fetch from the confirmed-live `hit-nexus.nist.gov` into the standard artifact cache via the existing `artifact` registry, the same mechanism every other engine distribution already uses — no separate mirroring machinery needed. Remaining scope: lockfile entries for the six coordinates, plus the offline wrapper build itself. (Facts register F9 already corrected the earlier, unregistered claim that the upstream build disables SSL verification — no such config exists; this row no longer repeats that claim.) | The full-gate runtime, contingent on EXP-SBOM | 5 — after EXP-SBOM |

EXP-C3 (OperationOutcome stability across validator versions) and EXP-E6
(performance envelopes) trail the list above — they inform tuning once a
gate exists, not the architecture decisions the six experiments above
gate.
