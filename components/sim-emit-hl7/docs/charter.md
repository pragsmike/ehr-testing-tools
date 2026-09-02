# Charter — `sim-emit-hl7`

> **Draft for the author's edit.** Derived from
> `src/ehrt/sim_emit_hl7/interface.clj` and the three namespaces it
> delegates to (`emit-hl7`, `fan-out`, `v2-replay`), their own
> docstrings, and the ADRs those docstrings cite. **UNCLEAR** marks a
> contract the shipped surface does not settle.

## 1. Mission

Render a ground-truth event log as HL7 v2 messages on a wire — the
message vocabulary, the timing of each message relative to the event
that caused it, the emission add-ons that make traffic look real, and
the fan-out of one rendered stream to several subscribers.

Extracted from `components/sim` at sim split S3 (ADR-0025,
`.agents/plans/2026-08-02-sim-split-plan.md` AR-6). The seam is what
residual `sim`'s own src **and test** trees call, found by grep.

## 2. Interface contract

### Rendering

- `default-reference-date` — the pinned reference date HL7 timestamp
  anchoring defaults to.
- `default-utc-offset` — the pinned fixed UTC offset rendering
  defaults to.
- `control-id-for` — `(control-id-for ev)` → the MSH-10 control id for
  an event.
- `emit` — `GT × reference-date × utc-offset [× facility × providers
  [× site-profile]]` → rendered messages. Three arities on this seam;
  `emit-hl7`'s own **2-arg** arity has zero real external callers and
  stays unexported.
- `emit-wire` — `GT × reference-date × utc-offset × facility ×
  providers × site-profile × offsets [× emission]` → **TimedWire**.
  This is the arc-4 rendering entry point: `offsets` comes from
  `plan-latency`, and the optional `emission` map carries the add-on
  profiles `ehrt.sim.run` forwards verbatim.

### Planners — the second clock and the arc-4 add-ons

Each planner is a **separate, pure step** that computes instructions
`emit-wire` then renders. `ehrt.sim.run` is the caller for all of them.

- `plan-latency` — `RNG × GT × LatencyProfile → offsets`. The second
  clock (ADR-0109): a message's wire time is not its event's time.
- `plan-chatter` — `RNG × GT × ChatterProfile → chatter render
  instructions`. `plan-latency`'s sibling, exported for the same
  reason and at the same seam.
- `plan-charges` — `GT × ChargesProfile → {:lines … :skipped …}`.
- `plan-ladders` — `GT × LadderProfile → {:rungs [...] :final #{...}}`.
  **No RNG in the signature, and that is the design rather than an
  omission**: a rung is a fixed fraction of an interval the log
  already carries.
- `plan-fan-out` — `messages × subscribers × site-profile` → one plan
  entry per subscriber. `ehrt.sim-emit-hl7.fan-out/plan`'s docstring
  carries the **subsequence law** and the **PV1-less rule**. Called
  once the message vector exists — the only seam at which the base
  spool is complete and nothing has been written yet.
- `mask-msh` — `message × overrides × replacement-fn` → the message
  with exactly the named MSH fields rewritten. **The mask half of the
  subsequence law**, exported so a gate can *erase* the same fields on
  both sides rather than reimplement the mask.

There is deliberately **no `plan-siu`**, and that is the point: `:siu`
has no planner, no stream and no draw. `ehrt.sim.run` forwards the
profile verbatim into `emit-wire`'s `:emission` map, and
`event->messages` reads it per event.

- `siu-renders?` — `SiuProfile × event-kind → boolean`.

### Vocabularies

These are the closed sets a config or a gate may name.

- `skeleton-message-types` — every MSH-9 the registry produces, as
  `TYPE^TRIGGER`; `gate v2`'s sampling policy's own skeleton half.
- `add-on-message-types` — every MSH-9 arc 4's emission add-ons
  produce.
- `emittable-message-types` — the whole vocabulary this emitter can
  put on a wire, and therefore **the allow-list a `:fan-out` filter
  may name**.
- `chatter-event-kinds` — the three ground-truth kinds an
  event-driven chatter rule may cover.
- `siu-event-kinds` — the four ground-truth kinds SIU^S12 renders,
  derived from the registry.
- `room-and-board-code` — the reserved price-table key for a
  per-inpatient-day charge line.

### Replay (`v2-replay`)

- `fold-message` — `acc × message → acc'`. Gained its first real
  external caller at the player board (ADR-0067, AR-BB2-1):
  `corpus`'s board sink folds a paced HL7 v2 stream into the same
  accumulator shape the emitter-coherence property already reasons
  about. **`fold-message` is the entire surface that caller needs, so
  it is the entire surface exported.**
- `beds-key` — the A20 stream's own key inside a `fold-message`
  accumulator (arc 3b sweep 2).

## 3. Data shapes owned

| shape | what it fixes |
|---|---|
| the **message-type registry** | the `TYPE^TRIGGER` vocabulary, and the event-kind keywords `sim-model`'s `LatencyProfile` is keyed by |
| **TimedWire** | a rendered message plus its wire time |
| the **fan-out plan** | one entry per subscriber, under the subsequence law |
| the `fold-message` **accumulator** | including `beds-key` |
| **site profiles** | fully internal — see §5 |

The **profile schemas** (`LatencyProfile`, `ChatterProfile`,
`ChargesProfile`, `LadderProfile`, `SiuProfile`, `FanOutProfile`) are
**`sim-model`'s**, not this brick's. This brick consumes them.

## 4. Invariants guaranteed

- **The subsequence law.** A subscriber's stream is a subsequence of
  the base stream, modulo the MSH fields a routing override rewrites —
  which is exactly why `mask-msh` is exported: a gate erases the same
  fields on both sides rather than reimplementing the mask and
  drifting from it.
- **Distinct subscriber names.** Enforced upstream by
  `FanOutProfile`; two subscribers sharing a name would write two
  spools into one directory.
- **Planning is separate from rendering.** Every add-on is a pure
  planner producing instructions, and `emit-wire` renders them. Only
  `plan-latency` and `plan-chatter` take an RNG; `plan-ladders`,
  `plan-charges`, `plan-fan-out` and SIU take none, because nothing
  about them is sampled.
- **The wire clock is not the event clock.** Message time is the
  event's time plus a planned offset (ADR-0109).
- **Closed, derived vocabularies.** `skeleton-message-types`,
  `add-on-message-types`, `emittable-message-types`,
  `chatter-event-kinds` and `siu-event-kinds` are derived from the
  registry rather than restated, so a config's allow-list cannot name
  a type this emitter could not produce.

## 5. Non-goals

- **Does not decide what happens** — it renders a log it is handed.
  No simulation, no world, no RNG beyond the two planners that draw.
- **Does not own the profiles** it is configured by; those are
  `sim-model`'s schemas, validated before the engine starts.
- **Does not check invariants.** `sim-check` does.
- **Does not render FHIR.** That is `sim-emit-fhir`.
- **`site-profile` is not on the seam.** It has **no real external
  caller at all** — fully internal to this component, invisible from
  outside it — even though `emit` and `emit-wire` both take one as an
  argument.
- **Does not spool or write files.** `plan-fan-out` returns a plan;
  writing is the caller's.

## 6. Forbidden edges

Requires exactly `sim-model` (for the facility, providers, and the
profile schemas) in `src`.

Must never require:

- **`sim`** — `sim` orchestrates this brick.
- **`sim-engine`** — this brick consumes the event log as **data**,
  validating its input against the exported `Event` contract in
  **test scope only**. Note the asymmetry with `sim-emit-fhir`, which
  does require `sim-engine` in `src` (see UNCLEAR-H1).
- **`corpus`** — `corpus` is a consumer of `fold-message`, not a
  dependency.
- **`patient-simulator`**, **`person-simulator`**, **`sim-check`**.

## UNCLEAR — the author's review queue

- **UNCLEAR-H1 — the two emitters take different edges to the same
  contract.** `sim-emit-fhir`'s `src` requires
  `ehrt.sim-engine.interface`; this brick's `src` requires only
  `sim-model`, and reaches the `Event` contract in test scope. Both
  emitters are described as consumers validating their own input
  against the explicit contract. Two readings: *(a)* the difference is
  real and principled — the FHIR emitter needs something from
  `sim-engine` at runtime that the HL7 emitter does not; *(b)* it is
  drift, and one of the two edges is incidental. The shipped surface
  shows the difference but does not explain it.
- **UNCLEAR-H2 — `emit` and `emit-wire` overlap, and neither is
  marked the successor.** Both render; `emit-wire` takes planned
  `offsets` and an `:emission` map, `emit` does not. Whether `emit`
  is retained for callers that want the unplanned rendering, retained
  only for tests, or is a predecessor awaiting retirement is not
  stated on the seam. Its 3-arity is exported and its 2-arity
  deliberately is not, which suggests the former, but the charter will
  not assert what the surface does not say.
