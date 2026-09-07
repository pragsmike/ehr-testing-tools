### `a7500-persons-before` — gen

`a7500-persons-before.gen.jfr` (20M), **7145** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 996 | 13.94% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 290 | 4.06% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 748` |
| 271 | 3.79% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 269 | 3.76% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 262 | 3.67% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 190 | 2.66% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 167 | 2.34% | `clojure.lang.RT.first(Object) line: 712` |
| 139 | 1.95% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 120 | 1.68% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |
| 116 | 1.62% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 116 | 1.62% | `clojure.lang.LazySeq.force() line: 50` |
| 108 | 1.51% | `clojure.lang.Numbers.category(Object) line: 1180` |
| 106 | 1.48% | `clojure.lang.PersistentHashMap$NodeSeq.create(Object[], int, ISeq) line: 1325` |
| 100 | 1.40% | `clojure.lang.PersistentVector.arrayFor(int) line: 158` |
| 93 | 1.30% | `ehrt.person_simulator.process$walk_person.invokeStatic(Object, Object, Object, Object, Object) line: 272` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1454 | 20.35% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1249 | 17.48% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 104 | 1.46% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 275 | 3.85% |
| decide (the generator's dispatch) | 2665 | 37.30% |
| sim-check, whole namespace | 1882 | 26.34% |
| person-simulator (what :persons costs) | 1341 | 18.77% |
| patient-simulator (the module cohort's walk) | 389 | 5.44% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 2 | 0.03% |
| malli (residue after the 642d70a validator hoist) | 392 | 5.49% |

#### Top-15 PROJECT frames, self AND inclusive

SELF attributes each sample to the first `ehrt.` frame in its stack —
the project function actually running, with the `clojure.lang`
machinery it is running THROUGH folded into it. INCLUSIVE counts
each sample once for every DISTINCT `ehrt.` frame anywhere in it,
so a caller carries everything under it. Ranked by SELF; inclusive
shares do not sum to 100%. This is the table that names a fix site:
the self-time table above is all `KeywordLookupSite`/`Util.equiv`
and belongs to no site at all, and the named-site table only sees
sites someone thought to name.

| self | self % | incl % | `ehrt.` frame |
|---|---|---|---|
| 576 | 8.06% | 13.06% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 515 | 7.21% | 7.32% | `ehrt.sim_engine.decide$eval10066$fn__10069$fn__10087.invoke` |
| 453 | 6.34% | 14.07% | `ehrt.sim_engine.fold$apply_events$fn__9742.invoke` |
| 375 | 5.25% | 11.07% | `ehrt.sim_engine.decide$eval10038$fn__10041.invoke` |
| 333 | 4.66% | 18.56% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 300 | 4.20% | 4.60% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 265 | 3.71% | 3.71% | `ehrt.sim_engine.decide$eval10038$fn__10041$fn__10046.invoke` |
| 235 | 3.29% | 3.29% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 227 | 3.18% | 12.40% | `ehrt.sim_engine.decide$eval10066$fn__10069.invoke` |
| 186 | 2.60% | 2.60% | `ehrt.sim_engine.fold$apply_events$fn__9769$fn__9771.invoke` |
| 180 | 2.52% | 2.52% | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 151 | 2.11% | 2.11% | `ehrt.sim_engine.decide$eval10038$fn__10041$fn__10053.invoke` |
| 137 | 1.92% | 1.92% | `ehrt.person_simulator.process$walk_person$fn__12863.invoke` |
| 119 | 1.67% | 4.27% | `ehrt.sim_engine.fold$apply_events$fn__9769.invoke` |
| 113 | 1.58% | 1.58% | `ehrt.person_simulator.persona$weighted_pick.invokeStatic` |

#### `apply-events` concern breakdown

Of the **1454** samples anywhere beneath `fold/apply-events`
(20.35% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 274 | 18.84% | 3.83% |
| :encounter-stamp -- `encounters/stamp-encounter` | 76 | 5.23% | 1.06% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 33 | 2.27% | 0.46% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 26 | 1.79% | 0.36% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 17 | 1.17% | 0.24% |
| :board -- `update-board` (ADR-0180 site 3) | 5 | 0.34% | 0.07% |
| :eligible-index -- `update-eligible` (ADR-0180 site 5) | 0 | 0.00% | 0.00% |
| **residual** -- no named concern frame | 1023 | 70.36% | 14.32% |

