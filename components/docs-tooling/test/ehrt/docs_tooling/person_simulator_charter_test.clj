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

  2. MIRROR: the charter's eleven rows and ADR-0172 section 4's eleven
     rows carry the same bolded limitation titles and name the same
     eleven gates. This is the obligation the sibling has no need of,
     and it exists because two tables of the same eleven facts in two
     files is exactly the drift this gate family was built for.

  3. Every citation RESOLVES: the quoted text occurs verbatim in the
     named file. Anchored by stable TEXT, never by line number
     (`rulings.md#R-anchored-register-edits`). And every citation
     anchors EXACTLY ONE place in its own file -- ADR-0162's own
     hard-won correction, bought by a red witness that stayed green
     because a generic snippet matched wherever it was pasted.

  Obligation 4, the in-source marker drift lint that limitations row 9
  names, lands with the component's own behaviour -- there is no rate
  constant to mark until there is a hazard, and a scan that finds
  nothing proves nothing (`rulings.md#R-empty-population-is-red`). So
  does obligation 5, the tie between the table's eleven named gates
  and eleven real `deftest`s."
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
  "ADR-0172 section 4 tables ELEVEN limitations. Pinned, so that a row
  quietly dropped from ONE of the two tables is caught by the mirror
  below while a row dropped from BOTH is caught here
  (`rulings.md#R-empty-population-is-red`, one step further: a mirror
  gate over two empty tables agrees perfectly)."
  11)

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

;; --- 2. the mirror: two tables, eleven rows, same titles, same gates -------

(deftest the-charter-and-the-adr-table-the-same-eleven-limitations-test
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

(deftest the-charter-and-the-adr-name-the-same-eleven-gates-test
  (let [adr-gates (gate-names (section-4 (text adr-path)))
        charter-gates (gate-names (text charter-path))]
    (is (= expected-row-count (count adr-gates))
        (str "ADR-0172 section 4 names " (count adr-gates) " gates, expected "
             expected-row-count))
    (is (= adr-gates charter-gates)
        (str "the two tables do not name the same gates -- ADR: " adr-gates
             ", charter: " charter-gates))
    (testing "every gate name is distinct -- eleven rows owe eleven tests"
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
