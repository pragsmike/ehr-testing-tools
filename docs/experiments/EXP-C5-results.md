# EXP-C5 — Results

## Metadata

- **Experiment:** EXP-C5
- **Date:** 2026-07-24
- **Executor:** Claude Code (P5 session)
- **HEAD at execution:** `2232c2b` (`gate.v2: base-structural gate over HAPI` commit)
- **Protocol:** [`docs/experiments/EXP-C5.md`](EXP-C5.md)

## Environment record

| Field | Value |
|---|---|
| OS / kernel | WSL2 (orchestrator host) |
| JVM(s) used | Validator subprocess: Eclipse Temurin 17.0.19+10 (`artifacts.lock.edn`, kind `:runtime`), resolved through the artifact registry and invoked directly (not via `corpus.generate`'s wrapper -- this experiment ran the validator by hand, ahead of `gate.fhir` existing) |
| Locale / timezone (host default) | Irrelevant to this experiment (no locale/timezone-sensitive behavior under test) |
| Artifact(s) resolved | `fhir-validator-cli` 6.9.12 (`artifacts.lock.edn`, sha256 `0e53ab1d1a6f1e35f505255c0b8ce10a35fcf27e6e96b503640f784cd07e5ad6`, facts register [F18](../../notes/facts-register.md)); `temurin-jdk` 17.0.19+10 |
| Config file(s) used | `config/synthea/synthea.properties` (EXP-A4/EXP-B2's pinned config, sha256 `ead0388b86d5d60bff86d8475cd65d6c3d8ef7cdeb5f7b8a58b55c911ad79bb7`) |

Sample corpus: 8-patient population, seed 100 / clinician-seed 555 /
reference-date 20260101 (EXP-A4/EXP-B2's pinned settings), generated
into `out/exp-c5-corpus/` (gitignored). Three patient files selected
(first three by filename sort): `Brandon214...`, `Cathie710...`,
`Daniel959...`. Mutants: one per registered operator per selected
file (15 total), per the protocol's locator table.

## Package-cache priming (online, not part of the offline measurement)

A single online run (no network isolation) against one input file
resolved and cached `hl7.fhir.r4.core#4.0.1` plus every package the
validator auto-loaded because the input's own `Resource.meta.profile`
declares US Core 8.0.1 (Synthea R4's default profile annotation):
`hl7.fhir.us.core.r4#8.0.1`, `hl7.terminology.r4#7.0.0`,
`hl7.terminology.r5#5.3.0`, `hl7.fhir.uv.extensions*` (several),
`hl7.terminology#5.5.0`, `us.nlm.vsac#0.24.0`, `hl7.fhir.r4.examples#4.0.1`,
`hl7.fhir.uv.sdc#3.0.0`, `us.cdc.phinvads#0.12.0`,
`hl7.fhir.uv.smart-app-launch#2.2.0` -- none of these were requested
via `-ig`; every one was auto-resolved purely from the input's own
declared profile, confirming `validator_cli.jar -help`'s own
documented behavior ("profiles declared in the resource... or
specified on the command line"). This took ~2m20s (mostly network
fetches for ~10 packages); the local package cache (`~/.fhir/packages`)
now holds everything the 18 offline runs below needed.

## Offline execution: network-isolation behavior

All 18 runs executed under `unshare -r -n` (user+network namespace,
matching `AGENTS.md`'s hermeticity-check discipline), with
`-Duser.home` forced ahead of `-jar` (pattern
[#15](../../.agents/memory/patterns.md): `unshare -r` remaps the
effective uid, which silently changed where the JVM resolved
`user.home` -- to `/root` instead of the real cache location -- until
forced explicitly; caught directly by a failed first attempt, not
assumed). Confirmed genuinely network-isolated: every run's log shows
DNS resolution failures for `packages.fhir.org`,
`packages2.fhir.org`, and `build.fhir.org` ("Name or service not
known"), each followed immediately by "Latest version of package X
found locally is Y - using that" -- the validator attempts a
version-check fetch for every package on every run *even with `-tx
n/a`*, exactly the behavior the protocol's Network note anticipated,
and falls back to the cache gracefully rather than failing. This is
itself a finding: `-tx n/a` disables only the *terminology-server*
network path, not the package-registry version-check path -- a wrapper
that only isolates terminology traffic would still leak (harmlessly,
here, because DNS failures degrade to cache) real network attempts on
every single gate run.

## Per-file issue histograms (baseline, valid files)

| File | Total issues | `error`/`structure` | `information`/`code-invalid` | `information`/`structure` | `warning`/`not-found` | `warning`/`invariant` | (other categories, see below) |
|---|---|---|---|---|---|---|---|
| Brandon214 | 6,554 | 736 | 1,477 | 2,111 | 1,026 | 500 | business-rule, informational, unknown, code-invalid(warning), invalid(warning) |
| Cathie710 | 7,735 | 837 | 1,851 | 2,412 | 1,365 | 551 | (same categories, proportionally larger file) |
| Daniel959 | 4,811 | 513 | 1,161 | 1,434 | 779 | 329 | (same categories, proportionally smaller file) |

**Central finding, stated plainly:** every one of the three "valid"
Synthea R4 baseline files carries several hundred `error`/`structure`
issues (736 / 837 / 513) *before any mutation is applied*. These are
not base-FHIR structural defects -- Synthea's output is round-trip-
verified valid JSON (EXP-B2) and was never claimed to be US-Core-
conformant. They are entirely attributable to the auto-loaded US Core
8.0.1 profile rejecting a Synthea-authored extension it doesn't
recognize (see the `error`/`structure` classification row below).
Consequence: **file-level verdict alone cannot discriminate a valid
file from a mutant in this corpus** -- both are `:rejected` under the
pre-authorized mapping rule, because both carry pre-existing
profile-driven errors unrelated to any operator's mutation. This is
exactly the honest observation `docs/notation.md`'s Gate law and
`gate.fhir`'s own docstring now state: the gate targets whatever the
validator actually checks given the input's own declared profile, not
a sanitized base-spec-only view. It also fixes how Step 6's contract
pairing must assert detection: by the *appearance of a new finding*
matching the mutation's locator/code, never by the file's aggregate
verdict alone.

## Classification: every distinct `{severity, code}` category observed

| Severity | Code | Terminology-suppressed? | Representative diagnostics | Classification |
|---|---|---|---|---|
| error | structure | No | "The extension http://synthetichealth.github.io/synthea/disability-adjusted-life-years could not be found so is not allowed here" | **profile-error** (US Core rejecting an unrecognized Synthea extension; baseline noise on every file in this corpus) |
| error | invalid | No | "This property must be a simple value, not an Array" / "Not a valid date format: '2026-13-45'" / "Error parsing JSON: the primitive value must be a boolean" | **base-spec-error** (structural/type/lexical-format violations -- exactly what `duplicate-element`, `malformed-date`, and `wrong-type-value` each introduce, one new instance per mutant) |
| error | code-invalid | No | "The value provided ('not-a-valid-code-9f3a1c') was not found in the value set 'AdministrativeGender' (http://hl7.org/fhir/ValueSet/administrative-gender\|4.0.1), and a code is required from this value set" | **base-spec-error** -- `AdministrativeGender` is a small, base-FHIR-bundled ValueSet, checkable fully offline with no terminology server; this contradicts the protocol's a-priori hypothesis that `invalid-code-value` would be terminology-suppressed offline, and the *observed* class is reported here rather than the anticipated one, per the protocol's own instruction |
| error | not-found | No | "The System URI could not be determined for the code 'not-a-valid-code-9f3a1c' in the ValueSet '...administrative-gender\|4.0.1'" | **base-spec-error** (the companion issue `invalid-code-value` always produces alongside `code-invalid`, above -- same root cause, same offline-checkable ValueSet) |
| warning | business-rule | Yes (some instances) | "Unable to validate code 'Cel' in system 'http://unitsofmeasure.org' because the validator is running without terminology services" | **terminology-suppressed** |
| warning | code-invalid | Yes | "Unable to validate code without using server because: Resolved system urn:oid:2.16.840.1.113883.6.238 (v3.0.2), but the definition doesn't include any codes, so the code has not been validated" | **terminology-suppressed** |
| warning | informational | Yes | "Unable to validate code without using server because: Resolved system http://unitsofmeasure.org (v3.0.1)..." | **terminology-suppressed** |
| warning | invalid | No | "Best Practice Recommendation: In general, all observations should have a performer" | **advisory** |
| warning | invariant | No | "Constraint failed: dom-6: 'A resource should have narrative for robust management'... (Best Practice Recommendation)" | **advisory** |
| warning | not-found | Yes (some instances) | "A definition for CodeSystem 'urn:ietf:bcp:47' could not be found, so the code cannot be validated" | **terminology-suppressed** |
| information | business-rule | No | "Reference to draft CodeSystem http://hl7.org/fhir/narrative-status\|4.0.1" | **advisory** |
| information | code-invalid | Yes | "The value provided ('MA') could not be validated in the absence of a terminology server" | **terminology-suppressed** |
| information | informational | No | "This element does not match any known slice defined in the profile ...us-core-patient\|8.0.1 (this may not be a problem...)" | **advisory** |
| information | structure | No | "Details for urn:uuid:... matching against profile http://hl7.org/fhir/StructureDefinition/Patient\|4.0.1" | **advisory** |
| information | unknown | Yes | "The definition for the Code System with URI 'urn:oid:2.16.840.1.113883.6.238' from 'hl7.terminology.r4#6.2.0' doesn't provide any codes so the code cannot be validated" | **terminology-suppressed** |

**Important nuance:** `code-invalid` and `not-found` are NOT uniformly
terminology-suppressed -- their classification depends on severity and
diagnostics text, not code alone: the `error`-severity instances above
(from `invalid-code-value`'s mutation against `AdministrativeGender`, a
base-bundled ValueSet) are genuine detections; the `warning`/
`information`-severity instances of the same codes (against
terminology-server-dependent code systems like `unitsofmeasure.org` or
UUID-OID-named systems) are suppressed. `gate.fhir`'s
`terminology-suppressed?` check is therefore a diagnostics-text pattern
match (five phrases, versioned as data alongside
`verdict-mapping-version`), applied per-issue, never a `{severity,
code}` lookup table -- a lookup table would misclassify one bucket or
the other.

## Per-operator mutation-introduced findings (new issues vs. the corresponding baseline file, averaged across the 3 selected files -- identical delta on every file)

| Operator | New issue(s) introduced | Classification | Detected by `gate.fhir`? |
|---|---|---|---|
| `remove-required-element` (locator: `entry[0].resource.gender`) | **None** | -- | **No** -- see finding below |
| `duplicate-element` (`entry[0].resource.gender`) | 1× `error`/`invalid`: "This property must be a simple value, not an Array" | base-spec-error | Yes -- `:rejected` |
| `invalid-code-value` (`entry[0].resource.gender`) | 1× `error`/`code-invalid` + 1× `error`/`not-found` (both against `AdministrativeGender`) | base-spec-error (contrary to a-priori hypothesis) | Yes -- `:rejected` |
| `malformed-date` (`entry[0].resource.birthDate`) | 1× `error`/`invalid`: "Not a valid date format: '2026-13-45'" | base-spec-error | Yes -- `:rejected` |
| `wrong-type-value` (`entry[0].resource.multipleBirthBoolean`) | 2× `error`/`invalid`: "Error parsing JSON: the primitive value must be a boolean" | base-spec-error | Yes -- `:rejected` |

**Finding: `remove-required-element` against `Patient.gender`
produced zero new validator issues.** `Patient.gender` is
min-cardinality 0 in base FHIR R4 (`Element.min = 0` on the base
`Patient` StructureDefinition) -- it is genuinely optional, not
required, so removing it violates nothing the validator checks. This
is an operator/locator choice problem specific to this experiment's
test data, not a `gate.fhir` limitation or an EXP-C5 upstream-behavior
finding: `remove-required-element`'s own contract
(`corpus.operators`) is conditional on the locator naming an
*actually* min-cardinality-1 element, and `Patient.gender` doesn't
qualify. Step 6's contract-pairing test uses a different, genuinely
required locator (`Patient.resourceType`, min-cardinality 1 on every
FHIR resource) for this operator specifically, so its `:violates`
contract is tested honestly rather than against a field that was
never required.

## Protocol amendments made

None to the protocol's procedure. One correction made *during*
execution, recorded here rather than silently absorbed: the first
`unshare -r -n` invocation failed to resolve the FHIR package cache
(it looked under `/root/.fhir/packages` instead of the real cache)
because remapping the effective uid to 0 changes how the JVM resolves
`user.home` from the OS user database, independent of the `$HOME`
env var. Fixed by forcing `-Duser.home` explicitly ahead of `-jar`
(now documented in the protocol's own Procedure step 3 and in pattern
nursery [#15](../../.agents/memory/patterns.md)).

## Acceptance verdict

- **Acceptance criterion (quoted from the protocol):** "Every
  `{severity, code}` category observed across all 18 runs is
  classified with an example, or an explicit 'none observed' row is
  not required (categories are additive, discovered from real output,
  not enumerated in advance); the verdict-mapping table `gate.fhir`
  will carry is derived directly from this classification, not
  re-litigated."
- **Met?** Yes. All 15 distinct `{severity, code}` categories observed
  across the 18 runs are classified above with a representative
  example each; `gate.fhir`'s `terminology-suppressed-patterns` and
  verdict policy are copied directly from this table (cited by name in
  `gate.fhir`'s own docstring, `verdict-mapping-cited-to`).
- **Stop condition triggered?** No. Every category fit one of the four
  classification buckets (base-spec-error / profile-error /
  terminology-suppressed / advisory); nothing required the "doesn't
  fit any bucket" escape hatch.

## Artifacts produced

| Artifact | Path | Hash |
|---|---|---|
| Sample corpus (8 patients, EXP-A4/EXP-B2's pinned settings) | `out/exp-c5-corpus/` (not committed, gitignored) | manifest in `out/exp-c5-corpus/manifest.edn` |
| 15 mutants (5 operators × 3 files) | `out/exp-c5-corpus/mutants/<operator>/` (not committed) | lineage records in each operator dir's `lineage/` |
| 18 raw OperationOutcome JSON results | `out/exp-c5-corpus/validator-results/` (not committed, ~2MB+ each) | -- |
| `fhir-validator-cli` 6.9.12 lockfile entry | `artifacts.lock.edn` | sha256 `0e53ab1d1a6f1e35f505255c0b8ce10a35fcf27e6e96b503640f784cd07e5ad6` (facts register F18) |

## Rubric self-score

| Criterion | Met? | Evidence |
|---|---|---|
| 1. Every finding is classified | Yes | All 15 `{severity, code}` categories classified in the table above, each with a representative example and a classification bucket |
| 2. Environment record is complete | Yes | Every field in the Environment record table is filled in |
| 3. Amendments are justified | Yes | One amendment stated (the `-Duser.home` fix), with what changed, why, and where it's now documented |
| 4. Verdict is traceable to criteria | Yes | Acceptance verdict quotes the protocol's own Acceptance/Stop-condition text and states plainly both were met |
| 5. No unexplained divergences | Yes | The baseline profile-noise finding, the `remove-required-element`/`gender` non-detection, and `invalid-code-value`'s contrary-to-hypothesis result are all stated explicitly, not smoothed over |

All five criteria met.
