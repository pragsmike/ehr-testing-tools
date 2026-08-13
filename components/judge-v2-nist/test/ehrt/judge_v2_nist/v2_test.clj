(ns ehrt.judge-v2-nist.v2-test
  "interpret is pure (pattern nursery #1's whole point): these tests
  exercise the raw-capture -> findings/verdict function with synthetic
  captures shaped exactly like execute's output, no engine on the
  classpath needed. Engine-in-the-loop coverage lives behind a profile
  bundle fixture (Π) -- add one under test-fixtures once a
  representative IGAMT export is committed (adoption plan step 1)."
  (:require [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as kernel]
            [ehrt.judge-v2-nist.v2 :as v2]))

(def engine {:name "nist-v2-validation" :version "test"})

(defn- capture [entries & [exc]]
  {:engine engine :entries entries :check-exception exc})

(defn- entry [& {:as m}]
  (merge {:area "structure" :path "PID[1]-3[1]" :line 2 :column 1
          :category "Usage" :classification "Error" :description "d"}
         m))

(deftest error-classification-rejects
  (let [{:keys [verdict findings]} (v2/interpret (capture [(entry)]))]
    (is (= :rejected verdict))
    (is (= :error (:severity (first findings))))
    (is (= "structure/Usage" (:code (first findings))))))

(deftest engine-exception-rejects
  (is (= :rejected (:verdict (v2/interpret (capture [] {:class "x" :message "boom"}))))))

(deftest warnings-alone-pass
  (is (= :pass (:verdict (v2/interpret
                          (capture [(entry :classification "Warning")
                                    (entry :classification "Informational")]))))))

(deftest vs-suppression-is-no-verdict-with-cause
  (let [r (v2/interpret (capture [(entry :area "value-set"
                                         :category "VS Not Found"
                                         :classification "Informational")]))]
    (is (= :no-verdict (:verdict r)))
    (is (= :terminology-suppressed (:cause r)))))

(deftest spec-error-is-no-verdict-with-profile-spec-error-cause
  (let [r (v2/interpret (capture [(entry :classification "Specification Error")]))]
    (is (= :no-verdict (:verdict r)))
    (is (= :profile-spec-error (:cause r)))))

(deftest rejected-dominates-suppression
  (testing "worst-of semantics: a confirmed Error still rejects even when
            VS checking was suppressed elsewhere in the same file (same
            rationale as tools/ADR-0010's revised ranking)"
    (is (= :rejected (:verdict (v2/interpret
                                (capture [(entry)
                                          (entry :area "value-set"
                                                 :category "VS Not Found"
                                                 :classification "Informational")])))))))

(deftest empty-report-passes
  (is (= :pass (:verdict (v2/interpret (capture []))))))

;; ---- msg-id contract (ADR-0012): `execute`'s own refusal/default
;; logic runs before the engine is ever touched (the check sits at the
;; top of `execute`, ahead of the .check call), so these two tests use
;; synthetic validator-state maps -- no real SyncHL7Validator, no
;; profile bundle, matching this namespace's own synthetic-capture
;; style above. A nil :validator is safe for the single-id case: the
;; NIST call it reaches next is caught by execute's own try/catch and
;; returned as :check-exception, never thrown -- irrelevant to what
;; these tests assert. ----

(deftest plural-msg-ids-without-explicit-selection-refuses
  (testing "no implicit default (e.g. sorting) when a profile declares more than one msg-id"
    (let [ex (try
               (v2/execute {:msg-ids ["ORU_R01" "ADT_A01"] :bundle-sha256s {}} "MSH|...")
               nil
               (catch clojure.lang.ExceptionInfo e e))]
      (is (some? ex))
      (is (= :ambiguous-msg-id (:type (ex-data ex))))
      (is (= ["ORU_R01" "ADT_A01"] (:msg-ids (ex-data ex)))))))

(deftest single-msg-id-needs-no-explicit-selection
  (testing "a single-id profile defaults to it without an explicit :msg-id"
    (let [result (v2/execute {:msg-ids ["ORU_R01"] :bundle-sha256s {}} "MSH|...")]
      (is (= "ORU_R01" (:msg-id result))))))

;; ---- gate-file entry guard (ADR-0098): the .isFile check runs before
;; validator-state is ever touched, so a nil validator-state (same
;; synthetic-style convention as the msg-id-contract tests above) is
;; safe here too -- these tests never reach `execute`. ----

(deftest gate-file-missing-path-is-a-categorized-error-test
  (let [r (v2/gate-file nil "/no/such/file.hl7")]
    (is (kernel/error? r))
    (is (= :file-not-found (:category r)))
    (is (not (contains? (:payload r) :reason)))))

(deftest gate-file-permission-denied-path-is-a-categorized-error-test
  ;; the .isFile-only guard passed a chmod-000 path straight through to
  ;; a bare `slurp`, which threw raw -- extended to check .canRead too,
  ;; same :file-not-found category, a distinguishing :reason
  ;; :permission-denied payload key.
  (let [tmp (java.io.File/createTempFile "gate-v2-nist-test" ".hl7")
        path (.getAbsolutePath tmp)
        _ (spit path "MSH|^~\\&|")
        _ (shell/sh "chmod" "000" path)]
    (if (.canRead tmp)
      ;; root (or an equivalent environment) bypasses permission bits
      ;; entirely -- skip rather than silently lie about reproducing
      ;; the unreadable-file leg.
      (println "SKIPPED gate-file-permission-denied-path-is-a-categorized-error-test: running as an environment where chmod 000 did not remove read access (root?)")
      (let [r (v2/gate-file nil path)]
        (is (kernel/error? r))
        (is (= :file-not-found (:category r)))
        (is (= path (:path (:payload r))))
        (is (= :permission-denied (:reason (:payload r))))))
    (.setReadable tmp true false)
    (.delete tmp)))
