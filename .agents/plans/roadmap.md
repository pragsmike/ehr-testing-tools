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

## M2 — Churn family

Lands **InjectChurn** (currently `;; NEXT` in the theory, `:planned`):
cancel-\*, \*-in-error, bed-swap, and merge step types, as the IR→IR
transform the theory already names. Brings `churn-profile` in as real
config rather than a named-but-unbuilt resource.

Co-landing invariants: each new step type's own catalog entries
(e.g. a cancelled event must reference an event it cancels; a merge
must reference MRNs that both exist; a bed-swap preserves the
occupancy invariants M1 established rather than bypassing them). The
IR-endomorphism and clinical-steps-preserved laws already stated on
`:churn` in the EDN become property tests here, not new claims.

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
