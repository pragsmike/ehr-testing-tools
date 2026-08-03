(ns ehrt.oracle.digest
  "Standing regression-oracle equipment (post-Wave-D cleanup session,
  2026-08-02, J1/J2, notes/ADRs.md ADR-0030). A regression-oracle claim
  in this repo means running THIS script -- fixed-seed golden runs for
  every pre-existing vendored root, written to files and SHA-256-
  digested by bin/regression-oracle's own worktree-and-diff harness --
  never a test-count or assertion-count comparison (the deviation D1b's
  own literal digest-across-a-worktree precedent set, then D2/D3 stood
  down from without a second literal check, per this session's own
  finding).

  Deliberately independent of `ehrt.kernel.digest` and of any
  deps.edn alias landing in a historical commit: `bin/regression-
  oracle` runs THIS file (always read from the current checkout) with
  a synthetic, from-scratch classpath pointing `:local/root` at
  whichever commit's own worktree is under test -- so the SAME code
  here exercises two different component-code versions, and hashing
  itself happens in the calling shell (`sha256sum`), not in-process.

  Six roots, matching this session's own J1 ruling verbatim:
  appendicitis/sore-throat/ear-infections (interpreter-layer batches,
  the same well-mixed-seed pattern every vendored-module test in this
  repo already uses -- SEQUENTIAL small java.util.Random seeds are NOT
  well-distributed for their own first draw, confirmed repeatedly
  across GMF coverage waves), sinusitis/death-fixture/sepsis
  (engine-layer: engine/run plus emit-hl7/emit, ground truth AND
  emitted HL7 both captured, the exact run-configs each root's own
  vendored/engine test already established as producing real content)."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-trajectory.gmf-interpreter :as interp]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim.engine :as engine]
            [ehrt.sim-emit-hl7.interface :as emit-hl7])
  (:import [java.util Random]))

(defn- person [seed sex] (assoc (sim-model/persona (Random. seed) {}) :sex sex))

(defn- mixed-seeds
  "The established mixer-RNG pattern (vendored_appendicitis_test.clj,
  reused verbatim across every GMF coverage wave since this repo's own
  M7 milestone) -- sequential small seeds are not well-distributed for
  java.util.Random's own first draw."
  [n mixer-seed]
  (let [mixer (Random. mixer-seed)]
    (repeatedly n #(.nextLong mixer))))

(defn- run-walk
  [module modules seed sex reg-offset-years horizon-years]
  (let [p (person seed sex)
        reg-t (+ (interp/dob-epoch-day p) (* 365 reg-offset-years))
        end-t (+ reg-t (* 365 horizon-years))]
    (:trajectory (if modules
                   (interp/run-module module (Random. seed) p reg-t end-t modules)
                   (interp/run-module module (Random. seed) p reg-t end-t)))))

(defn- interpreter-batch
  "100 well-mixed seeds x both sexes = 200 walks, concatenated into one
  deterministic EDN vector -- enough real content across enough of each
  module's own branches to make a silent behavior change unlikely to
  slip through undetected, without the runtime cost of the much larger
  seed pools each root's own vendored test uses to hunt one specific
  rare branch."
  [module modules mixer-seed reg-offset-years horizon-years]
  (vec
   (for [seed (mixed-seeds 100 mixer-seed)
         sex [:male :female]]
     (run-walk module modules seed sex reg-offset-years horizon-years))))

(defn- appendicitis-batch []
  (let [module (:payload (gmf/load-module "appendicitis" (slurp (io/resource "sim/modules/appendicitis.json"))))]
    (interpreter-batch module nil 20260727 70 80)))

(defn- sore-throat-batch []
  (let [module (:payload (gmf/load-module "sore-throat" (slurp (io/resource "sim/modules/sore_throat.json"))))]
    (interpreter-batch module nil 20260802 25 10)))

(defn- ear-infections-batch []
  (let [resolve-call-path (fn [call-path] (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))
        loaded (gmf/load-closure "ear-infections" (slurp (io/resource "sim/modules/ear_infections.json")) resolve-call-path)
        modules (:modules (:payload loaded))
        root (get modules "ear-infections")]
    (interpreter-batch root modules 20260802 25 10)))

(defn- engine-pair
  "Ground truth plus rendered HL7, both content this session's own oracle
  digests -- matching D1b's own literal-digest precedent (HL7 emission
  bytes included, not just the in-memory event log)."
  [run-config]
  (let [{:keys [ground-truth facility providers]} (engine/run run-config)
        messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" facility providers)]
    {:ground-truth ground-truth :hl7 (vec messages)}))

(defn- sinusitis-pair []
  (let [module (:payload (gmf/load-module "sinusitis" (slurp (io/resource "sim/modules/sinusitis.json"))))]
    (engine-pair {:seed 1 :patients 30 :pathway {:name "module-only" :steps []}
                  :modules [(gmf/singleton-closure module)] :module-assignment [{:module-id "sinusitis" :weight 1}]
                  :module-horizon-days 3650})))

(defn- death-fixture-pair []
  (let [module (:payload (gmf/load-module "death-fixture" (slurp (io/resource "ehrt/sim/fixtures/death-fixture.json"))))]
    (engine-pair {:seed 20260802 :patients 200 :pathway {:name "module-only" :steps []}
                  :modules [(gmf/singleton-closure module)] :module-assignment [{:module-id "death-fixture" :weight 1}]
                  :module-horizon-days 3650})))

(defn- sepsis-pair []
  (let [module (:payload (gmf/load-module "sepsis" (slurp (io/resource "sim/modules/sepsis.json"))))]
    (engine-pair {:seed 20260802 :patients 500 :pathway {:name "module-only" :steps []}
                  :modules [(gmf/singleton-closure module)] :module-assignment [{:module-id "sepsis" :weight 1}]
                  :module-horizon-days 36500})))

(def ^:private roots
  {"appendicitis"   appendicitis-batch
   "sore-throat"    sore-throat-batch
   "ear-infections" ear-infections-batch
   "sinusitis"      sinusitis-pair
   "death-fixture"  death-fixture-pair
   "sepsis"         sepsis-pair})

(defn -main
  "Writes one <root>.edn per root into out-dir (pr-str of the batch or
  the {:ground-truth :hl7} pair) -- bin/regression-oracle's own shell
  loop sha256sums each file itself; this process never hashes."
  [out-dir]
  (.mkdirs (io/file out-dir))
  (doseq [[root-name f] (sort-by key roots)]
    (println "running" root-name "...")
    (let [content (f)
          file (io/file out-dir (str root-name ".edn"))]
      (spit file (pr-str content))
      (println "  wrote" (.getPath file))))
  (println "done"))
