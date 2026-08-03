(ns ehrt.sim-emit-hl7.vendored-uti-test
  "Post-Wave-D cleanup session (2026-08-02, ADR-0030 J3): the full
  compile-trajectory/engine/emit round trip for `urinary_tract_
  infections.json` -- the same standing gap
  `ehrt.sim-emit-hl7.vendored-ear-infections-test`'s own docstring
  documents in full, confirmed here against this closure too rather
  than assumed to generalize. UTI's own mandatory Care Pathways state
  (`type_of_care_transition`, D5) selects one of Telemedicine/
  Ambulatory/ED, each a `CallSubmodule` into its own path file -- so
  EVERY real onset walks straight into the same missing-registry throw
  `ear_infections.json` hits, not merely a possible branch. This was
  also the specific check Wave B deferred (ADR-0027's own D6 finding:
  'a `type_of_care_transition` path taken, cross-boundary Encounter/
  EncounterEnd citations inside a called path submodule' -- proven at
  the INTERPRETER layer by
  `ehrt.sim-trajectory.vendored-uti-test`'s own
  `a-walk-reaches-a-care-pathway-and-carries-cross-boundary-encounter-
  events`, still never proven through the engine until this file tried
  it for real).

  Tests only, per J3: this round trip does not work today; this file
  PINS the confirmed failure (same mechanism as ear_infections' own
  file — see that file's docstring for the full engine.clj citation),
  not a fix. UTI's own lookup-table entry path
  (`lookup_table_transition`, H2) is a SECOND engine-closure gap this
  session found live: `engine.clj`'s 5-arity `run-module` call also
  never threads a `tables` map through (`ehrt.sim-trajectory.interface/
  run-module`'s own 7-arity default, `{}`), so even a walk that never
  reaches a CallSubmodule would still be unable to resolve
  `uti.csv`/`uti_recurrence.csv` correctly through the engine -- moot
  here since the CallSubmodule throw fires first on the very same
  entry path, but named for whichever session eventually wires this."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.sim-trajectory.gmf :as gmf]
            [ehrt.sim.engine :as engine]))

(def ^:private uti-json (slurp (io/resource "sim/modules/urinary_tract_infections.json")))

(def ^:private uti-module
  (:payload (gmf/load-module "urinary-tract-infections" uti-json)))

;; Small population, small horizon -- a round-trip proof, not a soak
;; test (J3). `Wait_for_UTI`'s own self-looping Delay needs a long
;; horizon to sweep through candidate onsets (the interpreter-layer
;; vendored test's own 100-year horizon), but the throw fires on the
;; FIRST onset any patient reaches, so this population/horizon is
;; already enough -- confirmed empirically this session.
(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [uti-module] :module-assignment [{:module-id "urinary-tract-infections" :weight 1}]
   :module-horizon-days 36500})

(deftest engine-run-throws-on-uti-callsubmodule-content
  (testing "PINS the confirmed engine gap (ADR-0030 J3), same mechanism
            ear_infections' own file documents -- expected to start
            FAILING once a future session wires engine.clj to carry a
            closure's modules/tables maps; update this test then, not
            leave it silently red."
    (let [ex (try (engine/run run-config) (catch clojure.lang.ExceptionInfo e e))]
      (is (instance? clojure.lang.ExceptionInfo ex)
          "expected engine/run to throw for this closure, not complete")
      (is (re-find #"CallSubmodule names a call-path missing from the resolved closure"
                   (ex-message ex)))
      (is (= "urinary-tract-infections" (:caller (ex-data ex))))
      (is (contains? #{"uti/telemed_path" "uti/ambulatory_path" "uti/ed_path"}
                     (:call-path (ex-data ex)))))))
