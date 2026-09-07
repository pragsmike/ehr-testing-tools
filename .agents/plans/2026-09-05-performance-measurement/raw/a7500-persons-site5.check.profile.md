### `a7500-persons-site5` — check

`a7500-persons-site5.check.jfr` (8.5M), **3037** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 312 | 10.27% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 154 | 5.07% | `java.io.LineNumberReader.read() line: 141` |
| 139 | 4.58% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 112 | 3.69% | `java.io.FilterReader.read() line: 65` |
| 93 | 3.06% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 92 | 3.03% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 70 | 2.30% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 70 | 2.30% | `clojure.lang.MethodImplCache.fnFor(Class) line: 67` |
| 68 | 2.24% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 65 | 2.14% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 64 | 2.11% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 54 | 1.78% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |
| 52 | 1.71% | `clojure.lang.PersistentVector.arrayFor(int) line: 158` |
| 50 | 1.65% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 45 | 1.48% | `clojure.lang.RT.first(Object) line: 712` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1456 | 47.94% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1456 | 47.94% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 104 | 3.42% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 262 | 8.63% |
| decide (the generator's dispatch) | 6 | 0.20% |
| sim-check, whole namespace | 2226 | 73.30% |
| person-simulator (what :persons costs) | 5 | 0.16% |
| patient-simulator (the module cohort's walk) | 6 | 0.20% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 570 | 18.77% |
| malli (residue after the 642d70a validator hoist) | 51 | 1.68% |

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
| 569 | 18.74% | 18.74% | `ehrt.cli.core$read_ground_truth_stdin$fn__14524.invoke` |
| 455 | 14.98% | 31.71% | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 197 | 6.49% | 6.49% | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 166 | 5.47% | 11.95% | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 144 | 4.74% | 13.34% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 102 | 3.36% | 15.61% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 96 | 3.16% | 3.16% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 86 | 2.83% | 3.06% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9767.invoke` |
| 42 | 1.38% | 1.38% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 39 | 1.28% | 1.28% | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 38 | 1.25% | 2.04% | `ehrt.sim_check.check$events_by_patient$fn__11057.invoke` |
| 38 | 1.25% | 1.45% | `ehrt.sim_check.check$bed_fold$fn__11579.invoke` |
| 34 | 1.12% | 47.94% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 27 | 0.89% | 1.81% | `ehrt.sim_check.check$no_double_occupancy$fn__11411.invoke` |
| 25 | 0.82% | 1.38% | `ehrt.sim_engine.evolve$eval9569$fn__9571.invoke` |

#### `apply-events` concern breakdown

Of the **1456** samples anywhere beneath `fold/apply-events`
(47.94% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 261 | 17.93% | 8.59% |
| :encounter-stamp -- `encounters/stamp-encounter` | 96 | 6.59% | 3.16% |
| :eligible-index -- `update-eligible` (ADR-0180 site 5) | 0 | 0.00% | 0.00% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 0 | 0.00% | 0.00% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 0 | 0.00% | 0.00% |
| :board -- `update-board` (ADR-0180 site 3) | 0 | 0.00% | 0.00% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 0 | 0.00% | 0.00% |
| **residual** -- no named concern frame | 1099 | 75.48% | 36.19% |

