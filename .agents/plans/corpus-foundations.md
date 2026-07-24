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
| P5 | Corpus intake (foreign corpora, catalog + intake records); EXP-C5 executed (FHIR validator offline behavior, 15 categories classified); `gate.fhir` (two-step, EXP-C5-derived verdict mapping, IG machinery built/unpinned) and `gate.v2` (base-structural, HAPI) landed; `gate.report` (aggregation, diffing) + `ehr gate fhir\|v2` CLI; contract pairing (5 defect operators vs `gate.fhir`, `^:integration` suite against the real validator) + the Gate kind law tested against the real engine; pipeline as-built (Intake/Gate/Report `:built`); pattern nursery #13 trial evidence (second real diagram-caught gap); README/positioning/gate-calibration updated | Done (2026-07-24) | `.agents/prompts/archive/2026-07-24-p5-gates.md` |
| P6 (sketched, not started) | v2 mutation over foreign/fixture corpora (unblocked by P5's intake capability plus EXP-B2's `PipeParser` round-trip finding — the representation question is already answered); IG pinning once the motivating team's target profile is known (`gate.fhir`'s `-ig` machinery is built, waiting on an artifact); EXP-D3 (CDC/NIST full-gate candidacy) unchanged, still contingent on EXP-SBOM's residual NIST-license inquiry. EXP-A3 (Synthea `PatientExporter` v2 spike) demoted to backlog: P5's intake capability means foreign v2 corpora already exist as a v2-mutation input, so EXP-A3's generation-side motivation narrows to serving only the guide's own Experiment 3, not also unblocking this repo's v2 mutation | Not started | — |
| Publication | Editorial coherence pass (README, docs/ reading order, pipeline/notation/components/experiments/positioning rewrites); minimal CI (`.github/workflows/ci.yml`, hermeticity verified); ADR-0008 (go-public gate walked and met; repo published) | Done (2026-07-24) | `.agents/prompts/2026-07-24-publication-wave.md` |
| Enforcement wave | Pre-push hook running the test suite; offline GitHub Actions CI; coverage threshold gating; artifact-cache priming for CI; pipeline lint (tier 1: every catalytic resource in `docs/pipeline.edn` resolves to one of the four catalytic targets, `docs/notation.md`); `make pipeline` freshness check (fails if `docs/pipeline.md` is stale relative to `docs/pipeline.edn`); nightly/optional CI job running `make integration` with artifact-cache priming (proposed 2026-07-25 CI hotfix session — not added to `.github/workflows/ci.yml` yet, listed here only) | Not scheduled — planned per ADR-0006's staged-enforcement decision | — |
| First release | Gates capability landed (`gate.fhir`/`gate.v2`, done P5); the resource-equation notation trial (pattern nursery #13) concluded one way or the other (two sessions of evidence gathered, promotion still a design-channel call); coverage-threshold gating landed. Version tag, published coordinates (Clojars vs. Maven Central, `docs/positioning.md` open decision), guide-repo cross-references begin. | Not started — the milestone after publication (ADR-0008) | — |

**v2 mutation deferral, updated (P5).** P4 scoped mutation to FHIR
only; EXP-B2 already characterized v2 ER7 round-trip fidelity via HAPI
HL7v2's `PipeParser` (faithful for realistically-populated messages;
confirmed trailing-delimiter canonicalization on a message crafted to
trigger it), so the representation question v2 mutation will need is
already answered — only the operator catalog and locator grammar for
v2 remain to be built (P6). What changed in P5: v2 mutation's
dependency used to be framed as EXP-A3 alone (the Synthea
`PatientExporter` plugin spike, the only route to v2 *generation* this
repo had). `corpus.intake` (P5) opens a second route — a foreign v2
corpus, brought by whoever is gating their own pipeline's output,
already exists as v2-mutation input without waiting on generation.
EXP-A3 is demoted to backlog accordingly, not abandoned: it still
matters for the guide's own Experiment 3 (which needs *this repo's*
generated v2 output specifically, not a foreign corpus), but it is no
longer this repo's own v2-mutation blocker. `docs/experiments.md`'s
EXP-A3 row is updated with this rationale.
