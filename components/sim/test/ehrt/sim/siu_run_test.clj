(ns ehrt.sim.siu-run-test
  "ARC 4 SWEEP 4 (`notes/adr/0175-arc-4-emission-add-ons.md` ruling B1,
  landed DARK): `:siu` as a `:config` passenger, at POPULATION scale.

  This is the half `ehrt.sim-emit-hl7.siu-test` cannot reach -- that
  brick may not depend on `components/sim`, so it cannot ask
  `run-command` anything, and the CONFIG SURFACE is what this namespace
  is about: a malformed `:siu` must be rejected before the engine's RNG
  starts, and the key must be provably unable to reach `engine/run` at
  all.

  GROUND TRUTH DOES NOT MOVE, asserted here rather than argued: the
  same run with and without `:siu` produces the identical
  `:ground-truth`. `bin/ground-truth-bracket` makes that claim across
  every engine-layer oracle root per commit; this makes it per test
  run, without a worktree.

  THE ASYMMETRY WORTH KNOWING BEFORE READING THE ASSERTIONS: `:siu`
  differs from every other emission key in this repository in that `{}`
  is ON. Its siblings carry the settings that make them do anything, so
  their empty map has nothing to do; here the KEY'S PRESENCE is the
  opt-in and `:triggers` only narrows it."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim.run :as run]))

(def ^:private scheduling
  "Every rate well clear of zero, so all four kinds occur. A profile that
  produced only bookings would make three quarters of this namespace
  vacuous (`rulings.md#R-empty-population-is-red`), and the family
  census below is what proves it did not."
  {:scheduled-fraction 0.7 :lead-time-days [3 21]
   :no-show-rate 0.15 :reschedule-rate 0.25 :cancel-rate 0.15
   :follow-up {:rate 0.6 :interval-days [30 120]}})

(def ^:private base-opts
  {:seed 202 :patients 24 :arrival-gap 90
   :encounters true
   :scheduling scheduling
   :emit "hl7"})

(defn- run-payload [opts] (:payload (run/run-command opts)))

(defn- msh-9 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 8))
(defn- msh-10 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 9))
(defn- siu? [m] (str/starts-with? (msh-9 m) "SIU^"))
(defn- segment-ids [m] (mapv #(first (str/split % #"\|")) (str/split m #"\r")))

(def ^:private siu-kinds #{:appointment :reschedule :appointment-cancel :no-show})

(deftest siu-is-emission-only-and-ground-truth-does-not-move
  (let [without (run-payload (dissoc base-opts :emit))
        with (run-payload (assoc (dissoc base-opts :emit) :siu {}))]
    (is (seq (:ground-truth without)))
    (is (pos? (count (filter #(siu-kinds (:event %)) (:ground-truth without))))
        "rulings.md#R-empty-population-is-red: a run with no appointment would make
         every assertion in this namespace vacuous")
    (is (= (:ground-truth without) (:ground-truth with))
        "the whole claim of arc 4: an emission add-on moves no ground truth")
    (testing "and the key provably cannot reach the engine at all -- unlike
              `:scheduling`, which is an ENGINE key and does draw"
      (is (not (contains? (set engine/config-keys) :siu)))
      (is (contains? (set engine/config-keys) :scheduling)))))

(deftest a-malformed-siu-profile-is-rejected-before-the-engine-starts
  (testing "the failure mode is the loudest of arc 4's four keys: `:trigger` for
            `:triggers` reads as `on for all four`, and a misspelled kind reads as
            `on for none`. Both would be silent."
    (is (= :invalid-siu (:category (run/run-command (assoc base-opts :siu {:trigger [:no-show]})))))
    (is (= :invalid-siu (:category (run/run-command (assoc base-opts :siu {:triggers [:noshow]})))))
    (is (= :invalid-siu (:category (run/run-command (assoc base-opts :siu {:triggers "no-show"})))))
    (is (= :invalid-siu (:category (run/run-command (assoc base-opts :siu {:triggers []}))))
        "an empty allow-list is indistinguishable in effect from `:siu` absent, and a
         reader who wrote it meant something")
    (is (= :invalid-siu (:category (run/run-command (assoc base-opts :siu {:triggers ["S26"]})))
           ) "HL7 trigger strings are not this key's vocabulary"))
  (testing "and the two valid shapes are accepted"
    (is (= :ok (:status (run/run-command (assoc base-opts :siu {})))))
    (is (= :ok (:status (run/run-command (assoc base-opts :siu {:triggers [:no-show :appointment]})))))))

(deftest siu-absent-is-byte-identical-and-siu-on-adds-exactly-the-scheduling-events
  (let [without (run-payload base-opts)
        with (run-payload (assoc base-opts :siu {}))
        siu (filterv siu? (:messages with))
        families (frequencies (map msh-9 siu))
        events (count (filter #(siu-kinds (:event %)) (:ground-truth with)))]
    (testing "sanity: all four triggers occur, so the claims below are about a
              population and not about one lucky booking"
      (is (= #{"SIU^S12" "SIU^S14" "SIU^S15" "SIU^S26"} (set (keys families)))
          (str "measured 2026-08-28 at " (pr-str base-opts) ". Got " (pr-str families))))
    (testing "absent renders none of them"
      (is (empty? (filterv siu? (:messages without)))))
    (testing "on adds exactly ONE message per scheduling event, and moves nothing else"
      (is (= events (count siu)))
      (is (= (:messages without) (filterv (complement siu?) (:messages with)))))
    (testing "an allow-list renders exactly the kinds it names"
      (let [only-no-show (run-payload (assoc base-opts :siu {:triggers [:no-show]}))]
        (is (= #{"SIU^S26"} (set (map msh-9 (filterv siu? (:messages only-no-show))))))
        (is (= (count (filter #(= :no-show (:event %)) (:ground-truth with)))
               (count (filterv siu? (:messages only-no-show)))))
        (is (= (:messages without) (filterv (complement siu?) (:messages only-no-show))))))
    (testing "MSH-10 is unique across the whole wire, SIU and ADT together"
      (is (= (count (:messages with)) (count (distinct (map msh-10 (:messages with)))))))
    (testing "and no SIU carries a PV1 -- structural, not seeded: both booking producers
              decide outside an open encounter (`emit-hl7/siu-message`'s measurement)"
      (is (every? #(= ["MSH" "SCH" "PID"] (segment-ids %)) siu)))))

(deftest a-run-that-schedules-nothing-gains-no-siu
  (let [no-scheduling (dissoc base-opts :scheduling)
        without (run-payload no-scheduling)
        with (run-payload (assoc no-scheduling :siu {}))]
    (testing "the run really books nothing -- that is the case under test"
      (is (zero? (count (filter #(siu-kinds (:event %)) (:ground-truth with))))))
    (is (pos? (count (:messages without))))
    (is (= (:messages without) (:messages with))
        "`:siu` creates no event; a corpus with no appointment is byte-identical with the
         key on, which is why a config may take it for uniformity")))

(deftest siu-rides-the-latency-clock-alongside-every-other-key
  (testing "`:siu` composes with `:latency` rather than bypassing it: an SIU takes its own
            event's offset, so turning latency on moves SIU MSH-7s and leaves the message
            count exactly where it was"
    (let [latency {:appointment {:from-minutes 5 :to-minutes 30}}
          plain (run-payload (assoc base-opts :siu {}))
          lagged (run-payload (assoc base-opts :siu {} :latency latency))]
      (is (= (count (:messages plain)) (count (:messages lagged))))
      (is (= (frequencies (map msh-9 (:messages plain)))
             (frequencies (map msh-9 (:messages lagged)))))
      (is (not= (:messages plain) (:messages lagged))
          "if these were equal the offsets never reached the SIU builder"))))
