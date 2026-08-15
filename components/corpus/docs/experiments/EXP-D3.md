# EXP-D3 — Offline NIST/CDC HL7 v2 profile validator

**Objective.** Build and run `CDCgov/lib-hl7v2-nist-validator` (the CDC
wrapper around NIST's `v2-validation` engine) fully offline after a
one-time, user-initiated dependency fetch, and characterize what it
actually reports: the shape of a `NistReport`, which classifications
occur against valid and mutated messages, and whether the three
customers `judge.v2`'s base-structural tier dropped as unconvictable
(`corpus/operators.clj`'s docstring; facts register
[F22](../../../../notes/facts-register.md); `docs/judge-calibration.md`
CAL-1) convict here.

**Decision informed.** Whether a resident, profile-aware `judge-v2-nist`
component is worth building next: this experiment supplies the verdict-
mapping evidence (which NIST classifications occur and how), the
profile-reference shape a future site-config needs, and whether the
three named customers actually convict under an applicable profile.
Verdict-mapping itself is **not** decided here — an author ruling, per
this session's own governing prompt.

**Apparatus.**

- `CDCgov/lib-hl7v2-nist-validator`, pinned at its last tagged release,
  `1.4.0` (tag → commit `e059e58ac5592baff57f04ee744398357d3258f3`,
  published 2025-08-29). **Not** `main` — `main` has moved to an
  in-progress `1.5.0` and, as of commit `3fc7950` (2026-04-03), vendors
  the six NIST-origin jars directly into `lib/` as a `file://` Maven
  repo. The last actual *release* does not carry that change; its own
  `pom.xml` and `README.md` still declare a live
  `https://hit-nexus.nist.gov/repository/releases` Maven repository and
  document build flags that disable SSL verification
  (`-Dmaven.wagon.http.ssl.insecure=true` et al.). Pinning to the
  released artifact, not the unreleased tree, is deliberate: it is what
  an actual adopter would consume, and it is the version this
  experiment's own governing prompt anticipated finding (the SSL-bypass
  STOP clause, below) — using `main` instead would have dodged the
  question rather than answered it.
- Six NIST-origin coordinates (`gov.nist:hl7-v2-parser:1.7.3`,
  `gov.nist:hl7-v2-profile:1.7.3`, `gov.nist:hl7-v2-validation:1.7.3`,
  `gov.nist:xml-util:2.1.0`, `gov.nist.hit:hl7-v2-schemas:1.7.2`,
  `com.github.hl7-tools:validation-report:1.2.0`), per facts register
  F9/F14 and `EXP-SBOM-results.md`'s dependency-closure table.
- Apache Maven 3.9.9 (binary distribution, sha512-verified against
  `archive.apache.org`'s published checksum — no package-manager
  install available without root in this environment), invoked with
  `-Dmaven.repo.local=<scratch>/m2-repo` so resolution is isolated from
  any pre-existing `~/.m2`.
- Temurin JDK 21 (already present in the execution environment; not
  re-pinned through the artifact registry for this scratch, out-of-tree
  build — see the Boundary note under Steps).
- The wrapper's own shipped test fixtures:
  `src/test/resources/TEST_PROF/` (`PROFILE.xml` + `CONSTRAINTS.xml` +
  `VALUESETS.xml`, an ORU^R01 "PHIN Spec for Case Notification"
  profile) paired with `src/test/resources/hl7TestMessage.txt`, and
  `src/test/resources/COVID19_ELR-v2.3.1/` (`PROFILE.xml` +
  `CONSTRAINTS.xml` + a value-set file deliberately named
  `VALUESETS-disabled.xml`, i.e. not wired in) paired with
  `src/test/resources/covidELR/231HL7TestFilewithHHSData.txt`. Both
  profiles are ORU^R01 — **neither is ADT**, which bears directly on
  acceptance question (d), below.
- `unshare -r -n` (WSL2's kernel; confirmed available, `util-linux
  2.34`) for network-namespace isolation of the offline runs, matching
  EXP-C5's own precedent.

**Acceptance questions.**

(a) Does the wrapper build and run with zero network after an initial
    user-initiated dependency fetch?
(b) What does a `NistReport` actually carry, field by field?
(c) Which classifications occur in practice against valid and mutated
    messages?
(d) Do the three named customers (drop the PID segment; corrupt PID's
    own segment name; blank a non-header field) convict under an
    applicable profile?
(e) What is the exact transitive jar inventory (coordinates, resolved
    URLs, sha256s) a future lockfile entry set needs?

**Hypotheses going in** (stated so the results can confirm or refute
them, not smuggle them in as findings):

- H1 — The 1.4.0 release's own build instructions (SSL-verification-
  disabling flags) will actually be *necessary* to resolve the six NIST
  coordinates from `hit-nexus.nist.gov`, reproducing the very claim
  facts register F9 corrected (for a *later*, unreleased commit).
- H2 — `NistReport`'s classification vocabulary as CDC's own code
  filters it (`ProfileManager.kt`'s `filterAndConvert`) is narrower
  than the underlying NIST engine's own (`reference.conf` inside
  `hl7-v2-validation-1.7.3.jar` defines seven: `Error`, `Warning`,
  `Alert`, `High Alert`, `Informational`, `Affirmative`, `Specification
  Error`; CDC's wrapper string-matches exactly four of them) — entries
  classified `High Alert`, `Affirmative`, or `Specification Error`
  would be silently dropped from both the entry lists and the summary
  counts. This is a **static** finding from reading the source ahead of
  execution; whether it's ever *observed* against real input is a
  results-file question, not assumed here.
- H3 — Because neither shipped profile is ADT-typed, and no ADT profile
  exists in any of the wrapper's, NIST's, or IGAMT's own published
  sample bundles (checked directly against each repo's tree via the
  GitHub API ahead of writing this protocol), customer conviction
  against the repo's own vendored SimHospital ADT corpus cannot be
  proven this session. The three customer mutations can still be run
  against the shipped ORU profile/message pair as a lesser but real
  data point — conviction there does not prove conviction under an ADT
  profile, and the results file must say so plainly rather than
  conflate the two.

**Procedure.**

1. **Scratch build, outside the workspace tree.** Clone
   `CDCgov/lib-hl7v2-nist-validator` at the pinned commit into a scratch
   directory outside this repo (`/tmp/exp-d3/` under WSL). The
   workspace's own `deps.edn`/`pom.xml` files gain nothing this
   session — this is research, not adoption (ADR-0011's own framing:
   the NIST engine is a *named future* addition, not landed here).
2. **Initial fetch (online, once, explicit).** Run `mvn
   dependency:resolve` (later `mvn package`) against the pinned
   `pom.xml`, unmodified — no SSL-insecure flags added preemptively.
   Record whether resolution succeeds as declared, or whether H1's
   predicted SSL failure actually occurs. If it occurs, this is the
   protocol's own STOP clause for SSL-bypass: do **not** add the
   insecure flags to make it pass; report the exact command and error,
   and fall back to building against `main`'s vendored `lib/` (an
   explicitly named deviation, not a silent substitution) so the
   remaining acceptance questions can still be answered.
3. **Jar inventory.** For each of the six NIST-origin coordinates
   resolved, record: coordinate, the resolving repository ID (from
   Maven's own `_remote.repositories` metadata), and a freshly computed
   sha256 of the jar. Independently confirm `hit-nexus.nist.gov`'s live
   reachability (re-verifying F14) with a plain `curl` (default TLS
   verification, no `-k`) against one coordinate's `.pom`, decoupled
   from Maven's own resolution — a different HTTP/TLS stack corroborates
   or contradicts Maven's result rather than trusting one tool's verdict
   alone.
4. **Full build.** `mvn package` (or `-DskipTests` if the bundled
   `ProfileManagerTest` needs network itself — checked, not assumed) to
   produce the wrapper jar; `mvn dependency:build-classpath` for the
   full runtime classpath needed to drive `ProfileManager` directly.
5. **Offline proof.** Sever network via `unshare -r -n`
   (`-Duser.home` forced explicitly ahead of `-jar`/`-cp` invocations,
   per pattern [#15](../../../../notes/tools/agents/memory/patterns.md) — the same
   uid-remap hazard EXP-C5 hit) and re-run the built classpath against
   the harness described in step 6. A run that still produces a
   `NistReport` is the offline claim's evidence.
6. **Harness against the wrapper's own fixtures.** A small Java test
   harness (scratch-only, not committed to this repo) instantiates
   `ProfileManager(new ResourceFileFetcher(), "/TEST_PROF")` and
   `.../"/COVID19_ELR-v2.3.1"` and calls `.validate(...)` against:
   - each profile's own shipped baseline message, unmodified;
   - mutants of the `TEST_PROF` message (which has a `PID` segment,
     structurally ordinary regardless of the profile's ORU shape)
     produced by simple ER7 text transforms mirroring
     `docs/judge-calibration.md`'s own registered v2 operators:
     `blank-required-field` (MSH-9), `corrupt-encoding-characters`
     (MSH-2), `truncate-segment-fields` (MSH-9), `corrupt-segment-name`
     (MSH → MSX), `malformed-datetime-value` (PID-7 → an invalid date);
   - mutants for the three named customers: drop the `PID` segment
     entirely; corrupt `PID`'s own segment name (`PID` → `PIX`); blank
     PID-7 (empty, not malformed) — run against the same ORU profile,
     explicitly labeled as *not* the ADT conviction question (d) asks,
     since no ADT profile is available (H3).
   For every run, capture the full raw `Report.getEntries()` map (all
   three of `structure`/`content`/`value-set`, each `Entry`'s
   `getCategory()`/`getClassification()`/`getPath()`/`getDescription()`)
   *and* the filtered `NistReport` (`status`, four `SummaryCount`s,
   three `Entries` lists) side by side, so H2 can be checked directly:
   does any raw entry carry a classification the filtered report drops?
7. **SimHospital applicability, checked not assumed.** Before writing
   the results file, directly search `usnistgov/v2-validation`,
   `usnistgov/hl7-igamt`, and this wrapper's own tree (via each repo's
   GitHub tree API) for any ADT-typed sample profile. Record the
   search and its (expected, per H3) negative result rather than
   asserting it from memory.

**Boundary — what this session does NOT do.** No code lands in
`src/` (`components/`); the `judge-v2-nist` component is future work
gated on this evidence plus an author verdict-mapping ruling. Nothing
in `.github/workflows/` changes. The scratch JDK is not re-pinned
through `artifacts.lock.edn` — it's an ambient tool for a throwaway
build outside the repo tree, not a runtime this repo's own CI or gate
depends on; if `judge-v2-nist` is adopted later, *that* session pins
its own JDK/Maven the way EXP-C5's `fhir-validator-cli` pinned Temurin.

**Acceptance.** All five questions (a)–(e) above are answered with
direct evidence (a build log, a report excerpt, a jar inventory table,
a convict/no-convict table) — a negative answer (e.g., "no ADT profile
available, customer conviction unproven") counts as answered, not as a
failure, provided the gap and its cause are stated plainly. The verdict-
mapping table itself is explicitly out of scope for this file to decide.

**Stop conditions.**

- SSL-verification bypass required to resolve any coordinate — reported
  per step 2, build proceeds via the `main`-vendored fallback instead of
  disabling verification.
- A licensing surface worse than the recorded `license-unstated` /
  `:use-permitted--unstated--confirmation-pending` posture (a
  coordinate carrying restrictive license text, a fetch requiring
  acceptance of terms) — stop the fetch, record, report.
- `hit-nexus.nist.gov` unreachable, or any of the six coordinates
  absent from it — itself a finding (F14 re-verification failed);
  record and close honestly, no mirror-hunting.
- Effort cap: this experiment characterizes what the wrapper actually
  reports against available fixtures; it does not author an IGAMT
  profile (a web tool + account, outside an autonomous session's
  reach) to manufacture ADT applicability that doesn't currently exist.

**Expected artifacts.**

- `components/corpus/docs/experiments/EXP-D3-results.md` (this
  protocol's results, per `results-template.md`, self-scored against
  `results-rubric.md`).
- `components/corpus/docs/experiments.md`'s EXP-D3 row, updated with the
  executed date and a one-line pointer to the customer-conviction gap.
- Facts-register rows for the offline-build claim, the SSL/H1 finding,
  and the jar inventory (`notes/facts-register.md`).
- Conditionally, `artifacts.lock.edn` entries for the six coordinates
  (if the URL+sha256 shape fits cleanly) or a documented jar-inventory
  table in the results file instead (if Maven's transitive-graph
  resolution doesn't reduce to a flat URL list) — a decision made in
  the results file, not pre-judged here.
