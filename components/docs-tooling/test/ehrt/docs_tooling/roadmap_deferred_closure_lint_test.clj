(ns ehrt.docs-tooling.roadmap-deferred-closure-lint-test
  "Lint family (AR-LF-3(i), D2-5, `.agents/plans/2026-08-07-repo-review-
  findings.md`): the Deferred section's own standing contract (AR-A-5,
  `.agents/rulings.md`) is that a row that closes moves to Done WITH
  its notes intact -- never left in place with a closure note
  substituted. `myocardial_infarction.json` violated exactly this
  pattern for an extended period (ADR-0047, fixed ADR-0055/0048) before
  any session caught it by hand; nothing mechanically catches a
  repeat. This test scans the LIVE Deferred section for rows that
  close IN PLACE ('RESOLVED'/'CLOSED'/'FIXED') without also disclosing
  where the closed content relocated to (D7-3's own compliant shape --
  'a row with sub-items marked closed-in-place... explicitly discloses
  its own relocation, which is the compliant shape, not the
  violation').

  A row is compliant if every in-place closure word it contains is
  accompanied SOMEWHERE in that same row by a relocation-disclosure
  phrase ('see Done', 'relocated', or 'moved to Done') -- this
  deliberately does not require the two to sit adjacent, since the
  live compliant precedent rows state the closure and the relocation
  in different sentences of the same row.

  The closure-word match is deliberately CASE-SENSITIVE, ALL-CAPS only
  -- matching the D2-5 finding's own literal shape
  ('RESOLVED'/'CLOSED'/'FIXED'), the convention this repo's own two
  live closure rows actually use ('**CLOSED 2026-08-07...', 'CLOSED
  this session'). A case-insensitive match was tried first and threw a
  false positive on ordinary prose ('a real bug found and fixed
  mid-step', describing an unrelated incident, not this row closing)
  -- the pattern was wrong, not the row; narrowing to the all-caps
  status-marker convention fixed it without touching the row."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]))

(def ^:private roadmap-path ".agents/plans/roadmap.md")

(def ^:private closure-word-pattern #"\b(RESOLVED|CLOSED|FIXED)\b")
(def ^:private disclosure-phrase-pattern #"(?i)(see Done|relocated|moved to Done)")

(defn- section-lines
  "The lines of `content`'s own section starting at a `## <heading>`
  line matching `heading`, up to (not including) the next `## ` line,
  or end of file."
  [content heading]
  (let [lines (str/split-lines content)
        start (->> lines
                   (keep-indexed (fn [i l] (when (str/starts-with? l heading) i)))
                   first)]
    (when start
      (let [after (drop (inc start) lines)]
        (cons (nth lines start)
              (take-while #(not (str/starts-with? % "## ")) after))))))

(defn- rows
  "Groups `lines` (a section's own lines, header line included) into
  top-level bullet rows: every line starting `- ` at column 0 begins a
  new row; every following line (indented continuation prose,
  including nested **Dated note** paragraphs) belongs to that same
  row, until the next top-level bullet."
  [lines]
  (->> lines
       (reduce (fn [acc line]
                 (if (str/starts-with? line "- ")
                   (conj acc [line])
                   (if (seq acc)
                     (conj (pop acc) (conj (peek acc) line))
                     acc)))
               [])
       (map #(str/join "\n" %))))

(defn- closes-in-place-without-disclosure?
  [row]
  (and (re-find closure-word-pattern row)
       (not (re-find disclosure-phrase-pattern row))))

(deftest deferred-rows-that-close-in-place-disclose-their-own-relocation-test
  (testing "a row closing in place without naming where its content relocated to is the myocardial_infarction.json incident, recurring"
    (let [lines (section-lines (slurp roadmap-path) "## Deferred")
          violations (filter closes-in-place-without-disclosure? (rows lines))]
      (is (empty? violations)
          (str "Deferred row(s) close in place ('RESOLVED'/'CLOSED'/'FIXED') without disclosing "
               "where the closed content relocated to (D2-5, D7-3's compliant shape) -- "
               "either add a 'see Done'/'relocated'/'moved to Done' disclosure, or this is "
               "the myocardial_infarction.json pattern recurring: "
               (vec (map #(subs % 0 (min 80 (count %))) violations)))))))

;; -- mechanism-sanity: prove the extraction functions actually catch what they claim to --

(deftest section-lines-extraction-is-actually-caught-test
  (let [fixture "## Now\n- in progress\n\n## Deferred\n- row one\n  continuation\n- row two\n\n## Done\n- irrelevant\n"]
    (is (= ["## Deferred" "- row one" "  continuation" "- row two" ""]
           (section-lines fixture "## Deferred")))))

(deftest rows-grouping-is-actually-caught-test
  (is (= ["- row one\n  continuation" "- row two"]
         (rows ["- row one" "  continuation" "- row two"]))))

(deftest a-closed-in-place-row-without-disclosure-is-caught-test
  (is (closes-in-place-without-disclosure? "- **Some item** CLOSED this session, done.")))

(deftest a-closed-in-place-row-with-disclosure-is-not-flagged-test
  (testing "the live compliant precedent rows must pass -- disclosure anywhere in the row is sufficient"
    (is (not (closes-in-place-without-disclosure?
              "- **Census tool refinements**: (a) and (c) **CLOSED** ... their own original text relocated verbatim into ADR-0069's own record.")))
    (is (not (closes-in-place-without-disclosure?
              "- **Lookup-table column time**: The race half of the original combined row CLOSED this session — see Done, below.")))))

(deftest a-row-mentioning-neither-closure-word-is-never-flagged-test
  (is (not (closes-in-place-without-disclosure? "- an ordinary open Deferred row, untouched"))))
