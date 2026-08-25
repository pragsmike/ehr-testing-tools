# 2026-08-24 -- throughput spike: the scaling exponent of generate -> check

Measurement session (pre-arc-1), penny / WSL / JDK 21. HEAD at start
`ff45ad1`, tree clean. **No `src`, `test`, `schema` or vendored change** (F1):
the driver and the scenario configs are scratch, and the only files this
session writes are the program plan, the roadmap and this record. One commit,
local only -- no push, no tag.

**Result, one sentence:** on a dense synthetic scenario at 1,001 / 10,232 /
104,851 events, **both** phases are quadratic-shaped -- generate slope 0.989
then **1.786**, check 1.471 then **1.814** -- and the check side, which arc 3's
scope did not name at all, is the larger of the two quadratics (69% of the
work at 10^5, coefficient 2.2x the generate side), while the single worst
generator site is a full `replay` inside the cancel decides rather than either
ADR-0164 scan.

## Step 0 -- environment and health record

    $ date -Is        -> 2026-08-24T19:14:17-04:00
    $ git log --oneline -1   -> ff45ad1 (the prompt's own base)
    $ git status --porcelain -> empty
    $ git rev-parse --abbrev-ref HEAD -> main

    $ uptime          -> 19:14:17 up 4:03, load average 0.14, 0.47, 0.48
    $ uptime -s       -> 2026-08-24 15:10:46   (the same post-reboot VM the
                         residual probe measured 13m59s on)
    /proc/stat since boot: busy 1.55%, idle 98.45%, steal 0
    $ free -h         -> 15Gi total, 1.1Gi used, 13Gi free, 0B swap used
    $ df -hT .        -> /dev/sdd ext4 251G, 26% used   (NOT /mnt/c)
    $ nproc           -> 12

    $ java -version
    openjdk version "21.0.7" 2025-04-15
    OpenJDK Runtime Environment (build 21.0.7+6-Ubuntu-0ubuntu120.04)
    OpenJDK 64-Bit Server VM (build 21.0.7+6-Ubuntu-0ubuntu120.04, mixed mode, sharing)
    $ readlink -f $(which java) -> /usr/lib/jvm/java-21-openjdk-amd64/bin/java

    Default heap ceiling, the figure every run below was taken under:
    $ java -XX:+PrintFlagsFinal -version | grep MaxHeapSize
      MaxHeapSize = 4162846720   ->  3.88 GB   (ergonomic, 1/4 of 15Gi)
    `bin/ehrt` sets no JVM options and the Makefile names none, so 3.88 GB
    IS the shipped ceiling -- not a driver choice.

    Windows side, 19:14 (interop alive):
      LoadPercentage x3        -> 4, 3, 3
      wslhost.exe              -> 5 procs, thread counts 4/1/1/4/1, all created
                                  15:10:49 / 15:13:06 -- no orphan, no dead parent
      powercfg /getactivescheme -> High performance
      Win32_Battery            -> BatteryStatus 2 (AC), 100%
      Win32_Processor          -> Max 2592 / Current 2592 MHz

## Step 1 -- the dense scenario, and the premise it corrected

### F5-1 (finding, premise correction): a module-only dense scenario does not exist

The prompt's step 1 asks for a mix "drawn from the vendored set weighted
toward high-emission modules". Measured first rather than assumed. One run
per vendored module, 20 patients, 3,650-day horizon, seed 7, empty
authored pathway (`ehrt.sim.run/incompatible-assignments` rejects a module
alongside the default admission-bearing pathway, so the empty pathway is
forced -- clinic-decade uses the same shape):

| events/patient | modules |
| --- | --- |
| 3.40 | `sore_throat`, `med_rec` |
| 3.15 | `bronchitis`, `injuries` |
| 3.00 | `metabolic_syndrome_care` |
| 2.90 | `veteran_substance_abuse_treatment` |
| 2.80 | `sinusitis` |
| 1.40-1.50 | `ear_infections`, `urinary_tract_infections`, `colorectal_cancer` |
| 1.15 | `sleep_apnea` |
| **1.00** | **the other 19 measured** -- `asthma`, `fibromyalgia`, `dementia`, `appendicitis`, `total_joint_replacement`, `sepsis`, `allergic_rhinitis`, `anemia___unknown_etiology`, `attention_deficit_disorder`, `dermatitis`, `hypothyroidism`, `osteoarthritis`, `osteoporosis`, `rheumatoid_arthritis`, `veteran_lung_cancer`, `veteran_prostate_cancer`, `veteran_ptsd`, `veteran_self_harm`, `vhd_pulmonic`, `vhd_tricuspid` |

**1.00 events/patient is the `:registered` event and nothing else** --
nineteen of thirty-one vendored modules emit no clinical content at all
over a decade at this seed. The best emitter in the tree yields 3.4. A
module-only corpus therefore reaches 10^5 events only at ~30,000 patients,
which is the *sparse* regime the spike exists to escape: it would measure
the population term, not density. This is the same low-incidence mechanism
`ed-tuesday`'s own config header discloses and `roadmap.md#ed-tuesday-
module-tail-inert` already rows, now quantified across the whole set.

### What was built instead, and why it is still the shipped path

Authored pathways carry the density; a 1-in-8 module cohort carries the
vendored content. Both halves are legal, ordinary config -- `pathway.clj`'s
own `Step` schema says of `:procedure`/`:observation`/`:medication-order`/
`:medication-end` that they "are compile targets first, author-facing IR
second; nothing stops a scenario author from writing one directly". The
citations on the authored medication/care-plan spans are deliberately
**module-coordinate-shaped and identical across every patient** -- exactly
what a real GMF module produces and exactly the collision ADR-0164's two
decide-time scans must search past.

Cohorts are disjoint by explicit ordinal, the pattern `ed-tuesday` proves:
every 8th ordinal gets an explicit empty-pathway override plus an explicit
`:module-id`, so no ordinal is ever certain to receive both an
encounter-opening pathway and a module.

## Which functions were measured -- the path measured IS the path shipped

`bin/ehrt` -> `clojure -M:ehrt` -> `ehrt.cli.core/sim-run-command` ->
`ehrt.corpus.interface/sim-run!` -> **`ehrt.sim.run/run-command`**.

**F5-2 (finding, phase-boundary premise).** The prompt's step 2 asks for
"generate-only" and "check-only" walls as if `ehrt sim run` and `ehrt sim
check` were the two phases. They are not the boundary in the tree.
`run-command` runs the invariant catalog **inside** the generate verb:

```clojure
engine-result (engine-run-fn engine-opts)
{:keys [ground-truth facility providers exhausted]} engine-result
checked (when (and (not exhausted) (not (result/error? engine-result)))
          (check/check-all ground-truth facility warm-up-seconds))
```

So a single `ehrt sim run` already pays generate + check, and the separate
`ehrt sim check` verb is a *third* thing: it reads EDN off stdin and calls
the **1-arg** `check-all` arity (default facility, warm-up 0), not the
3-arg arity the run itself uses. The driver measures the 3-arg,
real-facility call `run-command` makes -- the one that gates every run --
and brackets it separately from `engine/run`. That is the only place in the
tree where the two phases are separable at all.

The driver is a transcription of `run-command`'s own `:else` branch:
`merge-config-file` -> `incompatible-assignments` -> `resolve-modules` ->
`effective-churn-profile` -> `(merge (select-keys opts engine/config-keys)
{:seed .. :churn-profile ..})` -> `engine/run` -> `check/check-all`. It
calls the public `ehrt.sim.run` fns directly; nothing is reimplemented.
What it does NOT pay, deliberately, is JVM start, classpath load and
namespace load -- see the startup figure in step 2, which is exactly why
the 13.89s clinic-decade number extrapolated to nothing.

## Step 2 -- the measurement matrix

Every run: `seed 20260824`, the generated dense config, one warm-up then
two timed runs per cell, each run its own JVM, in-process nanoTime
brackets around `engine/run` and `check-all` separately. Host sampled
before each cell (`LoadPercentage` 4/3/3, 2/7/1, 1/3/3, 4/4/3 -- quiet
throughout).

**Achieved event counts, not targets** -- and they landed on the targets:

| cell | patients | **events** | events/patient | self-check |
| --- | --- | --- | --- | --- |
| A | 75 | **1,001** | 13.3 | **clean** |
| B | 750 | **10,232** | 13.6 | **clean** |
| C | 7,500 | **104,851** | 14.0 | **clean** |

Every measured run was self-check clean, so no cell measured an error
path. Cell C's event mix, for the record -- 21 distinct types, the churn
family live (`:merge` 873, `:bed-swap` 2,595, `:cancel-transfer` 2,349,
`:cancel-admit` 68, `:cancel-discharge` 36, `:step-rejected` 435) and both
ADR-0164 scan-triggering types dense (`:medication-end` 8,008,
`:care-plan-end` 4,108).

### Walls

| cell | generate (ms) | check (ms) |
| --- | --- | --- |
| A warm-up | 504.3 | 322.3 |
| A timed 1 / 2 / **3** | 427.5 / 509.1 / 528.7 | 350.1 / 342.2 / 307.9 |
| B warm-up | 5,059.4 | 10,374.1 |
| B timed 1 / 2 | 5,133.3 / 5,011.4 | 10,630.3 / 10,270.3 |
| C warm-up | 330,141.2 | 693,495.9 |
| C timed 1 / 2 | 331,400.3 / 316,772.2 | 740,841.2 / 681,341.6 |

**Cell A needed a third run** (the prompt's own >10% rule): generate's
first two timed runs were 427.5 and 509.1, a 19.1% spread. The third,
528.7, sides with the second, so 427.5 is the outlier and the **median,
509.1 ms, is the figure of record**. Not averaged over silently. Cell A
sits at half a second per phase, where JIT warm-up and GC scheduling are
comparable to the work itself; this is a known property of the scale
point, not a machine problem -- cells B and C agree to 2.4% and 4.6%
(generate) and 3.5% and 8.7% (check).

**Figures of record** -- cell A median of three, cells B and C mean of two:

| cell | events | generate | check | generate ev/s | check ev/s |
| --- | --- | --- | --- | --- | --- |
| A | 1,001 | **0.509 s** | **0.342 s** | 1,966 | 2,925 |
| B | 10,232 | **5.072 s** | **10.450 s** | 2,017 | 979 |
| C | 104,851 | **324.09 s** | **711.09 s** | **324** | **147** |

Throughput does not merely fail to improve with scale -- it **collapses**:
generate falls from ~2,000 events/sec to 324, check from ~2,900 to 147.

### The startup term the clinic-decade figure was made of

Cell A's whole `/usr/bin/time` wall is 6.8 s, of which the two measured
phases are 0.85 s. **~6 s of every `ehrt sim run` is JVM start, classpath
resolution and namespace load**, independent of scale. That is what the
13.89 s / 343-event clinic-decade figure was mostly measuring, and why the
prompt is right that it extrapolates to nothing. At cell C the same ~6 s
is 0.6% of a 17-minute run.

## The scaling exponents -- the spike's own answer

Log-log slope between consecutive scale points, on the figures of record:

| step | events | **generate** | **check** |
| --- | --- | --- | --- |
| A -> B | x10.22 | x9.96 time, **slope 0.989** | x30.54 time, **slope 1.471** |
| B -> C | x10.25 | x63.89 time, **slope 1.786** | x68.05 time, **slope 1.814** |
| A -> C overall | x104.7 | x636.6 time, slope 1.388 | x2078 time, slope 1.642 |

**Both phases are quadratic-shaped, and generate hides it until 10^4.**
Generate's A->B slope is 0.989 -- indistinguishable from linear, and
exactly the reassuring number a spike that stopped at 10^4 would have
reported. One decade further it is 1.786. The single-slope reading is a
trap here; the two-term model is the honest one.

### The two-term fit, and what it predicts

A single exponent is the wrong model for `T = aN + bN^2`: the apparent
slope drifts from 1 to 2 as the quadratic overtakes the linear, which is
precisely the pattern above. Solving both coefficients from the two dense
points (B and C):

| phase | fit (seconds, N = events) |
| --- | --- |
| generate | `T = 2.151e-4 * N + 2.743e-8 * N^2` |
| check | `T = 3.984e-4 * N + 6.088e-8 * N^2` |
| both | `T = 6.135e-4 * N + 8.831e-8 * N^2` |

The fit back-predicts cell A -- the point it was **not** fitted on -- at
0.7 s against 0.85 s measured, so the two-term shape is not curve-fitting
noise.

| N events | linear term | quadratic term | total | quadratic share |
| --- | --- | --- | --- | --- |
| 10^3 | 0.6 s | 0.1 s | 0.7 s | 13% |
| 10^5 | 61 s | 883 s | **944 s (16 min)** | **94%** |
| 10^6 | 614 s | 88,310 s | **88,924 s (24.7 h)** | **99.3%** |

**The program's 10^6-message day, on today's generator, is a 25-hour
compute.** Not "plausibly pathological" -- measured, at one decade's
extrapolation from a measured point, on a fit validated against a third.

**And the plan's post-arc-3 estimate survives.** Strip the quadratic and
the linear term alone predicts **215 s generate + 398 s check = 10.2
minutes at 10^6** -- inside the appendix's "skeleton gen 1-5 min" for
generate (3.6 min) and its "streaming self-check 1-3 min" is the one
estimate the measurement pushes back on (6.6 min). The estimates were the
right order of magnitude *for the fixed algorithm*; what they underrated
is the cost of NOT fixing it.

### Throughput, stated plainly

| cell | events | generate ev/s | check ev/s |
| --- | --- | --- | --- |
| A | 1,001 | 1,966 | 2,925 |
| B | 10,232 | 2,017 | 979 |
| C | 104,851 | **324** | **147** |

Throughput does not plateau with scale, it **collapses** -- 6x down on
generate and 20x down on check across two decades.

## Read facts: every whole-log / whole-population scan in the measured path

Read off the tree at `ff45ad1` before any run, so the profile confirms or
exonerates named candidates rather than discovering them. N = events,
P = patients.

### Generate side (`components/sim-engine/src/ehrt/sim_engine/engine.clj`)

| site | shape | cost | gated on |
| --- | --- | --- | --- |
| `decide :medication-end` :857 | `keep-indexed` over `(:ground-truth world)` | O(N) per event | a `:medication-end` with a non-nil `:order-citation` |
| `decide :care-plan-end` :897 | the twin | O(N) per event | a `:care-plan-end` with a non-nil `:care-plan-citation` |
| `last-uncancelled-index` :525 | builds a set from the whole log **and** `keep-indexed`s it | 2 x O(N) per call | `:cancel-admit`/`:cancel-transfer`/`:cancel-discharge` (churn) |
| `replay` via `decide :cancel-transfer`/`:cancel-discharge` :1211/:1226 | a full `evolve` fold of the whole log, to read ONE index | O(N) per call, with allocation | the same two churn steps |
| `sim-model/occupancy-board` :502 / :623 / :1200 | folds **every patient ever created** | O(P) per call | `:admission`, `:transfer`, bed-swap, the two reinstating cancels |

The last row matters more than its size suggests: `run`'s `init-world`
seeds `:patients` with **all** patients at t=0 (`(into {} (map-indexed ...
arrivals))`), not on arrival, so `occupancy-board` is O(P_total) from the
first event, never O(P_live).

**ADR-0164 scoped these scans by patient; it did not shorten them.** The
participant predicate is an extra test inside the same full-length
`keep-indexed`, which is exactly what the prompt's context says and what
the profile is asked to confirm.

### Check side (`components/sim-check/src/ehrt/sim_check/check.clj`)

`check-all` runs 29 invariants; **14 of them each call `engine/replay`
independently** -- 14 separate full folds of the log, each materialising a
vector of N maps carrying `:world-before`/`:world-after`.

Unconditionally superlinear, and NOT among the ADR-0164 suspects:

| invariant | shape | cost |
| --- | --- | --- |
| `no-double-occupancy` :147 | `(vals world-after)` + `frequencies` per event | O(N x P) |
| `admitted-occupies-one-slot` :156 | iterates `world-after` per event | O(N x P) |
| `outpatient-patients-occupy-no-bed` :185 | iterates `world-after` per event | O(N x P) |
| `occupancy-within-capacity` :215 | `(vals world-after)` filtered **per ward** per event | O(N x P x W) |

Conditionally quadratic, gated on churn density:

| invariant | shape | cost |
| --- | --- | --- |
| `cancel-references-existing-uncancelled-event` :268 | inner `(some ... (map-indexed vector indexed))` over the whole log, per cancel | O(C x N) |
| `no-events-after-merged-terminal` :325 | inner full-log loop per merge | O(M x N) |

Linear by construction, for contrast: the two ADR-0166/0164 referential
invariants (`medication-end-...` :478, `care-plan-end-...` :536) resolve
through `(get indexed target-idx)`, an O(1) lookup, and are **not** a
quadratic term on the check side even though their decide-time producers
are one on the generate side.

## Step 3 -- locate, don't fix: the dominant sites, named

One profile run at the largest scale (104,851 events). Two instruments,
both built into the driver. async-profiler is **not** installed on penny
(checked: no `libasyncProfiler` anywhere on the filesystem, nothing under
`~/.m2/repository/tools/profiler`), so the prompt's fallback applies. JFR
*is* available in JDK 21 and was deliberately not used: it attributes by
JVM method, and Clojure compiles the predicates inside `keep-indexed` into
anonymous `engine$eval1234$fn__5678` classes, which would have named none
of the sites below. Attributing by **source line** instead is what makes
`engine.clj:857` legible as ADR-0164's own scan.

- **generate** -- a 10 ms stack sampler on the measuring thread,
  attributing by **source file and line** of the deepest `ehrt.*` frame
  (self) and of every `ehrt.*` frame on the stack (inclusive).
  **30,801 samples** over the 320.5 s generate phase.
- **check** -- each of the 29 catalog entries called and timed
  individually. Not sampling: exact walls. They **sum to 703.3 s against
  the 711.1 s figure of record, 1.1% apart**, so the attribution accounts
  for the whole phase with nothing hidden.

Disclosed: this run's host sample was `LoadPercentage` 29/20/14, higher
than the timed cells, because this session's own agent processes were
active. It is an **attribution** run, not a figure of record, and its
generate wall (320.5 s) still lands inside the timed spread
(316.8-331.4 s), which is the evidence the contention did not distort it.

### Generate: the ADR-0164 scans are CONFIRMED -- and are not the worst site

| rank | site | inclusive | what it is |
| --- | --- | --- | --- |
| 1 | **`engine.clj:1142` `replay`**, called from `:1201` / `:1216` | **35.3%** | a full `evolve` fold of the whole log, allocating a vector of N maps, **to read ONE element** |
| 2 | **`engine.clj:857`** | **21.3%** | ADR-0164's `:medication-end` citation scan |
| 3 | **`engine.clj:897`** | **10.9%** | ADR-0164's `:care-plan-end` citation scan |
| 4 | `facility.clj:44` `occupancy-board` | 8.1% | folds every patient ever created |
| 5 | `engine.clj:501-505` | ~7.9% (self) | `decide :discharge`'s waiting-boarder search: `filter` + **`sort-by`** over all patients |
| 6 | `engine.clj:525` `last-uncancelled-index` | 5.9% | a set-build plus a `keep-indexed`, both full-log |

**~89% of the generate phase is whole-log or whole-population work.**

**The ADR-0164 sites: CONFIRMED, specifically.** `engine.clj:857` and
`:897` are the two scans the prompt named as suspects, and they are #2 and
#3 at **32.2% of generate combined**. ADR-0164 added a participant
predicate *inside* the same full-length `keep-indexed`; the profile is the
measurement of what that ADR explicitly did not do.

**The bigger site, which no register row names.** `decide :cancel-transfer`
and `decide :cancel-discharge` each do this:

```clojure
{:keys [home-ward location]} (:before (nth (replay ground-truth) idx))
```

`replay` folds the **entire** log through `evolve` and materialises a
vector of N maps carrying `:world-before`/`:world-after`, and the caller
then takes `nth ... idx` and discards the rest. Once per cancel event.
It is not a scan -- it is a full re-simulation with allocation, and at
35.3% it is the **single largest cost in generation**, larger than both
ADR-0164 scans together. Cheap to remove in principle (the fold only needs
the prefix up to `idx`, and `idx` is already in hand), but it is a
generator change and therefore arc-3's, not this session's (F1).

### Check: a SECOND quadratic, entirely distinct, and worse

| invariant | wall | share |
| --- | --- | --- |
| **`occupancy-within-capacity`** | **386.0 s** | **54.9%** |
| **`no-double-occupancy`** | **104.3 s** | 14.8% |
| **`admitted-occupies-one-slot`** | **82.2 s** | 11.7% |
| **`outpatient-patients-occupy-no-bed`** | **75.0 s** | 10.7% |
| `cancel-references-existing-uncancelled-event` | 34.8 s | 4.9% |
| `no-events-after-merged-terminal` | 16.9 s | 2.4% |
| *the other 23 invariants, combined* | **4.1 s** | **0.6%** |

Six invariants are **99.4%** of the check phase. The top four are the
unconditionally O(N x P) family -- each walks `(vals world-after)`, the
whole patient population, once per event -- and `occupancy-within-capacity`
is worse by its ward loop, O(N x P x W), which is exactly why it alone is
more than half the phase. The last two are O(C x N) and O(M x N), quadratic
only because churn makes cancels and merges scale with the log.

**These are not the ADR-0164 sites and they are not on arc 3's scope list.**
Arc 3 names "the O(n^2) decide-time scan removals (carry order indexes in
fold state, the `:result-available` pattern)" -- a **generate**-side remedy.
It would leave 711 seconds of check untouched, i.e. **69% of the total**
work at 10^5 and the larger of the two quadratic coefficients
(6.09e-8 vs 2.74e-8).

**Exonerated, specifically.** The two referential invariants that ADR-0164
and ADR-0166 landed -- `medication-end-references-existing-order-...` and
`care-plan-end-references-existing-start-...` -- cost **75.1 ms and 57.3
ms**, together 0.019% of the phase. They resolve through `(get indexed
target-idx)`, an O(1) lookup. The citation family is a generate-side
problem only; on the check side it is free.

## Memory: does held-whole survive 10^5? (plan arc-3's streaming premise)

Two instruments, both reported because they answer different questions
(deviation 5): `/usr/bin/time -v` maximum RSS for the whole JVM, and an
in-process 100 ms sampler on `MemoryMXBean` heap-used, reset between the
two phases.

The ceiling is not a driver choice: `bin/ehrt` sets no JVM options and the
Makefile names none, so every run here ran under the ergonomic default,
**MaxHeapSize = 4,162,846,720 bytes = 3.88 GB**.

### Measured

| quantity | at 104,851 events | per event |
| --- | --- | --- |
| retained engine result (ground-truth + state-history + facility + providers), after two settling GCs | **109.0 MB** | 1.065 KB |
| one materialised `replay` vector (N maps carrying `world-before`/`world-after`) | **90.5 MB** | 0.883 KB |
| peak heap, generate phase (sampler) | 845-941 MB | |
| peak heap, check phase (sampler) | 679-1,388 MB | |
| **peak process RSS** (`/usr/bin/time -v`) | **1.29 / 1.43 / 2.10 / 2.18 GB** across four runs | |

**Structural sharing works, and this is the load-bearing number.** A
`replay` vector carries a full `{patient-id -> state}` map twice per event,
which naively would be O(N x P) *memory* -- 105k events x 7.5k patients. It
measures 0.883 KB/event, i.e. **linear**, because Clojure's persistent maps
share all but O(log P) nodes per update. The check phase's quadratic is
**time only**. That distinction matters for arc 3: the four expensive
invariants can be fixed by carrying an incremental occupancy index without
any change to how the log is held.

Peak RSS varies 1.7x across four identical runs (1.29 -> 2.18 GB) with
identical inputs and identical outputs. That is GC scheduling, not
workload, and it is why RSS is reported as a range and the retained figures
-- taken after two forced settling GCs -- are the ones extrapolated from.

### Does held-whole survive 10^5? **Yes, comfortably. At 10^6, no.**

| N events | retained log | + one replay | live set | vs the 3.88 GB default ceiling |
| --- | --- | --- | --- | --- |
| 10^5 | 104 MB | 86 MB | **190 MB** | **4.8%** -- not close to binding |
| 10^6 | 1,040 MB | 863 MB | **1,902 MB** | **47.9%** |

**Verdict on plan arc-3's streaming premise: CONFIRMED as necessary at
10^6, and NOT necessary at 10^5.** At the program's own 10^5 skeleton
target, memory is nowhere near the constraint -- the live set is under 5%
of the default heap, and peak RSS (2.2 GB worst observed) fits with room.
At 10^6 the live set alone is ~48% of the default ceiling before GC
headroom, in a JVM whose ceiling is ergonomic (1/4 of a 15 GiB machine)
and cannot be raised far; and the check phase then wants **fourteen
sequential replay vectors of 863 MB each**, all allocated and discarded.

So the two constraints do not bind at the same scale, and the honest
statement is the ordering: **at 10^5, time is the whole problem and memory
is not a problem at all** -- 944 s of compute against 190 MB of live data.
Streaming is owed by 10^6, not by the program's stated skeleton target.

## Step 4 -- disposition into the plan

`.agents/plans/2026-08-24-traffic-scale-program.md` edited in three places:

1. **Arc 3's scope list gains three items**, in measured cost order --
   (a) the check-side quadratic family, (b) `replay` inside the two
   reinstating cancel decides, (c) the ADR-0164 scans confirmed and
   quantified, with `occupancy-board` and `decide :discharge`'s
   all-patients `sort-by` riding along. Item (a) is the material change:
   arc 3 previously named a **generate**-side remedy only, which would
   have left the larger quadratic in place.
2. **The "Measurements that gate" row is marked DONE**, naming the second
   quadratic it found.
3. **The appendix is rewritten** from "estimates" to "figures", each
   labeled MEASURED / PROJECTED / ESTIMATE per F3, with the run
   parameters stated once at the top so a reader in a year can re-run it.

**F3 discipline, stated explicitly.** Only the three measured scale points
and what was measured at them carry MEASURED. The 10^6 figures -- both the
24.7 h without the fix and the 10.2 min with it -- are labeled **PROJECTED**
and say so in their own text: they are one decade's extrapolation of a
two-term fit, not measurements. Three estimates are untouched and stay
labeled ESTIMATE, because nothing here measured them: the metro-hospital
day, the emission/render/NIST phases this spike never ran, and the 10^4
reviewability inversion point.

`.agents/plans/roadmap.md`:

- **`roadmap.md#engine-fold-extensions`** (arc 3) rewritten in place,
  still six lines, carrying the amendment.
- **Two new `## Next` rows**, both pathologies found and deliberately not
  patched (the prompt's FINDINGS-over-fixes rule):
  `roadmap.md#vendored-module-emission-floor` (PRIORITY 30) and
  `roadmap.md#no-eligible-provider-throws` (PRIORITY 31).
- **No `## Done` row, and no rotation.** This spike never had a roadmap
  row to close -- it lived in the plan's own "Measurements that gate"
  section, which is where it is marked done. `## Done` therefore stays at
  23 lines against ADR-0161's cap of 30, and rotating early would be its
  own deviation. (The prompt anticipated 24 lines and the residual-probe
  record left it at 30; `ff45ad1` rotated it since. Noted per F5 as a
  premise that had moved, not a problem.)

### F5-3 (finding): a custom facility with no matching provider dies on a bare JDK exception

Found while calibrating, reported because it is a real robustness gap on
the config surface this program's arcs will lean on hard.

A `:facility` naming wards no `:providers` entry is eligible for reaches
`sim-model/choose-attending`, whose `eligible` vector is then empty, and
`choose` calls `.nextInt` with bound 0:

    Execution error (IllegalArgumentException) at java.util.Random/nextInt
    bound must be positive
      ehrt.sim_model.facility/choose        facility.clj:66
      ehrt.sim_model.facility/choose-attending facility.clj:150
      ehrt.sim_engine.engine  (decide :admission)  engine.clj:407

Every other bad-config case on this surface is a structured rejection
surfaced BEFORE `engine/run` -- `:module-not-found`, `:module-load-failed`,
`:incompatible-assignment`, `:invalid-seed` (sim/ADR-0116 made that one a
guard clause for precisely this reason). A ward/provider mismatch is the
same class of author error and gets a raw JDK throw from three frames deep
instead, naming neither the ward nor the facility.

Not fixed here (measurement session, F1). The remedy shape is the one
sim/ADR-0116 already established: a guard at `run` entry returning
`result/error :no-eligible-provider {:ward ...}`.

## Run budget (F4)

F4 allows ~12 generation runs. Spent:

| runs | what |
| --- | --- |
| 1 | driver smoke test, 5 patients |
| 31 | module census -- 31 tiny runs inside ONE JVM, 20 patients each |
| 2 | dense-config calibration at 40 patients (first died on F5-3) |
| 3 | cell A (warm-up + 2 timed) |
| 3 | cell B (warm-up + 2 timed) |
| 2 | **aborted 10^5 runs, disclosed** -- see deviation 2 |
| 3 | cell C (warm-up + 2 timed) |
| 1 | cell A third timed run (the >10% spread rule) |
| 1 | profile run at the largest scale |

The census's 31 runs are counted honestly but are not "generation runs" in
F4's sense: 20 patients apiece, all inside one JVM, ~2 minutes for the lot.
The budget's real unit is the 10^4/10^5 cell, and 9 of those were spent
(6 measured + 2 aborted + 1 profile).

## Deviations

1. **No baseline suite run.** Disclosed per the prompt's step 0: nothing
   lands in `src`, `test`, `schema` or the vendored modules, so `make test`
   would only re-measure `roadmap.md#suite-time-residual`'s own closed
   question at a cost of ~14 minutes. Not run.
2. **Two 10^5 runs were aborted and discarded, and the second was
   contended.** The first attempt backgrounded the run with `nohup ... &`
   inside a tracked shell; the tracked shell exited immediately, the JVM
   was orphaned, and the harness reported success over an empty output
   file. The restart then ran for ~40 seconds ALONGSIDE that orphan --
   two JVMs, one machine -- before `ps` caught it. Both were killed **by
   PID** (`kill -9 20656 20655; kill -9 20829 20828`), never by
   `pkill -f`, whose self-matching cost this repo a 35-minute build once
   already. Neither aborted run contributed a figure; cell C was rerun
   from scratch on a re-verified-quiet machine. Recorded rather than
   quietly re-run, because it is the same class as the Overwatch lesson:
   **the contention was of this session's own making, and only a
   process-level check found it.**
3. **The prompt's step-1 module mix could not be built as specified.**
   See F5-1: the vendored set's best emitter yields 3.4 events/patient and
   19 of 31 yield exactly 1.0. Routed around, disclosed, and the modules
   kept as a 1-in-8 cohort so vendored content is still in every corpus.
4. **The prompt's generate/check phase boundary is not the tree's.** See
   F5-2: `ehrt sim run` already pays both. The driver brackets
   `engine/run` and `check/check-all` separately inside the single verb.
5. **Peak RSS is process-level, not phase-level.** `/usr/bin/time -v`
   reports one maximum for the whole JVM, so the per-phase split comes
   from an in-process 100 ms heap sampler (`MemoryMXBean` heap used) and
   the two instruments are reported side by side rather than merged.
   Method stated because the numbers differ by design: RSS includes JVM
   overhead and unreturned heap, the sampler measures live heap.
6. **This session's own agent processes drew ~0.4 of one core throughout**
   (measured by rate: `claude` 22.3% + 17.7% of a core over a 20 s
   sample). Constant across every cell, ~3% of penny's 12 logical CPUs,
   and disclosed rather than corrected for.

## Appendix: the scratch artifacts, verbatim enough to re-run in a year

Five files, all under a session scratch directory, none committed (F1):

| file | lines | what |
| --- | --- | --- |
| `gen-config.py` | 137 | emits one `--config` EDN per patient count |
| `spike/driver.clj` | 119 | `measure` (the two timed phases) + `census` |
| `spike/profile.clj` | 93 | per-invariant wall + 10 ms stack sampler |
| `cell.sh` | 17 | one cell: warm-up + two timed runs, `/usr/bin/time -v` |
| `run.sh` | 7 | classpath shim (below) |

The classpath shim is the only non-obvious part -- the `:ehrt` alias
carries `:main-opts ["-m" "ehrt.cli.core"]`, so a scratch namespace needs
its own alias whose `:main-opts` wins:

```bash
clojure -Sdeps "{:aliases {:drv {:extra-paths [\"$SP/src\"]
                                 :main-opts [\"-m\" \"spike.driver\" \"$FN\"]}}}" \
        -M:ehrt:drv "$@"
```

`deps.edn` prints `WARNING: Use of :paths external to the project has been
deprecated` on every invocation; harmless, and the reason the driver lives
outside the tree rather than inside it.

### The scenario, in full

Generated identically at every scale point; only the per-ordinal module
cohort grows with `:patients`, so events-per-patient stays ~constant and
the log-log slope measures the algorithm, not the mix.

- `:arrival-gap 2` (minutes, uniform 0..2 -> ~1/min mean), `:churn true`
- `:module-horizon-days 1825`, module cohort every 8th ordinal, cycling
  `sore_throat`, `med_rec`, `injuries`, `bronchitis`,
  `metabolic_syndrome_care`, `veteran_substance_abuse_treatment`,
  `sinusitis` -- the seven top emitters from the census
- seed `20260824` throughout
- facility `:metro-general`: Emergency 180+40 (`:ed`), Medicine A 200+40,
  Medicine B 200+40, Surgery 160+40 (all `:inpatient`) -- sized so the
  steady-state census never reaches the surge rungs, since a
  capacity-exhausted run measures the error path, not generation
- four providers, between them eligible for all four wards (see F5-3)
- three weighted pathways: `dense-inpatient` w45 (21 steps: admission ->
  CBC -> med-order -> transfer -> BMP -> observation -> care-plan-start ->
  procedure -> diagnostic-report -> CBC -> med-order -> observation -> two
  med-ends -> care-plan-end -> discharge), `dense-fast` w35 (8 steps),
  `dense-surgical` w20 (12 steps)

Because arrival rate is fixed and only the patient count grows, the
concurrent census is the same at all three scale points -- so the growth
measured below is the total-log and total-population term, isolated, not a
busier hospital.

## Close

**What this session establishes.**

- Penny's dense-regime figures at HEAD `ff45ad1`, three scale points, every
  run self-check clean: **1,001 / 10,232 / 104,851 events**, generate
  **0.509 / 5.072 / 324.1 s**, check **0.342 / 10.450 / 711.1 s**.
- **The scaling exponents the spike was chartered to produce:** generate
  0.989 -> **1.786**, check 1.471 -> **1.814**, with a two-term fit that
  back-predicts the point it was not fitted on.
- **A second quadratic, found, located and named** -- the check-side
  O(N x P) invariant family, 69% of the work at 10^5, with a coefficient
  2.2x the generate side's, entirely outside arc 3's prior scope.
- **The ADR-0164 sites confirmed specifically** (`engine.clj:857` 21.3%,
  `:897` 10.9%) and **the ADR-0164/0166 check-side invariants exonerated
  specifically** (75.1 ms and 57.3 ms, 0.019% of the phase).
- **The held-whole question answered with numbers:** 1.065 KB/event
  retained, linear, 4.8% of the shipped ceiling at 10^5 -- so arc 3's
  streaming premise is **weakened at the program's own skeleton target and
  confirmed at 10^6**, which is a narrower claim than the plan assumed.

**What it does not establish.** Nothing at 10^6 is measured -- the 24.7 h
and the 10.2 min are both projections, labeled as such in the plan.
Nothing about emission, rendering, fan-out or NIST gating is measured; the
spike ran generate and check only. The exponents are for THIS scenario's
event mix; a mix with a different churn ratio moves the cancel-family terms
(`cancel-references-existing-uncancelled-event` and both cancel decides
scale with cancels, not events), and a mix with fewer patients per event
would shrink every O(N x P) term. The absolute walls are penny's, not a
runner's -- ADR-0167 measured penny at 3.1x a GitHub runner on one
namespace, and nothing here re-measures that gap.

**The finding worth more than the exponents.** Generate's slope over
10^3 -> 10^4 is **0.989**. A spike that had stopped one decade short --
which the existing 343-event clinic-decade figure would have encouraged --
would have reported generation as linear and clean, and arc 3 would have
been scoped against the ADR-0164 scans alone. The quadratic that dominates
at scale is invisible until the scale point that shows it, and the site
that dominates generation (`replay` in the cancel decides, 35.3%) was in
no register row before today.

**Verification.** No full suite was run and none is owed (deviation 1):
this session changed no `src`, no `test`, no schema and no vendored module.
What it did change is documentation that IS gated, so those gates were run
rather than assumed:

    make state-derived  -> regenerated both INDEXes + state-derived.md, exit 0

    Every docs-tooling gate that reads .agents/plans, .agents/prompts or
    .agents/session-records -- found by grep, not by memory:

      roadmap-lint-test            index-completeness-test   stale-path-test
      state-derived-test           attic-rotation-test       done-pointer-adr-test
      prompt-record-pairing-test   state-residue-test        readme-presence-test
      state-staleness-tripwire-test  reading-set-budget-test invocation-lint-test
      notes-prompts-frozen-test

      -> Ran 102 tests containing 701 assertions. 0 failures, 0 errors.

**What was NOT run, disclosed.** `poly test :project:docs-tooling` was
started and hit a 25-minute wall-clock limit (SIGTERM, exit 143) without
completing -- that project re-runs the ADR-0149 trace regeneration, which
is a slow docsgen leaf and is not reached by any edit here. It was not
retried. The thirteen namespaces above are the ones whose scan roots
actually include the files this session touched, and they are green; the
tree also stayed clean of trace residue throughout that partial run, which
is its own evidence the traces were unaffected.

**The roadmap gate caught a real error, on the first run.** The two new
`## Next` rows were first inserted above `roadmap.md#emission-add-ons`,
giving PRIORITY order `... 28 30 31 29`, and
`next-rows-carry-unique-ascending-priorities-test` went red with the
sequence printed:

    ## Next PRIORITY values are not ascending in file order:
    [3 4 ... 27 28 30 31 29]

The rows were moved below PRIORITY 29 and it went green. Recorded because
it is the ADR-0144 row contract doing exactly the job it was built for --
and because a session that reported "roadmap updated" without running its
gate would have shipped that.
