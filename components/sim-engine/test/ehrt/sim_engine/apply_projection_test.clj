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
  records, and that all three are subsets of the closure.

  THE CLOSURE HAS GROWN TWICE SINCE THE CENSUS, and this namespace is
  where that is written down. ADR-0180 site 1 added `:boarder-index` on
  2026-09-06, the first index of the generate-quadratic program to ride
  `apply-events`; site 3 added `:board` the same day, the occupancy
  board. Site 1 -- the run loop -- opts into both; sites 2 and 3 opt
  into neither, under that charter's own contract that an index is
  guarded by its projection membership and by nothing else, so a site
  which never reads one pays nothing for it. Those FOUR ABSENCES are
  transcribed here as deliberately as the presences are -- a later
  session that 'completes' any of those columns would be undoing a
  decision, exactly as it would with site 2's `:warm-up-mark`, and the
  four negative assertions below are what say so out loud."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-engine.fold :as fold]
            [ehrt.sim-engine.log-index :as log-index]))

(def ^:private census-arc-thirteen
  "Section 1's inventory: the apply-unification arc's own THIRTEEN. Kept
  as its own set so the arc's 38-of-39 arithmetic below is still
  checkable after ADR-0180 widened the closure past it."
  #{:encounter-stamp :warm-up-mark :log-ordinal :reinstate-index
    :citation-index :registration-index :patient-bootstrap
    :patient-state :bed-index :log-mirror :log-accumulator
    :state-history :replay-entries})

(def ^:private census-full-algebra
  "FIFTEEN: section 1's inventory plus ADR-0180's two in-fold indexes,
  site 1's `:boarder-index` and site 3's `:board` -- the closure a
  projection is a subset of today."
  (conj census-arc-thirteen :boarder-index :board))

(def ^:private census-site-1
  "Section 2, column `site 1 -- run fold`: eleven PRESENT cells at stage
  1, ALL THIRTEEN since stage 2 enabled both of section 3a's pairs,
  `:patient-bootstrap` and `:replay-entries`. Site 1 is at the ruled end
  state -- full product -- and this set is `full-algebra` written out
  rather than aliased, for the same reason every other column here is a
  transcription.

  ADR-0180 makes it FIFTEEN, and both of its additions are here for the
  same reason: this is the ONE site that reads either. `decide
  :discharge` and `decide :bed-ready` ask `waiting-boarder` its question
  against the world this fold returns (`:boarder-index`, site 1), and
  four `decide` methods plus
  `log-index/bed-reoccupied-by-someone-else?` ask their occupancy
  question against that same world (`:board`, site 3)."
  #{:encounter-stamp :warm-up-mark :log-ordinal :reinstate-index
    :citation-index :registration-index :patient-bootstrap
    :patient-state :bed-index :boarder-index :board :log-mirror
    :log-accumulator :state-history :replay-entries})

(def ^:private census-site-2
  "Section 2, column `site 2 -- replay`: TWELVE PRESENT cells -- three
  at stage 1, the eight INERT pairs stage 2 enabled in census order, and
  the DECORATION `:encounter-stamp` under ruling A1(b), whose
  OUTPUT-MOVING prediction stage 2's measurement refuted. Whether that
  one STAYS inert is gated by
  `ehrt.sim-engine.apply-restamp-identity-test`, not by this
  transcription.

  THE THIRTEENTH IS ABSENT ON PURPOSE AND PERMANENTLY: 2 x
  `:warm-up-mark`, ruling A2(b), because `replay` has no source for the
  window and a declared 0 measurably destroys the log's own marks
  (census section 3e). A later session that 'completes' this column has
  undone a decision, not finished the arc -- which is why the ARC's own
  count below reads 38 of 39 and not 39.

  THE FOURTEENTH AND FIFTEENTH ARE ABSENT TOO, and for a third reason:
  ADR-0180's `:boarder-index` (site 1) and `:board` (site 3) are not
  cells of the arc's thirty-nine at all. `replay` returns entries,
  neither index is in one, and this site is 50.97% of the check phase --
  so the charter's guard-by-membership contract is what keeps two
  generate-side indexes off the check-side wall. `ehrt.sim-check.check`
  asks the board's own question over these entries and asks it of
  `sim-model/occupancy-board`, the DEFINITION, which is what leaves this
  column free to decline the index."
  #{:encounter-stamp :log-ordinal :reinstate-index :citation-index
    :registration-index :patient-bootstrap :patient-state :bed-index
    :log-mirror :log-accumulator :state-history :replay-entries})

(def ^:private census-site-3
  "Section 2, column `site 3 -- reinstated-state`: ALL THIRTEEN --
  three inherited from site 2 at stage 1 (correction C5), the INERT
  pairs stage 2 enabled in census order, and the DECORATION
  `:encounter-stamp` under ruling A1(b). FULL PRODUCT OF THE ARC'S
  THIRTEEN, its ruled end state -- and since ADR-0180 that is thirteen
  of FIFTEEN, `:boarder-index` and `:board` both being absent because
  `reinstated-state` returns a patient state and neither index is in
  one. This set is `census-arc-thirteen` written out rather than aliased
  for the same reason site 1's is."
  #{:encounter-stamp :warm-up-mark :log-ordinal :reinstate-index
    :citation-index :registration-index :patient-bootstrap
    :patient-state :bed-index :log-mirror :log-accumulator
    :state-history :replay-entries})

(deftest projections-match-the-census-matrix
  (testing "the closure is section 1's thirteen concerns plus ADR-0180's two"
    (is (= census-full-algebra fold/full-algebra)
        "fold/full-algebra is the census's section-1 inventory plus the two indexes")
    (is (= 15 (count fold/full-algebra)))
    (is (= #{:boarder-index :board} (set (remove census-arc-thirteen fold/full-algebra)))
        "exactly two concerns have been added since the census, and both are ADR-0180's"))

  (testing "each site's projection is its own matrix column"
    (is (= census-site-1 fold/run-loop-projection)
        "site 1 -- run's in-loop fold, all fifteen -- full product")
    (is (= census-site-2 fold/replay-projection)
        "site 2 -- replay, twelve of fifteen")
    (is (= census-site-3 fold/reinstated-projection)
        "site 3 -- reinstated-state's fallback, thirteen of fifteen"))

  (testing "ADR-0180's contract, stated as an assertion and not a
            comment: an index is guarded by its projection membership
            and by nothing else, so the two sites that never read
            `:boarder-index` or `:board` do not name them. Completing
            any of those columns would undo a decision, not finish an
            arc"
    (is (contains? fold/run-loop-projection :boarder-index))
    (is (not (contains? fold/replay-projection :boarder-index)))
    (is (not (contains? fold/reinstated-projection :boarder-index)))
    (is (contains? fold/run-loop-projection :board))
    (is (not (contains? fold/replay-projection :board)))
    (is (not (contains? fold/reinstated-projection :board))))

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

  (testing "the ARC's own arithmetic, unmoved by ADR-0180: 38 present
            cells of 39. The ONE omitted cell is 2 x `:warm-up-mark`,
            and it is a MEASURED PERMANENT omission rather than an
            unfinished pair"
    (let [arc (fn [p] (count (filter census-arc-thirteen p)))]
      (is (= 38 (+ (arc fold/run-loop-projection)
                   (arc fold/replay-projection)
                   (arc fold/reinstated-projection))))
      (is (= 1 (- (* 3 (count census-arc-thirteen))
                  (+ (arc fold/run-loop-projection)
                     (arc fold/replay-projection)
                     (arc fold/reinstated-projection)))))))

  (testing "and the WHOLE matrix's, which ADR-0180 moved twice: 40
            present cells of 45. The five absences are the arc's one
            plus two each for `:boarder-index` and `:board`, and every
            one of them is ruled"
    (is (= 40 (+ (count fold/run-loop-projection)
                 (count fold/replay-projection)
                 (count fold/reinstated-projection))))
    (is (= 5 (- (* 3 (count fold/full-algebra))
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
