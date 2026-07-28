2026-07-27 — Capture session: formal Source and Sink types (design doc + ADR + palgebra equations; no implementation)

Context
Corpus input and output in this repo are currently ad-hoc: `corpus.generate` knows Synthea specifically, `corpus.intake` knows directories specifically, the sim consumer loop lives only in `test-integration/` harness code, and sinks are bare output paths. The author has ruled that source and sink become formal types: anything that produces EHR data plugs in as a source, anything that receives it as a sink, with lineage surviving every edge. This is a capture session: it lands the design document, an ADR, the palgebra equations for the new capability, and plan rows for the build sessions. It writes NO `src/` code. The design content below is author-ruled in chat (2026-07-27) and is to be captured faithfully; the open questions in §"Deferred decisions" are recorded as open, not resolved by the agent.

Read first

* `src/ehr_testing_tools/corpus/intake.clj` (the single ingestion door; `CatalogEntry`, format sniffing, manifest-sidecar enrichment)
* `src/ehr_testing_tools/corpus/generate.clj` (two-step engine pattern), `artifact.clj` (pinned-artifact registry), `invocation.clj`
* `test-integration/ehr_testing_tools/sim_harness.clj` + ADR-0013 (sim: subprocess-only, never classpath)
* `docs/notation.md` (source/sink as type classifications in the equation language), `docs/pipeline.edn`
* `notes/ADRs.md` ADR-0004 (generation), ADR-0014 (manifest sidecars)

The ruled design (capture verbatim in substance, adapt to house style)

1. Two species of source, one unification

* Generator sources run an engine and are parameterized (seed, module set, patient count…): `synthea`, `sim`, and — deliberately unimplemented but registrable — `simhospital`. Deterministic by seed; emit provenance.
* Reader sources ingest existing bytes: `dir` (a directory or tree), `file`, `stdin`, `blaze` (a FHIR search/query against a Blaze endpoint).

Unification: a generator source executes its engine into a fresh directory with a ManifestV1_1 sidecar, then is a `dir` source over that directory. A streaming reader (`stdin`, anything piped from `nc`) spools to a directory first (one file per framed item, plus a capture manifest: timestamp, framing, declared origin), then is a `dir` source. Consequences, all load-bearing:

* `corpus.intake` remains the single ingestion door; catalog, lineage, content-hash identity, and manifest enrichment apply to every source uniformly with no per-source adapters.
* Every corpus is replayable: network and pipe inputs exist on disk before anything judges them. Determinism claims attach to the spool, not the wire.
* Adding a generator = adding a registry entry (engine artifact or subprocess recipe + param schema), the same shape as `corpus.operators`' registry. SimHospital-readiness is exactly this and nothing more.

2. Framing is an axis, not an assumption

File ≠ item. The vendored SimHospital corpus is 1,013 messages in one file, NDJSON and Bundles pack many FHIR resources per file, MLLP frames a byte stream. Sources carry `:framing`: `:file-per-item | :er7-multi | :ndjson | :bundle-entries | :mllp`. The MLLP support is a framing codec only (pure functions: bytes ⇄ framed messages, the 0x0B/0x1C 0x0D envelope); transport is `nc`'s job, and no socket code enters this repo. Charset/encoding honesty: v2's MSH-18 and FHIR's UTF-8 assumption are named in the design doc as the codec's edge conditions, resolved at build time with tests, not assumed away.

3. Sinks

Kinds: `dir`, `file`, `stdout` (optionally MLLP-framed), `blaze` (FHIR: transaction bundle vs per-resource PUT is an explicit sink option, never inferred). Rules:

* Write discipline: fail-if-exists is the default; `:overwrite` / `:append` are explicit. Network sink failures are Result values (result-not-throw), with per-item outcomes recorded.
* Sinks emit manifest sidecars, same ManifestV1_1 mechanism sim uses, so that every sink's output is a valid source — the composability invariant this design is built around. State it as a law in the design doc; the build sessions get a property test for it.
* Sinks declare `:format` and `:framing`/protocol explicitly. No inference on the write side, ever.

4. Naming things: maps canonical, URLs as surface

The canonical form is a Clojure map with well-known fields:

```clojure
{:kind    :dir | :file | :stdin | :blaze | :synthea | :sim   ; open set via registry
 :format  :fhir-json | :v2-er7 | :inferred                    ; sources may infer; recorded when inferred
 :framing :file-per-item | :er7-multi | :ndjson | :bundle-entries | :mllp
 ;; kind-specific:
 :path "..." :url "..." :query "..." :params {...} :seed 42}

```

CLI argv accepts a compact URL string surface — `dir:./corpus?format=v2-er7&framing=er7-multi`, `sim:?seed=42`, `blaze://host:8080/fhir?query=Patient%3F_count%3D100`, `stdin:?framing=mllp&format=v2-er7` — parsed into the map. Law: every URL has exactly one canonical map; parse ∘ print = identity on canonical maps (round-trip tested). EDN config files use maps directly; nested generator params never get forced through query strings. Format inference (extension, then content sniff, as intake does today) applies only where `:format` is absent, and the fact of inference is recorded in the catalog entry.

5. Explicit assumptions and non-goals

* Sim remains subprocess-only (ADR-0013 unchanged). Interaction with the release decision: once sim is published (Clojars/Maven, the author's open call), the sim generator source's engine can become a pinned entry in the existing artifact registry (`ehr artifact fetch sim`) exactly like synthea/temurin/validator, dissolving the sibling-checkout requirement. Record as the intended evolution, not built now.
* No SimHospital implementation; the registry shape is the whole accommodation.
* No PHI-handling posture change, but the design doc says plainly: network sources (`blaze`, piped feeds) can carry real data; the `:foreign` layer tag and provenance fields are where that fact lives, and synthetic-only guarantees apply only to this repo's own generators.
* Spooling gets a size cap with an explicit override — unbounded `stdin` into a laptop disk is an error, not a surprise.
* Format ≠ version ≠ profile: sources declare format only; FHIR version/profile remain the judge tier's concern.

6. Vocabulary (load-bearing; one collision to resolve)

`docs/notation.md` already classifies equation types as source/intermediate/sink — the runtime types being introduced here deliberately rhyme with that (a `Source` value is what a notation-source type's bytes come from). The design doc draws this correspondence explicitly. Collision: `CatalogEntry` already has a `:source :string` field (a provenance label). Author ruling: rename the catalog field to `:origin` in the build session that touches intake, with the usual compatibility note — a formal `Source` type and a stringly `:source` label coexisting would be exactly the abstraction incoherence the judge/gate rename existed to prevent.

7. Where existing pieces land

* `corpus.generate` becomes the `synthea` generator source's engine half (two-step pattern unchanged).
* `sim_harness` grows up: its subprocess seam becomes the `sim` generator source in `src/`, harness delegating to it; the consumer loop keeps testing through it.
* `corpus.intake` is untouched in role; gains nothing but callers.
* `corpus.mutate` and the judges are transforms between sources and sinks; their signatures eventually accept source/sink values where they take paths today (build sessions, staged).

Deferred decisions (record as open, do not resolve)

* D-a: URL scheme spellings (`dir:` vs `file:` with trailing slash; invented schemes for generators) — author taste call at build time.
* D-b: whether `blaze` sink lands before or after the IG-pinning blocker clears (they interact: what profile does a written resource claim?).
* D-c: whether `:origin` rename ships with the first source build session or its own micro-session.

Steps

Step 1 — Design doc
Write `docs/source-sink-design.md` from §"The ruled design" above, in this repo's design-doc register (cf. `docs/palgebra-design.md`): decisions numbered, laws stated as laws, deferred decisions listed as open with their D-labels. Include the palgebra equations for the capability (source kinds → `corpus-tree` union; `corpus-tree` → catalog → … → sink kinds; spool and engine-execute as stages; the composability law noted beside the sink stages) in equation form per `docs/notation.md`, marking unbuilt stages `# planned:` per that document's own convention.
Commit: `docs: source/sink design — formal types, unification via dir+manifest, framing axis, composability law`

Step 2 — ADR
Append the next ADR: the decision to formalize source/sink; the generator/reader unification and spool rule; maps-canonical/URLs-surface with the round-trip law; sink manifest emission and the every-sink-output-is-a-source law; the vocabulary ruling (§6) including the `:origin` rename intent; alternatives considered (URLs-canonical — rejected, nested params; per-source adapters into the catalog — rejected, N×M seams; sockets in-repo — rejected, `nc` exists; SimHospital support — deferred to a registry entry).
Commit: `adr: formal source and sink types (ADR-00NN)`

Step 3 — Plan rows
In `.agents/plans/corpus-foundations.md` (or a new plan file if that document's conventions prefer), add staged build-session rows:

* SS-1 `Source`/`Sink` schemas + URL⇄map parser with round-trip property test; `dir`/`file` source and sink over the new types; intake called through them (no behavior change; golden catalog comparison).
* SS-2 Generator registry; `synthea` re-expressed as a registry entry over `corpus.generate`; `sim` source in `src/` (subprocess, ADR-0013-shaped); harness delegates; smoke-tier coverage per the verification-tiers policy.
* SS-3 Framing codecs (`er7-multi`, `ndjson`, `bundle-entries`, `mllp` bytes⇄messages) as pure functions with property tests (round-trip; SimHospital fixture as the `er7-multi` witness); `stdin` source via spool.
* SS-4 Sinks: write discipline, manifest emission, the composability property test (sink output re-intakes with lineage intact); `stdout` incl. MLLP framing.
* SS-5 `blaze` source/sink (Result-not-throw, transaction vs PUT explicit) — sequenced against D-b.

Each row names its test-first obligations and its verification tier.
Commit: `plans: source/sink build sessions SS-1..SS-5 staged`

Step 4 — Archive this prompt
To `.agents/prompts/archive/`, deviation appendix if any.
Commit: `prompts: archive 2026-07-27 source-sink capture session`

Final report
Files landed, ADR number taken, plan rows added, any wording where the agent's house-style adaptation changed substance (should be none), deviations.
