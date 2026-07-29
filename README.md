# ehr-testing-tools

[![CI](https://github.com/pragsmike/ehr-testing-tools/actions/workflows/test.yml/badge.svg)](https://github.com/pragsmike/ehr-testing-tools/actions/workflows/test.yml)

**`ehrt`, pronounced "e-heart."** Reproducible test data for EHR
integrations — generated, broken on purpose, and conformance-gated,
end to end. One system: generate deterministic synthetic hospital
traffic, mutate it to probe conformance gates, and judge candidate
corpora against HL7 v2 and FHIR rules. Offline and deterministic by
construction; plain FHIR JSON, HL7v2 text, and EDN out the other end,
so you can use the results from Python, SQL, or anything else. Clojure
inside, no Clojure skills required.

It's for the people who actually test EHR integrations day to day —
interface analysts, QA engineers, data engineers — not necessarily
Clojure programmers. If you're comfortable working alongside an AI
assistant, [`SETUP.md`](SETUP.md) has a copy-paste prompt that walks
it through installing and running this for you.

**I want to generate or judge test data** → you want the [`docs/`](docs/)
path — start at [`docs/what-is-this.md`](docs/what-is-this.md), or jump
straight to the Quickstart below.

**I want to maintain or extend this workspace** → you want the
[`docs/dev/`](docs/dev/) path — start at
[`docs/dev/architecture.md`](docs/dev/architecture.md), and read
[`AGENTS.md`](AGENTS.md) before your first commit.

This workspace is the operational companion to
[`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide):
the guide teaches the testing method, this workspace makes it runnable.
See [`docs/dev/positioning.md`](docs/dev/positioning.md) for the fuller
map of how the two relate.

## Maturity

Pre-release honesty is a feature here, not a hedge — these labels are
the actual contract with readers, not a formality.

| Capability | Maturity | Evidence |
|---|---|---|
| **Generate** (sim; `corpus.generate`) | **Usable** | Clean-environment byte-reproducibility proven — [EXP-A4](components/tools/docs/experiments/EXP-A4-results.md). |
| **Mutate** (`corpus.mutate`) | **Experimental** | FHIR and v2 both work (v2: locator grammar, `corpus.er7` substrate, seed operators, contract-pairing proof against `judge.v2`); interfaces may still move — [EXP-B2](components/tools/docs/experiments/EXP-B2-results.md). |
| **Intake** (`corpus.intake`) | **Experimental** | Foreign-corpus cataloging; same content-hash lineage as generated corpora — [intake tests](components/tools/test/ehrt/tools/corpus/intake_test.clj). |
| **Gate** (`judge.fhir` / `judge.v2`) | **Experimental** | Base-spec (FHIR, official validator) / base-structural (v2, HAPI); offline verdict policy; no implementation guide pinned yet; baseline-relative mode for real-world corpora — [EXP-C5](components/tools/docs/experiments/EXP-C5-results.md), [judge calibration](docs/judge-calibration.md). |
| **Check** (`ehrt.tools.check`) | **Experimental** | Dataset-vs-expectations judge alongside Gate: golden equivalence plus a small per-file assertion vocabulary — [check tests](components/tools/test/ehrt/tools/check_test.clj). |

**Status: pre-release.** No version tag, nothing published to Clojars
or Maven Central, interfaces may still move. See
[`docs/dev/positioning.md`](docs/dev/positioning.md) for what
publication does and doesn't mean.

## Quickstart

Prerequisites and full verification steps: [`SETUP.md`](SETUP.md).
Every command below is run for real and asserted by `bin/quickstart-demo`
(`make quickstart`) — this fence and that script are meant to teach the
identical sequence, in the identical order (DOC-5's own discipline); if
you ever see them drift, that script is the bug report.

```sh
bin/ehrt help

bin/ehrt artifact fetch --name synthea --version 4.0.0
bin/ehrt artifact fetch --name temurin-jdk --version 21.0.12+8

bin/ehrt corpus generate

PATIENT_FILE=$(ls target/corpus/synthea-s1-p5/fhir/*.json | grep -v -e hospitalInformation -e practitionerInformation | head -1)
bin/ehrt corpus mutate $PATIENT_FILE \
  --operator-id remove-required-element --locator-path entry[0].resource.gender \
  --out-dir out/demo-mutants

bin/ehrt artifact fetch --name fhir-validator-cli --version 6.9.12
bin/ehrt gate v2 components/tools/test-fixtures/v2
# gate fhir exits 1 here -- a genuine defect in the mutant, correctly caught
bin/ehrt gate fhir out/demo-mutants --report out/demo-mutants-report.edn

bin/ehrt check target/corpus/synthea-s1-p5/fhir --expected target/corpus/synthea-s1-p5/fhir

# mounted in-process (ADR-0005) -- no separate sim checkout needed
bin/ehrt sim run --seed 100 --patients 1

clojure -M:poly test :all
```

`bin/ehrt help <group>` documents every command group and its flags
(`artifact`, `corpus`, `gate`, `check`, `version`, `doctor`, `sim`).

## Contributing

Read [`AGENTS.md`](AGENTS.md) first — it is the canonical instruction
surface for agents and contributors alike, including the WSL-only git
rule that applies before your first commit. [`CONTRIBUTING.md`](CONTRIBUTING.md)
and [`AUTHORS-GUIDE.md`](AUTHORS-GUIDE.md) go deeper on the workspace's
own session discipline.

## License

See [`LICENSE`](LICENSE).
