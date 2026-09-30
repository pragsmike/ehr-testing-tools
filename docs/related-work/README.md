# Related work — lineage, neighbours, and where this sits

This page is an anchor, not a plan. It names the projects this
workspace learned from, and one recent neighbour, so a reader can place
it: what it inherited, what it deliberately left behind, and which
problems it is *not* trying to solve. Nothing here is a commitment, and
nothing here has a date. The menu of things this workspace does not do
yet is [`future-features.md`](../future-features.md); this page is the
reason some of those entries are shaped the way they are.

The mechanical record — exactly what was read, ported, or vendored from
each upstream, under which license, at which commit — is
[`third-party-sources.md`](../../components/sim/docs/third-party-sources.md).
This page is the argument that record supports.

## Three reference points

Synthetic clinical data is not one problem. Three projects, each good at
its own thing, bracket the space this workspace sits in.

| | Produces | For whom | Truth lives in |
|---|---|---|---|
| **Synthea** (MITRE) | Lifelong longitudinal patient histories, emitted as FHIR, C-CDA and CSV | Population-scale analytics, interoperability demos, anyone needing a plausible *patient* | The generative module state machines |
| **Google Simulated Hospital** (archived) | Live HL7v2 ADT/ORM/ORU traffic from scripted pathways | Interface engines that need a *feed* to point at | The running simulator's mutable state |
| **Synthetic Hospital** (Park, Chen, Dettmers, 2026) | Narrative longitudinal charts served through a FHIR, Epic-style API | Evaluating language models on clinical reasoning over a *chart* | An ontology-grounded knowledge graph behind the notes |
| **This workspace** | HL7v2 hospital-operations traffic plus a ground-truth event log, and traffic broken on purpose | QA of systems that *consume a feed* | The ground-truth event log — a published, versioned contract |

The last column is the one that matters. All three neighbours, and this
workspace, answer the question "what is this data *about*?" somewhere
other than in the rendered bytes. Where they differ is what the bytes
are for.

## Synthea — the clinical layer we borrowed

Synthea is the reason this workspace does not have to argue clinical
content from first principles. Its Generic Module Framework describes a
disease course as plain JSON — states, delays, conditional transitions,
coded conditions and medications — and that representation is open to
inspection by anyone with a text editor. This workspace vendors a
curated subset of those modules verbatim, each hashed and pinned to its
upstream commit, and runs them through its own port of the module
interpreter; the SNOMED, LOINC and RxNorm codes they carry travel
through unchanged. Synthea is also wrapped directly, as a corpus source
of its own, for anyone who needs its FHIR output as-is.

What we did not take is Synthea's horizon. Synthea simulates a life;
this workspace simulates a hospital stay and the operational churn
around it. Lifelong history is Synthea's to serve, and it serves it well.

## Simulated Hospital — the operational shape we borrowed

Google's Simulated Hospital, now archived, got the operational half of
the problem right first: hospital traffic is not a tidy sequence of
admit and discharge, it is transfers, bed swaps, cancellations,
merges, pending admits and transfers entered in error. Its pathway step
vocabulary is the documented ancestor of this workspace's own, and its
discrete-event core — a queue of pending events and a loop that runs
the next one when it comes due — is the shape this workspace's engine
reduces to a pure function over an immutable log.

What we took is design only: no code, and none of its data. Its
configuration is UK-centric (NHS numbers, mmol/L, London ethnicities),
and its answer to "what happened?" is whatever its in-memory state held
when a message went out. This workspace makes that answer a first-class
output — the event log — so a test can check a receiver against it
rather than against a second reading of the same messages.

## Synthetic Hospital — a neighbour arriving from the other side

Synthetic Hospital (arXiv 2609.30027, 2026) is not an ancestor; it
appeared after this workspace's shape was set, and nothing was taken
from it. It is included because it makes the same central bet from the
opposite direction — ground truth fixed first, rendering second, and
the renderer never trusted to define what is true — and because its
physician-blinded realism study is the clearest recent example of how
someone tried to *measure* realism rather than assert it. It gets its
own essay:
[`synthetic-hospital.md`](synthetic-hospital.md).

## What this workspace does not intend

Read against those three, the fence is easy to state.

- **Not a population model.** Synthea already serves lifelong
  longitudinal history at population scale. This workspace's defaults
  are not calibrated to any population's epidemiology; site-tunable
  rates exist so a team can calibrate against *their own* feed.
- **Not a live feed to point an engine at.** Simulated Hospital served
  that need. This workspace produces reproducible corpora first; replay
  over time is a way of delivering a corpus, not a running hospital.
- **Not narrative clinical documentation, and not a reasoning
  benchmark.** Synthetic Hospital renders notes for a language model or
  a physician to read. This workspace renders messages for a system to
  consume, and judges the system, not a reader.
- **No model in the generation path.** Same seed and configuration,
  byte-identical output. A generator whose narrative stage is a
  language model can make its labels deterministic, but not its bytes;
  that trade is reasonable for a reasoning benchmark and wrong for a
  regression oracle.
- **Not a hosted service or an EHR.** Local, test-time tooling; see
  [`what-is-this.md`](../what-is-this.md#scope--what-this-deliberately-does-not-do)
  for the normative scope fence, which nothing on this page moves.
