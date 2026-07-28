2026-07-28 — SS-2 (build): the generator registry — synthea re-expressed, sim promoted from harness to src/, SimHospital-ready by shape alone
Context
SS-1 landed the Source/Sink types and the six-scheme parser; `synthea:` and `sim:` currently parse and reject `:unsupported-source-kind`. SS-2 gives them handlers via the design's central unification: a generator source executes its engine into a fresh derived directory (with its manifest) and then IS a `dir` source over that directory — so intake, catalog, lineage, and everything downstream stay untouched, and adding a generator is registering an entry, the same shape as `corpus.operators`. `synthea` is re-expressed as the first registry entry over `corpus.generate`'s existing two-step engine; `sim` is promoted from `test-integration/` harness code into a `src/` engine adapter in its ADR-0013 shape (subprocess-only, never on the classpath), with the harness delegating so the consumer loop now tests through the shipped code path. `simhospital` is deliberately NOT registered — the registry shape is the whole accommodation.
One carry-in first: SS-1's checker pass found the D-c rename half-done — `CatalogEntry` became `:origin` but the sibling `IntakeRecord` (written to `intake-record.edn`) still declares and writes `:source`. Same concept, same colliding word, same namespace. Step 0 finishes it.
This session touches `corpus/generate.clj` and `test-integration/`, both on the ADR-0016 trigger list: T2 owed.
Read first

* `docs/source-sink-design.md` (Parts on generator/reader unification, registry, §5 non-goals incl. the released-sim-artifact evolution note), the closed SS-1 plan row and open SS-2 row
* `src/ehr_testing_tools/corpus/source_sink.clj`, `source_sink_url.clj` (SS-1's types and parser), `intake.clj` (IntakeRecord — Step 0), `generate.clj` (two-step engine, the `:out-dir-exists` guard, UX-1's pinned defaults), `operators.clj` (the registry precedent), `invocation.clj` (the subprocess seam and its fake pattern)
* `test-integration/ehr_testing_tools/sim_harness.clj` (sim discovery, skip-when-absent, manifest assertions) and `smoke_test.clj`
* `notes/ADRs.md` ADR-0013 (sim subprocess-only), ADR-0016 (trigger list), ADR-0017 (source/sink; the artifact-registry evolution for a released sim), ADR-0019
* Sim's `README.md` CLI invocation section (sibling checkout or public repo) — the command shape the adapter drives

Author rulings

1. Step 0 finishes D-c. `IntakeRecord`'s `:source` → `:origin`: schema, the `{:source source-label …}` writer, any reader, and `docs/formats.md` if it documents the intake record. Nothing golden-compares `intake-record.edn`, so no re-baseline; say so in the report after checking rather than assuming. The design doc's D-c row gains one dated sentence noting completion scope.
2. The registry is data, its entries are recipes. A generator entry declares: `:kind` keyword, a param Malli schema (seed and kind-specific params, all with pinned defaults per the D8 law — the zero-param `synthea:` URL must mean exactly what zero-flag `ehr corpus generate` means), a derived-out-dir function (deterministic from params, `target/corpus/<kind>-s<seed>…`, reusing the `:out-dir-exists` fail-fast guard), and an execute recipe driving `invocation.clj`'s seam. Registering `simhospital` is out of scope; the report states what registering one would require (it should be: an entry, an engine artifact or path, and nothing else — if the honest answer is more than that, that is a design finding, not something to fix silently).
3. Resolving a generator Source yields a dir Source. One function owns the unification: validate params → derive out-dir → execute engine → verify the directory materialized non-empty → return the `dir:` Source over it (Result-valued throughout; engine failure, empty output, and pre-existing out-dir are three distinct rejections). Hermetic tests use the invocation fake; the real-engine paths are integration-tier.
4. The sim adapter is ADR-0013-shaped and honest about absence. Discovery order: explicit `:sim-dir` param → `EHR_TESTING_SIM_DIR` env var → the sibling-checkout default the harness uses today. Absent or not-a-sim-checkout → Result rejection `:sim-not-available` naming the three discovery paths tried — never a throw, never a silent skip in `src/` (skip-when-absent is a TEST policy, not adapter behavior). The adapter drives sim's real CLI with an explicit seed always passed; sim's own ManifestV1_1 sidecar is the manifest (tools writes none for sim output — provenance is the generator's word, per the design). Record in the adapter's ns docstring the intended evolution: a released sim becomes a pinned artifact-registry entry (ADR-0017 §5), at which point discovery gains a fourth, preferred path.
5. The harness delegates; the consumer loop tightens. `sim_harness.clj` calls the `src/` adapter for discovery and invocation, keeping its own skip-when-absent wrapping and its manifest assertions. The smoke test stays green unchanged in intent; if its fixture setup duplicates discovery logic, that duplication dies here. Net effect stated in the report: the consumer loop now exercises the shipped sim path, not parallel harness code.
6. CLI surface: generator URLs land at intake; `corpus generate` is untouched. `ehr corpus intake sim:?seed=42 --out DIR` (and `synthea:` likewise) resolves the generator per ruling 3 and intakes the resulting directory — the one-command generate-and-catalog path. `ehr corpus generate`'s verb, flags, and defaults do not change this session; whether it grows `--engine` is recorded as a new OPEN item, not decided. Help text gains the generator-URL mention in the corpus group; `make cli-doc` regenerates.
7. Strips: one new, others re-verified. If a sim checkout is present, add an eleventh `docs/use-cases.edn` strip — sim as a corpus source via the intake URL — verified for real like the other ten; if absent, do not add an unverified strip (unearned specificity), record the errand instead. Affected existing strips (intake at minimum) re-run.
8. Tiers: T0 per commit; T1 + T2 before the final commit; `make coverage` before session close per AGENTS.md. Report wall times and whether the sim-source integration test ran or skipped (and why).

Steps
Step 0 — Finish D-c
Per ruling 1. Commit: `fix: IntakeRecord :source -> :origin — D-c completed across the namespace (SS-1 checker finding)`
Step 1 — Registry (red→green, fakes)
Per ruling 2, `synthea` as the first entry over `generate`'s engine. Commit: `feat: generator registry — synthea re-expressed as a registry entry; params carry D8-law pinned defaults (SS-2)`
Step 2 — Generator→dir resolution
Per ruling 3, hermetic first. Commit: `feat: resolving a generator Source executes its engine and yields a dir Source — the unification, Result-valued (SS-2)`
Step 3 — The sim adapter + harness delegation
Per rulings 4–5. Commit A: `feat: sim engine adapter in src/ — subprocess-only (ADR-0013), three-path discovery, :sim-not-available is a value` Commit B: `refactor: sim harness delegates to the shipped adapter; consumer loop now tests the real path`
Step 4 — CLI intake of generator URLs
Per ruling 6; the new OPEN item recorded in the design doc's register. Commit: `feat: ehr corpus intake accepts generator URLs — generate-and-catalog in one command (SS-2)`
Step 5 — Strips and docs
Per ruling 7; `make cli-doc`; freshness gates. Commit: `docs: sim-as-source strip (verified) [or errand recorded]; intake strips re-verified; cli regenerated`
Step 6 — Tiers, plan row, archive, push
Per ruling 8; SS-2 row → Done with any fence notes; archive; push. Commit: `prompts: archive 2026-07-28 ss-2 build session`
Final report
Step 0 re-baseline check outcome; the honest answer to ruling 2's "what would registering simhospital require"; sim discovery paths as shipped and which the integration run used (or the skip reason); the consumer-loop tightening statement (ruling 5); strip 11 verified or errand recorded; T0/T1/T2 wall times + coverage; red→green evidence; deviations.
