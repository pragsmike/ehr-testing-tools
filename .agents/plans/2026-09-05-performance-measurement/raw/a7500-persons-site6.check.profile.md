### `a7500-persons-site6` — check

`a7500-persons-site6.check.jfr` (8.4M), **3016** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 320 | 10.61% | `java.io.FilterReader.read() line: 65` |
| 285 | 9.45% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 153 | 5.07% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 108 | 3.58% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 102 | 3.38% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 83 | 2.75% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 82 | 2.72% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 70 | 2.32% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 65 | 2.16% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 64 | 2.12% | `clojure.lang.LazySeq.force() line: 50` |
| 60 | 1.99% | `clojure.core.protocols$fn__8207$G__8202__8220.invoke(Object, Object, Object) line: 13` |
| 49 | 1.62% | `java.util.regex.Matcher.reset() line: 451` |
| 43 | 1.43% | `clojure.lang.RT.first(Object) line: 712` |
| 40 | 1.33% | `clojure.lang.Util.pcequiv(Object, Object) line: 125` |
| 39 | 1.29% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1444 | 47.88% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1444 | 47.88% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 96 | 3.18% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 259 | 8.59% |
| decide (the generator's dispatch) | 5 | 0.17% |
| sim-check, whole namespace | 2162 | 71.68% |
| person-simulator (what :persons costs) | 5 | 0.17% |
| patient-simulator (the module cohort's walk) | 8 | 0.27% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 624 | 20.69% |
| malli (residue after the 642d70a validator hoist) | 53 | 1.76% |

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
| 623 | 20.66% | 20.66% | `ehrt.cli.core$read_ground_truth_stdin$fn__14511.invoke` |
| 469 | 15.55% | 32.19% | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 194 | 6.43% | 6.43% | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 150 | 4.97% | 11.41% | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 123 | 4.08% | 12.67% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 100 | 3.32% | 3.45% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9767.invoke` |
| 97 | 3.22% | 14.26% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 94 | 3.12% | 3.12% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 46 | 1.53% | 1.53% | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 41 | 1.36% | 1.36% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 39 | 1.29% | 1.49% | `ehrt.sim_check.check$bed_fold$fn__11566.invoke` |
| 35 | 1.16% | 1.99% | `ehrt.sim_check.check$events_by_patient$fn__11044.invoke` |
| 34 | 1.13% | 47.88% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 27 | 0.90% | 1.72% | `ehrt.sim_engine.evolve$eval9506$fn__9508.invoke` |
| 25 | 0.83% | 0.83% | `ehrt.sim_engine.evolve$eval9569$fn__9571$fn__9574.invoke` |

#### `apply-events` concern breakdown

Of the **1444** samples anywhere beneath `fold/apply-events`
(47.88% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 259 | 17.94% | 8.59% |
| :encounter-stamp -- `encounters/stamp-encounter` | 94 | 6.51% | 3.12% |
| :eligible-index -- `update-eligible` (ADR-0180 site 5) | 0 | 0.00% | 0.00% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 0 | 0.00% | 0.00% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 0 | 0.00% | 0.00% |
| :board -- `update-board` (ADR-0180 site 3) | 0 | 0.00% | 0.00% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 0 | 0.00% | 0.00% |
| **residual** -- no named concern frame | 1091 | 75.55% | 36.17% |

