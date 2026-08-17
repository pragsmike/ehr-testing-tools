(ns ehrt.docs-tooling.state-derived
  "Renderer for `.agents/state-derived.md` -- the countable half of the
  continuity register (compression arc session D, 2026-08-17,
  `notes/adr/0147-compression-arc-close.md`).

  `.agents/state.md` was 724 lines: 340 lines of dated preamble
  (thirteen append-only update blocks) plus nine sections stamped
  `[V @b96c246]` -- a probe-verified snapshot of counts, inventories
  and gate lists taken at ADR-0139's close and stale by three arc
  closes before this session read it. Its own regeneration contract
  (`rulings.md#R-state-regeneration`) asks a session to re-probe every
  `[V]` claim by hand at each arc close, and the record of what that
  actually produced is in the file's own preamble: the contract was
  satisfied ONCE in fifty ADRs, and the eleven blocks in between each
  say, in their own words, that the sections below them are stale.

  A claim re-derived by hand at each close is a claim that goes stale
  between closes. The fix is the arc's own pattern, third application
  after `notes/ADRs.md` (ADR-0143) and the roadmap/rulings row
  contracts (ADR-0144/ADR-0145): what is DERIVABLE is generated from
  the tree on every `make docsgen` and diffed by CI, so it cannot be
  stale and cannot be hand-written wrong; what is HAND-OWNED stays in
  `state.md`, capped and linted; what is HISTORICAL moves verbatim to
  a dated attic file. This namespace is the first of those three.

  POPULATION IS THE TREE, never a registry (`rulings.md#R-population-
  closure`, the class ADR-0139 named). Every count below is a fresh
  directory listing, file parse, or line count at render time. Where a
  count is parsed out of source text rather than listed off the
  filesystem -- the oracle's own `roots` map -- the parse is line-
  anchored and has its own mechanism-sanity case in
  `ehrt.docs-tooling.state-derived-test`, because a parser that
  silently matches nothing renders `0` and `0` reads as a fact.

  DELIBERATELY NOT DERIVED HERE: the `stable-*` tag census, which
  `state.md` carried as a live count. A tag is pushed AFTER the commit
  it points at -- tag law, `rulings.md#R-tag-law`, and R30's own
  order -- so a generated tag count is wrong in the commit that
  records it and right only until the next tag. CI's freshness step
  would then fail on the FIRST push after every tag ceremony, blaming
  a session that changed nothing. The count stays a command in
  `state.md`'s own hand-owned half, where a reader runs it and gets
  today's answer instead of last week's."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [ehrt.docs-tooling.docsgen :as docsgen]
            [ehrt.kernel.interface :as kernel]))

;; ---- filesystem primitives (result-or-loud, ADR-0078) ----

(defn- entries
  "Every direct child of `dir` as a `java.io.File`. Via
  `ehrt.kernel.interface/list-files` rather than a bare `.listFiles`
  (`ehrt.docs-tooling.io-vocabulary-lint-test` forbids the latter in
  brick src outside its own two-namespace allowlist). The distinction
  is load-bearing, not ceremonial: a nil from `.listFiles` is an I/O
  failure, and reading it as an empty directory would regenerate this
  register with ZERO of everything and call it current."
  [dir]
  (let [r (kernel/list-files dir)]
    (when-not (kernel/ok? r)
      (throw (ex-info (str "failed to list " dir
                           " -- refusing to render the derived register from an unknown directory")
                      r)))
    (:payload r)))

(defn- subdir-names [dir]
  (->> (entries dir) (filter #(.isDirectory ^java.io.File %)) (map #(.getName ^java.io.File %)) sort vec))

(defn- file-names [dir]
  (->> (entries dir) (filter #(.isFile ^java.io.File %)) (map #(.getName ^java.io.File %)) sort vec))

(defn line-count
  "Lines in `path`, by `line-seq`. Public because
  `ehrt.docs-tooling.reading-set-budget-test` measures a set's actual
  with this exact function -- one definition, so the number this
  register renders and the number the budget gate enforces cannot be
  two different measurements of the same file (R13's own 'one
  definition each, in the script')."
  [path]
  (with-open [r (io/reader path)]
    (count (line-seq r))))

(defn total-lines
  "Sum of `line-count` across `paths`. Throws on a nonexistent path,
  deliberately -- treating a ghost as zero lines would let an
  over-budget set render as in-budget."
  [paths]
  (reduce + (map line-count paths)))

;; ---- pure parsers (each with its own sanity case in the test ns) ----

(defn parse-oracle-roots
  "The string keys of `digest.clj`'s own `roots` map, in file order.

  Line-anchored inside the map form rather than a free `re-seq` over
  the whole file: the namespace is 400+ lines of per-batch `def`s and
  long docstrings that mention root names in prose, and a free match
  would count those too. Scanning starts at the `(def ^:private roots`
  line and stops at the first line whose first non-space character
  closes the map.

  Returns a vector, so the caller can render the COUNT and the SET --
  ADR-0139's own D1-4 method note is that the root set and the
  vendored-test set are different populations that must be compared as
  sets, never by their cardinalities.

  TWO BUGS THIS FUNCTION SHIPPED WITH, both caught by its own synthetic
  fixture in `ehrt.docs-tooling.state-derived-test` and recorded because
  the live-tree sanity case could see neither:

  1. The first key sits on the SAME LINE as the opening brace
     (`{\"appendicitis\"  appendicitis-batch`), so a `^\\s+\"` anchor
     skipped it and rendered one root fewer than the tree holds.
  2. The map's last key CLOSES it (`\"injuries\" injuries-pair})`), so
     there is no line whose first non-space character is `}` and a
     `take-while` on that condition never terminated -- the scan ran
     past the form into `-main`'s docstring below, which contains both
     a `{:ground-truth :hl7}` and several quoted phrases. It returned
     the right answer anyway, by luck: none of those lines happens to
     hold a complete `\"...\"` at its start. A docstring reflowed by one
     word would have added a phantom root.

  So the window is brace-balanced instead: scan from the form's own
  first `{` and stop where depth returns to zero. Both figures a
  reader takes from this -- 35 roots -- are then the map's, not the
  file's."
  [content]
  (let [lines (str/split-lines content)
        start (first (keep-indexed (fn [i l] (when (re-find #"^\(def \^:private roots$|^\(def \^:private roots\s" l) i)) lines))]
    (when start
      (->> (reduce (fn [acc line]
                     (let [opened (+ (:depth acc) (count (filter #{\{} line)))
                           depth' (- opened (count (filter #{\}} line)))
                           acc' (-> acc (assoc :depth depth') (update :lines conj line))]
                       (if (and (pos? opened) (zero? depth'))
                         (reduced acc')
                         acc')))
                   {:depth 0 :lines []}
                   (drop (inc start) lines))
           :lines
           (keep #(second (re-find #"^\s*\{?\s*\"([^\"]+)\"" %)))
           vec))))

(defn parse-roadmap-rows
  "Every top-level roadmap row as `{:section :token}`. Section is the
  `## ` heading's first word; token is the ruled status word
  (`OPEN`/`CLOSED`/`DEFERRED`/`EXTERNAL`), which
  `ehrt.docs-tooling.roadmap-lint-test` already guarantees is present
  and first -- this parser reports the distribution, it does not
  re-gate the contract."
  [content]
  (let [lines (str/split-lines content)]
    (:rows
     (reduce (fn [{:keys [section] :as acc} line]
               (cond
                 (str/starts-with? line "## ")
                 (assoc acc :section (second (re-find #"^## (\S+)" line)))

                 (str/starts-with? line "- ")
                 (if-let [token (second (re-find #"^- (OPEN|CLOSED|DEFERRED|EXTERNAL)\b" line))]
                   (update acc :rows conj {:section section :token token})
                   acc)

                 :else acc))
             {:section nil :rows []}
             lines))))

(defn parse-rulings-rows
  "Every `- **R-<slug>** -- ...` row as `{:slug :superseded?}`. The row
  contract is ADR-0145's and gated by
  `ehrt.docs-tooling.rulings-lint-test`; this only counts."
  [content]
  (->> (str/split-lines content)
       (keep (fn [l] (when-let [slug (second (re-find #"^- \*\*R-([a-z0-9-]+)\*\*" l))]
                       {:slug slug})))
       (map-indexed (fn [i r] (assoc r :index i)))
       vec))

(defn- superseded-slugs
  "Slugs whose row names a successor. A row's `SUPERSEDED-BY` may sit
  on any of its (up to three) lines, so this reads the whole file
  rather than the row's own first line."
  [content]
  (->> (re-seq #"\*\*R-([a-z0-9-]+)\*\*[^\n]*(?:\n[^\n-][^\n]*)*?SUPERSEDED-BY" content)
       (map second)
       set))

;; ---- fact collection (impure: reads the live tree) ----

(defn collect
  "Every derived fact this register renders, read fresh from the tree."
  []
  (let [reading-sets (edn/read-string (slurp ".agents/reading-sets.edn"))
        baselines (edn/read-string (slurp ".agents/reading-sets-baseline.edn"))
        rulings (slurp ".agents/rulings.md")
        roadmap-rows (parse-roadmap-rows (slurp ".agents/plans/roadmap.md"))]
    {:components (subdir-names "components")
     :bases (subdir-names "bases")
     :projects (subdir-names "projects")
     :modules (->> (file-names "components/sim/resources/sim/modules")
                   (filter #(str/ends-with? % ".json")))
     :notice-rows (->> (str/split-lines (slurp "components/sim/resources/sim/modules/NOTICE"))
                       (filter #(str/starts-with? % "| `"))
                       count)
     :oracle-roots (parse-oracle-roots (slurp "components/oracle/src/ehrt/oracle/digest.clj"))
     :adrs (->> (file-names "notes/adr") (filter #(re-matches #"\d{4}-.*\.md" %)))
     :docs-tooling-gates (->> (file-names "components/docs-tooling/test/ehrt/docs_tooling")
                              (filter #(str/ends-with? % "_test.clj")))
     :test-namespaces (->> (concat (file-seq (io/file "components")) (file-seq (io/file "bases"))
                                   (file-seq (io/file "projects")))
                           (filter #(.isFile ^java.io.File %))
                           (filter #(str/ends-with? (.getName ^java.io.File %) "_test.clj"))
                           (filter #(str/includes? (str/replace (.getPath ^java.io.File %) "\\" "/") "/test/"))
                           count)
     :roadmap-rows roadmap-rows
     :rulings-rows (parse-rulings-rows rulings)
     :rulings-superseded (superseded-slugs rulings)
     :session-records (->> (file-names ".agents/session-records")
                           (remove #{"README.md" "INDEX.md"}))
     :prompts (->> (file-names ".agents/prompts")
                   (remove #{"README.md" "INDEX.md"}))
     :reading-sets (into (sorted-map)
                         (for [[k {:keys [paths budget-lines]}] reading-sets]
                           [k {:actual (total-lines paths)
                               :budget budget-lines
                               :baseline (get baselines k)
                               :paths (vec paths)}]))}))

;; ---- pure render ----

(defn- md-table [headers rows]
  (str/join "\n"
            (concat [(str "| " (str/join " | " headers) " |")
                     (str "|" (str/join "|" (repeat (count headers) "---")) "|")]
                    (for [row rows]
                      (str "| " (str/join " | " (map docsgen/escape-cell row)) " |")))))

(defn- names-line [names]
  (str/join ", " (map #(str "`" % "`") names)))

(def ^:private preamble
  "The reader-facing half of the generation contract. Kept here as a
  string literal, the same way `render-adr-index`'s preamble is: the
  output is WHOLLY generated, so it has no hand-edited region for
  prose to live in."
  "# State of the project — derived register

**Every number and list on this page is re-derived from the live tree
on every `make docsgen`, and CI's own generated-doc freshness step
diffs the result.** Nothing here is carried forward on a previous
version's authority, and nothing here is hand-written — which is what
retires the re-probe-by-hand half of `.agents/state.md`'s own
regeneration contract (`.agents/rulings.md#R-state-regeneration`,
ADR-0047 AR-C-1; compression arc session D,
`notes/adr/0147-compression-arc-close.md`).

Its companion, [`state.md`](state.md), holds what a tree cannot
derive: the judgement, the watch items, the environment, and the
design-channel contract. Read that first; this page answers *how many*
and *which ones*.

**A count here is not a claim that two counts correspond.** Oracle
roots and vendored round-trip tests are different populations. Review 2
asserted a correspondence between them because both totals happened to
read 34 on the day it looked; they have diverged since, with nothing
wrong (ADR-0139 D1-4). Compare the sets, not the cardinalities.")

(defn render
  "Pure: the collected facts -> `.agents/state-derived.md`'s content."
  [{:keys [components bases projects modules notice-rows oracle-roots adrs
           docs-tooling-gates test-namespaces roadmap-rows rulings-rows
           rulings-superseded session-records prompts reading-sets]}]
  (let [by-section (group-by :section roadmap-rows)
        section-line (fn [s] (let [rows (get by-section s)]
                               [s (str (count rows))
                                (names-line (sort (distinct (map :token rows))))]))]
    (str (docsgen/banner "state-derived"
                         "the live tree -- directory listings, file parses and line counts"
                         (str "Change the tree, or this file's renderer "
                              "(components/docs-tooling/src/ehrt/docs_tooling/state_derived.clj), "
                              "and regenerate. Hand-owned continuity lives in .agents/state.md instead."))
         "\n"
         preamble
         "\n\n## Workspace shape\n\n"
         (md-table ["kind" "count" "names"]
                   [["components" (count components) (names-line components)]
                    ["bases" (count bases) (names-line bases)]
                    ["projects" (count projects) (names-line projects)]])
         "\n\n## Vendored GMF content\n\n"
         (md-table ["fact" "count"]
                   [["module JSONs (`components/sim/resources/sim/modules/*.json`)" (count modules)]
                    ["NOTICE provenance rows" notice-rows]
                    ["regression-oracle roots (`ehrt.oracle.digest`'s own `roots` map)" (count oracle-roots)]])
         "\n\nOracle roots: " (names-line oracle-roots) "\n"
         "\n## Test surface\n\n"
         (md-table ["fact" "count"]
                   [["`*_test.clj` namespaces under any brick's own `test/`" test-namespaces]
                    ["docs-tooling gate namespaces" (count docs-tooling-gates)]])
         "\n\nDocs-tooling gates: "
         (names-line (map #(str/replace % #"\.clj$" "") docs-tooling-gates)) "\n"
         "\n## Registers\n\n"
         (md-table ["register" "count"]
                   [["ADR files (`notes/adr/NNNN-*.md`)" (count adrs)]
                    ["roadmap rows (all sections)" (count roadmap-rows)]
                    ["rulings rows" (count rulings-rows)]
                    ["rulings rows superseded" (count rulings-superseded)]
                    ["session records" (count session-records)]
                    ["archived prompts" (count prompts)]])
         "\n\n### Roadmap rows by section\n\n"
         (md-table ["section" "rows" "tokens in use"]
                   (map section-line (distinct (keep :section roadmap-rows))))
         "\n\n## Reading sets\n\n"
         "`:budget-lines` may never exceed the ratchet baseline and may always fall "
         "(`.agents/reading-sets-baseline.edn`, ADR-0143 guard #3); over budget a session "
         "compacts or STOPs (`.agents/rulings.md#R-budget-stop`).\n\n"
         (md-table ["set" "paths" "actual" "budget" "baseline" "headroom"]
                   (for [[k {:keys [actual budget baseline paths]}] reading-sets]
                     [(str k) (count paths) actual budget baseline (- budget actual)]))
         "\n")))

;; ---- the two dated record indexes ----
;;
;; `.agents/session-records/README.md` (223 lines) and
;; `.agents/prompts/README.md` (171 lines) each stated a convention in
;; their first ~30 lines and then listed every file in the directory --
;; 149 and 142 rows, one more per session, forever. Both are
;; `:onboarding` members, so every cold session of every task class
;; read 390 lines of dated index to learn a naming convention. The list
;; half is a directory listing and generates; the convention half is
;; prose and stays, capped by
;; `ehrt.docs-tooling.index-completeness-test`.
;;
;; Rendered to a SIBLING `INDEX.md` rather than folded into
;; `state-derived.md`, for three reasons recorded here because the
;; alternative was live: the index belongs beside what it indexes, so a
;; reader already in the directory finds it; folding would grow
;; `state-derived.md` by one line per session forever, which is exactly
;; the growth curve this arc is removing from a reading set; and
;; `index-completeness-test` already scans one file per directory, so
;; retargeting it costs one line and keeps the population identical.
;; `INDEX.md` is in NO reading set -- that is the whole win.

(defn render-index
  "Pure: a directory's own file list -> its `INDEX.md` content, in the
  star-bullet format `ehrt.docs-tooling.index-completeness-test`
  already parses (`  * filename.md`). Files sort by name, which for
  this repo's `YYYY-MM-DD-slug.md` convention is chronological."
  [{:keys [dir make-target readme what files]}]
  (str (docsgen/banner make-target
                       (str "a directory listing of " dir)
                       (str "Add or remove a file in " dir " and regenerate; the naming convention and "
                            "everything else a reader needs is in " readme "."))
       "\n# " what "\n\n"
       "Generated index of [`" dir "`](.) — " (count files) " files. "
       "The convention, what a record contains, and where this sits relative to "
       "every other register are in [`README.md`](README.md); annotations that used "
       "to ride these rows are in "
       "[`../plans/state-history-2026-08.md`](../plans/state-history-2026-08.md), dated.\n\n"
       (str/join "\n" (map #(str "  * " %) files))
       "\n"))

(def index-targets
  "The two generated record indexes. Each `:files` is read fresh from
  the directory at write time -- the population is the tree."
  [{:dir ".agents/session-records" :make-target "state-derived"
    :readme "`README.md`" :what "Session records — index"}
   {:dir ".agents/prompts" :make-target "state-derived"
    :readme "`README.md`" :what "Session prompts — index"}])

(defn index-files
  "Every indexable file in `dir`: the directory's own contents minus
  the two files that are not records (`README.md`, and the generated
  `INDEX.md` itself, which must never index itself)."
  [dir]
  (->> (file-names dir) (remove #{"README.md" "INDEX.md"}) vec))

;; ---- impure shell ----

(defn write-state-derived!
  "-X-invokable: renders `.agents/state-derived.md` and the two record
  `INDEX.md` files from the live tree."
  [{:keys [out] :or {out ".agents/state-derived.md"}}]
  (spit out (render (collect)))
  (doseq [{:keys [dir] :as target} index-targets]
    (spit (io/file dir "INDEX.md")
          (render-index (assoc target :files (index-files dir))))))
