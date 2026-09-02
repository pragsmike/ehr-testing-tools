# Charter — `corpus`

> **Draft for the author's edit.** Derived from
> `src/ehrt/corpus/interface.clj` and the thirteen namespaces it
> delegates to, their own docstrings, and the ADRs those docstrings
> cite. **UNCLEAR** marks a contract the shipped surface does not
> settle.

## 1. Mission

Own the corpus domain: generate, mutate, intake, check, compare,
display and play synthetic clinical corpora — plus the operator and
generator **registries** those capabilities are parameterized by.

Successor to `ehrt.tools.interface` at tools split stage 3 (ADR-0018,
2026-07-31). **Designed from live consumers, not inherited** (AR-2):
every def has a named caller in `bases/cli`, `components/docs-tooling`
(lint's registry lookups), or a project test tree. The façade's 25
relay re-exports of kernel/judge/engine entries **dissolved** — their
consumers now require those interfaces directly, and this component no
longer requires any judge engine at all.

## 2. Interface contract

Defs marked **test-consumer only** are AR-2's ruled disposition —
*keep, but say so*. They are contract surface for the
conformance/integration lanes, not CLI wiring.

### Generation

- `generate!` — runs a generator engine to produce a corpus.
- `jdk-name` — the JDK this generator requires, by name.
- `jdk-version` — its required version.
- `resolve-java-bin` — locates the `java` executable for that JDK.
- `out-dir-exists?` — the `:out-dir-exists` guard. Shared by
  `generate!` and `ehrt.cli.core/generate-sim-command` (ADR-0015) **so
  every generator source's guard is the same check**.
- `out-dir-exists-error` — and the same `:hint` text with it.

### The generator registry

Symmetric noun prefixes are AR-2's sanctioned improvement — *names,
never signatures*. The old bare `lookup`/`entries`/`register!` meant
"operators" only because that registry won a name collision (ADR-0002).

- `generator-lookup` — a generator by id.
- `generator-register!` — register one. *Test-consumer only.*
- `generator-resolve-params` — resolve a generator's parameters.

### Generator sources

- `resolve-generator-source!` — executes a generator engine and yields
  a dir Source. Formerly bare `resolve!`, unqualified for the same
  historical reason; its spool twin moved to `corpus-io` at ADR-0017.
- `parse-source-designator` — the URL entry point whose **generator
  branch lives here rather than in `corpus-io`** (ADR-0017's own seam
  ruling), so that `corpus-io` never depends on this domain.

### Intake

- `intake!` — ingest an external corpus.
- `intake-via-source!` — ingest through a Source.
- `sniff-format` — detect a corpus's format.
- `valid-catalog-entry?` — *test-consumer only (conformance).*
- `valid-intake-record?` — *test-consumer only (conformance).*

### Mutation and operators

- `mutate` — apply an operator to a corpus.
- `operator-entries` — every registered operator.
- `operator-lookup` — one by id.
- `operator-register!` — *test-consumer only.*
- `operator-registry-snapshot` — *test-consumer only.*
- `operator-registry-reset!` — *test-consumer only.*

### Checking and comparison

- `check-corpus` — validate a corpus against the assertion catalog.
  Its own docstring documents the assertion shape (which is why
  `Assertion` was deleted with grep evidence rather than kept).
- `check-schemas-lookup` — the check-schema registry's lookup, needed
  from outside the component by `docs-tooling.lint`'s target-4
  (in-repo registry) verification.
- `compare-catalogs` — golden comparison. *Test-consumer only
  (integration).*

### Provenance

- `ManifestV1_1` — repointed to `provenance` directly (sim split B,
  M1 step 2), naming that dependency explicitly rather than relaying
  through `ehrt.corpus.manifest`. *Test-consumer only (conformance).*

### Display — `ehrt show` (ADR-0013)

**Pretty rendering for eyes, never wire format.**

- `render-er7-message` — one HL7 v2 message, readably.
- `render-er7-stream` — a stream of them.
- `render-fhir-json` — a FHIR bundle, readably.
- `split-er7-multi` — display's own input-adapter seam, **reused by
  `ehrt play`** (ADR-0014) rather than duplicated — *not a second
  splitter.*

### Player — `ehrt play` (ADR-0014)

**The pure pacing core — no clock, no IO.**

- `default-rate` — the default replay rate.
- `default-idle-cap-ms` — the cap on idle gaps.
- `plan` — messages → a pacing plan.
- `message-timestamp-ms` — a message's own timestamp.
- `event-timestamp-ms` — an event's.
- `message-type-trigger` — its `TYPE^TRIGGER`.
- `message-patient-id` — its patient id.
- `frame-event` — frame one event for emission.

### Board — `ehrt play --board` (ADR-0067)

The bed board's fold-and-render pair, **both pure**; the board sink in
`bases/cli` is the one named caller.

- `board-fold-event` — accumulator × event → accumulator.
- `board-render-snapshot` — accumulator → a rendered board.

### The sim adapter — in-process since 2026-07-28 (ADR-0005)

Renamed from `tools.sim` (AR-1: the old name collided confusingly with
the `sim` component itself).

- `sim-run!` — mounts `ehrt sim run`.
- `sim-check!` — mounts `ehrt sim check`. *P3-6 parity mount.*
- `sim-identifiers!` — mounts `ehrt sim identifiers`. *P3-6.*
- `sim-version!` — mounts `ehrt sim version`. *P3-6.*

## 3. Data shapes owned

- The **operator registry** and the **generator registry** — their
  entries, and the lookup/register contract over them.
- The **check-schema registry** (`check.schemas`).
- The **catalog entry** and **intake record**.
- The **pacing plan** the player produces, and the **board
  accumulator**.
- The **corpus** itself: what intake produces and check validates.

`ManifestV1_1` is **`provenance`'s** shape, re-exported here, not
owned. The **assertion** shape is documented in `check-corpus`'s
docstring rather than as an exported schema.

## 4. Invariants guaranteed

- **One guard, one hint.** `out-dir-exists?` and
  `out-dir-exists-error` are shared with the CLI so every generator
  source's `:out-dir-exists` check and message are identical.
- **One splitter.** `split-er7-multi` serves both `show` and `play`.
- **Purity where it is claimed.** The player's pacing core has no
  clock and no IO; the board's fold and render are both pure. Time and
  writing belong to the caller.
- **Display is never wire format.** Rendered output is for eyes and
  must not be fed back as a corpus.
- **The façade is stable for its consumers.** `corpus` depends on
  `ehrt.sim.interface`'s stability (ADR-0012); `docs-tooling` depends
  on this one's registry lookups.
- **Registry lookups fire their load-time registrations transitively**
  when the interface loads — the same discipline `docs-tooling.lint`
  relies on for framing and check-schemas.

## 5. Non-goals

- **No transport, no IO codecs.** Sources, sinks, spooling, framing
  and the ER7 delimiter grammar are `corpus-io`'s. This component
  *implements or consumes* those protocols and constructors.
- **No judging.** This component **no longer requires any judge engine
  at all**; consumers reach the engines' own interfaces directly.
- **Not exported, deliberately:** `diff`, `lineage`, and the
  check/mutate internals (component-internal); `operators-doc` (a
  Makefile `-X` entry point, the same rule `docs-tooling`'s own
  `-X`-invokables follow). `Assertion` was **deleted**, with grep
  evidence of zero live consumers.
- **Does not simulate.** The sim adapter mounts `sim`'s capabilities
  in-process; it does not reimplement them.

## 6. Forbidden edges

Requires, in `src`: `corpus-io`, `judge`, `kernel`, `provenance`,
`sim`, `sim-emit-hl7`.

Must never require:

- **any judge engine** — `judge-v2-hapi`, `judge-v2-nist`,
  `judge-fhir-official`. Stage 3 dissolved those relays deliberately,
  and the docstring records that this component "no longer requires
  any judge engine at all". Re-adding one would undo ADR-0018.
- **`docs-tooling`** — the edge runs `docs-tooling → corpus`; the
  reverse is a cycle.
- **`bases/cli`** — bases depend on components, never the reverse.
- **`sim-engine`, `sim-model`, `patient-simulator`,
  `person-simulator`, `sim-check`** — the simulator is reached only
  through the `sim` façade, which is precisely the stability ADR-0012
  buys.

## UNCLEAR — the author's review queue

- **UNCLEAR-C1 — the docstring's def count has drifted.** The ns
  docstring says "The former `ehrt.tools.interface`'s 64 defs became
  **38** here". The file now carries **44**. The sentence is a *dated,
  historically true* statement about what stage 3 landed, and the
  growth since is fully accounted for and individually annotated in
  the file (`board-*`, ADR-0067; the three P3-6 parity mounts;
  `mutate`). But its tense reads as present, and a cold agent
  reasoning from the docstring would get today's number wrong. Two
  readings: *(a)* leave it — it describes an event, not a state, and
  every addition is annotated; *(b)* re-word to name stage 3
  explicitly, or drop the number. Not fixed here: `interface.clj` is
  source, and this session's fence is docs-only.
- **UNCLEAR-C2 — `mutate` is the one unannotated capability.** Every
  other section of this interface carries a comment naming its ADR,
  its caller, or its disposition. `mutate` has a bare `;; ---- mutate
  ----` header and one def. Given ADR-0176 landed an event-mutation
  spine and `ehrt sim mutate` after this interface was written,
  whether `corpus/mutate` is the corpus-side twin of that work, its
  predecessor, or unrelated is not answerable from this seam.
- **UNCLEAR-C3 — this brick has eight capability families under one
  interface.** Generate, intake, mutate/operators, check, compare,
  display, play/board, and the sim adapter each have their own ADR,
  their own namespaces, and largely disjoint consumers. That is an
  observation the charter format made visible, not a proposal; see
  the session record for the seam described and priced.
