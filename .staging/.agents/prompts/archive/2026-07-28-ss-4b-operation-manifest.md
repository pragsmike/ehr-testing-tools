2026-07-28 — SS-4b (capture + build): D-d resolved — the operation manifest (A1), sink emission, the composability law completed
Context
The author has ruled D-d: option A, sub-choice A1 — a distinct, versioned manifest schema for operation-producers, under its own filename `operation-manifest.edn`. `ManifestV1_1`, sim's emitters, and ADR-0012's mounting clause are untouched by construction, the "eventually sim's mirrored schema" coordination in D-d's own text never triggers, because sim neither emits nor reads operation manifests. The doctrinal ground: a generator manifest and an operation manifest are different speech acts — engine provenance (which artifact, which config, which subprocess) versus transformation lineage (these input hashes, this operator at this version, these output hashes) — and forcing the second into the first's vocabulary is the ADR-0009/ ADR-0017 incoherence class one level down. Option B (proxy values in V1_1 fields) is rejected for institutionalizing fabricated identity in a file format.
This session captures the schema (design section + ADR + D-d resolution) and builds it: dir/file sinks emit, intake recognizes, and the composability property gains its deferred `:origin`/provenance half. It is one session because the decision is fully made; the capture is Steps 1–2, the build is 3–6.
Read first

* `docs/source-sink-design.md` D-d row (the probe evidence), Part III (composability law, its reduced-this-session note), the SS-4 plan row's deferred-half note; `notes/ADRs.md` ADR-0014 (sim: manifests as consumer evidence), ADR-0016 (trigger list — apply it honestly, see ruling 8), ADR-0017
* `src/ehr_testing_tools/corpus/manifest.clj` (V1_1 — the schema this one deliberately is not), `intake.clj` (`sidecar-result`, the enrichment path, and today's lineage fields — ruling 4's reconciliation), `mutate.clj` (what lineage the producer actually knows), `sink_write.clj` (items-then-manifest ordering seam), `cli.clj` (`ehr version` identity machinery — the producer field's source)
* `test/…/composability` property from SS-4 Step 2 (the half to complete)

Author rulings

1. The schema is OperationManifest, version 1, own file. Filename `operation-manifest.edn`. Required: a schema discriminator (`:manifest-kind :operation`, `:schema-version 1`); `:producer` — this repo's honest identity via the `ehr version` machinery (name + git-describe/pre-release string; NO sha256 field — an absent field is honest, a fabricated one is not); `:operation` — what was done (`:kind` e.g. `:mutate`, and its real params: operator-id@version, locator, whatever the producer actually held); `:written-at` (already on D8's exemption list); `:format`, `:framing`; `:items` — per output file: name, sha256, and where the producer knows it, the input content hash it derives from (mutate knows this; a plain write may not — the field is per-item optional, present iff known, the no-verdict cause-pairing shape applied to provenance).
2. Two sidecars, two speech acts, never both. Intake gains a second recognizer for `operation-manifest.edn` with its own enrichment path (`:origin` from `:producer`, lineage enrichment from `:items`), symmetric to V1_1's. A directory presenting BOTH sidecars is rejected `:ambiguous-sidecars` — loud, named, never a precedence rule; a directory claiming two producers is a defect to surface, not an ordering to pick.
3. Items-then-manifest ordering (SS-4 ruling 3 carried): the manifest is written last; a torn write is detectable as items-without-manifest.
4. One fact, one authority — the register reconciliation. The operation manifest is the directory's self-description: the producer's word, portable, readable by any future consumer with no tools running. The catalog remains the consumer's word: intake's record after examining the bytes, enriched from whichever sidecar it trusted. Mutate's existing lineage fields in the catalog are fed, on re-intake, FROM the operation manifest's `:items` — intake copies the producer's claim and marks its provenance as sidecar-derived exactly as the V1_1 path does. No third register; if the agent finds mutate writing lineage anywhere that would now duplicate the manifest's content as a second authority, that is a finding for the report and a one-line design-doc note, not a silent consolidation.
5. The composability property completes. The SS-4 property gains its provenance half: write a generated item set through a dir sink → intake → content hashes equal AND `:origin` reflects the manifest's producer AND per-item input-hash lineage survives where the producer supplied it. The reduced-property note in the design doc gets its dated closure.
6. Registers. D-d → Resolved (A1), dated, in the Decision Register; a design-doc section specifying the schema (the ADR holds the why, the design doc the what); ADR at the next number verified at run time — the decision, the speech-act rationale, B's rejection with the unearned-specificity ground, A2's rejection (dispatch-by-try-order as an implicit contract, and sim's filename under ADR-0012 opinions), and the never-both rule of ruling 2. OPEN-6's rationale gains one sentence: dir-append's manifest-merge question now has a concrete schema to merge, still open.
7. Surface and strips. No CLI flags change; mutate's dir writes simply start emitting. The mutate and loopback strips re-verify (the loopback is unaffected — stdout sinks still emit nothing, per the design's byte-stream form of the law; confirm the strip still passes rather than assuming). `make cli-doc` only if help text changed.
8. Tiers, honestly. Apply ADR-0016's trigger list as written and state the conclusion in the report: if `test-integration/` is untouched and no listed namespace changes, T0 + T1 suffice; if the session ends up touching a trigger, T2 is owed. Coverage before the final commit either way. Pause before push.

Steps
Step 1 — Design section + D-d resolution
Per rulings 1, 4, 6 (design-doc half). Commit: `docs: D-d resolved A1 — the operation manifest specified; one fact, one authority (capture)`
Step 2 — ADR
Per ruling 6 (ADR half). Commit: `adr: operation manifest — distinct schema, distinct filename; generator and operation manifests are different speech acts`
Step 3 — Schema + emission (red→green)
`operation-manifest.clj` (or the house-style home), Malli schema, builder from mutate/sink context, items-then-manifest wiring in `write-dir!`/file-sink paths. Commit: `feat: dir/file sinks emit operation-manifest.edn — producer's honest identity, per-item lineage where known (SS-4b)`
Step 4 — Intake's second recognizer + never-both
Per ruling 2, red→green including the `:ambiguous-sidecars` case. Commit: `feat: intake recognizes operation manifests; two sidecars in one directory is a named rejection (SS-4b)`
Step 5 — The property, completed
Per ruling 5. Commit: `test: composability law complete — hashes, origin, and supplied lineage survive the sink->intake loop (SS-4b acceptance)`
Step 6 — Strips, plan, registers closed
Per rulings 6–7; SS-4's deferred-half note closed in the plan. Commit: `docs: strips re-verified; SS-4 deferred half closed; reduced-property note dated closed`
Step 7 — Tiers per ruling 8, archive (pause before push)
Commit: `prompts: archive 2026-07-28 ss-4b session`
Final report
The schema as shipped (field list); the ruling-4 duplication check's outcome; the never-both rejection's test evidence; the completed property's parameters and results; the ruling-8 tier conclusion with wall times + coverage; ADR number taken; red→green evidence; deviations.
