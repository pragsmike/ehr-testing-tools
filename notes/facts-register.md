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
