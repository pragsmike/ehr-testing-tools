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

This simulator's engine (`ehrt.sim-engine.engine`) implements exactly
that inversion, decided at [sim/ADR-0008](../../../notes/sim/ADRs.md) and
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
  (read-only) and the run's seeded RNG streams (ADR-0171) to decide what happens,
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
(`EmitState`, built — M6, FHIR R4 first, CDA deferred) is
`state-history` — itself a derived fold, per sim/ADR-0008 — rendered as
FHIR resources at a queried instant. The **emitter-coherence** law
(`docs/sim-theory.md`'s global laws) — replaying `hl7v2-stream`
reconstructs `state-history`, and a FHIR snapshot at instant *t* agrees
with the state implied by messages up to *t* — is not a hoped-for
property bolted onto two independently-built emitters. It is the
direct consequence of both emitters consuming the same log-is-primitive
architecture the domain already implied. This project didn't invent a
clever trick to keep two output formats in sync; it made explicit a
structure HL7v2 and FHIR were already, silently, two views of.

## The coherence property, tested

Milestone M6 is where the paragraph above stops being an architectural
argument and becomes a property test — the first time this project has
had two emitters to check against each other at all.
`ehrt.sim-emit-hl7.v2-replay` is an INDEPENDENT reconstruction of patient
state, built the wire-consumer's way: parse a run's own emitted ER7
stream (the same `org.clojars.cmiles74/clojure-hl7-parser` structures
`EmitHL7` renders through) and fold it, message by message
(`fold-message`), into state — never touching `ehrt.sim-engine.engine`,
the ground-truth log, or the RNG. This is the same shape `EmitState`'s
own `snapshot-at` embodies from the log-fold side (sim/ADR-0008's `replay`),
mirrored from the wire side: two independent folds of two independent
renderings, checked against each other rather than against a shared
implementation either could quietly share a bug with.

The comparison needs one more piece, because the two folds don't carry
identical information by design — the wire is a lossy rendering of
truth, on purpose (no PV1 field distinguishes a licensed bed from a
surge slot; DG1/RXO segments for conditions and medications were never
built). `ehrt.sim-emit-hl7.v2-replay/project-to-wire-visible-fields` is
the formal statement of exactly what's wire-visible and what isn't —
sibling of `ehrt.sim-emit-hl7.site-profile`'s own dialect-masking function
(that one states what a *dialect* may touch; this one states what the
wire carries *at all*, truth-only or not) — applied identically to both
the log-folded state and the message-reconstructed state before they're
compared, so "what the wire carries" is answered once, not maintained
as two hand-tuned shapes that could drift from each other.

The property
(`ehrt.sim-emit-hl7.v2-replay-test/emitter-coherence-reconstructed-state-matches-the-log-fold-at-every-boundary`,
150 trials over pathways, order/result, and non-two-participant churn,
plus a 150-trial sibling over module-driven trajectories) checks
agreement at EVERY message boundary, not just end-of-run — the stronger
claim `docs/sim-theory.md`'s own wording ("a snapshot at instant *t*
agrees with the state implied by messages up to *t*") actually makes.
One genuine finding surfaced along the way, not papered over: a
degenerate but structurally legal churn sequence (a cancel-admit against
an already-discharged patient's original admission, followed by a
cancel-discharge) left ground truth's own `:class` field absent, while
`EmitHL7`'s own PV1-2 rendering always asserts `:inpatient` for that
message family regardless. The fix landed in `ehrt.sim-engine.engine`'s
own `:cancel-discharge` fold (restoring `:class` as part of what it
reinstates) — the property caught a real gap in ground truth, and the
fix closed the gap rather than loosening what the projection would
tolerate.

The cross-emitter id sub-law (`docs/sim-theory.md`'s global laws,
originally a named gap this document's own determinism table below
flagged as open) is checked the same way, from the other direction:
`ehrt.sim-emit-fhir.emit-fhir-test/fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-identity`
asserts that a FHIR `Patient.id` is the same `patient-id`
`ehrt.sim-engine.streams/patient-id-for` assigns, and `Patient.identifier`
carries the same active MRN that patient's own HL7 messages render as
PID-3 — over 150 random runs, not merely by construction of one
hand-picked demo.

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
[sim/ADR-0010](../../../notes/sim/ADRs.md) extends the same argument to
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
provenance discipline, the same way sim/ADR-0010 flags its own SimHospital
merge inference.) M5's GMF interpreter port compiles `PriorState`-style
guards to queries over *this* project's ground-truth log directly —
typed, timestamped, and already authoritative — rather than porting
`Person.history` itself; the accumulator deliberately carries no
visit-history field of its own for exactly this reason.

## Scope: what this architecture buys, and what it doesn't

A tempering note, not a walk-back: an independent research pass over
SimHospital's and Synthea's own public issue trackers, source, and
practitioner discussion (`docs/research/SimHospital-Synthea-limitations-
considered.md`, retrieved 2026-07-26) confirms the state-machinery
critique this document makes — and sharpens exactly where its boundary
sits. The report verifies further receipts beyond the two this document
already cites above, each the same shadow-field-accretion family as
`PatientInfo`'s six location fields, not a new failure mode: visit
deletion pops the latest identifier off a `PastVisits` stack rather than
deriving "the prior visit" from a durable fact stream; pending-location
cancellation shuffles values between `PendingLocation` and
`PriorPendingLocation` rather than the log simply being queried; and an
in-code comment on consecutive pending encounters admits outright that
the first "will never be finished, since only the latest Encounter is
checked" — direct evidence of the single-current-object assumption this
document's own argument predicts.

What that critique does **not** reach, and the report is explicit about
this too: the dominant complaints practitioners actually file against
both tools are about **clinical fidelity** — Synthea's simplified and
isolated disease models, its heterogeneous-outcome gap, its US-centric
demographics — not storage architecture. Event sourcing dissolves the
state-machinery failure class this document is about: reproducibility,
correction/supersession, audit, and the coherence laws this project's
own validation program states as claims 3 and 7
(`docs/problem-statement.md`). It does not, by itself, buy clinical
realism — validation claims 4 and 5, "would a clinician find these
trajectories credible" and "does this look like a real ADT feed" —
which rest on entirely separate mechanisms this project already treats
as separate: content provenance (Synthea-derived GMF modules, M5),
statistical calibration against published references, and capacity
modeling (`docs/operational-models.md`'s allocation ladder, already
built). Keeping these two arguments apart is deliberate, not a gap the
report caught: an event-sourced engine that generated medically
implausible trajectories would be exactly as untrustworthy as a
mutable-state one, just for a different, unrelated reason.

The report's own final assessment (§9) states, independently, the same
design target this project's architecture and roadmap already converge
on: "an immutable, replayable clinical-event ledger with explicit
correction/supersession semantics; deterministic simulation time and
identifiers; separately materialized current-state views; composable
reactions across modules; and empirically validated domain models."
Every clause in that sentence names a decision this project has already
made or has on its roadmap — sim/ADR-0008's log-is-primitive engine,
sim/ADR-0011's deterministic time model and sim/ADR-0010's deterministic
identity, the occupancy-board/state-history projection pattern
(`docs/operational-models.md`), `sim-theory.edn`'s IR-transform
composition (InjectChurn), and M5's empirically-sourced GMF module
content — arrived at independently of this report, not retrofitted to
match it after the fact. See `docs/research/SimHospital-Synthea-
limitations-considered.md` for the full compendium this section
distills; this project treats it as evidence to cite, not a source to
re-verify each time it comes up.

## Determinism threats: what a seed alone doesn't guarantee

Synthea's own reproducibility saga — issues #682 and #1342, PRs #756
and #1237 (`docs/research/SimHospital-Synthea-limitations-considered.md`
§4.1) — is worth citing directly here because it demonstrates, with a
paper trail, a claim this document otherwise only argues structurally:
a seed is necessary, not sufficient. Their failure modes double as a
checklist against this project's own defenses.

| Threat (theirs) | Our status |
|---|---|
| UUID/id generation divergence | Defended: deterministic `:patient-id` (sim/ADR-0010) and control ids, both derived from the run's seed, never `java.util.UUID/randomUUID` or similar wall-clock/hardware entropy |
| Unordered-collection iteration | Mostly defended (the engine's work queue is a `sorted-map`); gap closed this session — `emitter-order-independence-test` (`test/ehrt/sim/emitter_order_independence_test.clj`) guards that `emit-hl7` never depends on a map's or set's own iteration order when building segments |
| Reference *date* vs full timestamp | Defended: relative seconds (sim/ADR-0011) plus an explicit `:reference-date` and a fixed `:utc-offset`, both pinned in the run manifest, never a bare current-time reference |
| Locale/OS differences | Partially defended (locale/timezone recorded in the manifest); revisit once CI exists (`.agents/plans/roadmap.md`'s CI trigger) |
| Cross-format id divergence (CDA vs FHIR vs CSV) | Defended for HL7v2/FHIR, property-tested: `Patient.id`/`Patient.identifier` resolve to the SAME `patient-id`/active-mrn `EmitHL7` uses (`sim-theory.md`'s cross-emitter id sub-law, `emit-fhir-test/fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-identity`, 150 trials). CDA is out of scope until it's built (EmitState's own format-dispatch contract note) — not a gap in what's landed, a boundary of what hasn't |

The structural defense behind the first four rows is already argued
elsewhere in this document (`:patient-id`/`:mrns` generation, the
seconds/UTC-offset time model, the seeded RNG streams of ADR-0171); the table exists
to make the comparison to a documented real-world failure explicit
rather than left merely asserted.

## The practical payoffs

Making the domain's event-sourced shape explicit, rather than leaving
it implicit the way HL7v2 and FHIR both do, pays for itself in ways
that are specific to this project's actual use as a test-corpus
generator, not just architectural tidiness:

- **`(config, seed, version)` *is* the corpus.** Because every output —
  messages, state documents, the invariant verdict — is a pure
  function of config, seed, and the engine's own version (sim/ADR-0009),
  a generated log or corpus is a **cache** of that function's output,
  not an artifact that has to be preserved and shipped as the source
  of truth. A consumer who needs "the same traffic again" ships the
  manifest (`ehrt.sim.manifest`) — the pinned inputs — and
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
  boundary, not a gap: [sim/ADR-0009](../../../notes/sim/ADRs.md) states
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
[`notes/sim/ADRs.md`](../../../notes/sim/ADRs.md) sim/ADR-0002, sim/ADR-0008, sim/ADR-0009, and
sim/ADR-0010 for the decision records this explainer narrates in prose.
