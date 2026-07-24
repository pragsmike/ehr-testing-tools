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
            [ehr-testing-tools.corpus.intake :as intake]
            [ehr-testing-tools.corpus.operators :as operators]
            [ehr-testing-tools.locator :as locator]
            [ehr-testing-tools.gate.v2 :as gate-v2]
            [ehr-testing-tools.gate.fhir :as gate-fhir]
            [ehr-testing-tools.gate.report :as report])
  (:import [java.time LocalDate]))

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

(defn intake-command
  "`ehr corpus intake`: catalogs :source-dir as a foreign-corpus batch
  labeled :label. :received defaults to today (the CLI's own impure
  boundary -- corpus.intake/intake! itself never touches the wall
  clock, matching corpus.generate's :reference-date discipline)."
  [{:keys [source-dir label out received]}]
  (intake/intake! {:source-dir source-dir :source-label label :out out
                    :received (or received (str (LocalDate/now)))}))

(defn gate-command
  "Builds an `ehr gate <format>` command function from that format's
  gate-file/gate-dir functions (ehr-testing-tools.gate.v2, and
  eventually gate.fhir -- same shape). :path may name a single file or
  a directory; either way the result is normalized into one
  gate.report (gate-label identifies which gate ran, in :run). Writes
  the report to :report when given (EDN, canonical -- ADR-0004).

  Exit-code contract (ADR-0004's generic ok/rejected/error mapping,
  applied here rather than special-cased): result/ok when the
  aggregate has zero rejected files -- including an indeterminate-only
  run, which is exit 0 by this rule, but :totals in the written/
  returned report says so loudly, exactly as README's gate section
  documents; result/rejected :gate-rejected the moment any file was
  rejected. `main!`'s result->exit-code needs no gate-specific
  handling: 0 all pass (indeterminate-only included), 1 any rejected,
  2 an operational error from the gate-file-fn/gate-dir-fn themselves."
  [gate-file-fn gate-dir-fn gate-label]
  (fn [{:keys [path report]}]
    (let [f (io/file path)
          results-result (if (.isDirectory f)
                            (gate-dir-fn path)
                            (let [r (gate-file-fn path)]
                              (if (result/ok? r)
                                (result/ok {:results [(:payload r)]})
                                r)))]
      (if-not (result/ok? results-result)
        results-result
        (let [results (:results (:payload results-result))
              rpt (report/build-report results {:gate gate-label :path path})]
          (when report (spit report (pr-str rpt)))
          (if (pos? (:rejected (:totals rpt)))
            (result/rejected :gate-rejected rpt)
            (result/ok rpt)))))))

(def gate-v2-command
  (gate-command gate-v2/gate-file gate-v2/gate-dir :v2))

(def default-fhir-gate-out-dir
  "target/gate-fhir")

(defn fhir-gate-command
  "`ehr gate fhir`: unlike gate.v2 (fully self-contained, no options),
  gate.fhir needs the lockfile's artifacts plus a scratch directory
  for the validator's raw OperationOutcome output and invocation logs
  -- resolved here, then gate.fhir/gate-file and gate-dir are curried
  down to the 1-arity shape gate-command expects. :out-dir defaults to
  `target/gate-fhir` (gitignored build scratch, like `target/` already
  is for `make pipeline`); :java-bin, when given, bypasses registry
  resolution exactly like corpus.generate's own :java-bin override."
  [{:keys [path report lockfile out-dir java-bin]}]
  (let [artifacts-result (default-lockfile-artifacts lockfile)]
    (if-not (result/ok? artifacts-result)
      artifacts-result
      (let [fhir-opts (cond-> {:artifacts (:payload artifacts-result)
                                :out-dir (or out-dir default-fhir-gate-out-dir)}
                        java-bin (assoc :java-bin java-bin))
            gate-fn (gate-command #(gate-fhir/gate-file % fhir-opts)
                                  #(gate-fhir/gate-dir % fhir-opts)
                                  :fhir)]
        (gate-fn {:path path :report report})))))

(defn dispatch
  "Routes [group action] positional args to the corresponding capability
  function with opts. The -fn keys are injectable (tests use this
  to avoid real subprocesses/network); default to the real commands."
  ([args opts] (dispatch args opts {}))
  ([args opts {:keys [fetch-fn resolve-fn generate-fn mutate-fn intake-fn gate-v2-fn gate-fhir-fn]
               :or {fetch-fn fetch-command
                    resolve-fn resolve-command
                    generate-fn generate/generate!
                    mutate-fn mutate-command
                    intake-fn intake-command
                    gate-v2-fn gate-v2-command
                    gate-fhir-fn fhir-gate-command}}]
   (let [[group action path] args
         ;; `ehr gate fhir PATH|DIR` / `ehr gate v2 PATH|DIR`: PATH is
         ;; a positional third arg, not a --path flag (the CLI's other
         ;; commands are all --flag-driven, but the prompt's own gate
         ;; CLI contract is a trailing bare path -- honored here rather
         ;; than silently reinterpreted as --path). An explicit --path
         ;; opt, if given, is NOT overridden by a positional path.
         opts (if (and (= group "gate") path (not (:path opts)))
                (assoc opts :path path)
                opts)]
     (case group
       "artifact" (case action
                    "fetch" (fetch-fn opts)
                    "resolve" (resolve-fn opts)
                    (result/error :unknown-command {:args args}))
       "corpus" (case action
                  "generate" (generate-fn opts)
                  "mutate" (mutate-fn opts)
                  "intake" (intake-fn opts)
                  (result/error :unknown-command {:args args}))
       "gate" (case action
                "v2" (gate-v2-fn opts)
                "fhir" (gate-fhir-fn opts)
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
