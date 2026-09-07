### `a7500-persons-site7-before` — check

`a7500-persons-site7-before.check.jfr` (8.6M), **3035** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 310 | 10.21% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 151 | 4.98% | `java.io.LineNumberReader.read() line: 141` |
| 128 | 4.22% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 120 | 3.95% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 107 | 3.53% | `java.io.FilterReader.read() line: 65` |
| 104 | 3.43% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 98 | 3.23% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 91 | 3.00% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 80 | 2.64% | `clojure.core.protocols$fn__8207$G__8202__8220.invoke(Object, Object, Object) line: 13` |
| 63 | 2.08% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 58 | 1.91% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 48 | 1.58% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |
| 47 | 1.55% | `clojure.lang.RT.first(Object) line: 712` |
| 45 | 1.48% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 43 | 1.42% | `clojure.lang.PersistentVector.arrayFor(int) line: 158` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1462 | 48.17% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1462 | 48.17% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 107 | 3.53% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 255 | 8.40% |
| decide (the generator's dispatch) | 6 | 0.20% |
| sim-check, whole namespace | 2195 | 72.32% |
| person-simulator (what :persons costs) | 1 | 0.03% |
| patient-simulator (the module cohort's walk) | 7 | 0.23% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 613 | 20.20% |
| malli (residue after the 642d70a validator hoist) | 59 | 1.94% |

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
| 612 | 20.16% | 20.16% | `ehrt.cli.core$read_ground_truth_stdin$fn__14511.invoke` |
| 466 | 15.35% | 31.83% | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 187 | 6.16% | 6.16% | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 172 | 5.67% | 11.83% | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 133 | 4.38% | 12.72% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 97 | 3.20% | 3.20% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 96 | 3.16% | 3.26% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9767.invoke` |
| 92 | 3.03% | 14.86% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 45 | 1.48% | 1.48% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 45 | 1.48% | 1.48% | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 41 | 1.35% | 1.55% | `ehrt.sim_check.check$bed_fold$fn__11566.invoke` |
| 40 | 1.32% | 48.17% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 27 | 0.89% | 0.89% | `ehrt.sim_check.check$events_by_patient$fn__11044$fn__11046.invoke` |
| 25 | 0.82% | 1.22% | `ehrt.sim_check.check$admitted_occupies_one_slot$fn__11437.invoke` |
| 22 | 0.72% | 1.61% | `ehrt.sim_check.check$events_by_patient$fn__11044.invoke` |

#### `apply-events` concern breakdown

Of the **1462** samples anywhere beneath `fold/apply-events`
(48.17% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 253 | 17.31% | 8.34% |
| :encounter-stamp -- `encounters/stamp-encounter` | 97 | 6.63% | 3.20% |
| :eligible-index -- `update-eligible` (ADR-0180 site 5) | 0 | 0.00% | 0.00% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 0 | 0.00% | 0.00% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 0 | 0.00% | 0.00% |
| :board -- `update-board` (ADR-0180 site 3) | 0 | 0.00% | 0.00% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 0 | 0.00% | 0.00% |
| **residual** -- no named concern frame | 1112 | 76.06% | 36.64% |

