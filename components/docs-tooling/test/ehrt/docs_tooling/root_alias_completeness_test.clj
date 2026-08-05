(ns ehrt.docs-tooling.root-alias-completeness-test
  "Register row A-5 / AR-F2-4 (alignment fixes 2, 2026-08-05,
  `notes/adr/0051-alignment-fixes-2.md`): A-5's own scripted audit
  confirmed, by hand, that the root `deps.edn`'s `:dev` alias's
  `:local/root` brick set and its `:test` alias's `:extra-paths` set
  both mirror the real `components/*`/`bases/*` tree exactly, with zero
  drift in either direction -- but nothing GATES that mapping, so a
  future brick addition (or removal) could silently miss a root alias
  entry and nothing would fail the build. This test promotes A-5's own
  one-off scripted check to a standing gate, bidirectional both ways it
  checked:

  1. `:dev`'s `:local/root` entries <-> every `components/*`/`bases/*`
     directory on disk -- exact set equality, both directions.
  2. Every real `components/*/test` and `bases/*/test` directory is
     listed in `:test`'s `:extra-paths` (brick side, one direction --
     A-5's own note: project test dirs like `projects/*/test` are
     allowed listings verified for existence, never required from the
     brick side, since no brick owns them). Every `:extra-paths` entry,
     brick or project, must exist on disk (the reverse direction,
     covering ghosts from either source).

  Reads `deps.edn` as EDN via `clojure.edn/read-string`, never by grep
  -- the file's own extensive comments (dated provenance notes on
  nearly every entry) would make a text-based scan fragile against
  exactly the kind of incidental drift this gate exists to catch."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.set :as set]))

(defn- deps-edn [] (edn/read-string (slurp "deps.edn")))

(defn- dev-local-roots
  "The `:local/root` value of every `:dev` alias `:extra-deps` entry."
  [deps]
  (->> (get-in deps [:aliases :dev :extra-deps])
       vals
       (keep :local/root)
       set))

(defn- real-brick-dirs
  "Every real `components/<name>` and `bases/<name>` directory, as the
  same `<root>/<name>` string form `deps.edn`'s own `:local/root`
  values use."
  []
  (letfn [(dirs-under [root]
            (->> (.listFiles (io/file root))
                 (filter #(.isDirectory %))
                 (map #(str root "/" (.getName %)))))]
    (set (concat (dirs-under "components") (dirs-under "bases")))))

(defn- test-extra-paths [deps]
  (set (get-in deps [:aliases :test :extra-paths])))

(defn- real-brick-test-dirs
  "Every `real-brick-dirs` entry that actually has its own `test/`
  subdirectory on disk, in `<brick>/test` form."
  []
  (->> (real-brick-dirs)
       (map #(str % "/test"))
       (filter #(.isDirectory (io/file %)))
       set))

(deftest dev-local-roots-match-every-real-brick-dir-exactly-test
  (let [real (real-brick-dirs)
        declared (dev-local-roots (deps-edn))]
    (testing "every real brick has a :dev :local/root entry"
      (is (empty? (set/difference real declared))
          (str "components/bases directories missing from :dev's :local/root entries: "
               (set/difference real declared))))
    (testing "no :dev :local/root entry points at a nonexistent brick"
      (is (empty? (set/difference declared real))
          (str ":dev's :local/root entries with no matching directory on disk: "
               (set/difference declared real))))))

(deftest every-real-brick-test-dir-is-listed-in-test-extra-paths-test
  (let [missing (set/difference (real-brick-test-dirs) (test-extra-paths (deps-edn)))]
    (is (empty? missing)
        (str "real components/bases test/ directories missing from :test's :extra-paths: " missing))))

(deftest every-listed-test-extra-path-exists-on-disk-test
  (let [ghosts (remove #(.isDirectory (io/file %)) (test-extra-paths (deps-edn)))]
    (is (empty? ghosts)
        (str ":test's :extra-paths lists path(s) that do not exist on disk: " (vec ghosts)))))

;; -- mechanism-sanity: prove the extraction functions actually catch what they claim to --

(deftest dev-local-roots-extraction-is-actually-caught-test
  (is (= #{"components/sim" "components/kernel" "bases/cli"}
         (dev-local-roots {:aliases {:dev {:extra-deps {'poly/sim {:mvn/version "0" :local/root "components/sim"}
                                                          'poly/kernel {:local/root "components/kernel"}
                                                          'poly/cli {:local/root "bases/cli"}
                                                          'org.clojure/clojure {:mvn/version "1.12.5"}}}}}))
      "a non-:local/root :dev entry (plain clojure.org coordinate) must not be mistaken for a brick"))

(deftest test-extra-paths-extraction-is-actually-caught-test
  (is (= #{"components/sim/test" "projects/integration/test"}
         (test-extra-paths {:aliases {:test {:extra-paths ["components/sim/test" "projects/integration/test"]}}}))))

(deftest missing-and-ghost-directions-are-actually-caught-test
  (testing "a real brick absent from :dev is reported missing"
    (is (= #{"components/new-brick"}
           (set/difference #{"components/new-brick" "components/sim"} #{"components/sim"}))))
  (testing "a declared :local/root with no matching directory is reported as a ghost"
    (is (= #{"components/retired-brick"}
           (set/difference #{"components/sim" "components/retired-brick"} #{"components/sim"})))))
