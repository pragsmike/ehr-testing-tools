# Contributing

Three doors, depending on what brought you here.

## Using the simulator

You want to generate traffic, not change this repo. Go to
[`SETUP.md`](SETUP.md) — installation, a verification ladder, and a
first-traffic walkthrough. Nothing below applies to you.

## Contributing code or docs

Read [`AGENTS.md`](AGENTS.md) and [`AUTHORS-GUIDE.md`](AUTHORS-GUIDE.md)
**before opening a PR** — not after, and not skimmed. The discipline
they describe is real, mechanically checked in places, and not
optional ceremony:

- **Test-first** (ADR-0004): a failing test precedes the implementation
  that makes it pass; red→green evidence is expected in what you write
  up. Property tests are required for law-bearing constructs
  (determinism, the invariant catalog, emitter derivability, schema
  round-trips).
- **The facts register** (`notes/facts-register.md`): any load-bearing,
  externally verifiable claim you add (a license, a version, a
  capability of some dependency or upstream source) gets a numbered
  F-row — claim, evidence, date — in the same commit that asserts it.
- **ADRs** (`notes/ADRs.md`): structural or architectural decisions get
  a numbered, append-only record. An Accepted ADR is never silently
  reverted; it's superseded by a new one.
- **Git operations are WSL-only** — see `AUTHORS-GUIDE.md` section 1
  before your first commit. This is enforced by a pre-commit hook, not
  merely requested.

If you skip straight to a PR without reading these, expect review
comments asking you to go back and do so — better to arrive knowing
the shape the discipline expects.

## Domain knowledge — no code required

**The single most valuable contribution this project can receive from
a clinician, informaticist, or health-IT veteran is a clinical-reality
report** — a real phenomenon this simulator doesn't model yet, or
models wrong. [`docs/clinical-realities.md`](docs/clinical-realities.md)
is the catalog these reports feed; every entry there follows the same
four-part shape, and that shape is exactly what a report should supply:

1. **The reality** — what actually happens, in your own words.
2. **The wire truth** — how it shows up in HL7v2/FHIR (standard fields
   and codes, and what's site-custom) — as much as you know; partial
   is fine.
3. **How you'd know we got it right** — a concrete check a reviewer
   could run against generated output. (Modeling it — the states,
   events, config a fix would need — is *our* job to fill in, not
   yours; a real-world description is the whole contribution.)

[Open an issue](https://github.com/pragsmike/ehr-testing-sim/issues/new/choose)
using the **Clinical reality report** template — it asks for exactly
these fields. No pull request, no code, no need to read `AGENTS.md`.

Found a bug instead? Use the **Bug report** template — because every
run here is deterministic (same seed + config ⇒ byte-identical
output), a bug report that includes `sim version`'s output plus your
seed and config *is* a reproduction, not just a description of one.
