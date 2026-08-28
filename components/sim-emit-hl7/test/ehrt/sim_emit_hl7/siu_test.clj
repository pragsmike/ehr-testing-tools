(ns ehrt.sim-emit-hl7.siu-test
  "ARC 4 SWEEP 4 (`notes/adr/0175-arc-4-emission-add-ons.md` ruling B1,
  landed DARK): scheduling's four ground-truth kinds reach the wire as
  SIU^S12/S14/S15/S26, behind `:siu`.

  THE LOG IS HAND-BUILT, for `ehrt.sim-emit-hl7.ladders-test`'s reason
  (`components/sim-emit-hl7` may not depend on `components/sim`, so no
  `run-command` is reachable from this brick) and one of its own: a
  hand-built log can carry the three cases no seed in this repository
  produces at all -- an appointment booked while an encounter is OPEN
  (measured absent from every population, see `emit-hl7/siu-message`),
  two appointments for one patient at ONE SECOND (the control-id
  collision the four-part key exists for), and a full
  S12 -> S14 -> S26 chain on one appointment id.

  THE POPULATION HALF IS BELOW IT AND IS REAL, through
  `ehrt.sim-engine.interface/run` with `:scheduling` on -- the same
  seam `ehrt.sim-emit-hl7.event-conformance-test` already uses from
  this brick. It is what makes the PV1 claim a claim about a corpus
  rather than about a fixture.

  NOTHING HERE DRAWS. `:siu` has no planner and no RNG at all -- it is
  a per-event yes/no over events the log already holds -- so there is
  no draw-consumption law to assert. What replaces it is the identity
  property: absent `:siu` is `emit`, byte for byte."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.sim-emit-hl7.emit-hl7 :as emit-hl7]
            [ehrt.sim-emit-hl7.v2-replay :as v2-replay]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-model.interface :as sim-model]))

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

(def ^:private log
  "One patient. Indices are named because the assertions below cite them:

    0 :registered            t=0
    1 :appointment    APT-A  t=100    scheduled 864000, reason present
    2 :reschedule     APT-A  t=200    864000 -> 950400
    3 :appointment    APT-B  t=200    a SECOND open booking at the SAME t
    4 :appointment-cancel  APT-B  t=300
    5 :admission             t=1000   ENC-1 opens
    6 :appointment    APT-C  t=1100   ENC-1 STILL OPEN -- carries :encounter-id
    7 :discharge             t=2000
    8 :no-show        APT-A  t=950400 at its own scheduled instant

  EVENTS 2 AND 3 SHARE AN INSTANT AND A PATIENT, which is the case the
  four-part control-id key exists for: `mrn-S14-200` and `mrn-S12-200`
  differ by trigger, but two BOOKINGS at one second would not, and the
  engine really does emit a booking and its reschedule in one batch
  (`engine/decide :appointment`: \"booking and re-booking are one
  decide\"). The appointment id is what separates them.

  EVENT 6 IS THE UNREACHABLE CASE, and it is here precisely because no
  run produces it -- see `emit-hl7/siu-message`'s own measurement. It is
  the only place in this repository where the PV1 branch is exercised.

  EVENT 8 IS THE ONLY ONE OUT OF `:t` ORDER-ADJACENCY, and it is not out
  of ORDER: the log is `:t`-nondecreasing throughout, which
  `emit-wire`'s identity property rests on."
  [(ev {:event :registered :t 0 :active-mrn "MRN000001" :persona persona-0})
   (ev {:event :appointment :t 100 :active-mrn "MRN000001"
        :appointment-id "APT-A" :scheduled-t 864000
        :appointment-class :outpatient :reason "Follow-up"})
   (ev {:event :reschedule :t 200 :active-mrn "MRN000001"
        :appointment-id "APT-A" :prior-scheduled-t 864000 :scheduled-t 950400})
   (ev {:event :appointment :t 200 :active-mrn "MRN000001"
        :appointment-id "APT-B" :scheduled-t 604800
        :appointment-class :inpatient})
   (ev {:event :appointment-cancel :t 300 :active-mrn "MRN000001"
        :appointment-id "APT-B"})
   (ev {:event :admission :t 1000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893" :home-ward "Renal"
        :reason "Acute kidney injury" :forced false})
   (ev {:event :appointment :t 1100 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :appointment-id "APT-C" :scheduled-t 1728000
        :appointment-class :outpatient})
   (ev {:event :discharge :t 2000 :active-mrn "MRN000001" :encounter-id "ENC-1"
        :location ward-location :attending "1234567893"})
   (ev {:event :no-show :t 950400 :active-mrn "MRN000001"
        :appointment-id "APT-A"})])

(def ^:private facility sim-model/default-facility)
(def ^:private providers [{:id "1234567893" :name {:family "Reyes" :given "Priya"}}])

(defn- wire
  ([siu] (wire siu {} nil))
  ([siu offsets site-profile]
   (emit-hl7/emit-wire log ref-date utc-offset facility providers site-profile offsets
                       {:siu siu})))

(defn- msh-9 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 8))
(defn- msh-10 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 9))
(defn- msh-7 [m] (nth (str/split (first (str/split m #"\r")) #"\|") 6))
(defn- segment [m nm] (first (filter #(str/starts-with? % (str nm "|")) (str/split m #"\r"))))
(defn- segment-ids [m] (mapv #(first (str/split % #"\|")) (str/split m #"\r")))
(defn- field
  "STANDARD 1-based HL7 field index, on a non-MSH segment (where the
  segment id occupies position 0 and field N is at split index N)."
  [seg n]
  (nth (str/split seg #"\|" -1) n ""))
(defn- siu? [m] (str/starts-with? (msh-9 m) "SIU^"))
(defn- siu-of [messages trigger]
  (filterv #(= (str "SIU^" trigger) (msh-9 %)) messages))

;; --- (i) the registry, and what an entry does and does not promise ------

(deftest the-four-scheduling-kinds-have-registry-entries
  (testing "the entries themselves -- the whole of what the jar settles is that
            S12/S14/S15/S26 are legal v2.4 triggers resolving to one structure"
    (is (= {:type "SIU" :trigger "S12"} (emit-hl7/message-type-registry :appointment)))
    (is (= {:type "SIU" :trigger "S14"} (emit-hl7/message-type-registry :reschedule)))
    (is (= {:type "SIU" :trigger "S15"} (emit-hl7/message-type-registry :appointment-cancel)))
    (is (= {:type "SIU" :trigger "S26"} (emit-hl7/message-type-registry :no-show))))
  (testing "S14 IS THE EVENT CONTRACT'S OWN CHOICE, pinned here so a later reading of
            HL7 Table 0003 -- which is in NO jar on any classpath in this tree -- is a
            deliberate contract change and not a quiet edit. `event-schema`'s
            `:reschedule` doc says SIU^S14 at 1.7.0; `notes/adr/0174-*.md`:697 says S13
            and is the lone surface that disagrees."
    (is (= "S14" (:trigger (emit-hl7/message-type-registry :reschedule)))))
  (testing "the kind set is DERIVED, never listed twice"
    (is (= #{:appointment :reschedule :appointment-cancel :no-show}
           emit-hl7/siu-event-kinds)))
  (testing "and all four are SKELETON families, so `gate v2` gates every SIU in FULL
            with no list for anyone to widen"
    (doseq [t ["SIU^S12" "SIU^S14" "SIU^S15" "SIU^S26"]]
      (is (contains? emit-hl7/skeleton-message-types t) t))))

(deftest an-siu-registry-entry-does-not-mean-unconditional-rendering
  (testing "every OTHER entry in the registry renders whenever its event occurs; these
            four render only when `:siu` asks. That asymmetry is the whole mechanism
            behind `absent = today byte-for-byte`, and it is asserted rather than
            described because nothing else in this namespace has it."
    (doseq [kind emit-hl7/siu-event-kinds]
      (is (some? (emit-hl7/message-type-registry kind)) kind)
      (is (false? (emit-hl7/siu-renders? nil kind)) kind))))

;; --- the config shape, and its DEFAULTS, stated as assertions ----------

(deftest siu-renders-defaults-and-the-optional-allow-list
  (testing "absent/nil is OFF"
    (is (false? (emit-hl7/siu-renders? nil :appointment))))
  (testing "`{}` is ON FOR ALL FOUR -- the key's presence IS the opt-in, unlike
            `:ladders`/`:chatter`/`:charges`, whose `{}` is off because their settings
            are what make them do anything"
    (doseq [kind emit-hl7/siu-event-kinds]
      (is (true? (emit-hl7/siu-renders? {} kind)) kind)))
  (testing "`:triggers` narrows it, in ENGINE vocabulary and never HL7 trigger strings"
    (is (true? (emit-hl7/siu-renders? {:triggers [:no-show]} :no-show)))
    (is (false? (emit-hl7/siu-renders? {:triggers [:no-show]} :appointment)))
    (is (false? (emit-hl7/siu-renders? {:triggers ["S26"]} :no-show))
        "an HL7 trigger string names no kind and therefore allows nothing"))
  (testing "and it never speaks for a kind outside the family"
    (is (false? (emit-hl7/siu-renders? {} :admission)))
    (is (false? (emit-hl7/siu-renders? {:triggers [:admission]} :admission)))))

;; --- the identity half: absent `:siu` is what this emitter rendered
;; before ruling B1's third tranche existed ------------------------------

(deftest absent-and-nil-siu-are-the-byte-identical-path
  (let [plain (emit-hl7/emit log ref-date utc-offset facility providers)]
    (is (= plain (emit-hl7/emit-wire log ref-date utc-offset facility providers nil {})))
    (is (= plain (emit-hl7/emit-wire log ref-date utc-offset facility providers nil {} {})))
    (is (= plain (wire nil)))
    (testing "and the log really does carry appointments, or the claim above is vacuous
              (`rulings.md#R-empty-population-is-red`)"
      (is (= 6 (count (filter #(contains? emit-hl7/siu-event-kinds (:event %)) log))))
      (is (empty? (filter siu? plain))))))

(deftest turning-siu-on-adds-siu-messages-and-moves-nothing-else
  (let [plain (emit-hl7/emit log ref-date utc-offset facility providers)
        on (wire {})]
    (testing "every non-SIU message is byte-equal AND in the same relative order"
      (is (= plain (filterv (complement siu?) on))))
    (testing "the SIU half is exactly one message per scheduling event, in log order"
      (is (= ["SIU^S12" "SIU^S14" "SIU^S12" "SIU^S15" "SIU^S12" "SIU^S26"]
             (mapv msh-9 (filterv siu? on))))
      (is (= (+ (count plain) 6) (count on))))
    (testing "an allow-list renders exactly the kinds it names"
      (is (= ["SIU^S26"] (mapv msh-9 (filterv siu? (wire {:triggers [:no-show]})))))
      (is (= plain (filterv (complement siu?) (wire {:triggers [:no-show]})))))
    (testing "DERIVABLE: the same log and the same profile reproduce the stream"
      (is (= on (wire {}))))))

;; --- (i) SCH-1/SCH-2: one id, stable across the whole family -----------

(deftest sch-1-and-sch-2-are-the-appointment-s-own-id-and-are-stable-across-its-family
  (let [on (wire {})
        family (filterv #(str/includes? (segment % "SCH") "APT-A") (filterv siu? on))]
    (testing "APT-A's own chain is S12 -> S14 -> S26, three messages"
      (is (= ["SIU^S12" "SIU^S14" "SIU^S26"] (mapv msh-9 family))))
    (testing "and all three carry the SAME placer and the SAME filler id -- the reason
              `:reschedule` keeps the id rather than minting a new one"
      (doseq [m family]
        (let [sch (segment m "SCH")]
          (is (= "APT-A" (field sch 1)) m)
          (is (= "APT-A" (field sch 2)) m))))
    (testing "a DIFFERENT appointment's family carries a different id, so the assertion
              above is about the id and not about the patient"
      (doseq [m (filterv #(str/includes? (segment % "SCH") "APT-B") (filterv siu? on))]
        (is (= "APT-B" (field (segment m "SCH") 1)))))))

;; --- (i) SCH timing, FROM THE EVENT ------------------------------------

(deftest sch-11-carries-the-scheduled-instant-the-event-itself-holds
  (let [on (wire {})
        tq (fn [m] (field (segment m "SCH") 11))]
    (testing "TQ-4 (start date/time) is the event's own `:scheduled-t`, rendered with the
              same anchor and offset every timestamp in this emitter uses"
      (is (= "^^^20240111000000+0000" (tq (first (siu-of on "S12")))))
      (testing "and a reschedule renders the NEW instant, not the prior one"
        (is (= "^^^20240112000000+0000" (tq (first (siu-of on "S14")))))))
    (testing "a CANCEL and a NO-SHOW carry no scheduled instant on the event at all, so
              SCH-11 is BLANK on both. That is a limit of the event contract, stated as
              a gate so it cannot be mistaken for an omission -- recovering it would mean
              folding an appointment timeline, a second state derivation this
              renders-only namespace does not own."
      (is (= "" (tq (first (siu-of on "S15")))))
      (is (= "" (tq (first (siu-of on "S26"))))))))

;; --- (i) SCH-25, and the site profile that owns its vocabulary ---------

(deftest sch-25-renders-the-filler-status-through-the-site-profile
  (let [on (wire {})
        status (fn [m] (field (segment m "SCH") 25))]
    (is (= "Booked" (status (first (siu-of on "S12")))))
    (is (= "Booked" (status (first (siu-of on "S14"))))
        "a rescheduled appointment is still BOOKED -- the trigger says it moved")
    (is (= "Cancelled" (status (first (siu-of on "S15")))))
    (is (= "Noshow" (status (first (siu-of on "S26")))))
    (testing "and a site overrides the table without forking the builder"
      (let [overridden (wire {} {} {:code-tables {:appointment-status
                                                  {:no-show {:code "DNA" :coding-system "L"}}}})]
        (is (= "DNA^L" (status (first (siu-of overridden "S26")))))
        (is (= "Booked" (status (first (siu-of overridden "S12"))))
            "an override of one entry leaves the rest of the table standing")))))

;; --- (ii) the no-show, and (iii)/(vii) the segment set ----------------

(deftest a-no-show-references-its-filler-id-and-carries-no-encounter
  (let [s26 (first (siu-of (wire {}) "S26"))]
    (is (= "APT-A" (field (segment s26 "SCH") 2)))
    (is (= ["MSH" "SCH" "PID"] (segment-ids s26))
        "no PV1: a no-show closes an appointment and opens nothing, which is exactly why
         it cannot be derived from an encounter")
    (testing "and its PID is the patient's, read at the message's own instant"
      (is (= "MRN000001" (field (segment s26 "PID") 3))))))

(deftest an-s12-booked-before-arrival-has-no-pv1-and-one-booked-mid-stay-does
  (let [on (wire {})
        bookings (siu-of on "S12")
        pre-arrival (filterv #(str/includes? (segment % "SCH") "APT-A") bookings)
        mid-stay (filterv #(str/includes? (segment % "SCH") "APT-C") bookings)]
    (testing "the common case, and the only one any run in this repository produces: a
              booking PRECEDES the encounter it opens, so there is no visit to describe"
      (is (= 1 (count pre-arrival)))
      (is (= ["MSH" "SCH" "PID"] (segment-ids (first pre-arrival)))))
    (testing "the unreachable case, exercised here because nothing else can: event 6 is
              stamped with the encounter that was open when it happened, and PV1 rides it
              carrying that encounter's visit number in PV1-19"
      (is (= 1 (count mid-stay)))
      (let [m (first mid-stay)]
        (is (= ["MSH" "SCH" "PID" "PV1"] (segment-ids m)))
        (is (= "ENC-1" (field (segment m "PV1") 19)))
        (is (= "" (field (segment m "PV1") 3))
            "an appointment names no location")
        (is (= "" (field (segment m "PV1") 7))
            "and no attending")))))

;; --- (iii) control ids on the four-part key ---------------------------

(deftest siu-control-ids-key-on-mrn-appointment-trigger-and-t
  (let [on (wire {})
        siu (filterv siu? on)]
    (testing "the key is four parts, and the appointment id is the third-from-last"
      (is (= "MRN000001-APT-A-S12-100" (msh-10 (first siu)))))
    (testing "TWO BOOKINGS FOR ONE PATIENT AT ONE SECOND still mint distinct MSH-10s --
              the collision `:result-available`'s own three-part key does NOT survive
              (`roadmap.md#oru-control-id-collision`), avoided here from the first
              message rather than repaired later"
      (let [at-200 (filterv #(= "200" (last (str/split (msh-10 %) #"-"))) siu)]
        (is (= 2 (count at-200)))
        (is (= ["MRN000001-APT-A-S14-200" "MRN000001-APT-B-S12-200"]
               (sort (mapv msh-10 at-200))))))
    (testing "and MSH-10 is unique across the whole stream, SIU and ADT together"
      (is (= (count on) (count (distinct (map msh-10 on))))))
    (testing "`control-id-for` agrees with what the wire actually renders, which is the
              contract `sim identifiers` depends on"
      (is (= (mapv msh-10 siu)
             (mapv emit-hl7/control-id-for
                   (filterv #(contains? emit-hl7/siu-event-kinds (:event %)) log)))))))

(deftest an-siu-takes-its-own-event-s-latency-offset
  (testing "unlike a ladder rung, an SIU has no basis message to borrow a lag from: it IS
            its own event, so MSH-7 shifts by that event's own control-id offset while
            every other message stays where it was"
    (let [plain (wire {})
          offsets {"MRN000001-APT-A-S12-100" 600}
          shifted (wire {} offsets nil)
          apt-a-s12 (fn [ms] (first (filterv #(= "MRN000001-APT-A-S12-100" (msh-10 %)) ms)))]
      (is (= (count plain) (count shifted)))
      (is (= "20240101000140+0000" (msh-7 (apt-a-s12 plain))))
      (is (= "20240101001140+0000" (msh-7 (apt-a-s12 shifted))))
      (testing "and the shift really does re-sort the stream, which is what makes the
                lookup above by MSH-10 rather than by position"
        (is (not= (mapv msh-10 plain) (mapv msh-10 shifted))))
      (is (= (mapv msh-7 (filterv (complement siu?) plain))
             (mapv msh-7 (filterv (complement siu?) shifted)))))))

;; --- (v) replay: SIU is not an ADT ------------------------------------

(deftest v2-replay-skips-the-siu-family-and-round-trip-coherence-is-unaffected
  (let [on (wire {})
        without (filterv (complement siu?) on)]
    (testing "sanity: there really are SIU messages to skip"
      (is (= 6 (count (filterv siu? on)))))
    (testing "folding a stream WITH SIU reconstructs exactly what folding the same stream
              WITHOUT it does -- the round trip this sweep owes"
      (is (= (v2-replay/replay-messages without) (v2-replay/replay-messages on))))
    (testing "and the accumulator is not empty, or the equality above proves nothing"
      (is (seq (v2-replay/replay-messages on)))
      (is (= :discharged (:status (get (v2-replay/replay-messages on) "MRN000001")))))
    (testing "an SIU folded ALONE bootstraps NO entry from its PID. That is the reason
              the skip is by FAMILY and not an `evolve-entry` arm: a booking made weeks
              before a patient arrives would otherwise put that patient into the
              reconstruction as `:new` while the true side has no record of them."
      (is (= {} (v2-replay/fold-message {} (first (siu-of on "S12"))))))
    (testing "the throw is still there for a family that BELONGS in the accumulator"
      (is (thrown? clojure.lang.ExceptionInfo
                   (v2-replay/fold-message
                    {} (str/replace (first (siu-of on "S12")) "SIU^S12" "ADT^A99")))))))

;; --- the population half: a real run, through the real engine ----------

(def ^:private scheduling-run
  {:seed 7 :patients 20 :arrival-gap 90 :encounters true
   :scheduling {:scheduled-fraction 0.7 :lead-time-days [3 21] :no-show-rate 0.15
                :reschedule-rate 0.25 :cancel-rate 0.15
                :follow-up {:rate 0.6 :interval-days [30 120]}}})

(deftest a-real-scheduling-run-renders-all-four-triggers-and-no-pv1-anywhere
  (let [{:keys [ground-truth facility providers]} (engine/run scheduling-run)
        plain (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
        on (emit-hl7/emit-wire ground-truth ref-date utc-offset facility providers nil {}
                               {:siu {}})
        siu (filterv siu? on)
        families (frequencies (map msh-9 siu))]
    (testing "sanity: this run really schedules (R-empty-population-is-red)"
      (is (<= 20 (count siu)))
      (is (= #{"SIU^S12" "SIU^S14" "SIU^S15" "SIU^S26"} (set (keys families)))
          (str "measured 2026-08-28 at " (pr-str scheduling-run) ". Got " (pr-str families))))
    (testing "`:siu` absent renders none of them, at population scale"
      (is (empty? (filterv siu? plain)))
      (is (= plain (filterv (complement siu?) on))))
    (testing "NOT ONE SIU IN A REAL RUN CARRIES A PV1, and the reason is structural
              rather than seeded: both of this project's booking producers decide OUTSIDE
              an open encounter -- the pre-loop books before the patient has any
              encounter, and a follow-up is a step `decide :discharge` PREPENDS, so its
              own decide runs after that discharge closed the encounter. Measured 0 of
              72 appointment-family events at the `scheduling` oracle root, 0 of 64 at
              seed-202-ed-tuesday and 0 of 56 at seed-424242-clinic-decade. If this ever
              goes red the engine started stamping a mid-stay booking, which is a
              CHANGE and not a break -- re-measure, do not delete."
      (is (empty? (filter #(some? (:encounter-id %))
                          (filter #(contains? emit-hl7/siu-event-kinds (:event %))
                                  ground-truth))))
      (is (every? #(= ["MSH" "SCH" "PID"] (segment-ids %)) siu)))
    (testing "MSH-10 is unique across the whole population's wire"
      (is (= (count on) (count (distinct (map msh-10 on))))))
    (testing "and every SIU event this emitter consumes still conforms to the contract --
              the four kinds did not move, only their rendering did"
      (doseq [event (filter #(contains? emit-hl7/siu-event-kinds (:event %)) ground-truth)]
        (is (engine/valid-event? event) (pr-str (engine/explain-event event)))))))

(deftest siu-on-with-scheduling-off-renders-nothing-and-moves-nothing
  (testing "the key is emission, not generation: `:siu` creates no event, so a run with
            no `:scheduling` has nothing to render and is byte-identical with it on"
    (let [{:keys [ground-truth facility providers]} (engine/run {:seed 11 :patients 6})
          plain (emit-hl7/emit ground-truth ref-date utc-offset facility providers)
          on (emit-hl7/emit-wire ground-truth ref-date utc-offset facility providers nil {}
                                 {:siu {}})]
      (is (seq plain))
      (is (= plain on)))))
