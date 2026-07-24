# docs/

Reading order through this repo's documentation, spine-first: the
pipeline is the map, and every other document below locates itself
relative to it. Start at the top of this list and go as deep as you
need — each entry names the question that document answers.

1. **[pipeline.md](pipeline.md)** — what does this repo actually build,
   stage by stage? (Generate, Normalize, Mutate are built; Gate and
   Report are planned.)
2. **[notation.md](notation.md)** — what does the resource-equation
   notation on that page mean, and why does this repo use it?
3. **[components.md](components.md)** — what external engines and
   libraries does each stage actually depend on, and what's each one's
   license, steward, and role?
4. **[experiments.md](experiments.md)** — how were the stages' laws and
   the design decisions behind them actually verified, not just
   asserted?
   - **EXP-SBOM** ([protocol](experiments/EXP-SBOM.md),
     [results](experiments/EXP-SBOM-results.md)) — executed
     2026-07-23, deep-research extension 2026-07-24 — NIST/CDC
     licensing and artifact-provenance classification.
   - **EXP-A4** ([protocol](experiments/EXP-A4.md),
     [results](experiments/EXP-A4-results.md)) — executed 2026-07-24 —
     Synthea determinism and the reproducibility-manifest schema.
   - **EXP-B2** ([protocol](experiments/EXP-B2.md),
     [results](experiments/EXP-B2-results.md)) — executed 2026-07-24 —
     parse→serialize round-trip fidelity; picked the mutation substrate.
   - **EXP-A3** — not yet run — HL7 v2 generation via a Synthea
     projector plugin.
   - **EXP-C5** — not yet run — official FHIR validator offline
     behavior and verdict-classification policy.
   - **EXP-D3** — not yet run — CDC wrapper offline build.
5. **[positioning.md](positioning.md)** — how does this repo relate to
   `ehr-testing-guide`, and what was the go-public gate this repo set
   for itself (and how was it met — [ADR-0008](../notes/ADRs.md))?
6. **[engine-onboarding.md](engine-onboarding.md)** — what must a *new*
   engine wrapper answer before it's trusted enough to feed a committed
   manifest?
7. **[research/](research/)** — the primary-source research behind the
   above; read only if you need depth past a components.md citation:
   - `EHR-testing-tools-selection-research.md`
   - `License Status of NIST HL7 v2 Validation Software  Evidence-Based Classification.md`

Not part of this walk, but referenced from several points in it:
[`ehr-testing-tools-problem-statement.md`](ehr-testing-tools-problem-statement.md)
(the original problem statement `experiments.md`'s backlog was adopted
from) and the experiment protocol/results files under `experiments/`
themselves, which the table in step 4 links directly.
