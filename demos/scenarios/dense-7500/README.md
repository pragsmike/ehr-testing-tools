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

*Measured figures land here in this session's step 5, and the exerciser
re-derives them from this prose rather than carrying them as literals.*

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
