(ns ehrt.docs-tooling.citation-gate
  "ADR-0129 (manual-review dimension 1, strip executability): every
  \"Strip source citations\" table in `docs/manual/0*.md` names, per
  strip, where that strip's own command/output came from. Before this
  namespace, nothing enforced that a cited source is actually one this
  workspace re-runs -- the exact gap dimension 1 found for three
  chapters. `manual-citations` extracts every citable row; `uncovered`
  is the violation set a citation-gate test asserts empty against the
  live `ehrt.docs-tooling.exercised-sources` registry.

  A citation table row's own \"source\" cell is scoped to a CITABLE
  doc path -- `README.md`, a `demos/scenarios/*/README.md`, or a
  `docs/use-cases/*.md` page, the three source shapes the exercised-
  sources register actually carries rows for -- via a backtick-quoted
  token matching one of those three patterns specifically, not any
  backtick-quoted text in the cell. Several real rows (verified against
  every citation table in the live tree before choosing this scope,
  see the session record) cite something else entirely: a fixture path
  (`test-fixtures/fhir/storefront-patient.json`), a config file
  (`demos/scenarios/ed-tuesday/config-latency.edn`), or nothing at all
  (\"witnessed this session, same run\") -- self-contained provenance
  about how a value was re-derived, not a citation to another
  document's own strip. Those rows are correctly out of this gate's
  own scope; matching any backtick token at all would false-positive
  on every one of them."
  (:require [clojure.string :as str]
            [ehrt.kernel.interface :as kernel]))

(def ^:private citable-source-re
  #"`(README\.md|demos/scenarios/[^/`]+/README\.md|docs/use-cases/[^/`]+\.md)`")

(defn- table-row-cells
  "A markdown table row line's own cells, trimmed -- `| a | b |` ->
  [\"a\" \"b\"]. nil if `line` doesn't start with `|`."
  [line]
  (when (str/starts-with? (str/trim line) "|")
    (let [inner (-> (str/trim line)
                     (str/replace #"^\|" "")
                     (str/replace #"\|$" ""))]
      (mapv str/trim (str/split inner #"\|")))))

(defn- separator-row?
  "A markdown table's own header-separator row (`|---|---|`) -- every
  cell is dashes only."
  [cells]
  (every? #(re-matches #"-+" %) cells))

(defn- citation-tables-in-doc
  "Every \"Strip source citations\" table's own data rows (header and
  separator rows dropped) in `path`, each as [strip-cell source-cell].
  A small state machine, deliberately explicit about the three lines
  that separate the marker from real data (a blank line, the header
  row, the `|---|---|` separator row) rather than reusing a single
  boolean flag for all of them -- an earlier version collapsed
  \"marker seen\" and \"header seen\" into one flag and reset to
  :before on the blank line between the marker and the table, before
  the header/separator/data rows were ever reached (caught live: the
  first run against the real committed manual returned zero rows from
  five chapters with real tables -- the mechanism-sanity check this
  namespace's own test suite adds exists because of that near-miss)."
  [path]
  (let [lines (str/split-lines (slurp path))]
    (loop [remaining lines
           state :before
           rows []]
      (if (empty? remaining)
        rows
        (let [line (first remaining)
              more (rest remaining)
              cells (table-row-cells line)]
          (case state
            :before
            (recur more (if (str/includes? line "Strip source citations") :seeking-header :before) rows)

            :seeking-header
            (recur more (if cells :seeking-separator :seeking-header) rows)

            :seeking-separator
            (recur more (if cells :in-rows :seeking-separator) rows)

            :in-rows
            (if cells
              (recur more :in-rows (conj rows cells))
              (recur more :before rows))))))))

(def ^:private section-re
  #"^,\s*\"?([^\";]+?)\"?\s*(?:;|$)")

(defn- cited-source+section
  "The {:cited-source :cited-section} `source-cell` names -- nil if it
  names no citable doc path at all (a self-contained provenance note,
  out of this gate's own scope -- see this namespace's own docstring).
  :cited-section is the quoted-or-bare label immediately following the
  path (`README.md`, \"What you get\" -> \"What you get\"; `README.md`,
  Quickstart; witnessed... -> \"Quickstart\"), nil if the cell names no
  such label -- only load-bearing when the cited :source has more than
  one register row (README.md's own two; see `covered?`)."
  [source-cell]
  (when-let [m (re-find citable-source-re source-cell)]
    (let [whole (first m)
          path (second m)
          idx (+ (str/index-of source-cell whole) (count whole))
          tail (subs source-cell idx)
          section-m (re-find section-re tail)]
      {:cited-source path :cited-section (second section-m)})))

(defn- default-manual-paths
  "Every path under docs/manual/, via ehrt.kernel.interface/list-files
  (result-or-loud, ADR-0078 -- io-vocabulary-lint-test forbids a bare
  `.listFiles` call outside that namespace's own allowlist)."
  []
  (let [r (kernel/list-files "docs/manual")]
    (if (kernel/ok? r)
      (sort (map str (:payload r)))
      (throw (ex-info "failed to list docs/manual" r)))))

(defn manual-citations
  "Every {:chapter :strip :cited-source :cited-section} row across
  every `docs/manual/0*.md` file's own \"Strip source citations\"
  table, restricted to rows whose own source cell names a citable doc
  path."
  ([] (manual-citations (default-manual-paths)))
  ([manual-paths]
   (->> manual-paths
        (filter #(re-find #"docs/manual/0.*\.md$" %))
        (mapcat (fn [path]
                  (->> (citation-tables-in-doc path)
                       (keep (fn [[strip source-cell]]
                               (when-let [cited (cited-source+section source-cell)]
                                 (assoc cited :chapter path :strip strip)))))))
        vec)))

(defn- covered?
  "true if some `register-rows` entry covers `citation` (a
  manual-citations row). A :cited-source with exactly one register
  row is covered by :source alone -- every source in this register
  except README.md has exactly one row, so :cited-section is
  irrelevant there. A :cited-source with MORE than one row (README.md
  alone, today) requires a case-insensitive substring match between
  :cited-section and some candidate row's own :section -- the
  precision README.md's own two, unrelated sections (Quickstart, What
  you get) need: without it, either row's own :source match would
  silently satisfy a citation to the OTHER section."
  [{:keys [cited-source cited-section]} register-rows]
  (let [candidates (filter #(= cited-source (:source %)) register-rows)]
    (case (count candidates)
      0 false
      1 true
      (boolean
       (some (fn [row]
               (and cited-section (:section row)
                    (let [a (str/lower-case cited-section)
                          b (str/lower-case (:section row))]
                      (or (str/includes? a b) (str/includes? b a)))))
             candidates)))))

(defn uncovered
  "Every `manual-citation-rows` row `covered?` finds no register-rows
  match for -- the citation gate's own violation set."
  [manual-citation-rows register-rows]
  (vec (remove #(covered? % register-rows) manual-citation-rows)))

(defn violation-message
  "Actionable failure text for one `uncovered` row -- names the
  offending chapter, the cited source, and where a register row
  belongs."
  [{:keys [chapter strip cited-source cited-section]}]
  (str chapter " cites `" cited-source "`"
       (when cited-section (str " (section \"" cited-section "\")"))
       " for strip `" strip
       "` but no row in components/docs-tooling/resources/docs-tooling/"
       "exercised-sources.edn covers it (:source \"" cited-source "\""
       (when cited-section (str " with a :section matching \"" cited-section "\""))
       ")"))
