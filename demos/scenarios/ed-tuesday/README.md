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
bin/ehrt play out/scenarios/ed-tuesday --board 60 --rate 100000
```

Renders a bed-state snapshot every 60 stream-minutes instead of a
message-by-message ticker (`ehrt play`'s own `--board`), at 100,000
stream-seconds per wallclock-second.

Or play the sim's own story directly, from its own event log rather
than the emitted HL7 v2 messages:

```bash
bin/ehrt play out/scenarios/ed-tuesday/events.edn --rate 100000
```

## What to look for

Witnessed this session (seed 20260811, 100 patients, `--config` as
above, `--churn` on): 383 ground-truth events, 283 HL7 v2 messages,
34 board snapshots over a 128,520,000 ms (~35.7-hour) stream span --
one busy shift running a little past the next morning, day-scale as
intended.

**Inpatients rise and fall.** The first snapshot already shows 4
occupied beds; the census climbs through the shift to a peak of 21
concurrent inpatients, then drains back down to 3 by the run's own
last snapshot -- the whole point of this scenario, unlike
clinic-decade's own `inpatients: 0` throughout:

```
-- board snapshot: 2026-08-11T01:12:00Z --

Emergency:
  ED-H01  Smith, Mary  MRN MRN000001  inpatient  attending: 3327386918
  ED-H03  Johnson, Michelle  MRN MRN000004  inpatient  attending: 3327386918
  ED-H06  Davis, Matthew  MRN MRN000003  inpatient  attending: 3327386918
  ED-H12  Rodriguez, Jacob  MRN MRN000005  inpatient  attending: 3327386918

inpatients: 4  active outpatients: 0  discharged: 1  merged: 0
```

```
-- board snapshot: 2026-08-12T11:42:00Z --

Emergency:
  ED-H03  Walker, James  MRN MRN000051  inpatient  attending: 3327386918
  ED-H10  Smith, Jessica  MRN MRN000033  inpatient  attending: 3327386918
  ED-H16  Garcia, Mary  MRN MRN000028  inpatient  attending: 3327386918

inpatients: 3  active outpatients: 0  discharged: 84  merged: 5
```

**Discharges accrue and churn fires.** `discharged` climbs from 1 to
84 across the run; `merged` (an `InjectChurn` bed-merge event) climbs
from 0 to 5 -- real churn traffic, the direct payoff of scripting real
admissions for `--churn` to work with, unlike clinic-decade's own
outpatient-only mix where a merge has no admitted patient to touch.

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

Full closing summary: `{:unparseable-count 0, :snapshot-count 34,
:skip-count 0, :rate 100000.0, :idle-cap-ms 5000, :wallclock-ms 1855,
:stream-span-ms 128520000, :clamped-count 0, :emitted 283,
:unfolded-count 0, :sink "ticker"}`.

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
b4e776f773502cf78795a83bb52836ea208c831935330cb0480a731525e637f1  out/scenarios/ed-tuesday-base/events.edn
b4e776f773502cf78795a83bb52836ea208c831935330cb0480a731525e637f1  out/scenarios/ed-tuesday-latency/events.edn
```

`diff` reports no differences; the digests match exactly -- the same
383 ground-truth events either way. Only the *rendering* differs: the
`msg-%03d.hl7` files carry different MSH-7 values and a different file
order (`emit-wire` sorts by transmit time, not log order) between the
two out-dirs -- the palgebra's own `GT -> TimedWire` arrow
(`docs/dev/simulator-architecture.md` section 5), visible in a diff of
two directories generated from the same seed.

**Play the latency wire into the board:**

```bash
bin/ehrt play out/scenarios/ed-tuesday-latency --board 60 --rate 100000
```

**What the board actually shows.** Patient MRN000013 (Walker,
William), pathway `ed-fast-track`: admitted (EVN-2 clinical time
`2026-08-11T03:36:00Z`), discharged 37 minutes later (`04:13:00Z`) --
ordinary, unremarkable, log-order-correct clinical history. On the
*latency* wire, the discharge message's own sampled delay (20m54s) is
shorter than the admission message's own (1h00m46s), so the discharge
(A03) transmits first (MSH-7 `04:33:54Z`) and the admission (A01)
transmits second (MSH-7 `04:36:46Z`) -- reordered on the wire, never in
ground truth. The board, folding messages in the order it receives
them:

```
-- board snapshot: 2026-08-11T04:33:54Z --

Emergency:
  ED-H08  D'Angelo, James  MRN MRN000012  inpatient  attending: 3327386918
  ED-H12  Rodriguez, Jacob  MRN MRN000005  inpatient  attending: 3327386918
  ED-H16  Anderson-Lee, Linda  MRN MRN000009  inpatient  attending: 3327386918

inpatients: 3  active outpatients: 0  discharged: 9  merged: 0
-- board snapshot: 2026-08-11T05:43:41Z --

Emergency:
  ED-H01  Garcia-Lopez, Amanda  MRN MRN000018  inpatient  attending: 3327386918
  ED-H03  Moore, Amanda  MRN MRN000015  inpatient  attending: 3327386918
  ED-H13  Walker, William  MRN MRN000013  inpatient  attending: 3327386918
  ED-H13  Gonzalez, Emma  MRN MRN000017  inpatient  attending: 3327386918
  ED-H14  Johnson, Joshua  MRN MRN000014  inpatient  attending: 3327386918
  ED-H16  Anderson-Lee, Linda  MRN MRN000009  inpatient  attending: 3327386918

inpatients: 6  active outpatients: 0  discharged: 10  merged: 0
```

Walker's own discharge (folded first, off-camera between these two
snapshots) already removed him from the board. His admission then
arrives -- `fold-message`'s own `:admission` case applies
unconditionally (ADR-0109's Step 5 finding, live here rather than
probed): it puts him right back on the board as `inpatient` in
`ED-H13` -- the same bed label the board independently shows occupied
by Gonzalez, Emma in this same snapshot, Walker's own ghost entry never
having cleared. Two patients shown occupying the same bed, one of them
(Walker) already discharged, in ground truth, before his own admission
message ever posts. Walker's
own phantom entry never clears (no further message for him exists) --
it is still on the board at the run's own last snapshot. The *same*
patient in the base (no-latency) run above appears exactly once,
admitted and never seen again once discharged -- the entire disorder
is the wire's doing, not the ground truth's. This is one of 8 (of 92
admitted patients, seed
20260811) whose own admission message arrives after its own transfer
or discharge message on this wire -- occasional and visible, not
universal (`config-latency.edn`'s own header has the tuning
rationale).

Closing summary: `{:unparseable-count 0, :snapshot-count 33,
:skip-count 0, :rate 100000.0, :idle-cap-ms 5000, :wallclock-ms 1765,
:stream-span-ms 128950000, :clamped-count 0, :emitted 283,
:unfolded-count 0, :sink "ticker"}` -- the same 283 messages as the
base run, one fewer snapshot (33 vs 34: the board's own tick-crossing
schedule shifts when transmit times shift), and a stream span 430
seconds longer (a shifted final message extends the wire's own tail
past the last clinical event).

**What a receiver could do better.** Nothing here is prescribed or
built this session -- as reader orientation only: a receiver that
buffered incoming messages briefly and reconciled by clinical time
(EVN-2, when present) rather than folding strictly in arrival order
would not have produced Walker's own phantom re-admission. Whether, or
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

Witnessed this session (same seed-20260811 run as above, 283 messages
across 34 occupied hourly buckets, `2026-08-11T00:00Z` through
`2026-08-12T13:00Z`):

```
{:status :ok,
 :payload
 {:out-dir "out/scenarios/ed-tuesday-latency-batches",
  :interval-ms 3600000,
  :batches
  [{:file "batch-000.hl7", :count 3,
    :start-ms 1786406400000, :end-ms 1786410000000, :verified true}
   {:file "batch-001.hl7", :count 4,
    :start-ms 1786410000000, :end-ms 1786413600000, :verified true}
   {:file "batch-002.hl7", :count 5,
    :start-ms 1786413600000, :end-ms 1786417200000, :verified true}
   ;; ... batch-003.hl7 through batch-030.hl7, one per occupied hour ...
   {:file "batch-031.hl7", :count 1,
    :start-ms 1786518000000, :end-ms 1786521600000, :verified true}
   {:file "batch-032.hl7", :count 2,
    :start-ms 1786528800000, :end-ms 1786532400000, :verified true}
   {:file "batch-033.hl7", :count 1,
    :start-ms 1786536000000, :end-ms 1786539600000, :verified true}],
  :span {:earliest-ms 1786406400000, :latest-ms 1786539600000}}}
```

**Epoch-aligned, and the interior gap is real.** `batch-031` spans
`[07:00Z, 08:00Z)` on 2026-08-12 and `batch-032` spans `[10:00Z,
11:00Z)` -- the two hours in between (`08:00Z`-`10:00Z`) carried no
traffic at all in this run's own tail and are simply absent, never
written as empty files (ADR-0111's own named v1 deferral: an
interior empty batch isn't represented, only skipped). Every one of
the 34 written files self-verified: `write-and-verify-batch!`
(`bases/cli`) decodes what it just wrote straight back and checks
`BTS-1` against the real message count before ever reporting success
-- `:verified true` on all 34 is that check, exercised, not merely
claimed.

**The wrapper itself**, `batch-000.hl7`, head and tail:

```
$ head -c 100 out/scenarios/ed-tuesday-latency-batches/batch-000.hl7
BHS|^~\&

MSH|^~\&|EHR-TESTING-SIM|SIM|||20260811003026+0000||ADT^A01|MRN000002-A01-360|P|2.3EVN|A0
$ tail -c 45 out/scenarios/ed-tuesday-latency-batches/batch-000.hl7 | cat -A
dicare-advantage|Medicare Advantage^M$
$
BTS|3$
$
```

`BHS|^~\&` opens the batch; `BTS|3` closes it, `BTS-1` naming the true
count of 3 messages this file actually carries -- the minimal,
deterministic field set ADR-0111 rules for v1 (no creation-time field
populated at all, so the determinism law -- no wall clock anywhere --
holds trivially rather than by threading one through).

**A straddling encounter.** Smith, James (MRN000002, bed ED-H05):
admitted (A01, MSH-7 transmit time `2026-08-11T00:30:26Z`) lands in
`batch-000.hl7`; discharged (A03, MSH-7 transmit time
`2026-08-11T01:34:19Z`) lands in `batch-001.hl7` -- one clock-hour
later, the very next batch. A downstream receiver holding only
`batch-000.hl7` has Smith's admission and nothing else for him: by
every transport-level measure that receiver is looking at a complete,
BTS-verified batch (3 of 3 messages present, exactly as declared) --
and yet, clinically, his encounter is half there. Nothing in the batch
protocol itself says otherwise; `batch-000.hl7`'s own `BTS-1` checks
out whether or not any of the encounters it carries are clinically
finished.

**The lesson** (the author's own charter, ADR-0107/ADR-0109, quoted
above, restated for batching specifically): transport-level
completeness -- every `BTS-1` count checks out, exactly as this run's
own 34-for-34 self-verification shows -- says nothing about
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
