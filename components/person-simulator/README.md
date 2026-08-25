# person-simulator

**The person process exists so that demographic and identity traffic is
realistic; a person's life is relevant only inasmuch as it changes a
message.**

Chartered by [ADR-0172](../../notes/adr/0172-person-simulator-charter.md)
(Accepted 2026-08-25, rulings A1 B1 C1 D1 E1 F1 G1) and implemented by
arc 2b. Sibling in shape to `patient-simulator`: a mission sentence at
the front door, a dependency-direction paragraph, and one table row per
limitation declined ON PURPOSE -- each with the gate that goes red if
the decline is silently lifted.

## The front door

```clojure
(persons config stream)                  ; -> [PersonEvent], t-ordered
(initial-persona person-id t0)           ; -> Persona   (the t0 state)
(initial-persona person-id t0 birth-ctx) ; -> Persona   (a newborn, ruling A1)
```

`persons` returns **data, never state**: a t-ascending vector of person
events the engine folds. This component folds nothing.
`initial-persona` is the t0 construction that replaces the engine's own
`(sim-model/persona rng ...)` call, and in v1 it IS that call -- which
is why a wired consumer that reads no events is byte-identical to
today.

## Dependency direction

The engine CONSUMES the person stream; the person process knows nothing
of encounters, beds, wards or messages. Dependencies are `sim-model`
(`Persona`, `places`, the payer pools) and `sim-engine`'s
stream-partition surface for `stream` / `newborn-id-tag` ONLY. No
`sim-engine` namespace requires this component. Limitations row 10 is
the gate.

Every draw comes from `(engine/stream master :person id-tag)` and from
no other stream family. That is what makes this component
corpus-neutral: the `:person` family has zero draw sites in the engine,
so a component drawing only from it cannot move a byte of any existing
corpus.

## Scope and declared limitations

The eleven gaps declined on purpose, each with its citation and its
gate, are in [`docs/limitations.md`](docs/limitations.md). That table is
gated by `ehrt.docs-tooling.person-simulator-charter-test`: the mission
sentence must occur verbatim here, in the charter and in
`interface.clj`'s own SCOPE section; every row must be mirrored in
ADR-0172 section 4; every citation must resolve and anchor exactly one
place; and every deliberate-limitation marker in this component's `src`
must be covered by a row. A new marker with no row is red.
