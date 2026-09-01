(ns ehrt.sim-emit-hl7.chatter-test
  "ARC 4 SWEEP 2 (`notes/adr/0175-arc-4-emission-add-ons.md` design (a),
  ruling B1, landed DARK): re-statement chatter -- A08 / A31 / A28 and
  the IN1-only coverage update.

  THE LOG HERE IS HAND-BUILT, and that is a deliberate choice with a
  known cost. `components/sim-emit-hl7` may not depend on
  `components/sim`, so a `run-command` with `:persons` -- the only
  producer of `:demographic-update` and `:coverage-change` -- is out of
  reach from this brick. What a hand-built log CAN do is put both sides
  of the A08-vs-A31 rule under the assertion at once, including the
  same-instant collision that makes the control-id ordinal necessary,
  which no seed reliably produces. What it cannot do is find the
  defects only a population finds -- so the `pos?` witness table over
  real corpora lives in `ehrt.sim.chatter-run-test`, one layer up,
  where `run-command` is available. Arc 3b sweep 3's own record is the
  precedent: `neither defect was reachable from the unit fixture`.

  Every assertion below is about EMISSION. Nothing here reads or writes
  ground truth, and `bin/ground-truth-bracket` is what proves that per
  commit rather than this docstring asserting it."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.string :as str]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-emit-hl7.emit :as emit]
            [ehrt.sim-emit-hl7.planners :as planners]
            [ehrt.sim-emit-hl7.registry :as registry]
            [ehrt.sim-emit-hl7.segments :as segments]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.util Random]))

(def ^:private ref-date "2024-01-01")
(def ^:private utc-offset "+00:00")

(def ^:private persona-0
  {:name {:family "Kowalski" :given "Dana"}
   :dob "1980-04-17"
   :sex :female
   :address {:street "12 Elm St" :city "Boise" :state "ID" :zip "83702"}
   :phone "208-555-0134"
   :payer {:id "commercial-ppo" :name "Commercial PPO" :type :commercial}})

(def ^:private subject "PID-000000-cafebabe")

(defn- ev
  [m]
  (merge {:participants [{:patient-id subject :role :subject}] :warm-up false} m))

(def ^:private ward-location
  {:ward "Emergency" :bed "ED-H01" :placement :surge})

(def ^:private log
  "One patient, one closed encounter, and demographic churn on BOTH
  sides of it -- the A08-vs-A31 split under one assertion. `t` 1200 and
  1201 are inside the encounter; 90000 and 90001 are a day later and
  well after the discharge; the two at 90000 are the SAME instant
  deliberately, which is the control-id collision ADR-0175 section 2(a)
  says the ordinal exists for."
  [(ev {:event :registered :t 0 :active-mrn "MRN000001" :persona persona-0})
   (ev {:event :admission :t 1000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893" :home-ward "Emergency"
        :reason "Chest pain" :forced false})
   (ev {:event :demographic-update :t 1200 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :cause :identity-correction :field :name
        :prior-value {:family "Kowalski" :given "Dana"}
        :value {:family "Nowak" :given "Dana"}})
   (ev {:event :coverage-change :t 1201 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :cause :eligibility
        :prior-payer {:id "commercial-ppo" :name "Commercial PPO" :type :commercial}
        :payer {:id "medicare" :name "Medicare" :type :medicare}})
   (ev {:event :discharge :t 2000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893"})
   (ev {:event :demographic-update :t 90000 :active-mrn "MRN000001"
        :cause :residence-move :field :residence
        :prior-value {:status :housed :address {:street "12 Elm St" :city "Boise" :state "ID" :zip "83702"}}
        :value {:status :housed :address {:street "44 Oak Ave" :city "Reno" :state "NV" :zip "89501"}}})
   (ev {:event :demographic-update :t 90000 :active-mrn "MRN000001"
        :cause :identity-correction :field :dob
        :prior-value "1980-04-17" :value "1980-04-18"})
   (ev {:event :coverage-change :t 90001 :active-mrn "MRN000001"
        :cause :eligibility
        :prior-payer {:id "medicare" :name "Medicare" :type :medicare}
        :payer {:id "self-pay" :name "Self-Pay" :type :self-pay}})])

(def ^:private facility sim-model/default-facility)

(def ^:private providers
  [{:id "1234567893" :name {:family "Reyes" :given "Priya"}}])

(def ^:private all-on
  {:demographic-update 1.0 :coverage-change 1.0 :registered 1.0})

(defn- wire
  ([chatter-profile] (wire chatter-profile 7))
  ([chatter-profile rng-seed]
   (emit/emit-wire log ref-date utc-offset facility providers nil {}
                       {:chatter (planners/plan-chatter (Random. (long rng-seed)) log chatter-profile)})))

(defn- msh-9 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 8))
(defn- msh-10 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 9))
(defn- segments [m] (mapv #(first (str/split % #"\|")) (str/split m #"\r")))
(defn- segment [m name] (first (filter #(str/starts-with? % (str name "|")) (str/split m #"\r"))))
(defn- of-type [messages t] (filterv #(= t (msh-9 %)) messages))

;; --- The identity half: absent, nil and {} chatter all render exactly
;; what this emitter rendered before design (a) existed. --------------------

(deftest absent-nil-and-empty-chatter-are-the-byte-identical-path
  (let [plain (emit/emit log ref-date utc-offset facility providers)]
    (is (= plain (emit/emit-wire log ref-date utc-offset facility providers nil {})))
    (is (= plain (emit/emit-wire log ref-date utc-offset facility providers nil {} {})))
    (is (= plain (emit/emit-wire log ref-date utc-offset facility providers nil {} {:chatter nil})))
    (is (= plain (emit/emit-wire log ref-date utc-offset facility providers nil {} {:chatter []})))
    (testing "and a profile that names no rule still draws its whole
              census and produces nothing"
      (is (= [] (planners/plan-chatter (Random. 1) log {})))
      (is (= [] (planners/plan-chatter (Random. 1) log nil)))
      (is (= plain (wire {}))))))

;; --- ADR-0175 section 2(a)'s message table, verbatim ---------------------

(deftest the-message-table-verbatim
  (let [messages (wire all-on)]
    (testing ":demographic-update with an encounter open at t -> ADT^A08,
              MSH EVN PID PV1"
      (let [a08 (of-type messages "ADT^A08")]
        (is (pos? (count a08)))
        (is (= 1 (count (filter #(str/includes? % "-A08-1200-") a08)))
            "the in-encounter demographic update, and only it")
        (let [m (first (filter #(str/includes? % "-A08-1200-") a08))]
          (is (= ["MSH" "EVN" "PID" "PV1"] (segments m)))
          (is (str/includes? (segment m "PV1") "ENC-1")
              "PV1-19 carries the encounter the instant falls inside"))))

    (testing ":demographic-update with no encounter open -> ADT^A31,
              MSH EVN PID and no PV1 at all"
      (let [a31 (of-type messages "ADT^A31")
            m (first (filter #(str/includes? % "-A31-90000-0") a31))]
        (is (pos? (count a31)))
        (is (some? m))
        (is (= ["MSH" "EVN" "PID"] (segments m)))))

    (testing ":coverage-change follows the SAME rule and carries IN1,
              with the PID unchanged"
      (let [in-enc (first (filter #(str/includes? % "-A08-1201-") messages))
            out-enc (first (filter #(str/includes? % "-A31-90001-") messages))]
        (is (some? in-enc) "in-encounter coverage change is an A08")
        (is (some? out-enc) "out-of-encounter coverage change is an A31")
        (is (= ["MSH" "EVN" "PID" "PV1" "IN1"] (segments in-enc)))
        (is (= ["MSH" "EVN" "PID" "IN1"] (segments out-enc)))
        (is (str/includes? (segment in-enc "IN1") "medicare"))
        (is (str/includes? (segment out-enc "IN1") "self-pay"))
        (testing "PID unchanged: the coverage restatement's PID is the
                  same PID the demographic restatement one second
                  earlier rendered -- a payer change moves no PID field"
          (is (= (segment (first (filter #(str/includes? % "-A08-1200-") messages)) "PID")
                 (segment in-enc "PID"))))))

    (testing ":registered -> ADT^A28"
      (let [a28 (of-type messages "ADT^A28")]
        (is (= 1 (count a28)))
        (is (= ["MSH" "EVN" "PID"] (segments (first a28))))
        (is (str/includes? (msh-10 (first a28)) "-A28-0-"))))

    (testing "every restatement renders the demographics AS THEY STOOD
              at its own instant, never the ones the log ended with"
      (is (str/includes? (segment (first (filter #(str/includes? % "-A08-1200-") messages)) "PID")
                         "Nowak^Dana")
          "the name correction at t=1200 is already folded")
      (is (str/includes? (segment (first (filter #(str/includes? % "-A31-90000-0") messages)) "PID")
                         "44 Oak Ave")
          "and the residence move at t=90000 too"))))

(deftest periodic-restatement-is-counted-separately-and-is-where-the-a08-volume-comes-from
  (testing "ADR-0175 section 2(a): an event-driven-only reading of an
            A08 witness is the miss the design names. These two halves
            are asserted apart, never as one A08 total."
    (let [plan (planners/plan-chatter (Random. 7) log
                                      (assoc all-on :restatement {:rate-per-patient-day 1.0}))
          {periodic true event-driven false} (group-by (comp boolean :periodic?) plan)]
      (is (pos? (count event-driven)))
      (is (pos? (count periodic)))
      (is (= #{"A08"} (set (map :trigger periodic)))
          "every periodic instant is inside an open encounter by
           construction, so the A08-vs-A31 rule answers A08 for all of
           them -- the rule is applied, not bypassed")
      (is (= 1 (count periodic))
          "one encounter, 1000 seconds long, is one started patient-day")
      (is (= 2 (count (filter #(= "A08" (:trigger %)) event-driven)))
          "and exactly the two event-driven restatements that happened
           inside it")))
  (testing "the rate scales the census: r = 3 is three restatements on
            every patient-day of care, evenly spaced inside the day"
    (let [plan (planners/plan-chatter (Random. 7) log {:restatement {:rate-per-patient-day 3.0}})]
      (is (= 3 (count plan)))
      (is (= [1000 1333 1666] (mapv :at plan))))))

;; --- The fixed-consumption law: two configs differing in ONE rule --------

(deftest two-configs-differing-in-one-rule-draw-identically-for-everything-else
  (testing "ADR-0175 section 2(a)'s draw-and-discard law, `plan-latency`'s
            own words: ALWAYS one draw per ground-truth event in log
            order plus one per patient-day of care, whether or not a
            rule covers it -- so adding a rule for kind X can never
            shift kind Y's draws"
    (doseq [[label a b] [["registered on/off"
                          {:demographic-update 0.5 :coverage-change 0.5}
                          {:demographic-update 0.5 :coverage-change 0.5 :registered 1.0}]
                         ["coverage-change on/off"
                          {:demographic-update 0.5 :registered 0.5}
                          {:demographic-update 0.5 :registered 0.5 :coverage-change 1.0}]
                         ["periodic on/off"
                          {:demographic-update 0.5 :coverage-change 0.5}
                          {:demographic-update 0.5 :coverage-change 0.5
                           :restatement {:rate-per-patient-day 1.0}}]]]
      (testing label
        (let [pa (planners/plan-chatter (Random. 99) log a)
              pb (planners/plan-chatter (Random. 99) log b)
              shared (fn [plan kinds] (filterv #(kinds (:kind %)) plan))
              kinds (into #{} (map :kind) pa)]
          (is (seq kinds) "the unchanged rules must actually fire, or
                           this comparison is vacuous")
          (is (= (mapv #(dissoc % :ordinal :control-id) (shared pa kinds))
                 (mapv #(dissoc % :ordinal :control-id) (shared pb kinds)))
              "every instruction the UNCHANGED rules produced is
               identical on both sides"))))))

(defspec plan-chatter-is-a-pure-function-of-rng-log-and-profile 50
  (prop/for-all [rng-seed (gen/large-integer* {:min 0 :max 1000000})]
    (= (planners/plan-chatter (Random. ^long rng-seed) log
                              (assoc all-on :restatement {:rate-per-patient-day 0.5}))
       (planners/plan-chatter (Random. ^long rng-seed) log
                              (assoc all-on :restatement {:rate-per-patient-day 0.5})))))

;; --- MSH-10: the control-id ordinal, at a real t collision --------------

(deftest msh-10-is-unique-across-the-whole-stream-including-at-a-t-collision
  (let [messages (wire (assoc all-on :restatement {:rate-per-patient-day 2.0}))
        ids (mapv msh-10 messages)]
    (is (= (count ids) (count (distinct ids)))
        (str "duplicate MSH-10: "
             (sort (map first (filter #(< 1 (val %)) (frequencies ids))))))
    (testing "the two demographic updates at t=90000 are the collision
              this ordinal exists for -- same mrn, same trigger, same
              instant, two messages"
      (let [colliding (filterv #(str/includes? % "-A31-90000-") messages)]
        (is (= 2 (count colliding)))
        (is (= #{"MRN000001-A31-90000-0" "MRN000001-A31-90000-1"}
               (set (map msh-10 colliding))))))
    (testing "and a ground-truth event's own control id still carries NO
              ordinal, so the two id spaces cannot collide"
      (is (= "MRN000001-A01-1000" (segments/control-id-for (nth log 1)))))))

;; --- The interleave: chatter rides emit-wire's own sort, and the
;; latency plan for every non-chatter message is untouched. ---------------

(deftest chatter-interleaves-at-its-own-instant-and-moves-no-other-messages-bytes
  (let [latency {:admission {:from-minutes 30 :to-minutes 30}
                 :discharge {:from-minutes 5 :to-minutes 5}}
        offsets (planners/plan-latency (Random. 3) log latency)
        without (emit/emit-wire log ref-date utc-offset facility providers nil offsets)
        with (emit/emit-wire log ref-date utc-offset facility providers nil offsets
                                 {:chatter (planners/plan-chatter (Random. 7) log all-on)})
        add-on? #(#{"ADT^A08" "ADT^A31" "ADT^A28"} (msh-9 %))]
    (is (pos? (count (filter add-on? with))))
    (is (= without (filterv (complement add-on?) with))
        "byte-equal AND in the same order -- the latency plan is
         untouched and no existing message moved")
    (testing "and the stream as a whole is still transmit-time ordered"
      (let [ts (mapv #(nth (str/split (first (str/split % #"\r")) #"\|") 6) with)]
        (is (= ts (sort ts)))))))

;; --- The derivability law ADR-0175 section 4 replaces the bijection with:
;; every message is derivable from (log, emission-config), and every
;; message maps to exactly one (basis-event-index, trigger, ordinal). ------

(deftest every-message-is-derivable-from-the-log-and-the-emission-config
  (let [profile (assoc all-on :restatement {:rate-per-patient-day 2.0})
        plan (planners/plan-chatter (Random. 7) log profile)
        messages (wire profile)
        base (emit/emit log ref-date utc-offset facility providers)]
    (testing "TOTAL: every message traces to exactly one basis -- a
              ground-truth event's own render, or one chatter
              instruction. The old `bidirectional-derivability`
              bijection cannot say this (a periodic A08 has no event of
              its own), which is why ADR-0175 section 4 replaces it."
      (is (= (count messages) (+ (count base) (count plan)))))
    (testing "INJECTIVE -- and the KEY IS FOUR-PART, not the three ADR-0175
              section 4 names. Its sentence is `every message maps to
              exactly one (basis-event-index, trigger, ordinal) triple,
              which is what its MSH-10 carries`, and the two halves of
              that sentence contradict each other: section 2(a) scopes
              the ordinal to `(mrn, trigger, t)`, so two periodic
              restatements inside ONE patient-day share a basis (their
              encounter's opener), a trigger and an ordinal 0, and
              differ only in the instant. Measured here at r = 2.0: 8
              instructions, 7 distinct triples. MSH-10 itself is
              `mrn-trigger-t-ordinal` and IS unique -- the INSTANT is
              the part section 4 dropped. The law asserted is therefore
              the four-part key MSH-10 actually carries."
      (let [keys4 (mapv (juxt :active-mrn :trigger :at :ordinal) plan)]
        (is (= (count keys4) (count (distinct keys4)))))
      (is (= (count messages) (count (distinct (map msh-10 messages))))))
    (testing "DERIVABLE: re-rendering from the same log and the same
              config reproduces the stream byte for byte"
      (is (= messages (wire profile))))))

;; --- The contract half: chatter adds no event kind and no registry entry -

(deftest chatter-adds-no-message-type-registry-entry-and-no-event-kind
  (testing "a chatter message has no ground-truth event of its own to
            register, and design (a) is EMISSION by
            `rulings.md#R-skeleton-or-emission`. If this ever fails, the
            sweep has crossed that ruling and owes an event-schema
            answer, not a registry edit."
    ;; ARC 4 SWEEP 4 widened this set, and the widening is what a
    ;; whole-registry pin is FOR: a sweep that adds a family has to come
    ;; here and say so. Sweep 4 added scheduling's four
    ;; (SIU^S12/S14/S15/S26, ADR-0175 ruling B1), which are ground-truth
    ;; kinds the contract already declared -- a SKELETON family finally
    ;; rendered, not a restatement. The sentence above still binds a
    ;; CHATTER sweep, unchanged: chatter has no event of its own and
    ;; still owns no entry here.
    (is (= #{:admission :discharge :transfer :cancel-admit :cancel-transfer
             :cancel-discharge :bed-swap :merge :order-placed :result-available
             :outpatient-visit :bed-status-change :observation :diagnostic-report
             :appointment :reschedule :appointment-cancel :no-show}
           (set (keys registry/message-type-registry))))
    (is (empty? (filter #(contains? registry/message-type-registry %)
                        (keys registry/chatter-event-kinds)))
        "the three kinds chatter restates are exactly three of the
         registry's own deliberate silences, and stay silent there")
    (is (every? (into #{} (map first) (rest (rest engine/Event)))
                (keys registry/chatter-event-kinds))
        "and all three are contract kinds -- chatter invents none")))
