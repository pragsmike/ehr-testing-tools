(ns ehrt.corpus-io.spool-test
  "Test-first (ruling 4, SS-3 Step 5): written before ehrt.corpus-io.
  spool existed. Hermetic throughout -- every :in is an injected
  ByteArrayInputStream, never a real stdin or subprocess; the real-pipe
  case is test-integration-tier (Step 6)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.kernel.interface :as kernel]
            [ehrt.corpus-io.spool :as spool])
  (:import [java.io ByteArrayInputStream File]))

(defn- temp-dir-path
  "A fresh, not-yet-existing temp directory path -- spool! itself must
  create it, so this must not mkdirs."
  []
  (let [f (File/createTempFile "spool-test" "")]
    (.delete f)
    (.getAbsolutePath f)))

(defn- er7-multi-stream
  [messages]
  (ByteArrayInputStream.
   (.getBytes (str (clojure.string/join "\n\n" messages) "\n\n") "UTF-8")))

(defn- read-file-bytes
  [f]
  (java.nio.file.Files/readAllBytes (.toPath f)))

(defn- delete-tree!
  [path]
  (let [f (io/file path)]
    (when (.exists f)
      (when (.isDirectory f) (run! delete-tree! (.listFiles f)))
      (.delete f))))

;; ---- happy path ----

(deftest spool-happy-path-writes-one-file-per-item-plus-a-capture-manifest-test
  (let [out-dir (temp-dir-path)
        r (spool/spool! {:in (er7-multi-stream ["MSH|^~\\&|A" "MSH|^~\\&|B"])
                          :framing :er7-multi
                          :format :v2-er7
                          :origin "stdin"
                          :captured-at "2026-07-28T00:00:00Z"
                          :out-dir out-dir})]
    (is (kernel/ok? r))
    (is (= 2 (:item-count (:payload r))))
    (let [files (->> (.listFiles (io/file out-dir))
                     (map #(.getName %))
                     sort)]
      (is (= ["capture-manifest.edn" "item-0000.hl7" "item-0001.hl7"] files)))
    (is (= "MSH|^~\\&|A" (slurp (io/file out-dir "item-0000.hl7"))))
    (is (= "MSH|^~\\&|B" (slurp (io/file out-dir "item-0001.hl7"))))
    (let [manifest (edn/read-string (slurp (io/file out-dir "capture-manifest.edn")))]
      (is (= "2026-07-28T00:00:00Z" (:captured-at manifest)))
      (is (= "stdin" (:origin manifest)))
      (is (= :er7-multi (:framing manifest)))
      (is (= :v2-er7 (:format manifest)))
      (is (= 2 (:item-count manifest)))
      (is (= 2 (count (:items manifest))))
      (is (= "item-0000.hl7" (:file (first (:items manifest)))))
      (is (= (kernel/sha256-bytes (read-file-bytes (io/file out-dir "item-0000.hl7")))
             (:sha256 (first (:items manifest))))))))

;; ---- default out-dir derivation ----

(deftest spool-default-out-dir-is-derived-from-captured-at-test
  (let [expected-dir "out/spool/2026-07-28T01-02-03Z"]
    (delete-tree! expected-dir) ;; idempotent across repeated runs -- this default is a fixed, derived path
    (let [r (spool/spool! {:in (er7-multi-stream ["MSH|^~\\&|A"])
                            :framing :er7-multi
                            :format :v2-er7
                            :origin "stdin"
                            :captured-at "2026-07-28T01-02-03Z"})]
      (is (kernel/ok? r))
      (is (= expected-dir (:out-dir (:payload r))))
      (delete-tree! expected-dir))))

;; ---- fail-if-exists ----

(deftest spool-rejects-a-pre-existing-non-empty-out-dir-test
  (let [out-dir (temp-dir-path)]
    (.mkdirs (io/file out-dir))
    (spit (io/file out-dir "leftover.txt") "from a previous run")
    (let [r (spool/spool! {:in (er7-multi-stream ["MSH|^~\\&|A"])
                            :framing :er7-multi :format :v2-er7
                            :origin "stdin" :captured-at "2026-07-28T00:00:00Z"
                            :out-dir out-dir})]
      (is (kernel/rejected? r))
      (is (= :spool-target-exists (:category r)))
      (is (= ["leftover.txt"] (map #(.getName %) (.listFiles (io/file out-dir))))
          "the pre-existing directory is left untouched"))))

;; ---- the cap (ruling 4/D5): a rejection, never a truncated corpus
;; dressed as success -- no partial spool ever lands on disk ----

(deftest spool-cap-exceeded-leaves-no-partial-spool-on-disk-test
  (let [out-dir (temp-dir-path)
        r (spool/spool! {:in (er7-multi-stream ["MSH|^~\\&|" (apply str (repeat 1000 "X"))])
                          :framing :er7-multi :format :v2-er7
                          :origin "stdin" :captured-at "2026-07-28T00:00:00Z"
                          :out-dir out-dir
                          :max-bytes 100})]
    (is (kernel/rejected? r))
    (is (= :spool-cap-exceeded (:category r)))
    (is (= 100 (:max-bytes (:payload r))))
    (is (not (.exists (io/file out-dir)))
        "no truncated corpus is left behind when the cap is exceeded")))

(deftest spool-default-cap-is-1-gib-test
  (is (= (* 1024 1024 1024) spool/default-max-bytes)))

(deftest spool-under-cap-with-explicit-override-succeeds-test
  (let [out-dir (temp-dir-path)
        big-message (str "MSH|^~\\&|" (apply str (repeat 1000 "X")))
        r (spool/spool! {:in (er7-multi-stream [big-message])
                          :framing :er7-multi :format :v2-er7
                          :origin "stdin" :captured-at "2026-07-28T00:00:00Z"
                          :out-dir out-dir
                          :max-bytes 10000})]
    (is (kernel/ok? r))
    (is (= 1 (:item-count (:payload r))))))

;; ---- malformed framing propagates; nothing is written ----

(deftest spool-propagates-a-malformed-framing-rejection-and-writes-nothing-test
  (let [out-dir (temp-dir-path)
        r (spool/spool! {:in (ByteArrayInputStream. (.getBytes "no message here" "UTF-8"))
                          :framing :er7-multi :format :v2-er7
                          :origin "stdin" :captured-at "2026-07-28T00:00:00Z"
                          :out-dir out-dir})]
    (is (kernel/rejected? r))
    (is (= :malformed-er7-multi-frame (:category r)))
    (is (not (.exists (io/file out-dir))))))

;; ---- :bundle-entries materializes one JSON file per resource ----

(deftest spool-bundle-entries-writes-one-json-file-per-resource-test
  (let [out-dir (temp-dir-path)
        bundle-json (str "{\"resourceType\":\"Bundle\",\"type\":\"collection\",\"entry\":"
                          "[{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"1\"}},"
                          "{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"2\"}}]}")
        r (spool/spool! {:in (ByteArrayInputStream. (.getBytes bundle-json "UTF-8"))
                          :framing :bundle-entries :format :fhir-json
                          :origin "./bundle.json" :captured-at "2026-07-28T00:00:00Z"
                          :out-dir out-dir})]
    (is (kernel/ok? r))
    (is (= 2 (:item-count (:payload r))))
    (is (= ["capture-manifest.edn" "item-0000.json" "item-0001.json"]
           (sort (map #(.getName %) (.listFiles (io/file out-dir))))))
    (is (clojure.string/includes? (slurp (io/file out-dir "item-0000.json")) "\"id\":\"1\""))))
