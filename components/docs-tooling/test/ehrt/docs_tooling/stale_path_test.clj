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
  request, not assumed).

  2026-08-01 addendum (storefront + ruled literals session, AR-3): a
  second, unrelated tripwire in the same family, scanning README.md
  specifically. README.md is this workspace's storefront, read by
  people who have never seen an ADR number and shouldn't need to --
  internal provenance codes (`ADR-\\d+`, `EXP-[A-Z]?\\d+`, `DOC-\\d+`,
  and bare `D\\d+` ruling codes like source-sink-design.md's D9/D13)
  leaking into its body prose is exactly the kind of internal-logbook
  drift the storefront must not carry. Deliberately narrower than the
  scan above: markdown link destinations (`](...)`) and HTML comments
  are exempt and stripped before matching, because the Maturity
  table's own Evidence-column hrefs legitimately point at files named
  `EXP-A4-results.md` -- a citation, not a leak -- and an editorial
  HTML comment is invisible prose, not storefront-facing text.

  2026-08-01 addendum (agent-ux capture session, `notes/ADRs.md`
  ADR-0023, AR-4): `positioning.md` joins the forbidden-string family
  above -- `docs/dev/positioning.md` was renamed `docs/dev/AUDIENCES.md`
  this session when agents joined its audience register as an explicit
  class, and every live citation across `docs/` was swept to the new
  name. A stray `positioning.md` reference surviving anywhere under
  `docs/**/*.md` is by construction stale -- the file no longer exists
  at that path -- so it is forbidden outright, the same denylist shape
  as `ehrt.tools.` above, not scoped to a prefix or suffix pattern.
  `notes/`'s own historical citations of the old name (pre-rename
  prompts, ADR context) are untouched, out of this test's scan scope,
  same as every other entry in this family."
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
    (conj :retired-ehrt-tools-namespace)
    (str/includes? content "positioning.md")
    (conj :retired-positioning-filename)))

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
    (is (empty? (violations "see ehrt.corpus.manifest/ManifestV1_1"))))
  (is (= [:retired-positioning-filename] (violations "see docs/dev/positioning.md for the audience register")))
  (testing "the post-rename citation form does not trip the retired-filename pattern"
    (is (empty? (violations "see docs/dev/AUDIENCES.md for the audience register")))))

;; README register tripwire (2026-08-01, AR-3) -- separate from the scan
;; above: different source (README.md only), different exemptions (link
;; destinations and HTML comments, not a path-prefix distinction).

(defn- strip-exempt-spans
  "Strips markdown link destinations (`](...)`) and HTML comments
  (`<!-- ... -->`) from README.md's text before the register-code scan
  below -- both are legitimate places for an internal code to appear
  (the Maturity table's own Evidence-column hrefs, an editorial aside)
  and must not trip the tripwire. Link targets are blanked, not
  deleted, so surrounding prose offsets/structure survive intact."
  [content]
  (-> content
      (str/replace #"\]\([^)]*\)" "]()")
      (str/replace #"(?s)<!--.*?-->" "")))

(def ^:private register-code-re
  #"ADR-\d+|EXP-[A-Z]?\d+|DOC-\d+|\bD\d+\b")

(defn- register-code-violations
  "Every internal provenance-code match in README.md's prose, link
  targets and HTML comments already stripped. Distinct, in match
  order."
  [content]
  (->> (re-seq register-code-re (strip-exempt-spans content))
       distinct
       vec))

(deftest readme-body-carries-no-internal-provenance-codes-test
  (let [found (register-code-violations (slurp "README.md"))]
    (is (empty? found)
        (str "README.md's storefront prose cites internal provenance codes: " found))))

(deftest each-register-code-pattern-is-actually-caught-test
  (is (= ["ADR-0012"] (register-code-violations "ratified in ADR-0012, see below")))
  (is (= ["EXP-A4"] (register-code-violations "results in EXP-A4 confirm this")))
  (is (= ["EXP-5"] (register-code-violations "results in EXP-5 confirm this")))
  (is (= ["DOC-5"] (register-code-violations "landed under DOC-5")))
  (is (= ["D9"] (register-code-violations "the zero-flag defaults, D9"))))

(deftest link-destinations-and-html-comments-are-exempt-test
  (testing "a real Evidence-column href citing an EXP results file"
    (is (empty? (register-code-violations
                  "[Byte-reproducibility proof](components/corpus/docs/experiments/EXP-A4-results.md) in a clean environment."))))
  (testing "an HTML comment"
    (is (empty? (register-code-violations "<!-- ADR-0012 predates this rename -->"))))
  (testing "the same code outside both exemptions still trips it"
    (is (= ["ADR-0012"] (register-code-violations "predates ADR-0012, unlike the comment above")))))
