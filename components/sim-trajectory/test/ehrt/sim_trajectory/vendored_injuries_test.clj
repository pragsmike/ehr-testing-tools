(ns ehrt.sim-trajectory.vendored-injuries-test
  "Injuries arc close (2026-08-11, notes/ADRs.md ADR-0107): the closure
  ADR-0070 deferred WHOLE (2026-08-07, `run-submodule exceeded
  max-steps` at `broken_jaw.json`'s own dangling `dental_referral`
  gate), then re-deferred narrower by ADR-0106 (2026-08-11, a SEPARATE
  `nested :encounter` gap surfaced once ADR-0105 closed the max-steps
  leg), now lands whole -- both legs closed. Eight-file closure: root
  plus `medications/ear_infection_antibiotic.json`, `medications/
  otc_pain_reliever.json`, `medications/moderate_opioid_pain_
  reliever.json`, `dme/wheelchair.json`, `dme/wheelchair_end.json`
  (all five already vendored, byte-identical at this same pin, re-used
  not re-copied), plus `injuries/broken_jaw.json` and `snf/
  skilled_nursing_facility.json` (newly landed alongside this file).

  `broken_jaw.json`'s own `dental_referral` attribute-gate (Dental
  Referral sets it once, nothing ever clears it, `Check for Dental
  Visit`/`Wait for Dental Visit` loops for the rest of the patient's
  simulated life) is HANDLED by ADR-0105's horizon-aware `run-
  submodule` plus its zero-advance-only `max-steps` counting -- not a
  disqualifying finding for this closure (re-confirmed below: the
  bounded defspec walk never throws `max-steps` at that branch).

  The named regression test at the bottom of this file re-walks ONE of
  the two ADR-0106-measured failing seeds
  (`-576131918266266247`/`-5690589783821964774`, mixer-seed 20260803,
  the SAME well-mixed derivation `census.clj`'s own `mixed-seeds`
  uses, at `census.clj`'s own exact parameters: registration age 30,
  50-year horizon, `default-persona-config`) against the NOW-REAL
  vendored closure -- the arc's own closing witness: the walk
  completes, `:status :horizon-complete`, with exactly one synthesized
  `:encounter-end` present and correctly cited."
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

(def injuries-json (slurp (io/resource "sim/modules/injuries.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`, the same shape every
  prior closure's own interpreter-layer test already establishes."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(def loaded-closure (gmf/load-closure "injuries" injuries-json resolve-call-path))

(deftest vendored-injuries-closure-loads-clean
  (testing "load-clean over the WHOLE eight-file closure -- root plus
            all seven called submodules, five already vendored from
            prior batches, two newly landed alongside this file"
    (is (result/ok? loaded-closure)
        (str "expected the vendored closure to validate against the v1 subset; got " (pr-str loaded-closure)))
    (is (= #{"injuries" "injuries/broken_jaw" "snf/skilled_nursing_facility"
             "medications/ear_infection_antibiotic" "medications/otc_pain_reliever"
             "medications/moderate_opioid_pain_reliever" "dme/wheelchair" "dme/wheelchair_end"}
           (into #{} (keys (:modules (:payload loaded-closure))))))))

(def modules (:modules (:payload loaded-closure)))
(def injuries-module (get modules "injuries"))

;; --- Census parameters (mirroring `census.clj`'s own `default-persona-
;; config`/`default-registration-offset-years`/`default-horizon-years`
;; exactly -- the SAME parameters ADR-0070/ADR-0105/ADR-0106 all probed
;; this closure at) --------------------------------------------------

(def ^:private census-persona-config
  {:race-weights [{:race "White" :weight 1.0} {:race "Black" :weight 1.0}
                  {:race "Hispanic" :weight 1.0} {:race "Asian" :weight 1.0}
                  {:race "Native" :weight 1.0} {:race "Other" :weight 1.0}]
   :socioeconomic-weights [{:category "High" :weight 1.0} {:category "Middle" :weight 1.0}
                           {:category "Low" :weight 1.0}]
   :state-weights [{:state "Alabama" :weight 1.0}]})

(defn- person [seed] (sim-model/persona (Random. seed) census-persona-config))
(defn- registration-t-for [persona] (+ (interp/dob-epoch-day persona) (* 365 30)))
(def ^:private horizon-window-days (* 365 50))

(defn- walk-result [seed]
  (let [p (person seed)
        reg-t (registration-t-for p)]
    (interp/run-module injuries-module (Random. seed) p reg-t (+ reg-t horizon-window-days) modules)))

(defspec vendored-injuries-walks-to-a-bounded-horizon-without-throwing 200
  (prop/for-all [seed gen/large-integer]
    (contains? #{:terminal :blocked :horizon-complete} (:status (walk-result seed)))))

(deftest vendored-injuries-walk-is-deterministic-for-the-same-seed
  (testing "D4: whole-walk reproducibility extends over this closure --
            walk twice, identical"
    (let [r1 (walk-result 20260802)
          r2 (walk-result 20260802)]
      (is (= (:trajectory r1) (:trajectory r2))))))

;; --- The arc's own closing witness (ADR-0107): one of the two
;; ADR-0106-measured failing seeds, re-walked against the NOW-REAL
;; vendored closure -----------------------------------------------------

(deftest nested-encounter-regression-adr-0106-spinal-injury-seed-now-completes
  (testing "ADR-0106's own direct-interpreter probe (mixer-seed 20260803,
            120 well-mixed seeds, these SAME census parameters) found
            this seed throwing `nested :encounter` pre-ADR-0107 --
            `ED_Visit_For_Spinal_Injury` (Encounter, emergency) opens
            and is never explicitly closed before `Spinal_Injury_
            Treatment_Encounter` (Encounter, ambulatory) reopens on the
            SAME walk. Post-ADR-0107: the walk completes, and the
            trajectory carries exactly one synthesized `:encounter-end`
            for the stale one, correctly cited."
    (let [seed -576131918266266247
          result (walk-result seed)
          trajectory (:trajectory result)
          encounters (filterv #(= :encounter (:event %)) trajectory)
          ends (filterv #(= :encounter-end (:event %)) trajectory)]
      (is (= :horizon-complete (:status result)) "the walk completes -- no throw")
      (is (>= (count encounters) 2) "both the ED visit and the treatment encounter reach the trajectory")
      (is (= 1 (:synthesized-encounter-ends result))
          "exactly one auto-close fired -- ED_Visit_For_Spinal_Injury's own stale open")
      (is (some #(= (:references %) (.indexOf trajectory (first encounters))) ends)
          "the synthesized end references the FIRST encounter's own index, not a guess"))))
