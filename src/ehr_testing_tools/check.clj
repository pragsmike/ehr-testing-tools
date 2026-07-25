(ns ehr-testing-tools.check
  "The Check capability (P6): dataset vs expectations -- the corpus's
  second judge, alongside Gate. Gate's judges are the **validator**
  species (docs/palgebra-design.md D2): their criterion comes from an
  institutional registry (a spec, a profile). Check is the **checker**
  species: its criterion is supplied by the occasion -- an expected
  corpus, or explicit assertions -- which is a genuinely different
  question -- \"is this conformant\" vs \"is this what I expected.\"
  Same arrow (subject × criterion -> judgment), same verdict type; the
  species differ only in where the criterion comes from.

  Check is judge-kind (docs/notation.md, ADR-0009): it produces a
  verdict plus findings over the datum and never modifies it. Unlike
  Gate, nothing in this v1 vocabulary maps to :indeterminate -- stated
  plainly here rather than left implicit, since :indeterminate is
  otherwise part of the judge kind's own floor.

  Assertions are data (EDN, Malli-schema'd, versioned as
  `assertion-vocabulary-version`) -- a deliberately small v1 vocabulary:
  :matches-expected (corpus-level golden equivalence against an
  expected corpus), :present/:absent (a locator resolves or doesn't),
  :value (a locator resolves to an exact expected value), :count (the
  element count at a locator satisfies an op), and :schema (a datum
  validates against a named, versioned Malli schema from
  ehr-testing-tools.check.schemas' registry). No arbitrary predicate
  functions in v1 -- data only, so an assertion set is itself
  inspectable, diffable, and portable, not a bag of closures.

  Golden equivalence (:matches-expected) pairs candidate-corpus files
  against expected-corpus files (by relative path, default, or by
  content-hash identity via :pair-by :hash), then compares each pair
  as `(= (canon x cs) (canon y cs))` for a declared, ordered
  canonicalizer list `cs` (ehr-testing-tools.canonical) -- equivalence
  IS canonical equality, not byte equality; an empty canonicalizer
  list falls back to literal byte/string equality with no parsing.
  Differences are reported with locator paths via
  ehr-testing-tools.diff (promoted from corpus.mutate's own diffing,
  now a second real consumer); unpaired files are reported as
  missing/extra findings.

  Report: reuses ehr-testing-tools.judge.report's aggregation verbatim
  -- build-report takes a format-agnostic {:path :verdict :findings}
  seq, which Check's own per-file results already are, so no refactor
  was needed there.

  Known v1 limitation, stated rather than hidden: an assertion set
  that mixes :matches-expected with per-file assertion kinds produces
  two separate report entries for a file targeted by both categories
  (one from corpus pairing, one from the per-file pass), rather than
  merging them -- acceptable for a deliberately small v1 vocabulary
  whose primary use pairs one corpus-level mode against a separate
  per-file mode, not both at once."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [malli.core :as m]
            [ehr-testing-tools.canonical :as canonical]
            [ehr-testing-tools.check.schemas :as schemas]
            [ehr-testing-tools.diff :as diff]
            [ehr-testing-tools.digest :as digest]
            [ehr-testing-tools.judge.report :as report]
            [ehr-testing-tools.locator :as locator]
            [ehr-testing-tools.result :as result])
  (:import [java.io File]))

(def engine-name "check")
(def assertion-vocabulary-version "v1")

;; ---- assertion vocabulary: data, Malli-schema'd, versioned ----

(def MatchesExpected
  [:map [:kind [:= :matches-expected]]])

(def PresentAbsent
  [:map [:kind [:enum :present :absent]] [:locator locator/Locator]])

(def ValueAssertion
  [:map [:kind [:= :value]] [:locator locator/Locator] [:expected :any]])

(def CountOp [:enum := :<= :>=])

(def CountAssertion
  [:map [:kind [:= :count]] [:locator locator/Locator] [:op CountOp] [:value :int]])

(def SchemaRef
  [:map [:id :keyword] [:version :string]])

(def SchemaAssertion
  [:map [:kind [:= :schema]] [:malli SchemaRef]])

(def Assertion
  [:multi {:dispatch :kind}
   [:matches-expected MatchesExpected]
   [:present PresentAbsent]
   [:absent PresentAbsent]
   [:value ValueAssertion]
   [:count CountAssertion]
   [:schema SchemaAssertion]])

(def Assertions [:vector Assertion])

(defn valid-assertion?
  [a]
  (m/validate Assertion a))

(defn valid-assertions?
  [as]
  (m/validate Assertions as))

(def default-assertions
  "The implied assertion set when :expected-dir is given and
  :assertions is omitted -- corpus-level golden equivalence, the
  common case."
  [{:kind :matches-expected}])

;; ---- file helpers (mirrors corpus.intake's own private helpers --
;; not reused directly since they're defn- there and this is a
;; genuinely separate concern; two similar ~10-line helpers is not
;; worth a shared abstraction yet) ----

(defn- files-in
  [dir]
  (->> (file-seq (io/file dir))
       (filter #(.isFile ^File %))
       (sort-by #(.getPath ^File %))))

(defn- relative-path
  [root-dir file]
  (let [root (str (.getCanonicalPath (io/file root-dir)) File/separator)
        full (.getCanonicalPath ^File file)
        rel (if (str/starts-with? full root) (subs full (count root)) full)]
    (str/replace rel File/separator "/")))

;; ---- pairing: candidate corpus files vs expected corpus files ----

(defn- index-by-relative-path
  [dir]
  (into {} (map (fn [f] [(relative-path dir f) f])) (files-in dir)))

(defn- index-by-content-hash
  [dir]
  (into {} (map (fn [f] [(digest/sha256-string (slurp f)) f])) (files-in dir)))

(defn- pair-files
  "Pairs candidate-dir's files against expected-dir's files, by
  relative path (:path, default) or by raw content-hash identity
  (:hash -- identical bytes present in both corpora regardless of
  filename). Returns {:pairs [[key candidate-file expected-file] ...]
  :missing [key ...] :extra [key ...]} -- missing: a key present in
  expected, absent from candidate; extra: the reverse. Sorted for
  deterministic report ordering."
  [candidate-dir expected-dir pair-by]
  (let [index-fn (if (= pair-by :hash) index-by-content-hash index-by-relative-path)
        cand-idx (index-fn candidate-dir)
        exp-idx (index-fn expected-dir)
        cand-keys (set (keys cand-idx))
        exp-keys (set (keys exp-idx))
        common (set/intersection cand-keys exp-keys)]
    {:pairs (mapv (fn [k] [k (get cand-idx k) (get exp-idx k)]) (sort common))
     :missing (vec (sort (set/difference exp-keys cand-keys)))
     :extra (vec (sort (set/difference cand-keys exp-keys)))}))

;; ---- canonicalization + comparison ----

(defn- canon-value
  "Canonicalizes content for comparison. An empty canonicalizer list
  is literal byte/string equality -- no parsing, works for any file
  type. A non-empty list parses content as JSON (the corpus substrate
  corpus.mutate's own canonicalizers already assume, EXP-B2), then
  threads the parsed value through the ordered canonicalizer chain
  (ehr-testing-tools.canonical/apply-canonicalizers)."
  [content steps]
  (if (empty? steps)
    (result/ok {:data content :applied []})
    (let [parsed (try (result/ok (json/read-str content))
                       (catch Exception _ (result/error :unparseable-content {})))]
      (if-not (result/ok? parsed)
        parsed
        (canonical/apply-canonicalizers (:payload parsed) steps)))))

(defn- compare-pair
  "Compares one candidate/expected file pair after canonicalization.
  Returns [] when equivalent, or a seq of findings -- one per differing
  locator path (diff/diff-paths), or a single unparseable-content
  finding if canonicalization itself failed on either side."
  [engine rel cand-file exp-file steps]
  (let [cand-canon (canon-value (slurp cand-file) steps)
        exp-canon (canon-value (slurp exp-file) steps)]
    (cond
      (not (result/ok? cand-canon))
      [{:severity :error :code "unparseable-content" :locator {:format :fhir :path ""}
        :message (str rel ": candidate content did not parse for canonicalization") :engine engine}]

      (not (result/ok? exp-canon))
      [{:severity :error :code "unparseable-content" :locator {:format :fhir :path ""}
        :message (str rel ": expected content did not parse for canonicalization") :engine engine}]

      :else
      (let [cand-data (:data (:payload cand-canon))
            exp-data (:data (:payload exp-canon))]
        (if (= cand-data exp-data)
          []
          (let [paths (sort-by diff/path->locator-path (diff/diff-paths exp-data cand-data))]
            (mapv (fn [p]
                    {:severity :error :code "content-mismatch"
                     :locator {:format :fhir :path (diff/path->locator-path p)}
                     :message (str rel ": candidate differs from expected")
                     :engine engine})
                  paths)))))))

(defn- run-matches-expected
  [{:keys [candidate-dir expected-dir pair-by canonicalizers]} engine]
  (if-not expected-dir
    [{:path (str candidate-dir) :verdict :rejected
      :findings [{:severity :error :code "missing-expected-dir" :locator {:format :fhir :path ""}
                  :message ":matches-expected requires :expected-dir" :engine engine}]}]
    (let [{:keys [pairs missing extra]} (pair-files candidate-dir expected-dir (or pair-by :path))
          paired (map (fn [[rel cand-file exp-file]]
                        (let [findings (compare-pair engine rel cand-file exp-file canonicalizers)]
                          {:path rel :verdict (if (seq findings) :rejected :pass) :findings findings}))
                      pairs)
          missing* (map (fn [k] {:path k :verdict :rejected
                                  :findings [{:severity :error :code "missing-file" :locator {:format :fhir :path ""}
                                              :message (str k " is present in the expected corpus but missing from the candidate corpus")
                                              :engine engine}]})
                        missing)
          extra* (map (fn [k] {:path k :verdict :rejected
                                :findings [{:severity :error :code "extra-file" :locator {:format :fhir :path ""}
                                            :message (str k " is present in the candidate corpus but not in the expected corpus")
                                            :engine engine}]})
                      extra)]
      (vec (concat paired missing* extra*)))))

;; ---- per-file assertions: :present, :absent, :value, :count, :schema ----

(defn- resolve-locator-value
  "Resolves locator-envelope against parsed-datum via the FHIR locator
  grammar (ehr-testing-tools.locator/fhir-data-path) -- the same
  data-path substrate corpus.mutate and gate.fhir already operate on.
  Returns [true value] if the path resolves (including to a
  legitimate nil), or [false nil] if the locator doesn't parse or
  doesn't resolve anywhere in parsed-datum."
  [parsed-datum locator-envelope]
  (let [path-result (locator/fhir-data-path (:path locator-envelope))]
    (if-not (result/ok? path-result)
      [false nil]
      (let [path (:payload path-result)
            sentinel ::not-found
            v (get-in parsed-datum path sentinel)]
        (if (= sentinel v) [false nil] [true v])))))

(defn assertion-findings
  "Evaluates one per-file assertion (:present/:absent/:value/:count/
  :schema -- never :matches-expected, which is corpus-level, not
  per-file) against parsed-datum. Returns [] when the assertion holds,
  or a seq of findings (ehr-testing-tools.judge.finding-shaped) when it
  doesn't. Exposed publicly (unlike the rest of this namespace's
  per-file machinery) because it's the natural unit to test the
  finding envelope against directly, without threading through
  check-corpus's own report aggregation, which collapses findings down
  to a bare :finding-count per file."
  [assertion parsed-datum engine]
  (case (:kind assertion)
    :present
    (let [[found? _] (resolve-locator-value parsed-datum (:locator assertion))]
      (if found?
        []
        [{:severity :error :code "absent" :locator (:locator assertion)
          :message (str "expected a value present at " (:path (:locator assertion))) :engine engine}]))

    :absent
    (let [[found? _] (resolve-locator-value parsed-datum (:locator assertion))]
      (if-not found?
        []
        [{:severity :error :code "present" :locator (:locator assertion)
          :message (str "expected no value at " (:path (:locator assertion))) :engine engine}]))

    :value
    (let [[found? v] (resolve-locator-value parsed-datum (:locator assertion))]
      (if (and found? (= v (:expected assertion)))
        []
        [{:severity :error :code "value-mismatch" :locator (:locator assertion)
          :message (str "expected " (pr-str (:expected assertion)) " at " (:path (:locator assertion))
                        (if found? (str ", found " (pr-str v)) ", but the locator did not resolve"))
          :engine engine}]))

    :count
    (let [[found? v] (resolve-locator-value parsed-datum (:locator assertion))
          n (cond (not found?) 0 (coll? v) (count v) :else 1)
          op-fn (case (:op assertion) := = :<= <= :>= >=)]
      (if (op-fn n (:value assertion))
        []
        [{:severity :error :code "count-mismatch" :locator (:locator assertion)
          :message (str "expected count " (name (:op assertion)) " " (:value assertion)
                        " at " (:path (:locator assertion)) ", found " n)
          :engine engine}]))

    :schema
    (let [{:keys [id version]} (:malli assertion)
          entry (schemas/lookup id version)]
      (cond
        (nil? entry)
        [{:severity :error :code "unknown-schema" :locator {:format :fhir :path ""}
          :message (str "no registered schema " id "@" version) :engine engine}]

        (schemas/valid-against? entry parsed-datum)
        []

        :else
        [{:severity :error :code "schema-invalid" :locator {:format :fhir :path ""}
          :message (str "datum does not validate against schema " id "@" version) :engine engine}]))

    ;; :matches-expected is handled at corpus level, never reached here
    []))

(defn- run-per-file-assertions
  [candidate-dir assertions engine]
  (mapv (fn [file]
          (let [rel (relative-path candidate-dir file)
                parsed (try (result/ok (json/read-str (slurp file)))
                            (catch Exception _ (result/error :unparseable-datum {})))]
            (if-not (result/ok? parsed)
              {:path rel :verdict :rejected
               :findings [{:severity :error :code "unparseable-datum" :locator {:format :fhir :path ""}
                           :message (str rel " could not be parsed as JSON") :engine engine}]}
              (let [findings (vec (mapcat #(assertion-findings % (:payload parsed) engine) assertions))]
                {:path rel :verdict (if (seq findings) :rejected :pass) :findings findings}))))
        (files-in candidate-dir)))

;; ---- check-corpus: the main entry point ----

(defn check-corpus
  "Checks :candidate-dir against :assertions (default: implied
  [{:kind :matches-expected}] whenever :expected-dir is given).
  :pair-by (:path default, or :hash) and :canonicalizers (an ordered
  vector of [id version] pairs, default []) govern :matches-expected
  only. Returns result/ok {report} when every file passes, or
  result/rejected :check-rejected {report} the moment any file was
  rejected -- report is a ehr-testing-tools.judge.report Report, built
  from the same {:path :verdict :findings} shape Gate's own gate-dir
  functions produce. Never writes to :candidate-dir or :expected-dir
  (the gate kind's own law: never modifies the datum it judges)."
  [{:keys [candidate-dir expected-dir assertions pair-by canonicalizers]
    :or {pair-by :path canonicalizers []}}]
  (let [assertions (or assertions (when expected-dir default-assertions) [])]
    (if-not (valid-assertions? assertions)
      (result/rejected :invalid-assertions {:assertions assertions})
      (let [engine {:name engine-name :version assertion-vocabulary-version}
            grouped (group-by #(= :matches-expected (:kind %)) assertions)
            matches-expected? (seq (get grouped true))
            per-file (get grouped false)
            me-results (when matches-expected?
                         (run-matches-expected {:candidate-dir candidate-dir :expected-dir expected-dir
                                                 :pair-by pair-by :canonicalizers canonicalizers}
                                                engine))
            per-file-results (when (seq per-file)
                                (run-per-file-assertions candidate-dir per-file engine))
            results (vec (concat me-results per-file-results))
            rpt (report/build-report results {:check {:name engine-name :version assertion-vocabulary-version}
                                                :candidate-dir (str candidate-dir)
                                                :expected-dir (some-> expected-dir str)
                                                :assertions assertions
                                                :pair-by pair-by
                                                :canonicalizers canonicalizers})]
        (if (pos? (:rejected (:totals rpt)))
          (result/rejected :check-rejected rpt)
          (result/ok rpt))))))
