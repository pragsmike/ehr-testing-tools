(ns ehrt.tools.corpus.canonicalizers
  "Concrete canonicalizer entries discovered by EXP-A4, registered into
  ehrt.tools.canonical at namespace load time (require this
  namespace to make them available; the registration IS the load-time
  side effect -- see canonical.clj's registry doc). Their idempotence-law
  property tests live on the test side (canonicalizers_test.clj), per
  the same split as canonical.clj itself: these entries need no
  test.check dependency to run, only to be verified.

  Both entries here address volatility EXP-A4 found in Synthea's output
  that is execution metadata, not corpus content: a wall-clock export
  timestamp embedded in two filenames, and a run-audit record (random
  run ID, wall-clock start time, wall-clock duration) inside Synthea's
  own metadata/ output."
  (:require [clojure.string :as str]
            [ehrt.tools.canonical :as canonical]))

(defn strip-run-timestamp-suffix
  "hospitalInformation<digits>.json / practitionerInformation<digits>.json
  -> the same name with the digit run removed. Synthea names these two
  files after a wall-clock export timestamp that isn't derived from any
  seed we can pin; the file *content* is otherwise deterministic once
  seed + clinician-seed are pinned (confirmed empirically, EXP-A4)."
  [filename]
  (str/replace filename #"(hospitalInformation|practitionerInformation)\d+\.json$" "$1.json"))

(defn strip-synthea-run-metadata
  "Removes Synthea's own per-execution audit fields from a parsed
  metadata/*.json map: runID (random UUID), runStartTime and
  runTimeInSeconds (wall-clock) -- these describe the execution, not the
  generated corpus, and are expected to differ run over run even with
  every generation input pinned."
  [metadata-map]
  (dissoc metadata-map "runID" "runStartTime" "runTimeInSeconds"))

(canonical/register!
 {:id :strip-run-timestamp-suffix :version "1" :format :text
  :fn strip-run-timestamp-suffix
  :docstring "Strips Synthea's wall-clock export-timestamp suffix from hospitalInformation/practitionerInformation filenames."})

(canonical/register!
 {:id :strip-synthea-run-metadata :version "1" :format :edn
  :fn strip-synthea-run-metadata
  :docstring "Removes Synthea's per-execution audit fields (runID, runStartTime, runTimeInSeconds) from a parsed metadata/*.json map."})
