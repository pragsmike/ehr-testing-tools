(ns ehrt.sim-engine.apply-projection-test
  "The co-landed invariant of the application-path unification's stage 1
  (`.agents/plans/apply-unification-census.md`): the three apply sites'
  PROJECTIONS are exactly the census's own matrix, section 2.

  WHY A GATE AND NOT A COMMENT. Stage 2 enables omitted (site x
  accumulator) pairs ONE COMMIT EACH, and its whole discipline is that a
  delta is checkable against a WRITTEN prediction rather than against
  memory. That only holds while the code and the matrix agree. This
  namespace transcribes section 2's three columns as literal sets and
  asserts the vars against them, so a projection cannot gain or lose a
  concern without a commit that also moves the row here -- which is the
  point at which the census is owed an edit too.

  IT IS DELIBERATELY A TRANSCRIPTION, not a derivation. Reading the
  matrix out of the markdown would make the gate agree with whatever the
  file happens to say; writing the expected sets here by hand, from the
  tree the census was derived from, is what lets the two disagree
  loudly. The same reason `ehrt.sim-check.check` writes out its own bed
  arithmetic rather than calling `fold/update-beds` (`check.clj`'s own
  comment above `bed-allocating-event-types`).

  WHAT IT DOES NOT GATE. It says nothing about whether a projection is
  RIGHT -- that is the census's cone predictions and stage 2's job. It
  says only that the code's three subsets are the ones the matrix
  records, and that all three are subsets of the closure."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-engine.fold :as fold]
            [ehrt.sim-engine.log-index :as log-index]))

(def ^:private census-full-algebra
  "Section 1's inventory: THIRTEEN concerns, the closure a projection is
  a subset of."
  #{:encounter-stamp :warm-up-mark :log-ordinal :reinstate-index
    :citation-index :registration-index :patient-bootstrap
    :patient-state :bed-index :log-mirror :log-accumulator
    :state-history :replay-entries})

(def ^:private census-site-1
  "Section 2, column `site 1 -- run fold`: eleven PRESENT cells at stage
  1, ALL THIRTEEN since stage 2 enabled both of section 3a's pairs,
  `:patient-bootstrap` and `:replay-entries`. Site 1 is at the ruled end
  state -- full product -- and this set is `full-algebra` written out
  rather than aliased, for the same reason every other column here is a
  transcription."
  #{:encounter-stamp :warm-up-mark :log-ordinal :reinstate-index
    :citation-index :registration-index :patient-bootstrap
    :patient-state :bed-index :log-mirror :log-accumulator
    :state-history :replay-entries})

(def ^:private census-site-2
  "Section 2, column `site 2 -- replay`: TWELVE PRESENT cells -- three
  at stage 1, the eight INERT pairs stage 2 enabled in census order, and
  the DECORATION `:encounter-stamp` under ruling A1(b), whose
  OUTPUT-MOVING prediction stage 2's measurement refuted. Whether that
  one STAYS inert is gated by
  `ehrt.sim-engine.apply-restamp-identity-test`, not by this
  transcription."
  #{:encounter-stamp :log-ordinal :reinstate-index :citation-index
    :registration-index :patient-bootstrap :patient-state :bed-index
    :log-mirror :log-accumulator :state-history :replay-entries})

(def ^:private census-site-3
  "Section 2, column `site 3 -- reinstated-state`: ALL THIRTEEN --
  three inherited from site 2 at stage 1 (correction C5), the INERT
  pairs stage 2 enabled in census order, and the DECORATION
  `:encounter-stamp` under ruling A1(b). FULL PRODUCT, the arc's ruled
  end state, and this set is `full-algebra` written out rather than
  aliased for the same reason site 1's is."
  #{:encounter-stamp :warm-up-mark :log-ordinal :reinstate-index
    :citation-index :registration-index :patient-bootstrap
    :patient-state :bed-index :log-mirror :log-accumulator
    :state-history :replay-entries})

(deftest projections-match-the-census-matrix
  (testing "the closure is section 1's thirteen concerns"
    (is (= census-full-algebra fold/full-algebra)
        "fold/full-algebra is the census's section-1 inventory")
    (is (= 13 (count fold/full-algebra))))

  (testing "each site's projection is its own matrix column"
    (is (= census-site-1 fold/run-loop-projection)
        "site 1 -- run's in-loop fold, all thirteen -- full product")
    (is (= census-site-2 fold/replay-projection)
        "site 2 -- replay, twelve of thirteen")
    (is (= census-site-3 fold/reinstated-projection)
        "site 3 -- reinstated-state's fallback, thirteen of thirteen -- full product"))

  (testing "every projection is a SUBSET of the closure -- no site names
            a concern the algebra does not have"
    (doseq [[site projection] [[:site-1 fold/run-loop-projection]
                               [:site-2 fold/replay-projection]
                               [:site-3 fold/reinstated-projection]]]
      (is (empty? (remove fold/full-algebra projection))
          (str site " names only concerns in full-algebra"))))

  (testing "site 3's projection is its OWN literal since stage 2's
            de-alias commit, and must STAY one -- re-aliasing would
            re-couple the two columns and silently enable a site-2 pair's
            twin at site 3, which is what one-pair-per-commit forbids"
    (is (not (identical? fold/replay-projection fold/reinstated-projection))))

  (testing "the matrix's own arithmetic: 38 present cells of 39. The
            ONE omitted cell is 2 x `:warm-up-mark`, and it is a
            MEASURED PERMANENT omission rather than an unfinished pair"
    (is (= 38 (+ (count fold/run-loop-projection)
                 (count fold/replay-projection)
                 (count fold/reinstated-projection))))
    (is (= 1 (- (* 3 (count fold/full-algebra))
                 (+ (count fold/run-loop-projection)
                    (count fold/replay-projection)
                    (count fold/reinstated-projection)))))))

(deftest the-two-policy-sets-still-resolve-through-log-index
  (testing "stage 1 moved them into `fold` and left delegating defs
            under C1(a) -- the defs hold the same objects, so a caller
            that named `log-index` still resolves"
    (is (identical? fold/reinstatable-event-types
                    log-index/reinstatable-event-types))
    (is (identical? fold/cited-opening-event-types
                    log-index/cited-opening-event-types))
    (is (= #{:transfer :discharge} fold/reinstatable-event-types))
    (is (= #{:medication-order :care-plan-start}
           fold/cited-opening-event-types))))
