(ns ehrt.sim.gmf-interpreter-test
  "Red tests for the GMF interpreter core (M5a Task 2, docs/gmf-
  interpreter.md sections 1-2 and 6) -- written before
  ehrt.sim.gmf-interpreter exists (sim/ADR-0004 test-first). Pure,
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
            [ehrt.sim.gmf :as gmf]
            [ehrt.sim.gmf-interpreter :as interp]
            [ehrt.sim.persona :as persona])
  (:import [java.util Random]))

(def fixture-clinic-json
  (slurp (io/resource "ehrt/sim/fixtures/fixture-clinic.json")))

(def fixture-clinic
  (:payload (gmf/load-module "fixture-clinic" fixture-clinic-json)))

(defn- persona-at [seed & [config]]
  (persona/persona (Random. seed) (or config {})))

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

;; --- SetAttribute / Symptom -------------------------------------------------

(deftest set-attribute-writes-a-module-namespaced-key
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :log-onset-attribute)
        outcome (interp/step fixture-clinic (Random. 1) ctx)]
    (is (= true (get-in outcome [:attributes :fixture-clinic/onset-logged])))
    (is (= [] (:events outcome)) "SetAttribute is consumed internally -- no trajectory event")))

(deftest symptom-writes-a-module-namespaced-key-with-a-sampled-severity
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :nasal-congestion-symptom)
        outcome (interp/step fixture-clinic (Random. 5) ctx)
        severity (get-in outcome [:attributes :fixture-clinic/nasal-congestion])]
    (is (<= 10 severity 40))
    (is (= [] (:events outcome)) "Symptom is consumed internally -- no trajectory event, docs/gmf-interpreter.md section 1")))

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
  (testing ":at-least (Synthea's own compound N-of wrapper) is still outside
            v1's vocabulary -- docs/gmf-interpreter.md section 2's own gap
            note, unlike :active-allergy/:active-condition/:active-medication/
            :and, which joined v1 at M5b (this namespace's own updated tests,
            below)"
    (is (thrown? clojure.lang.ExceptionInfo
                 (interp/evaluate-condition "any-mod" (ctx-for (persona-at 1))
                                             {:condition-type :at-least})))))

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
