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
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [ehrt.kernel.interface :as result]
            [ehrt.patient-simulator.interface :as patient-simulator]
            [ehrt.sim-check.interface :as check]
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
   :history ::history-sentinel
   ;; ARC 3B SWEEP 1 (ADR-0174): `:encounters` joins `config-keys` in the
   ;; SAME change that teaches `run` to read it, and earns a sentinel
   ;; here so the forwarding assertion below is real rather than a
   ;; nil-equals-nil pass. `run-command` forwards it untranslated -- it
   ;; is a bare opt-in flag, not a two-layer value like `:persons` or
   ;; `:modules`.
   :encounters ::encounters-sentinel})

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

;; --- ADR-0165 P2: ONE shared corpus fixture for the population-scale
;; gates. Each gated scenario run is generated ONCE per run of this
;; namespace and read by every gate that needs it, so the coverage gate
;; below can measure the SAME corpora the self-check gates judge
;; without generating a second copy of any of them. The existing gates'
;; assertions moved here verbatim -- only their `let` binding changed,
;; from a `run-command` call to a `corpus` lookup.

(def ^:private gated-runs
  "The population-scale scenario runs this namespace gates. Adding a
  run here adds it to the coverage gate's own population at the same
  moment -- that coupling is the point: a gated run no coverage meter
  reads is exactly the gap ADR-0163's defect fell through.

  ARC 3A PART 4 (ADR-0173 ruling D1's COMMIT 2, 2026-08-26): all four
  now carry `:persons`. The three scenario-driven runs get it from
  their own `config.edn` -- which is the SAME opt-in the scenarios
  themselves take, so a gated corpus and the scenario a reader runs by
  hand cannot disagree about what is in it -- and `:adhd-seed-45`,
  which names no config, carries it in its own opts. `merge-config-file`
  merges the file UNDER the caller's opts, so naming it in both places
  would have been one declaration too many.

  `:count` is the ARRIVAL COUNT in every case, and `:years` is 20 in
  every case; each config carries the measurement behind those two
  numbers."
  [{:id :seed-202-ed-tuesday
    :opts {:seed 202 :patients 100 :churn true
           :config "demos/scenarios/ed-tuesday/config.edn"}}
   {:id :seed-424242-clinic-decade
    :opts {:seed 424242 :patients 200 :reference-date "2026-08-04" :churn true
           :config "demos/scenarios/clinic-decade/config.edn"}}
   {:id :seed-5-clinic-decade
    :opts {:seed 5 :patients 200 :reference-date "2026-08-04" :churn true
           :config "demos/scenarios/clinic-decade/config.edn"}}
   ;; ADR-0165 step 5, the coverage hunt's own result. The three
   ;; scenario runs above produce NEITHER :medication-end NOR
   ;; :care-plan-end -- ADR-0163's drop rule removed the only ones they
   ;; had, which is precisely the hole this gate exists to see. 180
   ;; clinic-decade variations found :medication-end easily and
   ;; :care-plan-end never (bronchitis/asthma reach their CarePlanEnd
   ;; only by the never-written-attribute route ADR-0163 now drops), so
   ;; the covering run comes from a module those scenarios do not
   ;; name. attention_deficit_disorder over TEN patients is the smallest
   ;; run found producing both, PAIRED and self-check clean, in ~25ms:
   ;; one patient whose ADHD care plan and Ritalin order both fall in
   ;; history phase and whose ends both land in horizon -- the DESIGNED
   ;; pre-horizon straddle, so this run also exercises both
   ;; end-invariants' own pre-horizon escape branch at population scale
   ;; rather than only in a scripted fixture.
   ;;
   ;; RE-POINTED from seed 2 to seed 130 by ADR-0171's migration, and the
   ;; reason is a FINDING, not a convenience. The straddle is a
   ;; knife-edge: at seed 2 exactly ONE of the ten patients had it, and
   ;; the stream partition -- which reshuffles which patient walks what
   ;; -- took it away. Post-partition, seed 2 produces ten :registered
   ;; events and nothing else, so BOTH cited-end witnesses below would
   ;; have gone vacuous at once. That is precisely the failure repo
   ;; review 5 predicted for this run (`.agents/plans/2026-08-25-repo-
   ;; review-findings.md`, L1-7: "a reshuffle in adhd-seed-2 takes both
   ;; end types dark at once").
   ;;
   ;; The replacement was found ADR-0165's own way -- a seed sweep under
   ;; the LIVE (post-partition) engine, filtering for runs with at least
   ;; one CITED :medication-end and one CITED :care-plan-end. Seeds
   ;; 0-399 at ten patients yield exactly four: 130, 158, 233, 331, each
   ;; producing the identical 12-event, one-of-each shape seed 2 used to
   ;; have. 130 is the smallest, and is taken for that reason alone.
   ;;
   ;; RE-POINTED AGAIN 2026-08-26, seed 130 -> seed 45, by arc 3a part
   ;; 4's own opt-in, and for EXACTLY the reason the partition re-point
   ;; above records. `:persons` moves where a bound patient's Persona
   ;; comes from -- the `:person` family instead of `:patient`'s
   ;; thirteen draws -- so it reshuffles which patient walks what, and
   ;; the straddle is a knife-edge: measured at seed 130 under
   ;; `:persons {:count 10 :years 20}`, this run produces SIX
   ;; registrations, fourteen `:demographic-update`s and NOT ONE cited
   ;; end. Both counted witnesses below would have gone vacuous at once,
   ;; which is the failure repo review 5 predicted for this run a second
   ;; time (L1-7).
   ;;
   ;; The replacement was found ADR-0165's own way -- a seed sweep under
   ;; the LIVE engine WITH `:persons` on, at the pool size this slot
   ;; actually ships, filtering for runs with at least one CITED
   ;; `:medication-end` and one CITED `:care-plan-end`. Seeds 0-799
   ;; yield FOURTEEN: 45, 114, 237, 305, 313, 392, 429, 484 and six
   ;; beyond the eight printed. 45 is the smallest, and is taken for
   ;; that reason alone -- the same criterion the seed-2 -> seed-130
   ;; re-point above used.
   {:id :adhd-seed-45
    :opts {:seed 45 :patients 10 :reference-date "2026-08-04"
           :module-horizon-days 3650
           :pathway {:name "adhd-only" :steps []}
           :modules ["attention_deficit_disorder"]
           :module-assignment [{:module-id "attention_deficit_disorder" :weight 1}]
           ;; No `:config` to carry it, so the opt-in is here. Twice the
           ;; arrival count and twenty years, for the reasons the two
           ;; scenario configs state at length.
           :persons {:count 20 :years 20}}}])

(def ^:private corpora
  "run id -> that run's own `run-command` result, populated once by
  `generate-corpora-once`."
  (atom {}))

(defn- generate-corpora-once [f]
  (reset! corpora (into {} (map (juxt :id #(run/run-command (:opts %)))) gated-runs))
  (f))

(use-fixtures :once generate-corpora-once)

(defn- corpus
  "The `run-command` result for one gated run -- never a fresh run."
  [id]
  (or (get @corpora id)
      (throw (ex-info "ehrt.sim.run-test: no corpus under this id -- the :once fixture did not populate it"
                      {:id id :have (sort (keys @corpora))}))))

;; --- ADR-0169 (arc 0): THE EQUIVALENCE GATE ------------------------------
;;
;; The definition of "nothing moved", for a refactor that owes an
;; equivalence proof rather than red-before-green. Born green on the
;; UNREFACTORED tree, in its own commit, so the gate is witnessed passing
;; before it has anything to catch -- ADR-0169's own operational clause.
;;
;; TWO gates over the SAME four corpora, deliberately, because they can
;; disagree and the disagreement is itself the finding (ADR-0169 fence F3:
;; a map re-keyed in a different order prints differently but is still
;; `=`; whether key order is part of "byte-identical" is an author
;; ruling, not a session's). A digest alone could not tell those apart,
;; and a value comparison alone could not see them at all.

(def ^:private arc0-baseline-resource
  "run id -> its committed baseline corpus, a resource under this
  component's own test path (the `pinned_seed_42_patients_5.edn` pattern
  ehrt.sim-engine.engine-test/pinned-seed-survives-decide-evolve-refactor
  already established, one scale up: four population-scale corpora
  instead of one 5-patient run). ~812 KB committed in total, disclosed --
  the same weight class as `demos/traces/module-mix/ground-truth.edn`
  (318 KB) and the four synthea census EDNs (300-314 KB each) this repo
  already tracks. The whole VALUE is committed, not merely its digest,
  because the diagnostic this gate owes -- the FIRST DIFFERING EVENT
  INDEX, never a bare 'digest mismatch' -- cannot be computed without it."
  {:seed-202-ed-tuesday       "ehrt/sim/fixtures/arc0_gated_seed_202_ed_tuesday.edn"
   :seed-424242-clinic-decade "ehrt/sim/fixtures/arc0_gated_seed_424242_clinic_decade.edn"
   :seed-5-clinic-decade      "ehrt/sim/fixtures/arc0_gated_seed_5_clinic_decade.edn"
   :adhd-seed-45               "ehrt/sim/fixtures/arc0_gated_adhd_seed_45.edn"})

(def ^:private arc0-pinned-digest
  "run id -> SHA-256 of that run's ground-truth AS THE SHIPPED WRITER
  SERIALISES IT, taken on the unrefactored tree at ADR-0169's own
  baseline commit.

  `(pr-str ground-truth)` is not a serialisation this test invented: it
  is verbatim what BOTH shipped writers emit, and the tree already turns
  on their being the same bytes --
  `ehrt.cli.core/sim-ground-truth-bare-text` (`ehrt sim run --format
  ground-truth`) and `ehrt.corpus.generators/spool-sim-output!`
  (`events.edn`) are each exactly `(pr-str ground-truth)`, which is what
  makes `cat events.edn | ehrt sim check` a working pipe. Digesting what
  the shipped writer writes, never a `pr-str` shape invented for a test,
  is `bin/regression-oracle`'s own idiom (`rulings.md#R-oracle-script-contract`)
  applied one layer in.

  Round-trip verified when pinned: for all four corpora,
  `(= gt (edn/read-string (pr-str gt)))` AND `(= (pr-str gt) (pr-str
  (edn/read-string (pr-str gt))))` -- value AND bytes both survive, so
  the committed baseline is a faithful pin for BOTH gates below.

  RE-PINNED 2026-08-26, all four, by ADR-0173 ruling D1's COMMIT 2 --
  the ONE declared sweep this arc is allowed, and the reason the fold
  landed dark in three prior commits so that this diff would have
  exactly one possible cause. `:persons` is now ON in all four (from
  each scenario's own `config.edn`, and from the adhd run's own opts),
  and the fourth run also changed SEED, 130 -> 45, because the opt-in
  took its two counted cited-end witnesses vacuous -- `gated-runs`
  above carries the sweep that replaced it.

  WHAT MOVED, and it is everything: with `:persons` present a bound
  patient's Persona comes from the `:person` family rather than from
  thirteen `:patient` draws, so every downstream `:patient` draw shifts
  and the whole corpus reshuffles. The counts, before -> after:
  seed-202 393 -> 648 events, seed-424242 343 -> 1,279, seed-5 363 ->
  1,058, adhd 12 -> 66. The growth is the fold: `:demographic-update`,
  `:coverage-change`, hook-created encounters and unidentified
  arrivals, none of which existed in any corpus before this commit."
  {:seed-202-ed-tuesday       "edcc7008125c0f5de24ea8751ce10adbcf6880270d3af5876d5b6ba9e7b779ba"
   :seed-424242-clinic-decade "0f7e38e63c35a12865b46b5b79ada5bb36bb381bb3bbf8279e821b1ff9759d4d"
   :seed-5-clinic-decade      "f754c3b7a346756357f7232016a4f170bf3d716a58cc4b6828c53a51dbf4651d"
   :adhd-seed-45              "36530c9ab0123cd1a349d4f158bd152c00be5b09acca582f6af042a6f45c5950"})

(defn- arc0-baseline
  "The committed baseline ground-truth vector for one gated run."
  [id]
  (edn/read-string (slurp (io/resource (get arc0-baseline-resource id)))))

(defn- first-divergence
  "Where `actual` first departs from `baseline`, as a human-readable
  string naming the EVENT INDEX -- the diagnostic ADR-0169 requires in
  place of a bare digest mismatch. nil when the two are equal.

  Reports a length difference explicitly (a corpus that is a strict
  prefix of the other reshuffled nothing but stopped early, a different
  defect from one that diverged mid-log), and otherwise the first index
  whose event differs, with both events."
  [baseline actual]
  (let [n (min (count baseline) (count actual))
        idx (first (keep-indexed (fn [i b] (when (not= b (nth actual i)) i))
                                 (take n baseline)))]
    (cond
      idx (str "first differing event index " idx " of " (count baseline)
               "\n  baseline: " (pr-str (nth baseline idx))
               "\n  actual:   " (pr-str (nth actual idx)))
      (not= (count baseline) (count actual))
      (str "no event differs in the common prefix, but LENGTHS differ: baseline "
           (count baseline) " events, actual " (count actual)
           " -- first extra event at index " n ": "
           (pr-str (nth (if (> (count actual) n) actual baseline) n)))
      :else nil)))

(deftest arc0-gated-corpora-are-byte-and-value-identical-to-the-pinned-baseline
  (testing "ADR-0169's equivalence gate: NOTHING in arc 0 may move a single
            draw, event, or event ordering. A red here is a STOP, never a
            reason to re-pin (fence F1)."
    (doseq [{:keys [id]} gated-runs]
      (testing (str "gated run " id)
        (let [r (corpus id)
              actual (get-in r [:payload :ground-truth])
              baseline (arc0-baseline id)
              actual-bytes (pr-str actual)
              actual-digest (result/sha256-string actual-bytes)
              pinned-digest (get arc0-pinned-digest id)
              value-identical? (= baseline actual)]
          (is (result/ok? r) (str "the gated run itself failed: " (pr-str (:payload r))))
          (is (seq actual) "the gated run produced an empty ground-truth")
          ;; Gate 1 -- byte identity, against the shipped writer's own bytes.
          (is (= pinned-digest actual-digest)
              (str "BYTE identity broke for " id ".\n"
                   "  pinned sha256: " pinned-digest "\n"
                   "  actual sha256: " actual-digest "\n  "
                   (or (first-divergence baseline actual)
                       "every event is `=` to its baseline -- see the F3 assertion below")))
          ;; Gate 2 -- value identity, against the same committed baseline.
          (is value-identical?
              (str "VALUE identity broke for " id ".\n  "
                   (first-divergence baseline actual)))
          ;; Fence F3 -- the two gates disagreeing is its own finding.
          (is (= value-identical? (= pinned-digest actual-digest))
              (str "STOP-AND-REPORT (ADR-0169 fence F3): the byte gate and the value gate "
                   "DISAGREE for " id ". value-identical? " value-identical?
                   ", digest-identical? " (= pinned-digest actual-digest) ". Every event is "
                   "`=` to its baseline but the shipped writer's bytes changed (or the "
                   "converse) -- e.g. a map re-keyed in a different order. Whether key "
                   "order is part of \"byte-identical\" is an AUTHOR ruling, not this "
                   "session's: report the diff, do not re-pin.")))))))

(def ^:private arrival-shift-gap
  "The `:arrival-gap` the shifted twin of each gated run is driven at --
  a value none of the four gated configs uses (ed-tuesday authors 30,
  clinic-decade 5, the adhd run takes `run`'s own default 60), and a
  LARGER one deliberately: spreading arrivals out lowers bed pressure, so
  the twin cannot end early on `:exhausted` and change which patients
  registered at all."
  97)

(def ^:private arrival-shifted-corpora
  "run id -> the SAME gated run with every arrival moved to a different
  instant, and nothing else changed. `:arrival-gap` feeds
  `rand-int-in world-rng 0 arrival-gap` -- the WORLD family, ADR-0171 --
  so it moves every patient's arrival `:t` while touching no
  `:patient`-family draw at all. That is exactly the perturbation ADR-
  0173 ruling C1's move has to survive.

  A `delay`, not the `:once` fixture, so the cost (one extra generation
  of all four corpora) is paid by the one gate that needs it and is
  legible as that gate's own price."
  (delay (into {} (map (juxt :id #(run/run-command (assoc (:opts %) :arrival-gap arrival-shift-gap))))
               gated-runs)))

(defn- first-demographic-divergence
  "Which patient's `:registered` event first differs between two
  arrival-shifted runs of the same gated config, as a human-readable
  string -- the same diagnostic-over-a-bare-mismatch posture
  `first-divergence` above takes for the arc-0 corpora."
  [a b]
  (if-let [pid (first (filter #(not= (get a %) (get b %)) (keys a)))]
    (str "the compile moved with the arrival time, first at patient " pid
         "\n  at the config's own arrival gap: " (pr-str (get a pid))
         "\n  at arrival gap " arrival-shift-gap ":          " (pr-str (get b pid)))
    (str "no patient's event differs, but the patient SETS do: only-in-base "
         (pr-str (vec (remove (set (keys b)) (keys a)))) ", only-in-shifted "
         (pr-str (vec (remove (set (keys a)) (keys b)))))))

(defn- registered-by-patient-id
  "patient-id -> that patient's `:registered` event, for one run-command
  result."
  [r]
  (into {} (comp (filter #(= :registered (:event %)))
                 (map (fn [ev] [(:patient-id (first (:participants ev))) ev])))
        (get-in r [:payload :ground-truth])))

(deftest every-gated-run-compiles-the-same-persona-at-any-arrival-time
  (testing "ADR-0173 ruling C1, at population scale: what `decide
            :registered` compiles for a patient does not depend on WHEN
            that patient arrives. Move every arrival in a gated run and
            every patient's Persona, and every history-phase fact their
            module walk compiled, must come back byte-identical -- which
            is what licenses `run` to do the compile at run start
            instead. Born green on the unrefactored tree, in the same
            commit that moved the compile (ADR-0169's equivalence-proof
            pattern: a pure refactor owes proof that nothing moved, not
            red-before-green)."
    (let [pairs (for [{:keys [id]} gated-runs]
                  [id (registered-by-patient-id (corpus id))
                   (registered-by-patient-id (get @arrival-shifted-corpora id))])]
      (doseq [[id base shifted] pairs]
        (testing (str "gated run " id)
          (testing "population is non-empty (R-empty-population-is-red)"
            (is (seq base) "the gated run registered nobody at all"))
          (is (= (set (keys base)) (set (keys shifted)))
              (str "the shifted twin of " id " registered a DIFFERENT patient set -- "
                   "the two runs are no longer comparable, and this gate proves nothing "
                   "until that is understood (an early :exhausted is the likely cause)"))
          (testing "the perturbation actually perturbed something"
            (is (not= (mapv (comp :t val) (sort-by key base))
                      (mapv (comp :t val) (sort-by key shifted)))
                (str "arrival instants are IDENTICAL between the two runs of " id
                     " -- :arrival-gap " arrival-shift-gap " moved nothing, so this gate is vacuous")))
          ;; ONE assertion per run, not one per patient -- the shape
          ;; `arc0-gated-corpora-are-byte-and-value-identical-to-the-
          ;; pinned-baseline` above already uses: compare the whole
          ;; thing, and let the failure message name the first patient
          ;; that diverged. 510 patients across the four runs would
          ;; otherwise be 1,020 assertions for one gate.
          ;;
          ;; `:t` is the arrival instant itself and `:warm-up` is derived
          ;; from it; everything else on a `:registered` event is the
          ;; compile's own output.
          (let [strip (fn [m] (into (sorted-map)
                                    (map (fn [[pid ev]] [pid (dissoc ev :t :warm-up)]))
                                    m))
                a (strip base)
                b (strip shifted)]
            (is (= a b) (str id ": " (or (first-demographic-divergence a b) "")))
            (is (= (pr-str a) (pr-str b))
                (str id ": every :registered event is `=` across the two arrival "
                     "schedules but the bytes differ -- a re-keyed map printing in a "
                     "different order. Report it, do not re-pin (ADR-0169 fence F3).")))))
      (testing "the compile half of this gate is not vacuous: at least one
                gated run really does walk a module and compile
                history-phase facts onto its :registered events"
        (is (some (fn [[_ base _]] (some :pre-horizon-facts (vals base))) pairs)
            "not one gated run produced a single :pre-horizon-facts -- this gate is
             testing the persona draw alone, and the module walk is untested here")))))

(def ^:private arc0-pre-partition-digest
  "What each gated corpus digested to BEFORE ADR-0171's stream partition
  -- ADR-0169's own pins, frozen here as the migration boundary and
  never to be edited again.

  `arc0-pinned-digest` above is the LIVE pin and moves when the corpus
  legitimately moves. These four are historical: they are the values the
  live pin held at HEAD 97f22fd, the last commit before the partition,
  and they exist so `gated-corpora-re-pin-exactly-once-under-the-new-
  scheme` can assert that the corpus moved OFF them -- which is how a
  reader tells \"the migration landed\" from \"the migration is still
  pending\" without resolving a git history."
  {:seed-202-ed-tuesday       "584a5f01004ee4228f6a352209798310dd6c2daadb5596ff7316c163ca6db9bd"
   :seed-424242-clinic-decade "793910e0d2d57205093dd100cb27419299e6b8902c09a78613d6df3a3652bf24"
   :seed-5-clinic-decade      "dd0beff7a02bf1e50f498c6f87ab1cc927bd7896786adaa75cbfd5209ccf80a5"
   ;; Keyed by this gated SLOT's current id even though the digest was
   ;; taken when the slot ran seed 2: it is the slot's pre-partition
   ;; value, and the slot is what the test below looks it up by. The
   ;; slot has now been re-pointed TWICE -- seed 2 -> 130 by ADR-0171's
   ;; partition, 130 -> 45 by arc 3a part 4's `:persons` opt-in -- and
   ;; this value is untouched by both, because what it records is what
   ;; the slot digested to BEFORE the partition and nothing since. Both
   ;; re-points, and why each was forced, are disclosed in `gated-runs`
   ;; above.
   :adhd-seed-45             "34f5faf0283d71a79b0e7ded21f3e5cb4515ef844cebd42fd84347be3a6e0b40"})

(deftest gated-corpora-re-pin-exactly-once-under-the-new-scheme
  (testing "ADR-0171 section 3's DETERMINISM CONTINUITY obligation. The
            partition is draw-affecting BY DESIGN and the author's ruling
            F1 bought exactly ONE reshuffle for it: the partition, the
            from = to delay skip, every re-pin and the oracle re-baseline
            land together, because splitting them spends a second.

            `arc0-gated-corpora-are-byte-and-value-identical-to-the-
            pinned-baseline` above still says a red is a STOP and never a
            reason to re-pin. ADR-0171 being Accepted is the one licensed
            exception, and this test is what makes the exception legible
            after the fact: the four pre-partition digests are frozen
            here, so the migration is visible on the test's own face and
            a SECOND unlicensed reshuffle cannot hide behind the first.

            The ADR-0169 F3 tripwire is untouched -- byte gate and value
            gate disagreeing remains its own finding, in its own test."
    (doseq [{:keys [id]} gated-runs]
      (testing (str "gated run " id)
        (let [pre  (get arc0-pre-partition-digest id)
              live (get arc0-pinned-digest id)]
          (is (some? pre) "no pre-partition digest recorded for this gated run")
          (is (some? live) "no live pinned digest for this gated run")
          (is (not= pre live)
              (str "the PIN for " id " is still its pre-partition value (" pre
                   "). Either the partition has not landed, or a re-pin was "
                   "reverted -- ADR-0171 says all 35 oracle roots and all four "
                   "gated corpora must move exactly once."))
          (let [actual (result/sha256-string (pr-str (get-in (corpus id) [:payload :ground-truth])))]
            (is (not= pre actual)
                (str "the LIVE " id " corpus still digests to its pre-partition "
                     "value -- the partition is not reaching this run."))
            (is (= live actual)
                (str "the live pin and the live corpus disagree for " id
                     "; the byte gate above carries the diagnostic."))))))))

(def ^:private arc0-invariant-catalog
  "The 36 invariant names `check-all` reports, in reporting order --
  pinned so the findings assertion below is a full-value `=` rather than
  a check of one key. Catalog drift is itself a change in what \"identical
  findings\" means, so it belongs inside the gate, not outside it.

  RE-PINNED AGAIN 2026-08-26, 35 -> 36, by arc 3b sweep 1 (ADR-0174
  section 2(a)), and disclosed for the same reason the previous re-pin
  is. WHICH INVARIANT MOVED: none of the FINDINGS. `:status` is `:ok` on
  both sides and `:events` is unchanged on all four corpora, and the
  ROSTER moved by three edits that net to one: `admission-only-when-new`
  became `admission-only-when-no-open-encounter`,
  `outpatient-visit-only-when-new` was ABSORBED into it (the two were
  always one rule written twice), and two encounter rows joined --
  `discharge-closes-an-open-encounter` and
  `every-encounter-is-opened-and-closed-or-still-open`, placed beside
  the guard they split from because this catalog is documented as being
  in REPORTING order.

  RE-PINNED 2026-08-26, 29 -> 35, and the re-pin is disclosed because
  this gate's own failure message says a red here is a STOP and not a
  re-pin. WHICH INVARIANT MOVED: none. `:status` is `:ok` on both sides
  and `:events` is unchanged on all four corpora, so not one FINDING
  moved -- what grew is the ROSTER, by exactly the six ADR-0173 section
  2(e) adds (the person-fold family, appended after
  `expired-patient-retains-location` and before the facility catalog,
  which is where `catalog` itself puts them). That is the case this
  def's own docstring anticipates: catalog drift belongs inside the
  gate, so the gate moves with the catalog and keeps asserting a
  full-value `=` rather than being weakened to a subset check."
  '[timestamps-monotone discharge-follows-admission every-event-has-participants
    participant-ids-exist-in-run admission-only-when-no-open-encounter
    discharge-closes-an-open-encounter
    every-encounter-is-opened-and-closed-or-still-open transfer-only-when-admitted
    transfer-from-matches-state no-double-occupancy admitted-occupies-one-slot
    cancel-references-existing-uncancelled-event bed-swap-both-admitted-before-swap
    merge-survivor-absorbs-merged-mrns no-events-after-merged-terminal
    step-rejected-reason-is-documented order-only-when-admitted
    result-references-existing-order-and-follows-it-in-time
    abnormal-flags-consistent-with-value-vs-range registered-is-every-patients-first-event
    registered-persona-is-schema-valid
    outpatient-patients-occupy-no-bed clinical-content-only-when-admitted
    medication-end-references-existing-order-and-follows-it-in-time
    care-plan-end-references-existing-start-and-follows-it-in-time
    expired-patient-retains-location
    identity-fill-references-its-placeholder-registration
    identification-merge-survivor-is-the-persons-prior-patient
    every-placeholder-registration-is-resolved-or-still-open
    demographic-update-reports-a-real-change
    no-demographic-event-after-a-patient-expires
    person-scoped-provenance-is-a-stamp-not-a-reference
    occupancy-within-capacity
    surge-only-when-earlier-rungs-exhausted warm-up-mark-matches-window
    result-analytes-match-order-profile])

(def ^:private arc0-baseline-facility
  "run id -> the facility its corpus was generated under. A scenario
  config declares one only if it means to override the default:
  `demos/scenarios/ed-tuesday` does (its Emergency surge bump),
  `demos/scenarios/clinic-decade` does not, and `adhd-seed-45` names no
  config at all -- so `sim-model/default-facility` is the honest fallback
  for three of the four, not a guess. Measured, not assumed: passing nil
  instead lands `surge-only-when-earlier-rungs-exhausted` on a nil ward
  and dies inside `sim-model/licensed-bed-ids` on the seed-5 corpus,
  which is the only one carrying a surge placement."
  {:seed-202-ed-tuesday       "demos/scenarios/ed-tuesday/config.edn"
   :seed-424242-clinic-decade "demos/scenarios/clinic-decade/config.edn"
   :seed-5-clinic-decade      "demos/scenarios/clinic-decade/config.edn"
   :adhd-seed-45               nil})

(defn- arc0-facility-for [id]
  (or (when-let [path (get arc0-baseline-facility id)]
        (:facility (edn/read-string (slurp path))))
      sim-model/default-facility))

(deftest arc0-check-all-findings-are-identical-on-every-gated-corpus
  (testing "ADR-0169's SECOND equivalence claim: identical FINDINGS, asserted
            by full-value `=` of `check-all`'s own result over each committed
            baseline corpus. Run against the BASELINE rather than a fresh run
            deliberately -- it isolates the check half, so a check-side
            regression cannot hide behind a generate side that also moved."
    (doseq [{:keys [id]} gated-runs]
      (testing (str "gated corpus " id)
        (let [baseline (arc0-baseline id)]
          (is (= {:status :ok
                  :payload {:invariants-checked arc0-invariant-catalog
                            :events (count baseline)}}
                 (check/check-all baseline (arc0-facility-for id) 0))
              (str "check-all's findings over the pinned " id " corpus are no longer "
                   "what they were at ADR-0169's baseline. A red here is a STOP: "
                   "re-derive which invariant moved, do not re-pin.")))))))

(def ^:private reinstating-cancel-fields
  "Which reinstated fields each cancel class carries on the wire.
  `:cancel-admit` carries none -- its decide reads no prior state -- so
  it is absent here rather than mapped to an empty vector."
  {:cancel-transfer  [:home-ward :location]
   :cancel-discharge [:home-ward :location :attending]})

(defn- cancel-reinstatement-mismatches
  "Every emitted reinstating cancel in `ground-truth` whose reinstated
  fields are NOT what `(:before (nth (replay ground-truth) idx))` hands
  back for its own `:cancels-event-id` -- ADR-0169's post-hoc check on
  the fold-carried `:reinstate-index`, recomputed against the `replay`
  the decide no longer calls.

  Post hoc, and deliberately NOT an assertion inside the decide: an
  assertion in the decide would make the run pay the very replay this
  arc removed, so the equivalence would be unfalsifiable in exactly the
  configuration that matters."
  [ground-truth]
  (let [records (engine/replay ground-truth)]
    (for [ev ground-truth
          :let [fields (get reinstating-cancel-fields (:event ev))]
          :when fields
          :let [idx (:cancels-event-id ev)
                before (:before (nth records idx))]
          field fields
          :when (not= (get ev field) (get before field))]
      {:cancel (:event ev) :at (:t ev) :cancels-event-id idx :field field
       :emitted (get ev field) :replay-says (get before field)})))

(deftest cancel-decides-reinstate-exactly-what-replay-would-hand-back
  (testing "ADR-0169 family (ii): the two reinstating cancel decides read
            their prior state from `run`'s fold-carried `:reinstate-index`
            instead of replaying the whole log per cancel. This is the
            equivalence, checked against `replay` itself on every gated
            corpus."
    (doseq [{:keys [id]} gated-runs]
      (testing (str "gated corpus " id)
        (is (= [] (vec (cancel-reinstatement-mismatches (arc0-baseline id)))))))
    (testing "and the check is not vacuous -- FINDING, disclosed: only ONE of
              the four gated corpora carries reinstating cancels at all
              (seed-202: 6 :cancel-transfer + 1 :cancel-discharge = SEVEN; the
              two clinic-decade runs and adhd-seed-45 carry none, and
              seed-202's own 1 :cancel-admit reinstates nothing). RE-COUNTED
              TWICE now, and both times because a licensed reshuffle moved
              this corpus rather than because anything about churn changed:
              TEN (9 + 1, 2 :cancel-admit) before ADR-0171's stream
              partition, NINE (8 + 1) after it, SEVEN after arc 3a part 4's
              `:persons` opt-in. The gated-corpus half of this gate
              therefore rests on seven events in one run;
              `cancel-reinstatement-survives-the-fold-carried-index` in
              engine-test is what carries it at population scale."
      (let [counted (into {} (map (fn [{:keys [id]}]
                                    [id (count (filter #(contains? reinstating-cancel-fields (:event %))
                                                       (arc0-baseline id)))]))
                          gated-runs)]
        (is (= 7 (get counted :seed-202-ed-tuesday))
            (str "seed-202's reinstating-cancel count moved: " (pr-str counted)))
        (is (pos? (reduce + (vals counted)))
            (str "NO gated corpus carries a reinstating cancel -- this gate has "
                 "gone vacuous: " (pr-str counted)))))))

(def ^:private cited-end-resolution
  "Terminal event type -> [the citation key it carries, the opening event
  type it resolves against, the resolved-index key it emits]."
  {:medication-end [:order-citation     :medication-order :order-event-id]
   :care-plan-end  [:care-plan-citation :care-plan-start  :start-event-id]})

(defn- citation-index-mismatches
  "Every cited end event in `ground-truth` whose emitted resolved index is
  NOT what ADR-0164's whole-log scan would have answered -- ADR-0169's
  post-hoc check on the fold-carried `:citation-index`.

  Scanned over the PREFIX `(subvec log 0 idx)`, not the whole log, because
  that is what the decide saw: `decide` reads `(:ground-truth world)`,
  which run.clj's own comment calls a persistent mirror of the log SO
  FAR. A later matching order would change `last`'s answer over the full
  log and change nothing about what the decide could legitimately have
  resolved, so a whole-log recomputation would be a DIFFERENT claim --
  and a wrong one."
  [ground-truth]
  (let [log (vec ground-truth)]
    (for [[idx ev] (map-indexed vector log)
          :let [[citation-key opening-type resolved-key] (get cited-end-resolution (:event ev))]
          :when citation-key
          :let [citation (get ev citation-key)]
          :when citation
          :let [patient-id (:patient-id (first (:participants ev)))
                scanned (last (keep-indexed
                               (fn [i prior]
                                 (when (and (= opening-type (:event prior))
                                            (= citation (:citation prior))
                                            (some #(= patient-id (:patient-id %)) (:participants prior)))
                                   i))
                               (subvec log 0 idx)))]
          :when (not= (get ev resolved-key) scanned)]
      {:end (:event ev) :at (:t ev) :key resolved-key
       :emitted (get ev resolved-key) :scan-says scanned})))

(deftest citation-resolution-matches-the-whole-log-scan
  (testing "ADR-0169 family (iii): the two ADR-0164 decide-time scans are
            retired for a fold-carried [opening-type patient-id citation]
            index. This is the INDEX equality, recomputed post hoc by the
            scan itself, on every gated corpus -- including seed 424242,
            ADR-0163's own run."
    (doseq [{:keys [id]} gated-runs]
      (testing (str "gated corpus " id)
        (is (= [] (vec (citation-index-mismatches (arc0-baseline id)))))))
    (testing "FINDING, disclosed: the gated corpora exercise only the NIL arm.
              Exactly two cited end events exist across all four corpora, both
              in adhd-seed-45, and BOTH resolve to nil -- their opening
              :medication-order and :care-plan-start fall in the history phase
              and never enter the log, which is the designed pre-horizon
              straddle ADR-0165 chose that run for. The two clinic-decade runs
              carry 20 and 21 :medication-order events and NO cited end at
              all. So this gate proves the index does not INVENT a resolution;
              `ehrt.sim-engine.engine-test/citation-index-resolves-exactly-what-
              the-scan-resolved` is what proves it FINDS one."
      (let [ends (for [{:keys [id]} gated-runs
                       ev (arc0-baseline id)
                       :let [[ck _ rk] (get cited-end-resolution (:event ev))]
                       :when (and ck (get ev ck))]
                   [id (:event ev) (get ev rk)])]
        (is (= [[:adhd-seed-45 :care-plan-end nil]
                [:adhd-seed-45 :medication-end nil]]
               (vec ends))
            (str "the gated corpora's cited-end population moved -- re-read the "
                 "disclosure above before adjusting: " (pr-str (vec ends))))))))

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
    (let [r (corpus :seed-202-ed-tuesday)]
      (is (result/ok? r) (str "violations: " (pr-str (:payload r))))
      (is (seq (:ground-truth (:payload r)))))))

;; --- ADR-0163: the seed-424242 unpaired-end failure, at the run level -----

(defn- unpaired-ends
  "Every ground-truth terminal event whose own opening citation is
  absent -- the compiled-unpaired-end shape ADR-0163 fixes one layer
  up, read straight off a real log. `:medication-end` with no
  `:order-citation` is what tripped
  check.clj's own medication-end-references-existing-order-and-follows-
  it-in-time; `:care-plan-end` with no `:care-plan-citation` is the
  declared twin, which no invariant covers, so this gate is the only
  thing that would catch its return."
  [ground-truth]
  (filterv (fn [{:keys [event order-citation care-plan-citation]}]
             (or (and (= :medication-end event) (nil? order-citation))
                 (and (= :care-plan-end event) (nil? care-plan-citation))))
           ground-truth))

(defn- end-events-of
  "Every terminal event of `kind` in `ground-truth` -- the subset that can
  EXHIBIT the unpaired-end failure, as a COUNTABLE population rather
  than a hoped-for one.

  `(empty? (unpaired-ends gt))` is satisfied by a `gt` carrying no
  terminal event at all, and that is not a hypothetical: repo review 5
  measured all four gated corpora and found the two clinic-decade runs
  below carry ZERO `:medication-end` and ZERO `:care-plan-end`, so both
  ADR-0163 gates were green over a candidate population of zero. The two
  gates therefore pin a NON-ZERO count from the one gated run that does
  produce both -- ADR-0169's arc-0 pattern (`cancel-decides-reinstate-
  exactly-what-replay-would-hand-back`'s ten-cancel pin, and
  `citation-resolution-matches-the-whole-log-scan`'s two-event pin) --
  and disclose their own corpus's zero rather than leave it unstated."
  [kind ground-truth]
  (filterv #(= kind (:event %)) ground-truth))

(defn- cited-end-witness
  "The counted witness both ADR-0163 gates rest on: `:adhd-seed-45`'s
  terminal events of `kind`, which carry their opening citation
  (ADR-0165 step 5 chose that run precisely because it produces both
  ends PAIRED, self-check clean, in ~25ms). Returned as a vector so a
  caller can pin its SIZE and then run `unpaired-ends` over it -- an
  emptiness claim made over a population known to be non-empty."
  [kind]
  (end-events-of kind (:ground-truth (:payload (corpus :adhd-seed-45)))))

(deftest clinic-decade-seed-424242-self-checks-clean
  (testing "ADR-0163: the exact reproducing invocation --
            `ehrt corpus generate sim --seed 424242 --patients 200
            --reference-date 2026-08-04 --churn --config
            demos/scenarios/clinic-decade/config.edn` -- exited :error
            :self-check-failed on
            :medication-end-references-existing-order-and-follows-it-in-
            time for PID-000089-c02fd3a8 at t 5629740. That patient's
            UTI walk reached `End UTI Tx` via telemed ->
            referral-to-ambulatory without ever entering
            `uti/abx_tx.json`, so its referenced_by_attribute `UTI_Tx`
            was never written, resolved to nil, and
            ehrt.patient-simulator.compile-trajectory compiled a
            :medication-end step carrying no :order-citation at all.
            Kept at the per-push tier alongside the seed-202 gate
            above: ~13s in a warm JVM against a tracked demo config,
            and the population-scale exercise is the point -- the unit
            tests one layer up pin the drop rule on minimized
            trajectories, while only a real 200-patient decade reaches
            the walk that produced it."
    (let [r (corpus :seed-424242-clinic-decade)]
      (is (result/ok? r) (str "violations: " (pr-str (:payload r))))
      (is (empty? (unpaired-ends (:ground-truth (:payload r)))))
      (testing "and the emptiness above is NOT vacuous by accident -- FINDING,
                disclosed (repo review 5, L1-1): this corpus carries ZERO
                :medication-end events of its own, because ADR-0163's drop
                rule removed the only ones it had. The counted witness is
                :adhd-seed-45's ONE cited :medication-end, pinned here, with
                `unpaired-ends` run over it: the same assertion, over a
                population that could have failed it."
        (let [own (end-events-of :medication-end (:ground-truth (:payload r)))
              witness (cited-end-witness :medication-end)]
          (is (zero? (count own))
              (str "this corpus's :medication-end population moved off the "
                   "disclosed zero -- that is GOOD news, but re-read the "
                   "disclosure and re-point the witness before adjusting: "
                   (pr-str (mapv :t own))))
          (is (= 1 (count witness))
              (str "the counted witness population moved -- :adhd-seed-45 no "
                   "longer produces exactly one :medication-end, so this gate "
                   "is asserting emptiness over nothing: " (pr-str witness)))
          (is (every? :order-citation witness)
              (str "the witness :medication-end lost its :order-citation, so "
                   "the population is no longer a CITED end: " (pr-str witness)))
          (is (empty? (unpaired-ends witness))
              (str "the counted witness itself carries an unpaired end: "
                   (pr-str (unpaired-ends witness)))))))))

(deftest clinic-decade-seed-5-carries-no-unpaired-care-plan-end
  (testing "ADR-0163's control, and the ONLY population-scale exercise of
            the :care-plan-end half. This seed self-checked CLEAN both
            before and after the fix -- no invariant covers
            :care-plan-end -- while its log silently carried TWO
            :care-plan-end events with :care-plan-citation nil
            (PID-000045-03ebff87 at t 3636360, PID-000187-899c715a at t
            27417360). Exit code alone could never have caught that, so
            this gate asserts the shape directly. Both ends had a zero
            preceding gap, hence no compiled :delay and no shared-RNG
            draw removed, so the fix changes this log by exactly those
            two events and nothing else (ADR-0163's own oracle sweep)."
    (let [r (corpus :seed-5-clinic-decade)]
      (is (result/ok? r) (str "violations: " (pr-str (:payload r))))
      (is (empty? (unpaired-ends (:ground-truth (:payload r)))))
      (testing "and the emptiness above is NOT vacuous by accident -- FINDING,
                disclosed (repo review 5, L1-1): the docstring above is now
                HISTORY. It describes what this seed's log carried BEFORE
                ADR-0163; the fixed log carries ZERO :care-plan-end events,
                so the shape assertion has had nothing to judge since the fix
                landed. The counted witness is :adhd-seed-45's ONE cited
                :care-plan-end, pinned here, with `unpaired-ends` run over
                it -- and it is the ONLY population-scale exercise of the
                :care-plan-end half in the suite, which no invariant covers."
        (let [own (end-events-of :care-plan-end (:ground-truth (:payload r)))
              witness (cited-end-witness :care-plan-end)]
          (is (zero? (count own))
              (str "this corpus's :care-plan-end population moved off the "
                   "disclosed zero -- that is GOOD news, but re-read the "
                   "disclosure and re-point the witness before adjusting: "
                   (pr-str (mapv :t own))))
          (is (= 1 (count witness))
              (str "the counted witness population moved -- :adhd-seed-45 no "
                   "longer produces exactly one :care-plan-end, so this gate "
                   "is asserting emptiness over nothing: " (pr-str witness)))
          (is (every? :care-plan-citation witness)
              (str "the witness :care-plan-end lost its :care-plan-citation, "
                   "so the population is no longer a CITED end: "
                   (pr-str witness)))
          (is (empty? (unpaired-ends witness))
              (str "the counted witness itself carries an unpaired end: "
                   (pr-str (unpaired-ends witness)))))))))

;; --- ADR-0165: the generator-side event-type coverage gate ---------------
;; ADR-0160 gave the JUDGE side a coverage gate -- every oracle root is
;; exercised. Nothing measured the GENERATOR side, and that is exactly
;; how ADR-0163's defect stayed invisible: the invariant that caught it
;; was correct all along, but the population the suite ran it over
;; produced zero-to-one :medication-end events, so it had almost
;; nothing to judge. This gate asserts the gated runs above
;; COLLECTIVELY produce every ground-truth event type the modules those
;; scenarios name can actually drive.

(defn- run-module-names
  "The module names a gated run walks: the ones its own opts name, or --
  for a run driven by a tracked scenario file -- the ones that file
  names. Read off the config rather than restated here, so a scenario
  that gains a module gains that module's event types in this gate at
  the same moment."
  [{:keys [opts]}]
  (or (:modules opts)
      (:modules (edn/read-string (slurp (:config opts))))))

(defn- emittable-events-for
  "Every ground-truth event type a gated run's own modules can drive --
  `patient-simulator/emittable-ground-truth-events` over the REAL
  resolved closures, never a restatement of them."
  [gated-run]
  (let [resolved (run/resolve-modules (run-module-names gated-run) {})]
    (when-not (result/ok? resolved)
      (throw (ex-info "ehrt.sim.run-test: could not resolve a gated run's own modules"
                      {:run (:id gated-run) :result resolved})))
    (patient-simulator/emittable-ground-truth-events (:payload resolved))))

(defn- generator-side-event-types
  "The event types a corpus produced FROM COMPILED MODULE CONTENT: every
  ground-truth event carrying a `:citation`.

  The citation is the discriminator, and it is load-bearing rather than
  convenient. `ehrt.sim-engine.engine`'s own `citation-fields` attaches
  `:citation` only when the step the event came from carried one, and
  only `ehrt.patient-simulator.compile-trajectory`'s own `->step`
  functions ever set it -- never a hand-authored `:pathways` step,
  never a churn-injected one. Without this filter, ed-tuesday's five
  scripted ED pathways would satisfy :admission/:discharge coverage
  that no vendored module ever produced, and the gate would be
  measuring the scenario author instead of the generator."
  [result]
  (into #{} (comp (filter :citation) (map :event)) (:ground-truth (:payload result))))

(def ^:private coverage-waivers
  "event type -> the queue row that owns the hole (ADR-0165 P3(a)).
  A waiver is DATA with a roadmap row behind it, never a silent
  narrowing of the gate: the type stays declared emittable, and the
  waiver states who owes the run that would cover it."
  {})

(deftest gated-runs-collectively-produce-every-emittable-event-type
  (testing "ADR-0165: the generator-side coverage meter. Every
            ground-truth event type the gated scenarios' own modules
            can emit must be produced, from compiled module content, by
            at least one gated corpus -- or carry an explicit waiver
            row. A hole here means the suite is running its invariant
            catalog over a population that never exercises that event
            type, which is the shape of ADR-0163's own invisibility."
    (let [emittable (into #{} (mapcat emittable-events-for) gated-runs)
          produced (into #{} (mapcat #(generator-side-event-types (corpus (:id %)))) gated-runs)
          missing (into (sorted-set) (remove (some-fn produced coverage-waivers)) emittable)]
      (is (seq emittable) "the emittable set is empty -- the table walk or the closure resolution is broken")
      (is (empty? missing)
          (str "emittable by the gated scenarios' own modules but produced by NO gated corpus: "
               (pr-str (vec missing))
               " -- add a gated run that produces each, or waive it with a queue row "
               "(ADR-0165 P3(a)). Produced: " (pr-str (vec (sort produced)))
               "; emittable: " (pr-str (vec (sort emittable))))))))

;; --- ADR-0173 ruling D1's COMMIT 2: the wire witnesses -------------------
;;
;; `:persons` is ON in all four gated corpora as of 2026-08-26, and this
;; is what that BUYS. Every witness below is `pos?` and none is pinned
;; (`rulings.md#R-empty-population-is-red`): a pinned count turns a
;; future hazard retune into a false red, while `pos?` catches the
;; failure that matters -- a declared traffic family that no corpus
;; produces. That failure is not hypothetical here. Repo review 5 found
;; it twice, and this very session found it twice more: the
;; identification flow is UNREACHABLE by the coincidence rule ADR-0173
;; section 2(d) states (measured: 9 windows covering 0.018% of a
;; 200-person decade, earliest at day 418, against arrivals that finish
;; inside the first 17 hours), and the occupational-injury hook produces
;; NOTHING at ten person-years per person (measured: zero injuries
;; across 2,000 person-years at seed 424242, because the hazard is
;; conditioned on `:employed` years). Both are why the configs say what
;; they say, and these are the gates that keep them honest.

(defn- witness-counts
  "One gated corpus's part-4 traffic, counted by family."
  [ground-truth]
  (let [of (fn [pred] (count (filter pred ground-truth)))
        hook-admission? (fn [ev] (and (= :admission (:event ev)) (:person-event-id ev)))
        reason-is (fn [prefix]
                    (fn [ev] (and (hook-admission? ev)
                                  (str/starts-with? (str (:reason ev)) prefix))))]
    {:placeholder-registrations
     (of #(and (= :registered (:event %)) (= :placeholder (:identity %))))
     :identity-fills
     (of #(and (= :demographic-update (:event %)) (= :identity-fill (:cause %))))
     :identification-merges
     (of #(and (= :merge (:event %)) (= :identification (:cause %))))
     :unhoused-registrations
     (of #(and (= :registered (:event %)) (= :unhoused (:status (:residence %)))))
     :newborn-registrations (of :mother-patient-id)
     :birth-encounters (of (reason-is "Live birth"))
     :parent-delivery-encounters (of (reason-is "Delivery"))
     :occupational-injury-arrivals (of (reason-is "Occupational injury"))
     :unidentified-arrivals (of (reason-is "Unidentified patient"))
     :demographic-updates (of #(= :demographic-update (:event %)))
     :coverage-changes (of #(= :coverage-change (:event %)))
     :person-stamped-registrations (of #(and (= :registered (:event %)) (:person-id %)))}))

(defn- gated-witness-totals []
  (apply merge-with + (map #(witness-counts (arc0-baseline (:id %))) gated-runs)))

(deftest every-part-4-traffic-family-is-witnessed-across-the-gated-corpora
  (testing "ADR-0173 ruling D1's commit 2: each family `pos?` across the
            opted-in set, so no part of the fold is declared and never
            produced. Asserted over the COMMITTED baselines, which is what
            a reader of this repo actually receives."
    (let [totals (gated-witness-totals)]
      (doseq [family (sort (keys totals))]
        (is (pos? (get totals family))
            (str "no gated corpus produces " family
                 " -- the fold declares traffic no corpus carries. Totals: "
                 (pr-str (into (sorted-map) totals))))))))

(deftest the-witness-families-are-not-all-carried-by-one-corpus
  (testing "FINDING, disclosed rather than left implicit: the families are
            NOT evenly spread, and a reader deciding which corpus to point a
            consumer at needs to know which one carries what. Each corpus's
            own counts are printed by this gate's failure message; what is
            ASSERTED is only that no single corpus is the sole witness for
            EVERY family at once, which is the shape that would make the
            other three corpora decorative."
    (let [per-corpus (into {} (for [{:keys [id]} gated-runs]
                                [id (witness-counts (arc0-baseline id))]))
          families (keys (gated-witness-totals))
          sole (fn [family]
                 (let [carriers (filter #(pos? (get-in per-corpus [% family])) (keys per-corpus))]
                   (when (= 1 (count carriers)) (first carriers))))
          sole-by (into {} (keep (fn [f] (when-let [c (sole f)] [f c]))) families)]
      (is (not= 1 (count (distinct (vals sole-by))))
          (str "every single-carrier family is carried by the SAME corpus, so the "
               "other three witness nothing of their own: "
               (pr-str (into (sorted-map) sole-by))
               " -- per-corpus counts: " (pr-str per-corpus))))))

(deftest an-unhoused-registration-renders-pid-11-absent-on-a-gated-corpus
  (testing "ADR-0173 ruling E1, on a POPULATION-SCALE corpus rather than a
            scripted fixture: a patient with nowhere to live renders an
            EMPTY PID-11, and their Persona still carries the row they last
            lived at -- `sim-model/Persona`'s own `:address` is required and
            non-nilable, which is why the sum lives beside it and not in it."
    (let [unhoused (for [{:keys [id]} gated-runs
                         ev (arc0-baseline id)
                         :when (and (= :registered (:event ev))
                                    (= :unhoused (:status (:residence ev))))]
                     ev)]
      (is (pos? (count unhoused))
          "no gated corpus registers an unhoused patient at all")
      (is (every? #(some? (:address (:persona %))) unhoused)
          "an unhoused registration lost its Persona's address, so the sum is
           standing in for the row instead of beside it")
      (is (every? #(some? (:last-known-address (:residence %))) unhoused)
          "an unhoused registration carries no last-known address -- this corpus's
           unhoused persons all entered the run with nowhere to live, which is
           the `:at-t0` case and not the loss case"))))

(deftest a-placeholder-is-either-resolved-or-was-never-due
  (testing "The identification flow's own closing claim, over the gated
            corpora rather than a fixture: every placeholder registration is
            joined to the person's other record, or filled in place, or
            carries no `:window-close-t` at all -- which is what the engine
            mints for a window nobody lived to see close (the person died
            inside it; `engine/placeholder-registration` carries the
            population-scale failure that found this)."
    (let [rows (for [{:keys [id]} gated-runs
                     :let [gt (arc0-baseline id)
                           resolved (into #{}
                                          (concat
                                           (for [ev gt
                                                 :when (and (= :demographic-update (:event ev))
                                                            (= :identity-fill (:cause ev)))]
                                             (:patient-id (first (:participants ev))))
                                           (for [ev gt
                                                 :when (and (= :merge (:event ev))
                                                            (= :identification (:cause ev)))
                                                 p (:participants ev) :when (= :merged (:role p))]
                                             (:patient-id p))))]
                     ev gt
                     :when (and (= :registered (:event ev)) (= :placeholder (:identity ev)))]
                 {:corpus id
                  :patient-id (:patient-id (first (:participants ev)))
                  :resolved? (contains? resolved (:patient-id (first (:participants ev))))
                  :due? (some? (:window-close-t ev))})]
      (is (pos? (count rows)) "no gated corpus mints a placeholder registration")
      (is (pos? (count (filter :resolved? rows)))
          "no placeholder in any gated corpus is ever resolved -- the flow mints
           unidentified arrivals and never closes one")
      (is (empty? (remove #(or (:resolved? %) (not (:due? %))) rows))
          (str "a placeholder is past its own due close and was never resolved: "
               (pr-str (remove #(or (:resolved? %) (not (:due? %))) rows))))
      (testing "FINDING, disclosed rather than gated: the NEVER-DUE case --
                a person who died inside their own identity window, so no
                resolution was ever emitted for it -- is not exercised by
                these four corpora. It is a rare event at the process's own
                0.004-per-person-year window rate, and gating `pos?` on it at
                population scale would make this test a coin flip that a
                hazard retune could flip. Where it IS exercised is
                `ehrt.sim-engine.persons-test`, on a stream that puts the
                death inside the window by construction. This assertion
                records the current split so a reader knows which clause the
                population-scale half is actually testing."
        (is (= (count rows) (count (filter :due? rows)))
            (str "a placeholder in a gated corpus now carries NO close instant -- "
                 "that is the died-inside-the-window case arriving at population "
                 "scale, which is good news and means the disclosure above is "
                 "stale: re-read it before adjusting. Split: "
                 (pr-str (frequencies (map (juxt :corpus :due?) rows)))))))))
