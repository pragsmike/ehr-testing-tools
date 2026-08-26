(ns ehrt.person-simulator.witness-test
  "The counted witness (`rulings.md#R-witness-population-is-counted`).

  One config+seed under which every one of ADR-0172 section 2's
  FIFTEEN event kinds occurs at least once, with the count of each
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
  "RE-PINNED at arc 3a's green, 2026-08-25: 60 persons, 24 years, master
  seed 42, `:merge-fraction` 0.35, `:unhoused {:t0-fraction 0.08}`.

  ONE cause, and it is declared: `:residence-loss` appended a
  NINETEENTH variate to the year block and a t0 residence variate to
  every t0 person, so every draw from a person's second year onward
  moved. Nothing calls this component (ADR-0172 ruling F1), so no
  corpus moved with it -- this table is the whole blast radius.

  Two movements are FINDINGS rather than numbers, disclosed here so a
  later reader meets them:

  * `:occupational-injury` fell 5 -> 2, and ADR-0173 section 1 named
    it as one of the four thinnest fixtures in this witness BEFORE the
    reshuffle. It is still `pos?`, so no gate went vacuous, but it is
    now the thinnest thing here.
  * `:delivery` / `:person-registered` / `:pregnancy` fell 26 -> 11
    together, which is the one movement of the three (a delivery is
    deterministic given its pregnancy).

  The witness population was deliberately NOT widened to compensate.
  Widening it would move all fifteen counts for a SECOND reason in the
  same diff, which is exactly the confounding ADR-0173 ruling D1 exists
  to avoid.

  Previous pin, arc 2b 2026-08-25 (fourteen kinds, 697 events):
  coverage-change 147, delivery 26, employment-change 135,
  household-form 38, household-join 52, household-leave 16,
  identity-correction 36, identity-resolution 5, identity-unavailable
  5, occupational-injury 5, person-death 17, person-registered 26,
  pregnancy 26, residence-move 163."
  {:coverage-change      131
   :delivery             11
   :employment-change    122
   :household-form       34
   :household-join       37
   :household-leave      10
   :identity-correction  26
   :identity-resolution  8
   :identity-unavailable 8
   :occupational-injury  2
   :person-death         19
   :person-registered    11
   :pregnancy            11
   :residence-loss       6
   :residence-move       118})

(def expected-total 554)

(deftest every-one-of-the-fifteen-kinds-has-a-counted-witness-test
  (let [actual (frequencies (map :event (fx/evs)))]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq (fx/evs)) "the witness config produced no events at all"))
    (testing "all fifteen kinds occur"
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
