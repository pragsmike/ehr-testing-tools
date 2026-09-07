### `a7500-persons-before6` — check

`a7500-persons-before6.check.jfr` (8.3M), **2853** `jdk.ExecutionSample` samples.

#### Top-15 self-time frames (the TOP frame of each sample)

| samples | share | frame |
|---|---|---|
| 301 | 10.55% | `clojure.lang.KeywordLookupSite$1.get(Object) line: 45` |
| 157 | 5.50% | `java.io.LineNumberReader.read() line: 141` |
| 139 | 4.87% | `clojure.lang.PersistentHashMap$BitmapIndexedNode.find(int, int, Object, Object) line: 789` |
| 128 | 4.49% | `java.io.FilterReader.read() line: 65` |
| 95 | 3.33% | `clojure.lang.Util.hasheq(Object) line: 170` |
| 94 | 3.29% | `clojure.lang.Util.equiv(Object, Object) line: 33` |
| 79 | 2.77% | `clojure.lang.PersistentArrayMap.indexOf(Object) line: 318` |
| 76 | 2.66% | `clojure.lang.Util.equiv(Object, Object) line: 30` |
| 66 | 2.31% | `clojure.lang.Util.dohasheq(IHashEq) line: 177` |
| 65 | 2.28% | `clojure.lang.LazySeq.force() line: 50` |
| 64 | 2.24% | `clojure.lang.RT.get(Object, Object) line: 780` |
| 49 | 1.72% | `java.io.PushbackReader.read() line: 86` |
| 41 | 1.44% | `clojure.lang.PersistentHashMap$ArrayNode.find(int, int, Object, Object) line: 454` |
| 40 | 1.40% | `java.lang.AbstractStringBuilder.ensureCapacityInternal(int) line: 243` |
| 40 | 1.40% | `clojure.lang.Util.pcequiv(Object, Object) line: 125` |

#### Named sites, INCLUSIVE share (the site appears anywhere in the sample)

| site | samples | share |
|---|---|---|
| apply-events (the one fold both phases run) | 1341 | 47.00% |
| replay -- ADR-0169's 14-calls-per-check-all site | 1341 | 47.00% |
| occupancy-within-capacity (ADR-0169: 54.9% of check) | 103 | 3.61% |
| occupancy-board (ADR-0169 OUT-list, 8.1% of generate) | 0 | 0.00% |
| decide :discharge's boarder sort-by (ADR-0169 OUT-list, ~7.9%) | 0 | 0.00% |
| last-uncancelled-index (ADR-0169 F-3, 5.9%) | 0 | 0.00% |
| evolve (the per-event patient fold) | 202 | 7.08% |
| decide (the generator's dispatch) | 7 | 0.25% |
| sim-check, whole namespace | 2058 | 72.13% |
| person-simulator (what :persons costs) | 2 | 0.07% |
| patient-simulator (the module cohort's walk) | 11 | 0.39% |
| select-person (NOT on any prior list -- full pool scan per arrival) | 0 | 0.00% |
| EDN parsing of the input log -- EdnReader, which is what clojure.edn/read uses (cli/read-ground-truth-stdin); LispReader here read 0.35% of a phase whose parse is 20% | 566 | 19.84% |
| malli (residue after the 642d70a validator hoist) | 43 | 1.51% |

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
| 565 | 19.80% | 19.80% | `ehrt.cli.core$read_ground_truth_stdin$fn__14524.invoke` |
| 438 | 15.35% | 30.81% | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 191 | 6.69% | 6.69% | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 137 | 4.80% | 11.50% | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 136 | 4.77% | 11.85% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 107 | 3.75% | 3.75% | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 89 | 3.12% | 3.22% | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9767.invoke` |
| 89 | 3.12% | 14.41% | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 40 | 1.40% | 1.75% | `ehrt.sim_check.check$bed_fold$fn__11579.invoke` |
| 34 | 1.19% | 1.19% | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 32 | 1.12% | 1.12% | `ehrt.sim_engine.event_schema$valid_event_QMARK_.invokeStatic` |
| 31 | 1.09% | 1.09% | `ehrt.sim_check.check$events_by_patient$fn__11057$fn__11059.invoke` |
| 28 | 0.98% | 2.07% | `ehrt.sim_check.check$events_by_patient$fn__11057.invoke` |
| 27 | 0.95% | 47.00% | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 25 | 0.88% | 0.98% | `ehrt.sim_check.check$outpatient_patients_occupy_no_bed$fn__11482.invoke` |

#### `apply-events` concern breakdown

Of the **1341** samples anywhere beneath `fold/apply-events`
(47.00% of the phase),
which of the fold's own named per-concern functions they are in. A
sample in two concerns counts in both, so these do not sum to the
total; the residual row is the samples in NONE of them, which is
`apply-events`' own machinery plus the ten concerns of
`full-algebra` that are inline forms with no frame of their own.

| concern | samples | % of `apply-events` | % of phase |
|---|---|---|---|
| :patient-state -- the `evolve` MULTIMETHOD (any fn of `ehrt.sim-engine.evolve`) | 202 | 15.06% | 7.08% |
| :encounter-stamp -- `encounters/stamp-encounter` | 107 | 7.98% | 3.75% |
| :eligible-index -- `update-eligible` (ADR-0180 site 5) | 0 | 0.00% | 0.00% |
| :cancel-index -- `update-cancel-index` (ADR-0180 site 4) | 0 | 0.00% | 0.00% |
| :boarder-index -- `update-boarders` (ADR-0180 site 1) | 0 | 0.00% | 0.00% |
| :board -- `update-board` (ADR-0180 site 3) | 0 | 0.00% | 0.00% |
| :bed-index -- `update-beds` (arc 3b sweep 2) | 0 | 0.00% | 0.00% |
| **residual** -- no named concern frame | 1032 | 76.96% | 36.17% |

