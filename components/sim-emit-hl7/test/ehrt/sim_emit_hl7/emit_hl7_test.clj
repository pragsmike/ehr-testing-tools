(ns ehrt.sim-emit-hl7.emit-hl7-test
  "The EmitHL7 stage's laws (docs/sim-theory.edn): bidirectional
  derivability, determinism, round-trip through an independent parser,
  and timestamp anchoring to a pinned :reference-date. Written before
  ehrt.sim-emit-hl7.emit-hl7 exists (sim/ADR-0004 test-first).

  M2a (sim/ADR-0011): timestamps are SECONDS from run start (was minutes),
  and every rendered timestamp carries the pinned :utc-offset's HL7-
  style zone suffix. M2a (sim/ADR-0010): PID-3 renders the event's own
  :active-mrn, not a bare :mrn."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-engine.config :as config]
            [ehrt.sim-engine.decide :as decide]
            [ehrt.sim-engine.evolve :as evolve]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-engine.state :as state]
            [ehrt.sim-engine.streams :as streams]
            [ehrt.sim-emit-hl7.emit :as emit]
            [ehrt.sim-emit-hl7.er7 :as er7]
            [ehrt.sim-emit-hl7.hl7-time :as hl7-time]
            [ehrt.sim-emit-hl7.messages :as messages]
            [ehrt.sim-emit-hl7.registry :as registry]
            [ehrt.sim-emit-hl7.segments :as segments]
            [ehrt.sim-emit-hl7.site-profile :as site-profile]
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
  vocabulary. mrn here is the event's rendered :active-mrn (sim/ADR-0010)."
  [{:keys [event active-mrn t]}]
  (let [{:keys [type trigger]} (registry/message-type-registry event)]
    [active-mrn (str type "^" trigger) (hl7-time/hl7-timestamp ref-date t utc-offset)]))

(defn- message-key
  [parsed]
  [(message/get-field-first-value parsed "PID" 3)
   (message/get-field-first-value parsed "MSH" 9)
   (message/get-field-first-value parsed "MSH" 7)])

(defspec bidirectional-derivability 100
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 15)]
    (let [{:keys [ground-truth]} (run/run {:seed seed :patients patients})
          events (admission-discharge-events ground-truth)
          messages (emit/emit ground-truth ref-date utc-offset)
          expected-keys (mapv event-key events)
          actual-keys (mapv (comp message-key parser/parse) messages)]
      (and (= (count events) (count messages))
           (= (count expected-keys) (count (distinct expected-keys)))
           (= (set expected-keys) (set actual-keys))))))

(defspec determinism-is-a-pure-function-of-the-log 100
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 15)]
    (let [{:keys [ground-truth]} (run/run {:seed seed :patients patients})]
      (= (emit/emit ground-truth ref-date utc-offset)
         (emit/emit ground-truth ref-date utc-offset)))))

(deftest round-trip-through-independent-parser
  (let [{:keys [ground-truth]} (run/run {:seed 42 :patients 2})
        events (admission-discharge-events ground-truth)
        messages (emit/emit ground-truth ref-date utc-offset)]
    (testing "every emitted message parses back, MRN and message-type intact"
      (is (= 4 (count messages)))
      (doseq [[event msg] (map vector events messages)
              :let [parsed (parser/parse msg)]]
        (is (= (:active-mrn event) (message/get-field-first-value parsed "PID" 3)))
        (is (= (let [{:keys [type trigger]} (registry/message-type-registry (:event event))]
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
  (is (= {:type "ADT" :trigger "A02"} (registry/message-type-registry :transfer))))

(deftest transfer-emits-a02-with-pv1-3-6-7
  (let [{:keys [ground-truth facility providers]}
        (run/run {:seed 1 :patients 3 :facility crowded-facility})
        transfer-event (first (filter #(= :transfer (:event %)) ground-truth))
        _ (assert transfer-event "expected this seed/facility to produce a bed-ready transfer")
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
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
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
        admission (first ground-truth)
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
        parsed (parser/parse (first messages))]
    (is (= "" (or (message/get-field-first-value parsed "PV1" 6) "")))))

(deftest timestamp-anchoring-concrete
  (testing "a known offset renders the expected absolute timestamp, suffixed
            with the HL7-style (colon-free) zone offset"
    (is (= "20240101013000+0000" (hl7-time/hl7-timestamp "2024-01-01" 5400 "+00:00")))
    (is (= "20240102000000+0000" (hl7-time/hl7-timestamp "2024-01-01" 86400 "+00:00")))
    (testing "a non-UTC fixed offset renders its own suffix, arithmetic unaffected (no timezone database, sim/ADR-0011)"
      (is (= "20240101013000-0500" (hl7-time/hl7-timestamp "2024-01-01" 5400 "-05:00"))))))

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
                          (update-in w2 [:patients patient-id] evolve/evolve ev))
                        w (:participants ev)))
              world events)
      (update :ground-truth into events)))

(defn- admit
  [world t patient-id location]
  (let [{:keys [events]} (decide/decide (streams/one-stream (Random. 1)) t world patient-id
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

(deftest message-type-registry-has-the-bed-cycles-a20
  (testing "arc 3b sweep 2 (ADR-0174 ruling C): the message family's own
            first cost -- a registry entry, co-landed with the kind"
    (is (= {:type "ADT" :trigger "A20"} (registry/message-type-registry :bed-status-change)))))

(deftest a20-control-ids-are-derivable-and-unique-per-bed-and-leg
  (testing "the message family's own second and third costs: a
            control-id derivation, and a derivability-property row. An
            A20 has no PID to key on, so the derivability triple is
            (bed, MSH-9, MSH-7) rather than (mrn, MSH-9, MSH-7) -- and
            two legs of ONE bed's cycle must not collide even at the
            same instant, which is why the status is in the key"
    (let [{:keys [ground-truth facility providers]}
          (run/run {:seed 4242 :patients 6 :arrival-gap 60 :bed-cycle true
                       :pathway {:name "ad" :steps [{:type :admission :location "Renal"}
                                                    {:type :delay :from 30 :to 30}
                                                    {:type :discharge}]}})
          bed-events (filterv #(= :bed-status-change (:event %)) ground-truth)
          messages (mapcat #(messages/event->messages ref-date utc-offset facility providers {} nil {} %)
                           bed-events)
          expected (mapv (fn [ev] [(:bed ev) "ADT^A20"
                                   (hl7-time/hl7-timestamp ref-date (:t ev) utc-offset)])
                         bed-events)
          actual (mapv (fn [m]
                         (let [p (parser/parse m)]
                           [(nth (message/get-segment-field (first (message/get-segments p "NPU")) 1) 2)
                            (message/get-field-first-value p "MSH" 9)
                            (message/get-field-first-value p "MSH" 7)]))
                       messages)
          control-ids (mapv #(segments/control-id-for %) bed-events)]
      (is (pos? (count bed-events)) "the fixture actually produced bed events")
      (is (= (count bed-events) (count messages)))
      (is (= expected actual) "every message maps back to its own bed event")
      (is (= (count control-ids) (count (distinct control-ids)))
          "and every control id is unique"))))

(deftest message-type-registry-has-the-churn-family
  (is (= {:type "ADT" :trigger "A11"} (registry/message-type-registry :cancel-admit)))
  (is (= {:type "ADT" :trigger "A12"} (registry/message-type-registry :cancel-transfer)))
  (is (= {:type "ADT" :trigger "A13"} (registry/message-type-registry :cancel-discharge)))
  (is (= {:type "ADT" :trigger "A17"} (registry/message-type-registry :bed-swap)))
  (is (= {:type "ADT" :trigger "A40"} (registry/message-type-registry :merge))))

(deftest cancel-admit-round-trips-as-a11
  (let [world0 (world-of {"P1" (state/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (decide/decide (streams/one-stream (Random. 1)) 10 world1 "P1" {:type :cancel-admit})
        world2 (fold-events world1 events)
        messages (emit/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)
        a11 (last messages)
        parsed (parser/parse a11)]
    (is (= 2 (count messages)))
    (is (= "MRN000001" (message/get-field-first-value parsed "PID" 3)))
    (is (= "ADT^A11" (message/get-field-first-value parsed "MSH" 9)))))

(deftest cancel-transfer-round-trips-as-a12-and-reinstates-location
  (let [world0 (world-of {"P1" (state/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        pre-location (get-in world1 [:patients "P1" :location])
        {t-events :events} (decide/decide (streams/one-stream (Random. 1)) 10 world1 "P1" {:type :transfer :location "ED"})
        world2 (fold-events world1 t-events)
        {c-events :events} (decide/decide (streams/one-stream (Random. 1)) 20 world2 "P1" {:type :cancel-transfer})
        world3 (fold-events world2 c-events)
        messages (emit/emit (:ground-truth world3) ref-date utc-offset churn-facility churn-providers)
        a12 (last messages)
        parsed (parser/parse a12)]
    (is (= 3 (count messages)))
    (is (= "ADT^A12" (message/get-field-first-value parsed "MSH" 9)))
    (testing "PV1-3 shows the reinstated (current) location"
      (is (= (str (:ward pre-location) "^^" (:bed pre-location) "^churn-test")
             (message/get-field-first-value parsed "PV1" 3))))))

(deftest cancel-discharge-round-trips-as-a13
  (let [world0 (world-of {"P1" (state/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {d-events :events} (decide/decide (streams/one-stream (Random. 1)) 10 world1 "P1" {:type :discharge})
        world2 (fold-events world1 d-events)
        {c-events :events} (decide/decide (streams/one-stream (Random. 1)) 20 world2 "P1" {:type :cancel-discharge})
        world3 (fold-events world2 c-events)
        messages (emit/emit (:ground-truth world3) ref-date utc-offset churn-facility churn-providers)
        a13 (last messages)
        parsed (parser/parse a13)]
    (is (= 3 (count messages)))
    (is (= "ADT^A13" (message/get-field-first-value parsed "MSH" 9)))))

(deftest transfer-in-error-emits-two-messages-a02-then-a12-in-error
  (let [world0 (world-of {"P1" (state/initial-patient "P1" "MRN000001")})
        world1 (admit world0 0 "P1" "Renal")
        {:keys [events]} (decide/decide (streams/one-stream (Random. 1)) 10 world1 "P1"
                                        {:type :transfer-in-error :location "ED"})
        world2 (fold-events world1 events)
        messages (emit/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)]
    (is (= 3 (count messages)))
    (is (= "ADT^A02" (message/get-field-first-value (parser/parse (second messages)) "MSH" 9)))
    (is (= "ADT^A12" (message/get-field-first-value (parser/parse (last messages)) "MSH" 9)))))

(deftest bed-swap-emits-one-a17-message-carrying-both-patients
  (let [world0 (world-of {"P1" (state/initial-patient "P1" "MRN000001")
                          "P2" (state/initial-patient "P2" "MRN000002")})
        world1 (-> world0 (admit 0 "P1" "Renal") (admit 5 "P2" "Renal"))
        {:keys [events]} (decide/decide (streams/one-stream (Random. 1)) 10 world1 "P1" {:type :bed-swap})
        world2 (fold-events world1 events)
        messages (emit/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)
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
  (let [world0 (world-of {"P1" (state/initial-patient "P1" "MRN000001")
                          "P2" (state/initial-patient "P2" "MRN000002")})
        world1 (-> world0 (admit 0 "P1" "Renal") (admit 5 "P2" "Renal"))
        {:keys [events]} (decide/decide (streams/one-stream (Random. 1)) 10 world1 "P1" {:type :merge :with "P2"})
        world2 (fold-events world1 events)
        messages (emit/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)
        a40 (last messages)
        parsed (parser/parse a40)]
    (is (= 3 (count messages)))
    (testing "PID carries the SURVIVING mrn, MRG carries the prior (merged-away) one"
      (is (= "ADT^A40" (message/get-field-first-value parsed "MSH" 9)))
      (is (= "MRN000001" (message/get-field-first-value parsed "PID" 3)))
      (is (= "MRN000002" (message/get-field-first-value parsed "MRG" 1))))))

(deftest churn-family-emission-is-deterministic
  (let [world0 (world-of {"P1" (state/initial-patient "P1" "MRN000001")
                          "P2" (state/initial-patient "P2" "MRN000002")})
        world1 (-> world0 (admit 0 "P1" "Renal") (admit 5 "P2" "Renal"))
        {:keys [events]} (decide/decide (streams/one-stream (Random. 1)) 10 world1 "P1" {:type :bed-swap})
        world2 (fold-events world1 events)]
    (is (= (emit/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)
           (emit/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)))))

;; --- sim/ADR-0012: :step-rejected renders NO message, by design ---------------

(deftest step-rejected-has-no-message-type-registry-entry
  (is (nil? (registry/message-type-registry :step-rejected))))

(deftest step-rejected-event-renders-no-message
  (testing "truth about the run, never wire traffic (sim/ADR-0012): a
            :step-rejected event produces the SAME empty vector any
            unregistered event type does"
    (let [world0 {:patients {"P1" (state/initial-patient "P1" "MRN000001")}
                  :facility churn-facility :providers churn-providers :ground-truth []}
          world1 (admit world0 0 "P1" "Renal")
          {:keys [events]} (decide/decide (streams/one-stream (Random. 1)) 10 world1 "P1" {:type :cancel-transfer})
          rejected-event (first events)]
      (is (= :step-rejected (:event rejected-event)))
      (is (= [] (messages/event->messages ref-date utc-offset churn-facility churn-providers rejected-event))))))

;; --- M5b: :outpatient-visit -> A04; :outpatient-visit-end -> no message ---

(def ^:private outpatient-pathway
  {:name "outpatient" :steps [{:type :outpatient-visit :reason "Sinus congestion"}
                              {:type :delay :from 30 :to 30}
                              {:type :outpatient-visit-end}]})

(deftest message-type-registry-has-a04
  (is (= {:type "ADT" :trigger "A04"} (registry/message-type-registry :outpatient-visit))))

(deftest outpatient-visit-end-has-no-message-type-registry-entry
  (testing "item 7: a real ground-truth event, deliberately no wire message
            -- the same sim/ADR-0012 :step-rejected precedent"
    (is (nil? (registry/message-type-registry :outpatient-visit-end)))))

(deftest outpatient-visit-emits-a04-with-pv1-2-o-and-empty-pv1-3
  (let [{:keys [ground-truth facility providers]}
        (run/run {:seed 1 :patients 1 :pathways [{:pathway outpatient-pathway :weight 1}]})
        visit-event (first (filter #(= :outpatient-visit (:event %)) ground-truth))
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
        a04 (first (filter #(re-find #"\^A04" %) messages))
        parsed (parser/parse a04)]
    (is (some? a04))
    (testing "PV1-2: outpatient's own code table entry, \"O\""
      (is (= "O" (message/get-field-first-value parsed "PV1" 2))))
    (testing "PV1-3: empty -- no location, no bed (item 6)"
      (is (= "" (or (message/get-field-first-value parsed "PV1" 3) ""))))
    (testing "PV1-7: attending still renders, even with no ward to have filtered by"
      (is (not= "" (message/get-field-first-value parsed "PV1" 7))))))

(deftest outpatient-visit-end-event-renders-no-message
  (let [{:keys [ground-truth facility providers]}
        (run/run {:seed 1 :patients 1 :pathways [{:pathway outpatient-pathway :weight 1}]})
        end-event (first (filter #(= :outpatient-visit-end (:event %)) ground-truth))]
    (is (some? end-event))
    (is (= [] (messages/event->messages ref-date utc-offset facility providers end-event)))))

(deftest other-message-types-still-render-pv1-2-i-unaffected-by-outpatient
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
        admission-msg (first (emit/emit ground-truth ref-date utc-offset facility providers))]
    (is (= "I" (message/get-field-first-value (parser/parse admission-msg) "PV1" 2)))))

;; --- M5b: :observation -> ORU^R01 (OBX only, no ORC/OBR -- unsolicited,
;; not order-linked); :procedure/:medication-order/:medication-end are
;; truth-only, per the mapping table (components/patient-simulator/docs/gmf-interpreter.md section 1) --
;; no message-type-registry entry, DG1/billing rendering gated on
;; snomed-icd10-map, never built this milestone. ------------------------

(def ^:private a-concept {:system :snomed :code "8310-5" :display "Body temperature"})

(deftest message-type-registry-has-observation-but-not-procedure-or-medication
  (is (= {:type "ORU" :trigger "R01"} (registry/message-type-registry :observation)))
  (is (nil? (registry/message-type-registry :procedure)))
  (is (nil? (registry/message-type-registry :medication-order)))
  (is (nil? (registry/message-type-registry :medication-end))))

(deftest observation-emits-oru-with-one-obx-and-no-orc-or-obr
  (let [pathway {:name "vitals" :steps [{:type :admission :location "Renal"}
                                        {:type :observation :codes [a-concept] :value 38.2 :unit "Cel"}]}
        {:keys [ground-truth facility providers]} (run/run {:seed 1 :patients 1 :pathways [{:pathway pathway :weight 1}]})
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
        oru (first (filter #(re-find #"\^R01" %) messages))
        parsed (parser/parse oru)
        obx-line (first (filter #(str/starts-with? % "OBX") (str/split oru #"\r")))]
    (is (some? oru))
    (is (= "8310-5" (first (str/split (message/get-field-first-value parsed "OBX" 3) #"\^"))))
    (is (= "38.2" (message/get-field-first-value parsed "OBX" 5)))
    (is (= "Cel" (message/get-field-first-value parsed "OBX" 6)))
    (is (= "" (or (message/get-field-first-value parsed "ORC" 1) "")) "no order context -- unsolicited observation")
    (testing "GMF coverage Wave D stage D1 (ADR-0029): no reference-
              range/abnormal-flag CONTENT when the observation carries
              neither.

              RE-BASELINED 2026-08-16 (ADR-0142), disclosed rather than
              silently edited. This assertion read `(= 7 ...)` and
              stated the D1 property as 'byte-identical to pre-D1
              output -- no fields appended'. ADR-0142 renders OBX-14
              (clinical time) on every OBX, and HL7v2 field positions
              being ordinal, reaching OBX-14 requires a positional pad
              through OBX-7..13 -- so the field COUNT is now fixed at
              14 for every OBX (15 split elements, the segment name
              included), and D1's absence-based phrasing can no longer
              be what carries the property. Author ruling Q2 'a' names
              this pad and accepts it, OBX-7/8 included.

              What D1 actually cared about survives intact and is what
              is asserted here now: OBX-7 and OBX-8 are EMPTY, not
              populated, when the observation carries neither a
              reference-range nor an interpretation. A downstream
              reader still cannot mistake this observation for one
              carrying a range; it simply learns that from an empty
              field rather than from a missing one."
      (let [parts (str/split obx-line #"\|" -1)]
        (is (= 15 (count parts)) "OBX-1..14, plus the segment name")
        (is (= "" (nth parts 7)) "OBX-7 empty: no reference range supplied")
        (is (= "" (nth parts 8)) "OBX-8 empty: no abnormal flag supplied")))))

;; --- GMF coverage Wave D stage D1 (2026-08-02, ADR-0029 P6): observation's
;; new fields (value-code/reference-range/abnormal-flag), and
;; :diagnostic-report -> ORU^R01 with ORC+OBR present -----------------------

(def ^:private a-value-code {:system :snomed :code "10828004" :display "Positive (qualifier value)"})

(deftest observation-with-value-code-emits-cwe-obx2-and-a-system-aware-obx5
  (let [pathway {:name "finding" :steps [{:type :admission :location "Renal"}
                                         {:type :observation :codes [a-concept] :value-code a-value-code}]}
        {:keys [ground-truth facility providers]} (run/run {:seed 1 :patients 1 :pathways [{:pathway pathway :weight 1}]})
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
        oru (first (filter #(re-find #"\^R01" %) messages))
        parsed (parser/parse oru)]
    (is (= "CWE" (message/get-field-first-value parsed "OBX" 2)))
    (is (= "10828004^Positive (qualifier value)^SCT" (message/get-field-first-value parsed "OBX" 5)))))

(deftest observation-with-reference-range-emits-obx7-and-obx8
  (let [pathway {:name "vitals" :steps [{:type :admission :location "Renal"}
                                        {:type :observation :codes [a-concept] :value 98.0 :unit "%"
                                         :reference-range {:low 95 :high 100} :interpretation :normal}]}
        {:keys [ground-truth facility providers]} (run/run {:seed 1 :patients 1 :pathways [{:pathway pathway :weight 1}]})
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
        oru (first (filter #(re-find #"\^R01" %) messages))
        parsed (parser/parse oru)]
    (is (= "95-100" (message/get-field-first-value parsed "OBX" 7)))
    (is (= "N" (message/get-field-first-value parsed "OBX" 8)))))

(def ^:private a-report-concept {:system :loinc :code "600-7" :display "Bacteria identified in Blood by Culture"})
(def ^:private an-analyte-concept {:system :loinc :code "88262-1" :display "Gram positive blood culture panel"})

(deftest message-type-registry-has-diagnostic-report
  (is (= {:type "ORU" :trigger "R01"} (registry/message-type-registry :diagnostic-report))))

(deftest diagnostic-report-emits-oru-with-orc-and-obr-and-one-obx-per-child
  (let [pathway {:name "panel" :steps [{:type :admission :location "Renal"}
                                       {:type :diagnostic-report :codes [a-report-concept]
                                        :observations [{:codes [an-analyte-concept] :value-code a-value-code}]}]}
        {:keys [ground-truth facility providers]} (run/run {:seed 1 :patients 1 :pathways [{:pathway pathway :weight 1}]})
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
        oru (first (filter #(re-find #"\^R01" %) messages))
        parsed (parser/parse oru)]
    (is (some? oru))
    (testing "ORC-1: new order, PRESENT -- unlike :observation's own order-less shape"
      (is (= "NW" (message/get-field-first-value parsed "ORC" 1))))
    (testing "OBR-4: the report-level codes"
      (is (= "600-7^Bacteria identified in Blood by Culture^LN" (message/get-field-first-value parsed "OBR" 4))))
    (testing "one OBX for the one child, sharing observation-obx-segment's own field set"
      (is (= 1 (count (message/get-segments parsed "OBX"))))
      (is (= "CWE" (message/get-field-first-value parsed "OBX" 2)))
      (is (= "88262-1^Gram positive blood culture panel^LN" (message/get-field-first-value parsed "OBX" 3)))
      (is (= "10828004^Positive (qualifier value)^SCT" (message/get-field-first-value parsed "OBX" 5))))))

(deftest diagnostic-report-with-multiple-children-emits-one-obx-per-child-in-order
  (let [pathway {:name "bp-panel"
                 :steps [{:type :admission :location "Renal"}
                         {:type :diagnostic-report
                          :observations [{:codes [{:system :loinc :code "8480-6" :display "Systolic Blood Pressure"}]
                                          :value 92.0 :unit "mm[Hg]"}
                                         {:codes [{:system :loinc :code "8462-4" :display "Diastolic Blood Pressure"}]
                                          :value 64.0 :unit "mm[Hg]"}]}]}
        {:keys [ground-truth facility providers]} (run/run {:seed 1 :patients 1 :pathways [{:pathway pathway :weight 1}]})
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
        oru (first (filter #(re-find #"\^R01" %) messages))
        parsed (parser/parse oru)
        obx-segments (message/get-segments parsed "OBX")]
    (is (= 2 (count obx-segments)))
    (let [field #(parser/pr-field (:delimiters parsed) (message/get-segment-field-raw %1 %2))]
      (is (= "1" (field (first obx-segments) 1)))
      (is (= "92.0" (field (first obx-segments) 5)))
      (is (= "2" (field (second obx-segments) 1)))
      (is (= "64.0" (field (second obx-segments) 5))))))

(deftest diagnostic-report-with-no-report-level-codes-emits-a-degenerate-obr4
  (testing "no report-level :codes -> obr-segment receives nil, the same
            degenerate-but-legal CWE cwe-field already renders for any
            nil concept -- blank code/display, coding-system still LN,
            never a crash"
    (let [pathway {:name "panel" :steps [{:type :admission :location "Renal"}
                                         {:type :diagnostic-report :observations [{:codes [an-analyte-concept] :value 1.0}]}]}
          {:keys [ground-truth facility providers]} (run/run {:seed 1 :patients 1 :pathways [{:pathway pathway :weight 1}]})
          messages (emit/emit ground-truth ref-date utc-offset facility providers)
          oru (first (filter #(re-find #"\^R01" %) messages))
          parsed (parser/parse oru)]
      (is (= "^^LN" (message/get-field-first-value parsed "OBR" 4))))))

(deftest procedure-and-medication-events-render-no-message
  (let [pathway {:name "clinical" :steps [{:type :admission :location "Renal"}
                                          {:type :procedure :codes [a-concept]}
                                          {:type :medication-order :codes [a-concept]}
                                          {:type :medication-end}]}
        {:keys [ground-truth facility providers]} (run/run {:seed 1 :patients 1 :pathways [{:pathway pathway :weight 1}]})]
    (doseq [event (filter #(#{:procedure :medication-order :medication-end} (:event %)) ground-truth)]
      (is (= [] (messages/event->messages ref-date utc-offset facility providers event))
          (str (:event event) " should render no message")))))

;; GMF coverage Wave D stage D2 (2026-08-02, ADR-0029 R3/G3): the SAME
;; truth-only, no-registry-entry, no-message treatment
;; :procedure/:medication-order/:medication-end already establish,
;; asserted the same way (G3's own "deliberate silence is an invariant,
;; not an absence").

(deftest message-type-registry-has-no-care-plan-entries
  (is (nil? (registry/message-type-registry :care-plan-start)))
  (is (nil? (registry/message-type-registry :care-plan-end))))

(deftest care-plan-events-render-no-message
  (let [pathway {:name "post-op" :steps [{:type :admission :location "Renal"}
                                         {:type :care-plan-start :codes [a-concept]}
                                         {:type :care-plan-end}]}
        {:keys [ground-truth facility providers]} (run/run {:seed 1 :patients 1 :pathways [{:pathway pathway :weight 1}]})]
    (doseq [event (filter #(#{:care-plan-start :care-plan-end} (:event %)) ground-truth)]
      (is (= [] (messages/event->messages ref-date utc-offset facility providers event))
          (str (:event event) " should render no message")))))

;; --- M3: ORM^O01 + ORU^R01 --------------------------------------------

(def ^:private cbc-order-pathway
  {:name "cbc-order" :steps [{:type :admission :location "Renal"}
                             {:type :order :profile :cbc}
                             {:type :discharge}]})

(defn- run-with-order
  [seed]
  (run/run {:seed seed :patients 1 :pathways [{:pathway cbc-order-pathway :weight 1}]}))

(deftest message-type-registry-has-orm-and-oru
  (is (= {:type "ORM" :trigger "O01"} (registry/message-type-registry :order-placed)))
  (is (= {:type "ORU" :trigger "R01"} (registry/message-type-registry :result-available))))

(defn- find-message
  [messages trigger]
  (first (filter #(re-find (re-pattern (str "\\^" trigger)) %) messages)))

(deftest order-placed-emits-orm-with-orc-and-obr
  (let [{:keys [ground-truth facility providers]} (run-with-order 1)
        order-event (first (filter #(= :order-placed (:event %)) ground-truth))
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
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
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
        oru (find-message messages "R01")
        parsed (parser/parse oru)
        obx-segments (message/get-segments parsed "OBX")]
    (testing "one OBX per analyte, same count and order as the result's own :results"
      (is (= (count (:results result-event)) (count obx-segments)) "CBC has 5 analytes"))
    (doseq [[i {:keys [concept unit value reference-range abnormal-flag]}]
            (map-indexed vector (:results result-event))]
      (let [obx (nth obx-segments i)
            field #(parser/pr-field (:delimiters parsed) (message/get-segment-field-raw obx %))]
        (testing (str "OBX #" (inc i) ": " (:code concept))
          (is (= (str (inc i)) (field 1)) "OBX-1: set id")
          (is (= "NM" (field 2)) "OBX-2: value type")
          (is (= (str (:code concept) "^" (:display concept) "^LN") (field 3)) "OBX-3: CWE, LOINC triplet")
          (is (= (str value) (field 5)) "OBX-5: value")
          (is (= unit (field 6)) "OBX-6: unit")
          (is (= (str (:low reference-range) "-" (:high reference-range)) (field 7)) "OBX-7: reference range")
          (is (= (case abnormal-flag :normal "N" :low "L" :high "H") (field 8)) "OBX-8: abnormal flag"))))))

(deftest order-and-result-round-trip-deterministically
  (let [{:keys [ground-truth facility providers]} (run-with-order 1)]
    (is (= (emit/emit ground-truth ref-date utc-offset facility providers)
           (emit/emit ground-truth ref-date utc-offset facility providers)))))

(defspec order-and-result-messages-derive-bijectively-from-the-log 50
  (prop/for-all [seed (gen/large-integer* {:min 0})]
    (let [{:keys [ground-truth facility providers]} (run-with-order seed)
          order-result-events (filterv #(#{:order-placed :result-available} (:event %)) ground-truth)
          messages (emit/emit ground-truth ref-date utc-offset facility providers)
          order-result-messages (filter #(or (re-find #"\^O01" %) (re-find #"\^R01" %)) messages)]
      (= 2 (count order-result-events) (count order-result-messages)))))

;; --- M4: PID demographic enrichment + IN1 (payer) -------------------------

(defn- find-registered
  [ground-truth patient-id]
  (first (filter #(and (= :registered (:event %))
                       (some (fn [p] (= patient-id (:patient-id p))) (:participants %)))
                 ground-truth)))

(deftest admission-pid-carries-demographic-fields
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
        admission (first (filter #(= :admission (:event %)) ground-truth))
        patient-id (:patient-id (first (:participants admission)))
        {:keys [persona]} (find-registered ground-truth patient-id)
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
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
    (testing "PID-13: phone, in the parenthesised US shape (ADR-0175 A1)"
      ;; The persona's own `NNN-NNN-NNNN` is what the LOG carries; the
      ;; wire carries `(NNN)NNN-NNNN`, which is what HAPI's v2.4 TN
      ;; primitive rule accepts. Derived from the persona here rather
      ;; than hard-coded, so this stays a statement about the RULE.
      (is (= (str/replace (:phone persona) #"^(\d{3})-(\d{3})-(\d{4})$" "($1)$2-$3")
             (message/get-field-first-value parsed "PID" 13))))))

(deftest pid-13-renders-the-parenthesised-us-phone-shape
  "ARC 4 SWEEP 1 (ADR-0175 ruling A1, commit 1 of 2). PID-13 renders
  `(NNN)NNN-NNNN`, not the persona's own `NNN-NNN-NNNN`.

  The reason is a conformance fact, not a taste: HAPI's v2.4 TN
  primitive rule accepts `\"(303)292-0567\"` and rejects
  `\"492-292-0567\"`, and `PipeParser` enforces primitives DURING the
  parse -- so at MSH-12 \"2.4\" the persona's own shape does not warn,
  it throws, and 346 of the 747 messages ADR-0175 probed never resolved
  to a structure at all. Rendering is the right place to fix it:
  GROUND TRUTH DOES NOT MOVE. `ehrt.sim-model.persona`'s `:phone`
  regex and its three draws are untouched, and
  `bin/ground-truth-bracket` proves that per commit rather than this
  test asserting it.

  An ABSENT phone still renders an EMPTY PID-13 -- the placeholder
  registration ADR-0173 ruling E1 introduced. Reformatting must not
  turn a blank into `\"()-\"`."
  (let [persona {:name {:family "Alvarez" :given "Rosa"}
                 :sex :female
                 :dob "1975-03-14"
                 :address {:street "1 Main St" :city "Denver" :state "CO" :zip "80202"}
                 :phone "303-292-0567"}
        ;; Rendered the way the emitter renders -- through
        ;; `parser/str-message` and back through an INDEPENDENT parser
        ;; -- rather than read off the segment data structure, so this
        ;; asserts what a consumer receives and not what a builder
        ;; returns. The MSH is scaffolding; only PID-13 is under test.
        render (fn [p]
                 (message/get-field-first-value
                  (parser/parse
                   (parser/str-message
                    (parser/create-message
                     parser/DEFAULT-DELIMITERS
                     (#'segments/msh-segment nil {:type "ADT" :trigger "A01"}
                      "CTRL-1" "20240101000000+0000")
                     (#'segments/pid-segment "MRN000001" p))))
                  "PID" 13))]
    (testing "a persona phone is reformatted, digit for digit"
      (is (= "(303)292-0567" (render persona))))
    (testing "an absent phone still renders an EMPTY PID-13, not a malformed one"
      (is (= "" (render (dissoc persona :phone)))))))

(deftest admission-carries-in1-with-the-sampled-payer
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
        admission (first (filter #(= :admission (:event %)) ground-truth))
        patient-id (:patient-id (first (:participants admission)))
        {:keys [persona]} (find-registered ground-truth patient-id)
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
        parsed (parser/parse (first messages))]
    (testing "IN1-1: set id"
      (is (= "1" (message/get-field-first-value parsed "IN1" 1))))
    (testing "IN1-3/IN1-4: insurance company id/name, from the sampled payer pool entry"
      (is (= (get-in persona [:payer :id]) (message/get-field-first-value parsed "IN1" 3)))
      (is (= (get-in persona [:payer :name]) (message/get-field-first-value parsed "IN1" 4))))))

(deftest non-admission-messages-carry-enriched-pid-but-no-in1
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 5})
        discharge (first (filter #(= :discharge (:event %)) ground-truth))
        patient-id (:patient-id (first (:participants discharge)))
        {:keys [persona]} (find-registered ground-truth patient-id)
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
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
    (let [world0 {:patients {"P1" (state/initial-patient "P1" "MRN000001")}
                  :facility sim-model/default-facility :providers sim-model/default-provider-templates
                  :ground-truth []}
          {:keys [events]} (decide/decide (streams/one-stream (Random. 1)) 0 world0 "P1" {:type :admission :location "Renal"})
          messages (emit/emit events ref-date utc-offset sim-model/default-facility sim-model/default-provider-templates)
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
  (assoc (sim-model/persona (Random. 1) {}) :name {:family family-name :given "Pat"}))

(defn- pid5-round-trip
  "Builds a minimal admission-shaped world with `persona`, emits it, parses
  the message back, and returns the raw PID-5 family name substring (before
  the ^ component separator)."
  [persona]
  (let [world0 {:patients {"P1" (assoc (state/initial-patient "P1" "MRN000001") :persona persona)}
                :facility sim-model/default-facility :providers sim-model/default-provider-templates
                :ground-truth []}
        registered-event {:event :registered :t 0 :active-mrn "MRN000001" :persona persona
                          :participants [{:patient-id "P1" :role :subject}]}
        world1 (update-in world0 [:patients "P1"] evolve/evolve registered-event)
        {:keys [events]} (decide/decide (streams/one-stream (Random. 1)) 0 world1 "P1" {:type :admission :location "Renal"})
        ground-truth (into [registered-event] events)
        messages (emit/emit ground-truth ref-date utc-offset sim-model/default-facility sim-model/default-provider-templates)
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
      (is (= "Sm|th" (er7/unescape-er7 raw)) "our own decoder recovers the original exactly"))))

(deftest escape-er7-is-identity-for-strings-with-no-delimiter-characters
  (doseq [s ["O'Brien" "Smith-Jones" "D'Angelo" "Anderson-Lee" "Plain Name"]]
    (is (= s (er7/escape-er7 s)))))

(deftest escape-then-unescape-round-trips-every-reserved-character
  (doseq [ch [\| \^ \~ \& \\]]
    (let [s (str "a" ch "b")]
      (is (= s (er7/unescape-er7 (er7/escape-er7 s)))))))

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
        (= family-name (er7/unescape-er7 raw))))))

(defspec timestamp-anchoring-property 100
  (prop/for-all [seconds (gen/choose 0 6000000)]
    (let [ts (hl7-time/hl7-timestamp ref-date seconds utc-offset)
          local-part (subs ts 0 14)
          offset-part (subs ts 14)
          parsed (LocalDateTime/parse local-part (DateTimeFormatter/ofPattern "yyyyMMddHHmmss"))
          expected (.plusSeconds (.atStartOfDay (LocalDate/parse ref-date)) seconds)]
      (and (= expected parsed)
           (= "+0000" offset-part)))))

;; --- Milestone site-profiles, Task 1: the default-profile identity -------
;; docs/site-profiles.md's own determinism anchor: no profile arg at all,
;; an explicit nil, and an explicit {} must all render IDENTICALLY -- and,
;; since nothing in emit-hl7 consumed a site-profile before this milestone,
;; identically to today's own pre-milestone output too (the 5-arg arity
;; below is untouched).

(defspec default-profile-is-the-absent-profile 100
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 8)]
    (let [{:keys [ground-truth facility providers]} (run/run {:seed seed :patients patients})
          five-arg (emit/emit ground-truth ref-date utc-offset facility providers)
          nil-profile (emit/emit ground-truth ref-date utc-offset facility providers nil)
          empty-profile (emit/emit ground-truth ref-date utc-offset facility providers {})]
      (= five-arg nil-profile empty-profile))))

;; --- Milestone site-profiles, Task 2: MSH dialect + code-table overrides -

(def ^:private aldric-profile
  "A deliberately different-looking profile -- distinct MSH dialect
  fields and an overridden patient-class code -- exercised throughout
  this milestone's own tests."
  {:name "St. Aldric's Memorial"
   :msh {:version "2.5.1" :sending-app "ALDRIC-EHR" :sending-facility "ALDRIC"
         :receiving-app "DOWNSTREAM" :receiving-facility "DOWNSTREAM-FAC"
         :processing-id "T"}
   :code-tables {:patient-class {:inpatient {:code "IN" :coding-system "99ALDRIC"}}
                 :discharge-disposition {:discharged-to-home {:code "HOME" :coding-system "99ALDRIC"}}}})

(deftest msh-dialect-renders-the-profiles-version-and-app-facility-fields
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
        messages (emit/emit ground-truth ref-date utc-offset facility providers aldric-profile)
        parsed (parser/parse (first messages))]
    (testing "MSH-12: version id"
      (is (= "2.5.1" (message/get-field-first-value parsed "MSH" 12))))
    (testing "MSH-3/4: sending app/facility"
      (is (= "ALDRIC-EHR" (message/get-field-first-value parsed "MSH" 3)))
      (is (= "ALDRIC" (message/get-field-first-value parsed "MSH" 4))))
    (testing "MSH-5/6: receiving app/facility"
      (is (= "DOWNSTREAM" (message/get-field-first-value parsed "MSH" 5)))
      (is (= "DOWNSTREAM-FAC" (message/get-field-first-value parsed "MSH" 6))))
    (testing "MSH-11: processing id (post-M6, sim/ADR-0014's own Task 4 knob)"
      (is (= "T" (message/get-field-first-value parsed "MSH" 11))))))

(deftest msh-11-processing-id-defaults-to-P-and-accepts-T-or-D
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
        parsed-for (fn [processing-id]
                     (parser/parse
                      (first (emit/emit ground-truth ref-date utc-offset facility providers
                                            (when processing-id {:msh {:processing-id processing-id}})))))]
    (is (= "P" (message/get-field-first-value (parsed-for nil) "MSH" 11)) "no profile: today's default")
    (is (= "P" (message/get-field-first-value (parsed-for "P") "MSH" 11)))
    (is (= "T" (message/get-field-first-value (parsed-for "T") "MSH" 11)))
    (is (= "D" (message/get-field-first-value (parsed-for "D") "MSH" 11)))))

(deftest absent-profile-renders-todays-hardcoded-msh-values
  "MSH-12 IS \"2.4\" SINCE 2026-08-27 (arc 4 sweep 1, ADR-0175 ruling
  A1, commit 2 of 2). It read \"2.3\" from this project's first message
  until then, and this assertion is the pin that made the flip visible
  rather than silent -- it is re-pinned, once, and the escape hatch is
  pinned beside it so the two move together or not at all."
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
        msh (fn [profile n]
              (message/get-field-first-value
               (parser/parse (first (emit/emit ground-truth ref-date utc-offset
                                                   facility providers profile)))
               "MSH" n))]
    (testing "the DEFAULT profile declares 2.4"
      (is (= "2.4" (msh nil 12))))
    (testing "and a site that must speak 2.3 still can -- the flip's escape hatch"
      ;; Without this, a later change could quietly stop honouring the
      ;; override and only the default would be under test. What such a
      ;; site GIVES UP is pinned separately, in
      ;; `ehrt.conformance.v2-structure-resolution-test`: every 2.3
      ;; message falls back to GenericMessage and is structurally
      ;; unchecked.
      (is (= "2.3" (msh {:msh {:version "2.3"}} 12))))
    (testing "every other MSH default is untouched by the flip"
      (is (= "P" (msh nil 11)))
      (is (= "EHR-TESTING-SIM" (msh nil 3)))
      (is (= "SIM" (msh nil 4))))))

(deftest pv1-2-patient-class-renders-through-the-profiles-code-table
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
        default-parsed (parser/parse (first (emit/emit ground-truth ref-date utc-offset facility providers)))
        aldric-parsed (parser/parse (first (emit/emit ground-truth ref-date utc-offset facility providers aldric-profile)))]
    (testing "no profile: today's hard-coded \"I\""
      (is (= "I" (message/get-field-first-value default-parsed "PV1" 2))))
    (testing "overridden: site code + coding-system suffix"
      (is (= "IN^99ALDRIC" (message/get-field-first-value aldric-parsed "PV1" 2))))))

(deftest pv1-36-discharge-disposition-renders-only-on-discharge-through-the-profiles-code-table
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
        admission-msg (first (emit/emit ground-truth ref-date utc-offset facility providers))
        discharge-msg (first (filter #(re-find #"\^A03" %) (emit/emit ground-truth ref-date utc-offset facility providers)))
        aldric-discharge-msg (first (filter #(re-find #"\^A03" %)
                                            (emit/emit ground-truth ref-date utc-offset facility providers aldric-profile)))]
    (testing "non-discharge messages carry no disposition"
      (is (= "" (or (message/get-field-first-value (parser/parse admission-msg) "PV1" 36) ""))))
    (testing "discharge, no profile: today's standard default"
      (is (= "01" (message/get-field-first-value (parser/parse discharge-msg) "PV1" 36))))
    (testing "discharge, overridden profile"
      (is (= "HOME^99ALDRIC" (message/get-field-first-value (parser/parse aldric-discharge-msg) "PV1" 36))))))

;; --- Milestone site-profiles, Task 3: Z-segment templates -- THE SEAM -----

(def ^:private zpi-profile
  "A profile carrying a ZPI payer Z-segment, triggered on :admission,
  bound to persona/payer state paths plus a literal fallback field."
  (assoc aldric-profile
         :z-segments [{:segment "ZPI" :trigger #{:admission}
                       :fields [{:path [:persona :payer :id]}
                                {:path [:persona :payer :type]}
                                {:path [:persona :payer :nonexistent-key]}
                                {:literal "ALDRIC-PAYER-V1"}]}]))

(deftest z-segment-renders-after-standard-segments-on-its-trigger-event
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
        admission (first (filter #(= :admission (:event %)) ground-truth))
        patient-id (:patient-id (first (:participants admission)))
        {:keys [persona]} (find-registered ground-truth patient-id)
        messages (emit/emit ground-truth ref-date utc-offset facility providers zpi-profile)
        parsed (parser/parse (first messages))]
    (testing "standard segments still present, in order, ahead of the Z-segment
              (IN1 too -- this is an admission message)"
      (is (= ["MSH" "EVN" "PID" "PV1" "IN1" "ZPI"] (mapv (comp name :id) (:segments parsed)))))
    (testing "ZPI-1/ZPI-2: bound to persona/payer state paths"
      (is (= (get-in persona [:payer :id]) (message/get-field-first-value parsed "ZPI" 1)))
      (is (= (name (get-in persona [:payer :type])) (message/get-field-first-value parsed "ZPI" 2))))
    (testing "ZPI-3: an unbound path renders empty, never throws"
      (is (= "" (or (message/get-field-first-value parsed "ZPI" 3) ""))))
    (testing "ZPI-4: literal fallback"
      (is (= "ALDRIC-PAYER-V1" (message/get-field-first-value parsed "ZPI" 4))))))

(deftest z-segment-only-renders-on-its-declared-trigger
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
        messages (emit/emit ground-truth ref-date utc-offset facility providers zpi-profile)
        discharge-msg (first (filter #(re-find #"\^A03" %) messages))]
    (is (empty? (message/get-segments (parser/parse discharge-msg) "ZPI")))))

(deftest no-site-profile-renders-no-z-segments
  (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
        messages (emit/emit ground-truth ref-date utc-offset facility providers)]
    (is (every? #(empty? (message/get-segments (parser/parse %) "ZPI")) messages))))

;; --- Milestone site-profiles, Task 4: the invariance property ------------
;; docs/site-profiles.md's own thesis: two site profiles over one seed
;; produce the SAME ground truth in two accents. The strong half (ground
;; truth) is checked structurally, above (run-test's own
;; not-an-engine-input assertion) and trivially here (both emit calls
;; below consume the exact same `ground-truth` value -- there is no second
;; ground truth to diverge). The weak half (messages) needs the masking
;; function: the precise, documented enumeration of every field a site
;; profile is allowed to touch.

(defn- mask-msh-fields
  "Blanks MSH-3/4/5/6/11/12 (sending/receiving app+facility, processing
  id, version id) -- the declared MSH dialect surface. MSH-1 is the
  field-separator character itself (not a token `str/split` produces);
  MSH-2 (encoding characters) is index 1 after the split, so
  MSH-3/4/5/6/11/12 land at indices 2/3/4/5/10/11. MSH-11 (processing
  id) joined this surface post-M6 (sim/ADR-0014's own Task 4 knob) --
  docs/site-profiles.md's own rule that a new dialect knob extends this
  function in the same change, or the invariance property stops
  actually covering it."
  [msh-line]
  (let [fields (str/split msh-line #"\|" -1)]
    (str/join "|" (map-indexed (fn [i f] (if (#{2 3 4 5 10 11} i) "" f)) fields))))

(defn- mask-pv1-fields
  "Blanks PV1-2/PV1-36 -- the two declared code-table-override fields.
  PV1's segment id occupies split-index 0, so field N lands at index N."
  [pv1-line]
  (let [fields (str/split pv1-line #"\|" -1)]
    (str/join "|" (map-indexed (fn [i f] (if (#{2 36} i) "" f)) fields))))

(defn mask-dialect-surfaces
  "Masks every declared site-profile dialect surface (docs/site-
  profiles.md Task 4, extended post-M6 by sim/ADR-0014's own MSH-11 knob)
  in one rendered ER7 message: MSH-3/4/5/6/11/12, PV1-2/PV1-36, and
  strips Z-segment lines entirely (a Z-segment's
  CONTENT, not merely a field within it, is site-specific -- its bare
  presence following a trigger is still exercised by the Z-segment
  tests above, not by this property). Two profiles' renderings of the
  SAME ground-truth event must be equal after this masking -- the
  invariance property's own precise statement of what a dialect may
  touch, and nothing more."
  [message]
  (->> (str/split message #"\r\n|\r|\n")
       (remove #(re-find #"^Z" %))
       (map (fn [line]
              (cond
                (str/starts-with? line "MSH") (mask-msh-fields line)
                (str/starts-with? line "PV1") (mask-pv1-fields line)
                :else line)))
       (str/join "\r")))

(def ^:private gaudy-profile
  "The deliberately different-looking second profile Task 4 asks for:
  a different HL7 version (2.5.1), a renamed sending facility, custom
  patient-class/disposition codes, and a ZPI payer Z-segment -- exactly
  `zpi-profile` above, reused here under the name this property's own
  intent names it by."
  zpi-profile)

(deftest site-profile-never-reaches-the-engine
  (testing "the invariance property's OWN strong half, stated structurally
            (docs/site-profiles.md): :site-profile is not a member of
            ehrt.sim-engine.config/config-keys, so it is structurally
            incapable of perturbing ground-truth -- not merely untested"
    (is (not (contains? (set config/config-keys) :site-profile)))))

(defspec invariance-messages-agree-after-masking-dialect-surfaces 100
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 6)]
    (let [{:keys [ground-truth facility providers]} (run/run {:seed seed :patients patients})
          default-messages (emit/emit ground-truth ref-date utc-offset facility providers nil)
          gaudy-messages (emit/emit ground-truth ref-date utc-offset facility providers gaudy-profile)]
      (and (= (count default-messages) (count gaudy-messages))
           (= (map mask-dialect-surfaces default-messages)
              (map mask-dialect-surfaces gaudy-messages))))))

(deftest parser-round-trips-messages-bearing-an-unknown-z-segment
  (testing "docs/site-profiles.md Task 3: the parser must still parse
            messages bearing unknown Z-segments -- asserted directly,
            not merely assumed; a failure here would be a documented
            parser finding (notes/facts-register.md), not a silent gap"
    (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
          messages (emit/emit ground-truth ref-date utc-offset facility providers zpi-profile)
          zpi-msg (first messages)]
      (is (some? (parser/parse zpi-msg)))
      (is (= 1 (count (message/get-segments (parser/parse zpi-msg) "ZPI")))))))

;; --- post-M6 (sim/ADR-0014): control-id-for is the ONE source every message
;; builder AND `sim identifiers` derive MSH-10 from -- proven here by
;; checking it against a real run's own rendered messages, not merely
;; against itself.

(defspec control-id-for-matches-every-rendered-messages-own-msh-10 100
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 6)]
    (let [{:keys [ground-truth facility providers]} (run/run {:seed seed :patients patients})
          messages (emit/emit ground-truth ref-date utc-offset facility providers)
          rendered-control-ids (into #{} (map #(message/get-field-first-value (parser/parse %) "MSH" 10)) messages)
          derived-control-ids (into #{} (keep segments/control-id-for) ground-truth)]
      (= rendered-control-ids derived-control-ids))))

;; --- ADR-0150 S-Z: the ADT family sees the WHOLE event ---------------------
;; `.agents/plans/2026-08-16-event-log-census.md`, "One genuine defect found
;; in a consumer": `single-subject-message` handed `z-segments-for` a
;; synthesized seven-key subset while every other family handed it `ev`, so a
;; template bound to any other key rendered EMPTY on ADT and populated
;; everywhere else -- silently, because `render-z-field` never throws on an
;; unbound path. One run, two families, one template: the asymmetry is the
;; whole assertion.

(def ^:private whole-event-profile
  "A profile whose Z-segment binds keys OUTSIDE the seven the ADT builder
  used to synthesize -- `:warm-up` (universal, so an empty rendering is the
  bug and never the data, the census's own probe) and `:home-ward` (present
  on :admission, absent on :order-placed, so its per-family emptiness is
  DATA and is asserted as such)."
  (assoc aldric-profile
         :z-segments [{:segment "ZWU"
                       :trigger #{:admission :order-placed}
                       :fields [{:path [:warm-up]}
                                {:path [:home-ward]}]}]))

(deftest z-segments-see-the-whole-event-on-the-adt-family-too
  (let [{:keys [ground-truth facility providers]} (run-with-order 1)
        messages (emit/emit ground-truth ref-date utc-offset facility providers whole-event-profile)
        adt (parser/parse (find-message messages "A01"))
        orm (parser/parse (find-message messages "O01"))
        admission (first (filter #(= :admission (:event %)) ground-truth))]
    (testing "the non-ADT family has always seen the whole event -- the control"
      (is (= "false" (message/get-field-first-value orm "ZWU" 1))))
    (testing "ADT sees it too: :warm-up is not one of the seven keys the ADT
              builder used to synthesize, and it must render all the same"
      (is (= "false" (message/get-field-first-value adt "ZWU" 1))))
    (testing ":home-ward, an ADT-only key, renders on the ADT message"
      (is (= (:home-ward admission) (message/get-field-first-value adt "ZWU" 2))))
    (testing "and stays empty on the order message, because the ORDER EVENT
              has no :home-ward -- absence is data, not a dropped context"
      (is (= "" (or (message/get-field-first-value orm "ZWU" 2) ""))))))

;; --- ADR-0150 S-6: OBX-6 reads the entry's `:unit`, singular ---------------

(deftest oru-obx6-renders-the-result-entrys-singular-unit
  (let [{:keys [ground-truth facility providers]} (run-with-order 1)
        result (first (filter #(= :result-available (:event %)) ground-truth))
        first-entry (first (:results result))
        oru (find-message (emit/emit ground-truth ref-date utc-offset facility providers) "R01")
        parsed (parser/parse oru)]
    (is (some? first-entry) "a population gate asserts its population is non-empty")
    (testing "the log entry carries :unit, not :units"
      (is (contains? first-entry :unit))
      (is (not (contains? first-entry :units))))
    (testing "OBX-6 renders exactly that value"
      (is (= (:unit first-entry) (message/get-field-first-value parsed "OBX" 6))))))

;; --- arc 3a part 3: the demographic fold reaches the wire -----------------
;;
;; This is what REPLACED ADR-0172 limitations row 6, struck 2026-08-26.
;; That row said a delta folded onto patient state was invisible to every
;; message, and its gate asserted the lookup's key shape -- a NEGATIVE
;; law, red the day the limitation was lifted. Here is the positive one
;; it was standing in for.

(def ^:private folded-persona (sim-model/persona (Random. 99) {}))
(def ^:private folded-pid "PID-000000-abcdef01")
(def ^:private folded-addr {:street "77 Cedar Ln" :city "Portland" :state "OR" :zip "97201"})

(defn- folded-log
  "One patient, admitted, whose address then changes, who is then
  transferred. Hand-built because the engine's own single-encounter
  horizon (sim/ADR-0007 point 3) gives a demographic delta no MESSAGE to
  land in front of: a run's person events fall after its discharge. What
  is asserted here is the emitter's law, not the engine's scheduling."
  [& {:keys [residence]}]
  (let [subject [{:patient-id folded-pid :role :subject}]
        loc {:ward "Renal" :bed "RENAL-01" :placement :licensed}]
    [(cond-> {:event :registered :t 0 :active-mrn "MRN000001" :persona folded-persona
              :participants subject :warm-up false}
       residence (assoc :residence residence))
     {:event :admission :t 10 :active-mrn "MRN000001" :attending "1234567890"
      :home-ward "Renal" :location loc :forced false :participants subject :warm-up false}
     {:event :demographic-update :t 20 :active-mrn "MRN000001" :cause :residence-move
      :field :residence :value {:status :housed :address folded-addr}
      :prior-value {:status :housed :address (:address folded-persona)}
      :person-event-id "PERSON-000000#0" :participants subject :warm-up false}
     {:event :transfer :t 30 :active-mrn "MRN000001" :attending "1234567890"
      :home-ward "Renal" :from loc
      :location {:ward "Renal" :bed "RENAL-02" :placement :licensed}
      :participants subject :warm-up false}]))

(defn- pid-11-of [msg]
  (message/get-field-first-value (parser/parse msg) "PID" 11))

(defn- xad-of
  "The five-component XAD `xad-field` renders for one places row --
  street^^city^state^zip, other-designation always empty."
  [{:keys [street city state zip]}]
  (str street "^^" city "^" state "^" zip))

(deftest demographics-at-answers-state-at-t-test
  (let [msgs (emit/emit (folded-log) ref-date utc-offset)]
    (testing "the fold produced the two messages it should have (A01, then A02)"
      (is (= 2 (count msgs)) "a :demographic-update rendered a message of its own"))
    (let [[admit transfer] msgs]
      (testing "the admission, BEFORE the delta, renders the t0 address"
        (is (= (xad-of (:address folded-persona)) (pid-11-of admit))))
      (testing "the transfer, AFTER it, renders the NEW one -- which is exactly
                what ADR-0172 limitations row 6 said could not happen"
        (is (= (xad-of folded-addr) (pid-11-of transfer)))
        (is (not= (pid-11-of admit) (pid-11-of transfer)))))))

(deftest an-unhoused-patient-renders-pid-11-absent-test
  ;; ADR-0173 ruling E1: PID-11 ABSENT on the wire, with the distinction
  ;; carried in ground truth. No sentinel -- HL7 v2 offers no code for it
  ;; and every literal is one site's local convention, which belongs in a
  ;; site profile.
  (let [housed (emit/emit (folded-log) ref-date utc-offset)
        unhoused (emit/emit (folded-log :residence {:status :unhoused
                                                        :last-known-address folded-addr})
                                ref-date utc-offset)]
    (testing "the housed control renders a street"
      (is (seq (pid-11-of (first housed)))))
    (testing "the unhoused registration renders nothing there"
      (is (str/blank? (str (pid-11-of (first unhoused))))))
    (testing "and it is ABSENT rather than five empty components"
      (is (not (str/includes? (first unhoused) "|^^^^|"))
          "PID-11 rendered as an empty XAD rather than an empty field"))
    (testing "everything else about the message is unchanged, so the absence is
              the ONLY difference the residence sum makes on the wire"
      (is (= (str/replace (first housed) (xad-of (:address folded-persona)) "")
             (first unhoused))))))

(deftest a-later-message-still-renders-a-persons-corrected-name-and-payer-test
  (let [subject [{:patient-id folded-pid :role :subject}]
        loc {:ward "Renal" :bed "RENAL-01" :placement :licensed}
        payer {:id "x" :name "X Health" :type :commercial}
        log [{:event :registered :t 0 :active-mrn "MRN000001" :persona folded-persona
              :participants subject :warm-up false}
             {:event :admission :t 10 :active-mrn "MRN000001" :attending "1234567890"
              :home-ward "Renal" :location loc :forced false :participants subject
              :warm-up false}
             {:event :demographic-update :t 20 :active-mrn "MRN000001"
              :cause :identity-correction :field :name
              :value {:family "Corrected" :given "Name"}
              :prior-value (:name folded-persona)
              :person-event-id "PERSON-000000#1" :participants subject :warm-up false}
             {:event :coverage-change :t 25 :active-mrn "MRN000001" :cause :employment
              :payer payer :prior-payer (:payer folded-persona)
              :person-event-id "PERSON-000000#2" :participants subject :warm-up false}
             {:event :transfer :t 30 :active-mrn "MRN000001" :attending "1234567890"
              :home-ward "Renal" :from loc
              :location {:ward "Renal" :bed "RENAL-02" :placement :licensed}
              :participants subject :warm-up false}]
        msgs (emit/emit log ref-date utc-offset)
        [admit transfer] msgs]
    (is (= 2 (count msgs)))
    (testing "PID-5 follows the correction"
      (is (= (str (:family (:name folded-persona)) "^" (:given (:name folded-persona)))
             (message/get-field-first-value (parser/parse admit) "PID" 5)))
      (is (= "Corrected^Name"
             (message/get-field-first-value (parser/parse transfer) "PID" 5))))
    (testing "and the payer the admission's IN1 carried is the t0 one, because the
              coverage change had not happened yet"
      (is (str/includes? admit (:name (:payer folded-persona)))))))

;; --- arc 3a part 4: the John Doe on the wire, and the fill that ends him ---
;;
;; ADR-0173 section 2(d). GROUND TRUTH knows who a placeholder patient
;; is -- their `:registered` event carries the real Persona -- and the
;; WIRE may not say so. These are the gates on that gap, which is the
;; whole of what the identification flow buys a consumer testing an MPI.

(defn- placeholder-log
  "One unidentified arrival, admitted, whose identity is then
  established. Hand-built for the same reason `folded-log` above is: the
  emitter's law is what is asserted here, not the engine's scheduling."
  [& {:keys [fill?] :or {fill? true}}]
  (let [pid "PID-000004-abcdef02"
        subject [{:patient-id pid :role :subject}]
        loc {:ward "Emergency" :bed "ED-H01" :placement :surge}]
    (cond-> [{:event :registered :t 0 :active-mrn "MRN000005"
              :persona folded-persona
              :person-id "PERSON-000000"
              :identity :placeholder
              :alias-name {:family "Doe" :given "Unknown"}
              :window-close-t 5000
              :residence {:status :unknown}
              :participants subject :warm-up false}
             {:event :admission :t 10 :active-mrn "MRN000005" :attending "1234567890"
              :home-ward "Emergency" :location loc :forced false
              :reason "Unidentified patient" :person-event-id "PERSON-000000#0"
              :participants subject :warm-up false}]
      fill?
      (conj {:event :demographic-update :t 5000 :active-mrn "MRN000005"
             :cause :identity-fill :field :identity :value :known :prior-value :placeholder
             :placeholder-event-id 0 :persona folded-persona
             :person-event-id "PERSON-000000#1" :participants subject :warm-up false})
      true
      (conj {:event :transfer :t 6000 :active-mrn "MRN000005" :attending "1234567890"
             :home-ward "Emergency" :from loc
             :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}
             :participants subject :warm-up false}))))

(deftest a-placeholder-registration-renders-a-john-doe-pid-test
  (let [[admit] (emit/emit (placeholder-log :fill? false) ref-date utc-offset)
        pid (first (filter #(str/starts-with? % "PID") (str/split admit #"\r")))]
    (testing "PID-5 is the window's alias, never the person's real name"
      (is (= "Doe^Unknown" (message/get-field-first-value (parser/parse admit) "PID" 5)))
      (is (not (str/includes? admit (:family (:name folded-persona))))
          "the real family name reached the wire for a patient nobody had identified"))
    (testing "and every fact the hospital does not have renders EMPTY -- DOB, sex,
              address, phone"
      (is (str/blank? (str (pid-11-of admit))))
      (is (= "PID|1||MRN000005||Doe^Unknown||||||||" pid)
          (str "the John Doe PID carries something it should not: " pid)))
    (testing "no IN1 rides the admission: an unidentified patient has no known
              coverage, and an empty IN1 would claim they have none"
      (is (not (str/includes? admit "IN1"))))))

(deftest the-identity-fill-makes-later-messages-render-the-real-patient-test
  (let [msgs (emit/emit (placeholder-log) ref-date utc-offset)
        [admit transfer] msgs]
    (is (= 2 (count msgs)) "the fill rendered a message of its own")
    (testing "before the fill, the John Doe"
      (is (= "Doe^Unknown" (message/get-field-first-value (parser/parse admit) "PID" 5)))
      (is (str/blank? (str (pid-11-of admit)))))
    (testing "after it, the person the record turned out to belong to"
      (let [{:keys [family given]} (:name folded-persona)]
        (is (= (str family "^" given)
               (message/get-field-first-value (parser/parse transfer) "PID" 5))))
      (is (= (xad-of (:address folded-persona)) (pid-11-of transfer))))
    (testing "and the MRN never moved -- a fill keeps the record it fills"
      (is (= (message/get-field-first-value (parser/parse admit) "PID" 3)
             (message/get-field-first-value (parser/parse transfer) "PID" 3))))))

;; --- ADR-0174 ruling C1 (arc 3b sweep 1): PV1-19, the visit number ------
;;
;; The encounter's ONE wire face. Before this sweep PV1-19 was empty on
;; every message this project had ever produced -- one of the 28 blanks
;; `pv1-segment` laid down between PV1-7 and PV1-36 -- which is
;; `registry.clj`'s own comment's definition of a failure mode:
;; traffic invisible to every consumer.

(defn- pv1-19-of [message] (or (message/get-field-first-value (parser/parse message) "PV1" 19) ""))

(defn- pv1-field-count
  "How many fields the PV1 line carries, counted off the wire rather than
  off the source: PV1-36 is the last, so a `pv1-segment` that laid down
  the wrong number of blanks moves this."
  [message]
  (->> (str/split message #"\r\n|\r|\n")
       (filter #(str/starts-with? % "PV1"))
       first
       (#(str/split % #"\|" -1))
       count
       dec))

(deftest pv1-19-is-empty-without-the-encounters-opt-in
  (testing "the blank count moved 28 -> 27 and the BYTE count did not:
            nil renders the same empty field that stood here before"
    (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 3})
          messages (emit/emit ground-truth ref-date utc-offset facility providers)]
      (is (seq messages))
      (is (every? #(= "" (pv1-19-of %)) messages))
      (is (every? #(= 36 (pv1-field-count %)) messages)
          "PV1 still ends at PV1-36 -- 7 explicit + 11 blank + PV1-19 + 16 blank + PV1-36"))))

(deftest pv1-19-renders-the-encounter-id-when-the-run-opted-in
  (let [{:keys [ground-truth facility providers]}
        (run/run {:seed 42 :patients 3 :encounters true})
        messages (emit/emit ground-truth ref-date utc-offset facility providers)
        by-message (mapv (juxt #(message/get-field-first-value (parser/parse %) "MSH" 9) pv1-19-of)
                         messages)]
    (is (seq messages))
    (testing "every message carries its encounter's own visit number"
      (is (every? (fn [[_ v]] (re-matches #"ENC-\d{6}-\d{2}-[0-9a-f]{8}" v)) by-message)
          (str "a PV1 rendered no visit number: " (pr-str by-message))))
    (testing "and it is the SAME id the ground-truth event carries -- the
              cross-emitter id sub-law, one level down"
      (is (= (mapv :encounter-id (filterv #(registry/message-type-registry (:event %)) ground-truth))
             (mapv second by-message))))
    (is (every? #(= 36 (pv1-field-count %)) messages))))

(deftest a17-renders-each-patients-own-visit-number
  (testing "a `:bed-swap` names TWO encounters and carries neither at top
            level, so each PV1-19 comes from that patient's own `:swap`
            entry -- the same place its PV1-3 comes from"
    (let [world0 (assoc (world-of {"P1" (state/initial-patient "P1" "MRN000001")
                                   "P2" (state/initial-patient "P2" "MRN000002")})
                        :encounter-minting {:seed 7 :ordinals {"P1" 0 "P2" 1}})
          world1 (-> world0 (admit 0 "P1" "Renal") (admit 5 "P2" "Renal"))
          {:keys [events]} (decide/decide (streams/one-stream (Random. 1)) 10 world1 "P1" {:type :bed-swap})
          world2 (fold-events world1 events)
          messages (emit/emit (:ground-truth world2) ref-date utc-offset churn-facility churn-providers)
          parsed (parser/parse (last messages))
          pv1-19s [(nth-field-value parsed "PV1" 19 0) (nth-field-value parsed "PV1" 19 1)]]
      (is (= "ADT^A17" (message/get-field-first-value parsed "MSH" 9)))
      (is (= [(streams/encounter-id-for 7 0 0) (streams/encounter-id-for 7 1 0)] pv1-19s)
          "two patients, two visit numbers, neither borrowed from the other"))))
