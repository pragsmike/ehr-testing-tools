(ns ehr-testing-sim.emit-hl7-test
  "The EmitHL7 stage's laws (docs/sim-theory.edn): bidirectional
  derivability, determinism, round-trip through an independent parser,
  and timestamp anchoring to a pinned :reference-date. Written before
  ehr-testing-sim.emit-hl7 exists (ADR-0004 test-first).

  M2a (ADR-0011): timestamps are SECONDS from run start (was minutes),
  and every rendered timestamp carries the pinned :utc-offset's HL7-
  style zone suffix. M2a (ADR-0010): PID-3 renders the event's own
  :active-mrn, not a bare :mrn."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehr-testing-sim.config :as config]
            [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.emit-hl7 :as emit-hl7]
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

(defspec timestamp-anchoring-property 100
  (prop/for-all [seconds (gen/choose 0 6000000)]
    (let [ts (emit-hl7/hl7-timestamp ref-date seconds utc-offset)
          local-part (subs ts 0 14)
          offset-part (subs ts 14)
          parsed (LocalDateTime/parse local-part (DateTimeFormatter/ofPattern "yyyyMMddHHmmss"))
          expected (.plusSeconds (.atStartOfDay (LocalDate/parse ref-date)) seconds)]
      (and (= expected parsed)
           (= "+0000" offset-part)))))
