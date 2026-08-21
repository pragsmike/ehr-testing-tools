(ns ehrt.patient-simulator.vendored-module-test
  "M5b Task 1: the FIRST real vendored module (resources/modules/
  sinusitis.json, sim/ADR-0013's own curation criterion, docs/gmf-
  interpreter.md's own recommendation) loads and validates against the
  v1 subset, registers cleanly alongside the M5a fixture (no attribute
  collisions), and can be walked to completion by the interpreter for
  many seeds without throwing.

  Written test-first (sim/ADR-0004): the FIRST version of this file (before
  ehrt.patient-simulator.gmf/ehrt.patient-simulator.gmf-interpreter were extended this
  session) asserted the same success shape below and went RED for real
  reasons -- `gmf/load-module` rejected the file outright with
  :unsupported-state-type (the vendored file's own Device/DeviceEnd
  Nebulizer content, docs/gmf-interpreter.md section 1's own deferred-
  type table), and even once that was fixed, a full walk threw
  ex-info'd 'unsupported condition type' for every patient who ever
  reached `Wait_for_condition_to_resolve` (an And/Active-Medication/
  Active-Condition condition on the module's own MANDATORY post-
  encounter path, not an excludable tail the way Device/DeviceEnd is) --
  see docs/gmf-interpreter.md section 1/2's own updated tables and this
  session's own M5b findings note for the full account of both gaps and
  how each was resolved (Device/DeviceEnd join v1 as consumed-internally
  states; Active Condition/Active Medication/And join v1's condition
  vocabulary as log-query predicates, architecturally the same shape
  PriorState already established; Active Allergy is a documented,
  always-false simplification -- this project's Persona has no allergy
  concept yet)."
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.patient-simulator.gmf-interpreter :as interp]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.util Random]))

(def sinusitis-json (slurp (io/resource "sim/modules/sinusitis.json")))
(def fixture-clinic-json (slurp (io/resource "ehrt/sim/fixtures/fixture-clinic.json")))

(deftest vendored-sinusitis-loads-and-validates
  (let [loaded (gmf/load-module "sinusitis" sinusitis-json)]
    (is (result/ok? loaded)
        (str "expected the vendored module to validate against the v1 subset; got " (pr-str loaded)))))

(deftest vendored-sinusitis-registers-cleanly-alongside-fixture-clinic
  (testing "no module-id collision, no attribute collision -- the
            namespacing rule (docs/gmf-interpreter.md section 5) makes
            cross-module attribute collisions structurally impossible"
    (let [sinusitis (:payload (gmf/load-module "sinusitis" sinusitis-json))
          fixture-clinic (:payload (gmf/load-module "fixture-clinic" fixture-clinic-json))
          registry (-> (gmf/empty-registry)
                       (gmf/register "fixture-clinic" fixture-clinic) :payload
                       (gmf/register "sinusitis" sinusitis))]
      (is (result/ok? registry))
      (let [listed (gmf/loaded-modules (:payload registry))]
        (is (= #{"fixture-clinic" "sinusitis"} (into #{} (map :id) listed)))
        (is (empty? (apply set/intersection (map :attributes listed))))))))

(def ^:private sinusitis (:payload (gmf/load-module "sinusitis" sinusitis-json)))

(defn- adult [seed] (assoc (sim-model/persona (Random. seed) {}) :sex :female))

;; A registration instant old enough that the module has had real room
;; to onset at least once (Potential_Onset's own 1%-ish per-month-tick
;; onset probability, docs/gmf-interpreter.md) -- 30 years post-DOB.
(defn- registration-t-for [persona] (+ (interp/dob-epoch-day persona) (* 365 30)))

;; A modest horizon window past registration -- this project's own
;; encounter-horizon scope (sim/ADR-0007 point 3), not a lifelong walk. The
;; vendored module has NO Terminal state at all (a real finding: unlike
;; the hand-written fixture, sinusitis.json is authored for Synthea's
;; own lifelong tick engine and never reaches Terminal) -- `run-module`'s
;; own optional `horizon-end-t` bound is exactly the mechanism M5b's real
;; engine wiring depends on to keep every real vendored-module walk
;; finite, never a max-steps runaway.
(def ^:private horizon-window-days 90)

(defspec vendored-sinusitis-walks-to-a-bounded-horizon-without-throwing 200
  (prop/for-all [seed gen/large-integer]
    (let [p (adult seed)
          reg-t (registration-t-for p)
          result (interp/run-module sinusitis (Random. seed) p reg-t (+ reg-t horizon-window-days))]
      (contains? #{:terminal :blocked :horizon-complete} (:status result)))))

(deftest vendored-sinusitis-device-states-are-pass-through-no-trajectory-event
  (testing "Device/DeviceEnd join v1 as consumed-internally states (like
            Simple) -- no trajectory event, no attribute write, ordinary
            transition resolution"
    (let [seed 42
          p (adult seed)
          reg-t (registration-t-for p)
          result (interp/run-module sinusitis (Random. seed) p reg-t (+ reg-t 365))]
      (is (not (some #(#{:device :device-end} (:event %)) (:trajectory result)))))))
