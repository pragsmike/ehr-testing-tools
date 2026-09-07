# Scenario: dense-7500

The scale scenario, and the only one in this tree sized for a
**10^5-event corpus**. `clinic-decade` and `ed-tuesday` are 200- and
100-patient scenarios a reader runs in under a minute; this one runs
for minutes and holds a hospital's worth of concurrent census while it
does. `--config` is [`config.edn`](config.edn); see that file's own
header comment for what is re-authored here and what is frozen.

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

## Three configs, one provenance

| file | what | how it relates to `config.edn` |
|---|---|---|
| [`config.edn`](config.edn) | the skeleton **plus all nine opt-in keys** | the provenance -- these are the bytes the other two are cut from |
| [`config-nobed.edn`](config-nobed.edn) | the same, **less `:bed-cycle`** | `config.edn` with exactly one line deleted |
| [`config-bare.edn`](config-bare.edn) | the skeleton, **no opt-in key at all** | `config.edn`'s own byte PREFIX, plus a closing brace |

**Both siblings are DERIVED, and each rule is a command rather than a
description.** Run from this directory, either line reproduces its own
file byte for byte:

```bash
grep -v '^ :bed-cycle true$' config.edn > config-nobed.edn
sed -n '1,/^ :module-horizon-days /p' config.edn > config-bare.edn && printf '}\n' >> config-bare.edn
```

Edit `config.edn` and re-run both lines; never edit a sibling directly.
Neither derivation is asserted afterwards either -- both are checked,
and `bin/demo-exerciser-dense-7500` checks them on every integration
run -- that script is a step of `make integration`, alongside the
ed-tuesday and clinic-decade exercisers, and it is the one that makes
that tier minutes rather than seconds. The first check is one command, run from the workspace root, and
the exerciser runs it too:

```
$ diff demos/scenarios/dense-7500/config.edn demos/scenarios/dense-7500/config-nobed.edn
2113d2112
<  :bed-cycle true
```

The second is the additive claim the 2026-08-29 record made of its own
three scale points, re-established here: `config-bare.edn` minus its
own closing brace and newline is byte-identical to the first **154,196
bytes** of `config.edn`, sha-256 `5c3d7659...` on both sides. The
exerciser re-derives that length from the files themselves rather than
carrying the number as a literal.

Population-scale, no captured trace -- see this directory's own parent
[`README.md`](../README.md) for why. Generate the scenario's own output
first.

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

**This arithmetic is a mean-value bound and not a simulation.** It
carries no variance term and no census for the module cohort, whose
walk length is the vendored modules' business rather than this file's.
The bound is what sizes the delays; the gate is the run, and all four
cells below complete.

## Generate

```bash
bin/ehrt corpus generate sim --seed 20260824 --patients 7500 --churn \
  --config demos/scenarios/dense-7500/config.edn \
  --out-dir out/scenarios/dense-7500
```

**This is a minutes-long run, not a seconds-long one** -- see the walls
under "What to look for" below. `--out-dir` is rejected if it already
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
is where the case for taking it at 10^6 is argued -- **it is the only
path that reaches that decade**, and the reason is the emitter's peak
heap rather than the log's.

## What to look for

Witnessed 2026-09-07 at `2ba3490c`, seed 20260824, `--churn`, on the
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

**The counts last moved under ADR-0179**, which releases the bed
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
| `config.edn` | 7,500 | **167,197** | **222,819** | **1.3327** | 106.25 s | 1,927 MB |
| `config-nobed.edn` | 7,500 | **125,642** | **165,466** | **1.3170** | 93.34 s | 1,984 MB |
| `config-bare.edn` | 7,500 | **100,868** | **65,457** | **0.6489** | 42.36 s | 1,135 MB |
| `config.edn` | 750 | **33,306** | **40,291** | **1.2097** | 48.20 s | 1,183 MB |

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

**The bed cycle is worth a quarter of this corpus.** 167,197 events
against 125,642 is **41,555 events, 24.9% of the whole log**, and
57,353 messages with them -- every one of it bed housekeeping, on a
scenario that actually boards people, against `clinic-decade`'s 300
where nobody is boarded. The cycle is slightly MESSAGE-RICHER than the
log it rides on -- 1.3327 with it against 1.3170 without, so its own
41,555 events carry **1.3802 messages each** -- which is what a
housekeeping event lowered into its own ADT message looks like.

**Messages per event is still climbing at 10^5.** 1.2097 at 750
arrivals to 1.3327 at 7,500. Read the pair as a direction and not as a
decade: `:persons {:count 15000}` does not shrink with `--patients`, so
the 750 cell carries the SAME 15,000-person demographic timeline as the
7,500 cell with a tenth of the clinical traffic on top of it. **The two
cells are not one scenario an order of magnitude apart**, and no
scaling exponent is quoted from them here for that reason.

## What to look for in the log itself

**All five referential carrier columns are populated**, counted with
`ehrt.corpus.operators`' own `carrier?` predicates over `config.edn`'s
7,500-arrival log, re-measured 2026-09-06 at `4ddf62c2` (A and B1 moved
under ADR-0179; the other three did not):

| column | field | carrier | candidate sites |
|---|---|---|---|
| A | `:cancels-event-id` | the three cancels | **2,251** |
| B1 | `:order-event-id` | `:result-available` | **10,274** |
| B2 | `:order-event-id` | `:medication-end` | **4,884** |
| C | `:start-event-id` | `:care-plan-end` | **3,486** |
| D | `:placeholder-event-id` | `:identity-fill` | **943** |

B1 is now equinumerous with `:order-placed`, which it was not before:
ADR-0179's R-queue carries a result whose subject was merged away over
to the survivor instead of dropping it, and this scenario has **21** of
them -- the first non-empty population for that ruling in this tree.

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

**Not a clinically shaped hospital.** The pathway mix, the ward sizes
and the arrival rate were chosen by the 2026-08-24 spike to hold
concurrent census CONSTANT while the patient count grows, so that a
log-log slope over this scenario measures the algorithm and not a
busier hospital. Two of the nine opt-in keys carry an ambulatory
decade's numbers over acute traffic on purpose, frozen rather than
retuned; `config.edn`'s own block above `:scheduling` names which and
why.

**Not a substitute for the other two scenarios.** To READ a corpus,
read [`../ed-tuesday/`](../ed-tuesday/README.md) or
[`../clinic-decade/`](../clinic-decade/README.md) -- both are small
enough to follow patient by patient, and both ship a `--board`
playback. This one is for measuring.
