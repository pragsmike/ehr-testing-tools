### `a7500-persons-before6` — gen, ALLOCATION

`a7500-persons-before6.gen.jfr`, **29330** `jdk.ObjectAllocationSample` events
(`settings=profile`'s own `300/s` throttle, stack traces on),
**54.7 GB** of estimated total allocation.

#### Top-15 by ALLOCATING PROJECT FRAME (innermost `ehrt.`)

| est. alloc | share | samples | `ehrt.` frame |
|---|---|---|---|
| 7.78 GB | 14.23% | 5323 | `(no project frame)` |
| 6.98 GB | 12.76% | 3176 | `ehrt.sim_engine.decide$eval10055$fn__10058.invoke` |
| 4.79 GB | 8.76% | 2163 | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 2.50 GB | 4.58% | 805 | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 2.37 GB | 4.34% | 1277 | `ehrt.sim_engine.fold$apply_events$fn__9759.invoke` |
| 2.15 GB | 3.93% | 934 | `ehrt.sim_engine.fold$apply_events$fn__9759$fn__9775.invoke` |
| 1.91 GB | 3.50% | 1319 | `ehrt.person_simulator.process$persons.invokeStatic` |
| 1.62 GB | 2.96% | 1331 | `ehrt.person_simulator.process$walk_person.invokeStatic` |
| 1.54 GB | 2.82% | 812 | `ehrt.sim_engine.fold$apply_events$fn__9786$fn__9788.invoke` |
| 1.49 GB | 2.72% | 775 | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 1.36 GB | 2.49% | 567 | `ehrt.sim_engine.fold$apply_events$fn__9786.invoke` |
| 1.15 GB | 2.11% | 360 | `ehrt.patient_simulator.gmf_interpreter$parse_dob.invokeStatic` |
| 0.98 GB | 1.79% | 448 | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 0.94 GB | 1.73% | 786 | `ehrt.person_simulator.process$year_variates.invokeStatic` |
| 0.78 GB | 1.43% | 319 | `ehrt.patient_simulator.gmf_interpreter$run_module.invokeStatic` |

#### Top-15 by CLASS

| est. alloc | share | samples | class |
|---|---|---|---|
| 10.42 GB | 19.06% | 5823 | `java.lang.Object[]` |
| 3.45 GB | 6.31% | 2338 | `byte[]` |
| 3.02 GB | 5.52% | 1734 | `clojure.lang.PersistentVector$ChunkedSeq` |
| 2.69 GB | 4.91% | 1070 | `clojure.lang.LazySeq` |
| 2.61 GB | 4.78% | 1417 | `clojure.lang.PersistentVector` |
| 2.27 GB | 4.16% | 1158 | `java.util.concurrent.locks.ReentrantLock$NonfairSync` |
| 2.18 GB | 3.99% | 1082 | `clojure.lang.PersistentHashMap$INode[]` |
| 1.65 GB | 3.02% | 325 | `java.lang.StringBuilder` |
| 1.54 GB | 2.82% | 639 | `clojure.lang.PersistentHashMap$ArrayNode$Seq` |
| 1.24 GB | 2.26% | 650 | `int[]` |
| 1.17 GB | 2.14% | 567 | `java.util.concurrent.locks.ReentrantLock` |
| 1.10 GB | 2.01% | 593 | `clojure.core$filter$fn__5983` |
| 1.08 GB | 1.97% | 771 | `java.lang.String` |
| 1.01 GB | 1.84% | 491 | `clojure.lang.ArraySeq` |
| 0.98 GB | 1.80% | 684 | `clojure.lang.MapEntry` |

