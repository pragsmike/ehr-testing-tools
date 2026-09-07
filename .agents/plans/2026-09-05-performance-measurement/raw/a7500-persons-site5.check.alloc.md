### `a7500-persons-site5` — check, ALLOCATION

`a7500-persons-site5.check.jfr`, **13707** `jdk.ObjectAllocationSample` events
(`settings=profile`'s own `300/s` throttle, stack traces on),
**20.7 GB** of estimated total allocation.

#### Top-15 by ALLOCATING PROJECT FRAME (innermost `ehrt.`)

| est. alloc | share | samples | `ehrt.` frame |
|---|---|---|---|
| 3.38 GB | 16.36% | 3291 | `(no project frame)` |
| 2.34 GB | 11.34% | 1806 | `ehrt.cli.core$read_ground_truth_stdin$fn__14524.invoke` |
| 2.30 GB | 11.13% | 1305 | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 1.60 GB | 7.73% | 952 | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 1.55 GB | 7.53% | 810 | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 1.38 GB | 6.67% | 538 | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 0.53 GB | 2.55% | 310 | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 0.48 GB | 2.34% | 273 | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 0.46 GB | 2.25% | 279 | `ehrt.sim_check.check$bed_fold.invokeStatic` |
| 0.46 GB | 2.22% | 249 | `ehrt.sim_check.check$participants_of.invokeStatic` |
| 0.33 GB | 1.57% | 210 | `ehrt.sim_engine.encounters$stamp_encounter.invokeStatic` |
| 0.29 GB | 1.40% | 199 | `ehrt.sim_engine.evolve$eval9506$fn__9508.invoke` |
| 0.25 GB | 1.19% | 193 | `ehrt.sim_check.check$events_by_patient$fn__11057$fn__11059.invoke` |
| 0.20 GB | 0.95% | 64 | `ehrt.sim_engine.evolve$eval9672$fn__9674.invoke` |
| 0.20 GB | 0.95% | 64 | `ehrt.sim_check.check$bed_fold$fn__11579.invoke` |

#### Top-15 by CLASS

| est. alloc | share | samples | class |
|---|---|---|---|
| 3.24 GB | 15.70% | 2073 | `java.lang.Object[]` |
| 1.70 GB | 8.24% | 1587 | `byte[]` |
| 1.57 GB | 7.60% | 981 | `clojure.lang.PersistentHashMap$INode[]` |
| 1.38 GB | 6.69% | 784 | `clojure.lang.PersistentVector$ChunkedSeq` |
| 0.97 GB | 4.71% | 551 | `clojure.lang.PersistentVector` |
| 0.93 GB | 4.48% | 500 | `clojure.lang.LazySeq` |
| 0.62 GB | 3.02% | 592 | `java.lang.String` |
| 0.58 GB | 2.82% | 180 | `java.lang.StringBuilder` |
| 0.57 GB | 2.77% | 439 | `java.util.concurrent.locks.ReentrantLock$NonfairSync` |
| 0.57 GB | 2.77% | 494 | `int[]` |
| 0.56 GB | 2.71% | 304 | `clojure.lang.PersistentArrayMap` |
| 0.53 GB | 2.57% | 311 | `clojure.lang.ArrayChunk` |
| 0.53 GB | 2.55% | 238 | `java.util.concurrent.locks.ReentrantLock` |
| 0.52 GB | 2.52% | 271 | `clojure.lang.PersistentHashMap` |
| 0.49 GB | 2.37% | 219 | `clojure.core$filter$fn__5983` |

