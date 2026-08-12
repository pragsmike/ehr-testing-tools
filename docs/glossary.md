# Glossary

The authoritative vocabulary for this workspace — sim's and tools'
glossaries merged into one, alphabetized, editorially reconciled where
they used the same word for two different things (R38, [^adr-0010]).
Entries link to the document that treats the term in depth; source
citations point at where the term is enforced in code, not just
mentioned. Some terms also appear in the companion
[`ehr-testing-guide`](https://github.com/pragsmike/ehr-testing-guide);
where the guide gives the fuller treatment, the entry says so. This
glossary deliberately includes implementation-level terms the guide
doesn't need.

## Terms with colliding meanings (read these first)

Four words mean one thing in clinical/HL7 usage and a different, related
thing in this workspace's own vocabulary. Getting these backwards is
the single most common way to misread a page here.

**Pathway.** *Here:* a data structure — an ordered sequence of
simulation steps (admit, delay, order, transfer, discharge…) the
engine executes for one patient; the intermediate representation both
hand-authored scripts and generated trajectories compile to. See
[`sim-theory.md`](../components/sim/docs/sim-theory.md). *In clinical
usage:* a "clinical pathway" or "care pathway" is a standardized care
plan for a condition — related but not the same: our pathways describe
what the simulation will do, not what a clinician should do.

**Resource.** *Here (resource-theory notation):* a typed value flowing
between pipeline stages — "the wires" in the diagrams; see
[`docs/dev/notation.md`](dev/notation.md) and
[`sim-theory.md`](../components/sim/docs/sim-theory.md). *In FHIR:* a
Resource is a standardized data object (Patient, Encounter,
Observation) — sim's emitter renders exactly six: Patient, Encounter,
Condition, Observation, MedicationRequest, Coverage, each only when
the folded state actually holds the fact it would carry. When this
workspace's own docs say "renders resources," they mean FHIR
Resources; when a resource-theory document says "resource," it means a
wire. *In this glossary's own colliding-terms sense above, unrelated
to both:* not used that way anywhere in this workspace — noted only so
a reader who's seen "resource" collide with something else doesn't
expect a third meaning here.

**Profile.** *Here:* a **site profile** — configuration describing one
hospital's local dialect (custom code values, Z-segments, MSH
identity). See [`site-profiles.md`](site-profiles.md). *In FHIR:* a
profile is a formal constraint on a Resource type. Unrelated
mechanisms; same word.

**Diagnosis.** *Here (as sim's own generated content):* a clinical
determination — the thing that gets an ICD-10-CM code and rides in a
DG1 segment; content the simulator *generates*. *Here (as a judge
term):* never used this way at all — a judge's explanation of a
verdict is **Findings**, below, and the two are kept apart
deliberately: in a healthcare tooling workspace, "the gate's
diagnosis" would be parsed clinically by half the audience, and
findings/diagnosis already carries a pleasant non-colliding coincidence
(clinicians also call itemized observed facts "findings"), so keeping
the words apart costs nothing and avoids a real misreading. *In
clinical usage generally:* the same as sim's own sense — a clinical
determination.

## Everything else, alphabetically

**ADR (Architecture Decision Record).** A numbered, append-only entry
in [`notes/ADRs.md`](../notes/ADRs.md) recording a design decision, its
context, and its consequences. An Accepted ADR is never silently
reverted — it is superseded by a new numbered record.

**ADT.** Admit / Discharge / Transfer — the HL7v2 message family
tracking where a patient is and their administrative status; the
workhorse feed of hospital integration. Message types are named by
trigger-event codes: **A01** admit, **A02** transfer, **A03**
discharge, **A04** outpatient registration, **A06/A07** patient-class
change, **A08** demographics update, **A11/A12/A13** cancel
admit/transfer/discharge, **A17** bed swap, **A21/A22** leave of
absence/return, **A34/A40** merges. The guide treats ADT flows in
depth; [`clinical-realities.md`](../components/sim/docs/clinical-realities.md)
covers the ones nobody expects.

**Allocation ladder.** The engine's bed-assignment policy, tried in
order: home-ward licensed bed → home-ward surge slot → other-ward
licensed bed ("outlier placement") → boarding in an ED surge slot.
Seeded-random within each rung. See
[`operational-models.md`](../components/sim/docs/operational-models.md).

**Attending.** The physician responsible for an admitted patient
(PV1-7). Assigned here from a ward-eligible provider pool.

**Baseline.** A pinned, committed report with a provenance header
(date, the commit it was generated against, reason). Deltas against a
baseline are how change is *reviewed*: a new corpus is diffed, findings
are read, and only then is the baseline regenerated — ratification by
regeneration, with the history in the headers. The conformance suite
maintains **two** baselines, not one: a **legacy-floor** baseline (the
plainest default pathway — cheap, long-running, proves the judge still
runs clean over the simplest traffic) and a **full-capability**
baseline (a wider, deliberately-scoped reference corpus exercising the
current breadth of message types). Neither supersedes the other.
Register: the design record[^tools-adr-0013]/[^tools-adr-0015] (tools' pre-merge
sequence, `notes/tools/ADRs.md`).

**Boarding.** *In real hospitals:* holding an admitted patient in the
ED (often a hallway) because no inpatient bed is free. *Here:* the
modeled version — a patient whose administrative status is "admitted
to ward W" while their physical location is an ED surge slot; it
emerges from capacity pressure rather than being scripted. See
[`operational-models.md`](../components/sim/docs/operational-models.md).

**Catalog.** The index `corpus.intake` builds over a corpus: one entry
per item, carrying id, layer, format, a lineage ref where one exists,
and tags. Meaning lives in the data, not in filenames. Register:
`ehrt.corpus.intake` (`CatalogEntry`).

**Catalytic (resource).** In the resource-theory notation: something a
pipeline stage *uses without consuming* — a code table, a dependency,
a configuration file. Each catalytic must resolve to a pinned,
versioned source. See [`docs/dev/notation.md`](dev/notation.md).

**CDA / C-CDA.** Clinical Document Architecture — HL7's XML document
standard for clinical summaries (Consolidated CDA is the US
implementation). A *state-based* format: a snapshot document, not an
event stream. A planned emitter here.

**Census.** How many patients occupy a unit right now. Census pressure
against capacity is what makes boarding and diversion emerge in this
simulator rather than being scripted.

**Ceremony.** The session-end ritual: commit → push. See
`AUTHORS-GUIDE.md` §1 and `notes/ADRs.md`[^adr-0007] for this workspace's
own current form of it.

**Churn.** Operational noise: transfers, bed swaps, cancellations,
error-entries, merges — the administrative messiness real ADT feeds
are full of and hand-crafted test data never contains. Generated by
**InjectChurn**, below.

**CLI (command-line interface).** `bin/ehrt`'s verbs (`corpus`, `gate`,
`check`, `sim`, `version`, `doctor`, …) — the product boundary users
touch. Renamed from `ehr`[^adr-0009]; `ehr` stays reserved for future
payload-EHR tooling.

**Clojure / EDN / malli.** The implementation language (a Lisp on the
JVM); its data notation (EDN — like JSON with richer types; configs,
logs, and reports are EDN); and the schema library used to define and
validate every data shape.

**Coherence property** (also: emitter coherence). The property test
that two independently-built emitters of the same ground truth —
HL7v2 messages and FHIR snapshots — never disagree: replaying a run's
own emitted messages reconstructs the same state its FHIR snapshot
shows, at every message boundary. See
[`event-sourcing.md`](../components/sim/docs/event-sourcing.md).

**Co-landing.** The house rule that every new engine step type lands
*in the same change* as its invariants (and its emitter message type).
Prevents capabilities from outrunning their checks.

**Consumer loop.** The cross-brick integration in `projects/conformance`
that consumes sim's output as a real downstream system would —
validating the manifest contract, gating the messages, intaking the
corpus. Sim's first ecological consumer; it has caught defects sim's
own tests could not see.

**Corpus layer.** The `:layer` field on a catalog entry, naming a
corpus item's provenance kind — currently `:foreign`, the tag intake
gives every entry from a corpus this workspace did not generate.
Register: `ehrt.corpus.intake` (`CatalogEntry`'s `:layer`).

**CPT.** Procedures for billing; **AMA-licensed, deliberately excluded
from this workspace** — see the constraints in
[`what-is-this.md`](what-is-this.md) and `AGENTS.md`'s own constraints
section.

**CVX.** Vaccine codes; CDC-maintained; free to use.

**decide / evolve.** The engine's event-sourcing split[^sim-adr-0008]:
`decide (state,
step) → events` chooses what happens (may consult the world and the
RNG); `evolve (state, event) → state'` is the only function that
changes patient state, by folding events. State can therefore never
disagree with the log. See
[`event-sourcing.md`](../components/sim/docs/event-sourcing.md).

**Determinism / seeded RNG.** Same configuration + same seed ⇒
byte-identical output, always[^sim-adr-0002]. All randomness flows from one seeded
random-number generator consumed in a fixed order — this is the
property that makes any interesting run reproducible from a one-line
manifest, enforced by property tests, not by hope.

**Dialect vs. site config.** The two classes of site-profile knob:
*dialect* changes only how truth is rendered (emit-time,
truth-invariant — MSH identity, code-table overrides, Z-segments);
*site config* changes the truth itself (pre-run — e.g. surge-slot
naming baked into bed IDs). See [`site-profiles.md`](site-profiles.md).

**Discharge disposition.** Where the patient went at discharge (home,
another facility, left against advice, expired) — PV1-36, table 0112.
Post-mortem values and workflows:
[`clinical-realities.md`](../components/sim/docs/clinical-realities.md).

**Discrete-event simulation (DES).** The engine's execution model: a
queue of future events ordered by simulated time; the engine
repeatedly pops the earliest, applies it, and schedules consequences.
Nothing "ticks" — quiet hours cost nothing.

**ED / ED diversion.** Emergency Department; and the state of
redirecting ambulances elsewhere when full. Diversion is a captured
future model; today full exhaustion is a structured error.

**Emitter.** A pure function from the ground-truth log (or state
history) to a wire format. HL7v2 messages and FHIR resources are both
built emitters over the same truth, property-tested against each
other (see **Coherence property**); CDA is a still-planned emitter.
"Formats are just emitters of the patient state machine" is sim's
founding sentence.

**Encounter.** *In clinical/EHR usage:* a discrete interaction between
patient and health system (a visit, a hospitalization). *Here:* the
same concept, but sim's current model is encounter-scoped (a run
simulates a window of hospital operations, not a lifetime). See
[`patient-state-model.md`](../components/sim/docs/patient-state-model.md).

**Error (vs. rejected).** An *error* is a judge failing operationally
— it could not run (a bad invocation, a missing artifact, a crashed
subprocess). A crashed judge yields an error, never a verdict.
Rejected is an answer; error is the absence of the ability to answer.
A corpus full of rejections is information; a corpus full of errors is
a broken harness. Register: `ehrt.cli.help` (`exit-codes`, code `2`).

**Event.** *Here:* an entry in the ground-truth log — an immutable
fact about the simulated world ("patient P admitted to bed B at t").
*In HL7v2:* a "trigger event" is the real-world happening a message
reports (the A01 in ADT^A01). The two align on purpose, but the
ground-truth log also contains events that never become messages
(e.g. `:step-rejected`).

**Event sourcing.** The architecture where an immutable event log is
the authoritative record and all state is derived by replaying it. See
[`event-sourcing.md`](../components/sim/docs/event-sourcing.md) —
including the observation that HL7v2 feeds *are* event streams and
FHIR resources *are* materialized views, so hospital integration has
been doing this for decades without the name.

**Facts register.** [`notes/facts-register.md`](../notes/facts-register.md):
externally verifiable claims (versions, licenses, code verifications,
upstream findings) as numbered F-rows with evidence links and
last-verified dates.

**Findings.** The itemized, located reasons attached to any non-`:pass`
verdict: each names the check that fired, where in the artifact, and
the stated reason. Findings are the actionable content of a verdict —
and, in the consumer loop, the currency in which the gate reports what
a producer should fix. "Findings, not failures": integration tests
assert the gate *runs and verdicts*, never that everything passes.
Register: `ehrt.judge.finding` (`Finding`).

**Fixture.** *In testing generally:* any pre-arranged test data.
*Here, usually:* **the pinned fixture** — a committed ground-truth log
for one exact seed and configuration, which must remain byte-identical
across sessions unless a documented policy decision regenerates it.

**Fold / accumulator.** A *fold* runs through a sequence, feeding each
element into a function along with a running result (the
*accumulator*). Patient state is the accumulator produced by folding
that patient's events through `evolve`.

**Gate.** The workflow that runs judges across a corpus and acts on
their verdicts — the CLI verb `ehrt gate` genuinely is a gate (its
exit-code mapping is policy; `--baseline` is an explicit policy
argument), where the libraries underneath (`ehrt.judge.fhir`,
`ehrt.judge.v2`) are judges. Contrast **Judge**: a judge only decides,
gating is what happens with the decision. Register:
[`docs/dev/notation.md`](dev/notation.md) (observe → judge → act).

**GMF (Generic Module Framework).** Synthea's format for encoding
disease progression as JSON state machines — states (ConditionOnset,
Encounter, Delay, Guard…) connected by probabilistic transitions, with
clinical codes embedded inline. Sim ports a defined subset of its
semantics. See
[`gmf-interpreter.md`](../components/sim-trajectory/docs/gmf-interpreter.md).

**Ground-truth log.** The simulator's primary output and single
source of truth[^sim-adr-0002]: a time-ordered, immutable sequence of events
describing everything that happened in a run. Messages, state
snapshots, and test assertions all derive from it. See
[`event-sourcing.md`](../components/sim/docs/event-sourcing.md).

**History / horizon.** The two phases of running a GMF module for one
patient: the *history* phase fast-forwards from birth to run start
(establishing conditions and attributes; facts marked `:pre-horizon`),
and the *horizon* phase emits real trajectory events inside the
simulated window. See
[`gmf-interpreter.md`](../components/sim-trajectory/docs/gmf-interpreter.md).

**HL7 (organization) / HL7v2.** Health Level Seven International, the
standards body; and its version-2 messaging standard (1980s-vintage,
still carrying most hospital traffic). "HL7" colloquially means v2
messages. See the guide for the standards landscape.

**HTEST.** The HL7 v3-ActReason code
(`http://terminology.hl7.org/CodeSystem/v3-ActReason`, display "test
health data") stamped into `meta.security` on every FHIR resource sim
renders (`sim/F14`) — a standard, queryable marker that lets a real system that
ever received this data find and purge it.

**ICD-10-CM / ICD-10-PCS.** US billing diagnoses / inpatient
procedures; CDC/CMS maintained; free to use.

**InjectChurn.** Sim's own transform weaving coherent *operational*
churn — cancel/reschedule events, bed swaps, merges — into a simulated
pathway. Not this workspace's fault injection: deliberately breaking a
message or bundle to violate a stated conformance constraint lives in
the mutation operators (see **Operator**, below) — the names sound
alike, the concepts don't share a register.

**Intake.** The ingestion route that catalogs a corpus — generated by
this workspace or foreign to it — and, when a manifest sidecar is
present and valid, enriches every catalog entry in that same directory
with the manifest's own provenance. Register:
`ehrt.corpus.intake`.

**Interface engine.** Middleware that routes, transforms, and monitors
HL7 traffic between hospital systems (e.g. Mirth Connect). A primary
intended consumer of this simulator's output.

**Invariant catalog.** The machine-checkable consistency rules in
sim's `check.clj` (no bed holds two patients; results follow orders;
timestamps monotone; …), run over ground-truth logs — standalone via
`ehrt sim check`, in every run's self-check, and across hundreds of
randomized runs in property tests.

**IR (intermediate representation).** The pathway format — the single
shape both hand-authored scenario scripts and module-generated
trajectories compile to, and the only thing the engine executes. Steps
in, events out. See
[`sim-theory.md`](../components/sim/docs/sim-theory.md).

**Judge.** A component that examines one artifact against one tier of
checks — e.g. the base-structural v2 judge (`ehrt.judge.v2`, over
HAPI), or the base-spec FHIR judge (`ehrt.judge.fhir`, over the
official validator). *Judging* is the act; a judge decides, it does
not act on what it decided — that's the Gate's job. Register:
[`docs/dev/notation.md`](dev/notation.md), `notes/ADRs.md`[^adr-0008]
(the judge extraction).

**Lineage.** The provenance record (`<output-dir>/lineage/<filename>.lineage.edn`)
tracing a mutant back to its parent's content hash, the operator and
locator applied, and the contract violated — content-addressed and
append-only, so a directory of lineage records is the real derivation
graph. Register: `ehrt.corpus.lineage` (`LineageRecord`),
[`formats.md`](formats.md).

**LOINC.** Lab tests and observations; Regenstrief Institute; free to
use.

**Manifest.** The provenance record emitted with every generated
corpus: seed, generator version, config hash, environment. Because
generation is deterministic, *(config + seed + version) is the
corpus* — the manifest is enough to regenerate the data exactly.

**Manifest sidecar.** A `manifest.edn` file beside a generated (or
intaken) corpus, validating against `ehrt.provenance.manifest/ManifestV1_1`,
naming the generator, seed, and settings that produced the corpus it
sits beside. Register: `ehrt.corpus.intake`, [`formats.md`](formats.md).

**MLLP.** Minimal Lower Layer Protocol — the thin TCP framing HL7v2
messages travel over. Transport only; below this workspace's level of
description.

**MRN.** Medical Record Number — a facility's patient identifier. In
sim, MRNs are *state* (a patient carries a set with one active)
because merges rebind them; the stable identity underneath is an
internal patient-id[^sim-adr-0010].

**Mutant.** The file an operator produces — a deliberately broken
variant of a base bundle, with a lineage record tracing it back to
where it came from. Register: [`formats.md`](formats.md) ("The lineage
record"), `ehrt.corpus.operators`.

**NPI / NPPES.** National Provider Identifier — the US 10-digit
provider ID (Luhn check-digit over an `80840` prefix) — and the public
registry that issues them. Sim's synthetic providers carry Luhn-valid
NPIs[^sim-adr-0007]; coincidence with real assignments is possible and harmless.

**Observation status.** A billing-critical middle state between
outpatient and inpatient; converting between them mid-stay (A06/A07)
is real, rarely-tested traffic. See
[`clinical-realities.md`](../components/sim/docs/clinical-realities.md).

**Occupancy projection.** The bed board — which patient is in which
bed/slot — computed as a *projection* (see below) from patient states,
never written directly, with a property-tested law that it always
equals the fold over patient locations.

**Operator.** A registered defect transform, applied by
`--operator-id`/`--operator-version` at a `--locator-path`, to every
matching file under `PATH`. Each operator names what it edits (the
change) and its contract (which base-spec constraint the edited file
now violates). Register: [`operators.md`](operators.md).

**OPO / donor management.** Organ Procurement Organization; and the
ICU care of a deceased donor before organ recovery — the reason
"expired" patients legally continue to generate orders, results, and
even a payer change. See
[`clinical-realities.md`](../components/sim/docs/clinical-realities.md).

**Oracle.** A mechanism that says whether an output is correct without
hand-specifying the answer. This workspace has two: the byte-digest
regression oracle, which checks that every one of the 35 fixed corpus
roots still hashes to its recorded value (`bin/regression-oracle`);
and the NIST conformance engine, used in an inject-a-known-defect-and-
expect-it-caught loop (`ehrt gate v2-nist`).

**Pack.** Retired mechanism (the design record[^tools-adr-0006]-era, tools'
pre-merge sequence) — a single text file concatenating tracked files
for a non-git chat surface. Not part of this workspace's own ritual;
see `AUTHORS-GUIDE.md` §2 for why.

**Participants.** Every ground-truth event names the patients it
involves, with roles[^sim-adr-0010]. Most events have one participant; bed-swaps have
two; merges have a survivor and a merged identity.

**Payer.** The insurance entity (Medicare, Medicaid, commercial,
self-pay). Modeled as an *attribute pool*[^sim-adr-0007] — sampled per patient,
age-linked, never a tracked resource. Rendered in IN1.

**Persona.** A patient's generated demographics: name, DOB, sex,
address, phone, synthetic SSN-shaped identifier, and payer — sampled
deterministically from vendored US tables at registration.

**Projection / materialized view.** A derived data structure computed
from an authoritative source, never edited independently — here,
patient state is a projection of the log, and the occupancy board is a
projection of patient states, each with a proven consistency law.

**Property-based testing.** Testing a *law* over hundreds of
randomized inputs rather than one example: "for any seed and patient
count, every run satisfies the invariant catalog." This workspace's
headline guarantees (determinism, log↔state consistency,
message↔truth derivability) are all held this way (via `test.check`).

**Red → green (test-first).** House discipline: a failing test is
written and *observed to fail* before the code that makes it pass.

**Report.** The aggregate a gate run produces over a corpus: the
verdict table plus all findings, one entry per file. Register:
`ehrt.judge.report`, [`formats.md`](formats.md).

**RxNorm.** Medications; NLM; free to use.

**Script space / truth space.** The two state machines every patient
is driven by, and the classic error the architecture makes structural
rather than a matter of discipline: *script space* is a GMF module
walking its own clinical logic — what a disease *should* do, never
which bed or attending — while *truth space* is `decide`/`evolve`
computing what a capacity-bounded hospital actually did. Nothing in
script space can write truth. See
[`trajectory-computation.md`](../components/sim-trajectory/docs/trajectory-computation.md).

**Seam.** A designated clean stopping point in a work session: if
budget runs out, everything before the seam commits green and the rest
becomes its own session.

**Segments and fields.** An HL7v2 message is a stack of *segments*
(3-letter names), each a sequence of *fields*: **MSH** (header),
**EVN** (event type), **PID** (patient identity/demographics),
**PV1** (visit), **MRG** (merge), **IN1** (insurance), **DG1**
(diagnosis), **ORC/OBR** (order control/detail), **OBX**
(observation), **GT1** (guarantor — future), **Z-segments**
(site-invented). Data types worth knowing: **CWE** (coded value with
system), **XPN/XAD/XCN** (name/address/provider-name composites).

**Site profile.** One hospital's local dialect as configuration:
custom code-table values, MSH sending/receiving identity, HL7 version
literal, Z-segment templates. Two profiles over one seed produce the
same ground truth in two hospitals' accents. See
[`site-profiles.md`](site-profiles.md).

**SNOMED CT.** Clinical concepts (conditions, procedures, findings);
SNOMED International; free for US use via the NLM (National Library of
Medicine). House law across this workspace: codes travel as
`{:system :code :display}` triplets from source data to every emitter,
verified against official releases and receipted in the facts
register — never invented.

**State-document.** Sim's name for a snapshot rendering of
`state-history` at one instant — a FHIR Bundle today, a CDA document
once that arm is built. Contrast **Event**: a state-document describes
what's true *now*; an event describes what *happened*.

**Step-rejected.** A ground-truth event recording that the engine
*refused* an attempted step (illegal in the current state; bed since
reclaimed) — truth about the run that never becomes a message[^sim-adr-0012]. Exists
so authored scenarios are debuggable.

**Subprocess coupling.** Retired as of the `ehrt sim` mount[^adr-0005]: tools used to consume sim by running its
CLI as a separate program; it now calls `ehrt.sim.interface` directly,
in-process. Named here as history, not current architecture.

**Surge slot.** A pseudo-bed — hallway stretcher position, chair
code — that sites invent in bed management so un-bedded patients still
have a location the ADT feed can carry. Naming schemes are
site-idiosyncratic, hence configurable.

**Trajectory.** The clinically-meaningful event sequence a GMF module
run produces for one patient — what happened medically and when —
before it's compiled into operational IR steps.

**Trigger event.** The real-world happening an HL7v2 message announces
— the `A01` in `ADT^A01`. See the collision entry for **Event**, above.

**User-defined tables.** HL7v2 tables where the standard explicitly
lets each site invent values — patient class (table 0004), discharge
disposition (0112), location types. The standards-sanctioned escape
hatch that makes every hospital's feed a dialect; the reason site
profiles exist.

**Validators (HAPI, NIST).** Independent HL7 conformance tooling (`sim/F6`) —
HAPI's v2 parser and the official FHIR validator — used to judge
generated messages, so the simulator is never graded by its own
homework.

**Verdict.** A judge's per-artifact classification, four arms:
`:pass` / `:rejected` / `:indeterminate` / `:no-verdict`.
`:indeterminate` is **RESERVED**: kept in the enum only because old
baseline reports still serialize it, but nothing in this workspace
produces it — the case it used to name is now `:no-verdict` instead,
always paired with a `:cause` keyword (Malli-enforced). `worst-of`'s
composition law ranks `:pass` < `:indeterminate` < `:no-verdict` <
`:rejected`: a confirmed violation dominates the aggregate over
incidental partiality elsewhere in the same file. The CLI's exit-code
ladder follows: `0` pass, `1` rejected, `2` operational error, `3` the
aggregate contains `:no-verdict` under the default policy —
`--treat-no-verdict-as pass|rejected` is the explicit opt-in to fold
it into an existing polarity. Register: `ehrt.judge.finding`
(`Verdict`, `Cause`, `VerdictOutcome`, `worst-of`), `ehrt.cli.help`
(`exit-codes`, `--treat-no-verdict-as`).

**Warm-up.** The initial window of a run during which the simulated
hospital is filling from empty; its events are *marked* (never
trimmed) so steady-state corpora can filter the cold-start artifact
without violating log completeness[^sim-adr-0011].

**Witness.** A concrete, checkable piece of evidence for a claim —
usually a named passing test, sometimes a captured run. A claim is
"witnessed" when it was actually run and observed, never merely
asserted.

## Organizations and upstream projects

**Synthea** (MITRE). The open-source synthetic-patient generator whose
GMF modules, US demographics, and embedded codes this workspace mines
as *data* (Apache-2.0, `sim/F2`). Not a runtime dependency.

**Simulated Hospital / SimHospital** (Google Health; archived; `sim/F3`). The
HL7v2 hospital-workflow simulator whose operational design — pathway
step vocabulary, churn family, event queue — sim mined, and whose
mutable-state workarounds motivated the event-sourced alternative. See
[`components/sim/docs/research/`](../components/sim/docs/research/)
for the evidence review, and
[`third-party-sources.md`](../components/sim/docs/third-party-sources.md)
for exactly what came from where.

**clojure-hl7-parser** (cmiles74). The one runtime HL7 dependency: ER7
parse/emit structures. Known limitation (no escape handling) receipted
in the facts register with this workspace's workaround.

**Mirth Connect.** A widely-deployed open-source interface engine; a
representative real-world consumer.

**`ehr-testing-guide`.** The sibling repository, deliberately outside
this workspace[^adr-0001]. Teaches the testing method;
this workspace makes it runnable. See [`what-is-this.md`](what-is-this.md).

[^adr-0001]: Design record [ADR-0001](../notes/ADRs.md).
[^adr-0005]: Design record [ADR-0005](../notes/ADRs.md).
[^adr-0007]: Design record [ADR-0007](../notes/ADRs.md).
[^adr-0008]: Design record [ADR-0008](../notes/ADRs.md).
[^adr-0009]: Design record [ADR-0009](../notes/ADRs.md).
[^adr-0010]: Design record [ADR-0010](../notes/ADRs.md).
[^sim-adr-0002]: Design record [sim/ADR-0002](../notes/sim/ADRs.md).
[^sim-adr-0007]: Design record [sim/ADR-0007](../notes/sim/ADRs.md).
[^sim-adr-0008]: Design record [sim/ADR-0008](../notes/sim/ADRs.md).
[^sim-adr-0010]: Design record [sim/ADR-0010](../notes/sim/ADRs.md).
[^sim-adr-0011]: Design record [sim/ADR-0011](../notes/sim/ADRs.md).
[^sim-adr-0012]: Design record [sim/ADR-0012](../notes/sim/ADRs.md).
[^tools-adr-0006]: Design record [tools/ADR-0006](../notes/tools/ADRs.md).
[^tools-adr-0013]: Design record [tools/ADR-0013](../notes/tools/ADRs.md).
[^tools-adr-0015]: Design record [tools/ADR-0015](../notes/tools/ADRs.md).
