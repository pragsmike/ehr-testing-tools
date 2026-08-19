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

  WIDENED 2026-08-19 by ADR-0157, closing review-4 row D4-1. The lint
  enforcing `R-io-result-or-loud` had a population NARROWER than the
  rule it enforces: `.mkdirs` and `.delete` were not in the forbidden
  set, and the tree held 13 `.mkdirs` sites discarding the boolean --
  one inside `ehrt.kernel` itself -- plus 2 `.delete`. Both booleans are
  ambiguous the same way (`false` means either 'already so' or
  'refused'), which is exactly why every site discarded them.
  `ehrt.kernel.io` now provides `mkdirs!`, `delete!` and the declared
  cleanup exception `delete-quietly!`, and the two patterns are gated
  here. This is the third instance in review 4 of a gate read as
  covering a class wider than its own population.

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
  boundary.

  `\\bdelete\\b` does not match inside `.deleteOnExit` for the same
  reason `\\blist\\b` does not match inside `.listFiles`: e->O is not a
  word boundary. `.mkdirs` and `.mkdir` are distinct patterns and only
  `.mkdirs` is forbidden here -- ADR-0157's own census found zero
  `.mkdir`, `.createNewFile` and `.setExecutable` call sites in
  production `src`, and widening to patterns with no population would
  be a gate written from imagination rather than from the tree."
  [#"\(\.listFiles\b" #"\(\.list\b" #"\(\.renameTo\b"
   #"\(\.mkdirs\b" #"\(\.delete\b"])

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

(deftest no-bare-guarded-io-call-outside-the-kernel-io-allowlist-test
  (let [sources (scan-sources)]
    ;; rulings.md#R-empty-population-is-red -- this gate is a `doseq`
    ;; over a scan, so a scan that found nothing was a pass that proved
    ;; nothing, silently, forever. Added by ADR-0157 with the widening:
    ;; the rule already bound tests when this lint was written and this
    ;; one did not carry it.
    (is (pos? (count sources))
        (str "sanity: the source scan must find production .clj files under components/*/src "
             "and bases/*/src -- an empty population makes every assertion below vacuous "
             "(rulings.md#R-empty-population-is-red)"))
    (doseq [path sources]
      (let [content (slurp path)
            ns-name (file-namespace content)]
        (when-not (contains? allowlisted-namespaces ns-name)
          (is (empty? (forbidden-hits content))
              (str path " (ns " ns-name ") calls .listFiles/.list/.renameTo/.mkdirs/.delete "
                   "directly -- route through ehrt.kernel.io instead (result or loud, ADR-0078, "
                   "AR-RL-4; .mkdirs/.delete added by ADR-0157, review-4 D4-1)")))))))

(deftest forbidden-pattern-detection-is-actually-caught-test
  (is (re-find (nth forbidden-patterns 0) "(let [x (.listFiles f)] x)"))
  (is (re-find (nth forbidden-patterns 1) "(.list f)"))
  (is (not (re-find (nth forbidden-patterns 1) "(.listFiles f)"))
      "a bare .list pattern must never false-positive on .listFiles")
  (is (re-find (nth forbidden-patterns 2) "(.renameTo tmp dest)"))
  (is (nil? (re-find (nth forbidden-patterns 0) "a nil `.listFiles` here silently"))
      "a docstring/comment MENTIONING .listFiles is never a false positive -- no leading open-paren")
  (is (re-find (nth forbidden-patterns 3) "(.mkdirs (io/file out-dir))"))
  (is (re-find (nth forbidden-patterns 4) "(.delete tmp)"))
  (is (nil? (re-find (nth forbidden-patterns 4) "(.deleteOnExit f)"))
      "a bare .delete pattern must never false-positive on .deleteOnExit")
  (is (nil? (re-find (nth forbidden-patterns 3) "the helper is `.mkdirs` plus a check"))
      "prose naming .mkdirs is never a false positive -- no leading open-paren"))

(deftest allowlisted-namespaces-are-exactly-the-disclosed-two-test
  (is (= #{"ehrt.kernel.io" "ehrt.sim.run"} allowlisted-namespaces)))
