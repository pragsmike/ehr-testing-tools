### `a7500-persons-post` — gen, ALLOCATION

`a7500-persons-post.gen.jfr`, **43065** `jdk.ObjectAllocationSample` events
(`settings=profile`'s own `300/s` throttle, stack traces on),
**61.4 GB** of estimated total allocation.

#### Top-15 by ALLOCATING PROJECT FRAME (innermost `ehrt.`)

| est. alloc | share | samples | `ehrt.` frame |
|---|---|---|---|
| 10.09 GB | 16.45% | 9821 | `(no project frame)` |
| 7.11 GB | 11.59% | 4741 | `ehrt.sim_engine.decide$eval10038$fn__10041.invoke` |
| 4.27 GB | 6.96% | 2938 | `ehrt.sim_engine.decide$eval10066$fn__10069.invoke` |
| 4.24 GB | 6.91% | 2676 | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 2.81 GB | 4.57% | 1452 | `ehrt.sim_engine.fold$apply_events$fn__9742.invoke` |
| 1.99 GB | 3.24% | 955 | `ehrt.sim_engine.fold$apply_events$fn__9742$fn__9758.invoke` |
| 1.97 GB | 3.20% | 1664 | `ehrt.person_simulator.process$persons.invokeStatic` |
| 1.88 GB | 3.06% | 1164 | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 1.73 GB | 2.82% | 903 | `ehrt.sim_engine.fold$apply_events$fn__9769$fn__9771.invoke` |
| 1.60 GB | 2.61% | 1526 | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 1.55 GB | 2.53% | 749 | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 1.31 GB | 2.13% | 592 | `ehrt.sim_engine.fold$apply_events$fn__9769.invoke` |
| 0.99 GB | 1.61% | 982 | `ehrt.person_simulator.process$year_variates.invokeStatic` |
| 0.94 GB | 1.53% | 561 | `ehrt.patient_simulator.gmf_interpreter$parse_dob.invokeStatic` |
| 0.86 GB | 1.40% | 407 | `ehrt.patient_simulator.gmf_interpreter$run_module.invokeStatic` |

#### Top-15 by CLASS

| est. alloc | share | samples | class |
|---|---|---|---|
| 10.27 GB | 16.74% | 7154 | `java.lang.Object[]` |
| 5.09 GB | 8.30% | 3357 | `clojure.lang.PersistentVector$ChunkedSeq` |
| 4.22 GB | 6.88% | 4224 | `byte[]` |
| 2.69 GB | 4.39% | 1774 | `clojure.lang.PersistentVector` |
| 2.60 GB | 4.24% | 1719 | `clojure.lang.LazySeq` |
| 2.56 GB | 4.17% | 1786 | `java.util.concurrent.locks.ReentrantLock$NonfairSync` |
| 1.77 GB | 2.88% | 1059 | `clojure.lang.PersistentHashMap$INode[]` |
| 1.76 GB | 2.87% | 1287 | `clojure.lang.PersistentHashMap$ArrayNode$Seq` |
| 1.55 GB | 2.53% | 915 | `java.util.concurrent.locks.ReentrantLock` |
| 1.51 GB | 2.46% | 1860 | `java.util.ArrayList$Itr` |
| 1.44 GB | 2.35% | 981 | `clojure.lang.MapEntry` |
| 1.43 GB | 2.33% | 507 | `java.lang.StringBuilder` |
| 1.37 GB | 2.22% | 828 | `clojure.lang.PersistentHashMap$NodeSeq` |
| 1.34 GB | 2.19% | 859 | `int[]` |
| 1.33 GB | 2.17% | 1057 | `clojure.core$filter$fn__5983` |

