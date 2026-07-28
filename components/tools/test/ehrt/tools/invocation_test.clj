(ns ehrt.tools.invocation-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehrt.tools.result :as result]
            [ehrt.tools.invocation :as invocation])
  (:import [java.io File]))

(defn- temp-file [prefix]
  (.getAbsolutePath (File/createTempFile prefix ".log")))

(deftest run-captures-stdout-and-exit-code-test
  (let [out (temp-file "stdout")
        err (temp-file "stderr")
        r (invocation/run! {:command "sh"
                             :args ["-c" "echo hello"]
                             :stdout-path out
                             :stderr-path err})]
    (is (result/ok? r))
    (let [rec (:payload r)]
      (is (= 0 (:exit-code rec)))
      (is (= "hello\n" (slurp out)))
      (is (string? (:stdout-sha256 rec)))
      (is (= (:stdout-sha256 rec)
             (invocation/sha256-file out)))
      (is (invocation/valid? rec)))))

(deftest run-captures-nonzero-exit-test
  (let [out (temp-file "stdout")
        err (temp-file "stderr")
        r (invocation/run! {:command "sh"
                             :args ["-c" "exit 7"]
                             :stdout-path out
                             :stderr-path err})]
    (is (result/ok? r) "a nonzero exit is still a completed invocation, not a wrapper failure")
    (is (= 7 (:exit-code (:payload r))))))

(deftest run-spawn-failure-test
  (let [out (temp-file "stdout")
        err (temp-file "stderr")
        r (invocation/run! {:command "this-command-does-not-exist-xyz"
                             :args []
                             :stdout-path out
                             :stderr-path err})]
    (is (result/error? r))
    (is (= :spawn-failed (:category r)))))
