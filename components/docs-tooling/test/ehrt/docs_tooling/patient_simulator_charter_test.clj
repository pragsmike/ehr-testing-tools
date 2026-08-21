(ns ehrt.docs-tooling.patient-simulator-charter-test
  "ADR-0162: `components/patient-simulator` carries its own charter at
  `docs/limitations.md` -- the mission sentence at the front door, and
  one table row per DELIBERATE limitation, each with the citation that
  records the decision and the trigger (if any) that would end it.

  This gate exists because a charter that is not gated is a charter
  that drifts. Three obligations, in ascending order of how easy they
  are to break by accident:

  1. The charter exists and carries the mission sentence VERBATIM
     (whitespace-normalized, since both surfaces wrap it) -- and so
     does `interface.clj`'s own SCOPE section, so a reader who never
     opens the docs still meets the scope statement at the component's
     one public namespace.

  2. Every citation in the table RESOLVES: the quoted text occurs
     verbatim in the named file. Anchored by stable TEXT, never by line
     number -- the roadmap-lint precedent
     (`rulings.md#R-anchored-register-edits`, and
     `ehrt.docs-tooling.roadmap-lint-test`'s own
     `no-live-surface-cites-the-roadmap-by-line-number-test`).

  3. DRIFT: every deliberate-limitation marker in the component's own
     `src` -- the three tokens the Step-0 census of ADR-0162 found,
     `UNDECLARED` / `DELIBERATELY` / `not ported` -- is COVERED by a
     citation landing inside that marker's own `;;` comment block. Add
     a fourth marker to the source without tabling it and this goes
     red. The marker population is asserted NON-EMPTY first
     (`rulings.md#R-empty-population-is-red`): a scan that finds
     nothing proves nothing.

  Coverage is per COMMENT BLOCK, not per line, because one block can
  carry two markers about two different limitations (`gmf.clj`'s own
  CarePlan block does exactly that -- `not ported` for MedicationOrder's
  `:reason`, `UNDECLARED` for the CarePlan attribute pair) and each gets
  its own row and its own citation into the same block."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private charter-path "components/patient-simulator/docs/limitations.md")
(def ^:private interface-path
  "components/patient-simulator/src/ehrt/patient_simulator/interface.clj")
(def ^:private src-root "components/patient-simulator/src")

(def ^:private mission
  "Realistic EHR message traffic is the priority; patient-lifetime simulation is relevant only inasmuch as it contributes to realistic traffic.")

;; The three tokens ADR-0162's Step-0 census found across this
;; component's own src. Widening this pattern is allowed and expected;
;; narrowing it needs a reason, since every token dropped is a class of
;; drift this gate stops seeing.
(def ^:private marker-pattern #"UNDECLARED|DELIBERATELY|not ported")

;; `\`path\` "quoted snippet"` -- the citation form the table's own
;; Citation column uses, one or more per row.
(def ^:private citation-pattern #"`([^`\n]+)`\s+\"([^\"\n]+)\"")

(defn- squash
  "Whitespace-normalized, emphasis-stripped text -- so a sentence that
  is line-wrapped in Markdown and line-wrapped differently inside a
  Clojure docstring still compares equal."
  [s]
  (-> s (str/replace #"[*`]" "") (str/replace #"\s+" " ") str/trim))

(defn- comment-line? [lines i]
  (and (>= i 0) (< i (count lines)) (some? (re-find #"^\s*;;" (nth lines i)))))

(defn- comment-block
  "The inclusive [start end] line-index range of the contiguous `;;`
  comment block containing `idx`. A marker on a non-comment line is its
  own one-line block."
  [lines idx]
  (if-not (comment-line? lines idx)
    [idx idx]
    (let [start (loop [i idx] (if (comment-line? lines (dec i)) (recur (dec i)) i))
          end (loop [i idx] (if (comment-line? lines (inc i)) (recur (inc i)) i))]
      [start end])))

(defn- clj-files [root]
  (->> (file-seq (io/file root))
       (filter #(.isFile %))
       (filter #(str/ends-with? (.getName %) ".clj"))
       (sort-by #(.getPath %))))

(defn- charter-text
  "The charter's text, or \"\" when the file is absent. Absent must FAIL
  every obligation below legibly, not throw out of the first `slurp` --
  a gate whose own subject is missing should say so in its message, not
  in a stack trace."
  []
  (let [f (io/file charter-path)]
    (if (.isFile f) (slurp f) "")))

(defn- citations [charter-text]
  (for [[_ path snippet] (re-seq citation-pattern charter-text)]
    {:path path :snippet snippet}))

(defn- markers
  "Every marker occurrence under `root`, with its own comment block."
  [root]
  (for [f (clj-files root)
        :let [path (str/replace (.getPath f) "\\" "/")
              lines (vec (str/split-lines (slurp f)))]
        idx (range (count lines))
        :when (re-find marker-pattern (nth lines idx))
        :let [[s e] (comment-block lines idx)]]
    {:file path :line (inc idx) :text (str/trim (nth lines idx))
     :block-lines (subvec lines s (inc e))}))

(defn- uncovered
  "Markers under `root` that no citation in `charter-text` lands inside."
  [root charter-text]
  (let [by-path (group-by :path (citations charter-text))]
    (for [m (markers root)
          :let [snippets (map :snippet (get by-path (:file m)))]
          :when (not (some (fn [sn] (some #(str/includes? % sn) (:block-lines m)))
                           snippets))]
      (select-keys m [:file :line :text]))))

;; --- 1. the charter itself, and the SCOPE section on the interface ---------

(deftest the-charter-exists-and-carries-the-mission-sentence-test
  (let [f (io/file charter-path)]
    (is (.isFile f) (str charter-path " does not exist (ADR-0162)"))
    (is (str/includes? (squash (charter-text)) (squash mission))
        (str charter-path " does not carry the mission sentence verbatim: \"" mission "\""))))

(deftest the-interface-docstring-carries-a-scope-section-test
  (let [text (slurp (io/file interface-path))]
    (testing "the one public namespace states the scope, not just the docs"
      (is (str/includes? text "SCOPE")
          (str interface-path "'s docstring carries no SCOPE section (ADR-0162)"))
      (is (str/includes? (squash text) (squash mission))
          (str interface-path "'s docstring does not carry the mission sentence"))
      (is (str/includes? text "docs/limitations.md")
          (str interface-path "'s SCOPE section does not point at docs/limitations.md")))))

;; --- 2. every citation resolves -------------------------------------------

(defn- occurrences [haystack needle]
  (loop [from 0 n 0]
    (if-let [i (str/index-of haystack needle from)]
      (recur (inc i) (inc n))
      n)))

(deftest every-charter-citation-resolves-test
  (let [cits (citations (charter-text))]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq cits) "the charter table carries no `path` \"snippet\" citations at all"))
    (let [bad (for [{:keys [path snippet]} cits
                    :let [f (io/file path)]
                    :when (or (not (.isFile f)) (zero? (occurrences (slurp f) snippet)))]
                (str path " :: \"" snippet "\""
                     (if (.isFile f) " -- text not found in file" " -- file does not exist")))]
      (is (empty? bad)
          (str (count bad) " charter citation(s) do not resolve: " (vec bad))))))

(deftest every-charter-citation-anchors-exactly-one-place-test
  "A snippet that occurs TWICE in its file does not anchor anything, and
  a generic one is worse than useless here: the drift lint below asks
  whether a citation lands inside a marker's own comment block, so a
  snippet that matches wherever it is pasted marks every new marker
  'covered' the moment the new marker quotes it.

  This is not hypothetical. ADR-0162's own red witness planted
  `;; :some-invented-field is DELIBERATELY UNDECLARED here.` in
  `gmf.clj` and the drift lint stayed GREEN, because the charter's
  VitalSign citation was the bare string \"DELIBERATELY UNDECLARED\" and
  the planted line contained it. The plant was supposed to prove the
  lint worked; it proved the opposite, and this test is what the
  proof-of-failure bought."
  (let [bad (for [{:keys [path snippet]} (citations (charter-text))
                  :let [f (io/file path)]
                  :when (.isFile f)
                  :let [n (occurrences (slurp f) snippet)]
                  :when (> n 1)]
              (str path " :: \"" snippet "\" -- occurs " n " times"))]
    (is (empty? bad)
        (str (count bad) " charter citation(s) are not unique in their own file"
             " -- an anchor that matches twice anchors nothing: " (vec bad)))))

;; --- 3. the drift lint ----------------------------------------------------

(deftest every-in-source-limitation-marker-is-tabled-test
  (let [found (markers src-root)]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq found)
          (str "no deliberate-limitation marker found under " src-root
               " -- the scan proves nothing; widen or fix the pattern before trusting it")))
    (let [bad (uncovered src-root (charter-text))]
      (is (empty? bad)
          (str (count bad) " in-source limitation marker(s) with no covering citation in "
               charter-path " -- table each, or the decline is undocumented: " (vec bad))))))

;; --- mechanism-sanity: prove the lint catches what it claims to -----------

(deftest marker-coverage-is-actually-caught-test
  (let [tmp (io/file (System/getProperty "java.io.tmpdir")
                     (str "patient-simulator-charter-test-" (System/nanoTime)))
        src (io/file tmp "src")
        f (io/file src "planted.clj")]
    (try
      (.mkdirs src)
      (spit f (str ";; a first comment line\n"
                   ";; :expression is DELIBERATELY UNDECLARED here\n"
                   "(def x 1)\n"))
      (testing "an in-source marker with no citation at all is caught"
        (is (= 1 (count (uncovered (.getPath src) "no citations here")))))
      (testing "a citation landing inside the marker's own comment block covers it"
        ;; Deliberately cites a NEIGHBOURING line in the same block, not the
        ;; marker line itself -- citing the marker text back at itself is the
        ;; self-match this gate's own red witness caught.
        (is (empty? (uncovered (.getPath src)
                               (str "| x | y | `" (.getPath f) "` \"a first comment line\" | none |")))))
      (testing "a citation into the RIGHT file but the WRONG block does not cover it"
        (is (= 1 (count (uncovered (.getPath src)
                                   (str "| x | y | `" (.getPath f) "` \"(def x 1)\" | none |"))))))
      (testing "a citation into a DIFFERENT file does not cover it"
        (is (= 1 (count (uncovered (.getPath src)
                                   "| x | y | `deps.edn` \"DELIBERATELY UNDECLARED\" | none |")))))
      (finally
        (when (.exists f) (.delete f))
        (when (.exists src) (.delete src))
        (when (.exists tmp) (.delete tmp))))))

(deftest citation-extraction-is-actually-caught-test
  (is (= [{:path "a/b.clj" :snippet "some text"} {:path "c.md" :snippet "other"}]
         (citations "| r | w | `a/b.clj` \"some text\"; `c.md` \"other\" | none |")))
  (is (empty? (citations "a row with `a backtick` but no quoted snippet"))))

(deftest comment-block-extraction-is-actually-caught-test
  (let [lines ["(def a 1)" "   ;; one" "   ;; two" "   ;; three" "(def b 2)"]]
    (is (= [1 3] (comment-block lines 2)))
    (is (= [1 3] (comment-block lines 1)))
    (is (= [0 0] (comment-block lines 0)))))
