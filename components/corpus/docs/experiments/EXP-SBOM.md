# EXP-SBOM — NIST/CDC license and artifact-provenance inventory

**Objective.** Determine, with primary-source evidence, the license and
artifact-provenance status of (a) NIST v2-validation
(`usnistgov/v2-validation`), (b) NIST IGAMT (`usnistgov/hl7-igamt`), and
(c) CDC `lib-hl7v2-nist-validator` — including the full dependency
closure the CDC wrapper pulls in (especially anything sourced from the
NIST-hosted Nexus) — evaluated against this repo's Apache License 2.0
distribution target (`notes/ADRs.md` ADR-0001).

**Decision informed.** The v2 gate architecture choice (NIST-engine-based
"full gate" vs. HAPI-light + local rules, `docs/experiments.md`); the
go-public gate's licensing condition; resolution of facts-register
[F1](../../../../notes/facts-register.md) and the guide repo's claims-register
F3.

**Apparatus.** Public GitHub repositories only — no local checkout or
build required for the license/provenance inventory itself:

- `usnistgov/v2-validation` (root listing, subproject listings,
  source-header sampling)
- `usnistgov/hl7-igamt` (root listing, subproject listings)
- `CDCgov/lib-hl7v2-nist-validator` (root listing, `pom.xml`/build
  metadata, `.mvn`/Maven `settings.xml` if present, source headers)
- `raw.githubusercontent.com` and `api.github.com` for direct file and
  license-detector access
- NIST website pages the above repos link to (software/licensing policy)
- Maven Central search (`search.maven.org` / `repo1.maven.org`) to check
  whether NIST-Nexus-hosted coordinates are mirrored there

**Procedure.**

1. For each of the three repos, enumerate license evidence
   systematically: LICENSE/COPYING files at root and in every
   subproject directory, a representative sample of source-file headers
   (not cherry-picked — sample across subprojects/languages present),
   `pom.xml`/`build.sbt`/Gradle license metadata blocks, README license
   statements, and any NIST/CDC policy pages the repos link to. Record
   exact paths/URLs for every piece of evidence found or checked-and-
   absent.
2. For the CDC wrapper specifically: read its build files
   (`pom.xml`/equivalent) to list every dependency coordinate, resolve
   each dependency's source repository (Maven Central vs. NIST Nexus vs.
   other), and its license per that dependency's own POM metadata or
   upstream project. Flag the exact file/line of any SSL-verification-
   disabled repository configuration. For every NIST-Nexus-sourced
   coordinate, check Maven Central for a matching group/artifact ID to
   determine whether it's also available from a standard, trusted
   repository.
3. Check for a published US-Government/public-domain declaration
   covering NIST's Systems Interoperability Group software specifically.
   A NIST page that names these artifacts (or the group that produces
   them) counts as evidence; a general federal-agency software policy
   page with no link to these specific artifacts is recorded as
   suggestive-not-sufficient, not as resolving the question.
4. Classify every component and every CDC-wrapper dependency as one of:
   `license-verified-compatible`, `license-verified-incompatible`, or
   `license-unstated`. For every `license-unstated` classification,
   record the exact evidence that is absent (e.g. "no LICENSE file at
   root or in any subproject; no SPDX header found in a N-file sample of
   M files checked").
5. Draft (do not send) a short inquiry email to the appropriate NIST
   contact — identified from the repos' own pages or NIST tool pages —
   asking for license clarification on the named repositories.
6. Write up findings, self-score against the results rubric, update
   `docs/experiments.md` and the facts register.

**Expected artifacts.**

- `docs/experiments/EXP-SBOM-results.md` — classification tables,
  dependency-closure inventory, evidence log, self-score.
- `docs/experiments/EXP-SBOM-inquiry-draft.md` — the drafted (unsent)
  NIST inquiry email.
- Facts-register updates: F1's status line, plus new F-rows for any
  CDC-wrapper dependency license newly verified during this inventory.
- `docs/experiments.md` EXP-SBOM row updated to executed, linking
  results.

**Acceptance.** Every component and every CDC-wrapper dependency
classified as `license-verified-compatible` /
`license-verified-incompatible` / `license-unstated`, with exact evidence
(or exact evidence-absence) recorded for each — plus, for every
`license-unstated` case, a drafted inquiry the author can send to close
the gap.

**Stop condition.** This is inventory, not adjudication: where evidence
is absent, the classification is `license-unstated` and the absence is
documented — do not infer US-Government/public-domain status from the
fact that NIST is a federal agency, or from any other vibes-based
reasoning. The architecture decision (which gate design to adopt) is
explicitly out of scope; this experiment classifies, it does not decide.
Effort cap: one session; if exceeded, stop and report state.
