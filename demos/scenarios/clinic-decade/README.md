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
bin/ehrt play out/scenarios/clinic-decade --board 60 --rate 10000000
```

Renders a bed-state snapshot every 60 stream-minutes instead of a
message-by-message ticker (`ehrt play`'s own `--board`), at 100,000
stream-seconds per wallclock-second.

Or play the sim's own story directly, from its own event log rather
than the emitted HL7 v2 messages:

```bash
bin/ehrt play out/scenarios/clinic-decade/events.edn --rate 10000000
```

## What to look for

Witnessed 2026-08-26 (seed 20260807, 200 patients, `--config` as
above): **1,136 ground-truth events, 228 HL7 v2 messages, 183 board
snapshots** over a 611,762,027,000 ms (~19.4-year) stream span.

**THE DECADE IS NOW TWO DECADES, and that is the point of the change
that produced these figures.** `config.edn` opted this scenario into
`ehrt.sim-engine.engine`'s demographic fold on 2026-08-26 (arc 3a part
4, ADR-0173 ruling D1's commit 2): the run now carries a POPULATION of
400 people walking twenty years, and every demographic and identity
event of theirs that lands after their own arrival becomes an event in
this log. So the stream no longer stops when the last module walk
does -- it runs to the person horizon, which is what a demographic
timeline IS. Every figure in this section moved with it, and the
previous witness is recorded at the bottom of this block rather than
overwritten.

**The board-snapshot census, 183 snapshots:** `inpatients: 0` 94,
`inpatients: 1` 87, `inpatients: 2` 1, `inpatients: 3` 1.
`bin/demo-exerciser-clinic-decade` re-derives that whole distribution
from this paragraph at runtime and asserts every count exactly, plus
that they exhaust the snapshot lines -- so a future re-witness cannot
silently desync from the check. It used to be a two-way 41/7 split, and
before that a universal `inpatients: 0`; the claim's SHAPE has widened
twice now, each time because the scenario genuinely gained traffic.

**Where the inpatients come from is entirely new.** All 90 admissions
in this run are HOOK-created -- the person stream's own clinical
events, not any module's:

| reason | what it is |
|---|---|
| `Unidentified patient` | an unresponsive arrival nobody can name yet: PID-5 renders `Doe^Unknown`, PID-7/8/11/13 render EMPTY, and no IN1 rides the admission at all |
| `Live birth` | a newborn's first encounter, on a patient of their own whose `:registered` carries `:mother-patient-id` |
| `Delivery` | the parent's own admission at the same instant, when their record is clinically idle |
| `Occupational injury: <class>` | an employed person's ED presentation |

**And seven ADT^A40 merges.** Every one of them is an IDENTIFICATION
merge: a John Doe record joined to the patient the same person already
had. The board's own summary line counts them (`merged: 7`), which is
the first time either scenario has shown a nonzero merge count at all.

The identity traffic, counted: **28 placeholder registrations, 21 of
them later filled in place, 7 merged away; 29 newborns; 2 registrations
for a patient with nowhere to live** (PID-11 absent -- ruling E1, no
sentinel, because HL7 v2 has no code for it and every literal is one
site's local convention).

The run's own closing summary: `{:unparseable-count 0, :snapshot-count
183, :skip-count 0, :rate 1.0E7, :idle-cap-ms 5000, :wallclock-ms
61546, :stream-span-ms 611762027000, :clamped-count 0, :emitted 228,
:unfolded-count 0, :sink "ticker"}` -- 183 of the 228 messages rendered
a snapshot; the others landed in a board window a prior message had
already opened. NOT ONE inter-message wait exceeded `--idle-cap`'s
default 5 seconds, so nothing was skipped and the whole ~19.4-year
stream played out in ~62 seconds. `:wallclock-ms` is the one key
`bin/demo-exerciser-clinic-decade` does NOT assert, as genuinely
run-volatile; every other key in that map it re-derives from this
paragraph and subset-matches against the live run.

**`--rate` MOVED, 100,000 -> 10,000,000, and it is the one taught
command this session changed.** `--rate` is stream-seconds per
wallclock-second, so it has to be read against the stream it is
pacing. At 100,000 this scenario's ten-year span played in about four
minutes; at nineteen-and-a-bit years and triple the events it played
in **nine minutes forty for the board and roughly an hour for the
events.edn ticker**, because most inter-event gaps had grown past
`--idle-cap` and were being skipped one five-second timeout at a time
(`:skip-count` 98). Ten million restores the original experience --
62 seconds for the board, 63 for the ticker, `:skip-count` 0 on both --
and the board census is byte-identical at 100,000, 1,000,000 and
10,000,000, so the number paces the demo and changes nothing it
shows.

**What has NOT changed is the ambulatory character.** The module half
of this scenario is as sparse as it ever was -- 28 outpatient visits,
13 medication orders, 5 care plans and ONE diagnostic report across 200
patients and ten years of clinical horizon, and not one module-compiled
admission at this seed. The inpatient census this scenario now shows is
person-driven, not module-driven, and the two are worth reading
separately: `:citation` is present on every event a module compiled and
absent from every event a hook created.

**Previous witnesses, kept rather than overwritten.** Before the
2026-08-26 opt-in this run produced 68 messages, 48 board snapshots and
a 41/7 `inpatients: 0` / `inpatients: 1` split over a 310,531,980,000
ms span, with its single inpatient a genuine glass-box-cited sepsis
presentation (MRN000074, `{:ward "Emergency", :bed "ED-H04",
:placement :surge}`, cited `{:module "sepsis", :state
:sepsis-ed-encounter}`). ADR-0171's stream partition had moved it there
from a zero-inpatient universal on 2026-08-25; ADR-0103 corrected the
snapshot count from 68 to 48 on 2026-08-11. Seed and module mix are
untouched across all of it -- nothing here was ever tuned to restore a
figure.

**Contrast with [`../ed-tuesday/`](../ed-tuesday/README.md)** (ADR-0104,
2026-08-11): this scenario is population-scale incidence -- sparse,
and ambulatory in everything its MODULES drive; ed-tuesday is a
day-scale, scripted single ED shift with real admissions, transfers,
and discharges driving visible inpatient census on a `--board`.
AMENDED 2026-08-26: "outpatient-only by design" was true of this
scenario until it opted into the demographic fold, and is no longer --
its inpatient census now comes from the person stream's own births,
injuries and unidentified arrivals rather than from any module. The
contrast still holds and is now about WHERE the inpatients come from
rather than whether there are any.
