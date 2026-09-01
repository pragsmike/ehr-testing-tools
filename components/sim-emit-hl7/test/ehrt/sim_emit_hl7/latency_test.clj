(ns ehrt.sim-emit-hl7.latency-test
  "ADR-0109: the second clock. `plan-latency`'s own fixed-RNG-consumption
  law (the RNG-path law's own worked precedent, `ehrt.sim-engine.assignment/
  assign-pathway`/`assign-module`, extended here); `emit-wire`'s own
  split-clock rendering (MSH-7 transmit time, EVN-2 clinical time --
  ADR-0109's own field audit) and transmit-time ordering; and the
  identity property `emit-wire` rests on: absent/nil/{} offsets renders
  byte-identical to `emit`, in `emit`'s own order. Written test-first
  per that session's own red-then-green requirement on every gate
  touched (build-session ceremony).

  The RESULT wire's own half of the split clock (OBR-7/OBX-14, ADR-0142,
  2026-08-16) lives in the sibling `ehrt.sim-emit-hl7.result-clock-test`
  -- half of what that file asserts is about plain `emit` rather than
  about latency at all. The 100-trial identity property below is
  deliberately NOT duplicated there and must stay green through any
  emitter-seam change: both sides move together or neither does."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-engine.churn :as churn]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-emit-hl7.emit :as emit]
            [ehrt.sim-emit-hl7.hl7-time :as hl7-time]
            [ehrt.sim-emit-hl7.planners :as planners]
            [ehrt.sim-emit-hl7.segments :as segments]
            [com.nervestaple.hl7-parser.parser :as parser]
            [com.nervestaple.hl7-parser.message :as message])
  (:import [java.util Random]))

(def ref-date "2024-01-01")
(def utc-offset "+00:00")

;; --- The identity property: emit-wire's own absent/nil/{}-offsets law,
;; the site-profile milestone's own default-profile identity precedent
;; extended to offsets. -------------------------------------------------

(defspec emit-wire-with-absent-nil-or-empty-offsets-is-byte-identical-to-emit 100
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 8)
                 use-churn gen/boolean]
    (let [config (cond-> {:seed seed :patients patients}
                   use-churn (assoc :churn-profile churn/sample-profile))
          {:keys [ground-truth facility providers]} (run/run config)
          plain (emit/emit ground-truth ref-date utc-offset facility providers)
          wire-nil-offsets (emit/emit-wire ground-truth ref-date utc-offset facility providers nil nil)
          wire-empty-offsets (emit/emit-wire ground-truth ref-date utc-offset facility providers nil {})]
      (= plain wire-nil-offsets wire-empty-offsets))))

(defspec plan-latency-with-an-absent-profile-draws-and-discards-and-returns-empty 50
  (prop/for-all [seed (gen/large-integer* {:min 0})
                 patients (gen/choose 1 8)
                 rng-seed gen/large-integer]
    (let [{:keys [ground-truth]} (run/run {:seed seed :patients patients :churn-profile churn/sample-profile})]
      (= {} (planners/plan-latency (Random. ^long rng-seed) ground-truth nil)))))

;; --- Fixed RNG consumption: the RNG-path law's own worked precedent
;; (assign-pathway/assign-module, assignment.clj) extended to plan-latency ---

(deftest plan-latency-adding-a-covered-event-type-never-shifts-another-types-own-offset
  (testing "ALWAYS one draw per ground-truth event, regardless of
            coverage: covering :discharge in addition to :admission must
            not change any :admission offset already drawn under the
            narrower profile -- proven with churn on, so the log carries
            a real mix of event types, not merely the two under test"
    (let [{:keys [ground-truth]} (run/run {:seed 7 :patients 6 :churn-profile churn/sample-profile})
          admission-only {:admission {:from-minutes 5 :to-minutes 45}}
          admission-and-discharge {:admission {:from-minutes 5 :to-minutes 45}
                                   :discharge {:from-minutes 1 :to-minutes 10}}
          offsets-narrow (planners/plan-latency (Random. 123) ground-truth admission-only)
          offsets-wide (planners/plan-latency (Random. 123) ground-truth admission-and-discharge)]
      (is (seq offsets-narrow) "non-vacuity: at least one :admission event exists")
      (is (> (count offsets-wide) (count offsets-narrow))
          "non-vacuity: covering :discharge actually added entries, proving the wider profile was live")
      (doseq [[control-id offset] offsets-narrow]
        (is (= offset (get offsets-wide control-id))
            (str "admission offset for " control-id " shifted when :discharge joined the profile"))))))

(deftest plan-latency-samples-uniformly-within-the-declared-minutes-range-converted-to-seconds
  (testing "offset-seconds falls within [from-minutes*60, to-minutes*60]
            for every covered event, over many distinct RNG seeds"
    (let [{:keys [ground-truth]} (run/run {:seed 3 :patients 4})
          profile {:admission {:from-minutes 10 :to-minutes 20}}]
      (doseq [rng-seed (range 30)]
        (let [offsets (planners/plan-latency (Random. rng-seed) ground-truth profile)]
          (is (seq offsets))
          (doseq [[_ offset] offsets]
            (is (<= 600 offset 1200))))))))

;; --- The split clock, end to end: MSH-7 (transmit) moves, EVN-2
;; (clinical) does not -- this session's own field audit, proven live ---

(deftest emit-wire-shifts-msh-7-transmit-time-and-leaves-evn-2-clinical-time-unchanged
  (testing "the ONLY two timestamp-bearing fields this emitter renders
            (this session's own field audit, notes/adr/0109-*.md) diverge
            exactly as the audit says: MSH-7 (message/transmit time)
            shifts by the offset, EVN-2 (event/clinical time) does not
            move at all"
    (let [{:keys [ground-truth facility providers]} (run/run {:seed 42 :patients 1})
          admission (first (filter #(= :admission (:event %)) ground-truth))
          control-id (segments/control-id-for admission)
          offset-seconds 3600
          offsets {control-id offset-seconds}
          plain-messages (emit/emit ground-truth ref-date utc-offset facility providers)
          wire-messages (emit/emit-wire ground-truth ref-date utc-offset facility providers nil offsets)
          plain-parsed (parser/parse (first (filter #(re-find #"\^A01" %) plain-messages)))
          wire-parsed (parser/parse (first (filter #(re-find #"\^A01" %) wire-messages)))]
      (testing "EVN-2 unchanged (clinical time)"
        (is (= (message/get-field-first-value plain-parsed "EVN" 2)
               (message/get-field-first-value wire-parsed "EVN" 2))))
      (testing "MSH-7 shifted by exactly the offset"
        (is (= (hl7-time/hl7-timestamp ref-date (+ (:t admission) offset-seconds) utc-offset)
               (message/get-field-first-value wire-parsed "MSH" 7)))
        (is (not= (message/get-field-first-value plain-parsed "MSH" 7)
                  (message/get-field-first-value wire-parsed "MSH" 7)))))))

(deftest emit-wire-msh-only-message-types-shift-their-sole-timestamp-field
  (testing "ORM^O01 (order) carries no EVN and no rendered OBR-7/ORC-9
            -- MSH-7 is its ONLY timestamp field, so it shifts
            unconditionally.

            AMENDED 2026-08-16 (ADR-0142): this docstring used to say
            'ORM^O01/ORU^R01', on ADR-0109's own field audit. ADR-0142
            put OBR-7 and OBX-14 on all three ORU shapes, so ORU is no
            longer an MSH-only type and its own split-clock assertions
            live in `ehrt.sim-emit-hl7.result-clock-test`. The
            ASSERTIONS below are unchanged and always exercised ORM
            alone (`\\^O01`); only the claim they were described under
            was wider than what they tested. ORM stays byte-frozen by
            author ruling Q3, 'Results only; ORM byte-frozen'"
    (let [pathway {:name "cbc-order" :steps [{:type :admission :location "Renal"}
                                             {:type :order :profile :cbc}
                                             {:type :discharge}]}
          {:keys [ground-truth facility providers]}
          (run/run {:seed 7 :patients 1 :pathways [{:pathway pathway :weight 1}]})
          order-placed (first (filter #(= :order-placed (:event %)) ground-truth))
          control-id (segments/control-id-for order-placed)
          offsets {control-id 900}
          plain-messages (emit/emit ground-truth ref-date utc-offset facility providers)
          wire-messages (emit/emit-wire ground-truth ref-date utc-offset facility providers nil offsets)
          plain-orm (first (filter #(re-find #"\^O01" %) plain-messages))
          wire-orm (first (filter #(re-find #"\^O01" %) wire-messages))]
      (is (= (hl7-time/hl7-timestamp ref-date (+ (:t order-placed) 900) utc-offset)
             (message/get-field-first-value (parser/parse wire-orm) "MSH" 7)))
      (is (not= (message/get-field-first-value (parser/parse plain-orm) "MSH" 7)
                (message/get-field-first-value (parser/parse wire-orm) "MSH" 7))))))

;; --- Transmit-time ordering: the disorder mechanism itself ----------------

(deftest emit-wire-orders-messages-by-transmit-time-reordering-a-lagged-event-past-its-followers
  (testing "the disorder mechanism itself: a large offset on an EARLY
            clinical event can push its own transmit instant PAST a
            LATER event's own unshifted transmit instant -- exactly the
            downstream-receiver reality ADR-0109 exists to supply
            (author charter, docs/dev/simulator-architecture.md section 5)"
    (let [pathway {:name "admission-transfer-discharge"
                   :steps [{:type :admission :location "Renal"}
                           {:type :delay :from 30 :to 30}
                           {:type :transfer :location "Cardiology"}
                           {:type :delay :from 30 :to 30}
                           {:type :discharge}]}
          {:keys [ground-truth facility providers]}
          (run/run {:seed 1 :patients 1 :pathways [{:pathway pathway :weight 1}]})
          admission (first (filter #(= :admission (:event %)) ground-truth))
          control-id (segments/control-id-for admission)
          offsets {control-id (* 999 3600)}
          plain-messages (emit/emit ground-truth ref-date utc-offset facility providers)
          wire-messages (emit/emit-wire ground-truth ref-date utc-offset facility providers nil offsets)]
      (testing "log order: admission first"
        (is (re-find #"\^A01" (first plain-messages))))
      (testing "wire order: admission no longer first -- its own huge
                transmit lag pushed it past every other message"
        (is (not (re-find #"\^A01" (first wire-messages))))
        (is (re-find #"\^A01" (last wire-messages))))
      (testing "wire output is sorted by transmit time (MSH-7), non-decreasing"
        (let [transmit-ts (mapv #(message/get-field-first-value (parser/parse %) "MSH" 7) wire-messages)]
          (is (= transmit-ts (sort transmit-ts)))))
      (testing "the SAME clinical events, just reordered -- never dropped or
                duplicated (identified by trigger + EVN-2, the unshifted
                clinical instant, since MSH-7 legitimately diverges for the
                one lagged event under test)"
        (let [clinical-key (fn [m]
                             (let [parsed (parser/parse m)]
                               [(message/get-field-first-value parsed "MSH" 9)
                                (message/get-field-first-value parsed "EVN" 2)]))]
          (is (= (frequencies (map clinical-key plain-messages))
                 (frequencies (map clinical-key wire-messages)))))
        (is (= (count plain-messages) (count wire-messages)))))))
