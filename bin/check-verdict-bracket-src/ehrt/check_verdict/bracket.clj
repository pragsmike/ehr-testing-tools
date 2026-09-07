(ns ehrt.check-verdict.bracket
  "THE VERDICT BRACKET: does `check-all` return the SAME answer when it
  is HANDED a record projection as when it folds one for itself?

  WHY `bin/ground-truth-bracket` CANNOT ASK THIS. ADR-0180 site 7 moves
  no ground-truth byte by construction -- it changes only how the
  checker gets its records -- so the ground-truth bracket reports
  IDENTICAL before the session starts and would report IDENTICAL if
  every invariant in the catalog had been silently disabled. Its
  verdict is necessary here and says almost nothing. The obligation
  ruling R-verdict-bracket states is a different one: \"check-all's
  violation vector, computed via the handed projection and via a fresh
  replay, is equal -- element for element, in order -- over all 38
  oracle roots and the three cells; the instrument has its own zero
  before any repoint.\"

  THE POPULATION IS THREE-PART, and each part answers a different half
  of that obligation:

  * ROOTS -- every `ehrt.oracle.digest` root that carries a
    `:ground-truth` key (38 of 41 at the time of writing; the three
    interpreter-layer batch roots write a vector of walks and are
    SKIPPED BY NAME, exactly as `bin/ground-truth-bracket` skips them).
    Handed = an `engine/replay` this instrument computes and passes in;
    replayed = the public `check-all` path. Both are replays of the
    same log, so this half gates the THREADING -- that an invariant
    handed records reaches the same verdict as one that replays -- and
    not the population question below.

  * THE NEGATIVE CONTROL -- the same comparison over a DELIBERATELY
    BROKEN log, so the vectors being compared are NOT EMPTY. Every log
    in the roots and cells populations is one this checker passes, so
    without a control the whole instrument would be comparing `[]` with
    `[]` and reporting IDENTICAL over a population that contains none
    of the thing under test (`rulings.md#R-empty-population-is-red`,
    the shape `bin/ground-truth-bracket`'s own verdict function already
    refuses). The control REVERSES the largest root's log, which puts
    every discharge before its admission and every reference before its
    referent; a control whose violation vector comes back EMPTY is a
    STOP, not a pass.

    IT WAS `(rest log)` FIRST, and this instrument's own first run
    STOPPED on it: dropping the head event of `allergic-rhinitis.edn`
    convicts NOTHING, because these roots walk a module rather than an
    authored pathway and their first event strands no reference. The
    refusal is the guard working, and the perturbation was made
    stronger rather than the guard weaker.

  * CELLS -- the measurement cells, run through
    `ehrt.sim.interface/run-command` (whose own in-run self-check is
    the HANDED side once site 7's step 4 lands) and then through
    `ehrt.sim.interface/check-command` over the log it emitted (the
    STANDALONE side). Both are the real user-facing paths -- `ehrt sim
    run` and `ehrt sim check --config` -- so this half needs no second
    path of its own to express, and it is the half that answers the
    population divergence: `run`'s entries carry every patient from
    t 0, `replay`'s carry only patients seen so far.

  THE INSTRUMENT'S OWN ZERO. Before site 7's step 3 lands there is no
  handed arity to call, so both ways ARE the public path and every
  comparison is trivially IDENTICAL. That is not a defect of the run
  and it is not silently passed off as a result either: the banner says
  `handed arity: ABSENT` and the verdict line says the run was the
  instrument's own zero."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ehrt.oracle.interface :as oracle]
            [ehrt.sim.interface :as sim]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim-model.interface :as sim-model])
  (:import (java.security MessageDigest)))

;; --- digesting -------------------------------------------------------------

(defn- sha256
  "The digest of `s`, hex, lower case -- the same shape
  `bin/ground-truth-bracket`'s manifest prints, so the two instruments'
  output reads the same way."
  [^String s]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (->> (.digest md (.getBytes s "UTF-8"))
         (map #(format "%02x" %))
         (apply str))))

(defn- violations-of
  "The violation vector of a `check-all` Result: `[]` when it is `:ok`,
  its `:violations` when it is `:rejected`, and a one-element vector
  naming the failure for anything else -- an `:error` is a finding, and
  averaging it into an empty vector would report it as a pass."
  [result]
  (cond
    (= :ok (:status result)) []
    (= :rejected (:status result)) (vec (:violations (:payload result)))
    :else [{:instrument/unexpected-result-status (:status result)
            :instrument/category (:category result)}]))

(defn- vsig
  "One violation vector's signature: its count and its digest. The
  digest is over `pr-str` of the WHOLE vector, so equal signatures mean
  equal element for element AND in order, which is what
  R-verdict-bracket asks for."
  [violations]
  {:n (count violations) :sha (sha256 (pr-str violations))})

;; --- the handed arity, resolved rather than assumed -------------------------

(def ^:private handed-arity
  "Whether `ehrt.sim-check.check/check-all` accepts a records
  projection as a fifth argument. Resolved from `:arglists` rather than
  caught from an exception, so a tip that has no such arity is reported
  as the instrument's own zero rather than as a failure."
  (boolean (some #(= 5 (count %)) (:arglists (meta #'check/check-all)))))

(defn- check-handed
  "`check-all` over `ground-truth` with `records` HANDED to it, at the
  defaults every root here is checked under. Falls back to the public
  path on a tip with no handed arity -- see the ns docstring."
  [ground-truth records]
  (if handed-arity
    ((resolve 'ehrt.sim-check.check/check-all)
     ground-truth sim-model/default-facility 0 engine/default-profiles records)
    (check/check-all ground-truth)))

(defn- check-plain
  "`check-all` over `ground-truth` through the public 1-arity -- the
  path a caller with no records of its own takes."
  [ground-truth]
  (check/check-all ground-truth))

;; --- the roots half ---------------------------------------------------------

(defn- root-logs
  "Every `<root>.edn` the oracle wrote into `dir`, as
  `[name ground-truth-or-nil]` sorted by name. A root with no
  `:ground-truth` key is returned with nil and SKIPPED BY NAME by the
  caller, the same disposition `bin/ground-truth-bracket` gives the
  three interpreter-layer batch roots."
  [dir]
  (->> (.listFiles (io/file dir))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".edn"))
       (sort-by #(.getName ^java.io.File %))
       (mapv (fn [^java.io.File f]
               (let [m (edn/read-string (slurp f))]
                 [(.getName f) (when (map? m) (:ground-truth m))])))))

(defn- compare-log
  "One log, both ways. Returns a report row."
  [kind nm ground-truth]
  (let [handed (vsig (violations-of (check-handed ground-truth (engine/replay ground-truth))))
        plain (vsig (violations-of (check-plain ground-truth)))]
    {:kind kind :name nm :handed handed :replayed plain
     :same (= handed plain)}))

;; --- the cells half ---------------------------------------------------------

(defn- compare-cell
  "One measurement cell, both ways: `run-command`'s own IN-RUN
  self-check against `check-command` over the log that run emitted.

  A run that comes back `:self-check-failed` still yields a violation
  vector -- that is the in-run verdict, and comparing it is the whole
  point -- but it carries no `:ground-truth`, so the standalone side
  has no log to check and the row is reported as a STOP rather than as
  a difference."
  [{:keys [label config patients seed]}]
  (let [ran (sim/run-command {:seed seed :patients patients :churn true :config config})
        in-run (cond
                 (= :ok (:status ran)) []
                 (= :self-check-failed (:category ran)) (vec (:violations (:payload ran)))
                 :else [{:instrument/run-command-failed (:category ran)}])
        gt (:ground-truth (:payload ran))]
    (if (nil? gt)
      {:kind "cell" :name label :handed (vsig in-run) :replayed nil :same false
       :stop (str "run-command returned no :ground-truth (" (:category ran) ")")}
      (let [standalone (vsig (violations-of (sim/check-command gt {:config config})))]
        {:kind "cell" :name label :handed (vsig in-run) :replayed standalone
         :events (count gt)
         :same (= (vsig in-run) standalone)}))))

;; --- reporting --------------------------------------------------------------

(defn- print-row [{:keys [kind name handed replayed same stop events]}]
  (println (format "%-8s %-52s handed=%s/%d %s=%s/%d %s%s"
                   kind name
                   (subs (:sha handed) 0 16) (:n handed)
                   (if (= kind "cell") "standalone" "replayed")
                   (if replayed (subs (:sha replayed) 0 16) "-")
                   (if replayed (:n replayed) -1)
                   (if same "SAME" "DIFFERS")
                   (cond stop (str " -- STOP: " stop)
                         events (str " -- " events " events")
                         :else ""))))

(def ^:private cells
  "The three measurement cells, by the same labels, configs, seed and
  arrival counts `run-cells.sh` uses -- read from that driver rather
  than re-chosen here."
  [{:label "a2500-nopersons" :patients 2500}
   {:label "a7500-persons" :patients 7500}
   {:label "a22500-nopersons" :patients 22500}])

(defn -main
  "Usage: bracket <oracle-out-dir> [cell-config-dir] [cell-label ...]

  With no cell labels every cell above runs. `--roots-only` runs the
  roots and the control alone, which is the cheap form."
  [& args]
  (let [[roots-dir cfg-dir & rest-args] args
        roots-only? (some #{"--roots-only"} rest-args)
        wanted (set (remove #(str/starts-with? % "--") rest-args))
        logs (root-logs roots-dir)
        skipped (mapv first (filter (comp nil? second) logs))
        present (filterv second logs)]
    (println "== check-verdict-bracket: check-all's violation vector, handed vs replayed ==")
    (println (format "handed arity: %s" (if handed-arity "PRESENT" "ABSENT -- both ways are the public path; this run is the instrument's own zero")))
    (println (format "roots: %d carry :ground-truth and are compared; %d skipped (no such key): %s"
                     (count present) (count skipped)
                     (if (seq skipped) (str/join ", " skipped) "none")))
    (println)
    (when (zero? (count present))
      (println "STOP: no root carried a :ground-truth key -- this bracket covered an EMPTY population")
      (System/exit 1))
    (let [root-rows (mapv (fn [[nm gt]] (compare-log "root" nm gt)) present)
          ;; THE NEGATIVE CONTROL, over the LARGEST root reversed --
          ;; see the ns docstring for why it is not the head-drop this
          ;; started as. Largest, because a bigger log reaches more of
          ;; the catalog and the control's whole job is to be non-empty.
          [ctl-name ctl-gt] (apply max-key (comp count second) present)
          ctl (compare-log "control" (str ctl-name " (log REVERSED)") (vec (reverse ctl-gt)))
          cell-rows (if roots-only?
                      []
                      (mapv (fn [c]
                              (compare-cell (assoc c :seed 20260824
                                                   :config (str cfg-dir "/cell-" (:label c) ".edn"))))
                            (if (seq wanted) (filterv #(wanted (:label %)) cells) cells)))]
      (doseq [r root-rows] (print-row r))
      (print-row ctl)
      (doseq [r cell-rows] (print-row r))
      (println)
      (let [rows (concat root-rows [ctl] cell-rows)
            differing (remove :same rows)
            control-empty? (zero? (:n (:handed ctl)))]
        (when control-empty?
          (println "STOP: the negative control's violation vector is EMPTY -- the comparison is vacuous")
          (System/exit 1))
        (println (format "control: %d violations on a broken log, both ways -- the comparison is not vacuous"
                         (:n (:handed ctl))))
        (if (seq differing)
          (do (println (format "DIFFERS: %d of %d rows disagree -- STOP, escalate"
                               (count differing) (count rows)))
              (doseq [r differing] (println "  " (:kind r) (:name r) (or (:stop r) "")))
              (System/exit 1))
          (do (println (format "IDENTICAL: check-all's violation vector agrees both ways on all %d rows%s"
                               (count rows)
                               (if handed-arity "" " (the instrument's own zero -- no handed arity at this tip)")))
              (System/exit 0)))))))
