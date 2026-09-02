# Charter — `sim-check`

> **Draft for the author's edit.** Derived from
> `src/ehrt/sim_check/interface.clj` and `check.clj`, their own
> docstrings, and the ADRs those docstrings cite. **UNCLEAR** marks a
> contract the shipped surface does not settle.

## 1. Mission

Own the **invariant catalog**: the acceptance criteria a ground-truth
event log must satisfy, and the verdict over one.

Extracted from `components/sim` at sim split B, M4 (ADR-0043,
`.agents/plans/2026-08-04-sim-split-b-plan.md`).

## 2. Interface contract

One function, four arities. The seam's contents are **exactly the
union of what residual sim's own src-scope callers reach today** —
`interface.clj`'s façade delegation, all four arities, and `run.clj`'s
3-arity call — found by fresh call-position grep, **not by
interface-design judgment** (ADR-0018's from-live-consumers
precedent). Test-scope callers repoint to this component's internal
`check` namespace directly, never through this seam.

- `check-all` — the catalog's verdict over a ground-truth log.
  - `(check-all ground-truth)` — against shipped defaults.
  - `(check-all ground-truth facility-config)` — against the facility
    that produced the log.
  - `(check-all ground-truth facility-config warm-up-seconds)`.
  - `(check-all ground-truth facility-config warm-up-seconds
    order-profiles-config)`.

The arities exist because **four invariants need config to judge
correctly.** Q14(a) (2026-09-01) records what happens without it: `ehrt
sim check` had no config input at all, so those four ran against the
shipped defaults whatever produced the log — and a scenario overriding
`:facility` therefore read as violating `:occupancy-within-capacity`
on its **own clean log** (`demos/scenarios/ed-tuesday`, whose Emergency
ward carries 16 surge slots against the default 6). **The corpus was
sound; the checker was starved.**

## 3. Data shapes owned

- The **invariant catalog** itself: which invariants exist, what each
  one is called, and what a violation of it looks like.
- The **verdict** `check-all` returns over a log.

The event log it reads is `sim-engine`'s `Event` contract; the
facility and order-profile configs are `sim-model`'s. This brick owns
the *judgement*, not the *shapes*.

## 4. Invariants guaranteed

- **A run self-checks.** `sim`'s `run-command` runs this catalog over
  its own output, and **a violation is an `:error`, not a
  `:rejected`** — a bug in us rather than a legitimate no.
- **A flagless check is byte-identical to the 1-arity call**, so
  adding config cannot silently change the default verdict.
- **Config-fed invariants judge against the config that produced the
  log**, once given it — the whole point of the arities.
- **One `:ready` predicate.** `earlier-rungs-exhausted?` draws through
  `sim-model`'s exported `free` rather than restating it, so the
  bed-status gate is the same predicate here, in `sim-model`'s ladder,
  and in `sim-engine`'s `bed-ready-location`.
- **Rejection reasons come from a closed enum.** Every
  `:step-rejected` reason is checked against `sim-engine`'s
  `documented-step-rejection-reasons` (ADR-0012's invariant).
- **Consumer conformance.** This is one of the three built-in
  consumers that validate their own input against the explicit `Event`
  contract, in test scope — the contract costs no runtime.

## 5. Non-goals

- **Simulates nothing.** It reads a log it is handed.
- **Emits nothing.** No HL7, no FHIR.
- **Owns no config schema.** Facility and order-profile shapes are
  `sim-model`'s, validated before the engine starts.
- **Not a CLI capability.** `ehrt sim check` is `sim`'s
  `check-command`, which lives beside `run-command`'s own self-check
  call deliberately — the same orchestration step (config → facility →
  catalog). This brick supplies only the catalog.
- **Does not widen its own seam for tests.** Test-scope callers reach
  `check` directly; the seam stays sized by src-scope callers.

## 6. Forbidden edges

Requires exactly `kernel`, `sim-engine` and `sim-model` in `src`.

Must never require:

- **`sim`** — `sim` delegates to this brick; the reverse is a cycle.
- **`sim-emit-hl7`** or **`sim-emit-fhir`** — the catalog judges
  ground truth, never rendered output. An edge to an emitter would let
  an invariant depend on how a message happens to render, which is the
  coupling the split removed.
- **`patient-simulator`**, **`person-simulator`** — it checks the log,
  not how the log was produced.
- **`corpus`**, **`bases/cli`**.

## UNCLEAR — the author's review queue

- **UNCLEAR-CK1 — `order-profiles-config` is on the seam but not
  threaded by the CLI.** The 4-arity exists and `sim`'s façade
  delegates all four, but Q14(a) records that `ehrt sim check
  --config` threads **facility and warm-up, not order profiles**. So
  the fourth argument is reachable from a library caller and
  unreachable from the command line. Two readings: *(a)* deliberate
  and staged — facility was the invariant that fired live, order
  profiles have no witnessed failure yet, so the flag is owed but not
  urgent; *(b)* an oversight in the same class as the one Q14(a)
  fixed, in which case a scenario overriding order profiles will
  misreport on its own clean log exactly as `ed-tuesday` did. The
  shipped surface shows the gap but does not say which.
- **UNCLEAR-CK2 — the catalog is not enumerable from the seam.**
  `check-all` returns a verdict, but nothing exported names the
  invariants — there is no `documented-invariants` counterpart to
  `sim-engine`'s `documented-step-rejection-reasons` or
  `config-keys`. An agent asked "what does this project guarantee
  about a run?" cannot answer from this brick's interface, only by
  reading `check.clj`. Whether the catalog should be data on the seam,
  the way the pairing registry and the rejection enum are, is the
  author's call.
