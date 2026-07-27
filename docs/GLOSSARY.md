# Glossary

This glossary exists so that everyone who reads this repository's
documentation can actually understand it: software developers getting
acclimated to the EHR domain, and domain experts — clinicians, medical
informaticists, senior EHR system developers — encountering this
project's software architecture and shorthand. Entries link to the
repository document that treats the term in depth. Some terms also
appear in the companion [ehr-testing-guide](https://github.com/pragsmike/ehr-testing-guide);
where the guide gives the fuller treatment, entries say so. This
glossary deliberately includes implementation-level terms that do not
appear in the guide.

**Start here if a word seems to mean the wrong thing:** the first
section covers terms whose meaning here differs from the meaning you
may bring with you.

---

## Terms with colliding meanings (read these first)

**Pathway.** *Here:* a data structure — an ordered sequence of
simulation steps (admit, delay, order, transfer, discharge…) that the
engine executes for one patient; the intermediate representation both
hand-authored scripts and generated trajectories compile to. See
[sim-theory.md](sim-theory.md). *In clinical usage:* a "clinical
pathway" or "care pathway" is a standardized care plan for a
condition. Related but not the same: our pathways describe what the
simulation will do, not what a clinician should do.

**Resource.** *Here (in sim-theory documents):* a typed value flowing
between pipeline stages in the resource-theory notation — "the wires"
in the diagrams. *In FHIR:* a Resource is a standardized data object
(Patient, Encounter, Observation) — M6's `ehr-testing-sim.emit-state`
renders exactly six: Patient, Encounter, Condition, Observation,
MedicationRequest, Coverage, each only when the folded state actually
holds the fact it would carry. When this project's own docs say
"renders resources," they mean FHIR Resources; when
[sim-theory.md](sim-theory.md) says "resource," it means a wire.

**Profile.** *Here:* a **site profile** — configuration describing one
hospital's local dialect (custom code values, Z-segments, MSH
identity). See [site-profiles.md](site-profiles.md). *In FHIR:* a
profile is a formal constraint on a Resource type. Unrelated
mechanisms; same word.

**Event.** *Here:* an entry in the ground-truth log — an immutable
fact about the simulated world ("patient P admitted to bed B at t").
*In HL7v2:* a "trigger event" is the real-world happening a message
reports (the A01 in ADT^A01). The two align on purpose — our events
are designed to be renderable as HL7 trigger events — but the
ground-truth log also contains events that never become messages
(e.g. `:step-rejected`).

**Boarding.** *In real hospitals:* holding an admitted patient in the
ED (often a hallway) because no inpatient bed is free. *Here:* the
modeled version — a patient whose administrative status is "admitted
to ward W" while their physical location is an ED surge slot; it
emerges from capacity pressure rather than being scripted. See
[operational-models.md](operational-models.md).

**Diagnosis.** *Here and everywhere in this project family:* a clinical
determination — the thing that gets an ICD-10-CM code and rides in a DG1
segment; content the simulator *generates*. **Never** used for a conformance judge's
explanation of a verdict — that is a set of **findings** (see the conformance
vocabulary below). The restriction is deliberate: in a healthcare product, "the
gate's diagnosis" would be parsed clinically by half the audience. (Pleasant
coincidence: "findings" is also what clinicians call itemized observed facts, so
the connotation transfers instead of colliding.)

**Encounter.** *In clinical/EHR usage:* a discrete interaction between
patient and health system (a visit, a hospitalization). *Here:* the
same concept, but note the simulator's current model is
encounter-scoped (a run simulates a window of hospital operations,
not a lifetime) and first-class encounter objects (visit numbers,
concurrent pending encounters) are a captured, partially-landed
design. See [patient-state-model.md](patient-state-model.md).

**Fixture.** *In testing generally:* any pre-arranged test data.
*Here, usually:* **the pinned fixture** — a committed ground-truth
log for one exact seed and configuration, which must remain
byte-identical across sessions unless a documented policy decision
(ADR-0009) regenerates it. It is simultaneously a regression test and
a recorded corpus.

---

## Project shorthand and architecture

**ADR (Architecture Decision Record).** A numbered, append-only entry
in [notes/ADRs.md](../notes/ADRs.md) recording a design decision, its
context, and its consequences. House rule: an Accepted ADR is never
silently reverted — it is superseded by a new numbered record.

**Allocation ladder.** The engine's bed-assignment policy, tried in
order: home-ward licensed bed → home-ward surge slot → other-ward
licensed bed ("outlier placement") → boarding in an ED surge slot.
Seeded-random within each rung. See
[operational-models.md](operational-models.md).

**Catalytic (resource).** In the resource-theory notation: something a
pipeline stage *uses without consuming* — a code table, a dependency,
a configuration file. Each catalytic must resolve to a pinned,
versioned source. See [sim-theory.md](sim-theory.md).

**Ceremony.** The session-end ritual: commit → `git push origin`.
`make pack-push` (ADR-0006) was part of this ritual while the repo's
GitHub remote was private; it went dormant when the remote went public
(ADR-0015) — the pack transport is no longer the chat-read path,
`raw.githubusercontent.com` against the public remote is.

**Churn.** Operational noise: transfers, bed swaps, cancellations,
error-entries, merges — the administrative messiness real ADT feeds
are full of and hand-crafted test data never contains. Generated by
**InjectChurn**, a transform that weaves churn steps into a pathway
without touching its clinical content. See
[clinical-realities.md](clinical-realities.md) for why testers care.

**Co-landing.** The house rule that every new engine step type lands
*in the same change* as its invariants (and, since M3, its emitter
message type). Prevents capabilities from outrunning their checks.

**Consumer loop.** The cross-repo integration in ehr-testing-tools
that consumes sim's output as a real downstream system would —
validating the manifest contract, gating the messages, intaking the
corpus. Sim's first ecological consumer; it has caught defects sim's
own tests could not see.

**decide / evolve.** The engine's event-sourcing split (ADR-0008):
`decide (state, step) → events` chooses what happens (may consult
the world and the RNG); `evolve (state, event) → state'` is the only
function that changes patient state, by folding events. State can
therefore never disagree with the log. See
[event-sourcing.md](event-sourcing.md).

**Dialect vs. site config.** The two classes of site-profile knob:
*dialect* changes only how truth is rendered (emit-time,
truth-invariant — MSH identity, code-table overrides, Z-segments);
*site config* changes the truth itself (pre-run — e.g. surge-slot
naming baked into bed IDs). See [site-profiles.md](site-profiles.md).

**Emitter.** A pure function from the ground-truth log (or state
history) to a wire format. HL7v2 messages (`EmitHL7`) and FHIR
resources (`EmitState`, M6) are both built emitters over the same
truth, property-tested against each other (see **coherence property**,
below); CDA is a still-planned emitter, deferred with its own contract
note rather than stubbed. "Formats are just emitters of the patient
state machine" is this project's founding sentence.

**Facts register.** [notes/facts-register.md](../notes/facts-register.md):
externally verifiable claims (versions, licenses, code
verifications, upstream findings) as numbered F-rows with evidence
links and last-verified dates. If a doc asserts a checkable fact, the
register is where the receipt lives.

**Fold / accumulator.** Functional-programming terms domain experts
will meet constantly here. A *fold* runs through a sequence, feeding
each element into a function along with a running result (the
*accumulator*). Patient state is the accumulator produced by folding
that patient's events through `evolve`. If you know spreadsheet
running totals, you know folds.

**Ground-truth log.** The simulator's primary output and single
source of truth: a time-ordered, immutable sequence of events
describing everything that happened in a run. Messages, state
snapshots, and test assertions all derive from it. See
[event-sourcing.md](event-sourcing.md). `sim run --format ground-truth`
renders exactly this log, bare EDN, to stdout — the shape `sim check`
reads on stdin, so `sim run --format ground-truth | sim check` is a
real, working self-check pipe.

**GMF (Generic Module Framework).** Synthea's format for encoding
disease progression as JSON state machines — states (ConditionOnset,
Encounter, Delay, Guard…) connected by probabilistic transitions,
with clinical codes embedded inline. This project ports a defined
subset of its semantics. See [gmf-interpreter.md](gmf-interpreter.md).

**History / horizon.** The two phases of running a GMF module for one
patient: the *history* phase fast-forwards from birth to run start
(establishing conditions and attributes; facts marked
`:pre-horizon`), and the *horizon* phase emits real trajectory events
inside the simulated window. See
[gmf-interpreter.md](gmf-interpreter.md).

**InjectChurn.** See **Churn**.

**Invariant catalog.** The machine-checkable consistency rules in
`check.clj` (no bed holds two patients; results follow orders;
timestamps monotone; …), run over ground-truth logs — standalone via
`sim check`, in every run's self-check, and across hundreds of
randomized runs in property tests.

**IR (intermediate representation).** The pathway format —
the single shape both hand-authored scenario scripts and
module-generated trajectories compile to, and the only thing the
engine executes. Steps in, events out. See [sim-theory.md](sim-theory.md).

**Manifest.** The provenance record emitted with every run: seed,
generator version, config hash, environment. Shaped to
ehr-testing-tools' corpus-manifest schema so a sim run can be
ingested as a pinned corpus. Because the simulator is deterministic,
*(config + seed + version) is the corpus* — the manifest is enough to
regenerate the data exactly.

**now / next / want.** The convention for reading
[sim-theory.edn](sim-theory.edn): *want* is the whole envisioned
design; *now* is the subset marked built; *next* is the single stage
marked `;; NEXT`. One file, three system descriptions, no drift.

**Occupancy projection.** The bed board — which patient is in which
bed/slot — computed as a *projection* (see below) from patient
states, never written directly, with a property-tested law that it
always equals the fold over patient locations.

**Pack.** A single text file concatenating the repository's tracked
files with a provenance header (commit, timestamp, tree status),
published to a transport repository so chat-based design sessions can
read the codebase. `make pack-push` publishes it; see
AUTHORS-GUIDE.md.

**Participants.** Every ground-truth event names the patients it
involves, with roles. Most events have one participant; bed-swaps
have two; merges have a survivor and a merged identity (ADR-0010).

**Persona.** A patient's generated demographics: name, DOB, sex,
address, phone, synthetic SSN-shaped identifier, and payer — sampled
deterministically from vendored US tables at registration.

**Projection / materialized view.** A derived data structure computed
from an authoritative source, never edited independently — here,
patient state is a projection of the log, and the occupancy board is
a projection of patient states, each with a proven consistency law.
Software-architecture cousins: database materialized views, CQRS read
models.

**Property-based testing.** Testing a *law* over hundreds of
randomized inputs rather than one example: "for any seed and patient
count, every run satisfies the invariant catalog." This repo's
headline guarantees (determinism, log↔state consistency,
message↔truth derivability) are all held this way (via
`test.check`). Property tests here have repeatedly caught real bugs
example tests missed.

**Script space / truth space.** The two state machines every patient
here is driven by, and the classic error the architecture makes
structural rather than a matter of discipline: *script space* is a
GMF module walking its own clinical logic — what a disease *should*
do, never which bed or attending — while *truth space* is
`decide`/`evolve` computing what a capacity-bounded hospital actually
did. Nothing in script space can write truth. See
[trajectory-computation.md](trajectory-computation.md).

**Seam.** A designated clean stopping point in a work session: if
budget runs out, everything before the seam commits green and the
rest becomes its own session. Nothing lands half-done.

**Site profile.** One hospital's local dialect as configuration:
custom code-table values, MSH sending/receiving identity, HL7
version literal, Z-segment templates. Two profiles over one seed
produce the same ground truth in two hospitals' accents. See
[site-profiles.md](site-profiles.md) — this is the "simulate *my*
hospital" answer.

**Step-rejected.** A ground-truth event recording that the engine
*refused* an attempted step (illegal in the current state; bed since
reclaimed) — truth about the run that never becomes a message
(ADR-0012). Exists so authored scenarios are debuggable.

**Trajectory.** The clinically-meaningful event sequence a GMF module
run produces for one patient — what happened medically and when —
before CompileTrajectory turns it into operational IR steps.

**Warm-up.** The initial window of a run during which the simulated
hospital is filling from empty; its events are *marked* (never
trimmed) so steady-state corpora can filter the cold-start artifact
without violating log completeness.

---
## Conformance & gating vocabulary (family terms)

These terms belong to the sibling
[ehr-testing-tools](https://github.com/pragsmike/ehr-testing-tools)
repository, which judges this simulator's output; sim's docs use
them when discussing the consumer loop and the validation program.
Tools' own code and ADRs are authoritative if any detail here
drifts; this block exists so sim's readers need not switch repos
mid-sentence. (The planned guide crosswalk reconciles against this
definition set.)

**Judge.** A component that examines one artifact against one tier
of checks — e.g. the base-structural HL7v2 judge, or the FHIR
judge. *Judging* is the act; a judge examines, it does not fix.

**Verdict.** A judge's per-artifact classification. The enumeration
is **pass / rejected / indeterminate / no-verdict** — deliberately
not "pass/fail": *rejected* means the check ran and the answer is
no (the same doctrine as this repo's `:rejected` Result arm);
*indeterminate* means the judge examined the artifact but cannot
classify it (e.g. a check needing a terminology tier this judge
lacks); *no-verdict* means no determination was produced. Exit
codes follow the ladder: 0 pass, 1 rejected, 2 error, 3 no-verdict.

**Error (vs. rejected).** An *error* is the judge itself failing
operationally — it could not run. A crashed judge yields an error,
never a verdict. Rejected is an answer; error is the absence of the
ability to answer. Keeping these apart is load-bearing: a corpus
full of rejections is information, a corpus full of errors is a
broken harness.

**Findings.** The itemized, located reasons attached to any
non-pass verdict: each names the check that fired, where in the
artifact, and the stated reason. Findings are the actionable
content of a verdict — and, in the cross-repo consumer loop, the
currency in which the gate tells this simulator what to fix
("findings, not failures" is that loop's assertion discipline:
integration tests assert the gate *runs and verdicts*, never that
everything passes).

**Report.** The aggregate a gate run produces over a corpus: the
verdict table plus all findings.

**Baseline.** A pinned, committed report with a provenance header
(date, the sim commit it was generated against, reason). Deltas
against a baseline are how change is *reviewed*: a new corpus is
diffed, findings are read, and only then is the baseline
regenerated — ratification by regeneration, with the history in
the headers.

**Gate.** The workflow that runs judges across a corpus and
produces the report; "gating" a corpus means putting it through.
In this simulator's validation program, the gate is the independent
examiner — the reason the output is never graded on its own
homework.

---

## Software concepts (for domain experts)

**CLI (command-line interface).** The `sim` program's verbs (`run`,
`check`, `identifiers`, `version`, …) — the product boundary
consumers touch, and by house rule the surface all demos run through.

**`sim identifiers`.** The CLI verb answering "what would I have to
find and remove?" — config + seed to the complete EDN inventory of
every identifier a run would ever contain (patient-ids, MRNs, message
control ids, FHIR resource ids, provider NPIs, the run's own id),
without generating the corpus itself (ADR-0014).

**Clojure / EDN / malli.** The implementation language (a Lisp on the
JVM); its data notation (EDN — like JSON with richer types; our
configs, logs, and theory files are EDN); and the schema library used
to define and validate every data shape (configs, events, manifests).

**Determinism / seeded RNG.** Same configuration + same seed ⇒
byte-identical output, always. All randomness flows from one seeded
random-number generator consumed in a fixed order. This is the
property that makes any interesting run reproducible from a one-line
manifest — and it is enforced by property tests, not by hope.
Guarantee scope: within a generator version (ADR-0009).

**Discrete-event simulation (DES).** The engine's execution model: a
queue of future events ordered by simulated time; the engine
repeatedly pops the earliest, applies it, and schedules consequences.
Nothing "ticks" — quiet hours cost nothing.

**Event sourcing.** The architecture where an immutable event log is
the authoritative record and all state is derived by replaying it.
[event-sourcing.md](event-sourcing.md) explains ours — including the
observation that HL7v2 feeds *are* event streams and FHIR resources
*are* materialized views, so hospital integration has been doing this
for decades without the name.

**State-document.** This project's name (`sim-theory.edn`'s own
`EmitState` stage) for a snapshot rendering of `state-history` at one
instant — a FHIR Bundle today, a CDA document once that arm is built.
Contrast **event**: a state-document describes what's true *now*
(or at a queried *t*); an event describes what *happened*.

**Coherence property** (also: emitter coherence). The property test
(M6) that two independently-built emitters of the same ground truth —
`EmitHL7`'s messages and `EmitState`'s FHIR snapshots — never
disagree: replaying a run's own emitted messages reconstructs the same
state its FHIR snapshot shows, at every message boundary. See
[event-sourcing.md](event-sourcing.md#the-coherence-property-tested)
for the mechanism (`ehr-testing-sim.v2-replay`) and
[sim-theory.md](sim-theory.md)'s own global-laws section for the law's
formal statement.

**Red → green (test-first).** House discipline (ADR-0006 in tools,
ADR-0004 here): a failing test is written and *observed to fail*
before the code that makes it pass. Session reports carry the
evidence.

**Subprocess coupling.** How ehr-testing-tools consumes sim: by
running its CLI as a separate program and reading the output, rather
than linking its code — the same relationship a real consumer has.

---

## Health-IT standards, protocols, and message anatomy (for developers)

**ADT.** Admit / Discharge / Transfer — the HL7v2 message family
tracking where a patient is and their administrative status; the
workhorse feed of hospital integration. Message types are named by
trigger-event codes: **A01** admit, **A02** transfer, **A03**
discharge, **A04** outpatient registration, **A06/A07** patient-class
change, **A08** demographics update, **A11/A12/A13** cancel
admit/transfer/discharge, **A17** bed swap, **A21/A22** leave of
absence/return, **A34/A40** merges. The guide treats ADT flows in
depth; [clinical-realities.md](clinical-realities.md) covers the ones
nobody expects.

**CDA / C-CDA.** Clinical Document Architecture — HL7's XML document
standard for clinical summaries (Consolidated CDA is the US
implementation). A *state-based* format: a snapshot document, not an
event stream. A planned emitter here.

**ER7.** The classic pipe-and-caret text encoding of HL7v2 messages
(`MSH|^~\&|...`) — segments on lines, fields split by `|`, components
by `^`. Special characters in data must be *escaped*; see the facts
register for what our parser dependency does and doesn't handle.
`sim run --format er7` (go-public session) renders exactly this —
bare wire bytes to stdout, nothing else — and requires `--emit hl7`.

**FHIR.** Fast Healthcare Interoperability Resources — HL7's modern
REST/JSON standard. State-based: Resources describe current state.
Emitter landed at M6 (`ehr-testing-sim.emit-state`, R4 JSON) — a Bundle
of Patient/Encounter/Condition/Observation/MedicationRequest/Coverage
per patient snapshot; CDA is the format dispatch's other, still-deferred
arm. Every resource carries the standard HTEST security label and a
run-tag (`meta.tag`) identifying its generator/seed. This project's own
in-repo FHIR evidence stops at the serverless set — the emitter-
coherence property, cross-emitter ids, shape validation
(ADR-0014); checking output against a real FHIR server is the
consumer's job, the same division of labor v2 conformance already
follows.

**HTEST.** The HL7 v3-ActReason code (`http://terminology.hl7.org/CodeSystem/v3-ActReason`,
display "test health data") this project stamps into `meta.security`
on every FHIR resource it renders — a standard, queryable marker that
lets a real system that ever received this data find and purge it
(facts-register F14).

**HL7 (organization) / HL7v2.** Health Level Seven International, the
standards body; and its version-2 messaging standard (1980s-vintage,
still carrying most hospital traffic). "HL7" colloquially means v2
messages. See the guide for the standards landscape.

**Interface engine.** Middleware that routes, transforms, and
monitors HL7 traffic between hospital systems (e.g. Mirth Connect).
A primary intended consumer of this simulator's output.

**MLLP.** Minimal Lower Layer Protocol — the thin TCP framing HL7v2
messages travel over. Transport only; below this project's level of
description.

**MRN.** Medical Record Number — a facility's patient identifier. In
this simulator MRNs are *state* (a patient carries a set with one
active) because merges rebind them; the stable identity underneath is
an internal patient-id (ADR-0010).

**NPI / NPPES.** National Provider Identifier — the US 10-digit
provider ID (Luhn check-digit over an `80840` prefix) — and the
public registry that issues them. Our synthetic providers carry
Luhn-valid NPIs; coincidence with real assignments is possible and
harmless (the registry is public data).

**Segments and fields.** An HL7v2 message is a stack of *segments*
(3-letter names), each a sequence of *fields*. The ones this repo's
docs mention: **MSH** (message header: sender, receiver, timestamp,
type, version in MSH-12), **EVN** (event type), **PID** (patient
identity/demographics; PID-29/30 death datetime/indicator), **PV1**
(visit: class in PV1-2, location in PV1-3, prior location PV1-6,
attending PV1-7, visit number PV1-19, discharge disposition PV1-36),
**MRG** (merge: the prior MRN), **IN1** (insurance), **DG1**
(diagnosis, ICD-coded), **ORC/OBR** (order control/detail), **OBX**
(observation: LOINC code in OBX-3, value OBX-5, units OBX-6,
reference range OBX-7, abnormal flag OBX-8), **GT1** (guarantor —
future here), **Z-segments** (site-invented segments, ZPI etc. — see
site profiles). Data types worth knowing: **CWE** (coded value with
system), **XPN/XAD/XCN** (name/address/provider-name composites).

**Trigger event.** The real-world happening an HL7v2 message
announces — the `A01` in `ADT^A01`. See the collision entry for
**Event**.

**User-defined tables.** HL7v2 tables where the standard explicitly
lets each site invent values — patient class (table 0004), discharge
disposition (0112), location types. The standards-sanctioned escape
hatch that makes every hospital's feed a dialect; the reason site
profiles exist. The guide's opening chapters treat this at length.

**Validators (HAPI, NIST).** Independent HL7 conformance tooling —
HAPI's v2 parser and NIST's validation suite — used (via
ehr-testing-tools) to judge generated messages, so the simulator is
never graded by its own homework.

See the conformance & gating vocabulary above for how judging is organized
(judges, verdicts, findings, baselines).

---

## Clinical and operational terms (for developers)

**Attending.** The physician responsible for an admitted patient
(PV1-7). Assigned here from a ward-eligible provider pool.

**Census.** How many patients occupy a unit right now. Census
pressure against capacity is what makes boarding and diversion
emerge in this simulator rather than being scripted.

**Discharge disposition.** Where the patient went at discharge
(home, another facility, left against advice, expired) — PV1-36,
table 0112. Post-mortem values and workflows:
[clinical-realities.md](clinical-realities.md).

**ED / ED diversion.** Emergency Department; and the state of
redirecting ambulances elsewhere when full. Diversion is a captured
future model; today full exhaustion is a structured error.

**Observation status.** A billing-critical middle state between
outpatient and inpatient; converting between them mid-stay (A06/A07)
is real, rarely-tested traffic. See
[clinical-realities.md](clinical-realities.md).

**OPO / donor management.** Organ Procurement Organization; and the
ICU care of a deceased donor before organ recovery — the reason
"expired" patients legally continue to generate orders, results, and
even a payer change. The canonical example of post-mortem traffic:
[clinical-realities.md](clinical-realities.md).

**Payer.** The insurance entity (Medicare, Medicaid, commercial,
self-pay). Modeled as an *attribute pool* — sampled per patient,
age-linked, never a tracked resource. Rendered in IN1.

**Surge slot.** A pseudo-bed — hallway stretcher position, chair
code — that sites invent in bed management so un-bedded patients
still have a location the ADT feed can carry. Naming schemes are
site-idiosyncratic, hence configurable here.

---

## Terminology (code) systems and their stewards

**SNOMED CT** — clinical concepts (conditions, procedures, findings);
SNOMED International; free for US use via the **NLM** (National
Library of Medicine, the US member/distributor, also home of UMLS and
the SNOMED→ICD-10-CM map this project pins for DG1 rendering).
**LOINC** — lab tests and observations; Regenstrief Institute; free.
**RxNorm** — medications; NLM; free.
**ICD-10-CM / ICD-10-PCS** — US billing diagnoses / inpatient
procedures; CDC/CMS maintained.
**CVX** — vaccines; CDC.
**CPT** — procedures for billing; **AMA-licensed, deliberately
excluded from this project** (see the constraints in
[problem-statement.md](problem-statement.md)).
House law: codes travel as `{:system :code :display}` triplets from
source data to every emitter, verified against official releases and
receipted in the facts register — never invented.

---

## Organizations and upstream projects

**Synthea** (MITRE). The open-source synthetic-patient generator whose
GMF modules, US demographics, and embedded codes this project mines
as *data* (Apache-2.0), and whose peer-reviewed pedigree backs the
clinical-plausibility claims. Not a runtime dependency.
**Simulated Hospital / SimHospital** (Google Health; archived). The
HL7v2 hospital-workflow simulator whose operational design — pathway
step vocabulary, churn family, event queue — this project mined, and
whose mutable-state workarounds motivated the event-sourced
alternative. See [docs/research/](research/) for the evidence
review, and [third-party-sources.md](third-party-sources.md) for
exactly what came from where.
**clojure-hl7-parser** (cmiles74). The one runtime HL7 dependency:
ER7 parse/emit structures. Known limitation (no escape handling)
receipted in the facts register with this repo's workaround.
**Mirth Connect.** A widely-deployed open-source interface engine; a
representative real-world consumer.
**ehr-testing-guide / ehr-testing-tools.** The sibling repositories:
the guide teaches the testing method; tools makes it runnable
(corpus construction, conformance gating) and is this simulator's
first real consumer. Terms shared with the guide defer to the
guide's fuller treatment; a crosswalk reconciliation is planned.
