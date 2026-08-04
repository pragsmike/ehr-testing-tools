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

(defn load-closure
  ([root-id root-json-text resolve-fn]
   (gmf/load-closure root-id root-json-text resolve-fn))
  ([root-id root-json-text resolve-fn table-resolve-fn]
   (gmf/load-closure root-id root-json-text resolve-fn table-resolve-fn)))

(defn singleton-closure [module] (gmf/singleton-closure module))

(defn run-module
  ([module rng persona registration-t]
   (gmf-interpreter/run-module module rng persona registration-t))
  ([module rng persona registration-t horizon-end-t]
   (gmf-interpreter/run-module module rng persona registration-t horizon-end-t))
  ;; ADR-0033 AR-3: the full arity, purely additive -- `:registered`'s
  ;; own decide method (ehrt.sim-engine.engine) now calls this one, threading a
  ;; closure's own `modules`/`tables` maps and an optional per-patient
  ;; `initial-attributes` seed straight through to the interpreter.
  ([module rng persona registration-t horizon-end-t modules initial-attributes tables]
   (gmf-interpreter/run-module module rng persona registration-t horizon-end-t modules initial-attributes tables))
  ;; ADR-0042 AR-1/AR-3 (Wave H pre-roll): the full 9-arity, purely
  ;; additive -- `history?` gates the interpreter's own `:phase` mint,
  ;; `:registered`'s own decide method passes this run's `:history`
  ;; config flag straight through.
  ([module rng persona registration-t horizon-end-t modules initial-attributes tables history?]
   (gmf-interpreter/run-module module rng persona registration-t horizon-end-t modules initial-attributes tables history?)))

(defn compile-trajectory
  ([trajectory facility registration-t]
   (compile-trajectory/compile-trajectory trajectory facility registration-t))
  ;; ADR-0042 AR-1/AR-3: the optional 4th argument, purely additive --
  ;; see `ehrt.sim-trajectory.compile-trajectory/compile-trajectory`'s
  ;; own docstring for the gated new path this threads to.
  ([trajectory facility registration-t history?]
   (compile-trajectory/compile-trajectory trajectory facility registration-t history?)))
