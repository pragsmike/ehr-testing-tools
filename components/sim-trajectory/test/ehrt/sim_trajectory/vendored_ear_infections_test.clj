(ns ehrt.sim-trajectory.vendored-ear-infections-test
  "GMF coverage Wave B payoff (2026-08-02, ADR-0027): the FOURTH real
  vendored module, and the first CLOSURE (root + two called submodules,
  `resources/modules/NOTICE`'s own new table rows) -- Step 1's own
  characterization found `ear_infections.json`'s closure clean of every
  Wave-D-scoped deferred type (the only state-type gap is CallSubmodule
  itself, Wave B's own reason for existing), at the cost of four
  mandatory-path findings this Wave already resolved: MedicationOrder's
  own assign-to-attribute / MedicationEnd's own referenced-by-attribute
  (Step 2c), Attribute condition's own is-nil/is-not-nil operators
  (Step 2c), and two encounter-class loader normalizations (Step 2e).

  Written test-first (sim/ADR-0004): the FIRST version of this file,
  run before the vendored resources existed, went RED for the expected
  reason (the resources do not exist yet). `urinary_tract_infections.
  json` is NOT vendored this wave -- its own real closure (twelve
  files, not the four the original wave plan anticipated) is dirty in
  every branch with Wave-D-scoped deferred types
  (`DiagnosticReport`/`MultiObservation`), per D6
  (docs/gmf-interpreter.md section 9's own full account)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-trajectory.gmf-interpreter :as interp]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.util Random]))

(def ear-infections-json (slurp (io/resource "sim/modules/ear_infections.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def loaded-closure (gmf/load-closure "ear-infections" ear-infections-json resolve-call-path))

(deftest vendored-ear-infections-closure-loads-clean
  (testing "load-clean over the WHOLE closure -- root plus both called
            submodules, D3's own all-or-nothing gate extended"
    (is (result/ok? loaded-closure)
        (str "expected the vendored closure to validate against the v1 subset; got " (pr-str loaded-closure)))
    (is (= #{"ear-infections" "medications/ear_infection_antibiotic" "medications/otc_pain_reliever"}
           (into #{} (keys (:modules (:payload loaded-closure))))))))

(def modules (:modules (:payload loaded-closure)))
(def ear-infections (get modules "ear-infections"))

(deftest full-vendored-set-registers-together-with-zero-attribute-collisions
  (testing "the registry's first real FIVE-module load: fixture-clinic
            (test fixture) + sinusitis + appendicitis + sore-throat +
            ear-infections (four real vendored root modules) -- module-
            namespaced attributes (docs/gmf-interpreter.md section 5)
            make cross-module collisions structurally impossible,
            checked here for the full real vendored root-module set
            (the ear_infections closure's OWN two submodules are a
            separate, root-scoped concern at RUNTIME -- section 5's own
            dated note -- not a load-time registry collision risk)"
    (let [fixture-clinic-json (slurp (io/resource "ehrt/sim/fixtures/fixture-clinic.json"))
          sinusitis-json (slurp (io/resource "sim/modules/sinusitis.json"))
          appendicitis-json (slurp (io/resource "sim/modules/appendicitis.json"))
          sore-throat-json (slurp (io/resource "sim/modules/sore_throat.json"))
          registry (-> (gmf/empty-registry)
                       (gmf/register "fixture-clinic" (:payload (gmf/load-module "fixture-clinic" fixture-clinic-json))) :payload
                       (gmf/register "sinusitis" (:payload (gmf/load-module "sinusitis" sinusitis-json))) :payload
                       (gmf/register "appendicitis" (:payload (gmf/load-module "appendicitis" appendicitis-json))) :payload
                       (gmf/register "sore-throat" (:payload (gmf/load-module "sore-throat" sore-throat-json))) :payload
                       (gmf/register "ear-infections" ear-infections))]
      (is (result/ok? registry)))))

(defn- person [seed sex] (assoc (sim-model/persona (Random. seed) {}) :sex sex))

;; Onset is a monthly-tick, sub-1%-probability distributed transition off
;; No_Infection (the SAME shape sinusitis.json's/sore_throat.json's own
;; onset loops use) -- 25 years of registration offset gives real room
;; for it to fire at least once over a realistic history-phase horizon.
(defn- registration-t-for [persona] (+ (interp/dob-epoch-day persona) (* 365 25)))
(def ^:private horizon-window-days (* 365 10))

(defn- walk-result [seed sex]
  (let [p (person seed sex)
        reg-t (registration-t-for p)]
    (interp/run-module ear-infections (Random. seed) p reg-t (+ reg-t horizon-window-days) modules)))

(defspec vendored-ear-infections-walks-to-a-bounded-horizon-without-throwing 200
  (prop/for-all [seed gen/large-integer
                 sex (gen/elements [:female :male])]
    (contains? #{:terminal :blocked :horizon-complete} (:status (walk-result seed sex)))))

(deftest vendored-ear-infections-walk-is-deterministic-for-the-same-seed
  (testing "D4: whole-walk reproducibility extends over closures -- walk
            with closure, twice, identical"
    (let [r1 (walk-result 20260802 :female)
          r2 (walk-result 20260802 :female)]
      (is (= (:trajectory r1) (:trajectory r2))))))

;; --- AR obligation (iii): at least one walk reaching THROUGH a
;; submodule -- medication events with call-path citations -----------------

(defn- well-mixed-candidate-seeds
  "Sequential small Random seeds are NOT well-distributed for their own
  first draw (confirmed live, Step 2d's own commit) -- the same mixer-
  RNG fix `vendored_sore_throat_test.clj` already established, reused
  here rather than re-solved."
  [n mixer-seed]
  (let [mixer (Random. mixer-seed)]
    (repeatedly n #(.nextLong mixer))))

(defn- medication-order-events [trajectory] (filter #(= :medication-order (:event %)) trajectory))
(defn- medication-end-events [trajectory] (filter #(= :medication-end (:event %)) trajectory))

(deftest a-walk-reaching-through-a-called-submodule-carries-call-path-citations
  (testing "D2: every event emitted inside a submodule cites the full
            call path, root-first -- real vendored content, not the
            synthetic call/return fixture Step 2c's own unit tests use"
    (let [seed (first (keep (fn [seed]
                               (let [result (walk-result seed :male)]
                                 (when (seq (medication-order-events (:trajectory result)))
                                   seed)))
                             (well-mixed-candidate-seeds 2000 20260802)))]
      (is (some? seed) "expected at least one well-mixed candidate seed to reach a called submodule's own MedicationOrder")
      (let [result (walk-result seed :male)
            order-events (medication-order-events (:trajectory result))
            end-events (medication-end-events (:trajectory result))]
        (is (seq order-events))
        (testing "every order event was emitted INSIDE the call -- cites the full root-first path"
          (is (every? (fn [e] (= "ear-infections" (first (:call-path e)))) order-events))
          (is (every? (fn [e] (contains? #{"medications/ear_infection_antibiotic" "medications/otc_pain_reliever"}
                                          (second (:call-path e))))
                       order-events))
          (is (every? (fn [e] (= (:module e) (second (:call-path e)))) order-events)))
        (testing "the ROOT module's own MedicationEnd states resolve the cross-module referenced-by-attribute
                  back to a real order event's own trajectory index, not nil"
          (is (every? (fn [e] (some? (:references e))) end-events))
          (is (every? (fn [e] (nil? (:call-path e))) end-events)
              "MedicationEnd runs in the ROOT module, outside any active call"))))))

;; --- GMF coverage Wave G (2026-08-03, ADR-0037 AR-3/AR-7): the
;; create-now substitution retired -- Next_Wellness_Encounter now waits
;; for a real cadence tick rather than firing the instant medications
;; end. -----------------------------------------------------------------

(defn- wellness-events [trajectory]
  (filter #(and (= :encounter (:event %)) (= :wellness (:encounter-class %))) trajectory))

(deftest next-wellness-encounter-now-resolves-at-a-real-cadence-tick-not-immediately
  (testing "ADR-0037 AR-3: End_Ear_Infection_Medications' own zero-time
            fallback transition to Next_Wellness_Encounter used to fire
            an :outpatient-visit AT THE SAME instant medications ended
            (the retired create-now substitution, ADR-0031 AR-5(b)) --
            it now waits for next-wellness-tick, strictly later"
    (let [seed (first (keep (fn [seed]
                               (let [result (walk-result seed :male)]
                                 (when (seq (wellness-events (:trajectory result)))
                                   seed)))
                             (well-mixed-candidate-seeds 2000 20260803)))]
      (is (some? seed) "expected at least one well-mixed candidate seed to reach the wellness encounter")
      (let [result (walk-result seed :male)
            trajectory (:trajectory result)
            wellness-event (first (wellness-events trajectory))
            last-med-end-t (apply max (map :t (medication-end-events trajectory)))]
        (is (some? wellness-event))
        (is (> (:t wellness-event) last-med-end-t)
            "the wellness encounter now fires strictly AFTER the last medication ends, never at the same instant")))))
