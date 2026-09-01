(ns ehrt.sim-model.config
  "Config schemas and shipped defaults for the operational resource
  models (components/sim/docs/operational-models.md): facility (beds -- exclusive),
  providers (shared). Payers land with Persona (M4); not here.

  Also: the synthetic-NPI Luhn math. `sim/ADR-0007` decision (a): provider
  identifiers are structurally valid NPIs (correct Luhn check digit
  over the CMS `80840` health-industry-issuer prefix), generated from
  the run's own seeded RNG -- not an obviously-fake sentinel format.
  `materialize-providers` is the one place a run turns the static,
  id-less `default-provider-templates` into a real provider pool by
  drawing NPIs; called once per run, before the main event loop
  (ehrt.sim-engine.run/run), so provider identity is as deterministic
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
   [:class [:enum :inpatient :ed]]
   ;; ARC 3B SWEEP 2 (ADR-0174 section 2(c), ruling D1): the bed
   ;; TURNAROUND range, in MINUTES, `[lo hi]` inclusive. Housekeeping is
   ;; a property of the WARD, not of the discharge that dirtied the bed
   ;; -- an ED bay and an inpatient room do not turn around at the same
   ;; rate, which is why ruling D1 put this here and drew it on the
   ;; `:facility` stream rather than on `:world`.
   ;;
   ;; ONE key, TWO draws: the cycle has two legs (`:dirty` -> `:cleaning`
   ;; and `:cleaning` -> `:ready`) and each draws INDEPENDENTLY from this
   ;; same range, so a ward's whole turnaround runs `[2*lo, 2*hi]`. Two
   ;; keys would have made a config author state a decomposition of
   ;; housekeeping that no site actually reports separately.
   ;;
   ;; `{:optional true}` on purpose, and it is NOT the bed cycle's
   ;; opt-in: `:bed-cycle` on the RUN CONFIG is (ehrt.sim-engine.engine's
   ;; own `config-keys`). This key is what a ward's cycle is TUNED by
   ;; once that opt-in is taken, and absent it falls back to
   ;; `default-turnaround-minutes` below -- so a facility config written
   ;; before this sweep keeps validating unchanged, which is the same
   ;; law the run-config opt-in follows.
   [:turnaround-minutes {:optional true} [:tuple :int :int]]])

(def default-turnaround-minutes
  "The fallback `:turnaround-minutes` for a ward that declares none, by
  ward CLASS (arc 3b sweep 2, ADR-0174 ruling D1). Per LEG, so the whole
  turnaround is twice each range.

  An ED bay is wiped down between patients in minutes; an inpatient room
  is a terminal clean, and the difference is the reason ruling D1
  rejected one global distribution (`D2`). These two numbers are this
  repository's own shipped default and nothing more -- a site with real
  housekeeping data states its own per ward."
  {:ed [5 15]
   :inpatient [15 30]})

(defn turnaround-minutes
  "`ward`'s own `:turnaround-minutes`, or its class's default. The ONE
  reading of that key: `ehrt.sim-engine.engine`'s cycle draws through
  this and never through `get` directly, so a ward declaring none and a
  ward declaring the default are the same run."
  [ward]
  (or (:turnaround-minutes ward)
      (get default-turnaround-minutes (:class ward))))

(def Facility
  [:map
   [:id :keyword]
   [:wards [:vector Ward]]])

(defn valid-facility? [facility] (m/validate Facility facility))
(defn explain-facility [facility] (m/explain Facility facility))

(def default-facility
  "Small on purpose (components/sim/docs/operational-models.md): one ED ward and two
  inpatient wards, enough to exercise transfers and surge without
  asking a config author to model a whole hospital first."
  {:id :general-hospital
   ;; ARC 3B SWEEP 2 (ADR-0174 ruling D1): every shipped ward states its
   ;; own `:turnaround-minutes` rather than leaning on the class fallback
   ;; -- the values here ARE `default-turnaround-minutes`, written out so
   ;; a reader of this default facility can see the bed cycle's pacing on
   ;; the same page as its bed counts.
   :wards [{:id :ed :name "Emergency" :beds 0 :surge-slots 6
            :surge-format "%s-H%02d" :class :ed :turnaround-minutes [5 15]}
           {:id :renal :name "Renal" :beds 4 :surge-slots 2
            :surge-format "%s-H%02d" :class :inpatient :turnaround-minutes [15 30]}
           {:id :cardiology :name "Cardiology" :beds 4 :surge-slots 2
            :surge-format "%s-H%02d" :class :inpatient :turnaround-minutes [15 30]}]})

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
  "A small pool, each ward-eligible per components/sim/docs/operational-models.md's
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

;; --- Latency (ADR-0109): the second-clock config surface ------------------
;; A LatencyProfile is per-event-type sampling ranges, MINUTES-authored
;; (sim/ADR-0011's own authoring-in-minutes/engine-in-seconds convention,
;; mirrored here for the same ergonomic reason: a config author writes
;; "15 to 45 minutes late", never a raw second count). `:from-minutes`
;; and `:to-minutes` may be fractional (bare `number?`, not `:int`) so a
;; sub-minute range is expressible without forcing a config author to
;; scale everything into seconds by hand.

(def LatencyRange
  [:map
   [:from-minutes number?]
   [:to-minutes number?]])

(def LatencyProfile
  "Keyed by event-type keyword (ehrt.sim-emit-hl7.registry/message-type-
  registry's own vocabulary -- :admission, :discharge, :transfer, etc.);
  an event type absent from the map draws no latency at all (see
  ehrt.sim-emit-hl7.planners/plan-latency's own draw-and-discard law)."
  [:map-of :keyword LatencyRange])

(defn valid-latency-profile? [profile] (m/validate LatencyProfile profile))
(defn explain-latency-profile [profile] (m/explain LatencyProfile profile))

;; --- Chatter (ADR-0175 design (a), arc 4 sweep 2): the re-statement
;; config surface -------------------------------------------------------
;; A ChatterProfile is per-event-type restatement RATES plus one
;; periodic rule. It is EMISSION config, never engine config: nothing
;; here reaches `ehrt.sim-engine.config/config-keys`, and a chatter
;; message restates demographic state the log already carries rather
;; than minting any fact of its own (`rulings.md#R-skeleton-or-
;; emission`). `:chatter` absent from a run's `:config` is the
;; byte-identical path.

(def ChatterProfile
  "The three event-driven keys are RATES in [0,1] -- the probability
  that a given event of that kind produces its restatement -- and an
  absent key means that kind produces none (see
  `ehrt.sim-emit-hl7.planners/plan-chatter`'s own draw-and-discard law:
  the draw still happens, so turning one rule off never shifts
  another's).

  `:restatement` is the PERIODIC half, and it is the half the program's
  A08 volume actually comes from (ADR-0175 section 2(a)):
  `:rate-per-patient-day` restatements per patient-day of open-encounter
  care. It may exceed 1.

  CLOSED, unlike `LatencyProfile`'s open `:map-of`: the four keys here
  are the whole surface, so a typo is a misconfiguration this schema
  can catch outright rather than a rule that silently never fires."
  [:map
   {:closed true}
   [:demographic-update {:optional true} [:and number? [:>= 0] [:<= 1]]]
   [:coverage-change {:optional true} [:and number? [:>= 0] [:<= 1]]]
   [:registered {:optional true} [:and number? [:>= 0] [:<= 1]]]
   [:restatement {:optional true}
    [:map [:rate-per-patient-day [:and number? [:>= 0]]]]]])

(defn valid-chatter-profile? [profile] (m/validate ChatterProfile profile))
(defn explain-chatter-profile [profile] (m/explain ChatterProfile profile))

;; --- Charges (ADR-0175 design (c), arc 4 sweep 2): the DFT^P03 price
;; table ------------------------------------------------------------------
;; A ChargesProfile is EMISSION CONFIG and nothing else. The engine
;; never reads it, no invariant reads it, and a code the table does not
;; price produces a COUNTED SKIP rather than a read-back into the log
;; for something else to bill (`ehrt.sim-emit-hl7.planners/plan-charges`).
;; Invented money is not a clinical fact -- ADR-0175 section 2(c)'s own
;; rejected option (1).

(def ChargesProfile
  "`:price-table` maps a CODE STRING to its price. The codes are the
  ones the log already carries -- an `:order-placed`'s `:concept` code,
  a `:procedure`'s first `:codes` entry -- plus the one reserved key
  `ehrt.sim-emit-hl7.registry/room-and-board-code` for the per-inpatient-
  day line, which is the one charge no log fact carries a code for.

  There is NO DEFAULT PRICE, deliberately: an unpriced code is a
  counted skip, so a half-populated table reads as a number rather than
  as a quietly short DFT."
  [:map
   {:closed true}
   [:price-table [:map-of :string [:map {:closed true}
                                   [:amount number?]
                                   [:display {:optional true} :string]]]]])

(defn valid-charges-profile? [profile] (m/validate ChargesProfile profile))
(defn explain-charges-profile [profile] (m/explain ChargesProfile profile))

;; --- Status ladders (ADR-0175 design (b), arc 4 sweep 3): the
;; order/result restatement config surface ------------------------------
;; A LadderProfile is two vectors of FRACTIONS of an order's own
;; `(:order-placed -> :result-available)` interval. It is EMISSION
;; config and nothing else: `:result-available` carries
;; `:order-event-id`, so both ends of that interval are in the log and a
;; rung at a fixed fraction of it is a PURE FUNCTION OF THE LOG
;; (`rulings.md#R-skeleton-or-emission`). No invariant reads a rung, no
;; key here reaches `ehrt.sim-engine.config/config-keys`, and
;; `ehrt.sim-emit-hl7.planners/plan-ladders` takes NO RNG AT ALL --
;; there is no draw to consume and therefore no fixed-consumption law to
;; obey. ADR-0175 section 2(b)'s rejected option (2) is why the
;; fractions are not sampled: a sampled rung costs a second RNG consumer
;; for no realism, and it makes the rung un-derivable from the log.

(def LadderProfile
  "`:rungs` are ORU^R01 result-status restatements (OBR-25 + OBX-11);
  `:order-rungs` are ORM^O01 order-status restatements (ORC-5). Each is
  a vector of fractions STRICTLY between 0 and 1 -- a rung at 0 would
  land on the order's own instant and one at 1 on the result's, which is
  not a rung but a duplicate of a message that already exists.

  Both are optional and both default to none, so `:ladders` absent, nil
  or `{}` is the byte-identical path -- the same three-way agreement
  `:chatter`, `:latency` and `:site-profile` already have. An order with
  no rung of its own renders exactly today's bytes, INCLUDING its final
  result message: the terminal OBR-25/OBX-11 codes ride the ladder,
  per-order, never the config's mere presence.

  CLOSED, like `ChatterProfile` and for the same reason: two keys are
  the whole surface, so a typo is a misconfiguration this schema catches
  rather than a ladder that silently never fires."
  [:map
   {:closed true}
   [:rungs {:optional true} [:vector [:and number? [:> 0] [:< 1]]]]
   [:order-rungs {:optional true} [:vector [:and number? [:> 0] [:< 1]]]]])

(defn valid-ladder-profile? [profile] (m/validate LadderProfile profile))
(defn explain-ladder-profile [profile] (m/explain LadderProfile profile))

;; --- SIU (ADR-0175 ruling B1, arc 4 sweep 4): the scheduling-message
;; config surface ------------------------------------------------------
;; Scheduling's four kinds are GROUND TRUTH -- `:scheduling` (an engine
;; config key) is what creates them, and it draws. `:siu` creates
;; nothing: it decides whether four events the log already holds are
;; RENDERED. Pure emission (`rulings.md#R-skeleton-or-emission`), no
;; RNG, no `ehrt.sim-engine.config/config-keys` membership, and a run
;; with `:siu` on and `:scheduling` off emits nothing at all, because
;; there is nothing to render.

(def SiuProfile
  "An on/off with an optional allow-list, and nothing else.

  `:siu {}` -- the whole surface, at its default -- renders ALL FOUR
  kinds. `:triggers` narrows that to exactly the kinds named, as ENGINE
  vocabulary rather than HL7 trigger strings: a config author names what
  happened and `ehrt.sim-emit-hl7.registry/message-type-registry`
  owns the trigger.

  UNLIKE `:latency`, `:chatter`, `:charges`, `:ladders` AND
  `:site-profile`, `{}` IS ON, NOT OFF, and the asymmetry is deliberate.
  Those five carry the settings that make them do anything, so an empty
  map has nothing to do; here the KEY'S PRESENCE is the opt-in and
  `:triggers` only narrows it. An empty map that meant `off` would be a
  knob with a silent no-op setting -- exactly the failure mode
  `rulings.md#R-empty-population-is-red` exists to make loud. Absent or
  nil is off, and is byte-identical to every corpus this project shipped
  before this sweep.

  CLOSED, like its four siblings and for the same reason: one key is the
  whole surface, so a typo is a misconfiguration this schema catches
  rather than an SIU stream that silently never fires. An EMPTY
  `:triggers` is rejected for the same reason -- it is indistinguishable
  in effect from `:siu` absent, and a reader who wrote it meant
  something."
  [:map
   {:closed true}
   [:triggers {:optional true}
    [:and [:vector [:enum :appointment :reschedule :appointment-cancel :no-show]]
     [:fn {:error/message "must name at least one kind"} seq]]]])

(defn valid-siu-profile? [profile] (m/validate SiuProfile profile))
(defn explain-siu-profile [profile] (m/explain SiuProfile profile))

;; --- FAN-OUT (ADR-0175 design (f), ruling B1, arc 4 sweep 5): the
;; subscriber table ----------------------------------------------------
;; A filter over an already-rendered stream. It creates no content,
;; reads no state and draws nothing -- the purest emission key in arc 4
;; (`rulings.md#R-skeleton-or-emission`). No member of
;; `ehrt.sim-engine.config/config-keys`, no RNG, no ground truth.
;; `ehrt.sim-emit-hl7.fan-out` carries the SUBSEQUENCE LAW this schema
;; only shapes the declaration of.

(def MshOverrideValue
  "An MSH routing value may not carry any of the ER7 delimiters this
  emitter declares in MSH-2 (`^~\\&`), the field separator, or a
  segment terminator -- writing one would not route a message, it would
  corrupt the segment the mask is defined over. Rejected at config
  time rather than escaped silently, because a subscriber whose
  receiving application is spelled with a `|` is a typo, never an
  intent."
  [:and :string [:re #"^[^|^~\\&\r\n]*$"]])

(def FanOutSubscriber
  "One subscriber: a `:name` (its own spool directory), an optional
  `:filter`, and an optional `:msh` routing override.

  CLOSED at every level, like `:siu`/`:ladders`/`:charges` and for the
  same reason: a misspelled `:message-type` (singular) inside a filter
  would be a subscriber that silently receives EVERYTHING, and a
  misspelled `:recieving-app` a spool that is silently unroutable.

  `:filter` ABSENT means every message -- a full mirror of the base
  spool, which is a legitimate subscriber and the identity case the
  subsequence law is easiest to read on. An EMPTY `:filter` map is
  rejected: it is indistinguishable in effect from an absent one, and a
  reader who wrote it meant something. Same rule for an empty `:msh`,
  an empty `:message-types` and an empty `:patient-classes`.

  `:patient-classes` is PLURAL. ADR-0175 section 2(f)'s own sketch
  spells it `:patient-class`; the session prompt that ruled this sweep
  spells it `:patient-classes`, and the value is a SET, so the plural
  is what the shape actually is. Recorded rather than silently
  reconciled."
  [:map
   {:closed true}
   [:name :keyword]
   [:filter {:optional true}
    [:and
     [:map
      {:closed true}
      [:message-types {:optional true}
       [:and [:set :string] [:fn {:error/message "must name at least one TYPE^TRIGGER"} seq]]]
      [:patient-classes {:optional true}
       [:and [:set [:enum :inpatient :outpatient :emergency :preadmit :recurring :obstetrics]]
        [:fn {:error/message "must name at least one patient class"} seq]]]]
     [:fn {:error/message "an empty :filter means the same as no :filter -- write one or the other"} seq]]]
   [:msh {:optional true}
    [:and
     [:map
      {:closed true}
      [:sending-app {:optional true} MshOverrideValue]
      [:sending-facility {:optional true} MshOverrideValue]
      [:receiving-app {:optional true} MshOverrideValue]
      [:receiving-facility {:optional true} MshOverrideValue]]
     [:fn {:error/message "an empty :msh overrides nothing -- omit the key"} seq]]]])

(def FanOutProfile
  "The subscriber table. A non-empty vector of subscribers with
  DISTINCT `:name`s -- two subscribers sharing a name would write two
  spools into one directory, which is the silent-overwrite failure this
  repository has already paid for once (`spool-sim-output!`'s own
  `msg-%03d` overflow, arc 4 sweep 2). Absent or nil is off, and is
  byte-identical to every corpus this project shipped before this
  sweep."
  [:and
   [:vector FanOutSubscriber]
   [:fn {:error/message "must name at least one subscriber"} seq]
   [:fn {:error/message "subscriber :name values must be distinct"}
    (fn [subs] (= (count subs) (count (into #{} (map :name) subs))))]])

(defn valid-fan-out-profile? [profile] (m/validate FanOutProfile profile))
(defn explain-fan-out-profile [profile] (m/explain FanOutProfile profile))
