# SimHospital and Synthea: Limitations, Practitioner Experience

*Research current to July 26, 2026*

## 1. Research prompt

> Compile a structured, source-cited compendium of real-world problems, limitations, and practitioner opinions regarding Google’s SimHospital and MITRE’s Synthea. Profile each tool’s architecture and intended use; verify its internal state model from source code and architecture documentation; mine GitHub issues, pull requests, discussions, contributors, activity, and notable forks; search interoperability forums, practitioner communities, blogs, academic studies, conferences, social media, and health-IT trade press; and synthesize whether observed limitations actually support a mutable-state-versus-event-sourcing critique. Distinguish explicit practitioner claims from architectural inference, identify counterevidence, and state where public evidence is sparse. Every factual claim must link to a checkable source.

## 2. Executive summary

**Bottom line:** the public evidence supports a narrower claim than the prompt’s premise. Both tools are stateful simulations, and neither is an event-sourced system in which an immutable event log is the authoritative record from which state is rebuilt. Yet neither is simply “mutable current state”: SimHospital has a time-ordered event queue and per-event pathway history, while Synthea retains state-machine history and timestamped clinical-record entries ([SimHospital event source](https://github.com/google/simhospital/blob/master/pkg/state/event.go), [Synthea generic-module architecture](https://github.com/synthetichealth/synthea/wiki/Generic-Module-Framework), [Synthea `Person.java`](https://github.com/synthetichealth/synthea/blob/master/src/main/java/org/mitre/synthea/world/agents/Person.java)).

Commonly reported root causes are:

1. **Synthea:** simplified and isolated disease models, incomplete variation and comorbidity logic, output inconsistencies, surprising time/history semantics, module composition difficulty, US-centric input data, installation friction, and limited fitness for real-world inference ([2019 validation study](https://pmc.ncbi.nlm.nih.gov/articles/PMC6416981/), [2022 simulation-platform evaluation](https://pmc.ncbi.nlm.nih.gov/articles/PMC9360775/), [2026 model-development study](https://pmc.ncbi.nlm.nih.gov/articles/PMC12772637/)).
2. **SimHospital:** build/toolchain breakage, MLLP networking confusion, inflexible HL7 schema/version handling, missing segments/features, limited output formats, thin support, and eventual abandonment—the repository was archived on March 28, 2025 ([repository](https://github.com/google/simhospital), [issue list](https://github.com/google/simhospital/issues)).

There is nevertheless code-level evidence of the kind of complexity the hypothesis anticipates. SimHospital cancellations restore saved “prior” locations, visit deletion pops a previous visit, and a code comment warns that with two consecutive pending encounters the first “will never be finished” because only the latest is checked ([event-processing source](https://github.com/google/simhospital/blob/master/pkg/hospital/event_types.go)). Synthea mutates active `Person`, provider, module, and `HealthRecord` objects, while ending conditions, allergies, medications, care plans, and encounters by changing stop/status fields in place ([`Person.java`](https://github.com/synthetichealth/synthea/blob/master/src/main/java/org/mitre/synthea/world/agents/Person.java), [`HealthRecord.java`](https://github.com/synthetichealth/synthea/blob/master/src/main/java/org/mitre/synthea/world/concepts/HealthRecord.java)). These are legitimate examples of mutable-state machinery, but public evidence does not establish that replacing it with event sourcing would solve the dominant realism and usability complaints.

## 3. Tool profiles and verified state models

| Dimension | SimHospital | Synthea |
|---|---|---|
| Primary purpose | Generates configurable hospital workflow messages in HL7v2 and can send them over MLLP, save them, or print them ([README](https://github.com/google/simhospital)). | Simulates birth-to-death patient histories and exports FHIR, C-CDA, CSV, and other formats ([repository](https://github.com/synthetichealth/synthea)). |
| Modeling unit | A pathway: a sequence of hospital events such as admission, transfer, order, result, discharge, cancellation, and merge ([pathway documentation](https://github.com/google/simhospital/blob/master/docs/write-pathways.md)). | A patient processed through disease and care modules represented as states and transitions at simulation time steps ([generic-module documentation](https://github.com/synthetichealth/synthea/wiki/Generic-Module-Framework)). |
| Event-like elements | A priority queue contains `Event` objects with event/message times, current step, pathway, index, and a `History` array; the source calls an event a “stateful object” for events “currently in progress” ([event source](https://github.com/google/simhospital/blob/master/pkg/state/event.go)). | `Person.history` holds state-machine history; clinical record entries carry start/stop timestamps; modules process states sequentially ([`Person.java`](https://github.com/synthetichealth/synthea/blob/master/src/main/java/org/mitre/synthea/world/agents/Person.java), [module framework](https://github.com/synthetichealth/synthea/wiki/Generic-Module-Framework)). |
| Mutable-state elements | `Hospital` owns mutable patient, bed/location, event-queue, and message-queue structures; `Patient` indexes mutable `PatientInfo`, orders, documents, and past visits ([hospital package](https://pkg.go.dev/github.com/google/simhospital/pkg/hospital), [`Patient` source](https://github.com/google/simhospital/blob/master/pkg/state/patient.go)). | `Person` exposes mutable attributes, current modules, active record(s), chronic medications, current providers, and state history; `HealthRecord` updates active entries and stop times ([`Person.java`](https://github.com/synthetichealth/synthea/blob/master/src/main/java/org/mitre/synthea/world/agents/Person.java), [`HealthRecord.java`](https://github.com/synthetichealth/synthea/blob/master/src/main/java/org/mitre/synthea/world/concepts/HealthRecord.java)). |
| Event sourced? | **No.** Events are queued work items and may be persisted, but the application’s authoritative operational state also resides in mutable patient/location/order structures; no replay-derived aggregate architecture is documented ([event source](https://github.com/google/simhospital/blob/master/pkg/state/event.go), [extension documentation](https://github.com/google/simhospital/blob/master/docs/extend-sh.md)). | **No.** State history and dated entries exist, but current state is not derived solely by replaying an immutable append-only event log ([`Person.java`](https://github.com/synthetichealth/synthea/blob/master/src/main/java/org/mitre/synthea/world/agents/Person.java), [`HealthRecord.java`](https://github.com/synthetichealth/synthea/blob/master/src/main/java/org/mitre/synthea/world/concepts/HealthRecord.java)). |

### Important counterevidence to an overbroad critique

SimHospital explicitly models transfer-in-error, discharge-in-error, cancel admit/visit, cancel transfer, cancel discharge, delete visit, amendments, and result corrections ([pathway documentation](https://github.com/google/simhospital/blob/master/docs/write-pathways.md)). It is therefore incorrect to say the tool is categorically unable to represent corrections or reversals. The better criticism is that these workflows are implemented as named procedural cases with saved prior fields and inverse mutations, which may become cumbersome as scenarios become more concurrent or non-linear ([event-processing source](https://github.com/google/simhospital/blob/master/pkg/hospital/event_types.go)).

Synthea likewise preserves more history than a pure “current-row overwrite” model: it keeps timestamped record entries and module-state history ([`HealthRecord.java`](https://github.com/synthetichealth/synthea/blob/master/src/main/java/org/mitre/synthea/world/concepts/HealthRecord.java), [`Person.java`](https://github.com/synthetichealth/synthea/blob/master/src/main/java/org/mitre/synthea/world/agents/Person.java)). Its central limitation is better described as **forward state-machine simulation with mutable aggregates and simplified domain models**, not absence of temporal data.

## 4. Synthea: catalog of problems and practitioner opinions

### 4.1 Temporal control, reproducibility, and export consistency

| Evidence | Reported problem | Interpretation |
|---|---|---|
| [Issue #1465](https://github.com/synthetichealth/synthea/issues/1465) | In versions 3.1.x–3.2.0, encounters extended beyond `exporter.years_of_history`; the reporter requested day-level history control. | Export-window semantics do not reliably match users’ mental model of generated history. |
| [Issue #1342](https://github.com/synthetichealth/synthea/issues/1342) | A run made without an explicit reference time could not be exactly reproduced because the internal reference is a full timestamp while `-r` accepted only a date. | Current-time dependence leaked into reproducibility. |
| [Issue #682](https://github.com/synthetichealth/synthea/issues/682) | Identical seeds produced differing timestamps, while IDs were inconsistent across runs and between CDA, FHIR, and CSV. | Export and generation layers did not initially share stable identity/time semantics. |
| [PR #756](https://github.com/synthetichealth/synthea/pull/756) | Maintainers centralized randomness, cloned modules/states, and added a reference date, but warned that UUIDs and filtered export history would still differ. | Reproducibility required cross-cutting controls, not merely a seed. |
| [PR #1237](https://github.com/synthetichealth/synthea/pull/1237) | Maintainers fixed nondeterministic set ordering, exporter UUID generation, leaked imaging objects, and OS differences; the PR called extra end sorting “not ideal.” | Mutable/shared objects, concurrency, unordered collections, and exporter behavior all caused divergence. |

The FHIR implementers’ forum records the same control mismatch in practitioner terms. Asked to place an encounter on a specified day, maintainer Jason Walonoski replied: “**No, Synthea doesn't work that way. It is a simulation and generator, not a tool for you to hand-write sample data**,” and “**There is no out of the box parameter to do what you want**”; his workaround was to filter FHIR resources by date after generation ([FHIR implementers chat](https://chat-archive.fhir.org/stream/179166-implementers/topic/Generate.20Synthea.20Patients.html)).

Stable clocks, deterministic random streams, explicit bitemporal semantics, immutable identifiers, and consistent exporter contracts could address many of these failures without a full event-sourced rewrite.

### 4.2 Module composition and extensibility

- A user whose direct-transition module appeared to require an encounter for every patient saw ten condition rows but only a few encounter rows ([issue #475](https://github.com/synthetichealth/synthea/issues/475)). A similar user saw the state execute but the expected endoscopy absent from FHIR while a follow-up visit remained ([issue #458](https://github.com/synthetichealth/synthea/issues/458)).
- Users repeatedly expected a selected module to affect every generated patient, but obtained patients with no expected observations or devices ([issue #941](https://github.com/synthetichealth/synthea/issues/941), [discussion #1040](https://github.com/synthetichealth/synthea/discussions/1040)). In the latter, the maintainer explained that all modules run from birth at every time step, so an unguarded device state may execute on newborns and later disappear from the configured export window ([discussion #1040](https://github.com/synthetichealth/synthea/discussions/1040)).
- Cross-module augmentation is awkward: a practitioner wanting standard vital signs on every emergency encounter asked whether every module’s emergency encounter had to be found and individually edited ([issue #780](https://github.com/synthetichealth/synthea/issues/780)).
- Running only custom modules is not a clean isolation boundary. Maintainer Jason Walonoski warned it “**might not behave the way you expect**” because hard-coded Java lifecycle modules still execute, then jokingly described the advanced alternative as exploring “**strange new worlds alone and in the dark**” ([discussion #1126](https://github.com/synthetichealth/synthea/discussions/1126)).
- Local module replacement can also be surprising: adding a module directory may run the local and bundled versions in parallel, while modifying the bundled module requires rebuilding the JAR ([discussion #1529](https://github.com/synthetichealth/synthea/discussions/1529)).

These complaints indicate a composition model dominated by global time-step execution, shared patient state, hard-coded lifecycle behavior, and export filtering. An event bus or event-sourced aggregate could make cross-cutting reactions and replay more explicit, but discoverability, module packaging, scoping, and documentation are at least as important.

### 4.3 Clinical realism and validity

The best-documented Synthea limitations come from validation studies, not GitHub.

- A 2019 clinical-quality-measure validation concluded that Synthea modeled demographics and average service probabilities reasonably but had limited ability to model heterogeneous post-service outcomes. SyntheticMass had obesity prevalence of 62.6% versus 23.3% in the real Massachusetts comparison, and the implementation covered only two of five eligible colorectal-screening methods ([Chen et al.](https://pmc.ncbi.nlm.nih.gov/articles/PMC6416981/)).
- The original Synthea paper itself cautioned that synthetic records are not suitably nuanced or rich for biomedical, genetic, or pharmaceutical discovery and that “real clinical discovery requires real data” ([Walonoski et al.](https://pmc.ncbi.nlm.nih.gov/articles/PMC7651916/)).
- A 2022 evaluation found no ingestion path for record-level data to estimate model parameters, no user-friendly way to specify complex multivariate models, and a predominance of guideline pathways rather than guideline-discordant, alternative, or hypothetical care paths ([Meeker et al.](https://pmc.ncbi.nlm.nih.gov/articles/PMC9360775/)).
- A 2024/2025 benchmark found Synthea generated nonzero prevalence for only 160 phecodes, tended to underestimate prevalence, and overestimated associations relative to MIMIC data; its privacy resistance was comparatively strong, illustrating a fidelity–privacy trade-off rather than universal inferiority ([Chen et al.](https://arxiv.org/html/2411.04281v1)).
- The 2020 COVID-19 model authors said the model lacked knowledge emerging after May 2020, did not constrain care or supplies by capacity, represented an upper bound on delivered care, and would need extra pathways for unavailable ventilators or other resources ([Walonoski et al.](https://pmc.ncbi.nlm.nih.gov/articles/PMC7531559/)).
- A 2026 Synthea model-development paper described “frequent medical code inaccuracies,” isolated models without comorbidity considerations, continued need for clinical experts, and a time-consuming co-development process that may encode only the treatment variations familiar to participating clinicians ([Kramer et al.](https://pmc.ncbi.nlm.nih.gov/articles/PMC12772637/)).
- An OHDSI evaluation summarized prior work as finding Synthea medication data unreliable without a Medication Diversification Tool and said limited model diversity hindered fidelity ([Wagner and Blacketer](https://www.ohdsi.org/wp-content/uploads/2024/10/41-Wagner-Evaluating_Synthea-Clair-Blacketer.pdf)).

These findings mostly concern **model content, parameterization, dependency structure, and validation**, not storage architecture. Event sourcing would improve provenance and replay but would not by itself create guideline-discordant care, realistic comorbidities, resource constraints, diverse medications, or empirically calibrated correlations.

### 4.4 Data integrity, output semantics, and runtime failures

- An encounter CSV column called `provider` mapped to an organization rather than a clinician, leaving many encounters without an identifiable physician ([issue #547](https://github.com/synthetichealth/synthea/issues/547)).
- A COVID-19 module contained an incomplete builder placeholder that threw an error, while an intubation reason referred to respiratory failure under a contradictory state; the reporter removed one value and replaced another ([issue #707](https://github.com/synthetichealth/synthea/issues/707)).
- A city-scale run produced `NullPointerException`s for 1,152 of 43,944 demographic rows, including missing next-state lookups deep in submodule processing ([issue #1380](https://github.com/synthetichealth/synthea/issues/1380)).
- An Abu Dhabi localization failed to find ambulatory, wellness, or inpatient providers even after the user replaced hospital, primary-care, urgent-care, demographic, ZIP-code, and timezone inputs ([issue #1542](https://github.com/synthetichealth/synthea/issues/1542)).
- The international configuration project acknowledges incomplete regions, mismatched English/local place names, US-format phone numbers, US-derived common names outside Finland, and even reuse of Finnish demographics across European countries ([Synthea International](https://github.com/synthetichealth/synthea-international)).

### 4.5 Usability and practitioner sentiment

Practitioner opinion is mixed rather than uniformly negative.

- Rob Rossmiller praised Synthea for simplifying generation “without compromising on its quality,” called its CLI straightforward, and valued the ability to create privacy-safe demo data; his cancer-only experiment also encountered unrelated non-cancer records, aligning with the module-filter complaint he filed on GitHub ([first-use blog](https://medium.com/@rrossmiller24/my-first-experience-with-synthea-synthetic-health-data-generation-74fffd74a138), [issue #546](https://github.com/synthetichealth/synthea/issues/546)).
- An InterSystems team wrote that “Synthea came to our rescue” for a public readmission demo, but called installing the JDK and Gradle across a team a “nightmare” and containerized the tool to make the environment reproducible ([InterSystems community post](https://community.intersystems.com/post/using-synthea-and-docker-consistent-realistic-synthetic-patient-generation)).
- The SyntheaWeb project describes Synthea’s CLI as a significant technical barrier for clinical researchers and raw FHIR JSON as an “interpretability gap” for end users ([LOINC article](https://loinc.org/articles/syntheaweb-a-web-based-platform-for-the-generation-and-interactive-inspection-of-synthetic-healthcare-data/)).
- In a mortality-prediction discussion, maintainer Dylan Hall said the disease-progression models are “**extremely simplified compared to the complexities of the real world**” and warned that a model trained only on Synthea would not work on real patients ([discussion #1445](https://github.com/synthetichealth/synthea/discussions/1445)).
- MITRE’s own account is strongly positive: its engineers emphasize that Synthea gives developers complex, standards-based test data that would be impractical to handcraft, particularly for API testing ([MITRE impact story](https://www.mitre.org/news-insights/impact-story/patient-data-synthea-missing-piece-health-it)).

### 4.6 Community and maintenance

Synthea remains active. Its repository showed about 2.8k stars, 789 forks, 80 watchers, 18 releases, and 4,994 commits in the July 2026 search snapshot ([repository](https://github.com/synthetichealth/synthea)). A local history analysis found 36 commits in 2026 through July 21, following 94 in 2025 and 76 in 2024; the two largest normalized author identities account for about 48% of commits, and the leading contributors are predominantly MITRE-affiliated ([repository history](https://github.com/synthetichealth/synthea), [contributors page](https://github.com/synthetichealth/synthea/graphs/contributors)).

This is a mature but concentrated project, not a stalled one. Open issues and discussions that remain unanswered for years indicate support bandwidth limits, but ongoing commits and 2026 documentation updates contradict an abandonment narrative ([developer setup, updated March 2026](https://github.com/synthetichealth/synthea/wiki/Developer-Setup-and-Running)).

## 5. SimHospital: catalog of problems and practitioner opinions

### 5.1 Maintenance is the dominant limitation

SimHospital’s repository was archived on March 28, 2025 and is read-only; its last cloned commit was August 9, 2023, and the repository contains 150 commits ([repository](https://github.com/google/simhospital)). The July 2026 snapshot showed about 710 stars, 86 forks, 20 watchers, and seven listed contributors, but those popularity signals no longer translate into maintainability ([repository](https://github.com/google/simhospital)).

Local history analysis found 96% of commits attributable to the top five normalized author identities and 78% to the top two; nearly all leading author emails were at Google. This concentration is consistent with a small internal team publishing a useful tool rather than a broadly governed community project ([contributors page](https://github.com/google/simhospital/graphs/contributors), [repository history](https://github.com/google/simhospital/commits/master/)).

The issue list is unusually stark: the visible public tracker consists of a small set of open questions and defects, many with no assignee, linked development, or visible resolution, and archiving permanently froze them ([issue list](https://github.com/google/simhospital/issues)).

### 5.2 Installation, build, and transport problems

| Evidence | Practitioner problem |
|---|---|
| [Issue #2](https://github.com/google/simhospital/issues/2) | Windows Bazel builds found no targets or failed while fetching Gazelle tools. |
| [Issue #7](https://github.com/google/simhospital/issues/7) | A user reported failed builds on both Windows and Linux due to old Bazel/Python dependency machinery. |
| [Issue #8](https://github.com/google/simhospital/issues/8) | A macOS build failed because Bazel expected a `python` executable and could not fetch `pip_deps`. |
| [Issue #19](https://github.com/google/simhospital/issues/19) | A 2024 user requested documented Go/Bazel versions after compatibility failures with Go 1.22 and Bazel 7.3. |
| [Issue #4](https://github.com/google/simhospital/issues/4) and [issue #12](https://github.com/google/simhospital/issues/12) | Users could not connect the Dockerized simulator to local MLLP listeners; Docker host/network semantics were not made obvious. |
| [Issue #9](https://github.com/google/simhospital/issues/9) | Docker startup produced dozens of poorly formatted information lines. |

These are dependency-age and deployment-documentation failures, not consequences of patient-state architecture.

### 5.3 Protocol, schema, and output limitations

- A user could not find a way to include the HL7v2 IN1 insurance segment ([issue #3](https://github.com/google/simhospital/issues/3)).
- Another asked how schemas were generated and whether the parser could be separated into its own package, implying that a potentially reusable component was tightly embedded ([issue #13](https://github.com/google/simhospital/issues/13)).
- A consumer of HL7v2.6 could see multiple generated schemas in the project but could not determine how to switch versions ([issue #17](https://github.com/google/simhospital/issues/17)).
- A 2023 user requested FHIR output, arguing that Synthea already supported it and FHIR was easier to use ([issue #11](https://github.com/google/simhospital/issues/11)). SimHospital later contained some FHIR resource-generation code, but the issue remained open and the README still defines HL7v2 as the main output ([repository](https://github.com/google/simhospital), [FHIR package](https://pkg.go.dev/github.com/google/simhospital/pkg/fhir)).
- A 2024 user said US phone formatting was unavailable and uneditable and asked how to add GT1 and ZG1 segments ([issue #21](https://github.com/google/simhospital/issues/21)).

### 5.4 Mutable-state workarounds and edge cases

SimHospital’s event vocabulary is rich, but its implementation exposes explicit inverse-operation machinery:

- transfers assign `PriorLocation` and `PriorLocationForCancelTransfer`, then cancellation swaps the patient back and frees the newer location;
- pending-admission and pending-transfer cancellation move `PendingLocation` into `PriorPendingLocation` before clearing it;
- visit deletion pops the latest identifier from a `PastVisits` stack; and
- discharge cancellation resets current account status rather than deriving it from a durable fact stream ([event-processing source](https://github.com/google/simhospital/blob/master/pkg/hospital/event_types.go), [`Patient` source](https://github.com/google/simhospital/blob/master/pkg/state/patient.go)).

The clearest code-level awkwardness is a comment on consecutive pending encounters: the first “**will never be finished, since only the latest Encounter is checked**” ([event-processing source](https://github.com/google/simhospital/blob/master/pkg/hospital/event_types.go)). That is direct evidence of a single-current-object assumption limiting overlapping workflow representation.

Yet this evidence should not be overstated. The tool also supports result amendments and corrections, historical data, readmission flags, transfer tracking, and many ADT cancellation message types ([pathway documentation](https://github.com/google/simhospital/blob/master/docs/write-pathways.md)). Its extension API permits event/message pre-, post-, and override processors, custom validation, item synchronization, generators, and arbitrary patient data ([extension documentation](https://github.com/google/simhospital/blob/master/docs/extend-sh.md)). The limitation is **complexity and fragility of procedural reversals**, not absence of event concepts or all correction support.

### 5.5 Practitioner, forum, academic, and trade evidence

Public practitioner discussion about SimHospital is extremely thin. The located LinkedIn result merely shared the repository and was not accessible for substantive quotation; targeted searches of Stack Overflow, Reddit health-IT communities, FHIR chat, Google Groups, Mastodon/X, conferences, and health-IT trade publications did not produce a checkable first-person review of the tool’s architecture. The meaningful public evidence is therefore the GitHub tracker, code, and usage examples such as Snowflake’s HL7v2 quickstart ([Snowflake quickstart](https://www.snowflake.com/en/developers/guides/processing-hl7-v2-messages-with-snowflake/)).

No peer-reviewed paper located in the academic searches evaluated Google SimHospital itself or documented its limitations. Search results were frequently polluted by unrelated hospital simulation software and games, which further reduces confidence in broad social-search conclusions.

## 6. Cross-cutting comparison

| Theme | Synthea | SimHospital | Event-sourcing relevance |
|---|---|---|---|
| Temporal model | Birth-to-death, time-step state machines with dated record entries ([module framework](https://github.com/synthetichealth/synthea/wiki/Generic-Module-Framework)). | Explicit ordered pathway events and message times ([pathway documentation](https://github.com/google/simhospital/blob/master/docs/write-pathways.md)). | Both are event-aware; neither uses an immutable event log as sole truth. |
| Reported temporal pain | History windows, exact encounter dates, reproducibility, UUID/timestamp divergence ([issue #1465](https://github.com/synthetichealth/synthea/issues/1465), [PR #1237](https://github.com/synthetichealth/synthea/pull/1237)). | Overlapping pending encounters and cancellation bookkeeping appear in code, not user reports ([event source](https://github.com/google/simhospital/blob/master/pkg/hospital/event_types.go)). | Stronger architectural relevance for deterministic replay and corrections, but practitioners do not name event sourcing. |
| Extension pain | Global modules, built-in Java behavior, cross-module observations, surprising export filtering ([discussion #1126](https://github.com/synthetichealth/synthea/discussions/1126), [issue #780](https://github.com/synthetichealth/synthea/issues/780)). | Embedded schemas, custom segments, missing insurance/FHIR options, archived project ([issues](https://github.com/google/simhospital/issues)). | Event subscriptions could help cross-cutting rules, but packaging and governance are larger factors. |
| Realism | Extensively studied: limited heterogeneity, sparse phenotypes, isolated comorbidity models, guideline-centric pathways ([Chen et al.](https://pmc.ncbi.nlm.nih.gov/articles/PMC6416981/), [Kramer et al.](https://pmc.ncbi.nlm.nih.gov/articles/PMC12772637/)). | Little public validation; “realistic” is principally the project’s own claim ([repository](https://github.com/google/simhospital)). | Mostly unrelated to event sourcing; realism requires better models and calibration. |
| Operational reliability | Active project with recurring data/runtime issues and fixes ([repository](https://github.com/synthetichealth/synthea)). | Build breakage plus permanent archival ([repository](https://github.com/google/simhospital)). | Maintenance status dominates SimHospital risk. |
| Community | Large, active, MITRE-led; substantial practitioner and academic footprint ([repository](https://github.com/synthetichealth/synthea)). | Small Google-led contributor base; minimal external discourse ([repository](https://github.com/google/simhospital)). | Governance and contributor capacity are independent of state architecture. |

## 7. Forks and alternatives

### Notable Synthea adaptations

- **NHS England `swpc_synthea`** makes deliberately incompatible changes for English primary-care data, removes US-specific features, and incorporates UK statistics. Its documentation warns that outputs are “realistic but not real,” reports only smoke testing, and says the current release has not undergone statistical validation ([fork](https://github.com/nhsengland/swpc_synthea), [documentation](https://nhsengland.github.io/swpc_synthea/)).
- **Synthea International** supplies location metadata and exposes how difficult localization is: incomplete provider maps, naming mismatches, US phone/name residue, and weak demographic substitutions ([repository](https://github.com/synthetichealth/synthea-international)).
- An **openEHR adaptation discussion** describes Synthea output as “very episodic” and encounter-centered; a practitioner is anglicizing the code, aligning it to UK Core, and exploring mixed FHIR/openEHR output, while noting difficulty grouping all Synthea content into compositions ([openEHR forum](https://discourse.openehr.org/t/synthea-data-in-openehr-format/6906)).
- **SynthEHRella** is not a replacement generator but a benchmark wrapper comparing Synthea with statistical and generative methods on fidelity, utility, and privacy ([repository](https://github.com/chenxran/synthEHRella), [benchmark paper](https://arxiv.org/html/2411.04281v1)).
- **SynTEG** and other learned longitudinal generators target temporal sequence fidelity rather than Synthea’s transparent rule-based pathways, but no source located describes them as event-sourced replacements ([SynTEG paper](https://pmc.ncbi.nlm.nih.gov/articles/PMC7936402/)).

### SimHospital forks

The network showed dozens of forks but no well-documented fork that became an active successor or introduced event sourcing. Search results surfaced downstream use and mirrors rather than a maintained architectural rewrite ([SimHospital repository](https://github.com/google/simhospital), [fork network](https://github.com/google/simhospital/forks)).

### Event-sourced alternative: negative finding

No checkable project was found that explicitly positions itself as an event-sourced successor to either SimHospital or Synthea. Searches found temporal deep-learning generators, FHIR scenario tools, localized forks, output converters, and commercial scenario generators, but not an immutable-domain-event EHR simulator. That gap may represent an opportunity, but it is not evidence that a market already converged on the proposed architecture.

## 8. Evidence gaps and research cautions

1. **GitHub stars and forks reveal attention, not production use.** The report treats them only as rough popularity signals.
2. **SimHospital has a severe evidence imbalance.** Its tracker is tiny, the project is archived, and practitioner/academic discussion is nearly absent; conclusions about user experience therefore have lower confidence than those for Synthea ([repository](https://github.com/google/simhospital)).
3. **Social media was sparse or inaccessible.** LinkedIn, Reddit, and X/Mastodon searches produced few specific posts, and access restrictions prevented reliable quotation of several results. No claims were inferred from inaccessible snippets.
4. **Trade press did not yield substantive reviews.** Targeted searches of Healthcare IT News, Fierce Healthcare, HIMSS, and MobiHealthNews did not locate a hands-on review of either tool. Government, MITRE, LOINC, community blogs, and academic literature supplied the usable evidence instead.
5. **Issue reports are not all confirmed defects.** Some reflect configuration misunderstandings or deployment networking; they are presented as reported experiences, not independently reproduced failures.
6. **Architecture-to-causation remains inferential.** Source code verifies mutable aggregates and event/history elements, but no public comparative experiment shows that an event-sourced rewrite would improve realism, extensibility, or correctness.
7. **Synthea evolves.** Older issues and studies may describe versions that have since changed; newer 2026 work and ongoing commits were included to avoid treating historical limitations as necessarily current ([2026 study](https://pmc.ncbi.nlm.nih.gov/articles/PMC12772637/), [repository](https://github.com/synthetichealth/synthea)).

## 9. Final assessment

The original hypothesis identifies a real architectural tension but overstates both the purity of the tools’ mutable-state designs and the strength of public corroboration. SimHospital is already event-driven at the scheduling/message level, and Synthea already records temporal entries and state history. Their common characteristic is that **current mutable aggregates remain authoritative during simulation**, with procedural logic producing and revising output.

For a new system, the strongest evidence-backed design target is an **immutable, replayable clinical-event ledger with explicit correction/supersession semantics; deterministic simulation time and identifiers; separately materialized current-state views; composable reactions across modules; and empirically validated domain models**. The first four address the reproducibility, overlap, rollback, and audit concerns visible in code and issues; the last is indispensable because the dominant Synthea limitations are about clinical fidelity rather than persistence architecture.
