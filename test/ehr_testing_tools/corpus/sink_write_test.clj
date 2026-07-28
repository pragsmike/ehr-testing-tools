(ns ehr-testing-tools.corpus.sink-write-test
  "Test-first (ADR-0006; ruling 8, SS-1 Step 6): written before
  ehr-testing-tools.corpus.sink-write existed. Plain write discipline
  only this session -- fail-if-exists is the default (D3); no
  :overwrite/:append yet (SS-4 Step 5); dir/file ManifestV1_1 sidecar
  emission is blocked on D-d (SS-4's manifest-interop STOP). SS-4 Step 3
  adds write-stdout! coverage below."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.corpus.source-sink :as ss]
            [ehr-testing-tools.corpus.sink-write :as write])
  (:import [java.io ByteArrayOutputStream File]))

(defn- temp-dir []
  (let [f (File/createTempFile "sink-write-test" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

;; ---- write-file! ----

(deftest write-file-happy-path-test
  (let [dir (temp-dir)
        target (str dir "/out.json")
        sink (:payload (ss/file-sink {:path target :format :fhir-json}))
        r (write/write-file! sink "hello")]
    (is (result/ok? r))
    (is (= target (:path (:payload r))))
    (is (= "hello" (slurp target)))))

(deftest write-file-creates-missing-parent-dirs-test
  (let [dir (temp-dir)
        target (str dir "/nested/deeper/out.json")
        sink (:payload (ss/file-sink {:path target :format :fhir-json}))
        r (write/write-file! sink "hello")]
    (is (result/ok? r))
    (is (= "hello" (slurp target)))))

(deftest write-file-fail-if-exists-test
  (let [dir (temp-dir)
        target (str dir "/out.json")
        sink (:payload (ss/file-sink {:path target :format :fhir-json}))]
    (spit target "already here")
    (let [r (write/write-file! sink "hello")]
      (is (result/rejected? r))
      (is (= :sink-target-exists (:category r)))
      (is (= "already here" (slurp target)) "the existing file must be left untouched"))))

(deftest write-file-rejects-a-non-file-sink-test
  (let [r (write/write-file! {:kind :dir :path "./x" :format :fhir-json} "hello")]
    (is (result/rejected? r))
    (is (= :invalid-sink (:category r)))))

;; ---- write-dir! ----

(deftest write-dir-happy-path-test
  (let [parent (temp-dir)
        target (str parent "/out")
        sink (:payload (ss/dir-sink {:path target :format :fhir-json}))
        r (write/write-dir! sink {"a.json" "AAA" "nested/b.json" "BBB"})]
    (is (result/ok? r))
    (is (= target (:path (:payload r))))
    (is (= "AAA" (slurp (io/file target "a.json"))))
    (is (= "BBB" (slurp (io/file target "nested" "b.json"))))))

(deftest write-dir-fail-if-exists-and-non-empty-test
  (let [parent (temp-dir)
        target (str parent "/out")
        sink (:payload (ss/dir-sink {:path target :format :fhir-json}))]
    (.mkdirs (io/file target))
    (spit (io/file target "already-here.json") "x")
    (let [r (write/write-dir! sink {"a.json" "AAA"})]
      (is (result/rejected? r))
      (is (= :sink-target-exists (:category r)))
      (is (not (.exists (io/file target "a.json")))
          "no partial write into an existing non-empty directory"))))

(deftest write-dir-into-an-existing-empty-dir-is-fine-test
  (let [parent (temp-dir)
        target (str parent "/out")
        sink (:payload (ss/dir-sink {:path target :format :fhir-json}))]
    (.mkdirs (io/file target))
    (let [r (write/write-dir! sink {"a.json" "AAA"})]
      (is (result/ok? r))
      (is (= "AAA" (slurp (io/file target "a.json")))))))

(deftest write-dir-rejects-a-non-dir-sink-test
  (let [r (write/write-dir! {:kind :file :path "./x" :format :fhir-json} {})]
    (is (result/rejected? r))
    (is (= :invalid-sink (:category r)))))

;; ---- write-stdout! (SS-4 Step 3) ----

(deftest write-stdout-default-framing-single-item-test
  (let [sink (:payload (ss/stdout-sink {:format :fhir-json}))
        out (ByteArrayOutputStream.)
        r (write/write-stdout! sink [(.getBytes "hello" "UTF-8")] :out out)]
    (is (result/ok? r))
    (is (= 5 (:bytes-written (:payload r))))
    (is (= "hello" (String. (.toByteArray out) "UTF-8")))))

(deftest write-stdout-mllp-framing-multiple-items-test
  (let [sink (:payload (ss/stdout-sink {:format :v2-er7 :framing :mllp}))
        out (ByteArrayOutputStream.)
        items [(.getBytes "MSH|^~\\&|A" "UTF-8") (.getBytes "MSH|^~\\&|B" "UTF-8")]
        r (write/write-stdout! sink items :out out)]
    (is (result/ok? r))
    (is (= (concat [0x0B] (map int "MSH|^~\\&|A") [0x1C 0x0D]
                   [0x0B] (map int "MSH|^~\\&|B") [0x1C 0x0D])
           (map #(bit-and 0xff %) (.toByteArray out))))))

(deftest write-stdout-defaults-to-system-out-test
  (testing "the :out arg is optional, defaulting to System/out -- not exercised by
            writing to the real stream here, only checked that the sink/framing
            validation runs before any write is attempted"
    (let [r (write/write-stdout! {:kind :dir :path "./x" :format :fhir-json} [])]
      (is (result/rejected? r))
      (is (= :invalid-sink (:category r))))))

(deftest write-stdout-propagates-framing-rejection-test
  (testing ":file-per-item (the default) requires exactly one item"
    (let [sink (:payload (ss/stdout-sink {:format :fhir-json}))
          out (ByteArrayOutputStream.)
          r (write/write-stdout! sink [] :out out)]
      (is (result/rejected? r))
      (is (= :invalid-item-count (:category r)))
      (is (= 0 (alength (.toByteArray out))) "nothing written when encode itself rejects"))))

(deftest write-stdout-rejects-a-non-stdout-sink-test
  (let [r (write/write-stdout! {:kind :dir :path "./x" :format :fhir-json} [])]
    (is (result/rejected? r))
    (is (= :invalid-sink (:category r)))))
