(ns ehrt.sim-trajectory.interface
  "sim-trajectory split (sim split S2, .agents/plans/2026-08-02-sim-split-
  plan.md, AR-6): re-exports exactly the vars real callers outside this
  component use -- determined by grep against components/sim's own src
  and test trees before the move (residual sim's `engine` and `run`,
  confirmed the only two, matching the plan's own prediction), not by
  interface-design judgment. `gmf` -> `gmf-interpreter` -> pathway IR via
  `compile-trajectory`, depending on `sim-model` (pathway/facility) and
  `kernel` only -- never on `sim` itself."
  (:require [ehrt.sim-trajectory.compile-trajectory :as compile-trajectory]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim-trajectory.gmf-interpreter :as gmf-interpreter]))

(defn load-module [module-name json-string] (gmf/load-module module-name json-string))
(defn valid-modules-config? [modules-config] (gmf/valid-modules-config? modules-config))

(defn run-module
  ([module rng persona registration-t]
   (gmf-interpreter/run-module module rng persona registration-t))
  ([module rng persona registration-t horizon-end-t]
   (gmf-interpreter/run-module module rng persona registration-t horizon-end-t)))

(defn compile-trajectory [trajectory facility registration-t]
  (compile-trajectory/compile-trajectory trajectory facility registration-t))
