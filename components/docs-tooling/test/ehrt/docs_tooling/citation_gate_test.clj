(ns ehrt.docs-tooling.citation-gate-test
  "ADR-0129: every `docs/manual/0*.md` \"Strip source citations\" table
  entry (that names a citable doc path -- see
  `ehrt.docs-tooling.citation-gate`'s own docstring) must resolve to a
  row in the exercised-sources registry. Red witnessed (scratch,
  pasted into the session record) against a simulated pre-session
  register holding only the two rows that existed before this
  session's own additions -- four of dimension 1's own five gaps
  (both `docs/use-cases/*.md` citations in Chapter 7, both in
  Chapter 8) surfaced this way; the fifth (Chapter 6's own README.md
  \"What you get\" citation) is source-covered by mere row-count even
  pre-session (README.md had exactly one row then, same as now minus
  the new readme-what-you-get row) and needs `:section`-aware
  disambiguation to catch correctly -- proven on a synthetic fixture
  below (`covered-does-not-collapse-two-different-readme-sections-
  test`), not by the live-tree red witness alone, which this test's
  own committed-manual-is-fully-covered-test proves stays green on
  the real, final register."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.docs-tooling.citation-gate :as cg]
            [ehrt.docs-tooling.exercised-sources :as reg]))

;; ---- the real, committed manual against the real, live register --
;; the gate this session exists to land ----

(deftest committed-manual-is-fully-covered-test
  (let [rows (reg/load-registry)
        citations (cg/manual-citations)
        violations (cg/uncovered citations rows)]
    (is (pos? (count citations)) "sanity: the extractor must find real citations, not silently return none")
    (is (empty? violations) (str "uncovered citations:\n" (clojure.string/join "\n" (map cg/violation-message violations))))))

;; ---- mechanism sanity: the table extractor must actually find the
;; real tables (the earlier state-machine bug this namespace's own
;; docstring names returned zero rows from five real chapters silently
;; -- catching a silent-empty-result bug needs an explicit count
;; assertion, not just "no violations") ----

(deftest manual-citations-finds-the-expected-count-across-all-five-chapters-test
  (let [by-chapter (group-by :chapter (cg/manual-citations))]
    (is (= 4 (count (get by-chapter "docs/manual/04-time-on-the-wire.md"))))
    (is (= 3 (count (get by-chapter "docs/manual/05-batch-delivery.md"))))
    (is (= 2 (count (get by-chapter "docs/manual/06-breaking-data-on-purpose.md"))))
    (is (= 2 (count (get by-chapter "docs/manual/07-judging.md"))))
    (is (= 3 (count (get by-chapter "docs/manual/08-your-own-data.md"))))))

;; ---- red before green: a simulated pre-session register (only the
;; two pairs that existed before this session's own additions) ----

(deftest uncovered-against-pre-session-register-finds-the-real-dimension-1-gaps-test
  ;; ADR-0130: was `(contains? #{:quickstart-fresh :demo-exerciser-fresh}
  ;; (:extraction %))` -- a correct proxy for "the two rows that
  ;; existed before ADR-0129" only as long as those extraction kinds
  ;; stayed at exactly two rows total. The busy-tuesday row (ADR-0130)
  ;; is a legitimate third :demo-exerciser-fresh row, so the kind-based
  ;; proxy now overcounts; filtering by :script name keeps this test's
  ;; own documented intent -- the exact two ADR-0129 pre-session
  ;; pairs -- accurate regardless of how many future rows share a kind.
  (let [pre-session-rows (filterv #(contains? #{"bin/quickstart-demo" "bin/demo-exerciser-ed-tuesday"} (:script %))
                                   (reg/load-registry))
        violations (cg/uncovered (cg/manual-citations) pre-session-rows)
        cited (set (map :cited-source violations))]
    (is (= 2 (count pre-session-rows)) "sanity: exactly the two pre-existing pairs")
    (is (= #{"docs/use-cases/profile-tier-hl7v2-conformance-gating.md"
             "docs/use-cases/judge-tier-calibration-studies.md"
             "docs/use-cases/acceptance-qa-of-vendor-corpora.md"
             "docs/use-cases/regression-baselining.md"}
           cited)
        "the four docs/use-cases citations dimension 1 found uncovered")))

;; ---- section-aware disambiguation, synthetic: when a :source has
;; MORE THAN ONE register row (README.md's own two, today), a citation
;; to a specific section must resolve against the row whose own
;; :section actually names it -- not against any row merely because
;; :source matches ----

(deftest covered-distinguishes-two-different-sections-of-the-same-source-test
  (let [register [{:source "README.md" :script "bin/quickstart-demo" :section "Quickstart"}
                   {:source "README.md" :script "bin/readme-what-you-get" :section "What you get"}]
        what-you-get-citation {:chapter "x.md" :strip "s1" :cited-source "README.md" :cited-section "What you get"}
        quickstart-citation {:chapter "x.md" :strip "s2" :cited-source "README.md" :cited-section "Quickstart"}
        unrelated-section-citation {:chapter "x.md" :strip "s3" :cited-source "README.md"
                                     :cited-section "Something else entirely"}]
    (is (empty? (cg/uncovered [what-you-get-citation] register))
        "the readme-what-you-get row covers a citation naming its own section")
    (is (empty? (cg/uncovered [quickstart-citation] register))
        "the quickstart-demo row covers a citation naming its own section")
    (is (= 1 (count (cg/uncovered [unrelated-section-citation] register)))
        "a citation to a section NEITHER row's own :section names is flagged uncovered, even though :source matches two rows -- proving the match is genuinely section-aware, not a source-only pass masked by row count")))

(deftest covered-does-not-require-a-section-for-a-single-row-source-test
  (let [register [{:source "docs/use-cases/regression-baselining.md" :script "bin/usecase-regression-baselining"}]
        citation {:chapter "x.md" :strip "s" :cited-source "docs/use-cases/regression-baselining.md"
                   :cited-section nil}]
    (is (empty? (cg/uncovered [citation] register))
        "a source with exactly one register row is covered regardless of section -- matches every citable source except README.md today")))
