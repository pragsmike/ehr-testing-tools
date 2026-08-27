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

Witnessed 2026-08-27 (seed 20260811, 100 patients, `--config` as
above, `--churn` on): **1,151 ground-truth events, 739 HL7 v2 messages,
128 board snapshots** over a 603,761,136,000 ms stream span.

**BEDS TAKE TIME TO TURN OVER NOW.** `config.edn` opted this scenario
into the BED-STATUS CYCLE on 2026-08-27 (arc 3b sweep 2, ADR-0174
section 2(c) and ruling C), and that is where these figures moved from
the previous witness's 745/333/105. A vacated bed is no longer free the
instant its occupant leaves: it goes `:dirty`, then `:cleaning`, then
`:ready`, and the allocation ladder will not hand out a bed that is not
ready. **406 of this run's messages are ADT^A20 bed-status updates** --
one per transition, over 136 turnovers -- and they are the reason the
message count nearly doubled while the clinical traffic barely moved.

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
carries **127 encounter openers across 116 patients: 14 patients with
more than one, 15 encounters the pre-sweep engine threw away, and a
maximum of THREE on a single patient** -- every one of those five
figures unchanged by the bed cycle, which moves WHEN a bed changes
hands and not who is admitted. Same patient, same patient-id,
same MRN each time -- which is the whole point, because an MPI under
test has to see the same MRN twice.

**And every message now carries a visit number.** PV1-19 was EMPTY on
every message this project had ever produced; it now renders the
encounter's own `ENC-` id (ADR-0174 ruling C1). 340 of this run's 341
PV1 segments carry one. The ONE that does not is an ORU^R01 result
arriving after its patient's discharge -- the pending-labs-at-discharge
case, which belongs to no open encounter and correctly says so. (The
blank count was five before the bed cycle reshuffled this run; four of
those five messages now fall inside an open encounter instead.)

Note that the A20 bed-status messages carry NO PV1 at all -- an
`ADT^A20` is `[MSH EVN NPU]`, no PID and no PV1, because a bed that
nobody is in has no patient to name. That is why 739 messages carry
only 341 PV1 segments between them.

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
under its ward for as long as housekeeping has it, and 45 such lines
render across this run's snapshots.

```
-- board snapshot: 2026-08-11T05:00:00Z --

Emergency:
  ED-H02  Garcia, Michael  MRN MRN000013  inpatient  attending: 5761303028
  ED-H03  Wilson, Amanda  MRN MRN000016  inpatient  attending: 5761303028
  ED-H04  Miller, Deborah  MRN MRN000017  inpatient  attending: 5761303028
  ED-H07  Johnson, James  MRN MRN000012  inpatient  attending: 5761303028
  ED-H08  Gonzalez, Joshua  MRN MRN000010  inpatient  attending: 5761303028
  ED-H10  Gonzalez, Emily  MRN MRN000015  inpatient  attending: 5761303028
  ED-H15  Martinez, James  MRN MRN000007  inpatient  attending: 5761303028
  ED-H06  (dirty)
  ED-H13  (cleaning)
```

A `:ready` bed is still not listed. An available bed is the normal
case, and listing every one of them would bury the two states a charge
nurse is actually looking for.

```
-- board snapshot: 2026-08-11T01:12:00Z --

Emergency:
  ED-H01  Patel, Lisa  MRN MRN000004  inpatient  attending: 5761303028
  ED-H05  Johnson, Christopher  MRN MRN000003  inpatient  attending: 5761303028
  ED-H09  Hernandez, Sandra  MRN MRN000002  inpatient  attending: 5761303028
  ED-H11  Johnson, Michael  MRN MRN000005  inpatient  attending: 5761303028

inpatients: 4  active outpatients: 0  discharged: 1  merged: 0
```

```
-- board snapshot: 2045-09-27T23:25:36Z --

Emergency:
  ED-H06  Lee, Jennifer  MRN MRN000040  inpatient  attending: 5761303028

inpatients: 1  active outpatients: 0  discharged: 110  merged: 1
```

Note the DATE on that second snapshot: 2045, not 2026. That is the
population tail, not the shift.

**Discharges accrue and churn fires.** `discharged` climbs from 1 to
110 across the run; `merged` climbs from 0 to 1. That merge is now an
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

Full closing summary: `{:unparseable-count 0, :snapshot-count 104,
:skip-count 2, :rate 1.0E7, :idle-cap-ms 5000, :wallclock-ms 57860,
:stream-span-ms 603759336000, :clamped-count 0, :emitted 286,
:unfolded-count 0, :sink "ticker"}`.

**`--rate` MOVED, 100,000 -> 10,000,000**, on both scenarios and for
the same measured reason: `--rate` is stream-seconds per
wallclock-second, so it has to be read against the stream it paces, and
this one grew from 35 hours to nineteen years. At the old rate the
playback spent its time waiting; at the new one the whole stream plays
in ~58 seconds with two skips. The board census is unchanged by the
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
729f07ad1cecbb770682633e668f2247dce57769c443fe46206d928ce9fd8afe  out/scenarios/ed-tuesday-base/events.edn
729f07ad1cecbb770682633e668f2247dce57769c443fe46206d928ce9fd8afe  out/scenarios/ed-tuesday-latency/events.edn
```

`diff` reports no differences; the digests match exactly -- the same
1,151 ground-truth events either way. (That figure read `375` until
2026-08-27: it was witnessed before ADR-0171's stream partition and
nothing kept it true -- one of the stale tokens
`.agents/plans/roadmap.md#post-partition-narrative-refresh` counts in
this file, corrected here rather than left because this sweep had to
re-witness the line above it anyway.) Only the *rendering* differs: the
`msg-%03d.hl7` files carry different MSH-7 values and a different file
order (`emit-wire` sorts by transmit time, not log order) between the
two out-dirs -- the palgebra's own `GT -> TimedWire` arrow
(`docs/dev/simulator-architecture.md` section 5), visible in a diff of
two directories generated from the same seed.

**Play the latency wire into the board:**

```bash
bin/ehrt play out/scenarios/ed-tuesday-latency --board 60 --rate 10000000
```

**What the board actually shows.** Patient MRN000005 (Johnson,
Michael), bed `ED-H11`: admitted (EVN-2 clinical time
`2026-08-11T01:12:00Z`), discharged 47 minutes later (`01:59:00Z`) --
ordinary, unremarkable, log-order-correct clinical history. On the
*latency* wire, the discharge message's own sampled delay (23m36s) is
shorter than the admission message's own (1h25m37s), so the discharge
(A03) transmits first (MSH-7 `02:22:36Z`) and the admission (A01)
transmits second (MSH-7 `02:37:37Z`) -- reordered on the wire, never in
ground truth. The board, folding messages in the order it receives
them:

```
-- board snapshot: 2026-08-11T01:37:00Z --

Emergency:
  ED-H05  Johnson, Christopher  MRN MRN000003  inpatient  attending: 5761303028
  ED-H09  Hernandez, Sandra  MRN MRN000002  inpatient  attending: 5761303028

inpatients: 2  active outpatients: 0  discharged: 1  merged: 0
-- board snapshot: 2026-08-11T02:37:37Z --

Emergency:
  ED-H01  Patel, Lisa  MRN MRN000004  inpatient  attending: 5761303028
  ED-H11  Johnson, Michael  MRN MRN000005  inpatient  attending: 5761303028
  ED-H13  Martinez, Emily  MRN MRN000008  inpatient  attending: 5761303028
  ED-H14  Garcia, James  MRN MRN000006  inpatient  attending: 5761303028

inpatients: 4  active outpatients: 0  discharged: 3  merged: 0
```

Johnson is absent from the earlier snapshot even though he was
clinically admitted 25 minutes before it: NEITHER of his two messages
had transmitted yet, so the board had never heard of him. His discharge
arrives first, at `02:22:36Z`, and folds against an entry that
bootstraps from that message alone -- he is `:discharged` on a board he
was never on. Then his admission arrives at `02:37:37Z` and
`fold-message`'s own `:admission` case applies UNCONDITIONALLY
(ADR-0109's Step 5 finding, live here rather than probed): it puts him
on the board as `inpatient` in `ED-H11`, a bed he vacated 38 minutes of
clinical time earlier. His phantom entry never clears -- no further
message for him exists -- and he is still on that board at the run's
own last snapshot, in **2045**, nineteen years of stream time later.
The *same* patient in the base (no-latency) run above appears exactly
once, admitted and never seen again once discharged -- the entire
disorder is the wire's doing, not the ground truth's.

**RE-WITNESSED 2026-08-27** (arc 3b sweep 2's opt-in reshuffled this
run): the cast changed from MRN000020 in `ED-H16` to MRN000005 in
`ED-H11`, and the SHAPE changed slightly with it. The previous witness
had a patient removed from the board by an early discharge and then put
back by a late admission; this one is never on the board at all before
its discharge arrives. Both are the same underlying fact -- an
unconditional `:admission` fold on an out-of-order wire -- and the
second is the cleaner illustration of it, because the phantom is
manufactured entirely out of two messages that arrived backwards.

This is one of **5 (of 112 admitted patients, seed 20260811)** whose
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

Closing summary: `{:unparseable-count 0, :snapshot-count 132,
:skip-count 2, :rate 1.0E7, :idle-cap-ms 5000, :wallclock-ms 58191,
:stream-span-ms 603759087000, :clamped-count 0, :emitted 739,
:unfolded-count 0, :sink "ticker"}` -- the same 739 messages as the
base run, FOUR more snapshots than it (132 against 128), and a stream
span 2,049 seconds SHORTER. It has gone both ways across four
witnesses -- 430 seconds longer before ADR-0171, 306 shorter after it,
131 longer at the encounter horizon, 2,049 shorter now -- and
neither direction is the claim; all three are artifacts of where
transmit times fall against the board's own tick-crossing schedule. The
claim is that the same ground truth, played on a delayed wire, produces
a different board.

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

Witnessed 2026-08-27 (same seed-20260811 run as above, 739 messages
across 135 occupied hourly buckets, `2026-08-11T00:00Z` through
`2045-09-27T23:00Z`). The bucket count moved 106 -> 135 with the bed
cycle: A20 bed-status traffic lands in hours that previously carried
nothing, so more hours are occupied AND the busy ones are fuller.
CORRECTED at the 2026-08-26 re-witness and still true: the closing
bucket used to be printed as `13:00Z` here, which was wrong by ten
hours -- `:start-ms` 2390166000000 is 23:00Z, and that figure itself
has never moved.

```
{:status :ok,
 :payload
 {:out-dir "out/scenarios/ed-tuesday-latency-batches",
  :interval-ms 3600000,
  :batches
  [{:file "batch-000.hl7", :count 5,
    :start-ms 1786406400000, :end-ms 1786410000000, :verified true}
   {:file "batch-001.hl7", :count 9,
    :start-ms 1786410000000, :end-ms 1786413600000, :verified true}
   {:file "batch-002.hl7", :count 16,
    :start-ms 1786413600000, :end-ms 1786417200000, :verified true}
   ;; ... batch-003.hl7 through batch-133.hl7 ...
   {:file "batch-133.hl7", :count 1,
    :start-ms 2390162400000, :end-ms 2390166000000, :verified true}
   {:file "batch-134.hl7", :count 3,
    :start-ms 2390166000000, :end-ms 2390169600000, :verified true}],
  :span {:earliest-ms 1786406400000, :latest-ms 2390169600000}}}
```

**Epoch-aligned, and the interior gaps are enormous.** The first thirty
or so batches are the ED shift itself, packed hour after hour with 2 to
35 messages each -- the upper end tripled with the bed cycle, because
every bed turnover puts three more A20s into the hour it falls in.
Everything after them is the population tail, and they are almost all
`:count 1` -- one birth, one injury, one unidentified arrival, in an
hour that carried nothing else, years apart from the batch before it.
`batch-133` and `batch-134` sit in 2045. The empty hours between are
simply ABSENT, never written as empty files (ADR-0111's own named v1
deferral: an interior empty batch is not represented, only skipped),
which is why nineteen years partition into 135 files rather than
168,000. Every one of the 135 written files self-verified:
`write-and-verify-batch!` (`bases/cli`) decodes what it just wrote
straight back and checks `BTS-1` against the real message count before
ever reporting success -- `:verified true` on all 135 is that check,
exercised, not merely claimed.

**The wrapper itself**, `batch-000.hl7`, head and tail:

```
$ head -c 100 out/scenarios/ed-tuesday-latency-batches/batch-000.hl7
BHS|^~\&

MSH|^~\&|EHR-TESTING-SIM|SIM|||20260811003626+0000||ADT^A01|MRN000001-A01-0|P|2.3EVN|A01|
$ tail -c 45 out/scenarios/ed-tuesday-latency-batches/batch-000.hl7 | cat -A
mergency^^ED-H16^general-hospital|U^M$
$
BTS|5$
$
```

`BHS|^~\&` opens the batch; `BTS|5` closes it, `BTS-1` naming the true
count of 5 messages this file actually carries. Note what the LAST of
those five is: an `NPU` segment ending `|U` -- an ADT^A20 reporting bed
`ED-H16` unoccupied and ready again. A bed-status update is an ordinary
message to the batch protocol, which is the point of rendering the
cycle onto the wire at all -- the minimal,
deterministic field set ADR-0111 rules for v1 (no creation-time field
populated at all, so the determinism law -- no wall clock anywhere --
holds trivially rather than by threading one through).

**A straddling encounter.** MRN000002 (Hernandez, Sandra, bed ED-H09):
admitted (A01, MSH-7 transmit time `2026-08-11T00:37:39Z`) lands in
`batch-000.hl7`; discharged (A03, MSH-7 transmit time
`2026-08-11T02:13:46Z`) lands in `batch-002.hl7` -- TWO clock-hours
later, skipping a whole batch in between. (It was the very next batch
until 2026-08-27; the bed cycle's reshuffle redrew this discharge's own
transmit delay and pushed it a bucket further out, which makes the
point harder rather than softer.) A downstream receiver holding
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
own 135-for-135 self-verification shows -- says nothing about
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
