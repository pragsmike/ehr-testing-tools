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
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
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

;; ---- runtime output comparison (bin/readme-what-you-get, ADR-0129
;; Q4(a): "extraction pairs command fences with adjacent expected-
;; output fences and compares output"). README.md's own ```clojure
;; output fences are hand-formatted, illustrative EXCERPTS of the real
;; CLI output, not verbatim captures -- the real output carries extra
;; fields (:engine, :native-ref) the fence never claims, and the
;; fence's own innermost maps end in a literal ", ..." eliding the
;; rest ("keys not shown here"). A literal byte-diff can never match
;; this convention (verified live before choosing this design: the
;; real `gate fhir` output for the fixture the fence documents is a
;; single line with :engine/:native-ref present, the fence's own text
;; is neither single-line nor missing those keys, it elides them).
;; "normalize only what quickstart-demo already normalizes" (the
;; charter's own words) turned out to have an empty base to inherit --
;; quickstart-demo asserts only exit codes, no output-text comparison
;; at all -- so this fn's own elision-tolerant subset match is a new,
;; disclosed design rather than an inherited one: every value the
;; fence's own text states must be present and equal in the real
;; captured output; extra real fields (whatever the fence's own
;; trailing "..." elides) are always allowed; vectors must match in
;; length and element-wise (an omitted or extra element is real
;; content drift, not elision).

(defn- strip-ellipsis-markers
  "README.md's own fenced-output convention marks an elided map tail
  with a literal `, ...` (or ` ...`) immediately before the closing
  `}`/`]` -- not valid EDN on its own. Stripped so the remainder reads
  as plain EDN; the elision itself needs no further tracking, because
  `subset-match?` below always tolerates extra map keys everywhere,
  not only at a `...` site."
  [text]
  (str/replace text #",?\s*\.\.\.\s*(?=[}\]])" ""))

(defn parse-elided-edn
  "Reads `lines` (a fenced output block's own raw content) as EDN,
  after stripping README.md's own `...` elision markers."
  [lines]
  (edn/read-string (strip-ellipsis-markers (str/join "\n" lines))))

(defn subset-match?
  "true if every value `expected` states is present and equal in
  `actual`, recursively. Maps: every key in `expected` must exist in
  `actual` with a recursively-matching value; extra keys in `actual`
  are always allowed (README.md's own fence documents \"at least this
  much\", never a closed set). Vectors: same length, element-wise
  match (an omitted or extra element is real content drift the fence
  would be wrong to gloss over, unlike an extra map key). Anything
  else: `=`."
  [expected actual]
  (cond
    (map? expected)
    (and (map? actual)
         (every? (fn [[k v]] (and (contains? actual k) (subset-match? v (get actual k))))
                 expected))

    (vector? expected)
    (and (vector? actual)
         (= (count expected) (count actual))
         (every? true? (map subset-match? expected actual)))

    :else
    (= expected actual)))

(defn paired-output-check!
  "-X-invokable (`clojure -X:dev ehrt.docs-tooling.strip-fresh/paired-
  output-check!`): compares the `pair-index`'th (0-based) paired
  output fence extracted live from `source` (`fence-lang` fences)
  against the real captured stdout in `actual-file` -- an elision-
  tolerant subset match, per this namespace's own header comment
  above. Prints OK/FAIL with both sides shown on failure; exits 0/1.
  bin/readme-what-you-get's own per-pair runtime check, run AFTER its
  own `expect`-asserted exit codes, against real freshly captured
  output, never a canned fixture."
  [{:keys [source fence-lang pair-index actual-file]}]
  (let [pairs (vec (filter :output-lines (command-output-pairs source fence-lang)))
        {:keys [output-lines]} (get pairs pair-index)
        expected (parse-elided-edn output-lines)
        actual (edn/read-string (slurp actual-file))]
    (if (subset-match? expected actual)
      (do (println (str "OK: pair " pair-index
                         " -- real captured output matches " source
                         "'s own expected fence (subset match, extra fields allowed)"))
          (System/exit 0))
      (do (println (str "FAIL: pair " pair-index
                         " -- real captured output does NOT match " source "'s own expected fence"))
          (println (str "  expected: " (pr-str expected)))
          (println (str "  actual:   " (pr-str actual)))
          (System/exit 1)))))
