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
            [clojure.edn :as edn]))

(def ^:private reading-sets-path ".agents/reading-sets.edn")

(defn- read-reading-sets []
  (edn/read-string (slurp reading-sets-path)))

(defn- missing-paths
  "The subset of `paths` that does not resolve to a real file, relative
  to the workspace root (this test always runs cwd-at-root, same as
  every other docs-tooling filesystem test in this suite)."
  [paths]
  (remove #(.isFile (io/file %)) paths))

(defn- line-count [path]
  (with-open [r (io/reader path)]
    (count (line-seq r))))

(defn- total-lines
  "Sum of real line counts across `paths`. Callers are expected to have
  already checked `missing-paths` is empty -- a nonexistent path here
  throws, deliberately: silently treating a ghost as zero lines would
  let an over-budget set masquerade as in-budget by citing a path that
  doesn't exist."
  [paths]
  (reduce + (map line-count paths)))

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

;; -- mechanism-sanity: prove the two checks above actually catch bad data --

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
