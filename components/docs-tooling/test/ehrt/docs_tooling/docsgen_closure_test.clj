(ns ehrt.docs-tooling.docsgen-closure-test
  "ADR-0155, closing register row L3-1 -- the CLASS ADR-0136 opened and
  that ADR-0149 and ADR-0152 each then closed for exactly one more
  artifact.

  `.github/workflows/test.yml`'s freshness comment has stated the rule
  in prose since ADR-0136: *\"if a new derived file appears, it goes on
  a make target AND on the diff list below, same commit.\"* Review 4
  measured what enforced it: **2 of 12 docsgen leaves and 2 of 19
  diff-list paths**. A sub-agent dropped `operators-doc` and
  `palgebra-examples` from `docsgen:` and deleted `docs/operators.md`
  plus the three palgebra `.mermaid` from the diff list, then ran the
  real per-push lane: exit 0, 348 blocks / 3,960 tests / 17,758
  assertions, zero failures. Either surface could be voided by editing
  one line, silently.

  This namespace replaces per-artifact assertions with one closure gate
  over the whole population, per author ruling R4-Q10 (d).

  WHAT IT TRUSTS, stated plainly, because (b) is the weaker of the two
  options the plan offered: the gate derives each leaf's write set from
  what that leaf's own recipe NAMES -- its `-o` flags, its `:out` /
  `:index-out` / `:pages-dir` arguments, its `cp` destinations, and its
  closing `@echo \"Regenerated ...\"`. A recipe that wrote a file it
  never names would still escape. That residue is deliberate: option
  (a) -- enumerating the write set by RUNNING docsgen -- is the stronger
  property and belongs at integration tier, where the `traces` leaf's
  ~84s can be carried. ADR-0155's Step 0 ran (a) ONCE by hand in a
  scratch worktree and reconciled the measured write set against this
  gate's recipe-derived set; the delta is recorded there.

  Five claims, each red at this session's own red-first commit:

  (a) DIFF-LIST CLOSURE, BOTH DIRECTIONS -- every file a docsgen leaf's
      recipe declares is covered by the CI diff list, and every
      diff-list entry covers at least one declared file. Set equality
      at the level of what is actually gated, so dropping a leaf from
      `docsgen:` OR a path from the list is red. Both populations are
      asserted non-empty (`rulings.md#R-empty-population-is-red`), and
      every individual leaf is asserted to declare at least one output:
      an extractor that silently stopped matching would otherwise make
      the whole claim vacuous, which is this review's own cross-
      dimension pattern (a gate whose population is narrower than the
      class it is read as enforcing).

  (b) THE FREEZE IS OFF DOCSGEN (register L3-2) -- `event-schema-freeze`
      is not a transitive prerequisite of `docsgen`. That target
      re-freezes the BASELINE the event-schema stability gate measures
      against; on `docsgen` it would make the gate compare the schema
      against itself -- always empty, always green, forever -- and
      because the baseline is (correctly) off CI's diff list, nothing
      would be diffed either. `event-schema-baseline.edn`'s header
      claims this; until now the claim was enforced by the sentence.

  (c) THE GENERATED -> GENERATED EDGE IS DECLARED (register L3-4) --
      `docs/dev/pipeline.md` is a generated artifact that is also a
      line-counted INPUT to `.agents/state-derived.md`. The edge existed
      only as the left-to-right ORDER of `docsgen:`'s prerequisite list;
      `make -j8 pipeline state-derived` produced a stale
      `state-derived.md` against a changed `pipeline.md`, twice.

  (d) USE-CASES PAGE CLOSURE (register L3-9) --
      `set(docs/use-cases/*.md) == set(case ids)`. `write-use-cases!`
      did `.mkdirs` then `spit` and never pruned, so a case dropped from
      `use-cases.edn` left a page that outlived its own source: the
      index loses the row, the page is untouched, `git diff --exit-code
      docs/use-cases/` sees no change, green forever.

  (e) PALGEBRA EXAMPLE PAIRING (register L3-10) -- the recipe hardcoded
      three converter calls against FIVE `*-equations.txt`, with zero
      test references to the directory. Step 0 read both unrendered
      files and the recipe comment: `lemon-pie` and `decision-monad`
      ship as equation sources only, deliberately (they are the
      vendoring surface, not the rendered-example surface). So this is
      NOT a blanket pairing assertion -- it asserts the DECLARED
      exception list, which the recipe comment now states in a
      machine-readable line, and requires every other equations file to
      be rendered by the recipe."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.docs-tooling.make-graph :as mg]
            [ehrt.docs-tooling.usecases :as usecases]))

;; ---- the tracked tree, which is all a CI diff list can see ----

(defn- tracked-files []
  (let [{:keys [exit out err]} (shell/sh "git" "ls-files")]
    (when-not (zero? exit)
      (throw (ex-info "git ls-files failed -- cannot derive the gated population"
                      {:exit exit :err err})))
    (->> (str/split-lines out) (remove str/blank?) set)))

;; ---- each docsgen leaf's DECLARED write set, from its own recipe ----

(def ^:private path-token
  ;; A repo-relative path names at least one directory, so requiring a
  ;; `/` is what separates `docs/formats.md` from the prose words in the
  ;; same @echo line. `'` is excluded on purpose: the formats leaf says
  ;; "Regenerated docs/formats.md's event-log section".
  #"[A-Za-z0-9_.][A-Za-z0-9_.*-]*(?:/[A-Za-z0-9_.*-]+)+/?")

(defn- glob->pattern
  "A shell-ish glob to a regex over whole paths: `**` spans directories,
  `*` does not. Tokenised rather than substituted, so no sentinel
  character is needed and every literal run is quoted rather than
  hand-escaped."
  [glob]
  (->> (re-seq #"\*\*|\*|[^*]+" glob)
       (map (fn [part]
              (case part
                "**" ".*"
                "*" "[^/]*"
                (java.util.regex.Pattern/quote part))))
       str/join
       re-pattern))

(defn- expand
  "One declared token to the TRACKED files it names: itself if it is a
  tracked file, every tracked file beneath it if it is a directory,
  every tracked file matching it if it is a glob. `target/` is scratch
  and never tracked, so it drops out here rather than by a name rule."
  [tracked token]
  (let [token (str/replace token #"/$" "")]
    (cond
      (str/includes? token "*")
      (set (filter #(re-matches (glob->pattern token) %) tracked))

      (contains? tracked token) #{token}

      (.isDirectory (io/file token))
      (set (filter #(str/starts-with? % (str token "/")) tracked))

      :else #{})))

(defn- declared-output-tokens
  "The output paths a recipe NAMES, by the four forms this Makefile uses
  to name one. Inputs are named by other forms (a bare argument, a
  `:pipeline-edn` / `:use-cases-edn` key) and are deliberately not
  collected -- the claim is about what a leaf WRITES."
  [recipe-lines]
  (let [joined (str/join "\n" recipe-lines)]
    (concat
     ;; -o <path>          the python converter's output flag
     (map second (re-seq #"-o\s+(\S+)" joined))
     ;; :out / :index-out / :pages-dir '"<path>"'   the -X entry points
     (map second (re-seq #"(?::out|:index-out|:pages-dir)\s+'\"([^\"]+)\"'" joined))
     ;; cp <src> <dst>     the sim-theory splice
     (map second (re-seq #"cp\s+\S+\s+(\S+)" joined))
     ;; @echo "Regenerated <paths> ..."   every leaf's own closing line.
     ;; `path-token` carries no capturing group, so `re-seq` yields the
     ;; matched strings themselves -- not match vectors.
     (mapcat #(re-seq path-token %)
             (map second (re-seq #"@echo\s+\"Regenerated([^\"]*)\"" joined))))))

(defn- leaf-declared-files
  "The tracked files docsgen leaf `leaf` declares it writes."
  [makefile-text tracked leaf]
  (->> (mg/target-recipe makefile-text leaf)
       declared-output-tokens
       (mapcat #(expand tracked %))
       set))

(defn- covered-by
  "The tracked files one CI diff-list entry covers."
  [tracked entry]
  (if (str/ends-with? entry "/")
    (set (filter #(str/starts-with? % entry) tracked))
    (if (contains? tracked entry) #{entry} #{})))

;; ---- (a) diff-list closure ----

(deftest every-docsgen-leaf-declares-at-least-one-output-test
  (let [text (slurp mg/makefile)
        tracked (tracked-files)
        leaves (mg/target-prerequisites text "docsgen")]
    (is (pos? (count leaves))
        "sanity: `docsgen:` must have prerequisites -- an empty list makes every claim below vacuous")
    (is (pos? (count tracked))
        "sanity: `git ls-files` must return tracked files")
    (doseq [leaf (sort leaves)]
      (testing leaf
        (is (pos? (count (leaf-declared-files text tracked leaf)))
            (str "docsgen leaf `" leaf "` names no tracked output path in its own recipe.\n"
                 "Every leaf declares its write set -- by an `-o` flag, an `:out`/`:index-out`/"
                 "`:pages-dir` argument, a `cp` destination, or its closing "
                 "`@echo \"Regenerated <paths>\"`.\n"
                 "A leaf that names none is invisible to the closure gate below, which is exactly "
                 "how whole artifacts stayed ungated through ADR-0136, ADR-0149 and ADR-0152.\n"
                 "Recipe today: " (pr-str (mg/target-recipe text leaf))))))))

(deftest the-docsgen-write-set-and-the-ci-diff-list-are-the-same-population-test
  (let [text (slurp mg/makefile)
        tracked (tracked-files)
        leaves (mg/target-prerequisites text "docsgen")
        declared (into {} (map (fn [l] [l (leaf-declared-files text tracked l)])) leaves)
        declared-files (reduce into #{} (vals declared))
        entries (mg/freshness-diff-paths (slurp mg/workflow))
        covered (reduce into #{} (map #(covered-by tracked %) entries))]
    (is (pos? (count entries))
        "sanity: the freshness step must name paths -- if this is empty the extractor broke and both claims below are silently vacuous")
    (is (pos? (count declared-files))
        "sanity: the docsgen leaves must declare files -- an empty write set passes the forward claim trivially")
    (testing "forward: every file a docsgen leaf writes is on CI's freshness diff list"
      (let [ungated (sort (remove covered declared-files))]
        (is (empty? ungated)
            (str "docsgen writes these tracked files and CI's freshness diff does not cover them:\n  "
                 (str/join "\n  " ungated)
                 "\nA generated file no `git diff --exit-code` compares is regenerated by a target "
                 "whose output nothing checks -- the ungated state ADR-0136 found five artifacts in, "
                 "three of them already stale against their own converter. Add each to the diff list "
                 "in `.github/workflows/test.yml`, same commit."))))
    (testing "reverse: every diff-list entry is produced by some docsgen leaf"
      (let [unproduced (sort (remove #(seq (filter declared-files (covered-by tracked %))) entries))]
        (is (empty? unproduced)
            (str "CI's freshness diff names these paths and no docsgen leaf declares writing them:\n  "
                 (str/join "\n  " unproduced)
                 "\nTwo readings, and they need different fixes: the list is STALE (the generator was "
                 "retired -- drop the path), or a generator SILENTLY STOPPED writing it (the path is "
                 "right and the recipe regressed -- fix the recipe). Do not guess. What each leaf "
                 "declares today:\n  "
                 (str/join "\n  " (map (fn [[l fs]] (str l " -> " (str/join ", " (sort fs))))
                                       (sort declared)))))))))

;; ---- (b) L3-2: the freeze is off docsgen ----

(deftest event-schema-freeze-is-not-reachable-from-docsgen-test
  (let [text (slurp mg/makefile)
        reachable (mg/transitive-prerequisites text "docsgen")]
    (is (pos? (count reachable))
        "sanity: docsgen must reach targets -- an empty graph passes the claim below trivially")
    (is (not (contains? reachable "event-schema-freeze"))
        (str "`event-schema-freeze` must NOT be reachable from `docsgen`.\n"
             "It re-freezes the BASELINE that `non-additive-change-requires-a-version-bump` measures "
             "against -- the repo's only gate that can force an event-schema version bump. On docsgen "
             "it would compare the schema against itself: always empty, always green, forever; and "
             "because the baseline is (correctly) off CI's diff list, nothing would be diffed either. "
             "Run it ONLY when bumping `schema-version`.\n"
             "Reachable from docsgen today: " (pr-str (sort reachable))))))

;; ---- (c) L3-4: the generated -> generated edge ----

(deftest state-derived-declares-its-dependency-on-pipeline-test
  (let [text (slurp mg/makefile)
        prereqs (mg/target-prerequisites text "state-derived")]
    (is (some? prereqs) "a `state-derived:` rule must exist")
    (is (some #{"pipeline"} prereqs)
        (str "`state-derived:` must declare `pipeline` as a prerequisite.\n"
             "`docs/dev/pipeline.md` is itself generated AND is a line-counted input to "
             "`.agents/state-derived.md`, so this is a generated -> generated edge. It existed only "
             "as the left-to-right order of `docsgen:`'s prerequisite list, which `make -j` is free "
             "to ignore: `make -j8 pipeline state-derived` produced a stale state-derived.md against "
             "a changed pipeline.md, twice.\n"
             "state-derived prerequisites today: " (pr-str prereqs)))))

;; ---- (d) L3-9: use-cases page closure ----

(def ^:private use-cases-edn "components/corpus/docs/use-cases.edn")
(def ^:private pages-dir "docs/use-cases")

(deftest the-use-case-pages-are-exactly-the-use-case-ids-test
  (let [case-ids (->> (usecases/read-use-cases-edn use-cases-edn) :cases (map (comp name :id)) set)
        pages (->> (.listFiles (io/file pages-dir))
                   (map #(.getName %))
                   (filter #(str/ends-with? % ".md"))
                   (map #(str/replace % #"\.md$" ""))
                   set)]
    (is (pos? (count case-ids))
        "sanity: use-cases.edn must declare cases -- an empty case set would pass by matching an empty page set")
    (is (pos? (count pages))
        "sanity: docs/use-cases/ must hold pages")
    (is (= case-ids pages)
        (str "the committed page set and the case set must be equal.\n"
             "  pages with no case (a page that outlived its own source): "
             (pr-str (sort (remove case-ids pages))) "\n"
             "  cases with no page (the generator did not write one):     "
             (pr-str (sort (remove pages case-ids))) "\n"
             "`write-use-cases!` used to `.mkdirs` then `spit` and never prune, so a case dropped "
             "from use-cases.edn left its page behind: the index loses the row, the page is "
             "untouched, `git diff --exit-code docs/use-cases/` sees no change, and the orphan "
             "lives on, green, indefinitely."))))

;; ---- (e) L3-10: palgebra pairing, against the DECLARED exceptions ----

(def ^:private examples-dir "components/palgebra/examples")

(defn- example-file-names []
  (map #(.getName %) (.listFiles (io/file examples-dir))))

(defn- declared-example-exceptions
  "The equations files the `palgebra-examples` recipe comment declares
  ship WITHOUT a rendered `.mermaid`. Read from the Makefile so the
  exception list is stated once, where the maintainer already reads it,
  rather than duplicated as a hand list here."
  [makefile-text]
  (when-let [[_ names] (re-find #"(?m)^#\s*EXAMPLES-WITHOUT-MERMAID:(.*)$" makefile-text)]
    (set (remove str/blank? (str/split (str/trim names) #"\s+")))))

(deftest every-palgebra-example-is-rendered-or-declared-unrendered-test
  (let [text (slurp mg/makefile)
        names (example-file-names)
        stems (->> names
                   (filter #(str/ends-with? % "-equations.txt"))
                   (map #(str/replace % #"-equations\.txt$" ""))
                   set)
        rendered (->> (mg/target-recipe text "palgebra-examples")
                      (mapcat #(map second (re-seq #"-o\s+(\S+)" %)))
                      (map #(str/replace % (str examples-dir "/") ""))
                      set)
        exceptions (declared-example-exceptions text)
        rendered-stems (set (keep (fn [s] (when (some #(str/starts-with? % (str s "-flow")) rendered) s))
                                  stems))
        accounted (into rendered-stems (or exceptions #{}))]
    (is (pos? (count stems))
        "sanity: the examples directory must hold *-equations.txt -- an empty population passes every claim below")
    (is (pos? (count rendered))
        "sanity: the `palgebra-examples` recipe must render something")
    (is (some? exceptions)
        (str "the `palgebra-examples` recipe comment must carry a machine-readable\n"
             "`# EXAMPLES-WITHOUT-MERMAID: <stems>` line.\n"
             "ADR-0155's Step 0 read both unrendered files and confirmed the omission is deliberate "
             "-- lemon-pie and decision-monad are the vendoring surface, not the rendered-example "
             "surface -- so this gate asserts a DECLARED exception list rather than a blanket "
             "pairing. Undeclared, that same state is indistinguishable from drift."))
    (testing "every equations file is either rendered by the recipe or a declared exception"
      (is (= stems accounted)
          (str "unaccounted-for example(s): " (pr-str (sort (remove accounted stems))) "\n"
               "A sixth example added tomorrow lands in exactly the ungated state ADR-0136 found the "
               "first three in -- already stale against their own converter. Either render it on "
               "`palgebra-examples` (and add its .mermaid to CI's diff list, same commit) or declare "
               "it on the EXAMPLES-WITHOUT-MERMAID line.")))
    (testing "a declared exception really has no committed .mermaid"
      (doseq [s (sort (or exceptions #{}))]
        (is (empty? (filter #(str/starts-with? % (str s "-flow")) names))
            (str s " is declared as shipping without a rendered .mermaid, but one is committed. "
                 "A committed .mermaid regenerated by no target IS the ungated state: put it on "
                 "`palgebra-examples` and drop the declaration."))))
    (testing "every rendered .mermaid has its equations source"
      (doseq [r (sort rendered)]
        (is (some #(str/starts-with? r (str % "-flow")) stems)
            (str r " is rendered by `palgebra-examples` but no sibling *-equations.txt names it"))))))
