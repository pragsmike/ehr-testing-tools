(ns ehrt.sim-engine.handed-projection-test
  "ADR-0180 SITE 7's LAW: the run's own entries and a replay's entries
  answer the SAME QUESTION at every site that reads one.

  Site 7 stops `check-all` folding the log for itself and lets a caller
  hand it a projection instead. In-run, that caller is `ehrt.sim.run`
  and the projection is `engine/run`'s own `:entries` -- which the
  engine has always minted, one per event, and used to throw away. The
  charter's central obligation is that those entries are usable where
  a replay's were, and it names THE ONE REASON THEY MIGHT NOT BE:

  * POPULATION. `run`'s `init-world` seeds `:patients` with EVERY
    patient at t 0, so its `:world-before` carries all of them from the
    first event. `replay` starts `{:patients {}}` and bootstraps a
    participant on first sight, so its `:world-before` carries only
    patients seen so far. THE TWO MAPS ARE GENUINELY DIFFERENT, and
    `the-population-divergence-is-real` asserts that rather than
    assuming it -- a law over two projections that turned out to be
    equal would be a law about nothing.

  * THE BOOTSTRAP MRN (ruling R-bootstrap-mrn). `init-world` seeds with
    `(mrn-for i)` and `replay`'s bootstrap with `(:active-mrn ev)`. For
    a patient's first event these are the same MRN in every log `run`
    produces -- and that is an assertion this namespace owes rather
    than an assumption the session may make.

  WHAT IS ASSERTED, site by site, is the addendum's own reading table
  (`notes/adr/0180-indexes-ride-the-fold.md`, `Addendum, 2026-09-06`
  section 7) rather than a general claim of entry equality, which is
  FALSE and is asserted to be false above:

  | what a reading site takes | asserted here |
  |---|---|
  | `:event` | equal, and equal to the log itself, entry for entry |
  | `:patient-id`, `:before`, `:after` | equal, entry for entry |
  | `:world-before`/`:world-after` AT A PARTICIPANT | equal for every participant of every event |
  | `:board` (ADR-0180 site 7, R-board-in-entry) | equal, entry for entry |

  The third row is the one that carries the population argument: the
  eight subject-only sites and the two participant-indexed sites read
  through a `get`, and a `get` cannot see a patient it does not ask
  for. The fourth row is what replaced the ONE site that did walk the
  whole map.

  AND IT IS NOT VACUOUS. `the-corpus-reaches-what-the-law-is-about`
  asserts that these logs actually reach multi-participant events,
  non-empty boards, bed cycles and repeat bootstraps -- ADR-0169's rule
  that comparing two empty things is not an equivalence proof."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.fold :as fold]
            [ehrt.sim-engine.run :as run]))

(def ^:private facility
  "`eligible_index_test.clj`'s fixture, and for its reason: a surge-only
  ED against a small inpatient ward keeps beds contended, so the board
  is non-empty and the surge ladder is actually exercised."
  {:id :handed-projection-fixture
   :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 4
            :surge-format "%s-H%02d" :class :ed :turnaround-minutes [10 10]}
           {:id :renal :name "Renal" :beds 6 :surge-slots 6
            :surge-format "%s-H%02d" :class :inpatient :turnaround-minutes [10 10]}]})

(def ^:private stay
  {:name "stay"
   :steps [{:type :admission :location "Renal"}
           {:type :delay :from 45 :to 45}
           {:type :discharge}]})

(def ^:private churn-profile
  "Every churn kind at a common, high fraction, the same device
  `eligible_index_test.clj` uses: the shipped default fires `:merge`
  and `:bed-swap` at 0.01-0.02, which over a few dozen arrivals is a
  corpus in which the two MULTI-PARTICIPANT events barely occur -- and
  a multi-participant event is exactly where a per-participant claim
  can fail."
  (into {} (map (fn [[k _]] [k 0.5])) churn/default-churn-profile))

(def ^:private scheduling
  "`scheduling_test.clj`'s own fixture rates, fat on purpose so all four
  appointment outcomes are witnessed at a small population. `:scheduling`
  is a CONFIG MAP and not a flag -- passing `true` makes `run` answer
  `result/error :invalid-scheduling` and produce no log at all, which is
  how this namespace's own `(is (seq entries))` caught the first cut of
  the configuration below."
  {:scheduled-fraction 0.5
   :lead-time-days [1 7]
   :no-show-rate 0.1
   :reschedule-rate 0.1
   :cancel-rate 0.1
   :follow-up {:rate 0.4 :interval-days [7 30]}})

(def ^:private configurations
  "Four runs, chosen so that between them they reach every event family
  a reading site can be handed: churn on its own, churn with the bed
  cycle (which mints `:bed-status-change`, whose first participant
  names a BED and carries no `:patient-id`), encounters (which stamp
  ids the per-encounter rows read off `:before`), and scheduling
  (whose appointments and follow-ups put events in the log that no
  patient's own pathway queued)."
  [{:label "churn" :seed 7 :patients 40}
   {:label "churn+bed-cycle" :seed 11 :patients 40 :bed-cycle true}
   {:label "churn+encounters" :seed 13 :patients 40 :encounters true}
   {:label "churn+scheduling+encounters" :seed 17 :patients 40
    :encounters true :scheduling scheduling}])

(defn- run-of [{:keys [seed patients bed-cycle encounters] :as cfg}]
  (run/run (cond-> {:seed seed :patients patients :arrival-gap 20
                    :facility facility :pathway stay
                    :churn-profile churn-profile}
             bed-cycle (assoc :bed-cycle true)
             encounters (assoc :encounters true)
             (:scheduling cfg) (assoc :scheduling (:scheduling cfg)))))

(defn- participants-of [event]
  (distinct (keep :patient-id (:participants event))))

(deftest run-entries-are-parallel-to-the-log-it-returns
  (doseq [cfg configurations]
    (testing (:label cfg)
      (let [{:keys [ground-truth entries]} (run-of cfg)]
        (is (seq entries)
            "the run returns its own replay entries -- ADR-0180 site 7")
        (is (= (count ground-truth) (count entries))
            "one entry per event, or the two cannot be indexed together")
        (is (= (vec ground-truth) (mapv :event entries))
            "and each entry's :event IS the log's event at that index")))))

(deftest every-reading-site-gets-the-same-answer-from-either-projection
  (doseq [cfg configurations]
    (testing (:label cfg)
      (let [{:keys [ground-truth entries]} (run-of cfg)
            replayed (fold/replay ground-truth)]
        (is (= (count entries) (count replayed)))
        (testing "the subject view -- :patient-id, :before, :after"
          (is (= (mapv #(select-keys % [:patient-id :before :after]) entries)
                 (mapv #(select-keys % [:patient-id :before :after]) replayed))))
        (testing ":board, the whole-world reader's replacement (R-board-in-entry)"
          (is (= (mapv :board entries) (mapv :board replayed))))
        (testing "the world views, AT EVERY PARTICIPANT of every event"
          (let [at-participants
                (fn [rows]
                  (mapv (fn [{:keys [event world-before world-after]}]
                          (mapv (fn [pid] [pid (get world-before pid) (get world-after pid)])
                                (participants-of event)))
                        rows))]
            (is (= (at-participants entries) (at-participants replayed)))))))))

(deftest the-bootstrap-mrn-is-the-seeded-mrn
  (testing "R-bootstrap-mrn: `(mrn-for i)` equals `(:active-mrn ev)` at
    every patient's FIRST event, asserted over generated logs. `run`
    seeds a patient's state with the first; `replay`'s bootstrap seeds
    it with the second, so the two `:before` values at a first event
    agree only if the two MRNs do."
    (doseq [cfg configurations]
      (testing (:label cfg)
        (let [{:keys [ground-truth entries]} (run-of cfg)
              firsts (reduce (fn [acc {:keys [event] :as entry}]
                               (reduce (fn [a pid]
                                         (if (contains? a pid) a (assoc a pid entry)))
                                       acc (participants-of event)))
                             {} entries)]
          (is (seq firsts))
          (doseq [[pid entry] firsts]
            (is (= (:active-mrn (get (:world-before entry) pid))
                   (:active-mrn (:event entry)))
                (str "seeded MRN vs the event's own :active-mrn at " pid
                     "'s first event"))))))))

(deftest the-population-divergence-is-real
  (testing "the two projections' worlds are NOT equal -- if they were,
    every assertion above would be a tautology and site 7 would have no
    question to answer."
    (let [cfg (first configurations)
          {:keys [ground-truth entries]} (run-of cfg)
          replayed (fold/replay ground-truth)]
      (is (not= (mapv :world-before entries) (mapv :world-before replayed))
          "run's :world-before carries every patient from t 0")
      (is (> (count (:world-before (first entries)))
             (count (:world-before (first replayed))))
          "and strictly more of them at the first event"))))

(deftest the-corpus-reaches-what-the-law-is-about
  (testing "not vacuous: these logs reach the shapes the assertions are
    about (ADR-0169's own rule)."
    (let [runs (mapv run-of configurations)
          all-entries (mapcat :entries runs)]
      (is (some #(> (count (participants-of (:event %))) 1) all-entries)
          "a multi-participant event -- :bed-swap or :merge")
      (is (some #(seq (:board %)) all-entries)
          "a non-empty occupancy board")
      (is (some #(= :bed-status-change (:event (:event %))) all-entries)
          "a bed participant, which names no patient at all")
      (is (some #(> (count (:world-before %)) 1) all-entries)
          "more than one patient in a world"))))
