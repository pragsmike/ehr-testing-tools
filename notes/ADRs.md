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

**Projection frame (retrofit).** The file-level keyword verdict is a
conservative *projection* of the true per-file aggregate ⟨verdict over
decided findings, coverage⟩ — "decided" is the fold over every finding
the judge could actually classify (rejected if any, else pass);
"coverage" is complete iff no finding was classified `:no-verdict`.
`worst-of` collapses that pair to one keyword: `:rejected` wins
outright regardless of coverage; short of that, incomplete coverage
collapses even an all-`:pass` decided portion into `:no-verdict`,
deliberately — "passed" must mean "checked and clean," not "clean on
what we managed to check." This is *exactly* the pre-split fold
(`:rejected > :indeterminate > :pass`, with `:no-verdict` occupying
`:indeterminate`'s old role) — the old three-valued code already had
the right projection; it only lacked the theory naming it and the
`:cause` channel explaining *why* coverage was incomplete. The
discarded coverage dimension is not lost, though: `judge.report`'s
per-file `:no-verdict-causes` (added in this retrofit) surfaces it back
independently of which keyword the projection picked, so a `:rejected`
file can still show its own partiality.

This design's own text already carried the warning against the first
draft's ranking: D10 states `no-verdict` sits "in a different type
position than the verdicts proper" (§II.5) — i.e. it names an
orthogonal *coverage* signal, not a fourth point commensurate with the
pass/rejected axis. Ranking it above `:rejected` in the SAME
totally-ordered fold `worst-of` maxes over did exactly what that
warning cautioned against: it made `:no-verdict` commensurate with (and
dominant over) the decided verdicts, collapsing the "different type
position" into "same axis, top slot." Placing an *absence of decision*
at the top of a max-based fold makes it an absorbing element: any
single undecided finding overrides every decided one, no matter how
much concrete evidence — rejected or otherwise — coexists with it. This
was falsified by measurement, not merely re-argued on paper: one real
mutant bundle carried 3062 terminology-suppressed issues out of 7736
total, alongside its own clearly-`:rejected`-worthy injected defect —
proof the collision is the common case for real, profile-stamped
corpora, not a corner case.
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
being decided piecemeal. **Ranking `:no-verdict` above `:rejected`
outright (this ADR's own first draft, retrofitted here as a formally
rejected alternative)** — the projection-frame analysis above states
why: it collapses `worst-of`'s "different type position" (D10) into
"same axis, dominant slot," turning an absence-of-decision into an
absorbing element under the max-based fold. Falsified by measurement
(3062/7736 terminology-suppressed issues on one real mutant bundle,
alongside its own genuine defect), not merely reasoned around after the
fact.
**Cites.** D10, O1, O2 (`docs/palgebra-design.md`).
**Status.** Accepted (author-directed), 2026-07-25.

---
## ADR-0011 — External data artifacts vendor into the tree; engine artifacts stay in the registry
**Context.** ADR-0005 made every external input an artifact resolved
through `artifacts.lock.edn` and materialized outside git in a
content-addressed cache. Every artifact adopted under it so far has been
an *engine* or an engine's bulk input: the Temurin JDK, the Synthea
distribution, `validator_cli.jar` — tens to hundreds of megabytes,
binary, and in one case license-unstated (ADR-0005's 2026-07-24
amendment names that class explicitly). The mechanism was built for that
shape. This session adopts the repo's first external **data** artifact:
Google SimHospital's bundled 1,013-message HL7 v2.3 corpus
(`docs/artifacts/messages.out`, 1,158,713 bytes of Apache-2.0 ASCII),
identified by `docs/research/HL7v2-sanitized-corpus-research.md` as the
best freely available foreign v2 corpus and needed as *fast-path test
input* — the P7 residue the plan has tracked as externally blocked since
2026-07-25. Nothing in ADR-0005 says which mechanism a small,
redistributable, test-path data file belongs to, and the two candidate
answers have materially different consequences.

**Decision.** Split the two by role, not by the fact of being external.

*Vendor into the tree, with a provenance sidecar*, when all three hold:

1. **Size is in the ~megabyte order of magnitude**, not the ~hundred-
   megabyte one — small enough that git owning every byte is cheaper
   than a cache plus a fetch path, and diffable enough that a change to
   it is visible in review.
2. **The license permits redistribution**, verified by reading the
   upstream license artifact itself, not a description of it — because
   vendoring *is* redistribution, and ADR-0005's amendment holds the
   redistributed class to the strict bar with no exceptions.
3. **Its role is fast-path test input** (`test/`), not integration-tier
   engine configuration. This is the load-bearing criterion: `AGENTS.md`'s
   hermeticity rule is a *path split*, so anything a `test/` test needs
   must be present from a cold clone with no network. A registry-fetched
   input can only be consumed from `test-integration/`, i.e. the nightly
   tier.

A vendored data artifact ships with a `PROVENANCE.md` beside it
recording upstream URL pinned to a resolved commit SHA, sha256, byte
count, retrieval date, upstream status, structural counts, and any
handling caveats — plus F-rows for each externally verifiable claim, per
`AUTHORS-GUIDE.md` section 4. Provenance is the price of vendoring;
`artifacts.lock.edn` carries this metadata for registry artifacts, and a
vendored artifact must not lose it just because git is holding the
bytes.

*Stay registry-fetched* otherwise: large, generated-on-demand,
binary, or license-encumbered artifacts — the JDK, Synthea, the
validator CLI, and the eventual IG package, unchanged.

**First instance:** `test/fixtures/v2/simhospital/` (this session) —
the corpus, upstream `LICENSE`, and `PROVENANCE.md` together, with the
corpus's CR framing protected by a `.gitattributes` `-text` entry
alongside the existing v2 fixture rule. **Anticipated registry-side
case:** the research doc's Phase B output — a pinned SimHospital run
producing the missing `ORM^O01`/`A02`/`A03` story. That is *generated*
by an engine from a pathway config, plausibly bulk, and its inputs are
engine configuration; the pathway files this repo authors are ordinary
git-versioned source (ADR-0005's "git is already the lockfile for what
git owns"), while any bulk generated output beyond fixture scale is an
artifact.

**Alternatives rejected.** *Registry-pinning the corpus* — it would
demote every consumer of it to `test-integration/` and the nightly tier
by construction, which is precisely backwards for a file whose entire
purpose is to be the substrate's fast, per-push input; and upstream was
archived on 2025-03-28, so fetch-at-use would make a permanently frozen,
unmaintained URL a single point of failure for the suite forever, where
vendoring extinguishes that dependency at adoption time. *Committing the
corpus without provenance, as just another fixture* — the fixture
precedent covers five short hand-written messages this repo authored and
owns; it does not cover a sourced third-party artifact whose license
carries a notice obligation, whose upstream can move or vanish, and
whose structural claims this repo's own docs assert. A file nobody can
trace is a file nobody can re-verify.

**Consequence.** ADR-0005's boundary sentence gains a third case:
acquired-external-and-binary → registry; repo-authored text → ordinary
source; acquired-external-small-redistributable-data → vendored *with*
the provenance the registry would otherwise have carried. The
hermeticity rule and the vendoring criterion now point the same
direction, which is the intended relationship: a test on the `test/`
path may depend on vendored data precisely because vendoring is what
makes it cold-clone-available. Vendoring also imports a standing
obligation this repo did not previously carry — the upstream `LICENSE`
travels with the bytes, and any *derived* corpus published from a
vendored one must first walk the research doc's §5 sanitization gate
(the SimHospital corpus contains realistic demographics including
valid-format NHS numbers; acceptable as committed internal test input,
not as a published derivative).
**Status.** Accepted (author-directed), 2026-07-26.

---
## ADR-0012 — The CLI properties `ehr-testing-sim` mounts against
**Context.** `ehr-testing-sim` (a sibling repo, not a dependency of this
one — the arrow points tools → sim only, and nothing in this repo's
compile path touches sim today) was designed to mount into `ehr` as an
`ehr sim` subcommand group with roughly four lines of change on this
side. Its maintainer sent a note explaining that the ease is not
accidental: it rests on five specific properties of this repo's CLI
architecture, mostly consequences of ADR-0004, plus one commitment about
`corpus/manifest.clj`'s schema. The note is vendored verbatim at
[`notes/ehr-testing-sim-mounting-note.md`](ehr-testing-sim-mounting-note.md)
as provenance; this record is the commitment.

The reason to record it *now*, before any mount exists: none of these
properties is individually precious, so a refactor could change one
without anybody noticing it was load-bearing, and the breakage would
surface at runtime in the *other* repo, where this repo's tests cannot
see it. An interface commitment nobody wrote down is not an interface.

Every claim below was re-verified against source while writing this
record (2026-07-26, `src/ehr_testing_tools/cli.clj`,
`cli/help.clj`, `result.clj`, `corpus/manifest.clj`,
`corpus/intake.clj`, `test/ehr_testing_tools/cli/help_test.clj`); where
the note's description and the code disagree, **the code is recorded and
the disagreement is named**. Three such corrections appear, marked
*[correction]*.

**Decision.** These five properties, and the manifest commitment below,
are interface commitments of this repo's CLI: refactor freely *within*
each line, and treat crossing one as a change that needs a superseding
record and a heads-up to sim, not a silent internal cleanup. Scope is
exactly this: what the mount seam depends on. Everything about *how* a
mount would be built is out of scope (see the closing paragraph).

**1. Dispatch is a data-in / data-out function keyed on `[group action]`
positionals.** `cli/dispatch` takes already-parsed `[args opts]`,
destructures `[group action path]`, and returns a Result map to
`main!`, which renders and exits. *Safe:* renaming `dispatch`,
reordering or reorganizing arms, adding groups, replacing the `case`
with data-driven routing (a group → handler map is fine). *Breaking:*
dispatching on raw unparsed argv; requiring a handler to print or
`System/exit` itself; passing `opts` in a per-group shape.

**2. One `babashka.cli` parse, one spec.** `cli/parse` calls
`babashka.cli/parse-args` exactly once, with `cli/cli-spec`, and
`main!` is the only caller — coercion happens once, host-side, before
dispatch. Today's spec coerces `:seed` and `:population` to `:long`,
`:json` to `:boolean`, and carves `:reference-date`/`:version` out to
`:string` because they are digit-shaped *identifiers* (a coerced long
breaks `ProcessBuilder`'s `String[]`). `:help` is not in the spec at
all — `--help` arrives as `:help true` from babashka.cli's own default
handling. *Safe:* adding spec keys; merging another spec's keys in
before parsing. *Breaking:* parsing per-group, parsing after dispatch,
or moving off babashka.cli's coercion semantics — flags this repo never
declared would inherit the change invisibly. The hazard the note names
is real and unmitigated: merged specs disagreeing on a shared key are
last-merge-wins, silently.

**3. Result maps are structurally typed, not nominally.** `result.clj`
defines `Result` as a Malli map — `:status` (`:ok`/`:rejected`/
`:error`), optional `:category`, `:payload` — and the shell renders and
exit-codes any conforming map without knowing which namespace built it.
*Safe:* adding optional keys; adding categories; extending the
exit-code table. *Breaking:* new required keys, or records/protocols in
place of plain maps.

*[correction]* The note says interpreting `:category` globally rather
than per-status would be breaking. The shell already interprets exactly
two category values globally, and has since DOC-1/ADR-0010:
`:gate-no-verdict` maps to exit 3 in `result->exit-code`, and
`:cli-help` makes `main!` print `:payload`'s `:text` verbatim instead of
rendering EDN. CLI-2 adds a third category the shell itself produces,
`:report-write-failed`, but that one is not interpreted specially. So
the commitment is narrower than the note assumes and states a real
constraint on sim: those two names are globally meaningful and a
foreign category vocabulary must not reuse them. Adding a *third*
globally-interpreted category is the breaking change.

**4. Help is data this repo's machinery walks.** `cli/help/cli-spec` is
`{:program, :exit-codes, :global-flags, :groups [...]}`, and a group is
`{:group, :doc, :verbs [{:verb, :doc, :flags [{:flag, :doc, :default}]}]}`
— matching the shape the note describes, plus two optional keys the
`gate` and `check` groups use for their positional conventions
(`:positional`, `:positional-doc`). Renderers are pure functions over
it. *Safe:* any change to rendering. *Breaking:* reshaping the help data
model without a shim.

*[correction]* The note expects this repo's help-vs-dispatch coverage
test to start covering a mounted sim group "for free." It will not, and
that is better than it sounds: `test/ehr_testing_tools/cli/help_test.clj`
checks both directions through two *hand-mirrored* structures — a
`stub-key` `case` and a `known-dispatch-pairs` set. A new group that
appears in the spec and in dispatch without an entry in both fails
loudly (the `case` has no default). Mounting sim therefore requires
updating those two, which is a two-line chore that cannot be forgotten,
not free coverage.

**5. The `-fn` injection point in dispatch.** `dispatch`'s 3-arity
option map carries one injectable function per command
(`:fetch-fn`, `:gate-v2-fn`, `:check-fn`, …), defaulting to the real
one; `main!` does the same for `:dispatch-fn`/`:println-fn`/
`:exit-fn`. This is what keeps CLI tests hermetic (AGENTS.md's rule),
and a mounted sim arm gets the same treatment — an injectable
`:sim-fn` — so this repo's tests never load a simulation engine.
*Breaking:* dropping the injection convention, or hard-wiring one arm's
handler.

**The manifest commitments.** Sim emits run manifests shaped to
`corpus/manifest.clj`'s `ManifestV1_1` and holds a mirror of that schema
with a tripwire test, which can only detect drift after the fact. Two
commitments, both of which this repo already keeps:

- **Version, don't mutate.** `ManifestV0` (`:schema-version` `0`),
  `ManifestV1` (`1`), and `ManifestV1_1` (the *string* `"1.1"`, not the
  integer 2 — deliberate, per that namespace's own docstring) coexist
  frozen; nothing regenerates or reinterprets an older manifest as a
  newer one. That discipline continues: a shape change gets a new named
  schema and a new version value, never an edit in place.
- **The binding contract test belongs here.** Sim cannot host a test
  that validates its emitted manifest against this repo's authoritative
  schema without inverting the dependency arrow. When sim is mounted,
  that test lives in this repo's `test-integration/` tree (it runs a
  real simulation subprocess — the hermeticity path split applies).

*[correction]* The note describes the manifest as the shape "so
`ehr corpus intake` can ingest a sim run." `corpus/intake.clj` never
reads a manifest: it catalogs every file it finds by content hash and a
sniffed format (`:fhir-json`/`:v2-er7`/`:unknown`), and a `manifest.edn`
sitting in a source directory would be catalogued as `:unknown` like any
other unrecognized file. Intake ingesting a sim *corpus* is true; intake
validating or consuming a sim *manifest* is not a thing this repo does
today, and the contract test above — not intake — is what would catch
manifest drift.

**Alternatives rejected.** *Recording nothing until sim is actually
mounted* — the properties are load-bearing now, and the whole failure
mode is a refactor that looks internal from inside this repo; CLI-2
itself edited `cli.clj` before this record existed, which is the
argument. *Adopting the note as the record* — a cross-repo note is one
party's account, and three of its claims turned out not to match this
repo's code; an ADR that says "see the note" would have inherited those.
The note is kept verbatim as provenance instead. *Designing the mount
here* — see below; a commitment and a design have different lifetimes.

**Consequence.** A refactor near `cli/parse`, `cli/dispatch`,
`result.clj`, or `cli/help.clj` now has a stated line to check, and a
one-line comment at the dispatch site points here. Explicitly **out of
scope of this record and of CLI-2**: dependency coordinates for sim,
whether the sim namespace loads optionally, the startup assertion that
merged specs agree on shared keys (the property-2 hazard — worth doing,
not decided here), the `:sim-fn` default, and the contract test itself.
Those are mount-time design choices and belong to the session that does
the mount. No `"sim"` arm exists in `dispatch` today, and this record
does not add one.
**Status.** Accepted (author-directed), 2026-07-26.

---
## ADR-0013 — The cross-repo consumer loop: sim consumed by subprocess, findings not failures, baseline-delta drift detection
**Context.** `ehr-testing-sim`'s own ADR-0001 (clause 5) assigned this
repo's integration tree the binding manifest contract test its own
mirror (`ehr-testing-sim.manifest/MirroredManifest`) cannot provide for
itself — a mirror only detects drift from what it once copied, never
that the copy now disagrees with the authoritative source
(`corpus/manifest.clj`'s `ManifestV1_1`). That debt, and sim's own
problem-statement validation claim #6 (fitness as a test instrument —
`ehr gate` judging sim-generated traffic), had never been executed: no
test in this repo had ever invoked sim, and no sim-generated corpus had
ever been gated. This session builds that first loop. ADR-0012 already
recorded the interface commitments a future `ehr sim` mount would rest
on and explicitly deferred the mount itself until the classpath question
resolves (sim going public, or a private-dependency mechanism); this
record does not revisit that deferral, only the narrower, already-open
question of how this repo's *tests* consume sim today, without a mount.

**Decision.**

1. **Subprocess, never a classpath or `deps.edn` dependency.** Every
   test in this loop invokes sim's own CLI (`clojure -M:cli run ...`) as
   a subprocess in the sibling checkout (`../ehr-testing-sim`), through
   `ehr-testing-tools.invocation/run!` — the same injectable wrapper
   `judge.fhir` and `corpus.generate` already use for every other
   pinned-engine subprocess (pattern nursery #2); no second subprocess
   convention was invented for sim. `ehr-testing-tools.sim-harness`
   (`test-integration/`) is the seam: it shells out, captures stdout,
   and parses sim's own printed EDN `Result` map — nothing about sim's
   internals is assumed beyond that public, already-stable CLI contract
   (`ehr-testing-sim.cli`'s own `help-group`/`dispatch-action`, ADR-0001
   there). This is deliberate and load-bearing, not a placeholder for a
   future mount: sim is a private repo today and this repo is public
   (ADR-0008); a git or Maven dependency from a public repo onto a
   private one breaks public CI outright (no credentials to resolve it),
   and even once sim is public, a classpath dependency would invert
   ADR-0012's own stated arrow (tools depending on sim's *code*, not
   merely its CLI contract) and tangle the two repos' version lockstep
   for a relationship this loop doesn't need — everything this session
   does only ever needs sim's stdout, never its namespaces.
2. **Every sim-dependent test skips cleanly when the sibling is
   absent.** `sim-harness/available?` guards every test built on it;
   the guard prints `sim-harness/absence-message` and records a passing,
   visible `is true` assertion rather than either failing (public CI has
   no sibling checkout and must stay green) or silently vanishing (a
   skip with no trace is indistinguishable from a test nobody wrote).
   This is why the tests live in `test-integration/` and not `test/`:
   AGENTS.md's hermeticity rule is a path split, and a subprocess
   dependent on an optional sibling checkout is exactly the shape that
   split exists for.
3. **Findings, not failures, is the assertion discipline for what sim
   actually returns.** Three of this session's four tests found a real
   mismatch between what this repo's schemas/machinery expect and what
   sim's Package-less v0 output actually provides, and none of the three
   was resolved by loosening this repo's own side: the manifest contract
   test asserts real conformance to `ManifestV1_1` and is currently
   **red** (sim's manifest omits `:schema-version` entirely) — left red,
   not patched, because the red *is* the deliverable, exactly the
   drift-tripwire ADR-0012 anticipated; the gate loop test does not
   assert all-pass (a corpus that's too well-behaved to reject anything
   is itself measured, not asserted away) and, measured rather than
   assumed, currently finds zero rejections at the v2 base-structural
   tier; the intake trial confirms, by running it rather than by
   quoting ADR-0012's own correction, that intake's catalog carries none
   of a dropped-alongside manifest's provenance fields. AUTHORS-GUIDE.md
   section 7's two-failure-modes discipline (a sound check disagreeing
   with reality is a finding; a check misencoding its own invariant is
   an escalation) is the general rule this decision instantiates for the
   cross-repo case specifically.
4. **Baseline-delta is how the gate loop's own drift becomes visible
   over time.** `judge.report/build-report`'s output over a sim corpus
   (fixed seed, churn on) is committed once
   (`test-integration/fixtures/reports/sim-v2-gate-baseline.edn`, this
   repo's existing `--baseline` artifact shape, e.g.
   `test/fixtures/reports/pre-split-baseline.edn`); the test diffs a
   fresh run against it (`judge.report/diff-reports`) every time the
   sibling is present. A changed verdict, an appeared/disappeared
   finding code, or a different file set is a sim-side change surfacing
   here as a reported delta, not a silent no-op the next session would
   have to rediscover from scratch. The baseline is regenerated
   deliberately when sim's output legitimately changes, never hand-
   edited, matching `pre-split-baseline.edn`'s own convention.
5. **The `ehr sim` mount remains DEFERRED (ADR-0012, unchanged).**
   Nothing in this record reopens that question: this loop proves sim is
   consumable and reveals concrete drift without needing a mount, which
   is exactly why a subprocess-only loop was worth building ahead of the
   classpath question resolving. Recorded here so a future session
   doesn't re-litigate whether *this* session's tests imply the mount
   question was implicitly answered — they don't.

**Rejected.** *Waiting for a public sim repo (or a private-dependency
mechanism) before writing any cross-repo test* — this is precisely the
"none of these properties is individually precious... the breakage
would surface in the other repo" risk ADR-0012 named for the mount
seam; the manifest contract test existing and passing (or, as it turns
out, failing informatively) now is strictly more valuable than a
mount-shaped test that only starts existing once the mount does.
*Loosening `ManifestV1_1` to accept sim's current output* — the schema
is the authoritative side of a binding contract test by construction
(ADR-0012 clause 5's own reasoning); bending the authority to match a
drifted implementation is not a passing test, it is deleting the test's
reason to exist. *Failing the sibling-absent case instead of skipping*
— would make every contributor session in this repo without a sim
checkout red for a reason unrelated to anything they touched, and would
make public CI (which has no sibling) permanently red on a suite
ADR-0006/staged-enforcement already treats as reporting-only.
*Asserting a fixed pass/reject distribution for the gate loop* — the
whole point of claim #6's first motion is to observe what the gate
actually says about sim's traffic; hard-coding an expected distribution
(all-pass, or some-rejected) turns an ecological measurement into a
tautology the moment sim's message shape changes for an unrelated
reason.

**Consequence.** `test-integration/ehr_testing_tools/sim_harness.clj`
(plus its own fake-invocation unit test), `sim_manifest_contract_test.clj`,
`sim_gate_loop_test.clj` (with its committed baseline fixture under
`test-integration/fixtures/reports/`), and `sim_intake_test.clj` are new
entry points on the `test-integration` path — `make integration`/
`clojure -X:integration` picks them up automatically (no alias or
Makefile change needed, matching how `contract_pairing_test.clj` and
`baseline_gating_test.clj` were added before them). `.github/workflows/
integration.yml`'s nightly run has no sibling checkout, so these four
tests skip there every night until a future session deliberately adds a
sim checkout step to that workflow — an explicit, not accidental,
consequence of decision 2, noted in that workflow's own comments.
`deps.edn` gains nothing: no `ehr-testing-sim` coordinate, git or
Maven, anywhere in this repo, per decision 1. The manifest contract
test's current red state is carried forward as an open, sim-side fix
(the triage list this session's own report closes with), not silently
tracked to green by editing `ManifestV1_1`.
**Status.** Accepted (author-directed), 2026-07-26.

---
## ADR-0014 — Intake learns optional manifest sidecars, directory-scoped and generator-agnostic
**Context.** ADR-0012's own `[correction]` recorded that `corpus.intake`
never reads a manifest — it catalogs every file by content hash and
sniffed format only, so a `manifest.edn` dropped alongside a corpus is
catalogued as `:unknown` like any other file, and none of its provenance
survives into the catalog. ADR-0013 clause 3 exercised this directly
rather than merely citing the correction: the M3 `sim_intake_test.clj`
trial ran `ehr corpus intake` over a real sim run (messages plus its own
`manifest.edn`) and confirmed the gap as a **finding**, not a bug in
intake as it then stood — `ehr-testing-sim`'s manifest is shaped for
exactly this purpose (`ehr-testing-sim.manifest`'s own docstring: "so
`ehr corpus intake` can ingest a sim run"), and the mismatch is real
impedance, not a misunderstanding on either side. The author has now
ratified closing it: intake should read an optional manifest sidecar and
attach its provenance to the catalog entries it describes.
**Decision.** `corpus.intake` gains sidecar support, directory-scoped:
for a file being catalogued, intake looks for `manifest.edn` in that
file's own immediate parent directory only — never an ancestor directory,
never inherited by a subdirectory that lacks its own copy. This matches
the shape a producer like sim actually emits (one flat directory of
output files plus one `manifest.edn` dropped beside them) without adding
any directory-tracking machinery beyond what `source-files`/`catalog-entry`
already compute per file. When the sidecar parses and validates against
`corpus.manifest/ManifestV1_1`, every catalog entry for a file in that
same directory — `manifest.edn`'s own entry included, deliberately not
special-cased, since a filename branch would exist only to justify itself
— gains `:provenance`: `{:schema-version :stage :generator :seeds}`,
`select-keys` of the validated manifest, `:schema-version` typed as a
plain string rather than the schema's `[:= "1.1"]` literal so a future
schema version doesn't need a matching change here. An absent or invalid
sidecar leaves the catalog byte-identical to today; an invalid one
(malformed EDN, or well-formed EDN that fails `ManifestV1_1`, with
malli's own `explain` as the reason) is recorded as one dedup'd
`:invalid-manifest-sidecar` note per affected directory on the
`IntakeRecord`, never an error and never a rejected intake run —
`corpus.intake`'s own enrich-kind law (adds fields, never alters,
already stated in its module docstring) governs invalid input the same
way it governs absent input.

The implementation is generator-agnostic by construction: nothing in
`corpus.intake` names sim or any other producer. Any pipeline that drops
a `ManifestV1_1`-shaped `manifest.edn` beside its output gets the same
treatment; sim is this feature's first producer, not a parameter to its
design.
**Rejected.** Ancestor-directory inheritance (a subdirectory picking up a
parent directory's sidecar) — no current producer emits nested output
needing it, and it would make provenance attribution ambiguous the moment
two sibling subdirectories both wanted their own manifest; deferred until
a real producer needs it, not spelled out speculatively. Per-file manifest
overrides (a sidecar naming which specific files it describes) — the
one-manifest-per-directory shape already matches every producer this repo
knows about, and a per-file join is unbuilt complexity for a case nobody
has yet. Treating an invalid sidecar as an intake error — would turn a
foreign pipeline's own manifest bug into a blocked intake run for data
that is otherwise perfectly catalogable; the note is exactly enough signal
without that cost.
**Consequence.** `CatalogEntry` gains an optional `:provenance` field and
`IntakeRecord` gains an optional `:notes` field (both additive — a
sidecar-less intake run, which is every run before this session and the
overwhelming majority after it, produces byte-identical catalog entries
and an `IntakeRecord` with no `:notes` key at all). `sim_intake_test.clj`
is updated in the same session: the old assertions documenting the
absence of provenance passthrough are now assertions of its presence,
since this record resolves the finding they were recording. A future
producer needs nothing repo-side to get the same treatment — dropping a
conformant `manifest.edn` beside its output is the entire integration
surface.
**Status.** Accepted (author-directed), 2026-07-26.

---
## ADR-0015 — The gate loop maintains TWO baselines: legacy-floor and full-capability
**Context.** This session reran `sim_gate_loop_test.clj` against the
M3-era baseline ADR-0013 committed and found the first real drift since
that loop was built: 43 messages became 44 for the identical `--seed 42
--patients 20 --churn --emit hl7` invocation, an extra `ADT^A13`
(cancel-discharge) churn event, verdicts and codes otherwise unchanged
(still all `:pass`, zero findings). The cause is not a defect: M4
(Persona) prepends an unconditional per-patient RNG draw ahead of every
other stage, and that one extra draw reshuffles what the SAME seed's RNG
stream hands to every downstream stochastic decision, including churn's
own rolls -- an expected consequence of a new unconditional draw landing
earlier in the sequence, not a sim-side bug. The baseline was
regenerated (reviewed delta first, then regeneration with a provenance
header, per ADR-0013's own discipline) and the loop is green again.

But the deeper finding is scope, not drift: this loop's own default
pathway has *never* carried an `:order` step, a module assignment, or an
outpatient encounter -- sim's own M3 (order/result), M4 (Persona), M5a/M5b
(GMF modules, outpatient visits) milestones all landed and this loop's
own traffic shape never moved, because nothing in it exercises what
those milestones added. A loop that only ever gates `ADT^A01/A02/A03`
plus churn's ADT family cannot become a picture of sim's *current*
capability merely by staying green -- it was never wired to see the rest.
sim's own README names `ehr-testing-tools` gating its output as the
reason sim's own 850+ assertions aren't graded on their own homework; a
gate loop that structurally cannot see three milestones' worth of new
message types undercuts that claim quietly, the kind of gap that doesn't
announce itself in red text the way a real regression does.

**Decision.** The cross-repo consumer loop now maintains TWO committed
baselines, not one, each with its own test and its own scope statement:

1. **`sim-v2-gate-baseline.edn` (LEGACY-FLOOR)** --
   `sim_gate_loop_test.clj`, unchanged in shape (plain default pathway,
   `--seed 42 --patients 20 --churn`), regenerated this session per the
   delta above. Its own header now says explicitly what it is: a floor
   proving the base-structural v2 judge still runs clean over sim's
   *plainest* traffic, not a picture of sim's breadth. Kept, not
   retired -- it is cheap (20 patients, ~44 messages), it is the
   longest-running signal this loop has, and ADR-0013's own
   baseline-delta discipline already treats a re-verified floor as
   worth keeping even after a richer measurement exists alongside it
   (the same reason `pre-split-baseline.edn` was never deleted when
   newer baselines joined it).
2. **`sim-v2-full-capability-baseline.edn` (the reference picture)** --
   a NEW test, `sim_full_capability_gate_test.clj`, running a NEW
   committed config fixture
   (`test-integration/fixtures/sim-configs/full-capability.edn`) through
   the SAME `--config` passthrough M4 Task 0 wired: an order-bearing CBC
   pathway cohort (ordinals 0..39) and a disjoint module-only sinusitis
   cohort (ordinals 40..59, an EMPTY authored pathway plus
   `:module-assignment` -- the two cohorts cannot share a patient
   population under this project's own single-encounter-horizon
   invariant, confirmed empirically this session: a combined
   pathway+module attempt on the SAME patients made every run
   `:self-check-failed`, since a module's own compiled encounter is
   prepended ahead of a patient's authored pathway and a second
   encounter-opening step for an already-non-`:new` patient is illegal
   by construction), `--churn` on with an elevated `:churn-profile`
   (this file's own header documents why: `churn/sample-profile`'s
   default rates produced only 2 of the 5 churn trigger codes across 40
   CBC-cohort patients in this session's own trial runs). At `--seed 42
   --patients 60`, this corpus contains `ADT^A01/A02/A03/A04` (outpatient,
   sinusitis module), `ORM^O01`/`ORU^R01` (CBC order/result), and
   `ADT^A11/A12/A13/A17/A40` (the full churn trigger family) -- 210
   messages, 11 distinct message types, none of which the legacy-floor
   loop has ever produced except A01/A02/A03. Captured baseline: all 210
   `:pass`, zero findings -- this baseline's own header carries the full
   verdict-by-message-type table, labeled explicitly as M6's own
   reference picture: the current, dated, honestly-labeled statement of
   what the v2 judge's base-structural tier makes of sim's *current*
   breadth, for sim's own M6 (FHIR emitter, emitter-coherence work) to be
   measured against later.
3. **Both baselines stay.** This is a policy extension of ADR-0013's own
   baseline-delta discipline (decision 4 there), not a supersession of
   it: ADR-0013 established ONE baseline for the loop that existed at
   the time; this record recognizes that a single baseline's *scope* can
   go stale even while its *verdicts* stay green, and that the fix is
   not to keep widening one baseline's own corpus (which would silently
   change what "legacy-floor" means every time sim gains a milestone)
   but to keep the narrow floor AND add a second baseline whose own job
   is to keep pace with sim's breadth. Future sim milestones (site
   profiles in gate traffic, a dialect-variant loop, FHIR once M6 lands)
   are candidate THIRD/FOURTH baselines under this same policy, each
   scoped and named for what it actually covers -- not folded into
   whichever baseline happens to exist already.

**Rejected.** *Widening the legacy-floor baseline's own corpus in place*
(adding `:pathways`/`:modules` to `sim_gate_loop_test.clj` itself) -- would
destroy the one property that makes a "floor" useful: a fixed, minimal,
long-unchanged reference point. Every future milestone would then face a
choice between silently growing that same corpus again (scope creep with
no name for what changed) or leaving it stale on purpose (this session's
own finding, repeating). *Retiring the legacy-floor loop once the
full-capability loop exists* -- the two measure different things (a
minimal floor vs. a breadth picture); retiring the floor would lose the
cheapest, fastest signal this loop has for a plain-pathway regression,
for no real savings (both loops together still run in seconds).
*Combining the CBC pathway and the sinusitis module on the SAME patient
cohort*, to make the fixture read as one simpler population -- empirically
impossible under this project's own single-encounter-horizon invariant
(see Decision 2's own parenthetical); two disjoint cohorts in one corpus
is the only shape that actually exercises both without a self-check
failure. *A site-profile variant in this same fixture* -- deliberately
deferred (`full-capability.edn`'s own header): the full-capability
baseline is already a wide surface; adding a THIRD dimension (dialect) in
the same commit would conflate "sim's breadth grew" with "the gate now
sees a different accent" in one diff. A candidate follow-on, not this
session's scope.

**Consequence.** `test-integration/fixtures/sim-configs/full-capability.edn`
(new), `test-integration/ehr_testing_tools/sim_full_capability_gate_test.clj`
(new), `test-integration/fixtures/reports/sim-v2-full-capability-baseline.edn`
(new, full provenance header), `test-integration/fixtures/reports/
sim-v2-gate-baseline.edn` (regenerated in place, provenance header
updated to record both the M3-to-current delta and this record's own
legacy-floor framing), `ehr-testing-tools.sim-harness/cli-args` (extended
with a `:config` passthrough, resolved to an absolute path before it
reaches the subprocess's argv -- the sibling's own working directory,
not this repo's root, is where a relative path would otherwise resolve),
and a new unit test on `sim_harness_test.clj` covering that resolution.
Nothing in `../ehr-testing-sim` changes; `deps.edn` gains nothing;
`make integration`/`clojure -X:integration` picks up the new test
automatically, matching how every other `test-integration` entry point
has been added since ADR-0013.
**Status.** Accepted (author-directed), 2026-07-27.

---
## ADR-0016 — Verdict cache + verification tiers: T2 is change-triggered and nightly, not per-commit ritual

**Context.** `make integration` costs 19m11s locally and 10m29s in CI
cold-cache (CI run 30175880198), dominated by per-file
`validator_cli.jar` subprocess launches — JVM startup alone runs
roughly 1–2 minutes per invocation, and a single `gate fhir` strip was
recorded at 99s. The suite's *placement* was already right
(hermeticity is a path split, `test-integration/` vs `test/`; T2 runs
nightly plus `workflow_dispatch`, blocking no merge) but session
*discipline* still forced its cost onto every commit:
`.agents/plans/judge-gate-refactor.md`'s Phase 1 stated "Verify after
each commit: full test suite + both integration suites..." — a rule
sized for that phase's own renaming sweep, generalized since into a
per-session ritual that pays T2's ~19-minute cost regardless of
whether a given commit touches anything T2 uniquely exercises. Nothing
in the suite's own design was wrong; the ritual wrapped around it was
sized for the wrong grain.

**Decision.**

1. **Three verification tiers, named once, referenced everywhere.**
   - **T0 — fast gates** (unchanged): `make test` + `lint-pipeline` +
     `lint-deps` + `quickstart-fresh`. Pre-push-hook-enforced
     (`.githooks/pre-push`) and owed after every commit.
   - **T1 — integration-smoke** (new, `make integration-smoke`,
     `:integration-smoke` alias, target under 2 minutes measured
     *warm*): one real `validator_cli.jar` pairing (one known-clean
     Synthea file, one mutant with a known conviction at a fixed
     locator) asserting pairing polarity only — never an aggregate
     verdict, since EXP-C5/contract-pairing already established a real
     US-Core-profiled file always carries hundreds of incidental
     profile-driven findings — plus one `sim-harness` run at a fixed
     seed asserting its emitted manifest validates against
     `corpus.manifest/ManifestV1_1`, skip-when-absent like every other
     sim-consuming suite. Owed at session boundaries and on any
     integration-adjacent commit (trigger list below). The two `gate
     fhir` calls this tier makes share the verdict cache (decision 3)
     with every other invocation this session makes: the first T1 run
     in a session pays for two real subprocess launches; every
     subsequent T1 run against the same fixed corpus is a cache hit on
     both files and finishes near-instantly — which is *why* the
     2-minute target is stated warm, not cold.
   - **T2 — full integration** (`make integration`, unchanged in
     content): nightly CI (`.github/workflows/integration.yml`) plus
     release gates plus any in-session commit whose changed paths
     intersect the trigger list below.
2. **T2's in-session trigger is change-aware, stated as text.** T2 is
   owed in-session only when the changed paths intersect:
   `src/ehr_testing_tools/judge/fhir.clj`,
   `src/ehr_testing_tools/judge/v2*`,
   `src/ehr_testing_tools/invocation.clj`,
   `src/ehr_testing_tools/artifact.clj`,
   `src/ehr_testing_tools/corpus/generate.clj`, anything under
   `test-integration/`, the `:integration` alias in `deps.edn`, or
   `.github/workflows/`. Everything else owes T0 per commit and T1 at
   session close; nightly T2 remains the backstop regardless. This
   text is the authority (`AGENTS.md`'s own "Verification tiers"
   section states it too, for discoverability, but does not supersede
   this record if the two ever drift — this ADR is amended first). A
   `bin/needs-integration` helper was considered (diff changed paths
   against this list, exit 0/1) but judged unnecessary machinery for a
   six-entry list a human or an agent can read directly; not added.
3. **Verdict cache is content-addressed and inert on miss**
   (`ehr-testing-tools.judge.verdict-cache`, wired at
   `judge.fhir/gate-file`). Key = SHA-256 of {the input file's own
   content hash, the resolved validator artifact's identity
   (name+version+sha256 — the sha256 specifically so a same-named,
   same-versioned but differently-published jar can never alias, per
   `digest.clj`'s own claim-vs-fact distinction, ADR-0005), the
   resolved IG/profile artifacts' identities (same shape), the
   validator's own argv shape (`-version`, `-tx`, `-ig` flags — *not*
   the per-run `-output=`/input scratch paths, which don't change what
   the validator does), and `judge.fhir/verdict-mapping-version`
   (`interpret`'s own classification-table version — a mapping-version
   bump must invalidate every cached verdict, since the same raw
   OperationOutcome now classifies differently)}. Value = the judge's
   own `interpret` output (`{:verdict :findings [:cause]}`), EDN, at
   `target/verdict-cache/<key>.edn` (gitignored, `target/` already is).
   A hit skips `execute` entirely — the validator subprocess never
   runs; a miss runs exactly as before and stores its result under
   that key. **Determinism assumption, stated because nothing else in
   this repo states it:** the pinned `validator_cli.jar`, given
   byte-identical input content, an identical argv shape, and
   identical IG/profile artifacts, produces the identical
   OperationOutcome every time. Nothing in this session falsified that
   assumption (contract-pairing's own polarity assertions passed
   unchanged with caching wired in), but it is an assumption, not a
   proof — a determinism regression in some future validator release
   would silently serve a stale verdict rather than erroring, which is
   exactly what the escape hatches below are for. **Escape hatches:**
   delete `target/verdict-cache/` (it is pure build scratch, safe to
   remove any time), or disable caching for one invocation
   (`ehr gate fhir --no-verdict-cache`, library `:verdict-cache?
   false`) when the assumption is ever suspect. `judge.v2` (HAPI
   HL7v2) needs no cache and gets none: it runs HAPI in-process
   (pattern nursery #1's two-step engine discipline applies without a
   subprocess, per that namespace's own docstring) — there is no
   subprocess to skip, so a cache there would add a stale-data risk
   for zero wall-clock benefit.
4. **Batch validator invocation: probed, not assumed.** Whether the
   pinned `validator_cli.jar` accepts multiple files or a directory
   per invocation, with per-file-attributable findings, is an
   empirical question about a third-party tool — probed directly
   (facts-register row, `notes/facts-register.md`) rather than assumed
   either way. Adopted in `contract_pairing_test.clj` only if
   attribution came back exact; the verdict cache (decision 3) is this
   session's primary cost reduction regardless of the probe's outcome.

**Rejected.** *Widening `judge.report`'s or `judge.finding`'s own
schema to carry cache provenance* — the cache is an invocation-layer
optimization (whether the subprocess ran), not a judgment-layer fact;
folding it into the finding/report schema would conflate "how this
verdict was computed" with "what this verdict says," the same
layering mistake ADR-0009's judge/gate factorization already argues
against elsewhere. *A time-based (TTL) cache instead of content-
addressed* — a TTL cache can serve a stale verdict for unchanged
inputs past its expiry and can also evict a still-valid one before its
expiry; content-addressing is exact by construction (same key = same
inputs = same computation, given the stated determinism assumption)
and needs no clock. *Tag-filtering T1 out of the existing
`:integration` alias instead of a dedicated namespace* — this repo's
own hermeticity precedent (`test/` vs `test-integration/`,
`AGENTS.md`) is a path/namespace split specifically because
`cognitect.test-runner.api/test`'s `:excludes` is not honored
uniformly across every runner this repo uses (`make coverage`'s
cloverage runner is the documented example); a dedicated namespace
(`smoke_test.clj`, selected via `:nses`) sidesteps that class of bug
entirely rather than re-risking it for a new tier. *A shorter T2
cadence (e.g. every-other-commit) instead of change-triggered* —
would still pay T2's cost on commits that can't possibly regress
anything it uniquely covers (a doc fix, an ADR append), the same
waste this record exists to remove, merely at a lower average rate
rather than eliminated at the source.

**Consequence.** `.agents/plans/judge-gate-refactor.md`'s Phase 1
"Verify after each commit" line is amended in place with a dated note
pointing here — that line was sized for Phase 1's own renaming sweep,
not a standing rule; it is not itself in error, but this record now
governs going forward. `AGENTS.md` gains a "Verification tiers"
section restating decisions 1–2 for discoverability. `.githooks/pre-
push` is unchanged (T0 was already correct there). CI is unchanged:
`.github/workflows/ci.yml` already runs only T0-tier work per push;
`.github/workflows/integration.yml` already runs T2 nightly plus
`workflow_dispatch` — this record documents why that placement is
right, it does not move anything. Measured wall times (T0/T1/T2
cold/T2 warm-cache) on the author's own machine are recorded in this
session's own report, not restated here — a wall-clock number is a
measurement, not an architectural decision, and belongs beside the
session that took it.

**Cites.** ADR-0005 (claim-vs-fact, the reasoning behind keying on
sha256 rather than name+version alone), ADR-0006 (staged enforcement —
this record is staged enforcement's own logical continuation, one
level more granular: not just fast-vs-slow gates, but which commits
owe the slow one), ADR-0013 (sim-harness subprocess convention, reused
unchanged by T1's own sim-harness half).
**Status.** Accepted (author-directed), 2026-07-27.

---
## ADR-0017 — Formal Source and Sink types: generator/reader unification, framing as an axis, maps-canonical/URLs-surface, sink composability law
**Context.** Corpus input and output in this repo are ad-hoc today:
`corpus.generate` (`src/ehr_testing_tools/corpus/generate.clj`) knows
Synthea specifically; `corpus.intake` (`corpus/intake.clj`) knows
directories specifically; the sim consumer loop's subprocess seam
(`test-integration/ehr_testing_tools/sim_harness.clj`, ADR-0013) lives
only in harness code, never in `src/`; sinks are bare output paths with
no declared format, framing, or write discipline. `docs/notation.md`'s
own equation notation already classifies resources as source-like
(no producing stage) versus sink-like (no consuming stage) at the
diagram level; this record promotes that classification to a runtime
type. `docs/source-sink-design.md` (landed alongside this record) is
the full design; this ADR is its reasoning-of-record.
**Decision.** Source and Sink become formal, registry-open types.
Full detail in `docs/source-sink-design.md`; the load-bearing points:

1. **Generator/reader unification via `dir` plus manifest.** A
   generator source (`synthea`, `sim`, registrable `simhospital`)
   executes its engine into a fresh directory with a `ManifestV1_1`
   sidecar, then *is* a `dir` source over that directory. A streaming
   reader (`stdin`, anything piped from `nc`) spools to a directory
   first — one file per framed item, plus a capture manifest — then is
   a `dir` source too. Every corpus is therefore replayable: network
   and pipe inputs exist on disk before anything judges them: `corpus.
   intake` stays the single ingestion door, with no per-source
   adapters, for every source alike.
2. **Framing is an axis, independent of format.** Sources and sinks
   carry `:framing` (`:file-per-item \| :er7-multi \| :ndjson \|
   :bundle-entries \| :mllp`) — file does not imply item, as the
   vendored 1,013-message-in-one-file SimHospital corpus (ADR-0011)
   already demonstrates. MLLP support is a framing *codec* only (pure
   bytes⇄messages functions); transport is `nc`'s job, and no socket
   code enters this repo.
3. **Sinks emit manifest sidecars; every sink's output is a valid
   source.** Stated as a law, not a convenience: `dir`/`file`/`stdout`
   (optionally MLLP-framed)/`blaze` sinks all declare `:format` and
   `:framing`/protocol explicitly (no inference, ever, on the write
   side), default to fail-if-exists, and report network failures as
   Result values, never a thrown exception. The composability law gets
   a property test in SS-4: a sink's own output re-intakes with
   lineage intact.
4. **Maps canonical, URLs surface.** The runtime type is a Clojure map
   (`:kind`/`:format`/`:framing`/kind-specific fields, open `:kind` set
   via registry); the CLI/wire format is a compact URL string that
   parses to exactly one canonical map (`parse ∘ print = identity`,
   round-trip tested). EDN config files use maps directly, so nested
   generator params never get forced through query-string encoding.
   Format inference (extension, then content sniff — `corpus.intake`'s
   existing order) applies only where `:format` is absent on a
   *source*, and the fact of inference is recorded in the catalog
   entry.
5. **Vocabulary: the `:origin` rename.** The runtime `Source`/`Sink`
   types deliberately rhyme with `docs/notation.md`'s existing
   source/sink classification of equation resources. `CatalogEntry`'s
   existing `:source :string` field (a provenance label,
   `corpus/intake.clj:60`) collides with the new type and is renamed
   `:origin`, in the same genus of fix as the judge/gate `:policy` →
   `:disposition` rename (ADR-0009) — a criterion-layer or data-layer
   fact should not wear a word a formal type now claims. The rename
   ships in a build session (SS-1 or its own micro-session, open as
   D-c), not this capture session, which touches no `src/` code.
6. **Explicit non-goals.** Sim stays subprocess-only (ADR-0013,
   unchanged); the intended future evolution — sim as a pinned
   artifact-registry entry once published, dissolving the
   sibling-checkout requirement — is recorded, not built. No
   SimHospital implementation ships; the generator registry slot is
   the entire accommodation. Network sources may carry real
   (non-synthetic) data, tagged `:foreign` with provenance; synthetic-
   only guarantees apply only to this repo's own generators. Spooling
   carries a size cap with an explicit override. Format ≠ version ≠
   profile: sources declare format only, and FHIR version/profile stay
   the judge tier's concern.

Three questions are deliberately left open, not resolved by this
record: **D-a** (URL scheme spellings — `dir:` vs. `file:` with
trailing slash, invented schemes for generators), **D-b** (whether the
`blaze` sink lands before or after the IG-pinning blocker clears —
they interact on what profile a written resource claims), **D-c**
(which build session ships the `:origin` rename). `docs/source-sink-
design.md`'s Decision Register carries all three as open; this ADR
records that they are open by design, not an oversight.
**Rejected.** *URLs as the canonical form, maps as a parsed
projection* — inverts D4's own reasoning: nested generator params
(module sets, patient counts) would have to round-trip through
query-string encoding on every access, not just at the CLI boundary,
for no benefit over treating the map as canonical and the URL as one
surface onto it. *Per-source adapters into the catalog* (one
Intake-shaped function per source kind) — exactly the N×M seam
proliferation the generator/reader unification (decision 1) exists to
avoid; a single `dir`-shaped ingestion door, fed by any source that has
already normalized to that shape, is the whole point. *Sockets in this
repo* — `nc` already exists, is already trusted infrastructure for
MLLP transport in HL7 tooling generally, and building socket code here
would duplicate a solved problem while adding an attack surface this
repo has no reason to own; the framing *codec* (pure functions) is the
genuinely new, in-scope work, not the wire. *A real SimHospital
implementation* — no consuming use case exists yet to justify the
build cost; the registry slot alone (decision 6) keeps the door open
at the cost of one entry, when a real use case shows up.
**Consequence.** `docs/source-sink-design.md` is the design record this
ADR accepts; `.agents/plans/corpus-foundations.md` gains staged build-
session rows SS-1..SS-5, each naming its own test-first obligation and
verification tier (ADR-0016). No `src/` namespace changes with this
record — `corpus.generate`, `corpus.intake`, and `sim-harness` are
unchanged in behavior until a build session lands. `CatalogEntry`'s
`:source` field remains named `:source` until the rename ships (D-c);
this ADR is the forward notice that it will change, not the change
itself.
**Status.** Accepted (author-directed), 2026-07-27.
