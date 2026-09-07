### `a22500-nopersons-post` — check

`a22500-nopersons-post.check.jfr` (19M), **8630** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 833 | 9.65% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 746 | 8.64% | `java.io.LineNumberReader.read() line: 141` |
| 515 | 5.97% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 273 | 3.16% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 267 | 3.09% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 249 | 2.89% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 237 | 2.75% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 214 | 2.48% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 209 | 2.42% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 202 | 2.34% | `java.io.FilterReader.read() line: 65` |
| 188 | 2.18% | `clojure.core.protocols$fn__8207$G__8202__8220.invoke(Object, Object, Object) line: 13` |
| 166 | 1.92% | `clojure.lang.LazySeq.force() line: 50` |
| 162 | 1.88% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 158 | 1.83% | `clojure.lang.PersistentHashMap$ArrayNode.assoc(int, int, Object, Object, Box) line: 419` |
| 143 | 1.66% | `clojure.lang.PersistentHashMap.valAt(Object) line: 158` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 4302 | 49.85% |
| replay -- ADR-0169's 14-calls-per-check-all site | 4302 | 49.85% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 310 | 3.59% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 119 | 1.38% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 739 | 8.56% |
| decide (the generator's dispatch) | 8 | 0.09% |
| sim-check, whole namespace | 6534 | 75.71% |
| person-simulator (what :persons costs) | 3 | 0.03% |
| patient-simulator (the module cohort's walk) | 9 | 0.10% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 1736 | 20.12% |
| malli (residue after the 642d70a validator hoist) | 127 | 1.47% |

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
| 1735 | 20.10% | 20.10% | `ehrt.cli.core$read_ground_truth_stdin$fn__14527.invoke` |
| 1349 | 15.63% | 33.08% | `ehrt.sim_engine.fold$apply_events$fn__9742.invoke` |
| 696 | 8.06% | 8.06% | `ehrt.sim_engine.fold$apply_events$fn__9769$fn__9771.invoke` |
| 412 | 4.77% | 12.84% | `ehrt.sim_engine.fold$apply_events$fn__9769.invoke` |
| 385 | 4.46% | 13.01% | `ehrt.sim_engine.fold$apply_events$fn__9742$fn__9758.invoke` |
| 308 | 3.57% | 3.70% | `ehrt.sim_engine.fold$apply_events$fn__9742$fn__9750.invoke` |
| 263 | 3.05% | 15.13% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 254 | 2.94% | 2.94% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 129 | 1.49% | 1.87% | `ehrt.sim_check.check$bed_fold$fn__11582.invoke` |
| 114 | 1.32% | 1.32% | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 110 | 1.27% | 1.27% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 87 | 1.01% | 1.88% | `ehrt.sim_check.check$events_by_patient$fn__11060.invoke` |
| 83 | 0.96% | 0.96% | `ehrt.sim_check.check$bed_fold$step__11577.invoke` |
| 83 | 0.96% | 49.85% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 75 | 0.87% | 0.87% | `ehrt.sim_check.check$events_by_patient$fn__11060$fn__11062.invoke` |

#### `apply-events` concern breakdown

Of the **4302** samples anywhere beneath `fold/apply-events`
(49.85% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 738 | 17.15% | 8.55% |
| :encounter-stamp -- `encounters/stamp-encounter` | 254 | 5.90% | 2.94% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 0 | 0.00% | 0.00% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 0 | 0.00% | 0.00% |
| :board -- `update-board` (ADR-0180 site 3) | 0 | 0.00% | 0.00% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 0 | 0.00% | 0.00% |
| **residual** -- no named concern frame | 3310 | 76.94% | 38.35% |

