# Source and Sink: Formal Types for Corpus I/O

**Status:** design record, pre-implementation. No `src/` code lands with
this document; build sessions SS-1..SS-5
(`.agents/plans/corpus-foundations.md`) implement it in stages.
**Scope:** formal `Source` and `Sink` types for everything that produces
or receives EHR corpus bytes in this repo — unifying `corpus.generate`'s
Synthea-specific engine, `corpus.intake`'s directory-specific ingestion,
the sim consumer loop's harness-only subprocess seam
(`projects/conformance/test/ehrt/conformance/sim_harness.clj`, tools/ADR-0013), and
today's bare output-path sinks under one typed, registry-open surface.
**Companion:** ADR-0017 (`notes/ADRs.md`) is the reasoning-of-record for
the decision to formalize; `.agents/plans/corpus-foundations.md`'s
SS-1..SS-5 rows are the build-session sequencing. This document is the
*what and why*, ADR-0017 is the accepted decision, the plan is the
*when, in what order* — the same three-way split
`docs/palgebra-design.md` uses against `judge-gate-refactor.md`.

Every fact asserted below about the current codebase was re-read from
source while writing this record (pre-Polylith paths at the time; homes
below are the Polylith equivalents, added in the 2026-07-31 errata
pass): `components/corpus/src/ehrt/corpus/intake.clj`,
`components/corpus/src/ehrt/corpus/generate.clj`,
`components/kernel/src/ehrt/kernel/artifact.clj`,
`components/kernel/src/ehrt/kernel/invocation.clj`,
`projects/conformance/test/ehrt/conformance/sim_harness.clj`, tools/ADR-0013,
tools/ADR-0014, `docs/notation.md`, `docs/pipeline.edn`.

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
| D-a | URL scheme spellings (`dir:` vs `file:` with trailing slash; invented schemes for generators). | Author taste call, cheap to defer, expensive to guess wrong before any parser exists. | **Resolved 2026-07-28 (SS-1 build):** six fixed spellings, no trailing-slash magic -- `file:` (single file), `dir:` (directory tree, a distinct scheme), `stdin:`, `synthea:`, `sim:` (all four: scheme, colon, optional path, optional `?query`), and `blaze://host:port/path?query=...` (the one scheme with an authority, since it names a network endpoint). Format/framing ride as query params on every scheme. All six parse far enough to name their own `:kind`; only `dir:`/`file:` have real constructors this session (`ehr-testing-tools.corpus.source-sink`) -- the other four are recognized-but-rejected (`:unsupported-source-kind`/`:unsupported-sink-kind`), never silently accepted and never confused with a genuinely unknown scheme. See `ehr-testing-tools.corpus.source-sink-url`. |
| D-b | Whether the `blaze` sink lands before or after the IG-pinning blocker clears — they interact: what profile does a written resource claim? | No IG package is pinned in `artifacts.lock.edn` today (`docs/pipeline.edn`'s own Gate contract note); a `blaze` sink writing FHIR resources without an answer to "which profile" is premature. | **Open** |
| D-c | Whether the `:origin` rename (D6) ships with the first source build session (SS-1) or gets its own micro-session. | Sequencing convenience only; no design consequence either way. | **Resolved 2026-07-28 (SS-1 build), Step 5:** ships in SS-1, sequenced after the golden-catalog comparison (ruling 5) passes -- `CatalogEntry`'s `:source` field is `:origin` now; `IntakeRecord`'s own (distinct) `:source` field is unaffected -- D6/ADR-0017 decision 5 names only `CatalogEntry`'s collision. No compatibility alias for the old spelling (pre-release, D10's own reasoning). **IntakeRecord follow-up (2026-07-28, SS-2 Step 0):** the SS-1 checker pass found `IntakeRecord`'s own `:source` field -- a different schema, colliding on spelling only, never named by D6/ADR-0017 decision 5 -- carrying the identical incoherence; renamed to `:origin` for consistency, same no-alias reasoning, closing D-c's scope completely. |
| D-d | **Manifest interop: can a `dir`/`file` sink honestly emit a `ManifestV1_1` sidecar naming this repo as producer?** | Probed 2026-07-28 (SS-4 build), ruling 2: `ManifestV1_1`'s required `:generator`/`:config`/`:invocation` fields are all shaped for an *external pinned engine* -- an `artifacts.lock.edn` artifact record, a real properties file, a subprocess invocation record -- exactly what `corpus.generate`'s and `corpus.generators`'s own `:synthea`/`:sim` entries supply (`corpus/generate.clj`'s `manifest/build-v1-1` call site), and exactly what a `dir`/`file` sink write (pure in-process Clojure -- `corpus.mutate` is pure, `corpus.sink-write` is a plain file write, neither runs a subprocess or consumes a pinned artifact) structurally does not have. Forcing values in would fabricate an artifact identity or a config file that doesn't exist -- the schema improvisation ruling 2 directs against. Two options framed for the author, neither adopted this session: **(A)** a distinct, versioned manifest schema for operation-producers (write/mutate), with `:generator` replaced by a plain producer-identity field (this repo's own `ehrt version` identity, not sha256-verified) and `:config`/`:invocation` made optional or dropped -- requires coordinating with intake's sidecar recognition (today: any `manifest.edn` is tried against `ManifestV1_1` specifically, `corpus/intake.clj`'s `sidecar-result`) and, eventually, with sim's own mirrored schema (ADR-0012 clause on manifest commitments). **(B)** reuse `ManifestV1_1` unchanged, with `:generator`/`:config`/`:invocation` populated by placeholder/proxy values (e.g. a hash of this repo's own git-describe string standing in for an artifact `:sha256`) -- unblocks emission now, but privately redefines what those field names mean relative to every other producer (sim included) using the identical schema, the same abstraction-incoherence class ADR-0009/ADR-0017 decision 5 already exist to prevent. | **Resolved 2026-07-28 (SS-4b build): option A, sub-choice A1** -- a distinct, versioned manifest schema for operation-producers, `ehr-testing-tools.corpus.operation-manifest/OperationManifestV1`, under its own filename `operation-manifest.edn` (never `manifest.edn` -- the two are never confused, never merged, and a directory carrying both is rejected, not resolved by precedence). `ManifestV1_1`, `corpus.generate`'s emitters, and ADR-0012's sim-mounting clause are untouched by construction: sim neither emits nor reads operation manifests, so the "eventually sim's mirrored schema" coordination option A's own text names never triggers. Option B is rejected outright: reusing `ManifestV1_1` with proxy values institutionalizes fabricated identity in a file format, the same incoherence class ADR-0009/ADR-0017 decision 5 already exist to prevent. Option A's sibling, **A2** (dispatch intake's existing `manifest.edn` recognizer by try-order across both schemas instead of a second, distinctly-named file) is also rejected: an implicit try-order contract is exactly the kind of unstated precedence rule the never-both rule (below) refuses to have. Full schema in Part III.5; ADR-0020 is the reasoning-of-record. |
| D8 | **The determinism law of defaults.** A CLI flag may default only to a pinned constant or a value derived deterministically from other pinned inputs — never the clock, the environment, the network, or the machine. `corpus intake`'s `--received` (a record-keeping date, not a generation input) is the one named exemption. | The reproducibility toolkit's own reason for existing (`docs/dev/AUDIENCES.md`, Dogfooding) is undermined by a convenience default that reads wall-clock time; naming the one legitimate exception (lineage dates) prevents it from being read as a loophole. | Settled (§IX) |
| D9 | **Ratified zero-flag defaults for `corpus generate`.** `--seed 1`; `--clinician-seed` defaults to `--seed`'s value; `--reference-date 20260101` (a named pinned constant beside `default-locale`/`default-timezone`); `--population 5`; `--output-dir` derived as `target/corpus/synthea-s<seed>-p<pop>`; `--config-path` defaults to a minimal properties file shipped in `resources/`. | The zero-flag run is the quickstart's first impression and doubles as a reproducibility demo (byte-identical across machines) rather than a mere convenience — leaving `generate` flag-heavy would bury that demo under six required flags. | Settled (§IX) |
| D10 | **One flag vocabulary, spellings ratified, old spellings removed.** `--lockfile` (not `--lockfile-path`), `--out` (new, for a single output file), `--out-dir` (not `--output-dir`), positional `PATH` with `--path` as its explicit twin (not `--input`/`--source-dir`). No aliases for the old spellings. | Pre-release (ADR-0008) is the one window where a breaking flag rename is cheap; `docs/cli.md`/`ehrt help` regenerate from `cli-spec` so the two surfaces cannot drift apart. | Settled (§IX) |
| D11 | **`ehrt gate PATH` dispatches per file via format sniffing.** `gate v2`/`gate fhir` remain as explicit overrides; a sniff-dispatched directory containing both formats is an error naming the override, not a silent per-file split. | Reuses `corpus.intake/sniff-format` (already a heuristic, already honest about being one) instead of inventing a second sniffing mechanism; erroring on a mixed directory keeps the default path from silently doing the wrong thing. | Settled (§IX) — mixed-directory behavior itself is OPEN-1 |
| D12 | **`corpus mutate` output and locator defaults.** `--out-dir` derives as `<input>-mutants/<operator-id>@<version>/`; a registry entry MAY declare `:default-locator` (its canonical conviction target); `--locator-path` stays required for operators without one. | Matches D9's derived-output-dir pattern; no operator's default locator is invented speculatively — declaring one is calibration work against `docs/judge-calibration.md`, done when that operator's default is actually authored. | Settled (§IX) |
| D13 | **Three new CLI conveniences.** `ehrt version` (repo version-of-record plus pinned artifact versions from the lockfile); `ehrt artifact fetch --all` (every lockfile artifact in one invocation); `ehrt doctor` (runs `SETUP.md`'s verification checklist as a command, exit 0/1/2 per the existing ladder). | Collapses recurring multi-step setup friction (T2's three-fetch incantation, `SETUP.md`'s manual checklist) into single commands, at the cost of one freshness obligation (`doctor`'s content must not disagree with `SETUP.md`). | Settled (§IX) |
| D14 | **The flag-vocabulary change and SS-1's URL surface land in the same build session.** Either SS-1 grows to include D9–D13, or a UX build session lands immediately before SS-1 and SS-1 rebases onto it — proposed in `.agents/plans/corpus-foundations.md`, decided by the author at build time. `docs/use-cases.md`'s ten command strips and the quickstart are re-verified end to end in whichever session changes the surface. | Two surface-breaking changes to the same CLI in two separate sessions would cost users two migrations instead of one; this repo's readers are pre-release and there is no reason to spend that cost twice. | Settled (§IX) — sequencing resolved 2026-07-27 (UX-1 build): UX-1 runs as its own build session, SS-1 rebases onto its result |
| OPEN-1 | Mixed-format directory behavior under bare `gate` (D11): error naming the override, vs. silent per-file dispatch. | Author taste call — either is implementable; the capture session ruled the error-by-default reading in but left it open for reconsideration at build time. | **Resolved 2026-07-27 (UX-1 build):** error default confirmed — a mixed-format directory under bare `gate` is an error naming the override (`gate v2`/`gate fhir`), not a silent per-file split |
| OPEN-2 | Whether `corpus generate`'s zero-flag `--population` default (D9) is `5` or `1`. | Trade-off between run speed (`1`, fastest) and corpus usefulness (`5`, enough patients for a non-degenerate first run) — a build-time taste call, not a design consequence. | **Resolved 2026-07-27 (UX-1 build):** `--population 5` |
| OPEN-3 | Whether `ehrt doctor` (D13) belongs in the first release or ships after. | Sequencing convenience only — `doctor` has no design dependency on anything else in this capture. | **Resolved 2026-07-27 (UX-1 build):** `doctor` ships in the first release |
| OPEN-4 | Whether `corpus generate` grows an `--engine` flag now that the generator registry (SS-2) names more than one engine kind (`synthea`, `sim`), so a caller could pick which engine `corpus generate` drives instead of only ever driving Synthea. | Raised 2026-07-28 (SS-2 build), ruling 6: `corpus generate`'s own verb, flags, and defaults are explicitly out of scope for SS-2 — generator URLs land at `corpus intake` only this session — so this is recorded rather than decided; a future session either adds `--engine` or leaves `corpus generate` Synthea-only forever, with `intake GENERATOR-URL` as the one multi-engine door. | **Open** |
| OPEN-6 | Dir-sink `:append` -- append-to-a-corpus means merging into an existing catalog/manifest (which files were already there, whose provenance, whose hashes), not a per-file bytes-concatenation the way `:er7-multi`/`:ndjson`/`:mllp` file-sink append is. | Raised 2026-07-28 (SS-4 build), ruling 7: SS-4's own write-discipline scope names this REJECTED `:append-unsound` unconditionally this session, regardless of framing -- `write-dir!` (`ehr-testing-tools.corpus.sink-write`) rejects it by name, not silently. What a sound dir-append would even mean (manifest merge semantics) is itself unresolved, and interacts with D-d (manifest interop) once that resolves. **D-d resolved (2026-07-28, SS-4b build):** a dir-append now has a concrete schema to merge against -- `OperationManifestV1`'s own `:items` -- but what merging two `:items` vectors (and two `:producer`/`:operation` claims for the same directory) means is still not decided here; this note only removes the "no schema exists yet" half of why it was deferred. | **Open** |
| OPEN-5 | Whether a `dir:` Source ever grows framing-awareness — i.e. a directory containing one or more multi-item files (an `er7-multi`/`ndjson`/`mllp`/`bundle-entries`-framed file sitting inside an otherwise ordinary directory), spooled per-file rather than treated as a single opaque foreign file. | Raised 2026-07-28 (SS-3 build), ruling 5: this session's spool resolves exactly two cases — a `stdin:` source (always) and a bare `file:` source whose own `:framing` isn't `:file-per-item` — deliberately leaving `dir:` sources `:file-per-item`-only, per the session's own scope fence (ruling 7). A directory mixing ordinary files with one or more multi-item files inside it is unaddressed; whether that ever needs the same treatment recursively (walk the directory, spool anything non-`:file-per-item` it contains) or stays out of scope permanently is a future call, not decided here. | **Open** |

---

## Part I — Sources (D1)

### I.1 Two species

**Generator sources** run an engine and are parameterized (seed, module
set, patient count, …): `synthea` (built today as `corpus.generate`),
`sim` (built today only as `projects/conformance/test`'s own
subprocess seam, tools/ADR-0013), and — deliberately unimplemented but
registrable —
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
file (`test-fixtures/v2/simhospital/`, ADR-0011); NDJSON and FHIR
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

**`:er7-multi` grammar, probed (2026-07-28, SS-3 build session, ruling
3).** Re-measured directly against the vendored fixture
(`test-fixtures/v2/simhospital/messages.out`), independently of the
existing `simhospital-corpus` test helper's own docstring (which
records the same finding, citing facts-register F25): 1,013 messages,
1,013 `MSH` occurrences (one per message, none embedded elsewhere),
every message starting with `MSH` at a line start; segments within a
message are CR-terminated (0x0D) with no CR after the last segment;
messages are separated by a blank LF line (`\n\n`, 0x0A 0x0A), and the
file ends with a trailing `\n\n` after the last message (no message
lacks its own trailing separator). `ehr-testing-tools.corpus.framing`'s
`:er7-multi` codec decodes via MSH-line-start scanning (a message
starts at offset 0 or immediately after a bare LF) rather than a
literal `\n\n` split — behaviorally identical on this fixture, but
robust to a message payload that happens to contain a literal `\n\n`
substring internally, which pure blank-line splitting cannot
distinguish from a real boundary.

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

**SS-4 build session (2026-07-28) — manifest emission gated by D-d.**
The `dir`/`file` half of this law's own sidecar mechanism is blocked on
D-d (Decision Register, above): `ManifestV1_1`'s `:generator`/`:config`/
`:invocation` fields have no honest value for a sink write, which is
pure in-process code, not an external pinned engine. No `dir`/`file`
sink emits a `manifest.edn` this session; the composability property
this session proves is the hash-identity half only (write → intake →
content hashes equal), with the `:origin`/provenance half deferred to
whichever session D-d resolves in. The `stdout` sink (below) carries no
manifest by design regardless (no directory to drop one in) — its own
form of the law is unaffected by D-d and lands in full this session.

**SS-4b (2026-07-28) — the reduced property's provenance half, dated
closed.** D-d resolved (Decision Register, above): `dir`/`file` sinks
now emit `operation-manifest.edn` (Part III.5, below), and the
composability property (`sink_composability_test.clj`) gained the half
this note deferred -- write via a `dir` Sink with an operation manifest,
then intake the same directory back, and the catalog's
`:operation-provenance :origin` reflects the manifest's own
`:producer`, with per-item `:input-hash` surviving into
`:operation-provenance :input-hash` wherever the write actually
supplied one. The `stdout` sink is unaffected, exactly as predicted
above: no directory, no manifest, by design, not by gap.

**The stdout sink's own law (byte-stream form, SS-4 Step 3).** A
`stdout` sink has no directory, so it cannot restate the composability
law as "re-intake the output" the way `dir`/`file` do. Its law is
stated instead as a two-part chain, matching ruling 4: (1) the framing
round-trip already proved per-kind by
`ehr-testing-tools.corpus.framing`'s own property tests (SS-3) —
`decode(encode(items)) = items`, byte-exact for every framing but
`:bundle-entries` (item-level identity there, Part II) — is what makes
the bytes a `stdout` sink writes decodable at all; (2) the loopback
(SS-4 Step 4) is the acceptance form of "every sink's output is a
valid source" for a byte stream rather than a directory: this repo's
own `stdout:` sink output, piped directly into this repo's own
`stdin:` source (SS-3), re-intakes with hashes equal — the CLI, not a
hand-authored glue script, proves the pipe. This is the byte-stream
analogue of the `dir`/`file` law, not an exemption from it.

**No inference on the write side, ever.** Sinks declare `:format` and
`:framing`/protocol explicitly. Sources may infer (see Part IV); sinks
never do.

---

## Part III.5 — The Operation Manifest (D-d, resolved via option A1)

A generator manifest (`ManifestV1_1`, above) and an operation manifest
are different speech acts, per D-d's resolution: engine provenance
(which artifact, which config, which subprocess) versus transformation
lineage (these input hashes, this operator at this version, these
output hashes). `ehr-testing-tools.corpus.operation-manifest/
OperationManifestV1`, written as `operation-manifest.edn` beside a
`dir`/`file` sink's own output, is that second schema -- never reusing
`ManifestV1_1`'s field names for a value they were never shaped to
hold, and never sharing its filename.

### Fields

| Field | Type | Meaning |
|---|---|---|
| `:manifest-kind` | `:operation` | the schema discriminator -- distinguishes this file from `ManifestV1_1`'s own `manifest.edn` before even reading `:schema-version` |
| `:schema-version` | `1` | this schema's own version lineage, independent of `ManifestV1_1`'s `"1.1"` -- the two never share a version lineage |
| `:producer` | map | `{:name :identity :git}` -- this repo's own honest identity, via the `ehrt version` machinery (`cli/repo-identity`, `cli/real-git-describe`). No `:sha256` field: an absent field is honest, a fabricated one is not |
| `:operation` | map | `{:kind :operator-id :operator-version :locator}` -- what was done: the operator applied, its version, and the locator it ran at. `:kind` today is always `:mutate`, the one dir/file-writing operation this repo has; the field exists so a future operation-producer names itself here too, not because more than one exists yet |
| `:written-at` | string | a record-keeping date (D8's exemption list, extended here alongside `:received`/`:captured-at`) -- when this manifest was written, never a generation input |
| `:format` | keyword | the sink's own declared format (`:fhir-json`/`:v2-er7`), read off the sink -- never inferred (this Part's own no-inference law) |
| `:framing` | keyword | the sink's own declared framing |
| `:items` | vector of maps | one entry per file written this call: `{:name :sha256 :input-hash}` -- `:name` the file's path relative to the sink's own `:path`, `:sha256` its content hash, `:input-hash` (optional, present iff the producer actually held it) the content hash of whatever this item was derived from. `mutate` always knows this (its own lineage record's `:parent`); a plain write may not |

### Write discipline: items-then-manifest ordering (ruling 3, carried)

Every item file lands on disk before `operation-manifest.edn` does, so
a process that dies mid-write leaves items without a manifest --
detectable, never a manifest naming items that don't exist.
`write-dir!`/`write-file!` (`ehr-testing-tools.corpus.sink-write`) both
accept an optional `:operation-manifest` argument; when present, the
manifest is written last, unconditionally overwriting any prior
`operation-manifest.edn` at that path -- the outer call's own `:mode`
(`:fail-if-exists`/`:overwrite`) already gates whether the *items* land
at all; the manifest write that follows is not a second gate on the
same directory.

### Never both (ruling 2)

A directory presenting both `manifest.edn` (`ManifestV1_1`) and
`operation-manifest.edn` (`OperationManifestV1`) claims two producers
for the same bytes -- a defect to surface, not an ordering question.
`corpus.intake` rejects the whole intake run `:ambiguous-sidecars`
rather than picking one by precedence. This rules out **A2** (dispatch
intake's existing recognizer by try-order across both schemas instead
of a second, distinctly-named file): an implicit try-order contract is
exactly the kind of unstated precedence rule the never-both rule
refuses to have.

### One fact, one authority (ruling 4)

`operation-manifest.edn` is the directory's own self-description --
portable, readable with no tools running. `corpus.intake`'s catalog
remains the *consumer's* word: its own record after examining the
bytes, enriched from whichever sidecar it trusted -- `:provenance` from
a `ManifestV1_1` sidecar, `:operation-provenance` from an
`OperationManifestV1` one, two distinct enrichment fields, symmetric,
never merged into one.

**Named finding, not resolved here.** Mutate's own per-mutant
`lineage/*.lineage.edn` sidecars are a *third*, pre-existing register
this design does not consolidate: a lineage sidecar's own
`:parent`/`:produced`/`:transformation` now genuinely duplicate
`operation-manifest.edn`'s own `:items[].input-hash`/`:sha256`/
`:operation` for the same mutant, once both exist side by side for the
same `corpus mutate` run. Consolidating the two registers -- making
`operation-manifest.edn` the lineage sidecars' one authority, or vice
versa -- is future work, not built or decided this session; ruling 8
directs this be named, not silently fixed.

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
  existing artifact registry (`ehrt artifact fetch sim`), exactly like
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
- **`sim-harness`** (`projects/conformance/test/ehrt/conformance/sim_harness.clj`)
  grows up: its subprocess seam becomes the `sim` generator source in
  `src/`, with the harness delegating to it; the cross-repo consumer
  loop keeps testing through it, unchanged in spirit (tools/ADR-0013's
  subprocess-only rule is not reopened by this design — superseded in
  practice by ADR-0004's in-process `ehrt sim` mount, which this design
  doc predates).
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
;; SimHospital fixture (test-fixtures/v2/simhospital/, ADR-0011) is the
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

## Part IX — CLI Ergonomics (D8–D14, 2026-07-27, UX-1 capture)

**Why this section lives here rather than in a standalone doc.** SS-1
(Part I–IV above) is about to change the CLI's own input/output surface
(the URL string format, D4); this capture session rules the surface's
*ergonomics* — its defaults and flag names — in the same document SS-1
builds from, so the two land together (D14) rather than costing readers
two migrations. Nothing below changes `src/` — same no-code convention
as the rest of this document (see the header).

### IX.1 The determinism law of defaults (D8)

**Law.** A CLI flag may default only to a pinned constant, or a value
derived deterministically from other pinned inputs (another flag's
value, a hash, a path template). No default may read the clock, the
environment, the network, or the machine.

**The one named exemption.** `corpus intake`'s `--received` currently
defaults to `today` (`cli/help.clj:85`, `corpus/intake.clj:230`'s own
docstring already frames `:received` as "a required, explicit date
string — never read from the wall clock *here*," i.e. inside the core
function; the CLI shell is where the today-default lives). This is a
**record-keeping** date — when a batch was received for cataloging — not
a generation input that determines what bytes get produced. The law
governs the latter: `corpus generate`'s `--reference-date` is exactly
what `--received` is not, which is why Synthea's own generator treats an
unpinned reference date as a reproducibility hazard
(`corpus/generate.clj`'s `reference-date` docstring: "Synthea generates
relative to wall-clock \"now\" unless told otherwise, which would make
every run non-reproducible by construction") while an intake batch's
received-date has no such downstream effect on any byte produced. Every
other wall-clock-shaped default proposed for this CLI is judged against
this same generation-input-vs-record-keeping-date distinction, not
against convenience.

**SS-4 Step 0 (2026-07-28) — D8's named exemption list extended, plus
two adjacent notes from the SS-3 checker pass.** D8's named
record-keeping exemption list gains the spool's own `:captured-at`
(`ehr-testing-tools.corpus.spool`) and a sink manifest's own
`:written-at` (SS-4 Part III, below) alongside `corpus intake`'s
`--received` — every one of the three records when bytes arrived,
landed, or were captured, not what bytes a deterministic run produces,
the same distinction D8 already draws; naming `:written-at` here too,
ahead of its own implementation, keeps this list extended once,
completely, rather than piecemeal across two sessions. The spool's
wall-clock-derived output path (`default-spool-out-dir`,
unlike every other D9-precedent derived path) is named as a deliberate
exception in the same family, not an oversight: a capture is an event,
not a generation, so a stable path would trip the spool's own
fail-if-exists guard on every second capture, inverting the guard from
catching an accidental overwrite to blocking every legitimate one. The
spool's check-then-write atomicity (nothing lands on disk until the
full input is confirmed under-cap and decodes cleanly) buys that
guarantee at a memory cost equal to the cap itself (default 1 GiB, held
fully in memory before any byte is written) — an argument for keeping
the default conservative, not for raising it casually.

### IX.2 Ratified defaults for `corpus generate` (D9)

The zero-flag happy path:

| Flag | Default | Derivation |
|---|---|---|
| `--seed` | `1` | pinned constant |
| `--clinician-seed` | value of `--seed` | derived — one seed to remember, not two |
| `--reference-date` | `20260101` | pinned constant, named beside `default-locale`/`default-timezone` (`corpus/generate.clj:22-24`) — a comment at the definition site states it is intentionally frozen, not "today" |
| `--population` | `5` | pinned constant (OPEN-2: `5` vs. `1`) |
| `--output-dir` | `out/corpus/synthea-s<seed>-p<pop>` | derived from `--seed`/`--population` (ADR-0013, 2026-07-30: moved from `target/corpus/…` to the single tool-owned `out/` root) |
| `--config-path` | a minimal Synthea properties file shipped in `resources/` | pinned artifact, authored in the build session; its content is part of the pin |

**Acceptance property.** `ehrt corpus generate` with no flags must be
byte-reproducible across machines given the same pinned artifacts (the
shipped `resources/` properties file, the locked Synthea/JDK artifact
versions) — the same claim EXP-A4 already proved for an explicit-flags
invocation (`../../components/corpus/docs/experiments/EXP-A4-results.md`), now extended to the
zero-flag case specifically because it is what a first-time reader
actually runs. This makes the quickstart's first command a
reproducibility demonstration, not merely a convenience.

**Determinism probe (2026-07-28, UX-1 build session) — record-keeping
exemption.** Two real generations at the pinned D9 values, into a
freshly-emptied identical output directory, produced byte-identical
corpus payloads (modulo the two canonicalizations EXP-A4 already
registered — the filename timestamp suffix and Synthea's own
`metadata/*.json` run-audit fields) and identical `manifest.edn` content
except for exactly two fields inside `:invocation`: `:started-at` and
`:duration-ms`. Both describe when and how long the subprocess ran, not
a generation input — the same record-keeping-vs-generation-input
distinction D8 already draws for `corpus intake`'s `--received`. These
two fields are named here as the D8-exempt set for the zero-flag
acceptance property's manifest-identity check; every other manifest
field, including `:invocation`'s `:command`/`:args`/`:stdout-path`/
`:stderr-path`/`:stdout-sha256`/`:stderr-sha256`, is asserted equal.

**Output-directory collision (2026-07-28, UX-1 build session) —
addendum, author-directed.** Because `--output-dir` is now a *derived,
stable* path for a given seed/population rather than a required flag a
caller chooses fresh each time, a second zero-flag invocation lands in
the same directory as the first by construction. Probed directly: `corpus.generate`
had no guard against this, and Synthea's own per-file writer throws
`FileAlreadyExistsException` for every already-written patient bundle
on a second run into a non-empty directory — caught internally by
Synthea (its process still exits 0), so the second invocation silently
wrote nothing and `generate!` still returned `result/ok`, with no signal
anywhere that the "regenerated" corpus was actually untouched leftovers
from the first run. Resolved by pulling one piece of Part III's sink
discipline (D3: fail-if-exists is the default) forward into
`corpus.generate` itself, ahead of the SS-1..SS-5 Source/Sink build-out:
`generate!` now rejects with `result/error :output-dir-exists` before
invoking anything if `--output-dir` already exists and is non-empty,
naming the path and hinting at removing it or passing a different
`--output-dir`. This is scoped to this one collision, not a preview of
the general Sink type — SS-4 still owns building `:overwrite`/`:append`
as explicit opt-ins across every sink kind.

### IX.3 One flag vocabulary (D10)

| Concept | Spelling | Replaces |
|---|---|---|
| lockfile path | `--lockfile` | `--lockfile-path` (`corpus generate`'s current spelling, `cli/help.clj:74`) |
| output file | `--out` | — (new; `corpus intake`'s `--out` already uses this spelling and is unaffected) |
| output directory | `--out-dir` | `--output-dir` (`corpus generate`, `corpus mutate`) |
| primary input | positional `PATH`, with `--path` as the explicit twin (existing `gate`/`check` precedent, `cli/help.clj:92-93,106-107`) | `--input` (`corpus mutate`), `--source-dir` (`corpus intake`) |

Old spellings are **removed, not aliased** — pre-release (ADR-0008), one
vocabulary, no deprecation shims to carry into a first release. `docs/
cli.md` and `ehrt help` both render from `cli-spec` (`cli/help.clj`), so
neither can drift from the other or from this table once a build session
applies it.

### IX.4 `ehrt gate PATH` sniffs (D11)

Bare `ehrt gate PATH` dispatches per file via the existing format-sniffing
heuristic (`corpus.intake/sniff-format`, `corpus/intake.clj:101-111`) —
the same cheap, unvalidating, extension-then-content heuristic Part IV's
format inference already reuses (§IV). `gate v2` / `gate fhir` remain as
explicit overrides, and are what a directory mixing both formats
requires: a sniff-dispatched directory containing both `:fhir-json` and
`:v2-er7` files is an **error naming the override**, telling the caller
to run `gate v2`/`gate fhir` explicitly rather than silently splitting
the directory per file. OPEN-1 resolved 2026-07-27 (UX-1 build): the
error default is confirmed, not the silent-split reading.

### IX.5 `corpus mutate` defaults (D12)

`--out-dir` derives as `<input>-mutants/<operator-id>@<version>/` — the
same derived-from-inputs pattern as D9's `--output-dir`. Each registry
entry (`corpus.operators`, `corpus/operators.clj:54-71`'s `Operator`
schema) MAY declare `:default-locator`, its own canonical conviction
target — the seed catalog already documents, per operator, which locator
is verified to convict (e.g. `:corrupt-segment-name`'s docstring names
MSH specifically, `corpus/operators.clj:267`). No operator's default
locator is invented by this capture session: declaring one is
calibration work against `docs/judge-calibration.md`, done by whichever
build session actually authors it. `--locator-path` remains required for
any operator without a declared default.

### IX.6 New conveniences (D13)

- **`ehrt version`** — prints this repo's own version-of-record plus the
  pinned artifact versions read from `artifacts.lock.edn` (Synthea, the
  Temurin JDK, the FHIR validator CLI — ADR-0005's registry).
- **`ehrt artifact fetch --all`** — fetches every artifact the lockfile
  names, collapsing `SETUP.md`'s own three-invocation walkthrough
  (`synthea`, `temurin-jdk`, and, for the T2/integration path,
  `fhir-validator-cli`) into one command.
- **`ehrt doctor`** — runs `SETUP.md`'s verification checklist as a
  command: WSL detection where relevant, Java resolution through the
  artifact registry, artifact cache presence, exit 0/1/2 per the
  existing ladder (`cli/help.clj`'s `exit-codes`). `doctor`'s checklist
  content is drawn from `SETUP.md`, not authored independently, so the
  two cannot silently disagree — the same freshness obligation `docs/
  cli.md` already owes `cli-spec` (OPEN-3 resolved 2026-07-27, UX-1
  build: `doctor` ships in the first release).

### IX.7 Sequencing with SS-1 (D14)

The flag-vocabulary table (D10) and SS-1's URL/source-sink surface
(Part IV) are a **breaking change to the same CLI, twice, if built in
two separate sessions** — this capture session rules that they land
together: either SS-1's own scope grows to include D9–D13, or a
dedicated UX build session lands immediately before SS-1 and SS-1
rebases onto its result. `.agents/plans/corpus-foundations.md` records
both shapes as a proposal for the author to decide at build time (see
that plan's own UX-1 row). Whichever session changes the surface owes
re-verification of `docs/use-cases.md`'s ten command strips end to end,
and the quickstart's structural enforcement (`make quickstart-fresh`,
T0) must stay green with the new zero-flag `generate` as its first
command.

---

## Deferred decisions (record only — not resolved here)

- ~~**D-a** — URL scheme spellings (`dir:` vs `file:` with trailing
  slash; invented schemes for generators).~~ **Resolved 2026-07-28
  (SS-1 build)** — see the Decision Register above.
- **D-b** — whether the `blaze` sink lands before or after the
  IG-pinning blocker clears. They interact: what profile does a
  written resource claim?
- ~~**D-d** — manifest interop: can a `dir`/`file` sink honestly emit a
  `ManifestV1_1` sidecar naming this repo as producer?~~ **Resolved
  2026-07-28 (SS-4b build)** — option A1: a distinct `OperationManifestV1`
  schema, `operation-manifest.edn` — see the Decision Register above.
- ~~**D-c** — whether the `:origin` rename (D6) ships with the first
  source build session (SS-1) or its own micro-session.~~ **Resolved
  2026-07-28 (SS-1 build)** — see the Decision Register above.
- ~~**OPEN-1** — mixed-format directory behavior under bare `gate`
  (D11).~~ **Resolved 2026-07-27 (UX-1 build)** — see the Decision
  Register above.
- ~~**OPEN-2** — whether `corpus generate`'s zero-flag `--population`
  default (D9) is `5` or `1`.~~ **Resolved 2026-07-27 (UX-1 build)** —
  see the Decision Register above.
- ~~**OPEN-3** — whether `ehrt doctor` (D13) belongs in the first
  release or ships after.~~ **Resolved 2026-07-27 (UX-1 build)** — see
  the Decision Register above.
- **OPEN-4** — whether `corpus generate` grows an `--engine` flag now
  that the generator registry (SS-2) names more than one engine kind.
  Raised, not decided, 2026-07-28 (SS-2 build) — see the Decision
  Register above.
- **OPEN-5** — whether a `dir:` Source ever grows framing-awareness
  (a directory containing one or more multi-item files, spooled
  per-file). Raised, not decided, 2026-07-28 (SS-3 build) — see the
  Decision Register above.
- **OPEN-6** — dir-sink `:append` (catalog/manifest merge semantics,
  not byte concatenation). Raised, not decided, 2026-07-28 (SS-4
  build) — `write-dir!` rejects it `:append-unsound` unconditionally
  this session — see the Decision Register above.
