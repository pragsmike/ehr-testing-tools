(ns ehrt.sim-engine.event-schema-test
  "The event contract's own gate (event-log contract arc, Step 2;
  `.agents/plans/2026-08-16-event-log-census.md` is the census it was
  derived from).

  THE COVERAGE ASSERTION IS THE POINT. A validity test alone passes
  vacuously for any kind the runs never happen to produce -- which is
  exactly how a schema silently rots. So `every-declared-kind-is-
  actually-produced` asserts that the fixture fleet's own union of
  observed kinds equals the schema's declared vocabulary EXACTLY, in
  both directions: a kind declared but never produced fails just as
  loudly as a kind produced but never declared. That bidirectional
  assertion is what turned red when the schema was first landed one
  kind short, deliberately, to witness it.

  WHY FIXTURES AND NOT THE DEMO CORPORA. The census needed 400-patient,
  ten-year module runs to reach `:medication-end` and
  `:diagnostic-report`, and five churn seeds to reach `:cancel-admit`
  and `:step-rejected` -- minutes of work, per-push-hostile, and
  seed-fragile besides (a scenario retune could silently stop producing
  a kind and this gate would quietly stop checking it). The fleet below
  reaches all 21 kinds DETERMINISTICALLY: the churn family is authored
  as explicit IR steps rather than hunted for across seeds (the same
  technique `ehrt.sim-engine.churn-scenarios-test` already uses), and
  the clinical family comes from one in-test GMF fixture module that
  walks the whole state vocabulary in a single encounter.

  NO PRODUCTION VALIDATION. Nothing here is wired into the engine or
  either emitter. The contract costs zero runtime."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.set :as set]
            [clojure.string :as str]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-engine.event-schema :as es]
            [ehrt.sim-trajectory.interface :as sim-trajectory]
            [malli.core :as m]))

;; --- fixtures -------------------------------------------------------------

(def ^:private crowded-facility
  "Two Renal rungs and an ED overflow -- enough contention for a
  bed-swap and a boarding merge, small enough to stay fast."
  {:id :schema-fixture
   :wards [{:id :ed :name "ED" :beds 0 :surge-slots 4 :surge-format "%s-H%02d" :class :ed}
           {:id :renal :name "Renal" :beds 1 :surge-slots 1 :surge-format "%s-H%02d" :class :inpatient}]})

(def ^:private clinical-fixture-module
  "One ambulatory encounter that walks the entire clinical state
  vocabulary, so a single small run produces every clinically-sourced
  event kind. Authored here rather than borrowed from a vendored root
  because a real module reaches these states only probabilistically --
  the census needed 400 patients over ten simulated years to see one
  `:medication-end`.

  The medication and care-plan pairs both close INSIDE the encounter,
  by explicit `:medication-order` / `:careplan` state citation, which
  is what makes `:medication-end` and `:care-plan-end` land as horizon
  events with RESOLVED back-references. That matters beyond coverage:
  the census observed all seven `:care-plan-end` events with a nil
  `:care-plan-citation`, and this fixture is the control proving the
  mechanism itself works -- the nils came from vendored modules using
  `referenced_by_attribute`, whose resolution `ehrt.sim-trajectory.gmf`
  deliberately never declared for the CarePlan family (census S-2).

  THE LEAD-IN DELAY IS LOAD-BEARING, and was found by running rather
  than reasoned about. The GMF walk starts at the patient's DOB, and
  everything before the run's registration instant is history --
  dropped, or folded into `:pre-horizon-facts`. A newborn's DOB is
  still sampled up to a year before today, so an encounter authored at
  t=0 lands in that history window and produces nothing at all. 400
  days clears it for every DOB the persona sampler can draw."
  {:id "schema-fixture-mod" :name "Schema Fixture"
   :states {:initial {:type :initial :direct-transition :lead-in}
            :lead-in {:type :delay :exact {:quantity 400 :unit "days"}
                      :direct-transition :visit}
            :visit {:type :encounter :encounter-class :ambulatory
                    :codes [{:system :snomed :code "185345009" :display "Encounter for symptom"}]
                    :direct-transition :the-procedure}
            :the-procedure {:type :procedure
                            :codes [{:system :snomed :code "80146002" :display "Excision of appendix"}]
                            :direct-transition :the-observation}
            :the-observation {:type :observation :category "vital-signs" :unit "Cel"
                              :codes [{:system :loinc :code "8310-5" :display "Body temperature"}]
                              :range {:low 37.5 :high 38.0}
                              :direct-transition :the-panel}
            :the-panel {:type :diagnostic-report
                        :codes [{:system :loinc :code "58410-2" :display "CBC panel"}]
                        :observations [{:category "laboratory" :unit "K/uL"
                                        :codes [{:system :loinc :code "6690-2" :display "Leukocytes"}]
                                        :range {:low 4.5 :high 11.0}}
                                       {:category "laboratory"
                                        :codes [{:system :snomed :code "10828004" :display "Positive"}]
                                        :value-code {:system :snomed :code "10828004" :display "Positive"}}]
                        :direct-transition :the-med}
            :the-med {:type :medication-order
                      :codes [{:system :rxnorm :code "308182" :display "Amoxicillin 250 MG"}]
                      :direct-transition :the-plan}
            :the-plan {:type :care-plan-start
                       :codes [{:system :snomed :code "324911001" :display "Antibiotic therapy"}]
                       :activities [{:system :snomed :code "710824005" :display "Assessment of health"}]
                       :direct-transition :course}
            :course {:type :delay :exact {:quantity 10 :unit "days"} :direct-transition :stop-med}
            :stop-med {:type :medication-end :medication-order :the-med :direct-transition :stop-plan}
            :stop-plan {:type :care-plan-end :careplan :the-plan :direct-transition :visit-end}
            :visit-end {:type :encounter-end :direct-transition :done}
            :done {:type :terminal}}})

;; --- the fleet ------------------------------------------------------------
;;
;; Each entry is [label run-result]. Together their kinds must equal the
;; schema's own declared vocabulary, exactly.

(defn- clinical-run
  []
  (engine/run {:seed 11 :patients 2 :arrival-gap 0
               :pathway {:name "module-only" :steps []}
               ;; Newborns, so the walk's own clock starts near the run's
               ;; registration instant -- see the fixture's lead-in note.
               :persona-config {:age-min 0 :age-max 0}
               :modules [(sim-trajectory/singleton-closure clinical-fixture-module)]
               :module-assignment [{:module-id "schema-fixture-mod" :weight 1}]
               :module-horizon-days 1200}))

(defn- operational-run
  "The order/result pair plus the plain admit-transfer-discharge arc."
  []
  (engine/run {:seed 12 :patients 2 :arrival-gap 0
               :facility crowded-facility
               :pathways [{:patient-ordinal 0
                           :pathway {:name "worked-up"
                                     :steps [{:type :admission :location "ED" :reason "Chest pain"}
                                             {:type :order :profile :cbc}
                                             {:type :delay :from 60 :to 60}
                                             {:type :transfer :location "Renal"}
                                             {:type :discharge}]}}
                          {:patient-ordinal 1
                           :pathway {:name "quick" :steps [{:type :admission :location "ED"}
                                                           {:type :discharge}]}}]}))

(defn- churn-run
  "The churn family, authored as explicit IR rather than hunted across
  seeds -- `:step-rejected` comes from a cancel-admit attempted by a
  patient who was never admitted (`:illegal-cancel-admit`), which is a
  legal log, not a bug."
  []
  (let [seed 13
        p1 (engine/patient-id-for seed 1)]
    (engine/run {:seed seed :patients 4 :arrival-gap 0
                 :facility crowded-facility
                 :pathways [{:patient-ordinal 0
                             :pathway {:name "cancels"
                                       :steps [{:type :admission :location "Renal"}
                                               {:type :transfer-in-error :location "ED"}
                                               {:type :discharge}
                                               {:type :cancel-discharge}
                                               {:type :bed-swap :with p1}
                                               {:type :merge :with p1}]}}
                            {:patient-ordinal 1
                             :pathway {:name "peer" :steps [{:type :admission :location "Renal"}]}}
                            {:patient-ordinal 2
                             :pathway {:name "admit-then-unadmit"
                                       :steps [{:type :admission :location "ED"}
                                               {:type :cancel-admit}]}}
                            ;; Never admitted: the cancel-admit below is
                            ;; rejected, which is exactly the point.
                            {:patient-ordinal 3
                             :pathway {:name "illegal" :steps [{:type :cancel-admit}]}}]})))

(defn- death-run
  "The `:disposition :expired` discharge -- 1 event in the census's own
  4,997, and 0 in anything the docs teach, so it gets its own fixture
  rather than being left to chance."
  []
  (engine/run {:seed 14 :patients 1 :arrival-gap 0
               :facility crowded-facility
               :pathways [{:patient-ordinal 0
                           :pathway {:name "expires"
                                     :steps [{:type :admission :location "Renal"}
                                             {:type :discharge :disposition :expired
                                              :codes [{:system :snomed :code "410429000"
                                                       :display "Cardiac arrest"}]}]}}]}))

(defn- fleet
  []
  [["clinical" (clinical-run)]
   ["operational" (operational-run)]
   ["churn" (churn-run)]
   ["death" (death-run)]])

(defn- declared-kinds
  []
  (into #{} (map first) (rest (rest es/Event))))

;; --- the gate -------------------------------------------------------------

(deftest every-declared-kind-is-actually-produced
  (testing "the fleet's own kinds equal the schema's declared
            vocabulary EXACTLY -- without this, a validity test passes
            vacuously for any kind nothing produces, which is how a
            schema rots in place"
    (let [produced (into #{} (mapcat (fn [[_ r]] (map :event (:ground-truth r)))) (fleet))
          declared (declared-kinds)]
      (is (empty? (set/difference declared produced))
          (str "declared but never produced by the fixture fleet: "
               (sort (set/difference declared produced))))
      (is (empty? (set/difference produced declared))
          (str "produced but not declared in the Event schema: "
               (sort (set/difference produced declared))))
      (is (= 21 (count declared))
          "the census reconciled source and corpora at exactly 21 kinds"))))

(deftest every-event-of-every-fixture-run-validates
  (doseq [[label {:keys [ground-truth]}] (fleet)]
    (testing label
      (is (seq ground-truth) "fixture produced no events at all")
      (doseq [event ground-truth]
        (is (es/valid-event? event)
            (str label " / " (:event event) " at t=" (:t event) ": "
                 (pr-str (m/explain es/Event event))))))))

(deftest t-is-monotone-within-every-fixture-run
  (testing "the RUN-level property the schema deliberately does not
            express per event"
    (doseq [[label {:keys [ground-truth]}] (fleet)]
      (is (es/run-t-monotone? ground-truth) label))))

(deftest run-t-monotone-holds-degenerately-for-short-logs
  (is (es/run-t-monotone? []))
  (is (es/run-t-monotone? [{:t 5}])))

(defspec every-event-of-every-generated-run-validates 100
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 6)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients})]
      (every? es/valid-event? ground-truth))))

(defspec every-event-of-every-generated-churn-run-validates 100
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 2 6)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients
                                              :facility crowded-facility
                                              :churn true})]
      ;; A capacity-exhausted run returns no :ground-truth; vacuously
      ;; true, and not this property's business.
      (every? es/valid-event? ground-truth))))

(defspec generated-runs-are-t-monotone 100
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 6)]
    (es/run-t-monotone? (:ground-truth (engine/run {:seed seed :patients patients})))))

;; --- Q-B (a): the EDN export, and Q-A (a): the stability gate -------------

(deftest committed-export-matches-the-source
  (testing "the published EDN contract can never lag the Clojure source
            -- `make docsgen` regenerates it, CI diffs it, this asserts
            it"
    (is (= (es/export) (es/committed-export))
        "run `make event-schema-export`")))

(deftest export-carries-the-declared-version
  (is (= es/schema-version (:event-schema-version (es/export)))))

(deftest non-additive-change-requires-a-version-bump
  (testing "author ruling Q-A (a): the contract is public and
            versioned, and the promise is enforced rather than
            asserted. The BASELINE is frozen at the last versioned
            contract, which is the only reason this comparison can
            ever be non-empty."
    (let [baseline (es/committed-baseline)
          live (es/export)]
      (if (= (:event-schema-version baseline) es/schema-version)
        (let [{:keys [additive? breaking]} (es/classify-change baseline live)]
          (is additive?
              (str "schema-version is still " es/schema-version
                   " but the schema changed non-additively since the frozen"
                   " baseline:\n  "
                   (str/join "\n  " breaking)
                   "\n\nEither make the change additive, or bump"
                   " ehrt.sim-engine.event-schema/schema-version and re-freeze"
                   " with `make event-schema-freeze`.")))
        (is (= baseline live)
            (str "schema-version was bumped to " es/schema-version
                 " but the baseline is still at "
                 (:event-schema-version baseline)
                 " -- re-freeze it with `make event-schema-freeze`"))))))

;; The gate's own classifier, tested directly: a gate whose verdict
;; nothing checks is a gate nobody can trust.

(def ^:private tiny-baseline
  {:event-schema-version "1.0.0"
   :schema [:multi {:dispatch :event}
            [:a [:map [:event [:= :a]] [:keep :string] [:opt {:optional true} :int]]]
            [:b [:map [:event [:= :b]]]]]})

(defn- reclassify [schema]
  (es/classify-change tiny-baseline {:event-schema-version "1.0.0" :schema schema}))

(deftest classify-change-calls-an-identical-schema-additive
  (is (:additive? (reclassify (:schema tiny-baseline)))))

(deftest classify-change-calls-a-new-kind-additive
  (is (:additive? (reclassify [:multi {:dispatch :event}
                               [:a [:map [:event [:= :a]] [:keep :string] [:opt {:optional true} :int]]]
                               [:b [:map [:event [:= :b]]]]
                               [:c [:map [:event [:= :c]]]]]))))

(deftest classify-change-calls-a-new-optional-key-additive
  (is (:additive? (reclassify [:multi {:dispatch :event}
                               [:a [:map [:event [:= :a]] [:keep :string]
                                    [:opt {:optional true} :int]
                                    [:fresh {:optional true} :string]]]
                               [:b [:map [:event [:= :b]]]]]))))

(deftest classify-change-calls-a-new-required-key-breaking
  (let [{:keys [additive? breaking]}
        (reclassify [:multi {:dispatch :event}
                     [:a [:map [:event [:= :a]] [:keep :string]
                          [:opt {:optional true} :int] [:fresh :string]]]
                     [:b [:map [:event [:= :b]]]]])]
    (is (not additive?))
    (is (some #(re-find #"new REQUIRED key" %) breaking))))

(deftest classify-change-calls-a-removed-key-breaking
  (let [{:keys [additive? breaking]}
        (reclassify [:multi {:dispatch :event}
                     [:a [:map [:event [:= :a]] [:opt {:optional true} :int]]]
                     [:b [:map [:event [:= :b]]]]])]
    (is (not additive?))
    (is (some #(re-find #"key removed: :keep" %) breaking))))

(deftest classify-change-calls-a-removed-kind-breaking
  (let [{:keys [additive? breaking]}
        (reclassify [:multi {:dispatch :event}
                     [:a [:map [:event [:= :a]] [:keep :string] [:opt {:optional true} :int]]]])]
    (is (not additive?))
    (is (some #(re-find #"event kind removed" %) breaking))))

(deftest classify-change-calls-optionality-and-value-changes-breaking
  (testing "optional -> required"
    (let [{:keys [additive? breaking]}
          (reclassify [:multi {:dispatch :event}
                       [:a [:map [:event [:= :a]] [:keep :string] [:opt :int]]]
                       [:b [:map [:event [:= :b]]]]])]
      (is (not additive?))
      (is (some #(re-find #"optional -> required" %) breaking))))
  (testing "a value schema changed -- reported breaking even when it widens"
    (let [{:keys [additive? breaking]}
          (reclassify [:multi {:dispatch :event}
                       [:a [:map [:event [:= :a]] [:keep [:or :string :int]]
                            [:opt {:optional true} :int]]]
                       [:b [:map [:event [:= :b]]]]])]
      (is (not additive?))
      (is (some #(re-find #"value schema changed" %) breaking)))))

(deftest whole-logs-validate-as-ground-truth
  (testing "GroundTruth, not just Event by Event"
    (doseq [[label {:keys [ground-truth]}] (fleet)]
      (is (es/valid-ground-truth? ground-truth) label))))
