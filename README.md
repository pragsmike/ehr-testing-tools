# ehr-testing-tools

Operational tooling for testing EHR integrations: synthetic corpus
construction (generation, mutation, provenance) and conformance gating
(HL7 v2 and FHIR), for JVM/Clojure teams. Offline-first, reproducible —
no hosted services, no non-deterministic runs.

**Status: pre-release.** This repo is under private development. Nothing
in it is stable, nothing is released, and nothing here should be depended
on yet. See `docs/positioning.md` for the conditions under which that
changes.

## Relationship to ehr-testing-guide

This repo is the companion to
[`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide), a
book on testing EHR integrations. The two exist for different purposes:
**the guide's companion code exists to be read; the tools here exist to
be run.** The guide explains the testing method and where each capability
belongs in a test plan; this repo makes the capabilities runnable.

If you arrived here first and want to know *why* these tools exist or
*what to do with their output*, read the guide — it's the method this
repo implements. See `docs/positioning.md` for the fuller map of how the
two projects relate.

## Scope

This repo does **not** do:

- Semantic correctness checking — properties, metamorphic relations, and
  golden-case comparison remain the caller's own code, written against
  their own transforms.
- Full terminology validation against licensed vocabularies (e.g. complete
  SNOMED CT) — that imports licensing and distribution problems this
  project does not take on.
- Production message routing or integration-engine functionality — these
  are test-time tools, not runtime infrastructure.
- A hosted, public validation service — local deployment is the target.

See `docs/ehr-testing-tools-problem-statement.md` for the full problem
statement and `docs/positioning.md` for how this repo relates to the
guide, its go-public gate, and its open decisions.
