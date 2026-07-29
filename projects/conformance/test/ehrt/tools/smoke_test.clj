(ns ehrt.tools.smoke-test
  "T1, integration-smoke (2026-07-27 verification-tiers session, ADR-0016):
  the sub-2-minute session-boundary tier between T0 (make test, hermetic,
  seconds) and T2 (make integration, ~19min cold / DOC-4) -- proves the
  ehr-testing-sim cross-repo consumer loop seam T2 exercises at length
  still wires end-to-end, without T2's own full cost.

  sim-harness half only, as of 2026-07-28 (ADR-0004, carve-loss recovery
  session): ONE run! at a fixed seed (100, 1 patient -- matching
  sim_manifest_contract_test.clj's own smallest-known-fast invocation)
  -- asserts the run completes and its own emitted :manifest validates
  against corpus.manifest/ManifestV1_1 (session ruling 1's own
  \"manifest validates\" -- the SAME binding contract
  sim_manifest_contract_test.clj checks at T2 depth, field-by-field;
  this tier asserts only the schema-validation half, not each field, and
  is not a substitute for that suite: a schema-conformant manifest can
  still drift on individual field values, which is exactly what T2's
  own per-field assertions catch and T1 deliberately does not). Runs
  unconditionally now (ADR-0005, same session): sim is an in-process
  mount, never a sibling checkout that might be absent -- the
  skip-when-absent branch every sim-consuming suite in this tree used
  to carry is gone.

  The FHIR-validator half this file originally also carried (ONE
  clean/mutant pairing-polarity check) moved to
  `projects/integration/test/ehrt/tools/smoke_test.clj` in the same
  session, along the seam this docstring already drew (\"FHIR half\" /
  \"sim-harness half\") -- it needs `ehr artifact fetch` machinery this
  project's own lane (R18/R19, ADR-0004) deliberately excludes from
  per-push, while this half needs only `ehrt.tools.sim-harness`, a
  conformance-project-local test helper (`sim_harness.clj`, this same
  directory) also required by this project's five sim_*_test.clj
  suites -- a dependency that made keeping the two halves in one
  project-scoped test file impossible once they needed different lanes.
  This is a real, disclosed cost: T1's original point was proving BOTH
  real-engine seams wire end-to-end in one sub-2-minute pass; split, the
  FHIR half only runs on the nightly/dispatch integration lane, not
  per-push, and per-push's own DOC-5/ci-parity checks no longer smoke
  the FHIR seam at all until that lane runs. See ADR-0004 for the full
  disposition."
  (:require [clojure.test :refer [deftest is]]
            [malli.core :as m]
            [ehrt.tools.interface :as result]
            [ehrt.tools.interface :as manifest]
            [ehrt.tools.sim-harness :as sim-harness]))

(deftest sim-harness-manifest-smoke-test
  (let [run-result (sim-harness/run! {:seed 100 :patients 1})]
    (is (result/ok? run-result) "sim-harness/run! completes at a fixed seed")
    (when (result/ok? run-result)
      (let [mf (:manifest (:payload run-result))]
        (is (m/validate manifest/ManifestV1_1 mf)
            (str "sim's emitted manifest does not conform to ManifestV1_1: "
                 (pr-str (m/explain manifest/ManifestV1_1 mf))))))))
