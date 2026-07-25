(ns ehr-testing-tools.judge.v2
  "Base-structural HL7 v2 gate (P5): HAPI HL7v2 strict/default-validation
  parsing, plus HAPI's DefaultValidator, in-process. Pattern nursery #1
  (two-step engines) applies even without a subprocess: `execute`
  captures HAPI's raw parse/validate outcome verbatim (never throws --
  every exception it can hit is caught and returned as data); `interpret`
  is the pure, versioned function from that raw capture to canonical
  findings/verdict. Pattern nursery #2 (invocation record) applies too,
  in its in-process form: `execute`'s return carries the engine name and
  version (read from the running jar's own packaged Maven pom.properties
  on the classpath, per `docs/engine-onboarding.md` -- the version that
  actually ran, not a value hand-copied from deps.edn) and the input's
  content hash, even though there is no subprocess/PID/exit-code to record.

  Two HAPI signals feed a gate result, discovered empirically against
  this repo's own fixtures (see docs/experiments.md's P5 session report):
  1. Parse-time exceptions (`ca.uhn.hl7v2.HL7Exception` and its
     subclasses: message-structure resolution failures, encoding/
     delimiter failures, and -- because HAPI's `defaultValidation`
     context wires primitive-type checking into parsing itself --
     primitive data-type violations too, e.g. a malformed DTM value).
     Every one of these is a base-structural failure: verdict
     `:rejected`.
  2. Post-parse `DefaultValidator/validate`, run against an already-
     successfully-parsed message via a collecting
     `ValidationExceptionHandler` rather than a throw. Empty for every
     fixture this session's probes exercised (no terminology, no
     conformance profile at this tier -- there is little left for this
     path to catch once parsing itself already enforces primitive
     types). By author ruling, ANY exception this path collects is
     `:pass`-with-findings, never `:rejected` -- a deliberate policy
     simplification for the base-structural tier, not a claim that
     HAPI itself always reports low severity here.

  Nothing in this gate ever produces an `:indeterminate` finding: there
  is no terminology server and no conformance profile in play at this
  tier, so there is no check this gate can only partially resolve -- a
  check either ran (feeding a finding) or the message failed to parse
  at all (`:rejected`)."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [ehr-testing-tools.digest :as digest]
            [ehr-testing-tools.judge.finding :as finding]
            [ehr-testing-tools.result :as result])
  (:import [ca.uhn.hl7v2 DefaultHapiContext HL7Exception]
           [ca.uhn.hl7v2.validation ValidationExceptionHandler]
           [ca.uhn.hl7v2.validation.impl ValidationContextFactory]
           [java.io File]
           [java.util Properties]))

(def engine-name "hapi-hl7v2")

(defn hapi-version
  "The running hapi-base jar's own version, read from its packaged Maven
  pom.properties on the classpath -- the engine version that actually
  ran (docs/engine-onboarding.md), not a value hand-maintained
  alongside deps.edn's pin. Returns \"unknown\" if the resource isn't
  found (e.g. a non-Maven-packaged classpath in some other build)."
  []
  (if-let [res (io/resource "META-INF/maven/ca.uhn.hapi/hapi-base/pom.properties")]
    (let [props (doto (Properties.) (.load (io/input-stream res)))]
      (.getProperty props "version"))
    "unknown"))

;; ---- execute: raw capture, never throws ----

(defn- new-context
  []
  (doto (DefaultHapiContext.)
    (.setValidationContext (ValidationContextFactory/defaultValidation))))

(defn- collecting-handler
  []
  (let [store (atom [])]
    (reify ValidationExceptionHandler
      (onExceptions [_ exs] (swap! store into (seq exs)))
      (hasFailed [_] (boolean (seq @store)))
      (result [_] @store)
      (setValidationSubject [_ _] nil))))

(defn- location->raw
  [loc]
  (when (and loc (not (.isUnknown loc)))
    {:segment (.getSegmentName loc)
     :segment-repetition (.getSegmentRepetition loc)
     :field (.getField loc)
     :component (.getComponent loc)}))

(defn- exception->raw
  [^Exception e]
  (let [hl7? (instance? HL7Exception e)]
    {:class (.getName (class e))
     :message (.getMessage e)
     :severity (if hl7? (str (.getSeverity ^HL7Exception e)) "ERROR")
     :location (when hl7? (location->raw (.getLocation ^HL7Exception e)))}))

(defn execute
  "Execute half of the two-step engine (pattern nursery #1). Parses
  content with a strict/default-validation HAPI PipeParser; if parsing
  succeeds, runs the in-process DefaultValidator against the parsed
  message, collecting (never throwing) via a `ValidationExceptionHandler`.
  Never throws itself -- every exception either path can raise is caught
  and returned as data.

  Returns {:engine {:name :version} :input-sha256
  :parse-exception (nil, or {:class :message :severity :location})
  :validation-exceptions [...]} (each entry shaped like
  :parse-exception)."
  [content]
  (let [engine {:name engine-name :version (hapi-version)}
        input-sha256 (digest/sha256-string content)]
    (try
      (let [ctx (new-context)
            parser (.getPipeParser ctx)
            msg (.parse parser content)
            handler (collecting-handler)]
        (.validate (.getMessageValidator ctx) msg handler)
        {:engine engine
         :input-sha256 input-sha256
         :parse-exception nil
         :validation-exceptions (mapv exception->raw (.result handler))})
      (catch Exception e
        {:engine engine
         :input-sha256 input-sha256
         :parse-exception (exception->raw e)
         :validation-exceptions []}))))

;; ---- interpret: pure, versioned ----

(defn- kebab-case-class-name
  "\"ca.uhn.hl7v2.model.DataTypeException\" -> \"data-type-exception\" --
  the finding :code, derived from the raising exception's own simple
  class name rather than a hand-maintained code table, so a HAPI
  exception type this namespace hasn't seen before still produces a
  legible code instead of silently falling into a catch-all."
  [class-name]
  (-> class-name
      (str/replace #".*\." "")
      (str/replace #"([a-z0-9])([A-Z])" "$1-$2")
      str/lower-case))

(defn- severity->keyword
  [s]
  (case s
    "ERROR" :error
    "WARNING" :warning
    "INFO" :information
    :error))

(defn- raw->locator-path
  "The v2 locator string for a raw exception's :location. \"MSH\" (a
  coarse, segment-only fallback -- not further parseable by
  locator/v2-data-path, which requires a field) when no field-level
  location is available: message-structure-resolution and encoding
  failures are inherently about the message header, before any
  field-level location is even knowable."
  [{:keys [location]}]
  (let [{:keys [segment segment-repetition field component]} location]
    (if (and segment (some-> field pos?))
      (str segment
           (when (and segment-repetition (pos? segment-repetition)) (str "[" segment-repetition "]"))
           "-" field
           (when (and component (pos? component)) (str "-" component)))
      "MSH")))

(defn- raw->finding
  [engine raw]
  {:severity (severity->keyword (:severity raw))
   :code (kebab-case-class-name (:class raw))
   :locator {:format :v2 :path (raw->locator-path raw)}
   :message (:message raw)
   :engine engine
   :native-ref (select-keys raw [:class :location])})

(defn interpret
  "Interpret half (pure, versioned -- \"v1\", cited here since the
  mapping below may change with a future tier). raw's :parse-exception,
  when present, is a base-structural failure: verdict :rejected, one
  finding built from it. Otherwise every :validation-exceptions entry
  becomes a finding and the verdict is :pass -- see the module
  docstring for why every collected (non-parse-failure) signal is
  :pass-with-findings at this tier, by policy. Never :indeterminate:
  see the module docstring."
  [raw]
  (let [engine (:engine raw)]
    (if-let [pe (:parse-exception raw)]
      {:verdict :rejected :findings [(raw->finding engine pe)]}
      {:verdict :pass :findings (mapv #(raw->finding engine %) (:validation-exceptions raw))})))

;; ---- gate: read (never mutate) -> execute -> interpret ----

(defn gate-file
  "Gates one v2 message file. Reads content and never writes to it --
  the Gate stage kind's own law (docs/notation.md): gating never
  modifies the datum it judges. Returns result/ok {:verdict :findings
  :path}, or result/error :file-not-found if path doesn't name a
  readable file -- an operational condition (ADR-0004: exceptions are
  for programmer error, not for \"the caller passed a bad path\")."
  [path]
  (let [f (io/file path)]
    (if-not (.isFile f)
      (result/error :file-not-found {:path (str path)})
      (let [content (slurp f)
            raw (execute content)]
        (result/ok (assoc (interpret raw) :path (str path)))))))

(defn- hl7-files-in
  [dir]
  (->> (.listFiles (io/file dir))
       (filter #(str/ends-with? (.getName ^File %) ".hl7"))
       (sort-by #(.getName ^File %))))

(defn gate-dir
  "Gates every *.hl7 file under dir (sorted, deterministic order).
  Returns result/ok {:results [{:verdict :findings :path} ...]}."
  [dir]
  (result/ok {:results (mapv #(:payload (gate-file %)) (hl7-files-in dir))}))
