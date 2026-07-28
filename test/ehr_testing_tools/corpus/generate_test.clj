(ns ehr-testing-tools.corpus.generate-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [ehr-testing-tools.result :as result]
            [ehr-testing-tools.corpus.generate :as generate]
            [ehr-testing-tools.corpus.manifest :as manifest])
  (:import [java.io File]))

(defn- temp-dir []
  (let [f (File/createTempFile "generate-test" "")]
    (.delete f)
    (.mkdirs f)
    (.getAbsolutePath f)))

(defn- delete-tree!
  "Removes a derived, fixed-path output-dir before a zero-flag test runs
  -- these tests deliberately omit :output-dir to exercise D9's own
  derivation, so (unlike temp-dir above) the path is not unique per run;
  a leftover directory from a previous `make test` invocation would
  otherwise collide with the :output-dir-exists guard."
  [path]
  (let [f (io/file path)]
    (when (.exists f)
      (doseq [child (reverse (file-seq f))] (.delete ^File child)))))

(def synthea-artifact
  {:kind :engine :name "synthea" :version "4.0.0"
   :sha256 (apply str (repeat 64 "c"))
   :source "https://example.invalid/synthea.jar"
   :acquired "2026-07-24" :license-status :verified})

(defn- stub-deps
  "Fake read-lockfile/resolve-artifact/resolve-java-bin/run-invocation
  for fast, hermetic tests -- no real jar, no real JVM archive, no real
  subprocess. resolve-java-bin defaults to an always-ok stub so tests
  that don't care about JVM resolution (most of them -- it's exercised
  directly by the java-bin-specific tests below) aren't forced to stub
  it individually."
  [{:keys [lockfile-result resolve-result invocation-result invocation-args-atom]}]
  {:read-lockfile (fn [_path] lockfile-result)
   :resolve-artifact (fn [_artifacts _name _version] resolve-result)
   :resolve-java-bin (fn [_artifacts _opts]
                       (result/ok {:path "/stub/jdk/bin/java"
                                   :artifact {:name "temurin-jdk" :version "17.0.19+10"
                                              :sha256 (apply str (repeat 64 "d"))}}))
   :run-invocation (fn [invocation-opts]
                     (when invocation-args-atom (reset! invocation-args-atom invocation-opts))
                     invocation-result)})

(defn- ok-lockfile [] (result/ok {:artifacts [synthea-artifact]}))
(defn- ok-resolve [] (result/ok {:path "/fake/synthea.jar" :artifact synthea-artifact}))
(defn- ok-invocation []
  (result/ok {:command "java" :args ["-jar" "/fake/synthea.jar"]
              :exit-code 0 :duration-ms 42 :started-at "2026-07-24T00:00:00Z"
              :stdout-path "/fake/out.log" :stderr-path "/fake/err.log"
              :stdout-sha256 (apply str (repeat 64 "0"))
              :stderr-sha256 (apply str (repeat 64 "0"))}))

(deftest generate-happy-path-writes-manifest-test
  (let [out-dir (temp-dir)
        config-file (File/createTempFile "synthea" ".properties")
        _ (spit config-file "generate.thread_pool_size = 1\n")
        args-atom (atom nil)
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)
                          :invocation-args-atom args-atom})
        r (generate/generate! (merge deps
                                      {:config-path (.getAbsolutePath config-file)
                                       :seed 42
                                       :clinician-seed 999
                                       :population 10
                                       :reference-date "20260101"
                                       :output-dir out-dir}))]
    (is (result/ok? r))
    (let [manifest (:manifest (:payload r))]
      (is (= "1.1" (:schema-version manifest)))
      (is (= :generate (:stage manifest)))
      (is (= 42 (:master (:seeds manifest))))
      (is (= 999 (:clinician (:seeds manifest))))
      (is (= "20260101" (:reference-date (:engine-params manifest))))
      (is (= "synthea" (:name (:generator manifest))))
      (is (= "4.0.0" (:version (:generator manifest))))
      (is (= (:sha256 synthea-artifact) (:sha256 (:generator manifest))))
      ;; :runtime -- the JVM artifact resolved through the registry
      ;; (stub-deps' default resolve-java-bin stub), {name, version,
      ;; sha256} like :generator.
      (is (= "temurin-jdk" (:name (:runtime manifest))))
      (is (= "17.0.19+10" (:version (:runtime manifest))))
      (is (string? (:sha256 (:runtime manifest))))
      (is (= (.getAbsolutePath config-file) (:path (:config manifest))))
      (is (string? (:sha256 (:config manifest))))
      (is (= [] (:canonicalizers-applied manifest)))
      ;; Defaults, forced -- not queried from wherever the orchestrator
      ;; happens to be running (EXP-A4 found locale/timezone are
      ;; genuinely load-bearing for byte-identical output, so these must
      ;; be exactly what was passed to the subprocess, not ambient state).
      (is (= "en-US" (:locale (:environment manifest))))
      (is (= "UTC" (:timezone (:environment manifest))))
      (is (contains? (:environment manifest) :jvm-version)))
    ;; manifest.edn written alongside the (would-be) generated tree
    (let [written (edn/read-string (slurp (io/file out-dir "manifest.edn")))]
      (is (= (:manifest (:payload r)) written)))
    ;; invocation was constructed correctly: jar path, seed, population,
    ;; config path, output directory, and default forced locale/timezone
    ;; (before -jar) all land in the args.
    (let [invoked @args-atom
          arg-list (:args invoked)
          arg-str (clojure.string/join " " arg-list)
          jar-index (.indexOf arg-list "-jar")]
      (is (clojure.string/includes? arg-str "/fake/synthea.jar"))
      (is (clojure.string/includes? arg-str "-s 42"))
      (is (clojure.string/includes? arg-str "-cs 999"))
      (is (clojure.string/includes? arg-str "-p 10"))
      (is (clojure.string/includes? arg-str (str "-c " (.getAbsolutePath config-file))))
      (is (clojure.string/includes? arg-str "-r 20260101"))
      (is (clojure.string/includes? arg-str (str "--exporter.baseDirectory=" out-dir)))
      (is (< (.indexOf arg-list "-Duser.language=en") jar-index))
      (is (< (.indexOf arg-list "-Duser.country=US") jar-index))
      (is (< (.indexOf arg-list "-Duser.timezone=UTC") jar-index)))))

(deftest generate-honors-explicit-locale-and-timezone-override-test
  (let [out-dir (temp-dir)
        config-file (File/createTempFile "synthea" ".properties")
        args-atom (atom nil)
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)
                          :invocation-args-atom args-atom})
        r (generate/generate! (merge deps
                                      {:config-path (.getAbsolutePath config-file)
                                       :seed 1 :clinician-seed 2 :population 1
                                       :reference-date "20260101"
                                       :output-dir out-dir
                                       :locale "fr-FR"
                                       :timezone "Asia/Tokyo"}))]
    (is (result/ok? r))
    (is (= "fr-FR" (:locale (:environment (:manifest (:payload r))))))
    (is (= "Asia/Tokyo" (:timezone (:environment (:manifest (:payload r))))))
    (let [arg-list (:args @args-atom)]
      (is (some #{"-Duser.language=fr"} arg-list))
      (is (some #{"-Duser.country=FR"} arg-list))
      (is (some #{"-Duser.timezone=Asia/Tokyo"} arg-list)))))

(deftest generate-places-jvm-args-before-jar-test
  ;; JVM system properties (-Duser.language=, -Duser.timezone=, etc.) are
  ;; only honored by the JVM if they appear BEFORE -jar on the command
  ;; line -- after -jar, they'd be passed as plain program arguments to
  ;; Synthea's main class instead. EXP-A4's locale/timezone rounds
  ;; depend on this placement being correct.
  (let [out-dir (temp-dir)
        config-file (File/createTempFile "synthea" ".properties")
        args-atom (atom nil)
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)
                          :invocation-args-atom args-atom})
        r (generate/generate! (merge deps
                                      {:config-path (.getAbsolutePath config-file)
                                       :seed 1 :clinician-seed 2 :population 1 :reference-date "20260101"
                                       :output-dir out-dir
                                       :jvm-args ["-Duser.language=fr" "-Duser.country=FR"]}))]
    (is (result/ok? r))
    (let [invoked-args (:args @args-atom)
          jar-index (.indexOf invoked-args "-jar")
          lang-index (.indexOf invoked-args "-Duser.language=fr")
          country-index (.indexOf invoked-args "-Duser.country=FR")]
      (is (not= -1 jar-index))
      (is (not= -1 lang-index))
      (is (< lang-index jar-index) "-Duser.language must precede -jar")
      (is (< country-index jar-index) "-Duser.country must precede -jar"))))

(deftest generate-records-actual-generator-jvm-version-not-orchestrators-test
  ;; The manifest's :jvm-version must describe the JVM that actually ran
  ;; Synthea (the subprocess named by :java-bin), not the Clojure
  ;; orchestrator's own JVM -- those can differ (and did, in practice:
  ;; this environment runs the CLI on Java 11 but Synthea v4.0.0 requires
  ;; Java 17+, so a fixed :java-version-fn call is the only way to
  ;; capture the truth instead of System/getProperty "java.version").
  (let [out-dir (temp-dir)
        config-file (File/createTempFile "synthea" ".properties")
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)})
        r (generate/generate!
           (merge deps
                  {:config-path (.getAbsolutePath config-file)
                   :seed 1 :population 1 :reference-date "20260101"
                   :output-dir out-dir
                   :java-bin "/fake/jdk17/bin/java"
                   :java-version-fn (fn [java-bin] (str "STUBBED-VERSION-FOR:" java-bin))}))]
    (is (result/ok? r))
    (is (= "STUBBED-VERSION-FOR:/fake/jdk17/bin/java"
           (:jvm-version (:environment (:manifest (:payload r))))))))

(deftest generate-propagates-lockfile-read-failure-test
  (let [deps (stub-deps {:lockfile-result (result/error :not-found {:path "artifacts.lock.edn"})
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)})
        r (generate/generate! (merge deps {:config-path "x" :seed 1 :population 1
                                            :output-dir (temp-dir)}))]
    (is (result/error? r))
    (is (= :not-found (:category r)))))

(deftest generate-propagates-resolve-failure-without-fetching-test
  ;; generate! never auto-fetches -- if the artifact isn't already
  ;; resolvable, that's the caller's job (`ehr artifact fetch` first).
  (let [deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (result/rejected :not-cached {:name "synthea" :version "4.0.0"})
                          :invocation-result (ok-invocation)})
        r (generate/generate! (merge deps {:config-path "x" :seed 1 :population 1
                                            :output-dir (temp-dir)}))]
    (is (result/rejected? r))
    (is (= :not-cached (:category r)))))

(deftest generate-propagates-invocation-failure-test
  (let [deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (result/error :spawn-failed {:message "no java"})})
        r (generate/generate! (merge deps {:config-path "x" :seed 1 :population 1
                                            :output-dir (temp-dir)}))]
    (is (result/error? r))
    (is (= :spawn-failed (:category r)))))

;; ---- java-bin resolved via the artifact registry, not PATH (P4:
;; the JVM is now a locked :runtime artifact) ----

(deftest generate-resolves-java-bin-from-registry-when-not-given-test
  (let [out-dir (temp-dir)
        config-file (File/createTempFile "synthea" ".properties")
        args-atom (atom nil)
        resolve-java-bin-calls (atom [])
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)
                          :invocation-args-atom args-atom})
        r (generate/generate!
           (merge deps
                  {:config-path (.getAbsolutePath config-file)
                   :seed 1 :clinician-seed 2 :population 1 :reference-date "20260101"
                   :output-dir out-dir
                   :resolve-java-bin (fn [artifacts _opts]
                                       (swap! resolve-java-bin-calls conj artifacts)
                                       (result/ok {:path "/resolved/jdk/bin/java"
                                                   :artifact {:name "temurin-jdk" :version "17.0.19+10"
                                                              :sha256 (apply str (repeat 64 "d"))}}))}))]
    (is (result/ok? r))
    (is (= 1 (count @resolve-java-bin-calls)) "resolve-java-bin must be consulted when :java-bin isn't given")
    (is (= "/resolved/jdk/bin/java" (:command @args-atom))
        "the invocation must run the registry-resolved java, not PATH's \"java\"")))

(deftest generate-explicit-java-bin-skips-registry-resolution-test
  (let [out-dir (temp-dir)
        config-file (File/createTempFile "synthea" ".properties")
        args-atom (atom nil)
        resolve-java-bin-calls (atom 0)
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)
                          :invocation-args-atom args-atom})
        r (generate/generate!
           (merge deps
                  {:config-path (.getAbsolutePath config-file)
                   :seed 1 :clinician-seed 2 :population 1 :reference-date "20260101"
                   :output-dir out-dir
                   :java-bin "/explicit/java"
                   :resolve-java-bin (fn [_artifacts _opts] (swap! resolve-java-bin-calls inc) (result/ok {}))}))]
    (is (result/ok? r))
    (is (zero? @resolve-java-bin-calls) "an explicit :java-bin must bypass registry resolution entirely")
    (is (= "/explicit/java" (:command @args-atom)))
    (is (not (contains? (:manifest (:payload r)) :runtime))
        "no resolved JVM artifact means the manifest must not fabricate a :runtime record")))

(deftest generate-manifest-validates-as-schema-v1-1-test
  (let [out-dir (temp-dir)
        config-file (File/createTempFile "synthea" ".properties")
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)})
        r (generate/generate! (merge deps {:config-path (.getAbsolutePath config-file)
                                            :seed 1 :clinician-seed 2 :population 1
                                            :reference-date "20260101" :output-dir out-dir}))]
    (is (result/ok? r))
    (is (manifest/valid-v1-1? (:manifest (:payload r))))))

(deftest generate-propagates-java-bin-resolution-failure-test
  (let [deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)})
        r (generate/generate!
           (merge deps
                  {:config-path "x" :seed 1 :population 1 :reference-date "20260101"
                   :output-dir (temp-dir)
                   :resolve-java-bin (fn [_artifacts _opts]
                                       (result/rejected :not-cached {:name "temurin-jdk" :version "17.0.19+10"}))}))]
    (is (result/rejected? r))
    (is (= :not-cached (:category r)))))

(deftest resolve-java-bin-composes-resolve-and-extract-and-find-executable-test
  (let [extract-calls (atom [])
        find-calls (atom [])
        resolve-and-extract (fn [artifacts name version _opts]
                              (swap! extract-calls conj [name version])
                              (result/ok {:extracted-dir "/fake/extracted"
                                          :artifact {:name name :version version
                                                     :sha256 (apply str (repeat 64 "e"))}}))
        find-executable (fn [dir relative-path]
                          (swap! find-calls conj [dir relative-path])
                          (result/ok {:path (str dir "/" relative-path)}))
        r (generate/resolve-java-bin [] {:resolve-and-extract resolve-and-extract
                                          :find-executable find-executable})]
    (is (result/ok? r))
    (is (= [[generate/jdk-name generate/jdk-version]] @extract-calls))
    (is (= [["/fake/extracted" "bin/java"]] @find-calls))
    (is (= "/fake/extracted/bin/java" (:path (:payload r))))
    (is (= generate/jdk-name (:name (:artifact (:payload r)))))))

(deftest resolve-java-bin-propagates-resolve-and-extract-failure-test
  (let [r (generate/resolve-java-bin [] {:resolve-and-extract (fn [_ _ _ _] (result/rejected :not-cached {}))
                                          :find-executable (fn [_ _] (throw (ex-info "must not be called" {})))})]
    (is (result/rejected? r))
    (is (= :not-cached (:category r)))))

;; ---- D9: zero-flag defaults (docs/source-sink-design.md Part IX.2,
;; ADR-0019) -- red until Step 4 wires :or defaults into generate! and
;; ships resources/synthea-default.properties. ----

(deftest generate-zero-flag-defaults-assembly-test
  ;; No :seed/:clinician-seed/:population/:reference-date/:output-dir/
  ;; :config-path given at all -- the pinned D9 defaults must be what
  ;; actually reaches the subprocess args and the manifest, not merely
  ;; documented.
  (delete-tree! (generate/default-output-dir generate/default-seed generate/default-population))
  (let [args-atom (atom nil)
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)
                          :invocation-args-atom args-atom})
        r (generate/generate! deps)]
    (is (result/ok? r))
    (let [manifest (:manifest (:payload r))
          arg-str (clojure.string/join " " (:args @args-atom))]
      (is (= 1 (:master (:seeds manifest))) "default --seed is the pinned constant 1")
      (is (= 1 (:clinician (:seeds manifest))) "default --clinician-seed derives from --seed, not a separate constant")
      (is (= "20260101" (:reference-date (:engine-params manifest))) "default --reference-date is the pinned constant")
      (is (= generate/default-config-path (:path (:config manifest))) "default --config-path is the shipped resources/ properties file")
      (is (clojure.string/includes? arg-str "-s 1"))
      (is (clojure.string/includes? arg-str "-cs 1"))
      (is (clojure.string/includes? arg-str "-p 5"))
      (is (clojure.string/includes? arg-str "-r 20260101"))
      (is (clojure.string/includes? arg-str (str "-c " generate/default-config-path)))
      (is (clojure.string/includes? arg-str "--exporter.baseDirectory=")
          "default --output-dir is derived, not required")
      (is (clojure.string/includes? (:output-dir (:payload r)) "synthea-s1-p5")
          "derived --output-dir names the seed and population it was derived from"))))

(deftest generate-clinician-seed-derives-from-explicit-seed-test
  ;; D9: "--clinician-seed defaults to --seed's value" -- must track an
  ;; explicitly-given --seed too, not just the pinned default.
  (delete-tree! (generate/default-output-dir 7 generate/default-population))
  (let [args-atom (atom nil)
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)
                          :invocation-args-atom args-atom})
        r (generate/generate! (merge deps {:seed 7}))]
    (is (result/ok? r))
    (is (= 7 (:master (:seeds (:manifest (:payload r))))))
    (is (= 7 (:clinician (:seeds (:manifest (:payload r)))))
        "an explicit --seed with no --clinician-seed still derives clinician-seed from it")
    (is (clojure.string/includes? (clojure.string/join " " (:args @args-atom)) "-cs 7"))))

(deftest generate-rejects-when-output-dir-already-has-content-test
  ;; Determinism probe finding (2026-07-28, UX-1 build session,
  ;; author-directed): D9's derived --output-dir is a *stable* path for a
  ;; given seed/population, so a second zero-flag invocation lands in the
  ;; same directory as the first. Synthea itself throws
  ;; FileAlreadyExistsException per patient file but swallows it and
  ;; still exits 0 -- silently writing nothing while generate! reported
  ;; :ok. Fail fast instead, before invoking anything.
  (let [out-dir (temp-dir)
        _ (spit (io/file out-dir "stale-file.json") "{}")
        run-invocation-calls (atom 0)
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)})
        r (generate/generate!
           (merge deps
                  {:run-invocation (fn [opts] (swap! run-invocation-calls inc) ((:run-invocation deps) opts))
                   :seed 1 :clinician-seed 1 :population 5 :reference-date "20260101"
                   :output-dir out-dir}))]
    (is (result/error? r))
    (is (= :output-dir-exists (:category r)))
    (is (zero? @run-invocation-calls) "must fail before invoking the subprocess, not after")))

(deftest generate-creates-output-dir-if-missing-test
  (let [parent (temp-dir)
        out-dir (str parent "/nested/does/not/exist/yet")
        config-file (File/createTempFile "synthea" ".properties")
        deps (stub-deps {:lockfile-result (ok-lockfile)
                          :resolve-result (ok-resolve)
                          :invocation-result (ok-invocation)})
        r (generate/generate! (merge deps {:config-path (.getAbsolutePath config-file)
                                            :seed 1 :population 1 :reference-date "20260101"
                                            :output-dir out-dir}))]
    (is (result/ok? r))
    (is (.isDirectory (io/file out-dir)))))
