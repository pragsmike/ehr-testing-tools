(ns ehrt.sim-emit-hl7.site-profile-test
  "docs/site-profiles.md: the site-profile schema (Task 1), MSH dialect
  and code-table override helpers (Task 2). Written before
  ehrt.sim-emit-hl7.site-profile exists (sim/ADR-0004 test-first)."
  (:require [clojure.test :refer [deftest is testing]]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim-emit-hl7.site-profile :as site-profile]))

;; --- Task 1: schema, all keys optional, absent profile is legal ----------

(deftest absent-profile-is-a-valid-profile
  (testing "the default profile IS the absent profile (docs/site-profiles.md)
            -- nil and {} both validate, every key is optional"
    (is (site-profile/valid-profile? {}))))

(deftest a-fully-populated-profile-validates
  (let [profile {:name "St. Aldric's Memorial"
                 :msh {:version "2.5.1" :sending-app "ALDRIC-EHR" :sending-facility "ALDRIC"
                       :receiving-app "DOWNSTREAM" :receiving-facility "DOWNSTREAM-FAC"}
                 :code-tables {:patient-class {:inpatient {:code "IN" :coding-system "99ALDRIC"}}
                               :discharge-disposition {:discharged-to-home {:code "HOME"}}}
                 :naming {:surge-format "%s-OVERFLOW-%d"}
                 :z-segments [{:segment "ZPI" :trigger #{:admission}
                               :fields [{:path [:persona :payer :type]}
                                        {:literal "V1"}]}]}]
    (is (site-profile/valid-profile? profile))))

(deftest malformed-profile-fails-validation
  (testing "a Z-segment name that isn't Z-prefixed is rejected"
    (is (not (site-profile/valid-profile?
              {:z-segments [{:segment "XPI" :trigger #{:admission} :fields []}]})))))

;; --- Task 2: MSH dialect ---------------------------------------------------

(deftest effective-msh-defaults-match-todays-hardcoded-values
  (testing "absent profile / nil / {} all render today's hard-coded MSH
            values -- the default-profile identity anchor, at the MSH layer"
    (is (= site-profile/default-msh (site-profile/effective-msh nil)))
    (is (= site-profile/default-msh (site-profile/effective-msh {})))))

(deftest effective-msh-overrides-field-by-field
  (let [profile {:msh {:version "2.5.1" :sending-app "ALDRIC-EHR"}}]
    (is (= (assoc site-profile/default-msh :version "2.5.1" :sending-app "ALDRIC-EHR")
           (site-profile/effective-msh profile)))))

;; --- post-M6 (sim/ADR-0014, Task 4): MSH-11 processing id -----------------------

(deftest default-msh-processing-id-is-P
  (is (= "P" (:processing-id site-profile/default-msh))))

(deftest effective-msh-overrides-processing-id
  (is (= "T" (:processing-id (site-profile/effective-msh {:msh {:processing-id "T"}}))))
  (is (= "D" (:processing-id (site-profile/effective-msh {:msh {:processing-id "D"}})))))

(deftest processing-id-only-accepts-the-documented-enum
  (is (site-profile/valid-profile? {:msh {:processing-id "T"}}))
  (is (site-profile/valid-profile? {:msh {:processing-id "D"}}))
  (is (not (site-profile/valid-profile? {:msh {:processing-id "X"}}))))

;; --- Task 2: code-table overrides ------------------------------------------

(deftest code-for-falls-back-to-standard-when-no-override
  (is (= ["I"] (site-profile/code-for nil :patient-class
                                      site-profile/standard-patient-class-codes :inpatient)))
  (is (= ["I"] (site-profile/code-for {} :patient-class
                                      site-profile/standard-patient-class-codes :inpatient))))

(deftest code-for-uses-the-profiles-override-when-present
  (let [profile {:code-tables {:patient-class {:inpatient {:code "IN" :coding-system "99ALDRIC"}}}}]
    (is (= ["IN" "99ALDRIC"]
           (site-profile/code-for profile :patient-class
                                  site-profile/standard-patient-class-codes :inpatient)))))

(deftest code-for-discharge-disposition-standard-default
  (is (= ["01"] (site-profile/code-for nil :discharge-disposition
                                      site-profile/standard-discharge-disposition-codes
                                      :discharged-to-home))))

;; --- Task 2: :naming :surge-format facility-config transform --------------

(deftest apply-naming-is-identity-when-no-naming-key
  (is (= sim-model/default-facility (site-profile/apply-naming nil sim-model/default-facility)))
  (is (= sim-model/default-facility (site-profile/apply-naming {} sim-model/default-facility))))

(deftest apply-naming-overrides-every-wards-surge-format-when-present
  (let [profile {:naming {:surge-format "%s-OVERFLOW-%d"}}
        result (site-profile/apply-naming profile sim-model/default-facility)]
    (is (every? #(= "%s-OVERFLOW-%d" (:surge-format %)) (:wards result)))
    (testing "profile wins over the ward's own facility-level surge-format"
      (is (not= sim-model/default-facility result)))))
