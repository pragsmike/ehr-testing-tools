(ns ehr-testing-tools.corpus.sink-write-test
  "Test-first (ADR-0006; ruling 8, SS-1 Step 6): written before
  ehr-testing-tools.corpus.sink-write existed. Plain write discipline
  only this session -- fail-if-exists is the default (D3); no
  :overwrite/:append yet (SS-4 Step 5); dir/file ManifestV1_1 sidecar
  emission is blocked on D-d (SS-4's manifest-interop STOP). SS-4 Step 3
  adds write-stdout! coverage below. SS-4b Step 3 (D-d resolved, ADR-0020)
  adds :operation-manifest coverage for write-dir!/write-file!, test-first
  again -- written before either function accepts the new kwarg."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehr-testing-tools.digest :as digest]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.corpus.operation-manifest :as om]
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

;; ---- write-file! write discipline (SS-4 Step 5, ruling 7): :mode
;; defaults to :fail-if-exists (every test above); :overwrite is
;; explicit and destructive; :append is sound only where the sink's own
;; :framing concatenates (:er7-multi/:ndjson/:mllp), rejected
;; :append-unsound otherwise (:bundle-entries, or the :file-per-item
;; default -- single-item semantics don't support append at all). ----

(deftest write-file-overwrite-replaces-existing-content-test
  (let [dir (temp-dir)
        target (str dir "/out.json")
        sink (:payload (ss/file-sink {:path target :format :fhir-json}))]
    (spit target "old")
    (let [r (write/write-file! sink "new" :mode :overwrite)]
      (is (result/ok? r))
      (is (= "new" (slurp target))))))

(deftest write-file-overwrite-on-a-missing-file-is-a-plain-create-test
  (let [dir (temp-dir)
        target (str dir "/out.json")
        sink (:payload (ss/file-sink {:path target :format :fhir-json}))
        r (write/write-file! sink "hello" :mode :overwrite)]
    (is (result/ok? r))
    (is (= "hello" (slurp target)))))

(deftest write-file-append-sound-framings-concatenate-onto-an-existing-file-test
  (doseq [framing [:er7-multi :ndjson :mllp]]
    (testing (name framing)
      (let [dir (temp-dir)
            target (str dir "/out.dat")
            sink (:payload (ss/file-sink {:path target :format :v2-er7 :framing framing}))]
        (spit target "AAA")
        (let [r (write/write-file! sink "BBB" :mode :append)]
          (is (result/ok? r))
          (is (= "AAABBB" (slurp target))))))))

(deftest write-file-append-onto-a-missing-file-creates-it-test
  (let [dir (temp-dir)
        target (str dir "/out.dat")
        sink (:payload (ss/file-sink {:path target :format :v2-er7 :framing :ndjson}))
        r (write/write-file! sink "AAA" :mode :append)]
    (is (result/ok? r))
    (is (= "AAA" (slurp target)))))

(deftest write-file-append-rejects-bundle-entries-as-unsound-test
  (let [dir (temp-dir)
        target (str dir "/out.json")
        sink (:payload (ss/file-sink {:path target :format :fhir-json :framing :bundle-entries}))]
    (spit target "{}")
    (let [r (write/write-file! sink "{}" :mode :append)]
      (is (result/rejected? r))
      (is (= :append-unsound (:category r)))
      (is (= "{}" (slurp target)) "nothing written when append itself is rejected as unsound"))))

(deftest write-file-append-rejects-default-file-per-item-framing-as-unsound-test
  (let [dir (temp-dir)
        target (str dir "/out.json")
        sink (:payload (ss/file-sink {:path target :format :fhir-json}))]
    (let [r (write/write-file! sink "hello" :mode :append)]
      (is (result/rejected? r))
      (is (= :append-unsound (:category r))))))

(deftest write-file-rejects-an-unknown-mode-test
  (let [dir (temp-dir)
        sink (:payload (ss/file-sink {:path (str dir "/out.json") :format :fhir-json}))
        r (write/write-file! sink "hello" :mode :not-a-real-mode)]
    (is (result/rejected? r))
    (is (= :invalid-write-mode (:category r)))))

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

;; ---- write-dir! write discipline (SS-4 Step 5, ruling 7): :overwrite
;; writes into a non-empty existing directory; :append is REJECTED
;; :append-unsound unconditionally this session -- append-to-corpus
;; means manifest merge, an OPEN item (docs/source-sink-design.md),
;; not improvised here regardless of framing. ----

(deftest write-dir-overwrite-writes-into-a-non-empty-existing-dir-test
  (let [parent (temp-dir)
        target (str parent "/out")
        sink (:payload (ss/dir-sink {:path target :format :fhir-json}))]
    (.mkdirs (io/file target))
    (spit (io/file target "already-here.json") "x")
    (let [r (write/write-dir! sink {"a.json" "AAA"} :mode :overwrite)]
      (is (result/ok? r))
      (is (= "AAA" (slurp (io/file target "a.json"))))
      (is (.exists (io/file target "already-here.json"))
          "overwrite only affects the files this call names, not a directory wipe"))))

(deftest write-dir-overwrite-replaces-a-named-file-that-already-exists-test
  (let [parent (temp-dir)
        target (str parent "/out")
        sink (:payload (ss/dir-sink {:path target :format :fhir-json}))]
    (.mkdirs (io/file target))
    (spit (io/file target "a.json") "old")
    (let [r (write/write-dir! sink {"a.json" "new"} :mode :overwrite)]
      (is (result/ok? r))
      (is (= "new" (slurp (io/file target "a.json")))))))

(deftest write-dir-append-is-rejected-unsound-unconditionally-test
  (let [parent (temp-dir)
        target (str parent "/out")
        sink (:payload (ss/dir-sink {:path target :format :fhir-json}))
        r (write/write-dir! sink {"a.json" "AAA"} :mode :append)]
    (is (result/rejected? r))
    (is (= :append-unsound (:category r)))
    (is (not (.exists (io/file target))) "nothing written -- append is rejected before any write")))

(deftest write-dir-rejects-an-unknown-mode-test
  (let [parent (temp-dir)
        sink (:payload (ss/dir-sink {:path (str parent "/out") :format :fhir-json}))
        r (write/write-dir! sink {"a.json" "AAA"} :mode :not-a-real-mode)]
    (is (result/rejected? r))
    (is (= :invalid-write-mode (:category r)))))

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

;; ---- :operation-manifest (SS-4b Step 3, D-d resolved via ADR-0020) ----

(def ^:private producer
  {:name "ehr-testing-tools" :identity "pre-release" :git "abc1234-dirty"})

(def ^:private operation
  {:kind :mutate :operator-id :blank-required-field :operator-version "1"
   :locator {:format :v2 :path "MSH-9"}})

(deftest write-dir-with-no-operation-manifest-writes-nothing-extra-test
  (let [parent (temp-dir)
        target (str parent "/out")
        sink (:payload (ss/dir-sink {:path target :format :v2-er7}))
        r (write/write-dir! sink {"a.hl7" "AAA"})]
    (is (result/ok? r))
    (is (not (.exists (io/file target "operation-manifest.edn")))
        "absent :operation-manifest is a no-op -- backward compatible with every SS-4 write-dir! caller")))

(deftest write-dir-with-explicit-items-writes-operation-manifest-last-test
  (let [parent (temp-dir)
        target (str parent "/out")
        sink (:payload (ss/dir-sink {:path target :format :v2-er7}))
        sha (digest/sha256-string "AAA")
        r (write/write-dir! sink {"a.hl7" "AAA"}
                             :operation-manifest {:producer producer
                                                   :operation operation
                                                   :written-at "2026-07-28"
                                                   :items [{:name "a.hl7" :sha256 sha}]})]
    (is (result/ok? r))
    (is (= "AAA" (slurp (io/file target "a.hl7"))))
    (let [manifest (edn/read-string (slurp (io/file target "operation-manifest.edn")))]
      (is (om/valid? manifest))
      (is (= producer (:producer manifest)))
      (is (= operation (:operation manifest)))
      (is (= :v2-er7 (:format manifest)))
      (is (= :file-per-item (:framing manifest))
          "the sink's own default framing, read off the sink, never re-declared by the caller")
      (is (= [{:name "a.hl7" :sha256 sha}] (:items manifest))))))

(deftest write-dir-derives-items-from-files-when-not-given-explicitly-test
  (let [parent (temp-dir)
        target (str parent "/out")
        sink (:payload (ss/dir-sink {:path target :format :fhir-json}))
        r (write/write-dir! sink {"a.json" "AAA" "b.json" "BBB"}
                             :operation-manifest {:producer producer
                                                   :operation operation
                                                   :written-at "2026-07-28"
                                                   :input-hashes {"a.json" (digest/sha256-string "parent-A")}})]
    (is (result/ok? r))
    (let [manifest (edn/read-string (slurp (io/file target "operation-manifest.edn")))
          by-name (into {} (map (juxt :name identity)) (:items manifest))]
      (is (om/valid? manifest))
      (is (= (digest/sha256-string "AAA") (:sha256 (get by-name "a.json"))))
      (is (= (digest/sha256-string "parent-A") (:input-hash (get by-name "a.json"))))
      (is (not (contains? (get by-name "b.json") :input-hash))
          "input-hash present only where the caller actually supplied one"))))

(deftest write-dir-operation-manifest-is-honored-under-overwrite-with-empty-files-test
  (testing "mutate-command's own split-write shape: files already landed via a prior progressive loop, this call names none but still emits the manifest"
    (let [parent (temp-dir)
          target (str parent "/out")
          sink (:payload (ss/dir-sink {:path target :format :v2-er7}))]
      (.mkdirs (io/file target))
      (spit (io/file target "a.hl7") "AAA")
      (let [r (write/write-dir! sink {} :mode :overwrite
                                 :operation-manifest {:producer producer
                                                       :operation operation
                                                       :written-at "2026-07-28"
                                                       :items [{:name "a.hl7" :sha256 (digest/sha256-string "AAA")}]})]
        (is (result/ok? r))
        (is (.exists (io/file target "operation-manifest.edn")))))))

(deftest write-file-with-operation-manifest-writes-a-sibling-manifest-test
  (let [dir (temp-dir)
        target (str dir "/out.hl7")
        sink (:payload (ss/file-sink {:path target :format :v2-er7}))
        r (write/write-file! sink "AAA"
                              :operation-manifest {:producer producer :operation operation
                                                    :written-at "2026-07-28"})]
    (is (result/ok? r))
    (let [manifest (edn/read-string (slurp (io/file dir "operation-manifest.edn")))]
      (is (om/valid? manifest))
      (is (= [{:name "out.hl7" :sha256 (digest/sha256-string "AAA")}] (:items manifest))))))

(deftest write-file-with-operation-manifest-preserves-input-hash-when-given-test
  (let [dir (temp-dir)
        target (str dir "/out.hl7")
        sink (:payload (ss/file-sink {:path target :format :v2-er7}))
        parent-hash (digest/sha256-string "parent")
        r (write/write-file! sink "AAA"
                              :operation-manifest {:producer producer :operation operation
                                                    :written-at "2026-07-28"
                                                    :input-hash parent-hash})]
    (is (result/ok? r))
    (let [manifest (edn/read-string (slurp (io/file dir "operation-manifest.edn")))]
      (is (= parent-hash (:input-hash (first (:items manifest))))))))
