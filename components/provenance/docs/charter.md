# Charter — `provenance`

> **Draft for the author's edit.** Derived from
> `src/ehrt/provenance/interface.clj` and `manifest.clj`, their own
> docstrings, and the ADRs those docstrings cite. **UNCLEAR** marks a
> contract the shipped surface does not settle.

## 1. Mission

Own the **provenance-manifest schema family** — `ManifestV0`,
`ManifestV1`, `ManifestV1_1` and their validators — as the single
acyclic home two producers can both validate against.

Sim split B, M1 (2026-08-04,
`.agents/plans/2026-08-04-sim-split-b-plan.md` AR-2). Moved **verbatim**
out of `ehrt.corpus.manifest`.

**Why this brick exists at all**, in one sentence from its own seam:
`corpus → sim` already exists (`ehrt.corpus.sim-adapter` requires the
sim façade, ADR-0012), so `sim → corpus` for this schema would be a
**cycle**; `provenance` — depended on by both and depending on neither
— is **the only acyclic single home.**

Named `provenance` rather than bare `manifest` to avoid
three-things-called-manifest ambiguity during the migration, and to
leave room for the family to grow (e.g. `corpus-io`'s own operation
manifest — *noted, not proposed*, by that plan).

## 2. Interface contract

### Schema v0 — EXP-A4's working hypothesis; **frozen historical record**

- `ManifestV0` — the v0 schema.
- `valid?` — `(valid? m)` against v0. The bare name is v0's; see
  UNCLEAR-PV1.

### Schema v1 — EXP-A4's correction: adds `:reference-date`; **frozen**

- `ManifestV1` — the v1 schema.
- `valid-v1?` — `(valid-v1? m)`.

### Schema v1.1 — P4's upgrade: `:stage`, `:seeds`, `:engine-params`, `:runtime`

- `ManifestV1_1` — the v1.1 schema.
- `valid-v1-1?` — `(valid-v1-1? m)`.

## 3. Data shapes owned

The three manifest versions, and **only** those. This brick is the
authority for what a provenance manifest is at each version, for both
producers.

## 4. Invariants guaranteed

- **One definition, two producers.** `corpus` and `sim` each validate
  their own output against *these* schemas. Neither holds a copy.
  `ehrt.sim.manifest`'s mirror was **retired entirely** rather than
  kept in sync, for the stated reason that a copy validates against
  itself and agrees with its own mistake.
- **Acyclicity by construction.** This brick depends on nothing, which
  is the whole property that let it break the `corpus`/`sim` cycle.
- **v0 and v1 are frozen.** They are a historical record, not a live
  target; only v1.1 grows.
- **Builders stay producer-side, deliberately.** `corpus` keeps
  `build`/`build-v1-1` (`ehrt.corpus.manifest`); `sim` keeps `build`
  (`ehrt.sim.manifest`). **This component knows nothing about either
  producer.**

## 5. Non-goals

- **Builds nothing.** No constructor is exported, and that is the
  point: a builder would have to know its producer's world, and this
  brick deliberately knows neither.
- **Stamps nothing.** `sim` stamps `:event-schema-version` and
  `:stream-scheme` into a run manifest; this brick only says what a
  well-formed manifest is.
- **Does not own `corpus-io`'s `OperationManifestV1`** — a sibling
  lineage sidecar that lives in `corpus-io`. Absorbing it was *noted,
  not proposed*.
- **Migrates nothing between versions.** No v0→v1 or v1→v1.1 upgrade
  function is exported.

## 6. Forbidden edges

Requires **no other brick** — with `kernel`, `sim-model` and
`palgebra`, one of the workspace's root bricks.

Must never require:

- **`corpus`** or **`sim`** — either edge re-creates the exact cycle
  this brick was carved to break. This is the strictest forbidden
  edge in the workspace, because the prohibition *is* the brick's
  reason for existing.
- Anything else. A dependency here would compromise the acyclicity
  that makes it a valid shared home.

## UNCLEAR — the author's review queue

- **UNCLEAR-PV1 — the bare `valid?` names the frozen version.** Of
  the three validators, the one with the unqualified name is
  `valid?` = v0's — the **oldest and explicitly frozen** schema, while
  the live one (v1.1) carries the longest name, `valid-v1-1?`. Two
  readings: *(a)* historical and harmless — `valid?` was the only
  validator when the family had one member, and the names are
  additive, so renaming would break callers for cosmetics; *(b)* a
  live trap — a caller reaching for the obvious name gets the frozen
  historical schema, and would see a v1.1 manifest fail validation
  against v0 without any hint that they picked the wrong version.
  Which risk is real depends on whether v0 is still permissive enough
  to pass a v1.1 manifest, which this seam does not say.
- **UNCLEAR-PV2 — growth was left room but no rule.** The seam says
  the family may grow, and names `corpus-io`'s operation manifest as a
  candidate, explicitly *noted, not proposed*. What it does not say is
  what would make a schema belong here rather than with its producer.
  The v0/v1/v1.1 family qualified because **two** producers needed one
  definition; `OperationManifestV1` today has one. If that is the
  rule, it is worth stating, because it also answers when a future
  schema should *not* move.
