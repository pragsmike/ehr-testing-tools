(ns ehr-testing-tools.cli
  "The `ehr` entrypoint (ADR-0004) -- the only namespace that prints.
  A thin shell: parse, call the capability function, print, map the
  result to an exit code. EDN is canonical output; --json is a
  projection, never the source of truth. One deliberate exception
  (DOC-1): `ehr help`, `ehr help <group>`, and `--help` anywhere print
  plain human-readable usage text instead of EDN/JSON -- they're for a
  human or an AI assistant at a shell, not a pipeline, so the EDN-out
  convention doesn't serve them. `dispatch` marks these results
  `:category :cli-help`; `main!` prints their `:text` payload verbatim
  rather than passing them through `render`."
  (:require [babashka.cli :as cli]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.artifact :as artifact]
            [ehr-testing-tools.cli.help :as help]
            [ehr-testing-tools.corpus.generate :as generate]
            [ehr-testing-tools.corpus.mutate :as mutate]
            [ehr-testing-tools.corpus.intake :as intake]
            [ehr-testing-tools.corpus.operators :as operators]
            [ehr-testing-tools.locator :as locator]
            [ehr-testing-tools.check :as check]
            [ehr-testing-tools.judge.v2 :as gate-v2]
            [ehr-testing-tools.judge.fhir :as gate-fhir]
            [ehr-testing-tools.judge.report :as report])
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

(def no-verdict-exit-code
  "Full exit-code mapping (ADR-0004, extended by ADR-0010 for the
  fourth verdict arm): 0 ok, 1 rejected, 2 operational error, 3 a
  gate's aggregate contains :no-verdict under the default (undecided)
  --treat-no-verdict-as policy. Distinct from 1 so no workflow silently
  inherits a no-verdict-handling default (the policy-totality law, D10)
  -- `ehr gate ... --treat-no-verdict-as pass|rejected` is the explicit
  opt-in to fold :no-verdict into an existing polarity instead."
  3)

(defn result->exit-code
  "0 = ran and passed; 1 = ran and legitimately rejected; 2 = operational
  error; 3 = a gate's aggregate contains :no-verdict under the default
  policy (see `no-verdict-exit-code`). Per ADR-0004's CLI exit-code
  contract, extended by ADR-0010."
  [r]
  (cond
    (result/ok? r) 0
    (= :gate-no-verdict (:category r)) no-verdict-exit-code
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

(def ^:private format-file-extension
  "The file extension `ehr corpus mutate` selects :input files by, and
  reads/writes base-data/mutant through, per operator :format."
  {:fhir "json" :v2 "hl7"})

(defn- files-with-extension-in
  "A directory -> its *.<ext> files, sorted for deterministic
  processing order; a file -> itself, as a single-element seq."
  [path ext]
  (let [f (io/file path)]
    (if (.isDirectory f)
      (->> (.listFiles f)
           (filter #(str/ends-with? (.getName %) (str "." ext)))
           (sort-by #(.getName %)))
      [f])))

(defn- read-base-data
  "Reads a file into the shape corpus.mutate expects for format:
  parsed JSON data for :fhir, the raw ER7 string for :v2."
  [format file]
  (case format
    :fhir (json/read-str (slurp file))
    :v2 (slurp file)))

(defn- write-mutant
  "Writes a mutant to file in the shape it needs to land on disk:
  JSON-serialized for :fhir (mutant is data); verbatim for :v2 (mutant
  is already the serialized ER7 string, corpus.mutate/mutate-v2's own
  return shape)."
  [format file mutant]
  (case format
    :fhir (spit file (json/write-str mutant))
    :v2 (spit file mutant)))

(defn mutate-command
  "`ehr corpus mutate`: applies one operator, at one locator, to every
  matching file under :input (a file or a directory) -- *.json for a
  :fhir operator, *.hl7 for a :v2 one, dispatched on the looked-up
  operator's own :format -- writing each mutant alongside a lineage EDN
  sidecar under :output-dir/lineage/ (a subdirectory, not interleaved
  sidecars -- chosen so a downstream stage can glob :output-dir for
  data and :output-dir/lineage for provenance without filtering one out
  of the other). Fails fast: the first file whose locator doesn't
  resolve, or any other per-file failure, is returned as-is and stops
  the batch -- partial output on disk from files already processed
  before the failure is left in place (not rolled back), since it's
  individually valid.

  Options: :input, :operator-id (a string, coerced to keyword),
  :operator-version (default \"1\"), :locator-path, :output-dir."
  [{:keys [input operator-id operator-version locator-path output-dir]
    :or {operator-version "1"}}]
  (let [operator (operators/lookup (keyword operator-id) operator-version)]
    (if-not operator
      (result/rejected :unknown-operator {:id operator-id :version operator-version})
      (let [format (:format operator)
            locator-result (locator/make format locator-path)]
        (if-not (result/ok? locator-result)
          locator-result
          (let [locator-envelope (:payload locator-result)
                files (files-with-extension-in input (format-file-extension format))]
            (.mkdirs (io/file output-dir))
            (.mkdirs (io/file output-dir "lineage"))
            (loop [remaining files processed []]
              (if (empty? remaining)
                (result/ok {:count (count processed) :files processed})
                (let [f (first remaining)
                      base-data (read-base-data format f)
                      mutate-result (mutate/mutate base-data operator locator-envelope)]
                  (if-not (result/ok? mutate-result)
                    mutate-result
                    (let [{:keys [mutant lineage]} (:payload mutate-result)
                          basename (.getName f)]
                      (write-mutant format (io/file output-dir basename) mutant)
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

(defn- parse-treat-no-verdict-as
  "\"pass\" / \"rejected\" / nil -> result/ok :pass|:rejected|nil, or
  result/rejected :invalid-treat-no-verdict-as for anything else -- the
  CLI's own flag-validation boundary (ADR-0004: exceptions are for
  programmer error, not a bad CLI argument)."
  [s]
  (case s
    nil (result/ok nil)
    "pass" (result/ok :pass)
    "rejected" (result/ok :rejected)
    (result/rejected :invalid-treat-no-verdict-as {:value s})))

(defn- gate-decision
  "The policy-totality law (ADR-0010, D10) made total in code: :ok,
  :rejected, or :no-verdict, given a report's totals and the resolved
  :treat-no-verdict-as policy (nil, :pass, or :rejected). A nil policy
  with any :no-verdict present is its own distinct outcome -- never
  silently folded into :ok or :rejected -- which is exactly what makes
  the CLI's default exit code for that case (`no-verdict-exit-code`)
  honest rather than an arbitrary pick."
  [{:keys [rejected no-verdict]} treat-no-verdict-as]
  (cond
    (and (pos? no-verdict) (nil? treat-no-verdict-as)) :no-verdict
    (and (pos? no-verdict) (= treat-no-verdict-as :rejected)) :rejected
    (pos? rejected) :rejected
    :else :ok))

(defn- decision->result
  [decision payload]
  (case decision
    :ok (result/ok payload)
    :rejected (result/rejected :gate-rejected payload)
    :no-verdict (result/rejected :gate-no-verdict payload)))

(defn gate-command
  "Builds an `ehr gate <format>` command function from that format's
  gate-file/gate-dir functions (ehr-testing-tools.judge.v2, and
  eventually judge.fhir -- same shape). :path may name a single file or
  a directory; either way the result is normalized into one
  judge.report (gate-label identifies which gate ran, in :run). Writes
  the report to :report when given (EDN, canonical -- ADR-0004).

  :baseline (P6, a path to a previously-written --report EDN file)
  switches to baseline-relative mode (ehr-testing-tools.judge.report/
  baseline-relative-report): the written/returned payload becomes
  {:absolute :relative} instead of a bare Report, and the exit-code
  decision below follows :relative's totals, not :absolute's -- see
  docs/judge-calibration.md for when to reach for this and its exact-
  match limitation. (:relative verdicts are always binary, so
  :no-verdict never actually appears there in practice -- `gate-decision`
  is applied uniformly anyway, for one policy-totality law rather than
  two near-duplicate ones.)

  :treat-no-verdict-as (ADR-0010, D10) is \"pass\" or \"rejected\" (a
  string, validated by `parse-treat-no-verdict-as` before anything else
  runs); anything else is rejected with :invalid-treat-no-verdict-as.

  Exit-code contract (ADR-0004's generic ok/rejected/error mapping,
  extended by ADR-0010 -- see `result->exit-code`): result/ok when the
  aggregate has zero rejected files and zero no-verdict files;
  result/rejected :gate-rejected the moment any file was rejected (or
  --treat-no-verdict-as rejected folds a no-verdict file in);
  result/rejected :gate-no-verdict when the aggregate has a no-verdict
  file and no --treat-no-verdict-as policy was given -- the CLI's own
  distinct exit code for that case, so no workflow silently inherits a
  no-verdict-handling default."
  [gate-file-fn gate-dir-fn gate-label]
  (fn [{:keys [path report baseline treat-no-verdict-as]}]
    (let [policy-result (parse-treat-no-verdict-as treat-no-verdict-as)]
      (if-not (result/ok? policy-result)
        policy-result
        (let [policy (:payload policy-result)
              f (io/file path)
              results-result (if (.isDirectory f)
                                (gate-dir-fn path)
                                (let [r (gate-file-fn path)]
                                  (if (result/ok? r)
                                    (result/ok {:results [(:payload r)]})
                                    r)))]
          (if-not (result/ok? results-result)
            results-result
            (let [results (:results (:payload results-result))
                  run {:gate gate-label :path path}]
              (if baseline
                (let [baseline-report (edn/read-string (slurp baseline))
                      br (report/baseline-relative-report results run baseline-report)
                      decision (gate-decision (:totals (:relative br)) policy)]
                  (when report (spit report (pr-str br)))
                  (decision->result decision br))
                (let [rpt (report/build-report results run)
                      decision (gate-decision (:totals rpt) policy)]
                  (when report (spit report (pr-str rpt)))
                  (decision->result decision rpt))))))))))

(def gate-v2-command
  (gate-command gate-v2/gate-file gate-v2/gate-dir :v2))

(def default-fhir-gate-out-dir
  "target/gate-fhir")

(defn fhir-gate-command
  "`ehr gate fhir`: unlike judge.v2 (fully self-contained, no options),
  judge.fhir needs the lockfile's artifacts plus a scratch directory
  for the validator's raw OperationOutcome output and invocation logs
  -- resolved here, then judge.fhir/gate-file and gate-dir are curried
  down to the 1-arity shape gate-command expects. :out-dir defaults to
  `target/gate-fhir` (gitignored build scratch, like `target/` already
  is for `make pipeline`); :java-bin, when given, bypasses registry
  resolution exactly like corpus.generate's own :java-bin override.
  :treat-no-verdict-as (ADR-0010) passes straight through to
  gate-command."
  [{:keys [path report lockfile out-dir java-bin baseline treat-no-verdict-as]}]
  (let [artifacts-result (default-lockfile-artifacts lockfile)]
    (if-not (result/ok? artifacts-result)
      artifacts-result
      (let [fhir-opts (cond-> {:artifacts (:payload artifacts-result)
                                :out-dir (or out-dir default-fhir-gate-out-dir)}
                        java-bin (assoc :java-bin java-bin))
            gate-fn (gate-command #(gate-fhir/gate-file % fhir-opts)
                                  #(gate-fhir/gate-dir % fhir-opts)
                                  :fhir)]
        (gate-fn {:path path :report report :baseline baseline
                  :treat-no-verdict-as treat-no-verdict-as})))))

(defn- parse-canonicalizer-steps
  "\"id@v,id2@v2\" -> [[:id \"v\"] [:id2 \"v2\"]] -- the ordered
  [id version] pairs ehr-testing-tools.canonical/apply-canonicalizers
  expects. Blank/nil -> []."
  [s]
  (if (str/blank? s)
    []
    (mapv (fn [pair]
            (let [[id version] (str/split pair #"@" 2)]
              [(keyword id) version]))
          (str/split s #","))))

(defn check-command
  "`ehr check DIR --expected DIR --assertions FILE
  [--canonicalizers id@v,...] [--pair-by path|hash] [--report ...]`.
  :assertions names an EDN file holding a vector of assertion maps
  (ehr-testing-tools.check/Assertion) -- read here, the CLI's own
  impure boundary; omitted entirely (with :expected given) delegates
  straight to check/check-corpus's own default
  ([{:kind :matches-expected}]). :canonicalizers is a comma-separated
  \"id@version\" list; :pair-by is \"path\" (default) or \"hash\"."
  [{:keys [path expected assertions canonicalizers pair-by report]}]
  (let [assertions-data (when assertions (edn/read-string (slurp assertions)))
        opts (cond-> {:candidate-dir path}
               expected (assoc :expected-dir expected)
               assertions-data (assoc :assertions assertions-data)
               canonicalizers (assoc :canonicalizers (parse-canonicalizer-steps canonicalizers))
               pair-by (assoc :pair-by (keyword pair-by)))
        r (check/check-corpus opts)]
    (when report (spit report (pr-str (:payload r))))
    r))

(defn- help-text-for
  "Group usage text for a known group name, top-level usage text
  otherwise (nil group, or a name that isn't a real group -- e.g. a
  bare `ehr`, or `ehr help bogus`)."
  [group]
  (or (and group (help/render-group help/cli-spec group))
      (help/render-top-level help/cli-spec)))

(defn- help-response
  "An explicit help request (`ehr help`, `ehr help <group>`, `--help`
  anywhere): result/ok (exit 0) carrying the plain-text usage under
  :payload's :text, marked :category :cli-help so main! prints it
  verbatim instead of through `render`."
  [group]
  (assoc (result/ok {:text (help-text-for group)}) :category :cli-help))

(defn- bare-invocation-response
  "Bare `ehr` (no group at all): prints the same top-level usage text
  as `ehr help`, but stays result/error (exit 2) -- an incomplete
  invocation is operationally an error, not a help request, even
  though the text shown is identical."
  []
  (result/error :cli-help {:text (help-text-for nil)}))

(defn dispatch
  "Routes [group action] positional args to the corresponding capability
  function with opts. The -fn keys are injectable (tests use this
  to avoid real subprocesses/network); default to the real commands.

  `ehr help`, `ehr help <group>`, and `--help` (given anywhere --
  `opts`'s :help true) short-circuit before any capability function
  runs, returning a :category :cli-help result instead of routing to a
  command -- see `help-response`/`bare-invocation-response` and the ns
  docstring's EDN-out exception."
  ([args opts] (dispatch args opts {}))
  ([args opts {:keys [fetch-fn resolve-fn generate-fn mutate-fn intake-fn gate-v2-fn gate-fhir-fn check-fn]
               :or {fetch-fn fetch-command
                    resolve-fn resolve-command
                    generate-fn generate/generate!
                    mutate-fn mutate-command
                    intake-fn intake-command
                    gate-v2-fn gate-v2-command
                    gate-fhir-fn fhir-gate-command
                    check-fn check-command}}]
   (let [[group action path] args]
     (cond
       (:help opts) (help-response group)
       (= group "help") (help-response action)
       (nil? group) (bare-invocation-response)

       :else
       (let [;; `ehr gate fhir PATH|DIR` / `ehr gate v2 PATH|DIR`: PATH is
             ;; a positional third arg, not a --path flag (the CLI's other
             ;; commands are all --flag-driven, but the prompt's own gate
             ;; CLI contract is a trailing bare path -- honored here rather
             ;; than silently reinterpreted as --path). `ehr check DIR` has
             ;; no sub-verb, so its positional path is the *second* arg
             ;; (bound above as `action`), not the third. An explicit
             ;; --path opt, if given, is NOT overridden by a positional
             ;; path in either case.
             opts (cond
                    (and (= group "gate") path (not (:path opts))) (assoc opts :path path)
                    (and (= group "check") action (not (:path opts))) (assoc opts :path action)
                    :else opts)]
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
           "check" (check-fn opts)
           (result/error :unknown-command {:args args})))))))

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
  System/exit (which would kill the test JVM).

  A :category :cli-help result (help-response/bare-invocation-response
  in `dispatch`) prints its :text payload verbatim via println-fn
  instead of going through `render` -- the ns docstring's one
  deliberate EDN-out exception; --json is ignored for these regardless
  of what was passed, since there is no EDN form to project."
  ([raw-args] (main! raw-args {}))
  ([raw-args {:keys [dispatch-fn println-fn exit-fn]
              :or {dispatch-fn dispatch println-fn println exit-fn #(System/exit %)}}]
   (let [{:keys [args opts]} (parse raw-args)
         r (dispatch-fn args opts)
         code (result->exit-code r)]
     (println-fn (if (= :cli-help (:category r))
                   (:text (:payload r))
                   (render r (:json opts))))
     (exit-fn code)
     code)))

(defn -main
  [& raw-args]
  (main! raw-args))
