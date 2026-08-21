(ns ehrt.patient-simulator.vendored-appendicitis-test
  "M7 Task 2: the SECOND real vendored module (resources/modules/
  appendicitis.json, docs/gmf-interpreter.md's own M7 survey
  recommendation -- the only one of 26 modules read that session with
  ZERO state-type gap AND zero condition-vocabulary gap). Written
  test-first (sim/ADR-0004): the FIRST version of this file, run before
  `resources/modules/appendicitis.json` existed, went RED for the
  simple, expected reason (the resource does not exist yet) -- once the
  file is vendored, every assertion below is expected to go GREEN
  outright, unlike sinusitis.json's own M5b vendoring (which surfaced
  six real gaps against the real loader/interpreter). That asymmetry IS
  the finding: this module's survey score was cleaner than sinusitis's
  own needed to be, so the real loader/interpreter has nothing left to
  contradict.

  Also proves, empirically, docs/gmf-interpreter.md's own M7 finding
  that compile-trajectory's `encounter-closed?` mechanism drops
  appendicitis.json's real inpatient/surgical content: the vendored
  module's own interpreter-level trajectory carries the inpatient
  encounter and Appendectomy procedure in full (interpreter-level
  evidence, this file), while ehrt.patient-simulator.compile-trajectory-test's
  own synthetic-event test proves the compiled IR stops at the first
  discharge (compile-time evidence, that file) -- two independent tests
  at two different layers, for the same real gap."
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

(def appendicitis-json (slurp (io/resource "sim/modules/appendicitis.json")))
(def sinusitis-json (slurp (io/resource "sim/modules/sinusitis.json")))
(def fixture-clinic-json (slurp (io/resource "ehrt/sim/fixtures/fixture-clinic.json")))

(deftest vendored-appendicitis-loads-and-validates
  (let [loaded (gmf/load-module "appendicitis" appendicitis-json)]
    (is (result/ok? loaded)
        (str "expected the vendored module to validate against the v1 subset; got " (pr-str loaded)))))

(deftest full-vendored-set-registers-together-with-zero-attribute-collisions
  (testing "the registry's first real THREE-module load: fixture-clinic (test
            fixture) + sinusitis + appendicitis (both real vendored modules) --
            module-namespaced attributes (docs/gmf-interpreter.md section 5)
            make cross-module collisions structurally impossible, checked here
            for the full real vendored set, not just a pair"
    (let [appendicitis (:payload (gmf/load-module "appendicitis" appendicitis-json))
          sinusitis (:payload (gmf/load-module "sinusitis" sinusitis-json))
          fixture-clinic (:payload (gmf/load-module "fixture-clinic" fixture-clinic-json))
          registry (-> (gmf/empty-registry)
                       (gmf/register "fixture-clinic" fixture-clinic) :payload
                       (gmf/register "sinusitis" sinusitis) :payload
                       (gmf/register "appendicitis" appendicitis))]
      (is (result/ok? registry))
      (let [listed (gmf/loaded-modules (:payload registry))]
        (is (= #{"fixture-clinic" "sinusitis" "appendicitis"} (into #{} (map :id) listed)))
        (is (empty? (apply set/intersection (map :attributes listed))))))))

(def ^:private appendicitis (:payload (gmf/load-module "appendicitis" appendicitis-json)))

(defn- person [seed sex] (assoc (sim-model/persona (Random. seed) {}) :sex sex))

;; Old enough that every age-bracket branch (1-17/18-44/45-64/65+) has had
;; room to fire and, per-branch, the appendectomy's own recovery delay
;; (1-5 days) has had room to complete within the horizon window below.
(defn- registration-t-for [persona] (+ (interp/dob-epoch-day persona) (* 365 70)))

;; A generous horizon: appendicitis.json's own Pre_appendicitis age delay
;; alone can run up to 99 years (the Ages_65_Plus bracket's own :range) --
;; unlike sinusitis.json's monthly-tick onset loop, this module's real
;; content lives entirely inside the age-delay chain BEFORE any encounter,
;; so a real walk needs the full window to reach it, not just a modest
;; post-registration slice.
(def ^:private horizon-window-days (* 365 80))

(defspec vendored-appendicitis-walks-to-a-bounded-horizon-without-throwing 200
  (prop/for-all [seed gen/large-integer
                 sex (gen/elements [:female :male])]
    (let [p (person seed sex)
          reg-t (registration-t-for p)
          result (interp/run-module appendicitis (Random. seed) p reg-t (+ reg-t horizon-window-days))]
      (contains? #{:terminal :blocked :horizon-complete} (:status result)))))

;; Only ~8.6%/6.7% (male/female) of patients EVER onset appendicitis at
;; all (a single one-shot distributed_transition off Initial, not a
;; recurring monthly-tick loop the way sinusitis.json's own onset is) --
;; finding a real hit needs many candidate seeds. Sequential/nearby seeds
;; (`(range N)`) are UNSAFE for this: java.util.Random's first draw has
;; poor avalanche for small/close seed values (confirmed directly this
;; session -- seeds 1-29 ALL landed on the SAME >91%-probability branch),
;; so candidates are drawn from a well-mixed source (a separate, fixed-
;; seed Random's own .nextLong stream) instead, per this project's own
;; documented gotcha for exactly this shape of statistical search.
(defn- well-mixed-candidate-seeds [n]
  (let [mixer (Random. 20260727)]
    (repeatedly n #(.nextLong mixer))))

(deftest vendored-appendicitis-real-onset-reaches-both-encounters-in-the-uncompiled-trajectory
  (testing "at the INTERPRETER layer (before compile-trajectory ever runs),
            a real onset carries both the emergency admission AND the
            inpatient surgical encounter, in full -- the drop
            docs/gmf-interpreter.md's own M7 finding describes happens at
            COMPILE time, not here; this test is the control proving the
            content genuinely exists upstream of that drop"
    (let [seed-with-onset (first (keep (fn [seed]
                                          (let [p (person seed :male)
                                                reg-t (registration-t-for p)
                                                result (interp/run-module appendicitis (Random. seed) p reg-t
                                                                           (+ reg-t horizon-window-days))
                                                classes (keep :encounter-class (:trajectory result))]
                                            (when (= [:emergency :inpatient] classes) seed)))
                                        (well-mixed-candidate-seeds 400)))]
      (is (some? seed-with-onset) "expected at least one well-mixed candidate seed to reach a full appendectomy")
      (let [p (person seed-with-onset :male)
            reg-t (registration-t-for p)
            result (interp/run-module appendicitis (Random. seed-with-onset) p reg-t (+ reg-t horizon-window-days))
            events (:trajectory result)]
        (is (some #(= :procedure (:event %)) events) "expected the Appendectomy procedure in the real trajectory")
        (is (= [:emergency :inpatient] (keep :encounter-class events)))))))
