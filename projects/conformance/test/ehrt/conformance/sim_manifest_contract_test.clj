(ns ehrt.conformance.sim-manifest-contract-test
  "DATED NOTE (sim split B, M1 step 3, 2026-08-04,
  `.agents/plans/2026-08-04-sim-split-b-plan.md` AR-5(b) / this
  session's own AR-M1-3): this test's ORIGINAL framing -- below,
  preserved -- was 'the binding half' of a two-part drift-detection
  design: sim carried a local mirror of this repo's manifest schema as
  a TRIPWIRE (`ehr-testing-sim.manifest/MirroredManifest`), and this
  test was the other half, validating sim's real output against the
  authoritative schema so the mirror's drift couldn't hide. That
  design retires with the mirror itself (Step 4, AR-M1-2): drift
  becomes impossible by construction once both sides read the same
  `ehrt.provenance.interface/ManifestV1_1` var, so there is nothing
  left to detect drift between. What survives, unchanged, is this
  test's other purpose -- end-to-end builder validity: run sim for
  real, take its own emitted :manifest verbatim, and prove it actually
  conforms to the schema intake expects, exercised through the real
  harness rather than a hand-built fixture.
  `built-manifest-validates-against-provenance-test`
  (`ehrt.sim.manifest-test`, added this same step) is the fast-lane
  companion -- same claim, no harness, builder output checked directly
  against provenance.

  ORIGINAL framing (ehr-testing-sim's own ADR-0001 clause 5, scaffold
  time): sim mirrors this repo's corpus.manifest schema locally as a
  drift TRIPWIRE, but a mirror can only detect that it drifted from
  what it once copied -- never that the copy itself now disagrees with
  the authoritative source. This test was the binding half: run sim
  for real and validate its own emitted :manifest against the real
  schema, the one sim's own manifest.clj docstring says it targets.

  Findings, not silently bent schemas (this session's own prompt;
  AUTHORS-GUIDE.md section 7's two-failure-modes discipline): if sim's
  manifest fails to validate, that failure IS the deliverable -- report
  the exact malli mismatch, never loosen ManifestV1_1 to accommodate
  it. Runs unconditionally (ADR-0005): sim is an in-process mount now,
  never a sibling checkout that might be absent.

  ADR-0005 dated finding, 2026-07-28: this test's own :generator :name
  expectation was still \"ehr-testing-sim\" -- stale since the H2
  rename (ADR-0001) gave sim its current self-identity, \"ehrt.sim\"
  (components/sim/src/ehrt/sim/manifest.clj). This test path never
  actually ran end to end before the in-process mount (always skipped,
  local and CI both, for lack of a sibling checkout), so the staleness
  went uncaught until now. Per AUTHORS-GUIDE.md's own two-failure-modes
  split, this is the SECOND mode -- \"a check misencoding its own
  invariant\" -- not sim-side drift: the rename was already deliberate
  and ratified (ADR-0001's own mechanical rename), so the fix is
  correcting this test's stale expectation, not leaving it red."
  (:require [clojure.test :refer [deftest is]]
            [malli.core :as m]
            [ehrt.kernel.interface :as result]
            [ehrt.provenance.interface :as provenance]
            [ehrt.conformance.sim-harness :as sim-harness]))

(deftest sim-manifest-conforms-to-tools-manifest-v1-1-test
  (let [run-result (sim-harness/run! {:seed 100 :patients 1})]
    (when-not (result/ok? run-result)
      (throw (ex-info "sim-manifest-contract: sim run failed" run-result)))
    (let [mf (:manifest (:payload run-result))]
      (is (m/validate provenance/ManifestV1_1 mf)
          (str "sim's emitted manifest does not conform to ManifestV1_1 -- "
               "exact mismatch (a FINDING for the sim repo, not something "
               "to paper over here): "
               (pr-str (m/explain provenance/ManifestV1_1 mf))))
      ;; The fields intake cares about, asserted individually so a
      ;; single :schema-version-shaped mismatch above doesn't obscure
      ;; whether the rest of the manifest is otherwise sound.
      (is (= :simulated (:stage mf)))
      (is (= "ehrt.sim" (get-in mf [:generator :name])))
      (is (string? (get-in mf [:generator :version])))
      (is (map? (:seeds mf)))
      (is (every? keyword? (keys (:seeds mf))))
      (is (every? int? (vals (:seeds mf)))))))
