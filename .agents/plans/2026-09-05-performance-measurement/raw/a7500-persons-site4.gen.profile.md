### `a7500-persons-site4` — gen

`a7500-persons-site4.gen.jfr` (20M), **7470** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 909 | 12.17% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 318 | 4.26% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 300 | 4.02% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 748` |
| 268 | 3.59% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 249 | 3.33% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 183 | 2.45% | `clojure.lang.RT.first(Object) line: 712` |
| 157 | 2.10% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 142 | 1.90% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 140 | 1.87% | `clojure.lang.PersistentHashMap$NodeSeq.create(Object[], int, ISeq) line: 1325` |
| 120 | 1.61% | `clojure.lang.LazySeq.force() line: 50` |
| 119 | 1.59% | `clojure.lang.Numbers.category(Object) line: 1180` |
| 117 | 1.57% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |
| 112 | 1.50% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 97 | 1.30% | `ehrt.sim_engine.decide$eval10066$fn__10069$fn__10087.invoke(Object) line: 1470` |
| 94 | 1.26% | `ehrt.person_simulator.process$walk_person.invokeStatic(Object, Object, Object, Object, Object) line: 272` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1498 | 20.05% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1263 | 16.91% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 102 | 1.37% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 1 | 0.01% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 236 | 3.16% |
| decide (the generator's dispatch) | 2753 | 36.85% |
| sim-check, whole namespace | 1916 | 25.65% |
| person-simulator (what :persons costs) | 1508 | 20.19% |
| patient-simulator (the module cohort's walk) | 453 | 6.06% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 2 | 0.03% |
| EDN parsing of the input log (reader, not invariants) | 23 | 0.31% |
| malli (residue after the 642d70a validator hoist) | 384 | 5.14% |

#### Top-20 by INNERMOST PROJECT FRAME

Each sample attributed to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. This is the table
that names a fix site: the self-time table above is all
`KeywordLookupSite`/`Util.equiv` and belongs to no site at all,
and the named-site table only sees sites someone thought to name.

| samples | share | innermost `ehrt.` frame |
|---|---|---|
| 607 | 8.13% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 516 | 6.91% | `ehrt.sim_engine.decide$eval10066$fn__10069$fn__10087.invoke` |
| 474 | 6.35% | `ehrt.sim_engine.fold$apply_events$fn__9742.invoke` |
| 396 | 5.30% | `ehrt.sim_engine.decide$eval10038$fn__10041.invoke` |
| 356 | 4.77% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 312 | 4.18% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 217 | 2.90% | `ehrt.sim_engine.decide$eval10038$fn__10041$fn__10046.invoke` |
| 215 | 2.88% | `ehrt.sim_engine.decide$eval10066$fn__10069.invoke` |
| 209 | 2.80% | `ehrt.person_simulator.process$walk_person$fn__12863.invoke` |
| 204 | 2.73% | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 197 | 2.64% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 196 | 2.62% | `ehrt.sim_engine.fold$apply_events$fn__9769$fn__9771.invoke` |
| 174 | 2.33% | `ehrt.sim_engine.decide$eval10038$fn__10041$fn__10053.invoke` |
| 131 | 1.75% | `ehrt.sim_engine.fold$apply_events$fn__9769.invoke` |
| 131 | 1.75% | `ehrt.person_simulator.persona$weighted_pick.invokeStatic` |
| 116 | 1.55% | `ehrt.sim_engine.fold$apply_events$fn__9742$fn__9758.invoke` |
| 87 | 1.16% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 86 | 1.15% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 85 | 1.14% | `ehrt.sim_engine.fold$apply_events$fn__9742$fn__9750.invoke` |
| 81 | 1.08% | `ehrt.patient_simulator.gmf_interpreter$run_module.invokeStatic` |

