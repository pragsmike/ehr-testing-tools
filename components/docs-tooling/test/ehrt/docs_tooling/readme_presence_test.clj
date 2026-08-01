(ns ehrt.docs-tooling.readme-presence-test
  "Items 4+11 (migration report §5.3, migration session 2, 2026-08-02):
  a per-directory README-presence gate over `.agents/` and `notes/`.
  Before this test, nothing enforced that a newly created subdirectory
  under either tree got an indexing README at all -- 11 directories
  (`.agents/skills/` itself plus its 10 skill subdirectories) had none
  until this session wrote them.

  Scope, decided here to match exactly what the migration report itself
  enumerated (not a blind recursive walk): the direct subdirectories of
  `.agents/`, PLUS the direct subdirectories of `.agents/skills/`
  specifically (one extra level, since that's where this workspace's
  actual per-skill directories live), PLUS the direct subdirectories of
  `notes/`. Does NOT recurse into a skill's own `references/`,
  `scripts/`, `templates/`, or `agents/` subdirectories -- those are
  content assets a skill owns, not registers in the discipline-surface
  sense `AGENTS.md` maps, and the migration report never asks for
  READMEs there.

  Exemption (ruling 6, migration report open question 6, `.agents/plans/2026-08-01-migration-report.md`
  'RULED 2026-08-01' item 6): `notes/sim/` and `notes/tools/` are frozen
  provenance -- byte-identical, never edited for new paths or
  namespaces (`AGENTS.md` 'Discipline surface, mapped'). Forcing a
  README onto a directory whose whole charter is 'never rewritten'
  would be in tension with that promise, so they're exempt, encoded as
  data below with the ruling cited, not silently skipped."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]))

(def ^:private exempt-directories
  "Frozen-provenance directories exempt from the README-presence gate
  (ruling 6, migration report open question 6, 2026-08-01) -- cited
  here as the ruling's own data, not an unexplained denylist."
  #{"notes/sim" "notes/tools"})

(defn- subdirectories
  "Direct subdirectory paths of `root`, as forward-slash relative-to-
  workspace-root strings."
  [root]
  (->> (.listFiles (io/file root))
       (filter #(.isDirectory %))
       (map (fn [f] (str root "/" (.getName f))))
       sort))

(defn- directories-requiring-a-readme []
  (let [agents-subdirs (subdirectories ".agents")
        skills-subdirs (subdirectories ".agents/skills")
        notes-subdirs (subdirectories "notes")]
    (->> (concat agents-subdirs skills-subdirs notes-subdirs)
         (remove exempt-directories)
         distinct
         sort)))

(deftest every-required-directory-has-a-readme-test
  (doseq [dir (directories-requiring-a-readme)]
    (is (.exists (io/file dir "README.md"))
        (str dir " has no README.md -- every .agents/ or .agents/skills/ "
             "subdirectory and every notes/ subdirectory needs one, except "
             "the frozen-provenance exemptions (ruling 6): " exempt-directories))))

(deftest exempt-directories-are-the-frozen-provenance-pair-test
  (is (= #{"notes/sim" "notes/tools"} exempt-directories)
      "the exemption list itself is ruling 6's own data -- pinned so a future edit to it is a visible, deliberate change, not silent scope creep"))
