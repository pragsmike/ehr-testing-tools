# Charter — `person-simulator`

> **Draft for the author's edit.** Derived from
> `src/ehrt/person_simulator/interface.clj`, its two delegate
> namespaces, and this brick's own `docs/limitations.md`.
> **UNCLEAR** marks a contract the shipped surface does not settle.

**Ancestry.** Chartered at birth by **ADR-0172** (2026-08-25), "arc 2:
the person-simulator, chartered from the tree". The mission sentence
in §1, the dependency-direction paragraph in §4, the front door in §2
and the limitations table are that ADR's, and its table is **gated**
(`ehrt.docs-tooling.person-simulator-charter-test`): a new deliberate
limitation in this `src` that is not tabled in `docs/limitations.md`
is red. This charter restates the scope in the common format and
**does not supersede** the ADR or the table.

## 1. Mission

> The person process exists so that demographic and identity traffic
> is realistic; a person's life is relevant only inasmuch as it
> changes a message.

That sentence settles arguments about scope before they start. It is
why this component models a **residence move** (one A08 and a changed
PID-11) and **not** a commute; why it models an **employment change**
(a coverage change, an IN1, and an occupational-injury hazard) and
**not** a job title.

## 2. Interface contract

The front door, exactly as ADR-0172 §2 states it:

```
(persons config stream)                  ; -> [PersonEvent], t-ordered
(initial-persona person-id t0)           ; -> Persona   (the t0 state)
(initial-persona person-id t0 birth-ctx) ; -> Persona   (a newborn, ruling A1)
```

- `persons` — `(persons config stream)` → the run's person-event
  stream: a **t-ascending vector** of person events, drawn **entirely**
  from the `:person` stream family. Returns **data, never state** —
  the engine folds them; this component folds nothing. See
  `ehrt.person-simulator.process/persons` for the config keys and the
  nineteen-variate-per-person-year draw block.
- `initial-persona` — the t0 Persona for one person.
  `(initial-persona person-id t0)` is, in v1, exactly the
  `(sim-model/persona rng …)` call it replaces at
  `ehrt.sim-engine.engine`'s `:registered` decide method.
  `(initial-persona person-id t0 birth-ctx)` is ruling A1's **newborn
  path**: `birth-ctx` carries what the household determines, so a
  newborn's Persona is **derived rather than sampled — four draws, not
  thirteen**.

`t0` is a **context** rather than a bare instant; see
`ehrt.person-simulator.persona/initial-persona` for what it carries
and why.

## 3. Data shapes owned

- **`PersonEvent`** — the person-event vocabulary `persons` emits
  (residence move, employment change, coverage change,
  `:person-death`, birth, and their siblings). This brick is the
  authority for it; `sim-engine` reads `:person-death` off the stream
  through its own `person-deaths` fold but does not define the shape.
- The **person process's config keys** and its
  nineteen-variate-per-person-year draw block.

`Persona` itself is **`sim-model`'s** shape, not this brick's: this
component produces Personas to that definition.

## 4. Invariants guaranteed

- **Dependency direction.** The engine **consumes** the person stream;
  the person process knows nothing of encounters, beds, wards or
  messages. Limitations row 10 is the gate that makes
  "engine → person: none in v1" a **structural fact rather than a
  discipline** — and its reverse half is a bare token scan, which is
  why `sim-engine`'s own interface names this component's need without
  naming the component.
- **Data, not state.** `persons` returns a t-ascending vector; folding
  is the caller's.
- **One stream family.** Every draw comes from `:person`. This is what
  made arc 2b's corpus proof possible: the `:person` family has **zero
  draw sites in the engine**, so a component drawing only from it
  cannot move a byte of any existing corpus, and
  `bin/regression-oracle` reporting IDENTICAL with no declaration is
  **evidence rather than an absence of red**.
- **One payer pool set.** Coverage changes draw from the same
  `:payers-under-65` / `:payers-65-plus` pools a run supplies to
  `sim-model/persona` — the pools were promoted onto `sim-model`'s
  seam (arc 3a part 4) precisely so this component need not fork them.
- **Newborns are derived, not sampled**: four draws, not thirteen.

## 5. Non-goals

Authoritative list: `docs/limitations.md`, gated. In summary — and by
the mission sentence, the general rule is that anything that does not
change a message is out of scope:

- No commute, no job title, and no modelling of a life beyond what
  reaches a message.
- **No fold.** This component computes a stream; it never advances a
  world.
- **No knowledge of encounters, beds, wards or messages** — row 10
  makes that a gate, not a habit.

## 6. Forbidden edges

Requires exactly `sim-model` (for `Persona`, `places`, and the payer
pools) and `sim-engine`'s **stream-partition surface only** — `stream`
and `newborn-id-tag`.

Must never require:

- **`sim`**, **`sim-check`**, **`sim-emit-hl7`**, **`sim-emit-fhir`**,
  **`patient-simulator`** — none of them is on this brick's path, and
  each would breach the mission sentence's dependency direction.
- Anything from `sim-engine` **beyond the stream-partition surface**.
  The narrow edge is deliberate: it buys reproducible seeding without
  buying knowledge of the loop.

## Findings — the author's review queue

- **STALE DOCSTRING (found, not caused) — ruling F1's "lands ALONE"
  clause no longer describes the tree.** `interface.clj` still states:
  "The component lands ALONE: nothing in this workspace calls it, and
  nothing may until arc 3's fold." The call sites say otherwise —
  `components/sim/src/ehrt/sim/run.clj` requires this interface at
  `:36` and calls it three times: `initial-persona` at `:206`, and
  `persons` at `:212` and `:216` (the two-pass deaths fold).
  `sim-model`'s payer-pool promotion comment independently names
  `ehrt.sim.run/person-walk-config` as a live caller. This is not
  ambiguous: **arc 3's fold happened, and F1's sentence outlived its
  condition.** It is recorded here rather than fixed — the session's
  fence is docs-only, and `interface.clj` is a source file. The
  sentence is now the kind of prose a cold agent would reason from and
  get wrong, which is exactly what these charters exist to prevent.
- **UNCLEAR-N1 — who owns `PersonEvent`'s schema.** `sim-engine`
  exports `person-deaths`, which reads `:person-death` events off the
  stream, and `valid-persons?`, which decides what a well-formed
  `:persons` value is. So the *validator* for this brick's own output
  lives in the consumer. That may be correct (the engine validates its
  own input at its own entry, result-not-throw) or it may be the
  restatement hazard `ObservationEntry`'s own promotion exists to
  avoid. The shipped surface supports both readings.
