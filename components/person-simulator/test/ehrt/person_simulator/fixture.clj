(ns ehrt.person-simulator.fixture
  "The ONE witness population every gate in this component runs
  against: sixty persons, twenty-four years, master seed 42.

  It is a fixture and not a per-test config on purpose. The counted
  witness (`ehrt.person-simulator.witness-test`) pins how many of each
  of ADR-0172's fourteen event kinds this exact config+seed produces,
  and every other gate here asserts a law over the SAME stream -- so a
  law that holds only because its population was empty is caught by
  the witness rather than passing quietly
  (`rulings.md#R-empty-population-is-red`).

  The payer pools are supplied by config because `sim-model`'s own
  `under-65-payers` / `sixty-five-plus-payers` are private to
  `ehrt.sim-model.persona` and not on its interface; see
  `ehrt.person-simulator.process/payer-pool`. They are DELIBERATELY
  not copies of sim-model's rows -- a short pool named as a fixture
  cannot be mistaken for a vendored table."
  (:require [ehrt.person-simulator.interface :as ps]))

(def master 42)

(def stream {:master master})

(def population
  (vec (for [i (range 1 61)] {:person-id (format "p-%03d" i) :id-tag i})))

(def payers-under-65
  [{:id "fixture-commercial" :name "Fixture Commercial" :type :commercial :weight 45.0}
   {:id "fixture-medicaid" :name "Fixture Medicaid" :type :medicaid :weight 22.0}
   {:id "fixture-self-pay" :name "Fixture Self-Pay" :type :self-pay :weight 8.0}])

(def payers-65-plus
  [{:id "fixture-medicare" :name "Fixture Medicare" :type :medicare :weight 70.0}
   {:id "fixture-medicare-advantage" :name "Fixture Medicare Advantage" :type :medicare :weight 15.0}])

(def config
  {:t0 0
   :years 24
   :population population
   :persona {}
   :payers-under-65 payers-under-65
   :payers-65-plus payers-65-plus
   :identification {:merge-fraction 0.35}
   :deaths {}})

(def events (delay (ps/persons config stream)))

(defn evs [] @events)

(defn of-kind [kind] (filterv #(= kind (:event %)) (evs)))

(def by-id (delay (into {} (map (juxt :event-id identity) (evs)))))

(defn personas
  "person-id -> the Persona that person entered the run with. t0
  persons have theirs sampled by `initial-persona`; a newborn's rides
  its own `:person-registered`."
  []
  (into {} (for [e (of-kind :person-registered)] [(:person-id e) (:persona e)])))
