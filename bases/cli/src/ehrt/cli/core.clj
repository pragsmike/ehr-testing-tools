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
            ;; tools split stage 3 (2026-07-31, ADR-0018): the tools
            ;; façade retired -- every alias below now names the owning
            ;; interface directly. kernel's result/artifact/locator
            ;; vocabulary, judge's report vocabulary, and the three
            ;; gate engines were relay re-exports in ehrt.tools.interface;
            ;; the corpus domain itself is ehrt.corpus.interface.
            [ehrt.kernel.interface :as result]
            [ehrt.kernel.interface :as artifact]
            [ehrt.kernel.interface :as locator]
            ;; kernel's io vocabulary under its own role name
            ;; (ADR-0157, review-4 D4-1): result/artifact/locator
            ;; all read wrong on mkdirs!.
            [ehrt.kernel.interface :as kernel-io]
            [ehrt.cli.help :as help]
            [ehrt.corpus.interface :as generate]
            [ehrt.corpus.interface :as generators]
            [ehrt.corpus.interface :as mutate]
            [ehrt.corpus.interface :as intake]
            [ehrt.corpus.interface :as operators]
            [ehrt.corpus.interface :as generator-source]
            [ehrt.corpus.interface :as check]
            [ehrt.corpus.interface :as sim]
            [ehrt.corpus.interface :as display]
            [ehrt.corpus.interface :as player]
            [ehrt.corpus.interface :as board]
            ;; corpus-io stage 2 (2026-07-31, ADR-0017): the transport
            ;; seam, required directly since that stage -- same alias
            ;; names as before it moved.
            [ehrt.corpus-io.interface :as spool-source]
            [ehrt.corpus-io.interface :as source-sink]
            [ehrt.corpus-io.interface :as source-sink-url]
            [ehrt.corpus-io.interface :as sink-write]
            ;; ADR-0111: the corpus batcher's own partition fn + the
            ;; :batch framing codec, both corpus-io.
            [ehrt.corpus-io.interface :as corpus-io]
            [ehrt.judge-v2-hapi.interface :as gate-v2]
            [ehrt.judge-fhir-official.interface :as gate-fhir]
            [ehrt.judge-v2-nist.interface :as gate-v2-nist]
            [ehrt.judge.interface :as report]
            ;; ARC 4 SWEEP 2 (ADR-0175 design (h)): `gate v2`'s sampling
            ;; policy classifies a message family as skeleton or add-on
            ;; against the EMITTER's own registry, so the base -- the
            ;; composition root, and the one place allowed to see both
            ;; sides -- is where the two meet. `components/judge` stays
            ;; free of any emitter dependency; it takes the set.
            [ehrt.sim-emit-hl7.interface :as emit-hl7])
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
   ;; --board (player board, ADR-0067): stream-minutes per snapshot,
   ;; the same whole-count convention --seed already uses.
   :board {:coerce :long}
   ;; Digit-only strings that are identifiers, not numbers -- must not be
   ;; auto-coerced to a long (which would break ProcessBuilder's String[]
   ;; args downstream in corpus.generate/invocation).
   :reference-date {:coerce :string}
   :version {:coerce :string}
   :no-verdict-cache {:coerce :boolean}
   ;; ARC 4 SWEEP 2 (ADR-0175 design (h), ruling D1): `gate v2`'s own
   ;; per-MSH-9-stratum cap on ADD-ON message families. Skeleton-kind
   ;; families are always gated in full whatever this says.
   :sample-add-ons {:coerce :long}
   :all {:coerce :boolean}
   ;; `ehrt sim run` (ADR-0005, ADR-0012 fulfilled): sim's own opt names,
   ;; unchanged -- ehrt.sim.interface/run-command's own :patients/
   ;; :warm-up-seconds/:churn.
   :patients {:coerce :long}
   :warm-up-seconds {:coerce :long}
   :churn {:coerce :boolean}
   ;; P3-6 parity mount (2026-08-01): `ehrt sim run` already forwarded
   ;; every parsed opt to run-command unchanged, but :arrival-gap/:at
   ;; had no :coerce entry, so either flag arrived as an uncoerced
   ;; string -- silently wrong once run.clj does arithmetic on it
   ;; (:arrival-gap feeds engine-params directly; :at is compared
   ;; against numeric run-elapsed seconds in emit-state/bundle-run).
   ;; :utc-offset already worked (default string coercion), added here
   ;; only for the same digit-only-string discipline :reference-date
   ;; documents above.
   :arrival-gap {:coerce :long}
   :at {:coerce :long}
   ;; ADR-0111: ehrt corpus batch's own --interval, in minutes -- the
   ;; same whole-count convention --seed/--board already use.
   :interval {:coerce :long}
   :utc-offset {:coerce :string}
   ;; --width (AR-EP-3, ux epilogue, `notes/adr/0065-ux-epilogue.md`):
   ;; kept a string, not :coerce :long -- babashka.cli throws on a
   ;; non-numeric :long value, which would crash before this flag's own
   ;; reject-by-name validation (help/parse-width-flag) ever ran.
   :width {:coerce :string}})

(defn parse
  "Parses raw CLI args into {:args [positional...] :opts {...}}."
  [raw-args]
  (cli/parse-args raw-args {:spec cli-spec}))

(defn- flag-expected-type
  "The human-readable type name for a coerced flag's own cli-spec entry
  -- \"a long\"/\"a double\"/\"true or false\", or \"a string\" for
  anything else (`parse-error-result`'s own catch-all, since babashka.cli
  only ever coerces to a handful of scalar shapes here)."
  [option]
  (case (:coerce (get cli-spec option))
    :long "a long"
    :double "a double"
    :boolean "true or false"
    "a string"))

(defn- parse-error-result
  "F2 (R3-B2-2, ADR-0117): babashka.cli's own parse-time ExceptionInfo
  (a coercion failure, e.g. --seed abc) carries :option/:value in its
  own ex-data -- confirmed live by direct probe: {:type :org.babashka/cli
  :cause :coerce :option :seed :value \"abc\" ...}. Translated here into
  the same :invalid-flag-value categorized shape every other CLI
  rejection already uses, naming the offending flag, its raw value, and
  the expected type (read from cli-spec's own :coerce entry) -- nil when
  ex-data carries no :option, the caller's cue to let the exception
  propagate unchanged rather than misrepresent a shape this doesn't
  recognize."
  [ex]
  (let [{:keys [option value]} (ex-data ex)]
    (when option
      (result/error :invalid-flag-value
                    {:flag (str "--" (name option)) :value value
                     :expected (flag-expected-type option)}))))

(defn safe-parse
  "raw-args -> {:args [...] :opts {...}}, or {:parse-error result} when
  babashka.cli's own parse-time ExceptionInfo fires (F2, R3-B2-2,
  ADR-0117): the library's own name and a source file:line used to leak
  straight to the operator, at the wrong exit code (1, an uncaught JVM
  exception, not the operational-error 2) -- see `parse-error-result`.
  An ExceptionInfo this doesn't recognize (no :option in its own
  ex-data) propagates unchanged, rethrown rather than silently
  misrepresented. This is the CLI's own parse boundary -- `dispatch`
  itself never calls `parse` and is unaffected; only `main!` (the real
  entry point) routes through here."
  [raw-args]
  (try
    (parse raw-args)
    (catch clojure.lang.ExceptionInfo e
      (if-let [r (parse-error-result e)]
        {:parse-error r}
        (throw e)))))

(def no-verdict-exit-code
  "Full exit-code mapping (ADR-0004, extended by tools/ADR-0010 for the
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
  contract, extended by tools/ADR-0010."
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
  tag, a separate, later change.

  Clarified 2026-08-05 (alignment fixes 1, ADR-0050, register row F-5):
  \"no version tag has been cut\" means no SEMVER release tag
  (`^v[0-9].*`, workspace.edn's own :release pattern) -- it is
  unaffected by the `stable-*` tags that exist from ADR-0048 onward.
  Those are continuity/verification points (ADR-0003's own trust
  boundary), a different kind of tag entirely, not a release."
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
  "SETUP.md section 4's walkthrough assumes every lockfile artifact that
  resolves through the artifact cache is already cached -- checked
  directly, per entry, rather than waiting for a mid-walkthrough
  failure to reveal it. Rows marked :resolved-via :deps-edn (P2-3,
  ruled 2026-07-31: review finding 8, the NIST engine's six lockfile
  rows) resolve through a project's own deps.edn -- a Maven coordinate
  into ~/.m2, engine-onboarding checklist item 4's third lockfile
  target -- never through this cache, so this check skips them rather
  than reporting a false gap; they're still listed, just not
  cache-checked, and the :detail line below says so explicitly so the
  human-readable story matches what's actually true."
  [artifacts resolve-artifact-fn]
  (let [deps-edn-resolved (filter #(= :deps-edn (:resolved-via %)) artifacts)
        cache-checked (remove #(= :deps-edn (:resolved-via %)) artifacts)
        per (map (fn [a] [a (resolve-artifact-fn artifacts (:name a) (:version a))]) cache-checked)
        failing (remove (fn [[_ r]] (result/ok? r)) per)]
    {:name "artifact cache (per lockfile entry)"
     :status (if (empty? failing) :pass :fail)
     :detail (if (empty? failing)
               (str (count cache-checked) " artifact(s) cached"
                    (when (seq deps-edn-resolved)
                      (str "; " (count deps-edn-resolved) " resolved via deps.edn (not cache-checked): "
                           (str/join ", " (map :name deps-edn-resolved)))))
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
  lockfile-reading command here). Both non-ok outcomes carry a :hint
  (2026-07-30 doctor-rendering session, the hint-family rule from the
  cold-start UX session: no category of doctor output is ever a dead
  end, even rendered by the generic fallback path). The exit-2 hint is
  attached here, at this CLI-boundary construction site, rather than
  inside kernel/artifact's shared read-lockfile -- default-lockfile-
  artifacts' other callers (fetch, generate, version, gate v2-nist)
  read the same lockfile and are untouched, out of this session's
  scope."
  [{:keys [lockfile resolve-java-bin-fn resolve-artifact-fn git-config-fn os-name-fn]
    :or {resolve-java-bin-fn generate/resolve-java-bin
         resolve-artifact-fn artifact/resolve-artifact
         git-config-fn real-git-config
         os-name-fn real-os-name}}]
  (let [artifacts-result (default-lockfile-artifacts lockfile)]
    (if-not (result/ok? artifacts-result)
      (update artifacts-result :payload assoc :hint
              (str "couldn't read the lockfile at " (:path (:payload artifacts-result))
                   " -- see SETUP.md section 1"))
      (let [artifacts (:payload artifacts-result)
            checks [(check-java-resolution artifacts resolve-java-bin-fn)
                    (check-artifact-cache artifacts resolve-artifact-fn)
                    (check-hooks-path git-config-fn)
                    (check-platform os-name-fn)]
            failing (filter #(= :fail (:status %)) checks)]
        (if (empty? failing)
          (result/ok {:checks checks})
          (result/rejected :doctor-checks-failed
                            {:checks checks
                             :hint (str (count failing) " check(s) failed: "
                                        (str/join ", " (map :name failing))
                                        " -- run: ehrt doctor --edn for the full per-check detail")}))))))

(def ^:private format-file-extension
  "The file extension `ehrt corpus mutate` selects :input files by, and
  reads/writes base-data/mutant through, per operator :format."
  {:fhir "json" :v2 "hl7"})

(defn- files-with-extension-in
  "A directory -> its *.<ext> files (via list-files-fn -- result/error
  :listing-failed on an I/O failure, never a silent empty seq, result
  or loud, ADR-0078), sorted for deterministic processing order; a
  file -> itself, as a single-element seq, result/ok. Returns a
  Result; callers must unwrap."
  [path ext list-files-fn]
  (let [f (io/file path)]
    (if (.isDirectory f)
      (let [r (list-files-fn f)]
        (if-not (result/ok? r)
          r
          (result/ok (->> (:payload r)
                           (filter #(str/ends-with? (.getName ^java.io.File %) (str "." ext)))
                           (sort-by #(.getName ^java.io.File %))))))
      (result/ok [f]))))

(defn- read-base-data
  "Reads a file into the shape corpus.mutate expects for format:
  parsed JSON data for :fhir, the raw ER7 string for :v2. Result or
  loud (ADR-0078, ADR-0096, D4-5/D8-3): result/ok the read value, or
  result/error :base-data-unreadable on a malformed or unreadable
  file -- the same try/catch-around-the-read shape
  ehrt.kernel.artifact/read-lockfile and ehrt.sim.run's config loader
  already use. Callers must unwrap."
  [format file]
  (try
    (result/ok (case format
                 :fhir (json/read-str (slurp file))
                 :v2 (slurp file)))
    (catch Exception e
      (result/error :base-data-unreadable {:path (str file) :message (.getMessage e)}))))

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
  tests never shell out to a real git process.

  :name is the literal \"ehrt\" (author ruling 2026-08-01, closing the
  named-future ADR-0018 left open): the product name, not a component
  layout artifact -- \"ehrt.tools\" was left over from the tools
  component's own pre-retirement name (ADR-0018) and would have needed
  editing again on the next unrelated internal rename. Pinned by
  ehrt.cli.core-test's own dedicated test so a future rename can't
  silently change this output vocabulary again."
  [git-describe-fn]
  {:name "ehrt" :identity repo-identity :git (git-describe-fn)})

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
  IS a data map, ehrt.corpus-io.interface's own item shape for
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
            base-data-result (read-base-data format f)]
        (if-not (result/ok? base-data-result)
          base-data-result
          (let [mutate-result (mutate/mutate (:payload base-data-result) operator locator-envelope)]
            (if-not (result/ok? mutate-result)
              mutate-result
              (recur (rest remaining)
                     (conj items (mutant->stdout-item
                                  format
                                  (or (:framing sink) source-sink/default-framing)
                                  (:mutant (:payload mutate-result))))))))))))

;; ---- ADR-0015: `ehrt corpus generate sim` -- the sim source's own
;; generation front door, alongside bare `ehrt corpus generate`/`ehrt
;; corpus generate synthea` (both still `generate/generate!`,
;; unchanged). No generation logic of its own: resolves CLI opts
;; against the :sim generator registry entry's own :default-params,
;; then drives that same entry's :out-dir-fn/:execute-fn -- the
;; registry (ehrt.corpus.generators) stays the single source of
;; what :sim generation does. ----

(defn generate-sim-command
  "`ehrt corpus generate sim` (ADR-0015): CLI opts -> the :sim
  generator registry entry's own :default-params (D9's zero-flag
  contract -- a zero-flag `generate sim` is a complete, deterministic
  command, sharing generate/default-seed with :synthea's own zero-flag
  default), merged params -> that entry's own :out-dir-fn for the
  derived out-dir, then its own :execute-fn. Rejects up front with the
  shared :out-dir-exists guard (ehrt.corpus.interface/out-dir-exists?/
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
        resolved-result (generators/generator-resolve-params :sim params)]
    (if-not (result/ok? resolved-result)
      resolved-result
      (let [entry (generators/generator-lookup :sim)
            merged (:payload resolved-result)
            resolved-out-dir (or out-dir ((:out-dir-fn entry) merged))
            exists-result (generate/out-dir-exists? resolved-out-dir)]
        (cond
          (not (result/ok? exists-result)) exists-result
          (:payload exists-result) (generate/out-dir-exists-error resolved-out-dir)
          :else ((:execute-fn entry) merged resolved-out-dir))))))

(defn- with-generate-breadcrumb
  "ADR-0015: attaches a `try: bin/ehrt show <out-dir>` breadcrumb to a
  successful generate result -- both `generate/generate!` (:synthea)
  and `generate-sim-command` (:sim) already echo :out-dir in their own
  OK payload, so no new data is needed, just a pointer at what to try
  next. Metadata only, per pretty-generic-summary's own docstring --
  never the payload, so the EDN/JSON envelope is unaffected."
  [r]
  (if (result/ok? r)
    (vary-meta r assoc :breadcrumb (str "try: bin/ehrt show " (:out-dir (:payload r))))
    r))

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
           git-describe-fn now-fn list-files-fn]
    :or {operator-version "1" git-describe-fn real-git-describe
         now-fn #(str (LocalDate/now))
         list-files-fn result/list-files}}]
  (let [operator (when operator-id (operators/operator-lookup (keyword operator-id) operator-version))]
    (cond
      ;; F4 (R3-B1-5, ADR-0117): a literally absent --operator-id is a
      ;; missing-required-flag invocation error (:missing-required-opt,
      ;; exit 2) -- distinct from a real, unrecognized id given, still
      ;; :unknown-operator/exit 1 below (a legitimate rejection: the
      ;; lookup ran, the id just doesn't exist).
      (nil? operator-id)
      (result/error :missing-required-opt
                    {:opt :operator-id :hint "--operator-id is required -- run: ehrt corpus operators"})

      (not operator)
      (result/rejected :unknown-operator
                        {:id operator-id :version operator-version
                         :valid-options (sort (map :id (operators/operator-entries)))
                         :hint "run: ehrt corpus operators"})

      ;; result or loud (ADR-0078, AR-RL-3): the same :file-not-found
      ;; category gate/sim run already give for a missing path, rather
      ;; than falling through into files-with-extension-in's own
      ;; single-file branch and a raw, unhandled exception at read time.
      (not (.exists (io/file path)))
      (result/error :file-not-found {:path path})

      :else
      (let [format (:format operator)
            stdout-result (stdout-out-dir-result out-dir)
            out-dir (or out-dir (default-mutate-out-dir path operator-id operator-version))
            locator-result (locator/make format (or locator-path (:default-locator operator)))]
        (if-not (result/ok? locator-result)
          locator-result
          (let [locator-envelope (:payload locator-result)
                files-result (files-with-extension-in path (format-file-extension format) list-files-fn)]
            (if-not (result/ok? files-result)
              files-result
              (let [files (:payload files-result)]
                (cond
                  (some? stdout-result)
                  (if-not (result/ok? stdout-result)
                    stdout-result
                    (mutate-to-stdout! format operator locator-envelope files (:payload stdout-result) stdout-out))

                  :else
                  (do
                    (kernel-io/mkdirs! (io/file out-dir))
                    (kernel-io/mkdirs! (io/file out-dir "lineage"))
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
                            ;; ADR-0015 breadcrumb: metadata only, never the
                            ;; payload -- invisible to pr-str/json/write-str,
                            ;; so the EDN/JSON envelope is unaffected; only
                            ;; pretty-generic-summary reads it.
                            (vary-meta (result/ok {:count (count processed) :files processed})
                                       assoc :breadcrumb (str "try: bin/ehrt gate " out-dir))))
                        (let [f (first remaining)
                              base-data-result (read-base-data format f)]
                          (if-not (result/ok? base-data-result)
                            base-data-result
                            (let [mutate-result (mutate/mutate (:payload base-data-result) operator locator-envelope)]
                              (if-not (result/ok? mutate-result)
                                mutate-result
                                (let [{:keys [mutant lineage]} (:payload mutate-result)
                                      basename (.getName f)]
                                  (write-mutant format (io/file out-dir basename) mutant)
                                  (spit (io/file out-dir "lineage" (str basename ".lineage.edn")) (pr-str lineage))
                                  (recur (rest remaining)
                                         (conj processed {:file basename :lineage-id (:id lineage)
                                                           :sha256 (:produced lineage) :input-hash (:parent lineage)})))))))))))))))))))

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
  first via generator-source/parse-source-designator; a generator-kind
  result resolves the generator for real
  (ehrt.corpus.interface/resolve-generator-source!, executing its
  engine and yielding a dir Source) before intaking it via intake/
  intake-via-source!.

  SS-3 Step 6 (ruling 5): :path may also be a stdin designator
  (\"stdin:?format=v2-er7&framing=er7-multi\") -- read (real System/in,
  or :in-override for tests), spooled
  (ehrt.corpus-io.interface/spool-resolve!, :captured-at the
  CLI's own wall-clock-now, the impure boundary matching :received's
  own discipline), then intaken via intake/intake-via-source! exactly
  like a resolved generator Source.

  Any other outcome (a bare directory path, a dir:/file: URL --
  already reduced to a bare path by dispatch's own
  resolve-path-designators before this function ever runs, per ruling
  7 -- or simply an unparseable string) falls through to the unchanged
  intake!/:source-dir path exactly as before SS-2. `ehrt corpus
  generate` itself is untouched by any of this -- its own verb, flags,
  and defaults do not change here.

  F3 (R3-B2-3 + R3-B4-1, ADR-0117): :out is REQUIRED -- validated
  first, before any generator/stdin resolution or file I/O begins
  (used to crash with a raw NullPointerException four layers deep in
  intake.clj when omitted). Ruled require-not-derive: a derived path
  would fold :received's own wall-clock default into a filesystem
  name, quietly unreproducible -- requiring is honest."
  [{:keys [path label out received in-override]}]
  (if (nil? out)
    (result/error :missing-required-opt
                  {:opt :out :hint "--out is required -- run: ehrt corpus intake PATH --out CATALOG-PATH"})
    (let [received (or received (str (LocalDate/now)))
          designator-result (generator-source/parse-source-designator path)]
      (cond
        (generator-url? designator-result)
        (let [source (:payload designator-result)
              resolved-result (generator-source/resolve-generator-source! (:kind source) (dissoc source :kind))]
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
                          :source-label label :out out :received received})))))

;; ---- ADR-0111: `ehrt corpus batch` -- a corpus-level tool,
;; deliberately separate from the sim (author ruling, Q1 a): it works
;; on any directory of valid v2 message files, including a foreign
;; corpus this repo never generated. Reuses the exact machinery `ehrt
;; play`'s own directory input already established: files-with-
;; extension-in for candidate *.hl7 discovery, player/split-er7-multi
;; (ehrt.corpus.interface, ADR-0100's own MSH-7-extraction seam's
;; sibling) for multi-message-file splitting -- never a second
;; implementation of either. The partitioning arithmetic itself is
;; ehrt.corpus-io.interface/partition-messages, corpus-io per Part 1 of
;; this session's own driving prompt; the wire wrapper is corpus-io's
;; own :batch framing codec (Q2 a). ----

(def ^:private batch-file-extension "hl7")

(defn- batch-out-dir
  "D12's own derived-out-dir pattern (docs/source-sink-design.md Part
  IX.5), applied here: <DIR>-batches/, the corpus group's own
  precedent (default-mutate-out-dir's sibling -- batch has no
  operator-id/version to fold in, so the derivation is simpler)."
  [path]
  (str path "-batches/"))

(defn- slurp-batch-input
  "Result or loud (ADR-0078), the same try/catch-around-the-read shape
  slurp-play-input already uses for directory input."
  [f]
  (try
    (result/ok (slurp f))
    (catch Exception e
      (result/error :batch-input-unreadable {:path (.getPath ^java.io.File f) :message (.getMessage e)}))))

(defn- read-batch-messages-from-file
  "f -> result/ok [{:message :source} ...] (one map per message the
  file contains, :source its own filename -- partition-messages' own
  categorized-error-naming-the-file law reads this field), or a
  categorized rejection: :batch-input-unreadable on an I/O failure, or
  :batch-input-malformed (player/split-er7-multi's own
  :malformed-er7-multi-frame rejection, re-categorized with :path
  added -- a directory candidate file that isn't actually a valid v2
  message is named by file, never a bare, file-less complaint)."
  [f]
  (let [content-result (slurp-batch-input f)]
    (if-not (result/ok? content-result)
      content-result
      (let [split-result (player/split-er7-multi (:payload content-result))]
        (if-not (result/ok? split-result)
          (result/error :batch-input-malformed
                        (assoc (:payload split-result) :path (.getPath ^java.io.File f)))
          (result/ok (mapv (fn [m] {:message m :source (.getName ^java.io.File f)})
                            (:payload split-result))))))))

(defn- read-all-batch-messages
  "files (sorted) -> result/ok [tagged messages across every file, in
  file order] -- concatenation order is irrelevant to the eventual
  batches (partition-messages sorts globally by MSH-7), but fail-fast
  per-file order still determines WHICH file a read failure names
  first, the same discipline mutate-command's own per-file loop uses."
  [files]
  (loop [remaining files acc []]
    (if (empty? remaining)
      (result/ok acc)
      (let [r (read-batch-messages-from-file (first remaining))]
        (if-not (result/ok? r)
          r
          (recur (rest remaining) (into acc (:payload r))))))))

(defn- write-batch-file!
  "Writes bs (a :batch-encoded byte array) to file, raw bytes, no
  charset conversion -- the same .write-an-OutputStream shape
  write-stdout! (corpus-io) already uses for byte-exact framed output,
  applied here to a real file instead of a stream."
  [^java.io.File file ^bytes bs]
  (io/make-parents file)
  (with-open [os (io/output-stream file)]
    (.write os bs)))

(defn- write-and-verify-batch!
  "One occupied bucket -> writes out-dir/batch-NNN.hl7 in :batch
  framing, then decodes what was just written straight back
  (corpus-io/decode :batch, over the in-memory bytes -- no re-read)
  to self-check BTS-1 against the real message count before ever
  claiming success: the free transport-integrity check the codec's own
  decode already performs, exercised here rather than merely trusted.
  Returns result/ok a summary map, or result/error
  :batch-self-check-failed (should never fire in practice -- encode's
  own BTS-1 is always the true count by construction; this only guards
  against a future encode/decode drift going undetected)."
  [out-dir index {:keys [start-ms end-ms messages]}]
  (let [items (mapv #(.getBytes ^String (:message %) "UTF-8") messages)
        encoded (corpus-io/encode :batch items)
        filename (format "batch-%03d.hl7" index)
        file (io/file out-dir filename)]
    (write-batch-file! file (:payload encoded))
    (let [verify-result (corpus-io/decode :batch (:payload encoded))]
      (if (and (result/ok? verify-result) (= (count messages) (count (:payload verify-result))))
        (result/ok {:file filename :count (count messages)
                    :start-ms start-ms :end-ms end-ms :verified true})
        (result/error :batch-self-check-failed
                      {:file filename :expected (count messages) :verify-result verify-result})))))

(defn batch-command
  "`ehrt corpus batch DIR --interval MINUTES --out-dir OUT` (ADR-0111):
  reads every candidate *.hl7 file directly under DIR (multi-message
  files split via player/split-er7-multi, the same machinery `ehrt
  play`'s own directory input already uses), sorts EVERY message
  across every file by its own MSH-7 (never file order -- the wire's
  own transmit order is the batch order), partitions into --interval-
  minute buckets aligned to the Unix epoch (so hourly batches align to
  the hour, daily to UTC midnight -- ehrt.corpus-io.interface/
  partition-messages's own law), and writes one batch-NNN.hl7 per
  occupied bucket (empty buckets skipped, v1) in :batch (BHS/BTS)
  framing, numbered sequentially over occupied buckets only (never the
  bucket's own absolute epoch index).

  DIR is deliberately corpus-agnostic (author ruling 2026-08-11, Q1 a):
  any directory of valid v2 message files works, including a foreign
  corpus this repo never generated -- nothing here reads a manifest,
  a catalog, or anything sim/generator-specific.

  :interval is REQUIRED (:missing-required-opt when absent -- retired
  the verb-specific :interval-required, F4, R3-B1-5, ADR-0117) -- there
  is no universally sensible default schedule to assume (D8's
  determinism law governs defaults, not requiredness; this mirrors
  `sim run --seed`'s own \"determinism is a feature, not a default\"
  reasoning). Must be positive (:interval-must-be-positive otherwise).
  :out-dir defaults to (batch-out-dir path) when omitted (D12's derived-
  out-dir pattern) and is rejected :out-dir-exists when it already
  exists and is non-empty (the same guard generate-sim-command's own
  zero-flag determinism story uses) -- never silently overwritten.

  Missing :path names :file-not-found; a DIR with no *.hl7 candidate
  files names :batch-input-empty; an unreadable or malformed candidate
  file names :batch-input-unreadable/:batch-input-malformed, by path;
  a message whose own MSH-7 doesn't parse propagates partition-
  messages' own :unparseable-transmit-time, naming the file -- fail
  fast, never a silent skip (the corpus is presumed foreign-but-valid).

  :list-files-fn is injectable (defaults to result/list-files), the
  same hermetic seam mutate-command's own directory read already uses."
  [{:keys [path interval out-dir list-files-fn]
    :or {list-files-fn result/list-files}}]
  (cond
    (not (.exists (io/file path)))
    (result/error :file-not-found {:path path})

    (nil? interval)
    (result/error :missing-required-opt
                  {:opt :interval :hint "--interval MINUTES is required -- e.g. --interval 60 for hourly batches, 1440 for daily"})

    (not (pos? interval))
    (result/rejected :interval-must-be-positive {:value interval :hint "must be a positive number of minutes"})

    :else
    (let [resolved-out-dir (or out-dir (batch-out-dir path))
          exists-result (generate/out-dir-exists? resolved-out-dir)]
      (cond
        (not (result/ok? exists-result)) exists-result
        (:payload exists-result) (generate/out-dir-exists-error resolved-out-dir)

        :else
        (let [files-result (files-with-extension-in path batch-file-extension list-files-fn)]
          (if-not (result/ok? files-result)
            files-result
            (let [files (:payload files-result)]
              (if (empty? files)
                (result/error :batch-input-empty
                              {:path path :hint "ehrt corpus batch found no *.hl7 candidate files in this directory"})
                (let [messages-result (read-all-batch-messages files)]
                  (if-not (result/ok? messages-result)
                    messages-result
                    (let [partition-result (corpus-io/partition-messages
                                            (:payload messages-result)
                                            {:interval-ms (* 60000 (long interval))})]
                      (if-not (result/ok? partition-result)
                        partition-result
                        (let [buckets (:buckets (:payload partition-result))]
                          (kernel-io/mkdirs! (io/file resolved-out-dir))
                          (loop [remaining (map-indexed vector buckets) written []]
                            (if (empty? remaining)
                              (let [span (when (seq written)
                                           {:earliest-ms (:start-ms (first written))
                                            :latest-ms (:end-ms (last written))})]
                                (vary-meta (result/ok {:out-dir resolved-out-dir
                                                       :interval-ms (* 60000 (long interval))
                                                       :batches written
                                                       :span span})
                                          assoc :breadcrumb (str "try: bin/ehrt show " resolved-out-dir)))
                              (let [[index bucket] (first remaining)
                                    write-result (write-and-verify-batch! resolved-out-dir index bucket)]
                                (if-not (result/ok? write-result)
                                  write-result
                                  (recur (rest remaining) (conj written (:payload write-result))))))))))))))))))))

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
  (let [entries (operators/operator-entries)
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
  "The policy-totality law (tools/ADR-0010, D10) made total in code: :ok,
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

(defn- gate-unreadable-file-error
  "Result or loud (ADR-0078, AR-RL-3; review-3 D4-3): result/error
  :path-unreadable when `f` names a file that exists but whose bytes
  cannot actually be read, nil when there is nothing to report -- the
  same category, and the same try/catch-around-the-read shape,
  `show`'s own sniff-path-format/show-file already use.

  Category honesty is the whole point. The three judge engines below
  all name this condition :file-not-found {:reason :permission-denied}
  (ADR-0098's own ruled engine-level shape, untouched here), so `ehrt
  gate` and `ehrt show` used to disagree about one condition on one
  file: show said the file could not be read, gate said it did not
  exist, and a stranger diagnosing a permissions problem was pointed at
  the wrong fix. This runs first so the CLI surface answers with one
  voice.

  A MISSING path is deliberately left alone: it is not a file, this
  check does not fire, and the engine's own :file-not-found stays the
  right answer for it."
  [^java.io.File f]
  (when (.isFile f)
    (try
      (with-open [_ (io/input-stream f)] nil)
      (catch Exception e
        (result/error :path-unreadable {:path (.getPath f) :message (.getMessage e)})))))

(defn- sampled-gate-entries
  "Every *.hl7 file under `dir`, with its MSH-9/MSH-10 read off the
  first line. The read is `report/sampling-header`'s two-field split,
  not a parse: this runs over a corpus that may be too expensive to
  gate in full, so it must cost far less than the gating it is deciding
  about.

  THE `slurp` IS GUARDED (ADR-0096's own read-guard rule, and
  `ehrt.cli.cli-parse-guard-lint-test` is what said so). A file that
  cannot be read is not an error HERE: it becomes an entry with no
  MSH-9, which `stratified-selection` puts in the `unknown` stratum and
  gates in FULL -- so the failure is reported by the gate engine's own
  categorized `:file-not-found {:reason :permission-denied}` path
  rather than as a raw exception thrown past the CLI boundary by the
  code that was only deciding what to gate. Fail-safe, not fail-open:
  the unreadable file is gated, never sampled away."
  [dir]
  (->> (file-seq (io/file dir))
       (filter #(and (.isFile ^java.io.File %)
                     (str/ends-with? (.getName ^java.io.File %) ".hl7")))
       (mapv (fn [f]
               (let [{:keys [msh-9 msh-10]}
                     (try (report/sampling-header (slurp f))
                          (catch Exception _ nil))]
                 {:path (.getPath ^java.io.File f)
                  :msh-9 msh-9
                  :msh-10 (or msh-10 (.getName ^java.io.File f))})))))

(defn- sampled-gate-dir
  "`gate-dir`'s sampled sibling (ADR-0175 ruling D1). Returns kernel/ok
  `{:results [...] :sampling {:cap :strata}}` -- the per-stratum census
  rides the result so `gate-command` can put it on the report's own
  `:run` map. NO SILENT CAPS: every stratum's `n` and `gated` is in
  there whether it was capped or not."
  [gate-file-fn dir cap]
  (let [entries (sampled-gate-entries dir)
        {:keys [selected strata]} (report/stratified-selection
                                   entries {:skeleton-types emit-hl7/skeleton-message-types :cap cap})]
    (result/ok {:results (mapv #(:payload (gate-file-fn (:path %))) selected)
                :sampling {:cap cap :strata strata}})))

(defn gate-command
  "Builds an `ehrt gate <format>` command function from that format's
  gate-file/gate-dir functions (an engine interface, e.g. ehrt.judge-v2-hapi.interface, and
  eventually judge.fhir -- same shape). :path may name a single file or
  a directory; either way the result is normalized into one
  judge.report (gate-label identifies which gate ran, in :run). Writes
  the report to :report when given (EDN, canonical -- ADR-0004), via
  `write-report!`: missing parent directories are created, and a
  residual IO failure is returned as :report-write-failed (exit 2)
  *instead of* the verdict -- a run whose recorded output didn't land
  is an operational failure, not a judgment.

  :baseline (P6, a path to a previously-written --report EDN file)
  switches to baseline-relative mode (ehrt.judge.interface/
  baseline-relative-report): the written/returned payload becomes
  {:absolute :relative} instead of a bare Report, and the exit-code
  decision below follows :relative's totals, not :absolute's -- see
  docs/judge-calibration.md for when to reach for this and its exact-
  match limitation. (:relative verdicts are always binary, so
  :no-verdict never actually appears there in practice -- `gate-decision`
  is applied uniformly anyway, for one policy-totality law rather than
  two near-duplicate ones.)

  :treat-no-verdict-as (tools/ADR-0010, D10) is \"pass\" or \"rejected\" (a
  string, validated by `parse-treat-no-verdict-as` before anything else
  runs); anything else is rejected with :invalid-treat-no-verdict-as.

  Exit-code contract (ADR-0004's generic ok/rejected/error mapping,
  extended by tools/ADR-0010 -- see `result->exit-code`): result/ok when the
  aggregate has zero rejected files and zero no-verdict files;
  result/rejected :gate-rejected the moment any file was rejected (or
  --treat-no-verdict-as rejected folds a no-verdict file in);
  result/rejected :gate-no-verdict when the aggregate has a no-verdict
  file and no --treat-no-verdict-as policy was given -- the CLI's own
  distinct exit code for that case, so no workflow silently inherits a
  no-verdict-handling default."
  [gate-file-fn gate-dir-fn gate-label]
  (fn [{:keys [path report baseline treat-no-verdict-as sample-add-ons]}]
    (let [policy-result (parse-treat-no-verdict-as treat-no-verdict-as)]
      (if-not (result/ok? policy-result)
        policy-result
        (let [policy (:payload policy-result)
              f (io/file path)
              results-result (if (.isDirectory f)
                                (if (and sample-add-ons (pos? sample-add-ons))
                                  (sampled-gate-dir gate-file-fn path sample-add-ons)
                                  (gate-dir-fn path))
                                (or (gate-unreadable-file-error f)
                                    (let [r (gate-file-fn path)]
                                      (if (result/ok? r)
                                        (result/ok {:results [(:payload r)]})
                                        r))))]
          (if-not (result/ok? results-result)
            results-result
            (let [results (:results (:payload results-result))
                  ;; ARC 4 SWEEP 2 (ADR-0175 ruling D1): `no silent
                  ;; caps`. The per-stratum census rides the report's
                  ;; own `:run` map, so a sampled report says on its
                  ;; face which families were capped and by how much --
                  ;; a truncation nobody prints reads as full coverage.
                  run (cond-> {:gate gate-label :path path}
                        (:sampling (:payload results-result))
                        (assoc :sampling (:sampling (:payload results-result))))]
              (if baseline
                (let [baseline-result
                      (try
                        (result/ok (edn/read-string (slurp baseline)))
                        (catch Exception e
                          (result/error :baseline-unreadable
                                         {:path baseline :message (.getMessage e)})))]
                  (if-not (result/ok? baseline-result)
                    baseline-result
                    (let [br (report/baseline-relative-report results run (:payload baseline-result))
                          decision (gate-decision (:totals (:relative br)) policy)
                          write-error (when report (write-report! report br))]
                      (or write-error (decision->result decision br)))))
                (let [rpt (report/build-report results run)
                      decision (gate-decision (:totals rpt) policy)
                      write-error (when report (write-report! report rpt))]
                  (or write-error (decision->result decision rpt)))))))))))

(def gate-v2-command
  (gate-command gate-v2/gate-file gate-v2/gate-dir :v2))

(def default-fhir-gate-scratch-dir
  "out/scratch/gate-fhir")

(defn fhir-gate-command
  "`ehrt gate fhir`: unlike judge.v2 (fully self-contained, no options),
  judge.fhir needs the lockfile's artifacts plus a scratch directory
  for the validator's raw OperationOutcome output and invocation logs
  -- resolved here, then judge.fhir/gate-file and gate-dir are curried
  down to the 1-arity shape gate-command expects. :scratch-dir defaults
  to `out/scratch/gate-fhir` (ADR-0013: the single tool-owned,
  gitignored out/ root -- moved from a bare target/gate-fhir); :java-bin,
  when given, bypasses registry resolution exactly like corpus.generate's
  own :java-bin override. :treat-no-verdict-as (tools/ADR-0010) passes straight
  through to gate-command. :no-verdict-cache (ADR-0016) disables the
  content-addressed verdict cache at the validator seam for this
  invocation -- judge.fhir/gate-file's own :verdict-cache? false, the
  escape hatch for when the cache's determinism assumption is ever
  suspect; caching stays on by default.

  F7 (R3-B1-1, RULED ADR-0115 RQ1, ADR-0117): this CLI option was
  :out-dir/--out-dir; renamed to :scratch-dir/--scratch-dir, no
  back-compat alias -- corpus generate/mutate/batch's own --out-dir
  names a protected, collision-refused output artifact, an unrelated
  concept this flag only happened to share a name with (a freely
  reusable validator scratch directory, never an artifact). judge-fhir-
  official's own internal :out-dir parameter name (in fhir-opts, below)
  is untouched -- this rename is CLI-surface only."
  [{:keys [path report lockfile scratch-dir java-bin baseline treat-no-verdict-as no-verdict-cache]}]
  (let [artifacts-result (default-lockfile-artifacts lockfile)]
    (if-not (result/ok? artifacts-result)
      artifacts-result
      (let [fhir-opts (cond-> {:artifacts (:payload artifacts-result)
                                :out-dir (or scratch-dir default-fhir-gate-scratch-dir)}
                        java-bin (assoc :java-bin java-bin)
                        no-verdict-cache (assoc :verdict-cache? false))
            gate-fn (gate-command #(gate-fhir/gate-file % fhir-opts)
                                  #(gate-fhir/gate-dir % fhir-opts)
                                  :fhir)]
        (gate-fn {:path path :report report :baseline baseline
                  :treat-no-verdict-as treat-no-verdict-as})))))

;; ---- ADR-0015: `ehrt gate v2-nist` -- picks up ADR-0012's own
;; skipped CLI expansion. As of the judge-family parity pass (P2-2,
;; ruled 2026-07-31), ehrt.judge-v2-nist.interface/gate-file/-dir
;; return the same kernel/ok {:verdict :findings :path [:cause]} /
;; kernel/error envelope judge-v2-hapi's own engine does, and walk
;; recursively like judge-v2-hapi's own gate-dir now does too -- these
;; adapters only need to catch the engine's own :ambiguous-msg-id
;; ex-info (ADR-0012's deliberate caller-contract throw) and surface it
;; as a named CLI error instead of an uncaught stack trace. ----

(defn- v2-nist-wrap-ambiguous-msg-id
  [path ex-data-map]
  (result/error :v2-nist-ambiguous-msg-id
                (merge {:path path
                        :hint "this profile bundle declares more than one msg-id -- ehrt gate v2-nist has no --msg-id flag yet; narrow --profile to a single-msg-id bundle"}
                       ex-data-map)))

(defn- v2-nist-gate-file*
  "Delegates to ehrt.judge-v2-nist.interface/gate-file, which now
  returns the kernel envelope directly, including kernel/error
  :file-not-found for a missing path (parity pass, ruled 2026-07-31) --
  catches the engine's own :ambiguous-msg-id ex-info (ADR-0012's
  deliberate caller-contract throw) and surfaces it as a named CLI
  error instead of an uncaught stack trace; everything else the engine
  can throw propagates unchanged, since only this one exception type is
  a recognized, named condition at this seam."
  [validator-state path]
  (try
    (gate-v2-nist/gate-file validator-state (io/file path))
    (catch clojure.lang.ExceptionInfo e
      (if (= :ambiguous-msg-id (:type (ex-data e)))
        (v2-nist-wrap-ambiguous-msg-id path (ex-data e))
        (throw e)))))

(defn- v2-nist-gate-dir*
  "Delegates to ehrt.judge-v2-nist.interface/gate-dir, itself now
  kernel-enveloped and recursive like judge-v2-hapi's own gate-dir
  (parity pass, ruled 2026-07-31) -- catches :ambiguous-msg-id exactly
  like v2-nist-gate-file* above."
  [validator-state dir]
  (try
    (gate-v2-nist/gate-dir validator-state dir)
    (catch clojure.lang.ExceptionInfo e
      (if (= :ambiguous-msg-id (:type (ex-data e)))
        (v2-nist-wrap-ambiguous-msg-id dir (ex-data e))
        (throw e)))))

(def default-v2-nist-profile-hint
  "test-fixtures/v2-nist/COVID19_ELR-v2.3.1 -- the CDC COVID19_ELR-v2.3.1 fixture, this repo's own documented try-it bundle (ADR-0012/ADR-0015)")

(defn v2-nist-gate-command
  "`ehrt gate v2-nist PATH --profile BUNDLE_DIR` (ADR-0015): the
  profile-tier NIST engine (ADR-0012), reaching the CLI for the first
  time. --profile is REQUIRED -- there is no project-owned default
  profile yet (ADR-0012's own \"stand-in, not this project's own
  profile\" disclosure), so an absent --profile is a named rejection,
  never a silently-assumed bundle. The validator is built exactly ONCE
  per invocation (:make-validator-fn, defaulting to
  ehrt.judge-v2-nist.interface/make-validator, injectable so a test can
  count calls) -- context construction dominates this engine's own
  cost (ADR-0012), so gate-command's own gate-file-fn/gate-dir-fn
  closures below close over one already-built validator-state, never
  rebuilding it per file. A malformed --profile (missing PROFILE.xml,
  or anything else v2-nist-make-validator itself throws on -- one of
  this workspace's few deliberate throw sites, a caller-contract
  violation per ADR-0012) is caught here and surfaced as a named
  operational error, not an uncaught stack trace."
  [{:keys [path report profile baseline treat-no-verdict-as make-validator-fn]
    :or {make-validator-fn gate-v2-nist/make-validator}}]
  ;; F4 (R3-B1-5, ADR-0117): retired :v2-nist-profile-required in favor
  ;; of the shared :missing-required-opt shape at exit 2.
  (if (str/blank? profile)
    (result/error :missing-required-opt
                  {:opt :profile :hint (str "--profile BUNDLE_DIR is required -- try " default-v2-nist-profile-hint)})
    (let [validator-result (try
                              (result/ok (make-validator-fn profile))
                              (catch Exception e
                                (result/error :v2-nist-profile-error
                                              {:profile profile :message (.getMessage e)
                                               :hint (str "check --profile names a directory containing a valid PROFILE.xml -- try " default-v2-nist-profile-hint)})))]
      (if-not (result/ok? validator-result)
        validator-result
        (let [validator-state (:payload validator-result)
              gate-fn (gate-command (partial v2-nist-gate-file* validator-state)
                                    (partial v2-nist-gate-dir* validator-state)
                                    :v2-nist)]
          (gate-fn {:path path :report report :baseline baseline
                    :treat-no-verdict-as treat-no-verdict-as}))))))

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
  mistaken for an unclassifiable gate candidate. Result or loud
  (ADR-0078): result/ok a vector of candidate files, or result/error
  :listing-failed on an I/O failure listing dir; callers must unwrap."
  [dir]
  (let [r (result/list-files (io/file dir))]
    (if-not (result/ok? r)
      r
      (result/ok (->> (:payload r)
                       (filter #(.isFile ^java.io.File %))
                       (filter #(contains? gate-candidate-extensions
                                            (last (str/split (.getName ^java.io.File %) #"\."))))
                       (sort-by #(.getName ^java.io.File %)))))))

(def ^:private sniffed-format->gate-label
  {:fhir-json :fhir :v2-er7 :v2})

(defn- sniff-path-format
  "Result or loud (ADR-0078, ADR-0096, D8-3): result/ok the sniffed
  gate label (:fhir, :v2, or nil for unrecognized), or result/error
  :path-unreadable on a read failure -- the same try/catch-around-the-
  read shape ehrt.kernel.artifact/read-lockfile and ehrt.sim.run's
  config loader already use. Callers must unwrap."
  [f]
  (try
    (result/ok (get sniffed-format->gate-label (intake/sniff-format (slurp f))))
    (catch Exception e
      (result/error :path-unreadable {:path (.getPath ^java.io.File f) :message (.getMessage e)}))))

(defn- sniff-files
  "Sniffs every file in `files` via sniff-path-format, short-circuiting
  on the first read failure (ADR-0096, D8-3): result/ok a vector of
  [filename gate-label] pairs, or the first result/error encountered."
  [files]
  (reduce (fn [acc-result file]
            (let [r (sniff-path-format file)]
              (if-not (result/ok? r)
                (reduced r)
                (result/ok (conj (:payload acc-result) [(.getName ^java.io.File file) (:payload r)])))))
          (result/ok [])
          files))

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
      (let [sniff-result (sniff-path-format f)]
        (if-not (result/ok? sniff-result)
          sniff-result
          (case (:payload sniff-result)
            :fhir (gate-fhir-fn opts)
            :v2 (gate-v2-fn opts)
            (ambiguous-format-error path {:unrecognized-files [path]}))))

      :else
      (let [files-result (gate-candidate-files-in path)]
        (if-not (result/ok? files-result)
          files-result
          (let [files (:payload files-result)]
            (if (empty? files)
              (ambiguous-format-error path {:reason :no-candidate-files})
              (let [sniffed-result (sniff-files files)]
                (if-not (result/ok? sniffed-result)
                  sniffed-result
                  (let [sniffed (:payload sniffed-result)
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
                            :v2 (gate-v2-fn opts)))))))))))))))

;; ---- ADR-0013: `ehrt show` -- the pretty-always display verb. Joins
;; D11's own sniff dispatch (gate-candidate-files-in/sniff-path-format
;; above) rather than inventing a second one; rendering itself lives in
;; ehrt.corpus.display, required here as `display`. ----

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
  (let [sniff-result (sniff-path-format f)]
    (if-not (result/ok? sniff-result)
      sniff-result
      (let [gate-label (:payload sniff-result)]
        (if (nil? gate-label)
          (show-ambiguous-error (.getPath f) {:unrecognized-files [(.getPath f)]})
          (try
            (render-sniffed-content gate-label (slurp f))
            (catch Exception e
              (result/error :path-unreadable {:path (.getPath f) :message (.getMessage e)}))))))))

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
  anything -- ehrt.corpus.display's own functions only ever read the
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
      (let [files-result (gate-candidate-files-in path)]
        (if-not (result/ok? files-result)
          files-result
          (let [files (:payload files-result)]
            (if (empty? files)
              (show-ambiguous-error path {:reason :no-candidate-files})
              (let [sniffed-result (sniff-files files)]
                (if-not (result/ok? sniffed-result)
                  sniffed-result
                  (let [sniffed (:payload sniffed-result)
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
                               (result/ok (str/join "\n\n" (map :payload rendered)))))))))))))))))))

;; ---- ADR-0014: `ehrt play`'s executor -- folds player/plan's own
;; emission plan through an injected :sleep-fn and one sink function.
;; This is the only place in this file that ever sleeps for the
;; player's own sake; the plan itself (ehrt.corpus.player) never does. ----

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

(defn- format-day-offset-hms
  "seconds (a sim ground-truth event's own :t, sim/ADR-0011 -- already
  an offset from the run's own epoch, no subtraction needed) -> \"Dd
  HH:MM:SS\". The event-line ticker's own clock (ADR-0100)."
  [seconds]
  (let [total (long seconds)
        days (quot total 86400)
        rem-s (mod total 86400)]
    (format "%dd %02d:%02d:%02d" days (quot rem-s 3600) (quot (mod rem-s 3600) 60) (mod rem-s 60))))

(defn- render-event-location
  "A ground-truth event's own :location ({:ward :bed ...}, compile-
  trajectory's own admission/discharge/transfer shape) -> \"ward/bed\",
  \"ward\" alone when no :bed, or nil when :location itself is absent
  -- outpatient-visit/observation/etc. carry none."
  [location]
  (when location
    (let [{:keys [ward bed]} location]
      (cond
        (and ward bed) (str ward "/" bed)
        ward (str ward)
        :else nil))))

(defn- render-event-citation
  "A ground-truth event's own :citation ({:module :state}, glass-box
  traceability) -> \"[module/state]\", or nil when either half is
  absent -- an engine-native churn event (:transfer, :bed-swap, ...)
  carries no citation at all."
  [citation]
  (when citation
    (let [{:keys [module state]} citation]
      (when (and module state) (str "[" module "/" state "]")))))

(defn- event-line-sink
  "The compact event-line ticker for sim event-log input (ADR-0100):
  one line per event -- :t as a day+HH:MM:SS offset from the run's own
  epoch, the event kind (:event -- ehrt.patient-simulator.compile-
  trajectory's own key, NOT :type), :location when present, :citation
  when present. Both --ticker full and --ticker line render this SAME
  line for event input: there is no wire-format \"full\" rendering for
  a compiled ground-truth event, so the mode distinction that matters
  for message input collapses to one rendering here. A missing/
  unparseable :t renders \"?\", the same never-throw convention
  ticker-line-sink already applies to a missing MSH field."
  [println-fn]
  (fn [event]
    (println-fn (str (if-let [ts-ms (player/event-timestamp-ms event)]
                        (format-day-offset-hms (/ ts-ms 1000))
                        "?")
                      "  " (or (:event event) "?")
                      (when-let [loc (render-event-location (:location event))] (str "  " loc))
                      (when-let [cit (render-event-citation (:citation event))] (str "  " cit))))))

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

(defn- board-sink
  "AR-BB2-3/4 (player board, `notes/ADRs.md` ADR-0067): the bed
  board's own sink -- closes over a fold accumulator and the board's
  own snapshot cadence. Each event folds via
  `ehrt.corpus.interface/board-fold-event` (the exported
  `ehrt.sim-emit-hl7.interface/fold-message`, wrapped); a message
  whose own trigger is outside the emitter's handled set is SKIPPED
  (the accumulator held unchanged) with a cue through the same
  println-fn the ticker itself would use -- board IS the display, so
  the cue prints inline, never routed to stderr (the cue rule,
  ADR-0014).

  The boundary invariant (ADR-0103): boundaries live on a grid at
  `first-ts + k*span`. At most one snapshot renders per grid window
  that contains messages, rendered at the first message at or after
  each crossed boundary; a message with no parseable timestamp
  neither advances the anchor nor crosses a boundary (the pacer's own
  lenient posture). After rendering at `ts`, the next boundary is the
  smallest grid point strictly greater than `ts`, computed
  arithmetically (`first-ts + span * (1 + floor((ts - first-ts) /
  span))`) -- never by looping span-at-a-time, so an arbitrarily large
  stream-time jump (an idle-skip) costs one division, not one
  iteration per crossed span. Empty windows inside a gap render
  nothing. Returns {:sink-fn :cue-fn :finalize-fn :snapshot-count-fn
  :unfolded-count-fn} -- play-command calls `finalize-fn` once after
  `run-plan!` completes (an unconditional final snapshot at the last
  timestamp seen, regardless of boundary position), then reads the
  two *-count-fn calls for the result envelope."
  [board-minutes println-fn]
  (let [acc-atom (atom {})
        first-ts (atom nil)
        next-boundary-ms (atom nil)
        last-ts (atom nil)
        snapshot-count (atom 0)
        unfolded-count (atom 0)
        boundary-span-ms (* board-minutes 60000)
        render! (fn [ts]
                  (println-fn (board/board-render-snapshot @acc-atom ts))
                  (swap! snapshot-count inc))
        next-boundary-after (fn [ts]
                               (+ @first-ts (* boundary-span-ms
                                                (inc (quot (- ts @first-ts) boundary-span-ms)))))
        maybe-snapshot! (fn [ts]
                          (when ts
                            (reset! last-ts ts)
                            (if (nil? @first-ts)
                              (do (reset! first-ts ts)
                                  (reset! next-boundary-ms (+ ts boundary-span-ms)))
                              (when (>= ts @next-boundary-ms)
                                (render! ts)
                                (reset! next-boundary-ms (next-boundary-after ts))))))]
    {:cue-fn (fn [_idx] (println-fn "-- idle-skip: stream-time jumped --"))
     :sink-fn (fn [event]
                (let [{:keys [acc unfolded?]} (board/board-fold-event @acc-atom event)]
                  (if unfolded?
                    (do (println-fn "-- unfolded: unsupported message trigger, skipped --")
                        (swap! unfolded-count inc))
                    (reset! acc-atom acc)))
                (maybe-snapshot! (player/message-timestamp-ms event)))
     :finalize-fn (fn [] (when @last-ts (render! @last-ts)))
     :snapshot-count-fn (fn [] @snapshot-count)
     :unfolded-count-fn (fn [] @unfolded-count)}))

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
  (cond
    (str/includes? designator "?") designator
    ;; ARC 4 SWEEP 5 (ADR-0175 design (g)): a `:mllp` sink IMPLIES
    ;; `:framing :mllp` and REJECTS any other framing at construction,
    ;; so filling in the er7-multi default here would make every bare
    ;; `mllp://host:port` an :invalid-sink. The implication is honoured
    ;; rather than worked around: the framing is left absent, which is
    ;; what the schema already means by "implied".
    (str/starts-with? designator "mllp:") (str designator "?format=v2-er7")
    :else (str designator "?format=v2-er7&framing=er7-multi")))

(defn- resolve-play-sink
  "--sink DESIGNATOR (ADR-0014: reuses the existing source-sink
  designator vocabulary, never a parallel flag scheme) -> kernel/ok
  {:sink-fn :close-fn :cue-fn} for a supported kind, or an operational
  error naming what's unsupported.

  ARC 4 SWEEP 5 (ADR-0175 design (g)): `:mllp` joins `:file`, and it
  cost NO NEW FLAG -- the prompt sketched `--transport`, and
  `--sink mllp://host:port` was chosen instead because `--sink` already
  takes a designator and already inherits ADR-0017's vocabulary and its
  round-trip parse/print law. Disclosed as a deviation from that
  sketch. `:dir`/`:blaze` remain named, disclosed deferrals
  (ADR-0014's own bail-out procedure); nil sink (no --sink given) means
  \"use the ticker\" and isn't resolved here.

  The MLLP leg returns two extra keys the ticker/file legs do not:
  `:failure-fn`, whose non-nil value `play-command` returns INSTEAD of
  a success (a negative acknowledgement, a timeout or a closed stream
  must make the command fail, not print a cheerful summary), and
  `:summary-fn`, merged into the ok payload as `:mllp`."
  [designator println-fn]
  (let [parsed (source-sink-url/parse-sink-designator (ensure-default-play-sink-format designator))]
    (if-not (result/ok? parsed)
      parsed
      (let [{:keys [kind path host port]} (:payload parsed)]
        (case kind
          :file (let [{:keys [sink-fn close-fn]} (file-sink-fn path)]
                  (result/ok {:sink-fn sink-fn :close-fn close-fn :cue-fn stderr-cue-fn}))
          :mllp (let [opened (source-sink/mllp-open-sink! host port)]
                  (if-not (result/ok? opened)
                    opened
                    (let [{:keys [send-fn failure-fn summary-fn close-fn]} (:payload opened)]
                      (result/ok {:sink-fn send-fn :close-fn close-fn :cue-fn stderr-cue-fn
                                  :failure-fn failure-fn :summary-fn summary-fn}))))
          (result/error :play-sink-kind-unsupported
                        {:kind kind :path path
                         :hint "ehrt play supports file: and mllp:// sinks -- dir:/blaze: are named, disclosed deferrals (ADR-0014)"}))))))

(defn- slurp-play-input
  "Result or loud (ADR-0096 Finding 2, ADR-0100): result/ok the file's
  content, or result/error :play-input-unreadable on a read failure --
  the same try/catch-around-the-read shape sniff-path-format/read-
  base-data already use. Callers must unwrap."
  [f]
  (try
    (result/ok (slurp f))
    (catch Exception e
      (result/error :play-input-unreadable {:path (.getPath ^java.io.File f) :message (.getMessage e)}))))

(defn- play-events-from-file
  [f]
  (let [sniff-result (sniff-path-format f)]
    (if-not (result/ok? sniff-result)
      sniff-result
      (if-not (= :v2 (:payload sniff-result))
        (result/error :play-input-unsupported
                      {:path (.getPath f) :hint "ehrt play only supports HL7 v2 (ER7) input this session -- FHIR is a named, disclosed deferral (ADR-0014)"})
        (let [content-result (slurp-play-input f)]
          (if-not (result/ok? content-result)
            content-result
            (player/split-er7-multi (:payload content-result))))))))

(defn- play-events-from-edn-file
  "ADR-0100: the sim's own event log -- `ehrt sim run --format
  ground-truth`'s own bare stdout, or `generate sim`'s own events.edn,
  byte-identical (ADR-0100's own byte-equality claim). Recognized by
  its own `.edn` extension in play's dispatch, never the shared
  sniff-path-format (a channel-inferred ruling verified against that
  helper's own caller set: widening the SHARED sniff would change
  gate/show's own behavior on .edn files, which no ruling covers).
  Guarded EDN read (result/error :play-input-unreadable on either an
  IO failure or malformed EDN -- one try wraps both, the same shape
  gate-command's own --baseline read already uses), then a shape check
  on the parsed value: a non-empty vector of maps, the first of which
  carries both :event and :t (the real ground-truth event shape,
  ehrt.patient-simulator.compile-trajectory -- NOT :type, a channel claim
  this session's own live probe corrected, disclosed in ADR-0100).
  Anything else is :play-input-unsupported, the same category play's
  own message-input legs already use for an unsupported shape."
  [f]
  (let [content-result (slurp-play-input f)]
    (if-not (result/ok? content-result)
      content-result
      (let [parse-result (try
                            (result/ok (edn/read-string (:payload content-result)))
                            (catch Exception e
                              (result/error :play-input-unreadable
                                            {:path (.getPath ^java.io.File f) :message (.getMessage e)})))]
        (if-not (result/ok? parse-result)
          parse-result
          (let [parsed (:payload parse-result)]
            (if (and (vector? parsed) (seq parsed) (map? (first parsed))
                     (contains? (first parsed) :event) (contains? (first parsed) :t))
              (result/ok parsed)
              (result/error :play-input-unsupported
                            {:path (.getPath ^java.io.File f)
                             :hint "ehrt play only supports a sim event log shaped as a vector of ground-truth event maps carrying :event and :t -- run: ehrt sim run --format ground-truth, or ehrt corpus generate sim (ADR-0100)"}))))))))

(defn- play-events-from-dir
  "ADR-0015: a directory of files sharing the sniffed v2 format,
  concatenated in lexical filename order -- the exact candidate set
  `gate-dir`/`show`'s own directory dispatch already use
  (gate-candidate-files-in, already name-sorted), and exactly the
  order the sim generator's own width-padded msg-NNN emission produces by
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
  (let [files-result (gate-candidate-files-in path)]
    (if-not (result/ok? files-result)
      files-result
      (let [files (:payload files-result)]
        (if (empty? files)
          (result/error :play-input-unsupported
                        {:path path :reason :no-candidate-files
                         :hint "ehrt play found no HL7 v2 (ER7) or FHIR JSON candidate files in this directory"})
          (let [sniffed-result (sniff-files files)]
            (if-not (result/ok? sniffed-result)
              sniffed-result
              (let [sniffed (:payload sniffed-result)
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
                  (let [per-file (map (fn [file]
                                        (let [content-result (slurp-play-input file)]
                                          (if-not (result/ok? content-result)
                                            content-result
                                            (player/split-er7-multi (:payload content-result)))))
                                      files)
                        failed (first (remove result/ok? per-file))]
                    (if failed
                      failed
                      (result/ok (vec (mapcat :payload per-file))))))))))))))

(defn play-command
  "`ehrt play PATH [--rate R] [--idle-cap SECONDS] [--ticker full|line]
  [--board N] [--sink DESIGNATOR]` (ADR-0014, directories per ADR-0015,
  a sim event log per ADR-0100): paces PATH's own events -- HL7 v2
  (ER7) messages against their MSH-7 timestamps, or a sim event log's
  own ground-truth events against their :t (seconds from the run's own
  epoch, sim/ADR-0011) -- and renders them through a ticker (the
  default -- full ER7 blocks via `render-er7-message`, or one compact
  `--ticker line` per message for message input; ONE compact event
  line per event for event input, both --ticker modes alike, since
  there is no wire-format \"full\" rendering for a compiled event),
  renders a bed-board snapshot every N stream-minutes via `--board N`
  (message input only, display-only, ADR-0067 -- wins over --ticker,
  ignored when --sink is given; :play-board-unsupported-for-events on
  event input, ADR-0100 -- the board's fold is wire-side and an event
  log doesn't carry the emission parameters needed to synthesize ADT
  traffic), or writes them, byte-identically to an unpaced batch write,
  through a `--sink` designator (message input only;
  :play-sink-unsupported-for-events on event input, ADR-0100 -- an
  event log has no wire framing to write). `ehrt play PATH` at an
  arbitrarily large --rate, ticker sink, is exactly `ehrt show PATH`
  for message input (ADR-0013/ADR-0014's own identity) -- ordinary
  division makes this true with no special-cased rate value.

  PATH is a single HL7 v2 (ER7) file, a directory of files sharing the
  sniffed v2 format (ADR-0015 -- concatenated in lexical filename
  order, see `play-events-from-dir`'s own docstring for the order
  contract; a `.edn` sim event log sitting in that same directory is
  ignored -- directory input stays message-only, ADR-0100), or a
  single `.edn` file recognized as a sim event log (ADR-0100 --
  `ehrt sim run --format ground-truth`'s own bare stdout, or `generate
  sim`'s own events.edn, byte-identical; see
  `play-events-from-edn-file`'s own docstring for the shape check). A
  FHIR JSON path, or a FHIR/mixed/unclassifiable directory, is
  :play-input-unsupported (a named, disclosed deferral, ADR-0014).

  :sleep-fn is injectable (defaults to real-sleep!, Thread/sleep) so
  hermetic tests never actually wait; :println-fn defaults to println.
  Returns the standard Result envelope (events emitted, stream-time
  span, wallclock elapsed, the resolved rate/idle-cap, and every count
  player/plan itself computed, plus :snapshot-count/:unfolded-count
  when board mode ran) -- rendered through the ordinary
  TTY/--pretty/--edn/--json machinery, exactly like every other
  command; ADR-0014 does not add a second output convention."
  [{:keys [path rate idle-cap ticker sink board sleep-fn println-fn now-ms-fn]
    :or {sleep-fn real-sleep! println-fn println now-ms-fn #(System/currentTimeMillis)}}]
  (let [rate-result (validate-positive :invalid-rate rate)]
    (if-not (result/ok? rate-result)
      rate-result
      (let [idle-cap-result (validate-positive :invalid-idle-cap idle-cap)]
        (if-not (result/ok? idle-cap-result)
          idle-cap-result
          (let [board-result (validate-positive :invalid-board board)]
            (if-not (result/ok? board-result)
              board-result
              (let [resolved-rate (or (:payload rate-result) player/default-rate)
                resolved-idle-cap-ms (if-let [s (:payload idle-cap-result)] (long (* 1000 s)) player/default-idle-cap-ms)
                resolved-board (:payload board-result)
                f (io/file path)]
            (cond
              (not (.exists f))
              (result/error :gate-path-not-found
                             {:path path :hint "no such file or directory -- run: ehrt help play"})

              :else
              (let [event-input? (and (not (.isDirectory f)) (str/ends-with? (.getName f) ".edn"))
                    events-result (cond
                                    (.isDirectory f) (play-events-from-dir path)
                                    event-input? (play-events-from-edn-file f)
                                    :else (play-events-from-file f))]
                (if-not (result/ok? events-result)
                  events-result
                  (let [events (:payload events-result)
                        timestamp-fn (if event-input? player/event-timestamp-ms player/message-timestamp-ms)
                        plan-result (player/plan events {:rate resolved-rate :idle-cap-ms resolved-idle-cap-ms
                                                          :timestamp-fn timestamp-fn})
                        started-ms (now-ms-fn)
                        sink-result
                        (cond
                          (and event-input? sink)
                          (result/error :play-sink-unsupported-for-events
                                        {:hint "ehrt play --sink only supports HL7 v2 message input -- a sim event log has no wire framing to write; pipe it through ehrt sim check instead (ADR-0100)"})

                          (and event-input? resolved-board)
                          (result/error :play-board-unsupported-for-events
                                        {:hint "ehrt play --board only supports HL7 v2 message input -- the board's fold is wire-side and a sim event log doesn't carry the emission parameters (site/facility/providers) needed to synthesize ADT traffic (ADR-0014/ADR-0100)"})

                          sink (resolve-play-sink sink println-fn)
                          resolved-board (result/ok (assoc (board-sink resolved-board println-fn) :close-fn (fn [])))
                          :else (result/ok {:sink-fn (if event-input?
                                                        (event-line-sink println-fn)
                                                        (case ticker
                                                          "line" (ticker-line-sink println-fn)
                                                          (ticker-full-sink println-fn)))
                                            :close-fn (fn [])
                                            :cue-fn (ticker-cue-fn println-fn)}))]
                    (if-not (result/ok? sink-result)
                      sink-result
                      (let [{:keys [sink-fn close-fn cue-fn finalize-fn snapshot-count-fn unfolded-count-fn
                                    failure-fn summary-fn]} (:payload sink-result)
                            run-result (run-plan! plan-result sleep-fn cue-fn sink-fn)
                            _ (when finalize-fn (finalize-fn))
                            _ (close-fn)
                            ended-ms (now-ms-fn)
                            first-ts (some timestamp-fn events)
                            last-ts (some timestamp-fn (reverse events))
                            board-counts (when finalize-fn {:snapshot-count (snapshot-count-fn)
                                                             :unfolded-count (unfolded-count-fn)})
                            ;; ARC 4 SWEEP 5 (ADR-0175 design (g)): the
                            ;; MLLP leg is the first sink that can FAIL
                            ;; after the run has started -- a negative
                            ;; acknowledgement, a missing ACK, a closed
                            ;; stream. Its error is returned INSTEAD of
                            ;; a success, so `ehrt play --sink mllp://`
                            ;; exits non-zero on a receiver that refused
                            ;; or never answered rather than printing a
                            ;; cheerful summary of an undelivered run.
                            sink-failure (when failure-fn (failure-fn))]
                        (if sink-failure
                          sink-failure
                          (result/ok (merge run-result
                                            board-counts
                                            ;; COUNTS ONLY at the CLI. The
                                            ;; component's own `summary-fn`
                                            ;; carries every pair, which is
                                            ;; what the gates read; a
                                            ;; command that printed 273 of
                                            ;; them would bury its own
                                            ;; result.
                                            (when summary-fn {:mllp (select-keys (summary-fn) [:sent :acked])})
                                            {:rate resolved-rate
                                             :idle-cap-ms resolved-idle-cap-ms
                                             :wallclock-ms (- ended-ms started-ms)
                                             :stream-span-ms (when (and first-ts last-ts) (- last-ts first-ts))
                                             :sink (or sink "ticker")})))))))))))))))))

(defn- parse-canonicalizer-steps
  "\"id@v,id2@v2\" -> [[:id \"v\"] [:id2 \"v2\"]] -- the ordered
  [id version] pairs ehrt.kernel.interface/apply-canonicalizers
  expects. Blank/nil -> []."
  [s]
  (if (str/blank? s)
    []
    (mapv (fn [pair]
            (let [[id version] (str/split pair #"@" 2)]
              [(keyword id) version]))
          (str/split s #","))))

(defn- check-target-error
  "F1 (R3-B2-1, ADR-0117, HIGHEST PRIORITY): `check` used to report a
  clean, all-zero pass on a missing arg, a nonexistent path, or a
  genuinely empty directory -- indistinguishable from a real
  zero-finding pass over real files (confirmed live before this fix:
  all three returned {:status :ok :payload {:totals {:pass 0 ...}}} at
  exit 0). DIR is now required and must name an existing, non-empty
  directory -- validated here, at the CLI boundary only; judge/check's
  own check-corpus is untouched. nil when path passes; a categorized
  Result (exit 2) otherwise."
  [path]
  (cond
    (nil? path)
    (result/error :missing-required-opt
                  {:opt :path :hint "DIR is required -- run: ehrt check DIR"})

    (not (.exists (io/file path)))
    (result/error :invalid-target
                  {:path path :reason :not-found
                   :hint "no such file or directory -- run: ehrt check EXISTING-DIR"})

    (empty? (filter #(.isFile ^java.io.File %) (file-seq (io/file path))))
    (result/error :invalid-target
                  {:path path :reason :empty
                   :hint "this directory contains no files to check -- point ehrt check at a populated directory"})

    :else nil))

(defn check-command
  "`ehrt check DIR --expected DIR --assertions FILE
  [--canonicalizers id@v,...] [--pair-by path|hash] [--report ...]`.
  :assertions names an EDN file holding a vector of assertion maps
  (see check-corpus's own docstring for the shape) -- read here, the CLI's own
  impure boundary; omitted entirely (with :expected given) delegates
  straight to check/check-corpus's own default
  ([{:kind :matches-expected}]). :canonicalizers is a comma-separated
  \"id@version\" list; :pair-by is \"path\" (default) or \"hash\".
  :report goes through `write-report!` on the same terms as the gate's:
  parents created, a residual IO failure returned as
  :report-write-failed instead of the check's own verdict.

  F1 (R3-B2-1, ADR-0117): DIR is validated first (`check-target-error`)
  -- required, must exist, must be non-empty -- before any assertions
  file is even read."
  [{:keys [path expected assertions canonicalizers pair-by report]}]
  (or
   (check-target-error path)
   (let [assertions-result
         (if assertions
           (try
             (result/ok (edn/read-string (slurp assertions)))
             (catch Exception e
               (result/error :assertions-unreadable
                              {:path assertions :message (.getMessage e)})))
           (result/ok nil))]
     (if-not (result/ok? assertions-result)
       assertions-result
       (let [assertions-data (:payload assertions-result)
             opts (cond-> {:candidate-dir path}
                    expected (assoc :expected-dir expected)
                    assertions-data (assoc :assertions assertions-data)
                    canonicalizers (assoc :canonicalizers (parse-canonicalizer-steps canonicalizers))
                    pair-by (assoc :pair-by (keyword pair-by)))
             r (check/check-corpus opts)
             write-error (when report (write-report! report (:payload r)))]
         (or write-error r))))))

(defn- help-text-for
  "Group usage text for a known group name, top-level usage text
  otherwise (nil group, or a name that isn't a real group -- e.g. a
  bare `ehrt`, or `ehrt help bogus`). `width` (AR-EP-3, ux epilogue,
  `notes/adr/0065-ux-epilogue.md`) defaults to help/default-wrap-width
  for callers that don't care -- every real dispatch call site below
  passes its own resolved width."
  ([group] (help-text-for group help/default-wrap-width))
  ([group width]
   (or (and group (help/render-group help/cli-spec group width))
       (help/render-top-level help/cli-spec width))))

(defn- help-response
  "An explicit help request (`ehrt help`, `ehrt help <group>`, `--help`
  anywhere): result/ok (exit 0) carrying the plain-text usage under
  :payload's :text, marked :category :cli-help so main! prints it
  verbatim instead of through `render`."
  ([group] (help-response group help/default-wrap-width))
  ([group width]
   (assoc (result/ok {:text (help-text-for group width)}) :category :cli-help)))

;; B1 (R3-B3-2, ADR-0118): `ehrt help <group> <verb>` and `ehrt <group>
;; <verb> --help` narrow to just that verb's own usage text -- every
;; invocation form used to fall back to the whole group's page
;; regardless of how specifically a caller asked. `group-takes-verbs?`
;; scopes this entirely to groups that HAVE verbs (artifact/corpus/
;; gate/sim) -- a group with none (check/version/doctor/show/play)
;; takes its own second positional as a path candidate, never a verb
;; selector, and stays exactly as unaffected as it always was.

(defn- group-takes-verbs?
  [group-name]
  (boolean (:verbs (help/find-group help/cli-spec group-name))))

(defn- verb-known?
  [group-name verb-name]
  (when-let [g (help/find-group help/cli-spec group-name)]
    (when (:verbs g)
      (contains? (set (help/verb-names g)) verb-name))))

(defn- verb-help-response
  "A narrowed verb-level help render -- callers only reach this once
  `verb-known?` has already confirmed the [group verb] pair resolves."
  [group verb width]
  (assoc (result/ok {:text (help/render-verb-help help/cli-spec group verb width)}) :category :cli-help))

(defn- bare-invocation-response
  "Bare `ehrt` (no group at all): prints the same top-level usage text
  as `ehrt help`, and now exits the same way too (B-5, ux fixes 2,
  `notes/adr/0060-ux-fixes-2.md`, author-ruled 2026-08-06): matching
  the `--help`-exits-0 convention documented for --help itself --
  asking for help by omission is still asking for help, not an
  operational error."
  ([] (bare-invocation-response help/default-wrap-width))
  ([width]
   (assoc (result/ok {:text (help-text-for nil width)}) :category :cli-help)))

(defn- resolved-help-width
  "AR-EP-3: resolves the effective wrap width for a help render.
  {:width n} in the common case; {:width-error result} when an
  explicit --width was given and didn't parse to an integer >=
  help/min-wrap-width (rejected by name, an operational error) --
  COLUMNS itself never produces an error, per help/resolve-width's own
  silent-fallback contract, so there is no :width-error arm for it.
  `columns-env-fn` is injectable (default reads the real COLUMNS env
  var) -- same shape as `main!`'s own :println-fn/:exit-fn/:tty?-fn,
  for the same reason: a real env var can't be set for a running JVM,
  so testing the COLUMNS arm at all needs a seam here."
  [opts columns-env-fn]
  (if-let [raw (:width opts)]
    (let [{:keys [width error]} (help/parse-width-flag raw)]
      (if width
        {:width width}
        {:width-error (result/error :invalid-width (merge {:flag "--width"} error))}))
    {:width (help/resolve-width {:columns-env (columns-env-fn)})}))

(defn- verb-name-groups
  "Every group name in `spec` whose own verbs include `verb-name` --
  used by `unknown-command-error` to catch a near-miss that crosses a
  GROUP boundary (a bare top-level verb like `run`, not a group name
  at all) rather than the within-group near-miss `ehrt sim` (missing
  verb) already handles. `verb-name` nil or matching no group's own
  verbs returns empty, same as no near miss existing."
  [spec verb-name]
  (->> (:groups spec)
       (filter #(contains? (set (help/verb-names %)) verb-name))
       (map :group)))

(defn- unknown-command-error
  "An unrecognized group or verb: :unknown-command, extended (DOC-1's
  bounded error-message pass) with :valid-options (drawn from the help
  spec -- one source of truth with `ehrt help`'s own text, so the two
  can't drift apart) and a :hint pointing at the fuller help surface --
  `run: ehrt help <group>` when the FIRST arg token is itself a real
  group name (B-6/D-3, ux fixes 2, `notes/adr/0060-ux-fixes-2.md`:
  `ehrt sim` with no verb knows it means the `sim` group, so its own
  hint should say so, not point at the generic top-level listing); the
  generic `run: ehrt help` for a genuinely-unrecognized token.

  AR-EP-2 (ux epilogue, `notes/adr/0065-ux-epilogue.md`): a token that
  is itself a VERB name in exactly one group (`run`, sim's own -- not a
  group name, so the check above never catches it) gets the same
  treatment, plus a `:did-you-mean \"<group> <verb>\"` payload key the
  group-name case never carries. A token matching a verb in more than
  one group, or matching none, keeps the prior generic behavior --
  ambiguous or genuinely unrecognized, nothing to point at."
  [args valid-options]
  (let [token (first args)
        group-names (set (help/group-names help/cli-spec))
        verb-owning-groups (when-not (contains? group-names token)
                              (verb-name-groups help/cli-spec token))
        hint (cond
               (contains? group-names token) (str "run: ehrt help " token)
               (= 1 (count verb-owning-groups)) (str "run: ehrt help " (first verb-owning-groups))
               :else "run: ehrt help")]
    (result/error :unknown-command
                  (cond-> {:args args :valid-options valid-options :hint hint}
                    (= 1 (count verb-owning-groups))
                    (assoc :did-you-mean (str (first verb-owning-groups) " " token))))))

(defn- sim-er7-requires-emit-hl7?
  [format opts]
  (and (= "er7" format) (not= "hl7" (:emit opts))))

(defn- sim-er7-bare-text
  "Bare wire bytes: every rendered message, joined by one blank line,
  nothing else -- the property bases/sim-cli's own `--format er7`
  always promised."
  [r]
  (str/join "\n\n" (get-in r [:payload :messages])))

(defn- sim-ground-truth-bare-text
  "Bare EDN: the run's own :ground-truth vector, pr-str'd, nothing
  else -- readable straight back by `edn/read`, exactly the shape
  `sim-check-command` requires on stdin. Makes `ehrt sim run --format
  ground-truth | ehrt sim check` an actual working pipe, same property
  bases/sim-cli's own run-then-check-cli-pipe-round-trips test proved
  for the standalone binary."
  [r]
  (pr-str (get-in r [:payload :ground-truth])))

(defn sim-run-command
  "`ehrt sim run`: mounts ehrt.sim.interface/run-command in-process
  (ADR-0005, 2026-07-28 -- ADR-0012's own long-deferred \"ehrt sim
  mount\", fulfilled once sim and tools shared one workspace/classpath).
  No translation layer: this CLI's own flag names already match sim's
  own opts 1:1 (:seed, :patients, :reference-date, :emit, :churn,
  :config, :warm-up-seconds, :arrival-gap, :at, :utc-offset -- see
  ehrt.sim.interface/run-command's own docstring for the full set); the
  -fn injection point below (:sim-run-fn) is what keeps this repo's own
  CLI tests hermetic, per ADR-0012 property 5's own commitment.

  P3-6 parity mount (2026-08-01): :format \"er7\"/\"ground-truth\" mount
  bases/sim-cli's own two bare-stdout rendering modes -- :format
  \"json\" needs no separate mount, since it's already exactly what
  this CLI's own --json does (the full envelope, JSON instead of EDN);
  :format \"edn\" is already the default. An :ok result under a bare
  format gets its bare content attached as `:bare-text` metadata (never
  :payload, so the EDN/JSON envelope is unaffected) -- `main!` prints
  that verbatim when present, same precedence as its :cli-help/
  :display-text special cases, but WITHOUT forcing exit 0: unlike
  `show-command`'s :category :display-text (always ok, ADR-0013), a
  bare-format run keeps the real result->exit-code contract, since
  bases/sim-cli's own format-er7-on-a-failing-run-shows-stderr-edn-and-
  exit-2 test requires a failing run to still exit non-zero under a
  bare format. Simplification, disclosed rather than silent: a non-:ok
  result under a bare format renders through the normal stdout path
  (EDN or --pretty) instead of bases/sim-cli's own stderr-only
  discipline for that case -- still visible, still the right exit code,
  just not stream-split; `ehrt sim check` reading a non-vector off a
  failed pipe already reports :malformed-input rather than
  misbehaving, so the pipe fails loudly either way."
  [opts]
  (let [format (:format opts)]
    (if (sim-er7-requires-emit-hl7? format opts)
      (result/rejected :format-er7-requires-emit-hl7
                        {:message "--format er7 renders bare wire messages and requires --emit hl7"
                         :format format :emit (:emit opts)})
      (let [r (sim/sim-run! (dissoc opts :format))]
        (cond
          (and (= format "er7") (result/ok? r)) (vary-meta r assoc :bare-text (sim-er7-bare-text r))
          (and (= format "ground-truth") (result/ok? r)) (vary-meta r assoc :bare-text (sim-ground-truth-bare-text r))
          :else r)))))

(defn sim-check-command
  "`ehrt sim check`: mounts sim's own invariant catalog
  (ehrt.sim.interface/check-all, via the sim adapter's `check!` --
  same dependency-direction invariant sim-run-command already relies
  on: corpus requires sim, never the reverse) over a ground-truth EDN
  vector read from stdin. Same stdin contract bases/sim-cli's own
  check-command always used (ported directly, same three named
  rejections), and the SAME 1-arg check-all arity (default facility/
  warm-up/order-profiles) it always called.

  P3-6 parity mount (2026-08-01): bases/sim-cli's `check` verb had no
  `ehrt` equivalent before this -- found during the sim-cli retirement
  review's own parity check (notes/facts-register.md F2), escalated,
  and wired before that retirement could proceed."
  [_opts]
  (let [log (try (edn/read {:eof ::eof} (java.io.PushbackReader. *in*))
                 (catch Exception _e ::unreadable))]
    (cond
      (= ::eof log) (result/rejected :empty-input {:message "expected a ground-truth EDN vector on stdin"})
      (= ::unreadable log) (result/rejected :unreadable-input {:message "stdin was not readable EDN"})
      (not (vector? log)) (result/rejected :malformed-input {:message "expected a vector of event maps"})
      :else (sim/sim-check! log))))

(defn sim-identifiers-command
  "`ehrt sim identifiers`: mounts ehrt.sim.interface/identifiers-command
  (via the sim adapter's `identifiers!`, same dependency-direction
  invariant as sim-run-command/sim-check-command) -- the SAME config
  surface `ehrt sim run` accepts (:seed, :patients, :config; see
  identifiers-command's own docstring for the full inventory it
  returns).

  P3-6 parity mount (2026-08-01): docs/simulate-your-facility.md
  already teaches this capability as the standalone `sim identifiers`
  binary invocation (the privacy/removal FAQ's own answer) -- this is
  the SAME capability, reachable through `ehrt` instead; the doc was
  updated to the new spelling in the same session."
  [opts]
  (sim/sim-identifiers! opts))

(defn sim-version-command
  "`ehrt sim version`: mounts ehrt.sim.interface/version + git-sha (via
  the sim adapter's `version!`, same dependency-direction invariant as
  every other sim-* command here) -- sim's own library version marker,
  the SAME source the run manifest's :generator block stamps; distinct
  from this repo's own `ehrt version` (repo identity + pinned
  artifacts).

  P3-6 parity mount (2026-08-01): bases/sim-cli's `version` verb had no
  `ehrt` equivalent before this."
  [_opts]
  (result/ok (sim/sim-version!)))

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
;; provenance, notes/tools/ehr-testing-sim-mounting-note.md) before
;; changing any of these.

(defn- resolve-path-designators
  "CLI acceptance is additive (ruling 7, docs/source-sink-design.md
  Part IX via SS-1 Step 6): wherever a positional PATH names an input,
  or --out-dir/--out/--scratch-dir names an output, a dir:/file: URL
  designator is now also accepted alongside the documented bare-path
  spelling -- parsed to the same path a bare spelling would have given
  (source-sink-url/path-designator->path), never the other way
  around. Applied once here, in dispatch, so every downstream command
  function keeps working with plain path strings exactly as before --
  this is CLI-shell-boundary sugar, not a new capability those
  functions need to know about. A key absent from opts is left absent
  (most verbs use only one or two of these four). :scratch-dir added
  F7 (ADR-0117), gate fhir's own renamed --out-dir -- preserves the
  designator-acceptance behavior under the new flag name; a valid-input
  behavior change is exactly what the fence forbids."
  [opts]
  (reduce (fn [opts k]
            (if (contains? opts k)
              (update opts k source-sink-url/path-designator->path)
              opts))
          opts
          [:path :out-dir :out :scratch-dir]))

;; ---- unknown flags (ux fixes 3, AR-U3-1/2/3/4, `notes/adr/0061-ux-
;; fixes-3.md`): C-4's founding-adjacent defect -- an unrecognized flag
;; used to be silently absorbed into :opts and echoed back in the
;; manifest as if intended. Every flag token now has to be declared for
;; its verb, or the run is rejected by name before any capability
;; function ever sees it. ----

(def ^:private gate-explicit-verbs
  "The only verb names `ehrt gate` itself resolves to a real spec entry
  for -- D11's bare, verb-less `ehrt gate PATH` is a fourth, unlisted
  shape (sniff-dispatched into v2 or fhir, never v2-nist)."
  #{"v2" "fhir" "v2-nist"})

(defn- flag-key
  "\"--out-dir\" -> :out-dir, babashka.cli's own key-munging (the
  leading \"--\" dropped, the rest keywordized unchanged) -- see
  `parse`/cli-spec above; never inverted."
  [flag-str]
  (keyword (subs flag-str 2)))

(defn- declared-flag-keywords
  "AR-U3-1: the flag universe derives straight from `help/cli-spec` --
  no hand-maintained duplicate list. The spec's own :global-flags
  (--json/--pretty/--edn/--help) plus a group's own :flags (a group
  with no :verbs -- check/version/doctor/show/play) or one named verb's
  own :flags (a group with :verbs -- artifact/corpus/gate/sim). nil
  when the group has :verbs but names no verb entry matching `verb`
  (the caller's cue to skip validation, not to validate against an
  empty set)."
  [group-spec verb]
  (when-let [flags (if (:verbs group-spec)
                      (:flags (first (filter #(= verb (:verb %)) (:verbs group-spec))))
                      (:flags group-spec))]
    (into (set (map (comp flag-key :flag) help/global-flags))
          (map (comp flag-key :flag))
          flags)))

(defn- flag-validation-context
  "The [valid-flag-keywords verb-label] pair `validate-known-flags`
  checks `opts`'s own keys against, for one [group action] pair -- nil
  when there's nothing to validate against yet, because dispatch's own
  `case group` is about to produce its own :unknown-command error for
  this same [group action] a moment later (an unrecognized group, or a
  group that requires a verb and didn't get a recognized one) -- flag
  validation steps aside rather than piling a second, more confusing
  error on top of that one.

  \"gate\" is the one named exception (D11, gate-explicit-verbs above):
  its bare, verb-less invocation (`ehrt gate PATH`, action carrying the
  path candidate, not a verb) sniff-dispatches into EITHER gate v2 or
  gate fhir -- never gate v2-nist, which has no default profile to
  sniff into -- so its own valid-flags target, absent an explicit
  v2/fhir/v2-nist token, is the union of v2's and fhir's own declared
  flags: the full reachable set before sniffing decides which one
  actually runs. (Every v2-nist-only flag, --profile, is therefore not
  independently checked against a bare `ehrt gate PATH` -- disclosed,
  not an oversight: bare gate already never routes to v2-nist by
  construction, D11, so a --profile alongside it is inert either way.)"
  [group action]
  (when-let [g (help/find-group help/cli-spec group)]
    (cond
      (= group "gate")
      (if (contains? gate-explicit-verbs action)
        [(declared-flag-keywords g action) (str group " " action)]
        [(into (declared-flag-keywords g "v2") (declared-flag-keywords g "fhir")) group])

      (:verbs g)
      (when (contains? (set (help/verb-names g)) action)
        [(declared-flag-keywords g action) (str group " " action)])

      :else
      [(declared-flag-keywords g nil) group])))

(defn- levenshtein-distance
  "Small, iterative edit-distance implementation -- `ehrt.kernel` has no
  distance/similarity helper (checked, confirmed absent this session,
  same discipline U4's own sibling-config check applied, ADR-0060) --
  scoped locally to `nearest-declared-flag`'s own suggestion, not a
  reusable API."
  [a b]
  (let [a (vec a) b (vec b)
        m (count a) n (count b)
        next-row (fn [prev i]
                   (reduce (fn [row j]
                             (conj row
                                   (if (= (a (dec i)) (b (dec j)))
                                     (nth prev (dec j))
                                     (inc (min (nth prev j) (nth prev (dec j)) (peek row))))))
                           [i]
                           (range 1 (inc n))))]
    (loop [prev (vec (range (inc n))) i 1]
      (if (> i m)
        (peek prev)
        (recur (next-row prev i) (inc i))))))

(defn- nearest-declared-flag
  "AR-U3-2: the nearest declared flag name within Levenshtein distance
  2 of `unknown-name`, ties broken alphabetically; nil when nothing is
  that close (no :did-you-mean key at all, never a present-but-nil
  one -- see `unknown-flag-error`)."
  [unknown-name candidate-names]
  (let [within (->> candidate-names
                     (map (fn [c] [(levenshtein-distance unknown-name c) c]))
                     (filter #(<= (first %) 2)))]
    (when (seq within)
      (second (first (sort-by (juxt first second) within))))))

(defn- unknown-flag-error
  [flag-kw verb-label valid-keywords]
  (let [flag-name (name flag-kw)
        suggestion (nearest-declared-flag flag-name (map name valid-keywords))]
    (result/error :unknown-flag
                  (cond-> {:flag (str "--" flag-name) :verb verb-label}
                    suggestion (assoc :did-you-mean (str "--" suggestion))))))

(defn- validate-known-flags
  "AR-U3-2: at the point dispatch has resolved [group action] to a real
  verb (or a no-verb group, or gate's own D11 bare-sniff target) --
  every key `parse` put in `opts` (the parser's OWN flag-position
  tokens, per babashka.cli's own arity-aware consumption; AR-U3-3: never
  re-derived here, so a flag value that happens to start with \"-\" --
  a negative --at/--arrival-gap, a --utc-offset offset -- is never at
  risk of being reclassified, since it was never a key in `opts` to
  begin with) must belong to that target's own valid set. Returns the
  first unknown one as a :category :unknown-flag error (\"parsing stops
  at the first unknown flag -- one clear error beats a cascade\"), or
  nil when every key is accounted for (including when there's nothing
  to validate against yet, per `flag-validation-context`)."
  [group action opts]
  (when-let [[valid-keywords verb-label] (flag-validation-context group action)]
    (when-let [unknown-kw (first (remove valid-keywords (keys opts)))]
      (unknown-flag-error unknown-kw verb-label valid-keywords))))

(defn- validate-top-level-flags
  "D8-4 (ADR-0098): dispatch's bare (`(nil? group)`) and `help`-verb
  (`(= group \"help\")`) short-circuits run BEFORE `validate-known-
  flags` (which lives in the `:else` branch's own `(or ...)`), so a
  typo'd flag at this level used to be silently absorbed into a help
  render instead of rejected by name -- unlike the identical typo one
  level down, at a real [group verb]. `help/global-flags` is the
  complete declared surface at this level (`--json`/`--pretty`/`--edn`/
  `--help`/`--width` -- confirmed by reading `help.clj` directly, not
  assumed); every key `parse` put in opts must belong to it. Returns
  the same :category :unknown-flag/did-you-mean shape
  `unknown-flag-error` builds for a real subcommand, or nil when every
  key is accounted for. `(:help opts)` itself (`--help` given anywhere,
  including alongside a real group/verb) is a SEPARATE dispatch branch,
  untouched by this rider -- deliberately out of D8-4's own named scope
  (\"bare/`help`-level\"), and `ehrt --help` alone must keep working."
  [verb-label opts]
  (let [valid-keywords (set (map (comp flag-key :flag) help/global-flags))]
    (when-let [unknown-kw (first (remove valid-keywords (keys opts)))]
      (unknown-flag-error unknown-kw verb-label valid-keywords))))

;; ---- F5 (R3-B1-3, ADR-0117): `corpus generate`'s unknown-flag check
;; (validate-known-flags above) only asks "is this flag declared
;; anywhere in the verb's own spec" -- it never asked "is this flag
;; applicable to the SOURCE actually selected." A synthea:-scoped flag
;; given while generating sim (or vice versa) used to pass silently,
;; with zero effect -- a narrower, still-live descendant of the same
;; silent-misconfiguration class C-4 fixed for genuinely unknown
;; flags. Reject, not warn (ruled, F5's reject-not-warn [C]). ----

(def ^:private generate-flag-scope
  "Flag keyword -> :sim or :synthea, derived from cli-spec's own
  doc-string prefix convention (\"sim: \"/\"synthea: \") -- one source
  of truth, the same discipline `declared-flag-keywords` already
  applies (AR-U3-1). A flag with no such prefix (--seed,
  --reference-date, --out-dir) is shared and carries no scope entry."
  (into {}
        (keep (fn [{:keys [flag doc]}]
                (cond
                  (str/starts-with? doc "sim: ") [(flag-key flag) :sim]
                  (str/starts-with? doc "synthea: ") [(flag-key flag) :synthea])))
        (:flags (first (filter #(= "generate" (:verb %))
                               (:verbs (help/find-group help/cli-spec "corpus")))))))

(defn- source-scoped-flag-error
  [flag-kw scope selected-source]
  (result/error :flag-source-mismatch
                {:flag (str "--" (name flag-kw)) :flag-scope scope :selected-source selected-source
                 :hint (str "--" (name flag-kw) " only applies to corpus generate " (name scope)
                            " -- this run selected " (name selected-source))}))

(defn- validate-generate-source-scope
  "nil when every flag in opts is either shared or scoped to `source`;
  the first mismatched flag's own error otherwise (\"first\" mirrors
  validate-known-flags's own \"one clear error beats a cascade\"
  precedent)."
  [source opts]
  (let [other (if (= source :sim) :synthea :sim)]
    (when-let [[flag-kw scope] (first (filter (fn [[k v]] (and (= v other) (contains? opts k)))
                                              generate-flag-scope))]
      (source-scoped-flag-error flag-kw scope source))))

(defn dispatch
  "Routes [group action] positional args to the corresponding capability
  function with opts. The -fn keys are injectable (tests use this
  to avoid real subprocesses/network); default to the real commands.

  `ehrt help`, `ehrt help <group>`, and `--help` (given anywhere --
  `opts`'s :help true) short-circuit before any capability function
  runs, returning a :category :cli-help result instead of routing to a
  command -- see `help-response`/`bare-invocation-response` and the ns
  docstring's EDN-out exception. A bare or `help`-level unknown flag
  (D8-4, ADR-0098) is rejected by name (:category :unknown-flag,
  `validate-top-level-flags`) before either short-circuit renders help
  text -- `--help` itself is unaffected, only a typo'd sibling flag is."
  ([args opts] (dispatch args opts {}))
  ([args opts {:keys [fetch-fn fetch-all-fn resolve-fn generate-fn generate-sim-fn mutate-fn intake-fn operators-fn batch-fn
                       gate-v2-fn gate-fhir-fn gate-v2-nist-fn check-fn version-fn doctor-fn
                       sim-run-fn sim-check-fn sim-identifiers-fn sim-version-fn show-fn play-fn columns-env-fn]
               :or {columns-env-fn #(System/getenv "COLUMNS")
                    fetch-fn fetch-command
                    fetch-all-fn fetch-all-command
                    resolve-fn resolve-command
                    generate-fn generate/generate!
                    generate-sim-fn generate-sim-command
                    mutate-fn mutate-command
                    intake-fn intake-command
                    operators-fn operators-command
                    batch-fn batch-command
                    gate-v2-fn gate-v2-command
                    gate-fhir-fn fhir-gate-command
                    gate-v2-nist-fn v2-nist-gate-command
                    check-fn check-command
                    version-fn version-command
                    doctor-fn doctor-command
                    sim-run-fn sim-run-command
                    sim-check-fn sim-check-command
                    sim-identifiers-fn sim-identifiers-command
                    sim-version-fn sim-version-command
                    show-fn show-command
                    play-fn play-command}}]
   (let [[group action path] args]
     (cond
       (:help opts)
       (let [{:keys [width width-error]} (resolved-help-width opts columns-env-fn)]
         (or width-error
             ;; B1 (R3-B3-2, ADR-0118): `<group> <verb> --help` on a
             ;; group that takes verbs but not this one -- F6's own
             ;; unknown-command treatment, reused verbatim, naming the
             ;; group's real verbs.
             (when (and group action (group-takes-verbs? group) (not (verb-known? group action)))
               (unknown-command-error [group action] (help/verb-names (help/find-group help/cli-spec group))))
             (if (verb-known? group action)
               (verb-help-response group action width)
               (help-response group width))))

       (= group "help")
       ;; F6 (R3-B2-5 + R3-B3-3, ADR-0117): `ehrt help <unknown-group>`
       ;; used to silently fall back to the top-level usage screen,
       ;; exit 0 -- unlike `ehrt <unknown-group>` itself, which already
       ;; names the bad group at exit 2 (below, the fallthrough
       ;; unknown-command-error). Same treatment now, reusing the
       ;; category verbatim; bare `ehrt help` (action nil) is
       ;; unaffected.
       (or (validate-top-level-flags "help" opts)
           (when (and action (not (contains? (set (help/group-names help/cli-spec)) action)))
             (unknown-command-error [action] (help/group-names help/cli-spec)))
           ;; B1 (R3-B3-2, ADR-0118): the 3-arg form, `ehrt help
           ;; <group> <verb>` -- `path` is the verb token here (dispatch's
           ;; own [group action path] destructure, one level down from
           ;; the "help" group token itself). Same unknown-verb F6
           ;; treatment as the --help form above; a group with no verbs
           ;; at all ignores `path` exactly as it always did.
           (when (and action path (group-takes-verbs? action) (not (verb-known? action path)))
             (unknown-command-error [action path] (help/verb-names (help/find-group help/cli-spec action))))
           (let [{:keys [width width-error]} (resolved-help-width opts columns-env-fn)]
             (or width-error
                 (if (verb-known? action path)
                   (verb-help-response action path width)
                   (help-response action width)))))

       (nil? group)
       (or (validate-top-level-flags "ehrt" opts)
           (let [{:keys [width width-error]} (resolved-help-width opts columns-env-fn)]
             (or width-error (bare-invocation-response width))))

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
                    (and (= group "corpus") (#{"mutate" "intake" "batch"} action) path (not (:path opts))) (assoc opts :path path)
                    (and (= group "check") action (not (:path opts))) (assoc opts :path action)
                    (and (= group "show") action (not (:path opts))) (assoc opts :path action)
                    (and (= group "play") action (not (:path opts))) (assoc opts :path action)
                    :else opts)
             opts (resolve-path-designators opts)]
         (or
          (validate-known-flags group action opts)
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
                      ;; the subcommand name, not a filesystem path.
                      ;; ADR-0015 amendment (2026-07-30, cold-start
                      ;; ruling): bare `corpus generate` (path nil) now
                      ;; means sim, not synthea -- sim needs no fetched
                      ;; artifacts, so the cold first command succeeds
                      ;; unfetched; `generate synthea` remains the
                      ;; explicit, unchanged spelling for that lane.
                      "generate" (case (or path "sim")
                                   "synthea" (or (validate-generate-source-scope :synthea opts)
                                                 (with-generate-breadcrumb (generate-fn opts)))
                                   "sim" (or (validate-generate-source-scope :sim opts)
                                             (with-generate-breadcrumb (generate-sim-fn opts)))
                                   (unknown-command-error args ["synthea" "sim"]))
                      "mutate" (mutate-fn opts)
                      "intake" (intake-fn opts)
                      "operators" (operators-fn opts)
                      "batch" (batch-fn opts)
                      (unknown-command-error args (help/verb-names (help/find-group help/cli-spec "corpus"))))
           "gate" (cond
                    (= action "v2") (gate-v2-fn opts)
                    (= action "fhir") (gate-fhir-fn opts)
                    ;; ADR-0015: explicit verb only -- bare `ehrt gate
                    ;; PATH` sniffing (D11, below) never dispatches
                    ;; here, since sniffing has no --profile bundle to
                    ;; build a validator from.
                    (= action "v2-nist") (gate-v2-nist-fn opts)
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
                   "check" (sim-check-fn opts)
                   "identifiers" (sim-identifiers-fn opts)
                   "version" (sim-version-fn opts)
                   (unknown-command-error args (help/verb-names (help/find-group help/cli-spec "sim"))))
           "show" (show-fn opts)
           "play" (play-fn opts)
           (unknown-command-error args (help/group-names help/cli-spec)))))))))

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

(defn- doctor-checks-payload
  "The Checks shape doctor-command produces: {:checks [{:name :status
  :detail} ...]}. A grep of this workspace found :checks used nowhere
  else (2026-07-30, doctor-rendering session) -- shape-keyed dispatch
  stays consistent with report-payload's own precedent; no departure
  to category-keying was needed."
  [payload]
  (and (map? payload) (contains? payload :checks) payload))

(defn- pretty-doctor-check-line
  [{:keys [name status detail]}]
  (str (clojure.core/name status) "  " name " -- " detail))

(defn- pretty-doctor-summary
  "doctor's own tailored summary (2026-07-30 doctor-rendering session,
  ADR-0013's shape-dispatch sanction): one line per check -- status,
  name, and the check's own :detail in full, pass or fail alike (the
  detail already carries the remedy on a failing check, and the
  existing pass details are already short, so no truncation is needed
  either way -- a diagnostic that hid its own passing findings would
  invite doubt). A final line states the outcome and, when any check
  failed, the failing count -- worded as a checklist report, never
  \"rejected\": doctor succeeded at diagnosing, the checks are what
  failed."
  [checks]
  (let [failing (remove #(= :pass (:status %)) checks)]
    (str (str/join "\n" (map pretty-doctor-check-line checks))
         "\n\n"
         (if (empty? failing)
           "all checks passed"
           (str (count failing) " of " (count checks) " check(s) failed")))))

(defn- pretty-generic-summary
  "Every other envelope command's brief summary (ADR-0013): status,
  category, whatever key counts/paths the payload happens to carry,
  the payload's own :hint (ADR-0015: a rejection's remedy belongs in
  the human summary, not only in the EDN a human isn't reading -- the
  :out-dir-exists rejection is why this exists, but any payload
  carrying a :hint gets it, not a special case just for that one
  category), then a breadcrumb (ADR-0015) when `r` itself carries one
  as METADATA -- never in :payload, so it never touches the EDN/JSON
  envelope (pr-str/json/write-str both ignore metadata by
  construction; a command function that wants a breadcrumb attaches it
  to its own result via `vary-meta`, e.g. `mutate-command`'s directory-
  write path, or `with-generate-breadcrumb` below) -- plus a pointer at
  the full envelope. Never a prettified EDN envelope -- the envelope is
  the machine form, full stop."
  [r]
  (let [{:keys [status category payload]} r
        interesting (select-keys payload [:count :out-dir :path :cached :git :identity :item-count])]
    (str (name status)
         (when category (str " (" (name category) ")"))
         (when (seq interesting)
           (str "\n" (str/join "\n" (map pretty-kv-line interesting))))
         (when-let [hint (:hint payload)]
           (str "\n" hint))
         (when-let [breadcrumb (:breadcrumb (meta r))]
           (str "\n" breadcrumb))
         "\n(--edn or --json for the full result)")))

(defn render-pretty
  "Human-facing rendering for a Result envelope (ADR-0013). Dispatches
  on the payload's own shape, not on which command ran: a Report-shaped
  payload (gate, check) gets the tailored per-file summary; a Checks-
  shaped payload (doctor, 2026-07-30 doctor-rendering session) gets the
  tailored checklist summary; everything else -- including baseline
  mode's {:absolute :relative} payload -- gets the generic summary
  (this ruling's own named, permitted skip: tailoring beyond gate/
  check/doctor is not required). The machine contract (:status/
  :category/:payload) is never touched by any of this -- only which
  human-facing string gets built from it."
  [r report-path]
  (cond
    (report-payload (:payload r))
    (pretty-report-summary (report-payload (:payload r)) report-path)

    (doctor-checks-payload (:payload r))
    (pretty-doctor-summary (:checks (doctor-checks-payload (:payload r))))

    :else (pretty-generic-summary r)))

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
  one case -- stdout stays exactly, only, the framed bytes.

  P3-6 (2026-08-01): a result carrying :bare-text METADATA (`ehrt sim
  run --format er7|ground-truth`, sim-run-command) prints that text
  verbatim instead of going through render/render-pretty -- same
  stdout-is-reserved-for-bare-content property as :stdout-sink?, but
  metadata rather than :payload (so, unlike :stdout-sink?, it never
  touches the EDN/JSON envelope) and, unlike :cli-help/:display-text,
  does NOT force exit 0 -- `code` below is still computed from the
  real Result, since a failing bare-format run must still exit
  non-zero (bases/sim-cli's own contract, ported).

  F2 (R3-B2-2, ADR-0117): raw-args is parsed via `safe-parse`, not
  `parse` directly -- a babashka.cli coercion failure (a malformed
  --seed, say) is translated into a categorized :invalid-flag-value
  Result here rather than propagating as an uncaught ExceptionInfo;
  `dispatch-fn` is never called in that case."
  ([raw-args] (main! raw-args {}))
  ([raw-args {:keys [dispatch-fn println-fn exit-fn tty?-fn]
              :or {dispatch-fn dispatch println-fn println exit-fn #(System/exit %)
                   tty?-fn real-tty?}}]
   (let [{:keys [args opts parse-error]} (safe-parse raw-args)
         r (or parse-error (dispatch-fn args opts))
         code (result->exit-code r)
         text (cond
                (#{:cli-help :display-text} (:category r)) (:text (:payload r))
                (:bare-text (meta r)) (:bare-text (meta r))
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
