# ehr-testing-sim

Deterministic, seeded generation of synthetic hospital traffic for
testing EHR integrations: clinically plausible, operationally messy
(ADT churn: transfers, cancellations, merges, corrections), US-coded
patient event streams from minimal parameters — with a ground-truth
trajectory log as a first-class output so test harnesses can assert
against what *should* be true, independent of message parsing.

**Status: pre-release walking skeleton.** The engine runs trivial
pathways deterministically; the invariant catalog and CLI seams are
real; HL7v2 emission and generative clinical modules are next. See
[`docs/problem-statement.md`](docs/problem-statement.md) for the full
problem, constraints, black-box contract, and validation program.

## Relation to the ehr-testing-* family

- [`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide)
  teaches the testing method.
- [`ehr-testing-tools`](https://github.com/pragsmike/ehr-testing-tools)
  makes it runnable (corpus construction, conformance gating).
- **ehr-testing-sim** (this repo) is the traffic source. It is usable
  standalone, and ehr-testing-tools can mount it as the `ehr sim`
  subcommand and ingest its runs as corpora — the embedding contract
  is three exported values in `ehr-testing-sim.cli`
  (see [`notes/ADRs.md`](notes/ADRs.md), ADR-0001).

Dependency direction: tools → sim, never the reverse.

## Quick start

```bash
clojure -X:test                                  # run the suite
clojure -M:cli run --seed 42 --patients 5        # a run, as EDN
clojure -M:cli run --seed 42 | clojure -M:cli check   # self-check, exit 0
clojure -M:cli help
```

Same seed + config ⇒ byte-identical output, always. That's not a
convenience; it's guarantee #1, enforced by property tests.

## Design in three sentences

Pathways are data (a common intermediate representation shared by
hand-authored scripts and, later, trajectories generated from
Synthea-style clinical modules); a seeded discrete-event engine
executes them into a format-free ground-truth log. Wire formats
(HL7v2 first; FHIR/CDA later) are emitters consuming that log. Codes
(SNOMED CT, LOINC, RxNorm, ICD-10-CM) travel as data on the state, so
every emitter renders them natively (ADR-0002).

Design lineage — what was mined from Google's Simulated Hospital
(operational model, churn vocabulary, event queue) and Synthea
(generative clinical modules, US demographics, embedded codes) — is
recorded in [`.agents/memory/architecture.md`](.agents/memory/architecture.md).

## License

MIT — see [LICENSE](LICENSE). No PHI anywhere, by construction.
