(ns ehrt.person-simulator.interface
  "SCOPE (author rulings 2026-08-25, ADR-0172). The person process
  exists so that demographic and identity traffic is realistic; a
  person's life is relevant only inasmuch as it changes a message.
  That sentence settles arguments about scope before they start: it is
  why this component models a residence move (one A08 and a changed
  PID-11) and does not model a commute; why it models an employment
  change (a coverage change, an IN1, and an occupational-injury
  hazard) and does not model a job title. The gaps this component
  declined ON PURPOSE, each with its citation and the gate that goes
  red if the decline is silently lifted, are tabled in
  `docs/limitations.md`; that table is gated
  (`ehrt.docs-tooling.person-simulator-charter-test`), so a new
  deliberate limitation in this src that is not tabled there is red.

  DEPENDENCY DIRECTION. The engine CONSUMES the person stream; the
  person process knows nothing of encounters, beds, wards or messages.
  It depends on `sim-model` (`Persona`, `places`, the payer pools) and
  on `sim-engine`'s stream-partition surface for `stream` /
  `newborn-id-tag` ONLY -- limitations row 10 is the gate that makes
  \"engine -> person: none in v1\" a structural fact rather than a
  discipline.

  THE FRONT DOOR, exactly as ADR-0172 section 2 states it:

    (persons config stream)                  ; -> [PersonEvent], t-ordered
    (initial-persona person-id t0)           ; -> Persona   (the t0 state)
    (initial-persona person-id t0 birth-ctx) ; -> Persona   (a newborn, ruling A1)

  `persons` takes the run's `:person`-family stream descriptor and a
  config and returns a timed, t-ascending vector of events -- DATA,
  never state. The engine folds them; this component folds nothing.
  `initial-persona` is the replacement for the `(sim-model/persona
  rng ...)` call at `ehrt.sim-engine.engine`'s own `:registered`
  decide method, and in v1 it IS that call.

  RULING F1. The component lands ALONE: nothing in this workspace
  calls it, and nothing may until arc 3's fold. That is what made arc
  2b's corpus proof possible -- the `:person` stream family has zero
  draw sites in the engine, so a component drawing only from it cannot
  move a byte of any existing corpus, and `bin/regression-oracle`
  reporting IDENTICAL with no declaration is evidence rather than an
  absence of red.

  SKELETON (arc 2b step 1). No behaviour yet: every var below throws
  `not-implemented`. Step 2's limitation tests are born RED against
  exactly this, for exactly one reason, which is the only kind of red
  worth capturing."
  (:require [ehrt.person-simulator.not-implemented :as ni]))

(defn persons
  "The run's person-event stream: a t-ascending vector of person
  events, drawn entirely from the `:person` stream family."
  [config stream]
  (ni/not-implemented `persons {:config config :stream stream}))

(defn initial-persona
  "The t0 Persona for one person. The 3-arity is ruling A1's newborn
  path: `birth-ctx` carries what the household determines, so a
  newborn's Persona is DERIVED rather than sampled -- four draws, not
  thirteen."
  ([person-id t0]
   (ni/not-implemented `initial-persona {:person-id person-id :t0 t0}))
  ([person-id t0 birth-ctx]
   (ni/not-implemented `initial-persona
                       {:person-id person-id :t0 t0 :birth-ctx birth-ctx})))
