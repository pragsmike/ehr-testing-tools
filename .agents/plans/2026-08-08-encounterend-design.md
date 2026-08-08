# EncounterEnd fidelity — design brief

Design-channel draft (2026-08-08), for the author's ruling before any
interpreter code moves. Probe-grounded: every upstream claim below was
read from `synthetichealth/synthea` `State.java` at the pin
(`7e08387c68a7f0e21d13076609a159fd473fc902`, `EncounterEnd.process`,
~lines 1159–1200), and every in-tree claim from
`components/sim-trajectory/src/ehrt/sim_trajectory/gmf_interpreter.clj`
at `42cd1e0`. This brief lands with the arc's opening riders; the
rulings it requests (R1–R3, bottom) gate the fix session.

## The incident record

Two vendoring batches deferred a module whole on the same root cause:
`anemia___unknown_etiology.json` (ADR-0071 — 12/17/6 invariant
violations at 300 patients across three seeds) and
`colorectal_cancer.json` (ADR-0072 — 2 of 3 seeds rejected), both via
`anemia/anemia_sub.json`'s close-encounter-if-open idiom. The shared
submodule is ALREADY vendored (it landed inside `hypothyroidism`'s
closure, whose own call path never reaches the idiom unmatched). Both
deferred modules are otherwise vendor-ready: content-producing at
census, closures enumerated, NOTICE rows drafted and withdrawn.

## Upstream's semantics — five arms, guarded

`EncounterEnd.process(person, time)`:

- **A1 — own encounter open** (`hasCurrentEncounter()` and the holder
  is this module): really end it — record close, discharge disposition
  attached, provider released. Proceed.
- **A2 — wellness encounter open** (holder is the shared
  EncounterModule): do NOT close the record encounter; remove the
  module's own active-wellness attribute (exit the wellness context).
  Proceed.
- **A3 — stale active-wellness attribute** (no current encounter, but
  the attribute lingers from a timestep-boundary race): cleanup,
  proceed.
- **A4 — someone else's encounter open**: return `false` — the state
  BLOCKS and retries on a later pass.
- **A5 — nothing open**: proceed as a no-op.

The idiom the two deferred modules use is exactly upstream-legal: an
EncounterEnd on a path where the encounter may or may not still be
open, relying on A5's no-op arm.

## Our divergence — two defects, one compile site

`gmf_interpreter.clj` ~1697 compiles `:encounter-end`
UNCONDITIONALLY via `emit-and-advance` — there is no arm dispatch at
all. And the reference indexer (`index-of-last-open-encounter`, ~1209)
is openness-blind: it returns the index of the last `:encounter`
EVENT, closed or not, and `nil` when there was never one — the
dangling discharge, precisely. Note the walk already proves openness
is trackable: Wave H's phase-inheritance fold (~1937–1950) pairs each
`:encounter` with its matching `:encounter-end` and clears state on
consumption, one in-flight encounter at a time ("encounters never nest
in this project's own GMF subset").

## Proposed semantics for our subset

Walk state gains real openness tracking (an open-encounter index, set
on `:encounter`, cleared on the matched `:encounter-end` — the Wave H
fold's own discipline, moved from the post-walk phase pass into the
walk itself). The compile rule becomes:

- **A1 (open, ours)**: emit `:encounter-end` referencing the TRACKED
  open index (not the last-encounter guess). Unchanged observable
  behavior for every currently-passing module.
- **A5 (nothing open)**: NO EVENT — ordinary transition taken. The
  fix's heart, and upstream's own observable behavior.
- **A2/A3 (wellness arms)**: our subset runs one module per walk and
  compiles wellness encounters as the module's own events (Wave G),
  so the shared-EncounterModule ownership distinction has no analog;
  proposed treatment: identical to A1/A5 by openness alone, with the
  divergence DISCLOSED in the interpreter doc (a wellness encounter
  opened by this module's walk closes like any other; there is no
  cross-module wellness context to exit). — R1 rules on this.
- **A4 (blocked on another module)**: unreachable under
  one-module-per-patient (the Wave G deferral's own standing scope);
  no compile arm, disclosed, with the existing multi-module revisit
  trigger extended to name it.

Suppressed ends are COUNTED: the walk context tallies
`:suppressed-encounter-ends`, surfaced in census walk rows and
round-trip metadata — not an error (A5 is legal semantics), but a
zero-cost diagnostic so a module leaning on the no-op arm is visible,
per the error-honesty lesson that absorbed signals should at least be
countable. — R2 rules on this.

## Blast radius, and the protocol that contains it

A semantics change to a compile arm can move trajectories for ANY
module whose walks contain currently-unmatched ends — which would move
oracle digests: the first declared digest change since the coverage
waves. The protocol: BEFORE the fix, a probe pass enumerates unmatched
`:encounter-end` occurrences across all vendored modules' oracle-seed
walks and PREDICTS, per root, identical-or-moves. Expected: all 27
current roots identical (each passed the invariant catalog at its own
seeds, which dangling ends fail) — a predicted mover is
STOP-AND-REPORT with evidence before any code lands. AFTER the fix:
the oracle bracket must match the prediction exactly; a labeled census
re-run records substance movement catalog-wide; anemia and colorectal's
currently-failing round-trips are the red-first tests, their green the
fix's own proof. — R3 rules on the acceptance bar.

## The payoff rider

With the fix green: anemia and colorectal vendor as a mini-batch under
the standing mechanics (verbatim copy, NOTICE rows, first-baseline
oracle roots, population-scale round-trips) — closing the vendoring
arc's two oldest deferrals and retiring the roadmap's two-module
blocker row.

## Rulings requested

- **R1 — the wellness arms**: adopt the proposed openness-only
  treatment with disclosed divergence, or require a deeper wellness
  ownership model first?
- **R2 — suppressed-end visibility**: count-and-surface (recommended),
  or silent no-op matching upstream's own silence?
- **R3 — the acceptance bar**: all 27 roots predicted-and-confirmed
  identical with any mover escalating (recommended), or tolerate
  declared movement with per-root evidence?
