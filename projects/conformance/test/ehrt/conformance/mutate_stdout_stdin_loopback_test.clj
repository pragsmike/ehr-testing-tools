(ns ehrt.conformance.mutate-stdout-stdin-loopback-test
  "SS-4 Step 4 acceptance (rulings 5-6, docs/source-sink-design.md Part
  III): the loopback -- this repo's own `corpus mutate` writing to a
  `stdout:` Sink, piped directly into this repo's own `corpus intake`
  reading a `stdin:` Source, run for real (not simulated with an
  injected stream -- cli_test.clj's own hermetic coverage of the same
  wiring, mutate-command-stdout-sink-*-test). This is the byte-stream
  form of the composability law's acceptance: `bin/ehrt ... | bin/ehrt
  ...`, via a real `bash -c` shell pipe (two chained subprocesses, not
  simulated) so the exit-code/piping semantics match exactly what a
  human running this at a shell would see. ^:integration, run via
  `make integration`."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.edn :as edn]
            [ehrt.kernel.interface :as digest])
  (:import [java.io File]))

(def ^:private input-dir "target/integration-loopback-input")
(def ^:private reference-mutant-dir "target/integration-loopback-reference-mutants")
(def ^:private catalog-out-dir "target/integration-loopback-catalog")

(defn- delete-tree!
  [^File f]
  (when (.isDirectory f)
    (doseq [child (.listFiles f)] (delete-tree! child)))
  (.delete f))

(defn- run-shell!
  "Runs cmd through a real bash -c (so a `|` in cmd is a genuine OS-level
  pipe between two real bin/ehrt subprocesses, not something this test
  simulates in-process). Returns {:exit-code :stdout :stderr}."
  [^String cmd]
  (let [pb (ProcessBuilder. (into-array String ["bash" "-c" cmd]))
        proc (.start pb)
        stdout (future (slurp (.getInputStream proc)))
        stderr (future (slurp (.getErrorStream proc)))
        exit-code (.waitFor proc)]
    {:exit-code exit-code :stdout @stdout :stderr @stderr}))

(deftest ^:integration mutate-stdout-into-intake-stdin-real-loopback-test
  (doseq [d [input-dir reference-mutant-dir catalog-out-dir]] (delete-tree! (io/file d)))
  (.mkdirs (io/file input-dir))
  (io/copy (io/file "components/corpus/test-fixtures/v2/adt-a01-admit.hl7") (io/file input-dir "a.hl7"))
  (io/copy (io/file "components/corpus/test-fixtures/v2/adt-a02-transfer.hl7") (io/file input-dir "b.hl7"))

  ;; The reference: the SAME mutate run, written to a plain directory --
  ;; what the loopback's re-intaken hashes must equal, not merely "some
  ;; hash" (proves the bytes crossing the stdout->stdin pipe are the
  ;; SAME bytes mutate produces, not an artifact of the framing codec).
  (let [reference (run-shell! (str "bin/ehrt corpus mutate " input-dir
                                    " --operator-id blank-required-field --locator-path MSH-9"
                                    " --out-dir " reference-mutant-dir))]
    (is (= 0 (:exit-code reference))
        (str "reference mutate run failed -- stdout: " (:stdout reference) " stderr: " (:stderr reference))))

  (let [loopback-cmd (str "bin/ehrt corpus mutate " input-dir
                           " --operator-id blank-required-field --locator-path MSH-9"
                           " --out-dir 'stdout:?format=v2-er7&framing=mllp'"
                           " | bin/ehrt corpus intake 'stdin:?format=v2-er7&framing=mllp'"
                           " --label loopback --out " catalog-out-dir
                           " --received 2026-07-28")
        loopback (run-shell! loopback-cmd)]
    (is (= 0 (:exit-code loopback))
        (str "loopback pipeline failed -- cmd: " loopback-cmd
             " stdout: " (:stdout loopback) " stderr: " (:stderr loopback))))

  (let [catalog (edn/read-string (slurp (io/file catalog-out-dir "catalog.edn")))
        item-entries (filter #(str/starts-with? (:path %) "item-") catalog)
        reference-hashes (into #{} (map #(digest/sha256-file %))
                                [(io/file reference-mutant-dir "a.hl7")
                                 (io/file reference-mutant-dir "b.hl7")])]
    ;; 2 spooled item files + the spool's own capture-manifest.edn (a
    ;; distinct schema from ADR-0014's manifest.edn), same shape as the
    ;; SS-3 stdin real-pipe precedent.
    (is (= 3 (count catalog)))
    (is (= 2 (count item-entries)))
    (is (every? #(= "loopback" (:origin %)) catalog))
    (is (= reference-hashes (into #{} (map :id) item-entries))
        "the loopback's re-intaken content hashes equal the reference mutate run's own mutant bytes")))
