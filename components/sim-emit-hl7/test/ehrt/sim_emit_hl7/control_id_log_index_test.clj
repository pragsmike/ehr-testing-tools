(ns ehrt.sim-emit-hl7.control-id-log-index-test
  "ADR-0181 candidate 3, the ruled key shape: MSH-10 carries the LOG
  INDEX. The author ruling of 2026-09-23 takes the third of the three
  candidates ADR-0181's own closing section left standing -- the one
  the 2026-08-28 fan-out ruling already made this project's identity of
  record (`ehrt.sim-emit-hl7.fan-out`'s own docstring: identity is the
  log index, never MSH-10). The index is minted at EMISSION out of a
  field the funnel stamps onto its own copy of the log; ground truth
  does not move.

  WHAT IS ASSERTED HERE is the local law, over hand-built events:

  1. Two events EQUAL in `(active-mrn, trigger, t)` -- the default
     branch's whole key before this change -- mint DISTINCT ids when
     their stamped indices differ. That is the defect ADR-0181
     measured, stated as a unit.
  2. An UNSTAMPED registered event THROWS. Fail closed: there is no
     path that silently mints the old three-part id, because a
     three-part id in a corpus would be indistinguishable from a
     pre-change one and would collide exactly as before.
  3. An event OUTSIDE `message-type-registry` still returns nil,
     stamped or not -- the 'no message, no id' rule `event->messages`
     already follows is untouched, and is checked BEFORE the stamp is
     required so that `plan-latency`'s own `when-let` and
     `event->messages`' own empty-vector path keep working on raw logs.
  4. The old id is a strict PREFIX of the new one, and the suffix is
     the marker plus the index. This is the downstream-notice law
     stated as an assertion: a reader holding a pre-change id can find
     its successor by prefix, and can read the event's log position
     straight out of the id.
  5. Every discriminated arm (SIU, `:bed-status-change`, `:bed-swap`,
     `:merge`) takes the suffix too. `:merge` is the reason it is
     uniform rather than default-branch-only: ADR-0181 finding 3
     measured `:merge` colliding with an arm of its own, so an arm is
     not by itself a guarantee and the index is what makes it one.

  THE WHOLE-LOG GATE IS ELSEWHERE, deliberately:
  `ehrt.sim.run-test/control-id-for-is-injective-over-every-gated-
  corpus` runs the ruled key over real corpora, which is the gate
  ADR-0181's own 'what this costs the gates that already exist'
  section says was missing. This namespace proves the mechanism; that
  one proves the population."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.sim-emit-hl7.segments :as segments]))

;; The stamp key and the marker, written as LITERALS here rather than
;; as `::segments/log-index` and the emitter's own marker def, for one
;; reason worth naming: this file was authored as the red test at
;; `2b52fc57`, where neither var existed. A literal resolves at any
;; commit, so these assertions failed as ASSERTIONS at the baseline --
;; the defect itself -- rather than as a namespace that would not load.
;; `the-marker-and-key-this-file-pins-are-the-ones-the-emitter-uses`
;; below is what keeps the two copies from drifting; it was born green
;; in the green commit, having nothing to catch until one of them moves.
(def ^:private log-index-key :ehrt.sim-emit-hl7.segments/log-index)
(def ^:private marker "#")

(defn- twin
  "Two `:observation`s one patient holds at one second -- ADR-0181's
  own finding-1 shape, which `seed-424242-clinic-decade` really
  produces, reduced to two literals."
  [i]
  {:event :observation :active-mrn "MRN000189" :t 119086260 log-index-key i})

(deftest two-events-equal-in-mrn-trigger-and-t-mint-distinct-ids
  (testing "ADR-0181's default-branch defect, as a unit: the three-part
            key is non-injective over exactly this pair and the log
            index is what separates them"
    (let [a (twin 17)
          b (twin 23)]
      (is (= "MRN000189-R01-119086260" (subs (segments/control-id-for a) 0 23))
          "the pre-change id is still the id's own leading bytes")
      (is (not= (segments/control-id-for a) (segments/control-id-for b))
          "two events differing ONLY in log index must not share an MSH-10"))))

(deftest the-old-id-is-a-strict-prefix-and-the-suffix-is-the-index
  (testing "the downstream notice, as an assertion: old id + marker +
            0-based log position, so a held id finds its successor by
            prefix and the id names the event's own place in the log"
    (let [id (segments/control-id-for (twin 4021))
          old "MRN000189-R01-119086260"]
      (is (not= id old))
      (is (str/starts-with? id old) (str "not prefixed by the pre-change id: " id))
      (is (= (str old marker "4021") id)))))

(deftest an-unstamped-registered-event-throws
  (testing "FAIL CLOSED. A missing stamp is a programmer error -- a
            funnel that forgot to stamp -- and the one outcome that must
            never happen is a silent three-part id, which no reader could
            tell from a pre-change one"
    (let [unstamped (dissoc (twin 0) log-index-key)]
      (is (thrown? clojure.lang.ExceptionInfo (segments/control-id-for unstamped))))))

(deftest an-event-outside-the-registry-is-still-nil-stamped-or-not
  (testing "'no message, no id' is untouched, and the registry is
            consulted BEFORE the stamp is required -- `plan-latency`'s
            `when-let` and `event->messages`' empty-vector path both
            walk raw logs full of unregistered kinds"
    (let [ev {:event :care-plan-end :active-mrn "MRN000001" :t 10}]
      (is (nil? (segments/control-id-for ev)))
      (is (nil? (segments/control-id-for (assoc ev log-index-key 3)))))))

(deftest every-discriminated-arm-takes-the-suffix-too
  (testing "ADR-0181 finding 3: `:merge` ALREADY had an arm and still
            collided with itself, so the index is uniform across every
            arm rather than bolted onto the default branch alone"
    (let [arms [{:ev {:event :appointment :active-mrn "MRN000001" :appointment-id "APPT-1" :t 90}
                 :old "MRN000001-APPT-1-S12-90"}
                {:ev {:event :bed-status-change :bed "RENAL-01" :to :cleaning :t 90}
                 :old "RENAL-01-cleaning-A20-90"}
                {:ev {:event :merge :surviving-mrn "MRN000001" :t 90}
                 :old "MRN000001-A40-90"}
                {:ev {:event :bed-swap :t 90
                      :participants [{:patient-id "P1"} {:patient-id "P2"}]
                      :swap {"P1" {:active-mrn "MRN000001"} "P2" {:active-mrn "MRN000002"}}}
                 :old "MRN000001+MRN000002-A17-90"}]]
      (doseq [{:keys [ev old]} arms]
        (testing (str (:event ev))
          (is (= (str old marker "6")
                 (segments/control-id-for (assoc ev log-index-key 6)))))))))
