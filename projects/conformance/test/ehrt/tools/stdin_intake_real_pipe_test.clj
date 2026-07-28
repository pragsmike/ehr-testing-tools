(ns ehrt.tools.stdin-intake-real-pipe-test
  "SS-3 Step 6 acceptance (ruling 5, docs/source-sink-design.md Part
  I.2): the real-pipe case -- printf ... | bin/ehr corpus intake
  'stdin:?...' --out ... -- run for real, not simulated with an
  injected InputStream (cli_test.clj's own hermetic coverage of the
  same wiring). Spawns the real bin/ehr entry point (a fresh JVM per
  invocation, several seconds) with its own stdin piped from bytes
  this test writes directly -- ^:integration, run via `make
  integration`, not `make test`."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [clojure.edn :as edn])
  (:import [java.io File]))

(def ^:private out-dir "target/integration-stdin-intake-real-pipe")

(defn- delete-tree!
  [^File f]
  (when (.isDirectory f)
    (doseq [child (.listFiles f)] (delete-tree! child)))
  (.delete f))

(defn- run-ehr-with-piped-stdin!
  "Spawns `bin/ehr args...`, writing input-bytes to the child's own
  stdin (ProcessBuilder's default stdin redirect is PIPE, so
  getOutputStream is the child's stdin), and returns {:exit-code
  :stdout} once the process has exited."
  [args ^bytes input-bytes]
  (let [pb (ProcessBuilder. (into-array String (cons "bin/ehr" args)))]
    (.redirectErrorStream pb true)
    (let [proc (.start pb)]
      (with-open [stdin (.getOutputStream proc)]
        (.write stdin input-bytes)
        (.flush stdin))
      (let [stdout (slurp (.getInputStream proc))
            exit-code (.waitFor proc)]
        {:exit-code exit-code :stdout stdout}))))

(deftest ^:integration real-pipe-stdin-intake-test
  (delete-tree! (io/file out-dir))
  (let [messages "MSH|^~\\&|A\n\nMSH|^~\\&|B\n\n"
        result (run-ehr-with-piped-stdin!
                ["corpus" "intake" "stdin:?format=v2-er7&framing=er7-multi"
                 "--label" "real-pipe" "--out" out-dir "--received" "2026-07-28"]
                (.getBytes messages "UTF-8"))]
    (is (= 0 (:exit-code result))
        (str "bin/ehr exited non-zero -- output: " (:stdout result)))
    (let [catalog (edn/read-string (slurp (io/file out-dir "catalog.edn")))]
      ;; 2 spooled item files + the spool's own capture-manifest.edn
      ;; (a distinct schema from ADR-0014's manifest.edn, so it is not
      ;; recognized as a provenance sidecar -- it's a third foreign file)
      (is (= 3 (count catalog)))
      (is (every? #(= "real-pipe" (:origin %)) catalog))
      (is (= #{"item-0000.hl7" "item-0001.hl7" "capture-manifest.edn"}
             (into #{} (map :path) catalog))))))
