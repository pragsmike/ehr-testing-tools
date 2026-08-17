# Chapter 3 — A simulated hospital

Chapter 2 generated one patient's worth of traffic. This chapter scales
that up to a real shift: `ehrt sim run` is the engine underneath every
corpus this workspace's own simulator produces, and
[`ed-tuesday`](../../demos/scenarios/ed-tuesday/README.md) — this
manual's one running scenario — is what a hundred patients' worth of
that engine, scripted toward the trauma/injury traffic a real emergency
department actually sees, looks like.

## `sim run`, and the door you actually walk through

[`docs/cli.md`](../cli.md) names two tiers over the same engine: `ehrt
sim run` is the strict tier — every input (`--seed`, `--patients`,
`--reference-date`) required, nothing defaulted, built for scripted or
programmatic use. `ehrt corpus generate sim` is the ergonomic front
door most of this manual uses — same engine, sensible defaults, an
`--out-dir` and a `manifest.edn` it manages for you. Neither is more
"real" than the other; they're the same simulator at two different
distances from the machinery. `ed-tuesday`'s own README uses the front
door throughout, and so does this chapter — copied verbatim from that
README, never composed for the occasion:

```bash
bin/ehrt corpus generate sim --seed 20260811 --patients 100 \
  --reference-date 2026-08-11 --churn \
  --config demos/scenarios/ed-tuesday/config.edn \
  --out-dir out/scenarios/ed-tuesday
```

Witnessed this session: `{:status :ok, :payload {:out-dir
"out/scenarios/ed-tuesday"}}` — a hundred patients, a real inpatient
[census](../glossary.md), real administrative [churn](../glossary.md), all from one seed and one config
file. Play it back with `--board` and watch the shift happen —
[`ed-tuesday`'s own "What to look for"](../../demos/scenarios/ed-tuesday/README.md#what-to-look-for)
has the full witnessed board snapshots: occupied beds climbing from 4
to a peak of 21 and draining back to 3, discharges accruing from 1 to
84, churn firing along the way. That's `--config`'s own job: one EDN
file (`demos/scenarios/ed-tuesday/config.edn`) is the entire difference
between this scripted ED shift and Chapter 2's own bare, single-patient
default.

## Your hospital's own accent: site profiles

Every real hospital extends the standards it nominally conforms to, and
every hospital does it differently — a patient-class code, a discharge
disposition, a Z-segment nobody outside that one institution has ever
seen. Hard-coding one hospital's idiom into the emitter would make
every corpus this workspace generates look like the same fictional
institution. [`site-profiles.md`](../site-profiles.md) is the config
layer that answers this: an MSH dialect, code-table overrides, and
Z-segment templates you supply once, in one `--config` file, applied
purely at render time. The load-bearing guarantee, proven there, not
merely asserted: two [site profiles](../glossary.md) run over the same seed produce
**the same ground truth** in two accents — a site profile can change
how a fact is *said*, never what fact is true. `ed-tuesday`'s own
`config.edn` doesn't use one (this scenario is deliberately institution-
neutral), which is itself the point: a site profile is opt-in dialect,
layered on top of a scenario that runs identically without one.

## Scripted patients, and patients the module walk generates

Look inside `ed-tuesday`'s own `config.edn` and you'll find two
different ways a patient ends up in this shift, side by side. Most of
the population follows one of five hand-**scripted** ED
[pathways](../glossary.md) —
admission, workup, transfer, discharge — authored directly as `:pathways`,
weighted toward the fast, common end the way a real ED's own admission
rate actually skews. A small tail of eight patients is instead assigned
a MITRE-authored disease module (`sore_throat`, `sinusitis`,
`bronchitis`, `ear_infections`) via `:module-assignment` — **generative**
in the sense that nobody scripted what happens to them; the module's own
state graph, walked under this run's seeded RNG, decides it. Both kinds
of patient are real inputs to the same engine, resolved into the same
kind of pathway data before the engine ever sees which one produced it
— [`trajectory-computation.md`](../../components/sim-trajectory/docs/trajectory-computation.md)
section 2 is the full account of how a module walk compiles down to the
same intermediate representation a hand-authored pathway already is.
`ed-tuesday`'s own README discloses something worth knowing before you
lean on a thin generative tail for demo content: at this scenario's
short, day-scale horizon, all eight module-assigned patients produced
**zero** live encounters in the witnessed run — genuinely low-incidence
modules behaving exactly as low-incidence, not a bug, and not silently
retuned away. Real generative content needs either more patients or a
longer horizon to show up; `ed-tuesday`'s scripted half is what actually
drives this shift's own visible traffic.

## Two spaces, one wall

Every patient this simulator produces traffic for passes through two
state machines that must never be confused with each other — the
classic error this domain invites, and the reason
[`trajectory-computation.md`](../../components/sim-trajectory/docs/trajectory-computation.md)
exists as a document at all.

The first is [**script space**](../glossary.md): whatever produced a patient's own
pathway — a hand-authored `:pathways` entry or a walked disease module,
Chapter 3's own previous section — is only ever a *plan*. It says an
encounter *should* happen, a medication *should* be ordered — never
which bed, which attending, whether a transfer later gets cancelled.
Nothing in script space can write a fact; it only ever produces data
waiting to be interpreted.

The second is [**truth space**](../glossary.md): a single, capacity-aware engine reads
that plan, alongside every other patient's own state so far, and turns
it into what *actually* happened — this bed, this attending, cancelled
or not. There is a wall between the two, and it's structural, not a
matter of discipline: nothing downstream of it can be un-decided once
crossed. `ed-tuesday`'s own peak-21-inpatients, capacity-held-without-
exhaustion shift is truth space's own output; the five weighted
pathways that fed it were only ever script space's plan for what
*might* happen.

**The same split shows up one layer downstream, in how a shift gets
rendered onto a wire.** Once truth space has settled what actually
happened — the [ground truth](../glossary.md), `GT` — this workspace renders it two
independent ways: as HL7v2 messages (`emitH`), or as FHIR resources
(`emitF`). Neither is derived from the other; both read the same `GT`
object and produce their own wire format, entirely independently:

<img src="assets/gt-emitters.svg" alt="Ground truth GT rendered by two independent emitters, emitH to HL7v2 messages and emitF to FHIR bundles" width="640" />

This is this workspace's own founding idea, stated as plainly as it
gets: **formats are just [emitters](../glossary.md) of the patient state machine.**
`GT` is the one thing that's true; HL7v2 and FHIR are just two accents
it's read aloud in. That two independently-written renderers agree
about *who* they're both talking about isn't assumed — it's a proven
property (the "naturality square" in the figure above: both emitter
arrows out of `GT` commute with patient identity, checked by a
150-trial generated-case test, not merely inspected by eye). The full
formal treatment — what a "lowering," an "emitter," and this
naturality claim precisely mean — lives in
[`docs/dev/simulator-architecture.md`](../dev/simulator-architecture.md#4-the-palgebra)
section 4. This manual won't re-teach that formalism; the two-spaces
story above is everything you need to read `ed-tuesday`'s own traffic
correctly, and the link is there for when you want the proof underneath
it.

## The log underneath every message

`GT` is not just a letter in a diagram. It is a file you can have, and
the same engine that rendered those two accents will hand you the log
itself:

```bash
bin/ehrt sim run --seed 42 --patients 5 --format ground-truth
```

That prints the bare EDN vector — one map per fact, in run order, with
`:t` an integer of seconds since the run began. Copied verbatim from
[Write your own emitter from the event log](../use-cases/custom-emitter-from-the-event-log.md),
never composed for the occasion, the same way this chapter's own
`generate sim` strip is copied from `ed-tuesday`'s README.

This matters beyond curiosity, and it is the practical form of the
founding idea above. If your own system speaks a format this workspace
doesn't ship — a proprietary interface, an internal schema, a vendor's
flat file — the log is where you start, because it is a **published,
versioned contract** rather than an internal shape that happens to be
printable. [`formats.md`](../formats.md#the-event-log)'s "The event log"
is that contract: twenty-one event kinds, closed, with the keys each one
carries and one real example each, all generated from a committed
schema. Every run's `manifest.edn` records the `:event-schema-version`
it was produced under, so a log always says which version of the
contract made it.

The use-case page above walks the whole path and links a worked example
emitter that depends on nothing off this repo's classpath. That last
part is the demonstration, not a detail: an example that needed our code
to run would prove the opposite of what it claims.

One signpost, because the next chapters invite the confusion: Chapter 8
is about your own **data** arriving. This is your own **format** going
out — the other direction entirely.
