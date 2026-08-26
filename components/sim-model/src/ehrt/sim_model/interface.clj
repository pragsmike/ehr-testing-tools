(ns ehrt.sim-model.interface
  "sim-model split (sim split S1, .agents/plans/2026-08-02-sim-split-plan.md,
  AR-6): re-exports exactly the vars real callers outside this component
  use -- determined by grep against components/sim's own src and test
  trees before the move, not by interface-design judgment (same
  discipline the fat ehrt.sim.interface itself was built with, R5,
  notes/ADRs.md ADR-0001). pathway/facility/persona/config are schema-
  and-sampling namespaces with no cross-namespace dependency among
  themselves or on any other sim namespace -- this interface is the
  only path the residual sim component (and, from sim split S2 on,
  patient-simulator) may reach them through."
  (:require [ehrt.sim-model.config :as config]
            [ehrt.sim-model.facility :as facility]
            [ehrt.sim-model.pathway :as pathway]
            [ehrt.sim-model.persona :as persona]))

;; --- pathway ---------------------------------------------------------

(def Concept pathway/Concept)
(def ConditionAnnotation pathway/ConditionAnnotation)
;; Exported by the event-log contract arc (2026-08-16): a
;; `:diagnostic-report` event's own `:observations` children ARE
;; `ObservationEntry`, and `ehrt.sim-engine.event-schema` reuses this
;; one definition rather than restating the shape. A restatement would
;; be exactly the mirror `ehrt.sim.manifest`'s own retirement
;; disclosure warns about -- a copy validates against itself and
;; agrees with its own mistake. Additive: one new name on this seam,
;; nothing moved.
(def ObservationEntry pathway/ObservationEntry)
(def Citation pathway/Citation)
(def Step pathway/Step)
(def PathwaysConfig pathway/PathwaysConfig)
(def sample-admission-discharge pathway/sample-admission-discharge)

(defn valid? [pathway] (pathway/valid? pathway))
(defn valid-pathways-config? [config] (pathway/valid-pathways-config? config))

;; --- facility ----------------------------------------------------------

(defn ward-by-name [facility ward-name] (facility/ward-by-name facility ward-name))
(defn licensed-bed-ids [ward] (facility/licensed-bed-ids ward))
(defn surge-slot-ids [ward] (facility/surge-slot-ids ward))
(defn occupancy-board [patients] (facility/occupancy-board patients))
(defn ward-census [facility board] (facility/ward-census facility board))
(defn allocate [rng facility board home-ward-name force-placement]
  (facility/allocate rng facility board home-ward-name force-placement))
(defn choose-attending [rng providers ward-id] (facility/choose-attending rng providers ward-id))

;; --- persona -------------------------------------------------------------

(def Persona persona/Persona)
(defn valid-persona? [p] (persona/valid-persona? p))
(defn persona [rng config] (persona/persona rng config))

;; The two payer pools `persona` itself defaults to, PROMOTED onto this
;; façade 2026-08-26 (arc 3a part 4) to close an interface gap arc 2b
;; recorded rather than worked around.
;;
;; `ehrt.person-simulator.process` draws a coverage change from "the
;; same `:payers-under-65` / `:payers-65-plus` keys a run already
;; supplies to `sim-model/persona`", and its own docstring says a run
;; that supplies NEITHER gets no `:coverage-change` events at all (the
;; variates are still drawn). That gap made a declared 1.3.0 event kind
;; unreachable from any config that did not restate the pools -- and
;; restating them in a scenario file is exactly the forked-pool drift
;; that component's docstring forbids for addresses, for the same
;; reason. Exposing the real ones is the fix that removes a fork
;; instead of creating one: `ehrt.sim.run/person-walk-config` defaults
;; to these, so a run's people and its patients draw coverage from ONE
;; pool set by construction.
(def under-65-payers persona/under-65-payers)
(def sixty-five-plus-payers persona/sixty-five-plus-payers)
(defn reference-today-epoch-day [] (persona/reference-today-epoch-day))

;; --- config --------------------------------------------------------------

(def default-facility config/default-facility)
(def default-provider-templates config/default-provider-templates)
(defn materialize-providers [rng provider-templates] (config/materialize-providers rng provider-templates))
