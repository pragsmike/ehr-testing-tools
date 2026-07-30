(ns ehrt.judge-v2-nist.v2
  "Profile-tier HL7 v2 judge: NIST v2-validation engine
  (gov.nist:hl7-v2-validation), in-process via its Java-friendly
  hl7.v2.validation.SyncHL7Validator -- verified against 1.7.3 by
  direct Clojure interop spike (2026-07-30, cloud session): build a
  ValidationContext from an IGAMT profile bundle (Π), call
  `.check(message, msgId)`, get a gov.nist.validation.report.Report
  back synchronously. No Scala-specific interop friction on this path:
  SyncHL7Validator, ValidationContextBuilder, Report, and Entry are all
  plain-shaped JVM classes; the one Scala surface touched is
  `(.messages (.profile v))` (a scala Map), crossed once at validator
  construction via scala.jdk.javaapi.CollectionConverters.

  Pattern nursery #1 (two-step engines): `execute` captures the raw
  Report verbatim as data (never throws); `interpret` is the pure
  function from that capture to canonical findings/verdict.
  Pattern nursery #2 (invocation record): engine name + version (read
  from the jar's pom.properties, the version that actually ran) +
  input sha256 + PROFILE-BUNDLE sha256s -- the profile is an input in
  this tier, so the invocation record hashes Π alongside m.

  Verdict policy (ADR-0012, mirrors ADR-0010 vocabulary):
  - classification \"Error\" -> finding :error; any -> :rejected.
  - \"Warning\"/\"Alert\"/\"High Alert\" -> :warning findings, :pass.
  - \"Informational\"/\"Affirmative\" -> :information, :pass.
  - \"Specification Error\" -> the PROFILE (not the message) is
    defective; the criterion could not be fully applied ->
    :no-verdict with cause :profile-spec-error
    (ehrt.judge.finding/Cause's second specimen, ADR-0012).
  - category \"VS Not Found\"/\"Empty VS\"/\"External Value Set
    Validation Disabled\" when the value-set library is absent or
    partial -> the v2 analog of judge-fhir-official's
    terminology-suppressed :no-verdict cause.

  msg-id contract (ADR-0012): a profile bundle may declare more than
  one message id (`make-validator`'s own :msg-ids). `execute` refuses
  (throws ex-info, {:type :ambiguous-msg-id :msg-ids [...]}) when more
  than one msg-id exists and no explicit :msg-id was given -- picking
  one implicitly (e.g. by sorting) would silently validate against the
  wrong message type. A single-id profile needs no :msg-id at all.

  Alignment hook for the mutation module: the engine's finding
  taxonomy is DECLARED DATA -- reference.conf inside the validation
  jar defines every classification and category (Usage, Cardinality,
  Length, Format, Constraint Failure, CoConstraint Failure, Predicate
  Failure, Slicing, Code Not Found, ...) and `checkUsingConfiguration`
  accepts a config Reader overriding classifications per detection.
  Mutation defect classes can therefore be paired 1:1 with expected
  Entry categories, and the pairing is checkable against the engine's
  own config rather than prose."
  (:require [clojure.java.io :as io]
            [ehrt.kernel.interface :as kernel])
  (:import [hl7.v2.validation ValidationContextBuilder SyncHL7Validator]
           [gov.nist.validation.report Report Entry]
           [java.io File FileInputStream InputStream]
           [java.util Arrays Properties]))

(def engine-name "nist-v2-validation")

(defn engine-version
  "The running hl7-v2-validation jar's own version from its packaged
  pom.properties (docs/dev/engine-onboarding.md discipline), else
  \"unknown\"."
  []
  (if-let [res (io/resource "META-INF/maven/gov.nist/hl7-v2-validation/pom.properties")]
    (let [props (doto (Properties.) (.load (io/input-stream res)))]
      (.getProperty props "version"))
    "unknown"))

;; ---- profile bundle (Π) ----

(def ^:private bundle-files
  {:profile           "PROFILE.xml"          ; required
   :constraints       "CONSTRAINTS.xml"      ; optional
   :value-sets        "VALUESETS.xml"        ; optional
   :value-set-bindings "VALUESETBINDINGS.xml" ; optional (CDC's fetcher
                                             ; spells it VALUSETBINDINGS.xml
                                             ; -- accept both, see below)
   :co-constraints    "COCONSTRAINTS.xml"    ; optional
   :slicings          "SLICINGS.xml"})       ; optional

(defn- bundle-file ^File [dir k]
  (let [f (io/file dir (bundle-files k))]
    (cond
      (.exists f) f
      ;; IGAMT/CDC spelling drift observed in the wild (gov.cdc wrapper
      ;; reads VALUSETBINDINGS.xml, one E): accept the variant.
      (= k :value-set-bindings)
      (let [alt (io/file dir "VALUSETBINDINGS.xml")] (when (.exists alt) alt))
      :else nil)))

(defn make-validator
  "Builds a SyncHL7Validator from an IGAMT profile-bundle directory.
  PROFILE.xml is required; every other artifact is wired only when
  present. Returns {:validator SyncHL7Validator :msg-ids [id ...]
  :bundle-sha256s {k sha}} -- msg-ids are the conformance profile's own
  message ids (the required second argument to `.check`); bundle
  sha256s feed the invocation record."
  [bundle-dir]
  (let [pf (or (bundle-file bundle-dir :profile)
               (throw (ex-info "PROFILE.xml is required" {:dir (str bundle-dir)})))
        opt (fn [k] (bundle-file bundle-dir k))
        in  (fn [^File f] (FileInputStream. f))
        b   (ValidationContextBuilder. ^InputStream (in pf))]
    (when-let [f (opt :constraints)]
      (.useConformanceContext b ^java.util.List (Arrays/asList (into-array InputStream [(in f)]))))
    (when-let [f (opt :value-sets)]        (.useValueSetLibrary b (in f)))
    (when-let [f (opt :value-set-bindings)] (.useVsBindings b (in f)))
    (when-let [f (opt :co-constraints)]    (.useCoConstraintsContext b (in f)))
    (when-let [f (opt :slicings)]          (.useSlicingContext b (in f)))
    (let [v (SyncHL7Validator. (.getValidationContext b))]
      {:validator v
       :msg-ids (-> (scala.jdk.javaapi.CollectionConverters/asJava
                     (.messages (.profile v)))
                    .keySet vec)
       :bundle-sha256s (into {}
                             (keep (fn [[k _]]
                                     (when-let [f (bundle-file bundle-dir k)]
                                       [k (kernel/sha256-string (slurp f))])))
                             bundle-files)})))

;; ---- execute: raw capture, never throws ----

(defn- entry->raw [area ^Entry e]
  {:area area
   :path (.getPath e)
   :line (.getLine e)
   :column (.getColumn e)
   :category (.getCategory e)
   :classification (.getClassification e)
   :description (.getDescription e)})

(defn execute
  "Runs `.check` for one message against one profile msg-id, capturing
  the raw Report as data. Never throws for engine/data conditions:
  engine exceptions (unparseable message, unknown msg-id) are returned
  under :check-exception. DOES throw ex-info, {:type :ambiguous-msg-id
  :msg-ids [...]}, when the profile declares more than one msg-id and
  the caller didn't pass an explicit :msg-id -- this is a caller-
  contract violation (a programming defect in the call, not an engine
  verdict about the message under test), so it must not masquerade as
  :rejected or :no-verdict (ADR-0012)."
  [{:keys [validator msg-ids bundle-sha256s]} content & {:keys [msg-id]}]
  (when (and (nil? msg-id) (> (count msg-ids) 1))
    (throw (ex-info "profile declares more than one msg-id; :msg-id is required"
                     {:type :ambiguous-msg-id :msg-ids msg-ids})))
  (let [id (or msg-id (first msg-ids))
        base {:engine {:name engine-name :version (engine-version)}
              :input-sha256 (kernel/sha256-string content)
              :bundle-sha256s bundle-sha256s
              :msg-id id}]
    (try
      (let [^Report report (.check ^SyncHL7Validator validator ^String content ^String id)]
        (assoc base
               :check-exception nil
               :entries (vec (for [[area es] (.getEntries report), e es]
                               (entry->raw area e)))))
      (catch Exception e
        (assoc base
               :check-exception {:class (.getName (class e)) :message (.getMessage e)}
               :entries [])))))

;; ---- interpret: pure raw -> findings/verdict ----

(def ^:private classification->severity
  {"Error" :error
   "Specification Error" :error       ; severity of the finding itself
   "Warning" :warning
   "Alert" :warning
   "High Alert" :warning
   "Informational" :information
   "Affirmative" :information})

(def ^:private suppressed-vs-categories
  #{"VS Not Found" "Empty VS" "External Value Set Validation Disabled"
    "Excluded From Validation"})

(defn- raw-entry->finding [engine {:keys [area path line column category classification description] :as raw}]
  {:severity (get classification->severity classification :information)
   :code (str area "/" category)
   :locator {:path path :line line :column column}
   :message (or description "")
   :engine engine
   :native-ref raw})

(defn interpret
  "Pure function from execute's raw capture to
  {:findings [Finding ...] :verdict kw :cause kw?}.
  Verdict policy per this ns's docstring: engine exception or any
  \"Error\" classification -> :rejected; \"Specification Error\"
  (defective Π, criterion not fully applicable) -> :no-verdict/
  :profile-spec-error; suppressed value-set categories -> :no-verdict/
  :terminology-suppressed; otherwise :pass."
  [{:keys [engine entries check-exception]}]
  (let [findings (mapv #(raw-entry->finding engine %) entries)
        classifications (into #{} (map :classification) entries)
        vs-suppressed? (some suppressed-vs-categories (map :category entries))
        spec-error? (contains? classifications "Specification Error")]
    (cond
      check-exception
      {:findings findings :verdict :rejected
       :native-exception check-exception}

      (contains? classifications "Error")
      {:findings findings :verdict :rejected}

      spec-error?
      {:findings findings :verdict :no-verdict :cause :profile-spec-error}

      vs-suppressed?
      {:findings findings :verdict :no-verdict :cause :terminology-suppressed}

      :else
      {:findings findings :verdict :pass})))

;; ---- gates (same shape as judge-v2-hapi) ----

(defn gate-file
  "Profile-tier gate for one ER7 file against a profile bundle dir.
  validator-state comes from make-validator (build once per bundle,
  reuse across files -- context construction dominates cost)."
  [validator-state file & opts]
  (interpret (apply execute validator-state (slurp file) opts)))

(defn gate-dir
  "Applies gate-file to every *.hl7 file under dir. Returns
  {filename gate-result}."
  [validator-state dir & opts]
  (into {}
        (for [^File f (sort-by #(.getName ^File %) (file-seq (io/file dir)))
              :when (and (.isFile f) (.endsWith (.getName f) ".hl7"))]
          [(.getName f) (apply gate-file validator-state f opts)])))
