(ns ehrt.tools.sim-generator-source-test
  "Real-engine acceptance (SS-2 Step 3; ruling 3's own \"the real-engine
  paths are integration-tier\"): ehrt.tools.interface
  source/resolve! over the real :sim registry entry, against the real
  ehr-testing-sim subprocess -- not a fake. Then intake/intake-via-
  source! over the resulting dir Source, proving the whole SS-2 chain
  (registry -> unification -> intake) end to end for a real generator,
  not just synthea's own SS-1 golden-catalog trial. Skips cleanly (see
  sim-harness/absence-message) when ../ehr-testing-sim isn't checked
  out."
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.io :as io]
            [ehrt.tools.interface :as result]
            [ehrt.tools.interface :as generator-source]
            [ehrt.tools.interface :as intake]
            [ehrt.tools.sim-harness :as sim-harness])
  (:import [java.io File]))

(defn- delete-tree!
  [^File f]
  (when (.exists f)
    (doseq [child (reverse (file-seq f))] (.delete ^File child))))

(deftest ^:integration sim-generator-source-round-trips-through-intake-test
  (if-not (sim-harness/available?)
    (do (println sim-harness/absence-message)
        (is true sim-harness/absence-message))
    (let [seed 101
          patients 2
          corpus-dir (str "target/corpus/sim-s" seed "-p" patients)
          out-dir "target/integration-sim-generator-source-catalog"
          _ (delete-tree! (io/file corpus-dir))
          _ (delete-tree! (io/file out-dir))
          resolve-result (generator-source/resolve! :sim {:seed seed :patients patients})]
      (is (result/ok? resolve-result)
          (str "generator-source/resolve! :sim failed: " (pr-str resolve-result)))
      (when (result/ok? resolve-result)
        (let [source (:payload resolve-result)]
          (is (= :dir (:kind source)))
          (is (= corpus-dir (:path source)))
          (is (.isFile (io/file corpus-dir "manifest.edn")))
          (let [intake-result (intake/intake-via-source!
                                {:source source :source-label "sim-generator-source"
                                 :out out-dir :received "2026-07-28"})]
            (is (result/ok? intake-result))
            (when (result/ok? intake-result)
              (let [{:keys [catalog]} (:payload intake-result)]
                (is (some #(= :v2-er7 (:format %)) catalog))
                (is (every? #(some? (:provenance %)) catalog)
                    "sim's own manifest.edn sidecar is picked up (ADR-0014)")))))))))
