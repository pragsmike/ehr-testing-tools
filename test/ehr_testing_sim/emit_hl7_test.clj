(ns ehr-testing-sim.emit-hl7-test
  "The EmitHL7 stage's laws (docs/sim-theory.edn): bidirectional
  derivability, determinism, round-trip through an independent parser,
  and timestamp anchoring to a pinned :reference-date. Written before
  ehr-testing-sim.emit-hl7 exists (ADR-0004 test-first)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.emit-hl7 :as emit-hl7]
            [com.nervestaple.hl7-parser.parser :as parser]
            [com.nervestaple.hl7-parser.message :as message])
  (:import [java.time LocalDate LocalDateTime]
           [java.time.format DateTimeFormatter]))

(def ref-date "2024-01-01")

(defn- admission-discharge-events
  [ground-truth]
  (filterv #(#{:admission :discharge} (:event %)) ground-truth))

(defn- event-key
  "The (mrn, message-type, timestamp) triple a message must carry to
  map back to its unique log event -- the derivability law's own
  vocabulary."
  [{:keys [event mrn t]}]
  (let [{:keys [type trigger]} (emit-hl7/message-type-registry event)]
    [mrn (str type "^" trigger) (emit-hl7/hl7-timestamp ref-date t)]))

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
          messages (emit-hl7/emit ground-truth ref-date)
          expected-keys (mapv event-key events)
          actual-keys (mapv (comp message-key parser/parse) messages)]
      (and (= (count events) (count messages))
           (= (count expected-keys) (count (distinct expected-keys)))
           (= (set expected-keys) (set actual-keys))))))

(defspec determinism-is-a-pure-function-of-the-log 100
  (prop/for-all [seed gen/large-integer
                 patients (gen/choose 1 15)]
    (let [{:keys [ground-truth]} (engine/run {:seed seed :patients patients})]
      (= (emit-hl7/emit ground-truth ref-date)
         (emit-hl7/emit ground-truth ref-date)))))

(deftest round-trip-through-independent-parser
  (let [{:keys [ground-truth]} (engine/run {:seed 42 :patients 2})
        events (admission-discharge-events ground-truth)
        messages (emit-hl7/emit ground-truth ref-date)]
    (testing "every emitted message parses back, MRN and message-type intact"
      (is (= 4 (count messages)))
      (doseq [[event msg] (map vector events messages)
              :let [parsed (parser/parse msg)]]
        (is (= (:mrn event) (message/get-field-first-value parsed "PID" 3)))
        (is (= (let [{:keys [type trigger]} (emit-hl7/message-type-registry (:event event))]
                 (str type "^" trigger))
               (message/get-field-first-value parsed "MSH" 9)))))))

(deftest timestamp-anchoring-concrete
  (testing "a known offset renders the expected absolute timestamp"
    (is (= "20240101013000" (emit-hl7/hl7-timestamp "2024-01-01" 90)))
    (is (= "20240102000000" (emit-hl7/hl7-timestamp "2024-01-01" 1440)))))

(defspec timestamp-anchoring-property 100
  (prop/for-all [minutes (gen/choose 0 100000)]
    (let [ts (emit-hl7/hl7-timestamp ref-date minutes)
          parsed (LocalDateTime/parse ts (DateTimeFormatter/ofPattern "yyyyMMddHHmmss"))
          expected (.plusMinutes (.atStartOfDay (LocalDate/parse ref-date)) minutes)]
      (= expected parsed))))
