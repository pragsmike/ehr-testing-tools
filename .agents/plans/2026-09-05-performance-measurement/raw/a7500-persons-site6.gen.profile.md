### `a7500-persons-site6` — gen

`a7500-persons-site6.gen.jfr` (17M), **5708** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 451 | 7.90% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 257 | 4.50% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 748` |
| 231 | 4.05% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 176 | 3.08% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 136 | 2.38% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 125 | 2.19% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 108 | 1.89% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 107 | 1.87% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 98 | 1.72% | `ehrt.person_simulator.process$walk_person.invokeStatic(Object, Object, Object, Object, Object) line: 272` |
| 91 | 1.59% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 91 | 1.59% | `clojure.lang.RT.first(Object) line: 712` |
| 86 | 1.51% | `clojure.core$seq__5488.invokeStatic(Object) line: 139` |
| 84 | 1.47% | `clojure.core.protocols$fn__8207$G__8202__8220.invoke(Object, Object, Object) line: 13` |
| 83 | 1.45% | `clojure.lang.Util.pcequiv(Object, Object) line: 125` |
| 73 | 1.28% | `clojure.lang.PersistentVector.arrayFor(int) line: 158` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1518 | 26.59% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1307 | 22.90% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 107 | 1.87% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 317 | 5.55% |
| decide (the generator's dispatch) | 1069 | 18.73% |
| sim-check, whole namespace | 1981 | 34.71% |
| person-simulator (what :persons costs) | 1404 | 24.60% |
| patient-simulator (the module cohort's walk) | 420 | 7.36% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 3 | 0.05% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 1 | 0.02% |
| malli (residue after the 642d70a validator hoist) | 362 | 6.34% |

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
| 603 | 10.56% | 17.31% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 449 | 7.87% | 18.71% | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 333 | 5.83% | 24.26% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 330 | 5.78% | 6.18% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 208 | 3.64% | 3.70% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 179 | 3.14% | 3.14% | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 166 | 2.91% | 2.91% | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 144 | 2.52% | 2.52% | `ehrt.person_simulator.process$walk_person$fn__12847.invoke` |
| 133 | 2.33% | 7.80% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 125 | 2.19% | 2.19% | `ehrt.person_simulator.persona$weighted_pick.invokeStatic` |
| 118 | 2.07% | 5.20% | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 93 | 1.63% | 6.45% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 82 | 1.44% | 1.52% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9767.invoke` |
| 81 | 1.42% | 1.42% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 75 | 1.31% | 7.04% | `ehrt.patient_simulator.gmf_interpreter$run_module.invokeStatic` |

#### `apply-events` concern breakdown

Of the **1518** samples anywhere beneath `fold/apply-events`
(26.59% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 312 | 20.55% | 5.47% |
| :encounter-stamp -- `encounters/stamp-encounter` | 81 | 5.34% | 1.42% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 27 | 1.78% | 0.47% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 20 | 1.32% | 0.35% |
| :board -- `update-board` (ADR-0180 site 3) | 12 | 0.79% | 0.21% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 11 | 0.72% | 0.19% |
| :eligible-index -- `update-eligible` (ADR-0180 site 5) | 6 | 0.40% | 0.11% |
| **residual** -- no named concern frame | 1049 | 69.10% | 18.38% |

