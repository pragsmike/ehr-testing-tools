### `a7500-persons-site7-after` — check

`a7500-persons-site7-after.check.jfr` (4.4M), **1629** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 151 | 9.27% | `java.io.LineNumberReader.read() line: 141` |
| 118 | 7.24% | `java.io.FilterReader.read() line: 65` |
| 108 | 6.63% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 68 | 4.17% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 51 | 3.13% | `java.lang.AbstractStringBuilder.ensureCapacityInternal(int) line: 243` |
| 42 | 2.58% | `java.io.PushbackReader.read() line: 86` |
| 41 | 2.52% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 37 | 2.27% | `clojure.core$seq__5488.invokeStatic(Object) line: 139` |
| 36 | 2.21% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 35 | 2.15% | `clojure.lang.RT.nthFrom(Object, int, Object) line: 1002` |
| 33 | 2.03% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 32 | 1.96% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 26 | 1.60% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 24 | 1.47% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 22 | 1.35% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 84 | 5.16% |
| replay -- ADR-0169's 14-calls-per-check-all site | 84 | 5.16% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 48 | 2.95% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 23 | 1.41% |
| decide (the generator's dispatch) | 8 | 0.49% |
| sim-check, whole namespace | 859 | 52.73% |
| person-simulator (what :persons costs) | 0 | 0.00% |
| patient-simulator (the module cohort's walk) | 8 | 0.49% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 558 | 34.25% |
| malli (residue after the 642d70a validator hoist) | 58 | 3.56% |

#### Top-15 PROJECT frames, self AND inclusive

SELF attributes each sample to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. INCLUSIVE counts
each sample once for every DISTINCT `ehrt.` frame anywhere in it,
so a caller carries everything under it. Ranked by SELF; inclusive
shares do not sum to 100%. This is the table that names a fix site:
the self-time table above is all `KeywordLookupSite`/`Util.equiv`
and belongs to no site at all, and the named-site table only sees
sites someone thought to name.

| self | self % | incl % | `ehrt.` frame |
|---|---|---|---|
| 558 | 34.25% | 34.25% | `ehrt.cli.core$read_ground_truth_stdin$fn__14512.invoke` |
| 88 | 5.40% | 10.13% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 59 | 3.62% | 4.05% | `ehrt.sim_check.check$bed_fold$fn__11566.invoke` |
| 51 | 3.13% | 3.13% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 35 | 2.15% | 3.87% | `ehrt.sim_check.check$events_by_patient$fn__11044.invoke` |
| 28 | 1.72% | 1.72% | `ehrt.sim_check.check$events_by_patient$fn__11044$fn__11046.invoke` |
| 24 | 1.47% | 1.60% | `ehrt.sim_check.check$outpatient_patients_occupy_no_bed$fn__11469.invoke` |
| 23 | 1.41% | 4.24% | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 23 | 1.41% | 1.90% | `ehrt.sim_check.check$no_double_occupancy$fn__11398$fn__11405.invoke` |
| 23 | 1.41% | 1.41% | `ehrt.sim_check.check$participants_of.invokeStatic` |
| 22 | 1.35% | 3.44% | `ehrt.sim_check.check$no_double_occupancy$fn__11398.invoke` |
| 20 | 1.23% | 1.72% | `ehrt.sim_check.check$non_admitted_patients_hold_no_bed$fn__11526.invoke` |
| 20 | 1.23% | 1.35% | `ehrt.sim_check.check$no_events_after_merged_terminal$fn__11818.invoke` |
| 20 | 1.23% | 1.23% | `ehrt.sim_check.check$timestamps_monotone$iter__11055__11061$fn__11062$iter__11057__11066$fn__11067.invoke` |
| 16 | 0.98% | 2.33% | `ehrt.sim_check.check$occupancy_within_capacity$fn__11652.invoke` |

#### `apply-events` concern breakdown

Of the **84** samples anywhere beneath `fold/apply-events`
(5.16% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 23 | 27.38% | 1.41% |
| :board -- `update-board` (ADR-0180 site 3) | 12 | 14.29% | 0.74% |
| :encounter-stamp -- `encounters/stamp-encounter` | 3 | 3.57% | 0.18% |
| :eligible-index -- `update-eligible` (ADR-0180 site 5) | 0 | 0.00% | 0.00% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 0 | 0.00% | 0.00% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 0 | 0.00% | 0.00% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 0 | 0.00% | 0.00% |
| **residual** -- no named concern frame | 46 | 54.76% | 2.82% |

