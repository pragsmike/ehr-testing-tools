(ns ehrt.docs-tooling.event-log-doc
  "Renders `docs/formats.md`'s own event-log section FROM the published
  contract (event-log contract arc Step 3, author ruling 2026-08-16).

  READS THE ARTIFACT, NOT THE NAMESPACE. This namespace opens
  `components/sim-engine/resources/sim-engine/event-schema.edn` and
  `event-examples.edn` as plain files -- it does not require
  `ehrt.sim-engine.event-schema`, and docs-tooling gains no dependency
  on sim-engine. That is deliberate twice over:

  - The EDN export is what a consumer actually receives. Rendering the
    documentation from the same bytes means the page cannot describe
    something the artifact does not say -- if the export were ever
    wrong, the docs would be wrong in exactly the same way, visibly,
    rather than papering over it.
  - It is the one reading of `usecases.clj`'s own precedent that keeps
    the doc layer free of the domain layer.

  WHAT IS AUTHORED AND WHAT IS GENERATED. Everything between the
  BEGIN/END markers in `docs/formats.md` is generated and will be
  overwritten. The prose around it -- the envelope, the ordering
  guarantee, the EDN and JSON conventions, the stability policy -- is
  hand-written, because it states things no schema can carry. The
  nested-`:event` warning is generated, and leads the section by the
  author's own ruling: it is the log's sharpest edge, and it comes off
  the schema's own `PreHorizonFact` so it cannot drift from the
  vocabulary it warns about."
  (:require [clojure.edn :as edn]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def schema-path "components/sim-engine/resources/sim-engine/event-schema.edn")
(def examples-path "components/sim-engine/resources/sim-engine/event-examples.edn")
(def formats-path "docs/formats.md")

(def begin-marker "<!-- EVENT-LOG-GENERATED-BEGIN -->")
(def end-marker "<!-- EVENT-LOG-GENERATED-END -->")

;; --- describing a malli form to a reader who does not read Clojure --------

(defn describe
  "A malli form -> a short human description.

  Deliberately lossy and deliberately honest about it: the page says,
  once, that the precise machine-readable form is in
  `event-schema.edn`. A description that tried to be exact would just
  be the form again, in worse notation."
  [form]
  (cond
    (keyword? form)
    (case form
      :string "string"
      :int "integer"
      :boolean "boolean"
      :keyword "keyword"
      :any "any"
      (name form))

    (symbol? form) (str form)

    (vector? form)
    (let [[op & args] form
          args (if (map? (first args)) (rest args) args)]
      (case op
        :map (str "map with keys "
                  (str/join ", " (map #(str "`" (first %) "`")
                                      (filter vector? args))))
        :vector (str "vector of " (describe (first args)))
        :set (str "set of " (describe (first args)))
        :map-of (str "map of " (describe (first args)) " -> " (describe (second args)))
        :enum (str "one of " (str/join ", " (map #(str "`" (pr-str %) "`") args)))
        := (str "`" (pr-str (first args)) "`")
        :maybe (str (describe (first args)) ", or nil")
        :or (str/join " or " (map describe args))
        :re (str "string matching `" (first args) "`")
        (str op)))

    :else (pr-str form)))

(defn- backtick-keywords
  "Wraps bare `:keyword` tokens in the schema's own `:doc` / `:transition`
  prose in backticks. Done at render time rather than in the schema so
  those strings stay plain sentences -- they are read by anything that
  loads the EDN artifact, not only by this renderer, and Markdown
  punctuation would be noise there."
  [text]
  (str/replace text #"(?<![`\w:]):[a-z][a-z0-9-]*" #(str "`" % "`")))

(defn- entries
  "The [key props value] triples of a [:map ...] form."
  [map-form]
  (for [e (rest map-form) :when (vector? e)]
    (let [[k & more] e
          props (when (map? (first more)) (first more))]
      [k props (if props (second more) (first more))])))

(defn- props-of [map-form]
  (let [p (second map-form)] (when (map? p) p)))

(defn- branches [schema]
  (for [b (rest schema) :when (vector? b)] b))

;; --- rendering ------------------------------------------------------------

(defn- render-example [event]
  (str "```clojure\n"
       (str/trim-newline (with-out-str (pp/pprint event)))
       "\n```\n"))

(defn- render-key-table [map-form]
  (str "| Key | Required | Value |\n|---|---|---|\n"
       (str/join "\n"
                 (for [[k props value] (sort-by first (entries map-form))]
                   (format "| `%s` | %s | %s |"
                           k
                           (if (:optional props) "optional" "always")
                           (describe value))))
       "\n"))

(defn- render-kind [[kind map-form] examples]
  (let [{:keys [doc transition]} (props-of map-form)]
    (str "#### `" kind "`\n\n"
         (backtick-keywords doc) "\n\n"
         "**State transition:** " (backtick-keywords transition) "\n\n"
         (render-key-table map-form)
         "\n"
         (if-let [ex (get examples kind)]
           (render-example ex)
           "")
         "\n")))

(defn- render-nested-warning
  "The nested-`:event` hazard, LEADING the section by author ruling
  (2026-08-16). Generated off `PreHorizonFact`'s own enum in the
  schema, so the list of colliding names cannot drift from the schema
  that declares them."
  [schema examples]
  (let [reg (first (filter #(= :registered (first %)) (branches schema)))
        facts-form (some (fn [[k _ v]] (when (= k :pre-horizon-facts) v)) (entries (second reg)))
        ;; [:vector [:map ... [:event [:enum ...]] ...]]
        fact-map (second facts-form)
        fact-enum (some (fn [[k _ v]] (when (= k :event) v)) (entries fact-map))
        fact-kinds (vec (rest fact-enum))
        log-kinds (set (map first (branches schema)))
        colliding (sort (filter log-kinds fact-kinds))]
    (str "### Read the top-level vector only\n\n"
         "**This is the one thing most likely to go wrong, so it comes first.**\n\n"
         "The log is a vector of events. Some of those events carry *nested* maps that\n"
         "have an `:event` key of their own, drawn from a **different vocabulary** —\n"
         "a `:registered` event's `:pre-horizon-facts` (clinical history that predates\n"
         "the run's window) and an encounter's `:conditions`.\n\n"
         "Those nested names are: "
         (str/join ", " (map #(str "`" % "`") fact-kinds))
         ".\n\n"
         "And "
         (count colliding)
         " of them — "
         (str/join ", " (map #(str "`" % "`") colliding))
         " — **are also top-level event kinds, with entirely different keys.**\n\n"
         "So: iterate the top-level vector. Do not walk the tree looking for `:event`.\n"
         "A tree-walking consumer will find pre-horizon facts, mistake them for log\n"
         "events, and emit clinical activity that never happened during the run.\n\n"
         "Nothing here is being renamed to make the collision go away — the nested\n"
         "names are the vocabulary the trajectory layer genuinely uses. It is\n"
         "documented instead.\n\n")))

(defn render
  "schema-edn x examples-edn -> the generated markdown block."
  [{:keys [event-schema-version schema]} examples]
  (let [kinds (branches schema)]
    (str "<!-- Generated by `make docsgen` from "
         "components/sim-engine/resources/sim-engine/event-schema.edn. "
         "Do not edit between the markers. -->\n\n"
         "Event schema version: **`" event-schema-version "`** — "
         "stamped into every run's `manifest.edn` as `:event-schema-version`.\n\n"
         (render-nested-warning schema examples)
         "### The vocabulary\n\n"
         "There are exactly **" (count kinds) "** event kinds. The set is closed; a\n"
         "reader may treat an unknown `:event` value as a contract violation rather\n"
         "than as something to skip.\n\n"
         (str/join " " (map #(str "`" (first %) "`") (sort-by first kinds)))
         "\n\n"
         "Every event of every kind carries these four keys:\n\n"
         "| Key | Value | Meaning |\n|---|---|---|\n"
         "| `:event` | keyword | which kind this is — the discriminator |\n"
         "| `:t` | integer | seconds from the start of the run, not a wall-clock instant |\n"
         "| `:participants` | vector of maps | `{:patient-id :role}` — who this event is about |\n"
         "| `:warm-up` | boolean | whether `:t` fell inside the configured warm-up window |\n\n"
         "**`:active-mrn` is *not* one of them.** It is absent from `:bed-swap`,\n"
         "`:merge`, and `:step-rejected`. Partition a log by `:participants`, never by\n"
         "`:active-mrn`.\n\n"
         "### Every kind\n\n"
         "Each entry below gives the kind's meaning, which patient-state transition it\n"
         "drives, its keys, and one **real** example — every example was produced by an\n"
         "actual engine run, not hand-written.\n\n"
         (str/join (map #(render-kind % examples) (sort-by first kinds))))))

(defn splice
  "Replaces the marked block in `page` with `block`."
  [page block]
  (let [b (str/index-of page begin-marker)
        e (str/index-of page end-marker)]
    (when (or (nil? b) (nil? e))
      (throw (ex-info "docs/formats.md is missing the event-log generation markers"
                      {:begin begin-marker :end end-marker})))
    (str (subs page 0 (+ b (count begin-marker)))
         "\n\n" (str/trim-newline block) "\n\n"
         (subs page e))))

(defn write-event-log-section!
  "`make formats-event-log`."
  [_]
  (let [schema (edn/read-string (slurp schema-path))
        examples (edn/read-string (slurp examples-path))
        page (slurp formats-path)]
    (spit formats-path (splice page (render schema examples)))))
