(ns ehrt.sim.interface
  "Deliberately wide (migration ruling R5, notes/ADRs.md ADR-0001).
  Re-exports exactly what bases/sim-cli's own src calls from outside
  its own namespace -- determined by grep against the pre-merge
  ehr-testing-sim repo, not by interface-design judgment. Narrowing
  this surface (splitting components/sim, tightening what's exposed)
  is a future, author-ruled extraction session's call -- see
  AGENTS.md's fat-component disclosure. Don't treat this file's width
  as evidence about how components/sim should be decomposed.

  2026-08-04 (sim split B, M4, `notes/ADRs.md` ADR-0043): the
  extraction this docstring disclosed is complete. Every concern that
  can be named as its own domain -- sim-model, sim-trajectory,
  sim-engine, sim-emit-hl7, sim-emit-fhir, sim-check, provenance -- now
  lives in its own component; residual `components/sim` is pure
  orchestration (`run`, `identifiers`, `version`, `manifest`) behind
  this SAME façade, unchanged in width or shape (`.agents/plans/
  2026-08-02-sim-split-plan.md` AR-3, honored across every stage:
  corpus depends on this interface's own stability, ADR-0012). Any
  future narrowing of this façade itself is a separate, author-ruled
  decision -- not a consequence of the split completing."
  (:require [ehrt.kernel.interface :as result]
            [ehrt.sim-check.interface :as check]
            [ehrt.sim.identifiers :as identifiers]
            [ehrt.sim.run :as run]
            [ehrt.sim.version :as version]))

(defn ok [payload] (result/ok payload))
(defn rejected [category payload] (result/rejected category payload))
(defn error [category payload] (result/error category payload))
(defn ok? [r] (result/ok? r))
(defn rejected? [r] (result/rejected? r))

(defn check-all
  ([ground-truth] (check/check-all ground-truth))
  ([ground-truth facility-config] (check/check-all ground-truth facility-config))
  ([ground-truth facility-config warm-up-seconds]
   (check/check-all ground-truth facility-config warm-up-seconds))
  ([ground-truth facility-config warm-up-seconds order-profiles-config]
   (check/check-all ground-truth facility-config warm-up-seconds order-profiles-config)))

(defn identifiers-command [opts] (identifiers/identifiers-command opts))

(defn run-command [opts] (run/run-command opts))

(def version version/version)
(defn git-sha [] (version/git-sha))
