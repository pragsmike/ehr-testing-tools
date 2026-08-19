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

;; ---- mkdirs! / delete! / delete-quietly! (ADR-0157, review-4 D4-1) ----

(deftest mkdirs-bang-creates-a-new-path-including-parents-test
  (let [dir (io/file (temp-dir) "a" "b" "c")]
    (is (not (.exists dir)))
    (let [f (kio/mkdirs! dir)]
      (is (.isDirectory dir))
      (is (= (.getPath dir) (.getPath ^File f)) "returns the directory it ensured"))))

(deftest mkdirs-bang-is-ok-on-a-directory-that-already-exists-test
  ;; The whole reason the bare boolean was discarded at thirteen sites:
  ;; `.mkdirs` answers false here, and false here is correct. A helper
  ;; that threw on false would be unusable and would have been routed
  ;; around within a session.
  (let [dir (io/file (temp-dir))]
    (is (.isDirectory dir))
    (is (false? (.mkdirs dir)) "sanity: java itself reports false for an existing directory")
    (is (some? (kio/mkdirs! dir)) "and mkdirs! must read that false as the success it is")))

(deftest mkdirs-bang-throws-and-names-the-path-when-the-parent-is-a-file-test
  (let [root (temp-dir)
        blocker (io/file root "not-a-dir")]
    (spit blocker "i am a file")
    (let [target (io/file blocker "child")
          e (try (kio/mkdirs! target) nil (catch clojure.lang.ExceptionInfo e e))]
      (is (some? e)
          "a directory that CANNOT be created must fail loud -- silently continuing yields a FileNotFoundException from whatever writes next, naming a path nobody chose")
      (is (= :mkdirs-failed (:error (ex-data e))))
      (is (= (.getPath target) (:path (ex-data e)))
          "the failure names the artifact (rulings.md#R-errors-name-artifact)"))))

(deftest delete-bang-removes-an-existing-file-test
  (let [f (io/file (temp-dir) "gone.txt")]
    (spit f "x")
    (is (.exists f))
    (kio/delete! f)
    (is (not (.exists f)))))

(deftest delete-bang-is-ok-on-a-path-that-does-not-exist-test
  ;; Stated rather than inferred: `.delete` returns false for a missing
  ;; file, but this helper's contract is the POSTCONDITION -- the path
  ;; does not exist afterwards -- and a missing file already satisfies
  ;; it. Deleting twice is not an error.
  (let [f (io/file (temp-dir) "never-existed.txt")]
    (is (not (.exists f)))
    (is (some? (kio/delete! f)) "a no-op deletion is a success, not a failure")))

(deftest delete-bang-throws-and-names-the-path-on-a-non-empty-directory-test
  (let [dir (io/file (temp-dir) "populated")]
    (.mkdirs dir)
    (spit (io/file dir "child.txt") "x")
    (let [e (try (kio/delete! dir) nil (catch clojure.lang.ExceptionInfo e e))]
      (is (some? e)
          "`.delete` always refuses a non-empty directory and says so only through a boolean nobody read")
      (is (= :delete-failed (:error (ex-data e))))
      (is (= (.getPath dir) (:path (ex-data e))))
      (is (.isDirectory dir) "and the directory is untouched, so the throw is the only signal"))))

(deftest delete-quietly-bang-reports-its-outcome-and-never-throws-test
  (let [root (temp-dir)
        f (io/file root "gone.txt")
        populated (io/file root "populated")]
    (spit f "x")
    (is (true? (kio/delete-quietly! f)) "a successful deletion reports true")
    (is (true? (kio/delete-quietly! f)) "and a path that is already absent reports true too")
    (.mkdirs populated)
    (spit (io/file populated "child.txt") "x")
    (is (false? (kio/delete-quietly! populated))
        "a refused deletion reports false rather than throwing -- the declared exception, for cleanup after an already-diagnosed failure")
    (is (.isDirectory populated))))
