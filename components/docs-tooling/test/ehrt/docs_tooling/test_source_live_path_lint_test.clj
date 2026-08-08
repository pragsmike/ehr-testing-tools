(ns ehrt.docs-tooling.test-source-live-path-lint-test
  "Lint family (AR-LF-3(ii), D2-6, `.agents/plans/2026-08-07-repo-
  review-findings.md`): 'tests build their own directories, standing'
  (AR-BB2-R, player arc) -- the one known violation
  (`merge-config-file-suggests-a-same-stem-sibling-file` reading the
  live, mutable `config/busy-weekday.md` to answer 'does a same-stem
  sibling file exist') was fixed at its origin (ADR-0067) and hardened
  again this arc (ADR-0076's atomic-tempdir fix, `run_test.clj`'s
  `merge-config-file-suggests-a-same-stem-sibling-file`, which now
  builds its own temp dir via `temp-dir-path*`). Nothing repo-wide
  catches a NEW test reintroducing the same shape.

  Design note, found live against this repo's own tree (first-draft
  discipline: run the lint against the live tree BEFORE landing,
  disclosed per this session's own fence): a purely syntactic
  'any `.listFiles`/`.list(` call outside docs-tooling' pattern was
  tried first and false-positived on five files (`run_test.clj` itself
  -- the FIXED test, listing its own `temp-dir-path*` for a debug
  message; `display_test.clj` listing the allowlisted `test-fixtures`
  root; three `corpus`/`corpus-io` tests listing their own `out-dir`
  scratch directories) -- every hit was a test listing a directory IT
  built or an allowlisted fixture root, not the busy-weekday hazard.
  The pattern was wrong, not the files: this version narrows to the
  hazard's own actual shape -- a `.listFiles`/`.list` call whose
  argument is a LITERAL STRING PATH (`(io/file \"...\")` or a bare
  string), not a dynamically-bound symbol. A symbol (`out-dir`,
  `fixture-dir`, `dir-file`) is, in every live case, either a test-
  built temp dir or an allowlisted fixture path threaded in from a
  `def`; a literal string naming a live repo path is exactly the
  busy-weekday shape (a hardcoded path into the checked-out tree,
  whose directory CONTENTS the test does not control and can vary by
  environment).

  Allowlist is BY NAMESPACE, same mechanism as
  `ehrt.docs-tooling.io-vocabulary-lint-test`'s own move: every
  `ehrt.docs-tooling.*` test namespace is a deliberate, repo-wide
  SCANNER whose entire job is to list live, tracked, committed
  directories by literal path (`notice-verbatim-test`,
  `invocation-lint-test`, `stale-path-test`, this file's own
  siblings) -- dimension 5/7's own sanctioned pattern, not the
  busy-weekday hazard. A literal path under `test-fixtures`, `config/
  synthea`, or `resources` is allowlisted everywhere, docs-tooling or
  not -- those are the tracked-fixture carve-out this repo's own
  conventions already sanction."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private allowlisted-namespace-prefix "ehrt.docs-tooling.")

(def ^:private allowlisted-path-prefixes
  ["test-fixtures" "config/synthea" "resources"])

(def ^:private literal-listing-pattern
  "Matches `.listFiles`/`.list` called directly against a literal
  string path, either wrapped in `(io/file \"...\")` or bare -- the
  string itself is capture group 1. An open-paren immediately before
  the dot excludes docstring/comment mentions, same technique as
  `ehrt.docs-tooling.io-vocabulary-lint-test`'s own `forbidden-
  patterns`."
  #"\(\.list(?:Files)?\s+(?:\(io/file\s+)?\"([^\"]+)\"")

(defn- clj-test-files [^java.io.File root]
  (->> (file-seq root)
       (filter #(.isFile ^java.io.File %))
       (filter #(str/ends-with? (.getName ^java.io.File %) "_test.clj"))
       (map #(.getPath ^java.io.File %))))

(defn- test-roots [^String parent-dir]
  (->> (.listFiles (io/file parent-dir))
       (filter #(.isDirectory ^java.io.File %))
       (map #(io/file % "test"))
       (filter #(.exists ^java.io.File %))))

(defn- scan-test-sources []
  (mapcat clj-test-files (concat (test-roots "components") (test-roots "bases"))))

(defn- file-namespace [^String content]
  (second (re-find #"\(ns\s+([\w.-]+)" content)))

(defn- allowlisted-namespace? [ns-name]
  (and ns-name (str/starts-with? ns-name allowlisted-namespace-prefix)))

(defn- allowlisted-path? [path]
  (some #(str/starts-with? path %) allowlisted-path-prefixes))

(defn- live-path-hits
  "Every literal path `content` lists via `.listFiles`/`.list` that
  isn't under an allowlisted root."
  [content]
  (->> (re-seq literal-listing-pattern content)
       (map second)
       (remove allowlisted-path?)))

(deftest no-literal-live-path-directory-listings-outside-allowlist-test
  (doseq [path (scan-test-sources)]
    (let [content (slurp path)
          ns-name (file-namespace content)]
      (when-not (allowlisted-namespace? ns-name)
        (let [hits (live-path-hits content)]
          (is (empty? hits)
              (str path " (ns " ns-name ") lists a literal, live repo path directly "
                   "(" (vec hits) ") -- build a temp dir instead (AR-BB2-R, lint family "
                   "AR-LF-3(ii), D2-6), point at an allowlisted root ("
                   (str/join ", " allowlisted-path-prefixes) "), or if this is a genuine "
                   "repo-wide scanner, its home is an ehrt.docs-tooling.* namespace")))))))

;; -- mechanism-sanity: prove the extraction/matching functions actually catch what they claim to --

(deftest literal-listing-pattern-is-actually-caught-test
  (is (= ["config"] (map second (re-seq literal-listing-pattern "(.listFiles (io/file \"config\"))"))))
  (is (= ["config/busy-weekday.md"] (map second (re-seq literal-listing-pattern "(.list \"config/busy-weekday.md\")"))))
  (is (empty? (re-seq literal-listing-pattern "(.listFiles (io/file out-dir))"))
      "a symbol argument (not a literal string) must never match -- it's a test-built or threaded-in path")
  (is (empty? (re-seq literal-listing-pattern "a nil `.listFiles` here silently"))
      "a docstring/comment MENTIONING .listFiles is never a false positive -- no leading open-paren"))

(deftest allowlisted-path-check-is-actually-caught-test
  (is (allowlisted-path? "test-fixtures/v2/adt-a01.hl7"))
  (is (allowlisted-path? "config/synthea/synthea.properties"))
  (is (allowlisted-path? "resources/whatever"))
  (is (not (allowlisted-path? "config/busy-weekday.md"))
      "config/ outside config/synthea must never be silently allowlisted -- this IS the busy-weekday path"))

(deftest allowlisted-namespace-check-is-actually-caught-test
  (is (allowlisted-namespace? "ehrt.docs-tooling.notice-verbatim-test"))
  (is (not (allowlisted-namespace? "ehrt.sim.run-test"))
      "a non-docs-tooling namespace must never be silently allowlisted"))

(deftest live-path-hits-is-actually-caught-test
  (is (= ["config/busy-weekday.md"]
         (live-path-hits "(let [f (.listFiles (io/file \"config/busy-weekday.md\"))] f)")))
  (is (empty? (live-path-hits "(let [f (.listFiles (io/file out-dir))] f)"))))
