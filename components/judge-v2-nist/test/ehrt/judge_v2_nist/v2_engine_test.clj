(ns ehrt.judge-v2-nist.v2-engine-test
  "Engine-in-the-loop coverage for judge-v2-nist (v2_test.clj's own
  docstring named this gap): the real gov.nist:hl7-v2-validation
  1.7.3 SyncHL7Validator, built from CDC's vendored COVID19_ELR-v2.3.1
  Π fixture (components/tools/test-fixtures/v2-nist/, see that
  directory's own NOTICE.md for provenance), gating the fixture's one
  companion ER7 message.

  Provenance for the pinned numbers below: engine gov.nist:hl7-v2-validation
  1.7.3 (resolved from hit-nexus, artifacts.lock.edn's nist-hl7-v2-validation
  entry, sha256 3e5b6a9b95066c4abeae1435de0a06e08c43fa8e786bb5c1a609d8172925de50);
  fixture sha256s per NOTICE.md; measured 2026-07-30 (judge-v2-nist
  landing session). The engine's own jar packages no Maven
  pom.properties (confirmed by inspection: no META-INF/maven/ entry in
  hl7-v2-validation-1.7.3.jar), so `v2/engine-version` returns
  \"unknown\" for this engine -- an honest reflection of the jar's own
  packaging, not a defect in the version-lookup code (which already
  falls back to \"unknown\" when the resource isn't found, same as
  judge-v2-hapi's own hapi-version).

  These counts (473 total: structure 441, value-set 28, content 4)
  match the spike's own NOTES.md exactly -- same wiring (PROFILE.xml +
  CONSTRAINTS.xml only; VALUESETS-disabled.xml's name deliberately
  doesn't match `bundle-files`' :value-sets spelling, so it's never
  wired, which is the whole point of this fixture). The VERDICT/CAUSE
  differs from the spike notes' own prediction (:no-verdict/
  :terminology-suppressed there vs. :no-verdict/:profile-spec-error
  here) because this session's own Cause-growth work (ADR-0012) made
  `interpret` return the real :profile-spec-error cause directly
  instead of the spike's placeholder -- a consequence of this session's
  own change, not a wiring difference from the spike (see this
  landing's own deviation record).

  `gate-file` now returns the kernel/ok envelope (judge-family parity
  pass, ruled 2026-07-31, P2-2) -- this test unwraps :payload and
  additionally validates every finding against
  ehrt.judge.finding/Finding, giving this component its own missing
  test-tier dependency on `judge`, mirroring judge-v2-hapi's own
  v2_test.clj."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as kernel]
            [ehrt.judge.finding :as finding]
            [ehrt.judge-v2-nist.interface :as v2nist]))

(def ^:private bundle-dir "components/tools/test-fixtures/v2-nist/COVID19_ELR-v2.3.1")
(def ^:private message-file "components/tools/test-fixtures/v2-nist/covidELR/231HL7TestFilewithHHSData.txt")

(deftest make-validator-wires-exactly-the-recognized-bundle-files-test
  (testing "PROFILE.xml + CONSTRAINTS.xml are wired; VALUESETS-disabled.xml's
            name doesn't match the :value-sets spelling, so it's absent --
            the bundle-sha256s map covers exactly the files actually wired"
    (let [{:keys [msg-ids bundle-sha256s]} (v2nist/make-validator bundle-dir)]
      (is (= ["5e94ca8e16408b128af8a105"] msg-ids))
      (is (= #{:profile :constraints} (set (keys bundle-sha256s))))
      (is (= "5a709a2f719b2aa3ae900afba600f31e087ff3ee5a87bb550794f6b635fe4704"
             (:profile bundle-sha256s)))
      (is (= "9ed06afd7dc8fe2d0a2f418b28f15d7e0788a1259f570b14ece8911ee1dea0ee"
             (:constraints bundle-sha256s))))))

(deftest gate-file-against-real-engine-and-fixture-test
  (let [validator-state (v2nist/make-validator bundle-dir)
        r (v2nist/gate-file validator-state (io/file message-file))
        result (:payload r)
        findings (:findings result)
        area-of (fn [finding] (first (clojure.string/split (:code finding) #"/")))]
    (testing "gate-file returns the kernel envelope (parity pass, ruled 2026-07-31)"
      (is (kernel/ok? r)))
    (testing "verdict/cause: a defective Π (Specification Error entries,
              a consequence of the value-set library being absent) --
              :no-verdict/:profile-spec-error, not :rejected"
      (is (= :no-verdict (:verdict result)))
      (is (= :profile-spec-error (:cause result)))
      (is (finding/valid-cause-pairing? (:verdict result) (:cause result))))
    (testing "finding counts, pinned (see ns docstring for provenance)"
      (is (= 473 (count findings)))
      (is (= {"structure" 441 "value-set" 28 "content" 4}
             (frequencies (map area-of findings)))))
    (testing "every finding validates against the shared schema"
      (is (every? finding/valid? findings)))))
