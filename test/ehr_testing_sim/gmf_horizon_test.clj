(ns ehr-testing-sim.gmf-horizon-test
  "Red tests for the history/horizon two-phase run (M5a Task 3, docs/gmf-
  interpreter.md section 3) -- written before ehr-testing-sim.gmf-
  interpreter/run-module exists (ADR-0004 test-first). The ratified
  design: ONE continuous walk from the persona's own DOB (Task 2's
  `initial-context`) through registration and on to a (caller-supplied)
  horizon end, no fixed tick -- every emitted trajectory event is marked
  `:pre-horizon` by the SAME pure predicate ADR-0011's own warm-up mark
  already uses (`t < boundary`), never a second, separately-driven phase
  pass. This is what makes 'the phases genuinely share state' true by
  construction rather than by extra plumbing: attributes and the
  trajectory-so-far are the SAME accumulating values threaded across the
  boundary, because there never were two accumulators to begin with."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehr-testing-sim.gmf :as gmf]
            [ehr-testing-sim.gmf-interpreter :as interp]
            [ehr-testing-sim.persona :as persona])
  (:import [java.time LocalDate]
           [java.util Random]))

(def fixture-clinic-json (slurp (io/resource "ehr_testing_sim/fixtures/fixture-clinic.json")))
(def fixture-clinic (:payload (gmf/load-module "fixture-clinic" fixture-clinic-json)))

(defn- adult [seed] (assoc (persona/persona (Random. seed) {}) :sex :female))

;; A registration instant well past DOB, so the module has room to run
;; both an onset (history) and an encounter+observation (horizon).
(defn- registration-t-for [persona] (+ (interp/dob-epoch-day persona) (* 365 20)))

;; --- Property 1: the phase boundary is exactly t < registration-t --------

(defspec phase-boundary-is-exactly-t-less-than-registration-instant 150
  (prop/for-all [seed gen/large-integer]
    (let [p (adult seed)
          reg-t (registration-t-for p)
          {:keys [trajectory]} (interp/run-module fixture-clinic (Random. seed) p reg-t)]
      (every? (fn [event] (= (boolean (:pre-horizon event)) (< (:t event) reg-t))) trajectory))))

;; --- Property 2: determinism across both phases --------------------------

(defspec run-module-is-deterministic-for-the-same-inputs 150
  (prop/for-all [seed gen/large-integer]
    (let [p (adult seed)
          reg-t (registration-t-for p)
          run1 (interp/run-module fixture-clinic (Random. seed) p reg-t)
          run2 (interp/run-module fixture-clinic (Random. seed) p reg-t)]
      (= run1 run2))))

;; --- Property 3: a Guard blocking on an attribute set only in history
;; passes in horizon -- the phases genuinely share state -------------------

(def shared-state-module
  {:id "shared-mod"
   :name "SharedState"
   :states {:initial {:type :initial :direct-transition :mark}
            :mark {:type :set-attribute :attribute "seen-doctor" :value true :direct-transition :wait}
            :wait {:type :delay :range {:low 400 :high 400 :unit "days"} :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :attribute :attribute "seen-doctor" :operator "==" :value true}
                    :direct-transition :visit}
            :visit {:type :encounter :encounter-class :ambulatory
                    :codes [{:system :snomed :code "185345009" :display "Encounter for symptom"}]
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest guard-blocking-on-an-attribute-set-only-in-history-passes-in-horizon
  (let [p (adult 7)
        ;; :mark (history, t = DOB) sets the attribute; :wait's 400-day
        ;; delay crosses well past a registration instant set at +100
        ;; days, so :check (and the resulting :visit encounter) fall in
        ;; the HORIZON phase, reading an attribute this same run set
        ;; during HISTORY -- there is no second module instance, no
        ;; attribute reset at the boundary.
        reg-t (+ (interp/dob-epoch-day p) 100)
        result (interp/run-module shared-state-module (Random. 1) p reg-t)]
    (is (= :terminal (:status result)))
    (is (true? (get-in result [:attributes :shared-mod/seen-doctor])))
    (let [visit-event (first (filter #(= :encounter (:event %)) (:trajectory result)))]
      (is (some? visit-event))
      (is (false? (:pre-horizon visit-event)) "the encounter fires in horizon, unmarked"))))

;; --- Property 4: an under-age Guard flips correctly as the virtual clock
;; crosses the threshold -----------------------------------------------------

(def age-gated-module
  {:id "age-gate-mod"
   :name "AgeGate"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :age :operator ">=" :quantity 5 :unit "years"}
                    :direct-transition :visit}
            :visit {:type :encounter :encounter-class :wellness
                    :codes [{:system :snomed :code "185345009" :display "Encounter for symptom"}]
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest age-guard-crossing-lands-in-history-when-registration-is-still-early
  (let [p (adult 3)
        five-years-days (* 365 5)
        ;; registration BEFORE the age-5 threshold is reached -- the
        ;; guard's own analytic jump still crosses it (no fixed tick, no
        ;; polling), but the resulting encounter's own :t is still less
        ;; than registration-t, so it is :pre-horizon true.
        reg-t (+ (interp/dob-epoch-day p) five-years-days 10)
        result (interp/run-module age-gated-module (Random. 1) p reg-t)
        visit-event (first (filter #(= :encounter (:event %)) (:trajectory result)))]
    (is (true? (:pre-horizon visit-event)))))

(deftest age-guard-crossing-lands-in-horizon-when-registration-is-early-enough
  (let [p (adult 3)
        ;; registration set BEFORE the patient turns 5 -- the guard still
        ;; resolves the SAME way (the analytic jump doesn't know or care
        ;; about registration-t), but now the crossing happens AFTER
        ;; registration, so the same encounter is unmarked.
        reg-t (+ (interp/dob-epoch-day p) 30)
        result (interp/run-module age-gated-module (Random. 1) p reg-t)
        visit-event (first (filter #(= :encounter (:event %)) (:trajectory result)))]
    (is (false? (:pre-horizon visit-event)))))

;; --- Task 3.3: scripted end-to-end against fixture-clinic -----------------

(deftest scripted-end-to-end-fixture-clinic-onset-in-history-encounter-in-horizon
  (testing "a persona and seed chosen so the module takes the onset branch
            (Merge_Point's 0.7 arm) and the female intake branch (so
            Treatment_Branch's own 3-way split is reachable), with
            registration landing between the condition onset and the
            doctor visit"
    (let [seed 1
          p (adult seed)
          onset-run (interp/run-module fixture-clinic (Random. seed) p (+ (interp/dob-epoch-day p) 1))
          onset-event (first (filter #(= :condition-onset (:event %)) (:trajectory onset-run)))
          _ (is (some? onset-event) "seed 1 must take the onset branch for this demo to mean anything")
          reg-t (inc (:t onset-event))
          result (interp/run-module fixture-clinic (Random. seed) p reg-t)
          citations (mapv (fn [e] (cond-> {:module (:module e) :state (:state e) :event (:event e)
                                            :pre-horizon (boolean (:pre-horizon e))}
                                     (:references e) (assoc :references (:references e))))
                          (:trajectory result))]
      (is (= :terminal (:status result)))
      (is (= [{:module "fixture-clinic" :state :sinusitis-onset :event :condition-onset :pre-horizon true}
              {:module "fixture-clinic" :state :doctor-visit-encounter :event :encounter :pre-horizon false}
              {:module "fixture-clinic" :state :take-temperature :event :observation :pre-horizon false}
              {:module "fixture-clinic" :state :prescribe-amoxicillin :event :medication-order :pre-horizon false}
              {:module "fixture-clinic" :state :end-encounter :event :encounter-end :pre-horizon false :references 1}
              {:module "fixture-clinic" :state :end-medication :event :medication-end :pre-horizon false :references 3}
              {:module "fixture-clinic" :state :resolve-condition :event :condition-end :pre-horizon false :references 0}]
             citations)
          "exact event sequence (module/state/event citations, pre-horizon
           marks, and cross-event references): onset in history, everything
           from the doctor visit onward in horizon -- glass-box-traceable,
           per docs/gmf-interpreter.md section 6")
      (testing "concept triplets carried verbatim (code passthrough law)"
        (is (= [{:system :snomed :code "36971009" :display "Sinusitis (disorder)"}]
               (:codes (first (:trajectory result)))))
        (is (= [{:system :snomed :code "185345009" :display "Encounter for symptom"}]
               (:codes (second (:trajectory result)))))))))

;; --- Remaining coverage: PriorState's own windowed form, and
;; run-module's optional horizon-end-t bound --------------------------------

(def windowed-prior-state-module
  {:id "window-mod" :name "Window"
   :states {:initial {:type :initial :direct-transition :visit}
            :visit {:type :encounter :encounter-class :ambulatory
                    :codes [{:system :snomed :code "185345009" :display "Encounter for symptom"}]
                    :direct-transition :wait}
            :wait {:type :delay :exact {:quantity 40 :unit "days"} :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :prior-state :name :visit :window {:quantity 30 :unit "days"}}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest prior-state-with-a-window-blocks-once-the-visit-is-outside-it
  (let [ctx (interp/initial-context (adult 1))
        outcome (interp/walk-module windowed-prior-state-module (Random. 1) ctx)]
    (is (= :blocked (:status outcome))
        "the visit was 40 days ago, outside the guard's own 30-day window")))

(deftest run-module-stops-at-an-explicit-horizon-end-t
  (let [p (adult 1)
        early-end (+ (interp/dob-epoch-day p) 1)
        result (interp/run-module fixture-clinic (Random. 1) p early-end early-end)]
    (is (= :horizon-complete (:status result)))))
