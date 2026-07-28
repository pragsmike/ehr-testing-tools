2026-07-28 — SS-1 (build): Source/Sink types, URL⇄map parser, dir/file over the new types — preceded by the toolchain upgrades (git, Java 21)
Context
SS-1 rebases onto UX-1's landed surface (positional `PATH`, the D10 vocabulary, ADR-0019) and implements the first slice of `docs/source-sink-design.md`: the `Source`/`Sink` schemas, the URL⇄map parser with its round-trip law (D4), and the `dir`/`file` kinds, with `corpus.intake` called through the new types under a golden-catalog comparison proving zero behavior change. Two environment items are folded in first by author instruction: (1) upgrade git to current — this machine's older git emits fsmonitor stderr noise that has already caused one real shipped bug (leaking into `ehr version`'s git identity, found and fixed in UX-1); (2) Java 21, in BOTH of its roles here: the system JDK that runs the orchestrator (SETUP.md apt line, CI `setup-java`) and the pinned `temurin-jdk` `:runtime` artifact in `artifacts.lock.edn` that runs Synthea and the validator. The lockfile change puts `artifact.clj` semantics in play, so this session owes T2 regardless of the source/sink work.
Two capture-era open decisions are ruled for this session (author, 2026-07-28, via the design channel): D-a — URL scheme spellings — and D-c — the `:origin` rename ships in this session, sequenced AFTER the golden comparison passes. D-b (Blaze vs IG-pinning) remains open and untouched.
Read first

* `docs/source-sink-design.md` (all of it; especially D1–D7, D-a/D-c rows, Part IX), `notes/ADRs.md` ADR-0017, ADR-0019, ADR-0005 (lockfile), ADR-0016 (tiers, trigger list)
* `src/ehr_testing_tools/corpus/intake.clj` (CatalogEntry, `:source` field, sniffing), `artifact.clj`, `cli/help.clj` + dispatch, `corpus/generate.clj` (only as intake's upstream for the golden corpus)
* `artifacts.lock.edn`, `SETUP.md` (apt line ~52, fetch lines ~28/129/195), `.github/workflows/ci.yml` + `integration.yml` (java-version pins, and integration.yml's artifact-pin comment block)
* `notes/facts-register.md` F12 (the JDK-17 verification row this session's new row parallels)

Author rulings

1. Step 0 is author-present. Upgrading git (git-core PPA) and installing the system JDK 21 need sudo; pause and ask rather than assuming credentials. After upgrading: verify `git --version`, verify the fsmonitor stderr noise is gone (probe the same operation that leaked in UX-1), verify hooksPath/pre-push still fire, and run `ehr doctor` — all four are the acceptance for Step 0. UX-1's defensive stderr handling in `ehr version` STAYS (belt and suspenders; other machines have old git).
2. The Temurin 21 pin is evidence, not memory. Update the lockfile entry to the current Temurin 21 LTS release: the sha256 is computed from the downloaded bytes AND cross-checked against Adoptium's published checksum — never transcribed from anywhere else. New facts-register row (F-next) recording the verification, paralleling F12; `:acquired` is today; license note carries forward (GPLv2+CE — verify the 21 release states the same before writing it). Update: SETUP.md's three version strings and its apt line (`openjdk-21-jdk`), both workflows' `java-version: '21'`, integration.yml's pin comment, `docs/components.md`'s JDK section, and any doctor expectation that names 17. T0 + a real `artifact fetch` + T2 prove the engines run under 21.
3. D-a ruled — scheme spellings: `file:` (single file), `dir:` (directory tree — a distinct scheme, no trailing-slash magic), `stdin:`, `synthea:`, `sim:`, `blaze://host:port/path?query=…`. Format/framing ride as query params (`?format=v2-er7&framing=er7-multi`). Only `file:`/`dir:` get handlers this session; the parser recognizes all six spellings now (rejecting unimplemented kinds with a Result error naming the kind as not-yet-supported) so the grammar is fixed once. Record the D-a resolution in the Decision Register with today's date, in the commit that lands the parser.
4. Schemas first, test-first. Malli schemas for the canonical maps (per the design's well-known fields; open kind set via registry), constructors/validators, then the parser: `parse-source-designator` / `print-source-designator` (and sink twins), with a test.check round-trip property (print ∘ parse = identity on canonical maps; parse ∘ print = identity on normalized URL strings) plus explicit negative cases (unknown scheme, whitespace, missing required kind-specific fields).
5. The golden comparison is the acceptance for the refactor half. Generate one corpus at the zero-flag defaults; run intake the pre-SS-1 way and through a `dir:` Source value; the resulting catalog EDN must be byte-identical. This test lands RED-impossible (it is a comparison, not a red/green), so the discipline is: commit the comparison harness before the refactor commit, run it against both paths in the same commit that switches intake's callers. Only after it passes does D-c's rename proceed.
6. D-c ruled — `:origin` rename ships here, as its own commit, after ruling 5 passes. `CatalogEntry`'s `:source` field becomes `:origin` — schema, writers, readers, docs (`docs/formats.md` / intake docs where the field is described), tests, and the glossary's Corpus-vocabulary entry if it names the field. This is a deliberate pre-release catalog-format change: the golden comparison is re-baselined in the same commit with a one-line note, and the design doc's D-c row gets its dated resolution. No compatibility shim for old catalogs — pre-release, same reasoning as D10's no-aliases.
7. CLI acceptance is additive. Wherever a positional `PATH` names an input or `--out-dir`/`--out` names an output, a `file:`/`dir:` URL spelling is now also accepted (parsed to the same map; bare paths remain the common spelling and remain documented first). `cli-spec` help text mentions the URL form once per group, not per flag; `make cli-doc` regenerates; quickstart and use-cases strips are NOT rewritten to URL spellings (bare paths stay the taught form) — one strip MAY gain a URL variant if a natural place exists, author-taste, agent's call.
8. Scope fence. No `stdin`/`synthea`/`sim`/`blaze` handlers, no framing codecs, no sink manifest emission — those are SS-2..SS-5. If implementing dir/file sinks would drag manifest emission in, land the sink types with plain write discipline (fail-if-exists default per the design) and leave manifest emission to SS-4, noted in the plan row.
9. Tiers: T0 per commit; T1 + T2 before the final commit (T2 owed via `artifact.clj`). Report wall times; note whether T2 under JDK 21 differs materially from UX-1's 3m40s baseline.

Steps
Step 0 — Toolchain (author-present)
Per ruling 1. No repo commit from this step alone.
Step 1 — Java 21, both roles
Per ruling 2. Commit: `chore: Java 21 — temurin-jdk 21 pinned (F-next, checksum verified from bytes), system/CI/SETUP moved off 17`
Step 2 — Schemas + constructors (red→green)
Per ruling 4, schemas half. Commit: `feat: Source and Sink canonical-map schemas (SS-1, design D1–D7)`
Step 3 — URL⇄map parser + round-trip law
Per rulings 3–4; Decision Register D-a dated resolution in this commit. Commit: `feat: source/sink designator parser — six schemes fixed, file:/dir: live, round-trip property (D4, D-a resolved)`
Step 4 — Golden harness, then intake through Source
Per ruling 5: harness commit, then the refactor commit switching intake's callers to `dir:`/`file:` Source values with the comparison green in the same commit. Commit A: `test: golden-catalog comparison harness (SS-1 acceptance)` Commit B: `refactor: intake consumes Source values; golden catalog byte-identical (SS-1)`
Step 5 — `:origin` rename
Per ruling 6. Commit: `feat!: CatalogEntry :source -> :origin — the word is freed for the formal type (D-c resolved, ADR-0017 §vocabulary)`
Step 6 — dir/file Sink + CLI URL acceptance
Per rulings 7–8. Commit: `feat: dir/file Sink types (fail-if-exists discipline); URL spellings accepted alongside bare paths (additive)`
Step 7 — Regenerate, re-verify, plan row
`make cli-doc`, freshness gates, affected use-cases strips re-run (intake/mutate strips at minimum), plan row SS-1 → Done with the manifest-emission note if ruling 8's fence was exercised. Commit: `docs: cli regenerated; strips re-verified; SS-1 plan row closed`
Step 8 — Tiers, archive, push
Per ruling 9. Archive; push. Commit: `prompts: archive 2026-07-28 ss-1 build session`
Final report
Step 0 acceptance (git version, fsmonitor probe, hooks, doctor); Temurin 21 checksum cross-verification evidence and F-row number; T2 under JDK 21 vs the 3m40s baseline; golden-comparison result pre-rename and re-baseline note; parser property-test seed/iterations; scope-fence exercises; deviations.
