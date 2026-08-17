(ns ehrt.sim-emit-hl7.result-clock-test
  "ADR-0142: clinical time on the result wire. ADR-0109 built the second
  clock and, in the same step, recorded what it could not shift: its own
  field audit found OBR-7 and OBX-14 simply NOT RENDERED, so a
  latency-shifted ORU carried only MSH-7 and a downstream receiver had
  nothing on the wire with which to back-date a late result. This
  namespace is that gap's own gate.

  WHY A SIBLING OF `latency-test` RATHER THAN MORE OF IT. Half of what
  is asserted here is about PLAIN `emit` -- that a result message
  carries its own clinical instant at all, latency or no latency --
  which `latency-test`'s own namespace docstring (\"the second clock\")
  would misdescribe. The other half IS the split-clock law, made for
  results exactly as `latency-test` already makes it for EVN-2. Keeping
  both halves together, under a name that says which wire and which
  clock, is what makes the file self-describing; `latency-test`'s own
  100-trial identity property is left where it is and re-run, never
  duplicated here.

  ALL THREE ORU SHAPES (author ruling Q2 \"a\"): `:result-available`
  (ORC+OBR+one OBX per analyte), `:observation` (a single OBX, no
  ORC/OBR -- the unsolicited-observation shape), and
  `:diagnostic-report` (ORC+OBR+one OBX per embedded child).

  FIELD READING. OBR-7 and OBX-14 are read off the RAW ER7 text by
  splitting a segment on `|`, not through the parser's own field
  accessors: a segment split gives `[\"OBX\" f1 f2 ...]`, so field n is
  element n exactly, and an ABSENT trailing field reads as nil rather
  than as whatever an accessor happens to do at an out-of-range index.
  That distinction is the whole point of a red-first run here -- the
  red must be \"the field is not there\", unambiguously. MSH-7 keeps
  the parser (MSH's own off-by-one field numbering is what the
  accessor exists for)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-emit-hl7.emit-hl7 :as emit-hl7]
            [com.nervestaple.hl7-parser.parser :as parser]
            [com.nervestaple.hl7-parser.message :as message]))

(def ref-date "2024-01-01")
(def utc-offset "+00:00")

;; --- raw-ER7 helpers (see the namespace docstring) ------------------------

(defn- segments-named
  "Every `seg-name` segment of `er7`, as raw segment strings, in order."
  [er7 seg-name]
  (->> (str/split er7 #"\r")
       (filter #(str/starts-with? % (str seg-name "|")))
       vec))

(defn- raw-field
  "Field `n` of a raw non-MSH segment string, or nil when the segment
  carries no such field at all."
  [segment n]
  (let [parts (str/split segment #"\|" -1)]
    (when (> (count parts) n)
      (nth parts n))))

(defn- msh-7 [er7]
  (message/get-field-first-value (parser/parse er7) "MSH" 7))

(defn- oru-for
  "The single ORU^R01 in `messages` (every pathway below produces exactly
  one)."
  [messages]
  (first (filter #(re-find #"\|ORU\^R01\|" %) messages)))

;; --- the three ORU shapes, as pathway steps -------------------------------

(def ^:private an-analyte {:system :loinc :code "8480-6" :display "Systolic Blood Pressure"})
(def ^:private another-analyte {:system :loinc :code "8462-4" :display "Diastolic Blood Pressure"})
(def ^:private a-report-concept {:system :loinc :code "600-7" :display "Bacteria identified in Blood by Culture"})

(def ^:private result-available-pathway
  {:name "cbc-order" :steps [{:type :admission :location "Renal"}
                             {:type :order :profile :cbc}
                             {:type :discharge}]})

(def ^:private observation-pathway
  {:name "vitals" :steps [{:type :admission :location "Renal"}
                          {:type :observation :codes [an-analyte] :value 98.0 :unit "mm[Hg]"}]})

(def ^:private diagnostic-report-pathway
  {:name "bp-panel" :steps [{:type :admission :location "Renal"}
                            {:type :diagnostic-report :codes [a-report-concept]
                             :observations [{:codes [an-analyte] :value 92.0 :unit "mm[Hg]"}
                                            {:codes [another-analyte] :value 64.0 :unit "mm[Hg]"}]}]})

(defn- run-with
  "One patient down `pathway`, emitted plainly. Returns the run's own
  ground truth, the emitted messages, and the result event whose clock
  this namespace is about."
  [pathway seed result-event-type]
  (let [{:keys [ground-truth facility providers]}
        (engine/run {:seed seed :patients 1 :pathways [{:pathway pathway :weight 1}]})
        messages (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        result-event (first (filter #(= result-event-type (:event %)) ground-truth))]
    {:ground-truth ground-truth :facility facility :providers providers
     :messages messages :result-event result-event}))

;; --- Plain `emit`: the clinical instant reaches the wire at all -----------

(deftest result-available-oru-renders-obr-7-and-obx-14-as-the-events-own-clinical-time
  (testing "ORU^R01 (order-linked): OBR-7 and every OBX-14 carry the
            result event's own :t, rendered by `hl7-timestamp` exactly
            as EVN-2's clinical-ts is (author ruling Q1 \"a\") -- and
            under plain `emit`, with no offsets anywhere, they equal
            MSH-7 too (the identity case: one instant, three fields)"
    (let [{:keys [messages result-event]} (run-with result-available-pathway 7 :result-available)
          oru (oru-for messages)
          expected (emit-hl7/hl7-timestamp ref-date (:t result-event) utc-offset)
          obr (first (segments-named oru "OBR"))
          obxs (segments-named oru "OBX")]
      (is (some? result-event) "non-vacuity: the pathway really produced a :result-available")
      (is (some? oru) "non-vacuity: it really rendered an ORU^R01")
      (is (some? obr) "non-vacuity: this shape really carries an OBR")
      (is (seq obxs) "non-vacuity: this shape really carries OBX segments")
      (testing "OBR-7 = the event's own clinical instant"
        (is (= expected (raw-field obr 7))))
      (testing "OBX-14 = the same instant, on every OBX"
        (doseq [[i obx] (map-indexed vector obxs)]
          (is (= expected (raw-field obx 14))
              (str "OBX-14 missing or wrong on OBX #" (inc i)))))
      (testing "identity under plain emit: no offsets, so MSH-7 agrees"
        (is (= expected (msh-7 oru)))))))

(deftest observation-oru-renders-obx-14-as-the-events-own-clinical-time
  (testing "ORU^R01 (unsolicited observation, a single OBX and NO
            ORC/OBR): OBX-14 carries the event's own :t. This is the
            shape whose builder's docstring said 'never a positional
            pad' -- ruled Q2 \"a\", the pad is accepted for OBX-14"
    (let [{:keys [messages result-event]} (run-with observation-pathway 1 :observation)
          oru (oru-for messages)
          expected (emit-hl7/hl7-timestamp ref-date (:t result-event) utc-offset)
          obxs (segments-named oru "OBX")]
      (is (some? result-event) "non-vacuity: the pathway really produced an :observation")
      (is (some? oru) "non-vacuity: it really rendered an ORU^R01")
      (is (empty? (segments-named oru "OBR"))
          "this shape carries no OBR at all -- so OBR-7 is not owed here")
      (is (= 1 (count obxs)) "non-vacuity: exactly one OBX, the shape's own signature")
      (is (= expected (raw-field (first obxs) 14)))
      (is (= expected (msh-7 oru))))))

(deftest diagnostic-report-oru-renders-obr-7-and-obx-14-on-every-child
  (testing "ORU^R01 (report panel, ORC+OBR present): OBR-7 once, OBX-14
            on EVERY embedded child's own OBX -- the multi-OBX case
            proving the field rides `observation-obx-segment` itself,
            not a single call site"
    (let [{:keys [messages result-event]} (run-with diagnostic-report-pathway 1 :diagnostic-report)
          oru (oru-for messages)
          expected (emit-hl7/hl7-timestamp ref-date (:t result-event) utc-offset)
          obr (first (segments-named oru "OBR"))
          obxs (segments-named oru "OBX")]
      (is (some? result-event) "non-vacuity: the pathway really produced a :diagnostic-report")
      (is (some? oru) "non-vacuity: it really rendered an ORU^R01")
      (is (= 2 (count obxs)) "non-vacuity: two children, two OBX segments")
      (is (= expected (raw-field obr 7)))
      (doseq [[i obx] (map-indexed vector obxs)]
        (is (= expected (raw-field obx 14))
            (str "OBX-14 missing or wrong on child OBX #" (inc i))))
      (is (= expected (msh-7 oru))))))

;; --- The split clock, made for results ------------------------------------
;; ADR-0109 proved this for EVN-2 on ADT messages. The whole point of
;; ADR-0142 is that a result message now has a clinical field to hold
;; still while MSH-7 moves.

(defn- split-clock-case
  "Emits `pathway` plainly and again through `emit-wire` with a covering
  offset on the result event's own control-id. Returns both ORUs plus
  the offset applied."
  [pathway seed result-event-type offset-seconds]
  (let [{:keys [ground-truth facility providers messages result-event]}
        (run-with pathway seed result-event-type)
        control-id (emit-hl7/control-id-for result-event)
        offsets {control-id offset-seconds}
        wire (emit-hl7/emit-wire ground-truth ref-date utc-offset facility providers nil offsets)]
    {:result-event result-event
     :plain-oru (oru-for messages)
     :wire-oru (oru-for wire)
     :offset offset-seconds}))

(deftest emit-wire-shifts-msh-7-on-every-oru-shape-and-never-obr-7-or-obx-14
  (testing "the split-clock law ADR-0109 asserts for EVN-2, asserted for
            results: a covering LatencyProfile offset moves the result
            message's TRANSMIT time and leaves its CLINICAL fields
            exactly where plain `emit` put them"
    (doseq [[label pathway seed event-type] [["result-available" result-available-pathway 7 :result-available]
                                             ["observation" observation-pathway 1 :observation]
                                             ["diagnostic-report" diagnostic-report-pathway 1 :diagnostic-report]]]
      (testing label
        (let [{:keys [result-event plain-oru wire-oru offset]}
              (split-clock-case pathway seed event-type 5400)
              clinical (emit-hl7/hl7-timestamp ref-date (:t result-event) utc-offset)]
          (testing "MSH-7 shifted by exactly the offset"
            (is (= (emit-hl7/hl7-timestamp ref-date (+ (:t result-event) offset) utc-offset)
                   (msh-7 wire-oru)))
            (is (not= (msh-7 plain-oru) (msh-7 wire-oru))
                "non-vacuity: the offset really moved the transmit clock"))
          (testing "OBR-7 did NOT move (where this shape renders an OBR)"
            (when-let [wire-obr (first (segments-named wire-oru "OBR"))]
              (is (= clinical (raw-field wire-obr 7)))
              (is (= (raw-field (first (segments-named plain-oru "OBR")) 7)
                     (raw-field wire-obr 7)))))
          (testing "OBX-14 did NOT move, on any OBX"
            (let [plain-obxs (segments-named plain-oru "OBX")
                  wire-obxs (segments-named wire-oru "OBX")]
              (is (= (count plain-obxs) (count wire-obxs)))
              (doseq [[plain-obx wire-obx] (map vector plain-obxs wire-obxs)]
                (is (= clinical (raw-field wire-obx 14)))
                (is (= (raw-field plain-obx 14) (raw-field wire-obx 14)))))))))))

(deftest emit-wire-with-empty-offsets-is-byte-identical-to-emit-on-every-oru-shape
  (testing "the identity property, held THROUGH this change on exactly
            the message family this change touches -- both sides moved
            together or neither did. `latency-test`'s own 100-trial
            generative version stays where it is and is re-run, not
            duplicated; this is its per-shape witness"
    (doseq [[label pathway seed] [["result-available" result-available-pathway 7]
                                  ["observation" observation-pathway 1]
                                  ["diagnostic-report" diagnostic-report-pathway 1]]]
      (testing label
        (let [{:keys [ground-truth facility providers messages]} (run-with pathway seed :admission)]
          (is (= messages
                 (emit-hl7/emit-wire ground-truth ref-date utc-offset facility providers nil {})
                 (emit-hl7/emit-wire ground-truth ref-date utc-offset facility providers nil nil))))))))

;; --- Contract neutrality: this is an emitter-seam change, not a schema
;; change (ADR-0141 Q-A's versioning is untouched) -------------------------

(deftest the-result-clock-increment-does-not-touch-the-event-log-contract
  (testing "ADR-0142 renders two HL7v2 fields from a value the event log
            ALREADY carries (`:t`). Nothing enters the log, so the
            contract's own version must not move -- a bump here would
            mean this session had quietly changed the ground truth's
            shape. Asserted against both halves of ADR-0141's
            two-artifact gate: the live schema's version and the
            committed EDN export's own stamp"
    (is (= "1.0.0" engine/event-schema-version))
    (let [export (edn/read-string (slurp (io/resource "sim-engine/event-schema.edn")))]
      (is (= "1.0.0" (:event-schema-version export))))))
