(ns ehrt.judge.pairing
  "The mutate<->judge pairing registry (pairing-as-data, ADR-0088;
  review P3-3, ADR-0050 AR-F1-6/D-3 named `judge` as its accepted
  acyclic home). Prior art: `ehrt.judge.finding`'s `Severity` docstring
  names \"P5's contract-pairing exercise\" -- the ad hoc precedent this
  registry generalizes into checked data, one WITNESSED row per
  operator x judge pair this project has actually exercised (mutate a
  real fixture, gate it through a real judge, observe the finding
  class), never a full operators x judges matrix asserted from the
  catalog's own :contract prose alone.

  Per AR-PD-1 (the design pass's own granularity ruling): a row lands
  only when exercised in-session. This namespace stays a LEAF within
  `judge` -- it knows nothing about `corpus`'s operator catalog or
  either v2 judge engine; the registry's own :operator/:judge keys are
  plain data (id/version, and an enum keyword respectively), read and
  cross-referenced by test-tier code that legitimately crosses those
  bricks (`components/judge/test/ehrt/judge/pairing_conviction_test.clj`),
  not by this namespace itself. Keeps the dependency arrow the design
  pass named: operators (and their consuming tests) reference this
  registry, never the reverse."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [malli.core :as m]))

(def JudgeId
  "The two v2 judges this session witnessed rows against (AR-PD-2: v2
  first, no FHIR rows this session). A future FHIR-rows session grows
  this enum; it is not speculative pre-registration -- each of these
  two entries has at least one witnessed row below."
  [:enum :judge-v2-hapi :judge-v2-nist])

(def PairingRow
  "One witnessed row (AR-PD-1's own shape, plus this registry's own
  two disclosed additions -- see the committed EDN resource's own
  header comment for why :locator and :profile exist: a row without
  its own replay locator cannot be replayed by the tier-one test, and
  a judge-v2-nist row needs its own profile-bundle directory alongside
  its message fixture)."
  [:map
   [:operator [:map [:id :keyword] [:version :string]]]
   [:judge JudgeId]
   [:locator [:string {:min 1}]]
   [:expected [:set {:min 1} [:string {:min 1}]]]
   [:fixture [:string {:min 1}]]
   [:profile {:optional true} [:string {:min 1}]]
   [:witness [:map [:adr :string] [:date :string]]]])

(def Registry
  [:vector PairingRow])

(def ^:private registry-resource "judge/pairing-registry.edn")

(defn load-registry
  "Loads and schema-validates the committed pairing registry (a
  resource on the classpath). Throws ex-info if the resource is
  missing or fails validation -- a malformed or missing registry is a
  build-time defect, not an operational condition this fn routes
  through a kernel/error result."
  []
  (let [res (io/resource registry-resource)]
    (when-not res
      (throw (ex-info "pairing registry resource not found" {:resource registry-resource})))
    (let [rows (edn/read-string (slurp res))]
      (when-not (m/validate Registry rows)
        (throw (ex-info "pairing registry failed schema validation"
                         {:resource registry-resource
                          :explain (m/explain Registry rows)})))
      rows)))

(defn coverage
  "Tier-two, report-only (AR-PD-4): for every id in `operator-ids`, the
  set of judges `rows` witnesses at least one row against -- an empty
  set for an operator with zero witnessed rows. Pure; takes
  `operator-ids` as an argument rather than reaching for `corpus`'s own
  operator catalog directly, keeping this leaf component free of a new
  dependency edge -- callers needing the live catalog (e.g. a test
  crossing into `ehrt.corpus.interface`) supply it."
  [rows operator-ids]
  (let [witnessed (reduce (fn [acc {:keys [operator judge]}]
                             (update acc (:id operator) (fnil conj #{}) judge))
                           {}
                           rows)]
    (into (sorted-map)
          (map (fn [id] [id (get witnessed id #{})]))
          operator-ids)))
