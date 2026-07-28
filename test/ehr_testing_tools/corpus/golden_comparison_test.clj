(ns ehr-testing-tools.corpus.golden-comparison-test
  "Test-first (ruling 5, SS-1 Step 4): written before ehr-testing-tools.
  corpus.golden-comparison existed. This is the comparison HARNESS's own
  self-test -- both cases use only the pre-SS-1 intake! call shape, since
  the harness itself must be proven sound (confirms a real match, catches
  a real mismatch) before Step 4's refactor commit points it at the new
  dir: Source call path (test-integration/...intake_source_golden_test.
  clj, ^:integration, needs a real generated corpus)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehr-testing-tools.corpus.intake :as intake]
            [ehr-testing-tools.corpus.golden-comparison :as golden])
  (:import [java.io File]))

(defn- temp-dir []
  (let [f (File/createTempFile "golden-comparison-test" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

(def sample-bundle-json
  "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"p1\",\"gender\":\"female\"}}]}")

(deftest catalogs-byte-identical?-confirms-a-real-match-test
  (testing "two intake! runs against the same source dir, same label/received, land byte-identical"
    (let [src (temp-dir)
          out-a (temp-dir)
          out-b (temp-dir)
          _ (spit (io/file src "patient.json") sample-bundle-json)]
      (intake/intake! {:source-dir src :source-label "golden" :out out-a :received "2026-07-28"})
      (intake/intake! {:source-dir src :source-label "golden" :out out-b :received "2026-07-28"})
      (is (golden/catalogs-byte-identical? out-a out-b)))))

;; ---- harness sanity: proves the comparator actually catches a real
;; violation, not just rubber-stamping every pair (same discipline as
;; canonical_test/mutate_test's own harness-catches-a-violation tests) ----

(deftest catalogs-byte-identical?-catches-a-real-mismatch-test
  (testing "a different :source-label changes catalog.edn's :source field on every entry -- must be caught"
    (let [src (temp-dir)
          out-a (temp-dir)
          out-b (temp-dir)
          _ (spit (io/file src "patient.json") sample-bundle-json)]
      (intake/intake! {:source-dir src :source-label "golden-a" :out out-a :received "2026-07-28"})
      (intake/intake! {:source-dir src :source-label "golden-b" :out out-b :received "2026-07-28"})
      (is (not (golden/catalogs-byte-identical? out-a out-b)))))
  (testing "different file content changes the catalog id -- must be caught"
    (let [src-a (temp-dir)
          src-b (temp-dir)
          out-a (temp-dir)
          out-b (temp-dir)
          _ (spit (io/file src-a "patient.json") sample-bundle-json)
          _ (spit (io/file src-b "patient.json") "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[]}")]
      (intake/intake! {:source-dir src-a :source-label "golden" :out out-a :received "2026-07-28"})
      (intake/intake! {:source-dir src-b :source-label "golden" :out out-b :received "2026-07-28"})
      (is (not (golden/catalogs-byte-identical? out-a out-b))))))

(deftest catalogs-byte-identical?-reports-the-diverging-file-test
  (testing "on a mismatch, the explain payload names which of the two compared files diverged"
    (let [src (temp-dir)
          out-a (temp-dir)
          out-b (temp-dir)
          _ (spit (io/file src "patient.json") sample-bundle-json)]
      (intake/intake! {:source-dir src :source-label "golden-a" :out out-a :received "2026-07-28"})
      (intake/intake! {:source-dir src :source-label "golden-b" :out out-b :received "2026-07-28"})
      (let [explain (golden/compare-catalogs out-a out-b)]
        (is (false? (:identical? explain)))
        (is (contains? (set (:diverging-files explain)) "catalog.edn"))))))
