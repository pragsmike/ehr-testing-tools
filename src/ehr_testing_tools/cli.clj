(ns ehr-testing-tools.cli
  "The `ehr` entrypoint (ADR-0004) -- the only namespace that prints.
  A thin shell: parse, call the capability function, print, map the
  result to an exit code. EDN is canonical output; --json is a
  projection, never the source of truth."
  (:require [babashka.cli :as cli]
            [clojure.data.json :as json]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.artifact :as artifact]
            [ehr-testing-tools.corpus.generate :as generate]))

(def cli-spec
  {:seed {:coerce :long}
   :population {:coerce :long}
   :json {:coerce :boolean}})

(defn parse
  "Parses raw CLI args into {:args [positional...] :opts {...}}."
  [raw-args]
  (cli/parse-args raw-args {:spec cli-spec}))

(defn result->exit-code
  "0 = ran and passed; 1 = ran and legitimately rejected; 2 = operational
  error. Per ADR-0004's CLI exit-code contract."
  [r]
  (cond
    (result/ok? r) 0
    (result/rejected? r) 1
    :else 2))

(defn- default-lockfile-artifacts
  [lockfile-path]
  (let [r (artifact/read-lockfile (or lockfile-path "artifacts.lock.edn"))]
    (if (result/ok? r)
      (result/ok (:artifacts (:payload r)))
      r)))

(defn fetch-command
  [{:keys [name version lockfile]}]
  (let [artifacts-result (default-lockfile-artifacts lockfile)]
    (if-not (result/ok? artifacts-result)
      artifacts-result
      (let [artifact-entry (clojure.core/first
                             (filter #(and (= name (:name %)) (= version (:version %)))
                                     (:payload artifacts-result)))]
        (if-not artifact-entry
          (result/rejected :unknown-artifact {:name name :version version})
          (artifact/fetch artifact-entry))))))

(defn resolve-command
  [{:keys [name version lockfile]}]
  (let [artifacts-result (default-lockfile-artifacts lockfile)]
    (if-not (result/ok? artifacts-result)
      artifacts-result
      (artifact/resolve (:payload artifacts-result) name version))))

(defn dispatch
  "Routes [group action] positional args to the corresponding capability
  function with opts. The three -fn keys are injectable (tests use this
  to avoid real subprocesses/network); default to the real commands."
  ([args opts] (dispatch args opts {}))
  ([args opts {:keys [fetch-fn resolve-fn generate-fn]
               :or {fetch-fn fetch-command
                    resolve-fn resolve-command
                    generate-fn generate/generate!}}]
   (let [[group action] args]
     (case group
       "artifact" (case action
                    "fetch" (fetch-fn opts)
                    "resolve" (resolve-fn opts)
                    (result/error :unknown-command {:args args}))
       "corpus" (case action
                  "generate" (generate-fn opts)
                  (result/error :unknown-command {:args args}))
       (result/error :unknown-command {:args args})))))

(defn render
  [r json?]
  (if json?
    (json/write-str r)
    (pr-str r)))

(defn -main
  [& raw-args]
  (let [{:keys [args opts]} (parse raw-args)
        r (dispatch args opts)]
    (println (render r (:json opts)))
    (System/exit (result->exit-code r))))
