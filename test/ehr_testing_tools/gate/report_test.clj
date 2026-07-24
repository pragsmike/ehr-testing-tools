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
    (is (= [{:path "a.json" :verdict :pass :finding-count 0 :findings []}
            {:path "b.json" :verdict :rejected :finding-count 2 :findings [(finding "structure") (finding "structure")]}
            {:path "c.json" :verdict :indeterminate :finding-count 1 :findings [(finding "code-invalid")]}]
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

;; ---- baseline-relative verdicts (P6): motivated by EXP-C5's
;; discovery that a profile-stamped corpus carries pre-existing
;; findings on every file -- file-level verdict alone can't
;; discriminate a genuinely new problem from baseline noise. A finding
;; counts toward rejection only if it isn't already present in the
;; baseline for that file, matched by an exact {severity, code,
;; locator-path} triple. ----

(defn- loc [path] {:format :fhir :path path})

(defn- f [severity code path]
  {:severity severity :code code :locator (loc path) :message "m" :engine {:name "e" :version "1"}})

(def baseline-noisy-file-findings
  ;; A file that's already :rejected in the baseline -- pre-existing
  ;; profile noise, nothing to do with any real regression.
  [(f :error "structure" "meta.profile") (f :error "structure" "meta.profile")])

(def baseline-report
  (report/build-report
   [{:path "noisy.json" :verdict :rejected :findings baseline-noisy-file-findings}
    {:path "clean.json" :verdict :pass :findings []}]
   {}))

(deftest baseline-relative-report-a-file-whose-findings-are-all-in-the-baseline-is-relative-pass-test
  (let [results [{:path "noisy.json" :verdict :rejected :findings baseline-noisy-file-findings}]
        br (report/baseline-relative-report results {} baseline-report)]
    (is (= :rejected (:verdict (first (:files (:absolute br))))))
    (is (= :pass (:verdict (first (:files (:relative br))))))))

(deftest baseline-relative-report-a-genuinely-new-finding-stays-rejected-test
  (let [new-finding (f :error "invalid" "entry[0].resource.gender")
        results [{:path "noisy.json" :verdict :rejected
                   :findings (conj baseline-noisy-file-findings new-finding)}]
        br (report/baseline-relative-report results {} baseline-report)
        relative-file (first (:files (:relative br)))]
    (is (= :rejected (:verdict relative-file)))
    (is (= 1 (:finding-count relative-file))
        "only the novel finding counts -- the two baseline-matched findings are excluded")))

(deftest baseline-relative-report-a-file-not-in-the-baseline-at-all-is-fully-novel-test
  (let [results [{:path "brand-new.json" :verdict :rejected :findings [(f :error "invalid" "x")]}]
        br (report/baseline-relative-report results {} baseline-report)]
    (is (= :rejected (:verdict (first (:files (:relative br))))))))

(deftest baseline-relative-report-matches-on-the-exact-severity-code-locator-triple-test
  ;; Same code and locator, but a different severity -- not a match;
  ;; the finding still counts as novel. Exact-triple matching is a
  ;; documented limitation (docs/gate-calibration.md), not a bug.
  (let [different-severity (f :warning "structure" "meta.profile")
        results [{:path "noisy.json" :verdict :rejected :findings [different-severity]}]
        br (report/baseline-relative-report results {} baseline-report)]
    (is (= :rejected (:verdict (first (:files (:relative br))))))))

(deftest baseline-relative-report-clean-file-with-no-findings-stays-pass-test
  (let [results [{:path "clean.json" :verdict :pass :findings []}]
        br (report/baseline-relative-report results {} baseline-report)]
    (is (= :pass (:verdict (first (:files (:relative br))))))))

(deftest baseline-relative-report-carries-both-absolute-and-relative-sections-test
  (let [results [{:path "noisy.json" :verdict :rejected :findings baseline-noisy-file-findings}]
        br (report/baseline-relative-report results {:gate :fhir} baseline-report)]
    (is (report/valid? (:absolute br)))
    (is (report/valid? (:relative br)))
    (is (= {:gate :fhir} (:run (:absolute br))))))
