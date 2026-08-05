> Archived verbatim from a Cowork cloud session (2026-07-30), landed
> into this repo by the `judge-v2-nist` landing session (this same
> date, `notes/prompts/2026-07-30-ehr-testing-judge-v2-nist-landing.md`).
> Below this header, unchanged from the session's own `NOTES.md`.

**Dated annotation, 2026-08-05 (`notes/adr/0053-alignment-fixes-4.md`,
AR-F4-4):** "Wiring into the workspace" item 4 below prescribes
mirroring the resolved NIST jars into an in-repo `file://` repo, CDC's
own pattern. That path is FORECLOSED, not merely superseded: ADR-0005's
2026-07-24 amendment (`notes/tools/ADRs.md`) holds these coordinates to
`:use-permitted--unstated--confirmation-pending` only as a
user-initiated fetch from NIST's own official channel, never vendored
or shipped by this repo. The safe end-state that actually landed is a
USER-SIDE mirror outside this repo (`bin/mirror-nist`, `bin/verify-
nist-lock`, `components/judge-v2-nist/docs/nist-mirror.md`) — this
body is left as-is below, archived verbatim, per this repo's own
frozen-archive discipline; the correction lives here, not in a rewrite
of item 4's own prose.

# judge-v2-nist — spike results and wiring notes

2026-07-30, cloud session. Companion to `components/corpus/docs/research/NIST-HL7v2-dev-test-platform.md`.

## What the spike proved (all verified by execution, not reading)

The NIST v2-validation engine (1.7.3) runs **in-process from Clojure, offline,
synchronously**, with no CDC wrapper needed. The engine ships a Java-friendly API the
research report didn't surface: `hl7.v2.validation.SyncHL7Validator` —
`.check(message, msgId) → gov.nist.validation.report.Report` — plus
`ValidationContextBuilder(profileInputStream)` with `useConformanceContext /
useValueSetLibrary / useVsBindings / useCoConstraintsContext / useSlicingContext`
accepting the Π bundle piecewise. The only Scala surface crossed is
`(.messages (.profile v))` (one `scala.jdk.javaapi.CollectionConverters/asJava` call at
validator construction). Everything else is plain JVM interop.

Run against CDC's COVID-19 ELR v2.3.1 profile fixture + their test message:
473 findings — structure 441, value-set 28, content 4 — mapped cleanly into the
`ehrt.judge` Finding envelope, and the draft verdict policy produced
`:no-verdict / :terminology-suppressed` (the fixture ships `VALUESETS-disabled.xml`,
so value-set checking was genuinely suppressed — the policy fired for the right reason).

## The taxonomy is data (mutation-alignment surface)

`reference.conf` inside the validation jar declares the full finding vocabulary:

- classifications: Error, Warning, Alert, High Alert, Informational, Affirmative,
  Specification Error
- structure categories: Usage, O-Usage, Cardinality, Length, Format, Extra,
  Unexpected, Invalid Content, Unescaped Separator, Constant Value, …
- constraint categories: Constraint/CoConstraint/Content Failure + Success +
  Spec Error, Predicate Failure/Success, Slicing, …
- value-set categories: Code Not Found, VS Not Found, Empty VS, EVS/PVS/RVS,
  Binding Location, Duplicate Code, …

`checkUsingConfiguration(msg, id, configReader)` accepts a Typesafe-config override of
classification per detection. So the mutate↔judge pairing can be declared as data
(defect class → expected Entry category) and checked against the engine's own config,
not prose. This is the closed inject-X-expect-X loop.

## Verdict-policy notes needing author ruling

1. `Specification Error` means Π itself is defective (profile references value set
   `0396` wrongly, etc.) — the criterion couldn't be fully applied. Drafted as
   `:no-verdict` with **proposed new Cause `:profile-spec-error`** (the shared
   `ehrt.judge.finding/Cause` enum currently only has `:terminology-suppressed`).
   Until the enum grows, the code returns `:terminology-suppressed` and carries
   `:proposed-cause` alongside.
2. VS-suppression categories (`VS Not Found`, `Empty VS`, `External Value Set
   Validation Disabled`, `Excluded From Validation`) → `:no-verdict /
   :terminology-suppressed` — the v2 analog of judge-fhir-official's case.
3. `:rejected` (any `Error` classification, or engine exception) dominates
   suppression — same rationale as ADR-0010's revised ranking. Unit-tested.

## Wiring into the workspace (manual steps, deliberately not auto-done)

1. Root `deps.edn`: add `:mvn/repos {"nist-hit" {:url
   "https://hit-nexus.nist.gov/repository/releases/"}}` (component-level
   `:mvn/repos` is ignored by tools.deps). Add `poly/judge-v2-nist` to `:dev`,
   `:ehrt`, and the test path to `:test`.
2. `components/judge-v2-nist/` from this bundle; runs `poly check` clean only after
   step 1 (it requires `ehrt.kernel.interface`, real in the workspace — the spike
   used a 5-line shim).
3. `ehrt.tools.interface`: re-export as `v2-nist-gate-file` / `v2-nist-gate-dir`.
4. `artifacts.lock.edn`: after first resolution, record sha256s of the six
   engine jars. hit-nexus has no SLA and its operator just changed (NIST →
   Prometheus Computing, Aug 2026); mirroring the resolved jars into a `file://`
   repo (CDC's own pattern) is the safe end-state for offline determinism.
5. A committed Π fixture: one representative IGAMT export (six-file bundle) under
   `test-fixtures/`, enabling engine-in-the-loop tests (current tests cover the
   pure `interpret` only). Until you author one in IGAMT, CDC's
   `COVID19_ELR-v2.3.1` fixture (Apache-2.0 repo) is a usable stand-in.

## Gotchas learned the hard way

- `gov.nist` artifacts are NOT on Maven Central — hit-nexus only (mvnrepository
  indexes it; 1.7.3 published 2025-08-19). Transitives (scala-library 2.13.10,
  typesafe config, xom 1.3.7, scala-xml 1.3.0, commons-lang3, jackson,
  httpclient 4.5.x) resolve from Central normally.
- IGAMT/CDC spelling drift: CDC's fetcher reads `VALUSETBINDINGS.xml` (one E).
  The component accepts both spellings.
- Build the validator once per Π and reuse across files — context construction
  (XSD-validated XOM parse of the whole bundle) dominates; `.check` itself is fast.
- Engine version for the invocation record is read from the jar's own
  `pom.properties` (same discipline as judge-v2-hapi), and Π's file sha256s belong
  in the invocation record too — the profile is an input at this tier.
- Scala minor-version note: jars target scala-library 2.13.10; the spike ran fine
  on 2.13.18 (binary compatible within 2.13), but pin what hit-nexus's POM says.

## Direct engine vs CDC wrapper — recommendation update

The research report recommended the CDC wrapper as the integration point. The spike
flips that: `SyncHL7Validator` is directly usable, and the wrapper's value-adds are
mostly negative for this workspace — it filters the Report down to ERROR/WARNING
(discarding Affirmative/Informational/Spec-Error signal the verdict policy and the
mutation loop need), hides the msg-id choice, and its README/POM version drift is a
supply-chain smell. Depend on the three `gov.nist` coordinates directly; keep the
wrapper as a worked example only.
