(ns ehr-testing-tools.locator-test
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.locator :as locator]))

(deftest make-valid-locator-test
  (let [r (locator/make :fhir "Patient.name")]
    (is (result/ok? r))
    (is (= {:format :fhir :path "Patient.name"} (:payload r)))
    (is (locator/valid? (:payload r)))))

(deftest make-rejects-unknown-format-test
  (let [r (locator/make :bogus "x")]
    (is (result/rejected? r))
    (is (= :invalid-locator (:category r)))))

(deftest make-rejects-empty-path-test
  (let [r (locator/make :fhir "")]
    (is (result/rejected? r))
    (is (= :invalid-locator (:category r)))))

(deftest valid-schema-check-test
  (is (locator/valid? {:format :v2 :path "PID-3"}))
  (is (not (locator/valid? {:format :v2 :path ""})))
  (is (not (locator/valid? {:format :not-a-format :path "x"})))
  (is (not (locator/valid? "garbage"))))

(deftest known-formats-test
  (is (contains? locator/known-formats :fhir))
  (is (contains? locator/known-formats :v2))
  (is (contains? locator/known-formats :table))
  (is (contains? locator/known-formats :xpath)))
