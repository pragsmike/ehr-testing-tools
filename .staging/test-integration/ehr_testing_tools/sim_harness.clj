(ns ehr-testing-tools.sim-harness
  "Test support (not `src/`): the TEST-side wrapping around the sim
  engine adapter (ehr-testing-tools.sim, src/, SS-2 Step 3) -- this
  namespace no longer owns the subprocess seam itself (it did before
  Step 3; see git history for that shape). `run!` delegates discovery
  and invocation to the adapter unchanged, with this suite's own
  gitignored log-directory convention (\"target/sim-harness\", distinct
  from the adapter's own default so a test run's logs never collide
  with a generator-source run's); `available?`/`absence-message` stay
  here because skip-when-absent is a TEST policy (AGENTS.md's
  hermeticity path split), never something the adapter decides for
  itself -- ehr-testing-tools.sim/run! returns a real, informative
  result/error :sim-not-available instead of ever skipping silently.

  Every consumer of this namespace (sim_manifest_contract_test.clj,
  sim_gate_loop_test.clj, sim_intake_test.clj, sim_full_capability_
  gate_test.clj, smoke_test.clj) is unchanged in what it asserts --
  this is a delegation, not a behavior change: the consumer loop now
  tests through the SAME code the sim generator source
  (ehr-testing-tools.corpus.generators) drives, rather than parallel
  harness code that happened to do the same thing."
  (:refer-clojure :exclude [run!])
  (:require [ehr-testing-tools.sim :as sim]))

(defn available?
  []
  (sim/available?))

(def absence-message
  (str "SKIP: no ehr-testing-sim checkout found (tried an explicit :sim-dir, "
       sim/sim-dir-env-var ", and the sibling-checkout default "
       sim/default-sim-repo-dir ") -- this suite consumes ehr-testing-sim as "
       "a sibling checkout, subprocess-only (never a classpath/deps.edn "
       "dependency -- notes/ADRs.md ADR-0013); clone it alongside this repo "
       "to run these tests."))

(defn run!
  "Delegates to ehr-testing-tools.sim/run! unchanged, defaulting
  :out-dir to this suite's own gitignored log convention
  (\"target/sim-harness\") when the caller doesn't override it."
  [opts]
  (sim/run! (merge {:out-dir "target/sim-harness"} opts)))
