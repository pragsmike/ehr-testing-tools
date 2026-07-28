# EHR Testing Tools: Problem Statement and Design Questions

> **Status:** Draft for review
> **Date:** 2026-07-22
> **Purpose:** Part 1 states the problem in solution-free terms, so that candidate
> tools and architectures can be evaluated against needs rather than against each
> other. Part 2 lists the questions we must answer to make that evaluation.
> The companion research-agent prompt is in a separate file.

---

## Part 1: Problem Statement

### Context

Teams building and maintaining EHR integrations — transforms and interfaces that
move clinical data between systems in standard wire formats — need to test that
work using a layered method: a small hand-authored corpus of golden cases with
known expected outputs; a large generated corpus of realistic cases checked by
properties rather than oracles; a mutation corpus of deliberately defective
cases that proves the test suite can fail; and structural conformance gates
positioned upstream of all semantic checks. The method is documented (in the
ehr-testing-guide) but is not operational: the guide tells a team *where* a
data generator, a defect injector, and a conformance gate belong in a test
plan, and *what properties* each must have, but supplies none of them. Every
team applying the method today must assemble these capabilities from scratch,
and the assembly work — not the method — is where adoption stalls.

### The problem

**Teams applying the testing method have no ready-made, trustworthy way to
(a) produce realistic clinical test data in the wire formats under test,
(b) derive controlled invalid data from it, and (c) gate messages and
resources for structural and profile conformance — reproducibly, offline,
and inside ordinary JVM-based development and CI workflows.**

Each capability, stated as a need rather than a solution:

#### Need 1: Realistic synthetic clinical data, at volume, in the formats under test

The generated layer of the corpus requires synthetic (never real) patient data
that is longitudinally plausible — values that co-vary the way real clinical
data does across demographics, encounters, conditions, observations,
medications, and time. It must be available in the wire formats the transforms
under test actually consume: initially HL7 v2 messages and FHIR resources,
with other formats (e.g. C-CDA) anticipated. Volume must be practical:
thousands of patients, generated in minutes not days, on a developer machine
or CI runner.

#### Need 2: Determinism and recorded provenance for everything generated

A corpus whose generated layer cannot be regenerated identically is not a
fixed corpus. Generation must be reproducible: the same pinned inputs (seed,
generator version, configuration) must yield the same outputs, and all inputs
capable of varying the output must be capturable in a manifest that travels
with the corpus. A failing case found today must be reproducible next month,
on a different machine, by a different person.

#### Need 3: Controlled defect injection with labelled provenance

Generators produce data that is valid by construction; the most dangerous
inputs to a transform are invalid or edge-lying ones. The mutation layer
therefore requires the ability to take known-good data and derive variants
containing specific, deliberate defects — a missing mandatory field, an
out-of-range date, an invalid code, a violated cardinality — where each
defect is labelled with what it is, where it was injected, and which
constraint or test-plan row it exists to exercise. Mutations must be
format-aware: parsing, altering, and re-serialising a message must not
introduce *unintended* changes alongside the intended one.

#### Need 4: Structural and profile conformance gating

Before semantic properties are asked to reason about a message or resource,
malformed input must be gated out. This requires validation at two levels for
each supported format:

- **Base-standard validation:** is this legal HL7 v2 / legal FHIR at all —
  well-formed, correctly typed, structurally complete per the published
  standard?
- **Profile/interface validation:** does it satisfy the *local* definition of
  valid — the constrained message profile or implementation-guide profile
  that specifies what a particular interface actually requires (usage,
  cardinality, lengths, value sets, conditional and cross-field rules)?

Validation results must be machine-readable, stable enough to diff across CI
runs, and traceable: a failure must identify the violated constraint and its
location precisely enough to act on.

#### Need 5: Management of the conformance artifacts themselves

Profile validation presupposes profiles. The local interface definitions —
however they are authored — must be storable, versionable, and resolvable at
validation time: given a message's type, trigger event, standard version, and
interface, the correct profile version must be selectable deterministically.
Profile artifacts are part of the corpus's reproducibility surface: a
validation verdict is only repeatable if the profile it was rendered against
is pinned.

#### Need 6: Offline, deterministic operation in development and CI

All of the above must run with no network access at execution time. Any
artifacts normally fetched from the network (specification packages,
terminology, profile dependencies) must be acquirable once, pinned, and
cached locally. Runs must be deterministic: identical inputs and pinned
artifacts must produce identical verdicts, run after run, so that a change
in output always signals a change in inputs, code, or pinned artifacts —
never environmental drift.

#### Need 7: Fit with the team's development environment

The capabilities must be usable from Clojure and Java codebases: callable as
JVM libraries where interactive, in-process use matters (REPL-driven
development, test suites), and as command-line processes where isolation
matters (CI gates, batch runs). They must be locally deployable — no
dependence on hosted services at runtime — and free to use, with open-source
licensing strongly preferred and license compatibility across the whole
dependency tree established, not assumed.

### Non-goals

To keep the problem bounded, the following are explicitly out of scope:

- **Semantic correctness checking.** Properties, metamorphic relations, and
  golden-case comparison remain the user's code, written against their own
  transforms. The tools gate structure and supply data; they do not judge
  meaning.
- **Full terminology validation against licensed vocabularies.** Validating
  deep vocabulary bindings (e.g. against complete SNOMED CT or proprietary
  code systems) imports licensing and distribution problems that this
  project does not take on. The boundary — what minimal code/value-set
  checking is feasible without licensed content — is a design question, not
  a commitment.
- **Production message routing or integration-engine functionality.** These
  are test-time tools, not runtime infrastructure.
- **A hosted, public validation service.** Local deployment is the target;
  operating a service for third parties is not.

### Constraints

| Constraint | Requirement |
|---|---|
| Platform | JVM; usable from Clojure and Java |
| Deployment | Locally deployable; no hosted-service dependency at runtime or in CI |
| Cost | Free; open source strongly preferred |
| Licensing | Compatible open licenses across the dependency tree; no copyleft surprises |
| Network | Fully offline at execution time; network permitted only for one-time artifact acquisition |
| Reproducibility | Deterministic outputs from pinned inputs; all variable inputs capturable in a manifest |

### How candidate solutions will be evaluated

A candidate (tool, library, service, or composition of them) is evaluated by:

1. **Coverage:** which of Needs 1–7 it satisfies, wholly or partly, and what
   gaps remain.
2. **Constraint fit:** platform, deployment, cost, licensing, offline
   operation, determinism.
3. **Integration cost:** effort to wrap, embed, or orchestrate it from
   Clojure/Java; ergonomic hazards (global state, heavyweight contexts,
   builder-heavy APIs, shutdown behaviour).
4. **Sustainability:** maintenance status, release cadence, bus factor,
   and the cost to us if it goes dormant.
5. **Verifiability:** whether its claimed capabilities have been confirmed
   hands-on, not just read from documentation.

---

## Part 2: Questions to Answer

The questions below are what we need answered to select tools and components
against the problem statement. They are grouped by need; each carries the
letter-number key used in the research prompt.

### A. Synthetic data generation (Needs 1, 2)

- **A1.** For each candidate generator: which output formats and versions
  does it emit natively? Specifically: which FHIR versions (R4, R4B, R5),
  which HL7 v2 versions, and which v2 message types / trigger events?
- **A2.** Synthea specifically: what exactly does its HL7 v2 export produce
  (versions, message types, segment coverage, quality)? Is the v2 exporter
  maintained, or a neglected corner of the project?
- **A3.** If Synthea's v2 output is insufficient: what exists for generating
  or projecting HL7 v2 messages — FHIR-to-v2 conversion tools, standalone v2
  message generators, or template-based approaches? What would building a
  minimal FHIR→v2 projection ourselves cost?
- **A4.** Can each candidate generator be made fully deterministic —
  identical output bytes from identical seed + version + configuration? What
  is the complete list of inputs that must be pinned (seed, tool version,
  module-set version, config files, locale/timezone, anything else)?
- **A5.** Can each candidate run entirely offline? Does anything in its
  generation path fetch from the network?
- **A6.** Embedding mode: can the generator run as a JVM library in-process,
  or only as a subprocess? What are startup time, memory footprint, and
  throughput (patients/minute) at the volumes we need?
- **A7.** Extensibility: how are custom disease/lifecycle modules or output
  customisations written, and what is the effort curve?
- **A8.** What population characteristics are directly controllable
  (demographics, prevalence, geography), and which of our needs would
  require post-generation filtering or seed search instead?

### B. Defect injection / mutation (Need 3)

- **B1.** Does any existing open-source tool perform controlled,
  labelled mutation of FHIR resources or HL7 v2 messages for test purposes?
  (Survey: healthcare-specific mutators, generic structure-aware fuzzers
  adaptable to the task, mutation-testing frameworks with data-mutation
  modes.)
- **B2.** For each format: which parse/serialise round-trip is faithful
  enough for mutation work — i.e. re-serialising an unmodified parsed
  message yields the original bytes (or a documented canonical form)?
  Where do candidate parsers normalise, reorder, or drop content silently?
- **B3.** What defect taxonomy should the mutation layer support at minimum
  (missing required element, cardinality violation, invalid code, malformed
  date, length violation, broken conditional/co-constraint, encoding-level
  corruption), and which of these can be expressed generically vs. requiring
  per-format operators?
- **B4.** How should mutation provenance be recorded so each defective case
  carries: base-case identity, operator applied, location, violated
  constraint, and the test-plan row it exercises?

### C. FHIR validation (Needs 4, 6)

- **C1.** What is the exact, current procedure for running the official FHIR
  validator fully offline with pinned IG versions? Which flags, which cache
  locations, which one-time acquisition steps? What known bugs affect
  offline/pinned operation (package-cache version skew), and in which
  versions are they fixed?
- **C2.** Library vs. subprocess: what are the tradeoffs of embedding the
  validation engine in-process (JVM version requirements, memory, startup
  cost, global state) versus invoking the CLI jar as a subprocess? Which is
  more stable across validator releases?
- **C3.** How stable is the machine-readable output (OperationOutcome)
  across validator versions — message texts, issue codes, locations? Can
  verdicts be diffed reliably in CI across a validator upgrade?
- **C4.** Profile authoring pipeline: to author local FHIR profiles, what is
  the current minimal toolchain (e.g. FSH → compiler → package), can it run
  locally and offline after initial setup, and is a Node.js authoring-time
  dependency acceptable given the JVM runtime constraint applies to
  execution, not authoring?
- **C5.** What terminology checking does the validator perform with no
  terminology server (`-tx n/a` or equivalent), and what is silently skipped?
  What is the minimal local setup to check code-system membership for freely
  redistributable vocabularies only?
- **C6.** HAPI FHIR's validator module vs. the official validator: do they
  ever disagree, and which should be treated as authoritative for our gates?

### D. HL7 v2 profile validation (Needs 4, 5, 6)

- **D1.** **The handshake question:** does IGAMT's profile export produce
  artifacts that HAPI HL7v2's ProfileParser / conformance validator can
  consume directly? What format(s) does IGAMT export (classic HL7 Message
  Profile XML, IGAMT-native XML/JSON, NIST-engine formats)? If the dialects
  differ, what translation exists or would be required?
- **D2.** What are HAPI HL7v2's conformance-validation capabilities in
  detail: which constraint types does it actually enforce (usage,
  cardinality, length, datatype, value-set/table membership, conditional
  predicates, co-constraints), and which does it silently ignore?
- **D3.** CDC's `lib-hl7v2-nist-validator`: what exactly does it wrap, what
  artifact inputs does it take (IGAMT exports?), what is its API surface,
  license, maintenance status, transitive dependency tree, and can it run
  fully offline? Is it published to Maven Central or must it be built?
- **D4.** The NIST validation engine itself (independent of the CDC
  wrapper): is it available as an embeddable open-source JVM library? Under
  what license? What constraint types does it enforce beyond HAPI's?
- **D5.** IGAMT as software: it is open source (usnistgov/hl7-igamt) — can
  it be self-hosted locally, what does deployment require (databases,
  services), and what is the effort? Or is the hosted NIST instance with
  export-and-pin discipline the pragmatic choice, and what does that imply
  for reproducibility (recording IGAMT version and export date)?
- **D6.** Are there other viable v2 profile authoring routes if IGAMT is
  unsuitable: Message Workbench (still available? maintained?), direct XML
  authoring against the HL7 message-profile schema, or data-driven profile
  definition compiled to checks?
- **D7.** For the constraint types no engine covers (local vocabulary
  bindings, cross-field dependencies, workflow semantics): what is the
  cleanest pattern for a Clojure-native rule layer over the parsed message,
  and where exactly is the seam between profile-enforced and code-enforced
  constraints?
- **D8.** How are HL7 tables and local value sets handled by each candidate
  v2 engine — bundled, user-supplied, or ignored?
- **D9.** What do candidate v2 validators emit as machine-readable results,
  and how stable/diffable is that output across versions? Is there a v2
  equivalent of OperationOutcome, or must we define a normalised result
  format?

### E. Cross-cutting: selection information for every candidate (Need 7, constraints)

For **each** candidate tool or library surfaced by A–D:

- **E1.** License, including the licenses of transitive dependencies;
  compatibility with permissive open-source distribution of our repo.
- **E2.** Maintenance status: last release, release cadence, issue
  responsiveness, steward, bus factor.
- **E3.** Distribution: Maven Central availability, artifact coordinates,
  or build-from-source requirements.
- **E4.** JVM requirements: minimum Java version, module-system issues,
  native dependencies.
- **E5.** Clojure interop ergonomics: static entry points vs. builder
  chains, global/static state, thread pools requiring shutdown, heavyweight
  contexts that must be singletons, REPL friendliness.
- **E6.** Resource profile: startup time, memory, and throughput relevant
  to REPL use and CI.
- **E7.** Determinism: any nondeterministic output (timestamps, UUIDs,
  iteration order) that would break byte-level reproducibility, and whether
  it can be controlled.
- **E8.** Failure modes: what happens offline, on malformed input, on
  version mismatch between artifact and engine.

### F. Composition and prior art (all needs)

- **F1.** Does any existing open-source project already compose these
  capabilities — generation + mutation + gating — into one toolkit we could
  adopt or extend instead of building? (Survey conformance test beds and
  test frameworks for embeddability, language, and license; note explicitly
  where a strong tool fails our constraints, e.g. non-JVM implementation
  or hosted-only operation.)
- **F2.** What would each candidate architecture look like as a pipeline
  (generate → mutate → gate), and where are the format-conversion or
  artifact-translation seams that add fragility?
- **F3.** Forward compatibility: for each candidate composition, what would
  adding C-CDA support later require, and does any choice now foreclose it?
- **F4.** What reproducibility manifest schema covers the whole pipeline —
  generator inputs (A4), mutation provenance (B4), validator and artifact
  versions (C1, D5) — as a single, versionable object?
