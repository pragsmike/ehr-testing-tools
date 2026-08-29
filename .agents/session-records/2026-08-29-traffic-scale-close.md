# 2026-08-29 -- traffic-scale close: the spike rerun at target scale, and the defects volume found

Arc 4 sweep 6 of 6, and ADR-0168's last obligation. A MEASUREMENT
session: no `src/` or `test/` file was touched, no behaviour changed,
and every defect below is rowed rather than fixed. Base `6eb4aa6`.

## 1. Scope

The traffic-scale program plan (`.agents/plans/2026-08-24-traffic-scale-
program.md`) owed two measurements and had two 10^6 appendix entries
PROJECTED from a generator that no longer exists. This session:

- reran the 2026-08-24 throughput spike's own dense scenario at HEAD, as
  the continuity check against arc 0's curve (the **old** series);
- built `dense-<N>-v2.edn` -- the same scenario plus the nine opt-in keys
  the gated corpora now carry -- and measured it as the program's own
  baseline (the **v2** series);
- priced ADR-0175 ruling D1's `gate v2 --sample-add-ons` at scale;
- annotated both 10^6 projections -- NEITHER converted, because the 10^6
  cell was declined on measured memory arithmetic;
- closed `roadmap.md#emission-add-ons` (6 of 6) and ADR-0168's program.

**What it found that it was not looking for: the v2 baseline does not
complete a run at 10^4 or above.** FOUR invariant families go red across
the blocked cells (TS-1 to TS-4 below); two of them are probably one
defect seen from both sides, and one of the four was not characterised,
so "how many defects" is deliberately not asserted. Not one of the four
fires at 10^3, and not one is reachable by any corpus this repository
ships. The cells are reported BLOCKED, per the session's own fences, and
each family is rowed below. Sweep 2's lesson holds: volume finds what
invariants don't.

## 2. Step 0 -- health record, taken before anything timed

    $ date -Is                        -> 2026-08-29T04:19:17-04:00
    $ git log --oneline -1            -> 6eb4aa6 (the prompt's own base)
    $ git status --porcelain          -> empty
    $ git rev-parse --abbrev-ref HEAD -> main

    $ uptime      -> 04:19:17 up 4 days, 13:08, load average 2.12, 2.09, 0.95
    $ uptime -s   -> 2026-08-24 15:10:46   (the SAME boot ADR-0167's
                     post-reboot baseline was taken on -- no reboot since)
    $ free -h     -> 15Gi total, 2.0Gi used, 13Gi avail, 0B swap used
    $ df -hT .    -> /dev/sdd ext4 251G, 27% used   (NOT /mnt/c)
    $ nproc       -> 12

    $ java -version
    openjdk version "21.0.7" 2025-04-15
    OpenJDK Runtime Environment (build 21.0.7+6-Ubuntu-0ubuntu120.04)
    $ readlink -f $(which java) -> /usr/lib/jvm/java-21-openjdk-amd64/bin/java
    $ java -XX:+PrintFlagsFinal -version | grep MaxHeapSize
      MaxHeapSize = 4162846720  ->  3.88 GB  {ergonomic}
    `bin/ehrt` sets no JVM options; 3.88 GB IS the shipped ceiling.

    Windows side, 04:19-04:23 (interop alive):
      LoadPercentage             -> 0, then 2
      Get-Process -Name wslhost  -> 5 procs, cumulative CPU 0.03-0.55 s
                                    each, started 08/24 15:10 and 08/28
                                    16:46 -- no orphan, no dead parent
      powercfg /getactivescheme  -> High performance
      Win32_Battery              -> BatteryStatus 2 (AC)

**The Linux load average of 2.12 is this session's own `find` sweeps,
not contention**, and is why the Windows side is the one quoted:
`LoadPercentage` read 0 at the same instant. Per ADR-0167's amendment
the host was re-sampled AT EVERY CELL BOUNDARY rather than once at
session start; all 17 samples are in appendix B, and the highest reading
taken while any timed cell was in flight was 17.

**Disclosed:** penny has been up 4 days 13 hours -- the same boot as
ADR-0167's own post-reboot baseline, so no fresh reboot separates these
figures from that one, and the orphan check above is what stands in for it.

## 3. The scratch survived, and what "v2" is

The spike's scratch **survived on penny** in full, for the second arc
running (`dense-7500.edn` byte-identical to the 2026-08-24 original by
md5), so the prompt's 45-minute re-authoring budget was not spent. It was
copied into this session's scratch rather than run in place.

`dense-7500.edn` predates every opt-in key this project has shipped since
2026-08-25. `gen-v2.py` therefore emits `dense-<N>-v2.edn` = **the old
file byte for byte, minus only its closing brace, plus the nine keys** --
and that additive claim is checked, not asserted: `sha256(prefix)` of old
and v2 agree at all three scale points (7,664 / 19,371 / 138,693 bytes).

The nine keys and where each value came from:

| key | value | source |
| --- | --- | --- |
| `:persons` | `{:count 2N :years 20}` | `:years` copied from clinic-decade; **`:count` is its RULE, not its value** -- clinic-decade's own block establishes twice-the-arrival-count as a measurement, and its literal 400 against 7,500 arrivals would collide nearly every arrival onto an already-registered person |
| `:encounters` | `true` | clinic-decade |
| `:bed-cycle` | `true` | clinic-decade |
| `:scheduling` | 0.70 scheduled, `[3 21]` lead, 0.15 no-show, 0.10 reschedule, 0.08 cancel, follow-up 0.35 `[30 120]` | clinic-decade, **copied and disclosed as a mismatch**: those are a booked ambulatory decade's numbers and the dense scenario is an acute metro hospital, for which ed-tuesday's 0.15 would be the clinical choice |
| `:chatter` | three rates 1.0, restatement 0.25/patient-day | clinic-decade, **same mismatch**: 0.25 is the ambulatory census rate against ed-tuesday's 0.02, so this biases the v2 series toward MORE chatter |
| `:charges` | the five-code price table | clinic-decade verbatim |
| `:ladders` | `{:rungs [0.5] :order-rungs [0.25]}` | clinic-decade verbatim |
| `:siu` | `{}` | clinic-decade verbatim |
| `:fan-out` | `:adt-feed` + `:bed-feed` | clinic-decade verbatim |

The prompt called this "the six keys"; the list it then gave names nine,
and nine is what was copied.

## 4. Comparability -- which figures may be compared to what

**This is the section to read before quoting any number below.**

- **The `old` series is comparable to 2026-08-24 and 2026-08-25 in
  SHAPE, and NOT in corpus.** Same config, same seed 20260824, same
  driver classpath shim -- but the corpus has MOVED: 7,500 patients gave
  **105,214 events** here against **104,851** at both the spike and arc 0
  (+0.35%), and 750 patients gave **9,956** against **10,232** (-2.7%).
  That is arc 1's stream partition doing exactly what it was declared to
  do. A wall-for-wall comparison against arc 0 is therefore a comparison
  of two nearly-identical populations, not of one population twice, and
  is quoted that way throughout.
- **The `v2` series starts a NEW series.** Nothing before 2026-08-29 is
  comparable to it. Its 10^3 point is the only one that completed.
- **The `nobed` series is an ISOLATION series, not a substitute
  baseline.** It is `v2` minus `:bed-cycle` -- the one key whose turn-on
  blocks the 10^4 cell -- and exists only so that the other eight keys
  could be measured at all once the baseline stopped completing. Its
  mix genuinely differs (no `:bed-status-change`, no ADT^A20: 338 of
  1,488 events at the 10^3 point are bed-status changes in v2 and none
  in nobed), so **it is never quoted as a v2 figure.** This is a
  judgment call, recorded in section 10.
- **A blocked cell's `generate` figure is clean; its `check` figure is
  NOT.** The check phase on a failing run materialises every violation
  -- 762,302 of them at nobed 10^5, 897,597 at v2 10^5 -- so those check
  walls are the wall of a FAILING check and are not comparable to a
  passing one. Said again wherever they appear.
- Every figure is the **mean of two timed runs after one warm-up**, one
  JVM per run, `/usr/bin/time -v` around each.

## 5. What was measured, and by what

`spike/driver2.clj` succeeds `spike/driver.clj` (2026-08-24) and differs
from it in the way that matters: the old driver **transcribed**
`run-command`'s config path into its own `engine-opts-for`, and a
transcription can drift from what it copies. This one **calls
`run-command` itself** and times the functions it calls, by rebinding
those vars around the one real call. Nothing is transcribed.

| phase | the var timed |
| --- | --- |
| modules | `ehrt.sim.run/resolve-modules` |
| persons | `ehrt.sim.run/engine-persons` |
| generate | `ehrt.sim-engine.interface/run` (+ forcing the log, inside the timed region) |
| check | `ehrt.sim-check.interface/check-all` |
| emit | `ehrt.sim-emit-hl7.interface/emit` \| `/emit-wire`, plus `/plan-fan-out` |
| spool | `ehrt.corpus.generators/spool-sim-output!` (the private var) |
| other | by difference, so the six plus `other` sum to the in-process wall by construction |

**`persons` is not residue and giving it its own row is this session's
own correction to the instrument.** The person walk runs OUTSIDE
`engine/run`, in run-command's translation step (two `persons` calls,
ADR-0173 ruling C1's death cycle). A driver that timed only the engine
would have billed the entire demographic layer to `other` -- 38.3 s of a
290 s cell at v2 10^5, or 13%.

## 6. Step 1 -- the cells

Warm-up plus two timed runs each, one JVM per run, seed 20260824
throughout. Every figure is the mean of the two timed runs; per-run
values are in the appendix.

### 6a. The `old` series -- the continuity check against arc 0

| | events | messages | msg/event | modules | generate | check | emit | spool | other | in-process | process wall | peak RSS |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 750 patients | 9,956 | 6,405 | 0.643 | 0.322 s | 3.083 s | 1.241 s | 0.969 s | 0.669 s | 0.763 s | 6.378 s | 13.05 s | 461 MB |
| 7,500 patients | 105,214 | 67,638 | 0.643 | 0.324 s | **99.667 s** | **10.454 s** | **7.382 s** | **5.631 s** | 1.122 s | **118.9 s** | **131.4 s** | 1,553 MB |

Throughput: generate 3,229 -> **1,056 ev/s**; check 8,020 -> **10,065
ev/s**; emit 6,613 -> **9,163 msg/s**; spool 9,575 -> **12,012 msg/s**.

**Against arc 0 (2026-08-25) at the same cell.** Arc 0 measured generate
101.2 s and check 7.26 s over 104,851 events. Here, over 105,214:

| phase | arc 0 (2026-08-25) | this session | delta |
| --- | --- | --- | --- |
| generate | 101.2 s | **99.7 s** | **-1.5%** |
| check | 7.26 s | **10.45 s** | **+44%** |
| generate throughput | 1,036 ev/s | **1,056 ev/s** | +1.9% |

**Arc 0's generate result has held to within 1.5% across five arcs of
payload** -- streams, persons, encounters, bed cycle, scheduling,
chatter, ladders, SIU, fan-out -- on a config that opts into none of
them. The check side is 44% slower, which is the arcs' own new invariant
families being run, not a regression in an old one; check is still 8.8%
of this cell against arc 0's 6.7%.

**Emit and spool are measured here for the first time.** The spike timed
generate and check only, and the plan's `render+fan-out 5-15 min`
ESTIMATE at 10^6 has never had a measurement under it. At 10^5 the two
together are **13.0 s -- 11% of the cell** -- and both are indistinguishable
from linear (exponents 0.861 and 0.904).

### 6b. The `v2` series -- the program's own baseline, BLOCKED above 10^3

| | events | messages | msg/event | persons | generate | check | emit | spool | wall | peak RSS | self-check |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 75 patients | 1,488 | 1,562 | **1.050** | 0.543 s | 0.432 s | 0.324 s | 0.445 s | 0.222 s | 8.86 s | 339 MB | **clean** |
| 750 patients | 16,322 | -- | -- | 3.156 s | 3.439 s | (2.005 s) | -- | -- | 15.41 s | 596 MB | **BLOCKED** |
| 7,500 patients | 171,925 | -- | -- | 38.341 s | **161.524 s** | (88.805 s) | -- | -- | 307.2 s | 1,842 MB | **BLOCKED** |

A blocked cell's `emit` and `spool` never run: `run-command` returns
`:self-check-failed` and discards the payload. Its event count is
recovered by an UNTIMED shape probe that captures the engine result from
inside `engine/run`. **Its `check` figure is parenthesised throughout
because it is the wall of a FAILING check** -- 2 violations at 10^4, but
**897,597** at 10^5, every one of them materialised into a map.

### 6c. The `nobed` isolation series -- `v2` minus `:bed-cycle`

Not a baseline. See section 4.

| | events | messages | msg/event | persons | generate | check | emit | spool | wall | peak RSS | self-check |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 75 patients | 1,148 | 1,212 | 1.056 | 0.491 s | 0.341 s | 0.260 s | 0.347 s | 0.176 s | 8.36 s | 333 MB | **clean** |
| 750 patients | 12,353 | 15,002 | **1.214** | 3.073 s | 2.614 s | 1.623 s | **2.331 s** | **1.166 s** | 18.07 s | 637 MB | **clean** |
| 7,500 patients | 129,419 | -- | -- | 38.522 s | 123.523 s | (69.008 s) | -- | -- | 248.6 s | 1,944 MB | **BLOCKED** |

### 6d. Messages per event -- the headline the program was commissioned for

| corpus | events | messages | msg/event |
| --- | --- | --- | --- |
| old (no add-ons), 10^4 and 10^5 | 9,956 / 105,214 | 6,405 / 67,638 | **0.643** at both |
| v2 (nine keys on), 10^3 | 1,488 | 1,562 | **1.050** |
| nobed (eight keys on), 10^3 | 1,148 | 1,212 | 1.056 |
| nobed (eight keys on), 10^4 | 12,353 | 15,002 | **1.214** |

**The ratio is stable at 0.643 across a decade on the pre-arc-4 skeleton
and lands between 1.05 and 1.21 with the add-ons on** -- so the arc-4
payload is worth **1.63x to 1.89x the message volume per event**, and the
ratio is still climbing at 10^4 rather than settled. The plan's own
ESTIMATE of "delivered messages 5-20x via fan-out" is a claim about
FAN-OUT, and remains unmeasured as such: the two subscribers here
re-deliver 2,906 of the 15,002 base messages (19.4% more files), which is
a property of this subscriber table and not a general multiplier.

**Not measured, and owed by nobody yet:** msg/event at 10^5 on any
add-on-bearing corpus. Both 10^5 add-on cells are blocked, so the 1.214
figure has no second decade under it. Stated rather than extrapolated.

### 6e. The exponents -- MEASURED, with their decade span

Log-log slope over the event counts actually measured. **A first-decade
slope here is startup-contaminated and is printed only for completeness**:
at the 10^3 cell every phase is a few hundred milliseconds inside a JVM
that is still JIT-ing, which drags each slope below 1. The spike's own
record named the same startup term.

| series | phase | 10^3 -> 10^4 | 10^4 -> 10^5 |
| --- | --- | --- | --- |
| old | generate | -- | **1.474** |
| old | check | -- | 0.904 |
| old | emit | -- | 0.861 |
| old | spool | -- | 0.904 |
| v2 | persons | 0.735 | **1.061** |
| v2 | generate | 0.866 | **1.635** |
| v2 | check | 0.761 | (1.610) |
| nobed | persons | 0.772 | **1.076** |
| nobed | generate | 0.857 | **1.641** |
| nobed | check | 0.771 | (1.596) |
| nobed | emit | 0.802 | -- (blocked) |
| nobed | spool | 0.795 | -- (blocked) |

Four things this table says.

1. **Generate is still super-linear after arc 0, and the payload made it
   worse.** 1.474 on the old config; **1.64 on both add-on configs**,
   which agree with each other to three decimal places' worth of
   confidence and so are unlikely to be noise. Arc 0 removed the
   quadratics it named; it did not make generate linear, and
   `roadmap.md#performance-residual-sites` is where the remainder lives.
2. **The person layer is linear** (1.06 / 1.08 in the second decade) and
   costs 13% of the v2 10^5 cell. A demographic timeline over 15,000
   people x 20 years is affordable at this scale.
3. **The two check exponents are parenthesised because they are
   contaminated**, and by a large factor: the 10^5 check walls include
   materialising 0.76M and 0.9M violation records. No claim is made about
   the check phase's true exponent on an add-on corpus; the honest
   statement is that it is unmeasured, and it will stay unmeasured until
   the defects in section 8 are fixed.
4. **Emit and spool are linear** where they could be measured, which is
   the first evidence under the plan's `render+fan-out` estimate.

## 7. Memory, and the 10^6 decision

Measured, at the 10^5 cells:

| | retained after generate | KB/event | peak heap, generate | peak heap, check | peak heap, emit | peak heap, spool | peak process RSS |
| --- | --- | --- | --- | --- | --- | --- | --- |
| old | 118.2 MB | 1.124 | 500 MB | 807 MB | **987 MB** | 577 MB | 1,553 MB |
| v2 (blocked) | 157.3 MB | 0.915 | 783 MB | 1,207 MB | -- | -- | 1,842 MB |
| nobed (blocked) | 144.6 MB | 1.117 | 727 MB | 962 MB | -- | -- | 1,944 MB |

Arc 0 measured 1.065 KB/event retained; **1.124 KB/event here is +5.5%**,
which is the arcs' new event fields, and it is still linear.

**The 10^6 cell is DECLINED, and here is the arithmetic.** Projecting the
old series -- the only one that completes at 10^5 -- one decade on its own
measured exponents:

| | projection at ~1.05M events |
| --- | --- |
| generate | 99.667 s x 10^1.474 = **2,968 s (49.5 min)** |
| check | 10.454 s x 10^0.904 = 83.8 s |
| emit | 7.382 s x 10^0.861 = 53.6 s |
| spool | 5.631 s x 10^0.904 = 45.1 s |
| **one run** | **~3,151 s = 52.5 min** |
| warm-up + two timed | **~2 h 38 min** |
| retained | 118.2 MB x 10 = **1.18 GB** (30% of the 3.88 GB ceiling) |
| peak heap, emit | 987 MB x 10 = **9.87 GB -- 2.5x the shipped 3.88 GB ceiling** |
| peak process RSS | 1,553 MB x 10 = **15.5 GB -- above the machine's 15 GiB** |

**Memory is what declines it, and not the phase the plan expected.** Plan
:239 projected a ~1.9 GB live set at 10^6 and named that as where arc 3's
streaming premise becomes necessary. The measured retained set projects
to **1.18 GB**, comfortably inside the ceiling -- so on the plan's own
chosen quantity, 10^6 would fit. What does not fit is the **emit phase's
peak heap**, which the plan never considered because the spike never
emitted: rendering 676k messages as strings while the log is still held
projects to 9.87 GB against a 3.88 GB ceiling. **The binding constraint
at 10^6 is the message vector, not the event log.** That is a new fact
and it moves where the streaming work has to happen.

The v2 10^6 cell is declined twice over: its series does not complete at
10^4.

**Both PROJECTED 10^6 entries therefore stay PROJECTED** (F3: nothing
extrapolated is promoted), and both gain a dated superseded-basis note --
their basis config is `dense-7500.edn` with no opt-in key, which is now
one of three configs and the only one that still runs to completion.

## 8. Step 2 -- gating at scale, ruling D1's policy priced for real

`gate v2 --sample-add-ons` gates every SKELETON family in full and caps
each ADD-ON family (an MSH-9 outside the emitter's registry: ADT^A08,
ADT^A31, ADT^A28, DFT^P03) at the first `n` by MSH-10.

**The corpus the prompt named does not exist.** The plan asked for the
sampled gate over the v2 10^5 spool; the v2 10^5 cell is blocked and
produces no spool, and so is nobed's. The two corpora that exist are
therefore what was priced, and both are disclosed as substitutes:

| corpus | files gated | of which base / fan-out | add-on families present |
| --- | --- | --- | --- |
| `nobed` 10^4 | 17,908 | 15,002 / 2,906 | all four |
| `old` 10^5 | 67,638 | 67,638 / 0 | **none** |

### 8a. The runs

| run | cap | files gated | verdicts | wall | peak RSS |
| --- | --- | --- | --- | --- | --- |
| `nobed` 10^4, full width | -- | 17,908 | 17,908 pass, 0 rejected | **50.96 s** | 443 MB |
| `nobed` 10^4, sampled (a) | 5 | 12,147 | 12,147 pass, 0 rejected | **37.76 s** | 492 MB |
| `nobed` 10^4, sampled (b) | 5 | 12,147 | 12,147 pass, 0 rejected | **38.49 s** | 504 MB |
| `old` 10^5, sampled | 5 | **67,638 -- every file** | 67,638 pass, 0 rejected | **149.81 s** | 672 MB |

### 8b. Determinism, asserted

The two sampled runs' reports are **byte-identical**:

    sha256(nobed-1e4-s5-a/report.edn)
      = sha256(nobed-1e4-s5-b/report.edn)
      = d93ca42caf768bc1fdd6d397d954f97c3f4a325e805b60d241b97006005c7b47

which is stronger than the same-verdict-set the prompt asked for, and was
checked as that too: 12,147 `[path verdict]` pairs, equal as ORDERED
sequences and not merely as sets. That is
`ehrt.judge.sampling/stratified-selection`'s "the sample is DERIVED, not
drawn" holding -- ordered by MSH-10, no RNG, recomputable by any reader
from the corpus alone.

### 8c. The per-stratum census, printed because the policy promises it

`nobed` 10^4, cap 5. Four add-on strata, fifteen skeleton:

| stratum | n | gated | |
| --- | --- | --- | --- |
| ADT^A08 | 2,261 | **5** | sampled (add-on) |
| ADT^A31 | 1,937 | **5** | sampled (add-on) |
| ADT^A28 | 836 | **5** | sampled (add-on) |
| DFT^P03 | 747 | **5** | sampled (add-on) |
| ORU^R01 | 3,183 | 3,183 | full (skeleton) |
| ADT^A01 | 2,289 | 2,289 | full (skeleton) |
| ORM^O01 | 2,202 | 2,202 | full (skeleton) |
| ADT^A03 | 1,492 | 1,492 | full (skeleton) |
| ADT^A02 | 1,268 | 1,268 | full (skeleton) |
| SIU^S12 | 719 | 719 | full (skeleton) |
| ADT^A17 | 220 | 220 | full (skeleton) |
| ADT^A04 | 205 | 205 | full (skeleton) |
| ADT^A12 | 199 | 199 | full (skeleton) |
| SIU^S26 | 107 | 107 | full (skeleton) |
| ADT^A40 | 100 | 100 | full (skeleton) |
| SIU^S14 | 72 | 72 | full (skeleton) |
| SIU^S15 | 63 | 63 | full (skeleton) |
| ADT^A13 | 5 | 5 | full (skeleton) |
| ADT^A11 | 3 | 3 | full (skeleton) |

`old` 10^5, cap 5: eleven strata, **`:add-on? false` on every one of
them**, `n = :gated` on every one, 67,638 of 67,638 gated.

### 8d. What the policy is worth, in numbers

Two points on the SAME corpus in the SAME JVM shape give a clean fit:

    full     17,908 files -> 50.96 s
    sampled  12,147 files -> 38.13 s   (mean of a and b)
    ------------------------------------
    delta     5,761 files -> 12.83 s

- **marginal cost: 2.23 ms per message**; fixed cost (JVM + HAPI init +
  the header pass over all 17,908 files, which the sampled run still
  pays in full): **11.1 s**.
- **the sampled run saves 25.2% of wall for 32.2% fewer files.** The
  saving is smaller than the file saving because the fixed 11.1 s is
  paid either way.
- **the ceiling is set by the mix, not by the cap.** Add-ons are 5,781 of
  17,908 files (32.3%); the other 67.7% is skeleton and is irreducible by
  construction. Dropping the cap from 5 to 1 would buy 20 more files.
  **On this corpus no cap can save more than a third**, and that is the
  policy's real shape: it is a bound on the ADD-ON tail, not a general
  sampling knob.
- **on a pre-arc-4 corpus the policy is a no-op.** `old` 10^5 has no
  add-on family at all, so `--sample-add-ons 5` gated 100% of it and cost
  what full width costs. Every corpus this project shipped before arc 4
  sweep 2 is in that position.

**Why a full-width 10^5 NIST-tier run is not owed, with the arithmetic.**
Two reasons, and the first is decisive: **there is no 10^5 add-on corpus
to run it on** -- both add-on cells at that scale are blocked (section 9).
Second, even if there were, the fit above prices it at
`11.1 s + 0.00223 x 179,080 = 410 s`, about 6.8 min, against a sampled
counterpart at `11.1 + 0.00223 x 121,470 = 282 s`, about 4.7 min. The
quantity of interest is the RATIO, and the 10^4 pair measures it directly
at a tenth of the cost; a 10^5 pair would re-confirm 25% and learn
nothing new, because both terms of the fit are already measured.

**ADR-0175 section 2(h)'s own figure is corrected by this.** It measured
`gate v2` at ~5.3 ms/message (~189 msg/s) and projected ~88 min for 10^6
messages. Measured here: **2.23 ms/message** on the add-on corpus and
**2.05 ms/message** on the 10^5 skeleton corpus (using the same 11.1 s
fixed term), i.e. 450-490 msg/s. **Section 2(h) is 2.4-2.6x pessimistic**,
and 10^6 messages full-width prices at **~34-37 min**, not ~88. The
policy's justification is unchanged in kind -- half an hour of gating is
still something people skip -- but the number in the ADR should be read
as an upper bound, not as a measurement of today's gate.

## 9. Findings -- the defects, rowed and not fixed

Every one of these is reachable only at volume: none fires at 10^3, and
none is reachable by any corpus this repository ships. Fences held --
nothing in `src/` or `test/` was touched.

### TS-1 (defect, BLOCKS the v2 10^4 cell): a reinstating cancel that lands during `:cleaning` needs a SEVENTH bed transition

`ehrt.sim-check.check/legal-bed-transitions` enumerates six arcs. A run
at 750 patients produces `[:cleaning :occupied]`, which is not among
them, and the run fails its own self-check.

Probed to the event, in the `v2` 750-patient log:

    5563  ...       :transfer         PID-000473 OUT of SURGERY-34
    5570  t=402540  :bed-status-change SURGERY-34  :dirty -> :cleaning
    5571  t=402780  :cancel-transfer   cancels 5563, REINSTATES
                                       PID-000473 into SURGERY-34
    5574  t=402780  :bed-status-change SURGERY-34  :occupied -> :dirty

`update-beds`' second rule reads the participant's location delta and
writes `:occupied`; the bed's prior status was `:cleaning`, so the
transition the fold records is `[:cleaning :occupied]`.

**The engine is behaving correctly and the RELATION is what is
incomplete.** ADR-0174 section 2(c) carves out the reinstatement arc from
`:dirty` only -- `engine.clj`'s own comment says "a `:cancel-discharge`
can reinstate a patient into a bed whose cycle is already in flight (the
dirty->occupied arc ADR-0174's invariant 3 carves out)" -- but the cycle
has TWO in-flight legs, and a cancel can land in the second one just as
easily as the first. `decide :bed-ready`'s guard then does exactly the
right thing (it sees a non-`:cleaning` bed and emits nothing), so the bed
ends up correctly occupied; only the check-side enumeration disagrees.

This is the same class of gap as the SIXTH arc (`[:occupied :ready]`),
which sweep 2 disclosed rather than smuggled. Frequency: **2 of 16,322
events** at 750 patients, **16** at 7,500. Zero in every shipped corpus,
because the window is one draw of `:turnaround-minutes` wide and the
gated corpora are thin on churn
(`roadmap.md#gated-corpus-churn-and-citation-depth`'s own measurement,
recorded before that row was retired: 10 cancels in a whole run).

### TS-2 (defect, BLOCKS both 10^5 add-on cells): a `:transfer` inside an OUTPATIENT encounter allocates a licensed bed

The dominant failure at 10^5, and the reason the isolation series blocks
too. From the `nobed` 7,500-patient log:

    92832  t=2842620  :outpatient-visit      ENC-001490-03, honouring
                                             outpatient appointment
                                             APT-001490-01
    92836  t=2843280  :transfer   :from nil  -> Medicine A, MEDICINE-A-166,
                                             :placement :licensed
    92839  t=2843820  :outpatient-visit-end  ENC-001490-03

`:from nil` is the tell: the patient held no bed, because they were an
outpatient. The transfer places them in a licensed one anyway, and
`outpatient-patients-occupy-no-bed` then fires for that patient at every
subsequent event for the rest of the run.

- `nobed` 10^5: **762,301 violations across 24 patients** of 129,419 events.
- `v2` 10^5: **897,579 violations across 25 patients** of 171,925 events.

The enormous counts are the invariant's own per-event shape, not 762,000
distinct bad things: **24 patients is the defect's real size.** It is
first reachable at the intersection of two arc-3b sweeps -- scheduling
(sweep 3) books outpatient appointments, and the encounter horizon
(sweep 1) lets a second encounter open for an already-registered patient
-- against an authored pathway that contains a `:transfer` step. Nothing
gates the pathway walk on the encounter's class.

### TS-3 (defect, one instance): `:admission-only-when-no-open-encounter` violated by an `:outpatient-visit`

`v2` 10^5 only, one instance (`PID-000640-f57cb996`, t=100609860). Almost
certainly the same root as TS-2 seen from the other side -- an
`:outpatient-visit` opening while an encounter is already open -- but it
is ONE event against TS-2's 24 patients, so it is rowed separately rather
than assumed to be the same bug.

### TS-4 (defect, one instance each, NOT characterised): `:every-placeholder-registration-is-resolved-or-still-open`

Exactly one violation in each of the two 10^5 add-on cells. Not probed --
this session spent its investigation budget on TS-1 and TS-2, and a
single-instance failure it did not look at is reported as such rather
than described from a guess.

### TS-5 (finding, not a defect): `ehrt gate v2` gates the fan-out spools too

Both `hl7-files-in` and `sampled-gate-entries` walk `file-seq`
RECURSIVELY, so gating a spooled corpus with `:fan-out` on gates the
subscriber directories as well as the base: **17,908 files for a 15,002
message corpus, 19.4% more**. Every one of those extra files is a
re-delivery of bytes already in the base spool with at most four MSH
fields rewritten (ADR-0175 design (f)'s own description), so the extra
work is the gate validating the same message twice. Recursion is a ruled
property (2026-07-31 P2-2 parity pass), so this is a consequence rather
than an oversight -- but it is 19.4% of the bill on a two-subscriber
corpus and scales with the subscriber table.

### TS-6 (instrument finding): a failing check's wall is not a check measurement

`check-all` materialises every violation. At 10^5 that is 0.76M-0.9M
maps, which is why section 6's blocked-cell check figures are
parenthesised and why no check exponent is claimed for the add-on
series. Not obviously a defect -- a failing run is meant to be
diagnosable -- but it is a fact any future measurement session needs
before it quotes a check wall.

### TS-7 (finding): ADR-0175 section 2(h)'s gate cost is 2.4-2.6x pessimistic

See section 8d. ~5.3 ms/message in the ADR against 2.05-2.23 ms/message
measured; 10^6 messages is ~34-37 min, not ~88.

## 10. Judgment calls, and their ratification status

None of these is ratified; each is disclosed here rather than folded in.

1. **The `nobed` isolation series exists at all.** The prompt named two
   configs. When the v2 baseline stopped completing at 10^4, a third --
   v2 minus the single key that blocks it -- was built so that the other
   eight keys, and the messages-per-event headline the program was
   commissioned for, could be measured at some scale above 10^3. It is
   labelled an isolation series everywhere and is never quoted as a v2
   figure. **The v2 cells are still reported BLOCKED**, which is the
   fence's own instruction; this is an addition beside them, not a way
   around them. The counter-argument, stated fairly: it is one key of
   config removed to get a number, which is close to the line the fence
   "no config tuning to improve a number" draws. It went ahead because
   the alternative was a session that measured the arc's headline
   nowhere above 1,488 events.
2. **`:persons :count` is `2N`, not clinic-decade's literal 400.** The
   prompt said to copy values. clinic-decade's own block establishes
   twice-the-arrival-count as a MEASUREMENT with the coverage gate that
   forced it; copying 400 against 7,500 arrivals would have collided
   nearly every arrival onto an already-registered person and measured a
   population that is not the one the key describes. The rule was copied
   instead of the value, and the file says so at the point of use.
3. **`:scheduling` and `:chatter` were copied from clinic-decade
   verbatim despite a disclosed clinical mismatch.** Those are a booked
   ambulatory decade's numbers (0.70 scheduled, 0.25 restatements per
   patient-day) and the dense scenario is an acute metro hospital, for
   which ed-tuesday's 0.15 and 0.02 are the clinical choice. The prompt
   said clinic-decade, so clinic-decade it is; the effect is a heavier
   scheduling and chatter stream than an ED would produce, which biases
   the v2 series toward MORE work, never less.
4. **Step 2 ran over corpora the prompt did not name, because the ones it
   named do not exist.** The v2 10^5 spool cannot be produced. The
   `nobed` 10^4 corpus (the largest add-on-bearing one that exists) and
   the `old` 10^5 corpus (the only 10^5 one) were priced instead, and
   both substitutions are stated at the point of use.
5. **The instrument gained two phases the spike did not have.**
   `modules` and `persons` are now timed separately rather than falling
   into `other`. This makes the 2026-08-29 `other` column not comparable
   to the spike's, which is disclosed; it was done because the person
   walk is 13% of a v2 10^5 cell and billing it to residue would have
   hidden the demographic layer's entire cost.
6. **10^6 was declined rather than attempted.** The prompt allowed this
   "with the arithmetic", and section 7 gives it: the wall is affordable
   (~2 h 38 min for a full cell) and the MEMORY is not (projected emit
   peak 9.87 GB against a 3.88 GB ceiling; projected RSS 15.5 GB against
   15 GiB). No 10^6 run was started, so no 10^6 figure of any kind
   appears above.
7. **Blocked cells' check walls are reported, parenthesised, rather than
   suppressed.** They are real walls of a real call and a future session
   may want them; they are not check measurements, and every appearance
   says so.

## 11. What landed -- the paper

No `src/` or `test/` file was touched. Three documents moved.

- **`.agents/plans/2026-08-24-traffic-scale-program.md`.** Both gating
  bullets closed with pointers; a dated completion note for ADR-0168's
  programme, which also states plainly what the programme did NOT achieve
  (its own 10^5-events-to-10^6-messages target is not demonstrated, and
  the measured 1.05–1.21 msg/event is an order of magnitude short of the
  5–20× fan-out multiplier the target assumed). Six new MEASURED
  (2026-08-29) blocks under their own run-parameters preamble, plus one
  NOT MEASURED block for the blocked baseline. The 2026-08-24 preamble
  was re-scoped -- it said "every MEASURED figure below" and there are now
  figures below it that it does not describe. Both PROJECTED 10^6 entries
  keep their label and gain dated superseded-basis notes; the second is
  also CORRECTED on its memory half. The stale ESTIMATE claiming "no
  emission, rendering or gating figure is measured" was corrected --
  two of its three thirds now have measurements under them, and both
  were pessimistic.
- **`.agents/plans/roadmap.md`.** `[emission-add-ons]` CLOSED (6 of 6),
  moved to `## Done` condensed to one line per the row contract.
  `[corpus-player-slices]` re-derived against the live tree and trimmed
  from 33 lines to 26 -- **two items remain** (the board accumulator's
  final state as an output, and `--board`'s event-input blind spot) and
  the row now names what has shipped so the next reader does not
  re-derive it a third time.
- **`.agents/session-records/2026-08-29-traffic-scale-close.md`** (this
  file) and its prompt archive.

Closed the de-scaffold way: no ADR for a measurement session, no tag,
close ceremony by hand, `make state-derived` for the two generated
indexes.

## 12. Appendix A -- per-run figures

Each cell: warm-up discarded, both timed runs shown. Seconds.

| cell | run | modules | persons | generate | check | emit | spool | other | in-process | process wall | peak RSS |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| old 10^4 | 1 | 0.329 | — | 3.017 | 1.321 | 0.978 | 0.704 | 0.773 | 6.418 | 13.25 | 464 MB |
| old 10^4 | 2 | 0.315 | — | 3.150 | 1.162 | 0.959 | 0.634 | 0.752 | 6.337 | 12.85 | 458 MB |
| old 10^5 | 1 | 0.364 | — | 100.108 | 10.614 | 8.050 | 5.975 | 1.135 | 120.271 | 133.17 | 1,764 MB |
| old 10^5 | 2 | 0.285 | — | 99.227 | 10.293 | 6.713 | 5.287 | 1.108 | 117.626 | 129.67 | 1,343 MB |
| v2 10^3 | 1 | 0.357 | 0.531 | 0.480 | 0.337 | 0.491 | 0.232 | 0.763 | 2.959 | 8.87 | 341 MB |
| v2 10^3 | 2 | 0.316 | 0.556 | 0.384 | 0.311 | 0.399 | 0.212 | 0.766 | 2.732 | 8.84 | 337 MB |
| v2 10^4 BLOCKED | 1 | 0.356 | 3.119 | 3.424 | (1.968) | — | — | 0.738 | 9.604 | 15.25 | 585 MB |
| v2 10^4 BLOCKED | 2 | 0.315 | 3.193 | 3.455 | (2.042) | — | — | 0.761 | 9.766 | 15.57 | 608 MB |
| v2 10^5 BLOCKED | 1 | 0.352 | 38.461 | 165.512 | (84.758) | — | — | 1.271 | 290.353 | 308.04 | 1,887 MB |
| v2 10^5 BLOCKED | 2 | 0.404 | 38.220 | 157.536 | (92.853) | — | — | 1.303 | 290.317 | 306.31 | 1,798 MB |
| nobed 10^3 | 1 | 0.344 | 0.481 | 0.324 | 0.269 | 0.341 | 0.160 | 0.774 | 2.533 | 8.18 | 335 MB |
| nobed 10^3 | 2 | 0.338 | 0.501 | 0.359 | 0.251 | 0.352 | 0.193 | 0.741 | 2.542 | 8.53 | 331 MB |
| nobed 10^4 | 1 | 0.281 | 3.038 | 2.581 | 1.636 | 2.140 | 1.182 | 0.896 | 10.572 | 17.95 | 665 MB |
| nobed 10^4 | 2 | 0.256 | 3.109 | 2.648 | 1.610 | 2.522 | 1.150 | 0.898 | 11.041 | 18.19 | 609 MB |
| nobed 10^5 BLOCKED | 1 | 0.295 | 38.396 | 123.785 | (67.691) | — | — | 1.226 | 231.392 | 248.07 | 2,037 MB |
| nobed 10^5 BLOCKED | 2 | 0.285 | 38.648 | 123.261 | (70.326) | — | — | 1.300 | 233.820 | 249.05 | 1,852 MB |

Peak heap by phase, both runs, at the three 10^5 cells:

| cell | run | persons | generate | check | emit | spool | retained after generate |
| --- | --- | --- | --- | --- | --- | --- | --- |
| old 10^5 | 1 | — | 485 MB | 703 MB | 1,059 MB | 496 MB | 118.1 MB |
| old 10^5 | 2 | — | 515 MB | 910 MB | 914 MB | 657 MB | 118.3 MB |
| v2 10^5 | 1 | 428 MB | 776 MB | 1,394 MB | — | — | 157.2 MB |
| v2 10^5 | 2 | 443 MB | 789 MB | 1,020 MB | — | — | 157.3 MB |
| nobed 10^5 | 1 | 418 MB | 737 MB | 1,020 MB | — | — | 144.6 MB |
| nobed 10^5 | 2 | 400 MB | 716 MB | 903 MB | — | — | 144.5 MB |

## 13. Appendix B -- the health record, per cell boundary

Sampled either side of every cell, Windows side quoted (the Linux side
lies about WSL2 contention). Linux 1-min load in brackets for contrast:

| moment | Windows LoadPercentage | linux 1-min |
| --- | --- | --- |
| session start, 04:19 | 0 | 2.12 |
| 04:23, before v2 10^3 | 2 | 0.62 |
| matrix start, 04:31 | 1 | 0.24 |
| 04:32, after v2 10^3 | 3, then 4 | 1.38 |
| 04:33, after old 10^4 | 4, then **17** | 2.58 |
| 04:33, after v2 10^4 | 16, then 7 | 3.34 |
| 04:34, after nobed 10^3 | 3, then 5 | 2.79 |
| 04:35, after nobed 10^4 | 3, then 3 | 2.94 |
| 04:42, after old 10^5 | 3, then 2 | 1.35 |
| 04:57, after v2 10^5 | 13, then 10 | 2.09 |
| 05:09, after nobed 10^5 | 21 | 1.09 |
| 05:26, before gates | 4 | 1.35 |
| 05:27 / 05:28 / 05:29 / 05:31, between gate runs | 2 / 10 / 9 / 3 | — |

The highest reading taken while a timed cell was in flight is **17**
(04:33, between the old 10^4 and v2 10^4 cells). The 21 at 05:09 and the
several low-teens readings are host activity that started as a cell
FINISHED, which biases against this session's figures rather than for
them. The Linux load column is why the Windows column is the one quoted:
they disagree in both directions and only one of them can see the host.

## 14. Appendix C -- the scratch, and how to regenerate it

The spike's own lesson: a figure whose driver died is a figure nobody can
check. The 2026-08-24 scratch survived on penny for the SECOND arc
running and `dense-7500.edn` is md5-identical to the original, so the
2026-08-24 scenario needs no re-authoring. What this session added:

| file | lines | what |
| --- | --- | --- |
| `spike/driver2.clj` | 152 | the six-phase driver -- rebinds the six vars around one real `run-command` call |
| `spike/shape.clj` | 44 | UNTIMED shape probe: captures the engine result a blocked run discards, prints the full violation census |
| `spike/probe_bed.clj`, `probe_bed2.clj` | 33 | the TS-1 diagnosis -- events naming a bed, then a raw window by log index |
| `gen-v2.py` | 84 | `dense-<N>-v2.edn` = the old file minus its closing brace, plus the nine keys |
| `cell2.sh` | 22 | one cell: fresh spool target, warm-up + two timed, `/usr/bin/time -v` |
| `run2.sh` | 7 | classpath shim, unchanged from the spike's `run.sh` but pointing at driver2 |
| `health.sh` | 9 | the two-sided host sample, unpiped |
| `gate.sh` | 14 | one timed `bin/ehrt gate v2` run with its report |
| `collect.py` | 190 | reads `results/*/timed{1,2}.{out,err}` into the tables above and the log-log slopes |

Regeneration, if this scratch does not survive:

1. `dense-<N>.edn` -- `gen-config.py <N>` from the 2026-08-24 scratch,
   whose scenario is documented in full in that session's own record.
2. `dense-<N>-v2.edn` -- append the nine keys of section 3's table to
   `dense-<N>.edn` in place of its closing brace. The `:persons :count`
   is `2N`; every other value is `demos/scenarios/clinic-decade/config.edn`
   verbatim.
3. `dense-<N>-nobed.edn` -- the same file with the `:bed-cycle true` line
   removed.
4. The classpath shim is the only non-obvious mechanic, and is unchanged
   from 2026-08-24: the `:ehrt` alias carries its own `:main-opts`, so a
   scratch namespace needs an alias whose `:main-opts` wins ---

       clojure -Sdeps "{:aliases {:drv {:extra-paths [\"$SP/src\"]
                                        :main-opts [\"-m\" \"spike.driver2\"]}}}" \
               -M:ehrt:drv "$@"

## 15. Close

**What this session establishes.**

- **Arc 0's result has held.** Generate at the 10^5 cell is within 1.5%
  of its 2026-08-25 figure across the four payload arcs that followed it
  (99.7 s against 101.2 s; 1,056 against 1,036 ev/s), on a config that
  opts into none of it. Check is +44%, which is new invariants running rather than old ones
  regressing.
- **Generate is still super-linear, and the payload made it worse:**
  1.474 on the pre-arc-4 config, **1.64 on both add-on configs**, which
  agree independently. `roadmap.md#performance-residual-sites` is where
  the remainder lives.
- **Emit and spool are measured for the first time** and are linear:
  13.0 s together at 10^5, 11% of the cell. The plan's `render+fan-out
  5–15 min` estimate at 10^6 was an order of magnitude pessimistic.
- **Messages per event, the headline the programme was commissioned
  for:** **0.643** before arc 4 (stable across a full decade) and
  **1.05–1.21** after — the add-ons are worth 1.63×–1.89×, and the ratio
  is still climbing at 10^4.
- **Ruling D1's gating policy, priced:** 2.23 ms/message marginal, 11.1 s
  fixed, **25.2% wall saved for 32.2% fewer files**, reports
  BYTE-IDENTICAL across two runs. Its ceiling is the corpus mix, not the
  cap — add-ons are a third of an arc-4 corpus and **none of a pre-arc-4
  one**, where the flag is a no-op. ADR-0175 section 2(h)'s 5.3
  ms/message is 2.4–2.6× pessimistic.
- **10^6 declined on measured memory arithmetic**, and the constraint is
  not where the plan put it: retained projects to 1.18 GB and fits; the
  **emit phase's peak heap projects to 9.87 GB against a 3.88 GB
  ceiling**. Streaming is needed at EMIT, which the spike could not see
  because it never emitted.
- **And the result nobody asked for: the programme's own baseline
  configuration does not complete a run at 10^4 or above.** Three
  distinct defects (TS-1, TS-2, TS-3) plus one uncharacterised (TS-4),
  none reachable at 10^3, none reachable by any corpus this repository
  ships. Diagnosed to the event and rowed; not fixed, per the fences.
  **This is the largest thing the rerun found, and it is negative:** the
  target scale ADR-0168 set is not currently reachable by the
  configuration the programme itself describes.

**`roadmap.md#emission-add-ons` is CLOSED, 6 of 6, and ADR-0168's
five-arc programme is complete.** What it did not deliver is stated in
the plan beside what it did.
