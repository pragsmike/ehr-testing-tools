# Components

What someone needs to know about the external tools this repo builds on,
at decision-informing depth. Every dated or legal fact here either cites
an F-row in [`notes/facts-register.md`](../notes/facts-register.md) or
carries its own verification date. Depth beyond this lives in
[`docs/research/`](research/).

## Synthea

Synthetic patient population generator: simulates whole lifecycles
(demographics, encounters, conditions, medications, observations) from
configurable modules, producing longitudinally plausible — never real —
patient data. Created and maintained by The MITRE Corporation; open
source, Apache License 2.0 (facts register [F2](../notes/facts-register.md));
current release **v4.0.0** (2026-03-05, facts register F2). Used widely
for research datasets, EHR testing, and demos (e.g. the SyntheticMass
dataset).

In this repo: the generation engine behind `corpus.generate`, run as a
pinned subprocess; its distribution, module sets, and our properties
files are artifacts or repo-authored config per ADR-0005. Native outputs
are FHIR (R4/STU3/DSTU2), bulk ndjson, C-CDA, CSV, CPCDS — not HL7 v2 and
not OMOP (facts register F4 in the guide repo; mirrored evidence in
`docs/research/`). Deliberately not used for: v2 messages (we project
from its output — see EXP-A3) or profile-constrained FHIR beyond what
filtering achieves.

## HAPI HL7v2

The standard JVM library for HL7 v2: parsers (strict and lenient),
message object models for v2.1–2.8, ER7/XML encoding, MLLP transport.
Originated at University Health Network (a multi-site teaching hospital
in Toronto, verified against the project's `pom.xml`); now maintained
under the `hapifhir` organization; dual-licensed MPL/GPL at the
licensee's choice (facts register [F3](../notes/facts-register.md));
current release **v2.6.0** (2025-02-05, facts register F3 — corrects an
earlier draft of this document, which cited 2.5.1/2024; 2.6.0 is the
current tag as of this verification). Used in countless integration
engines and hospital interfaces.

In this repo: in-process parse/serialize engine for `corpus.mutate` (v2)
and the light `gate.v2` tier. Deliberately not used for: profile
conformance beyond the classic HL7 Message Profile XML — its conformance
module predates and does not enforce conformance statements, predicates,
or co-constraints (`docs/research/` D2).

## HAPI FHIR

The dominant JVM FHIR library: parsers, resource models per FHIR
version, client/server frameworks, and a validation module wrapping the
core validation engine. Stewarded by Smile Digital Health (the project's
license page is Smile-Digital-Health-branded and points to Smile CDR for
commercial support; facts register [F4](../notes/facts-register.md));
Apache License 2.0 (facts register F4).

In this repo: in-process FHIR parse/serialize for `corpus.mutate`.
Deliberately not used for: authoritative gate verdicts — the official
validator (below) is canonical where they diverge (`docs/research/` C6).

## Official FHIR validator (`validator_cli` / `org.hl7.fhir.core`)

HL7's reference validation engine for FHIR resources against the base
spec and implementation guides; the tool "does it validate" means in the
FHIR world. The project's own README describes it as HL7-maintained core
FHIR tooling (it is also the basis for HL7's IG publisher); Apache
License 2.0 (facts register [F5](../notes/facts-register.md)).
Distributed as a CLI jar and as the library HAPI FHIR wraps.

In this repo: the engine behind `gate.fhir`, run as a pinned subprocess
with IG packages as locked artifacts; verdicts are consumed as
OperationOutcome data and normalized by `gate.report`. Known operational
constraints that shape our wrapper: offline/no-terminology operation has
open upstream bugs (locally packaged ValueSets can still fail; see
`docs/research/` C1/C5), so verdict policy classifies rather than trusts
raw pass/fail. Deliberately not used for: terminology validation against
licensed vocabularies.

## NIST v2-validation engine

NIST's HL7 v2 conformance-validation core (JVM/Scala): validates
messages against full conformance-profile semantics including usage,
cardinality, conformance statements, predicates, and co-constraints —
the constraint tiers HAPI does not enforce. Steward: NIST Systems
Interoperability Group; license unstated — no LICENSE artifact in the
repository (facts register [F1](../notes/facts-register.md); resolution
pending EXP-SBOM). Underpins NIST's hosted validation tools used in US
EHR certification.

In this repo: candidate engine for the full `gate.v2` tier; adoption
blocked on EXP-SBOM.

## CDC `lib-hl7v2-nist-validator`

CDC's library wrapper around the NIST engine, used in public-health
message processing; Apache License 2.0 (facts register
[F6](../notes/facts-register.md) — note: GitHub's automatic license
detector reports this repository as `NOASSERTION`/"Other", but the raw
`LICENSE` file is unambiguous standard Apache 2.0 boilerplate; the
detector's classification, not the license, is what's wrong here). Its
build pulls NIST artifacts from a NIST-hosted Nexus rather than Maven
Central, with SSL verification disabled in upstream build config — a
supply-chain caveat we will not inherit (mirroring plan: EXP-D3).

In this repo: candidate runtime for the full `gate.v2` tier, contingent
on EXP-SBOM.

## IGAMT

NIST's Implementation Guide Authoring and Management Tool: the authoring
environment for HL7 v2 implementation guides and conformance profiles,
aligned with the HL7 v2 conformance methodology. Hosted by NIST; source
at `usnistgov/hl7-igamt`; license unstated — no LICENSE artifact in the
repository, the same pattern as the NIST v2-validation engine (F1); this
is folded into EXP-SBOM's existing scope (`docs/experiments.md` already
lists IGAMT explicitly alongside NIST v2-validation and the CDC wrapper)
rather than given its own F-row. Exports feed NIST's validation ecosystem
— not HAPI's classic profile format (`docs/research/` D1).

In this repo: upstream authoring tool only, never embedded; its exports
enter as artifacts of kind `:profile` per ADR-0005, pinned with export
date and IGAMT version.

## FSH / SUSHI

FHIR Shorthand is HL7's language for authoring FHIR profiles and
implementation guides; SUSHI is its compiler (Node.js). Per its own
FHIR Foundation Project Statement, SUSHI "is maintained by the HL7
community"; Apache License 2.0 (facts register
[F7](../notes/facts-register.md)).

In this repo: authoring-time only — the JVM constraint applies at
execution, not authoring (problem statement C4). Compiled IG packages
enter as locked artifacts consumed by `gate.fhir`. Deliberately not used
at: run time.

---

License and steward cells verified 2026-07-23.
