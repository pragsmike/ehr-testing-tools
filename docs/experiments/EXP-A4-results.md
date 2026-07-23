# EXP-A4 — Results

## Metadata

- **Experiment:** EXP-A4
- **Date:** 2026-07-24
- **Executor:** Claude Code (P3 session)
- **HEAD at execution:** `41f1b9e92c01f271223bdd67304b7b5176c916ce`
- **Protocol:** [`docs/experiments/EXP-A4.md`](EXP-A4.md) (as amended — see its own "Amendments" section)

## Environment record

| Field | Value |
|---|---|
| OS / kernel | WSL2 Ubuntu 20.04 (orchestrator host) |
| JVM(s) used | Orchestrator (Clojure process): OpenJDK 11.0.27. Generator subprocess: Eclipse Temurin 17.0.19 — required: Synthea v4.0.0's release jar is class file version 61.0 (Java 17+); this host's system `java` is 11.0.27 (class file version 55.0 max) and fails with `UnsupportedClassVersionError` on this jar. A portable Temurin 17 build was downloaded for this session specifically to run it. |
| Locale / timezone (host default) | Irrelevant to output as of this session's fix — every invocation forces `-Duser.language=en -Duser.country=US -Duser.timezone=UTC` regardless of host default (see Round: Locale / Round: Timezone below for why this is load-bearing, not cosmetic) |
| Artifact(s) resolved | `synthea` v4.0.0, sha256 `ed43c20ad40ba5c3bc724503a5af032715fe3c491620b766148e7c2361e6ecc1`, source `https://github.com/synthetichealth/synthea/releases/download/v4.0.0/synthea-with-dependencies.jar` (`artifacts.lock.edn`) |
| Config file(s) used | `config/synthea/synthea.properties`, sha256 `ead0388b86d5d60bff86d8475cd65d6c3d8ef7cdeb5f7b8a58b55c911ad79bb7` |

## Per-round findings

### Round: Baseline, initial (clinician-seed unpinned)

Three runs, same `:seed`, single-threaded, no clinician seed passed.

| Divergence observed | Field(s) | Classification | Action taken |
|---|---|---|---|
| Every patient's assigned practitioner (name, gender, email, practitioner ID) differs across otherwise-identical runs | `fhir/practitionerInformation*.json` content in full; every `Reference.display` pointing at a practitioner inside patient files (Encounter, Claim, etc.) | **pin** | Added `:clinician-seed` as a required `corpus.generate` parameter, passed as Synthea's `-cs` flag. Synthea defaults this to `System.currentTimeMillis()` when `-cs` is omitted — confirmed directly: the unpinned runs' own `metadata/*.json` reported `"clinicianSeed": <epoch-millis matching the run's wall-clock time>`. |
| `hospitalInformation<timestamp>.json` / `practitionerInformation<timestamp>.json` filenames embed a wall-clock export timestamp | Filename only — content is byte-identical across runs once clinician-seed is pinned (verified directly) | **canonicalize** | Registered `:strip-run-timestamp-suffix` v1 (`ehr-testing-tools.corpus.canonicalizers`) |
| `metadata/<ts>_<population>_<state>_<uuid>.json`: filename and its `runID`/`runStartTime`/`runTimeInSeconds` fields are per-execution audit data, not corpus content | Filename + those 3 fields | **canonicalize** | Registered `:strip-synthea-run-metadata` v1; pragmatic default comparison policy also excludes the whole `metadata/` directory from tree-hash computation |

### Round: Baseline, corrected (clinician-seed pinned) — 3 runs, single-threaded

| Divergence observed | Field(s) | Classification | Action taken |
|---|---|---|---|
| None | — | — | Canonicalized tree hash identical across all 3 runs: `0c56d186a7943412fb8ecf9a5733ee09c0a09dd241e97fd1a32020b74f91a39e` (119 files) |

### Round: Parallelism (`generate.thread_pool_size` 1 → 4) — 3 runs

| Divergence observed | Field(s) | Classification | Action taken |
|---|---|---|---|
| None | — | — | Canonicalized tree hash identical to the corrected baseline: `0c56d186a7943412fb8ecf9a5733ee09c0a09dd241e97fd1a32020b74f91a39e`. Thread count does not affect determinism once seed + clinician-seed are pinned — no action needed. |

### Round: Locale (en-US → fr-FR) — 3 runs

| Divergence observed | Field(s) | Classification | Action taken |
|---|---|---|---|
| A GS1 medication-package barcode's embedded expiration-date subcomponent differs by one digit (`carrierHRF`, application identifier `(17)` = expiration date YYMMDD: `381231` vs `371231`) | 2 of ~90 patient files in the round; the `carrierHRF` string on a Device/medication-package resource | **control** | Locale forced to `en-US` by default in every `corpus.generate` invocation (`-Duser.language=en -Duser.country=US`), recorded in `manifest.environment.locale` |

Internally, all 3 locale runs agreed with each other (own consistent hash `2a262bce18c1e9904c7d8293bc0edfa80b9775e8b954b1339edab874d153a6e1`) but differed from baseline — confirming the divergence is a genuine, deterministic locale sensitivity, not noise.

### Round: Timezone (UTC → Asia/Tokyo) — 3 runs

| Divergence observed | Field(s) | Classification | Action taken |
|---|---|---|---|
| Every FHIR `dateTime`/`instant` field's serialized UTC offset differs — same underlying instant, different local-offset representation (e.g. `1975-03-11T05:17:58-04:00` vs `1975-03-11T18:17:58+09:00`, both equal to `1975-03-11T09:17:58Z`) | Nearly every patient file: `Encounter.period.start/end`, `Condition.onsetDateTime`/`recordedDate`, `Observation.effectiveDateTime`/`issued`, etc. | **control** | Timezone forced to `UTC` by default in every `corpus.generate` invocation (`-Duser.timezone=UTC`), recorded in `manifest.environment.timezone` |

Internally, all 3 timezone runs agreed with each other (own consistent hash `cb423e387952103b9b54739ee0ab46d1a7f83a934f94ad2b853256412d4110c8`) but differed from baseline — again a genuine, deterministic sensitivity, and the widest-reaching finding of the four variable rounds.

### Round: Clean-environment reproduction

| Divergence observed | Field(s) | Classification | Action taken |
|---|---|---|---|
| None | — | — | Cleared `~/.cache/ehr-testing-tools/artifacts` entirely; re-fetched Synthea via `ehr artifact fetch` (genuine network download, fresh sha256 verification against the lockfile); regenerated twice from the final pinned configuration (seed 100, clinician-seed 555, reference-date 20260101, config as above, locale/timezone forced by default). Canonicalized tree hash identical both times: `a42a4e19dcd25af8e199bcaaf06d5022eada3af1198ab5f9b6633275d2eff051` |

### Round: Second JVM version — not executed

| Divergence observed | Field(s) | Classification | Action taken |
|---|---|---|---|
| Not tested | — | — | Only one Java 17+ runtime was available in this environment (Eclipse Temurin 17.0.19 — downloaded specifically for this session, since the host's system Java is 11 and cannot run Synthea v4.0.0 at all). A second, distinct JDK 17 build (different vendor or patch version) was not available within this session's scope. This dimension is explicitly untested, not silently assumed clean. |

## Protocol amendments made

- **2026-07-24 — parallelism control corrected.** Recorded in `docs/experiments/EXP-A4.md`'s own "Amendments" section: `-p` is Synthea's `populationSize` flag, not a thread-count control (confirmed against the v4.0.0 jar's `--help` usage text); the real control is the `generate.thread_pool_size` property (confirmed by extracting `synthea.properties` from the jar), set via `--generate.thread_pool_size=<n>`. The corrected control was used for the parallelism round above.

No other amendments were needed; the rest of the protocol executed as written.

## Acceptance verdict

- **Acceptance criterion (quoted from the protocol):** "A clean-environment regeneration from manifest + lockfile + cache is byte-identical, modulo canonicalizations that are themselves recorded in the manifest."
- **Met?** Yes. The Clean-environment reproduction round above regenerated from a freshly-emptied cache (genuine re-fetch, re-verified hash) using the fully pinned configuration, twice, with identical canonicalized tree hashes. The only bytes excluded from that identity are exactly the two canonicalizations registered above (filename timestamp suffix, run-audit metadata fields) — both recorded in `corpus.canonicalizers` and in this results file.
- **Stop condition triggered?** No. The protocol's stop condition — "a divergence source that resists three investigation rounds" — never applied; every divergence found (clinician-seed, two filename/metadata volatilities, locale, timezone) was identified, classified, and resolved (pinned or controlled) within this session, each within its first round of investigation.

## Artifacts produced

| Artifact | Path | Hash |
|---|---|---|
| Synthea v4.0.0 distribution (locked artifact) | `~/.cache/ehr-testing-tools/artifacts/<sha256>` (recorded in `artifacts.lock.edn`) | sha256 `ed43c20ad40ba5c3bc724503a5af032715fe3c491620b766148e7c2361e6ecc1` |
| First real corpus — 1000 patients, final pinned config, multi-threaded (`generate.thread_pool_size=4`, confirmed safe by the Parallelism round above) | `/tmp/exp-a4-corpus-1000` (not committed — gitignored per ADR-0005; this manifest is the committed provenance record, not the bytes) | Raw tree hash `3c681d842c6c6409fea8911a147ae40fe332a02cca5d4877d02167e85077ae7b` (1151 files); canonicalized tree hash `7b0e1a4117953bd59c15aca9c869ffa52c4b6191c9d294867403fc04678a19bb` (1150 files, 1148 patients) |
| `corpus.manifest` schema v1 | `src/ehr_testing_tools/corpus/manifest.clj` | — |
| `:strip-run-timestamp-suffix` v1 canonicalizer | `src/ehr_testing_tools/corpus/canonicalizers.clj` | — |
| `:strip-synthea-run-metadata` v1 canonicalizer | `src/ehr_testing_tools/corpus/canonicalizers.clj` | — |

## Rubric self-score

| Criterion | Met? | Evidence |
|---|---|---|
| 1. Every finding is classified | Yes | Every findings-table row above carries pin / control / canonicalize, or an explicit "None" for rounds with no divergence |
| 2. Environment record is complete | Yes | Every field in the Environment record table above is filled in |
| 3. Amendments are justified | Yes | The one amendment (parallelism control) states what changed, the date, and the evidence (jar `--help` text, extracted `synthea.properties`) that motivated it |
| 4. Verdict is traceable to criteria | Yes | The Acceptance verdict section quotes the protocol's Acceptance text verbatim and states plainly that it was met, with the supporting hash comparison |
| 5. No unexplained divergences | Yes | Every observed byte-level difference (practitioner assignment, two filename/metadata volatilities, locale, timezone) appears in a findings row with a classification; the one untested dimension (a second JVM build) is explicitly flagged as not executed rather than silently assumed clean |

All five criteria met.
