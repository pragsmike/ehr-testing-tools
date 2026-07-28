(ns ehrt.tools.diff-test
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.tools.diff :as diff]))

;; ---- diff-paths: the minimal set of paths at which two values differ ----

(deftest diff-paths-identical-values-is-empty-test
  (is (= #{} (diff/diff-paths {"a" 1} {"a" 1}))))

(deftest diff-paths-finds-a-single-changed-map-key-test
  (is (= #{["b"]} (diff/diff-paths {"a" 1 "b" 2} {"a" 1 "b" 3}))))

(deftest diff-paths-reports-a-whole-differing-subtree-once-test
  ;; A wholesale type replacement (map -> scalar) reports the
  ;; subtree's own path once, not every path beneath it -- unlike two
  ;; same-shaped maps with disjoint keys, which recurses per key
  ;; (covered by diff-paths-finds-a-single-changed-map-key-test).
  (is (= #{["b"]} (diff/diff-paths {"a" 1 "b" {"x" 1 "y" 2}} {"a" 1 "b" "totally-different"}))))

(deftest diff-paths-recurses-into-vectors-by-index-test
  (is (= #{[0 "resource" "gender"]}
         (diff/diff-paths [{"resource" {"gender" "female"}}]
                           [{"resource" {"gender" "male"}}]))))

(deftest diff-paths-different-length-vectors-reports-whole-vector-test
  ;; Under-reporting risk avoided: a vector whose length itself differs
  ;; can't be diffed index-by-index, so the whole vector's path is
  ;; reported rather than a misleading partial index comparison.
  (is (= #{["entry"]} (diff/diff-paths {"entry" [1 2]} {"entry" [1 2 3]}))))

(deftest diff-paths-catches-a-real-violation-not-just-rubber-stamps-test
  (let [base {"a" 1 "b" 2}
        broken {"a" 1 "b" 3 "c" 4}]
    (is (not= #{["b"]} (diff/diff-paths base broken)))
    (is (= #{["b"] ["c"]} (diff/diff-paths base broken)))))

;; ---- path->locator-path: renders a diff path in the FHIR locator
;; grammar's own dotted/bracketed form (ehrt.tools.locator) ----

(deftest path-to-locator-path-renders-dotted-fields-test
  (is (= "resource.gender" (diff/path->locator-path ["resource" "gender"]))))

(deftest path-to-locator-path-renders-bracketed-indices-test
  (is (= "entry[0].resource.gender" (diff/path->locator-path ["entry" 0 "resource" "gender"]))))

(deftest path-to-locator-path-renders-an-empty-path-as-whole-value-test
  (is (= "" (diff/path->locator-path []))))
