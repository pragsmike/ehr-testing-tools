(ns ehrt.person-simulator.persona
  "`initial-persona`: the t0 Persona for one person, and -- ruling A1 --
  the DERIVED Persona for a newborn.

  The adult path IS `sim-model`'s own `persona` call, unchanged and
  through the interface, which is what makes a wired consumer that
  reads no person events byte-identical to today (ruling F1). It draws
  `sim-model`'s own fixed 13 (16 with the config-gated demographic
  weights supplied).

  The newborn path draws FOUR, from the newborn's own `:person` stream
  keyed by `ehrt.sim-engine.engine/newborn-id-tag`. Ruling A1 states
  the reason in one line -- *a newborn is not a sampled adult*:
  surname, address and phone come from the household, `:dob` from the
  delivery instant, `:payer` from the parent's current coverage, and
  only `:sex`, the given name and the two SSN components are left for
  the stream to decide.

  The vendored demographic tables are `sim-model`'s own, read off the
  classpath as resources rather than copied: `given-names.edn` for the
  newborn's given name, and (in `process`) `places.edn` for a
  residence move. Reading the resource is deliberate -- these tables
  are not on `ehrt.sim-model.interface`, and forking their CONTENT
  into this component is the one thing `rulings.md#R-mix-3` and this
  component's own limitations row 7 forbid."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.person-simulator.clock :as clock]
            [ehrt.sim-model.interface :as sim-model])
  (:import [java.time LocalDate]))

(def given-names-by-sex-and-decade
  "`sim-model`'s own vendored given-name table, read as a resource:
  {sex -> {decade -> [{:name :weight} ...]}}."
  (edn/read-string (slurp (io/resource "sim-model/demographics/given-names.edn"))))

(def ^:private decades
  (vec (sort (keys (:female given-names-by-sex-and-decade)))))

(defn- nearest-decade [year]
  (let [decade (* 10 (quot year 10))]
    (apply min-key #(Math/abs (long (- % decade))) decades)))

(defn weighted-pick
  "Which member of `pool` (maps carrying `:weight`) the already-drawn
  uniform `draw` falls into -- cumulative-weight bucketing. An
  independent small copy, the same call `ehrt.sim-model.persona`'s own
  private `weighted-pick` makes and for the same stated reason: a
  shared dependency for twenty lines of arithmetic would couple two
  components that must not depend on each other's internals."
  [pool draw]
  (let [total (reduce + (map :weight pool))
        target (* draw total)]
    (loop [members pool acc 0.0]
      (let [m (first members) more (rest members) acc' (+ acc (double (:weight m)))]
        (if (or (empty? more) (< target acc')) m (recur more acc'))))))

(defn- rand-int-in [^java.util.Random rng lo hi]
  (+ lo (.nextInt rng (inc (- hi lo)))))

;; The area-number prefix `sim-model`'s own persona uses, for the same
;; reason it uses it: 900 sits inside the block SSA states it will
;; never issue, so the identifier is obviously-synthetic by
;; construction. Repeated here rather than reached for, because it is
;; a three-character constant and the alternative is an interface
;; widening in a component this one may not change.
(def ^:private ssn-area-prefix "900")

(defn epoch-day-at
  "The epoch day of engine instant `t`. Run t=0 is
  `sim-model/reference-today-epoch-day`, the same anchor the engine's
  own `:registered` decide method already uses as `registration-t`, so
  a person event's calendar date and a patient's are read off ONE
  anchor."
  ^long [^long t]
  (+ (sim-model/reference-today-epoch-day) (quot t clock/seconds-per-day)))

(defn newborn-persona
  "Ruling A1's derived newborn Persona -- FOUR draws from `rng`, in this
  exact order, always:

    1. sex (1 draw)
    2. given name, weighted within the sampled sex + birth-decade
       bucket (1 draw)
    3. SSN group number, 1-99 (1 draw)
    4. SSN serial number, 1-9999 (1 draw)

  Everything else is DERIVED and draws nothing: `:name :family`,
  `:address` and `:phone` from the household; `:dob` and `:age` from
  the delivery instant; `:payer` from the parent's current coverage."
  [^java.util.Random rng {:keys [household parent-payer delivery-t]}]
  (let [sex (if (< (.nextDouble rng) 0.5) :female :male)
        birth-day (epoch-day-at delivery-t)
        birth-date (LocalDate/ofEpochDay birth-day)
        birth-year (.getYear birth-date)
        pool (get-in given-names-by-sex-and-decade [sex (nearest-decade birth-year)])
        given (:name (weighted-pick pool (.nextDouble rng)))
        ssn-group (rand-int-in rng 1 99)
        ssn-serial (rand-int-in rng 1 9999)]
    {:name {:family (:surname household) :given given}
     :sex sex
     :dob (str birth-date)
     :age 0
     :address (:address household)
     :phone (:phone household)
     :ssn (format "%s-%02d-%04d" ssn-area-prefix ssn-group ssn-serial)
     :payer parent-payer}))

(defn initial-persona
  "ADR-0172 section 2's `initial-persona`, both arities.

  `t0` is the t0 CONTEXT, not a bare instant -- a premise this arc had
  to settle against the tree. The charter writes the signature as
  `(initial-persona person-id t0)`, and a two-argument function that
  must DRAW cannot: there is no seed in `person-id` and none in an
  instant. Ruling C1 names the resolution in its own words -- the
  compiled trajectory's death instant arrives \"as a t0 parameter\" --
  so `t0` is the map of parameters available at t0:

    {:t        the person's t0 instant (engine seconds)
     :master   the run's master seed          (or :rng, below)
     :id-tag   this person's :person-family id-tag
     :rng      an already-positioned java.util.Random, when the caller
               has one -- `process/persons` does, because the walk
               continues on the SAME stream the persona was drawn from
     :death-t  the COMPILED trajectory's death instant, if any (ruling
               C1). Taken as DATA. This component never requires
               `patient-simulator` to learn it, and never draws its own
               death for a person who carries one.
     :persona  the `sim-model/persona` config}

  The 2-arity IS `sim-model`'s own call: 13 draws (16 with the
  config-gated demographic weights), so a consumer that reads no
  person events is byte-identical to today. The 3-arity is ruling A1's
  newborn: FOUR draws and a Persona derived from the household.

  `:death-t` shapes no field of the returned Persona -- a t0 Persona
  does not encode a death, and widening `sim-model/Persona` is a
  change this component may not make. It rides the context because
  ruling C1 puts it there, and `process` reads it to truncate."
  ([person-id t0]
   (let [rng (or (:rng t0) (throw (ex-info "initial-persona needs :rng or :master/:id-tag in its t0 context"
                                           {:person-id person-id :t0 (dissoc t0 :rng)})))]
     (sim-model/persona rng (:persona t0))))
  ([person-id t0 birth-ctx]
   (let [rng (or (:rng t0) (throw (ex-info "initial-persona needs :rng or :master/:id-tag in its t0 context"
                                           {:person-id person-id :t0 (dissoc t0 :rng)})))]
     (newborn-persona rng birth-ctx))))
