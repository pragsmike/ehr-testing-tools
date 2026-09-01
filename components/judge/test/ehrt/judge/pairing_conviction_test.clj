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

  FHIR rows (`:judge-fhir-official`) are deliberately EXCLUDED from
  this file's own tier-one loop (storefront-fixture session, ADR-0091,
  AR-SD-2, a correction mid-session): `judge-fhir-official/gate-file`
  needs the real `fhir-validator-cli` artifact fetched first
  (`ehr artifact fetch`), and this file lives in `judge`'s own test
  tree, composed by EVERY project including `conformance`/`ehrt-cli`
  -- whose ordinary push-triggered CI lane never primes the artifact
  cache (only the `integration` project's own scheduled/workflow_
  dispatch lane does, AGENTS.md's hermetic-test-suite rule). A first
  attempt put the FHIR arm here and passed locally (this session's own
  artifact cache was already warm from manual measurement runs) but
  failed in CI's fresh environment -- `:not-cached`, `fhir-validator-
  cli`. `projects/integration/test/ehrt/integration/pairing_
  conviction_fhir_test.clj` witnesses the FHIR rows instead, the same
  placement `contract_pairing_test.clj`/`baseline_gating_test.clj`
  already use for the same reason.

  Also gates tier two (storefront-fixture session, ADR-0091, AR-SD-3):
  `every-catalog-operator-has-at-least-one-witnessed-row-test` promotes
  `ehrt.judge.pairing/coverage` from ADR-0088's own report-only
  computation to a live gate against `ehrt.corpus.interface/operator-
  entries`'s own catalog -- every operator must have at least one
  witnessed row, any judge; a judge-specific skip does not count
  against an operator witnessed elsewhere. This tier stays here
  (artifact-independent -- it only reads the registry and the live
  catalog, never gates a mutant) even though it now counts FHIR rows
  witnessed elsewhere.

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

(defn- hermetically-witnessable-rows
  "Every registry row except :judge-fhir-official ones (see ns
  docstring) -- those are witnessed by
  ehrt.integration.pairing-conviction-fhir-test instead."
  []
  (remove #(= :judge-fhir-official (:judge %)) (judge/load-pairing-registry)))

(deftest every-registry-row-witnesses-its-own-expected-class-test
  (doseq [{:keys [operator judge expected] :as row} (hermetically-witnessable-rows)]
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
  ;;
  ;; SCOPED TO FILE-LEVEL OPERATORS (2026-09-01, ADR-0176). This gate's
  ;; premise is that an operator's conviction is witnessed by a JUDGE --
  ;; a third-party validator reading bytes off disk. Event-log
  ;; operators (:format :event) have no judge and cannot acquire one:
  ;; their substrate is the in-memory ground-truth log, never a file,
  ;; and their oracle is `ehrt sim check`, whose catalog this
  ;; repository owns. They are NOT unwitnessed. Their conviction is
  ;; proved harder than this registry proves anything -- by the closed
  ;; oracle loop in ehrt.corpus.event-mutate-test, which asserts the
  ;; observed finding set EQUALS the declared one over a real generated
  ;; log, where a pairing row asserts only that SOME expected class
  ;; appears among the observed ones. Excluding them here is therefore
  ;; a statement about which instrument witnesses which layer, not a
  ;; coverage exemption; registering an event operator with no
  ;; convicting finding is refused outright at `corpus.operators/
  ;; register!` and recorded as a catalog gap, which is the event
  ;; layer's own version of this gate and is strictly earlier.
  (let [rows (judge/load-pairing-registry)
        operator-ids (->> (corpus/operator-entries)
                          (filter :doc)
                          (remove #(= :event (:format %)))
                          (map :id)
                          distinct)
        coverage (judge/pairing-coverage rows operator-ids)
        uncovered (into (sorted-set) (keep (fn [[id judges]] (when (empty? judges) id))) coverage)]
    (is (empty? uncovered)
        (str "operators with zero witnessed registry rows (any judge): " uncovered))))
