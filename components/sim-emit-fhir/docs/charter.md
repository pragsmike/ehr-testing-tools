# Charter — `sim-emit-fhir`

> **Draft for the author's edit.** Derived from
> `src/ehrt/sim_emit_fhir/interface.clj` and `emit-fhir.clj`, their own
> docstrings, and the ADRs those docstrings cite. **UNCLEAR** marks a
> contract the shipped surface does not settle.

## 1. Mission

Render a ground-truth event log as FHIR bundles — the second of the
two built-in emitters, and the smallest interface in the workspace.

Extracted from `components/sim` at sim split B, M3 (ADR-0043,
`.agents/plans/2026-08-04-sim-split-b-plan.md`, AR-3 discipline).

## 2. Interface contract

The whole seam is one function.

- `bundle-run` — `(bundle-run ground-truth reference-date utc-offset
  run-id t)` → the run's FHIR bundles. `reference-date` and
  `utc-offset` are the same pinned rendering inputs the HL7 emitter
  takes; `run-id` and `t` identify the bundle.

The seam is exactly what residual `sim`'s own `src` calls from outside
this component, determined by grep against `run.clj` and
`identifiers.clj`. `snapshot-at` has **no real external caller** —
confirmed by that same grep, its two mentions in `identifiers.clj`
being **docstring prose, not calls** — and stays unexported, fully
internal. Test scope reaches it, and every other internal def,
directly.

## 3. Data shapes owned

- The **FHIR bundle** this emitter produces, and the mapping from each
  ground-truth event kind to the FHIR resources that represent it.
- The **patient-snapshot** shape that internal `snapshot-at` produces.

It owns no schema shared with another brick: the `Event` contract it
consumes is `sim-engine`'s.

## 4. Invariants guaranteed

- **Pinned rendering inputs.** `reference-date` and `utc-offset` are
  arguments, never ambient — the same discipline the HL7 emitter
  follows, so a bundle is reproducible from the log plus those two
  values.
- **Consumer conformance.** This brick is one of the three built-in
  consumers that validate their own **input** against
  `sim-engine`'s exported `Event` contract, rather than against a
  shape reverse-engineered from the HL7 emitter. That validation is
  test-time: **the contract costs no runtime.**
- **A minimal seam.** One exported function; everything else is
  internal by demonstrated absence of an external caller, not by
  assertion.

## 5. Non-goals

- **Does not simulate.** It renders a log it is handed.
- **Does not render HL7 v2.** That is `sim-emit-hl7`.
- **Does not check invariants.** `sim-check` does.
- **No emission add-ons.** Latency, chatter, charges, status ladders,
  SIU and fan-out are all HL7-side concerns; this emitter has no
  planner surface and takes no RNG.
- **Does not expose snapshots.** `snapshot-at` is deliberately
  unexported despite being named in a sibling component's prose.

## 6. Forbidden edges

Requires exactly `sim-engine` in `src` (`emit_fhir.clj:61`).

Must never require:

- **`sim`** — `sim` orchestrates this brick.
- **`sim-emit-hl7`** — the two emitters are siblings; neither may
  depend on the other, or the "second consumer validating
  independently" property is lost.
- **`sim-check`**, **`patient-simulator`**, **`person-simulator`**,
  **`corpus`**.

## UNCLEAR — the author's review queue

- **UNCLEAR-F1 — why this emitter requires `sim-engine` in `src` and
  its sibling does not.** `sim-emit-hl7`'s `src` requires only
  `sim-model`; this brick's requires `sim-engine`. Both are described
  as consumers of the same event-log contract, and the contract is
  stated to cost no runtime — which makes a *production* edge to
  `sim-engine` surprising. Two readings: *(a)* the FHIR emitter
  genuinely needs something from `sim-engine` at runtime (a projection,
  a vocabulary) that the HL7 emitter reaches another way; *(b)* the
  edge is incidental and could be narrowed to `sim-model` plus a
  test-scope contract dependency, matching its sibling. Recorded, not
  resolved — narrowing an edge is a code change, and this session's
  fence is docs-only.
- **UNCLEAR-F2 — is `bundle-run` the whole capability?** The HL7 side
  has a planner/renderer split, latency, and a documented second
  clock; the FHIR side has one function and no notion of wire time at
  all. Whether FHIR bundles are deliberately outside the second-clock
  model (bundles are pulled, not pushed, so wire time is meaningless)
  or simply have not been given one is not stated on this seam or in
  its ADR citation.
