# Free, sanitized longitudinal HL7v2 corpora and generators

**Research date:** 25 July 2026  
**Decision target:** synthetic or demonstrably de-identified, PII-free HL7v2 event streams suitable for EHR system testing, preferably from Clojure/JVM.

## Executive conclusion

The strongest answer is **Google Simulated Hospital (SimHospital)**, not Synthea. SimHospital was purpose-built to generate a patient's hospital-stay event stream: a YAML/JSON “pathway” drives admission, orders, results, transfers, documents, updates, and discharge, with stable patient/visit/order identifiers across the resulting messages ([repository](https://github.com/google/simhospital), [pathway guide](https://github.com/google/simhospital/blob/master/docs/write-pathways.md)). It is Apache-2.0 and synthetic by construction, but Google archived the repository on 28 March 2025; the last source push was in March 2024 ([repository](https://github.com/google/simhospital)).

The best immediately downloadable corpus is SimHospital's **1,013-message, approximately 1.2 MB `messages.out`** artifact ([direct file](https://github.com/google/simhospital/blob/master/docs/artifacts/messages.out), [generation instructions](https://github.com/google/simhospital/blob/master/docs/sample.md)). A local structural count found 610 `ORU^R01`, 400 `ADT^A01`, two `MDM^T02`, and one `ADT^A34`, covering 403 synthetic patient identifiers. It contains genuine longitudinal subseries—some patients have 6–14 linked messages over time—but it is **not** the desired full admit → order → result → transfer → discharge chain because this particular run contains no `ORM^O01`, `ADT^A02`, or `ADT^A03`. The identical artifact is mirrored in Snowflake Labs' Apache-2.0 demo ([repository and provenance statement](https://github.com/Snowflake-Labs/Snowflake-HL7V2-Parsing), [direct mirror](https://github.com/Snowflake-Labs/Snowflake-HL7V2-Parsing/blob/main/src/test/data/hl7/hl7_2-3_samples.txt)).

No current Synthea release exports HL7v2. Its documented outputs remain FHIR, Bulk FHIR, C-CDA, CSV, and CPCDS ([Synthea repository](https://github.com/synthetichealth/synthea)); its open ADT-export request confirms that a useful event feed requires scheduling/event assumptions absent from ordinary Synthea output ([issue #535](https://github.com/synthetichealth/synthea/issues/535)). The one real community bridge found is **MayaMaker**, a GPL-3.0 C# project that consumes Synthea CSV and invents scheduling between encounters to emit coherent HL7 2.3 ADT sequences; it supports A01/A02/A03/A04/A06/A08/A11/A13/A15/A16 and can process one encounter or a patient's lifetime ([MayaMaker](https://github.com/mayankthebest/MayaMaker)). It does not generate ORM/ORU, and its default-branch source has not materially advanced since April 2020.

For a Clojure/JVM project, the recommended path is:

1. **Use the 1,013-message SimHospital artifact now** as a safe seed corpus and parser/storage fixture.
2. **Run a pinned SimHospital Docker image/source commit with a custom deterministic pathway** to produce the missing end-to-end episode (`A01 → ORM^O01 → ORR^O02 → ORU^R01 → A02 → MDM^T02 → A03`).
3. **Parse and validate in Clojure through HAPI HL7v2 Java interop**; write one message plus extracted correlation keys per Arrow/Parquet row. HAPI is a full Java HL7v2 API, not a clinical-story generator ([HAPI](https://hapifhir.github.io/hapi-hl7v2/)).
4. If Synthea's birth-to-death narrative is mandatory, build a **custom Java Synthea `PatientExporter`** with HAPI. MITRE provides an official Apache-2.0 external exporter template loaded by Java `ServiceLoader`, so Synthea itself need not be forked ([custom exporter template](https://github.com/synthetichealth/custom-exporter-template)). This is a custom mapping project, not an existing turnkey FHIR→HL7v2 pipeline.

## 1. Existing free corpora

### Comparison

| Candidate | What is actually present | Coherent patient sequence? | PII/synthetic confidence | License / availability | Freshness and verdict |
|---|---|---|---|---|---|
| **SimHospital `messages.out`** | 1,013 HL7 2.3 messages: 610 ORU^R01, 400 ADT^A01, 2 MDM^T02, 1 ADT^A34; ~1.2 MB; 403 synthetic patient IDs by local count ([file](https://github.com/google/simhospital/blob/master/docs/artifacts/messages.out)) | **Yes, partially.** Stable patient IDs connect historical results and admission; one patient has 14 messages. No order/transfer/discharge in this particular artifact. | **High.** Official docs say it generates realistic configurable data so developers can test without real data ([README](https://github.com/google/simhospital)). Synthetic, rather than transformed real records. | Apache-2.0; direct download in repo ([license](https://github.com/google/simhospital/blob/master/LICENSE)) | Generated in 2020-era sample; repo archived 2025. **Best ready corpus**, with narrow event coverage. |
| **Snowflake Labs HL7v2 sample** | Same 1,013-message SimHospital file, packaged with Java/Python parsing demos ([README](https://github.com/Snowflake-Labs/Snowflake-HL7V2-Parsing), [file](https://github.com/Snowflake-Labs/Snowflake-HL7V2-Parsing/blob/main/src/test/data/hl7/hl7_2-3_samples.txt)) | Same coherence and limitations as above. | **High**, because Snowflake explicitly attributes the file to SimHospital ([README](https://github.com/Snowflake-Labs/Snowflake-HL7V2-Parsing)). | Apache-2.0 ([repository](https://github.com/Snowflake-Labs/Snowflake-HL7V2-Parsing)) | Last source push March 2023. Useful stable mirror, not a second independent corpus. |
| **ANS France `hl7V2-exemples`** | Locally counted 65 messages: ADT A01/A03, ORU R01, MDM T02/T04/T10, ACKs, and French ZAM events ([repository](https://github.com/ansforge/hl7V2-exemples)). Includes an explicit linked admission/discharge pair for test patient `PAT-TROIS DOMINIQUE` and initial/replacement/deletion document/result sets ([SGL guide](https://github.com/ansforge/hl7V2-exemples/tree/main/SGL)). | **Small linked scenarios**, not a broad longitudinal corpus. Strong for French PAM/SEGUR/CDA-document workflows. | **Medium-high.** The repository explicitly labels the identity as a test patient ([SGL guide](https://github.com/ansforge/hl7V2-exemples/tree/main/SGL)); other folders should still be reviewed before bulk use. | Public, but **no repository license detected**; treat redistribution/derivatives as legally unclear ([repository](https://github.com/ansforge/hl7V2-exemples)). | Active through 18 June 2026. Technically useful, licensing blocks a clean corpus recommendation. |
| **HL7 International v2 samples** | A large attachment library spanning ADT, SIU, OML, ORM, ORU, VXU, PPR, DFT, MFN, MDM, and RDE ([HL7 page](https://confluence.hl7.org/spaces/OO/pages/49644116/v2+Sample+Messages)). | Mostly message-by-message examples; “add/update/delete” or vaccine-history files can be related, but no corpus-level patient chronology is promised ([HL7 page](https://confluence.hl7.org/spaces/OO/pages/49644116/v2+Sample+Messages)). | **Unconfirmed.** Sample status does not itself establish PII-free provenance for every attachment. | Publicly readable; page gives **no explicit corpus license** ([HL7 page](https://confluence.hl7.org/spaces/OO/pages/49644116/v2+Sample+Messages)). | Attachments were added through at least August 2024. Excellent syntax coverage, not suitable as a hard-guarantee PII-free longitudinal corpus without per-file review. |
| **NIST HL7v2 conformance suites / GVT resources** | Free context-free and context-based tools for immunization, syndromic surveillance, lab results/ELR, and patient-care-device profiles; directed test cases include typical real-world scenarios and example messages ([NIST tools](https://www.nist.gov/itl/health-it-testing-infrastructure/testing-tools/hl7-v2-conformance-testing-tools)). | Test-case coherence inside a profile, but not a general multi-department patient lifetime corpus. | Test content is intended for conformance, but the public overview does not make a blanket no-PII assertion for every downloadable artifact ([NIST tools](https://www.nist.gov/itl/health-it-testing-infrastructure/testing-tools/hl7-v2-conformance-testing-tools)). | Free public use/download; individual GitHub resource repositories do not consistently declare an SPDX license ([GVT resources](https://github.com/usnistgov/gvt-resource-bundle)). | NIST repos were active in 2026. Best for validation edge cases, not corpus volume or longitudinal narrative. |
| **HAPI HL7v2 tests/examples** | Local inspection of the current repo found about 76 embedded ER7 messages across tests/examples, including ADT, ORU, ORM, OML/OUL, BAR and QRY ([repository](https://github.com/hapifhir/hapi-hl7v2)). | No: unit fixtures and code examples, generally isolated. | Test fixtures are not advertised as a sanitized corpus; inspect individually. | HAPI is dual MPL-1.1/GPL ([license](https://hapifhir.github.io/hapi-hl7v2/license.html)). | Source active in 2026; latest advertised release 2.5.1 on 1 Nov 2023 ([HAPI site](https://hapifhir.github.io/hapi-hl7v2/)). Useful tests, not a corpus. |
| **`bqfan/sample-hl7-messages`** | Local count: 258 messages in 68 files, aggregated from three older repositories; broad ADT/ORM/ORU/SIU/MDM/REF/VX coverage ([repository](https://github.com/bqfan/sample-hl7-messages)). | Some identifiers recur and some pairs exist, but the README describes aggregation rather than a designed patient story ([README](https://github.com/bqfan/sample-hl7-messages)). | **Low confidence.** One upstream collection calls itself “a bunch of random sample messages from various sources” ([Mirth resources](https://github.com/tiskinty/Mirth-Connect-Resources)). Do not assume de-identification. | No license in the aggregator; upstream licenses/provenance vary ([repository](https://github.com/bqfan/sample-hl7-messages)). | One commit in March 2024. Reject for the hard PII/license requirement unless every file is independently cleared. |
| **Mirth Connect Resources sample collection** | Locally counted 191 messages in one file, broad ADT plus some ORU/ORM/SIU/MDM/VX/REF ([repository](https://github.com/tiskinty/Mirth-Connect-Resources)). | Reused test-patient IDs occur, but it is not designed as a single chronology. | **Low confidence:** README explicitly says random samples from various sources ([README](https://github.com/tiskinty/Mirth-Connect-Resources)). | No license detected. | Last source change July 2023. Reject for strict PII-free use. |
| **Academic INPC synthetic-message study** | 2014 study generated segment frameworks with Markov-chain and resampling models over 33 event types; source was a de-identified 24-hour Indiana Network for Patient Care feed ([paper](https://pmc.ncbi.nlm.nih.gov/articles/PMC4419874/)). | No patient-level narrative; structural/message-frequency synthesis only. | Source was de-identified, but neither source data nor code is openly downloadable; software was “available upon request” ([paper](https://pmc.ncbi.nlm.nih.gov/articles/PMC4419874/)). | Paper free; corpus/code not released under a public license. | Historical research lead, not an actionable corpus. |
| **IHE De-identification Handbook example** | One ADT^A08 shown before, pseudonymized, and anonymized ([IHE example](https://build.fhir.org/ig/IHE/ITI.DeIdHandbook/branches/main/hl7-example.html)). | No; stages of one message. | Explicitly demonstrates masking/pseudonymization/anonymization. | Continuous-build handbook; no explicit example-corpus license on page. | Useful as a sanitization test vector only. |
| **MIMIC / PhysioNet / i2b2** | No freely downloadable raw HL7v2 event-stream corpus was identified in the reviewed catalogs; their commonly available research datasets are normalized tables, notes, waveforms, or derived clinical records rather than source interface feeds ([PhysioNet catalog](https://physionet.org/content/), [i2b2 datasets](https://www.i2b2.org/NLP/DataSets/)). | Not applicable. | Their de-identification regimes do not create the requested HL7v2 files. | Dataset-specific. | Do not plan around a “MIMIC HL7 feed”; none was located. |

### Best existing corpus: precise assessment

SimHospital's bundled file is unusually valuable because its documentation gives both a direct artifact and the exact generation command ([sample page](https://github.com/google/simhospital/blob/master/docs/sample.md)). The 1,013 messages are ordered output from a simulator, not independent snippets, and PID-3 values recur consistently across historical ORUs and admissions ([direct artifact](https://github.com/google/simhospital/blob/master/docs/artifacts/messages.out)). Local analysis found this distribution of messages per synthetic patient: 345 patients with two messages; 17 with three; 13 with eight; 10 with six; and a smaller tail up to 14. That makes it useful for grouping, sorting, partitioning, deduplication, timestamp handling, correlation-key extraction, and longitudinal Parquet/Iceberg tests.

Its limitation is substantive: the artifact demonstrates mostly historical lab observations followed by admission. It does not exercise order/result correlation (`ORC-2/ORC-3`, `OBR-2/OBR-3`) across ORM and ORU, patient movement through A02/A03, or appointments through SIU. Those gaps are best filled by rerunning the same generator with a custom pathway rather than mixing in unrelated samples.

For a strict “no real PII” policy, synthetic-by-construction is safer than de-identifying a real interface dump. Still, SimHospital can generate realistic names, addresses, telephone numbers, and **valid-format NHS numbers** ([pathway guide](https://github.com/google/simhospital/blob/master/docs/write-pathways.md)). Before publishing a derived corpus, replace PID-3/5/7/11/13/14, PV1-7/8/9/17, ORC-12, OBR-16 and free text with unmistakable reserved values, preserve referential integrity through deterministic keyed hashes, and run a deny-list/regex scan. This avoids accidental collision with a real person even when every record was algorithmically generated.

## 2. Generators and toolkits

### Longitudinal generators

| Tool | Longitudinal capability and coverage | Runtime / fit | License | Activity as of 25 Jul 2026 | Setup and verdict |
|---|---|---|---|---|---|
| **Google SimHospital** | **Purpose-built sequences.** YAML/JSON pathways model a patient's stay; steps include admission, transfer, discharge, registration, cancel/pending events, merge, bed swap, order + acknowledgement, results/corrections, documents and clinical notes ([pathway guide](https://github.com/google/simhospital/blob/master/docs/write-pathways.md)). Emits ADT A01/A02/A03/A04/A05/A08/A09/A10/A11/A12/A13/A14/A15/A16/A17/A23/A25/A26/A27/A28/A31/A34/A40; MDM T02; ORM O01; ORR O02; ORU R01/R03/R32 ([message table](https://github.com/google/simhospital/blob/master/docs/write-pathways.md#messages-types-and-pathway-events)). **No SIU support.** | Go executable or Docker; easy to invoke from Clojure as a build fixture/sidecar. Writes file/stdout or sends MLLP ([README](https://github.com/google/simhospital)). | Apache-2.0 ([license](https://github.com/google/simhospital/blob/master/LICENSE)) | Archived 28 Mar 2025; last source push 20 Mar 2024; no formal releases ([repository](https://github.com/google/simhospital)). | **Low–medium setup. Best generator.** Pin commit/image, mount pathway/config and output directory. Maintenance risk is the archive status. |
| **MayaMaker** | Consumes Synthea CSV, adds scheduling between consecutive encounters, and emits one-encounter or lifetime HL7 2.3 ADT scenarios. Supports A01/A02/A03/A04/A06/A08/A11/A13/A15/A16 ([README](https://github.com/mayankthebest/MayaMaker)). Its built-in scenarios include chains such as `A01,A06,A02,A08,A03` and cancellation/pending variants. | C#/.NET with NHapi and bundled database/data; not JVM-native. Bundles roughly 1,173 patients and 57,406 encounter rows in its checkout. | GPL-3.0 ([repository](https://github.com/mayankthebest/MayaMaker)) | Default branch's latest substantive commit is 14 Apr 2020, despite later repository metadata activity. Hosted demo availability should not be assumed. | **Medium/high setup, stale.** Best evidence that a Synthea→HL7v2 bridge exists, but ADT-only and not recommended as the production foundation. |
| **Custom Synthea exporter + HAPI** | Synthea supplies coherent birth-to-death patients and encounter-linked conditions, allergies, medications, vaccinations, observations/labs, procedures and care plans ([Synthea](https://github.com/synthetichealth/synthea)). MITRE's external exporter template calls a `PatientExporter` for every patient via `ServiceLoader` and permits extra Gradle dependencies ([template](https://github.com/synthetichealth/custom-exporter-template)). A custom exporter can therefore sort record events and emit deterministic ADT/ORM/ORU/MDM chains with HAPI. | **Best JVM-native architecture.** Java template; callable/buildable alongside Clojure. | Both Synthea and template are Apache-2.0 ([Synthea](https://github.com/synthetichealth/synthea), [template](https://github.com/synthetichealth/custom-exporter-template)). HAPI is MPL-1.1/GPL dual-licensed ([HAPI license](https://hapifhir.github.io/hapi-hl7v2/license.html)). | Synthea active through 22 Jul 2026; template updated Aug 2025. | **Medium–high implementation.** Not turnkey. Best when full-life narrative matters more than quickest delivery. |

### Message builders, validators, and single-message generators

| Tool | What it does | Does it create a coherent episode? | License and freshness | Practical role |
|---|---|---|---|---|
| **HAPI HL7v2** | Full Java API for typed message structures, parsing, encoding, validation, MLLP and ACK generation; TestPanel edits/transmits/validates messages ([HAPI](https://hapifhir.github.io/hapi-hl7v2/), [TestPanel](https://hapifhir.github.io/hapi-hl7v2/hapi-testpanel/)). | **No.** `MessageGenerator`/`XsdMessageGenerator` in `hapi-sourcegen` generate Java model source from specifications, not randomized clinical stories ([source generator](https://github.com/hapifhir/hapi-hl7v2/blob/master/hapi-sourcegen/src/main/java/ca/uhn/hl7v2/sourcegen/XsdMessageGenerator.java)). UUID/ID generators and ACK helpers are plumbing only. | MPL-1.1 or GPL; source active 2026; latest site release 2.5.1 in Nov 2023 ([license](https://hapifhir.github.io/hapi-hl7v2/license.html), [site](https://hapifhir.github.io/hapi-hl7v2/)). | **Primary Clojure/JVM parser, encoder and validator.** Supply the scenario/state machine yourself or consume SimHospital output. |
| **NextGen/Mirth Connect Message Generator** | UI creates pseudo-valued messages of any trigger/version from HL7 2.1–2.6, with control over segments, fields and components ([NextGen docs](https://docs.nextgen.com/en-US/mirthc2ae-connect-by-nextgen-healthcare-user-guide-3231169/message-generator-14233)). | **No documented linking/state.** Generates templates or individual test messages. A channel script could maintain state, but that is custom work. | NextGen Connect example repo is MPL-2.0 and last pushed Sep 2024 ([examples](https://github.com/nextgenhealthcare/connect-examples)); Message Generator documentation published Oct 2025. | Good interactive single-message fixture builder and transport harness; not a corpus generator. |
| **NIST GVT / IGAMT / TCAMT** | Free profile-driven context-free/context-based validation. IGAMT defines implementation guides, TCAMT defines XML test cases, and GVT automatically builds a testing tool from them ([GVT brief](https://www.nist.gov/document/product-brief-nist-gvt-tool), [tools portal](https://www.nist.gov/itl/health-it-testing-infrastructure/testing-tools/hl7-v2-conformance-testing-tools)). | Directed test cases can express scenarios, but no general patient-story/random corpus generator was found. “Automatically generates test tools” does **not** mean it synthesizes arbitrary message streams. | Free; NIST repos active in 2026, but individual resource repos often lack explicit SPDX license ([GVT resources](https://github.com/usnistgov/gvt-resource-bundle)). | Validate generated messages against a target IG and harvest specific positive/negative conformance cases. |
| **HL7apy** | Python library for parsing, constructing, validating and ER7/MLLP encoding across HL7 2.1–2.8.2; includes a simple MLLP server ([docs](https://crs4.github.io/hl7apy/), [repository](https://github.com/crs4/hl7apy)). | No scenario or patient-state engine. | MIT; v1.3.4 in repo; last source push Feb 2025 ([repository](https://github.com/crs4/hl7apy)). | Best Python building block if Python is preferred; use templates/state tables to create sequences. |
| **`python-hl7`** | Lightweight Python parsing/accessor/MLLP library rather than a standards-aware synthetic generator ([PyPI](https://pypi.org/project/hl7/)). | No. | BSD-style project distribution; check the installed release metadata before redistribution ([PyPI](https://pypi.org/project/hl7/)). | Parsing and transport, not generation. HL7apy is stronger for construction/validation. |
| **Clojure HL7 v2 parser** (`org.clojars.cmiles74/clojure-hl7-parser`) | Pure Clojure parse, map manipulation, emit, ACK creation, and manual message construction ([repository](https://github.com/cmiles74/clojure-hl7-messaging-2-parser), [Clojars](https://clojars.org/clojure-hl7-parser)). | No scenario engine and no profile-aware randomizer. | MIT; version 3.5.1; last source commit 11 Oct 2023 ([repository](https://github.com/cmiles74/clojure-hl7-messaging-2-parser)). | Convenient idiomatic maps for light transformation. Prefer HAPI where validation/version-specific structures matter. |
| **Redox `redox-hl7-v2`** | Node.js schema-driven JSON↔ER7 parser/writer; custom schemas supported ([repository](https://github.com/RedoxEngine/redox-hl7-v2)). | No; “generator” serializes supplied JSON, not clinical events. | Apache-2.0; source pushed Aug 2025 ([repository](https://github.com/RedoxEngine/redox-hl7-v2)). | Useful serializer in JavaScript systems, not relevant to Clojure-first generation. |
| **IRIS-HL7v2Gen** | InterSystems IRIS web app wrapping HL7apy; claims 184 HL7 2.5 message types, validation, structure browsing and TCP sending ([repository](https://github.com/mwaseem75/iris-HL7v2Gen)). | No: selection-driven individual messages, without longitudinal correlation. | MIT; created Dec 2024, last source push Mar 2025 ([repository](https://github.com/mwaseem75/iris-HL7v2Gen)). | Broad single-message exploration; requires IRIS/Docker and adds little for a JVM pipeline. |
| **`hl7v2-rs`** | Rust parser/validator/server with deterministic profile/template-based synthetic generation and corpus replay/redaction features ([repository](https://github.com/EffortlessMetrics/hl7v2-rs)). | The generator creates deterministic message batches; lifecycle is marked beta and no ready hospital-story pathway comparable to SimHospital was verified. | AGPL-3.0-or-later; young project created Aug 2025 and active June 2026 ([repository](https://github.com/EffortlessMetrics/hl7v2-rs)). | Interesting future option; non-JVM, restrictive license, and too new to displace SimHospital/HAPI. |
| **Redox / InterSystems / Rhapsody web samples** | Public documentation pages explain representative ADT/ORU/MDM/etc. messages ([Rhapsody HL7 resources](https://rhapsody.health/resources/hl7-messages/), [Redox library](https://github.com/RedoxEngine/redox-hl7-v2), [InterSystems tools](https://docs.intersystems.com/healthconnectlatest/csp/docbook/DocBook.UI.Page.cls?KEY=EHL72_tools)). | Generally isolated examples, not downloadable longitudinal corpora. | Page/product-specific; no uniform corpus license or blanket PII guarantee. | Documentation only unless a specific sample is separately licensed and reviewed. |
| **Ruby `adt-generator` / Interfaceware random ADT examples** | Older tools generate sample ADT data/messages ([Ruby repository](https://github.com/iwtsolutions/adt-generator), [Interfaceware guide](https://help.interfaceware.com/getting-sample-hl7-data.html)). | No verified longitudinal order/result/document story. | Ruby repo has no declared license and last push Oct 2014. | Obsolete/legally unclear; do not select. |

## 3. Synthea specifically: what exists and what does not

Synthea's current README explicitly lists FHIR R4/STU3/DSTU2, Bulk FHIR, C-CDA, CSV and CPCDS, but not HL7v2 ([Synthea](https://github.com/synthetichealth/synthea)). Source inspection likewise finds no HL7v2 exporter class among its normal exporters. Microsoft FHIR Converter and LinuxForHealth's converter both go **HL7v2 → FHIR**, not the reverse ([Microsoft FHIR Converter](https://github.com/microsoft/FHIR-Converter), [LinuxForHealth converter](https://github.com/LinuxForHealth/hl7v2-fhir-converter)). Therefore they cannot turn Synthea bundles into v2 messages.

The strongest primary evidence is Synthea issue #535. A contributor explained that ordinary output lacked enough scheduling detail for useful ADT and proposed either adding a scheduling module or inventing times/events around encounters; another user then released MayaMaker to implement that idea ([issue and comments](https://github.com/synthetichealth/synthea/issues/535), [MayaMaker](https://github.com/mayankthebest/MayaMaker)). A commercial Smile CDR FHIR-repository transform was also reported in that discussion, but it is not open source and therefore does not satisfy the free-tool requirement ([issue comment](https://github.com/synthetichealth/synthea/issues/535#issuecomment-506433109)).

MayaMaker is real and unusually aligned with the request, but it is only an ADT bridge. It loads Synthea patients/encounters/providers/organizations, chooses a scenario, distributes message timestamps between consecutive encounters, and uses NHapi typed builders ([repository](https://github.com/mayankthebest/MayaMaker)). It has no ORM/ORU mapping, is GPL-3.0, is C# rather than JVM, and its default branch is effectively a 2020 work-in-progress. It is best mined for scheduling/scenario logic, not adopted wholesale.

A new **Synthea→HL7v2 custom exporter** is feasible without converting through FHIR. The official custom exporter template is called once per generated Patient and is packaged as an external JAR; build is `./gradlew jar`, then either put the JAR in Synthea's `lib/custom/` or add it to the standalone classpath ([template instructions](https://github.com/synthetichealth/custom-exporter-template)). Adding HAPI dependencies to the template's Gradle build gives direct access to both Synthea's in-memory chronological record and typed HL7v2 structures. This avoids information loss and awkward reverse mapping from FHIR snapshots.

### Suggested Synthea mapping

A pragmatic first exporter should define explicit, documented inference rules:

| Synthea event | Suggested HL7v2 event | Correlation/timing rule |
|---|---|---|
| Patient first seen | `ADT^A28` or `A04` | Deterministic MRN from Synthea UUID; one per patient. |
| Inpatient/emergency encounter start | `ADT^A01`; ambulatory start `A04` | PV1-19 = deterministic encounter ID; MSH-7/EVN-2 = encounter start. |
| Encounter location change, if modeled or intentionally simulated | `ADT^A02` | Only emit from an explicit transfer extension or documented synthetic rule. |
| Observation/lab panel | `ORM^O01` then `ORU^R01` | Since Synthea usually records the observation/result rather than a separate order, synthesize ORM at a deterministic offset before the observation; reuse ORC/OBR placer/filler IDs. |
| Procedure | `ORM^O01` or procedure-specific order; optional `ORU`/MDM | Map only when a target interface contract is defined. |
| Clinical note/report | `MDM^T02` | Use synthetic free text or aggressively scrub narrative. |
| Encounter end | `ADT^A03` | PV1-45 = encounter stop; same PV1-19. |
| Appointment lifecycle | `SIU^S12/S13/S14/S15/S17/S26` | Requires a new scheduling model; do not infer silently from encounter alone. |

These rules deliberately distinguish facts in Synthea from events invented for interface realism. Store provenance in a Z-segment or sidecar manifest (`source_resource_id`, `inference_rule`, `seed`, exporter version) so downstream tests can separate original narrative from generated workflow.

## 4. Recommended implementation for Clojure/JVM

### Phase A — immediate corpus

Download and checksum the SimHospital artifact:

```bash
curl -L \
  https://raw.githubusercontent.com/google/simhospital/master/docs/artifacts/messages.out \
  -o messages.out
sha256sum messages.out
```

The file and its creation command are documented by SimHospital ([artifact](https://github.com/google/simhospital/blob/master/docs/artifacts/messages.out), [sample documentation](https://github.com/google/simhospital/blob/master/docs/sample.md)). Retain the upstream SHA-256, repository commit, license, and a generated sanitization manifest beside the data.

### Phase B — generate the missing full story

Fork or vendor the archived SimHospital commit and add a pathway resembling:

```yaml
longitudinal_demo:
  persons:
    main_patient:
      first_name: TEST
      surname: PATIENT
      mrn: DEMO-000001
  pathway:
    - admission: {loc: ED}
    - order: {order_id: u-and-e-1, order_profile: UREA AND ELECTROLYTES}
    - delay: {from: 1m, to: 1m}
    - result: {order_id: u-and-e-1, order_profile: UREA AND ELECTROLYTES}
    - transfer: {loc: Renal}
    - clinical_note: {content_type: txt, document_content: "Synthetic discharge note"}
    - discharge: {}
```

Exact field names should be copied from the repository's current pathway examples because order/result options are profile-driven ([built-in pathways](https://github.com/google/simhospital/tree/master/configs/pathways), [pathway guide](https://github.com/google/simhospital/blob/master/docs/write-pathways.md)). Configure zero-width delays or an accelerated `pathways_per_hour` for batch generation, select file output, and pin all demographic dictionaries. SimHospital can also emit order acknowledgement `ORR^O02` and corrected result `ORU^R03/R32`, which are valuable for update/delete semantics ([message mapping](https://github.com/google/simhospital/blob/master/docs/write-pathways.md#messages-types-and-pathway-events)).

Generate at least these scenario families:

- uncomplicated admission → order → result → transfer → discharge;
- outpatient registration → order → preliminary/final/corrected result;
- cancelled admission/transfer/discharge;
- patient update and merge (`A08/A31/A34/A40`);
- multiple orders and multiple results on one visit;
- late result after discharge;
- document initial/correction/replacement if required;
- invalid-message fixtures kept in a separate negative-test partition.

### Phase C — HAPI from Clojure

Use HAPI for parsing, version-aware structures and validation; do not mistake HAPI's source-code generators for synthetic-data generators ([HAPI getting started](https://hapifhir.github.io/hapi-hl7v2/getting_started.html), [source generator](https://github.com/hapifhir/hapi-hl7v2/blob/master/hapi-sourcegen/src/main/java/ca/uhn/hl7v2/sourcegen/XsdMessageGenerator.java)). A minimal Clojure shape is:

```clojure
(ns hl7.ingest
  (:import (ca.uhn.hl7v2 DefaultHapiContext)
           (ca.uhn.hl7v2.util Terser)))

(defonce ctx (DefaultHapiContext.))
(defonce parser (.getPipeParser ctx))

(defn parse-row [raw]
  (let [msg (.parse parser raw)
        t   (Terser. msg)]
    {:raw raw
     :message-type (.get t "/MSH-9-1")
     :trigger      (.get t "/MSH-9-2")
     :message-id   (.get t "/MSH-10")
     :patient-id   (.get t "/PID-3-1")
     :visit-id     (.get t "/PV1-19-1")
     :event-time   (or (.get t "/EVN-2-1") (.get t "/MSH-7-1"))
     :placer-id    (.get t "/ORC-2-1")
     :filler-id    (.get t "/ORC-3-1")}))
```

HAPI's Java API is directly accessible from Clojure, while the pure Clojure parser remains an option for simple hash-map transformations ([HAPI](https://hapifhir.github.io/hapi-hl7v2/), [Clojure parser](https://github.com/cmiles74/clojure-hl7-messaging-2-parser)). Validate each generated message both structurally and against the actual interface conformance profile; NIST tools are useful for profile-specific second opinions ([NIST tools](https://www.nist.gov/itl/health-it-testing-infrastructure/testing-tools/hl7-v2-conformance-testing-tools)).

### Phase D — Arrow/Parquet/Iceberg layout

Keep the immutable ER7 payload and a normalized envelope rather than flattening every segment into one wide table:

- `messages`: `corpus_id`, `sequence_id`, `sequence_no`, `message_id`, `message_type`, `trigger`, `event_ts`, `patient_key`, `visit_key`, `placer_order_key`, `filler_order_key`, `raw_er7`, `sha256`, `generator`, `generator_commit`, `seed`, `license`;
- `segments`: one row per segment with `message_id`, `segment_no`, `segment_name`, and canonical segment text/JSON;
- optional typed tables: `patient`, `visit`, `order`, `observation`, `document`;
- `corpus_manifest`: source URLs, checksums, license, PHI scan rules/results, profile versions and generation configuration.

Partition by corpus and message date/type, not synthetic patient ID. Preserve `sequence_no` because MSH timestamps can tie, historical results can precede the admission that caused the simulator to start, and corrected/cancelled messages require stream order in addition to event time.

## 5. Privacy and release gate

“De-identified” and “synthetic” are not interchangeable. The recommended output should be synthetic and then sanitized again. Before treating a corpus as PII-free:

1. Pin generator, dictionaries, seed/pathway and output checksum.
2. Replace identifiers with a corpus namespace (`TEST-MRN-*`, `TEST-VISIT-*`) while preserving references.
3. Use reserved names, domains (`example.test`), telephone ranges and non-deliverable addresses; avoid realistic national identifiers.
4. Remove or regenerate NTE, OBX textual values, embedded CDA/PDF payloads and Z-segments.
5. Scan all fields—not only PID—for names, email, phone, SSN/NHS-like identifiers, IP addresses and unexpected dates.
6. Verify that every input is synthetic or has a documented de-identification basis; “sample,” “test,” and “public GitHub” are not sufficient.
7. Publish a machine-readable manifest and the sanitizer's test report with the corpus.

The IHE handbook identifies PID, PV1, OBR, NTE, message control IDs and document text as privacy-sensitive locations and demonstrates two-stage pseudonymization/anonymization ([IHE de-identification example](https://build.fhir.org/ig/IHE/ITI.DeIdHandbook/branches/main/hl7-example.html)). Use that as a field checklist, but enforce a stricter synthetic-only policy for the final distributable.

## Bottom line

- **Best free existing corpus:** SimHospital's 1,013-message artifact, Apache-2.0, synthetic and partly longitudinal; directly downloadable, but event coverage is mostly ORU+A01 ([artifact](https://github.com/google/simhospital/blob/master/docs/artifacts/messages.out)).
- **Best free generator:** SimHospital pathways, because it natively models correlated hospital events and supports ADT/ORM/ORR/ORU/MDM chains ([pathway guide](https://github.com/google/simhospital/blob/master/docs/write-pathways.md)). Its archival status requires vendoring and pinning.
- **Best Clojure/JVM approach:** SimHospital for generation plus HAPI via Java interop for parsing/validation; optionally replace SimHospital later with a HAPI-backed Synthea custom exporter ([HAPI](https://hapifhir.github.io/hapi-hl7v2/), [Synthea exporter template](https://github.com/synthetichealth/custom-exporter-template)).
- **Actual Synthea bridge found:** MayaMaker, but it is stale, C#/GPL and ADT-only ([MayaMaker](https://github.com/mayankthebest/MayaMaker)). There is no maintained free turnkey Synthea FHIR→HL7v2 sequence converter in the sources reviewed.
- **Do not use as trusted PII-free corpora without review:** random GitHub sample collections, HL7/Redox/Rhapsody/InterSystems snippets, or NIST/IHE examples whose role is conformance/documentation rather than a blanket sanitized-corpus release.
