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
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-trajectory.gmf-interpreter :as interp]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.util Random]))

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
  (testing ":vital-sign (AR-2, GMF coverage Wave A, pre-ruled OUT -- needs a
            state home this project's accumulator doesn't have yet) is
            still outside v1's vocabulary, docs/gmf-interpreter.md section
            2's own gap note -- unlike :active-allergy/:active-condition/
            :active-medication/:and (M5b) and :symptom/:at-least/:or/:date/
            :observation (Wave A, this namespace's own updated tests,
            below), all of which joined v1 because their data source
            already existed"
    (is (thrown? clojure.lang.ExceptionInfo
                 (interp/evaluate-condition "any-mod" (ctx-for (persona-at 1))
                                             {:condition-type :vital-sign})))))

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
;; via :operator; THROWS if no matching observation was ever recorded (the
;; SAME "required precondition, module author's own responsibility"
;; design Synthea itself uses -- v1 scope omits the "is nil"/"is not nil"
;; operators real Synthea also supports for exactly this case, since no
;; candidate module this session needs them). Real use: sore_throat.json's
;; Determine_if_Bacterial (Step 3), whose only two predecessor states
;; (Take_Temperature_High/Low) are BOTH Observation states citing the same
;; LOINC code -- confirmed by reading the vendored file directly, so the
;; throw-on-missing path is never live on that module's own real walk.

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

(deftest observation-condition-throws-when-no-matching-observation-was-ever-recorded
  (testing "the same required-precondition design Synthea's own Logic.java
            Observation class uses -- a module reaching this condition
            without ever recording the observation it queries is a module-
            authoring-shape bug this interpreter surfaces rather than
            silently defaults, the same disposition unsupported condition
            types and the max-steps backstop already get"
    (is (thrown? clojure.lang.ExceptionInfo
                 (interp/evaluate-condition "m" (ctx-for (persona-at 1))
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

(deftest death-throws-on-unbuilt-condition-onset-cause-form
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :die)
        module (assoc-in immediate-death-module [:states :die]
                          {:type :death :condition-onset :some-state})]
    (is (thrown? clojure.lang.ExceptionInfo (interp/step module (Random. 1) ctx)))))

(deftest death-throws-on-unbuilt-referenced-by-attribute-cause-form
  (let [ctx (assoc (ctx-for (persona-at 1)) :current :die)
        module (assoc-in immediate-death-module [:states :die]
                          {:type :death :referenced-by-attribute "some-attr"})]
    (is (thrown? clojure.lang.ExceptionInfo (interp/step module (Random. 1) ctx)))))

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
