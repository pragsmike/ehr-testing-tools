(ns ehrt.tools.sim-harness
  "Test support (not `src/`): a thin project-local pass-through to
  `ehrt.tools.interface/sim-run!` -- kept as its own namespace so this
  project's five sim_*_test.clj consumers didn't need touching when the
  underlying mechanism changed once already (ADR-0013 subprocess ->
  ADR-0005 in-process mount), and won't need touching again if it
  changes a second time.

  ADR-0005: there is no `available?`/`absence-message` here anymore.
  Sim is always on this project's own classpath now
  (projects/conformance/deps.edn's poly/sim entry, added the same
  session) -- never something to discover or degrade gracefully
  without, so the skip-when-absent policy this namespace used to own
  (AGENTS.md's hermeticity path split) has nothing left to guard."
  (:refer-clojure :exclude [run!])
  (:require [ehrt.tools.interface :as sim]))

(defn run!
  "Delegates to ehrt.tools.interface/sim-run! unchanged."
  [opts]
  (sim/sim-run! opts))
