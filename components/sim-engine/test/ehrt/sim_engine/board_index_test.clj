(ns ehrt.sim-engine.board-index-test
  "ADR-0180 site 3's LAW: the from-scratch definition is the gate.

  `sim-model/occupancy-board` is an `into {}` over the WHOLE `:patients`
  map, and `run`'s `init-world` seeds that map with every patient at
  t 0 -- so P is the whole population from the first event, and at the
  top of the measured decade the board was 17.11% of the generate phase
  (`.agents/plans/2026-09-05-performance-measurement/measurements.md`).
  ADR-0180's R-fold-carrier replaces the five shipped recomputations
  with an index maintained at `fold/apply-events` -- `bed-id ->
  patient-id` -- and keeps the definition where it has always been,
  public in `sim-model`, as the reference the index is proven equal to.
  THE BOARD IS STILL A DEFINITION (`components/sim-model/docs/
  charter.md` section 4); what this namespace adds is the proof that a
  cache of it answers the same.

  WHAT IS ASSERTED, and why each half is load-bearing (ADR-0180 site 3's
  equivalence argument, written before this session rather than after
  it):

  * THE WHOLE MAP, `=`, not just its key set. The allocator needs the
    KEY SET -- `sim-model/free` uses the board as a PREDICATE over a
    derived, deterministically ordered id list, so the board's own
    iteration order never reaches `choose`'s draw -- but
    `log-index/bed-reoccupied-by-someone-else?` reads a VALUE as an
    occupant id, so value equality is separately owed and asserting the
    whole map is what owes it once.

  * AT EVERY REPLAY ENTRY, not at the end. An index is a claim about
    every intermediate state, and an end-state-only assertion is exactly
    the assertion that passes on a carrier which is wrong for 500,000
    events and right for the last one (ADR-0180's law, point 2).

  * THE SECOND QUERY SHAPE, on its own. `decide`'s `bed-ready-location`
    asks `(occupancy-board (dissoc (:patients world) patient-id))` --
    the board with the subject removed -- and ADR-0180 names that as
    where an index most easily diverges from its definition. It is
    answered by `fold/board-without`, a MASK over the one index, and the
    property drives it for every patient at every world rather than
    waiting for the shapes to occur.

  * DOUBLE OCCUPANCY, named rather than assumed away
    (ADR-0180's R-double-occupancy). `into {}` keeps whichever of two
    patients sharing a bed comes last in the `:patients` map's own SEQ
    order; the index keeps whichever wrote the bed last in EVENT order.
    `ehrt.sim-check.check`'s `no-double-occupancy` convicts any log that
    reaches that state, so the index is specified on no-double-occupancy
    worlds and the equality property above quantifies over generated
    logs. `the-double-occupancy-divergence-is-what-the-ruling-says-it-is`
    below pins what the index actually answers there, so the divergence
    is a recorded fact rather than a surprise.

  AND IT IS NOT VACUOUS. `the-corpus-actually-reaches-every-board-shape`
  is this namespace's version of ADR-0169's rule that a comparison of
  two empty maps is not an equivalence proof: it asserts that the
  generated corpus reaches non-empty boards, reaches beds that are
  vacated as well as filled, reaches events that move two patients'
  beds at once, and reaches masked queries that actually remove a key.

  THE DUPLICATION IS PERMANENT AND DECLARED, exactly as ADR-0169's six
  `naive-*` invariant bodies and site 1's `naive-waiting-boarder` are
  (`rulings.md#R-move-not-improve`: the definition is COPIED here
  verbatim, and is not improved on the way)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.fold :as fold]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-model.interface :as sim-model]))

;; --- the from-scratch definition, kept verbatim ---------------------------

(defn- naive-board
  "`ehrt.sim-model.facility/occupancy-board`'s BODY, copied here
  character for character under `rulings.md#R-move-not-improve`. Unlike
  site 1's `naive-waiting-boarder`, this one is a COPY rather than a
  move: the definition does not retire, because
  `ehrt.sim-check.check`'s `surge-only-when-earlier-rungs-exhausted`
  still calls it over a replay entry's `:world-before` -- a site the
  index deliberately does not reach, `replay` not carrying `:board`.

  Kept anyway, and not called through `sim-model`, for the reason every
  `naive-*` in this repository is kept: a law whose reference side is
  the shipped function is a law that cannot notice the shipped function
  changing."
  [patients]
  (into {}
        (keep (fn [[patient-id patient]]
                (when-let [bed (get-in patient [:location :bed])]
                  [bed patient-id])))
        patients))

;; --- the corpus -----------------------------------------------------------

(def ^:private boarding-facility
  "ONE licensed Renal bed and one Renal surge slot, with an ED that has
  no licensed beds -- `boarder_index_test.clj`'s own fixture, and chosen
  for the same reason at one remove: at this capacity every bed in the
  facility is contended within a handful of arrivals, so the board is
  full, churned and rebuilt rather than mostly empty."
  {:id :board-index-fixture
   :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 4
            :surge-format "%s-H%02d" :class :ed :turnaround-minutes [10 10]}
           {:id :renal :name "Renal" :beds 1 :surge-slots 1
            :surge-format "%s-H%02d" :class :inpatient :turnaround-minutes [10 10]}]})

(def ^:private stay
  {:name "stay"
   :steps [{:type :admission :location "Renal"}
           {:type :delay :from 45 :to 45}
           {:type :discharge}]})

(def ^:private churn-fraction-gen
  "Churn-BEARING logs, per ADR-0180's law. `:bed-swap` is the reason
  this matters more here than at site 1: it is the ONE churn kind whose
  event moves TWO patients' beds at once, which is exactly the input
  `update-board`'s value guard exists for -- one participant files its
  new bed over a key the other still holds, and the pair must still land
  where a from-scratch scan lands. `:merge` and the three cancels supply
  the rest of the enter/leave paths an add-remove-per-event-kind index
  would have had to enumerate."
  (gen/let [p (gen/elements [0.2 0.4 0.7])]
    (into {} (map (fn [[k _]] [k p])) churn/default-churn-profile)))

(defn- worlds
  "EVERY INTERMEDIATE WORLD, one event at a time -- `reductions`, so the
  seed world (before any event) is included and the last is the whole
  log applied.

  The projection is the three per-event concerns this question needs and
  no others: `:patient-bootstrap` puts each event's participants in the
  map, `:patient-state` folds `evolve`, `:board` maintains the index.
  All three are per-EVENT, so folding one event at a time is the same
  computation the run loop performs per batch.

  THE SEED CARRIES `:board {}` BECAUSE `run`'s `init-world` DOES, and
  that is a claim rather than a convenience. The concern is per-EVENT,
  so a world that has folded no events has no `:board` key at all --
  which is the difference between MISSING and EMPTY, and the reason
  `run` seeds the empty one (`sim-model/free` calls the board as a
  predicate and would throw on a missing one). Seeding the trace the
  same way is what makes the entry BEFORE the first event a real entry
  of the law rather than an artefact of this helper;
  `the-run-loop-world-carries-the-board-from-t-zero` below pins both
  halves of the distinction directly."
  [ground-truth]
  (reductions (fn [w ev]
                (:world (fold/apply-events {:world w} [ev]
                                           #{:patient-bootstrap :patient-state
                                             :board})))
              {:patients {} :board {}}
              ground-truth))

(defn- log-of
  [{:keys [seed patients churn-profile bed-cycle]}]
  (:ground-truth (run/run (cond-> {:seed seed :patients patients
                                   :arrival-gap 20
                                   :facility boarding-facility
                                   :pathway stay
                                   :churn-profile churn-profile}
                            bed-cycle (assoc :bed-cycle true)))))

(defn- probe-ids
  "The subjects the masked query is asked about at one world: every
  patient in the map, plus one id no patient carries -- `dissoc` of an
  absent key is a no-op for the definition, and the mask must agree."
  [world]
  (conj (vec (sort (keys (:patients world)))) "no-such-patient"))

(defn- disagreements
  "Every place the two sides differ at one world, as data rather than as
  assertions, so the property and the non-vacuity count can read the
  same shape. Empty is the law holding."
  [world]
  (let [patients (:patients world)]
    (cond-> []
      (not= (naive-board patients) (:board world))
      (conj [:whole-board (naive-board patients) (:board world)])

      :always
      (into (keep (fn [pid]
                    (let [expected (naive-board (dissoc patients pid))
                          actual (fold/board-without world pid)]
                      (when (not= expected actual)
                        [:masked pid expected actual])))
                  (probe-ids world))))))

;; --- the law --------------------------------------------------------------

(defspec the-index-is-the-board-and-its-mask-is-the-dissoc
  {:num-tests 40 :seed 20260906}
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

(deftest the-corpus-actually-reaches-every-board-shape
  (testing "ADR-0169's rule, applied to an index: a comparison of two
            empty maps at every entry is not an equivalence proof. These
            are the shapes the property must actually be deciding, each
            counted over one PINNED case per bed-cycle setting."
    (doseq [bed-cycle [false true]]
      (let [profile (into {} (map (fn [[k _]] [k 0.4])) churn/default-churn-profile)
            log (log-of {:seed 20260906 :patients 20
                         :churn-profile profile :bed-cycle bed-cycle})
            trace (worlds log)
            boards (mapv :board trace)
            occupied (count (filter seq boards))
            biggest (apply max 0 (map count boards))
            shrinks (count (filter (fn [[a b]] (< (count b) (count a)))
                                   (partition 2 1 boards)))
            two-movers (count (filter (fn [ev]
                                        (< 1 (count (filter :patient-id (:participants ev)))))
                                      log))
            masked-removes (count (for [w trace
                                        pid (probe-ids w)
                                        :when (not= (:board w) (fold/board-without w pid))]
                                    pid))]
        (is (every? (comp empty? disagreements) trace)
            (str "bed-cycle " bed-cycle ": every world agrees, whole board and mask"))
        (is (pos? occupied)
            (str "bed-cycle " bed-cycle ": the corpus reaches a NON-EMPTY board"))
        (is (< 1 biggest)
            (str "bed-cycle " bed-cycle ": and one holding more than a single bed"))
        (is (pos? shrinks)
            (str "bed-cycle " bed-cycle ": beds are VACATED as well as filled"
                 " -- the retirement half of the reconcile"))
        (is (pos? two-movers)
            (str "bed-cycle " bed-cycle ": the corpus reaches events with TWO patient"
                 " participants -- the input `update-board`'s value guard exists for"))
        (is (pos? masked-removes)
            (str "bed-cycle " bed-cycle ": the masked query actually REMOVES a key"
                 " somewhere -- otherwise it is the identity and proves nothing"))))))

(deftest the-double-occupancy-divergence-is-what-the-ruling-says-it-is
  (testing "ADR-0180's R-double-occupancy: the index is specified on
            no-double-occupancy worlds, and the equality property above
            quantifies over generated logs. On an input
            `ehrt.sim-check.check`'s `no-double-occupancy` would convict,
            the index answers LAST-WRITER-BY-EVENT-ORDER where the
            definition answers last-in-seq-order. Pinned here so the
            divergence is a recorded fact rather than a surprise a later
            reader has to rediscover."
    (let [bed "RENAL-01"
          admit (fn [pid t] {:event :admission :t t :active-mrn pid
                             :home-ward "Renal"
                             :location {:ward "Renal" :bed bed :placement :licensed}
                             :participants [{:patient-id pid :role :subject}]})
          world (reduce (fn [w ev]
                          (:world (fold/apply-events {:world w} [ev]
                                                     #{:patient-bootstrap :patient-state
                                                       :board})))
                        {:patients {}}
                        [(admit "P1" 0) (admit "P2" 10)])
          patients (:patients world)]
      (is (= {bed "P1"} (naive-board (dissoc patients "P2")))
          "sanity: both patients really are in the one bed")
      (is (= {bed "P2"} (:board world))
          "the index answers the LAST WRITER IN EVENT ORDER -- P2 admitted second")
      (is (contains? #{"P1" "P2"} (get (naive-board patients) bed))
          "the definition answers whichever comes last in the map's own SEQ order,
           which is a hash-order artefact and is deliberately not pinned to a name")
      (testing "and the mask diverges in the direction the ruling names: dropping
                the id the index holds drops the KEY, where the definition would
                hand the bed to the other occupant"
        (is (= {} (fold/board-without world "P2")))
        (is (= {bed "P1"} (naive-board (dissoc patients "P2"))))
        (is (= {bed "P2"} (fold/board-without world "P1"))
            "in the other direction the mask keeps the key, because the board does
             not name P1 -- and the definition keeps it too")
        (is (= {bed "P2"} (naive-board (dissoc patients "P1")))
            "-- the one direction in which the two agree even here, because only
             one occupant is left for either to name")))))

(deftest the-run-loop-world-carries-the-board-from-t-zero
  (testing "the board is the ONE in-fold index `run` seeds rather than
            waits for, because `decide` runs BEFORE the batch that would
            open it and `sim-model/free` calls the board as a PREDICATE
            -- `(remove board ids)` on a missing one throws. The seed is
            the definition's own answer: every patient `init-world`
            carries is `state/initial-patient`, which names no location."
    (let [empty-world {:patients {"p1" {:patient-id "p1" :status :new}}}]
      (is (= {} (naive-board (:patients empty-world)))
          "a world of freshly seeded patients has an EMPTY board, not a missing one")
      (is (= {} (sim-model/occupancy-board (:patients empty-world)))
          "and the shipped definition says the same, which is what makes {} the seed")
      (is (nil? (:board (:world (fold/apply-events {:world empty-world} []
                                                   #{:patient-state :board}))))
          "an empty batch opens NO index at all -- the concern is per-EVENT, and
           THIS is the gap `run` seeds across: the first `decide` runs before the
           first batch, so the world it reads has folded nothing")
      (is (nil? (:board (:world (fold/apply-events {:world empty-world} []
                                                   #{:patient-state}))))
          "and a projection that does not name it opens nothing either -- guarded
           by membership and by nothing else")
      (is (= {} (:board (:world (fold/apply-events {:world (assoc empty-world :board {})}
                                                   [] #{:patient-state :board}))))
          "a seeded world keeps its seed across a batch that touches nothing"))))

(deftest the-fold-tolerates-an-absent-index-on-entry
  (testing "`update-board`'s `(or index {})` seed, exercised directly: a
            world carrying patients but no `:board` key must fold to the
            board those patients have, not throw and not stay missing.
            This is what makes a hand-built world legal input to the
            choke point."
    (let [ev {:event :admission :t 0 :active-mrn "P1" :home-ward "Renal"
              :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}
              :participants [{:patient-id "P1" :role :subject}]}
          w (:world (fold/apply-events {:world {:patients {}}} [ev]
                                       #{:patient-bootstrap :patient-state :board}))]
      (is (= {"RENAL-01" "P1"} (:board w)))
      (is (= (naive-board (:patients w)) (:board w))))))
