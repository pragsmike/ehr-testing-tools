(ns ehrt.corpus.sim-adapter-test
  "Hermetic: run! is tested against an injected :run-command-fn, never
  a real simulation (ADR-0005, carve-loss recovery -- this namespace no
  longer subprocesses or discovers a sibling checkout at all, so there
  is no discovery order left to test the way the pre-mount version of
  this suite did). :run-command-fn rides the same single opts map as
  every other value here -- the convention
  ehrt.corpus.generate/generate!'s own :run-invocation already
  uses, not a separate injection argument."
  (:require [clojure.test :refer [deftest is]]
            [ehrt.kernel.interface :as kernel]
            [ehrt.corpus.sim-adapter :as sim]))

(deftest run-delegates-to-run-command-fn-test
  (let [captured (atom nil)
        fake (fn [opts] (reset! captured opts) (kernel/ok {:manifest {:stage :simulated}}))
        r (sim/run! {:seed 42 :patients 3 :run-command-fn fake})]
    (is (kernel/ok? r))
    (is (= {:stage :simulated} (:manifest (:payload r))))
    (is (= {:seed 42 :patients 3} @captured)
        "run-command-fn itself is stripped out before delegating, same as :out-dir")))

(deftest run-strips-out-dir-before-delegating-test
  (let [captured (atom nil)
        fake (fn [opts] (reset! captured opts) (kernel/ok {}))]
    (sim/run! {:seed 42 :out-dir "target/sim-harness" :run-command-fn fake})
    (is (= {:seed 42} @captured)
        "the old subprocess stdout/stderr log location never reaches run-command")))

;; The legacy sibling-checkout discovery keys (:sim-dir,
;; :env-sim-dir-fn, :default-dir) retired 2026-08-05 (scaffolding
;; compaction A, AR-A-3) -- ADR-0012's own in-process mount
;; (2026-07-28) already made sibling-checkout discovery dead code,
;; and a fresh grep this session found zero callers still passing
;; them. Nothing left to test: an unrecognized key is now ordinary
;; opts-map noise, same as any other unknown key, not a case this
;; namespace special-cases.

(deftest run-passes-through-rejected-and-error-unchanged-test
  (let [rejected-fake (fn [_] (kernel/rejected :incompatible-assignment {:conflicts []}))
        error-fake (fn [_] (kernel/error :missing-required-opt {:opt :seed}))]
    (is (= :incompatible-assignment (:category (sim/run! {:run-command-fn rejected-fake}))))
    (is (= :missing-required-opt (:category (sim/run! {:run-command-fn error-fake}))))))

(deftest run-default-calls-the-real-run-command-test
  ;; The ONE non-hermetic case, deliberately: proves the default
  ;; (no :run-command-fn given) actually wires to the real
  ;; ehrt.sim.interface/run-command, not just to itself -- a real, fast,
  ;; deterministic 1-patient run (same fixed seed convention as
  ;; sim_manifest_contract_test.clj's own smallest-known-fast
  ;; invocation) rather than mocking the one seam meant to prove the
  ;; real wiring works.
  (let [r (sim/run! {:seed 100 :patients 1})]
    (is (kernel/ok? r))
    (is (map? (:manifest (:payload r))))))

;; ---- check!/identifiers!/version! (P3-6 parity mount, 2026-08-01):
;; same -fn injection convention as run!'s own :run-command-fn. ----

(deftest check-delegates-to-check-all-fn-test
  (let [captured (atom nil)
        fake (fn [ground-truth] (reset! captured ground-truth) (kernel/ok {}))]
    (sim/check! [{:event :admission}] {:check-all-fn fake})
    (is (= [{:event :admission}] @captured))))

(deftest check-default-calls-the-real-check-all-test
  (let [bad [{:event :discharge :t 0 :participants [{:patient-id "P1" :role :subject}]}
             {:event :admission :t 5 :participants [{:patient-id "P1" :role :subject}] :location "Renal"}]
        r (sim/check! bad)]
    (is (kernel/rejected? r))
    (is (= :invariant-violation (:category r)))))

(deftest identifiers-delegates-to-identifiers-fn-and-strips-injection-key-test
  (let [captured (atom nil)
        fake (fn [opts] (reset! captured opts) (kernel/ok {:run-id "1"}))
        r (sim/identifiers! {:seed 1 :patients 2 :identifiers-fn fake})]
    (is (kernel/ok? r))
    (is (= {:seed 1 :patients 2} @captured)
        "identifiers-fn itself is stripped out before delegating, same as run!'s own :run-command-fn")))

(deftest identifiers-default-calls-the-real-identifiers-command-test
  (let [r (sim/identifiers! {:seed 100 :patients 1})]
    (is (kernel/ok? r))
    (is (contains? (:payload r) :patient-ids))))

(deftest version-delegates-to-git-sha-fn-test
  (let [r (sim/version! {:git-sha-fn (constantly "abc123")})]
    (is (string? (:version r)))
    (is (= "abc123" (:git-sha r)))))

(deftest version-default-calls-the-real-version-and-git-sha-test
  (let [r (sim/version!)]
    (is (string? (:version r)))
    (is (contains? r :git-sha))))
