(ns ehrt.sim-trajectory.vendored-tjr-test
  "GMF coverage Wave D stage D3 payoff, second entry (2026-08-02,
  ADR-0029): the EIGHTH real vendored module, `total_joint_
  replacement.json` (`resources/modules/NOTICE`'s own new table rows)
  -- deferred at Wave D stage D2 (its own `joint_replacement` attribute
  gap resolved that stage, but a SECOND, independent blocker found
  live: `Joint_Replacement_Guard`'s own compound Age condition,
  `{:and [Attribute joint_replacement is-not-nil, Age > 50 years]}`,
  outside `age-guard-jump-days`'s own v1 analytical-resolution shape --
  the walk blocked PERMANENTLY at age 0). Landed this session (D3e/H4:
  sound-jump-or-escalate). Full closure re-verification (by hash) and
  the compound-Guard design: `docs/gmf-interpreter.md` section 14.

  `initial-attributes` seeding, disclosed per H7's own rider (ADR-0029):
  `joint_replacement` is DELEGATED content this project has no in-
  project source for (`total_joint_replacement.json`'s own `remarks`
  block: 'Currently joint replacements are triggered by the
  \"joint_replacement\" attribute set by the osteoarthritis and
  rheumatoid arthritis modules' -- neither vendored, out of this
  session's own scope). This test supplies it directly as an authored,
  provenance-cited starting attribute via `run-module`'s own
  `initial-attributes` arity (D2) -- a walk-entry input standing in for
  those two out-of-closure writers, per-use here, NOT a general
  cross-module channel (cross-module facts still travel through
  clinical state, ADR-0027 D1).

  Interpreter-layer coverage only -- the same standing, already-
  disclosed full-pipeline gap `ehrt.sim-trajectory.vendored-uti-test`'s
  own docstring names applies here too (no compile-trajectory/engine/
  emit round trip exists for ANY closure-having module vendored to
  date). G3's own silence assertion (CarePlan events produce zero HL7
  messages) is proven at the `sim-emit-hl7` unit-test layer already
  (D2, against a synthetic fixture, not a real vendored closure) --
  not re-provable here without that same missing engine-closure
  wiring; this test proves the interpreter-layer SPAN itself
  (:care-plan-start paired with its own :care-plan-end, correct
  :references linkage), which is what G3's own silence claim actually
  depends on existing."
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

(def tjr-json (slurp (io/resource "sim/modules/total_joint_replacement.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def loaded-closure (gmf/load-closure "total-joint-replacement" tjr-json resolve-call-path))

(deftest vendored-tjr-closure-loads-clean
  (testing "load-clean over the WHOLE closure -- root plus all three
            called submodules, D3's own all-or-nothing gate"
    (is (result/ok? loaded-closure)
        (str "expected the vendored closure to validate against the v1 subset; got " (pr-str loaded-closure)))
    (is (= #{"total-joint-replacement" "medications/moderate_opioid_pain_reliever"
             "total_joint_replacement/functional_status_assessments" "dme/wheelchair_end"}
           (into #{} (keys (:modules (:payload loaded-closure))))))))

(def modules (:modules (:payload loaded-closure)))
(def tjr (get modules "total-joint-replacement"))

(defn- person [seed] (sim-model/persona (Random. seed) {}))

;; `Joint_Replacement_Guard`'s own compound condition (Attribute is-not-
;; nil AND Age > 50 years) -- `joint_replacement` seeded via
;; `initial-attributes` (this file's own docstring has the full
;; disclosure) holds from :t = DOB onward, so the ONLY thing blocking
;; progress is the Age sub-condition; H4's own sound-jump resolves it
;; analytically to age 51. A generous horizon past that (60 years)
;; comfortably covers the guard jump plus the whole post-op CarePlan
;; cycle that follows it.
(defn- walk-result [seed joint]
  (let [p (person seed)
        reg-t (interp/dob-epoch-day p)
        horizon-end-t (+ reg-t (* 365 60))]
    (interp/run-module tjr (Random. seed) p reg-t horizon-end-t modules {:total-joint-replacement/joint-replacement joint})))

(defspec vendored-tjr-walks-to-a-bounded-horizon-without-throwing 200
  (prop/for-all [seed gen/large-integer
                 joint (gen/elements ["knee" "hip"])]
    (contains? #{:terminal :blocked :horizon-complete} (:status (walk-result seed joint)))))

(deftest vendored-tjr-walk-is-deterministic-for-the-same-seed
  (let [r1 (walk-result 20260802 "knee")
        r2 (walk-result 20260802 "knee")]
    (is (= (:trajectory r1) (:trajectory r2)))))

;; --- H4/H6: a walk that provably ADVANCES past the compound age guard ------

(deftest seeded-joint-replacement-attribute-lets-the-walk-advance-past-age-0
  (testing "D2's own fix-forward finding, closed: before H4, this same
            seeding blocked PERMANENTLY at age 0, zero trajectory events"
    (let [result (walk-result 1 "knee")]
      (is (not= :blocked (:status result))
          "the compound-Guard jump (H4) resolves the Age sub-condition analytically once joint_replacement holds")
      (is (seq (:trajectory result)) "real trajectory content, not an empty walk"))))

;; --- H6: the care-plan span, correct :references linkage -------------------

(defn- reaches-care-plan-end? [result] (some #(= :care-plan-end (:event %)) (:trajectory result)))

(deftest a-walk-carries-the-full-care-plan-span-with-correct-references
  (testing "Post_Op_CarePlan (:care-plan-start) paired with
            End_Post_Op_CarePlan (:care-plan-end, R2(b)'s own pair-
            mirror) -- TJR's own reason for existing this stage"
    (let [seed (first (keep (fn [seed] (when (reaches-care-plan-end? (walk-result seed "knee")) seed))
                            (range 1 50)))]
      (is (some? seed) "expected at least one seed to complete the post-op CarePlan cycle within the horizon")
      (let [{:keys [trajectory]} (walk-result seed "knee")
            start-idx (first (keep-indexed (fn [i e] (when (= :care-plan-start (:event e)) i)) trajectory))
            end-event (first (filter #(= :care-plan-end (:event %)) trajectory))]
        (is (some? start-idx) "expected a :care-plan-start event")
        (is (= [{:system :snomed :code "737567002" :display "Major surgery care management (procedure)"}]
               (:codes (nth trajectory start-idx))))
        (is (= [{:system :snomed :code "91251008" :display "Physical therapy procedure (regime/therapy)"}
                {:system :snomed :code "229070002" :display "Stretching exercises (regime/therapy)"}]
               (:activities (nth trajectory start-idx))))
        (is (= start-idx (:references end-event))
            "CarePlanEnd's own :careplan reference resolves back to the SAME trajectory index CarePlanStart occupies")))))
