# Synthetic Hospital — what a physician-validated benchmark can and cannot tell a traffic simulator

*An essay on related work. It records what one recent paper did, what
this workspace might learn from it, and where the analogy stops. It is
not a plan and makes no commitment; where it sketches something this
workspace could build, that sketch is an idea on
[`future-features.md`](../future-features.md)'s menu, nothing more.*

**The paper.** Christine Park, Valerie Chen, Tim Dettmers (Carnegie
Mellon University), *Synthetic Hospital: An Open, Verifiable,
Physician-Validated Longitudinal EHR Benchmark*, arXiv
[2609.30027](https://arxiv.org/abs/2609.30027) (v1, 24 September 2026).
Code and data: [sparkcpark/synthetic_hospital](https://github.com/sparkcpark/synthetic_hospital)
(MIT). Figures quoted below are the paper's own, not measurements made
here.

## What it built

Synthetic Hospital is a benchmark for language models doing chart work:
reconstructing a longitudinal problem list, summarizing a record,
retrieving evidence for a diagnosis, and inferring the clinical
question behind a vague imaging order.

Its pipeline runs in a fixed order:

1. **Ingest** USMLE-style board questions and flashcard decks — public
   medical-education material, no patient data.
2. **Extract and ground.** A language model proposes diagnoses and
   typed findings from each question; those are then grounded
   *deterministically* to ICD-10-CM, SNOMED CT and LOINC, and the
   model's own codes are treated only as candidates. The result is a
   knowledge graph in which every node keeps its source and its
   grounding method.
3. **Cluster** questions into patients before any narrative exists:
   demographically compatible questions that share a diagnosis become
   one longitudinal patient. 5,602 questions become 1,268 patients.
4. **Render.** A language model writes each patient's profile, orders
   the encounters on a timeline, and adds cross-encounter continuity to
   the history of present illness. Problem and medication lists
   propagate deterministically.
5. **Label** from the graph. Three of the four tasks' reference answers
   are deterministic functions of it; the fourth (imaging indication)
   is model-written but anchored to graph-fixed inputs.
6. **Serve** the records through a simulated EHR — FHIR R4 resources,
   OAuth2 and role-based access, a 13-tool agent API — with a scoring
   endpoint that returns a reward without ever returning a label.

The sentence that matters most is the paper's own: *"the record shown
to a model still reads like a chart; it is the ground truth behind the
record, not the record itself, that is complete."*

## The bet it shares with this workspace

This workspace makes the same bet. The ground-truth event log is fixed
first; HL7v2 is one rendering of it among several; a change that alters
only the rendering is a different kind of change from one that alters
what happened, and is declared as such. Synthetic Hospital's narrative
stage and this workspace's emitters play the same role: they are
allowed to be wrong about *style*, and never allowed to decide *truth*.

The two projects arrive at the bet from opposite ends. Synthetic
Hospital starts from content — diagnoses, findings, their relations —
and has almost no operational layer: time is an ordering of encounters
("eight months later"), not a clock; there are no admissions,
transfers, beds, cancellations, merges or messages. This workspace
starts from operations and borrows its clinical layer from Synthea.
Neither is a substitute for the other.

## What the physicians were actually shown

The headline — physicians told synthetic from real at 53%, near
chance — is only as strong as the protocol behind it, so it is worth
setting out in full.

- **The material.** Ten patient records: five real, from MIMIC-IV, and
  five synthetic, each synthetic record matched to a real one on
  clinical domain, sex, age and chart length. They were presented in
  the benchmark's own Epic-like interface.
- **The judges.** Ten licensed physicians, each reviewing all ten
  records in an individually randomized order and labelling each real
  or synthetic, with a stated confidence. One hundred judgments.
- **The conversion of the real arm.** Real MIMIC-IV documentation does
  not look like the synthetic schema, and physicians would have
  discriminated on format alone. So each real admission's discharge
  note, admission record and radiology reports were parsed into the
  same eighteen section types, in the same order, as the synthetic
  notes. Dates were shifted into 2020–2024 with intervals preserved;
  masked provider names were replaced with consistent fictional ones,
  or the clause dropped; lab panels were expanded to one line per
  result with units and ranges; medication frequency codes and tall-man
  lettering were expanded; vitals were re-rendered as sentences;
  radiology reports were cut to their impression.
- **Harmonization of both arms.** Sections only one arm could carry
  were removed — social history from the synthetic arm (MIMIC redacts
  it), demographics and assessment/plan from the real arm — and
  trailing admissions beyond a shared length budget were dropped, always
  at a section or encounter boundary.
- **What was left alone.** The clinical prose itself was not rewritten,
  so the telegraphic register of real discharge notes remained for the
  physicians to notice if they could.

The result: 53% accuracy (95% bootstrap interval 43–63%), no bias
toward either label, no physician above chance after correction for
multiple comparisons, and inter-rater agreement no better than chance
(Fleiss' κ = −0.05). Every synthetic record was judged real by at
least one physician.

### What that result supports, and what it does not

The authors are careful here, and their care is the lesson.

- **It is a record-level claim, not a population claim.** They say so
  directly, and characterize case mix separately: it follows the
  education-derived source material, not any real population, by
  design.
- **The effective sample is ten records, not a hundred judgments.**
  Judgments cluster by record; an interval reaching 63% cannot rule out
  real detectability. "We could not detect a difference" is the
  supportable reading, and it is weaker than "indistinguishable."
- **Harmonization narrowed the question, deliberately.** By removing
  the style layer, the study tested whether *clinical content*, stripped
  of institutional style, reads as plausible. That is a legitimate and
  useful question. It is not "does this look like a real hospital's
  chart."

One further number deserves a reader's attention: on whole-patient
summarization, the physician reference scored 0.05 finding-level F1
against roughly 0.5 for the best models. When experts score near zero
on a metric, the metric is measuring something other than expertise —
here, exhaustive enumeration of graph-defined findings. That is not a
flaw peculiar to this paper; it is a general hazard of any oracle
derived from a complete ground truth, this workspace's included.

## Why the realism result does not transfer

A physician can judge a chart because a chart is written for a
physician. Hospital-operations traffic is not written for anyone to
read. Whether an HL7v2 feed looks real is a question about
distributions a human is poorly placed to judge by eye: the mix of
event kinds, the gaps between them, how often admissions are cancelled
and reinstated, how many updates follow a registration, which optional
fields a site populates and which it leaves empty. A panel of
physicians shown this workspace's output would be judging the wrong
object; a panel of interface analysts would be closer, and a
statistical test closer still.

So the comparison is meaningful as *method* and not as *evidence*.
Nothing in Synthetic Hospital's result says anything about this
workspace's realism. A great deal in its design says how one might go
about measuring it.

## Ideas worth keeping

Each of these is an observation about the paper, followed by the shape
the same idea would take here. None is scheduled.

**Renderer invariance as a tested property.** The paper re-rendered one
hundred patients with a different language model, holding the graph
fixed, and showed that scores and model rankings barely moved — direct
evidence that the benchmark measures the ground truth and not the
prose. This workspace already proves the narrow form for site
profiles: two profiles over one seed yield a byte-identical ground-truth
log, and their messages agree once the declared dialect surfaces are
masked. The broader test would state it as a property of every emitter
configuration, not only a site profile: for a fixed ground truth, judge
verdicts and consumer-facing invariants are unchanged. That would turn
"emission-only" from a declaration into a check wherever it is
claimed.

**Chart-neutral scoring.** The paper neither credits nor penalizes a
diagnosis a model reports that the reference does not contain; only
omissions and contradictions count. For a consumer checking their own
system against this workspace's log, the same split is useful: *absent
from the oracle* and *contradicts the oracle* are different findings,
and a check that conflates them will either drown in noise or miss
real errors.

**Domain rules pre-registered, gaps published.** Before building its
graph, the team had a physician write down 119 relationships the graph
was required to recover. It recovered 111, and the eight misses are
documented as accepted gaps rather than tuned away. The analogue here
would be a list of operational rules, written by someone who runs
hospital interfaces rather than by this workspace's authors, measured
against the invariant catalog, with the misses published alongside the
hits.

**Record-level and population-level realism, kept apart.** The paper
never lets its physician result stand in for a distributional claim.
This workspace's documentation should hold the same line: per-patient
pathway plausibility is one claim; fidelity of event mix and arrival
process to a real hospital is another; only what has been measured
gets claimed.

**The limitation it names is the capability this workspace builds.**
The paper states plainly that it does not simulate missingness,
contradictions or documentation errors. Controlled, attributable faults
are exactly what the torture kit on
[`future-features.md`](../future-features.md) is about. That is a
difference of purpose, not a criticism in either direction.

**Leakage through the serving surface.** The paper's own repository
discloses that one of its tools revealed the answer key and inflated
some agentic scores. The mirror question for a traffic generator is
what on the wire gives synthesis away — identifiers too regular,
control IDs too informative, timestamps too clean. Those are the
features a discriminator would find first.

## An equivalent study, sketched

If this workspace ever set out to measure how real its traffic looks,
the paper's protocol suggests the order of work. Every step below is a
sketch, not a design.

1. **A real arm that never leaves its owner.** A team with real feed
   data — the natural partner is a consumer already comparing this
   workspace's corpora against their own population — keeps it on
   their side. What crosses the boundary is summary statistics and test
   results, never messages.
2. **Harmonize both arms onto one site profile.** This is the step the
   paper shows cannot be skipped: without it, any judge discriminates
   on style. Here the instrument already half-exists —
   [site profiles](../site-profiles.md) describe part of a site's
   idiom for emission — header conventions, code tables, Z-segments.
   Run in the other direction, the same description would normalize
   those in a real feed; optional-field population, identifier formats
   and timestamp precision are idiom it does not yet describe.
3. **A machine discriminator before any human panel.** Compute the same
   corpus census on both arms — `ehrt sim describe` gives part of it
   for a log this workspace produced, and the summarize entry on the
   future-features menu names the rest — and train a classifier to tell
   them apart. The number that comes back (how separable the two are)
   matters less than the list of features that separate them: that list
   is a ranked account of where this workspace's traffic is least like
   a real hospital's.
4. **A human panel only if the machine cannot tell.** Matched patient
   timelines, both arms rendered in the same message viewer, judged by
   interface analysts rather than physicians, with enough records that
   the effective sample is not ten.

## Open questions

- What does a site-profile harmonizer have to erase for a comparison to
  be fair, and what must it preserve for the comparison to mean
  anything?
- Which wire-level regularities that make a corpus easy to *test with*
  — stable, joinable identifiers above all — also make it easy to
  *recognize*, and is that a trade a consumer should be able to choose?
- Is there an operational analogue of the paper's physician-written
  relationship list that a partner would be willing to write, and would
  they write it before seeing the invariant catalog?
- A benchmark that renders with a language model can make its labels
  deterministic but not its bytes. Is there any stage of this
  workspace's pipeline where that trade would be worth making, or is
  byte-identity a line that stays where it is?

## Further reading

- [`README.md`](README.md) in this directory — the lineage this essay
  sits beside: Synthea, Google Simulated Hospital, and what was taken
  from each.
- [`what-is-this.md`](../what-is-this.md#validation--evidence) — the
  claims this workspace makes about itself and how each is meant to be
  proved.
- [`site-profiles.md`](../site-profiles.md) — the configuration layer a
  harmonizer would reuse.
