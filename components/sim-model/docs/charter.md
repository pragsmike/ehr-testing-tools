# Charter — `sim-model`

> **Draft for the author's edit.** Derived from
> `src/ehrt/sim_model/interface.clj` and the four namespaces it
> delegates to (`pathway`, `facility`, `persona`, `config`), their own
> docstrings, and the ADRs those docstrings cite. **UNCLEAR** marks a
> contract the shipped surface does not settle.

## 1. Mission

Own the *nouns* of the simulated world — pathways, facilities and
their beds, personas, and the run-config profiles — as schemas and
seeded samplers, with no dependency on any other brick and no
knowledge of time, encounters, or messages.

Extracted from `components/sim` at sim split S1
(`.agents/plans/2026-08-02-sim-split-plan.md`, AR-6). The four
namespaces have **no cross-namespace dependency among themselves**;
this interface is the only path residual `sim` — and, from split S2 on,
`patient-simulator` — may reach them through.

## 2. Interface contract

Sized by grep against `components/sim`'s own src and test trees before
the move, not by interface-design judgment (R5, ADR-0001).

### Pathways (`pathway.clj`)

- `Concept` — a coded clinical or administrative concept; `:system` is
  a keyword naming the code system (`:snomed :loinc :rxnorm :icd10cm
  :cvx` …).
- `ConditionAnnotation` — M5b: a ConditionOnset/ConditionEnd
  trajectory event compiles to an **annotation on its enclosing
  Encounter-mapped step**, never a standalone IR step of its own.
- `ObservationEntry` — the value/unit/codes/category/value-code/
  reference-range/interpretation shape a compiled Observation-family
  event carries (ADR-0029 P1/P2, Wave D stage D1a schema ruling).
  Exported by the event-log contract arc (2026-08-16) so
  `ehrt.sim-engine.event-schema` **reuses this one definition** for a
  `:diagnostic-report`'s `:observations` children rather than
  restating the shape — a restatement would validate against itself
  and agree with its own mistake.
- `Citation` — the `{:module :state}` back-reference a
  CompileTrajectory-produced IR step carries, riding through from the
  trajectory event it came from (M5b, provenance obligation 3).
- `Step` — the IR step schema, a `[:multi {:dispatch :type}]` over
  `:admission`, `:discharge`, and its siblings.
- `PathwaysConfig` — a vector of `PathwayAssignment`: each entry is
  **either** a weighted-pool member (`{:pathway :weight}`, a sampled
  mixture across the population) **or** an explicit per-patient
  override (`{:patient-ordinal …}`).
- `sample-admission-discharge` — the walking-skeleton pathway: admit,
  dwell, discharge.
- `valid?` — `(valid? pathway)` → true when it conforms to `Pathway`.
  This is `pathway/valid?`; see UNCLEAR-M2 on the bare name.
- `valid-pathways-config?` — `(valid-pathways-config? config)`.

### Facility and beds (`facility.clj`)

- `ward-by-name` — `(ward-by-name facility ward-name)` → the ward map.
- `licensed-bed-ids` — **derived, never enumerated**: ward tag plus a
  2-digit index, so `:renal` with `:beds 3` derives
  `["RENAL-01" "RENAL-02" "RENAL-03"]`.
- `surge-slot-ids` — derived using the ward's own `:surge-format`;
  surge naming is site-idiosyncratic config, not code.
- `occupancy-board` — the derived index `bed-id -> patient-id`, folded
  from patient states. **This IS the consistency law stated as code**:
  recomputing it from `patients` is the definition, not a cache.
- `ward-census` — a snapshot of every ward's occupancy against its
  declared capacity (licensed + surge), keyed by ward name — the
  diagnostic payload a capacity-exhausted outcome carries.
- `allocate` — the four-rung allocation ladder, seeded within each
  rung. Optional `force-placement` (`{:ward :bed}`) overrides the
  ladder outright and **draws no RNG**. Arities:
  `(allocate rng facility board home-ward-name force-placement)` and
  the 6-arity taking an explicit `beds` index.
- `free` — `(free ids board beds)` → the candidate ids **available**
  to allocate. Promoted to the seam by ARC 3B sweep 2 (ADR-0174 §2(c))
  because the `:ready` gate must be **one** predicate across three
  namespaces — sim-model's ladder, `sim-engine`'s `bed-ready-location`,
  and `sim-check`'s `earlier-rungs-exhausted?` — and a same-looking
  copy in any of them is the drift the promotion exists to prevent.
- `initial-beds` — the bed-status index a `:bed-cycle` run starts
  from: every licensed bed and every surge slot the facility declares,
  born `:ready` at t 0. `bed-id -> {:status :since-t :last-patient-id}`.
- `ward-of-bed` — the ward **name** owning `bed`, or nil. Derived from
  the same two id functions the index is built from, so a bed id and
  its ward can never disagree.
- `bed-placement` — which ladder rung a bed belongs to, `:licensed` or
  `:surge`, found by searching the facility's own derived id lists
  **rather than by parsing the id string**. nil for a bed no ward
  declares.
- `choose-attending` — `(choose-attending rng providers ward-id)` →
  the provider id, a seeded uniform sample among providers eligible
  for that ward.

### Personas (`persona.clj`)

- `Persona` — the sampled person shape.
- `valid-persona?` — `(valid-persona? p)`.
- `persona` — `(persona rng config)` → a seeded Persona.
- `under-65-payers` — the weighted payer pool sampled for patients
  younger than 65: Medicare present but minor, commercial dominant.
- `sixty-five-plus-payers` — the 65-and-over counterpart. Both were
  promoted to the seam on 2026-08-26 (arc 3a part 4) to close a real
  gap: `ehrt.person-simulator.process` draws a coverage change from
  *the same* `:payers-under-65` / `:payers-65-plus` keys, and a run
  supplying neither got no `:coverage-change` events at all — making a
  declared 1.3.0 event kind unreachable from any config that did not
  restate the pools. **Exposing the real pools removes a fork instead
  of creating one**: `ehrt.sim.run/person-walk-config` defaults to
  these, so a run's people and its patients draw from one pool set by
  construction.
- `reference-today-epoch-day` — `(reference-today-epoch-day)` → the
  run's reference day as an epoch day.

### Run-config profiles (`config.clj`)

- `default-facility` — the built-in facility a run uses absent config.
- `turnaround-minutes` — `(turnaround-minutes ward)` → the ward's own
  `:turnaround-minutes` or its **class default**. The ONE reading of
  that key (ADR-0174 ruling D1): `ehrt.sim-engine.engine`'s cycle draws
  through this and never through `get` directly.
- `default-provider-templates` — the built-in provider templates.
- `materialize-providers` — `(materialize-providers rng templates)` →
  templates with `:id` filled in. **The one place provider NPIs are
  generated**: one synthetic Luhn-valid NPI per template, in template
  order (fixed — determinism), called once per run.

The five ARC-4 profile families each ship schema + validator +
explainer, and each is on this seam **for the same fail-fast reason**:
`ehrt.sim.run` validates the config *before* the engine and its RNG
ever start — the same posture a missing `--seed` already gets.

- `ChatterProfile` — three event-driven keys are **rates in [0,1]**;
  an absent key means that kind produces no restatement.
- `valid-chatter-profile?` — `(valid-chatter-profile? profile)`.
- `explain-chatter-profile` — the malli explanation, for the error.
- `ChargesProfile` — `:price-table` maps a **code string** to its
  price, using codes the log already carries.
- `valid-charges-profile?` — as above.
- `explain-charges-profile` — as above.
- `LadderProfile` — `:rungs` are ORU^R01 result-status restatements
  (OBR-25 + OBX-11); `:order-rungs` are ORM^O01 order-status
  restatements (ORC-5). Each is a vector of fractions **strictly
  between 0 and 1**.
- `valid-ladder-profile?` — as above.
- `explain-ladder-profile` — as above.
- `SiuProfile` — an on/off with an optional allow-list, and nothing
  else; `:siu {}` renders all four message kinds.
- `valid-siu-profile?` — as above.
- `explain-siu-profile` — as above.
- `FanOutProfile` — the subscriber table: a non-empty vector of
  subscribers with **distinct `:name`s**, because two sharing a name
  would write two spools into one directory.
- `valid-fan-out-profile?` — as above.
- `explain-fan-out-profile` — as above.

## 3. Data shapes owned

Authority for: `Ward`, `Facility`, `Provider`, `ProviderName`,
`ProviderTemplate`, `Payer`, `Persona`, `Pathway`, `Step`,
`PathwayAssignment`, `PathwaysConfig`, `Concept`, `Citation`,
`ConditionAnnotation`, `ObservationEntry`, `ForcePlacement`,
`LatencyRange`, `LatencyProfile`, and the five ARC-4 profiles above.

Not all of them are on the seam — see §5 and UNCLEAR-M1.

## 4. Invariants guaranteed

- **Bed ids are derived, never enumerated.** `licensed-bed-ids` and
  `surge-slot-ids` compute from ward config; `ward-of-bed` and
  `bed-placement` search those same lists, so an id and its ward
  cannot disagree, and no code parses a bed-id string.
- **The occupancy board is a definition, not a cache.**
- **One reading of `:turnaround-minutes`**, via `turnaround-minutes`.
- **One `:ready` predicate**, via `free`, shared by three namespaces.
- **Fixed-draw determinism.** Seeded choice consumes exactly one RNG
  draw regardless of candidate count; `materialize-providers` draws in
  fixed template order; `force-placement` draws **no** RNG at all.
- **One payer pool set per run**, since `persona`'s own defaults are
  the exported pools.
- **`ObservationEntry` has one definition**, reused by
  `sim-engine.event-schema` rather than restated.

## 5. Non-goals

- **No time, no encounters, no beds-in-motion, no messages.** This
  brick samples and validates nouns; the engine moves them.
- **Not a full re-export.** `Pathway`, `PathwayAssignment`,
  `ForcePlacement`, `Facility`, `Ward`, `Provider`, `Payer`, and the
  `explain-*` counterparts for pathway and facility are public in
  their own namespaces but deliberately absent from the seam.
- **`LatencyProfile` is deliberately not on the seam**, schema or
  validators — stated in `interface.clj`: unlike the five ARC-4
  families, **nothing ever called** its validators, because latency is
  applied at emission rather than checked before the RNG starts.

## 6. Forbidden edges

`sim-model` requires **no other brick**. It is, with `kernel`,
`palgebra` and `provenance`, one of the workspace's root bricks.
In particular it must never require `sim`, `sim-engine`, or
`patient-simulator` — all three depend on it, and any reverse edge
would be a cycle.

## UNCLEAR — the author's review queue

- **UNCLEAR-M1 — `Facility`'s validator has no production caller.**
  `valid-facility?` and `explain-facility` are defined in `config.clj`
  and referenced **only by `components/sim-model/test/ehrt/sim_model/
  config_test.clj`** (verified by grep across every `.clj` in the
  workspace). They are not on the interface. Yet a facility **is**
  user-supplied config — `ehrt sim check --config` threads a facility
  through — and the five ARC-4 profile families were each put on this
  seam expressly so a malformed value fails fast before the RNG
  starts. Two readings: *(a)* deliberate — the facility is validated
  elsewhere, or is trusted because it is structural rather than
  numeric, so a validator is test-only equipment; *(b)* a gap of
  exactly the kind ARC-4 sweep 2 closed for `:chatter`, not yet
  noticed for `:facility`. The shipped surface does not say which,
  and this charter does not guess.
- **UNCLEAR-M2 — `valid?` at the seam.** Exported `valid?` is
  `pathway/valid?`, but this component also defines `valid-facility?`,
  `valid-persona?`, `valid-provider?`, `valid-npi?` and six
  `valid-*-profile?`. The one that gets the bare name is the pathway
  one, which a reader at the seam cannot tell without opening
  `interface.clj`. (`kernel` has the same shape — its bare `valid?` is
  `result/valid?`. Worth deciding once, for both.)
