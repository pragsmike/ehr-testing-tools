(ns ehr-testing-tools.corpus.source-sink-url
  "The URL<->map surface for Source and Sink (docs/source-sink-design.md
  Part IV, D4; ADR-0017 decision 4): a compact URL string is the
  CLI/wire projection of a canonical Source/Sink map (ehr-testing-
  tools.corpus.source-sink) -- maps are canonical, this namespace is
  the parser/printer onto them, never the other way around.

  D-a (URL scheme spellings, resolved 2026-07-28, this commit): `file:`
  (single file), `dir:` (directory tree -- a distinct scheme, no
  trailing-slash magic), `stdin:`, `synthea:`, `sim:` (no authority --
  scheme, colon, then an optional path and/or query string) and
  `blaze://host:port/path?query=...` (the one scheme with an
  authority, since it names a network endpoint). Format/framing ride
  as query params (`?format=v2-er7&framing=er7-multi`) on every
  scheme, including blaze's.

  Ruling 3 (SS-1): the grammar above is fixed for all six source
  schemes now (`source-schemes`) and all four sink schemes
  (`sink-schemes`) -- growing `ehr-testing-tools.corpus.source-sink`'s
  own `implemented-source-kinds`/`implemented-sink-kinds` later (SS-2's
  generators, SS-3's stdin, SS-5's blaze) never requires touching this
  grammar again. Only `dir:`/`file:` have real constructors this
  session (ehr-testing-tools.corpus.source-sink); every other
  recognized scheme parses far enough to name its own :kind, then is
  rejected :unsupported-source-kind / :unsupported-sink-kind -- never
  silently accepted, never confused with an actually-unknown scheme
  (:unknown-source-scheme / :unknown-sink-scheme).

  :path is used literally, never percent-encoded/decoded -- matching
  the design's own unencoded example (\"dir:./corpus\"); only query
  *values* (format, framing, and any other kind-specific query param)
  are percent-decoded on parse and percent-encoded on print, since
  those ride through '&'/'='-delimited query syntax where encoding is
  load-bearing."
  (:require [clojure.string :as str]
            [ehr-testing-tools.corpus.source-sink :as ss]
            [ehr-testing-tools.result :as result])
  (:import [java.net URLDecoder URLEncoder]))

(def source-schemes
  "scheme string -> :kind, the six spellings D-a fixes."
  {"file" :file "dir" :dir "stdin" :stdin "synthea" :synthea "sim" :sim "blaze" :blaze})

(def sink-schemes
  "scheme string -> :kind, the four sink spellings (D3): dir/file share
  source's spellings, stdout is sink-only, blaze is shared."
  {"file" :file "dir" :dir "stdout" :stdout "blaze" :blaze})

(defn- url-decode [^String s] (URLDecoder/decode s "UTF-8"))
(defn- url-encode [^String s] (URLEncoder/encode s "UTF-8"))

(defn- has-whitespace? [^String s] (boolean (re-find #"\s" s)))

(defn- split-scheme
  "\"dir:./corpus?format=v2-er7\" -> [\"dir\" \"./corpus?format=v2-er7\"],
  or nil when there's no scheme colon at all (not a designator)."
  [^String s]
  (let [idx (str/index-of s ":")]
    (when (and idx (pos? idx))
      [(subs s 0 idx) (subs s (inc idx))])))

(defn- split-path-query
  [^String s]
  (let [idx (str/index-of s "?")]
    (if idx
      [(subs s 0 idx) (subs s (inc idx))]
      [s nil])))

(defn- parse-query
  "\"a=1&b=2\" -> {:a \"1\" :b \"2\"}, percent-decoded. Blank/nil -> {}."
  [s]
  (if (str/blank? s)
    {}
    (into {}
          (map (fn [pair]
                 (let [[k v] (str/split pair #"=" 2)]
                   [(keyword (url-decode k)) (url-decode (or v ""))])))
          (str/split s #"&"))))

(defn- extract-format-framing
  "query (a keyword-keyed string-valued map from parse-query) -> a map
  with :format/:framing coerced to keywords when present, plus every
  other query key passed through as a kind-specific field (e.g. :seed,
  :query) -- the general shape docs/source-sink-design.md Part IV
  names, only :dir/:file actually consume any of it this session."
  [query]
  (cond-> (dissoc query :format :framing)
    (:format query) (assoc :format (keyword (:format query)))
    (:framing query) (assoc :framing (keyword (:framing query)))))

(defn- parse-plain
  "scheme:[path][?query] -- every non-blaze scheme."
  [kind rest]
  (let [[path query-str] (split-path-query rest)]
    (cond-> (merge {:kind kind} (extract-format-framing (parse-query query-str)))
      (seq path) (assoc :path path))))

(defn- parse-blaze
  "blaze://host[:port][/path][?query] -- the one scheme with an
  authority, since it names a network endpoint. nil when the input
  after \"blaze:\" doesn't start with \"//\" (malformed)."
  [rest]
  (when (str/starts-with? rest "//")
    (let [[authority-and-path query-str] (split-path-query (subs rest 2))
          [host-port path] (let [i (str/index-of authority-and-path "/")]
                              (if i
                                [(subs authority-and-path 0 i) (subs authority-and-path i)]
                                [authority-and-path nil]))
          [host port] (let [i (str/index-of host-port ":")]
                        (if i
                          [(subs host-port 0 i) (subs host-port (inc i))]
                          [host-port nil]))]
      (cond-> (merge {:kind :blaze :host host} (extract-format-framing (parse-query query-str)))
        port (assoc :port port)
        (seq path) (assoc :path path)))))

(defn- finish-source
  [kind m]
  (case kind
    :dir (ss/dir-source m)
    :file (ss/file-source m)))

(defn- finish-sink
  [kind m]
  (case kind
    :dir (ss/dir-sink m)
    :file (ss/file-sink m)))

(defn- parse-designator
  "Shared parse skeleton for parse-source-designator/parse-sink-
  designator: differ only in which scheme table, implemented-kind set,
  finisher, and error-category prefix they use."
  [url schemes implemented-kinds finish unknown-scheme-category unsupported-kind-category malformed-category]
  (cond
    (has-whitespace? url)
    (result/rejected malformed-category {:url url :hint "a source/sink designator may not contain whitespace"})

    :else
    (if-let [[scheme rest] (split-scheme url)]
      (if-let [kind (get schemes scheme)]
        (let [m (if (= kind :blaze) (parse-blaze rest) (parse-plain kind rest))]
          (if (nil? m)
            (result/rejected malformed-category {:url url :hint "blaze: requires blaze://host..."})
            (if (contains? implemented-kinds kind)
              (finish kind m)
              (result/rejected unsupported-kind-category
                                {:kind kind :url url
                                 :hint (str "kind " (name kind) " is recognized (D-a) but not yet supported -- a later SS-1..SS-5 session lands it")}))))
        (result/rejected unknown-scheme-category
                          {:url url :scheme scheme :valid-options (sort (keys schemes))}))
      (result/rejected malformed-category
                        {:url url :hint "expected scheme:... e.g. dir:./corpus"}))))

(defn parse-source-designator
  "Parses a Source URL string (e.g. \"dir:./corpus?format=v2-er7\") into
  a canonical Source map. Returns result/ok the map (already validated
  through ehr-testing-tools.corpus.source-sink's own dir-source/file-
  source constructors for the two implemented kinds), or
  result/rejected :malformed-source-designator, :unknown-source-scheme,
  :unsupported-source-kind, or (propagated from the constructor)
  :invalid-source."
  [url]
  (parse-designator url source-schemes ss/implemented-source-kinds finish-source
                     :unknown-source-scheme :unsupported-source-kind :malformed-source-designator))

(defn parse-sink-designator
  "Sink twin of parse-source-designator."
  [url]
  (parse-designator url sink-schemes ss/implemented-sink-kinds finish-sink
                     :unknown-sink-scheme :unsupported-sink-kind :malformed-sink-designator))

(defn- print-designator
  "Shared print skeleton: renders {:kind :path :format :framing} for an
  implemented dir/file kind into \"kind:path?format=...&framing=...\".
  format/framing are omitted from the query string when absent (round-
  trip identity requires the printed form to omit exactly what was
  absent, not print an empty placeholder)."
  [m implemented-kinds valid? unsupported-kind-category invalid-category]
  (let [{:keys [kind path format framing]} m]
    (cond
      (not (valid? m))
      (result/rejected invalid-category {:map m})

      (not (contains? implemented-kinds kind))
      (result/rejected unsupported-kind-category {:kind kind :map m})

      :else
      (let [query-pairs (cond-> []
                           format (conj (str "format=" (url-encode (name format))))
                           framing (conj (str "framing=" (url-encode (name framing)))))
            query-str (when (seq query-pairs) (str "?" (str/join "&" query-pairs)))]
        (result/ok (str (name kind) ":" (or path "") query-str))))))

(defn print-source-designator
  "Renders a canonical Source map back to its URL string. Only :dir/
  :file (the implemented kinds) are printable this session; any other
  kind, or a map that doesn't validate, is result/rejected."
  [m]
  (print-designator m ss/implemented-source-kinds ss/valid-source?
                     :unsupported-source-kind :invalid-source))

(defn print-sink-designator
  "Sink twin of print-source-designator."
  [m]
  (print-designator m ss/implemented-sink-kinds ss/valid-sink?
                     :unsupported-sink-kind :invalid-sink))

(defn path-designator->path
  "CLI-boundary sugar (ruling 7, SS-1 Step 6): for a positional PATH or
  a --out-dir/--out value, accepts a dir:/file: URL designator
  alongside the documented bare-path spelling. If s starts with a
  recognized \"dir\" or \"file\" scheme, returns just its :path
  component (any query string, e.g. ?format=..., is ignored -- these
  CLI flags never needed a :format/:framing before URL acceptance
  landed, and still don't). Any other string -- a bare path, or any of
  the other four recognized schemes, which aren't file-path-shaped
  arguments anyway -- passes through unchanged: this function only
  ever WIDENS what's accepted, never rejects a string a bare-path
  caller could already pass. A Windows absolute path (\"C:\\...\") is
  never mistaken for a scheme -- only the literal \"dir\"/\"file\"
  scheme names trigger the URL reading, and \"C\" is neither."
  [s]
  (if-let [[scheme rest] (split-scheme s)]
    (if (#{"dir" "file"} scheme)
      (let [[path _query] (split-path-query rest)]
        (if (seq path) path s))
      s)
    s))
