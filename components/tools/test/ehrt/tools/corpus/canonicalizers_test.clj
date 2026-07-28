(ns ehrt.tools.corpus.canonicalizers-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [ehrt.tools.canonical :as canonical]
            [ehrt.tools.corpus.canonicalizers :as canonicalizers]))

;; Loading the canonicalizers namespace registers its entries as a
;; side effect (this is the registration point the pattern nursery's
;; "registry + laws" entry describes) -- require it above is enough.

(deftest strip-run-timestamp-suffix-registered-test
  (is (some? (canonical/lookup :strip-run-timestamp-suffix "1"))))

(deftest strip-run-timestamp-suffix-strips-hospital-and-practitioner-test
  (let [entry (canonical/lookup :strip-run-timestamp-suffix "1")
        f (:fn entry)]
    (is (= "hospitalInformation.json" (f "hospitalInformation1784838114079.json")))
    (is (= "practitionerInformation.json" (f "practitionerInformation1784838156341.json")))
    (is (= "fhir/Abraham_100.json" (f "fhir/Abraham_100.json")) "leaves unrelated names untouched")))

(deftest strip-run-timestamp-suffix-idempotent-property-test
  (let [entry (canonical/lookup :strip-run-timestamp-suffix "1")
        f (:fn entry)
        matching-gen (gen/let [prefix (gen/elements ["hospitalInformation" "practitionerInformation"])
                                n (gen/large-integer* {:min 0 :max Long/MAX_VALUE})]
                       (str prefix n ".json"))
        input-gen (gen/one-of [gen/string-ascii matching-gen])
        check-result (tc/quick-check 100 (prop/for-all [s input-gen] (= (f (f s)) (f s))))]
    (is (:pass? check-result))))

(deftest strip-synthea-run-metadata-registered-test
  (is (some? (canonical/lookup :strip-synthea-run-metadata "1"))))

(deftest strip-synthea-run-metadata-removes-volatile-keys-test
  (let [entry (canonical/lookup :strip-synthea-run-metadata "1")
        f (:fn entry)
        input {"runID" "abc-123" "seed" 100 "clinicianSeed" 555
               "runStartTime" "2026-07-24T00:00:00Z" "runTimeInSeconds" 42
               "patientCount" 100}]
    (is (= {"seed" 100 "clinicianSeed" 555 "patientCount" 100} (f input)))))

(deftest strip-synthea-run-metadata-idempotent-property-test
  (let [entry (canonical/lookup :strip-synthea-run-metadata "1")
        f (:fn entry)
        key-gen (gen/elements ["runID" "runStartTime" "runTimeInSeconds" "seed" "clinicianSeed" "other"])
        map-gen (gen/map key-gen gen/simple-type-printable)
        check-result (tc/quick-check 100 (prop/for-all [m map-gen] (= (f (f m)) (f m))))]
    (is (:pass? check-result))))
