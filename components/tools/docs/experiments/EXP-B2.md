# EXP-B2 — Parse→serialize round-trip fidelity

**Objective.** Characterize parse→no-op→serialize fidelity for the
representations the mutation capability will operate on: (a) FHIR JSON
via HAPI FHIR's parser, (b) FHIR JSON via plain Clojure data
(`clojure.data.json` read/write), (c) HL7 v2 ER7 via HAPI HL7v2's
`PipeParser` (known suspect, per `docs/experiments.md`: trailing-
delimiter canonicalization).

**Decision informed.** The mutation-layer design: mutate parsed trees
vs. encoded strings; the intended-diff-only invariant Mutate's law
depends on (`docs/pipeline.edn`) — a representation whose round-trip
silently rewrites bytes it wasn't asked to touch would corrupt that
invariant before mutation logic even runs.

**Apparatus.** HAPI FHIR 8.2.0 (`hapi-fhir-base`,
`hapi-fhir-structures-r4`) and HAPI HL7v2 2.6.0 (`hapi-base`,
`hapi-structures-v24`), both exact-pinned in `deps.edn` (facts
register F13); `clojure.data.json` (already a dependency, F-free —
already exact-pinned pre-P4); a byte-diff harness comparing original
bytes to re-serialized bytes; two input sets:

- **FHIR JSON**: files sampled from the P3/EXP-A4 corpus (`out/` —
  regenerated via a small population from the pinned EXP-A4
  configuration if not present, since the manifest makes this cheap
  and the corpus itself was never committed, per ADR-0005).
- **HL7 v2 ER7**: 3–5 hand-authored ADT fixture messages committed
  under `components/tools/test-fixtures/v2/` (MSH/EVN/PID/PV1, v2.4, realistic field
  population — not minimal skeletons, so the round-trip is exercised
  against messages that look like something a real interface would
  send).

**Procedure.**

1. For representation (a) — FHIR JSON via HAPI FHIR: for each sampled
   FHIR file, parse with `FhirContext.forR4().newJsonParser()`,
   re-serialize with no modification, byte-diff original vs.
   re-serialized.
2. For representation (b) — FHIR JSON via plain Clojure data: for each
   sampled FHIR file, `clojure.data.json/read-str` then
   `clojure.data.json/write-str` with no modification, byte-diff
   original vs. re-serialized.
3. For representation (c) — HL7 v2 ER7 via HAPI HL7v2: for each
   fixture message, parse with `PipeParser`, re-encode with no
   modification, byte-diff original vs. re-encoded.
4. For every byte-level difference found in any of the three rounds,
   classify it:
   - **none** — byte-identical.
   - **whitespace-canonical** — differs only in whitespace/formatting
     (indentation, trailing newline) with no semantic content change.
   - **key-reordering** — object/field order changed, values did not.
   - **content-normalizing** — a value itself was rewritten (e.g. a
     canonicalized delimiter, a re-formatted date, a stripped trailing
     field) without being asked to.
   - **lossy** — content present in the original is absent from the
     re-serialized form.
   Every difference gets a concrete example (a diff snippet), not just
   a category label.

**Expected artifacts.**

- `docs/experiments/EXP-B2-results.md` (this template:
  `docs/experiments/results-template.md`, self-scored against
  `docs/experiments/results-rubric.md`).
- `docs/experiments.md`'s EXP-B2 row updated with the executed date.
- `components/tools/test-fixtures/v2/*.hl7` — the hand-authored ADT fixtures, committed
  (small, deterministic, not gitignored the way generated corpora are).

**Acceptance.** Every representation × input-set combination has every
byte-level difference classified with an example, or an explicit "none
— byte-identical" row when there is none; the pre-authorized decision
rule (below) is applied, not re-litigated, once the classification
table is complete.

**Pre-authorized decision rule** (from the design channel — applied by
the executing session, not decided by it): mutation operates on the
representation whose round-trip is faithful (or faithful-modulo-a-
registered-canonicalizer). Expectation: plain-data JSON for FHIR; if
HAPI FHIR's round-trip is also faithful, plain data is still preferred
(fewer moving parts) and HAPI FHIR is recorded as a parse-validation
aid only, not the mutation substrate. For HL7 v2: record findings:
v2 mutation is out of scope for this session (deferred to post-EXP-A3,
when v2 generation exists) regardless of what EXP-B2 finds about
`PipeParser`'s fidelity.

**Stop condition.** A representation whose round-trip differences
resist classification (a difference that doesn't fit any of the five
categories above) is recorded as an open finding, not silently
dropped. Effort cap: this protocol is scoped to round-trip
characterization only — it does not investigate *why* HAPI's parser
behaves a given way beyond what's needed to classify the observed
difference.
