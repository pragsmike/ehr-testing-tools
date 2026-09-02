# Future features — the torture kit

This page is a menu, not a plan. Everything on it is a thing this
workspace does **not** do yet, written down so you can tell whether
waiting for it is reasonable and so nobody builds around a capability
that isn't here. Nothing below has a date, and nothing below is a
commitment.

**What the menu is for.** If you run an HL7-handling system — an
interface engine, a receiving EHR, an MPI, an archive — the question
you actually have is *"what breaks it?"* Correct traffic answers only
half of that. The other half is a torture kit: traffic that is wrong on
purpose, in a way you chose, so that what your system does about it is
a measurement rather than a surprise.

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

*Design stance:* these are faults **of a live connection**, and this
workspace does not currently open one — the MLLP frame codec is here and
byte-exact, but nothing transmits over a socket, and that was a
deliberate stop rather than an oversight. This entry therefore waits on
a decision to do transport work at all, not merely on somebody writing
it.[^transport]

## Scale ergonomics

**Not every gap on this page is a fault class.** Everything above is
about traffic that is wrong on purpose. The entries below are about
the ergonomics of asking for a lot of traffic at once — what a
consumer generating at scale has to do by hand today, and would
rather not.

### Stopping at a natural boundary, by event count

Ask for a corpus of roughly *N* events rather than *N* patients, and
have the run stop at the next natural boundary — an encounter closed,
a shift ended — instead of mid-trajectory. What you get is a corpus
sized the way you actually think about it, with no truncated patient
at the end of it.

*Today:* `--patients` counts arrivals, so sizing by event count means
running, counting, and adjusting the patient count by hand — and the
ratio moves with which opt-in keys are on.

### A summarize command

Point it at a corpus — a ground-truth log or a rendered directory —
and get the census back: how many events of each kind, how many
messages of each type, how many patients and encounters, over what
span. The thing you want immediately after generating, and the thing
you want to diff between two runs.

*Today:* `ehrt sim run`'s default EDN envelope carries a run summary,
and `--format ground-truth` piped into a frequency count of your own
gives the per-kind census.

### Progress while a long run generates

A large run produces nothing observable until it produces everything.
Progress instrumentation would say which phase is in flight and how
far through it is, so a run that is merely slow can be told apart
from a run that has stopped making progress.

*Today:* there is no workaround. A long run is silent until it exits.

### Richer capacity-exhaustion diagnostics

When a run stops on `:capacity-exhausted`, the useful answer is not
only *that* it stopped but *what would have prevented it*: how far
over the ceiling the arrivals ran, which ward bound first, and what
bed count or surge allowance would have absorbed them.

*Today:* the error payload names the patient, the ward, and the
census at the moment of refusal — enough to find the ward that
filled, not enough to size the fix without another run.

### Streaming output

Emit as the run goes rather than accumulating a whole corpus in
memory first, so corpus size stops being bounded by heap. This one
enters the menu as **pending measurement** rather than as a design:
what has been measured is that the emit phase, not the retained event
log, is the binding constraint at the top of the range — see
[Scale](consuming-ground-truth.md#scale). Whether streaming is the
right answer to that is a question the measurement has not been taken
far enough to settle.

*Today:* `--format ground-truth` is the cheap path; a consumer who
writes their own emitter is not paying the emit phase at all.

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
[^transport]: Design record [ADR-0102](../notes/ADRs.md) records the decision to stop transport work, and it stands until it is revisited deliberately.
