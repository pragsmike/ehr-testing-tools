(ns ehrt.docs-tooling.dense-7500-figures-test
  "The gate ADR-0180's R-gate-gaps commissions over the dense-7500
  scenario's published figures, under author ruling R-figures-file
  (2026-09-07): *the deterministic figures live in ONE committed file
  the exerciser writes; README rows and the Scale table quote it and a
  test proves they match. Process-wall cells are dated quotations
  (sha, date, machine) of the same file, never asserted against a live
  run.*

  WHAT WENT WRONG WITHOUT IT, in the concrete. Three cells of
  `docs/consuming-ground-truth.md`'s Scale table are that scenario's
  own, they moved under ADR-0179, and they were re-measured by hand
  because nothing failed when they did not move with it. Then ADR-0180's
  seven sites shortened the same configuration's generate phase three
  more times and every wall in both documents went stale in silence.
  Prose cannot carry the claim `these two documents agree with one
  measurement`; this namespace can.

  THE SOURCE IS `demos/scenarios/dense-7500/figures.edn`, whose own
  header states which half `bin/demo-exerciser-dense-7500` rewrites on
  every run and which half is hand-measured. This gate does not care
  which half a figure came from -- it holds both documents to the
  merged file. What the halves buy is upstream of here: the exerciser's
  tree-clean postcondition turns a moved count red at the run, and this
  namespace turns a document that stopped quoting the file red at the
  suite.

  Six claims:

  (a) BOTH TABLES ARE FOUND -- each is located by its own header row,
      and a header that has been reworded fails by name rather than
      leaving a zero-row loop passing vacuously
      (`rulings.md#R-empty-population-is-red`).

  (b) POPULATION CLOSURE -- the README's table has a row for every cell
      in `figures.edn` and no row that the file does not know, and the
      Scale table's three rows map onto three of those same cells.

  (c) EVERY QUOTED NUMBER MATCHES -- events, messages, msg/event,
      process wall and peak RSS, cell by cell, in both documents.

  (d) `:msg-per-event` IS RE-DERIVED, not trusted: it must equal
      `:messages` over `:events` to four decimal places. A hand
      re-measure that updates two of the three and not the third is
      the failure this catches.

  (e) BOTH DOCUMENTS CARRY THE SAME DATED QUOTATION -- each names
      `:provenance`'s own `:sha` and `:date`, in its own witnessing
      sentence, and that sha resolves to a commit in this repository.
      This is what makes the wall cells honest without asserting them
      against a live run.

  (f) THE WRITER AND THE FILE STILL AGREE -- the exerciser names this
      file and the file carries the markers the exerciser splices
      between. Either half renamed without the other leaves a rewrite
      that changes nothing, which would read as `every count still
      holds` forever.

  (g) THE FRONT DOOR QUOTES IT TOO -- the workspace README's own
      msg/event sentence is a third quoting document, and it is prose
      rather than a table row, so (b)'s population closure cannot reach
      it. It went stale exactly the way the two tables did: it still
      published the pre-ADR-0179 counts after both re-measures. Its two
      bolded figures are located by the paragraph's opening clause and
      held to the same merged file -- exactly two, so a third figure
      added there fails rather than going unchecked."
  (:require [clojure.edn :as edn]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def ^:private figures-path "demos/scenarios/dense-7500/figures.edn")
(def ^:private readme-path "demos/scenarios/dense-7500/README.md")
(def ^:private scale-doc-path "docs/consuming-ground-truth.md")
(def ^:private exerciser-path "bin/demo-exerciser-dense-7500")
(def ^:private root-readme-path "README.md")

(def ^:private readme-header
  "| cell | arrivals | events | messages | msg/event | process wall | peak RSS |")

(def ^:private scale-header
  "| Cell | events | messages | msg/event | process wall |")

(def ^:private readme-row-keys
  "How the scenario README identifies a row -- config file and arrival
  count -- mapped to this file's own cell keys."
  {["`config.edn`" "7,500"] :config-edn-7500
   ["`config-nobed.edn`" "7,500"] :config-nobed-7500
   ["`config-bare.edn`" "7,500"] :config-bare-7500
   ["`config.edn`" "750"] :config-edn-750})

(def ^:private scale-row-keys
  "How the Scale table identifies a row -- by what the config turns on,
  prose rather than filename -- mapped to the same cell keys. All three
  are the 7,500-arrival cells; the 750 one is the scenario README's
  alone."
  {"all nine opt-in keys" :config-edn-7500
   "the same, less `:bed-cycle`" :config-nobed-7500
   "no opt-in key at all" :config-bare-7500})

(defn- figures [] (edn/read-string (slurp figures-path)))

(defn- merged-cells
  "The two halves as one map of cell key -> figures. A field declared in
  both halves is itself a defect -- the halves are a division of labour,
  not a fallback chain -- so it is reported rather than resolved."
  [{:keys [asserted quoted]}]
  (reduce (fn [acc k]
            (let [a (get asserted k) q (get quoted k)
                  overlap (set (filter (set (keys q)) (keys a)))]
              (assoc acc k (cond-> (merge a q)
                             (seq overlap) (assoc ::overlap overlap)))))
          {}
          (distinct (concat (keys asserted) (keys quoted)))))

(defn- table-rows
  "Every row of the markdown table `header` opens, as vectors of trimmed
  cell strings -- the header and the `|---|` rule dropped. nil when the
  header line is not present at all, which claim (a) reports."
  [path header]
  (let [lines (str/split-lines (slurp path))
        idx (first (keep-indexed #(when (= header (str/trim %2)) %1) lines))]
    (when idx
      (->> (drop (+ idx 2) lines)
           (take-while #(str/starts-with? (str/trim %) "|"))
           (map #(->> (str/split (str/trim %) #"\|") (drop 1) (map str/trim) vec))))))

(defn- num
  "A figure as both documents write one: bolded, comma-grouped, and
  carrying its own unit."
  [s]
  (-> s (str/replace "*" "") (str/replace "," "")
      (str/replace #"\s*(s|MB)$" "") str/trim Double/parseDouble))

(defn- readme-quoted []
  (into {} (for [row (table-rows readme-path readme-header)
                 :let [k (readme-row-keys [(nth row 0) (nth row 1)])]
                 :when k]
             [k {:events (num (nth row 2)) :messages (num (nth row 3))
                 :msg-per-event (num (nth row 4)) :wall-s (num (nth row 5))
                 :peak-rss-mb (num (nth row 6))}])))

(defn- scale-quoted []
  (into {} (for [row (table-rows scale-doc-path scale-header)
                 :let [k (scale-row-keys (nth row 0))]
                 :when k]
             [k {:events (num (nth row 1)) :messages (num (nth row 2))
                 :msg-per-event (num (nth row 3)) :wall-s (num (nth row 4))}])))

;; -- (a) both tables are found --

(deftest both-quoting-tables-are-located-test
  (testing "the scenario README's measured-cells table"
    (is (seq (table-rows readme-path readme-header))
        (str readme-path " has no row under " (pr-str readme-header)
             " -- if that header was reworded, reword it here too; every check below "
             "would otherwise loop over nothing and pass")))
  (testing "the Scale table"
    (is (seq (table-rows scale-doc-path scale-header))
        (str scale-doc-path " has no row under " (pr-str scale-header) " -- see above"))))

;; -- (b) population closure --

(deftest every-published-row-is-a-known-cell-test
  (let [known (set (keys (merged-cells (figures))))]
    (testing "the README publishes exactly the cells figures.edn carries"
      (is (= known (set (keys (readme-quoted))))
          (str "figures.edn carries " (pr-str known) " and " readme-path " publishes "
               (pr-str (set (keys (readme-quoted))))
               " -- a row on one side only is a figure with no source or a source no one reads")))
    (testing "every Scale row maps onto one of them"
      (is (= (set (vals scale-row-keys)) (set (keys (scale-quoted))))
          (str scale-doc-path "'s Scale rows are no longer the three this gate knows -- "
               "a renamed row label reads here as an absent row")))
    (testing "and the Scale rows are cells figures.edn actually carries"
      (is (every? known (keys (scale-quoted)))))))

;; -- (c) every quoted number matches, in both documents --

(deftest the-readme-quotes-the-figures-file-test
  (let [cells (merged-cells (figures))]
    (doseq [[k quoted] (readme-quoted)
            [field want] (get cells k)
            :when (not= field ::overlap)]
      (testing (str k " " field)
        (is (== (double want) (get quoted field))
            (str readme-path " quotes " (get quoted field) " for " k " " field
                 " but " figures-path " holds " want
                 " -- re-measure through the figures file, never a document"))))))

(deftest the-scale-table-quotes-the-figures-file-test
  (let [cells (merged-cells (figures))]
    (doseq [[k quoted] (scale-quoted)
            [field want] quoted]
      (testing (str k " " field)
        (is (== (double (get (get cells k) field)) want)
            (str scale-doc-path "'s Scale table quotes " want " for " k " " field
                 " but " figures-path " holds " (get (get cells k) field)
                 " -- these three cells are the ones ADR-0180 found stale twice"))))))

;; -- (d) msg/event is re-derived, and the halves do not overlap --

(deftest msg-per-event-is-messages-over-events-test
  (doseq [[k {:keys [events messages msg-per-event] :as cell}] (merged-cells (figures))]
    (testing (str k " declares each figure in exactly one half")
      (is (nil? (::overlap cell))
          (str k " declares " (pr-str (::overlap cell)) " in both :asserted and :quoted -- "
               "the halves divide the work, and a field in both has two sources of truth")))
    (testing (str k " msg/event")
      (is (= msg-per-event
             (-> (/ (double messages) events) (* 10000.0) Math/round (/ 10000.0)))
          (str k " carries " msg-per-event " msg/event against " messages " messages over "
               events " events -- a re-measure moved two of the three and left the third")))))

;; -- (e) the dated quotation --

(deftest both-documents-carry-the-same-dated-quotation-test
  (let [{:keys [sha date]} (:provenance (figures))]
    (testing "the scenario README's witnessing sentence"
      (is (str/includes? (slurp readme-path) (str "Witnessed " date " at `" sha "`"))
          (str readme-path " does not witness " date " at " sha
               " -- the walls it publishes are quotations and must say what they are quoting")))
    (testing "the Scale table's own"
      (is (str/includes? (slurp scale-doc-path) (str "re-measured " date " at `" sha "`"))
          (str scale-doc-path " does not name " date " at " sha " under its Scale table")))
    (testing "and the sha is a commit in this repository, not a plausible-looking string"
      (is (zero? (:exit (shell/sh "git" "rev-parse" "--verify" "--quiet" (str sha "^{commit}"))))
          (str figures-path " names " sha ", which does not resolve to a commit here")))))

;; -- (f) the writer and the file still agree --

(deftest the-exerciser-still-writes-this-file-test
  (let [script (slurp exerciser-path)
        raw (slurp figures-path)]
    (testing "the exerciser names the figures file"
      (is (str/includes? script figures-path)
          (str exerciser-path " no longer names " figures-path
               " -- nothing would rewrite the asserted half and every count would freeze")))
    (testing "the file carries the markers the exerciser splices between"
      (doseq [marker [";; BEGIN asserted -- rewritten by bin/demo-exerciser-dense-7500 on every run"
                      ";; END asserted"]]
        (is (str/includes? raw marker)
            (str figures-path " has lost " (pr-str marker)
                 " -- the splice would then change nothing, which reads as 'every count still holds'"))
        (is (str/includes? script marker)
            (str exerciser-path " no longer carries " (pr-str marker) " -- see above"))))))

;; -- (g) the front door's own two figures --

(def ^:private root-readme-anchor
  "The opening clause of the workspace README's msg/event sentence.
  Reworded, the locator below finds nothing and this gate fails by
  name rather than looping over an empty paragraph
  (`rulings.md#R-empty-population-is-red`)."
  "How much wire traffic one event turns into depends on what you switch")

(def ^:private root-readme-figure-re
  "A bold run opening on a decimal. The run may wrap a line -- the
  README writes `**1.3327 messages per\\nevent**` -- so the whole run is
  matched and its leading decimal taken."
  #"\*\*([0-9]+\.[0-9]+)[^*]*\*\*")

(defn- root-readme-figures
  "The bolded figures of that one paragraph, in document order. nil when
  the anchor clause is gone."
  []
  (let [lines (str/split-lines (slurp root-readme-path))
        idx (first (keep-indexed #(when (str/includes? %2 root-readme-anchor) %1) lines))]
    (when idx
      (->> (drop idx lines)
           (take-while #(not (str/blank? %)))
           (str/join "\n")
           (re-seq root-readme-figure-re)
           (mapv #(Double/parseDouble (second %)))))))

(deftest the-front-door-quotes-the-figures-file-test
  (let [cells (merged-cells (figures))
        found (root-readme-figures)]
    (testing "the paragraph is located and publishes exactly two figures"
      (is (= 2 (count found))
          (str root-readme-path " publishes " (pr-str found) " under "
               (pr-str root-readme-anchor)
               " -- this gate holds exactly two msg/event figures there; a reworded "
               "clause reads as none, and a third figure would go unchecked")))
    (testing "all nine opt-in keys"
      (is (== (:msg-per-event (:config-edn-7500 cells)) (first found))
          (str root-readme-path " quotes " (first found)
               " for the all-keys cell but " figures-path " holds "
               (:msg-per-event (:config-edn-7500 cells))
               " -- re-measure through the figures file, never a document")))
    (testing "no opt-in key at all"
      (is (== (:msg-per-event (:config-bare-7500 cells)) (second found))
          (str root-readme-path " quotes " (second found)
               " for the bare cell but " figures-path " holds "
               (:msg-per-event (:config-bare-7500 cells)) " -- see above")))))
