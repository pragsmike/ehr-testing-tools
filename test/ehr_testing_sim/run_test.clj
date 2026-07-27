(ns ehr-testing-sim.run-test
  "The `sim run` capability's result-not-throw contract. Milestone M2b,
  Task 0: allocation-ladder exhaustion is a structured outcome, not a
  thrown exception -- ehr-testing-sim.facility/allocate returns
  {:exhausted true ...} instead of throwing, ehr-testing-sim.engine/run
  halts the loop and echoes it back, and run-command surfaces it as
  :error :capacity-exhausted with the patient, ward, and census in the
  payload (docs/clinical-realities.md's ED-diversion stub names the
  modeling gap this leaves for M3+: a real waiting/diversion state).

  Milestone M4, Task 0: the plumbing-completeness test below is the red
  test that reproduces the tools consumer-loop's own finding -- M3's
  `:pathways` reached `ehr-testing-sim.engine/run` from a direct API
  caller (engine-test exercises it directly) but never from
  `run-command`, so it was invisible to every CLI invocation despite
  181 green tests and a demo. `ehr-testing-sim.engine/config-keys` is
  now the canonical, documented list of every key `engine/run` accepts;
  this test asserts `run-command` forwards ALL of them, not just the
  ones already known to work, using the injectable `:engine-run-fn`
  seam (same -fn convention as `ehr-testing-sim.cli/dispatch-action`)
  so no real simulation ever has to run against sentinel data."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehr-testing-sim.engine :as engine]
            [ehr-testing-sim.run :as run]
            [ehr-testing-sim.result :as result]))

(def ^:private one-bed-no-ed-facility
  "A facility with exactly one bed and no ED ward at all -- the second
  admission has nowhere to go at any ladder rung, guaranteeing
  exhaustion deterministically rather than relying on a lucky seed."
  {:id :tiny
   :wards [{:id :renal :name "Renal" :beds 1 :surge-slots 0
            :surge-format "%s-H%02d" :class :inpatient}]})

(deftest run-command-surfaces-capacity-exhaustion-as-a-structured-error
  (let [r (run/run-command {:seed 1 :patients 2 :facility one-bed-no-ed-facility})]
    (testing "an :error, not a thrown exception reaching the caller"
      (is (result/error? r))
      (is (= :capacity-exhausted (:category r))))
    (testing "payload carries patient, ward, and census"
      (let [{:keys [patient-id ward census]} (:payload r)]
        (is (string? patient-id))
        (is (= "Renal" ward))
        (is (= {"Renal" {:occupied 1 :capacity 1}} census))))))

;; --- M4 Task 0: plumbing completeness -------------------------------------

(def ^:private sentinel-opts
  "One sentinel value per ehr-testing-sim.engine/config-keys entry --
  distinguishable from any real default so a dropped key reads as a
  clear mismatch, not a coincidental match. :churn-profile is a MAP
  sentinel, not a bare keyword: run-command's own effective-churn-
  profile logic merges a caller-supplied :churn-profile over
  churn/default-churn-profile before forwarding it, so its sentinel
  must survive being merge'd, not compared for raw equality (see the
  test below)."
  {:seed 42
   :patients ::patients-sentinel
   :pathway ::pathway-sentinel
   :pathways ::pathways-sentinel
   :arrival-gap ::arrival-gap-sentinel
   :warm-up-seconds ::warm-up-seconds-sentinel
   :facility ::facility-sentinel
   :providers ::providers-sentinel
   :churn-profile {::churn-profile-sentinel true}
   :order-profiles ::order-profiles-sentinel})

(deftest run-command-forwards-every-engine-config-key
  (testing "the FULL ehr-testing-sim.engine/config-keys set reaches
            engine/run, not just the keys already known to work -- red
            today on :pathway/:pathways/:order-profiles (M3's
            :pathways was the tools consumer-loop's own finding; this
            test audits for every other straggler at once, per its own
            documented purpose)"
    (let [captured (atom nil)
          stub-engine-run (fn [engine-opts]
                            (reset! captured engine-opts)
                            {:ground-truth [] :facility nil :providers nil})]
      (run/run-command sentinel-opts {:engine-run-fn stub-engine-run})
      (doseq [k engine/config-keys]
        (if (= :churn-profile k)
          (is (true? (::churn-profile-sentinel (:churn-profile @captured)))
              "the :churn-profile sentinel must survive run-command's own default-merge")
          (is (= (get sentinel-opts k) (get @captured k))
              (str k " was not forwarded from run-command to engine/run")))))))

(deftest run-command-config-file-passthrough
  (testing "M4 Task 0: :config (a path to an EDN file) supplies the
            data-heavy keys that have no CLI flag of their own -- read
            once, merged UNDER explicit opts"
    (let [tmp (java.io.File/createTempFile "sim-config" ".edn")
          _ (spit tmp (pr-str {:pathways ::from-file-pathways :patients ::from-file-patients}))
          captured (atom nil)
          stub-engine-run (fn [engine-opts] (reset! captured engine-opts) {:ground-truth [] :facility nil :providers nil})]
      (try
        (run/run-command {:seed 1 :config (.getPath tmp) :patients ::from-opts-patients}
                          {:engine-run-fn stub-engine-run})
        (is (= ::from-file-pathways (:pathways @captured))
            "a key the file names but opts doesn't should reach the engine")
        (is (= ::from-opts-patients (:patients @captured))
            "an explicit opt wins over the same key in the config file")
        (finally (.delete tmp))))))

;; --- Milestone site-profiles: :site-profile is emit-only, never an
;; engine/run input (docs/site-profiles.md's own binds-at-emit-time-only
;; law) -- passthrough proven end to end via :emit "hl7", not via the
;; engine-run-fn seam above, since :site-profile never reaches engine/run
;; at all.

(deftest run-command-threads-site-profile-into-emitted-messages
  (testing "a :site-profile reaches ehr-testing-sim.emit-hl7/emit (its own
            MSH dialect renders) without being a member of
            ehr-testing-sim.engine/config-keys"
    (is (not (contains? (set engine/config-keys) :site-profile)))
    (let [r (run/run-command {:seed 42 :patients 1 :emit "hl7"
                              :site-profile {:msh {:sending-app "ALDRIC-EHR"}}})
          message (first (:messages (:payload r)))]
      (is (result/ok? r))
      (is (str/includes? message "ALDRIC-EHR")))))

(deftest run-command-config-file-passthrough-carries-site-profile
  (testing ":site-profile is a data-heavy key with no CLI flag of its own --
            the same :config passthrough vehicle :pathway/:order-profiles use"
    (let [tmp (java.io.File/createTempFile "sim-config" ".edn")
          _ (spit tmp (pr-str {:site-profile {:msh {:sending-app "FROM-FILE"}}}))]
      (try
        (let [r (run/run-command {:seed 1 :patients 1 :emit "hl7" :config (.getPath tmp)})]
          (is (str/includes? (first (:messages (:payload r))) "FROM-FILE")))
        (finally (.delete tmp))))))
