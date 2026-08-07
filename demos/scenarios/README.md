# Scenarios

Sibling of [`../traces/`](../traces/README.md), for a different purpose: a
scenario is a RUNNABLE configuration, not a captured trace. Each
subdirectory holds:

- `config.edn` — the `--config` passthrough vehicle for a real,
  population-scale simulation run (weighted modules, an arrival gap,
  a horizon — the shape `demos/traces/module-mix/config.edn` already
  previews at small scale).
- `README.md` — the exact commands to generate the scenario's own
  output and then watch it play back, both root-resolvable and gated
  by the same invocation-lint/fence-path machinery
  (`ehrt.docs-tooling.invocation-lint-test`) as every other live doc
  in this repo.

**No captured trace.** `../traces/`'s own convention — a fixture folder
holding `ground-truth.edn`/`messages.txt` for a ≤10-patient run, small
enough to read in full — deliberately does NOT apply here. A scenario's
output is population-scale by design (tens to hundreds of patients,
a busy multi-ward mix); committing that as a fixture would defeat the
whole point of `../traces/`'s own size discipline without adding anything
a scenario reader needs. Run the scenario's own generate command to
produce its output; nothing under a scenario directory is a generated
artifact.

## Contents

- [`busy-tuesday/`](busy-tuesday/) — a busy weekday ED mix across the
  twelve everyday-ambulatory and acute modules the vendoring arc's
  first two batches landed, a five-minute arrival gap, and a
  total-joint-replacement seed so that closure's own content lands
  without relying on its default attribute distribution. Vendoring
  batch 2's own rider (AR-VB2-R, `notes/ADRs.md` ADR-0071).
