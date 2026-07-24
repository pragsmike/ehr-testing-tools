(ns ehr-testing-tools.gate.report
  "Corpus-level aggregation for CI consumers (P5): normalizes per-file
  gate results (either format -- gate.fhir and gate.v2 both produce the
  same {:path :verdict :findings [...]} shape) into one report:
  {:run :totals :by-code :files}. EDN is canonical (ADR-0004); the CLI's
  --json flag is a projection over this same data, not a separate code
  path. `diff-reports` makes the CI-diffability promise real: what
  changed between two runs of the same corpus."
  (:require [clojure.set :as set]
            [malli.core :as m]
            [ehr-testing-tools.gate.finding :as finding]))

(def Totals
  [:map [:pass :int] [:rejected :int] [:indeterminate :int]])

(def FileEntry
  [:map
   [:path :string]
   [:verdict finding/Verdict]
   [:finding-count :int]
   [:id {:optional true} :string]])

(def Report
  [:map
   [:run :map]
   [:totals Totals]
   [:by-code [:map-of :string :int]]
   [:files [:vector FileEntry]]])

(defn valid?
  [report]
  (m/validate Report report))

(defn build-report
  "results is a seq of per-file gate outcomes {:path :verdict :findings
  [...] :id (optional)}. run is free-form metadata about this run
  (which gate, which path/corpus was gated, etc.) -- carried through
  verbatim as :run."
  [results run]
  (let [totals (reduce (fn [acc {:keys [verdict]}] (update acc verdict inc))
                        {:pass 0 :rejected 0 :indeterminate 0}
                        results)
        by-code (reduce (fn [acc {:keys [findings]}]
                          (reduce (fn [acc2 f] (update acc2 (:code f) (fnil inc 0))) acc findings))
                        {}
                        results)
        files (mapv (fn [{:keys [path verdict findings id]}]
                      (cond-> {:path path :verdict verdict :finding-count (count findings)}
                        id (assoc :id id)))
                    results)]
    {:run run :totals totals :by-code by-code :files files}))

;; ---- diff-reports: the CI-diffability promise made real ----

(defn diff-reports
  "Compares two corpus reports (e.g. before/after a pipeline change):
  which files' verdict changed, which files were added/removed, and
  which finding codes appeared/disappeared between the two runs.
  Matches files by :path (the stable join key both reports share).
  Returns {:changed-verdicts [{:path :from :to} ...] :files-added
  [...] :files-removed [...] :codes-appeared [...]
  :codes-disappeared [...]} -- every list sorted/deterministic."
  [a b]
  (let [a-files (into {} (map (juxt :path identity)) (:files a))
        b-files (into {} (map (juxt :path identity)) (:files b))
        changed (for [[path bf] (sort-by key b-files)
                      :let [af (get a-files path)]
                      :when (and af (not= (:verdict af) (:verdict bf)))]
                  {:path path :from (:verdict af) :to (:verdict bf)})
        added (sort (remove #(contains? a-files %) (keys b-files)))
        removed (sort (remove #(contains? b-files %) (keys a-files)))
        a-codes (set (keys (:by-code a)))
        b-codes (set (keys (:by-code b)))]
    {:changed-verdicts (vec changed)
     :files-added (vec added)
     :files-removed (vec removed)
     :codes-appeared (vec (sort (set/difference b-codes a-codes)))
     :codes-disappeared (vec (sort (set/difference a-codes b-codes)))}))
