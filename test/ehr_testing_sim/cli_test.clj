(ns ehr-testing-sim.cli-test
  "The embedding contract's behavior: dispatch-action routes verbs to
  capability functions (injectable, so no simulation runs here), the
  Result->exit-code mapping matches the host's, and help-group is
  well-formed for the host's help machinery."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as string]
            [ehr-testing-sim.cli :as cli]
            [ehr-testing-sim.check :as check]
            [ehr-testing-sim.result :as result]))

(deftest dispatch-routes-run
  (let [seen (atom nil)
        r (cli/dispatch-action "run" {:seed 1}
                               {:run-fn (fn [opts] (reset! seen opts) (result/ok :ran))})]
    (is (result/ok? r))
    (is (= {:seed 1} @seen))))

(deftest dispatch-rejects-unknown-verb
  (let [r (cli/dispatch-action "explode" {})]
    (is (result/error? r))
    (is (= :unknown-command (:category r)))
    (is (= ["run" "check"] (:known (:payload r))))))

(deftest run-requires-seed
  (let [r (cli/dispatch-action "run" {})]
    (is (result/error? r))
    (is (= :missing-required-opt (:category r)))))

(deftest exit-code-contract
  (is (= 0 (cli/result->exit-code (result/ok :x))))
  (is (= 1 (cli/result->exit-code (result/rejected :why :x))))
  (is (= 2 (cli/result->exit-code (result/error :why :x)))))

(deftest help-group-shape
  (testing "the shape ehr-testing-tools' help machinery walks"
    (is (string? (:group cli/help-group)))
    (is (string? (:doc cli/help-group)))
    (doseq [{:keys [verb doc flags]} (:verbs cli/help-group)]
      (is (string? verb))
      (is (string? doc))
      (is (vector? flags)))))

(deftest help-group-documents-emit
  (testing "the run verb's flags cover --emit and --reference-date (EmitHL7 wiring)"
    (let [run-verb (first (filter #(= "run" (:verb %)) (:verbs cli/help-group)))
          flag-names (set (map :flag (:flags run-verb)))]
      (is (contains? flag-names "--emit"))
      (is (contains? flag-names "--reference-date")))))

(deftest help-group-documents-churn
  (testing "M2b: the run verb's flags cover --churn (InjectChurn wiring)"
    (let [run-verb (first (filter #(= "run" (:verb %)) (:verbs cli/help-group)))
          flag-names (set (map :flag (:flags run-verb)))]
      (is (contains? flag-names "--churn")))))

(deftest main!-with-churn-flag-runs-and-may-emit-churn-events
  (let [printed (atom []) exited (atom nil)]
    (cli/main! ["run" "--seed" "5" "--patients" "8" "--churn" "--emit" "hl7"]
               {:println-fn #(swap! printed conj %)
                :exit-fn #(reset! exited %)})
    (is (= 0 @exited))
    (let [payload (:payload (read-string (first @printed)))]
      (testing "this specific seed produces at least one churn-family message (A17 bed-swap)"
        (is (some #(string/includes? % "^A17") (:messages payload)))))))

(deftest main!-emits-hl7-messages
  (let [printed (atom []) exited (atom nil)]
    (cli/main! ["run" "--seed" "42" "--patients" "2" "--emit" "hl7"]
               {:println-fn #(swap! printed conj %)
                :exit-fn #(reset! exited %)})
    (is (= 0 @exited))
    (let [payload (:payload (read-string (first @printed)))]
      (is (= 4 (count (:messages payload))))
      (is (every? #(string/starts-with? % "MSH|") (:messages payload))))))

(deftest main!-prints-and-exits
  (let [printed (atom []) exited (atom nil)]
    (cli/main! ["run" "--seed" "3"]
               {:println-fn #(swap! printed conj %)
                :exit-fn #(reset! exited %)})
    (is (= 0 @exited))
    (is (= 1 (count @printed)))
    ;; canonical output is readable EDN
    (is (map? (read-string (first @printed))))))

;; --- M4 Task 0: the wiring fix, proven at the CLI-embedding level ---------

(deftest dispatch-action-run-with-pathways-reaches-the-engine-and-emits-orm-oru
  (testing "M3's :pathways (and, through it, an authored :order step)
            reaches the engine THROUGH dispatch-action -- the same
            entrypoint the standalone shell and a mounting host both
            go through -- proving the completeness fix, not just
            run-command's own unit test"
    (let [pathway {:name "cbc-order" :steps [{:type :admission :location "Renal"}
                                             {:type :order :profile :cbc}
                                             {:type :discharge}]}
          r (cli/dispatch-action "run" {:seed 7 :patients 1 :emit "hl7"
                                        :pathways [{:pathway pathway :weight 1}]})]
      (is (result/ok? r))
      (let [messages (:messages (:payload r))]
        (is (some #(string/includes? % "^O01") messages) "ORM^O01 (order-placed) present")
        (is (some #(string/includes? % "^R01") messages) "ORU^R01 (result-available) present")))))

(deftest check-catches-planted-violation
  (let [bad [{:event :discharge :t 0 :participants [{:patient-id "P1" :role :subject}]}
             {:event :admission :t 5 :participants [{:patient-id "P1" :role :subject}] :location "Renal"}]
        r (check/check-all bad)]
    (is (result/rejected? r))
    (is (= :invariant-violation (:category r)))
    (is (some #(= :discharge-follows-admission (:invariant %))
              (:violations (:payload r))))))
