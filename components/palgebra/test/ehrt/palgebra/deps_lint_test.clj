(ns ehrt.palgebra.deps-lint-test
  "Exercises ehrt.palgebra.deps-lint against the real components/palgebra/
  tree (must pass -- it's this session's own dependency-direction claim,
  D9, renamed per H2 landing session ruling R16) and seeded fixtures
  (must fail -- proving the lint actually fires, not just vacuously
  passing). Since the gate-hardening session (2026-07-31, notes/ADRs.md
  ADR-0002 amendment) the rule is an allowlist, not a denylist of
  `ehrt.tools.*`/`ehrt.sim.*` -- the fixtures below cover both a
  historical forbidden name and an arbitrary future one, proving the
  allowlist forbids by default rather than by name."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehrt.palgebra.deps-lint :as deps-lint])
  (:import [java.io File]))

(deftest the-real-palgebra-tree-passes-deps-lint-test
  (let [{:keys [ok? violations]} (deps-lint/lint {:root "components/palgebra"})]
    (is ok? (str "violations: " violations))
    (is (= [] violations))))

;; ---- seeded violations: a palgebra namespace requiring a forbidden ehrt.* ----

(defn- temp-fixture-tree!
  "Writes one .clj file, with a `ns` form requiring `required-ns`, under
  a fresh temp directory -- proves the lint fires on exactly the
  violation it exists to catch, without touching the real
  components/palgebra/ tree."
  [required-ns]
  (let [dir (doto (File/createTempFile "palgebra-deps-lint-fixture" "")
              (.delete)
              (.mkdirs))
        f (io/file dir "bad_ns.clj")]
    (spit f (str "(ns ehrt.palgebra.bad-ns\n"
                 "  (:require [" required-ns " :as lint]))\n"))
    (.getAbsolutePath dir)))

(deftest lint-catches-a-palgebra-namespace-requiring-ehr-test
  (let [fixture-root (temp-fixture-tree! "ehrt.tools.lint")
        {:keys [ok? violations]} (deps-lint/lint {:root fixture-root})]
    (is (not ok?))
    (is (= 1 (count violations)))
    (is (= "ehrt.tools.lint" (:required (first violations))))))

(deftest lint-catches-an-arbitrary-future-component-outside-the-allowlist-test
  ;; ehrt.tools/ehrt.sim were the old denylist's own names; this fixture
  ;; uses a name that never existed, proving the allowlist forbids any
  ;; non-palgebra ehrt.* namespace by default -- a future rename or new
  ;; component needs no edit to this file to stay caught.
  (let [fixture-root (temp-fixture-tree! "ehrt.some-future-component.core")
        {:keys [ok? violations]} (deps-lint/lint {:root fixture-root})]
    (is (not ok?))
    (is (= 1 (count violations)))
    (is (= "ehrt.some-future-component.core" (:required (first violations))))))

(deftest lint-passes-on-an-empty-tree-test
  (let [dir (doto (File/createTempFile "palgebra-deps-lint-empty" "")
              (.delete)
              (.mkdirs))
        {:keys [ok? violations]} (deps-lint/lint {:root (.getAbsolutePath dir)})]
    (is ok?)
    (is (= [] violations))))
