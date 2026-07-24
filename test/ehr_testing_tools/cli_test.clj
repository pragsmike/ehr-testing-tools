(ns ehr-testing-tools.cli-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.artifact :as artifact]
            [ehr-testing-tools.cli :as cli])
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

(deftest dispatch-routes-artifact-fetch-test
  (let [called (atom nil)
        r (cli/dispatch ["artifact" "fetch"] {:name "synthea" :version "4.0.0"}
                         {:fetch-fn (fn [opts] (reset! called opts) (result/ok {:cached true}))})]
    (is (result/ok? r))
    (is (= {:name "synthea" :version "4.0.0"} @called))))

(deftest dispatch-routes-artifact-resolve-test
  (let [called (atom nil)
        r (cli/dispatch ["artifact" "resolve"] {:name "synthea" :version "4.0.0"}
                         {:resolve-fn (fn [opts] (reset! called opts) (result/rejected :not-cached {}))})]
    (is (result/rejected? r))
    (is (= {:name "synthea" :version "4.0.0"} @called))))

(deftest dispatch-routes-corpus-generate-test
  (let [called (atom nil)
        r (cli/dispatch ["corpus" "generate"] {:seed 1}
                         {:generate-fn (fn [opts] (reset! called opts) (result/ok {:manifest {}}))})]
    (is (result/ok? r))
    (is (= {:seed 1} @called))))

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
    (with-redefs [artifact/resolve (fn [artifacts name version]
                                     (reset! called [artifacts name version])
                                     (result/rejected :not-cached {:name name :version version}))]
      (let [r (cli/resolve-command {:name "synthea" :version "4.0.0" :lockfile lockfile})]
        (is (result/rejected? r))
        (is (= [[art] "synthea" "4.0.0"] @called))))))

(deftest resolve-command-propagates-lockfile-read-failure-test
  (let [r (cli/resolve-command {:name "synthea" :version "4.0.0" :lockfile "/no/such/lockfile.edn"})]
    (is (result/error? r))
    (is (= :not-found (:category r)))))

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
