# ehr-testing

**⚠️ Draft — flagged for author review before this commits (R22).** This
is the workspace's public face; the content below is accurate as of
2026-07-28 but hasn't had a human editorial pass yet.

A [Polylith](https://polylith.gitbook.io/polylith) monorepo for testing
EHR (electronic health record) integrations: generate deterministic
synthetic hospital traffic, mutate it to probe conformance gates, and
judge candidate corpora against HL7 v2 and FHIR conformance rules — one
workspace, one development REPL, several deployable artifacts.

This workspace consolidates three formerly-separate repositories:

- **`ehr-testing-sim`** — a deterministic, seeded generator of synthetic
  hospital traffic (patients, encounters, orders, results), for testing
  EHR integrations against realistic-shaped data without any real PHI.
  Landed as [`components/sim`](components/sim) + [`bases/sim-cli`](bases/sim-cli).
- **`ehr-testing-tools`** — corpus construction (generation, mutation,
  provenance tracking) and conformance gating (HL7 v2 via HAPI, FHIR via
  the official validator). Landed as [`components/tools`](components/tools) +
  [`components/palgebra`](components/palgebra) (string-diagram tooling for
  documenting data-flow pipelines) + [`bases/ehr-cli`](bases/ehr-cli).
- **`ehr-testing-guide`** — deliberately **not** part of this workspace,
  permanently (`notes/ADRs.md` ADR-0001, R2). Not a deferred landing.

See [`notes/ADRs.md`](notes/ADRs.md) for the full decision record of how
and why this consolidation happened, and [`notes/carve-loss-audit.md`](notes/carve-loss-audit.md)
for an inventory of what did and didn't survive the move.

## Project map

| Path | What it is |
|---|---|
| `components/sim` | Fat component: the simulation engine (patients, encounters, GMF-driven pathways, churn, HL7 v2 / FHIR emission). |
| `bases/sim-cli` | Thin CLI dispatch for sim, standalone (`clojure -M:run run --seed ...` from `projects/sim`). |
| `components/tools` | Fat component: corpus generation/mutation/intake, HL7 v2 and FHIR gates, reporting. |
| `components/palgebra` | String-diagram tooling (resource-equation → Mermaid) used to document this workspace's own data-flow pipelines. |
| `bases/ehr-cli` | Thin CLI dispatch for tools — the `ehr` command. As of the sim mount (`notes/ADRs.md` ADR-0005), `ehr sim run` also dispatches straight into `components/sim`, in-process, no subprocess. |
| `projects/sim` | Composes sim's own artifact. |
| `projects/tools-cli` | Composes tools + palgebra + ehr-cli — the published CLI artifact. |
| `projects/conformance` | Base-less: exercises sim + tools + palgebra together, workspace-internal suites only. |
| `projects/integration` | Base-less: the artifact-fetch-dependent suites (real Synthea, the real FHIR validator) — nightly/on-demand only, never per-push (`notes/ADRs.md` ADR-0004). |

## Quickstart

Prerequisites and full verification steps: [`SETUP.md`](SETUP.md).
Short version, from a WSL2/Linux/macOS shell with the Clojure CLI on
PATH:

```sh
git clone <this repo>
cd ehr-testing
bin/ehr help
```

Generate a small synthetic corpus and gate it against HL7 v2 base
structure:

```sh
bin/ehr corpus generate --seed 1 --population 5 --out-dir target/demo
bin/ehr gate v2 target/demo
```

Run the sim engine directly (mounted in-process as of ADR-0005 — no
separate sim checkout needed):

```sh
bin/ehr sim run --seed 100 --patients 1
```

`bin/ehr help <group>` documents every command group and its flags
(`artifact`, `corpus`, `gate`, `check`, `version`, `doctor`, `sim`).

## Contributing

Read [`AGENTS.md`](AGENTS.md) first — it is the canonical instruction
surface for agents and contributors alike, including the WSL-only git
rule that applies before your first commit. [`CONTRIBUTING.md`](CONTRIBUTING.md)
and [`AUTHORS-GUIDE.md`](AUTHORS-GUIDE.md) go deeper on the workspace's
own session discipline.

## License

See [`LICENSE`](LICENSE).
