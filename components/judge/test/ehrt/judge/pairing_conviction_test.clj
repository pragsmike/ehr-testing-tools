(ns ehrt.judge.pairing-conviction-test
  "Tier one of the pairing registry's own two consumer tiers (AR-PD-4,
  ADR-0088): the inject-X-expect-X loop, closed by execution. For
  EVERY row `ehrt.judge.pairing/load-registry` returns: load its
  `:fixture`, apply its `:operator` at its `:locator` via
  `ehrt.corpus.interface/mutate`, gate the mutant through its own
  `:judge`, assert at least one of its `:expected` classes appears
  among the gate's own findings. A row that stops witnessing this
  (an engine upgrade changes its finding vocabulary, a fixture moves)
  fails HERE, not silently.

  Test context crossing into `corpus`, `judge-v2-hapi`, and
  `judge-v2-nist` from `judge`'s own test tree is deliberate and
  precedented (`ehrt.judge-v2-nist.v2-engine-test` already crosses into
  `judge` the same way, ADR-0012) -- none of these three gain a new
  `deps.edn` edge from this file; `judge` itself stays free of them
  too (`ehrt.judge.pairing` never requires any of the three)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as kernel]
            [ehrt.judge.interface :as judge]
            [ehrt.corpus.interface :as corpus]
            [ehrt.judge-v2-hapi.interface :as v2-hapi]
            [ehrt.judge-v2-nist.interface :as v2-nist]))

(def ^:private work-dir "target/pairing-conviction")

(defn- mutant-content
  [{:keys [fixture locator] {:keys [id]} :operator}]
  (let [base (slurp (io/file fixture))
        operator (corpus/operator-lookup id "1")
        mutate-result (corpus/mutate base operator {:format :v2 :path locator})]
    (when-not (kernel/ok? mutate-result)
      (throw (ex-info "pairing-conviction: mutate failed" (assoc mutate-result :row fixture))))
    (:mutant (:payload mutate-result))))

(defn- write-mutant!
  [{:keys [judge] {:keys [id]} :operator} content]
  (let [path (str work-dir "/" (name judge) "-" (name id) "-mutant.hl7")]
    (io/make-parents path)
    (spit path content)
    path))

(defmulti ^:private gate-mutant (fn [row _path] (:judge row)))

(defmethod gate-mutant :judge-v2-hapi
  [_row path]
  (let [r (v2-hapi/gate-file path)]
    (when-not (kernel/ok? r) (throw (ex-info "pairing-conviction: hapi gate failed" r)))
    (->> (:findings (:payload r)) (map :code) set)))

(defmethod gate-mutant :judge-v2-nist
  [{:keys [profile]} path]
  (let [validator-state (v2-nist/make-validator profile)
        r (v2-nist/gate-file validator-state path)]
    (when-not (kernel/ok? r) (throw (ex-info "pairing-conviction: nist gate failed" r)))
    (->> (:findings (:payload r)) (map (comp :category :native-ref)) set)))

(deftest every-registry-row-witnesses-its-own-expected-class-test
  (doseq [{:keys [operator judge expected] :as row} (judge/load-pairing-registry)]
    (testing (str (:id operator) " x " judge)
      (let [path (write-mutant! row (mutant-content row))
            observed (gate-mutant row path)]
        (is (some expected observed)
            (str "expected one of " expected " among observed classes " observed))))))
