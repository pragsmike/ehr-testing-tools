(ns ehrt.sim-emit-hl7.v2-replay-test
  "Task 2 (M6): the v2-replay accumulator and the emitter-coherence
  property it exists to check -- 'the law graduates' (docs/sim-theory.md's
  global emitter-coherence law becomes a real property test once a
  second emitter exists to fold the wire back into state). Written
  test-first (sim/ADR-0004).

  Player fold (ADR-0066): bed-swap (A17) and merge (A40) -- genuinely
  two-participant messages (two PID/PV1 pairs in ONE message, a
  shared-MRN reassignment mid-run) -- are now IN scope. This property
  runs over the FULL `ehrt.sim-engine.churn/sample-profile` churn set,
  bed-swap/merge included; `coherent-at-every-boundary?` checks EVERY
  participant named in an event's own :participants, not only the
  primary one, so a bed-swap/merge boundary is checked twice (once per
  participant) at the same message. `absolutize` is the test-side
  adapter AR-BB1-4 licenses: the engine's own run-relative seconds ->
  absolute epoch millis, anchored to this run's `ref-date` -- the SAME
  anchoring `ehrt.sim-emit-hl7.v2-replay/hl7-instant->millis` performs
  from the wire's own explicit or implied UTC offset, never a shape
  `project-to-wire-visible-fields` itself has to know about."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.engine :as engine]
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
          reconstructed (v2-replay/replay-messages (take 1 messages))
          [mrn entry] (first reconstructed)]
      (is (= 1 (count reconstructed)))
      (is (string? mrn))
      (is (= :admitted (:status entry)))
      (is (some? (:persona entry))))))

(deftest replay-messages-reconstructs-admission-transfer-discharge
  (let [{:keys [ground-truth facility providers]} (engine/run {:seed 7 :patients 1})
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        reconstructed (v2-replay/replay-messages messages)
        [_ entry] (first reconstructed)]
    (is (= :discharged (:status entry)))
    (is (nil? (:location entry)))
    (is (some? (:discharged-at entry)))))

(deftest replay-messages-reconstructs-cancel-admit-back-to-new
  (let [pathway {:name "cancel-admit" :steps [{:type :admission :location "Renal"}
                                              {:type :cancel-admit}]}
        {:keys [ground-truth facility providers]} (engine/run {:seed 1 :patients 1 :pathways [{:pathway pathway :weight 1}]})
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        reconstructed (v2-replay/replay-messages messages)
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
        reconstructed (v2-replay/replay-messages messages)
        [_ entry] (first reconstructed)]
    (is (= 5 (count (:observations entry))) "CBC's 5 analytes")
    (doseq [{:keys [codes value unit reference-range interpretation]} (:observations entry)]
      (is (= 1 (count codes)))
      (is (double? value))
      (is (some? unit))
      (is (some? reference-range))
      (is (some? interpretation)))))

;; --- AR-BB1-1/2 red-first: the fold's NEW behavior on a REAL rendered
;; A17/A40 (built via the emitter's own bed-swap-message/merge-message
;; path -- churn_scenarios_test's own deterministic two-patient
;; construction, reused here -- never a hand-typed message string).
;; Both fail RED against the pre-fix fold (:unsupported-trigger, an
;; uncaught ExceptionInfo) and must go GREEN once the fold extension
;; lands -------------------------------------------------------------

(deftest fold-message-folds-a-real-rendered-a17-into-two-participant-location-updates
  (testing "AR-BB1-1: fold-message folds a real A17's own two PID/PV1
            pairs independently, each as the A02 treatment onto its
            own PID-3's entry -- bootstrap-from-empty holds for both,
            since this test folds the A17 alone, from an empty
            accumulator (foreign traffic opening mid-stream)"
    (let [seed 100
          p2-id (engine/patient-id-for seed 1)
          {:keys [ground-truth facility providers]}
          (engine/run {:seed seed :patients 2 :arrival-gap 0
                       :pathways [{:patient-ordinal 0
                                   :pathway {:name "p1" :steps [{:type :admission :location "Renal"}
                                                                 {:type :bed-swap :with p2-id}]}}
                                  {:patient-ordinal 1
                                   :pathway {:name "p2" :steps [{:type :admission :location "Renal"}]}}]})
          bed-swap-event (last ground-truth)
          [p1-id p2-id*] (mapv :patient-id (:participants bed-swap-event))
          mrn1 (get-in bed-swap-event [:swap p1-id :active-mrn])
          mrn2 (get-in bed-swap-event [:swap p2-id* :active-mrn])
          loc1 (get-in bed-swap-event [:swap p1-id :to])
          loc2 (get-in bed-swap-event [:swap p2-id* :to])
          a17 (last (emit-hl7/emit ground-truth ref-date utc-offset facility providers))
          acc (v2-replay/fold-message {} a17)]
      (is (= 2 (count acc)))
      (is (= {:ward (:ward loc1) :bed (:bed loc1)} (:location (get acc mrn1))))
      (is (= {:ward (:ward loc2) :bed (:bed loc2)} (:location (get acc mrn2))))
      (is (some? (:persona (get acc mrn1))))
      (is (some? (:persona (get acc mrn2))))
      (is (= mrn1 (:active-mrn (get acc mrn1))))
      (is (= mrn2 (:active-mrn (get acc mrn2)))))))

(deftest fold-message-folds-a-real-rendered-a40-into-a-survivor-and-a-tombstone
  (testing "AR-BB1-2: fold-message folds a real A40 -- the surviving
            entry (PID-3) absorbs per the engine's own merge semantics
            (unchanged besides bootstrap), the merged-away entry
            (MRG-1) becomes a :merged tombstone that keeps its own
            last-known fields, mirroring ehrt.sim-engine.engine's own
            evolve :merge :merged arm exactly (only :status changes)"
    (let [seed 100
          p2-id (engine/patient-id-for seed 1)
          {:keys [ground-truth facility providers]}
          (engine/run {:seed seed :patients 2 :arrival-gap 0
                       :pathways [{:patient-ordinal 0
                                   :pathway {:name "p1" :steps [{:type :admission :location "Renal"}
                                                                 {:type :merge :with p2-id}]}}
                                  {:patient-ordinal 1
                                   :pathway {:name "p2" :steps [{:type :admission :location "Renal"}]}}]})
          messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
          [a01-p1 a01-p2 a40] messages
          seeded-acc (-> {} (v2-replay/fold-message a01-p1) (v2-replay/fold-message a01-p2))
          acc (v2-replay/fold-message seeded-acc a40)
          merge-event (last ground-truth)
          survivor-mrn (:surviving-mrn merge-event)
          merged-mrn (:merged-mrn merge-event)]
      (is (= :admitted (:status (get acc survivor-mrn))))
      (is (= :merged (:status (get acc merged-mrn))))
      (is (= (:location (get seeded-acc merged-mrn)) (:location (get acc merged-mrn)))
          "the tombstone keeps its own last-known location, only :status flips")
      (is (= (:attending (get seeded-acc merged-mrn)) (:attending (get acc merged-mrn))))
      (is (= (:location (get seeded-acc survivor-mrn)) (:location (get acc survivor-mrn)))
          "the survivor's own entry is untouched by the wire (PV1 rides blank on A40)"))))

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

(def ^:private ref-date-anchor-millis
  "`ref-date`'s own start-of-day instant, UTC, as epoch millis -- the
  SAME anchor `ehrt.sim-emit-hl7.emit-hl7/reference-instant` uses to
  render every message's own MSH-7 in the first place."
  (.toEpochMilli (.toInstant (.atStartOfDay (java.time.LocalDate/parse ref-date)) java.time.ZoneOffset/UTC)))

(defn- absolutize
  "The test-side adapter AR-BB1-4 licenses: a true (engine) PatientState
  carries run-relative SECONDS on :admitted-at/:discharged-at/each
  observation's own :t; the wire-reconstructed side now carries absolute
  epoch MILLIS instead (`ehrt.sim-emit-hl7.v2-replay/hl7-instant->millis`).
  Converts the true side's own seconds -> millis, anchored to THIS run's
  `ref-date`, so both sides land in the SAME units before
  `project-to-wire-visible-fields` (itself unchanged, reference-date-
  agnostic) projects them."
  [state]
  (let [->millis (fn [seconds] (when (some? seconds) (+ ref-date-anchor-millis (* 1000 seconds))))]
    (cond-> state
      (:admitted-at state) (update :admitted-at ->millis)
      (:discharged-at state) (update :discharged-at ->millis)
      (seq (:observations state)) (update :observations (fn [obs] (mapv #(update % :t ->millis) obs))))))

(defn- coherent-at-every-boundary?
  "Checks EVERY participant named in an event's own :participants (not
  only the primary one, ehrt.sim-engine.engine/replay's own doc) against
  the SAME folded accumulator -- generalizes cleanly over single-
  participant events (one participant, same check as before) and
  bed-swap/merge (two, each keyed by that participant's OWN
  post-event :active-mrn, which neither event ever reassigns -- ADR-0066
  AR-BB1-1/2's own tombstone design)."
  [ground-truth messages]
  (let [records (engine/replay ground-truth)
        rendered (filterv #(emit-hl7/message-type-registry (:event (:event %))) records)]
    (and (= (count rendered) (count messages))
         (loop [acc {} rs rendered ms messages]
           (if (empty? rs)
             true
             (let [record (first rs)
                   message (first ms)
                   participant-ids (mapv :patient-id (:participants (:event record)))
                   acc' (v2-replay/fold-message acc message)]
               (if (every? (fn [pid]
                             (let [true-state (absolutize (get (:world-after record) pid))
                                   mrn (:active-mrn true-state)]
                               (= (v2-replay/project-to-wire-visible-fields true-state)
                                  (v2-replay/project-to-wire-visible-fields (get acc' mrn)))))
                           participant-ids)
                 (recur acc' (rest rs) (rest ms))
                 false)))))))

(defspec emitter-coherence-reconstructed-state-matches-the-log-fold-at-every-boundary 150
  (prop/for-all [seed (gen/large-integer* {:min 0})
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
                   use-churn (assoc :churn-profile churn/sample-profile))
          {:keys [ground-truth facility providers]} (engine/run config)
          messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)]
      (coherent-at-every-boundary? ground-truth messages))))

(defspec emitter-coherence-holds-for-module-driven-outpatient-trajectories 150
  (prop/for-all [seed (gen/large-integer* {:min 0})]
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
