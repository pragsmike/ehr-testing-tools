(ns ehrt.tools.sim
  "The sim engine adapter (D7 lineage, docs/source-sink-design.md Part
  VII). As of 2026-07-28 (ADR-0005, carve-loss recovery session): calls
  `ehrt.sim.interface/run-command` directly, in-process -- no
  subprocess, no sibling-checkout discovery, no availability check.
  This retires ADR-0013 decision 1's subprocess-only mechanism
  (notes/tools/ADRs.md) -- its motivating constraint (sim living in a
  separate, then-private repo; a classpath dependency tangling two
  independently-versioned repos) is gone now that sim and tools are
  bricks in the same workspace, same classpath, same version, same
  commit -- while keeping ADR-0013's own dependency-*direction*
  invariant intact and now poly-enforced: this file requires
  `ehrt.sim.interface`, never the reverse. This is ADR-0012's own
  long-deferred \"ehr sim mount\" (notes/tools/ADRs.md; provenance note
  notes/tools/ehr-testing-sim-mounting-note.md), fulfilled once the
  classpath question it named actually resolved.

  Promotes projects/conformance/test/ehrt/tools/sim_harness.clj's own
  former subprocess seam into src/, so the sim generator source
  (ehrt.tools.corpus.generators), the CLI's own `ehr sim` mount
  (bases/ehr-cli), and the test harness all drive the SAME code.
  There is deliberately no `available?` here anymore: sim is always on
  this classpath, never something to discover or degrade gracefully
  without -- see ADR-0005 for why the old skip-when-absent machinery
  was removed rather than kept as a permanently-true no-op."
  (:require [ehrt.sim.interface :as sim]))

(defn run!
  "Delegates straight to ehrt.sim.interface/run-command -- opts pass
  through unchanged (:seed, :patients, :reference-date, :emit, :churn,
  :config, ...; see run-command's own docstring for the full set).
  Already a proper Result map (sim's own Result-not-throw discipline is
  structurally, not just nominally, the same shape as this repo's own
  -- ADR-0012 property 3), so there is nothing left to unwrap, parse,
  or reshape the way the subprocess version had to. :out-dir (the old
  subprocess stdout/stderr log location) and every discovery-related
  key (:sim-dir, :env-sim-dir-fn, :default-dir) are accepted and
  ignored -- harmless no-ops for any caller still passing them, rather
  than a breaking signature change to every call site in one commit.

  :run-command-fn is injectable, pulled out of the SAME opts map
  (defaults to sim/run-command, the real in-process engine call) --
  the same single-map -fn convention ehrt.tools.corpus.generate's own
  :run-invocation/:resolve-artifact/:resolve-java-bin already use, so
  ehrt.tools.corpus.generators' :sim entry (which calls `(sim/run!
  params)` with one argument, the same way it calls
  ehrt.tools.corpus.generate/generate!) can inject a fake exactly like
  it already does for :synthea, and this namespace's own tests never
  run a real simulation just to prove the delegation and key-stripping
  are correct."
  [opts]
  (let [run-command-fn (get opts :run-command-fn sim/run-command)]
    (run-command-fn (dissoc opts :out-dir :sim-dir :env-sim-dir-fn :default-dir :run-command-fn))))
