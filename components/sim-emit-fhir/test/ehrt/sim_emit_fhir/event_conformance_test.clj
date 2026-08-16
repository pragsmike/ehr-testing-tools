(ns ehrt.sim-emit-fhir.event-conformance-test
  "Consumer conformance for the FHIR emitter (event-log contract arc
  Step 2, author rulings Q-A (a) / Q-B (a), 2026-08-16).

  THIS EMITTER IS A DIFFERENT KIND OF CONSUMER, and the census had to
  discover that rather than assume it. `bundle-run` reads exactly ONE
  key off a raw event -- `:t`, to resolve an `:end` snapshot -- and
  gets everything else from `engine/replay`, i.e. from folded
  `PatientState` (`emit_fhir`'s own snapshot-at-instant law: 'the FHIR
  emitter touches NOTHING but folded state, never the log directly').

  So its real contract surface is `evolve`'s per-kind reads, not its
  own. A conformance test that only validated what `bundle-run`
  destructures would check a single integer and prove nothing. This
  one instead asserts the contract over the whole log the fold
  consumes, plus the fold's own output shape -- because if an event
  violates the contract, the damage here shows up as a wrong BUNDLE,
  with no malformed input anywhere for a reader to find.

  TESTS ONLY: `bundle-run` validates nothing at runtime."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-emit-fhir.emit-fhir :as emit-fhir]))

(def ^:private ref-date "2024-01-01")
(def ^:private utc-offset "+00:00")

(deftest every-event-the-fold-consumes-conforms-to-the-contract
  (testing "the log `replay` folds -- this emitter's real input, one
            layer up from the state it reads"
    (doseq [opts [{:seed 31 :patients 4}
                  {:seed 32 :patients 4 :churn true}]]
      (let [{:keys [ground-truth]} (engine/run opts)]
        (is (seq ground-truth))
        (doseq [event ground-truth]
          (is (engine/valid-event? event)
              (str (pr-str opts) " / " (:event event) " at t=" (:t event) ": "
                   (pr-str (engine/explain-event event)))))))))

(deftest the-only-raw-event-key-this-emitter-reads-is-t
  (testing "`bundle-run`'s :end resolution is `(reduce max 0 (map :t
            ground-truth))` -- typed :int by the contract, which is
            what makes that reduction total"
    (let [{:keys [ground-truth]} (engine/run {:seed 33 :patients 3})]
      (is (every? int? (map :t ground-truth)))
      (is (engine/run-t-monotone? ground-truth)
          "and monotone, so the last event's :t IS the run's end"))))

(deftest bundles-are-produced-from-a-conforming-log
  (testing "end to end: a contract-conforming log yields real bundles,
            so this test fails loudly if the fold ever stops seeing
            what the schema says it will"
    (let [{:keys [ground-truth]} (engine/run {:seed 34 :patients 3})
          bundles (emit-fhir/bundle-run ground-truth ref-date utc-offset 34 :end)]
      (is (every? engine/valid-event? ground-truth))
      (is (seq bundles) "the run must actually reach the emitter")
      (is (every? #(= "Bundle" (:resourceType %)) (vals bundles))))))

(deftest replay-records-wrap-conforming-events
  (testing "`replay`'s own trace record carries the event under
            :event -- a DERIVED view WRAPPING an event, not an event.
            The census had to correct the driving prompt on exactly
            this point, so it is pinned here: the wrapper's :before/
            :after/:world-* keys are not part of the event contract,
            and the thing under :event is."
    (let [{:keys [ground-truth]} (engine/run {:seed 35 :patients 3})
          records (engine/replay ground-truth)]
      (is (= (count ground-truth) (count records)))
      (doseq [record records]
        (is (engine/valid-event? (:event record)))
        (is (not (engine/valid-event? record))
            "the wrapper itself is NOT an event -- if this ever passes,
             the schema has gone open and stopped distinguishing them")))))
