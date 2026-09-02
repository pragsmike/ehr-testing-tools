# Charter — `patient-simulator`

> **Draft for the author's edit.** Derived from
> `src/ehrt/patient_simulator/interface.clj`, the namespaces it
> delegates to, and this brick's own `docs/limitations.md`.
> **UNCLEAR** marks a contract the shipped surface does not settle.

**Ancestry.** This brick already had a chartered scope before this
page existed: **ADR-0162** (2026-08-21) named it — it was
`sim-trajectory`, which named the *output* of one of its three stages
rather than the thing it is — and gave it the mission sentence in §1
plus the eight-row limitations table in `docs/limitations.md`. That
table is the authority for what this brick declines, and it is
**gated** (`ehrt.docs-tooling.patient-simulator-charter-test`): a new
deliberate limitation in this `src` that is not tabled there is red.
This charter restates the scope in the common format and **does not
supersede** either the ADR or the table.

## 1. Mission

> Realistic EHR message traffic is the priority; patient-lifetime
> simulation is relevant only inasmuch as it contributes to realistic
> traffic.

That sentence is the author's ruling of 2026-08-20, and it settles
scope arguments before they start. Concretely: load Synthea-style GMF
modules, walk them against a persona under a seeded RNG, and compile
the resulting trajectory into pathway IR.

## 2. Interface contract

Sized by grep against `components/sim`'s own src and test trees before
the split — residual `sim`'s `engine` and `run`, confirmed the only
two consumers, matching the plan's own prediction — not by
interface-design judgment.

**Loading (`gmf.clj`)**

- `load-module` — `(load-module module-name json-string)` → a loaded
  GMF module.
- `valid-modules-config?` — `(valid-modules-config? modules-config)`.
- `load-closure` — `(load-closure root-id root-json-text resolve-fn)`
  and the 4-arity adding `table-resolve-fn`. Loads a module **and
  everything it calls**, following `CallSubmodule` edges through
  `resolve-fn` and lookup tables through `table-resolve-fn`.
- `singleton-closure` — `(singleton-closure module)` → the closure
  containing exactly one module, for a module that calls nothing.

**Walking (`gmf-interpreter.clj`)**

- `run-module` — walks a module against a persona under `rng`,
  producing a trajectory. Four arities, each **purely additive** over
  the last:
  `(run-module module rng persona registration-t)`;
  `+ horizon-end-t`;
  `+ modules initial-attributes tables` (ADR-0033 AR-3 — the closure's
  own maps plus an optional per-patient attribute seed, threaded by
  `:registered`'s decide method);
  `+ history?` (ADR-0042 AR-1/AR-3, Wave H pre-roll — gates the
  interpreter's `:phase` mint).
- `dob-epoch-day` — `(dob-epoch-day persona)` → the persona's date of
  birth as an epoch day. On this seam for one reason, recorded: it is
  the single var `components/oracle`'s `digest.clj` needed that was
  not already here, found by a fresh call-position census
  (standing-equipment promotion, 2026-08-05, AR-P-2).

**Compiling (`compile-trajectory.clj`)**

- `compile-trajectory` —
  `(compile-trajectory trajectory facility registration-t)` and the
  4-arity adding `history?` (ADR-0042). Turns a walked trajectory into
  the pathway IR the engine consumes.

**Coverage (`emittable-events.clj`)**

- `emittable-ground-truth-events` — `(emittable-ground-truth-events
  closures)` → which ground-truth event types a loaded closure can
  actually drive. The generator-side coverage gate's own input
  (ADR-0165), read by `ehrt.sim.run-test`, which lives outside this
  component and therefore comes through here.

## 3. Data shapes owned

- The **loaded GMF module** and the **closure** (modules + lookup
  tables) produced by `load-module` / `load-closure` /
  `singleton-closure`.
- The **trajectory**: the walked, timed sequence `run-module` returns.
- The **modules config** validated by `valid-modules-config?`.

The **pathway IR** `compile-trajectory` produces is shaped by
`sim-model`'s `Step` / `Citation` / `ConditionAnnotation` /
`ObservationEntry` — this brick produces IR to that shape but is not
its author.

## 4. Invariants guaranteed

- **Dependency direction is one-way.** Traffic *consumes* what this
  component computes, as compiled pathway IR, and never the reverse.
  Nothing downstream is required from here, and this component knows
  nothing of the engine or the emitters.
- **Additive arities.** Every arity added to `run-module` and
  `compile-trajectory` was declared purely additive, so an existing
  call keeps its exact behaviour — the condition under which the
  regression oracle can report IDENTICAL across those landings.
- **Seeded walks.** `run-module` takes `rng` explicitly; the walk
  draws nothing it was not handed.
- **Declared limitations are tabled and gated.** Every deliberate
  gap carries its citation and its trigger-if-any in
  `docs/limitations.md`; the gate requires each in-source limitation
  marker to be covered by a citation anchored in **stable text, never
  a line number**, and each citation to anchor **exactly one place**
  (the weak-anchor defect ADR-0162 found in its own gate and fixed).

## 5. Non-goals

Authoritative list: `docs/limitations.md`, eight rows. In summary, and
without restating the table's own reasoning:

- **No richer care-plan events.** The `assign_to_attribute` /
  `referenced_by_attribute` pair is a **declared limitation**, not a
  queued defect. The trigger is named and the fix is priced: it is
  owed when any emitter surface renders care-plan state — a FHIR
  CarePlan resource, or a render-time patient-context feature
  reachable by site-profile Z bindings. Until then, note the order of
  the risk: **without the fix, such a surface would render every plan
  ever started as active — a plausible-looking lie, worse than
  absence.**
- **No `:reason` three-way resolution** for MedicationOrder /
  CarePlanStart (attribute / PriorState / ConditionOnset).
- **No attribute-reference resolution for `ConditionEnd`**: the
  interpreter declares `:condition-onset` and nothing else, so the
  attribute-reference form loads without schema failure (state maps
  are open) and simply does not resolve.
- **No VitalSign `expression`.**
- **Not a lifetime model for its own sake.** Anything that does not
  change a message is out of scope by the mission sentence.

## 6. Forbidden edges

Requires exactly `kernel` and `sim-model`.

Must never require:

- **`sim`** — the split's whole point; `sim` wires this brick to the
  engine, and the coupling is the pathway-IR data contract, not a call.
- **`sim-engine`** — the engine requires *this* brick, so the reverse
  is a cycle. This brick knows nothing of encounters, beds or the
  priority queue.
- **`sim-emit-hl7` / `sim-emit-fhir`** — this brick knows nothing of
  the emitters, by the mission sentence's own dependency direction.

## UNCLEAR — the author's review queue

- **UNCLEAR-P1 — where the scope sentence lives, now that there are
  two places.** ADR-0162 deliberately put the mission sentence in
  `interface.clj`'s own SCOPE section *so that a reader who never
  opens the docs still meets it*, and in `docs/limitations.md`. This
  charter is a third copy. The gate
  (`patient-simulator-charter-test`) checks the sentence in
  `limitations.md`, not here. Two readings: *(a)* the charter is the
  new front door and the gate should be re-pointed at it; *(b)* the
  charter is a derived view and `limitations.md` stays the gated
  original — in which case this page must never be edited without
  editing that one. **This charter assumes (b)** and changed no gate,
  per the session's docs-only fence; the choice is the author's.
- **UNCLEAR-P2 — `dob-epoch-day`'s standing.** It is on the seam for
  a *tool's* convenience (`components/oracle`'s digest), not for any
  production consumer. That is recorded honestly in the interface, but
  it means the seam's stated sizing rule ("what residual sim's own
  callers reach") no longer describes the whole seam. Whether
  dev-equipment needs count as first-class consumers for sizing
  purposes is not settled by the shipped surface.
