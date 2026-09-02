# Charter — `corpus-io`

> **Draft for the author's edit.** Derived from
> `src/ehrt/corpus_io/interface.clj` and the eleven namespaces it
> delegates to, their own docstrings, and the ADRs those docstrings
> cite. **UNCLEAR** marks a contract the shipped surface does not
> settle.

## 1. Mission

Own the **transport and IO seam** of the former corpus
mega-component: sources, sinks, spooling, framing codecs, and
wire-level wrappers (the v2 ER7 delimiter grammar, operation-manifest
lineage) — **and no domain logic at all**.

ADR-0017 stage 2, 2026-07-31. Deliberately thin per that stage's own
author ruling (AR-1): it exports exactly what the domain's namespaces
and `bases/cli` call from outside this component, **not an
interface-design ideal**.

## 2. Interface contract

### Framing codecs

- `decode` — wire bytes → messages, per the registered framing.
- `encode` — messages → wire bytes.
- `lookup` — a framing by id. (The registry lookup
  `docs-tooling.lint` depends on.)

### ER7 — the v2 mutation substrate (P7)

The domain's mutate/operators logic stayed in `corpus`; **this is only
the delimiter-split codec they call.**

- `parse` — ER7 text → a parsed message.
- `serialize` — back to ER7 text.
- `content-hash` — a message's content digest.
- `field-index` — index into a segment's fields.
- `segment-occurrence-index` — index across repeated segments.
- `resolve-locator` — resolve a `kernel` v2 `Locator` against a
  parsed message.

### ER7 field readers (ADR-0111)

A **move-don't-improve** micro-relocation: moved down from
`ehrt.corpus.player`, which re-exports these same names unchanged.
`partition-messages` is the in-component caller the move exists for.

- `parse-dtm-lenient` — a lenient HL7 DTM parse.
- `message-timestamp-ms` — a message's timestamp, in ms.
- `message-type-trigger` — its `TYPE^TRIGGER`.
- `message-patient-id` — its patient id.
- `message-control-id` — its MSH-10.
- `segment-field-of` — a named segment's field.

### Batching (ADR-0111)

- `partition-messages` — the corpus batcher's own **pure** partition
  function: messages → epoch-aligned, schedule-partitioned buckets.

### Spooling

- `spool-resolve!` — resolves a spool Source. Kept qualified for
  continuity even though the collision that forced it (with
  `corpus.generator-source/resolve!`, ADR-0002) **dissolved this
  stage**: that twin now lives in a different component entirely.

### Sources and sinks

- `default-framing` — the framing a source or sink assumes absent one.
- `implemented-source-kinds` — the closed set of source kinds.
- `implemented-sink-kinds` — the closed set of sink kinds.
- `dir-source` — a directory as a Source.
- `file-source` — a file as a Source.
- `stdin-source` — stdin as a Source.
- `dir-sink` — a directory as a Sink.
- `file-sink` — a file as a Sink.
- `stdout-sink` — stdout as a Sink.
- `mllp-sink` — an MLLP endpoint as a Sink.

### Designator grammar

- `parse-designator` — **the shared parse skeleton.** Public and
  re-exported here specifically so `ehrt.corpus.generator-source` can
  supply its own domain-aware `finish` callback **without this
  component ever depending on the domain.** This is the mechanism that
  made the directional rule in §4 keepable.
- `source-schemes` — the recognized source URL schemes.
- `parse-sink-designator` — the sink-side parse.
- `path-designator->path` — a designator → a filesystem path.
- `print-source-designator` — the inverse of parsing. No domain edge
  (only `:dir`/`:file` are printable, SS-1/SS-2) but a real cross-brick
  caller: `corpus.generator-source-test`'s round-trip property pairs
  it with that namespace's `parse-source-designator`.

### Writing

- `write-dir!` — write a corpus to a directory sink.
- `write-stdout!` — write it to stdout.

### Operation manifest — the sink-write lineage sidecar

Moved here from the domain because it has **no domain edges of its
own**, and `sink-write` — its most demanding consumer — is transport,
not domain. `ehrt.corpus.intake`'s manifest-sidecar recognizer is the
one domain consumer, repointed here per AR-4.

- `OperationManifestV1` — the sidecar schema.
- `operation-manifest-valid?` — its validator.

### Canonicalizers

Real cross-brick caller: `projects/integration`'s
zero-flag-reproducibility test. The `kernel/register!` load-time side
effect fires **transitively the moment this interface loads** — the
same discipline `docs-tooling.lint` depends on for framing.

- `strip-run-timestamp-suffix` — removes a run's timestamp suffix.
- `strip-synthea-run-metadata` — removes Synthea run metadata.

### MLLP (ADR-0175 design (g), arc 4 sweep 5)

- `mllp-open-sink!` — opens the MLLP socket sink.
  `ehrt play --sink mllp://host:port` is its one caller.
- `mllp-ack-server!` — the loopback ACK responder. **Test/demo
  apparatus**: an ACK is *received*, never emitted by this project's
  generator, and the responder is what a loopback round trip needs.
- `mllp-ack-codes` — the ACK code vocabulary.
- `mllp-default-ack-timeout-ms` — the default ACK timeout.

## 3. Data shapes owned

- **Source** and **Sink** — the protocols, and every constructor for
  them listed above.
- The **designator grammar** — the URL forms a source or sink is named
  by, and the `parse-designator` skeleton that keeps it domain-free.
- The **framing registry** and its codecs.
- The **parsed ER7 message** — the delimiter-split representation the
  domain's operators mutate.
- The **operation manifest** — the sink-write lineage sidecar, whose
  schema this brick carries.
- The **MLLP** wire framing and ACK vocabulary.

## 4. Invariants guaranteed

- **The directional rule (AR-2) — the one that matters more than the
  file list.** This component may **never** require `ehrt.corpus.*`,
  `ehrt.docs-tooling.*`, or any judge component. The domain
  implements or consumes this component's protocols and constructors;
  **never the reverse.**
- **The rule was kept by relocation, not by exception.** Two real
  edges into the domain's generator registry were found during
  characterization, and both were resolved by *keeping the
  domain-touching code behind* in `ehrt.corpus.generator-source` —
  the generator-kind Source constructor, and the generator-URL parsing
  branch — each relocated whole, each author-ruled.
- **Purity where claimed.** `partition-messages` is a pure function
  of messages; framing codecs and the designator grammar are pure.
- **Round-trip.** `parse`/`serialize` and
  `parse-source-designator`/`print-source-designator` are paired, the
  latter under a property test.
- **Registration is a load-time side effect** of requiring this
  interface, relied on transitively by `docs-tooling.lint`.

## 5. Non-goals

- **No domain logic, at all.** Intake, mutate, generate and the
  operator registry stayed in `corpus`. This component carries the
  codec those capabilities call, not the capability.
- **Not an interface-design ideal.** Deliberately thin (AR-1): it
  exports what real callers reach, and nothing for symmetry's sake.
- **Does not emit ACKs.** The generator never sends one;
  `mllp-ack-server!` exists only so a loopback round trip can be
  tested or demonstrated.
- **Does not judge, render for eyes, or simulate.**

## 6. Forbidden edges

Requires exactly `kernel` in `src`. Nothing else — and that is the
point of the brick.

Must never require, restating the directional rule as a list:

- **`corpus`** (né `ehrt.tools.*`) — the whole reason
  `parse-designator` is shaped as a skeleton with a caller-supplied
  `finish`.
- **`docs-tooling`**.
- **`judge`, `judge-v2-hapi`, `judge-v2-nist`,
  `judge-fhir-official`** — any judge component.
- **`sim`** and every simulator brick — a corpus is transported here,
  not produced.

## UNCLEAR — the author's review queue

- **UNCLEAR-IO1 — three names are reachable at two seams at once.**
  `message-timestamp-ms`, `message-type-trigger` and
  `message-patient-id` are exported by this interface **and** by
  `ehrt.corpus.interface`. This is not a drift hazard: the player's
  copies are true delegations — `ehrt.corpus.player:44-47` reads
  `(def message-timestamp-ms corpus-io/message-timestamp-ms)` and its
  three siblings likewise — so the two seams cannot disagree by
  construction, and ADR-0111's move-don't-improve relocation is
  intact. The open question is navigational, not correctness: a
  consumer wanting a message's timestamp can require either brick,
  and nothing says which is intended. Two readings: *(a)* deliberate —
  `corpus` callers should not have to learn `corpus-io` exists, so the
  re-export is a convenience the domain owes its consumers; *(b)*
  residue of the relocation, and new callers should reach `corpus-io`
  directly, the player's re-exports surviving only for the callers
  that predate the move.
- **UNCLEAR-IO2 — `lookup`'s bare name.** Exported `lookup` is the
  **framing** registry's, but this component also carries source,
  sink and canonicalizer registries. As with `kernel/valid?` and
  `sim-model/valid?`, the bare name at the seam does not say which
  registry it reads.
