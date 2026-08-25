# Session prompt -- arc 2b: person-simulator, the component (ADR-0172 executed)

Context. HEAD 9d64ae2. ADR-0172 chartered the person-simulator; the author
ruled 2026-08-25: **A1 B1 C1 D1 E1 F1 G1**. F1 fixes this session's shape:
the component lands ALONE, the engine does not call it, and the corpus is
therefore provably untouched -- `bin/regression-oracle 9d64ae2 HEAD` must be
IDENTICAL with no declaration, and every pinned fixture stays byte-equal.
Green here is evidence, not absence of red (ADR-0169's shape). Read ADR-0172
whole; §2 is your spec (front door, 14 event kinds with referential fields,
hazards, streams, hooks), §3 the invariant shape, §4 the 11 limitations and
the test each owes, §5 the rulings. Do not re-derive the design; do
re-derive every line number. Payload session under the moratorium.

Step 0. ADR-0172 -> Accepted, the seven rulings quoted where they land.
Own commit.

Step 1. Component skeleton, no behaviour: `components/person-simulator/`,
top-ns `ehrt.person-simulator`, `interface.clj` exposing exactly §2's front
door -- `(persons config stream)`, `(initial-persona person-id t0)`,
`(initial-persona person-id t0 birth-ctx)` -- each throwing
`not-implemented`. Wire into root `deps.edn` `:dev` + `:test` (mirror
`poly/patient-simulator` at :42/:133/:196), `workspace.edn` if it lists
bricks, and `projects/conformance/deps.edn` ONLY if `poly check` demands
it (it should not: nothing requires the new interface yet). A
`person_simulator_charter_test.clj` in docs-tooling on the pattern of
`patient_simulator_charter_test.clj`: the ADR's front-door sentence and the
11 limitation rows are asserted present in the ADR and mirrored in the
component's own `README.md`. `poly check` clean; `make test` green.
Commit: `feat(person-simulator): skeleton and charter gate, no behaviour`.

Step 2. RED. One test per §4 limitation row (11), named as the row names
them, each asserting the limitation HOLDS (a guard that goes red if lifted:
e.g. `pregnancy-and-delivery-are-one-to-one`, `every-provisional-rate-is-
marked`); plus the §3 referential invariants over `persons` output (referent
exists and is the right kind; same subject; follows in time); plus fixed
consumption (`R-fixed-draw-consumption` spirit: the draw count of a person
over N years is a function of N alone -- assert two persons with different
outcomes consume identically); plus determinism (same config+seed ->
identical vector; different `:person` id-tags -> disjoint sequences); plus
a counted witness per event kind (`R-witness-population-is-counted`: a
config+seed under which each of the 14 kinds occurs at least once, counts
pinned, `pos?`). All RED for the reason "not implemented". Commit RED.

Step 3. GREEN, per §2: hazards as authored-provisional rates with an
in-source `PROVISIONAL` marker (E1); every draw via
`(engine/stream master :person id-tag)` -- newborn key `(parity-index, 0)`
(ADR-0171 B1); newborn Persona DERIVED from the household (A1), fewer than
13 draws, say how many; household moves drawn once by the head, members
reference `:household-move-event-id` (B1); death: `initial-persona` takes
the compiled death instant as a t0 parameter and truncates (C1) -- the
person-simulator must NOT require patient-simulator; take the instant as
data; `:identification {:merge-fraction 0.35}` config default (D1);
identification as `:identity-unavailable` / `:identity-resolution`
dispositions only (G1) -- no placeholder/fill/merge minting here. Places
from `sim-model/resources/sim-model/demographics/places.edn` (row 7).
Persona construction reuses `sim-model` -- do not fork it.

Step 4. Proof of F1: `make test` unpiped (counts reconciled vs 9d64ae2:
the delta is exactly the new tests, list them); `make integration`;
`bin/regression-oracle 9d64ae2 HEAD` with NO declaration -> IDENTICAL, all
35 roots; the four `arc0_gated_*` digests, `pinned_seed_42`, both
conformance baselines untouched (`git diff --stat` on those paths empty).
Push; CI is the gate; no tag. Record one page: kinds and their witness
counts, draw count per process, what the corpus proof showed, ADR
premises the tree contradicted (one line each). Roadmap row one line.

Fences. NO change under `components/sim-engine`, `sim-model`,
`sim-check`, `sim-emit-hl7`, `patient-simulator`, or `sim` -- if the
component needs something from them that is not on their interface, STOP
and say what. No draw outside the `:person` family. No new rulings rows.
No hazard rate presented as sourced. One reshuffle is ZERO reshuffles: if
any pinned artifact moves, that is a STOP, not a re-pin.
