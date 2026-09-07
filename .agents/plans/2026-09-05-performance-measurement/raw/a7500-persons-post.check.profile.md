### `a7500-persons-post` — check

`a7500-persons-post.check.jfr` (9.0M), **3399** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 342 | 10.06% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 273 | 8.03% | `java.io.LineNumberReader.read() line: 141` |
| 174 | 5.12% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 101 | 2.97% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 96 | 2.82% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 93 | 2.74% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 89 | 2.62% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 88 | 2.59% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 85 | 2.50% | `java.io.FilterReader.read() line: 65` |
| 76 | 2.24% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 75 | 2.21% | `clojure.core.protocols$fn__8207$G__8202__8220.invoke(Object, Object, Object) line: 13` |
| 74 | 2.18% | `clojure.lang.LazySeq.force() line: 50` |
| 65 | 1.91% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 49 | 1.44% | `clojure.lang.Util.pcequiv(Object, Object) line: 125` |
| 46 | 1.35% | `java.lang.AbstractStringBuilder.ensureCapacityInternal(int) line: 243` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1696 | 49.90% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1696 | 49.90% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 110 | 3.24% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 332 | 9.77% |
| decide (the generator's dispatch) | 1 | 0.03% |
| sim-check, whole namespace | 2497 | 73.46% |
| person-simulator (what :persons costs) | 1 | 0.03% |
| patient-simulator (the module cohort's walk) | 8 | 0.24% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log (reader, not invariants) | 27 | 0.79% |
| malli (residue after the 642d70a validator hoist) | 50 | 1.47% |

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
| 638 | 18.77% | 18.77% | `ehrt.cli.core$read_ground_truth_stdin$fn__14527.invoke` |
| 532 | 15.65% | 33.86% | `ehrt.sim_engine.fold$apply_events$fn__9742.invoke` |
| 211 | 6.21% | 6.21% | `ehrt.sim_engine.fold$apply_events$fn__9769$fn__9771.invoke` |
| 184 | 5.41% | 11.62% | `ehrt.sim_engine.fold$apply_events$fn__9769.invoke` |
| 138 | 4.06% | 13.83% | `ehrt.sim_engine.fold$apply_events$fn__9742$fn__9758.invoke` |
| 126 | 3.71% | 3.80% | `ehrt.sim_engine.fold$apply_events$fn__9742$fn__9750.invoke` |
| 103 | 3.03% | 3.03% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 97 | 2.85% | 13.50% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 46 | 1.35% | 1.35% | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 46 | 1.35% | 49.90% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 44 | 1.29% | 1.44% | `ehrt.sim_check.check$bed_fold$fn__11582.invoke` |
| 36 | 1.06% | 1.06% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 32 | 0.94% | 0.94% | `ehrt.sim_check.check$events_by_patient$fn__11060$fn__11062.invoke` |
| 29 | 0.85% | 0.85% | `ehrt.sim_engine.evolve$eval9569$fn__9571$fn__9574.invoke` |
| 29 | 0.85% | 0.85% | `ehrt.sim_engine.evolve$eval9482$fn__9484.invoke` |

#### `apply-events` concern breakdown

Of the **1696** samples anywhere beneath `fold/apply-events`
(49.90% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 332 | 19.58% | 9.77% |
| :encounter-stamp -- `encounters/stamp-encounter` | 103 | 6.07% | 3.03% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 0 | 0.00% | 0.00% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 0 | 0.00% | 0.00% |
| :board -- `update-board` (ADR-0180 site 3) | 0 | 0.00% | 0.00% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 0 | 0.00% | 0.00% |
| **residual** -- no named concern frame | 1261 | 74.35% | 37.10% |

