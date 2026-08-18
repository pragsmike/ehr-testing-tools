(ns ehrt.sim.run-test
  "The `sim run` capability's result-not-throw contract. Milestone M2b,
  Task 0: allocation-ladder exhaustion is a structured outcome, not a
  thrown exception -- ehrt.sim-model.facility/allocate returns
  {:exhausted true ...} instead of throwing, ehrt.sim-engine.engine/run
  halts the loop and echoes it back, and run-command surfaces it as
  :error :capacity-exhausted with the patient, ward, and census in the
  payload (docs/clinical-realities.md's ED-diversion stub names the
  modeling gap this leaves for M3+: a real waiting/diversion state).

  Milestone M4, Task 0: the plumbing-completeness test below is the red
  test that reproduces the tools consumer-loop's own finding -- M3's
  `:pathways` reached `ehrt.sim-engine.engine/run` from a direct API
  caller (engine-test exercises it directly) but never from
  `run-command`, so it was invisible to every CLI invocation despite
  181 green tests and a demo. `ehrt.sim-engine.engine/config-keys` is
  now the canonical, documented list of every key `engine/run` accepts;
  this test asserts `run-command` forwards ALL of them, not just the
  ones already known to work, using the injectable `:engine-run-fn`
  seam (same -fn convention as `ehrt.sim-cli.core/dispatch-action`)
  so no real simulation ever has to run against sentinel data."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [ehrt.kernel.interface :as result]
            [ehrt.sim-engine.engine :as engine]
            [ehrt.sim-model.interface :as sim-model]
            [ehrt.sim.run :as run]))

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
  "One sentinel value per ehrt.sim-engine.engine/config-keys entry --
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
   :order-profiles ::order-profiles-sentinel
   :module-assignment ::module-assignment-sentinel
   :module-horizon-days ::module-horizon-days-sentinel
   :history ::history-sentinel})

(deftest run-command-forwards-every-engine-config-key
  (testing "the FULL ehrt.sim-engine.engine/config-keys set reaches
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

;; --- M5b: :modules (names) -> closure-shaped entries ----------------------
;; ADR-0033 AR-2: engine-facing :modules entries are now ALWAYS closure-
;; shaped (`load-closure`'s own :ok payload), whether or not the named
;; module actually calls a submodule.

(deftest run-command-resolves-module-names-against-the-real-vendored-directory
  (testing "the config/CLI-facing :modules (names) translates to engine/
            run's own :modules (already-loaded, closure-shaped entries,
            ADR-0033 AR-2) -- the SAME kind of translation :churn/
            :churn-profile already does"
    (let [captured (atom nil)
          stub-engine-run (fn [engine-opts] (reset! captured engine-opts) {:ground-truth [] :facility nil :providers nil})]
      (run/run-command {:seed 1 :modules ["sinusitis"]} {:engine-run-fn stub-engine-run})
      (is (= 1 (count (:modules @captured))))
      (let [closure (first (:modules @captured))]
        (is (= "sinusitis" (:root closure)))
        (is (= "sinusitis" (:id (get (:modules closure) "sinusitis"))))
        (is (= {} (:tables closure)))
        (is (not (contains? closure :initial-attributes))
            "absent :module-initial-attributes means no :initial-attributes key at all, byte-identical to pre-ADR-0033")))))

(deftest run-command-threads-module-initial-attributes-onto-the-resolved-closure
  (testing "ADR-0033 AR-1: :module-initial-attributes, keyed by module
            name, attaches per-entry onto the resolved closure -- a
            scenario-authoring seed the engine only ever threads, never
            invents"
    (let [captured (atom nil)
          stub-engine-run (fn [engine-opts] (reset! captured engine-opts) {:ground-truth [] :facility nil :providers nil})]
      (run/run-command {:seed 1 :modules ["sinusitis"]
                        :module-initial-attributes {"sinusitis" {:some-attr 1}}}
                       {:engine-run-fn stub-engine-run})
      (is (= {:some-attr 1} (:initial-attributes (first (:modules @captured))))))))

(deftest run-command-surfaces-an-unresolvable-module-name-as-a-structured-error
  (let [r (run/run-command {:seed 1 :modules ["not-a-real-module"]})]
    (is (result/error? r))
    (is (= :module-not-found (:category r)))
    (is (= "not-a-real-module" (:module (:payload r))))))

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
  (testing "a :site-profile reaches ehrt.sim-emit-hl7.emit-hl7/emit (its own
            MSH dialect renders) without being a member of
            ehrt.sim-engine.engine/config-keys"
    (is (not (contains? (set engine/config-keys) :site-profile)))
    (let [r (run/run-command {:seed 42 :patients 1 :emit "hl7"
                              :site-profile {:msh {:sending-app "ALDRIC-EHR"}}})
          message (first (:messages (:payload r)))]
      (is (result/ok? r))
      (is (str/includes? message "ALDRIC-EHR")))))

;; --- M6 Task 0: the tools full-capability session's own reproduction --
;; assigning one patient ordinal BOTH an authored encounter-opening
;; pathway AND a GMF module used to reach engine/run and blow up as
;; :self-check-failed only once the invariant catalog caught the
;; resulting double-encounter -- a config authoring error wearing a
;; "bug in us" category. Caught here, statically, before any simulation
;; runs (sim/ADR-0007's single-encounter-horizon makes the combination
;; illegal by construction).

(deftest run-command-rejects-a-patient-assigned-both-the-default-pathway-and-a-module
  (testing "the plainest reproduction shape: no :pathways override at
            all means EVERY ordinal gets the default (admission-
            bearing) :pathway -- an explicit module assignment on any
            ordinal therefore always conflicts"
    (let [r (run/run-command {:seed 1 :patients 1
                              :module-assignment [{:patient-ordinal 0 :module-id "sinusitis"}]})]
      (is (result/rejected? r))
      (is (= :incompatible-assignment (:category r)))
      (let [[conflict] (:conflicts (:payload r))]
        (is (= 0 (:patient-ordinal conflict)))
        (is (= :pathway (:pathway-source conflict)))
        (is (= :module-assignment (:module-source conflict)))))))

(deftest run-command-rejects-explicit-per-ordinal-pathway-plus-module-conflict-but-not-the-disjoint-cohort-pattern
  (testing "the tools full-capability fixture's own shape: ordinal 0 gets
            BOTH an admission-bearing pathway and a module (illegal);
            ordinal 1 gets an EMPTY pathway plus a module (legal, the
            documented module-only-patient pattern) -- only ordinal 0
            is reported"
    (let [r (run/run-command {:seed 1 :patients 2
                              :pathways [{:patient-ordinal 0 :pathway sim-model/sample-admission-discharge}
                                         {:patient-ordinal 1 :pathway {:name "module-only" :steps []}}]
                              :module-assignment [{:patient-ordinal 0 :module-id "sinusitis"}
                                                  {:patient-ordinal 1 :module-id "sinusitis"}]})]
      (is (result/rejected? r))
      (is (= :incompatible-assignment (:category r)))
      (is (= [0] (mapv :patient-ordinal (:conflicts (:payload r)))))
      (is (= :pathways (:pathway-source (first (:conflicts (:payload r)))))))))

(deftest run-command-does-not-reject-the-legal-disjoint-cohort-pattern
  (testing "an empty pathway plus a module, alone, is not a conflict --
            proves the check doesn't over-reject the documented
            module-only-patient pattern engine-test's own fixtures use"
    (let [captured (atom nil)
          stub-engine-run (fn [engine-opts] (reset! captured engine-opts) {:ground-truth [] :facility nil :providers nil})
          r (run/run-command {:seed 1 :patients 1
                              :pathways [{:pathway {:name "module-only" :steps []} :weight 1}]
                              :module-assignment [{:module-id "sinusitis" :weight 1}]}
                             {:engine-run-fn stub-engine-run})]
      (is (result/ok? r))
      (is (some? @captured)))))

(deftest incompatible-assignment-check-does-not-misfire-on-the-plumbing-completeness-sentinels
  (testing "sentinel-opts (above) carries a bare-keyword :module-assignment
            and :pathways -- structurally invalid, so the check must
            skip rather than throw or false-positive"
    (let [captured (atom nil)
          stub-engine-run (fn [engine-opts] (reset! captured engine-opts) {:ground-truth [] :facility nil :providers nil})
          r (run/run-command sentinel-opts {:engine-run-fn stub-engine-run})]
      (is (result/ok? r))
      (is (some? @captured)))))

;; --- M6 Task 1: --emit fhir --------------------------------------------

(deftest run-command-emit-fhir-produces-end-of-run-bundles-per-patient
  (let [r (run/run-command {:seed 42 :patients 2 :emit "fhir"})]
    (is (result/ok? r))
    (let [bundles (:fhir-bundles (:payload r))]
      (is (= 2 (count bundles)))
      (doseq [[patient-id bundle] bundles]
        (is (string? patient-id))
        (is (= "Bundle" (:resourceType bundle)))))))

(deftest run-command-emit-fhir-honors-an-explicit-at-instant
  (testing "--at queries an arbitrary instant, not only end-of-run"
    (let [end (run/run-command {:seed 42 :patients 1 :emit "fhir"})
          mid (run/run-command {:seed 42 :patients 1 :emit "fhir" :at 0})
          pid (engine/patient-id-for 42 0)]
      (is (not= (get (:fhir-bundles (:payload end)) pid)
                (get (:fhir-bundles (:payload mid)) pid))
          "an early instant sees less state than end-of-run"))))

(deftest run-command-without-emit-carries-no-fhir-bundles-key
  (let [r (run/run-command {:seed 42 :patients 1})]
    (is (not (contains? (:payload r) :fhir-bundles)))))

(deftest run-command-config-file-passthrough-carries-site-profile
  (testing ":site-profile is a data-heavy key with no CLI flag of its own --
            the same :config passthrough vehicle :pathway/:order-profiles use"
    (let [tmp (java.io.File/createTempFile "sim-config" ".edn")
          _ (spit tmp (pr-str {:site-profile {:msh {:sending-app "FROM-FILE"}}}))]
      (try
        (let [r (run/run-command {:seed 1 :patients 1 :emit "hl7" :config (.getPath tmp)})]
          (is (str/includes? (first (:messages (:payload r))) "FROM-FILE")))
        (finally (.delete tmp))))))

;; --- ADR-0109: :latency is the SAME emit-only, config-passthrough
;; treatment :site-profile already gets -- proven end to end via
;; :emit "hl7", never via the engine-run-fn seam above, since :latency
;; never reaches engine/run at all.

(deftest run-command-threads-latency-into-emit-wire-transmit-time-ordering
  (testing "a :latency profile reaches ehrt.sim-emit-hl7.emit-hl7/emit-wire
            (transmit-time order, MSH-7 shifted) without being a member
            of ehrt.sim-engine.engine/config-keys"
    (is (not (contains? (set engine/config-keys) :latency)))
    (let [pathway {:name "admission-transfer-discharge"
                   :steps [{:type :admission :location "Renal"}
                           {:type :delay :from 30 :to 30}
                           {:type :transfer :location "Cardiology"}
                           {:type :delay :from 30 :to 30}
                           {:type :discharge}]}
          latency {:admission {:from-minutes 6000 :to-minutes 6000}}
          plain (run/run-command {:seed 1 :patients 1 :emit "hl7" :pathways [{:pathway pathway :weight 1}]})
          wired (run/run-command {:seed 1 :patients 1 :emit "hl7" :pathways [{:pathway pathway :weight 1}]
                                  :latency latency})]
      (is (result/ok? plain))
      (is (result/ok? wired))
      (testing "log order: admission first"
        (is (str/includes? (first (:messages (:payload plain))) "^A01")))
      (testing "wire order: the huge-latency admission is pushed past its followers"
        (is (not (str/includes? (first (:messages (:payload wired))) "^A01")))
        (is (str/includes? (last (:messages (:payload wired))) "^A01"))))))

(deftest run-command-without-latency-renders-plain-emit-byte-identical
  (testing "absent :latency is the identity input -- :messages renders
            byte-identical to a run that never named the key at all"
    (let [without-key (run/run-command {:seed 42 :patients 3 :emit "hl7"})
          with-nil (run/run-command {:seed 42 :patients 3 :emit "hl7" :latency nil})]
      (is (= (:messages (:payload without-key)) (:messages (:payload with-nil)))))))

(deftest run-command-config-file-passthrough-carries-latency
  (testing ":latency is a data-heavy key with no CLI flag of its own --
            the same :config passthrough vehicle :site-profile uses"
    (let [pathway {:name "admission-only" :steps [{:type :admission :location "Renal"}]}
          tmp (java.io.File/createTempFile "sim-config" ".edn")
          _ (spit tmp (pr-str {:latency {:admission {:from-minutes 60 :to-minutes 60}}
                               :pathways [{:pathway pathway :weight 1}]}))]
      (try
        (let [plain (run/run-command {:seed 1 :patients 1 :emit "hl7"
                                      :pathways [{:pathway pathway :weight 1}]})
              wired (run/run-command {:seed 1 :patients 1 :emit "hl7" :config (.getPath tmp)})]
          (is (not= (first (:messages (:payload plain))) (first (:messages (:payload wired))))
              "MSH-7 shifted by the config-file-supplied :latency profile"))
        (finally (.delete tmp))))))

;; --- C-1/U4 (ux fixes 2, ADR-0060): --config crashes with a raw JVM
;; exception today (`merge-config-file` does `(edn/read-string (slurp
;; path))` with no exception handling at all) -- these tests pin the
;; Result-wrapped replacement: a missing path is :config-not-found
;; (U4: plus :did-you-mean when a same-stem sibling exists in the same
;; directory), a present-but-malformed file is :config-unreadable, and
;; either category propagates through run-command/identifiers-command
;; unchanged, exactly as :missing-required-opt already does. ---------

(defn- temp-dir-path*
  "Quality riders (AR-QR-2, ADR-0076): the prior delete-then-mkdirs
  sequence raced another process for the same temp name between
  `createTempFile` and `.delete` -- both ignored-boolean calls, no
  failure ever surfaced. `Files/createTempDirectory` creates the
  directory in one atomic step and throws on failure instead."
  []
  (str (java.nio.file.Files/createTempDirectory
        "merge-config-file-test"
        (make-array java.nio.file.attribute.FileAttribute 0))))

(deftest merge-config-file-returns-config-not-found-for-a-missing-path
  (let [dir (temp-dir-path*)
        path (str dir "/does-not-exist.edn")
        r (run/merge-config-file {:config path})]
    (is (result/error? r))
    (is (= :config-not-found (:category r)))
    (is (= path (:path (:payload r))))
    (is (not (contains? (:payload r) :did-you-mean))
        "no same-stem sibling exists in this fresh temp dir")))

(deftest merge-config-file-returns-config-unreadable-for-malformed-edn
  (let [tmp (java.io.File/createTempFile "malformed-config" ".edn")
        _ (spit tmp "{:pathway {:name \"broken\"")] ;; deliberately truncated
    (try
      (let [r (run/merge-config-file {:config (.getPath tmp)})]
        (is (result/error? r))
        (is (= :config-unreadable (:category r)))
        (is (= (.getPath tmp) (:path (:payload r))))
        (is (string? (:message (:payload r))))
        (is (seq (:message (:payload r)))))
      (finally (.delete tmp)))))

(deftest merge-config-file-suggests-a-same-stem-sibling-file
  (testing "the founding incident's own shape, reproduced in a fixture
            this test builds itself (player board, AR-BB2-R): a
            same-stem .md sibling sits where the requested .edn was
            named -- busy-weekday.edn does not exist alongside it"
    (let [dir (temp-dir-path*)
          md-path (str dir "/busy-weekday.md")
          edn-path (str dir "/busy-weekday.edn")
          _ (spit md-path "not actually config")
          r (run/merge-config-file {:config edn-path})]
      (is (result/error? r))
      (is (= :config-not-found (:category r)))
      (is (= edn-path (:path (:payload r))))
      (is (= md-path (:did-you-mean (:payload r)))
          (let [dir-file (java.io.File. ^String dir)
                listed (.listFiles dir-file)]
            (str "same-stem sibling lookup mismatch (quality riders, AR-QR-2) -- "
                 "temp dir " dir ", .list() => " (pr-str (seq (.list dir-file)))
                 ", raw .listFiles() => " (if (nil? listed) "nil" (pr-str (seq listed)))))))))

(deftest merge-config-file-with-no-config-key-is-the-identity-on-opts
  (is (= (result/ok {:seed 1 :patients 2}) (run/merge-config-file {:seed 1 :patients 2}))))

(deftest run-command-propagates-config-not-found-unchanged
  (let [dir (temp-dir-path*)
        path (str dir "/does-not-exist.edn")
        r (run/run-command {:seed 1 :patients 1 :config path})]
    (is (result/error? r))
    (is (= :config-not-found (:category r)))
    (is (= path (:path (:payload r))))))

;; --- ADR-0153: the seed-202 self-check failure, at the run level ----------

(deftest ed-tuesday-churn-seed-202-self-checks-clean
  (testing "roadmap.md#surge-policy-self-check-202 (census S-5): the exact
            reproducing invocation --
            `ehrt sim run --seed 202 --patients 100 --churn --config
            demos/scenarios/ed-tuesday/config.edn` -- exited :error
            :self-check-failed on :surge-only-when-earlier-rungs-exhausted
            at t 78480, because the bed-ready transfer coupling handed a
            waiting boarder a just-vacated SURGE slot while RENAL-04 stood
            free (ehrt.sim-engine.engine's own decide :discharge). Kept at
            the per-push tier, not integration: the whole run is ~1s in a
            warm JVM, and the demo config it reads is tracked content."
    (let [r (run/run-command {:seed 202 :patients 100 :churn true
                              :config "demos/scenarios/ed-tuesday/config.edn"})]
      (is (result/ok? r) (str "violations: " (pr-str (:payload r))))
      (is (seq (:ground-truth (:payload r)))))))
