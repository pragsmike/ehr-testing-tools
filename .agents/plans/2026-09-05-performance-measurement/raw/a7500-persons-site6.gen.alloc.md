### `a7500-persons-site6` — gen, ALLOCATION

`a7500-persons-site6.gen.jfr`, **26474** `jdk.ObjectAllocationSample` events
(`settings=profile`'s own `300/s` throttle, stack traces on),
**47.0 GB** of estimated total allocation.

#### Top-15 by ALLOCATING PROJECT FRAME (innermost `ehrt.`)

| est. alloc | share | samples | `ehrt.` frame |
|---|---|---|---|
| 7.86 GB | 16.74% | 5626 | `(no project frame)` |
| 4.64 GB | 9.89% | 2144 | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 2.57 GB | 5.47% | 1373 | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 1.93 GB | 4.11% | 706 | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 1.87 GB | 3.98% | 1347 | `ehrt.person_simulator.process$persons.invokeStatic` |
| 1.85 GB | 3.94% | 989 | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 1.85 GB | 3.94% | 690 | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 1.65 GB | 3.52% | 1330 | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 1.52 GB | 3.24% | 732 | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 1.36 GB | 2.89% | 508 | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 1.17 GB | 2.48% | 316 | `ehrt.patient_simulator.gmf_interpreter$run_module.invokeStatic` |
| 0.97 GB | 2.06% | 382 | `ehrt.patient_simulator.gmf_interpreter$parse_dob.invokeStatic` |
| 0.86 GB | 1.82% | 784 | `ehrt.person_simulator.process$year_variates.invokeStatic` |
| 0.84 GB | 1.80% | 606 | `ehrt.person_simulator.persona$weighted_pick.invokeStatic` |
| 0.80 GB | 1.70% | 419 | `ehrt.sim_engine.fold$apply_events.invokeStatic` |

#### Top-15 by CLASS

| est. alloc | share | samples | class |
|---|---|---|---|
| 10.19 GB | 21.69% | 5887 | `java.lang.Object[]` |
| 3.25 GB | 6.91% | 1680 | `clojure.lang.PersistentVector$ChunkedSeq` |
| 3.21 GB | 6.83% | 2441 | `byte[]` |
| 2.62 GB | 5.58% | 1351 | `clojure.lang.PersistentVector` |
| 2.30 GB | 4.89% | 991 | `clojure.lang.PersistentHashMap$INode[]` |
| 1.51 GB | 3.21% | 702 | `java.util.concurrent.locks.ReentrantLock$NonfairSync` |
| 1.26 GB | 2.69% | 645 | `int[]` |
| 1.24 GB | 2.64% | 319 | `java.lang.StringBuilder` |
| 1.20 GB | 2.56% | 649 | `clojure.lang.LazySeq` |
| 1.13 GB | 2.41% | 847 | `java.lang.String` |
| 0.97 GB | 2.07% | 549 | `clojure.lang.PersistentArrayMap` |
| 0.94 GB | 2.00% | 514 | `clojure.lang.ArrayChunk` |
| 0.89 GB | 1.89% | 350 | `java.util.concurrent.locks.ReentrantLock` |
| 0.85 GB | 1.81% | 906 | `java.util.ArrayList$Itr` |
| 0.84 GB | 1.79% | 511 | `clojure.lang.ArraySeq` |

