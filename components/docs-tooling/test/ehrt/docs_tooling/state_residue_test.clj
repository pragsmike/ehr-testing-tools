(ns ehrt.docs-tooling.state-residue-test
  "The hand-owned half of the continuity register, capped and linted
  (compression arc session D, `notes/adr/0147-compression-arc-close.md`).

  `.agents/state.md` cannot be generated -- it holds judgement, watch
  items, environment facts and the design-channel contract, none of
  which a tree derives. So it takes the roadmap's and the rulings
  register's treatment instead (ADR-0144/ADR-0145): made SMALL and
  LINTED, with a cap that a session cannot satisfy by editing the cap.

  Four assertions, each answering a way the file grew to 724 lines:

  - **The cap.** 120 lines. The file reached 724 by appending: thirteen
    dated preamble blocks, each honest, each individually justified,
    none of them ever the moment someone stopped and moved history to
    an attic. A cap makes the next append a conscious act.
  - **The tripwire's own anchor survives compaction.** The regeneration
    citation `ehrt.docs-tooling.state-staleness-tripwire-test` reads is
    a specific phrase in this file's header. Compaction that removed it
    would leave that gate matching nil against nil-ish and passing for
    the wrong reason, so this asserts the phrase is present here rather
    than trusting the other gate to notice its own anchor vanish.
  - **The pointer table is complete in both directions.** Every
    top-level `.agents/` register has a row naming its own gate, and
    every row resolves to a real file and a real gate namespace. This
    is the arc's second law made mechanical (`no register in a reading
    set without a lint on its growth`) -- if a future session adds
    `.agents/something.md`, this test asks it where the gate is.
  - **No duplication of the derived half.** A `[V @sha]` claim was the
    old file's own 'probe-verified at this landing' stamp, and every
    fact it stamped is now re-derived on every `make docsgen`. Carrying
    one here again would recreate the exact failure this arc closed: a
    number that was true at a landing, in a file nothing regenerates."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.set :as set]
            [clojure.java.io :as io]))

(def ^:private state-path ".agents/state.md")
(def ^:private line-cap 120)

(defn- content [] (slurp state-path))

(deftest state-md-stays-within-its-line-cap-test
  (testing "the hand-owned register is small by law, not by habit"
    (let [n (count (str/split-lines (content)))]
      (is (<= n line-cap)
          (str state-path " is " n " lines, over its " line-cap "-line cap by " (- n line-cap)
               " -- move history verbatim to `.agents/plans/state-history-2026-08.md` and any "
               "countable claim into the generated `.agents/state-derived.md`. Raising this cap "
               "is the move this arc exists to make unavailable.")))))

(deftest state-md-keeps-the-staleness-tripwires-own-anchor-test
  (testing "the phrase state-staleness-tripwire-test matches on must survive every compaction of this file"
    (is (re-find #"own\s+close\s*\(`notes/adr/(\d{4})-[\w-]+-arc-close\.md`" (content))
        (str state-path " no longer carries the `... own close (`notes/adr/NNNN-<slug>-arc-close.md`)` "
             "regeneration citation that `ehrt.docs-tooling.state-staleness-tripwire-test` reads. "
             "That gate would then be matching against nothing -- compaction must keep the anchor."))))

;; -- the pointer table, both directions --

(defn- pointer-rows
  "Every `| `path` | ... | `gate` |` row of the register pointer table.
  Returns `{:path :gate}`; `:gate` is the row's LAST backticked token,
  which is where the table puts the enforcing namespace or command."
  [content]
  (->> (str/split-lines content)
       (keep (fn [line]
               (when (str/starts-with? line "| `")
                 (let [cells (->> (str/split line #"\|") (map str/trim) (remove str/blank?))
                       tokens (map #(second (re-find #"`([^`]+)`" (str %))) cells)]
                   (when (and (first tokens) (last tokens) (< 1 (count cells)))
                     {:path (first tokens) :gate (last tokens)})))))
       vec))

(defn- agents-top-level-registers
  "Every regular file directly under `.agents/`. The POPULATION IS THE
  TREE (`rulings.md#R-population-closure`) -- a hand-listed set here
  would be the very defect ADR-0139 named, in the gate that guards the
  register that records it."
  []
  (->> (.listFiles (io/file ".agents"))
       (filter #(.isFile %))
       (map #(str ".agents/" (.getName %)))
       set))

(deftest every-agents-register-has-a-pointer-row-test
  (testing "presence: a register with no row is a register a cold reader never finds"
    (let [rows (set (map :path (pointer-rows (content))))
          missing (set/difference (agents-top-level-registers) rows)]
      (is (empty? missing)
          (str state-path "'s pointer table has no row for: " missing
               " -- every top-level .agents/ register owes a row naming what it is and what gates it.")))))

(deftest every-pointer-row-resolves-test
  (testing "absence: a row pointing at a file that does not exist sends a cold reader to read nothing"
    (let [ghosts (->> (pointer-rows (content))
                      (map :path)
                      (remove #(.isFile (io/file %)))
                      vec)]
      (is (empty? ghosts)
          (str state-path "'s pointer table cites a path that does not exist: " ghosts)))))

(deftest every-pointer-row-names-a-gate-that-exists-test
  (testing "a gate cite is only worth having if the gate is real -- an `ehrt.*-test` token must resolve to a test file on disk"
    (let [bad (for [{:keys [path gate]} (pointer-rows (content))
                    :when (str/starts-with? (str gate) "ehrt.")
                    :let [file (str "components/docs-tooling/test/"
                                    (-> gate (str/replace "." "/") (str/replace "-" "_")) ".clj")]
                    :when (not (.isFile (io/file file)))]
                (str path " -> " gate " (expected " file ")"))]
      (is (empty? (vec bad))
          (str state-path " names a gate that does not exist: " (vec bad))))))

(deftest state-md-does-not-duplicate-the-derived-register-test
  (testing "no `[V @sha]` stamped claim: every fact that carried one is now re-derived on every `make docsgen`"
    (is (not (re-find #"\[V @" (content)))
        (str state-path " carries a `[V @sha]` probe-verified claim. Those are exactly the claims "
             "that went stale for fifty ADRs; they belong in `.agents/state-derived.md`, which is "
             "regenerated from the tree and diffed by CI."))))

;; -- mechanism sanity: the row parser actually parses a row --

(deftest pointer-row-parsing-is-actually-caught-test
  (let [fixture (str "| register | what it is | gate |\n"
                     "|---|---|---|\n"
                     "| `.agents/rulings.md` | standing rules | `ehrt.docs-tooling.rulings-lint-test` |\n"
                     "| `.agents/state.md` | this file | `ehrt.docs-tooling.state-residue-test` |\n"
                     "some prose mentioning `.agents/plans/roadmap.md` outside any table\n")]
    (is (= [{:path ".agents/rulings.md" :gate "ehrt.docs-tooling.rulings-lint-test"}
            {:path ".agents/state.md" :gate "ehrt.docs-tooling.state-residue-test"}]
           (pointer-rows fixture))
        "only `| `...` |` table rows count; a backticked path in ordinary prose is not a pointer row")))
