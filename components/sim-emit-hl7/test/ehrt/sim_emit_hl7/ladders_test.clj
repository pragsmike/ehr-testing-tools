(ns ehrt.sim-emit-hl7.ladders-test
  "ARC 4 SWEEP 3 (`notes/adr/0175-arc-4-emission-add-ons.md` design (b),
  ruling B1, landed DARK): order/result STATUS LADDERS -- ORM^O01
  restatements carrying ORC-5, ORU^R01 restatements carrying OBR-25 and
  OBX-11, at fixed fractions of an order's own
  `:order-placed` -> `:result-available` interval.

  THE LOG IS HAND-BUILT, for `ehrt.sim-emit-hl7.chatter-test`'s reason
  and one more of its own. `components/sim-emit-hl7` may not depend on
  `components/sim`, so no `run-command` is reachable from this brick;
  and a hand-built log can put the three cases no seed reliably
  produces under one assertion -- two orders placed at the SAME INSTANT
  for one patient (the control-id collision the ordinal exists for), a
  ZERO-LENGTH interval (an order and its result at one second, which
  must ladder not at all), and an unsolicited `:observation` beside
  them (which has no order and must therefore never grow a rung). The
  population-scale half is `ehrt.sim.ladders-run-test`.

  NOTHING HERE DRAWS. `plan-ladders` takes no RNG at all -- see its own
  docstring -- so unlike the chatter tests there is no draw-consumption
  law to assert and no seed to thread. What replaces it is a purity
  assertion and the byte-equality of every non-ladder message."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [ehrt.sim-emit-hl7.emit :as emit]
            [ehrt.sim-emit-hl7.planners :as planners]
            [ehrt.sim-emit-hl7.registry :as registry]
            [ehrt.sim-emit-hl7.v2-replay :as v2-replay]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.util Random]))

(def ^:private ref-date "2024-01-01")
(def ^:private utc-offset "+00:00")

(def ^:private persona-0
  {:name {:family "Okonkwo" :given "Ada"}
   :dob "1974-11-02"
   :sex :female
   :address {:street "9 Birch Ln" :city "Reno" :state "NV" :zip "89501"}
   :phone "775-555-0117"
   :payer {:id "medicare" :name "Medicare" :type :medicare}})

(def ^:private subject "PID-000000-0badcafe")

(defn- ev [m]
  (merge {:participants [{:patient-id subject :role :subject}] :warm-up false} m))

(def ^:private ward-location {:ward "Renal" :bed "RN-H02" :placement :surge})

(def ^:private cbc-concept
  {:system :loinc :code "58410-2" :display "CBC panel - Blood by Automated count"})
(def ^:private bmp-concept
  {:system :loinc :code "24323-8" :display "Basic metabolic panel - Serum or Plasma"})

(def ^:private cbc-results
  [{:concept {:system :loinc :code "6690-2" :display "Leukocytes"}
    :unit "K/uL" :value 2.0 :reference-range {:low 4.5 :high 11.0} :abnormal-flag :low}
   {:concept {:system :loinc :code "789-8" :display "Erythrocytes"}
    :unit "M/uL" :value 4.9 :reference-range {:low 4.2 :high 5.9} :abnormal-flag :normal}])

(def ^:private bmp-results
  [{:concept {:system :loinc :code "2345-7" :display "Glucose"}
    :unit "mg/dL" :value 142.0 :reference-range {:low 70.0 :high 99.0} :abnormal-flag :high}])

(def ^:private log
  "One patient, one encounter, THREE orders. Indices matter and are
  named here because `:order-event-id` is a log index:

    0 :registered
    1 :admission
    2 :order-placed  CBC  t=1200
    3 :order-placed  BMP  t=2200
    4 :result-available for 2, t=5200
    5 :result-available for 3, t=6200
    6 :observation         t=6500  -- unsolicited, no order, no ladder
    7 :order-placed  CBC  t=7000
    8 :result-available for 7, t=7000  -- ZERO-LENGTH interval
    9 :discharge          t=8000

  THE LOG IS `:t`-NONDECREASING, as every real log is (the engine's own
  priority-queue invariant), and that is load-bearing rather than
  cosmetic: `emit-wire`'s identity property -- absent offsets reproduce
  `emit`'s exact order and therefore its exact bytes -- rests on the
  stable tie-break over a sorted log. An earlier draft of this fixture
  put the `:observation` between two results at 6000 and 6200 and broke
  the byte-identity assertions for that reason alone.

  THE TWO INTERVALS ARE OFFSET BY EXACTLY 1000 SECONDS, and both are
  4000 long, so at `:rungs [0.25 0.5]` order 2's SECOND rung and order
  3's FIRST land on the same instant, 3200, for one MRN under one
  trigger -- the collision the control-id ordinal exists for.

  THE OFFSET IS DELIBERATE RATHER THAN INCIDENTAL. The obvious way to
  force that collision is to give both orders the same interval
  outright; that also gives their two RESULTS one instant, and
  `control-id-for` mints `mrn-R01-t` for a result, so the two terminal
  messages would collide in MSH-10 before any ladder existed. That
  collision is real and PRE-EXISTING -- it is the same shape
  `:bed-status-change`'s own arm of `control-id-for` was widened to fix
  -- and this sweep neither introduces nor repairs it. Offsetting the
  intervals keeps this namespace testing the ladder rather than
  re-discovering a defect it is not fixing."
  [(ev {:event :registered :t 0 :active-mrn "MRN000001" :persona persona-0})
   (ev {:event :admission :t 1000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893" :home-ward "Renal"
        :reason "Acute kidney injury" :forced false})
   (ev {:event :order-placed :t 1200 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893"
        :profile :cbc :concept cbc-concept})
   (ev {:event :order-placed :t 2200 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893"
        :profile :bmp :concept bmp-concept})
   (ev {:event :result-available :t 5200 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893"
        :order-event-id 2 :profile :cbc :concept cbc-concept :results cbc-results})
   (ev {:event :result-available :t 6200 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893"
        :order-event-id 3 :profile :bmp :concept bmp-concept :results bmp-results})
   (ev {:event :observation :t 6500 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893"
        :codes [{:system :loinc :code "8867-4" :display "Heart rate"}]
        :value 88.0 :unit "/min" :category "vital-signs"})
   (ev {:event :order-placed :t 7000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893"
        :profile :cbc :concept cbc-concept})
   (ev {:event :result-available :t 7000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893"
        :order-event-id 7 :profile :cbc :concept cbc-concept :results cbc-results})
   (ev {:event :discharge :t 8000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893"})])

(def ^:private facility sim-model/default-facility)
(def ^:private providers [{:id "1234567893" :name {:family "Reyes" :given "Priya"}}])

(def ^:private both-ladders {:rungs [0.25 0.5] :order-rungs [0.1 0.2]})

(defn- plan [ladders] (planners/plan-ladders log ladders))

(defn- wire
  ([ladders] (wire ladders {} nil))
  ([ladders offsets site-profile]
   (emit/emit-wire log ref-date utc-offset facility providers site-profile offsets
                       {:ladders (plan ladders)})))

(defn- msh-9 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 8))
(defn- msh-10 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 9))
(defn- msh-7 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 6))
(defn- segment [m nm] (first (filter #(str/starts-with? % (str nm "|")) (str/split m #"\r"))))
(defn- segments-named [m nm] (filterv #(str/starts-with? % (str nm "|")) (str/split m #"\r")))
(defn- field
  "STANDARD 1-based HL7 field index, on a non-MSH segment (where the
  segment id occupies position 0 and field N is at split index N)."
  [seg n]
  (let [parts (str/split seg #"\|" -1)]
    (nth parts n "")))
(defn- rung? [m] (= 4 (count (str/split (msh-10 m) #"-"))))

;; --- The identity half: absent, nil and {} ladders render exactly what
;; this emitter rendered before design (b) existed. -------------------------

(deftest absent-nil-and-empty-ladders-are-the-byte-identical-path
  (let [plain (emit/emit log ref-date utc-offset facility providers)]
    (is (= plain (emit/emit-wire log ref-date utc-offset facility providers nil {})))
    (is (= plain (emit/emit-wire log ref-date utc-offset facility providers nil {} {})))
    (is (= plain (emit/emit-wire log ref-date utc-offset facility providers nil {}
                                     {:ladders nil})))
    (is (= plain (wire nil)))
    (is (= plain (wire {})))
    (testing "a profile naming no rung plans nothing and renders nothing new"
      (is (= {:rungs [] :final #{}} (plan {})))
      (is (= {:rungs [] :final #{}} (plan nil)))
      (is (= plain (wire {:rungs [] :order-rungs []}))))))

;; --- ADR-0175 section 2(b)'s shape: rungs at k/(r+1)-style fractions of
;; the order->result interval, each restating the SAME ORC/OBR ------------

(deftest a-rung-lands-at-its-fraction-of-the-order-to-result-interval
  (let [{:keys [rungs]} (plan both-ladders)
        for-order (fn [i fam] (filterv #(and (= i (:order-index %)) (= fam (:family %))) rungs))]
    (testing "the population is not empty -- rulings.md#R-empty-population-is-red"
      (is (= 8 (count rungs))
          "two ladders x two fractions x two laddered orders; the third order's
           interval is zero-length and ladders not at all"))
    (testing "ORU rungs of order 2: t0=1200, t1=5200, so 0.25 -> 2200 and 0.5 -> 3200"
      (is (= [2200 3200] (mapv :at (for-order 2 :oru)))))
    (testing "ORM rungs of order 2: 0.1 -> 1600 and 0.2 -> 2000"
      (is (= [1600 2000] (mapv :at (for-order 2 :orm)))))
    (testing "and order 3's own ORU rungs, offset by 1000: 3200 and 4200"
      (is (= [3200 4200] (mapv :at (for-order 3 :oru)))))
    (testing "a rung is STRICTLY inside its interval, always"
      (is (every? (fn [{:keys [at order-index result-index]}]
                    (< (long (:t (nth log order-index)))
                       (long at)
                       (long (:t (nth log result-index)))))
                  rungs)))
    (testing "the zero-length interval (order 7 / result 8) produces nothing"
      (is (empty? (filterv #(= 7 (:order-index %)) rungs))))))

(deftest a-rung-restates-the-same-orc-and-obr-the-final-message-carries
  (let [messages (wire both-ladders)
        oru-rungs (filterv #(and (rung? %) (= "ORU^R01" (msh-9 %))) messages)
        orm-rungs (filterv #(and (rung? %) (= "ORM^O01" (msh-9 %))) messages)
        final-cbc (first (filter #(= "MRN000001-R01-5200" (msh-10 %)) messages))]
    (is (seq oru-rungs) "ORU rung population")
    (is (seq orm-rungs) "ORM rung population")
    (is (some? final-cbc) "the terminal CBC result message")
    (testing "an ORU rung's OBR-4 is the final message's OBR-4"
      (let [rung (first (filter #(str/includes? % "58410-2") oru-rungs))]
        (is (= (field (segment final-cbc "OBR") 4)
               (field (segment rung "OBR") 4)))
        (testing "and so is every OBX-3/OBX-5 pair -- a rung invents no value"
          (is (= (mapv #(vector (field % 3) (field % 5)) (segments-named final-cbc "OBX"))
                 (mapv #(vector (field % 3) (field % 5)) (segments-named rung "OBX")))))))
    (testing "an ORM rung's OBR-4 is its order's own"
      (is (some #(= (field (segment % "OBR") 4)
                    (str "58410-2^CBC panel - Blood by Automated count^LN"))
                orm-rungs)))))

;; --- The codes, and where they come from ---------------------------------

(deftest the-ladder-renders-the-three-site-profile-tables
  (let [messages (wire both-ladders)
        oru-rungs (filterv #(and (rung? %) (= "ORU^R01" (msh-9 %))) messages)
        orm-rungs (filterv #(and (rung? %) (= "ORM^O01" (msh-9 %))) messages)
        finals (filterv #(and (not (rung? %)) (= "ORU^R01" (msh-9 %))
                              (some? (segment % "ORC"))) messages)]
    (testing "OBR-25 and OBX-11 carry the PRELIMINARY code on every ORU rung"
      (is (seq oru-rungs))
      (is (every? #(= "P" (field (segment % "OBR") 25)) oru-rungs))
      (is (every? (fn [m] (every? #(= "P" (field % 11)) (segments-named m "OBX"))) oru-rungs)))
    (testing "ORC-5 walks the order ladder and SATURATES at its last stage"
      (is (= ["SC" "IP"]
             (mapv #(field (segment % "ORC") 5)
                   (sort-by msh-10
                            (filterv #(#{"MRN000001-O01-1600-0" "MRN000001-O01-2000-0"}
                                       (msh-10 %))
                                     orm-rungs))))
          "rung 0 is :scheduled, rung 1 :in-progress -- read off the two rungs of ONE order")
      (is (= #{"SC" "IP"} (into #{} (map #(field (segment % "ORC") 5)) orm-rungs))))
    (testing "the two LADDERED finals carry F in both fields; the un-laddered one carries neither"
      (let [laddered (filterv #(#{"MRN000001-R01-5200" "MRN000001-R01-6200"} (msh-10 %)) finals)
            unladdered (filterv #(= "MRN000001-R01-7000" (msh-10 %)) finals)]
        (is (= 2 (count laddered)))
        (is (= 1 (count unladdered)))
        (is (every? #(= "F" (field (segment % "OBR") 25)) laddered))
        (is (every? (fn [m] (every? #(= "F" (field % 11)) (segments-named m "OBX"))) laddered))
        (is (every? #(= "" (field (segment % "OBR") 25)) unladdered)
            "an order that grew no rung renders no terminal status either -- the codes ride
             the LADDER, per order, never the config's mere presence")))))

(deftest every-one-of-the-three-tables-is-overridable
  (let [profile {:code-tables {:order-status {:scheduled {:code "ZZ"} :in-progress {:code "YY"}}
                               :result-status {:preliminary {:code "XX"} :final {:code "WW"}}
                               :observation-result-status {:preliminary {:code "VV"}
                                                           :final {:code "UU"}}}}
        messages (emit/emit-wire log ref-date utc-offset facility providers profile {}
                                     {:ladders (plan both-ladders)})
        oru-rungs (filterv #(and (rung? %) (= "ORU^R01" (msh-9 %))) messages)
        orm-rungs (filterv #(and (rung? %) (= "ORM^O01" (msh-9 %))) messages)
        laddered-finals (filterv #(#{"MRN000001-R01-5200" "MRN000001-R01-6200"} (msh-10 %))
                                 messages)]
    (testing "the site profile wins in all three positions, rungs and terminal alike"
      (is (every? #(= "XX" (field (segment % "OBR") 25)) oru-rungs))
      (is (every? (fn [m] (every? #(= "VV" (field % 11)) (segments-named m "OBX"))) oru-rungs))
      (is (= #{"ZZ" "YY"} (into #{} (map #(field (segment % "ORC") 5)) orm-rungs)))
      (is (every? #(= "WW" (field (segment % "OBR") 25)) laddered-finals))
      (is (every? (fn [m] (every? #(= "UU" (field % 11)) (segments-named m "OBX")))
                  laddered-finals)))
    (testing "and the profile is EMISSION-only: the same plan, unchanged"
      (is (= (plan both-ladders) (plan both-ladders))))))

;; --- What must NOT move --------------------------------------------------

(deftest turning-ladders-on-moves-no-other-message-s-bytes
  (let [plain (emit/emit log ref-date utc-offset facility providers)
        laddered (wire both-ladders)
        ;; The two terminal messages of the two LADDERED orders are the
        ;; one place this sweep edits an existing message's bytes, and
        ;; they are excluded by control-id, named, rather than by a
        ;; filter that would hide a third mover.
        moved-ids #{"MRN000001-R01-5200" "MRN000001-R01-6200"}
        untouched (filterv #(and (not (rung? %)) (not (moved-ids (msh-10 %)))) laddered)]
    (is (= (filterv #(not (moved-ids (msh-10 %))) plain) untouched)
        "every message that is neither a rung nor a laddered order's own terminal result is
         byte-equal AND in the same position")
    (testing "and the two that DID move differ ONLY in OBR-25 and OBX-11"
      (let [before (filterv #(moved-ids (msh-10 %)) plain)
            after (filterv #(moved-ids (msh-10 %)) laddered)
            ;; Field-wise rather than by regex: OBR-8..25 are dropped when
            ;; present and OBX-11 is blanked, so what remains is exactly
            ;; the segment the un-laddered builder renders.
            strip (fn [ms]
                    (mapv (fn [m]
                            (str/join "\r"
                                      (mapv (fn [seg]
                                              (let [f (vec (str/split seg #"\|" -1))]
                                                (cond
                                                  (and (= "OBR" (first f)) (> (count f) 25))
                                                  (str/join "|" (subvec f 0 8))

                                                  (= "OBX" (first f))
                                                  (str/join "|" (assoc f 11 ""))

                                                  :else seg)))
                                            (str/split m #"\r"))))
                          ms))]
        (is (= 2 (count before)) "both laddered results exist before the ladder too")
        (is (not= before after) "they DID move -- this sweep declares that")
        (is (= (strip before) (strip after))
            "and stripping the two status fields puts them back")))))

(deftest an-unsolicited-observation-grows-no-rung
  (let [messages (wire both-ladders)
        observation-orus (filterv #(and (= "ORU^R01" (msh-9 %)) (nil? (segment % "ORC")))
                                  messages)]
    (testing "the population exists -- an assertion of ZERO over an empty set proves nothing"
      (is (= 1 (count observation-orus))
          "the log's one `:observation`, rendered ORC-less exactly as it always was"))
    (is (every? #(not (rung? %)) observation-orus))
    (is (zero? (count (filterv #(= 6 (:order-index %)) (:rungs (plan both-ladders)))))
        "`:observation` has no `:order-event-id`, so it cannot be an order index at all")))

;; --- Identity: the rung's own tuple, and its injectivity ------------------

(deftest the-rung-identity-tuple-is-injective-and-msh-10-carries-it
  (let [{:keys [rungs]} (plan both-ladders)
        tuples (mapv (juxt :active-mrn :trigger :at :ordinal) rungs)]
    (testing "the population is not empty"
      (is (= 8 (count rungs))))
    (is (= (count tuples) (count (set tuples)))
        "(active-mrn, trigger, at, ordinal) is INJECTIVE over the plan -- the four-part key
         sweep 2 measured, not ADR-0175 section 4's three-part triple, which sweep 2 measured
         non-injective and which this sweep must not worsen")
    (is (= (mapv (fn [[mrn trig at ord]] (str mrn "-" trig "-" at "-" ord)) tuples)
           (mapv :control-id rungs))
        "and MSH-10 IS that tuple, rendered")
    (testing "the same-instant collision is real, and the ordinal is what resolves it"
      (let [at-3200 (filterv #(and (= 3200 (:at %)) (= "R01" (:trigger %))) rungs)]
        (is (= 2 (count at-3200))
            "order 2's second rung and order 3's first, one patient, one instant, one trigger")
        (is (= #{2 3} (into #{} (map :order-index) at-3200)))
        (is (= [0 1] (sort (mapv :ordinal at-3200))))))))

(deftest msh-10-is-unique-across-base-chatter-and-ladder-at-a-t-collision
  (let [chatter (planners/plan-chatter (Random. 7) log
                                       {:demographic-update 1.0 :coverage-change 1.0
                                        :registered 1.0
                                        :restatement {:rate-per-patient-day 2.0}})
        messages (emit/emit-wire log ref-date utc-offset facility providers nil {}
                                     {:chatter chatter :ladders (plan both-ladders)})
        ids (mapv msh-10 messages)]
    (testing "all three streams are present -- otherwise this asserts nothing"
      (is (seq (filterv #(#{"ADT^A08" "ADT^A31" "ADT^A28"} (msh-9 %)) messages)) "chatter")
      (is (seq (filterv rung? messages)) "ladder rungs")
      (is (seq (filterv #(and (not (rung? %)) (= "ADT^A01" (msh-9 %))) messages)) "base"))
    (is (= (count ids) (count (set ids)))
        "MSH-10 is unique across every stream this emitter can put on one wire")))

;; --- The clock: a rung never overtakes the message it restates ------------

(deftest a-rung-rides-its-basis-event-s-own-latency-offset
  (let [offsets {"MRN000001-O01-1200" 1800 "MRN000001-R01-5200" 3600}
        messages (wire both-ladders offsets nil)
        transmit (into {} (map (juxt msh-10 msh-7)) messages)
        of-order-2 #{"MRN000001-O01-1600-0" "MRN000001-O01-2000-0"}
        of-result-4 #{"MRN000001-R01-2200-0" "MRN000001-R01-3200-0"}
        orm-rungs (filterv #(of-order-2 (msh-10 %)) messages)
        oru-rungs (filterv #(of-result-4 (msh-10 %)) messages)]
    (testing "the offsets actually bite -- an offset nothing applies proves nothing"
      (is (= "20240101005000+0000" (transmit "MRN000001-O01-1200"))
          "t=1200 is 00:20:00; the order transmits 1800s later, at 00:50:00"))
    (testing "every ORM rung of the offset order transmits at or after that order"
      (is (= 2 (count orm-rungs)))
      (is (every? #(<= (compare (transmit "MRN000001-O01-1200") (msh-7 %)) 0) orm-rungs)))
    (testing "every ORU rung of the offset result transmits at or before that result"
      (is (= 2 (count oru-rungs)))
      (is (every? #(<= (compare (msh-7 %) (transmit "MRN000001-R01-5200")) 0) oru-rungs)))
    (testing "and the ordering holds in the emitted SEQUENCE, not only in the field"
      (let [ids (mapv msh-10 messages)
            pos (into {} (map-indexed (fn [i id] [id i])) ids)]
        (is (every? #(< (long (pos "MRN000001-O01-1200")) (long (pos (msh-10 %)))) orm-rungs))
        (is (every? #(> (long (pos "MRN000001-R01-5200")) (long (pos (msh-10 %)))) oru-rungs))))))

(deftest the-latency-plan-for-every-non-ladder-message-is-untouched
  (let [offsets {"MRN000001-O01-1200" 1800 "MRN000001-R01-5200" 3600
                 "MRN000001-A01-1000" 600}
        without (emit/emit-wire log ref-date utc-offset facility providers nil offsets)
        with (wire both-ladders offsets nil)
        moved-ids #{"MRN000001-R01-5200" "MRN000001-R01-6200"}]
    (is (= (filterv #(not (moved-ids (msh-10 %))) without)
           (filterv #(and (not (rung? %)) (not (moved-ids (msh-10 %)))) with))
        "byte-equal under a real latency profile too: the ladder inserts, it never re-times")))

;; --- Purity, in place of a draw law --------------------------------------

(deftest plan-ladders-is-a-pure-function-of-log-and-config
  (is (= (plan both-ladders) (plan both-ladders)))
  (is (= (wire both-ladders) (wire both-ladders)))
  (testing "turning the ladder on does not disturb chatter's own draws"
    (let [chatter-profile {:demographic-update 1.0 :coverage-change 1.0 :registered 1.0
                           :restatement {:rate-per-patient-day 2.0}}
          chatter (planners/plan-chatter (Random. 7) log chatter-profile)
          without (emit/emit-wire log ref-date utc-offset facility providers nil {}
                                      {:chatter chatter})
          with (emit/emit-wire log ref-date utc-offset facility providers nil {}
                                   {:chatter chatter :ladders (plan both-ladders)})
          chatter? (comp #{"ADT^A08" "ADT^A31" "ADT^A28"} msh-9)]
      (is (seq (filterv chatter? without)))
      (is (= (filterv chatter? without) (filterv chatter? with))))))

;; --- The consumer: a preliminary is not folded ---------------------------

(deftest replaying-a-laddered-corpus-reconstructs-the-un-laddered-state
  (let [plain (emit/emit log ref-date utc-offset facility providers)
        laddered (wire both-ladders)
        fold (fn [ms] (reduce v2-replay/fold-message {} ms))]
    (testing "the observations are actually there -- an equality of two empties proves nothing"
      (is (pos? (count (:observations (get (fold plain) "MRN000001"))))))
    (is (= (fold plain) (fold laddered))
        "a PRELIMINARY ORU^R01 is superseded by the final one and must not be folded twice --
         so a laddered corpus and its un-laddered twin reconstruct the same accumulator")
    (testing "and the rungs really did reach the folder"
      (is (< (count plain) (count laddered))))))

;; --- The derivability law, in the form sweep 2 corrected it ---------------

(deftest every-message-is-derivable-from-the-log-and-the-ladder-config
  (let [{:keys [rungs]} (plan both-ladders)
        messages (wire both-ladders)
        base (emit/emit log ref-date utc-offset facility providers)]
    (testing "TOTAL: every message traces to exactly one basis -- a ground-truth event's
              own render, or one ladder instruction. The terminal status codes ADD NO
              MESSAGE; they edit one that already existed, which is why this arithmetic
              is a plain sum and not a sum plus a correction."
      (is (= (count messages) (+ (count base) (count rungs)))))
    (testing "INJECTIVE on the FOUR-part key -- `(mrn, trigger, at, ordinal)`, the key
              MSH-10 actually carries. ADR-0175 section 4's three-part
              `(basis-event-index, trigger, ordinal)` is NOT asserted here, because sweep 2
              measured it non-injective for chatter; the ladder must not worsen that, and
              on its own instructions the triple happens to hold -- which is stated rather
              than relied on, since one rung per (order, family, index) is a property of
              the CONFIG, not of the law."
      (let [keys4 (mapv (juxt :active-mrn :trigger :at :ordinal) rungs)]
        (is (= (count keys4) (count (distinct keys4)))))
      (is (= (count messages) (count (distinct (map msh-10 messages))))))
    (testing "DERIVABLE: re-rendering from the same log and the same config reproduces
              the stream byte for byte"
      (is (= messages (wire both-ladders))))))

(deftest the-ladder-adds-no-registry-entry-and-no-event-kind
  (testing "a rung is a restatement of a family the registry ALREADY carries, so unlike
            chatter's A08/A31/A28 and unlike the DFT it needs no entry, no fold arm, and
            no sampler change. If this set ever moves under a ladder sweep, the sweep has
            crossed `rulings.md#R-skeleton-or-emission` and owes an event-schema answer."
    ;; ARC 4 SWEEP 4 widened this set, and the widening is exactly what
    ;; this assertion is for: it is a whole-registry pin, so a sweep
    ;; that adds a family has to come here and say so. Sweep 4 added
    ;; scheduling's four (SIU^S12/S14/S15/S26, ADR-0175 ruling B1) --
    ;; which is a REGISTRY change made deliberately by a sweep whose
    ;; own charter was to make one, and not a ladder crossing
    ;; `rulings.md#R-skeleton-or-emission`. The sentence above still
    ;; binds a LADDER sweep, unchanged.
    (is (= #{:admission :discharge :transfer :cancel-admit :cancel-transfer
             :cancel-discharge :bed-swap :merge :order-placed :result-available
             :outpatient-visit :bed-status-change :observation :diagnostic-report
             :appointment :reschedule :appointment-cancel :no-show}
           (set (keys registry/message-type-registry))))
    (testing "and both ladder families are SKELETON, so `gate v2` gates every rung in full"
      (is (contains? registry/skeleton-message-types "ORM^O01"))
      (is (contains? registry/skeleton-message-types "ORU^R01")))))
