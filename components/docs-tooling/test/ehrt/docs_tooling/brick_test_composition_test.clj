(ns ehrt.docs-tooling.brick-test-composition-test
  "W-1's THIRD shape: a brick whose tests never EXECUTE.

  `poly test` runs a brick's tests in the projects that COMPOSE it.
  The development project is not one of those -- a brick present only
  in root `deps.edn`'s `:dev`/`:test` aliases has its test namespaces
  on the REPL classpath and never runs them under `make test`. Arc 2b
  proved it rather than assuming it: a deliberately failing probe
  under `components/person-simulator/test` left `clojure -M:poly test
  project:development` at exit 0, and only adding `poly/person-
  simulator` to a real project's own `deps.edn` made the probe red
  (`.agents/session-records/2026-08-25-arc-2b-person-simulator-
  component.md`).

  W-1 itself (repo review 4, `notes/adr/0159-review-4-arc-close.md`) is
  the class: a gate that lands unexecuted is a gate that is not there.
  Its first two shapes were an integration tier `make test` skips and
  an invariant nothing called; this is the third, and it is the one no
  existing gate could see -- `ehrt.docs-tooling.project-classpath-test`
  asks the mirror-image question (does a composed brick's test tree
  require something the project does NOT compose?) and is blind to a
  brick composed into no project at all.

  So: every brick with a `test` directory carrying at least one `.clj`
  file is composed into at least one `projects/*/deps.edn`. The root
  `deps.edn`'s own aliases are DELIBERATELY not counted -- counting
  them is exactly the mistake this gate exists to make impossible.

  Same reader-based extraction as its sibling: each project's own
  `deps.edn` is read with `clojure.edn`, never grepped -- for the
  reason `ehrt.docs-tooling.project-classpath-test`'s own docstring
  states, that a regex over raw file text trips on a `poly/`-shaped
  token appearing only in a comment."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(defn- subdirs [parent]
  (let [f (io/file parent)]
    (if (.isDirectory f) (sort-by #(.getName %) (filter #(.isDirectory %) (.listFiles f))) [])))

(defn- has-clj? [dir]
  (boolean (some #(and (.isFile %) (str/ends-with? (.getName %) ".clj"))
                 (file-seq (io/file dir)))))

(defn- bricks-with-tests
  "Every `components/<name>` and `bases/<name>` whose own `test`
  directory carries at least one `.clj` file, as `<name>` strings. A
  brick with no test tree owes no composition -- there is nothing that
  could fail to execute."
  []
  (vec (for [parent ["components" "bases"]
             d (subdirs parent)
             :let [t (io/file d "test")]
             :when (and (.isDirectory t) (has-clj? t))]
         (.getName d))))

(defn- project-deps-files []
  (vec (for [d (subdirs "projects")
             :let [f (io/file d "deps.edn")]
             :when (.isFile f)]
         f)))

(defn- composed-bricks
  "brick name -> the set of project names composing it, read off each
  project's own `:deps` map: every `poly/<name>` key's `<name>`."
  []
  (reduce (fn [acc f]
            (let [project (.getName (.getParentFile f))
                  deps (:deps (edn/read-string (slurp f)))]
              (reduce (fn [a k]
                        (if (= "poly" (namespace k))
                          (update a (name k) (fnil conj #{}) project)
                          a))
                      acc (keys deps))))
          {} (project-deps-files)))

(deftest every-brick-test-path-is-composed-into-a-project-test
  (let [bricks (bricks-with-tests)
        composed (composed-bricks)]
    (testing "population is non-empty (rulings.md#R-empty-population-is-red)"
      (is (seq bricks) "no brick in this workspace has a test directory at all")
      (is (seq (project-deps-files)) "no projects/*/deps.edn was read")
      (is (seq composed) "no project composes any poly/* brick -- the parse found nothing"))
    (let [orphans (remove composed bricks)]
      (is (empty? orphans)
          (str (count orphans) " brick(s) of " (count bricks)
               " carry tests that NO project composes, so `poly test` never runs them"
               " -- add the brick to a projects/*/deps.edn, not to a root alias: "
               (vec orphans))))))
