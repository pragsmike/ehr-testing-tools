(ns ehrt.palgebra.deps-lint-test
  "Exercises ehrt.palgebra.deps-lint against the real components/palgebra/
  tree (must pass -- it's this session's own dependency-direction claim,
  D9, renamed per H2 landing session ruling R16) and a seeded fixture
  (must fail -- proving the lint actually fires, not just vacuously
  passing)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehrt.palgebra.deps-lint :as deps-lint])
  (:import [java.io File]))

(deftest the-real-palgebra-tree-passes-deps-lint-test
  (let [{:keys [ok? violations]} (deps-lint/lint {:root "components/palgebra"})]
    (is ok? (str "violations: " violations))
    (is (= [] violations))))

;; ---- seeded violation: a palgebra namespace requiring ehrt.tools.* ----

(defn- temp-fixture-tree!
  "Writes one .clj file, with a `ns` form requiring
  ehrt.tools.lint, under a fresh temp directory -- proves the
  lint fires on exactly the violation it exists to catch, without
  touching the real components/palgebra/ tree."
  []
  (let [dir (doto (File/createTempFile "palgebra-deps-lint-fixture" "")
              (.delete)
              (.mkdirs))
        f (io/file dir "bad_ns.clj")]
    (spit f (str "(ns ehrt.palgebra.bad-ns\n"
                 "  (:require [ehrt.tools.lint :as lint]))\n"))
    (.getAbsolutePath dir)))

(deftest lint-catches-a-palgebra-namespace-requiring-ehr-test
  (let [fixture-root (temp-fixture-tree!)
        {:keys [ok? violations]} (deps-lint/lint {:root fixture-root})]
    (is (not ok?))
    (is (= 1 (count violations)))
    (is (= "ehrt.tools.lint" (:required (first violations))))))

(deftest lint-passes-on-an-empty-tree-test
  (let [dir (doto (File/createTempFile "palgebra-deps-lint-empty" "")
              (.delete)
              (.mkdirs))
        {:keys [ok? violations]} (deps-lint/lint {:root (.getAbsolutePath dir)})]
    (is ok?)
    (is (= [] violations))))
