(ns ehrt.corpus.check.schemas
  "Malli-schema registry for Check's :schema assertion kind (P6) --
  named, versioned schemas referenced by {id, version}: the same
  in-repo code-registry catalytic target (docs/notation.md's target 4)
  ehrt.kernel.canonical and ehrt.corpus.operators
  already use, same registry shape (register!/lookup/entries/snapshot/
  reset), same reason it's not a shared top-level registry namespace --
  this catalog has exactly one consumer (ehrt.corpus.check) so
  far, and a generic cross-namespace registry abstraction would be
  premature until a second, non-Check catalog actually needs it (the
  same call corpus.operators' own docstring makes).

  Ships one seed entry (:fhir-resource-shape) so the :schema assertion
  kind has something real to validate against out of the box; callers
  register their own project-specific schemas the same way
  corpus.canonicalizers registers concrete canonicalizer entries."
  (:require [malli.core :as m]
            [ehrt.kernel.interface :as kernel]))

(def Entry
  [:map
   [:id :keyword]
   [:version :string]
   [:schema :any]
   [:docstring :string]])

(defonce ^:private registry (atom {}))

(defn register!
  "Registers a schema entry, keyed by [id version]. Returns kernel/ok
  {:id :version} or kernel/rejected :invalid-entry."
  [entry]
  (if (m/validate Entry entry)
    (do (swap! registry assoc [(:id entry) (:version entry)] entry)
        (kernel/ok (select-keys entry [:id :version])))
    (kernel/rejected :invalid-entry {:entry entry})))

(defn lookup
  [id version]
  (get @registry [id version]))

(defn entries
  []
  (vals @registry))

(defn registry-snapshot
  "Test/dev support: the full registry map, keyed by [id version] --
  for saving and later restoring exact state, same convention as
  ehrt.kernel.canonical and ehrt.corpus.operators."
  []
  @registry)

(defn reset-registry!
  ([] (reset-registry! {}))
  ([snapshot] (reset! registry snapshot)))

(defn valid-against?
  "True if datum validates against entry's own :schema."
  [entry datum]
  (m/validate (:schema entry) datum))

;; ---- seed catalog ----

(register!
 {:id :fhir-resource-shape :version "1"
  :schema [:map ["resourceType" :string]]
  :docstring "The minimal FHIR-JSON shape: a top-level \"resourceType\" string key. Deliberately loose -- this is a corpus sanity check (does every file look like a FHIR resource at all), not a conformance gate (that's judge.fhir's job)."})
