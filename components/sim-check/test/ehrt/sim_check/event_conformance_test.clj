(ns ehrt.sim-check.event-conformance-test
  "Consumer conformance for the invariant catalog (event-log contract
  arc Step 2, author rulings Q-A (a) / Q-B (a), 2026-08-16).

  `check-all` is the log's other heavy reader: thirty-odd invariants
  that between them touch `:disposition`, `:cancels-event-id`,
  `:order-event-id`, `:profile`, `:forced`, `:home-ward`, `:warm-up`,
  `:reason`, `:persona`, and `:pre-horizon-facts` -- several of which
  NEITHER emitter renders. That makes it the consumer whose reads come
  closest to covering the whole contract, and the one where a silently
  changed event shape would do the most damage: an invariant reading a
  key that quietly moved does not fail, it stops finding violations.

  The census found one read here that looked dead and was not:
  `check.clj:212`'s `(:disposition event)` on `:discharge` never
  occurred in any demo corpus, and needed a purpose-built death corpus
  to prove live. That is exactly the class of thing this test exists to
  keep honest -- so the death path is exercised explicitly below rather
  than left to a seed.

  TESTS ONLY: `check-all` validates against invariants, never against
  this schema, at runtime."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-check.check :as check]
            [ehrt.kernel.interface :as result]))

(def ^:private crowded-facility
  {:id :check-conformance
   :wards [{:id :ed :name "ED" :beds 0 :surge-slots 4 :surge-format "%s-H%02d" :class :ed}
           {:id :renal :name "Renal" :beds 1 :surge-slots 1 :surge-format "%s-H%02d" :class :inpatient}]})

(defn- expired-discharge-run
  "The death path, authored rather than hunted -- `:disposition` and
  `:codes` on a `:discharge`, the two keys the census reached in 1 of
  4,997 events and 0 of anything the docs teach."
  []
  (engine/run {:seed 41 :patients 1 :arrival-gap 0
               :facility crowded-facility
               :pathways [{:patient-ordinal 0
                           :pathway {:name "expires"
                                     :steps [{:type :admission :location "Renal"}
                                             {:type :discharge :disposition :expired
                                              :codes [{:system :snomed :code "410429000"
                                                       :display "Cardiac arrest"}]}]}}]}))

(deftest every-event-this-checker-consumes-conforms-to-the-contract
  (doseq [[label {:keys [ground-truth facility]}]
          [["plain" (engine/run {:seed 42 :patients 5})]
           ["churn" (engine/run {:seed 43 :patients 5 :facility crowded-facility :churn true})]
           ["expired-discharge" (expired-discharge-run)]]]
    (testing label
      (is (seq ground-truth))
      (doseq [event ground-truth]
        (is (engine/valid-event? event)
            (str label " / " (:event event) " at t=" (:t event) ": "
                 (pr-str (engine/explain-event event)))))
      (testing "and the log this checker was handed actually passes it,
                so conformance is being proven over a log the catalog
                itself accepts -- not over an arbitrary one"
        (is (result/ok? (check/check-all ground-truth facility)))))))

(deftest the-disposition-read-that-looked-dead-is-exercised-here
  (testing "census finding: `expired-discharge-vacates-no-bed` reads
            `:disposition`, which no demo corpus produces. Pinned so
            the read cannot rot back into looking dead."
    (let [{:keys [ground-truth]} (expired-discharge-run)
          expired (filter #(= :expired (:disposition %)) ground-truth)]
      (is (= 1 (count expired)))
      (is (every? engine/valid-event? expired))
      (is (contains? (first expired) :codes)
          "cause of death rides alongside the disposition"))))

(deftest the-step-rejected-reason-enum-is-the-engines-not-the-censuss
  (testing "the schema types `:step-rejected`'s `:reason` from
            `documented-step-rejection-reasons` rather than from what
            the census happened to observe (1 of 7 values across five
            churn seeds). Every documented reason must therefore
            validate -- a schema narrowed to observation would reject
            six legal logs."
    (doseq [reason engine/documented-step-rejection-reasons]
      (is (engine/valid-event?
           {:event :step-rejected :t 0 :warm-up false
            :participants [{:patient-id "PID-000000-abcdef01" :role :subject}]
            :reason reason
            :attempted-step {:type :cancel-admit}})
          (str "documented rejection reason rejected by the schema: " reason)))))

(deftest warm-up-marked-events-conform
  (testing "`warm-up-mark-matches-window` is the only consumer of
            `:warm-up`, and the census saw the key take `true` only
            under an explicit window"
    (let [{:keys [ground-truth facility]}
          (engine/run {:seed 44 :patients 5 :warm-up-seconds 3600})]
      (is (some :warm-up ground-truth) "the window must actually bite")
      (is (every? engine/valid-event? ground-truth))
      (is (result/ok? (check/check-all ground-truth facility 3600))))))
