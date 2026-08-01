(ns ehrt.docs-tooling.notes-prompts-frozen-test
  "Item 1 (migration session 2, 2026-08-02): notes/prompts/ is ratified
  frozen in place (`.agents/plans/2026-08-01-migration-report.md`
  'RULED 2026-08-01', reading 2 -- the forward pointer already landed
  in `notes/prompts/README.md` IS the whole migration; the 29 dated
  session-prompt files never physically move, `git mv` or otherwise).
  Nothing enforced that 'frozen' stays true until this test: a future
  session could add, remove, or rename a file here and nothing would
  fail. Freshness-gate pattern (AR-1(a)) -- pins the literal filename
  set, since there is only one tree here to freeze, not two to diff
  (contrast `ehrt.docs-tooling.skill-mirror-currency-test`'s
  presence/absence-across-two-trees shape).

  README.md is deliberately NOT part of the pinned set below -- its
  wording may still legitimately change (a typo fix, a dated addendum
  to its own forward-pointer text) without that being drift. What must
  never change is the SET of dated prompt filenames: each one is a
  session's own historical record, and the directory's entire point,
  post-ruling, is that none of them are ever added to, removed, or
  renamed after the fact. A separate assertion below only checks that
  README.md still exists at all (the tombstone/pointer itself must
  survive, even though this test doesn't pin its bytes)."
  (:require [clojure.test :refer [deftest is]]
            [clojure.set :as set]
            [clojure.java.io :as io]))

(def ^:private frozen-prompt-files
  "The exact 29 dated session-prompt filenames frozen in place as of
  2026-08-01 (agent-ux capture session, the last prompt to land here
  before item 1 froze the directory). Add nothing here going forward --
  every session prompt from 2026-08-01 onward archives to
  `.agents/prompts/` instead (AGENTS.md's `.agents/` routing section)."
  #{"2026-07-28-ehr-testing-bootstrap-sim-landing.md"
    "2026-07-28-ehr-testing-carve-loss-recovery.md"
    "2026-07-28-ehr-testing-ci-red-executable-bits.md"
    "2026-07-28-ehr-testing-discipline-parity.md"
    "2026-07-28-ehr-testing-h2-closeout-sweep.md"
    "2026-07-28-ehr-testing-tools-landing.md"
    "2026-07-29-ehr-testing-development-resumption.md"
    "2026-07-29-ehr-testing-exp-d3-nist-validator.md"
    "2026-07-29-ehr-testing-judge-engine-extraction.md"
    "2026-07-29-ehr-testing-sim-sibling-errata-sweep.md"
    "2026-07-29-ehr-testing-storefront-polish.md"
    "2026-07-29-ehr-testing-wsl-clone-igamt-hygiene.md"
    "2026-07-30-ehr-testing-cli-trial-ux.md"
    "2026-07-30-ehr-testing-cold-start-ux.md"
    "2026-07-30-ehr-testing-corpus-player.md"
    "2026-07-30-ehr-testing-doctor-rendering.md"
    "2026-07-30-ehr-testing-judge-v2-nist-followthrough.md"
    "2026-07-30-ehr-testing-judge-v2-nist-landing.md"
    "2026-07-30-ehr-testing-output-ux-overhaul.md"
    "2026-07-31-ehr-testing-gate-hardening.md"
    "2026-07-31-ehr-testing-review-catchup-batch.md"
    "2026-07-31-ehr-testing-ruled-p2-batch.md"
    "2026-07-31-ehr-testing-split-stage1-docs-tooling.md"
    "2026-07-31-ehr-testing-split-stage2-corpus-io.md"
    "2026-07-31-ehr-testing-split-stage3-corpus.md"
    "2026-08-01-ehr-testing-retire-sim-cli.md"
    "2026-08-01-ehr-testing-sim-kernel-result.md"
    "2026-08-01-ehr-testing-storefront-and-ruled-literals.md"})

(defn- real-prompt-files
  "Every regular file in notes/prompts/ except README.md, by filename --
  the set this test pins against."
  []
  (->> (.listFiles (io/file "notes/prompts"))
       (filter #(.isFile %))
       (map #(.getName %))
       (remove #{"README.md"})
       set))

(deftest notes-prompts-file-set-is-pinned-test
  (let [real (real-prompt-files)
        added (set/difference real frozen-prompt-files)
        removed (set/difference frozen-prompt-files real)]
    (is (empty? added)
        (str "notes/prompts/ carries a file outside the frozen set (added or renamed into it): " added))
    (is (empty? removed)
        (str "notes/prompts/ is missing a file the frozen set expects (removed or renamed away): " removed))))

(deftest readme-tombstone-still-exists-test
  (is (.exists (io/file "notes/prompts/README.md"))
      "notes/prompts/README.md (the tombstone/forward-pointer) must still exist, even though this test does not pin its wording"))
