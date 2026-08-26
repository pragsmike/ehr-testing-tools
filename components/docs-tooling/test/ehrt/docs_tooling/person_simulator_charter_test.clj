(ns ehrt.docs-tooling.person-simulator-charter-test
  "ADR-0172 section 4: `components/person-simulator` carries its own
  charter at `docs/limitations.md` -- the mission sentence at the front
  door, and one table row per DELIBERATE limitation, each with the
  citation that records the decision and the GATE that goes red if the
  decline is silently lifted.

  Sibling to `ehrt.docs-tooling.patient-simulator-charter-test`, and
  for the same stated reason: a charter that is not gated is a charter
  that drifts. It adds one obligation the sibling does not have,
  because ADR-0172 tables the same eleven rows the component does --

  1. The mission sentence occurs VERBATIM (whitespace-normalized, since
     every surface wraps it) in FOUR places: ADR-0172, the charter,
     the component's `README.md`, and `interface.clj`'s own SCOPE
     section -- so a reader who never opens the docs still meets the
     scope statement at the component's one public namespace.

  2. MIRROR: the charter's rows and ADR-0172 section 4's rows carry
     the same bolded limitation titles and name the same gates. This is the obligation the sibling has no need of,
     and it exists because two tables of the same eleven facts in two
     files is exactly the drift this gate family was built for.

  3. Every citation RESOLVES: the quoted text occurs verbatim in the
     named file. Anchored by stable TEXT, never by line number
     (`rulings.md#R-anchored-register-edits`). And every citation
     anchors EXACTLY ONE place in its own file -- ADR-0162's own
     hard-won correction, bought by a red witness that stayed green
     because a generic snippet matched wherever it was pasted.

  4. DRIFT (limitations row 9's own gate,
     `every-provisional-rate-is-tabled-test`): every
     deliberate-limitation marker in the component's own `src` --
     `PROVISIONAL`, plus the sibling's three tokens `UNDECLARED` /
     `DELIBERATELY` / `not ported` -- is COVERED by a citation landing
     inside that marker's own `;;` comment block. `PROVISIONAL` is the
     fourth token ADR-0172 row 9 asks for, and it exists because every
     hazard rate in this component is authored with no table behind it
     (ruling E1): the marker is what keeps an unsourced rate visible
     forever instead of letting it quietly become folklore.

  5. And the tie between the table and the tests: every gate the two
     tables NAME exists as a `deftest` somewhere under this
     workspace's test trees. Ten live in
     `ehrt.person-simulator.limitations-test`; row 9's lives here,
     beside the citation machinery it needs. Neither file can drop one
     quietly."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private charter-path "components/person-simulator/docs/limitations.md")
(def ^:private readme-path "components/person-simulator/README.md")
(def ^:private adr-path "notes/adr/0172-person-simulator-charter.md")
(def ^:private interface-path
  "components/person-simulator/src/ehrt/person_simulator/interface.clj")

(def ^:private mission
  "The person process exists so that demographic and identity traffic is realistic; a person's life is relevant only inasmuch as it changes a message.")

(def ^:private expected-row-count
  "ADR-0172 section 4 tables THIRTEEN limitations -- eleven at
  2026-08-25, plus row 12 (a parent may head more than one household),
  the v1 artefact arc 2b stated in `persons`' own docstring and arc 3a
  tabled, plus row 13 (a household never loses its housing), which arc
  3a's own `:residence-loss` forced and ADR-0173 did not anticipate.
  Pinned, so that a row
  quietly dropped from ONE of the two tables is caught by the mirror
  below while a row dropped from BOTH is caught here
  (`rulings.md#R-empty-population-is-red`, one step further: a mirror
  gate over two empty tables agrees perfectly)."
  13)

;; `\`path\` "quoted snippet"` -- the citation form the Citation column
;; uses, one or more per row. Identical to the sibling gate's.
(def ^:private citation-pattern #"`([^`\n]+)`\s+\"([^\"\n]+)\"")

;; `| <n> | **Title.** rest... |` -- a numbered limitation row in either
;; table. Both tables are numbered; the sibling's is not, which is why
;; this pattern is not shared.
(def ^:private row-pattern #"(?m)^\|\s*(\d+)\s*\|\s*\*\*(.+?)\*\*")

(defn- squash
  "Whitespace-normalized, emphasis-stripped text -- so a sentence
  line-wrapped in Markdown and line-wrapped differently inside a
  Clojure docstring still compares equal. Blockquote markers go too:
  the ADR carries the mission sentence as a BLOCKQUOTE, both Markdown
  surfaces carry it as a bold paragraph, and `interface.clj` carries
  it as plain docstring prose -- three renderings of one sentence,
  which is exactly what this gate exists to hold together."
  [s]
  (-> s
      (str/replace #"(?m)^\s*>\s?" "")
      (str/replace #"[*`]" "")
      (str/replace #"\s+" " ")
      str/trim))

(defn- text
  "A file's text, or \"\" when absent. Absent must FAIL every obligation
  below legibly, not throw out of the first `slurp` -- a gate whose own
  subject is missing should say so in its message, not in a stack
  trace."
  [path]
  (let [f (io/file path)] (if (.isFile f) (slurp f) "")))

(defn- rows
  "Limitation number -> bolded title, for one table's text."
  [t]
  (into {} (for [[_ n title] (re-seq row-pattern t)]
             [(parse-long n) (squash title)])))

(defn- section-4
  "ADR-0172 section 4's text alone -- section 5 opens with `### 5.` and
  the ADR's other tables are not limitation tables, so slicing at the
  two headings is what keeps `rows` from reading the census."
  [adr]
  (let [start (str/index-of adr "### 4. Deliberate limitations")
        end (str/index-of adr "### 5. Rulings needed")]
    (if (and start end (< start end)) (subs adr start end) "")))

(defn- gate-names
  "Limitation number -> the FIRST inline-code token in the row's last
  column, which is the gate's own test name. Taken from the row's tail
  after the citation column, so a citation path is never mistaken for a
  gate."
  [t]
  (into {} (for [line (str/split-lines t)
                 :when (re-find row-pattern line)
                 :let [n (parse-long (second (re-find #"^\|\s*(\d+)\s*\|" line)))
                       cells (vec (str/split line #"\s\|\s"))
                       gate-cell (last cells)
                       token (second (re-find #"`([^`\n]+)`" gate-cell))]
                 :when token]
             [n token])))

(defn- citations [t]
  (for [[_ path snippet] (re-seq citation-pattern t)]
    {:path path :snippet snippet}))

(defn- occurrences [haystack needle]
  (loop [from 0 n 0]
    (if-let [i (str/index-of haystack needle from)]
      (recur (inc i) (inc n))
      n)))

;; --- 1. the mission sentence, in all four places --------------------------

(deftest the-mission-sentence-lands-in-every-front-door-test
  (doseq [path [adr-path charter-path readme-path interface-path]]
    (testing path
      (is (.isFile (io/file path)) (str path " does not exist (ADR-0172)"))
      (is (str/includes? (squash (text path)) (squash mission))
          (str path " does not carry the mission sentence verbatim: \"" mission "\"")))))

(deftest the-interface-docstring-carries-a-scope-section-test
  (let [t (text interface-path)]
    (testing "the one public namespace states the scope, not just the docs"
      (is (str/includes? t "SCOPE")
          (str interface-path "'s docstring carries no SCOPE section (ADR-0172)"))
      (is (str/includes? t "docs/limitations.md")
          (str interface-path "'s SCOPE section does not point at docs/limitations.md")))))

(deftest the-interface-exposes-exactly-the-chartered-front-door-test
  (let [t (text interface-path)]
    (testing "ADR-0172 section 2's three arities, and no fourth public var"
      (is (str/includes? t "(defn persons"))
      (is (str/includes? t "(defn initial-persona"))
      (is (= #{"persons" "initial-persona"}
             (set (map second (re-seq #"(?m)^\(defn ([a-z][a-z-]*)" t))))
          (str interface-path " exposes a public var ADR-0172 section 2 does not charter")))))

;; --- 2. the mirror: two tables, same rows, same titles, same gates --------

(deftest the-charter-and-the-adr-table-the-same-limitations-test
  (let [adr-rows (rows (section-4 (text adr-path)))
        charter-rows (rows (text charter-path))]
    (testing "population is non-empty and pinned (R-empty-population-is-red)"
      (is (= expected-row-count (count adr-rows))
          (str "ADR-0172 section 4 tables " (count adr-rows) " rows, expected "
               expected-row-count))
      (is (= expected-row-count (count charter-rows))
          (str charter-path " tables " (count charter-rows) " rows, expected "
               expected-row-count)))
    (testing "same numbers, same bolded titles"
      (is (= (set (keys adr-rows)) (set (keys charter-rows))))
      (let [bad (for [[n title] adr-rows
                      :let [mine (get charter-rows n)]
                      :when (not= title mine)]
                  (str "row " n ": ADR says \"" title "\", charter says \"" mine "\""))]
        (is (empty? bad)
            (str (count bad) " limitation row(s) drifted between the two tables: "
                 (vec bad)))))))

(deftest the-charter-and-the-adr-name-the-same-gates-test
  (let [adr-gates (gate-names (section-4 (text adr-path)))
        charter-gates (gate-names (text charter-path))]
    (is (= expected-row-count (count adr-gates))
        (str "ADR-0172 section 4 names " (count adr-gates) " gates, expected "
             expected-row-count))
    (is (= adr-gates charter-gates)
        (str "the two tables do not name the same gates -- ADR: " adr-gates
             ", charter: " charter-gates))
    (testing "every gate name is distinct -- every row owes its own test"
      (is (= expected-row-count (count (set (vals charter-gates))))))))

;; --- 3. every citation resolves, and anchors exactly one place -------------

(deftest every-charter-citation-resolves-test
  (let [cits (citations (text charter-path))]
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
  ;; ADR-0162's own red witness: the charter's VitalSign citation was
  ;; the bare string "DELIBERATELY UNDECLARED", a planted marker line
  ;; contained it, and the drift lint stayed GREEN. A snippet that
  ;; matches wherever it is pasted anchors nothing.
  (let [bad (for [{:keys [path snippet]} (citations (text charter-path))
                  :let [f (io/file path)]
                  :when (.isFile f)
                  :let [n (occurrences (slurp f) snippet)]
                  :when (> n 1)]
              (str path " :: \"" snippet "\" -- occurs " n " times"))]
    (is (empty? bad)
        (str (count bad) " charter citation(s) are not unique in their own file"
             " -- an anchor that matches twice anchors nothing: " (vec bad)))))

(deftest every-row-carries-at-least-one-citation-test
  (let [lines (filter #(re-find row-pattern %) (str/split-lines (text charter-path)))
        bad (for [line lines
                  :when (empty? (citations line))]
              (subs line 0 (min 80 (count line))))]
    (is (= expected-row-count (count lines)))
    (is (empty? bad)
        (str (count bad) " charter row(s) carry no citation at all: " (vec bad)))))

;; --- mechanism-sanity: prove the extractors catch what they claim to -------

(deftest row-and-gate-extraction-is-actually-caught-test
  (let [t (str "| 1 | **A thing is excluded.** why | because | `f.clj` \"snip\" | `a-test` -- red when |\n"
               "| 2 | **Another thing.** why | because | `g.clj` \"snap\" | `b-test` -- red when |\n")]
    (is (= {1 "A thing is excluded." 2 "Another thing."} (rows t)))
    (is (= {1 "a-test" 2 "b-test"} (gate-names t)))
    (is (= [{:path "f.clj" :snippet "snip"} {:path "g.clj" :snippet "snap"}]
           (citations t))))
  (testing "a row whose gate column is empty contributes no gate, so the count check bites"
    (is (= {} (gate-names "| 1 | **A thing.** why | because | `f.clj` \"snip\" | none |\n")))))

(deftest section-slicing-is-actually-caught-test
  (let [adr (text adr-path)]
    (is (str/includes? (section-4 adr) "Deliberate limitations"))
    (is (not (str/includes? (section-4 adr) "Rulings needed"))
        "section 4's slice leaked into section 5")
    (is (not (str/includes? (section-4 adr) "The census: every place demographics"))
        "section 4's slice leaked into section 1")
    (is (= "" (section-4 "no headings here at all")))))

;; --- 4. the drift lint: every PROVISIONAL rate is tabled -------------------

(def ^:private src-root "components/person-simulator/src")

;; The sibling's three tokens plus ADR-0172 row 9's fourth,
;; `PROVISIONAL`. Widening this pattern is allowed and expected;
;; narrowing it needs a reason, since every token dropped is a class of
;; drift this gate stops seeing.
(def ^:private marker-pattern #"PROVISIONAL|UNDECLARED|DELIBERATELY|not ported")

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

(defn- markers [root]
  (for [f (clj-files root)
        :let [path (str/replace (.getPath f) "\\" "/")
              lines (vec (str/split-lines (slurp f)))]
        idx (range (count lines))
        :when (re-find marker-pattern (nth lines idx))
        :let [[s e] (comment-block lines idx)]]
    {:file path :line (inc idx) :text (str/trim (nth lines idx))
     :block-lines (subvec lines s (inc e))}))

(defn- uncovered
  "Markers under `root` that no citation in `charter` lands inside."
  [root charter]
  (let [by-path (group-by :path (citations charter))]
    (for [m (markers root)
          :let [snippets (map :snippet (get by-path (:file m)))]
          :when (not (some (fn [sn] (some #(str/includes? % sn) (:block-lines m)))
                           snippets))]
      (select-keys m [:file :line :text]))))

(deftest every-provisional-rate-is-tabled-test
  (let [found (markers src-root)]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (seq found)
          (str "no deliberate-limitation marker found under " src-root
               " -- the scan proves nothing; every authored hazard rate owes a"
               " PROVISIONAL marker (ADR-0172 row 9, ruling E1)")))
    (testing "and every authored rate constant carries one"
      (let [hazards (slurp (io/file src-root "ehrt/person_simulator/hazards.clj"))
            rates (map second (re-seq #"(?m)^\(def ([a-z0-9-]*rate[a-z0-9-]*)" hazards))]
        (is (seq rates) "no rate constant found in hazards.clj at all")
        (is (<= (count rates) (count (filter #(= (str src-root "/ehrt/person_simulator/hazards.clj")
                                                 (:file %)) found)))
            (str (count rates) " rate constant(s) in hazards.clj but only "
                 (count (filter #(str/includes? (:file %) "hazards") found))
                 " PROVISIONAL marker(s)"))))
    (let [bad (uncovered src-root (text charter-path))]
      (is (empty? bad)
          (str (count bad) " in-source limitation marker(s) with no covering citation in "
               charter-path " -- table each, or the decline is undocumented: " (vec bad))))))

;; --- 5. every gate the table names actually exists -------------------------

(defn- deftest-names
  "Every `deftest` name declared under the given test roots."
  [roots]
  (into #{}
        (for [root roots
              f (clj-files root)
              [_ nm] (re-seq #"\(deftest ([a-z0-9-]+)" (slurp f))]
          nm)))

(deftest every-gate-the-charter-names-exists-as-a-test-test
  (let [named (set (vals (gate-names (text charter-path))))
        declared (deftest-names ["components/person-simulator/test"
                                 "components/docs-tooling/test"])]
    (testing "population is non-empty (R-empty-population-is-red)"
      (is (= expected-row-count (count named)))
      (is (seq declared)))
    (let [missing (sort (remove declared named))]
      (is (empty? missing)
          (str (count missing) " limitation gate(s) the charter names have no deftest"
               " anywhere: " (vec missing))))))

;; --- mechanism-sanity for the drift lint ----------------------------------

(deftest marker-coverage-is-actually-caught-test
  (let [tmp (io/file (System/getProperty "java.io.tmpdir")
                     (str "person-simulator-charter-test-" (System/nanoTime)))
        src (io/file tmp "src")
        f (io/file src "planted.clj")]
    (try
      (.mkdirs src)
      (spit f (str ";; a first comment line about the rate below\n"
                   ";; PROVISIONAL move rate, invented on the spot\n"
                   "(def r 0.11)\n"))
      (testing "an in-source marker with no citation at all is caught"
        (is (= 1 (count (uncovered (.getPath src) "no citations here")))))
      (testing "a citation landing inside the marker's own comment block covers it"
        ;; Deliberately cites a NEIGHBOURING line in the same block, not the
        ;; marker line itself -- citing the marker text back at itself is the
        ;; self-match ADR-0162's own red witness caught.
        (is (empty? (uncovered (.getPath src)
                               (str "| x | y | z | `" (.getPath f)
                                    "` \"a first comment line about the rate below\" | g |")))))
      (testing "a citation into the RIGHT file but the WRONG block does not cover it"
        (is (= 1 (count (uncovered (.getPath src)
                                   (str "| x | y | z | `" (.getPath f) "` \"(def r 0.11)\" | g |"))))))
      (testing "a citation into a DIFFERENT file does not cover it"
        (is (= 1 (count (uncovered (.getPath src)
                                   "| x | y | z | `deps.edn` \"PROVISIONAL\" | g |")))))
      (finally
        (when (.exists f) (.delete f))
        (when (.exists src) (.delete src))
        (when (.exists tmp) (.delete tmp))))))

(deftest comment-block-extraction-is-actually-caught-test
  (let [lines ["(def a 1)" "   ;; one" "   ;; two" "   ;; three" "(def b 2)"]]
    (is (= [1 3] (comment-block lines 2)))
    (is (= [1 3] (comment-block lines 1)))
    (is (= [0 0] (comment-block lines 0)))))
