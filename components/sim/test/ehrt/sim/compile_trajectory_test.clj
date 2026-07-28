(ns ehrt.sim.compile-trajectory-test
  "Red tests for CompileTrajectory (M5b Task 3, docs/gmf-interpreter.md
  section 1's per-state-type mapping table and section 6's own build-
  session test obligations) -- written before ehrt.sim.compile-
  trajectory exists (ADR-0004 test-first). Pure, RNG-free: every value
  CompileTrajectory ever touches was already sampled by the GMF
  interpreter (M5a); this stage only re-shapes already-decided content
  into pathway IR.

  Laws under test: clinical-content preservation (every non-internal
  trajectory event maps to >=1 IR artifact -- a step, a condition
  annotation, or a registration-time fact -- none dropped without a
  documented reason, none invented, clinical order preserved);
  provenance (every compiled step cites the trajectory event it
  realizes, the third link of the module-state -> trajectory-event ->
  IR-step glass-box chain); the day -> minutes conversion, at exactly
  one point (the durations rule's own day clause, docs/patient-state-
  model.md)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim.compile-trajectory :as ct]
            [ehrt.sim.config :as config]
            [ehrt.sim.gmf :as gmf]
            [ehrt.sim.gmf-interpreter :as interp]
            [ehrt.sim.pathway :as pathway]
            [ehrt.sim.persona :as persona])
  (:import [java.util Random]))

(def ^:private facility config/default-facility)

(defn- ev
  "Test-fixture convenience: a trajectory event, defaults filled in."
  [event overrides]
  (merge {:module "m" :state :s :t 0 :event event :pre-horizon false} overrides))

;; --- Encounter-class mapping (docs/gmf-interpreter.md section 4) ---------

(deftest ambulatory-and-wellness-encounters-compile-to-outpatient-visit
  (doseq [class [:ambulatory :wellness]]
    (let [trajectory [(ev :encounter {:t 100 :encounter-class class
                                      :codes [{:system :snomed :code "185345009" :display "Encounter for symptom"}]})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (= :outpatient-visit (:type (first steps))))
      (is (= {:module "m" :state :s} (:citation (first steps)))))))

(deftest emergency-encounters-compile-to-admission-targeting-an-ed-ward
  (let [trajectory [(ev :encounter {:t 100 :encounter-class :emergency
                                    :codes [{:system :snomed :code "50849002" :display "ED"}]})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= :admission (:type (first steps))))
    (is (= "Emergency" (:location (first steps))))))

(deftest inpatient-encounters-compile-to-admission-targeting-an-inpatient-ward
  (let [trajectory [(ev :encounter {:t 100 :encounter-class :inpatient
                                    :codes [{:system :snomed :code "32485007" :display "Hospital admission"}]})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= :admission (:type (first steps))))
    (is (contains? #{"Renal" "Cardiology"} (:location (first steps))))))

(deftest encounter-end-mirrors-its-opening-encounters-class
  (testing "outpatient-opened -> :outpatient-visit-end"
    (let [trajectory [(ev :encounter {:t 100 :encounter-class :ambulatory :codes []})
                      (ev :encounter-end {:t 110 :references 0})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (= [:outpatient-visit :delay :outpatient-visit-end] (mapv :type steps)))))
  (testing "inpatient-opened -> :discharge"
    (let [trajectory [(ev :encounter {:t 100 :encounter-class :inpatient :codes []})
                      (ev :encounter-end {:t 110 :references 0})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (= [:admission :delay :discharge] (mapv :type steps))))))

(deftest a-second-same-episode-encounter-after-the-first-encounter-end-is-dropped
  (testing "M7 finding (docs/gmf-interpreter.md's own M7 survey section,
            confirmed here against synthetic events rather than only
            reasoned about): `encounter-closed?` is a single boolean set by
            the FIRST :encounter-end and never cleared -- it was built to
            stop a module recurring across a patient's WHOLE LIFE
            (sinusitis.json's own Potential_Onset loop, M5b) from minting a
            second admission, but it fires identically for a SAME-EPISODE
            transfer to a different care setting (appendicitis.json's own
            real shape: an ED encounter closes, an inpatient surgical
            encounter opens immediately after, zero elapsed time) -- the
            second encounter, and everything compiled from it, is silently
            dropped. This is the real gap standing between
            appendicitis.json vendoring cleanly (Task 2) and a demo that
            shows its own surgery (Task 3) -- not a throw, not a load
            rejection, a genuine compile-time content-completeness gap,
            recorded here as a permanent regression witness, not merely a
            one-off finding."
    (let [trajectory [(ev :encounter {:t 100 :encounter-class :emergency
                                      :codes [{:system :snomed :code "50849002" :display "ED admission"}]})
                      (ev :encounter-end {:t 110 :references 0})
                      (ev :encounter {:t 110 :encounter-class :inpatient
                                      :codes [{:system :snomed :code "185347001" :display "Encounter for problem"}]})
                      (ev :procedure {:t 111 :codes [{:system :snomed :code "80146002" :display "Excision of appendix"}]})
                      (ev :encounter-end {:t 200 :references 2})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (= [:admission :delay :discharge] (mapv :type steps))
          "the inpatient admission, the appendectomy procedure, and the second discharge are all missing -- exactly the drop this test exists to witness"))))

;; --- Standalone clinical step types ----------------------------------------

(deftest procedure-compiles-to-a-procedure-step-with-codes-and-citation
  (let [codes [{:system :snomed :code "112790001" :display "Nasal sinus endoscopy"}]
        trajectory [(ev :procedure {:t 100 :codes codes})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= [{:type :procedure :codes codes :citation {:module "m" :state :s}}] steps))))

(deftest observation-compiles-with-its-sampled-value-and-unit
  (let [codes [{:system :loinc :code "8310-5" :display "Body temperature"}]
        trajectory [(ev :observation {:t 100 :codes codes :value 38.4 :unit "Cel"})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= :observation (:type (first steps))))
    (is (= 38.4 (:value (first steps))))
    (is (= "Cel" (:unit (first steps))))))

(deftest observation-with-no-sampled-value-omits-it
  (let [trajectory [(ev :observation {:t 100 :codes []})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (not (contains? (first steps) :value)))))

(deftest medication-order-then-end-carries-a-matching-order-citation
  (let [codes [{:system :rxnorm :code "308191" :display "Amoxicillin"}]
        trajectory [(ev :medication-order {:t 100 :state :rx :codes codes})
                    (ev :medication-end {:t 110 :state :end-rx :references 0})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)
        order-step (first (filter #(= :medication-order (:type %)) steps))
        end-step (first (filter #(= :medication-end (:type %)) steps))]
    (is (= (:citation order-step) (:order-citation end-step)))))

;; --- Condition annotation (docs/gmf-interpreter.md section 1's own
;; "annotation on the enclosing Encounter-mapped step" ruling) --------------

(deftest condition-onset-annotates-the-most-recently-compiled-encounter-step
  (let [codes [{:system :snomed :code "36971009" :display "Sinusitis"}]
        trajectory [(ev :encounter {:t 100 :state :visit :encounter-class :ambulatory :codes []})
                    (ev :condition-onset {:t 100 :state :onset :codes codes})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)
        visit-step (first (filter #(= :outpatient-visit (:type %)) steps))]
    (is (= 1 (count (:conditions visit-step))))
    (is (= codes (:codes (first (:conditions visit-step)))))
    (is (= :condition-onset (:event (first (:conditions visit-step)))))
    (is (= {:module "m" :state :onset} (:citation (first (:conditions visit-step)))))))

(deftest condition-onset-with-no-prior-encounter-is-dropped-not-invented
  (testing "log-only fact, the same shape :step-rejected already
            established (docs/gmf-interpreter.md section 1) -- no step,
            no crash, no fabricated attachment point"
    (let [trajectory [(ev :condition-onset {:t 100 :codes []})]
          {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
      (is (empty? steps)))))

;; --- Pre-horizon handling: the day -> seconds/minutes boundary is where
;; ratified item 5 (registration-time facts) actually lands -------------

(deftest pre-horizon-encounter-and-procedure-and-observation-are-dropped
  (testing "no operational trajectory event for the encounter machinery
            during history (docs/gmf-interpreter.md section 3) -- enforced
            HERE, since the M5a interpreter itself does not discriminate"
    (let [trajectory [(ev :encounter {:t 10 :pre-horizon true :encounter-class :ambulatory :codes []})
                      (ev :procedure {:t 11 :pre-horizon true :codes []})
                      (ev :observation {:t 12 :pre-horizon true :codes []})]
          {:keys [steps registration-facts]} (ct/compile-trajectory trajectory facility 100)]
      (is (empty? steps))
      (is (empty? registration-facts)))))

(deftest pre-horizon-condition-and-medication-facts-become-registration-facts
  (let [onset-codes [{:system :snomed :code "36971009" :display "Sinusitis"}]
        med-codes [{:system :rxnorm :code "308191" :display "Amoxicillin"}]
        trajectory [(ev :condition-onset {:t 10 :pre-horizon true :state :onset :codes onset-codes})
                    (ev :medication-order {:t 20 :pre-horizon true :state :rx :codes med-codes})]
        {:keys [steps registration-facts]} (ct/compile-trajectory trajectory facility 100)]
    (is (empty? steps) "never pathway IR -- these ride :registered instead")
    (is (= 2 (count registration-facts)))
    (is (= #{:condition-onset :medication-order} (into #{} (map :event) registration-facts)))
    (is (every? :citation registration-facts))))

;; --- The day -> minutes conversion, exactly once (durations rule) ---------

(deftest the-gap-between-registration-and-the-first-horizon-event-becomes-a-delay-in-minutes
  (let [trajectory [(ev :procedure {:t 105 :codes []})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= [{:type :delay :from 7200 :to 7200} {:type :procedure :codes [] :citation {:module "m" :state :s}}]
           steps)
        "5 days * 1440 minutes/day = 7200")))

(deftest no-delay-step-when-the-gap-is-zero
  (let [trajectory [(ev :procedure {:t 100 :codes []})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= 1 (count steps)))
    (is (= :procedure (:type (first steps))))))

(deftest gaps-between-consecutive-compiled-steps-also-become-delays
  (let [trajectory [(ev :procedure {:t 100 :state :p1 :codes []})
                    (ev :procedure {:t 103 :state :p2 :codes []})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (= [:procedure :delay :procedure] (mapv :type steps)))
    (is (= 4320 (:from (second steps))) "3 days * 1440")))

;; --- Every compiled step validates as real pathway IR ---------------------

(deftest every-compiled-pathway-is-valid-ir
  (let [trajectory [(ev :encounter {:t 100 :state :visit :encounter-class :ambulatory
                                    :codes [{:system :snomed :code "185345009" :display "Encounter for symptom"}]})
                    (ev :observation {:t 101 :codes [{:system :loinc :code "8310-5" :display "Temp"}] :value 38.0})
                    (ev :medication-order {:t 102 :state :rx :codes [{:system :rxnorm :code "308191" :display "Amox"}]})
                    (ev :medication-end {:t 103 :state :end-rx :references 2})
                    (ev :encounter-end {:t 104 :references 0})]
        {:keys [steps]} (ct/compile-trajectory trajectory facility 100)]
    (is (pathway/valid? {:name "compiled" :steps steps}))))

;; --- End-to-end property tests, against the REAL interpreter output ------

(def fixture-clinic
  (:payload (gmf/load-module "fixture-clinic" (slurp (io/resource "ehrt/sim/fixtures/fixture-clinic.json")))))

(def sinusitis
  (:payload (gmf/load-module "sinusitis" (slurp (io/resource "sim/modules/sinusitis.json")))))

(defn- adult [seed] (assoc (persona/persona (Random. seed) {}) :sex :female))
(defn- registration-t-for [p] (+ (interp/dob-epoch-day p) (* 365 20)))

(defn- accounted-for?
  "Every trajectory event is either (a) a horizon-phase event that
  produced a real compiled step, (b) a condition-onset/end that landed as
  an annotation OR was legitimately dropped (no open encounter), (c) a
  pre-horizon event dropped or captured as a registration-fact, or (d)
  everything from the FIRST horizon-phase :encounter-end onward -- this
  project's own single-encounter-horizon scope (ADR-0007 point 3) -- per
  this session's own resolution -- never silently unaccounted for."
  [trajectory {:keys [steps registration-facts]}]
  (let [step-citations (into #{} (keep :citation) steps)
        annotation-citations (into #{} (comp (keep :conditions) cat (map :citation)) steps)
        registration-citations (into #{} (map :citation) registration-facts)
        first-close-idx (first (keep-indexed (fn [i ev] (when (and (not (:pre-horizon ev)) (= :encounter-end (:event ev))) i))
                                             trajectory))]
    (every? (fn [[i event]]
              (let [c {:module (:module event) :state (:state event)}]
                (or (contains? step-citations c)
                    (contains? annotation-citations c)
                    (contains? registration-citations c)
                    ;; dropped, documented: pre-horizon encounter-machinery,
                    ;; a condition with no open encounter to attach to, or
                    ;; past the first encounter this run's own scope closed.
                    (:pre-horizon event)
                    (#{:condition-onset :condition-end} (:event event))
                    (and first-close-idx (> i first-close-idx)))))
            (map-indexed vector trajectory))))

(defspec clinical-content-preservation-fixture-clinic 150
  (prop/for-all [seed gen/large-integer]
    (let [p (adult seed)
          reg-t (registration-t-for p)
          {:keys [trajectory]} (interp/run-module fixture-clinic (Random. seed) p reg-t)
          compiled (ct/compile-trajectory trajectory config/default-facility reg-t)]
      (accounted-for? trajectory compiled))))

(defspec clinical-content-preservation-sinusitis 150
  (prop/for-all [seed gen/large-integer]
    (let [p (adult seed)
          reg-t (registration-t-for p)
          {:keys [trajectory]} (interp/run-module sinusitis (Random. seed) p reg-t (+ reg-t 90))]
      (accounted-for? trajectory (ct/compile-trajectory trajectory config/default-facility reg-t)))))

(defspec every-compiled-step-cites-a-real-trajectory-event
  150
  (prop/for-all [seed gen/large-integer]
    (let [p (adult seed)
          reg-t (registration-t-for p)
          {:keys [trajectory]} (interp/run-module fixture-clinic (Random. seed) p reg-t)
          {:keys [steps]} (ct/compile-trajectory trajectory config/default-facility reg-t)
          trajectory-citations (into #{} (map (fn [e] {:module (:module e) :state (:state e)})) trajectory)]
      (every? (fn [step] (or (nil? (:citation step)) (contains? trajectory-citations (:citation step)))) steps))))

(defspec compiled-pathway-is-always-valid-ir 150
  (prop/for-all [seed gen/large-integer]
    (let [p (adult seed)
          reg-t (registration-t-for p)
          {:keys [trajectory]} (interp/run-module sinusitis (Random. seed) p reg-t (+ reg-t 90))
          {:keys [steps]} (ct/compile-trajectory trajectory config/default-facility reg-t)]
      (pathway/valid? {:name "compiled" :steps steps}))))
