(ns ehr-testing-tools.corpus.generators
  "The generator registry (D1/D7, docs/source-sink-design.md Parts I and
  VII; SS-2 Step 1): shaped like ehr-testing-tools.corpus.operators's
  own registry (register!/lookup/entries) -- adding a generator means
  registering an entry here, not writing a new per-kind adapter.

  An entry is a recipe, not code: :default-params (pinned constants,
  the D8 determinism law -- never the clock/environment/machine) merged
  UNDER whatever params a caller supplies, :params-schema (a Malli
  schema validating the MERGED map, open by default so kind-specific
  extras -- including injected-fake dependencies in tests -- pass
  through unvalidated at this level, same convention as
  ehr-testing-tools.corpus.source-sink's own open :map schemas),
  :out-dir-fn (merged params -> a deterministic output directory, D9's
  own derived-path pattern generalized past synthea alone), and
  :execute-fn (merged params x out-dir -> a Result, driving whatever
  engine seam that kind needs -- corpus.generate's own two-step engine
  for :synthea, ehr-testing-tools.sim's subprocess adapter for :sim,
  SS-2 Step 3).

  This namespace owns the registry and per-kind param resolution only;
  ehr-testing-tools.corpus.generator-source (SS-2 Step 2) owns the
  unification (execute, verify, wrap as a dir Source) that actually
  CALLS an entry's :out-dir-fn/:execute-fn -- the same registry-vs-
  consumption split operators.clj draws against corpus.mutate."
  (:require [malli.core :as m]
            [ehr-testing-tools.corpus.generate :as generate]
            [ehr-testing-tools.result :as result]))

(def GeneratorEntry
  [:map
   [:kind :keyword]
   [:default-params [:map-of :keyword :any]]
   [:params-schema :any]
   [:out-dir-fn [:fn fn?]]
   [:execute-fn [:fn fn?]]])

(defonce ^:private registry (atom {}))

(defn register!
  "Registers a generator entry, keyed by :kind. Returns result/ok
  {:kind} or result/rejected :invalid-generator-entry."
  [entry]
  (if (m/validate GeneratorEntry entry)
    (do (swap! registry assoc (:kind entry) entry)
        (result/ok (select-keys entry [:kind])))
    (result/rejected :invalid-generator-entry {:entry entry})))

(defn lookup
  [kind]
  (get @registry kind))

(defn entries
  []
  (vals @registry))

(defn registry-snapshot
  "Test/dev support: the full registry map, keyed by :kind -- for
  saving and later restoring exact state, same convention as
  ehr-testing-tools.canonical/corpus.operators."
  []
  @registry)

(defn reset-registry!
  ([] (reset-registry! {}))
  ([snapshot] (reset! registry snapshot)))

(defn resolve-params
  "Merges params onto kind's own pinned :default-params (D8 -- a
  param a caller omits falls back to a pinned constant, never the
  clock/environment/machine), then validates the merged map against
  the registered :params-schema. Returns result/ok the merged params,
  or result/rejected :unknown-generator-kind (naming every registered
  kind, DOC-1's enumerable-options convention) / :invalid-generator-
  params (malli's own explain)."
  [kind params]
  (if-let [entry (lookup kind)]
    (let [merged (merge (:default-params entry) params)]
      (if (m/validate (:params-schema entry) merged)
        (result/ok merged)
        (result/rejected :invalid-generator-params
                          {:kind kind :params merged
                           :explain (m/explain (:params-schema entry) merged)})))
    (result/rejected :unknown-generator-kind
                      {:kind kind :valid-options (sort (map :kind (entries)))})))

;; ---- seed catalog: synthea, re-expressed over corpus.generate's own
;; two-step engine (D7). Every default below is corpus.generate's OWN
;; pinned var, imported directly -- never a re-typed copy that could
;; silently drift -- so the zero-param `synthea:` URL means exactly
;; what zero-flag `ehr corpus generate` means (D9). ----

(def synthea-params-schema
  [:map
   [:seed {:optional true} :int]
   [:clinician-seed {:optional true} :int]
   [:population {:optional true} :int]
   [:reference-date {:optional true} :string]
   [:config-path {:optional true} :string]])

(register!
 {:kind :synthea
  :default-params {:seed generate/default-seed
                    :population generate/default-population
                    :reference-date generate/default-reference-date
                    :config-path generate/default-config-path}
  :params-schema synthea-params-schema
  :out-dir-fn (fn [{:keys [seed population]}] (generate/default-out-dir seed population))
  :execute-fn (fn [params out-dir] (generate/generate! (assoc params :out-dir out-dir)))})
