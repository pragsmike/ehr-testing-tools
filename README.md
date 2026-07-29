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
Every command below is run for real and asserted by `bin/quickstart-demo`
(`make quickstart`) — this fence and that script are meant to teach the
identical sequence, in the identical order (DOC-5's own discipline); if
you ever see them drift, that script is the bug report.

```sh
bin/ehr help

bin/ehr artifact fetch --name synthea --version 4.0.0
bin/ehr artifact fetch --name temurin-jdk --version 21.0.12+8

bin/ehr corpus generate

PATIENT_FILE=$(ls target/corpus/synthea-s1-p5/fhir/*.json | grep -v -e hospitalInformation -e practitionerInformation | head -1)
bin/ehr corpus mutate $PATIENT_FILE \
  --operator-id remove-required-element --locator-path entry[0].resource.gender \
  --out-dir out/demo-mutants

bin/ehr artifact fetch --name fhir-validator-cli --version 6.9.12
bin/ehr gate v2 components/tools/test-fixtures/v2
# gate fhir exits 1 here -- a genuine defect in the mutant, correctly caught
bin/ehr gate fhir out/demo-mutants --report out/demo-mutants-report.edn

bin/ehr check target/corpus/synthea-s1-p5/fhir --expected target/corpus/synthea-s1-p5/fhir

# mounted in-process (ADR-0005) -- no separate sim checkout needed
bin/ehr sim run --seed 100 --patients 1

clojure -M:poly test :all
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
