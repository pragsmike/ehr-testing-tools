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

Witnessed 2026-08-26 (seed 20260811, 100 patients, `--config` as
above, `--churn` on): **745 ground-truth events, 333 HL7 v2 messages,
105 board snapshots** over a 603,759,336,000 ms stream span.

**PATIENTS COME BACK NOW.** `config.edn` opted this scenario into the
lifted ENCOUNTER HORIZON on 2026-08-26 (arc 3b sweep 1, ADR-0174 ruling
A1), and that is where these figures moved from the previous witness's
695/286/104. Until that day every patient in every corpus this repo had
got exactly ONE encounter, ever -- `check.clj`'s own
`admission-only-when-new` was that horizon written as an invariant, and
a returning person's arrival simply queued nothing. This run now
carries **127 encounter openers across 116 patients: 14 patients with
more than one, 15 encounters the pre-sweep engine threw away, and a
maximum of THREE on a single patient.** Same patient, same patient-id,
same MRN each time -- which is the whole point, because an MPI under
test has to see the same MRN twice.

**And every message now carries a visit number.** PV1-19 was EMPTY on
every message this project had ever produced; it now renders the
encounter's own `ENC-` id (ADR-0174 ruling C1). 328 of this run's 333
PV1 segments carry one. The five that do not are messages that belong
to no OPEN encounter and correctly say so: an ADT^A40 identification
merge on a patient whose stay had already ended, three ORU^R01 results
arriving after discharge (the pending-labs-at-discharge case), and one
ADT^A11 cancelling an already-discharged patient's original admission.

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
occupied beds; the census climbs through the shift to a peak of 15
concurrent inpatients, then drains to 1 by the run's own last snapshot.
The peak moved 11 -> 15 with the horizon lift, for the obvious reason:
a returning patient occupies a bed the second time too.

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
-- board snapshot: 2045-09-27T22:55:36Z --

Emergency:
  ED-H08  Lee, Jennifer  MRN MRN000040  inpatient  attending: 5761303028

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
020857cbd3a0fce5faeaebc178f051ebce905dc2e0c08c450df87a055aad086f  out/scenarios/ed-tuesday-base/events.edn
020857cbd3a0fce5faeaebc178f051ebce905dc2e0c08c450df87a055aad086f  out/scenarios/ed-tuesday-latency/events.edn
```

`diff` reports no differences; the digests match exactly -- the same
375 ground-truth events either way. Only the *rendering* differs: the
`msg-%03d.hl7` files carry different MSH-7 values and a different file
order (`emit-wire` sorts by transmit time, not log order) between the
two out-dirs -- the palgebra's own `GT -> TimedWire` arrow
(`docs/dev/simulator-architecture.md` section 5), visible in a diff of
two directories generated from the same seed.

**Play the latency wire into the board:**

```bash
bin/ehrt play out/scenarios/ed-tuesday-latency --board 60 --rate 10000000
```

**What the board actually shows.** Patient MRN000020 (Jones, Ava),
bed `ED-H16`: admitted (EVN-2 clinical time `2026-08-11T06:03:00Z`),
discharged 43 minutes later (`06:46:00Z`) -- ordinary, unremarkable,
log-order-correct clinical history. On the *latency* wire, the
discharge message's own sampled delay (43m08s) is shorter than the
admission message's own (1h27m09s), so the discharge (A03) transmits
first (MSH-7 `07:29:08Z`) and the admission (A01) transmits second
(MSH-7 `07:30:09Z`) -- reordered on the wire, never in ground truth.
The board, folding messages in the order it receives them:

```
-- board snapshot: 2026-08-11T06:36:57Z --

Cardiology:
  CARDIOLOGY-02  Garcia, Jacob  MRN MRN000015  inpatient  attending: 5761303028

Emergency:
  ED-H02  Johnson, Matthew  MRN MRN000011  inpatient  attending: 5761303028
  ED-H06  Wilson, Michael  MRN MRN000021  inpatient  attending: 5761303028
  ED-H07  Johnson, Robert  MRN MRN000010  inpatient  attending: 5761303028
  ED-H07  Martinez, Michelle  MRN MRN000013  inpatient  attending: 5761303028
  ED-H08  Patel, David  MRN MRN000016  inpatient  attending: 5761303028
  ED-H11  Smith, James  MRN MRN000012  inpatient  attending: 5761303028
  ED-H14  Jones, Olivia  MRN MRN000018  inpatient  attending: 5761303028

inpatients: 8  active outpatients: 0  discharged: 10  merged: 0
-- board snapshot: 2026-08-11T07:49:35Z --

Cardiology:
  CARDIOLOGY-02  Garcia, Jacob  MRN MRN000015  inpatient  attending: 5761303028

Emergency:
  ED-H06  Wilson, Michael  MRN MRN000021  inpatient  attending: 5761303028
  ED-H07  Smith, Ashley  MRN MRN000024  ?
  ED-H08  Patel, David  MRN MRN000016  inpatient  attending: 5761303028
  ED-H14  Jones, Olivia  MRN MRN000018  inpatient  attending: 5761303028
  ED-H15  Smith, Susan  MRN MRN000022  inpatient  attending: 5761303028
  ED-H16  Jones, Ava  MRN MRN000020  inpatient  attending: 5761303028

inpatients: 6  active outpatients: 0  discharged: 14  merged: 0
```

Jones is absent from the earlier snapshot: her discharge (folded first,
off-camera before it) had already removed her from the board. Her
admission then arrives -- `fold-message`'s own `:admission` case applies
unconditionally (ADR-0109's Step 5 finding, live here rather than
probed): it puts her right back on the board as `inpatient` in
`ED-H16`, a bed she vacated an hour of clinical time earlier. Her
phantom entry never clears (no further message for her exists) -- she is
still on the board at the run's own last snapshot, more than 27 hours
of stream time later. The *same* patient in the base (no-latency) run
above appears exactly once, admitted and never seen again once
discharged -- the entire disorder is the wire's doing, not the ground
truth's.

The same snapshot shows the other two shapes this produces, neither
scripted: `ED-H07  Smith, Ashley  MRN MRN000024  ?` -- a bed whose
occupant the board cannot state a status for, because the only message
it has folded for her is out of order -- and, in the run's own last
snapshot, `ED-H08` carrying BOTH `Brown, David (MRN000063)` and `Chen,
Jessica (MRN000048)`: two patients shown in one bed, one of them a
phantom re-admission and the other a transfer message (A02) that posted
after its own cancellation.

This is one of **3 (of 110 admitted patients, seed 20260811)** whose
own admission message arrives after its own transfer or discharge
message on this wire -- occasional and visible, not universal
(`config-latency.edn`'s own header has the tuning rationale).

**RE-WITNESSED 2026-08-26, and the direction is worth stating: 8 of 92
became 3 of 110.** The demographic-fold opt-in added 18 more admitted
patients, and every one of them is a HOOK encounter spread across the
population's twenty years -- a birth, an injury, an unidentified
arrival -- whose own admission-to-discharge gap is hours or days.
`config-latency.edn`'s bands are 15 to 90 minutes, so a gap that wide
cannot be crossed by a transmit delay and those encounters simply
cannot disorder. The three that do are the scripted ED shift's own,
where the gaps are tight enough for the wire to overtake the clinic.
The mechanism is unchanged and so is the claim; what changed is that
the denominator now contains patients the mechanism cannot reach, and
saying "3 of 110" without saying which 110 would understate it.

Closing summary: `{:unparseable-count 0, :snapshot-count 104,
:skip-count 2, :rate 1.0E7, :idle-cap-ms 5000, :wallclock-ms 57828,
:stream-span-ms 603759467000, :clamped-count 0, :emitted 286,
:unfolded-count 0, :sink "ticker"}` -- the same 286 messages as the
base run, the same 104 snapshots, and a stream span 131 seconds LONGER
(the wire's own tail now ends after the last clinical event rather than
before it). It has gone both ways across three witnesses -- 430 seconds
longer before ADR-0171, 306 shorter after it, 131 longer now -- and
neither direction is the claim; all three are artifacts of where
transmit times fall against the board's own tick-crossing schedule. The
claim is that the same ground truth, played on a delayed wire, produces
a different board.

**What a receiver could do better.** Nothing here is prescribed or
built this session -- as reader orientation only: a receiver that
buffered incoming messages briefly and reconciled by clinical time
(EVN-2, when present) rather than folding strictly in arrival order
would not have produced Jones's own phantom re-admission. Whether, or
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

Witnessed 2026-08-26 (same seed-20260811 run as above, 333 messages
across 106 occupied hourly buckets, `2026-08-11T00:00Z` through
`2045-09-27T23:00Z`). CORRECTED at the same re-witness: the closing
bucket used to be printed as `13:00Z` here, which was wrong by ten
hours and had been since this section was written -- `:start-ms`
2390166000000 is 23:00Z, and that figure itself never moved.

```
{:status :ok,
 :payload
 {:out-dir "out/scenarios/ed-tuesday-latency-batches",
  :interval-ms 3600000,
  :batches
  [{:file "batch-000.hl7", :count 2,
    :start-ms 1786406400000, :end-ms 1786410000000, :verified true}
   {:file "batch-001.hl7", :count 4,
    :start-ms 1786410000000, :end-ms 1786413600000, :verified true}
   {:file "batch-002.hl7", :count 7,
    :start-ms 1786413600000, :end-ms 1786417200000, :verified true}
   ;; ... batch-003.hl7 through batch-103.hl7 ...
   {:file "batch-104.hl7", :count 1,
    :start-ms 2389993200000, :end-ms 2389996800000, :verified true}
   {:file "batch-105.hl7", :count 1,
    :start-ms 2390166000000, :end-ms 2390169600000, :verified true}],
  :span {:earliest-ms 1786406400000, :latest-ms 2390169600000}}}
```

**Epoch-aligned, and the interior gaps are enormous.** The first thirty
or so batches are the ED shift itself, packed hour after hour with 2 to
12 messages each; everything after them is the population tail, and
they are almost all `:count 1` -- one birth, one injury, one
unidentified arrival, in an hour that carried nothing else, years apart
from the batch before it. `batch-104` and `batch-105` sit in 2045. The
empty hours between are simply ABSENT, never written as empty files
(ADR-0111's own named v1 deferral: an interior empty batch is not
represented, only skipped), which is why nineteen years partition into
106 files rather than 168,000. Every one of the 106 written files
self-verified: `write-and-verify-batch!` (`bases/cli`) decodes what it
just wrote straight back and checks `BTS-1` against the real message
count before ever reporting success -- `:verified true` on all 106 is
that check, exercised, not merely claimed.

**The wrapper itself**, `batch-000.hl7`, head and tail:

```
$ head -c 100 out/scenarios/ed-tuesday-latency-batches/batch-000.hl7
BHS|^~\&

MSH|^~\&|EHR-TESTING-SIM|SIM|||20260811003626+0000||ADT^A01|MRN000001-A01-0|P|2.3EVN|A01|
$ tail -c 45 out/scenarios/ed-tuesday-latency-batches/batch-000.hl7 | cat -A
|||||||^MIN1|1||medicare-65|Medicare^M$
$
BTS|2$
$
```

`BHS|^~\&` opens the batch; `BTS|2` closes it, `BTS-1` naming the true
count of 2 messages this file actually carries -- the minimal,
deterministic field set ADR-0111 rules for v1 (no creation-time field
populated at all, so the determinism law -- no wall clock anywhere --
holds trivially rather than by threading one through).

**A straddling encounter.** MRN000002 (Hernandez, Sandra, bed ED-H09):
admitted (A01, MSH-7 transmit time `2026-08-11T00:37:39Z`) lands in
`batch-000.hl7`; discharged (A03, MSH-7 transmit time
`2026-08-11T01:59:02Z`) lands in `batch-001.hl7` -- one clock-hour
later, the very next batch. A downstream receiver holding only
`batch-000.hl7` has that admission and nothing else for that patient:
by every transport-level measure the receiver is looking at a complete,
BTS-verified batch (2 of 2 messages present, exactly as declared) --
and yet, clinically, the encounter is half there. Nothing in the batch
protocol itself says otherwise; `batch-000.hl7`'s own `BTS-1` checks
out whether or not any of the encounters it carries are clinically
finished.

**The lesson** (the author's own charter, ADR-0107/ADR-0109, quoted
above, restated for batching specifically): transport-level
completeness -- every `BTS-1` count checks out, exactly as this run's
own 106-for-106 self-verification shows -- says nothing about
clinical-level completeness -- whether an encounter's own full record
set has actually arrived yet. A downstream receiver deciding "do I
have all of this encounter?" gets exactly the case it needs to test
that decision against: Smith's own admission and discharge, split
across two adjacent, individually-clean batches.

**A taxonomy note, for the record.** Transport realism -- delayed
individual transmission (ADR-0109) and now schedule batching
(ADR-0111) -- simulates CORRECT transport behaviors, deterministically;
mutation (`ehrt corpus mutate`) injects INCORRECT content with an
expected finding. Message loss and duplication sit on the boundary
between the two (a real transport does both) -- a named future
taxonomy question, not resolved here; its own origin is the author's
"mutation as imperfect transport" framing from the driving
conversation this session.
