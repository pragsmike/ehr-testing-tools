# Roadmap

**Status: accepted (2026-07-26).** ADR-0003 deferred writing a plan
"until first real use" of `.agents/plans/`; this is that first use.
Milestone order, scope, and the deliberate exclusions below are
author-ratified. Each milestone names the
[`sim-theory.edn`](../../docs/sim-theory.edn) stage(s) it advances and
the invariants that must co-land with it (`AGENTS.md`'s co-landing
convention: every new engine step type ships with its `check.clj`
invariants in the same change).

## M1 — Facility + providers models, transfer step, occupancy projection

Advances **Execute** (`:built`, growing its step vocabulary) via its
new `provider-pool` catalytic, plus the transfer step type.
**A02 emission is IN M1** — the co-landing convention (`AGENTS.md`)
extends to the emitter's `message-type-registry`, not just
`check.clj`: a step type without a registered message type produces
traffic that's invisible to every consumer downstream of `EmitHL7`,
which is exactly the kind of silent gap the co-landing rule exists to
prevent. The registry entry and derivability-law test coverage for
A02 land in the same change as the `:transfer` step itself, not in a
follow-on.
[`docs/operational-models.md`](../../docs/operational-models.md),
reviewed this session, is this milestone's spec — nothing here
redecides what that document already decided.

Co-landing invariants: no bed holds two patients; an admitted patient
occupies exactly one bed; a transfer's from-location matches the
patient's current state; occupancy never exceeds ward capacity;
surge placement only when earlier ladder rungs are exhausted (unless
`:forced true`). Plus the occupancy board's own consistency law as a
property test: board ≡ fold over patient locations.

## M2a — Engine prep: identity, participants, and the time model — **landed**

(Identity/participants per ADR-0010 and the time model per ADR-0011,
including the warm-up mark, are implemented and test-first — 73 tests
/ 156 assertions green; the seeded arrival process is the one sketched
item not built, explicitly a stretch M2b doesn't depend on.)

Split out of the original single M2 milestone (this session) because
its two decisions — [ADR-0010](../../notes/ADRs.md#adr-0010) (patient
identity + the `:participants` event shape) and
[ADR-0011](../../notes/ADRs.md#adr-0011) (seconds granularity, a
pinned UTC offset, a seeded arrival process, a warm-up window) — are
both **engine refactors that M2b's actual churn step types depend on**,
not churn content themselves. Landing them first means M2b's merge and
bed-swap steps are written directly against `:patient-id`/
`:participants` and the new clock, rather than against `:mrn` and
minutes with a mid-milestone migration. Both refactors are seam-able:
each can land, be tested, and be reviewed independently of the other,
and neither blocks on M2b existing yet.

**Seed perturbation, expected twice over, both already covered by
existing policy.** M1 already perturbed pinned-seed output once
(ADR-0009, the allocation-ladder and provider-sampling draws). M2a's
time-granularity change (ADR-0011 decision 1, minutes → seconds) shifts
every timestamp-consuming draw again — a second, equally expected
instance of exactly the pattern ADR-0009 already names and accepts:
same config + seed stays byte-identical *within* a generator version,
and each milestone that grows the engine's stochastic surface is
expected to regenerate pinned fixtures, not treated as a regression to
chase down. `:patient-id` generation (ADR-0010) is a further, third
draw-order change from the same milestone; the same policy covers it.

Co-landing invariants: the fold-key/queue-key rename (`:mrn` →
`:patient-id`, `docs/patient-state-model.md`'s accumulator gaining
`{:mrns :active-mrn}`) touches every existing `evolve`/`decide` method
and test helper, so the determinism and invariant-catalog property
tests must stay green across the rename, not just for new step types;
`:participants` becomes a real field on every event (single-element
vector for today's step types) with its own schema round-trip test.

## M2b — Churn family — **landed**

Lands **InjectChurn** (`:built` in the theory; `;; NEXT` moved to
`Execute`'s own further growth, M3): cancel-admit/cancel-transfer/
cancel-discharge (A11/A12/A13), transfer-in-error (A02+A12, in-error
marked), bed-swap (A17, genuinely two-participant), and merge (A40,
the identity payoff) — 134 tests / 318 assertions green, coverage
94.76%/97.25% (up from the M2a baseline 91.72%/94.69%). `churn-profile`
is real config (`ehr-testing-sim.churn/ChurnProfile`) rather than a
named-but-unbuilt resource, wired into `sim run` via `--churn` or an
explicit `:churn-profile`. Task 0's two ratified items landed alongside:
the durations rule (one line, `docs/patient-state-model.md`) and
result-not-throw capacity exhaustion (`ehr-testing-sim.facility/allocate`
no longer throws; `run-command` surfaces `:error :capacity-exhausted`),
plus an ED-diversion/waiting-room-boarding stub entry in
`docs/clinical-realities.md`. `docs/patient-state-model.md`'s conditional
validity rows (status × event-class × attribute-conditions, added M2a)
name two further candidate step families for a future milestone, as
**stretch/candidate steps, not landed this session**: leave-of-absence
(A21/A22) and observation/inpatient class-flip (A06/A07), both from
`docs/clinical-realities.md`'s stub catalog.

Co-landing invariants, landed: `cancel-references-existing-uncancelled-event`
(a cancelled event must reference an event it cancels, of the right
type, not already cancelled), `bed-swap-both-admitted-before-swap`,
`merge-survivor-absorbs-merged-mrns`, `no-events-after-merged-terminal`
— all expressed as `:participants` cross-participant coherence checks
per ADR-0010. The IR-endomorphism, clinical-steps-preserved, and
zero-probability-identity laws stated on `:churn` in the EDN are now
property tests (`churn-test`), not just claims. One design decision
surfaced only by property-testing InjectChurn against the full
invariant catalog, recorded here rather than in an ADR since it's an
internal robustness fix, not a wire/contract change: a churn-inserted
step that is STATICALLY legal (per the applicability oracle) can still
be REJECTED at execution time by live world state InjectChurn has no
visibility into (e.g. a bed a cancel-discharge would reinstate into was
reclaimed by someone else's admission in the meantime) — such a
rejection is a no-op for that one step, not a run-halting condition,
and InjectChurn's own state model treats `:cancel-discharge`
conservatively (never assumes it succeeds) for exactly this reason.

## M3 — Order profiles + order/result steps

Advances **Execute** via its new `order-profiles` catalytic (added
this session, target 3 — hashed US-units config): order and result
step types, and ORM/ORU emission in `EmitHL7`. This is the milestone
that repairs the capture gap named in this session's theory
sync — Simulated Hospital's order profiles and the ORM/ORU cycle were
discussed from the project's first session but, until now, lived in
no planning artifact.

Co-landing invariants: results reference orders that exist and
precede them in time; order/result message types register in
`message-type-registry` the same way ADT types already do.

## M4 — Persona + demographics tables; payer sampling; PID/IN1 enrichment

Lands **Persona** (`:planned`) for real: demographics sampling from
vendored, hashed tables (`demographics-tables`, target 3), plus the
`payer-pool` catalytic this session recorded as a comment-only forward
reference on `:persona` — this is the milestone that turns that
comment into a real `:catalytic` wire. PID gains demographic fields;
IN1 lands as a segment for the first time, carrying the sampled payer
(`docs/operational-models.md`'s payers model).

Co-landing invariants: sampled demographics and payer are internally
consistent with any age-linked rule (e.g. Medicare weighting at 65+
is only checkable once age is a real field); schema round-trip tests
for the new `persona` resource type.

## M5 — GMF interpreter + module vendoring decision + CompileTrajectory

Lands **RunModules** and **CompileTrajectory** (both `:planned`),
which means finally resolving `sim-theory.md`'s open question #1: is
`gmf-module-set` a pinned lockfile (target 1) or vendored-and-hashed
(target 3)? This is ADR-0003's own named trigger ("adopt
[a pinned-artifact/vendoring pattern] once Synthea modules... actually
land in this repo") — this milestone is that landing, and the
decision gets its own ADR when it's made, per that trigger.

Co-landing invariants: code-passthrough (every coded concept in a
trajectory event is carried verbatim from its source module, never
invented or translated); glass-box traceability (every trajectory
event cites the module id and state name that produced it);
clinical-content-preserving compilation (every trajectory event maps
to at least one IR step, none dropped or reordered against clinical
causality).

## M6 — EmitState (FHIR snapshots first); emitter-coherence property

Lands **EmitState** (`:planned`): state-document rendering from
`state-history`, FHIR resources before CDA. This is also the natural
point to resolve `sim-theory.md`'s open question #3 — whether
`state-history` is primitive or derived from `ground-truth-log` —
since EmitState existing is exactly the precondition that question's
own deferral names.

Co-landing invariants: snapshot-at-instant (a state-document is a
pure function of `state-history` at a queried instant, no access to
the log, engine, or RNG); the cross-emitter **emitter-coherence**
property — replaying `hl7v2-stream` reconstructs `state-history`, and
a FHIR snapshot at instant *t* agrees with the state implied by
messages up to *t* — becomes a real property test for the first time
once there are two emitters to check against each other.

## Later / triggers

Not sequenced, because each is gated on a condition rather than on
the milestone before it:

- **Calibrate** (`:planned`, feedback stage) — waits on a real
  `sim-corpus` and a site's `feed-statistics` to calibrate against;
  premature before Package and a first external consumer exist.
- **CI + integration validation** (independent-parser round-tripping
  via NIST/HAPI, per `docs/third-party-sources.md` Tier 2) — triggers
  once this repo has a public GitHub remote (ADR-0003's existing
  trigger) or another CI-capable remote.
- **Packs demotion** — `pack-push` stays the active session-end
  ceremony (ADR-0006) until this repo's GitHub remote goes *public*;
  demoting it to dormant, the way `ehr-testing-tools` did at its own
  ADR-0008, is recorded as its own ADR when that happens, not folded
  into this roadmap.
- **Site profiles** (`docs/site-profiles.md`, this session) —
  **proposed**, for author review rather than decided placement:
  code-table overrides, naming idioms beyond `:surge-format`, and
  Z-segment templates, as their own milestone landing **after M3**.
  Reasoned there rather than earlier because Z-segment templates are
  thin content without order/result data to bind them to — a
  site-profile milestone landing before M3's `order-profiles` catalytic
  exists would have little beyond code-table overrides to actually
  exercise.

## Consumer plan: sim doesn't validate itself in a vacuum

This roadmap's milestones describe what this repo builds; two items
describe how this repo's output gets exercised by consumers outside
it, named here so they aren't lost between repos.

- **Tools as first consumer.** An integration-tree item belongs **in
  `ehr-testing-tools`**, not here (ADR-0001's dependency direction: sim
  never depends on tools, but tools already may depend on sim): `ehr
  gate` judging a sim-generated corpus end to end, as a real exercise
  of tools' Gate machinery against this project's own traffic rather
  than only hand-crafted fixtures. This can share its test harness with
  the manifest contract test [`ADR-0001`](../../notes/ADRs.md#adr-0001)
  already assigns to tools' integration tree (the binding
  cross-repo contract tests live where both codebases share a
  classpath). **Noted here, built there** — this roadmap does not
  schedule tools' own work, only records the dependency so a future
  session in either repo knows the item exists.
- **Blaze as M6's ecological target.** `samply/blaze` (a Clojure FHIR
  server) is named as the natural first real-world consumer for M6's
  **EmitState** output (`docs/sim-theory.edn`) — a same-language FHIR
  server this project's state-documents can be loaded into and queried
  against, giving the emitter-coherence property test a genuine
  external system to check against rather than only this repo's own
  parsing round-trip. This is a target for M6's own validation work,
  not a dependency this repo takes on; recorded here so M6 doesn't have
  to rediscover the natural fit from scratch.

## The adversarial-traffic exclusion

**This simulator generates coherent truth; it deliberately never
generates delivery incoherence.** Out-of-order arrival, dropped
messages, malformed segments, and duplicate delivery are real
phenomena a downstream interface must survive — but they are failures
of a *transport* or *delivery* layer acting on a coherent stream, not
facts about the hospital the stream describes. This project's own laws
(ADR-0002's ground-truth-log primacy, the emitter-coherence law,
`InjectChurn`'s own IR-endomorphism and clinical-steps-preserved laws)
forbid the engine from ever emitting a stream that contradicts itself
— which means this engine structurally **cannot** be the place
out-of-order or dropped-message traffic comes from, on purpose, by the
same laws that make its output trustworthy in the first place.

That is exactly why this territory belongs to `ehr-testing-tools`'
`corpus mutate` instead, operating on a sim-generated corpus **after**
this project has produced it: sequence-reorder, segment-mangle, and
duplicate-delivery operators, each with recorded lineage back to the
coherent corpus it mutated. This isn't a gap sim leaves for tools to
fill reluctantly — it's the intended division of labor, now written
down as a reason rather than left to be inferred: sim's job is
producing ground truth a mutation operator can trust was coherent
*before* mutation, precisely because sim itself never introduces
incoherence as a side effect of its own generation.

## Deliberate exclusions

Recorded so they read as decisions, not gaps someone might otherwise
try to fill in:

- **Lifelong birth-to-death records.** This simulator's scope is
  hospital-operations traffic over an *encounter horizon* — one
  admission through its discharge and immediate churn — not a
  patient's full longitudinal history. Synthea already serves the
  longitudinal need well (that's precisely why it's mined as a
  tier-1 source rather than reimplemented); duplicating its scope
  here would be redundant, not additive.
- **CPT codes.** AMA-licensed; excluded by the standing constraint
  (`docs/problem-statement.md` §3, `AGENTS.md` Constraints), not a
  future milestone.
- **Delivery/transport** (file, stream, MLLP pacing). Below this
  theory's level of description — `sim-theory.md`'s own open question
  #5 already names this as deliberately absent unless paced emission
  ever acquires laws of its own.
