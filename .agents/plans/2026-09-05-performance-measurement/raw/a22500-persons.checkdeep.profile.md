### `a22500-persons` — checkdeep

`a22500-persons.checkdeep.jfr` (25M), **8800** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 1016 | 11.55% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 649 | 7.38% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 414 | 4.70% | `java.io.LineNumberReader.read() line: 141` |
| 357 | 4.06% | `java.io.FilterReader.read() line: 65` |
| 304 | 3.45% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 284 | 3.23% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 272 | 3.09% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 249 | 2.83% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 176 | 2.00% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 169 | 1.92% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 163 | 1.85% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 163 | 1.85% | `clojure.lang.LazySeq.force() line: 50` |
| 140 | 1.59% | `java.lang.AbstractStringBuilder.ensureCapacityInternal(int) line: 243` |
| 138 | 1.57% | `java.io.PushbackReader.read() line: 86` |
| 134 | 1.52% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 4486 | 50.98% |
| replay -- ADR-0169's 14-calls-per-check-all site | 4485 | 50.97% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 359 | 4.08% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 69 | 0.78% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 753 | 8.56% |
| decide (the generator's dispatch) | 22 | 0.25% |
| sim-check, whole namespace | 6884 | 78.23% |
| person-simulator (what :persons costs) | 7 | 0.08% |
| patient-simulator (the module cohort's walk) | 14 | 0.16% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log (reader, not invariants) | 23 | 0.26% |
| malli (residue after the 642d70a validator hoist) | 121 | 1.38% |

#### Top-20 by INNERMOST PROJECT FRAME

Each sample attributed to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. This is the table
that names a fix site: the self-time table above is all
`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,
and the named-site table only sees sites someone thought to name.

| samples | share | innermost `ehrt.` frame |
|---|---|---|
| 1708 | 19.41% | `ehrt.cli.core$read_ground_truth_stdin$fn__14516.invoke` |
| 1419 | 16.12% | `ehrt.sim_engine.fold$apply_events$fn__9714.invoke` |
| 702 | 7.98% | `ehrt.sim_engine.fold$apply_events$fn__9741$fn__9743.invoke` |
| 428 | 4.86% | `ehrt.sim_engine.fold$apply_events$fn__9741.invoke` |
| 394 | 4.48% | `ehrt.sim_engine.fold$apply_events$fn__9714$fn__9730.invoke` |
| 334 | 3.80% | `ehrt.sim_engine.fold$apply_events$fn__9714$fn__9722.invoke` |
| 287 | 3.26% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 285 | 3.24% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 144 | 1.64% | `ehrt.sim_check.check$bed_fold$fn__11571.invoke` |
| 127 | 1.44% | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 107 | 1.22% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 101 | 1.15% | `ehrt.sim_check.check$events_by_patient$fn__11049$fn__11051.invoke` |
| 93 | 1.06% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 85 | 0.97% | `ehrt.sim_check.check$events_by_patient$fn__11049.invoke` |
| 79 | 0.90% | `ehrt.sim_check.check$outpatient_patients_occupy_no_bed$fn__11474.invoke` |
| 75 | 0.85% | `ehrt.sim_check.check$participants_of.invokeStatic` |
| 72 | 0.82% | `ehrt.sim_check.check$no_double_occupancy$fn__11403.invoke` |
| 66 | 0.75% | `ehrt.sim_check.check$admitted_occupies_one_slot$fn__11442.invoke` |
| 64 | 0.73% | `ehrt.sim_engine.evolve$eval9577$fn__9579$fn__9582.invoke` |
| 62 | 0.70% | `ehrt.sim_engine.evolve$eval9490$fn__9492.invoke` |

