(ns ehrt.tools.corpus.golden-comparison
  "The comparison harness SS-1's Step 4 acceptance property is built
  from (ruling 5, `.agents/plans/corpus-foundations.md`): byte-identity
  between two `ehrt.tools.corpus.intake/intake!` output
  directories. Landed and self-tested (`golden_comparison_test.clj`)
  BEFORE the refactor commit that actually points it at the new `dir:`
  Source call path, per ruling 5's own discipline -- this namespace
  never calls intake itself, so it compiles and its self-test passes
  using only the pre-SS-1 call shape, with no forward reference to code
  the refactor commit hasn't written yet."
  (:require [clojure.java.io :as io]))

(def ^:private compared-files
  "Every file intake! writes to its :out directory -- both must match
  for two runs to count as the same catalog."
  ["catalog.edn" "intake-record.edn"])

(defn compare-catalogs
  "Compares two intake! :out directories file-by-file over
  `compared-files`. Returns {:identical? bool :diverging-files [...]}
  -- the diverging-files list names which of the compared files
  differ (empty when identical?), so a caller doesn't have to re-diff
  by hand to find out what broke."
  [out-dir-a out-dir-b]
  (let [diverging (->> compared-files
                       (remove (fn [filename]
                                 (= (slurp (io/file out-dir-a filename))
                                    (slurp (io/file out-dir-b filename)))))
                       vec)]
    {:identical? (empty? diverging)
     :diverging-files diverging}))

(defn catalogs-byte-identical?
  "True iff every one of compare-catalogs' compared-files is byte-
  identical between the two :out directories -- the predicate form,
  for a direct `is` assertion."
  [out-dir-a out-dir-b]
  (:identical? (compare-catalogs out-dir-a out-dir-b)))
