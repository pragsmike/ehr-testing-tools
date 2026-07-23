(ns ehr-testing-tools.core-test
  (:require [clojure.test :refer [deftest is]]
            [ehr-testing-tools.core :as core]))

(deftest repo-info-test
  (let [info (core/repo-info)]
    (is (= "ehr-testing-tools" (:name info)))
    (is (re-matches #"\d+\.\d+\.\d+" (:clojure-version info))
        "clojure-version should be the exact-pinned version string from deps.edn")))
