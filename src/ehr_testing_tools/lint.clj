(ns ehr-testing-tools.lint
  "Tier-1 pipeline lint (P6, pattern nursery #13): every catalytic
  resource named in docs/pipeline.edn and docs/use-cases.edn resolves
  to one of the four catalytic targets docs/notation.md defines.

  \"Resolves\" means classified into the correct target KIND, not
  necessarily a concretely pinned instance -- pattern nursery #13's
  own P5 evidence already established this reading (`profile-artifact`:
  \"target 1, artifacts.lock.edn, present but unpinned ... the target
  still resolves even though no entry exists yet\"). Where a concrete
  ref IS declared (a lockfile name+version, a deps.edn coordinate, a
  repo-relative config path, a registry {id version}), this namespace
  verifies it mechanically; a target-1/3 entry with no ref is
  classification-only and passes on that basis alone (matching the
  established reading above); a target-2/4 entry always requires a
  ref, since deps.edn coordinates and in-repo registries are cheap and
  local to check for real.

  `catalytic-resource-targets` is this session's own declared mapping
  -- every catalytic resource name docs/pipeline.edn and
  docs/use-cases.edn use, classified by hand when each was authored.
  Adding a new catalytic resource to either EDN file means adding its
  classification here too, in the same commit -- an unclassified
  resource is exactly the :unclassified violation this lint exists to
  catch.

  External stages ({external: true}) are exempt: this repo makes no
  claim about a black-box stage's own catalytic inputs. Not wired into
  CI yet -- see .agents/plans/corpus-foundations.md's enforcement-wave
  entry; `make lint-pipeline` and this namespace's own test suite are
  the tier-1 enforcement itself, CI wiring is a separate, later step."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [ehr-testing-tools.canonical :as canonical]
            ;; Registration is a load-time side effect of requiring
            ;; these seed-catalog namespaces (same convention as
            ;; corpus.mutate-test's own docstring) -- required here,
            ;; not just transitively via some other namespace, so
            ;; target-4 verification against operator-catalog/
            ;; canonicalizer-set resolves regardless of what else this
            ;; process happened to load first.
            [ehr-testing-tools.corpus.canonicalizers]
            [ehr-testing-tools.corpus.operators :as operators]
            [ehr-testing-tools.check.schemas :as schemas]))

(def registry-lookup-fns
  "Dispatch table for target-4 (in-repo code registry) verification:
  registry keyword -> its [id version] lookup fn."
  {:corpus.operators operators/lookup
   :canonical canonical/lookup
   :check.schemas schemas/lookup})

(def catalytic-resource-targets
  "resource-name -> {:target 1|2|3|4 :ref (optional, target-specific)}.
  :ref shapes: target 1 {:name :version} (artifacts.lock.edn); target
  2 a deps.edn coordinate string (e.g. \"ca.uhn.hapi/hapi-base\");
  target 3 a repo-relative file path string; target 4
  {:registry :id :version}."
  {"synthea-artifact"    {:target 1 :ref {:name "synthea" :version "4.0.0"}}
   "jdk-runtime"         {:target 1 :ref {:name "temurin-jdk" :version "17.0.19+10"}}
   "runtime"             {:target 1 :ref {:name "temurin-jdk" :version "17.0.19+10"}}
   "validator-artifact"  {:target 1 :ref {:name "fhir-validator-cli" :version "6.9.12"}}
   ;; No IG pinned in artifacts.lock.edn this session (gate.fhir's own
   ;; docstring; docs/pipeline.edn's Gate :contract) -- classification
   ;; only, per the established P5 reading quoted in this ns docstring.
   "profile-artifact"    {:target 1}
   "config-hash"         {:target 3 :ref "config/synthea/synthea.properties"}
   "operator-catalog"    {:target 4 :ref {:registry :corpus.operators :id :remove-required-element :version "1"}}
   "hapi-hl7v2-dep"      {:target 2 :ref "ca.uhn.hapi/hapi-base"}
   ;; expected-corpus/assertion-set: Check's own :contract
   ;; (docs/pipeline.edn) -- hashed repo-authored config OR an
   ;; intaken artifact, invocation-specific (there is no single fixed
   ;; path this lint could check); classification only.
   "expected-corpus"     {:target 3}
   "assertion-set"       {:target 3}
   "canonicalizer-set"   {:target 4 :ref {:registry :canonical :id :strip-run-timestamp-suffix :version "1"}}})

;; ---- per-target verification ----

(defn- verify-target-1
  [{:keys [ref]}]
  (if-not ref
    {:ok? true :note "classified (artifacts.lock.edn), not yet concretely pinned"}
    (let [lockfile (edn/read-string (slurp "artifacts.lock.edn"))
          found? (boolean (some #(and (= (:name ref) (:name %)) (= (:version ref) (:version %)))
                                 (:artifacts lockfile)))]
      {:ok? found? :note (if found?
                            (str (:name ref) "@" (:version ref) " found in artifacts.lock.edn")
                            (str (:name ref) "@" (:version ref) " not found in artifacts.lock.edn"))})))

(defn- verify-target-2
  [{:keys [ref]}]
  (if-not ref
    {:ok? false :note "target 2 (deps.edn) requires a coordinate ref"}
    (let [deps-edn (edn/read-string (slurp "deps.edn"))
          coord (symbol ref)
          found? (contains? (:deps deps-edn) coord)]
      {:ok? found? :note (if found? (str coord " found in deps.edn :deps") (str coord " not found in deps.edn :deps"))})))

(defn- verify-target-3
  [{:keys [ref]}]
  (if-not ref
    {:ok? true :note "classified (hashed repo-authored config), invocation-specific -- no single fixed path to check"}
    (let [exists? (.exists (io/file ref))]
      {:ok? exists? :note (str "path " ref (if exists? " exists" " does not exist"))})))

(defn- verify-target-4
  [{:keys [ref]}]
  (if-not ref
    {:ok? false :note "target 4 (in-repo registry) requires a {registry id version} ref"}
    (let [{:keys [registry id version]} ref
          lookup-fn (get registry-lookup-fns registry)]
      (cond
        (nil? lookup-fn)
        {:ok? false :note (str "unknown registry " registry " -- add it to lint/registry-lookup-fns")}

        (nil? (lookup-fn id version))
        {:ok? false :note (str "no entry " id "@" version " in registry " registry)}

        :else
        {:ok? true :note (str "resolved " id "@" version " in registry " registry)}))))

(defn- verify-classification
  [{:keys [target] :as classification}]
  (case target
    1 (verify-target-1 classification)
    2 (verify-target-2 classification)
    3 (verify-target-3 classification)
    4 (verify-target-4 classification)
    {:ok? false :note (str "unknown target kind " (pr-str target) " -- must be one of 1, 2, 3, 4")}))

;; ---- extraction: catalytic resource names actually used ----

(defn- pipeline-catalytic-resources
  [pipeline-edn-path]
  (let [{:keys [stages]} (edn/read-string (slurp pipeline-edn-path))]
    (set (mapcat :catalytic stages))))

(defn- line-catalytic-resources
  "Extracts the {catalytic: a, b, c} resource names from one raw
  equation-line string (docs/use-cases.edn's own format). A line
  whose annotation block contains \"external: true\" contributes
  nothing -- external stages are exempt."
  [line]
  (if (re-find #"\{[^}]*external:\s*true" line)
    []
    (when-let [m (re-find #"catalytic:\s*([^;}]+)" line)]
      (mapv str/trim (str/split (second m) #",")))))

(defn- use-cases-catalytic-resources
  [use-cases-edn-path]
  (let [{:keys [cases]} (edn/read-string (slurp use-cases-edn-path))]
    (set (mapcat (fn [{:keys [equations]}] (mapcat line-catalytic-resources equations)) cases))))

;; ---- lint ----

(defn lint
  "Returns {:ok? bool :violations [{:resource :issue :note} ...]}.
  :issue is :unclassified (no entry in catalytic-resource-targets) or
  :unresolved (classified, but verification failed)."
  ([] (lint {}))
  ([{:keys [pipeline-edn use-cases-edn]
     :or {pipeline-edn "docs/pipeline.edn" use-cases-edn "docs/use-cases.edn"}}]
   (let [resources (set/union (pipeline-catalytic-resources pipeline-edn)
                               (use-cases-catalytic-resources use-cases-edn))
         violations (reduce
                     (fn [acc resource]
                       (if-let [classification (get catalytic-resource-targets resource)]
                         (let [{:keys [ok? note]} (verify-classification classification)]
                           (if ok? acc (conj acc {:resource resource :issue :unresolved :note note})))
                         (conj acc {:resource resource :issue :unclassified
                                    :note "no declared target classification in ehr-testing-tools.lint/catalytic-resource-targets"})))
                     []
                     (sort resources))]
     {:ok? (empty? violations) :violations violations})))

(defn lint-pipeline!
  "-X-invokable: runs lint over the real docs/pipeline.edn and
  docs/use-cases.edn, prints a summary, and exits non-zero on any
  violation -- `make lint-pipeline` fails the build the way any other
  lint would."
  [_]
  (let [{:keys [ok? violations]} (lint)]
    (if ok?
      (println "lint-pipeline: OK -- every catalytic resource resolves to one of the four targets")
      (do (println "lint-pipeline: FAILED")
          (doseq [{:keys [resource issue note]} violations]
            (println (str "  " resource " -- " issue " -- " note)))
          (System/exit 1)))))
