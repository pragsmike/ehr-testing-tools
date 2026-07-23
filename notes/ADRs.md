# Architecture / Authoring Decision Records

<!-- Each record: Context / Decision / Alternatives rejected / Consequence.
     Status: Accepted unless noted. These capture WHY, where AUTHORS-GUIDE.md
     captures WHAT-TO-DO. All records are kept together in this single file
     (notes/ADRs.md); they are intentionally not fanned out into separate
     per-record files. Do not silently revert an Accepted decision; supersede
     it with a new numbered record. -->

## ADR-0001 — License: Apache 2.0
**Context.** This repo will be published and must interoperate with a JVM
healthcare tooling ecosystem that is predominantly Apache-2.0/MIT (HAPI FHIR,
Synthea) with some MPL/GPL dual-licensed components consumed as unmodified
dependencies under their MPL election. Every dependency-compatibility
judgment made from here on — including the pending NIST/CDC SBOM work
(EXP-SBOM, see `docs/experiments.md`) — needs a fixed target to be judged
against.
**Decision.** Apache License 2.0 for all original code and docs in this
repo.
**Rejected.** MIT — permissive enough, but Apache's explicit patent grant
suits healthcare tooling better, where downstream consumers can least
afford a submarine patent surprise. EPL — Clojure-conventional, but it
would complicate mixing with the Apache-heavy dependency set for no
benefit specific to this project.
**Consequence.** Dependency review always asks one question: "compatible
with Apache 2.0 distribution?" Copyleft-only dependencies (GPL without an
MPL/Apache-compatible election) are adoption blockers, not judgment calls.
**Status.** Accepted (author-directed).

---
## ADR-0002 — Inherit the guide's authoring discipline, adapted
**Context.** The sibling `ehr-testing-guide` repo evolved working
conventions over its own history — single-file ADRs, a claims register
with an external-factual-claims table (its ADR-0013), per-entry
verification dates, a pack ritual for session context, WSL-only commits.
Five factual errors in that repo were caught precisely by that discipline
(its `notes/claims-register.md` F1–F4), not by casual review.
**Decision.** This repo adopts the same mechanisms from day one:
`notes/ADRs.md` (this file), a facts register (`notes/facts-register.md`,
F-rows only — there is no manuscript here, so no C-table of drafting
coverage), the pack target with a freshness header (`Makefile`), and
WSL-only commits.
**Rejected.** (a) Deferring process until the repo "is real" — the guide's
errors show facts rot fastest early, when assertions outrun verification,
which is exactly the phase this repo is in. (b) Inventing different
conventions for this repo — one author, two repos, no reason for two
disciplines.
**Consequence.** Any externally verifiable fact asserted in this repo's
docs gets an F-row with evidence and a verification date (see F1 in
`notes/facts-register.md`). Packs are the unit of session context; the
pack header makes staleness a one-glance check instead of a manual diff.
**Status.** Accepted (author-directed).
