(ns ehrt.patient-simulator.vendored-uti-test
  "GMF coverage Wave D stage D3 payoff (2026-08-02, ADR-0029): the
  SEVENTH real vendored module, `urinary_tract_infections.json`
  (`resources/modules/NOTICE`'s own new table rows) -- deferred at Wave
  B (D6, dirty with DiagnosticReport/MultiObservation and a genuinely
  new sixth transition kind, lookup_table_transition) until both landed
  (DiagnosticReport/MultiObservation at Wave D stage D1; lookup_table_
  transition this session, D3a/H2). Twelve real files -- the closure's
  SECOND (`ear_infections.json`'s own closure was the first) -- plus
  this project's FIRST data-file closure members, two lookup-table
  CSVs (`resources/modules/lookup_tables/`). Full closure re-survey:
  `docs/gmf-interpreter.md` section 14.

  Interpreter-layer coverage only, the SAME disposition `ear_infections.
  json`'s own vendored test already establishes for a closure: the full
  compile-trajectory/engine/emit round trip is a standing, already-
  disclosed gap for EVERY closure-having module vendored to date (D2's
  own dated note, ADR-0029 -- confirmed by direct search this session:
  no such test exists for `ear_infections.json` either), not newly
  introduced by this vendoring."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.patient-simulator.gmf-interpreter :as interp]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.util Random]))

(def uti-json (slurp (io/resource "sim/modules/urinary_tract_infections.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(defn- resolve-table-name
  "D3a/H2's own real caller shape -- a thin io/resource wrapper over
  `sim/modules/lookup_tables/<table-name>` (the table name already
  carries its own `.csv` extension, unlike a module call-path)."
  [table-name]
  (some-> (io/resource (str "sim/modules/lookup_tables/" table-name)) slurp))

(def loaded-closure (gmf/load-closure "urinary-tract-infections" uti-json resolve-call-path resolve-table-name))

(deftest vendored-uti-closure-loads-clean
  (testing "load-clean over the WHOLE closure -- root plus all eleven
            called submodules AND both lookup tables, D3's own
            all-or-nothing gate extended to data-file members (H2)"
    (is (result/ok? loaded-closure)
        (str "expected the vendored closure to validate against the v1 subset; got " (pr-str loaded-closure)))
    (is (= #{"urinary-tract-infections" "uti/telemed_path" "uti/ambulatory_path" "uti/ed_path"
             "uti/hpi" "uti/gu_pregnancy_check" "uti/abx_tx" "uti/labs" "uti/lab_follow_up"
             "uti/ambulatory_eval" "uti/ed_eval" "uti/ed_bundle"}
           (into #{} (keys (:modules (:payload loaded-closure))))))
    (is (= #{"uti.csv" "uti_recurrence.csv"} (into #{} (keys (:tables (:payload loaded-closure))))))))

(def modules (:modules (:payload loaded-closure)))
(def tables (:tables (:payload loaded-closure)))
(def uti (get modules "urinary-tract-infections"))

(defn- well-mixed-candidate-seeds
  "Sequential small Random seeds are NOT well-distributed for their own
  first draw -- the established mixer-RNG fix, reused verbatim across
  every GMF coverage wave since `vendored_sore_throat_test.clj`."
  [n mixer-seed]
  (let [mixer (Random. mixer-seed)]
    (repeatedly n #(.nextLong mixer))))

(defn- person [seed sex dob] (assoc (sim-model/persona (Random. seed) {}) :sex sex :dob dob))

;; `Wait Unit 15`'s own bare Age >= 15 years Guard gates entry -- a very
;; long horizon lets `Wait_for_UTI`'s own self-looping Delay (0-12
;; months) sweep the walk through every uti.csv age bucket (15-24
;; through 75-140) over many re-roll chances, surfacing even the rarer
;; Pyelonephritis branch within one walk rather than needing an
;; astronomical candidate-seed count against any single bucket's own
;; single-roll probability.
(defn- registration-t-for [persona] (interp/dob-epoch-day persona))
(def ^:private horizon-window-days (* 365 100))

(defn- walk-result [seed sex]
  (let [p (person seed sex "1946-01-01")
        reg-t (registration-t-for p)]
    (interp/run-module uti (Random. seed) p reg-t (+ reg-t horizon-window-days) modules {} tables)))

(defspec vendored-uti-walks-to-a-bounded-horizon-without-throwing 200
  (prop/for-all [seed gen/large-integer
                 sex (gen/elements [:female :male])]
    (contains? #{:terminal :blocked :horizon-complete} (:status (walk-result seed sex)))))

(deftest vendored-uti-walk-is-deterministic-for-the-same-seed
  (let [r1 (walk-result 20260802 :female)
        r2 (walk-result 20260802 :female)]
    (is (= (:trajectory r1) (:trajectory r2)))))

;; --- H6: entry-path lookup dispatch proven BOTH ways -----------------------

(defn- condition-onset-events [trajectory] (filter #(= :condition-onset (:event %)) trajectory))
(defn- onset-codes [trajectory] (into #{} (mapcat :codes) (condition-onset-events trajectory)))

(def ^:private cystitis-code {:system :snomed :code "307426000" :display "Acute infective cystitis (disorder)"})
(def ^:private pyelonephritis-code {:system :snomed :code "45816000" :display "Pyelonephritis (disorder)"})

(deftest lookup-table-transition-entry-path-reaches-cystitis-for-some-seed
  (testing "uti.csv's own row-weighted dispatch, real closure, real table"
    (let [seed (first (keep (fn [seed] (when (contains? (onset-codes (:trajectory (walk-result seed :female))) cystitis-code)
                                          seed))
                            (well-mixed-candidate-seeds 200 20260802)))]
      (is (some? seed) "expected at least one well-mixed candidate seed to onset Cystitis"))))

(deftest lookup-table-transition-entry-path-reaches-pyelonephritis-for-some-seed
  (testing "the SAME entry path's own much rarer branch (uti.csv's own
            0.0026 weight for an elderly female) -- real closure, real
            table, no different mechanism than the Cystitis case above"
    (let [seed (first (keep (fn [seed] (when (contains? (onset-codes (:trajectory (walk-result seed :female))) pyelonephritis-code)
                                          seed))
                            (well-mixed-candidate-seeds 500 777)))]
      (is (some? seed) "expected at least one well-mixed candidate seed to onset Pyelonephritis"))))

;; --- H6: a type_of_care_transition path taken, cross-boundary
;; encounter events asserted (Wave B's own deferred (f) check, closed
;; here for the first time against a real closure) ---------------------

(defn- encounter-events [trajectory] (filter #(#{:encounter :encounter-end} (:event %)) trajectory))

(deftest a-walk-reaches-a-care-pathway-and-carries-cross-boundary-encounter-events
  (testing "Care Pathways (type_of_care_transition, D5) selects one of
            Telemedicine/Ambulatory/ED, each a CallSubmodule into its
            own path file -- every Encounter/EncounterEnd inside that
            path submodule cites the full call path, root-first (D2),
            the SAME shape ear_infections.json's own MedicationOrder
            citations already proved for a different state type"
    (let [seed (first (keep (fn [seed] (let [result (walk-result seed :female)]
                                          (when (seq (encounter-events (:trajectory result))) seed)))
                            (well-mixed-candidate-seeds 200 42)))]
      (is (some? seed) "expected at least one well-mixed candidate seed to reach an Encounter inside a care-pathway submodule")
      (let [result (walk-result seed :female)
            events (encounter-events (:trajectory result))]
        (is (every? (fn [e] (= "urinary-tract-infections" (first (:call-path e)))) events))
        (is (every? (fn [e] (contains? #{"uti/telemed_path" "uti/ambulatory_path" "uti/ed_path"} (second (:call-path e))))
                    events))
        (is (every? (fn [e] (= (:module e) (second (:call-path e)))) events))))))
