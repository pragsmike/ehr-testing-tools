(ns ehr-testing-sim.check
  "The invariant catalog: internal-consistency claims about a
  ground-truth log, machine-checkable (validation program, claim #3 in
  docs/problem-statement.md). Runs standalone as `sim check` and in CI
  as the regression suite; the property tests in test/ drive the same
  functions over generated runs, so the catalog does double duty.

  Each invariant is a named function (ground-truth) -> seq of violation
  maps (empty = holds). `check-all` aggregates to a Result: :ok when
  every invariant holds, :rejected (:category :invariant-violation)
  otherwise -- the check ran and the answer is no, which is exactly
  what the result-not-throw doctrine's :rejected arm is for.

  v0 catalog is minimal; every new step type added to the engine MUST
  land with its invariants here in the same change."
  (:require [ehr-testing-sim.result :as result]))

(defn- by-patient [ground-truth]
  (group-by :mrn ground-truth))

(defn timestamps-monotone
  "Within a patient, event times never decrease (log order is emission
  order, which the engine guarantees is time order)."
  [ground-truth]
  (for [[mrn events] (by-patient ground-truth)
        [a b] (partition 2 1 events)
        :when (> (:t a) (:t b))]
    {:invariant :timestamps-monotone :mrn mrn :at [(:t a) (:t b)]}))

(defn discharge-follows-admission
  "No patient is discharged without a prior admission, and not twice."
  [ground-truth]
  (for [[mrn events] (by-patient ground-truth)
        :let [kinds (mapv :event events)
              first-admit (.indexOf ^java.util.List kinds :admission)
              discharges (keep-indexed #(when (= :discharge %2) %1) kinds)]
        d discharges
        :when (or (neg? first-admit) (< d first-admit))]
    {:invariant :discharge-follows-admission :mrn mrn :at d}))

(def catalog
  "The full invariant catalog, in reporting order."
  [#'timestamps-monotone
   #'discharge-follows-admission])

(defn check-all
  "Runs every invariant in the catalog over a ground-truth log."
  [ground-truth]
  (let [violations (into [] (mapcat #(% ground-truth)) catalog)]
    (if (empty? violations)
      (result/ok {:invariants-checked (mapv (comp :name meta) catalog)
                  :events (count ground-truth)})
      (result/rejected :invariant-violation {:violations violations}))))
