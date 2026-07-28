(ns ehr-testing-tools.corpus.source-sink
  "Formal Source and Sink types (ADR-0017, docs/source-sink-design.md
  Parts I-IV): the canonical Clojure-map shape every corpus input/output
  in this repo will eventually pass through -- generators (synthea, sim,
  simhospital) and readers (dir, file, stdin, blaze) unify to Source;
  dir, file, stdout, blaze unify to Sink. SS-1 (this namespace's first
  build session, `.agents/plans/corpus-foundations.md`) lands the
  canonical-map schemas and the dir/file kinds only (ruling 8's scope
  fence); the other four kinds are named in `known-source-kinds`/
  `known-sink-kinds` (the design's own set) but have no constructor
  here yet -- ehr-testing-tools.corpus.source-sink-url (Step 3) is the
  URL surface that recognizes all six schemes and rejects the
  unimplemented ones by name, not silently.

  :kind is deliberately a plain keyword, not a closed Malli enum: the
  open-set extensibility D4 calls for means a new kind (SS-2's
  generator registry, SS-5's blaze) is a new constructor plus a parser
  branch, never a widening edit to the schemas below. `known-source-
  kinds`/`implemented-source-kinds` (and their sink twins) are this
  namespace's own bookkeeping of what the wider design names versus
  what has an actual constructor -- not a runtime registry; a
  generator registry proper (shaped like corpus.operators's) is SS-2
  (D7)."
  (:require [malli.core :as m]
            [ehr-testing-tools.corpus.generators :as generators]
            [ehr-testing-tools.result :as result]))

(def known-source-kinds
  "Every source kind the design names (Part I.1, D1): two generators
  (:synthea, already built as corpus.generate; :sim, subprocess-only
  per ADR-0013) and three readers. :simhospital is deliberately absent
  -- D5 makes the registry slot itself the entire accommodation for
  it, not a keyword this namespace should recognize before SS-2 gives
  it one."
  #{:dir :file :stdin :blaze :synthea :sim})

(def implemented-source-kinds
  "Kinds with an actual constructor. SS-1 built :dir/:file (the two
  reader kinds with no engine, D1's 'no per-source adapters'
  unification target); SS-2 Step 4 adds :synthea/:sim (the two
  generator kinds, via `generator-source` below, backed by the
  registry in ehr-testing-tools.corpus.generators); SS-3 Step 6 adds
  :stdin (`stdin-source` below -- resolved to a real :dir Source via
  ehr-testing-tools.corpus.spool-source, never executed here).
  `printable-source-kinds` below stays narrower: no session yet builds
  a printer for anything but :dir/:file (ruling 6, docs/source-sink-
  design.md -- generator and stdin URLs are parsed and consumed, never
  printed back out). :blaze remains parser-recognized (D-a) but
  rejected as not-yet-supported until SS-5."
  #{:dir :file :synthea :sim :stdin})

(def printable-source-kinds
  "The subset of implemented-source-kinds print-source-designator
  actually knows how to render -- :dir/:file only. A generator Source's
  own fields (:seed, :population, ...) have no query-param renderer
  built this session; printing one stays :unsupported-source-kind
  rather than silently producing a lossy or wrong URL."
  #{:dir :file})

(def known-sink-kinds
  "Every sink kind the design names (Part III, D3)."
  #{:dir :file :stdout :blaze})

(def Framing
  "The five framing kinds Part II names (D2), as a closed enum -- SS-1
  left :framing an open :keyword since no framing codec existed yet to
  dispatch on an unrecognized value; SS-3's ehr-testing-tools.corpus.
  framing gives every kind here a real codec, so a Source/Sink
  declaring anything else is invalid at construction, not a silent
  pass-through that fails later at decode/encode time."
  [:enum :file-per-item :er7-multi :ndjson :bundle-entries :mllp])

(def default-framing
  "The design's own stated default (Part II) for a Source/Sink that
  declares no :framing at all: :file-per-item, the identity framing --
  one file, one item. A named constant for framing-aware callers to
  consult (ehr-testing-tools.corpus.framing's dispatch, the spool's
  framed-file? check, SS-3) -- deliberately NOT injected into a
  constructed Source/Sink map by the builders below, so an absent
  :framing stays absent and the D4 round-trip law (parse ∘ print =
  identity on canonical maps) is unaffected by this default existing."
  :file-per-item)

(def implemented-sink-kinds
  "Kinds SS-1 actually built a constructor for."
  #{:dir :file})

(def Source
  "The canonical Source map's well-known fields (Part IV, D4): :kind is
  open (any keyword validates here; known-source-kinds documents the
  design's own named set, not a schema constraint), :format/:framing
  are optional since sources may infer (Part IV). Everything else is
  kind-specific and passes through unvalidated at this general level
  -- Malli's default :map is open (extra keys allowed), and a specific
  kind schema (DirSource, FileSource) validates its own required
  fields."
  [:map
   [:kind :keyword]
   [:format {:optional true} :keyword]
   [:framing {:optional true} Framing]])

(def Sink
  "The canonical Sink map's well-known fields (Part III/IV). Unlike
  Source, :format is not optional here -- D3's no-inference-on-write
  law means a sink always declares its own format explicitly; a
  kind-specific schema may still narrow further (DirSink/FileSink also
  require :path)."
  [:map
   [:kind :keyword]
   [:format :keyword]
   [:framing {:optional true} Framing]])

(def DirSource [:and Source [:map [:kind [:= :dir]] [:path :string]]])
(def FileSource [:and Source [:map [:kind [:= :file]] [:path :string]]])
(def StdinSource
  "No :path -- stdin names no filesystem location. :format/:framing
  are how a caller declares what the piped bytes actually are
  (ehr-testing-tools.corpus.spool-source, SS-3 Step 6, is what actually
  reads and spools them; this schema only shapes+validates the
  declaration)."
  [:and Source [:map [:kind [:= :stdin]]]])
(def DirSink [:and Sink [:map [:kind [:= :dir]] [:path :string]]])
(def FileSink [:and Sink [:map [:kind [:= :file]] [:path :string]]])

(defn valid-source?
  [m]
  (m/validate Source m))

(defn valid-sink?
  [m]
  (m/validate Sink m))

(defn- build
  "Shared constructor shape for every kind-specific builder below:
  merges the given, non-nil kind-specific fields onto {:kind kind},
  validates against schema, and returns result/ok the canonical map or
  result/rejected :invalid-source / :invalid-sink naming what didn't
  conform (ADR-0004: a bad constructor call is an operational
  rejection, never a thrown exception)."
  [error-category kind schema base-fields]
  (let [m (into {:kind kind}
                (filter (fn [[_ v]] (some? v)))
                base-fields)]
    (if (m/validate schema m)
      (result/ok m)
      (result/rejected error-category {:kind kind :explain (m/explain schema m)}))))

(defn dir-source
  "Constructs+validates a canonical :dir Source map. :path is required;
  :format/:framing are optional (sources may infer -- format inference
  itself is corpus.intake/sniff-format's own concern, not built by
  this constructor)."
  [{:keys [path format framing]}]
  (build :invalid-source :dir DirSource {:path path :format format :framing framing}))

(defn file-source
  "Like dir-source, for a single-file :file Source."
  [{:keys [path format framing]}]
  (build :invalid-source :file FileSource {:path path :format format :framing framing}))

(defn stdin-source
  "Constructs+validates a canonical :stdin Source map. No required
  fields at all -- a bare `stdin:` is valid, meaning file-per-item over
  whatever bytes arrive (the schema default, source-sink/default-
  framing); :format/:framing are how a caller declares real framing."
  [{:keys [format framing]}]
  (build :invalid-source :stdin StdinSource {:format format :framing framing}))

(defn dir-sink
  "Constructs+validates a canonical :dir Sink map. :path and :format
  are both required -- D3's no-inference-on-write law means a sink
  never leaves :format for a reader to guess."
  [{:keys [path format framing]}]
  (build :invalid-sink :dir DirSink {:path path :format format :framing framing}))

(defn file-sink
  "Like dir-sink, for a single-file :file Sink."
  [{:keys [path format framing]}]
  (build :invalid-sink :file FileSink {:path path :format format :framing framing}))

(defn generator-source
  "Constructs+validates a canonical generator Source map for a
  registered :kind (:synthea/:sim, SS-2 Step 4) -- params are resolved
  against ehr-testing-tools.corpus.generators's own registry (merged
  onto that kind's pinned defaults, D8, and validated against its own
  params-schema), so a zero-param URL means exactly what that kind's
  own zero-flag invocation means. This constructor only validates and
  shapes the Source value; it never executes the generator itself
  (ehr-testing-tools.corpus.generator-source/resolve! does that, later,
  when intake actually needs bytes). Returns result/ok the shaped map,
  or generators/resolve-params's own :unknown-generator-kind /
  :invalid-generator-params, propagated unchanged."
  [kind params]
  (let [params-result (generators/resolve-params kind params)]
    (if-not (result/ok? params-result)
      params-result
      (result/ok (assoc (:payload params-result) :kind kind)))))
