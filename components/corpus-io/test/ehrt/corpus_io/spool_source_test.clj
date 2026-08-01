(ns ehrt.corpus-io.spool-source-test
  "Test-first (ruling 5, SS-3 Step 6): written before ehrt.corpus-io.
  spool-source existed. Hermetic throughout -- every :in-override
  is an injected ByteArrayInputStream, never real stdin; the real-pipe
  case (printf ... | bin/ehr corpus intake 'stdin:?...') is
  test-integration-tier (this step's own real-pipe test)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehrt.kernel.interface :as kernel]
            [ehrt.corpus-io.spool-source :as spool-source])
  (:import [java.io ByteArrayInputStream File]))

(defn- temp-dir-path
  []
  (let [f (File/createTempFile "spool-source-test" "")]
    (.delete f)
    (.getAbsolutePath f)))

(defn- stream
  [^String s]
  (ByteArrayInputStream. (.getBytes s "UTF-8")))

;; ---- needs-spooling? (the dispatch rule, ruling 5) ----

(deftest needs-spooling-test
  (testing "a :stdin source always needs spooling -- there is no directory yet"
    (is (spool-source/needs-spooling? {:kind :stdin}))
    (is (spool-source/needs-spooling? {:kind :stdin :framing :file-per-item})))
  (testing "a :file source needs spooling only when its framing isn't the identity framing"
    (is (not (spool-source/needs-spooling? {:kind :file :path "./x.hl7"})))
    (is (not (spool-source/needs-spooling? {:kind :file :path "./x.hl7" :framing :file-per-item})))
    (is (spool-source/needs-spooling? {:kind :file :path "./x.hl7" :framing :er7-multi})))
  (testing "a :dir source never needs spooling this session -- a directory of multi-item
            files is a recorded OPEN item, not silently supported (ruling 5)"
    (is (not (spool-source/needs-spooling? {:kind :dir :path "./x"})))))

;; ---- resolve! -- :stdin ----

(deftest resolve-stdin-happy-path-test
  (let [out-dir (temp-dir-path)
        r (spool-source/resolve! {:source {:kind :stdin :format :v2-er7 :framing :er7-multi}
                                   :captured-at "2026-07-28T00:00:00Z"
                                   :in-override (stream "MSH|^~\\&|A\n\n")
                                   :out-dir out-dir})]
    (is (kernel/ok? r))
    (is (= {:kind :dir :path out-dir} (:payload r)))
    (is (= "MSH|^~\\&|A" (slurp (io/file out-dir "item-0000.hl7"))))
    (is (.exists (io/file out-dir "capture-manifest.edn")))))

(deftest resolve-stdin-origin-is-stdin-test
  (let [out-dir (temp-dir-path)
        r (spool-source/resolve! {:source {:kind :stdin :format :v2-er7 :framing :er7-multi}
                                   :captured-at "2026-07-28T00:00:00Z"
                                   :in-override (stream "MSH|^~\\&|A\n\n")
                                   :out-dir out-dir})]
    (is (kernel/ok? r))
    (is (= "stdin" (:origin (clojure.edn/read-string (slurp (io/file out-dir "capture-manifest.edn"))))))))

;; ---- resolve! -- a framed :file source ----

(deftest resolve-file-with-non-default-framing-test
  (let [in-dir (temp-dir-path)
        out-dir (temp-dir-path)
        _ (.mkdirs (io/file in-dir))
        source-file (io/file in-dir "messages.hl7")
        _ (spit source-file "MSH|^~\\&|A\n\nMSH|^~\\&|B\n\n")
        r (spool-source/resolve! {:source {:kind :file :path (.getAbsolutePath source-file)
                                            :format :v2-er7 :framing :er7-multi}
                                   :captured-at "2026-07-28T00:00:00Z"
                                   :out-dir out-dir})]
    (is (kernel/ok? r))
    (is (= {:kind :dir :path out-dir} (:payload r)))
    (is (= "MSH|^~\\&|A" (slurp (io/file out-dir "item-0000.hl7"))))
    (is (= "MSH|^~\\&|B" (slurp (io/file out-dir "item-0001.hl7"))))
    (let [manifest (clojure.edn/read-string (slurp (io/file out-dir "capture-manifest.edn")))]
      (is (= (.getAbsolutePath source-file) (:origin manifest))))))

;; ---- propagation: the spool's own rejections pass through unchanged ----

(deftest resolve-propagates-spool-rejection-test
  (let [out-dir (temp-dir-path)
        r (spool-source/resolve! {:source {:kind :stdin :format :v2-er7 :framing :er7-multi}
                                   :captured-at "2026-07-28T00:00:00Z"
                                   :in-override (stream "no message here")
                                   :out-dir out-dir})]
    (is (kernel/rejected? r))
    (is (= :malformed-er7-multi-frame (:category r)))))
