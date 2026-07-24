(ns ehr-testing-tools.pipeline-test
  "Tests the equation-data schema itself (docs/notation.md, pattern
  nursery #13) -- the notation eats the repo's own dogfood: if
  docs/pipeline.edn doesn't validate, the notation trial fails on its
  own terms."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [ehr-testing-tools.pipeline :as pipeline]))

(def sample-stage
  {:id :generate
   :label "Generate"
   :kind :transform
   :status :built
   :inputs ["synthea-config"]
   :outputs ["raw-corpus"]
   :catalytic ["synthea-artifact" "jdk-runtime" "config-hash"]
   :laws ["appends a manifest recording every pinned input"]})

(deftest valid-stage-passes-test
  (is (pipeline/valid-stage? sample-stage)))

(deftest stage-requires-known-kind-test
  (is (not (pipeline/valid-stage? (assoc sample-stage :kind :not-a-real-kind)))))

(deftest stage-requires-known-status-test
  (is (not (pipeline/valid-stage? (assoc sample-stage :status :not-a-real-status)))))

(deftest stage-catalytic-and-laws-are-optional-test
  (is (pipeline/valid-stage? (dissoc sample-stage :catalytic :laws))))

(deftest stage-requires-inputs-and-outputs-test
  (is (not (pipeline/valid-stage? (dissoc sample-stage :inputs))))
  (is (not (pipeline/valid-stage? (dissoc sample-stage :outputs)))))

(deftest stage-allows-a-contract-note-test
  (is (pipeline/valid-stage? (assoc sample-stage :contract "round-trip fidelity verified by EXP-B2"))))

(deftest all-five-stage-kinds-are-known-test
  (doseq [k [:transform :normalize :enrich :gate :feedback]]
    (is (pipeline/valid-stage? (assoc sample-stage :kind k)))))

(deftest valid-pipeline-passes-test
  (is (pipeline/valid? {:schema-version 1 :stages [sample-stage]})))

(deftest pipeline-rejects-non-vector-stages-test
  (is (not (pipeline/valid? {:schema-version 1 :stages #{sample-stage}}))))

(deftest pipeline-rejects-a-bad-stage-among-good-ones-test
  (is (not (pipeline/valid? {:schema-version 1
                              :stages [sample-stage (dissoc sample-stage :kind)]}))))

;; ---- dogfooding: the actual committed docs/pipeline.edn must itself
;; validate against this schema -- the point of pattern #13's trial ----

(deftest committed-pipeline-edn-is-valid-test
  (let [data (edn/read-string (slurp "docs/pipeline.edn"))]
    (is (pipeline/valid? data))))

(deftest committed-pipeline-edn-has-every-stage-built-test
  ;; P5: Intake, Gate, and Report all move from stub/planned to built.
  (let [data (edn/read-string (slurp "docs/pipeline.edn"))
        by-id (into {} (map (juxt :id identity)) (:stages data))]
    (is (= :built (:status (by-id :generate))))
    (is (= :built (:status (by-id :normalize))))
    (is (= :built (:status (by-id :mutate))))
    (is (= :built (:status (by-id :intake))))
    (is (= :built (:status (by-id :gate))))
    (is (= :built (:status (by-id :report))))))

;; ---- rendering: stage -> equation-line (docs/notation.md's equation
;; form, the string-diagram skill's own grammar) ----

(deftest stage-with-no-catalytic-renders-simple-equation-test
  (is (= "raw-corpus → canonical-corpus  [Normalize]"
         (pipeline/stage->equation-line {:label "Normalize" :inputs ["raw-corpus"]
                                          :outputs ["canonical-corpus"]}))))

(deftest stage-with-catalytic-appears-on-both-sides-test
  ;; The skill's own grammar requires catalytic resources to appear in
  ;; the LHS product AND in the {catalytic: ...} annotation -- this is
  ;; the skill's convention, not a duplication bug.
  (is (= "synthea-config × synthea-artifact × jdk-runtime → raw-corpus  [Generate]  {catalytic: synthea-artifact, jdk-runtime}"
         (pipeline/stage->equation-line {:label "Generate" :inputs ["synthea-config"]
                                          :outputs ["raw-corpus"]
                                          :catalytic ["synthea-artifact" "jdk-runtime"]}))))

(deftest stage-with-multiple-outputs-uses-coproduct-test
  (is (= "canonical-fhir-datum → mutant-fhir-datum + lineage-record  [Mutate]"
         (pipeline/stage->equation-line {:label "Mutate" :inputs ["canonical-fhir-datum"]
                                          :outputs ["mutant-fhir-datum" "lineage-record"]}))))

(deftest pipeline->equations-text-renders-every-stage-and-nothing-else-test
  (let [text (pipeline/pipeline->equations-text {:stages [sample-stage]})]
    (is (= 1 (count (clojure.string/split-lines (clojure.string/trim text)))))
    (is (clojure.string/includes? text "[Generate]"))))

(deftest pipeline->equations-text-notes-planned-status-as-a-comment-test
  (let [planned (assoc sample-stage :status :planned)
        text (pipeline/pipeline->equations-text {:stages [planned]})]
    (is (clojure.string/starts-with? text "# planned:"))))
