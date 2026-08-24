(ns ehrt.patient-simulator.emittable-events-test
  "The P1 gate (ADR-0165): `ehrt.patient-simulator.emittable-events/
  state-type->emittable` must not drift from
  `ehrt.patient-simulator.gmf-interpreter/step`'s own dispatch.

  Reads `gmf_interpreter.clj` with the Clojure reader (`*read-eval*`
  false), never a regex over raw text -- the discipline
  `ehrt.docs-tooling.sim-purity-lint-test` and
  `ehrt.cli.cli-parse-guard-lint-test` already establish. Two
  divergences fail here:

  1. `step`'s own `case` grows (or loses) a state type and the table
     does not -- the INVARIANT this gate exists for: a new state type
     added to the interpreter with no table row is a test failure, not
     a silently-uncovered event type.
  2. the interpreter emits a trajectory event type the table's
     `:trajectory-event` column does not name, or vice versa."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.patient-simulator.emittable-events :as emittable]))

(def ^:private interpreter-source
  "components/patient-simulator/src/ehrt/patient_simulator/gmf_interpreter.clj")

(defn- read-all-forms
  "Every top-level form in the file at `path`, via the Clojure reader
  (`*read-eval*` false)."
  [path]
  (with-open [rdr (java.io.PushbackReader. (io/reader path))]
    (binding [*read-eval* false]
      (loop [forms []]
        (let [f (read {:eof ::eof} rdr)]
          (if (= f ::eof) forms (recur (conj forms f))))))))

(defn- find-form
  "Depth-first: the first sub-form of `node` (itself included) matching
  `pred`, or nil."
  [pred node]
  (cond
    (pred node) node
    (coll? node) (some #(find-form pred %) node)
    :else nil))

(defn- step-defn [forms]
  (find-form #(and (seq? %) (= 'defn (first %)) (= 'step (second %))) forms))

(defn- step-dispatch-case
  "`step`'s own `(case (:type state) ...)` form."
  [forms]
  (find-form #(and (seq? %) (= 'case (first %)) (= '(:type state) (second %)))
             (step-defn forms)))

(defn- case-dispatch-constants
  "Every constant `case-form` dispatches on. A `case` clause key is
  either one constant or a LIST of them (`step`'s own
  `(:multi-observation :diagnostic-report)` pair); a trailing odd
  element is the default clause and carries no constants."
  [case-form]
  (let [clauses (drop 2 case-form)
        pairs (if (odd? (count clauses)) (butlast clauses) clauses)]
    (into #{}
          (mapcat (fn [k] (if (seq? k) k [k])))
          (take-nth 2 pairs))))

(defn- emit-site-keyword
  "The trajectory event type an emit site names, or nil for anything
  else: `emit-and-advance`'s 5th argument and `trajectory-event`'s 3rd.
  Kept only where that argument is a literal keyword --
  `emit-and-advance`'s own body forwards the SYMBOL `event-type` to
  `trajectory-event`, which is a pass-through, not an emit site."
  [node]
  (when (seq? node)
    (let [arg (case (first node)
                emit-and-advance (nth node 5 nil)
                trajectory-event (nth node 3 nil)
                nil)]
      (when (keyword? arg) arg))))

(defn- emitted-event-types
  "Every trajectory event type the interpreter's own emit sites name."
  [forms]
  (into #{} (keep emit-site-keyword) (tree-seq coll? seq forms)))

(deftest table-covers-every-state-type-the-interpreter-dispatches-on
  (testing "the INVARIANT: a state type added to `step`'s own case with
            no row in `state-type->emittable` fails HERE, so a new
            state type can never reach a vendored module without its
            emittable event types being declared"
    (let [forms (read-all-forms interpreter-source)
          dispatched (case-dispatch-constants (step-dispatch-case forms))]
      (is (seq dispatched) "the dispatch walk found no state types at all -- the walker itself is broken")
      (is (= dispatched (set (keys emittable/state-type->emittable)))
          (str "state types in `step` but not in the table: "
               (pr-str (sort (remove (set (keys emittable/state-type->emittable)) dispatched)))
               "; in the table but not in `step`: "
               (pr-str (sort (remove dispatched (keys emittable/state-type->emittable)))))))))

(deftest table-names-every-trajectory-event-type-the-interpreter-emits
  (testing "the `:trajectory-event` column, gated against the
            interpreter's own emit sites rather than against a reading
            of them"
    (let [forms (read-all-forms interpreter-source)
          emitted (emitted-event-types forms)
          declared (into #{} (keep :trajectory-event) (vals emittable/state-type->emittable))]
      (is (seq emitted) "the emit-site walk found nothing -- the walker itself is broken")
      (is (= emitted declared)
          (str "emitted by the interpreter but not declared: " (pr-str (sort (remove declared emitted)))
               "; declared but never emitted: " (pr-str (sort (remove emitted declared))))))))

(deftest ground-truth-column-names-only-real-log-event-kinds
  (testing "every `:ground-truth` entry is a kind
            `ehrt.sim-engine.event-schema/Event` actually carries --
            asserted against a literal list here rather than by
            depending on sim-engine, which patient-simulator sits
            BELOW in the brick graph"
    (let [log-kinds #{:registered :admission :transfer :discharge :cancel-admit
                      :cancel-transfer :cancel-discharge :bed-swap :merge
                      :order-placed :result-available :outpatient-visit
                      :outpatient-visit-end :procedure :observation
                      :diagnostic-report :medication-order :medication-end
                      :care-plan-start :care-plan-end :step-rejected}
          declared (into #{} (mapcat :ground-truth) (vals emittable/state-type->emittable))]
      (is (empty? (remove log-kinds declared))
          (str "not a ground-truth event kind: " (pr-str (sort (remove log-kinds declared))))))))
