# Provenance — SimHospital `messages.out`

Vendored under [ADR-0011](../../../../notes/ADRs.md) (external data
artifacts vendor into the tree; engine artifacts stay in the registry).
This directory holds a third-party artifact, not fixtures this repo
authored: the corpus, its upstream `LICENSE`, and this manifest travel
together.

## Upstream

| | |
|---|---|
| Project | Google Simulated Hospital (SimHospital) |
| Repository | `https://github.com/google/simhospital` |
| Pinned commit | `e69eef7de3c01da3f110dda19200f84d7776fb6b` (`master`, committed 2023-08-09) |
| Corpus source | `https://raw.githubusercontent.com/google/simhospital/e69eef7de3c01da3f110dda19200f84d7776fb6b/docs/artifacts/messages.out` |
| License source | `https://raw.githubusercontent.com/google/simhospital/e69eef7de3c01da3f110dda19200f84d7776fb6b/LICENSE` |
| License | Apache License 2.0 (upstream `LICENSE`, vendored verbatim beside this file) |
| Retrieved | 2026-07-26 |

Fetched at the resolved commit SHA, never at the branch ref: archived is
a social status, not a technical guarantee that `master` cannot move.

**Upstream status: archived.** `https://api.github.com/repos/google/simhospital`
reported `"archived": true` and `"pushed_at": "2024-03-20T02:12:42Z"` when
this artifact was retrieved (2026-07-26); the API response carries no
`archived_at` value, so the archive *date* of 2025-03-28 rests on
`docs/research/HL7v2-sanitized-corpus-research.md`'s citation, not on
anything observed here. Vendoring is what makes that irrelevant to this
repo's test suite: nothing here fetches from that URL again.

## Bytes

| | |
|---|---|
| File | `messages.out` |
| Size | 1,158,713 bytes |
| sha256 | `fa9719a5f157391dcf78197e4239bce8af0382ae40b903d019a2773a1a9ff520` |

`LICENSE` is 11,357 bytes, sha256
`58d1e17ffe5109a7ae296caafcadfdbe6a7d176f0bc4ab01e12a689b0499d8bd`.

## Framing — do not let git translate newlines

The file mixes two terminators, deliberately, and both are load-bearing:

- **Segments within a message** are terminated by a bare **CR**
  (`\r`) — classic ER7, exactly what a real interface sends. 9,938 CR
  bytes; zero CRLF pairs.
- **Messages are separated from each other** by a blank **LF** line
  (`\n\n`): the last segment of each message is *not* CR-terminated, and
  the file ends with a final `\n\n`. Splitting the whole file on `\n\n`
  yields 1,014 blocks, the last empty — hence 1,013 messages.

A checkout that normalizes line endings would rewrite every one of those
bytes and silently invalidate every round-trip assertion this corpus
exists to support. `.gitattributes` at the repo root carries
`test/fixtures/v2/simhospital/messages.out -text` for exactly this
reason, alongside the pre-existing rule for the hand-written
`test/fixtures/v2/*.hl7` fixtures.

## Structural counts (reproduced 2026-07-26 against the vendored copy)

1,013 messages, all HL7 v2.3 (`MSH-12` = `2.3` on every message).

| Message type | Count |
|---|---|
| `ORU^R01` | 610 |
| `ADT^A01` | 400 |
| `MDM^T02` | 2 |
| `ADT^A34` | 1 |

403 distinct `PID-3.1` MRNs. Messages per patient: 3×1, 345×2, 17×3,
3×4, 3×5, 10×6, 4×7, 13×8, 4×9, 1×14 — genuine longitudinal subseries,
which is why this corpus was chosen over broader but unlinked sample
collections.

Every one of the 1,013 `PID` segments carries a repeated `PID-3`
(`<MRN>^^^SIMULATOR MRN^MRN~<NHS number>^^^NHSNBR^NHSNMBR`), which makes
this corpus a direct exercise of the repetition-preservation property
`test/fixtures/v2/adt-a01-admit-repeated-identifiers.hl7` was
hand-written to check.

**Event coverage is narrow, by construction of this particular run:** no
`ORM^O01`, no `ADT^A02`, no `ADT^A03`. Order/result correlation and
patient movement are not exercised here; filling that gap is Phase B of
`docs/research/HL7v2-sanitized-corpus-research.md` (a pinned SimHospital
run against a custom pathway), not a matter of mixing in unrelated
samples.

## Handling: internal test input only

The corpus is synthetic by construction — SimHospital generates it, it
is not a de-identified real interface dump — and that is why it is
acceptable as committed test input. It is nonetheless **realistic**:
UK-flavored names, addresses, and **valid-format NHS numbers**
(`docs/research/HL7v2-sanitized-corpus-research.md` §1). "Synthetic" and
"safe to publish" are not the same claim.

**No corpus derived from this file may be published or redistributed
without first walking §5 ("Privacy and release gate") of
`docs/research/HL7v2-sanitized-corpus-research.md`** — identifier
replacement into a reserved namespace with referential integrity
preserved, reserved names/domains/phone ranges, regeneration of free
text, a full-field (not PID-only) scan, and a published sanitization
manifest. Using the file in place, inside this repo's test suite, is not
publication; emitting a mutated or reframed derivative into anything
shipped is.

Redistribution of the file *as vendored* is what Apache-2.0 permits and
what the accompanying `LICENSE` satisfies; the §5 gate is this repo's
own additional bar for anything it would present as a sanitized corpus.

## Consumers

`test/ehr_testing_tools/corpus/simhospital_corpus.clj` (test support
loader) and the `corpus.er7` / `corpus.intake` slice tests that read
through it. Per ADR-0011 and the ruling recorded with it, the exhaustive
1,013-message verifications are one-time probes registered in
`notes/facts-register.md` (F23–F27), not per-push assertions: git
content-addresses these bytes, so the suite guards path-and-framing
behavior rather than re-proving corpus integrity on every run.
