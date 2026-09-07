### `a7500-persons-site5` — gen

`a7500-persons-site5.gen.jfr` (18M), **6208** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 552 | 8.89% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 269 | 4.33% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 243 | 3.91% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 748` |
| 201 | 3.24% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 175 | 2.82% | `clojure.lang.RT.first(Object) line: 712` |
| 142 | 2.29% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 128 | 2.06% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 124 | 2.00% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 115 | 1.85% | `clojure.lang.LazySeq.force() line: 50` |
| 105 | 1.69% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 104 | 1.68% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |
| 93 | 1.50% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 82 | 1.32% | `clojure.core.protocols$fn__8207$G__8202__8220.invoke(Object, Object, Object) line: 13` |
| 80 | 1.29% | `ehrt.person_simulator.process$walk_person.invokeStatic(Object, Object, Object, Object, Object) line: 272` |
| 80 | 1.29% | `clojure.lang.PersistentHashMap$NodeSeq.create(Object[], int, ISeq) line: 1325` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1429 | 23.02% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1219 | 19.64% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 93 | 1.50% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 278 | 4.48% |
| decide (the generator's dispatch) | 1767 | 28.46% |
| sim-check, whole namespace | 1892 | 30.48% |
| person-simulator (what :persons costs) | 1319 | 21.25% |
| patient-simulator (the module cohort's walk) | 392 | 6.31% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 1 | 0.02% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 1 | 0.02% |
| malli (residue after the 642d70a validator hoist) | 394 | 6.35% |

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
| 578 | 9.31% | 15.37% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 442 | 7.12% | 16.24% | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 357 | 5.75% | 12.00% | `ehrt.sim_engine.decide$eval10055$fn__10058.invoke` |
| 317 | 5.11% | 5.51% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 301 | 4.85% | 21.01% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 247 | 3.98% | 3.98% | `ehrt.sim_engine.decide$eval10055$fn__10058$fn__10063.invoke` |
| 200 | 3.22% | 3.24% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 196 | 3.16% | 3.16% | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 163 | 2.63% | 2.63% | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 141 | 2.27% | 2.27% | `ehrt.sim_engine.decide$eval10055$fn__10058$fn__10070.invoke` |
| 133 | 2.14% | 2.14% | `ehrt.person_simulator.process$walk_person$fn__12860.invoke` |
| 127 | 2.05% | 4.67% | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 121 | 1.95% | 6.39% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 106 | 1.71% | 1.71% | `ehrt.person_simulator.persona$weighted_pick.invokeStatic` |
| 91 | 1.47% | 6.02% | `ehrt.sim_check.check$bed_fold.invokeStatic` |

#### `apply-events` concern breakdown

Of the **1429** samples anywhere beneath `fold/apply-events`
(23.02% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 276 | 19.31% | 4.45% |
| :encounter-stamp -- `encounters/stamp-encounter` | 72 | 5.04% | 1.16% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 32 | 2.24% | 0.52% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 21 | 1.47% | 0.34% |
| :eligible-index -- `update-eligible` (ADR-0180 site 5) | 13 | 0.91% | 0.21% |
| :board -- `update-board` (ADR-0180 site 3) | 10 | 0.70% | 0.16% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 9 | 0.63% | 0.14% |
| **residual** -- no named concern frame | 996 | 69.70% | 16.04% |

