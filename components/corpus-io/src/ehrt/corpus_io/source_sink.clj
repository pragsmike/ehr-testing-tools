(ns ehrt.corpus-io.source-sink
  "Formal Source and Sink types (ADR-0017, docs/source-sink-design.md
  Parts I-IV): the canonical Clojure-map shape every corpus input/output
  in this repo will eventually pass through -- generators (synthea, sim,
  simhospital) and readers (dir, file, stdin, blaze) unify to Source;
  dir, file, stdout, blaze unify to Sink. SS-1 (this namespace's first
  build session, `.agents/plans/corpus-foundations.md`) lands the
  canonical-map schemas and the dir/file kinds only (ruling 8's scope
  fence); the other four kinds are named in `known-source-kinds`/
  `known-sink-kinds` (the design's own set) but have no constructor
  here yet -- ehrt.corpus-io.source-sink-url (Step 3) is the
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
            [ehrt.kernel.interface :as kernel]))

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
  generator kinds, via `ehrt.corpus.generator-source/
  generator-source` -- corpus-io stage 2, 2026-07-31: the generator-
  kind Source constructor stayed behind in `tools`, the only piece of
  this namespace with a real edge into the domain's generator
  registry, backed by the registry in ehrt.corpus.generators);
  SS-3 Step 6 adds :stdin (`stdin-source` below -- resolved to a real
  :dir Source via ehrt.corpus-io.spool-source, never executed here).
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
  "Every sink kind the design names (Part III, D3), plus `:mllp` (arc 4
  sweep 5, ADR-0175 design (g)) -- the first sink kind in this
  namespace that is not a filesystem location or a byte stream but a
  SOCKET. See `MllpSink` below for the kind/framing name collision and
  the one sentence that pays for it."
  #{:dir :file :stdout :blaze :mllp})

(def Framing
  "The five framing kinds Part II names (D2), as a closed enum -- SS-1
  left :framing an open :keyword since no framing codec existed yet to
  dispatch on an unrecognized value; SS-3's ehrt.corpus-io.
  framing gives every kind here a real codec, so a Source/Sink
  declaring anything else is invalid at construction, not a silent
  pass-through that fails later at decode/encode time."
  [:enum :file-per-item :er7-multi :ndjson :bundle-entries :mllp])

(def default-framing
  "The design's own stated default (Part II) for a Source/Sink that
  declares no :framing at all: :file-per-item, the identity framing --
  one file, one item. A named constant for framing-aware callers to
  consult (ehrt.corpus-io.framing's dispatch, the spool's
  framed-file? check, SS-3) -- deliberately NOT injected into a
  constructed Source/Sink map by the builders below, so an absent
  :framing stays absent and the D4 round-trip law (parse ∘ print =
  identity on canonical maps) is unaffected by this default existing."
  :file-per-item)

(def implemented-sink-kinds
  "Kinds with an actual constructor. SS-1 built :dir/:file; SS-4 Step 3
  adds :stdout (`stdout-sink` below -- no :path, no manifest sidecar,
  the byte-stream form of the composability law, docs/source-sink-
  design.md Part III). :blaze remains parser-recognized (D-a) but
  rejected as not-yet-supported until SS-5 (D-b).

  Export-for-symmetry, 2026-08-05 (alignment fixes 1, ADR-0050, register
  row B-1): unlike its sibling `implemented-source-kinds`, this var has
  no external caller today -- corpus's own sink-designator path doesn't
  exist yet. Kept exported anyway, mirroring the source side exactly,
  so the player's own sink slice has a ready-made answer to consume
  once the sink-designator path lands, rather than a var to invent from
  scratch.

  ARC 4 SWEEP 5 (ADR-0175 design (g)): `:mllp` joins, and it is the
  first kind here with a real lifecycle -- ADR-0014's own assessment,
  which still stands and is quoted at whoever reads this, is that
  building it \"was found to cross three namespace boundaries rather
  than one\" and that \"a half-built network sink with no ACK handling
  and untested lifecycle is a worse outcome than a clearly named
  deferral\". `ehrt.corpus-io.mllp` is what pays that: the framing codec
  is REUSED rather than rewritten (a no-drift gate names the
  functions), ACK pairing is a stated positional law, and every failure
  mode -- timeout, negative acknowledgement, unrecognized code, stream
  close, refused connection -- has its own named error and its own
  test."
  #{:dir :file :stdout :mllp})

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
  (ehrt.corpus-io.spool-source, SS-3 Step 6, is what actually
  reads and spools them; this schema only shapes+validates the
  declaration)."
  [:and Source [:map [:kind [:= :stdin]]]])
(def DirSink [:and Sink [:map [:kind [:= :dir]] [:path :string]]])
(def FileSink [:and Sink [:map [:kind [:= :file]] [:path :string]]])
(def StdoutSink
  "No :path -- stdout names no filesystem location, the sink-side twin
  of StdinSource above. :format is still required (Sink's own base
  schema, D3's no-inference-on-write law); :framing defaults to
  default-framing when absent, same as every other Source/Sink."
  [:and Sink [:map [:kind [:= :stdout]]]])

(def MllpSink
  "ARC 4 SWEEP 5 (ADR-0175 design (g)): the socket sink.

  THE NAME COLLISION, RESOLVED EXPLICITLY AND HERE. `:mllp` as a
  `:framing` means \"these bytes are VT/FS-CR framed\"; `:mllp` as a
  `:kind` means \"send these to a socket\". They live in different
  fields of the same map and never collide mechanically, but they WILL
  collide in a reader's head -- so the sentence that pays for the
  collision is this one: A `:mllp` SINK IMPLIES `:framing :mllp`, and
  declaring any other framing on it is a CONSTRUCTION-TIME ERROR, never
  a silent override. That is what `[:= :mllp]` below buys, and
  `mllp-sink-implies-mllp-framing` is the gate.

  `:host` and `:port` rather than `:path`: this kind names a network
  endpoint, the same shape `blaze://host:port` already parses. `:port`
  is an INT here (blaze's parser leaves its port a string, which is
  fine for a URL it only ever re-prints) because a socket constructor
  needs a number, so the designator parser coerces and rejects a
  non-numeric port by name."
  [:and Sink [:map
              [:kind [:= :mllp]]
              [:host :string]
              [:port :int]
              [:framing {:optional true} [:= :mllp]]]])

(defn valid-source?
  [m]
  (m/validate Source m))

(defn valid-sink?
  [m]
  (m/validate Sink m))

(defn- build
  "Shared constructor shape for every kind-specific builder below:
  merges the given, non-nil kind-specific fields onto {:kind kind},
  validates against schema, and returns kernel/ok the canonical map or
  kernel/rejected :invalid-source / :invalid-sink naming what didn't
  conform (ADR-0004: a bad constructor call is an operational
  rejection, never a thrown exception)."
  [error-category kind schema base-fields]
  (let [m (into {:kind kind}
                (filter (fn [[_ v]] (some? v)))
                base-fields)]
    (if (m/validate schema m)
      (kernel/ok m)
      (kernel/rejected error-category {:kind kind :explain (m/explain schema m)}))))

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

(defn stdout-sink
  "Constructs+validates a canonical :stdout Sink map (SS-4 Step 3). No
  :path; :format is required (D3), :framing optional (defaults to
  default-framing at the point something actually encodes, e.g.
  ehrt.corpus-io.sink-write/write-stdout! -- not injected
  here, same discipline as stdin-source above)."
  [{:keys [format framing]}]
  (build :invalid-sink :stdout StdoutSink {:format format :framing framing}))

(defn mllp-sink
  "Constructs+validates a canonical `:mllp` Sink map (arc 4 sweep 5,
  ADR-0175 design (g)). `:host`, `:port` and `:format` are all required
  -- D3's no-inference-on-write law, and a socket with no endpoint is
  not a sink. `:framing` is optional and, when present, must be
  `:mllp`: the IMPLICATION RULE, enforced by `MllpSink` rather than
  applied silently, so a caller who writes `?framing=er7-multi` on an
  `mllp://` designator learns that at construction rather than by
  finding unframed bytes on a wire."
  [{:keys [host port format framing]}]
  (build :invalid-sink :mllp MllpSink {:host host :port port :format format :framing framing}))
