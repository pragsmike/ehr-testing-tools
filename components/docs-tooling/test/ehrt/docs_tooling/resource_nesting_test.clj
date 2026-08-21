(ns ehrt.docs-tooling.resource-nesting-test
  "Register rows S1/C-1 / AR-F3-2 (alignment fixes 3, 2026-08-05,
  `notes/adr/0052-alignment-fixes-3.md`): every `components/*/resources`
  directory that exists must nest its files under exactly one top-level
  entry, a directory named after its own brick -- the convention six of
  the seven such directories already followed by construction
  (`judge`, `kernel`, `provenance`, `sim-engine`, `patient-simulator`,
  `sim`), with `sim-model` the sole, disclosed violator: its resources
  sat under `resources/sim/`, not `resources/sim-model/`, a tolerance
  ADR-0025 disclosed once at the S1 sim-split (`sim-model`'s own
  `io/resource` calls kept the pre-split `sim/demographics/...` path so
  the move needed no edit) but never ruled permanent. That tolerance
  closes in the same commit this gate lands in -- this test proves the
  rule holds AFTER the rename, and (per its own red-witnessed history,
  the session record) proves the OLD layout was a genuine violation
  before it.

  No allowlist: a brick whose `resources/` directory nests anything
  other than one directory named for the brick itself fails this gate,
  full stop -- the point is that the next drift, whatever brick it
  hits, is caught structurally rather than needing another audit to
  notice."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]))

(defn- component-dirs []
  (->> (.listFiles (io/file "components"))
       (filter #(.isDirectory %))
       sort))

(defn- resources-dir [component-dir]
  (let [d (io/file component-dir "resources")]
    (when (.isDirectory d) d)))

(defn- top-level-entries [dir]
  (->> (.listFiles dir) (map #(.getName %)) sort))

(defn- conforms?
  "A `resources/` dir conforms when it has exactly one top-level entry,
  itself a directory, named after `brick-name`."
  [brick-name resources-dir]
  (let [entries (.listFiles resources-dir)]
    (and (= 1 (count entries))
         (.isDirectory (first entries))
         (= brick-name (.getName (first entries))))))

(deftest every-component-resources-dir-nests-under-its-own-brick-name-test
  (doseq [component-dir (component-dirs)]
    (when-let [res-dir (resources-dir component-dir)]
      (let [brick-name (.getName component-dir)]
        (is (conforms? brick-name res-dir)
            (str "components/" brick-name "/resources top-level entries "
                 (top-level-entries res-dir)
                 " -- expected exactly one directory named \"" brick-name "\""))))))

;; -- mechanism-sanity: prove conforms? actually catches what it claims to --

(deftest conforms-predicate-is-actually-caught-test
  (let [tmp (io/file (System/getProperty "java.io.tmpdir")
                      (str "resource-nesting-test-" (System/nanoTime)))]
    (try
      (testing "a single correctly-named subdirectory conforms"
        (let [res (io/file tmp "conforming" "resources")
              nested (io/file res "conforming")]
          (.mkdirs nested)
          (is (true? (conforms? "conforming" res)))))
      (testing "a single WRONGLY-named subdirectory (the pre-rename sim-model shape) does not conform"
        (let [res (io/file tmp "sim-model" "resources")
              nested (io/file res "sim")]
          (.mkdirs nested)
          (is (false? (conforms? "sim-model" res)))))
      (testing "more than one top-level entry does not conform"
        (let [res (io/file tmp "multi" "resources")]
          (.mkdirs (io/file res "multi"))
          (.mkdirs (io/file res "extra"))
          (is (false? (conforms? "multi" res)))))
      (testing "a top-level file (not a directory) does not conform"
        (let [res (io/file tmp "filecase" "resources")]
          (.mkdirs res)
          (spit (io/file res "filecase") "not a directory")
          (is (false? (conforms? "filecase" res))))
        )
      (finally
        (doseq [f (reverse (file-seq tmp))] (.delete f))))))
