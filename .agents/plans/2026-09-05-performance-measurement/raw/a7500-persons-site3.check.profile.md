### `a7500-persons-site3` — check

`a7500-persons-site3.check.jfr` (8.5M), **3036** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 300 | 9.88% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 163 | 5.37% | `java.io.LineNumberReader.read() line: 141` |
| 145 | 4.78% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 116 | 3.82% | `java.io.FilterReader.read() line: 65` |
| 98 | 3.23% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 86 | 2.83% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 83 | 2.73% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 72 | 2.37% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 66 | 2.17% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 65 | 2.14% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 62 | 2.04% | `clojure.core.protocols$fn__8207$G__8202__8220.invoke(Object, Object, Object) line: 13` |
| 55 | 1.81% | `clojure.lang.RT.first(Object) line: 712` |
| 51 | 1.68% | `clojure.lang.LazySeq.force() line: 50` |
| 50 | 1.65% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 46 | 1.52% | `java.io.PushbackReader.read() line: 86` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1447 | 47.66% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1447 | 47.66% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 106 | 3.49% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 259 | 8.53% |
| decide (the generator's dispatch) | 6 | 0.20% |
| sim-check, whole namespace | 2196 | 72.33% |
| person-simulator (what :persons costs) | 4 | 0.13% |
| patient-simulator (the module cohort's walk) | 11 | 0.36% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log (reader, not invariants) | 19 | 0.63% |
| malli (residue after the 642d70a validator hoist) | 47 | 1.55% |

#### Top-20 by INNERMOST PROJECT FRAME

Each sample attributed to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. This is the table
that names a fix site: the self-time table above is all
`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,
and the named-site table only sees sites someone thought to name.

| samples | share | innermost `ehrt.` frame |
|---|---|---|
| 571 | 18.81% | `ehrt.cli.core$read_ground_truth_stdin$fn__14529.invoke` |
| 448 | 14.76% | `ehrt.sim_engine.fold$apply_events$fn__9734.invoke` |
| 201 | 6.62% | `ehrt.sim_engine.fold$apply_events$fn__9761$fn__9763.invoke` |
| 150 | 4.94% | `ehrt.sim_engine.fold$apply_events$fn__9761.invoke` |
| 140 | 4.61% | `ehrt.sim_engine.fold$apply_events$fn__9734$fn__9750.invoke` |
| 99 | 3.26% | `ehrt.sim_engine.fold$apply_events$fn__9734$fn__9742.invoke` |
| 98 | 3.23% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 88 | 2.90% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 49 | 1.61% | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 39 | 1.28% | `ehrt.sim_check.check$no_double_occupancy$fn__11416$fn__11423.invoke` |
| 36 | 1.19% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 35 | 1.15% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 33 | 1.09% | `ehrt.sim_check.check$bed_fold$fn__11584.invoke` |
| 31 | 1.02% | `ehrt.sim_check.check$no_double_occupancy$fn__11416.invoke` |
| 28 | 0.92% | `ehrt.sim_engine.evolve$eval9506$fn__9508.invoke` |
| 27 | 0.89% | `ehrt.sim_check.check$participants_of.invokeStatic` |
| 24 | 0.79% | `ehrt.sim_check.check$fold_records.invokeStatic` |
| 24 | 0.79% | `ehrt.sim_check.check$events_by_patient$fn__11062.invoke` |
| 24 | 0.79% | `ehrt.sim_check.check$events_by_patient$fn__11062$fn__11064.invoke` |
| 23 | 0.76% | `ehrt.sim_engine.evolve$eval9569$fn__9571$fn__9574.invoke` |

