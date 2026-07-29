# EXP-D3 — Results

## Metadata

- **Experiment:** EXP-D3
- **Date:** 2026-07-29
- **Executor:** Claude Code (R30 autonomous session, EXP-D3 prompt)
- **HEAD at execution:** `1dbe9b5` (EXP-D3 protocol commit)
- **Protocol:** [`EXP-D3.md`](EXP-D3.md)

## Environment record

| Field | Value |
|---|---|
| OS / kernel | WSL2, `Linux 6.18.33.2-microsoft-standard-WSL2`, Ubuntu 20.04.6 LTS userland |
| JVM(s) used | Temurin/OpenJDK 21.0.7 (Ubuntu package `openjdk-21-jdk-headless`), ambient — **not** pinned through `artifacts.lock.edn` for this scratch build, per the protocol's own Boundary note |
| Locale / timezone (host default) | `C.UTF-8`; irrelevant to this experiment (no locale/timezone-sensitive behavior under test) |
| Artifact(s) resolved (name, version, sha256) | Six NIST-origin coordinates, resolved twice independently (Maven's own resolver and a plain `curl` against `hit-nexus.nist.gov`) — see the Jar Inventory table below for the full list; Apache Maven 3.9.9 (sha512-verified against `archive.apache.org`'s own published checksum, installed to `~/tools`, no root) |
| Config file(s) used (path, sha256) | None repo-owned — this is a scratch, out-of-tree build. The wrapper's own `pom.xml` at pinned commit `e059e58ac5592baff57f04ee744398357d3258f3` (tag `1.4.0`) is the only "config" and is fully described by that commit SHA |

**Scratch location.** `/tmp/exp-d3/` (WSL), outside the workspace tree, per the protocol's own instruction. Nothing under this path is committed.

## Round 1 — Build and offline-run (acceptance question (a); H1)

**H1 refuted.** The pinned release's own `pom.xml` declares a live
`hit-nexus` repository (`https://hit-nexus.nist.gov/repository/releases`)
and its `README.md` documents SSL-verification-disabling build flags
(`-Dmaven.wagon.http.ssl.insecure=true -Dmaven.wagon.http.ssl.allowall=true
-Dmaven.wagon.http.ssl.ignore.validtidy.dates=true`). Running `mvn
dependency:resolve` and `mvn package` against the **unmodified** `pom.xml`
— no insecure flags added — succeeded outright: all six NIST-origin
coordinates resolved from `hit-nexus.nist.gov` under Maven's default
(verified) TLS trust, and the wrapper jar built cleanly
(`lib-hl7v2-nist-validator-1.4.0.jar`, `BUILD SUCCESS`). The
SSL-bypass STOP clause was **not triggered** — hit-nexus.nist.gov's
certificate validates today under a stock JVM/Maven trust store,
whatever was true when the 1.4.0 README was written. The protocol's
STOP-clause language ("if your build appears to demand
`-Dmaven.wagon.http.ssl.insecure=true`... STOP") is therefore moot for
this run, stated here rather than silently dropped since the protocol
explicitly anticipated the opposite outcome.

One unrelated build hiccup, unrelated to SSL/NIST: `mvn package`
initially failed on the `maven-javadoc-plugin` step
("Unable to find javadoc command: JAVA_HOME is not correctly set") —
fixed by exporting `JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`
ahead of the build. Recorded as an environment fact, not a finding
about the wrapper or NIST engine.

**Offline proof.** All harness runs (both profiles, all mutants, 16
total invocations) were re-executed inside `unshare -r -n`, with
`-Duser.home` forced explicitly (the same `pattern #15` precaution
EXP-C5 used) — though, unlike the FHIR validator, this engine turned
out **not** to need it: it has no filesystem package cache of its own
(profiles/constraints/valuesets load from the classpath at
construction time, nothing is fetched or cached at `validate()` time),
so there was no `user.home`-dependent cache path to go wrong. A direct
`curl` to `hit-nexus.nist.gov` from inside the same isolated namespace
returned no response at all (exit `000`, confirming genuine isolation,
not a soft DNS-to-cache fallback the way EXP-C5's FHIR validator
degraded). Every offline `FILTERED status=...` line was
byte-identical to its online counterpart, for both `/TEST_PROF` (12
runs) and `/COVID19_ELR-v2.3.1` (4 runs) — **acceptance question (a)
is answered: yes, unconditionally, once the one-time dependency fetch
has happened.**

## Round 2 — `NistReport`, field by field (acceptance question (b))

Read directly from `gov.cdc.NistReport`/`gov.cdc.ProfileManager`
source (wrapper 1.4.0) and `gov.nist.validation.report.Entry`/`Report`
(from `com.github.hl7-tools:validation-report:1.2.0`, decompiled via
`javap` against the resolved jar — no source repo needed):

- **`NistReport`** carries: `status` (one of three strings —
  `VALID_MESSAGE`, `STRUCTURE_ERRORS`, `CONTENT_ERRORS`, computed by
  `ProfileManager.filterAndConvert`: `STRUCTURE_ERRORS` if any
  `structure`-bucket error exists, else `CONTENT_ERRORS` if any
  `content`- or `value-set`-bucket error exists, else
  `VALID_MESSAGE`); four `SummaryCount`s (`errorCounts`,
  `warningCounts`, `alertCounts`, `informationalCounts`), each itself
  `{structure, value-set, content}` integer counts; and `entries`
  (`Entries`: three lists, `structure`/`content`/`value-set`, each of
  `gov.nist.validation.report.Entry`).
- **`Entry`** (the underlying engine's own type, unchanged by the
  wrapper) carries, per finding: `getClassification()` (a string —
  see Round 3), `getCategory()` (a finer-grained string — `Usage`,
  `Predicate Success`, `Constraint Failure`, `Unexpected`, `Length`,
  `Dynamic Mapping Match`, etc., see Round 3's tally), `getPath()` (a
  positional path like `PID[1]-7[1]` or `MSH[1]-9[1].3`, repetition-
  and component-aware), `getLine()`/`getColumn()`, `getDescription()`
  (a human-readable sentence, often naming the field by both position
  and profile-declared name — e.g. `"Field PID-7 (Date/Time of Birth)
  is missing..."`), `getMessageProfilePath()`/
  `getMessageProfilePositionPath()`/`getMessageInstancePathName()`/
  `getMessageInstancePositionPath()` (not exercised by this
  experiment — profile-vs-instance path pairs, presumably for
  slicing/repetition disambiguation), `getStackTrace()`/`getMetaData()`
  (not populated on any observed entry), and `toJson()`/`toText()`
  serializers.
- **The underlying `Report`** (`hl7.v2.validation.report.Report`,
  decompiled Scala source read directly from
  `usnistgov/v2-validation`) is a fixed three-key structure by
  construction — `structure`, `content`, `value-set` — **always**
  exactly these three, never a fourth bucket. This resolves a concern
  raised while reading the wrapper's source ahead of writing the
  protocol (whether predicate/co-constraint findings might live under
  an unrecognized fourth top-level key the wrapper's `filterAndConvert`
  never reads): they don't — predicate and constraint findings both
  surface under the `content` bucket, which the wrapper does read.

## Round 3 — Classification filtering (acceptance question (c); H2)

**H2 confirmed, empirically, with numbers, not just from source
reading.** The underlying engine's own `reference.conf` (read directly
out of `hl7-v2-validation-1.7.3.jar`) declares eight classification
strings (`Error`, `Warning`, `Alert`, `High Alert`, `Informational`,
`Affirmative`, `Validation Notes`, `Specification Error`).
`ProfileManager.filterAndConvert` string-matches exactly four
(`Error`, `Warning`, `Alert`, `Informational`) — any entry classified
`Affirmative`, `Validation Notes`, `High Alert`, or `Specification
Error` is silently excluded from **both** the `entries` lists and the
`SummaryCount`s.

Observed against the `TEST_PROF` baseline message (raw
`Report.getEntries()`, all three buckets, tallied by classification):

| Classification | Count | Kept by `NistReport`? |
|---|---|---|
| Affirmative | 1,469 | **No** |
| Informational | 648 | Yes |
| Validation Notes | 545 | **No** |
| Error | 1 | Yes |

Raw total: 2,663 entries. `NistReport.entries` total: 649. **The
wrapper's own filtered report represents 24.4% of what the underlying
engine actually found** for this message — 2,014 entries (75.6%),
every one of them `Affirmative` or `Validation Notes`, never appear in
anything `ProfileManager.validate()` returns. `Warning`, `High Alert`,
and `Specification Error` were never observed in this experiment's
corpus (neither profile's shipped fixtures nor any mutant triggered
them) — their filtering behavior is a **static**, source-read finding
(`Warning` is kept, `High Alert`/`Specification Error` would be
dropped), not independently confirmed by execution.

**Category tally** (raw, `TEST_PROF` baseline, all three buckets):
`Usage` 864, `Predicate Usage Selection`/`Predicate Path Occurrence`/
`Predicate Success` 441 each, `O-Usage` 207, `Constraint Success` 164,
`Dynamic Mapping Match` 104, `Length` 1. Every `(classification,
category)` combination observed: `(Affirmative, Usage)` 864,
`(Validation Notes, Predicate Usage Selection)` 441, `(Informational,
Predicate Path Occurrence)` 441, `(Affirmative, Predicate Success)`
441, `(Informational, O-Usage)` 207, `(Affirmative, Constraint
Success)` 164, `(Validation Notes, Dynamic Mapping Match)` 104,
`(Error, Length)` 1 — the last one is the `TEST_PROF` baseline's own
pre-existing structure error (see Round 4), the profile-noise analogue
of EXP-C5's own baseline-error finding for the FHIR validator.

**Consequence, stated plainly:** a consumer reading only
`NistReport`/`ProfileManager.validate()` output (the wrapper's own
public API) never sees three-quarters of what the engine actually
checked against this message, and specifically never sees any
`Affirmative` finding — which turns out to be exactly the
classification the engine uses to say "this optional/RE field or
group is empty, and that's fine" (see Round 5's customer findings,
where this is the entire reason two of the three named customers
don't convict). A future `judge-v2-nist` component that wants full
fidelity to what the NIST engine actually decided must call the raw
`SyncHL7Validator.check(...)` directly (as this experiment's harness
did, via reflection on `ProfileManager`'s private `validator` field)
rather than going through `ProfileManager.validate()`.

## Round 4 — `TEST_PROF` (ORU^R01) characterization

Baseline message: the wrapper's own shipped
`hl7TestMessage.txt`. **Baseline is not clean** — `FILTERED
status=STRUCTURE_ERRORS`, one pre-existing `(Error, Length)` finding —
mirroring EXP-C5's own "valid file still carries baseline noise"
finding for the FHIR validator, now cross-confirmed for the NIST v2
engine. Every mutant's result below is reported as **new findings
relative to this baseline**, not raw aggregate verdict, per the same
discipline EXP-C5 established.

| Mutant | Locator | New finding(s) | Convicts? |
|---|---|---|---|
| `blank-required-field` | MSH-9 | `Error/Usage/MSH[1]-9[1]` | **Yes** |
| `corrupt-encoding-characters` | MSH-2 | Parse-time exception ("character(s) ['X'] used more than once as a separator") at **both** raw and filtered call | **Yes** (via exception, not a report) |
| `corrupt-segment-name` | MSH → MSX | Parse-time exception ("No MSH Segment found in the message") | **Yes** (via exception) |
| `truncate-segment-fields` | MSH-9 (slot dropped) | 15 new findings, cascading field-shift errors (`Cardinality`, `Length`, `Unescaped Separator`, `Usage` across MSH-9 through MSH-21) | **Yes** |
| `malformed-datetime-value` (original: PID-7 → `20261345`, calendar-invalid month/day, still 8 numeric digits) | PID-7 | **None** | **No** — see supplement below |

**Supplement, added mid-session** (protocol amendment — see below):
the zero-finding result for `malformed-datetime-value` was
surprising enough to check whether the mutation actually broke
anything the engine's own constraint checks for:

| PID-7 value | New finding | Convicts? |
|---|---|---|
| `20261345` (calendar-invalid: month 13, day 45; 8 digits) | none | No |
| `20261301` (calendar-invalid: month 13; 8 digits) | none | No |
| `2026134` (7 digits, wrong length) | `Error/Constraint Failure/PID[1]-7[1].1` | **Yes** |
| `ABCDEFGH` (non-numeric, 8 chars) | `Error/Constraint Failure/PID[1]-7[1].1` | **Yes** |

**Finding, stated plainly:** `TEST_PROF`'s own `CONSTRAINTS.xml`
checks PID-7.1 is the right **length** and **numeric**, but does
**not** check calendar validity (a real month 1-12, a real day for
that month) — a syntactically-8-digit, calendar-nonsensical date
passes silently. This is a property of this specific profile's own
authored constraint (a length/numeric-pattern check, not a full
`DTM`-grammar validator), not a general claim about what the NIST
engine is capable of expressing — contrary to this experiment's own
implicit expectation going in (mirroring EXP-C5's own "contrary to
a-priori hypothesis" finding for `invalid-code-value`).

## Round 5 — Customer conviction, two profiles (acceptance question (d); H3)

**SimHospital-ADT applicability confirmed absent, per H3.** Direct
tree searches (GitHub API, `recursive=1`) against `usnistgov/hl7-igamt`
(5,709 entries) and `usnistgov/v2-validation` (229 entries) for any
ADT-typed profile or sample export found none; the wrapper's own
shipped fixtures are `TEST_PROF` (ORU^R01, "Case Notification") and
`COVID19_ELR-v2.3.1` (ORU^R01, ELR) — **neither is ADT**. **Customer
conviction against the repo's own vendored SimHospital ADT corpus
remains unproven this session** — stated plainly, not glossed over.
Authoring an ADT profile in IGAMT (a web tool + account) is the named
gap; it is out of an autonomous session's reach, per the protocol's
own effort cap.

What **was** run: the three named customers against both shipped
ORU profiles (not ADT, but both carry an ordinary `PID` segment with
an authored Usage code — a real, if non-ADT, data point):

| Customer | `TEST_PROF` (ORU) | `COVID19_ELR` (ORU) | Convicts? |
|---|---|---|---|
| Drop the `PID` segment entirely | Only `Affirmative/Usage/PATIENT_RESULT.PATIENT[1]` (dropped by the wrapper's filter); `FILTERED` errors unchanged from baseline | Only fewer `Informational` entries (PID's own informational notes disappear); `FILTERED` status stays `VALID_MESSAGE`, unchanged from baseline | **No**, on either profile |
| Corrupt `PID`'s own segment name (`PID` → `PIX`) | New `Error/Unexpected/PIX` | New `Error/Unexpected/PIX` | **Yes**, on both profiles |
| Blank a non-header field (`PID-7`, empty not malformed) | Only `Affirmative/Usage/PID[1]-7[1]` (dropped by the wrapper's filter); `FILTERED` unchanged from baseline | Only `Affirmative/Usage/PID[1]-7[1]` (dropped); `FILTERED` status stays `VALID_MESSAGE` | **No**, on either profile |

**Why, precisely** (read from the profile XML, not inferred): both
tested profiles declare `PID`'s own group (`PATIENT`) as `Usage="RE"`
and `PID-7` itself as `Usage="RE"` (`TEST_PROF/PROFILE.xml` line
656-657: `<Field Name="Date/Time of Birth" Usage="RE"
Datatype="TS_Opt9s".../>`) — Required-but-may-be-Empty. The NIST
engine's own predicate/usage machinery **does** notice both the
dropped group and the blanked field (it emits an `Affirmative` finding
for each, meaning "I checked, and this is allowed to be empty") — it
is CDC's wrapper, not the engine, that discards the evidence the
engine actually produced (Round 3). This is a materially different
finding than HAPI's own base-structural-tier result for the same two
customers (`docs/judge-calibration.md`'s CAL-1: HAPI's tier has *no
rule at all* checking segment presence, and *every* lexical rule
wraps its check in `emptyOr(...)`) — the NIST engine's tier is
**capable** of convicting both, contingent entirely on the profile
author's own choice of `Usage="R"` (strictly required, non-emptyable)
rather than `Usage="RE"`. Neither of the two profiles this experiment
had available happened to mark `PID`/`PID-7` that way; a stricter,
purpose-authored profile (an ADT profile marking `PID` and its
identifying fields `Usage="R"`, as a real site's own conformance
requirements plausibly would) is expected, on this evidence, to
convict — but that is inference from this session's evidence, not a
result this session directly measured, and is named here as exactly
that: an inference, not a proven customer-conviction result.

`corrupt-segment-name` (the third customer) **does** convict on both
tested profiles — a materially different, and better, result than
HAPI's own tier, where the identical mutation against `PID`
specifically was probed and dropped as unconvictable (CAL-1's dropped-
candidates table). The NIST engine recognizes `PIX` as a genuinely
unexpected/unrecognized segment (`category=Unexpected`,
`classification=Error`) regardless of usage-optionality, because
segment-name resolution against the profile's own structure is a
different mechanism than field/group presence-usage checking.

## Protocol amendments made

1. **Sequencing, disclosed rather than smoothed over.** The protocol's
   own H1 was drafted as a forward-looking hypothesis ("will actually
   be necessary"), but the underlying `mvn dependency:resolve` had
   already been run once, successfully, without SSL-insecure flags,
   *before* the protocol document was finalized and committed — as
   part of ordinary pre-protocol reconnaissance (confirming the
   pinned-vs-`main` version discrepancy existed at all). The protocol
   text does not pretend otherwise, but the sequencing is recorded
   here explicitly: the empirical answer to H1 was known before the
   hypothesis was formally written down, not discovered fresh during
   Step 2's execution. This does not change the finding's validity
   (the build genuinely does succeed without SSL bypass, re-confirmed
   identically on the pinned commit during formal execution) but is
   named per the house discipline against silently absorbing
   sequencing slips.
2. **Three supplementary `PID-7` datetime mutants added mid-session**
   (Round 4), after the protocol's own registered `malformed-datetime-
   value` mutation produced zero findings — an unexpected result
   worth distinguishing "the engine doesn't check this" from "this
   particular mutation didn't land." Additive, not a correction to
   the registered operator's own definition; the protocol's own
   acceptance criterion (classification is additive, discovered from
   real output) covers exactly this addition, the same way EXP-C5's
   addendum covered its own late-discovered `fatal` severity.
3. No other corrections. The procedure as written was followed;
   both shipped profiles were characterized, as the protocol
   specified.

## Acceptance verdict

Quoted from the protocol's own **Acceptance questions**:

- **(a) "Does the wrapper build and run with zero network after an
  initial user-initiated dependency fetch?"** — **Yes.** Round 1;
  16/16 offline runs byte-identical to their online counterparts.
- **(b) "What does a `NistReport` actually carry, field by field?"** —
  Answered in full, Round 2.
- **(c) "Which classifications occur in practice against valid and
  mutated messages?"** — Answered in full, Round 3 (four kept, two
  observed-but-dropped, two never observed — table given).
- **(d) "Do the three named customers convict under an applicable
  profile?"** — **Answered honestly as a gap, not a yes.** No ADT
  profile exists to test the literal question; against the two
  available ORU profiles, one of three customers convicts
  (`corrupt-segment-name`), two do not (`drop-pid-segment`,
  `blank-non-header-field`) for a profile-authoring reason (both
  fields/groups tested happen to be `Usage="RE"`) rather than an
  engine limitation — see Round 5's inference caveat.
- **(e) "What is the exact transitive jar inventory (coordinates,
  resolved URLs, sha256s) a future lockfile entry set needs?"** —
  Answered in full; see the Jar Inventory table below. Confirmed by
  two independent mechanisms (Maven's own resolver and a direct
  `curl`), both agreeing on every sha256.

Quoted from the protocol's own **Stop conditions**:

- SSL-verification bypass — **not triggered** (Round 1).
- Licensing surface worse than recorded posture — **not triggered**;
  nothing newly discovered changes the six coordinates'
  `license-unstated` classification (F9) or the
  `:use-permitted--unstated--confirmation-pending` posture (ADR-0005's
  amendment) — no new license text, terms-acceptance gate, or
  restrictive grant was found anywhere this session touched.
- `hit-nexus.nist.gov` unreachable, or any coordinate absent —
  **not triggered**; all six returned HTTP 200 from two independent
  clients (Round 1, jar inventory below).
- Effort cap (no IGAMT profile authoring) — **respected**; the ADT gap
  is named, not worked around.

**All acceptance questions answered; no stop condition fired against
the actual build.** The verdict-mapping table itself remains
explicitly out of scope, per the protocol's own framing — this file
classifies, it does not decide.

## Jar inventory (acceptance question (e))

All six coordinates, resolved twice independently (Maven's Aether
resolver against a clean, isolated `-Dmaven.repo.local`, and a plain
`curl` with default TLS verification, no `-k`) — both mechanisms agree
on every sha256:

| Coordinate | Version | Resolved URL | sha256 |
|---|---|---|---|
| `gov.nist:hl7-v2-parser` | 1.7.3 | `https://hit-nexus.nist.gov/repository/releases/gov/nist/hl7-v2-parser/1.7.3/hl7-v2-parser-1.7.3.jar` | `62fac887d19842b26ddde2b9916db39dd7a2cdcdafeeeb1cac6a0e52c0d17999` |
| `gov.nist:hl7-v2-profile` | 1.7.3 | `https://hit-nexus.nist.gov/repository/releases/gov/nist/hl7-v2-profile/1.7.3/hl7-v2-profile-1.7.3.jar` | `354d7a3e0a39b5eae705d9f2d56cc9c25721bbd2ae80d71ea6bd0df92f4846e7` |
| `gov.nist:hl7-v2-validation` | 1.7.3 | `https://hit-nexus.nist.gov/repository/releases/gov/nist/hl7-v2-validation/1.7.3/hl7-v2-validation-1.7.3.jar` | `3e5b6a9b95066c4abeae1435de0a06e08c43fa8e786bb5c1a609d8172925de50` |
| `gov.nist:xml-util` | 2.1.0 | `https://hit-nexus.nist.gov/repository/releases/gov/nist/xml-util/2.1.0/xml-util-2.1.0.jar` | `ba41c996d50e2a14c617f780a301cc69f5c683ddc6830e1b52f065e50bef224c` |
| `gov.nist.hit:hl7-v2-schemas` | 1.7.2 | `https://hit-nexus.nist.gov/repository/releases/gov/nist/hit/hl7-v2-schemas/1.7.2/hl7-v2-schemas-1.7.2.jar` | `22fd209727f7d1e4fa17b773bf62f9b27b076c95b082e98faa7592ab454c28fb` |
| `com.github.hl7-tools:validation-report` | 1.2.0 | `https://hit-nexus.nist.gov/repository/releases/com/github/hl7-tools/validation-report/1.2.0/validation-report-1.2.0.jar` | `e21fc63edcb8efd5c1db137b224dfb187d2fed63b924564ff34693eedd46cc7f` |

This shape (one direct URL + one sha256 per coordinate) fits
`artifacts.lock.edn`'s existing `{kind, name, version, sha256, source,
acquired, license-status}` record cleanly — no transitive-graph
resolution is needed to express these six specifically, since each is
a single leaf artifact at a single resolvable URL. The other ~20
Maven-Central dependencies in the wrapper's own closure
(`EXP-SBOM-results.md`'s dependency table — gson, kotlin-stdlib,
scala-library, commons-lang3, etc.) are **not** proposed for
individual lockfile entries: they are ordinary Maven Central
artifacts with no provenance question attached, and lockfiling an
entire transitive closure by hand would duplicate what Maven's own
resolver already does reliably — ADR-0005's mechanism targets
artifacts whose acquisition or licensing needs a repo-level record,
not routine dependency resolution. This is a decision made in this
results file, per the protocol's own step 7 framing, not pre-judged in
the protocol.

## Artifacts produced

| Artifact | Path | Hash |
|---|---|---|
| Results (this file) | `docs/experiments/EXP-D3-results.md` | n/a — prose artifact, not hashed per repo convention |
| Protocol | `docs/experiments/EXP-D3.md` | n/a |
| Scratch build, mutants, harness, logs | `/tmp/exp-d3/` (WSL, not committed, outside the workspace tree per the protocol) | n/a |
| Six-coordinate jar inventory | this file, table above | sha256 per coordinate, above |

## Rubric self-score

| Criterion | Met? | Evidence |
|---|---|---|
| 1. Every finding is classified | Yes | Every mutant in Rounds 4-5 has an explicit convicts-yes/no verdict with the underlying new-finding evidence; every classification string observed in Round 3 is placed in the kept/dropped table |
| 2. Environment record is complete | Yes | Every field filled, including the explicit "not pinned through `artifacts.lock.edn`" note for the ambient JDK, per the protocol's own Boundary section |
| 3. Amendments are justified | Yes | Both amendments state what happened, when, and why — including the sequencing disclosure or a lesser session could have left implicit |
| 4. Verdict is traceable to criteria | Yes | Acceptance verdict quotes the protocol's own acceptance questions and stop conditions verbatim and states plainly whether each was met/triggered |
| 5. No unexplained divergences | Yes | The zero-finding `malformed-datetime-value` result, the baseline-noise finding, the H1 refutation, and the honest "customer conviction unproven for ADT" gap are all stated directly rather than smoothed into an aggregate summary |

All five criteria met.
