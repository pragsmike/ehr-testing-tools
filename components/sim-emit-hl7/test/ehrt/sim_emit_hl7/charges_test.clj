(ns ehrt.sim-emit-hl7.charges-test
  "ARC 4 SWEEP 2 (`notes/adr/0175-arc-4-emission-add-ons.md` design (c),
  ruling B1, landed DARK): DFT^P03 charges.

  THE PRICE TABLE IS EMISSION CONFIG AND THE ENGINE NEVER READS IT.
  That is the condition ADR-0175 section 2(c) makes its EMISSION
  classification conditional on, so it is the first thing asserted
  here: an unpriced code produces a COUNTED SKIP, never a fallback
  price and never a read-back into the log for something else to bill.

  The log is hand-built for the same reason
  `ehrt.sim-emit-hl7.chatter-test`'s is, and one case in it exists
  nowhere else in this tree at unit scale: an encounter closed TWICE,
  by a `:discharge` that a `:cancel-discharge` undoes and a later
  `:discharge` that closes it for good. `ehrt.sim.charges-run-test`
  carries the population-scale witness table."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [ehrt.sim-emit-hl7.emit-hl7 :as emit-hl7]
            [ehrt.sim-model.interface :as sim-model]))

(def ^:private ref-date "2024-01-01")
(def ^:private utc-offset "+00:00")

(def ^:private persona-0
  {:name {:family "Ferreira" :given "Luis"}
   :dob "1971-09-02"
   :sex :male
   :address {:street "8 Harbor Rd" :city "Tacoma" :state "WA" :zip "98402"}
   :phone "253-555-0180"
   :payer {:id "medicare" :name "Medicare" :type :medicare}})

(def ^:private subject "PID-000000-feedface")

(defn- ev [m]
  (merge {:participants [{:patient-id subject :role :subject}] :warm-up false} m))

(def ^:private bed {:ward "Renal" :bed "RENAL-01" :placement :licensed})

(def ^:private log
  "One patient, three encounters:

    ENC-1  inpatient, t 1000 -> 91000 (two started days), one order and
           one procedure inside it;
    ENC-2  inpatient, closed TWICE -- discharged at 200000, reinstated
           by a `:cancel-discharge`, re-discharged at 300000;
    ENC-3  an OUTPATIENT visit, which occupies no bed and therefore
           earns no room-and-board line at all."
  [(ev {:event :registered :t 0 :active-mrn "MRN000001" :persona persona-0})
   (ev {:event :admission :t 1000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location bed :attending "1234567893" :home-ward "Renal"
        :reason "Acute kidney injury" :forced false})
   (ev {:event :order-placed :t 2000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :attending "1234567893" :location bed :profile :cbc
        :concept {:system :loinc :code "58410-2" :display "CBC panel"}})
   (ev {:event :procedure :t 3000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :codes [{:system :snomed :code "10509002" :display "Dialysis"}]})
   (ev {:event :order-placed :t 4000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :attending "1234567893" :location bed :profile :bmp
        :concept {:system :loinc :code "24323-8" :display "Comprehensive metabolic panel"}})
   (ev {:event :discharge :t 91000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location bed :attending "1234567893"})
   (ev {:event :admission :t 100000 :active-mrn "MRN000001" :encounter-id "ENC-2"
        :location bed :attending "1234567893" :home-ward "Renal"
        :reason "Acute kidney injury" :forced false})
   (ev {:event :discharge :t 200000 :active-mrn "MRN000001" :encounter-id "ENC-2"
        :location bed :attending "1234567893"})
   (ev {:event :cancel-discharge :t 200000 :active-mrn "MRN000001" :encounter-id "ENC-2"
        :location bed :attending "1234567893" :home-ward "Renal" :cancels-event-id 7})
   (ev {:event :order-placed :t 250000 :active-mrn "MRN000001" :encounter-id "ENC-2"
        :attending "1234567893" :location bed :profile :cbc
        :concept {:system :loinc :code "58410-2" :display "CBC panel"}})
   (ev {:event :discharge :t 300000 :active-mrn "MRN000001" :encounter-id "ENC-2"
        :location bed :attending "1234567893"})
   (ev {:event :outpatient-visit :t 400000 :active-mrn "MRN000001" :encounter-id "ENC-3"
        :attending "1234567893"})
   (ev {:event :order-placed :t 401000 :active-mrn "MRN000001" :encounter-id "ENC-3"
        :attending "1234567893" :location bed :profile :cbc
        :concept {:system :loinc :code "58410-2" :display "CBC panel"}})
   (ev {:event :outpatient-visit-end :t 402000 :active-mrn "MRN000001" :encounter-id "ENC-3"
        :attending "1234567893"})])

(def ^:private facility sim-model/default-facility)
(def ^:private providers [{:id "1234567893" :name {:family "Reyes" :given "Priya"}}])

(def ^:private price-table
  "`24323-8` is DELIBERATELY ABSENT -- it is the unpriced code every
  skip assertion below is about."
  {"58410-2" {:amount 148.00 :display "CBC panel"}
   "10509002" {:amount 962.50 :display "Haemodialysis session"}
   emit-hl7/room-and-board-code {:amount 1875.00 :display "Room and board, per day"}})

(def ^:private charges {:price-table price-table})

(defn- plan [] (emit-hl7/plan-charges log charges))

(defn- wire
  ([] (wire charges))
  ([c] (emit-hl7/emit-wire log ref-date utc-offset facility providers nil {}
                           {:charges (:lines (emit-hl7/plan-charges log c))})))

(defn- msh-9 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 8))
(defn- msh-10 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 9))
(defn- segments [m] (mapv #(first (str/split % #"\|")) (str/split m #"\r")))
(defn- ft1s [m] (filterv #(str/starts-with? % "FT1|") (str/split m #"\r")))
(defn- field [seg n] (let [fs (str/split seg #"\|" -1)] (if (< n (count fs)) (nth fs n) "")))
(defn- dfts [ms] (filterv #(= "DFT^P03" (msh-9 %)) ms))

;; --- The identity half ---------------------------------------------------

(deftest absent-nil-and-empty-charges-are-the-byte-identical-path
  (let [plain (emit-hl7/emit log ref-date utc-offset facility providers)]
    (is (= plain (emit-hl7/emit-wire log ref-date utc-offset facility providers nil {})))
    (is (= plain (emit-hl7/emit-wire log ref-date utc-offset facility providers nil {} {:charges nil})))
    (is (= plain (emit-hl7/emit-wire log ref-date utc-offset facility providers nil {} {:charges {}})))
    (testing "NOT CONFIGURED and CONFIGURED-BUT-EMPTY are different
              things, deliberately. Absent `:charges` plans nothing and
              skips nothing -- there was no table to miss. A table that
              is present and empty skips EVERY candidate, and says so,
              because that is a misconfiguration a reader should be
              able to see rather than infer from silence."
      (is (= {:lines {} :skipped {}} (emit-hl7/plan-charges log nil)))
      (is (= {} (:lines (emit-hl7/plan-charges log {}))))
      (is (pos? (reduce + (vals (:skipped (emit-hl7/plan-charges log {})))))))
    (testing "a table that prices NOTHING renders no DFT at all, and
              counts every candidate as a skip"
      (let [{:keys [lines skipped]} (emit-hl7/plan-charges log {:price-table {}})]
        (is (= {} lines))
        (is (pos? (reduce + (vals skipped))))
        (is (= plain (wire {:price-table {}})))))))

;; --- One DFT per encounter close ----------------------------------------

(deftest one-dft-per-encounter-close-in-the-dft-p03-segment-order
  (let [messages (wire)
        d (dfts messages)]
    (is (= 4 (count d))
        "ENC-1 closes once, ENC-2 closes TWICE (reinstated), ENC-3's
         outpatient close has a priced order -- four closes, four DFTs")
    (doseq [m d]
      (is (= ["MSH" "EVN" "PID" "PV1"] (take 4 (segments m))))
      (is (every? #{"FT1"} (drop 4 (segments m)))
          "DFT_P03's own order is [MSH EVN PID PD1 ROL PV1 ... FINANCIAL],
           and FINANCIAL leads on FT1"))
    (testing "the DFT rides its own close's instant and follows the ADT
              that close already renders"
      (let [i (first (keep-indexed #(when (= "DFT^P03" (msh-9 %2)) %1) messages))]
        (is (= "ADT^A03" (msh-9 (nth messages (dec i)))))
        (is (= "MRN000001-A03-91000" (msh-10 (nth messages (dec i)))))
        (is (= "MRN000001-P03-91000" (msh-10 (nth messages i))))))
    (testing "an :outpatient-visit-end renders NO ADT -- its registry
              silence stands -- and the DFT is the only message it
              produces"
      (let [i (first (keep-indexed #(when (= "MRN000001-P03-402000" (msh-10 %2)) %1) messages))]
        (is (some? i))
        (is (not= "ADT" (subs (msh-9 (nth messages (dec i))) 0 3)))))))

(deftest the-three-line-types-and-their-ft1-fields
  (let [m (first (dfts (wire)))
        lines (ft1s m)]
    (testing "ENC-1: two started days of room and board, one priced
              order, one priced procedure -- and the unpriced order is
              simply not there"
      (is (= 4 (count lines)))
      (is (= ["1" "2" "3" "4"] (mapv #(field % 1) lines)) "FT1-1 set ids"))
    (let [by-code (into {} (map (juxt #(first (str/split (field % 7) #"\^")) identity)) lines)]
      (testing "FT1-7 carries the transaction code on EVERY line"
        (is (= #{"58410-2" "10509002" emit-hl7/room-and-board-code} (set (keys by-code)))))
      (testing "FT1-6 is the transaction type, FT1-10/11/12 the quantity
                and the extended and unit amounts"
        (let [order (get by-code "58410-2")]
          (is (= "CG" (field order 6)))
          (is (= "1" (field order 10)))
          (is (= "148.00" (field order 11)))
          (is (= "148.00" (field order 12)))))
      (testing "FT1-25 additionally names the PROCEDURE code, and only a
                procedure line carries it"
        (is (= "10509002" (first (str/split (field (get by-code "10509002") 25) #"\^"))))
        (is (= "" (field (get by-code "58410-2") 25)))
        (is (= "" (field (get by-code emit-hl7/room-and-board-code) 25))))
      (testing "FT1-4 is the fact's own CLINICAL instant, never the
                DFT's transmit instant -- ADR-0109's split clock"
        (is (= "20240101003320+0000" (field (get by-code "58410-2") 4))
            "the order at t=2000")))))

(deftest room-and-board-is-one-line-per-started-inpatient-day-and-outpatients-get-none
  (let [{:keys [lines]} (plan)
        rb (fn [k] (filterv #(= emit-hl7/room-and-board-code (:code %)) (get lines k)))]
    (testing "ENC-1 runs t 1000 -> 91000, which is 90,000 seconds and
              two started days, and the lines sit on the day grid from
              the admission instant"
      (is (= 2 (count (rb ["ENC-1" 91000]))))
      (is (= [1000 87400] (mapv :at (rb ["ENC-1" 91000])))))
    (testing "an OUTPATIENT encounter occupies no bed, so it earns no
              room-and-board line -- only its own priced order"
      (is (= [] (rb ["ENC-3" 402000])))
      (is (= 1 (count (get lines ["ENC-3" 402000])))))))

(deftest an-encounter-closed-twice-bills-each-close-for-what-had-happened-by-then
  (testing "ADR-0175 section 2(c) says `one DFT per encounter close`,
            and `ehrt.sim-engine.engine/stamp-encounter` says a
            reinstated stay is ONE encounter closed twice. Keying the
            plan by encounter ALONE would have billed the first close
            for bed-days it had not yet incurred; the key is
            (encounter, closer instant) for exactly this case."
    (let [{:keys [lines]} (plan)
          first-close (get lines ["ENC-2" 200000])
          second-close (get lines ["ENC-2" 300000])]
      (is (= 2 (count first-close)) "two started days by t=200000, no order yet")
      (is (= 4 (count second-close)) "three started days by t=300000, plus the order at t=250000")
      (is (every? #(<= (:at %) 200000) first-close))
      (is (some #(= "58410-2" (:code %)) second-close))
      (is (not-any? #(= "58410-2" (:code %)) first-close)))))

;; --- The skip census: the condition the EMISSION classification rests on -

(deftest an-unpriced-code-is-a-counted-skip-never-a-fallback-price
  (let [{:keys [lines skipped]} (plan)
        all-lines (mapcat val lines)]
    (is (= {"24323-8" 1} skipped)
        "the one unpriced code, counted once -- and nothing else was
         quietly dropped")
    (is (not-any? #(= "24323-8" (:code %)) all-lines))
    (is (every? #(contains? price-table (:code %)) all-lines)
        "every rendered line's amount came from the table and nowhere else")
    (testing "and the DFT for that encounter is SHORT rather than
              carrying an invented price"
      (is (= 4 (count (ft1s (first (dfts (wire))))))))
    (testing "dropping a price from the table moves the skip census, not
              the amounts of what remains"
      (let [{:keys [skipped]} (emit-hl7/plan-charges log {:price-table (dissoc price-table "58410-2")})]
        (is (= {"24323-8" 1 "58410-2" 3} skipped))))))

(deftest amounts-render-locale-free-at-two-decimal-places
  (testing "`String/format`'s `%.2f` reads the DEFAULT LOCALE for its
            decimal separator, so a host configured for de-DE would
            render `1875,00` and the corpus would stop being a function
            of its own inputs. BigDecimal at scale 2 is what is used
            instead, and this is the gate that says so."
    (let [line (first (filter #(str/includes? % "ROOM-BOARD") (ft1s (first (dfts (wire))))))]
      (is (= "1875.00" (field line 11)))
      (is (= "1875.00" (field line 12))))
    (let [default java.util.Locale/GERMANY
          previous (java.util.Locale/getDefault)]
      (try
        (java.util.Locale/setDefault default)
        (is (str/includes? (first (dfts (wire))) "|1875.00|1875.00"))
        (finally (java.util.Locale/setDefault previous))))))

;; --- Determinism: no RNG anywhere in the charge path --------------------

(deftest plan-charges-takes-no-rng-and-is-a-pure-function-of-log-and-table
  (testing "ADR-0175 section 2(c)'s own rejected option (3): a price
            that changes per run is not a price"
    (is (= (plan) (plan) (plan)))
    (is (= (wire) (wire)))))
