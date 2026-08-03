(ns ehrt.sim-emit-hl7.v2-replay-test
  "Task 2 (M6): the v2-replay accumulator and the emitter-coherence
  property it exists to check -- 'the law graduates' (docs/sim-theory.md's
  global emitter-coherence law becomes a real property test once a
  second emitter exists to fold the wire back into state). Written
  test-first (sim/ADR-0004).

  Scope boundary, documented not silent: bed-swap (A17) and merge (A40)
  are genuinely two-participant messages (two PID/PV1 pairs in ONE
  message, a shared-MRN reassignment mid-run) whose own wire-identity
  reconstruction is real, separate engineering scope -- this property
  runs over churn EXCLUDING bed-swap/merge (cancel-admit/cancel-transfer/
  cancel-discharge/transfer-in-error only), the same 'deferred with a
  contract note, not silently stubbed' treatment EmitState's own CDA arm
  gets. `ehrt.sim-emit-hl7.v2-replay/unsupported-trigger` documents the
  same boundary from the accumulator's own side."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim.engine :as engine]
            [ehrt.sim-emit-hl7.emit-hl7 :as emit-hl7]
            [ehrt.sim-trajectory.interface :as sim-trajectory]
            [ehrt.sim-emit-hl7.v2-replay :as v2-replay]))

(def ref-date "2024-01-01")
(def utc-offset "+00:00")

;; --- message-evolve: hand-built single-message unit tests -----------------

(deftest a-bare-admission-message-self-initializes-the-accumulator
  (testing "bootstrap-from-empty (M6 Task 2): replay from an empty
            accumulator succeeds because a patient's first message
            self-initializes -- no separate 'register' step needed on
            the wire side, since EVERY message carries full PID
            enrichment already"
    (let [{:keys [ground-truth facility providers]} (engine/run {:seed 42 :patients 1})
          messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
          reconstructed (v2-replay/replay-messages (take 1 messages) ref-date)
          [mrn entry] (first reconstructed)]
      (is (= 1 (count reconstructed)))
      (is (string? mrn))
      (is (= :admitted (:status entry)))
      (is (some? (:persona entry))))))

(deftest replay-messages-reconstructs-admission-transfer-discharge
  (let [{:keys [ground-truth facility providers]} (engine/run {:seed 7 :patients 1})
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        reconstructed (v2-replay/replay-messages messages ref-date)
        [_ entry] (first reconstructed)]
    (is (= :discharged (:status entry)))
    (is (nil? (:location entry)))
    (is (some? (:discharged-at entry)))))

(deftest replay-messages-reconstructs-cancel-admit-back-to-new
  (let [pathway {:name "cancel-admit" :steps [{:type :admission :location "Renal"}
                                              {:type :cancel-admit}]}
        {:keys [ground-truth facility providers]} (engine/run {:seed 1 :patients 1 :pathways [{:pathway pathway :weight 1}]})
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        reconstructed (v2-replay/replay-messages messages ref-date)
        [_ entry] (first reconstructed)]
    (is (= :new (:status entry)))
    (is (nil? (:location entry)))
    (is (nil? (:admitted-at entry)))))

(deftest replay-messages-reconstructs-order-result-observations
  (let [pathway {:name "cbc-order" :steps [{:type :admission :location "Renal"}
                                           {:type :order :profile :cbc}
                                           {:type :discharge}]}
        {:keys [ground-truth facility providers]} (engine/run {:seed 7 :patients 1 :pathways [{:pathway pathway :weight 1}]})
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        reconstructed (v2-replay/replay-messages messages ref-date)
        [_ entry] (first reconstructed)]
    (is (= 5 (count (:observations entry))) "CBC's 5 analytes")
    (doseq [{:keys [codes value unit reference-range interpretation]} (:observations entry)]
      (is (= 1 (count codes)))
      (is (double? value))
      (is (some? unit))
      (is (some? reference-range))
      (is (some? interpretation)))))

;; --- the projection function -----------------------------------------------

(deftest projection-excludes-patient-id-mrns-home-ward-and-placement
  (let [state {:patient-id "P1" :mrns #{"M1"} :active-mrn "M1" :status :admitted
               :home-ward "Renal" :location {:ward "ED" :bed "ED-H01" :placement :surge}}
        projected (v2-replay/project-to-wire-visible-fields state)]
    (is (not (contains? projected :patient-id)))
    (is (not (contains? projected :mrns)))
    (is (not (contains? projected :home-ward)))
    (is (= {:ward "ED" :bed "ED-H01"} (:location projected)))))

(deftest projection-excludes-conditions-and-medication-orders-entirely
  (let [state {:active-mrn "M1" :status :admitted
               :conditions [{:codes [] :citation {:module "m" :state :s} :onset-t 0 :clinical-status :active}]
               :medication-orders [{:codes [] :citation {:module "m" :state :s} :ordered-t 0 :status :active}]}
        projected (v2-replay/project-to-wire-visible-fields state)]
    (is (not (contains? projected :conditions)))
    (is (not (contains? projected :medication-orders)))))

(deftest projection-gates-payer-on-admission-having-happened
  (let [persona {:name {:family "A" :given "B"} :sex :female :dob "2000-01-01" :age 24
                 :address {:street "s" :city "c" :state "s" :zip "z"} :phone "p" :ssn "900-11-2222"
                 :payer {:id "medicaid" :name "Medicaid" :type :medicaid}}
        before-admission {:active-mrn "M1" :status :new :persona persona}
        after-admission {:active-mrn "M1" :status :admitted :admitted-at 0 :persona persona}]
    (is (not (contains? (:persona (v2-replay/project-to-wire-visible-fields before-admission)) :payer)))
    (is (= {:id "medicaid" :name "Medicaid"}
           (:payer (:persona (v2-replay/project-to-wire-visible-fields after-admission)))))
    (is (not (contains? (:persona (v2-replay/project-to-wire-visible-fields after-admission)) :ssn))
        "wait -- :ssn lives on persona itself, not under :payer; this asserts persona's own :ssn is dropped")))

;; --- the emitter-coherence property: reconstructed state == log-folded
;; state, projected, at EVERY message boundary --------------------------

(def ^:private no-bed-swap-no-merge-churn
  "This property's own documented scope boundary (this namespace's own
  header comment): every churn type EXCEPT the genuinely two-participant
  ones."
  {:cancel-admit 0.08 :cancel-transfer 0.08 :cancel-discharge 0.08 :transfer-in-error 0.08})

(defn- coherent-at-every-boundary?
  [ground-truth messages]
  (let [records (engine/replay ground-truth)
        rendered (filterv #(emit-hl7/message-type-registry (:event (:event %))) records)]
    (and (= (count rendered) (count messages))
         (loop [acc {} rs rendered ms messages]
           (if (empty? rs)
             true
             (let [record (first rs)
                   message (first ms)
                   mrn (:active-mrn (:event record))
                   patient-id (:patient-id record)
                   true-state (get (:world-after record) patient-id)
                   acc' (v2-replay/fold-message acc message ref-date)
                   reconstructed-state (get acc' mrn)]
               (if (= (v2-replay/project-to-wire-visible-fields true-state)
                      (v2-replay/project-to-wire-visible-fields reconstructed-state))
                 (recur acc' (rest rs) (rest ms))
                 false)))))))

(defspec emitter-coherence-reconstructed-state-matches-the-log-fold-at-every-boundary 150
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 10)
                 use-churn gen/boolean
                 use-order gen/boolean]
    (let [pathway (if use-order
                    {:name "cbc-order" :steps [{:type :admission :location "Renal"}
                                               {:type :order :profile :cbc}
                                               {:type :delay :from 30 :to 30}
                                               {:type :discharge}]}
                    {:name "plain" :steps [{:type :admission :location "Renal"}
                                           {:type :delay :from 30 :to 30}
                                           {:type :discharge}]})
          config (cond-> {:seed seed :patients patients :pathways [{:pathway pathway :weight 1}]}
                   use-churn (assoc :churn-profile no-bed-swap-no-merge-churn))
          {:keys [ground-truth facility providers]} (engine/run config)
          messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)]
      (coherent-at-every-boundary? ground-truth messages))))

(defspec emitter-coherence-holds-for-module-driven-outpatient-trajectories 150
  (prop/for-all [seed gen/large-integer]
    (let [{:keys [ground-truth facility providers]}
          (engine/run {:seed seed :patients 5
                       :pathway {:name "module-only" :steps []}
                       :modules [(sim-trajectory/singleton-closure
                                  (:payload (sim-trajectory/load-module
                                             "sinusitis" (slurp (io/resource "sim/modules/sinusitis.json")))))]
                       :module-assignment [{:module-id "sinusitis" :weight 1}]
                       :module-horizon-days 3650})
          messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)]
      (coherent-at-every-boundary? ground-truth messages))))
