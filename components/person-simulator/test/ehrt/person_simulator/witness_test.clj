(ns ehrt.person-simulator.witness-test
  "The counted witness (`rulings.md#R-witness-population-is-counted`).

  One config+seed under which every one of ADR-0172 section 2's
  FOURTEEN event kinds occurs at least once, with the count of each
  PINNED. Both halves matter and neither replaces the other: `pos?`
  alone would let a kind that should be common appear once by
  accident, and a pinned total alone would not say which kinds a
  reshuffle emptied.

  A change to any hazard rate, to the draw block, or to the fixture
  moves these numbers, and that is the point -- the diff is the
  evidence. Re-pin deliberately, never reflexively: ADR-0171's own
  lesson is that a reshuffle EMPTIES knife-edge fixtures silently, so
  a count that fell to zero is a finding and not a number to update."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.person-simulator.fixture :as fx]))

(def expected-counts
  "Pinned at arc 2b's green, 2026-08-25: 60 persons, 24 years, master
  seed 42, `:merge-fraction` 0.35."
  {:coverage-change      147
   :delivery             26
   :employment-change    135
   :household-form       38
   :household-join       52
   :household-leave      16
   :identity-correction  36
   :identity-resolution  5
   :identity-unavailable 5
   :occupational-injury  5
   :person-death         17
   :person-registered    26
   :pregnancy            26
   :residence-move       163})

(def expected-total 697)

(deftest every-one-of-the-fourteen-kinds-has-a-counted-witness-test
  (let [actual (frequencies (map :event (fx/evs)))]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq (fx/evs)) "the witness config produced no events at all"))
    (testing "all fourteen kinds occur"
      (is (= (set (keys expected-counts)) (set (keys actual)))
          (str "kinds present: " (sort (keys actual))))
      (doseq [k (sort (keys expected-counts))]
        (is (pos? (get actual k 0)) (str k " has no witness at all"))))
    (testing "and each count is exactly what this config+seed pins"
      (is (= expected-counts actual)
          (str "witness counts moved. Expected " (into (sorted-map) expected-counts)
               ", got " (into (sorted-map) actual)
               " -- re-pin only after deciding the move was intended.")))
    (is (= expected-total (count (fx/evs))))))
