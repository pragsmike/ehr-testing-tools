(ns ehrt.corpus.sim-adapter
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
  long-deferred \"ehrt sim mount\" (notes/tools/ADRs.md; provenance note
  notes/tools/ehr-testing-sim-mounting-note.md), fulfilled once the
  classpath question it named actually resolved.

  Promotes projects/conformance/test/ehrt/conformance/sim_harness.clj's own
  former subprocess seam into src/, so the sim generator source
  (ehrt.corpus.generators), the CLI's own `ehrt sim` mount
  (bases/cli), and the test harness all drive the SAME code.
  There is deliberately no `available?` here anymore: sim is always on
  this classpath, never something to discover or degrade gracefully
  without -- see ADR-0005 for why the old skip-when-absent machinery
  was removed rather than kept as a permanently-true no-op.

  `:refer-clojure :exclude [run!]` (cold-start UX session, 2026-07-30):
  this namespace's own public `run!` below shadows `clojure.core/run!`
  -- silences the resulting `already refers to` load-time warning,
  which otherwise printed as the first line of output on every cold
  invocation of this CLI. No behavior change: nothing in this namespace
  calls `clojure.core/run!`."
  (:refer-clojure :exclude [run!])
  (:require [ehrt.sim.interface :as sim]))

(defn run!
  "Delegates straight to ehrt.sim.interface/run-command -- opts pass
  through unchanged (:seed, :patients, :reference-date, :emit, :churn,
  :config, ...; see run-command's own docstring for the full set).
  Already a proper Result map (sim's own Result-not-throw discipline is
  structurally, not just nominally, the same shape as this repo's own
  -- ADR-0012 property 3), so there is nothing left to unwrap, parse,
  or reshape the way the subprocess version had to. :out-dir (the old
  subprocess stdout/stderr log location) is accepted and ignored --
  a harmless no-op for any caller still passing it, rather than a
  breaking signature change to every call site in one commit.

  Retired (2026-08-05, scaffolding compaction A, AR-A-3): the
  sibling-checkout discovery keys (:sim-dir, :env-sim-dir-fn,
  :default-dir) are no longer accepted-and-ignored -- ADR-0012's own
  in-process mount (2026-07-28) made sibling-checkout discovery dead
  code, and this session's own fresh grep found zero callers still
  passing them. A caller that still passes them today gets no
  special handling -- ordinary unused map entries, same as any other
  unknown key.

  :run-command-fn is injectable, pulled out of the SAME opts map
  (defaults to sim/run-command, the real in-process engine call) --
  the same single-map -fn convention ehrt.corpus.generate's own
  :run-invocation/:resolve-artifact/:resolve-java-bin already use, so
  ehrt.corpus.generators' :sim entry (which calls `(sim/run!
  params)` with one argument, the same way it calls
  ehrt.corpus.generate/generate!) can inject a fake exactly like
  it already does for :synthea, and this namespace's own tests never
  run a real simulation just to prove the delegation and key-stripping
  are correct."
  [opts]
  (let [run-command-fn (get opts :run-command-fn sim/run-command)]
    (run-command-fn (dissoc opts :out-dir :run-command-fn))))

(defn check!
  "Delegates to ehrt.sim.interface/check-command -- the ground-truth log
  plus whatever config the caller has (`:config`, and the `:facility`/
  `:warm-up-seconds` an explicit caller may pass directly). Q14(a)
  (2026-09-01): before this it called the 1-arg `check-all` arity and
  dropped every opt, so `ehrt sim check` could not be told what facility
  produced the log it was judging -- see `ehrt.sim.run/check-command`.

  With no config opts at all, `check-command` reduces to exactly the
  1-arg `check-all` call this delegated to before, so the flagless path
  is byte-identical.

  :check-all-fn is still injectable, same -fn convention as run!'s own
  :run-command-fn, and still takes the log alone -- an injected checker
  short-circuits the config step rather than being handed a config it
  never asked for."
  ([ground-truth] (check! ground-truth {}))
  ([ground-truth {:keys [check-all-fn] :as opts}]
   (if check-all-fn
     (check-all-fn ground-truth)
     (sim/check-command ground-truth opts))))

(defn identifiers!
  "Delegates to ehrt.sim.interface/identifiers-command -- opts pass
  through unchanged (:seed, :patients, :config; see
  identifiers-command's own docstring for the full config surface,
  shared with run!). :identifiers-fn is injectable, same -fn
  convention as run!'s own :run-command-fn (P3-6 parity mount,
  2026-08-01)."
  [opts]
  (let [identifiers-fn (get opts :identifiers-fn sim/identifiers-command)]
    (identifiers-fn (dissoc opts :identifiers-fn))))

(defn version!
  "Delegates to ehrt.sim.interface/version + git-sha -- the SAME source
  the run manifest's own :generator block stamps (ehrt.sim.manifest),
  not this repo's own `ehrt version` identity (a different concept:
  the repo's pre-release identity plus pinned artifacts, vs. sim's own
  library version marker). :git-sha-fn is injectable, matching the -fn
  convention run!/check!/identifiers! already use (P3-6 parity mount,
  2026-08-01)."
  ([] (version! {}))
  ([{:keys [git-sha-fn] :or {git-sha-fn sim/git-sha}}]
   {:version sim/version :git-sha (git-sha-fn)}))
