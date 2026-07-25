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

;; ---- worst-of: the Judge kind's composition law, now over four
;; values (ADR-0010): :no-verdict > :rejected > :indeterminate > :pass ----

(deftest worst-of-empty-is-pass-test
  (is (= :pass (finding/worst-of []))))

(deftest worst-of-all-pass-is-pass-test
  (is (= :pass (finding/worst-of [:pass :pass]))))

(deftest worst-of-any-indeterminate-beats-pass-test
  (is (= :indeterminate (finding/worst-of [:pass :indeterminate :pass]))))

(deftest worst-of-any-rejected-beats-everything-below-it-test
  (is (= :rejected (finding/worst-of [:pass :indeterminate :rejected])))
  (is (= :rejected (finding/worst-of [:rejected])))
  (is (= :rejected (finding/worst-of [:rejected :indeterminate]))))

(deftest worst-of-no-verdict-alone-is-no-verdict-test
  (is (= :no-verdict (finding/worst-of [:no-verdict]))))

(deftest worst-of-no-verdict-beats-rejected-test
  (is (= :no-verdict (finding/worst-of [:rejected :no-verdict]))
      "a corpus that couldn't be fully judged is not a corpus that passed"))

(deftest worst-of-no-verdict-beats-all-other-three-test
  (is (= :no-verdict (finding/worst-of [:pass :indeterminate :rejected :no-verdict]))))

;; ---- the no-verdict/cause pairing schema (ADR-0010, O2): a Malli
;; schema enforcing :cause is present if and only if verdict is
;; :no-verdict ----

(deftest verdict-outcome-no-verdict-with-cause-is-valid-test
  (is (finding/valid-cause-pairing? :no-verdict :terminology-suppressed)))

(deftest verdict-outcome-no-verdict-without-cause-is-invalid-test
  (is (not (finding/valid-cause-pairing? :no-verdict nil))))

(deftest verdict-outcome-cause-on-a-non-no-verdict-verdict-is-invalid-test
  (is (not (finding/valid-cause-pairing? :rejected :terminology-suppressed)))
  (is (not (finding/valid-cause-pairing? :pass :terminology-suppressed)))
  (is (not (finding/valid-cause-pairing? :indeterminate :terminology-suppressed))))

(deftest verdict-outcome-plain-verdicts-without-cause-are-valid-test
  (is (finding/valid-cause-pairing? :pass nil))
  (is (finding/valid-cause-pairing? :rejected nil))
  (is (finding/valid-cause-pairing? :indeterminate nil)))
