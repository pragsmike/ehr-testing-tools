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
  reaches, produce a single result that crosses a MERGE?
  `.agents/plans/2026-09-01-event-mutation-population-ledger.md`
  recorded that the gated corpora contain zero of those, so the bracket
  that would catch a defect there is blind, and the cells this script
  runs over are the first population large enough to say whether the
  gap is a scale artefact or a structural one.

  CORRECTION, 2026-09-06 (ADR-0180 session). The first cut asked that
  question with the WRONG PREDICATE -- a result whose own subject is
  `:merged` -- and got zero on all eight cells. ADR-0179's R-queue
  makes that predicate structurally unsatisfiable rather than merely
  rare: a pending result FOLLOWS the survivor, so the subject a result
  lands on is by construction the one that did not disappear, and a
  log in which it were `:merged` is one R-queue forbids. The scenario
  the corpus actually contains, and the one R-inv is written for, is a
  result whose CITED ORDER names a different subject, joined to the
  result's own subject by a `:merge` at `:t` at or before the result's
  `:t`. That is what `:result-after-merge` counts below.

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

(defn- merges-forward
  "The `:merge` relation over the whole log, absorbed patient-id ->
  `{:survivor .. :t ..}`. First merge wins, which matters only for a
  hand-authored log that breaks `decide :merge`'s own already-merged
  guard.

  DERIVED HERE RATHER THAN CALLED OUT OF `check.clj`, deliberately and
  for this namespace's own stated reason: `merges-forward` and
  `resolves-through-merges?` are private there, and an instrument that
  borrowed the checker's implementation could only ever confirm the
  checker agrees with itself. This is R-inv's condition read off
  ADR-0179's own wording, and if it and the checker ever disagree that
  disagreement is the finding."
  [ground-truth]
  (reduce (fn [m ev]
            (if (= :merge (:event ev))
              (let [by-role (fn [r] (some #(when (= r (:role %)) (:patient-id %))
                                          (:participants ev)))
                    absorbed (by-role :merged)]
                (if (or (nil? absorbed) (contains? m absorbed))
                  m
                  (assoc m absorbed {:survivor (by-role :survivor) :t (:t ev)})))
              m))
          {} ground-truth))

(defn- resolves-through-merges?
  "ADR-0179 R-inv: `from` IS `to`, or was merged into it by a `:merge`
  at `:t` at or before `at`.

  R-inv is worded for ONE HOP. This walks the chain, which strictly
  contains the one-hop case: a survivor is itself mergeable, and when
  it is merged away the run loop carries the same follow-up on again.
  `seen` terminates a hand-authored cycle; the engine cannot write one."
  [merges from to at]
  (loop [pid from seen #{}]
    (cond
      (= pid to) true
      (seen pid) false
      :else (let [{:keys [survivor t]} (get merges pid)]
              (if (and survivor (<= t at))
                (recur survivor (conj seen pid))
                false)))))

(defn- crosses-a-merge?
  "R-inv's condition, as a predicate over one `:result-available`
  event: the order it cites names a subject OTHER than the result's,
  and that subject resolves to the result's subject through a merge no
  later than the result's own `:t`.

  `indexed` is the log as a vector because `:order-event-id` is an
  INDEX into it -- the same addressing `check.clj`'s referential row
  uses, and the reason this predicate needs the log and not just
  `replay`'s entries."
  [merges indexed event]
  (let [subject (:patient-id (first (:participants event)))
        target (get indexed (:order-event-id event))]
    (boolean
     (and target
          (= :order-placed (:event target))
          (some (fn [{:keys [patient-id]}]
                  (and patient-id
                       (not= patient-id subject)
                       (resolves-through-merges? merges patient-id subject (:t event))))
                (:participants target))))))

(defn census
  "Folds `replay`'s entries into one counts map. Every column's
  definition is stated where it is counted, because the interesting
  columns are the ones whose obvious definition is the wrong one.

  Takes the LOG as well as `replay`'s entries: `:result-after-merge` is
  a referential question (`:order-event-id` is an index into the log),
  and no per-entry projection can answer it."
  [entries ground-truth]
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
        indexed (vec ground-truth)
        merges (merges-forward ground-truth)
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
                   ;;
                   ;; THE MERGE ARM IS REFERENTIAL, NOT A STATUS TEST
                   ;; (corrected 2026-09-06; the ns docstring carries
                   ;; the why). `(= st :merged)` counted a result
                   ;; landing ON an absorbed record, which R-queue
                   ;; makes impossible; R-inv's own condition is a
                   ;; result whose CITED ORDER names a different
                   ;; subject, joined through a merge.
                   (= kind :result-available)
                   (let [st (:status before)
                         merged? (crosses-a-merge? merges indexed event)]
                     (cond-> (update acc :results inc)
                       merged? (update :result-after-merge inc)
                       (and (not merged?)
                            (terminal-statuses st))
                       (update :result-after-discharge inc)
                       (and (not merged?)
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
        counts (census (engine/replay gt) gt)
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
