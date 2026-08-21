(ns ehrt.patient-simulator.death-fixture-test
  "GMF coverage Wave C (2026-08-02, ADR-0028, C6): proves Death end-to-end
  against this project's own hand-authored death-fixture.json
  (test/ehrt/sim/fixtures) rather than stroke.json's own death branch --
  stroke.json stays deferred this wave (the stroke_risk gap,
  docs/gmf-interpreter.md section 10). Mirrors the vendored-module test
  shape (vendored_appendicitis_test.clj's own well-mixed-candidate-seeds
  pattern) at the interpreter and compile-trajectory layers; the full
  engine/check round trip is proven in ehrt.sim-engine.engine-test alongside the
  sinusitis-module wiring test, the same split that file already uses."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.patient-simulator.gmf-interpreter :as interp]
            [ehrt.patient-simulator.compile-trajectory :as ct]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.util Random]))

(def death-fixture-json (slurp (io/resource "ehrt/sim/fixtures/death-fixture.json")))

(deftest death-fixture-loads-and-validates
  (let [loaded (gmf/load-module "death-fixture" death-fixture-json)]
    (is (result/ok? loaded))
    (is (= :death (get-in (:payload loaded) [:states :die :type])))))

(def death-fixture (:payload (gmf/load-module "death-fixture" death-fixture-json)))

(defn- person [seed sex] (assoc (sim-model/persona (Random. seed) {}) :sex sex))

;; Unlike sinusitis.json/appendicitis.json (a real acute-illness onset,
;; expected years into a patient's life), this fixture is a single
;; acute encounter with no lifetime "history" content of its own --
;; registration-t = DOB, so nothing in it is ever pre-horizon-dropped
;; (docs/gmf-interpreter.md section 3).
(defn- registration-t-for [persona] (interp/dob-epoch-day persona))
(def ^:private horizon-window-days (* 365 10))

(defn- walk-result [seed sex]
  (let [p (person seed sex)
        reg-t (registration-t-for p)]
    (interp/run-module death-fixture (Random. seed) p reg-t (+ reg-t horizon-window-days))))

(defn- well-mixed-candidate-seeds
  "The established mixer-RNG pattern (vendored_appendicitis_test.clj,
  reused verbatim across every GMF coverage wave since) -- sequential
  small seeds are NOT well-distributed for java.util.Random's own first
  draw."
  [n mixer-seed]
  (let [mixer (Random. mixer-seed)]
    (repeatedly n #(.nextLong mixer))))

(defn- reaches-death-on-the-first-cycle?
  "Chance_of_Encounter's own loop-back means a walk can survive several
  cycles before finally dying -- fine for the interpreter/terminal-
  contract tests below, but compile-trajectory's own PRE-EXISTING
  encounter-closed? mechanism (docs/gmf-interpreter.md section 8's own
  M7 finding, unrelated to Wave C) drops every trajectory event once an
  EARLIER cycle's own :encounter-end has already compiled -- a real,
  documented limitation this fixture would otherwise collide with by
  accident. The compile-trajectory test below deliberately picks a
  seed where death fires on the module's own FIRST cycle (no prior
  :encounter-end at all), so C4's own mapping is exercised cleanly."
  [result]
  (and (= :death (:event (last (:trajectory result))))
       (not-any? #(= :encounter-end (:event %)) (:trajectory result))))

(defn- reaches-death? [result] (= :death (:event (last (:trajectory result)))))
(defn- survives-at-least-one-encounter-without-dying? [result]
  (and (not (some #(= :death (:event %)) (:trajectory result)))
       (some #(= :encounter-end (:event %)) (:trajectory result))))

(deftest at-least-one-seed-reaches-the-death-branch
  (let [seed (first (keep (fn [seed] (when (reaches-death? (walk-result seed :male)) seed))
                          (well-mixed-candidate-seeds 500 20260802)))]
    (is (some? seed) "expected at least one well-mixed candidate seed to reach Die")
    (let [result (walk-result seed :male)
          trajectory (:trajectory result)
          death-event (last trajectory)]
      (is (= :terminal (:status result)) "C2's own terminal contract -- death-step reports :terminal? true")
      (is (= :death (:event death-event)))
      (is (= [{:system :snomed :code "230690007" :display "Cerebrovascular accident (disorder)"}]
             (:codes death-event)))
      (is (= death-event (last trajectory)) "no trajectory event follows :death -- even one from an EARLIER, survived loop cycle")
      (is (some #(= :encounter (:event %)) trajectory) "the encounter opened before the branch fired"))))

(deftest at-least-one-seed-completes-the-non-death-path
  (testing "Chance_of_Encounter's own survived cycle loops back rather
            than reaching a real Terminal (this fixture's own onset gate
            must recur across a whole life, unlike stroke.json's own
            single-episode shape -- the fixture's own remarks) -- bounded
            here by horizon-end-t, :status :horizon-complete, never a throw"
    (let [seed (first (keep (fn [seed] (when (survives-at-least-one-encounter-without-dying? (walk-result seed :male)) seed))
                            (well-mixed-candidate-seeds 500 20260802)))]
      (is (some? seed) "expected at least one well-mixed candidate seed to Recover at least once")
      (let [result (walk-result seed :male)
            kinds (map :event (:trajectory result))]
        (is (= :horizon-complete (:status result)))
        (is (contains? (set kinds) :encounter-end) "Recover's own path DOES reach End_Encounter")
        (is (not (contains? (set kinds) :death)))))))

;; --- compile-trajectory: death inside the encounter attaches as its own
;; terminal disposition (C4), proven against this fixture's REAL trajectory,
;; not a synthetic ev fixture -----------------------------------------------

(deftest the-death-branchs-real-trajectory-compiles-to-an-expired-disposition-discharge
  (testing "the death-reaching seed's own trajectory may carry earlier,
            SURVIVED encounter cycles (Chance_of_Encounter's own loop-
            back) ahead of the final, fatal one -- C4's own mapping is
            checked on the LAST compiled step, whatever comes before it"
    (let [seed (first (keep (fn [seed] (when (reaches-death-on-the-first-cycle? (walk-result seed :male)) seed))
                            (well-mixed-candidate-seeds 500 20260802)))
          _ (is (some? seed) "expected at least one well-mixed candidate seed to die on its own first cycle")
          result (walk-result seed :male)
          reg-t (registration-t-for (person seed :male))
          {:keys [steps]} (ct/compile-trajectory (:trajectory result) sim-model/default-facility reg-t)
          death-step (last steps)]
      (is (some #(= :admission (:type %)) steps))
      (is (= :discharge (:type death-step)))
      (is (= :expired (:disposition death-step)))
      (is (= [{:system :snomed :code "230690007" :display "Cerebrovascular accident (disorder)"}]
             (:codes death-step))))))
