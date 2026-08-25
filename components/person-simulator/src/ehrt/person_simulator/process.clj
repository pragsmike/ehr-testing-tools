(ns ehrt.person-simulator.process
  "`persons`: the run's person-event stream.

  SKELETON (arc 2b step 2). The walk is not written yet -- `persons`
  throws `not-implemented`, so every gate in this commit is red for
  exactly that one reason. What IS here is the data those gates must
  read out of the component rather than out of a copy of their own: the
  address pool a residence move draws from, and the size of the
  per-person-year draw block. A gate that carried its own copy of
  either would go green against a component that had drifted away from
  it."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.person-simulator.not-implemented :as ni]))

(def places
  "`sim-model`'s own vendored address pool, read as a resource -- the
  flat 24-row weighted pool `ehrt.sim-model.persona` draws a t0
  address from. A residence move draws a whole new row from the SAME
  pool (`rulings.md#R-mix-3`, limitations row 7): no adjacency, no
  distance, no local-versus-cross-country move."
  (edn/read-string (slurp (io/resource "sim-model/demographics/places.edn"))))

(def draws-per-person-year
  "Eighteen variates per person-year, fired or not, branch taken or
  not. The number ADR-0172's fixed-consumption paragraph turns into an
  arithmetic the consumption gate can check: a person walked for N
  years consumes exactly 18N, whatever happens to them."
  18)

(defn persons
  [config stream]
  (ni/not-implemented `persons {:config config :stream stream}))
