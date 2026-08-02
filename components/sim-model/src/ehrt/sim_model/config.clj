(ns ehrt.sim-model.config
  "Config schemas and shipped defaults for the operational resource
  models (docs/operational-models.md): facility (beds -- exclusive),
  providers (shared). Payers land with Persona (M4); not here.

  Also: the synthetic-NPI Luhn math. `sim/ADR-0007` decision (a): provider
  identifiers are structurally valid NPIs (correct Luhn check digit
  over the CMS `80840` health-industry-issuer prefix), generated from
  the run's own seeded RNG -- not an obviously-fake sentinel format.
  `materialize-providers` is the one place a run turns the static,
  id-less `default-provider-templates` into a real provider pool by
  drawing NPIs; called once per run, before the main event loop
  (ehrt.sim.engine/run), so provider identity is as deterministic
  as everything else in the theory."
  (:require [malli.core :as m]))

;; --- Facility --------------------------------------------------------

(def Ward
  [:map
   [:id :keyword]
   [:name :string]
   [:beds :int]
   [:surge-slots :int]
   [:surge-format :string]
   [:class [:enum :inpatient :ed]]])

(def Facility
  [:map
   [:id :keyword]
   [:wards [:vector Ward]]])

(defn valid-facility? [facility] (m/validate Facility facility))
(defn explain-facility [facility] (m/explain Facility facility))

(def default-facility
  "Small on purpose (docs/operational-models.md): one ED ward and two
  inpatient wards, enough to exercise transfers and surge without
  asking a config author to model a whole hospital first."
  {:id :general-hospital
   :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 6
            :surge-format "%s-H%02d" :class :ed}
           {:id :renal :name "Renal" :beds 4 :surge-slots 2
            :surge-format "%s-H%02d" :class :inpatient}
           {:id :cardiology :name "Cardiology" :beds 4 :surge-slots 2
            :surge-format "%s-H%02d" :class :inpatient}]})

;; --- Providers ---------------------------------------------------------

(def ProviderName
  [:map [:family :string] [:given :string]])

(def ProviderTemplate
  "A provider entry before NPI generation -- everything about a
  provider except its identifier, which `materialize-providers` fills
  in from the run's seed."
  [:map
   [:name ProviderName]
   [:role [:enum :attending :consulting :referring]]
   [:specialty :string]
   [:wards [:vector :keyword]]])

(def Provider
  [:map
   [:id :string]
   [:name ProviderName]
   [:role [:enum :attending :consulting :referring]]
   [:specialty :string]
   [:wards [:vector :keyword]]])

(defn valid-provider-template? [template] (m/validate ProviderTemplate template))
(defn valid-provider? [provider] (m/validate Provider provider))

(def default-provider-templates
  "A small pool, each ward-eligible per docs/operational-models.md's
  'ward-eligible providers' rule; Dr. Reyes is deliberately eligible
  everywhere so the default config always has an attending for any
  ward, including the ED wards used only for boarding/surge."
  [{:name {:family "Chen" :given "Amara"} :role :attending
    :specialty "Nephrology" :wards [:renal]}
   {:name {:family "Okafor" :given "David"} :role :attending
    :specialty "Cardiology" :wards [:cardiology]}
   {:name {:family "Reyes" :given "Priya"} :role :attending
    :specialty "Emergency Medicine" :wards [:ed :renal :cardiology]}])

;; --- Synthetic NPIs: Luhn over the 80840 issuer prefix ------------------

(def ^:private npi-issuer-prefix "80840")

(defn- luhn-check-digit
  "The check digit that makes `digits` (a string of decimal digits)
  pass the Luhn checksum once appended. Processes digits right to
  left, doubling every digit at an even 0-based index -- the
  about-to-be-appended check digit will occupy the new rightmost
  (index 0) position, so a digit at index i in `digits` sits at index
  i+1 afterward; doubling on even i here is what lines up with the
  standard Luhn doubling-every-other-digit-from-the-right rule once
  the check digit is in place."
  [digits]
  (let [total (->> digits
                   reverse
                   (map-indexed (fn [i c]
                                  (let [d (- (int c) (int \0))]
                                    (if (even? i)
                                      (let [doubled (* 2 d)]
                                        (if (> doubled 9) (- doubled 9) doubled))
                                      d))))
                   (reduce +))]
    (mod (- 10 (mod total 10)) 10)))

(defn npi-check-digit
  "The NPI standard's check digit for a 9-digit body: Luhn over the
  constant issuer prefix \"80840\" plus the body (docs/operational-
  models.md). E.g. body \"123456789\" -> 3, the well-known example
  NPI 1234567893's own check digit."
  [body9]
  (luhn-check-digit (str npi-issuer-prefix body9)))

(defn valid-npi?
  [npi10]
  (and (string? npi10)
       (= 10 (count npi10))
       (every? #(Character/isDigit ^char %) npi10)
       (= (Character/digit ^char (nth npi10 9) 10)
          (npi-check-digit (subs npi10 0 9)))))

(defn- rand-digit
  [^java.util.Random rng]
  (.nextInt rng 10))

(defn generate-npi
  "Draws 9 random digits from `rng` and appends the correct Luhn check
  digit -- a structurally valid synthetic NPI (docs/operational-
  models.md decision (a)) that would pass any downstream NPI-format
  validator without being assigned to a real provider."
  [rng]
  (let [body (apply str (repeatedly 9 #(rand-digit rng)))]
    (str body (npi-check-digit body))))

(defn materialize-providers
  "The one place provider NPIs are generated: draws one synthetic NPI
  per template from `rng`, in template order (fixed -- determinism),
  and returns the templates with `:id` filled in. Called once per run,
  before the main event loop, so provider identity is as deterministic
  as arrival staggering and bed choice."
  [rng provider-templates]
  (mapv #(assoc % :id (generate-npi rng)) provider-templates))
