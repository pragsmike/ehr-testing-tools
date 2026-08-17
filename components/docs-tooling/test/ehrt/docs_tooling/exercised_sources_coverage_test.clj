(ns ehrt.docs-tooling.exercised-sources-coverage-test
  "ADR-0148, closing `roadmap.md#exercised-row-gate-closure` -- the part
  ADR-0146 did NOT close. U-15 found that `bin/usecase-custom-emitter`
  was the one row of the nine in `exercised-sources.edn` with no live
  `check-entry` case, added that case, and left the general problem
  standing: nothing asserted that EVERY row has one, so the next row
  added could go ungated in exactly the same silence.

  `rulings.md#R-population-closure` names the shape -- enumerate the
  population from the tree, diff it against whatever claims to cover
  it -- rather than a tenth hand-written case. So this namespace holds
  ONE test over `load-registry` in place of nine per-row cases:
  `check-all` is run over the register itself, and a row added tomorrow
  is gated the moment it is registered, with no test edit at all.

  Four claims, in the order they matter:

  (a) COVERAGE -- `check-all` over the live register returns one result
      per row and every result is fresh. This is the closure itself.

  (b) THE INSTRUMENT FAILS -- a synthetic two-row registry with one row
      seeded diverged yields exactly one failure, naming that row. A
      coverage test that cannot go red is a coverage claim, not a gate
      (`rulings.md#R-red-before-green`'s own reasoning, applied to the
      instrument rather than to the fix).

  (c) NO TRIVIAL PASS -- two ways a freshness check could report fresh
      while proving nothing, both pinned:
        - a script that wraps a taught line in `bash -c`, which the
          unwrapper cannot read (U-15's second layer). Already caught
          as a loud divergence before this session -- pinned here so it
          stays caught, never re-derived.
        - a row whose source yields ZERO taught command lines. Comparing
          an empty list to an empty list matched trivially and reported
          `:ok? true` -- a genuine silent pass, found by this session's
          own probe and fixed in `strip-fresh/check-entry`. THIS is the
          `bash -c` hazard's real sibling: not an unreadable line, an
          absent population.

  (d) THE DUAL -- a page cannot claim \"exercised\" by a script the
      register never gates. Every exerciser-shaped script the tree
      contains is a register row; every register row's script exists;
      every `bin/` exerciser a doc page cites is a register row. All
      three populations are enumerated from the tree and the docs, never
      listed here -- a list in the test is the same defect one layer up."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.docs-tooling.exercised-sources :as reg]
            [ehrt.docs-tooling.strip-fresh :as sf]))

;; ---- (a) coverage: check-all over the live register ----

(defn- row-id
  "A row's own identity in a failure message -- source and script
  together, because neither alone is unique (README.md owns two rows)."
  [{:keys [source script]}]
  (str source " -> " script))

(defn- failure-line
  "One register row's own failure, naming the row, its source, its
  script, and the FIRST diverging line on both sides -- so a red run
  says which page and which script disagree and where, without the
  reader opening either file."
  [{:keys [source script divergence readme-count script-count]}]
  (str "  " source " -> " script
       "\n    source lines: " readme-count ", script lines: " script-count
       (when divergence
         (str "\n    first divergence at index " (:index divergence)
              "\n      source: " (pr-str (:readme divergence))
              "\n      script: " (pr-str (:script divergence))))))

(deftest every-registered-row-is-fresh-test
  (let [rows (reg/load-registry)
        results (sf/check-all rows)
        failures (remove :ok? results)]
    (is (pos? (count rows))
        "sanity: the register must load real rows -- a silently empty registry would make every claim below vacuous")
    (is (= (count rows) (count results))
        "check-all must return exactly one result per register row")
    (is (= (mapv row-id rows) (mapv row-id results))
        "results must come back in register order, each carrying its own row's identity")
    (is (empty? failures)
        (str "exercised-sources rows whose page and script no longer teach the same commands:\n"
             (str/join "\n" (map failure-line failures))))))

;; ---- (b) the instrument must be able to fail ----

(defn- temp-file!
  [suffix content]
  (let [f (java.io.File/createTempFile "coverage-fixture" suffix)]
    (spit f content)
    (.getAbsolutePath f)))

(defn- synthetic-row
  [doc-commands script-commands]
  {:source (temp-file! ".md" (str "```sh\n" (str/join "\n" doc-commands) "\n```\n"))
   :script (temp-file! ".sh" (str "# BEGIN t\n"
                                  (str/join "\n" (map #(str "expect 0 " %) script-commands))
                                  "\n# END t\n"))
   :extraction :single-fence
   :fence-lang "sh"
   :marker-open "# BEGIN t"
   :marker-close "# END t"})

(deftest coverage-over-a-seeded-registry-reports-exactly-the-diverged-row-test
  (let [fresh-row (synthetic-row ["bin/ehrt help"] ["bin/ehrt help"])
        diverged-row (synthetic-row ["bin/ehrt corpus generate"] ["bin/ehrt corpus generate --typo"])
        results (sf/check-all [fresh-row diverged-row])
        failures (remove :ok? results)]
    (is (= 2 (count results)))
    (is (= 1 (count failures))
        "exactly one of the two synthetic rows diverges -- if this test cannot distinguish them, the coverage claim above proves nothing")
    (is (= (row-id diverged-row) (row-id (first failures)))
        "the failure must name the DIVERGED row, not merely report that something failed")
    (is (= "bin/ehrt corpus generate --typo" (:script (:divergence (first failures))))
        "and it must carry the first diverging line, so the message is actionable")
    (is (str/includes? (failure-line (first failures)) "--typo")
        "the rendered failure message the reader actually sees names the diverging line")))

;; ---- (c) no trivial pass ----

;; U-15's second layer, made a permanent case. `bin/usecase-custom-
;; emitter` wrapped a taught redirect as `expect 0 bash -c '...'`, and
;; `strip-fresh`'s unwrapper reads `expect` and `expect_eval`, not a
;; `bash -c` wrapper -- so the row was structurally ungateable until it
;; was rewritten with `expect_eval` (`rulings.md#R-taught-shell-lines-
;; use-expect-eval`). GREEN ON ARRIVAL, disclosed: probed at 5c1d73e
;; before this test was written, the wrapper already produced a loud
;; divergence carrying the wrapper text verbatim, never a silent pass.
;; It is pinned rather than fixed, so a future widening of the unwrapper
;; that made `bash -c` merely "readable" -- and therefore silently equal
;; to the page's own bare line -- fails here by name.

(deftest a-bash-c-wrapped-taught-line-is-never-reported-fresh-test
  (let [taught "bin/ehrt sim run --seed 42 > out/events.edn"
        doc (temp-file! ".md" (str "```sh\n" taught "\n```\n"))
        script (temp-file! ".sh" (str "# BEGIN t\n"
                                      "expect 0 bash -c '" taught "'\n"
                                      "# END t\n"))
        result (sf/check-entry {:source doc :script script :extraction :single-fence
                                :fence-lang "sh"
                                :marker-open "# BEGIN t" :marker-close "# END t"})]
    (is (false? (:ok? result))
        "a taught line the unwrapper cannot read must never be certified fresh")
    (is (str/includes? (str (:script (:divergence result))) "bash -c")
        "and the divergence must show the wrapper verbatim, so the reader sees the CAUSE rather than a mismatch")))

;; The absent-population hazard, found by this session's own probe and
;; RED before the `check-entry` guard landed. Three ways a source yields
;; zero taught command lines -- no fence of the row's own language, a
;; fence that is entirely comments, or a `:paired` row whose source
;; holds no genuine pair -- and in all three the old code compared `[]`
;; to `[]`, found no divergence, and reported `:ok? true` with counts
;; 0/0. That is a freshness check certifying a page-script pair no
;; command ever passed through: the same "proves nothing while looking
;; green" shape as U-15, arriving through an empty population rather
;; than an unreadable line.

(deftest a-source-yielding-no-taught-commands-is-never-reported-fresh-test
  (let [empty-script (temp-file! ".sh" "# BEGIN t\n\n# END t\n")
        check (fn [doc extraction fence-lang]
                (sf/check-entry {:source doc :script empty-script :extraction extraction
                                 :fence-lang fence-lang
                                 :marker-open "# BEGIN t" :marker-close "# END t"}))]
    (testing "no fence of the row's own language"
      (let [r (check (temp-file! ".md" "```mermaid\nflowchart LR\n```\n") :single-fence "sh")]
        (is (false? (:ok? r)))
        (is (= 0 (:readme-count r)))
        (is (= ::sf/no-taught-commands (:readme (:divergence r))))))
    (testing "a fence that is entirely comments"
      (let [r (check (temp-file! ".md" "```sh\n# only a comment\n```\n") :single-fence "sh")]
        (is (false? (:ok? r)))
        (is (= 0 (:readme-count r)))
        (is (= ::sf/no-taught-commands (:readme (:divergence r))))))
    (testing "a :paired row whose source holds no genuine pair"
      (let [r (check (temp-file! ".md" "```bash\nfoo\n```\n\nprose, not an output fence\n") :paired "bash")]
        (is (false? (:ok? r)))
        (is (= 0 (:readme-count r)))
        (is (= ::sf/no-taught-commands (:readme (:divergence r))))))))

(deftest the-vacuous-guard-does-not-fire-on-a-real-row-test
  ;; The guard above rejects zero-command sources. Every live row must
  ;; therefore carry at least one taught command -- asserted separately
  ;; from (a) so a future regression that emptied an extraction would
  ;; name ITSELF here rather than surfacing as an opaque freshness
  ;; failure.
  (let [results (sf/check-all (reg/load-registry))]
    (is (every? #(pos? (:readme-count %)) results)
        (str "rows whose source extraction yields no taught commands: "
             (pr-str (map row-id (remove #(pos? (:readme-count %)) results)))))))

;; ---- (d) the dual: a cited or existing exerciser must be a row ----

(def ^:private exerciser-shaped?
  "A `bin/` script whose NAME says it exercises a doc page: the
  `usecase-` prefix the use-case exercisers carry, or the `exerciser`
  infix the demo exercisers carry. Derived from the naming convention,
  not from a list -- a script named by this convention that is not a
  register row is exactly the gap (d) exists to catch. `bin/quickstart-
  demo` and `bin/readme-what-you-get` are register rows under neither
  pattern; they are covered by the rows-have-scripts direction below."
  (fn [script-name]
    (or (str/starts-with? script-name "usecase-")
        (str/includes? script-name "exerciser"))))

(defn- tree-exerciser-scripts
  "Every exerciser-shaped script `bin/` actually contains, as repo-
  relative paths -- the population, enumerated from the tree."
  []
  (->> (.listFiles (io/file "bin"))
       (filter #(.isFile %))
       (map #(.getName %))
       (filter exerciser-shaped?)
       (map #(str "bin/" %))
       sort
       vec))

(deftest every-exerciser-shaped-script-in-the-tree-is-a-register-row-test
  (let [registered (set (map :script (reg/load-registry)))
        in-tree (tree-exerciser-scripts)
        ungated (remove registered in-tree)]
    (is (pos? (count in-tree))
        "sanity: the tree must actually hold exerciser-shaped scripts -- a silently empty population would make this gate vacuous")
    (is (empty? ungated)
        (str "exerciser-shaped scripts in bin/ that no exercised-sources row gates -- "
             "each one can exercise a page while diverging from it silently:\n  "
             (str/join "\n  " ungated)))))

(deftest every-register-row-names-a-script-that-exists-test
  (let [rows (reg/load-registry)
        missing (remove #(.exists (io/file (:script %))) rows)]
    (is (empty? missing)
        (str "register rows naming a script the tree does not contain:\n"
             (str/join "\n" (map row-id missing))))))

(def ^:private cite-re
  "A `bin/<name>` mention in prose or a fence. Deliberately broad: the
  exerciser-shaped filter, not this regex, decides what counts."
  #"bin/([A-Za-z0-9][A-Za-z0-9._-]*)")

(defn- doc-pages
  "Every reader-facing markdown page whose citations are load-bearing:
  the doc tree, the demo READMEs, and README.md itself. `.agents/**` is
  excluded on purpose -- prompts, session records and plans are the
  workspace talking to itself about scripts, including ones deferred or
  renamed, and a historical mention there is not a page claiming to be
  exercised."
  []
  (->> (concat (file-seq (io/file "docs")) (file-seq (io/file "demos")) [(io/file "README.md")])
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".md"))
       sort
       vec))

(defn- exerciser-cites
  "Every {:page :script} pair where a reader-facing page names an
  exerciser-shaped script that the tree actually contains -- cites from
  the docs, scripts from the tree, no list anywhere."
  []
  (let [in-tree (set (tree-exerciser-scripts))]
    (->> (doc-pages)
         (mapcat (fn [page]
                   (->> (re-seq cite-re (slurp page))
                        (map (fn [[_ nm]] (str "bin/" nm)))
                        (filter in-tree)
                        (map (fn [script] {:page (str page) :script script})))))
         distinct
         vec)))

(deftest every-exerciser-a-doc-page-cites-is-a-register-row-test
  (let [registered (set (map :script (reg/load-registry)))
        cites (exerciser-cites)
        uncovered (remove #(registered (:script %)) cites)]
    (is (pos? (count cites))
        "sanity: reader-facing pages must actually cite exercisers -- if this ever returns none, the extractor broke and the gate below is silently vacuous")
    (is (empty? uncovered)
        (str "pages citing an exerciser script the register never gates -- "
             "the page claims 'exercised' by a script nothing proves teaches it:\n  "
             (str/join "\n  " (map #(str (:page %) " cites " (:script %)) uncovered))))))
