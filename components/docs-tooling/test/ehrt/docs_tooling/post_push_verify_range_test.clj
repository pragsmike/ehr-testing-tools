(ns ehrt.docs-tooling.post-push-verify-range-test
  "Repo review 3, D1-6: `bin/post-push-verify`'s default range
  derivation, against its own documented contract.

  The script's header promised the `<base-sha>` default was \"the sha
  that was origin's tip immediately before your push\"; its code used
  `${tip_sha}^1` -- the tip's own FIRST PARENT, i.e. always exactly one
  commit, however many the push actually carried. Three live pushes
  this arc spanned 4, 3, and 1 commits; in the first two, every message
  but the tip's went unchecked while the script reported OK. The
  population error is the same one the review's own amendment names:
  the stated population is \"the pushed range,\" but the enumeration ran
  off the commit graph, which cannot know what was pushed.

  This is the script-level gate for the fix. It builds a throwaway repo
  with a bare `file://`-style origin (a bare repo reached by path is a
  real origin -- `git push` fast-forwards the remote-tracking ref
  exactly as it does against GitHub, which is the mechanism the fix
  reads), pushes A, then commits B, C, D locally with C's message
  carrying a deliberate non-ASCII byte, pushes B..D, and runs the
  script with NO base argument.

  Pre-fix that run reports \"every commit message in range is pure
  ASCII\" and exits 0, because its range is D..D. Post-fix the range is
  A..D and the em dash in C is caught.

  RUNTIME DEPENDENCY: shells out to `git`, which every path into this
  repo already requires (the pre-commit/pre-push hooks, `bin/preflight`,
  `bin/regression-oracle`). The script under test resolves its own repo
  root from `${BASH_SOURCE[0]}/..`, so the fixture gets its own copy at
  `<fixture>/bin/post-push-verify` -- the real bytes of the real script,
  rooted at the throwaway repo instead of this one."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]))

(def ^:private script-under-test "bin/post-push-verify")

(defn- git!
  "Runs git in `dir`, throwing on non-zero exit -- a fixture step that
  fails silently would leave the assertions below testing nothing."
  [dir & args]
  (let [{:keys [exit out err]} (apply shell/sh "git" (concat args [:dir dir]))]
    (when-not (zero? exit)
      (throw (ex-info (str "fixture git " (pr-str args) " failed: " err)
                      {:exit exit :out out :err err})))
    (str/trim out)))

(defn- delete-tree!
  [^java.io.File f]
  (when (.isDirectory f)
    (doseq [child (.listFiles f)]
      (delete-tree! child)))
  (.delete f))

(defn- build-fixture!
  "A bare origin plus a work clone whose history is A (pushed), then
  B, C, D (pushed as one range). C's message carries an em dash.
  Returns {:work :pre-push-tip :tip}."
  [^java.io.File root]
  (let [origin (io/file root "origin.git")
        work (io/file root "work")]
    (git! root "init" "--bare" "-q" "--initial-branch=main" (.getPath origin))
    (git! root "init" "-q" "--initial-branch=main" (.getPath work))
    (git! work "config" "user.email" "fixture@example.invalid")
    (git! work "config" "user.name" "fixture")
    (git! work "config" "commit.gpgsign" "false")
    (git! work "remote" "add" "origin" (.getAbsolutePath origin))
    (spit (io/file work "f") "a\n")
    (git! work "add" "f")
    (git! work "commit" "-q" "-m" "A")
    (git! work "push" "-q" "origin" "main")
    (let [pre-push-tip (git! work "rev-parse" "HEAD")]
      (spit (io/file work "f") "a\nb\n")
      (git! work "commit" "-qam" "B")
      (spit (io/file work "f") "a\nb\nc\n")
      (let [msg (io/file root "c-message.txt")]
        ;; The deliberate non-ASCII byte: U+2014 EM DASH, the exact
        ;; character AR-RL2-5's ASCII line exists to keep out of commit
        ;; messages. Written as an escape, and with the encoding pinned,
        ;; so this test source stays pure ASCII itself and the byte on
        ;; disk does not depend on the JVM's default charset.
        (spit msg (str "C carrying a non-ASCII byte " (char 0x2014) " an em dash\n")
              :encoding "UTF-8")
        (git! work "commit" "-qa" "-F" (.getAbsolutePath msg)))
      (spit (io/file work "f") "a\nb\nc\nd\n")
      (git! work "commit" "-qam" "D")
      (git! work "push" "-q" "origin" "main")
      {:work work
       :pre-push-tip pre-push-tip
       :tip (git! work "rev-parse" "HEAD")})))

(defn- install-script!
  "Copies the real script into the fixture's own bin/, so its
  `${BASH_SOURCE[0]}/..` repo-root resolution lands on the fixture --
  and `bin/ascii-scan` beside it, because ADR-0157 extracted check 2's
  byte scan into that script when `.githooks/commit-msg` became its
  second caller. post-push-verify is fail-closed on a scanner it cannot
  run (an unmeasurable check is not a passing one, ADR-0155), so a
  fixture missing it never reaches check 2's real verdict."
  [^java.io.File work]
  (let [dest-dir (io/file work "bin")
        dest (io/file dest-dir "post-push-verify")]
    (.mkdirs dest-dir)
    (doseq [script [script-under-test "bin/ascii-scan"]]
      (let [copied (io/file dest-dir (.getName (io/file script)))]
        (io/copy (io/file script) copied)
        (.setExecutable copied true)))
    dest))

(deftest default-range-covers-every-pushed-commit-test
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "post-push-verify-range" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (let [{:keys [work pre-push-tip tip]} (build-fixture! root)
            _ (install-script! work)
            {:keys [exit out err]} (shell/sh "bin/post-push-verify" :dir work)
            combined (str out err)]
        (testing "the fixture really did push more than one commit"
          (is (= 3 (count (str/split-lines (git! work "log" "--format=%H"
                                                (str pre-push-tip ".." tip)))))))
        (testing "a non-ASCII message anywhere in the pushed range fails the check"
          (is (not (zero? exit))
              (str "expected non-zero exit; the pushed range carries a non-ASCII "
                   "commit message. Output was:\n" combined))
          (is (str/includes? combined "non-ASCII") combined))
        (testing "the derived range starts at origin's pre-push tip, not the tip's parent"
          (is (str/includes? combined (subs pre-push-tip 0 8))
              (str "expected the reported range to start at " (subs pre-push-tip 0 8)
                   " (origin's pre-push tip). Output was:\n" combined))))
      (finally
        (delete-tree! root)))))
