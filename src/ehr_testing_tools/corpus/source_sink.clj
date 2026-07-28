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
  "Kinds SS-1 actually built a constructor for -- the two reader kinds
  with no engine (D1's 'no per-source adapters' unification target).
  The remaining known kinds are parser-recognized (D-a) but rejected
  as not-yet-supported (Step 3's ruling) until their own build
  session."
  #{:dir :file})

(def known-sink-kinds
  "Every sink kind the design names (Part III, D3)."
  #{:dir :file :stdout :blaze})

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
   [:framing {:optional true} :keyword]])

(def Sink
  "The canonical Sink map's well-known fields (Part III/IV). Unlike
  Source, :format is not optional here -- D3's no-inference-on-write
  law means a sink always declares its own format explicitly; a
  kind-specific schema may still narrow further (DirSink/FileSink also
  require :path)."
  [:map
   [:kind :keyword]
   [:format :keyword]
   [:framing {:optional true} :keyword]])

(def DirSource [:and Source [:map [:kind [:= :dir]] [:path :string]]])
(def FileSource [:and Source [:map [:kind [:= :file]] [:path :string]]])
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
