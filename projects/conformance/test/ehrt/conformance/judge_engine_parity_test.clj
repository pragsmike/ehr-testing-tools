(ns ehrt.conformance.judge-engine-parity-test
  "Cross-engine contract test (P2-2, review finding 6, ruled 2026-07-31
  AR-1): pins the shape every v2-capable judge engine must now share
  after the parity pass -- both judge-v2-hapi and judge-v2-nist return
  the kernel result envelope from gate-file/gate-dir (never throw for
  an operational condition like a missing file), every finding
  validates against ehrt.judge.finding/Finding, every verdict/cause
  pair satisfies valid-cause-pairing?, and gate-dir walks recursively
  (finds a file nested one subdirectory deep -- test-fixtures/
  gate-dir-nested/, deliberately the same fixture for both engines).
  Gives judge-v2-nist a genuine test-tier dependency on judge here too
  (this project's deps.edn already lists both), mirroring what
  judge-v2-hapi's own component test (v2_test.clj) already does."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as kernel]
            [ehrt.judge.finding :as finding]
            [ehrt.judge-v2-hapi.v2 :as hapi]
            [ehrt.judge-v2-nist.v2 :as nist]))

(def ^:private nested-dir "projects/conformance/test-fixtures/gate-dir-nested")

(def ^:private nist-bundle-dir
  "components/corpus/test-fixtures/v2-nist/COVID19_ELR-v2.3.1")

(def ^:private nist-message-file
  "components/corpus/test-fixtures/v2-nist/covidELR/231HL7TestFilewithHHSData.txt")

(def ^:private hapi-message-file
  "components/corpus/test-fixtures/v2/adt-a08-update-trailing-empty-fields.hl7")

;; ---- missing file: kernel error, never a throw ----

(deftest hapi-missing-file-returns-kernel-error-test
  (let [r (hapi/gate-file "/no/such/file.hl7")]
    (is (kernel/error? r))
    (is (= :file-not-found (:category r)))))

(deftest nist-missing-file-returns-kernel-error-test
  (let [validator-state (nist/make-validator nist-bundle-dir)
        r (nist/gate-file validator-state (io/file "/no/such/file.hl7"))]
    (is (kernel/error? r))
    (is (= :file-not-found (:category r)))))

;; ---- gate-dir walks recursively (AR-1: nist's file-seq behavior is
;; the standard both engines now share) ----

(deftest hapi-gate-dir-walks-recursively-test
  (let [r (hapi/gate-dir nested-dir)]
    (is (kernel/ok? r))
    (is (= 2 (count (:results (:payload r))))
        "finds both the top-level file and the one nested one subdirectory deep")))

(deftest nist-gate-dir-walks-recursively-test
  (let [validator-state (nist/make-validator nist-bundle-dir)
        r (nist/gate-dir validator-state nested-dir)]
    (is (kernel/ok? r))
    (is (= 2 (count (:results (:payload r)))))))

;; ---- findings validate against the shared schema; verdict/cause
;; pairing is Malli-valid ----

(deftest hapi-findings-validate-against-shared-schema-test
  (let [r (hapi/gate-file hapi-message-file)
        {:keys [verdict cause findings]} (:payload r)]
    (is (kernel/ok? r))
    (is (every? finding/valid? findings))
    (is (finding/valid-cause-pairing? verdict cause))))

(deftest nist-findings-validate-against-shared-schema-test
  (let [validator-state (nist/make-validator nist-bundle-dir)
        r (nist/gate-file validator-state (io/file nist-message-file))
        {:keys [verdict cause findings]} (:payload r)]
    (is (kernel/ok? r))
    (is (every? finding/valid? findings))
    (is (finding/valid-cause-pairing? verdict cause))))
