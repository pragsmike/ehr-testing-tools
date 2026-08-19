(ns ehrt.docs-tooling.exercised-sources
  "The exercised-sources register (ADR-0129): schema and loader for
  `components/docs-tooling/resources/docs-tooling/exercised-sources.edn`,
  one row per {doc-taught-strip, exercising bin/ script} pair this
  workspace actually re-runs against the live tree -- the mechanism
  dimension 1 of the manual-review skill's own first run
  (`.agents/plans/2026-08-13-manual-review-1.md`) found missing for a
  `docs/use-cases/*.md` page or README's own \"What you get\" fence.
  Follows `ehrt.judge.pairing`'s own load-registry shape (a
  malli-validated EDN resource, thrown ex-info on missing/invalid) --
  the same registry-as-data pattern, a different domain."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [malli.core :as m]))

(def ExtractionKind
  "How a row's own :source is walked for taught command lines --
  :quickstart-fresh/:demo-exerciser-fresh delegate verbatim to the
  pre-existing, independently tested namespaces of the same name (the
  two rows seeded before this register existed); :single-fence and
  :paired are ehrt.docs-tooling.strip-fresh's own new extraction shapes
  (see that namespace's docstring)."
  [:enum :quickstart-fresh :demo-exerciser-fresh :single-fence :paired])

(def ExercisedSource
  [:map
   [:source [:string {:min 1}]]
   [:script [:string {:min 1}]]
   [:extraction ExtractionKind]
   [:marker-open [:string {:min 1}]]
   [:marker-close [:string {:min 1}]]
   [:fence-lang {:optional true} [:string {:min 1}]]
   ;; :fence-index selects WHICH fence of :fence-lang a :single-fence row
   ;; extracts, 0-based, defaulting to 0 -- the behaviour every row
   ;; before ADR-0158 had and keeps. Added because SETUP.md's runnable
   ;; verification ladder is its SECOND ```sh fence (the first installs
   ;; system packages and is exempt, never exercised), and the honest way
   ;; to reach it is to say so rather than to re-tag the fence's language
   ;; until the first-match rule happens to land on it.
   [:fence-index {:optional true} [:int {:min 0}]]
   [:section {:optional true} [:string {:min 1}]]
   [:env [:map-of :string :string]]
   [:witness [:map [:adr :string] [:date :string]]]])

(def Registry
  [:vector ExercisedSource])

(def ^:private registry-resource "docs-tooling/exercised-sources.edn")

(defn load-registry
  "Loads and schema-validates the committed exercised-sources registry
  (a resource on the classpath). Throws ex-info if the resource is
  missing or fails validation -- a malformed or missing registry is a
  build-time defect, not an operational condition this fn routes
  through a kernel/error result."
  []
  (let [res (io/resource registry-resource)]
    (when-not res
      (throw (ex-info "exercised-sources registry resource not found" {:resource registry-resource})))
    (let [rows (edn/read-string (slurp res))]
      (when-not (m/validate Registry rows)
        (throw (ex-info "exercised-sources registry failed schema validation"
                         {:resource registry-resource
                          :explain (m/explain Registry rows)})))
      rows)))

(defn by-source
  "Every row in `rows` whose own :source equals `source-path` -- a
  citation gate lookup key (README.md and the ed-tuesday README each
  own more than one row, one per script that exercises them)."
  [rows source-path]
  (filterv #(= source-path (:source %)) rows))
