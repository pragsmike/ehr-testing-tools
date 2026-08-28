(ns ehrt.judge.sampling
  "ARC 4 SWEEP 2 (`notes/adr/0175-arc-4-emission-add-ons.md` design (h),
  ruling D1): gating at scale, as a pure selection over corpus
  metadata.

  WHY A POLICY IS NEEDED AT ALL. ADR-0175 section 2(h) measured `ehrt
  gate v2` at ~5.3 ms/message marginal (~189 messages/second) on a warm
  JVM; at 10^6 messages that is ~88 minutes for the base-structural
  tier alone. Ruling D2 -- gate everything, always -- was declined for
  the honest reason that it will simply not be run.

  THE POLICY, and each clause is a rule this namespace implements:

  * FULL WIDTH on skeleton-kind messages. Every message whose MSH-9 is
    one the arcs 1-3 contract produces is gated, always.
  * STRATIFIED SAMPLING on add-on messages: one stratum per MSH-9, and
    `min(n, cap)` gated per stratum. Ruling D3 -- a uniform sample
    across the whole corpus -- was declined because at the probe
    corpus's mix a uniform 5% sample draws ~19 ADT^A20s and, in
    expectation, less than one ADT^A13: stratification is what keeps a
    rare family from vanishing.
  * THE SAMPLE IS DERIVED, NOT DRAWN. Each stratum is ordered by MSH-10
    -- a total order over a value this project mints itself -- and the
    first `cap` are taken. No RNG, no seed to thread, no
    `rulings.md#R-no-derivation-through-nondeterminism` exposure, and
    any reader can recompute the selection from the corpus alone.
  * NO SILENT CAPS. `:strata` reports `n` and `:gated` for every
    stratum, so a truncation is a printed number rather than something
    a reader has to infer from a total that looks like full coverage.

  UNCLASSIFIABLE IS SKELETON, deliberately: a file whose first line is
  not an MSH, or whose MSH-9 cannot be read, lands in the `unknown`
  stratum and is gated in FULL. A sampler that quietly dropped what it
  could not parse would hide exactly the damage the gate exists to
  find.

  This namespace knows no HL7 structure library and no emitter: it
  takes entries a caller has already read, so the classification set is
  the CALLER's (`bases/cli` passes the emitter's own registry). That is
  what keeps `components/judge` free of a dependency on
  `components/sim-emit-hl7`."
  (:require [clojure.string :as str]))

(def unknown-stratum
  "The stratum for a message whose MSH-9 could not be read. Gated in
  full, never sampled."
  "unknown")

(defn header
  "The first line of an ER7 message -> `{:msh-9 :msh-10}`, or nil when
  the content does not start with an MSH segment. Field 9 is MSH-9 and
  field 10 is MSH-10 counting the way HL7 does -- MSH-1 IS the field
  separator, so a pipe-split of the MSH line puts MSH-n at index n-1.

  No HL7 parser is used, and that is a scale decision rather than a
  shortcut: this runs over every file of a corpus that may be too
  expensive to gate in full, and constructing a parse just to read two
  fields would cost more than the gating it is deciding about."
  [content]
  (let [line (first (str/split (or content "") #"[\r\n]"))]
    (when (and line (str/starts-with? line "MSH"))
      (let [fields (str/split line #"\|" -1)]
        (when (< 9 (count fields))
          {:msh-9 (nth fields 8) :msh-10 (nth fields 9)})))))

(defn stratified-selection
  "`entries` (each `{:path .. :msh-9 .. :msh-10 ..}`) x
  `{:skeleton-types <set of MSH-9 strings> :cap <int>}` ->
  `{:selected [entry ...] :strata {msh-9 {:n :gated :add-on? }}}`.

  `:selected` is sorted by `[msh-9 msh-10]`, so it is a function of the
  SET of entries and not of the order they were listed in -- which is
  what makes the determinism gate assertable as an equality rather than
  as a set comparison.

  A nil or non-positive `cap` means no cap: every add-on stratum is
  gated in full too, which is ruling D2 expressed as a configuration
  rather than as a second code path."
  [entries {:keys [skeleton-types cap]}]
  (let [skeleton-types (or skeleton-types #{})
        by-type (group-by #(or (:msh-9 %) unknown-stratum) entries)]
    (reduce
     (fn [acc [msh-9 group]]
       (let [add-on? (not (or (= unknown-stratum msh-9) (contains? skeleton-types msh-9)))
             ordered (sort-by :msh-10 group)
             taken (if (and add-on? (number? cap) (pos? cap))
                     (take cap ordered)
                     ordered)]
         (-> acc
             (update :selected into taken)
             (assoc-in [:strata msh-9] {:n (count group)
                                        :gated (count taken)
                                        :add-on? add-on?}))))
     {:selected [] :strata {}}
     (sort-by key by-type))))

(defn render-strata
  "The per-stratum census as lines a human reads, one per stratum,
  `n`/`gated` printed for every one. `no silent caps` is a promise
  about OUTPUT, so it is a function here rather than a comment."
  [strata]
  (mapv (fn [[msh-9 {:keys [n gated add-on?]}]]
          (format "%-12s n=%-6d gated=%-6d %s"
                  msh-9 n gated (if add-on? "sampled (add-on)" "full (skeleton)")))
        (sort-by key strata)))
