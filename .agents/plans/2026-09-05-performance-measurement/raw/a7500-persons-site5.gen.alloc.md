### `a7500-persons-site5` — gen, ALLOCATION

`a7500-persons-site5.gen.jfr`, **27876** `jdk.ObjectAllocationSample` events
(`settings=profile`'s own `300/s` throttle, stack traces on),
**54.1 GB** of estimated total allocation.

#### Top-15 by ALLOCATING PROJECT FRAME (innermost `ehrt.`)

| est. alloc | share | samples | `ehrt.` frame |
|---|---|---|---|
| 8.14 GB | 15.05% | 5485 | `(no project frame)` |
| 7.34 GB | 13.58% | 2905 | `ehrt.sim_engine.decide$eval10055$fn__10058.invoke` |
| 4.41 GB | 8.16% | 1801 | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 2.61 GB | 4.83% | 1288 | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 1.81 GB | 3.35% | 1222 | `ehrt.person_simulator.process$persons.invokeStatic` |
| 1.77 GB | 3.28% | 911 | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 1.74 GB | 3.22% | 702 | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 1.67 GB | 3.08% | 606 | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 1.57 GB | 2.90% | 1230 | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 1.55 GB | 2.87% | 691 | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 1.22 GB | 2.26% | 401 | `ehrt.patient_simulator.gmf_interpreter$parse_dob.invokeStatic` |
| 1.22 GB | 2.25% | 385 | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 0.95 GB | 1.76% | 728 | `ehrt.person_simulator.process$year_variates.invokeStatic` |
| 0.88 GB | 1.63% | 297 | `ehrt.patient_simulator.gmf_interpreter$run_module.invokeStatic` |
| 0.79 GB | 1.46% | 414 | `ehrt.sim_engine.fold$apply_events.invokeStatic` |

#### Top-15 by CLASS

| est. alloc | share | samples | class |
|---|---|---|---|
| 10.39 GB | 19.21% | 5440 | `java.lang.Object[]` |
| 3.18 GB | 5.88% | 2443 | `byte[]` |
| 2.77 GB | 5.12% | 1636 | `clojure.lang.PersistentVector$ChunkedSeq` |
| 2.50 GB | 4.62% | 1078 | `clojure.lang.LazySeq` |
| 2.30 GB | 4.25% | 1079 | `java.util.concurrent.locks.ReentrantLock$NonfairSync` |
| 2.14 GB | 3.96% | 1262 | `clojure.lang.PersistentVector` |
| 1.91 GB | 3.54% | 937 | `clojure.lang.PersistentHashMap$INode[]` |
| 1.62 GB | 3.00% | 610 | `java.util.concurrent.locks.ReentrantLock` |
| 1.57 GB | 2.91% | 606 | `clojure.lang.PersistentHashMap$ArrayNode$Seq` |
| 1.55 GB | 2.86% | 278 | `java.lang.StringBuilder` |
| 1.52 GB | 2.81% | 626 | `clojure.lang.MapEntry` |
| 1.31 GB | 2.42% | 797 | `java.lang.String` |
| 1.23 GB | 2.28% | 557 | `clojure.core$filter$fn__5983` |
| 1.23 GB | 2.27% | 604 | `int[]` |
| 0.96 GB | 1.77% | 346 | `clojure.lang.PersistentHashMap$BitmapIndexedNode` |

