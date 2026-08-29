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
(defn allocate
  ([rng facility board home-ward-name force-placement]
   (facility/allocate rng facility board home-ward-name force-placement))
  ([rng facility board beds home-ward-name force-placement]
   (facility/allocate rng facility board beds home-ward-name force-placement)))

;; ARC 3B SWEEP 2 (ADR-0174 section 2(c)): the bed-status cycle's own
;; three facility-level primitives. `free` is promoted here because the
;; `:ready` gate has to be ONE predicate across three namespaces --
;; sim-model's own ladder, sim-engine's `bed-ready-location`, and
;; sim-check's `earlier-rungs-exhausted?` -- and a same-looking copy in
;; any of them is the drift this promotion exists to make impossible.
(defn free [ids board beds] (facility/free ids board beds))
(defn initial-beds [facility] (facility/initial-beds facility))
(defn ward-of-bed [facility bed] (facility/ward-of-bed facility bed))
(defn bed-placement [facility bed] (facility/bed-placement facility bed))
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
;; ARC 3B SWEEP 2 (ADR-0174 ruling D1): the ONE reading of a ward's
;; turnaround range, promoted so `ehrt.sim-engine.engine`'s cycle draws
;; through it rather than reaching for the key itself.
(defn turnaround-minutes [ward] (config/turnaround-minutes ward))
(def default-provider-templates config/default-provider-templates)
(defn materialize-providers [rng provider-templates] (config/materialize-providers rng provider-templates))

;; --- ARC 4 SWEEP 2 (ADR-0175 design (a)): the chatter config surface ------
;; `ehrt.sim.run` validates `:chatter` BEFORE the engine (and its RNG)
;; ever starts, the same fail-fast-on-a-bad-config posture a missing
;; `--seed` and a malformed `:persons` already get -- so the schema has
;; to be reachable from outside this component, unlike
;; `LatencyProfile`'s own validators, which nothing ever called.
(def ChatterProfile config/ChatterProfile)
(defn valid-chatter-profile? [profile] (config/valid-chatter-profile? profile))
(defn explain-chatter-profile [profile] (config/explain-chatter-profile profile))

;; --- ARC 4 SWEEP 2 (ADR-0175 design (c)): the charge config surface ------
(def ChargesProfile config/ChargesProfile)
(defn valid-charges-profile? [profile] (config/valid-charges-profile? profile))
(defn explain-charges-profile [profile] (config/explain-charges-profile profile))

;; --- ARC 4 SWEEP 3 (ADR-0175 design (b)): the status-ladder config
;; surface, reachable from outside for the same fail-fast reason.
(def LadderProfile config/LadderProfile)
(defn valid-ladder-profile? [profile] (config/valid-ladder-profile? profile))
(defn explain-ladder-profile [profile] (config/explain-ladder-profile profile))

;; --- ARC 4 SWEEP 4 (ADR-0175 ruling B1): the SIU config surface ------
(def SiuProfile config/SiuProfile)
(defn valid-siu-profile? [profile] (config/valid-siu-profile? profile))
(defn explain-siu-profile [profile] (config/explain-siu-profile profile))

;; --- ARC 4 SWEEP 5 (ADR-0175 design (f)): the fan-out subscriber
;; table, reachable from outside for the same fail-fast reason the four
;; keys above are -- `ehrt.sim.run` rejects a malformed table BEFORE the
;; engine's RNG starts.
(def FanOutProfile config/FanOutProfile)
(defn valid-fan-out-profile? [profile] (config/valid-fan-out-profile? profile))
(defn explain-fan-out-profile [profile] (config/explain-fan-out-profile profile))
