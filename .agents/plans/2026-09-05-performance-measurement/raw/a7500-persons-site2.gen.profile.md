### `a7500-persons-site2` — gen

`a7500-persons-site2.gen.jfr` (24M), **10914** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 1690 | 15.48% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 651 | 5.96% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 462 | 4.23% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 438 | 4.01% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 411 | 3.77% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 400 | 3.67% | `clojure.lang.Util.pcequiv(Object, Object) line: 125` |
| 251 | 2.30% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 754` |
| 237 | 2.17% | `clojure.lang.PersistentVector.arrayFor(int) line: 158` |
| 231 | 2.12% | `clojure.lang.RT.first(Object) line: 712` |
| 207 | 1.90% | `clojure.lang.PersistentHashMap$NodeIter.advance() line: 1252` |
| 196 | 1.80% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 178 | 1.63% | `clojure.lang.LazySeq.force() line: 50` |
| 159 | 1.46% | `clojure.lang.PersistentHashMap$ArrayNode$Iter.hasNext() line: 656` |
| 156 | 1.43% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 143 | 1.31% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1425 | 13.06% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1242 | 11.38% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 95 | 0.87% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 1950 | 17.87% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 1 | 0.01% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 1924 | 17.63% |
| evolve (the per-event patient fold) | 279 | 2.56% |
| decide (the generator's dispatch) | 6487 | 59.44% |
| sim-check, whole namespace | 1879 | 17.22% |
| person-simulator (what :persons costs) | 1311 | 12.01% |
| patient-simulator (the module cohort's walk) | 415 | 3.80% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 1 | 0.01% |
| EDN parsing of the input log (reader, not invariants) | 12 | 0.11% |
| malli (residue after the 642d70a validator hoist) | 350 | 3.21% |

#### Top-20 by INNERMOST PROJECT FRAME

Each sample attributed to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. This is the table
that names a fix site: the self-time table above is all
`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,
and the named-site table only sees sites someone thought to name.

| samples | share | innermost `ehrt.` frame |
|---|---|---|
| 1296 | 11.87% | `ehrt.sim_model.facility$occupancy_board$fn__6563.invoke` |
| 818 | 7.49% | `ehrt.sim_engine.log_index$last_uncancelled_index$fn__9781.invoke` |
| 803 | 7.36% | `ehrt.sim_engine.log_index$last_uncancelled_index$fn__9779.invoke` |
| 636 | 5.83% | `ehrt.sim_model.facility$occupancy_board.invokeStatic` |
| 532 | 4.87% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 472 | 4.32% | `ehrt.sim_engine.decide$eval10057$fn__10060$fn__10078.invoke` |
| 444 | 4.07% | `ehrt.sim_engine.fold$apply_events$fn__9723.invoke` |
| 382 | 3.50% | `ehrt.sim_engine.decide$eval10029$fn__10032.invoke` |
| 335 | 3.07% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 302 | 2.77% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 231 | 2.12% | `ehrt.sim_engine.log_index$last_uncancelled_index.invokeStatic` |
| 226 | 2.07% | `ehrt.sim_engine.decide$eval10057$fn__10060.invoke` |
| 222 | 2.03% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 206 | 1.89% | `ehrt.sim_engine.decide$eval10029$fn__10032$fn__10037.invoke` |
| 187 | 1.71% | `ehrt.sim_engine.fold$apply_events$fn__9750$fn__9752.invoke` |
| 162 | 1.48% | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 158 | 1.45% | `ehrt.person_simulator.process$walk_person$fn__12854.invoke` |
| 152 | 1.39% | `ehrt.sim_engine.decide$eval10029$fn__10032$fn__10044.invoke` |
| 117 | 1.07% | `ehrt.sim_engine.fold$apply_events$fn__9750.invoke` |
| 111 | 1.02% | `ehrt.sim_engine.fold$apply_events$fn__9723$fn__9739.invoke` |

