(ns ehrt.sim-emit-hl7.fan-out
  "ARC 4 SWEEP 5 (`notes/adr/0175-arc-4-emission-add-ons.md` design (f),
  ruling B1; author ruling 2026-08-28, collision option (b)): the
  subscriber table -- a FILTER over an already-rendered stream.

  It creates no content, reads no state, and draws nothing. Every
  message a subscriber receives is a message `emit-hl7/emit-wire`
  already produced, at the position it already occupied, with at most
  the four MSH routing fields MSH-3/4/5/6 overwritten. That is the
  whole of what this namespace does.

  IDENTITY IS THE LOG INDEX, NEVER MSH-10 (author ruling 2026-08-28,
  option (b)). `control-id-for` is known NON-INJECTIVE over
  `:result-available` -- two results for one patient at one second mint
  the same MSH-10, live in two shipped corpora today
  (`roadmap.md#oru-control-id-collision`, arc 4 sweep 3's finding 1) --
  so a subscriber spool keyed on MSH-10 would deliver one twin twice
  and the other never. A message's identity here is its POSITION in
  `emit-wire`'s own output vector, which is total, ordered and minted
  by nothing.

  THE SUBSEQUENCE LAW, which `fan_out_test`'s own property gates:

    For every subscriber s and every base message vector M,
    `:indices` is a STRICTLY INCREASING vector of positions in M and

        (:messages s) = [ (mask s (M i)) | i <- (:indices s) ]

    where `mask` replaces exactly the MSH fields named by s's own
    `:msh` map -- MSH-3, MSH-4, MSH-5, MSH-6 and no others -- and is
    the IDENTITY when s names no `:msh`.

    Equivalently: with no `:msh` override a subscriber's spool is a
    BYTE-EXACT SUBSEQUENCE of the base spool; with one it is byte-exact
    after masking, in BOTH spools, exactly the overridden MSH field
    positions.

  THE PV1-LESS RULE, written down rather than discovered (ADR-0175
  section 2(f)). A `:patient-classes` filter reads PV1-2, and ADT^A20
  has no PV1 at all (`[MSH EVN NPU]`). A class filter therefore
  EXCLUDES every PV1-less message UNLESS the subscriber names that
  message's own `TYPE^TRIGGER` explicitly in `:message-types`. Stated
  as a rule this is sane routing; discovered as a behaviour it is a
  silently empty bed-management feed.

  THE ALLOW-LIST LAW. A `:message-types` entry naming a `TYPE^TRIGGER`
  this emitter cannot produce is a CONFIGURATION ERROR, rejected before
  the engine runs (`ehrt.sim.run`'s own `:invalid-fan-out` branch, the
  `:invalid-siu` precedent) -- never a feed that is silently empty
  because of a typo. The vocabulary is `emit-hl7/emittable-message-
  types`, derived from the registry plus the arc-4 add-on families.

  WHY THIS NAMESPACE PARSES ITS OWN TWO FIELDS rather than requiring
  `ehrt.corpus-io.er7-fields`, which already reads MSH-9 leniently: a
  cross-brick dependency for two field reads costs four `deps.edn`
  files and a new edge in the workspace graph, and this brick already
  parses its own emitted stream in `ehrt.sim-emit-hl7.v2-replay`. The
  reads below are the same lenient split-on-the-declared-separator
  shape, kept private and used for routing only."
  (:require [clojure.string :as str]
            [ehrt.sim-emit-hl7.site-profile :as site-profile]))

;; --- lenient reads: MSH-9 and PV1-2, from the rendered text ----------
;; A router reads the wire, not the log. These two reads are all a
;; filter consults.

(defn- segments
  [^String message]
  ;; -1: trailing empty pieces are KEPT, so `(str/join "\r" (segments m))`
  ;; is the exact inverse of this split for every message, including one
  ;; that ends with its own segment terminator. A default-limit split
  ;; would silently drop that terminator and the mask would move a byte
  ;; the subsequence law forbids it to move.
  (str/split message #"\r" -1))

(defn- separator-char
  "MSH-1 IS the field separator -- the character immediately after the
  three-character \"MSH\" literal."
  [^String msh-segment]
  (when (>= (count msh-segment) 4)
    (str (nth msh-segment 3))))

(defn- split-fields
  [^String segment sep]
  (str/split segment (re-pattern (java.util.regex.Pattern/quote sep)) -1))

(def ^:private msh-field-index
  "Override key -> its 0-based position in the MSH segment split on the
  declared separator. Splitting the WHOLE segment yields \"MSH\" at 0
  and MSH-2 at 1, so MSH-N sits at N-1. THIS MAP IS THE MASK: exactly
  these four positions may be rewritten, and the subsequence law's own
  `mask` is defined over exactly this key set."
  {:sending-app 2 :sending-facility 3 :receiving-app 4 :receiving-facility 5})

(defn- message-type-trigger
  "The message's MSH-9 as `TYPE^TRIGGER` -- the first TWO components
  only, so a three-component MSH-9 (`ADT^A01^ADT_A01`) still matches a
  filter written the way this emitter renders it. nil when there is no
  readable MSH."
  [^String message]
  (let [msh (first (segments message))]
    (when-let [sep (separator-char msh)]
      (let [v (nth (split-fields msh sep) 8 nil)]
        (when (seq v)
          (str/join "^" (take 2 (str/split v #"\^" -1))))))))

(defn- patient-class-code
  "PV1-2 (patient class) as its raw code string, or nil when the
  message carries no PV1 segment at all -- the ADT^A20 case the
  PV1-less rule exists for."
  [^String message]
  (let [segs (segments message)
        msh (first segs)]
    (when-let [sep (separator-char msh)]
      (when-let [pv1 (first (filter #(str/starts-with? % "PV1") segs))]
        (let [v (nth (split-fields pv1 sep) 2 nil)]
          (when (seq v) v))))))

;; --- the mask ---------------------------------------------------------

(defn mask-msh
  "`message` with exactly the MSH fields named by `overrides` (a map
  keyed by `msh-field-index`'s own keys) replaced by `replacement-fn`
  applied to that key -- the mask half of the subsequence law, and the
  ONLY byte a subscriber's spool may differ in.

  Used two ways: at plan time with `(fn [k] (get overrides k))` to
  WRITE the override, and by the law's own property with a constant
  sentinel to ERASE it on both sides before comparing."
  [^String message overrides replacement-fn]
  (if (empty? overrides)
    message
    (let [segs (segments message)
          msh (first segs)
          sep (separator-char msh)]
      (if (nil? sep)
        message
        (let [fields (vec (split-fields msh sep))
              rewritten (reduce (fn [fs k]
                                  (let [i (get msh-field-index k)]
                                    (if (and i (< i (count fs)))
                                      (assoc fs i (str (replacement-fn k)))
                                      fs)))
                                fields
                                (sort (keys overrides)))]
          (str/join "\r" (cons (str/join sep rewritten) (rest segs))))))))

;; --- the filter -------------------------------------------------------

(defn- class-codes
  "The `:patient-classes` keywords resolved to the CODE STRINGS this
  run actually renders in PV1-2 -- through `site-profile/code-for`, the
  same override path the emitter itself used, so a site that renders
  :inpatient as something other than \"I\" routes on what it wrote."
  [site-profile classes]
  (into #{} (map (fn [c] (first (site-profile/code-for site-profile :patient-class
                                                        site-profile/standard-patient-class-codes c))))
        classes))

(defn- accepts?
  "The routing predicate, over one message's already-read MSH-9 and
  PV1-2. The two dimensions are ANDed; the PV1-less rule is the class
  dimension's own escape clause, not a third dimension."
  [{:keys [message-types class-code-set]} msh-9 pv1-class]
  (let [type-ok? (or (nil? message-types) (contains? message-types msh-9))
        class-ok? (cond
                    (nil? class-code-set) true
                    (some? pv1-class) (contains? class-code-set pv1-class)
                    ;; PV1-less: only an explicitly named trigger survives
                    :else (boolean (and message-types (contains? message-types msh-9))))]
    (and type-ok? class-ok?)))

(defn plan
  "`messages` (the run's own rendered vector, in emission order) x
  `subscribers` (the validated `:fan-out` table) x `site-profile` ->
  a vector of

    {:name <keyword> :filter <as authored> :msh <as authored>
     :indices [<position in `messages`> ...] :count <n>
     :messages [<masked message> ...]}

  one entry per subscriber, in the table's own order. Pure: no IO, no
  clock, no RNG. `:indices` is strictly increasing BY CONSTRUCTION --
  the base vector is walked once, in order, and nothing here sorts."
  [messages subscribers site-profile]
  (let [read-once (mapv (fn [m] [(message-type-trigger m) (patient-class-code m)]) messages)]
    (mapv
     (fn [{:keys [name] flt :filter msh-overrides :msh}]
       (let [compiled {:message-types (:message-types flt)
                       :class-code-set (when-let [cs (:patient-classes flt)]
                                         (class-codes site-profile cs))}
             indices (into [] (keep (fn [i]
                                      (let [[msh-9 pv1-class] (nth read-once i)]
                                        (when (accepts? compiled msh-9 pv1-class) i))))
                           (range (count messages)))]
         (cond-> {:name name
                  :indices indices
                  :count (count indices)
                  :messages (mapv (fn [i] (mask-msh (nth messages i) msh-overrides #(get msh-overrides %)))
                                  indices)}
           flt (assoc :filter flt)
           msh-overrides (assoc :msh msh-overrides))))
     subscribers)))
