(ns ehrt.docs-tooling.adr-index-test
  "Guard #2 of the register-compression arc (session A, 2026-08-16,
  `notes/adr/0143-adr-index-generated.md`): `notes/ADRs.md` is a
  GENERATED surface, and its rows derive from each ADR's own heading
  and Status line.

  The defect this answers is a shape that decayed rather than a rule
  that was broken. ADR-0046 AR-B-1 made the file an index of one-line
  rows on 2026-08-05 and nothing enforced the line; by 2026-08-16 it
  held 140 rows averaging 977 characters (134 KB), the longest 8,447 --
  each row a session write-up written into a slot sized for a title,
  each addition locally reasonable, the aggregate a 134 KB register
  nobody could read. The prior arc's own guards were written to the one
  specimen in front of them and outgrown the same way, which is why
  this one is not a size limit: a row cannot regrow if no one writes a
  row.

  Three gates over the live tree, plus mechanism-sanity tests on
  synthetic data proving each gate's own extraction actually catches
  what it claims to -- the gate-and-the-proof-the-gate-gates pairing
  that `reading-set-budget-test` (deleted by `e189418`) and
  `ehrt.docs-tooling.done-pointer-adr-test` established here.

  - **Parity**: the committed `notes/ADRs.md` is byte-for-byte what
    `ehrt.docs-tooling.docsgen/render-adr-index` renders for the live
    `notes/adr/` tree. This is the same freshness claim CI's
    `make docsgen && git diff --exit-code` step makes, asserted in the
    normal suite too so a hand edit fails before it reaches a push.
  - **Shape**: every ADR file carries a `## ADR-NNNN — Title` heading
    whose number matches its filename, and a `**Status:**` line in the
    seven lines below it. Without this the parity gate would be the
    only thing standing between a malformed ADR and a broken row, and
    it would report the failure as a diff rather than as the missing
    line it is.
  - **Anchors**: every live inbound `ADRs.md#<anchor>` reference in the
    workspace resolves to a heading the generated file actually
    carries. The census that opened ADR-0143 found this population
    EMPTY -- every live citation is the marker-only form ADR-0102
    established, `[ADR-NNNN](../notes/ADRs.md)`, with no anchor, and
    the only four anchored references in the tree are frozen-era, in
    `notes/sim/`, pointing at a path that never resolved here. The gate
    exists so that stays a fact this test enforces rather than a fact
    about 2026-08-16, since the generated file's heading set is now
    decided by a renderer that no one editing a doc will think to
    check."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ehrt.docs-tooling.docsgen :as docsgen]))

(def ^:private index-path "notes/ADRs.md")
(def ^:private adr-dir "notes/adr")

;; -- gate 1: parity with the generator --

(deftest notes-adrs-md-is-exactly-what-the-generator-renders-test
  (testing "the committed index is byte-for-byte the generator's own output for the live notes/adr/ tree"
    (is (= (docsgen/render-adr-index (docsgen/adr-entries adr-dir))
           (slurp index-path))
        (str index-path " differs from `make adr-index`'s own output -- it is a GENERATED file "
             "(ADR-0143). Edit the ADR under " adr-dir "/ and run `make adr-index` (or `make docsgen`); "
             "never hand-edit a row."))))

;; -- gate 2: every ADR file has the uniform shape the rows derive from --

(deftest every-adr-file-carries-a-heading-and-a-status-line-test
  ;; The population comes from `adr-entries`, the generator's own, rather
  ;; than a second listing walked here -- a shape gate that enumerated the
  ;; directory differently from the renderer could pass while the renderer
  ;; was reading a different set of files.
  (let [entries (map #(assoc % :name (:file %)) (docsgen/adr-entries adr-dir))]
    (testing "sanity: the population is the directory, and it is not empty"
      (is (seq entries) (str "no ADR files found under " adr-dir)))
    (testing "every ADR file has a `## ADR-NNNN — Title` heading"
      (let [headless (remove #(and (:number %) (:title %)) entries)]
        (is (empty? headless)
            (str "ADR file(s) with no parseable `## ADR-NNNN — Title` heading: "
                 (vec (map :name headless))))))
    (testing "every ADR file has a `**Status:**` line in its header block"
      (let [statusless (remove :status entries)]
        (is (empty? statusless)
            (str "ADR file(s) with no `**Status:**` line in the header block (heading to first `###`): "
                 (vec (map :name statusless))
                 " -- the index's status column is rendered from that line, so a missing one "
                 "cannot be rendered around."))))
    (testing "every heading's number matches its own filename"
      (let [mismatched (remove #(str/starts-with? (:name %) (str (:number %))) entries)]
        (is (empty? mismatched)
            (str "ADR file(s) whose heading number disagrees with the filename: "
                 (vec (map (juxt :name :number) mismatched))))))))

;; -- gate 3: inbound anchors resolve in the generated file --

(def ^:private excluded-roots
  "Two exclusions, both precedented, both disclosed rather than quietly
  narrowing the population:

  - `notes/sim`, `notes/tools`: frozen provenance, exempt by standing
    ruling (2026-08-01 item 6, the same exemption
    `ehrt.docs-tooling.index-completeness-test` and
    `readme-presence-test` carry). These archives cite the shape the
    pre-workspace repos had and are never rewritten for this one --
    they hold the only four anchored `ADRs.md#` references in the tree,
    at a relative path (`../../notes/ADRs.md`) that has never resolved
    here.
  - `notes/adr`, and `notes/ADRs.md` itself: the register NARRATES
    citation history, so it quotes anchor forms it is not using --
    exactly the reason `ehrt.docs-tooling.stale-path-test` already
    excludes `notes/ADRs.md` from its own denylist scan (\"narrate
    history and legitimately cite the old names\"). This exclusion was
    not foreseen: this gate's first red run flagged
    `notes/adr/0143-adr-index-generated.md` for the frozen-era anchor
    its own census QUOTES as evidence. A false positive on the first
    pass, recorded rather than filtered away silently.

  Everything that actually links to the index for a reader -- `docs/`,
  `components/*/docs/`, `.agents/`, `AGENTS.md`, `README.md` -- stays
  in the population."
  ["notes/sim" "notes/tools" "notes/adr"])

(def ^:private excluded-files ["notes/ADRs.md"])

(def ^:private anchor-scan-roots
  ["docs" "components" ".agents" "notes"])

(def ^:private anchor-scan-files
  ["AGENTS.md" "README.md"])

(defn- markdown-files-under [root]
  (->> (file-seq (io/file root))
       (filter #(.isFile ^java.io.File %))
       (map #(.getPath ^java.io.File %))
       (map #(.replace ^String % "\\" "/"))
       (filter #(str/ends-with? % ".md"))
       (remove (fn [p] (some (fn [f] (str/starts-with? p (str f "/"))) excluded-roots)))
       (remove (set excluded-files))))

(defn- anchor-scan-population []
  (concat (filter #(.isFile (io/file %)) anchor-scan-files)
          (mapcat markdown-files-under (filter #(.isDirectory (io/file %)) anchor-scan-roots))))

(defn inbound-index-anchors
  "Every `ADRs.md#<anchor>` fragment cited by `content`, as bare anchor
  strings. Deliberately matches the filename rather than a full path,
  so a reference at any relative depth is caught."
  [content]
  (->> (re-seq #"ADRs\.md#([A-Za-z0-9._-]+)" content)
       (map second)
       set))

(defn heading-anchor
  "GitHub's own heading-slug rule, the one a `#fragment` in this repo's
  markdown resolves against: lowercase, drop everything that is not
  alphanumeric/space/underscore/hyphen (so an em dash vanishes and
  leaves its two spaces behind), then spaces to hyphens."
  [heading]
  (-> heading
      (str/replace #"^#+\s*" "")
      str/lower-case
      (str/replace #"[^a-z0-9\s_-]" "")
      str/trim
      (str/replace #"\s" "-")))

(defn heading-anchors
  "Every anchor `content`'s own ATX headings expose."
  [content]
  (->> (str/split-lines content)
       (filter #(re-find #"^#{1,6}\s" %))
       (map heading-anchor)
       set))

(deftest every-live-inbound-anchor-into-the-index-resolves-test
  (testing "every live ADRs.md#anchor citation resolves to a heading the generated index carries"
    (let [available (heading-anchors (slurp index-path))
          cited (->> (anchor-scan-population)
                     (mapcat (fn [p] (map (fn [a] [p a]) (inbound-index-anchors (slurp p)))))
                     (remove (fn [[_ a]] (contains? available a))))]
      (is (empty? cited)
          (str "inbound ADRs.md#anchor citation(s) that the generated index does not expose: "
               (vec cited)
               " -- notes/ADRs.md's headings are rendered by "
               "ehrt.docs-tooling.docsgen/render-adr-index; either the citation is stale or the "
               "renderer dropped a heading something links to.")))))

(deftest the-two-standing-index-anchors-are-still-exposed-test
  (testing "the index's own two headings keep the anchors they have carried since 2026-08-05"
    (let [available (heading-anchors (slurp index-path))]
      (is (contains? available "architecture-decision-records--ehr-testing-workspace"))
      (is (contains? available "index")))))

;; -- mechanism-sanity: prove each gate's extraction actually catches what it claims --

(deftest parse-adr-catches-a-missing-status-line-test
  (let [with-status "## ADR-0001 — A title\n\n**Status:** Accepted, 2026-07-28 (ratified).\n\n### Context\n"
        without "## ADR-0001 — A title\n\n### Context\n\n**Status:** Accepted\n"]
    (is (= {:file "0001-a.md" :number "0001" :title "A title" :status "Accepted"}
           (docsgen/parse-adr "0001-a.md" with-status))
        "the status word is cut at the first comma -- the date and provenance after it stay in the ADR")
    (is (nil? (:status (docsgen/parse-adr "0001-a.md" without)))
        (str "a `**Status:**` token BELOW the first `###` subsection is not the record's own status. "
             "The header block is delimited by that subsection, not by a line count -- a seven-line "
             "window passed this fixture on 2026-08-16's first red run, which is why it isn't one."))))

(deftest render-adr-index-refuses-a-malformed-entry-test
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"cannot render the ADR index"
       (docsgen/render-adr-index [{:file "0001-a.md" :number "0001" :title "A title" :status "Accepted"}
                                  {:file "0002-b.md" :number "0002" :title "B title" :status nil}]))
      "a status-less entry must stop the render, not emit a row with a blank status column that reads as a fact"))

(deftest render-adr-index-orders-ascending-and-uses-the-ruled-row-shape-test
  (let [out (docsgen/render-adr-index
             [{:file "0022-b.md" :number "0022" :title "B title" :status "Accepted"}
              {:file "0013-a.md" :number "0013" :title "A title" :status "Accepted"}])
        rows (filter #(str/starts-with? % "- **ADR-") (str/split-lines out))]
    (is (= ["- **ADR-0013** — A title — [`0013-a.md`](adr/0013-a.md) — Accepted"
            "- **ADR-0022** — B title — [`0022-b.md`](adr/0022-b.md) — Accepted"]
           rows)
        "ADR-0046 AR-B-1's own row shape, ascending by number (the pre-split order is not reconstructible from the tree -- ADR-0143 Finding 3)")))

(deftest inbound-anchor-extraction-is-actually-caught-test
  (is (= #{"adr-0010" "index"}
         (inbound-index-anchors
          "see [x](../../notes/ADRs.md#adr-0010) and [y](../notes/ADRs.md#index) and [z](../notes/ADRs.md)"))
      "anchored citations at any depth are caught; the marker-only anchorless form is correctly not one"))

(deftest heading-anchor-derivation-is-actually-caught-test
  (is (= "architecture-decision-records--ehr-testing-workspace"
         (heading-anchor "# Architecture Decision Records — ehr-testing (workspace)"))
      "the em dash drops out and leaves the doubled hyphen GitHub actually produces")
  (is (= #{"index" "architecture-decision-records--ehr-testing-workspace"}
         (heading-anchors "# Architecture Decision Records — ehr-testing (workspace)\n\ntext\n\n## Index\n\n- **ADR-0001** — not a heading\n"))))

(deftest an-unresolvable-inbound-anchor-is-caught-test
  (let [available (heading-anchors "# Architecture Decision Records — ehr-testing (workspace)\n\n## Index\n")
        cited (remove available (inbound-index-anchors "[a](../notes/ADRs.md#index) [b](../notes/ADRs.md#adr-0010)"))]
    (is (= ["adr-0010"] (vec cited))
        "sanity: a citation naming a heading the file does not carry must be reported, proving the failure branch fires")))
