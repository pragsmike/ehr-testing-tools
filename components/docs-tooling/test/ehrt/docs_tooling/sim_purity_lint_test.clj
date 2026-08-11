(ns ehrt.docs-tooling.sim-purity-lint-test
  "The mutable-state census `docs/dev/simulator-architecture.md` section 3
  states is a checkable claim, not prose: zero `atom`/`ref`/`agent`/
  `volatile!`/`set-validator!` FORMS anywhere in the seven sim-family
  bricks' own `src` (`sim-model`, `sim-trajectory`, `sim-engine`,
  `sim-emit-hl7`, `sim-emit-fhir`, `sim-check`, `sim`), with exactly two
  named, disclosed exceptions. A future session adding a third exception
  (or reintroducing a fourth mutable-state primitive anywhere else in
  the simulation path) fails here instead of the architecture doc's own
  census silently going stale.

  Reader-based, not regex (the same `ehrt.cli.cli-parse-guard-lint-test`
  discipline this test's own walker structure mirrors): each file's
  top-level forms are parsed with the Clojure reader and walked for a
  `(atom ...)`/`(ref ...)`/`(agent ...)`/`(volatile! ...)`/
  `(set-validator! ...)` call -- never a regex over raw text, so a
  docstring or comment MENTIONING any of these five names (this file's
  own docstring does, repeatedly) is never even part of the read
  s-expression tree, hence never a false positive.

  Allowlisted BY NAMESPACE (the `ns` form actually declared in the
  file, not its path -- a file move can't silently exempt itself),
  exactly the two named in `docs/dev/simulator-architecture.md` section
  3, each with the SAME reason stated there:

  - `ehrt.sim-trajectory.census` -- `walk-one`'s own `fetched` atom
    (census.clj ~407), a census-run's own probe-fetch memoization
    bookkeeping (`ehrt sim census`), never read or written by
    `decide`/`evolve`/`run`/`replay`.
  - `ehrt.sim.version` -- `git-sha`'s own `.git/HEAD` read
    (version.clj ~19-37): not actually a mutable-state primitive this
    lint's own five forms would ever catch (grep finds none there
    today), listed here for parity with the architecture doc's own
    two-exception statement, and so a future atom/ref/agent/volatile
    ADDED to this namespace does not silently need a THIRD allowlist
    entry invented on the spot -- the exception was already named for
    this file, for this reason, by the doc this lint enforces."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private sim-family-brick-names
  "The seven bricks `docs/dev/simulator-architecture.md` section 1
  enumerates, by directory name under `components/` -- the exact set
  `ehrt.docs-tooling.project-classpath-test`'s own sim-brick enumeration
  already uses."
  ["sim-model" "sim-trajectory" "sim-engine" "sim-emit-hl7" "sim-emit-fhir" "sim-check" "sim"])

(def ^:private allowlisted-namespaces
  "See this namespace's own docstring for the reason each is here --
  restated in `docs/dev/simulator-architecture.md` section 3, not
  re-derived independently by this file."
  #{"ehrt.sim-trajectory.census" "ehrt.sim.version"})

(def ^:private forbidden-call-heads
  "The five mutable-state-introducing primitives the architecture doc's
  own census names: `atom`/`ref`/`agent`/`volatile!`/`set-validator!`,
  as bare `clojure.core` symbols (none of the seven bricks alias or
  qualify them differently, confirmed by the same grep this test's own
  census re-derives)."
  '#{atom ref agent volatile! set-validator!})

(defn- clj-files [^java.io.File root]
  (->> (file-seq root)
       (filter #(.isFile ^java.io.File %))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))
       (map #(.getPath ^java.io.File %))))

(defn- scan-sources []
  (mapcat (fn [brick]
            (let [src (io/file "components" brick "src")]
              (if (.exists src) (clj-files src) [])))
          sim-family-brick-names))

(defn- read-all-forms
  "Every top-level form in the file at `path`, via the Clojure reader
  (`*read-eval*` false) -- never a regex over raw text (the
  `ehrt.cli.cli-parse-guard-lint-test` discipline)."
  [path]
  (with-open [rdr (java.io.PushbackReader. (io/reader path))]
    (binding [*read-eval* false]
      (loop [forms []]
        (let [f (read {:eof ::eof} rdr)]
          (if (= f ::eof)
            forms
            (recur (conj forms f))))))))

(defn- ns-form? [form]
  (and (seq? form) (= 'ns (first form))))

(defn- form-namespace [forms]
  (some #(when (ns-form? %) (str (second %))) forms))

(defn- contains-forbidden-call?
  "Walks `node` (a parsed s-expression, or any nested vector/map/set/
  seq within it) for any `forbidden-call-heads` invocation, anywhere --
  unlike the parse-guard lint's own `try`-ancestry tracking, purity has
  no guard that makes an occurrence acceptable: this walker asks only
  whether the form is present at all."
  [node]
  (cond
    (and (seq? node) (seq node))
    (or (contains? forbidden-call-heads (first node))
        (some contains-forbidden-call? node))

    (or (vector? node) (set? node))
    (some contains-forbidden-call? node)

    (map? node)
    (some contains-forbidden-call? (mapcat identity node))

    :else false))

(defn- violating-paths []
  (->> (scan-sources)
       (remove (fn [path] (contains? allowlisted-namespaces (form-namespace (read-all-forms path)))))
       (filter (fn [path] (some contains-forbidden-call? (read-all-forms path))))))

(deftest no-mutable-state-primitives-outside-the-two-named-exceptions-test
  (let [violators (violating-paths)]
    (is (empty? violators)
        (str "The following sim-family src files call atom/ref/agent/volatile!/"
             "set-validator! outside the two allowlisted namespaces "
             "(docs/dev/simulator-architecture.md section 3, ADR-0108): "
             (pr-str violators)))))

(deftest allowlisted-namespaces-are-exactly-the-disclosed-two-test
  (is (= #{"ehrt.sim-trajectory.census" "ehrt.sim.version"} allowlisted-namespaces)))

(deftest the-two-allowlisted-files-are-actually-in-scan-scope-test
  (let [namespaces (into #{} (map (comp form-namespace read-all-forms)) (scan-sources))]
    (is (contains? namespaces "ehrt.sim-trajectory.census"))
    (is (contains? namespaces "ehrt.sim.version"))))

(deftest forbidden-pattern-detection-is-actually-caught-test
  (is (contains-forbidden-call? (read-string "(defn- f [] (atom {}))")))
  (is (contains-forbidden-call? (read-string "(defn- f [] (ref {}))")))
  (is (contains-forbidden-call? (read-string "(defn- f [] (agent nil))")))
  (is (contains-forbidden-call? (read-string "(defn- f [x] (volatile! x))")))
  (is (contains-forbidden-call? (read-string "(defn- f [a v] (set-validator! a v))")))
  (is (contains-forbidden-call? (read-string "(defn- f [] (let [x {:y (atom 1)}] x))"))
      "a nested occurrence inside a map literal value is still caught")
  (is (not (contains-forbidden-call? (read-string "(defn- f \"mentions atom, ref, agent, volatile!, set-validator! in prose\" [] (+ 1 1))")))
      "a docstring MENTIONING any of the five names is never a false positive -- it is never part of the read s-expression tree at all")
  (is (not (contains-forbidden-call? (read-string "(defn- f [] (swap! the-atom inc))")))
      "swap!/reset! operate on an EXISTING atom passed in -- not one of the five introducing forms this lint polices"))

;; ---- Minimal-reproduction witness pair (cluster A's own method,
;; `ehrt.docs-tooling.project-classpath-test`'s
;; `violation-predicate-reproduces-the-2088763-incident-test`): a
;; structural reduction of the temporary plant this session's own build
;; used to prove this lint red before the fix, kept as a permanent
;; regression proof independent of git history. ----

(def ^:private pre-fix-example
  "(defn- some-helper [seed]
     (let [cache (atom {})]
       (get @cache seed)))")

(def ^:private post-fix-example
  "(defn- some-helper [seed]
     (get {} seed))")

(deftest violation-predicate-reproduces-the-planted-atom-shape-test
  (is (contains-forbidden-call? (read-string pre-fix-example)))
  (is (not (contains-forbidden-call? (read-string post-fix-example)))))
