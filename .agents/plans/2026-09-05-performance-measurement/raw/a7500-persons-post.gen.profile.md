### `a7500-persons-post` — gen

`a7500-persons-post.gen.jfr` (22M), **10081** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 1411 | 14.00% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 484 | 4.80% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 367 | 3.64% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 748` |
| 306 | 3.04% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 221 | 2.19% | `clojure.lang.RT.first(Object) line: 712` |
| 207 | 2.05% | `clojure.lang.Numbers.category(Object) line: 1180` |
| 189 | 1.87% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 167 | 1.66% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 163 | 1.62% | `clojure.lang.Numbers$DoubleOps.combine(Numbers$Ops) line: 636` |
| 157 | 1.56% | `clojure.lang.PersistentHashMap$NodeSeq.create(Object[], int, ISeq) line: 1325` |
| 149 | 1.48% | `clojure.lang.Util.pcequiv(Object, Object) line: 125` |
| 146 | 1.45% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |
| 146 | 1.45% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 141 | 1.40% | `clojure.lang.PersistentVector.arrayFor(int) line: 158` |
| 134 | 1.33% | `ehrt.person_simulator.process$walk_person.invokeStatic(Object, Object, Object, Object, Object) line: 272` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1744 | 17.30% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1425 | 14.14% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 113 | 1.12% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 1 | 0.01% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 2 | 0.02% |
| evolve (the per-event patient fold) | 304 | 3.02% |
| decide (the generator's dispatch) | 3680 | 36.50% |
| sim-check, whole namespace | 2160 | 21.43% |
| person-simulator (what :persons costs) | 2298 | 22.80% |
| patient-simulator (the module cohort's walk) | 618 | 6.13% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 2 | 0.02% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 1 | 0.01% |
| malli (residue after the 642d70a validator hoist) | 567 | 5.62% |

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
| 886 | 8.79% | 16.67% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 639 | 6.34% | 6.49% | `ehrt.sim_engine.decide$eval10066$fn__10069$fn__10087.invoke` |
| 515 | 5.11% | 12.01% | `ehrt.sim_engine.fold$apply_events$fn__9742.invoke` |
| 488 | 4.84% | 10.40% | `ehrt.sim_engine.decide$eval10038$fn__10041.invoke` |
| 475 | 4.71% | 22.40% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 448 | 4.44% | 4.80% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 425 | 4.22% | 4.22% | `ehrt.person_simulator.process$walk_person$fn__12863.invoke` |
| 314 | 3.11% | 3.11% | `ehrt.sim_engine.decide$eval10038$fn__10041$fn__10046.invoke` |
| 303 | 3.01% | 3.01% | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 266 | 2.64% | 11.20% | `ehrt.sim_engine.decide$eval10066$fn__10069.invoke` |
| 245 | 2.43% | 2.43% | `ehrt.sim_engine.decide$eval10038$fn__10041$fn__10053.invoke` |
| 234 | 2.32% | 2.32% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 218 | 2.16% | 2.16% | `ehrt.sim_engine.fold$apply_events$fn__9769$fn__9771.invoke` |
| 157 | 1.56% | 1.56% | `ehrt.person_simulator.persona$weighted_pick.invokeStatic` |
| 146 | 1.45% | 4.43% | `ehrt.sim_engine.fold$apply_events$fn__9742$fn__9758.invoke` |

#### `apply-events` concern breakdown

Of the **1744** samples anywhere beneath `fold/apply-events`
(17.30% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 301 | 17.26% | 2.99% |
| :encounter-stamp -- `encounters/stamp-encounter` | 89 | 5.10% | 0.88% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 49 | 2.81% | 0.49% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 25 | 1.43% | 0.25% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 22 | 1.26% | 0.22% |
| :board -- `update-board` (ADR-0180 site 3) | 19 | 1.09% | 0.19% |
| **residual** -- no named concern frame | 1239 | 71.04% | 12.29% |

