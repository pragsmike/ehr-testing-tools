# Facts Register

<!-- Tracks load-bearing, externally verifiable assertions made anywhere in
     this repo (docs, README, experiment backlog) about tools, licenses,
     and ecosystem capabilities — evidence and a last-verified date, so a
     claim can be checked instead of trusted. This is the F-table half of
     ehr-testing-guide's notes/claims-register.md (see that repo's
     ADR-0013); there is no C-table here because there is no manuscript
     tracking drafting coverage. See notes/ADRs.md ADR-0002 and
     AUTHORS-GUIDE.md section 4 for the assert -> register -> date
     discipline this file exists to support. -->

| # | Claim | Where asserted | Evidence | Last verified | Status |
|---|---|---|---|---|---|
| F1 | NIST v2-validation (usnistgov/v2-validation) ships no license artifact (no LICENSE file, no source-header license text); public-domain status plausible but unverified. | `docs/positioning.md` (go-public gate), experiment backlog (EXP-SBOM) | https://github.com/usnistgov/v2-validation (root listing) | 2026-07-22 | unverified — pending EXP-SBOM; this row mirrors ehr-testing-guide claims-register F3, update both when EXP-SBOM resolves |
| F2 | Synthea ships under Apache License 2.0 (GitHub repository-license API, SPDX `Apache-2.0`); current release is v4.0.0, published 2026-03-05 (GitHub's "latest release" endpoint instead returns a non-semver rolling tag, `master-branch-latest`, dated 2026-07-22 — v4.0.0 is the current versioned release). | `docs/components.md` (Synthea) | https://api.github.com/repos/synthetichealth/synthea/license ; https://api.github.com/repos/synthetichealth/synthea/releases | 2026-07-23 | verified |
| F3 | HAPI HL7v2 is dual-licensed MPL/GPL at the licensee's election, per the project's `pom.xml` (no canonical top-level LICENSE file recognized by GitHub's detector); current release is v2.6.0, published 2025-02-05. Mirrors ehr-testing-guide claims-register F2 (license only; that row does not cover the release version). | `docs/components.md` (HAPI HL7v2) | https://raw.githubusercontent.com/hapifhir/hapi-hl7v2/master/pom.xml ; https://api.github.com/repos/hapifhir/hapi-hl7v2/releases | 2026-07-23 | verified |
| F4 | HAPI FHIR ships under Apache License 2.0; its license page is Smile-Digital-Health-branded and points to Smile CDR for commercial support, supporting Smile Digital Health as steward. | `docs/components.md` (HAPI FHIR) | https://api.github.com/repos/hapifhir/hapi-fhir/license ; https://hapifhir.io/hapi-fhir/license.html | 2026-07-23 | verified |
| F5 | The official FHIR validator (hapifhir/org.hl7.fhir.core) ships under Apache License 2.0; its README describes it as HL7-maintained core FHIR tooling (also underlying HL7's IG publisher). | `docs/components.md` (Official FHIR validator) | https://api.github.com/repos/hapifhir/org.hl7.fhir.core/license ; https://raw.githubusercontent.com/hapifhir/org.hl7.fhir.core/master/README.md | 2026-07-23 | verified |
| F6 | CDC's `lib-hl7v2-nist-validator` LICENSE file is standard Apache License 2.0 boilerplate (`Copyright 2024 CDC.gov`), though GitHub's automatic license detector misclassifies the repository as `NOASSERTION`/"Other" — verified by reading the raw file rather than trusting the detector. | `docs/components.md` (CDC lib-hl7v2-nist-validator) | https://raw.githubusercontent.com/CDCgov/lib-hl7v2-nist-validator/main/LICENSE | 2026-07-23 | verified |
| F7 | SUSHI (FHIR/sushi) ships under Apache License 2.0 and, per its FHIR Foundation Project Statement, is maintained by the HL7 community. | `docs/components.md` (FSH/SUSHI) | https://api.github.com/repos/FHIR/sushi/license ; https://raw.githubusercontent.com/FHIR/sushi/master/README.md | 2026-07-23 | verified |
