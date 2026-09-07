### `a7500-persons-before6` — gen

`a7500-persons-before6.gen.jfr` (19M), **6463** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 587 | 9.08% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 306 | 4.73% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 273 | 4.22% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 754` |
| 211 | 3.26% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 198 | 3.06% | `clojure.lang.RT.first(Object) line: 712` |
| 168 | 2.60% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 158 | 2.44% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 132 | 2.04% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 109 | 1.69% | `clojure.lang.LazySeq.force() line: 50` |
| 105 | 1.62% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 103 | 1.59% | `clojure.lang.Numbers.category(Object) line: 1180` |
| 102 | 1.58% | `clojure.lang.PersistentHashMap$NodeSeq.create(Object[], int, ISeq) line: 1325` |
| 101 | 1.56% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |
| 96 | 1.49% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 78 | 1.21% | `ehrt.person_simulator.process$walk_person.invokeStatic(Object, Object, Object, Object, Object) line: 272` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1484 | 22.96% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1250 | 19.34% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 91 | 1.41% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 1 | 0.02% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 268 | 4.15% |
| decide (the generator's dispatch) | 1886 | 29.18% |
| sim-check, whole namespace | 1893 | 29.29% |
| person-simulator (what :persons costs) | 1396 | 21.60% |
| patient-simulator (the module cohort's walk) | 453 | 7.01% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 1 | 0.02% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 2 | 0.03% |
| malli (residue after the 642d70a validator hoist) | 386 | 5.97% |

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
| 607 | 9.39% | 15.05% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 461 | 7.13% | 16.06% | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 403 | 6.24% | 12.58% | `ehrt.sim_engine.decide$eval10055$fn__10058.invoke` |
| 331 | 5.12% | 21.27% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 320 | 4.95% | 5.45% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 257 | 3.98% | 3.98% | `ehrt.sim_engine.decide$eval10055$fn__10058$fn__10063.invoke` |
| 188 | 2.91% | 2.91% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 187 | 2.89% | 2.89% | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 182 | 2.82% | 2.82% | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 158 | 2.44% | 2.44% | `ehrt.sim_engine.decide$eval10055$fn__10058$fn__10070.invoke` |
| 151 | 2.34% | 2.34% | `ehrt.person_simulator.process$walk_person$fn__12860.invoke` |
| 129 | 2.00% | 4.81% | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 100 | 1.55% | 1.64% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9767.invoke` |
| 92 | 1.42% | 6.31% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 91 | 1.41% | 1.41% | `ehrt.person_simulator.persona$weighted_pick.invokeStatic` |

#### `apply-events` concern breakdown

Of the **1484** samples anywhere beneath `fold/apply-events`
(22.96% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 267 | 17.99% | 4.13% |
| :encounter-stamp -- `encounters/stamp-encounter` | 74 | 4.99% | 1.14% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 36 | 2.43% | 0.56% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 23 | 1.55% | 0.36% |
| :eligible-index -- `update-eligible` (ADR-0180 site 5) | 14 | 0.94% | 0.22% |
| :board -- `update-board` (ADR-0180 site 3) | 12 | 0.81% | 0.19% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 8 | 0.54% | 0.12% |
| **residual** -- no named concern frame | 1050 | 70.75% | 16.25% |

