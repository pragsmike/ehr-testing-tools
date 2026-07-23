(ns ehr-testing-tools.cli-test
  (:require [clojure.test :refer [deftest is testing]]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.cli :as cli]))

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
