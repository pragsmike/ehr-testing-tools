(ns ehr-testing-tools.sim-harness
  "Test support (not `src/`): the subprocess seam the cross-repo consumer
  loop (notes/ADRs.md ADR-0013) runs `ehr-testing-sim` through. Invokes
  sim's own CLI (`clojure -M:cli run ...`) as a subprocess in the sibling
  checkout via `ehr-testing-tools.invocation/run!` -- the same injectable
  wrapper `judge.fhir`/`corpus.generate` already use for every other
  pinned-engine subprocess in this repo (pattern nursery #2); no new
  subprocess convention invented here. Captures the subprocess's stdout
  and parses it as the EDN `Result` map sim's own `cli.clj` prints.

  `ehr-testing-sim` is NEVER added to this repo's classpath or
  `deps.edn` (ADR-0013's coupling rule) -- consumed only as an external
  process, so every test built on this namespace must degrade to a
  clean skip, not a failure, when the sibling checkout is absent
  (`available?`/`absence-message` below)."
  (:refer-clojure :exclude [run!])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehr-testing-tools.invocation :as invocation]
            [ehr-testing-tools.result :as result]))

(def sim-repo-dir
  "The sibling checkout's path, relative to this repo's root (the
  working directory `clojure -X:integration` runs from) -- a path, never
  a dependency coordinate, per ADR-0013."
  "../ehr-testing-sim")

(defn available?
  []
  (.isDirectory (io/file sim-repo-dir)))

(def absence-message
  (str "SKIP: " sim-repo-dir " not found -- this suite consumes "
       "ehr-testing-sim as a sibling checkout, subprocess-only (never a "
       "classpath/deps.edn dependency -- notes/ADRs.md ADR-0013); clone "
       "it alongside this repo to run these tests."))

(defn- cli-args
  "opts -> sim's own `run` verb's argv, per its cli-spec
  (ehr-testing-sim.cli/help-group): :seed is required by sim itself (not
  re-validated here -- that is sim's own operational-error boundary, not
  this harness's); :patients/:churn/:emit/:reference-date/:config are
  optional passthroughs. :config is resolved to an ABSOLUTE path before
  it ever reaches argv -- the subprocess's own working directory is
  `sim-repo-dir` (`../ehr-testing-sim`, per `run!`'s own `:dir`), not this
  repo's root, so a bare relative fixture path (e.g.
  `test-integration/fixtures/sim-configs/full-capability.edn`, this
  repo's own convention for where such fixtures live) would resolve
  against the WRONG directory if passed through verbatim."
  [{:keys [seed patients churn emit reference-date config]}]
  (cond-> ["-M:cli" "run" "--seed" (str seed)]
    patients (conj "--patients" (str patients))
    churn (conj "--churn")
    emit (conj "--emit" emit)
    reference-date (conj "--reference-date" reference-date)
    config (conj "--config" (.getAbsolutePath (io/file config)))))

(defn run!
  "Runs `clojure -M:cli run --seed ...` (plus any of :patients/:churn/
  :emit/:reference-date) as a subprocess in the sibling ehr-testing-sim
  checkout, through the invocation wrapper (:run-invocation, injectable,
  defaults to `ehr-testing-tools.invocation/run!` -- fakeable at the
  unit level, the real subprocess at the integration level, the same
  split judge.fhir's own :run-invocation already uses). Never throws.

  Returns:
  - `result/ok` <sim's own :payload>, unwrapped, when the subprocess
    exits 0 and sim's own printed Result has :status :ok;
  - `result/error :sim-run-failed` (carrying :exit-code, :stdout,
    :stderr) on a nonzero exit -- a clear, actionable failure rather
    than an uncaught EDN-read exception on malformed/absent output;
  - `result/error :sim-run-rejected` (carrying sim's own Result map)
    on a zero exit whose own status is :rejected/:error -- sim's own
    exit-code mapping (0 ok / 1 rejected / 2 error) makes this pairing
    unreachable in practice, but this seam checks it explicitly rather
    than assuming a subprocess contract it doesn't control;
  - the first failing step's result unchanged (e.g. `:spawn-failed`)
    if the subprocess could not even start.

  :out-dir (default \"target/sim-harness\") is where stdout/stderr land
  -- this repo's own gitignored build-scratch convention, never the
  sibling's own tree."
  [{:keys [seed out-dir run-invocation]
    :or {out-dir "target/sim-harness" run-invocation invocation/run!}
    :as opts}]
  (.mkdirs (io/file out-dir))
  (let [stdout-path (str out-dir "/run-" seed "-stdout.log")
        stderr-path (str out-dir "/run-" seed "-stderr.log")
        invocation-result (run-invocation {:command "clojure"
                                            :args (cli-args opts)
                                            :dir sim-repo-dir
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
