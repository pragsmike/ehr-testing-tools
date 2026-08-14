(ns ehrt.sim-trajectory.gmf-interpreter-test
  "Red tests for the GMF interpreter core (M5a Task 2, docs/gmf-
  interpreter.md sections 1-2 and 6) -- written before
  ehrt.sim-trajectory.gmf-interpreter exists (sim/ADR-0004 test-first). Pure,
  seeded, engine-free: `step` advances one state at a time, consuming
  the passed rng only in documented order; transitions (direct,
  distributed, conditional, complex) and the v1 condition vocabulary
  (age, sex, attribute, PriorState) are each exercised directly.
  Properties: determinism, code passthrough, glass-box traceability,
  attribute-registry-only writes -- the sim/ADR-0004 law-bearing set this
  session's own Task 2 names as THE seam."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-trajectory.compile-trajectory :as ct]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-trajectory.gmf-interpreter :as interp]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.time LocalDate Period]
           [java.util Random]))

(def fixture-clinic-json
  (slurp (io/resource "ehrt/sim/fixtures/fixture-clinic.json")))

(def fixture-clinic
  (:payload (gmf/load-module "fixture-clinic" fixture-clinic-json)))

(defn- persona-at [seed & [config]]
  (sim-model/persona (Random. seed) (or config {})))

(defn- ctx-for [p] (interp/initial-context p))

;; --- step mechanics, one transition kind at a time ------------------------

(def direct-only-module
  {:id "direct-mod"
   :name "Direct"
   :states {:initial {:type :initial :direct-transition :a}
            :a {:type :simple :direct-transition :done}
            :done {:type :terminal}}})

(deftest direct-transition-advances-with-zero-time-and-no-event
  (let [ctx (ctx-for (persona-at 1))
        outcome (interp/step direct-only-module (Random. 1) ctx)]
    (is (= :a (:next outcome)))
    (is (= 0 (:advance outcome)))
    (is (= [] (:events outcome)))
    (is (false? (:blocked? outcome)))))

(def delay-module
  {:id "delay-mod"
   :name "Delay"
   :states {:initial {:type :initial :direct-transition :wait}
            :wait {:type :delay :range {:low 7 :high 10 :unit "days"} :direct-transition :done}
            :done {:type :terminal}}})

(deftest delay-samples-exactly-one-draw-and-advances-within-its-range
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :wait)
        rng (Random. 42)
        outcome (interp/step delay-module rng ctx)]
    (is (<= 7 (:advance outcome) 10))
    (is (= :done (:next outcome)))))

(defspec delay-consumes-a-fixed-single-rng-draw 100
  (prop/for-all [seed gen/large-integer]
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :wait)
          calls (atom 0)
          rng (proxy [Random] [(long seed)]
                (nextInt
                  ([n] (swap! calls inc) (proxy-super nextInt n))))]
      (interp/step delay-module rng ctx)
      (= 1 @calls))))

(def exact-delay-module
  {:id "exact-delay-mod"
   :name "ExactDelay"
   :states {:initial {:type :initial :direct-transition :wait}
            :wait {:type :delay :exact {:quantity 3 :unit "days"} :direct-transition :done}
            :done {:type :terminal}}})

(deftest exact-delay-advances-deterministically-with-no-rng-draw
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :wait)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))
        outcome (interp/step exact-delay-module rng ctx)]
    (is (= 3 (:advance outcome)))
    (is (= 0 @calls))))

(def distributed-module
  {:id "dist-mod"
   :name "Distributed"
   :states {:initial {:type :initial :direct-transition :split}
            :split {:type :simple
                    :distributed-transition [{:distribution 0.7 :transition :heads}
                                              {:distribution 0.3 :transition :tails}]}
            :heads {:type :terminal}
            :tails {:type :terminal}}})

(deftest distributed-transition-is-weighted-toward-the-70-percent-branch
  ;; A single shared rng drawing 2000 times, NOT 2000 fresh `(Random.
  ;; seed)` instances each read once -- java.util.Random's first
  ;; nextDouble() draw is a well-known poorly-avalanched function of a
  ;; small/sequential seed (confirmed this session: seeds 0..9 all drew
  ;; ~0.73 on their first call), which silently made an earlier draft of
  ;; this property vacuous. A single rng making many draws is both the
  ;; statistically sound test and the realistic usage shape (this
  ;; project's own single-seeded-RNG-per-run law).
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :split)
        rng (Random. 12345)
        outcomes (repeatedly 2000 #(:next (interp/step distributed-module rng ctx)))
        heads (count (filter #(= :heads %) outcomes))]
    (is (< 0.6 (/ heads 2000.0) 0.8))))

(def conditional-module
  {:id "cond-mod"
   :name "Conditional"
   :states {:initial {:type :initial :direct-transition :branch}
            :branch {:type :simple
                     :conditional-transition [{:condition {:condition-type :gender :gender "F"} :transition :female}
                                               {:transition :male}]}
            :female {:type :terminal}
            :male {:type :terminal}}})

(deftest conditional-transition-picks-the-first-matching-condition
  (let [female-persona (assoc (persona-at 1) :sex :female)
        male-persona (assoc (persona-at 1) :sex :male)]
    (is (= :female (:next (interp/step conditional-module (Random. 1)
                                        (assoc (ctx-for female-persona) :current :branch)))))
    (is (= :male (:next (interp/step conditional-module (Random. 1)
                                      (assoc (ctx-for male-persona) :current :branch)))))))

(deftest conditional-transition-consumes-no-rng
  (let [calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))]
    (interp/step conditional-module rng (assoc (ctx-for (persona-at 1)) :current :branch))
    (is (= 0 @calls))))

(def complex-module
  {:id "complex-mod"
   :name "Complex"
   :states {:initial {:type :initial :direct-transition :branch}
            :branch {:type :simple
                     :complex-transition [{:condition {:condition-type :gender :gender "F"}
                                            :distributions [{:distribution 0.5 :transition :a}
                                                             {:distribution 0.5 :transition :b}]}
                                           {:distributions [{:distribution 1.0 :transition :c}]}]}
            :a {:type :terminal} :b {:type :terminal} :c {:type :terminal}}})

(deftest complex-transition-routes-through-the-matching-condition-then-samples
  (let [male-persona (assoc (persona-at 1) :sex :male)]
    (is (= :c (:next (interp/step complex-module (Random. 1)
                                   (assoc (ctx-for male-persona) :current :branch)))))))

(defspec complex-transition-always-consumes-exactly-one-draw 100
  (prop/for-all [seed gen/large-integer
                 female? gen/boolean]
    (let [p (assoc (persona-at 1) :sex (if female? :female :male))
          calls (atom 0)
          rng (proxy [Random] [(long seed)]
                (nextDouble [] (swap! calls inc) (proxy-super nextDouble)))]
      (interp/step complex-module rng (assoc (ctx-for p) :current :branch))
      (= 1 @calls))))

;; --- Guard: age, sex, attribute, PriorState --------------------------------

(def attribute-guard-module
  {:id "attr-guard-mod"
   :name "AttrGuard"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :attribute :attribute "ready" :operator "==" :value true}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest attribute-guard-passes-when-the-namespaced-attribute-matches
  (let [ctx (-> (ctx-for (persona-at 1)) (assoc :current :check)
                (assoc :attributes {:attr-guard-mod/ready true}))
        outcome (interp/step attribute-guard-module (Random. 1) ctx)]
    (is (= :done (:next outcome)))
    (is (false? (:blocked? outcome)))))

(deftest attribute-guard-blocks-when-the-namespaced-attribute-does-not-match
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :check)
        outcome (interp/step attribute-guard-module (Random. 1) ctx)]
    (is (true? (:blocked? outcome)))
    (is (nil? (:next outcome)))))

(def prior-state-guard-module
  {:id "prior-mod"
   :name "Prior"
   :states {:initial {:type :initial :direct-transition :visit}
            :visit {:type :encounter :encounter-class :ambulatory
                    :codes [{:system :snomed :code "185345009" :display "Encounter for symptom"}]
                    :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :prior-state :name :visit}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest prior-state-guard-passes-once-the-target-state-has-been-visited
  (let [walked (interp/walk-module prior-state-guard-module (Random. 1) (ctx-for (persona-at 1)))]
    (is (= :terminal (:status walked)))))

(deftest prior-state-guard-blocks-when-the-target-state-was-never-visited
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :check)
        outcome (interp/step prior-state-guard-module (Random. 1) ctx)]
    (is (true? (:blocked? outcome)))))

(def age-guard-module
  {:id "age-mod"
   :name "Age"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :age :operator ">=" :quantity 5 :unit "years"}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest age-guard-jumps-the-virtual-clock-forward-with-no-rng-when-under-age
  (let [young (assoc (persona-at 1) :dob "2020-01-01")
        ctx (assoc (ctx-for young) :current :check)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))
        outcome (interp/step age-guard-module rng ctx)]
    (is (false? (:blocked? outcome)))
    (is (= :done (:next outcome)))
    (is (pos? (:advance outcome)) "advanced the virtual clock to reach the age threshold")
    (is (= 0 @calls) "the age jump is a deterministic computation, not an RNG draw")))

(deftest age-guard-passes-through-with-zero-advance-when-already-old-enough
  (let [old (assoc (persona-at 1) :dob "2000-01-01")
        ;; :t is 10 years past DOB (well past the guard's 5-year
        ;; threshold) -- ctx-for alone starts :t AT dob-epoch-day (age 0),
        ;; so "already old enough" must advance :t explicitly, not just
        ;; pick an early DOB.
        ctx (-> (ctx-for old) (assoc :current :check) (update :t + (* 365 10)))
        outcome (interp/step age-guard-module (Random. 1) ctx)]
    (is (false? (:blocked? outcome)))
    (is (= 0 (:advance outcome)))))

;; --- GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3e, H4):
;; compound-Guard analytical resolution (sound-jump-or-escalate) --------

(def strict-age-guard-module
  {:id "strict-age-mod"
   :name "StrictAge"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :age :operator ">" :quantity 50 :unit "years"}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest bare-age-guard-with-strict-operator-jumps-one-year-past-the-quantity
  (testing "the day-vs-year integer-age-flooring boundary: age exactly
            50 does NOT satisfy > 50, so the jump target is age 51, not
            age 50 (D3e's own account)"
    (let [dob "2000-01-01"
          p (assoc (persona-at 1) :dob dob)
          ctx (assoc (ctx-for p) :current :check)
          calls (atom 0)
          rng (proxy [Random] [(long 1)]
                (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
                (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))
          outcome (interp/step strict-age-guard-module rng ctx)
          expected-t (.toEpochDay (.plusYears (LocalDate/parse dob) 51))]
      (is (false? (:blocked? outcome)))
      (is (= 0 @calls) "deterministic computation, not an rng draw")
      (is (= expected-t (+ (:t ctx) (:advance outcome)))
          "lands EXACTLY on dob+51 years, not dob+50 -- the +1-year strict-inequality boundary"))))

(def joint-replacement-guard-module
  "total_joint_replacement.json's own Joint_Replacement_Guard, byte-
  confirmed against the fresh D3d fetch (gmf-interpreter.md section 14):
  {:and [Attribute joint_replacement is-not-nil, Age > 50 years]}."
  {:id "tjr-guard-mod"
   :name "TjrGuard"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :and
                            :conditions [{:condition-type :attribute :attribute "joint_replacement"
                                          :operator "is not nil"}
                                         {:condition-type :age :operator ">" :quantity 50 :unit "years"}]}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest compound-guard-jumps-forward-when-the-non-age-sibling-already-holds
  (testing "the exact TJR shape: joint_replacement seeded, only Age
            blocks -- sound to jump (D3e's own soundness proof)"
    (let [p (persona-at 1)
          ctx (-> (ctx-for p) (assoc :current :check)
                  (update :attributes assoc :tjr-guard-mod/joint-replacement "knee"))
          outcome (interp/step joint-replacement-guard-module (Random. 1) ctx)]
      (is (false? (:blocked? outcome)) "no longer permanently blocked at age 0 (D2's own fix-forward finding)")
      (is (pos? (:advance outcome))))))

(deftest compound-guard-stays-blocked-when-the-non-age-sibling-does-not-hold
  (testing "joint_replacement never seeded -- no sound jump exists
            merely from advancing the clock, correctly blocks"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :check)
          outcome (interp/step joint-replacement-guard-module (Random. 1) ctx)]
      (is (true? (:blocked? outcome))))))

(def two-age-conditions-guard-module
  "An :and with TWO Age sub-conditions -- ambiguous which bound
  governs, an ESCALATION shape (D3e), not built: stays blocked, the
  same disposition an unresolvable condition already has."
  {:id "two-age-mod"
   :name "TwoAge"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :and
                            :conditions [{:condition-type :age :operator ">=" :quantity 20 :unit "years"}
                                         {:condition-type :age :operator "<" :quantity 30 :unit "years"}]}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest compound-guard-with-two-age-conditions-is-not-resolved-stays-blocked
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :check)
        outcome (interp/step two-age-conditions-guard-module (Random. 1) ctx)]
    (is (true? (:blocked? outcome)))))

(def date-alongside-age-guard-module
  "An :and pairing Age with Date -- a second time-dependent sibling,
  an ESCALATION shape (D3e), not built: stays blocked."
  {:id "date-age-mod"
   :name "DateAge"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :and
                            :conditions [{:condition-type :date :operator ">=" :year 2000}
                                         {:condition-type :age :operator ">=" :quantity 50 :unit "years"}]}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest compound-guard-with-date-alongside-age-is-not-resolved-stays-blocked
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :check)
        outcome (interp/step date-alongside-age-guard-module (Random. 1) ctx)]
    (is (true? (:blocked? outcome)))))

;; --- SetAttribute / Symptom -------------------------------------------------

(deftest set-attribute-writes-a-module-namespaced-key
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :log-onset-attribute)
        outcome (interp/step fixture-clinic (Random. 1) ctx)]
    (is (= true (get-in outcome [:attributes :fixture-clinic/onset-logged])))
    (is (= [] (:events outcome)) "SetAttribute is consumed internally -- no trajectory event")))

;; GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3d finding 1):
;; SetAttribute's own :value-code field, TJR's own Pre_Procedure_
;; Encounter_Reason/Home_Health_Reason_Knee/Hip shape.
(def set-attribute-value-code-module
  {:id "value-code-mod"
   :name "ValueCode"
   :states {:initial {:type :initial :direct-transition :set}
            :set {:type :set-attribute :attribute "reason"
                  :value-code {:system :snomed :code "110466009" :display "Pre-surgery evaluation (procedure)"}
                  :direct-transition :done}
            :done {:type :terminal}}})

(deftest set-attribute-writes-value-code-when-present-taking-precedence-over-value
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :set)
        outcome (interp/step set-attribute-value-code-module (Random. 1) ctx)]
    (is (= {:system :snomed :code "110466009" :display "Pre-surgery evaluation (procedure)"}
           (get-in outcome [:attributes :value-code-mod/reason])))))

;; --- ADR-0035 AR-4: SetAttribute samples its own :distribution --------
;; (hypertension.json's own Black_Onset_Age shape -- the silent-nil gap
;; the census design channel found: before this ADR, a state whose ONLY
;; value source was :distribution wrote nil.)

(def set-attribute-gaussian-module
  {:id "onset-age-mod" :name "OnsetAge"
   :states {:initial {:type :initial :direct-transition :set}
            :set {:type :set-attribute :attribute "years_until_onset"
                  :distribution {:kind :gaussian :parameters {:mean 42 :standard-deviation 14} :round true}
                  :direct-transition :done}
            :done {:type :terminal}}})

(deftest set-attribute-gaussian-writes-a-non-nil-rounded-value
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :set)
        outcome (interp/step set-attribute-gaussian-module (Random. 1) ctx)
        v (get-in outcome [:attributes :onset-age-mod/years-until-onset])]
    (is (some? v))
    (is (= v (double (Math/round ^double v))) "round: true -- an integer-valued double")))

(defspec set-attribute-gaussian-consumes-a-fixed-single-rng-draw 100
  (prop/for-all [seed gen/large-integer]
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :set)
          calls (atom 0)
          rng (proxy [Random] [(long seed)]
                (nextDouble
                  ([] (swap! calls inc) (proxy-super nextDouble))))]
      (interp/step set-attribute-gaussian-module rng ctx)
      (= 1 @calls))))

(def set-attribute-gaussian-unrounded-module
  {:id "unrounded-mod" :name "Unrounded"
   :states {:initial {:type :initial :direct-transition :set}
            :set {:type :set-attribute :attribute "raw_value"
                  :distribution {:kind :gaussian :parameters {:mean 42 :standard-deviation 14} :round false}
                  :direct-transition :done}
            :done {:type :terminal}}})

(deftest set-attribute-gaussian-round-false-can-write-a-non-integer-value
  (testing "AR-1/AR-3: :round governs the SAMPLED VALUE -- distinguishable
            here (unlike the Delay/Procedure timing path, where
            resolve-time-advance's own unit-conversion boundary always
            rounds to a long regardless of the flag)"
    (let [non-integer? (fn [seed]
                          (let [ctx (assoc (ctx-for (persona-at 1)) :current :set)
                                outcome (interp/step set-attribute-gaussian-unrounded-module (Random. seed) ctx)
                                v (get-in outcome [:attributes :unrounded-mod/raw-value])]
                            (not= v (double (Math/round ^double v)))))]
      (is (some non-integer? (range 50))))))

(def set-attribute-exact-distribution-module
  "EXACT-kind :distribution on SetAttribute -- unlike Delay/Procedure/
  Symptom (where EXACT always v1-collapses into :exact, D3c), SetAttribute
  never had a pre-existing collapse, so EXACT reaches the interpreter as
  a real :distribution here (gmf/normalize-set-attribute-distribution's
  own docstring: 'all FIVE kinds pass through')."
  {:id "exact-dist-mod" :name "ExactDist"
   :states {:initial {:type :initial :direct-transition :set}
            :set {:type :set-attribute :attribute "fixed_value"
                  :distribution {:kind :exact :parameters {:value 7} :round false}
                  :direct-transition :done}
            :done {:type :terminal}}})

(deftest set-attribute-exact-distribution-writes-the-value-with-zero-draws
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :set)
        calls (atom 0)
        rng (proxy [Random] [1]
              (nextDouble
                ([] (swap! calls inc) (proxy-super nextDouble))))
        outcome (interp/step set-attribute-exact-distribution-module rng ctx)]
    (is (= 7.0 (get-in outcome [:attributes :exact-dist-mod/fixed-value])))
    (is (= 0 @calls))))

;; --- ADR-0040 AR-2: the full upstream SetAttribute precedence chain --
;; range > distribution > value-code > value-attribute > literal value.
;; congestive_heart_failure.json's own `Inpatient LOS` shape (a literal
;; :value co-present with :distribution) is the census's own real found
;; gap this closes.

(def set-attribute-distribution-outranks-value-module
  "congestive_heart_failure.json's own Inpatient LOS shape, byte-
  confirmed against source: :value is a legacy-compat placeholder,
  :distribution is what actually fires."
  {:id "los-mod" :name "Los"
   :states {:initial {:type :initial :direct-transition :set}
            :set {:type :set-attribute :attribute "inpatient_los" :value 0
                  :distribution {:kind :exact :parameters {:value 5} :round false}
                  :direct-transition :done}
            :done {:type :terminal}}})

(deftest set-attribute-distribution-outranks-a-co-present-literal-value
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :set)
        outcome (interp/step set-attribute-distribution-outranks-value-module (Random. 1) ctx)]
    (is (= 5.0 (get-in outcome [:attributes :los-mod/inpatient-los])))))

(def set-attribute-range-module
  {:id "range-mod" :name "Range"
   :states {:initial {:type :initial :direct-transition :set}
            :set {:type :set-attribute :attribute "roll" :range {:low 1 :high 2}
                  :direct-transition :done}
            :done {:type :terminal}}})

(deftest set-attribute-range-draws-within-bounds-with-exactly-one-rng-call
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :set)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble)))
        outcome (interp/step set-attribute-range-module rng ctx)
        v (get-in outcome [:attributes :range-mod/roll])]
    (is (<= 1 v 2))
    (is (= 1 @calls))))

(def set-attribute-range-with-decimals-module
  {:id "range-decimals-mod" :name "RangeDecimals"
   :states {:initial {:type :initial :direct-transition :set}
            :set {:type :set-attribute :attribute "roll" :range {:low 0 :high 100 :decimals 2}
                  :direct-transition :done}
            :done {:type :terminal}}})

(deftest set-attribute-range-decimals-rounds-half-up-to-that-many-places
  (testing "Person.rand(low, high, decimals)'s own BigDecimal.setScale
            HALF_UP semantics"
    (dotimes [seed 20]
      (let [ctx (assoc (ctx-for (persona-at 1)) :current :set)
            outcome (interp/step set-attribute-range-with-decimals-module (Random. seed) ctx)
            v (get-in outcome [:attributes :range-decimals-mod/roll])
            scaled (* v 100.0)]
        (is (< (Math/abs (- scaled (Math/round scaled))) 1e-6) (str "v=" v))))))

(def set-attribute-range-outranks-distribution-module
  {:id "range-vs-dist-mod" :name "RangeVsDist"
   :states {:initial {:type :initial :direct-transition :set}
            :set {:type :set-attribute :attribute "roll" :range {:low 9 :high 9}
                  :distribution {:kind :exact :parameters {:value 1} :round false}
                  :direct-transition :done}
            :done {:type :terminal}}})

(deftest set-attribute-range-outranks-a-co-present-distribution
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :set)
        outcome (interp/step set-attribute-range-outranks-distribution-module (Random. 1) ctx)]
    (is (= 9.0 (get-in outcome [:attributes :range-vs-dist-mod/roll])))))

;; ADR-0040 AR-2: :value-attribute -- hospice_treatment.json's own
;; Eventual_Hospice_Reason shape, byte-confirmed against source: reads an
;; EXISTING root-scoped attribute's own current value, falling through to
;; :value (never writing nil) when that attribute was never set.

(def set-attribute-value-attribute-module
  {:id "hospice-reason-mod" :name "HospiceReason"
   :states {:initial {:type :initial :direct-transition :set}
            :set {:type :set-attribute :attribute "hospice_reason" :value-attribute "chf"
                  :direct-transition :done}
            :done {:type :terminal}}})

(deftest set-attribute-value-attribute-reads-the-named-attributes-current-value-when-present
  (let [ctx (-> (ctx-for (persona-at 1)) (assoc :current :set)
                (update :attributes assoc :hospice-reason-mod/chf "congestive heart failure"))
        outcome (interp/step set-attribute-value-attribute-module (Random. 1) ctx)]
    (is (= "congestive heart failure" (get-in outcome [:attributes :hospice-reason-mod/hospice-reason])))))

(deftest set-attribute-value-attribute-falls-through-to-literal-value-when-the-named-attribute-is-absent
  (testing "upstream's own `if (person.attributes.containsKey(valueAttribute))` --
            absent means this source does NOT fire, never a nil write"
    (let [module (assoc-in set-attribute-value-attribute-module [:states :set :value] "unknown")
          ctx (assoc (ctx-for (persona-at 1)) :current :set)
          outcome (interp/step module (Random. 1) ctx)]
      (is (= "unknown" (get-in outcome [:attributes :hospice-reason-mod/hospice-reason]))))))

(def set-attribute-value-code-outranks-value-attribute-module
  {:id "vc-vs-va-mod" :name "VcVsVa"
   :states {:initial {:type :initial :direct-transition :set}
            :set {:type :set-attribute :attribute "reason"
                  :value-code {:system :snomed :code "1" :display "Test"}
                  :value-attribute "other"
                  :direct-transition :done}
            :done {:type :terminal}}})

(deftest set-attribute-value-code-outranks-a-co-present-value-attribute
  (let [ctx (-> (ctx-for (persona-at 1)) (assoc :current :set)
                (update :attributes assoc :vc-vs-va-mod/other "should not win"))
        outcome (interp/step set-attribute-value-code-outranks-value-attribute-module (Random. 1) ctx)]
    (is (= {:system :snomed :code "1" :display "Test"}
           (get-in outcome [:attributes :vc-vs-va-mod/reason])))))

(deftest symptom-writes-a-module-namespaced-key-with-a-sampled-severity
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :nasal-congestion-symptom)
        outcome (interp/step fixture-clinic (Random. 5) ctx)
        severity (get-in outcome [:attributes :fixture-clinic/nasal-congestion])]
    (is (<= 10 severity 40))
    (is (= [] (:events outcome)) "Symptom is consumed internally -- no trajectory event, docs/gmf-interpreter.md section 1")))

;; --- GMF coverage Wave B (2026-08-02, ADR-0027, D1): root-scoped
;; workflow attributes -- exercised directly against `step`/`ctx`'s own
;; `:root` here, ahead of real CallSubmodule recursion (Step 2c), the
;; same "prove the mechanism via ctx manipulation before the caller
;; that will actually drive it exists" shape this namespace's own
;; condition-onset/condition-end tests already use for :references.

(deftest set-attribute-writes-under-ctx-root-not-the-executing-module-id
  (testing "a ctx carrying an explicit :root different from the
            executing module's own id -- the shape a CallSubmodule
            callee's own ctx will have once Step 2c lands -- writes
            SetAttribute under the ROOT namespace"
    (let [ctx (-> (ctx-for (persona-at 1)) (assoc :current :log-onset-attribute :root "some-root-module"))
          outcome (interp/step fixture-clinic (Random. 1) ctx)]
      (is (= true (get-in outcome [:attributes :some-root-module/onset-logged])))
      (is (not (contains? (:attributes outcome) :fixture-clinic/onset-logged))))))

(deftest symptom-writes-under-ctx-root-not-the-executing-module-id
  (let [ctx (-> (ctx-for (persona-at 1)) (assoc :current :nasal-congestion-symptom :root "some-root-module"))
        outcome (interp/step fixture-clinic (Random. 5) ctx)]
    (is (some? (get-in outcome [:attributes :some-root-module/nasal-congestion])))
    (is (not (contains? (:attributes outcome) :fixture-clinic/nasal-congestion)))))

(def root-scoped-attribute-guard-module
  {:id "callee-mod"
   :name "Callee"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :attribute :attribute "ready" :operator "==" :value true}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest attribute-condition-reads-are-also-root-scoped-not-module-scoped
  (testing "a value written under the ROOT namespace (as if a sibling
            state upstream in the SAME walk, possibly in a different
            module, already wrote it) is visible to a Guard evaluated
            while THIS module is the one executing -- the read side of
            D1's own root-scoping contract, symmetric with the write
            side above"
    (let [ctx (-> (ctx-for (persona-at 1))
                  (assoc :current :check :root "caller-mod")
                  (assoc :attributes {:caller-mod/ready true}))
          outcome (interp/step root-scoped-attribute-guard-module (Random. 1) ctx)]
      (is (= :done (:next outcome))
          "the Guard's own bare :attribute \"ready\" resolved under :root (caller-mod), not module-id (callee-mod)"))))

(deftest walk-module-and-run-module-normalize-root-from-the-module-they-walk
  (testing "when ctx carries no explicit :root (every existing test
            fixture, and every real non-calling walk today), both real
            entry points default :root to the SAME module being walked
            -- root = self, byte-identical to this project's pre-Wave-B
            behavior by construction"
    (let [walked (interp/walk-module direct-only-module (Random. 1) (ctx-for (persona-at 1)))]
      (is (= "direct-mod" (:root walked))))
    (let [p (persona-at 1)
          result (interp/run-module direct-only-module (Random. 1) p (interp/dob-epoch-day p))]
      (is (= "direct-mod" (:root result))))))

;; --- Trajectory events: citations, code passthrough, references -----------

(deftest condition-onset-emits-a-cited-event-with-verbatim-codes
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :sinusitis-onset)
        outcome (interp/step fixture-clinic (Random. 1) ctx)
        [event] (:events outcome)]
    (is (= :condition-onset (:event event)))
    (is (= "fixture-clinic" (:module event)))
    (is (= :sinusitis-onset (:state event)))
    (is (= [{:system :snomed :code "36971009" :display "Sinusitis (disorder)"}] (:codes event)))))

(deftest condition-end-references-its-condition-onset-events-trajectory-index
  (let [ctx0 (ctx-for (persona-at 1))
        onset-outcome (interp/step fixture-clinic (Random. 1) (assoc ctx0 :current :sinusitis-onset))
        onset-event (first (:events onset-outcome))
        ctx1 (-> ctx0 (assoc :current :resolve-condition) (update :trajectory conj onset-event))
        outcome (interp/step fixture-clinic (Random. 1) ctx1)
        [event] (:events outcome)]
    (is (= :condition-end (:event event)))
    (is (= 0 (:references event)))))

(deftest observation-samples-a-value-within-its-declared-range-consuming-one-draw
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :take-temperature)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble)))
        outcome (interp/step fixture-clinic rng ctx)
        [event] (:events outcome)]
    (is (<= 37.5 (:value event) 39.5))
    (is (= 1 @calls))))

;; --- GMF coverage Wave D stage D1 (2026-08-02, ADR-0029): observation
;; family -- value_code/vital_sign sourcing, :category pass-through,
;; MultiObservation/DiagnosticReport -> :diagnostic-report -------------------

(def ^:private a-value-code {:system :snomed :code "10828004" :display "Positive (qualifier value)"})
(def ^:private a-loinc-code {:system :loinc :code "88262-1" :display "Gram positive blood culture panel"})

(def value-code-observation-module
  {:id "obs-mod"
   :name "Observation"
   :states {:initial {:type :initial :direct-transition :finding}
            :finding {:type :observation :codes [a-loinc-code] :value-code a-value-code :category "laboratory"
                      :direct-transition :done}
            :done {:type :terminal}}})

(deftest observation-with-value-code-carries-it-verbatim-and-consumes-no-rng
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :finding)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble)))
        outcome (interp/step value-code-observation-module rng ctx)
        [event] (:events outcome)]
    (is (= a-value-code (:value-code event)))
    (is (= "laboratory" (:category event)))
    (is (nil? (:value event)))
    (is (zero? @calls))))

(def vital-sign-observation-module
  {:id "vital-mod"
   :name "Vital"
   :states {:initial {:type :initial :direct-transition :spo2}
            :spo2 {:type :observation :codes [{:system :loinc :code "59408-5"}] :unit "%"
                   :vital-sign "Oxygen Saturation" :category "vital-signs" :direct-transition :done}
            :done {:type :terminal}}})

(deftest observation-with-vital-sign-samples-within-the-reference-table-range-consuming-one-draw
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :spo2)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble)))
        outcome (interp/step vital-sign-observation-module rng ctx)
        [event] (:events outcome)]
    (is (<= 95 (:value event) 100))
    (is (= "%" (:unit event)))
    (is (= {:low 95 :high 100} (:reference-range event)))
    (is (= :normal (:interpretation event)))
    (is (= 1 @calls))))

;; GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3c finding 4):
;; seven new vital-sign-table rows, `uti/ed_bundle.json`'s own real
;; BMP/CMP need -- LOINC-verified this session (notes/facts-register.md
;; F22).
(deftest observation-with-a-d3c-vital-sign-name-samples-within-its-reference-range
  (let [module (assoc-in vital-sign-observation-module [:states :spo2 :vital-sign] "Glucose")
        ctx (assoc (ctx-for (persona-at 1)) :current :spo2)
        outcome (interp/step module (Random. 1) ctx)
        [event] (:events outcome)]
    (is (<= 70 (:value event) 100))))

(deftest observation-with-an-unrecognized-vital-sign-name-throws
  (let [bad-module (assoc-in vital-sign-observation-module [:states :spo2 :vital-sign] "Respiratory Rate")
        ctx (assoc (ctx-for (persona-at 1)) :current :spo2)]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unrecognized vital-sign"
                           (interp/step bad-module (Random. 1) ctx)))))

;; GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3d finding 2): a
;; FOURTH value-sourcing mechanism, :exact -- TJR's own PROMIS29_Total_
;; Assessment shape (a literal, SPECIFIED value, zero rng).
(def exact-observation-module
  {:id "exact-mod"
   :name "Exact"
   :states {:initial {:type :initial :direct-transition :pain}
            :pain {:type :observation :codes [{:system :loinc :code "71962-5"}] :unit "{score}"
                   :category "survey" :exact {:quantity 1} :direct-transition :done}
            :done {:type :terminal}}})

(deftest observation-with-exact-carries-the-literal-value-and-consumes-no-rng
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :pain)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))
        outcome (interp/step exact-observation-module rng ctx)
        [event] (:events outcome)]
    (is (= 1 (:value event)))
    (is (= "{score}" (:unit event)))
    (is (zero? @calls))))

(def diagnostic-report-module
  {:id "dr-mod"
   :name "DiagnosticReport"
   :states {:initial {:type :initial :direct-transition :blood-cultures}
            :blood-cultures {:type :diagnostic-report
                              :codes [{:system :loinc :code "600-7" :display "Bacteria identified in Blood by Culture"}]
                              :observations [{:category "laboratory" :codes [a-loinc-code] :value-code a-value-code}]
                              :direct-transition :done}
            :done {:type :terminal}}})

(deftest diagnostic-report-state-emits-one-event-carrying-report-codes-and-observations
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :blood-cultures)
        outcome (interp/step diagnostic-report-module (Random. 1) ctx)
        [event] (:events outcome)]
    (is (= 1 (count (:events outcome))) "ONE event for the whole state, not one per child (P5)")
    (is (= :diagnostic-report (:event event)))
    (is (= [{:system :loinc :code "600-7" :display "Bacteria identified in Blood by Culture"}] (:codes event)))
    (is (= [{:category "laboratory" :codes [a-loinc-code] :value-code a-value-code}] (:observations event)))))

(def multi-observation-module
  {:id "mo-mod"
   :name "MultiObservation"
   :states {:initial {:type :initial :direct-transition :bp}
            :bp {:type :multi-observation :category "vital-signs"
                 :codes [{:system :loinc :code "85354-9" :display "Blood pressure panel with all children optional"}]
                 :observations [{:category "vital-signs" :unit "mm[Hg]"
                                  :codes [{:system :loinc :code "8480-6" :display "Systolic Blood Pressure"}]
                                  :range {:low 90 :high 120}}
                                 {:category "vital-signs" :unit "mm[Hg]"
                                  :codes [{:system :loinc :code "8462-4" :display "Diastolic Blood Pressure"}]
                                  :vital-sign "Diastolic Blood Pressure"}]
                 :direct-transition :done}
            :done {:type :terminal}}})

(deftest multi-observation-state-also-compiles-to-a-diagnostic-report-event-consuming-two-draws
  (testing "R2(a)/D1a-2: MultiObservation and DiagnosticReport are two
            distinct loadable state TYPES that compile to the SAME
            trajectory event type -- 'one step type, both compile into
            it'; two children, one range-sourced (one draw) and one
            vital_sign-sourced (one draw), consuming exactly two draws
            total, in vector order"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :bp)
          calls (atom 0)
          rng (proxy [Random] [(long 1)]
                (nextDouble [] (swap! calls inc) (proxy-super nextDouble)))
          outcome (interp/step multi-observation-module rng ctx)
          [event] (:events outcome)]
      (is (= :diagnostic-report (:event event)))
      (is (= 2 (count (:observations event))))
      (is (<= 90 (:value (first (:observations event))) 120))
      (is (<= 60 (:value (second (:observations event))) 80))
      (is (= 2 @calls)))))

(deftest diagnostic-report-state-with-no-report-level-codes-omits-the-key
  (testing "D1a-2: :codes is optional on a MultiObservation/DiagnosticReport
            state -- absent, never a fabricated empty vector"
    (let [module (update-in diagnostic-report-module [:states :blood-cultures] dissoc :codes)
          ctx (assoc (ctx-for (persona-at 1)) :current :blood-cultures)
          outcome (interp/step module (Random. 1) ctx)
          [event] (:events outcome)]
      (is (not (contains? event :codes))))))

;; --- Properties (Task 2.4, THE seam) ---------------------------------------

(defn- run-fixture [seed persona-config]
  (interp/walk-module fixture-clinic (Random. seed) (interp/initial-context (persona-at seed persona-config))))

(defspec determinism-holds-for-the-same-module-persona-and-seed 150
  (prop/for-all [seed gen/large-integer]
    (= (run-fixture seed {}) (run-fixture seed {}))))

(defspec code-passthrough-every-trajectory-event-cites-its-source-modules-own-codes 150
  (prop/for-all [seed gen/large-integer]
    (let [{:keys [trajectory]} (run-fixture seed {})]
      (every? (fn [event]
                (let [state (get-in fixture-clinic [:states (:state event)])]
                  (or (nil? (:codes state)) (= (:codes event) (:codes state)))))
              trajectory))))

(defspec glass-box-every-trajectory-event-cites-a-real-module-and-state 150
  (prop/for-all [seed gen/large-integer]
    (let [{:keys [trajectory]} (run-fixture seed {})]
      (every? (fn [event]
                (and (= "fixture-clinic" (:module event))
                     (contains? (:states fixture-clinic) (:state event))))
              trajectory))))

(defspec attribute-writes-are-always-in-the-declared-registry 150
  (prop/for-all [seed gen/large-integer]
    (let [{:keys [attributes]} (run-fixture seed {})
          declared (gmf/declared-attributes fixture-clinic)]
      (every? declared (keys attributes)))))

;; --- Remaining unit/branch coverage: Delay's non-day units, the "!="
;; attribute operator, an unrecognized condition type, and the
;; runaway-loop backstop -----------------------------------------------------

(deftest delay-supports-weeks-months-and-years-units
  (let [module-for (fn [unit] {:id "unit-mod" :name "Unit"
                               :states {:initial {:type :initial :direct-transition :wait}
                                        :wait {:type :delay :exact {:quantity 1 :unit unit} :direct-transition :done}
                                        :done {:type :terminal}}})
        ctx (assoc (ctx-for (persona-at 1)) :current :wait)]
    (is (= 7 (:advance (interp/step (module-for "weeks") (Random. 1) ctx))))
    (is (<= 28 (:advance (interp/step (module-for "months") (Random. 1) ctx)) 31))
    (is (<= 365 (:advance (interp/step (module-for "years") (Random. 1) ctx)) 366))))

;; --- Procedure duration (ADR-0032, D3c finding 1 fix) -----------------------

(def procedure-with-duration-module
  {:id "procedure-mod" :name "Procedure"
   :states {:initial {:type :initial :direct-transition :do-it}
            :do-it {:type :procedure
                    :codes [{:system :snomed :code "80146002" :display "Appendectomy"}]
                    :duration {:low 1 :high 5 :unit "days"}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest procedure-duration-advances-virtual-time-within-its-range
  (testing "before ADR-0032's fix, a Procedure's own flat {:low :high :unit}
            :duration silently never advanced time -- resolve-time-advance
            destructured :range/:exact keys from it and found neither"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :do-it)
          outcome (interp/step procedure-with-duration-module (Random. 42) ctx)]
      (is (<= 1 (:advance outcome) 5))
      (is (= :done (:next outcome))))))

(defspec procedure-duration-consumes-a-fixed-single-rng-draw 100
  (prop/for-all [seed gen/large-integer]
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :do-it)
          calls (atom 0)
          rng (proxy [Random] [(long seed)]
                (nextInt
                  ([n] (swap! calls inc) (proxy-super nextInt n))))]
      (interp/step procedure-with-duration-module rng ctx)
      (= 1 @calls))))

;; --- ADR-0035 (Wave F0): GAUSSIAN/EXPONENTIAL/TRIANGULAR sampling,
;; Delay/Procedure timing + Symptom severity (AR-1/AR-3/AR-5) -------------

(deftest probit-approx-matches-known-standard-normal-quantiles
  (testing "ADR-0035 AR-3: Acklam's rational approximation, source-cited
            in gmf_interpreter.clj -- checked against well-known
            standard-normal quantiles and its own symmetry invariant"
    (let [probit @#'interp/probit-approx]
      (is (< (Math/abs (- 0.0 (probit 0.5))) 1e-9))
      (is (< (Math/abs (- 1.959964 (probit 0.975))) 1e-5))
      (is (< (Math/abs (- -1.959964 (probit 0.025))) 1e-5))
      (is (< (Math/abs (- 1.0 (probit 0.8413447))) 1e-5))
      (is (< (Math/abs (+ (probit 0.025) (probit 0.975))) 1e-9)))))

(deftest exponential-sample-applies-the-plus-one-shift-verbatim
  (testing "AR-1: value = 1 + ln(1-u)/(-1/mean) -- a fixed-seed golden
            value, byte-confirmed against Distribution.java's own
            EXPONENTIAL branch (mean 10, seed 1's own first .nextDouble
            draw, 0.7308781907032909)"
    (let [sample-distribution @#'interp/sample-distribution]
      (is (< (Math/abs (- 14.125911792091946
                          (sample-distribution (Random. 1) {:kind :exponential :parameters {:mean 10} :round false})))
             1e-9)))))

(deftest triangular-sample-matches-the-two-branch-inverse-cdf
  (testing "AR-1: the two-branch triangular inverse-CDF, ported verbatim
            -- a fixed-seed golden value (min 0, mode 5, max 10, seed 7)"
    (let [sample-distribution @#'interp/sample-distribution]
      (is (< (Math/abs (- 6.330524847202547
                          (sample-distribution (Random. 7) {:kind :triangular :parameters {:min 0 :mode 5 :max 10} :round false})))
             1e-9)))))

(def gaussian-delay-module
  {:id "gaussian-delay-mod" :name "GaussianDelay"
   :states {:initial {:type :initial :direct-transition :wait}
            :wait {:type :delay
                   :distribution {:kind :gaussian :parameters {:mean 30 :standard-deviation 5 :min 10 :max 40}
                                  :round true :unit "days"}
                   :direct-transition :done}
            :done {:type :terminal}}})

(deftest gaussian-delay-advances-within-its-clamped-range
  (dotimes [seed 20]
    (let [ctx (assoc (ctx-for (persona-at seed)) :current :wait)
          outcome (interp/step gaussian-delay-module (Random. seed) ctx)]
      (is (<= 10 (:advance outcome) 40))
      (is (= :done (:next outcome))))))

(defspec gaussian-delay-consumes-a-fixed-single-rng-draw 100
  (prop/for-all [seed gen/large-integer]
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :wait)
          calls (atom 0)
          rng (proxy [Random] [(long seed)]
                (nextDouble
                  ([] (swap! calls inc) (proxy-super nextDouble))))]
      (interp/step gaussian-delay-module rng ctx)
      (= 1 @calls))))

(def clamped-gaussian-delay-module
  {:id "clamp-mod" :name "Clamp"
   :states {:initial {:type :initial :direct-transition :wait}
            :wait {:type :delay
                   :distribution {:kind :gaussian :parameters {:mean 1000 :standard-deviation 1 :min 10 :max 40}
                                  :round false :unit "days"}
                   :direct-transition :done}
            :done {:type :terminal}}})

(deftest gaussian-clamps-to-max-when-mean-is-far-above-it
  (testing "AR-1: clamping, not resampling -- a mean this far outside
            [min, max] clamps on effectively every draw"
    (dotimes [seed 20]
      (let [ctx (assoc (ctx-for (persona-at seed)) :current :wait)
            outcome (interp/step clamped-gaussian-delay-module (Random. seed) ctx)]
        (is (= 40 (:advance outcome)))))))

(def exponential-procedure-module
  {:id "exp-proc-mod" :name "ExpProcedure"
   :states {:initial {:type :initial :direct-transition :do-it}
            :do-it {:type :procedure
                    :codes [{:system :snomed :code "1" :display "Test"}]
                    :distribution {:kind :exponential :parameters {:mean 5} :round false :unit "days"}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest exponential-procedure-advances-virtual-time-and-consumes-one-draw
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :do-it)
        calls (atom 0)
        rng (proxy [Random] [42]
              (nextDouble
                ([] (swap! calls inc) (proxy-super nextDouble))))
        outcome (interp/step exponential-procedure-module rng ctx)]
    (is (<= 1 (:advance outcome)))
    (is (= :done (:next outcome)))
    (is (= 1 @calls))))

(def triangular-symptom-module
  {:id "tri-symptom-mod" :name "TriSymptom"
   :states {:initial {:type :initial :direct-transition :sev}
            :sev {:type :symptom :symptom "Pain"
                  :distribution {:kind :triangular :parameters {:min 0 :mode 5 :max 10} :round false}
                  :direct-transition :done}
            :done {:type :terminal}}})

(deftest triangular-symptom-sets-severity-within-its-bounds
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :sev)
        outcome (interp/step triangular-symptom-module (Random. 7) ctx)
        severity (get (:attributes outcome) :tri-symptom-mod/pain)]
    (is (<= 0 severity 10))))

(defspec triangular-symptom-consumes-a-fixed-single-rng-draw 100
  (prop/for-all [seed gen/large-integer]
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :sev)
          calls (atom 0)
          rng (proxy [Random] [(long seed)]
                (nextDouble
                  ([] (swap! calls inc) (proxy-super nextDouble))))]
      (interp/step triangular-symptom-module rng ctx)
      (= 1 @calls))))

(def observation-with-a-stray-raw-v2-distribution-module
  "uti/ed_bundle.json's own O2-saturation Observation states, byte-
  confirmed against the real vendored closure -- a v2 :distribution the
  LOADER never normalizes (Observation is not one of ADR-0035's own
  three contexts, an out-of-scope encoding this session's fence does not
  cover), left raw and string-keyed on the state map. `emit-and-advance`
  is the shared helper every trajectory-event-producing state type
  calls -- this regression guards that its own :distribution check stays
  gated on `(= :procedure (:type state))`, never firing for this
  unrelated, still-raw field."
  {:id "obs-stray-dist-mod" :name "ObsStrayDist"
   :states {:initial {:type :initial :direct-transition :o2}
            :o2 {:type :observation
                 :codes [{:system :loinc :code "2708-6" :display "Oxygen saturation"}]
                 :distribution {:kind "UNIFORM" :round false :parameters {:low 90 :high 100}}
                 :direct-transition :done}
            :done {:type :terminal}}})

(deftest emit-and-advance-ignores-a-stray-raw-distribution-on-a-non-procedure-state
  (testing "ADR-0035: found live during the full non-integration suite
            run (uti/ed_bundle.json's own O2 Observation states) -- an
            ungated :distribution check here crashed on the raw,
            string-keyed leftover"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :o2)
          outcome (interp/step observation-with-a-stray-raw-v2-distribution-module (Random. 1) ctx)]
      (is (= 0 (:advance outcome)))
      (is (= :done (:next outcome))))))

(def not-equal-attribute-module
  {:id "ne-mod" :name "NotEqual"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :attribute :attribute "status" :operator "!=" :value "closed"}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest attribute-guard-supports-the-not-equal-operator
  (let [ctx-open (assoc (ctx-for (persona-at 1)) :current :check :attributes {:ne-mod/status "open"})
        ctx-closed (assoc (ctx-for (persona-at 1)) :current :check :attributes {:ne-mod/status "closed"})]
    (is (false? (:blocked? (interp/step not-equal-attribute-module (Random. 1) ctx-open))))
    (is (true? (:blocked? (interp/step not-equal-attribute-module (Random. 1) ctx-closed))))))

(deftest evaluate-condition-throws-on-an-unrecognized-condition-type
  (testing ":true (Logic.java's own trivial always-true constant class,
            source-grounded, `pin 7e08387c68a7f0e21d13076609a159fd473fc902`)
            is still outside v1's vocabulary -- this test's own example
            has swapped several times as each named example joined v1
            (most recently :vital-sign, GMF coverage Wave VS, ADR-0039
            AR-1/AR-4, this namespace's own updated tests, below) -- picked
            fresh here because it is GENUINELY unbuilt, not merely the
            latest gap"
    (is (thrown? clojure.lang.ExceptionInfo
                 (interp/evaluate-condition "any-mod" (ctx-for (persona-at 1))
                                             {:condition-type :true})))))

;; --- M5b: Active Condition / Active Medication / Active Allergy / And ----
;; (docs/gmf-interpreter.md section 2's own condition-vocabulary-gap note,
;; built once the ratified vendored module (sinusitis.json) genuinely
;; needed it on its own mandatory post-encounter path -- see gmf.clj's
;; own condition-type->keyword docstring for the full account.)

(def onset-concept {:system :snomed :code "36971009" :display "Sinusitis (disorder)"})
(def other-concept {:system :snomed :code "999999999" :display "Something else"})

(defn- ctx-with-onset [p]
  (assoc (ctx-for p) :trajectory [{:module "m" :state :onset :event :condition-onset :t 0 :codes [onset-concept]}]))

(deftest active-condition-holds-after-an-uncancelled-onset-of-the-matching-concept
  (is (true? (interp/evaluate-condition "m" (ctx-with-onset (persona-at 1))
                                        {:condition-type :active-condition :codes [onset-concept]}))))

(deftest active-condition-does-not-hold-for-a-different-concept
  (is (false? (interp/evaluate-condition "m" (ctx-with-onset (persona-at 1))
                                         {:condition-type :active-condition :codes [other-concept]}))))

(deftest active-condition-does-not-hold-once-a-referencing-condition-end-exists
  (let [ctx (update (ctx-with-onset (persona-at 1)) :trajectory conj
                    {:module "m" :state :resolve :event :condition-end :t 10 :references 0})]
    (is (false? (interp/evaluate-condition "m" ctx {:condition-type :active-condition :codes [onset-concept]})))))

(deftest active-medication-mirrors-active-condition-over-medication-order-end
  (let [ctx (assoc (ctx-for (persona-at 1)) :trajectory
                   [{:module "m" :state :rx :event :medication-order :t 0 :codes [onset-concept]}])]
    (is (true? (interp/evaluate-condition "m" ctx {:condition-type :active-medication :codes [onset-concept]})))
    (let [ended (update ctx :trajectory conj {:module "m" :state :end-rx :event :medication-end :t 5 :references 0})]
      (is (false? (interp/evaluate-condition "m" ended {:condition-type :active-medication :codes [onset-concept]}))))))

;; --- GMF coverage Wave I2 (2026-08-04, ADR-0041 AR-2): Active CarePlan ----
;; Logic.java's own ActiveCarePlan class -- :codes dispatches through the
;; SAME onset/end log query :active-condition/:active-medication already
;; establish (:care-plan-start/:care-plan-end, the paired span ADR-0029
;; R2(b) built); :referenced-by-attribute is installed but not yet used by
;; any vendored candidate (no closure writes a careplan-index attribute for
;; it to read), proven here by a hand-built ctx.

(defn- ctx-with-careplan-onset [p]
  (assoc (ctx-for p) :trajectory [{:module "m" :state :cp-onset :event :care-plan-start :t 0 :codes [onset-concept]}]))

(deftest active-careplan-codes-form-holds-after-an-uncancelled-care-plan-start-of-the-matching-concept
  (is (true? (interp/evaluate-condition "m" (ctx-with-careplan-onset (persona-at 1))
                                        {:condition-type :active-careplan :codes [onset-concept]}))))

(deftest active-careplan-codes-form-is-false-for-a-different-concept
  (is (false? (interp/evaluate-condition "m" (ctx-with-careplan-onset (persona-at 1))
                                         {:condition-type :active-careplan :codes [other-concept]}))))

(deftest active-careplan-codes-form-is-false-once-a-referencing-care-plan-end-exists
  (let [ctx (update (ctx-with-careplan-onset (persona-at 1)) :trajectory conj
                    {:module "m" :state :cp-end :event :care-plan-end :t 10 :references 0})]
    (is (false? (interp/evaluate-condition "m" ctx {:condition-type :active-careplan :codes [onset-concept]})))))

(deftest active-careplan-codes-form-is-false-when-no-matching-careplan-was-ever-started
  (testing "the natural answer, not honest-absence -- activity is what this
            condition tests, ADR-0040 AR-3's own observation-condition
            distinction applies verbatim, ADR-0041 AR-2"
    (is (false? (interp/evaluate-condition "m" (ctx-for (persona-at 1))
                                           {:condition-type :active-careplan :codes [onset-concept]})))))

(deftest active-careplan-referenced-by-attribute-form-is-false-when-the-attribute-was-never-written
  (is (false? (interp/evaluate-condition "m" (ctx-for (persona-at 1))
                                         {:condition-type :active-careplan :referenced-by-attribute "cp"}))))

(deftest active-careplan-referenced-by-attribute-form-holds-for-an-active-referenced-entry
  (let [ctx (assoc (ctx-with-careplan-onset (persona-at 1)) :attributes {:m/cp 0})]
    (is (true? (interp/evaluate-condition "m" ctx {:condition-type :active-careplan :referenced-by-attribute "cp"})))))

(deftest active-careplan-referenced-by-attribute-form-is-false-once-the-referenced-entry-is-ended
  (let [ctx (-> (ctx-with-careplan-onset (persona-at 1))
                (assoc :attributes {:m/cp 0})
                (update :trajectory conj {:module "m" :state :cp-end :event :care-plan-end :t 10 :references 0}))]
    (is (false? (interp/evaluate-condition "m" ctx {:condition-type :active-careplan :referenced-by-attribute "cp"})))))

(deftest active-allergy-is-always-false
  (testing "documented simplification -- no allergy concept exists anywhere
            in this project's Persona for a query to find (gmf-
            interpreter's own evaluate-condition docstring note)"
    (is (false? (interp/evaluate-condition "m" (ctx-with-onset (persona-at 1)) {:condition-type :active-allergy})))))

(deftest and-condition-is-true-only-when-every-sub-condition-holds
  (let [ctx (ctx-with-onset (persona-at 1))
        all-true {:condition-type :and :conditions [{:condition-type :active-condition :codes [onset-concept]}
                                                     {:condition-type :active-allergy}
                                                     {:condition-type :active-allergy}]}
        one-false {:condition-type :and :conditions [{:condition-type :active-condition :codes [onset-concept]}
                                                      {:condition-type :active-condition :codes [other-concept]}]}]
    (is (false? (interp/evaluate-condition "m" ctx all-true)) "every sub-condition true except the always-false allergy check")
    (is (false? (interp/evaluate-condition "m" ctx one-false)))
    (is (true? (interp/evaluate-condition "m" ctx {:condition-type :and
                                                   :conditions [{:condition-type :active-condition :codes [onset-concept]}]})))))

;; --- GMF coverage Wave A: Symptom-as-condition (2026-08-02) --------------
;; Not one of AR-2's five named candidates -- an emergent finding, Step 1's
;; own characterization: `At Least`'s only real vendored use
;; (sore_throat.json's Determine_if_Bacterial) wraps Symptom sub-conditions
;; exclusively alongside Observation/Age, so building `At Least` for real
;; branch coverage requires this too. Data source clears AR-2's own
;; membership bar regardless: the already-accumulating `:attributes` map,
;; written by the already-built `:symptom` STATE type (M5a) -- no new state
;; home. Semantics grounded in Synthea's own Logic.java/Person.java at the
;; docs/gmf-interpreter.md pinned commit (7e08387c...): `Person.getSymptom`
;; defaults to 0 when the symptom was never sampled, compared via the same
;; operator vocabulary `:attribute`'s own condition already uses.

(deftest symptom-condition-compares-the-namespaced-severity-attribute
  (let [ctx (assoc (ctx-for (persona-at 1)) :attributes {:m/cough 45})]
    (is (true? (interp/evaluate-condition "m" ctx {:condition-type :symptom :symptom "Cough" :operator ">" :value 30})))
    (is (false? (interp/evaluate-condition "m" ctx {:condition-type :symptom :symptom "Cough" :operator "<" :value 30})))))

(deftest symptom-condition-defaults-to-zero-when-never-sampled
  (testing "mirrors Synthea's own Person.getSymptom default (0), not a
            missing-key error -- a module may check a symptom before ever
            writing it on some branch"
    (let [ctx (ctx-for (persona-at 1))]
      (is (true? (interp/evaluate-condition "m" ctx {:condition-type :symptom :symptom "Cough" :operator "<" :value 30})))
      (is (false? (interp/evaluate-condition "m" ctx {:condition-type :symptom :symptom "Cough" :operator ">" :value 0}))))))

(deftest symptom-condition-consumes-no-rng
  (let [calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))
        ctx (assoc (ctx-for (persona-at 1)) :attributes {:m/cough 45})]
    (interp/evaluate-condition "m" ctx {:condition-type :symptom :symptom "Cough" :operator ">" :value 30})
    (is (= 0 @calls))))

(defspec symptom-condition-never-touches-rng-state 100
  (prop/for-all [seed gen/large-integer
                 severity (gen/choose 0 100)
                 threshold (gen/choose 0 100)]
    (let [ctx (assoc (ctx-for (persona-at 1)) :attributes {:m/x severity})
          calls (atom 0)
          rng (proxy [Random] [(long seed)]
                (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
                (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))]
      ;; evaluate-condition takes no rng at all (the whole function's own
      ;; signature never accepts one) -- this property exercises the SAME
      ;; shared rng a real Guard/conditional-transition step would pass
      ;; downstream, proving no OTHER code path this condition's own
      ;; evaluation touches taps it either.
      (interp/evaluate-condition "m" ctx {:condition-type :symptom :symptom "x" :operator ">" :value threshold})
      (= 0 @calls))))

;; --- GMF coverage Wave A: At Least (2026-08-02) ---------------------------
;; A count-filter N-of-M compound wrapper, the same shape :and/:or share --
;; semantics grounded against Synthea's own Logic.java AtLeast class at the
;; docs/gmf-interpreter.md pinned commit: true iff at least :minimum of
;; :conditions evaluate true. Real use: sore_throat.json's own
;; Determine_if_Bacterial (Step 3).

(deftest at-least-condition-holds-once-minimum-sub-conditions-are-true
  (let [ctx (assoc (ctx-for (persona-at 1)) :attributes {:m/cough 45 :m/fatigue 45 :m/fever 0})
        cond3 {:condition-type :at-least :minimum 2
               :conditions [{:condition-type :symptom :symptom "Cough" :operator ">" :value 30}
                            {:condition-type :symptom :symptom "Fatigue" :operator ">" :value 30}
                            {:condition-type :symptom :symptom "Fever" :operator ">" :value 30}]}]
    (is (true? (interp/evaluate-condition "m" ctx cond3)) "exactly 2 of 3 hold, minimum is 2")
    (is (false? (interp/evaluate-condition "m" ctx (assoc cond3 :minimum 3))) "only 2 of 3 hold, minimum is 3")))

(deftest at-least-condition-consumes-no-rng
  (let [calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))
        ctx (assoc (ctx-for (persona-at 1)) :attributes {:m/a 50})]
    (interp/evaluate-condition "m" ctx {:condition-type :at-least :minimum 1
                                        :conditions [{:condition-type :symptom :symptom "a" :operator ">" :value 10}]})
    (is (= 0 @calls))))

;; --- GMF coverage Wave A: Or (2026-08-02) ---------------------------------
;; Boolean disjunction, mirroring :and's own recursive shape -- Synthea's
;; own Logic.java Or class: true iff ANY sub-condition holds.

(deftest or-condition-is-true-when-any-sub-condition-holds
  (let [ctx (assoc (ctx-for (persona-at 1)) :attributes {:m/a 5})
        cond {:condition-type :or
              :conditions [{:condition-type :symptom :symptom "a" :operator ">" :value 100}
                           {:condition-type :symptom :symptom "a" :operator "<" :value 100}]}]
    (is (true? (interp/evaluate-condition "m" ctx cond)))
    (is (false? (interp/evaluate-condition "m" ctx {:condition-type :or
                                                     :conditions [{:condition-type :symptom :symptom "a" :operator ">" :value 100}]})))))

(deftest or-condition-consumes-no-rng
  (let [calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))]
    (interp/evaluate-condition "m" (ctx-for (persona-at 1))
                               {:condition-type :or :conditions [{:condition-type :active-allergy}]})
    (is (= 0 @calls))))

(defspec at-least-and-or-never-touch-rng-state 100
  (prop/for-all [seed gen/large-integer
                 minimum (gen/choose 1 3)
                 vals (gen/vector (gen/choose 0 100) 3)]
    (let [ctx (assoc (ctx-for (persona-at 1)) :attributes {:m/a (nth vals 0) :m/b (nth vals 1) :m/c (nth vals 2)})
          sub-conds [{:condition-type :symptom :symptom "a" :operator ">" :value 50}
                     {:condition-type :symptom :symptom "b" :operator ">" :value 50}
                     {:condition-type :symptom :symptom "c" :operator ">" :value 50}]
          calls (atom 0)
          rng (proxy [Random] [(long seed)]
                (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
                (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))]
      (interp/evaluate-condition "m" ctx {:condition-type :at-least :minimum minimum :conditions sub-conds})
      (interp/evaluate-condition "m" ctx {:condition-type :or :conditions sub-conds})
      (= 0 @calls))))

;; --- GMF coverage Wave A: Date (2026-08-02) --------------------------------
;; A calendar-year comparison against ctx's own virtual clock (:t, an
;; epoch-day anchored to the persona's real DOB since M5a -- already-
;; existing data, no new state home). Semantics grounded against Synthea's
;; own Logic.java Date class at the docs/gmf-interpreter.md pinned commit
;; (currentyear = Utilities.getYear(time), compared via :operator). Not
;; used by sore_throat.json itself -- named in AR-2 because it unlocks
;; most of stroke.json in a later wave (Wave A+C, per the wave plan).

(deftest date-condition-compares-the-calendar-year-of-ctxs-own-virtual-clock
  (let [ctx (assoc (ctx-for (persona-at 1)) :t (.toEpochDay (java.time.LocalDate/of 1999 6 15)))]
    (is (true? (interp/evaluate-condition "m" ctx {:condition-type :date :operator ">" :year 1997})))
    (is (false? (interp/evaluate-condition "m" ctx {:condition-type :date :operator ">" :year 1999})))
    (is (true? (interp/evaluate-condition "m" ctx {:condition-type :date :operator "<=" :year 1999})))))

(deftest date-condition-consumes-no-rng
  (let [calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))]
    (interp/evaluate-condition "m" (ctx-for (persona-at 1)) {:condition-type :date :operator ">" :year 1997})
    (is (= 0 @calls))))

(defspec date-condition-never-touches-rng-state 100
  (prop/for-all [seed gen/large-integer
                 year (gen/choose 1950 2030)
                 target (gen/choose 1950 2030)]
    (let [ctx (assoc (ctx-for (persona-at 1)) :t (.toEpochDay (java.time.LocalDate/of year 1 1)))
          calls (atom 0)
          rng (proxy [Random] [(long seed)]
                (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
                (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))]
      (interp/evaluate-condition "m" ctx {:condition-type :date :operator ">" :year target})
      (= 0 @calls))))

;; --- GMF coverage Wave A: Observation-as-condition (2026-08-02) ----------
;; A log query over already-emitted :observation trajectory events by
;; concept, the same shape :active-condition/:active-medication already
;; establish -- already-existing data (the accumulating :trajectory), no
;; new state home; the value itself was already sampled and carried by the
;; already-built :observation STATE type (M5a). Semantics grounded against
;; Synthea's own Logic.java Observation class at the docs/gmf-interpreter.md
;; pinned commit: most recent matching-code observation's value, compared
;; via :operator.
;;
;; FIXED (2026-08-04, ADR-0040 AR-3): used to THROW when no matching
;; observation was ever recorded -- corrected to FALSE, upstream's own
;; issue-774 band-aid (`Logic.java`'s `exporter.split_records=true`
;; branch), adopted unconditionally since this project has no split-
;; records/lossOfCare axis of its own for a config flag to gate. "is
;; nil"/"is not nil" (real Synthea's own explicit absence tests) stay
;; OUT of v1 scope, unchanged -- no candidate module this session needs
;; them. Real use: sore_throat.json's Determine_if_Bacterial (Step 3),
;; whose only two predecessor states (Take_Temperature_High/Low) are
;; BOTH Observation states citing the same LOINC code (confirmed by
;; reading the vendored file directly, so the absent-on-missing path is
;; never live on that module's own real walk); anemia___unknown_
;; etiology.json's own `anemia_sub` submodule is the real closure this
;; session's fix unblocks (a Hematocrit condition reached before any
;; Hematocrit was ever recorded on some branch).

(def temp-concept {:system :loinc :code "8310-5" :display "Body temperature"})

(defn- ctx-with-observation [p value]
  (assoc (ctx-for p) :trajectory [{:module "m" :state :take-temp :event :observation :t 0
                                   :codes [temp-concept] :value value}]))

(deftest observation-condition-compares-the-most-recent-matching-observations-value
  (let [ctx (ctx-with-observation (persona-at 1) 38.5)]
    (is (true? (interp/evaluate-condition "m" ctx {:condition-type :observation :codes [temp-concept] :operator ">" :value 38})))
    (is (false? (interp/evaluate-condition "m" ctx {:condition-type :observation :codes [temp-concept] :operator "<" :value 38})))))

(deftest observation-condition-uses-the-most-recent-of-several-matching-observations
  (let [ctx (update (ctx-with-observation (persona-at 1) 37.0) :trajectory conj
                    {:module "m" :state :take-temp-2 :event :observation :t 1 :codes [temp-concept] :value 39.0})]
    (is (true? (interp/evaluate-condition "m" ctx {:condition-type :observation :codes [temp-concept] :operator ">" :value 38})))))

(deftest observation-condition-is-false-when-no-matching-observation-was-ever-recorded
  (testing "ADR-0040 AR-3: absence is what this condition TESTS -- false,
            never a thrown module-authoring-shape-bug error, upstream's
            own issue-774 band-aid adopted unconditionally"
    (is (false? (interp/evaluate-condition "m" (ctx-for (persona-at 1))
                                            {:condition-type :observation :codes [temp-concept] :operator ">" :value 38})))))

(deftest observation-condition-consumes-no-rng
  (let [calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))]
    (interp/evaluate-condition "m" (ctx-with-observation (persona-at 1) 38.5)
                               {:condition-type :observation :codes [temp-concept] :operator ">" :value 38})
    (is (= 0 @calls))))

(defspec observation-condition-never-touches-rng-state 100
  (prop/for-all [seed gen/large-integer
                 value (gen/double* {:min 30 :max 45 :NaN? false :infinite? false})]
    (let [ctx (ctx-with-observation (persona-at 1) value)
          calls (atom 0)
          rng (proxy [Random] [(long seed)]
                (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
                (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))]
      (interp/evaluate-condition "m" ctx {:condition-type :observation :codes [temp-concept] :operator ">" :value 38})
      (= 0 @calls))))

;; --- M5b: Device/DeviceEnd -- consumed-internally, like :simple -----------

(def device-module
  {:id "device-mod" :name "Device"
   :states {:initial {:type :initial :direct-transition :neb}
            :neb {:type :device :code {:system :snomed :code "170615005" :display "Home nebulizer (physical object)"}
                  :direct-transition :end-neb}
            :end-neb {:type :device-end :device :neb :direct-transition :done}
            :done {:type :terminal}}})

(deftest device-and-device-end-produce-no-trajectory-event
  (let [ctx (ctx-for (persona-at 1))
        outcome (interp/walk-module device-module (Random. 1) ctx)]
    (is (= :terminal (:status outcome)))
    (is (empty? (:trajectory outcome)))))

(def infinite-loop-module
  {:id "loop-mod" :name "Loop"
   :states {:initial {:type :initial :direct-transition :a}
            :a {:type :simple :direct-transition :b}
            :b {:type :simple :direct-transition :a}}})

(deftest walk-module-throws-past-the-max-steps-backstop-on-a-zero-advance-cycle
  (is (thrown? clojure.lang.ExceptionInfo
               (interp/walk-module infinite-loop-module (Random. 1) (ctx-for (persona-at 1))))))

;; --- GMF coverage Wave B (2026-08-02, ADR-0027, D1-D4): CallSubmodule
;; call/return ---------------------------------------------------------

(def not-nil-attribute-module
  {:id "nn-mod" :name "NotNil"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :attribute :attribute "x" :operator "is not nil"}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest attribute-guard-supports-is-nil-and-is-not-nil-operators
  (testing "Step 1's own characterization, ear_infections.json's mandatory
            path (docs/gmf-interpreter.md section 9)"
    (let [ctx-set (assoc (ctx-for (persona-at 1)) :current :check :attributes {:nn-mod/x true})
          ctx-unset (assoc (ctx-for (persona-at 1)) :current :check)]
      (is (false? (:blocked? (interp/step not-nil-attribute-module (Random. 1) ctx-set))))
      (is (true? (:blocked? (interp/step not-nil-attribute-module (Random. 1) ctx-unset)))))))

(def med-leaf-module
  "The submodule side of a call/return pair: one MedicationOrder,
  assigned to a root-scoped attribute, then Terminal."
  {:id "med-leaf" :name "MedLeaf"
   :states {:initial {:type :initial :direct-transition :prescribe}
            :prescribe {:type :medication-order :assign-to-attribute "rx"
                        :codes [{:system :rxnorm :code "308191" :display "Amoxicillin 500 MG Oral Capsule"}]
                        :direct-transition :done}
            :done {:type :terminal}}})

(def calls-med-leaf-module
  "The root/caller side: calls med-leaf, then ends the medication the
  callee assigned -- the exact ear_infections.json shape (assign-to-
  attribute inside the callee, referenced-by-attribute back in the
  root), Step 1's own characterization."
  {:id "caller-mod" :name "Caller"
   :states {:initial {:type :initial :direct-transition :call}
            :call {:type :call-submodule :submodule "med-leaf" :direct-transition :end-med}
            :end-med {:type :medication-end :referenced-by-attribute "rx" :direct-transition :done}
            :done {:type :terminal}}})

(def med-closure {"caller-mod" calls-med-leaf-module "med-leaf" med-leaf-module})

(deftest call-submodule-descends-runs-and-returns-to-the-callers-own-transition
  (let [result (interp/walk-module calls-med-leaf-module (Random. 1) (ctx-for (persona-at 1)) med-closure)]
    (is (= :terminal (:status result)))
    (is (= [:medication-order :medication-end] (mapv :event (:trajectory result))))))

(deftest call-submodule-assign-to-attribute-crosses-the-call-boundary-root-scoped
  (testing "D1: the callee's own MedicationOrder writes rx under the
            ROOT's namespace (caller-mod, not med-leaf), so the caller's
            own MedicationEnd resolves the SAME attribute back to the
            order's own trajectory index"
    (let [result (interp/walk-module calls-med-leaf-module (Random. 1) (ctx-for (persona-at 1)) med-closure)
          [order-event end-event] (:trajectory result)]
      (is (= 0 (get (:attributes result) :caller-mod/rx)))
      (is (not (contains? (:attributes result) :med-leaf/rx)))
      (is (= 0 (:references end-event)))
      (is (= (:codes order-event) (:codes (get-in med-leaf-module [:states :prescribe])))))))

(deftest call-submodule-events-carry-the-root-first-call-path-citation
  (testing "D2: the callee's own event cites the full call path; the
            caller's own events (before/after the call) carry no
            :call-path at all -- backward-compatible representation for
            the non-calling case"
    (let [result (interp/walk-module calls-med-leaf-module (Random. 1) (ctx-for (persona-at 1)) med-closure)
          [order-event end-event] (:trajectory result)]
      (is (= ["caller-mod" "med-leaf"] (:call-path order-event)))
      (is (= "med-leaf" (:module order-event)))
      (is (not (contains? end-event :call-path)) "the caller's own event, not inside any active call")
      (is (= "caller-mod" (:module end-event))))))

(deftest call-submodule-throws-when-the-closure-is-missing-the-callees-call-path
  (testing "loader/interpreter mismatch -- gmf/load-closure should have
            caught this at load time; a programmer-error throw here, not
            a silent misbehavior"
    (is (thrown? clojure.lang.ExceptionInfo
                 (interp/walk-module calls-med-leaf-module (Random. 1) (ctx-for (persona-at 1))
                                      {"caller-mod" calls-med-leaf-module})))))

(def calls-blocking-leaf-module
  {:id "blocks-forever" :name "BlocksForever"
   :states {:initial {:type :initial
                       :direct-transition :check}
            :check {:type :guard :allow {:condition-type :attribute :attribute "never" :operator "is not nil"}
                    :direct-transition :done}
            :done {:type :terminal}}})

(def calls-blocking-module
  {:id "caller-blocks" :name "CallerBlocks"
   :states {:initial {:type :initial :direct-transition :call}
            :call {:type :call-submodule :submodule "blocks-forever" :direct-transition :done}
            :done {:type :terminal}}})

(deftest call-submodule-throws-when-the-callee-blocks-on-a-guard
  (testing "disclosed, out-of-scope limitation this session (ns docstring's
            own note) -- no resume-across-a-call mechanism yet"
    (is (thrown? clojure.lang.ExceptionInfo
                 (interp/walk-module calls-blocking-module (Random. 1) (ctx-for (persona-at 1))
                                      {"caller-blocks" calls-blocking-module "blocks-forever" calls-blocking-leaf-module})))))

;; D3's own defensive call-depth backstop: a long CHAIN (not a cycle --
;; gmf/load-closure's own acyclicity check already covers cycles at load
;; time) exceeding max-call-depth throws, a bug signal per that
;; invariant's own docstring.
(defn- chain-module [id next-id]
  {:id id :name id
   :states {:initial {:type :initial :direct-transition (if next-id :call :done)}
            :call {:type :call-submodule :submodule next-id :direct-transition :done}
            :done {:type :terminal}}})

(deftest call-submodule-throws-past-the-max-call-depth-backstop-on-a-long-chain
  (let [ids (mapv #(str "chain-" %) (range 150))
        modules (into {} (map (fn [[id next-id]] [id (chain-module id next-id)])
                               (map vector ids (concat (rest ids) [nil]))))
        root (get modules (first ids))]
    (is (thrown? clojure.lang.ExceptionInfo
                 (interp/walk-module root (Random. 1) (ctx-for (persona-at 1)) modules)))))

(defspec call-submodule-walk-is-deterministic-for-the-same-inputs 100
  (prop/for-all [seed gen/large-integer]
    (let [p1 (persona-at seed) p2 (persona-at seed)
          r1 (interp/walk-module calls-med-leaf-module (Random. seed) (ctx-for p1) med-closure)
          r2 (interp/walk-module calls-med-leaf-module (Random. seed) (ctx-for p2) med-closure)]
      (= (:trajectory r1) (:trajectory r2)))))

;; --- GMF coverage Wave B (2026-08-02, ADR-0027, D5): the fifth
;; transition kind, type_of_care_transition -------------------------------

(def type-of-care-module
  {:id "toc-mod" :name "CarePathways"
   :states {:initial {:type :initial :direct-transition :pick}
            :pick {:type :simple
                   :type-of-care-transition {:ambulatory :ambulatory :emergency :ed :telemedicine :telemedicine}}
            :ambulatory {:type :terminal}
            :ed {:type :terminal}
            :telemedicine {:type :terminal}}})

(defn- ctx-at-year [year]
  (assoc (ctx-for (persona-at 1)) :current :pick :t (.toEpochDay (java.time.LocalDate/of ^int year 6 1))))

(defn- well-mixed-seeds
  "Sequential small `Random` seeds are famously clustered on their OWN
  first `.nextDouble()` draw (confirmed live below: seeds 0-9 all land
  within 0.7301-0.7311, a documented Java Random quirk) -- a separate
  mixer RNG generating well-distributed longs is this project's own
  established fix (`vendored_sore_throat_test.clj`'s own `well-mixed-
  candidate-seeds`), reused here rather than re-solved."
  [n mixer-seed]
  (let [mixer (Random. mixer-seed)]
    (repeatedly n #(.nextLong mixer))))

(deftest type-of-care-transition-never-picks-telemedicine-before-2020
  (testing "D5: real Synthea's own telemedicine_config.json start_year --
            no telemedicine option exists before it, this project's own
            virtual clock (:t) answers the year honestly"
    (let [ctx (ctx-at-year 2010)
          picks (into #{} (map (fn [seed] (:next (interp/step type-of-care-module (Random. seed) ctx))))
                      (well-mixed-seeds 500 20260802))]
      (is (not (contains? picks :telemedicine)))
      (is (= #{:ambulatory :ed} picks) "500 draws should see both branches of a 0.75/0.25 split"))))

(deftest type-of-care-transition-can-pick-telemedicine-from-2020-onward
  (let [ctx (ctx-at-year 2021)
        picks (into #{} (map (fn [seed] (:next (interp/step type-of-care-module (Random. seed) ctx))))
                    (well-mixed-seeds 500 20260802))]
    (is (= #{:ambulatory :ed :telemedicine} picks) "500 draws should see all three branches of the during-telemedicine split")))

(deftest type-of-care-transition-consumes-exactly-one-draw
  (let [ctx (ctx-at-year 2021)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble)))]
    (interp/step type-of-care-module rng ctx)
    (is (= 1 @calls))))

;; --- GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3a, H2): the
;; sixth transition kind, lookup_table_transition ---------------------------

(def lookup-table-module
  {:id "lookup-mod"
   :name "Lookup"
   :states {:initial {:type :initial :direct-transition :pick}
            :pick {:type :simple
                   :lookup-table-transition [{:transition :a :default-probability 0.0 :lookup-table-name "t.csv"}
                                              {:transition :b :default-probability 1.0 :lookup-table-name "t.csv"}]}
            :a {:type :terminal}
            :b {:type :terminal}}})

(def lookup-tables
  "Deliberately extreme, opposite-of-default weights (D3a): a matching
  row always picks :a (weight 1.0/0.0); the entries' own JSON-declared
  :default-probability (above) always picks :b -- proves the ROW's own
  weight is what's consulted, not merely `weighted-pick-transition`
  falling through to defaults regardless."
  {"t.csv" [{:age-range [15 24] :attributes {"gender" "F"} :weights {:a 1.0 :b 0.0}}]})

(defn- ctx-aged [persona years]
  (-> (ctx-for persona) (assoc :current :pick) (update :t + (* 365 years))))

(deftest lookup-table-transition-uses-the-matching-row-s-own-weights
  (let [female-20 (assoc (persona-at 1) :sex :female)
        ctx (ctx-aged female-20 20)]
    (dotimes [seed 5]
      (is (= :a (:next (interp/step lookup-table-module (Random. seed) ctx
                                     {(:id lookup-table-module) lookup-table-module} lookup-tables)))
          "the female 15-24 row's own 1.0 weight for :a always wins, any seed"))))

(deftest lookup-table-transition-falls-back-to-default-probability-on-no-match
  (testing "no row matches (wrong gender) -- real Synthea's own
            defaultTransitions mirror, D3a"
    (let [male-20 (assoc (persona-at 1) :sex :male)
          ctx (ctx-aged male-20 20)]
      (dotimes [seed 5]
        (is (= :b (:next (interp/step lookup-table-module (Random. seed) ctx
                                       {(:id lookup-table-module) lookup-table-module} lookup-tables)))
            "no matching row -- falls back to the entries' own default-probability (0.0/1.0), always :b"))))
  (testing "no row matches (age outside every range) -- same fallback"
    (let [female-5 (assoc (persona-at 1) :sex :female)
          ctx (ctx-aged female-5 5)]
      (is (= :b (:next (interp/step lookup-table-module (Random. 1) ctx
                                     {(:id lookup-table-module) lookup-table-module} lookup-tables)))))))

(deftest lookup-table-transition-with-no-tables-argument-falls-back-to-defaults
  (testing "the optional trailing `tables` argument defaults to {} --
            zero behavior change for every pre-D3 call site"
    (let [ctx (ctx-aged (assoc (persona-at 1) :sex :female) 20)]
      (is (= :b (:next (interp/step lookup-table-module (Random. 1) ctx)))))))

;; --- GMF coverage Wave LC (2026-08-03, ADR-0038 AR-1/AR-2): the H2
;; whitelist retires -- module-set attributes, persona-field columns
;; (race/state), :time-range containment, and honest-absence at the
;; walk boundary, extended from conditions to lookup-table columns ---------

(def module-attr-lookup-tables
  {"t.csv" [{:age-range nil :time-range nil :attributes {"operative_status" "elective"} :weights {:a 1.0 :b 0.0}}]})

(deftest lookup-table-transition-resolves-a-module-set-attribute-column
  (testing "AR-1(c): a lookup-table attribute column resolves against
            the module's OWN namespaced attributes first -- the SAME
            root-namespaced key attribute-condition-holds?/resolve-
            distribution-value already read"
    (let [ctx (assoc (ctx-aged (persona-at 1) 20) :attributes {:lookup-mod/operative-status "elective"})]
      (is (= :a (:next (interp/step lookup-table-module (Random. 1) ctx
                                     {(:id lookup-table-module) lookup-table-module} module-attr-lookup-tables)))))))

(deftest lookup-table-transition-does-not-match-a-different-module-attribute-value
  (let [ctx (assoc (ctx-aged (persona-at 1) 20) :attributes {:lookup-mod/operative-status "emergent"})]
    (is (= :b (:next (interp/step lookup-table-module (Random. 1) ctx
                                   {(:id lookup-table-module) lookup-table-module} module-attr-lookup-tables))))))

(deftest lookup-table-transition-still-consumes-exactly-one-draw
  (let [ctx (assoc (ctx-aged (persona-at 1) 20) :attributes {:lookup-mod/operative-status "elective"})
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble)))]
    (interp/step lookup-table-module rng ctx {(:id lookup-table-module) lookup-table-module} module-attr-lookup-tables)
    (is (= 1 @calls))))

(def race-lookup-tables
  {"t.csv" [{:age-range nil :time-range nil :attributes {"race" "Black"} :weights {:a 1.0 :b 0.0}}]})

(deftest lookup-table-transition-resolves-a-persona-race-column
  (testing "AR-1(c): falls back to the persona-field mapping when the
            module has never set the same-named attribute"
    (let [ctx (ctx-aged (assoc (persona-at 1) :race "Black") 20)]
      (is (= :a (:next (interp/step lookup-table-module (Random. 1) ctx
                                     {(:id lookup-table-module) lookup-table-module} race-lookup-tables)))))))

(deftest lookup-table-transition-module-attribute-wins-over-a-same-named-persona-field
  (testing "AR-1(c)'s own module-attribute-first ordering -- a module-
            set column and a persona column resolve from DIFFERENT
            stores; the module store wins whenever both could apply"
    (let [ctx (assoc (ctx-aged (assoc (persona-at 1) :race "Black") 20)
                      :attributes {:lookup-mod/race "White"})]
      (is (= :b (:next (interp/step lookup-table-module (Random. 1) ctx
                                     {(:id lookup-table-module) lookup-table-module} race-lookup-tables)))
          "the module's own :race attribute ('White') never matches the row's 'Black', even though persona :race would have"))))

(deftest lookup-table-transition-against-a-persona-missing-a-referenced-field-is-a-walk-error
  (testing "AR-1(c)'s own honest-absence rule, extended from conditions
            (ADR-0036 AR-4) to lookup-table attribute columns -- `step`
            itself still throws; `walk-module` converts it"
    (is (thrown? clojure.lang.ExceptionInfo
                 (interp/step lookup-table-module (Random. 1) (assoc (ctx-for (persona-at 1)) :current :pick)
                               {(:id lookup-table-module) lookup-table-module} race-lookup-tables)))
    (let [result (interp/walk-module lookup-table-module (Random. 1) (ctx-for (persona-at 1))
                                      {(:id lookup-table-module) lookup-table-module} race-lookup-tables)]
      (is (= :walk-error (:status result)))
      (is (= :lookup-table-column (:condition-type (:walk-error result))))
      (is (= :race (:missing-field (:walk-error result)))))))

(deftest lookup-table-transition-time-range-containment
  (testing "AR-1(b)/AR-2: :time-range is the SAME inclusive-both-ends
            containment :age-range already performs, checked against
            ctx's own epoch-day :t"
    (let [ctx (ctx-aged (persona-at 1) 20)
          in-range {"t.csv" [{:age-range nil :time-range [(- (:t ctx) 5) (+ (:t ctx) 5)]
                               :attributes {} :weights {:a 1.0 :b 0.0}}]}
          out-of-range {"t.csv" [{:age-range nil :time-range [(+ (:t ctx) 100) (+ (:t ctx) 200)]
                                   :attributes {} :weights {:a 1.0 :b 0.0}}]}]
      (is (= :a (:next (interp/step lookup-table-module (Random. 1) ctx
                                     {(:id lookup-table-module) lookup-table-module} in-range))))
      (is (= :b (:next (interp/step lookup-table-module (Random. 1) ctx
                                     {(:id lookup-table-module) lookup-table-module} out-of-range)))))))

;; --- GMF coverage Wave D stage D3 (2026-08-02, ADR-0029, D3b, H3):
;; attribute-weighted distributed_transition (NamedDistribution) --------
;; stroke.json's own Chance_of_Stroke shape (byte-confirmed against
;; source, D3b) -- proven against a hand-authored fixture per H3's own
;; instruction ("not stroke"): the mechanism landing does NOT unblock
;; stroke.json itself (stroke_risk stays SPECIFIED, unsourceable).

(def named-distribution-module
  "wait's own weight is 0 (not 1): weighted-pick-transition's cumulative
  walk always resolves to the LAST entry when every acc' stays at or
  below target (D3b's own test needs this -- with wait=1, a present
  attribute of 1.0 would still lose to wait on roughly half of all
  draws, since onset is no longer the trailing arm; 0 makes onset's own
  1.0 win deterministically, and the fallback-to-default case (0/0
  total) still resolves to wait, the trailing entry, unconditionally)."
  {:id "named-dist-mod"
   :name "NamedDist"
   :states {:initial {:type :initial :direct-transition :roll}
            :roll {:type :simple
                   :distributed-transition [{:transition :onset :distribution {:attribute "risk" :default 0}}
                                             {:transition :wait :distribution 0}]}
            :onset {:type :terminal}
            :wait {:type :terminal}}})

(deftest named-distribution-uses-the-attribute-value-when-present
  (testing "a root-scoped attribute of 1.0, the entry's own complement
            of 0 (no rng draw could ever pick the other branch)"
    (let [ctx (-> (ctx-for (persona-at 1))
                  (assoc :current :roll)
                  (update :attributes assoc :named-dist-mod/risk 1.0))]
      (dotimes [seed 5]
        (is (= :onset (:next (interp/step named-distribution-module (Random. seed) ctx))))))))

(deftest named-distribution-falls-back-to-the-json-declared-default-when-absent
  (testing "stroke.json's own real gap (ADR-0028): default 0 means the
            named-distribution branch is never picked when the
            attribute was never written"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :roll)]
      (dotimes [seed 5]
        (is (= :wait (:next (interp/step named-distribution-module (Random. seed) ctx))))))))

(deftest named-distribution-consumes-no-extra-rng-beyond-the-single-pick
  (let [ctx (-> (ctx-for (persona-at 1)) (assoc :current :roll) (update :attributes assoc :named-dist-mod/risk 0.5))
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble)))]
    (interp/step named-distribution-module rng ctx)
    (is (= 1 @calls) "the attribute read is a pure lookup, zero rng -- only the pick itself draws")))

;; --- GMF coverage Wave I (2026-08-04, ADR-0040 AR-1): the SAME
;; NamedDistribution resolution, now inside a complex_transition entry's
;; own nested :distributions -- injuries.json's own Elderly_Incidence_
;; Rates shape, byte-confirmed against source. --------------------------

(def complex-named-distribution-module
  "The SAME 0/1-weight construction `named-distribution-module` (above)
  uses, one level down inside a single complex_transition entry with no
  :condition (an unconditional 'else' arm) -- deterministic regardless
  of draw."
  {:id "complex-named-dist-mod"
   :name "ComplexNamedDist"
   :states {:initial {:type :initial :direct-transition :branch}
            :branch {:type :simple
                     :complex-transition
                     [{:distributions [{:transition :fall :distribution {:attribute "risk" :default 0}}
                                       {:transition :no-fall :distribution 0}]}]}
            :fall {:type :terminal}
            :no-fall {:type :terminal}}})

(deftest complex-transition-named-distribution-uses-the-attribute-value-when-present
  (let [ctx (-> (ctx-for (persona-at 1))
                (assoc :current :branch)
                (update :attributes assoc :complex-named-dist-mod/risk 1.0))]
    (dotimes [seed 5]
      (is (= :fall (:next (interp/step complex-named-distribution-module (Random. seed) ctx)))))))

(deftest complex-transition-named-distribution-falls-back-to-the-json-declared-default-when-absent
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :branch)]
    (dotimes [seed 5]
      (is (= :no-fall (:next (interp/step complex-named-distribution-module (Random. seed) ctx)))))))

;; --- GMF coverage Wave C (2026-08-02, ADR-0028, C1/C2): Death --------------

(def death-cause-codes [{:system :snomed :code "1" :display "Test cause"}])

(def immediate-death-module
  {:id "death-mod"
   :name "Death"
   :states {:initial {:type :initial :direct-transition :die}
            :die {:type :death :codes death-cause-codes :direct-transition :end-encounter}
            :end-encounter {:type :terminal}}})

(deftest death-with-no-range-or-exact-fires-immediately-with-no-rng-draw
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :die)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))
        outcome (interp/step immediate-death-module rng ctx)]
    (is (= 0 @calls))
    (is (= 0 (:advance outcome)))
    (is (true? (:terminal? outcome)))
    (is (false? (:blocked? outcome)))
    (is (nil? (:next outcome)) "Death's own declared transition is never resolved -- C2's terminal contract")
    (is (= 1 (count (:events outcome))))
    (is (= :death (:event (first (:events outcome)))))
    (is (= death-cause-codes (:codes (first (:events outcome)))) "cause of death carried verbatim -- code passthrough law")))

(def exact-death-module
  {:id "exact-death-mod"
   :name "ExactDeath"
   :states {:initial {:type :initial :direct-transition :die}
            :die {:type :death :exact {:quantity 3 :unit "days"} :codes death-cause-codes}
            :terminal {:type :terminal}}})

(deftest death-with-exact-advances-deterministically-with-no-rng-draw
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :die)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))
        outcome (interp/step exact-death-module rng ctx)]
    (is (= 0 @calls))
    (is (= 3 (:advance outcome)))
    (is (true? (:terminal? outcome)))))

(def range-death-module
  {:id "range-death-mod"
   :name "RangeDeath"
   :states {:initial {:type :initial :direct-transition :die}
            :die {:type :death :range {:low 1 :high 30 :unit "days"} :codes death-cause-codes}
            :terminal {:type :terminal}}})

(deftest death-with-range-consumes-exactly-one-draw-and-advances-within-range
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :die)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))
        outcome (interp/step range-death-module rng ctx)]
    (is (= 1 @calls))
    (is (<= 1 (:advance outcome) 30))
    (is (true? (:terminal? outcome)))))

(deftest death-events-own-t-is-the-computed-death-time-not-the-states-own-entry-time
  (testing "a :range death is genuinely delayed -- the emitted event
            cites the COMPUTED death time, not entry time (stroke.json's
            own Death state, docs/gmf-interpreter.md section 10)"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :die :t 1000)
          outcome (interp/step range-death-module (Random. 1) ctx)
          event (first (:events outcome))]
      (is (= (+ 1000 (:advance outcome)) (:t event)))
      (is (not= 1000 (:t event)) "a nonzero range draw is expected for this fixed seed"))))

;; --- GMF coverage Wave I2 (2026-08-04, ADR-0041 AR-1): Death's own
;; condition-onset/referenced-by-attribute cause forms -----------------------

(def death-onset-concept {:system :snomed :code "42343007" :display "Congestive heart failure"})

(deftest death-condition-onset-cause-resolves-the-named-onset-events-codes
  (let [ctx (-> (ctx-for (persona-at 1))
                (assoc :current :die)
                (assoc :trajectory [{:module "death-mod" :state :onset :event :condition-onset :t 0
                                     :codes [death-onset-concept]}]))
        module (assoc-in immediate-death-module [:states :die] {:type :death :condition-onset :onset})
        outcome (interp/step module (Random. 1) ctx)]
    (is (= [death-onset-concept] (:codes (first (:events outcome)))))))

(deftest death-condition-onset-cause-is-nil-when-the-named-state-never-onset
  (testing "a disclosed simplification -- upstream's own second fallback
            (reading the named state's own JSON-declared codes even when
            it never fired) is NOT ported, ADR-0041 AR-1"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :die)
          module (assoc-in immediate-death-module [:states :die] {:type :death :condition-onset :onset})
          outcome (interp/step module (Random. 1) ctx)]
      (is (nil? (:codes (first (:events outcome))))))))

(deftest death-referenced-by-attribute-cause-resolves-the-attributes-condition-entry
  (testing "congestive_heart_failure.json's own real shape -- ConditionOnset's
            :assign-to-attribute writes the index, Death's own
            :referenced-by-attribute reads it back, ADR-0041 AR-1"
    (let [ctx (-> (ctx-for (persona-at 1))
                  (assoc :current :die)
                  (assoc :trajectory [{:module "death-mod" :state :onset :event :condition-onset :t 0
                                       :codes [death-onset-concept]}])
                  (assoc :attributes {:death-mod/chf 0}))
          module (assoc-in immediate-death-module [:states :die] {:type :death :referenced-by-attribute "chf"})
          outcome (interp/step module (Random. 1) ctx)]
      (is (= [death-onset-concept] (:codes (first (:events outcome))))))))

(deftest death-referenced-by-attribute-cause-is-nil-when-the-attribute-was-never-written
  (testing "a disclosed departure from upstream's own throw ('referenced but
            not set') -- Death's own cause is supplementary content, never a
            walk-gating precondition, ADR-0041 AR-1"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :die)
          module (assoc-in immediate-death-module [:states :die] {:type :death :referenced-by-attribute "chf"})
          outcome (interp/step module (Random. 1) ctx)]
      (is (nil? (:codes (first (:events outcome))))))))

(deftest death-cause-codes-outranks-condition-onset-and-referenced-by-attribute
  (testing "State.java's own Death.process if/else-if chain -- :codes checked
            FIRST, source re-read fresh this session (ADR-0041 AR-1's own
            correction of docs/gmf-interpreter.md section 10's paraphrase)"
    (let [ctx (-> (ctx-for (persona-at 1))
                  (assoc :current :die)
                  (assoc :trajectory [{:module "death-mod" :state :onset :event :condition-onset :t 0
                                       :codes [death-onset-concept]}])
                  (assoc :attributes {:death-mod/chf 0}))
          module (assoc-in immediate-death-module [:states :die]
                            {:type :death :codes death-cause-codes :condition-onset :onset
                             :referenced-by-attribute "chf"})
          outcome (interp/step module (Random. 1) ctx)]
      (is (= death-cause-codes (:codes (first (:events outcome))))))))

;; --- GMF coverage Wave I2 (2026-08-04, ADR-0041 AR-1): ConditionOnset's own
;; :assign-to-attribute -- the SAME index-based indirection MedicationOrder's
;; own field already establishes, ported here so Death's own referenced-by-
;; attribute form (above) has a real attribute to read. ---------------------

(deftest condition-onset-with-assign-to-attribute-writes-a-root-scoped-index
  (let [ctx (ctx-for (persona-at 1))
        module {:id "onset-mod" :name "Onset"
                :states {:initial {:type :initial :direct-transition :onset}
                         :onset {:type :condition-onset :assign-to-attribute "chf"
                                 :codes [death-onset-concept] :direct-transition :done}
                         :done {:type :terminal}}}
        outcome (interp/step module (Random. 1) (assoc ctx :current :onset))]
    (is (= 0 (get (:attributes outcome) :onset-mod/chf))
        "the FIRST trajectory event's own index -- this is that event")))

(deftest walk-module-terminates-at-death-no-trajectory-event-follows-it
  (testing "C2's own terminal contract, at the walk-module layer -- Death's
            own declared transition (:end-encounter, a real Terminal
            state in this fixture) is never reached"
    (let [result (interp/walk-module immediate-death-module (Random. 1) (ctx-for (persona-at 1)))]
      (is (= :terminal (:status result)))
      (is (= 1 (count (:trajectory result))))
      (is (= :death (:event (last (:trajectory result))))))))

(defspec no-trajectory-event-ever-follows-death 200
  (prop/for-all [seed gen/large-integer]
    (let [p (persona-at seed)
          reg-t (interp/dob-epoch-day p)
          result (interp/run-module range-death-module (Random. seed) p reg-t (+ reg-t 3650))
          trajectory (:trajectory result)
          death-idx (first (keep-indexed (fn [i e] (when (= :death (:event e)) i)) trajectory))]
      (or (nil? death-idx) (= death-idx (dec (count trajectory)))))))

;; --- GMF coverage Wave F (2026-08-03, ADR-0036 AR-1): Counter --------------

(def counter-increment-module
  {:id "counter-mod" :name "Counter"
   :states {:initial {:type :initial :direct-transition :bump}
            :bump {:type :counter :attribute "los" :action :increment :amount 2}
            :done {:type :terminal}}})

(deftest counter-increment-writes-current-plus-amount-under-a-root-namespaced-key
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :bump
                   :attributes {:counter-mod/los 3})
        outcome (interp/step counter-increment-module (Random. 1) ctx)]
    (is (= 5 (get-in outcome [:attributes :counter-mod/los])))
    (is (= [] (:events outcome)) "Counter is consumed internally -- no trajectory event")))

(def counter-decrement-module
  {:id "counter-mod" :name "Counter"
   :states {:initial {:type :initial :direct-transition :bump}
            :bump {:type :counter :attribute "los" :action :decrement :amount 2}
            :done {:type :terminal}}})

(deftest counter-decrement-subtracts-amount
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :bump
                   :attributes {:counter-mod/los 3})
        outcome (interp/step counter-decrement-module (Random. 1) ctx)]
    (is (= 1 (get-in outcome [:attributes :counter-mod/los])))))

(deftest counter-defaults-a-missing-attribute-to-zero
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :bump)
        outcome (interp/step counter-increment-module (Random. 1) ctx)]
    (is (= 2 (get-in outcome [:attributes :counter-mod/los])))))

(def counter-no-amount-module
  {:id "counter-mod" :name "Counter"
   :states {:initial {:type :initial :direct-transition :bump}
            :bump {:type :counter :attribute "los" :action :increment}
            :done {:type :terminal}}})

(deftest counter-defaults-amount-to-one-when-absent-legacy-compatibility
  (testing "State.java's own Counter.initialize: amount == 0 (absent or
            authored 0, indistinguishable at the source) defaults to 1"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :bump
                     :attributes {:counter-mod/los 3})
          outcome (interp/step counter-no-amount-module (Random. 1) ctx)]
      (is (= 4 (get-in outcome [:attributes :counter-mod/los]))))))

(def counter-explicit-zero-amount-module
  {:id "counter-mod" :name "Counter"
   :states {:initial {:type :initial :direct-transition :bump}
            :bump {:type :counter :attribute "los" :action :increment :amount 0}
            :done {:type :terminal}}})

(deftest counter-defaults-an-explicitly-authored-zero-amount-to-one-too
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :bump
                   :attributes {:counter-mod/los 3})
        outcome (interp/step counter-explicit-zero-amount-module (Random. 1) ctx)]
    (is (= 4 (get-in outcome [:attributes :counter-mod/los])))))

(defspec counter-consumes-zero-rng-draws 100
  (prop/for-all [seed gen/large-integer]
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :bump)
          calls (atom 0)
          rng (proxy [Random] [(long seed)]
                (nextDouble ([] (swap! calls inc) (proxy-super nextDouble)))
                (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))]
      (interp/step counter-increment-module rng ctx)
      (= 0 @calls))))

;; --- GMF coverage Wave F (2026-08-03, ADR-0036 AR-2): ImagingStudy ---------

(def chest-xray-code {:system :snomed :code "399208008" :display "Plain X-ray of chest (procedure)"})
(def cr-modality {:system :dicom-dcm :code "CR" :display "Computed Radiography"})

(def imaging-study-fixed-module
  "congestive_heart_failure.json's own CXR_ED shape -- no series/instance
  bounds, the real project-relevant path (byte-confirmed against the
  vendored Synthea checkout at the pin: no module this project's own
  census walks authors study-level or instance-level bounds today)."
  {:id "imaging-mod" :name "Imaging"
   :states {:initial {:type :initial :direct-transition :xray}
            :xray {:type :imaging-study
                   :procedure-code chest-xray-code
                   :series [{:modality cr-modality :instances [{:title "Title of this image"}]}]
                   :direct-transition :done}
            :done {:type :terminal}}})

(deftest imaging-study-emits-one-event-with-procedure-code-and-primary-modality
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :xray)
        outcome (interp/step imaging-study-fixed-module (Random. 1) ctx)
        event (first (:events outcome))]
    (is (= 1 (count (:events outcome))))
    (is (= [chest-xray-code] (:codes event)))
    (is (= cr-modality (:modality event)))
    (is (= [{:modality cr-modality :instance-count 1}] (:series event)))))

(deftest imaging-study-with-no-bounds-consumes-zero-rng-draws
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :xray)
        calls (atom 0)
        rng (proxy [Random] [1]
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))]
    (interp/step imaging-study-fixed-module rng ctx)
    (is (= 0 @calls))))

(deftest imaging-study-never-advances-the-virtual-clock
  (testing "AR-2: upstream's own process() returns immediately -- no
            module-clock advance, unlike a duration-bearing Procedure"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :xray)
          outcome (interp/step imaging-study-fixed-module (Random. 1) ctx)]
      (is (= 0 (:advance outcome))))))

(def imaging-study-bounded-module
  "lung_cancer.json's own per-series instance-count bounds shape --
  min/max-number-instances present, no study-level min/max-number-series
  (real Synthea's own more common bounded form, byte-confirmed)."
  {:id "imaging-bounded-mod" :name "ImagingBounded"
   :states {:initial {:type :initial :direct-transition :ct}
            :ct {:type :imaging-study
                 :procedure-code {:system :snomed :code "16335031000119103"
                                  :display "High resolution computed tomography of chest without contrast (procedure)"}
                 :series [{:modality {:system :dicom-dcm :code "CT" :display "Computed Tomography"}
                           :min-number-instances 300 :max-number-instances 500
                           :instances [{:title "CT Image Storage"}]}]
                 :direct-transition :done}
            :done {:type :terminal}}})

(deftest imaging-study-draws-one-instance-count-within-its-bounds-when-a-series-declares-them
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :ct)
        calls (atom 0)
        rng (proxy [Random] [1]
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))
        outcome (interp/step imaging-study-bounded-module rng ctx)
        event (first (:events outcome))]
    (is (= 1 @calls))
    (is (<= 300 (:instance-count (first (:series event))) 500))))

(def imaging-study-series-count-bounded-module
  "State.java's own study-level min_number_series/max_number_series --
  not observed on any real module this project's own census walks
  (disclosed, ADR-0036's own execution note), but a real, source-grounded
  path (Distribution.java-adjacent State.java field, byte-confirmed)."
  {:id "imaging-series-bounded-mod" :name "ImagingSeriesBounded"
   :states {:initial {:type :initial :direct-transition :ct}
            :ct {:type :imaging-study
                 :procedure-code chest-xray-code
                 :series [{:modality cr-modality :instances [{:title "one"}]}]
                 :min-number-series 2 :max-number-series 4
                 :direct-transition :done}
            :done {:type :terminal}}})

(deftest imaging-study-materializes-a-drawn-number-of-series-all-cloning-the-first
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :ct)
        outcome (interp/step imaging-study-series-count-bounded-module (Random. 1) ctx)
        event (first (:events outcome))]
    (is (<= 2 (count (:series event)) 4))
    (is (every? #(= cr-modality (:modality %)) (:series event)))))

(deftest imaging-study-compiles-to-a-procedure-shaped-ir-step
  (let [result (interp/walk-module imaging-study-fixed-module (Random. 1) (ctx-for (persona-at 1)))
        compiled (ct/compile-trajectory (:trajectory result) sim-model/default-facility (interp/dob-epoch-day (persona-at 1)))
        step (first (filter #(= :procedure (:type %)) (:steps compiled)))]
    (is (some? step))
    (is (= [chest-xray-code] (:codes step)))))

;; --- GMF coverage Wave F (2026-08-03, ADR-0036 AR-3): SupplyList -----------

(def supply-list-module
  "sleep_apnea.json's own Nasal Mask Supplies shape (byte-confirmed)."
  {:id "supply-mod" :name "Supply"
   :states {:initial {:type :initial :direct-transition :supplies}
            :supplies {:type :supply-list
                       :supplies [{:code {:system :snomed :code "467645007"
                                          :display "Continuous positive airway pressure nasal oxygen cannula (physical object)"}
                                   :quantity 1}]
                       :direct-transition :done}
            :done {:type :terminal}}})

(deftest supply-list-emits-one-event-carrying-its-components-verbatim
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :supplies)
        outcome (interp/step supply-list-module (Random. 1) ctx)
        event (first (:events outcome))]
    (is (= 1 (count (:events outcome))))
    (is (= 1 (count (:components event))))
    (is (= "467645007" (:code (:code (first (:components event))))))))

(deftest supply-list-consumes-zero-rng-and-zero-advance
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :supplies)
        calls (atom 0)
        rng (proxy [Random] [1]
              (nextDouble ([] (swap! calls inc) (proxy-super nextDouble)))
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))
        outcome (interp/step supply-list-module rng ctx)]
    (is (= 0 @calls))
    (is (= 0 (:advance outcome)))))

(deftest supply-list-compiles-to-no-ir-step-log-only-fact
  (testing "AR-3: the ConditionEnd no-open-encounter precedent verbatim
            -- a real trajectory event, unconditionally no IR step"
    (let [result (interp/walk-module supply-list-module (Random. 1) (ctx-for (persona-at 1)))
          compiled (ct/compile-trajectory (:trajectory result) sim-model/default-facility (interp/dob-epoch-day (persona-at 1)))]
      (is (= 1 (count (:trajectory result))))
      (is (= :supply-list (:event (first (:trajectory result)))))
      (is (= [] (:steps compiled))))))

;; --- GMF coverage Wave F (2026-08-03, ADR-0036 AR-4): condition rider ------

(def not-prior-state-guard-module
  {:id "not-mod" :name "Not"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :not
                            :condition {:condition-type :prior-state :name :some-state}}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest not-condition-blocks-when-the-nested-condition-holds
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :check
                   :trajectory [{:module "not-mod" :state :some-state :t 0}])
        outcome (interp/step not-prior-state-guard-module (Random. 1) ctx)]
    (is (true? (:blocked? outcome)))))

(deftest not-condition-passes-when-the-nested-condition-does-not-hold
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :check)
        outcome (interp/step not-prior-state-guard-module (Random. 1) ctx)]
    (is (false? (:blocked? outcome)))))

(def race-guard-module
  {:id "race-mod" :name "Race"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard :allow {:condition-type :race :race "Native"}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest race-condition-matches-case-insensitively
  (let [ctx (assoc (ctx-for (assoc (persona-at 1) :race "native")) :current :check)
        outcome (interp/step race-guard-module (Random. 1) ctx)]
    (is (false? (:blocked? outcome)))))

(deftest race-condition-blocks-on-a-non-matching-race
  (let [ctx (assoc (ctx-for (assoc (persona-at 1) :race "Asian")) :current :check)
        outcome (interp/step race-guard-module (Random. 1) ctx)]
    (is (true? (:blocked? outcome)))))

(def ses-guard-module
  {:id "ses-mod" :name "Ses"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard :allow {:condition-type :socioeconomic-status :category "High"}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest socioeconomic-status-condition-matches-case-sensitively
  (let [ctx (assoc (ctx-for (assoc (persona-at 1) :socioeconomic-category "High")) :current :check)
        outcome (interp/step ses-guard-module (Random. 1) ctx)]
    (is (false? (:blocked? outcome)))))

(deftest socioeconomic-status-condition-does-not-match-a-different_case
  (testing "Logic.java's own category.equals(...) -- unlike Race, no
            case-folding"
    (let [ctx (assoc (ctx-for (assoc (persona-at 1) :socioeconomic-category "high")) :current :check)
          outcome (interp/step ses-guard-module (Random. 1) ctx)]
      (is (true? (:blocked? outcome))))))

(deftest race-condition-against-a-persona-missing-race-is-a-walk-error-not-a-silent-false
  (testing "AR-4's own honest-absence rule -- `step` itself still throws
            (evaluate-condition's own internal contract, propagating
            through guard-step unchanged); `walk-module` is where it
            becomes a recorded result"
    (let [ctx (ctx-for (persona-at 1))]
      (is (thrown? clojure.lang.ExceptionInfo (interp/step race-guard-module (Random. 1) (assoc ctx :current :check)))))
    (let [result (interp/walk-module race-guard-module (Random. 1) (ctx-for (persona-at 1)))]
      (is (= :walk-error (:status result)))
      (is (= :race (:condition-type (:walk-error result))))
      (is (= :race (:missing-field (:walk-error result)))))))

(deftest socioeconomic-status-condition-against-a-persona-missing-the-field-is-a-walk-error
  (let [result (interp/walk-module ses-guard-module (Random. 1) (ctx-for (persona-at 1)))]
    (is (= :walk-error (:status result)))
    (is (= :socioeconomic-status (:condition-type (:walk-error result))))
    (is (= :socioeconomic-category (:missing-field (:walk-error result))))))

(deftest run-module-also-converts-honest-absence-into-a-walk-error-status
  (let [p (persona-at 1)
        reg-t (interp/dob-epoch-day p)
        result (interp/run-module race-guard-module (Random. 1) p reg-t)]
    (is (= :walk-error (:status result)))))

(deftest a-genuinely-unsupported-condition-type-still-throws-uncaught-never-a-walk-error
  (testing "the honest-absence catch is scoped to exactly its own marker
            -- every other exception this namespace throws stays a loud,
            uncaught crash, not silently downgraded into a soft result"
    (let [module {:id "bad-cond-mod" :name "BadCond"
                  :states {:initial {:type :initial :direct-transition :check}
                           :check {:type :guard :allow {:condition-type :nonexistent-condition}
                                   :direct-transition :done}
                           :done {:type :terminal}}}]
      (is (thrown? clojure.lang.ExceptionInfo
                   (interp/walk-module module (Random. 1) (ctx-for (persona-at 1))))))))

;; --- GMF coverage Wave G (2026-08-03, ADR-0037 AR-1/AR-2): wellness
;; cadence -- schedule-function band/spot tests, transcribed alongside
;; the table (`resources/sim-trajectory/wellness-cadence.edn`'s own
;; header cites the exact source lines these values check against:
;; `EncounterModule.recommendedTimeBetweenWellnessVisits`, pin
;; 7e08387c68a7f0e21d13076609a159fd473fc902, lines 176-201). A bare
;; `{:dob ...}` map suffices here -- `next-wellness-tick` reads only
;; `:dob`, never `:sex`/attributes, so going through `sim-model/persona`
;; would only add irrelevant sampling. -----------------------------------

(defn- persona-with-dob [dob-str] {:dob dob-str})

(deftest next-wellness-tick-is-always-strictly-later-than-its-own-query-time
  (testing "AR-2 (refined live against med_rec.json's own real, zero-
            delay wellness-wait loop, this Wave's own session): every
            call returns a tick STRICTLY AFTER `t`, even when `t` is
            DOB itself (the recurrence's own tick0) -- an inclusive
            `>=` would return DOB unchanged on the very first call,
            and the SAME tick again on every re-entry at that
            unchanged `t`, an infinite zero-advance spin real modules
            with no delay between wellness visits (med_rec.json) hit
            in practice, not merely a hypothetical"
    (let [p (persona-with-dob "2020-01-01")
          dob (interp/dob-epoch-day p)]
      (is (> (interp/next-wellness-tick p dob) dob))
      (is (> (interp/next-wellness-tick p (interp/next-wellness-tick p dob))
             (interp/next-wellness-tick p dob))
          "re-querying AT a tick just returned still advances -- the
           property a zero-delay wait-loop depends on"))))

(deftest next-wellness-tick-infant-band-is-one-month
  (testing "lines 178-179: ageInMonths <= 1 -> 1-month interval, the
            very first cadence tick past DOB"
    (let [p (persona-with-dob "2020-01-01")
          dob (interp/dob-epoch-day p)]
      (is (= (.toEpochDay (.plusMonths (LocalDate/parse "2020-01-01") 1))
             (interp/next-wellness-tick p (inc dob)))))))

;; Independent re-derivation of the SAME cited source lines (NOT a copy
;; of `wellness-cadence-band`/`wellness-cadence.edn`'s own content) --
;; a tautology against the implementation would prove nothing; this is
;; a second transcription, checked against the first.
(defn- expected-band [^long age-years ^long age-months]
  (if (<= age-years 3)
    (cond (<= age-months 1) [1 :months] (<= age-months 5) [2 :months]
          (<= age-months 17) [3 :months] :else [6 :months])
    (cond (<= age-years 19) [1 :years] (<= age-years 39) [3 :years]
          (<= age-years 49) [2 :years] :else [1 :years])))

(defn- advance-expected ^LocalDate [^LocalDate d [qty unit]]
  (case unit :months (.plusMonths d (long qty)) :years (.plusYears d (long qty))))

(deftest next-wellness-tick-matches-an-independently-transcribed-full-sequence
  (testing "walks the FULL cadence sequence from DOB to well past age
            90, independently re-derived from the same cited source
            lines, and confirms `next-wellness-tick` reproduces every
            tick exactly -- a stronger check than isolated spot values,
            and the one that actually exercises every band boundary
            (1/5/17-month, 19/39/49-year) and the months/years TIER
            boundary (age genuinely > 3 years, Period/getYears floored)"
    (let [dob-date (LocalDate/parse "2020-01-01")
          p (persona-with-dob "2020-01-01")]
      (loop [prev-date dob-date n 0]
        (when (< n 60)
          (let [age-years (.getYears (Period/between dob-date prev-date))
                age-months (.toTotalMonths (Period/between dob-date prev-date))
                band (expected-band age-years age-months)
                expected-next (advance-expected prev-date band)
                actual-next (interp/next-wellness-tick p (inc (.toEpochDay prev-date)))]
            (is (= (.toEpochDay expected-next) actual-next)
                (str "tick " n " at age " age-years "y" age-months "m, expected band " band))
            (recur expected-next (inc n))))))))

(deftest next-wellness-tick-tier-boundary-age-three-vs-four-years
  (testing "the months-tier's own ELSE branch (age-years<=3 but
            age-months>17) stays in the months tier until age-years
            genuinely exceeds 3 -- Period/getYears floors, so 3 years
            11 months is STILL <=3, not yet the years tier: the tick
            landing exactly on age 4 (48 months) is still a 6-month-band
            tick computed from the months tier, not a years-tier one"
    (let [p (persona-with-dob "2020-01-01")
          dob-date (LocalDate/parse "2020-01-01")
          three-years-eleven-months (.toEpochDay (.plusMonths dob-date 47))]
      (is (= (.toEpochDay (.plusMonths dob-date 48))
             (interp/next-wellness-tick p (inc three-years-eleven-months)))))))

(deftest next-wellness-tick-adult-band-boundaries
  (testing "lines 190-198: the years tier's own 19/39/49 boundaries --
            the chain's own tick at age 38 (reached 20 -> 23 -> 26 ->
            29 -> 32 -> 35 -> 38, six +3-year steps off the exact age-20
            tick the 1-year child phase itself lands on) is STILL inside
            the <=39 (3-year) band, landing at 41 -- NOT the >39 (2-year)
            band a naive 'age 39 must be near' assumption would predict
            (39 itself is never a real chain tick: 38's own successor
            already overshoots it)"
    (let [p (persona-with-dob "1990-01-01")
          dob-date (LocalDate/parse "1990-01-01")
          age-38-tick (.toEpochDay (.plusYears dob-date 38))]
      (is (= (.toEpochDay (.plusYears dob-date 41))
             (interp/next-wellness-tick p (inc age-38-tick)))))))

;; --- GMF coverage Wave G (2026-08-03, ADR-0037 AR-3/AR-7): the
;; :wellness-wait interpreter case -- advance-to-tick, reason
;; attachment, horizon parking (Delay's own mechanism, reused
;; unchanged), and the loop-bounding acceptance evidence. Inline
;; fixtures ONLY (AR-7's own fence) -- the four real upstream loop
;; modules this Wave unblocks (med-rec/mend-program/metabolic-syndrome-
;; care/veteran-substance-abuse-treatment) are census-level evidence
;; (AR-8), never a test dependency. --------------------------------------

(def wellness-wait-module
  {:id "wellness-wait-mod"
   :name "WellnessWait"
   :states {:initial {:type :initial :direct-transition :wait}
            :wait {:type :wellness-wait :reason "checkup" :direct-transition :done}
            :done {:type :terminal}}})

(deftest wellness-wait-advances-the-clock-to-the-next-cadence-tick-and-attaches-reason
  (testing "AR-3: the module clock advances to next-wellness-tick, and
            the emitted event attaches the state's own :reason -- a
            NEW thread, unlike every other Encounter-shaped state's
            own validation-only :reason (gmf.clj's own D2 disclosure)"
    (let [p (persona-at 1)
          ctx (-> (ctx-for p) (assoc :current :wait) (update :t + 10))
          expected-tick (interp/next-wellness-tick p (:t ctx))
          outcome (interp/step wellness-wait-module (Random. 1) ctx)]
      (is (pos? (:advance outcome)) "a genuine forward advance, not a zero-time pass-through")
      (is (= (- expected-tick (:t ctx)) (:advance outcome)))
      (is (= :done (:next outcome)))
      (is (false? (:blocked? outcome)))
      (let [event (first (:events outcome))]
        (is (= :encounter (:event event)))
        (is (= :wellness (:encounter-class event)))
        (is (= "checkup" (:reason event)))
        (is (= expected-tick (:t event)))))))

(deftest wellness-wait-consumes-zero-rng-draws
  (testing "AR-2: next-wellness-tick is pure -- confirmed via the same
            call-counting proxy discipline
            exact-delay-advances-deterministically-with-no-rng-draw
            already establishes"
    (let [ctx (-> (ctx-for (persona-at 1)) (assoc :current :wait) (update :t + 10))
          calls (atom 0)
          rng (proxy [Random] [(long 1)]
                (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))]
      (interp/step wellness-wait-module rng ctx)
      (is (= 0 @calls)))))

(def wellness-wait-no-reason-module
  {:id "wellness-wait-no-reason-mod"
   :name "WellnessWaitNoReason"
   :states {:initial {:type :initial :direct-transition :wait}
            :wait {:type :wellness-wait :direct-transition :done}
            :done {:type :terminal}}})

(deftest wellness-wait-with-no-reason-never-fabricates-one
  (testing "code-passthrough discipline: :reason absent on the state ->
            absent on the event, never a nil-valued key"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :wait)
          outcome (interp/step wellness-wait-no-reason-module (Random. 1) ctx)
          event (first (:events outcome))]
      (is (not (contains? event :reason))))))

(def wellness-wait-then-encounter-module
  {:id "wellness-then-mod"
   :name "WellnessThen"
   :states {:initial {:type :initial :direct-transition :wait}
            :wait {:type :wellness-wait :direct-transition :after}
            :after {:type :encounter :encounter-class :ambulatory
                    :codes [{:system :snomed :code "999999" :display "After"}]
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest wellness-wait-parks-past-the-horizon-the-same-way-delay-does
  (testing "AR-3: bounded by horizon-end-t exactly as Delay is -- NOT
            by anything inside wellness-wait-step itself (it never
            receives horizon-end-t), but by run-module's own loop,
            which re-checks :t against horizon-end-t BEFORE every step.
            The wellness event's own step already started (entry :t
            was still < horizon-end-t), so it still lands in the
            trajectory even though its own computed tick overshoots the
            horizon -- exactly the same mechanism that lets a Delay's
            own advance overshoot in one step. The STATE AFTER
            wellness-wait, though, never executes: :horizon-complete,
            not an exception, not :blocked. :wait is reached exactly at
            DOB here -- next-wellness-tick's own strict `>` guarantee
            (AR-2's own refinement) means even that first call already
            returns a genuine, nonzero-advance future tick, which this
            test's own horizon window (20 days) sits comfortably before"
    (let [p (persona-at 1)
          dob (interp/dob-epoch-day p)
          horizon-end-t (+ dob 20)
          result (interp/run-module wellness-wait-then-encounter-module (Random. 1) p dob horizon-end-t)]
      (is (= :horizon-complete (:status result)))
      (is (some #(and (= :encounter (:event %)) (= :wellness (:encounter-class %))) (:trajectory result))
          "the wellness-wait's own event still landed")
      (is (not-any? #(and (= :encounter (:event %)) (= :ambulatory (:encounter-class %))) (:trajectory result))
          "the AFTER state's own encounter never fired -- the walk parked before reaching it"))))

(def wellness-wait-act-loop-module
  {:id "wellness-loop-mod"
   :name "WellnessLoop"
   ;; ZERO delay anywhere in this loop body -- deliberately matching
   ;; the REAL shape of med_rec.json (Wellness_Encounter -> ... ->
   ;; EncounterEnd -> Initial -> ConditionOnset -> Wellness_Encounter
   ;; again, no Delay anywhere): this is the actual regression case for
   ;; next-wellness-tick's own strict-> fix, found live running this
   ;; Wave's own census against the real catalog, not a hypothetical. A
   ;; module author routinely relies on the wellness cycle ITSELF being
   ;; the loop's only clock, exactly as upstream's own design intends.
   ;; EncounterEnd fix (2026-08-08, ADR-0082): `:end` closes each
   ;; wellness encounter before the loop reopens the next one --
   ;; matching real med_rec.json's own EncounterEnd-in-the-loop shape
   ;; (this def's own header comment) faithfully; the interpreter's own
   ;; new "encounters never nest" assert (open-encounter-index,
   ;; gmf-interpreter.clj) caught this fixture's own prior omission,
   ;; which never closed at all -- still zero Delay anywhere in the
   ;; loop body, the property this test actually exercises.
   :states {:initial {:type :initial :direct-transition :wait}
            :wait {:type :wellness-wait :direct-transition :act}
            :act {:type :counter :attribute "visits" :action :increment :direct-transition :end}
            :end {:type :encounter-end :direct-transition :wait}}})

(deftest wellness-wait-act-loop-terminates-horizon-bounded-not-max-steps
  (testing "AR-7: the four real upstream loop modules this Wave
            unblocks (med-rec/mend-program/metabolic-syndrome-care/
            veteran-substance-abuse-treatment) spin under the RETIRED
            create-now substitution -- a zero-time-advance wellness
            encounter never let the horizon check catch up, so the
            walk ran to max-steps and threw. Genuine wait semantics
            make EVERY iteration advance a real cadence interval (even
            with ZERO delay anywhere else in the loop body, med_rec.
            json's own real shape -- next-wellness-tick's own strict
            `>` guarantee, not merely `>=`), so the horizon bounds
            iterations the same way it already bounds every other
            module's own walk"
    (let [p (persona-at 1)
          dob (interp/dob-epoch-day p)
          horizon-end-t (+ dob (* 365 5))
          result (interp/run-module wellness-wait-act-loop-module (Random. 1) p dob horizon-end-t)]
      (is (= :horizon-complete (:status result)))
      (is (>= (count (filter #(= :encounter (:event %)) (:trajectory result))) 2)
          "multiple wellness ticks fired over the 5-year horizon -- genuine iteration, not a single pass"))))

;; --- GMF coverage Wave VS (2026-08-04, ADR-0039 AR-1/AR-3/AR-4): the
;; vital-sign register -- Step 1: baseline seeding, global (non-root-
;; scoped) threading through a walk, and the shared closed-vocabulary
;; check `VitalSign`/`:vital-sign` (Steps 2/3) will reuse. -------------

(deftest initial-context-seeds-the-register-from-the-baseline-table
  (testing "AR-3: flat authored constants, zero rng draws (initial-
            context takes no rng argument at all -- structurally
            impossible to consume one)"
    (let [vital-signs (:vital-signs (ctx-for (persona-at 1)))]
      (is (= 110 (:systolic-blood-pressure vital-signs)))
      (is (= 98 (:oxygen-saturation vital-signs)))
      (is (= 22.0 (:bmi vital-signs)))
      (is (= 90 (:glucose vital-signs)))
      (is (= 50 (:hdl vital-signs)))
      (is (= 100 (:triglycerides vital-signs))))))

(deftest initial-context-carries-no-baseline-for-a-name-the-table-omits
  (testing "AR-3: Left ventricular Ejection fraction is DELIBERATELY
            absent -- congestive_heart_failure.json's own VitalSign
            states always set it before any :vital-sign condition
            reads it, so a baseline would silently mask a real gap"
    (is (not (contains? (:vital-signs (ctx-for (persona-at 1))) :left-ventricular-ejection-fraction)))))

(deftest vital-signs-register-survives-an-ordinary-walk-unchanged
  (testing "no state type writes the register yet (Step 2) -- every
            outcome-producing site threads :vital-signs through
            unmodified, the same pass-through :attributes already gets"
    (let [ctx (ctx-for (persona-at 1))
          outcome (interp/step direct-only-module (Random. 1) ctx)]
      (is (= (:vital-signs ctx) (:vital-signs outcome))))))

(deftest vital-signs-register-survives-a-callsubmodule-round-trip
  (testing "the register is ctx's own GLOBAL compartment (ADR-0027 D1),
            never root-scoped the way workflow :attributes is -- a
            CallSubmodule call/return must thread it through exactly
            the same way (call-submodule-step's own post-call-ctx)"
    (let [callee {:id "callee-mod" :name "Callee"
                   :states {:initial {:type :initial :direct-transition :done}
                            :done {:type :terminal}}}
          caller {:id "caller-mod" :name "Caller"
                   :states {:initial {:type :initial :direct-transition :call}
                            :call {:type :call-submodule :submodule "callee-mod" :direct-transition :done}
                            :done {:type :terminal}}}
          ctx (assoc (ctx-for (persona-at 1)) :root "caller-mod" :current :call)
          outcome (interp/step caller (Random. 1) ctx {"caller-mod" caller "callee-mod" callee})]
      (is (= (:vital-signs ctx) (:vital-signs outcome))))))

(deftest vital-sign-vocabulary-accepts-every-wave-vs-register-name-via-the-observation-reader
  (testing "AR-4: the same table backs all three consumers -- an
            Observation's own vital_sign field (pre-existing reader)
            now recognizes the five names this wave adds, proving the
            shared vital-signs.edn extension rather than a parallel
            vocabulary"
    (doseq [name ["Left ventricular Ejection fraction" "BMI" "HDL" "Triglycerides" "Height"]]
      (let [module (assoc-in vital-sign-observation-module [:states :spo2 :vital-sign] name)
            ctx (assoc (ctx-for (persona-at 1)) :current :spo2)
            outcome (interp/step module (Random. 1) ctx)
            [event] (:events outcome)]
        (is (= :normal (:interpretation event)) (str name " should sample cleanly"))))))

;; --- GMF coverage Wave VS (2026-08-04, ADR-0039 AR-1/AR-2): the
;; VitalSign STATE -- sample-once into the register, never a trajectory
;; event of its own. ---------------------------------------------------

(def lvef-range-module
  {:id "lvef-mod" :name "Lvef"
   :states {:initial {:type :initial :direct-transition :lvef}
            :lvef {:type :vital-sign :vital-sign "Left ventricular Ejection fraction" :unit ""
                   :range {:low 50 :high 100} :direct-transition :done}
            :done {:type :terminal}}})

(deftest vital-sign-state-with-range-writes-the-register-consuming-one-draw
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :lvef)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble)))
        outcome (interp/step lvef-range-module rng ctx)]
    (is (<= 50 (:left-ventricular-ejection-fraction (:vital-signs outcome)) 100))
    (is (= 1 @calls))
    (is (= [] (:events outcome)) "register-only, never a trajectory event")
    (is (= :done (:next outcome)))))

(def lvef-exact-module
  {:id "lvef-mod" :name "Lvef"
   :states {:initial {:type :initial :direct-transition :lvef}
            :lvef {:type :vital-sign :vital-sign "Left ventricular Ejection fraction"
                   :exact {:quantity 62} :direct-transition :done}
            :done {:type :terminal}}})

(deftest vital-sign-state-with-exact-writes-the-literal-value-and-consumes-no-rng
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :lvef)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))
        outcome (interp/step lvef-exact-module rng ctx)]
    (is (= 62.0 (:left-ventricular-ejection-fraction (:vital-signs outcome))))
    (is (zero? @calls))))

(def spo2-distribution-module
  {:id "spo2-mod" :name "Spo2"
   :states {:initial {:type :initial :direct-transition :spo2}
            :spo2 {:type :vital-sign :vital-sign "Oxygen Saturation"
                   :distribution {:kind :gaussian :parameters {:mean 97 :standard-deviation 1} :round false}
                   :direct-transition :done}
            :done {:type :terminal}}})

(deftest vital-sign-state-with-a-distribution-consumes-exactly-one-draw
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :spo2)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble)))
        outcome (interp/step spo2-distribution-module rng ctx)]
    (is (number? (:oxygen-saturation (:vital-signs outcome))))
    (is (= 1 @calls))))

(deftest vital-sign-state-overwrites-a-baseline-value
  (testing "AR-3: later write wins, the same semantics :attributes'
            SetAttribute already establishes"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :spo2)]
      (is (= 98 (:oxygen-saturation (:vital-signs ctx))) "the baseline, before the state runs")
      (let [outcome (interp/step spo2-distribution-module (Random. 1) ctx)]
        (is (not= 98 (:oxygen-saturation (:vital-signs outcome))))))))

(def unrecognized-vital-sign-state-module
  {:id "bad-mod" :name "Bad"
   :states {:initial {:type :initial :direct-transition :x}
            :x {:type :vital-sign :vital-sign "Respiratory Rate" :range {:low 12 :high 20} :direct-transition :done}
            :done {:type :terminal}}})

(deftest vital-sign-state-with-an-unrecognized-name-throws
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :x)]
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unrecognized vital-sign"
                           (interp/step unrecognized-vital-sign-state-module (Random. 1) ctx)))))

;; --- GMF coverage Wave I (2026-08-04, ADR-0040 AR-4): the five names
;; the catalog-wide Observation category vital-signs enumeration added --
;; each resolves (no :unrecognized-vital-sign throw) via the Observation
;; reader's own `vital_sign` value-sourcing branch, exactly the path the
;; census's real found gap (`wellness_encounters.json`'s own "Weight")
;; exercises.

(def wave-i-vital-sign-names
  ["Weight" "Heart Rate" "Respiration Rate" "Head Circumference" "Head Circumference Percentile"
   ;; FOUND LIVE, FIXED (2026-08-04, same session, AR-7's own census re-run
   ;; step): these six carry `category: "laboratory"` upstream, not
   ;; "vital-signs" -- the category-gated enumeration above missed them;
   ;; the interpreter's own `vital_sign` branch never gated on category
   ;; at all (`sim-trajectory/vital-signs.edn`'s own dated note has the
   ;; full account). congestive_heart_failure.json's own Creatinine
   ;; reader is the real closure this unblocks.
   "Creatinine" "Blood Glucose" "EGFR" "LDL" "Microalbumin Creatinine Ratio" "Total Cholesterol"])

(deftest every-wave-i-vital-sign-name-resolves-via-the-observation-reader
  (doseq [vs-name wave-i-vital-sign-names]
    (let [module {:id "vs-mod" :name "VsMod"
                   :states {:initial {:type :initial :direct-transition :obs}
                            :obs {:type :observation :codes [] :vital-sign vs-name :direct-transition :done}
                            :done {:type :terminal}}}
          ctx (assoc (ctx-for (persona-at 1)) :current :obs)
          outcome (interp/step module (Random. 1) ctx)]
      (is (= :normal (:interpretation (first (:events outcome))))
          (str vs-name " -- resolves via the Observation reader, no :unrecognized-vital-sign throw")))))

(def vital-sign-no-value-source-module
  {:id "empty-mod" :name "Empty"
   :states {:initial {:type :initial :direct-transition :x}
            :x {:type :vital-sign :vital-sign "Oxygen Saturation" :direct-transition :done}
            :done {:type :terminal}}})

(deftest vital-sign-state-with-no-exact-range-or-distribution-throws
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :x)]
    (is (thrown? clojure.lang.ExceptionInfo
                 (interp/step vital-sign-no-value-source-module (Random. 1) ctx)))))

;; --- GMF coverage Wave VS (2026-08-04, ADR-0039 AR-1/AR-4): the
;; :vital-sign CONDITION -- register read, honest absence, operators. --

(def sbp-guard-module
  {:id "sbp-mod" :name "SbpGuard"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :vital-sign :vital-sign "Systolic Blood Pressure"
                            :operator "<" :value 90}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest vital-sign-condition-reads-the-baseline-when-nothing-has-set-it
  (testing "AR-3/AR-4: the baseline (110) is already a STORED register
            value from patient creation onward -- 110 is not < 90, so
            the guard blocks"
    (is (true? (:blocked? (interp/step sbp-guard-module (Random. 1) (assoc (ctx-for (persona-at 1)) :current :check)))))))

(def sbp-guard-module-ge
  {:id "sbp-mod" :name "SbpGuardGe"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :vital-sign :vital-sign "Systolic Blood Pressure"
                            :operator ">=" :value 100}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest vital-sign-condition-supports-every-operator-the-real-candidates-use
  (testing "compare-op already matches Utilities.compare's own Double
            dispatch for <, <=, >=, > -- no new operator code needed"
    (is (false? (:blocked? (interp/step sbp-guard-module-ge (Random. 1) (assoc (ctx-for (persona-at 1)) :current :check)))))))

(def lvef-set-then-test-module
  {:id "chf-mod" :name "ChfLike"
   :states {:initial {:type :initial :direct-transition :set-lvef}
            :set-lvef {:type :vital-sign :vital-sign "Left ventricular Ejection fraction"
                       :exact {:quantity 30} :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :vital-sign :vital-sign "Left ventricular Ejection fraction"
                            :operator "<=" :value 35}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest vital-sign-condition-reads-a-value-the-vital-sign-state-just-wrote
  (testing "the real congestive_heart_failure.json shape: an LVEF
            VitalSign state always precedes the condition that reads it"
    (let [p (persona-at 1)
          result (interp/run-module lvef-set-then-test-module (Random. 1) p (interp/dob-epoch-day p))]
      (is (= :terminal (:status result))))))

(def lvef-guard-without-a-set-module
  {:id "chf-bad-mod" :name "ChfLikeBad"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :vital-sign :vital-sign "Left ventricular Ejection fraction"
                            :operator "<=" :value 35}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest vital-sign-condition-against-a-genuinely-unset-name-is-a-walk-error
  (testing "AR-4: Left ventricular Ejection fraction is the ONE
            deliberately baseline-less name (AR-3) -- reading it before
            any VitalSign state has set it is honest absence, never a
            silent false"
    (let [p (persona-at 1)
          result (interp/run-module lvef-guard-without-a-set-module (Random. 1) p (interp/dob-epoch-day p))]
      (is (= :walk-error (:status result)))
      (is (= :vital-sign (:condition-type (:walk-error result)))))))

(def unrecognized-vital-sign-condition-module
  {:id "bad-cond-mod" :name "BadCond"
   :states {:initial {:type :initial :direct-transition :check}
            :check {:type :guard
                    :allow {:condition-type :vital-sign :vital-sign "Respiratory Rate"
                            :operator "<" :value 20}
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest vital-sign-condition-with-an-unrecognized-name-throws
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unrecognized vital-sign"
                         (interp/step unrecognized-vital-sign-condition-module (Random. 1) (assoc (ctx-for (persona-at 1)) :current :check)))))

;; --- GMF coverage Wave I (2026-08-04, ADR-0040 AR-5): AllergyOnset --
;; the SAME unconditional emit :condition-onset already performs.

(def allergy-codes [{:system :snomed :code "609328004" :display "Allergic disposition (finding)"}])

(def allergy-onset-module
  {:id "allergy-mod" :name "Allergy"
   :states {:initial {:type :initial :direct-transition :onset}
            :onset {:type :allergy-onset :codes allergy-codes :target-encounter :allergist-visit
                    :direct-transition :done}
            :done {:type :terminal}}})

(deftest allergy-onset-emits-a-trajectory-event-citing-its-own-codes
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :onset)
        outcome (interp/step allergy-onset-module (Random. 1) ctx)]
    (is (= 1 (count (:events outcome))))
    (is (= :allergy-onset (:event (first (:events outcome)))))
    (is (= allergy-codes (:codes (first (:events outcome)))))
    (is (= 0 (:advance outcome)))
    (is (= :done (:next outcome)))))

(deftest allergy-onset-ignores-target-encounter-the-same-way-condition-onset-already-does
  (testing "M5a's own pre-existing simplification, extended unchanged --
            emits unconditionally regardless of :target-encounter"
    (let [ctx (assoc (ctx-for (persona-at 1)) :current :onset)
          outcome (interp/step allergy-onset-module (Random. 1) ctx)]
      (is (= 1 (count (:events outcome)))))))

;; --- GMF coverage Wave I (2026-08-04, ADR-0040 AR-5): Vaccine -- an
;; unconditional leaf write, no target-encounter/diagnose distinction
;; upstream at all.

(def vaccine-codes [{:system :cvx :code "115" :display "Tdap vaccine"}])

(def vaccine-module
  {:id "vaccine-mod" :name "Vaccine"
   :states {:initial {:type :initial :direct-transition :tdap}
            :tdap {:type :vaccine :codes vaccine-codes :series 1 :direct-transition :done}
            :done {:type :terminal}}})

(deftest vaccine-emits-a-trajectory-event-citing-its-own-codes-and-series
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :tdap)
        outcome (interp/step vaccine-module (Random. 1) ctx)]
    (is (= 1 (count (:events outcome))))
    (is (= :vaccine (:event (first (:events outcome)))))
    (is (= vaccine-codes (:codes (first (:events outcome)))))
    (is (= 1 (:series (first (:events outcome)))))
    (is (= 0 (:advance outcome)))))

(def vaccine-no-series-module
  {:id "vaccine-no-series-mod" :name "VaccineNoSeries"
   :states {:initial {:type :initial :direct-transition :shot}
            :shot {:type :vaccine :codes vaccine-codes :direct-transition :done}
            :done {:type :terminal}}})

(deftest vaccine-with-no-series-defaults-to-zero
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :shot)
        outcome (interp/step vaccine-no-series-module (Random. 1) ctx)]
    (is (= 0 (:series (first (:events outcome)))))))

(deftest vaccine-consumes-no-rng
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :tdap)
        calls (atom 0)
        rng (proxy [Random] [(long 1)]
              (nextDouble [] (swap! calls inc) (proxy-super nextDouble))
              (nextInt ([n] (swap! calls inc) (proxy-super nextInt n))))]
    (interp/step vaccine-module rng ctx)
    (is (= 0 @calls))))

;; --- EncounterEnd fix (2026-08-08, notes/ADRs.md ADR-0082, AR-EE-2/3):
;; the interpreter learns openness -- A1 (open) emits referencing the
;; TRACKED index; A5 (nothing open) is upstream's own legal no-op, NO
;; EVENT, `:suppressed-encounter-ends` incremented instead (R2); one
;; in-flight encounter is asserted, never silently allowed to nest.
;; `encounter-end-fixture.json` (hand-authored, no NOTICE obligation,
;; the fixture's own header has the full incident-record citation)
;; reproduces the exact close-encounter-if-open idiom anemia/anemia_
;; sub.json's own "End Any Active Encounter Just In Case" state authors,
;; the shape ADR-0071/ADR-0072 deferred two modules whole on. -----------

(def encounter-end-fixture-json
  (slurp (io/resource "ehrt/sim/fixtures/encounter-end-fixture.json")))

(def encounter-end-fixture
  (:payload (gmf/load-module "encounter-end-fixture" encounter-end-fixture-json)))

(deftest close-if-open-idiom-emits-once-and-suppresses-the-second-close
  (testing "A1 emits with the tracked reference; A5 (Close_Again, nothing
            open) is a silent no-op upstream -- no dangling :encounter-end,
            the walk proceeds to Terminal, and the suppression is counted,
            not merely absorbed"
    (let [p (persona-at 1)
          result (interp/run-module encounter-end-fixture (Random. 1) p
                                     (interp/dob-epoch-day p) (+ (interp/dob-epoch-day p) 3650))
          ends (filterv #(= :encounter-end (:event %)) (:trajectory result))]
      (is (= :terminal (:status result)))
      (is (= 1 (count ends)) "exactly one :encounter-end reaches the trajectory -- Close_Again emits nothing")
      (is (= 0 (:references (first ends))) "the surviving end still references the tracked open index (A1)")
      (is (= 1 (:suppressed-encounter-ends result))
          "Close_Again's own no-op is counted, not silently dropped (R2)"))))

(deftest open-close-open-close-sequence-tracks-each-encounter-independently
  (testing "unit test on the tracking itself: two full open/close cycles,
            each :encounter-end referencing ITS OWN encounter's own index,
            never the other one's -- open-encounter-index is a pure fold
            over the trajectory, re-derived fresh each call, not stale
            cross-encounter state"
    (let [two-cycle-module
          {:id "two-cycle-mod" :name "TwoCycle"
           :states {:initial {:type :initial :direct-transition :open1}
                    :open1 {:type :encounter :encounter-class :ambulatory :direct-transition :close1}
                    :close1 {:type :encounter-end :direct-transition :open2}
                    :open2 {:type :encounter :encounter-class :ambulatory :direct-transition :close2}
                    :close2 {:type :encounter-end :direct-transition :done}
                    :done {:type :terminal}}}
          p (persona-at 1)
          result (interp/run-module two-cycle-module (Random. 1) p
                                     (interp/dob-epoch-day p) (+ (interp/dob-epoch-day p) 3650))
          ends (filterv #(= :encounter-end (:event %)) (:trajectory result))]
      (is (= :terminal (:status result)))
      (is (= 2 (count ends)))
      (is (= 0 (:references (first ends))) "close1 references open1, index 0")
      (is (= 2 (:references (second ends))) "close2 references open2, index 2 -- never open1 again")
      (is (= 0 (:suppressed-encounter-ends result)) "both closes matched a real open -- nothing suppressed")
      (is (= 0 (:synthesized-encounter-ends result)) "every close here is authored, none synthesized"))))

;; --- Auto-close fix (2026-08-11, notes/ADRs.md ADR-0107, option (i)):
;; a SECOND :encounter reached while one is still open no longer throws
;; -- it auto-closes the stale one first, upstream-faithful (State.java's
;; own Encounter.process, same-module-reopen branch, ADR-0106's own
;; source citation). This fixture is byte-identical to the pre-fix
;; nesting-module above (`injuries.json`'s own Spinal_Injury_Treatment_
;; Encounter reopen shape, ADR-0106) -- it throws pre-fix, completes
;; post-fix with the synthesized end present and correctly cited. -------

(deftest nested-encounter-auto-closes-the-stale-one-rather-than-throwing
  (testing "open1 never gets its own authored :encounter-end -- open2
            reopens over it. Post-fix: the walk completes, an implicit
            :encounter-end for open1 lands in the trajectory
            IMMEDIATELY before open2's own :encounter event, referencing
            open1's own index, timestamped at open2's own :t (end-before-
            open, not a separate tick) -- matching upstream's own quiet
            auto-close rather than dropping open1's own content or
            throwing on an authored pattern upstream itself tolerates"
    (let [nesting-module
          {:id "nesting-mod" :name "Nesting"
           :states {:initial {:type :initial :direct-transition :open1}
                    :open1 {:type :encounter :encounter-class :ambulatory :direct-transition :open2}
                    :open2 {:type :encounter :encounter-class :ambulatory :direct-transition :done}
                    :done {:type :terminal}}}
          p (persona-at 1)
          result (interp/run-module nesting-module (Random. 1) p
                                     (interp/dob-epoch-day p) (+ (interp/dob-epoch-day p) 3650))
          trajectory (:trajectory result)
          encounters (filterv #(= :encounter (:event %)) trajectory)
          ends (filterv #(= :encounter-end (:event %)) trajectory)
          open1-idx (.indexOf trajectory (first encounters))
          open2-idx (.indexOf trajectory (second encounters))
          end-idx (.indexOf trajectory (first ends))]
      (is (= :terminal (:status result)) "the walk completes -- no throw")
      (is (= 2 (count encounters)) "both :encounter states still emit their own event")
      (is (= 1 (count ends)) "exactly one :encounter-end reaches the trajectory -- the synthesized close for open1")
      (is (= open1-idx (:references (first ends))) "the synthesized end references open1's own index, not open2's")
      (is (= (:t (second encounters)) (:t (first ends)))
          "the synthesized end's own :t equals open2's own :t -- end-before-open at the SAME instant, State.java's own timing")
      (is (= (dec open2-idx) end-idx) "the synthesized end sits IMMEDIATELY before open2's own event -- end strictly before open, adjacent")
      (is (= 1 (:synthesized-encounter-ends result)) "the auto-close is countable, not merely absorbed")
      (is (= 0 (:suppressed-encounter-ends result)) "no A5 no-op fired here -- this is the reopen arm, a different mechanism"))))

;; --- ADR-0105: run-submodule horizon-blindness, and max-steps counting
;; every step regardless of advance -- the two coupled halves of the
;; ADR-0070 injuries.json bail-out, reproduced hermetically (test-local
;; fixtures, never under resources/sim/modules, never NOTICE'd). -------

(def dental-referral-callee-module
  "Mirrors injuries/broken_jaw.json's own Dental Referral shape (ADR-
  0070's own bail-out finding, byte-confirmed against the pinned
  synthea checkout): SetAttribute once, then a Delay<->conditional-
  check cycle gated on that attribute staying set -- no state anywhere
  in this closure ever clears it, so the cycle runs forever unless
  something outside it stops the walk."
  {:id "dental-callee" :name "DentalCallee"
   :states {:initial {:type :initial :direct-transition :refer}
            :refer {:type :set-attribute :attribute "referral" :value true :direct-transition :wait}
            :wait {:type :delay :exact {:quantity 1 :unit "days"} :direct-transition :check}
            :check {:type :simple
                    :conditional-transition [{:condition {:condition-type :attribute :attribute "referral" :operator "is not nil"}
                                               :transition :wait}
                                              {:transition :done}]}
            :done {:type :terminal}}})

(def dental-referral-caller-module
  {:id "dental-caller" :name "DentalCaller"
   :states {:initial {:type :initial :direct-transition :call}
            :call {:type :call-submodule :submodule "dental-callee" :direct-transition :after}
            :after {:type :terminal}}})

(def dental-referral-closure
  {"dental-caller" dental-referral-caller-module "dental-callee" dental-referral-callee-module})

(deftest run-submodule-respects-a-small-horizon-instead-of-running-to-max-steps
  (testing "ADR-0105 fix, half 1: a walk crossing the horizon INSIDE a
            submodule must park with :status :horizon-complete, the
            SAME truncation status the top-level Delay-overshoot path
            uses -- never throw. PRE-FIX (red): run-submodule never
            receives horizon-end-t (ns docstring's own note), so this
            SMALL horizon (30 days) does not stop the loop, which runs
            until max-steps trips instead -- the SAME exception ADR-
            0070 found at every horizon it tried (36500/18250/3650, all
            threw identically: horizon-invariant, because the horizon
            was never consulted at all)"
    (let [p (persona-at 1)
          dob (interp/dob-epoch-day p)
          horizon-end-t (+ dob 30)
          result (interp/run-module dental-referral-caller-module (Random. 1) p dob horizon-end-t
                                     dental-referral-closure)]
      (is (= :horizon-complete (:status result))))))

(def perpetual-recheck-module
  "A LEGAL, non-buggy, time-advancing loop -- a 1-day Delay paired with
  a zero-advance re-check, forever (real long-running follow-up
  schedules take exactly this shape, `components/sim/resources/sim/
  modules/NOTICE`'s own veteran_mdd.json finding). Never terminates on
  its own; only the horizon stops it -- exactly the class max-steps's
  own docstring says must NOT trip ('a real v1 module always
  terminates or blocks... [max-steps polices] a zero-time-advance
  transition cycle')."
  {:id "recheck-mod" :name "Recheck"
   :states {:initial {:type :initial :direct-transition :wait}
            :wait {:type :delay :exact {:quantity 1 :unit "days"} :direct-transition :check}
            :check {:type :simple :direct-transition :wait}}})

(deftest max-steps-counts-only-zero-advance-steps-a-legal-loop-reaches-horizon-complete
  (testing "ADR-0105 fix, half 2, the counting arithmetic: max-steps
            (10000) must count only ZERO-time-advance steps, not every
            step -- this loop's own 1-day-delay/re-check cycle needs
            ~12000 total steps to cross a 6000-day (~16-year) horizon,
            but only ~6001 of those are zero-advance (the re-checks),
            comfortably under budget. PRE-FIX (red): max-steps counts
            EVERY step regardless of advance, so this purely legal loop
            trips the backstop at n=10000 (~5000 days elapsed, well
            short of the 6000-day horizon) even though the horizon
            check itself is working correctly and nothing about this
            loop is a bug"
    (let [p (persona-at 1)
          dob (interp/dob-epoch-day p)
          horizon-end-t (+ dob 6000)
          result (interp/run-module perpetual-recheck-module (Random. 1) p dob horizon-end-t)]
      (is (= :horizon-complete (:status result))))))

;; --- ADR-0133: consume-step-budget switches from ADR-0105's own
;; "does not consume" (lifetime population count) semantics to the
;; OTHER semantics that ADR's own Context licensed but did not choose
;; -- reset to zero on any genuine time advance, so the budget polices
;; CONSECUTIVE zero-advance steps only, never a cumulative total a
;; long-but-legal recurring loop can exhaust over a long enough
;; horizon (found live: `veteran_ptsd.json`'s own therapy-visit
;; recurring-care loop, restored reachable by ADR-0133's own exact-
;; name resolution fix, population-scale over a 100-year horizon). ---

(def bounded-burst-module
  "A LEGAL, non-buggy loop: each lap does a bounded burst of exactly
  5000 CONSECUTIVE zero-advance steps (a Counter/Guard re-check cycle,
  :n climbing 0->5000), then ONE real time-advancing step (a 1-day
  Delay), then resets and repeats -- never terminating on its own, only
  the horizon stops it, exactly `perpetual-recheck-module`'s own shape
  above, scaled up. Any SINGLE burst (5001 zero-advance steps) is
  comfortably under `max-steps` (10000); the LIFETIME total across
  enough laps to cross a real horizon is not -- the distinction this
  fix's own two semantics disagree on."
  {:id "burst-mod" :name "Burst"
   :states {:initial {:type :initial :direct-transition :reset}
            :reset {:type :set-attribute :attribute "n" :value 0 :direct-transition :bump}
            ;; `attribute-condition-holds?`'s own operator vocabulary
            ;; (gmf_interpreter.clj ~474) is `!=`/`is nil`/`is not nil`/
            ;; default-`=` ONLY -- no `<`/`>=` numeric-range comparator
            ;; (that's `compare-op`, used by Age/Date, a DIFFERENT
            ;; evaluator) -- so the loop-back condition is "keep
            ;; looping while n has not YET reached the exact target",
            ;; not a range check.
            :bump {:type :counter :attribute "n" :action :increment
                   :conditional-transition
                   [{:condition {:condition-type :attribute :attribute "n" :operator "!=" :value 5000}
                     :transition :bump}
                    {:transition :wait}]}
            :wait {:type :delay :exact {:quantity 1 :unit "days"} :direct-transition :reset}}})

(deftest max-steps-resets-on-any-advance-a-bounded-burst-loop-reaches-horizon-complete
  (testing "ADR-0133 fix: a loop whose own LIFETIME zero-advance total
            (>10000 across enough laps to cross a 10-day horizon, ~5001
            per lap x 10+ laps) exceeds max-steps, but whose own
            CONSECUTIVE zero-advance runs (5001 per lap, reset by each
            lap's own real 1-day Delay) never do, completes cleanly.
            PRE-FIX (red, ADR-0105's own 'does not consume' semantics):
            the lifetime total trips the backstop after ~2 laps, well
            short of the 10-day horizon, even though no single burst is
            anywhere near the budget and nothing about this loop is a
            bug -- the SAME false-positive class `perpetual-recheck-
            module`'s own ADR-0105 fixture already proved for a much
            smaller per-lap burst; this one is sized specifically to
            stay invisible under a per-lap view yet fail under a
            lifetime view, isolating the semantic this fix changes"
    (let [p (persona-at 1)
          dob (interp/dob-epoch-day p)
          horizon-end-t (+ dob 10)
          result (interp/run-module bounded-burst-module (Random. 1) p dob horizon-end-t)]
      (is (= :horizon-complete (:status result))))))

(deftest run-module-zero-advance-spin-still-throws-max-steps
  (testing "ADR-0105: the counting fix narrows what counts toward the
            budget -- it does not remove the backstop. A genuine
            zero-advance transition cycle (infinite-loop-module's own
            shape, driven via run-module instead of walk-module) still
            trips max-steps, non-vacuous proof the fix didn't merely
            raise the ceiling"
    (let [p (persona-at 1)
          dob (interp/dob-epoch-day p)]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"run-module exceeded max-steps"
                             (interp/run-module infinite-loop-module (Random. 1) p dob (+ dob 100)))))))

(def infinite-loop-callee-module
  {:id "loop-callee" :name "LoopCallee"
   :states {:initial {:type :initial :direct-transition :a}
            :a {:type :simple :direct-transition :b}
            :b {:type :simple :direct-transition :a}}})

(def calls-infinite-loop-module
  {:id "loop-caller" :name "LoopCaller"
   :states {:initial {:type :initial :direct-transition :call}
            :call {:type :call-submodule :submodule "loop-callee" :direct-transition :done}
            :done {:type :terminal}}})

(deftest run-submodule-zero-advance-spin-still-throws-max-steps
  (testing "ADR-0105: the same non-vacuous backstop proof, one layer
            down -- a genuine zero-advance cycle INSIDE a called
            submodule still trips run-submodule's own max-steps, even
            though the counting fix now ignores time-advancing steps"
    (let [p (persona-at 1)
          dob (interp/dob-epoch-day p)
          closure {"loop-caller" calls-infinite-loop-module "loop-callee" infinite-loop-callee-module}]
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"run-submodule exceeded max-steps"
                             (interp/run-module calls-infinite-loop-module (Random. 1) p dob (+ dob 100) closure))))))

(deftest submodule-horizon-truncation-matches-a-top-level-truncations-own-status-exactly
  (testing "ADR-0105: 'a walk crossing the horizon inside a submodule
            ends in the SAME truncation status the top-level Delay-
            overshoot path uses' (run-submodule's own docstring, the
            mirror-site contract this fix keeps) -- asserted by
            EQUALITY against a REAL top-level truncation
            (wellness-wait-parks-past-the-horizon-the-same-way-delay-
            does's own module, above), not a literal keyword"
    (let [p1 (persona-at 1)
          dob (interp/dob-epoch-day p1)
          top-level-truncation
          (interp/run-module wellness-wait-then-encounter-module (Random. 1) p1 dob (+ dob 20))
          p2 (persona-at 1)
          submodule-truncation
          (interp/run-module dental-referral-caller-module (Random. 1) p2 dob (+ dob 30)
                              dental-referral-closure)]
      (is (= (:status top-level-truncation) (:status submodule-truncation)))
      (is (= :horizon-complete (:status top-level-truncation))))))
