(ns ehrt.sim.emitter-order-independence-test
  "Guard test, mining group B (docs/research/SimHospital-Synthea-limitations-
  considered.md, its own mining notes' determinism-threat table): Synthea's
  own reproducibility saga (issues #682/#1342, PR #1237) shows unordered-
  collection iteration is a real determinism threat elsewhere, not merely a
  hypothetical here. This project's own defense is structural -- `emit-hl7`
  reads every field by keyword lookup (`:active-mrn`, `:location`, ...),
  never by iterating a map's own key order -- but the defense had no test of
  its own until this one. Builds facility/provider config maps via
  independent, differently-ordered `assoc` chains (`=`-equal, but with
  different construction histories, so a >8-key map could in principle land
  in a different internal bucket layout) and asserts the emitted ER7 is
  byte-identical either way.

  Expected to already be green -- this is a tripwire against a regression,
  not a red-then-green fix (ADR-0004's test-first rule applies to new
  behavior; a guard test for an existing structural guarantee is the
  documented exception). Run repeatedly (`clojure -X:test`, 3x per the
  mining notes) precisely because a hash-order-dependent bug, if one were
  ever introduced, might not fail every single run."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim.emit-hl7 :as emit-hl7]
            [ehrt.sim.engine :as engine]))

(def ref-date "2024-01-01")
(def utc-offset "+00:00")

(defn- ward-forward
  [id name beds surge class]
  (-> {}
      (assoc :id id)
      (assoc :name name)
      (assoc :beds beds)
      (assoc :surge-slots surge)
      (assoc :surge-format "%s-H%02d")
      (assoc :class class)))

(defn- ward-reversed
  [id name beds surge class]
  (-> {}
      (assoc :class class)
      (assoc :surge-format "%s-H%02d")
      (assoc :surge-slots surge)
      (assoc :beds beds)
      (assoc :name name)
      (assoc :id id)))

(def facility-a
  (-> {}
      (assoc :id :order-test)
      (assoc :wards [(ward-forward :ed "ED" 2 5 :ed)
                     (ward-forward :renal "Renal" 2 1 :inpatient)])))

(def facility-b
  (-> {}
      (assoc :wards [(ward-reversed :ed "ED" 2 5 :ed)
                     (ward-reversed :renal "Renal" 2 1 :inpatient)])
      (assoc :id :order-test)))

(defn- provider-template-forward
  [family given specialty wards]
  (-> {}
      (assoc :name {:family family :given given})
      (assoc :role :attending)
      (assoc :specialty specialty)
      (assoc :wards wards)))

(defn- provider-template-reversed
  [family given specialty wards]
  (-> {}
      (assoc :wards wards)
      (assoc :specialty specialty)
      (assoc :role :attending)
      (assoc :name {:given given :family family})))

(def providers-a
  [(provider-template-forward "Chen" "Amara" "Nephrology" [:renal])
   (provider-template-forward "Reyes" "Priya" "Emergency Medicine" [:ed :renal])])

(def providers-b
  [(provider-template-reversed "Chen" "Amara" "Nephrology" [:renal])
   (provider-template-reversed "Reyes" "Priya" "Emergency Medicine" [:ed :renal])])

(deftest emitted-output-does-not-depend-on-map-insertion-order
  (testing "the two configs are = despite each map being built via a different
            assoc order"
    (is (= facility-a facility-b))
    (is (= providers-a providers-b)))
  (testing "same seed, structurally-equal-but-differently-built facility/provider
            config -> byte-identical ground truth and byte-identical ER7"
    (let [run-a (engine/run {:seed 42 :patients 5 :facility facility-a :providers providers-a})
          run-b (engine/run {:seed 42 :patients 5 :facility facility-b :providers providers-b})]
      (is (= (:ground-truth run-a) (:ground-truth run-b)))
      (is (= (:providers run-a) (:providers run-b)))
      (is (= (emit-hl7/emit (:ground-truth run-a) ref-date utc-offset (:facility run-a) (:providers run-a))
             (emit-hl7/emit (:ground-truth run-b) ref-date utc-offset (:facility run-b) (:providers run-b)))))))
