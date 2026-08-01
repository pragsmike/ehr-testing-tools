(ns ehrt.sim.emit-state
  "EmitState (docs/sim-theory.edn): state-history -> FHIR R4 Bundle
  (JSON, via data.json -- no new dep). Format dispatch: FHIR now, CDA is
  the dispatch's other arm, deferred with a contract note (below) rather
  than stubbed.

  Laws:
  1. Snapshot-at-instant: `snapshot-at` is a pure function of REPLAY
     RECORDS (ehrt.sim.engine/replay's own output -- the fold,
     sim/ADR-0008) at a queried instant `t`. No access to the log beyond
     the fold: everything downstream of `snapshot-at` (the resource
     builders, `patient-bundle`) reads ONLY the folded PatientState map
     `replay` already computed -- never a raw ground-truth event's own
     fields, never the RNG, never the clock. `bundle-run` is the one
     convenience call site that also calls `replay` itself, so a caller
     never has to; it does not weaken the law, since `replay` IS the
     fold this law is about, the same one ehrt.sim.check already
     reuses rather than reimplementing (sim/ADR-0008's own precedent).
  2. Format dispatch: FHIR resources now. A CDA arm is real future
     scope (docs/sim-theory.edn's own equation names both), not stubbed
     here -- no XML shell, no half-finished document type. A future
     session adding CDA support does so by adding a sibling emitter
     namespace and a dispatch key, not by extending this one.
  3. Cross-emitter id sub-law (the GLOBAL emitter-coherence law, docs/
     sim-theory.md): every resource id/reference here derives from the
     SAME identifiers ehrt.sim.emit-hl7 renders -- Patient.id is
     the patient-id ehrt.sim.engine/patient-id-for assigns
     (never a fresh UUID), Patient.identifier carries the active-mrn
     ehrt.sim.emit-hl7 renders as PID-3, and every other
     resource's own id is a deterministic, patient-id-scoped ordinal
     (Encounter/Condition/Observation/MedicationRequest), never
     invented independently of state. Property-tested in
     emit-state-test/fhir-patient-id-and-active-mrn-resolve-to-the-
     same-hl7-identity.
  4. Minimal-but-valid, no invented fields: the rendered resource set is
     exactly Patient/Encounter/Condition/Observation/MedicationRequest/
     Coverage, each populated ONLY from fields ehrt.sim.engine's
     own fold already carries (this namespace's own header comment on
     ehrt.sim.engine/PatientState records what had to LAND in
     the fold for this reason). Procedure is deliberately absent --
     ground truth carries :procedure events, but nothing in this
     resource set needs them, and inventing a seventh resource type
     nobody asked for would violate this law for its own sake.
  5. Standards-native test-data marking (post-M6, sim/ADR-0014): EVERY
     resource this namespace renders carries `meta.security` (the
     standard FHIR/HL7 HTEST \"test health data\" label, verified
     against terminology.hl7.org before landing --
     notes/facts-register.md F14) and `meta.tag` (this run's own
     seed/run-id, system \"urn:ehrt.sim\") -- generator and
     provenance on every resource, so a real system that ever received
     this data could find and purge it by a standard security-label
     query alone, never a project-specific convention only this repo
     knows to look for. Applied as a POST-PROCESSING step over already-
     built resources (`with-test-meta`, below), deliberately NOT
     threaded into the individual resource builders: a run's own seed
     is a property of the RUN, not of any patient's own folded state,
     so threading it into `patient-resource`/`encounter-resource`/etc.
     would violate law 4 above (\"populated ONLY from fields the fold
     already carries\") for a concern that has nothing to do with
     clinical content."
  (:require [ehrt.sim.engine :as engine]))

;; --- Law 1: snapshot-at-instant -------------------------------------------

(defn snapshot-at
  "{patient-id -> PatientState} as it stood at simulated instant `t`,
  given `replay-records` (ehrt.sim.engine/replay's own output).
  Pure: the last record whose own event occurred at or before `t`
  supplies its `:world-after`; no applicable record (t before this
  run's first event, or an empty run) -> {}."
  [replay-records t]
  (let [applicable (take-while #(<= (:t (:event %)) t) replay-records)]
    (if (seq applicable) (:world-after (last applicable)) {})))

;; --- CodeableConcept / timestamp rendering ---------------------------------

(defn- coding-system-uri
  "Concept :system keyword -> FHIR's own canonical coding-system URI
  (the standard mappings for this project's four code systems, sim/ADR-0002's
  native-code-rendering law extended to FHIR: never translated, only
  its SYSTEM identifier is format-specific)."
  [system]
  (case system
    :snomed "http://snomed.info/sct"
    :loinc "http://loinc.org"
    :rxnorm "http://www.nlm.nih.gov/research/umls/rxnorm"
    :icd10cm "http://hl7.org/fhir/sid/icd-10-cm"
    :cvx "http://hl7.org/fhir/sid/cvx"
    (name system)))

(defn- codeable-concept
  [concepts]
  {:coding (mapv (fn [{:keys [system code display]}]
                   (cond-> {:system (coding-system-uri system) :code code}
                     display (assoc :display display)))
                 concepts)})

(def ^:private iso-formatter java.time.format.DateTimeFormatter/ISO_LOCAL_DATE_TIME)

(defn- iso-timestamp
  "The same pinned-clock arithmetic ehrt.sim.emit-hl7/hl7-timestamp
  uses (reference-date + seconds, suffixed with the pinned utc-offset) --
  rendered in ISO-8601 (FHIR's own dateTime shape) instead of HL7's
  colon-free zone convention. Kept independent of emit-hl7's own
  formatter (no cross-emitter code dependency) -- two renderings of one
  pinned clock, not one emitter calling the other."
  [reference-date seconds utc-offset]
  (str (.format (.plusSeconds (.atStartOfDay (java.time.LocalDate/parse reference-date)) (long seconds))
                iso-formatter)
       utc-offset))

;; --- Resource builders: pure functions of one patient's own folded state -

(defn- patient-resource
  [{:keys [patient-id active-mrn persona]}]
  (when persona
    {:resourceType "Patient"
     :id patient-id
     :identifier [{:system "urn:ehrt.sim:mrn" :value active-mrn}]
     :name [{:family (:family (:name persona)) :given [(:given (:name persona))]}]
     :gender (case (:sex persona) :female "female" :male "male")
     :birthDate (:dob persona)
     :address [{:line [(:street (:address persona))]
                :city (:city (:address persona))
                :state (:state (:address persona))
                :postalCode (:zip (:address persona))}]
     :telecom [{:system "phone" :value (:phone persona)}]}))

(def ^:private fhir-encounter-class
  "PatientState's own :class enum -> FHIR R4's v3-ActCode Encounter.class
  (only :inpatient/:outpatient are ever set by this project's own
  evolve methods today -- :emergency/:preadmit/:recurring/:obstetrics
  are reserved enum values no step type produces yet)."
  {:inpatient "IMP" :emergency "EMER" :outpatient "AMB"})

(defn- encounter-resource
  [reference-date utc-offset {:keys [patient-id status class location admitted-at discharged-at]}]
  (when admitted-at
    (cond-> {:resourceType "Encounter"
             :id (str patient-id "-encounter")
             :status (if (= :discharged status) "finished" "in-progress")
             :class {:code (fhir-encounter-class class)}
             :subject {:reference (str "Patient/" patient-id)}
             :period (cond-> {:start (iso-timestamp reference-date admitted-at utc-offset)}
                       discharged-at (assoc :end (iso-timestamp reference-date discharged-at utc-offset)))}
      location (assoc :location [{:location {:display (:ward location)}}]))))

(defn- condition-resources
  [reference-date utc-offset {:keys [patient-id conditions]}]
  (map-indexed
   (fn [i {:keys [codes onset-t end-t clinical-status]}]
     (cond-> {:resourceType "Condition"
              :id (str patient-id "-condition-" i)
              :subject {:reference (str "Patient/" patient-id)}
              :encounter {:reference (str "Encounter/" patient-id "-encounter")}
              :clinicalStatus {:coding [{:code (name clinical-status)}]}
              :onsetDateTime (iso-timestamp reference-date onset-t utc-offset)}
       (seq codes) (assoc :code (codeable-concept codes))
       end-t (assoc :abatementDateTime (iso-timestamp reference-date end-t utc-offset))))
   conditions))

(defn- observation-resources
  [reference-date utc-offset {:keys [patient-id observations]}]
  (map-indexed
   (fn [i {:keys [codes t value unit reference-range interpretation]}]
     (cond-> {:resourceType "Observation"
              :id (str patient-id "-obs-" i)
              :status "final"
              :subject {:reference (str "Patient/" patient-id)}
              :encounter {:reference (str "Encounter/" patient-id "-encounter")}
              :code (codeable-concept codes)
              :effectiveDateTime (iso-timestamp reference-date t utc-offset)}
       (some? value) (assoc :valueQuantity (cond-> {:value value} unit (assoc :unit unit)))
       reference-range (assoc :referenceRange [{:low {:value (:low reference-range)}
                                                 :high {:value (:high reference-range)}}])
       interpretation (assoc :interpretation
                              [{:coding [{:code (case interpretation :normal "N" :low "L" :high "H")}]}])))
   observations))

(defn- medication-request-resources
  [reference-date utc-offset {:keys [patient-id medication-orders]}]
  (map-indexed
   (fn [i {:keys [codes ordered-t status]}]
     (cond-> {:resourceType "MedicationRequest"
              :id (str patient-id "-med-" i)
              :status (name status)
              :intent "order"
              :subject {:reference (str "Patient/" patient-id)}
              :encounter {:reference (str "Encounter/" patient-id "-encounter")}
              :authoredOn (iso-timestamp reference-date ordered-t utc-offset)}
       (seq codes) (assoc :medicationCodeableConcept (codeable-concept codes))))
   medication-orders))

(defn- coverage-resource
  [{:keys [patient-id persona]}]
  (when-let [payer (:payer persona)]
    {:resourceType "Coverage"
     :id (str patient-id "-coverage")
     :status "active"
     :beneficiary {:reference (str "Patient/" patient-id)}
     :payor [{:display (:name payer)}]
     :type {:text (name (:type payer))}}))

;; --- Law 5: standards-native test-data marking -----------------------------

(def ^:private htest-security
  "The standard FHIR/HL7 test-data security label -- HL7 v3-ActReason
  HTEST, display \"test health data\" (notes/facts-register.md F14,
  verified against terminology.hl7.org before landing, per this
  project's own no-guessing-codes rule). Carried on every resource this
  namespace renders (law 5, above)."
  {:system "http://terminology.hl7.org/CodeSystem/v3-ActReason"
   :code "HTEST"
   :display "test health data"})

(defn- run-tag
  "This run's own provenance tag: system \"urn:ehrt.sim\", code =
  the run's seed/run-id (rendered as a string -- FHIR's own Coding.code
  is a string, never a bare number)."
  [run-id]
  {:system "urn:ehrt.sim" :code (str run-id)})

(defn- with-test-meta
  "Merges law 5's own meta.security/meta.tag onto one already-built
  resource. Merge, not overwrite: no builder above sets :meta today, so
  this is additive in practice, but merging (not assoc-ing a bare
  {:security ... :tag ...}) means a future resource type that DOES
  carry its own :meta content composes rather than silently losing it."
  [run-id resource]
  (update resource :meta merge {:security [htest-security] :tag [(run-tag run-id)]}))

(defn patient-bundle
  "One patient's own state -> a FHIR Bundle (type \"collection\") of
  every resource that state actually holds -- Patient/Coverage whenever
  a persona has folded (M4's :registered, always this patient's first
  event); Encounter once admitted; Condition/Observation/
  MedicationRequest exactly as many as the accumulator carries, zero
  otherwise. No resource is emitted speculatively. `run-id` (this run's
  own seed) is stamped onto every resource's own meta.security/meta.tag
  (law 5) -- a run-level fact, applied after the resource builders run,
  never threaded into them (see law 5's own docstring note)."
  [reference-date utc-offset run-id patient-state]
  (let [resources (concat
                    (some-> (patient-resource patient-state) vector)
                    (some-> (encounter-resource reference-date utc-offset patient-state) vector)
                    (condition-resources reference-date utc-offset patient-state)
                    (observation-resources reference-date utc-offset patient-state)
                    (medication-request-resources reference-date utc-offset patient-state)
                    (some-> (coverage-resource patient-state) vector))]
    {:resourceType "Bundle"
     :type "collection"
     :entry (mapv (fn [r] {:resource (with-test-meta run-id r)}) resources)}))

(defn bundle-run
  "The stage function: ground-truth log -> {patient-id -> Bundle}, one
  per patient who exists (has folded at least a :registered event) at
  or before `t`. `t` may be an explicit instant (seconds from run
  start) or `:end` (this run's own last event time) -- the CLI's own
  `sim run --emit fhir [--at ...]` convenience. `run-id` (this run's
  own seed) is stamped onto every resource, per law 5. Calls
  ehrt.sim.engine/replay exactly once (the fold); every bundle
  is a pure projection of that single call's own output, per
  `snapshot-at`'s law."
  [ground-truth reference-date utc-offset run-id t]
  (let [t (if (= :end t) (reduce max 0 (map :t ground-truth)) t)
        snapshot (snapshot-at (engine/replay ground-truth) t)]
    (into {} (map (fn [[patient-id state]] [patient-id (patient-bundle reference-date utc-offset run-id state)])) snapshot)))
