(ns ehrt.sim-engine.cancel-index-test
  "ADR-0180 site 4's LAW: the from-scratch definition is the gate.

  `log-index/last-uncancelled-index` made TWO whole-log passes PER CALL
  -- one building the set of indices this cancel class had already
  consumed, one `keep-indexed` taking the LAST matching index -- and the
  three cancel `decide` methods call it once each. Flat across the
  measured decade at around 10.8% of the generate phase, and the largest
  named site left under `decide` once sites 1 to 3 had landed
  (`.agents/plans/2026-09-05-performance-measurement/measurements.md`).
  ADR-0180's R-fold-carrier replaces the scan with an index maintained
  at `fold/apply-events`; ADR-0169's F-3 admitted this site only on
  condition that THE SAME CARRIER answers the query with NO SECOND CODE
  PATH, and this namespace is where the from-scratch scan lives now that
  `src` holds exactly one implementation of the answer.

  WHAT IS ASSERTED, and why each part is load-bearing (ADR-0180 site 4's
  equivalence argument, written before this session rather than after
  it):

  * INDEX-VALUE IDENTITY, integer or nil, not \"a legal target\". The
    answer is written into the emitted event as `:cancels-event-id`, and
    a nil where the scan returned an integer turns a legal cancel into a
    `:step-rejected` -- which changes the event stream and therefore
    every draw after it, the same mechanism that moved 21
    `:cancel-transfer` events in ADR-0179's own addendum. Site 4 is
    neither draw- nor allocation-affecting and owes no draw-ORDER
    argument; that is not the same as being cheap to get wrong.

  * AT EVERY INTERMEDIATE WORLD, not at the end. An index is a claim
    about every intermediate state, and an end-state-only assertion is
    exactly the assertion that passes on a carrier which is wrong for
    500,000 events and right for the last one (ADR-0180's law, point 2).

  * FOR EVERY (patient, event-type, cancel-type) THE CALLERS ASK, plus a
    patient that never existed. The three pairs are `decide`'s own and
    are transcribed below rather than derived, for the reason
    `apply-projection-test` transcribes the projection matrix by hand.

  * THE LAST-NOT-FIRST SUBTLETY, pinned on its own. A patient re-admitted
    after a `:cancel-admit` has TWO `:admission` entries with the first
    already consumed, and the scan's `last` is what makes the next
    cancel find the SECOND. An index read forwards would find the first
    uncancelled entry -- a different function that agrees on every
    patient admitted once, which is nearly all of them, so a generated
    corpus alone would not decide it.

  AND IT IS NOT VACUOUS. `the-corpus-actually-reaches-every-cancel-shape`
  is this namespace's version of ADR-0169's rule that a comparison of
  two nils is not an equivalence proof: it asserts that the generated
  corpus reaches non-nil answers, reaches the already-cancelled nil as
  well as the never-existed nil, reaches events filed under two patients
  at once, and reaches a patient carrying more than one event of a
  cancellable class.

  THE DUPLICATION IS PERMANENT AND DECLARED, exactly as ADR-0169's six
  `naive-*` invariant bodies and site 1's `naive-waiting-boarder` are
  (`rulings.md#R-move-not-improve`: the definition is MOVED here
  verbatim, and is not improved on the way)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.fold :as fold]
            [ehrt.sim-engine.run :as run]))

;; --- the from-scratch definition, kept verbatim ---------------------------

(defn- naive-last-uncancelled-index
  "`ehrt.sim-engine.log-index/last-uncancelled-index`'s BODY as it stood
  before ADR-0180 site 4, copied here character for character under
  `rulings.md#R-move-not-improve`. Unlike site 3's `naive-board` this is
  a MOVE and not a copy: `src` keeps no second implementation of this
  answer, which is the whole of ADR-0169 F-3's condition for admitting
  the site at all.

  Kept, and not called through `log-index`, for the reason every
  `naive-*` in this repository is kept: a law whose reference side is
  the shipped function is a law that cannot notice the shipped function
  changing."
  [ground-truth patient-id event-type cancel-type]
  (let [already-cancelled (into #{}
                                (comp (filter #(= cancel-type (:event %)))
                                      (map :cancels-event-id))
                                ground-truth)]
    (last (keep-indexed (fn [i ev]
                          (when (and (= event-type (:event ev))
                                     (some #(= patient-id (:patient-id %)) (:participants ev))
                                     (not (already-cancelled i)))
                            i))
                        ground-truth))))

;; --- what the callers actually ask ----------------------------------------

(def ^:private caller-queries
  "`decide`'s own three (event-type, cancel-type) pairs -- `:cancel-admit`
  over `:admission`, `:cancel-transfer` over `:transfer`,
  `:cancel-discharge` over `:discharge`. Transcribed by hand rather than
  derived from `decide`, so the two can disagree loudly: a fourth cancel
  class would be a new pair here and a new commit, which is the point at
  which this law is owed an edit."
  [[:admission :cancel-admit]
   [:transfer :cancel-transfer]
   [:discharge :cancel-discharge]])

;; --- the corpus -----------------------------------------------------------

(def ^:private cancel-facility
  "`board_index_test.clj`'s fixture with TWO licensed Renal beds instead
  of one, and the difference is measured rather than stylistic. This law
  needs patients carrying MORE THAN ONE event of a cancellable class --
  the shape `last`-not-`first` is about -- and at ONE bed the
  `:bed-cycle` half of the corpus starves: the ward is never free, the
  run exhausts inside thirty events, and no patient reaches a second
  admission at all. At two beds both halves reach the shape, and
  `the-corpus-actually-reaches-every-cancel-shape` below is what says so
  rather than leaving it assumed. Everything else is that fixture's: a
  surge-only ED, so beds stay contended and admissions, transfers and
  discharges pile up per patient."
  {:id :cancel-index-fixture
   :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 4
            :surge-format "%s-H%02d" :class :ed :turnaround-minutes [10 10]}
           {:id :renal :name "Renal" :beds 2 :surge-slots 2
            :surge-format "%s-H%02d" :class :inpatient :turnaround-minutes [10 10]}]})

(def ^:private stay
  {:name "stay"
   :steps [{:type :admission :location "Renal"}
           {:type :delay :from 45 :to 45}
           {:type :discharge}]})

(def ^:private churn-fraction-gen
  "Churn-BEARING logs, per ADR-0180's law, and here that means CANCEL-
  bearing: the shipped default profile fires the three cancel kinds at
  0.01 to 0.02, which over a few dozen arrivals is a corpus that mostly
  proves nil equals nil. Every kind is raised to a common fraction so
  the three query pairs are all actually decided, and `:merge` and
  `:bed-swap` come up with them because they are the events filed under
  TWO patients at once."
  (gen/let [p (gen/elements [0.2 0.4 0.7])]
    (into {} (map (fn [[k _]] [k p])) churn/default-churn-profile)))

(defn- worlds
  "EVERY INTERMEDIATE WORLD, one event at a time -- `reductions`, so the
  seed world (before any event) is included and the last is the whole
  log applied.

  THE PROJECTION IS THREE CONCERNS AND THE ABSENCES ARE A CLAIM.
  `:log-ordinal` supplies the log position the index keys on,
  `:log-mirror` grows the `:ground-truth` the naive side scans, and
  `:cancel-index` is the concern under test. `:patient-bootstrap` and
  `:patient-state` are DELIBERATELY ABSENT: site 4's index is
  event-derived, so no patient state is among its inputs, and a
  projection that folded one would leave that unsaid. The law still
  holds at every entry, which is the assertion that says so.

  THE SEED CARRIES `:cancel-index {}` BECAUSE `run`'s `init-world`
  DOES. The concern is per-EVENT, so a world that has folded no events
  has no `:cancel-index` key at all -- the difference between MISSING
  and EMPTY, and `fold/last-uncancelled` throws on the first where it
  answers nil on the second (`rulings.md#R-raw-read`). Seeding the trace
  the same way is what makes the entry BEFORE the first event a real
  entry of the law rather than an artefact of this helper;
  `the-run-loop-world-carries-the-cancel-index-from-t-zero` below pins
  both halves directly."
  [ground-truth]
  (reductions (fn [w ev]
                (:world (fold/apply-events {:world w} [ev]
                                           #{:log-ordinal :log-mirror :cancel-index})))
              {:ground-truth [] :cancel-index {}}
              ground-truth))

(defn- log-of
  [{:keys [seed patients churn-profile bed-cycle]}]
  (:ground-truth (run/run (cond-> {:seed seed :patients patients
                                   :arrival-gap 20
                                   :facility cancel-facility
                                   :pathway stay
                                   :churn-profile churn-profile}
                            bed-cycle (assoc :bed-cycle true)))))

(defn- probe-ids
  "The patients the query is asked about at one world: every patient the
  WHOLE log names, plus one id no event carries. The never-existed case
  is a real answer of this function -- nil, which `decide` turns into a
  structured rejection -- so it belongs in the law rather than outside
  it."
  [ground-truth]
  (conj (vec (sort (into #{} (comp (mapcat :participants) (keep :patient-id)) ground-truth)))
        "no-such-patient"))

(defn- disagreements
  "Every place the two sides differ at one world, as data rather than as
  assertions, so the property and the non-vacuity counts can read the
  same shape. Empty is the law holding."
  [world pids]
  (let [gt (:ground-truth world)]
    (vec (for [pid pids
               [event-type cancel-type] caller-queries
               :let [expected (naive-last-uncancelled-index gt pid event-type cancel-type)
                     actual (fold/last-uncancelled world pid event-type cancel-type)]
               :when (not= expected actual)]
           [pid event-type cancel-type expected actual]))))

;; --- the law --------------------------------------------------------------

(defspec the-index-answers-what-the-whole-log-scan-answers
  {:num-tests 25 :seed 20260906}
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 8 20)
                 churn-profile churn-fraction-gen
                 bed-cycle gen/boolean]
    (let [log (log-of {:seed seed :patients patients
                       :churn-profile churn-profile
                       :bed-cycle bed-cycle})
          pids (probe-ids log)
          trace (worlds log)]
      (and (seq trace)
           (every? (fn [w] (empty? (disagreements w pids))) trace)))))

;; --- and it is not vacuous ------------------------------------------------

(defn- answers
  "Every (pid, pair) answer at one world, tagged with why it is nil --
  `:hit` for an integer, `:all-cancelled` for a class the patient has
  events of but every one consumed, `:never` for a class they have none
  of. The three are counted separately below because they are three
  DIFFERENT things the law has to decide, and a corpus that reached only
  the third would be proving nil equals nil."
  [world pids]
  (let [gt (:ground-truth world)]
    (for [pid pids
          [event-type cancel-type] caller-queries
          :let [answer (fold/last-uncancelled world pid event-type cancel-type)
                has-any? (some (fn [ev]
                                 (and (= event-type (:event ev))
                                      (some #(= pid (:patient-id %)) (:participants ev))))
                               gt)]]
      (cond answer :hit
            has-any? :all-cancelled
            :else :never))))

(deftest the-corpus-actually-reaches-every-cancel-shape
  (testing "ADR-0169's rule, applied to an index: a comparison of two
            nils at every entry is not an equivalence proof. These are
            the shapes the property must actually be deciding, each
            counted over one PINNED case per bed-cycle setting."
    (doseq [bed-cycle [false true]]
      (let [profile (into {} (map (fn [[k _]] [k 0.4])) churn/default-churn-profile)
            log (log-of {:seed 20260906 :patients 16
                         :churn-profile profile :bed-cycle bed-cycle})
            pids (probe-ids log)
            trace (worlds log)
            tallies (frequencies (mapcat #(answers % pids) trace))
            last-world (last trace)
            by-patient (:by-patient (:cancel-index last-world))
            repeats (count (filter (fn [[[_ event-type] v]]
                                     (and (< 1 (count v))
                                          (some #{event-type} (map first caller-queries))))
                                   by-patient))
            two-movers (count (filter (fn [ev]
                                        (< 1 (count (filter :patient-id (:participants ev)))))
                                      log))
            cancelled (reduce + 0 (map count (vals (:cancelled (:cancel-index last-world)))))]
        (is (every? (fn [w] (empty? (disagreements w pids))) trace)
            (str "bed-cycle " bed-cycle ": every world agrees, every patient, every pair"))
        (is (pos? (:hit tallies 0))
            (str "bed-cycle " bed-cycle ": the corpus reaches a NON-NIL answer"))
        (is (pos? (:all-cancelled tallies 0))
            (str "bed-cycle " bed-cycle ": and the ALREADY-CANCELLED nil -- a class the"
                 " patient has events of, every one consumed"))
        (is (pos? (:never tallies 0))
            (str "bed-cycle " bed-cycle ": and the NEVER-EXISTED nil, which is a different"
                 " answer arrived at a different way"))
        (is (pos? cancelled)
            (str "bed-cycle " bed-cycle ": events really do carry :cancels-event-id --"
                 " otherwise the second map is empty and half the law is untested"))
        (is (pos? repeats)
            (str "bed-cycle " bed-cycle ": some patient carries MORE THAN ONE event of a"
                 " cancellable class -- the shape last-not-first is about"))
        (is (pos? two-movers)
            (str "bed-cycle " bed-cycle ": the corpus reaches events with TWO patient"
                 " participants, which are filed under both"))))))

;; --- the subtlety a generated corpus cannot be trusted to decide ----------

(deftest a-re-admit-after-a-cancel-admit-finds-the-LAST-admission
  (testing "ADR-0180 site 4's own named subtlety, pinned rather than left
            to a generator: the scan took `last`, so a patient whose
            first admission was cancelled and who was then re-admitted
            must have the SECOND admission found by the next
            cancel-admit. An index read forwards would answer 0 here and
            agree with the scan on every patient admitted once."
    (let [admit (fn [t] {:event :admission :t t :active-mrn "P1" :home-ward "Renal"
                         :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}
                         :participants [{:patient-id "P1" :role :subject}]})
          cancel (fn [t idx] {:event :cancel-admit :t t :active-mrn "P1"
                              :cancels-event-id idx
                              :participants [{:patient-id "P1" :role :subject}]})
          log [(admit 0) (cancel 10 0) (admit 20)]
          w (last (worlds log))]
      (is (= {["P1" :admission] [0 2] ["P1" :cancel-admit] [1]}
             (:by-patient (:cancel-index w)))
          "both admissions are filed, in log order, under the one key")
      (is (= {:cancel-admit #{0}} (:cancelled (:cancel-index w)))
          "and the cancel consumed index 0")
      (is (= 2 (fold/last-uncancelled w "P1" :admission :cancel-admit))
          "the SECOND admission, not the first")
      (is (= 2 (naive-last-uncancelled-index log "P1" :admission :cancel-admit))
          "-- which is what the scan answers, and the reason `last` was in it")
      (testing "and once that one is cancelled too there is nothing left"
        (let [w2 (last (worlds (conj log (cancel 30 2))))]
          (is (nil? (fold/last-uncancelled w2 "P1" :admission :cancel-admit)))
          (is (nil? (naive-last-uncancelled-index (conj log (cancel 30 2))
                                                  "P1" :admission :cancel-admit))))))))

(deftest a-cancel-class-only-consumes-its-own-events
  (testing "the second map is keyed by CANCEL TYPE, so a
            `:cancel-transfer` consuming index 1 leaves the
            `:cancel-admit` query's own view of index 1 untouched. The
            scan got this from its `(filter #(= cancel-type (:event %)))`
            and the index gets it from the key; asserted because a
            single flat set of consumed indices would pass every other
            test in this namespace."
    (let [log [{:event :admission :t 0 :active-mrn "P1" :home-ward "Renal"
                :participants [{:patient-id "P1" :role :subject}]}
               {:event :transfer :t 10 :active-mrn "P1" :home-ward "Renal"
                :participants [{:patient-id "P1" :role :subject}]}
               {:event :cancel-transfer :t 20 :active-mrn "P1" :cancels-event-id 1
                :participants [{:patient-id "P1" :role :subject}]}]
          w (last (worlds log))]
      (is (nil? (fold/last-uncancelled w "P1" :transfer :cancel-transfer))
          "the transfer at 1 is consumed")
      (is (= 0 (fold/last-uncancelled w "P1" :admission :cancel-admit))
          "and the admission at 0 is untouched by it")
      (is (= 0 (naive-last-uncancelled-index log "P1" :admission :cancel-admit))))))

(deftest an-event-with-two-patient-participants-is-filed-under-both
  (testing "`log-index/events-for-patient`'s own rule, which the index
            inherits: a `:merge` or a `:bed-swap` names two patients and
            appears in BOTH their vectors, not just the subject's. The
            scan got this from `some` over `:participants`."
    (let [log [{:event :bed-swap :t 0 :active-mrn "P1"
                :participants [{:patient-id "P1" :role :subject}
                               {:patient-id "P2" :role :counterparty}]}]
          w (last (worlds log))]
      (is (= {["P1" :bed-swap] [0] ["P2" :bed-swap] [0]}
             (:by-patient (:cancel-index w))))
      (is (= 0 (fold/last-uncancelled w "P2" :bed-swap :cancel-admit))
          "P2's own vector holds the event, exactly as the scan's `some` found it"))))

;; --- the seed, and the read that refuses to rebuild -----------------------

(deftest the-run-loop-world-carries-the-cancel-index-from-t-zero
  (testing "`:cancel-index` is the SECOND in-fold index `run` seeds
            rather than waits for, because `decide` runs BEFORE the
            batch that would open it and `fold/last-uncancelled` THROWS
            on a missing one (`rulings.md#R-raw-read`). It must throw
            rather than answer nil: nil is this query's own
            'no such event' answer, so a missing index read as nil would
            silently reject a legal cancel."
    (is (nil? (:cancel-index (:world (fold/apply-events {:world {}} []
                                                        #{:log-ordinal :cancel-index}))))
        "an empty batch opens NO index at all -- the concern is per-EVENT, and THIS is
         the gap `run` seeds across")
    (is (nil? (:cancel-index (:world (fold/apply-events {:world {}} []
                                                        #{:log-ordinal}))))
        "and a projection that does not name it opens nothing either -- guarded by
         membership and by nothing else")
    (is (= {} (:cancel-index (:world (fold/apply-events {:world {:cancel-index {}}} []
                                                        #{:log-ordinal :cancel-index}))))
        "a seeded world keeps its seed across a batch that touches nothing")
    (is (thrown? clojure.lang.ExceptionInfo
                 (fold/last-uncancelled {} "P1" :admission :cancel-admit))
        "a world carrying NO index throws rather than rebuilding the scan")
    (is (nil? (fold/last-uncancelled {:cancel-index {}} "P1" :admission :cancel-admit))
        "-- where the SEEDED empty index answers nil, which is the scan's own answer
         over an empty log")))

(deftest the-fold-tolerates-an-absent-index-on-entry
  (testing "`update-cancel-index`'s `(or index {})` seed, exercised
            directly: a world carrying a log but no `:cancel-index` key
            must fold to the index that log has, not throw and not stay
            missing. This is what makes a hand-built world legal input to
            the choke point even though the READ refuses one."
    (let [ev {:event :admission :t 0 :active-mrn "P1" :home-ward "Renal"
              :participants [{:patient-id "P1" :role :subject}]}
          w (:world (fold/apply-events {:world {:ground-truth []}} [ev]
                                       #{:log-ordinal :log-mirror :cancel-index}))]
      (is (= {:by-patient {["P1" :admission] [0]}} (:cancel-index w)))
      (is (= 0 (fold/last-uncancelled w "P1" :admission :cancel-admit)))
      (is (= (naive-last-uncancelled-index (:ground-truth w) "P1" :admission :cancel-admit)
             (fold/last-uncancelled w "P1" :admission :cancel-admit))))))
