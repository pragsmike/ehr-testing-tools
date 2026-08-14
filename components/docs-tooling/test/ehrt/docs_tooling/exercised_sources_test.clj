(ns ehrt.docs-tooling.exercised-sources-test
  "ADR-0129: the committed exercised-sources registry loads, is
  schema-valid, and seeds the two pre-existing pairs plus the five new
  ones dimension 1 charters -- script-file existence is deliberately
  NOT asserted here (five of the seven rows name a script this same
  session lands one commit later; `ehrt.docs-tooling.strip-fresh`'s own
  `check-entry` is what reports a missing script as its own RED
  finding, not this loader test)."
  (:require [clojure.test :refer [deftest is]]
            [ehrt.docs-tooling.exercised-sources :as reg]))

(deftest registry-loads-and-validates-test
  (let [rows (reg/load-registry)]
    (is (= 7 (count rows)))
    (is (every? #(contains? #{:quickstart-fresh :demo-exerciser-fresh
                               :single-fence :paired}
                             (:extraction %))
                rows))))

(deftest registry-seeds-the-two-pre-existing-pairs-test
  (let [rows (reg/load-registry)]
    (is (some #(and (= "README.md" (:source %))
                     (= "bin/quickstart-demo" (:script %))
                     (= :quickstart-fresh (:extraction %)))
              rows))
    (is (some #(and (= "demos/scenarios/ed-tuesday/README.md" (:source %))
                     (= "bin/demo-exerciser-ed-tuesday" (:script %))
                     (= :demo-exerciser-fresh (:extraction %)))
              rows))))

(deftest registry-seeds-the-five-new-rows-test
  (let [rows (reg/load-registry)
        by-script (into {} (map (juxt :script identity)) rows)]
    (is (= "docs/use-cases/judge-tier-calibration-studies.md"
           (:source (by-script "bin/usecase-judge-tier-calibration"))))
    (is (= "docs/use-cases/profile-tier-hl7v2-conformance-gating.md"
           (:source (by-script "bin/usecase-profile-tier-v2"))))
    (is (= "docs/use-cases/acceptance-qa-of-vendor-corpora.md"
           (:source (by-script "bin/usecase-acceptance-qa"))))
    (is (= {"VENDOR_CORPUS" "test-fixtures/v2"}
           (:env (by-script "bin/usecase-acceptance-qa"))))
    (is (= "docs/use-cases/regression-baselining.md"
           (:source (by-script "bin/usecase-regression-baselining"))))
    (is (= "README.md" (:source (by-script "bin/readme-what-you-get"))))
    (is (= :paired (:extraction (by-script "bin/readme-what-you-get"))))))

(deftest by-source-finds-readmes-two-rows-test
  (let [rows (reg/load-registry)]
    (is (= 2 (count (reg/by-source rows "README.md"))))
    (is (= 1 (count (reg/by-source rows "demos/scenarios/ed-tuesday/README.md"))))))
