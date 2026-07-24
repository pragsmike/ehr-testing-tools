(ns ehr-testing-tools.gate.report-test
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-tools.gate.report :as report]))

(defn- finding [code]
  {:severity :error :code code :locator {:format :fhir :path "x"}
   :message "m" :engine {:name "e" :version "1"}})

(def sample-results
  [{:path "a.json" :verdict :pass :findings []}
   {:path "b.json" :verdict :rejected :findings [(finding "structure") (finding "structure")]}
   {:path "c.json" :verdict :indeterminate :findings [(finding "code-invalid")]}])

;; ---- build-report ----

(deftest build-report-computes-totals-test
  (let [r (report/build-report sample-results {:path "corpus/"})]
    (is (= {:pass 1 :rejected 1 :indeterminate 1} (:totals r)))))

(deftest build-report-computes-by-code-counts-test
  (let [r (report/build-report sample-results {:path "corpus/"})]
    (is (= {"structure" 2 "code-invalid" 1} (:by-code r)))))

(deftest build-report-lists-per-file-summaries-test
  (let [r (report/build-report sample-results {:path "corpus/"})]
    (is (= [{:path "a.json" :verdict :pass :finding-count 0}
            {:path "b.json" :verdict :rejected :finding-count 2}
            {:path "c.json" :verdict :indeterminate :finding-count 1}]
           (:files r)))))

(deftest build-report-carries-run-metadata-test
  (let [r (report/build-report sample-results {:path "corpus/" :gate :fhir})]
    (is (= {:path "corpus/" :gate :fhir} (:run r)))))

(deftest build-report-includes-id-when-present-test
  (let [results [{:path "a.json" :verdict :pass :findings [] :id "abc123"}]
        r (report/build-report results {})]
    (is (= "abc123" (:id (first (:files r)))))))

(deftest build-report-empty-results-is-all-zero-totals-test
  (let [r (report/build-report [] {})]
    (is (= {:pass 0 :rejected 0 :indeterminate 0} (:totals r)))
    (is (= {} (:by-code r)))
    (is (= [] (:files r)))))

(deftest build-report-validates-against-schema-test
  (is (report/valid? (report/build-report sample-results {:path "corpus/"}))))

;; ---- diff-reports: what changed between two runs ----

(def report-a
  (report/build-report
   [{:path "a.json" :verdict :pass :findings []}
    {:path "b.json" :verdict :rejected :findings [(finding "structure")]}
    {:path "d.json" :verdict :pass :findings []}]
   {}))

(def report-b
  (report/build-report
   [{:path "a.json" :verdict :rejected :findings [(finding "code-invalid")]}
    {:path "b.json" :verdict :rejected :findings [(finding "structure")]}
    {:path "e.json" :verdict :pass :findings []}]
   {}))

(deftest diff-reports-finds-changed-verdicts-test
  (let [d (report/diff-reports report-a report-b)]
    (is (= [{:path "a.json" :from :pass :to :rejected}] (:changed-verdicts d)))))

(deftest diff-reports-finds-added-and-removed-files-test
  (let [d (report/diff-reports report-a report-b)]
    (is (= ["e.json"] (:files-added d)))
    (is (= ["d.json"] (:files-removed d)))))

(deftest diff-reports-finds-appeared-and-disappeared-codes-test
  (let [d (report/diff-reports report-a report-b)]
    (is (= ["code-invalid"] (:codes-appeared d)))
    (is (= [] (:codes-disappeared d)))))

(deftest diff-reports-identical-reports-have-no-changes-test
  (let [d (report/diff-reports report-a report-a)]
    (is (= [] (:changed-verdicts d)))
    (is (= [] (:files-added d)))
    (is (= [] (:files-removed d)))
    (is (= [] (:codes-appeared d)))
    (is (= [] (:codes-disappeared d)))))
