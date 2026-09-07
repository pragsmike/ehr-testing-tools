### `a22500-nopersons-post` — gen

`a22500-nopersons-post.gen.jfr` (54M), **32394** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 8050 | 24.85% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 2124 | 6.56% | `clojure.lang.PersistentHashMap$NodeSeq.create(Object[], int, ISeq) line: 1325` |
| 1875 | 5.79% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 1664 | 5.14% | `clojure.lang.Numbers.category(Object) line: 1180` |
| 1389 | 4.29% | `clojure.lang.RT.first(Object) line: 712` |
| 1209 | 3.73% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 1158 | 3.57% | `clojure.lang.RT.first(Object) line: 713` |
| 791 | 2.44% | `java.util.concurrent.locks.AbstractQueuedSynchronizer.signalNext(AbstractQueuedSynchronizer$Node) line: 644` |
| 788 | 2.43% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 760 | 2.35% | `clojure.lang.LazySeq.force() line: 50` |
| 600 | 1.85% | `java.lang.String.equals(Object) line: 1861` |
| 521 | 1.61% | `clojure.lang.KeywordLookupSite.ilookupThunk(Class) line: 42` |
| 492 | 1.52% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 482 | 1.49% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 477 | 1.47% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 4729 | 14.60% |
| replay -- ADR-0169's 14-calls-per-check-all site | 4025 | 12.43% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 326 | 1.01% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 157 | 0.48% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 2 | 0.01% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 4 | 0.01% |
| evolve (the per-event patient fold) | 834 | 2.57% |
| decide (the generator's dispatch) | 23414 | 72.28% |
| sim-check, whole namespace | 6086 | 18.79% |
| person-simulator (what :persons costs) | 3 | 0.01% |
| patient-simulator (the module cohort's walk) | 288 | 0.89% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log (reader, not invariants) | 28 | 0.09% |
| malli (residue after the 642d70a validator hoist) | 660 | 2.04% |

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
| 6201 | 19.14% | 19.55% | `ehrt.sim_engine.decide$eval10066$fn__10069$fn__10087.invoke` |
| 4719 | 14.57% | 31.38% | `ehrt.sim_engine.decide$eval10038$fn__10041.invoke` |
| 3126 | 9.65% | 9.65% | `ehrt.sim_engine.decide$eval10038$fn__10041$fn__10046.invoke` |
| 2818 | 8.70% | 34.06% | `ehrt.sim_engine.decide$eval10066$fn__10069.invoke` |
| 2320 | 7.16% | 7.16% | `ehrt.sim_engine.decide$eval10038$fn__10041$fn__10053.invoke` |
| 1430 | 4.41% | 9.88% | `ehrt.sim_engine.fold$apply_events$fn__9742.invoke` |
| 971 | 3.00% | 3.00% | `ehrt.sim_engine.decide$eval10066$fn__10069$never_mergeable_QMARK___10073.invoke` |
| 934 | 2.88% | 3.19% | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 875 | 2.70% | 2.70% | `ehrt.sim_engine.decide$eval10066$fn__10069$fn__10082.invoke` |
| 783 | 2.42% | 2.42% | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 600 | 1.85% | 1.85% | `ehrt.sim_engine.fold$apply_events$fn__9769$fn__9771.invoke` |
| 446 | 1.38% | 3.23% | `ehrt.sim_engine.fold$apply_events$fn__9769.invoke` |
| 354 | 1.09% | 3.66% | `ehrt.sim_engine.fold$apply_events$fn__9742$fn__9758.invoke` |
| 270 | 0.83% | 0.83% | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 254 | 0.78% | 0.78% | `ehrt.sim_model.facility$free$fn__6571.invoke` |

#### `apply-events` concern breakdown

Of the **4729** samples anywhere beneath `fold/apply-events`
(14.60% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 833 | 17.61% | 2.57% |
| :encounter-stamp -- `encounters/stamp-encounter` | 226 | 4.78% | 0.70% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 123 | 2.60% | 0.38% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 61 | 1.29% | 0.19% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 27 | 0.57% | 0.08% |
| :board -- `update-board` (ADR-0180 site 3) | 22 | 0.47% | 0.07% |
| **residual** -- no named concern frame | 3437 | 72.68% | 10.61% |

