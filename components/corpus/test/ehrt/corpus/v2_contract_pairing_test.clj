(ns ehrt.corpus.v2-contract-pairing-test
  "v2's twin of test-integration/ehr_testing_tools/contract_pairing_test.clj
  (P5): for each of the five v2 defect operators (ehrt.corpus.
  operators, P7 seed catalog), mutate a real fixture
  (components/corpus/test-fixtures/v2/adt-a01-admit.hl7), gate the mutant through the
  real judge.v2 (in-process HAPI HL7v2, no external engine subprocess
  -- judge.v2's own docstring), and assert judge.v2's response matches
  the operator's own :violates contract. This is the polarity
  regression's v2 twin: `:rejected` here is SUCCESS -- the operator did
  what its contract claims and judge.v2 caught it -- the workflow, not
  the judge, supplies the polarity that turns a verdict into a pass/fail
  outcome (ADR-0009). Lives in test/, not test-integration/, because
  judge.v2 runs at unit speed (no real artifact, no network, no warm
  cache) -- unlike the FHIR suite, which needs a real validator_cli.jar
  subprocess and so belongs on the hermeticity-excluded path.

  Every assertion here was verified empirically against a real fixture
  before the corresponding operator was registered (corpus.operators's
  own docstring records which candidates did NOT convict and were
  dropped) -- this suite is where that empirical claim gets a
  regression, not just a one-off session probe."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehrt.kernel.interface :as kernel]
            [ehrt.corpus.mutate :as mutate]
            [ehrt.corpus.operators :as operators]
            [ehrt.judge-v2-hapi.interface :as v2-hapi]))

(def ^:private work-dir "target/v2-contract-pairing")
(def ^:private admit-fixture "components/corpus/test-fixtures/v2/adt-a01-admit.hl7")

(defn- mutate-and-gate!
  "Mutates admit-fixture's content at locator-path with operator-id,
  writes the mutant to work-dir, gates it through the real judge.v2.
  Returns the gate outcome ({:verdict :findings :path})."
  [operator-id locator-path]
  (let [base (slurp (io/file admit-fixture))
        operator (operators/lookup operator-id "1")
        mutate-result (mutate/mutate base operator {:format :v2 :path locator-path})]
    (when-not (kernel/ok? mutate-result)
      (throw (ex-info "v2 contract-pairing: mutate failed" mutate-result)))
    (let [mutant (:mutant (:payload mutate-result))
          mutant-path (str work-dir "/" (name operator-id) "-mutant.hl7")]
      (io/make-parents mutant-path)
      (spit mutant-path mutant)
      (let [gate-result (v2-hapi/gate-file mutant-path)]
        (when-not (kernel/ok? gate-result)
          (throw (ex-info "v2 contract-pairing: gate failed" gate-result)))
        (:payload gate-result)))))

;; ---- the baseline: the unmutated fixture does not convict. Every
;; per-operator test below proves the SAME fixture starts clean and
;; only the injected defect flips it -- this is the polarity these
;; tests exist to demonstrate, not an incidental sanity check. ----

(deftest unmutated-fixture-is-pass-test
  (let [outcome (:payload (v2-hapi/gate-file admit-fixture))]
    (is (= :pass (:verdict outcome)))
    (is (= [] (:findings outcome)))))

;; ---- structural failures: judge.v2's parse-time tier collapses every
;; one of these to a single finding at the bare "MSH" locator (no
;; field-level location survives a message-structure-resolution or
;; encoding failure) -- honestly reflecting the judge's own resolution
;; at this base-structural tier, not asserted as a limitation of these
;; tests. ----

(deftest blank-required-field-contract-test
  (let [outcome (mutate-and-gate! :blank-required-field "MSH-9")]
    (is (= :rejected (:verdict outcome)))
    (is (= 1 (count (:findings outcome))))
    (is (= "hl7-exception" (:code (first (:findings outcome)))))
    (is (= "MSH" (:path (:locator (first (:findings outcome))))))))

(deftest corrupt-encoding-characters-contract-test
  (let [outcome (mutate-and-gate! :corrupt-encoding-characters "MSH-2")]
    (is (= :rejected (:verdict outcome)))
    (is (= 1 (count (:findings outcome))))
    (is (= "hl7-exception" (:code (first (:findings outcome)))))
    (is (= "MSH" (:path (:locator (first (:findings outcome))))))))

(deftest truncate-segment-fields-contract-test
  (let [outcome (mutate-and-gate! :truncate-segment-fields "MSH-9")]
    (is (= :rejected (:verdict outcome)))
    (is (= 1 (count (:findings outcome))))
    (is (= "hl7-exception" (:code (first (:findings outcome)))))
    (is (= "MSH" (:path (:locator (first (:findings outcome))))))))

(deftest corrupt-segment-name-contract-test
  ;; verified only against MSH (corpus.operators's own contract
  ;; :target documents that corrupting a non-header segment's name,
  ;; e.g. PID, does NOT convict at this tier).
  (let [outcome (mutate-and-gate! :corrupt-segment-name "MSH")]
    (is (= :rejected (:verdict outcome)))
    (is (= 1 (count (:findings outcome))))
    (is (= "encoding-not-supported-exception" (:code (first (:findings outcome)))))
    (is (= "MSH" (:path (:locator (first (:findings outcome))))))))

;; ---- the one operator whose failure survives far enough into parsing
;; to carry a real field-level location ----

(deftest malformed-datetime-value-contract-test
  (let [outcome (mutate-and-gate! :malformed-datetime-value "PID-7")]
    (is (= :rejected (:verdict outcome)))
    (is (= 1 (count (:findings outcome))))
    (is (= "data-type-exception" (:code (first (:findings outcome)))))
    (is (= "PID-7" (:path (:locator (first (:findings outcome))))))))

;; ---- the Judge stage kind law (docs/notation.md): gating never
;; modifies the datum it judges -- ehrt.judge-v2-hapi.v2-test already
;; covers this against a real fixture; this is the same law against a
;; real MUTANT, same discipline as the FHIR suite's own
;; gate-fhir-never-modifies-its-input-test ----

(deftest gate-v2-never-modifies-its-input-test
  (let [_ (mutate-and-gate! :blank-required-field "MSH-9")
        mutant-path (str work-dir "/blank-required-field-mutant.hl7")
        before (slurp mutant-path)
        _ (v2-hapi/gate-file mutant-path)
        after (slurp mutant-path)]
    (is (= before after))))
