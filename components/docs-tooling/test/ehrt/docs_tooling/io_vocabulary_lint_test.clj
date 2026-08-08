(ns ehrt.docs-tooling.io-vocabulary-lint-test
  "Result or loud (ADR-0078, AR-RL-4): a recurrence gate for the D4-1/
  D3-4/D8-2/D8-3 root cause fix session 1 closed -- a bare
  java.io.File `.listFiles`/`.list`/`.renameTo` call can't distinguish
  an I/O failure from a genuinely empty result (nil vs. an empty
  array) or a silently-refused rename, the exact conflation this
  session's sweep replaced across every production call site the
  repo-review register named
  (`.agents/plans/2026-08-07-repo-review-findings.md`, D4-1/D3-4).
  `ehrt.kernel.io` is the shared, guarded replacement (`list-files`/
  `existing-dir-nonempty?`/`rename!`); a future call site
  reintroducing the bare idiom fails here instead of waiting for
  another review to find it by hand.

  Allowlist is BY NAMESPACE (the `ns` form actually declared in the
  file, not its path -- a file move can't silently exempt itself):
  `ehrt.kernel.io` is the shared helper itself, whose whole job IS
  calling these methods directly. `ehrt.sim.run` is grandfathered --
  `similar-sibling-config`'s own ADR-0076 fix already implements the
  correct retry-once-then-name-the-failure idiom locally, predating
  this helper; migrating it onto the shared helper is disclosed,
  deferred cargo (out of this session's own fence, AR-RL-6), not a
  violation of this gate."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private allowlisted-namespaces
  #{"ehrt.kernel.io" "ehrt.sim.run"})

(def ^:private forbidden-patterns
  "Clojure Java-interop call syntax specifically -- an open-paren
  immediately before the dot -- so a docstring/comment MENTIONING
  `.listFiles` (several do, describing this very fix) is never a false
  positive; only an actual call site is. \\blist\\b (not \\blistFiles\\b)
  distinguishes a bare `.list` call from `.listFiles` -- word-boundary
  regex won't match \\blist\\b inside \"listFiles\" since t->F has no
  boundary."
  [#"\(\.listFiles\b" #"\(\.list\b" #"\(\.renameTo\b"])

(defn- clj-files [^java.io.File root]
  (->> (file-seq root)
       (filter #(.isFile ^java.io.File %))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))
       (map #(.getPath ^java.io.File %))))

(defn- brick-src-roots [^String parent-dir]
  (->> (.listFiles (io/file parent-dir))
       (filter #(.isDirectory ^java.io.File %))
       (map #(io/file % "src"))
       (filter #(.exists ^java.io.File %))))

(defn- scan-sources []
  (mapcat clj-files (concat (brick-src-roots "components") (brick-src-roots "bases"))))

(defn- file-namespace [^String content]
  (second (re-find #"\(ns\s+([\w.-]+)" content)))

(defn- forbidden-hits [^String content]
  (filterv some? (map #(re-find % content) forbidden-patterns)))

(deftest no-bare-listfiles-list-renameto-outside-the-kernel-io-allowlist-test
  (doseq [path (scan-sources)]
    (let [content (slurp path)
          ns-name (file-namespace content)]
      (when-not (contains? allowlisted-namespaces ns-name)
        (is (empty? (forbidden-hits content))
            (str path " (ns " ns-name ") calls .listFiles/.list/.renameTo directly -- "
                 "route through ehrt.kernel.io instead (result or loud, ADR-0078, AR-RL-4)"))))))

(deftest forbidden-pattern-detection-is-actually-caught-test
  (is (re-find (nth forbidden-patterns 0) "(let [x (.listFiles f)] x)"))
  (is (re-find (nth forbidden-patterns 1) "(.list f)"))
  (is (not (re-find (nth forbidden-patterns 1) "(.listFiles f)"))
      "a bare .list pattern must never false-positive on .listFiles")
  (is (re-find (nth forbidden-patterns 2) "(.renameTo tmp dest)"))
  (is (nil? (re-find (nth forbidden-patterns 0) "a nil `.listFiles` here silently"))
      "a docstring/comment MENTIONING .listFiles is never a false positive -- no leading open-paren"))

(deftest allowlisted-namespaces-are-exactly-the-disclosed-two-test
  (is (= #{"ehrt.kernel.io" "ehrt.sim.run"} allowlisted-namespaces)))
