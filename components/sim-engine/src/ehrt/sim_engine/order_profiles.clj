(ns ehrt.sim-engine.order-profiles
  "The order-profiles catalytic (docs/sim-theory.edn's `:execute` stage,
  target 3 -- hashed repo-authored config, docs/sim-theory.md's
  Catalytic resolution table): a small, hand-curated starter set of lab
  order/result profiles (`resources/order-profiles.edn`) -- CBC and BMP
  panels, real LOINC codes verified against loinc.org
  (`sim/F7`), US conventional units, typical adult
  reference ranges, and a per-analyte value distribution.

  Concepts ride as {:system :loinc :code :display} triplets, the same
  coded-triplet shape sim-model/Concept already
  establishes (`sim/ADR-0002`'s code-provenance law) -- no CPT anywhere
  (docs/third-party-sources.md's standing constraint).

  Value sampling law: `sample-analyte-value` draws a 3-way categorical
  choice (normal / abnormal-low / abnormal-high, an analyte's own
  :weights) then a uniform value within whichever range was chosen,
  rounded to that analyte's own reporting :precision. `abnormal-flag` is
  a SEPARATE, pure function of value vs reference-range -- the flag is
  computed truth, never sampled independently (Milestone M3 Task 4's
  own mini-law; this namespace is where that law's mechanism lives, the
  order/result step types (engine.clj) just call it)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.sim-model.interface :as sim-model]
            [malli.core :as m])
  (:import [java.util Random]))

(def Range
  [:map [:low number?] [:high number?]])

(def Weights
  [:map [:normal :double] [:low :double] [:high :double]])

(def AnalyteDistribution
  [:map
   [:weights Weights]
   [:abnormal-low-range Range]
   [:abnormal-high-range Range]])

(def Analyte
  [:map
   [:concept sim-model/Concept]
   [:units :string]
   [:precision :int]
   [:reference-range Range]
   [:distribution AnalyteDistribution]])

(def Turnaround
  "Authored in MINUTES (docs/patient-state-model.md's durations rule) --
  the engine converts to seconds at decide-time, same treatment
  :delay's IR already gets."
  [:map [:from :int] [:to :int]])

(def OrderProfile
  [:map
   [:concept sim-model/Concept]
   [:turnaround-minutes Turnaround]
   [:analytes [:vector Analyte]]])

(def OrderProfiles
  [:map-of :keyword OrderProfile])

(defn valid-profiles? [profiles] (m/validate OrderProfiles profiles))

(defn explain-profiles [profiles] (m/explain OrderProfiles profiles))

(def default-profiles
  "Loaded once at namespace load: resources/order-profiles.edn, this
  repo's own hashed-config catalytic content (target 3 -- committed,
  repo-authored, not fetched or generated)."
  (edn/read-string (slurp (io/resource "sim-engine/order-profiles.edn"))))

(defn- round-to
  [^double v ^long precision]
  (let [scale (Math/pow 10 precision)]
    (/ (Math/round (* v scale)) scale)))

(defn- uniform-in
  [^Random rng lo hi]
  (+ lo (* (.nextDouble rng) (- hi lo))))

(defn sample-analyte-value
  "Samples one value for `analyte` from `rng`: a categorical draw among
  :normal/:low/:high (the analyte's own :weights, which sum to 1.0),
  then a uniform value within whichever range was chosen (the
  reference-range for :normal, the distribution's own abnormal-*-range
  otherwise), rounded to the analyte's own :precision. Consumes exactly
  two draws from `rng` (one categorical, one uniform) regardless of
  which branch is taken -- fixed consumption, the same law
  ehrt.sim-engine.engine/assign-pathway and ehrt.sim-engine.churn
  already establish for this project's other categorical/probabilistic
  choices."
  [^Random rng {:keys [reference-range distribution precision]}]
  (let [{:keys [weights abnormal-low-range abnormal-high-range]} distribution
        {:keys [normal low]} weights
        category-draw (.nextDouble rng)
        [lo hi] (cond
                  (< category-draw normal) [(:low reference-range) (:high reference-range)]
                  (< category-draw (+ normal low)) [(:low abnormal-low-range) (:high abnormal-low-range)]
                  :else [(:low abnormal-high-range) (:high abnormal-high-range)])]
    (round-to (uniform-in rng lo hi) precision)))

(defn abnormal-flag
  "The computed-truth mini-law (Milestone M3 Task 4): a value's
  abnormal flag is DERIVED from comparing it against `reference-range`,
  never sampled independently. :normal when within [low, high]
  inclusive, :low when below, :high when above."
  [value {:keys [low high]}]
  (cond
    (< value low) :low
    (> value high) :high
    :else :normal))
