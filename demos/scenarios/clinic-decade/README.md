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

Witnessed 2026-08-27 (seed 20260807, 200 patients, `--config` as
above): **1,569 ground-truth events, 575 HL7 v2 messages, 248 board
snapshots** over a 611,763,107,000 ms (~19.4-year) stream span.

**AND THIS DECADE BOOKS ITS VISITS.** `config.edn` opted in to
SCHEDULING on 2026-08-27 (arc 3b sweep 3, ADR-0174 section 2(b) and
ruling C), and that is where these figures moved from the previous
witness's 1,456/548/222. A decade of ambulatory care is BOOKED traffic,
so `:scheduled-fraction` is 0.70 here against `ed-tuesday`'s 0.15 --
the one sub-key the two scenarios genuinely disagree about. This run
holds **40 appointments, 6 rescheduled, 9 cancelled and 4 no-showed**,
with **27 encounter openers naming the appointment they were kept
against -- every single one of them an `:outpatient-visit`, i.e. a
SECOND encounter booked at a discharge.**

That last figure is the point of the whole sweep, and it reads
differently here than at `ed-tuesday`: this scenario admits people who
then come back to clinic, so its scheduled openers are follow-ups
almost by definition. NONE of the four scheduling kinds reaches the
wire (the SIU family is v2.4 structure; every message here says MSH-12
`2.3`), which is why the event count rose by 113 while the message
count rose by only 27.

**AND BEDS TAKE TIME TO TURN OVER**, though in this scenario that buys
less than it does at `ed-tuesday`. `config.edn` opted in to the
BED-STATUS CYCLE on 2026-08-27 (arc 3b sweep 2, ADR-0174 section 2(c)
and ruling C), and every one of the 300 events and 300 ADT^A20 messages
it added is bed housekeeping: this decade admits 100 people and BOARDS
NOBODY, so there is no discharge-to-relief coupling here for the cycle
to move. What it does buy is a bed board that shows a room being turned
over rather than a bed blinking from occupied to empty -- which is the
whole of what a nineteen-year ambulatory corpus needed from it.

**PATIENTS COME BACK NOW**, and in this scenario that shows up in one
place: `config.edn` opted in to the lifted ENCOUNTER HORIZON on
2026-08-26 (arc 3b sweep 1, ADR-0174 ruling A1), and the run went from
90 admissions to 100. All ten are PARENT DELIVERIES -- 17 before, 27
now. Until that day a hook could put an encounter only on a patient who
had never had one, so a person delivering a second child in this
population simply produced no admission for it; now they do, on the
same patient-id and the same MRN. Seven patients here have more than
one encounter, ten encounters the pre-sweep engine threw away, and one
patient reaches FOUR. Everything else about the run's clinical content
is unmoved, including all 28 outpatient visits and all seven merges.

**And every message now carries a visit number** -- PV1-19, empty on
every message this project had ever produced before that sweep, renders
the encounter's own `ENC-` id (ADR-0174 ruling C1). The only PV1s
without one here are the five ADT^A40 merges, whose patients' stays had
already ended.

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

**The board-snapshot census, 248 snapshots:** `inpatients: 0` 148,
`inpatients: 1` 98, `inpatients: 2` 1, `inpatients: 3` 1.
`bin/demo-exerciser-clinic-decade` re-derives that whole distribution
from this paragraph at runtime and asserts every count exactly, plus
that they exhaust the snapshot lines -- so a future re-witness cannot
silently desync from the check. It used to be a two-way 41/7 split, and
before that a universal `inpatients: 0`; the claim's SHAPE has widened
twice now, each time because the scenario genuinely gained traffic. Its
COUNTS moved a third time on 2026-08-26 without the shape changing --
ten hours that used to read `inpatients: 0` now read `inpatients: 1`,
which is precisely the ten recovered parent deliveries occupying a bed
-- and a FOURTH time on 2026-08-27, when the bed cycle added 39
snapshots, 38 of them `inpatients: 0`. Those are hours in which the
only traffic is an A20 saying a bed finished being cleaned: real
messages, on a board with nobody on it. A FIFTH time later the same
day, when scheduling added 26 more, EVERY ONE of them `inpatients: 0`
(122 -> 148) while the three occupied buckets held at 98/1/1 exactly.
That is the sweep's own signature and it is worth reading: a follow-up
opens an `:outpatient-visit`, which takes no bed, so every snapshot
scheduling adds is by construction an empty board. The bed cycle
widened the quiet hours; scheduling widened them again for a
completely different reason.

**Where the inpatients come from is entirely new.** All 100 admissions
in this run are HOOK-created -- the person stream's own clinical
events, not any module's (28 `Unidentified patient`, 29 `Live birth`,
27 `Delivery`, 16 `Occupational injury`):

| reason | what it is |
|---|---|
| `Unidentified patient` | an unresponsive arrival nobody can name yet: PID-5 renders `Doe^Unknown`, PID-7/8/11/13 render EMPTY, and no IN1 rides the admission at all |
| `Live birth` | a newborn's first encounter, on a patient of their own whose `:registered` carries `:mother-patient-id` |
| `Delivery` | the parent's own admission at the same instant, when their record is clinically idle |
| `Occupational injury: <class>` | an employed person's ED presentation |

**And seven ADT^A40 merges**, unchanged by the horizon lift. Every one of them is an IDENTIFICATION
merge: a John Doe record joined to the patient the same person already
had. The board's own summary line counts them (`merged: 7`), which is
the first time either scenario has shown a nonzero merge count at all.

The identity traffic, counted: **28 placeholder registrations, 21 of
them later filled in place, 7 merged away; 29 newborns; 2 registrations
for a patient with nowhere to live** (PID-11 absent -- ruling E1, no
sentinel, because HL7 v2 has no code for it and every literal is one
site's local convention).

The run's own closing summary: `{:unparseable-count 0, :snapshot-count
248, :skip-count 0, :rate 1.0E7, :idle-cap-ms 5000, :wallclock-ms
61719, :stream-span-ms 611763107000, :clamped-count 0, :emitted 575,
:unfolded-count 0, :sink "ticker"}` -- 248 of the 575 messages rendered
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
