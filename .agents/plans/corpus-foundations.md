# Plan: Corpus Foundations

The phase sequence from bootstrap to a working generation capability and
its first executed experiment. One line per phase: what it delivers,
where it stands, and the prompt that drove it (once archived).

| Phase | Deliverables | Status | Prompt |
|---|---|---|---|
| P0 | Workflow housekeeping: prompts-dir semantics, `make pack-push`, pack-ritual wording | Done (2026-07-23) | `.agents/prompts/archive/2026-07-23-p0-p2-landing.md` |
| P1 | ADR-0004 (capability-oriented structure, `ehr` CLI contract), ADR-0005 (artifact lockfile) | Done (2026-07-23) | `.agents/prompts/archive/2026-07-23-p0-p2-landing.md` |
| P2 | `docs/components.md` + license F-rows, `docs/experiments/EXP-A4.md` protocol, positioning open-decision closed | Done (2026-07-23) | `.agents/prompts/archive/2026-07-23-p0-p2-landing.md` |
| P3 | Artifact registry, result/invocation/locator/canonical schemas, `corpus.generate`, `ehr` CLI, EXP-A4 executed (manifest v1) | Done (2026-07-24) | `.agents/prompts/archive/2026-07-24-p3-generation-exp-a4.md` |
| P4 | JVM as a lockfile artifact; manifest v1.1; resource-equation notation trial (`docs/notation.md`, `docs/pipeline.edn`/`.md`); EXP-B2 executed (parse→serialize round-trip fidelity); mutation capability (`corpus.operators`, FHIR locator grammar, `ehr-testing-tools.lineage`, `corpus.mutate`, `ehr corpus mutate`) — FHIR only | Done (2026-07-24) | `.agents/prompts/archive/2026-07-24-p4-mutation-exp-b2.md` |
| P5 | EXP-SBOM mechanical half (dependency/license SBOM against the Apache-2.0 target), EXP-C5 (FHIR validator offline behavior), EXP-D3 (CDC wrapper offline build) | Not started | — |
| Enforcement wave | Pre-push hook running the test suite; offline GitHub Actions CI; coverage threshold gating; artifact-cache priming for CI; pipeline lint (tier 1: every catalytic resource in `docs/pipeline.edn` resolves to one of the three lockfile targets); `make pipeline` freshness check (fails if `docs/pipeline.md` is stale relative to `docs/pipeline.edn`) | Not scheduled — planned per ADR-0006's staged-enforcement decision | — |

**v2 mutation deferral.** P4 scoped mutation to FHIR only. HL7 v2
mutation is deferred, not abandoned — its dependency is EXP-A3 (the
Synthea `PatientExporter` plugin spike that gives this repo any v2
generation to mutate in the first place; see `docs/experiments.md`).
EXP-B2 already characterized v2 ER7 round-trip fidelity via HAPI
HL7v2's `PipeParser` (faithful for realistically-populated messages;
confirmed trailing-delimiter canonicalization on a message crafted to
trigger it), so the representation question v2 mutation will need is
already answered when EXP-A3 lands — only the operator catalog and
locator grammar for v2 remain to be built.
