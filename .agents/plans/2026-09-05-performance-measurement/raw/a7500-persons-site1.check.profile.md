### `a7500-persons-site1` — check

`a7500-persons-site1.check.jfr` (8.1M), **2682** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 304 | 11.33% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 145 | 5.41% | `java.io.LineNumberReader.read() line: 141` |
| 113 | 4.21% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 105 | 3.91% | `java.io.FilterReader.read() line: 65` |
| 86 | 3.21% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 86 | 3.21% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 70 | 2.61% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 66 | 2.46% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 58 | 2.16% | `clojure.core.protocols$fn__8207$G__8202__8220.invoke(Object, Object, Object) line: 13` |
| 56 | 2.09% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 51 | 1.90% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 46 | 1.72% | `clojure.lang.LazySeq.force() line: 50` |
| 40 | 1.49% | `java.lang.String.equals(Object) line: 1861` |
| 38 | 1.42% | `clojure.lang.RT.first(Object) line: 712` |
| 38 | 1.42% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1229 | 45.82% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1229 | 45.82% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 98 | 3.65% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 208 | 7.76% |
| decide (the generator's dispatch) | 4 | 0.15% |
| sim-check, whole namespace | 1897 | 70.73% |
| person-simulator (what :persons costs) | 2 | 0.07% |
| patient-simulator (the module cohort's walk) | 10 | 0.37% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log (reader, not invariants) | 20 | 0.75% |
| malli (residue after the 642d70a validator hoist) | 47 | 1.75% |

#### Top-20 by INNERMOST PROJECT FRAME

Each sample attributed to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. This is the table
that names a fix site: the self-time table above is all
`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,
and the named-site table only sees sites someone thought to name.

| samples | share | innermost `ehrt.` frame |
|---|---|---|
| 580 | 21.63% | `ehrt.cli.core$read_ground_truth_stdin$fn__14512.invoke` |
| 411 | 15.32% | `ehrt.sim_engine.fold$apply_events$fn__9723.invoke` |
| 159 | 5.93% | `ehrt.sim_engine.fold$apply_events$fn__9750$fn__9752.invoke` |
| 146 | 5.44% | `ehrt.sim_engine.fold$apply_events$fn__9750.invoke` |
| 95 | 3.54% | `ehrt.sim_engine.fold$apply_events$fn__9723$fn__9739.invoke` |
| 84 | 3.13% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 78 | 2.91% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 72 | 2.68% | `ehrt.sim_engine.fold$apply_events$fn__9723$fn__9731.invoke` |
| 36 | 1.34% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 35 | 1.30% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 33 | 1.23% | `ehrt.sim_check.check$events_by_patient$fn__11045$fn__11047.invoke` |
| 31 | 1.16% | `ehrt.sim_check.check$bed_fold$fn__11567.invoke` |
| 29 | 1.08% | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 29 | 1.08% | `ehrt.sim_check.check$participants_of.invokeStatic` |
| 24 | 0.89% | `ehrt.sim_check.check$admitted_occupies_one_slot$fn__11438.invoke` |
| 22 | 0.82% | `ehrt.sim_check.check$outpatient_patients_occupy_no_bed$fn__11470.invoke` |
| 19 | 0.71% | `ehrt.sim_engine.evolve$eval9482$fn__9484.invoke` |
| 19 | 0.71% | `ehrt.sim_check.check$no_double_occupancy$fn__11399$fn__11406.invoke` |
| 19 | 0.71% | `ehrt.sim_check.check$events_by_patient$fn__11045.invoke` |
| 18 | 0.67% | `ehrt.sim_engine.evolve$eval9506$fn__9508.invoke` |

