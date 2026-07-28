(ns ehrt.ehr-cli.executable-bits-test
  "Guard: every tracked bin/ and .githooks/ script, and every tracked file
  whose content starts with a shebang, must carry index mode 100755.

  Clones inherit the INDEX's mode, not the working tree's; under
  core.fileMode=false (this workspace's own setting) the two can
  disagree silently, so local execution success proves nothing about
  what CI receives -- this test checks the index, the only mode that
  ships. Same bug class recurred three times before this test existed:
  bin/check-palgebra-drift (fixed 406482e, see
  notes/tools/prompts/2026-07-27-palgebra-drift-check.md, which already
  names `git update-index --chmod=+x` as the fix -- `core.fileMode=false`
  makes a plain `git add` ignore the permission change entirely) and
  then bin/ehr + bin/quickstart-demo again during the H2 carve, which
  reached CI unnoticed and failed run 30405350913 (2026-07-28, exit 126
  on `bin/ehr`). See notes/prompts/2026-07-28-ehr-testing-ci-red-executable-bits.md."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn- tracked-files []
  (let [{:keys [exit out err]} (shell/sh "git" "ls-files" "-s")]
    (when-not (zero? exit)
      (throw (ex-info "git ls-files -s failed -- can't verify executable bits" {:exit exit :err err})))
    (->> (str/split-lines out)
         (remove str/blank?)
         (map (fn [line]
                (let [[meta path] (str/split line #"\t" 2)
                      mode (first (str/split meta #"\s+"))]
                  {:mode mode :path path}))))))

(defn- shebang?
  "True if `path` exists on disk and its first two bytes are `#!`.
  Non-text files can't be read as chars; treated as not-a-shebang."
  [path]
  (let [f (io/file path)]
    (and (.isFile f)
         (try
           (with-open [r (io/reader f)]
             (let [buf (char-array 2)]
               (and (= 2 (.read r buf))
                    (= "#!" (String. buf)))))
           (catch Exception _ false)))))

(defn- should-be-executable? [{:keys [path]}]
  (or (str/starts-with? path "bin/")
      (str/starts-with? path ".githooks/")
      (shebang? path)))

(deftest tracked-scripts-are-executable-in-the-index-test
  (let [offenders (->> (tracked-files)
                        (filter should-be-executable?)
                        (remove #(= "100755" (:mode %))))]
    (is (empty? offenders)
        (str "Tracked script(s) below are not mode 100755 in the git index -- "
             "a fresh clone (what CI checks out) will see them as "
             "non-executable even if your own working tree runs them fine "
             "(core.fileMode=false hides the mismatch locally). Fix each with:\n"
             (str/join "\n" (map #(str "  git update-index --chmod=+x " (:path %)) offenders))))))
