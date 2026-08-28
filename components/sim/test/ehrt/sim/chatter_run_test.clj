(ns ehrt.sim.chatter-run-test
  "ARC 4 SWEEP 2 (`notes/adr/0175-arc-4-emission-add-ons.md` design (a),
  rulings B1/C1/E1, landed DARK): `:chatter` as a `:config` passenger,
  at POPULATION scale.

  This is the half `ehrt.sim-emit-hl7.chatter-test` cannot reach. That
  namespace's log is hand-built, because `components/sim-emit-hl7` may
  not depend on `components/sim`; only a real `run-command` with
  `:persons` produces `:demographic-update` and `:coverage-change` at
  all, and only a real population shows what the A08/A31 split actually
  looks like in the modelled world -- which ADR-0175 section 2(a) says
  is ~99.5% A31 on the event-driven side, with the A08 volume coming
  from the PERIODIC half instead.

  GROUND TRUTH DOES NOT MOVE, and that is asserted here rather than
  argued: the same run with and without `:chatter` produces the
  identical `:ground-truth`. `bin/ground-truth-bracket` makes the same
  claim across all 36 engine-layer oracle roots per commit; this gate
  makes it per test run, without a worktree."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]
            [ehrt.sim.run :as run]))

(def ^:private base-opts
  "Small, fast, and REPRESENTATIVE of what the six gated corpora are:
  `:persons` (the only producer of the two demographic kinds),
  `:encounters` (the only thing that mints the `:encounter-id` the
  A08-vs-A31 rule reads and the periodic census groups on), and enough
  arrivals that both sides of the split have a population."
  {:seed 202 :patients 12 :churn true
   :persons {:count 24 :years 20}
   :encounters true
   :emit "hl7"})

(def ^:private chatter-profile
  {:demographic-update 1.0
   :coverage-change 1.0
   :registered 1.0
   :restatement {:rate-per-patient-day 1.0}})

(defn- run-payload [opts] (:payload (run/run-command opts)))

(defn- msh-9 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 8))
(defn- msh-10 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 9))
(defn- segments [m] (mapv #(first (str/split % #"\|")) (str/split m #"\r")))
(def ^:private add-on? (comp #{"ADT^A08" "ADT^A31" "ADT^A28"} msh-9))

(deftest chatter-is-emission-only-and-ground-truth-does-not-move
  (let [without (run-payload (dissoc base-opts :emit))
        with (run-payload (assoc (dissoc base-opts :emit) :chatter chatter-profile))]
    (is (seq (:ground-truth without)))
    (is (= (:ground-truth without) (:ground-truth with))
        "the whole claim of arc 4: an emission add-on moves no ground truth")
    (testing "and the key provably cannot reach the engine at all"
      (is (not (contains? (set engine/config-keys) :chatter))))))

(deftest chatter-absent-renders-the-byte-identical-stream
  (let [without (run-payload base-opts)
        with (run-payload (assoc base-opts :chatter chatter-profile))]
    (is (pos? (count (:messages without))))
    (is (= (:messages without) (filterv (complement add-on?) (:messages with)))
        "every non-chatter message is byte-equal AND in the same
         position -- turning chatter on adds messages and moves none")))

(deftest chatter-witness-table-at-population-scale
  (let [{:keys [ground-truth messages]} (run-payload (assoc base-opts :chatter chatter-profile))
        plan (emit-hl7/plan-chatter (engine/stream (:seed base-opts) :emission 1)
                                    ground-truth chatter-profile)
        {periodic true event-driven false} (group-by (comp boolean :periodic?) plan)
        by-type (frequencies (map msh-9 messages))
        in1-only (filterv #(and (= "ADT^A31" (msh-9 %))
                                (some #{"IN1"} (segments %)))
                          messages)]
    (testing "every family design (a) names has a NON-EMPTY population
              here -- `rulings.md#R-empty-population-is-red`: an
              assertion over nothing proves nothing"
      (is (pos? (get by-type "ADT^A08" 0)))
      (is (pos? (get by-type "ADT^A31" 0)))
      (is (pos? (get by-type "ADT^A28" 0)))
      (is (pos? (count in1-only))))

    (testing "the A08 volume comes from the PERIODIC half, counted
              SEPARATELY from the event-driven half -- ADR-0175 section
              2(a) names an event-driven-only reading of an A08 witness
              as the miss"
      (is (pos? (count periodic)))
      (is (pos? (count event-driven)))
      (is (= #{"A08"} (set (map :trigger periodic))))
      (is (< (count (filter #(= "A08" (:trigger %)) event-driven))
             (count periodic))
          "measured, not assumed: demographic churn happens almost
           entirely BETWEEN encounters, so the event-driven half is
           nearly all A31 and the A08s come from the census"))

    (testing "and the whole stream's MSH-10s stay distinct"
      (is (= (count messages) (count (distinct (map msh-10 messages))))))))

(deftest chatter-and-latency-are-independent-and-decorrelated-streams
  (testing "adding chatter shifts no latency offset. This half is true
            BY CONSTRUCTION -- each planner is handed its own
            `java.util.Random`, so neither can consume the other's
            draws whatever id-tag it holds -- and it is asserted anyway
            because the construction is what a future refactor would
            break."
    (let [latency {:admission {:from-minutes 10 :to-minutes 90}
                   :discharge {:from-minutes 5 :to-minutes 30}}
          without (run-payload (assoc base-opts :latency latency))
          with (run-payload (assoc base-opts :latency latency :chatter chatter-profile))]
      (is (pos? (count (:messages without))))
      (is (= (:messages without) (filterv (complement add-on?) (:messages with))))))

  (testing "WHAT THE ID-TAG ACTUALLY BUYS IS DECORRELATION, and this is
            the assertion that can fail. Latency holds `:emission` 0 and
            chatter holds 1 (ADR-0171 ruling C1's own reserved tag).
            Give chatter tag 0 and nothing is DISTURBED -- the two
            planners simply replay the identical draw sequence, which is
            the correlation ADR-0171 says one `mix64` exists to remove.
            A test that only asserted the paragraph above would pass
            under that mistake: probed directly, re-pointing chatter to
            tag 0 leaves every assertion above green."
    (let [draws (fn [tag] (let [rng (engine/stream (:seed base-opts) :emission tag)]
                            (vec (repeatedly 20 #(.nextDouble ^java.util.Random rng)))))]
      (is (not= (draws 0) (draws 1)))
      (is (= (draws 1) (draws 1)) "and each tag is itself deterministic"))))

(deftest a-malformed-chatter-profile-is-rejected-before-the-engine-starts
  (doseq [[label bad] [["a non-numeric rate" {:demographic-update "always"}]
                       ["a rate above 1" {:coverage-change 1.5}]
                       ["a misspelled key" {:demographic-updates 1.0}]
                       ["a restatement with no rate" {:restatement {}}]]]
    (testing label
      (let [r (run/run-command (assoc base-opts :chatter bad))]
        (is (result/error? r))
        (is (= :invalid-chatter (:category r)))))))

(deftest run-command-config-file-passthrough-carries-chatter
  (testing ":chatter rides `:config` exactly as `:latency` and
            `:site-profile` do (ADR-0175 ruling C1) -- no flag of its own"
    (let [path (str (System/getProperty "java.io.tmpdir") "/ehrt-chatter-config-test.edn")]
      (try
        (spit path (pr-str {:chatter chatter-profile
                            :persons {:count 24 :years 20}
                            :encounters true}))
        (let [r (run/run-command {:seed 202 :patients 12 :churn true :emit "hl7" :config path})]
          (is (result/ok? r))
          (is (some add-on? (:messages (:payload r)))))
        (finally (.delete (java.io.File. path)))))))
