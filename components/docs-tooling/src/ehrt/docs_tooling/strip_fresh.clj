(ns ehrt.docs-tooling.strip-fresh
  "ADR-0129 (manual-review dimension 1, strip executability, FAIL --
  `.agents/plans/2026-08-13-manual-review-1.md`): a generalized
  freshness check parameterized by one row of the exercised-sources
  register (`ehrt.docs-tooling.exercised-sources`), so a future cited
  source gains a freshness check by adding a register row rather than
  a new bespoke namespace.

  Two NEW extraction shapes, beside the two pre-existing, untouched
  ones (`ehrt.docs-tooling.quickstart-fresh`, `ehrt.docs-tooling.demo-
  exerciser-fresh` -- delegated to verbatim below, never reimplemented,
  so their own tests keep proving their own contract unmodified):

  - `:single-fence` -- the first fence of a given language in a doc,
    comment and blank lines stripped, everything else (including
    backslash-continuation lines) kept verbatim -- quickstart-fresh's
    own algorithm, generalized past its own hardcoded ```sh/README.md
    pair to an arbitrary (path, fence-lang). Used by the four new
    `docs/use-cases/*.md` register rows, each a single ```sh fence.

  - `:paired` -- every fence of a given language in a doc that is
    immediately followed (blank lines only, no intervening prose) by a
    fence of a DIFFERENT language yields a (command-lines,
    output-lines) pair; a same-language fence with no such pairing
    contributes command-lines with nil output-lines (the general
    extraction fn's own complete, documented behavior -- see
    `command-output-pairs` below). `check-entry`'s own :paired branch
    additionally filters to genuinely-paired blocks only before
    building its flattened command list, because a :paired-kind
    register row targets paired content specifically: README.md's own
    \"See it run\" ```bash fence (busy-tuesday) is the same language as
    the \"What you get\" section's two ```bash/```clojure pairs but is
    followed by prose, not an output fence, so `command-output-pairs`
    correctly returns it with nil :output-lines and `check-entry`
    correctly excludes it from the readme-what-you-get row's own
    command list -- no section-heading logic needed, adjacency alone
    disambiguates."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [ehrt.docs-tooling.demo-exerciser-fresh :as demo-fresh]
            [ehrt.docs-tooling.quickstart-fresh :as quickstart-fresh]))

;; ---- shared primitives (duplicated, deliberately, from quickstart-
;; fresh/demo-exerciser-fresh's own private fns -- both those
;; namespaces stay untouched, so neither's own tested contract becomes
;; answerable to a caller outside its own test file) ----

(defn- blank-line? [line] (= (str/trim line) ""))

(defn- comment-or-blank? [line]
  (let [trimmed (str/trim line)]
    (or (= trimmed "") (str/starts-with? trimmed "#"))))

(def ^:private expect-re #"expect\s+\d+\s+(.*)")
(def ^:private expect-eval-re #"expect_eval\s+\d+\s+'(.*)'")

(defn- unwrap-script-line
  [line]
  (or (second (re-matches expect-re line))
      (second (re-matches expect-eval-re line))
      line))

(defn script-command-lines
  "The taught command lines strictly between `marker-open`/`marker-close`
  in `script-path`, unwrapped and blank-stripped -- the same
  expect/expect_eval convention quickstart-fresh and demo-exerciser-
  fresh each already use, generalized to an arbitrary marker pair
  rather than each namespace's own hardcoded one. nil if `script-path`
  doesn't exist yet (the pre-script RED state) or the marker pair
  isn't found in it."
  [script-path marker-open marker-close]
  (when (.exists (io/file script-path))
    (let [lines (str/split-lines (slurp script-path))
          after-open (seq (rest (drop-while #(not= (str/trim %) marker-open) lines)))]
      (when after-open
        (->> (take-while #(not= (str/trim %) marker-close) after-open)
             (remove blank-line?)
             (mapv unwrap-script-line))))))

(defn- diverge-at
  [a-lines b-lines]
  (let [total (max (count a-lines) (count b-lines))
        idx (first (filter #(not= (get a-lines %) (get b-lines %)) (range total)))]
    (when idx
      {:index idx :readme (get a-lines idx ::missing) :script (get b-lines idx ::missing)})))

;; ---- new extraction shapes ----

(defn- fenced-blocks
  "Every fenced block in `path`, in document order, as {:lang string
  :lines [raw content lines, fence markers stripped]
  :blank-gap-before? bool} -- :blank-gap-before? is true when every
  line between this block's own open fence and the PREVIOUS block's
  own close fence is blank (true, vacuously, for the first block)."
  [path]
  (let [lines (str/split-lines (slurp path))]
    (loop [remaining lines
           current nil
           gap-lines []
           blocks []]
      (if (empty? remaining)
        blocks
        (let [line (first remaining)
              more (rest remaining)]
          (cond
            (and (nil? current) (str/starts-with? (str/trim line) "```"))
            (recur more
                   {:lang (subs (str/trim line) 3) :lines []
                    :blank-gap-before? (every? blank-line? gap-lines)}
                   []
                   blocks)

            (some? current)
            (if (= "```" (str/trim line))
              (recur more nil [] (conj blocks current))
              (recur more (update current :lines conj line) gap-lines blocks))

            :else
            (recur more nil (conj gap-lines line) blocks)))))))

(defn single-fence-command-lines
  "The first ```<fence-lang> fence's own taught command lines, comment
  and blank lines stripped, everything else kept verbatim -- nil if no
  such fence exists in `path`."
  [path fence-lang]
  (when-let [block (first (filter #(= fence-lang (:lang %)) (fenced-blocks path)))]
    (vec (remove comment-or-blank? (:lines block)))))

(defn command-output-pairs
  "Every ```<fence-lang> fenced block in `path`, in document order, as
  {:command-lines [...] :output-lines [...] or nil}. :output-lines is
  the very next block's own raw content when that block is a DIFFERENT
  language and the two fences are separated only by blank lines; nil
  otherwise -- the block still contributes its own :command-lines
  either way (a fence-lang block never disappears from this fn's own
  result just because it has no paired output)."
  [path fence-lang]
  (let [blocks (vec (fenced-blocks path))]
    (->> (map-indexed vector blocks)
         (filter (fn [[_ b]] (= fence-lang (:lang b))))
         (mapv (fn [[i b]]
                 (let [next-b (get blocks (inc i))
                       paired? (and next-b
                                    (:blank-gap-before? next-b)
                                    (not= fence-lang (:lang next-b)))]
                   {:command-lines (vec (remove comment-or-blank? (:lines b)))
                    :output-lines (when paired? (:lines next-b))}))))))

;; ---- the generalized check ----

(defn- absent-script-result
  [readme-lines]
  {:ok? false
   :readme-count (count readme-lines)
   :script-count 0
   :divergence {:index 0 :readme (first readme-lines) :script ::script-absent}})

(defn check-entry
  "Runs the freshness check for one exercised-sources register row.
  Returns {:source :script :ok? :readme-count :script-count
  :divergence} -- the same shape every :extraction kind produces,
  regardless of which extraction mechanism ran, so a caller (the
  citation gate, a future `make` target) never branches on kind.
  :quickstart-fresh/:demo-exerciser-fresh delegate to those namespaces'
  own `check` verbatim."
  [{:keys [source script extraction fence-lang marker-open marker-close]}]
  (merge
   {:source source :script script}
   (case extraction
     :quickstart-fresh
     (quickstart-fresh/check {:readme-path source :script-path script})

     :demo-exerciser-fresh
     (demo-fresh/check {:readme-path source :script-path script})

     :single-fence
     (let [readme-lines (or (single-fence-command-lines source fence-lang) [])
           script-lines (script-command-lines script marker-open marker-close)]
       (if (nil? script-lines)
         (absent-script-result readme-lines)
         {:ok? (nil? (diverge-at readme-lines script-lines))
          :readme-count (count readme-lines)
          :script-count (count script-lines)
          :divergence (diverge-at readme-lines script-lines)}))

     :paired
     (let [pairs (filter :output-lines (command-output-pairs source fence-lang))
           readme-lines (vec (mapcat :command-lines pairs))
           script-lines (script-command-lines script marker-open marker-close)]
       (if (nil? script-lines)
         (absent-script-result readme-lines)
         {:ok? (nil? (diverge-at readme-lines script-lines))
          :readme-count (count readme-lines)
          :script-count (count script-lines)
          :divergence (diverge-at readme-lines script-lines)})))))

(defn check-all
  "check-entry over every row in `rows`, as a vector in register order."
  [rows]
  (mapv check-entry rows))
