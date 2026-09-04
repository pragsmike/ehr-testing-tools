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

Neither derivation is asserted; both are checked. The first is one
command, run from the workspace root, and the exerciser runs it too:

```
$ diff demos/scenarios/dense-7500/config.edn demos/scenarios/dense-7500/config-nobed.edn
2092d2091
<  :bed-cycle true
```

The second is the additive claim the 2026-08-29 record made of its own
three scale points, re-established here: `config-bare.edn` minus its
own closing brace and newline is byte-identical to the first **152,872
bytes** of `config.edn`, sha-256 `02d77b97...` on both sides. The
exerciser re-derives that length from the files themselves rather than
carrying the number as a literal.

Population-scale, no captured trace -- see this directory's own parent
[`README.md`](../README.md) for why. Generate the scenario's own output
first.

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

Witnessed 2026-09-04 at HEAD, seed 20260824, `--churn`, on the
traffic-scale programme's own reference machine (WSL2, 6c/12t
i7-10750H, 15 GiB, OpenJDK 21.0.7, JVM defaults as shipped --
`MaxHeapSize` 3.88 GB, `bin/ehrt` sets no JVM options). Warm-up plus
two timed runs per cell, one JVM per run, a fresh spool target per run,
`/usr/bin/time -v` around each; every figure below is the mean of the
two timed runs, and both runs of every completing cell produced the
IDENTICAL event and message counts.

**The measured cells:**

| cell | arrivals | events | messages | msg/event | process wall | peak RSS |
|---|---|---|---|---|---|---|
| `config.edn` | 7,500 | **166,295** | **224,645** | **1.3509** | 276.06 s | 2,204 MB |
| `config-nobed.edn` | 7,500 | **124,999** | **168,869** | **1.3510** | 220.55 s | 1,965 MB |
| `config.edn` | 750 | **33,274** | **41,768** | **1.2553** | 53.64 s | 1,166 MB |

**`config-bare.edn` DOES NOT COMPLETE AT 7,500 ARRIVALS**, and the
reason is capacity rather than anything wrong with the run. Both timed
attempts stopped identically after ~42 s with
`:capacity-exhausted` -- the run refusing an arrival it cannot place,
patient `PID-002963-bfc158cf` for the Surgery ward, with Surgery at
200/200 and Medicine A at 240/240 at the moment of refusal. It is
reported BLOCKED and NOT tuned around; the cell's own row in
[`docs/consuming-ground-truth.md`](../../../docs/consuming-ground-truth.md#scale)
therefore still carries the 2026-08-29 programme's figure rather than a
re-measurement.

**`:persons` IS WHAT KEEPS THE CENSUS INSIDE CAPACITY, which is the
opposite of what the three configs look like.** `config-bare.edn` is
the cheapest of the three by every other measure and is the only one
that stops. With `:persons` present an arrival BINDS to a person, and a
repeat arrival of somebody already registered opens a second encounter
rather than a fresh concurrent stay; without it, all 7,500 arrivals are
distinct patients each holding a bed for their pathway's full dwell.
The opt-in that looks like pure added volume is also a throttle.

**The bed cycle is worth a quarter of this corpus.** 166,295 events
against 124,999 is **41,296 events, 24.8% of the whole log**, and
55,776 messages with them -- every one of it bed housekeeping, on a
scenario that actually boards people, against `clinic-decade`'s 300
where nobody is boarded. The msg/event ratio is unmoved to four places
(1.3509 vs 1.3510), so the cycle adds wire traffic in exact proportion
to the log it adds.

**Messages per event is still climbing at 10^5.** 1.2553 at 750
arrivals to 1.3509 at 7,500. Read the pair as a direction and not as a
decade: `:persons {:count 15000}` does not shrink with `--patients`, so
the 750 cell carries the SAME 15,000-person demographic timeline as the
7,500 cell with a tenth of the clinical traffic on top of it. **The two
cells are not one scenario an order of magnitude apart**, and no
scaling exponent is quoted from them here for that reason.

## What to look for in the log itself

**All five referential carrier columns are populated**, measured over
`config.edn`'s own 7,500-arrival log on 2026-09-04:

| column | field | carrier | candidate sites |
|---|---|---|---|
| A | `:cancels-event-id` | the three cancels | **2,164** |
| B1 | `:order-event-id` | `:result-available` | **10,196** |
| B2 | `:order-event-id` | `:medication-end` | **4,810** |
| C | `:start-event-id` | `:care-plan-end` | **3,449** |
| D | `:placeholder-event-id` | `:identity-fill` | **934** |

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
