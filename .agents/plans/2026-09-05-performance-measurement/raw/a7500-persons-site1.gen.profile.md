### `a7500-persons-site1` — gen

`a7500-persons-site1.gen.jfr` (23M), **12013** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 1549 | 12.89% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 941 | 7.83% | `clojure.lang.PersistentHashMap.valAt(Object) line: 158` |
| 743 | 6.18% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 597 | 4.97% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 558 | 4.64% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 466 | 3.88% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 748` |
| 452 | 3.76% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 345 | 2.87% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 258 | 2.15% | `clojure.lang.RT.get(Object, Object) line: 781` |
| 252 | 2.10% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 227 | 1.89% | `clojure.lang.RT.first(Object) line: 712` |
| 195 | 1.62% | `clojure.lang.PersistentVector.arrayFor(int) line: 158` |
| 167 | 1.39% | `clojure.lang.LazySeq.force() line: 50` |
| 127 | 1.06% | `clojure.lang.PersistentHashMap$NodeIter.advance() line: 1252` |
| 126 | 1.05% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1173 | 9.76% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1035 | 8.62% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 81 | 0.67% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 1503 | 12.51% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 1763 | 14.68% |
| evolve (the per-event patient fold) | 217 | 1.81% |
| decide (the generator's dispatch) | 5672 | 47.22% |
| sim-check, whole namespace | 1638 | 13.64% |
| person-simulator (what :persons costs) | 1331 | 11.08% |
| patient-simulator (the module cohort's walk) | 426 | 3.55% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 2303 | 19.17% |
| EDN parsing of the input log (reader, not invariants) | 17 | 0.14% |
| malli (residue after the 642d70a validator hoist) | 327 | 2.72% |

#### Top-20 by INNERMOST PROJECT FRAME

Each sample attributed to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. This is the table
that names a fix site: the self-time table above is all
`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,
and the named-site table only sees sites someone thought to name.

| samples | share | innermost `ehrt.` frame |
|---|---|---|
| 1939 | 16.14% | `ehrt.sim_engine.run$select_person$fn__10319.invoke` |
| 1020 | 8.49% | `ehrt.sim_model.facility$occupancy_board$fn__6563.invoke` |
| 749 | 6.23% | `ehrt.sim_engine.log_index$last_uncancelled_index$fn__9779.invoke` |
| 742 | 6.18% | `ehrt.sim_engine.log_index$last_uncancelled_index$fn__9781.invoke` |
| 575 | 4.79% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 464 | 3.86% | `ehrt.sim_model.facility$occupancy_board.invokeStatic` |
| 463 | 3.85% | `ehrt.sim_engine.decide$eval10057$fn__10060$fn__10078.invoke` |
| 364 | 3.03% | `ehrt.sim_engine.run$select_person.invokeStatic` |
| 357 | 2.97% | `ehrt.sim_engine.decide$eval10029$fn__10032.invoke` |
| 346 | 2.88% | `ehrt.sim_engine.fold$apply_events$fn__9723.invoke` |
| 321 | 2.67% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 286 | 2.38% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 235 | 1.96% | `ehrt.sim_engine.decide$eval10057$fn__10060.invoke` |
| 209 | 1.74% | `ehrt.sim_engine.log_index$last_uncancelled_index.invokeStatic` |
| 187 | 1.56% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 152 | 1.27% | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 151 | 1.26% | `ehrt.sim_engine.decide$eval10029$fn__10032$fn__10044.invoke` |
| 142 | 1.18% | `ehrt.sim_engine.fold$apply_events$fn__9750$fn__9752.invoke` |
| 142 | 1.18% | `ehrt.sim_engine.decide$eval10029$fn__10032$fn__10037.invoke` |
| 116 | 0.97% | `ehrt.person_simulator.process$walk_person$fn__12848.invoke` |

