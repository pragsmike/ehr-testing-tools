# Scenario: busy-tuesday

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
  --config components/sim/docs/scenarios/busy-tuesday/config.edn \
  --out-dir out/scenarios/busy-tuesday
```

`--out-dir` is rejected if it already exists and is non-empty — remove
or rename a prior run's own directory before regenerating.

## Play

```bash
bin/ehrt play out/scenarios/busy-tuesday --board 60 --rate 100000
```

Renders a bed-state snapshot every 60 stream-minutes instead of a
message-by-message ticker (`ehrt play`'s own `--board`), at 100,000
stream-seconds per wallclock-second.

## What to look for

Witnessed this session (seed 20260807, 200 patients, `--config` as
above): 68 messages generated, all of them outpatient encounters (this
scenario's module mix produces no inpatient admission), so every board
snapshot reads `inpatients: 0` and the `active outpatients` count
climbing as the ten-year horizon plays out — e.g.:

```
-- board snapshot: 2024-02-12T00:37:00Z --

inpatients: 0  active outpatients: 2  discharged: 0  merged: 0
```

The run's own closing summary: `{:snapshot-count 68, :skip-count 41,
:rate 100000.0, :idle-cap-ms 5000, :wallclock-ms 218598, :stream-span-ms
279155640000, :clamped-count 0, :emitted 68}` — every one of the 68
messages rendered a snapshot (no coalescing at this `--board` width),
41 of the 68 inter-message waits exceeded `--idle-cap`'s default 5
seconds and were skipped rather than actually waited out (`-- idle-skip:
stream-time jumped --`, printed immediately before the snapshot each
time), and total wallclock time for the full ten-year stream was
~3m39s. A denser population or a larger `--rate` shortens that; this
scenario's own module mix and five-minute arrival gap, at 200 patients,
is genuinely this sparse in message traffic — most of a "busy Tuesday"
in this scenario's own patient population unfolds as intake and
follow-up over months and years, not everyone arriving on one shift.
