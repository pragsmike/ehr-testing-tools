### `a22500-nopersons-post` — gen, ALLOCATION

`a22500-nopersons-post.gen.jfr`, **134379** `jdk.ObjectAllocationSample` events
(`settings=profile`'s own `300/s` throttle, stack traces on),
**217.4 GB** of estimated total allocation.

#### Top-15 by ALLOCATING PROJECT FRAME (innermost `ehrt.`)

| est. alloc | share | samples | `ehrt.` frame |
|---|---|---|---|
| 65.17 GB | 29.98% | 39916 | `ehrt.sim_engine.decide$eval10038$fn__10041.invoke` |
| 45.66 GB | 21.01% | 28198 | `ehrt.sim_engine.decide$eval10066$fn__10069.invoke` |
| 26.99 GB | 12.42% | 18536 | `(no project frame)` |
| 11.41 GB | 5.25% | 7203 | `ehrt.sim_model.facility$licensed_bed_ids$fn__6555.invoke` |
| 6.69 GB | 3.08% | 4249 | `ehrt.sim_engine.fold$apply_events$fn__9742.invoke` |
| 6.46 GB | 2.97% | 3849 | `ehrt.sim_engine.decide$eval10066$fn__10069$fn__10087.invoke` |
| 5.03 GB | 2.31% | 3008 | `ehrt.sim_engine.fold$apply_events$fn__9742$fn__9758.invoke` |
| 4.67 GB | 2.15% | 2468 | `ehrt.sim_engine.fold$apply_events$fn__9769$fn__9771.invoke` |
| 4.09 GB | 1.88% | 2597 | `ehrt.cli.core$sim_ground_truth_bare_text.invokeStatic` |
| 3.66 GB | 1.68% | 1583 | `ehrt.sim_engine.fold$apply_events$fn__9769.invoke` |
| 3.04 GB | 1.40% | 1289 | `ehrt.sim_model.pathway$valid_QMARK_.invokeStatic` |
| 2.41 GB | 1.11% | 1372 | `ehrt.sim_engine.fold$apply_events.invokeStatic` |
| 1.59 GB | 0.73% | 931 | `ehrt.sim_model.facility$surge_slot_ids$fn__6559.invoke` |
| 1.50 GB | 0.69% | 865 | `ehrt.sim_engine.encounters$close_encounter.invokeStatic` |
| 1.36 GB | 0.62% | 862 | `ehrt.sim_engine.decide$eval10038$fn__10041$fn__10046.invoke` |

#### Top-15 by CLASS

| est. alloc | share | samples | class |
|---|---|---|---|
| 28.15 GB | 12.95% | 17092 | `clojure.lang.PersistentVector$ChunkedSeq` |
| 20.97 GB | 9.65% | 12448 | `clojure.lang.PersistentHashMap$ArrayNode$Seq` |
| 16.74 GB | 7.70% | 9797 | `java.lang.Object[]` |
| 15.50 GB | 7.13% | 9663 | `java.util.concurrent.locks.ReentrantLock$NonfairSync` |
| 15.42 GB | 7.09% | 9400 | `clojure.lang.LazySeq` |
| 11.23 GB | 5.17% | 8987 | `byte[]` |
| 10.90 GB | 5.01% | 6647 | `clojure.lang.PersistentHashMap$NodeSeq` |
| 9.90 GB | 4.55% | 6572 | `clojure.core$filter$fn__5983` |
| 9.22 GB | 4.24% | 5602 | `clojure.lang.KeywordLookupSite$1` |
| 8.36 GB | 3.84% | 4815 | `java.util.concurrent.locks.ReentrantLock` |
| 8.32 GB | 3.83% | 5061 | `clojure.lang.MapEntry` |
| 7.82 GB | 3.60% | 4872 | `clojure.lang.Cons` |
| 5.61 GB | 2.58% | 1188 | `java.lang.StringBuilder` |
| 5.25 GB | 2.41% | 2991 | `clojure.lang.PersistentHashMap$INode[]` |
| 4.71 GB | 2.17% | 3980 | `java.util.ArrayList$Itr` |

