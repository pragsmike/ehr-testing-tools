(ns ehrt.sim-check.describe-test
  "ADR-0183: `ehrt.sim-check.describe`, the report of what a corpus
  proves beside what its configuration made possible.

  THE `immunization` ROOT, a third copy. `immunization-producer-config`
  below is `ehrt.oracle.digest/immunization-pair`'s run, which
  `ehrt.sim-engine.engine-test` already copies by hand (sim-engine
  cannot depend on oracle, and neither can this brick). This copy is
  kept honest differently: the golden report pins the log's own
  sha256 in `:identity`, so a drift in any of the three copies that
  changes the log fails the golden here rather than passing quietly."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-check.describe :as describe]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-engine.run :as run]
            [ehrt.patient-simulator.interface :as patient-simulator]))

(def ^:private immunization-producer-config
  (let [module (:payload (patient-simulator/load-module
                          "immunization-fixture"
                          (slurp (io/resource "ehrt/sim/fixtures/immunization-fixture.json"))))
        tdap {:system :cvx :code "115" :display "Tdap vaccine"}
        cohort (range 4)]
    {:seed 20260929 :patients 12 :arrival-gap 90
     :pathways (into (mapv (fn [i] {:patient-ordinal i :pathway {:name "module-only" :steps []}}) cohort)
                     [{:pathway {:name "renal-stay-immunized"
                                 :steps [{:type :admission :location "Renal"}
                                         {:type :immunization :codes [tdap] :series 1}
                                         {:type :delay :from 60 :to 60}
                                         {:type :discharge}]}
                       :weight 1}])
     :modules [(patient-simulator/singleton-closure module)]
     :module-assignment (mapv (fn [i] {:patient-ordinal i :module-id "immunization-fixture"}) cohort)
     :module-horizon-days 3650}))

(def ^:private immunization-log
  (delay (:ground-truth (run/run immunization-producer-config))))

(defn- row [report family] (first (filter #(= family (:family %)) (:families report))))

;; --- step 1 (a): determinism ----------------------------------------------

(deftest describing-the-immunization-root-twice-is-byte-identical
  (let [a (describe/describe @immunization-log)
        b (describe/describe (:ground-truth (run/run immunization-producer-config)))]
    (is (= (pr-str a) (pr-str b)) "a fresh run of the same root, described, gives the same bytes")
    (is (= describe/describe-version (:describe-version a) "1.2.0"))))

;; --- step 1 (b): the golden --------------------------------------------------

(deftest the-immunization-root-matches-its-committed-golden-report
  (let [golden (edn/read-string (slurp (io/resource "ehrt/sim_check/fixtures/describe-golden-immunization.edn")))]
    (is (= golden (describe/describe @immunization-log))
        "regenerate only by deliberate edit: (clojure.pprint/pprint (describe/describe log))")))

;; --- step 1 (c): configured, and nothing to show for it ----------------------

(deftest a-configured-family-with-no-witness-reports-zero-and-an-empty-list
  (testing "a churn type the configuration enables at a rate three patients cannot realize"
    (let [profile (merge churn/default-churn-profile {:merge 1.0E-9})
          log (:ground-truth (run/run {:seed 1 :patients 3 :churn-profile profile}))
          r (describe/describe log {:configuration {:source :manifest-invocation
                                                    :opts {:seed 1 :patients 3 :churn-profile {:merge 1.0E-9}}
                                                    :churn-profile profile}})]
      (is (= {:configured :yes :observed 0 :witnesses []}
             (select-keys (row r :merge) [:configured :observed :witnesses])))
      (is (= :no (:configured (row r :bed-swap))) "a rate of zero is :no, not :unknown"))))

;; --- step 1 (d): a bare log names no configuration ----------------------------

(deftest a-bare-log-reports-its-own-identity-and-every-configured-column-unknown
  (let [r (describe/describe @immunization-log)]
    (is (= :bare-log (get-in r [:identity :source])))
    (is (= :none (get-in r [:identity :configured-from])))
    (is (= 52 (get-in r [:identity :events])))
    (is (re-matches #"[0-9a-f]{64}" (get-in r [:identity :log-sha256])))
    (doseq [{:keys [group family configured]} (:families r)]
      (if (= :measure group)
        (is (= :emergent configured) (str family ": a measure is not a configuration question"))
        (is (= :unknown configured) (str family " is :unknown, never a guess"))))
    (is (not-any? #(contains? (:temporal r) %) [:min-iso :max-iso])
        "no reference date in a bare log, so no ISO instant")))

;; --- step 1 (e): what the log cannot prove -----------------------------------

(deftest a-pathway-family-is-configured-and-unprovable-with-the-reason
  (let [opts (select-keys immunization-producer-config [:pathways])
        r (describe/describe @immunization-log {:configuration {:source :caller-config :opts opts
                                                                :churn-profile :unknown}})
        p (row r "renal-stay-immunized")]
    (is (= :pathway (:group p)))
    (is (= :yes (:configured p)))
    (is (= :unprovable (:observed p)))
    (is (string? (:observed-reason p)))
    (testing "the churn flag a caller's config does not record stays :unknown"
      (is (= :unknown (:configured (row r :merge)))))
    (testing "an authored :immunization step is enough to say it was configured"
      (is (= :yes (:configured (row r :immunization)))))
    (testing "ADR-0184's row beside it: no authored :allergy-onset step and no
              modules in these opts, so nothing can produce one -- :no, and
              the log agrees"
      (is (= [:no 0] ((juxt :configured :observed) (row r :allergy-onset)))))))

(deftest an-authored-allergy-onset-step-is-configured-and-observed
  (let [peanut {:system :snomed :code "762952008" :display "Peanut (substance)"}
        opts {:seed 3 :patients 2
              :pathways [{:pathway {:name "allergy-visit"
                                    :steps [{:type :outpatient-visit}
                                            {:type :allergy-onset :codes [peanut] :category "food"}
                                            {:type :outpatient-visit-end}]}
                          :weight 1}]}
        log (:ground-truth (run/run opts))
        r (describe/describe log {:configuration {:source :caller-config :opts opts :churn-profile :unknown}})
        a (row r :allergy-onset)]
    (is (= [:yes 2] ((juxt :configured :observed) a)))
    (is (= {:category "food"} (:related (first (:witnesses a)))) "the witness field is :category")
    (testing "the predicate's :fails is the invariant's own count, as for every row"
      (is (empty? (check/clinical-content-only-when-admitted log))))))

(deftest a-cited-module-the-configuration-does-not-list-is-unknown-not-no
  (testing "an authored step's :citation, or a listed module's submodule, both cite a name :modules never lists"
    (let [r (describe/describe @immunization-log
                               {:configuration {:source :caller-config :opts {:modules ["some-other-module"]}
                                                :churn-profile :unknown}})
          cited (row r "immunization-fixture")
          listed (row r "some-other-module")]
      (is (= :unknown (:configured cited)))
      (is (string? (:configured-reason cited)))
      (is (= 16 (:observed cited)))
      (is (= {:configured :yes :observed 0 :subjects 0 :witnesses []}
             (select-keys listed [:configured :observed :subjects :witnesses]))))))

;; --- step 2: witnesses ----------------------------------------------------------

(deftest witnesses-are-the-first-k-by-log-index-and-point-at-the-event
  (let [log @immunization-log
        r (describe/describe log {:witnesses 2})
        ws (:witnesses (row r :immunization))]
    (is (= 2 (count ws)))
    (is (apply < (map :index ws)))
    (doseq [{:keys [index t patient-id]} ws]
      (is (= :immunization (:event (nth log index))))
      (is (= t (:t (nth log index))))
      (is (= patient-id (:patient-id (first (:participants (nth log index)))))))
    (is (= 16 (:observed (row r :immunization))) "the count is not capped by k")
    (is (= [] (:witnesses (row (describe/describe log {:witnesses 0}) :immunization))))))

;; --- step 4: every predicate counts what its invariant counts ----------------

(def ^:private churny-config
  {:seed 20260929 :patients 40 :arrival-gap 120 :encounters true
   ;; room for all forty, so the run is not halted by exhaustion
   :facility {:id :describe-test
              :wards [{:id :renal :name "Renal" :beds 30 :surge-slots 10
                       :surge-format "%s-H%02d" :class :inpatient}]}
   :churn-profile churn/sample-profile
   :pathways [{:pathway {:name "renal-workup"
                         :steps [{:type :admission :location "Renal"}
                                 {:type :order :profile :cbc}
                                 {:type :delay :from 180 :to 900}
                                 {:type :procedure :codes [{:system :snomed :code "1" :display "x"}]}
                                 {:type :discharge}]}
               :weight 1}]})

(defn- violations-of [log invariant]
  (count (filter #(= invariant (:invariant %))
                 (get-in (check/check-all log) [:payload :violations]))))

(defn- mutated
  "The churny log with one defect per predicate, each placed so no log
  index moves: two events APPENDED at the end (a second discharge, a
  procedure after it), and one result re-pointed in place at a later
  order of a DIFFERENT patient -- which breaks both of the result's
  relationships at once."
  [log]
  (let [log (vec log)
        last-t (:t (peek log))
        discharge (last (filter #(= :discharge (:event %)) log))
        results (keep-indexed (fn [i e] (when (= :result-available (:event e)) i)) log)
        r-idx (first results)
        subject (:patient-id (first (:participants (nth log r-idx))))
        other-order (last (keep-indexed (fn [i e] (when (and (= :order-placed (:event e))
                                                             (not= subject (:patient-id (first (:participants e)))))
                                                    i))
                                        log))]
    (-> log
        (assoc-in [r-idx :order-event-id] other-order)
        (conj (assoc discharge :t last-t))
        (conj {:event :procedure :t last-t :active-mrn (:active-mrn discharge)
               :codes [{:system :snomed :code "1" :display "x"}]
               :participants (:participants discharge)}))))

(deftest predicate-fails-equal-the-invariants-violations-on-the-same-log
  (let [clean (:ground-truth (run/run churny-config))]
    (doseq [[label log] [["clean" clean] ["mutated" (mutated clean)]]]
      (testing label
        (let [p (:predicates (describe/describe log))
              result-fail-indices (fn [pred] (set (map :index (get-in p [pred :witnesses :fails]))))]
          (is (= (violations-of log :discharge-closes-an-open-encounter)
                 (get-in p [:same-subject-opener-closer :fails])))
          (is (= (+ (violations-of log :order-only-when-admitted)
                    (violations-of log :clinical-content-only-when-admitted))
                 (get-in p [:during-encounter :fails])))
          ;; one invariant carries both halves: a result failing EITHER is
          ;; one violation, so it is the union the invariant counts
          (is (= (violations-of log :result-references-existing-order-and-follows-it-in-time)
                 (count (into (result-fail-indices :order-before-result)
                              (result-fail-indices :result-belongs-to-order)))))
          (is (pos? (get-in p [:same-subject-opener-closer :holds])) "the population is not vacuous")
          (is (pos? (get-in p [:result-belongs-to-order :holds])))
          (is (pos? (get-in p [:during-encounter :holds]))))))
    (testing "each mutation is seen, so the equalities above are not 0 = 0"
      (let [p (:predicates (describe/describe (mutated clean)))]
        (is (pos? (get-in p [:same-subject-opener-closer :fails])))
        (is (pos? (get-in p [:during-encounter :fails])))
        (is (pos? (get-in p [:result-belongs-to-order :fails])))
        (is (pos? (get-in p [:order-before-result :fails])))))))

(deftest a-caller-handing-replay-records-gets-the-same-report
  (let [log @immunization-log]
    (is (= (describe/describe log)
           (describe/describe log {:records (engine/replay log)})))))

;; --- slice 2: the manifest's assignment record ------------------------------

(defn- with-record
  "The immunization root described as its envelope would be: the run's
  own options as the configuration, and the engine's assignment record
  in the manifest."
  []
  (let [{:keys [ground-truth assignments]} (run/run immunization-producer-config)
        opts (select-keys immunization-producer-config [:patients :pathways :module-assignment])]
    (describe/describe ground-truth
                       {:manifest {:assignments assignments}
                        :configuration {:source :manifest-invocation
                                        :opts (assoc opts :modules ["immunization-fixture"])
                                        :churn-profile nil}})))

(deftest a-pathway-row-is-observed-from-the-assignment-record
  (let [r (with-record)]
    (is (= {:configured :yes :observed 4 :subjects 4}
           (select-keys (row r "module-only") [:configured :observed :subjects]))
        "four ordinals assigned; each walked its module, so each has an event past :registered")
    (is (= {:configured :yes :observed 8 :subjects 8}
           (select-keys (row r "renal-stay-immunized") [:configured :observed :subjects])))
    (let [ws (:witnesses (row r "renal-stay-immunized"))]
      (is (= 3 (count ws)))
      (is (apply < (map :index ws)) "first-k, by each patient's first event past :registered")
      (is (every? #(not= :registered (:event (nth @immunization-log (:index %)))) ws)))))

(deftest a-module-row-is-observed-from-the-assignment-record
  (let [m (row (with-record) "immunization-fixture")]
    (is (= {:configured :yes :assigned 4 :assigned-subjects 4}
           (select-keys m [:configured :assigned :assigned-subjects]))
        "four ordinals the record assigned the module, and all four cite it in the log")))

(deftest without-the-record-pathways-stay-unprovable-and-modules-carry-no-assignment
  (let [r (describe/describe @immunization-log
                             {:configuration {:source :caller-config
                                              :opts (select-keys immunization-producer-config [:pathways])
                                              :churn-profile :unknown}})]
    (is (= :unprovable (:observed (row r "module-only"))))
    (is (not-any? #(contains? % :assigned) (:families r)))))

(deftest the-text-view-renders-the-same-map
  (let [r (describe/describe @immunization-log)
        text (describe/render-text r)]
    (is (.contains ^String text (get-in r [:identity :log-sha256])))
    (is (.contains ^String text "immunization"))))
