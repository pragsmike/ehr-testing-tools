(ns ehrt.palgebra.lint-test
  "Exercises ehrt.palgebra.lint's catalytic-resource mechanism against the
  toy signature (ehrt.palgebra.signature-test's fixtures) and a toy
  taxonomy -- no EHR vocabulary anywhere in this file. This is the
  proof that a caller supplies its own classify/verify pair and the
  generic extraction-and-verification mechanism works unmodified."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.palgebra.lint :as lint]
            [ehrt.palgebra.signature :as signature]))

(def toy-pipeline-path "components/palgebra/test/ehrt/palgebra/fixtures/toy-pipeline.edn")

;; A toy taxonomy: every catalytic resource in the toy pipeline
;; resolves to a single toy target, ":toy-registry", whose "ref" is
;; just a member of a toy in-memory set -- deliberately simpler than
;; ehr-testing-tools.lint's four real targets, since this fixture only
;; needs to prove the mechanism works, not re-implement a taxonomy.
(def toy-registry #{"assembly-tool" "buffing-compound"})

(def toy-taxonomy
  {"assembly-tool"    {:target :toy-registry :ref "assembly-tool"}
   "buffing-compound" {:target :toy-registry :ref "buffing-compound"}})

(defn- toy-verify
  [{:keys [ref]}]
  (if (contains? toy-registry ref)
    {:ok? true :note (str ref " found in toy-registry")}
    {:ok? false :note (str ref " not found in toy-registry")}))

(deftest stages-catalytic-resources-extracts-the-toy-pipelines-catalytic-set-test
  (let [toy-pipeline (signature/read-signature-edn toy-pipeline-path)]
    (is (= #{"assembly-tool" "buffing-compound"}
           (lint/stages-catalytic-resources toy-pipeline)))))

(deftest line-catalytic-resources-parses-a-raw-equation-line-test
  (is (= ["widget-glue"]
         (lint/line-catalytic-resources
          "raw-widget → polished-widget  [Assemble]  {catalytic: widget-glue}"))))

(deftest line-catalytic-resources-exempts-external-lines-test
  (is (= [] (lint/line-catalytic-resources
             "raw-widget → polished-widget  [Assemble]  {external: true; catalytic: widget-glue}"))))

(deftest toy-pipeline-passes-lint-against-the-toy-taxonomy-test
  (let [toy-pipeline (signature/read-signature-edn toy-pipeline-path)
        resources (lint/stages-catalytic-resources toy-pipeline)
        {:keys [ok? violations]} (lint/lint {:resources resources
                                              :classify #(get toy-taxonomy %)
                                              :verify toy-verify})]
    (is ok? (str "violations: " violations))
    (is (= [] violations))))

;; ---- seeded violations, against the generic mechanism directly ----

(deftest lint-catches-an-unclassified-resource-test
  (let [{:keys [ok? violations]} (lint/lint {:resources #{"mystery-resource"}
                                              :classify (constantly nil)
                                              :verify toy-verify})]
    (is (not ok?))
    (is (= :unclassified (:issue (first violations))))
    (is (= "mystery-resource" (:resource (first violations))))))

(deftest lint-catches-an-unresolved-resource-test
  (let [{:keys [ok? violations]} (lint/lint {:resources #{"unregistered-tool"}
                                              :classify (constantly {:target :toy-registry :ref "unregistered-tool"})
                                              :verify toy-verify})]
    (is (not ok?))
    (is (= :unresolved (:issue (first violations))))
    (is (= "unregistered-tool" (:resource (first violations))))))
