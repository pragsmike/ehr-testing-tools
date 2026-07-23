(ns ehr-testing-tools.digest-test
  (:require [clojure.test :refer [deftest is]]
            [ehr-testing-tools.digest :as digest])
  (:import [java.io File]))

(deftest sha256-file-matches-known-vector-test
  ;; sha256("") = e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855
  (let [f (File/createTempFile "digest-test" ".empty")]
    (is (= "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
           (digest/sha256-file (.getAbsolutePath f))))))

(deftest sha256-file-is-deterministic-and-content-sensitive-test
  (let [f1 (File/createTempFile "digest-test" ".a")
        f2 (File/createTempFile "digest-test" ".b")]
    (spit f1 "hello")
    (spit f2 "hello world")
    (is (= (digest/sha256-file (.getAbsolutePath f1))
           (digest/sha256-file (.getAbsolutePath f1))))
    (is (not= (digest/sha256-file (.getAbsolutePath f1))
              (digest/sha256-file (.getAbsolutePath f2))))))
