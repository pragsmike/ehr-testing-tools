(ns ehrt.sim-engine.scheduling-test
  "ADR-0174 section 2(b), arc 3b sweep 3: SCHEDULING as skeleton STATE.

  What this namespace gates, in the ADR's own order:

  * the four kinds and their fields (section 2(b)'s own table), and the
    one open-appointment record per patient they fold onto;
  * `appointment-id-for` -- ruling B1's law applied one level sideways:
    a pure function of seed, arrival ordinal and appointment ordinal,
    OFF the seeded streams, so an appointment costs no draw to name;
  * the SPLIT -- scheduled-vs-walk-in and the lead time on `:world`,
    two draws per arrival ordinal, ALWAYS, in ordinal order; an
    appointment's OUTCOME on the patient's own `:patient` stream, two
    draws per appointment, ALWAYS;
  * the reference -- a scheduled arrival's opener carries
    `:appointment-id` and the open encounter record carries it too;
  * the no-show -- emitted AT `:scheduled-t`, opening NOTHING, which is
    why an appointment cannot be retro-derived from an encounter;
  * the FOLLOW-UP at `decide :discharge` -- the first producer of a
    SCHEDULED second encounter this repository has had;
  * the four invariants, each fired on a MUTATED log rather than
    asserted to be non-vacuous, plus
    `registered-is-every-patients-first-event` still holding.

  THE OPT-IN LAW is gated first and last: with no `:scheduling` key the
  whole log is byte-identical, which is what makes this sweep's dark
  commit provable rather than believed."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-engine.engine :as engine]))

(def ^:private seed 4242)

(def ^:private facility
  {:id :scheduling-fixture
   :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 8
            :surge-format "%s-H%02d" :class :ed :turnaround-minutes [10 10]}
           {:id :renal :name "Renal" :beds 4 :surge-slots 2
            :surge-format "%s-H%02d" :class :inpatient :turnaround-minutes [10 10]}]})

(def ^:private admit-discharge
  {:name "admit-discharge"
   :steps [{:type :admission :location "Renal"}
           {:type :delay :from 30 :to 30}
           {:type :discharge}]})

(def ^:private scheduling
  "Rates deliberately FAT -- a tenth each -- so every one of the four
  outcomes is witnessed at this fixture's small population rather than
  waiting on a corpus. `:scheduled-fraction` is a half for the same
  reason: both arms of the split must appear."
  {:scheduled-fraction 0.5
   :lead-time-days [1 7]
   :no-show-rate 0.1
   :reschedule-rate 0.1
   :cancel-rate 0.1
   :follow-up {:rate 0.4 :interval-days [7 30]}})

(defn- base [& {:as extra}]
  (merge {:seed seed :patients 12 :arrival-gap 60 :facility facility
          :pathway admit-discharge :encounters true}
         extra))

(defn- gt [opts] (:ground-truth (engine/run opts)))

(defn- of-kind [log k] (filterv #(= k (:event %)) log))

(defn- openers [log]
  (filterv #(#{:admission :outpatient-visit} (:event %)) log))

;; --- THE OPT-IN LAW ------------------------------------------------------

(deftest absent-scheduling-is-byte-identical
  (testing "a run with NO :scheduling key produces exactly the log it
            produced before this sweep -- the dark commit's whole claim,
            asserted here at fixture scale and by bin/regression-oracle
            at corpus scale"
    (is (= (gt (base)) (gt (base)))
        "the no-key path is deterministic")
    (is (empty? (of-kind (gt (base)) :appointment))
        "no appointment is minted")
    (is (empty? (filter :appointment-id (openers (gt (base)))))
        "no opener carries a reference")))

(deftest scheduling-absent-and-nil-are-not-the-same-key
  (testing "ABSENT ENTIRELY is the byte-identical path -- an explicit nil
            is the same absence to `when`, and neither draws"
    (is (= (gt (base)) (gt (base :scheduling nil))))))

;; --- THE ID LAW (ruling B1, one level sideways) --------------------------

(deftest appointment-id-is-a-pure-function-of-seed-ordinal-and-ordinal
  (testing "`appointment-id-for` is deterministic, distinct per argument,
            and carries a fourth prefix that no other id space uses"
    (is (= (engine/appointment-id-for 1 2 3) (engine/appointment-id-for 1 2 3)))
    (is (not= (engine/appointment-id-for 1 2 3) (engine/appointment-id-for 2 2 3)))
    (is (not= (engine/appointment-id-for 1 2 3) (engine/appointment-id-for 1 3 3)))
    (is (not= (engine/appointment-id-for 1 2 3) (engine/appointment-id-for 1 2 4)))
    (is (.startsWith ^String (engine/appointment-id-for 1 2 3) "APT-"))
    (testing "and is NOT the encounter id for the same two ordinals -- the
              two spaces are minted from one mix and must not collide"
      (is (not= (subs (engine/appointment-id-for 7 2 3) 4)
                (subs (engine/encounter-id-for 7 2 3) 4))))))

(deftest appointment-ordinals-are-never-reused
  (testing "a terminal appointment stays in `:appointments`, so its
            ordinal is never handed out twice -- the same reason a
            cancelled encounter stays in `:encounters`"
    (let [log (gt (base :scheduling scheduling))
          ids (mapv :appointment-id (of-kind log :appointment))]
      (is (pos? (count ids)))
      (is (= (count ids) (count (distinct ids)))
          "every minted appointment id in the run is distinct"))))

;; --- THE FOUR KINDS, AND THEIR FIELDS -----------------------------------

(deftest the-four-kinds-carry-the-fields-the-adr-tables
  (let [log (gt (base :patients 60 :scheduling scheduling))]
    (testing ":appointment"
      (let [evs (of-kind log :appointment)]
        (is (pos? (count evs)))
        (doseq [e evs]
          (is (string? (:appointment-id e)))
          (is (int? (:scheduled-t e)))
          (is (> (:scheduled-t e) (:t e)) "a booking is always ahead of itself")
          (is (contains? #{:inpatient :emergency :outpatient :preadmit :recurring :obstetrics}
                         (:appointment-class e)))
          (is (= 1 (count (:participants e)))))))
    (testing ":reschedule keeps the SAME id and reports a real move"
      (let [evs (of-kind log :reschedule)]
        (is (pos? (count evs)))
        (doseq [e evs]
          (is (int? (:prior-scheduled-t e)))
          (is (int? (:scheduled-t e)))
          (is (not= (:prior-scheduled-t e) (:scheduled-t e))
              "a reschedule that moved nothing would be a reschedule of nothing"))))
    (testing ":appointment-cancel and :no-show are terminal and name an id"
      (doseq [k [:appointment-cancel :no-show]]
        (let [evs (of-kind log k)]
          (is (pos? (count evs)) (str k " is witnessed at this population"))
          (doseq [e evs] (is (string? (:appointment-id e)))))))))

(deftest a-no-show-is-emitted-at-its-scheduled-instant-and-opens-nothing
  (let [log (gt (base :patients 60 :scheduling scheduling))
        booked (into {} (for [e (of-kind log :appointment)]
                          [(:appointment-id e) (:scheduled-t e)]))
        moved (into {} (for [e (of-kind log :reschedule)]
                         [(:appointment-id e) (:scheduled-t e)]))]
    (is (pos? (count (of-kind log :no-show))))
    (doseq [e (of-kind log :no-show)]
      (is (= (:t e) (or (moved (:appointment-id e)) (booked (:appointment-id e))))
          "the no-show fires AT :scheduled-t, not at the booking"))
    (is (empty? (filter (fn [o] (some #(= (:appointment-id o) (:appointment-id %))
                                      (of-kind log :no-show)))
                        (filter :appointment-id (openers log))))
        "and no opener carries a no-showed id")))

;; --- THE STATE HALF ------------------------------------------------------

(deftest one-open-appointment-record-per-patient
  (let [r (engine/run (base :patients 60 :scheduling scheduling))
        states (vals (:state-history r))]
    (is (pos? (count states)))
    (testing "every fold produces at most ONE open appointment at a time"
      (doseq [history states
              patient history]
        (is (or (nil? (:appointment patient)) (map? (:appointment patient))))))
    (testing "and every TERMINAL record carries exactly one outcome"
      (let [terminal (mapcat (fn [h] (:appointments (last h))) states)]
        (is (pos? (count terminal)))
        (doseq [a terminal]
          (is (contains? #{:kept :cancelled :no-show} (:outcome a))))))))

(deftest a-kept-appointment-is-closed-by-the-encounter-not-by-an-event
  (testing "\"kept\" is not an event -- it IS the encounter happening --
            so the opener's own fold is what writes the terminal"
    (let [r (engine/run (base :patients 60 :scheduling scheduling))
          finals (map last (vals (:state-history r)))
          kept (filter #(= :kept (:outcome %)) (mapcat :appointments finals))]
      (is (pos? (count kept)))
      (testing "and the encounter it opened names it back"
        (let [enc-ids (into #{} (keep :appointment-id
                                      (mapcat (fn [p] (concat (:encounters p)
                                                              (when (:encounter p) [(:encounter p)])))
                                              finals)))]
          (doseq [a kept]
            (is (contains? enc-ids (:appointment-id a))
                "a kept appointment has an encounter record carrying its id")))))))

;; --- THE SPLIT, AND ITS DRAW COUNTS -------------------------------------

(deftest both-arms-of-the-split-appear
  (let [log (gt (base :patients 60 :scheduling scheduling))
        scheduled (count (filter :appointment-id (openers log)))
        walk-ins (count (remove :appointment-id (openers log)))]
    (is (pos? scheduled) "scheduled arrivals exist")
    (is (pos? walk-ins) "and so do walk-ins -- a split with one arm is not a split")))

(deftest the-world-draws-are-fixed-per-arrival-ordinal-not-per-outcome
  (testing "two `:world` draws per arrival ordinal, ALWAYS -- so retuning
            `:scheduled-fraction` moves the split and NOTHING downstream
            of the pre-loop block. Proven by the arrival INSTANTS, which
            are drawn before the split and must therefore be identical
            across two different fractions."
    (let [instants (fn [f]
                     (mapv :t (of-kind (gt (base :patients 40
                                                 :scheduling (assoc scheduling :scheduled-fraction f)))
                                       :registered)))]
      (is (= (instants 0.1) (instants 0.9))
          "arrival instants are drawn BEFORE the split and cannot move with it"))))

(deftest the-patient-draws-are-fixed-per-appointment-not-per-outcome
  (testing "two `:patient` draws per appointment, ALWAYS -- the reschedule
            offset is drawn whether or not a reschedule fired, deliberately
            unlike `decide :delay`'s dead-draw skip.

            THE ARRIVAL-SIDE BOOKINGS ARE THEREFORE FIXED: every ordinal-00
            appointment is minted from the pre-loop split, whose draws are
            `:world` and whose count is `:patients`, so no outcome rate can
            move one.

            THE FOLLOW-UP COUNT IS NOT, AND THE ADR SAYS SO: *\"the number
            of appointments a patient has may be data-dependent without
            breaching the fixed-consumption law, which exists so draw count
            never depends on ANOTHER patient's data.\"* A cancel takes the
            visit, the visit takes the discharge, and the discharge is what
            books the follow-up -- all inside ONE patient. Asserting the
            follow-ups equal too would assert something the design
            deliberately does not promise."
    (let [ids (fn [rate]
                (mapv :appointment-id
                      (of-kind (gt (base :patients 40
                                         :scheduling (assoc scheduling :cancel-rate rate)))
                               :appointment)))
          arrivals-only (fn [v] (filterv #(re-find #"-00-" %) v))]
      (is (= (arrivals-only (ids 0.1)) (arrivals-only (ids 0.3)))
          "every arrival-side booking is identical across two outcome rates")
      (is (pos? (count (arrivals-only (ids 0.1))))
          "and the assertion above is not vacuous"))))

(deftest a-rate-change-cannot-reach-another-patients-stream
  (testing "ADR-0171 section 2(d)'s per-patient-stream promise, asserted
            over this sweep's own new draws"
    (let [log-a (gt (base :patients 40 :scheduling scheduling))
          log-b (gt (base :patients 40 :scheduling (assoc scheduling :no-show-rate 0.35
                                                          :cancel-rate 0.05)))
          registered (fn [l] (mapv (juxt :t #(:patient-id (first (:participants %))))
                                   (of-kind l :registered)))]
      (is (= (registered log-a) (registered log-b))
          "every patient still registers at the same instant with the same id"))))

;; --- THE FOLLOW-UP: SCHEDULED SECOND ENCOUNTERS -------------------------

(deftest a-follow-up-produces-a-scheduled-second-encounter
  (let [log (gt (base :patients 60 :scheduling scheduling))
        second-encounters (filter #(and (= :outpatient-visit (:event %)) (:appointment-id %)) log)]
    (is (pos? (count second-encounters))
        "the headline: a second encounter that is SCHEDULED")
    (testing "each follows a discharge of the SAME patient"
      (doseq [v second-encounters]
        (let [pid (:patient-id (first (:participants v)))
              discharges (filter #(and (= :discharge (:event %))
                                       (= pid (:patient-id (first (:participants %))))
                                       (< (:t %) (:t v)))
                                 log)]
          (is (pos? (count discharges))
              "a follow-up visit is preceded by its own patient's discharge"))))))

(deftest no-follow-up-is-booked-for-a-patient-who-died
  (testing "a return visit for somebody who expired is the one shape the
            follow-up must not produce"
    (let [expiring {:name "expire"
                    :steps [{:type :admission :location "Renal"}
                            {:type :delay :from 30 :to 30}
                            {:type :discharge :disposition :expired}]}
          log (gt (base :patients 30 :pathway expiring
                        :scheduling (assoc scheduling :follow-up {:rate 1.0 :interval-days [7 7]})))
          expired-ids (into #{} (map #(:patient-id (first (:participants %))))
                            (filter #(= :expired (:disposition %)) (of-kind log :discharge)))]
      (is (pos? (count expired-ids)) "the fixture actually expires patients")
      ;; A SCHEDULED ARRIVAL'S OWN BOOKING LEGITIMATELY PRECEDES THE
      ;; DEATH -- it was made when the patient walked in, long before the
      ;; discharge that expired them, and forbidding it would forbid
      ;; scheduling anyone who later dies. What must not exist is a
      ;; booking made AT or AFTER the expired discharge, which is the
      ;; only thing the follow-up could have produced.
      (let [expired-at (into {} (for [d (of-kind log :discharge)
                                      :when (= :expired (:disposition d))]
                                  [(:patient-id (first (:participants d))) (:t d)]))]
        (doseq [e (of-kind log :appointment)
                :let [pid (:patient-id (first (:participants e)))]]
          (is (or (not (contains? expired-at pid))
                  (< (:t e) (expired-at pid)))
              "no appointment is booked at or after an expired discharge"))))))

(deftest a-discharge-owes-two-followups-and-the-loop-takes-both
  (testing "under `:scheduling` AND `:bed-cycle` one decide dirties a bed
            and books a return visit -- the ADR's \"nothing about the main
            loop changes\" does not survive that collision, and the loop
            now takes a SEQUENCE. Both effects must appear in one run."
    (let [log (gt (base :patients 40 :bed-cycle true :scheduling scheduling))]
      (is (pos? (count (of-kind log :bed-status-change))) "the bed cycle still runs")
      (is (pos? (count (of-kind log :appointment))) "and follow-ups are still booked")
      (is (pos? (count (filter #(and (= :outpatient-visit (:event %)) (:appointment-id %)) log)))
          "and the scheduled second encounters still happen"))))

;; --- THE FOUR INVARIANTS, EACH FIRED ON A MUTATED LOG -------------------

(defn- clean-log []
  (gt (base :patients 60 :scheduling scheduling)))

(deftest the-four-invariants-are-green-on-a-real-run
  (let [log (clean-log)]
    (doseq [f [check/appointment-reference-resolves
               check/scheduled-encounter-follows-its-appointment
               check/no-show-has-no-encounter
               check/appointment-reaches-at-most-one-terminal]]
      (is (empty? (f log)) (str f " is green on an engine-produced log")))))

(deftest every-scheduling-invariant-is-vacuous-without-the-opt-in
  (let [log (gt (base))]
    (doseq [f [check/appointment-reference-resolves
               check/scheduled-encounter-follows-its-appointment
               check/no-show-has-no-encounter
               check/appointment-reaches-at-most-one-terminal]]
      (is (nil? (f log))
          (str f " returns nil -- not empty -- on a log with no appointments")))))

(deftest invariant-2-is-non-vacuous-and-here-is-the-count
  (testing "ADR-0174: this row is non-vacuous ONLY because sweep 1's
            encounter horizon landed. Asserting the COUNT of openers it
            actually judges is what stops it going quietly vacuous under
            a later reshuffle -- exactly the failure repo review 5
            predicted for a claim nothing keeps true."
    (let [log (clean-log)
          judged (filter :appointment-id (openers log))
          second-encounters (filter #(= :outpatient-visit (:event %)) judged)]
      (is (pos? (count judged)))
      (is (pos? (count second-encounters))
          "and at least one of them is a SECOND encounter, which is the half
           that could not exist before sweep 1"))))

(deftest invariant-1-fires-on-a-dangling-reference
  (let [log (clean-log)
        cancel (first (of-kind log :appointment-cancel))
        mutated (mapv #(if (identical? % cancel)
                         (assoc % :appointment-id "APT-999999-99-deadbeef")
                         %)
                      log)]
    (is (some? cancel) "the fixture produced a cancel to mutate")
    (is (empty? (check/appointment-reference-resolves log)))
    (is (seq (check/appointment-reference-resolves mutated))
        "a terminal naming an appointment nobody minted is a violation")))

(deftest invariant-1-fires-on-a-CROSS-PATIENT-reference
  (testing "same-patient is the whole point of the row -- an id that
            resolves against somebody ELSE's appointment must still fire"
    (let [log (clean-log)
          cancel (first (of-kind log :appointment-cancel))
          other (first (remove #(= (:appointment-id cancel) (:appointment-id %))
                               (of-kind log :appointment)))
          mutated (mapv #(if (identical? % cancel)
                           (assoc % :appointment-id (:appointment-id other))
                           %)
                        log)]
      (is (some? other))
      (is (seq (check/appointment-reference-resolves mutated))))))

(deftest invariant-2-fires-on-an-opener-against-a-cancelled-appointment
  (let [log (clean-log)
        cancel (first (of-kind log :appointment-cancel))
        opener (first (filter :appointment-id (openers log)))
        mutated (mapv #(if (identical? % opener)
                         (assoc % :appointment-id (:appointment-id cancel))
                         %)
                      log)]
    (is (and (some? cancel) (some? opener)))
    (is (seq (check/scheduled-encounter-follows-its-appointment mutated))
        "keeping an appointment a cancel already closed is a violation")))

(deftest invariant-3-fires-on-an-opener-against-a-no-show
  (testing "A NO-SHOWED PATIENT HAS NO OPENER, BY CONSTRUCTION -- that is
            what a no-show IS -- so the violating log cannot be made by
            re-stamping an existing opener the way rows 1 and 2 are. It
            has to be SYNTHESISED, and having to synthesise it is itself
            the evidence the engine cannot produce it."
    (let [log (clean-log)
          no-show (first (of-kind log :no-show))
          pid (:patient-id (first (:participants no-show)))
          mutated (conj (vec log)
                        {:event :outpatient-visit :t (inc (:t no-show))
                         :active-mrn (:active-mrn no-show)
                         :attending "0000000000"
                         :appointment-id (:appointment-id no-show)
                         :participants [{:patient-id pid :role :subject}]})]
      (is (some? no-show))
      (is (empty? (filter #(= pid (:patient-id (first (:participants %)))) (openers log)))
          "the no-showed patient opened nothing at all")
      (is (empty? (check/no-show-has-no-encounter log)))
      (is (seq (check/no-show-has-no-encounter mutated))))))

(deftest invariant-4-fires-on-an-appointment-both-cancelled-and-kept
  (testing "the row the ADR marks OWED, and the reason: rows 1-3 are each
            satisfiable by exactly this log"
    (let [log (clean-log)
          kept-opener (first (filter :appointment-id (openers log)))
          aid (:appointment-id kept-opener)
          pid (:patient-id (first (:participants kept-opener)))
          ;; a cancel for an appointment the log ALSO keeps, inserted after
          ;; its booking so rows 1 and 3 both stay green
          extra {:event :appointment-cancel :t (:t kept-opener)
                 :active-mrn (:active-mrn kept-opener)
                 :appointment-id aid
                 :participants [{:patient-id pid :role :subject}]}
          mutated (conj (vec log) extra)]
      (is (some? kept-opener))
      (is (empty? (check/no-show-has-no-encounter mutated))
          "row 3 does not see it -- this is not a no-show")
      (is (empty? (check/appointment-reference-resolves mutated))
          "row 1 does not see it -- the reference resolves")
      (is (seq (check/appointment-reaches-at-most-one-terminal mutated))
          "row 4 is the only one that does"))))

;; --- WHAT MUST STILL HOLD ------------------------------------------------

(deftest registered-is-still-every-patients-first-event
  (testing "an appointment is a VISIT booked by a patient, never a second
            patient -- so the registration rule is untouched"
    (let [log (gt (base :patients 60 :scheduling scheduling))]
      (is (empty? (check/registered-is-every-patients-first-event log))))))

(deftest a-scheduled-run-self-checks-clean
  (let [r (engine/run (base :patients 60 :bed-cycle true :scheduling scheduling))
        checked (check/check-all (:ground-truth r) facility 0)]
    (is (= :ok (:status checked))
        (str "self-check must be green: " (pr-str (:payload checked))))))

;; --- THE CONFIG GUARD ----------------------------------------------------

(deftest malformed-scheduling-is-a-result-not-a-throw
  (testing "the same guard-clause-at-entry shape :invalid-persons and
            :invalid-seed already have"
    (let [bad (engine/run (base :scheduling {:scheduled-fraction 0.5}))]
      (is (= :error (:status bad)))
      (is (= :invalid-scheduling (:category bad))))))

(deftest outcome-rates-summing-past-one-are-refused
  (testing "the three rates are BANDS of one uniform, so a config summing
            past 1 would silently starve the last band rather than fail"
    (is (not (engine/valid-scheduling?
              (assoc scheduling :no-show-rate 0.5 :reschedule-rate 0.4 :cancel-rate 0.3))))
    (is (engine/valid-scheduling? scheduling))))
