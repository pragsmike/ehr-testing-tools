(ns ehrt.sim-model.pathway-test
  "Schema validity for the IR step vocabulary as it grows past the v0
  walking skeleton -- M1 adds :transfer and the optional
  :force-placement authoring escape hatch (components/sim/docs/operational-models.md)."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-model.pathway :as pathway]))

(deftest sample-admission-discharge-still-valid
  (is (pathway/valid? pathway/sample-admission-discharge)))

(deftest transfer-step-is-valid-ir
  (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal"}
                                          {:type :transfer :location "Cardiology"}
                                          {:type :discharge}]})))

(deftest force-placement-is-valid-on-admission-and-transfer
  (testing "admission"
    (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal"
                                             :force-placement {:ward "Renal" :bed "RENAL-02"}}]})))
  (testing "transfer"
    (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal"}
                                            {:type :transfer :location "Cardiology"
                                             :force-placement {:ward "Cardiology" :bed "CARDIOLOGY-01"}}]}))))

(deftest transfer-without-location-is-invalid
  (is (not (pathway/valid? {:name "t" :steps [{:type :transfer}]}))))

;; --- M2b: churn family IR ------------------------------------------------

(deftest cancel-family-steps-are-valid-ir
  (doseq [step-type [:cancel-admit :cancel-transfer :cancel-discharge]]
    (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal"}
                                            {:type step-type}]})
        (str step-type " should be valid IR"))))

(deftest transfer-in-error-is-valid-ir
  (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal"}
                                          {:type :transfer-in-error :location "Cardiology"}]})))

(deftest transfer-in-error-without-location-is-invalid
  (is (not (pathway/valid? {:name "t" :steps [{:type :transfer-in-error}]}))))

(deftest bed-swap-is-valid-ir-with-and-without-explicit-peer
  (is (pathway/valid? {:name "t" :steps [{:type :bed-swap}]}))
  (is (pathway/valid? {:name "t" :steps [{:type :bed-swap :with "PID-000001"}]})))

(deftest merge-is-valid-ir-with-and-without-explicit-peer
  (is (pathway/valid? {:name "t" :steps [{:type :merge}]}))
  (is (pathway/valid? {:name "t" :steps [{:type :merge :with "PID-000001"}]})))

;; --- M3-adjacent: per-patient pathway assignment (roadmap.md's M3 entry) --

(deftest weighted-pool-entry-is-valid-pathways-config
  (is (pathway/valid-pathways-config?
       [{:pathway pathway/sample-admission-discharge :weight 1}])))

(deftest explicit-ordinal-entry-is-valid-pathways-config
  (is (pathway/valid-pathways-config?
       [{:patient-ordinal 0 :pathway pathway/sample-admission-discharge}])))

(deftest mixed-weighted-and-explicit-entries-are-valid-pathways-config
  (is (pathway/valid-pathways-config?
       [{:pathway pathway/sample-admission-discharge :weight 2}
        {:pathway {:name "t" :steps [{:type :admission :location "Renal"}]} :weight 1}
        {:patient-ordinal 3 :pathway pathway/sample-admission-discharge}])))

(deftest entry-with-neither-weight-nor-ordinal-is-invalid
  (is (not (pathway/valid-pathways-config? [{:pathway pathway/sample-admission-discharge}]))))

(deftest entry-with-invalid-pathway-ir-is-invalid
  (is (not (pathway/valid-pathways-config? [{:pathway {:steps "not-a-pathway"} :weight 1}]))))

;; --- M3: order (result auto-pairs, never hand-authored -- see decide.clj) --

(deftest order-step-is-valid-ir
  (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal"}
                                          {:type :order :profile :cbc}]})))

(deftest order-without-profile-is-invalid
  (is (not (pathway/valid? {:name "t" :steps [{:type :order}]}))))

;; --- M5b: outpatient-visit / outpatient-visit-end (components/patient-simulator/docs/gmf-interpreter.md
;; section 4's sketch, items 5-7) -- no :location field at all -----------

(deftest outpatient-visit-pair-is-valid-ir-with-and-without-a-reason
  (is (pathway/valid? {:name "t" :steps [{:type :outpatient-visit} {:type :outpatient-visit-end}]}))
  (is (pathway/valid? {:name "t" :steps [{:type :outpatient-visit :reason "Sinus congestion"}
                                          {:type :outpatient-visit-end}]}))
  (is (pathway/valid? {:name "t" :steps [{:type :outpatient-visit
                                           :reason {:system :snomed :code "36971009" :display "Sinusitis (disorder)"}}
                                          {:type :outpatient-visit-end}]})))

;; --- M5b: CompileTrajectory's new step types (components/patient-simulator/docs/gmf-interpreter.md
;; section 1's table) -- :procedure/:observation/:medication-order/
;; :medication-end, plus :citation/:conditions on compiled steps -------

(def ^:private a-citation {:module "sinusitis" :state :doctor-visit})
(def ^:private a-concept {:system :snomed :code "36971009" :display "Sinusitis (disorder)"})

(deftest procedure-step-is-valid-ir-with-and-without-a-citation
  (is (pathway/valid? {:name "t" :steps [{:type :procedure :codes [a-concept]}]}))
  (is (pathway/valid? {:name "t" :steps [{:type :procedure :codes [a-concept] :citation a-citation}]})))

(deftest observation-step-is-valid-ir-with-a-sampled-value
  (is (pathway/valid? {:name "t" :steps [{:type :observation :codes [a-concept] :value 38.2 :unit "Cel"
                                           :citation a-citation}]})))

;; --- GMF coverage Wave D stage D1 (ADR-0029 P1/P2, D1a schema RULING):
;; :observation's new optional fields, and :diagnostic-report -------------

(def ^:private a-value-code {:system :snomed :code "10828004" :display "Positive (qualifier value)"})

(deftest observation-step-is-valid-ir-with-value-code-category-and-reference-range
  (is (pathway/valid? {:name "t" :steps [{:type :observation :codes [a-concept] :value-code a-value-code
                                           :category "laboratory" :reference-range {:low 90 :high 120}
                                           :interpretation :normal :citation a-citation}]})))

(deftest diagnostic-report-step-is-valid-ir-with-mixed-child-value-mechanisms
  (is (pathway/valid?
       {:name "t"
        :steps [{:type :diagnostic-report :codes [a-concept] :citation a-citation
                 :observations [{:codes [a-concept] :value 92.0 :unit "mm[Hg]"}
                                {:codes [a-concept] :value-code a-value-code}
                                {:codes [a-concept] :value 98.0 :unit "%" :category "vital-signs"
                                 :reference-range {:low 95 :high 100} :interpretation :normal}]}]})))

(deftest diagnostic-report-step-is-valid-ir-without-report-level-codes
  (testing "D1a-2: :codes is optional -- a MultiObservation/DiagnosticReport
            state with no report-level code is real, source-grounded, not
            an authoring error"
    (is (pathway/valid? {:name "t" :steps [{:type :diagnostic-report :observations [{:codes [a-concept]}]}]}))))

(deftest diagnostic-report-step-without-observations-key-is-invalid
  (is (not (pathway/valid? {:name "t" :steps [{:type :diagnostic-report :codes [a-concept]}]}))))

(deftest medication-order-and-end-are-valid-ir
  (is (pathway/valid? {:name "t" :steps [{:type :medication-order :codes [a-concept] :citation a-citation}
                                          {:type :medication-end :order-citation a-citation :citation a-citation}]})))

(deftest admission-with-a-citation-and-condition-annotations-is-valid-ir
  (is (pathway/valid? {:name "t" :steps [{:type :admission :location "Renal" :citation a-citation
                                           :conditions [{:event :condition-onset :codes [a-concept] :citation a-citation}]}]})))

;; --- ADR-0182: :immunization, the Vaccine state's compile target and an
;; author-facing step in its own right. :series is carried only when the
;; module states it (absent, never 0-defaulted, never nil -- ADR-0178).

(def ^:private a-vaccine {:system :cvx :code "115" :display "Tdap vaccine"})

(deftest immunization-step-is-valid-ir-with-and-without-series-and-citation
  (is (pathway/valid? {:name "t" :steps [{:type :immunization :codes [a-vaccine] :series 1 :citation a-citation}]}))
  (is (pathway/valid? {:name "t" :steps [{:type :immunization :codes [a-vaccine]}]})
      ":series and :citation are both optional"))

(deftest immunization-step-refuses-a-nil-series-and-a-missing-codes-key
  (is (not (pathway/valid? {:name "t" :steps [{:type :immunization :codes [a-vaccine] :series nil}]}))
      "present-and-nil is not absent (ADR-0178)")
  (is (not (pathway/valid? {:name "t" :steps [{:type :immunization :series 1}]}))))


;; --- ADR-0184: :allergy-onset, the AllergyOnset state's compile target and
;; an author-facing step. :allergy-type, :category and :reactions are each
;; carried only when stated: ABSENT otherwise, never nil (ADR-0178). A
;; reaction is its Codes and nothing else -- no severity.

(def ^:private a-substance {:system :snomed :code "762952008" :display "Peanut (substance)"})
(def ^:private a-reaction {:system :snomed :code "49727002" :display "Cough (finding)"})

(deftest allergy-onset-step-is-valid-ir-with-and-without-its-optional-keys
  (is (pathway/valid? {:name "t" :steps [{:type :allergy-onset :codes [a-substance]
                                           :allergy-type "allergy" :category "food"
                                           :reactions [{:codes [a-reaction]}] :citation a-citation}]}))
  (is (pathway/valid? {:name "t" :steps [{:type :allergy-onset :codes [a-substance]}]})
      ":allergy-type, :category, :reactions and :citation are all optional"))

(deftest allergy-onset-step-refuses-nil-optionals-a-severity-and-a-missing-codes-key
  (is (not (pathway/valid? {:name "t" :steps [{:type :allergy-onset :codes [a-substance] :category nil}]}))
      "present-and-nil is not absent (ADR-0178)")
  (is (not (pathway/valid? {:name "t" :steps [{:type :allergy-onset :codes [a-substance]
                                                :reactions [{:codes [a-reaction] :severity :mild}]}]}))
      "a reaction carries no severity in this slice")
  (is (not (pathway/valid? {:name "t" :steps [{:type :allergy-onset :category "food"}]}))))
