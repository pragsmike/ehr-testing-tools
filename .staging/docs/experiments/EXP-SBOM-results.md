# EXP-SBOM — Results

## Metadata

- **Experiment:** EXP-SBOM
- **Date:** 2026-07-23
- **Executor:** Claude Code session (EXP-SBOM prompt,
  `.agents/prompts/archive/2026-07-24-exp-sbom.md`)
- **HEAD at execution:** `a0026e7` (EXP-SBOM protocol commit)
- **Protocol:** [`EXP-SBOM.md`](EXP-SBOM.md)

## Environment record

<!-- Adapted from the determinism-oriented template (EXP-A4's domain) to
     a research/inventory experiment: no build/JVM environment exists to
     record, so the fields below record the equivalent — what was
     queried, when, and against what snapshot — so an independent party
     could re-run the same checks and know whether a divergence is real
     or just upstream repos having moved on since this date. -->

| Field | Value |
|---|---|
| OS / kernel | N/A — inventory research only; no local build or execution environment involved |
| JVM(s) used | N/A |
| Locale / timezone (host default) | UTC (all repo-tree/API queries and timestamps below) |
| Artifact(s) resolved (name, version, sha256) | Repo tree snapshots, not build artifacts: `usnistgov/v2-validation@master` (229 tree entries, `git/trees` API, `truncated:false`); `usnistgov/hl7-igamt@master` (5,709 entries, `truncated:false`); `CDCgov/lib-hl7v2-nist-validator@main`. One CDC-vendored jar was downloaded and its `META-INF/MANIFEST.MF` inspected: `gov.nist:hl7-v2-validation:1.7.3` (`lib/gov/nist/hl7-v2-validation/1.7.3/hl7-v2-validation-1.7.3.jar`, 1,077,226 bytes) — no sha256 recorded, not needed for the license question it was opened to answer |
| Config file(s) used (path, sha256) | N/A — all evidence gathered via public GitHub REST API (`api.github.com`), raw file fetch (`raw.githubusercontent.com`), and Maven Central search (`search.maven.org`, `repo1.maven.org`); no local config |

## Per-round findings

<!-- Each "round" here is one investigation target rather than a
     determinism round; the "Divergence observed" column is repurposed
     as the finding, "Classification" uses this protocol's own
     three-way scheme (license-verified-compatible /
     license-verified-incompatible / license-unstated) rather than
     pin/control/canonicalize, since that's what EXP-SBOM's Acceptance
     section actually calls for. -->

### Round: NIST v2-validation (`usnistgov/v2-validation`)

| Finding | Evidence | Classification | Action taken |
|---|---|---|---|
| No LICENSE/COPYING/NOTICE anywhere in the repo | Full recursive tree (229 entries, `truncated:false`) grepped for `(^|/)(LICENSE\|LICENCE\|COPYING\|NOTICE)`; zero matches at root or in any of the 3 sbt modules (`parser`, `profile`, `validation`) or `dependencies/`, `project/` | license-unstated | Recorded below; inquiry draft maintained privately by the author |
| No license block in build metadata | `build.sbt` (root) has no `licenses` key; `project/Dependencies.scala`, `project/plugins.sbt` — dependency/plugin pins only; the two bundled dependency POMs under `dependencies/` (`validation-report-1.1.0.pom`, `xml-util-2.1.0.pom`) have no `<licenses>` block; no `pom.xml` exists anywhere in the tree | license-unstated | same |
| No copyright/license text in a 10-file header sample spread across all 3 modules, Java + Scala, main + test | Sampled: `parser/.../Component.scala`, `parser/.../DefaultParser.scala`, `parser/.../EscapeSeqHandlerSpec.scala`, `profile/.../domain.scala`, `profile/.../XMLSerializer.scala`, `validation/.../Detections.java`, `validation/.../Validator.java` (Java), `validation/.../Validator.scala`, `validation/.../Evaluator.scala`, `validation/.../SimpleElementValidatorSpec.scala`. 3 of 10 carry only an `@author Salifou Sidi M. Malick` tag (authorship credit, not a license/copyright grant); the other 7 have no header content at all | license-unstated | same |
| README has no license section, no NIST policy link, no mention of "license"/"copyright"/"public domain" | `README.md` (53 lines) — project description, two dependency mentions (both other `usnistgov` repos), build instructions only | license-unstated | same |
| GitHub's own license detector finds nothing to classify | `GET /repos/usnistgov/v2-validation/license` → HTTP 404 | license-unstated | same |
| **Caveat**: full-text content search across all 229 files was not performed | GitHub code-search API (`api.github.com/search/code`) requires authentication and returned 401; only the specific files/locations listed above were checked. It remains possible, though unindicated by any evidence gathered, that license text exists in an unsampled file (e.g. a resource/XSD/config file) | n/a — recorded as a scope limit, not a finding | Noted; does not change the license-unstated classification, which rests on the systematic (not exhaustive-of-every-byte) checks above |

**Repo-level classification: `license-unstated`.** This is the strongest possible negative finding within this experiment's scope: no LICENSE artifact, no build metadata, no source-header evidence anywhere sampled, no README statement, and GitHub's own detector agrees there's nothing to find. No repository-specific NIST public-domain declaration was located (contrast with IGAMT, below).

### Round: NIST IGAMT (`usnistgov/hl7-igamt`)

| Finding | Evidence | Classification | Action taken |
|---|---|---|---|
| `usnistgov/hl7-igamt` confirmed canonical and live | `GET /repos/usnistgov/hl7-igamt`: `archived:false`, `pushed_at: 2026-05-13T14:28:34Z`, default branch `master`. Cross-checked against `usnistgov` org repo listing (3 pages, 300 repos) and a repo-name search for "igamt" (12 hits) — other igamt-named repos (`igamt`, `igamt-hl7-data`, `hl7-igamt-infrastructure`, etc.) are auxiliary/infra, not alternate canonical homes | n/a (repo identification) | Used as the primary target per the protocol |
| No LICENSE/COPYING/NOTICE/DISCLAIMER/CONTRIBUTING anywhere in the repo | Full recursive tree (5,709 entries, `truncated:false`) grepped for `license\|copying\|notice\|disclaimer\|contributing` in path components; zero matches at root (38 top-level entries) or in any of the ~35 module subdirectories | license-unstated (repo level) | Recorded below |
| Root `pom.xml` has no `<licenses>` block | `groupId=gov.nist.hit.hl7`, `artifactId=igamt`, 24 declared `<module>`s; parented on `spring-boot-starter-parent:2.1.3.RELEASE` | contributes to license-unstated | same |
| Frontend `package.json` license fields disagree/are absent | `igamt-hl7-client-v1/package.json`: `"license": "PrimeNG Commercial"` (a third-party UI-library license string, almost certainly a copy-paste artifact — not a deliberate statement about this project's own code, reported as-is with no interpretation); `igamt-hl7-client-v2/package.json`: no `"license"` field at all | contributes to license-unstated | same |
| **2 of 12 sampled source files carry a first-party NIST public-domain header** | `conformance-profile/src/main/java/.../ConformanceProfile.java` and `valueset/src/main/java/.../Valueset.java` — identical Javadoc block asserting the file was developed by federal employees in the course of official duties, citing 17 U.S.C. §105, stating it is not subject to copyright and is in the public domain, with warranty disclaimer and attribution-on-redistribution language. The other 10 sampled files (5 other backend modules: `common-base`, `legacy`, `segment`, `structure-editor`, `web-app`, `co-constraint`; both Angular frontends: `igamt-hl7-client-v1`, `igamt-hl7-client-v2`) have no header at all | **This specific evidence rises above "suggestive"** — it is a first-party, artifact-embedded declaration, not a general agency policy page. But it covers only these 2 files, not the repo, so it does not license the whole repository | Recorded as a distinguishing finding (contrast with v2-validation, where zero such evidence exists anywhere); new facts-register row F8 |
| README has no license/copyright statement | `README.md` (7 lines): title, build badge, one-paragraph description, a link to the repo's own wiki. No nist.gov link, no license mention | contributes to license-unstated | same |
| GitHub's own license detector finds nothing to classify | `GET /repos/usnistgov/hl7-igamt/license` → HTTP 404; repo metadata `"license": null` | contributes to license-unstated | same |

**Repo-level classification: `license-unstated`** — inconsistent, partial coverage (2 of 12 sampled files) is not sufficient to license the repository, and there is no root grant. **But this is evidentially distinct from v2-validation**: IGAMT has at least some artifact-embedded public-domain evidence, in the specific modules sampled (`conformance-profile`, `valueset`); v2-validation has none anywhere. The inquiry draft flags this distinction explicitly.

### Round: CDC `lib-hl7v2-nist-validator` — wrapper license

| Finding | Evidence | Classification | Action taken |
|---|---|---|---|
| Root `LICENSE` is standard Apache-2.0 boilerplate, `Copyright 2024 CDC.gov` | `https://raw.githubusercontent.com/CDCgov/lib-hl7v2-nist-validator/main/LICENSE` (552 bytes); `pom.xml` independently declares a matching `<licenses><license><name>The Apache Software License, Version 2.0</name>...` block | **license-verified-compatible** | Reconfirms facts-register F6 unchanged; F6's last-verified date updated below |
| GitHub's detector still misclassifies the repo | `GET /repos/CDCgov/lib-hl7v2-nist-validator/license` → `{"license":{"key":"other","name":"Other","spdx_id":"NOASSERTION"}}` | n/a (detector limitation, not a license fact — F6 already documents this) | No change needed |

### Round: CDC wrapper — dependency closure and the SSL/Nexus claim

This is the most consequential finding of the experiment: it **corrects an unregistered claim** in `docs/components.md` ("Its build pulls NIST artifacts from a NIST-hosted Nexus rather than Maven Central, with SSL verification disabled in upstream build config"). That sentence had no F-row (a gap in itself — the facts-register discipline requires one for any load-bearing externally-verifiable claim) and does not match the current build.

| Finding | Evidence | Classification | Action taken |
|---|---|---|---|
| CDC's `pom.xml` declares exactly one `<repository>`, a **local, in-repo, file-based** repository — not a live network Nexus | `pom.xml` lines 49–54: `<repository><id>local-nist</id><url>file://${project.basedir}/lib</url></repository>`. No `<distributionManagement>` is active (a GitHub Packages block exists but is commented out, lines 36–39) | n/a (provenance fact) | Corrects `docs/components.md`; new F-row F9 |
| No SSL/TLS-verification-disabling configuration found anywhere in the searched surface | Checked `pom.xml`, `.github/workflows/mvn-settings.xml` (only Sonatype Central publish credentials + GPG passphrase server entry), `.github/workflows/deploy-to-maven.yml`; no `.mvn/` directory or other `settings.xml` exists in the tree. No `<httpConfiguration>`, custom wagon provider, `insecure`, or cert-check-disabling setting anywhere | n/a — **confirmed absence**, not an unchecked gap | Corrects `docs/components.md`; F9 |
| NIST artifacts are checked into git as a vendored local Maven repo (`lib/**`), not fetched at build time from any Nexus | Six jar+pom pairs confirmed present under `lib/gov/nist/**` and `lib/com/github/hl7-tools/**` (paths and byte sizes recorded in the raw evidence); `hl7-v2-validation-1.7.3.jar` opened directly — its `META-INF/MANIFEST.MF` has vendor/version fields only, no license/copyright string, no `META-INF/LICENSE`/`NOTICE` | contributes to license-unstated for the 6 NIST-origin coordinates | F9 |
| `hit-nexus.nist.gov` **does** exist, but only as inert metadata inside the *vendored NIST POMs' own* publish-target declarations — never referenced by CDC's own `pom.xml` | Three vendored `.pom` files (`hl7-v2-parser`, `hl7-v2-profile`, `hl7-v2-validation`, all 1.7.3) each declare `<repository><id>HIT Nexus</id><url>https://hit-nexus.nist.gov/repository/releases/</url></repository>`; two more (`hl7-v2-schemas`, `validation-report`) declare matching `<distributionManagement>` blocks. These are NIST's own upstream publish metadata, shipped as-is inside the vendored POMs — not live inputs to CDC's build, which never resolves against `hit-nexus.nist.gov` | n/a (provenance fact, primary evidence that this Nexus is NIST-controlled) | F9 |
| README confirms the vendoring and names a **CDC-internal** (not NIST) mirror | Verbatim (both under 15 words): "This project uses some 3rd party code from NIST." / "These libraries have additionally been cached and available on the CDC ImageHub Nexus repository (imagehub.cdc.gov)." Neither `hit-nexus.nist.gov` nor `imagehub.cdc.gov` was queried live — both are plausibly access-restricted internal systems; their existence is reported as source-asserted evidence only | n/a | F9 |
| 6 NIST-origin dependency coordinates carry no license metadata anywhere checked | `gov.nist:hl7-v2-parser:1.7.3`, `gov.nist:hl7-v2-profile:1.7.3`, `gov.nist:hl7-v2-validation:1.7.3`, `gov.nist:xml-util:2.1.0`, `gov.nist.hit:hl7-v2-schemas:1.7.2`, `com.github.hl7-tools:validation-report:1.2.0` — none of their six `.pom` files has a `<licenses>` block; the one jar opened has no LICENSE/NOTICE inside it | **license-unstated** (all 6) | F9; these are the dependency-closure analogue of the v2-validation finding above — same upstream engine, same absence |
| None of the 6 NIST-origin coordinates exist on Maven Central under any tried coordinate | `search.maven.org` queries for `g:"gov.nist"`, `g:"gov.nist.hit"`, `g:"com.github.hl7-tools"` → 0 results each; cross-checked by artifact ID (`hl7-v2-validation`, `hl7-v2-parser`, `hl7-v2-profile`, `hl7-v2-schemas`, `validation-report`) → 0 results each; direct `repo1.maven.org` path probes for all 6 → HTTP 404 in every case | n/a (answers the protocol's explicit "is it also on Maven Central" question: **no**) | F9 |
| 20 non-NIST dependency coordinates (direct + transitive) resolve from Maven Central with stated licenses | Full table below | mixed — see below | F9 (aggregate); F10 for the one outlier |

**Full non-NIST dependency table** (coordinate → scope → license, per that artifact's own Maven Central POM unless noted):

| Coordinate | Scope | License (evidence) | ADR-0001 classification |
|---|---|---|---|
| `com.google.code.gson:gson:2.10.1` | runtime | Apache-2.0 (POM `<licenses>`) | license-verified-compatible |
| `org.jetbrains.kotlin:kotlin-stdlib:1.9.0` | runtime | Apache-2.0 (POM) | license-verified-compatible |
| `org.jetbrains.kotlin:kotlin-test:1.9.0` | test | Apache-2.0 (POM) | license-verified-compatible |
| `org.scala-lang:scala-library:2.13.10` | runtime (transitive, all 4 NIST-vendored Scala libs) | Apache-2.0 (POM) | license-verified-compatible |
| `com.typesafe:config:1.4.2` | runtime (transitive) | Apache-2.0 (POM) | license-verified-compatible |
| `org.apache.commons:commons-lang3:3.12.0` | runtime (transitive) | Apache-2.0 by Apache Commons project convention — its own leaf POM (200 OK) has no `<licenses>` block; inherits from the `commons-parent:52` chain, not independently dereferenced this session | license-verified-compatible (evidence one hop removed — flagged, not re-fetched) |
| `org.apache.httpcomponents:httpclient:4.5.9` | runtime (transitive) | Apache-2.0 by HttpComponents project convention — same evidence caveat as commons-lang3 (leaf POM has no `<licenses>` block, inherits from parent, not independently dereferenced) | license-verified-compatible (evidence one hop removed — flagged) |
| `com.fasterxml.jackson.core:jackson-databind:2.17.0` | runtime (transitive) | Apache-2.0 ("The Apache Software License, Version 2.0", POM `<licenses>`) | license-verified-compatible |
| `org.scala-lang.modules:scala-xml_2.13:1.3.0` | runtime (transitive) | Apache-2.0 (POM) | license-verified-compatible |
| `org.slf4j:slf4j-api:2.0.5` | runtime | MIT by slf4j project convention — leaf POM (200 OK) has no `<licenses>` block; inherits from parent chain, not independently dereferenced this session | license-verified-compatible (evidence one hop removed — flagged) |
| `org.junit.jupiter:junit-jupiter-engine:5.9.0` | runtime (declared non-test scope in CDC's pom.xml) | Eclipse Public License v2.0 (POM `<licenses>`) | license-verified-compatible — EPL-2.0 is weak/file-scoped copyleft consumed unmodified; ADR-0001 doesn't name EPL, but explicitly blesses the analogous MPL-election pattern for weak copyleft consumed as an unmodified dependency. **This is an interpretation by analogy, not literal ADR-0001 text — flagged for author confirmation** |
| `org.junit.jupiter:junit-jupiter-api:5.9.0` | test | EPL-2.0 (POM) | license-verified-compatible (same analogy-flag as above; also test-scope, never distributed) |
| `org.junit.jupiter:junit-jupiter-params:5.9.0` | test | EPL-2.0 (POM) | license-verified-compatible (same) |
| `junit:junit:4.13.1` | test (transitive, via `hl7-v2-schemas` POM) | Eclipse Public License 1.0 (POM) | license-verified-compatible (same EPL analogy-flag; test-scope) |
| `junit:junit:4.13.2` | test (transitive, via `validation-report` POM) | Eclipse Public License 1.0 (POM) | license-verified-compatible (same) |
| `org.specs2:specs2-core_2.13:4.19.2` | test (transitive) | MIT-style (POM `<licenses>` present; exact SPDX string not re-quoted verbatim by the research pass — flagged for a follow-up exact-text check if this dependency ever becomes load-bearing) | license-verified-compatible (evidence caveat noted) |
| `org.specs2:specs2-mock_2.13:4.19.2` | test (transitive) | MIT-style (same caveat) | license-verified-compatible (same caveat) |
| `org.specs2:specs2-scalacheck_2.13:4.19.2` | test (transitive) | MIT-style (same caveat) | license-verified-compatible (same caveat) |
| `org.scalacheck:scalacheck_2.13:1.17.0` | test (transitive) | BSD 3-Clause (POM) | license-verified-compatible |
| `xom:xom:1.3.7` | **runtime** (transitive, via `hl7-v2-profile` and `xml-util` — both themselves license-unstated NIST coordinates) | **GNU LGPL 2.1** (POM `<licenses>`) — the only copyleft license found anywhere in this closure, and the only one carried at runtime (not test) scope | **See F10 below — not bucketed as compatible by analogy the way EPL was; flagged for explicit author adjudication** |

**xom / LGPL-2.1 — why this one is flagged differently than EPL.** ADR-0001 explicitly blesses "MPL/GPL dual-licensed components consumed as unmodified dependencies under their MPL election" and separately states "Copyleft-only dependencies (GPL without an MPL/Apache-compatible election) are adoption blockers." `xom` is single-licensed LGPL 2.1 — no alternative permissive election is offered by its authors, unlike the dual-licensed pattern ADR-0001 names as acceptable. Read strictly, ADR-0001's blocker clause covers it. (In general open-source practice, LGPL consumed unmodified as a runtime dependency is commonly treated as compatible with permissive aggregation — but that is a judgment ADR-0001's own text doesn't make for this repo, and making it is exactly the kind of architecture/legal call this experiment's stop condition puts out of scope.) This finding is also **practically secondary**: `xom` is only pulled in because `hl7-v2-profile` and `xml-util` (both NIST-origin, license-unstated) depend on it — the primary blocker for the full CDC-wrapper path is still the unstated NIST-component licenses, not `xom`. Recorded as F10 for the author's own ADR review, not resolved here.

## Dependency-closure summary (counts)

- **Total dependency coordinates in the CDC wrapper's build** (direct + transitive, excluding the CDC wrapper artifact itself): **26**.
- **By source**: Maven Central — **20**; vendored in-repo / NIST-Nexus-origin, not on Maven Central under any coordinate tried — **6** (all `gov.nist*` or `com.github.hl7-tools`).
- **By classification**: license-verified-compatible — **19** (9 Apache-2.0, 3 MIT/MIT-style-ish evidence, 1 MIT-convention, 3 EPL-2.0-by-analogy, 2 EPL-1.0-by-analogy, 1 BSD-3-Clause); license-unstated — **6** (all NIST-origin); **flagged-for-adjudication (not bucketed)** — **1** (`xom`, LGPL-2.1).
- Note the double-counting caveat: `xom` is verified-licensed (LGPL-2.1 is known) but deliberately not force-classified compatible/incompatible here — see the F10 discussion above. If a reader wants a strict compatible/incompatible-only count, treat `xom` as license-verified-incompatible under a literal ADR-0001 reading, which would make the compatible count 19 and incompatible count 1, with 6 unstated.

## US-Government / public-domain status: suggestive vs. sufficient

Per the protocol's stop condition, this line is deliberately kept separate from the classification tables above:

- NIST publishes an agency-wide open-source licensing policy
  (https://www.nist.gov/open/license, and a canonical license template at
  https://github.com/usnistgov/opensource-repo/blob/main/LICENSE.md)
  describing a public-domain-plus-MIT-variant framework for
  NIST-authored software, grounded in 17 U.S.C. §105. **This is a
  general, agency-wide statement — it does not name `v2-validation`,
  `hl7-igamt`, or any of the CDC-vendored `gov.nist:*` coordinates
  specifically. Per this protocol's own stop condition, it is recorded
  as suggestive, not sufficient**, for any of those artifacts.
- The **two IGAMT source files** with an embedded 17 U.S.C. §105 header
  (`ConformanceProfile.java`, `Valueset.java`) are a different category
  of evidence: a first-party, artifact-level declaration, not a general
  policy page. For those two files specifically, this is sufficient
  evidence of public-domain status. It does not extend to the rest of
  the IGAMT repository (10 of 12 sampled files carry no such header) or
  to `v2-validation` (zero such headers found anywhere in that repo).
- No NIST page found during this inventory names `v2-validation`,
  `hl7-igamt`, or the CDC-vendored NIST coordinates by name and
  declares them public domain. This is the exact gap the drafted
  inquiry (maintained privately by the author) asks NIST to close.

## Protocol amendments made

None. The protocol as written (`EXP-SBOM.md`) was followed as specified;
no procedural corrections were needed during execution. (The
IGAMT-repository-identity check in step 1 — confirming `usnistgov/hl7-igamt`
is still the canonical, live repo rather than having moved — was already
anticipated by the protocol's apparatus section and required no
amendment, just execution.)

## Acceptance verdict

- **Acceptance criterion (quoted from protocol):** "every component and
  dependency classified as one of: license-verified-compatible /
  license-verified-incompatible / license-unstated (with the exact
  evidence absence documented) — plus, for unstated cases, a drafted
  inquiry the author can send."
- **Met?** Yes, with one deliberate exception noted above: `xom`
  (LGPL-2.1) is verified-licensed but not force-bucketed into
  compatible/incompatible, because doing so would require an ADR-0001
  interpretation call this experiment's own stop condition puts out of
  scope (see the F10 discussion). Every other component and dependency
  across all three repos plus the CDC dependency closure (26 + 3 primary
  repos = 29 items) has an explicit classification with cited evidence
  or evidence-absence. The inquiry draft
  (maintained privately by the author) covers both `license-unstated` primary
  repos (`v2-validation`, `hl7-igamt`).
- **Stop condition (quoted from protocol):** "this is inventory, not
  adjudication — where the evidence is absent, record absence; do not
  construe US-Government status from vibes... The architecture decision
  (which gate design to adopt) is explicitly out of scope; this
  experiment classifies, it does not decide."
- **Triggered?** Not triggered as a failure — but its boundary is
  exactly where the `xom` LGPL question and the EPL-by-analogy
  classifications sit, and those are flagged rather than silently
  resolved, in keeping with the stop condition's intent. Effort cap (one
  session) was not exceeded.

## Artifacts produced

| Artifact | Path | Hash |
|---|---|---|
| Results (this file) | `docs/experiments/EXP-SBOM-results.md` | n/a — prose artifact, not hashed per repo convention |
| Inquiry draft | maintained privately by the author | n/a |
| Protocol | `docs/experiments/EXP-SBOM.md` | n/a |

## Rubric self-score

| Criterion | Met? | Evidence |
|---|---|---|
| 1. Every finding is classified | Yes, with one flagged exception | Every row in every per-round findings table has a classification; `xom` is explicitly verified-licensed but deliberately left un-bucketed with reasoning given (see "xom / LGPL-2.1" discussion) rather than silently omitted — this is disclosed, not a gap |
| 2. Environment record is complete | Yes | Every field in the Environment record table is filled, including explicit `N/A` with a stated reason where a determinism-experiment field doesn't apply to a research inventory |
| 3. Amendments are justified | Yes — "none" stated explicitly | See "Protocol amendments made" |
| 4. Verdict is traceable to criteria | Yes | "Acceptance verdict" quotes the protocol's own Acceptance and Stop Condition text verbatim and states plainly whether each was met |
| 5. No unexplained divergences | Adapted — see note | This criterion is written for byte-level determinism comparisons (EXP-A4's domain) and has no literal analogue in a license inventory. The equivalent completeness check applied here: every location checked (LICENSE/COPYING/NOTICE search, build-metadata search, source-header sample, README, GitHub detector, Maven Central lookup) has a stated finding in the tables above — none were checked-and-silently-dropped. Two explicit scope-limit caveats are called out rather than hidden: v2-validation's full-text content search was not possible (GitHub code-search requires auth, 401) and is disclosed as a residual gap; three Maven Central dependencies' license evidence is "one hop removed" (inherited from an undereferenced parent POM) and is flagged rather than presented as directly verified |
