(ns ehrt.judge.report
  "Corpus-level aggregation for CI consumers (P5): normalizes per-file
  judge results (either format -- judge.fhir and judge.v2 both produce the
  same {:path :verdict :findings [...]} shape) into one report:
  {:run :totals :by-code :files}. EDN is canonical (ADR-0004); the CLI's
  --json flag is a projection over this same data, not a separate code
  path. `diff-reports` makes the CI-diffability promise real: what
  changed between two runs of the same corpus."
  (:require [clojure.set :as set]
            [malli.core :as m]
            [ehrt.judge.finding :as finding]))

(def Totals
  [:map [:pass :int] [:rejected :int] [:indeterminate :int] [:no-verdict :int]])

(def FileEntry
  [:map
   [:path :string]
   [:verdict finding/Verdict]
   [:finding-count :int]
   [:findings {:optional true} [:vector finding/Finding]]
   [:id {:optional true} :string]
   ;; :cause (ADR-0010): present iff :verdict is :no-verdict -- carried
   ;; through from whichever result produced this file's overall
   ;; verdict (judge.fhir/interpret). No :fn-refinement pairing here
   ;; the way judge.finding/VerdictOutcome enforces it for a single
   ;; finding's :disposition -- a FileEntry's own :verdict/:cause pair
   ;; is populated by build-report itself (below), not hand-authored.
   [:cause {:optional true} finding/Cause]
   ;; :no-verdict-causes (post-close-out retrofit to R3): a per-cause
   ;; count over every finding in this file that carries a :cause --
   ;; present regardless of which verdict the file-level projection
   ;; picked. worst-of's fold lets a :rejected finding dominate the
   ;; aggregate over an incidental no-verdict-worthy finding in the
   ;; SAME file (the revised ranking, ADR-0010) -- that's the coverage
   ;; dimension the single :verdict keyword necessarily discards; this
   ;; field is how a :rejected file still surfaces its own partiality
   ;; instead of losing it entirely to the projection. Optional/absent
   ;; when no finding in the file carries a :cause -- additive, so
   ;; pre-existing reports and pre-ADR-0010 baselines are unaffected.
   [:no-verdict-causes {:optional true} [:map-of finding/Cause :int]]])

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
  "results is a seq of per-file judge outcomes {:path :verdict :findings
  [...] :id (optional) :cause (optional, iff :verdict is :no-verdict --
  ADR-0010)}. run is free-form metadata about this run (which gate,
  which path/corpus was gated, etc.) -- carried through verbatim as
  :run. Each FileEntry retains its full :findings (not just
  :finding-count) -- P6 needs this so a persisted report can later
  serve as a --baseline for baseline-relative-report below;
  :finding-count stays too, for a quick scan that doesn't need to walk
  :findings itself. Each FileEntry's :no-verdict-causes is computed
  here, not passed in: a per-cause count over every finding carrying a
  :cause, present regardless of which verdict worst-of's projection
  picked for the file -- the coverage dimension that projection
  otherwise discards when :rejected wins over an incidental
  no-verdict-worthy finding in the same file."
  [results run]
  (let [totals (reduce (fn [acc {:keys [verdict]}] (update acc verdict inc))
                        {:pass 0 :rejected 0 :indeterminate 0 :no-verdict 0}
                        results)
        by-code (reduce (fn [acc {:keys [findings]}]
                          (reduce (fn [acc2 f] (update acc2 (:code f) (fnil inc 0))) acc findings))
                        {}
                        results)
        files (mapv (fn [{:keys [path verdict findings id cause]}]
                      (let [causes (reduce (fn [acc f]
                                             (if-let [c (:cause f)]
                                               (update acc c (fnil inc 0))
                                               acc))
                                           {} findings)]
                        (cond-> {:path path :verdict verdict :finding-count (count findings) :findings (vec findings)}
                          id (assoc :id id)
                          cause (assoc :cause cause)
                          (seq causes) (assoc :no-verdict-causes causes))))
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

;; ---- baseline-relative verdicts (P6): motivated by EXP-C5's
;; discovery that a profile-stamped corpus carries pre-existing
;; findings on every file, so a file-level verdict alone can't
;; discriminate a genuinely new problem from baseline noise -- see
;; docs/judge-calibration.md for the full motivation and the exact-
;; match limitation this deliberately accepts. ----

(defn- finding-key
  "The {severity, code, locator-path} triple baseline matching keys
  on -- deliberately excludes :message and :native-ref, which can
  legitimately vary run to run for the *same* underlying finding
  (e.g. differing diagnostic text), and deliberately excludes any
  format-specific extension field (e.g. judge.fhir's own :disposition
  and :cause, ADR-0010) so this stays format-agnostic, matching
  build-report's own contract."
  [f]
  [(:severity f) (:code f) (get-in f [:locator :path])])

(defn- baseline-finding-keys
  "The set of finding-key triples already present in baseline's
  FileEntry for path -- empty if the file isn't in the baseline at
  all, or the baseline predates :findings (an older report.edn with
  only :finding-count, gracefully treated as \"nothing known\", not an
  error)."
  [baseline path]
  (let [entry (first (filter #(= path (:path %)) (:files baseline)))]
    (set (map finding-key (:findings entry)))))

(defn- relative-file-result
  "Recomputes one file's result relative to baseline: a finding counts
  toward rejection only if its finding-key triple is not already
  present in baseline for that file. Verdict is binary (:pass or
  :rejected) even against a four-valued judge's own absolute verdict
  (ADR-0010: :pass/:rejected/:indeterminate/:no-verdict) -- judge.report
  stays format-agnostic and has no access to any format-specific
  per-finding classification (e.g. judge.fhir's own :disposition and
  :cause) that would be needed to preserve :no-verdict here; a novel
  no-verdict-worthy finding still counts as :rejected in relative mode,
  same as it did for :indeterminate before this arm existed. Stated
  plainly (docs/judge-calibration.md), not left implicit. Baselines
  captured before ADR-0010 (three-valued, no :cause) read forward
  unmigrated -- finding-key never looks at :cause, so an old baseline's
  absence of the field is simply absence, not a mismatch."
  [{:keys [path findings id]} baseline]
  (let [known (baseline-finding-keys baseline path)
        novel (vec (remove #(contains? known (finding-key %)) findings))]
    (cond-> {:path path :verdict (if (seq novel) :rejected :pass) :findings novel}
      id (assoc :id id))))

(defn baseline-relative-report
  "Builds a baseline-relative report from results (the same
  {:path :verdict :findings [...] :id (optional)} shape build-report
  consumes) and baseline (a previously-produced Report, e.g. read back
  from a --report file of an earlier run). Returns {:absolute <Report>
  :relative <Report>} -- :absolute is exactly build-report's own
  output over results, unchanged; :relative recomputes each file's
  verdict against baseline first. Exit-code decisions belong to the
  caller (the CLI): the relative verdict is what --baseline mode
  promises to gate on, but the absolute findings are never hidden."
  [results run baseline]
  (let [absolute (build-report results run)
        relative-results (map #(relative-file-result % baseline) results)]
    {:absolute absolute :relative (build-report relative-results run)}))
