### `a7500-persons-site7-after` — gen

`a7500-persons-site7-after.gen.jfr` (13M), **4321** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 277 | 6.41% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 261 | 6.04% | `clojure.lang.PersistentVector$TransientVector.conj(Object) line: 748` |
| 192 | 4.44% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 103 | 2.38% | `ehrt.person_simulator.process$walk_person.invokeStatic(Object, Object, Object, Object, Object) line: 272` |
| 101 | 2.34% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 96 | 2.22% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 82 | 1.90% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 81 | 1.87% | `clojure.lang.Numbers$DoubleOps.combine(Numbers$Ops) line: 636` |
| 68 | 1.57% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 67 | 1.55% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 64 | 1.48% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 54 | 1.25% | `clojure.lang.PersistentVector.arrayFor(int) line: 158` |
| 49 | 1.13% | `clojure.core$seq__5488.invokeStatic(Object) line: 139` |
| 45 | 1.04% | `clojure.lang.RT.first(Object) line: 712` |
| 45 | 1.04% | `clojure.lang.LazySeq.force() line: 50` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 214 | 4.95% |
| replay -- ADR-0169's 14-calls-per-check-all site | 0 | 0.00% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 39 | 0.90% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 1 | 0.02% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 1 | 0.02% |
| evolve (the per-event patient fold) | 30 | 0.69% |
| decide (the generator's dispatch) | 1047 | 24.23% |
| sim-check, whole namespace | 661 | 15.30% |
| person-simulator (what :persons costs) | 1403 | 32.47% |
| patient-simulator (the module cohort's walk) | 388 | 8.98% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 1 | 0.02% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 3 | 0.07% |
| malli (residue after the 642d70a validator hoist) | 361 | 8.35% |

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
| 594 | 13.75% | 22.98% | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 332 | 7.68% | 32.19% | `ehrt.person_simulator.process$persons.invokeStatic` |
| 305 | 7.06% | 7.66% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 212 | 4.91% | 4.91% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 187 | 4.33% | 4.33% | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 160 | 3.70% | 3.70% | `ehrt.person_simulator.process$walk_person$fn__12848.invoke` |
| 109 | 2.52% | 2.52% | `ehrt.person_simulator.persona$weighted_pick.invokeStatic` |
| 102 | 2.36% | 3.49% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 75 | 1.74% | 1.74% | `ehrt.person_simulator.process$year_variates.invokeStatic` |
| 67 | 1.55% | 1.55% | `ehrt.sim_model.facility$free$fn__6571.invoke` |
| 58 | 1.34% | 8.52% | `ehrt.patient_simulator.gmf_interpreter$run_module.invokeStatic` |
| 55 | 1.27% | 29.11% | `ehrt.sim_engine.run$run.invokeStatic` |
| 48 | 1.11% | 3.89% | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 46 | 1.06% | 1.06% | `ehrt.patient_simulator.gmf_interpreter$parse_dob.invokeStatic` |
| 39 | 0.90% | 1.04% | `ehrt.sim_model.facility$surge_slot_ids$fn__6559.invoke` |

#### `apply-events` concern breakdown

Of the **214** samples anywhere beneath `fold/apply-events`
(4.95% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 33 | 15.42% | 0.76% |
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 30 | 14.02% | 0.69% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 21 | 9.81% | 0.49% |
| :eligible-index -- `update-eligible` (ADR-0180 site 5) | 10 | 4.67% | 0.23% |
| :encounter-stamp -- `encounters/stamp-encounter` | 9 | 4.21% | 0.21% |
| :board -- `update-board` (ADR-0180 site 3) | 6 | 2.80% | 0.14% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 4 | 1.87% | 0.09% |
| **residual** -- no named concern frame | 101 | 47.20% | 2.34% |

