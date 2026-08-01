(ns ehrt.corpus.generate
  "The generation capability (ADR-0004): resolves the Synthea artifact,
  invokes it as a pinned subprocess, and preserves its output tree
  verbatim -- no normalization, no renaming, no post-processing. This is
  the execute half of the two-step engine pattern (pattern nursery #1);
  interpretation into canonical data is a separate, later step. A
  manifest v0 (pattern nursery #2/#3, EXP-A4's hypothesis) is written
  alongside the output tree as the committed provenance record."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [ehrt.corpus.manifest :as manifest]
            [ehrt.kernel.interface :as kernel]))

(def synthea-name "synthea")
(def synthea-version "4.0.0")
(def jdk-name "temurin-jdk")
(def jdk-version "21.0.12+8")
(def jdk-relative-path "bin/java")
(def default-lockfile-path "artifacts.lock.edn")
(def default-locale "en-US")
(def default-timezone "UTC")

;; Zero-flag defaults (D9, docs/source-sink-design.md Part IX.2,
;; ADR-0019). Every value here is a pinned constant, or derived
;; deterministically from other pinned inputs (the determinism law of
;; defaults, D8) -- never the clock, the environment, or the machine.
;; reference-date is frozen at a fixed date, not "today", precisely
;; because Synthea generates relative to wall-clock "now" otherwise.
(def default-seed 1)
(def default-reference-date "20260101")
(def default-population 5)
(def default-config-path "resources/synthea-default.properties")

(defn default-out-dir
  "The zero-flag --out-dir: derived from seed/population, not a
  required flag -- out/corpus/synthea-s<seed>-p<pop> (ADR-0013: every
  zero-flag default lives under the single tool-owned out/ root, not
  build tooling's own target/)."
  [seed population]
  (str "out/corpus/synthea-s" seed "-p" population))

(defn resolve-java-bin
  "Resolves the pinned JVM runtime through the artifact registry
  (P4: the JVM is a locked :runtime artifact, not something read off
  PATH or a hardcoded home path) -- ensures the cached Temurin archive
  is extracted, then locates bin/java inside it. Returns kernel/ok
  {:path :artifact}, or propagates the first failing step's
  rejection/error (:unknown-artifact, :not-cached, :extract-failed,
  :executable-not-found)."
  ([artifacts] (resolve-java-bin artifacts {}))
  ([artifacts {:keys [resolve-and-extract find-executable]
               :or {resolve-and-extract kernel/resolve-and-extract
                    find-executable kernel/find-executable}}]
   (let [extract-result (resolve-and-extract artifacts jdk-name jdk-version {})]
     (if-not (kernel/ok? extract-result)
       extract-result
       (let [{:keys [extracted-dir artifact]} (:payload extract-result)
             found-result (find-executable extracted-dir jdk-relative-path)]
         (if-not (kernel/ok? found-result)
           found-result
           (kernel/ok {:path (:path (:payload found-result)) :artifact artifact})))))))

(defn real-java-version
  "Queries java-bin's own reported version by actually running it -- the
  generator subprocess's JVM version can differ from the orchestrating
  Clojure process's JVM (it does, in this environment: the CLI runs on
  Java 11, but Synthea v4.0.0 requires Java 17+), so
  System/getProperty \"java.version\" would silently record the wrong
  thing. Returns \"unknown\" if java-bin can't be queried."
  [java-bin]
  (try
    (let [pb (ProcessBuilder. (into-array String [java-bin "-version"]))]
      (.redirectErrorStream pb true)
      (let [proc (.start pb)
            output (slurp (.getInputStream proc))]
        (.waitFor proc)
        (or (second (re-find #"version \"([^\"]+)\"" output)) "unknown")))
    (catch Exception _ "unknown")))

(defn- environment-record
  "Records the locale/timezone actually forced into the generator
  subprocess (the values used to build its -D jvm-args), not whatever
  the orchestrating process happens to have -- EXP-A4 found locale and
  timezone are both load-bearing for byte-identical output, so this
  must describe the subprocess's true environment, not ambient state."
  [java-bin java-version-fn locale timezone]
  {:locale locale
   :timezone timezone
   :jvm-version (java-version-fn java-bin)})

(defn- locale-jvm-args
  "en-US -> [\"-Duser.language=en\" \"-Duser.country=US\"]"
  [locale]
  (let [[lang country] (str/split locale #"-")]
    (cond-> [(str "-Duser.language=" lang)]
      (seq country) (conj (str "-Duser.country=" country)))))

(defn- timezone-jvm-args
  [timezone]
  [(str "-Duser.timezone=" timezone)])

(defn- synthea-args
  "jvm-args (e.g. -Duser.language=fr) must precede -jar -- the JVM only
  honors -D system properties given before the jar/main-class argument;
  after it, they'd be passed as plain program arguments to Synthea
  instead. extra-args (Synthea's own --config*=value overrides) belong
  after, as additional program arguments."
  [{:keys [jar-path seed clinician-seed population reference-date config-path out-dir
           jvm-args extra-args]}]
  (vec (concat jvm-args
               ["-jar" jar-path
                "-s" (str seed)
                "-cs" (str clinician-seed)
                "-p" (str population)
                "-r" reference-date
                "-c" config-path
                (str "--exporter.baseDirectory=" out-dir)]
               extra-args)))

(defn non-empty-existing-dir?
  "Public (ADR-0015): shared by `generate!` below and
  `ehrt.cli.core/generate-sim-command`, so the :out-dir-exists guard --
  and its own :hint text -- have exactly one place to change, not one
  per generator source."
  [out-dir]
  (let [f (io/file out-dir)]
    (and (.isDirectory f) (seq (.listFiles f)))))

(defn out-dir-exists-error
  "The shared :out-dir-exists rejection (D9's determinism law: a
  zero-flag command derives a stable path, so a second zero-flag run
  must never silently land in the same directory as the first) --
  factored out here so `generate!` and every other generator source's
  own CLI front door raise it identically. :hint text is a single
  shared string for the same reason (ADR-0015's own remedy-hint
  ruling revises it in one place, not once per call site): the literal
  remedy, not a bare refusal -- the exact `rm -rf` for a fresh
  identical rerun, and the --out-dir alternative for keeping the prior
  run. `render-pretty` (bases/cli) frames this rejection as the
  determinism story it is, not a bug -- same inputs, same directory,
  never silently overwritten."
  [out-dir]
  (kernel/error :out-dir-exists
                {:out-dir out-dir
                 :hint (str "same inputs always derive the same out-dir, so this run refused to silently overwrite the last one -- "
                            "run `rm -rf " out-dir "` to regenerate in place, "
                            "or pass a different --out-dir to keep this run and start a new one")}))

(defn generate!
  "Generates a Synthea corpus. Options:
    :config-path     -- path to a repo-authored Synthea properties file.
                        Defaults to default-config-path (the shipped
                        resources/ properties file, D9) when omitted.
    :seed            -- integer seed (patient generation). Defaults to
                        default-seed (D9) when omitted.
    :clinician-seed  -- integer seed (clinician/practitioner generation).
                        Required, not optional: EXP-A4 found that Synthea
                        defaults this to System.currentTimeMillis() when
                        -cs is omitted, which silently makes every
                        practitioner assignment (and everything that
                        references one) non-reproducible even with :seed
                        pinned -- :seed alone does not determine output.
                        Defaults to :seed's own (possibly also defaulted)
                        value when omitted (D9: \"one seed to remember,
                        not two\").
    :population      -- integer population size. Defaults to
                        default-population (D9) when omitted.
    :reference-date  -- YYYYMMDD string. Required, not optional: Synthea
                        generates relative to wall-clock \"now\" unless
                        told otherwise, which would make every run
                        non-reproducible by construction regardless of
                        what else is pinned. Defaults to
                        default-reference-date (D9) when omitted -- a
                        pinned constant, never \"today\".
    :out-dir         -- directory for Synthea's output tree + manifest.edn
                        (created if missing; gitignored -- not committed).
                        Defaults to (default-out-dir seed population)
                        when omitted (D9) -- a *stable* path for a given
                        seed/population, which is exactly why this
                        function rejects up front (:out-dir-exists)
                        when the directory already has content: a second
                        zero-flag invocation would otherwise land in the
                        same directory as the first, and Synthea's own
                        writer silently no-ops per file
                        (FileAlreadyExistsException, caught internally,
                        exit 0) rather than erroring -- probed directly,
                        2026-07-28, see docs/source-sink-design.md Part
                        IX.2's determinism-probe addendum.
    :locale          -- BCP47-ish \"language-COUNTRY\" (default \"en-US\").
                        Forced via -Duser.language/-Duser.country, placed
                        before -jar. Required-with-a-default, not
                        optional: EXP-A4 found locale affects at least one
                        date-computation field (a medication packaging
                        expiration subcomponent) -- leaving it to the
                        host's ambient default would make output depend
                        on which machine generated it.
    :timezone        -- e.g. \"UTC\" (default). Forced via
                        -Duser.timezone, placed before -jar.
                        Required-with-a-default: EXP-A4 found every FHIR
                        dateTime/instant field's serialized UTC-offset
                        depends on this -- same instant, different bytes.
    :jvm-args        -- additional JVM system properties beyond
                        locale/timezone, placed before -jar so the JVM
                        actually honors them
    :extra-args      -- additional Synthea CLI args (e.g. for varying
                        generate.thread_pool_size in EXP-A4 rounds),
                        appended after the standard ones
    :java-bin        -- java executable to invoke. When omitted, resolved
                        through the artifact registry (resolve-java-bin
                        above) against the pinned Temurin JDK 21 :runtime
                        artifact -- never PATH, never a hardcoded home
                        path. Must be Java 17+ for Synthea v4.0.0 (21
                        satisfies this; the pin moved to 21 in SS-1's
                        toolchain step, not because Synthea's own
                        requirement changed).
                        Passing :java-bin explicitly bypasses registry
                        resolution entirely (useful for a non-default
                        JVM, or for tests).
    :resolve-java-bin -- injectable for testing; defaults to
                        resolve-java-bin above. Only consulted when
                        :java-bin is omitted.
    :java-version-fn -- how to query the resolved java-bin's actual
                        version for the manifest's environment record;
                        defaults to real-java-version (injectable for
                        testing).
    :lockfile, :read-lockfile, :resolve-artifact, :run-invocation
                     -- injectable for testing; default to the real
                        artifact/invocation implementations.

  Never auto-fetches: if the Synthea artifact isn't already resolvable
  from the cache, this returns the resolve failure as-is -- run
  `ehr artifact fetch` first. Rejects up front with kernel/error
  :out-dir-exists {:out-dir :hint} if :out-dir already exists
  and is non-empty, before reading the lockfile or invoking anything --
  D9's derived --out-dir is stable across zero-flag calls, so this is
  the guard against the silent-no-op hazard described above. Returns
  kernel/ok {:manifest :out-dir}, or the first failing step's result
  (the out-dir check, lockfile read, artifact resolve, JVM resolve,
  or invocation) unchanged."
  [{:keys [config-path seed clinician-seed population reference-date out-dir
           locale timezone jvm-args extra-args java-bin resolve-java-bin
           java-version-fn lockfile read-lockfile resolve-artifact run-invocation]
    :or {locale default-locale timezone default-timezone
         jvm-args [] extra-args []
         resolve-java-bin resolve-java-bin
         java-version-fn real-java-version
         lockfile default-lockfile-path
         read-lockfile kernel/read-lockfile
         resolve-artifact kernel/resolve-artifact
         run-invocation kernel/run-invocation!
         config-path default-config-path
         seed default-seed
         reference-date default-reference-date
         population default-population}}]
  (let [clinician-seed (or clinician-seed seed)
        out-dir (or out-dir (default-out-dir seed population))]
   (if (non-empty-existing-dir? out-dir)
     (out-dir-exists-error out-dir)
    (let [lockfile-result (read-lockfile lockfile)]
     (if-not (kernel/ok? lockfile-result)
      lockfile-result
      (let [artifacts (:artifacts (:payload lockfile-result))
            resolve-result (resolve-artifact artifacts synthea-name synthea-version)]
        (if-not (kernel/ok? resolve-result)
          resolve-result
          (let [{:keys [path artifact]} (:payload resolve-result)
                java-bin-result (if java-bin
                                   (kernel/ok {:path java-bin :artifact nil})
                                   (resolve-java-bin artifacts {}))]
            (if-not (kernel/ok? java-bin-result)
              java-bin-result
              (let [resolved-java-bin (:path (:payload java-bin-result))
                    jvm-artifact (:artifact (:payload java-bin-result))
                    out-dir-file (io/file out-dir)
                    _ (.mkdirs out-dir-file)
                    stdout-path (.getAbsolutePath (io/file out-dir-file "synthea-stdout.log"))
                    stderr-path (.getAbsolutePath (io/file out-dir-file "synthea-stderr.log"))
                    all-jvm-args (vec (concat (locale-jvm-args locale) (timezone-jvm-args timezone) jvm-args))
                    args (synthea-args {:jar-path path :seed seed :clinician-seed clinician-seed
                                         :population population
                                         :reference-date reference-date
                                         :config-path config-path
                                         :out-dir (.getAbsolutePath out-dir-file)
                                         :jvm-args all-jvm-args
                                         :extra-args extra-args})
                    invocation-result (run-invocation {:command resolved-java-bin :args args
                                                        :stdout-path stdout-path
                                                        :stderr-path stderr-path})]
                (if-not (kernel/ok? invocation-result)
                  invocation-result
                  (let [m (manifest/build-v1-1
                           {:stage :generate
                            :generator {:name (:name artifact)
                                        :version (:version artifact)
                                        :sha256 (:sha256 artifact)}
                            :runtime (when jvm-artifact
                                       {:name (:name jvm-artifact)
                                        :version (:version jvm-artifact)
                                        :sha256 (:sha256 jvm-artifact)})
                            :seeds {:master seed :clinician clinician-seed}
                            :engine-params {:reference-date reference-date}
                            :config {:path config-path :sha256 (kernel/sha256-file config-path)}
                            :invocation (:payload invocation-result)
                            :canonicalizers-applied []
                            :environment (environment-record resolved-java-bin java-version-fn locale timezone)})]
                    (spit (io/file out-dir-file "manifest.edn") (pr-str m))
                    (kernel/ok {:manifest m :out-dir out-dir})))))))))))))
