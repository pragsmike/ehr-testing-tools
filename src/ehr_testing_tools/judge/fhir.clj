(ns ehr-testing-tools.judge.fhir
  "FHIR conformance judge (P5): the official validator
  (`fhir-validator-cli`, `artifacts.lock.edn` kind `:engine`), run as a
  pinned subprocess against base FHIR R4 -- no implementation guide
  pinned in the lockfile this session; `-ig` wiring exists and is
  tested, ready for a caller to supply IG refs once one is pinned.
  Two-step engine discipline (pattern nursery #1): `execute` runs the
  subprocess and preserves its raw OperationOutcome JSON verbatim on
  disk alongside the run; `interpret` is a pure, versioned function
  from that raw JSON to canonical findings and a verdict.

  The verdict-mapping table below is DATA, versioned
  (`verdict-mapping-version`), and cited to the results file that
  produced it: `docs/experiments/EXP-C5-results.md`. EXP-C5's central
  finding, applied here rather than re-litigated: the official
  validator auto-validates against any implementation guide declared
  in an input's own `Resource.meta.profile` (Synthea R4 output
  declares US Core) even with no `-ig` flag given -- this is
  documented upstream behavior (`validator_cli.jar -help`: \"profiles
  declared in the resource... or specified on the command line\"), not
  something this judge suppresses. Consequence, observed directly:
  EXP-C5's own \"valid\" baseline corpus carries hundreds of
  profile-driven `error`/`structure` issues per file (an unrecognized
  Synthea extension against the US Core profile) alongside the small
  number of issues an actual defect operator introduces -- this judge
  targets whatever the validator actually checks given the input's own
  declared profile, honestly, not a sanitized base-spec-only view."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [ehr-testing-tools.artifact :as artifact]
            [ehr-testing-tools.digest :as digest]
            [ehr-testing-tools.invocation :as invocation]
            [ehr-testing-tools.judge.finding :as finding]
            [ehr-testing-tools.judge.verdict-cache :as verdict-cache]
            [ehr-testing-tools.result :as result])
  (:import [java.io File]))

(def engine-name "fhir-validator-cli")
(def default-fhir-version "4.0")

(def verdict-mapping-version "v2")
(def verdict-mapping-cited-to "docs/experiments/EXP-C5-results.md")

;; verdict-mapping-version bumped v1 -> v2 (ADR-0010, O2): the
;; EXP-C5-derived classification logic itself did not change (the same
;; five diagnostics-text patterns still mark terminology suppression),
;; but the mapping's OUTPUT vocabulary did -- the terminology-
;; suppressed case now emits :no-verdict/:terminology-suppressed
;; instead of :indeterminate. A consumer keying off this version string
;; (e.g. a stored baseline's provenance) sees the vocabulary change.

(def ^:private terminology-suppressed-patterns
  "Diagnostics-text substrings (case-insensitive) that mark an issue as
  terminology-suppressed offline -- EXP-C5 classified every distinct
  {severity, code} category the validator emitted against a real
  Synthea R4 corpus and its mutants, and these five phrases are what
  separated every genuinely terminology-degraded issue observed from
  every genuine base-spec/profile violation observed (none of the
  latter matched any of these). Versioned alongside
  `verdict-mapping-version`; a future EXP-C5 re-run that finds a new
  phrase bumps both together."
  ["without using server"
   "doesn't provide any codes"
   "in the absence of a terminology server"
   "without terminology services"
   "cannot be validated"])

(defn- terminology-suppressed?
  [text]
  (let [lower (str/lower-case (or text ""))]
    (boolean (some #(str/includes? lower %) terminology-suppressed-patterns))))

;; ---- execute: two-step engine, subprocess wrapper ----

(defn- resolve-java-bin
  [artifacts {:keys [resolve-and-extract find-executable]
              :or {resolve-and-extract artifact/resolve-and-extract
                   find-executable artifact/find-executable}}]
  ;; Mirrors corpus.generate/resolve-java-bin -- the same pinned
  ;; temurin-jdk :runtime artifact, resolved through the registry, is
  ;; what runs every JVM-based engine this repo wraps, never PATH.
  (let [extract-result (resolve-and-extract artifacts "temurin-jdk" "17.0.19+10" {})]
    (if-not (result/ok? extract-result)
      extract-result
      (let [{:keys [extracted-dir artifact]} (:payload extract-result)
            found-result (find-executable extracted-dir "bin/java")]
        (if-not (result/ok? found-result)
          found-result
          (result/ok {:path (:path (:payload found-result)) :artifact artifact}))))))

(defn- resolve-igs
  "Resolves each {:name :version} in ig-refs against artifacts (kind
  :profile), returning result/ok [path ...] or the first rejection/
  error encountered. Empty ig-refs resolves trivially to []."
  [artifacts ig-refs resolve-artifact]
  (reduce (fn [acc {:keys [name version]}]
            (if-not (result/ok? acc)
              (reduced acc)
              (let [r (resolve-artifact artifacts name version)]
                (if-not (result/ok? r)
                  (reduced r)
                  (result/ok (conj (:payload acc) (:path (:payload r))))))))
          (result/ok [])
          ig-refs))

(defn- resolve-ig-artifacts
  "Like `resolve-igs`, but resolves each ig-ref to its artifact record
  (not its filesystem path) -- the shape the verdict cache's key needs
  (name+version+sha256), not the shape the validator's argv needs."
  [artifacts ig-refs resolve-artifact]
  (reduce (fn [acc {:keys [name version]}]
            (if-not (result/ok? acc)
              (reduced acc)
              (let [r (resolve-artifact artifacts name version)]
                (if-not (result/ok? r)
                  (reduced r)
                  (result/ok (conj (:payload acc) (:artifact (:payload r))))))))
          (result/ok [])
          ig-refs))

(defn- cache-argv-shape
  "The parts of the validator's argv that vary the *invocation*, not
  the per-run scratch paths (-output=<path>, the input path itself) --
  those don't change what the validator does, only where its output
  lands, so they're deliberately excluded from the verdict-cache key.
  ig-refs are named by id (name@version), not resolved path, matching
  :ig-artifacts' own path-independence in the cache key."
  [fhir-version ig-refs]
  (vec (concat ["-version" fhir-version "-tx" "n/a"]
               (mapcat (fn [{:keys [name version]}] ["-ig" (str name "@" version)]) ig-refs))))

(defn- verdict-cache-lookup
  "Resolves the validator + IG artifacts (cheap: cache-directory
  lookups, no subprocess) and the input's content hash, builds the
  verdict-cache key, and checks for a hit -- all BEFORE `execute` would
  run the subprocess. Returns {:key ... :hit ...} (hit nil on a miss),
  or nil if artifact resolution itself failed (in which case the
  caller falls through to the normal execute path, which will surface
  the same resolution failure through its own, already-tested error
  handling, rather than this seam inventing a second copy of it)."
  [path {:keys [artifacts ig-refs fhir-version verdict-cache-dir resolve-artifact]
         :or {ig-refs [] fhir-version default-fhir-version resolve-artifact artifact/resolve}}]
  (let [validator-resolve (resolve-artifact artifacts engine-name "6.9.12")]
    (when (result/ok? validator-resolve)
      (let [ig-resolve (resolve-ig-artifacts artifacts ig-refs resolve-artifact)]
        (when (result/ok? ig-resolve)
          (let [key (verdict-cache/cache-key
                     {:content-sha256 (digest/sha256-file path)
                      :validator-artifact (:artifact (:payload validator-resolve))
                      :ig-artifacts (:payload ig-resolve)
                      :argv-shape (cache-argv-shape fhir-version ig-refs)
                      :verdict-mapping-version verdict-mapping-version})]
            {:key key :hit (verdict-cache/lookup verdict-cache-dir key)}))))))

(defn execute
  "Execute half of the two-step engine (pattern nursery #1). Resolves
  the validator artifact (name \"fhir-validator-cli\") and the pinned
  JVM runtime from artifacts, resolves any :ig-refs (each {:name
  :version} against a :profile-kind artifact -- empty by default, no
  IG pinned this session), then runs the validator as a subprocess via
  the invocation wrapper: `-version <fhir-version> -tx n/a [-ig
  <path>]... -output=<file> <input-path>`. The raw OperationOutcome
  JSON is preserved verbatim on disk (:outcome-path) alongside the
  invocation record, and read back into :raw-outcome for `interpret`.

  Options: :input-path, :artifacts (a lockfile's :artifacts vector),
  :out-dir (where the outcome JSON and invocation logs land),
  :fhir-version (default \"4.0\"), :ig-refs (default []), :java-bin
  (bypasses registry resolution when given, like corpus.generate),
  :resolve-artifact, :resolve-java-bin, :run-invocation (all
  injectable, defaulting to the real implementations).

  Returns result/ok {:invocation :outcome-path :raw-outcome :engine
  {:name :version}}, or the first failing step's result unchanged."
  [{:keys [input-path artifacts out-dir fhir-version ig-refs java-bin
           resolve-artifact resolve-java-bin run-invocation]
    :or {fhir-version default-fhir-version
         ig-refs []
         resolve-artifact artifact/resolve
         resolve-java-bin resolve-java-bin
         run-invocation invocation/run!}}]
  (let [validator-resolve (resolve-artifact artifacts engine-name "6.9.12")]
    (if-not (result/ok? validator-resolve)
      validator-resolve
      (let [{:keys [path artifact]} (:payload validator-resolve)
            java-bin-result (if java-bin
                               (result/ok {:path java-bin :artifact nil})
                               (resolve-java-bin artifacts {}))]
        (if-not (result/ok? java-bin-result)
          java-bin-result
          (let [ig-resolve (resolve-igs artifacts ig-refs resolve-artifact)]
            (if-not (result/ok? ig-resolve)
              ig-resolve
              (let [resolved-java-bin (:path (:payload java-bin-result))
                    out (io/file out-dir)
                    _ (.mkdirs out)
                    input-name (.getName (io/file input-path))
                    outcome-path (.getAbsolutePath (io/file out (str input-name ".outcome.json")))
                    stdout-path (.getAbsolutePath (io/file out (str input-name ".validator-stdout.log")))
                    stderr-path (.getAbsolutePath (io/file out (str input-name ".validator-stderr.log")))
                    ig-args (mapcat (fn [ig-path] ["-ig" ig-path]) (:payload ig-resolve))
                    args (vec (concat ["-version" fhir-version "-tx" "n/a"] ig-args
                                       [(str "-output=" outcome-path) input-path]))
                    invocation-result (run-invocation {:command resolved-java-bin :args (into ["-jar" path] args)
                                                        :stdout-path stdout-path :stderr-path stderr-path})]
                (if-not (result/ok? invocation-result)
                  invocation-result
                  (result/ok {:invocation (:payload invocation-result)
                              :outcome-path outcome-path
                              :raw-outcome (json/read-str (slurp outcome-path))
                              :engine {:name engine-name :version (:version artifact)}}))))))))))

;; ---- interpret: pure, versioned ----

(defn- severity->keyword
  [s]
  (case s
    "fatal" :fatal
    "error" :error
    "warning" :warning
    "information" :information
    :information))

(defn- issue-message
  [issue]
  (or (get-in issue ["details" "text"]) (get issue "diagnostics") ""))

(defn- issue-locator-path
  "Normalizes the validator's own expression syntax -- which embeds an
  inline resource-type/id disambiguation, e.g.
  \"Bundle.entry[0].resource/*Patient/9f8348fd-...*/.gender\" -- by
  stripping the /*...*/  annotation, so the resulting path
  (\"Bundle.entry[0].resource.gender\") is directly comparable
  (ends-with) to a mutation's own locator path
  (\"entry[0].resource.gender\")."
  [issue]
  (let [raw (first (get issue "expression"))]
    (str/replace (or raw "") #"/\*[^*]*\*/" "")))

(def ^:private rejecting-severities
  "\"fatal\" added after a contract-pairing exercise found the
  validator emits it (not \"error\") for a resource missing
  resourceType entirely -- FHIR's own IssueSeverity ValueSet defines
  both; EXP-C5's own corpus and its other four operators' mutants
  never happened to trigger it, so it wasn't in the original
  classification table (see docs/experiments/EXP-C5-results.md's
  dated addendum)."
  #{"error" "fatal"})

(defn- issue->classification
  "Returns {:verdict ... :cause (only when :verdict is :no-verdict)}:
  :no-verdict/:terminology-suppressed (the engine ran without applying
  terminology -- the judge failed to fully apply the criterion,
  ADR-0010/O2, not the criterion failing to decide -- formerly
  :indeterminate), :rejected (a genuine error or fatal issue), or
  :pass (advisory warning/information)."
  [issue]
  (let [severity (get issue "severity")
        suppressed? (terminology-suppressed? (issue-message issue))]
    (cond
      suppressed? {:verdict :no-verdict :cause :terminology-suppressed}
      (contains? rejecting-severities severity) {:verdict :rejected}
      :else {:verdict :pass})))

(defn- issue->finding
  "disposition = this finding's verdict contribution per the versioned
  mapping (`issue->classification`); `policy` is reserved for the
  verdict->action layer (ADR-0009), one level up from a single finding.
  :cause rides alongside :disposition, present iff :disposition is
  :no-verdict (ADR-0010, judge.finding/valid-cause-pairing?)."
  [engine issue]
  (let [{:keys [verdict cause]} (issue->classification issue)]
    (cond-> {:severity (severity->keyword (get issue "severity"))
             :code (get issue "code")
             :locator {:format :fhir :path (issue-locator-path issue)}
             :message (issue-message issue)
             :engine engine
             :disposition verdict
             :native-ref {:expression (get issue "expression")}}
      cause (assoc :cause cause))))

(defn interpret
  "Interpret half (pure, versioned -- see `verdict-mapping-version`,
  cited to `verdict-mapping-cited-to`). Every issue in raw-outcome's
  \"issue\" array becomes one finding, classified :rejected /
  :no-verdict / :pass by `issue->classification` (recorded on the
  finding itself as :disposition, for auditability, with a sibling
  :cause when :disposition is :no-verdict). Overall verdict is the
  worst-of every finding's classification (judge.finding/worst-of):
  :rejected > :no-verdict > :pass -- when the overall verdict is
  :no-verdict, :cause is taken from the first contributing finding (the
  only cause in this taxonomy today is :terminology-suppressed, so no
  finding ever disagrees with another in practice). No issues at all is
  :pass with no findings."
  [raw-outcome engine]
  (let [issues (get raw-outcome "issue" [])
        findings (mapv #(issue->finding engine %) issues)
        verdict (finding/worst-of (map :disposition findings))
        cause (when (= verdict :no-verdict)
                (:cause (first (filter #(= :no-verdict (:disposition %)) findings))))]
    (cond-> {:verdict verdict :findings findings}
      cause (assoc :cause cause))))

;; ---- gate: read (never mutate) -> execute -> interpret ----

(defn gate-file
  "Gates one FHIR JSON file against opts (same shape execute takes,
  minus :input-path -- set here from path). Reads the file only to
  confirm it exists; never writes to it -- the Judge stage kind's own
  law (docs/notation.md). Returns result/ok {:verdict :findings
  :path}, or the first failing execute step's result unchanged.

  Verdict cache (ADR-0016, session ruling 3): unless :verdict-cache?
  is false, a hit against the content-addressed cache (keyed on the
  input's content hash, the resolved validator+IG artifact identities,
  the argv shape, and judge.fhir/verdict-mapping-version -- see
  judge.verdict-cache) returns the cached {:verdict :findings [:cause]}
  directly, skipping `execute` entirely -- so the validator subprocess
  never runs on a hit. A miss runs execute+interpret exactly as before
  and stores the interpret result under that same key for next time.
  :verdict-cache-dir defaults to verdict-cache/default-cache-dir."
  [path opts]
  (let [{:keys [verdict-cache?] :or {verdict-cache? true}} opts
        cache-dir (:verdict-cache-dir opts verdict-cache/default-cache-dir)
        cache-lookup (when verdict-cache? (verdict-cache-lookup path opts))]
    (if-let [hit (:hit cache-lookup)]
      (result/ok (assoc hit :path (str path)))
      (let [execute-result (execute (assoc opts :input-path path))]
        (if-not (result/ok? execute-result)
          execute-result
          (let [{:keys [raw-outcome engine]} (:payload execute-result)
                interpreted (interpret raw-outcome engine)]
            (when (and verdict-cache? cache-lookup)
              (verdict-cache/store! cache-dir (:key cache-lookup) interpreted))
            (result/ok (assoc interpreted :path (str path)))))))))

(defn- json-files-in
  [dir]
  (->> (.listFiles (io/file dir))
       (filter #(str/ends-with? (.getName ^File %) ".json"))
       (sort-by #(.getName ^File %))))

(defn gate-dir
  "Gates every *.json file under dir (sorted, deterministic order),
  reusing opts for every file. Returns result/ok {:results [...]}, or
  the first file's failing result unchanged (fail-fast, matching
  mutate-command's own batch discipline)."
  [dir opts]
  (reduce (fn [acc path]
            (if-not (result/ok? acc)
              (reduced acc)
              (let [r (gate-file (str path) opts)]
                (if-not (result/ok? r)
                  (reduced r)
                  (result/ok (update (:payload acc) :results conj (:payload r)))))))
          (result/ok {:results []})
          (json-files-in dir)))
