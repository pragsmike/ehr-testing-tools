# EXP-B2 — Results

## Metadata

- **Experiment:** EXP-B2
- **Date:** 2026-07-24
- **Executor:** Claude Code (P4 session)
- **HEAD at execution:** `d2214f6` (EXP-B2 protocol + fixtures commit)
- **Protocol:** [`docs/experiments/EXP-B2.md`](EXP-B2.md)

## Environment record

| Field | Value |
|---|---|
| OS / kernel | WSL2 Ubuntu 20.04 (orchestrator host) |
| JVM(s) used | Orchestrator (Clojure process, this execution): Eclipse Temurin 17.0.19+10 (`JAVA_HOME` forced to the extracted, registry-resolved JDK artifact) -- required because `hapi-fhir-base`/`hapi-fhir-structures-r4` 8.2.0 are compiled to class file version 61.0 (Java 17+); the orchestrator's own default `java` is 11 and cannot even load the classes. Generator subprocess (for the sample corpus): also Temurin 17.0.19+10, same as P3/EXP-A4. |
| Locale / timezone (host default) | Irrelevant to this experiment's own execution (no date/locale-sensitive parsing under test); the sample corpus itself was generated with locale/timezone forced per `corpus.generate`'s defaults (`en-US` / `UTC`), same as every other generation this repo does. |
| Artifact(s) resolved | `synthea` v4.0.0 (`artifacts.lock.edn`); `temurin-jdk` 17.0.19+10 (`artifacts.lock.edn`, kind `:runtime`) |
| Config file(s) used | `config/synthea/synthea.properties`, sha256 `ead0388b86d5d60bff86d8475cd65d6c3d8ef7cdeb5f7b8a58b55c911ad79bb7` (same config EXP-A4 used; corpus regenerated fresh for this session -- `out/` was gone, exactly as the protocol anticipated) |

Sample corpus: 8-patient population, seed 100 / clinician-seed 555 /
reference-date 20260101 (EXP-A4's final pinned settings), generated
into `out/exp-b2-corpus/` (gitignored, not committed -- this results
file plus the manifest excerpt below is the provenance record).
Manifest excerpt: `{:seeds {:master 100, :clinician 555},
:engine-params {:reference-date "20260101"}, :generator {:name
"synthea", :version "4.0.0", ...}, :runtime {:name "temurin-jdk",
:version "17.0.19+10", ...}}` -- 12 patient FHIR files plus
`hospitalInformation*.json` and `practitionerInformation*.json` (14
files total), all sampled (not a subset -- the whole small corpus).

## Per-round findings

### Round: (a) FHIR JSON via HAPI FHIR (`hapi-fhir-structures-r4` 8.2.0, `newJsonParser` -> `parseResource` -> `encodeResourceToString`, pretty-print on)

| Divergence observed | Field(s) | Classification | Example |
|---|---|---|---|
| Every `Bundle.entry[].resource.id` is dropped entirely on re-serialization, for every entry in every one of the 14 sampled files (14/14 files, effectively every entry -- hundreds per file) | `entry[].resource.id` | **content-normalizing** (bordering on lossy -- see discussion) | Original entry 0's resource: keys include `id` = `"9f8348fd-3faf-fa1a-e7de-1ec5efe570e3"`; `fullUrl` = `"urn:uuid:9f8348fd-3faf-fa1a-e7de-1ec5efe570e3"`. HAPI's re-serialized entry 0's resource: no `id` key at all (`(contains? new-e0 "id")` => `false`). `fullUrl` is preserved unchanged. Verified this is a genuine structural drop, not a formatting artifact, via `clojure.data/diff` on the fully parsed (data.json) original vs. re-serialized structures: every entry shows up in the "only in original" side as `{"resource" {"id" ...}}`, nothing on the "only in new" side. |
| Field/key order changes throughout every resource (e.g. `meta` moved ahead of where `id` would have been) | every resource in every entry | **key-reordering** | Original: `"resourceType": "Patient",\n "id": "9f8348fd-...",` ; HAPI reserialization: `"resourceType": "Patient",\n "meta": {...` (no `id` at all, consistent with the drop above, but also genuinely a different field order for the fields that *do* survive) |
| Every file differs from its first byte of divergence onward; re-serialized files are consistently 1-2% smaller than the originals | whole file | (subsumes the above two, restated as an aggregate) | `Brandon214...json`: 1,859,762 bytes original vs. 1,830,953 bytes reserialized |

**Discussion — why "content-normalizing" and not flatly "lossy":** in this
specific corpus, every dropped `resource.id` happens to be recoverable
from the sibling `entry.fullUrl` (`urn:uuid:<same-id>`), because
Synthea generates both from the same UUID for `transaction`-type
Bundles. That coincidence is what keeps this out of the strict
"lossy" bucket rather than in it -- but it is not a property EXP-B2
verified holds in general (a `batch`-type Bundle, a differently-
authored FHIR file, or a resource without a matching `fullUrl` could
lose the id with no recovery path at all). Practically: this
representation is disqualified as a mutation substrate regardless of
which bucket it's filed under, since Mutate's law requires the
canonicalized diff to touch *only* the declared locator/target -- an
engine that silently drops an unrelated field on every single record
fails that law before mutation logic even runs.

### Round: (b) FHIR JSON via plain Clojure data (`clojure.data.json` 2.5.2, `read-str` -> `write-str`, no modification)

| Divergence observed | Field(s) | Classification | Example |
|---|---|---|---|
| Whitespace/formatting only -- original is Jackson-pretty-printed (2-space indent, `"key": value`), `write-str`'s default output is compact (no indentation, `"key":value`) | whole file (formatting only) | **whitespace-canonical** | Original starts `"{\n  \"resourceType\": \"Bundle\",\n  \"type\": \"transaction\"..."`; round-tripped starts `"{\"resourceType\":\"Bundle\",\"type\":\"transaction\"..."` |
| None (content, key set, key order, and values) | -- | -- | Deep structural check across all 15,588 distinct map-paths in the largest sampled file: every key ordering at every nesting level is identical between the original parse and the parse-of-the-rewritten-output; `(= parsed reparsed)` is `true` for every file; a second round-trip is byte-identical to the first (idempotent) |

**Note on key order specifically:** `clojure.data.json/read-str`
builds ordinary Clojure maps; JSON's own spec treats object key order
as insignificant, so "order preserved" here isn't a language
guarantee -- it was verified empirically (15,588 map-paths, one file)
rather than assumed. What actually matters for Mutate's law is
determinism/idempotence (confirmed directly), not fidelity to
Synthea's original ordering choice: `canon(base)` and `canon(mutant)`
both pass through the same read+write pipeline, so any incidental
reordering (were it to occur) would apply identically to both sides
of the diff and cancel out. This round's finding is stronger than
that minimum bar, though -- order was empirically identical to the
original too, not merely internally consistent.

### Round: (c) HL7 v2 ER7 via HAPI HL7v2 (`hapi-structures-v24` 2.6.0, `PipeParser.parse` -> `PipeParser.encode`, no modification)

| Divergence observed | Field(s) | Classification | Example |
|---|---|---|---|
| None (4 of 5 fixtures) | -- | -- | `adt-a01-admit.hl7`, `adt-a01-admit-repeated-identifiers.hl7`, `adt-a02-transfer.hl7`, `adt-a03-discharge.hl7`: byte-identical, confirmed by both direct string comparison and independent sha256 |
| Trailing empty fields stripped on re-encode (1 of 5 fixtures -- the one deliberately authored to probe this) | PID-18 (deliberately populated with 6 trailing empty subfields, then nothing) | **content-normalizing** | `adt-a08-update-trailing-empty-fields.hl7`: original PID segment ends `...(217)555-0142||||||\rPV1|1|I|...` (six trailing empty `\|` fields before the segment terminator); re-encoded: `...(217)555-0142\rPV1|1|I|...` (all six stripped). Confirms the "known suspect: trailing-delimiter canonicalization" named in `docs/experiments.md`'s EXP-B2 row -- not a rumor, reproduced directly against a fixture authored specifically to trigger it. |

## Protocol amendments made

None. The protocol as written executed without correction -- every
representation and input set behaved as scoped, and the five-way
classification vocabulary was sufficient for every divergence found
(no "doesn't fit any category" case arose).

## Acceptance verdict

- **Acceptance criterion (quoted from the protocol):** "Every
  representation × input-set combination has every byte-level
  difference classified with an example, or an explicit 'none —
  byte-identical' row when there is none; the pre-authorized decision
  rule (below) is applied, not re-litigated, once the classification
  table is complete."
- **Met?** Yes. All three representations × their input sets are
  covered above; every divergence has a classification and a concrete
  example; every representation with zero divergence on some or all
  inputs states so explicitly rather than omitting a row.
- **Stop condition triggered?** No. Every observed difference fit one
  of the five pre-defined categories (content-normalizing,
  key-reordering, whitespace-canonical, or none); nothing required the
  "resists classification" escape hatch.

## Applying the pre-authorized decision rule

Quoted from the protocol: "mutation operates on the representation
whose round-trip is faithful (or faithful-modulo-a-registered-
canonicalizer). Expectation: plain-data JSON for FHIR; if HAPI FHIR's
round-trip is also faithful, plain data is still preferred (fewer
moving parts) and HAPI FHIR is recorded as a parse-validation aid
only, not the mutation substrate."

**Applied, not re-decided:** Mutation operates on **FHIR JSON as plain
Clojure data** (`clojure.data.json`). Its round-trip is faithful
modulo whitespace formatting only (whitespace-canonical, and cheaply
neutralized by canonicalizing both sides of any comparison through the
same read+write pipeline, per pattern nursery #3). HAPI FHIR's
round-trip is **not** faithful even in the weaker sense the rule
anticipated -- the expectation was "faithful, but plain data still
preferred for fewer moving parts"; what EXP-B2 found instead is that
HAPI FHIR silently drops `resource.id` on every entry of every file
tested, a content-normalizing change no mutation law could tolerate
underneath it. HAPI FHIR remains available in `deps.edn` as a
parse-validation aid (e.g. confirming a mutant still parses as
*some* valid-shaped FHIR resource where that's useful), never as the
representation mutation logic edits.

For HL7 v2: `PipeParser`'s round-trip is faithful for realistically-
populated messages and only diverges (content-normalizing, trailing-
delimiter stripping) on a message deliberately authored to end a
segment in empty fields. This is recorded as a finding, not acted on
-- v2 mutation is out of scope this session (deferred to post-EXP-A3,
per the plan file), regardless of this result.

## Artifacts produced

| Artifact | Path | Hash |
|---|---|---|
| Sample corpus (8 patients, EXP-A4's final pinned settings) | `out/exp-b2-corpus/` (not committed, gitignored per ADR-0005) | Manifest: `{:seeds {:master 100, :clinician 555}, :engine-params {:reference-date "20260101"}, :generator {:sha256 "ed43c20ad4...1e6ecc1"}, :runtime {:sha256 "d8afc26375...9037d331"}}` (full manifest in `out/exp-b2-corpus/manifest.edn`) |
| `docs/experiments/EXP-B2.md` | protocol | -- |
| `test/fixtures/v2/*.hl7` | 5 fixtures | committed in the prior session commit (`EXP-B2 protocol + fixtures`) |
| `deps.edn` HAPI FHIR/HL7v2 additions | exact-pinned deps | facts register F13 |

## Rubric self-score

| Criterion | Met? | Evidence |
|---|---|---|
| 1. Every finding is classified | Yes | Every row in every per-round table carries a classification (content-normalizing / key-reordering / whitespace-canonical / none) |
| 2. Environment record is complete | Yes | Every field in the Environment record table above is filled in, including the JVM-version subtlety this experiment specifically required (JAVA_HOME forced to Java 17 for the orchestrator itself, not just the generator subprocess) |
| 3. Amendments are justified | Yes | States explicitly: none needed |
| 4. Verdict is traceable to criteria | Yes | The Acceptance verdict section quotes the protocol's Acceptance text verbatim and states plainly it was met |
| 5. No unexplained divergences | Yes | Every observed byte-level difference (HAPI's id-drop, HAPI's reordering, data.json's whitespace, v2's trailing-field stripping) appears in a findings row with a classification and a concrete example; the 4/5 zero-divergence v2 fixtures are stated as "none" rows, not omitted |

All five criteria met.
