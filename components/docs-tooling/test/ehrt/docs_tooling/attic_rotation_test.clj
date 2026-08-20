(ns ehrt.docs-tooling.attic-rotation-test
  "The attic rotation law (author ruling 2026-08-20, `notes/adr/
  0161-attic-rotation-law.md`; the row it closes is
  `roadmap.md#attic-rotation-law`, opened as ADR-0139 finding C-3 and
  made worse by ADR-0144, which retokened the Done pointers and added
  six missing ones rather than rotating any).

  The law, verbatim from the ruling:

    - `## Done` holds at most 30 LINES (not rows). The unit is lines
      because the reading-set budget counts lines.
    - Rotation is an act of the CLOSE CEREMONY: after a session adds
      its CLOSED row(s), if `## Done` exceeds 30 lines, the session
      rotates oldest rows (whole rows, never split) into the current
      month's attic file until <= 30, appending verbatim, chronological
      order preserved.
    - Attic files are append-only, one per month, flat under
      `.agents/plans/`; a new month's first rotation creates the file
      with the same header shape as the existing two.
    - Nothing pins: a rotated row is recorded twice over (its closing
      ADR, gated by `ehrt.docs-tooling.done-pointer-adr-test`; its
      verbatim attic copy).

  WHY LINES, AND WHICH LINES. The cap counts the `## Done` header line
  and every line under it to the next `## ` heading or end of file --
  the same extent `ehrt.docs-tooling.done-pointer-adr-test` reads, and
  the same extent the 134-line measurement in ADR-0161's own Step 0
  reported. A row is never split to make the count: rotation moves
  whole rows, so the live section lands at or under the cap, not
  exactly on it.

  WHY THE SECOND GATE IS 'DELETES NOTHING' RATHER THAN 'IS A BYTE
  PREFIX'. Append-only is the property wanted; byte-prefix is one way
  to have it, and the attic's own history does not have that one. Two
  of the twelve committed revisions of `roadmap-done-2026-08.md`
  (`2991a70`, ADR-0055's deferred rotation coming home; `0ebca6d`, the
  player arc appending two UX-arc pointers into the UX arc's own
  section) insert into the MIDDLE of the file rather than at its end,
  so a prefix test would be red on history no session may rewrite.
  What is true of all twelve, re-derived at ADR-0161's Step 0 by
  `git diff --numstat` over every consecutive pair, is that NONE of
  them deletes a line. That is the enforceable form of `rows verbatim,
  append-only`, and it is what this namespace gates -- over the file's
  whole committed history plus the working tree, not just the last
  commit, so the gate does not depend on `HEAD~1` existing or on which
  commit of a push happens to carry the rotation.

  WHY THE BYTE-PRESERVATION READ-BACK IS NOT A TEST HERE. `the union of
  live Done rows plus attic rows equals the pre-rotation Done set` is a
  statement about a TRANSITION between two commits, not a property of
  the tree. Encoded as a test it would either freeze one migration's
  constants into a permanent gate whose population can never grow --
  the `population is a registry rather than the tree` class ADR-0139
  named and this workspace keeps finding -- or degenerate into the
  append-only gate below. It is ADR-0161's own read-back table
  instead, computed as a byte comparison of the rotated block against
  the pre-rotation roadmap's own lines 285-393.

  Population is the TREE, not a list: every `.agents/plans/roadmap-
  done-*.md` is an attic file, so a new month's first rotation is
  gated the moment its file exists."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]))

(def ^:private roadmap-path ".agents/plans/roadmap.md")

(def ^:private done-line-cap
  "Author ruling 2026-08-20. Lines, not rows, header line included."
  30)

(defn- attic-paths
  "Every attic file, enumerated from the tree. Sorted, so the failure
  message is stable."
  []
  (->> (file-seq (io/file ".agents/plans"))
       (filter #(.isFile %))
       (map #(.getPath %))
       (filter #(re-matches #".*/roadmap-done-\d{4}-\d{2}\.md" %))
       sort
       vec))

;; -- gate (i): the live Done section's line cap --

(defn- done-section-lines
  "The live roadmap's own `## Done` section: the header line and every
  line under it up to the next `## ` heading or end of file."
  [content]
  (let [lines (str/split-lines content)
        start (->> lines
                   (keep-indexed (fn [i l] (when (str/starts-with? l "## Done") i)))
                   first)]
    (when start
      (vec (cons (nth lines start)
                 (take-while #(not (str/starts-with? % "## ")) (drop (inc start) lines)))))))

(deftest done-section-is-within-its-line-cap-test
  (testing "the close ceremony rotates oldest whole rows to the current month's attic until ## Done is at or under 30 lines"
    (let [section (done-section-lines (slurp roadmap-path))]
      (is (some? section) "the live roadmap has a ## Done section at all")
      (is (<= (count section) done-line-cap)
          (str ".agents/plans/roadmap.md's ## Done section is " (count section)
               " lines, over its " done-line-cap "-line cap by "
               (- (count section) done-line-cap)
               " -- rotate oldest WHOLE rows verbatim into "
               ".agents/plans/roadmap-done-<yyyy-mm>.md, oldest first, until it fits "
               "(ADR-0161; the law is mechanical, no arc boundaries)")))))

;; -- gate (ii): an attic file never deletes a line --

(defn- git
  [& args]
  (let [{:keys [exit out]} (apply shell/sh "git" args)]
    (when-not (zero? exit)
      (throw (ex-info (str "git " (str/join " " args) " failed") {:args args})))
    out))

(defn- numstat-deletions
  "The DELETED-line count `git diff --numstat` reports for one path, or
  0 when the path did not change (empty output). Parsed from --numstat
  rather than by reading the unified diff, deliberately: an attic row
  starts with `- `, so a DELETED row renders as `-- CLOSED ...` and the
  obvious `^-[^-]` scan would miss exactly the deletion this gate
  exists to catch."
  [numstat-out]
  (or (some-> (re-find #"(?m)^(\d+)\t(\d+)\t" numstat-out) (nth 2) parse-long) 0))

(defn- revisions
  "Every commit that touched `path`, oldest first."
  [path]
  (->> (git "log" "--format=%H" "--reverse" "--" path)
       str/split-lines
       (remove str/blank?)
       vec))

(defn- deleting-steps
  "Every step in `path`'s own life that deleted a line, as
  `{:from :to :deleted}`. The last step compares HEAD against the
  WORKING TREE, so a rotation that edits an existing attic row is red
  before it is ever committed."
  [path]
  (let [revs (revisions path)
        committed (for [[a b] (map vector revs (rest revs))
                        :let [d (numstat-deletions (git "diff" "--numstat" a b "--" path))]
                        :when (pos? d)]
                    {:from (subs a 0 8) :to (subs b 0 8) :deleted d})
        working (numstat-deletions (git "diff" "--numstat" "HEAD" "--" path))]
    (vec (cond-> committed
           (pos? working) (concat [{:from "HEAD" :to "working tree" :deleted working}])))))

(deftest the-attic-population-is-not-empty-test
  (testing "non-vacuity: a gate over zero attic files finds nothing wrong with any of them"
    (let [paths (attic-paths)]
      (is (seq paths) "no .agents/plans/roadmap-done-<yyyy-mm>.md file found at all")
      (is (some #(< 1 (count (revisions %))) paths)
          (str "no attic file has more than one committed revision, so the append-only walk "
               "below compares nothing -- is this a shallow clone? (CI uses fetch-depth: 0)")))))

(deftest no-attic-file-has-ever-deleted-a-line-test
  (testing "append-only: rows move into the attic verbatim and never leave or change"
    (doseq [path (attic-paths)]
      (let [bad (deleting-steps path)]
        (is (empty? bad)
            (str path " lost lines at " (count bad) " step(s) of its own history: " bad
                 " -- an attic file is append-only (ADR-0161); rows move in verbatim "
                 "and are never edited, reordered or removed"))))))

;; -- mechanism sanity: prove each extraction and each failure branch actually fires --

(deftest done-section-extraction-is-actually-caught-test
  (let [fixture "## Next\n- OPEN **[a]** PRIORITY 1\n\n## Done (live)\n- CLOSED 2026-08-20 ADR-0161 **[b]**\n  continuation\n"]
    (is (= ["## Done (live)" "- CLOSED 2026-08-20 ADR-0161 **[b]**" "  continuation"]
           (done-section-lines fixture))
        "the header line counts, the ## Next rows above it do not"))
  (let [fixture "## Done (live)\n- CLOSED 2026-08-20 ADR-0161 **[b]**\n\n## Appendix\n- not Done\n"]
    (is (= ["## Done (live)" "- CLOSED 2026-08-20 ADR-0161 **[b]**" ""]
           (done-section-lines fixture))
        "the section ends at the next ## heading, not only at end of file"))
  (is (nil? (done-section-lines "## Next\n- OPEN **[a]** PRIORITY 1\n"))
      "no ## Done section at all is reported, not silently measured as zero lines"))

(deftest an-over-cap-done-section-is-actually-caught-test
  (let [over (str "## Done (live)\n" (str/join (repeat done-line-cap "- CLOSED 2026-08-20 ADR-0161 **[x]**\n")))]
    (is (< done-line-cap (count (done-section-lines over)))
        "sanity: the cap's own failure branch fires -- 31 lines is over 30")))

(deftest numstat-parsing-is-actually-caught-test
  (is (= 0 (numstat-deletions "")) "an unchanged path produces no numstat line at all")
  (is (= 0 (numstat-deletions "939\t0\t.agents/plans/roadmap-done-2026-08.md")))
  (is (= 1676 (numstat-deletions "282\t1676\t.agents/plans/roadmap.md")))
  (is (= 4 (numstat-deletions "0\t4\t.agents/plans/roadmap.md\n9\t0\tother.md"))
      "the FIRST record is the one path asked for -- --numstat is always called with a pathspec"))

(deftest the-deletion-detector-fires-on-real-git-history-test
  (testing "both branches proven at one pinned pair -- 5b6e439 (ADR-0144's own migration) cut 1,676 lines out of the roadmap and moved 939 into the attic, deleting none"
    (is (= 1676 (numstat-deletions (git "diff" "--numstat" "5b6e439^" "5b6e439" "--" roadmap-path)))
        "a real deletion is seen")
    (is (zero? (numstat-deletions (git "diff" "--numstat" "5b6e439^" "5b6e439" "--"
                                       ".agents/plans/roadmap-done-2026-08.md")))
        "a real 939-line append is not mistaken for one")))
