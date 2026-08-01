(ns ehrt.docs-tooling.skill-mirror-currency-test
  "ADR-0024 (2026-08-01, migration session 1): `.claude/skills/` is a
  real-file mirror of `.agents/skills/`, carved out of the otherwise
  untracked `.claude/` for exactly this reason -- Claude Code discovers
  skills at `.claude/skills/<name>/SKILL.md`, never `.agents/skills/`,
  so the mirror is load-bearing, not cosmetic. `.agents/skills/` stays
  canonical (edit there); nothing enforced that the mirror actually
  stays in sync until this test. Same exact-token-both-directions shape
  `1c3d77c` hardened two other gates into: presence (every file under
  the canonical tree exists, byte-identical, under the mirror) and
  absence (the mirror carries no file the canonical tree doesn't also
  have) -- a one-sided check would pass a mirror that's stale in either
  direction (missing a new skill, or carrying a deleted one)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]))

(def ^:private canonical-root ".agents/skills")
(def ^:private mirror-root ".claude/skills")

(defn- relative-file-paths
  "Every regular file under `root`, as forward-slash relative paths
  from `root` itself -- comparable across the two trees regardless of
  OS path separator."
  [root]
  (let [base (io/file root)
        base-path (.toPath base)]
    (->> (file-seq base)
         (filter #(.isFile %))
         (map (fn [f] (str (.relativize base-path (.toPath f)))))
         (map #(.replace ^String % "\\" "/"))
         set)))

(deftest mirror-has-every-canonical-file-test
  (testing "presence: every canonical skill file exists, byte-identical, in the mirror"
    (doseq [rel (relative-file-paths canonical-root)]
      (let [canonical-file (io/file canonical-root rel)
            mirror-file (io/file mirror-root rel)]
        (is (.exists mirror-file)
            (str rel " exists under " canonical-root " but not under " mirror-root
                 " -- .claude/skills/ has drifted, re-sync from .agents/skills/"))
        (when (.exists mirror-file)
          (is (= (slurp canonical-file) (slurp mirror-file))
              (str rel " differs between " canonical-root " and " mirror-root
                 " -- .agents/skills/ is canonical, .claude/skills/ must match it exactly")))))))

(deftest mirror-has-no-orphaned-file-test
  (testing "absence: the mirror carries no file the canonical tree doesn't also have"
    (let [canonical (relative-file-paths canonical-root)]
      (doseq [rel (relative-file-paths mirror-root)]
        (is (contains? canonical rel)
            (str rel " exists under " mirror-root " but not under " canonical-root
                 " -- an orphaned mirror file (stale skill, manual edit made only in .claude/skills/)"))))))
