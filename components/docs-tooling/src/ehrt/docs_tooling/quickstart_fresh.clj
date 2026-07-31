(ns ehrt.docs-tooling.quickstart-fresh
  "DOC-5 (.agents/plans/user-docs.md): README.md's Quickstart fence and
  bin/quickstart-demo are two independently-edited copies of the same
  taught command sequence, and nothing forces them to stay in sync --
  the classic doc-rot site AUTHORS-GUIDE.md sec7's ENF-1 analogy warns
  about. This namespace is the fast, per-push half of DOC-5's two gates
  (the slow half, actually running the commands, is
  bin/quickstart-demo / `make quickstart-demo`, integration-tier).

  The invariant this check encodes: every command README.md's
  Quickstart fence teaches is a command bin/quickstart-demo runs, in
  the same order, and vice versa. It is structural, per AUTHORS-GUIDE.md
  sec7's craft discipline, not a substring search: extraction is
  anchored to the one ```sh fence in README.md and the one BEGIN/END
  marker pair in bin/quickstart-demo, each producing an ordered list of
  command lines (comment-only and blank lines stripped from both sides
  the same way; continuation lines kept as their own list entries,
  unmerged, so a plain line-for-line comparison is enough); a naive
  substring search over either file -- or worse, over a pack that
  includes this session's own archived prompt, which quotes the fence
  verbatim -- would be structurally guaranteed to false-positive
  (sec7's self-reference hazard).

  The script's commands are wrapped in `expect`/`expect_eval` (see
  bin/quickstart-demo) so each one carries a per-step exit-code
  assertion; `unwrap-script-line` strips exactly that wrapper back off
  the first physical line of each taught command, recovering the same
  text README.md teaches. Continuation lines (the 2nd/3rd physical line
  of a multi-line command) are copied verbatim in the script and need
  no unwrapping."
  (:require [clojure.string :as str]))

(defn- comment-or-blank?
  [line]
  (let [trimmed (str/trim line)]
    (or (= trimmed "") (str/starts-with? trimmed "#"))))

(defn- fence-lines
  "Lines strictly between a line whose trimmed text equals `open` and
  the next line whose trimmed text equals `close`, comment-only and
  blank lines removed, order preserved. nil if `open` never appears."
  [lines open close]
  (let [after-open (seq (rest (drop-while #(not= (str/trim %) open) lines)))]
    (when after-open
      (->> (take-while #(not= (str/trim %) close) after-open)
           (remove comment-or-blank?)
           vec))))

(defn readme-command-lines
  "The Quickstart fence's taught command lines, in order."
  [readme-path]
  (fence-lines (str/split-lines (slurp readme-path)) "```sh" "```"))

(def ^:private expect-re #"expect\s+\d+\s+(.*)")
(def ^:private expect-eval-re #"expect_eval\s+\d+\s+'(.*)'")

(defn- unwrap-script-line
  "Strips the `expect CODE `/`expect_eval CODE '...'` wrapper off the
  first physical line of a taught command, recovering README.md's own
  text. A continuation line (indented, no wrapper) passes through
  unchanged."
  [line]
  (or (second (re-matches expect-re line))
      (second (re-matches expect-eval-re line))
      line))

(defn script-command-lines
  "bin/quickstart-demo's taught command lines, unwrapped, in order."
  [script-path]
  (some->> (fence-lines (str/split-lines (slurp script-path))
                         "# BEGIN quickstart commands (verbatim from README.md's Quickstart fence)"
                         "# END quickstart commands")
           (mapv unwrap-script-line)))

(defn- diverge-at
  "First index where readme-lines and script-lines differ, as
  {:index :readme :script} (::missing for a list that ran out first);
  nil if the two lists are equal."
  [readme-lines script-lines]
  (let [total (max (count readme-lines) (count script-lines))
        idx (first (filter #(not= (get readme-lines %) (get script-lines %))
                            (range total)))]
    (when idx
      {:index idx
       :readme (get readme-lines idx ::missing)
       :script (get script-lines idx ::missing)})))

(defn check
  "{:ok? :readme-count :script-count :divergence}. :divergence is nil
  when :ok? is true; see diverge-at above otherwise."
  ([] (check {}))
  ([{:keys [readme-path script-path]
     :or {readme-path "README.md" script-path "bin/quickstart-demo"}}]
   (let [readme-lines (readme-command-lines readme-path)
         script-lines (script-command-lines script-path)
         divergence (diverge-at readme-lines script-lines)]
     {:ok? (nil? divergence)
      :readme-count (count readme-lines)
      :script-count (count script-lines)
      :divergence divergence})))

(defn- render-line
  [v]
  (if (= v ::missing) "(no line here -- this side ran out first)" (pr-str v)))

(defn quickstart-fresh!
  "-X-invokable: `make quickstart-fresh`. Prints the invariant and the
  first diverging line pair on failure, exits non-zero. Accepts the
  same :readme-path/:script-path opts as `check` (default README.md /
  bin/quickstart-demo) -- overridable for a scratch-copy sensitivity
  check; `make quickstart-fresh` itself always runs with the defaults."
  [opts]
  (let [{:keys [ok? readme-count script-count divergence]} (check opts)]
    (if ok?
      (println (str "quickstart-fresh: OK -- " readme-count
                     " commands, README.md's Quickstart fence and bin/quickstart-demo agree line-for-line, in order"))
      (do
        (println "quickstart-fresh: FAILED")
        (println "  invariant: every command README.md's Quickstart fence teaches is a command bin/quickstart-demo runs, in the same order, and vice versa")
        (println (str "  README.md: " readme-count " command lines -- bin/quickstart-demo: " script-count))
        (println (str "  first divergence at command line " (inc (:index divergence)) ":"))
        (println (str "    README.md ........ " (render-line (:readme divergence))))
        (println (str "    quickstart-demo ... " (render-line (:script divergence))))
        (System/exit 1)))))
