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
  "Every `ADR-NNNN` cited by a Done pointer in `content`'s own Done
  section. Line-anchored to `- `-leading bullet lines so an ADR number
  in the section's own header/prose is never mistaken for a pointer,
  and FIRST-match per line so a pointer's own trailing note cannot add
  a second.

  2026-08-17 (ADR-0144): the pointer shape became
  `- CLOSED <date> <ADR-NNNN|sha> **[slug]**` -- the token grammar the
  whole file now carries, which is the old `- DATE — slug — ADR-NNNN`
  with its fields reordered. The previous end-of-line anchor
  (`ADR-\\d{4}\\s*$`) matched ZERO of the 56 retokened pointers while
  the gate stayed green, because a gate that extracts nothing has
  nothing to find dangling. The anchor is dropped; the non-vacuity
  assertion above is what now holds the extraction honest, rather than
  a second anchor that the next reshape would break the same way."
  [content]
  (->> (str/split-lines (done-section content))
       (filter #(str/starts-with? % "- "))
       (keep #(second (re-find #"(ADR-\d{4})" %)))
       distinct))

(defn- indexed-adr-numbers
  "Every `ADR-NNNN` `notes/ADRs.md`'s own index lists (a `- **ADR-NNNN**
  ...` line)."
  [content]
  (->> (re-seq #"(?m)^- \*\*(ADR-\d{4})\*\*" content)
       (map second)
       set))

(defn- done-bullets-without-a-pointer
  "Every Done bullet line that carries NEITHER an `ADR-NNNN` nor a bare
  commit sha -- the two alternatives the row token's own
  `CLOSED <date> <ADR-NNNN|sha>` grammar allows.

  ADR-0158 replaced a proxy here. The non-vacuity check used to compare
  the count of DISTINCT cited ADR numbers against the count of bullets,
  tolerating a gap of two. That held only while no ADR had ever closed
  more than one roadmap row: two bullets citing one ADR collapse to a
  single distinct value, so the proxy went red on a legitimate close
  (this session's own, closing #edit-root-worktree-residue and
  #intake-staging-dir together) while a bullet that genuinely LOST its
  pointer could hide behind any duplicate elsewhere. Counting bullets
  that carry a pointer is both immune to the first and strictly
  stronger against the second, which is what the assertion was for."
  [content]
  (->> (str/split-lines (done-section content))
       (filter #(str/starts-with? % "- "))
       (remove #(re-find #"(ADR-\d{4}|\b[0-9a-f]{7,40}\b)" %))
       vec))

(deftest the-done-pointer-scan-is-not-vacuous-test
  (testing "the gate below is worthless if the extraction returns nothing -- ADR-0144's own retokening
            (`- CLOSED DATE ADR-NNNN **[slug]**`) moved the ADR number off the end of the line, and the
            original `ADR-\\d{4}\\s*$` anchor silently extracted ZERO of 56 live pointers. Caught by
            checking rather than by the gate, which stayed green throughout; this assertion is what
            makes the next such reshape loud."
    (let [roadmap (slurp roadmap-path)
          bullets (->> (str/split-lines (done-section roadmap))
                       (filter #(str/starts-with? % "- "))
                       count)
          pointerless (done-bullets-without-a-pointer roadmap)]
      (is (pos? bullets) "the live roadmap has a ## Done section with pointers in it")
      (is (seq (done-pointer-adr-numbers roadmap))
          "the ADR extraction below matches nothing at all -- it has stopped reading the pointer shape")
      (is (empty? pointerless)
          (str (count pointerless) " of " bullets
               " Done bullet(s) carry neither an ADR-NNNN nor a sha, so the extraction has "
               "stopped matching the pointer shape on them:\n"
               (str/join "\n" (map #(str "  " %) pointerless)))))))

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

(deftest the-adr-0144-pointer-shape-is-actually-read-test
  (testing "the retokened shape, and the sha alternative that carries no ADR at all"
    (let [fixture (str "## Done (current arc only)\n"
                       "- CLOSED 2026-08-05 ADR-0045 **[scaffolding-compaction-a]**\n"
                       "- CLOSED 2026-08-16 30cc335 **[d8-5-fence-battery]** -- register\n"
                       "  `.agents/plans/2026-08-16-fence-battery-findings.md`; the ADR was deferred\n"
                       "  to the ruled fixes' own session (ADR-0140).\n"
                       "- CLOSED 2026-08-16 ADR-0141 **[event-log-contract]**\n")]
      (is (= ["ADR-0045" "ADR-0141"] (done-pointer-adr-numbers fixture))
          (str "the sha-tokened pointer contributes nothing (its own ADR-0140 mention is on a "
               "CONTINUATION line, not a bullet), and each bullet contributes its FIRST ADR only")))))

(deftest indexed-adr-numbers-extraction-is-actually-caught-test
  (let [fixture "- **ADR-0001** — Title — [`0001-title.md`](adr/0001-title.md) — Accepted\n- **ADR-0045** — Other — [`0045-other.md`](adr/0045-other.md) — Accepted\n"]
    (is (= #{"ADR-0001" "ADR-0045"} (indexed-adr-numbers fixture)))))

(deftest a-dangling-done-pointer-is-caught-test
  (let [cited ["ADR-0045" "ADR-9999"]
        known #{"ADR-0045"}
        dangling (remove known cited)]
    (is (= ["ADR-9999"] dangling)
        "sanity: a Done pointer citing a number absent from the index must be reported, proving the over-budget-style failure branch actually fires")))

(deftest a-pointerless-done-bullet-is-caught-test
  (testing "mechanism sanity: the non-vacuity check's own failure branch fires, and two bullets sharing one ADR do NOT trip it"
    (let [good (str "## Done (live)\n"
                    "- CLOSED 2026-08-19 ADR-0158 **[one]** -- a\n"
                    "- CLOSED 2026-08-19 ADR-0158 **[two]** -- b\n"
                    "- CLOSED 2026-08-16 30cc335 **[sha-tokened]** -- c\n")
          bad (str good "- CLOSED 2026-08-19 **[no-pointer-at-all]** -- d\n")]
      (is (empty? (done-bullets-without-a-pointer good))
          "two bullets closed by ONE ADR, plus a sha-tokened bullet, are all properly pointed")
      (is (= 1 (count (done-bullets-without-a-pointer bad)))
          "a bullet with no ADR and no sha is reported"))))
