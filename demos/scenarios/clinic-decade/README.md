# Scenario: clinic-decade

A busy weekday emergency-department mix: twelve everyday-ambulatory and
acute modules (sore throat, sinusitis, bronchitis, asthma, ear
infections, UTIs, sleep apnea, fibromyalgia, dementia, appendicitis,
total joint replacement, sepsis), weighted toward the milder, more
common complaints, arriving every five minutes on average. `--config`
is [`config.edn`](config.edn), landed verbatim per the vendoring batch
2 rider (`notes/ADRs.md` ADR-0071, AR-VB2-R).

Population-scale, no captured trace — see this directory's own parent
[`README.md`](../README.md) for why. Generate the scenario's own output
first, then play it back.

## Generate

```bash
bin/ehrt corpus generate sim --seed 20260807 --patients 200 \
  --config demos/scenarios/clinic-decade/config.edn \
  --out-dir out/scenarios/clinic-decade
```

`--out-dir` is rejected if it already exists and is non-empty — remove
or rename a prior run's own directory before regenerating.

## Play

```bash
bin/ehrt play out/scenarios/clinic-decade --board 60 --rate 100000
```

Renders a bed-state snapshot every 60 stream-minutes instead of a
message-by-message ticker (`ehrt play`'s own `--board`), at 100,000
stream-seconds per wallclock-second.

Or play the sim's own story directly, from its own event log rather
than the emitted HL7 v2 messages:

```bash
bin/ehrt play out/scenarios/clinic-decade/events.edn --rate 100000
```

## What to look for

Witnessed this session (seed 20260807, 200 patients, `--config` as
above): 68 messages generated, almost all of them outpatient
encounters, so **41 of the 48 board snapshots read `inpatients: 0`**
while **the other 7 read `inpatients: 1`**, with the `active
outpatients` count climbing as the ten-year horizon plays out — e.g.:

```
-- board snapshot: 2024-02-12T00:37:00Z --

inpatients: 0  active outpatients: 2  discharged: 0  merged: 0
```

**The one inpatient, and why the claim changed.** Until ADR-0171's RNG
stream partition (2026-08-25) this scenario produced NO inpatient
admission at all, and this section said so: every snapshot read
`inpatients: 0`. The reshuffle moved which patient walks which module,
and this run now carries exactly one admission across the decade —
MRN000074, `{:ward "Emergency", :bed "ED-H04", :placement :surge}`,
cited `{:module "sepsis", :state :sepsis-ed-encounter}`, discharged
190 days of stream time later on `{:module "sepsis", :state :death}`.
That is a genuine, glass-box-cited sepsis presentation, not a defect,
and it strengthens rather than weakens the contrast drawn below: this
scenario is still overwhelmingly ambulatory (one admission against 42
outpatient visits), it simply is no longer categorically so. The
figure is a COUNT now rather than a universal, and
`bin/demo-exerciser-clinic-decade` asserts the counted split — 41 and
7 — re-derived from this paragraph at runtime, exactly as it asserted
the universal before.

The run's own closing summary: `{:unparseable-count 0, :snapshot-count
48, :skip-count 44, :rate 100000.0, :idle-cap-ms 5000, :wallclock-ms
226443, :stream-span-ms 310531980000, :clamped-count 0, :emitted 68,
:unfolded-count 0, :sink "ticker"}` — 48 of the 68 messages rendered a
snapshot; the other 20 landed in a board window a prior message had
already opened (the same 60-minute grid cell, no coalescing beyond
that — ADR-0103's boundary-catch-up fix). 44 of the 68 inter-message
waits exceeded `--idle-cap`'s default 5 seconds and were skipped rather
than actually waited out (`-- idle-skip: stream-time jumped --`,
printed immediately before the snapshot each time), and total wallclock
time for the full ten-year stream was ~3m39s. A denser population or a
larger `--rate` shortens that; this scenario's own module mix and
five-minute arrival gap, at 200 patients, is genuinely this sparse in
message traffic — most of a "clinic-decade" in this scenario's own
patient population unfolds as intake and follow-up over months and
years, not everyone arriving on one shift.

**Re-witnessed 2026-08-11 (ADR-0103):** the original 68-of-68 figure
above was produced under a boundary-cadence bug (ADR-0103) — the
snapshot count is corrected here; the seed, config, and every other
figure in this block are unchanged by the fix.

**Re-witnessed again 2026-08-25 (ADR-0171):** the RNG stream partition
moved every draw in this run. `:emitted` (68) and `:snapshot-count`
(48) are unchanged; `:skip-count` went 41 → 44 and `:stream-span-ms`
279,155,640,000 → 310,531,980,000 (a later final event stretches the
decade's own tail), and the zero-inpatient universal became the 41/7
count above. Seed and config are untouched — nothing here was tuned to
restore a figure.

**Contrast with [`../ed-tuesday/`](../ed-tuesday/README.md)** (ADR-0104,
2026-08-11): this scenario is population-scale incidence, sparse and
outpatient-only by design; ed-tuesday is a day-scale, scripted single
ED shift with real admissions, transfers, and discharges driving
visible inpatient census on a `--board`.
