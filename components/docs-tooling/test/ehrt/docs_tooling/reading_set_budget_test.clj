(ns ehrt.docs-tooling.reading-set-budget-test
  "Item 8 (migration session 4, 2026-08-02, charter R-D):
  `.agents/reading-sets.edn` stops being a document a session might
  read and starts being data this test checks on every push. Two
  gates, both real, over the live file:

  - **No ghost paths**: every path any named set cites must resolve to
    a real file. A reading set that cites a path that no longer exists
    (renamed, moved, deleted) is worse than no reading set at all --
    it sends a cold session to read nothing and not notice.
  - **Budget**: every named set's real, measured line-count sum must
    not exceed its own `:budget-lines`. Migration session 4 seeded
    every budget to the exact measured actual of the set as composed
    (charter §6: real budget numbers are the author's own future
    ruling, not this session's) -- so landing green here proves the
    numbers are honest on day one, and the test is what makes future
    growth to a set a conscious, visible act instead of silent drift.

  Below the two real gates, three mechanism-sanity tests prove the
  checking functions themselves actually catch what they claim to,
  using synthetic fixture data rather than the live file (so a future
  edit to `.agents/reading-sets.edn` can never accidentally make these
  three vacuously true): a ghost path is caught, an over-budget seed
  is caught, and a lean well-formed set passes both checks green. Same
  shape as `ehrt.docs-tooling.index-completeness-test`'s own
  extraction-sanity tests -- the gate and the proof that the gate
  actually gates are two different tests."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [ehrt.docs-tooling.state-derived :as sd]))

(def ^:private reading-sets-path ".agents/reading-sets.edn")

(defn- read-reading-sets []
  (edn/read-string (slurp reading-sets-path)))

(defn- missing-paths
  "The subset of `paths` that does not resolve to a real file, relative
  to the workspace root (this test always runs cwd-at-root, same as
  every other docs-tooling filesystem test in this suite)."
  [paths]
  (remove #(.isFile (io/file %)) paths))

;; `line-count`/`total-lines` moved to `ehrt.docs-tooling.state-derived`
;; (ADR-0147) and are used from there rather than re-implemented here.
;; `.agents/state-derived.md` renders each set's measured actual, and
;; this gate enforces it against the budget: two numbers describing the
;; same file, so they are one function (R13, "one definition each").
;; Local copies drifting apart is not hypothetical -- ADR-0145's own
;; erratum records eight itemised per-path figures that summed to 1,393
;; against a stated actual of 1,446.
(def ^:private line-count sd/line-count)
(def ^:private total-lines sd/total-lines)

;; -- the two real gates, over the live .agents/reading-sets.edn --

(deftest every-reading-set-path-resolves-test
  (testing "no named set cites a path that doesn't exist -- a reading set can't cite a ghost"
    (doseq [[set-name {:keys [paths]}] (read-reading-sets)]
      (let [missing (missing-paths paths)]
        (is (empty? missing)
            (str set-name " cites a path that does not exist: " (vec missing)))))))

(deftest every-reading-set-is-within-its-own-budget-test
  (testing "every set's real, measured line-count sum is within its own :budget-lines"
    (doseq [[set-name {:keys [paths budget-lines]}] (read-reading-sets)]
      (let [actual (total-lines paths)]
        (is (<= actual budget-lines)
            (str set-name " is " actual " lines, over its " budget-lines
                 "-line budget by " (- actual budget-lines)))))))

;; -- guard #3: the ratchet (compression arc session A, ADR-0143) --
;;
;; The budget above is a ceiling that moved every time it was hit. Its
;; own file records the history plainly: a 2026-08-05 note superseding
;; FOURTEEN in-place bumps, then eleven further dated re-derivations
;; through 2026-08-16, each one honest, each one raising the number the
;; gate had just caught. A ceiling that rises on contact measures
;; growth; it does not resist it.
;;
;; So `:budget-lines` gains a ceiling of its own. `.agents/reading-sets-
;; baseline.edn` holds one integer per set, written by a COMPACTION ADR
;; at that ADR's own post-compaction measured actuals; a budget may fall
;; below it freely (compaction is always allowed) but may never exceed
;; it. A build session that runs out of headroom therefore has exactly
;; two moves -- compact the set's own paths back under the budget, or
;; STOP-AND-REPORT -- and no longer has the third one, which is what
;; happened eleven times.

(def ^:private baseline-path ".agents/reading-sets-baseline.edn")

(defn- read-baseline []
  (edn/read-string (slurp baseline-path)))

(defn- over-baseline
  "Sets whose `:budget-lines` exceeds the committed baseline, as
  `[set-name budget baseline]` triples. A set with no baseline entry is
  reported separately by the coverage test below, not silently skipped
  here."
  [sets baseline]
  (for [[set-name {:keys [budget-lines]}] sets
        :let [ceiling (get baseline set-name)]
        :when (and ceiling (> budget-lines ceiling))]
    [set-name budget-lines ceiling]))

(deftest no-budget-exceeds-the-committed-baseline-test
  (testing "every :budget-lines is at or below the ratchet baseline -- a budget may fall, never rise"
    (let [offenders (over-baseline (read-reading-sets) (read-baseline))]
      (is (empty? offenders)
          (str "reading-set budget(s) above the committed baseline in " baseline-path ": "
               (vec offenders)
               " -- compact, or bump by compaction ADR -- never in a build session.")))))

(deftest every-set-has-a-baseline-and-every-baseline-names-a-real-set-test
  (testing "both directions: the ratchet covers every set, and names no set that doesn't exist"
    (let [sets (set (keys (read-reading-sets)))
          baseline (set (keys (read-baseline)))]
      (is (empty? (remove baseline sets))
          (str "reading set(s) with no entry in " baseline-path ": " (vec (remove baseline sets))
               " -- an uncovered set has no ceiling at all, which is the state this guard exists to end."))
      (is (empty? (remove sets baseline))
          (str baseline-path " names set(s) that no longer exist in " reading-sets-path ": "
               (vec (remove sets baseline)))))))

;; -- mechanism-sanity: prove the two checks above actually catch bad data --

(deftest a-budget-above-its-baseline-is-caught-test
  (is (= [[:onboarding 3200 3105]]
         (vec (over-baseline {:onboarding {:budget-lines 3200} :docs {:budget-lines 800}}
                             {:onboarding 3105 :docs 840})))
      "a set bumped above its baseline is reported; a set comfortably under is not")
  (is (empty? (over-baseline {:onboarding {:budget-lines 3105}} {:onboarding 3105}))
      "equality is allowed -- the baseline IS the ceiling, not one below it")
  (is (empty? (over-baseline {:onboarding {:budget-lines 900}} {:onboarding 3105}))
      "a budget compacted well below its baseline is always fine -- the ratchet only bites upward"))


(deftest missing-paths-catches-a-ghost-test
  (is (= ["components/does-not-exist/ghost.clj"]
         (missing-paths ["AGENTS.md" "components/does-not-exist/ghost.clj"]))
      "a set citing one real path and one ghost must report exactly the ghost, not pass silently"))

(deftest total-lines-exceeding-an-absurdly-low-budget-fails-test
  (let [seeded-budget 1
        actual (total-lines ["AGENTS.md"])]
    (is (> actual seeded-budget)
        "sanity: AGENTS.md is more than one line")
    (is (not (<= actual seeded-budget))
        "an over-budget seed (budget-lines set to 1) must be exceeded by the real file's line count, proving the over-budget branch actually fires and isn't vacuous")))

(deftest a-lean-well-formed-set-passes-both-checks-test
  (let [paths ["AGENTS.md"]
        generous-budget 1000]
    (is (empty? (missing-paths paths))
        "AGENTS.md resolves -- the presence check passes on a real path")
    (is (<= (total-lines paths) generous-budget)
        "a generously-budgeted single real file passes the budget check")))

;; -- guard: the header stays a header (compression arc session C, ADR-0145) --
;;
;; `.agents/reading-sets.edn` grew to 531 lines of which 480 were comment:
;; a 415-line header carrying NINETEEN dated per-session budget
;; re-derivations, plus 65 lines of per-set rationale inside the map, over
;; 48 lines of actual data. The file's own header said the quiet part out
;; loud -- "They stay as per-session provenance; they are history, not
;; instructions" -- which is a description of a move it had not made.
;;
;; ADR-0145 moves that history VERBATIM to
;; `.agents/plans/reading-sets-history.md` and caps what may live here. The
;; cap is deliberately small: a header a session actually reads before
;; editing a set, and no more. A future re-derivation is recorded in its
;; own compaction ADR and in the history file, never appended here.

(def ^:private max-comment-lines 20)

(defn- comment-lines
  "Every `;;` comment line in the file, header and in-map alike. Counting
  both is the point: the growth this guard exists to stop arrived as
  per-set rationale as readily as as a dated header note."
  [path]
  (->> (str/split-lines (slurp path))
       (filter #(str/starts-with? (str/triml %) ";;"))))

(deftest reading-sets-edn-carries-a-header-not-a-history-test
  (testing "ADR-0145: `.agents/reading-sets.edn` is data with a short header; its derivation history lives in the plans attic"
    (let [n (count (comment-lines reading-sets-path))]
      (is (<= n max-comment-lines)
          (str reading-sets-path " carries " n " comment lines, over the "
               max-comment-lines "-line header cap by " (- n max-comment-lines)
               " -- move the history VERBATIM to .agents/plans/reading-sets-history.md "
               "(ADR-0145), never trim it away")))))

(deftest the-comment-line-counter-is-not-vacuous-test
  (let [tmp (java.io.File/createTempFile "reading-sets" ".edn")]
    (try
      (spit tmp ";; one\n  ;; two, indented inside a map\n{:a 1} ; not a ;; comment line\n\n;; three\n")
      (is (= 3 (count (comment-lines (.getPath tmp))))
          "an indented `;;` line counts, a trailing `;` on a data line does not, blanks do not")
      (finally (.delete tmp))))
  (is (pos? (count (comment-lines reading-sets-path)))
      "sanity: the live file has a header at all -- a zero here would pass the cap vacuously"))

;; -- set membership, ruled (compression arc session D, ADR-0147) --
;;
;; `:onboarding`'s composition is where this arc's work becomes visible
;; or invisible to a cold session. Two assertions, in opposite
;; directions, each guarding one half of what session D did:
;;
;;  - `.agents/state.md` JOINS the set. The continuity register was in
;;    no reading set at all -- 724 lines that a cold session was told to
;;    read by `AGENTS.md`'s own routing and by the `repo-review` skill,
;;    counted against no budget and gated by nothing but a currency
;;    tripwire. It is small and linted now, so it can be carried; and
;;    being carried is what puts it under the ratchet, which is the only
;;    thing that has ever kept a register in this repo small.
;;  - No generated `INDEX.md` joins any set. The 390 lines of dated rows
;;    this session took out of `:onboarding` are still on disk, in the
;;    generated indexes. A future session adding one back to a `:paths`
;;    list would undo the compaction while every gate stayed green.

(deftest state-md-is-an-onboarding-member-test
  (testing "the continuity register is read cold, so it is budgeted and ratcheted like everything else read cold"
    (is (contains? (set (get-in (read-reading-sets) [:onboarding :paths])) ".agents/state.md")
        ":onboarding no longer carries .agents/state.md -- the register a cold session is routed to must sit under the budget gate (ADR-0147)")))

(deftest no-generated-index-is-a-reading-set-member-test
  (testing "the dated per-record rows left :onboarding by generation; a :paths entry would walk them straight back in"
    (let [offenders (for [[set-name {:keys [paths]}] (read-reading-sets)
                          path paths
                          :when (str/ends-with? path "INDEX.md")]
                      (str set-name " -> " path))]
      (is (empty? (vec offenders))
          (str "reading set(s) cite a generated INDEX.md: " (vec offenders)
               " -- those files grow one line per session forever, which is exactly what "
               "ADR-0147 removed from :onboarding. Cite the README's convention instead.")))))
