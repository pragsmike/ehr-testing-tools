### `a22500-persons` — gen

`a22500-persons.gen.jfr` (197M), **187327** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 23562 | 12.58% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 18026 | 9.62% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 12145 | 6.48% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 9097 | 4.86% | `clojure.lang.PersistentHashMap$NodeSeq.create(Object[]) line: 1302` |
| 8116 | 4.33% | `clojure.lang.PersistentHashMap$ArrayNode$Iter.hasNext() line: 656` |
| 7573 | 4.04% | `clojure.lang.RT.first(Object) line: 712` |
| 6743 | 3.60% | `clojure.lang.Util.pcequiv(Object, Object) line: 125` |
| 5806 | 3.10% | `clojure.lang.RT.count(Object) line: 663` |
| 5244 | 2.80% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 5189 | 2.77% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 4879 | 2.60% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 4703 | 2.51% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 748` |
| 4305 | 2.30% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 4091 | 2.18% | `clojure.lang.PersistentHashMap$NodeSeq.create(Object[], int, ISeq) line: 1325` |
| 4007 | 2.14% | `clojure.lang.PersistentHashMap$NodeIter.advance() line: 1258` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 5160 | 2.75% |
| replay -- ADR-0169's 14-calls-per-check-all site | 4348 | 2.32% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 394 | 0.21% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 32061 | 17.11% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 57209 | 30.54% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 20198 | 10.78% |
| evolve (the per-event patient fold) | 918 | 0.49% |
| decide (the generator's dispatch) | 131777 | 70.35% |
| sim-check, whole namespace | 6597 | 3.52% |
| person-simulator (what :persons costs) | 7672 | 4.10% |
| patient-simulator (the module cohort's walk) | 1484 | 0.79% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 37425 | 19.98% |
| EDN parsing of the input log (reader, not invariants) | 19 | 0.01% |
| malli (residue after the 642d70a validator hoist) | 1013 | 0.54% |

#### Top-20 by INNERMOST PROJECT FRAME

Each sample attributed to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. This is the table
that names a fix site: the self-time table above is all
`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,
and the named-site table only sees sites someone thought to name.

| samples | share | innermost `ehrt.` frame |
|---|---|---|
| 32677 | 17.44% | `ehrt.sim_engine.run$select_person$fn__10323.invoke` |
| 31658 | 16.90% | `ehrt.sim_engine.decide$waiting_boarder.invokeStatic` |
| 16669 | 8.90% | `ehrt.sim_model.facility$occupancy_board$fn__6563.invoke` |
| 15958 | 8.52% | `ehrt.sim_engine.decide$waiting_boarder$fn__9944.invoke` |
| 15392 | 8.22% | `ehrt.sim_model.facility$occupancy_board.invokeStatic` |
| 9590 | 5.12% | `ehrt.sim_engine.decide$waiting_boarder$fn__9953.invoke` |
| 8579 | 4.58% | `ehrt.sim_engine.log_index$last_uncancelled_index$fn__9764.invoke` |
| 8214 | 4.38% | `ehrt.sim_engine.log_index$last_uncancelled_index$fn__9762.invoke` |
| 5285 | 2.82% | `ehrt.sim_engine.decide$eval10061$fn__10064$fn__10082.invoke` |
| 5197 | 2.77% | `ehrt.sim_engine.decide$eval10033$fn__10036.invoke` |
| 4748 | 2.53% | `ehrt.sim_engine.run$select_person.invokeStatic` |
| 2907 | 1.55% | `ehrt.sim_engine.decide$eval10061$fn__10064.invoke` |
| 2755 | 1.47% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 2488 | 1.33% | `ehrt.sim_engine.decide$eval10033$fn__10036$fn__10041.invoke` |
| 2406 | 1.28% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 1988 | 1.06% | `ehrt.sim_engine.decide$eval10033$fn__10036$fn__10048.invoke` |
| 1561 | 0.83% | `ehrt.person_simulator.process$walk_person$fn__12852.invoke` |
| 1432 | 0.76% | `ehrt.sim_engine.fold$apply_events$fn__9714.invoke` |
| 1327 | 0.71% | `ehrt.sim_engine.log_index$last_uncancelled_index.invokeStatic` |
| 1132 | 0.60% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |

