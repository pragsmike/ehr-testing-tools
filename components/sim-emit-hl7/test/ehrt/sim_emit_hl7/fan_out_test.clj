(ns ehrt.sim-emit-hl7.fan-out-test
  "ARC 4 SWEEP 5 (`notes/adr/0175-arc-4-emission-add-ons.md` design (f),
  ruling B1; author ruling 2026-08-28, collision option (b)).

  THE PROOF SHAPE OF THIS SWEEP IS DIFFERENT FROM SWEEPS 1-4, and it is
  worth saying at the top of the first gate that carries it. Chatter,
  charges, ladders and SIU each put NEW BYTES on the wire, so each owed
  a declared message-digest change at its own turn-on. Fan-out puts
  none: it re-delivers bytes that already exist. BOTH brackets
  therefore read IDENTICAL at BOTH commits, and the evidence lives one
  layer out -- in the subsequence law below, the spool digests, and the
  ACK pairing law in `ehrt.corpus-io.mllp-test`.

  This namespace holds the half a population-scale run cannot state
  cheaply: the PV1-less rule needs an ADT^A20 beside an ADT^A01 whose
  patient class I chose, and the mask needs an MSH every field of which
  is known by hand. `ehrt.sim.fan-out-run-test` holds the other half --
  the same law over a REAL run's messages, where the population is not
  mine to choose."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.sim-emit-hl7.fan-out :as fan-out]))

;; ---- a hand-built base spool, in this emitter's own rendering -------

(defn- msh
  [trigger control-id]
  (str "MSH|^~\\&|EHR-TESTING-SIM|SIM|||20260811003000+0000||ADT^" trigger
       "|" control-id "|P|2.4"))

(defn- adt
  "An ADT carrying a PV1 whose PV1-2 is `class-code`."
  [trigger control-id class-code]
  (str (msh trigger control-id) "\r"
       "EVN|" trigger "|20260811003000+0000\r"
       "PID|1||" control-id "^^^SIM^MR||Doe^Jane^^^^^L|||F\r"
       "PV1|1|" class-code "|Emergency^ED-H01^^GENERAL||||||||||||||||V1\r"))

(defn- a20
  "ADT^A20 -- `[MSH EVN NPU]`, and THE POINT: no PV1 at all."
  [control-id]
  (str (msh "A20" control-id) "\r"
       "EVN|A20|20260811003000+0000\r"
       "NPU|ED-H01|C\r"))

(defn- oru
  "An ORU whose MSH-9 carries a THIRD component, which is the shape a
  filter written `ORU^R01` still has to match."
  [control-id]
  (str "MSH|^~\\&|EHR-TESTING-SIM|SIM|||20260811003000+0000||ORU^R01^ORU_R01|"
       control-id "|P|2.4\r"
       "PID|1||" control-id "^^^SIM^MR||Doe^Jane^^^^^L|||F\r"
       "OBX|1|NM|58410-2^CBC^LN||5.1|10*9/L|4.0-11.0|N|||F\r"))

(def ^:private base
  "Eleven messages: inpatient and outpatient ADTs, three PV1-less A20s,
  and two ORUs."
  [(adt "A01" "MRN0001-A01-10" "I")
   (adt "A02" "MRN0001-A02-20" "I")
   (a20 "ED-H01-dirty-A20-21")
   (adt "A01" "MRN0002-A01-30" "O")
   (oru "MRN0001-R01-40")
   (a20 "ED-H01-cleaning-A20-41")
   (adt "A03" "MRN0001-A03-50" "I")
   (adt "A03" "MRN0002-A03-60" "O")
   (a20 "ED-H01-ready-A20-61")
   (oru "MRN0002-R01-70")
   (adt "A01" "MRN0003-A01-80" "I")])

;; ---- the law, as an executable checker -----------------------------

(def ^:private sentinel " MASKED ")

(defn law-violations
  "THE SUBSEQUENCE LAW, executable. Returns a vector of violation
  strings -- empty means the law holds -- for one plan entry against
  the base message vector:

    (1) `:indices` are in range and STRICTLY INCREASING;
    (2) `:indices` and `:messages` have the same length;
    (3) for every k, message k equals `base[indices[k]]` after
        replacing, IN BOTH, exactly the MSH fields the subscriber's own
        `:msh` map names -- and nothing else.

  (3) with no `:msh` is plain byte equality, which is the no-override
  half of the law: a subscriber's spool is a BYTE-EXACT SUBSEQUENCE of
  the base spool.

  The mask is `fan-out/mask-msh` itself, applied to BOTH sides with a
  constant sentinel, so an override cannot be checked against a mask
  written twice; and a checker rather than a bare assertion,
  deliberately, so the property below and the mutant gate beside it ask
  the IDENTICAL question -- a mutant that fails a different question
  proves nothing about the property."
  [base-messages {:keys [name indices messages msh]}]
  (let [erase (fn [m] (fan-out/mask-msh m (or msh {}) (constantly sentinel)))]
    (cond-> []
      (not= (count indices) (count messages))
      (conj (str name ": " (count indices) " indices but " (count messages) " messages"))

      (not (every? #(< -1 % (count base-messages)) indices))
      (conj (str name ": an index falls outside the base spool -- " (pr-str indices)))

      (not (apply < -1 indices))
      (conj (str name ": indices are not strictly increasing -- " (pr-str indices)))

      :always
      (into (keep (fn [[k i m]]
                    (when (and (< -1 i (count base-messages))
                               (not= (erase (nth base-messages i)) (erase m)))
                      (str name ": message " k " (base index " i
                           ") is not the base message under the declared mask")))
                  (map vector (range) indices messages))))))

;; ---- the law, as a property ----------------------------------------

(def ^:private subscriber-gen
  (gen/let [n gen/nat
            types (gen/one-of
                   [(gen/return nil)
                    (gen/fmap set (gen/not-empty
                                   (gen/vector (gen/elements ["ADT^A01" "ADT^A02" "ADT^A03"
                                                              "ADT^A20" "ORU^R01"]))))])
            classes (gen/one-of
                     [(gen/return nil)
                      (gen/fmap set (gen/not-empty
                                     (gen/vector (gen/elements [:inpatient :outpatient]))))])
            msh-overrides (gen/elements
                           [nil
                            {:receiving-app "ADT-CONSUMER"}
                            {:receiving-app "LAB" :receiving-facility "WEST"}
                            {:sending-app "FEED" :sending-facility "S"
                             :receiving-app "R" :receiving-facility "F"}])]
    (cond-> {:name (keyword (str "sub-" n))}
      (or types classes) (assoc :filter (cond-> {}
                                          types (assoc :message-types types)
                                          classes (assoc :patient-classes classes)))
      msh-overrides (assoc :msh msh-overrides))))

(deftest the-subsequence-law
  (testing "ADR-0175 section 2(f)'s own law: every subscriber's spool
            is a byte-exact subsequence of the base spool, in the same
            order, after masking exactly the overridden MSH fields"
    (let [result (tc/quick-check
                  200
                  (prop/for-all [table (gen/vector subscriber-gen 1 4)]
                    (empty? (mapcat #(law-violations base %)
                                    (fan-out/plan base table nil)))))]
      (is (:pass? result) (str "subsequence law failed: " (pr-str (:shrunk result)))))))

(deftest the-subsequence-law-is-not-vacuous
  (testing "the RED WITNESS, made permanent. ADR-0175 section 2(f): 'a
            property that has never failed is a property nobody has
            tested', and the mutation it names is the rejected
            alternative -- render each subscriber's stream separately,
            which permits two subscribers to disagree about one MSH-10.
            On disk that shows up as a spool whose order is not the base
            spool's, so the checker must catch both a reordering and a
            re-sorting."
    (let [[sub] (fan-out/plan base [{:name :everything}] nil)
          reordered (assoc sub
                           :indices (vec (reverse (:indices sub)))
                           :messages (vec (reverse (:messages sub))))
          resorted (assoc sub :messages (vec (sort (:messages sub))))
          dropped (assoc sub :messages (vec (rest (:messages sub))))
          violations-reordered (law-violations base reordered)
          violations-resorted (law-violations base resorted)]
      (is (empty? (law-violations base sub))
          "the unmutated plan must satisfy the law, or these mutants prove nothing")
      (is (seq violations-reordered)
          "a reversed spool passed the subsequence law -- the law is vacuous")
      (is (some #(str/includes? % "strictly increasing") violations-reordered)
          (str "the reversal was caught, but not as an ordering violation: "
               (pr-str violations-reordered)))
      (is (seq violations-resorted)
          "a spool re-sorted by content passed the law -- position is not being checked")
      (is (seq (law-violations base dropped))
          "a spool missing a message passed the law -- length is not being checked"))))

;; ---- the rules, one gate each --------------------------------------

(deftest no-filter-is-the-whole-spool-byte-for-byte
  (let [[sub] (fan-out/plan base [{:name :mirror}] nil)]
    (is (= base (:messages sub)))
    (is (= (vec (range (count base))) (:indices sub)))
    (is (= (count base) (:count sub)))))

(deftest a-message-type-filter-selects-by-msh-9
  (let [[sub] (fan-out/plan base [{:name :adt-feed
                                   :filter {:message-types #{"ADT^A01" "ADT^A03"}}}] nil)]
    (is (= [0 3 6 7 10] (:indices sub)))
    (is (every? #(str/includes? (first (str/split % #"\r")) "|ADT^A0") (:messages sub)))))

(deftest a-three-component-msh-9-matches-a-two-component-filter
  (testing "ORU^R01^ORU_R01 on the wire matches a filter written
            ORU^R01 -- the first two components ARE the message type"
    (let [[sub] (fan-out/plan base [{:name :lab :filter {:message-types #{"ORU^R01"}}}] nil)]
      (is (= [4 9] (:indices sub))))))

(deftest a-class-filter-excludes-every-pv1-less-message
  (testing "ADR-0175 section 2(f)'s rule, stated rather than
            discovered: ADT^A20 is [MSH EVN NPU] and carries no PV1 at
            all, so a class filter drops it"
    (let [[sub] (fan-out/plan base [{:name :inpatients
                                     :filter {:patient-classes #{:inpatient}}}] nil)]
      (is (= [0 1 6 10] (:indices sub)))
      (is (not-any? #(str/includes? % "NPU|") (:messages sub))))))

(deftest unless-the-subscriber-names-that-trigger-explicitly
  (testing "the escape clause the same rule names: a bed-management
            feed that asks for A20 BY NAME gets its A20s, and still
            gets only inpatient PV1-bearing messages otherwise"
    (let [[sub] (fan-out/plan base [{:name :beds
                                     :filter {:message-types #{"ADT^A01" "ADT^A20"}
                                              :patient-classes #{:inpatient}}}] nil)]
      (is (= [0 2 5 8 10] (:indices sub))
          "the three A20s ride on the named trigger; MRN0002's OUTPATIENT A01 does not"))))

(deftest a-class-filter-alone-is-the-silently-empty-feed-this-rule-exists-for
  (testing "the failure the rule prevents, shown rather than described:
            a bed feed authored with a class filter and no explicit
            A20 receives NOT ONE bed-status message"
    (let [[sub] (fan-out/plan base [{:name :beds-authored-wrong
                                    :filter {:patient-classes #{:inpatient}}}] nil)]
      (is (not-any? #(str/includes? % "NPU|") (:messages sub))))))

(deftest an-msh-override-rewrites-exactly-msh-3-4-5-6
  (let [[sub] (fan-out/plan base [{:name :routed
                                   :filter {:message-types #{"ADT^A01"}}
                                   :msh {:receiving-app "ADT-CONSUMER"
                                         :receiving-facility "WEST"}}] nil)
        m (first (:messages sub))
        fields (str/split (first (str/split m #"\r")) #"\|" -1)
        base-fields (str/split (first (str/split (nth base 0) #"\r")) #"\|" -1)]
    (is (= "ADT-CONSUMER" (nth fields 4)) "MSH-5")
    (is (= "WEST" (nth fields 5)) "MSH-6")
    (is (= (nth base-fields 2) (nth fields 2)) "MSH-3 untouched")
    (is (= (nth base-fields 3) (nth fields 3)) "MSH-4 untouched")
    (testing "and no other byte of the message moves"
      (is (= (assoc base-fields 4 "ADT-CONSUMER" 5 "WEST") fields))
      (is (= (rest (str/split (nth base 0) #"\r" -1))
             (rest (str/split m #"\r" -1)))))))

(deftest an-msh-override-may-also-rewrite-the-sending-pair
  (let [[sub] (fan-out/plan base [{:name :relabelled
                                   :msh {:sending-app "GATEWAY" :sending-facility "DMZ"}}] nil)
        fields (str/split (first (str/split (first (:messages sub)) #"\r")) #"\|" -1)]
    (is (= "GATEWAY" (nth fields 2)))
    (is (= "DMZ" (nth fields 3)))))

(deftest the-mask-preserves-a-trailing-segment-terminator
  (testing "every message this emitter renders ends with its own CR,
            and a mask that split segments on the default limit would
            silently eat it -- a moved byte the subsequence law forbids"
    (let [[sub] (fan-out/plan base [{:name :routed
                                     :filter {:message-types #{"ADT^A01"}}
                                     :msh {:receiving-app "X"}}] nil)]
      (is (str/ends-with? (nth base 0) "\r"))
      (is (str/ends-with? (first (:messages sub)) "\r")))))

(deftest a-subscriber-that-matches-nothing-spools-nothing-and-says-so
  (let [[sub] (fan-out/plan base [{:name :none :filter {:message-types #{"ADT^A04"}}}] nil)]
    (is (= [] (:indices sub)))
    (is (= 0 (:count sub)))
    (is (empty? (law-violations base sub)))))

(deftest the-table-order-is-the-result-order
  (let [plan (fan-out/plan base [{:name :a} {:name :b} {:name :c}] nil)]
    (is (= [:a :b :c] (mapv :name plan)))))

(deftest a-site-profile-class-override-routes-on-what-was-actually-written
  (testing "`:patient-classes` names ENGINE vocabulary; PV1-2 carries a
            CODE. The two are joined through `site-profile/code-for` --
            the same override path the emitter itself used -- so a site
            that renders :inpatient as something other than \"I\"
            routes on the string it wrote, not on the default table."
    (let [profile {:code-tables {:patient-class {:inpatient {:code "IP"}}}}
          renamed (mapv #(str/replace % "PV1|1|I|" "PV1|1|IP|") base)
          [sub] (fan-out/plan renamed [{:name :inpatients
                                        :filter {:patient-classes #{:inpatient}}}]
                              profile)]
      (is (= [0 1 6 10] (:indices sub)))
      (testing "and the default table would have matched nothing here"
        (is (= [] (:indices (first (fan-out/plan renamed
                                                 [{:name :inpatients
                                                   :filter {:patient-classes #{:inpatient}}}]
                                                 nil))))))))
  )
