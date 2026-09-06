### `a7500-persons-site2` — check

`a7500-persons-site2.check.jfr` (8.4M), **2944** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 303 | 10.29% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 160 | 5.43% | `java.io.LineNumberReader.read() line: 141` |
| 151 | 5.13% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 114 | 3.87% | `java.io.FilterReader.read() line: 65` |
| 110 | 3.74% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 100 | 3.40% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 77 | 2.62% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 76 | 2.58% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 67 | 2.28% | `clojure.core.protocols$fn__8207$G__8202__8220.invoke(Object, Object, Object) line: 13` |
| 59 | 2.00% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 56 | 1.90% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 54 | 1.83% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 50 | 1.70% | `clojure.lang.LazySeq.force() line: 50` |
| 44 | 1.49% | `clojure.lang.RT.first(Object) line: 712` |
| 43 | 1.46% | `java.io.PushbackReader.read() line: 86` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1378 | 46.81% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1378 | 46.81% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 111 | 3.77% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 240 | 8.15% |
| decide (the generator's dispatch) | 7 | 0.24% |
| sim-check, whole namespace | 2134 | 72.49% |
| person-simulator (what :persons costs) | 5 | 0.17% |
| patient-simulator (the module cohort's walk) | 14 | 0.48% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log (reader, not invariants) | 25 | 0.85% |
| malli (residue after the 642d70a validator hoist) | 53 | 1.80% |

#### Top-20 by INNERMOST PROJECT FRAME

Each sample attributed to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. This is the table
that names a fix site: the self-time table above is all
`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,
and the named-site table only sees sites someone thought to name.

| samples | share | innermost `ehrt.` frame |
|---|---|---|
| 593 | 20.14% | `ehrt.cli.core$read_ground_truth_stdin$fn__14518.invoke` |
| 427 | 14.50% | `ehrt.sim_engine.fold$apply_events$fn__9723.invoke` |
| 198 | 6.73% | `ehrt.sim_engine.fold$apply_events$fn__9750$fn__9752.invoke` |
| 148 | 5.03% | `ehrt.sim_engine.fold$apply_events$fn__9750.invoke` |
| 134 | 4.55% | `ehrt.sim_engine.fold$apply_events$fn__9723$fn__9739.invoke` |
| 98 | 3.33% | `ehrt.sim_engine.fold$apply_events$fn__9723$fn__9731.invoke` |
| 96 | 3.26% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 90 | 3.06% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 44 | 1.49% | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 43 | 1.46% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 41 | 1.39% | `ehrt.sim_check.check$bed_fold$fn__11573.invoke` |
| 36 | 1.22% | `ehrt.sim_check.check$events_by_patient$fn__11051.invoke` |
| 28 | 0.95% | `ehrt.sim_engine.evolve$eval9482$fn__9484.invoke` |
| 28 | 0.95% | `ehrt.sim_check.check$no_double_occupancy$fn__11405.invoke` |
| 26 | 0.88% | `ehrt.sim_check.check$no_double_occupancy$fn__11405$fn__11412.invoke` |
| 26 | 0.88% | `ehrt.sim_check.check$events_by_patient$fn__11051$fn__11053.invoke` |
| 25 | 0.85% | `ehrt.sim_check.check$outpatient_patients_occupy_no_bed$fn__11476.invoke` |
| 25 | 0.85% | `ehrt.sim_check.check$no_events_after_merged_terminal$fn__11826.invoke` |
| 24 | 0.82% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 24 | 0.82% | `ehrt.sim_check.check$participants_of.invokeStatic` |

