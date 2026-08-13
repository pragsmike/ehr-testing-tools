(ns ehrt.docs-tooling.demo-exerciser-fresh
  "R3 (notes/ADRs.md ADR-0113, author verbatim: 'The demos must be known
  to work, and exercised as documented to make sure they actually play
  out as written'): generalizes ehrt.docs-tooling.quickstart-fresh's own
  single-fence, single-BEGIN/END-block identity check to a scenario
  README with MULTIPLE fenced blocks -- some ```bash command fences,
  some plain ``` transcript blocks mixing a `$ `-prefixed command with
  its own witnessed output inline
  (demos/scenarios/ed-tuesday/README.md's own 'Ground truth is
  invariant' and 'The wrapper itself' sections are exactly this style).

  Extraction walks every fenced block in document order and keeps
  exactly the command lines a reader would actually type: a line
  starting `bin/ehrt ` opens a taught command; a line starting `$ ` is a
  transcript command with the prompt stripped; any line immediately
  following a kept line that itself ended in a trailing backslash is a
  continuation, kept verbatim regardless of its own prefix -- the same
  unmerged-continuation-lines convention quickstart-fresh already uses.
  Every other fenced line -- prose asides, board-snapshot or
  JSON-payload output carrying no command prefix -- is not a taught
  command and is skipped. Blank lines are always skipped, inside or
  outside a fence.

  The script side reuses quickstart-demo's own `expect`/`expect_eval`
  wrapper shape verbatim (see bin/demo-exerciser-ed-tuesday); unwrapping
  is the identical regex pair, applied to a differently-named BEGIN/END
  marker pair so the two scripts' own freshness checks never cross-match
  each other's fence."
  (:require [clojure.string :as str]))

(defn- blank-line?
  [line]
  (= (str/trim line) ""))

(defn- fence-marker?
  [line]
  (str/starts-with? (str/trim line) "```"))

(defn- command-start?
  [line]
  (or (str/starts-with? (str/trim line) "bin/ehrt ")
      (str/starts-with? line "$ ")))

(defn- strip-prompt
  [line]
  (if (str/starts-with? line "$ ") (subs line 2) line))

(defn- continuation-of?
  [line]
  (str/ends-with? (str/trimr line) "\\"))

(defn readme-command-lines
  "Every taught command line across every fenced block in `readme-path`,
  in document order."
  [readme-path]
  (loop [lines (str/split-lines (slurp readme-path))
         in-fence? false
         continuing? false
         out []]
    (if (empty? lines)
      out
      (let [line (first lines)
            more (rest lines)]
        (cond
          (fence-marker? line)
          (recur more (not in-fence?) false out)

          (not in-fence?)
          (recur more in-fence? false out)

          (blank-line? line)
          (recur more in-fence? false out)

          continuing?
          (recur more in-fence? (continuation-of? line) (conj out line))

          (command-start? line)
          (let [content (strip-prompt line)]
            (recur more in-fence? (continuation-of? content) (conj out content)))

          :else
          (recur more in-fence? false out))))))

(def ^:private expect-re #"expect\s+\d+\s+(.*)")
(def ^:private expect-eval-re #"expect_eval\s+\d+\s+'(.*)'")

(defn- unwrap-script-line
  "Strips the `expect CODE `/`expect_eval CODE '...'` wrapper off the
  first physical line of a taught command, recovering the README's own
  text. A continuation line (indented, no wrapper) passes through
  unchanged."
  [line]
  (or (second (re-matches expect-re line))
      (second (re-matches expect-eval-re line))
      line))

(defn script-command-lines
  "bin/demo-exerciser-ed-tuesday's taught command lines, unwrapped, in
  order -- the lines strictly between its own BEGIN/END marker pair.
  nil if the script (or its marker pair) doesn't exist yet."
  [script-path]
  (when (.exists (java.io.File. ^String script-path))
    (let [lines (str/split-lines (slurp script-path))
          open "# BEGIN ed-tuesday commands (verbatim from demos/scenarios/ed-tuesday/README.md)"
          close "# END ed-tuesday commands"
          after-open (seq (rest (drop-while #(not= (str/trim %) open) lines)))]
      (when after-open
        (->> (take-while #(not= (str/trim %) close) after-open)
             (remove blank-line?)
             (mapv unwrap-script-line))))))

(defn- diverge-at
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
  when :ok? is true; see diverge-at above otherwise. :script-count and
  :script-lines are 0/nil when the script doesn't exist yet (RED state,
  before bin/demo-exerciser-ed-tuesday is written)."
  ([] (check {}))
  ([{:keys [readme-path script-path]
     :or {readme-path "demos/scenarios/ed-tuesday/README.md"
          script-path "bin/demo-exerciser-ed-tuesday"}}]
   (let [readme-lines (readme-command-lines readme-path)
         script-lines (or (script-command-lines script-path) [])
         divergence (diverge-at readme-lines script-lines)]
     {:ok? (nil? divergence)
      :readme-count (count readme-lines)
      :script-count (count script-lines)
      :divergence divergence})))
