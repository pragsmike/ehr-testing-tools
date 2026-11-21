# Future features — the torture kit

This page is a menu, not a plan: everything on it is something this
workspace does **not** do yet, written down so you can tell whether
waiting for it is reasonable and nobody builds around a capability that
isn't here. Nothing below has a date or is a commitment.

**What the menu is for.** If you run an HL7-handling system — an
interface engine, a receiving EHR, an MPI, an archive — the question
you actually have is *"what breaks it?"* Correct traffic answers half of
that. The other half is a torture kit: traffic wrong on purpose, in a
way you chose, so what your system does about it is a measurement rather
than a surprise.

Faults live at three layers, and the whole point of separating them is
that **a receiver that handles one correctly can still be wrong about
the others.**

| Layer | What is wrong | What is still right |
|---|---|---|
| **Content** | The bytes inside a message | The message arrives, once, in order, intact as a frame |
| **Stream** | The sequence — which messages arrive, how often, in what order | Every individual message is valid and well-formed |
| **Transport** | The framing and the connection | The messages, and the sequence they were sent in |

Some of this exists today. [`consuming-ground-truth.md`](consuming-ground-truth.md#fault-injection-as-it-stands-today)
has the honest inventory of what ships: awkward-but-correct traffic
from `--churn`, file-level defect injection through
[`ehrt corpus mutate`](operators.md), and delayed or batched
transmission. This page is the rest.

## The layer boundary, and why it is load-bearing

**A duplicate event and a duplicate message are opposite faults that
look identical on the wire.**

- A duplicate **event** means the world had two occurrences. The patient
  really was admitted twice; two A01s were correctly sent. **A receiver
  that dedupes them is wrong** — it has just erased a real admission.
- A duplicate **message** means the world had one occurrence and the
  transport sent it twice. **A receiver that keeps both is wrong** — it
  has just invented an admission that never happened.

Same two messages. Opposite correct behaviour. The only thing that
distinguishes them is which layer the fault was injected at, and that is
information a test harness has and a receiver does not.

This is why the kit is three injectors rather than one, and it is the
rule every entry below is written against: **an injector states its
layer, and its layer states what a correct receiver is supposed to do.**
The same split applies to loss (a missed event versus a dropped message)
and to ordering (a world whose events genuinely interleave versus a
transport that delivered them out of order).

## Content faults

### Event-stream mutation

Mutate the **ground-truth event log**, not the rendered files — so a
single operator produces the same defect in v2, in FHIR, and in any
emitter you wrote yourself, without being reimplemented per format.

*Design stance:* mutation is a stage over a finished log, not a setting
on the run — the record is wrong and the world was right, so the
simulation must not be re-derived from the mutant; and
[`ehrt sim check`](cli.md#ehrt-sim) is the oracle, so every operator
declares which invariant it is built to trip and the test is that
exactly that one fires and nothing else.[^event-mutation]

### Foreign-corpus mutation

Apply defects to a corpus **this workspace did not generate** — your own
captured traffic, a vendor's sample set — by splicing a span out of one
message and into another.

*Design stance:* there is no invariant catalog behind a corpus we did
not produce, so the oracle has to be differential rather than absolute
— two gate engines disagreeing about the same spliced message is the
finding — and lineage records the content hash of **both** parents, so a
mutant always names the two corpora it came from.[^foreign-corpus]

## Stream faults

### Drop, duplicate, reorder and delay

Take a rendered message sequence and disturb it: lose one, send one
twice, swap two, hold one back. Every message stays byte-valid; only
which ones arrive, how often, and in what order changes.

*Design stance:* a seeded pure function from a message sequence to a
message sequence, applied after rendering and before transmission, so it
composes with any emitter and any transport and never has to know what a
message means. Per the boundary above, this is the injector whose
duplicates a receiver **should** dedupe — and pairing it against
event-level duplication is how you find out whether your receiver can
tell the difference.[^stream-layer]

## Transport faults

### Framing corruption, truncation and resets

Break the envelope rather than the payload: a start-block byte that
never arrives, a message cut off mid-segment, a connection dropped
between the header and the trailer, a peer that resets after the
acknowledgement was owed.

*Design stance:* these are faults **of a live connection**. Playback
does open one: `ehrt play --sink mllp://HOST:PORT` sends each message
over MLLP and reads its ACK back, failing on a negative, unrecognized
or missing acknowledgement. That is delivery, not fault work — it sends
every frame intact and checks the answer, and nothing here breaks the
envelope on purpose. Fault injection over that connection was stopped
deliberately, not overlooked, so this entry waits on a decision to do
that work at all, not merely on somebody writing it.[^transport]

## Scale ergonomics

**Not every gap here is a fault class.** Everything above is traffic
that is wrong on purpose; below are the ergonomics of asking for a lot
of it at once — what generating at scale costs you by hand today.

### Stopping at a natural boundary, by event count

Ask for roughly *N* events rather than *N* patients, and have the run
stop at the next natural boundary — an encounter closed, a shift ended —
rather than mid-trajectory: a corpus sized the way you think about it,
with no truncated patient at the end.

*Today:* `--patients` counts arrivals, so sizing by event count means
running, counting and adjusting by hand — and the ratio moves with which
opt-in keys are on.

### A summarize command, and a scenario inventory

Point it at a corpus and get the census back: how many events of each
kind, how many messages of each type, how many patients and encounters,
over what span — the thing you want immediately after generating, and
the thing you want to diff between two runs.

**The harder half is the inventory.** A count says a corpus holds 2,850
merges; it does not say whether any absorbed a patient who was holding
a bed, or whether a result ever landed after its own subject was merged
away — and those decide whether a corpus can exercise the case you are
about to write a test for. Those 2,850 merges yield **43** results
landing post-merge across a 533,147-event run holding 33,681 results in
all, and the 7,500-arrival cell of
[`demos/scenarios/dense-7500/`](../demos/scenarios/dense-7500/README.md)
— `config.edn` byte for byte — yields **21**. The case is reachable;
it is just far too thin for any count of merges or of results to stand
in for. A first pass over these same logs read that column as zero, and
the zero was the predicate asking for a state the engine cannot produce
rather than the case being out of reach
([the correction](../.agents/plans/2026-09-05-performance-measurement/measurements.md)).
The inventory is the fixed set of columns that says
so: results landing after their subject moved, was discharged or was
merged; merges absorbing a bed-holder; cancels reinstating a bed; peak
ward census against capacity.

*Today:* `ehrt sim describe` reads an event log (or a `sim run`
envelope) and reports the log side of the census: events and subjects
per kind, the time span, and which churn and scheduling families were
configured beside which actually occurred, with witnesses located by
log index. It also covers three of the inventory's columns, as measures:
results landing after their subject was transferred, discharged or
merged. Still missing from the census: messages per type (describe
reads the event log, not the wire), encounter counts, and a command
that diffs two runs. Still missing from the inventory: merges absorbing
a bed-holder, cancels reinstating a bed, and peak ward census against
capacity. The inventory has been run only once, by hand, from a script
over `replay`'s projection
(`.agents/plans/2026-09-05-performance-measurement/`).

### Progress while a long run generates

A large run produces nothing observable until it produces everything.
Progress instrumentation would say which phase is in flight and how far
through it is, so a run that is merely slow can be told apart from one
that has stopped — at the top of the measured range that silence lasts
**39 minutes**.

*Today:* there is no workaround. A long run is silent until it exits.

### Richer capacity-exhaustion diagnostics

When a run stops on `:capacity-exhausted`, the useful answer is not only
*that* it stopped but *what would have prevented it*: how far over the
ceiling the arrivals ran, which ward bound first, and what bed count or
surge allowance would have absorbed them.

*Today:* the payload names the patient, the ward and the census at
refusal — enough to find the ward that filled, not enough to size the
fix without another run.

### Streaming output

Emit as the run goes rather than accumulating a whole corpus in memory
first, so corpus size stops being bounded by heap. The measurement cuts
both ways: with an emitter running the emit phase binds first
([Scale](consuming-ground-truth.md#scale)); with **no emitter at all**,
on `--format ground-truth`, the retained log is still held whole in
memory and binds on its own further up the same curve.

*Today:* `--format ground-truth` is the cheap path; a consumer who
writes their own emitter is not paying the emit phase at all.

## Fidelity evidence

**Not every gap here is a fault class.** Everything above is about
what the traffic does; this is about how you would know how closely it
resembles a real hospital's — evidence for a realism claim rather than
the claim itself.

### Measuring how real the traffic looks

Hold a generated corpus up against your own feed and get back where the
two differ: event mix, arrival gaps, churn rates, which optional fields
get populated — a ranked list of where the synthetic traffic is least
like yours, rather than a verdict that it is realistic.

*Design stance:* your real feed never leaves your side — only summary
statistics cross, taken after both arms are put into one site's idiom.

*Today:* nothing measures the resemblance. Operational realism rests on
pedigree and on site-tunable rates you calibrate by hand against your
own feed's statistics. [`related-work/synthetic-hospital.md`](related-work/synthetic-hospital.md)
sketches how such a study might run, borrowing the protocol of a
recent physician-validated benchmark.

## Clinical depth

**Not every gap here is a fault class.** Everything above is about how
the traffic is disturbed, sized or measured; this is about what the
simulated world itself contains — a clinical fact a real hospital
produces and this log stops short of.

### Medication dispensing and administration

Fills and administrations that each name the order they belong to, so a
medication order stops being the last thing the log says about a
medication: you could join a dispense to its prescription, count doses
given against doses ordered, and test what your system does with an
order nobody ever filled.

**"Dispense" is three different things, and the log would have to choose
among them.** An inpatient pharmacy dispense sends a drug to the unit. A
community fill happens at a retail pharmacy, against a prescription
written at discharge or in clinic, and a hospital feed may never see it
at all. An administration is a nurse giving a dose to a patient. They
have different sources, different timing and different messages, and a
consumer who asks for "dispense events" usually means exactly one of
them. **Which one you join against is the fact worth telling us.**

The first two now share one event kind, told apart by a stated
`:setting`; the third has none. What is still undecided: refills for a
medication the patient was already on before the log begins, a 90-day
supply beside the 30-day one, a lag whose shape is closer to real first
fills than a uniform range, and whether any of it reaches a wire
format.

*Today:* a `:medication-order` is a prescription fact and nothing more —
the patient, the medication's codes, and a citation to the module state
that wrote it. A `:medication-end` closes it, naming its order by that
citation. An order may be marked `:chronic true` (event schema 1.13.0):
a long-term medication, from a vendored module's own flag or a pathway
author's; absent means nobody marked it. A `:medication-dispense` names
the order it fills, states its `:setting`, `:inpatient` or
`:community`, and carries a `:fill-number` from 1; it repeats no
codes. **Two things produce one.**
A pathway step you author does, in either setting. The `:dispense`
engine key derives both. Each order an outpatient visit
wrote and left open is filled `:community` with a stated probability
(`:community-fill-share`, default 0.80) after a drawn lag
(`:community-fill-minutes`, default one hour to seven days after the
visit closes); both are facility options and both defaults are
provisional. About one in five is never filled at all, which a
consumer must not read as an error. A fill of a chronic order is then
refilled every 28 to 35 days, each fill followed by another with
chance 0.94, until the patient stops, the order ends, or a configured
tail after the run's last arrival is reached (`:refill-interval-days`,
`:refill-continuation`, `:refill-tail-days`, default 90 days; all
provisional). So one order may have several dispenses, numbered, and
the chain ends without an event. A fill pending when its patient's
record is merged away is given to the surviving record. Each order written
during an admission, which here includes every emergency-department
visit, gets one `:inpatient` dispense after a pharmacy turnaround drawn
from a range its ward states (`:dispense-minutes`, with provisional
defaults by ward class), unless the order has ended or the stay has
closed by then. An order still active when its stay is discharged, and
never dispensed inpatient, is a discharge prescription and is filled in
the community under the same share and lag. The key moves no
other draw, so the same seed with and without it is the same hospital
plus the dispenses. One shipped scenario sets the key, `dense-7500`,
whose 7,500-arrival log carries 2,533 inpatient dispenses and 2,298
community fills, 760 of them refills; none authors the step.
No event kind for a medication
administration exists. None of the three kinds renders to any HL7 v2
message: no pharmacy order, dispense or administration message (RDE,
RDS, RAS) is emitted. The FHIR emitter renders each order as a
`MedicationRequest` carrying its codes, status and authored time, and no
`MedicationDispense` or `MedicationAdministration`. The detail the
shipped modules author on an order — dosage, duration, refills,
as-needed and instructions, and the flag marking an administration —
is not carried into the log. The flag marking an order chronic is.
[`consuming-ground-truth.md`](consuming-ground-truth.md#what-is-not-warranted)
says the same where a consumer of the log will read it.

## What is not on this menu

Deliberately, and these are scope decisions rather than backlog:
semantic correctness checking of your own transforms, terminology
validation against licensed vocabularies, production message routing,
and a hosted validation service. The root
[`README.md`](../README.md#scope) and
[`what-is-this.md`](what-is-this.md#scope--what-this-deliberately-does-not-do)
state that fence normatively; nothing on this page erodes it.

[^event-mutation]: Design record [ADR-0176](../notes/ADRs.md) — the operator catalog, the injection contract, the oracle loop, and nine choices left open for a ruling.
[^foreign-corpus]: Named as out of scope by that same record, on the grounds that a log this workspace did not produce has no invariant catalog behind it.
[^stream-layer]: Whether message loss and duplication belong with mutation or with transport realism is an explicitly open question in design record [ADR-0111](../notes/ADRs.md); the layer boundary above is the shape an answer would take, not the answer itself.
[^transport]: Design record [ADR-0102](../notes/ADRs.md) records the decision to stop transport work. Design record [ADR-0175](../notes/ADRs.md) later ruled one exception in, the MLLP delivery sink `play` uses; the stop still stands for everything else here, fault injection included, until it is revisited deliberately.
