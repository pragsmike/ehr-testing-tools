(ns ehr-testing-tools.corpus.manifest-test
  (:require [clojure.test :refer [deftest is]]
            [ehr-testing-tools.corpus.manifest :as manifest]))

(def sample-fields
  {:generator {:name "synthea" :version "4.0.0" :sha256 (apply str (repeat 64 "a"))}
   :seed 42
   :config {:path "config/synthea/synthea.properties" :sha256 (apply str (repeat 64 "b"))}
   :invocation {:command "java" :args ["-jar" "synthea.jar"] :exit-code 0}
   :canonicalizers-applied [[:strip-timestamps "1"]]
   :environment {:locale "en-US" :timezone "UTC" :jvm-version "21"}})

(deftest build-produces-schema-version-0-test
  (let [m (manifest/build sample-fields)]
    (is (= 0 (:schema-version m)))
    (is (manifest/valid? m))))

(deftest build-defaults-canonicalizers-applied-to-empty-test
  (let [m (manifest/build (dissoc sample-fields :canonicalizers-applied))]
    (is (= [] (:canonicalizers-applied m)))
    (is (manifest/valid? m))))

(deftest valid-rejects-missing-required-fields-test
  (is (not (manifest/valid? (dissoc (manifest/build sample-fields) :seed))))
  (is (not (manifest/valid? (dissoc (manifest/build sample-fields) :generator))))
  (is (not (manifest/valid? {:schema-version 0}))))

(deftest valid-rejects-wrong-schema-version-test
  (is (not (manifest/valid? (assoc (manifest/build sample-fields) :schema-version 1)))))
