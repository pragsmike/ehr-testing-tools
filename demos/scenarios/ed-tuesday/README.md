# Scenario: ed-tuesday

A scripted single ED shift, weighted toward quick emergent
presentations -- the author's own 2026-08-10 direction (`notes/ADRs.md`
ADR-0103): *"Maybe weight the patient population toward immediate,
emergent conditions like trauma/injuries? This would simulate an
actual ED, which is where a lot of the activity and churn would
happen."* `--config` is [`config.edn`](config.edn); see that file's own
header comment for the full design rationale (the disjoint-cohort
shape that keeps the ambulatory module tail conflict-free, and the
facility surge-capacity bump).

**Contrast with [`../clinic-decade/`](../clinic-decade/README.md):**
clinic-decade is population-scale incidence -- twelve everyday-
ambulatory modules across 200 patients and a ten-year horizon,
genuinely sparse traffic (its own README: "most of a clinic-decade...
unfolds as intake and follow-up over months and years"). ed-tuesday is
day-scale and scripted -- five weighted ED pathways (admission,
workup, transfer, discharge) driving real inpatient census on a
`--board`, plus a thin, low-weight ambulatory module tail riding
alongside. Both are real, valid ways to populate this engine's
`:pathways`/`:module-assignment` surface; this pair is the A/B
contrast the redesign arc chartered.

Population-scale, no captured trace -- see this directory's own parent
[`README.md`](../README.md) for why. Generate the scenario's own
output first, then play it back.

## Generate

```bash
bin/ehrt corpus generate sim --seed 20260811 --patients 100 \
  --reference-date 2026-08-11 --churn \
  --config demos/scenarios/ed-tuesday/config.edn \
  --out-dir out/scenarios/ed-tuesday
```

`--out-dir` is rejected if it already exists and is non-empty -- remove
or rename a prior run's own directory before regenerating.

## Play

```bash
bin/ehrt play out/scenarios/ed-tuesday --board 60 --rate 10000000
```

Renders a bed-state snapshot every 60 stream-minutes instead of a
message-by-message ticker (`ehrt play`'s own `--board`), at 100,000
stream-seconds per wallclock-second.

Or play the sim's own story directly, from its own event log rather
than the emitted HL7 v2 messages:

```bash
bin/ehrt play out/scenarios/ed-tuesday/events.edn --rate 10000000
```

## What to look for

Witnessed 2026-08-28 (seed 20260811, 100 patients, `--config` as
above, `--churn` on): **1,269 ground-truth events, 1,447 HL7 v2
messages, 574 board snapshots** over a 630,342,955,000 ms stream span.

**THE EVENT COUNT DID NOT MOVE AND THE MESSAGE COUNT NEARLY DOUBLED,
and that is the whole of what arc 4 is.** `config.edn` opted this
scenario into RE-STATEMENT CHATTER and DFT^P03 CHARGES on 2026-08-28
(arc 4 sweep 2, ADR-0175 designs (a) and (c)), and neither reaches the
engine: both are EMISSION config, so 1,269 is the same 1,269 this
scenario produced the day before. What changed is how much of it the
WIRE carries. A demographic change and a coverage change used to reach
a consumer only inside the PID or IN1 of some LATER message; each now
gets a message of its own -- **288 ADT^A31** person-scoped updates and
**135 ADT^A08** visit-scoped ones (the split is derived from whether
the change fell inside an open encounter, never configured). Every
registration now sends an **ADT^A28** (116 of them), and every
encounter close sends a **DFT^P03** carrying its own charge lines (126
of them, 167 FT1 lines between them). The A08 half comes almost
entirely from the periodic re-statement of open encounters rather than
from the event-driven half -- demographic churn happens between visits,
not during them -- which is why `config.edn` carries a
`:rate-per-patient-day` at all.

**PATIENTS BOOK NOW, AND SOME DO NOT COME.** `config.edn` opted this
scenario into SCHEDULING on 2026-08-27 (arc 3b sweep 3, ADR-0174
section 2(b) and ruling C), and that is where these figures moved from
the previous witness's 1,151/739/128. Arrivals split scheduled-vs-
walk-in; a booking can be moved, cancelled or no-showed; and a
discharge can book a return visit. This run holds **44 appointments, 5
rescheduled, 3 cancelled and 5 no-showed**, with **35 encounter openers
naming the appointment they were kept against**.

**TWENTY-ONE OF THOSE ARE SECOND ENCOUNTERS THAT WERE SCHEDULED** --
the 21 `:outpatient-visit` / `:outpatient-visit-end` pairs above, each
booked at its own patient's discharge and each carrying its
appointment's id. No corpus in this repository could hold a second
encounter at all before arc 3b sweep 1 lifted the encounter horizon,
and none held one produced BY BOOKING before this sweep.

**NONE OF THE FOUR SCHEDULING KINDS REACHES THE WIRE**, which is why
the event count rose by 118 while the message count rose by only 43.
They map onto the SIU family, which is v2.4 structure. Until
2026-08-27 that was a VERSION problem -- every message here carried
MSH-12 `2.3`, and emitting a structure the version field disclaims
would be worse than emitting nothing. Arc 4 sweep 1 (`notes/adr/
0175-arc-4-emission-add-ons.md` ruling A1) declared `2.4`, so the
version objection is gone and what remains is simply work not yet
done: the SIU entries are arc 4 sweep 4's. The appointments are still
in `events.edn` and nowhere in the HL7.

**BEDS TAKE TIME TO TURN OVER.** `config.edn` opted this scenario
into the BED-STATUS CYCLE on 2026-08-27 (arc 3b sweep 2, ADR-0174
section 2(c) and ruling C). A vacated bed is no longer free the
instant its occupant leaves: it goes `:dirty`, then `:cleaning`, then
`:ready`, and the allocation ladder will not hand out a bed that is not
ready. **421 of this run's messages are ADT^A20 bed-status updates** --
one per transition, over 141 turnovers -- and they were the reason the
message count nearly doubled while the clinical traffic barely moved.
(They are no longer the largest single family: chatter's 288 A31s and
135 A08s together outnumber them since 2026-08-28.)

**Every bed-ready transfer used to fire in the same second as the
discharge that vacated the bed. None does now.** The relief of a
boarder is decided at the bed's own READY instant instead, against the
board as it stands then -- which is a more honest picture of a real
hospital than a bed changing hands with no gap at all.

**PATIENTS COME BACK NOW.** `config.edn` opted this scenario into the
lifted ENCOUNTER HORIZON on 2026-08-26 (arc 3b sweep 1, ADR-0174 ruling
A1), and that is where these figures moved from the previous witness's
695/286/104. Until that day every patient in every corpus this repo had
got exactly ONE encounter, ever -- `check.clj`'s own
`admission-only-when-new` was that horizon written as an invariant, and
a returning person's arrival simply queued nothing. This run now
carries **147 encounter openers across 111 patients: 30 patients with
more than one, 36 encounters the pre-sweep engine threw away, and a
maximum of THREE on a single patient** -- figures that were unchanged
by the bed cycle, which moves WHEN a bed changes hands and not who is
admitted, and that MORE THAN DOUBLED with the scheduling opt-in of
2026-08-27 (from 127/116/14/15). Scheduling is where the second
encounters now mostly come from: 21 of those 36 are follow-up visits
booked at a discharge, which is a different producer from a returning
person walking in again. Same patient, same patient-id,
same MRN each time -- which is the whole point, because an MPI under
test has to see the same MRN twice.

**And every message now carries a visit number.** PV1-19 was EMPTY on
every message this project had ever produced; it now renders the
encounter's own `ENC-` id (ADR-0174 ruling C1). 630 of this run's 631
PV1 segments carry one. The ONE that does not is an ORU^R01 result
arriving after its patient's discharge -- the pending-labs-at-discharge
case, which belongs to no open encounter and correctly says so. (The
blank count was five before the bed cycle reshuffled this run; four of
those five messages now fall inside an open encounter instead.)

Note that the A20 bed-status messages carry NO PV1 at all -- an
`ADT^A20` is `[MSH EVN NPU]`, no PID and no PV1, because a bed that
nobody is in has no patient to name, and an ADT^A31 or ADT^A28 carries
none either -- a person-scoped update names no visit. That is why 1,447
messages carry only 631 PV1 segments between them.

**THE SHIFT IS STILL A SHIFT; THE STREAM IS NOT.** `config.edn` opted
this scenario into `ehrt.sim-engine.engine`'s demographic fold on
2026-08-26 (arc 3a part 4, ADR-0173 ruling D1's commit 2), so the run
now carries a POPULATION of 200 people walking twenty years alongside
its hundred scripted arrivals. The scripted ED shift is untouched and
still happens in the first ~35 hours; what follows it is those people's
own lives -- moves, coverage changes, births, occupational injuries and
unidentified arrivals -- spread across two decades. So the run has TWO
PHASES now, and the day-scale contrast this scenario draws with
clinic-decade is about the first one. Read the board with that in mind:
the busy shift is the opening minute of the playback, and the long
sparse tail after it is the population.

**Inpatients rise and fall.** The first snapshot already shows 4
occupied beds; the census climbs through the shift to a peak of 14
concurrent inpatients, then drains to 1 by the run's own last snapshot.
The peak moved 11 -> 15 with the horizon lift, for the obvious reason
(a returning patient occupies a bed the second time too), and 15 -> 14
with the bed cycle, for a less obvious one: the reshuffle moves which
patients overlap, and a bed held out of service for housekeeping is a
bed nobody is admitted into.

**And the board now shows beds nobody is in.** Before this scenario
took the bed cycle, an empty bed was INVISIBLE on the whiteboard --
a room being turned over looked exactly like a room standing free.
The A20 stream fixed that: a `(dirty)` or `(cleaning)` line appears
under its ward for as long as housekeeping has it, and 43 such lines
render across this run's snapshots (15 `(dirty)`, 28 `(cleaning)`).

```
-- board snapshot: 2026-08-11T09:00:00Z --

Emergency:
  ED-H01  Lee, Sophia  MRN MRN000029  inpatient  attending: 5761303028
  ED-H04  Thomas, Jessica  MRN MRN000028  inpatient  attending: 5761303028
  ED-H11  Patel, James  MRN MRN000030  inpatient  attending: 5761303028
  ED-H12  Smith, Michelle  MRN MRN000027  inpatient  attending: 5761303028
  ED-H13  Gonzalez, Emily  MRN MRN000015  inpatient  attending: 5761303028
  ED-H02  (cleaning)
  ED-H07  (dirty)
  ED-H10  (dirty)

Renal:
  RENAL-04  Wilson, Jessica  MRN MRN000024  inpatient  attending: 5761303028

inpatients: 6  active outpatients: 0  discharged: 17  merged: 0
```

A `:ready` bed is still not listed. An available bed is the normal
case, and listing every one of them would bury the two states a charge
nurse is actually looking for.

```
-- board snapshot: 2026-08-11T01:12:00Z --

Emergency:
  ED-H05  Patel, Lisa  MRN MRN000004  inpatient  attending: 5761303028
  ED-H09  Hernandez, Sandra  MRN MRN000002  inpatient  attending: 5761303028
  ED-H14  Johnson, Christopher  MRN MRN000003  inpatient  attending: 5761303028

inpatients: 3  active outpatients: 0  discharged: 1  merged: 0
```

```
-- board snapshot: 2046-08-01T15:15:55Z --

Emergency:
  ED-H11  Lee, Jennifer-1  MRN MRN000040  inpatient  attending: 5761303028

inpatients: 1  active outpatients: 21  discharged: 88  merged: 1
```

Note the DATE on that second snapshot: 2046, not 2026. That is the
population tail, not the shift. (It reached 2045 before 2026-08-28: the
chatter opt-in put A31 person-scoped updates on the wire out to the far
end of the twenty-year demographic horizon, so the STREAM now runs
nearly a year longer than the messages that used to end it.)

**Discharges accrue and churn fires.** `discharged` climbs from 1 to
88 across the run; `merged` climbs from 0 to 1. That merge is now an
IDENTIFICATION merge rather than an `InjectChurn` one -- a John Doe
record joined to the patient the same person already had -- and churn's
own bed-merge lottery contributed none at this seed. Stated rather than
smoothed over: the claim this supports is that merges happen here at
all, and they do, but the family that produces them has changed.

**The person stream's own clinical traffic**, counted: **15
unidentified ED arrivals**, every one of them later filled in place
(PID-5 `Doe^Unknown`, PID-7/8/11/13 empty, no IN1); **15 newborns**,
each on a patient of their own whose `:registered` carries
`:mother-patient-id`; **8 occupational-injury presentations**; **1
parent delivery admission**; and **3 registrations for a patient with
nowhere to live** (PID-11 absent). Every one of those five counts is
UNCHANGED by the horizon lift, which is the right answer: the person
stream runs upstream of the encounter, so lifting the horizon adds
return VISITS and not people.

Still only one parent delivery, and the reason changed. It used to be
the single-encounter horizon. That horizon is gone, and the remaining
constraint is the STATIC one it always sat behind: a hook may put an
encounter only on a patient whose own compiled and authored queue is
otherwise empty (`prelude`'s `clinically-idle?`), and this scenario's
patients nearly all walk a scripted ED pathway. Contrast
`../clinic-decade/`, whose patients mostly walk nothing: its parent
deliveries went 17 -> 27 on exactly this change.

**Capacity held.** No `:capacity-exhausted` at any point in this
run -- `config.edn`'s own facility bump (Emergency's surge slots,
6 -> 16 over `sim-model/default-facility`) and the pathway pool's
own dwell-time tuning (see that file's header) were live-probed
against exactly this seed/population before landing; peak concurrent
Emergency + Renal + Cardiology occupancy never approached either
ward's own capacity.

**The ambulatory module tail: zero live encounters, disclosed, not
retuned away.** The 8 explicitly-assigned module-tail patients
(`sore_throat`/`sinusitis`/`bronchitis`/`ear_infections`, ordinals 6,
18, 30, 42, 54, 66, 78, 90) produced ZERO live `:outpatient-visit`
events in this run -- `active outpatients` reads 0 in every snapshot
above. This was live-probed, not assumed: the same 8-patient tail was
re-run at `:module-horizon-days` 14, 90 (the value actually shipped),
and even clinic-decade's own 3650, and produced a live encounter at
only ONE of those three horizons (3650 days: exactly 1 of 8). The
mechanism is disclosed in `config.edn`'s own header comment, grounded
in `sore_throat.json`'s own source (`Potential_Infection`'s
monthly-Delay-gated ~0.5-1% onset probability) -- these modules are
genuinely low-incidence per patient, the same shape clinic-decade's own
README already discloses at population scale. A thin, low-weight tail
at a genuinely short (day/week/month-scale) horizon is expected to
show sparse-to-zero live content; this run's own zero is that expected
outcome, not a config defect.

Full closing summary: `{:unparseable-count 0, :snapshot-count 574,
:skip-count 0, :rate 1.0E7, :idle-cap-ms 5000, :wallclock-ms 64855,
:stream-span-ms 630342955000, :clamped-count 0, :emitted 1447,
:unfolded-count 0, :sink "ticker"}`.

(RE-WITNESSED 2026-08-28, and the previous value was STALE rather than
merely superseded: it read `:snapshot-count 104 ... :emitted 286`, a
corpus size this scenario has not produced for several sweeps. Nothing
gates the transcript excerpts in this file, and three board snapshots
above had drifted the same way -- two of them naming instants that no
longer existed in any run. All of them are re-witnessed here against
one fresh execution of this file's own commands.)

**`--rate` MOVED, 100,000 -> 10,000,000**, on both scenarios and for
the same measured reason: `--rate` is stream-seconds per
wallclock-second, so it has to be read against the stream it paces, and
this one grew from 35 hours to twenty years. At the old rate the
playback spent its time waiting; at the new one the whole stream plays
in ~65 seconds with no skips at all -- the chatter opt-in filled in so
much of the twenty-year tail that the player's own idle cap never
fires. The board census is unchanged by the
rate -- the number paces the demo and changes nothing it shows.

**Same ground truth, a second, latency-realistic wire.** This shift's
own ground truth is also played onto a wire where messages transmit
late, the way a real EHR's own downstream feed does -- see "The second
clock" below.

## The second clock

The author's own charter (`notes/ADRs.md` ADR-0107, 2026-08-11, quoted
in full in ADR-0109/ADR-0110): *"lab results take time to come back,
providers take time to log things in the EHR, etc. so it's possible
that a downstream receiver of the HL7 traffic will have incomplete
encounter records for some time. That's not our problem to solve, but
in order to test that such downstream receivers handle it properly
(whatever that might mean for them) we need to supply them with such
cases."* ADR-0109 built the mechanism (`emit-hl7/plan-latency` +
`emit-hl7/emit-wire`, a second, independently-seeded RNG sampling
per-event-type transmit delays, `sim-model/config.clj`'s own
`LatencyProfile`). This section is that mechanism's demo:
[`config-latency.edn`](config-latency.edn) -- byte-identical to
[`config.edn`](config.edn) below the header, plus one added `:latency`
block -- generated at the *same* seed as the shift above, played into
this workspace's own `--board` as the downstream-receiver stand-in.

**Generate both, same seed, separate out-dirs:**

```bash
bin/ehrt corpus generate sim --seed 20260811 --patients 100 \
  --reference-date 2026-08-11 --churn \
  --config demos/scenarios/ed-tuesday/config.edn \
  --out-dir out/scenarios/ed-tuesday-base

bin/ehrt corpus generate sim --seed 20260811 --patients 100 \
  --reference-date 2026-08-11 --churn \
  --config demos/scenarios/ed-tuesday/config-latency.edn \
  --out-dir out/scenarios/ed-tuesday-latency
```

**Ground truth is invariant.** `:latency` never reaches
`engine/config-keys` (ADR-0109) -- it rides `:config` as an emit-only
passthrough, the same way `:site-profile` does. Witnessed directly,
not merely asserted:

```
$ diff out/scenarios/ed-tuesday-base/events.edn out/scenarios/ed-tuesday-latency/events.edn
$ sha256sum out/scenarios/ed-tuesday-base/events.edn out/scenarios/ed-tuesday-latency/events.edn
fe13a7ba59939e548be8d98589b005ff7c14e33ef8e82d4d54d47ad388bbb8d8  out/scenarios/ed-tuesday-base/events.edn
fe13a7ba59939e548be8d98589b005ff7c14e33ef8e82d4d54d47ad388bbb8d8  out/scenarios/ed-tuesday-latency/events.edn
```

`diff` reports no differences; the digests match exactly -- the same
1,269 ground-truth events either way. THIS IS WHY `config-latency.edn`
CARRIES `:scheduling` TOO, value for value: the split's own two draws
per arrival ordinal are `:world`, so a different `:scheduled-fraction`
in one file would reshuffle its arrivals away from the other's and
break exactly the identity this block witnesses. IT ALSO CARRIES
`:chatter` AND `:charges` VALUE FOR VALUE SINCE 2026-08-28, but for a
weaker reason that is worth separating: those two are EMISSION config
and reach `engine/run` never, so no value in them could move a
ground-truth byte even if the two files disagreed. They are copied so
the two files describe one scenario, not because the identity above
depends on it. (That figure read `375` until
2026-08-27: it was witnessed before ADR-0171's stream partition and
nothing kept it true -- one of the stale tokens
`.agents/plans/roadmap.md#post-partition-narrative-refresh` counts in
this file, corrected here rather than left because this sweep had to
re-witness the line above it anyway.) Only the *rendering* differs: the
`msg-NNNN.hl7` files carry different MSH-7 values and a different file
order (`emit-wire` sorts by transmit time, not log order) between the
two out-dirs -- the palgebra's own `GT -> TimedWire` arrow
(`docs/dev/simulator-architecture.md` section 5), visible in a diff of
two directories generated from the same seed.

**Play the latency wire into the board:**

```bash
bin/ehrt play out/scenarios/ed-tuesday-latency --board 60 --rate 10000000
```

**What the board actually shows.** Patient MRN000095 (Gonzalez,
Olivia), bed `ED-H13`: admitted (EVN-2 clinical time
`2026-08-11T23:11:00Z`), discharged 31 minutes later (`23:42:00Z`) --
ordinary, unremarkable, log-order-correct clinical history. On the
*latency* wire her three messages transmit in an order none of them
was written in: the TRANSFER (A02) first at MSH-7 `23:59:29Z`, the
DISCHARGE (A03) second at `2026-08-12T00:21:08Z`, and the ADMISSION
(A01) last at `00:25:22Z` -- reordered on the wire, never in ground
truth. The board, folding messages in the order it receives them:

```
-- board snapshot: 2026-08-12T00:01:02Z --

Cardiology:
  CARDIOLOGY-02  Smith, Michelle  MRN MRN000027  inpatient  attending: 5761303028

Emergency:
  ED-H10  Miller, Robert  MRN MRN000096  ?
  ED-H14  Gonzalez, Olivia  MRN MRN000095  ?  attending: 5761303028
  ED-H16  Johnson, Matthew  MRN MRN000092  inpatient  attending: 5761303028
  ED-H13  (cleaning)

Renal:
  RENAL-01  Brown, Richard  MRN MRN000082  inpatient  attending: 5761303028
  RENAL-02  Garcia, Lisa  MRN MRN000081  inpatient  attending: 5761303028
  RENAL-03  Nguyen, James  MRN MRN000020  inpatient  attending: 5761303028

inpatients: 5  active outpatients: 0  discharged: 52  merged: 0
-- board snapshot: 2026-08-12T01:16:00Z --

Cardiology:
  CARDIOLOGY-02  Smith, Michelle  MRN MRN000027  inpatient  attending: 5761303028

Emergency:
  ED-H09  Hernandez, William  MRN MRN000097  inpatient  attending: 5761303028
  ED-H11  Taylor, Jennifer  MRN MRN000035  inpatient  attending: 5761303028
  ED-H13  Gonzalez, Olivia  MRN MRN000095  inpatient  attending: 5761303028
  ED-H16  Johnson, Matthew  MRN MRN000092  inpatient  attending: 5761303028

Renal:
  RENAL-01  Brown, Richard  MRN MRN000082  inpatient  attending: 5761303028
  RENAL-02  Garcia, Lisa  MRN MRN000081  inpatient  attending: 5761303028

inpatients: 6  active outpatients: 0  discharged: 54  merged: 0
```

Read the FIRST snapshot's `ED-H14` line: Gonzalez is on the board with
a `?` where her patient class should be, and in the wrong bed. That is
the A02 transfer arriving alone -- `fold-message` bootstraps an entry
from whatever message it first sees, and a transfer carries a location
but no admission, so the board knows where she is and nothing about
what kind of patient she is. `ED-H13`, the bed she is actually in, is
`(cleaning)` in the same snapshot: the bed cycle's own view of her
stay has already moved on.

Then the DISCHARGE arrives at `00:21:08Z` and folds her to
`:discharged` -- and the ADMISSION arrives four minutes later, at
`00:25:22Z`, where `fold-message`'s own `:admission` case applies
UNCONDITIONALLY (ADR-0109's Step 5 finding, live here rather than
probed). The second snapshot is the result: she is `inpatient` in
`ED-H13`, a bed she vacated an hour and a half of clinical time
earlier, and the `discharged` tally has gone 52 -> 54 -> 53 across
three snapshots as the board un-discharges her. Her phantom entry never
clears -- her own later A31 restatements say nothing about the visit --
and she is still on that board at the run's own last snapshot, in
**2046**, twenty years of stream time later. The *same* patient in the
base (no-latency) run above appears exactly once, admitted and never
seen again once discharged -- the entire disorder is the wire's doing,
not the ground truth's.

**RE-WITNESSED 2026-08-28**, and this one is a CORRECTION rather than a
reshuffle. The paragraph above named MRN000005 (Johnson, Michael) with
transmit times `02:22:36Z` and `02:37:37Z` on 2026-08-11; that patient's
own messages are on 2026-08-14 and in the right order, and have been
for at least one sweep. Nothing gates a transcript excerpt in this file,
so the illustration had drifted off the run it claims to describe.
Arc 4 sweep 2 changes no base message's bytes or transmit times at all
-- proved directly, by generating this corpus with and without the
add-ons and diffing the 782 non-add-on messages -- so the case below was
re-derived from the live wire rather than adjusted. The cast is now
MRN000095 in `ED-H13`, and the shape is RICHER than either previous
witness: three messages, arriving transfer-first, discharge-second,
admission-last.

This is one of **5 (of 111 admitted patients, seed 20260811)** whose
own admission message arrives after its own transfer or discharge
message on this wire -- occasional and visible, not universal
(`config-latency.edn`'s own header has the tuning rationale).

**RE-WITNESSED 2026-08-27: 3 of 110 became 5 of 112**, and the reason
is the bed cycle, not the wire. Its opt-in reshuffles which patients
walk which pathway and shifts every attending draw, so the set of
admission-to-discharge gaps tight enough for a transmit delay to
overtake is simply a different set. The denominator moved for the same
reason: two more patients get an admission.

**RE-WITNESSED 2026-08-26: 8 of 92 became 3 of 110.** The
demographic-fold opt-in added 18 more admitted patients, and every one
of them is a HOOK encounter spread across the population's twenty years
-- a birth, an injury, an unidentified arrival -- whose own
admission-to-discharge gap is hours or days. `config-latency.edn`'s
bands are 15 to 90 minutes, so a gap that wide cannot be crossed by a
transmit delay and those encounters simply cannot disorder. The ones
that do are the scripted ED shift's own, where the gaps are tight
enough for the wire to overtake the clinic. The mechanism is unchanged
and so is the claim; what changed is that the denominator contains
patients the mechanism cannot reach, and saying "5 of 112" without
saying which 112 would understate it.

Closing summary: `{:unparseable-count 0, :snapshot-count 615,
:skip-count 0, :rate 1.0E7, :idle-cap-ms 5000, :wallclock-ms 64832,
:stream-span-ms 630342955000, :clamped-count 0, :emitted 1447,
:unfolded-count 0, :sink "ticker"}` -- the same 1,447 messages as the
base run, FORTY-ONE more snapshots than it (615 against 574), and a
stream span identical to it to the millisecond. The span figure used to
differ between the two runs, in one direction or the other across four
witnesses; it no longer does, and the reason is arc 4 rather than a
correction. The first and last messages of this stream are now an A28
at the very start and an A31 at the far end of the twenty-year
demographic horizon, and neither carries a latency offset -- chatter
mints its own control ids, which no `:latency` profile keys on -- so the
two wires now begin and end at the same instant and differ only in
between. THAT is where the claim lives: the same ground truth, played on
a delayed wire, produces a different board.

**What a receiver could do better.** Nothing here is prescribed or
built this session -- as reader orientation only: a receiver that
buffered incoming messages briefly and reconciled by clinical time
(EVN-2, when present) rather than folding strictly in arrival order
would not have produced Johnson's own phantom re-admission. Whether, or
how, to do that is the receiver's own design question -- this
workspace's job is only to supply the case (the author's own charter,
quoted above), not to fix `fold-message` (ADR-0109's own named scope
fence, unchanged here).

## Batched delivery

ADR-0111 lands a second, complementary transport realism: real EHR
feeds are rarely delivered message-by-message forever -- most
interfaces batch their traffic on a schedule (hourly, nightly) using
HL7 v2's own batch protocol, BHS/BTS segments wrapping a block of
messages with a declared message count. `ehrt corpus batch` is a
corpus-level tool, deliberately separate from the sim (author ruling,
2026-08-11, Q1 a: *"It should work on any corpus, even an existing
directory of foreign (but valid) message files."*) -- it happens to run
over this scenario's own latency out-dir below only because that
out-dir is a directory of valid v2 messages like any other.

**Run the batcher over the SAME latency wire the section above
generates, hourly:**

```bash
bin/ehrt corpus batch out/scenarios/ed-tuesday-latency --interval 60 \
  --out-dir out/scenarios/ed-tuesday-latency-batches
```

Witnessed 2026-08-28 (same seed-20260811 run as above, 1,447 messages
across 615 occupied hourly buckets, `2026-08-11T00:00Z` through
`2046-08-01T15:00Z`). The bucket count moved 186 -> 615 with the
CHATTER and CHARGES opt-ins, and this is the largest single move it has
taken. The mechanism is the bed cycle's rather than scheduling's --
this really is more traffic, 782 messages to 1,447 -- but with a twist
neither earlier sweep had: most of the new messages are PERSON-level.
An A31 restating a residence move fires wherever the person process put
that move, anywhere across twenty years, so the new traffic lands
overwhelmingly in hours that had carried nothing at all rather than
thickening the hours of the shift. The closing bucket moved out with it,
from 2045 to 2046, for exactly the same reason.

Earlier moves, kept: 106 -> 135 with the bed cycle (A20 traffic in
previously empty hours), and 135 -> 186 with SCHEDULING, which did not
add much traffic (43 more messages) but MOVED it -- a lead time
displaces an arrival's whole encounter by days.

```
{:status :ok,
 :payload
 {:out-dir "out/scenarios/ed-tuesday-latency-batches",
  :interval-ms 3600000,
  :batches
  [{:file "batch-000.hl7", :count 9,
    :start-ms 1786406400000, :end-ms 1786410000000, :verified true}
   {:file "batch-001.hl7", :count 14,
    :start-ms 1786410000000, :end-ms 1786413600000, :verified true}
   {:file "batch-002.hl7", :count 16,
    :start-ms 1786413600000, :end-ms 1786417200000, :verified true}
   ;; ... batch-003.hl7 through batch-613.hl7 ...
   {:file "batch-613.hl7", :count 2,
    :start-ms 2400498000000, :end-ms 2400501600000, :verified true}
   {:file "batch-614.hl7", :count 1,
    :start-ms 2416748400000, :end-ms 2416752000000, :verified true}],
  :span {:earliest-ms 1786406400000, :latest-ms 2416752000000}}}
```

**Epoch-aligned, and the interior gaps are enormous.** The first thirty
or so batches are the ED shift itself, packed hour after hour with 4 to
41 messages each. Everything after them is the population tail, and
they are almost all `:count 1` or `:count 2` -- one birth, one injury,
one unidentified arrival, one residence move restated as an A31, in an
hour that carried nothing else, years apart from the batch before it.
`batch-599` onward sit in 2045 and 2046. The empty hours between are
simply ABSENT, never written as empty files (ADR-0111's own named v1
deferral: an interior empty batch is not represented, only skipped),
which is why twenty years partition into 615 files rather than 175,000.
Every one of the 615 written files self-verified:
`write-and-verify-batch!` (`bases/cli`) decodes what it just wrote
straight back and checks `BTS-1` against the real message count before
ever reporting success -- `:verified true` on all 615 is that check,
exercised, not merely claimed.

**The wrapper itself**, `batch-000.hl7`, head and tail:

```
$ head -c 100 out/scenarios/ed-tuesday-latency-batches/batch-000.hl7
BHS|^~\&

MSH|^~\&|EHR-TESTING-SIM|SIM|||20260811000000+0000||ADT^A28|MRN000001-A28-0-0|P|2.4EVN|A2
$ tail -c 45 out/scenarios/ed-tuesday-latency-batches/batch-000.hl7 | cat -A
mergency^^ED-H08^general-hospital|U^M$
$
BTS|9$
$
```

`BHS|^~\&` opens the batch; `BTS|9` closes it, `BTS-1` naming the true
count of 9 messages this file actually carries. Note what the FIRST of
those nine is now: an `ADT^A28`, MSH-10 `MRN000001-A28-0-0`. Before the
chatter opt-in the batch opened on MRN000001's admission; it now opens
on that patient's REGISTRATION, which is what a real feed sends first
and what this project never used to send at all. The ordinal suffix
`-0` is the control-id shape chatter mints so two re-statements of one
patient at one instant cannot collide. Note the LAST of the nine too:
an `NPU` segment ending `|U` -- an ADT^A20 reporting bed `ED-H08`
unoccupied and ready again. A bed-status update is an ordinary
message to the batch protocol, which is the point of rendering the
cycle onto the wire at all -- the minimal,
deterministic field set ADR-0111 rules for v1 (no creation-time field
populated at all, so the determinism law -- no wall clock anywhere --
holds trivially rather than by threading one through).

**A straddling encounter.** MRN000002 (Hernandez, Sandra, bed ED-H09):
admitted (A01, MSH-7 transmit time `2026-08-11T00:37:39Z`) lands in
`batch-000.hl7`; discharged (A03, MSH-7 transmit time
`2026-08-11T02:10:37Z`) lands in `batch-002.hl7` -- TWO clock-hours
later, skipping a whole batch in between. (It was the very next batch
until 2026-08-27; the bed cycle's reshuffle redrew this discharge's own
transmit delay and pushed it a bucket further out, which makes the
point harder rather than softer. The scheduling opt-in later that day
redrew the same delay again -- 02:13:46Z became 02:10:37Z -- and the
STRUCTURE survived it: still three windows, still nothing for this
patient in the middle one. The admission's own 00:37:39Z has not
moved through any of it. IT VERY NEARLY DID NOT SURVIVE 2026-08-28:
this encounter's new DFT^P03 landed in `batch-001.hl7` -- the middle
window -- until arc 4 sweep 2 found that a DFT was looking its latency
offset up under its own control id, finding none, and transmitting at
its clinical instant while the ADT^A03 it accompanies lagged 46
minutes behind it. Fixed at the source rather than worked around: the
DFT now rides its basis event's own offset, so both messages of that
close land together in `batch-002.hl7`, and this patient's own new
ADT^A28 registration joins the admission in `batch-000.hl7`. Two
messages in the first window, two in the third, still nothing at all in
the middle one.) A downstream receiver holding
`batch-000.hl7` AND `batch-001.hl7` still has that admission and
nothing else for that patient: by every transport-level measure it is
looking at two complete, BTS-verified batches, exactly as declared --
and yet, clinically, the encounter is half there. Nothing in the batch
protocol itself says otherwise; `batch-000.hl7`'s own `BTS-1` checks
out whether or not any of the encounters it carries are clinically
finished.

**The lesson** (the author's own charter, ADR-0107/ADR-0109, quoted
above, restated for batching specifically): transport-level
completeness -- every `BTS-1` count checks out, exactly as this run's
own 615-for-615 self-verification shows -- says nothing about
clinical-level completeness -- whether an encounter's own full record
set has actually arrived yet. A downstream receiver deciding "do I
have all of this encounter?" gets exactly the case it needs to test
that decision against: Hernandez's own admission and discharge, split
across two individually-clean batches with a third sitting between
them. (This sentence read "Smith's" until 2026-08-27 -- a name that
matched no patient in the paragraph above it, and had not for at least
two witnesses.)

**A taxonomy note, for the record.** Transport realism -- delayed
individual transmission (ADR-0109) and now schedule batching
(ADR-0111) -- simulates CORRECT transport behaviors, deterministically;
mutation (`ehrt corpus mutate`) injects INCORRECT content with an
expected finding. Message loss and duplication sit on the boundary
between the two (a real transport does both) -- a named future
taxonomy question, not resolved here; its own origin is the author's
"mutation as imperfect transport" framing from the driving
conversation this session.
