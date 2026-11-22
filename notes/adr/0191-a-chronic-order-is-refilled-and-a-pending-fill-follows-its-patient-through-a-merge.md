## ADR-0191 — a chronic order is refilled, every dispense is numbered, and a pending fill follows its patient through a merge

**Status:** Accepted 2026-11-21 (author rulings of 2026-11-20 and
2026-11-21, quoted below; build session 2026-11-21). Amends ADR-0190.
Event schema **1.13.0**; describe report **1.4.0**; stream scheme
**"1.0"**, unchanged.
**Amended 2026-11-22** by the addendum at the foot: decisions 4 and 5
no longer describe the default. `:refill-tail-days` is optional and
absent by default.

### Context

ADR-0190 made a community fill a draw: a share that is filled, and a
lag. Two things were left.

Every prescription was filled at most once. A long-term medication is
refilled, month after month, for as long as the patient stays on it.

And a fill queued for a patient later absorbed by a merge died with
the patient's other queued steps. Measured at the base, on
`dense-7500`: 3, 48 and 157 pending community fills dropped at 750,
7,500 and 28,024 arrivals, and 1, 0 and 2 pending inpatient dispenses.
A merge corrects a record. The person still collects the medication.

Author rulings, verbatim. 2026-11-20, "Accept all, go.", to defaults
including: "Refill interval for chronic orders: 28 to 35 days,
uniform" and "Chance each refill is followed by another: 0.94".
2026-11-21, "1. Yes. 2. tail. 3. ok.", to: (1) "Pending fills follow
the survivor through a merge? I recommend yes, for both settings.";
(2) "A configured tail (recommended). Refills may continue for a set
number of days after the last arrival, as a config option. I'd default
it to 90"; (3) "an optional `:chronic true` on a medication order,
carried from modules and authorable in a pathway, and a `:fill-number`
on every dispense, starting at 1. That takes the event schema to
1.13.0, additively." Also ruled there: a prescription whose first fill
never happens gets no refills; refills for chronic orders that predate
the horizon stay owed.

### Decision

**1. A medication order may be marked chronic.** `:chronic true` on a
`:medication-order` event, and on the pathway step that produces it.
The key is `true` or absent; absent means not marked, and `false` is
never written. Four vendored modules already carried Synthea's own
flag (`osteoporosis`, `metabolic_syndrome/medications`, and the two
inhaler submodules, 37 states); the loader now declares it and the
interpreter and compile carry it. A pathway author states it on the
step. An order that predates the horizon is a registration fact and
does not carry it.

**2. Every dispense is numbered.** `:fill-number` on every
`:medication-dispense`, authored, inpatient and community alike: which
dispense of its order this is, from 1, in log order. Several dispenses
citing one `:order-event-id` are one prescription filled and refilled.
A new invariant,
`medication-dispense-fill-numbers-count-up-from-one`, holds it: no
gap, no repeat, none out of turn.

**3. A chronic order's community fill is refilled.** Under the
`:dispense` key, when a derived community fill of a chronic order is
emitted, two draws are taken on the ordering patient's `:pharmacy`
stream: whether another fill follows, then how many days later. Both
are always taken. If another follows, it is queued at this fill's time
plus the interval. When it is decided it is declined on the tests a
first fill is declined on (ADR-0190, decision 5): the order has ended,
or is no longer the patient's latest of its citation, or the patient
has died. A refill takes no share draw and no lag.

An inpatient dispense is never refilled. A prescription that is never
filled is never refilled. A chronic order written in an admission and
filled as a discharge prescription is refilled as a visit's is. An
authored `:medication-dispense` step starts no chain.

**4. Three more facility options**, beside ADR-0190's two.

| key | meaning | default |
|---|---|---|
| `:refill-interval-days` | `[lo hi]` days from one fill to the next, integers with `1 <= lo <= hi`, uniform | `[28 35]` |
| `:refill-continuation` | probability that a fill is followed by another, a number from 0 to 1 | `0.94` |
| `:refill-tail-days` | no refill is queued later than the last arrival plus this many days; a non-negative integer | `90` |

The defaults are **provisional**. A 30-day supply is the traditional
retail unit; 90-day fills ran at 12 to 15% across four drug classes in
a post-infarction cohort of 353,259 patients
(https://scholars.duke.edu/individual/pub1470204) and are not
modelled. About half of patients who start a chronic medication
discontinue it (Franklin et al., J Gen Intern Med 2018, PMC6206334, as
background), and 0.94 compounded over twelve refills is about one
half. One interval and one chance for every chronic order and every
fill is a simplification: real discontinuation is front-loaded.

**5. The tail.** The bound is the time of the run's last arrival plus
the tail. The arrival clock is drawn whole before anything is decided,
so the bound is a fact of the configuration. It is the walk-in clock:
a booked visit, a hook's arrival and a person's later encounters can
all land after it. An order written past the bound is filled and never
refilled. A site that wants refills across a long horizon states a
long tail.

**6. A pending pharmacy step follows the survivor.** When a patient is
absorbed by a merge, their pending `:pharmacy-dispense` and
`:community-fill` steps are re-queued on the survivor at the same
instant, as ADR-0179 re-queues a pending result. The step is marked
with the patient who wrote the order, and keeps that mark through a
second merge. When it is decided:

- the order, and the stay it was written in, are read off the absorbed
  record, which nothing changes after its merge;
- the dispense is the survivor's event, under the surviving MRN, and
  its `:order-event-id` is still the absorbed record's order;
- a prescription's draws stay on the stream of the patient who wrote
  it, so the survivor's own draws do not move.

A carried community fill is filled as it would have been, and if its
order is chronic the chain carries on for the survivor.

A carried inpatient dispense goes to the survivor **if the survivor is
an inpatient at that instant**, in whatever stay they hold, and the
encounter stamp names that stay. The absorbed record's own stay never
closes (ADR-0179), so there is no other stay it could be dispensed
into. If the absorbed stay had already been discharged, the step is a
discharge prescription as it would have been. Otherwise nothing is
emitted. All three steps dropped at the base (one at 750, two at
28,024) were of the first kind, and all three are now dispensed.

The dispense law
(`medication-dispense-references-existing-order-and-follows-it-in-time`)
resolves "the same patient" through a merge, with the function the
result's law already uses. The end's law is not widened: an absorbed
record's end is never carried.

### The schema, and what "additively" turned out to mean

`:chronic` is an optional key and is additive. `:fill-number` is
required, and `classify-change` calls a new required key breaking:

    {:additive? false,
     :breaking [":medication-dispense: new REQUIRED key: :fill-number (add it as {:optional true} to stay additive)"]}

The ruling's word was "additively". The tree's own definition
disagrees, and the tree's is the one a consumer can test against. It
is required because a consumer grouping fills needs it on every
dispense. The cost is real and small: a 1.12.0-era log that holds a
dispense does not validate against 1.13.0. One that holds none does. A
consumer that reads only the keys it knew keeps working. Made under
the deprecation waiver; nothing was removed.

### What a consumer must now expect

- **An order may have several dispenses.** Group them by
  `:order-event-id`; `:fill-number` orders them and says how many came
  before.
- **A dispense's subject may not be its order's subject.** After a
  merge the fill is the survivor's. The `:merge` event between them
  names both.
- **A chronic prescription's fills stop without an event.** The patient
  stopped (the draw), the tail was reached, the order ended, or a newer
  order of the same citation replaced it. As with a missing first fill
  (ADR-0190), no event marks it and none of these is a defect.
- **`:chronic` absent does not mean short-term.** It means nobody
  marked the order. Most vendored modules mark nothing.

### What moved, predicted and measured

Seed 20260824, `--churn`.

| | 750 predicted | 750 measured | 7,500 predicted | 7,500 measured | 28,024 predicted | 28,024 measured |
|---|---|---|---|---|---|---|
| visit prescriptions filled | 155 | 156 | about 1,510 | 1,511 | about 6,118 | 6,122 |
| filled share of visit prescriptions | 0.783 | 0.788 | 0.784 | 0.785 | 0.785 | 0.786 |
| inpatient dispenses | 288 | 288 | 2,533 | 2,533 | 10,334 | 10,334 |
| community fills, all | -- | 237 | -- | 2,298 | -- | 9,751 |
| chronic orders | 60 | 60 | 534 | 534 | 2,262 | 2,262 |
| chronic orders with a first fill | 49 | 48 | about 419 | 424 | about 1,792 | 1,799 |
| refills | 80 to 96 | 81 | 730 to 790 | 760 | 3,500 to 3,780 | 3,596 |
| refills per filled chronic order | 1.80 | 1.69 | 1.81 | 1.79 | 2.03 | 2.00 |
| chronic orders with 1 / 2 / 3 / 4 fills | -- | 6 / 4 / 37 / 1 | -- | 38 / 34 / 330 / 22 | -- | 127 / 149 / 1,122 / 401 |
| refill interval, days (min / median / max) | 28 / 31.5 / 35 | 28 / 32 / 35 | 28 / 31.5 / 35 | 28 / 31 / 35 | 28 / 31.5 / 35 | 28 / 31 / 35 |
| chains stopped by the tail | about 43 | 37 | about 370 | 345 | about 1,580 | 1,441 |
| chains stopped by the draw | about 6 | 11 | about 49 | 74 | about 210 | 342 |
| chains stopped by an end | 0 | 0 | 0 | 0 | 0 | 0 |
| chains stopped because a later order replaced theirs | not predicted | 0 | not predicted | 5 | not predicted | 16 |
| pharmacy steps carried to a survivor | 4 | 11 | 48 | 78 | 159 | 252 |
| dispenses whose subject is the survivor of the order's patient | -- | 17 | -- | 117 | -- | 470 |
| of them inpatient | 1 | 1 | 0 | 0 | 2 | 2 |
| of them refills | -- | 12 | -- | 70 | -- | 311 |
| fills carrying an `:encounter-id` (community) | 0 | 0 | 0 | 0 | 0 | 0 |
| last event, days | 7,392.52 | 7,392.52 | 7,353.10 | 7,353.10 | 7,394.74 | 7,394.74 |
| events | about 29,750 | 29,741 | about 127,550 | 127,549 | about 506,200 | 506,195 |

The prediction held on the counts and was wrong on the split of
causes: every fill takes the draw, the last one the tail allows
included, so more chains end on the draw than a one-draw-per-chain
count gives. And a chain can stop a fourth way, when the patient
returns and is prescribed the same thing again.

**The tail lengthens no dense log.** `dense-7500` already runs twenty
years past its last arrival (`:persons {:years 20}`); its arrival
clock is half a day at 750 and twenty days at 28,024. The prompt
expected the log's tail to hold refills only. It holds births,
admissions, visits and demographic changes, and the refills sit inside
its first 110 days.

**The erasure law** holds: delete every dispense, renumber, and the
keyless log is left, on the gated test and on dense at 750 and 7,500
(with the order events' `:chronic` set aside, since dense's config now
states it).

**Oracle roots.** Predicted to move: the three that hold a dispense
(`dispense`, `osteoporosis-dispense`, `dispense-inpatient`), for the
fill number, and `osteoporosis`, whose 156 orders gain `:chronic
true`; possibly `asthma` and `metabolic-syndrome-care`. Measured, both
instruments with `--declared-digest-change`: exactly the first four.
The other 42 roots are identical, which is the proof that without the
key and without a dispense or a marked order a log is the log it was.
`osteoporosis-dispense` reached no refill on the default tail (its
module writes orders across a hundred years, and the tail is measured
from an arrival clock six days long), so it now states
`:refill-tail-days 36500` and is the one root reaching a refill: 134
first fills, 2,129 refills, 3,343 events.

**Describe 1.4.0** counts both new things as measures, with the
mechanism the catalog already had: `:medication-refill` and
`:dispense-after-merge`. At 7,500 they read 760 and 117; at 28,024,
3,596 and 470.

The new 28,024 log is 190,088,591 bytes, sha-256
`dd4516c020a3b0730425e1d3bab02840f2d5d65f9e19a4ede077c873f286a42e`;
`bd4dec16...` is kept in `figures.edn` under `:pre-adr-0191`.

### Downstream

Event schema **1.13.0**. Corpora that move: every corpus holding a
dispense (each gains `:fill-number`), every corpus holding an order a
module marks chronic (each such order gains `:chronic true`), and
every corpus generated with `:dispense` and a chronic order (refills).
A retained corpus is a valid artifact of the earlier pin, at 1.12.0.

### Owed next

- **Refills for chronic orders that predate the horizon.** A patient
  who arrives already on a long-term medication has an order that is a
  registration fact, with no marker and no fill.
- **A 90-day supply** as a second interval.
- **A non-uniform first-fill lag** (carried from ADR-0190).
- **The pathway drug codes** (carried).
- **Prescription detail** (dose, duration, the module's own refill
  count) is still dropped at the loader.
- Found here: **the tail is measured from the arrival clock**, so in a
  run with a long module horizon an order written years in is never
  refilled on the default. A bound measured from each order would not
  have that property; not ruled.
- Found here: **an expired record absorbed by a merge** loses its
  `:expired` status to `:merged`, so a fill carried from it is not
  declined for death. Not reached in any cell measured.
- Carried: the mutation column for the dispense.

### What this does not do

- Renders no dispense on the wire, and no refill.
- Marks no authored order chronic but `dense-clinic-chronic`'s.
- Starts a chain from an authored `:medication-dispense`.
- Enforces the `Facility` schema at run time.

### Addendum, 2026-11-22: the tail is optional and absent by default

**The re-ruling.** Author, verbatim, 2026-11-22: "Have the prompt
execute on the tail re-ruling.", the re-ruling being the design
channel's: "Make the tail optional and off by default, so a chain
stops on the continuation draw or the order's end. ... The tail stays
available as an option for a scenario that wants a hard cutoff." The
interval and the continuation chance stand as ruled on 2026-11-20.

**The premise that was false.** The tail was ruled on 2026-11-21 on
the expectation that a dense log spans about three weeks, so that
refills past the arrivals would sit in an otherwise empty tail and
would need a bound to end the run. This ADR's own session measured
otherwise the same day: the arrival clock is 19.6 days at 28,024, and
the log runs twenty years. So the tail lengthened nothing. It confined
every refill to the first 110 days, stopped 1,441 of 1,799 chains
itself where the draw stopped 342, and over a long module horizon left
an order written years in with no refill at all, which is why
`osteoporosis-dispense` had to state 36,500 days to show one.

**What changed.** Decision 4's third row and decision 5 now read:

| key | meaning | default |
|---|---|---|
| `:refill-tail-days` | a hard cutoff: no refill is queued later than the last arrival plus this many days; a non-negative integer | none; absent means no cutoff |

`default-refill-tail-days` is gone; `sim-model/refill-tail-days`
returns the facility's own value or nil; nil bounds nothing. A stated
tail means exactly what it meant. The two draws are still taken first
and the bound is tested after them, so no chain's draws move. The
event schema is untouched (1.13.0), and so are the describe report and
the stream scheme. `dense-7500` drops the key and keeps the other two
written out; `osteoporosis-dispense` drops its 36,500.

**What stops a chain now**, read from the code: the continuation draw;
the order's end; a newer order of the same citation; the patient's
death (an `:expired` subject is declined). A merge stops none, since
the pending refill is carried and a merged record always has a
survivor. The one gap named under "Owed next" stands: a record that
had expired before it was absorbed is `:merged`, so a refill carried
from it is not declined for the death. No dense cell reaches it:
nobody in those logs dies.

**Measured**, seed 20260824, `--churn`, predicted before any edit.

| | 750 predicted | 750 measured | 7,500 predicted | 7,500 measured | 28,024 predicted | 28,024 measured |
|---|---|---|---|---|---|---|
| refills | about 700 | 647 | about 6,510 | 6,805 | about 27,600 | 27,638 |
| refills per filled chronic order, mean | 14.5 | 13.48 | 15.4 | 16.05 | 15.3 | 15.36 |
| median | 11 | 12 | 11 | 12 | 11 | 10 |
| max | about 60 | 50 | about 95 | 122 | about 118 | 123 |
| chains stopped by the draw | 48 | 48 | 419 | 419 | 1,783 | 1,783 |
| by a newer order | 0 | 0 | 5 | 5 | 16 | 16 |
| by an end, a death or a merge | 0 | 0 | 0 | 0 | 0 | 0 |
| last refill, day | about 1,900 | 1,609 | about 3,000 | 3,923 | about 3,700 | 3,948 |
| last event, day | 7,392.52 | 7,392.52 | 7,353.10 | 7,353.10 | 7,394.74 | 7,394.74 |
| events | about 30,360 | 30,307 | about 133,300 | 133,594 | about 530,200 | 530,238 |
| dispenses after a merge | not predicted | 94 | not predicted | 904 | not predicted | 2,731 |

No chain outruns a dense log, and no log's last event moved. The
oracle: every root IDENTICAL on both instruments, `osteoporosis-dispense`
included, whose 36,500 days had bounded nothing.

Three things the measurement showed that the re-ruling did not say.

- **Dispenses after a merge are now common**: 2,731 at 28,024 against
  470. A chain carried to a survivor in its first weeks runs on there
  for a year or more, and every one of its refills names an order
  written under another record.
- **A patient's later prescription can move.** A patient has one
  `:pharmacy` stream. A chain that runs longer takes more of it, so a
  prescription the same patient is written afterwards draws its share
  and lag further down. One such fill moved at 7,500 and one at 28,024,
  where one prescription is also now filled that was not. Every other
  order's fills are a prefix of what they were, and the log less its
  dispenses is unchanged.
- **Without a tail a run ends when its chains do.** A continuation of 1
  on an order nothing ends does not finish. The schema accepts the
  combination; the config reference says to state a tail with it.

**Downstream.** Event schema 1.13.0, unchanged. Corpora that move:
every corpus generated with `:dispense` that holds a filled chronic
order and stated no tail. A corpus built between `516cf579` and this
change is a valid artifact of its pin and holds no refill later than
90 days after its last arrival.

**Still owed**, restated: a per-order bound (a tail counted from each
order, not from the arrival clock); refills for chronic orders that
predate the horizon; a 90-day supply; a non-uniform first-fill lag;
the pathway drug codes; prescription detail (dose, duration, the
module's own refill count). And from this addendum: a refusal, or a
warning, for a continuation of 1 with no tail; the expired record
absorbed by a merge.
