(ns ehr-testing-tools.cli
  "The `ehr` entrypoint (ADR-0004) -- the only namespace that prints.
  A thin shell: parse, call the capability function, print, map the
  result to an exit code. EDN is canonical output; --json is a
  projection, never the source of truth."
  (:require [babashka.cli :as cli]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.artifact :as artifact]
            [ehr-testing-tools.corpus.generate :as generate]
            [ehr-testing-tools.corpus.mutate :as mutate]
            [ehr-testing-tools.corpus.operators :as operators]
            [ehr-testing-tools.locator :as locator]))

(def cli-spec
  {:seed {:coerce :long}
   :population {:coerce :long}
   :json {:coerce :boolean}
   ;; Digit-only strings that are identifiers, not numbers -- must not be
   ;; auto-coerced to a long (which would break ProcessBuilder's String[]
   ;; args downstream in corpus.generate/invocation).
   :reference-date {:coerce :string}
   :version {:coerce :string}})

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

(defn- json-files-in
  "A directory -> its *.json files, sorted for deterministic
  processing order; a file -> itself, as a single-element seq."
  [path]
  (let [f (io/file path)]
    (if (.isDirectory f)
      (->> (.listFiles f)
           (filter #(str/ends-with? (.getName %) ".json"))
           (sort-by #(.getName %)))
      [f])))

(defn mutate-command
  "`ehr corpus mutate`: applies one operator, at one locator, to every
  *.json file under :input (a file or a directory), writing each
  mutant alongside a lineage EDN sidecar under :output-dir/lineage/
  (a subdirectory, not interleaved sidecars -- chosen so a downstream
  stage can glob :output-dir for data and :output-dir/lineage for
  provenance without filtering one out of the other). Fails fast: the
  first file whose locator doesn't resolve, or any other per-file
  failure, is returned as-is and stops the batch -- partial output on
  disk from files already processed before the failure is left in
  place (not rolled back), since it's individually valid.

  Options: :input, :operator-id (a string, coerced to keyword),
  :operator-version (default \"1\"), :locator-path, :output-dir."
  [{:keys [input operator-id operator-version locator-path output-dir]
    :or {operator-version "1"}}]
  (let [operator (operators/lookup (keyword operator-id) operator-version)]
    (if-not operator
      (result/rejected :unknown-operator {:id operator-id :version operator-version})
      (let [locator-result (locator/make :fhir locator-path)]
        (if-not (result/ok? locator-result)
          locator-result
          (let [locator-envelope (:payload locator-result)
                files (json-files-in input)]
            (.mkdirs (io/file output-dir))
            (.mkdirs (io/file output-dir "lineage"))
            (loop [remaining files processed []]
              (if (empty? remaining)
                (result/ok {:count (count processed) :files processed})
                (let [f (first remaining)
                      base-data (json/read-str (slurp f))
                      mutate-result (mutate/mutate base-data operator locator-envelope)]
                  (if-not (result/ok? mutate-result)
                    mutate-result
                    (let [{:keys [mutant lineage]} (:payload mutate-result)
                          basename (.getName f)]
                      (spit (io/file output-dir basename) (json/write-str mutant))
                      (spit (io/file output-dir "lineage" (str basename ".lineage.edn")) (pr-str lineage))
                      (recur (rest remaining) (conj processed {:file basename :lineage-id (:id lineage)})))))))))))))

(defn dispatch
  "Routes [group action] positional args to the corresponding capability
  function with opts. The three -fn keys are injectable (tests use this
  to avoid real subprocesses/network); default to the real commands."
  ([args opts] (dispatch args opts {}))
  ([args opts {:keys [fetch-fn resolve-fn generate-fn mutate-fn]
               :or {fetch-fn fetch-command
                    resolve-fn resolve-command
                    generate-fn generate/generate!
                    mutate-fn mutate-command}}]
   (let [[group action] args]
     (case group
       "artifact" (case action
                    "fetch" (fetch-fn opts)
                    "resolve" (resolve-fn opts)
                    (result/error :unknown-command {:args args}))
       "corpus" (case action
                  "generate" (generate-fn opts)
                  "mutate" (mutate-fn opts)
                  (result/error :unknown-command {:args args}))
       (result/error :unknown-command {:args args})))))

(defn render
  [r json?]
  (if json?
    (json/write-str r)
    (pr-str r)))

(defn main!
  "The real body of -main, with every side-effecting boundary
  injectable for testing: :dispatch-fn (default `dispatch`),
  :println-fn (default `println`), :exit-fn (default `System/exit`).
  Returns the exit code it computed -- mainly useful for tests; -main
  itself ignores the return value since :exit-fn already terminated
  the process in real use. This split is what lets -main's exit-code
  mapping and command routing be unit-tested without a real
  System/exit (which would kill the test JVM)."
  ([raw-args] (main! raw-args {}))
  ([raw-args {:keys [dispatch-fn println-fn exit-fn]
              :or {dispatch-fn dispatch println-fn println exit-fn #(System/exit %)}}]
   (let [{:keys [args opts]} (parse raw-args)
         r (dispatch-fn args opts)
         code (result->exit-code r)]
     (println-fn (render r (:json opts)))
     (exit-fn code)
     code)))

(defn -main
  [& raw-args]
  (main! raw-args))
