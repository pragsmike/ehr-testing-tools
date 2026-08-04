(ns ehrt.sim.identifiers
  "The `sim identifiers` capability (post-M6, `sim/ADR-0014`): config + seed
  -> the complete, machine-readable EDN inventory of every identifier a
  run's own output actually contains -- patient-ids, MRNs (every one
  any patient-id ever held, including merged-away), the bed ids each
  patient's own visit touched (this project's own closest thing to a
  visit id today -- no separate encounter/visit-number field is landed
  yet, docs/GLOSSARY.md's own PV1-19 note), HL7 message control ids,
  FHIR resource ids, provider NPIs, and this run's own meta.tag run-id
  (ehrt.sim.emit-state's HTEST/run-tag law).

  Determinism makes synthetic data ENUMERABLE, not merely recognizable:
  if a run's own output ever reached a real system, this inventory --
  not a search, not forensics -- is the complete, regenerable list of
  every identifier to look for and remove (docs/simulate-your-
  facility.md's own FAQ answer to exactly that question).

  A PROJECTION over `ehrt.sim.engine/run`'s own output, reusing
  this project's existing single-sourced id-derivation functions
  (`ehrt.sim-emit-hl7.interface/control-id-for`, `ehrt.sim.emit-
  state/bundle-run`'s own resource ids) rather than re-deriving any of
  them independently, and reusing `ehrt.sim.run`'s own config-
  merging/module-resolution/incompatible-assignment plumbing (the same
  projection-over-the-same-run posture `run-command` itself has) rather
  than a second config dialect: this verb can never disagree with what
  a real emission of the SAME run actually contains, because it reads
  the exact functions that produce it."
  (:require [ehrt.kernel.interface :as result]
            [ehrt.sim-emit-hl7.interface :as emit-hl7]
            [ehrt.sim.emit-state :as emit-state]
            [ehrt.sim-engine.interface :as engine]
            [ehrt.sim.run :as run]))

(defn- final-world
  "{patient-id -> patient-state} as it stood at the end of the run --
  the same fold `ehrt.sim.emit-state/snapshot-at` computes for
  `:end`, taken directly off `replay`'s own last record rather than a
  second call (this namespace's own single-source-of-truth posture,
  applied to itself)."
  [replay-records]
  (if (seq replay-records) (:world-after (last replay-records)) {}))

(defn- visit-beds
  "patient-id -> the DISTINCT bed ids that patient's own `:location`
  ever named, in first-seen order -- read off `:world-after` at every
  replay step (not the per-record :before/:after convenience view,
  which only tracks the event's PRIMARY participant, sim/ADR-0010; a bed-
  swap's second participant would otherwise be missed)."
  [replay-records patient-ids]
  (into {}
        (map (fn [patient-id]
               [patient-id
                (into [] (distinct)
                      (keep #(get-in % [:world-after patient-id :location :bed]) replay-records))]))
        patient-ids))

(defn- control-ids
  [ground-truth]
  (into (sorted-set) (keep emit-hl7/control-id-for) ground-truth))

(defn- fhir-resource-ids
  [fhir-bundles]
  (into (sorted-set)
        (mapcat (fn [[_ bundle]] (map (comp :id :resource) (:entry bundle))))
        fhir-bundles))

(defn identifiers-command
  "opts: the SAME config surface `ehrt.sim.run/run-command`
  accepts (:seed required, :patients, :config, plus every
  `ehrt.sim.engine/config-keys` entry) -- this is a projection
  over the SAME run, not a parallel config dialect. Runs the simulation
  exactly once (`engine-run-fn`, injectable, the same -fn convention
  `run-command` uses), builds this run's own end-of-run FHIR Bundles
  (`ehrt.sim.emit-state/bundle-run`, the SAME resource-id
  derivation `sim run --emit fhir` uses) so resource ids come from the
  one real builder, and returns Result-wrapped:

    {:run-id             this run's own seed, as a string -- the SAME
                          value ehrt.sim.emit-state's meta.tag
                          carries on every FHIR resource
     :patient-ids        [...], sorted
     :mrns               [...], sorted -- every MRN any patient-id ever
                          held, including one merged away (a merge's
                          own evolve never clears the merged-away
                          patient's :mrns, only marks :status :merged)
     :visit-beds         {patient-id -> [bed-id ...]}, first-seen order
     :control-ids        [...], sorted -- every MSH-10 this run's own
                          messages would carry
     :fhir-resource-ids  [...], sorted
     :provider-npis      [...], sorted}

  Deterministic by construction (same config+seed => identical ground-
  truth/providers => identical inventory, sim/ADR-0002); complete against
  THIS run by construction (every id comes from the same ground-truth/
  bundle-run/materialized-providers a real emission of this run would
  also read, never a separately-maintained list -- proved as a property,
  test/ehrt/sim/identifiers_test.clj)."
  ([opts] (identifiers-command opts {}))
  ([raw-opts {:keys [engine-run-fn] :or {engine-run-fn engine/run}}]
   (let [opts (run/merge-config-file raw-opts)
         {:keys [seed reference-date utc-offset modules module-initial-attributes]} opts
         conflicts (run/incompatible-assignments opts)
         resolved-modules (when modules (run/resolve-modules modules (or module-initial-attributes {})))]
     (cond
       (nil? seed)
       (result/error :missing-required-opt
                     {:message "--seed is required (determinism is a feature, not a default)"
                      :opt :seed})

       (seq conflicts)
       (result/rejected :incompatible-assignment {:conflicts conflicts})

       (and resolved-modules (not (result/ok? resolved-modules)))
       resolved-modules

       :else
       (let [reference-date (or reference-date emit-hl7/default-reference-date)
             utc-offset (or utc-offset emit-hl7/default-utc-offset)
             engine-opts (cond-> (merge (select-keys opts engine/config-keys)
                                        {:seed seed :churn-profile (run/effective-churn-profile opts)})
                           resolved-modules (assoc :modules (:payload resolved-modules)))
             {:keys [ground-truth providers exhausted]} (engine-run-fn engine-opts)]
         (if exhausted
           (result/error :capacity-exhausted exhausted)
           (let [replay-records (engine/replay ground-truth)
                 world (final-world replay-records)
                 patient-ids (vec (sort (keys world)))
                 fhir-bundles (emit-state/bundle-run ground-truth reference-date utc-offset seed :end)]
             (result/ok
              {:run-id (str seed)
               :patient-ids patient-ids
               :mrns (vec (into (sorted-set) (mapcat :mrns) (vals world)))
               :visit-beds (visit-beds replay-records patient-ids)
               :control-ids (vec (control-ids ground-truth))
               :fhir-resource-ids (vec (fhir-resource-ids fhir-bundles))
               :provider-npis (vec (sort (map :id providers)))}))))))))
