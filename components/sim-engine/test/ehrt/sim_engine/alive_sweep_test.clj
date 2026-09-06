(ns ehrt.sim-engine.alive-sweep-test
  "ADR-0180 site 2's LAW: the from-scratch definition is the gate.

  `run/select-person` re-filtered the WHOLE `:population` once per
  arrival, dropping anyone whose death instant had passed, and
  `:persons :count` is twice the arrival count by rule -- so the site was
  O(arrivals^2) by construction and 19.98% of top-of-decade generate
  (`.agents/plans/2026-09-05-performance-measurement/measurements.md`).
  ADR-0180 section 2 replaces the filter with a rank/select sweep and
  keeps the filter HERE, verbatim, as the reference it is proven equal
  to.

  WHAT IS ASSERTED, and why each half is load-bearing (ADR-0180 site 2's
  equivalence argument, written before this session ran):

  * THE WHOLE CANDIDATE VECTOR, not its count. `select-person`'s one
    uniform resolves POSITIONALLY -- `(nth candidates (min ...))` -- so
    ORDER is half the function. A structure answering the right count in
    a different order rebinds arrivals and moves every byte after them.
  * THE SELECTED ID, through the draw. Two scripted `Random`s replay the
    SAME uniform sequence to both sides, so the property compares the two
    candidate vectors as the site itself consumes them, not just
    directly.
  * AT EVERY ARRIVAL, not at the end. An index is a claim about every
    intermediate state, and an end-state-only assertion is exactly the
    one that passes on a carrier which is wrong for 500,000 arrivals and
    right for the last (ADR-0180's law, point 2).
  * THE HALF-OPEN BOUNDARY. A person whose death instant IS the arrival
    instant is dead at it. The generator draws deaths and arrivals from
    the SAME small range so that coincidence is common rather than rare,
    and `the-corpus-actually-reaches-every-shape` counts it.
  * DRAWS NEAR 1.0. `Math/nextDown 1.0` is the largest uniform
    `.nextDouble` can return; it is the input under which the positional
    `nth` reads closest to the end of the candidate vector.

  AND IT IS NOT VACUOUS. `the-corpus-actually-reaches-every-shape` is
  this namespace's version of ADR-0169's rule that comparing two empty
  seqs proves nothing: it asserts the pinned corpus reaches an arrival
  where the filter removed SOME people, one where it removed EVERYONE
  (the nil binding), an arrival AT a death instant, and a selection that
  lands on the LAST candidate.

  THE DUPLICATION IS PERMANENT AND DECLARED, exactly as ADR-0169's six
  `naive-*` invariant bodies are (`rulings.md#R-move-not-improve`: the
  filter MOVES here, verbatim, and is not improved on the way)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-engine.run :as run])
  (:import [java.util Random]))

;; --- the from-scratch definition, kept verbatim ---------------------------

(defn- naive-candidates
  "`ehrt.sim-engine.run/select-person`'s own `filterv`, moved here
  character for character under `rulings.md#R-move-not-improve`. Every
  term of the predicate is the shipped filter's: the `get` against
  `alive`, the `nil?` arm that makes an absent person immortal, and the
  strict `>` that makes the death instant itself fatal."
  [population alive t]
  (filterv (fn [{:keys [person-id]}]
             (let [d (get alive person-id)]
               (or (nil? d) (> d t))))
           population))

(defn- naive-select
  "The rest of `select-person`'s shipped body, equally verbatim: one
  uniform, taken whether or not the filter removed anyone, then the
  positional `nth` under its `min` clamp."
  [^Random rng population alive t]
  (let [candidates (naive-candidates population alive t)
        draw (.nextDouble rng)]
    (when (seq candidates)
      (:person-id (nth candidates
                       (min (dec (count candidates))
                            (long (* draw (count candidates)))))))))

(defn- sweep-select
  "THE SAME BODY with its one filter replaced by the sweep's two answers
  and NOTHING else touched -- the draw in the same place and taken
  unconditionally, the `min` clamp character for character, the
  positional read where the `nth` was.

  It stands here rather than in `run.clj` because ADR-0180 site 2 lands
  the sweep and its law BEFORE the site reads it: this is what
  `select-person` becomes at the repoint, provable one commit early."
  [^Random rng sweep t]
  (let [candidates (run/sweep-advance! sweep t)
        draw (.nextDouble rng)]
    (when (pos? candidates)
      (run/sweep-nth sweep (min (dec candidates) (long (* draw candidates)))))))

;; --- the corpus -----------------------------------------------------------

(defn- scripted-rng
  "A `Random` replaying a FIXED uniform sequence, so both sides of the
  property see the same draw at the same arrival, and so the sequence can
  contain `Math/nextDown 1.0` -- which no seed reaches on demand. `proxy`
  because `select-person`'s parameter is type-hinted `^Random`."
  ^Random [draws]
  (let [i (atom -1)]
    (proxy [Random] []
      (nextDouble [] (nth draws (min (swap! i inc) (dec (count draws))))))))

(def ^:private draw-gen
  "Uniforms spanning the unit interval, weighted at the top end: the
  positional `nth` is only ever near the end of the candidate vector when
  the draw is near 1.0, and `Math/nextDown 1.0` is the largest value
  `.nextDouble` can return at all."
  (gen/elements [0.0 1.0E-12 0.25 0.5 0.75 0.9 0.999999 0.99999999999
                 (Math/nextDown 1.0)]))

(def ^:private case-gen
  "One case: a small population, a death instant for some of them, an
  ASCENDING arrival vector, and one uniform per arrival.

  The instants come from the SAME small range as the arrivals, and that
  is the point rather than laziness: it makes ties in death instant and
  deaths landing exactly ON an arrival instant common events instead of
  ones a wide range would visit once in a thousand cases. `sort` gives
  the t-ascending vector `prelude` builds with `reductions +` over
  non-negative gaps -- repeats included, since a zero gap is legal."
  (gen/let [n (gen/choose 1 12)
            deaths (gen/vector (gen/one-of [(gen/return nil) (gen/choose 0 6)]) n)
            ts (gen/not-empty (gen/vector (gen/choose 0 7) 1 24))
            draws (gen/vector draw-gen (count ts))]
    {:population (mapv (fn [i] {:person-id (str "p-" i) :id-tag (inc i)}) (range n))
     :alive (into {} (keep-indexed (fn [i d] (when d [(str "p-" i) d])) deaths))
     :arrivals (vec (sort ts))
     :draws draws}))

(defn- trace
  "Both sides, ARRIVAL BY ARRIVAL. The two scripted `Random`s are built
  from one `draws` vector, so at every arrival the naive body and the
  sweep body consume the same uniform.

  The sweep's alive vector is read AFTER `sweep-select` has already
  advanced the cursor to `t`; `sweep-advance!` at the same `t` is a no-op
  returning the same count, which is the property that makes reading it
  here legal rather than a second traversal.

  Each entry carries the case's own population size and death instants so
  the non-vacuity counts below can be taken over a flat sequence of
  entries from cases of different shapes."
  [{:keys [population alive arrivals draws]}]
  (let [sweep (run/alive-sweep population alive)
        naive-rng (scripted-rng draws)
        sweep-rng (scripted-rng draws)]
    (mapv (fn [t]
            (let [naive (naive-candidates population alive t)
                  naive-id (naive-select naive-rng population alive t)
                  swept-id (sweep-select sweep-rng sweep t)
                  live (run/sweep-advance! sweep t)]
              {:t t
               :n (count population)
               :death-instants (set (vals alive))
               :naive-ids (mapv :person-id naive)
               :sweep-ids (mapv #(run/sweep-nth sweep %) (range live))
               :naive-id naive-id
               :swept-id swept-id}))
          arrivals)))

(defn- agree?
  [{:keys [naive-ids sweep-ids naive-id swept-id]}]
  (and (= naive-ids sweep-ids) (= naive-id swept-id)))

;; --- the law --------------------------------------------------------------

(defspec the-sweep-answers-exactly-what-the-filter-does
  {:num-tests 300 :seed 20260906}
  (prop/for-all [c case-gen]
    (let [entries (trace c)]
      (and (seq entries) (every? agree? entries)))))

;; --- and it is not vacuous ------------------------------------------------

(def ^:private pinned-cases
  "Two hand-built cases, so the counts below are stable figures rather
  than whatever the generator happened to reach.

  CASE 1 keeps `p-0` immortal, so the candidate set never empties and the
  selection is always a real id. Its deaths TIE at 2, and 0/2/4/6 are all
  arrival instants, so the half-open boundary is struck four times.

  CASE 2 gives EVERY person a death inside the arrival range, so the last
  arrivals find nobody alive -- the nil binding, the one answer shape a
  count-only index could get right while getting the id wrong."
  [{:population (mapv (fn [i] {:person-id (str "p-" i) :id-tag (inc i)}) (range 6))
    :alive {"p-1" 2 "p-2" 2 "p-3" 4 "p-4" 0 "p-5" 6}
    :arrivals [0 1 2 2 3 4 5 6 6 7]
    :draws [0.0 (Math/nextDown 1.0) 0.5 0.99999999999 0.25
            (Math/nextDown 1.0) 0.75 1.0E-12 0.9 (Math/nextDown 1.0)]}
   {:population (mapv (fn [i] {:person-id (str "q-" i) :id-tag (inc i)}) (range 4))
    :alive {"q-0" 1 "q-1" 1 "q-2" 3 "q-3" 5}
    :arrivals [0 1 2 3 4 5 6]
    :draws [(Math/nextDown 1.0) 0.5 0.0 0.9 (Math/nextDown 1.0) 0.25 0.75]}])

(deftest the-corpus-actually-reaches-every-shape
  (testing "ADR-0169's rule, applied to a rank/select index: two equal
            empty vectors compared at every arrival is not an equivalence
            proof. These are the shapes the property must actually be
            deciding, counted over the two pinned cases."
    (let [entries (vec (mapcat trace pinned-cases))
          removed-some (count (filter (fn [{:keys [naive-ids n]}]
                                        (< 0 (count naive-ids) n))
                                      entries))
          removed-all (count (filter (fn [{:keys [naive-ids]}] (empty? naive-ids)) entries))
          at-instant (count (filter (fn [{:keys [t death-instants]}]
                                      (contains? death-instants t))
                                    entries))
          last-candidate (count (filter (fn [{:keys [naive-ids naive-id]}]
                                          (and (< 1 (count naive-ids))
                                               (= naive-id (last naive-ids))))
                                        entries))]
      (is (every? agree? entries) "every pinned arrival's two answers agree")
      (is (pos? removed-some)
          "the corpus reaches an arrival where the filter removed SOME people")
      (is (pos? removed-all)
          "the corpus reaches an arrival where the filter removed EVERYONE --
           the nil binding, which a count-only index could get right while
           getting the id wrong")
      (is (pos? at-instant)
          "the corpus reaches an arrival AT a death instant -- the half-open
           boundary, where a `>=` carrier and a `>` carrier disagree")
      (is (pos? last-candidate)
          "the corpus reaches a selection landing on the LAST candidate --
           the end of the vector the positional `nth` walks to"))))

(deftest ties-in-death-instant-are-evicted-together
  (testing "two people sharing a death instant leave the candidate set at
            the SAME arrival, and their relative order in the sweep's own
            death vector is unobservable -- the fact that lets
            `alive-sweep` sort by instant alone."
    (let [population (mapv (fn [i] {:person-id (str "p-" i)}) (range 4))
          sweep (run/alive-sweep population {"p-1" 3 "p-2" 3})]
      (is (= 4 (run/sweep-advance! sweep 2)))
      (is (= ["p-0" "p-1" "p-2" "p-3"] (mapv #(run/sweep-nth sweep %) (range 4))))
      (is (= 2 (run/sweep-advance! sweep 3)) "the tie did not evict as one unit")
      (is (= ["p-0" "p-3"] (mapv #(run/sweep-nth sweep %) (range 2)))
          "eviction reordered the survivors -- the positional read is now wrong"))))

(deftest an-empty-population-answers-zero-rather-than-throwing
  (testing "`select-person`'s FIXED CONSUMPTION law reaches the sweep: an
            empty pool takes its draw and binds nobody, so the structure
            has to answer a count of zero at every arrival rather than
            refuse to be built. `persons_test`'s own
            `the-selection-draw-is-taken-whether-or-not-anyone-is-eligible`
            is the behavioural half of this."
    (let [sweep (run/alive-sweep [] {})]
      (is (zero? (run/sweep-advance! sweep 0)))
      (is (zero? (run/sweep-advance! sweep 99999)))
      (is (nil? (sweep-select (scripted-rng [0.5]) sweep 42))))))

(deftest a-person-absent-from-alive-never-dies
  (testing "the `nil?` arm of the shipped predicate, which is not the same
            claim as a death instant in the future: `alive` carries only
            the people whose own death the stream produced."
    (let [population (mapv (fn [i] {:person-id (str "p-" i)}) (range 3))
          sweep (run/alive-sweep population {"p-1" 5})]
      (is (= 3 (run/sweep-advance! sweep 4)))
      (is (= 2 (run/sweep-advance! sweep 5)))
      (is (= ["p-0" "p-2"] (mapv #(run/sweep-nth sweep %) (range 2))))
      (is (= 2 (run/sweep-advance! sweep Long/MAX_VALUE))
          "somebody with no death instant was evicted anyway"))))
