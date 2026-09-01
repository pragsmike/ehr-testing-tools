(ns ehrt.sim-emit-hl7.vendored-uti-test
  "Engine closure-context session (2026-08-03, ADR-0033, J3 CLOSED): the
  full compile-trajectory/engine/emit round trip for `urinary_tract_
  infections.json` -- the same standing gap
  `ehrt.sim-emit-hl7.vendored-ear-infections-test`'s own docstring
  documents in full, closed here against this closure too rather than
  assumed to generalize. UTI's own mandatory Care Pathways state
  (`type_of_care_transition`, D5) selects one of Telemedicine/
  Ambulatory/ED, each a `CallSubmodule` into its own path file, so
  every real onset used to walk straight into the missing-registry
  throw this file's own previous version pinned (ADR-0030 J3); UTI's
  own lookup-table entry path (`lookup_table_transition`, H2) was a
  SECOND engine-closure gap the same pinning session found live --
  `decide.clj`'s bare 5-arity `run-module` call never threaded a
  `tables` map through either. This session's own AR-2/AR-3 close
  BOTH: the closure's real `:modules` AND `:tables` maps now reach
  `run-module`'s full arity.

  Dated note (2026-08-04, ADR-0042 AR-4, Wave H pre-roll -- closes the
  ADR-0033/0034 straddle linkage below, RETIRING seed 777): this
  closure's own mandatory Care Pathways Encounter reliably straddles
  `decide.clj`'s own fixed registration-t anchor for most seeds (opens
  in history, closes in horizon) -- a real, disclosed gap in the LEGACY
  (`:history` absent) path, where a straddling encounter's own opening
  drops as a pre-horizon fact but its closing compiles for real, an
  orphaned `:encounter-end`/`:discharge` that `check/check-all`'s own
  `:clinical-content-only-when-admitted` invariant trips. Seed 777 was
  chosen empirically as one that happens not to trigger it. ADR-0042
  AR-2 resolves the straddle at the ROOT instead: with `:history true`,
  an event's phase is inherited from its own encounter's OPENING phase,
  so a straddling encounter's own close drops together with its own
  open -- no orphan, ever, for any seed. This file now runs with
  `:history true` and an ORDINARY seed (this project's own `20260802`
  config default, the same value most other vendored-closure oracle
  batches already use) to prove exactly that: the straddle resolves by
  design, not by seed-picking."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.gmf :as gmf]
            [ehrt.sim-engine.run :as run]
            [ehrt.sim-check.check :as check]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]))

(def ^:private uti-json (slurp (io/resource "sim/modules/urinary_tract_infections.json")))

(defn- resolve-call-path
  "D3's own real caller shape -- a thin io/resource wrapper over the
  search path `sim/modules/<call-path>.json`."
  [call-path]
  (some-> (io/resource (str "sim/modules/" call-path ".json")) slurp))

(defn- resolve-table-name
  "D3a/H2's own real caller shape -- a thin io/resource wrapper over
  `sim/modules/lookup_tables/<table-name>`."
  [table-name]
  (some-> (io/resource (str "sim/modules/lookup_tables/" table-name)) slurp))

(def ^:private loaded-closure
  (gmf/load-closure "urinary-tract-infections" uti-json resolve-call-path resolve-table-name))
(def ^:private uti-closure (:payload loaded-closure))

;; Small population, small horizon -- a round-trip proof, not a soak
;; test (J3). `Wait_for_UTI`'s own self-looping Delay needs a long
;; horizon to sweep through candidate onsets (the interpreter-layer
;; vendored test's own 100-year horizon); the throw used to fire on the
;; FIRST onset any patient reached, so this population/horizon was
;; already enough to pin the failure and stays enough to prove the fix.
;;
;; ADR-0042 AR-4: `:history true`, an ORDINARY seed (this project's own
;; `20260802` config default) -- the straddle this closure's own
;; mandatory Care Pathways Encounter reliably hits (this namespace's
;; own docstring, above) now resolves by design (AR-2's encounter-
;; anchored inheritance), not by seed-picking.
(def ^:private run-config
  {:seed 20260802 :patients 300 :pathway {:name "module-only" :steps []}
   :modules [uti-closure] :module-assignment [{:module-id "urinary-tract-infections" :weight 1}]
   :module-horizon-days 36500 :history true})

(deftest engine-run-completes-real-uti-closure-content
  (testing "load-clean sanity -- root plus all eleven called submodules AND both lookup tables"
    (is (result/ok? loaded-closure)))
  (testing "ADR-0033 (J3 closed): engine/run no longer throws on this
            closure's own CallSubmodule care-pathway content, and the
            lookup-table entry path (H2) resolves correctly through the
            engine too -- real compiled clinical content lands"
    (let [{:keys [ground-truth] :as result} (run/run run-config)
          kinds (into #{} (map :event) ground-truth)]
      (is (some #{:condition-onset :encounter :encounter-end :medication-order} kinds)
          (str "expected real compiled clinical content across 300 patients, got " kinds))
      (testing "ADR-0042 AR-2/AR-4: the straddle this closure's own Care
                Pathways Encounter reliably hits resolves by design under
                :history true -- the full invariant catalog holds for an
                ORDINARY seed, no hand-picked dodge needed"
        (is (result/ok? (check/check-all ground-truth (:facility result)))
            "the full invariant catalog holds for a real closure-driven run"))
      (testing "content emitted inside a called care-pathway submodule renders real HL7"
        (let [messages (emit-hl7/emit ground-truth "2024-01-01" "+00:00" (:facility result) (:providers result))]
          (is (seq messages) "expected at least one HL7 message rendered from real clinical content"))))))
