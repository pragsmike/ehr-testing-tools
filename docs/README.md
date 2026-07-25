# docs/

Reading order through this repo's documentation, spine-first: the
pipeline is the map, and every other document below locates itself
relative to it. Start at the top of this list and go as deep as you
need — each entry names the question that document answers.

0. **[../SETUP.md](../SETUP.md)** — newcomer start here: what do I need
   to install, how do I verify it worked, how do I generate my first
   corpus? (Everything below this point assumes you already have that
   working and want to understand what you built.)
1. **[use-cases.md](use-cases.md)** — what can I actually do with this,
   formally? One entry per use case (generate, mutate, gate, judge your
   own data, black-box transform surrounds, drift detection, and more),
   each anchored to the resource equations it composes from the stages
   below — read this to find *your* use case before diving into the
   stage-by-stage pipeline page.
2. **[pipeline.md](pipeline.md)** — what does this repo actually build,
   stage by stage? (Every stage — Generate, Normalize, Mutate, Intake,
   Gate, Check, Report — is built as of P6.)
3. **[notation.md](notation.md)** — what does the resource-equation
   notation on that page mean, and why does this repo use it?
   [judge-calibration.md](judge-calibration.md) is the companion page for
   the Gate stage specifically: which defects each judge tier actually
   catches, cited to EXP-C5 and the contract-pairing test suite, plus
   baseline-relative gating for real-world corpora (P6).
4. **[components.md](components.md)** — what external engines and
   libraries does each stage actually depend on, and what's each one's
   license, steward, and role?
5. **[experiments.md](experiments.md)** — how were the stages' laws and
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
   - **EXP-A3** — demoted to backlog (2026-07-24, P5) — HL7 v2
     generation via a Synthea projector plugin; no longer this repo's
     own v2-mutation blocker (see `corpus.intake`), narrowed to serving
     the guide's own Experiment 3.
   - **EXP-C5** ([protocol](experiments/EXP-C5.md),
     [results](experiments/EXP-C5-results.md)) — executed 2026-07-24 —
     official FHIR validator offline behavior and verdict-classification
     policy; see [judge-calibration.md](judge-calibration.md) for what it
     resolved.
   - **EXP-D3** — not yet run — CDC wrapper offline build.
6. **[positioning.md](positioning.md)** — how does this repo relate to
   `ehr-testing-guide`, and what was the go-public gate this repo set
   for itself (and how was it met — [ADR-0008](../notes/ADRs.md))?
7. **[engine-onboarding.md](engine-onboarding.md)** — what must a *new*
   engine wrapper answer before it's trusted enough to feed a committed
   manifest?
8. **[research/](research/)** — the primary-source research behind the
   above; read only if you need depth past a components.md citation:
   - `EHR-testing-tools-selection-research.md`
   - `License Status of NIST HL7 v2 Validation Software  Evidence-Based Classification.md`

Not part of this walk, but referenced from several points in it:
[`ehr-testing-tools-problem-statement.md`](ehr-testing-tools-problem-statement.md)
(the original problem statement `experiments.md`'s backlog was adopted
from) and the experiment protocol/results files under `experiments/`
themselves, which the table in step 5 links directly.
