(ns ehrt.sim.persona
  "Persona (docs/sim-theory.edn's `:persona` stage): seeded, pure
  sampling of a patient's demographic identity plus payer -- name, DOB
  (age from a configured distribution), sex, address, a US-format
  phone number, an obviously-synthetic SSN-shaped identifier, and a
  payer drawn from the `:payers` pool with docs/operational-models.md's
  age-linkage (Medicare dominant 65+).

  This is the real `payer-pool` catalytic wire `docs/sim-theory.edn`'s
  `:persona` comment always named as a forward reference -- it RETIRES
  the engine-patient-init payer stand-in docs/operational-models.md
  itself named ('until Persona exists, this model runs at engine
  patient-init time, as a stand-in that Persona subsumes rather than
  replaces'): payer sampling now happens HERE, once, at patient
  creation, never resampled (the attribute-pool contract, `sim/ADR-0007`).
  There was no actual stand-in CODE to remove -- `:payer` has been a
  reserved, always-nil PatientState field since M1 -- so 'retiring' the
  stand-in means wiring the real sampler in its place, not deleting
  anything.

  Vendored tables: resources/demographics/{given-names,surnames,
  places}.edn -- SMALL and HAND-CURATED. resources/demographics/NOTICE
  records exactly why: this session found no `../` Synthea checkout to
  extract from (AGENTS.md's 'do not invent facts about upstream
  sources' rule applies to what this repo claims to have vendored, not
  only to prose claims), so the tables are original content in the
  SAME schema shape a real Synthea extraction would use -- a future
  session with a checkout available can replace their content wholesale
  with no reader-side change.

  RNG consumption is FIXED per persona (13 draws, always, regardless of
  which branch any weighted pick lands in) -- the same fixed-consumption
  law `ehrt.sim.engine/assign-pathway` and `ehrt.sim.churn`
  already establish for this project's other probabilistic choices (see
  `persona`'s own docstring for the exact sequence)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [malli.core :as m])
  (:import [java.util Random]))

;; --- Vendored tables (resources/demographics/) ----------------------------

(def given-names-by-sex-and-decade
  "Loaded once at namespace load: resources/demographics/given-names.edn
  -- {sex -> {decade -> [{:name :weight} ...]}}."
  (edn/read-string (slurp (io/resource "sim/demographics/given-names.edn"))))

(def surnames
  "Loaded once: resources/demographics/surnames.edn -- a flat weighted
  pool, [{:name :weight} ...]."
  (edn/read-string (slurp (io/resource "sim/demographics/surnames.edn"))))

(def places
  "Loaded once: resources/demographics/places.edn -- a flat weighted
  pool of full address rows, [{:street :city :state :zip :weight} ...]."
  (edn/read-string (slurp (io/resource "sim/demographics/places.edn"))))

;; --- Payer pool (docs/operational-models.md's payers model; this
;; namespace is its real binding, per sim/ADR-0007 decision 4) -----------------

(def Payer
  [:map
   [:id :string]
   [:name :string]
   [:type [:enum :medicare :medicaid :commercial :self-pay]]])

(def under-65-payers
  "The weighted pool sampled for patients younger than 65 -- Medicare
  present but minor (disability/ESRD-eligible non-elderly enrollees are
  real but a small minority), commercial dominant, per
  docs/operational-models.md's 'general shape of US payer-mix data'."
  [{:id "commercial-ppo" :name "Commercial PPO" :type :commercial :weight 45.0}
   {:id "commercial-hmo" :name "Commercial HMO" :type :commercial :weight 20.0}
   {:id "medicaid" :name "Medicaid" :type :medicaid :weight 22.0}
   {:id "self-pay" :name "Self-Pay" :type :self-pay :weight 8.0}
   {:id "medicare-disability" :name "Medicare (Disability/ESRD)" :type :medicare :weight 5.0}])

(def sixty-five-plus-payers
  "The weighted pool sampled for patients 65+ -- Medicare dominant
  (eligibility itself starts at 65, docs/operational-models.md), the
  age-linkage this namespace's own co-landing invariant checks."
  [{:id "medicare-65" :name "Medicare" :type :medicare :weight 70.0}
   {:id "medicare-advantage" :name "Medicare Advantage" :type :medicare :weight 15.0}
   {:id "commercial-ppo" :name "Commercial PPO" :type :commercial :weight 8.0}
   {:id "medicaid" :name "Medicaid" :type :medicaid :weight 5.0}
   {:id "self-pay" :name "Self-Pay" :type :self-pay :weight 2.0}])

;; --- Persona shape ---------------------------------------------------------

(def Persona
  [:map
   [:name [:map [:family :string] [:given :string]]]
   [:sex [:enum :female :male]]
   [:dob [:re #"^\d{4}-\d{2}-\d{2}$"]]
   [:age :int]
   [:address [:map [:street :string] [:city :string] [:state :string] [:zip :string]]]
   [:phone [:re #"^\d{3}-\d{3}-\d{4}$"]]
   [:ssn [:re #"^\d{3}-\d{2}-\d{4}$"]]
   [:payer Payer]])

(defn valid-persona? [p] (m/validate Persona p))
(defn explain-persona [p] (m/explain Persona p))

;; --- Sampling primitives ---------------------------------------------------

(defn- rand-int-in
  [^Random rng lo hi]
  (+ lo (.nextInt rng (inc (- hi lo)))))

(defn- weighted-pick
  "Which member of `pool` (a seq of maps carrying :weight) `draw` (a
  uniform double in [0,1), already consumed) falls into -- cumulative-
  weight bucketing, the same shape ehrt.sim.engine's own private
  weighted-pick uses, kept as an independent small copy here rather
  than a shared dependency (persona.clj must stay engine-independent --
  the engine calls INTO persona at patient-init, so the reverse
  dependency would be circular)."
  [pool draw]
  (let [total (reduce + (map :weight pool))
        target (* draw total)]
    (loop [members pool acc 0.0]
      (let [m (first members) more (rest members) acc' (+ acc (double (:weight m)))]
        (if (or (empty? more) (< target acc')) m (recur more acc'))))))

(def reference-birth-year
  "The fixed, documented anchor 'today' is relative to when computing a
  birth year from a sampled age -- deliberately NOT wall-clock (the
  determinism law: no unseeded entropy anywhere in the output path).
  2024 matches ehrt.sim.emit-hl7/default-reference-date's own
  year, so DOB and rendered message timestamps stay mutually plausible
  for a run using the default reference date; a caller who overrides
  :reference-date and needs exact DOB-vs-encounter-date coherence is
  out of this milestone's scope (recorded here, not silently assumed).
  Public as of M5b: ehrt.sim.engine's own :registered decide
  method needs this SAME anchor as `docs/gmf-interpreter.md`'s own
  `registration-t` -- 'that patient's own :registered event time' is,
  in THIS project's calendar terms, this fixed reference date, the same
  one every persona's own age is already computed against -- rather
  than inventing a second, potentially-drifting copy of the constant."
  2024)

(defn reference-today-epoch-day
  "The epoch-day (java.time.LocalDate/toEpochDay) of this run's own fixed
  calendar anchor -- `reference-birth-year` PLUS ONE, Jan 1, deliberately
  NOT `reference-birth-year` itself: an age-0 persona's own DOB is
  sampled ANYWHERE within `reference-birth-year` (month/day are free,
  `persona`'s own docstring), so a same-year anchor could fall BEFORE a
  real sampled DOB (a negative-age nonsense for the GMF interpreter's
  own history-phase walk, which assumes `registration-t >= dob-epoch-
  day`) -- one full year clears every possible DOB `persona` can ever
  produce, for any age from 0 up. Every persona's DOB is computed
  relative to `reference-birth-year`, so THIS date is also the correct,
  patient-independent `registration-t` for a real engine run's own GMF
  module walk (M5b, ehrt.sim.engine's :registered decide method)."
  []
  (.toEpochDay (java.time.LocalDate/of ^int (inc reference-birth-year) 1 1)))

(def ^:private decades
  "The birth decades resources/demographics/given-names.edn actually
  covers, ascending -- used to clamp a sampled birth year to the
  nearest bucket the vendored table has data for."
  (vec (sort (keys (:female given-names-by-sex-and-decade)))))

(defn- nearest-decade
  [year]
  (let [decade (* 10 (quot year 10))]
    (apply min-key #(Math/abs (long (- % decade))) decades)))

(def ^:private ssn-area-prefix
  "Fixed area-number prefix for the synthetic SSN-shaped identifier:
  '900' is inside the block SSA has never issued and states it will
  never issue (area numbers 000, 666, and 900-999 are permanently
  excluded from assignment -- `sim/F8`) -- an
  obviously-synthetic-by-construction choice, the same design move
  `sim/ADR-0007` already made for provider NPIs (a real, documented
  never-issued range rather than an arbitrary sentinel format)."
  "900")

(defn persona
  "Samples one persona from `rng` (the run's own seeded java.util.Random
  -- never a derived/isolated stream, `sim/ADR-0009`'s own reasoning extended
  here) and `config` ({:age-min :age-max :payers-under-65 :payers-65-plus},
  all optional). Fixed RNG consumption, in this exact order, always:

    1. sex (1 draw)
    2. age, uniform in [age-min, age-max] (1 draw)
    3. birth month, 1-12 (1 draw)
    4. birth day, 1-28 -- deliberately NOT month-length-aware, a
       documented simplification for non-PHI synthetic data (1 draw)
    5. given name, weighted within the sampled sex+birth-decade bucket
       (1 draw)
    6. surname, weighted over the flat pool (1 draw)
    7. address, weighted over the flat places pool (1 draw)
    8. phone area code, 200-999 (1 draw)
    9. phone exchange, 200-999 (1 draw)
    10. phone subscriber, 0-9999 (1 draw)
    11. SSN group number, 1-99 (1 draw)
    12. SSN serial number, 1-9999 (1 draw)
    13. payer, weighted over whichever pool the sampled age selects
        (:payers-65-plus at 65+, :payers-under-65 otherwise -- pool
        SELECTION is derived from age, not itself a draw) (1 draw)

  13 draws total, always -- fixed regardless of which pool/bucket any
  weighted pick lands in (the same `sim/ADR-0009`-derived law
  ehrt.sim.engine/assign-pathway and ehrt.sim.churn/
  roll-gap already state for this project's other probabilistic
  choices)."
  [^Random rng {:keys [age-min age-max payers-under-65 payers-65-plus]
                :or {age-min 0 age-max 90
                     payers-under-65 under-65-payers
                     payers-65-plus sixty-five-plus-payers}}]
  (let [sex (if (< (.nextDouble rng) 0.5) :female :male)
        age (rand-int-in rng age-min age-max)
        birth-year (- reference-birth-year age)
        birth-month (rand-int-in rng 1 12)
        birth-day (rand-int-in rng 1 28)
        decade (nearest-decade birth-year)
        given-name-pool (get-in given-names-by-sex-and-decade [sex decade])
        given-name (:name (weighted-pick given-name-pool (.nextDouble rng)))
        surname (:name (weighted-pick surnames (.nextDouble rng)))
        place (weighted-pick places (.nextDouble rng))
        area-code (rand-int-in rng 200 999)
        exchange (rand-int-in rng 200 999)
        subscriber (rand-int-in rng 0 9999)
        ssn-group (rand-int-in rng 1 99)
        ssn-serial (rand-int-in rng 1 9999)
        payer-pool (if (>= age 65) payers-65-plus payers-under-65)
        payer (dissoc (weighted-pick payer-pool (.nextDouble rng)) :weight)]
    {:name {:family surname :given given-name}
     :sex sex
     :dob (format "%04d-%02d-%02d" birth-year birth-month birth-day)
     :age age
     :address (select-keys place [:street :city :state :zip])
     :phone (format "%03d-%03d-%04d" area-code exchange subscriber)
     :ssn (format "%s-%02d-%04d" ssn-area-prefix ssn-group ssn-serial)
     :payer payer}))
