(ns ehrt.tools.corpus.generators
  "The generator registry (D1/D7, docs/source-sink-design.md Parts I and
  VII; SS-2 Step 1): shaped like ehrt.tools.corpus.operators's
  own registry (register!/lookup/entries) -- adding a generator means
  registering an entry here, not writing a new per-kind adapter.

  An entry is a recipe, not code: :default-params (pinned constants,
  the D8 determinism law -- never the clock/environment/machine) merged
  UNDER whatever params a caller supplies, :params-schema (a Malli
  schema validating the MERGED map, open by default so kind-specific
  extras -- including injected-fake dependencies in tests -- pass
  through unvalidated at this level, same convention as
  ehrt.tools.corpus.source-sink's own open :map schemas),
  :out-dir-fn (merged params -> a deterministic output directory, D9's
  own derived-path pattern generalized past synthea alone), and
  :execute-fn (merged params x out-dir -> a Result, driving whatever
  engine seam that kind needs -- corpus.generate's own two-step engine
  for :synthea, ehrt.tools.sim's subprocess adapter for :sim,
  SS-2 Step 3).

  This namespace owns the registry and per-kind param resolution only;
  ehrt.tools.corpus.generator-source (SS-2 Step 2) owns the
  unification (execute, verify, wrap as a dir Source) that actually
  CALLS an entry's :out-dir-fn/:execute-fn -- the same registry-vs-
  consumption split operators.clj draws against corpus.mutate."
  (:require [clojure.java.io :as io]
            [malli.core :as m]
            [ehrt.tools.corpus.generate :as generate]
            [ehrt.kernel.interface :as kernel]
            [ehrt.tools.sim :as sim]))

(def GeneratorEntry
  [:map
   [:kind :keyword]
   [:default-params [:map-of :keyword :any]]
   [:params-schema :any]
   [:out-dir-fn [:fn fn?]]
   [:execute-fn [:fn fn?]]])

(defonce ^:private registry (atom {}))

(defn register!
  "Registers a generator entry, keyed by :kind. Returns kernel/ok
  {:kind} or kernel/rejected :invalid-generator-entry."
  [entry]
  (if (m/validate GeneratorEntry entry)
    (do (swap! registry assoc (:kind entry) entry)
        (kernel/ok (select-keys entry [:kind])))
    (kernel/rejected :invalid-generator-entry {:entry entry})))

(defn lookup
  [kind]
  (get @registry kind))

(defn entries
  []
  (vals @registry))

(defn registry-snapshot
  "Test/dev support: the full registry map, keyed by :kind -- for
  saving and later restoring exact state, same convention as
  ehrt.tools.canonical/corpus.operators."
  []
  @registry)

(defn reset-registry!
  ([] (reset-registry! {}))
  ([snapshot] (reset! registry snapshot)))

(defn resolve-params
  "Merges params onto kind's own pinned :default-params (D8 -- a
  param a caller omits falls back to a pinned constant, never the
  clock/environment/machine), then validates the merged map against
  the registered :params-schema. Returns kernel/ok the merged params,
  or kernel/rejected :unknown-generator-kind (naming every registered
  kind, DOC-1's enumerable-options convention) / :invalid-generator-
  params (malli's own explain)."
  [kind params]
  (if-let [entry (lookup kind)]
    (let [merged (merge (:default-params entry) params)]
      (if (m/validate (:params-schema entry) merged)
        (kernel/ok merged)
        (kernel/rejected :invalid-generator-params
                          {:kind kind :params merged
                           :explain (m/explain (:params-schema entry) merged)})))
    (kernel/rejected :unknown-generator-kind
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

;; ---- seed catalog: sim, over ehrt.tools.sim's own subprocess
;; adapter (Step 3, D7). :patients/:emit get their own pinned defaults
;; here (sim's own CLI has no zero-flag default for either that would
;; produce a v2 corpus by itself -- :emit in particular defaults to
;; nothing upstream, which would produce zero messages) so that a
;; zero-param `sim:` URL still means something: one patient, HL7v2
;; messages emitted. :seed reuses generate/default-seed (the SAME
;; pinned value synthea's own zero-param URL uses) for one shared
;; convention across every registered generator, not a second,
;; independently-chosen constant. ----

(def sim-params-schema
  [:map
   [:seed {:optional true} :int]
   [:patients {:optional true} :int]
   [:churn {:optional true} :boolean]
   [:emit {:optional true} :string]
   [:reference-date {:optional true} :string]
   [:config {:optional true} :string]])

(defn- spool-sim-output!
  "Writes sim's own run! payload to out-dir: one .hl7 file per message
  (:messages), plus sim's own :manifest verbatim as manifest.edn --
  this repo writes no manifest of its own for sim output; provenance
  is the generator's word (ruling 4, docs/source-sink-design.md D7).
  Returns kernel/error :sim-produced-no-messages, writing NOTHING, when
  the run's own payload carried no messages at all -- an all-metadata
  directory (manifest.edn alone) would defeat generator-source/
  resolve!'s own generic empty-output check, since manifest.edn alone
  makes the directory non-empty."
  [{:keys [messages manifest]} out-dir]
  (if (empty? messages)
    (kernel/error :sim-produced-no-messages
                  {:hint (str "sim's own run produced no messages -- :emit \"hl7\" "
                              "(this entry's own pinned default) is required to produce a v2 corpus")})
    (do
      (.mkdirs (io/file out-dir))
      (dorun (map-indexed (fn [i m] (spit (io/file out-dir (format "msg-%03d.hl7" i)) m)) messages))
      (spit (io/file out-dir "manifest.edn") (pr-str manifest))
      (kernel/ok {:out-dir out-dir}))))

(register!
 {:kind :sim
  :default-params {:seed generate/default-seed :patients 1 :emit "hl7"}
  :params-schema sim-params-schema
  :out-dir-fn (fn [{:keys [seed patients]}] (str "target/corpus/sim-s" seed "-p" patients))
  :execute-fn (fn [params out-dir]
                (let [run-result (sim/run! params)]
                  (if-not (kernel/ok? run-result)
                    run-result
                    (spool-sim-output! (:payload run-result) out-dir))))})
