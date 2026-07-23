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
| P4 | Mutation capability (`corpus.mutate`), EXP-B2 executed (parse→serialize round-trip fidelity) | Not started | — |
| P5 | EXP-SBOM mechanical half (dependency/license SBOM against the Apache-2.0 target), EXP-C5 (FHIR validator offline behavior), EXP-D3 (CDC wrapper offline build) | Not started | — |
| Enforcement wave | Pre-push hook running the test suite; offline GitHub Actions CI; coverage threshold gating; artifact-cache priming for CI | Not scheduled — planned per ADR-0006's staged-enforcement decision | — |
