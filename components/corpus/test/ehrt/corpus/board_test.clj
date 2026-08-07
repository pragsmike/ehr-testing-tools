(ns ehrt.corpus.board-test
  "Red-first spec for the bed board (player board, AR-BB2-4, ADR-0067):
  `render-snapshot` against hand-built accumulators -- the SAME entry
  shape `ehrt.sim-emit-hl7.v2-replay/fold-message` produces, built
  directly here rather than folded through real messages, so each
  test pins exactly one rendering concern. `fold-event` is exercised
  separately, directly against the exported `fold-message` (AR-BB2-1's
  own caller-grep subject, corpus's first real external call into
  `ehrt.sim-emit-hl7.interface`)."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.corpus.board :as board]))

;; ---- render-snapshot: a hand-built accumulator, no real fold ----

(deftest render-snapshot-empty-accumulator-has-header-and-zero-tally-test
  (let [rendered (board/render-snapshot {} 1786060800000)]
    (is (str/includes? rendered "2026-08-07T00:00:00Z")
        "the snapshot instant renders as ISO-8601 UTC")
    (is (str/includes? rendered "inpatients: 0"))
    (is (str/includes? rendered "active outpatients: 0"))
    (is (str/includes? rendered "discharged: 0"))
    (is (str/includes? rendered "merged: 0"))))

(deftest render-snapshot-one-ward-lists-its-occupied-beds-test
  (let [acc {"445566" {:active-mrn "445566"
                        :persona {:name {:family "Doe" :given "Jane"}}
                        :status :admitted :class :inpatient
                        :location {:ward "3W" :bed "A"}
                        :attending "1234"}
             "778899" {:active-mrn "778899"
                       :persona {:name {:family "Smith" :given "Ann"}}
                       :status :admitted :class :inpatient
                       :location {:ward "3W" :bed "B"}}}
        rendered (board/render-snapshot acc 1786060800000)]
    (is (str/includes? rendered "3W"))
    (is (str/includes? rendered "A"))
    (is (str/includes? rendered "Doe"))
    (is (str/includes? rendered "445566"))
    (is (str/includes? rendered "attending: 1234"))
    (is (str/includes? rendered "B"))
    (is (str/includes? rendered "Smith"))
    (is (not (str/includes? rendered "attending:  ")) "no attending for the second patient -- rendered without a dangling label")
    (is (str/includes? rendered "inpatients: 2"))
    ;; ward-sorted, bed-sorted-within: A's own line precedes B's own line
    (is (< (str/index-of rendered "Doe") (str/index-of rendered "Smith")))))

(deftest render-snapshot-multi-ward-outpatient-discharge-merged-tombstone-test
  (let [acc {"111" {:active-mrn "111" :persona {:name {:family "Alpha" :given "A"}}
                     :status :admitted :class :inpatient :location {:ward "1E" :bed "1"} :attending "9001"}
             "222" {:active-mrn "222" :persona {:name {:family "Beta" :given "B"}}
                    :status :admitted :class :inpatient :location {:ward "2W" :bed "5"}}
             "333" {:active-mrn "333" :persona {:name {:family "Gamma" :given "C"}}
                    :status :admitted :class :outpatient :attending "9002"}
             "444" {:active-mrn "444" :persona {:name {:family "Delta" :given "D"}}
                    :status :discharged :location nil}
             ;; fold-merge absorbs into the survivor but never clears
             ;; the merged-away entry's own stale :location -- a real
             ;; tombstone can still carry one (live-probe-caught).
             "555" {:active-mrn "555" :status :merged :class :inpatient
                    :location {:ward "1E" :bed "9"}}}
        rendered (board/render-snapshot acc 1786060800000)]
    (testing "wards sorted, both wards present"
      (is (< (str/index-of rendered "1E") (str/index-of rendered "2W"))))
    (testing "the outpatient, the discharge, and the tombstone never appear as occupying a bed"
      (is (not (str/includes? rendered "Gamma")))
      (is (not (str/includes? rendered "Delta")))
      (is (not (str/includes? rendered "555"))))
    (testing "the tally counts every class exactly once, mutually exclusive"
      (is (str/includes? rendered "inpatients: 2"))
      (is (str/includes? rendered "active outpatients: 1"))
      (is (str/includes? rendered "discharged: 1"))
      (is (str/includes? rendered "merged: 1")))))

;; ---- fold-event: the real exported fold-message, wrapped ----

(defn- msh
  "A synthetic ER7 message with exactly the fields v2-replay's own
  readers touch -- PV1-7 (attending) is real HL7 field 7, so PV1-3
  (location) is followed by FOUR pipes (three empty fields: PV1-4/5/6,
  never populated by this fixture) to land attending at the right
  split index, matched against the parser directly, not assumed."
  [dtm trigger mrn family given & {:keys [class ward bed attending mrg]}]
  (str "MSH|^~\\&|A|B|C|D|" dtm "||ADT^" trigger "^ADT_" trigger "|MSG|P|2.4\r"
       "PID|1||" mrn "||" family "^" given "||19800101\r"
       (when (or class ward attending)
         (str "PV1|1|" (or class "") "|" (or ward "") "^^" (or bed "") "||||" (or attending "") "\r"))
       (when mrg (str "MRG|" mrg "\r"))))

(deftest fold-event-folds-a-supported-trigger-through-the-real-accumulator-test
  (let [message (msh "20260807120000" "A01" "445566" "Doe" "Jane"
                      :class "I" :ward "3W" :bed "A" :attending "1234")
        {:keys [acc unfolded?]} (board/fold-event {} message)]
    (is (false? unfolded?))
    (is (= {:ward "3W" :bed "A"} (:location (get acc "445566"))))
    (is (= :inpatient (:class (get acc "445566"))))))

(deftest fold-event-a-foreign-trigger-is-a-counted-skip-not-a-crash-test
  (let [message (msh "20260807120000" "A08" "445566" "Doe" "Jane")
        {:keys [acc unfolded?]} (board/fold-event {"already" {:status :admitted}} message)]
    (is (true? unfolded?))
    (is (= {"already" {:status :admitted}} acc) "acc returned unchanged on an unfolded skip")))
