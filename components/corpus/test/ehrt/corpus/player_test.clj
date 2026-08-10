(ns ehrt.corpus.player-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.corpus.player :as player]))

(defn- msh
  "A minimal synthetic message carrying just what these tests need:
  MSH-7 (dtm), optionally MSH-9 (type-trigger), optionally a PID
  segment with PID-3 (patient-id)."
  ([dtm] (msh dtm nil nil))
  ([dtm type-trigger] (msh dtm type-trigger nil))
  ([dtm type-trigger patient-id]
   (str "MSH|^~\\&|A|B|C|D|" dtm "||" type-trigger
        (when patient-id (str "\rPID|1||" patient-id)))))

;; ---- lenient DTM parse ----

(deftest parse-dtm-lenient-full-stamp-test
  (is (= (player/parse-dtm-lenient "20260101000000")
         (player/parse-dtm-lenient "20260101000000"))) ;; sanity: deterministic
  (is (some? (player/parse-dtm-lenient "20260101000000"))))

(deftest parse-dtm-lenient-date-only-defaults-time-to-midnight-test
  (is (= (player/parse-dtm-lenient "20260101")
         (player/parse-dtm-lenient "20260101000000"))))

(deftest parse-dtm-lenient-fraction-and-zone-ignored-test
  (is (= (player/parse-dtm-lenient "20260101120000")
         (player/parse-dtm-lenient "20260101120000.1234+0500"))))

(deftest parse-dtm-lenient-garbage-and-blank-is-nil-test
  (is (nil? (player/parse-dtm-lenient "not a date")))
  (is (nil? (player/parse-dtm-lenient "")))
  (is (nil? (player/parse-dtm-lenient nil))))

(deftest parse-dtm-lenient-orders-later-stamps-higher-test
  (is (< (player/parse-dtm-lenient "20260101000000")
         (player/parse-dtm-lenient "20260102000000"))))

;; ---- field extraction (MSH-7, MSH-9, PID-3), synthetic messages ----

(deftest message-timestamp-ms-reads-msh-7-test
  (is (= (player/parse-dtm-lenient "20260715142300")
         (player/message-timestamp-ms (msh "20260715142300")))))

(deftest message-timestamp-ms-nil-for-unparseable-test
  (is (nil? (player/message-timestamp-ms (msh "")))))

(deftest message-type-trigger-reads-msh-9-test
  (is (= "ADT^A01" (player/message-type-trigger (msh "20260101000000" "ADT^A01")))))

(deftest message-patient-id-reads-pid-3-test
  (is (= "445566^^^CGH^MR"
         (player/message-patient-id (msh "20260101000000" "ADT^A01" "445566^^^CGH^MR")))))

(deftest message-patient-id-nil-when-no-pid-segment-test
  (is (nil? (player/message-patient-id (msh "20260101000000" "ADT^A01")))))

;; ---- field extraction against the real, shared v2 fixtures ----

(defn- fixture
  [name]
  (slurp (clojure.java.io/file "test-fixtures/v2" name)))

(deftest message-timestamp-ms-reads-real-fixture-msh-7-test
  (is (= (player/parse-dtm-lenient "20260715142300")
         (player/message-timestamp-ms (fixture "adt-a01-admit.hl7")))))

(deftest message-type-trigger-reads-real-fixture-msh-9-test
  (is (= "ADT^A01^ADT_A01" (player/message-type-trigger (fixture "adt-a01-admit.hl7")))))

(deftest message-patient-id-reads-real-fixture-pid-3-test
  (is (= "445566^^^CGH^MR" (player/message-patient-id (fixture "adt-a01-admit.hl7")))))

;; ---- plan: order preservation ----

(deftest plan-preserves-event-order-and-content-test
  (let [events [(msh "20260101000000") (msh "20260101000010") (msh "garbage") (msh "20260101000005")]]
    (is (= events (map second (:plan (player/plan events {})))))))

(deftest plan-preserves-event-order-property-test
  (let [check-result
        (tc/quick-check 100
          (prop/for-all [events (gen/vector (gen/fmap #(msh (str %)) gen/nat) 0 20)]
            (= events (map second (:plan (player/plan events {}))))))]
    (is (:pass? check-result) (str check-result))))

;; ---- plan: wait-ms arithmetic ----

(deftest plan-first-event-always-zero-wait-test
  (let [{:keys [plan]} (player/plan [(msh "20260101000000")] {:rate 1})]
    (is (= 0 (ffirst plan)))))

(deftest plan-computes-rate-divided-wait-test
  (let [events [(msh "20260101000000") (msh "20260101000010")] ;; 10s apart
        {:keys [plan clamped-count unparseable-count skip-count]}
        (player/plan events {:rate 1 :idle-cap-ms 60000})]
    (is (= [0 10000] (mapv first plan)))
    (is (zero? clamped-count))
    (is (zero? unparseable-count))
    (is (zero? skip-count))))

(deftest plan-divides-by-rate-test
  (let [events [(msh "20260101000000") (msh "20260101001000")] ;; 600s apart
        {:keys [plan]} (player/plan events {:rate 60 :idle-cap-ms 600000})]
    (is (= [0 10000] (mapv first plan))))) ;; 600s / 60 = 10s = 10000ms

(deftest plan-caps-wait-at-idle-cap-and-counts-skip-test
  (let [events [(msh "20260101000000") (msh "20260101010000")] ;; 1hr apart
        {:keys [plan skip-count capped-indices clamped-count]}
        (player/plan events {:rate 1 :idle-cap-ms 5000})]
    (is (= [0 5000] (mapv first plan)))
    (is (= 1 skip-count))
    (is (= #{1} capped-indices))
    (is (zero? clamped-count))))

(deftest plan-clamps-negative-delta-and-counts-clamped-test
  (let [events [(msh "20260101001000") (msh "20260101000000")] ;; out of order
        {:keys [plan clamped-count skip-count]}
        (player/plan events {:rate 1 :idle-cap-ms 60000})]
    (is (= [0 0] (mapv first plan)))
    (is (= 1 clamped-count))
    (is (zero? skip-count))))

(deftest plan-unparseable-timestamp-paces-zero-and-counts-test
  (let [events [(msh "20260101000000") (msh "not-a-date") (msh "20260101000010")]
        {:keys [plan unparseable-count]}
        (player/plan events {:rate 1 :idle-cap-ms 60000})]
    ;; event 2 (unparseable) paces at 0 after event 1; event 3 paces
    ;; against event 1's own timestamp too (the unparseable one never
    ;; corrupts a later delta), so its wait is the full 10s.
    (is (= [0 0 10000] (mapv first plan)))
    (is (= 1 unparseable-count))))

(deftest plan-capped-wait-is-never-also-clamped-test
  (let [events [(msh "20260101000000") (msh "20260101010000")]
        {:keys [clamped-count skip-count]} (player/plan events {:rate 1 :idle-cap-ms 5000})]
    (is (zero? clamped-count))
    (is (= 1 skip-count))))

;; ---- plan: the show identity -- rate at (or near) infinity yields
;; all-zero waits with no special-cased sentinel ----

(deftest plan-at-huge-rate-yields-all-zero-waits-test
  (let [events [(msh "20260101000000") (msh "20260716091200")] ;; a real multi-month-scale gap
        {:keys [plan skip-count]} (player/plan events {:rate 1e15 :idle-cap-ms 5000})]
    (is (every? zero? (map first plan)))
    (is (zero? skip-count))))

(deftest plan-at-positive-infinity-rate-yields-all-zero-waits-test
  (let [events [(msh "20260101000000") (msh "20260716091200")]
        {:keys [plan]} (player/plan events {:rate Double/POSITIVE_INFINITY :idle-cap-ms 5000})]
    (is (every? zero? (map first plan)))))

;; ---- property: wait-ms = min(cap, max(0, delta)/rate), against
;; hand-computed deltas over a generated, strictly increasing timestamp
;; sequence (so no clamping ever fires, isolating the arithmetic) ----

(def ^:private increasing-seconds-gen
  "A vector of strictly increasing integer second-offsets from a fixed
  epoch, length 2..8 -- deterministic deltas to check the arithmetic
  against by hand."
  (gen/fmap (fn [deltas] (reductions + 0 deltas))
            (gen/vector (gen/choose 1 100000) 1 7)))

(defn- epoch-ms->dtm
  "Test-only inverse of parse-dtm-lenient, full precision, UTC --
  builds a DTM string a real message could carry from an epoch-ms this
  test already knows, so the expected delta is computed independently
  of parse-dtm-lenient itself."
  [epoch-ms]
  (let [instant (java.time.Instant/ofEpochMilli epoch-ms)
        ldt (java.time.LocalDateTime/ofInstant instant java.time.ZoneOffset/UTC)]
    (format "%04d%02d%02d%02d%02d%02d"
            (.getYear ldt) (.getMonthValue ldt) (.getDayOfMonth ldt)
            (.getHour ldt) (.getMinute ldt) (.getSecond ldt))))

(deftest plan-wait-ms-matches-hand-computed-deltas-real-property-test
  (let [base (player/parse-dtm-lenient "20260101000000")
        check-result
        (tc/quick-check 100
          (prop/for-all [offsets-s increasing-seconds-gen
                         rate (gen/elements [1 2 5 60 3600])
                         idle-cap-s (gen/choose 1 1000)]
            (let [epochs (mapv #(+ base (* 1000 %)) offsets-s)
                  events (mapv #(msh (epoch-ms->dtm %)) epochs)
                  idle-cap-ms (* 1000 idle-cap-s)
                  {:keys [plan]} (player/plan events {:rate rate :idle-cap-ms idle-cap-ms})
                  expected (mapv (fn [i]
                                    (if (zero? i)
                                      0
                                      (min idle-cap-ms
                                           (long (/ (- (nth epochs i) (nth epochs (dec i))) rate)))))
                                  (range (count epochs)))]
              (= expected (mapv first plan)))))]
    (is (:pass? check-result) (str check-result))))

;; ---- ADR-0100: event-timestamp-ms (the sim event-log adapter's own
;; :timestamp-fn) and plan's own injectable :timestamp-fn seam. The
;; default (no :timestamp-fn given) stays message-timestamp-ms,
;; byte-identical -- every test above, unmodified and green, is that
;; witness; these tests exercise the seam itself, hermetically, on
;; synthetic ground-truth-shaped events, never real messages. ----

(deftest event-timestamp-ms-scales-t-seconds-to-ms-test
  (is (= 12000 (player/event-timestamp-ms {:event :admission :t 12}))))

(deftest event-timestamp-ms-zero-t-is-zero-ms-test
  (is (= 0 (player/event-timestamp-ms {:event :registered :t 0}))))

(deftest event-timestamp-ms-nil-for-missing-or-non-numeric-t-test
  (is (nil? (player/event-timestamp-ms {:event :admission})))
  (is (nil? (player/event-timestamp-ms {:event :admission :t nil})))
  (is (nil? (player/event-timestamp-ms {:event :admission :t "12"}))))

(deftest plan-with-event-timestamp-fn-paces-by-t-seconds-test
  (let [events [{:event :registered :t 0} {:event :admission :t 10} {:event :discharge :t 70}]
        {:keys [plan clamped-count unparseable-count skip-count]}
        (player/plan events {:rate 1 :idle-cap-ms 1000000 :timestamp-fn player/event-timestamp-ms})]
    (is (= [0 10000 60000] (mapv first plan)))
    (is (= events (mapv second plan)))
    (is (= 0 clamped-count unparseable-count skip-count))))

(deftest plan-with-event-timestamp-fn-counts-unparseable-and-clamped-test
  (let [events [{:event :registered :t 0} {:event :missing-t} {:event :discharge :t -5}]
        {:keys [plan clamped-count unparseable-count]}
        (player/plan events {:rate 1 :idle-cap-ms 1000000 :timestamp-fn player/event-timestamp-ms})]
    (is (= [0 0 0] (mapv first plan)) "a missing :t paces at zero delta, same as an unparseable MSH-7")
    (is (= 1 unparseable-count))
    (is (= 1 clamped-count) "t -5 is a negative delta against the missing-t event's own effective (inherited) timestamp 0")))

(deftest plan-default-timestamp-fn-is-message-timestamp-ms-test
  (let [events [(msh "20260101000000") (msh "20260101000010")]]
    (is (= (player/plan events {:rate 1})
           (player/plan events {:rate 1 :timestamp-fn player/message-timestamp-ms})))))
