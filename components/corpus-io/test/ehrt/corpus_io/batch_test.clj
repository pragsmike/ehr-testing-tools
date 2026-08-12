(ns ehrt.corpus-io.batch-test
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as kernel]
            [ehrt.corpus-io.batch :as batch]))

(defn- msh
  "A minimal synthetic message carrying just MSH-7 (dtm) -- the same
  shape ehrt.corpus.player-test's own `msh` helper builds, independent
  of it (corpus-io may not require ehrt.corpus.*)."
  [dtm]
  (str "MSH|^~\\&|A|B|C|D|" dtm))

(defn- tagged
  [dtm source]
  {:message (msh dtm) :source source})

(def ^:private hour-ms (* 60 60 1000))

;; ---- epoch alignment, half-open intervals ----

(deftest bucket-spans-are-epoch-aligned-half-open-test
  (let [;; 1970-01-01T00:59:59.000Z (just inside bucket 0) and
        ;; 1970-01-01T01:00:00.000Z (exactly the bucket-1 boundary --
        ;; start-inclusive, so it belongs to bucket 1, not bucket 0).
        r (batch/partition-messages
           [(tagged "19700101005959" "a.hl7") (tagged "19700101010000" "b.hl7")]
           {:interval-ms hour-ms})]
    (is (kernel/ok? r))
    (let [buckets (:buckets (:payload r))]
      (is (= 2 (count buckets)))
      (is (= [0 1] (mapv :bucket-index buckets)))
      (is (= [0 hour-ms] (mapv :start-ms buckets)))
      (is (= [hour-ms (* 2 hour-ms)] (mapv :end-ms buckets)))
      (is (= 1 (count (:messages (first buckets)))))
      (is (= 1 (count (:messages (second buckets))))))))

(deftest bucket-end-boundary-message-lands-in-later-bucket-test
  (testing "a message exactly at k*interval belongs to bucket k, never bucket k-1"
    (let [r (batch/partition-messages [(tagged "19700101020000" "x.hl7")]
                                       {:interval-ms hour-ms})
          buckets (:buckets (:payload r))]
      (is (= [2] (mapv :bucket-index buckets))))))

;; ---- empty buckets are skipped, v1 ----

(deftest empty-buckets-are-skipped-test
  (let [;; bucket 0 and bucket 2 occupied; bucket 1 (the hour in between)
        ;; carries no message and must be entirely absent, not present
        ;; with an empty :messages vector.
        r (batch/partition-messages
           [(tagged "19700101000000" "a.hl7") (tagged "19700101020000" "b.hl7")]
           {:interval-ms hour-ms})
        buckets (:buckets (:payload r))]
    (is (= [0 2] (mapv :bucket-index buckets)))))

;; ---- global sort: cross-file MSH-7 ordering, never trusting input/
;; file order (the wire's own transmit order is the batch order) ----

(deftest cross-file-messages-sort-by-msh-7-globally-test
  (let [;; Deliberately out of chronological order, and interleaved
        ;; across two distinct "files" (sources) -- the wire's own
        ;; transmit order is the batch order, never file/input order.
        r (batch/partition-messages
           [(tagged "19700101000030" "b.hl7")
            (tagged "19700101000010" "a.hl7")
            (tagged "19700101000020" "b.hl7")
            (tagged "19700101000000" "a.hl7")]
           {:interval-ms hour-ms})
        buckets (:buckets (:payload r))
        ordered-sources (mapv :source (:messages (first buckets)))
        ordered-ts (mapv :ts-ms (:messages (first buckets)))]
    (is (= 1 (count buckets)))
    (is (= 4 (count ordered-sources)))
    (is (= ordered-ts (sort ordered-ts)) "ascending MSH-7, globally, not per-source")
    (is (= ["a.hl7" "a.hl7" "b.hl7" "b.hl7"] ordered-sources)
        (str "chronological order (000000/000010/000020/000030) interleaves the two "
             "sources exactly, regardless of the input list's own order"))))

;; ---- fail-fast on an unparseable MSH-7 -- categorized, names the
;; offending :source, never a silent skip (the corpus is presumed
;; foreign-but-valid) ----

(deftest unparseable-msh-7-is-a-categorized-error-naming-the-source-test
  (let [r (batch/partition-messages
           [(tagged "19700101000000" "good.hl7")
            {:message "MSH|^~\\&|A|B|C|D|not-a-date" :source "bad.hl7"}]
           {:interval-ms hour-ms})]
    (is (kernel/error? r))
    (is (= :unparseable-transmit-time (:category r)))
    (is (= "bad.hl7" (:source (:payload r))))))

(deftest unparseable-msh-7-fails-fast-before-sorting-test
  (testing "the first offending message in INPUT order is named, even when a
            later, valid-timestamp message would otherwise sort earlier"
    (let [r (batch/partition-messages
             [{:message "MSH|^~\\&|A|B|C|D|garbage" :source "first.hl7"}
              (tagged "19700101000000" "second.hl7")]
             {:interval-ms hour-ms})]
      (is (kernel/error? r))
      (is (= "first.hl7" (:source (:payload r)))))))

;; ---- no messages at all -- an empty, valid result, never an error ----

(deftest no-messages-yields-no-buckets-test
  (let [r (batch/partition-messages [] {:interval-ms hour-ms})]
    (is (kernel/ok? r))
    (is (= [] (:buckets (:payload r))))))
