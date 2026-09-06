### `a7500-persons` — gen

`a7500-persons.gen.jfr` (32M), **19012** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 2485 | 13.07% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 1107 | 5.82% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 862 | 4.53% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 849 | 4.47% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 828 | 4.36% | `clojure.lang.RT.first(Object) line: 712` |
| 663 | 3.49% | `clojure.lang.Numbers.category(Object) line: 1180` |
| 609 | 3.20% | `clojure.lang.PersistentHashMap$NodeSeq.create(Object[], int, ISeq) line: 1325` |
| 583 | 3.07% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 582 | 3.06% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 748` |
| 576 | 3.03% | `ehrt.sim_engine.run$select_person$fn__10323.invoke(Object) line: 167` |
| 510 | 2.68% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 430 | 2.26% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 422 | 2.22% | `clojure.lang.LazySeq.force() line: 50` |
| 418 | 2.20% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |
| 315 | 1.66% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1438 | 7.56% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1240 | 6.52% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 94 | 0.49% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 2215 | 11.65% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 4796 | 25.23% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 1957 | 10.29% |
| evolve (the per-event patient fold) | 287 | 1.51% |
| decide (the generator's dispatch) | 11792 | 62.02% |
| sim-check, whole namespace | 1879 | 9.88% |
| person-simulator (what :persons costs) | 1365 | 7.18% |
| patient-simulator (the module cohort's walk) | 438 | 2.30% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 2694 | 14.17% |
| EDN parsing of the input log (reader, not invariants) | 28 | 0.15% |
| malli (residue after the 642d70a validator hoist) | 372 | 1.96% |

#### Top-20 by INNERMOST PROJECT FRAME

Each sample attributed to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. This is the table
that names a fix site: the self-time table above is all
`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,
and the named-site table only sees sites someone thought to name.

| samples | share | innermost `ehrt.` frame |
|---|---|---|
| 2409 | 12.67% | `ehrt.sim_engine.decide$waiting_boarder.invokeStatic` |
| 2257 | 11.87% | `ehrt.sim_engine.run$select_person$fn__10323.invoke` |
| 1574 | 8.28% | `ehrt.sim_engine.decide$waiting_boarder$fn__9944.invoke` |
| 1523 | 8.01% | `ehrt.sim_model.facility$occupancy_board$fn__6563.invoke` |
| 857 | 4.51% | `ehrt.sim_engine.log_index$last_uncancelled_index$fn__9764.invoke` |
| 830 | 4.37% | `ehrt.sim_engine.log_index$last_uncancelled_index$fn__9762.invoke` |
| 813 | 4.28% | `ehrt.sim_engine.decide$waiting_boarder$fn__9953.invoke` |
| 692 | 3.64% | `ehrt.sim_model.facility$occupancy_board.invokeStatic` |
| 579 | 3.05% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 507 | 2.67% | `ehrt.sim_engine.decide$eval10061$fn__10064$fn__10082.invoke` |
| 437 | 2.30% | `ehrt.sim_engine.run$select_person.invokeStatic` |
| 413 | 2.17% | `ehrt.sim_engine.fold$apply_events$fn__9714.invoke` |
| 389 | 2.05% | `ehrt.sim_engine.decide$eval10033$fn__10036.invoke` |
| 363 | 1.91% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 350 | 1.84% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 236 | 1.24% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 230 | 1.21% | `ehrt.sim_engine.decide$eval10033$fn__10036$fn__10041.invoke` |
| 220 | 1.16% | `ehrt.sim_engine.decide$eval10061$fn__10064.invoke` |
| 217 | 1.14% | `ehrt.sim_engine.log_index$last_uncancelled_index.invokeStatic` |
| 198 | 1.04% | `ehrt.sim_engine.fold$apply_events$fn__9741$fn__9743.invoke` |

