(ns ehr-testing-sim.emit-hl7-test
  "The EmitHL7 stage's laws (docs/sim-theory.edn): bidirectional
  derivability, determinism, round-trip through an independent parser,
  and timestamp anchoring to a pinned :reference-date. Written before
  ehr-testing-sim.emit-hl7 exists (ADR-0004 test-first).

  M2a (ADR-0011): timestamps are SECONDS from run start (was minutes),
  and every rendered timestamp carries the pinned :utc-offset's HL7-
  style zone suffix. M2a (ADR-0010): PID-3 renders the event's own
  :active-mrn, not a bare :mrn."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehr-testing-sim.config :as config]
            [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.emit-hl7 :as emit-hl7]
            [ehr-testing-sim.persona :as persona]
            [com.nervestaple.hl7-parser.parser :as parser]
            [com.nervestaple.hl7-parser.message :as message])
  (:import [java.time LocalDate LocalDateTime]
           [java.time.format DateTimeFormatter]
           [java.util Random]))

(def ref-date "2024-01-01")
(def utc-offset "+00:00")

(defn- admission-discharge-events
  "Renamed from v0 in spirit only (kept the name -- not worth touching
  every call site) -- Milestone M1's :transfer joins the events this
  helper covers, since the derivability law now extends to A02
  (docs/operational-models.md, .agents/plans/roadmap.md M1)."
  [ground-truth]
  (filterv #(#{:admission :discharge :transfer} (:event %)) ground-truth))

(defn- event-key
  "The (mrn, message-type, timestamp) triple a message must carry to
  map back to its unique log event -- the derivability law's own
  vocabulary. mrn here is the event's rendered :active-mrn (ADR-0010)."
  [{:keys [event active-mrn t]}]
  (let [{:keys [type trigger]} (emit-hl7/message-type-registry event)]
    [active-mrn (str type "^" trigger) (emit-hl7/hl7-timestamp ref-date t utc-offset)]))

(defn- message-key
  [parsed]
  [(message/get-field-first-value parsed "PID" 3)
   (message/get-field-first-value parsed "MSH" 9)
   (message/get-field-first-value parsed "MSH" 7)])

(defspec bidirectional-derivability 100
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 15)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients})
          events (admission-discharge-events ground-truth)
          messages (emit-hl7/emit ground-truth ref-date utc-offset)
          expected-keys (mapv event-key events)
          actual-keys (mapv (comp message-key parser/parse) messages)]
      (and (= (count events) (count messages))
           (= (count expected-keys) (count (distinct expected-keys)))
           (= (set expected-keys) (set actual-keys))))))

(defspec determinism-is-a-pure-function-of-the-log 100
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 15)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients})]
      (= (emit-hl7/emit ground-truth ref-date utc-offset)
         (emit-hl7/emit ground-truth ref-date utc-offset)))))

(deftest round-trip-through-independent-parser
  (let [{:keys [ground-truth]} (engine/run {:seed 42 :patients 2})
        events (admission-discharge-events ground-truth)
        messages (emit-hl7/emit ground-truth ref-date utc-offset)]
    (testing "every emitted message parses back, MRN and message-type intact"
      (is (= 4 (count messages)))
      (doseq [[event msg] (map vector events messages)
              :let [parsed (parser/parse msg)]]
        (is (= (:active-mrn event) (message/get-field-first-value parsed "PID" 3)))
        (is (= (let [{:keys [type trigger]} (emit-hl7/message-type-registry (:event event))]
                 (str type "^" trigger))
               (message/get-field-first-value parsed "MSH" 9)))))))

;; --- Milestone M1: A02, PV1-3/6/7 ----------------------------------------

(def ^:private crowded-facility
  {:id :crowded-test
   :wards [{:id :ed :name "ED" :beds 0 :surge-slots 5
            :surge-format "%s-H%02d" :class :ed}
           {:id :renal :name "Renal" :beds 1 :surge-slots 0
            :surge-format "%s-H%02d" :class :inpatient}]})

(defn- find-a02
  [messages]
  (first (filter #(re-find #"\^A02" %) messages)))

(deftest message-type-registry-has-a02
  (is (= {:type "ADT" :trigger "A02"} (emit-hl7/message-type-registry :transfer))))

(deftest transfer-emits-a02-with-pv1-3-6-7
  (let [{:keys [ground-truth facility providers]}
        (engine/run {:seed 1 :patients 3 :facility crowded-facility})
        transfer-event (first (filter #(= :transfer (:event %)) ground-truth))
        _ (assert transfer-event "expected this seed/facility to produce a bed-ready transfer")
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        a02 (find-a02 messages)
        parsed (parser/parse a02)
        provider (first (filter #(= (:id %) (:attending transfer-event)) providers))]
    (testing "PV1-3: ward^^bed^facility"
      (is (= (str (get-in transfer-event [:location :ward]) "^^"
                  (get-in transfer-event [:location :bed]) "^"
                  (name (:id facility)))
             (message/get-field-first-value parsed "PV1" 3))))
    (testing "PV1-6: prior location, from the event's own :from -- no shadow field"
      (is (= (str (get-in transfer-event [:from :ward]) "^^"
                  (get-in transfer-event [:from :bed]) "^"
                  (name (:id facility)))
             (message/get-field-first-value parsed "PV1" 6))))
    (testing "PV1-7: id^family^given"
      (is (= (str (:id provider) "^" (get-in provider [:name :family]) "^" (get-in provider [:name :given]))
             (message/get-field-first-value parsed "PV1" 7))))))

(deftest admission-pv1-6-is-empty-no-prior-location
  (let [{:keys [ground-truth facility providers]} (engine/run {:seed 42 :patients 1})
        admission (first ground-truth)
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        parsed (parser/parse (first messages))]
    (is (= "" (or (message/get-field-first-value parsed "PV1" 6) "")))))

(deftest timestamp-anchoring-concrete
  (testing "a known offset renders the expected absolute timestamp, suffixed
            with the HL7-style (colon-free) zone offset"
    (is (= "20240101013000+0000" (emit-hl7/hl7-timestamp "2024-01-01" 5400 "+00:00")))
    (is (= "20240102000000+0000" (emit-hl7/hl7-timestamp "2024-01-01" 86400 "+00:00")))
    (testing "a non-UTC fixed offset renders its own suffix, arithmetic unaffected (no timezone database, ADR-0011)"
      (is (= "20240101013000-0500" (emit-hl7/hl7-timestamp "2024-01-01" 5400 "-05:00"))))))

;; --- M2b: churn family message types --------------------------------------

(def ^:private churn-facility
  {:id :churn-test
   :wards [{:id :ed :name "ED" :beds 0 :surge-slots 10
            :surge-format "%s-H%02d" :class :ed}
           {:id :renal :name "Renal" :beds 1 :surge-slots 1
            :surge-format "%s-H%02d" :class :inpatient}]})

(def ^:private churn-providers
  [{:id "1234567893" :name {:family "Chen" :given "A"} :role :attending
    :specialty "Nephrology" :wards [:renal :ed]}])

(defn- world-of
  [patients]
  {:patients patients :facility churn-facility :providers churn-providers :ground-truth []})

(defn- fold-events
  [world events]
  (-> (reduce (fn [w ev]
                (reduce (fn [w2 {:keys [patient-id]}]
                          (update-in w2 [:patients patient-id] engine/evolve ev))
                        w (:participants ev)))
              world events)
      (update :ground-truth into events)))

(defn- admit
  [world t patient-id location]
  (let [{:keys [events]} (engine/decide (Random. 1) t world patient-id
                                        {:type :admission :location location})]
    (fold-events world events)))

(defn- nth-field-value
  "Like message/get-field-first-value, but for the Nth (0-based)
  occurrence of a repeating segment -- needed for A17's two PID/PV1
  pairs in one message (get-field-first-value only ever sees the
  first)."
  [parsed segment-id field-index n]
  (let [segment (nth (message/get-segments parsed segment-id) n)]
    (parser/pr-field (:delimiters parsed) (message/get-segment-field-raw segment field-index))))

(deftest message-type-registry-has-the-churn-family
  (is (= {:type "ADT" :trigger "A11"} (emit-hl7/message-type-registry :cancel-admit)))
  (is (= {:type "ADT" :trigger "A12"} (emit-hl7/message-type-registry :cancel-transfer)))
  (is (= {:type "ADT" :trigger "A13"} (emit-hl7/message-type-registry :cancel-discharge)))
  (is (= {:type "ADT" :trigger "A17"} (emit-hl7/message-type-registry :bed-swap)))
  (is (= {:type "ADT" :trigger "A40"} (emit-hl7/message-type-registry :merge))))

(deftest cancel-admit-round-trips-as-a11
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1" {:type :cancel-admit})
        world2 (fold-events world1 events)
        messages (emit-hl7/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)
        a11 (last messages)
        parsed (parser/parse a11)]
    (is (= 2 (count messages)))
    (is (= "MRN000001" (message/get-field-first-value parsed "PID" 3)))
    (is (= "ADT^A11" (message/get-field-first-value parsed "MSH" 9)))))

(deftest cancel-transfer-round-trips-as-a12-and-reinstates-location
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        pre-location (get-in world1 [:patients "P1" :location])
        {t-events :events} (engine/decide (Random. 1) 10 world1 "P1" {:type :transfer :location "ED"})
        world2 (fold-events world1 t-events)
        {c-events :events} (engine/decide (Random. 1) 20 world2 "P1" {:type :cancel-transfer})
        world3 (fold-events world2 c-events)
        messages (emit-hl7/emit (:ground-truth world3) ref-date utc-offset churn-facility churn-providers)
        a12 (last messages)
        parsed (parser/parse a12)]
    (is (= 3 (count messages)))
    (is (= "ADT^A12" (message/get-field-first-value parsed "MSH" 9)))
    (testing "PV1-3 shows the reinstated (current) location"
      (is (= (str (:ward pre-location) "^^" (:bed pre-location) "^churn-test")
             (message/get-field-first-value parsed "PV1" 3))))))

(deftest cancel-discharge-round-trips-as-a13
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {d-events :events} (engine/decide (Random. 1) 10 world1 "P1" {:type :discharge})
        world2 (fold-events world1 d-events)
        {c-events :events} (engine/decide (Random. 1) 20 world2 "P1" {:type :cancel-discharge})
        world3 (fold-events world2 c-events)
        messages (emit-hl7/emit (:ground-truth world3) ref-date utc-offset churn-facility churn-providers)
        a13 (last messages)
        parsed (parser/parse a13)]
    (is (= 3 (count messages)))
    (is (= "ADT^A13" (message/get-field-first-value parsed "MSH" 9)))))

(deftest transfer-in-error-emits-two-messages-a02-then-a12-in-error
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1"
                                        {:type :transfer-in-error :location "ED"})
        world2 (fold-events world1 events)
        messages (emit-hl7/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)]
    (is (= 3 (count messages)))
    (is (= "ADT^A02" (message/get-field-first-value (parser/parse (second messages)) "MSH" 9)))
    (is (= "ADT^A12" (message/get-field-first-value (parser/parse (last messages)) "MSH" 9)))))

(deftest bed-swap-emits-one-a17-message-carrying-both-patients
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")
                          "P2" (engine/initial-patient "P2" "MRN000002")})
        world1 (-> world0 (admit 0 "P1" "Renal") (admit 5 "P2" "Renal"))
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1" {:type :bed-swap})
        world2 (fold-events world1 events)
        messages (emit-hl7/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)
        a17 (last messages)
        parsed (parser/parse a17)]
    (testing "ONE message for the two-participant event -- event id (log position), not mrn alone, is what derivability keys on"
      (is (= 3 (count messages)))
      (is (= "ADT^A17" (message/get-field-first-value parsed "MSH" 9))))
    (testing "both patients' PID segments are present, each with their NEW bed"
      (is (= #{"MRN000001" "MRN000002"}
             (set [(nth-field-value parsed "PID" 3 0) (nth-field-value parsed "PID" 3 1)])))
      (let [pv1-3-values (set [(nth-field-value parsed "PV1" 3 0) (nth-field-value parsed "PV1" 3 1)])]
        (is (contains? pv1-3-values (str (get-in world1 [:patients "P1" :location :ward]) "^^"
                                          (get-in world1 [:patients "P1" :location :bed]) "^churn-test")))
        (is (contains? pv1-3-values (str (get-in world1 [:patients "P2" :location :ward]) "^^"
                                          (get-in world1 [:patients "P2" :location :bed]) "^churn-test")))))))

(deftest merge-emits-one-a40-message-with-mrg-and-pid
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")
                          "P2" (engine/initial-patient "P2" "MRN000002")})
        world1 (-> world0 (admit 0 "P1" "Renal") (admit 5 "P2" "Renal"))
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1" {:type :merge :with "P2"})
        world2 (fold-events world1 events)
        messages (emit-hl7/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)
        a40 (last messages)
        parsed (parser/parse a40)]
    (is (= 3 (count messages)))
    (testing "PID carries the SURVIVING mrn, MRG carries the prior (merged-away) one"
      (is (= "ADT^A40" (message/get-field-first-value parsed "MSH" 9)))
      (is (= "MRN000001" (message/get-field-first-value parsed "PID" 3)))
      (is (= "MRN000002" (message/get-field-first-value parsed "MRG" 1))))))

(deftest churn-family-emission-is-deterministic
  (let [world0 (world-of {"P1" (engine/initial-patient "P1" "MRN000001")
                          "P2" (engine/initial-patient "P2" "MRN000002")})
        world1 (-> world0 (admit 0 "P1" "Renal") (admit 5 "P2" "Renal"))
        {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1" {:type :bed-swap})
        world2 (fold-events world1 events)]
    (is (= (emit-hl7/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)
           (emit-hl7/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)))))

;; --- ADR-0012: :step-rejected renders NO message, by design ---------------

(deftest step-rejected-has-no-message-type-registry-entry
  (is (nil? (emit-hl7/message-type-registry :step-rejected))))

(deftest step-rejected-event-renders-no-message
  (testing "truth about the run, never wire traffic (ADR-0012): a
            :step-rejected event produces the SAME empty vector any
            unregistered event type does"
    (let [world0 {:patients {"P1" (engine/initial-patient "P1" "MRN000001")}
                  :facility churn-facility :providers churn-providers :ground-truth []}
          world1 (admit world0 0 "P1" "Renal")
          {:keys [events]} (engine/decide (Random. 1) 10 world1 "P1" {:type :cancel-transfer})
          rejected-event (first events)]
      (is (= :step-rejected (:event rejected-event)))
      (is (= [] (emit-hl7/event->messages ref-date utc-offset churn-facility churn-providers rejected-event))))))

;; --- M3: ORM^O01 + ORU^R01 --------------------------------------------

(def ^:private cbc-order-pathway
  {:name "cbc-order" :steps [{:type :admission :location "Renal"}
                             {:type :order :profile :cbc}
                             {:type :discharge}]})

(defn- run-with-order
  [seed]
  (engine/run {:seed seed :patients 1 :pathways [{:pathway cbc-order-pathway :weight 1}]}))

(deftest message-type-registry-has-orm-and-oru
  (is (= {:type "ORM" :trigger "O01"} (emit-hl7/message-type-registry :order-placed)))
  (is (= {:type "ORU" :trigger "R01"} (emit-hl7/message-type-registry :result-available))))

(defn- find-message
  [messages trigger]
  (first (filter #(re-find (re-pattern (str "\\^" trigger)) %) messages)))

(deftest order-placed-emits-orm-with-orc-and-obr
  (let [{:keys [ground-truth facility providers]} (run-with-order 1)
        order-event (first (filter #(= :order-placed (:event %)) ground-truth))
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        orm (find-message messages "O01")
        parsed (parser/parse orm)]
    (testing "ORC-1: new order"
      (is (= "NW" (message/get-field-first-value parsed "ORC" 1))))
    (testing "OBR-4: universal service id, CWE with the panel-level LOINC concept"
      (let [{:keys [code display]} (:concept order-event)]
        (is (= (str code "^" display "^LN") (message/get-field-first-value parsed "OBR" 4)))))
    (testing "PID/PV1 context, same conventions as every other message type"
      (is (= "MRN000001" (message/get-field-first-value parsed "PID" 3)))
      (is (= (str (get-in order-event [:location :ward]) "^^"
                  (get-in order-event [:location :bed]) "^" (name (:id facility)))
             (message/get-field-first-value parsed "PV1" 3))))))

(deftest result-available-emits-oru-with-one-obx-per-analyte-in-profile-order
  (let [{:keys [ground-truth facility providers]} (run-with-order 1)
        result-event (first (filter #(= :result-available (:event %)) ground-truth))
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        oru (find-message messages "R01")
        parsed (parser/parse oru)
        obx-segments (message/get-segments parsed "OBX")]
    (testing "one OBX per analyte, same count and order as the result's own :results"
      (is (= (count (:results result-event)) (count obx-segments)) "CBC has 5 analytes"))
    (doseq [[i {:keys [concept units value reference-range abnormal-flag]}]
            (map-indexed vector (:results result-event))]
      (let [obx (nth obx-segments i)
            field #(parser/pr-field (:delimiters parsed) (message/get-segment-field-raw obx %))]
        (testing (str "OBX #" (inc i) ": " (:code concept))
          (is (= (str (inc i)) (field 1)) "OBX-1: set id")
          (is (= "NM" (field 2)) "OBX-2: value type")
          (is (= (str (:code concept) "^" (:display concept) "^LN") (field 3)) "OBX-3: CWE, LOINC triplet")
          (is (= (str value) (field 5)) "OBX-5: value")
          (is (= units (field 6)) "OBX-6: units")
          (is (= (str (:low reference-range) "-" (:high reference-range)) (field 7)) "OBX-7: reference range")
          (is (= (case abnormal-flag :normal "N" :low "L" :high "H") (field 8)) "OBX-8: abnormal flag"))))))

(deftest order-and-result-round-trip-deterministically
  (let [{:keys [ground-truth facility providers]} (run-with-order 1)]
    (is (= (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
           (emit-hl7/emit ground-truth ref-date utc-offset facility providers)))))

(defspec order-and-result-messages-derive-bijectively-from-the-log 50
  (prop/for-all [seed gen/large-integer]
    (let [{:keys [ground-truth facility providers]} (run-with-order seed)
          order-result-events (filterv #(#{:order-placed :result-available} (:event %)) ground-truth)
          messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
          order-result-messages (filter #(or (re-find #"\^O01" %) (re-find #"\^R01" %)) messages)]
      (= 2 (count order-result-events) (count order-result-messages)))))

;; --- M4: PID demographic enrichment + IN1 (payer) -------------------------

(defn- find-registered
  [ground-truth patient-id]
  (first (filter #(and (= :registered (:event %))
                       (some (fn [p] (= patient-id (:patient-id p))) (:participants %)))
                 ground-truth)))

(deftest admission-pid-carries-demographic-fields
  (let [{:keys [ground-truth facility providers]} (engine/run {:seed 42 :patients 1})
        admission (first (filter #(= :admission (:event %)) ground-truth))
        patient-id (:patient-id (first (:participants admission)))
        {:keys [persona]} (find-registered ground-truth patient-id)
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        parsed (parser/parse (first messages))]
    (testing "PID-5: XPN family^given"
      (is (= (str (get-in persona [:name :family]) "^" (get-in persona [:name :given]))
             (message/get-field-first-value parsed "PID" 5))))
    (testing "PID-7: DOB, HL7 date (YYYYMMDD, dashes stripped)"
      (is (= (clojure.string/replace (:dob persona) "-" "")
             (message/get-field-first-value parsed "PID" 7))))
    (testing "PID-8: sex, HL7 Table 0001 (F/M)"
      (is (= (case (:sex persona) :female "F" :male "M")
             (message/get-field-first-value parsed "PID" 8))))
    (testing "PID-11: XAD street^^city^state^zip"
      (let [{:keys [street city state zip]} (:address persona)]
        (is (= (str street "^^" city "^" state "^" zip)
               (message/get-field-first-value parsed "PID" 11)))))
    (testing "PID-13: phone"
      (is (= (:phone persona) (message/get-field-first-value parsed "PID" 13))))))

(deftest admission-carries-in1-with-the-sampled-payer
  (let [{:keys [ground-truth facility providers]} (engine/run {:seed 42 :patients 1})
        admission (first (filter #(= :admission (:event %)) ground-truth))
        patient-id (:patient-id (first (:participants admission)))
        {:keys [persona]} (find-registered ground-truth patient-id)
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        parsed (parser/parse (first messages))]
    (testing "IN1-1: set id"
      (is (= "1" (message/get-field-first-value parsed "IN1" 1))))
    (testing "IN1-3/IN1-4: insurance company id/name, from the sampled payer pool entry"
      (is (= (get-in persona [:payer :id]) (message/get-field-first-value parsed "IN1" 3)))
      (is (= (get-in persona [:payer :name]) (message/get-field-first-value parsed "IN1" 4))))))

(deftest non-admission-messages-carry-enriched-pid-but-no-in1
  (let [{:keys [ground-truth facility providers]} (engine/run {:seed 42 :patients 5})
        discharge (first (filter #(= :discharge (:event %)) ground-truth))
        patient-id (:patient-id (first (:participants discharge)))
        {:keys [persona]} (find-registered ground-truth patient-id)
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        discharge-msg (first (filter #(re-find #"\^A03" %) messages))
        parsed (parser/parse discharge-msg)]
    (testing "PID is enriched the same way on every message type, not admission-only"
      (is (= (str (get-in persona [:name :family]) "^" (get-in persona [:name :given]))
             (message/get-field-first-value parsed "PID" 5))))
    (testing "IN1 rides ONLY the admission message -- the real HL7 convention this milestone follows"
      (is (empty? (message/get-segments parsed "IN1"))))))

(deftest hand-built-worlds-without-a-registered-event-fall-back-to-legacy-pid
  (testing "old test worlds that never processed a :registered step (e.g. churn-scenarios-style
            hand-driven decide/evolve) still emit the pre-M4 3-field PID -- no persona, no crash"
    (let [world0 {:patients {"P1" (engine/initial-patient "P1" "MRN000001")}
                  :facility config/default-facility :providers config/default-provider-templates
                  :ground-truth []}
          {:keys [events]} (engine/decide (Random. 1) 0 world0 "P1" {:type :admission :location "Renal"})
          messages (emit-hl7/emit events ref-date utc-offset config/default-facility config/default-provider-templates)
          parsed (parser/parse (first messages))]
      (is (= "MRN000001" (message/get-field-first-value parsed "PID" 3)))
      (is (= "" (message/get-field-first-value parsed "PID" 5)))
      (is (empty? (message/get-segments parsed "IN1"))))))

;; --- M4 Task 4: the ER7 escaping property ----------------------------------
;; org.clojars.cmiles74/clojure-hl7-parser implements NO escape-sequence
;; handling in EITHER direction -- verified directly against its own source
;; (notes/facts-register.md F9): pr-field/pr-content concatenate field
;; content with no encoding step on write, and read-text's escape-handling
;; branch (and read-subcomponents') is commented-out dead code on read, so
;; a literal delimiter character corrupts field boundaries unless something
;; upstream escapes it, and get-field-first-value returns an escape sequence
;; LITERALLY rather than decoding it. This repo's own escape-er7/unescape-er7
;; are the documented workaround: encode on write (this repo's job, since
;; the library doesn't), decode on read (a small helper this repo provides
;; since the library's own decoder is dead code).

(defn- persona-with-family-name
  [family-name]
  (assoc (persona/persona (Random. 1) {}) :name {:family family-name :given "Pat"}))

(defn- pid5-round-trip
  "Builds a minimal admission-shaped world with `persona`, emits it, parses
  the message back, and returns the raw PID-5 family name substring (before
  the ^ component separator)."
  [persona]
  (let [world0 {:patients {"P1" (assoc (engine/initial-patient "P1" "MRN000001") :persona persona)}
                :facility config/default-facility :providers config/default-provider-templates
                :ground-truth []}
        registered-event {:event :registered :t 0 :active-mrn "MRN000001" :persona persona
                          :participants [{:patient-id "P1" :role :subject}]}
        world1 (update-in world0 [:patients "P1"] engine/evolve registered-event)
        {:keys [events]} (engine/decide (Random. 1) 0 world1 "P1" {:type :admission :location "Renal"})
        ground-truth (into [registered-event] events)
        messages (emit-hl7/emit ground-truth ref-date utc-offset config/default-facility config/default-provider-templates)
        parsed (parser/parse (first messages))
        pid5 (message/get-field-first-value parsed "PID" 5)]
    (first (str/split pid5 #"\^"))))

(deftest scripted-apostrophe-name-round-trips-byte-faithfully
  (testing "O'Brien needs NO escaping at all -- apostrophe isn't an ER7
            delimiter character, so the raw parsed value already IS the
            original, no workaround needed"
    (is (= "O'Brien" (pid5-round-trip (persona-with-family-name "O'Brien"))))))

(deftest scripted-adversarial-delimiter-name-is-a-documented-parser-finding
  (testing "Sm|th (a literal ER7 field-delimiter character embedded in a
            name) is the adversarial case: the RAW parsed value is the
            ESCAPED form, not the original -- proof the parser decodes
            NOTHING (F9) -- and this repo's own unescape-er7 is required
            to recover the original byte-faithfully"
    (let [raw (pid5-round-trip (persona-with-family-name "Sm|th"))]
      (is (not= "Sm|th" raw) "the library does not decode escape sequences -- this is the finding")
      (is (= "Sm\\F\\th" raw) "our own encoder escaped the embedded delimiter, per standard ER7")
      (is (= "Sm|th" (emit-hl7/unescape-er7 raw)) "our own decoder recovers the original exactly"))))

(deftest escape-er7-is-identity-for-strings-with-no-delimiter-characters
  (doseq [s ["O'Brien" "Smith-Jones" "D'Angelo" "Anderson-Lee" "Plain Name"]]
    (is (= s (emit-hl7/escape-er7 s)))))

(deftest escape-then-unescape-round-trips-every-reserved-character
  (doseq [ch [\| \^ \~ \& \\]]
    (let [s (str "a" ch "b")]
      (is (= s (emit-hl7/unescape-er7 (emit-hl7/escape-er7 s)))))))

(defspec every-generated-persona-name-round-trips-through-unescape-er7 200
  (testing "the general property: ANY family name -- letters, apostrophes,
            hyphens, spaces, or literal ER7 delimiter characters, in any
            combination -- round-trips byte-faithfully through emit + parse
            + unescape-er7, even when the raw parsed value (pre-unescape)
            is not the original"
    (prop/for-all [family-name (gen/not-empty
                                (gen/vector (gen/elements (concat "ABCDEFGabcdefg '-" "|^~&\\"))
                                            1 12))]
      (let [family-name (apply str family-name)
            raw (pid5-round-trip (persona-with-family-name family-name))]
        (= family-name (emit-hl7/unescape-er7 raw))))))

(defspec timestamp-anchoring-property 100
  (prop/for-all [seconds (gen/choose 0 6000000)]
    (let [ts (emit-hl7/hl7-timestamp ref-date seconds utc-offset)
          local-part (subs ts 0 14)
          offset-part (subs ts 14)
          parsed (LocalDateTime/parse local-part (DateTimeFormatter/ofPattern "yyyyMMddHHmmss"))
          expected (.plusSeconds (.atStartOfDay (LocalDate/parse ref-date)) seconds)]
      (and (= expected parsed)
           (= "+0000" offset-part)))))
