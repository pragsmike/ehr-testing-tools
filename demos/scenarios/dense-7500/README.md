# Scenario: dense-7500

The scale scenario, and the only one in this tree sized for a
**10^5-event corpus**. `clinic-decade` and `ed-tuesday` are 200- and
100-patient scenarios a reader runs in under a minute; this one runs
for minutes and holds a hospital's worth of concurrent census while it
does. `--config` is [`config.edn`](config.edn); see that file's own
header comment for what is re-authored here and what is frozen.

**Since 2026-11-20 it is a hospital with a clinic.** Half of the
arrivals that draw a pathway take an authored outpatient visit and the
other half an authored stay, so the log holds community prescriptions
beside inpatient orders in comparable numbers, and since ADR-0190 not
every prescription is filled. "The pathway mix, and
where to set it" below has the weights.

**It exists because three cells of a published table had no runnable
artifact under them.**
[`docs/consuming-ground-truth.md`](../../../docs/consuming-ground-truth.md#scale)'s
Scale section quotes an all-keys cell, a less-`:bed-cycle` cell and a
no-opt-in-key cell. Until 2026-09-04 all three came from session
scratch on one machine -- a `gen-config.py` the 2026-08-24 throughput
spike never committed, and a `gen-v2.py` the 2026-08-29 traffic-scale
close never committed either. **That scratch did not survive a third
arc**, so the configuration is re-authored here from those two records'
own descriptions and committed, and the table's rows now cite this
directory instead of a dead one.

## Four configs, one provenance

| file | what | how it relates to `config.edn` |
|---|---|---|
| [`config.edn`](config.edn) | the skeleton **plus all ten opt-in keys** | the provenance -- these are the bytes the other two are cut from |
| [`config-nobed.edn`](config-nobed.edn) | the same, **less `:bed-cycle`** | `config.edn` with exactly one line deleted |
| [`config-bare.edn`](config-bare.edn) | the skeleton, **no opt-in key at all** | `config.edn`'s own byte PREFIX, plus a closing brace |
| [`config-28024.edn`](config-28024.edn) | the same ten keys, **`:persons` sized for 28,024 arrivals** | `config.edn` with exactly one line changed: `:persons {:count 56048}` |

**The tenth key is `:dispense`, appended 2026-11-18 (ADR-0188).** It
has two halves. An order written during an admission gets one
`:inpatient` dispense, a pharmacy turnaround later (ADR-0189). The
turnaround is drawn from the `:dispense-minutes` range of the ward the
patient is in when the order is written, and every ward here states its
own: Emergency `[5 25]`, the three inpatient wards `[15 60]`. A
prescription the patient takes home is filled `:community`, or not at
all, by a draw (ADR-0190): the facility states
`:community-fill-share 0.80` and `:community-fill-minutes [60 10080]`,
four in five filled, one hour to seven days after the visit closes or
the patient is discharged. All of these are the defaults, written out,
and all are provisional
([`operational-models.md`](../../../components/sim/docs/operational-models.md#pharmacy-turnaround-adr-0189)
has their basis). The draws are on a stream of their own, so unlike the
other nine keys this one reshuffles no ground truth: delete the
dispenses from `config.edn`'s log, renumber, and the log of the same
file without the key is left, event for event. The rendered messages
are not held fixed the same way; see "The counts last moved" below. No
pathway here authors a dispense.

**About three clinic prescriptions in four are filled, days later, and
nearly every admission-written order is dispensed.** The two clinic
pathways that prescribe write one order per visit and leave it open.
Each is filled with probability 0.80, after a lag of one hour to seven
days from the visit's close. **A prescription with no fill is not a
defect**: the patient never filled it, or was merged into another
record before the lag ran out. An admission-written order is dispensed
only if it is still active, and the stay still open, when the
turnaround elapses. `dense-inpatient` writes two orders per stay: the
antibiotic in the ED, before the transfer, and the analgesia order on
the ward, directly after the care plan starts and ahead of a 60 to 180
minute delay. Both run to the stay's last instant, so pharmacy reaches
both. At the three measured arrival counts:

| | 7,500 (`config.edn`) | 750 | 28,024 |
|---|---|---|---|
| medication orders | 4,490 | 488 | 18,166 |
| written during an outpatient visit | 1,925 | 198 | 7,791 |
| of them filled `:community` | 1,463 (76.0%) | 152 (76.8%) | 5,961 (76.5%) |
| unfilled, patient merged away within a week of the close | 66 | 6 | 224 |
| unfilled, nothing else in the log to say why | 396 | 40 | 1,606 |
| lag from the visit's close, minutes (min / median / max) | 63 / 4,990 / 10,078 | 76 / 5,423 / 10,044 | 63 / 4,961 / 10,078 |
| written during an admission | 2,565 | 290 | 10,375 |
| `:inpatient` dispenses | 2,533 | 287 | 10,332 |
| discharge prescriptions (active and undispensed at the discharge) | 32 | 2 | 41 |
| of them filled `:community` | 27 | 0 | 33 |
| never dispensed or filled: the patient was merged away first | 0 | 1 | 2 |
| `:community` fills, all | 1,490 | 152 | 5,994 |
| community fills per inpatient dispense | 0.59 | 0.53 | 0.58 |
| community fills carrying an `:encounter-id` | 0 | 0 | 0 |
| community fills landing inside a later visit or stay | 1 | 0 | 15 |
| order to dispense, ordered in the ED (min / median / max, minutes) | 5 / 15 / 25 | 5 / 15 / 25 | 5 / 15 / 25 |
| order to dispense, ordered on an inpatient ward | 15 / 38 / 60 | 15 / 37 / 60 | 15 / 37 / 60 |

The filled share is under 0.80 because a patient absorbed by a merge
during the lag takes the unfilled prescription with them; the log
cannot say which of the merged rows the draw would have left unfilled
anyway. At 7,500 the visit fills are 1,015 of `dense-clinic-acute`'s
antibiotic, 406 of `dense-clinic-chronic`'s metformin and 42 of the
vendored modules' own orders. The inpatient dispenses are 1,267
antibiotic and 1,266 analgesia. **The discharge prescriptions are all
vendored modules'**: `injuries` and the submodules it calls prescribe
at the instant of an ED discharge, so pharmacy never reaches those
orders, and since ADR-0190 they are filled in the community like a
clinic prescription, counted from the discharge. `config-nobed.edn`
holds the same order, dispense and fill counts as `config.edn`.
Measured 2026-11-20 at `10ae3f71`.

**Until ADR-0190 every clinic prescription was filled, at the instant
its visit closed**: 1,925 community fills at 7,500, and the 32
discharge prescriptions got nothing.

**Until the clinic pathways (2026-11-20) the community side was 48
fills at 7,500**, all of them the vendored modules', against 4,912
inpatient dispenses.

**Until 2026-11-19 about half of the admission-written orders were
never dispensed**, and that was the pathway's doing: it wrote the
analgesia order at the stay's last instant, where its end shared the
instant and pharmacy never reached it (2,444 of 4,938
admission-written orders at 7,500). The order moved earlier; no step
was added or removed, and the drug codes are as they were.

**All three siblings are DERIVED, and each rule is a command rather
than a description.** Run from this directory, each line reproduces its
own file byte for byte:

```bash
grep -v '^ :bed-cycle true$' config.edn > config-nobed.edn
sed -n '1,/^ :module-horizon-days /p' config.edn > config-bare.edn && printf '}\n' >> config-bare.edn
sed 's/^ :persons {:count 15000 :years 20}$/ :persons {:count 56048 :years 20}/' config.edn > config-28024.edn
```

Edit `config.edn` and re-run all three lines; never edit a sibling
directly. **`config-28024.edn` is `config.edn`'s own rule applied at a
larger arrival count** -- `:persons :count` is twice the arrivals, and
twice 28,024 is 56,048 -- and it exists because that cell was run and
is quoted below; its derivation is checked by
`ehrt.docs-tooling.dense-7500-figures-test` rather than by the
exerciser, since `make test` runs the former and the exerciser does not
run that cell.

Neither of the first two derivations is asserted afterwards either --
both are checked,
and `bin/demo-exerciser-dense-7500` checks them on every integration
run -- that script is a step of `make integration`, alongside the
ed-tuesday and clinic-decade exercisers, and it is the one that makes
that tier minutes rather than seconds. The first check is one command, run from the workspace root, and
the exerciser runs it too:

```
$ diff demos/scenarios/dense-7500/config.edn demos/scenarios/dense-7500/config-nobed.edn
2214d2213
<  :bed-cycle true
```

The second is the additive claim the 2026-08-29 record made of its own
three scale points, re-established here: `config-bare.edn` minus its
own closing brace and newline is byte-identical to the first **160,419
bytes** of `config.edn`, sha-256 `45fdc8ad...` on both sides. The
exerciser re-derives that length from the files themselves rather than
carrying the number as a literal.

Population-scale, no captured trace -- see this directory's own parent
[`README.md`](../README.md) for why. Generate the scenario's own output
first.

## The pathway mix, and where to set it

**Six weighted pathways, and the weights are the dial.** Every arrival
that is not one of the 937 module ordinals draws one pathway from
`config.edn`'s `:pathways` pool, by weight:

| pathway | what it is | weight |
|---|---|---|
| `dense-inpatient` | ED, then a Medicine A stay; two medication orders | 45 |
| `dense-fast` | worked up in the ED and sent home | 35 |
| `dense-surgical` | ED, then Surgery | 20 |
| `dense-clinic-acute` | outpatient visit; a short-course antibiotic | 50 |
| `dense-clinic-chronic` | outpatient visit; a long-term medication (metformin) | 20 |
| `dense-clinic-review` | outpatient visit; nothing prescribed | 30 |

The outpatient share of pathway arrivals is the last three weights
over all six: 100 of 200, one half. The mix of visits is the last
three among themselves: 50% acute prescription, 20% long-term
prescription, 30% none. Change a `:weight` and re-derive the siblings;
because a weight applies to every arrival, the share holds at any
`--patients`.

**All three clinic figures are provisional.** One half, and 50 / 20 /
30, are the author's estimates and have no source behind them.

**A real hospital is far more outpatient than one half.** For context,
and not as what this scenario models: the American Hospital
Association's annual survey figures for 2016 give about 143 million
emergency department visits and 605 million outpatient visits against
roughly 35 million admissions
([AHA](https://www.aha.org/taxonomy/term/140); AHA Fast Facts 2017).
This scenario stays inpatient-heavy on purpose, because the inpatient
stay is where the bed cycle, the transfers and most of the event
volume are. The weight is there to be turned.

**What the weights do not control.** About 82 in 100 pathway arrivals
open an encounter: `:scheduling` books 70% of arrivals, and a booking
can be cancelled or missed. At 7,500 the three clinic pathways opened
2,712 visits (1,342 / 534 / 836) and the three stay pathways 2,721
admissions (1,268 / 921 / 532). The log holds other encounters no
weight here sets: 1,336 follow-up visits `:scheduling` books at a
discharge, 407 vendored-module visits, and 2,655 admissions no
pathway here authors (births and deliveries, occupational injuries,
unidentified patients).

**Nothing marks the long-term order as long-term.** An authored
medication order carries no chronic flag in the pathway IR. The
citation's state name, `:chronic-start`, is the only sign, and nothing
reads it. A refill rule would need a real marker; none exists yet.

**The drug codes are lifted, not chosen clinically.** Every code is one
a vendored module already carries: the antibiotic is `sinusitis.json`'s,
the metformin `metabolic_syndrome/medications.json`'s, the observation
the body-temperature LOINC code the stay pathways use.

## Why the dwells are what they are

**A pathway's delays are not clinical realism here; they are the dial
that sets concurrent census, and they were set WRONG the first time.**
As first committed on 2026-09-04, `dense-inpatient` held a Medicine A
bed for a mean 1,020 minutes, and `config-bare.edn` at 7,500 arrivals
stopped on `:capacity-exhausted` 2,963 arrivals in. The delays were
shortened the same day (ruling R-b); the ward sizes are the 2026-08-24
spike's own facts and did not move.

The bound is Little's law, one ward at a time. `:arrival-gap 2` draws a
uniform integer 0-2 minutes between arrivals, so **lambda = 1.00
arrivals per minute**. The module cohort takes every eighth ordinal
(12.5%) and the remaining 87.5% splits by weight 45 / 35 / 20, giving a
per-pathway arrival rate of **0.394 / 0.306 / 0.175 per minute**. A
`:delay` is a uniform integer draw in `[:from, :to]` minutes, so a
pathway's mean dwell on a ward is the sum of the `(from + to) / 2` it
spends there. Census is then rate times dwell:

| ward | beds + surge | fed by | dwell before | census before | dwell now | census now |
|---|---|---|---|---|---|---|
| Emergency | 180 + 40 = 220 | all three, pre-transfer | 142.5 / 247.5 / 60 | 142.4 | 50 / 247.5 / 60 | **106.0** |
| Medicine A | 200 + 40 = 240 | `dense-inpatient` | 1,020 | **401.6** | 255 | **100.4** |
| Surgery | 160 + 40 = 200 | `dense-surgical` | 990 | 173.3 | 990 | **173.3** |
| Medicine B | 200 + 40 = 240 | overflow only | -- | -- | -- | -- |

**Medicine A at 401.6 against a 240-bed ward is the whole defect**, and
it is 167% of the ward before a single overflow. The allocation ladder
(`ehrt.sim-model.facility/allocate`) spills the excess into the other
inpatient wards' LICENSED beds and then into ED surge, which is how a
Medicine A overrun surfaced as Surgery 200/200 in the refusal payload.
Shortening `dense-inpatient`'s five delays to 10-30 / 15-45 / 30-120 /
60-180 / 30-90 puts that ward at 42% of its own capacity and the ED at
48% of its; total inpatient census is 273.7 against 680 reachable
inpatient beds.

**Since 2026-11-20 every "census now" figure above is halved.** The
clinic pathways take half of the weighted arrivals and hold no bed, so
the three stay pathways arrive at **0.197 / 0.153 / 0.088 per minute**
and the same arithmetic gives Emergency 53.0, Medicine A 50.2 and
Surgery 86.6. The wards were not resized: they are sized for twice the
traffic they now see. Counted from `config-bare.edn`'s 7,500-arrival
log over the middle nine tenths of its admissions, the mean census is
Emergency **65.1** (peak 90), Medicine A **48.7** (peak 73) and
Surgery **69.2** (peak 92); the same count over the log before the
clinic pathways gave 123.8, 98.6 and 141.6. The ED runs above its
bound because the module cohort and the churn the bound leaves out
land there too. With `:scheduling` spreading arrivals over weeks,
`config.edn`'s own mean census is far lower: 10.5, 7.7 and 12.3.

**This arithmetic is a mean-value bound and not a simulation.** It
carries no variance term and no census for the module cohort, whose
walk length is the vendored modules' business rather than this file's.
The bound is what sizes the delays; the gate is the run, and all four
cells below complete.

**The module cohort is ENUMERATED, not a rule, and it stops at 7,496.**
"Every eighth ordinal" is what `config.edn`'s `:module-assignment`
spells out row by row -- ordinals 8, 16, ... 7,496, **937 rows** -- and
`:pathways` pins the same 937 ordinals to `module-only` by the same list.
`--patients` above 7,500 does not scale it: no ordinal past 7,496 is
assigned a module, so every arrival beyond that point takes one of the
six weighted pathways. The witness is the 28,024 cell's own
`ehrt sim describe` row, read over the `sim run` envelope so the
manifest's assignment record is present:
`pathway module-only configured yes observed 937 (892 subjects)` --
937 of 28,024 arrivals, 3.3% of them rather than 12.5%.

## Generate

```bash
bin/ehrt corpus generate sim --seed 20260824 --patients 7500 --churn \
  --config demos/scenarios/dense-7500/config.edn \
  --out-dir out/scenarios/dense-7500
```

**This is a minutes-long run, not a seconds-long one** -- see the walls
under "What to look for" below. The fourth config, at 28,024 arrivals,
is generated on the ground-truth-only path instead; its command is in
the next section. `--out-dir` is rejected if it already
exists and is non-empty; remove or rename a prior run's own directory
before regenerating.

## The ground-truth-only path

The command above generates, self-checks, renders HL7 v2 and spools the
lot. A consumer who wants only the event log -- the public, versioned
contract both shipped emitters read -- takes `--format ground-truth`
and pays for none of the emission half:

```bash
bin/ehrt sim run --seed 20260824 --patients 750 --churn --config demos/scenarios/dense-7500/config.edn --format ground-truth > out/scenarios/dense-7500-750.edn
```

`--patients 750` deliberately, not 7,500: this is the shape
demonstration, and the decade below the headline cell runs in a
fraction of its time. The same flag works at any arrival count, and
[`docs/consuming-ground-truth.md`](../../../docs/consuming-ground-truth.md#scale)
is where the case for taking it at scale is argued -- **it is the
cheaper path on heap, by a margin that widens as the corpus grows**,
because the emitter's peak rather than the log's is what binds first.
Whether it is the ONLY path that reaches 10^6 is not something this
repository has run.

**The largest cell this repository HAS run is the fourth config**, at
28,024 arrivals and on this path, with the heap set explicitly. Run it
from the workspace root -- `bin/ehrt` sets no JVM options, so this
calls the same `:ehrt` alias the wrapper does, with `-Xmx8g` in front:

```bash
clojure -J-Xmx8g -M:ehrt sim run --seed 20260824 --patients 28024 --churn --config demos/scenarios/dense-7500/config-28024.edn --format ground-truth > out/scenarios/dense-7500-28024.edn
```

It takes minutes, writes about 190 megabytes, and is not run by the
exerciser; its figures are under "What to look for" below, in their own
table.

## What to look for

Witnessed 2026-11-20 at `10ae3f71`, seed 20260824, `--churn`, on the
traffic-scale programme's own reference machine (WSL2, 6c/12t
i7-10750H, 15 GiB, OpenJDK 21.0.7, JVM defaults as shipped --
`MaxHeapSize` 3.88 GB, `bin/ehrt` sets no JVM options). One warm-up
run, then two timed runs per cell, one JVM per run, a fresh spool
target per run, `/usr/bin/time -v` around each; every figure below is
the mean of the two timed runs, and both runs of every one of the four
cells wrote a BYTE-IDENTICAL `events.edn` and spooled the same number
of messages. The wall is the whole `bin/ehrt corpus generate sim`
PROCESS, JVM startup included, and not an in-process phase total; peak
RSS is that process's own maximum resident set size, in MiB.

**Not one figure below lives in this file.** They live in
[`figures.edn`](figures.edn), which
[`docs/consuming-ground-truth.md`](../../../docs/consuming-ground-truth.md#scale)'s
Scale table quotes too, and `ehrt.docs-tooling.dense-7500-figures-test`
fails when either document stops agreeing with it.
`bin/demo-exerciser-dense-7500` rewrites that file's counts from its
own artefacts on every `make integration` run, so a moved count is a
dirty tree at the run rather than a paragraph nobody re-reads. Before
both existed, these cells sat at a measurement taken one commit before
ADR-0180's site 1 and survived all seven of its sites without one of
them going red.

**The counts last moved under ADR-0190**, on 2026-11-20: a community
fill is now a draw (above). The all-keys cell went from 1,925
community fills to 1,490, which is **-435** events there and at
`config-nobed.edn`, **-46** at 750, and nothing at `config-bare.edn`.
No other ground-truth event moved: the two logs, less their dispenses,
are the same events in the same order. Messages moved by **+13**,
**+11**, **0** and **+5**, for the reason given under ADR-0189 below.
The walls fell in every cell, the bare one included, whose counts did
not move, so that fall is not attributed to this change. The figures
before it are `figures.edn`'s `:provenance :prior`.

**Before that, the counts moved on 2026-11-20, at `2edf7a4e`**, with a scenario
edit and no ADR: three weighted clinic pathways joined the pool and
took half of the arrivals that draw one (above). A clinic visit is six
steps and holds no bed, so every cell lost events: **-45,691** at the
all-keys cell, **-29,228** at `config-nobed.edn`, **-33,289** at
`config-bare.edn` and **-4,118** at 750, and messages fell by
**-51,156**, **-34,546**, **-25,373** and **-4,787**. Under the
`:dispense` key the all-keys cell went from 48 `:community` fills to
1,925 and from 4,912 `:inpatient` dispenses to 2,533. The distinct
subjects at the all-keys cell and at 750 did not move (8,522 and
2,797). The walls did not fall with the events: the all-keys cell took
134.18 s against 127.35. The figures before it are `figures.edn`'s
`:provenance :prior-1`.

**Before that, the counts moved on 2026-11-19, at `b6fb4ab3`**, with a scenario
edit and no ADR: `dense-inpatient` writes its analgesia order mid-stay
(above), so under the `:dispense` key that order is dispensed. That is
**+2,451** dispenses at the all-keys cell, **+2,460** at
`config-nobed.edn` and **+244** at 750. This time the rest of the log
moved as well. The moved step changes what a stay cut short has
already done and where churn's insertions fall among the steps, and
every later draw on a shared stream follows: events moved by
**+2,643**, **+2,678**, **-51** and **+273**, and messages by
**+148**, **+138**, **-63** and **+27**. `config-bare.edn` moved with
no key at all, which is how the edit is known to be the cause and not
the dispenses. Subjects did not move in any cell. The figures before
it are `figures.edn`'s `:provenance :prior-2`.

**Before that, the counts moved under ADR-0189**: the `:dispense` key now
derives an `:inpatient` dispense for each order written during an
admission. That is **+2,461** events at the all-keys cell and at
`config-nobed.edn`, **+247** at 750, and nothing at `config-bare.edn`.
No other ground-truth event moved. Messages moved by **-17**, **+4**,
**0** and **-5**. A dispense renders no message of its own, but the
emitter takes one draw per ground-truth event to decide its
restatement chatter and its latency, so adding events to the log
shifts which ADT^A08 and ADT^A31 restatements fire: at 750 the whole
difference is six ADT^A08. The walls fell in every cell, the bare one
included, whose counts did not move, so that fall is not attributed to
this change. The figures before it are `figures.edn`'s
`:provenance :prior-3`.

**Before that, the counts moved under ADR-0188**, two ground-truth changes
declared as one. First, an end that names its opening by state now
references the latest opening no earlier end references, where it used
to reference the first; that moves facts carried on `:registered`
events and a handful of messages, and no event count. Second, the
`:dispense` key adds one `:community` fill per order an outpatient
visit wrote: **+48** events at the all-keys cell and at
`config-nobed.edn`, **+3** at 750, and nothing at `config-bare.edn`,
which does not carry the key. Messages moved by **+2**, **+5**, **0**
and **-1**; part of that is the key's own, for the reason given above,
and not the reference rule's as first recorded. The walls rose
again in every cell, the bare one included, whose counts did not move
at all, so that rise is not attributed to this change. The figures
before it are `figures.edn`'s `:provenance :prior-4`.

**Before that, the counts moved under ADR-0185**, the ground-truth rebaseline:
a legal `:cancel-discharge` now owes a closing `:discharge` after a 4-
to 24-hour delay, and churn picks its merge and bed-swap peers in
`:patient-id` order rather than hash order. Events moved little --
**+518** at the all-keys cell, **+58** at `config-nobed.edn`, **+252**
at `config-bare.edn`, **-10** at 750 -- and messages moved a lot where
chatter runs: **-45,848**, **-30,580**, **+186** and **-4,365**. The
difference is the periodic ADT^A08 a stay that never closed kept
emitting to the end of the run (A08 at the all-keys cell: 52,091 ->
5,520). The walls rose 14-22% in every cell, the bare one included,
whose counts barely moved, so that rise is not attributed to the
rebaseline. The figures before it are `figures.edn`'s
`:provenance :prior-5`.

**Before that, the counts moved under ADR-0179**, which releases the bed
an absorbed record was holding at the instant of a merge and carries a
pending result across to the survivor. Every one of the four moved, and
not all in the same direction -- the all-keys cell by **+7** events,
`config-nobed.edn` by **-183**, `config-bare.edn` by **-16**, the 750
cell by **+3** -- because `--churn` merges bed-holders in all of them.
Those four deltas are DERIVED and not attributed: all four cells were
regenerated at `007deea6`, the commit before ADR-0179's first code
commit, and all four reproduced the figures this file carried before to
the event, which also settles that ADR-0178 (landed between the
2026-09-04 measurement and `007deea6`) moved no count here. The
per-event derivation is
[`.agents/plans/2026-09-05-7-event-divergence.md`](../../../.agents/plans/2026-09-05-7-event-divergence.md).

**The walls moved again on 2026-09-07 and the counts did not**, which
is the distinction this table is now gated to keep. ADR-0180's
seven-site programme shortened the generate phase without touching the
log: `config.edn` at 7,500 wrote an `events.edn` whose sha-256 is
`096959857076f2...`, the same digest the 7-event derivation recorded
for this cell before any of those sites landed, and all four cells
reproduced their event and message counts to the event. What moved is
the wall -- 278.64 -> 106.25 s at the headline cell, 231.14 -> 93.34,
142.12 -> 42.36, and only 54.37 -> 48.20 at 750 arrivals, which barely
moves because that cell is dominated by the same 15,000-person
demographic timeline the programme did not touch. Peak RSS fell with
them except at `config-nobed.edn`, whose own two timed runs differ by
97 MiB: read that column as a GC high-water mark under a 3.88 GB
default heap, not as a footprint.

**The measured cells:**

| cell | arrivals | events | messages | msg/event | process wall | peak RSS |
|---|---|---|---|---|---|---|
| `config.edn` | 7,500 | **126,741** | **125,961** | **0.9938** | 105.08 s | 1,687 MB |
| `config-nobed.edn` | 7,500 | **101,224** | **100,498** | **0.9928** | 93.17 s | 1,487 MB |
| `config-bare.edn` | 7,500 | **67,780** | **40,207** | **0.5932** | 41.25 s | 1,114 MB |
| `config.edn` | 750 | **29,655** | **31,165** | **1.0509** | 54.22 s | 1,082 MB |

**The ground-truth-only cell** is a different measurement, in its own
table because it has no message column to fill: `config-28024.edn`,
measured 2026-11-20 at `10ae3f71` on the same machine, ONE run of the
`sim run --format ground-truth` command under "The ground-truth-only
path" above, `-Xmx8g` explicit, `/usr/bin/time -v` around the whole
process. Its log is 188,719,656 bytes with sha-256
`bd4dec16e8358184...cf2dcd24c5222f`, and it carries 10,332 `:inpatient`
and 5,994 `:community` dispenses. Events and subjects are
`ehrt sim describe`'s own counts over it. **The log this cell wrote
from the clinic pathways until ADR-0190** -- 189,203,716 bytes, sha-256
`d50929d2616fa983...6618dbc79b9c76`, at `2edf7a4e` -- remains a valid
artifact of that pin, and `figures.edn` keeps its figures under
`:pre-adr-0190`. **The log this cell wrote
from the analgesia order's move until the clinic pathways** --
270,551,734 bytes, sha-256 `26e31b4f44df3c9c...e8bea845b652b0`, at
`b6fb4ab3` -- remains a valid artifact of that pin, and `figures.edn`
keeps its figures under `:pre-clinic`. **The log this cell wrote
from ADR-0189 until the analgesia order moved** -- 267,680,351 bytes,
sha-256 `ce0982dec2e5d0dc...4b0ad9d961ce`, at `8acd6028` -- remains a
valid artifact of that pin, and `figures.edn` keeps its figures under
`:pre-order-move`. **The log this cell wrote
from ADR-0188 until ADR-0189** -- 264,575,690 bytes, sha-256
`5fb8187b5aef0bef...beef5d8cc6c0`, at `3ba95de9` -- remains a valid
artifact of that pin, and `figures.edn` keeps its figures under
`:pre-adr-0189`. **The log this cell wrote
from the ground-truth rebaseline until ADR-0188** -- 264,570,024 bytes,
sha-256 `ee59219eb81eb07f...8c96dd29a4`, at `fecbe43b` -- remains a
valid artifact of that pin, and `figures.edn` keeps its figures under
`:pre-adr-0188`. **The log this cell wrote
before the ground-truth rebaseline** (ADR-0185) -- 263,570,096 bytes,
sha-256 `589600c5a8051ff2...549cc335`, at `dac3d9c8` -- was reproduced
byte for byte on another host and JDK build; it remains a valid,
self-check-clean pre-rebaseline artifact, and `figures.edn` keeps its
figures under `:pre-rebaseline`.

| cell | arrivals | events | subjects | bytes | process wall | peak RSS |
|---|---|---|---|---|---|---|
| `config-28024.edn` | 28,024 | **502,436** | **32,080** | **188,719,656** | 325.65 s | 3,215 MB |

Its wall is NOT comparable with the table above -- a `sim run` that
emits nothing, against a `corpus generate` that also renders, checks
and spools -- and neither is its module cohort, which is the
7,500-arrival run's (see "Why the dwells are what they are").

**`:scheduling` IS WHAT SPREADS THE CENSUS, and it is worth knowing
before you cut the opt-in keys down.** `config-bare.edn` is the
cheapest of the three by every other measure and it is the one that
runs nearest to capacity, because a config with no `:scheduling` key
admits every arrival the minute it walks in. With `:scheduling`
present, `:scheduled-fraction` of the arrivals are booked instead, and
the whole visit runs `:lead-time-days` LATER -- `decide :appointment`
sets `scheduled-t` to the booking instant plus the lead, and the
outcome bands mean 8% are cancelled and 15% no-show, producing no visit
at all. At the 0.70 / [3 21] / 0.08 / 0.15 this file carries, that
takes a 5.2-day arrival stream, defers 54% of it across a three-week
window and drops 16% of it outright. **The opt-in that looks like pure
added volume is also a throttle**, and the bare cell is the one running
without it.

**The bed cycle is worth a fifth of this corpus.** 126,741 events
against 101,224 is **25,517 events, 20.1% of the whole log**, and
25,463 messages with them -- every one of it bed housekeeping, on a
scenario that actually boards people, against `clinic-decade`'s 300
where nobody is boarded. Its own 25,517 events carry **0.9979 messages
each** -- one ADT message per housekeeping event, which is what a
status change lowered into its own message looks like. (Before the
ground-truth rebaseline the cycle read as message-RICHER than the log
it rides on; that was the periodic chatter of reinstated stays that
never closed, which ran longer on the bed-cycle run.)

**Messages per event no longer climbs at 10^5.** 1.0509 at 750
arrivals to 0.9938 at 7,500. Until the ground-truth rebaseline it rose
(1.2097 to 1.3327): a reinstated stay never closed, so its periodic
ADT^A08 chatter ran to the end of the run, and the longer run carried
more of it -- at 7,500 arrivals A08 fell from 52,091 messages to 5,520
when those stays began to close. Read the pair as a direction and not
as a decade: `:persons {:count 15000}` does not shrink with `--patients`, so
the 750 cell carries the SAME 15,000-person demographic timeline as the
7,500 cell with a tenth of the clinical traffic on top of it. **The two
cells are not one scenario an order of magnitude apart**, and no
scaling exponent is quoted from them here for that reason.

## What to look for in the log itself

**All five referential carrier columns are populated**, counted with
`ehrt.corpus.operators`' own `carrier?` predicates over `config.edn`'s
7,500-arrival log, re-measured 2026-11-20 at `2edf7a4e` (every column
is smaller than it was: half the stays that carried them are now
clinic visits):

| column | field | carrier | candidate sites |
|---|---|---|---|
| A | `:cancels-event-id` | the three cancels | **1,187** |
| B1 | `:order-event-id` | `:result-available` | **5,250** |
| B2 | `:order-event-id` | `:medication-end` | **2,528** |
| C | `:start-event-id` | `:care-plan-end` | **1,787** |
| D | `:placeholder-event-id` | `:identity-fill` | **929** |

B1 is equinumerous with `:order-placed`, which it was not before
ADR-0179: its R-queue carries a result whose subject was merged away
over to the survivor instead of dropping it. This log holds **3**
results whose subject is not their order's (21 when that ruling
landed, at twice the stays).

This is the whole matrix, and it is why this scenario is worth more
than its walls. Until it landed, three of those five columns were
POPULATION GAPS -- convictable in principle, unwitnessable in practice,
because no config in this tree produced a single candidate site for
them
(`.agents/plans/2026-09-01-event-mutation-population-ledger.md`
section 6). Column A needs a cancel and neither `clinic-decade` nor
`ed-tuesday` produces one; B2 and C need a `:medication-end` and a
`:care-plan-end`, which the pathways here author with citations and
neither of those two does.

## What this scenario is NOT

**Not a clinically shaped hospital.** The stay pathways, the ward sizes
and the arrival rate were chosen by the 2026-08-24 spike to hold
concurrent census CONSTANT while the patient count grows, so that a
log-log slope over this scenario measures the algorithm and not a
busier hospital. Two of the ten opt-in keys carry an ambulatory
decade's numbers over acute traffic on purpose, frozen rather than
retuned; `config.edn`'s own block above `:scheduling` names which and
why.

**Not a substitute for the other two scenarios.** To READ a corpus,
read [`../ed-tuesday/`](../ed-tuesday/README.md) or
[`../clinic-decade/`](../clinic-decade/README.md) -- both are small
enough to follow patient by patient, and both ship a `--board`
playback. This one is for measuring.
