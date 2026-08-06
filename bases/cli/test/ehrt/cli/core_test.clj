(ns ehrt.cli.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn]
            [clojure.java.io :as io]
            [clojure.string]
            [ehrt.kernel.interface :as result]
            [ehrt.kernel.interface :as artifact]
            [ehrt.judge.interface :as report]
            [ehrt.corpus.interface :as operators]
            [ehrt.corpus.interface :as generators]
            [ehrt.corpus.interface :as generate]
            [ehrt.judge-v2-nist.interface :as gate-v2-nist]
            [ehrt.cli.core :as cli]
            [ehrt.cli.help :as help])
  (:import [java.io File]))

(defn- sample-artifact []
  {:kind :engine :name "synthea" :version "4.0.0"
   :sha256 (apply str (repeat 64 "a"))
   :source "https://example.invalid/synthea.jar"
   :acquired "2026-07-24" :license-status :verified})

(defn- temp-lockfile [artifacts]
  (let [f (File/createTempFile "lockfile" ".edn")]
    (spit f (pr-str {:artifacts artifacts}))
    (.getAbsolutePath f)))

;; ---- exit-code mapping (ADR-0004) ----

(deftest exit-code-mapping-test
  (is (= 0 (cli/result->exit-code (result/ok {}))))
  (is (= 1 (cli/result->exit-code (result/rejected :whatever {}))))
  (is (= 2 (cli/result->exit-code (result/error :whatever {})))))

(deftest report-write-failed-maps-to-exit-2-test
  ;; CLI-2's new member of the error family, asserted additively rather
  ;; than by editing the mapping test above: an unwritable --report path
  ;; is an operational failure, so it lands on 2 through ADR-0004's
  ;; generic mapping -- not on a rejection's 1, and not on ADR-0010's
  ;; :gate-no-verdict arm at 3.
  (let [r (result/error :report-write-failed {:path "x" :message "m"})]
    (is (= 2 (cli/result->exit-code r)))
    (is (result/valid? r))))

;; ---- arg parsing ----

(deftest parse-splits-positional-and-opts-test
  (let [{:keys [args opts]} (cli/parse ["corpus" "generate" "--seed" "42" "--population" "100" "--json"])]
    (is (= ["corpus" "generate"] args))
    (is (= 42 (:seed opts)))
    (is (= 100 (:population opts)))
    (is (true? (:json opts)))))

(deftest parse-defaults-json-false-test
  (let [{:keys [opts]} (cli/parse ["artifact" "fetch" "--name" "synthea" "--version" "4.0.0"])]
    (is (= "synthea" (:name opts)))
    (is (not (:json opts))))) ; nil is falsy -- --json wasn't supplied

(deftest parse-keeps-numeric-looking-strings-as-strings-test
  ;; --version and --reference-date look numeric but must never be
  ;; auto-coerced -- "4.0.0"/"20260101" are identifiers, not numbers, and
  ;; a coerced long breaks java.lang.ProcessBuilder's String[] args.
  (let [{:keys [opts]} (cli/parse ["corpus" "generate" "--reference-date" "20260101"])]
    (is (= "20260101" (:reference-date opts)))
    (is (string? (:reference-date opts))))
  (let [{:keys [opts]} (cli/parse ["artifact" "fetch" "--version" "4.0.0"])]
    (is (= "4.0.0" (:version opts)))))

(deftest parse-maps-config-flag-to-config-path-test
  ;; corpus.generate's option key is :config-path; the CLI flag is
  ;; --config-path so the two line up without a renaming layer in dispatch.
  (let [{:keys [opts]} (cli/parse ["corpus" "generate" "--config-path" "config/synthea/synthea.properties"])]
    (is (= "config/synthea/synthea.properties" (:config-path opts)))))

;; ---- dispatch ----

(deftest dispatch-unknown-command-test
  (let [r (cli/dispatch ["bogus" "thing"] {} {})]
    (is (result/error? r))
    (is (= :unknown-command (:category r)))))

;; ---- honest errors (DOC-1 Step 4): the enumerable-options family
;; names its valid options plus a help pointer, bounded to
;; unknown-command (group and action) and mutate-command's own
;; :unknown-operator -- category unchanged in every case. ----

(deftest dispatch-unknown-group-names-the-valid-groups-test
  (let [r (cli/dispatch ["bogus" "thing"] {} {})]
    (is (= :unknown-command (:category r)) "category survives the payload extension")
    (is (= #{"artifact" "corpus" "gate" "check" "version" "doctor" "sim" "show" "play"} (set (:valid-options (:payload r)))))
    (is (= "run: ehrt help" (:hint (:payload r))))))

(deftest dispatch-unknown-artifact-action-names-fetch-and-resolve-test
  (let [r (cli/dispatch ["artifact" "bogus"] {} {})]
    (is (= :unknown-command (:category r)))
    (is (= #{"fetch" "resolve"} (set (:valid-options (:payload r)))))))

(deftest dispatch-unknown-corpus-action-names-its-verbs-test
  (let [r (cli/dispatch ["corpus" "bogus"] {} {})]
    (is (= :unknown-command (:category r)))
    (is (= #{"generate" "mutate" "intake" "operators"} (set (:valid-options (:payload r)))))))

(deftest dispatch-unknown-verb-in-a-real-group-hints-that-groups-own-help-test
  ;; B-6/D-3 (ux fixes 2, ADR-0060): args = ["sim"] -- "sim" is a real
  ;; group missing its verb, not a genuinely-unrecognized token, so the
  ;; hint should point at `ehrt help sim`, not the generic top-level
  ;; listing (compare dispatch-unknown-group-names-the-valid-groups-test
  ;; above, "bogus" isn't a real group and keeps the generic hint).
  (let [r (cli/dispatch ["sim"] {} {})]
    (is (= :unknown-command (:category r)))
    (is (= "run: ehrt help sim" (:hint (:payload r))))))

(deftest dispatch-unrecognized-gate-action-is-sniffed-as-a-path-test
  ;; D11: an action that isn't "v2"/"fhir" is no longer necessarily an
  ;; unknown-command error -- it's sniff-dispatched as a candidate PATH
  ;; first (matching the bare `ehrt gate PATH` contract); "bogus" doesn't
  ;; exist as a path either, so this surfaces as :gate-path-not-found,
  ;; not the old :unknown-command shape.
  (let [r (cli/dispatch ["gate" "bogus"] {} {})]
    (is (= :gate-path-not-found (:category r)))))

;; ---- unknown flags (ux fixes 3, AR-U3-1/2/3/4, `notes/adr/0061-ux-
;; fixes-3.md`): C-4's founding-adjacent defect -- a typo'd flag used to
;; be silently absorbed into :opts and echoed back as if intended. Every
;; flag token now has to be declared for its verb, or the command
;; rejects it by name. ----

(deftest dispatch-unknown-flag-is-rejected-by-name-test
  ;; AR-U3-4a: the C-4 case itself -- `--patiens 200` used to succeed
  ;; silently, absorbed into :opts with `--patients`'s own default
  ;; quietly kept. Now named, exit 2, with a suggestion.
  (let [r (cli/dispatch ["sim" "run"] {:patiens 200 :seed 1}
                         {:sim-run-fn (constantly (result/ok {}))})]
    (is (= :error (:status r)))
    (is (= :unknown-flag (:category r)))
    (is (= "--patiens" (:flag (:payload r))))
    (is (= "sim run" (:verb (:payload r))))
    (is (= "--patients" (:did-you-mean (:payload r))))
    (is (= 2 (cli/result->exit-code r)))))

(deftest dispatch-unknown-flag-with-no-near-match-has-no-did-you-mean-test
  ;; AR-U3-4b: nothing declared for `sim run` is within Levenshtein
  ;; distance 2 of this token -- :did-you-mean must be absent entirely,
  ;; not present-and-nil.
  (let [r (cli/dispatch ["sim" "run"] {:completely-unrelated-nonsense true :seed 1}
                         {:sim-run-fn (constantly (result/ok {}))})]
    (is (= :unknown-flag (:category r)))
    (is (= "--completely-unrelated-nonsense" (:flag (:payload r))))
    (is (not (contains? (:payload r) :did-you-mean)))))

(deftest dispatch-every-declared-flag-of-every-verb-parses-without-unknown-flag-test
  ;; AR-U3-4c, the acceptance property: spec-derived, so a future flag
  ;; added to help/cli-spec is automatically covered -- iterate every
  ;; [group verb] pair help/command-pairs enumerates, and every flag
  ;; that pair's own spec entry declares (independently read straight
  ;; off the raw spec structure here, not via the validator's own
  ;; derivation helper -- otherwise a bug in that helper could never be
  ;; caught by this test). Every -fn is stubbed to a no-op ok, so only
  ;; the flag validator's own accept/reject behavior is under test.
  (let [stub-fns (into {} (map (fn [k] [k (constantly (result/ok {}))]))
                        [:fetch-fn :fetch-all-fn :resolve-fn :generate-fn :generate-sim-fn
                         :mutate-fn :intake-fn :operators-fn :gate-v2-fn :gate-fhir-fn
                         :gate-v2-nist-fn :check-fn :version-fn :doctor-fn :sim-run-fn
                         :sim-check-fn :sim-identifiers-fn :sim-version-fn :show-fn :play-fn])]
    (doseq [[group verb] (help/command-pairs help/cli-spec)
            :let [g (help/find-group help/cli-spec group)
                  declared (if verb
                             (:flags (first (filter #(= verb (:verb %)) (:verbs g))))
                             (:flags g))]
            {:keys [flag]} declared]
      (let [flag-kw (keyword (subs flag 2))
            args (if verb [group verb] [group])
            r (cli/dispatch args {flag-kw "x"} stub-fns)]
        (is (not= :unknown-flag (:category r))
            (str group " " verb " " flag " was rejected as unknown"))))))

;; ---- help / --help / bare ehrt (DOC-1 Step 2): a :category :cli-help
;; result short-circuits before any capability -fn runs. ----

(deftest dispatch-help-verb-alone-returns-top-level-usage-test
  (let [r (cli/dispatch ["help"] {} {})]
    (is (result/ok? r))
    (is (= :cli-help (:category r)))
    (is (clojure.string/includes? (:text (:payload r)) "Usage:"))))

(deftest dispatch-help-verb-with-group-returns-group-usage-test
  (let [r (cli/dispatch ["help" "gate"] {} {})]
    (is (result/ok? r))
    (is (= :cli-help (:category r)))
    (is (clojure.string/includes? (:text (:payload r)) "gate"))))

(deftest dispatch-help-verb-with-unknown-group-falls-back-to-top-level-test
  (let [r (cli/dispatch ["help" "bogus"] {} {})]
    (is (result/ok? r))
    (is (clojure.string/includes? (:text (:payload r)) "Usage:"))))

(deftest dispatch-double-dash-help-short-circuits-before-command-runs-test
  (let [called (atom false)
        r (cli/dispatch ["gate" "v2"] {:help true}
                         {:gate-v2-fn (fn [_opts] (reset! called true) (result/ok {}))})]
    (is (not @called) "the verb's own command fn must not run when --help is given")
    (is (result/ok? r))
    (is (= :cli-help (:category r)))
    (is (clojure.string/includes? (:text (:payload r)) "gate"))))

(deftest dispatch-double-dash-help-with-no-group-returns-top-level-usage-test
  (let [r (cli/dispatch [] {:help true} {})]
    (is (result/ok? r))
    (is (= :cli-help (:category r)))
    (is (clojure.string/includes? (:text (:payload r)) "Usage:"))))

(deftest dispatch-bare-invocation-succeeds-with-usage-text-test
  ;; B-5 (ux fixes 2, ADR-0060, author-ruled 2026-08-06): bare
  ;; invocation now matches help/--help's own exit-0 convention -- same
  ;; text, same :category :cli-help, but result/ok now, not
  ;; result/error (previously dispatch-bare-invocation-is-an-error-
  ;; with-usage-text-test asserted exit 2 here).
  (let [r (cli/dispatch nil {} {})]
    (is (result/ok? r))
    (is (= :cli-help (:category r)))
    (is (= 0 (cli/result->exit-code r)))
    (is (clojure.string/includes? (:text (:payload r)) "Usage:"))))

(deftest dispatch-routes-artifact-fetch-test
  (let [called (atom nil)
        r (cli/dispatch ["artifact" "fetch"] {:name "synthea" :version "4.0.0"}
                         {:fetch-fn (fn [opts] (reset! called opts) (result/ok {:cached true}))})]
    (is (result/ok? r))
    (is (= {:name "synthea" :version "4.0.0"} @called))))

(deftest dispatch-routes-artifact-fetch-all-test
  (let [fetch-called (atom false) fetch-all-called (atom nil)
        r (cli/dispatch ["artifact" "fetch"] {:all true}
                         {:fetch-fn (fn [_] (reset! fetch-called true) (result/ok {}))
                          :fetch-all-fn (fn [opts] (reset! fetch-all-called opts) (result/ok {:results []}))})]
    (is (result/ok? r))
    (is (= {:all true} @fetch-all-called))
    (is (not @fetch-called) "must not also call the single-artifact fetch")))

(deftest dispatch-routes-version-test
  (let [called (atom nil)
        r (cli/dispatch ["version"] {}
                         {:version-fn (fn [opts] (reset! called opts) (result/ok {:identity "pre-release"}))})]
    (is (result/ok? r))
    (is (some? @called))))

(deftest dispatch-routes-doctor-test
  (let [called (atom nil)
        r (cli/dispatch ["doctor"] {}
                         {:doctor-fn (fn [opts] (reset! called opts) (result/ok {:checks []}))})]
    (is (result/ok? r))
    (is (some? @called))))

(deftest dispatch-routes-artifact-resolve-test
  (let [called (atom nil)
        r (cli/dispatch ["artifact" "resolve"] {:name "synthea" :version "4.0.0"}
                         {:resolve-fn (fn [opts] (reset! called opts) (result/rejected :not-cached {}))})]
    (is (result/rejected? r))
    (is (= {:name "synthea" :version "4.0.0"} @called))))

(deftest dispatch-routes-corpus-generate-test
  ;; ADR-0015 amendment (2026-07-30, cold-start UX session): bare
  ;; `corpus generate` now routes to the sim lane, not synthea.
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "generate"] {:seed 1}
                         {:generate-sim-fn (fn [opts] (reset! called opts) (result/ok {:out-dir "x"}))})]
    (is (result/ok? r))
    (is (= {:seed 1} @called))))

;; ---- ADR-0015: `corpus generate` grows source subcommands. The third
;; positional slot (occupied by PATH for mutate/intake) is the source
;; discriminator here instead -- `corpus generate synthea` stays wired
;; to :generate-fn (generate!, unchanged); `corpus generate sim` is its
;; own :generate-sim-fn injection point. ADR-0015 amendment (2026-07-30,
;; cold-start UX session, notes/ADRs.md ADR-0015's own amendment
;; paragraph): bare `corpus generate` (path nil) now means sim, not
;; synthea -- sim needs no fetched artifacts, so the cold first command
;; succeeds unfetched. ----

(deftest dispatch-corpus-generate-bare-and-explicit-sim-route-identically-test
  ;; Pins bare-generate compatibility under the amendment: both
  ;; spellings hit the exact same injection point (:generate-sim-fn),
  ;; byte-identical (generate-sim-command-same-seed-is-byte-identical-test
  ;; below already proves the sim lane itself is byte-reproducible for a
  ;; given seed -- this pins that bare and `generate sim` are the SAME
  ;; call, not merely two byte-identical ones).
  (doseq [args [["corpus" "generate"] ["corpus" "generate" "sim"]]]
    (let [called (atom nil)
          r (cli/dispatch args {:seed 7}
                           {:generate-sim-fn (fn [opts] (reset! called opts) (result/ok {:out-dir "x"}))})]
      (is (result/ok? r))
      (is (= {:seed 7} @called) (str args " must route through :generate-sim-fn")))))

(deftest dispatch-routes-corpus-generate-sim-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "generate" "sim"] {:patients 3}
                         {:generate-sim-fn (fn [opts] (reset! called opts) (result/ok {:out-dir "x"}))})]
    (is (result/ok? r))
    (is (= {:patients 3} @called))))

(deftest dispatch-corpus-generate-unknown-source-names-synthea-and-sim-test
  (let [r (cli/dispatch ["corpus" "generate" "bogus"] {} {})]
    (is (= :unknown-command (:category r)))
    (is (= #{"synthea" "sim"} (set (:valid-options (:payload r)))))))

;; ---- generate-sim-command: real, hermetic (sim is in-process,
;; ADR-0005 -- no subprocess, no network), matching the house
;; convention of small real invocations over injected fakes when the
;; real thing is already this cheap. temp-dir* is defined further down
;; this file; declared here so the compiler accepts the forward
;; reference (deftest bodies don't run until the whole namespace has
;; already loaded, by which point the real def exists). ----

(declare temp-dir*)

(deftest generate-sim-command-zero-flag-defaults-writes-a-v2-corpus-test
  (let [out-dir (str (temp-dir*) "/fresh")
        r (cli/generate-sim-command {:out-dir out-dir})]
    (is (result/ok? r))
    (is (.exists (io/file out-dir "manifest.edn")))
    (is (.exists (io/file out-dir "msg-000.hl7")) "zero-flag :emit \"hl7\" default produces a v2 corpus")))

(deftest generate-sim-command-same-seed-is-byte-identical-test
  (let [dir-a (str (temp-dir*) "/a") dir-b (str (temp-dir*) "/b")
        opts {:seed 99 :patients 2 :emit "hl7"}]
    (is (result/ok? (cli/generate-sim-command (assoc opts :out-dir dir-a))))
    (is (result/ok? (cli/generate-sim-command (assoc opts :out-dir dir-b))))
    (is (= (slurp (io/file dir-a "msg-000.hl7")) (slurp (io/file dir-b "msg-000.hl7"))))
    (is (= (slurp (io/file dir-a "msg-001.hl7")) (slurp (io/file dir-b "msg-001.hl7"))))))

(deftest generate-sim-command-maps-flags-onto-registry-params-test
  ;; :patients 3 -> 6 messages (2 per patient, matching the emitter's
  ;; own fixture behavior already exercised elsewhere in this suite);
  ;; the point under test is that the CLI's own --patients flag reaches
  ;; the registry's :sim entry at all, not the emitter's own arithmetic.
  (let [out-dir (str (temp-dir*) "/fresh")
        r (cli/generate-sim-command {:seed 5 :patients 3 :churn false :emit "hl7" :out-dir out-dir})]
    (is (result/ok? r))
    (is (.exists (io/file out-dir "msg-005.hl7")) "3 patients' worth of messages actually landed")))

(deftest generate-sim-command-rejects-existing-nonempty-out-dir-test
  (let [out-dir (temp-dir*)
        _ (spit (io/file out-dir "stale.txt") "x")
        r (cli/generate-sim-command {:seed 1 :patients 1 :out-dir out-dir})]
    (is (result/error? r))
    (is (= :out-dir-exists (:category r)))))

(deftest generate-sim-command-default-out-dir-matches-registrys-own-out-dir-fn-test
  (let [entry (generators/generator-lookup :sim)
        params-result (generators/generator-resolve-params :sim {:seed 3 :patients 4})
        expected-out-dir ((:out-dir-fn entry) (:payload params-result))]
    (is (= "out/corpus/sim-s3-p4" expected-out-dir))))

(deftest dispatch-routes-corpus-mutate-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "mutate"] {:path "x.json"}
                         {:mutate-fn (fn [opts] (reset! called opts) (result/ok {:count 0}))})]
    (is (result/ok? r))
    (is (= {:path "x.json"} @called))))

;; ---- ehrt sim check / identifiers / version (P3-6 parity mount,
;; 2026-08-01) -- real, hermetic invocations, same house convention
;; generate-sim-command's own tests above already follow (sim runs
;; in-process, ADR-0005, so a real run is as cheap as a fake one). ----

(deftest dispatch-routes-sim-check-test
  (let [called (atom nil)
        r (cli/dispatch ["sim" "check"] {}
                         {:sim-check-fn (fn [opts] (reset! called opts) (result/ok {}))})]
    (is (result/ok? r))
    (is (= {} @called))))

(deftest dispatch-routes-sim-identifiers-test
  (let [called (atom nil)
        r (cli/dispatch ["sim" "identifiers"] {:seed 1}
                         {:sim-identifiers-fn (fn [opts] (reset! called opts) (result/ok {}))})]
    (is (result/ok? r))
    (is (= {:seed 1} @called))))

(deftest dispatch-routes-sim-version-test
  (let [called (atom nil)
        r (cli/dispatch ["sim" "version"] {}
                         {:sim-version-fn (fn [opts] (reset! called opts) (result/ok {}))})]
    (is (result/ok? r))
    (is (= {} @called))))

(deftest dispatch-sim-unknown-verb-names-run-check-identifiers-version-test
  (let [r (cli/dispatch ["sim" "explode"] {} {})]
    (is (= :unknown-command (:category r)))
    (is (= #{"run" "check" "identifiers" "version"} (set (:valid-options (:payload r)))))))

(deftest sim-check-command-runs-invariant-catalog-over-stdin-test
  (let [run-result (cli/sim-run-command {:seed 1 :patients 1})
        ground-truth (:ground-truth (:payload run-result))]
    (with-in-str (pr-str ground-truth)
      (is (result/ok? (cli/sim-check-command {}))))))

(deftest sim-check-command-catches-a-planted-violation-test
  (let [bad [{:event :discharge :t 0 :participants [{:patient-id "P1" :role :subject}]}
             {:event :admission :t 5 :participants [{:patient-id "P1" :role :subject}] :location "Renal"}]]
    (with-in-str (pr-str bad)
      (let [r (cli/sim-check-command {})]
        (is (result/rejected? r))
        (is (= :invariant-violation (:category r)))))))

(deftest sim-check-command-empty-stdin-is-a-named-rejection-test
  (with-in-str ""
    (let [r (cli/sim-check-command {})]
      (is (result/rejected? r))
      (is (= :empty-input (:category r))))))

(deftest sim-check-command-unreadable-stdin-is-a-named-rejection-test
  (with-in-str "]"
    (let [r (cli/sim-check-command {})]
      (is (result/rejected? r))
      (is (= :unreadable-input (:category r))))))

(deftest sim-check-command-non-vector-stdin-is-a-named-rejection-test
  (with-in-str "{:not :a-vector}"
    (let [r (cli/sim-check-command {})]
      (is (result/rejected? r))
      (is (= :malformed-input (:category r))))))

(deftest sim-identifiers-command-returns-the-complete-inventory-test
  (let [r (cli/sim-identifiers-command {:seed 1 :patients 1})]
    (is (result/ok? r))
    (doseq [k [:run-id :patient-ids :mrns :visit-beds :control-ids :fhir-resource-ids :provider-npis]]
      (is (contains? (:payload r) k)))))

(deftest sim-version-command-reports-version-and-git-sha-test
  (let [r (cli/sim-version-command {})]
    (is (result/ok? r))
    (is (string? (:version (:payload r))))
    (is (contains? (:payload r) :git-sha))))

;; ---- ehrt sim run --format (P3-6 parity mount, 2026-08-01): bare
;; er7/ground-truth stdout, mounted via :bare-text metadata (main!'s
;; own precedence) rather than :payload -- see sim-run-command's own
;; docstring for why exit-code computation stays unaffected. ----

(deftest sim-run-command-format-er7-requires-emit-hl7-test
  (let [r (cli/sim-run-command {:seed 1 :format "er7"})]
    (is (result/rejected? r))
    (is (= :format-er7-requires-emit-hl7 (:category r)))))

(deftest sim-run-command-format-er7-bare-text-is-messages-joined-test
  (let [r (cli/sim-run-command {:seed 42 :patients 2 :emit "hl7" :format "er7"})]
    (is (result/ok? r))
    (is (= (clojure.string/join "\n\n" (:messages (:payload r))) (:bare-text (meta r))))))

(deftest sim-run-command-format-ground-truth-bare-text-round-trips-test
  (let [r (cli/sim-run-command {:seed 42 :patients 3 :format "ground-truth"})]
    (is (result/ok? r))
    (is (= (:ground-truth (:payload r)) (clojure.edn/read-string (:bare-text (meta r)))))))

(deftest sim-run-command-format-edn-and-json-carry-no-bare-text-test
  (doseq [format [nil "edn" "json"]]
    (let [r (cli/sim-run-command (cond-> {:seed 1} format (assoc :format format)))]
      (is (nil? (:bare-text (meta r)))))))

(deftest run-then-check-pipe-round-trips-through-bare-text-test
  (testing "the real gap --format ground-truth exists to close: `ehrt sim
            run --format ground-truth | ehrt sim check`, proven at the
            sim-run-command/sim-check-command boundary (main! itself
            already proven to honor :bare-text, format-er7/ground-truth
            tests above)"
    (let [run-result (cli/sim-run-command {:seed 9 :patients 4 :churn true :format "ground-truth"})]
      (with-in-str (:bare-text (meta run-result))
        (is (result/ok? (cli/sim-check-command {})))))))

(deftest main!-sim-run-format-ground-truth-prints-bare-text-and-exits-per-result-test
  (let [printed (atom []) exited (atom nil)]
    (cli/main! ["sim" "run" "--seed" "1" "--format" "ground-truth"]
               {:println-fn #(swap! printed conj %) :exit-fn #(reset! exited %)})
    (is (= 0 @exited))
    (is (= 1 (count @printed)))
    (is (vector? (clojure.edn/read-string (first @printed))))))

(deftest main!-sim-run-format-er7-without-emit-hl7-exits-1-with-normal-rendering-test
  (let [printed (atom []) exited (atom nil)]
    (cli/main! ["sim" "run" "--seed" "1" "--format" "er7"]
               {:println-fn #(swap! printed conj %) :exit-fn #(reset! exited %)})
    (is (= 1 @exited))
    (is (= :format-er7-requires-emit-hl7 (:category (clojure.edn/read-string (first @printed)))))))

;; ---- ehrt sim run: --arrival-gap/--at numeric coercion (P3-6 parity
;; mount, 2026-08-01) -- both had no :coerce entry before this, so
;; either flag arrived as an uncoerced string; a real run.clj/emit-
;; state consumer needs a long. ----

(deftest parse-coerces-arrival-gap-and-at-to-longs-test
  (let [{:keys [opts]} (cli/parse ["sim" "run" "--seed" "1" "--arrival-gap" "30" "--at" "120"])]
    (is (= 30 (:arrival-gap opts)))
    (is (= 120 (:at opts)))))

(deftest sim-run-command-honors-utc-offset-test
  (let [r (cli/sim-run-command {:seed 1 :patients 1 :emit "fhir" :utc-offset "-05:00"})]
    (is (result/ok? r))))

(deftest dispatch-corpus-mutate-accepts-a-positional-path-test
  ;; D10: `ehrt corpus mutate PATH` -- PATH is the third positional arg,
  ;; with --path as its explicit twin, same convention gate already has.
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "mutate" "x.json"] {}
                         {:mutate-fn (fn [opts] (reset! called opts) (result/ok {:count 0}))})]
    (is (result/ok? r))
    (is (= "x.json" (:path @called)))))

(deftest dispatch-corpus-mutate-explicit-path-opt-not-overridden-by-positional-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "mutate" "positional.json"] {:path "explicit.json"}
                         {:mutate-fn (fn [opts] (reset! called opts) (result/ok {:count 0}))})]
    (is (result/ok? r))
    (is (= "explicit.json" (:path @called)))))

;; ---- ruling 7 (SS-1 Step 6): PATH/--out-dir/--out may also spell a
;; dir:/file: URL designator, resolved to the same plain path a bare
;; spelling would give -- additive, bare paths keep working unchanged
;; (covered above by every -accepts-a-positional-path-/-explicit-path-
;; opt- test already passing with plain strings). ----

(deftest dispatch-corpus-mutate-accepts-a-dir-url-positional-path-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "mutate" "dir:./some-dir"] {}
                         {:mutate-fn (fn [opts] (reset! called opts) (result/ok {:count 0}))})]
    (is (result/ok? r))
    (is (= "./some-dir" (:path @called)))))

(deftest dispatch-corpus-mutate-accepts-a-file-url-explicit-path-opt-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "mutate"] {:path "file:./x.json"}
                         {:mutate-fn (fn [opts] (reset! called opts) (result/ok {:count 0}))})]
    (is (result/ok? r))
    (is (= "./x.json" (:path @called)))))

(deftest dispatch-corpus-mutate-accepts-a-dir-url-out-dir-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "mutate" "x.json"] {:out-dir "dir:./mutants-out"}
                         {:mutate-fn (fn [opts] (reset! called opts) (result/ok {:count 0}))})]
    (is (result/ok? r))
    (is (= "./mutants-out" (:out-dir @called)))))

(deftest dispatch-corpus-generate-accepts-a-dir-url-out-dir-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "generate"] {:out-dir "dir:./corpus-out"}
                         {:generate-sim-fn (fn [opts] (reset! called opts) (result/ok {:out-dir "x"}))})]
    (is (result/ok? r))
    (is (= "./corpus-out" (:out-dir @called)))))

(deftest dispatch-corpus-intake-accepts-a-dir-url-positional-path-and-out-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "intake" "dir:./src"] {:out "dir:./catalog-out"}
                         {:intake-fn (fn [opts] (reset! called opts) (result/ok {:catalog []}))})]
    (is (result/ok? r))
    (is (= "./src" (:path @called)))
    (is (= "./catalog-out" (:out @called)))))

(deftest dispatch-gate-accepts-a-file-url-positional-path-test
  (let [called (atom nil)
        r (cli/dispatch ["gate" "v2" "file:./one.hl7"] {}
                         {:gate-v2-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= "./one.hl7" (:path @called)))))

(deftest dispatch-check-accepts-a-dir-url-positional-path-test
  (let [called (atom nil)
        r (cli/dispatch ["check" "dir:./candidate"] {}
                         {:check-fn (fn [opts] (reset! called opts) (result/ok {:results []}))})]
    (is (result/ok? r))
    (is (= "./candidate" (:path @called)))))

(deftest dispatch-gate-bare-sniff-accepts-a-dir-url-positional-path-test
  (let [called (atom nil)
        r (cli/dispatch ["gate" "dir:./mixed-corpus"] {}
                         {:gate-v2-fn (fn [opts] (reset! called [:v2 opts]) (result/ok {:totals {}}))
                          :gate-fhir-fn (fn [opts] (reset! called [:fhir opts]) (result/ok {:totals {}}))})]
    ;; sniff-gate-command dispatches by content, not scheme -- a
    ;; nonexistent directory (this test never creates one) yields
    ;; :gate-path-not-found, which is fine here: the point under test
    ;; is that the URL's :path ("./mixed-corpus") is what reaches
    ;; sniff-gate-command at all, not which format it sniffs to.
    ;; Confirmed via the error payload's own :path field, since neither
    ;; fn above gets called in this case.
    (is (result/error? r))
    (is (= :gate-path-not-found (:category r)))
    (is (= "./mixed-corpus" (:path (:payload r))))))

(deftest dispatch-routes-gate-v2-test
  (let [called (atom nil)
        r (cli/dispatch ["gate" "v2"] {:path "components/corpus/test-fixtures/v2"}
                         {:gate-v2-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= {:path "components/corpus/test-fixtures/v2"} @called))))

(deftest dispatch-routes-gate-fhir-test
  (let [called (atom nil)
        r (cli/dispatch ["gate" "fhir"] {:path "some-corpus/"}
                         {:gate-fhir-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= {:path "some-corpus/"} @called))))

(deftest dispatch-gate-accepts-a-positional-path-test
  ;; `ehrt gate v2 PATH` -- PATH is the third positional arg, not a
  ;; --path flag, matching the CLI contract as specified.
  (let [called (atom nil)
        r (cli/dispatch ["gate" "v2" "components/corpus/test-fixtures/v2"] {}
                         {:gate-v2-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= "components/corpus/test-fixtures/v2" (:path @called)))))

(deftest dispatch-gate-explicit-path-opt-not-overridden-by-positional-test
  (let [called (atom nil)
        r (cli/dispatch ["gate" "v2" "positional-path"] {:path "explicit-path"}
                         {:gate-v2-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= "explicit-path" (:path @called)))))

(deftest dispatch-routes-corpus-intake-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "intake"] {:path "src"}
                         {:intake-fn (fn [opts] (reset! called opts) (result/ok {:catalog []}))})]
    (is (result/ok? r))
    (is (= {:path "src"} @called))))

(deftest dispatch-corpus-intake-accepts-a-positional-path-test
  ;; D10: `ehrt corpus intake PATH` -- same positional/--path convention
  ;; as gate and corpus mutate.
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "intake" "src"] {}
                         {:intake-fn (fn [opts] (reset! called opts) (result/ok {:catalog []}))})]
    (is (result/ok? r))
    (is (= "src" (:path @called)))))

(deftest dispatch-routes-corpus-operators-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "operators"] {}
                         {:operators-fn (fn [opts] (reset! called opts) (result/ok {:operators []}))})]
    (is (result/ok? r))
    (is (= {} @called))))

;; ---- render ----

(deftest render-edn-by-default-test
  (is (= "{:status :ok, :payload {:x 1}}" (cli/render (result/ok {:x 1}) false))))

(deftest render-json-when-requested-test
  (let [rendered (cli/render (result/ok {:x 1}) true)]
    (is (clojure.string/includes? rendered "\"status\""))
    (is (clojure.string/includes? rendered "\"ok\""))))

;; ---- fetch-command / resolve-command (the real default-fn command
;; paths that dispatch's :fetch-fn/:resolve-fn tests above only ever
;; exercise via injected stubs -- these test the real wiring) ----

(deftest fetch-command-unknown-artifact-test
  (let [lockfile (temp-lockfile [(sample-artifact)])
        r (cli/fetch-command {:name "nope" :version "9.9.9" :lockfile lockfile})]
    (is (result/rejected? r))
    (is (= :unknown-artifact (:category r)))))

(deftest fetch-command-propagates-lockfile-read-failure-test
  (let [r (cli/fetch-command {:name "synthea" :version "4.0.0" :lockfile "/no/such/lockfile.edn"})]
    (is (result/error? r))
    (is (= :not-found (:category r)))))

(deftest fetch-command-delegates-known-artifact-to-artifact-fetch-test
  (let [art (sample-artifact)
        lockfile (temp-lockfile [art])
        called (atom nil)]
    (with-redefs [artifact/fetch (fn [artifact-entry] (reset! called artifact-entry) (result/ok {:cached true}))]
      (let [r (cli/fetch-command {:name "synthea" :version "4.0.0" :lockfile lockfile})]
        (is (result/ok? r))
        (is (= art @called))))))

(deftest fetch-command-defaults-to-repo-root-lockfile-when-omitted-test
  ;; No :lockfile opt -- falls back to "artifacts.lock.edn" (the real
  ;; repo-root lockfile, which is why this must reject :unknown-artifact
  ;; rather than :not-found for a name that really isn't in it).
  (let [r (cli/fetch-command {:name "definitely-not-a-real-artifact" :version "0.0.0"})]
    (is (result/rejected? r))
    (is (= :unknown-artifact (:category r)))))

(deftest resolve-command-delegates-to-artifact-resolve-test
  (let [art (sample-artifact)
        lockfile (temp-lockfile [art])
        called (atom nil)]
    (with-redefs [artifact/resolve-artifact (fn [artifacts name version]
                                     (reset! called [artifacts name version])
                                     (result/rejected :not-cached {:name name :version version}))]
      (let [r (cli/resolve-command {:name "synthea" :version "4.0.0" :lockfile lockfile})]
        (is (result/rejected? r))
        (is (= [[art] "synthea" "4.0.0"] @called))))))

(deftest resolve-command-propagates-lockfile-read-failure-test
  (let [r (cli/resolve-command {:name "synthea" :version "4.0.0" :lockfile "/no/such/lockfile.edn"})]
    (is (result/error? r))
    (is (= :not-found (:category r)))))

;; ---- D13: ehrt artifact fetch --all (docs/source-sink-design.md Part
;; IX.6, ADR-0019) -- every lockfile entry, one failure never masking
;; another, exit worst-of. ----

(deftest fetch-all-command-fetches-every-lockfile-entry-test
  (let [art1 (assoc (sample-artifact) :name "synthea")
        art2 (assoc (sample-artifact) :name "temurin-jdk")
        lockfile (temp-lockfile [art1 art2])
        called (atom [])]
    (with-redefs [artifact/fetch (fn [a] (swap! called conj a) (result/ok {:cached true}))]
      (let [r (cli/fetch-all-command {:lockfile lockfile})]
        (is (result/ok? r))
        (is (= [art1 art2] @called))
        (is (= 2 (count (:results (:payload r)))))))))

(deftest fetch-all-command-one-failure-does-not-abort-the-rest-test
  (let [art1 (assoc (sample-artifact) :name "will-fail")
        art2 (assoc (sample-artifact) :name "will-succeed")
        lockfile (temp-lockfile [art1 art2])
        called (atom [])]
    (with-redefs [artifact/fetch (fn [a]
                                   (swap! called conj (:name a))
                                   (if (= "will-fail" (:name a))
                                     (result/error :download-failed {})
                                     (result/ok {:cached true})))]
      (let [r (cli/fetch-all-command {:lockfile lockfile})]
        (is (= ["will-fail" "will-succeed"] @called)
            "every artifact is attempted regardless of an earlier failure")
        (is (result/error? r) "worst-of: any error makes the aggregate an error")
        (is (= :some-fetches-failed (:category r)))))))

(deftest fetch-all-command-worst-of-prefers-error-over-rejected-test
  (let [art1 (assoc (sample-artifact) :name "rejected-one")
        art2 (assoc (sample-artifact) :name "error-one")
        lockfile (temp-lockfile [art1 art2])]
    (with-redefs [artifact/fetch (fn [a]
                                   (if (= "rejected-one" (:name a))
                                     (result/rejected :hash-mismatch {})
                                     (result/error :download-failed {})))]
      (let [r (cli/fetch-all-command {:lockfile lockfile})]
        (is (result/error? r) "a single error outranks any number of rejections")))))

(deftest fetch-all-command-propagates-lockfile-read-failure-test
  (let [r (cli/fetch-all-command {:lockfile "/no/such/lockfile.edn"})]
    (is (result/error? r))
    (is (= :not-found (:category r)))))

;; ---- D13: ehrt version -- honestly-pre-release identity, never a
;; fabricated semver. ----

(deftest version-command-reports-pre-release-identity-and-artifacts-test
  (let [art (sample-artifact)
        lockfile (temp-lockfile [art])
        r (cli/version-command {:lockfile lockfile :git-describe-fn (fn [] "abc1234")})]
    (is (result/ok? r))
    (is (= "pre-release" (:identity (:payload r))))
    (is (= "abc1234" (:git (:payload r))))
    (is (= [{:name (:name art) :version (:version art)}] (:artifacts (:payload r))))))

(deftest version-command-real-git-describe-never-throws-test
  (is (string? (cli/real-git-describe))))

(deftest version-command-propagates-lockfile-read-failure-test
  (let [r (cli/version-command {:lockfile "/no/such/lockfile.edn"})]
    (is (result/error? r))
    (is (= :not-found (:category r)))))

;; ---- D13: ehrt doctor -- SETUP.md's verification checklist as checks,
;; hermetic via injected fakes (the artifact_test.clj pattern). ----

(deftest doctor-command-all-checks-pass-is-ok-test
  (let [art (sample-artifact)
        lockfile (temp-lockfile [art])
        r (cli/doctor-command
           {:lockfile lockfile
            :resolve-java-bin-fn (fn [_artifacts _opts] (result/ok {:path "/fake/java"}))
            :resolve-artifact-fn (fn [_artifacts _name _version] (result/ok {:path "/fake/cached"}))
            :git-config-fn (fn [_key] ".githooks")
            :os-name-fn (fn [] "Linux")})]
    (is (result/ok? r))
    (is (every? #(= :pass (:status %)) (:checks (:payload r))))))

(deftest doctor-command-any-failing-check-is-rejected-not-error-test
  (let [art (sample-artifact)
        lockfile (temp-lockfile [art])
        r (cli/doctor-command
           {:lockfile lockfile
            :resolve-java-bin-fn (fn [_artifacts _opts] (result/rejected :not-cached {}))
            :resolve-artifact-fn (fn [_artifacts _name _version] (result/ok {:path "/fake/cached"}))
            :git-config-fn (fn [_key] ".githooks")
            :os-name-fn (fn [] "Linux")})]
    (is (result/rejected? r))
    (is (= :doctor-checks-failed (:category r)))
    (is (= 1 (cli/result->exit-code r)))
    (is (= :fail (:status (first (:checks (:payload r))))))
    (is (clojure.string/includes? (:hint (:payload r)) "--edn")
        "the hint-family rule: no category of doctor output is ever a dead end")))

(deftest doctor-command-uncached-artifact-fails-the-cache-check-test
  (let [art (sample-artifact)
        lockfile (temp-lockfile [art])
        r (cli/doctor-command
           {:lockfile lockfile
            :resolve-java-bin-fn (fn [_artifacts _opts] (result/ok {:path "/fake/java"}))
            :resolve-artifact-fn (fn [_artifacts _name _version] (result/rejected :not-cached {}))
            :git-config-fn (fn [_key] ".githooks")
            :os-name-fn (fn [] "Linux")})]
    (is (result/rejected? r))
    (let [cache-check (first (filter #(clojure.string/includes? (:name %) "artifact cache") (:checks (:payload r))))]
      (is (= :fail (:status cache-check))))))

(deftest doctor-command-unwired-hooks-path-fails-that-check-test
  (let [art (sample-artifact)
        lockfile (temp-lockfile [art])
        r (cli/doctor-command
           {:lockfile lockfile
            :resolve-java-bin-fn (fn [_artifacts _opts] (result/ok {:path "/fake/java"}))
            :resolve-artifact-fn (fn [_artifacts _name _version] (result/ok {:path "/fake/cached"}))
            :git-config-fn (fn [_key] nil)
            :os-name-fn (fn [] "Linux")})]
    (is (result/rejected? r))
    (let [hooks-check (first (filter #(clojure.string/includes? (:name %) "hooksPath") (:checks (:payload r))))]
      (is (= :fail (:status hooks-check))))))

(deftest doctor-command-native-windows-fails-the-platform-check-test
  (let [art (sample-artifact)
        lockfile (temp-lockfile [art])
        r (cli/doctor-command
           {:lockfile lockfile
            :resolve-java-bin-fn (fn [_artifacts _opts] (result/ok {:path "/fake/java"}))
            :resolve-artifact-fn (fn [_artifacts _name _version] (result/ok {:path "/fake/cached"}))
            :git-config-fn (fn [_key] ".githooks")
            :os-name-fn (fn [] "Windows 11")})]
    (is (result/rejected? r))
    (let [platform-check (first (filter #(= "platform" (:name %)) (:checks (:payload r))))]
      (is (= :fail (:status platform-check))))))

(deftest doctor-command-cannot-even-check-is-error-not-rejected-test
  (let [r (cli/doctor-command {:lockfile "/no/such/lockfile.edn"})]
    (is (result/error? r))
    (is (= :not-found (:category r)))
    (is (= 2 (cli/result->exit-code r)))
    (is (clojure.string/includes? (:hint (:payload r)) "SETUP.md")
        "the hint-family rule: no category of doctor output is ever a dead end")))

(deftest doctor-command-deps-edn-resolved-artifact-skips-the-cache-check-test
  ;; P2-3 (ruled 2026-07-31, review finding 8): a row marked
  ;; :resolved-via :deps-edn (the NIST engine's six lockfile rows'
  ;; ruled posture) is never asked about the cache -- an engine that
  ;; only ever loads via a project's own deps.edn must not fail doctor
  ;; just because it was never fetched into the artifact cache.
  (let [cached (sample-artifact)
        deps-edn-art (assoc (sample-artifact)
                             :name "nist-hl7-v2-validation" :resolved-via :deps-edn)
        lockfile (temp-lockfile [cached deps-edn-art])
        r (cli/doctor-command
           {:lockfile lockfile
            :resolve-java-bin-fn (fn [_artifacts _opts] (result/ok {:path "/fake/java"}))
            :resolve-artifact-fn (fn [_artifacts name _version]
                                    (if (= name "nist-hl7-v2-validation")
                                      (result/rejected :not-cached {})
                                      (result/ok {:path "/fake/cached"})))
            :git-config-fn (fn [_key] ".githooks")
            :os-name-fn (fn [] "Linux")})]
    (is (result/ok? r)
        "the deps.edn-resolved row is skipped by the cache check entirely, so an artifact that never touches the cache doesn't fail doctor")
    (let [cache-check (first (filter #(clojure.string/includes? (:name %) "artifact cache") (:checks (:payload r))))]
      (is (= :pass (:status cache-check)))
      (is (clojure.string/includes? (:detail cache-check) "resolved via deps.edn")
          "the human-readable story distinguishes deps.edn resolution from cache resolution")
      (is (clojure.string/includes? (:detail cache-check) "nist-hl7-v2-validation")))))

(deftest doctor-command-real-fns-never-throw-test
  ;; The real, non-injected fns (git config/os name) must never throw --
  ;; a doctor that crashes trying to check something is worse than one
  ;; that reports it honestly.
  (is (string? (cli/real-os-name)))
  (is (or (nil? (cli/real-git-config "core.hooksPath"))
          (string? (cli/real-git-config "core.hooksPath")))))

;; ---- mutate-command (`ehrt corpus mutate`): input file/dir, operator
;; id, locator, output dir -> writes mutant JSON files plus lineage
;; EDN sidecars under output-dir/lineage/ (design choice, documented
;; on mutate-command's own docstring: a subdirectory rather than
;; interleaved sidecars, so a downstream stage can glob output-dir for
;; data and output-dir/lineage for provenance without filtering). ----

(defn- temp-dir* []
  (let [f (File/createTempFile "cli-mutate-test" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

(def sample-bundle-json
  "{\"resourceType\":\"Bundle\",\"type\":\"transaction\",\"entry\":[{\"resource\":{\"resourceType\":\"Patient\",\"id\":\"p1\",\"gender\":\"female\"}}]}")

(deftest mutate-command-happy-path-writes-mutant-and-lineage-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "patient1.json") sample-bundle-json)
        r (cli/mutate-command {:path in-dir :operator-id "remove-required-element"
                                :locator-path "entry[0].resource.gender" :out-dir out-dir})]
    (is (result/ok? r))
    (is (= 1 (:count (:payload r))))
    (let [mutant (slurp (io/file out-dir "patient1.json"))
          lineage-file (io/file out-dir "lineage" "patient1.json.lineage.edn")]
      (is (not (clojure.string/includes? mutant "gender")))
      (is (.exists lineage-file))
      (let [lineage (clojure.edn/read-string (slurp lineage-file))]
        (is (= :remove-required-element (:id (:operator (:transformation lineage)))))))))

;; ---- operation-manifest.edn (SS-4b, D-d resolved via ADR-0020): the
;; directory-write path now emits it last, after every mutant/lineage
;; sidecar -- git-describe-fn/now-fn are injected here for determinism,
;; mirroring version-command's own test precedent (line ~454). ----

(deftest mutate-command-emits-operation-manifest-after-the-batch-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "patient1.json") sample-bundle-json)
        r (cli/mutate-command {:path in-dir :operator-id "remove-required-element"
                                :locator-path "entry[0].resource.gender" :out-dir out-dir
                                :git-describe-fn (fn [] "abc1234") :now-fn (fn [] "2026-07-28")})]
    (is (result/ok? r))
    (let [manifest-file (io/file out-dir "operation-manifest.edn")]
      (is (.exists manifest-file))
      (let [manifest (clojure.edn/read-string (slurp manifest-file))
            lineage (clojure.edn/read-string
                     (slurp (io/file out-dir "lineage" "patient1.json.lineage.edn")))]
        (is (= :operation (:manifest-kind manifest)))
        (is (= {:name "ehrt" :identity "pre-release" :git "abc1234"}
               (:producer manifest)))
        (is (= :mutate (:kind (:operation manifest))))
        (is (= :remove-required-element (:operator-id (:operation manifest))))
        (is (= "1" (:operator-version (:operation manifest))))
        (is (= "2026-07-28" (:written-at manifest)))
        (is (= :fhir-json (:format manifest)))
        (is (= [{:name "patient1.json" :sha256 (:produced lineage) :input-hash (:parent lineage)}]
               (:items manifest))
            "the item's own sha256/input-hash come straight from the same lineage record, no re-derivation")))))

(deftest operation-manifest-producer-name-is-pinned-to-the-product-name-test
  (testing "the producer's :name is \"ehrt\" -- the product name, decoupled from
            component layout (author ruling 2026-08-01) -- pinned on its own so a
            future rename can't silently change this output vocabulary again"
    (let [in-dir (temp-dir*)
          out-dir (str (temp-dir*) "/out")
          _ (spit (io/file in-dir "patient1.json") sample-bundle-json)
          r (cli/mutate-command {:path in-dir :operator-id "remove-required-element"
                                  :locator-path "entry[0].resource.gender" :out-dir out-dir
                                  :git-describe-fn (fn [] "abc1234") :now-fn (fn [] "2026-07-28")})
          manifest (clojure.edn/read-string
                    (slurp (io/file out-dir "operation-manifest.edn")))]
      (is (result/ok? r))
      (is (= "ehrt" (:name (:producer manifest)))))))

(deftest mutate-command-processes-every-json-file-in-a-directory-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        _ (spit (io/file in-dir "b.json") sample-bundle-json)
        _ (spit (io/file in-dir "not-json.txt") "ignore me")
        r (cli/mutate-command {:path in-dir :operator-id "duplicate-element"
                                :locator-path "entry[0].resource.gender" :out-dir out-dir})]
    (is (result/ok? r))
    (is (= 2 (:count (:payload r))))
    (is (.exists (io/file out-dir "a.json")))
    (is (.exists (io/file out-dir "b.json")))
    (is (not (.exists (io/file out-dir "not-json.txt"))))))

(deftest mutate-command-accepts-a-single-file-as-input-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        f (io/file in-dir "one.json")
        _ (spit f sample-bundle-json)
        r (cli/mutate-command {:path (.getAbsolutePath f) :operator-id "wrong-type-value"
                                :locator-path "entry[0].resource.gender" :out-dir out-dir})]
    (is (result/ok? r))
    (is (= 1 (:count (:payload r))))))

(deftest mutate-command-rejects-unknown-operator-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:path in-dir :operator-id "no-such-operator"
                                :locator-path "entry[0].resource.gender" :out-dir (temp-dir*)})]
    (is (result/rejected? r))
    (is (= :unknown-operator (:category r)))))

(deftest mutate-command-unknown-operator-names-the-valid-ids-test
  ;; DOC-1 Step 4: the enumerable-options error pass, extended to this
  ;; site (the CLI-reachable equivalent of the prompt's named
  ;; :invalid-operator -- see the DOC-1 close-out report). Category is
  ;; unchanged; only the payload gains :valid-options and a hint.
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:path in-dir :operator-id "no-such-operator"
                                :locator-path "entry[0].resource.gender" :out-dir (temp-dir*)})]
    (is (= :unknown-operator (:category r)) "category survives the payload extension")
    (is (contains? (set (:valid-options (:payload r))) :remove-required-element))
    (is (= 10 (count (:valid-options (:payload r)))))
    (is (= "run: ehrt corpus operators" (:hint (:payload r))))))

(deftest mutate-command-defaults-operator-version-to-1-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:path in-dir :operator-id "remove-required-element"
                                :locator-path "entry[0].resource.gender" :out-dir (temp-dir*)})]
    (is (result/ok? r))))

;; ---- D12 (docs/source-sink-design.md Part IX.5, ADR-0019): derived
;; --out-dir; a registry entry MAY declare :default-locator, consulted
;; when --locator-path is omitted. No seed-catalog operator declares
;; one yet -- these tests exercise the mechanism with a fake operator,
;; snapshotting/restoring the real registry around it. ----

(deftest mutate-command-derives-out-dir-when-omitted-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:path in-dir :operator-id "remove-required-element"
                                :locator-path "entry[0].resource.gender"})]
    (is (result/ok? r))
    (is (.isDirectory (io/file (str in-dir "-mutants/remove-required-element@1/"))))
    (is (.isFile (io/file (str in-dir "-mutants/remove-required-element@1/") "a.json")))))

(deftest mutate-command-explicit-out-dir-not-overridden-by-default-test
  (let [in-dir (temp-dir*)
        out-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:path in-dir :operator-id "remove-required-element"
                                :locator-path "entry[0].resource.gender" :out-dir out-dir})]
    (is (result/ok? r))
    (is (.isFile (io/file out-dir "a.json")))))

(defn- with-fake-default-locator-operator
  "Registers a fake :fhir operator declaring :default-locator, runs f,
  then restores the real registry -- same snapshot/restore convention
  the corpus component's own tests use."
  [f]
  (let [snapshot (operators/operator-registry-snapshot)]
    (try
      (operators/operator-register!
       {:id :fake-default-locator-op :version "1" :format :fhir
        :contract {:type :violates :target "test contract"}
        :locator-required? true
        :default-locator "entry[0].resource.gender"
        :fn (fn [data _locator] (result/ok {:mutant data :lineage {:id "fake-lineage"}}))})
      (f)
      (finally (operators/operator-registry-reset! snapshot)))))

(deftest mutate-command-falls-back-to-operators-default-locator-test
  (with-fake-default-locator-operator
    (fn []
      (let [in-dir (temp-dir*)
            _ (spit (io/file in-dir "a.json") sample-bundle-json)
            r (cli/mutate-command {:path in-dir :operator-id "fake-default-locator-op"
                                    :out-dir (temp-dir*)})]
        (is (result/ok? r) (pr-str r))))))

(deftest mutate-command-explicit-locator-path-not-overridden-by-default-test
  (with-fake-default-locator-operator
    (fn []
      (let [in-dir (temp-dir*)
            _ (spit (io/file in-dir "a.json") sample-bundle-json)
            r (cli/mutate-command {:path in-dir :operator-id "fake-default-locator-op"
                                    :locator-path "entry[0].resource.id" :out-dir (temp-dir*)})]
        (is (result/ok? r) (pr-str r))))))

(deftest mutate-command-still-requires-locator-path-for-operator-without-a-default-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:path in-dir :operator-id "remove-required-element"
                                :out-dir (temp-dir*)})]
    (is (not (result/ok? r))
        "remove-required-element declares no :default-locator, so an omitted --locator-path must still fail")))

(deftest mutate-command-propagates-a-locator-that-does-not-resolve-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:path in-dir :operator-id "remove-required-element"
                                :locator-path "entry[0].resource.noSuchField" :out-dir (temp-dir*)})]
    (is (result/rejected? r))
    (is (= :locator-not-found (:category r)))))

(deftest mutate-command-rejects-invalid-locator-path-syntax-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:path in-dir :operator-id "remove-required-element"
                                :locator-path "entry[bad]" :out-dir (temp-dir*)})]
    (is (result/rejected? r))
    (is (= :invalid-fhir-path (:category r)))))

;; ---- mutate-command, v2 dispatch (P7): same command, format dispatch
;; by operator lookup routes *.hl7 files through the er7 substrate
;; instead of *.json through plain FHIR data. ----

(def ^:private admit-content
  (delay (slurp (io/file "components/corpus/test-fixtures/v2/adt-a01-admit.hl7"))))

(deftest mutate-command-v2-happy-path-writes-mutant-and-lineage-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "adt.hl7") @admit-content)
        r (cli/mutate-command {:path in-dir :operator-id "blank-required-field"
                                :locator-path "MSH-9" :out-dir out-dir})]
    (is (result/ok? r))
    (is (= 1 (:count (:payload r))))
    (let [mutant (slurp (io/file out-dir "adt.hl7"))
          lineage-file (io/file out-dir "lineage" "adt.hl7.lineage.edn")]
      (is (not (clojure.string/includes? mutant "ADT^A01^ADT_A01")))
      (is (.exists lineage-file))
      (let [lineage (clojure.edn/read-string (slurp lineage-file))]
        (is (= :blank-required-field (:id (:operator (:transformation lineage)))))
        (is (= {:format :v2 :path "MSH-9"} (:locator (:transformation lineage))))))))

(deftest mutate-command-v2-processes-every-hl7-file-in-a-directory-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "a.hl7") @admit-content)
        _ (spit (io/file in-dir "b.hl7") @admit-content)
        _ (spit (io/file in-dir "not-hl7.json") sample-bundle-json)
        r (cli/mutate-command {:path in-dir :operator-id "corrupt-encoding-characters"
                                :locator-path "MSH-2" :out-dir out-dir})]
    (is (result/ok? r))
    (is (= 2 (:count (:payload r))))
    (is (.exists (io/file out-dir "a.hl7")))
    (is (.exists (io/file out-dir "b.hl7")))
    (is (not (.exists (io/file out-dir "not-hl7.json"))))))

(deftest mutate-command-v2-propagates-a-locator-that-does-not-resolve-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.hl7") @admit-content)
        r (cli/mutate-command {:path in-dir :operator-id "blank-required-field"
                                :locator-path "ZZZ-3" :out-dir (temp-dir*)})]
    (is (result/rejected? r))
    (is (= :locator-not-found (:category r)))))

(deftest mutate-command-v2-rejects-invalid-locator-path-syntax-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.hl7") @admit-content)
        r (cli/mutate-command {:path in-dir :operator-id "blank-required-field"
                                :locator-path "PID-0" :out-dir (temp-dir*)})]
    (is (result/rejected? r))
    (is (= :invalid-v2-path (:category r)))))

;; ---- mutate-command routed through a :stdout Sink (SS-4 rulings 5-6):
;; :out-dir accepting a `stdout:...` designator instead of a directory
;; path, batched write, no lineage sidecar (named scope decision, see
;; mutate-to-stdout!'s own docstring). :stdout-out is the injected
;; ByteArrayOutputStream so nothing here touches the real process
;; stdout. ----

(deftest mutate-command-stdout-sink-v2-mllp-writes-every-mutant-test
  (let [in-dir (temp-dir*)
        out (java.io.ByteArrayOutputStream.)
        _ (spit (io/file in-dir "a.hl7") @admit-content)
        _ (spit (io/file in-dir "b.hl7") @admit-content)
        r (cli/mutate-command {:path in-dir :operator-id "corrupt-encoding-characters"
                                :locator-path "MSH-2"
                                :out-dir "stdout:?format=v2-er7&framing=mllp"
                                :stdout-out out})]
    (is (result/ok? r))
    (is (pos? (:bytes-written (:payload r))))
    (let [bs (.toByteArray out)]
      ;; two MLLP-framed messages: 0x0B ... 0x1C 0x0D, back to back
      (is (pos? (alength bs)))
      (is (= 0x0B (bit-and 0xff (aget bs 0))))
      (is (= 2 (count (re-seq #"\x0b" (String. bs "ISO-8859-1"))))
          "two 0x0B start markers, one per mutant, no directory/lineage written"))
    (is (not (.exists (io/file "stdout:?format=v2-er7&framing=mllp")))
        "no literal directory named after the designator string was ever created")))

(deftest mutate-command-stdout-sink-fhir-ndjson-writes-every-mutant-test
  (let [in-dir (temp-dir*)
        out (java.io.ByteArrayOutputStream.)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        r (cli/mutate-command {:path in-dir :operator-id "remove-required-element"
                                :locator-path "entry[0].resource.gender"
                                :out-dir "stdout:?format=fhir-json&framing=ndjson"
                                :stdout-out out})]
    (is (result/ok? r))
    (let [text (String. (.toByteArray out) "UTF-8")]
      (is (clojure.string/ends-with? text "\n") "ndjson's own trailing-LF-per-item convention")
      (is (= 1 (count (clojure.string/split-lines text)))))))

(deftest mutate-command-stdout-sink-propagates-mutation-failure-test
  (let [in-dir (temp-dir*)
        out (java.io.ByteArrayOutputStream.)
        _ (spit (io/file in-dir "a.hl7") @admit-content)
        r (cli/mutate-command {:path in-dir :operator-id "blank-required-field"
                                :locator-path "ZZZ-3"
                                :out-dir "stdout:?format=v2-er7&framing=mllp"
                                :stdout-out out})]
    (is (result/rejected? r))
    (is (= :locator-not-found (:category r)))
    (is (zero? (alength (.toByteArray out))) "nothing written when a per-file mutation fails")))

;; ---- intake-command (`ehrt corpus intake`): the real wiring, not the
;; injected-stub path dispatch-routes-corpus-intake-test exercises ----

(deftest intake-command-delegates-to-corpus-intake-with-explicit-received-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "patient.json") sample-bundle-json)
        r (cli/intake-command {:path in-dir :label "acme" :out out-dir :received "2026-07-24"})]
    (is (result/ok? r))
    (is (= 1 (:file-count (:intake-record (:payload r)))))
    (is (= "2026-07-24" (:date (:intake-record (:payload r)))))))

(deftest intake-command-defaults-received-to-today-test
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "patient.json") sample-bundle-json)
        r (cli/intake-command {:path in-dir :label "acme" :out out-dir})]
    (is (result/ok? r))
    (is (= (str (java.time.LocalDate/now)) (:date (:intake-record (:payload r)))))))

;; ---- intake-command accepts a generator URL in place of PATH (SS-2
;; Step 4, ruling 6): the one-command generate-and-catalog path.
;; Hermetic via a temporarily swapped-in :sim registry entry (restored
;; in a finally, since the corpus generator registry is a single
;; shared, process-wide registry -- other test namespaces' own :sim
;; coverage depends on the real entry surviving this test). ----

(defn- with-fake-sim-entry
  "Swaps the real :sim generator entry for a hermetic fake for the
  duration of thunk, then restores the real one -- even on failure."
  [thunk]
  (let [real-entry (generators/generator-lookup :sim)]
    (try
      (thunk)
      (finally
        (generators/generator-register! real-entry)))))

(deftest intake-command-generator-url-resolves-and-catalogs-test
  (with-fake-sim-entry
    (fn []
      (let [corpus-dir (str (temp-dir*) "/corpus")
            out-dir (str (temp-dir*) "/out")]
        (generators/generator-register!
         {:kind :sim
          :default-params {:seed 1}
          :params-schema [:map [:seed {:optional true} :int]]
          :out-dir-fn (fn [_] corpus-dir)
          :execute-fn (fn [_ dir]
                        (.mkdirs (io/file dir))
                        (spit (io/file dir "msg-000.hl7") "MSH|^~\\&|SIM|...")
                        (result/ok {:out-dir dir}))})
        (let [r (cli/intake-command {:path "sim:?seed=42" :label "sim-test"
                                      :out out-dir :received "2026-07-28"})]
          (is (result/ok? r))
          (is (= 1 (:file-count (:intake-record (:payload r)))))
          (is (every? #(= "sim-test" (:origin %)) (:catalog (:payload r)))))))))

(deftest intake-command-generator-url-out-dir-collision-propagates-test
  (with-fake-sim-entry
    (fn []
      (let [corpus-dir (temp-dir*) ; already exists and non-empty
            _ (spit (io/file corpus-dir "leftover.txt") "from a previous run")
            out-dir (str (temp-dir*) "/out")]
        (generators/generator-register!
         {:kind :sim
          :default-params {:seed 1}
          :params-schema [:map [:seed {:optional true} :int]]
          :out-dir-fn (fn [_] corpus-dir)
          :execute-fn (fn [_ dir] (result/ok {:out-dir dir}))})
        (let [r (cli/intake-command {:path "sim:" :label "sim-test" :out out-dir})]
          (is (result/error? r))
          (is (= :out-dir-exists (:category r))))))))

(deftest intake-command-plain-path-still-works-unaffected-test
  ;; a bare directory path that happens to have no scheme colon at all
  ;; must still intake directly, unaffected by the generator-URL branch
  (let [in-dir (temp-dir*)
        out-dir (str (temp-dir*) "/out")
        _ (spit (io/file in-dir "patient.json") sample-bundle-json)
        r (cli/intake-command {:path in-dir :label "acme" :out out-dir :received "2026-07-24"})]
    (is (result/ok? r))
    (is (= 1 (:file-count (:intake-record (:payload r)))))))

;; ---- intake-command accepts a stdin designator in place of PATH
;; (SS-3 Step 6, ruling 5): read (via :in-override in tests), spool,
;; then catalog exactly like a resolved generator Source. ----

(deftest intake-command-stdin-url-resolves-and-catalogs-test
  (let [out-dir (str (temp-dir*) "/out")
        piped (java.io.ByteArrayInputStream.
               (.getBytes "MSH|^~\\&|A\n\nMSH|^~\\&|B\n\n" "UTF-8"))
        r (cli/intake-command {:path "stdin:?format=v2-er7&framing=er7-multi"
                                :label "piped" :out out-dir :received "2026-07-28"
                                :in-override piped})]
    (is (result/ok? r))
    ;; 2 spooled item files + the spool's own capture-manifest.edn (a
    ;; distinct schema from ADR-0014's manifest.edn -- named differently
    ;; on purpose, ruling 4 -- so it is NOT recognized as a provenance
    ;; sidecar; it's just a third foreign file, cataloged like any other)
    (is (= 3 (:file-count (:intake-record (:payload r)))))
    (is (every? #(= "piped" (:origin %)) (:catalog (:payload r))))
    (is (= #{"item-0000.hl7" "item-0001.hl7" "capture-manifest.edn"}
           (into #{} (map :path) (:catalog (:payload r)))))))

(deftest intake-command-stdin-propagates-spool-rejection-test
  (let [out-dir (str (temp-dir*) "/out")
        piped (java.io.ByteArrayInputStream. (.getBytes "no message here" "UTF-8"))
        r (cli/intake-command {:path "stdin:?format=v2-er7&framing=er7-multi"
                                :label "piped" :out out-dir
                                :in-override piped})]
    (is (result/rejected? r))
    (is (= :malformed-er7-multi-frame (:category r)))))

;; ---- operators-command (`ehrt corpus operators`): a pure read of
;; corpus.operators' registry -- no filesystem, no subprocess, no
;; options required. ----

(deftest operators-command-lists-all-ten-registered-operators-test
  (let [r (cli/operators-command {})]
    (is (result/ok? r))
    (is (= 10 (count (:operators (:payload r)))))
    (is (= #{:remove-required-element :duplicate-element :invalid-code-value
             :malformed-date :wrong-type-value :blank-required-field
             :corrupt-encoding-characters :malformed-datetime-value
             :truncate-segment-fields :corrupt-segment-name}
           (set (map :id (:operators (:payload r))))))))

(deftest operators-command-filters-by-format-test
  (let [r (cli/operators-command {:format "v2"})]
    (is (result/ok? r))
    (is (= 5 (count (:operators (:payload r)))))
    (is (every? #(= :v2 (:format %)) (:operators (:payload r))))))

(deftest operators-command-sorted-by-format-then-id-test
  (let [rows (:operators (:payload (cli/operators-command {})))]
    (is (= rows (sort-by (juxt :format :id) rows)))))

(deftest operators-command-rows-carry-contract-type-and-target-test
  (let [rows (:operators (:payload (cli/operators-command {})))
        remove-req (first (filter #(= :remove-required-element (:id %)) rows))]
    (is (= :violates (:type remove-req)))
    (is (string? (:target remove-req)))
    (is (true? (:locator-required? remove-req)))
    (is (= "1" (:version remove-req)))))

(deftest operators-command-rows-carry-the-doc-sentence-test
  ;; DOC-4: the one-line description DOC-3 put in the registry is
  ;; readable at the shell too, not only in docs/operators.md. It is a
  ;; distinct register from :target -- :doc is the edit, :target is the
  ;; conformance claim -- so both are asserted present and different.
  (let [rows (:operators (:payload (cli/operators-command {})))
        remove-req (first (filter #(= :remove-required-element (:id %)) rows))]
    (is (every? #(and (string? (:doc %)) (seq (:doc %))) rows))
    (is (not= (:doc remove-req) (:target remove-req)))))

(deftest operators-command-is-a-pure-registry-read-no-io-test
  ;; Proven by construction: redefining io/file to throw confirms
  ;; nothing in operators-command touches the filesystem (it only
  ;; derefs corpus.operators' in-memory registry atom).
  (with-redefs [io/file (fn [& _] (throw (ex-info "no filesystem access expected" {})))]
    (let [r (cli/operators-command {})]
      (is (result/ok? r))
      (is (= 10 (count (:operators (:payload r))))))))

;; ---- gate-command / gate-v2-command (`ehrt gate v2`): builds a
;; format-agnostic gate report over a file or directory; exit-code
;; contract via result/ok vs result/rejected, not special-cased in
;; result->exit-code (ADR-0004's generic mapping already does the
;; right thing once gate-command itself signals :gate-rejected). ----

(deftest gate-v2-command-gates-a-single-passing-file-test
  (let [r (cli/gate-v2-command {:path "components/corpus/test-fixtures/v2/adt-a01-admit.hl7"})]
    (is (result/ok? r))
    (is (= {:pass 1 :rejected 0 :indeterminate 0 :no-verdict 0} (:totals (:payload r))))))

(deftest gate-v2-command-gates-a-directory-test
  (let [r (cli/gate-v2-command {:path "components/corpus/test-fixtures/v2"})]
    (is (result/ok? r))
    (is (= 5 (count (:files (:payload r)))))))

(deftest gate-v2-command-rejects-when-any-file-is-rejected-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "broken.hl7") "MSH|^~&|only-encoding-chars-broken")
        r (cli/gate-v2-command {:path in-dir})]
    (is (result/rejected? r))
    (is (= :gate-rejected (:category r)))
    (is (= 1 (cli/result->exit-code r)))))

(deftest gate-v2-command-writes-report-file-when-requested-test
  (let [out-file (str (temp-dir*) "/report.edn")
        r (cli/gate-v2-command {:path "components/corpus/test-fixtures/v2/adt-a01-admit.hl7" :report out-file})]
    (is (result/ok? r))
    (is (.exists (io/file out-file)))
    (is (= (:payload r) (clojure.edn/read-string (slurp out-file))))))

;; ---- --report path handling (CLI-2, ADR-0004): the user named where
;; they want the file, so a missing intermediate directory is created
;; rather than surfaced as a raw FileNotFoundException; an IO failure
;; that survives that becomes a categorized result/error, never an
;; uncaught throw. Both gate write sites (plain and --baseline) and
;; check's own site are covered. ----

(deftest gate-v2-command-report-creates-missing-parent-directories-test
  (let [out-file (str (temp-dir*) "/nested/deeper/report.edn")
        r (cli/gate-v2-command {:path "components/corpus/test-fixtures/v2/adt-a01-admit.hl7" :report out-file})]
    (is (result/ok? r))
    (is (.exists (io/file out-file)))
    (is (= (:payload r) (clojure.edn/read-string (slurp out-file))))))

(deftest gate-v2-command-report-write-failure-is-categorized-not-thrown-test
  ;; A report path whose parent is an existing *file*: no directory can
  ;; be created there, so the write genuinely fails after make-parents
  ;; has done everything it can. The failure outranks the gate's own
  ;; verdict -- the run's recorded output is incomplete, which is an
  ;; operational error (exit 2), not a verdict.
  (let [blocker (str (temp-dir*) "/not-a-directory")
        _ (spit blocker "i am a file, not a directory")
        out-file (str blocker "/report.edn")
        r (cli/gate-v2-command {:path "components/corpus/test-fixtures/v2/adt-a01-admit.hl7" :report out-file})]
    (is (result/error? r))
    (is (= :report-write-failed (:category r)))
    (is (= out-file (:path (:payload r))))
    (is (string? (:message (:payload r))))
    (is (= 2 (cli/result->exit-code r)))))

;; ---- fhir-gate-command: threads lockfile artifacts + :out-dir into
;; judge.fhir/gate-file|gate-dir, curried to gate-command's 1-arity
;; shape. No real subprocess exercised here (hermetic-suite discipline
;; -- judge.fhir's own test suite already covers execute/interpret with
;; injected fakes); this only proves the wiring propagates a real
;; lockfile-resolution failure correctly. ----

(deftest fhir-gate-command-propagates-unknown-artifact-when-validator-not-in-lockfile-test
  (let [lockfile (temp-lockfile [])
        r (cli/fhir-gate-command {:path "components/corpus/test-fixtures/v2/adt-a01-admit.hl7" :lockfile lockfile})]
    (is (result/rejected? r))
    (is (= :unknown-artifact (:category r)))))

(deftest fhir-gate-command-propagates-lockfile-read-failure-test
  (let [r (cli/fhir-gate-command {:path "x.json" :lockfile "/no/such/lockfile.edn"})]
    (is (result/error? r))
    (is (= :not-found (:category r)))))

;; ---- gate --baseline (`ehrt gate v2|fhir DIR --baseline report.edn`,
;; P6): a finding counts toward rejection only if it isn't already
;; present in the baseline for that file. ----

(deftest gate-v2-command-baseline-mode-suppresses-a-known-finding-test
  (let [in-dir (temp-dir*)
        broken-path (io/file in-dir "broken.hl7")
        _ (spit broken-path "MSH|^~&|only-encoding-chars-broken")
        baseline-run (cli/gate-v2-command {:path in-dir})
        baseline-file (str (temp-dir*) "/baseline.edn")
        _ (spit baseline-file (pr-str (:payload baseline-run)))
        r (cli/gate-v2-command {:path in-dir :baseline baseline-file})]
    (is (result/rejected? baseline-run) "sanity: the file is genuinely rejected absolutely")
    (is (result/ok? r) "every finding is already in the baseline -- nothing novel")
    (is (report/report-valid? (:absolute (:payload r))))
    (is (report/report-valid? (:relative (:payload r))))
    (is (= :rejected (:verdict (first (:files (:absolute (:payload r)))))))
    (is (= :pass (:verdict (first (:files (:relative (:payload r)))))))))

(deftest gate-v2-command-baseline-mode-still-rejects-a-genuinely-new-finding-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "ok.hl7") (slurp "components/corpus/test-fixtures/v2/adt-a01-admit.hl7"))
        baseline-run (cli/gate-v2-command {:path in-dir})
        baseline-file (str (temp-dir*) "/baseline.edn")
        _ (spit baseline-file (pr-str (:payload baseline-run)))
        ;; Now break the same file -- a genuinely new finding relative
        ;; to the passing baseline.
        _ (spit (io/file in-dir "ok.hl7") "MSH|^~&|only-encoding-chars-broken")
        r (cli/gate-v2-command {:path in-dir :baseline baseline-file})]
    (is (result/ok? baseline-run))
    (is (result/rejected? r))
    (is (= :gate-rejected (:category r)))
    (is (= 1 (cli/result->exit-code r)))))

;; ---- --treat-no-verdict-as (`ehrt gate --treat-no-verdict-as
;; pass|rejected`, ADR-0010): policy totality at the act layer. Tested
;; against `gate-command` directly with an injected fake gate-file-fn --
;; neither judge.v2 (never produces :no-verdict) nor judge.fhir
;; (needs a real subprocess) can produce a :no-verdict outcome
;; hermetically, matching this repo's own dependency-injection
;; testing convention. ----

(defn- fake-no-verdict-gate-file
  [_path]
  (result/ok {:verdict :no-verdict :cause :terminology-suppressed
              :findings [{:severity :warning :code "code-invalid"
                          :locator {:format :fhir :path "x"} :message "m"
                          :engine {:name "e" :version "1"}
                          :disposition :no-verdict :cause :terminology-suppressed}]}))

(defn- fake-rejected-gate-file
  [_path]
  (result/ok {:verdict :rejected
              :findings [{:severity :error :code "invalid"
                          :locator {:format :fhir :path "x"} :message "m"
                          :engine {:name "e" :version "1"}
                          :disposition :rejected}]}))

(deftest gate-command-default-exit-code-for-no-verdict-is-distinct-test
  (let [gate-fn (cli/gate-command fake-no-verdict-gate-file (fn [_dir] (result/ok {:results []})) :fake)
        r (gate-fn {:path "some-file.json"})]
    (is (= :gate-no-verdict (:category r)))
    (is (= cli/no-verdict-exit-code (cli/result->exit-code r)))
    (is (not (contains? #{0 1 2} (cli/result->exit-code r)))
        "distinct from the existing ok/rejected/error codes -- no silent default")))

(deftest gate-command-treat-no-verdict-as-pass-remaps-to-ok-test
  (let [gate-fn (cli/gate-command fake-no-verdict-gate-file (fn [_dir] (result/ok {:results []})) :fake)
        r (gate-fn {:path "some-file.json" :treat-no-verdict-as "pass"})]
    (is (result/ok? r))
    (is (= 0 (cli/result->exit-code r)))))

(deftest gate-command-treat-no-verdict-as-rejected-remaps-to-rejected-test
  (let [gate-fn (cli/gate-command fake-no-verdict-gate-file (fn [_dir] (result/ok {:results []})) :fake)
        r (gate-fn {:path "some-file.json" :treat-no-verdict-as "rejected"})]
    (is (result/rejected? r))
    (is (= :gate-rejected (:category r)))
    (is (= 1 (cli/result->exit-code r)))))

(deftest gate-command-treat-no-verdict-as-rejects-other-values-test
  (let [gate-fn (cli/gate-command fake-no-verdict-gate-file (fn [_dir] (result/ok {:results []})) :fake)
        r (gate-fn {:path "some-file.json" :treat-no-verdict-as "bogus"})]
    (is (result/rejected? r))
    (is (= :invalid-treat-no-verdict-as (:category r)))
    (is (= 1 (cli/result->exit-code r)))))

(deftest gate-command-treat-no-verdict-as-pass-does-not-mask-a-genuine-rejection-test
  (let [gate-fn (cli/gate-command fake-rejected-gate-file (fn [_dir] (result/ok {:results []})) :fake)
        r (gate-fn {:path "some-file.json" :treat-no-verdict-as "pass"})]
    (is (result/rejected? r))
    (is (= :gate-rejected (:category r)))))

(deftest gate-command-no-flag-and-no-no-verdict-is-plain-ok-test
  (let [gate-fn (cli/gate-command (fn [_path] (result/ok {:verdict :pass :findings []}))
                                   (fn [_dir] (result/ok {:results []})) :fake)
        r (gate-fn {:path "some-file.json"})]
    (is (result/ok? r))
    (is (= 0 (cli/result->exit-code r)))))

(deftest fhir-gate-command-threads-treat-no-verdict-as-through-to-gate-command-test
  ;; No real subprocess exercised (hermetic-suite discipline) -- an
  ;; invalid flag value is rejected before the validator artifact is
  ;; even resolved, which is enough to prove the option reaches
  ;; gate-command's own opts map rather than being silently dropped.
  (let [art {:kind :engine :name "fhir-validator-cli" :version "6.9.12"
             :sha256 (apply str (repeat 64 "c")) :source "https://example.invalid/v.jar"
             :acquired "2026-07-24" :license-status :verified}
        lockfile (temp-lockfile [art])
        r (cli/fhir-gate-command {:path "components/corpus/test-fixtures/v2/adt-a01-admit.hl7" :lockfile lockfile
                                   :treat-no-verdict-as "bogus"
                                   :java-bin "/fake/java"})]
    (is (result/rejected? r))
    (is (= :invalid-treat-no-verdict-as (:category r)))))

;; ---- ADR-0015: `ehrt gate v2-nist` -- the profile-tier NIST engine
;; reaches the CLI. Real, engine-in-the-loop coverage against the
;; committed CDC fixture (components/corpus/test-fixtures/v2-nist/) --
;; hermetic in the sense that it touches no network (the jars already
;; resolved into ~/.m2 for every other v2-nist test in this workspace)
;; but genuinely runs the validator, matching ADR-0012's own measured
;; numbers exactly (473 findings, :no-verdict/:profile-spec-error). ----

(def ^:private v2-nist-profile-dir "components/corpus/test-fixtures/v2-nist/COVID19_ELR-v2.3.1")
(def ^:private v2-nist-message-file "components/corpus/test-fixtures/v2-nist/covidELR/231HL7TestFilewithHHSData.txt")

(deftest v2-nist-gate-command-requires-profile-test
  (let [r (cli/v2-nist-gate-command {:path v2-nist-message-file})]
    (is (result/rejected? r))
    (is (= :v2-nist-profile-required (:category r)))
    (is (clojure.string/includes? (:hint (:payload r)) v2-nist-profile-dir)
        "the rejection names the committed CDC fixture as the try-it bundle")))

(deftest v2-nist-gate-command-bad-profile-dir-is-a-named-error-not-a-crash-test
  (let [r (cli/v2-nist-gate-command {:path v2-nist-message-file :profile (temp-dir*)})]
    (is (result/error? r))
    (is (= :v2-nist-profile-error (:category r)))
    (is (clojure.string/includes? (:message (:payload r)) "PROFILE.xml")
        "the engine's own PROFILE.xml-required message surfaces, not a stack trace")))

(deftest v2-nist-gate-command-file-happy-path-against-cdc-fixture-test
  (let [r (cli/v2-nist-gate-command {:path v2-nist-message-file :profile v2-nist-profile-dir})]
    (is (result/rejected? r))
    (is (= :gate-no-verdict (:category r)))
    (let [file-entry (first (:files (:payload r)))]
      (is (= :no-verdict (:verdict file-entry)))
      (is (= :profile-spec-error (:cause file-entry)))
      (is (= 473 (:finding-count file-entry))
          "matches ADR-0012's own measured finding count for this exact fixture"))))

(deftest v2-nist-gate-command-dir-happy-path-test
  (let [dir (temp-dir*)
        _ (spit (io/file dir "msg-000.hl7") (slurp v2-nist-message-file))
        r (cli/v2-nist-gate-command {:path dir :profile v2-nist-profile-dir})]
    (is (result/rejected? r))
    (is (= :gate-no-verdict (:category r)))
    (is (= 1 (count (:files (:payload r)))))
    (is (clojure.string/ends-with? (:path (first (:files (:payload r)))) "msg-000.hl7"))))

(deftest v2-nist-gate-command-builds-validator-exactly-once-per-invocation-test
  (let [dir (temp-dir*)
        _ (spit (io/file dir "msg-000.hl7") (slurp v2-nist-message-file))
        _ (spit (io/file dir "msg-001.hl7") (slurp v2-nist-message-file))
        build-calls (atom 0)
        r (cli/v2-nist-gate-command
           {:path dir :profile v2-nist-profile-dir
            :make-validator-fn (fn [profile]
                                  (swap! build-calls inc)
                                  (gate-v2-nist/make-validator profile))})]
    (is (result/rejected? r))
    (is (= :gate-no-verdict (:category r)))
    (is (= 2 (count (:files (:payload r)))) "both files in the directory were gated")
    (is (= 1 @build-calls) "the validator is built once per invocation, not once per file")))

(deftest v2-nist-gate-command-missing-file-is-a-named-error-not-a-crash-test
  ;; Pins the CLI's missing-file behavior byte-identical across the
  ;; judge-family parity pass (P2-2, ruled 2026-07-31): the engine's
  ;; own gate-file now returns kernel/error :file-not-found directly
  ;; (it used to be v2-nist-gate-file*'s own .isFile pre-check, dropped
  ;; once the component started behaving) -- exit code and category are
  ;; unchanged from the user's perspective either way.
  (let [r (cli/v2-nist-gate-command {:path "/no/such/file.hl7" :profile v2-nist-profile-dir})]
    (is (result/error? r))
    (is (= :file-not-found (:category r)))
    (is (= "/no/such/file.hl7" (:path (:payload r))))
    (is (= 2 (cli/result->exit-code r)))))

(deftest gate-v2-command-baseline-mode-writes-the-baseline-relative-report-when-requested-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.hl7") (slurp "components/corpus/test-fixtures/v2/adt-a01-admit.hl7"))
        baseline-file (str (temp-dir*) "/baseline.edn")
        _ (spit baseline-file (pr-str (:payload (cli/gate-v2-command {:path in-dir}))))
        out-file (str (temp-dir*) "/relative-report.edn")
        r (cli/gate-v2-command {:path in-dir :baseline baseline-file :report out-file})]
    (is (result/ok? r))
    (is (.exists (io/file out-file)))
    (is (= (:payload r) (clojure.edn/read-string (slurp out-file))))))

(deftest gate-v2-command-baseline-mode-report-creates-missing-parent-directories-test
  ;; --baseline mode is its own write site (the payload's shape differs);
  ;; it gets the same parent creation, not a second convention.
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.hl7") (slurp "components/corpus/test-fixtures/v2/adt-a01-admit.hl7"))
        baseline-file (str (temp-dir*) "/baseline.edn")
        _ (spit baseline-file (pr-str (:payload (cli/gate-v2-command {:path in-dir}))))
        out-file (str (temp-dir*) "/nested/deeper/relative-report.edn")
        r (cli/gate-v2-command {:path in-dir :baseline baseline-file :report out-file})]
    (is (result/ok? r))
    (is (.exists (io/file out-file)))
    (is (= (:payload r) (clojure.edn/read-string (slurp out-file))))))

;; ---- check-command (`ehrt check`): reads --assertions as an EDN file,
;; parses --canonicalizers "id@v,..." into ordered [id version] pairs,
;; delegates to ehrt.corpus.interface/check-corpus. ----

(deftest check-command-matches-expected-happy-path-test
  (let [cand-dir (temp-dir*) exp-dir (temp-dir*)
        bundle "{\"resourceType\":\"Bundle\"}"
        _ (spit (io/file cand-dir "a.json") bundle)
        _ (spit (io/file exp-dir "a.json") bundle)
        r (cli/check-command {:path cand-dir :expected exp-dir})]
    (is (result/ok? r))
    (is (= {:pass 1 :rejected 0 :indeterminate 0 :no-verdict 0} (:totals (:payload r))))))

(deftest check-command-rejects-when-corpora-differ-test
  (let [cand-dir (temp-dir*) exp-dir (temp-dir*)
        _ (spit (io/file cand-dir "a.json") "{\"resourceType\":\"Bundle\",\"x\":1}")
        _ (spit (io/file exp-dir "a.json") "{\"resourceType\":\"Bundle\",\"x\":2}")
        r (cli/check-command {:path cand-dir :expected exp-dir})]
    (is (result/rejected? r))
    (is (= :check-rejected (:category r)))
    (is (= 1 (cli/result->exit-code r)))))

(deftest check-command-reads-assertions-file-test
  (let [cand-dir (temp-dir*)
        _ (spit (io/file cand-dir "a.json") "{\"resourceType\":\"Bundle\"}")
        assertions-file (str (temp-dir*) "/assertions.edn")
        _ (spit assertions-file (pr-str [{:kind :present :locator {:format :fhir :path "resourceType"}}]))
        r (cli/check-command {:path cand-dir :assertions assertions-file})]
    (is (result/ok? r))))

(deftest check-command-parses-canonicalizers-flag-test
  (let [cand-dir (temp-dir*) exp-dir (temp-dir*)
        _ (spit (io/file cand-dir "a.json") "{\"a\":1,\"b\":2}")
        _ (spit (io/file exp-dir "a.json") "{\"a\":1,\"b\":9}")
        r (cli/check-command {:path cand-dir :expected exp-dir
                               :canonicalizers "strip-run-timestamp-suffix@1"})]
    ;; strip-run-timestamp-suffix operates on filenames, not this
    ;; content, so it doesn't make the pair equivalent -- this only
    ;; proves the flag parses and reaches check-corpus without error
    ;; (an unknown-canonicalizer rejection would be a different
    ;; :category than :check-rejected).
    (is (result/rejected? r))
    (is (= :check-rejected (:category r)))))

(deftest check-command-parses-pair-by-flag-test
  (let [cand-dir (temp-dir*) exp-dir (temp-dir*)
        _ (spit (io/file cand-dir "candidate-name.json") "{\"resourceType\":\"Bundle\"}")
        _ (spit (io/file exp-dir "expected-name.json") "{\"resourceType\":\"Bundle\"}")
        r (cli/check-command {:path cand-dir :expected exp-dir :pair-by "hash"})]
    (is (result/ok? r))))

(deftest check-command-writes-report-file-when-requested-test
  (let [cand-dir (temp-dir*) exp-dir (temp-dir*)
        bundle "{\"resourceType\":\"Bundle\"}"
        _ (spit (io/file cand-dir "a.json") bundle)
        _ (spit (io/file exp-dir "a.json") bundle)
        out-file (str (temp-dir*) "/check-report.edn")
        r (cli/check-command {:path cand-dir :expected exp-dir :report out-file})]
    (is (result/ok? r))
    (is (.exists (io/file out-file)))
    (is (= (:payload r) (clojure.edn/read-string (slurp out-file))))))

(deftest check-command-report-creates-missing-parent-directories-test
  (let [cand-dir (temp-dir*) exp-dir (temp-dir*)
        bundle "{\"resourceType\":\"Bundle\"}"
        _ (spit (io/file cand-dir "a.json") bundle)
        _ (spit (io/file exp-dir "a.json") bundle)
        out-file (str (temp-dir*) "/nested/deeper/check-report.edn")
        r (cli/check-command {:path cand-dir :expected exp-dir :report out-file})]
    (is (result/ok? r))
    (is (.exists (io/file out-file)))
    (is (= (:payload r) (clojure.edn/read-string (slurp out-file))))))

(deftest check-command-report-write-failure-is-categorized-not-thrown-test
  (let [cand-dir (temp-dir*) exp-dir (temp-dir*)
        bundle "{\"resourceType\":\"Bundle\"}"
        _ (spit (io/file cand-dir "a.json") bundle)
        _ (spit (io/file exp-dir "a.json") bundle)
        blocker (str (temp-dir*) "/not-a-directory")
        _ (spit blocker "i am a file, not a directory")
        out-file (str blocker "/check-report.edn")
        r (cli/check-command {:path cand-dir :expected exp-dir :report out-file})]
    (is (result/error? r))
    (is (= :report-write-failed (:category r)))
    (is (= out-file (:path (:payload r))))
    (is (= 2 (cli/result->exit-code r)))))

(deftest dispatch-routes-check-test
  (let [called (atom nil)
        r (cli/dispatch ["check"] {:path "some-corpus/"}
                         {:check-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= {:path "some-corpus/"} @called))))

(deftest dispatch-check-accepts-a-positional-path-test
  ;; `ehrt check DIR` -- check has no sub-verb, so the second positional
  ;; arg IS the path, unlike gate's third-positional convention.
  (let [called (atom nil)
        r (cli/dispatch ["check" "some-corpus/"] {}
                         {:check-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= "some-corpus/" (:path @called)))))

(deftest dispatch-check-explicit-path-opt-not-overridden-by-positional-test
  (let [called (atom nil)
        r (cli/dispatch ["check" "positional-path"] {:path "explicit-path"}
                         {:check-fn (fn [opts] (reset! called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= "explicit-path" (:path @called)))))

(deftest gate-v2-command-propagates-operational-error-for-a-missing-path-test
  ;; A path that is neither an existing file nor directory: gate-file
  ;; on it should surface as an operational error (slurp on a missing
  ;; file throws inside execute/gate-file's own boundary today via a
  ;; FileNotFoundException -- exercised here to confirm the CLI
  ;; doesn't silently swallow it as a false "pass").
  (let [r (cli/gate-v2-command {:path "/no/such/file.hl7"})]
    (is (not (result/ok? r)))))

;; ---- D11 (docs/source-sink-design.md Part IX.4, ADR-0019): bare
;; `ehrt gate PATH` sniffs via corpus.intake/sniff-format and dispatches
;; to gate-v2/gate-fhir; `gate v2`/`gate fhir` remain explicit overrides.
;; A sniff-dispatched directory mixing both formats, or containing a
;; file the sniffer can't classify, is an operational error naming the
;; override -- not a silent per-file split (OPEN-1, resolved). ----

(deftest dispatch-gate-bare-path-sniffs-v2-test
  (let [v2-called (atom nil) fhir-called (atom nil)
        r (cli/dispatch ["gate" "components/corpus/test-fixtures/v2/adt-a01-admit.hl7"] {}
                         {:gate-v2-fn (fn [opts] (reset! v2-called opts) (result/ok {:totals {}}))
                          :gate-fhir-fn (fn [opts] (reset! fhir-called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= "components/corpus/test-fixtures/v2/adt-a01-admit.hl7" (:path @v2-called)))
    (is (nil? @fhir-called) "must not also call the fhir gate")))

(deftest dispatch-gate-bare-path-sniffs-fhir-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "patient.json") sample-bundle-json)
        v2-called (atom nil) fhir-called (atom nil)
        r (cli/dispatch ["gate" in-dir] {}
                         {:gate-v2-fn (fn [opts] (reset! v2-called opts) (result/ok {:totals {}}))
                          :gate-fhir-fn (fn [opts] (reset! fhir-called opts) (result/ok {:totals {}}))})]
    (is (result/ok? r))
    (is (= in-dir (:path @fhir-called)))
    (is (nil? @v2-called) "must not also call the v2 gate")))

(deftest dispatch-gate-explicit-verb-still-works-test
  ;; `gate v2`/`gate fhir` remain explicit overrides -- unaffected by
  ;; the sniffing dispatch added for the no-verb, bare-path case.
  (let [v2-called (atom nil)
        r (cli/dispatch ["gate" "v2" "components/corpus/test-fixtures/v2"] {}
                         {:gate-v2-fn (fn [opts] (reset! v2-called opts) (result/ok {:totals {}}))
                          :gate-fhir-fn (fn [_] (throw (ex-info "must not be called" {})))})]
    (is (result/ok? r))
    (is (= "components/corpus/test-fixtures/v2" (:path @v2-called)))))

(deftest dispatch-gate-bare-with-explicit-path-opt-sniffs-test
  ;; `ehrt gate --path X` (no positional at all) must still sniff --
  ;; the sniffing dispatch is keyed on there being no recognized verb,
  ;; not on how :path got into opts.
  (let [v2-called (atom nil)
        r (cli/dispatch ["gate"] {:path "components/corpus/test-fixtures/v2/adt-a01-admit.hl7"}
                         {:gate-v2-fn (fn [opts] (reset! v2-called opts) (result/ok {:totals {}}))
                          :gate-fhir-fn (fn [_] (throw (ex-info "must not be called" {})))})]
    (is (result/ok? r))
    (is (= "components/corpus/test-fixtures/v2/adt-a01-admit.hl7" (:path @v2-called)))))

(deftest dispatch-gate-bare-invocation-still-errors-test
  ;; `ehrt gate` with no verb, no positional, and no --path is still an
  ;; operational error -- there is nothing to sniff.
  (let [r (cli/dispatch ["gate"] {} {})]
    (is (result/error? r))
    (is (= :unknown-command (:category r)))))

(deftest sniff-gate-command-rejects-mixed-format-directory-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        _ (spit (io/file in-dir "b.hl7") (slurp "components/corpus/test-fixtures/v2/adt-a01-admit.hl7"))
        r (cli/sniff-gate-command {:path in-dir} cli/gate-v2-command cli/fhir-gate-command)]
    (is (result/error? r))
    (is (= :gate-format-ambiguous (:category r)))
    (is (= {:fhir-json 1 :v2-er7 1} (:counts (:payload r))))
    (is (re-find #"gate v2" (:hint (:payload r))))
    (is (re-find #"gate fhir" (:hint (:payload r))))))

(deftest sniff-gate-command-rejects-unclassifiable-file-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        _ (spit (io/file in-dir "junk.hl7") "not er7 at all")
        r (cli/sniff-gate-command {:path in-dir} cli/gate-v2-command cli/fhir-gate-command)]
    (is (result/error? r))
    (is (= :gate-format-ambiguous (:category r)))
    (is (some #(= "junk.hl7" %) (:unrecognized-files (:payload r))))))

(deftest sniff-gate-command-rejects-unclassifiable-single-file-test
  (let [f (str (temp-dir*) "/junk.json")
        _ (spit f "not json at all")
        r (cli/sniff-gate-command {:path f} cli/gate-v2-command cli/fhir-gate-command)]
    (is (result/error? r))
    (is (= :gate-format-ambiguous (:category r)))
    (is (= [f] (:unrecognized-files (:payload r))))))

(deftest sniff-gate-command-homogeneous-directory-dispatches-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.json") sample-bundle-json)
        _ (spit (io/file in-dir "b.json") sample-bundle-json)
        fhir-called (atom nil)
        r (cli/sniff-gate-command {:path in-dir}
                                   (fn [_] (throw (ex-info "must not be called" {})))
                                   (fn [opts] (reset! fhir-called opts) (result/ok {:totals {}})))]
    (is (result/ok? r))
    (is (= in-dir (:path @fhir-called)))))

;; ---- main! (the real -main body, refactored to take injectable
;; :dispatch-fn / :println-fn / :exit-fn so its exit-code mapping and
;; command routing can be unit-tested without a real dispatch, real
;; stdout, or -- critically -- a real System/exit that would kill the
;; test JVM) ----

(deftest main-bang-prints-rendered-result-and-exits-zero-on-ok-test
  (let [printed (atom nil)
        exit-code (atom nil)
        code (cli/main! ["artifact" "fetch" "--name" "synthea" "--version" "4.0.0"]
                         {:dispatch-fn (fn [_args _opts] (result/ok {:cached true}))
                          :println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [c] (reset! exit-code c))})]
    (is (= 0 code))
    (is (= 0 @exit-code))
    (is (clojure.string/includes? @printed ":status :ok"))))

(deftest main-bang-exits-one-on-rejected-test
  (let [exit-code (atom nil)
        code (cli/main! ["corpus" "generate"]
                         {:dispatch-fn (fn [_args _opts] (result/rejected :not-cached {}))
                          :println-fn (fn [_s] nil)
                          :exit-fn (fn [c] (reset! exit-code c))})]
    (is (= 1 code))
    (is (= 1 @exit-code))))

(deftest main-bang-exits-two-on-error-test
  (let [exit-code (atom nil)
        code (cli/main! ["bogus" "thing"]
                         {:dispatch-fn (fn [_args _opts] (result/error :unknown-command {:args ["bogus" "thing"]}))
                          :println-fn (fn [_s] nil)
                          :exit-fn (fn [c] (reset! exit-code c))})]
    (is (= 2 code))
    (is (= 2 @exit-code))))

(deftest main-bang-renders-json-when-flag-given-test
  (let [printed (atom nil)
        code (cli/main! ["artifact" "fetch" "--json"]
                         {:dispatch-fn (fn [_args opts] (is (true? (:json opts))) (result/ok {:cached true}))
                          :println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)})]
    (is (= 0 code))
    (is (clojure.string/includes? @printed "\"status\""))))

;; ---- SS-4: a :stdout-sink? true payload (mutate-to-stdout!'s own
;; marker) redirects main!'s EDN summary print to *err*, so raw framed
;; bytes already on stdout aren't corrupted by a trailing summary --
;; the real loopback integration test caught this before the redirect
;; existed. ----

(deftest main-bang-redirects-stdout-sink-result-summary-to-stderr-test
  (let [printed-via-stderr? (atom nil)
        code (cli/main! ["corpus" "mutate"]
                         {:dispatch-fn (fn [_args _opts] (result/ok {:stdout-sink? true :bytes-written 42}))
                          :println-fn (fn [_s] (reset! printed-via-stderr? (identical? *out* *err*)))
                          :exit-fn (fn [_c] nil)})]
    (is (= 0 code))
    (is (true? @printed-via-stderr?)
        "the EDN summary must print through *err*, not *out*, when raw bytes already went to stdout")))

(deftest main-bang-does-not-redirect-an-ordinary-result-test
  (let [printed-via-stderr? (atom nil)
        code (cli/main! ["artifact" "fetch"]
                         {:dispatch-fn (fn [_args _opts] (result/ok {:cached true}))
                          :println-fn (fn [_s] (reset! printed-via-stderr? (identical? *out* *err*)))
                          :exit-fn (fn [_c] nil)})]
    (is (= 0 code))
    (is (false? @printed-via-stderr?))))

(deftest main-bang-passes-real-parsed-args-to-dispatch-fn-test
  (let [captured (atom nil)
        _code (cli/main! ["corpus" "generate" "--seed" "42"]
                          {:dispatch-fn (fn [args opts] (reset! captured [args opts]) (result/ok {}))
                           :println-fn (fn [_s] nil)
                           :exit-fn (fn [_c] nil)})]
    (is (= [["corpus" "generate"] {:seed 42}] @captured))))

(deftest main-bang-default-dispatch-fn-is-the-real-dispatch-test
  ;; No :dispatch-fn override -- main! must route through the real
  ;; `dispatch`, not silently no-op, when the caller doesn't inject one.
  (let [code (cli/main! ["bogus" "thing"] {:println-fn (fn [_s] nil) :exit-fn (fn [_c] nil)})]
    (is (= 2 code))))

;; ---- main! + help (DOC-1 Step 2): plain text, not EDN/JSON, exit 0
;; for a help request, exit 2 for a bare invocation. Real `dispatch`
;; (no :dispatch-fn override) -- these prove the wiring end to end. ----

(deftest main-bang-help-prints-plain-text-not-edn-and-exits-zero-test
  (let [printed (atom nil)
        exit-code (atom nil)
        code (cli/main! ["help"]
                         {:println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [c] (reset! exit-code c))})]
    (is (= 0 code))
    (is (= 0 @exit-code))
    (is (clojure.string/includes? @printed "Usage:"))
    (is (not (clojure.string/includes? @printed ":status")))))

(deftest main-bang-help-group-prints-that-groups-usage-test
  (let [printed (atom nil)
        code (cli/main! ["help" "corpus"]
                         {:println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)})]
    (is (= 0 code))
    (is (clojure.string/includes? @printed "mutate"))))

(deftest main-bang-double-dash-help-ignores-json-flag-test
  (let [printed (atom nil)
        code (cli/main! ["gate" "--help" "--json"]
                         {:println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)})]
    (is (= 0 code))
    (is (clojure.string/includes? @printed "--treat-no-verdict-as") "ehrt gate --help --json still renders gate's own group usage")
    (is (not (clojure.string/includes? @printed "{")) "plain text, never a JSON/EDN projection, regardless of --json")))

(deftest main-bang-bare-invocation-prints-usage-and-exits-zero-test
  ;; B-5 (ux fixes 2, ADR-0060): previously main-bang-bare-invocation-
  ;; prints-usage-and-exits-two-test, asserting exit 2 -- bare
  ;; invocation now matches help/--help's own exit-0 convention.
  (let [printed (atom nil)
        exit-code (atom nil)
        code (cli/main! []
                         {:println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [c] (reset! exit-code c))})]
    (is (= 0 code))
    (is (= 0 @exit-code))
    (is (clojure.string/includes? @printed "Usage:"))))

;; ---- ADR-0013: TTY-default rendering -- render-pretty's own dispatch,
;; then main!'s :tty?-fn seam and the --pretty/--edn/--json precedence
;; over it. ----

(def ^:private sample-report
  {:run {:gate :v2 :path "some/dir"}
   :totals {:pass 1 :rejected 1 :indeterminate 0 :no-verdict 0}
   :by-code {"hl7-exception" 1}
   :files [{:path "a.hl7" :verdict :pass :finding-count 0 :findings []}
           {:path "b.hl7" :verdict :rejected :finding-count 1 :findings []}]})

(deftest render-pretty-report-shaped-payload-lists-verdicts-and-totals-test
  (let [text (cli/render-pretty (result/rejected :gate-rejected sample-report) nil)]
    (is (clojure.string/includes? text "pass  a.hl7"))
    (is (clojure.string/includes? text "rejected  b.hl7  (1 finding)"))
    (is (clojure.string/includes? text "totals:"))
    (is (clojure.string/includes? text "by-code:"))
    (is (not (clojure.string/includes? text "report written")))))

(deftest render-pretty-report-shaped-payload-names-the-report-path-when-given-test
  (let [text (cli/render-pretty (result/rejected :gate-rejected sample-report) "out/r.edn")]
    (is (clojure.string/includes? text "report written: out/r.edn"))))

(deftest render-pretty-non-report-payload-falls-back-to-generic-summary-test
  ;; Baseline mode's {:absolute :relative} payload (and anything else
  ;; that isn't Report-shaped) is this ruling's own named, permitted
  ;; skip -- a generic status/category/hint summary, never a
  ;; prettified EDN envelope.
  (let [text (cli/render-pretty (result/ok {:count 3 :out-dir "out/demo"}) nil)]
    (is (clojure.string/includes? text "ok"))
    (is (clojure.string/includes? text "count=3"))
    (is (clojure.string/includes? text "out-dir=out/demo"))
    (is (clojure.string/includes? text "--edn or --json"))))

;; ---- doctor's tailored checklist rendering (2026-07-30 doctor-
;; rendering session): the same shape-dispatch ADR-0013 sanctioned for
;; gate/check, extended to doctor's own {:checks [...]} payload. The
;; human wording is a checklist report, never "rejected" -- doctor
;; succeeded at diagnosing; the checks are what failed. The machine
;; contract (:status/:category/:payload) is untouched by any of this;
;; the envelope-equality tests below pin it against this session's own
;; captured before-state. ----

(def ^:private sample-doctor-checks
  [{:name "java resolution (via the artifact registry)" :status :pass :detail "resolved: /fake/java"}
   {:name "artifact cache (per lockfile entry)" :status :pass :detail "1 artifact(s) cached"}
   {:name "git hooksPath wiring (contribution sessions only)" :status :pass :detail "core.hooksPath = .githooks"}
   {:name "platform" :status :fail :detail "Windows 11 -- native Windows is not supported; use WSL2 (SETUP.md section 2)"}])

(deftest render-pretty-doctor-checks-payload-all-pass-lists-each-check-and-says-so-test
  (let [checks (mapv #(assoc % :status :pass :detail "Linux") sample-doctor-checks)
        text (cli/render-pretty (result/ok {:checks checks}) nil)]
    (is (clojure.string/includes? text "pass  platform -- Linux"))
    (is (clojure.string/includes? text "pass  java resolution (via the artifact registry) -- Linux"))
    (is (clojure.string/includes? text "all checks passed"))
    (is (not (clojure.string/includes? text "rejected")))))

(deftest render-pretty-doctor-checks-payload-failing-check-shows-full-detail-and-never-says-rejected-test
  (let [text (cli/render-pretty (result/rejected :doctor-checks-failed {:checks sample-doctor-checks}) nil)]
    (is (clojure.string/includes? text "pass  java resolution (via the artifact registry) -- resolved: /fake/java"))
    (is (clojure.string/includes? text "fail  platform -- Windows 11 -- native Windows is not supported; use WSL2 (SETUP.md section 2)")
        "a failing check's detail is the remedy -- shown in full, not truncated")
    (is (clojure.string/includes? text "1 of 4 check(s) failed"))
    (is (not (clojure.string/includes? text "rejected"))
        "the human wording is a checklist report, never \"rejected\"")))

(deftest render-pretty-doctor-lockfile-unreadable-falls-back-to-generic-summary-test
  ;; The exit-2 "couldn't even read the lockfile" category carries no
  ;; :checks key -- doctor-checks-payload correctly declines it and it
  ;; falls through to the generic summary, not the doctor-tailored one.
  (let [text (cli/render-pretty (result/error :not-found {:path "/no/such/lockfile.edn"}) nil)]
    (is (clojure.string/includes? text "error (not-found)"))
    (is (clojure.string/includes? text "path=/no/such/lockfile.edn"))))

(deftest doctor-command-all-pass-envelope-pinned-against-before-state-test
  ;; Pinned against this session's own captured before-state (the
  ;; render-pretty change touches only the pretty string, never the
  ;; envelope this test asserts byte-for-byte).
  (let [lockfile (temp-lockfile [(sample-artifact)])
        r (cli/doctor-command
           {:lockfile lockfile
            :resolve-java-bin-fn (fn [_artifacts _opts] (result/ok {:path "/fake/java"}))
            :resolve-artifact-fn (fn [_artifacts _name _version] (result/ok {:path "/fake/cached"}))
            :git-config-fn (fn [_key] ".githooks")
            :os-name-fn (fn [] "Linux")})]
    (is (= {:status :ok
            :payload {:checks [{:name "java resolution (via the artifact registry)" :status :pass :detail "resolved: /fake/java"}
                                {:name "artifact cache (per lockfile entry)" :status :pass :detail "1 artifact(s) cached"}
                                {:name "git hooksPath wiring (contribution sessions only)" :status :pass :detail "core.hooksPath = .githooks"}
                                {:name "platform" :status :pass :detail "Linux"}]}}
           r))))

(deftest doctor-command-checks-failed-envelope-pinned-against-before-state-test
  ;; Updated for step 3's :hint addition (2026-07-30 doctor-rendering
  ;; session, notes/prompts/2026-07-30-ehr-testing-doctor-rendering.md)
  ;; -- the pin's referent changed by ruling, same as the compat-test
  ;; precedent; :hint is additive, everything else is the pre-hint pin.
  (let [lockfile (temp-lockfile [(sample-artifact)])
        r (cli/doctor-command
           {:lockfile lockfile
            :resolve-java-bin-fn (fn [_artifacts _opts] (result/ok {:path "/fake/java"}))
            :resolve-artifact-fn (fn [_artifacts _name _version] (result/ok {:path "/fake/cached"}))
            :git-config-fn (fn [_key] ".githooks")
            :os-name-fn (fn [] "Windows 11")})]
    (is (= {:status :rejected
            :category :doctor-checks-failed
            :payload {:checks [{:name "java resolution (via the artifact registry)" :status :pass :detail "resolved: /fake/java"}
                                {:name "artifact cache (per lockfile entry)" :status :pass :detail "1 artifact(s) cached"}
                                {:name "git hooksPath wiring (contribution sessions only)" :status :pass :detail "core.hooksPath = .githooks"}
                                {:name "platform" :status :fail :detail "Windows 11 -- native Windows is not supported; use WSL2 (SETUP.md section 2)"}]
                      :hint "1 check(s) failed: platform -- run: ehrt doctor --edn for the full per-check detail"}}
           r))))

(deftest doctor-command-lockfile-unreadable-envelope-pinned-against-before-state-test
  ;; Updated for step 3's :hint addition (2026-07-30 doctor-rendering
  ;; session) -- attached at this CLI-boundary site (doctor-command),
  ;; not inside kernel/artifact's shared read-lockfile.
  (let [r (cli/doctor-command {:lockfile "/no/such/lockfile.edn"})]
    (is (= {:status :error :category :not-found
            :payload {:path "/no/such/lockfile.edn"
                      :hint "couldn't read the lockfile at /no/such/lockfile.edn -- see SETUP.md section 1"}}
           r))))

;; ---- ADR-0015: remedy hints and breadcrumbs, pretty-only, envelope
;; untouched (verified below by comparing --edn/--json output with and
;; without the pretty-only annotations present). ----

(deftest render-pretty-generic-summary-surfaces-a-payload-hint-test
  (let [text (cli/render-pretty (result/error :out-dir-exists {:out-dir "out/x" :hint "do the thing"}) nil)]
    (is (clojure.string/includes? text "do the thing"))))

(deftest generate-out-dir-exists-hint-names-the-literal-remedy-test
  ;; The shared helper itself (both generate! and generate-sim-command
  ;; raise it identically) -- rm -rf and the --out-dir alternative,
  ;; both named literally, not just "remove the directory."
  (let [r (generate/out-dir-exists-error "out/corpus/sim-s1-p1")]
    (is (result/error? r))
    (is (clojure.string/includes? (:hint (:payload r)) "rm -rf out/corpus/sim-s1-p1"))
    (is (clojure.string/includes? (:hint (:payload r)) "--out-dir"))))

(deftest generate-sim-command-out-dir-exists-hint-renders-as-the-determinism-story-in-pretty-test
  (let [out-dir (temp-dir*)
        _ (spit (io/file out-dir "stale.txt") "x")
        r (cli/generate-sim-command {:seed 1 :patients 1 :out-dir out-dir})
        text (cli/render-pretty r nil)]
    (is (result/error? r))
    (is (clojure.string/includes? text (str "rm -rf " out-dir)))
    (is (clojure.string/includes? text "refused to silently overwrite"))))

(deftest dispatch-corpus-generate-synthea-and-sim-both-carry-a-show-breadcrumb-in-pretty-test
  (doseq [args [["corpus" "generate" "synthea"] ["corpus" "generate" "sim"]]]
    (let [r (cli/dispatch args {}
                           {:generate-fn (fn [_opts] (result/ok {:out-dir "out/corpus/synthea-s1-p5"}))
                            :generate-sim-fn (fn [_opts] (result/ok {:out-dir "out/corpus/sim-s1-p1"}))})
          text (cli/render-pretty r nil)]
      (is (result/ok? r))
      (is (clojure.string/includes? text "try: bin/ehrt show out/corpus/")
          (str args " must carry a show breadcrumb"))
      (is (not (clojure.string/includes? (pr-str r) "breadcrumb"))
          "the breadcrumb is metadata, invisible to pr-str -- the EDN envelope is unaffected")
      (is (not (clojure.string/includes? (cli/render r true) "breadcrumb"))
          "and invisible to the --json projection too"))))

(deftest mutate-command-directory-write-carries-a-gate-breadcrumb-test
  (let [in-dir (temp-dir*)
        _ (spit (io/file in-dir "a.hl7") (slurp "components/corpus/test-fixtures/v2/adt-a01-admit.hl7"))
        out-dir (str (temp-dir*) "/out")
        r (cli/mutate-command {:path in-dir :operator-id "blank-required-field" :locator-path "MSH-9" :out-dir out-dir})
        text (cli/render-pretty r nil)]
    (is (result/ok? r))
    (is (clojure.string/includes? text (str "try: bin/ehrt gate " out-dir)))
    (is (not (clojure.string/includes? (pr-str r) "breadcrumb"))
        "the breadcrumb is metadata, never part of the envelope pr-str would print")))

(deftest main-bang-tty-true-defaults-to-pretty-rendering-test
  (let [printed (atom nil)
        code (cli/main! ["artifact" "fetch"]
                         {:dispatch-fn (fn [_args _opts] (result/ok {:cached true}))
                          :println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)
                          :tty?-fn (fn [] true)})]
    (is (= 0 code))
    (is (clojure.string/includes? @printed "ok"))
    (is (not (clojure.string/includes? @printed ":status")))))

(deftest main-bang-tty-false-defaults-to-edn-envelope-test
  (let [printed (atom nil)
        code (cli/main! ["artifact" "fetch"]
                         {:dispatch-fn (fn [_args _opts] (result/ok {:cached true}))
                          :println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)
                          :tty?-fn (fn [] false)})]
    (is (= 0 code))
    (is (clojure.string/includes? @printed ":status :ok"))))

(deftest main-bang-default-tty-fn-is-real-tty-and-behaves-like-a-pipe-in-tests-test
  ;; No :tty?-fn override -- the test JVM has no real console attached,
  ;; so real-tty? returns false here, matching every pre-existing test's
  ;; own EDN-envelope assertions (backward compatible by construction).
  (is (false? (cli/real-tty?))))

(deftest main-bang-pretty-flag-forces-pretty-even-when-tty-fn-false-test
  (let [printed (atom nil)
        code (cli/main! ["artifact" "fetch" "--pretty"]
                         {:dispatch-fn (fn [_args _opts] (result/ok {:cached true}))
                          :println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)
                          :tty?-fn (fn [] false)})]
    (is (= 0 code))
    (is (clojure.string/includes? @printed "ok"))
    (is (not (clojure.string/includes? @printed ":status")))))

(deftest main-bang-edn-flag-forces-edn-even-when-tty-fn-true-test
  (let [printed (atom nil)
        code (cli/main! ["artifact" "fetch" "--edn"]
                         {:dispatch-fn (fn [_args _opts] (result/ok {:cached true}))
                          :println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)
                          :tty?-fn (fn [] true)})]
    (is (= 0 code))
    (is (clojure.string/includes? @printed ":status :ok"))))

(deftest main-bang-json-flag-wins-over-tty-fn-true-unchanged-from-before-test
  ;; formats.md's own documented contract: --json is a projection,
  ;; unconditionally, regardless of what's attached to stdout -- ADR-0013
  ;; only adds the sniff for when NONE of --pretty/--edn/--json is given.
  (let [printed (atom nil)
        code (cli/main! ["artifact" "fetch" "--json"]
                         {:dispatch-fn (fn [_args _opts] (result/ok {:cached true}))
                          :println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)
                          :tty?-fn (fn [] true)})]
    (is (= 0 code))
    (is (clojure.string/includes? @printed "\"status\""))))

(deftest main-bang-help-ignores-pretty-and-edn-flags-too-test
  (let [printed (atom nil)
        code (cli/main! ["help" "--pretty"]
                         {:println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)
                          :tty?-fn (fn [] false)})]
    (is (= 0 code))
    (is (clojure.string/includes? @printed "Usage:"))))

;; ---- ADR-0013: `ehrt show` -- pretty-always, joins D11's own sniff
;; dispatch, never consults --pretty/--edn/--json/:tty?-fn. ----

(def ^:private v2-fixture-dir "components/corpus/test-fixtures/v2")

(deftest show-command-renders-a-single-v2-file-test
  (let [r (cli/show-command {:path (str v2-fixture-dir "/adt-a01-admit.hl7")})]
    (is (result/ok? r))
    (is (= :display-text (:category r)))
    (is (not (clojure.string/includes? (:text (:payload r)) "\r")))
    (is (clojure.string/includes? (:text (:payload r)) "MSH"))))

(deftest show-command-path-not-found-test
  (let [r (cli/show-command {:path "components/corpus/test-fixtures/v2/no-such-file.hl7"})]
    (is (result/error? r))
    (is (= :gate-path-not-found (:category r)))))

(deftest show-command-unrecognized-single-file-is-ambiguous-test
  (let [f (File/createTempFile "show-unrecognized" ".txt")]
    (spit f "not hl7 or fhir at all")
    (let [r (cli/show-command {:path (.getAbsolutePath f)})]
      (is (result/error? r))
      (is (= :show-format-ambiguous (:category r))))))

(deftest show-command-renders-a-directory-of-uniform-format-files-test
  (let [r (cli/show-command {:path v2-fixture-dir})]
    (is (result/ok? r))
    (is (= :display-text (:category r)))
    (is (not (clojure.string/includes? (:text (:payload r)) "\r")))))

(deftest main-bang-show-prints-text-verbatim-regardless-of-flags-and-tty-test
  (let [printed (atom nil)
        code (cli/main! ["show" v2-fixture-dir "--json" "--edn"]
                         {:dispatch-fn (fn [_args _opts]
                                         (assoc (result/ok {}) :category :display-text
                                                :payload {:text "MSH\nPID"}))
                          :println-fn (fn [s] (reset! printed s))
                          :exit-fn (fn [_c] nil)
                          :tty?-fn (fn [] true)})]
    (is (= 0 code))
    (is (= "MSH\nPID" @printed))))

(deftest dispatch-show-positional-path-binds-like-checks-test
  (let [captured (atom nil)
        r (cli/dispatch ["show" (str v2-fixture-dir "/adt-a01-admit.hl7")] {}
                         {:show-fn (fn [opts] (reset! captured opts) (result/ok {}))})]
    (is (result/ok? r))
    (is (= (str v2-fixture-dir "/adt-a01-admit.hl7") (:path @captured)))))

;; ---- ADR-0014: `ehrt play` -- executor, sinks, and the CLI verb ----

(defn- fixture-content
  [name]
  (slurp (io/file v2-fixture-dir name)))

(defn- two-message-blob
  "Two real fixtures, joined the same way ehrt.corpus-io.framing's
  own :er7-multi encode would -- adt-a01 (MSH-7 20260715142300) then
  adt-a02 (MSH-7 20260715153015), 4035s apart."
  []
  (str (fixture-content "adt-a01-admit.hl7") "\n\n" (fixture-content "adt-a02-transfer.hl7")))

(defn- temp-file-with-content
  [content]
  (let [f (File/createTempFile "ehrt-play-test" ".hl7")]
    (spit f content)
    (.getAbsolutePath f)))

(deftest dispatch-play-positional-path-binds-like-checks-test
  (let [captured (atom nil)
        r (cli/dispatch ["play" "some/file.hl7"] {}
                         {:play-fn (fn [opts] (reset! captured opts) (result/ok {}))})]
    (is (result/ok? r))
    (is (= "some/file.hl7" (:path @captured)))))

(deftest play-command-path-not-found-test
  (let [r (cli/play-command {:path "components/corpus/test-fixtures/v2/no-such-file.hl7"})]
    (is (result/error? r))
    (is (= :gate-path-not-found (:category r)))))

;; ---- ADR-0015: `play` accepts a directory of files sharing the
;; sniffed v2 format, concatenated in lexical filename order. FHIR
;; directories, mixed directories, and empty ones remain
;; :play-input-unsupported -- the same shape D11's sniff dispatch
;; already uses, never a silent per-file split. ----

(deftest play-command-directory-input-concatenates-in-lexical-order-test
  ;; The strongest available assertion (per this session's own plan):
  ;; a directory of msg-000/msg-001 paces IDENTICALLY -- same sleeps,
  ;; same rendered output -- to the pre-`cat`-ed single file of the
  ;; same two messages in the same order.
  (let [dir (temp-dir*)
        _ (spit (io/file dir "msg-000.hl7") (fixture-content "adt-a01-admit.hl7"))
        _ (spit (io/file dir "msg-001.hl7") (fixture-content "adt-a02-transfer.hl7"))
        cat-file (temp-file-with-content (two-message-blob))
        slept-dir (atom []) printed-dir (atom [])
        slept-file (atom []) printed-file (atom [])
        r-dir (cli/play-command {:path dir :rate 1 :idle-cap 1e7
                                  :sleep-fn (fn [ms] (swap! slept-dir conj ms))
                                  :println-fn (fn [s] (swap! printed-dir conj s))})
        r-file (cli/play-command {:path cat-file :rate 1 :idle-cap 1e7
                                   :sleep-fn (fn [ms] (swap! slept-file conj ms))
                                   :println-fn (fn [s] (swap! printed-file conj s))})]
    (is (result/ok? r-dir))
    (is (result/ok? r-file))
    (is (= @slept-file @slept-dir) "directory input paces identically to the pre-cat file")
    (is (= @printed-file @printed-dir) "directory input renders identically to the pre-cat file")
    (is (= (:emitted (:payload r-file)) (:emitted (:payload r-dir))))))

(deftest play-command-directory-input-order-is-lexical-not-content-test
  ;; a-second.hl7 (adt-a02, the LATER MSH-7 timestamp) is named to sort
  ;; before b-first.hl7 (adt-a01, the EARLIER one) -- the directory
  ;; listing's own order wins; play never sorts by MSH-7.
  (let [dir (temp-dir*)
        _ (spit (io/file dir "a-second.hl7") (fixture-content "adt-a02-transfer.hl7"))
        _ (spit (io/file dir "b-first.hl7") (fixture-content "adt-a01-admit.hl7"))
        printed (atom [])
        r (cli/play-command {:path dir :rate 1e15 :idle-cap 1e7
                              :sleep-fn (fn [_ms] nil)
                              :println-fn (fn [s] (swap! printed conj s))})
        rendered (clojure.string/join "\n" @printed)
        a02-index (clojure.string/index-of rendered "A02")
        a01-index (clojure.string/index-of rendered "A01")]
    (is (result/ok? r))
    (is (and a02-index a01-index (< a02-index a01-index))
        "a-second.hl7's own A02 message must render before b-first.hl7's own A01")))

(deftest play-command-directory-input-empty-is-unsupported-test
  (let [r (cli/play-command {:path (temp-dir*)})]
    (is (result/error? r))
    (is (= :play-input-unsupported (:category r)))))

(deftest play-command-directory-input-fhir-only-is-unsupported-test
  (let [dir (temp-dir*)
        _ (spit (io/file dir "patient.json") "{\"resourceType\": \"Patient\"}")
        r (cli/play-command {:path dir})]
    (is (result/error? r))
    (is (= :play-input-unsupported (:category r)))))

(deftest play-command-directory-input-mixed-formats-is-unsupported-test
  (let [dir (temp-dir*)
        _ (spit (io/file dir "msg-000.hl7") (fixture-content "adt-a01-admit.hl7"))
        _ (spit (io/file dir "patient.json") "{\"resourceType\": \"Patient\"}")
        r (cli/play-command {:path dir})]
    (is (result/error? r))
    (is (= :play-input-unsupported (:category r)))))

(deftest play-command-invalid-rate-is-rejected-test
  (let [r (cli/play-command {:path (temp-file-with-content (fixture-content "adt-a01-admit.hl7")) :rate -1.0})]
    (is (result/rejected? r))
    (is (= :invalid-rate (:category r)))))

(deftest play-command-invalid-idle-cap-is-rejected-test
  (let [r (cli/play-command {:path (temp-file-with-content (fixture-content "adt-a01-admit.hl7")) :idle-cap 0.0})]
    (is (result/rejected? r))
    (is (= :invalid-idle-cap (:category r)))))

(deftest play-command-unsupported-sink-kind-is-rejected-test
  (let [r (cli/play-command {:path (temp-file-with-content (two-message-blob))
                              :sink "dir:./wherever"
                              :sleep-fn (fn [_ms] nil)})]
    (is (result/error? r))
    (is (= :play-sink-kind-unsupported (:category r)))))

(deftest play-command-ticker-full-executes-recorded-sleeps-in-order-test
  (let [slept (atom [])
        printed (atom [])
        r (cli/play-command {:path (temp-file-with-content (two-message-blob))
                              :rate 1
                              :idle-cap 1e7 ;; seconds -- comfortably bigger than the real 4035s delta below, so nothing is capped
                              :sleep-fn (fn [ms] (swap! slept conj ms))
                              :println-fn (fn [s] (swap! printed conj s))})]
    (is (result/ok? r))
    (is (= [0 4035000] @slept) "the second message's own wait is the real 4035s MSH-7 delta, undivided at rate 1")
    (is (= 2 (:emitted (:payload r))))
    (is (some #(clojure.string/includes? % "MSH") @printed) "the ticker's full mode renders a real block")))

(deftest play-command-ticker-line-mode-renders-compact-lines-test
  (let [printed (atom [])
        r (cli/play-command {:path (temp-file-with-content (two-message-blob))
                              :rate 1e15
                              :ticker "line"
                              :sleep-fn (fn [_ms] nil)
                              :println-fn (fn [s] (swap! printed conj s))})]
    (is (result/ok? r))
    (is (= 2 (count @printed)))
    (is (every? #(clojure.string/includes? % "ADT^A0") @printed))
    (is (every? #(clojure.string/includes? % "^^^CGH^MR") @printed) "PID-3 present in the compact line")))

(deftest play-command-at-huge-rate-matches-show-identity-test
  ;; ehrt play at an arbitrarily large --rate, ticker sink, renders the
  ;; identical full-block text `ehrt show` would for the same content
  ;; (ADR-0013/ADR-0014's own identity) -- checked directly rather than
  ;; merely asserted.
  (let [path (temp-file-with-content (fixture-content "adt-a01-admit.hl7"))
        play-printed (atom [])
        play-result (cli/play-command {:path path :rate 1e15
                                        :sleep-fn (fn [_ms] nil)
                                        :println-fn (fn [s] (swap! play-printed conj s))})
        show-result (cli/show-command {:path path})]
    (is (result/ok? play-result))
    (is (result/ok? show-result))
    (is (= (clojure.string/trim (:text (:payload show-result)))
           (clojure.string/trim (clojure.string/join "\n" @play-printed))))))

(deftest play-command-skip-cue-appears-in-ticker-stream-not-as-a-message-test
  (let [printed (atom [])
        r (cli/play-command {:path (temp-file-with-content (two-message-blob))
                              :rate 1 :idle-cap 5
                              :sleep-fn (fn [_ms] nil)
                              :println-fn (fn [s] (swap! printed conj s))})]
    (is (result/ok? r))
    (is (= 1 (:skip-count (:payload r))))
    (is (some #(clojure.string/includes? % "idle-skip") @printed)
        "the cue reaches the ticker's own stream")))

(deftest play-command-file-sink-writes-byte-identical-to-unpaced-content-test
  (let [in-path (temp-file-with-content (two-message-blob))
        out-path (str in-path ".out")
        cue-lines (atom [])
        r (cli/play-command {:path in-path
                              :rate 1e15 ;; no real waits -- keeps this test fast without faking sleep
                              :sink (str "file:" out-path)
                              :sleep-fn (fn [_ms] nil)
                              :println-fn (fn [s] (swap! cue-lines conj s))})]
    (is (result/ok? r))
    ;; framing/encode-er7-multi trails EVERY item (including the last)
    ;; with its own separator -- an unpaced batch encode over the same
    ;; 2 events would produce this exact trailing "\n\n" too, so this
    ;; is the real byte-identity target, not the bare source bytes.
    (is (= (str (two-message-blob) "\n\n") (slurp out-path))
        "N single-event sink writes, in order, equal the unpaced batch content byte-for-byte")
    (is (empty? @cue-lines) "no cue at rate 1e15 with no cap triggered, and no ticker text either -- stdout stays exactly the sink's own summary, never ticker prose")))

(deftest play-command-skip-cue-with-a-data-sink-goes-to-stderr-not-the-sink-test
  (let [in-path (temp-file-with-content (two-message-blob))
        out-path (str in-path ".out2")
        err-sw (java.io.StringWriter.)]
    (binding [*err* err-sw]
      (let [r (cli/play-command {:path in-path
                                  :rate 1 :idle-cap 5
                                  :sink (str "file:" out-path)
                                  :sleep-fn (fn [_ms] nil)})]
        (is (result/ok? r))
        (is (= (str (two-message-blob) "\n\n") (slurp out-path))
            "the cue never enters the sink's own bytes")
        (is (clojure.string/includes? (str err-sw) "idle-skip")
            "the cue still reaches stderr, per the cue rule")))))

(deftest play-command-summary-envelope-carries-every-documented-field-test
  (let [r (cli/play-command {:path (temp-file-with-content (two-message-blob))
                              :rate 1 :idle-cap 100000
                              :sleep-fn (fn [_ms] nil)
                              :println-fn (fn [_s] nil)})]
    (is (result/ok? r))
    (is (= #{:emitted :clamped-count :unparseable-count :skip-count
             :rate :idle-cap-ms :wallclock-ms :stream-span-ms :sink}
           (set (keys (:payload r)))))
    (is (= 4035000 (:stream-span-ms (:payload r))))))
