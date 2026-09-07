### `a7500-persons-before6` — check, ALLOCATION

`a7500-persons-before6.check.jfr`, **13062** `jdk.ObjectAllocationSample` events
(`settings=profile`'s own `300/s` throttle, stack traces on),
**20.6 GB** of estimated total allocation.

#### Top-15 by ALLOCATING PROJECT FRAME (innermost `ehrt.`)

| est. alloc | share | samples | `ehrt.` frame |
|---|---|---|---|
| 3.11 GB | 15.09% | 3075 | `(no project frame)` |
| 2.64 GB | 12.81% | 1439 | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 2.30 GB | 11.19% | 1678 | `ehrt.cli.core$read_ground_truth_stdin$fn__14524.invoke` |
| 1.85 GB | 9.01% | 628 | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 1.50 GB | 7.29% | 988 | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 1.18 GB | 5.75% | 433 | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 0.64 GB | 3.12% | 314 | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 0.53 GB | 2.58% | 253 | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 0.38 GB | 1.85% | 213 | `ehrt.sim_engine.evolve$eval9506$fn__9508.invoke` |
| 0.35 GB | 1.68% | 230 | `ehrt.sim_check.check$participants_of.invokeStatic` |
| 0.34 GB | 1.67% | 264 | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 0.33 GB | 1.63% | 180 | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 0.33 GB | 1.60% | 193 | `ehrt.sim_check.check$events_by_patient$fn__11057$fn__11059.invoke` |
| 0.26 GB | 1.28% | 130 | `ehrt.sim_engine.evolve$eval9569$fn__9571.invoke` |
| 0.22 GB | 1.07% | 120 | `ehrt.sim_check.check$bed_fold$step__11574.invoke` |

#### Top-15 by CLASS

| est. alloc | share | samples | class |
|---|---|---|---|
| 3.58 GB | 17.41% | 2040 | `java.lang.Object[]` |
| 1.70 GB | 8.28% | 905 | `clojure.lang.PersistentHashMap$INode[]` |
| 1.41 GB | 6.86% | 712 | `clojure.lang.PersistentVector$ChunkedSeq` |
| 1.37 GB | 6.66% | 1506 | `byte[]` |
| 1.01 GB | 4.92% | 436 | `java.util.concurrent.locks.ReentrantLock$NonfairSync` |
| 0.98 GB | 4.75% | 582 | `clojure.lang.PersistentVector` |
| 0.79 GB | 3.86% | 449 | `clojure.lang.LazySeq` |
| 0.77 GB | 3.75% | 180 | `java.lang.StringBuilder` |
| 0.70 GB | 3.39% | 305 | `clojure.lang.ArrayChunk` |
| 0.65 GB | 3.16% | 476 | `int[]` |
| 0.49 GB | 2.40% | 518 | `java.lang.String` |
| 0.49 GB | 2.36% | 232 | `clojure.core$filter$fn__5983` |
| 0.46 GB | 2.25% | 257 | `clojure.lang.PersistentHashMap` |
| 0.44 GB | 2.14% | 317 | `clojure.lang.PersistentArrayMap` |
| 0.41 GB | 2.00% | 208 | `clojure.lang.PersistentHashMap$BitmapIndexedNode` |

