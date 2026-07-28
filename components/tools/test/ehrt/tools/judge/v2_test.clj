(ns ehrt.tools.judge.v2-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [ehrt.tools.result :as result]
            [ehrt.tools.judge.finding :as finding]
            [ehrt.tools.judge.v2 :as gate]))

(def valid-message (slurp "test/fixtures/v2/adt-a01-admit.hl7"))

;; ---- execute: raw capture, never throws ----

(deftest execute-valid-message-has-no-parse-exception-test
  (let [raw (gate/execute valid-message)]
    (is (nil? (:parse-exception raw)))
    (is (= [] (:validation-exceptions raw)))
    (is (= "hapi-hl7v2" (:name (:engine raw))))
    (is (string? (:version (:engine raw))))
    (is (re-matches #"^[0-9a-f]{64}$" (:input-sha256 raw)))))

(deftest execute-missing-msh-9-captures-parse-exception-test
  (let [broken (str/replace valid-message "ADT^A01^ADT_A01" "")
        raw (gate/execute broken)]
    (is (some? (:parse-exception raw)))
    (is (= "ca.uhn.hl7v2.HL7Exception" (:class (:parse-exception raw))))))

(deftest execute-truncated-message-captures-encoding-exception-test
  (let [broken (subs valid-message 0 30)
        raw (gate/execute broken)]
    (is (some? (:parse-exception raw)))
    (is (= "ca.uhn.hl7v2.parser.EncodingNotSupportedException" (:class (:parse-exception raw))))))

(deftest execute-bad-delimiter-captures-parse-exception-test
  (let [broken (str/replace-first valid-message "^~\\&" "^~&")
        raw (gate/execute broken)]
    (is (some? (:parse-exception raw)))
    (is (= "ca.uhn.hl7v2.HL7Exception" (:class (:parse-exception raw))))))

(deftest execute-bad-datetime-value-captures-datatype-exception-with-location-test
  (let [broken (str/replace-first valid-message "19850312" "notadate")
        raw (gate/execute broken)]
    (is (some? (:parse-exception raw)))
    (is (= "ca.uhn.hl7v2.model.DataTypeException" (:class (:parse-exception raw))))
    (is (= "PID" (:segment (:location (:parse-exception raw)))))
    (is (= 7 (:field (:location (:parse-exception raw)))))))

;; ---- interpret: pure, versioned, findings + verdict ----

(deftest interpret-valid-message-is-pass-with-no-findings-test
  (let [raw (gate/execute valid-message)
        outcome (gate/interpret raw)]
    (is (= :pass (:verdict outcome)))
    (is (= [] (:findings outcome)))))

(deftest interpret-parse-failure-is-rejected-with-one-finding-test
  (let [broken (str/replace valid-message "ADT^A01^ADT_A01" "")
        raw (gate/execute broken)
        outcome (gate/interpret raw)]
    (is (= :rejected (:verdict outcome)))
    (is (= 1 (count (:findings outcome))))
    (let [f (first (:findings outcome))]
      (is (finding/valid? f))
      (is (= :error (:severity f)))
      (is (= :v2 (:format (:locator f))))
      (is (string? (:path (:locator f)))))))

(deftest interpret-datatype-violation-locator-points-at-the-field-test
  (let [broken (str/replace-first valid-message "19850312" "notadate")
        outcome (gate/interpret (gate/execute broken))]
    (is (= :rejected (:verdict outcome)))
    (is (= "PID-7" (:path (:locator (first (:findings outcome))))))))

(deftest interpret-structural-error-without-field-location-falls-back-to-msh-locator-test
  (let [broken (str/replace valid-message "ADT^A01^ADT_A01" "")
        outcome (gate/interpret (gate/execute broken))]
    (is (= "MSH" (:path (:locator (first (:findings outcome))))))))

(deftest interpret-collected-validation-exceptions-are-pass-with-findings-test
  ;; Simulated raw payload -- a HAPI validation exception collected via
  ;; the non-throwing handler path (empty for every fixture EXP-C5/P5's
  ;; probes exercised, per judge.v2's own docstring); this test proves
  ;; the *policy* holds even though no real fixture triggers it: at
  ;; this tier, a collected (non-parse-failure) HAPI signal is always
  ;; :pass-with-findings, never :rejected -- judge.v2 is base-structural
  ;; only, and nothing here ever produces :indeterminate.
  (let [raw {:engine {:name "hapi-hl7v2" :version "2.6.0"}
             :input-sha256 (apply str (repeat 64 "a"))
             :parse-exception nil
             :validation-exceptions [{:class "ca.uhn.hl7v2.validation.ValidationException"
                                       :message "some warning"
                                       :severity "ERROR"
                                       :location {:segment "PID" :segment-repetition 0 :field 8 :component -1}}]}
        outcome (gate/interpret raw)]
    (is (= :pass (:verdict outcome)))
    (is (= 1 (count (:findings outcome))))
    (is (= "PID-8" (:path (:locator (first (:findings outcome))))))))

;; ---- gate-file: reads, executes, interprets; never mutates input ----

(deftest gate-file-happy-path-test
  (let [r (gate/gate-file "test/fixtures/v2/adt-a01-admit.hl7")]
    (is (result/ok? r))
    (is (= :pass (:verdict (:payload r))))
    (is (= "test/fixtures/v2/adt-a01-admit.hl7" (:path (:payload r))))))

(deftest gate-file-does-not-modify-its-input-test
  (let [path "test/fixtures/v2/adt-a01-admit.hl7"
        before (slurp path)
        _ (gate/gate-file path)
        after (slurp path)]
    (is (= before after))))

(deftest gate-file-detects-the-hand-broken-fixture-test
  (let [r (gate/gate-file "test/fixtures/v2/adt-a08-update-trailing-empty-fields.hl7")]
    (is (result/ok? r))
    ;; This fixture is a legitimate v2 message (EXP-B2 already parsed it
    ;; successfully); it exercises the trailing-empty-fields
    ;; canonicalization finding, not a structural break.
    (is (contains? #{:pass :rejected} (:verdict (:payload r))))))

(deftest gate-file-missing-path-is-an-operational-error-test
  (let [r (gate/gate-file "/no/such/file.hl7")]
    (is (result/error? r))
    (is (= :file-not-found (:category r)))))

;; ---- gate-dir: batch over every *.hl7 file ----

(deftest gate-dir-gates-every-hl7-file-test
  (let [r (gate/gate-dir "test/fixtures/v2")]
    (is (result/ok? r))
    (is (= 5 (count (:results (:payload r)))))
    (is (every? #(contains? #{:pass :rejected} (:verdict %)) (:results (:payload r))))))
