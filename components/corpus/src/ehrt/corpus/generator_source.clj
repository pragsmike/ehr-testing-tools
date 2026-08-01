(ns ehrt.corpus.generator-source
  "The unification (D1, docs/source-sink-design.md Part I.2; SS-2 Step
  2): a generator source resolves to a dir Source by validating its
  params (ehrt.corpus.generators), deriving a stable
  out-dir, executing the registered engine, and verifying the
  directory materialized non-empty -- exactly the shape corpus.generate
  already produces for synthea, generalized to every registered kind.
  Result-valued throughout; three distinct rejections, not one:

  - a pre-existing, non-empty out-dir (:out-dir-exists) -- checked
    BEFORE the engine ever runs, so a caller never pays for (or
    silently no-ops through) a second run into the first run's own
    directory. Owned HERE, uniformly, rather than left to each
    registered kind to reimplement for itself (corpus.generate's own
    :out-dir-exists guard stays in place too, for its own direct
    `ehr corpus generate` call path, unchanged by this session --
    ruling 6);
  - the engine's own failure Result, propagated unchanged
    (:execute-fn's own category, whatever it is);
  - a successful execute-fn that nonetheless left the out-dir empty
    (:generator-produced-no-output) -- an engine claiming success
    while writing nothing is caught here, not silently accepted as a
    valid (empty) corpus.

  Also carries `generator-source` (corpus-io stage 2, 2026-07-31,
  relocated whole from ehrt.corpus-io.source-sink): the
  *validate+shape-only* generator Source constructor, as opposed to
  this namespace's own `resolve!` which additionally *executes* the
  engine. Both need the domain's generators registry
  (ehrt.corpus.generators/resolve-params), which is why neither
  can live in corpus-io -- that component may never depend on this one
  (ADR-0017 stage 2's directional rule). `generator-source` is called
  from this namespace's own `finish-source` (below), which stayed
  behind alongside it for the same reason (the URL surface's other,
  non-generator schemes moved to ehrt.corpus-io.source-sink-url)."
  (:require [clojure.java.io :as io]
            [ehrt.corpus.generators :as generators]
            [ehrt.corpus-io.interface :as corpus-io]
            [ehrt.kernel.interface :as kernel])
  (:import [java.io File]))

(defn generator-source
  "Constructs+validates a canonical generator Source map for a
  registered :kind (:synthea/:sim, SS-2 Step 4) -- params are resolved
  against ehrt.corpus.generators's own registry (merged
  onto that kind's pinned defaults, D8, and validated against its own
  params-schema), so a zero-param URL means exactly what that kind's
  own zero-flag invocation means. This constructor only validates and
  shapes the Source value; it never executes the generator itself
  (this namespace's own `resolve!` does that, later, when intake
  actually needs bytes). Returns kernel/ok the shaped map, or
  generators/resolve-params's own :unknown-generator-kind /
  :invalid-generator-params, propagated unchanged."
  [kind params]
  (let [params-result (generators/resolve-params kind params)]
    (if-not (kernel/ok? params-result)
      params-result
      (kernel/ok (assoc (:payload params-result) :kind kind)))))

(defn- non-empty-existing-dir?
  [dir]
  (let [f (io/file dir)]
    (and (.isDirectory f) (seq (.listFiles f)))))

(defn resolve!
  "kind -- a registered generator :kind (ehrt.corpus.
  generators); params -- the caller-supplied, kind-specific params
  (merged onto the registry's own pinned defaults, D8, by
  generators/resolve-params). Returns kernel/ok a canonical :dir
  Source over the freshly generated corpus, or one of the three
  rejections above, or generators/resolve-params's own
  :unknown-generator-kind / :invalid-generator-params, propagated
  unchanged."
  [kind params]
  (let [params-result (generators/resolve-params kind params)]
    (if-not (kernel/ok? params-result)
      params-result
      (let [merged-params (:payload params-result)
            entry (generators/lookup kind)
            out-dir ((:out-dir-fn entry) merged-params)]
        (if (non-empty-existing-dir? out-dir)
          (kernel/error :out-dir-exists
                        {:kind kind :out-dir out-dir
                         :hint (str "same params always derive the same out-dir, so this run refused to silently overwrite the last one -- "
                                    "run `rm -rf " out-dir "` to regenerate in place, "
                                    "or pass different params (e.g. a different seed) to keep this run and start a new one")})
          (let [execute-result ((:execute-fn entry) merged-params out-dir)]
            (if-not (kernel/ok? execute-result)
              execute-result
              (if-not (non-empty-existing-dir? out-dir)
                (kernel/error :generator-produced-no-output {:kind kind :out-dir out-dir})
                (corpus-io/dir-source {:path out-dir})))))))))

(def ^:private generator-int-query-keys
  "Query-string params that must coerce string -> int before reaching
  ehrt.corpus.generators's own params-schema (which
  validates real ints, matching every non-URL caller -- e.g. the
  hermetic tests calling generators/resolve-params directly with
  already-typed maps)."
  #{:seed :clinician-seed :population :patients})

(def ^:private generator-bool-query-keys
  #{:churn})

(defn- parse-long-safely
  "s -> a Long, or s unchanged if it isn't parseable as one -- never
  throws (ADR-0004: a malformed external value is the params-schema's
  own :invalid-generator-params rejection, not an uncaught exception
  thrown from this coercion step)."
  [s]
  (try (Long/parseLong s) (catch NumberFormatException _ s)))

(defn- coerce-generator-query
  "Generator-kind query params arrive from parse-query as ALL-string
  values (only :format/:framing are coerced upstream, by
  ehrt.corpus-io.source-sink-url's own extract-format-framing) --
  this widens that coercion to the numeric/boolean generator params
  every registered kind's own params-schema expects typed."
  [m]
  (as-> m m
    (reduce (fn [acc k] (if (contains? acc k) (update acc k parse-long-safely) acc))
            m generator-int-query-keys)
    (reduce (fn [acc k] (if (contains? acc k) (update acc k #(= "true" %)) acc))
            m generator-bool-query-keys)))

(defn- finish-source
  "corpus-io stage 2 (2026-07-31): relocated whole from
  ehrt.corpus-io.source-sink-url, the one case arm this project's
  URL-parsing surface can't hand off to corpus-io -- the generator
  kinds need this namespace's own generator-source, above. :dir/:file/
  :stdin are otherwise-pure corpus-io constructors, called here only
  because parse-designator (below) takes one finisher for the whole
  scheme table, not because they need anything domain-side."
  [kind m]
  (case kind
    :dir (corpus-io/dir-source m)
    :file (corpus-io/file-source m)
    :stdin (corpus-io/stdin-source m)
    (:synthea :sim) (generator-source kind (coerce-generator-query (dissoc m :kind)))))

(defn parse-source-designator
  "Parses a Source URL string (e.g. \"dir:./corpus?format=v2-er7\") into
  a canonical Source map. Relocated whole from
  ehrt.corpus-io.source-sink-url (corpus-io stage 2, 2026-07-31):
  the only source-parsing entry point with a real edge into the
  domain's generator registry (via finish-source's generator-kind
  branch, above), so it stayed in tools alongside generator-source and
  resolve!, calling corpus-io's own re-exported parse-designator as
  the shared grammar skeleton (source-schemes, implemented-source-
  kinds also re-exported from there). Returns kernel/ok the map
  (already validated through corpus-io's own dir-source/file-source/
  stdin-source constructors, or this namespace's own generator-source,
  for every implemented kind), or kernel/rejected
  :malformed-source-designator, :unknown-source-scheme,
  :unsupported-source-kind, or (propagated from the constructor)
  :invalid-source."
  [url]
  (corpus-io/parse-designator url corpus-io/source-schemes corpus-io/implemented-source-kinds finish-source
                               :unknown-source-scheme :unsupported-source-kind :malformed-source-designator))
