(ns ehrt.docs-tooling.done-pointer-adr-test
  "AR-B-4 (scaffolding compaction B, 2026-08-05, `notes/ADRs.md`
  ADR-0046): the live roadmap's own Done section stopped holding full
  session write-ups and started holding one-line pointers (date, slug,
  ADR number) once the compaction arc's history rotated out to
  `.agents/plans/roadmap-done-*.md` (AR-B-3). Nothing enforced that a
  pointer's own ADR number actually resolves -- a future session could
  add a Done pointer citing a typo'd or retired number and nothing
  would fail, sending a reader who follows it nowhere.

  One direction only (AR-B-4's own scope: 'both-direction checks are
  C's scope if wanted; this gate is one-direction, cheap'): every
  Done-pointer ADR number must exist in `notes/ADRs.md`'s own index.
  The reverse (every index entry has a Done pointer) is not checked --
  most ADRs never get one; only a Done pointer's own dangling
  reference is the failure mode this test exists to catch.

  Same shape as `ehrt.docs-tooling.reading-set-budget-test`'s own
  pairing: the real gate over the live files, plus a mechanism-sanity
  test on synthetic fixture data proving the extraction functions
  themselves actually catch what they claim to."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(def ^:private roadmap-path ".agents/plans/roadmap.md")
(def ^:private adr-index-path "notes/ADRs.md")

(defn- done-section
  "The live roadmap's own `## Done` section content -- from the line
  starting `## Done` to end of file (the Done section is always last)."
  [content]
  (let [lines (str/split-lines content)
        done-start (->> lines
                         (keep-indexed (fn [i l] (when (str/starts-with? l "## Done") i)))
                         first)]
    (if done-start
      (str/join "\n" (drop done-start lines))
      "")))

(defn- done-pointer-adr-numbers
  "Every `ADR-NNNN` cited by a one-line Done pointer (`- DATE — slug —
  ADR-NNNN`) in `content`'s own Done section. Line-anchored to
  `- `-leading bullet lines so an ADR number mentioned in the Done
  section's own header/prose is never mistaken for a pointer."
  [content]
  (->> (str/split-lines (done-section content))
       (filter #(str/starts-with? % "- "))
       (keep #(second (re-find #"(ADR-\d{4})\s*$" %)))
       distinct))

(defn- indexed-adr-numbers
  "Every `ADR-NNNN` `notes/ADRs.md`'s own index lists (a `- **ADR-NNNN**
  ...` line)."
  [content]
  (->> (re-seq #"(?m)^- \*\*(ADR-\d{4})\*\*" content)
       (map second)
       set))

(deftest every-done-pointer-cites-an-adr-that-exists-in-the-index-test
  (testing "every Done one-liner in the live roadmap cites an ADR number the index actually lists"
    (let [roadmap (slurp roadmap-path)
          index (slurp adr-index-path)
          cited (done-pointer-adr-numbers roadmap)
          known (indexed-adr-numbers index)
          dangling (remove known cited)]
      (is (empty? dangling)
          (str ".agents/plans/roadmap.md's Done section cites ADR number(s) not in notes/ADRs.md's own index: "
               (vec dangling))))))

;; -- mechanism-sanity: prove the extraction functions actually catch what they claim to --

(deftest done-pointer-extraction-is-actually-caught-test
  (let [fixture (str "## Now\n- something in progress, not a Done pointer, ADR-9999\n\n"
                      "## Done (live)\n"
                      "- 2026-08-05 — scaffolding-compaction-a — ADR-0045\n"
                      "- 2026-08-05 — scaffolding-compaction-b — ADR-0046\n")]
    (is (= ["ADR-0045" "ADR-0046"] (done-pointer-adr-numbers fixture))
        "only bullet lines inside the Done section are read as pointers -- the Now-section ADR-9999 mention must not leak in")))

(deftest indexed-adr-numbers-extraction-is-actually-caught-test
  (let [fixture "- **ADR-0001** — Title — [`0001-title.md`](adr/0001-title.md) — Accepted\n- **ADR-0045** — Other — [`0045-other.md`](adr/0045-other.md) — Accepted\n"]
    (is (= #{"ADR-0001" "ADR-0045"} (indexed-adr-numbers fixture)))))

(deftest a-dangling-done-pointer-is-caught-test
  (let [cited ["ADR-0045" "ADR-9999"]
        known #{"ADR-0045"}
        dangling (remove known cited)]
    (is (= ["ADR-9999"] dangling)
        "sanity: a Done pointer citing a number absent from the index must be reported, proving the over-budget-style failure branch actually fires")))
