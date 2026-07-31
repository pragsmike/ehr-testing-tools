(ns ehrt.docs-tooling.lint-test
  "Tier-1 pipeline lint (P6, pattern nursery #13): every catalytic
  resource named in docs/pipeline.edn and docs/use-cases.edn resolves
  to one of the four catalytic targets (docs/notation.md)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehrt.docs-tooling.lint :as lint])
  (:import [java.io File]))

(defn- temp-edn-file! [content]
  (let [f (File/createTempFile "lint-fixture" ".edn")]
    (spit f (pr-str content))
    (.getAbsolutePath f)))

(defn- temp-empty-use-cases-edn! []
  (temp-edn-file! {:schema-version 1 :cases []}))

;; ---- the committed docs/pipeline.edn + docs/use-cases.edn: the real
;; proof this lint exists for -- everything resolves cleanly today ----

(deftest committed-pipeline-and-use-cases-pass-lint-test
  (let [{:keys [ok? violations]} (lint/lint)]
    (is ok? (str "violations: " violations))
    (is (= [] violations))))

;; ---- seeded violation: an unclassified catalytic resource ----

(deftest lint-catches-an-unclassified-catalytic-resource-test
  (let [pipeline-fixture (temp-edn-file!
                           {:schema-version 1
                            :stages [{:id :fake :label "Fake" :kind :transform :status :built
                                      :inputs ["x"] :outputs ["y"]
                                      :catalytic ["totally-unclassified-resource"]}]})
        {:keys [ok? violations]} (lint/lint {:pipeline-edn pipeline-fixture
                                              :use-cases-edn (temp-empty-use-cases-edn!)})]
    (is (not ok?))
    (is (= 1 (count violations)))
    (is (= "totally-unclassified-resource" (:resource (first violations))))
    (is (= :unclassified (:issue (first violations))))))

;; ---- seeded violation: a classified-but-unresolvable reference ----

(deftest lint-catches-a-target-4-resource-referencing-an-unregistered-entry-test
  ;; "operator-catalog" IS classified (target 4, corpus.operators) --
  ;; this fixture points its ref at an id/version that was never
  ;; registered, proving the lint verifies resolution, not just
  ;; classification presence.
  (let [pipeline-fixture (temp-edn-file!
                           {:schema-version 1
                            :stages [{:id :fake :label "Fake" :kind :transform :status :built
                                      :inputs ["x"] :outputs ["y"]
                                      :catalytic ["operator-catalog"]}]})]
    (with-redefs [lint/catalytic-resource-targets
                  (assoc lint/catalytic-resource-targets
                         "operator-catalog" {:target 4 :ref {:registry :corpus.operators
                                                              :id :no-such-operator :version "999"}})]
      (let [{:keys [ok? violations]} (lint/lint {:pipeline-edn pipeline-fixture
                                                  :use-cases-edn (temp-empty-use-cases-edn!)})]
        (is (not ok?))
        (is (= "operator-catalog" (:resource (first violations))))
        (is (= :unresolved (:issue (first violations))))))))

;; ---- external stages are exempt ----

(deftest lint-exempts-catalytic-resources-under-an-external-stage-test
  (let [use-cases-fixture (temp-edn-file!
                            {:schema-version 1
                             :cases [{:id :fixture-case :title "Fixture" :audience "a" :bring "b" :get "c"
                                      :maturity :illustrative
                                      :equations ["x → y  [SomeExternalOp]  {external: true; catalytic: totally-unclassified-resource}"]}]})
        {:keys [ok? violations]} (lint/lint {:pipeline-edn (temp-edn-file! {:schema-version 1 :stages []})
                                              :use-cases-edn use-cases-fixture})]
    (is ok? (str "violations: " violations))
    (is (= [] violations))))

;; ---- non-external use-case equations ARE linted ----

(deftest lint-checks-use-case-equations-that-are-not-external-test
  (let [use-cases-fixture (temp-edn-file!
                            {:schema-version 1
                             :cases [{:id :fixture-case :title "Fixture" :audience "a" :bring "b" :get "c"
                                      :maturity :illustrative
                                      :equations ["x × totally-unclassified-resource → y  [SomeOp]  {catalytic: totally-unclassified-resource}"]}]})
        {:keys [ok? violations]} (lint/lint {:pipeline-edn (temp-edn-file! {:schema-version 1 :stages []})
                                              :use-cases-edn use-cases-fixture})]
    (is (not ok?))
    (is (= "totally-unclassified-resource" (:resource (first violations))))))
