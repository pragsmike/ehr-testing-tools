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

**Amendment (2026-07-24, additive — not a reversal).** The NIST
licensing deep-research (facts register
[F14](../notes/facts-register.md)/[F15](../notes/facts-register.md))
surfaced a distinction this ADR's original text didn't separate:
artifacts divide into those **redistributed by this repo** (vendored
into what this repo itself ships — must be fully license-verified
before redistribution, no exceptions) and those **fetched by users from
an official upstream source at their own initiative** (this repo's
lockfile records provenance and hash; resolution/fetch happens on the
user's machine against the artifact's official source; the repo itself
redistributes nothing). For the latter category, `license-status`
admits a new value, `:use-permitted--unstated--confirmation-pending` —
the artifact's use rights are plausible (a general policy statement, a
Mode-2 fetch-at-build path) but its formal license is unconfirmed, and
confirmation is pending rather than blocking, since recording a
lockfile entry is not itself a redistribution. This does not relax the
original license-verified bar for anything this repo vendors or ships —
it names a second, genuinely different bar for artifacts this repo
never touches beyond a hash.
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

---
## ADR-0008 — Publish the repository
**Context.** `docs/positioning.md`'s go-public gate set four conditions
before this repo could flip from private to public. The author decided
to publish today (2026-07-24). Walking each condition against its
evidence:

1. **Licensing is clean.** MIT (ADR-0007, above).
   Every dependency this repo actually distributes is verified
   permissive-compatible: EXP-SBOM (`docs/experiments/EXP-SBOM-results.md`)
   classified the CDC-wrapper dependency closure and found 19 of 26
   coordinates license-verified-compatible, with the sole flagged
   outlier (`xom`, LGPL-2.1, facts register
   [F10](../notes/facts-register.md)) reachable only through a NIST-origin
   coordinate this repo does not yet adopt. The six NIST-origin
   coordinates themselves ([F1](../notes/facts-register.md),
   [F9](../notes/facts-register.md)) are `license-unstated` — but this
   repo redistributes none of them: ADR-0005's 2026-07-24 amendment
   (above) records that class of artifact as
   `:use-permitted--unstated--confirmation-pending`, fetched by users
   from NIST's own official channel (`hit-nexus.nist.gov`,
   [F14](../notes/facts-register.md)) at their own initiative, never
   vendored or shipped by this repo (ADR-0005 is the record of that
   distinction). The residual NIST inquiry
   (`docs/experiments/EXP-SBOM-inquiry-draft.md`) narrows that status
   further; it is a narrowing in progress, not a blocker — nothing this
   repo ships depends on its answer.
2. **At least one capability is usable, honestly labeled.** Generation
   (`corpus.generate`) is usable: EXP-A4
   (`docs/experiments/EXP-A4-results.md`) proved clean-environment,
   byte-identical reproducibility from a freshly emptied artifact cache.
   Mutation (`corpus.mutate`) ships too, labeled experimental — it works
   (EXP-B2, `docs/experiments/EXP-B2-results.md`), but it is days old and
   its interfaces may still move. Gates are planned, not built
   (`docs/pipeline.edn`'s `:status :planned` stages) — no condition here
   requires them; the maturity table just says so plainly
   (`README.md`).
3. **CI is green and runs offline.** Added this session:
   `.github/workflows/ci.yml` runs `make test` and `make coverage` on
   push/PR. The suite's hermeticity was verified directly, not assumed —
   a fresh temporary clone, dependencies primed once, then `make test`
   re-run inside a network-isolated namespace: 166 tests, 0 failures, 0
   errors. No test needed `^:integration` tagging because none touches
   the network or a real external engine; every such boundary in the
   current suite is already an injected fake (see `AGENTS.md`'s hermetic
   test-suite rule, added this session).
4. **The referral README is in place.** `README.md` was rewritten this
   session: the pipeline diagram up front, the maturity table, real
   quickstart commands, the guide-relationship paragraph, the scope
   fence, and the MIT license line.

**Decision.** Publish the repository publicly. Publication is not a
release: no version tag is cut, nothing is published to Clojars or
Maven Central, and the guide's own register still waits for this
repo's first release before citing it (`docs/positioning.md`'s
referral-trigger contract, unchanged by this ADR).

**Alternatives rejected.** Waiting for the gates capability to land
before publishing — no go-public condition requires it, and the
maturity table's honest "planned" label already covers the gap; holding
publication hostage to a capability nobody asked this gate to require
would just be scope creep on the gate itself. Waiving the CI condition
for expedience — a verification-tools repo that waives its own
verification gate on the day it goes public is self-refuting; the
condition existed to be met, not talked around, so CI was built instead
of waived.

**Consequence.** Pre-release expectations govern from here: interfaces
may move, and the maturity table in `README.md` is the actual contract
with readers, not a formality. The NIST inquiry's eventual answer will
update facts-register F1/F14 and may upgrade the full `gate.v2` plan
from candidate to adopted. First release — a version tag, published
coordinates, coverage-threshold gating landed, the notation trial
(pattern nursery [#13](../.agents/memory/patterns.md)) concluded — is
the next gate-shaped milestone, tracked in
`.agents/plans/corpus-foundations.md`.
**Status.** Accepted (author-directed), 2026-07-24.

---
## ADR-0009 — Judge/gate factorization
**Context.** `docs/palgebra-design.md` (the palgebra design record,
landed this session) inventoried this repo's own pipeline and found a
recurring conflation: `gate` — an act-layer word, naming a workflow
position that acts on a verdict — had been used to name decide-layer
components that only ever compute a verdict and never act on it. The
`:gate` stage kind (`docs/pipeline.edn`, `docs/notation.md`) bundles two
things that don't have to travel together: judge laws (produce a
verdict plus findings; never modify the subject) and a routing policy
(splitting output into pass / rejected / indeterminate). Repo evidence
that verdict and action are already independent, not merely
separable-in-theory: contract pairing
(`test-integration/contract_pairing_test.clj`) treats `:rejected` as
*success* — same judge, opposite polarity, supplied by the workflow;
baseline-relative gating (`--baseline`, P6) applies a different action
to identical findings depending on a policy argument, added without
touching any judge. One level deeper, the same conflation recurs: the
per-finding key `gate.fhir` sets is named `:policy`, but it records a
criterion-layer datum (this issue's contribution to the verdict, per
the versioned EXP-C5 mapping) — the same act-layer word, wearing a
decide-layer fact.
**Decision.** Components that decide are **judges**; `gate` is reserved
for workflow positions that *act* on a verdict. `gate.fhir`, `gate.v2`,
`gate.finding`, and `gate.report` are renamed to `judge.*` — libraries
of judges and their judgment machinery, not gates. `docs/pipeline.edn`'s
`:gate` stage kind is renamed `:judge`; the kind's law is narrowed to
the judge law alone (never modifies its subject; produces a verdict and
findings), and the old kind's three-way output split is documented as
the derived, policy-bearing construct `gate = judge ⨟ route-by-verdict`
— derivable, not primitive, which is this ADR's one-line technical
justification for the split. The finding field `:policy` is renamed
`:disposition` — a criterion-layer datum, not an act-layer one; `policy`
is reserved for the verdict→action layer this factorization names. The
CLI verb `ehr gate` **keeps its name**: it genuinely is a gate — its
exit-code mapping is the policy, the shell is the actor, and
`--baseline` is already an explicit policy argument passed to it. Cites
D1, D2, D11, D12 in `docs/palgebra-design.md`.
**Rejected.** Renaming the CLI verb — `ehr gate` is the one place in
this repo that actually acts on a judgment (exit-code mapping), so it is
correctly named already; renaming it would erase the one real gate to
chase a false symmetry with the libraries being renamed away from it.
Renaming the verdict values (`:pass`/`:rejected`) — priced and deferred
to a later signature-format revision (`docs/palgebra-design.md` O1):
they are act-flavored words but are serialized in committed reports and
asserted in both integration suites, and renaming them is a separable,
higher-blast-radius change from the structural judge/gate split this
ADR makes. Leaving the `:gate` kind's three-way split as primitive —
the design record's factorization is exactly that this split is
composable from a judge plus a routing policy, and leaving it primitive
would keep the conflation this ADR exists to remove.
**Consequence.** `src/ehr_testing_tools/gate/` becomes
`src/ehr_testing_tools/judge/` (and its test tree correspondingly);
every require site, `docs/pipeline.edn`, `docs/use-cases.edn`,
`docs/notation.md`, and `docs/gate-calibration.md` (renamed
`docs/judge-calibration.md`) are updated to match, mechanically, with no
behavior change — verified by the suite staying green, `make pipeline`/
`make use-cases` regenerating cleanly, and `make lint-pipeline` passing.
The verdict split this design record also calls for
(`no-verdict(cause)`, `docs/palgebra-design.md` D10) is explicitly out
of scope for the rename session this ADR accompanies — it is the one
semantic change in `.agents/plans/judge-gate-refactor.md`, scheduled and
isolated as its own later phase so it doesn't camouflage inside
mechanical renames.
**Status.** Accepted (author-directed), 2026-07-25.

---
## ADR-0010 — Verdict partiality is explicit: the no-verdict arm
**Context.** `:indeterminate` conflated two different things under one
name: "the criterion doesn't decide this subject" and "the judge could
not reach a verdict" (operational partiality — in practice,
`judge.fhir`'s terminology-suppressed classification, its only
producer). `docs/palgebra-design.md` D10 already named the split;
O2 asked which reading `judge.fhir`'s terminology-suppression case is.
**Decision.** The verdict type gains a fourth value: the four-value
verdict set is `:pass`, `:rejected`, `:indeterminate` (reserved, no
longer produced), `:no-verdict` (paired with a `:cause` keyword,
Malli-enforced — present if and only if the verdict is `:no-verdict`).
Abstractly the judge is total, and partiality is introduced by
lowering (design §II.5, §III.2) — `no-verdict` is the cost layer's one
declared leak, given a typed channel and a totality law. O2 resolved as
designed: `judge.fhir`'s terminology-suppressed classification is
`no-verdict(:terminology-suppressed)` — the judge failed to fully
*apply* the criterion; the criterion didn't fail to decide.
`worst-of` ranks `:rejected` above `:no-verdict` above
`:indeterminate` above `:pass` — revised during this same session's
Step 5 integration run: the first draft ranked `:no-verdict` above
`:rejected` outright, but every real, US-Core-profiled Synthea file
mixes terminology-suppressed findings with genuine profile-driven
violations in the same file (EXP-C5) — confirmed directly, one mutant
bundle carried 3062 terminology-suppressed issues alongside its
injected defect's own — so that ordering made every real file's
aggregate verdict `:no-verdict` regardless of an actual detected
defect, overriding rather than merely losing to contract-pairing's and
baseline-gating's polarity regression (both integration suites failed
on exactly this). The revised ordering still ranks `:no-verdict` above
`:indeterminate`/`:pass` — a corpus the judge couldn't fully apply its
criterion to is worse than one the criterion simply didn't decide — but
a confirmed violation elsewhere in the same file still dominates the
aggregate. Noted as a policy-flavored ranking, an O3-adjacent exhibit
rather than a neutral fact. `:indeterminate` keeps its name in v1 (same conservatism
as O1: old baseline reports still serialize it); renaming to
`:ambiguous` is deferred to signature-format v2, alongside O1's own
verdict-name question.
**Consequence.** Every consumer of verdicts must handle the fourth arm:
`worst-of`, `judge.report`'s schema/aggregation/diff/baseline-relative
reading (old, pre-split baselines still read forward, unmigrated), and
the CLI. The CLI's distinct default exit code for a no-verdict outcome
is this law surfacing at the act layer: no workflow silently inherits
a no-verdict-handling default; `--treat-no-verdict-as pass|rejected`
is the explicit opt-in to fold it into an existing polarity.
**Rejected.** Folding causes into findings — policies would have to
spelunk findings to distinguish partiality from a genuine criterion
violation, exactly the conflation this record removes. Renaming
`:indeterminate` now — serialization stability (old reports/baselines
already carry the name); joins O1 at signature-format v2 instead of
being decided piecemeal.
**Cites.** D10, O1, O2 (`docs/palgebra-design.md`).
**Status.** Accepted (author-directed), 2026-07-25.
