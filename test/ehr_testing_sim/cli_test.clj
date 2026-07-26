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

(deftest check-catches-planted-violation
  (let [bad [{:event :discharge :t 0 :mrn "MRN000001"}
             {:event :admission :t 5 :mrn "MRN000001" :location "Renal"}]
        r (check/check-all bad)]
    (is (result/rejected? r))
    (is (= :invariant-violation (:category r)))
    (is (some #(= :discharge-follows-admission (:invariant %))
              (:violations (:payload r))))))
