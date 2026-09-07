### `a7500-persons-site6` — check, ALLOCATION

`a7500-persons-site6.check.jfr`, **13489** `jdk.ObjectAllocationSample` events
(`settings=profile`'s own `300/s` throttle, stack traces on),
**20.6 GB** of estimated total allocation.

#### Top-15 by ALLOCATING PROJECT FRAME (innermost `ehrt.`)

| est. alloc | share | samples | `ehrt.` frame |
|---|---|---|---|
| 3.35 GB | 16.23% | 3115 | `(no project frame)` |
| 2.47 GB | 11.98% | 1401 | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 2.36 GB | 11.44% | 1870 | `ehrt.cli.core$read_ground_truth_stdin$fn__14511.invoke` |
| 1.68 GB | 8.13% | 729 | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 1.64 GB | 7.94% | 976 | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 1.27 GB | 6.17% | 572 | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 0.54 GB | 2.61% | 313 | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 0.46 GB | 2.24% | 221 | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 0.45 GB | 2.17% | 266 | `ehrt.sim_check.check$participants_of.invokeStatic` |
| 0.45 GB | 2.17% | 237 | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 0.38 GB | 1.83% | 150 | `ehrt.sim_engine.evolve$eval9569$fn__9571.invoke` |
| 0.34 GB | 1.63% | 195 | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 0.34 GB | 1.62% | 193 | `ehrt.sim_engine.evolve$eval9506$fn__9508.invoke` |
| 0.25 GB | 1.24% | 106 | `ehrt.sim_check.check$bed_fold$step__11561.invoke` |
| 0.23 GB | 1.12% | 190 | `ehrt.sim_check.check$events_by_patient$fn__11044$fn__11046.invoke` |

#### Top-15 by CLASS

| est. alloc | share | samples | class |
|---|---|---|---|
| 3.65 GB | 17.72% | 2141 | `java.lang.Object[]` |
| 1.71 GB | 8.28% | 958 | `clojure.lang.PersistentHashMap$INode[]` |
| 1.51 GB | 7.32% | 1532 | `byte[]` |
| 1.44 GB | 6.97% | 795 | `clojure.lang.PersistentVector$ChunkedSeq` |
| 0.95 GB | 4.60% | 463 | `java.util.concurrent.locks.ReentrantLock$NonfairSync` |
| 0.91 GB | 4.42% | 484 | `clojure.lang.LazySeq` |
| 0.85 GB | 4.12% | 523 | `clojure.lang.PersistentVector` |
| 0.64 GB | 3.13% | 509 | `int[]` |
| 0.64 GB | 3.12% | 559 | `java.lang.String` |
| 0.62 GB | 3.01% | 198 | `java.lang.StringBuilder` |
| 0.59 GB | 2.84% | 279 | `clojure.lang.PersistentArrayMap` |
| 0.57 GB | 2.78% | 271 | `clojure.lang.PersistentHashMap` |
| 0.56 GB | 2.69% | 258 | `clojure.lang.KeywordLookupSite$1` |
| 0.51 GB | 2.46% | 332 | `clojure.lang.ArrayChunk` |
| 0.49 GB | 2.40% | 500 | `java.util.ArrayList$Itr` |

