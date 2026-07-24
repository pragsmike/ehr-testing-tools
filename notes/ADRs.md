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

---
## ADR-0003 — Agent-facing layout and prompt archive
**Context.** The guide repo standardized on `AGENTS.md` plus `.agents/`
(skills, handoffs) as its agent-facing layout. Both repos are driven by
chat-designed, Code-executed prompt sessions whose prompts are the real
provenance of changes — which file got touched, in what order, and why —
but those prompts currently live outside the repo, in the chat transcript
only.
**Decision.** This repo adopts the same standard layout: `AGENTS.md` at
root plus `.agents/skills/`, `.agents/handoffs/`, `.agents/prompts/`.
Executed Code prompts are archived in `.agents/prompts/` named
`YYYY-MM-DD-<slug>.md`. `CLAUDE.md` is kept as a thin pointer shim to
`AGENTS.md`, matching the guide's convention.
**Rejected.** (a) `notes/prompts/` — `notes/` is for the repo's own
knowledge (ADRs, registers); prompts are agent-facing operational
artifacts, not reasoning-of-record, and belong under `.agents/` instead.
(b) Not archiving prompts at all — this loses the provenance chain that,
in the guide repo, caught real errors no other mechanism caught.
**Consequence.** Every Code session's prompt lands in `.agents/prompts/`
as part of that session's own commits. Handoffs go to `.agents/handoffs/`
via the `handoff` skill.
**Status.** Accepted (author-directed).

---
## ADR-0004 — Internal structure: capabilities as data transformations
**Context.** The positioning doc left internal organization open, with a
working proposal (corpus construction vs. gating, CLI-first, data
orientation). The author has ratified: organize by data and function
rather than by tool or format; CLI; Malli for schemas. Component-
selection research (`docs/research/`) found no existing open-source
composition of these capabilities to imitate, and found Clojure-EHR
practice constrains the interop layer (thin Java-interop wrappers,
schemas as data), not repository shape.
**Decision.** One artifact, one source tree, organized by what happens to
data:

```
ehr-testing-tools.corpus.generate    ; valid data into existence
ehr-testing-tools.corpus.mutate      ; controlled defects from valid data
ehr-testing-tools.corpus.manifest    ; provenance records
ehr-testing-tools.gate.fhir          ; FHIR conformance verdicts
ehr-testing-tools.gate.v2            ; HL7 v2 conformance verdicts
ehr-testing-tools.gate.report        ; verdict normalization and diffing
ehr-testing-tools.artifact           ; external-input registry (ADR-0005)
ehr-testing-tools.cli                ; the only namespace that prints
```

Formats appear as sub-namespaces or dispatch inside capabilities, never
as top-level organization; adding a format (e.g. C-CDA) adds leaves, not
structure. Doctrine: every capability is a pure function from data to
data; verdicts, manifests, and mutation records are EDN-representable
values with Malli schemas colocated with the namespaces they describe;
the CLI is a thin shell (parse → call → print → exit code) using
`babashka.cli`; engines that cannot be pure (subprocesses) are confined
behind a single invocation wrapper that returns data including the
invocation record. Core functions return result data — including
failure results — rather than throwing; exceptions are reserved for
programmer error; the CLI maps results to exit codes.

The CLI is invoked as `ehr` (subcommands: `ehr corpus generate`,
`ehr gate fhir`, `ehr artifact fetch`, …). Exit-code contract: 0 = ran and
passed; 1 = ran and the check legitimately rejected; 2 = operational
error (missing artifact, bad invocation). Output contract: EDN is
canonical; every command accepts `--json` emitting a projection; JSON is
never the source of truth. Naming note: `ehr` is a generic domain term
claimed by one toolkit; the name gets one deliberate revisit at first
public release, when the cost of change becomes nonzero.
**Rejected.** Per-format organization — multiplies every capability by
every format, and the method being served is capability-shaped, not
format-shaped. Multiple artifacts per capability — pinning and
distribution complexity now for an independence nobody needs; the
per-capability maturity ladder already signals readiness independently.
Framework-style components/protocols — this is a toolkit of functions,
not an application framework.
**Consequence.** The corpus/gate split mirrors the guide's chapter
structure (corpus: ch 23; gates: ch 25), making the tools-cite-chapter
contract nearly mechanical. The maturity ladder attaches per top-level
capability namespace. The positioning doc's open structure decision
closes (see `docs/positioning.md`). Malli and `babashka.cli` enter
`deps.edn` when the first capability code lands (P3), exact-pinned.
**Status.** Accepted (author-directed).

---
## ADR-0005 — External inputs are locked artifacts
**Context.** Every engine this repo wraps is really engine plus parameter
artifacts: Synthea takes module sets and properties files; the FHIR
validator takes IG packages; v2 gates take conformance profiles; future
mappers take mapping specs; terminology checks take value sets. Left
unnamed, each wrapper invents its own pinning and the reproducibility
manifest degenerates into special cases. The problem statement's
reproducibility, profile-management, and offline needs all reduce to one
mechanism if this is named.
**Decision.** Every external input is an artifact: `{kind, name, version,
sha256, source, acquired, license-status}`. A committed lockfile —
`artifacts.lock.edn` at the repo root, deliberately parallel to
`deps.edn` — is the only path by which tools receive external inputs:
resolution is by name and version through the `artifact` registry, which
verifies the hash before handing over a filesystem path. Artifact bytes
live outside git in a content-addressed cache
(`~/.cache/ehr-testing-tools/artifacts/<sha256>`, overridable via
`EHR_TESTING_TOOLS_CACHE`), materialized by `ehr artifact fetch` from
each artifact's recorded source; offline CI runs against a
pre-populated cache. Manifests reference artifacts by
`{name, version, sha256}`, so a manifest plus the lockfile plus the cache
suffices to reproduce any run.

Boundary: acquired, external, or binary inputs go in the lockfile;
repo-authored text — properties files we write, module sets we author,
profiles we own — is ordinary git-versioned source, referenced by
manifests via path plus content-hash at run time. Git is already the
lockfile for what git owns; the artifact machinery exists for what it
doesn't.
**Rejected.** Artifacts committed to git — the Synthea distribution alone
is on the order of 100MB, and git-lfs adds infrastructure for no gain
over a content-addressed cache. Per-tool pinning conventions — the
special-case rot this ADR exists to prevent. Version pins without
hashes — versions are claims, hashes are facts; the FHIR validator's
package-cache version-skew bug (see `docs/research/`) is precisely a
version claim failing silently.
**Consequence.** `license-status` on every artifact makes the go-public
gate's licensing condition a mechanical scan of the lockfile. Profile
management becomes artifact management: IGAMT exports and IG packages
are artifacts of kind `:profile`. EXP-D3's NIST-artifact mirroring
becomes `ehr artifact fetch` against a local mirror source.
**Status.** Accepted (author-directed).

---
## ADR-0006 — Test-first, staged enforcement
**Context.** Capability code starts this session; the repo's credibility
rests on verification discipline, and its own method (the guide's) is a
testing method — a test-shy testing-tools repo is self-refuting. Sessions
are executed by agents that cannot see each other; discipline must be
written and mechanical, not remembered.
**Decision.** Test-first is a hard rule — a failing test precedes the
implementation it motivates; sessions demonstrate red→green in their
reports; property tests are required for law-bearing constructs
(canonicalizer laws, hash verification, schema round-trips); coverage is
measured (`cloverage` via a `:coverage` alias and `make coverage`) and
regressions in coverage require justification in the session report.

Enforcement is staged: **now** — convention + prompt discipline +
coverage measurement; **enforcement wave** (planned, see
`.agents/plans/corpus-foundations.md`) — pre-push hook running the
suite, offline GitHub Actions, coverage threshold gating.
**Rejected.** Full mechanical enforcement immediately — procrastination-
by-perfectionism; blocks the first capability on CI plumbing. Coverage
as vibes — unmeasured "good coverage" is unfalsifiable.
**Consequence.** Every code-producing prompt carries the red→green
reporting duty; `AGENTS.md` hard rules gain: "Test-first: a failing test
precedes implementation; red→green evidence in session reports;
`make test` and `make coverage` green/reported before any session-final
commit."
**Status.** Accepted (author-directed).

---
## ADR-0007 — License: MIT (supersedes ADR-0001)
**Context.** ADR-0001 chose Apache 2.0 for the patent grant and ecosystem
fit. The author's other public projects are MIT; a single license
posture across projects reduces per-repo reasoning for humans and agents
alike; the repo is pre-release with no external contributors or
downstream users, making this the cheapest moment a relicense will ever
be. All EXP-SBOM compatibility findings (see `docs/experiments/EXP-SBOM-
results.md`) were evaluated against "permissive open-source
distribution" and transfer unchanged — MIT and Apache 2.0 are in the
same compatibility class for every dependency examined (EPL entries, the
MPL election, the LGPL flag included).
**Decision.** MIT for all original code and documentation. Attribution
semantics are equivalent for the project's purpose (retain the notice in
copies).
**Rejected.** Staying on Apache 2.0 — its explicit patent grant and §5
contribution term are real but modest for testing tooling; the trade is
accepted knowingly. Dual licensing — complexity without a constituency.
**Consequence.** `LICENSE` replaced with MIT (standard text, author's
copyright line, current year). Forward-looking docs that named the
Apache target are updated: `docs/components.md`'s framing where it said
"against the Apache-2.0 target", `docs/positioning.md`'s go-public gate,
and ADR-0001's own dependency-review question — "compatible with Apache
2.0 distribution?" — is superseded by "compatible with MIT
distribution?" (same compatibility class, same answers for every
dependency examined to date). Historical documents — the EXP-SBOM
protocol and results, archived prompts, and ADR-0001 itself — are dated
records and are not edited; this record supersedes ADR-0001, it does not
revise it.
**Status.** Accepted (author-directed), 2026-07-24.
