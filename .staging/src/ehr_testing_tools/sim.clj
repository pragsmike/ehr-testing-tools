(ns ehr-testing-tools.sim
  "The sim engine adapter (D7, docs/source-sink-design.md Part VII;
  SS-2 Step 3): ehr-testing-sim consumed by subprocess only,
  ADR-0013-shaped -- NEVER added to this repo's classpath or deps.edn.
  Promotes test-integration/ehr_testing_tools/sim_harness.clj's own
  subprocess seam into src/, so the sim generator source
  (ehr-testing-tools.corpus.generators) and the test harness both drive
  the SAME code, rather than the harness owning its own copy.

  Discovery order (ruling 4, SS-2): an explicit :sim-dir param, then
  the EHR_TESTING_SIM_DIR env var, then the sibling-checkout default
  (../ehr-testing-sim, relative to this repo's own working directory --
  a path, never a dependency coordinate, per ADR-0013). Absent at every
  step -- a genuinely missing checkout, not merely \"no override
  given\" -- is result/error :sim-not-available, naming all three paths
  tried; never a thrown exception, and never a silent skip here --
  skip-when-absent is a TEST policy (test-integration/sim_harness.clj
  keeps that wrapping), not something this adapter decides for itself.

  Intended evolution (D5/ADR-0017 Part V, recorded here, not built):
  once sim is published (Clojars/Maven), the sim generator source's
  engine artifact becomes a pinned entry in the existing artifact
  registry (`ehr artifact fetch sim`), dissolving the sibling-checkout
  requirement -- discovery would then gain a fourth, preferred path
  (the registry-resolved artifact directory), checked ahead of the
  sibling-checkout default."
  (:refer-clojure :exclude [run!])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehr-testing-tools.invocation :as invocation]
            [ehr-testing-tools.result :as result]))

(def default-sim-repo-dir
  "The sibling checkout's path, relative to this repo's root -- third
  and last in the discovery order."
  "../ehr-testing-sim")

(def sim-dir-env-var
  "EHR_TESTING_SIM_DIR")

(defn env-sim-dir
  "The EHR_TESTING_SIM_DIR env var, if set. A separate, public function
  (rather than an inline System/getenv call) so tests can override it
  via an injected :env-sim-dir-fn instead of mutating real env vars --
  same convention as ehr-testing-tools.artifact/env-override."
  []
  (System/getenv sim-dir-env-var))

(defn- candidate-sim-dir
  "The discovery order (ruling 4): explicit override, env var, sibling
  default -- the first of the three that IS a directory, or nil if
  none is. :env-sim-dir-fn/:default-dir are injectable, defaulting to
  the real env-sim-dir/default-sim-repo-dir, so tests can force every
  branch of this order hermetically regardless of the real machine's
  own filesystem state."
  [{:keys [sim-dir env-sim-dir-fn default-dir]
    :or {env-sim-dir-fn env-sim-dir default-dir default-sim-repo-dir}}]
  (first (filter #(and % (.isDirectory (io/file %)))
                 [sim-dir (env-sim-dir-fn) default-dir])))

(defn available?
  ([] (available? {}))
  ([opts] (boolean (candidate-sim-dir opts))))

(defn- sim-not-available
  [{:keys [sim-dir env-sim-dir-fn default-dir]
    :or {env-sim-dir-fn env-sim-dir default-dir default-sim-repo-dir}}]
  (result/error :sim-not-available
                {:sim-dir sim-dir
                 :env-var (env-sim-dir-fn)
                 :sibling-checkout default-dir
                 :hint (str "no ehr-testing-sim checkout found -- tried an explicit :sim-dir, "
                            "the " sim-dir-env-var " env var, and the sibling-checkout default "
                            default-dir "; clone it alongside this repo, or set one of the above")}))

(defn- cli-args
  "opts -> sim's own `run` verb's argv, per its cli-spec
  (ehr-testing-sim.cli/help-group): :seed is required by sim itself
  (not re-validated here -- that is sim's own operational-error
  boundary, not this adapter's); :patients/:churn/:emit/:reference-date/
  :config are optional passthroughs. :config is resolved to an
  ABSOLUTE path before it ever reaches argv -- the subprocess's own
  working directory is the resolved sim checkout, not this repo's
  root, so a bare relative fixture path would resolve against the
  WRONG directory if passed through verbatim."
  [{:keys [seed patients churn emit reference-date config]}]
  (cond-> ["-M:cli" "run" "--seed" (str seed)]
    patients (conj "--patients" (str patients))
    churn (conj "--churn")
    emit (conj "--emit" emit)
    reference-date (conj "--reference-date" reference-date)
    config (conj "--config" (.getAbsolutePath (io/file config)))))

(defn run!
  "Drives ehr-testing-sim's own CLI (`clojure -M:cli run --seed ...`)
  as a subprocess in the checkout resolved by the discovery order above
  -- an explicit seed always reaches argv (sim's own CLI requires one).
  Never throws. Returns:
  - result/ok <sim's own :payload>, unwrapped, on a zero exit whose
    own printed Result has :status :ok;
  - result/error :sim-not-available when no checkout resolves (see
    above);
  - result/error :sim-run-failed (carrying :exit-code/:stdout/:stderr)
    on a nonzero exit;
  - result/error :sim-run-rejected (carrying sim's own Result map) on
    a zero exit whose own status is :rejected/:error;
  - the first failing step's result unchanged (e.g. :spawn-failed) if
    the subprocess could not even start.

  :sim-dir/:env-sim-dir-fn/:default-dir are the discovery overrides
  (see candidate-sim-dir); :out-dir (default \"target/sim\") is where
  stdout/stderr land -- this repo's own gitignored build-scratch
  convention, never the sibling's own tree."
  [{:keys [seed out-dir run-invocation]
    :or {out-dir "target/sim" run-invocation invocation/run!}
    :as opts}]
  (if-let [resolved-dir (candidate-sim-dir opts)]
    (do
      (.mkdirs (io/file out-dir))
      (let [stdout-path (str out-dir "/run-" seed "-stdout.log")
            stderr-path (str out-dir "/run-" seed "-stderr.log")
            invocation-result (run-invocation {:command "clojure"
                                                :args (cli-args opts)
                                                :dir resolved-dir
                                                :stdout-path stdout-path
                                                :stderr-path stderr-path})]
        (if-not (result/ok? invocation-result)
          invocation-result
          (let [{:keys [exit-code]} (:payload invocation-result)]
            (if-not (zero? exit-code)
              (result/error :sim-run-failed
                            {:exit-code exit-code
                             :stdout (slurp stdout-path)
                             :stderr (slurp stderr-path)})
              (let [sim-result (edn/read-string (slurp stdout-path))]
                (if-not (= :ok (:status sim-result))
                  (result/error :sim-run-rejected {:sim-result sim-result})
                  (result/ok (:payload sim-result)))))))))
    (sim-not-available opts)))
