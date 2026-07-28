(ns ehrt.tools.check.schemas-test
  "Loading check.schemas registers the seed catalog as a side effect
  (same convention as corpus.operators/corpus.canonicalizers) --
  requiring it below is enough."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.tools.result :as result]
            [ehrt.tools.check.schemas :as schemas]))

(deftest register-and-lookup-round-trips-test
  (let [r (schemas/register! {:id :test-schema :version "1"
                               :schema [:map ["x" :int]]
                               :docstring "test-only"})]
    (is (result/ok? r))
    (is (= {:id :test-schema :version "1"} (:payload r)))
    (is (some? (schemas/lookup :test-schema "1")))))

(deftest lookup-unknown-schema-is-nil-test
  (is (nil? (schemas/lookup :no-such-schema "1"))))

(deftest register-rejects-an-invalid-entry-test
  (let [r (schemas/register! {:id :bad :version "1"})]
    (is (result/rejected? r))
    (is (= :invalid-entry (:category r)))))

(deftest registry-snapshot-and-reset-round-trip-test
  (let [before (schemas/registry-snapshot)]
    (schemas/register! {:id :temp-schema :version "1" :schema :any :docstring "d"})
    (is (some? (schemas/lookup :temp-schema "1")))
    (schemas/reset-registry! before)
    (is (nil? (schemas/lookup :temp-schema "1")))))

;; ---- seed catalog: at least one real, usable schema ----

(deftest seed-fhir-resource-shape-schema-is-registered-test
  (is (some? (schemas/lookup :fhir-resource-shape "1"))))

(deftest seed-fhir-resource-shape-validates-a-resource-with-resourcetype-test
  (let [entry (schemas/lookup :fhir-resource-shape "1")]
    (is (schemas/valid-against? entry {"resourceType" "Patient" "id" "p1"}))
    (is (not (schemas/valid-against? entry {"id" "p1"})))))
