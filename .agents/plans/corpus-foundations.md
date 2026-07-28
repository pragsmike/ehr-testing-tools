# Plan: Corpus Foundations

The phase sequence from bootstrap to a working generation capability and
its first executed experiment. One line per phase: what it delivers,
where it stands, and the prompt that drove it (once archived).

**Spent phases (P0 through Enforcement-wave/ENF-1, all Done) archived verbatim to `.agents/plans/archive/corpus-foundations.md` (2026-07-27, NAV-1).**

| Phase | Deliverables | Status | Prompt |
|---|---|---|---|
| First release | Gates capability landed (`gate.fhir`/`gate.v2`, done P5); the resource-equation notation trial (pattern nursery #13) concluded — promoted to validated, P6. Coverage-threshold gating — **corrected 2026-07-25 (ENF-1): this row previously read "landed," which was false** — no `:coverage` alias or Makefile invocation ever carried a cloverage fail-threshold flag before ENF-1 (confirmed by `git log -p -- Makefile`); `ci.yml`'s own header comment had it right ("No coverage threshold gating yet -- that's the enforcement wave"). Gating lands in the Enforcement-wave row (ENF-1, this same date), not here; this row now only tracks the remaining First-release items. Version tag, published coordinates (Clojars vs. Maven Central, `docs/positioning.md` open decision), guide-repo cross-references begin. | Not started — the milestone after publication (ADR-0008) | — |
| SS-1 | `Source`/`Sink` schemas (Malli) + URL⇄map parser with a property test proving `parse ∘ print = identity` on canonical maps (`docs/source-sink-design.md` D4); `dir`/`file` source and sink built over the new types; `corpus.intake` called through the new `dir` source with no behavior change, verified by a golden catalog comparison against today's output. Also lands the `:origin` rename (`CatalogEntry`'s `:source` field, D6) unless D-c defers it to its own micro-session. Test-first: the round-trip property test and the golden-comparison test precede the parser/source/sink implementations. Verification tier: T0 per commit (no `judge/`, `invocation.clj`, `artifact.clj`, or `corpus/generate.clj` touched by this row alone); T1 owed at session close per ADR-0016. | Not started | — |
| SS-2 | Generator registry (shaped like `corpus.operators`'s, D1/D7); `synthea` re-expressed as a registry entry wrapping the unchanged `corpus.generate`; `sim` source built in `src/` (subprocess, ADR-0013-shaped — never a classpath/`deps.edn` dependency) with `sim-harness` delegating to it instead of owning the subprocess call itself. Smoke-tier coverage per the verification-tiers policy (ADR-0016): one real `sim` subprocess run, skip-when-absent. Test-first: registry-entry validation and injected-fake subprocess tests precede the real wiring. Verification tier: T2 owed in-session (touches `corpus/generate.clj` and adds a new `src/` subprocess seam, both on ADR-0016's trigger list); T1 at session close regardless. | Not started | — |
| SS-3 | Framing codecs (`er7-multi`, `ndjson`, `bundle-entries`, `mllp` bytes⇄messages) as pure functions with round-trip property tests per framing kind (D2); the vendored SimHospital fixture (`test/fixtures/v2/simhospital/`, ADR-0011) as the `er7-multi` witness; `stdin` source via `Spool`, with the size-cap default and its explicit-override case both tested. Test-first: a round-trip property test precedes each codec; the size-cap-exceeded case is a red test before the cap exists. Verification tier: T0 (pure functions, no `test-integration/`-tier surface touched); T1 at session close. | Not started | — |
| SS-4 | Sinks: write discipline (fail-if-exists default; `:overwrite`/`:append` explicit), `ManifestV1_1` sidecar emission, and the composability property test — a sink's own output re-intakes through `corpus.intake` with lineage intact, `docs/source-sink-design.md`'s load-bearing law (D3). `stdout` sink including MLLP framing, paired with SS-3's codec. Test-first: the composability property test is written and red before `Write` exists. Verification tier: T1 owed at session close (no `judge/`/`invocation.clj`/`artifact.clj` touched); T2 only if `test-integration/` gains a subprocess-backed sink test (e.g. exercising real `nc`). | Not started | — |
| SS-5 | `blaze` source/sink: Result-not-throw per network call; transaction-bundle-vs-per-resource-PUT explicit on the sink map, never inferred (D3). **Sequenced against D-b** (the IG-pinning blocker) — not started until that blocker's status is revisited, since a written resource's claimed profile is undefined until then. Test-first: injected-fake HTTP tests (hermetic, `test/`-tier) precede any real Blaze endpoint being touched; a real-endpoint smoke test, if added, lives in `test-integration/`. Verification tier: T0/T1 for the fake-backed unit tests; T2 owed only if a real-endpoint integration test is added under `test-integration/`. | Not started — blocked on D-b | — |

**Source/Sink formalization (2026-07-27).** SS-1..SS-5 above stage the
build-out of `docs/source-sink-design.md` and ADR-0017: formal `Source`
and `Sink` types unifying `corpus.generate`'s Synthea-specific engine,
`corpus.intake`'s directory-specific ingestion, and the sim consumer
loop's harness-only subprocess seam under one registry-open surface,
with framing as an axis independent of format and a sink-composability
law (every sink's output is a valid source). This is a capture session
only — no `src/` code changed; SS-1 is the first row that touches
`src/`. Open questions D-a/D-b/D-c (URL scheme spellings; `blaze` sink
sequencing against the IG-pinning blocker; which session ships the
`:origin` rename) are recorded as open in the design doc's own Decision
Register, not resolved by any row above.
