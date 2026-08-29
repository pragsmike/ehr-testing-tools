(ns ehrt.sim.fan-out-run-test
  "ARC 4 SWEEP 5 (`notes/adr/0175-arc-4-emission-add-ons.md` design (f),
  ruling B1; author ruling 2026-08-28, collision option (b)):
  `:fan-out` as a `:config` passenger, at POPULATION scale.

  This is the half `ehrt.sim-emit-hl7.fan-out-test` cannot reach. That
  namespace's base spool is hand-built, because `components/sim-emit-hl7`
  may not depend on `components/sim`; only a real `run-command`
  produces a message vector this project would actually ship, and only a
  real run can say that turning a subscriber table on moves NOT ONE BASE
  BYTE.

  THE SWEEP'S OWN CLAIM, and it is the strongest an arc-4 sweep can
  make: fan-out changes no ground truth AND no base message. Every
  previous arc-4 sweep owed a declared message-digest change at its
  turn-on; this one owes none, at either commit, and asserts the reason
  here per test run rather than only through the two brackets.

  THE SUBSEQUENCE LAW is restated compactly below rather than shared
  from `fan-out-test`'s own `law-violations`: a Polylith brick's test
  tree is its own, and a cross-brick test-namespace require would be a
  dependency `poly check` cannot see. The mask itself is NOT duplicated
  -- `emit-hl7/mask-msh` is exported for exactly this, so both gates
  erase the same fields with the same code."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]
            [ehrt.sim-engine.interface :as engine]
            [clojure.set :as set]
            [ehrt.sim.run :as run]))

(def ^:private base-opts
  "Small, fast, and REPRESENTATIVE of the six gated corpora: enough
  arrivals for several patient classes and several message families to
  exist, with the arc-3 opt-ins that make an encounter horizon and a
  bed cycle real -- the A20 stream in particular, which is what the
  PV1-less rule is about."
  {:seed 202 :patients 12 :churn true
   :persons {:count 24 :years 20}
   :encounters true
   :bed-cycle true
   :pathways [{:pathway {:name "workup"
                         :steps [{:type :admission :location "Renal"}
                                 {:type :order :profile :cbc}
                                 {:type :delay :from 180 :to 900}
                                 {:type :discharge}]}
               :weight 1}]
   :emit "hl7"})

(def ^:private table
  "(f)'s own worked example: an ADT feed and a lab feed."
  [{:name :adt-feed
    :filter {:message-types #{"ADT^A01" "ADT^A02" "ADT^A03"}}
    :msh {:receiving-app "ADT-CONSUMER" :receiving-facility "WEST"}}
   {:name :lab-feed
    :filter {:message-types #{"ORU^R01"}}
    :msh {:receiving-app "LAB-CONSUMER"}}])

(defn- run-payload [opts] (:payload (run/run-command opts)))

(defn- msh-9 [m] (nth (str/split (first (str/split m #"\r")) #"\|" -1) 8))

(def ^:private sentinel " MASKED ")

(defn- law-violations
  "The subsequence law, restated: indices strictly increasing and in
  range, lengths equal, and every spooled message equal to its base
  message once BOTH have had exactly the declared MSH fields erased.
  `ehrt.sim-emit-hl7.fan-out-test/law-violations` is the canonical
  statement; this is the same three clauses over a real run."
  [base {:keys [name indices messages msh]}]
  (let [erase #(emit-hl7/mask-msh % (or msh {}) (constantly sentinel))]
    (cond-> []
      (not= (count indices) (count messages))
      (conj (str name ": length mismatch"))

      (not (every? #(< -1 % (count base)) indices))
      (conj (str name ": index out of range"))

      (not (apply < -1 indices))
      (conj (str name ": indices not strictly increasing"))

      :always
      (into (keep (fn [[k i m]]
                    (when (and (< -1 i (count base)) (not= (erase (nth base i)) (erase m)))
                      (str name ": message " k " (base index " i ") differs under the mask")))
                  (map vector (range) indices messages))))))

;; ---- the sweep's own claim -----------------------------------------

(deftest fan-out-moves-no-ground-truth-and-no-base-message-byte
  (let [without (run-payload base-opts)
        with (run-payload (assoc base-opts :fan-out table))]
    (is (seq (:ground-truth without)))
    (is (= (:ground-truth without) (:ground-truth with))
        "arc 4's whole claim: an emission add-on moves no ground truth")
    (is (pos? (count (:messages without))))
    (is (= (:messages without) (:messages with))
        "THIS SWEEP's own claim, and it is stronger than any previous
         arc-4 sweep's: fan-out adds no message and moves none, so the
         base spool is byte-identical with the table on and off")
    (testing "and the key provably cannot reach the engine at all"
      (is (not (contains? (set engine/config-keys) :fan-out))))))

(deftest the-subsequence-law-over-a-real-run
  (let [{:keys [messages fan-out]} (run-payload (assoc base-opts :fan-out table))]
    (is (= 2 (count fan-out)))
    (is (= [:adt-feed :lab-feed] (mapv :name fan-out)))
    (testing "R-empty-population-is-red: the law says nothing about an
              empty spool, so the population is asserted first"
      (doseq [{:keys [name count]} fan-out]
        (is (pos? count) (str "subscriber " name " received no messages -- the law below is vacuous for it"))))
    (doseq [sub fan-out]
      (is (empty? (law-violations messages sub))
          (str/join "; " (law-violations messages sub))))))

(deftest a-subscriber-with-no-msh-override-is-a-byte-exact-subsequence
  (testing "the no-override half of the law, asserted as plain byte
            equality rather than through the mask"
    (let [{:keys [messages fan-out]}
          (run-payload (assoc base-opts :fan-out [{:name :mirror-adt
                                                   :filter {:message-types #{"ADT^A01"}}}]))
          {:keys [indices] :as sub} (first fan-out)]
      (is (pos? (:count sub)))
      (is (= (mapv #(nth messages %) indices) (:messages sub))))))

(deftest the-mirror-subscriber-is-the-whole-spool
  (let [{:keys [messages fan-out]}
        (run-payload (assoc base-opts :fan-out [{:name :everything}]))]
    (is (= messages (:messages (first fan-out))))))

;; ---- the allow-list law, and the vocabulary it reads ---------------

(deftest the-emitter-produces-nothing-outside-the-declared-vocabulary
  (testing "`emittable-message-types` is what the allow-list law
            measures against, and it is DECLARED rather than derivable
            for its add-on half -- so it owes a measurement. Over a run
            with every arc-4 add-on on, every MSH-9 actually emitted
            must be in the set, and each declared add-on family must be
            witnessed."
    (let [{:keys [messages]}
          (run-payload (assoc base-opts
                              :scheduling {:scheduled-fraction 0.5
                                           :lead-time-days [1 3]
                                           :no-show-rate 0.2
                                           :reschedule-rate 0.2
                                           :cancel-rate 0.2
                                           :follow-up {:rate 0.5 :interval-days [7 30]}}
                              :siu {}
                              :chatter {:demographic-update 1.0 :coverage-change 1.0
                                        :registered 1.0
                                        :restatement {:rate-per-patient-day 1.0}}
                              :charges {:price-table {"58410-2" {:amount 148.00}
                                                      "room-and-board" {:amount 900.00}}}
                              :ladders {:rungs [0.5] :order-rungs [0.25]}))
          emitted (into #{} (map msh-9) messages)]
      (is (seq emitted))
      (is (empty? (set/difference emitted emit-hl7/emittable-message-types))
          (str "the emitter produced an MSH-9 no fan-out filter could name: "
               (pr-str (sort (set/difference emitted emit-hl7/emittable-message-types)))
               " -- add it to `emit-hl7/add-on-message-types`"))
      (testing "and each declared add-on family is really produced"
        (doseq [t emit-hl7/add-on-message-types]
          (is (contains? emitted t)
              (str t " is declared as an add-on family but no run produces it")))))))

;; ---- rejection, before the engine ever runs ------------------------

(defn- reject-with-no-engine
  "Runs `opts` with an `:engine-run-fn` that THROWS. A branch that
  returns an error without the throw firing is a branch that ran before
  the engine -- the `:invalid-siu` precedent, asserted rather than
  assumed."
  [opts]
  (run/run-command opts {:engine-run-fn (fn [_] (throw (ex-info "the engine ran" {})))}))

(deftest a-malformed-fan-out-table-is-rejected-before-the-engine
  (doseq [[label bad] [["a misspelled subscriber key" [{:nome :x}]]
                       ["a misspelled filter key" [{:name :x :filter {:message-type #{"ADT^A01"}}}]]
                       ["a misspelled msh key" [{:name :x :msh {:recieving-app "X"}}]]
                       ["an empty table" []]
                       ["an empty filter" [{:name :x :filter {}}]]
                       ["an empty msh" [{:name :x :msh {}}]]
                       ["an empty message-type set" [{:name :x :filter {:message-types #{}}}]]
                       ["a duplicate name" [{:name :x} {:name :x}]]
                       ["a string name" [{:name "x"}]]
                       ["an msh value carrying a field separator" [{:name :x :msh {:receiving-app "A|B"}}]]
                       ["an unknown patient class" [{:name :x :filter {:patient-classes #{:daycase}}}]]]]
    (testing label
      (let [r (reject-with-no-engine (assoc base-opts :fan-out bad))]
        (is (result/error? r))
        (is (= :invalid-fan-out (:category r)))))))

(deftest an-unknown-type-trigger-is-a-config-error-not-an-empty-feed
  (testing "THE ALLOW-LIST LAW (ADR-0175 section 2(f)): a filter naming
            a TYPE^TRIGGER this emitter cannot produce is rejected
            before the engine runs -- never a subscriber spool that is
            silently empty forever because somebody wrote ADT^A05"
    (let [r (reject-with-no-engine
             (assoc base-opts :fan-out [{:name :x :filter {:message-types #{"ADT^A01" "ADT^A05"}}}]))]
      (is (result/error? r))
      (is (= :unknown-fan-out-message-type (:category r)))
      (is (= ["ADT^A05"] (:unknown (:payload r))))
      (is (contains? (set (:valid-options (:payload r))) "ADT^A01")))))

(deftest a-well-formed-table-naming-only-real-families-is-accepted
  (testing "the negative control: every family in the vocabulary is
            nameable, so the rejection above is about the TYPO and not
            about filters in general"
    (let [r (run/run-command (assoc base-opts :fan-out
                                    [{:name :all-of-it
                                      :filter {:message-types emit-hl7/emittable-message-types}}]))]
      (is (result/ok? r))
      (is (= (count (:messages (:payload r)))
             (:count (first (:fan-out (:payload r))))
             )
          "a filter naming the WHOLE vocabulary selects the whole spool"))))

(deftest fan-out-absent-adds-no-payload-key
  (testing "absent is the byte-identical path, all the way out to the
            shape of the payload a caller holds"
    (is (not (contains? (run-payload base-opts) :fan-out)))
    (is (contains? (run-payload (assoc base-opts :fan-out [{:name :x}])) :fan-out))))

(deftest fan-out-on-a-fhir-run-plans-nothing
  (testing "fan-out is a filter over an ER7 stream; a `--emit fhir` run
            has no message vector to filter, and the key is silently
            inert rather than an error -- the same treatment
            `:site-profile` already gets there"
    (let [r (run/run-command (assoc base-opts :emit "fhir" :fan-out table))]
      (is (result/ok? r))
      (is (not (contains? (:payload r) :fan-out))))))

(deftest run-command-config-file-passthrough-carries-fan-out
  (testing ":fan-out rides `:config` exactly as `:latency`,
            `:site-profile`, `:chatter`, `:charges`, `:ladders` and
            `:siu` do (ADR-0175 design (f)) -- no flag of its own"
    (let [path (str (System/getProperty "java.io.tmpdir") "/ehrt-fan-out-config-test.edn")]
      (try
        (spit path (pr-str {:fan-out [{:name :adt-feed
                                       :filter {:message-types #{"ADT^A01"}}
                                       :msh {:receiving-app "ADT-CONSUMER"}}]
                            :persons {:count 24 :years 20}
                            :encounters true}))
        (let [r (run/run-command {:seed 202 :patients 12 :churn true :emit "hl7" :config path})]
          (is (result/ok? r))
          (let [[sub] (:fan-out (:payload r))]
            (is (= :adt-feed (:name sub)))
            (is (pos? (:count sub)))
            (is (empty? (law-violations (:messages (:payload r)) sub)))))
        (finally (.delete (java.io.File. path)))))))
