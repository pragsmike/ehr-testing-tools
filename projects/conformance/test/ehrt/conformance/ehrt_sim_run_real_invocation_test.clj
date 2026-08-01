(ns ehrt.conformance.ehrt-sim-run-real-invocation-test
  "ADR-0005: the ONE deliberate real-OS-process witness for the `ehrt
  sim` mount -- every other sim-consuming test in this tree
  (sim_manifest_contract_test.clj and its four siblings, smoke_test.clj)
  now calls straight into ehrt.sim.interface in-process, which proves
  the mount's own LOGIC but never proves the actual `bin/ehrt sim run`
  invocation a human or a script would type still resolves, parses its
  flags, and prints a real Result to stdout. This is that proof, real
  `bin/ehrt` subprocess and all -- consumer-fidelity, not logic
  coverage, which is why there is exactly one of these and not five.
  Same real-subprocess style as mutate_stdout_stdin_loopback_test.clj
  and stdin_intake_real_pipe_test.clj.

  Renamed from sim_cli_real_invocation_test.clj (P3-6, 2026-08-01,
  sim-cli retirement sweep): this test has never subprocessed
  bases/sim-cli -- it always exercised `bin/ehrt sim run` -- so the old
  name became doubly misleading once sim-cli itself was retired."
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]))

(defn- run-ehr!
  "stdout and stderr kept separate, never merged -- a JVM/WSL warning
  printed to stderr (e.g. the stale-fsmonitor warning AUTHORS-GUIDE.md
  documents) must never corrupt the EDN this test reads from stdout,
  same discipline real-git-describe's own docstring names for exactly
  this reason."
  [args]
  (let [pb (ProcessBuilder. (into-array String (cons "bin/ehrt" args)))
        proc (.start pb)
        stdout (future (slurp (.getInputStream proc)))
        stderr (future (slurp (.getErrorStream proc)))
        exit-code (.waitFor proc)]
    {:exit-code exit-code :stdout @stdout :stderr @stderr}))

(deftest real-bin-ehr-sim-run-test
  (let [{:keys [exit-code stdout stderr]} (run-ehr! ["sim" "run" "--seed" "100" "--patients" "1"])]
    (is (= 0 exit-code) (str "bin/ehrt sim run exited non-zero -- stdout: " stdout " stderr: " stderr))
    (let [r (edn/read-string stdout)]
      (is (map? (:manifest (:payload r))))
      (is (= :simulated (:stage (:manifest (:payload r))))))))
