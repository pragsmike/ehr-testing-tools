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

  Also gates tier two (storefront-fixture session, ADR-0091, AR-SD-3):
  `every-catalog-operator-has-at-least-one-witnessed-row-test` promotes
  `ehrt.judge.pairing/coverage` from ADR-0088's own report-only
  computation to a live gate against `ehrt.corpus.interface/operator-
  entries`'s own catalog -- every operator must have at least one
  witnessed row, any judge; a judge-specific skip does not count
  against an operator witnessed elsewhere.

  Test context crossing into `corpus`, `judge-v2-hapi`, `judge-v2-nist`,
  and (storefront-fixture session, ADR-0091, AR-SD-2) `judge-fhir-
  official` from `judge`'s own test tree is deliberate and precedented
  (`ehrt.judge-v2-nist.v2-engine-test` already crosses into `judge` the
  same way, ADR-0012) -- none of these four gain a new `deps.edn` edge
  from this file; `judge` itself stays free of them too
  (`ehrt.judge.pairing` never requires any of the four). Every project
  composing `judge` already carries `judge-fhir-official` on its own
  classpath (ADR-0011), so this crossing needed no new project-level
  edge either -- confirmed by grep across `projects/*/deps.edn` before
  this row landed."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as kernel]
            [ehrt.judge.interface :as judge]
            [ehrt.corpus.interface :as corpus]
            [ehrt.judge-v2-hapi.interface :as v2-hapi]
            [ehrt.judge-v2-nist.interface :as v2-nist]
            [ehrt.judge-fhir-official.interface :as v2-fhir]))

(def ^:private work-dir "target/pairing-conviction")

(def ^:private lockfile-artifacts
  (delay (:artifacts (:payload (kernel/read-lockfile "artifacts.lock.edn")))))

(defn- mutant-content
  "Returns {:format :content}: :content is corpus.mutate's own :mutant
  payload, still format-dependent (a raw ER7 string for :v2, plain
  parsed Clojure data for :fhir -- corpus.mutate's own docstring) --
  `write-mutant!` below is what turns it back into file bytes."
  [{:keys [fixture locator] {:keys [id]} :operator}]
  (let [operator (corpus/operator-lookup id "1")
        format (:format operator)
        base (case format
               :fhir (json/read-str (slurp (io/file fixture)))
               :v2 (slurp (io/file fixture)))
        mutate-result (corpus/mutate base operator {:format format :path locator})]
    (when-not (kernel/ok? mutate-result)
      (throw (ex-info "pairing-conviction: mutate failed" (assoc mutate-result :row fixture))))
    {:format format :content (:mutant (:payload mutate-result))}))

(defn- write-mutant!
  [{:keys [judge] {:keys [id]} :operator} {:keys [format content]}]
  (let [ext (case format :fhir "json" :v2 "hl7")
        path (str work-dir "/" (name judge) "-" (name id) "-mutant." ext)
        text (case format :fhir (json/write-str content) :v2 content)]
    (io/make-parents path)
    (spit path text)
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

(defmethod gate-mutant :judge-fhir-official
  [_row path]
  (let [r (v2-fhir/gate-file path {:artifacts @lockfile-artifacts :out-dir work-dir})]
    (when-not (kernel/ok? r) (throw (ex-info "pairing-conviction: fhir gate failed" r)))
    (->> (:findings (:payload r)) (map :code) set)))

(deftest every-registry-row-witnesses-its-own-expected-class-test
  (doseq [{:keys [operator judge expected] :as row} (judge/load-pairing-registry)]
    (testing (str (:id operator) " x " judge)
      (let [path (write-mutant! row (mutant-content row))
            observed (gate-mutant row path)]
        (is (some expected observed)
            (str "expected one of " expected " among observed classes " observed))))))

(deftest every-catalog-operator-has-at-least-one-witnessed-row-test
  ;; Tier two, PROMOTED from report-only to gating (storefront-fixture
  ;; session, ADR-0091, AR-SD-3): every operator in corpus's live
  ;; catalog must have at least one witnessed registry row, against
  ;; ANY judge -- a judge-specific skipped cell (the three NIST skips
  ;; named in ADR-0088) does not count against an operator that has a
  ;; witnessed row elsewhere. Fails naming exactly which operator ids
  ;; have zero witnessed rows, rather than a bare boolean.
  ;; operator-entries reads the same shared, global, mutable registry
  ;; atom ehrt.corpus.operators-test's own registry-mechanics tests
  ;; register throwaway entries into (:test-op/:e1/:no-doc-op,
  ;; deliberately :doc-less -- that suite's own comment, operators_
  ;; test.clj:133-138). `poly test :all` runs every namespace in one
  ;; JVM process, so those entries are visible here too, depending on
  ;; run order -- filtering to entries carrying :doc is the SAME
  ;; distinguishing signal that suite already relies on, not a new
  ;; convention invented here.
  (let [rows (judge/load-pairing-registry)
        operator-ids (->> (corpus/operator-entries) (filter :doc) (map :id) distinct)
        coverage (judge/pairing-coverage rows operator-ids)
        uncovered (into (sorted-set) (keep (fn [[id judges]] (when (empty? judges) id))) coverage)]
    (is (empty? uncovered)
        (str "operators with zero witnessed registry rows (any judge): " uncovered))))
