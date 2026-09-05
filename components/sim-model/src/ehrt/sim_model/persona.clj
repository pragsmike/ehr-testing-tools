(ns ehrt.sim-model.persona
  "Persona (docs/sim-theory.edn's `:persona` stage): seeded, pure
  sampling of a patient's demographic identity plus payer -- name, DOB
  (age from a configured distribution), sex, address, a US-format
  phone number, an obviously-synthetic SSN-shaped identifier, and a
  payer drawn from the `:payers` pool with components/sim/docs/operational-models.md's
  age-linkage (Medicare dominant 65+).

  This is the real `payer-pool` catalytic wire `docs/sim-theory.edn`'s
  `:persona` comment always named as a forward reference -- it RETIRES
  the engine-patient-init payer stand-in components/sim/docs/operational-models.md
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
  law `ehrt.sim-engine.assignment/assign-pathway` and `ehrt.sim-engine.churn`
  already establish for this project's other probabilistic choices (see
  `persona`'s own docstring for the exact sequence). GMF coverage Wave F
  (2026-08-03, ADR-0036 AR-4/AR-5) adds two further, CONFIG-GATED draws
  (:race/:socioeconomic-category) -- a deliberate, narrow, documented
  exception to 'always,' not a silent violation (`persona`'s own
  docstring has the full reasoning). GMF coverage Wave LC (2026-08-03,
  ADR-0038 AR-3) adds a THIRD config-gated draw, :state -- the SAME
  pattern verbatim, a US-state-name value vocabulary the lookup-table
  column family's own CSVs key on (`myocardial_infarction.json`'s own
  closure), deliberately distinct from the EXISTING `:address :state`
  field (a USPS two-letter abbreviation, `places.edn`'s own vocabulary
  -- a real, disclosed divergence: two different :state-shaped fields
  for two different real-world vocabularies, never unified)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [malli.core :as m])
  (:import [java.util Random]))

;; --- Vendored tables (resources/demographics/) ----------------------------

(def given-names-by-sex-and-decade
  "Loaded once at namespace load: resources/demographics/given-names.edn
  -- {sex -> {decade -> [{:name :weight} ...]}}."
  (edn/read-string (slurp (io/resource "sim-model/demographics/given-names.edn"))))

(def surnames
  "Loaded once: resources/demographics/surnames.edn -- a flat weighted
  pool, [{:name :weight} ...]."
  (edn/read-string (slurp (io/resource "sim-model/demographics/surnames.edn"))))

(def places
  "Loaded once: resources/demographics/places.edn -- a flat weighted
  pool of full address rows, [{:street :city :state :zip :weight} ...]."
  (edn/read-string (slurp (io/resource "sim-model/demographics/places.edn"))))

;; --- Payer pool (components/sim/docs/operational-models.md's payers model; this
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
  components/sim/docs/operational-models.md's 'general shape of US payer-mix data'."
  [{:id "commercial-ppo" :name "Commercial PPO" :type :commercial :weight 45.0}
   {:id "commercial-hmo" :name "Commercial HMO" :type :commercial :weight 20.0}
   {:id "medicaid" :name "Medicaid" :type :medicaid :weight 22.0}
   {:id "self-pay" :name "Self-Pay" :type :self-pay :weight 8.0}
   {:id "medicare-disability" :name "Medicare (Disability/ESRD)" :type :medicare :weight 5.0}])

(def sixty-five-plus-payers
  "The weighted pool sampled for patients 65+ -- Medicare dominant
  (eligibility itself starts at 65, components/sim/docs/operational-models.md), the
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
   [:payer Payer]
   ;; GMF coverage Wave F (2026-08-03, ADR-0036 AR-4): optional, sampled
   ;; ONLY when `persona`'s own config supplies category weights (AR-5) --
   ;; the ONE persona field pair whose presence is config-time-gated
   ;; rather than always-present. Real Synthea's own closed vocabularies
   ;; (Logic.java Race/SocioeconomicStatus, source-grounded): race in
   ;; {"White" "Native" "Hispanic" "Black" "Asian" "Other"}, category in
   ;; {"High" "Middle" "Low"} -- not enforced here (the weighted pool a
   ;; caller supplies is this project's own scenario-authored content,
   ;; the same "declared, not validated against a closed set" treatment
   ;; `:payer`'s own :type enum is the one exception to, and only because
   ;; that enum backs real payer-mix modeling, components/sim/docs/operational-models.md).
   [:race {:optional true} :string]
   [:socioeconomic-category {:optional true} :string]
   ;; GMF coverage Wave LC (2026-08-03, ADR-0038 AR-3): optional,
   ;; sampled ONLY when persona config supplies `:state-weights` -- the
   ;; SAME config-gated pattern :race/:socioeconomic-category (above)
   ;; already establish. Deliberately NOT the same field as
   ;; `:address :state` (a USPS abbreviation) -- the lookup-table CSVs
   ;; this field exists to unblock key on full US state NAMES
   ;; (`myocardial_infarction.json`'s own closure, e.g.
   ;; `ace_arb_*_product_distribution.csv`'s own `state` column,
   ;; "Alabama"/"Alaska"/... -- confirmed by direct read against the
   ;; pin), a genuinely different vocabulary from `places.edn`'s own
   ;; two-letter codes, not enforced here (same "declared, not
   ;; validated against a closed set" treatment `:race`/
   ;; `:socioeconomic-category` already get).
   [:state {:optional true} :string]])

;; `Persona`'s validator, built ONCE at load -- the same one-line
;; change `sim-engine/event_schema.clj` makes for `Event` and
;; `GroundTruth`, made here for the same reason and measured the same
;; day. `valid-persona?` is called per RECORD, not per run:
;; `sim_check/check.clj`'s `registered-persona-is-schema-valid` runs it
;; on every `:registered` event in a log (2,015 of them in the
;; dense-7500 log at 20 arrivals), and `m/validate` recompiles `Persona`
;; on each of those calls. The schema is smaller than `Event`, so the
;; saving is smaller too -- 0.0239 ms a call before -- and it is the
;; same defect, so it is fixed rather than left as the one hot site
;; that was noticed and skipped. `explain-persona` stays interpreted:
;; it runs only on a persona that already failed.
(def ^:private persona-validator (m/validator Persona))

(defn valid-persona? [p] (persona-validator p))
(defn explain-persona [p] (m/explain Persona p))

;; --- Sampling primitives ---------------------------------------------------

(defn- rand-int-in
  [^Random rng lo hi]
  (+ lo (.nextInt rng (inc (- hi lo)))))

(defn- weighted-pick
  "Which member of `pool` (a seq of maps carrying :weight) `draw` (a
  uniform double in [0,1), already consumed) falls into -- cumulative-
  weight bucketing, the same shape ehrt.sim-engine.assignment's own private
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
  2024 matches ehrt.sim-emit-hl7.hl7-time/default-reference-date's own
  year, so DOB and rendered message timestamps stay mutually plausible
  for a run using the default reference date; a caller who overrides
  :reference-date and needs exact DOB-vs-encounter-date coherence is
  out of this milestone's scope (recorded here, not silently assumed).
  Public as of M5b: ehrt.sim-engine.engine's own :registered decide
  method needs this SAME anchor as `components/patient-simulator/docs/gmf-interpreter.md`'s own
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
  module walk (M5b, ehrt.sim-engine.engine's :registered decide method)."
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
  ehrt.sim-engine.assignment/assign-pathway and ehrt.sim-engine.churn/
  roll-gap already state for this project's other probabilistic
  choices).

  GMF coverage Wave F (2026-08-03, ADR-0036 AR-4/AR-5): TWO further
  draws, each CONFIG-GATED rather than always-on --

    14. race, weighted over `:race-weights` -- ONLY drawn when `config`
        supplies a non-empty pool; omitted (both the draw AND the
        :race key) otherwise
    15. socioeconomic-category, weighted over `:socioeconomic-weights`
        -- same conditional shape as 14

  GMF coverage Wave LC (2026-08-03, ADR-0038 AR-3): a THIRD config-gated
  draw, the SAME pattern verbatim --

    16. state, weighted over `:state-weights` -- same conditional shape
        as 14/15, ONLY drawn when `config` supplies a non-empty pool

  This is a DELIBERATE, narrow exception to 'fixed regardless of
  content,' not a violation of it: the law above guards against draw
  COUNT depending on a runtime OUTCOME within one persona's own
  sampling (which bucket a weighted pick lands in must never change how
  many draws happen). `:race-weights`/`:socioeconomic-weights`/
  `:state-weights` presence is a CONFIG-time decision -- the same class
  of variation `age-min`/`age-max` already are, a caller-supplied input
  that shapes what gets sampled, not a value THIS function chooses
  partway through. The identity-preservation reason this exception
  exists: adding these fields must not perturb the RNG stream for every
  EXISTING (unconfigured) caller -- an unconditional 14th/15th/16th draw
  would shift every subsequent draw for every persona this project has
  ever sampled, the actual concern the fixed-consumption law exists to
  prevent. `persona-test`'s own `counting-random` proves the pattern:
  13 draws with no config supplied (byte-identical to every persona
  sampled before ADR-0036), 16 with all three weights supplied."
  [^Random rng {:keys [age-min age-max payers-under-65 payers-65-plus
                       race-weights socioeconomic-weights state-weights]
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
    (cond-> {:name {:family surname :given given-name}
             :sex sex
             :dob (format "%04d-%02d-%02d" birth-year birth-month birth-day)
             :age age
             :address (select-keys place [:street :city :state :zip])
             :phone (format "%03d-%03d-%04d" area-code exchange subscriber)
             :ssn (format "%s-%02d-%04d" ssn-area-prefix ssn-group ssn-serial)
             :payer payer}
      ;; GMF coverage Wave F (ADR-0036 AR-4/AR-5): draws 14/15, config-
      ;; gated -- `(seq pool)` is both the presence check and the guard
      ;; against an accidentally-empty pool (weighted-pick divides by
      ;; the pool's own total weight; an empty pool would divide by
      ;; zero, the same silent-crash class every other weighted-pick
      ;; call site already avoids by only calling it over a real pool).
      (seq race-weights)
      (assoc :race (:race (weighted-pick race-weights (.nextDouble rng))))

      (seq socioeconomic-weights)
      (assoc :socioeconomic-category
             (:category (weighted-pick socioeconomic-weights (.nextDouble rng))))

      ;; GMF coverage Wave LC (ADR-0038 AR-3): draw 16, the SAME
      ;; config-gated pattern as 14/15 -- `:state`, not `:address
      ;; :state` (a genuinely different field, above).
      (seq state-weights)
      (assoc :state (:state (weighted-pick state-weights (.nextDouble rng)))))))
