# Charter — `palgebra`

> **Draft for the author's edit.** Derived from
> `src/ehrt/palgebra/interface.clj`, `lint.clj`, `signature.clj`,
> `components/corpus/docs/palgebra-design.md`, and the ADRs those
> docstrings cite. **UNCLEAR** marks a contract the shipped surface
> does not settle.

## 1. Mission

Own the **pipeline algebra**: the signature language in which a
pipeline stage declares what it consumes and produces, and the lint
that checks a declared pipeline against it.

## 2. Interface contract

**Deliberately wide** (H2 landing session ruling R13/R14, ADR-0002),
and it exists at all for a structural reason worth keeping: R13's own
target layout named **no `interface.clj`**, but Polylith's
dependency-direction enforcement requires one the moment `palgebra`
becomes its own component — a brick that reaches into another brick's
*implementation* namespace fails `poly check`. `lint.clj` and
`pipeline.clj` (now in `docs-tooling`) both genuinely require
`palgebra`: two real `:require` sites, confirmed by grep against the
pre-carve repo, **not a hypothetical**. It re-exports exactly what
`docs-tooling` calls.

### Lint

- `lint` — check a pipeline against its stages' declared signatures.
- `stages-catalytic-resources` — the catalytic resources a stage set
  declares.
- `lines-catalytic-resources` — the same, read per line.

### Signatures

- `read-signature-edn` — read a `signature.edn` into stage data.
- `stage-schema` — the schema one stage's signature must satisfy.
- `pipeline-schema` — the schema a whole pipeline must satisfy.
- `UnionResource` — a resource that is one of several alternatives.
- `ExternalStage` — a stage supplied from outside the pipeline.
- `valid-stage?` — `(valid-stage? s)`.
- `valid-union-resource?` — `(valid-union-resource? r)`.
- `valid-external-stage?` — `(valid-external-stage? s)`.
- `valid?` — the pipeline-level validator; see UNCLEAR-PA1.

## 3. Data shapes owned

- The **stage signature** — what a stage consumes, produces, and
  declares catalytic.
- The **pipeline** — a composed sequence of stages.
- The two non-plain resource forms — a union resource, and a stage
  supplied from outside the pipeline.
- The distinction between **consumed** and **catalytic** resources,
  which is what the lint is checking.

## 4. Invariants guaranteed

- **A pipeline is checkable before it is run.** `lint` is a static
  check over declared signatures; it does not execute a stage.
- **Catalytic resources are declared, not inferred.** A stage says
  which resources it needs present but does not consume, and the lint
  holds it to that.
- **The interface exists to satisfy dependency direction**, not to
  express a design ideal — its width is the consequence of R13/R14
  plus `poly check`, and is documented as such.

## 5. Non-goals

- **Runs no pipeline.** It reads, validates and lints declarations;
  execution belongs to whatever the pipeline describes.
- **Knows nothing of corpora, judges or the simulator.** It is a
  general algebra over stage signatures.
- **Does not own the pipeline documents.** `docs-tooling` reads and
  renders them; this brick supplies the grammar and the check.

## 6. Forbidden edges

Requires **no other brick** — with `kernel`, `sim-model` and
`provenance`, one of the workspace's four root bricks.

Must never require:

- **`docs-tooling`** — the edge runs `docs-tooling → palgebra`, which
  is the pair of `:require` sites this interface was created for. The
  reverse is a cycle.
- **`corpus`** — note that `components/corpus/docs/palgebra-design.md`
  is where this algebra's design is written up, which makes a
  documentation edge look like a dependency; it is not one, and must
  not become one. See UNCLEAR-PA2.
- **`kernel`** — not forbidden by any rule, but worth noting as a
  fact: this brick does **not** use the result envelope.

## UNCLEAR — the author's review queue

- **UNCLEAR-PA1 — the bare `valid?` and the missing
  `valid-pipeline?`.** The seam carries `valid-stage?`,
  `valid-union-resource?`, `valid-external-stage?` — three qualified
  validators — and then a bare `valid?`, alongside both `stage-schema`
  and `pipeline-schema`. By elimination the bare one is the
  pipeline-level validator, but the naming is the reverse of the
  pattern its three siblings establish, and nothing on the seam says
  so. (Third instance of this shape, with `kernel/valid?` and
  `sim-model/valid?`; `provenance/valid?` is a fourth. It may be worth
  one ruling rather than four.)
- **UNCLEAR-PA2 — this brick's design document lives in another
  brick.** `palgebra`'s design write-up is
  `components/corpus/docs/palgebra-design.md`, a residue of the
  pre-carve layout where the algebra lived inside the corpus domain.
  The code moved; the document did not. Two readings: *(a)* the
  document is genuinely about corpus pipelines, and palgebra is the
  extracted mechanism, so it sits correctly; *(b)* it is stale
  placement, and a reader looking for palgebra's rationale will not
  find it under `components/palgebra/docs/` — which, before this
  session, did not exist at all.
