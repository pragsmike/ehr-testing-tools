# ehr-testing-tools

[![CI](https://github.com/pragsmike/ehr-testing-tools/actions/workflows/ci.yml/badge.svg)](https://github.com/pragsmike/ehr-testing-tools/actions/workflows/ci.yml)

Operational tooling for testing EHR integrations: reproducible
synthetic-corpus construction (generation, mutation, provenance) and
conformance gating (HL7 v2 and FHIR), for JVM/Clojure teams.
Offline-first — no hosted services, no non-deterministic runs.

## The pipeline

This is the whole shape before any detail: a Synthea configuration goes
in, a mutated, gate-ready corpus comes out.

```mermaid
flowchart LR
    Generate --> Normalize --> Mutate --> Gate
    style Generate fill:#2d2d2d,stroke:#000,color:#fff,stroke-width:2px
    style Normalize fill:#2d2d2d,stroke:#000,color:#fff,stroke-width:2px
    style Mutate fill:#2d2d2d,stroke:#000,color:#fff,stroke-width:2px
    style Gate fill:#2d2d2d,stroke:#666,color:#aaa,stroke-width:2px,stroke-dasharray:5 5
```

*Gate (dashed) is planned, not built.* The full version — resource
equations, catalytic inputs, the diagram mechanically derived from them
— is [`docs/pipeline.md`](docs/pipeline.md); [`docs/notation.md`](docs/notation.md)
is the notation it's written in.

## Maturity

Pre-release honesty is a feature here, not a hedge — these labels are
the actual contract with readers, not a formality.

| Capability | Maturity | Evidence |
|---|---|---|
| **Generate** (`corpus.generate`) | **Usable** | Clean-environment byte-reproducibility proven — [EXP-A4](docs/experiments/EXP-A4-results.md) |
| **Mutate** (`corpus.mutate`) | **Experimental** | Works; days old; interfaces may still move — [EXP-B2](docs/experiments/EXP-B2-results.md) |
| **Gate** (`gate.fhir` / `gate.v2`) | **Planned** | Designed, not built — see the [plan](.agents/plans/corpus-foundations.md) |

**Status: pre-release.** This repo is public (as of
[ADR-0008](notes/ADRs.md)) but has not had a first release: no version
tag, nothing published to Clojars or Maven Central, interfaces may
still move. See [`docs/positioning.md`](docs/positioning.md) for what
publication does and doesn't mean.

## Quickstart

Requires a JDK 17+ runtime for Synthea itself — resolved through this
repo's own artifact registry, not your `PATH` (see
[`docs/components.md`](docs/components.md)). Fetch the pinned artifacts
once, then generate and mutate:

```sh
# One-time: fetch the pinned Synthea distribution and its JDK, into the
# local artifact cache (~/.cache/ehr-testing-tools/artifacts).
make ehr ARGS="artifact fetch --name synthea --version 4.0.0"
make ehr ARGS="artifact fetch --name temurin-jdk --version 17.0.19+10"

# Generate a small deterministic corpus (EXP-A4's pinned settings).
make ehr ARGS="corpus generate --config-path config/synthea/synthea.properties \
  --seed 100 --clinician-seed 555 --population 10 \
  --reference-date 20260101 --output-dir out/demo-corpus"

# Mutate one patient bundle: drop a required element at a named
# locator, with a lineage record for the mutant. (mutate --input takes
# a file or a directory of files sharing one locator's shape; the
# corpus dir above also holds two non-patient bundles --
# hospitalInformation*.json, practitionerInformation*.json -- so this
# picks a patient file specifically rather than the whole directory.)
PATIENT_FILE=$(ls out/demo-corpus/fhir/*.json | grep -v -e hospitalInformation -e practitionerInformation | head -1)
make ehr ARGS="corpus mutate --input $PATIENT_FILE \
  --operator-id remove-required-element --locator-path entry[0].resource.gender \
  --output-dir out/demo-mutants"

# Run the test suite (hermetic — see AGENTS.md).
make test
```

`make help` lists every target. Output is EDN by default; every command
accepts `--json` for a projection (EDN remains the source of truth).

## Relationship to ehr-testing-guide

This repo is the companion to
[`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide), a
book on testing EHR integrations. The two exist for different purposes:
**the guide's companion code exists to be read; the tools here exist to
be run.** The guide explains the testing method and where each
capability belongs in a test plan; this repo makes the capabilities
runnable.

If you arrived here first and want to know *why* these tools exist or
*what to do with their output*, read the guide — it's the method this
repo implements. See [`docs/positioning.md`](docs/positioning.md) for
the fuller map of how the two projects relate, including why the guide
doesn't cite this repo yet (that waits for this repo's first release,
not merely publication).

## Scope

This repo does **not** do:

- Semantic correctness checking — properties, metamorphic relations, and
  golden-case comparison remain the caller's own code, written against
  their own transforms.
- Full terminology validation against licensed vocabularies (e.g.
  complete SNOMED CT) — that imports licensing and distribution
  problems this project does not take on.
- Production message routing or integration-engine functionality — these
  are test-time tools, not runtime infrastructure.
- A hosted, public validation service — local deployment is the target.

See [`docs/ehr-testing-tools-problem-statement.md`](docs/ehr-testing-tools-problem-statement.md)
for the full problem statement.

## How this repo works

Start at [`docs/README.md`](docs/README.md) for the reading order
through everything under `docs/`. The short version: decisions live in
[`notes/ADRs.md`](notes/ADRs.md), externally verifiable facts get an
entry in [`notes/facts-register.md`](notes/facts-register.md), and
architecture claims are pinned by an experiment
([`docs/experiments.md`](docs/experiments.md)) before they're trusted —
this discipline is part of what this repo is selling, not overhead
around it.

## License

MIT — see [`LICENSE`](LICENSE).
