(ns ehrt.sim-engine.eligible-index-test
  "ADR-0180 site 5's LAW: the from-scratch definition is the gate.

  `decide :merge` scanned the WHOLE `:patients` map per merge step to
  build `eligible`, and `decide :bed-swap` scanned it again for its own
  narrower predicate -- 34.06% and 31.38% of the generate phase at
  22,500 arrivals, and the top two allocators in it
  (`.agents/plans/2026-09-05-performance-measurement/measurements.md`).
  ADR-0180's R-fold-carrier replaces both scans with ONE sub-map
  maintained at `fold/apply-events`, `patient-id -> bed-swap-eligible?`
  over the merge-eligible patients, from which two views are read.

  WHAT IS ASSERTED, and why each part is load-bearing (ADR-0180's
  2026-09-06 addendum, written before this session rather than after
  it):

  * VECTOR IDENTITY, ORDER INCLUDED, not set equality.
    `streams/uniform-choice` resolves POSITIONALLY -- `(nth candidates
    (.nextInt rng (count candidates)))` -- so a structure answering the
    same SET in a different order picks a different patient to merge and
    moves every byte after it.

  * BOTH VIEWS, one repointed at site 5 and one at site 6. `decide
    :merge` reads the merge view, `decide :bed-swap` the swap view, and
    each was proven here against that method's own inline scan BEFORE
    the repoint that deleted the scan -- so neither repoint arrived
    carrying a view it had to prove for itself.

  * AT EVERY REPLAY ENTRY, not at the end. An index is a claim about
    every intermediate state, and an end-state-only assertion is exactly
    the assertion that passes on a carrier which is wrong for 500,000
    events and right for the last one (ADR-0180's law, point 2).

  * THE CARRIER'S CLASS, on its own, at a world of FEWER THAN NINE
    ELIGIBLE PATIENTS (R-empty-carrier, 2026-09-07).
    `PersistentArrayMap` -- what `{}` reads as -- iterates in INSERTION
    order below nine entries, where `:patients` has been a
    `PersistentHashMap` since t 0. A carrier grown from `{}` would
    therefore answer eligibility order against a parent answering hash
    order, and it would do so on the FIRST merge of every run.
    `the-carrier-is-a-hash-map-from-empty` below asserts the class and
    then demonstrates the divergence a `{}` seed actually produces,
    rather than trusting the seed to stay right.

  * THE COLLISION HOLE, DOCUMENTED AND NOT ASSERTED AWAY (R-hash-order).
    Two keys whose full 32-bit `hasheq` collides land in a
    `HashCollisionNode`, whose array is ordered by INSERTION -- so at
    such a node a sub-map filled in eligibility order can differ from a
    parent filled in registration order.
    `at-a-hasheq-collision-the-two-orders-diverge` builds the addendum's
    own measured pair and pins what each side answers. Every committed
    cell of the measured decade is collision-free, so the bracket, the
    oracle and both timed cells are BLIND to this; the test is where it
    is visible at all.

  * THE R-ALREADY-MERGED IMPLICATION, over generated logs.
    `decide :merge` carried a `some` over the ENTIRE `:ground-truth`
    asking whether `merged-id` had already been merged away, and site 5
    deletes it as provably redundant rather than indexing it. The proof
    is that the LOG-side scan and the WORLD-side `:status` agree because
    the world is the fold of the log; `the-log-scan-and-the-world-status-
    are-the-same-answer` asserts exactly that, at every world and for
    every patient, with the naive scan kept verbatim.

  AND IT IS NOT VACUOUS.
  `the-corpus-actually-reaches-every-eligible-shape` is this namespace's
  version of ADR-0169's rule that a comparison of two empty vectors is
  not an equivalence proof: it asserts that the corpus reaches non-empty
  views, reaches a merge view STRICTLY WIDER than its swap view, reaches
  eviction as well as admission, reaches carriers on both sides of the
  nine-entry array-map boundary, and reaches real `:merge` events.

  AND ONE CLAUSE IT IS VACUOUS FOR, found by this session's own negative
  control rather than reasoned about afterwards: dropping the
  `(some? (:location p))` term from the swap view's flag fails ZERO
  assertions against the generated property, because no generated log
  reaches an `:admitted` patient without a location.
  `an-admitted-patient-with-no-location-is-in-the-merge-view-only`
  constructs that world, because the index is specified to equal the
  DEFINITION on every world and the definition tests both terms.

  THE DUPLICATION IS PERMANENT AND DECLARED, exactly as ADR-0169's six
  `naive-*` invariant bodies and sites 1, 3 and 4's are
  (`rulings.md#R-move-not-improve`: the definitions are MOVED and COPIED
  here verbatim, and are not improved on the way). ALL THREE ARE MOVES
  now that site 6 has landed: `naive-merge-eligible` and
  `naive-already-merged?` became moves at site 5, `naive-swap-eligible`
  when site 6 deleted `decide :bed-swap`'s inline scan. `src` holds no
  second implementation of any of the three, and this concern meets
  R-no-second-path at both of its views."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.fold :as fold]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-engine.state :as state]
            [ehrt.sim-engine.streams :as streams]))

;; --- the from-scratch definitions, kept verbatim ---------------------------

(defn- naive-merge-eligible
  "`decide :merge`'s `eligible` binding as it stood before ADR-0180 site
  5, copied here character for character under
  `rulings.md#R-move-not-improve`, with its `never-mergeable?` helper
  inlined from three lines above it in the same `let`. A MOVE rather
  than a copy: after the repoint `src` holds no second implementation of
  this answer.

  Kept, and not called through `decide`, for the reason every `naive-*`
  in this repository is kept: a law whose reference side is the shipped
  function is a law that cannot notice the shipped function changing."
  [patients patient-id]
  (let [never-mergeable? (fn [p] (#{:new :merged} (:status p)))]
    (->> patients
         (remove (fn [[pid _]] (= pid patient-id)))
         (remove (fn [[_ p]] (never-mergeable? p)))
         (mapv first))))

(defn- naive-swap-eligible
  "`decide :bed-swap`'s `eligible` binding as it stood before ADR-0180
  site 6, copied here character for character under the same ruling --
  THE DEFINITION, KEPT AS THE GATE. It was a COPY for as long as site 5
  had repointed `decide :merge` alone; site 6's repoint deleted that
  method's inline scan and made this a MOVE, so `src` now holds no
  second implementation of this answer either.

  Kept, and not called through `decide`, for the reason every `naive-*`
  in this repository is kept: a law whose reference side is the shipped
  function is a law that cannot notice the shipped function changing."
  [patients patient-id]
  (->> patients
       (remove (fn [[pid _]] (= pid patient-id)))
       (filter (fn [[_ p]] (and (= :admitted (:status p)) (some? (:location p)))))
       (mapv first)))

(defn- naive-already-merged?
  "`decide :merge`'s `already-merged?` binding, copied here character for
  character before site 5 deleted it -- a `some` over the ENTIRE
  `:ground-truth` per merge step. It is kept because the deletion is
  justified by a PROOF rather than by a measurement, and a proof owes an
  assertion: `the-log-scan-and-the-world-status-are-the-same-answer`
  below is where the two halves of that proof are actually compared."
  [ground-truth merged-id]
  (some (fn [ev]
          (and (= :merge (:event ev))
               (some #(and (= :merged (:role %)) (= merged-id (:patient-id %)))
                     (:participants ev))))
        ground-truth))

;; --- the corpus -----------------------------------------------------------

(def ^:private eligible-facility
  "`cancel_index_test.clj`'s fixture with SIX licensed Renal beds and
  six surge slots instead of two and two, and the difference is measured
  rather than stylistic. This law needs several patients ADMITTED WITH A
  LOCATION AT ONCE -- otherwise the swap view is empty or a singleton at
  every world and the half of the law that separates the two views
  proves nothing -- and it needs the carrier to reach NINE MEMBERS,
  which is the `PersistentArrayMap` boundary R-empty-carrier is about.

  SIX AND NOT THREE BECAUSE THE GATE SAID SO. At three licensed beds the
  `:bed-cycle` half of the corpus exhausts inside 61 events and the
  index tops out at EIGHT members whatever the arrival count -- 20, 30,
  40 and 60 all measured -- so the property never once exercised the
  hash-map side of that boundary, and
  `the-corpus-actually-reaches-every-eligible-shape` below failed on its
  own fixture. At six it reaches twelve. Everything else is that
  fixture's: a surge-only ED, so beds stay contended."
  {:id :eligible-index-fixture
   :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 4
            :surge-format "%s-H%02d" :class :ed :turnaround-minutes [10 10]}
           {:id :renal :name "Renal" :beds 6 :surge-slots 6
            :surge-format "%s-H%02d" :class :inpatient :turnaround-minutes [10 10]}]})

(def ^:private stay
  {:name "stay"
   :steps [{:type :admission :location "Renal"}
           {:type :delay :from 45 :to 45}
           {:type :discharge}]})

(def ^:private churn-fraction-gen
  "Churn-BEARING logs, per ADR-0180's law, and here that means MERGE-
  and SWAP-bearing: the shipped default profile fires `:merge` and
  `:bed-swap` at 0.01 to 0.02, which over a few dozen arrivals is a
  corpus in which the two events that MOVE this index's own membership
  barely occur. Every kind is raised to a common fraction so both are
  actually reached -- `:merge` is the one event that EVICTS a member
  (`evolve :merge`'s `:merged` arm sets the absorbing status and
  `dissoc`es `:location`), and `:bed-swap` is the one that moves two
  patients under a single event."
  (gen/let [p (gen/elements [0.2 0.4 0.7])]
    (into {} (map (fn [[k _]] [k p])) churn/default-churn-profile)))

(defn- seed-patients
  "`run`'s `init-world` seed, reproduced: EVERY patient the log names,
  each `state/initial-patient` (`:status :new`, so the eligible index
  they carry is the EMPTY one), in a `PersistentHashMap`.

  THE PARENT'S CLASS IS THE POINT, not an incidental. `init-world`
  builds `:patients` with `into {}` over the whole population and that
  population is thousands, so at run scale `:patients` is a hash map
  from t 0. A test corpus is a few dozen, where `into {}` would still be
  a hash map -- but seeding from `PersistentHashMap/EMPTY` explicitly
  says that the law is about a hash-map parent and removes the
  possibility that a smaller fixture quietly changes what is being
  compared. It is also what `:patient-bootstrap` growing the map cannot
  disturb: `assoc-in` on a hash map returns a hash map.

  Growing this map through `:patient-bootstrap` instead would be the
  WRONG corpus rather than a lazier one: below nine patients the parent
  would be an array-map in insertion order, and the equality this
  namespace asserts is between two hash orders."
  [ground-truth]
  (into clojure.lang.PersistentHashMap/EMPTY
        (map (fn [pid] [pid (state/initial-patient pid pid)]))
        (sort (into #{} (comp (mapcat :participants) (keep :patient-id)) ground-truth))))

(defn- worlds
  "EVERY INTERMEDIATE WORLD, one event at a time -- `reductions`, so the
  seed world (before any event) is included and the last is the whole
  log applied.

  The projection is the concerns this question needs and no others:
  `:patient-bootstrap` (INERT here for the same reason it is inert in
  `run` -- every patient is seeded already -- and named anyway so this
  trace is the run loop's own shape), `:patient-state` folds `evolve`,
  `:log-mirror` grows the `:ground-truth` the already-merged scan reads,
  and `:eligible-index` is the concern under test.

  THE SEED CARRIES `:eligible-index` BECAUSE `run`'s `init-world` DOES,
  and it carries `fold/empty-eligible-index` rather than `{}` for the
  reason that var's own docstring gives. The concern is per-EVENT, so a
  world that has folded no events has no key at all -- the difference
  between MISSING and EMPTY, and `fold/merge-eligible` THROWS on the
  first where it answers `[]` on the second."
  [ground-truth]
  (reductions (fn [w ev]
                (:world (fold/apply-events {:world w} [ev]
                                           #{:patient-bootstrap :patient-state
                                             :log-mirror :eligible-index})))
              {:patients (seed-patients ground-truth)
               :ground-truth []
               :eligible-index fold/empty-eligible-index}
              ground-truth))

(defn- log-of
  [{:keys [seed patients churn-profile bed-cycle]}]
  (:ground-truth (run/run (cond-> {:seed seed :patients patients
                                   :arrival-gap 20
                                   :facility eligible-facility
                                   :pathway stay
                                   :churn-profile churn-profile}
                            bed-cycle (assoc :bed-cycle true)))))

(defn- probe-ids
  "The subjects the two views are asked about at one world: every
  patient in the map, plus one id no patient carries -- the exclusion of
  an absent id is a no-op for both definitions, and both views must
  agree."
  [world]
  (conj (vec (sort (keys (:patients world)))) "no-such-patient"))

(defn- disagreements
  "Every place the two sides differ at one world, as data rather than as
  assertions, so the property and the non-vacuity counts can read the
  same shape. Empty is the law holding. `=` on two vectors is ORDER-
  SENSITIVE, which is the whole obligation."
  [world]
  (let [patients (:patients world)]
    (vec (for [pid (probe-ids world)
               [view naive shipped]
               [[:merge (naive-merge-eligible patients pid) (fold/merge-eligible world pid)]
                [:bed-swap (naive-swap-eligible patients pid) (fold/swap-eligible world pid)]]
               :when (not= naive shipped)]
           [view pid naive shipped]))))

;; --- the law --------------------------------------------------------------

(defspec the-index-answers-what-the-whole-patients-scan-answers
  {:num-tests 30 :seed 20260907}
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 8 24)
                 churn-profile churn-fraction-gen
                 bed-cycle gen/boolean]
    (let [trace (worlds (log-of {:seed seed :patients patients
                                 :churn-profile churn-profile
                                 :bed-cycle bed-cycle}))]
      (and (seq trace)
           (every? (comp empty? disagreements) trace)))))

;; --- and it is not vacuous ------------------------------------------------

(deftest the-corpus-actually-reaches-every-eligible-shape
  (testing "ADR-0169's rule, applied to an index: a comparison of two
            empty vectors at every entry is not an equivalence proof.
            These are the shapes the property must actually be deciding,
            each counted over one PINNED case per bed-cycle setting."
    (doseq [bed-cycle [false true]]
      (let [profile (into {} (map (fn [[k _]] [k 0.4])) churn/default-churn-profile)
            log (log-of {:seed 20260907 :patients 20
                         :churn-profile profile :bed-cycle bed-cycle})
            trace (worlds log)
            indexes (mapv :eligible-index trace)
            widest (apply max 0 (map count indexes))
            shrinks (count (filter (fn [[a b]] (< (count b) (count a)))
                                   (partition 2 1 indexes)))
            swap-views (for [w trace pid (probe-ids w)] (fold/swap-eligible w pid))
            merge-views (for [w trace pid (probe-ids w)] (fold/merge-eligible w pid))
            both-populated (count (filter (fn [[m s]] (and (seq s) (< (count s) (count m))))
                                          (map vector merge-views swap-views)))
            merges (count (filter #(= :merge (:event %)) log))
            small (count (filter #(< (count %) 9) (filter seq indexes)))
            large (count (filter #(<= 9 (count %)) indexes))]
        (is (every? (comp empty? disagreements) trace)
            (str "bed-cycle " bed-cycle ": every world agrees, both views, every subject"))
        (is (< 1 widest)
            (str "bed-cycle " bed-cycle ": the corpus reaches an index of more than one"
                 " member -- otherwise every draw is forced and order proves nothing"))
        (is (pos? shrinks)
            (str "bed-cycle " bed-cycle ": members are EVICTED as well as admitted"
                 " -- the retirement half of the reconcile"))
        (is (pos? both-populated)
            (str "bed-cycle " bed-cycle ": the corpus reaches a world where the swap view"
                 " is non-empty and STRICTLY NARROWER than the merge view -- otherwise"
                 " one assertion is doing both views' work"))
        (is (pos? merges)
            (str "bed-cycle " bed-cycle ": the corpus reaches real :merge events, which is"
                 " what makes the already-merged implication below non-vacuous"))
        (is (pos? small)
            (str "bed-cycle " bed-cycle ": the corpus reaches a NON-EMPTY carrier below the"
                 " nine-entry array-map boundary -- the side R-empty-carrier is about"))
        (is (pos? large)
            (str "bed-cycle " bed-cycle ": and one at or above it, so both sides of that"
                 " boundary are exercised by the property"))))))

;; --- the carrier's own class ---------------------------------------------

(deftest the-carrier-is-a-hash-map-from-empty
  (testing "R-empty-carrier, 2026-09-07: the sub-map is seeded from
            `PersistentHashMap/EMPTY` and never from `{}`, because below
            nine entries a `PersistentArrayMap` iterates in INSERTION
            order while `:patients` has been a hash map since t 0. The
            class is asserted directly, and then the divergence a `{}`
            seed actually produces is demonstrated -- a seed is not the
            kind of thing to take on trust."
    (let [ids (mapv #(streams/patient-id-for 20260907 %) (range 5))
          patients (into clojure.lang.PersistentHashMap/EMPTY
                         (map (fn [pid] [pid (state/initial-patient pid pid)]))
                         ids)
          ;; the events that make each of the five a member, in the
          ;; order `ids` is written -- which is NOT the parent's own
          ;; hash order, and that is the point.
          evs (mapv (fn [pid] {:event :admission :t 0 :active-mrn pid :home-ward "Renal"
                               :location {:ward "Renal" :bed pid}
                               :participants [{:patient-id pid :role :subject}]})
                    ids)
          fold-with (fn [seed]
                      (:world (fold/apply-events
                               {:world {:patients patients :eligible-index seed}}
                               evs
                               #{:patient-state :eligible-index})))
          hashed (fold-with fold/empty-eligible-index)
          arrayed (fold-with {})
          folded (:patients hashed)]
      (is (= 5 (count ids)) "five members, four short of the array-map boundary")
      (is (instance? clojure.lang.PersistentHashMap (:eligible-index hashed))
          "the shipped seed is a hash map even at five entries")
      (is (instance? clojure.lang.PersistentArrayMap (:eligible-index arrayed))
          "-- where `{}` at the same size is an array-map, which is the hazard")
      (is (= (naive-merge-eligible folded "no-such-patient")
             (fold/merge-eligible hashed "no-such-patient"))
          "the hash-map carrier answers the parent's own filtered order")
      (is (= ids (vec (keys (:eligible-index arrayed))))
          "the array-map carrier answers INSERTION order instead -- the order the
           events arrived in, which is eligibility order and not hash order")
      (is (not= ids (naive-merge-eligible folded "no-such-patient"))
          "and those two orders really are different for this set, which is what
           makes the assertion above a divergence rather than a coincidence")
      (is (not= (naive-merge-eligible folded "no-such-patient")
                (fold/merge-eligible arrayed "no-such-patient"))
          "so a `{}` seed would move the draw on the FIRST merge of a run"))))

(deftest at-a-hasheq-collision-the-two-orders-diverge
  (testing "R-hash-order's own HOLE, documented rather than asserted
            away. Two keys whose full 32-bit `hasheq` collides land in a
            `HashCollisionNode`, whose array is ordered by INSERTION --
            so a carrier filled in eligibility order can disagree with a
            parent filled in registration order, and only there. The
            pair below is ADR-0180's addendum's own measured one, at
            arrival ordinals 32,071 and 38,357 of the shipped seed;
            every committed cell of the measured decade is collision-
            free, so the bracket, the oracle and both timed cells cannot
            see this and THIS TEST IS WHERE IT IS VISIBLE AT ALL.
            Sorting the candidates would close it and is a declared
            oracle change with its own roadmap row, not a site session's
            judgment call."
    (let [early "PID-032071-30c64e95"
          late "PID-038357-4bf55dc9"
          ;; the PARENT in registration order: early, then late.
          patients (-> clojure.lang.PersistentHashMap/EMPTY
                       (assoc early (state/initial-patient early early))
                       (assoc late (state/initial-patient late late)))
          ;; the CARRIER in eligibility order: late becomes a member first.
          ev (fn [pid] {:event :admission :t 0 :active-mrn pid :home-ward "Renal"
                        :location {:ward "Renal" :bed pid}
                        :participants [{:patient-id pid :role :subject}]})
          w (:world (fold/apply-events
                     {:world {:patients patients
                              :eligible-index fold/empty-eligible-index}}
                     [(ev late) (ev early)]
                     #{:patient-state :eligible-index}))
          folded (:patients w)]
      (is (= (hash early) (hash late))
          "the construction: these two ids really do share a 32-bit `hasheq`")
      (is (not= early late) "-- and are not the same id")
      (is (= [early late] (naive-merge-eligible folded "no-such-patient"))
          "the parent answers registration order, because that is the order its
           collision node was filled in")
      (is (= [late early] (fold/merge-eligible w "no-such-patient"))
          "and the carrier answers eligibility order, because that is the order ITS
           collision node was filled in -- DOCUMENTED, not asserted equal")
      (is (not= (naive-merge-eligible folded "no-such-patient")
                (fold/merge-eligible w "no-such-patient"))
          "so at a collision the index and the definition are two different
           functions, and the only reason the shipped corpora do not notice is
           that none of them contains a colliding pair"))))

;; --- the disjunct that was deleted rather than indexed --------------------

(deftest the-log-scan-and-the-world-status-are-the-same-answer
  (testing "R-already-merged's PROOF, asserted rather than argued.
            `already-merged?` read the LOG and `never-mergeable?` reads
            the WORLD, and they agree because the world is the fold of
            the log -- so the disjunct never decided an outcome and site
            5 deletes it rather than indexing it. Quantified at every
            world and over every patient, which is stronger than the
            `:merge` events alone: it says the two answers coincide
            BEFORE a merge as well as after one."
    (doseq [bed-cycle [false true]]
      (let [profile (into {} (map (fn [[k _]] [k 0.4])) churn/default-churn-profile)
            log (log-of {:seed 20260907 :patients 20
                         :churn-profile profile :bed-cycle bed-cycle})
            trace (worlds log)
            rows (for [w trace
                       pid (probe-ids w)
                       :let [by-log (boolean (naive-already-merged? (:ground-truth w) pid))
                             by-world (= :merged (:status (get (:patients w) pid)))]]
                   [pid by-log by-world])
            trues (count (filter second rows))]
        (is (empty? (filter (fn [[_ by-log by-world]] (not= by-log by-world)) rows))
            (str "bed-cycle " bed-cycle ": the whole-log scan and the patient's own"
                 " :status answer the same thing, at every world and for every id"))
        (is (pos? trues)
            (str "bed-cycle " bed-cycle ": and the scan is TRUE somewhere -- otherwise"
                 " this is a comparison of two constant falses"))))))

;; --- the seed, and the read that refuses to rebuild -----------------------

(deftest the-run-loop-world-carries-the-eligible-index-from-t-zero
  (testing "`:eligible-index` is the THIRD in-fold index `run` seeds
            rather than waits for, because `decide` runs BEFORE the
            batch that would open it and both views THROW on a missing
            one (R-read-throws). They must throw rather than answer `[]`:
            an empty candidate list is a legal answer that turns every
            merge into a `:step-rejected` and moves every draw after it."
    (let [empty-world {:patients {"p1" {:patient-id "p1" :status :new}}}]
      (is (= [] (naive-merge-eligible (:patients empty-world) "no-such-patient"))
          "a world of freshly seeded patients has an EMPTY eligible set, not a
           missing one -- which is what makes the empty carrier the right seed")
      (is (nil? (:eligible-index (:world (fold/apply-events {:world empty-world} []
                                                            #{:patient-state :eligible-index}))))
          "an empty batch opens NO index at all -- the concern is per-EVENT, and
           THIS is the gap `run` seeds across: the first `decide` runs before the
           first batch, so the world it reads has folded nothing")
      (is (nil? (:eligible-index (:world (fold/apply-events {:world empty-world} []
                                                            #{:patient-state}))))
          "and a projection that does not name it opens nothing either -- guarded
           by membership and by nothing else")
      (is (= fold/empty-eligible-index
             (:eligible-index (:world (fold/apply-events
                                       {:world (assoc empty-world :eligible-index
                                                      fold/empty-eligible-index)}
                                       [] #{:patient-state :eligible-index}))))
          "a seeded world keeps its seed across a batch that touches nothing")
      (is (thrown? clojure.lang.ExceptionInfo (fold/merge-eligible {} "p1"))
          "a world carrying NO index throws rather than rebuilding the scan")
      (is (thrown? clojure.lang.ExceptionInfo (fold/swap-eligible {} "p1"))
          "-- and so does the swap view `decide :bed-swap` reads (site 6)")
      (is (= [] (fold/merge-eligible {:eligible-index fold/empty-eligible-index} "p1"))
          "where the SEEDED empty index answers [], which is the scan's own answer
           over a world of freshly seeded patients"))))

(deftest the-fold-tolerates-an-absent-index-on-entry
  (testing "`update-eligible`'s `(or index empty-eligible-index)` seed,
            exercised directly: a world carrying patients but no
            `:eligible-index` key must fold to the index those patients
            have, not throw and not stay missing. This is what makes a
            hand-built world legal input to the choke point even though
            the READ refuses one."
    (let [ev {:event :admission :t 0 :active-mrn "P1" :home-ward "Renal"
              :location {:ward "Renal" :bed "RENAL-01"}
              :participants [{:patient-id "P1" :role :subject}]}
          w (:world (fold/apply-events {:world {:patients {}}} [ev]
                                       #{:patient-bootstrap :patient-state :eligible-index}))]
      (is (= {"P1" true} (:eligible-index w))
          "one member, and its value is the bed-swap view's own flag")
      (is (instance? clojure.lang.PersistentHashMap (:eligible-index w))
          "opened from `empty-eligible-index`, not from `{}`")
      (is (= (naive-merge-eligible (:patients w) "no-such-patient")
             (fold/merge-eligible w "no-such-patient")))
      (is (= (naive-swap-eligible (:patients w) "no-such-patient")
             (fold/swap-eligible w "no-such-patient"))))))

(deftest a-merged-patient-leaves-both-views-and-a-new-one-leaves-neither
  (testing "the membership predicate's two exclusions, pinned on their
            own because a generated corpus reaches them together.
            `:new` is never a member -- no `:admission` event exists yet
            for `participant-ids-exist-in-run` to find -- and `:merged`
            is the ABSORBING status `evolve :merge`'s own arm sets. Both
            are eviction paths, and they are the only two: `:merged` is
            terminal, and `evolve :cancel-admit` puts a patient back to
            `:new`. The `false` value
            is asserted as well as the `true` one, because `false` is a
            MEMBER here and only nil is an absence."
    (let [pid "P1" other "P2"
          patients (into clojure.lang.PersistentHashMap/EMPTY
                         [[pid (state/initial-patient pid pid)]
                          [other (state/initial-patient other other)]])
          fold1 (fn [w ev] (:world (fold/apply-events {:world w} [ev]
                                                      #{:patient-state :eligible-index})))
          w0 {:patients patients :eligible-index fold/empty-eligible-index}
          admitted (fold1 w0 {:event :admission :t 0 :active-mrn pid :home-ward "Renal"
                              :location {:ward "Renal" :bed "RENAL-01"}
                              :participants [{:patient-id pid :role :subject}]})
          discharged (fold1 admitted {:event :discharge :t 10 :active-mrn pid
                                      :participants [{:patient-id pid :role :subject}]})
          merged (fold1 discharged {:event :merge :t 20 :surviving-mrn other
                                    :merged-mrns #{pid}
                                    :participants [{:patient-id other :role :survivor}
                                                   {:patient-id pid :role :merged}]})]
      (is (= fold/empty-eligible-index (:eligible-index w0))
          ":new is not a member, so a world of seeded patients has an empty index")
      (is (= {pid true} (:eligible-index admitted))
          "an admitted patient with a location is in BOTH views")
      (is (= {pid false} (:eligible-index discharged))
          "a discharged one is still merge-eligible and is no longer swap-eligible
           -- `false` is a MEMBER, which is why the reconcile tests nil and not truth")
      (is (= [pid] (fold/merge-eligible discharged "no-such-patient")))
      (is (= [] (fold/swap-eligible discharged "no-such-patient")))
      (is (= fold/empty-eligible-index (:eligible-index merged))
          ":merged is absorbing and EVICTS")
      (is (= (naive-merge-eligible (:patients merged) "no-such-patient")
             (fold/merge-eligible merged "no-such-patient"))
          "and the definition agrees, which is the eviction's own law"))))

(deftest an-admitted-patient-with-no-location-is-in-the-merge-view-only
  (testing "THE ONE CLAUSE THE GENERATED CORPUS CANNOT DECIDE, pinned
            because the negative control said so rather than because it
            looked likely. `decide :bed-swap`'s predicate is `(and (=
            :admitted (:status p)) (some? (:location p)))` and this
            index's value mirrors both terms -- but a mutation that
            drops the `:location` term fails ZERO assertions against the
            property, because no generated log reaches an `:admitted`
            patient without a location: `evolve :admission`,
            `:transfer` and `:bed-swap` all write one, and `:discharge`
            clears the location and the status together.

            That makes the second term unfalsifiable by corpus, not
            unnecessary. The index is specified to equal the DEFINITION
            on every world, including hand-built ones, and the
            definition tests both terms -- so the case is constructed
            here, which is the same move `cancel_index_test`'s
            participant-filing case makes for the same reason
            (ADR-0180's law, point 2, at one remove: the corpus is not
            vacuous, but it is vacuous FOR THIS CLAUSE)."
    (let [pid "P1"
          located {:patient-id pid :mrns #{pid} :active-mrn pid :status :admitted
                   :location {:ward "Renal" :bed "RENAL-01"}}
          unlocated (dissoc located :location)
          index-of (fn [p] (fold/update-eligible fold/empty-eligible-index
                                                 {pid (assoc p :status :new)}
                                                 {pid p}
                                                 [{:patient-id pid :role :subject}]))
          w-located {:patients (into clojure.lang.PersistentHashMap/EMPTY {pid located})
                     :eligible-index (index-of located)}
          w-unlocated {:patients (into clojure.lang.PersistentHashMap/EMPTY {pid unlocated})
                       :eligible-index (index-of unlocated)}]
      (is (= {pid true} (:eligible-index w-located))
          "admitted WITH a location: in both views")
      (is (= {pid false} (:eligible-index w-unlocated))
          "admitted WITHOUT one: merge-eligible, and NOT swap-eligible -- the
           term a mutation could drop and no generated log would notice")
      (doseq [[label w p] [["located" w-located located]
                           ["unlocated" w-unlocated unlocated]]]
        (is (= (naive-merge-eligible (:patients w) "no-such-patient")
               (fold/merge-eligible w "no-such-patient"))
            (str label ": the merge view agrees with the definition"))
        (is (= (naive-swap-eligible (:patients w) "no-such-patient")
               (fold/swap-eligible w "no-such-patient"))
            (str label ": and so does the swap view (site 6)"))
        (is (= (some? (:location p))
               (= [pid] (fold/swap-eligible w "no-such-patient")))
            (str label ": -- and the swap view's answer really does turn on the"
                 " :location term, which is what makes this case decide it"))))))
