(ns ehrt.docs-tooling.pipeline-test
  "Tests the equation-data schema itself (docs/notation.md, pattern
  nursery #13) -- the notation eats the repo's own dogfood: if
  docs/pipeline.edn doesn't validate, the notation trial fails on its
  own terms."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [ehrt.docs-tooling.pipeline :as pipeline]))

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
  (doseq [k [:transform :normalize :enrich :judge :feedback]]
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
  (let [data (edn/read-string (slurp "components/corpus/docs/pipeline.edn"))]
    (is (pipeline/valid? data))))

(deftest committed-pipeline-edn-has-every-stage-built-test
  ;; P5: Intake, Gate, and Report all move from stub/planned to built.
  ;; P6: Check joins them, also built from the start.
  (let [data (edn/read-string (slurp "components/corpus/docs/pipeline.edn"))
        by-id (into {} (map (juxt :id identity)) (:stages data))]
    (is (= :built (:status (by-id :generate))))
    (is (= :built (:status (by-id :normalize))))
    (is (= :built (:status (by-id :mutate))))
    (is (= :built (:status (by-id :intake))))
    (is (= :built (:status (by-id :gate))))
    (is (= :built (:status (by-id :report))))
    (is (= :built (:status (by-id :check))))))

(deftest committed-pipeline-edn-check-stage-consumes-datum-test
  ;; Check is the second judge alongside Gate (docs/notation.md) --
  ;; both consume the same union resource.
  (let [data (edn/read-string (slurp "components/corpus/docs/pipeline.edn"))
        by-id (into {} (map (juxt :id identity)) (:stages data))]
    (is (= :judge (:kind (by-id :check))))
    (is (some #{"datum"} (:inputs (by-id :check))))))

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

;; ---- union resources (docs/notation.md, P6): a named resource
;; declared as the union of others -- a stage consuming the union
;; accepts any member. Closes the P5 diagram gap (pattern nursery #13):
;; Gate's own "datum" input previously had no producing equation. ----

(def sample-union-resource
  {:resource "datum" :union-of ["canonical-fhir-datum" "mutant-fhir-datum" "foreign-file"]})

(deftest valid-union-resource-passes-test
  (is (pipeline/valid-union-resource? sample-union-resource)))

(deftest union-resource-requires-resource-and-union-of-test
  (is (not (pipeline/valid-union-resource? (dissoc sample-union-resource :resource))))
  (is (not (pipeline/valid-union-resource? (dissoc sample-union-resource :union-of)))))

(deftest union-resource-renders-as-a-funnel-equation-test
  ;; Reuses the string-diagram skill's existing funnel/spider machinery
  ;; (many-to-one convergence) for the union's merge node, rather than
  ;; inventing new diagram machinery for the same shape.
  (is (= "canonical-fhir-datum × mutant-fhir-datum × foreign-file → datum  [UnionDatum]  {spider: funnel}"
         (pipeline/union-resource->equation-line sample-union-resource))))

(deftest pipeline->equations-text-includes-union-resource-lines-test
  (let [text (pipeline/pipeline->equations-text {:stages [sample-stage] :resources [sample-union-resource]})]
    (is (clojure.string/includes? text "{spider: funnel}"))
    (is (clojure.string/includes? text "[UnionDatum]"))))

(deftest pipeline-schema-accepts-resources-key-test
  (is (pipeline/valid? {:schema-version 1 :stages [sample-stage] :resources [sample-union-resource]})))

(deftest pipeline-rejects-a-bad-union-resource-among-good-ones-test
  (is (not (pipeline/valid? {:schema-version 1 :stages [sample-stage]
                              :resources [sample-union-resource (dissoc sample-union-resource :union-of)]}))))

;; ---- external stages (docs/notation.md, P6): a black-box stage the
;; repo doesn't implement -- inputs/outputs, no laws, rendered dashed. ----

(def sample-external-stage
  {:id :transform :label "Transform" :external? true
   :inputs ["canonical-fhir-datum"] :outputs ["transform-output"]})

(deftest valid-external-stage-passes-test
  (is (pipeline/valid-external-stage? sample-external-stage)))

(deftest external-stage-requires-external-true-test
  (is (not (pipeline/valid-external-stage? (assoc sample-external-stage :external? false)))))

(deftest external-stage-renders-with-external-annotation-test
  (is (= "canonical-fhir-datum → transform-output  [Transform]  {external: true}"
         (pipeline/external-stage->equation-line sample-external-stage))))

(deftest pipeline->equations-text-includes-external-stage-lines-test
  (let [text (pipeline/pipeline->equations-text {:stages [sample-stage] :external-stages [sample-external-stage]})]
    (is (clojure.string/includes? text "{external: true}"))
    (is (clojure.string/includes? text "[Transform]"))))

(deftest pipeline-schema-accepts-external-stages-key-test
  (is (pipeline/valid? {:schema-version 1 :stages [sample-stage] :external-stages [sample-external-stage]})))

;; ---- dogfooding: the committed docs/pipeline.edn closes the P5
;; disconnected-wire gap -- Gate's "datum" input now has a producer,
;; either a stage output or a declared union resource. ----

(deftest committed-pipeline-edn-gate-input-datum-has-a-producer-test
  (let [data (edn/read-string (slurp "components/corpus/docs/pipeline.edn"))
        stage-outputs (mapcat :outputs (:stages data))
        union-resources (map :resource (:resources data))]
    (is (some #{"datum"} (concat stage-outputs union-resources)))))

;; ---- feedback annotation (ADR-0152): a `:kind :feedback` stage whose
;; own output re-enters its inputs renders the string-diagram skill's
;; `{feedback: X→X}` wire annotation. DERIVED from the equation, not a
;; new schema key -- the fact is already in :inputs/:outputs/:kind, and
;; adding a key would put the same fact in two places for an editor to
;; keep in step, which is the exact defect ADR-0152 was chartered to
;; close one hop upstream. ----

(def sample-feedback-stage
  {:id :calibrate
   :label "Calibrate"
   :kind :feedback
   :status :planned
   :inputs ["sim-corpus" "feed-statistics" "churn-profile"]
   :outputs ["churn-profile"]})

(deftest feedback-stage-renders-a-feedback-annotation-test
  (is (= (str "sim-corpus × feed-statistics × churn-profile → churn-profile"
              "  [Calibrate]  {feedback: churn-profile→churn-profile}")
         (pipeline/stage->equation-line sample-feedback-stage))))

(deftest feedback-annotation-follows-a-catalytic-annotation-test
  ;; Both annotations on one stage: catalytic first, feedback second, one
  ;; brace group each -- the converter's parse_annotations reads them
  ;; independently, so order is a house-style choice pinned here.
  (let [line (pipeline/stage->equation-line (assoc sample-feedback-stage :catalytic ["tuner"]))]
    (is (clojure.string/includes? line "{catalytic: tuner}"))
    (is (clojure.string/includes? line "{feedback: churn-profile→churn-profile}"))
    (is (< (clojure.string/index-of line "{catalytic:")
           (clojure.string/index-of line "{feedback:")))))

(deftest only-a-feedback-kind-stage-gets-the-feedback-annotation-test
  ;; A :transform whose output happens to share a name with an input is
  ;; not a feedback loop -- :kind is what declares the loop, so the
  ;; derivation is gated on it rather than on the name coincidence alone.
  (let [not-feedback (assoc sample-feedback-stage :kind :transform :status :built)]
    (is (not (clojure.string/includes? (pipeline/stage->equation-line not-feedback)
                                       "{feedback:")))))

(deftest a-feedback-stage-with-no-returning-output-gets-no-annotation-test
  (let [open-loop (assoc sample-feedback-stage :outputs ["tuning-report"])]
    (is (not (clojure.string/includes? (pipeline/stage->equation-line open-loop)
                                       "{feedback:")))))

(deftest committed-pipeline-edn-renders-no-feedback-annotation-test
  ;; The byte-unchanged assertion ADR-0152's fence requires: pipeline.edn
  ;; has no :feedback-kind stage, so the new derivation cannot move
  ;; docs/dev/pipeline.md. Stated as a test rather than as a claim so it
  ;; stays true if a feedback stage is ever added there.
  (let [data (edn/read-string (slurp "components/corpus/docs/pipeline.edn"))
        text (pipeline/pipeline->equations-text data)]
    (is (empty? (filter #(= :feedback (:kind %)) (:stages data))))
    (is (not (clojure.string/includes? text "{feedback:")))))

;; ---- the generated banner (ADR-0152): docsgen.clj's own `banner` is
;; HTML-comment shaped, and the string-diagram converter only ignores
;; lines beginning `#`, so the equations file needs a `#`-prefixed
;; equivalent. Kept here rather than generalising docsgen's helper with a
;; comment-prefix argument: that helper serves markdown outputs only, and
;; parameterizing it for one caller in another namespace buys coupling
;; with no second use. ----

(deftest generated-comment-header-is-hash-prefixed-and-four-lines-test
  (let [header (pipeline/generated-comment-header
                {:make-target "sim-theory" :source "components/sim/docs/sim-theory.edn"})
        lines (clojure.string/split-lines header)]
    (is (= 4 (count lines))
        "pinned at 4 lines -- the equations file's arrow numbering derives from its line count")
    (is (every? #(clojure.string/starts-with? % "#") lines)
        "every banner line must start with # -- the converter ignores only those")
    (is (clojure.string/includes? header "make sim-theory"))
    (is (clojure.string/includes? header "components/sim/docs/sim-theory.edn"))
    (is (clojure.string/includes? header "do not hand-edit"))
    (is (clojure.string/ends-with? header "\n")
        "ends with a newline so the first equation line starts its own line")))
