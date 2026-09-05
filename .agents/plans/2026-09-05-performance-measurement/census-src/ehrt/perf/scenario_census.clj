(ns ehrt.perf.scenario-census
  "Counts the SCENARIOS a measurement cell actually contains -- not how
  long it took, which is `run-cells.sh`'s question, but whether the
  corpus reaches the situations the invariant catalog and the mutation
  operators are written against.

  WHY THIS IS NOT A TEST. Its population is the CORPUS, not the schema,
  which is the same population-closure law `bin/event-census` and
  `bin/fence-census` run under -- an instrument that decided what to
  count by reading `check.clj` could only ever confirm the checker
  agrees with itself. This one asks a question the checker does not:
  does a 10^5-event corpus, generated at a scale nothing in `demos/`
  reaches, produce a single result landing after its own subject has
  been MERGED? `.agents/plans/2026-09-01-event-mutation-population-
  ledger.md` recorded that the gated corpora contain zero of those, so
  the bracket that would catch a defect there is blind, and the cells
  this script runs over are the first population large enough to say
  whether the gap is a scale artefact or a structural one.

  PURE OVER THE LOG. Nothing here draws, samples or reads a clock: the
  same log yields the same counts, which is what makes a count quotable
  as evidence rather than as an observation.

  Reads `ehrt.sim-engine.interface/replay`'s projection -- the same
  {:event :before :after :world-before :world-after} entries
  `check.clj`'s invariants read (fold.clj's `replay` docstring, APPLY
  SITE 2), so a scenario counted here is a scenario the catalog could
  in principle have an invariant over."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [ehrt.sim-engine.interface :as engine]))

(defn- read-edn [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (edn/read r)))

(defn- ward-of [pstate] (get-in pstate [:location :ward]))

(defn- role-participant [ev role]
  (some #(when (= role (:role %)) (:patient-id %)) (:participants ev)))

(def ^:private terminal-statuses
  "A subject in one of these holds no bed and is not coming back --
  `evolve`'s own :discharge/:death arms set them (evolve.clj)."
  #{:discharged :expired})

(defn census
  "Folds `replay`'s entries into one counts map. Every column's
  definition is stated where it is counted, because the interesting
  columns are the ones whose obvious definition is the wrong one."
  [entries]
  (let [init {:events 0
              :results 0
              :result-after-move 0
              :result-after-discharge 0
              :result-after-merge 0
              :merges 0
              :merge-absorbs-bed-holder 0
              :cancels 0
              :cancel-reinstates-bed 0
              :cancel-moves-bed 0
              :live {}
              :peak {}}
        out (reduce
             (fn [acc {:keys [event before after world-before world-after]}]
               (let [kind (:event event)
                     ;; WARD CENSUS, folded incrementally rather than
                     ;; recounted. A per-event recount over the whole
                     ;; patient map is the O(N x P) shape ADR-0169 named
                     ;; as 54.9% of the check phase; here only the
                     ;; event's own participants can have moved, so the
                     ;; delta is O(participants) and the answer is the
                     ;; same one. Bed participants carry `:bed-id` and
                     ;; no `:patient-id` and are skipped -- a nil key
                     ;; would put a phantom in the census.
                     live (reduce
                           (fn [m {:keys [patient-id]}]
                             (if patient-id
                               (let [w0 (ward-of (get world-before patient-id))
                                     w1 (ward-of (get world-after patient-id))]
                                 (if (= w0 w1)
                                   m
                                   (cond-> m
                                     w0 (update w0 (fnil dec 0))
                                     w1 (update w1 (fnil inc 0)))))
                               m))
                           (:live acc) (:participants event))
                     acc (-> acc
                             (update :events inc)
                             (assoc :live live)
                             (assoc :peak (merge-with max (:peak acc) live)))]
                 (cond
                   ;; A RESULT'S `:location` IS STAMPED AT ORDER TIME,
                   ;; not at landing time -- decide.clj says so in its
                   ;; own words at the `result-event` binding ("the
                   ;; patient's state AT ORDER TIME"). So the stamped
                   ;; location against the subject's state as the result
                   ;; LANDS is exactly "what moved during the
                   ;; turnaround", and the three arms below are the
                   ;; three ways it can have moved. They are ordered
                   ;; most-specific first: a result landing after a
                   ;; merge ALSO has a stale location, and counting it
                   ;; twice would inflate the cheap column with the
                   ;; interesting one.
                   (= kind :result-available)
                   (let [st (:status before)]
                     (cond-> (update acc :results inc)
                       (= st :merged) (update :result-after-merge inc)
                       (terminal-statuses st) (update :result-after-discharge inc)
                       (and (not= st :merged)
                            (not (terminal-statuses st))
                            (not= (:location event) (:location before)))
                       (update :result-after-move inc)))

                   ;; A MERGE ABSORBING A BED-HOLDER. Read off the
                   ;; `:merged` participant and not the primary one:
                   ;; `replay`'s `:before` is the FIRST participant's
                   ;; state, and a merge's first participant is not
                   ;; necessarily the side that disappears. `evolve`'s
                   ;; `:merged` arm dissocs `:location`, so a non-nil
                   ;; location BEFORE the merge is a bed that the merge
                   ;; itself is responsible for releasing.
                   (= kind :merge)
                   (cond-> (update acc :merges inc)
                     (some? (:location (get world-before (role-participant event :merged))))
                     (update :merge-absorbs-bed-holder inc))

                   ;; A CANCEL REINSTATING A BED is the narrow case --
                   ;; the subject held no bed and now holds one, which
                   ;; is what `cancel-discharge` does. `cancel-transfer`
                   ;; usually MOVES an already-held bed instead, so the
                   ;; two are counted apart rather than summed: they are
                   ;; different obligations on the allocator.
                   (#{:cancel-discharge :cancel-transfer :cancel-admit} kind)
                   (let [b (:location before) a (:location after)]
                     (cond-> (update acc :cancels inc)
                       (and (nil? b) (some? a)) (update :cancel-reinstates-bed inc)
                       (and (some? b) (some? a) (not= a b)) (update :cancel-moves-bed inc)))

                   :else acc)))
             init entries)]
    (dissoc out :live)))

(defn -main [& [log-path config-path]]
  (when-not log-path
    (binding [*out* *err*]
      (println "usage: scenario-census <ground-truth.edn> [config.edn]"))
    (System/exit 2))
  (let [gt (read-edn log-path)
        counts (census (engine/replay gt))
        caps (when config-path
               (into {} (for [w (get-in (read-edn config-path) [:facility :wards])]
                          [(:name w) (+ (:beds w) (:surge-slots w))])))]
    (pp/pprint (cond-> (assoc counts :log log-path)
                 caps (assoc :capacity caps)
                 caps (assoc :peak-vs-capacity
                             (into {} (for [[ward pk] (:peak counts)]
                                        [ward {:peak pk :capacity (get caps ward)
                                               :pct (when-let [c (get caps ward)]
                                                      (Double/parseDouble
                                                       (format "%.1f" (* 100.0 (/ (double pk) c)))))}])))))))
