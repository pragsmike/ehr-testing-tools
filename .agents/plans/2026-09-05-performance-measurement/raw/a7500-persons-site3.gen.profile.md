### `a7500-persons-site3` — gen

`a7500-persons-site3.gen.jfr` (21M), **9238** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 1870 | 20.24% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 766 | 8.29% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 467 | 5.06% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 345 | 3.73% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 261 | 2.83% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 748` |
| 206 | 2.23% | `clojure.lang.RT.first(Object) line: 712` |
| 174 | 1.88% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 146 | 1.58% | `clojure.lang.PersistentVector.arrayFor(int) line: 158` |
| 137 | 1.48% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 131 | 1.42% | `clojure.lang.LazySeq.force() line: 50` |
| 124 | 1.34% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 118 | 1.28% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |
| 112 | 1.21% | `clojure.lang.Numbers.category(Object) line: 1180` |
| 109 | 1.18% | `clojure.lang.PersistentHashMap$NodeSeq.create(Object[], int, ISeq) line: 1325` |
| 105 | 1.14% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1425 | 15.43% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1251 | 13.54% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 103 | 1.11% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 1 | 0.01% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 1998 | 21.63% |
| evolve (the per-event patient fold) | 265 | 2.87% |
| decide (the generator's dispatch) | 4727 | 51.17% |
| sim-check, whole namespace | 1912 | 20.70% |
| person-simulator (what :persons costs) | 1416 | 15.33% |
| patient-simulator (the module cohort's walk) | 414 | 4.48% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 1 | 0.01% |
| EDN parsing of the input log (reader, not invariants) | 22 | 0.24% |
| malli (residue after the 642d70a validator hoist) | 371 | 4.02% |

#### Top-20 by INNERMOST PROJECT FRAME

Each sample attributed to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. This is the table
that names a fix site: the self-time table above is all
`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,
and the named-site table only sees sites someone thought to name.

| samples | share | innermost `ehrt.` frame |
|---|---|---|
| 902 | 9.76% | `ehrt.sim_engine.log_index$last_uncancelled_index$fn__9792.invoke` |
| 820 | 8.88% | `ehrt.sim_engine.log_index$last_uncancelled_index$fn__9790.invoke` |
| 602 | 6.52% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 534 | 5.78% | `ehrt.sim_engine.decide$eval10068$fn__10071$fn__10089.invoke` |
| 440 | 4.76% | `ehrt.sim_engine.fold$apply_events$fn__9734.invoke` |
| 402 | 4.35% | `ehrt.sim_engine.decide$eval10040$fn__10043.invoke` |
| 345 | 3.73% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 305 | 3.30% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 237 | 2.57% | `ehrt.sim_engine.decide$eval10068$fn__10071.invoke` |
| 236 | 2.55% | `ehrt.sim_engine.decide$eval10040$fn__10043$fn__10048.invoke` |
| 207 | 2.24% | `ehrt.sim_engine.log_index$last_uncancelled_index.invokeStatic` |
| 180 | 1.95% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 178 | 1.93% | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 166 | 1.80% | `ehrt.sim_engine.fold$apply_events$fn__9761$fn__9763.invoke` |
| 161 | 1.74% | `ehrt.sim_engine.decide$eval10040$fn__10043$fn__10055.invoke` |
| 154 | 1.67% | `ehrt.person_simulator.process$walk_person$fn__12865.invoke` |
| 138 | 1.49% | `ehrt.sim_engine.fold$apply_events$fn__9734$fn__9750.invoke` |
| 128 | 1.39% | `ehrt.sim_engine.fold$apply_events$fn__9761.invoke` |
| 116 | 1.26% | `ehrt.person_simulator.persona$weighted_pick.invokeStatic` |
| 86 | 0.93% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |

