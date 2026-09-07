### `a7500-persons-site7-before` — gen

`a7500-persons-site7-before.gen.jfr` (18M), **6244** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 535 | 8.57% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 310 | 4.96% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 748` |
| 300 | 4.80% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 151 | 2.42% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 145 | 2.32% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 141 | 2.26% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 139 | 2.23% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 118 | 1.89% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 111 | 1.78% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 97 | 1.55% | `ehrt.person_simulator.process$walk_person.invokeStatic(Object, Object, Object, Object, Object) line: 272` |
| 93 | 1.49% | `clojure.lang.RT.first(Object) line: 712` |
| 91 | 1.46% | `clojure.lang.Numbers$DoubleOps.combine(Numbers$Ops) line: 636` |
| 82 | 1.31% | `clojure.lang.LazySeq.force() line: 50` |
| 81 | 1.30% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |
| 79 | 1.27% | `clojure.lang.RT.first(Object) line: 713` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1513 | 24.23% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1283 | 20.55% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 101 | 1.62% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 2 | 0.03% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 276 | 4.42% |
| decide (the generator's dispatch) | 1196 | 19.15% |
| sim-check, whole namespace | 1944 | 31.13% |
| person-simulator (what :persons costs) | 1667 | 26.70% |
| patient-simulator (the module cohort's walk) | 507 | 8.12% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 3 | 0.05% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 2 | 0.03% |
| malli (residue after the 642d70a validator hoist) | 444 | 7.11% |

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
| 675 | 10.81% | 18.99% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 448 | 7.17% | 16.37% | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 375 | 6.01% | 26.27% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 333 | 5.33% | 5.73% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 220 | 3.52% | 3.52% | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 217 | 3.48% | 3.48% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 214 | 3.43% | 3.43% | `ehrt.person_simulator.process$walk_person$fn__12847.invoke` |
| 187 | 2.99% | 2.99% | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 138 | 2.21% | 5.20% | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 132 | 2.11% | 2.11% | `ehrt.person_simulator.persona$weighted_pick.invokeStatic` |
| 115 | 1.84% | 6.26% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 96 | 1.54% | 6.23% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 92 | 1.47% | 1.47% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 84 | 1.35% | 1.35% | `ehrt.person_simulator.process$year_variates.invokeStatic` |
| 79 | 1.27% | 1.38% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9767.invoke` |

#### `apply-events` concern breakdown

Of the **1513** samples anywhere beneath `fold/apply-events`
(24.23% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 276 | 18.24% | 4.42% |
| :encounter-stamp -- `encounters/stamp-encounter` | 92 | 6.08% | 1.47% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 36 | 2.38% | 0.58% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 19 | 1.26% | 0.30% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 14 | 0.93% | 0.22% |
| :eligible-index -- `update-eligible` (ADR-0180 site 5) | 9 | 0.59% | 0.14% |
| :board -- `update-board` (ADR-0180 site 3) | 7 | 0.46% | 0.11% |
| **residual** -- no named concern frame | 1060 | 70.06% | 16.98% |

