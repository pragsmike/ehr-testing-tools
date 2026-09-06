(ns ehrt.sim-engine.boarder-index-test
  "ADR-0180 site 1's LAW: the from-scratch definition is the gate.

  `decide/waiting-boarder` was an O(P) scan of `(:patients world)` per
  `:discharge` and per `:bed-ready`, and at the top of the measured
  decade it was 30.54% of the whole generate phase
  (`.agents/plans/2026-09-05-performance-measurement/measurements.md`).
  ADR-0180's R-fold-carrier replaces it with an index maintained at
  `fold/apply-events` -- `home-ward -> sorted-set of [admitted-at
  patient-id]` -- and keeps the scan HERE, verbatim, as the reference
  the index is proven equal to.

  WHAT IS ASSERTED, and why each half of it is load-bearing
  (ADR-0180 site 1's equivalence argument, written before the session
  that landed the index rather than asserted by a bracket after it):

  * IDENTITY of the returned id, not \"a legal boarder\". The weaker
    claim is one an index can satisfy while moving every byte after it,
    because a non-nil answer takes a branch that DRAWS TWICE --
    `bed-ready-location` on the world stream, `vacate-bed` on the
    facility stream -- so which id comes back decides the draw order of
    everything downstream.
  * NIL-VS-NON-NIL on its own. A stale entry an index failed to evict
    emits a `:transfer` where the scan emitted nothing; a missing entry
    drops one.
  * AT EVERY REPLAY ENTRY, not at the end. An index is a claim about
    every intermediate state, and an end-state-only assertion is
    exactly the assertion that passes on a carrier which is wrong for
    500,000 events and right for the last one (ADR-0180's law, point
    2).
  * THE EXCLUDED-ID CASE. `decide :discharge` passes the discharging
    patient as `excluded-id`; `decide :bed-ready` passes nil. Excluding
    the head of a ward's order is the one input under which the index
    must read past its own first entry, so the property drives it
    deliberately rather than waiting for it to occur.

  AND IT IS NOT VACUOUS. `the-corpus-actually-reaches-every-answer-
  shape` below is this namespace's own version of ADR-0169's rule that
  a comparison of two empty seqs is not an equivalence proof: it asserts
  that the generated corpus reaches non-nil answers, reaches an
  exclusion that changes the answer to a DIFFERENT id, and reaches an
  exclusion that changes it to nil. Without that, `(= nil nil)` at every
  entry would pass forever.

  THE DUPLICATION IS PERMANENT AND DECLARED, exactly as ADR-0169's six
  `naive-*` invariant bodies are (`rulings.md#R-move-not-improve`: the
  scan MOVES here, verbatim, and is not improved on the way)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.fold :as fold]
            [ehrt.sim-engine.run :as run]))

;; --- the from-scratch definition, kept verbatim ---------------------------

(defn- naive-waiting-boarder
  "`ehrt.sim-engine.decide/waiting-boarder`'s BODY, moved here character
  for character under `rulings.md#R-move-not-improve`. Every term of the
  predicate, the `sort-by` key and the `ffirst` are the shipped scan's
  own; the docstring that stood over it stays with the shipped function,
  which is where a reader asking what a boarder IS should still land."
  [world excluded-id ward-name]
  (->> (:patients world)
       (remove (fn [[pid _]] (= pid excluded-id)))
       (filter (fn [[_ p]] (and (= :admitted (:status p))
                                (some? (get-in p [:location :ward]))
                                (not= (:home-ward p) (get-in p [:location :ward]))
                                (= ward-name (:home-ward p)))))
       (sort-by (fn [[pid p]] [(:admitted-at p) pid]))
       ffirst))

;; --- the corpus -----------------------------------------------------------

(def ^:private boarding-facility
  "ONE licensed Renal bed and one Renal surge slot, with an ED that has
  no licensed beds at all -- `bed_cycle_test.clj`'s own fixture shape,
  and chosen for the same reason: at this capacity the allocation ladder
  reaches BOARDING within a handful of arrivals, which is the only
  condition under which a boarder index has anything in it."
  {:id :boarder-index-fixture
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
  "Churn-BEARING logs, per ADR-0180's law: cancels and transfers-in-error
  are what move a patient into and out of the boarder set by paths an
  add/remove-per-event-kind index would have had to enumerate. The whole
  profile is driven at one probability so a single draw says how churned
  a case is."
  (gen/let [p (gen/elements [0.2 0.4 0.7])]
    (into {} (map (fn [[k _]] [k p])) churn/default-churn-profile)))

(def ^:private never-a-ward
  "A ward name no patient can ever hold, so the index's answer for it is
  the empty-bucket nil rather than the empty-set nil."
  "No Such Ward")

(defn- wards-in-play
  "Every ward the two answers could disagree about at this world: every
  `:home-ward` any patient holds, every bucket the index has opened, and
  one ward that exists nowhere."
  [world]
  (into #{never-a-ward}
        (concat (keep (comp :home-ward val) (:patients world))
                (keys (:boarder-index world)))))

(defn- worlds
  "EVERY INTERMEDIATE WORLD, one event at a time -- `reductions`, so the
  seed world (before any event) is included and the last is the whole
  log applied.

  The projection is the three per-event concerns this question needs and
  no others: `:patient-bootstrap` puts each event's participants in the
  map, `:patient-state` folds `evolve`, `:boarder-index` maintains the
  index. Those three are per-EVENT concerns, so folding one event at a
  time is the same computation the run loop performs per batch -- the
  per-batch concerns (`:log-mirror`, `:log-accumulator`,
  `:state-history`) and the two decorations are the ones batching would
  matter to, and none of them is here."
  [ground-truth]
  (reductions (fn [w ev]
                (:world (fold/apply-events {:world w} [ev]
                                           #{:patient-bootstrap :patient-state
                                             :boarder-index})))
              {:patients {}}
              ground-truth))

(defn- answers
  "The two functions' answers at one world, for every ward in play and
  for three exclusions: nil (what `decide :bed-ready` passes), the
  ward's own current head (which forces the index to read PAST its first
  entry, the one input `decide :discharge` can produce and the one the
  ordered set could get wrong), and an id no patient carries.

  Returned as a vector of `[naive index]` pairs rather than compared
  here, so the caller can compare with `=` -- which asserts ORDER as
  well as content, and so that the non-vacuity test below can read the
  same shape."
  [world]
  (vec (for [ward (sort (wards-in-play world))
             excluded [nil (naive-waiting-boarder world nil ward) "no-such-patient"]]
         [(naive-waiting-boarder world excluded ward)
          (fold/first-boarder world excluded ward)])))

(defn- corpus
  "One case's whole trace of answer pairs, over every intermediate
  world. `:bed-cycle` is driven both ways deliberately: the two callers
  are MUTUALLY EXCLUSIVE in practice -- `decide :discharge` asks only
  when `(:beds world)` is nil, `decide :bed-ready` exists only when it
  is not -- so a corpus that ran one setting would exercise one caller's
  question and not the other's."
  [{:keys [seed patients churn-profile bed-cycle]}]
  (let [{:keys [ground-truth]} (run/run (cond-> {:seed seed :patients patients
                                                 :arrival-gap 20
                                                 :facility boarding-facility
                                                 :pathway stay
                                                 :churn-profile churn-profile}
                                          bed-cycle (assoc :bed-cycle true)))]
    (mapv answers (worlds ground-truth))))

;; --- the law --------------------------------------------------------------

(defspec the-index-answers-exactly-what-the-scan-does
  {:num-tests 40 :seed 20260906}
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 8 24)
                 churn-profile churn-fraction-gen
                 bed-cycle gen/boolean]
    (let [trace (corpus {:seed seed :patients patients
                         :churn-profile churn-profile :bed-cycle bed-cycle})]
      (and (seq trace)
           (every? (fn [world-answers]
                     (every? (fn [[naive indexed]] (= naive indexed)) world-answers))
                   trace)))))

;; --- and it is not vacuous ------------------------------------------------

(deftest the-corpus-actually-reaches-every-answer-shape
  (testing "ADR-0169's rule, applied to an index: a comparison of two
            nils at every entry is not an equivalence proof. These are
            the four shapes the property must actually be deciding, and
            each is counted over one PINNED case per bed-cycle setting."
    (doseq [bed-cycle [false true]]
      (let [profile (into {} (map (fn [[k _]] [k 0.4])) churn/default-churn-profile)
            trace (corpus {:seed 20260906 :patients 20
                           :churn-profile profile :bed-cycle bed-cycle})
            pairs (apply concat trace)
            ;; per world x ward, the three exclusions in `answers`' own
            ;; order: nil, the head itself, an absent id.
            triples (partition 3 (mapv first pairs))
            non-nil (count (filter some? (map first triples)))
            excluded-to-other (count (filter (fn [[head excl _]]
                                               (and (some? head) (some? excl)
                                                    (not= head excl)))
                                             triples))
            excluded-to-nil (count (filter (fn [[head excl _]]
                                             (and (some? head) (nil? excl)))
                                           triples))
            all-agree (every? (fn [[naive indexed]] (= naive indexed)) pairs)]
        (is all-agree
            (str "bed-cycle " bed-cycle ": every answer pair agrees"))
        (is (pos? non-nil)
            (str "bed-cycle " bed-cycle ": the corpus reaches a NON-NIL boarder"))
        (is (pos? excluded-to-other)
            (str "bed-cycle " bed-cycle ": excluding the head reaches a DIFFERENT boarder"
                 " -- the input under which the index must read past its first entry"))
        (is (pos? excluded-to-nil)
            (str "bed-cycle " bed-cycle ": excluding the head reaches NIL"
                 " -- the sole-boarder case, where nil-vs-non-nil is the whole answer"))))))

(deftest the-index-is-absent-rather-than-wrong-before-the-first-batch
  (testing "the run loop's `init-world` carries no `:boarder-index` --
            every patient is seeded `:status :new`, so the index it does
            not carry is the empty one, and `first-boarder` must read a
            missing key as nil rather than throw. This is what makes the
            fold's own `(or index {})` seed legal."
    (let [seeded {:patients {"p1" {:patient-id "p1" :status :new}}}]
      (is (nil? (fold/first-boarder seeded nil "Renal")))
      (is (nil? (naive-waiting-boarder seeded nil "Renal")))
      (is (nil? (:boarder-index (:world (fold/apply-events {:world seeded} []
                                                            #{:patient-state :boarder-index}))))
          "an empty batch opens no index at all -- the concern is per-EVENT"))))

(deftest the-bed-flip-decide-bed-ready-makes-is-patients-identical
  (testing "THE SECOND CALLER'S ARGUMENT, shown irrelevant rather than
            argued to be. `decide :discharge` asks its question against
            `world` and excludes the discharging patient; `decide
            :bed-ready` asks against `world'`, which is `(assoc-in world
            [:beds bed :status] :ready)` -- the bed whose readiness is
            the occasion -- and excludes nobody. ADR-0180 site 1 rests
            on ONE carrier serving both, which holds only if that flip
            is invisible to a `:patients`-derived index. It is: the flip
            touches `:beds`, and neither the index nor the scan it
            replaces reads a bed."
    (let [profile (into {} (map (fn [[k _]] [k 0.4])) churn/default-churn-profile)
          {:keys [ground-truth]} (run/run {:seed 20260906 :patients 20
                                           :arrival-gap 20
                                           :facility boarding-facility
                                           :pathway stay
                                           :churn-profile profile
                                           :bed-cycle true})
          sampled (take-nth 5 (rest (worlds ground-truth)))]
      (is (seq sampled) "the corpus produced worlds to check")
      (doseq [w sampled]
        (let [with-beds (assoc w :beds {"RENAL-01" {:status :cleaning :since-t 0}})
              flipped (assoc-in with-beds [:beds "RENAL-01" :status] :ready)]
          (is (= (:patients with-beds) (:patients flipped))
              "the flip is :beds-only -- :patients is untouched")
          (is (= (:boarder-index with-beds) (:boarder-index flipped))
              "and so, therefore, is the index derived from it")
          (doseq [ward (wards-in-play w)]
            (is (= [(naive-waiting-boarder with-beds nil ward)
                    (fold/first-boarder with-beds nil ward)]
                   [(naive-waiting-boarder flipped nil ward)
                    (fold/first-boarder flipped nil ward)])
                (str "ward " ward ": both functions answer the same on both worlds"))))))))
