(ns palgebra.signature-test
  "Exercises palgebra.signature's loader and schema shapes against a
  toy signature (design D9/D13) -- two sorts, three stages, no EHR
  vocabulary. This is the fixture proving instantiation is
  data-authoring: a caller supplies its own kind set and its own
  equations EDN, and the generic loader/validator work unmodified."
  (:require [clojure.test :refer [deftest is testing]]
            [palgebra.signature :as signature]))

(def toy-signature-path "palgebra/test/palgebra/fixtures/toy-signature.edn")
(def toy-pipeline-path "palgebra/test/palgebra/fixtures/toy-pipeline.edn")

(deftest toy-signature-loads-its-kinds-test
  (let [{:keys [kinds]} (signature/read-signature-edn toy-signature-path)]
    (is (= #{:transform :judge :normalize} kinds))))

(deftest toy-pipeline-validates-against-its-own-kinds-test
  (let [kinds (:kinds (signature/read-signature-edn toy-signature-path))
        toy-pipeline (signature/read-signature-edn toy-pipeline-path)]
    (is (signature/valid? kinds toy-pipeline))))

(deftest toy-pipeline-rejects-a-kind-outside-the-toy-signature-test
  (let [kinds (:kinds (signature/read-signature-edn toy-signature-path))
        toy-pipeline (signature/read-signature-edn toy-pipeline-path)
        bad-pipeline (assoc-in toy-pipeline [:stages 0 :kind] :not-a-toy-kind)]
    (is (not (signature/valid? kinds bad-pipeline)))))

(deftest toy-pipeline-has-two-sorts-and-three-stages-test
  (let [{:keys [stages]} (signature/read-signature-edn toy-pipeline-path)
        sorts (set (mapcat (fn [{:keys [inputs outputs]}] (concat inputs outputs)) stages))]
    (is (= 3 (count stages)))
    (is (= #{"raw-widget" "polished-widget"} sorts))))

(deftest valid-stage-and-union-resource-helpers-work-against-the-toy-kinds-test
  (let [kinds (:kinds (signature/read-signature-edn toy-signature-path))
        sample-stage {:id :assemble :label "Assemble" :kind :transform :status :built
                      :inputs ["raw-widget"] :outputs ["polished-widget"]}
        sample-union {:resource "widget" :union-of ["raw-widget" "polished-widget"]}]
    (is (signature/valid-stage? kinds sample-stage))
    (is (not (signature/valid-stage? kinds (assoc sample-stage :kind :not-a-toy-kind))))
    (is (signature/valid-union-resource? sample-union))))
