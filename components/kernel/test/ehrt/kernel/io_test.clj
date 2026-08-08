(ns ehrt.kernel.io-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehrt.kernel.result :as result]
            [ehrt.kernel.io :as kio])
  (:import [java.io File]))

(defn- temp-dir []
  (let [f (File/createTempFile "kernel-io" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

;; ---- list-files ----

(deftest list-files-returns-ok-vector-for-a-populated-dir-test
  (let [dir (temp-dir)]
    (spit (io/file dir "a.txt") "a")
    (spit (io/file dir "b.txt") "b")
    (let [r (kio/list-files dir)]
      (is (result/ok? r))
      (is (= #{"a.txt" "b.txt"} (set (map #(.getName ^File %) (:payload r))))))))

(deftest list-files-returns-ok-empty-vector-for-a-genuinely-empty-dir-test
  (let [r (kio/list-files (temp-dir))]
    (is (result/ok? r))
    (is (= [] (:payload r)))))

(deftest list-files-retries-once-before-erroring-test
  (let [calls (atom 0)
        real-files (into-array File [(io/file "sentinel")])
        lister (fn [_] (swap! calls inc) (when (> @calls 1) real-files))
        r (kio/list-files (temp-dir) lister)]
    (is (result/ok? r) "a nil-then-real result self-heals on the retry, same as ADR-0076's own AR-QR-2 idiom")
    (is (= 2 @calls))
    (is (= [(io/file "sentinel")] (:payload r)))))

(deftest list-files-errors-loud-when-still-nil-after-retry-test
  (let [dir (temp-dir)
        r (kio/list-files dir (fn [_] nil))]
    (is (result/error? r))
    (is (= :listing-failed (:category r)))
    (is (= dir (:path (:payload r))))))

;; ---- existing-dir-nonempty? ----

(deftest existing-dir-nonempty-is-ok-false-for-a-nonexistent-path-test
  (let [r (kio/existing-dir-nonempty? (str (temp-dir) "/does-not-exist"))]
    (is (result/ok? r))
    (is (false? (:payload r)))))

(deftest existing-dir-nonempty-is-ok-false-for-a-real-file-not-a-dir-test
  (let [dir (temp-dir)
        f (io/file dir "a-file")]
    (spit f "x")
    (let [r (kio/existing-dir-nonempty? f)]
      (is (result/ok? r))
      (is (false? (:payload r))))))

(deftest existing-dir-nonempty-is-ok-false-for-a-genuinely-empty-dir-test
  (let [r (kio/existing-dir-nonempty? (temp-dir))]
    (is (result/ok? r))
    (is (false? (:payload r)))))

(deftest existing-dir-nonempty-is-ok-true-for-a-populated-dir-test
  (let [dir (temp-dir)]
    (spit (io/file dir "a.txt") "a")
    (let [r (kio/existing-dir-nonempty? dir)]
      (is (result/ok? r))
      (is (true? (:payload r))))))

(deftest existing-dir-nonempty-errors-loud-on-a-listing-failure-instead-of-reading-safe-test
  (testing "the D3-4/D4-1 guard-defeat this helper exists to close: an I/O
            failure listing an EXISTING directory must never silently read
            as 'empty, safe to proceed'"
    (let [dir (temp-dir)
          r (kio/existing-dir-nonempty? dir (fn [_] nil))]
      (is (result/error? r))
      (is (= :listing-failed (:category r))))))

;; ---- rename! ----

(deftest rename-bang-moves-the-file-and-returns-ok-test
  (let [dir (temp-dir)
        src (io/file dir "src.txt")
        dest (io/file dir "dest.txt")]
    (spit src "content")
    (let [r (kio/rename! src dest)]
      (is (result/ok? r))
      (is (not (.exists src)))
      (is (.exists dest)))))

(deftest rename-bang-errors-loud-when-the-renamer-returns-false-test
  (let [dir (temp-dir)
        src (io/file dir "src.txt")
        dest (io/file dir "dest.txt")]
    (spit src "content")
    (let [r (kio/rename! src dest (fn [_ _] false))]
      (is (result/error? r))
      (is (= :rename-failed (:category r)))
      (is (.exists src) "a refused rename never claims success -- the source is untouched")
      (is (not (.exists dest))))))
