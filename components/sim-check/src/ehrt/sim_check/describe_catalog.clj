(ns ehrt.sim-check.describe-catalog
  "The family catalog `ehrt.sim-check.describe` reports, as DATA in one
  file (ADR-0183). Each row says how a family is made POSSIBLE by the
  configuration and how it is OBSERVED in the log, so the report can
  set the two side by side -- including a family that was configured
  and has no witness at all, which is the row a consumer most needs and
  the one a hand census never printed.

  A ROW IS

    :group          what kind of family (`:churn`, `:scheduling`,
                    `:opt-in`, `:measure`)
    :family         its name
    :cites          the invariant (or engine site) that defines what the
                    observed predicate reads -- every predicate here is
                    one the catalog or the engine already states, never
                    a new reading of the log
    :configured-by  (fn [configuration]) -> `:yes`, `:no`, `:unknown`,
                    or `:emergent` (a measure: a situation no config key
                    turns on by itself). `configuration` is nil when the
                    input names no configuration at all, and that is
                    `:unknown`, never a guess.
    :observed-by    (fn [ctx record]) -> truthy when `record` (one
                    `engine/replay` record, plus its `:index`) is a
                    witness of the family. `ctx` carries the log and the
                    few per-patient facts `describe`'s one pass keeps.
    :witness-fields the event keys a witness copies into its `:related`
    :closed-by      OPTIONAL (fn [ctx record]) -> a patient-id. A row
                    that has one counts witnesses that were never
                    CLOSED: a candidate the predicate opened for a
                    patient is withdrawn by a later record that closes
                    it for that patient.

  `configuration`, when present, is `{:source .. :opts .. :churn-profile
  ..}`: `:opts` the run's merged options, `:churn-profile` the RESOLVED
  churn profile (nil for churn off, `:unknown` when the flag that could
  have turned it on is not recorded anywhere) -- resolved by
  `ehrt.sim.run`, which owns that resolution, and handed in rather than
  re-derived here."
  (:require [ehrt.sim-check.check :as check]))

(defn- opted-in
  "`:yes` when the run's options carry `k` truthy, `:no` when they do
  not, `:unknown` when there is no configuration to read. Every key this
  is used for is config-only (no CLI flag), so a caller-supplied config
  file settles it."
  [k]
  (fn [configuration]
    (cond (nil? configuration) :unknown
          (get-in configuration [:opts k]) :yes
          :else :no)))

(defn- churn-rate
  [step-type]
  (fn [{:keys [churn-profile] :as configuration}]
    (cond (nil? configuration) :unknown
          (= :unknown churn-profile) :unknown
          (pos? (double (get churn-profile step-type 0.0))) :yes
          :else :no)))

(defn- scheduling-rate
  [path]
  (fn [configuration]
    (cond (nil? configuration) :unknown
          (nil? (get-in configuration [:opts :scheduling])) :no
          (pos? (double (or (get-in configuration (into [:opts :scheduling] path)) 0))) :yes
          :else :no)))

(defn- authored-steps
  "Every step the run's authored pathway(s) carry, however they were
  given (`:pathway`, or `:pathways`' weighted and per-ordinal entries)."
  [opts]
  (let [pathways (cond-> (mapv :pathway (:pathways opts))
                   (:pathway opts) (conj (:pathway opts)))]
    (mapcat :steps pathways)))

(defn- immunization-configured
  "`:yes` when an authored step administers one; otherwise `:unknown`
  when modules are configured -- a module may carry a Vaccine state and
  this report does not open module files -- and `:no` when neither can
  produce one."
  [configuration]
  (let [opts (:opts configuration)]
    (cond (nil? configuration) :unknown
          (some #(= :immunization (:type %)) (authored-steps opts)) :yes
          (seq (:modules opts)) :unknown
          :else :no)))

(defn- allergy-onset-configured
  "`:immunization-configured`'s reading for ADR-0184's kind: `:yes` when an
  authored step records one; `:unknown` when modules are configured -- a
  module may carry an AllergyOnset state, and this report does not open
  module files -- and `:no` when neither can produce one."
  [configuration]
  (let [opts (:opts configuration)]
    (cond (nil? configuration) :unknown
          (some #(= :allergy-onset (:type %)) (authored-steps opts)) :yes
          (seq (:modules opts)) :unknown
          :else :no)))

(defn- kind? [k] (fn [_ctx {:keys [event]}] (= k (:event event))))

(def ^:private emergent (constantly :emergent))

(def ^:private terminal-statuses
  "A subject in one of these holds no bed and is not coming back."
  #{:discharged :expired})

(defn crosses-a-merge?
  "ADR-0179 R-inv's condition over one `:result-available`: the order
  it cites names a subject OTHER than the result's, joined to the
  result's subject by a merge no later than the result's own `:t` --
  read through `check/resolves-through-merges?`, the same resolution
  `result-references-existing-order-and-follows-it-in-time` uses."
  [{:keys [log merges]} event]
  (let [subject (:patient-id (first (:participants event)))
        target (get log (:order-event-id event))]
    (boolean
     (and target
          (= :order-placed (:event target))
          (some (fn [{:keys [patient-id]}]
                  (and patient-id
                       (not= patient-id subject)
                       (check/resolves-through-merges? merges patient-id subject (:t event))))
                (:participants target))))))

(def rows
  "The fixed rows, in report order within each group. Module and pathway
  rows depend on the configuration and are built by `module-rows` and
  `pathway-rows`."
  [;; --- churn: the six step types `churn/sample-profile` names --------
   {:group :churn :family :cancel-admit
    :cites "churn step :cancel-admit (decide :cancel-admit)"
    :configured-by (churn-rate :cancel-admit)
    :observed-by (kind? :cancel-admit)
    :witness-fields [:cancels-event-id]}
   {:group :churn :family :cancel-transfer
    :cites "churn step :cancel-transfer (decide :cancel-transfer); excludes :in-error"
    :configured-by (churn-rate :cancel-transfer)
    :observed-by (fn [_ {:keys [event]}] (and (= :cancel-transfer (:event event)) (not (:in-error event))))
    :witness-fields [:cancels-event-id]}
   {:group :churn :family :cancel-discharge
    :cites "churn step :cancel-discharge (decide :cancel-discharge)"
    :configured-by (churn-rate :cancel-discharge)
    :observed-by (kind? :cancel-discharge)
    :witness-fields [:cancels-event-id]}
   {:group :churn :family :transfer-in-error
    :cites "churn step :transfer-in-error (decide :transfer-in-error): the :cancel-transfer carrying :in-error true"
    :configured-by (churn-rate :transfer-in-error)
    :observed-by (fn [_ {:keys [event]}] (and (= :cancel-transfer (:event event)) (true? (:in-error event))))
    :witness-fields [:cancels-event-id]}
   {:group :churn :family :bed-swap
    :cites "churn step :bed-swap (decide :bed-swap)"
    :configured-by (churn-rate :bed-swap)
    :observed-by (kind? :bed-swap)
    :witness-fields []}
   {:group :churn :family :merge
    :cites "churn step :merge (decide :merge): a :merge carrying no :cause (docs: an ordinary merge)"
    :configured-by (churn-rate :merge)
    :observed-by (fn [_ {:keys [event]}] (and (= :merge (:event event)) (nil? (:cause event))))
    :witness-fields []}

   ;; --- scheduling: the four outcomes `config/Scheduling` rates -------
   {:group :scheduling :family :no-show
    :cites "config/Scheduling :no-show-rate"
    :configured-by (scheduling-rate [:no-show-rate])
    :observed-by (kind? :no-show)
    :witness-fields [:appointment-id]}
   {:group :scheduling :family :reschedule
    :cites "config/Scheduling :reschedule-rate"
    :configured-by (scheduling-rate [:reschedule-rate])
    :observed-by (kind? :reschedule)
    :witness-fields [:appointment-id]}
   {:group :scheduling :family :appointment-cancel
    :cites "config/Scheduling :cancel-rate"
    :configured-by (scheduling-rate [:cancel-rate])
    :observed-by (kind? :appointment-cancel)
    :witness-fields [:appointment-id]}
   {:group :scheduling :family :follow-up
    :cites "config/Scheduling :follow-up (decide :discharge books it with :reason \"Follow-up\")"
    :configured-by (scheduling-rate [:follow-up :rate])
    :observed-by (fn [_ {:keys [event]}] (and (= :appointment (:event event)) (= "Follow-up" (:reason event))))
    :witness-fields [:appointment-id]}

   ;; --- the other opt-in keys (docs/consuming-ground-truth.md) --------
   {:group :opt-in :family :bed-cycle
    :cites "config-keys :bed-cycle"
    :configured-by (opted-in :bed-cycle)
    :observed-by (kind? :bed-status-change)
    :witness-fields [:bed]}
   {:group :opt-in :family :repeat-encounter
    :cites "admission-only-when-no-open-encounter: an opener whose subject was :discharged before it"
    :configured-by (opted-in :encounters)
    :observed-by (fn [_ {:keys [event before]}]
                   (and (check/encounter-openers (:event event)) (= :discharged (:status before))))
    :witness-fields []}
   {:group :opt-in :family :persons-bound
    :cites "config-keys :persons: a :registered carrying :person-id"
    :configured-by (opted-in :persons)
    :observed-by (fn [_ {:keys [event]}] (and (= :registered (:event event)) (some? (:person-id event))))
    :witness-fields [:person-id]}
   {:group :opt-in :family :placeholder
    :cites "every-placeholder-registration-is-resolved-or-still-open: a :registered carrying :identity :placeholder"
    :configured-by (opted-in :persons)
    :observed-by (fn [_ {:keys [event]}] (and (= :registered (:event event)) (= :placeholder (:identity event))))
    :witness-fields [:window-close-t]}
   {:group :opt-in :family :immunization
    :cites "clinical-content-only-when-admitted (the :immunization kind)"
    :configured-by immunization-configured
    :observed-by (kind? :immunization)
    :witness-fields [:series]}
   {:group :opt-in :family :allergy-onset
    :cites "clinical-content-only-when-admitted (the :allergy-onset kind)"
    :configured-by allergy-onset-configured
    :observed-by (kind? :allergy-onset)
    :witness-fields [:category]}

   ;; --- measures: situations no key turns on, counted as the tree's
   ;; hand census counted them ----------------------------------------
   {:group :measure :family :result-after-merge
    :cites "result-references-existing-order-and-follows-it-in-time (ADR-0179 R-inv): the cited order names a subject merged into the result's"
    :configured-by emergent
    :observed-by (fn [ctx {:keys [event]}]
                   (and (= :result-available (:event event)) (crosses-a-merge? ctx event)))
    :witness-fields [:order-event-id]}
   {:group :measure :family :pending-result-at-discharge
    :cites "order-only-when-admitted (a result is deliberately not scoped to the stay): the subject is :discharged or :expired as the result lands"
    :configured-by emergent
    :observed-by (fn [ctx {:keys [event before]}]
                   (and (= :result-available (:event event))
                        (not (crosses-a-merge? ctx event))
                        (terminal-statuses (:status before))))
    :witness-fields [:order-event-id]}
   {:group :measure :family :transfer-between-order-and-result
    :cites "transfer-only-when-admitted: a :transfer of the result's subject logged after the cited order and before the result"
    :configured-by emergent
    :observed-by (fn [{:keys [last-transfer]} {:keys [event patient-id]}]
                   (and (= :result-available (:event event))
                        (when-let [t-idx (get last-transfer patient-id)]
                          (when-let [o-idx (:order-event-id event)]
                            (> t-idx o-idx)))))
    :witness-fields [:order-event-id]}
   {:group :measure :family :bed-reoccupied-by-someone-else
    :cites "step-rejected-reason-is-documented: the two -bed-reoccupied rejections log-index/bed-reoccupied-by-someone-else? produces"
    :configured-by emergent
    :observed-by (fn [_ {:keys [event]}]
                   (and (= :step-rejected (:event event))
                        (#{:illegal-cancel-transfer-bed-reoccupied :illegal-cancel-discharge-bed-reoccupied}
                         (:reason event))))
    :witness-fields [:reason]}
   {:group :measure :family :reinstated-stay-without-closer
    :cites "every-encounter-is-opened-and-closed-or-still-open: a :cancel-discharge whose subject is never discharged again"
    :configured-by emergent
    :observed-by (kind? :cancel-discharge)
    :closed-by (fn [_ {:keys [event patient-id]}] (when (= :discharge (:event event)) patient-id))
    :witness-fields [:cancels-event-id]}])

(defn- module-assignment-detail
  "What `:module-assignment` says about one module: how many ordinals
  it names explicitly, their range, and whether a weighted entry could
  also draw it -- set beside `:patients`, which is the cohort a reader
  compares it to."
  [opts module-id]
  (let [entries (filter #(= module-id (:module-id %)) (:module-assignment opts))
        ordinals (sort (keep :patient-ordinal entries))]
    (cond-> {:assigned-ordinals (count ordinals)
             :weighted (boolean (some :weight entries))}
      (seq ordinals) (assoc :ordinal-range [(first ordinals) (last ordinals)])
      (:patients opts) (assoc :patients (:patients opts)))))

(defn module-rows
  "One row per cited module: every module NAME the configuration lists,
  plus every `:citation :module` the log carries that it does not.

  A CITATION IS NOT ONLY A MODULE'S STAMP, and the rows say so. The
  compile layer stamps what a module produced, but an authored pathway
  step may carry a `:citation` of its own (dense-7500's pathways cite
  `dense-inpatient`), and a listed module's submodule cites its own
  name (`medications/otc_pain_reliever`). Neither is a name `:modules`
  lists, and the log does not say which of the two it is -- so such a
  row is `:configured :unknown`, never `:no`."
  [configuration observed-modules]
  (let [opts (:opts configuration)
        configured-names (into (sorted-set) (filter string?) (:modules opts))
        names (into configured-names observed-modules)]
    (for [module-id names
          :let [listed? (contains? configured-names module-id)]]
      (cond-> {:group :module :family module-id
               :cites "events carrying :citation {:module ..}: a compiled module's stamp, or an authored step's own"
               :configured (if listed? :yes :unknown)}
        listed? (assoc :configured-detail (module-assignment-detail opts module-id))
        (and (not listed?) configuration)
        (assoc :configured-reason
               "not a name :modules lists: an authored step's own :citation, or a submodule a listed module calls; the log does not say which")))))

(defn pathway-rows
  "One row per pathway NAME.

  WITHOUT an assignment record (a bare log, or any manifest written
  before slice 2) the row is observed `:unprovable`: no event carries
  the name of the pathway that produced it, so the log cannot prove
  which pathway a patient walked.

  WITH one (ADR-0183 slice 2: the manifest's `:assignments`, the run's
  own record of what each arrival ordinal was assigned), `observed` is
  what the record carries, per name -- `{name {:ordinals n :subjects s
  :witnesses [..]}}`: `:observed` is the ordinals assigned the name,
  `:subjects` the assigned patients with at least one event past
  `:registered`, and the witnesses are those patients' first such
  events. A name the record carries but the configuration does not list
  is the engine's plain `:pathway` default, and reads `:unknown`, never
  `:no`, on the module rows' own ground."
  ([configuration] (pathway-rows configuration nil))
  ([configuration observed]
   (let [opts (:opts configuration)
         configured-names (into (sorted-set)
                                (keep :name)
                                (cond-> (mapv :pathway (:pathways opts))
                                  (:pathway opts) (conj (:pathway opts))))
         names (into configured-names (keys observed))]
     (for [n names
           :let [listed? (contains? configured-names n)]]
       (if observed
         (let [{:keys [ordinals subjects witnesses]} (get observed n)]
           (cond-> {:group :pathway :family n
                    :cites "manifest :assignments (the run's record of each arrival ordinal's pathway), joined to the subjects' events past :registered"
                    :configured (if listed? :yes :unknown)
                    :observed (or ordinals 0)
                    :subjects (or subjects 0)
                    :witnesses (or witnesses [])}
             (not listed?)
             (assoc :configured-reason
                    "not a name :pathway/:pathways lists: the engine's default pathway, which the configuration does not name")))
         {:group :pathway :family n
          :cites "config :pathway/:pathways :name"
          :configured :yes
          :observed :unprovable
          :observed-reason (str "no event field records which pathway produced it (an authored step's "
                                ":citation is free text, not the pathway's name); a manifest assignment record would")})))))
