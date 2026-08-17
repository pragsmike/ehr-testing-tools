(ns ehrt.docs-tooling.rulings-lint-test
  "Compression arc session C (`notes/adr/0145-rulings-standing-register.md`).

  `.agents/rulings.md` has said since ADR-0047 that it holds \"only the
  STANDING rulings\" and \"is NOT a history\". It said so through 55
  `## From ...` blocks and 1,757 lines of execution record, because the
  contract as written -- \"standing rulings only, appended at each arc
  close\" -- names no shape a test can hold. A rule stated and never
  gated documents an intention, not a constraint (the same finding
  ADR-0144 made about the roadmap's own \"one line per item\" header).

  So the register gets a row contract, and this namespace holds it:

      - **R-<slug>** -- <rule> -- ADR-NNNN [SUPERSEDED-BY R-<slug> (ADR-NNNN)]

  Six gates over the live file:

  - **grammar**: every top-level bullet is a row of exactly that shape.
  - **unique slugs**: a slug is how a rule is cited (`rulings.md#R-<slug>`),
    so a duplicate makes a citation ambiguous.
  - **the cited ADR resolves**: a row may not cite a record that does not
    exist -- the same guard `done-pointer-adr-test` gives the roadmap.
  - **three lines a row**: the cap that keeps a rule a rule. A row that
    needs a paragraph is a narrative, and the narrative belongs in the ADR.
  - **no `## From` block survives**: the shape this ADR retires cannot
    grow back one heading at a time.
  - **a SUPERSEDED row names a successor that exists** in this same file,
    so \"superseded\" is never a dead end.

  Below the six, mechanism-sanity cases prove each pattern actually
  discriminates -- a known-good row matches and a known-bad row is
  rejected, on synthetic strings rather than the live file, so a future
  edit to `.agents/rulings.md` can never make them vacuously true. That
  discipline is not decoration here: ADR-0144's own token pattern
  terminated with `\\b`, silently rejected every `DEFERRED` row (which
  ends in `)`, a non-word character), and would have passed green over
  that whole class forever if a sanity case had not caught it."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private rulings-path ".agents/rulings.md")
(def ^:private adr-dir "notes/adr")
(def ^:private max-row-lines 3)

;; -- the row grammar --
;;
;; The rule text may itself contain ` -- ` (several rules do), so the
;; body is matched greedily and the LAST ` -- ADR-NNNN` is the cite. The
;; optional successor clause cannot be mistaken for that cite: its own
;; ADR number is parenthesised.

(def ^:private row-pattern
  #"^- \*\*R-([a-z0-9-]+)\*\* -- (.+) -- ADR-(\d{4})(?: SUPERSEDED-BY R-([a-z0-9-]+) \(ADR-(\d{4})\))?$")

(def ^:private block-heading-pattern #"(?m)^## From ")

(defn- read-lines [path]
  (with-open [r (io/reader path)]
    (vec (line-seq r))))

(defn- rows
  "Every top-level bullet, as `{:start <1-based line> :lines [...]}`. A row
  begins at a line starting `- ` and continues through every following
  line indented by two spaces."
  [lines]
  (loop [i 0, acc []]
    (if (>= i (count lines))
      acc
      (if (str/starts-with? (nth lines i) "- ")
        (let [j (loop [j (inc i)]
                  (if (and (< j (count lines))
                           (str/starts-with? (nth lines j) "  "))
                    (recur (inc j))
                    j))]
          (recur j (conj acc {:start (inc i) :lines (subvec (vec lines) i j)})))
        (recur (inc i) acc)))))

(defn- flatten-row
  "A row as one line: continuation lines joined by a single space, their
  two-space indent dropped. This is what the grammar matches, so wrapping
  is a style choice the gate does not care about."
  [{:keys [lines]}]
  (str/join " " (cons (first lines)
                      (map str/triml (rest lines)))))

(defn- parse-row [row]
  (when-let [[_ slug body adr succ succ-adr] (re-matches row-pattern (flatten-row row))]
    {:slug slug :body body :adr adr :superseded-by succ :superseded-adr succ-adr}))

(defn- adr-exists? [num]
  (some #(str/starts-with? (.getName %) (str num "-"))
        (filter #(.isFile %) (file-seq (io/file adr-dir)))))

;; -- the six gates, over the live .agents/rulings.md --

(deftest every-row-matches-the-row-contract-test
  (testing "ADR-0145: every top-level bullet is `- **R-<slug>** -- <rule> -- ADR-NNNN`"
    (let [lines (read-lines rulings-path)
          all (rows lines)
          bad (remove parse-row all)]
      (is (seq all) "sanity: the register has rows at all -- an empty file passes every other gate vacuously")
      (is (empty? bad)
          (str (count bad) " row(s) in " rulings-path " do not match the row contract, at line(s) "
               (vec (map :start bad)) ": " (vec (map #(subs (flatten-row %) 0 (min 90 (count (flatten-row %)))) bad)))))))

(deftest row-slugs-are-unique-test
  (testing "a slug is how a rule is cited (rulings.md#R-<slug>); a duplicate makes the citation ambiguous"
    (let [slugs (keep (comp :slug parse-row) (rows (read-lines rulings-path)))
          dupes (->> slugs frequencies (filter #(> (val %) 1)) (map key) vec)]
      (is (empty? dupes) (str "duplicate rule slug(s) in " rulings-path ": " dupes)))))

(deftest every-cited-adr-resolves-test
  (testing "a row may not cite an ADR record that does not exist"
    (let [parsed (keep parse-row (rows (read-lines rulings-path)))
          dangling (->> parsed
                        (mapcat (juxt :adr :superseded-adr))
                        (remove nil?)
                        distinct
                        (remove adr-exists?)
                        vec)]
      (is (empty? dangling)
          (str "row(s) in " rulings-path " cite ADR number(s) with no file in " adr-dir ": " dangling)))))

(deftest no-row-exceeds-three-lines-test
  (testing "three lines a row: a rule that needs a paragraph is a narrative, and belongs in its ADR"
    (let [over (filter #(> (count (:lines %)) max-row-lines) (rows (read-lines rulings-path)))]
      (is (empty? over)
          (str (count over) " row(s) over the " max-row-lines "-line cap, at line(s) "
               (vec (map :start over)) " (lengths " (vec (map #(count (:lines %)) over)) ")")))))

(deftest no-from-block-headings-remain-test
  (testing "the shape ADR-0145 retires cannot grow back one heading at a time"
    (let [body (slurp rulings-path)
          hits (count (re-seq block-heading-pattern body))]
      (is (zero? hits)
          (str hits " `## From ...` block heading(s) remain in " rulings-path
               " -- a ruling's narrative belongs in the ADR that owns it, not here (ADR-0145)")))))

(deftest superseded-rows-name-a-successor-that-exists-test
  (testing "'superseded' is never a dead end: the named successor is a row in this same file"
    (let [parsed (keep parse-row (rows (read-lines rulings-path)))
          slugs (set (map :slug parsed))
          orphans (->> parsed
                       (filter :superseded-by)
                       (remove #(contains? slugs (:superseded-by %)))
                       (map (juxt :slug :superseded-by))
                       vec)]
      (is (empty? orphans)
          (str "SUPERSEDED row(s) naming a successor with no row of its own: " orphans)))))

;; -- mechanism sanity: prove each pattern discriminates, on synthetic data --

(def ^:private good-row
  "- **R-mnt-c-retired** -- no session routes work through a Windows-mounted clone -- ADR-0047")

(def ^:private good-superseded-row
  "- **R-stable-tag-author-only** -- tagging remains the author's act alone -- ADR-0048 SUPERSEDED-BY R-tag-law (ADR-0057)")

(deftest the-row-pattern-matches-a-known-good-row-test
  (let [m (re-matches row-pattern good-row)]
    (is (some? m) "a well-formed row must match")
    (is (= "mnt-c-retired" (nth m 1)))
    (is (= "0047" (nth m 3)))
    (is (nil? (nth m 4)) "a row with no successor clause reports no successor"))
  (let [m (re-matches row-pattern good-superseded-row)]
    (is (some? m) "a well-formed SUPERSEDED row must match")
    (is (= "0048" (nth m 3)) "the rule's own ADR is the bare cite, not the successor's parenthesised one")
    (is (= "tag-law" (nth m 4)))
    (is (= "0057" (nth m 5)))))

(deftest the-row-pattern-rejects-known-bad-rows-test
  (doseq [[why bad] [["no ADR cite at all"
                      "- **R-mnt-c-retired** -- no session routes work through a Windows-mounted clone"]
                     ["no `R-` prefix on the slug"
                      "- **mnt-c-retired** -- no Windows-mounted clone -- ADR-0047"]
                     ["an upper-case slug -- slugs are the anchor and must be stable"
                      "- **R-MntC** -- no Windows-mounted clone -- ADR-0047"]
                     ["a three-digit ADR number"
                      "- **R-mnt-c-retired** -- no Windows-mounted clone -- ADR-047"]
                     ["prose, not a row"
                      "- The intake-front-door doctrine (AR-M1-4): a sim run enters intake"]
                     ["a successor clause without its parenthesised ADR"
                      "- **R-a** -- x -- ADR-0047 SUPERSEDED-BY R-b"]]]
    (is (nil? (re-matches row-pattern bad)) (str "must be rejected: " why))))

(deftest the-grammar-tolerates-a-rule-containing-its-own-separator-test
  (let [m (re-matches row-pattern
                      "- **R-x** -- a rule -- with an embedded separator -- ADR-0047")]
    (is (some? m) "several real rules contain ` -- `; the LAST one is the cite")
    (is (= "a rule -- with an embedded separator" (nth m 2)))
    (is (= "0047" (nth m 3)))))

(deftest row-extraction-folds-continuation-lines-test
  (let [lines ["# Header" "" "- **R-a** -- first half of a wrapped rule and its" "  second half -- ADR-0047"
               "- **R-b** -- short -- ADR-0048"]
        rs (rows lines)]
    (is (= 2 (count rs)) "a wrapped row is one row, not two")
    (is (= 2 (count (:lines (first rs)))) "and its length is counted in lines, for the cap")
    (is (= "0047" (:adr (parse-row (first rs))))
        "a row wrapped across lines still parses -- wrapping is style, not grammar")))

(deftest the-block-heading-pattern-is-not-vacuous-test
  (is (= 2 (count (re-seq block-heading-pattern
                          "# Reg\n\n## From ADR-0043 (sim split)\n- x\n\n## From the UX arc\n- y\n")))
      "the heading pattern must actually find the shape it forbids")
  (is (empty? (re-seq block-heading-pattern
                      "# Reg\n\n- **R-a** -- x -- ADR-0047\n## Fromage is not a heading here\n"))
      "and must not fire on prose that merely starts with the same letters"))

(deftest adr-existence-check-is-not-vacuous-test
  (is (adr-exists? "0145") "sanity: this ADR's own file resolves")
  (is (not (adr-exists? "9999")) "a number with no file must not resolve"))
