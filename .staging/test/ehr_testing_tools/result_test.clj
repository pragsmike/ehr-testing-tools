(ns ehr-testing-tools.result-test
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-tools.result :as result]))

(deftest ok-test
  (let [r (result/ok {:x 1})]
    (is (result/ok? r))
    (is (not (result/rejected? r)))
    (is (not (result/error? r)))
    (is (= {:x 1} (:payload r)))
    (is (result/valid? r))))

(deftest rejected-test
  (let [r (result/rejected :hash-mismatch {:expected "a" :actual "b"})]
    (is (result/rejected? r))
    (is (not (result/ok? r)))
    (is (= :hash-mismatch (:category r)))
    (is (= {:expected "a" :actual "b"} (:payload r)))
    (is (result/valid? r))))

(deftest error-test
  (let [r (result/error :spawn-failed {:message "boom"})]
    (is (result/error? r))
    (is (not (result/ok? r)))
    (is (= :spawn-failed (:category r)))
    (is (result/valid? r))))

(deftest invalid-shape-test
  (is (not (result/valid? {:status :bogus})))
  (is (not (result/valid? "not even a map"))))
