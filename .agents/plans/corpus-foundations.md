# Plan: Corpus Foundations

The phase sequence from bootstrap to a working generation capability and
its first executed experiment. One line per phase: what it delivers,
where it stands, and the prompt that drove it (once archived).

**Spent phases (P0 through Enforcement-wave/ENF-1, all Done) archived verbatim to `.agents/plans/archive/corpus-foundations.md` (2026-07-27, NAV-1).**

| Phase | Deliverables | Status | Prompt |
|---|---|---|---|
| First release | Gates capability landed (`gate.fhir`/`gate.v2`, done P5); the resource-equation notation trial (pattern nursery #13) concluded — promoted to validated, P6. Coverage-threshold gating — **corrected 2026-07-25 (ENF-1): this row previously read "landed," which was false** — no `:coverage` alias or Makefile invocation ever carried a cloverage fail-threshold flag before ENF-1 (confirmed by `git log -p -- Makefile`); `ci.yml`'s own header comment had it right ("No coverage threshold gating yet -- that's the enforcement wave"). Gating lands in the Enforcement-wave row (ENF-1, this same date), not here; this row now only tracks the remaining First-release items. Version tag, published coordinates (Clojars vs. Maven Central, `docs/positioning.md` open decision), guide-repo cross-references begin. | Not started — the milestone after publication (ADR-0008) | — |
| UX-1 (build) | CLI ergonomics (`docs/source-sink-design.md` Part IX, ADR-0019): `cli-spec` (`src/ehr_testing_tools/cli/help.clj`) updated to the one-flag-vocabulary table (D10) and the zero-flag `corpus generate` defaults (D9); the shipped default Synthea properties file (`resources/`) D9 depends on; the `ehr gate PATH` sniffing dispatch (D11); `corpus mutate`'s derived `--out-dir` and per-operator `:default-locator` (D12); the three new conveniences (`ehr version`, `ehr artifact fetch --all`, `ehr doctor`, D13); every command strip in `docs/use-cases.md` re-verified end to end against the new flags; `make quickstart-fresh` re-verified with the new zero-flag `generate` as its first command. **Sequencing resolved 2026-07-27 (UX-1 build session, author-directed):** this row runs as its own build session immediately before SS-1; SS-1 rebases onto its result. OPEN-1/OPEN-2/OPEN-3 resolved in the same session (error default; `--population 5`; `doctor` ships in the first release — see `docs/source-sink-design.md`'s Decision Register). Test-first: the acceptance-property test (zero-flag `generate` is byte-reproducible, D9) and the mixed-format-directory error test (D11) precede their implementations. Verification tier: T0 + T1 at session close; T2 owed in-session (this row's changes touch `src/ehr_testing_tools/corpus/generate.clj` and the `ehr gate` judge-dispatch seam, both on the ADR-0016 trigger list). | Done (2026-07-28) — T0/T1/T2 all green; see this session's own report | `.agents/prompts/archive/2026-07-27-ux-1-build-session.md` |
| SS-1 | `Source`/`Sink` schemas (Malli) + URL⇄map parser with a property test proving `parse ∘ print = identity` on canonical maps (`docs/source-sink-design.md` D4); `dir`/`file` source and sink built over the new types; `corpus.intake` called through the new `dir` source with no behavior change, verified by a golden catalog comparison against today's output. Also lands the `:origin` rename (`CatalogEntry`'s `:source` field, D6) unless D-c defers it to its own micro-session. Test-first: the round-trip property test and the golden-comparison test precede the parser/source/sink implementations. Verification tier: T0 per commit (no `judge/`, `invocation.clj`, `artifact.clj`, or `corpus/generate.clj` touched by this row alone); T1 owed at session close per ADR-0016. | **Done (2026-07-28)** — T0 green every commit, T1 green at close, T2 owed and run via the Java-21 toolchain step (`artifact.clj` in play); see this session's own report | `.agents/prompts/archive/2026-07-28-ss-1-build-session.md` |
| SS-2 | Generator registry (shaped like `corpus.operators`'s, D1/D7); `synthea` re-expressed as a registry entry wrapping the unchanged `corpus.generate`; `sim` source built in `src/` (subprocess, ADR-0013-shaped — never a classpath/`deps.edn` dependency) with `sim-harness` delegating to it instead of owning the subprocess call itself. Smoke-tier coverage per the verification-tiers policy (ADR-0016): one real `sim` subprocess run, skip-when-absent. Test-first: registry-entry validation and injected-fake subprocess tests precede the real wiring. Verification tier: T2 owed in-session (touches `corpus/generate.clj` and adds a new `src/` subprocess seam, both on ADR-0016's trigger list); T1 at session close regardless. | **Done (2026-07-28)** — the generator/reader unification built for real: `ehr-testing-tools.corpus.generators` (registry, `:synthea` + `:sim` entries), `ehr-testing-tools.corpus.generator-source/resolve!` (the unification function), `ehr-testing-tools.sim` (the promoted `src/` adapter, three-path discovery). CLI surface landed one step further than this row's own scope named: `ehr corpus intake` accepts `sim:`/`synthea:` generator URLs directly (ruling 6), verified for real against the `../ehr-testing-sim` sibling checkout, which was present this session (no skip). New `docs/use-cases.edn` strip (`:simulator-traffic-as-intake-source`, 15th case) verified for real, not recorded as an errand. `simhospital` registration would require exactly one thing beyond an entry: a real engine (an artifact or a path) — no additional design gap found. T0 green every commit; T1 (`make integration-smoke`, 46.5s) and T2 (`make integration`, 4m12s, including a new real-subprocess `sim-generator-source-test`) both green at close; `make coverage` green (91.40%/94.43%, new namespaces 94–100%). Full report in this session's own archived prompt/report. | `.agents/prompts/archive/2026-07-28-ss-2-build-session.md` |
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

**SS-1 closed (2026-07-28).** D-a and D-c both resolved in this
session (see the design doc's Decision Register); D-b stays open,
untouched. Ruling 8's scope fence was exercised as written: `dir`/
`file` Sink types land with plain write discipline only (fail-if-exists
default, `ehr-testing-tools.corpus.sink-write`) — no `ManifestV1_1`
sidecar emission and no composability-law property test this session;
both are SS-4's own obligation, not started here. No `stdin`/`synthea`/
`sim`/`blaze` handlers and no framing codecs landed either, per the
same fence — all four are parser-recognized (D-a) but rejected
`:unsupported-source-kind`/`:unsupported-sink-kind` by name.

**CLI ergonomics coordination (2026-07-27, UX-1 capture).** The UX-1 row
above stages `docs/source-sink-design.md` Part IX and ADR-0019: the
determinism law of defaults, the ratified zero-flag `corpus generate`
defaults, the one-flag-vocabulary rename (old spellings removed, not
aliased — pre-release is the window this is cheap in), the `ehr gate
PATH` sniffing dispatch, `corpus mutate`'s derived defaults, and three
new conveniences (`version`, `artifact fetch --all`, `doctor`). This is
also a capture session only — no `src/` code changed. Sequencing against
SS-1 is the one open call this capture makes explicitly rather than
leaving implicit: both rows change the same CLI surface, and landing them
in two separate sessions would cost readers two migrations instead of
one (ADR-0019's own reasoning). OPEN-1/OPEN-2/OPEN-3 (mixed-format
directory dispatch; `--population` default value; whether `doctor` ships
at first release) join D-a/D-b/D-c as recorded-open, not resolved by this
row.
