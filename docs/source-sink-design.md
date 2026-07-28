# Source and Sink: Formal Types for Corpus I/O

**Status:** design record, pre-implementation. No `src/` code lands with
this document; build sessions SS-1..SS-5
(`.agents/plans/corpus-foundations.md`) implement it in stages.
**Scope:** formal `Source` and `Sink` types for everything that produces
or receives EHR corpus bytes in this repo — unifying `corpus.generate`'s
Synthea-specific engine, `corpus.intake`'s directory-specific ingestion,
the sim consumer loop's harness-only subprocess seam
(`test-integration/ehr_testing_tools/sim_harness.clj`, ADR-0013), and
today's bare output-path sinks under one typed, registry-open surface.
**Companion:** ADR-0017 (`notes/ADRs.md`) is the reasoning-of-record for
the decision to formalize; `.agents/plans/corpus-foundations.md`'s
SS-1..SS-5 rows are the build-session sequencing. This document is the
*what and why*, ADR-0017 is the accepted decision, the plan is the
*when, in what order* — the same three-way split
`docs/palgebra-design.md` uses against `judge-gate-refactor.md`.

Every fact asserted below about the current codebase was re-read from
source while writing this record: `src/ehr_testing_tools/corpus/intake.clj`,
`corpus/generate.clj`, `artifact.clj`, `invocation.clj`,
`test-integration/ehr_testing_tools/sim_harness.clj`, ADR-0013, ADR-0014,
`docs/notation.md`, `docs/pipeline.edn`.

---

## 0. Decision Register

| # | Decision | Rationale | Status |
|---|---|---|---|
| D1 | **Two species of source, one unification.** Generators (`synthea`, `sim`, registrable `simhospital`) execute an engine into a fresh directory with a `ManifestV1_1` sidecar; readers (`dir`, `file`, `stdin`, `blaze`) ingest existing bytes, spooling to a directory first if the input is a live stream. Both end up `dir`-shaped. | `corpus.intake` stays the single ingestion door with no per-source adapters; every corpus is replayable because network/pipe input exists on disk before anything judges it. | Settled (§I) |
| D2 | **Framing is an axis, not an assumption.** Sources carry `:framing` (`:file-per-item \| :er7-multi \| :ndjson \| :bundle-entries \| :mllp`), independent of `:format`. MLLP support is a framing *codec* only (pure bytes⇄messages functions); transport is `nc`'s job, no socket code enters this repo. | File ≠ item (the vendored SimHospital corpus is 1,013 messages in one file, ADR-0011); a byte-stream needs an envelope codec regardless of who owns the socket. | Settled (§II) |
| D3 | **Sinks**: kinds `dir`/`file`/`stdout`(optionally MLLP-framed)/`blaze`; fail-if-exists is the default; every sink emits a `ManifestV1_1` sidecar, so every sink's output is a valid source. | Symmetry with sources; the composability law is what makes a pipeline of sinks-into-sources actually chain without hand-authored glue. | Settled (§III) |
| D4 | **Maps canonical, URLs surface.** The wire/CLI format is a compact URL string (`dir:./corpus?format=v2-er7&framing=er7-multi`); it parses to a canonical Clojure map with well-known fields (`:kind`/`:format`/`:framing`/kind-specific). `parse ∘ print = identity` on canonical maps, round-trip tested. EDN config files use maps directly. | Nested generator params (module sets, patient counts) don't force through query-string encoding if the map is the canonical form and the URL is a projection of it, not the other way around. | Settled (§IV) |
| D5 | **Explicit assumptions and non-goals.** Sim stays subprocess-only (ADR-0013, unchanged) with a named future evolution (sim as a pinned artifact-registry entry once published); no SimHospital implementation ships, only a registry slot; network sources may carry real (non-synthetic) data, tagged `:foreign` with provenance; spooling has a size cap with explicit override; format ≠ version ≠ profile. | Names what this design does *not* claim, so a later session can't accidentally read more into it than was decided. | Settled (§V) |
| D6 | **Vocabulary.** `Source`/`Sink` runtime types deliberately rhyme with `docs/notation.md`'s existing source/intermediate/sink classification of equation resources. Collision resolved: `CatalogEntry`'s stringly `:source` field (a provenance label) is renamed `:origin` — the build session that touches intake does the rename, not this capture session. | A formal `Source` type and a stringly `:source` label coexisting is exactly the abstraction incoherence the judge/gate rename (ADR-0009) existed to prevent. | Settled (§VI) |
| D7 | **Where existing pieces land.** `corpus.generate` becomes the `synthea` generator source's engine half; `sim-harness`'s subprocess seam becomes the `sim` generator source in `src/`, with the harness delegating; `corpus.intake` is unchanged in role, gains callers; `corpus.mutate` and the judges eventually accept source/sink values where they take paths today (staged, later sessions). | No pipeline stage this repo already built and tested gets rewritten to land this design — only re-expressed under the new type where a build session actually touches it. | Settled (§VII) |
| D-a | URL scheme spellings (`dir:` vs `file:` with trailing slash; invented schemes for generators). | Author taste call, cheap to defer, expensive to guess wrong before any parser exists. | **Open** |
| D-b | Whether the `blaze` sink lands before or after the IG-pinning blocker clears — they interact: what profile does a written resource claim? | No IG package is pinned in `artifacts.lock.edn` today (`docs/pipeline.edn`'s own Gate contract note); a `blaze` sink writing FHIR resources without an answer to "which profile" is premature. | **Open** |
| D-c | Whether the `:origin` rename (D6) ships with the first source build session (SS-1) or gets its own micro-session. | Sequencing convenience only; no design consequence either way. | **Open** |

---

## Part I — Sources (D1)

### I.1 Two species

**Generator sources** run an engine and are parameterized (seed, module
set, patient count, …): `synthea` (built today as `corpus.generate`),
`sim` (built today only as a `test-integration/`-only subprocess seam,
ADR-0013), and — deliberately unimplemented but registrable —
`simhospital`. Deterministic by seed; emit provenance. `simhospital`'s
registry entry is the entire accommodation this design makes for it; no
implementation ships (D5).

**Reader sources** ingest existing bytes without running anything:
`dir` (a directory or tree), `file`, `stdin`, `blaze` (a FHIR
search/query against a Blaze endpoint).

### I.2 Unification

A generator source executes its engine into a fresh directory with a
`ManifestV1_1` sidecar — exactly the shape `corpus.generate` already
produces (`manifest/build-v1-1`, `corpus/generate.clj:209`) — then *is*
a `dir` source over that directory. A streaming reader (`stdin`,
anything piped from `nc`) spools to a directory first: one file per
framed item, plus a capture manifest recording timestamp, framing, and
declared origin. It then is a `dir` source too.

**Law: every corpus is replayable.** Network and pipe inputs exist on
disk before anything judges them. Determinism claims attach to the
spool, not the wire.

**Consequences, all load-bearing:**

- `corpus.intake` remains the single ingestion door; catalog, lineage,
  content-hash identity, and manifest sidecar enrichment (ADR-0014)
  apply to every source uniformly, with no per-source adapters. Intake's
  real domain generalizes from today's `foreign-file` to a `dir`-shaped
  `corpus-tree` — a strict superset, since every source ends up
  `dir`-shaped by this unification.
- Adding a generator means adding a registry entry (engine artifact or
  subprocess recipe, plus a param schema) — the same shape
  `corpus.operators`' registry already uses (`register!`,
  `corpus/operators.clj:75`). SimHospital-readiness is exactly this
  registry slot and nothing more.

---

## Part II — Framing (D2)

File ≠ item. The vendored SimHospital corpus is 1,013 messages in one
file (`test/fixtures/v2/simhospital/`, ADR-0011); NDJSON and FHIR
Bundles pack many resources per file; MLLP frames a byte stream. Every
source (and sink) carries `:framing` as an axis independent of
`:format`:

```
:file-per-item | :er7-multi | :ndjson | :bundle-entries | :mllp
```

**MLLP support is a framing codec only** — pure functions, bytes ⇄
framed messages, the `0x0B` / `0x1C 0x0D` envelope. Transport is `nc`'s
job; no socket code enters this repo. `docs/notation.md`'s external-stage
device (dashed box, no laws claimed) is exactly the right shape for the
transport step — see the palgebra equations below.

**Charset/encoding honesty.** v2's MSH-18 and FHIR's UTF-8 assumption
are named here as the codec's own edge conditions, resolved at build
time with tests (SS-3), not assumed away.

---

## Part III — Sinks (D3)

Kinds: `dir`, `file`, `stdout` (optionally MLLP-framed), `blaze` (FHIR:
transaction bundle vs. per-resource PUT is an explicit sink option,
never inferred).

**Write discipline.** Fail-if-exists is the default; `:overwrite` /
`:append` are explicit opt-ins. Network sink failures are Result values
(result-not-throw, this repo's existing convention — `result.clj`),
with per-item outcomes recorded, never a thrown exception.

**Law (composability, load-bearing).** Sinks emit manifest sidecars,
the same `ManifestV1_1` mechanism sinks generators already use, so that
**every sink's output is a valid source.** This is the invariant the
entire two-type design is built around, not an incidental convenience —
stated as a law here; SS-4 owes it a property test (write, then read
the same output back through a `dir` source and `corpus.intake`,
confirm lineage survives).

**No inference on the write side, ever.** Sinks declare `:format` and
`:framing`/protocol explicitly. Sources may infer (see Part IV); sinks
never do.

---

## Part IV — Naming: maps canonical, URLs surface (D4)

The canonical form is a Clojure map with well-known fields:

```clojure
{:kind    :dir | :file | :stdin | :blaze | :synthea | :sim   ; open set via registry
 :format  :fhir-json | :v2-er7 | :inferred                    ; sources may infer; recorded when inferred
 :framing :file-per-item | :er7-multi | :ndjson | :bundle-entries | :mllp
 ;; kind-specific:
 :path "..." :url "..." :query "..." :params {...} :seed 42}
```

CLI argv accepts a compact URL string surface, parsed into the map:

```
dir:./corpus?format=v2-er7&framing=er7-multi
sim:?seed=42
blaze://host:8080/fhir?query=Patient%3F_count%3D100
stdin:?framing=mllp&format=v2-er7
```

**Law.** Every URL has exactly one canonical map; `parse ∘ print =
identity` on canonical maps — round-trip tested. EDN config files use
maps directly; nested generator params (module sets, patient counts)
never get forced through query-string encoding, since the map, not the
URL, is the canonical form.

**Format inference** (extension, then content sniff — the same order
`corpus.intake/sniff-format` already uses) applies only where `:format`
is absent, and the fact of inference is recorded in the catalog entry
— the same honesty `corpus.intake`'s own docstring already commits to
("format sniffing is a cheap heuristic, not a validator").

---

## Part V — Explicit assumptions and non-goals (D5)

- **Sim remains subprocess-only** (ADR-0013, unchanged by this design).
  Once sim is published (Clojars/Maven — the author's own open call),
  the `sim` generator source's engine can become a pinned entry in the
  existing artifact registry (`ehr artifact fetch sim`), exactly like
  `synthea`/`temurin-jdk`/`fhir-validator-cli` — dissolving the
  sibling-checkout requirement. Recorded as the intended evolution, not
  built now.
- **No SimHospital implementation** ships; the registry shape (§I.2) is
  the whole accommodation.
- **No PHI-handling posture change** — but stated plainly: network
  sources (`blaze`, piped feeds) can carry real data. The `:foreign`
  layer tag (already `CatalogEntry`'s `:layer` value for intaken
  corpora, `intake.clj:59`) and provenance fields are where that fact
  lives. Synthetic-only guarantees apply only to this repo's own
  generators.
- **Spooling gets a size cap with an explicit override** — unbounded
  `stdin` into a laptop disk is an error, not a surprise.
- **Format ≠ version ≠ profile.** Sources declare format only; FHIR
  version/profile remain the judge tier's own concern (`judge.fhir`,
  the not-yet-pinned IG package).

---

## Part VI — Vocabulary: the `:origin` rename (D6)

`docs/notation.md` already distinguishes, in its own generated diagram
(`docs/pipeline.md`'s Mermaid output), **source types** (resources with
no producing stage — the diagram's own "raw inputs, not produced by any
operation" comment) from produced/intermediate resources, with an
implicit sink category (resources with no consuming stage) at the other
end. The runtime `Source`/`Sink` types this design introduces
deliberately rhyme with that existing classification: a `Source` value
is what a notation-source-type resource's bytes come from.

**Collision.** `CatalogEntry` (`corpus/intake.clj:54`) already has a
`:source :string` field — a provenance label (e.g. an intake session's
`source-label`), not a type. **Ruling: rename the catalog field to
`:origin`** in the build session that touches intake (D-c: which
session, still open) — with the usual compatibility note. A formal
`Source` type and a stringly `:source` label coexisting would be
exactly the abstraction incoherence the judge/gate rename (ADR-0009,
`:policy` → `:disposition`) already exists to prevent; the same genus
of bug, caught before it ships rather than after.

---

## Part VII — Where existing pieces land (D7)

- **`corpus.generate`** becomes the `synthea` generator source's engine
  half — the two-step pattern (execute, then interpret) is unchanged;
  see the `EngineExecute`/`Generate` correspondence in the equations
  below.
- **`sim-harness`** (`test-integration/ehr_testing_tools/sim_harness.clj`)
  grows up: its subprocess seam becomes the `sim` generator source in
  `src/`, with the harness delegating to it; the cross-repo consumer
  loop keeps testing through it, unchanged in spirit (ADR-0013's
  subprocess-only rule is not reopened by this design).
- **`corpus.intake`** is untouched in role; gains nothing but callers —
  every source, generator or reader, ends up handing intake a
  `dir`-shaped tree exactly as it does today.
- **`corpus.mutate` and the judges** are transforms between sources and
  sinks; their signatures eventually accept source/sink values where
  they take paths today. Staged, in later build sessions — not SS-1..SS-5.

---

## Part VIII — Palgebra Equations

Equation form and vocabulary per `docs/notation.md`; `# planned` marks a
stage designed but not yet built, matching `docs/pipeline.md`'s own
convention ("a stage marked `# planned` below is designed but not yet
built — its equation and law are fixed, its implementation isn't").
None of the equations below are built this session; they are staged
across SS-1..SS-5.

**Catalytic resolution** (`docs/notation.md`'s four-target rule):
`engine-artifact`/`runtime` resolve to target 1 (`artifacts.lock.edn`),
the same slot `synthea-artifact`/`jdk-runtime` already occupy in the
built `Generate` equation; `param-hash` resolves to target 3 (hashed
repo-authored config), matching `Generate`'s own `config-hash`;
`framing-codec` resolves to target 4 (in-repo code registry), the same
shape as `corpus.operators`/`corpus.canonicalizers`. `source-map`,
`sink-map`, `blaze-query`/`blaze-endpoint`, and `size-cap` are ordinary
(non-catalytic) inputs — data the stage consumes, not a resource it
uses without consuming — the same status `synthea-config` already has
in the built `Generate` equation.

```
;; ---- Naming: URL surface <-> canonical map (round-trip law, D4) ----
source-url → source-map  [ParseSourceUrl]                                     # planned
source-map → source-url  [PrintSourceMap]                                     # planned
;; law: PrintSourceMap then ParseSourceUrl recovers the identical canonical
;; map -- round-trip tested per :kind. URL scheme spellings open (D-a).

sink-url → sink-map  [ParseSinkUrl]                                           # planned
sink-map → sink-url  [PrintSinkMap]                                           # planned
;; law: same round-trip law as above, over sink-shaped maps.

;; ---- Generator sources: engine execution into a fresh directory (D1) ----
generator-config × engine-artifact × runtime × param-hash → generated-corpus
  [EngineExecute]  {catalytic: engine-artifact, runtime, param-hash}          # planned
;; law: appends a ManifestV1_1 sidecar naming the engine, seeds, and params --
;; the sidecar corpus.generate already writes. A generated-corpus is a
;; dir-corpus by construction (the unification, D1).
;; contract: synthea-config x synthea-artifact x jdk-runtime x config-hash ->
;; raw-corpus [Generate] (BUILT, docs/pipeline.edn) is EngineExecute's own
;; synthea instance, not a rewrite -- corpus.generate becomes the synthea
;; registry entry's engine half (D7). sim and simhospital are two more
;; registry entries over this same equation shape: sim built (subprocess,
;; ADR-0013), simhospital registrable-only (D5).

;; ---- Reader sources: existing bytes, no engine (D1) ----
dir-bytes → dir-corpus  [ReadDir]                                             # planned
file-bytes → file-corpus  [ReadFile]                                         # planned
blaze-query × blaze-endpoint → blaze-corpus  [ReadBlaze]                      # planned
;; law: a blaze-corpus (or any network-read corpus) carries the :foreign
;; layer tag and provenance fields naming it as possibly-real data (D5) --
;; synthetic-only guarantees apply only to this repo's own generators.

;; ---- Streaming readers: spool before anything judges the bytes (D1) ----
mllp-wire → framed-stream  [Transport]  {external: true}                      # planned
;; no law claimed -- Transport is nc's own subprocess, outside this repo
;; (D2). framed-stream is whatever bytes arrive at this repo's own stdin;
;; the write-side sink mirrors this same external stage in reverse
;; (stdout piped to nc), not drawn twice here.

framed-stream × size-cap → spooled-corpus  [Spool]  {catalytic: framing-codec} # planned
;; law: one file per framed item, plus a capture manifest (timestamp,
;; framing, declared origin). Replayability and determinism claims attach
;; to the spool, not the wire. Unbounded stdin into a laptop disk is an
;; error (size-cap exceeded), not a surprise -- explicit override required (D5).

;; ---- Framing codecs: pure bytes <-> item-seq, per :framing kind (D2) ----
framed-bytes → item-seq  [DecodeFraming]  {catalytic: framing-codec}          # planned
item-seq → framed-bytes  [EncodeFraming]  {catalytic: framing-codec}          # planned
;; law: DecodeFraming composed with EncodeFraming recovers the identical
;; framed-bytes, for :er7-multi / :ndjson / :bundle-entries / :mllp --
;; round-trip property-tested per framing kind (SS-3). The vendored
;; SimHospital fixture (test/fixtures/v2/simhospital/, ADR-0011) is the
;; :er7-multi witness. v2's MSH-18 and FHIR's UTF-8 assumption are this
;; codec's own edge conditions, resolved with tests, not assumed away (D2).

;; ---- Unification: every source is, or feeds, a dir-shaped corpus-tree ----
generated-corpus × spooled-corpus × dir-corpus × file-corpus × blaze-corpus
  → corpus-tree  [UnionCorpusTree]  {spider: funnel}                          # planned

;; ---- Intake generalizes from foreign-file to corpus-tree (D1, D6) ----
corpus-tree → catalog-entry + intake-record  [Intake]
;; law: UNCHANGED from the built Intake (docs/pipeline.edn) -- corpus-tree
;; is a strict superset of foreign-file (dir-shaped, ManifestV1_1-sidecar-
;; aware, ADR-0014); no per-source adapter, intake stays the single
;; ingestion door (D1). The :source field this stage reads/writes is
;; renamed :origin (D6).

;; ---- Sinks: write discipline + the composability law (D3) ----
datum × sink-map → sink-bytes  [Write]                                        # planned
;; law: fail-if-exists is the default; :overwrite/:append are explicit
;; opt-ins, never inferred (D3).
;; law (COMPOSABILITY, load-bearing): sink-bytes emits its own ManifestV1_1
;; sidecar, so Write feeding ReadDir feeding Intake recovers the same
;; lineage the write started from -- every sink's output is a valid source
;; (D3). Property-test obligation, SS-4.

datum × blaze-sink-map → blaze-write-result  [WriteBlaze]                     # planned
;; law: Result-not-throw -- a network sink failure is a value, with
;; per-item outcomes recorded, never a thrown exception (D3). Transaction
;; bundle vs. per-resource PUT is explicit on blaze-sink-map, never
;; inferred (D3, sequenced against D-b).

sink-bytes → framed-stream  [EncodeFraming]  {catalytic: framing-codec}       # planned
;; the stdout sink, optionally MLLP-framed: EncodeFraming (this repo) hands
;; framed bytes to nc's own subprocess (Transport, external, D2) for the
;; wire -- the write-side mirror of the read-side Spool/Transport pair above.
```

---

## Deferred decisions (record only — not resolved here)

- **D-a** — URL scheme spellings (`dir:` vs `file:` with trailing
  slash; invented schemes for generators). Author taste call at build
  time.
- **D-b** — whether the `blaze` sink lands before or after the
  IG-pinning blocker clears. They interact: what profile does a
  written resource claim?
- **D-c** — whether the `:origin` rename (D6) ships with the first
  source build session (SS-1) or its own micro-session.
