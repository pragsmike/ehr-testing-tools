# AGENTS.md

> Primary instruction surface for AI coding agents working in this
> repository. Read by tools that support the `AGENTS.md` convention
> (Codex, OpenCode, and others). Claude Code users: see `CLAUDE.md`,
> which points here.

## Project overview

**Project:** ehr-testing-sim — a deterministic, seeded generator of
synthetic hospital traffic (patient event streams and, later,
state-based records) for testing EHR integrations. Sibling of
[`ehr-testing-tools`](https://github.com/pragsmike/ehr-testing-tools)
(operational tooling: corpus construction and conformance gating) and
[`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide)
(the testing method). This repo is the *traffic source*: tools can
mount it as the `ehr sim` subcommand and ingest its runs as corpora.
**Language(s):** Clojure.
**Status:** pre-release walking skeleton. Interfaces may move; nothing
is published to Clojars/Maven yet.

Read `docs/problem-statement.md` first — it is the reasoning-of-record
for *what* this library must do (problem, audience, constraints,
black-box contract, validation program). `notes/ADRs.md` records *how*
and outranks your own inference about why something is organized a
certain way. Never silently revert an Accepted ADR — supersede it with
a new numbered record. Durable design lineage (what was mined from
Simulated Hospital and Synthea, and why) lives in
`.agents/memory/architecture.md`.

## Quick start

```bash
# Run tests (same alias contract as the sibling repos)
clojure -X:test

# Standalone CLI
clojure -M:cli run --seed 42 --patients 5
clojure -M:cli run --seed 42 | clojure -M:cli check   # exit 0
```

## Architecture in one paragraph

Pathways (data, EDN — `pathway.clj`) are executed by a seeded
discrete-event engine (`engine.clj`) producing a **ground-truth event
log**, the primary output; message formats (HL7v2 first) are emitters
*consuming* that log, never the other way around. `check.clj` is the
invariant catalog over logs; `manifest.clj` bridges runs into
ehr-testing-tools' corpus provenance format; `cli.clj` is both the
standalone `sim` shell and the mountable command group; `result.clj`
is the (deliberately copied, see ADR-0001) result-not-throw
vocabulary.

## Code conventions

- **Result-not-throw:** every capability function returns
  `{:status :ok|:rejected|:error :category ... :payload ...}`.
  Exceptions are for programmer error only. Only `cli.clj` prints.
- **EDN is canonical output; `--json` is a projection**, never the
  source of truth. Exit codes: 0 ok, 1 rejected, 2 operational error.
- **Determinism is law:** all randomness flows from the single seeded
  RNG in `engine/run`. No wall-clock, no hash-order dependence, no
  unseeded entropy anywhere in the output path. If you add
  randomness, thread the RNG; the property tests will catch you if
  you don't.
- **Every new engine step type lands with its invariants** in
  `check.clj` and its tests, in the same change.
- Schemas are malli; keep versions aligned with ehr-testing-tools.

## Constraints

- **Dependency direction:** ehr-testing-tools may depend on this repo;
  this repo must NEVER depend on ehr-testing-tools (ADR-0001).
  The manifest/help shapes here are *mirrors* with tripwire tests;
  binding cross-repo contract tests belong in tools' integration tree.
- **No PHI, no real-person data, ever** — including in test fixtures
  and docs. Synthetic by construction is a core product claim.
- **No CPT codes** (AMA-licensed). SNOMED CT, LOINC, RxNorm,
  ICD-10-CM, CVX only.
- **The embedding contract is public API:** `cli/cli-spec`,
  `cli/help-group`, `cli/dispatch-action` are what ehr-testing-tools
  mounts. Changing their shapes is a breaking change; treat
  accordingly (ADR + version note).
- Do not invent facts about upstream sources (Simulated Hospital,
  Synthea); `.agents/memory/architecture.md` records what was
  verified and when.

## Skills

Repo-local skills live in `.agents/skills/` (none yet). Handoffs in
`.agents/handoffs/`, plans in `.agents/plans/`, durable knowledge in
`.agents/memory/`.
