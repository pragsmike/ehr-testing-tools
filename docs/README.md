# docs/

Find yourself in the list below and follow that path — each one is a
handful of steps and ends wherever that audience's actual question gets
answered. `docs/positioning.md`'s [Audience](positioning.md#audience)
section is the canonical register these paths are keyed off; this page
just routes.

## Task-first practitioner

You have a job to do — generate data, break it on purpose, gate it —
and don't need the method behind it yet.

1. **[../SETUP.md](../SETUP.md)** — install, verify, generate your first
   corpus.
2. **[../README.md](../README.md#quickstart)** — every command in the
   quickstart, in order.
3. **`ehr help`**, **`ehr help <group>`** — the CLI's own reference at
   the shell; **[cli.md](cli.md)** is the same thing as a page, when
   you're not at one.
4. **[use-cases.md](use-cases.md)** — find the use case that matches
   what you're actually trying to do.
5. When you get to breaking data on purpose:
   **[operators.md](operators.md)** — the catalog of what you can
   break and what each defect violates — and
   **[locators.md](locators.md)** — how to write the
   `--locator-path` that says exactly where.
6. **[formats.md](formats.md)** — what came back: report, manifest,
   lineage record, and the `--json` projection.
7. If a gate result surprises you: **[judge-calibration.md](judge-calibration.md)**
   — exactly which defects each judge tier catches, and which it doesn't.

## Method-first guide reader

You've read (or are reading) `ehr-testing-guide`'s account of the
method and want the map from method to capability.

1. **[use-cases.md](use-cases.md)** — what this repo actually lets you
   do, one entry per use case.
2. **[The deep walk](#the-deep-walk-pipeline-first-reading-order)**,
   below — the full pipeline-first reading order, ground up.

## The AI assistant, reading on someone's behalf

If you're an assistant acting for a human rather than a human reading
directly, this is probably how you arrived here.

1. **[../SETUP.md](../SETUP.md)**'s step 5 — the copy-paste prompt this
   repo expects you to be handed.
2. **`ehr help`** — exact commands and flags, not prose descriptions of
   them.
3. The **task-first practitioner** path above — same docs, same order.

These docs aim to be legible to you specifically: exact,
copy-pasteable commands over descriptions of commands; heading anchors
that stay stable across a page's regeneration; error text that's
self-explanatory without a human in the loop to interpret it.

## Downstream data consumer

You read `report.edn`, `manifest.edn`, or lineage records — via the
`--json` projection or EDN directly — and never run the CLI yourself.

1. **[formats.md](formats.md)** — start here. Every shape the tools
   emit, field by field, with the `--json` mapping stated from real
   output and one honest paragraph on reading it from Python.
2. **[judge-calibration.md](judge-calibration.md)**'s
   ["No-verdict, operationally"](judge-calibration.md#no-verdict-operationally-2026-07-25-adr-0010)
   and ["Reading this table"](judge-calibration.md#reading-this-table)
   sections — what the verdicts *mean* in bulk, once you can parse them.
3. **[locators.md](locators.md)** — how to read the `:locator` a
   finding hands back to you.

## Contributor

1. **[../AGENTS.md](../AGENTS.md)** — primary instruction surface, hard
   rules, quick start.
2. **[../AUTHORS-GUIDE.md](../AUTHORS-GUIDE.md)** — the full authoring
   discipline.
3. **[../notes/ADRs.md](../notes/ADRs.md)** and
   **[../notes/facts-register.md](../notes/facts-register.md)** —
   reasoning-of-record and externally verifiable facts.

That's the whole path — the rest of this page isn't written for this
audience.

## Evaluator, deciding whether to adopt this at all

1. **[../README.md](../README.md#maturity)** — the maturity table: the
   actual contract with readers, not a formality.
2. **[../README.md](../README.md#scope)** — what this repo explicitly
   does not do.
3. **[positioning.md](positioning.md)** — the fuller map: audiences,
   the constellation this repo sits in, and the go-public gate it set
   for itself.

## Clojure library consumer

Nothing to walk yet — this audience arrives post-first-release. See
`positioning.md`'s
[Go-public gate vs. first release](positioning.md#go-public-gate-vs-first-release)
for what changes at that point and why it hasn't yet.

## The deep walk: pipeline-first reading order

The full reading order every other document above ultimately locates
itself against: the pipeline is the map. Start at the top and go as
deep as you need — each entry names the question that document
answers.

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
