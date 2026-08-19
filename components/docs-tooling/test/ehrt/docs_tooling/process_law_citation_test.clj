(ns ehrt.docs-tooling.process-law-citation-test
  "ADR-0156, closing register rows D2-2 and D2-6 -- process laws cited
  where sessions actually read, and the `--amend` row that had no law
  either way.

  D2-2. `.agents/rulings.md` holds the standing rules; a session is not
  required to read it end to end, and in practice does not. The surface
  a build session IS routed through is
  `.agents/skills/build-session/SKILL.md`. Review 4 found
  `R-session-verifies-ci-via-gh` and `R-stop-only-on-two-defensible-
  readings` cited in NO skill, no bin script, no CI file and no test --
  `grep -rl` over `.agents/skills .claude/skills bin .githooks .github`
  came back empty for both. The first is load-bearing for the tag
  licence: it is the rule that decides whether a close tag is payable in
  session. It was applied correctly four times in that window, but by
  ADRs quoting it, not by any surface a session is routed through. This
  is `rulings.md#R-law-surface-propagation` applied to two rules that
  never got it.

  D2-6 / author ruling R4-Q1 (a). ADR-0153 disclosed one message-only
  `git commit --amend` of an unpushed commit. There was no row and no
  skill line either way -- the act was defensible and disclosed, which
  is the discipline working, and the next session had nothing to follow
  and would have had to re-reason it. Permitted narrowly, with a row.

  THE COMMITTED LIST, and why it is a list. Rows carry no `process` tag,
  so there is nothing to filter on; the population is named explicitly
  below and moves by editing this test. It holds NINE rows, not the six
  the plan estimated -- the plan counted only the four process rows
  added in review 4's own window, and `build-session` already cited
  three more (`R-tag-law`, `R-anchored-register-edits`,
  `R-oracle-script-contract`). Counted from the skill rather than from
  the plan's memory of it, which is the same discipline the rest of this
  review runs on."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def ^:private skill-copies
  ["/.agents/skills/build-session/SKILL.md"
   "/.claude/skills/build-session/SKILL.md"])

(def ^:private rulings-path ".agents/rulings.md")

(def ^:private process-laws
  "Every standing rule that binds what a build session DOES, as opposed
  to what the code must look like. Each must be citable from the surface
  a session reads at the moment it applies."
  ["R-full-suite-before-push"
   "R-red-pushed-with-green"
   "R-register-hygiene-at-close"
   "R-budget-stop"
   "R-anchored-register-edits"
   "R-tag-law"
   "R-oracle-script-contract"
   "R-session-verifies-ci-via-gh"
   "R-stop-only-on-two-defensible-readings"
   "R-amend-unpushed-message-only"])

(defn- skill-text [copy] (slurp (str "." copy)))

(deftest every-process-law-is-cited-where-a-session-reads-it-test
  (let [rulings (slurp rulings-path)]
    (testing "the population is real -- R-empty-population-is-red"
      (is (seq process-laws) "the committed list must not be empty"))
    (testing "every listed law is a real row in the register"
      (doseq [law process-laws]
        (is (str/includes? rulings (str "**" law "**"))
            (str "`" law "` is listed here as a process law but has no row in " rulings-path
                 " -- this test must never vouch for a citation of a rule that does not exist"))))
    (testing "rulings.md#R-law-surface-propagation, on the surface sessions actually read"
      (doseq [copy skill-copies
              law process-laws]
        (is (str/includes? (skill-text copy) law)
            (str "`" law "` is cited nowhere in `" copy "`. A standing rule that binds every "
                 "session but lives only as a row in a register the session is not required "
                 "to read end to end is a rule with no surface. Add it where the rule "
                 "APPLIES -- the tag/CI step, the STOP step, the staging-hygiene step -- not "
                 "as a list at the bottom."))))))

(deftest the-amend-row-exists-and-says-what-was-ruled-test
  (let [rulings (slurp rulings-path)
        row (first (filter #(str/includes? % "**R-amend-unpushed-message-only**")
                           (str/split-lines rulings)))]
    (testing "R4-Q1 (a): permitted narrowly, with a row"
      (is (some? row)
          (str rulings-path " must carry an `R-amend-unpushed-message-only` row. D2-6 found no "
               "law either way on `git commit --amend`; the ruling permits it only on an "
               "unpushed commit and only for the message."))
      (when row
        (is (str/includes? row "amend")
            "the row must name the act it governs")
        (is (str/includes? (str/lower-case row) "message")
            "the row must say the permission is MESSAGE-only -- that is the whole narrowing")))))
