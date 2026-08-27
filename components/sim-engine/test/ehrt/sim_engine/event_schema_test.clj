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
  reaches all 23 kinds DETERMINISTICALLY: the churn family is authored
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
            [ehrt.sim-engine.event-fleet :as fleet]
            [malli.core :as m]))

(defn- declared-kinds
  []
  (into #{} (map first) (rest (rest es/Event))))

;; --- the gate -------------------------------------------------------------

(deftest every-declared-kind-is-actually-produced
  (testing "the fleet's own kinds equal the schema's declared
            vocabulary EXACTLY -- without this, a validity test passes
            vacuously for any kind nothing produces, which is how a
            schema rots in place"
    (let [produced (into #{} (mapcat (fn [[_ r]] (map :event (:ground-truth r)))) (fleet/fleet))
          declared (declared-kinds)]
      (is (empty? (set/difference declared produced))
          (str "declared but never produced by the fixture fleet: "
               (sort (set/difference declared produced))))
      (is (empty? (set/difference produced declared))
          (str "produced but not declared in the Event schema: "
               (sort (set/difference produced declared))))
      (is (= 24 (count declared))
          (str "the census reconciled source and corpora at exactly 21 kinds,"
               " contract 1.3.0 (ADR-0173) added the two the person stream mints,"
               " and 1.6.0 (ADR-0174 section 2(c)) added the bed cycle's own one")))))

(deftest every-event-of-every-fixture-run-validates
  (doseq [[label {:keys [ground-truth]}] (fleet/fleet)]
    (testing label
      (is (seq ground-truth) "fixture produced no events at all")
      (doseq [event ground-truth]
        (is (es/valid-event? event)
            (str label " / " (:event event) " at t=" (:t event) ": "
                 (pr-str (m/explain es/Event event))))))))

(deftest t-is-monotone-within-every-fixture-run
  (testing "the RUN-level property the schema deliberately does not
            express per event"
    (doseq [[label {:keys [ground-truth]}] (fleet/fleet)]
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
                                              :facility fleet/crowded-facility
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
    (doseq [[label {:keys [ground-truth]}] (fleet/fleet)]
      (is (es/valid-ground-truth? ground-truth) label))))

;; --- ADR-0150 S-6: ResultEntry's unit key is SINGULAR ----------------------
;; Census S-6: a `:result-available` entry carried `:units` while
;; `:observation` and a `:diagnostic-report`'s children carried `:unit`, for
;; the same concept -- so a consumer writing one unit-handling function for
;; "an observed value" got it right for two shapes of three and silently
;; empty for the third. The EVENT key is renamed; the order-profile ANALYTE
;; config key stays `:units` (user-reachable via `--config`), translated at
;; the one construction site exactly as `evolve` already translated it.

(deftest result-entries-carry-unit-singular
  (let [{:keys [ground-truth]} (engine/run {:seed 1 :patients 1
                                            :pathways [{:pathway {:name "cbc"
                                                                  :steps [{:type :admission :location "Renal"}
                                                                          {:type :order :profile :cbc}
                                                                          {:type :discharge}]}
                                                        :weight 1}]})
        result (first (filter #(= :result-available (:event %)) ground-truth))
        entries (:results result)]
    (is (seq entries) "a population gate asserts its population is non-empty")
    (testing "every entry carries :unit, singular"
      (is (every? #(contains? % :unit) entries)))
    (testing "and none carries the retired plural"
      (is (not-any? #(contains? % :units) entries)))
    (testing "the value is the analyte's own unit string, not a translation loss"
      (is (every? #(string? (:unit %)) entries)))
    (testing "and the whole event still validates against the contract"
      (is (es/valid-event? result) (pr-str (es/explain-event result))))))
