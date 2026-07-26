# Event sourcing, and why this simulator's architecture is not a stylistic choice

This is the explainer for a question worth answering plainly, once,
rather than re-deriving from ADR cross-references every time it comes
up: why does this simulator's engine work the way it does, and why
should a tester reading its output trust that shape? The short answer
is that this project didn't choose event sourcing as an implementation
fashion — it recognized that the domain it models (hospital ADT
traffic) has always been an event-sourced system in every way but
name, and built the one piece that was actually missing: making the log
a first-class, queryable thing instead of a fact every downstream
system had to reconstruct for itself, forty years running.

## What event sourcing is, and how this simulator does it

Event sourcing is the discipline of treating a log of immutable facts
as the one authoritative record, and deriving every other view of the
system — current state, historical snapshots, aggregates — by
replaying that log through a pure function. The alternative, ordinary
mutable-state design, keeps "current state" as the primary record and
treats history (if it's kept at all) as a derivative, usually lossy,
side effect of mutation. Event sourcing inverts that: the log is
primitive, state is derived, and "what happened" and "what is true
now" can never silently disagree, because there is only one function
that produces the second from the first.

This simulator's engine (`ehr-testing-sim.engine`) implements exactly
that inversion, decided at [ADR-0008](../notes/ADRs.md#adr-0008) and
specified in full at
[`docs/patient-state-model.md`](patient-state-model.md). Three pieces
make it concrete:

- **The log is the primitive.** `ground-truth-log` — a flat,
  time-ordered sequence of events — is the only place a fact about a
  run originates. Nothing else the engine produces is authoritative;
  everything else is computed from it.
- **`decide`/`evolve` is the split that makes the inversion mechanical
  rather than aspirational.** `decide (rng, t, world, patient, step) ->
  {:events :advance}` consults the current fold of all patient state
  (read-only) and the run's single seeded RNG to decide what happens,
  and returns facts — it never mutates anything and never returns a
  new state. `evolve (state, event) -> state'` is the *only* function
  that ever produces a new patient state, and it is pure, total, and
  ignorant of everything except the one event it's folding. There is
  no code path in this engine that assigns into a patient's state
  directly; the only way state changes is by an event passing through
  `evolve`.
- **State is a fold, and so is every projection built on it.** A
  patient's current state, at any point in a run, is
  `(reduce evolve initial-state event-subsequence)` — a computed
  value, not a second thing the engine has to remember to keep in
  sync. The occupancy board (`docs/operational-models.md`'s "what's in
  RENAL-04 right now") is the same idea one layer up: a **projection**
  over the set of patient states, proven consistent by its own law
  (**board ≡ fold over patient locations**, a property test, not a
  code-review claim). The authority hierarchy this project actually
  has is three deep — **log → patient states → occupancy board** —
  each stage a projection of the one before it, each with its own
  provable consistency law, none of them a second place a fact can
  originate.

The payoff of this inversion is not abstract: log↔state drift, the
failure mode of any system that keeps events and current state as two
things a bug can let disagree, is **impossible by construction** here,
not merely tested for. A property test (patient state, at every event
boundary of a run, equals folding that patient's own event
subsequence through `evolve` from its initial state) is the executable
form of that claim, not an added nicety on top of it.

## The keystone framing: this domain already had this shape

Here is the claim this document exists to state plainly: **HL7v2 is an
event stream, and always has been.** Every ADT message — A01
admission, A02 transfer, A03 discharge, A08 update, the whole churn
family this project's `InjectChurn` stage will grow into — is a
domain event: a fact about something that happened to a patient, at a
time, with enough detail to act on. Every downstream system that
consumes an ADT feed — an ADT-triggered order system, a bed-management
board, a downstream ancillary system's own patient index — has spent
decades folding its own state from that feed, event by event, without
anyone involved calling it "event sourcing" or drawing the log as a
primitive. The pattern was there before the name; the industry just
never wrote it down as an architecture, so every consumer re-derives
the same fold logic independently, and every integration bug where two
systems' derived state disagrees is exactly the log↔state-drift failure
mode this project's engine makes impossible.

**FHIR is the materialized view of that same stream.** A FHIR
`Patient`, `Encounter`, or `Location` resource is current-state
data — a snapshot, not an event — which is precisely what a
materialized view *is* in event-sourcing vocabulary: a query-optimized
projection of an event log, kept (or in FHIR's case, typically
regenerated) for read convenience rather than carrying the history that
produced it. An ADT feed and a FHIR resource server describing the same
hospital are not two different architectures; they are the event side
and the materialized-view side of one architecture that healthcare
already had, expressed across two different standards that were never
designed to acknowledge each other's relationship this explicitly.

This simulator emits both sides *naturally*, not as two independent
rendering efforts that happen to agree, because the underlying engine
already has the shape that makes both sides cheap: `hl7v2-stream`
(`EmitHL7`, built) is the log rendered as messages; `state-document`
(`EmitState`, planned — M6) will be `state-history` — itself a
derived fold, per ADR-0008 — rendered as FHIR resources or CDA
documents at a queried instant. The **emitter-coherence** law
(`docs/sim-theory.md`'s global laws) — replaying `hl7v2-stream`
reconstructs `state-history`, and a FHIR snapshot at instant *t* agrees
with the state implied by messages up to *t* — is not a hoped-for
property bolted onto two independently-built emitters. It is the
direct consequence of both emitters consuming the same log-is-primitive
architecture the domain already implied. This project didn't invent a
clever trick to keep two output formats in sync; it made explicit a
structure HL7v2 and FHIR were already, silently, two views of.

## The upstream contrasts: what happens without this made explicit

Two mined upstream sources show, concretely, what the domain's
event-sourced shape costs a system that doesn't name it —
[`docs/patient-state-model.md`](patient-state-model.md#design-inputs-mined-from-upstream)
carries the full mining record; this section restates its two
receipts rather than re-verifying either source fresh.

**Simulated Hospital's `ir.PatientInfo`: a shadow-field zoo, one field
per thing that might need undoing.** `PatientInfo`'s location alone is
*six* fields — `Location`, `PriorLocation`,
`PriorLocationForCancelTransfer`, `PendingLocation`,
`PriorPendingLocation`, and a prior for that one too — and the
in-code comment on the third is the whole argument in one sentence:
normal flow clears `PriorLocation` after a transfer completes, but
`CancelTransfer` has to reinstate the value that was just cleared, so
it needs its own shadow copy to undo the clearing. Every cancellable
mutation in a mutable-state design without a log grows its own bespoke
undo field, because "what was true before the thing I need to cancel"
has nowhere else to live. This project's engine needs **one**
`:location` field, because `:transfer-in-error`'s `decide` can query
the log directly for the patient's prior location-setting event — the
log already *is* the undo history, so nothing needs its own copy of it.
[ADR-0010](../notes/ADRs.md#adr-0010) extends the same argument to
identity: a merge's "what MRN did this patient answer to before" is the
same kind of prior-value fact, and it lives on the merge event itself,
not in a redirect table a mutable design would need instead.

**Synthea's `Person.history`: a hand-rolled log, verified in this
project's own mining record.** Synthea's GMF modules guard conditional
transitions like "prior state X, within window" by walking
`Person.history`, the person's own recorded trail of visited module
states — which is, functionally, a mutable-world approximation of an
event log, built because the GMF interpreter had no primitive log to
query instead. `docs/patient-state-model.md`'s mining section reads
this as validation of this project's own accumulator/cursor split, not
as a design flaw in Synthea (Synthea's clinical modeling problem is a
different one, a patient's whole simulated life rather than one
hospital encounter, and it solves the history-query problem that
`Person.history` exists for well enough for its own purposes). The
observation worth carrying forward, rather than re-litigating
Synthea's own design choices: threading death through a module's
running state — so that later transitions can guard on "has this
person died" the way any other prior-state condition is guarded —
is exactly the kind of state-visit-history query `Person.history`
exists to answer, in a system with no primitive log to ask instead.
(This detail is carried per this document's own briefing rather than a
fresh source read this session; flagged as such per `AGENTS.md`'s
provenance discipline, the same way ADR-0010 flags its own SimHospital
merge inference.) M5's GMF interpreter port compiles `PriorState`-style
guards to queries over *this* project's ground-truth log directly —
typed, timestamped, and already authoritative — rather than porting
`Person.history` itself; the accumulator deliberately carries no
visit-history field of its own for exactly this reason.

## The practical payoffs

Making the domain's event-sourced shape explicit, rather than leaving
it implicit the way HL7v2 and FHIR both do, pays for itself in ways
that are specific to this project's actual use as a test-corpus
generator, not just architectural tidiness:

- **`(config, seed, version)` *is* the corpus.** Because every output —
  messages, state documents, the invariant verdict — is a pure
  function of config, seed, and the engine's own version (ADR-0009),
  a generated log or corpus is a **cache** of that function's output,
  not an artifact that has to be preserved and shipped as the source
  of truth. A consumer who needs "the same traffic again" ships the
  manifest (`ehr-testing-sim.manifest`) — the pinned inputs — and
  regenerates on demand, rather than archiving gigabytes of messages
  that a three-tuple could reproduce byte-for-byte.
- **Checkpoint/resume is replay-to-*t*.** Because `state-history` at
  any prefix is `(reduce evolve initial-world log-prefix)`, "resume a
  run from where it left off" and "show me the hospital's state at
  simulated instant *t*" are the same operation: fold the log up to
  that point. No separate checkpointing mechanism is needed because
  the log was always going to be replayed to get state anyway — a
  checkpoint is just a replay whose result is cached instead of
  recomputed.
- **Cross-version replay is explicitly not promised.** This is a
  boundary, not a gap: [ADR-0009](../notes/ADRs.md#adr-0009) states
  that a fixed seed and config reproduce byte-identical output only
  *within* a generator version — the engine's step vocabulary is
  expected to grow (M2 through M6, `.agents/plans/roadmap.md`), and
  each growth can add stochastic draws an old pathway didn't
  previously consume. A consumer who needs long-term reproducibility
  pins the generator version in the manifest alongside the seed, the
  same way they'd pin any dependency version — "replay forever" was
  never a claim this architecture makes, only "replay deterministically
  against a stated version," which is the claim a corpus's manifest
  actually lets a consumer verify.

## See also

[`docs/sim-theory.md`](sim-theory.md) and
[`docs/sim-theory.edn`](sim-theory.edn) for the full stage-by-stage
theory this document's architecture sits inside;
[`docs/patient-state-model.md`](patient-state-model.md) for the
accumulator shape `evolve` folds into and the full upstream-mining
record; [`docs/operational-models.md`](operational-models.md) for the
occupancy-board projection worked out in detail;
[`notes/ADRs.md`](../notes/ADRs.md) ADR-0002, ADR-0008, ADR-0009, and
ADR-0010 for the decision records this explainer narrates in prose.
