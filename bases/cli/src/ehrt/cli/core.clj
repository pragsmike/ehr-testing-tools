(ns ehrt.cli.core
  "The `ehrt` entrypoint (ADR-0004) -- the only namespace that prints.
  A thin shell: parse, call the capability function, print, map the
  result to an exit code. EDN is canonical output; --json is a
  projection, never the source of truth. Two deliberate exceptions to
  that: (DOC-1) `ehrt help`, `ehrt help <group>`, and `--help` anywhere
  print plain human-readable usage text instead of EDN/JSON -- they're
  for a human or an AI assistant at a shell, not a pipeline, so the
  EDN-out convention doesn't serve them (`dispatch` marks these results
  `:category :cli-help`); and (ADR-0013) `ehrt show` always renders for
  eyes, `:category :display-text`. `main!` prints either kind's `:text`
  payload verbatim rather than passing it through `render`/
  `render-pretty`. Beyond those two, ADR-0013 also adds a TTY-sensitive
  default for every other command: a real terminal gets a human summary
  (`render-pretty`) where a pipe/redirect still gets the unchanged EDN
  envelope -- see `main!`'s own docstring."
  (:require [babashka.cli :as cli]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ehrt.tools.interface :as result]
            [ehrt.tools.interface :as artifact]
            [ehrt.cli.help :as help]
            [ehrt.tools.interface :as generate]
            [ehrt.tools.interface :as generators]
            [ehrt.tools.interface :as mutate]
            [ehrt.tools.interface :as intake]
            [ehrt.tools.interface :as operators]
            [ehrt.tools.interface :as generator-source]
            [ehrt.tools.interface :as spool-source]
            [ehrt.tools.interface :as source-sink]
            [ehrt.tools.interface :as source-sink-url]
            [ehrt.tools.interface :as sink-write]
            [ehrt.tools.interface :as locator]
            [ehrt.tools.interface :as check]
            [ehrt.tools.interface :as gate-v2]
            [ehrt.tools.interface :as gate-fhir]
            [ehrt.tools.interface :as report]
            [ehrt.tools.interface :as sim]
            [ehrt.tools.interface :as display]
            [ehrt.tools.interface :as player])
  (:import [java.time LocalDate]
           [java.lang ProcessBuilder$Redirect]))

(def cli-spec
  {:seed {:coerce :long}
   :population {:coerce :long}
   :json {:coerce :boolean}
   ;; ADR-0013: TTY-default rendering forcing flags.
   :pretty {:coerce :boolean}
   :edn {:coerce :boolean}
   ;; ADR-0014: ehrt play's own pacing flags.
   :rate {:coerce :double}
   :idle-cap {:coerce :double}
   ;; Digit-only strings that are identifiers, not numbers -- must not be
   ;; auto-coerced to a long (which would break ProcessBuilder's String[]
   ;; args downstream in corpus.generate/invocation).
   :reference-date {:coerce :string}
   :version {:coerce :string}
   :no-verdict-cache {:coerce :boolean}
   :all {:coerce :boolean}
   ;; `ehrt sim run` (ADR-0005, ADR-0012 fulfilled): sim's own opt names,
   ;; unchanged -- ehrt.sim.interface/run-command's own :patients/
   ;; :warm-up-seconds/:churn.
   :patients {:coerce :long}
   :warm-up-seconds {:coerce :long}
   :churn {:coerce :boolean}})

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
  -- `ehrt gate ... --treat-no-verdict-as pass|rejected` is the explicit
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
  "`ehrt artifact fetch --all`: fetches every artifact the lockfile
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
      (artifact/resolve-artifact (:payload artifacts-result) name version))))

;; ---- D13 (docs/source-sink-design.md Part IX.6, ADR-0019): `ehrt
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
  `ehrt version` must never fail just because it can't get git info.
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
  "`ehrt version`: this repo's own pre-release identity (never a
  fabricated semver, D13) plus every pinned artifact's name@version
  read from the lockfile -- ADR-0005's registry, the same source
  `ehrt artifact fetch`/`resolve` already read from."
  [{:keys [lockfile git-describe-fn] :or {git-describe-fn real-git-describe}}]
  (let [artifacts-result (default-lockfile-artifacts lockfile)]
    (if-not (result/ok? artifacts-result)
      artifacts-result
      (result/ok {:identity repo-identity
                  :git (git-describe-fn)
                  :artifacts (mapv #(select-keys % [:name :version]) (:payload artifacts-result))}))))

;; ---- D13 (docs/source-sink-design.md Part IX.6, ADR-0019): `ehrt
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
               (str "not resolved -- run: ehrt artifact fetch --name "
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
                    " -- run: ehrt artifact fetch --all"))}))

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
  "`ehrt doctor`: SETUP.md's verification checklist as a command.
  0 = every check passed; 1 = at least one check failed (a real,
  actionable gap); 2 = doctor couldn't even read the lockfile to know
  what to check (the same operational-error class as every other
  lockfile-reading command here)."
  [{:keys [lockfile resolve-java-bin-fn resolve-artifact-fn git-config-fn os-name-fn]
    :or {resolve-java-bin-fn generate/resolve-java-bin
         resolve-artifact-fn artifact/resolve-artifact
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
  "The file extension `ehrt corpus mutate` selects :input files by, and
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

(def ^:private operator-format->sink-format
  "corpus.operators' own :format vocabulary (:fhir/:v2) to
  source-sink's (:fhir-json/:v2-er7) -- the same mapping
  sniff-gate-command already carries the inverse of."
  {:fhir :fhir-json :v2 :v2-er7})

(defn- mutate-producer
  "SS-4b (D-d resolved, ADR-0020): the operation manifest's own
  :producer field, this repo's honest identity via the same `ehrt
  version` machinery version-command already uses. git-describe-fn is
  injectable (mirrors version-command's own parameter) so hermetic
  tests never shell out to a real git process."
  [git-describe-fn]
  {:name "ehrt.tools" :identity repo-identity :git (git-describe-fn)})

(defn- stdout-out-dir-result
  "SS-4 ruling 6: --out-dir gains the Sink seam additively -- a bare
  path (including a derived one, D12) behaves exactly as before; a
  literal `stdout:...` designator (the one sink scheme this repo has a
  reason to route mutate's own output through this session, ruling 5)
  is parsed as a real Sink instead. The str/starts-with? check (rather
  than trying source-sink-url/parse-sink-designator on every --out-dir
  value unconditionally) is deliberate: most --out-dir values are plain
  filesystem paths, and on Windows some of those legitimately contain a
  colon (\"C:\\...\") that parse-sink-designator would otherwise read as
  an unknown scheme name -- `stdout:` is unambiguous as a literal
  prefix, so this never mistakes a real path for one. Returns nil when
  out-dir isn't a stdout: designator at all (the directory-write path
  runs unchanged); otherwise the parse result (ok the Sink map, or
  parse-sink-designator's own rejection, e.g. a missing/invalid
  ?format=)."
  [out-dir]
  (when (and out-dir (str/starts-with? out-dir "stdout:"))
    (source-sink-url/parse-sink-designator out-dir)))

(defn- mutant->stdout-item
  "The write-stdout!-ready shape for one mutant, per the sink's own
  :framing -- :bundle-entries wants the mutant's own parsed data
  directly (only sensible when format is :fhir, whose mutant already
  IS a data map, ehrt.tools.interface's own item shape for
  that codec); every other framing wants bytes, the same UTF-8
  serialization write-mutant already produces to a file, taken here in
  memory instead of spit to disk."
  [format framing-kind mutant]
  (if (= :bundle-entries framing-kind)
    mutant
    (.getBytes ^String (case format :fhir (json/write-str mutant) :v2 mutant) "UTF-8")))

(defn- mutate-to-stdout!
  "SS-4 rulings 5-6: mutate's output routed through a :stdout Sink
  instead of a directory. Every matching file is mutated in turn and
  its mutant collected as one item; the whole batch is encoded and
  written in a single write-stdout! call, per the sink's own :framing.
  Fails fast on the first per-file mutation failure, same discipline as
  the directory-write path.

  Scope decision, named rather than silently dropped: NO lineage
  sidecar is written for a stdout destination -- lineage is a file
  under :out-dir/lineage/, and a stdout sink names no directory to put
  one in (the same reason the sink itself carries no manifest, Part III's
  own law statement). A caller who needs lineage for a mutate run
  writes to a directory (the unchanged default); stdout is for the
  byte-stream composability loopback (ruling 5), not a lineage-preserving
  destination.

  :stdout-out is injectable (an OutputStream, defaults to System/out via
  write-stdout! itself when omitted) so hermetic tests never write to
  the real process stdout -- the same -fn injection discipline this
  namespace already uses for subprocess/network seams, applied here to
  a stream instead of a function."
  [format operator locator-envelope files sink stdout-out]
  (loop [remaining files items []]
    (if (empty? remaining)
      (let [write-result (if stdout-out
                            (sink-write/write-stdout! sink items :out stdout-out)
                            (sink-write/write-stdout! sink items))]
        ;; :stdout-sink? true tells main! (below) that raw framed bytes
        ;; already went to the real process stdout -- the human-readable
        ;; EDN result summary main! always prints must be redirected
        ;; away from stdout too, or it corrupts the byte stream a
        ;; downstream `stdin:` consumer expects to decode (caught by
        ;; the real loopback test, test-integration/, before this fix).
        (if (result/ok? write-result)
          (result/ok (assoc (:payload write-result) :stdout-sink? true))
          write-result))
      (let [f (first remaining)
            base-data (read-base-data format f)
            mutate-result (mutate/mutate base-data operator locator-envelope)]
        (if-not (result/ok? mutate-result)
          mutate-result
          (recur (rest remaining)
                 (conj items (mutant->stdout-item
                              format
                              (or (:framing sink) source-sink/default-framing)
                              (:mutant (:payload mutate-result))))))))))

;; ---- ADR-0015: `ehrt corpus generate sim` -- the sim source's own
;; generation front door, alongside bare `ehrt corpus generate`/`ehrt
;; corpus generate synthea` (both still `generate/generate!`,
;; unchanged). No generation logic of its own: resolves CLI opts
;; against the :sim generator registry entry's own :default-params,
;; then drives that same entry's :out-dir-fn/:execute-fn -- the
;; registry (ehrt.tools.corpus.generators) stays the single source of
;; what :sim generation does. ----

(defn generate-sim-command
  "`ehrt corpus generate sim` (ADR-0015): CLI opts -> the :sim
  generator registry entry's own :default-params (D9's zero-flag
  contract -- a zero-flag `generate sim` is a complete, deterministic
  command, sharing generate/default-seed with :synthea's own zero-flag
  default), merged params -> that entry's own :out-dir-fn for the
  derived out-dir, then its own :execute-fn. Rejects up front with the
  shared :out-dir-exists guard (ehrt.tools.interface/out-dir-exists?/
  out-dir-exists-error) when the resolved out-dir already exists and is
  non-empty -- the same determinism guard corpus.generate/generate!
  enforces for :synthea: a second zero-flag run must never silently
  land in the same directory as the first."
  [{:keys [seed patients churn emit reference-date config out-dir]}]
  (let [params (cond-> {}
                 (some? seed) (assoc :seed seed)
                 (some? patients) (assoc :patients patients)
                 (some? churn) (assoc :churn churn)
                 emit (assoc :emit emit)
                 reference-date (assoc :reference-date reference-date)
                 config (assoc :config config))
        resolved-result (generators/generators-resolve-params :sim params)]
    (if-not (result/ok? resolved-result)
      resolved-result
      (let [entry (generators/generators-lookup :sim)
            merged (:payload resolved-result)
            resolved-out-dir (or out-dir ((:out-dir-fn entry) merged))]
        (if (generate/out-dir-exists? resolved-out-dir)
          (generate/out-dir-exists-error resolved-out-dir)
          ((:execute-fn entry) merged resolved-out-dir))))))

(defn mutate-command
  "`ehrt corpus mutate`: applies one operator, at one locator, to every
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
  omitted).

  SS-4 rulings 5-6: :out-dir also accepts a `stdout:...` Sink
  designator (never a default -- only when the caller passes one
  explicitly) -- see stdout-out-dir-result/mutate-to-stdout! for that
  path's own contract (batched write, no lineage sidecar). Every other
  :out-dir value -- the derived default, an explicit directory path, a
  dir:/file: URL already reduced to a bare path by dispatch's own
  resolve-path-designators -- runs the unchanged directory-write path
  below. :stdout-out is test-only injection for the stdout path (see
  mutate-to-stdout!'s own docstring); real callers never pass it.

  SS-4b (D-d resolved, ADR-0020): the directory-write path additionally
  emits operation-manifest.edn last, after every mutant and lineage
  sidecar in this batch has already landed (items-then-manifest
  ordering) -- via sink-write/write-dir!, :mode :overwrite (the
  directory itself was already created by this same call; this is not
  a second fail-if-exists gate on top of it), :items built from what
  this loop already computed (lineage's own :produced/:parent, no
  re-hashing). :git-describe-fn/:now-fn are injectable (mirror
  version-command's own :git-describe-fn) so hermetic tests never shell
  out to a real git process or read the wall clock; real callers never
  pass either."
  [{:keys [path operator-id operator-version locator-path out-dir stdout-out
           git-describe-fn now-fn]
    :or {operator-version "1" git-describe-fn real-git-describe
         now-fn #(str (LocalDate/now))}}]
  (let [operator (operators/lookup (keyword operator-id) operator-version)]
    (if-not operator
      (result/rejected :unknown-operator
                        {:id operator-id :version operator-version
                         :valid-options (sort (map :id (operators/entries)))
                         :hint "run: ehrt corpus operators"})
      (let [format (:format operator)
            stdout-result (stdout-out-dir-result out-dir)
            out-dir (or out-dir (default-mutate-out-dir path operator-id operator-version))
            locator-result (locator/make format (or locator-path (:default-locator operator)))]
        (if-not (result/ok? locator-result)
          locator-result
          (let [locator-envelope (:payload locator-result)
                files (files-with-extension-in path (format-file-extension format))]
            (cond
              (some? stdout-result)
              (if-not (result/ok? stdout-result)
                stdout-result
                (mutate-to-stdout! format operator locator-envelope files (:payload stdout-result) stdout-out))

              :else
              (do
                (.mkdirs (io/file out-dir))
                (.mkdirs (io/file out-dir "lineage"))
                (loop [remaining files processed []]
                  (if (empty? remaining)
                    (let [items (mapv (fn [{:keys [file sha256 input-hash]}]
                                         (cond-> {:name file :sha256 sha256}
                                           input-hash (assoc :input-hash input-hash)))
                                       processed)
                          sink (:payload (source-sink/dir-sink
                                          {:path out-dir :format (get operator-format->sink-format format)}))
                          manifest-result (sink-write/write-dir!
                                           sink {}
                                           :mode :overwrite
                                           :operation-manifest
                                           {:producer (mutate-producer git-describe-fn)
                                            :operation {:kind :mutate
                                                        :operator-id (:id operator)
                                                        :operator-version (:version operator)
                                                        :locator locator-envelope}
                                            :written-at (now-fn)
                                            :items items})]
                      (if-not (result/ok? manifest-result)
                        manifest-result
                        (result/ok {:count (count processed) :files processed})))
                    (let [f (first remaining)
                          base-data (read-base-data format f)
                          mutate-result (mutate/mutate base-data operator locator-envelope)]
                      (if-not (result/ok? mutate-result)
                        mutate-result
                        (let [{:keys [mutant lineage]} (:payload mutate-result)
                              basename (.getName f)]
                          (write-mutant format (io/file out-dir basename) mutant)
                          (spit (io/file out-dir "lineage" (str basename ".lineage.edn")) (pr-str lineage))
                          (recur (rest remaining)
                                 (conj processed {:file basename :lineage-id (:id lineage)
                                                   :sha256 (:produced lineage) :input-hash (:parent lineage)})))))))))))))))

(defn- generator-url?
  [designator-result]
  (and (result/ok? designator-result)
       (contains? #{:synthea :sim} (:kind (:payload designator-result)))))

(defn- stdin-url?
  [designator-result]
  (and (result/ok? designator-result)
       (= :stdin (:kind (:payload designator-result)))))

(defn intake-command
  "`ehrt corpus intake`: catalogs :path (positional PATH, or --path --
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
  clock, matching corpus.generate's :reference-date discipline).

  SS-2 Step 4 (ruling 6): :path may also be a generator URL
  (\"sim:?seed=42\", \"synthea:?seed=1&population=5\") instead of a
  directory -- the generate-and-catalog path in one command. Tried
  first via source-sink-url/parse-source-designator; a generator-kind
  result resolves the generator for real
  (ehrt.tools.interface/resolve!, executing its
  engine and yielding a dir Source) before intaking it via intake/
  intake-via-source!.

  SS-3 Step 6 (ruling 5): :path may also be a stdin designator
  (\"stdin:?format=v2-er7&framing=er7-multi\") -- read (real System/in,
  or :in-override for tests), spooled
  (ehrt.tools.interface/resolve!, :captured-at the
  CLI's own wall-clock-now, the impure boundary matching :received's
  own discipline), then intaken via intake/intake-via-source! exactly
  like a resolved generator Source.

  Any other outcome (a bare directory path, a dir:/file: URL --
  already reduced to a bare path by dispatch's own
  resolve-path-designators before this function ever runs, per ruling
  7 -- or simply an unparseable string) falls through to the unchanged
  intake!/:source-dir path exactly as before SS-2. `ehrt corpus
  generate` itself is untouched by any of this -- its own verb, flags,
  and defaults do not change here."
  [{:keys [path label out received in-override]}]
  (let [received (or received (str (LocalDate/now)))
        designator-result (source-sink-url/parse-source-designator path)]
    (cond
      (generator-url? designator-result)
      (let [source (:payload designator-result)
            resolved-result (generator-source/resolve! (:kind source) (dissoc source :kind))]
        (if-not (result/ok? resolved-result)
          resolved-result
          (intake/intake-via-source! {:source (:payload resolved-result)
                                      :source-label label :out out :received received})))

      (stdin-url? designator-result)
      (let [source (:payload designator-result)
            captured-at (str (java.time.Instant/now))
            resolved-result (spool-source/spool-resolve! (cond-> {:source source :captured-at captured-at}
                                                      in-override (assoc :in-override in-override)))]
        (if-not (result/ok? resolved-result)
          resolved-result
          (intake/intake-via-source! {:source (:payload resolved-result)
                                      :source-label label :out out :received received})))

      :else
      (intake/intake! {:source-dir (source-sink-url/path-designator->path path)
                        :source-label label :out out :received received}))))

(defn operators-command
  "`ehrt corpus operators`: lists the registered mutation operator
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
  "Builds an `ehrt gate <format>` command function from that format's
  gate-file/gate-dir functions (ehrt.tools.interface, and
  eventually judge.fhir -- same shape). :path may name a single file or
  a directory; either way the result is normalized into one
  judge.report (gate-label identifies which gate ran, in :run). Writes
  the report to :report when given (EDN, canonical -- ADR-0004), via
  `write-report!`: missing parent directories are created, and a
  residual IO failure is returned as :report-write-failed (exit 2)
  *instead of* the verdict -- a run whose recorded output didn't land
  is an operational failure, not a judgment.

  :baseline (P6, a path to a previously-written --report EDN file)
  switches to baseline-relative mode (ehrt.tools.interface/
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
  (gate-command gate-v2/v2-gate-file gate-v2/v2-gate-dir :v2))

(def default-fhir-gate-out-dir
  "out/scratch/gate-fhir")

(defn fhir-gate-command
  "`ehrt gate fhir`: unlike judge.v2 (fully self-contained, no options),
  judge.fhir needs the lockfile's artifacts plus a scratch directory
  for the validator's raw OperationOutcome output and invocation logs
  -- resolved here, then judge.fhir/gate-file and gate-dir are curried
  down to the 1-arity shape gate-command expects. :out-dir defaults to
  `out/scratch/gate-fhir` (ADR-0013: the single tool-owned, gitignored
  out/ root -- moved from a bare target/gate-fhir); :java-bin, when
  given, bypasses registry
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
            gate-fn (gate-command #(gate-fhir/fhir-gate-file % fhir-opts)
                                  #(gate-fhir/fhir-gate-dir % fhir-opts)
                                  :fhir)]
        (gate-fn {:path path :report report :baseline baseline
                  :treat-no-verdict-as treat-no-verdict-as})))))

;; ---- D11 (docs/source-sink-design.md Part IX.4, ADR-0019): bare
;; `ehrt gate PATH` sniffs via corpus.intake/sniff-format instead of a
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
                        :hint "ambiguous format -- run: ehrt gate v2 PATH, or ehrt gate fhir PATH"}
                       payload)))

(defn sniff-gate-command
  "The bare `ehrt gate PATH` dispatch (D11): sniffs :path via
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
                      :hint "no such file or directory -- run: ehrt help gate"})

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

;; ---- ADR-0013: `ehrt show` -- the pretty-always display verb. Joins
;; D11's own sniff dispatch (gate-candidate-files-in/sniff-path-format
;; above) rather than inventing a second one; rendering itself lives in
;; ehrt.tools.display, required here as `display`. ----

(defn- show-ambiguous-error
  [path payload]
  (result/error :show-format-ambiguous
                (merge {:path path
                        :hint "ambiguous format -- ehrt show only renders HL7 v2 (ER7) or FHIR JSON"}
                       payload)))

(defn- render-sniffed-content
  [gate-label content]
  (case gate-label
    :v2 (display/render-er7-stream content)
    :fhir (display/render-fhir-json content)))

(defn- show-file
  [f]
  (let [gate-label (sniff-path-format f)]
    (if (nil? gate-label)
      (show-ambiguous-error (.getPath f) {:unrecognized-files [(.getPath f)]})
      (render-sniffed-content gate-label (slurp f)))))

(defn- as-display-text
  [r]
  (if-not (result/ok? r)
    r
    (assoc (result/ok {}) :category :display-text :payload {:text (:payload r)})))

(defn show-command
  "`ehrt show PATH`: pretty-always display verb (ADR-0013) -- never
  consults :tty?-fn or --pretty/--edn/--json; its entire job is
  rendering for eyes, so `ehrt show foo.hl7 | less` must work with no
  flag at all (main! recognizes this result's :category :display-text
  the same way it already special-cases :cli-help). Joins D11's own
  sniff dispatch: a single file sniffs and renders directly; a
  directory renders every ER7/FHIR-JSON candidate file in turn (the
  same candidate set gate-dir would look at), joined by a blank line,
  and a mixed or unclassifiable set is the same operational-error shape
  D11 already uses for gate -- naming what confused it, never a silent
  per-file split. Read-only by construction: this never writes
  anything -- ehrt.tools.display's own functions only ever read the
  string content this function already slurped."
  [{:keys [path]}]
  (let [f (io/file path)]
    (cond
      (not (.exists f))
      (result/error :gate-path-not-found
                     {:path path :hint "no such file or directory -- run: ehrt help show"})

      (.isFile f)
      (as-display-text (show-file f))

      :else
      (let [files (gate-candidate-files-in path)]
        (if (empty? files)
          (show-ambiguous-error path {:reason :no-candidate-files})
          (let [sniffed (map (fn [file] [(.getName file) (sniff-path-format file)]) files)
                unrecognized (mapv first (filter (fn [[_ fmt]] (nil? fmt)) sniffed))]
            (if (seq unrecognized)
              (show-ambiguous-error path {:unrecognized-files unrecognized})
              (let [formats (into #{} (map second) sniffed)]
                (if (> (count formats) 1)
                  (show-ambiguous-error
                   path
                   {:counts (into {}
                                  (map (fn [[gl fs]] [(get {:fhir :fhir-json :v2 :v2-er7} gl) (count fs)]))
                                  (group-by second sniffed))})
                  (let [rendered (map show-file files)
                        failed (first (remove result/ok? rendered))]
                    (if failed
                      failed
                      (as-display-text
                       (result/ok (str/join "\n\n" (map :payload rendered)))))))))))))))

;; ---- ADR-0014: `ehrt play`'s executor -- folds player/plan's own
;; emission plan through an injected :sleep-fn and one sink function.
;; This is the only place in this file that ever sleeps for the
;; player's own sake; the plan itself (ehrt.tools.player) never does. ----

(defn real-sleep!
  "Thread/sleep ms, skipped entirely for a non-positive wait -- the
  production :sleep-fn default; tests inject a recording fake instead."
  [ms]
  (when (pos? ms) (Thread/sleep (long ms))))

(defn- run-plan!
  "Folds a player/plan result through sleep-fn and sink-fn, in event
  order: sleep the computed wait, invoke cue-fn when this index was
  actually idle-capped (the skip cue -- ADR-0014's own cue rule: never
  routed into sink-fn), then sink-fn the event itself. Returns
  {:emitted n :clamped-count :unparseable-count :skip-count} --
  the plan's own counts, passed through for the end-of-run summary."
  [{:keys [plan clamped-count unparseable-count skip-count capped-indices]}
   sleep-fn cue-fn sink-fn]
  (doseq [[idx [wait-ms event]] (map-indexed vector plan)]
    (sleep-fn wait-ms)
    (when (contains? capped-indices idx) (cue-fn idx))
    (sink-fn event))
  {:emitted (count plan)
   :clamped-count clamped-count
   :unparseable-count unparseable-count
   :skip-count skip-count})

(defn- ticker-full-sink
  "Full-mode ticker (the default): render-er7-message, one block per
  event -- the exact rendering `ehrt show`'s own directory dispatch
  already uses (ADR-0013), pretty-always, no TTY consultation."
  [println-fn]
  (fn [event] (println-fn (display/render-er7-message event)) (println-fn "")))

(defn- ticker-line-sink
  "Compact --ticker line mode: MSH-7 timestamp, MSH-9 type^trigger,
  first PID-3 when the message carries one -- player's own lenient
  field reads, never a second HL7 parser. A field player can't read is
  rendered as \"?\", never a thrown exception."
  [println-fn]
  (fn [event]
    (println-fn (str (or (player/message-timestamp-ms event) "?") "  "
                      (or (player/message-type-trigger event) "?")
                      (when-let [pid (player/message-patient-id event)] (str "  " pid))))))

(defn- stderr-cue-fn
  "The skip cue, routed to stderr -- used whenever the ticker itself
  isn't already the destination (a data sink owns stdout instead), so
  the cue never lands in sink-fn's own bytes (ADR-0014's cue rule)."
  [_idx]
  (binding [*out* *err*] (println "-- idle-skip: stream-time jumped --")))

(defn- ticker-cue-fn
  "The skip cue, printed through the ticker's own stream -- display
  text, same as the ticker's own rendering, never a data sink's bytes."
  [println-fn]
  (fn [_idx] (println-fn "-- idle-skip: stream-time jumped --")))

(defn- file-sink-fn
  "A :file Sink's own :path -> a fn of one event, appending that
  event's own player/frame-event bytes (ADR-0014's byte-identity
  requirement: N single-event appends, in order, equal one batch
  encode over the same events). Opens the output stream once, up
  front (truncating any prior content -- this run owns the file for
  its own duration), and returns it closed via the 0-arity call the
  caller makes when the run finishes."
  [path]
  (io/make-parents (io/file path))
  (let [out (io/output-stream path)]
    {:sink-fn (fn [event]
                (let [r (player/frame-event event)]
                  (when (result/ok? r)
                    (.write ^java.io.OutputStream out ^bytes (:payload r)))))
     :close-fn (fn [] (.close ^java.io.OutputStream out))}))

(defn- validate-positive
  "opts's own numeric coercion (babashka.cli's :coerce :double on
  --rate/--idle-cap, same convention as --seed's :coerce :long) already
  guarantees a real double or nil here -- this only enforces the
  CLI-boundary contract that a given value must be positive (ADR-0004:
  a bad CLI argument is an operational rejection, not a thrown
  exception, matching parse-treat-no-verdict-as's own discipline)."
  [category v]
  (cond
    (nil? v) (result/ok nil)
    (pos? v) (result/ok v)
    :else (result/rejected category {:value v :hint "must be a positive number"})))

(defn- ensure-default-play-sink-format
  "D3's no-inference-on-write law means a Sink always declares its own
  :format explicitly -- a bare `--sink file:out/tail.hl7` (no
  ?format=...) would otherwise fail :invalid-sink for a missing key.
  Since ehrt play only ever emits v2-er7/er7-multi content this
  session, a designator with no query string at all gets that default
  filled in; a caller who already wrote a query string (any query,
  including a different format) is left completely alone."
  [designator]
  (if (str/includes? designator "?")
    designator
    (str designator "?format=v2-er7&framing=er7-multi")))

(defn- resolve-play-sink
  "--sink DESIGNATOR (ADR-0014: reuses the existing source-sink
  designator vocabulary, never a parallel flag scheme) -> kernel/ok
  {:sink-fn :close-fn :cue-fn} for a supported kind, or an operational
  error naming what's unsupported. Scoped to :file this session
  (:dir/:blaze -- including the future :mllp transport -- are named,
  disclosed deferrals, ADR-0014's own bail-out procedure); nil sink
  (no --sink given) means \"use the ticker\" and isn't resolved here."
  [designator println-fn]
  (let [parsed (source-sink-url/parse-sink-designator (ensure-default-play-sink-format designator))]
    (if-not (result/ok? parsed)
      parsed
      (let [{:keys [kind path]} (:payload parsed)]
        (case kind
          :file (let [{:keys [sink-fn close-fn]} (file-sink-fn path)]
                  (result/ok {:sink-fn sink-fn :close-fn close-fn :cue-fn stderr-cue-fn}))
          (result/error :play-sink-kind-unsupported
                        {:kind kind :path path
                         :hint "ehrt play only supports a file: sink this session -- dir:/blaze: (and a future mllp: transport) are named, disclosed deferrals (ADR-0014)"}))))))

(defn- play-events-from-file
  [f]
  (if-not (= :v2 (sniff-path-format f))
    (result/error :play-input-unsupported
                  {:path (.getPath f) :hint "ehrt play only supports HL7 v2 (ER7) input this session -- FHIR is a named, disclosed deferral (ADR-0014)"})
    (player/split-er7-multi (slurp f))))

(defn- play-events-from-dir
  "ADR-0015: a directory of files sharing the sniffed v2 format,
  concatenated in lexical filename order -- the exact candidate set
  `gate-dir`/`show`'s own directory dispatch already use
  (gate-candidate-files-in, already name-sorted), and exactly the
  order the sim generator's own msg-%03d emission produces by
  construction. Each file is decoded on its own via
  player/split-er7-multi, then the per-file message sequences are
  concatenated in that same directory-listing order -- this IS the
  order contract (help text states it too, not left to be discovered
  by reading source): the directory listing's own order is the
  corpus's own statement, never sorted by content. A FHIR directory, a
  mixed or unclassifiable one, or an empty one, are all
  :play-input-unsupported -- the same shape D11's sniff dispatch
  already uses for an ambiguous gate, never a silent per-file split."
  [path]
  (let [files (gate-candidate-files-in path)]
    (if (empty? files)
      (result/error :play-input-unsupported
                    {:path path :reason :no-candidate-files
                     :hint "ehrt play found no HL7 v2 (ER7) or FHIR JSON candidate files in this directory"})
      (let [sniffed (map (fn [file] [(.getName file) (sniff-path-format file)]) files)
            unrecognized (mapv first (filter (fn [[_ fmt]] (nil? fmt)) sniffed))
            formats (into #{} (map second) sniffed)]
        (cond
          (seq unrecognized)
          (result/error :play-input-unsupported
                        {:path path :unrecognized-files unrecognized
                         :hint "ehrt play only supports HL7 v2 (ER7) directories -- ambiguous format"})

          (> (count formats) 1)
          (result/error :play-input-unsupported
                        {:path path
                         :hint "ehrt play only supports a directory of one format -- this one mixes HL7 v2 and FHIR JSON"})

          (= :fhir (first formats))
          (result/error :play-input-unsupported
                        {:path path :hint "ehrt play only supports HL7 v2 (ER7) input this session -- a FHIR directory is a named, disclosed deferral (ADR-0014)"})

          :else
          (let [per-file (map (fn [file] (player/split-er7-multi (slurp file))) files)
                failed (first (remove result/ok? per-file))]
            (if failed
              failed
              (result/ok (vec (mapcat :payload per-file))))))))))

(defn play-command
  "`ehrt play PATH [--rate R] [--idle-cap SECONDS] [--ticker full|line]
  [--sink DESIGNATOR]` (ADR-0014, directories per ADR-0015): paces
  PATH's own HL7 v2 (ER7) messages against their MSH-7 timestamps and
  either renders them through a ticker (the default -- full blocks via
  `render-er7-message`, or one compact `--ticker line` per event) or
  writes them, byte-identically to an unpaced batch write, through a
  `--sink` designator. `ehrt play PATH` at an arbitrarily large --rate,
  ticker sink, is exactly `ehrt show PATH` (ADR-0013/ADR-0014's own
  identity) -- ordinary division makes this true with no special-cased
  rate value.

  PATH is a single HL7 v2 (ER7) file, or a directory of files sharing
  the sniffed v2 format (ADR-0015 -- concatenated in lexical filename
  order, see `play-events-from-dir`'s own docstring for the order
  contract). A FHIR JSON path, or a FHIR/mixed/unclassifiable
  directory, is :play-input-unsupported (a named, disclosed deferral --
  a sim event-log adapter and a bed-board sink are future work,
  ADR-0014).

  :sleep-fn is injectable (defaults to real-sleep!, Thread/sleep) so
  hermetic tests never actually wait; :println-fn defaults to println.
  Returns the standard Result envelope (events emitted, stream-time
  span, wallclock elapsed, the resolved rate/idle-cap, and every count
  player/plan itself computed) -- rendered through the ordinary
  TTY/--pretty/--edn/--json machinery, exactly like every other
  command; ADR-0014 does not add a second output convention."
  [{:keys [path rate idle-cap ticker sink sleep-fn println-fn now-ms-fn]
    :or {sleep-fn real-sleep! println-fn println now-ms-fn #(System/currentTimeMillis)}}]
  (let [rate-result (validate-positive :invalid-rate rate)]
    (if-not (result/ok? rate-result)
      rate-result
      (let [idle-cap-result (validate-positive :invalid-idle-cap idle-cap)]
        (if-not (result/ok? idle-cap-result)
          idle-cap-result
          (let [resolved-rate (or (:payload rate-result) player/default-rate)
                resolved-idle-cap-ms (if-let [s (:payload idle-cap-result)] (long (* 1000 s)) player/default-idle-cap-ms)
                f (io/file path)]
            (cond
              (not (.exists f))
              (result/error :gate-path-not-found
                             {:path path :hint "no such file or directory -- run: ehrt help play"})

              :else
              (let [events-result (if (.isDirectory f) (play-events-from-dir path) (play-events-from-file f))]
                (if-not (result/ok? events-result)
                  events-result
                  (let [events (:payload events-result)
                        plan-result (player/plan events {:rate resolved-rate :idle-cap-ms resolved-idle-cap-ms})
                        started-ms (now-ms-fn)
                        sink-result
                        (if sink
                          (resolve-play-sink sink println-fn)
                          (result/ok {:sink-fn (case ticker
                                                  "line" (ticker-line-sink println-fn)
                                                  (ticker-full-sink println-fn))
                                      :close-fn (fn [])
                                      :cue-fn (ticker-cue-fn println-fn)}))]
                    (if-not (result/ok? sink-result)
                      sink-result
                      (let [{:keys [sink-fn close-fn cue-fn]} (:payload sink-result)
                            run-result (run-plan! plan-result sleep-fn cue-fn sink-fn)
                            _ (close-fn)
                            ended-ms (now-ms-fn)
                            first-ts (some player/message-timestamp-ms events)
                            last-ts (some player/message-timestamp-ms (reverse events))]
                        (result/ok (merge run-result
                                           {:rate resolved-rate
                                            :idle-cap-ms resolved-idle-cap-ms
                                            :wallclock-ms (- ended-ms started-ms)
                                            :stream-span-ms (when (and first-ts last-ts) (- last-ts first-ts))
                                            :sink (or sink "ticker")}))))))))))))))

(defn- parse-canonicalizer-steps
  "\"id@v,id2@v2\" -> [[:id \"v\"] [:id2 \"v2\"]] -- the ordered
  [id version] pairs ehrt.tools.interface/apply-canonicalizers
  expects. Blank/nil -> []."
  [s]
  (if (str/blank? s)
    []
    (mapv (fn [pair]
            (let [[id version] (str/split pair #"@" 2)]
              [(keyword id) version]))
          (str/split s #","))))

(defn check-command
  "`ehrt check DIR --expected DIR --assertions FILE
  [--canonicalizers id@v,...] [--pair-by path|hash] [--report ...]`.
  :assertions names an EDN file holding a vector of assertion maps
  (ehrt.tools.interface/Assertion) -- read here, the CLI's own
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
  bare `ehrt`, or `ehrt help bogus`)."
  [group]
  (or (and group (help/render-group help/cli-spec group))
      (help/render-top-level help/cli-spec)))

(defn- help-response
  "An explicit help request (`ehrt help`, `ehrt help <group>`, `--help`
  anywhere): result/ok (exit 0) carrying the plain-text usage under
  :payload's :text, marked :category :cli-help so main! prints it
  verbatim instead of through `render`."
  [group]
  (assoc (result/ok {:text (help-text-for group)}) :category :cli-help))

(defn- bare-invocation-response
  "Bare `ehrt` (no group at all): prints the same top-level usage text
  as `ehrt help`, but stays result/error (exit 2) -- an incomplete
  invocation is operationally an error, not a help request, even
  though the text shown is identical."
  []
  (result/error :cli-help {:text (help-text-for nil)}))

(defn- unknown-command-error
  "An unrecognized group or verb: :unknown-command, extended (DOC-1's
  bounded error-message pass) with :valid-options (drawn from the help
  spec -- one source of truth with `ehrt help`'s own text, so the two
  can't drift apart) and a fixed :hint pointing at the fuller help
  surface."
  [args valid-options]
  (result/error :unknown-command {:args args :valid-options valid-options :hint "run: ehrt help"}))

(defn sim-run-command
  "`ehrt sim run`: mounts ehrt.sim.interface/run-command in-process
  (ADR-0005, 2026-07-28 -- ADR-0012's own long-deferred \"ehrt sim
  mount\", fulfilled once sim and tools shared one workspace/classpath).
  No translation layer: this CLI's own flag names already match sim's
  own opts 1:1 (:seed, :patients, :reference-date, :emit, :churn,
  :config, :warm-up-seconds -- see ehrt.sim.interface/run-command's own
  docstring for the full set); the -fn injection point below
  (:sim-run-fn) is what keeps this repo's own CLI tests hermetic, per
  ADR-0012 property 5's own commitment."
  [opts]
  (sim/sim-run! opts))

;; Cross-repo interface commitment (ADR-0012), now fulfilled by
;; sim-run-command above (ADR-0005): the five properties ADR-0012 named
;; are exactly what made this mount ~roughly four lines of change --
;; preserve them, when refactoring this boundary: parsed-[group
;; action]-in / Result-map-out dispatch; a single merged babashka.cli
;; spec parsed once, host-side; structural Result typing; the help-group
;; data shape; the -fn injection point (:sim-run-fn, alongside every
;; other -fn key dispatch already carries). Manifest schema changes
;; require a version bump, and the binding contract test lives in
;; projects/conformance/test/. Read ADR-0012 and ADR-0005 (and, for
;; provenance, notes/ehr-testing-sim-mounting-note.md) before changing
;; any of these.

(defn- resolve-path-designators
  "CLI acceptance is additive (ruling 7, docs/source-sink-design.md
  Part IX via SS-1 Step 6): wherever a positional PATH names an input,
  or --out-dir/--out names an output, a dir:/file: URL designator is
  now also accepted alongside the documented bare-path spelling --
  parsed to the same path a bare spelling would have given
  (source-sink-url/path-designator->path), never the other way
  around. Applied once here, in dispatch, so every downstream command
  function keeps working with plain path strings exactly as before --
  this is CLI-shell-boundary sugar, not a new capability those
  functions need to know about. A key absent from opts is left absent
  (most verbs use only one or two of these three)."
  [opts]
  (reduce (fn [opts k]
            (if (contains? opts k)
              (update opts k source-sink-url/path-designator->path)
              opts))
          opts
          [:path :out-dir :out]))

(defn dispatch
  "Routes [group action] positional args to the corresponding capability
  function with opts. The -fn keys are injectable (tests use this
  to avoid real subprocesses/network); default to the real commands.

  `ehrt help`, `ehrt help <group>`, and `--help` (given anywhere --
  `opts`'s :help true) short-circuit before any capability function
  runs, returning a :category :cli-help result instead of routing to a
  command -- see `help-response`/`bare-invocation-response` and the ns
  docstring's EDN-out exception."
  ([args opts] (dispatch args opts {}))
  ([args opts {:keys [fetch-fn fetch-all-fn resolve-fn generate-fn generate-sim-fn mutate-fn intake-fn operators-fn
                       gate-v2-fn gate-fhir-fn check-fn version-fn doctor-fn sim-run-fn show-fn play-fn]
               :or {fetch-fn fetch-command
                    fetch-all-fn fetch-all-command
                    resolve-fn resolve-command
                    generate-fn generate/generate!
                    generate-sim-fn generate-sim-command
                    mutate-fn mutate-command
                    intake-fn intake-command
                    operators-fn operators-command
                    gate-v2-fn gate-v2-command
                    gate-fhir-fn fhir-gate-command
                    check-fn check-command
                    version-fn version-command
                    doctor-fn doctor-command
                    sim-run-fn sim-run-command
                    show-fn show-command
                    play-fn play-command}}]
   (let [[group action path] args]
     (cond
       (:help opts) (help-response group)
       (= group "help") (help-response action)
       (nil? group) (bare-invocation-response)

       :else
       (let [;; `ehrt gate fhir PATH|DIR` / `ehrt gate v2 PATH|DIR` /
             ;; `ehrt corpus mutate PATH` / `ehrt corpus intake PATH`: PATH
             ;; is a positional third arg, with --path as its explicit
             ;; twin (D10) -- never overridden by a positional path when
             ;; --path was given explicitly. `ehrt check DIR` has no
             ;; sub-verb, so its positional path is the *second* arg
             ;; (bound above as `action`), not the third.
             opts (cond
                    (and (= group "gate") path (not (:path opts))) (assoc opts :path path)
                    (and (= group "corpus") (#{"mutate" "intake"} action) path (not (:path opts))) (assoc opts :path path)
                    (and (= group "check") action (not (:path opts))) (assoc opts :path action)
                    (and (= group "show") action (not (:path opts))) (assoc opts :path action)
                    (and (= group "play") action (not (:path opts))) (assoc opts :path action)
                    :else opts)
             opts (resolve-path-designators opts)]
         (case group
           "artifact" (case action
                        "fetch" (if (:all opts) (fetch-all-fn opts) (fetch-fn opts))
                        "resolve" (resolve-fn opts)
                        (unknown-command-error args (help/verb-names (help/find-group help/cli-spec "artifact"))))
           "corpus" (case action
                      ;; ADR-0015: `corpus generate` grows source
                      ;; subcommands (sim/synthea) via the same third
                      ;; positional slot gate's own v2/fhir discriminator
                      ;; already occupies one level up -- `path` here is
                      ;; the subcommand name, not a filesystem path; bare
                      ;; `corpus generate` (path nil) stays synthea,
                      ;; byte-for-byte unchanged.
                      "generate" (case (or path "synthea")
                                   "synthea" (generate-fn opts)
                                   "sim" (generate-sim-fn opts)
                                   (unknown-command-error args ["synthea" "sim"]))
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
                    ;; --path always wins when both are given. `action`
                    ;; never went through resolve-path-designators
                    ;; above (it wasn't known to be a path yet at that
                    ;; point) -- resolved here instead.
                    (or action (:path opts))
                    (sniff-gate-command (assoc opts :path (source-sink-url/path-designator->path (or (:path opts) action)))
                                         gate-v2-fn gate-fhir-fn)
                    :else
                    (unknown-command-error args (help/verb-names (help/find-group help/cli-spec "gate"))))
           "check" (check-fn opts)
           "version" (version-fn opts)
           "doctor" (doctor-fn opts)
           "sim" (case action
                   "run" (sim-run-fn opts)
                   (unknown-command-error args (help/verb-names (help/find-group help/cli-spec "sim"))))
           "show" (show-fn opts)
           "play" (play-fn opts)
           (unknown-command-error args (help/group-names help/cli-spec))))))))

(defn render
  [r json?]
  (if json?
    (json/write-str r)
    (pr-str r)))

;; ---- ADR-0013: TTY-default rendering. A live terminal gets a human
;; summary; a pipe/redirect gets the EDN envelope exactly as before --
;; the determinism doctrine governs artifacts (files, --report,
;; redirected bytes), not what a human sees at a shell. ----

(defn real-tty?
  "(some? (System/console)) -- the classic JVM idiom, chosen because it
  returns nil the moment either stream is redirected, which is exactly
  the conservative bias the TTY rule calls for (any doubt resolves to
  the machine format)."
  []
  (some? (System/console)))

(defn- pretty-kv-line
  [[k v]]
  (str (name k) "=" v))

(defn- pretty-verdict-line
  [{:keys [path verdict finding-count]}]
  (str (name verdict) "  " path
       (when (pos? finding-count)
         (str "  (" finding-count (if (= 1 finding-count) " finding)" " findings)")))))

(defn- report-payload
  "The Report shape (ehrt.judge.report/Report) both gate and check
  produce -- :files plus :totals. Baseline mode's {:absolute :relative}
  payload, and anything else that doesn't match, is not this shape."
  [payload]
  (and (map? payload) (contains? payload :files) (contains? payload :totals) payload))

(defn- pretty-report-summary
  "gate/check's own tailored summary (ADR-0013): one verdict line per
  file, then aggregate totals, then by-code counts, then any path this
  run actually wrote (--report)."
  [rpt report-path]
  (str (str/join "\n" (map pretty-verdict-line (:files rpt)))
       "\n\ntotals: " (str/join ", " (map pretty-kv-line (:totals rpt)))
       (when (seq (:by-code rpt))
         (str "\nby-code: " (str/join ", " (map pretty-kv-line (:by-code rpt)))))
       (when report-path
         (str "\nreport written: " report-path))))

(defn- pretty-generic-summary
  "Every other envelope command's brief summary (ADR-0013): status,
  category, whatever key counts/paths the payload happens to carry,
  plus a hint pointing at the full envelope. Never a prettified EDN
  envelope -- the envelope is the machine form, full stop."
  [r]
  (let [{:keys [status category payload]} r
        interesting (select-keys payload [:count :out-dir :path :cached :git :identity :item-count])]
    (str (name status)
         (when category (str " (" (name category) ")"))
         (when (seq interesting)
           (str "\n" (str/join "\n" (map pretty-kv-line interesting))))
         "\n(--edn or --json for the full result)")))

(defn render-pretty
  "Human-facing rendering for a Result envelope (ADR-0013). Dispatches
  on the payload's own shape, not on which command ran: a Report-shaped
  payload (gate, check) gets the tailored per-file summary; everything
  else -- including baseline mode's {:absolute :relative} payload --
  gets the generic summary (this ruling's own named, permitted skip:
  tailoring beyond gate/check is not required)."
  [r report-path]
  (if-let [rpt (report-payload (:payload r))]
    (pretty-report-summary rpt report-path)
    (pretty-generic-summary r)))

(defn main!
  "The real body of -main, with every side-effecting boundary
  injectable for testing: :dispatch-fn (default `dispatch`),
  :println-fn (default `println`), :exit-fn (default `System/exit`),
  :tty?-fn (default `real-tty?`, ADR-0013). Returns the exit code it
  computed -- mainly useful for tests; -main itself ignores the return
  value since :exit-fn already terminated the process in real use.
  This split is what lets -main's exit-code mapping and command
  routing be unit-tested without a real System/exit (which would kill
  the test JVM).

  A :category :cli-help result (help-response/bare-invocation-response
  in `dispatch`) or :category :display-text result (show-command,
  ADR-0013) prints its :text payload verbatim via println-fn instead of
  going through `render`/`render-pretty` -- the ns docstring's
  deliberate EDN-out exceptions; --json/--pretty/--edn are all ignored
  for these regardless of what was passed, since there is no EDN form
  to project.

  ADR-0013: stdout rendering resolves in this order -- --pretty forces
  `render-pretty` even into a pipe; else --edn forces the raw EDN
  envelope even at a terminal; else --json behaves exactly as it always
  has (a JSON projection, regardless of TTY); else, with none of the
  three given, :tty?-fn decides -- a real terminal gets `render-pretty`,
  a pipe/redirect gets the EDN envelope (today's unconditional default,
  unchanged for every existing piped/redirected caller). `--report`
  files are untouched by any of this.

  SS-4: a result whose :payload carries :stdout-sink? true (mutate-to-
  stdout!'s own marker) means raw framed bytes already went to the real
  process stdout -- printing the summary there too would corrupt the
  byte stream a downstream `stdin:` consumer expects to decode cleanly
  (caught for real by the loopback integration test before this
  redirect existed). That summary is printed to *err* instead in this
  one case -- stdout stays exactly, only, the framed bytes."
  ([raw-args] (main! raw-args {}))
  ([raw-args {:keys [dispatch-fn println-fn exit-fn tty?-fn]
              :or {dispatch-fn dispatch println-fn println exit-fn #(System/exit %)
                   tty?-fn real-tty?}}]
   (let [{:keys [args opts]} (parse raw-args)
         r (dispatch-fn args opts)
         code (result->exit-code r)
         text (cond
                (#{:cli-help :display-text} (:category r)) (:text (:payload r))
                (:pretty opts) (render-pretty r (:report opts))
                (:edn opts) (render r false)
                (:json opts) (render r true)
                (tty?-fn) (render-pretty r (:report opts))
                :else (render r false))]
     (if (:stdout-sink? (:payload r))
       (binding [*out* *err*] (println-fn text))
       (println-fn text))
     (exit-fn code)
     code)))

(defn -main
  [& raw-args]
  (main! raw-args))
