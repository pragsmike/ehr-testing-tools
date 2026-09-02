# Charter — `sim-engine`

> **Draft for the author's edit.** Derived from
> `src/ehrt/sim_engine/interface.clj` and the namespaces it delegates
> to, their own docstrings, and the ADRs those docstrings cite.
> **UNCLEAR** marks a contract the shipped surface does not settle.

## 1. Mission

Own the discrete-event simulation core: the priority queue, the
decide/evolve loop, the RNG stream partition that makes every draw
reproducible, and the ground-truth event log that loop produces —
which is this workspace's public, versioned contract.

Extracted from `components/sim` at sim split B, M2 (ADR-0043,
`.agents/plans/2026-08-04-sim-split-b-plan.md`). Contents are the
union of what residual `sim`'s src-scope callers (`run`, `check`,
`emit-state`, `identifiers`) reach — found by fresh grep, not by
interface-design judgment (ADR-0018's from-live-consumers precedent).
Test-scope callers reach this component's internal namespaces
directly; Polylith permits that, and they never come through this seam.

## 2. Interface contract

`interface.clj` documents its own sections by the caller each serves.

### Orchestration surface — what `run.clj` drives the engine with

- `run` — runs the simulation: the priority queue, the decide/evolve
  loop, and the ground-truth log it produces. Delegates to
  `ehrt.sim-engine.run/run`, which carries the contract and the full
  config vocabulary.
- `config-keys` — the canonical, documented list of **every** key
  `run`'s config map accepts. This def **is** the documentation the M4
  Task 0 plumbing-completeness test checks against: a new key earns an
  entry here or the test goes red. Also read by `identifiers.clj`,
  mirroring `run`'s own config forwarding.
- `compile-patient` — one patient's Persona and compiled module
  trajectory, drawn from that patient's **own** `:patient` stream and
  **independent of when that patient arrives**. Returns
  `{:persona p :compiled c}`; `:compiled` is nil for a patient with no
  assigned closure. The whole run-start compile — persona draw, module
  walk, trajectory compile — resolved **before** the loop (ADR-0173
  ruling C1).
- `person-plan` — for a config `run` would accept: the arrival-ordinal
  bindings, the compiled deaths keyed by person, and the person index.
  This exists because of a real ordering problem (ADR-0173 §2(a),
  ruling C1): the compiled trajectory's death instant is a t0
  parameter of the process producing the person stream, keyed by
  **person**, while this engine mints a patient id from an arrival
  **ordinal** and binds the two with a `:world`-family draw at a
  pinned position inside the run. A caller cannot key those deaths
  without asking which person each arrival bound to — and this is that
  question, answered by the same pre-loop `run` itself uses, so the
  two cannot disagree.
- `person-deaths` — `person-id -> that person's death instant`, read
  off the stream's `:person-death` events. **Data the caller
  supplies**, not something this engine computes.
- `valid-persons?` — whether `run`'s engine-facing `:persons` value is
  well-formed. Result-not-throw: `run` returns
  `result/error :invalid-persons` rather than blowing up inside the
  pre-loop.
- `default-churn-profile` — all-zero: **churn OFF**. The merge base
  for a caller-supplied partial profile.
- `sample-profile` — a modest, illustrative nonzero profile: what a
  bare `--churn` flag turns on when the caller wants churn without
  hand-tuning every rate. Each probability is **per gap** (an
  insertion point around an authored step).

### State-reader surface — what `emit-state` and `identifiers` fold over

- `replay` — replays a ground-truth log through `evolve` **from an
  empty world**, returning a parallel seq of
  `{:event :patient-id :before :after :world-before :world-after}`.
  `:patient-id` is a convenience view of the event's **primary
  (first)** patient. Also read by `check` — one def, several callers,
  not duplicated per section.

### Acceptance surface — what `check.clj` validates a log against

- `documented-step-rejection-reasons` — the **closed enum** every
  `:step-rejected` event's `:reason` must be drawn from (ADR-0012's
  invariant: every rejection's reason is from a documented enum).
- `default-profiles` — loaded once at namespace load from
  `resources/order-profiles.edn`: this repo's own hashed-config
  catalytic content (committed and repo-authored, not fetched or
  generated).
- `abnormal-flag` — the **computed-truth mini-law** (M3 Task 4): a
  value's abnormal flag is **derived** by comparing it against
  `reference-range`, never sampled independently. `:normal` when
  within `[low, high]`.

### Contract surface — the ground-truth event log's own schema

The log is a public, versioned contract (author ruling Q-A(a)), so a
consumer holding an `events.edn` can tell which contract produced it.

- `event-schema-version` — the contract's own semver-shaped version,
  stamped into every `sim run` manifest as `:event-schema-version`.
- `Event` — one ground-truth event, dispatching on `:event`. The 24
  kinds are a **closed vocabulary**.
- `valid-event?` — `(valid-event? event)`.
- `explain-event` — `(explain-event event)`, the malli explanation.
- `run-t-monotone?` — the **run-level** time property: within one run,
  event times never decrease. True of the empty log and of any
  single-event log.

`Event` is exported for the **consumer-conformance** tests in
`sim-emit-hl7`, `sim-emit-fhir` and `sim-check` — the three built-in
consumers validating their own **input** against the explicit contract
instead of against a shape reverse-engineered from our HL7 emitter.
**Nothing in any production path validates: the contract costs no
runtime.**

### Stream-partition surface (ADR-0171, arc 1)

- `mix64` — a fixed, fully-specified 64-bit mix of two longs,
  **deliberately not an RNG draw**. Public since ruling A1 because the
  partition derives every stream seed with it and consumers outside
  this component now need the same derivation.
- `stream-scheme` — the partition's own version marker, stamped
  top-level into every sim manifest. **A discriminator, not a
  warranty.**
- `stream-seed` — the seed of one stream:
  `(mix64 (mix64 master family-tag) id-tag)`.
- `stream` — a fresh `java.util.Random` for one stream:
  `stream-seed`'s value handed to the one constructor the engine has
  ever used.
- `newborn-id-tag` — the `:person`-family id-tag for a newborn.
  **Exported with no caller today, deliberately**: ruling B1 fixed its
  key now so arc 2 inherits the pair (parity-index,
  within-delivery-index) rather than choosing a bare parity index and
  owing a second reshuffle when multiples stop being a v1 limitation.

## 3. Data shapes owned

| shape | what it fixes |
|---|---|
| `Event` | the 24-kind closed ground-truth event vocabulary |
| `GroundTruth` | a whole log (validator internal; not on the seam) |
| `event-schema-version` | the contract version stamped into manifests |
| `config-keys` | the closed config vocabulary `run` accepts |
| `documented-step-rejection-reasons` | the closed `:step-rejected` reason enum |
| `InjectChurn` (`churn.clj`) | the churn profile shape |
| the stream partition | family tag × id tag → seed, via `mix64` |

`ObservationEntry` is **not** owned here: `event-schema` reuses
`sim-model`'s one definition for a `:diagnostic-report`'s
`:observations` children rather than restating it.

## 4. Invariants guaranteed

- **Reproducibility by partition.** Every draw comes from a stream
  whose seed is `(mix64 (mix64 master family-tag) id-tag)`. A
  patient's compile is drawn from that patient's own `:patient` stream
  and is **independent of arrival order**.
- **Run-level time monotonicity.** `run-t-monotone?` states it;
  within one run, event times never decrease.
- **Closed vocabularies.** Event kinds, config keys, and step-rejection
  reasons are each a closed set with a gate behind it.
- **Computed truth.** `abnormal-flag` is derived from
  `reference-range`, never sampled independently.
- **Result-not-throw at entry.** A malformed `:persons` yields
  `result/error :invalid-persons` rather than an exception inside the
  pre-loop.
- **Replay starts from an empty world**, so a log is self-sufficient.
- **The contract costs no runtime**: validation is a test-time act.

## 5. Non-goals

- **Does not emit messages.** HL7 and FHIR rendering belong to
  `sim-emit-hl7` and `sim-emit-fhir`, which consume the log.
- **Does not check invariants.** The acceptance catalog is
  `sim-check`'s; this brick only publishes what that catalog needs.
- **Does not model a person's life.** It *consumes* a person stream as
  data (`:persons`, `person-deaths`) and folds it; the person process
  is `person-simulator`'s.
- **Does not own the emission RNG family.** `ehrt.sim.run` builds the
  emission stream through `mix64`/`stream` rather than reusing the
  master seed (ADR-0171 ruling C1).

## 6. Forbidden edges

Requires exactly `kernel`, `sim-model`, and `patient-simulator`.

Must never require:

- **`sim`** — `sim` orchestrates this brick; the reverse is a cycle.
- **`sim-check`**, **`sim-emit-hl7`**, **`sim-emit-fhir`** — all three
  are consumers of the log this brick produces.
- **`person-simulator`** — and here the prohibition is *structural,
  not merely conventional*. ADR-0172 limitations row 10 makes
  "engine → person: none in v1" a gate, and its reverse half is a
  **bare token scan over this component's whole `src`** — so even a
  prose citation of that component's name in this brick reads as a
  feedback edge. That is why `compile-patient`'s own interface comment
  names the need without naming the component.

## UNCLEAR — the author's review queue

- **UNCLEAR-E1 — `compile-patient` is documented twice, differently.**
  Two docstrings reach the seam for one exported name: the
  `decide/compile-patient` one ("the whole of a patient's run-start
  compile — persona draw, module walk and trajectory compile,
  resolved BEFORE the loop") and the `interface.clj` comment ("one
  patient's Persona + compiled module trajectory, drawn from that
  patient's own `:patient` stream and INDEPENDENT of when that patient
  arrives"). They are compatible, but neither is the whole promise,
  and a reader gets whichever they happen to open. Which is the
  contract of record is the author's call.
- **UNCLEAR-E2 — `newborn-id-tag` is exported with no caller.** The
  interface says so deliberately, and gives the reason (arc 2
  inherits the key rather than owing a reshuffle). It is nonetheless
  the one var on this seam that no consumer exercises, so nothing
  fails if its contract drifts. Whether that warrants a gate — the way
  `config-keys` has one — or is accepted as a priced placeholder, is
  not settled by the shipped surface.
