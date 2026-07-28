(ns ehrt.tools.judge.report-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [ehrt.tools.judge.report :as report]))

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
    (is (= {:pass 1 :rejected 1 :indeterminate 1 :no-verdict 0} (:totals r)))))

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
    (is (= {:pass 0 :rejected 0 :indeterminate 0 :no-verdict 0} (:totals r)))
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
  ;; documented limitation (docs/judge-calibration.md), not a bug.
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

;; ---- no-verdict flows through the report (ADR-0010): totals,
;; per-file :cause, schema, and diff ----

(defn- no-verdict-finding [code]
  {:severity :warning :code code :locator {:format :fhir :path "x"}
   :message "m" :engine {:name "e" :version "1"}
   :disposition :no-verdict :cause :terminology-suppressed})

(deftest build-report-totals-include-no-verdict-count-test
  (let [results [{:path "a.json" :verdict :pass :findings []}
                 {:path "b.json" :verdict :no-verdict :cause :terminology-suppressed
                  :findings [(no-verdict-finding "code-invalid")]}]
        r (report/build-report results {})]
    (is (= {:pass 1 :rejected 0 :indeterminate 0 :no-verdict 1} (:totals r)))))

(deftest build-report-file-entry-carries-cause-when-no-verdict-test
  (let [results [{:path "b.json" :verdict :no-verdict :cause :terminology-suppressed
                  :findings [(no-verdict-finding "code-invalid")]}]
        r (report/build-report results {})]
    (is (= :terminology-suppressed (:cause (first (:files r)))))))

(deftest build-report-file-entry-has-no-cause-key-for-a-non-no-verdict-file-test
  (let [results [{:path "a.json" :verdict :pass :findings []}]
        r (report/build-report results {})]
    (is (not (contains? (first (:files r)) :cause)))))

(deftest build-report-with-no-verdict-validates-against-schema-and-round-trips-test
  (let [results [{:path "b.json" :verdict :no-verdict :cause :terminology-suppressed
                  :findings [(no-verdict-finding "code-invalid")]}]
        r (report/build-report results {})]
    (is (report/valid? r))
    (is (= r (edn/read-string (pr-str r))) "round-trips through EDN unchanged")))

;; ---- :no-verdict-causes (post-close-out retrofit): worst-of's
;; projection lets a :rejected finding dominate the file-level verdict
;; over an incidental :no-verdict finding in the same file -- exactly
;; the coverage dimension the projection discards. This per-file
;; cause-count surfaces it back, for :rejected files too, not just
;; :no-verdict ones. Additive: absent when no finding carries a
;; :cause, so pre-existing fixtures and old (pre-ADR-0010) baselines
;; are unaffected. ----

(defn- rejected-finding [code]
  {:severity :error :code code :locator {:format :fhir :path "y"}
   :message "m" :engine {:name "e" :version "1"} :disposition :rejected})

(deftest build-report-file-entry-carries-no-verdict-causes-when-no-verdict-wins-test
  (let [results [{:path "b.json" :verdict :no-verdict :cause :terminology-suppressed
                  :findings [(no-verdict-finding "code-invalid")]}]
        r (report/build-report results {})]
    (is (= {:terminology-suppressed 1} (:no-verdict-causes (first (:files r)))))))

(deftest build-report-file-entry-carries-no-verdict-causes-even-when-rejected-wins-test
  ;; The file's own :verdict is :rejected (a confirmed violation
  ;; dominates the aggregate, per the revised worst-of ranking) -- but
  ;; this file ALSO contains a no-verdict-worthy finding, and that
  ;; partiality must not vanish just because :rejected won the fold.
  (let [results [{:path "a.json" :verdict :rejected
                  :findings [(rejected-finding "structure")
                             (no-verdict-finding "code-invalid")
                             (no-verdict-finding "code-invalid")]}]
        r (report/build-report results {})]
    (is (= :rejected (:verdict (first (:files r)))))
    (is (= {:terminology-suppressed 2} (:no-verdict-causes (first (:files r)))))))

(deftest build-report-file-entry-omits-no-verdict-causes-when-no-finding-carries-a-cause-test
  (let [results [{:path "a.json" :verdict :pass :findings []}
                 {:path "b.json" :verdict :rejected :findings [(rejected-finding "structure")]}]
        r (report/build-report results {})]
    (is (not (contains? (first (:files r)) :no-verdict-causes)))
    (is (not (contains? (second (:files r)) :no-verdict-causes)))))

(deftest build-report-with-no-verdict-causes-validates-against-schema-and-round-trips-test
  (let [results [{:path "a.json" :verdict :rejected
                  :findings [(rejected-finding "structure") (no-verdict-finding "code-invalid")]}]
        r (report/build-report results {})]
    (is (report/valid? r))
    (is (= r (edn/read-string (pr-str r))) "round-trips through EDN unchanged")))

(deftest diff-reports-surfaces-a-change-to-no-verdict-test
  (let [before (report/build-report [{:path "a.json" :verdict :pass :findings []}] {})
        after (report/build-report [{:path "a.json" :verdict :no-verdict :cause :terminology-suppressed
                                      :findings [(no-verdict-finding "code-invalid")]}] {})
        d (report/diff-reports before after)]
    (is (= [{:path "a.json" :from :pass :to :no-verdict}] (:changed-verdicts d)))))

(deftest diff-reports-surfaces-a-change-from-no-verdict-test
  (let [before (report/build-report [{:path "a.json" :verdict :no-verdict :cause :terminology-suppressed
                                       :findings [(no-verdict-finding "code-invalid")]}] {})
        after (report/build-report [{:path "a.json" :verdict :rejected :findings [(finding "structure")]}] {})
        d (report/diff-reports before after)]
    (is (= [{:path "a.json" :from :no-verdict :to :rejected}] (:changed-verdicts d)))))

;; ---- baseline-relative reads a pre-split (three-valued) baseline
;; forward, without migration -- old baselines predate :no-verdict and
;; :cause entirely (docs/judge-calibration.md, ADR-0010) ----

(def pre-split-baseline
  (edn/read-string (slurp "test/fixtures/reports/pre-split-baseline.edn")))

(deftest baseline-relative-report-reads-a-pre-split-three-valued-baseline-test
  (let [results [{:path "suppressed.json" :verdict :no-verdict :cause :terminology-suppressed
                  :findings [{:severity :warning :code "code-invalid"
                              :locator {:format :fhir :path "value.coding"}
                              :message "still suppressed" :engine {:name "e" :version "1"}
                              :disposition :no-verdict :cause :terminology-suppressed}]}]
        br (report/baseline-relative-report results {} pre-split-baseline)]
    (is (= :no-verdict (:verdict (first (:files (:absolute br))))))
    (is (= :terminology-suppressed (:cause (first (:files (:absolute br))))))
    (is (= :pass (:verdict (first (:files (:relative br)))))
        "the finding's {severity code locator-path} triple already matches the pre-split baseline -- nothing novel")))

(deftest baseline-relative-report-a-novel-no-verdict-worthy-finding-against-a-pre-split-baseline-stays-relative-rejected-test
  (let [results [{:path "suppressed.json" :verdict :no-verdict :cause :terminology-suppressed
                  :findings [{:severity :warning :code "code-invalid"
                              :locator {:format :fhir :path "value.coding"}
                              :message "still suppressed" :engine {:name "e" :version "1"}
                              :disposition :no-verdict :cause :terminology-suppressed}
                             {:severity :warning :code "brand-new-code"
                              :locator {:format :fhir :path "brand.new.path"}
                              :message "a genuinely new suppressed finding" :engine {:name "e" :version "1"}
                              :disposition :no-verdict :cause :terminology-suppressed}]}]
        br (report/baseline-relative-report results {} pre-split-baseline)]
    (is (= :rejected (:verdict (first (:files (:relative br)))))
        "a novel no-verdict-worthy finding still counts as relative :rejected -- format-agnostic, not preserved (docs/judge-calibration.md)")))
