(ns ehrt.person-simulator.process
  "`persons`: the run's person-event stream.

  DATA, never state (ADR-0172 section 2). This namespace folds nothing
  and knows nothing of encounters, beds, wards or messages; it walks
  each person's own `:person` stream year by year and returns a
  t-ascending vector the engine may one day fold. Ruling F1: nothing
  folds it today, which is what makes arc 2b's corpus proof possible.

  THE DRAW BLOCK. Eighteen variates per person-year, in this exact
  order, ALWAYS -- fired or not, branch taken or not:

     1. residence-move hazard          10. household branch
     2. residence-address pick         11. pregnancy hazard
     3. employment-change hazard       12. gestation jitter
     4. employment-status branch       13. death hazard
     5. occupation-class branch        14. occupational-injury hazard
     6. coverage-payer pick            15. injury-class branch
     7. name-change hazard             16. identity-unavailable hazard
     8. identity-correction hazard     17. unavailable-window length
     9. household hazard               18. identity-resolution branch

  A person walked for N years consumes exactly 18N draws, whatever
  happens to them -- including dying in year 1, which stops EMISSION
  and not consumption (ruling C1's own phrasing: the person process
  draws its death hazard as always and discards the draw). A newborn
  costs FOUR more, from its own stream, for its derived Persona.

  Within-year instants are derived from the firing variate itself
  (`hazards/within-year-offset`), never drawn: a separately-drawn
  instant would be a variate whose COUNT depends on whether the hazard
  fired, which the fixed-consumption law forbids.

  Streams: every draw comes from `(engine/stream master :person
  id-tag)` and from no other family. Adults present at t0 use their
  arrival ordinal as `id-tag` -- the same key `patient-id-for` already
  uses -- and newborns use `(engine/newborn-id-tag parent-id-tag
  parity-index 0)`, the pair ADR-0171 ruling B1 minted with no caller
  precisely so this arc would inherit it.

  ORDER DEPENDENCE, disclosed. Households are threaded through the
  walk in population order: a person may join a household formed by a
  person walked before them, never after. That makes a person's
  household OUTCOME a function of the population vector's order --
  never their draw COUNT, which is what ADR-0171's own concern was
  about, and never another person's variates. It is ruling B1's stated
  cost (\"a member's address becomes a function of another person's
  stream\"), confined to one household and keyed on a stable id."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [ehrt.person-simulator.clock :as clock]
            [ehrt.person-simulator.hazards :as hz]
            [ehrt.person-simulator.persona :as pp]
            [ehrt.sim-engine.interface :as engine]))

(def places
  "`sim-model`'s own vendored address pool, read as a resource -- the
  flat 24-row weighted pool `ehrt.sim-model.persona` draws a t0
  address from. A residence move draws a whole new row from the SAME
  pool (`rulings.md#R-mix-3`, limitations row 7): no adjacency, no
  distance, no local-versus-cross-country move."
  (edn/read-string (slurp (io/resource "sim-model/demographics/places.edn"))))

(def employment-statuses [:employed :unemployed :retired :student])
(def occupation-classes [:manual :service :professional :clerical])
(def injury-classes [:laceration :fracture :burn :strain])
(def correction-fields [:name :dob])
(def coverage-causes [:employment :age-65 :loss :eligibility])

(def default-merge-fraction
  "Ruling D1's authored default for `:identification {:merge-fraction}`
  -- the share of `:identity-resolution` events taking the `:merge`
  branch rather than `:fill`. A config key and not a constant because
  the merge branch is what composes with churn's own `:merge` into the
  post-merge-shadow surface `docs/dev/traffic-model.md` calls the
  highest-value injectable class, and a tester generating a corpus to
  exercise exactly that needs to dial it up. Consumption is unaffected
  either way: one variate, compared against the threshold, whichever
  branch it takes."
  0.35)

;; --- helpers --------------------------------------------------------------

(defn- pick [coll ^double u]
  (nth coll (min (dec (count coll)) (int (* u (count coll))))))

(defn- adult? [age] (>= age 18))

(defn- payer-pool
  "The payer pool a coverage change draws from at `age`.

  These pools come from CONFIG, not from `sim-model`. Its own
  `under-65-payers` / `sixty-five-plus-payers` are private to
  `ehrt.sim-model.persona` and are NOT on
  `ehrt.sim-model.interface` -- unlike the vendored address and
  given-name tables, which are resources this component reads off the
  classpath. So a run that wants coverage-change traffic supplies the
  same `:payers-under-65` / `:payers-65-plus` keys it already supplies
  to `sim-model/persona`, and a run that supplies neither gets no
  `:coverage-change` events (the variates are still drawn). Recorded
  in the session record as an interface gap, not worked around by
  copying the pools here -- a forked payer pool is the same class of
  drift limitations row 7 forbids for addresses."
  [config age]
  (if (>= age 65)
    (:payers-65-plus config)
    (:payers-under-65 config)))

(defn- ev
  "One person event, with its `:event-id` minted from the person's own
  ordinal counter. Every event carries `:person-id`, `:t` and
  `:participants` -- the last is ADR-0172 section 3's same-subject
  law, the shape `:bed-swap` and `:merge` already use, and it is why a
  household event naming more than one person names every one of them."
  [ordinals person-id kind t payload]
  (let [n (get ordinals person-id 0)]
    [(assoc ordinals person-id (inc n))
     (merge {:event kind
             :person-id person-id
             :t t
             :event-id (str person-id "#" n)
             :participants [person-id]}
            payload)]))

;; --- one person's walk ----------------------------------------------------

(defn- year-variates
  "The eighteen variates of one person-year, drawn in the fixed order
  the namespace docstring tables. Drawn ALL AT ONCE, ahead of any
  emission, which is what makes the fixed-consumption law structural
  rather than a property every branch has to remember to preserve."
  [^java.util.Random rng]
  {:move (.nextDouble rng)            :address (.nextDouble rng)
   :employment (.nextDouble rng)      :status (.nextDouble rng)
   :occupation (.nextDouble rng)      :payer (.nextDouble rng)
   :name-change (.nextDouble rng)     :correction (.nextDouble rng)
   :household (.nextDouble rng)       :household-branch (.nextDouble rng)
   :pregnancy (.nextDouble rng)       :gestation (.nextDouble rng)
   :death (.nextDouble rng)           :injury (.nextDouble rng)
   :injury-class (.nextDouble rng)    :unavailable (.nextDouble rng)
   :unavailable-window (.nextDouble rng) :resolution (.nextDouble rng)})

(def draws-per-person-year
  "Eighteen. Asserted by `year-variates` above and by this component's
  own consumption gate, and stated here so a reader does not have to
  count the `.nextDouble` calls."
  18)

(defn- death-instant
  "The instant this person's own death hazard lands, or nil. Computed
  BEFORE any emission, from variates already drawn -- because a
  pregnancy must know whether its own delivery will fall after the
  mother's death, and a hazard that could only find out later would
  leave a `:pregnancy` with no `:delivery` and break limitations row
  11's bijection.

  Ruling C1: a person whose COMPILED trajectory carries a death takes
  that instant instead, and their own draw is discarded. The variate
  is drawn either way; only the answer changes."
  [config persona age-origin-year start-year variates compiled-death-t]
  (or compiled-death-t
      (let [{:keys [t0 years]} config]
        (first
         (for [y (range (long start-year) (long years))
               :let [age (clock/age-at-year persona age-origin-year y)
                     rate (hz/mortality-rate age)
                     u (:death (nth variates (- y (long start-year))))]
               :when (hz/fires? u rate)]
           (+ (long t0) (* y clock/seconds-per-year) (hz/within-year-offset u rate)))))))

(defn- walk-person
  "Every event `person` produces from `start-year` to the horizon, plus
  the newborns their deliveries mint. Returns
  `{:events [...] :births [...] :households [...] :ordinals {...}
    :draws n}`.

  `roster` is the running household roster the caller threads in, so a
  later-walked person may join an earlier-formed household; `households`
  is what THIS walk added to it."
  [config rng roster ordinals
   {:keys [person-id id-tag start-year age-origin-year persona death-t]}]
  (let [{:keys [t0 years]} config
        t0 (long t0)
        years (long years)
        start-year (long start-year)
        age-origin-year (long (or age-origin-year 0))
        merge-fraction (get-in config [:identification :merge-fraction] default-merge-fraction)
        n-years (max 0 (- years start-year))
        ;; EVERY variate, up front: 18 per person-year, whatever happens.
        variates (vec (repeatedly n-years #(year-variates rng)))
        horizon-end (+ t0 (* years clock/seconds-per-year))
        dead-at (death-instant config persona age-origin-year start-year variates death-t)
        own-death? (and dead-at (nil? death-t))
        alive? (fn [t] (and (or (nil? dead-at) (<= t dead-at)) (< t horizon-end)))]
    (loop [y start-year
           st {:address (:address persona) :payer (:payer persona)
               :employment {:status :unemployed :occupation-class nil}
               :household nil :open-unavailable nil :pending-pregnancy nil
               :parity 0}
           ords ordinals
           acc []
           births []
           added []]
      (if (>= y years)
        {:events acc :births births :households added :ordinals ords
         :draws (* n-years draws-per-person-year)}
        (let [v (nth variates (- y start-year))
              age (clock/age-at-year persona age-origin-year y)
              year-start (+ t0 (* y clock/seconds-per-year))
              year-end (+ year-start clock/seconds-per-year)
              at (fn [u rate] (+ year-start (hz/within-year-offset u rate)))
              emit (fn [[ords acc] kind t payload]
                     (let [[ords' e] (ev ords person-id kind t payload)]
                       [ords' (conj acc e)]))
              move-rate (hz/residence-move-rate age)
              emp-rate (hz/employment-change-rate age)
              preg-rate (if (and (= :female (:sex persona))
                                 (<= (first hz/pregnancy-age-band) age
                                     (second hz/pregnancy-age-band)))
                          hz/pregnancy-rate 0.0)
              injury-rate (if (= :employed (get-in st [:employment :status]))
                            hz/occupational-injury-rate 0.0)
              hh-rate (if (adult? age) hz/household-rate 0.0)
              name-rate (if (adult? age) hz/name-change-rate 0.0)
              member-not-head? (and (:household st) (not (:head? (:household st))))

              ;; 1. a delivery already due this year
              [ords acc births st]
              (let [pg (:pending-pregnancy st)]
                (if (and pg (< (:due-t pg) year-end))
                  (let [parity (:parity-index pg)
                        newborn-id (str person-id "/b" parity)
                        [ords acc] (emit [ords acc] :delivery (:due-t pg)
                                         {:newborn-person-id newborn-id
                                          :parity-index parity
                                          :within-delivery-index 0
                                          :pregnancy-event-id (:event-id pg)
                                          :participants [person-id newborn-id]})]
                    [ords acc
                     (conj births {:parent-person-id person-id
                                   :parent-id-tag id-tag
                                   :parent-payer (:payer st)
                                   :parity-index parity
                                   :newborn-person-id newborn-id
                                   :delivery-t (:due-t pg)
                                   :delivery-event-id (:event-id (peek acc))
                                   :household (:household st)
                                   :address (:address st)
                                   :phone (:phone persona)
                                   :surname (get-in persona [:name :family])})
                     (-> st (assoc :pending-pregnancy nil) (update :parity inc))])
                  [ords acc births st]))

              ;; 2. residence move -- suppressed for a non-head member, whose
              ;;    address follows the head's (ruling B1)
              move-t (at (:move v) move-rate)
              [ords acc st]
              (if (and (hz/fires? (:move v) move-rate) (not member-not-head?) (alive? move-t))
                (let [addr (select-keys (pp/weighted-pick places (:address v))
                                        [:street :city :state :zip])
                      [ords acc] (emit [ords acc] :residence-move move-t
                                       {:address addr :prior-address (:address st)})]
                  [ords acc (assoc st :address addr)])
                [ords acc st])

              ;; 3. employment change, and the coverage change it causes
              emp-t (at (:employment v) emp-rate)
              new-status (pick employment-statuses (:status v))
              [ords acc st]
              (if (and (hz/fires? (:employment v) emp-rate) (alive? emp-t))
                (let [[ords acc] (emit [ords acc] :employment-change emp-t
                                       {:status new-status
                                        :occupation-class (pick occupation-classes (:occupation v))})
                      emp-id (:event-id (peek acc))
                      pool (payer-pool config age)
                      [ords acc st]
                      (if (seq pool)
                        (let [payer (dissoc (pp/weighted-pick pool (:payer v)) :weight)
                              cause (case new-status
                                      :employed :employment
                                      :unemployed :loss
                                      :eligibility)
                              [ords acc] (emit [ords acc] :coverage-change (inc emp-t)
                                               (cond-> {:cause cause :payer payer
                                                        :prior-payer (:payer st)}
                                                 (= :employment cause)
                                                 (assoc :employment-event-id emp-id)))]
                          [ords acc (assoc st :payer payer)])
                        [ords acc st])]
                  [ords acc (assoc st :employment
                                   {:status new-status
                                    :occupation-class (pick occupation-classes (:occupation v))})])
                [ords acc st])

              ;; 4. the deterministic coverage change at 65 -- no hazard, and
              ;;    the payer pools already encode the age linkage
              [ords acc st]
              (let [pool (payer-pool config 65)]
                (if (and (= 65 age) (pos? y) (seq pool) (alive? year-start))
                  (let [payer (dissoc (pp/weighted-pick pool (:payer v)) :weight)
                        [ords acc] (emit [ords acc] :coverage-change year-start
                                         {:cause :age-65 :payer payer
                                          :prior-payer (:payer st)})]
                    [ords acc (assoc st :payer payer)])
                  [ords acc st]))

              ;; 5. identity corrections -- a legal name change and a
              ;;    registrar's data-entry correction, collapsed (row 5)
              name-t (at (:name-change v) name-rate)
              [ords acc st]
              (if (and (hz/fires? (:name-change v) name-rate) (alive? name-t))
                (let [prior (:corrected-name st)
                      value {:family (get-in persona [:name :family])
                             :given (str (get-in persona [:name :given]) "-"
                                         (inc (get st :corrections 0)))}
                      [ords acc] (emit [ords acc] :identity-correction name-t
                                       (cond-> {:field :name :value value
                                                :prior-value (or prior (:name persona))}
                                         prior (assoc :corrects-event-id (:corrects-id st))))]
                  [ords acc (-> st (assoc :corrected-name value
                                          :corrects-id (:event-id (peek acc)))
                                (update :corrections (fnil inc 0)))])
                [ords acc st])
              dob-t (at (:correction v) hz/identity-correction-rate)
              [ords acc]
              (if (and (hz/fires? (:correction v) hz/identity-correction-rate) (alive? dob-t))
                (emit [ords acc] :identity-correction dob-t
                      {:field :dob :value (:dob persona) :prior-value (:dob persona)})
                [ords acc])

              ;; 6. household: form, join or leave
              hh-t (at (:household v) hh-rate)
              [ords acc added st]
              (if (and (hz/fires? (:household v) hh-rate) (alive? hh-t))
                (cond
                  (nil? (:household st))
                  (let [eligible (filterv #(< (:t %) hh-t) roster)]
                    (if (and (< (:household-branch v) 0.5) (seq eligible))
                      (let [h (pick eligible (* 2.0 (:household-branch v)))
                            [ords acc] (emit [ords acc] :household-join hh-t
                                             {:household-id (:household-id h)
                                              :household-event-id (:event-id h)
                                              :participants [person-id (:head-person-id h)]})]
                        [ords acc added
                         (assoc st :household {:household-id (:household-id h) :head? false
                                               :head-person-id (:head-person-id h)
                                               :event-id (:event-id h)}
                                :address (:address h))])
                      (let [hid (str "hh-" person-id "-" (count acc))
                            [ords acc] (emit [ords acc] :household-form hh-t
                                             {:household-id hid
                                              :head-person-id person-id
                                              :member-person-ids [person-id]})
                            e (peek acc)]
                        [ords acc
                         (conj added {:household-id hid :head-person-id person-id
                                      :event-id (:event-id e) :t hh-t :address (:address st)})
                         (assoc st :household {:household-id hid :head? true
                                               :head-person-id person-id
                                               :event-id (:event-id e)})])))

                  (not (:head? (:household st)))
                  (let [h (:household st)
                        [ords acc] (emit [ords acc] :household-leave hh-t
                                         {:household-id (:household-id h)
                                          :household-event-id (:event-id h)
                                          :participants [person-id (:head-person-id h)]})]
                    [ords acc added (assoc st :household nil)])

                  ;; a head's own household hazard fires and nothing happens:
                  ;; dissolution is not modelled in v1, and the variate was
                  ;; consumed either way.
                  :else [ords acc added st])
                [ords acc added st])

              ;; 7. pregnancy -- minted only when its own delivery lands inside
              ;;    the horizon and before any death, so limitations row 11's
              ;;    bijection is a property of the construction
              preg-t (at (:pregnancy v) preg-rate)
              due-t (+ preg-t (clock/days hz/gestation-days)
                       (long (* (- (* 2.0 (:gestation v)) 1.0)
                                (clock/days hz/gestation-jitter-days))))
              [ords acc st]
              (if (and (hz/fires? (:pregnancy v) preg-rate)
                       (nil? (:pending-pregnancy st))
                       (alive? preg-t) (alive? due-t))
                (let [[ords acc] (emit [ords acc] :pregnancy preg-t
                                       {:expected-delivery-t due-t
                                        :parity-index (:parity st)})]
                  [ords acc (assoc st :pending-pregnancy
                                   {:due-t due-t :event-id (:event-id (peek acc))
                                    :parity-index (:parity st)})])
                [ords acc st])

              ;; 8. occupational injury -- employed person-years only
              inj-t (at (:injury v) injury-rate)
              [ords acc]
              (if (and (hz/fires? (:injury v) injury-rate) (alive? inj-t))
                (emit [ords acc] :occupational-injury inj-t
                      {:injury-class (pick injury-classes (:injury-class v))})
                [ords acc])

              ;; 9. an identification window opens ...
              un-t (at (:unavailable v) hz/identity-unavailable-rate)
              [ords acc st]
              (if (and (hz/fires? (:unavailable v) hz/identity-unavailable-rate)
                       (nil? (:open-unavailable st)) (alive? un-t))
                (let [[lo hi] hz/identity-unavailable-window-days
                      win (clock/days (+ lo (long (* (:unavailable-window v) (- hi lo)))))
                      until (+ un-t win)
                      [ords acc] (emit [ords acc] :identity-unavailable un-t
                                       {:until-t until
                                        :alias-name {:family "Doe" :given "Unknown"}})]
                  [ords acc (assoc st :open-unavailable
                                   {:until-t until :event-id (:event-id (peek acc))})])
                [ords acc st])
              ;; ... and closes, on whichever branch ruling D1's ratio picks
              [ords acc st]
              (let [open (:open-unavailable st)]
                (if (and open (< (:until-t open) year-end) (alive? (:until-t open)))
                  (let [merge? (< (:resolution v) merge-fraction)
                        [ords acc] (emit [ords acc] :identity-resolution (:until-t open)
                                         (cond-> {:branch (if merge? :merge :fill)
                                                  :unavailable-event-id (:event-id open)}
                                           merge? (assoc :surviving-person-id person-id)))]
                    [ords acc (assoc st :open-unavailable nil)])
                  [ords acc st]))

              ;; 10. death -- minted ONLY for a person whose compiled trajectory
              ;;     carries none (ruling C1). Nothing after it is emitted; the
              ;;     variates are drawn all the same.
              [ords acc]
              (if (and own-death? (< year-start dead-at) (< dead-at year-end)
                       (< dead-at horizon-end))
                (emit [ords acc] :person-death dead-at {})
                [ords acc])]
          (recur (inc y) st ords acc births added))))))

;; --- the household-move propagation pass (ruling B1) -----------------------

(defn- membership-intervals
  "For each household, `[member-person-id join-t leave-t-or-nil]`, read
  off the `:household-join` / `:household-leave` events already in the
  log. Derived, never carried: a membership that is a function of the
  events is a membership arc 3 can re-derive from a folded log."
  [events]
  (let [joins (filter #(= :household-join (:event %)) events)
        leaves (group-by (juxt :person-id :household-id)
                         (filter #(= :household-leave (:event %)) events))]
    (for [j joins
          :let [k [(:person-id j) (:household-id j)]
                leave (first (sort-by :t (filter #(> (:t %) (:t j)) (get leaves k))))]]
      {:household-id (:household-id j) :person-id (:person-id j)
       :join-t (:t j) :leave-t (:t leave)})))

(defn- propagate-household-moves
  "Ruling B1: the HEAD of household draws a family move once, and every
  member gets a `:residence-move` referencing the head's. One draw per
  household move regardless of household size -- a member joining or
  leaving does not change the head's draw sequence, and a non-member's
  stream is untouched.

  Also completes the household-form rows: `:member-person-ids` and
  `:participants` name every person who ever joined, which is what
  makes ADR-0172 section 3's same-subject law hold for a
  `:household-join` referencing its form."
  [events ordinals]
  (let [members (membership-intervals events)
        by-household (group-by :household-id members)
        head-of (into {} (for [e events :when (= :household-form (:event e))]
                           [(:household-id e) (:person-id e)]))
        covers? (fn [m t] (and (<= (:join-t m) t)
                               (or (nil? (:leave-t m)) (< t (:leave-t m)))))
        [ords extra]
        (reduce
         (fn [[ords extra] head-move]
           (let [hid (some (fn [[h p]] (when (= p (:person-id head-move)) h)) head-of)]
             (if-not hid
               [ords extra]
               (reduce (fn [[ords extra] m]
                         (if (covers? m (:t head-move))
                           (let [[ords e] (ev ords (:person-id m) :residence-move
                                              (:t head-move)
                                              {:address (:address head-move)
                                               :prior-address (:prior-address head-move)
                                               :household-move-event-id (:event-id head-move)
                                               :participants [(:person-id m) (:person-id head-move)]})]
                             [ords (conj extra e)])
                           [ords extra]))
                       [ords extra]
                       (get by-household hid [])))))
         [ordinals []]
         (filter #(and (= :residence-move (:event %))
                       (contains? (set (vals head-of)) (:person-id %)))
                 events))
        member-ids (reduce (fn [acc m] (update acc (:household-id m) (fnil conj []) (:person-id m)))
                           {} members)
        moves-by-id (into {} (for [e extra] [(:household-move-event-id e) true]))
        events' (mapv (fn [e]
                        (cond
                          (= :household-form (:event e))
                          (let [ms (distinct (concat [(:person-id e)]
                                                     (get member-ids (:household-id e) [])))]
                            (assoc e :member-person-ids (vec ms) :participants (vec ms)))

                          (and (= :residence-move (:event e)) (moves-by-id (:event-id e)))
                          (assoc e :participants
                                 (vec (distinct (concat [(:person-id e)]
                                                        (map :person-id
                                                             (filter #(covers? % (:t e))
                                                                     (get by-household
                                                                          (some (fn [[h p]]
                                                                                  (when (= p (:person-id e)) h))
                                                                                head-of)
                                                                          [])))))))
                          :else e))
                      events)]
    [(into events' extra) ords]))

;; --- the front door -------------------------------------------------------

(defn persons
  "ADR-0172 section 2's front door: a timed, t-ascending vector of
  person events. `stream` is the run's `:person`-family stream
  descriptor, `{:master <long>}` -- the master seed and not one
  `java.util.Random`, because a per-person stream is keyed by id-tag
  and `engine/stream-seed` needs the master to derive it.

  Config:

    :t0              engine instant the walk starts at (seconds)
    :years           horizon, in whole years
    :population      [{:person-id .. :id-tag ..} ...], in walk order
    :persona         the `sim-model/persona` config for a t0 Persona
    :payers-under-65 / :payers-65-plus   pools a coverage change draws
                     from; absent means no `:coverage-change` (see
                     `payer-pool`)
    :identification  {:merge-fraction 0.35}   (ruling D1)
    :deaths          {person-id -> instant} -- the COMPILED trajectory's
                     death instants, as DATA (ruling C1). A person named
                     here mints no `:person-death`; their processes stop
                     at that instant instead. The GMF death stays
                     authoritative for anything wire-visible, and this
                     component never requires `patient-simulator` to
                     learn it."
  [config stream]
  (let [master (:master stream)
        {:keys [t0 years population deaths]} config
        t0 (long (or t0 0))
        config (assoc config :t0 t0)]
    (loop [queue (vec population)
           roster []
           ords {}
           acc []]
      (if (empty? queue)
        (let [[events _] (propagate-household-moves acc ords)]
          (vec (sort-by (juxt :t :person-id :event-id) events)))
        (let [{:keys [person-id id-tag start-year persona] :as p} (first queue)
              ;; A newborn arrives with its stream ALREADY POSITIONED: its
              ;; derived Persona drew four from it in the births pass, and the
              ;; walk must continue where those left off. Building a second
              ;; `Random` for the same id-tag would replay those four variates
              ;; as the newborn's first year -- a silent stream collision
              ;; inside one person, which the consumption gate caught.
              rng (or (:rng p) (engine/stream master :person id-tag))
              persona (or persona
                          ;; a t0 adult: sim-model's own 13 (or 16) draws,
                          ;; on this person's own :person stream, through
                          ;; `initial-persona` -- the one construction seam.
                          (pp/initial-persona person-id
                                              {:rng rng :t t0 :master master :id-tag id-tag
                                               :death-t (get deaths person-id)
                                               :persona (:persona config)}))
              start-year (long (or start-year 0))
              {:keys [events births households ordinals]}
              (walk-person config rng roster ords
                           (assoc p :persona persona :start-year start-year
                                  :age-origin-year (long (or (:age-origin-year p) 0))
                                  :death-t (get deaths person-id)))
              ;; every birth mints a full person (ruling A1)
              [ords' acc' queue' roster']
              (reduce
               (fn [[ords acc queue roster] b]
                 (let [birth-year (quot (- (:delivery-t b) t0) clock/seconds-per-year)
                       ;; A newborn delivered in the run's LAST year still
                       ;; ENTERS -- it registers and joins its household -- it
                       ;; just has no whole year left to walk. Skipping the
                       ;; whole person instead would leave a `:delivery` whose
                       ;; `:newborn-person-id` names nobody, which is precisely
                       ;; the dangling reference limitations row 2's closure
                       ;; gate exists to catch.
                       walkable? (< (inc birth-year) (long years))]
                   (let [nb-tag (engine/newborn-id-tag (:parent-id-tag b) (:parity-index b) 0)
                           nb-rng (engine/stream master :person nb-tag)
                           ;; the parent's household, or one constituted by
                           ;; the birth itself if the parent has none
                           [ords acc roster hh]
                           (if-let [h (:household b)]
                             [ords acc roster h]
                             (let [hid (str "hh-" (:parent-person-id b) "-birth" (:parity-index b))
                                   [ords e] (ev ords (:parent-person-id b) :household-form
                                                (:delivery-t b)
                                                {:household-id hid
                                                 :head-person-id (:parent-person-id b)
                                                 :member-person-ids [(:parent-person-id b)]})]
                               [ords (conj acc e)
                                (conj roster {:household-id hid
                                              :head-person-id (:parent-person-id b)
                                              :event-id (:event-id e) :t (:delivery-t b)
                                              :address (:address b)})
                                {:household-id hid :head-person-id (:parent-person-id b)
                                 :event-id (:event-id e)}]))
                           nb-persona (pp/initial-persona
                                       (:newborn-person-id b)
                                       {:rng nb-rng :t (:delivery-t b) :master master
                                        :id-tag nb-tag}
                                       {:household {:surname (:surname b)
                                                    :address (:address b)
                                                    :phone (:phone b)}
                                        :parent-payer (:parent-payer b)
                                        :delivery-t (:delivery-t b)})
                           nb-id (:newborn-person-id b)
                           [ords reg] (ev ords nb-id :person-registered (:delivery-t b)
                                          {:persona nb-persona
                                           :delivery-event-id (:delivery-event-id b)
                                           :participants [nb-id (:parent-person-id b)]})
                           [ords join] (ev ords nb-id :household-join (:delivery-t b)
                                           {:household-id (:household-id hh)
                                            :household-event-id (:event-id hh)
                                            :participants [nb-id (:head-person-id hh)]})]
                       [ords (conj acc reg join)
                        (cond-> queue
                          walkable?
                          (conj {:person-id nb-id :id-tag nb-tag
                                 :rng nb-rng
                                 :start-year (inc birth-year)
                                 ;; ruling A1: the newborn's Persona carries
                                 ;; :age 0 as of ITS OWN birth year, not the
                                 ;; run's year 0.
                                 :age-origin-year birth-year
                                 :persona nb-persona}))
                        roster])))
               [ordinals (into acc events) (vec (rest queue)) (into roster households)]
               births)]
          (recur queue' roster' ords' acc'))))))
