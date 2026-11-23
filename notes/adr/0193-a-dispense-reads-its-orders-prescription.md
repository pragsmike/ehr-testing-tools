## ADR-0193 — a dispense reads its order's prescription: supply, quantity, the refill count and the refill interval

**Status:** Accepted 2026-11-23 (author ruling of 2026-11-23, quoted
below; build session 2026-11-23). Amends ADR-0191 (what refills, and
when) and ADR-0192 (its "nothing reads it"). Event schema **1.15.0**,
additive; describe report **1.4.0**, unchanged; stream scheme
**"1.0"**, unchanged; invariant catalog **49**.

### Context

ADR-0192 put the prescription on the order and changed no behaviour. A
dispense stated no quantity and no days-supply. A refill chain ignored
the order's authorised refills: `dense-7500`'s chronic order stated 5
and its 28,024 log showed up to 124 fills against one order. And the
refill interval was a facility range, 28 to 35 days, whatever the
order's supply.

Measured from the vendored modules before the ruling, and confirmed at
the base: of 37 chronic MedicationOrder states, the 32 inhaler states
author `refills: 0` and the other 5 author no prescription at all; the
one state with a positive count (hypothyroidism, 6) is not marked
chronic. So "chronic" and "has refills" are different facts in the
modules, and a rule built on either alone would get the other wrong.

Author ruling, verbatim, 2026-11-23: "Accept your recommendations.
Go.", to these four and the two fields below them:

1. "What makes an order refillable? I recommend: it is chronic, or it
   states a positive refill count."
2. "Does a stated count bound the chain? I recommend yes: at most that
   many refills, and a stated 0 means none even on a chronic order. An
   order with no count runs on the 0.94 draw alone."
3. "Dense's chronic order ... I recommend stating 11, a year at 30
   days. ... Renewal, meaning a new order when refills run out, would
   be named as owed."
4. "Interval from supply. Where an order states a duration, the next
   fill comes that many days later plus a drawn offset of -2 to +5
   days, as a config option. Orders with no duration keep the
   28-to-35-day range."

Fields: "Days-supply: the order's duration in days, where stated.
Quantity: dose amount times frequency over that supply, where both are
stated. For an inpatient dispense I'd use one day's supply ... labelled
provisional."

### Decision

**1. Refillable.** An order is refillable when it is `:chronic`, or
when its `:prescription` states a `:refills` above 0. An order that is
neither is filled at most once.

**2. A stated count bounds the chain.** Where an order states
`:refills N`, a fill is followed by another only while its
`:fill-number` is at most `N`: at most `N` + 1 dispenses. A stated 0
means the first fill alone, on a chronic order too. An order that
states no count has no bound and runs on `:refill-continuation`. Inside
a bound the continuation draw still applies.

The consequence for the modules, stated and not softened: **a
module-written inhaler order is filled once and never refilled**,
because its module says `refills: 0`. The module is the author; this
ADR reads what it wrote.

**3. A stated duration sets the interval.** One new facility option,
`:refill-offset-days`, `[lo hi]` whole days of either sign with
`lo <= hi`, default `[-2 5]`. Where an order states a duration that
converts to whole days, the next fill is queued that many days later
plus one draw from the offset range, and never less than one day
later. Where it states none, or one in a unit that does not convert,
the gap is one draw from `:refill-interval-days`, as before.

The default was chosen so that a 30-day supply refills 28 to 35 days
later, which is ADR-0191's range. It is also the same draw:
`30 + rand-int-in(-2, 5)` and `rand-int-in(28, 35)` are both
`lo + nextInt(8)`, and a gated test holds the two equal fill for fill.

**4. The draws do not change in number.** Two per community fill of a
refillable order, on the ordering patient's `:pharmacy` stream:
continuation, then the gap. Both are taken when the bound has already
ended the chain. An order refillable only by its count takes two draws
per fill that it did not take before.

**5. Two optional keys on `:medication-dispense`.**

| key | `:community` | `:inpatient` |
|---|---|---|
| `:days-supply` | the order's stated duration in whole days | `1`, when the order states a dosage |
| `:quantity` | amount x frequency x (days of supply / the dosage period in days), when the order states a dosage too | one day's doses |

Each is absent when the order states too little: no duration (a
community fill has neither key), no dosage (no quantity; an inpatient
dispense has neither key), or a unit that does not convert.

**6. Units.** `"days"` is 1, `"weeks"` is 7, `"hours"` is 1/24: the
three strings the vendored modules use. Nothing else converts, and a
duration that is not a whole number of days (36 hours) yields no
supply. Nothing is rounded.

**7. `:quantity` is exact.** The arithmetic is rational. A whole
result is an integer; any other is a double (one tablet every two days
for five days is 2.5). The schema types it as a positive number. It is
in the dose's own unit, which a prescription does not name.

**8. The inpatient day is an assumption, and provisional.** An
inpatient dispense is one day's supply whatever the order's duration.
Nothing in the model says how a ward is stocked; a day is the simplest
thing that is not obviously wrong.

**9. The order's record keeps the prescription.** The fold's
`:medication-orders` entry holds the event's `:prescription` whole, and
a dispense reads it there. A fill carried through a merge reads the
absorbed record's order: the same bound, interval and fields.

**10. An authored dispense reads it too.** A `:medication-dispense`
step a pathway author writes carries the two fields by the same rule.
It still starts no chain.

**11. A new invariant**,
`medication-dispense-fills-stay-within-the-orders-stated-refills`: an
order that states `:refills N` is dispensed at most `N` + 1 times,
counted over every setting. The numbering invariant stands.

**12. `dense-7500`'s chronic order states `:refills 11`**, a year at
30 days, and its facility writes `:refill-offset-days [-2 5]` out
beside the other refill options. Provisional like the rest of its
regimens.

### The schema

`classify-change` against the frozen 1.14.0 baseline, before the
re-freeze:

    {:additive? true, :breaking []}

A 1.14.0 log validates unchanged against 1.15.0. Made under the
deprecation waiver; nothing was removed.

### What a consumer may now check, and must expect

- **Fills against the stated count.** Group dispenses by
  `:order-event-id`; where the order states `:refills N` there are at
  most `N` + 1. `sim check` holds this.
- **Quantity against dose and supply**, on a community fill.
- **An inpatient `:days-supply` is 1 by assumption.** It is not the
  length of the course.
- **Absent still means unstated.** Most module-written orders state no
  duration, and their fills carry neither key.
- **Chains are shorter where orders state a count.** A corpus built
  before this ADR holds chains that run on the draw alone.
- **Nothing renews.** When the refills run out no new order is written.

### What moved, predicted and measured

Predicted before any edit by cutting each base chain at twelve fills.

| | 750 | 7,500 | 28,024 |
|---|---|---|---|
| refills at the base | 647 | 6,805 | 27,638 |
| predicted | 361 | 3,266 | 13,629 |
| measured | 361 | 3,266 | 13,629 |
| chains stopped by the bound, predicted and measured | 25 | 221 | 891 |
| chains stopped otherwise | 23 | 203 | 908 |
| fills per filled chronic order, mean / median / max | 8.52 / 12 / 12 | 8.70 / 12 / 12 | 8.58 / 11 / 12 |
| dispenses, predicted | 805 | 7,337 | 30,119 |
| dispenses, measured | 805 | 7,337 | 30,118 |
| first fills that left / arrived | 0 / 0 | 1 / 1 | 2 / 1 |
| dispenses with both fields | 802 of 805 | 7,271 of 7,337 | 30,045 of 30,118 |

Every refill in a new log is one the base log holds, at the same
instant: the 30-day offset draw lands where the interval draw did. The
first fills that moved are the ones step 1 expected and could not
count: a cut chain takes fewer draws, so a later prescription of the
same patient draws further up their `:pharmacy` stream. No inpatient
dispense moved. Every non-dispense event equals its base once the log
indexes that a shorter log shifts are renumbered.

The channel's arithmetic for 28,024, "roughly 14,000", was the
geometric sum times 1,799 filled orders. The measured 13,629 is a
little under it because some chains end early for another reason.

**Messages moved though no message is rendered for a dispense**: -30,
+19, 0 and -10 across the four cells, all ADT^A08 at 750. The wire
planner draws once per ground-truth event in log order (ADR-0189's
finding), so a log with fewer events draws differently for the events
after them.

**The oracle.** Three of the 46 roots hold a dispense and none of
their orders carries a prescription. Predicted: no root moves. See the
session record for the instruments' output. The consequence is a gap,
named below: no oracle root exercises any of this.

`dense-7500` by the usual drill at `ee33ac47`: `config.edn` at 7,500
is 130,055 events and 125,917 messages; the 28,024 cell 516,228 events
and 32,080 subjects, 195,875,969 bytes, sha-256
`1795158ea3362431d3a6b8533ad409fb9508530d914f5025c711a501089ecaf4`.
Before this ADR it was 199,000,793 bytes, `83b33fc1...`, kept in
`figures.edn` under `:pre-adr-0193`.

### Downstream

Event schema **1.15.0**, additive. Corpora that move: every corpus
generated with `:dispense` whose in-log orders state a `:refills`, a
duration or a dosage. For `dense-7500` that is every cell but the bare
one in count, and the bare one in bytes (its order events state 11
where they stated 5). A corpus with `:dispense` and no prescription on
any order, `osteoporosis-dispense` among them, is byte-identical. A
retained corpus is a valid artifact of its pin, at 1.14.0.

### Owed next

- **Renewal**: a new order when an order's authorised refills run out.
  Without it a chronic prescription that states a count stops after a
  year in `dense-7500`.
- **A 90-day supply.**
- **Pre-horizon order facts carrying a prescription, and being
  refilled.** A widened pre-horizon fact is a breaking bump (ADR-0192,
  decision 6).
- **A non-uniform first-fill lag.**
- **Dosage on the FHIR MedicationRequest, and a MedicationDispense
  resource.** Neither field renders on any wire.
- **`dense-inpatient`'s fracture codes**: care plan, procedure and
  report still describe a fracture in an abdominal-pain stay.
- **An oracle root that exercises this.** No root's dispensed order
  states a prescription, so both instruments are blind to the bound,
  the interval and the two fields. The tests and the dense exerciser
  carry it today.
- Carried from ADR-0191: a refusal for a continuation of 1 with no
  tail (now needed only for an order that states no count); the
  mutation column for the dispense.

### What this does not do

- Renders nothing on a wire.
- Defaults no dose, supply or count for an order that states none.
- Rounds nothing and converts no unit but the three named.
- Writes no new order when refills run out.
- Refills no pre-horizon order.
