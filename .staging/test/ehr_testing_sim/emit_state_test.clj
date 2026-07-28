(ns ehr-testing-sim.emit-state-test
  "EmitState (docs/sim-theory.edn): state-history -> FHIR R4 Bundle.
  Written test-first (ADR-0004). snapshot-at is the literal
  snapshot-at-instant law (a pure function of REPLAY RECORDS --
  ehr-testing-sim.engine/replay's own output, the fold, ADR-0008 -- at
  an instant t; no log access beyond the fold). The resource builders
  are pure functions of one patient's own folded state; `bundle-run` is
  the convenience that ties both to a real ground-truth log.

  Cross-emitter id property (M6 Task 1 point 3): FHIR ids/references
  derive from the SAME identifiers EmitHL7 uses -- patient-id,
  active-mrn in Patient.identifier -- checked here as a property over
  random runs, comparing against ehr-testing-sim.emit-hl7's own PID-3."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.data.json :as json]
            [ehr-testing-sim.emit-state :as emit-state]
            [ehr-testing-sim.emit-hl7 :as emit-hl7]
            [ehr-testing-sim.engine :as engine]
            [com.nervestaple.hl7-parser.parser :as parser]
            [com.nervestaple.hl7-parser.message :as message]))

(def ref-date "2024-01-01")
(def utc-offset "+00:00")

;; --- snapshot-at: the pure fold-at-instant primitive ----------------------

(deftest snapshot-at-before-any-event-is-empty
  (let [{:keys [ground-truth]} (engine/run {:seed 1 :patients 1})
        records (engine/replay ground-truth)]
    (is (= {} (emit-state/snapshot-at records -1)))))

(deftest snapshot-at-returns-the-fold-immediately-after-the-last-applicable-event
  (let [{:keys [ground-truth]} (engine/run {:seed 1 :patients 1})
        records (engine/replay ground-truth)
        admission-t (:t (first (filter #(= :admission (:event %)) ground-truth)))
        snapshot (emit-state/snapshot-at records admission-t)
        pid (engine/patient-id-for 1 0)]
    (is (= :admitted (:status (get snapshot pid))))))

(deftest snapshot-at-end-matches-the-runs-own-final-fold
  (let [{:keys [ground-truth]} (engine/run {:seed 1 :patients 3})
        records (engine/replay ground-truth)
        end-t (reduce max 0 (map :t ground-truth))
        snapshot (emit-state/snapshot-at records end-t)]
    (is (= 3 (count snapshot)))
    (doseq [i (range 3)]
      (is (= :discharged (:status (get snapshot (engine/patient-id-for 1 i))))))))

;; --- resource builders: pure functions of one patient's own state --------

(def ^:private a-persona
  {:name {:family "O'Brien" :given "Siobhan"} :sex :female :dob "1969-03-28" :age 55
   :address {:street "1 Main St" :city "Austin" :state "TX" :zip "78701"}
   :phone "512-555-0100" :ssn "900-11-2222"
   :payer {:id "commercial-hmo" :name "Commercial HMO" :type :commercial}})

(deftest patient-resource-carries-patient-id-active-mrn-and-persona-fields
  (let [state {:patient-id "PID-000000-abc" :active-mrn "MRN000001" :status :admitted :persona a-persona}
        bundle (emit-state/patient-bundle ref-date utc-offset 42 state)
        patient (first (filter #(= "Patient" (:resourceType %)) (map :resource (:entry bundle))))]
    (is (= "PID-000000-abc" (:id patient)))
    (is (= "MRN000001" (:value (first (:identifier patient)))))
    (is (= "O'Brien" (:family (first (:name patient)))))
    (is (= "Siobhan" (first (:given (first (:name patient))))))
    (is (= "female" (:gender patient)))
    (is (= "1969-03-28" (:birthDate patient)))))

(deftest encounter-resource-reflects-lifecycle-and-is-absent-before-admission
  (let [never-admitted {:patient-id "P1" :active-mrn "M1" :status :new :persona a-persona}
        admitted {:patient-id "P1" :active-mrn "M1" :status :admitted :class :inpatient
                  :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}
                  :admitted-at 100 :persona a-persona}
        discharged (assoc admitted :status :discharged :discharged-at 500)
        resources-of (fn [state] (map :resource (:entry (emit-state/patient-bundle ref-date utc-offset 42 state))))
        encounter-of (fn [state] (first (filter #(= "Encounter" (:resourceType %)) (resources-of state))))]
    (is (nil? (encounter-of never-admitted)))
    (is (= "in-progress" (:status (encounter-of admitted))))
    (is (= "IMP" (:code (:class (encounter-of admitted)))))
    (is (= "Renal" (:display (:location (first (:location (encounter-of admitted)))))))
    (is (= "finished" (:status (encounter-of discharged))))
    (is (some? (:end (:period (encounter-of discharged)))))))

(deftest condition-resource-renders-the-stored-triplet-and-clinical-status
  (let [a-concept {:system :snomed :code "36971009" :display "Sinusitis (disorder)"}
        state {:patient-id "P1" :active-mrn "M1" :status :discharged :persona a-persona
               :conditions [{:codes [a-concept] :citation {:module "sinusitis" :state :onset}
                             :onset-t 10 :clinical-status :resolved :end-t 200}]}
        resources (map :resource (:entry (emit-state/patient-bundle ref-date utc-offset 42 state)))
        condition (first (filter #(= "Condition" (:resourceType %)) resources))]
    (is (= "36971009" (:code (first (:coding (:code condition))))))
    (is (= "http://snomed.info/sct" (:system (first (:coding (:code condition))))))
    (is (= "resolved" (:code (first (:coding (:clinicalStatus condition))))))
    (is (some? (:abatementDateTime condition)))))

(deftest observation-resource-renders-loinc-value-unit-range-and-interpretation
  (let [loinc {:system :loinc :code "6690-2" :display "Leukocytes [#/volume] in Blood"}
        state {:patient-id "P1" :active-mrn "M1" :status :admitted :persona a-persona
               :observations [{:codes [loinc] :t 50 :value 4.1 :unit "K/uL"
                               :reference-range {:low 4.5 :high 11.0} :interpretation :low}]}
        resources (map :resource (:entry (emit-state/patient-bundle ref-date utc-offset 42 state)))
        obs (first (filter #(= "Observation" (:resourceType %)) resources))]
    (is (= "6690-2" (:code (first (:coding (:code obs))))))
    (is (= "http://loinc.org" (:system (first (:coding (:code obs))))))
    (is (= 4.1 (:value (:valueQuantity obs))))
    (is (= "K/uL" (:unit (:valueQuantity obs))))
    (is (= 4.5 (:value (:low (first (:referenceRange obs))))))
    (is (= "L" (:code (first (:coding (first (:interpretation obs)))))))))

(deftest medication-request-resource-renders-rxnorm-and-status
  (let [rxnorm {:system :rxnorm :code "308191" :display "Amoxicillin"}
        state {:patient-id "P1" :active-mrn "M1" :status :admitted :persona a-persona
               :medication-orders [{:codes [rxnorm] :citation {:module "m" :state :s}
                                    :ordered-t 20 :status :completed :ended-t 300}]}
        resources (map :resource (:entry (emit-state/patient-bundle ref-date utc-offset 42 state)))
        med (first (filter #(= "MedicationRequest" (:resourceType %)) resources))]
    (is (= "308191" (:code (first (:coding (:medicationCodeableConcept med))))))
    (is (= "completed" (:status med)))
    (is (= "order" (:intent med)))))

(deftest coverage-resource-renders-the-sampled-payer
  (let [state {:patient-id "P1" :active-mrn "M1" :status :admitted :persona a-persona}
        resources (map :resource (:entry (emit-state/patient-bundle ref-date utc-offset 42 state)))
        coverage (first (filter #(= "Coverage" (:resourceType %)) resources))]
    (is (= "active" (:status coverage)))
    (is (= "Commercial HMO" (:display (first (:payor coverage)))))))

(deftest no-invented-fields-a-bare-registered-only-patient-yields-just-patient-and-coverage
  (let [state {:patient-id "P1" :active-mrn "M1" :status :new :persona a-persona}
        resources (map (comp :resourceType) (map :resource (:entry (emit-state/patient-bundle ref-date utc-offset 42 state))))]
    (is (= #{"Patient" "Coverage"} (set resources)))))

;; --- the bundle is valid JSON --------------------------------------------

(deftest patient-bundle-round-trips-through-clojure-data-json
  (let [{:keys [ground-truth]} (engine/run {:seed 42 :patients 2})
        bundles (emit-state/bundle-run ground-truth ref-date utc-offset 42 :end)]
    (is (= 2 (count bundles)))
    (doseq [[_ bundle] bundles]
      (is (= "Bundle" (:resourceType (json/read-str (json/write-str bundle) :key-fn keyword)))))))

;; --- cross-emitter id property: patient-id/active-mrn resolve the same
;; way in both emitters, over random runs -----------------------------------

(defspec fhir-patient-id-and-active-mrn-resolve-to-the-same-hl7-identity 150
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 8)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients})
          bundles (emit-state/bundle-run ground-truth ref-date utc-offset seed :end)
          messages (emit-hl7/emit ground-truth ref-date utc-offset)]
      (every?
       (fn [i]
         (let [pid (engine/patient-id-for seed i)
               bundle (get bundles pid)]
           (or (nil? bundle) ;; never registered at end -- impossible today, but not this property's claim
               (let [patient (first (filter #(= "Patient" (:resourceType %)) (map :resource (:entry bundle))))
                     fhir-mrn (:value (first (:identifier patient)))
                     own-messages (filter #(= fhir-mrn (message/get-field-first-value (parser/parse %) "PID" 3)) messages)]
                 (and (= pid (:id patient))
                      (seq own-messages))))))
       (range patients)))))

;; --- Standards-native test-data marking (post-M6, ADR-0014) --------------
;; notes/facts-register.md F14: HTEST system/code/display verified against
;; terminology.hl7.org before landing (no-guessing-codes rule).

(def ^:private htest-security
  {:system "http://terminology.hl7.org/CodeSystem/v3-ActReason"
   :code "HTEST"
   :display "test health data"})

(deftest patient-bundle-resources-carry-htest-security-and-run-tag
  (let [state {:patient-id "P1" :active-mrn "M1" :status :admitted :persona a-persona}
        bundle (emit-state/patient-bundle ref-date utc-offset 42 state)
        resources (map :resource (:entry bundle))]
    (is (seq resources) "sanity: this state actually renders resources")
    (doseq [r resources]
      (is (= [htest-security] (:security (:meta r))))
      (is (= [{:system "urn:ehr-testing-sim" :code "42"}] (:tag (:meta r)))))))

(defspec every-resource-in-every-bundle-carries-htest-and-run-tag 100
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 5)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients})
          bundles (emit-state/bundle-run ground-truth ref-date utc-offset seed :end)]
      (and (seq bundles)
           (every?
            (fn [[_ bundle]]
              (and (seq (:entry bundle))
                   (every?
                    (fn [{:keys [resource]}]
                      (and (= [htest-security] (:security (:meta resource)))
                           (= [{:system "urn:ehr-testing-sim" :code (str seed)}]
                              (:tag (:meta resource)))))
                    (:entry bundle))))
            bundles)))))
