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
  (:import [java.time LocalDate]
           [java.lang ProcessBuilder$Redirect]))

(def cli-spec
  {:seed {:coerce :long}
   :population {:coerce :long}
   :json {:coerce :boolean}
   ;; Digit-only strings that are identifiers, not numbers -- must not be
   ;; auto-coerced to a long (which would break ProcessBuilder's String[]
   ;; args downstream in corpus.generate/invocation).
   :reference-date {:coerce :string}
   :version {:coerce :string}
   :no-verdict-cache {:coerce :boolean}
   :all {:coerce :boolean}})

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

(defn- worst-fetch-result
  "Folds a batch of per-artifact fetch Results by severity -- ok <
  rejected < error -- so one failing fetch never masks a worse one
  elsewhere in the batch (D13: 'exit worst-of'). :results always
  carries every individual outcome, in lockfile order, regardless of
  which status wins."
  [entries results]
  (let [labeled (mapv (fn [entry r] {:name (:name entry) :version (:version entry) :result r})
                       entries results)]
    (cond
      (some result/error? results) (result/error :some-fetches-failed {:results labeled})
      (some result/rejected? results) (result/rejected :some-fetches-rejected {:results labeled})
      :else (result/ok {:results labeled}))))

(defn fetch-all-command
  "`ehr artifact fetch --all`: fetches every artifact the lockfile
  names, one invocation instead of SETUP.md's own multi-fetch
  walkthrough (D13). One failing artifact does not abort the rest --
  every entry is attempted, and the aggregate result is the worst-of
  every individual outcome."
  [{:keys [lockfile]}]
  (let [artifacts-result (default-lockfile-artifacts lockfile)]
    (if-not (result/ok? artifacts-result)
      artifacts-result
      (let [entries (:payload artifacts-result)]
        (worst-fetch-result entries (mapv artifact/fetch entries))))))

(defn resolve-command
  [{:keys [name version lockfile]}]
  (let [artifacts-result (default-lockfile-artifacts lockfile)]
    (if-not (result/ok? artifacts-result)
      artifacts-result
      (artifact/resolve (:payload artifacts-result) name version))))

;; ---- D13 (docs/source-sink-design.md Part IX.6, ADR-0019): `ehr
;; version` -- an honestly-pre-release identity, never a fabricated
;; semver. ----

(def repo-identity
  "Never a semver -- this repo is pre-release (ADR-0008): no version
  tag has been cut. \"pre-release\" is the identity claim itself, not a
  placeholder for one; a real semver arrives with the first release
  tag, a separate, later change."
  "pre-release")

(defn real-git-describe
  "`git describe --always --dirty --long`, or \"unknown\" if git isn't
  on PATH, this isn't a git checkout, or the repo has no commits yet --
  `ehr version` must never fail just because it can't get git info.
  Reads stdout only, never merged with stderr: a warning git prints
  there (e.g. this WSL/NTFS checkout's own stale-fsmonitor warning,
  AUTHORS-GUIDE.md's documented workaround) must never leak into the
  reported identity string."
  []
  (try
    (let [pb (ProcessBuilder. (into-array String ["git" "describe" "--always" "--dirty" "--long"]))]
      (.redirectError pb ProcessBuilder$Redirect/DISCARD)
      (let [proc (.start pb)
            output (str/trim (slurp (.getInputStream proc)))]
        (if (and (zero? (.waitFor proc)) (seq output)) output "unknown")))
    (catch Exception _ "unknown")))

(defn version-command
  "`ehr version`: this repo's own pre-release identity (never a
  fabricated semver, D13) plus every pinned artifact's name@version
  read from the lockfile -- ADR-0005's registry, the same source
  `ehr artifact fetch`/`resolve` already read from."
  [{:keys [lockfile git-describe-fn] :or {git-describe-fn real-git-describe}}]
  (let [artifacts-result (default-lockfile-artifacts lockfile)]
    (if-not (result/ok? artifacts-result)
      artifacts-result
      (result/ok {:identity repo-identity
                  :git (git-describe-fn)
                  :artifacts (mapv #(select-keys % [:name :version]) (:payload artifacts-result))}))))

;; ---- D13 (docs/source-sink-design.md Part IX.6, ADR-0019): `ehr
;; doctor` -- runs SETUP.md's own verification ladder as checks.
;;
;; MAINTENANCE NOTE (the NAV-1 index pattern, notes/ADRs.md's own table
;; preamble): this checklist is hand-maintained against SETUP.md's
;; "Verification ladder" (section 3) and prerequisites table (section
;; 1) -- not enforced by a lint or CI check. Whenever SETUP.md's
;; prerequisites or verification steps change, update the checks below
;; in the SAME commit (and vice versa) -- the two must never silently
;; disagree about what "your setup is working" means.

(defn real-git-config
  "`git config --get key`, or nil if unset, git isn't on PATH, or
  this isn't a git checkout. Reads stdout only -- see real-git-describe's
  own docstring for why stderr is never merged in."
  [key]
  (try
    (let [pb (ProcessBuilder. (into-array String ["git" "config" "--get" key]))]
      (.redirectError pb ProcessBuilder$Redirect/DISCARD)
      (let [proc (.start pb)
            output (str/trim (slurp (.getInputStream proc)))]
        (when (and (zero? (.waitFor proc)) (seq output)) output)))
    (catch Exception _ nil)))

(defn real-os-name
  []
  (System/getProperty "os.name"))

(defn- check-java-resolution
  "SETUP.md section 1's JDK row, as a check: Synthea's own JVM must
  resolve through the artifact registry (never PATH), matching
  corpus.generate/resolve-java-bin's own contract."
  [artifacts resolve-java-bin-fn]
  (let [r (resolve-java-bin-fn artifacts {})]
    {:name "java resolution (via the artifact registry)"
     :status (if (result/ok? r) :pass :fail)
     :detail (if (result/ok? r)
               (str "resolved: " (:path (:payload r)))
               (str "not resolved -- run: ehr artifact fetch --name "
                    generate/jdk-name " --version " generate/jdk-version))}))

(defn- check-artifact-cache
  "SETUP.md section 4's walkthrough assumes every lockfile artifact is
  already cached -- this checks that directly, per entry, rather than
  waiting for a mid-walkthrough failure to reveal it."
  [artifacts resolve-artifact-fn]
  (let [per (map (fn [a] [a (resolve-artifact-fn artifacts (:name a) (:version a))]) artifacts)
        failing (remove (fn [[_ r]] (result/ok? r)) per)]
    {:name "artifact cache (per lockfile entry)"
     :status (if (empty? failing) :pass :fail)
     :detail (if (empty? failing)
               (str (count artifacts) " artifact(s) cached")
               (str (count failing) " not cached: "
                    (str/join ", " (map (fn [[a _]] (str (:name a) "@" (:version a))) failing))
                    " -- run: ehr artifact fetch --all"))}))

(defn- check-hooks-path
  "AGENTS.md's WSL-only-commit rule is hook-enforced, but only once
  `git config core.hooksPath .githooks` has been run for this clone
  (SETUP.md section 1's maintainer-tools row) -- contribution-session
  setup, not required to use the tools, but checked here since a
  doctor that can't tell you why `git commit` didn't get checked is
  less useful than one that can."
  [git-config-fn]
  (let [v (git-config-fn "core.hooksPath")]
    {:name "git hooksPath wiring (contribution sessions only)"
     :status (if (= ".githooks" v) :pass :fail)
     :detail (if (= ".githooks" v)
               "core.hooksPath = .githooks"
               "not configured -- run: git config core.hooksPath .githooks (only needed to contribute to this repo)")}))

(defn- check-platform
  "SETUP.md section 2: native Windows is not supported (no make/bash);
  WSL2, Linux, and macOS all are. Meaningful only on Windows -- this
  check's :fail arm is the one place doctor can say so before a reader
  hits `make: command not found` themselves."
  [os-name-fn]
  (let [os (os-name-fn)]
    {:name "platform"
     :status (if (str/includes? (str/lower-case os) "windows") :fail :pass)
     :detail (if (str/includes? (str/lower-case os) "windows")
               (str os " -- native Windows is not supported; use WSL2 (SETUP.md section 2)")
               os)}))

(defn doctor-command
  "`ehr doctor`: SETUP.md's verification checklist as a command.
  0 = every check passed; 1 = at least one check failed (a real,
  actionable gap); 2 = doctor couldn't even read the lockfile to know
  what to check (the same operational-error class as every other
  lockfile-reading command here)."
  [{:keys [lockfile resolve-java-bin-fn resolve-artifact-fn git-config-fn os-name-fn]
    :or {resolve-java-bin-fn generate/resolve-java-bin
         resolve-artifact-fn artifact/resolve
         git-config-fn real-git-config
         os-name-fn real-os-name}}]
  (let [artifacts-result (default-lockfile-artifacts lockfile)]
    (if-not (result/ok? artifacts-result)
      artifacts-result
      (let [artifacts (:payload artifacts-result)
            checks [(check-java-resolution artifacts resolve-java-bin-fn)
                    (check-artifact-cache artifacts resolve-artifact-fn)
                    (check-hooks-path git-config-fn)
                    (check-platform os-name-fn)]
            failing (filter #(= :fail (:status %)) checks)]
        (if (empty? failing)
          (result/ok {:checks checks})
          (result/rejected :doctor-checks-failed {:checks checks}))))))

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

(defn- default-mutate-out-dir
  "D12 (docs/source-sink-design.md Part IX.5, ADR-0019): the derived
  --out-dir, matching D9's derived-from-inputs pattern --
  <input>-mutants/<operator-id>@<version>/."
  [path operator-id operator-version]
  (str path "-mutants/" operator-id "@" operator-version "/"))

(defn mutate-command
  "`ehr corpus mutate`: applies one operator, at one locator, to every
  matching file under :path (a file or a directory, positional --
  D10) -- *.json for a :fhir operator, *.hl7 for a :v2 one, dispatched
  on the looked-up operator's own :format -- writing each mutant
  alongside a lineage EDN sidecar under :out-dir/lineage/ (a
  subdirectory, not interleaved sidecars -- chosen so a downstream
  stage can glob :out-dir for data and :out-dir/lineage for provenance
  without filtering one out of the other). Fails fast: the first file
  whose locator doesn't resolve, or any other per-file failure, is
  returned as-is and stops the batch -- partial output on disk from
  files already processed before the failure is left in place (not
  rolled back), since it's individually valid.

  Options: :path (positional PATH, or --path -- D10), :operator-id (a
  string, coerced to keyword), :operator-version (default \"1\"),
  :locator-path (D12: falls back to the looked-up operator's own
  :default-locator when omitted; still required if the operator
  declares none), :out-dir (D12: defaults to
  (default-mutate-out-dir path operator-id operator-version) when
  omitted)."
  [{:keys [path operator-id operator-version locator-path out-dir]
    :or {operator-version "1"}}]
  (let [operator (operators/lookup (keyword operator-id) operator-version)]
    (if-not operator
      (result/rejected :unknown-operator
                        {:id operator-id :version operator-version
                         :valid-options (sort (map :id (operators/entries)))
                         :hint "run: ehr corpus operators"})
      (let [format (:format operator)
            out-dir (or out-dir (default-mutate-out-dir path operator-id operator-version))
            locator-result (locator/make format (or locator-path (:default-locator operator)))]
        (if-not (result/ok? locator-result)
          locator-result
          (let [locator-envelope (:payload locator-result)
                files (files-with-extension-in path (format-file-extension format))]
            (.mkdirs (io/file out-dir))
            (.mkdirs (io/file out-dir "lineage"))
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
                      (write-mutant format (io/file out-dir basename) mutant)
                      (spit (io/file out-dir "lineage" (str basename ".lineage.edn")) (pr-str lineage))
                      (recur (rest remaining) (conj processed {:file basename :lineage-id (:id lineage)})))))))))))))

(defn intake-command
  "`ehr corpus intake`: catalogs :path (positional PATH, or --path --
  D10, replacing --source-dir) as a foreign-corpus batch labeled
  :label. Translated to corpus.intake/intake!'s own :source-dir
  parameter at this CLI-shell boundary -- D10 renames the CLI surface,
  not the capability layer's internal vocabulary. :label still names
  the batch's provenance label at this boundary (:source-label here,
  :origin on the written CatalogEntry -- ADR-0017 D6, D-c resolved
  SS-1 Step 5: the catalog field itself is :origin now, not :source;
  this CLI parameter's own name is unaffected, since it was never
  called :source). :received defaults to today (the CLI's own impure
  boundary -- corpus.intake/intake! itself never touches the wall
  clock, matching corpus.generate's :reference-date discipline)."
  [{:keys [path label out received]}]
  (intake/intake! {:source-dir path :source-label label :out out
                    :received (or received (str (LocalDate/now)))}))

(defn operators-command
  "`ehr corpus operators`: lists the registered mutation operator
  catalog (corpus.operators) -- a pure registry read, no filesystem or
  subprocess involved, so it takes no required options. :format
  (\"fhir\" or \"v2\", optional) narrows the listing to one format.
  Always result/ok (there is nothing to reject or fail here), sorted by
  format then id so the listing is diffable across runs. Dropped
  candidates -- defects probed and found unconvictable at a gate's
  current tier -- are docstring prose in corpus.operators, not
  registry data, so they never appear here; see
  docs/judge-calibration.md for those.

  :doc and :target answer different questions and both are carried
  (DOC-3's own distinction, surfaced at the shell by DOC-4): :doc is
  the edit -- what changes in the file -- and :target is the
  conformance claim -- which base-spec constraint the edited file now
  violates. A reader choosing an operator wants the first; a reader
  explaining a gate's finding wants the second."
  [{:keys [format]}]
  (let [entries (operators/entries)
        filtered (if format
                   (filter #(= (keyword format) (:format %)) entries)
                   entries)
        rows (->> filtered
                  (map (fn [{:keys [id format version locator-required? contract doc]}]
                         {:id id :format format :version version
                          :locator-required? locator-required?
                          :doc doc
                          :type (:type contract) :target (:target contract)}))
                  (sort-by (juxt :format :id)))]
    (result/ok {:operators (vec rows)})))

(defn- write-report!
  "Writes `data` to `path` as canonical EDN (ADR-0004), creating the
  path's missing parent directories first: `--report out/run/x.edn`
  names where the user wants the file, and a missing intermediate
  directory is not a mistake they should discover as a stack trace.

  Returns nil on success, so a caller can treat it as \"no problem
  here\". Any residual IO failure -- a parent that can't be created, a
  read-only directory, a full disk -- returns a categorized
  `result/error` instead: ADR-0004 reserves exceptions for programmer
  error, and an unwritable report path is an operational failure, so it
  joins DOC-1's enumerable-options error family (path, the cause's own
  message, a hint) rather than escaping as an uncaught throw."
  [path data]
  (try
    (io/make-parents (io/file path))
    (spit path (pr-str data))
    nil
    (catch java.io.IOException e
      (result/error :report-write-failed
                    {:path path
                     :message (.getMessage e)
                     :hint "check --report names a writable file path, not a directory"}))))

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
  the report to :report when given (EDN, canonical -- ADR-0004), via
  `write-report!`: missing parent directories are created, and a
  residual IO failure is returned as :report-write-failed (exit 2)
  *instead of* the verdict -- a run whose recorded output didn't land
  is an operational failure, not a judgment.

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
                      decision (gate-decision (:totals (:relative br)) policy)
                      write-error (when report (write-report! report br))]
                  (or write-error (decision->result decision br)))
                (let [rpt (report/build-report results run)
                      decision (gate-decision (:totals rpt) policy)
                      write-error (when report (write-report! report rpt))]
                  (or write-error (decision->result decision rpt)))))))))))

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
  gate-command. :no-verdict-cache (ADR-0016) disables the content-
  addressed verdict cache at the validator seam for this invocation --
  judge.fhir/gate-file's own :verdict-cache? false, the escape hatch
  for when the cache's determinism assumption is ever suspect; caching
  stays on by default."
  [{:keys [path report lockfile out-dir java-bin baseline treat-no-verdict-as no-verdict-cache]}]
  (let [artifacts-result (default-lockfile-artifacts lockfile)]
    (if-not (result/ok? artifacts-result)
      artifacts-result
      (let [fhir-opts (cond-> {:artifacts (:payload artifacts-result)
                                :out-dir (or out-dir default-fhir-gate-out-dir)}
                        java-bin (assoc :java-bin java-bin)
                        no-verdict-cache (assoc :verdict-cache? false))
            gate-fn (gate-command #(gate-fhir/gate-file % fhir-opts)
                                  #(gate-fhir/gate-dir % fhir-opts)
                                  :fhir)]
        (gate-fn {:path path :report report :baseline baseline
                  :treat-no-verdict-as treat-no-verdict-as})))))

;; ---- D11 (docs/source-sink-design.md Part IX.4, ADR-0019): bare
;; `ehr gate PATH` sniffs via corpus.intake/sniff-format instead of a
;; second sniffing mechanism. `gate v2`/`gate fhir` remain explicit
;; overrides -- required for a directory mixing both formats, or
;; containing a file the sniffer can't classify: both are the same
;; operational error (:gate-format-ambiguous, exit 2), naming the
;; override, not a silent per-file split (OPEN-1, resolved). ----

(def ^:private gate-candidate-extensions #{"json" "hl7"})

(defn- gate-candidate-files-in
  "Files directly under dir with either judge's own extension
  (judge.fhir/gate-dir's *.json, judge.v2/gate-dir's *.hl7) -- the
  exact candidate set either explicit gate would ever look at, so a
  manifest.edn/lineage/ sidecar sitting alongside gate output is never
  mistaken for an unclassifiable gate candidate."
  [dir]
  (->> (.listFiles (io/file dir))
       (filter #(.isFile %))
       (filter #(contains? gate-candidate-extensions
                            (last (str/split (.getName %) #"\."))))
       (sort-by #(.getName %))))

(def ^:private sniffed-format->gate-label
  {:fhir-json :fhir :v2-er7 :v2})

(defn- sniff-path-format
  [f]
  (get sniffed-format->gate-label (intake/sniff-format (slurp f))))

(defn- ambiguous-format-error
  [path payload]
  (result/error :gate-format-ambiguous
                (merge {:path path
                        :hint "ambiguous format -- run: ehr gate v2 PATH, or ehr gate fhir PATH"}
                       payload)))

(defn sniff-gate-command
  "The bare `ehr gate PATH` dispatch (D11): sniffs :path via
  corpus.intake/sniff-format and calls gate-v2-fn or gate-fhir-fn with
  opts unchanged -- both already handle file-vs-directory internally
  (gate-command's own .isDirectory branch), so this function's only job
  is deciding *which* of the two to call. A single unclassifiable file,
  a directory with no *.json/*.hl7 candidates, one containing any
  unclassifiable candidate, or one mixing both formats, are all the
  same :gate-format-ambiguous operational error (exit 2) -- never a
  silent per-file split."
  [{:keys [path] :as opts} gate-v2-fn gate-fhir-fn]
  (let [f (io/file path)]
    (cond
      (not (.exists f))
      (result/error :gate-path-not-found
                     {:path path
                      :hint "no such file or directory -- run: ehr help gate"})

      (.isFile f)
      (case (sniff-path-format f)
        :fhir (gate-fhir-fn opts)
        :v2 (gate-v2-fn opts)
        (ambiguous-format-error path {:unrecognized-files [path]}))

      :else
      (let [files (gate-candidate-files-in path)]
        (if (empty? files)
          (ambiguous-format-error path {:reason :no-candidate-files})
          (let [sniffed (map (fn [file] [(.getName file) (sniff-path-format file)]) files)
                unrecognized (mapv first (filter (fn [[_ fmt]] (nil? fmt)) sniffed))]
            (if (seq unrecognized)
              (ambiguous-format-error path {:unrecognized-files unrecognized})
              (let [formats (into #{} (map second) sniffed)]
                (if (> (count formats) 1)
                  (ambiguous-format-error
                   path
                   {:counts (into {}
                                  (map (fn [[fmt fs]] [(get {:fhir :fhir-json :v2 :v2-er7} fmt) (count fs)]))
                                  (group-by second sniffed))})
                  (case (first formats)
                    :fhir (gate-fhir-fn opts)
                    :v2 (gate-v2-fn opts)))))))))))

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
  \"id@version\" list; :pair-by is \"path\" (default) or \"hash\".
  :report goes through `write-report!` on the same terms as the gate's:
  parents created, a residual IO failure returned as
  :report-write-failed instead of the check's own verdict."
  [{:keys [path expected assertions canonicalizers pair-by report]}]
  (let [assertions-data (when assertions (edn/read-string (slurp assertions)))
        opts (cond-> {:candidate-dir path}
               expected (assoc :expected-dir expected)
               assertions-data (assoc :assertions assertions-data)
               canonicalizers (assoc :canonicalizers (parse-canonicalizer-steps canonicalizers))
               pair-by (assoc :pair-by (keyword pair-by)))
        r (check/check-corpus opts)
        write-error (when report (write-report! report (:payload r)))]
    (or write-error r)))

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

(defn- unknown-command-error
  "An unrecognized group or verb: :unknown-command, extended (DOC-1's
  bounded error-message pass) with :valid-options (drawn from the help
  spec -- one source of truth with `ehr help`'s own text, so the two
  can't drift apart) and a fixed :hint pointing at the fuller help
  surface."
  [args valid-options]
  (result/error :unknown-command {:args args :valid-options valid-options :hint "run: ehr help"}))

;; Cross-repo interface commitment (ADR-0012): ehr-testing-sim mounts as
;; one arm here, per its own ADR-0001. Preserve, when refactoring this
;; boundary: parsed-[group action]-in / Result-map-out dispatch; a single
;; merged babashka.cli spec parsed once, host-side; structural Result
;; typing; the help-group data shape; the -fn injection point. Manifest
;; schema changes require a version bump, and the binding contract test
;; lives in test-integration/. Read ADR-0012 (and, for provenance,
;; notes/ehr-testing-sim-mounting-note.md) before changing any of these.

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
  ([args opts {:keys [fetch-fn fetch-all-fn resolve-fn generate-fn mutate-fn intake-fn operators-fn
                       gate-v2-fn gate-fhir-fn check-fn version-fn doctor-fn]
               :or {fetch-fn fetch-command
                    fetch-all-fn fetch-all-command
                    resolve-fn resolve-command
                    generate-fn generate/generate!
                    mutate-fn mutate-command
                    intake-fn intake-command
                    operators-fn operators-command
                    gate-v2-fn gate-v2-command
                    gate-fhir-fn fhir-gate-command
                    check-fn check-command
                    version-fn version-command
                    doctor-fn doctor-command}}]
   (let [[group action path] args]
     (cond
       (:help opts) (help-response group)
       (= group "help") (help-response action)
       (nil? group) (bare-invocation-response)

       :else
       (let [;; `ehr gate fhir PATH|DIR` / `ehr gate v2 PATH|DIR` /
             ;; `ehr corpus mutate PATH` / `ehr corpus intake PATH`: PATH
             ;; is a positional third arg, with --path as its explicit
             ;; twin (D10) -- never overridden by a positional path when
             ;; --path was given explicitly. `ehr check DIR` has no
             ;; sub-verb, so its positional path is the *second* arg
             ;; (bound above as `action`), not the third.
             opts (cond
                    (and (= group "gate") path (not (:path opts))) (assoc opts :path path)
                    (and (= group "corpus") (#{"mutate" "intake"} action) path (not (:path opts))) (assoc opts :path path)
                    (and (= group "check") action (not (:path opts))) (assoc opts :path action)
                    :else opts)]
         (case group
           "artifact" (case action
                        "fetch" (if (:all opts) (fetch-all-fn opts) (fetch-fn opts))
                        "resolve" (resolve-fn opts)
                        (unknown-command-error args (help/verb-names (help/find-group help/cli-spec "artifact"))))
           "corpus" (case action
                      "generate" (generate-fn opts)
                      "mutate" (mutate-fn opts)
                      "intake" (intake-fn opts)
                      "operators" (operators-fn opts)
                      (unknown-command-error args (help/verb-names (help/find-group help/cli-spec "corpus"))))
           "gate" (cond
                    (= action "v2") (gate-v2-fn opts)
                    (= action "fhir") (gate-fhir-fn opts)
                    ;; D11: no recognized verb, but a path arrived
                    ;; either positionally (bound above as `action`) or
                    ;; via an explicit --path -- sniff-dispatch it.
                    ;; --path always wins when both are given.
                    (or action (:path opts))
                    (sniff-gate-command (assoc opts :path (or (:path opts) action))
                                         gate-v2-fn gate-fhir-fn)
                    :else
                    (unknown-command-error args (help/verb-names (help/find-group help/cli-spec "gate"))))
           "check" (check-fn opts)
           "version" (version-fn opts)
           "doctor" (doctor-fn opts)
           (unknown-command-error args (help/group-names help/cli-spec))))))))

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
