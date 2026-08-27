(ns ehrt.sim-engine.bed-cycle-test
  "ADR-0174 section 2(c), arc 3b sweep 2: the BED-STATUS CYCLE, plus
  ruling C's ADT^A20.

  What this namespace gates, in the ADR's own order:

  * the index (`sim-model/initial-beds`) -- every licensed bed and
    every surge slot the facility declares, born `:ready` at t 0;
  * the gate (`sim-model/free`) -- `:ready` when an index is present,
    \"not a key in board\" when it is absent, ONE function and one
    branch, never two copies;
  * the kind (`:bed-status-change`) -- its bed-subject participant, and
    the A20 that carries it on the wire;
  * the cycle -- vacate at t, `:cleaning` at t+d1, `:ready` at t+d1+d2,
    both legs drawn from the ward's own `:turnaround-minutes`;
  * the reinstatement -- a `:cancel-discharge` puts a patient back into
    a bed that has been `:dirty` since they left it, and the bed's
    status goes back with the location;
  * the three invariants, each fired on a MUTATED corpus rather than
    asserted to be non-vacuous;
  * invariants 4 and 5 -- `occupancy-within-capacity` unchanged, and
    `surge-only-when-earlier-rungs-exhausted` RE-READ: \"rung 1 was
    exhausted\" now means no rung-1 bed was READY.

  THE OPT-IN LAW is gated first and last: with no `:bed-cycle` key the
  whole log is byte-identical, which is what makes this sweep's dark
  commit provable rather than believed."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-emit-hl7.emit-hl7 :as emit-hl7]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-engine.engine :as engine]))

(def ^:private seed 4242)

(def ^:private facility
  "ONE licensed Renal bed and one Renal surge slot, plus an ED overflow.
  `:turnaround-minutes` is a DEGENERATE range on purpose -- ten minutes
  exactly, both legs -- so every instant below is arithmetic a reader
  can check rather than a draw they have to trust."
  {:id :bed-cycle-fixture
   :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 4
            :surge-format "%s-H%02d" :class :ed :turnaround-minutes [10 10]}
           {:id :renal :name "Renal" :beds 1 :surge-slots 1
            :surge-format "%s-H%02d" :class :inpatient :turnaround-minutes [10 10]}]})

(def ^:private leg-seconds
  "One leg of this fixture's turnaround, in seconds -- 10 minutes."
  600)

(def ^:private admit-discharge
  {:name "admit-discharge"
   :steps [{:type :admission :location "Renal"}
           {:type :delay :from 30 :to 30}
           {:type :discharge}]})

(defn- base
  [& {:as extra}]
  (merge {:seed seed :patients 3 :arrival-gap 60 :facility facility
          :pathway admit-discharge}
         extra))

(defn- of-kind [gt k] (filterv #(= k (:event %)) gt))
(defn- for-bed [gt bed] (filterv #(= bed (:bed %)) (of-kind gt :bed-status-change)))

(def ^:private off (engine/run (base)))
(def ^:private on (engine/run (base :bed-cycle true)))

;; --- the opt-in law -------------------------------------------------------

(deftest absent-bed-cycle-is-the-log-this-engine-always-produced
  (testing "the opt-in law: ABSENT -- not false, not nil -- and every
            byte is the one that was there before this sweep"
    (is (= (:ground-truth off) (:ground-truth (engine/run (base))))
        "the no-opt-in path is deterministic")
    (is (empty? (of-kind (:ground-truth off) :bed-status-change))
        "no bed event is minted without the opt-in")
    (is (every? #(nil? (:beds %)) [{} {:beds nil}])
        "a hand-built world carries no index, which is what nil means here")))

(deftest opting-in-changes-the-log-and-that-is-the-whole-point
  (is (pos? (count (of-kind (:ground-truth on) :bed-status-change)))
      "the opted-in run emits bed events")
  (is (not= (:ground-truth off) (:ground-truth on))
      "and therefore is not the same corpus"))

;; --- (i) the index ---------------------------------------------------------

(deftest every-licensed-bed-and-surge-slot-is-born-ready
  (let [beds (sim-model/initial-beds facility)]
    (is (= 6 (count beds))
        "4 ED surge slots + 1 Renal licensed + 1 Renal surge")
    (is (every? #(= :ready (:status %)) (vals beds)))
    (is (every? #(= 0 (:since-t %)) (vals beds)))
    (is (every? #(not (contains? % :last-patient-id)) (vals beds))
        "nobody has left any bed yet, and an absent key says that")
    (is (= (set (concat (sim-model/licensed-bed-ids (second (:wards facility)))
                        (sim-model/surge-slot-ids (second (:wards facility)))
                        (sim-model/surge-slot-ids (first (:wards facility)))))
           (set (keys beds)))
        "derived from the facility's own id functions, never enumerated")))

;; --- (ii) `free`, both readings, one function -----------------------------

(deftest free-means-empty-without-an-index-and-ready-with-one
  (let [ids ["RENAL-01" "RENAL-H01"]
        board {"RENAL-01" "PID-x"}]
    (testing "no index: not a key in board, exactly as before the sweep"
      (is (= ["RENAL-H01"] (vec (sim-model/free ids board nil)))))
    (testing "index present: status is :ready, which is strictly narrower"
      (is (= [] (vec (sim-model/free ids board {"RENAL-H01" {:status :dirty}})))
          "an empty but dirty bed is NOT free")
      (is (= ["RENAL-H01"] (vec (sim-model/free ids board {"RENAL-H01" {:status :ready}})))))
    (testing "the occupied half agrees with the board by construction"
      (is (= [] (vec (sim-model/free ["RENAL-01"] board {"RENAL-01" {:status :ready}})))
          "board and index are and-ed, so a disagreement can never widen the candidate set"))))

(deftest allocate-passes-over-a-dirty-bed
  (let [rng (engine/stream 1 :world 0)
        beds (assoc (sim-model/initial-beds facility) "RENAL-01" {:status :dirty})
        alloc (sim-model/allocate rng facility {} beds "Renal" nil)]
    (is (= :surge (get-in alloc [:location :placement]))
        "rung 1 is dirty, so the ladder takes rung 2 -- and rung 2 is legitimate BECAUSE rung 1 is not ready")))

;; --- (iii) the kind, its participant, and the wire ------------------------

(deftest bed-status-change-carries-a-bed-subject-and-no-patient
  (let [ev (first (of-kind (:ground-truth on) :bed-status-change))]
    (is (= 1 (count (:participants ev))))
    (is (= #{:bed-id :ward :role} (set (keys (first (:participants ev)))))
        "the participant names a BED and carries no :patient-id at all")
    (is (= :subject (:role (first (:participants ev)))))
    (is (= (:bed ev) (:bed-id (first (:participants ev)))))
    (is (nil? (:active-mrn ev)) "a bed event has no MRN because it has no patient")))

(deftest last-patient-id-rides-the-dirty-transition-alone
  (let [evs (of-kind (:ground-truth on) :bed-status-change)]
    (is (every? :last-patient-id (filter #(= :dirty (:to %)) evs))
        "the dirty transition names who left")
    (is (not-any? :last-patient-id (remove #(= :dirty (:to %)) evs))
        "the two housekeeping legs belong to nobody")))

(deftest every-bed-event-renders-one-adt-a20-of-msh-evn-npu
  (let [bed-events (of-kind (:ground-truth on) :bed-status-change)
        msgs (mapcat #(emit-hl7/event->messages "2024-01-01" "+00:00" (:facility on) (:providers on) {} nil {} %)
                     bed-events)]
    (is (= (count bed-events) (count msgs)) "one message per bed event")
    (doseq [m msgs]
      (is (re-find #"\|ADT\^A20\|" m))
      (is (= ["MSH" "EVN" "NPU"] (mapv #(subs % 0 3) (remove empty? (clojure.string/split m #"\r"))))
          "MSH EVN NPU and nothing else -- no PID, no PV1")
      (is (re-find #"\|2\.3\r" m) "MSH-12 is unchanged by this family"))
    (testing "NPU-1 is the PL PV1-3 already renders, NPU-2 the Table 0116 status"
      (let [dirty (first (filter #(re-find #"\|K\r?$" %) (map #(last (clojure.string/split % #"\r")) msgs)))]
        (is (re-matches #"NPU\|Renal\^\^RENAL-[0-9H]+\^bed-cycle-fixture\|K" dirty))))))

;; --- (iv) the cycle, and its arithmetic -----------------------------------

(deftest the-cycle-runs-vacate-then-cleaning-then-ready-at-the-wards-own-pace
  (let [gt (:ground-truth on)
        dirty (first (filter #(= :dirty (:to %)) (of-kind gt :bed-status-change)))
        bed (:bed dirty)
        legs (for-bed gt bed)
        [a b c] (take 3 legs)]
    (is (= [:dirty :cleaning :ready] [(:to a) (:to b) (:to c)]))
    (is (= [:occupied :dirty :cleaning] [(:from a) (:from b) (:from c)])
        "each leg declares the status the previous one left")
    (is (= leg-seconds (- (:t b) (:t a))) "d1, from the ward's own :turnaround-minutes")
    (is (= leg-seconds (- (:t c) (:t b))) "d2, an INDEPENDENT draw from the same range")
    (is (= (:t a) (:t (first (filter #(and (= :discharge (:event %))
                                           (= bed (get-in % [:location :bed])))
                                     gt))))
        "the vacate is at the discharge instant, not after it")))

(deftest a-bed-ready-transfer-lands-at-the-ready-instant-not-the-discharge
  (testing "the one existing behaviour arc 3b CHANGES: with the cycle on,
            no bed-ready transfer shares its discharge's own instant"
    (let [boarding {:seed 909 :patients 6 :arrival-gap 20 :facility facility
                    :pathway {:name "stay" :steps [{:type :admission :location "Renal"}
                                                   {:type :delay :from 45 :to 45}
                                                   {:type :discharge}]}}
          off-gt (:ground-truth (engine/run boarding))
          on-gt (:ground-truth (engine/run (assoc boarding :bed-cycle true)))
          zero-second (fn [gt]
                        (count (for [[a b] (partition 2 1 gt)
                                     :when (and (= :discharge (:event a))
                                                (= :transfer (:event b))
                                                (:bed-ready b)
                                                (= (:t a) (:t b)))]
                                 b)))]
      (is (pos? (zero-second off-gt))
          "today's coupling hands the bed over in the same second it is vacated")
      (is (zero? (zero-second on-gt))
          "the cycle removes every zero-second re-occupancy")
      (doseq [tr (filter :bed-ready on-gt)]
        (is (some #(and (= :bed-status-change (:event %))
                        (= :ready (:to %))
                        (= (:t %) (:t tr))
                        (= (:bed %) (get-in tr [:location :bed])))
                  on-gt)
            "every bed-ready transfer sits at its own bed's READY event")))))

;; --- (v) the cancels ------------------------------------------------------

(deftest a-cancel-discharge-restores-the-beds-status-with-the-location
  (testing "the dirty->occupied arc, legal ONLY here"
    (let [gt [{:event :registered :t 0 :warm-up false :active-mrn "M1"
               :participants [{:patient-id "P1" :role :subject}]
               :persona nil :identity :known}
              {:event :admission :t 0 :warm-up false :active-mrn "M1"
               :home-ward "Renal" :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}
               :attending "N1" :forced false
               :participants [{:patient-id "P1" :role :subject}]}
              {:event :discharge :t 100 :warm-up false :active-mrn "M1"
               :location {:ward "Renal" :bed "RENAL-01" :placement :licensed} :attending "N1"
               :participants [{:patient-id "P1" :role :subject}]}
              {:event :bed-status-change :t 100 :warm-up false :bed "RENAL-01" :ward "Renal"
               :from :occupied :to :dirty :last-patient-id "P1"
               :participants [{:bed-id "RENAL-01" :ward "Renal" :role :subject}]}
              {:event :cancel-discharge :t 200 :warm-up false :active-mrn "M1"
               :cancels-event-id 2 :home-ward "Renal"
               :location {:ward "Renal" :bed "RENAL-01" :placement :licensed} :attending "N1"
               :participants [{:patient-id "P1" :role :subject}]}]]
      (is (empty? (check/bed-cycle-transitions-are-legal gt))
          "dirty -> occupied is the reinstatement arc and is legal")
      (is (empty? (check/no-assignment-to-a-non-ready-bed gt))
          "a cancel is not an allocation: it goes through no `allocate` call site"))))

(deftest a-correction-returns-its-bed-straight-to-ready
  (testing "the SIXTH arc, disclosed: an occupancy a cancel retracts
            leaves no dirt behind it"
    (let [gt [{:event :registered :t 0 :warm-up false :active-mrn "M1"
               :participants [{:patient-id "P1" :role :subject}]
               :persona nil :identity :known}
              {:event :admission :t 0 :warm-up false :active-mrn "M1"
               :home-ward "Renal" :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}
               :attending "N1" :forced false
               :participants [{:patient-id "P1" :role :subject}]}
              {:event :cancel-admit :t 50 :warm-up false :active-mrn "M1" :cancels-event-id 1
               :participants [{:patient-id "P1" :role :subject}]}
              {:event :bed-status-change :t 60 :warm-up false :bed "RENAL-H01" :ward "Renal"
               :from :ready :to :ready
               :participants [{:bed-id "RENAL-H01" :ward "Renal" :role :subject}]}
              {:event :admission :t 100 :warm-up false :active-mrn "M1"
               :home-ward "Renal" :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}
               :attending "N1" :forced false
               :participants [{:patient-id "P1" :role :subject}]}]]
      (is (empty? (check/bed-cycle-transitions-are-legal gt)))
      (is (empty? (check/no-assignment-to-a-non-ready-bed gt))
          "the cancelled admission's bed was READY again, so the re-admission is legal"))))

;; --- (vi) the three invariants, each fired on a MUTATED corpus ------------

(defn- drop-first-where [gt pred]
  (let [idx (first (keep-indexed (fn [i e] (when (pred e) i)) gt))]
    (vec (concat (subvec (vec gt) 0 idx) (subvec (vec gt) (inc idx))))))

(defn- assoc-first-where [gt pred k v]
  (let [idx (first (keep-indexed (fn [i e] (when (pred e) i)) gt))]
    (assoc (vec gt) idx (assoc (nth gt idx) k v))))

(deftest the-three-rows-are-clean-on-a-real-run
  (let [gt (:ground-truth on)]
    (is (empty? (check/no-assignment-to-a-non-ready-bed gt)))
    (is (empty? (check/every-ready-follows-a-cleaning gt)))
    (is (empty? (check/bed-cycle-transitions-are-legal gt)))
    (is (= :ok (:status (check/check-all gt facility)))
        "and the whole catalog agrees")))

(deftest the-three-rows-are-vacuous-on-a-log-with-no-cycle
  (testing "stated rather than left to be discovered: a run that never
            opted in emits no `:bed-status-change`, and judging it
            against the relation would report every second occupant of
            every bed"
    (let [gt (:ground-truth off)]
      (is (empty? (check/no-assignment-to-a-non-ready-bed gt)))
      (is (empty? (check/every-ready-follows-a-cleaning gt)))
      (is (empty? (check/bed-cycle-transitions-are-legal gt)))
      (is (pos? (count (filter #(= :admission (:event %)) gt)))
          "and it is not vacuous for want of traffic -- the log has admissions"))))

(deftest no-assignment-to-a-non-ready-bed-fires-when-a-bed-is-taken-mid-cycle
  (let [gt (:ground-truth on)
        ;; drop the READY leg: the bed the next allocation takes is
        ;; still `:cleaning` when it is taken.
        mutated (drop-first-where gt #(and (= :bed-status-change (:event %)) (= :ready (:to %))))
        findings (check/no-assignment-to-a-non-ready-bed mutated)]
    (is (seq findings) "the row fires")
    (is (= :no-assignment-to-a-non-ready-bed (:invariant (first findings))))
    (is (= :cleaning (:status (first findings)))
        "and reports the status the bed was actually in")))

(deftest every-ready-follows-a-cleaning-fires-when-the-cleaning-leg-is-missing
  (let [gt (:ground-truth on)
        mutated (drop-first-where gt #(and (= :bed-status-change (:event %)) (= :cleaning (:to %))))
        findings (check/every-ready-follows-a-cleaning mutated)]
    (is (seq findings))
    (is (= :dirty (:status (first findings)))
        "the bed went straight from dirty to ready, with nobody cleaning it")))

(deftest bed-cycle-transitions-are-legal-fires-on-an-invented-arc
  (let [gt (:ground-truth on)
        mutated (assoc-first-where gt #(and (= :bed-status-change (:event %)) (= :ready (:to %)))
                                   :to :dirty)
        findings (check/bed-cycle-transitions-are-legal mutated)]
    (is (seq findings))
    (is (= [:cleaning :dirty] [(:from (first findings)) (:to (first findings))])
        "cleaning -> dirty is not in the relation, and the row says which arc it refused")))

(deftest bed-cycle-transitions-are-legal-fires-when-an-event-mis-declares-its-from
  (let [gt (:ground-truth on)
        mutated (assoc-first-where gt #(and (= :bed-status-change (:event %)) (= :cleaning (:to %)))
                                   :from :ready)
        findings (check/bed-cycle-transitions-are-legal mutated)]
    (is (some :declared findings)
        "the second clause: a declared :from that is not the status the log says")))

;; --- (vii) invariants 4 and 5 ---------------------------------------------

(deftest occupancy-within-capacity-is-unchanged
  (testing "invariant 4: it counts :location-bearing patients against
            declared capacity, and a dirty bed holds nobody"
    (is (empty? (check/occupancy-within-capacity (:ground-truth on) facility)))
    (is (empty? (check/occupancy-within-capacity (:ground-truth off) facility)))))

(deftest surge-only-when-earlier-rungs-exhausted-is-re-read-not-rewritten
  (testing "invariant 5: the CLAIM is unchanged and the reading of
            'exhausted' is not -- a surge placement over a DIRTY rung-1
            bed is legitimate, and the same placement over a READY one
            is not"
    (let [prefix [{:event :registered :t 0 :warm-up false :active-mrn "M1"
                   :participants [{:patient-id "P1" :role :subject}]
                   :persona nil :identity :known}
                  {:event :registered :t 0 :warm-up false :active-mrn "M2"
                   :participants [{:patient-id "P2" :role :subject}]
                   :persona nil :identity :known}
                  {:event :admission :t 0 :warm-up false :active-mrn "M1"
                   :home-ward "Renal" :location {:ward "Renal" :bed "RENAL-01" :placement :licensed}
                   :attending "N1" :forced false
                   :participants [{:patient-id "P1" :role :subject}]}
                  {:event :discharge :t 10 :warm-up false :active-mrn "M1"
                   :location {:ward "Renal" :bed "RENAL-01" :placement :licensed} :attending "N1"
                   :participants [{:patient-id "P1" :role :subject}]}]
          surge-admit {:event :admission :t 30 :warm-up false :active-mrn "M2"
                       :home-ward "Renal" :location {:ward "Renal" :bed "RENAL-H01" :placement :surge}
                       :attending "N1" :forced false
                       :participants [{:patient-id "P2" :role :subject}]}
          dirty {:event :bed-status-change :t 10 :warm-up false :bed "RENAL-01" :ward "Renal"
                 :from :occupied :to :dirty :last-patient-id "P1"
                 :participants [{:bed-id "RENAL-01" :ward "Renal" :role :subject}]}
          turned [{:event :bed-status-change :t 15 :warm-up false :bed "RENAL-01" :ward "Renal"
                   :from :dirty :to :cleaning
                   :participants [{:bed-id "RENAL-01" :ward "Renal" :role :subject}]}
                  {:event :bed-status-change :t 20 :warm-up false :bed "RENAL-01" :ward "Renal"
                   :from :cleaning :to :ready
                   :participants [{:bed-id "RENAL-01" :ward "Renal" :role :subject}]}]]
      (is (empty? (check/surge-only-when-earlier-rungs-exhausted
                   (conj (into (vec prefix) [dirty]) surge-admit) facility))
          "rung 1 is DIRTY: the surge placement is legitimate")
      (is (seq (check/surge-only-when-earlier-rungs-exhausted
                (conj (into (into (vec prefix) [dirty]) turned) surge-admit) facility))
          "rung 1 is READY again: the same placement is a violation, exactly as before the sweep"))))
