(ns ehrt.docs-tooling.license-text-pointer-test
  "Register row F-4 / ADR-0054 (alignment fixes 5, 2026-08-05): Apache-2.0
  section 4(a) expects a copy of the license to travel with redistributed
  content, but of this repo's four Apache-2.0-sourced vendored roots only
  `test-fixtures/v2/simhospital/` carried the actual
  license TEXT -- the other three relied on NOTICE narrative alone. This
  gate has two parts: (a) `LICENSES/Apache-2.0.txt` exists and is
  byte-identical to that root's own `LICENSE`; (b) every NOTICE file in
  the tree whose text CITES the Apache license as governing its own
  content also POINTS at that shared text file -- so a future vendored
  root that writes an Apache-citing NOTICE without the pointer trips the
  build, the same structural-not-audited shape
  `resource-nesting-test`/`skill-mirror-currency-test` already establish
  for their own drift classes.

  `cites-apache?` is deliberately NOT a plain substring search for
  \"Apache\" -- `components/sim-model/resources/sim-model/demographics/
  NOTICE` mentions \"Synthea (Apache-2.0, MITRE)\" as background for
  content it explicitly disclaims copying FROM Synthea (hand-curated,
  no Apache obligation, register row F-2); a naive substring test would
  false-positive that file forever. The predicate instead matches lines
  that assert GOVERNANCE -- a `License:`/`License |` line naming Apache,
  or an `under Apache`/`under the Apache` phrase -- which is what every
  real target actually writes and what the demographics NOTICE's
  mention-only sentence does not. `mechanism-sanity-test` below proves
  both directions: it catches a governance line and it does NOT catch a
  mention-only one, against literal excerpts of the real files (see
  ADR-0054 for the excerpt-vs-live-file citation)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ^:private license-text-file "LICENSES/Apache-2.0.txt")
(def ^:private canonical-source-file
  "test-fixtures/v2/simhospital/LICENSE")

(def ^:private excluded-dir-names #{"target" ".git"})

(defn- under-excluded-dir? [^java.io.File f]
  (some excluded-dir-names
        (map str (-> f .toPath .iterator iterator-seq))))

(defn- notice-files
  "Every file named exactly NOTICE or NOTICE.md under the repo root,
  target/ and .git/ pruned."
  []
  (->> (file-seq (io/file "."))
       (filter #(.isFile ^java.io.File %))
       (remove under-excluded-dir?)
       (filter #(#{"NOTICE" "NOTICE.md"} (.getName ^java.io.File %)))
       sort))

(def ^:private cites-apache-pattern
  #"(?i)license\s*[:|]\s*.*apache|under (?:the )?apache")

(defn- cites-apache?
  "True when some line of `content` asserts Apache-2.0 governs the
  content this NOTICE describes -- a `License:`/`License |` line naming
  Apache, or an `under Apache`/`under the Apache` phrase. A bare mention
  of \"Apache\" elsewhere in the prose (e.g. background on an upstream
  project's own licensing, not a claim about THIS file's content) does
  not match."
  [content]
  (boolean (some #(re-find cites-apache-pattern %) (str/split-lines content))))

(defn- has-pointer? [content]
  (str/includes? content license-text-file))

(deftest license-text-file-exists-and-matches-canonical-source-test
  (testing (str license-text-file " exists")
    (is (.exists (io/file license-text-file))
        (str license-text-file " is missing -- ADR-0054 AR-F5-1 vendors the "
             "Apache-2.0 text there, byte-copied from " canonical-source-file)))
  (when (.exists (io/file license-text-file))
    (testing (str license-text-file " is byte-identical to " canonical-source-file)
      (is (= (slurp canonical-source-file) (slurp license-text-file))
          (str license-text-file " differs from " canonical-source-file
               " -- it must be an exact byte-copy of the canonical source, "
               "per ADR-0054 AR-F5-1")))))

(deftest every-apache-citing-notice-points-at-the-shared-license-text-test
  (doseq [f (notice-files)]
    (let [content (slurp f)]
      (when (cites-apache? content)
        (is (has-pointer? content)
            (str f " cites the Apache license as governing its own content "
                 "but does not point at " license-text-file " -- append a "
                 "dated cross-ref line per ADR-0054 AR-F5-2"))))))

;; -- mechanism-sanity: prove cites-apache? actually catches what it claims to --

(deftest mechanism-sanity-test
  (testing "a License: line naming Apache is caught"
    (is (true? (cites-apache? "License: Apache-2.0 (`synthetichealth/synthea`, ...)"))))
  (testing "a License | table-row line naming Apache is caught"
    (is (true? (cites-apache? "| License | Apache License 2.0 (repo-root `LICENSE`, ...) |"))))
  (testing "an \"under Apache-2.0\" governance phrase is caught"
    (is (true? (cites-apache? "  under Apache-2.0, with its upstream URL/commit SHA/SHA-256 recorded"))))
  (testing "a mention-only sentence (the demographics NOTICE's own shape) is NOT caught"
    (is (false? (cites-apache? "Synthea (Apache-2.0, MITRE) as the mined source for this project's US demographics tables"))))
  (testing "no mention of Apache at all is NOT caught"
    (is (false? (cites-apache? "This directory's content is hand-curated and carries no third-party license obligation."))))
  (testing "has-pointer? matches only the real pointer string"
    (is (true? (has-pointer? "see LICENSES/Apache-2.0.txt for the full text")))
    (is (false? (has-pointer? "see the Apache-2.0 license text")))))
