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

**Contrast with [`../busy-tuesday/`](../busy-tuesday/README.md):**
busy-tuesday is population-scale incidence -- twelve everyday-
ambulatory modules across 200 patients and a ten-year horizon,
genuinely sparse traffic (its own README: "most of a busy Tuesday...
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
busy-tuesday's own `inpatients: 0` throughout:

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
admissions for `--churn` to work with, unlike busy-tuesday's own
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
and even busy-tuesday's own 3650, and produced a live encounter at
only ONE of those three horizons (3650 days: exactly 1 of 8). The
mechanism is disclosed in `config.edn`'s own header comment, grounded
in `sore_throat.json`'s own source (`Potential_Infection`'s
monthly-Delay-gated ~0.5-1% onset probability) -- these modules are
genuinely low-incidence per patient, the same shape busy-tuesday's own
README already discloses at population scale. A thin, low-weight tail
at a genuinely short (day/week/month-scale) horizon is expected to
show sparse-to-zero live content; this run's own zero is that expected
outcome, not a config defect.

Full closing summary: `{:unparseable-count 0, :snapshot-count 34,
:skip-count 0, :rate 100000.0, :idle-cap-ms 5000, :wallclock-ms 1855,
:stream-span-ms 128520000, :clamped-count 0, :emitted 283,
:unfolded-count 0, :sink "ticker"}`.
