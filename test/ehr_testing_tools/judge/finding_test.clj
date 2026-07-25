(ns ehr-testing-tools.judge.finding-test
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-tools.judge.finding :as finding]))

(defn- sample-finding
  [severity]
  {:severity severity :code "test-code"
   :locator {:format :fhir :path "Patient.gender"}
   :message "a message"
   :engine {:name "test-engine" :version "1.0"}})

(deftest valid-finding-passes-schema-test
  (is (finding/valid? (sample-finding :error))))

(deftest invalid-severity-fails-schema-test
  (is (not (finding/valid? (assoc (sample-finding :error) :severity :bogus)))))

(deftest native-ref-is-optional-test
  (is (finding/valid? (assoc (sample-finding :warning) :native-ref {:issue-index 0}))))

;; ---- worst-of: the Gate kind's ternary composition law ----

(deftest worst-of-empty-is-pass-test
  (is (= :pass (finding/worst-of []))))

(deftest worst-of-all-pass-is-pass-test
  (is (= :pass (finding/worst-of [:pass :pass]))))

(deftest worst-of-any-indeterminate-beats-pass-test
  (is (= :indeterminate (finding/worst-of [:pass :indeterminate :pass]))))

(deftest worst-of-any-rejected-beats-everything-test
  (is (= :rejected (finding/worst-of [:pass :indeterminate :rejected])))
  (is (= :rejected (finding/worst-of [:rejected])))
  (is (= :rejected (finding/worst-of [:rejected :indeterminate]))))
