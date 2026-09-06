### `a7500-persons-site4` — check

`a7500-persons-site4.check.jfr` (8.4M), **2909** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 274 | 9.42% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 168 | 5.78% | `java.io.LineNumberReader.read() line: 141` |
| 136 | 4.68% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 114 | 3.92% | `java.io.FilterReader.read() line: 65` |
| 103 | 3.54% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 90 | 3.09% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 79 | 2.72% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 71 | 2.44% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 59 | 2.03% | `clojure.lang.LazySeq.force() line: 50` |
| 58 | 1.99% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 56 | 1.93% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 44 | 1.51% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 43 | 1.48% | `clojure.lang.MethodImplCache.fnFor(Class) line: 67` |
| 41 | 1.41% | `java.lang.AbstractStringBuilder.ensureCapacityInternal(int) line: 243` |
| 39 | 1.34% | `clojure.lang.RT.assoc(Object, Object, Object) line: 847` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1406 | 48.33% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1406 | 48.33% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 98 | 3.37% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 279 | 9.59% |
| decide (the generator's dispatch) | 7 | 0.24% |
| sim-check, whole namespace | 2108 | 72.46% |
| person-simulator (what :persons costs) | 5 | 0.17% |
| patient-simulator (the module cohort's walk) | 12 | 0.41% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log (reader, not invariants) | 17 | 0.58% |
| malli (residue after the 642d70a validator hoist) | 44 | 1.51% |

#### Top-20 by INNERMOST PROJECT FRAME

Each sample attributed to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. This is the table
that names a fix site: the self-time table above is all
`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,
and the named-site table only sees sites someone thought to name.

| samples | share | innermost `ehrt.` frame |
|---|---|---|
| 563 | 19.35% | `ehrt.cli.core$read_ground_truth_stdin$fn__14527.invoke` |
| 435 | 14.95% | `ehrt.sim_engine.fold$apply_events$fn__9742.invoke` |
| 204 | 7.01% | `ehrt.sim_engine.fold$apply_events$fn__9769$fn__9771.invoke` |
| 124 | 4.26% | `ehrt.sim_engine.fold$apply_events$fn__9769.invoke` |
| 123 | 4.23% | `ehrt.sim_engine.fold$apply_events$fn__9742$fn__9758.invoke` |
| 103 | 3.54% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 93 | 3.20% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 83 | 2.85% | `ehrt.sim_engine.fold$apply_events$fn__9742$fn__9750.invoke` |
| 48 | 1.65% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 36 | 1.24% | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 34 | 1.17% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 30 | 1.03% | `ehrt.sim_check.check$bed_fold$fn__11582.invoke` |
| 27 | 0.93% | `ehrt.sim_engine.evolve$eval9506$fn__9508.invoke` |
| 26 | 0.89% | `ehrt.sim_check.check$outpatient_patients_occupy_no_bed$fn__11485.invoke` |
| 26 | 0.89% | `ehrt.sim_check.check$events_by_patient$fn__11060.invoke` |
| 25 | 0.86% | `ehrt.sim_engine.evolve$eval9569$fn__9571.invoke` |
| 21 | 0.72% | `ehrt.sim_check.check$participants_of.invokeStatic` |
| 20 | 0.69% | `ehrt.sim_engine.evolve$eval9482$fn__9484.invoke` |
| 19 | 0.65% | `ehrt.sim_engine.evolve$eval9630$fn__9632.invoke` |
| 19 | 0.65% | `ehrt.sim_engine.evolve$eval9569$fn__9571$fn__9574.invoke` |

