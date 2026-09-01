(ns ehrt.sim.identifiers-test
  "The `sim identifiers` capability (post-M6, sim/ADR-0014). Written test-
  first (sim/ADR-0004): determinism (same config+seed => identical
  inventory) and completeness against a real run (every id a real
  emission of THIS run actually carries -- MRNs/control-ids on the
  wire, resource ids in the FHIR Bundles, provider NPIs, patient-ids --
  is present in the inventory, checked by independent extraction and
  set-comparison, never by re-reading identifiers-command's own
  output)."
  (:require [clojure.set]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]
            [ehrt.sim-emit-fhir.emit-fhir :as emit-fhir]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-engine.streams :as streams]
            [ehrt.sim.identifiers :as identifiers]
            [com.nervestaple.hl7-parser.parser :as parser]
            [com.nervestaple.hl7-parser.message :as message]))

(def ref-date "2024-01-01")
(def utc-offset "+00:00")

(deftest identifiers-command-requires-seed
  (let [r (identifiers/identifiers-command {})]
    (is (result/error? r))
    (is (= :missing-required-opt (:category r)))))

(deftest identifiers-command-rejects-incompatible-assignment
  (testing "the SAME static incompatible-assignment check run-command
            performs (ehrt.sim.run/incompatible-assignments,
            reused here, never re-derived)"
    (let [r (identifiers/identifiers-command
             {:seed 1 :patients 1
              :module-assignment [{:patient-ordinal 0 :module-id "sinusitis"}]})]
      (is (result/rejected? r))
      (is (= :incompatible-assignment (:category r))))))

(deftest identifiers-command-propagates-config-unreadable-unchanged
  ;; C-1 (ux fixes 2, ADR-0060): `ehrt.sim.run/merge-config-file` is
  ;; the SAME config-merging step run-command uses (this namespace's
  ;; own docstring) -- a malformed :config file must surface here
  ;; unchanged, not silently unwrapped or crash-until-caught.
  (let [tmp (java.io.File/createTempFile "malformed-config" ".edn")
        _ (spit tmp "not-even-a-map [")]
    (try
      (let [r (identifiers/identifiers-command {:seed 1 :patients 1 :config (.getPath tmp)})]
        (is (result/error? r))
        (is (= :config-unreadable (:category r)))
        (is (= (.getPath tmp) (:path (:payload r)))))
      (finally (.delete tmp)))))

(deftest identifiers-command-surfaces-an-unresolvable-module-name
  (let [r (identifiers/identifiers-command {:seed 1 :modules ["not-a-real-module"]})]
    (is (result/error? r))
    (is (= :module-not-found (:category r)))))

(def ^:private one-bed-no-ed-facility
  "The same tiny facility ehrt.sim.run-test's own capacity-
  exhaustion test uses -- guarantees exhaustion deterministically
  rather than relying on a lucky seed."
  {:id :tiny
   :wards [{:id :renal :name "Renal" :beds 1 :surge-slots 0
            :surge-format "%s-H%02d" :class :inpatient}]})

(deftest identifiers-command-surfaces-capacity-exhaustion-as-a-structured-error
  (let [r (identifiers/identifiers-command {:seed 1 :patients 2 :facility one-bed-no-ed-facility})]
    (is (result/error? r))
    (is (= :capacity-exhausted (:category r)))))

(deftest identifiers-command-shape
  (let [r (identifiers/identifiers-command {:seed 42 :patients 3})]
    (is (result/ok? r))
    (let [{:keys [run-id patient-ids mrns visit-beds control-ids fhir-resource-ids provider-npis]} (:payload r)]
      (is (= "42" run-id))
      (is (= 3 (count patient-ids)))
      (is (seq mrns))
      (is (= (set patient-ids) (set (keys visit-beds))))
      (is (seq control-ids))
      (is (seq fhir-resource-ids))
      (is (seq provider-npis)))))

;; --- determinism -----------------------------------------------------------

(defspec identifiers-command-is-deterministic 50
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 5)]
    (let [opts {:seed seed :patients patients}
          r1 (identifiers/identifiers-command opts)
          r2 (identifiers/identifiers-command opts)]
      (= (:payload r1) (:payload r2)))))

;; --- completeness: every id a real emission of THIS run carries is present,
;; checked by independent extraction against emit-hl7/emit and emit-fhir/
;; bundle-run -- never by re-deriving from identifiers-command's own output.

(defspec identifiers-command-is-complete-against-a-real-run 50
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 5)]
    (let [{:keys [ground-truth facility providers]} (run/run {:seed seed :patients patients})
          messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
          bundles (emit-fhir/bundle-run ground-truth ref-date utc-offset seed :end)
          wire-mrns (into #{} (map #(message/get-field-first-value (parser/parse %) "PID" 3)) messages)
          wire-control-ids (into #{} (map #(message/get-field-first-value (parser/parse %) "MSH" 10)) messages)
          bundle-resource-ids (into #{} (mapcat (fn [[_ b]] (map (comp :id :resource) (:entry b)))) bundles)
          expected-patient-ids (into #{} (map #(streams/patient-id-for seed %)) (range patients))
          expected-npis (into #{} (map :id) providers)
          {:keys [payload]} (identifiers/identifiers-command {:seed seed :patients patients})]
      (and (clojure.set/subset? wire-mrns (set (:mrns payload)))
           (clojure.set/subset? wire-control-ids (set (:control-ids payload)))
           (clojure.set/subset? bundle-resource-ids (set (:fhir-resource-ids payload)))
           (= expected-patient-ids (set (:patient-ids payload)))
           (= expected-npis (set (:provider-npis payload)))))))

(deftest identifiers-command-includes-a-merged-away-patients-own-mrn
  (testing "a merge's own evolve never clears the merged-away patient's
            :mrns (only :status :merged) -- the inventory must still
            surface that MRN, since it once appeared on the wire"
    (let [r (identifiers/identifiers-command
             {:seed 7 :patients 4
              :churn-profile {:merge 1.0 :cancel-transfer 0 :cancel-discharge 0 :bed-swap 0}})]
      (is (result/ok? r))
      (is (= 4 (count (:mrns (:payload r))))))))
