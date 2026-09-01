(ns ehrt.sim-emit-hl7.event-conformance-test
  "Consumer conformance: this emitter's own INPUT, validated against
  the explicit event contract (`ehrt.sim-engine.event-schema`,
  event-log contract arc Step 2, author rulings Q-A (a) / Q-B (a),
  2026-08-16).

  WHY THIS TEST EXISTS AT ALL. Before the contract, a consumer wanting
  to render the ground-truth log into a format we cannot know ahead of
  time read THIS namespace to learn the event shape -- which quietly
  made this emitter's own field choices the contract, and made a
  schema change indistinguishable from a schema break. The census that
  opened the arc found the consequence: `emit_hl7` reads `:from` on a
  transfer and `:swap` on a bed-swap and `:results` on a result, but
  renders nothing at all for `:disposition`, `:home-ward`, `:forced`,
  `:bed-ready`, `:citation`, or `:profile`. A reader could not tell
  which absences were design and which were oversight.

  This test flips the direction. The emitter is no longer where the
  shape is DEFINED; it is a consumer that checks its input against a
  contract defined elsewhere. If the engine ever emits something this
  emitter would silently mis-render, the failure surfaces here as a
  schema violation rather than downstream as a malformed message.

  TESTS ONLY. `emit` itself validates nothing -- the contract adds no
  runtime cost to emission (`event-schema`'s own no-runtime-cost note)."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-emit-hl7.emit :as emit]
            [ehrt.sim-emit-hl7.registry :as registry]
            [ehrt.sim-model.interface :as sim-model]
            [clojure.set]))

(def ^:private ref-date "2024-01-01")
(def ^:private utc-offset "+00:00")

(defn- run-and-emit
  "One real run, emitted for real -- the input this emitter actually
  consumes, not a hand-built sample of it."
  [opts]
  (let [{:keys [ground-truth facility providers] :as result} (engine/run opts)]
    (assoc result :messages (emit/emit ground-truth ref-date utc-offset facility providers))))

(deftest every-event-this-emitter-consumes-conforms-to-the-contract
  (doseq [opts [{:seed 21 :patients 4}
                {:seed 22 :patients 4 :churn true}
                {:seed 23 :patients 3 :arrival-gap 0}]]
    (testing (pr-str opts)
      (let [{:keys [ground-truth messages]} (run-and-emit opts)]
        (is (seq ground-truth))
        (is (seq messages) "the run must actually reach the emitter")
        (doseq [event ground-truth]
          (is (engine/valid-event? event)
              (str (:event event) " at t=" (:t event) ": "
                   (pr-str (engine/explain-event event)))))))))

(deftest every-kind-this-emitter-renders-is-in-the-contracts-vocabulary
  (testing "the message-type registry may not name an event kind the
            contract does not declare -- the co-landing convention this
            registry already documents, now mechanically checked
            against the schema rather than against a reader's memory"
    (let [declared (into #{} (map first) (rest (rest engine/Event)))
          rendered (set (keys registry/message-type-registry))]
      (is (empty? (clojure.set/difference rendered declared))
          (str "rendered but not declared: "
               (sort (clojure.set/difference rendered declared)))))))

(deftest the-kinds-this-emitter-deliberately-does-not-render-are-still-contract-kinds
  (testing "recorded, not asserted away: these are real events a
            proprietary consumer WILL see and may want, and their
            absence from the wire is a design choice this project has
            documented (`message-type-registry`), not a gap in the log"
    (let [declared (into #{} (map first) (rest (rest engine/Event)))
          rendered (set (keys registry/message-type-registry))
          silent (clojure.set/difference declared rendered)]
      ;; 1.3.0 (ADR-0173, arc 3a part 3) adds TWO, and the addition is
      ;; recorded in all three places this gate's own message demands:
      ;; each kind's `:doc` (which is what `docs/formats.md`'s generated
      ;; section renders), `message-type-registry`'s own comment, and
      ;; here. Their silence is unlike the other eight's: the change
      ;; still reaches the wire, in the PID and IN1 of every message the
      ;; patient receives after it.
      ;; 1.7.0 (ADR-0174 section 2(b) plus ruling C, arc 3b sweep 3) added
      ;; FOUR -- `:appointment`, `:reschedule`, `:appointment-cancel`,
      ;; `:no-show` -- whose silence was a VERSION gap and not a design
      ;; preference: the SIU family is v2.4 structure and every message
      ;; this emitter carried said MSH-12 "2.3".
      ;;
      ;; ARC 4 CLOSED BOTH HALVES OF THAT, and this set is 14 -> 10 for
      ;; it. Sweep 1 (ADR-0175 ruling A1) made `default-msh` declare
      ;; "2.4"; sweep 4 (ruling B1) added the four registry entries. The
      ;; removal is recorded in the same three places an addition is:
      ;; each kind's own `:doc` (which still says "deliberately
      ;; unrendered in 1.7.0", and is TRUE of contract 1.7.0 -- the
      ;; rendering is emission config, not a contract change, and this
      ;; sweep's own fences forbid a schema diff),
      ;; `message-type-registry`'s own comment, and here.
      ;;
      ;; THEIR ENTRY IS UNLIKE EVERY OTHER ENTRY'S in one way this gate
      ;; cannot see and a reader should: they render only when `:siu` is
      ;; on. `message-type-registry` says a kind CAN reach the wire; for
      ;; these four it does not say it always does.
      (is (= #{:registered :step-rejected :outpatient-visit-end :procedure
               :medication-order :medication-end :care-plan-start :care-plan-end
               :demographic-update :coverage-change}
             silent)
          (str "the set of contract kinds this emitter renders no message for "
               "changed to " (sort silent) " -- that is a real change in what "
               "the HL7 surface covers, and belongs in docs/formats.md and "
               "the registry's own comment, not in a silently-updated test")))))

(deftest personas-the-emitter-reads-off-the-log-conform
  (testing "`personas-by-patient-id` scans :registered events for
            :persona -- the one place this emitter derives state from
            the log rather than from the event in hand"
    (let [{:keys [ground-truth]} (engine/run {:seed 24 :patients 5})]
      (doseq [event (filter #(= :registered (:event %)) ground-truth)]
        (is (sim-model/valid-persona? (:persona event)))))))
