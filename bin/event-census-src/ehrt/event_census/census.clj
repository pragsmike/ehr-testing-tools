(ns ehrt.event-census.census
  "Tabulates the ground-truth event log's own shape from EMITTED EDN --
  the population is the corpora, never a schema (the same
  population-closure discipline `bin/fence-census` runs under, and the
  reason it is a separate instrument from the schema it is used to
  check).

  Landed by the event-log contract arc (Step 1 evidence,
  `.agents/plans/2026-08-16-event-log-census.md`), promoted from that
  file's own appendix to `bin/` by author-licensed fence widening
  (2026-08-16) so the census stays re-derivable rather than being a
  one-session probe whose numbers age silently.

  Deliberately depends on nothing but `clojure.core` + `clojure.edn`:
  it must be able to read an events.edn produced by a DIFFERENT
  version of this repo -- including one whose schema this checkout
  would reject -- because 'does the emitted log still match the
  committed contract?' is exactly the question it exists to answer,
  and an instrument that loads the contract to read the data cannot
  ask it.

  Reads either shape `sim run` emits: the bare ground-truth vector
  (`--format ground-truth`, and `corpus generate sim`'s own
  events.edn) or the full envelope map (whose :ground-truth is taken).
  A kernel error envelope ({:status :error ...}) is skipped with a
  note rather than counted as an empty run -- ADR-0140's own
  distinguish-empty-from-error lesson, applied to this reader."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]))

;; --- shape description ----------------------------------------------------

(defn shape
  "A compact, stable description of one value's shape. Structural
  only: a map renders as its own key set, never its values, so the
  output is a contract description and not a data dump (no persona,
  no MRN, no patient-id ever reaches stdout through this function --
  with the one exception `shape*` below exists to close)."
  [v]
  (cond
    (nil? v) "nil"
    (keyword? v) "keyword"
    (string? v) "string"
    (boolean? v) "boolean"
    (integer? v) "long"
    (float? v) "double"
    (inst? v) "inst"
    (set? v) (str "set<" (str/join "|" (sort (distinct (map shape v)))) ">")
    (vector? v) (if (empty? v)
                  "vector<>"
                  (str "vector<" (str/join "|" (sort (distinct (map shape v)))) ">"))
    (sequential? v) (str "seq<" (str/join "|" (sort (distinct (map shape v)))) ">")
    (map? v) (str "map{" (str/join "," (sort (map #(if (keyword? %) (name %) (str %)) (keys v)))) "}")
    :else (str (type v))))

(defn shape*
  "`shape`, plus the one case where a map's KEYS are data rather than
  a fixed schema: :bed-swap's own :swap is keyed by patient-id.
  Rendering that through `shape` would print two synthetic patient
  ids per event into the census output; collapsing it keeps the
  census structural."
  [v]
  (if (and (map? v) (seq v) (every? string? (keys v)))
    (str "map{<patient-id> -> " (str/join "|" (sort (distinct (map shape (vals v))))) "}")
    (shape v)))

;; --- tabulation -----------------------------------------------------------

(defn census
  "events -> {kind {:n count :keys {key {:present n :nils n :shapes
  [...]}}}}. `:present` counts events carrying the key AT ALL;
  `:nils` counts, of those, how many carry an explicit nil -- the
  distinction that makes a present-but-always-nil key (a real shape
  defect class, this census's own S-1/S-2) visible instead of reading
  as a populated field."
  [events]
  (into (sorted-map)
        (for [[kind evs] (group-by :event events)]
          (let [n (count evs)
                all-keys (reduce into #{} (map keys evs))
                rows (for [k all-keys]
                       (let [present (filter #(contains? % k) evs)
                             nils (count (filter #(nil? (get % k)) present))
                             shapes (sort (distinct (map #(shape* (get % k))
                                                         (remove #(nil? (get % k)) present))))]
                         [k {:present (count present) :nils nils :shapes shapes}]))]
            [kind {:n n :keys (into (sorted-map) rows)}]))))

(defn universal-keys
  "The keys present on EVERY event of EVERY kind -- the common-key set
  a schema can factor. Computed, never assumed: this is how the arc
  found that :active-mrn is not one of them."
  [c]
  (reduce (fn [acc [_ {:keys [n] :as m}]]
            (into #{} (filter (fn [k] (and (acc k) (= n (:present (get (:keys m) k))))) acc)))
          (set (keys (:keys (val (first c)))))
          c))

;; --- reading --------------------------------------------------------------

(defn read-log
  "path -> [events note], where `note` is nil on success and a human
  string when the file held something other than a log (an error
  envelope, an empty file). Never throws on a well-formed non-log."
  [path]
  (let [raw (edn/read-string (slurp path))]
    (cond
      (and (map? raw) (= :error (:status raw))) [nil (str "error envelope, category "
                                                          (pr-str (:category raw)))]
      ;; `sim run` with no --format wraps the envelope in a kernel
      ;; result ({:status :ok :payload {...}}); --format ground-truth
      ;; emits the bare vector; `corpus generate sim` writes the bare
      ;; vector to events.edn. All three are read here, so a user does
      ;; not have to know which shape they happen to have.
      (and (map? raw) (contains? raw :ground-truth)) [(:ground-truth raw) nil]
      (and (map? raw) (contains? (:payload raw) :ground-truth)) [(get-in raw [:payload :ground-truth]) nil]
      (map? raw) [nil "a map that is neither a run envelope nor an error envelope"]
      (nil? raw) [nil "empty file"]
      (sequential? raw) [(vec raw) nil]
      :else [nil (str "not a log: " (shape raw))])))

;; --- reporting ------------------------------------------------------------

(defn- report-kind
  [kind {:keys [n] :as m}]
  (println (format "### `%s` (n=%d)\n" kind n))
  (println "| key | present | nil | value shape |")
  (println "|---|---|---|---|")
  (doseq [[k {:keys [present nils shapes]}] (:keys m)]
    (println (format "| `%s` | %s | %s | %s |"
                     k
                     (if (= present n) (str n " (always)") (format "%d/%d" present n))
                     (if (zero? nils) "-" (str nils))
                     (str/join " \\| " (map #(str "`" % "`") shapes)))))
  (println))

(defn -main
  "Usage: bin/event-census <events.edn>... -- markdown to stdout."
  [& paths]
  (when (empty? paths)
    (println "usage: bin/event-census <events.edn>...")
    (System/exit 2))
  (let [read-all (for [p paths] (into [p] (read-log p)))
        good (filter (fn [[_ evs _]] (seq evs)) read-all)
        all (vec (mapcat second good))]
    (println "## Corpora\n")
    (doseq [[p evs note] read-all]
      (println (if note
                 (format "- `%s` -- SKIPPED (%s)" p note)
                 (format "- `%s` -- %d events" p (count evs)))))
    (when (empty? all)
      (println "\nNo events read -- nothing to tabulate.")
      (System/exit 1))
    (let [c (census all)]
      (println (format "\nTotal: **%d events**, **%d kinds**.\n" (count all) (count c)))
      (println "## Vocabulary\n")
      (doseq [k (keys c)] (println (format "- `%s`" k)))
      (println "\n## Per-kind key population\n")
      (doseq [[kind m] c] (report-kind kind m))
      (println "## Universal keys (present on every event of every kind)\n")
      (doseq [k (sort (universal-keys c))] (println (format "- `%s`" k)))
      (println "\n## Nested `:event` vocabularies (NOT log events)\n")
      (println (format "- `:conditions` entries: %s"
                       (or (seq (sort (distinct (mapcat (fn [e] (map :event (:conditions e))) all))))
                           "none observed")))
      (println (format "- `:pre-horizon-facts` entries: %s"
                       (or (seq (sort (distinct (mapcat (fn [e] (map :event (:pre-horizon-facts e))) all))))
                           "none observed")))
      (println "\n## Participant roles\n")
      (println (format "- %s" (sort (distinct (mapcat (fn [e] (map :role (:participants e))) all)))))
      (println "\n## Per-corpus kind counts\n")
      (doseq [[p evs _] good]
        (println (format "- `%s`: %s" p
                         (str/join ", " (for [[k v] (sort (frequencies (map :event evs)))]
                                          (format "%s %d" k v))))))
      (println "\n## Per-corpus `:t` monotonicity (a RUN-level property)\n")
      (doseq [[p evs _] good]
        (println (format "- `%s`: `(apply <= (map :t ...))` = **%s**" p (apply <= (map :t evs))))))))
