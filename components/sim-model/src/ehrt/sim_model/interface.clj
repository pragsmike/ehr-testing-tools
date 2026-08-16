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
  sim-trajectory) may reach them through."
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
(defn reference-today-epoch-day [] (persona/reference-today-epoch-day))

;; --- config --------------------------------------------------------------

(def default-facility config/default-facility)
(def default-provider-templates config/default-provider-templates)
(defn materialize-providers [rng provider-templates] (config/materialize-providers rng provider-templates))
