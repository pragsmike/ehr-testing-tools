### `a7500-persons` — check

`a7500-persons.check.jfr` (8.4M), **2904** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 311 | 10.71% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 154 | 5.30% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 141 | 4.86% | `java.io.LineNumberReader.read() line: 141` |
| 124 | 4.27% | `java.io.FilterReader.read() line: 65` |
| 103 | 3.55% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 86 | 2.96% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 80 | 2.75% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 70 | 2.41% | `clojure.core.protocols$fn__8207$G__8202__8220.invoke(Object, Object, Object) line: 13` |
| 66 | 2.27% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 59 | 2.03% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 59 | 2.03% | `clojure.lang.LazySeq.force() line: 50` |
| 49 | 1.69% | `java.io.PushbackReader.read() line: 86` |
| 48 | 1.65% | `clojure.core$chunk_first.invokeStatic(Object) line: 704` |
| 47 | 1.62% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 40 | 1.38% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1399 | 48.17% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1399 | 48.17% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 107 | 3.68% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 263 | 9.06% |
| decide (the generator's dispatch) | 4 | 0.14% |
| sim-check, whole namespace | 2146 | 73.90% |
| person-simulator (what :persons costs) | 1 | 0.03% |
| patient-simulator (the module cohort's walk) | 10 | 0.34% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log (reader, not invariants) | 17 | 0.59% |
| malli (residue after the 642d70a validator hoist) | 48 | 1.65% |

#### Top-20 by INNERMOST PROJECT FRAME

Each sample attributed to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. This is the table
that names a fix site: the self-time table above is all
`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,
and the named-site table only sees sites someone thought to name.

| samples | share | innermost `ehrt.` frame |
|---|---|---|
| 553 | 19.04% | `ehrt.cli.core$read_ground_truth_stdin$fn__14516.invoke` |
| 437 | 15.05% | `ehrt.sim_engine.fold$apply_events$fn__9714.invoke` |
| 181 | 6.23% | `ehrt.sim_engine.fold$apply_events$fn__9741$fn__9743.invoke` |
| 147 | 5.06% | `ehrt.sim_engine.fold$apply_events$fn__9741.invoke` |
| 126 | 4.34% | `ehrt.sim_engine.fold$apply_events$fn__9714$fn__9730.invoke` |
| 103 | 3.55% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 99 | 3.41% | `ehrt.sim_engine.fold$apply_events$fn__9714$fn__9722.invoke` |
| 90 | 3.10% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 43 | 1.48% | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 39 | 1.34% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 39 | 1.34% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 32 | 1.10% | `ehrt.sim_check.check$bed_fold$fn__11571.invoke` |
| 28 | 0.96% | `ehrt.sim_check.check$events_by_patient$fn__11049.invoke` |
| 25 | 0.86% | `ehrt.sim_check.check$events_by_patient$fn__11049$fn__11051.invoke` |
| 22 | 0.76% | `ehrt.sim_engine.evolve$eval9490$fn__9492.invoke` |
| 21 | 0.72% | `ehrt.sim_check.check$fold_records.invokeStatic` |
| 21 | 0.72% | `ehrt.sim_check.check$admitted_occupies_one_slot$fn__11442.invoke` |
| 19 | 0.65% | `ehrt.sim_check.check$occupancy_within_capacity$fn__11657.invoke` |
| 19 | 0.65% | `ehrt.sim_check.check$no_double_occupancy$fn__11403.invoke` |
| 19 | 0.65% | `ehrt.sim_check.check$bed_fold$step__11566.invoke` |

