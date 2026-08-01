(ns ehrt.docs-tooling.stale-path-test
  "P1-1 (2026-07-31 review catch-up, finding 4): a stale-path family
  (pre-Polylith `ehr_testing_tools` paths, `test-integration/`, and a
  `docs/experiments/` link missing its component-adjacent prefix)
  recurred across four live docs -- this tripwire scans every
  docs/**/*.md file plus components/corpus/docs/use-cases.edn (the
  rendered form, docs/use-cases.md, is generated and covered by the
  same scan) so the species can't silently re-accumulate. The
  component-adjacent citation form, `components/corpus/docs/experiments/...`,
  is correct and must NOT trip this -- tested both directions below.

  Stage 3 (ADR-0018, AR-7) retired the tools component and added its
  namespace prefix, `ehrt.tools.`, to the forbidden list: no
  current-tense doc may cite a namespace under the retired prefix.
  Deliberately scoped: this scan covers docs/ (plus the use-cases.edn
  source above) only -- notes/ADRs.md, notes/prompts/, and
  .agents/session-records/ narrate history and legitimately cite the
  old names, and this test never reads them (confirmed at AR-7's own
  request, not assumed)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- markdown-files []
  (->> (file-seq (io/file "docs"))
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".md"))
       (map #(.getPath %))))

(defn- scan-sources []
  (conj (markdown-files) "components/corpus/docs/use-cases.edn"))

(defn- violations [content]
  (cond-> []
    (str/includes? content "ehr_testing_tools")
    (conj :ehr-testing-tools-underscore-path)
    (str/includes? content "test-integration/")
    (conj :test-integration-path)
    (re-find #"(?<!corpus/)docs/experiments/" content)
    (conj :docs-experiments-missing-corpus-prefix)
    (str/includes? content "ehrt.tools.")
    (conj :retired-ehrt-tools-namespace)))

(deftest no-stale-path-family-anywhere-in-docs-or-use-cases-edn-test
  (doseq [path (scan-sources)]
    (let [found (violations (slurp path))]
      (is (empty? found) (str path " carries stale-path residue: " found)))))

(deftest the-component-adjacent-form-does-not-trip-the-tripwire-test
  (testing "components/corpus/docs/experiments/... is the correct citation form"
    (is (empty? (violations "see components/corpus/docs/experiments/EXP-A4-results.md")))))

(deftest each-forbidden-pattern-is-actually-caught-test
  (is (= [:ehr-testing-tools-underscore-path] (violations "test/ehr_testing_tools/foo_test.clj")))
  (is (= [:test-integration-path] (violations "lives on the test-integration/ path")))
  (is (= [:docs-experiments-missing-corpus-prefix] (violations "see docs/experiments/EXP-A4-results.md")))
  (is (= [:retired-ehrt-tools-namespace] (violations "see ehrt.tools.corpus.manifest/ManifestV1_1")))
  (testing "the stage-3 citation form does not trip the retired-prefix pattern"
    (is (empty? (violations "see ehrt.corpus.manifest/ManifestV1_1")))))
