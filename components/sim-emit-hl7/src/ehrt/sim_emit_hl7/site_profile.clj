(ns ehrt.sim-emit-hl7.site-profile
  "Site profiles (docs/site-profiles.md): the 'simulate MY hospital'
  config layer. A site profile changes HOW a fact is said at emit time
  (MSH dialect, code-table overrides, Z-segment content) -- never WHAT
  happened (sim/ADR-0002; docs/site-profiles.md's own applied-at-the-
  emitter argument: all three bind at ehrt.sim-emit-hl7.emit-hl7's render
  call sites, never inside decide/evolve or the ground-truth log).

  Every key is optional; the default profile is the ABSENT profile
  (nil) -- ehrt.sim-emit-hl7.emit-hl7 renders identically whether no
  profile arg is passed, an explicit nil is passed, or {} is passed.
  That three-way agreement is this milestone's own determinism anchor,
  property-tested in emit-hl7-test alongside the stronger two-profile
  invariance property (Task 4): ground truth never depends on which
  site profile, if any, is in effect, because :site-profile is not a
  member of ehrt.sim-engine.config/config-keys at all -- structurally
  incapable of reaching `engine/run`, not merely undertested.

  :naming :surge-format is the one documented exception to 'binds at
  emit time only'. Surge bed ids are baked into ground truth at DECIDE
  time (ehrt.sim-model.facility/surge-slot-ids, a decision that
  pre-dates this namespace -- docs/operational-models.md) -- so unlike
  the other three components, a site profile's naming override
  (`apply-naming`, below) is a FACILITY-CONFIG TRANSFORM a caller
  applies to `:facility` BEFORE ehrt.sim-engine.run/run, not
  something ehrt.sim-emit-hl7.emit-hl7 ever reads. Calling it a
  'config-level compatibility shim' (docs/site-profiles.md,
  .agents/plans/roadmap.md) is exactly this distinction: config-level,
  not emit-time -- it is never auto-wired into `run`, so a caller who
  doesn't apply it sees the exact facility they always would."
  (:require [malli.core :as m]))

;; --- MSH dialect -----------------------------------------------------------

(def MshDialect
  [:map
   [:version {:optional true} :string]
   [:sending-app {:optional true} :string]
   [:sending-facility {:optional true} :string]
   [:receiving-app {:optional true} :string]
   [:receiving-facility {:optional true} :string]
   [:processing-id {:optional true} [:enum "P" "T" "D"]]])

(def default-msh
  "Today's hard-coded MSH values (ehrt.sim-emit-hl7.emit-hl7, pre-site-
  profiles) -- what an absent/empty profile's MSH dialect renders,
  byte-identical to always (SimHospital issue #17's own citation,
  .agents/plans/roadmap.md, is why :version in particular is now a
  configured field rather than a hard-coded emitter constant).
  `:processing-id` (MSH-11, HL7 Table 0103: \"P\" production, \"T\"
  training, \"D\" debugging) defaults to \"P\" -- this project's own
  output has always rendered a literal \"P\" here, pre-dating this
  field's own existence as a configured knob (post-M6, sim/ADR-0014's own
  Task 4); a site profile may override it to \"T\"/\"D\" for a
  training/debugging feed instead. See docs/site-profiles.md's own
  realism-vs-caution paragraph on this specific knob before changing it
  in a config a consumer's own routing logic depends on.

  MSH-12 IS `\"2.4\"` SINCE 2026-08-27 (arc 4 sweep 1,
  `notes/adr/0175-arc-4-emission-add-ons.md` ruling A1, commit 2 of 2).
  It was `\"2.3\"` from this project's first message until then, and the
  flip is a CONFORMANCE EVENT rather than a cosmetic one: there is no
  v2.3 structure library on any classpath in this tree, so HAPI
  resolved every message this project emits to
  `ca.uhn.hl7v2.model.GenericMessage$V23` -- no segment order, no
  cardinality, no required-segment check, no primitive typing. The
  base-structural gate this project SHIPS was structurally vacuous over
  this project's OWN output for its whole life. At `\"2.4\"` every
  message resolves to a real v2.4 structure, which is what
  `ehrt.conformance.v2-structure-resolution-test` asserts corpus-wide.

  A SITE THAT MUST SPEAK 2.3 KEEPS TODAY'S BYTES with `{:msh {:version
  \"2.3\"}}` -- this is a site-profile field precisely so the flip costs
  such a site nothing, and `effective-msh` below is the override path."
  {:version "2.4"
   :sending-app "EHR-TESTING-SIM"
   :sending-facility "SIM"
   :receiving-app ""
   :receiving-facility ""
   :processing-id "P"})

(defn effective-msh
  "`default-msh` overridden field-by-field by `site-profile`'s :msh --
  a nil profile, an empty profile, or a profile with no :msh key all
  render `default-msh` exactly (merge over nil's own :msh is nil,
  `(merge m nil)` is `m`)."
  [site-profile]
  (merge default-msh (:msh site-profile)))

;; --- Code-table overrides (docs/site-profiles.md's rendering-time
;; substitution: the underlying state value never changes, only which
;; code string an emitter writes for it does) --------------------------

(def CodeTableEntry
  [:map [:code :string] [:coding-system {:optional true} :string]])

(def CodeTable
  [:map-of :keyword CodeTableEntry])

(def CodeTables
  [:map
   [:patient-class {:optional true} CodeTable]
   [:discharge-disposition {:optional true} CodeTable]
   ;; ARC 3B SWEEP 2 (ADR-0174 ruling C's A20): NPU-2's own table.
   [:bed-status {:optional true} CodeTable]
   ;; ARC 4 SWEEP 3 (ADR-0175 design (b), ruling B1): the status
   ;; ladder's three tables -- ORC-5, OBR-25 and OBX-11. They are HERE,
   ;; rather than as constants in `ehrt.sim-emit-hl7.emit-hl7`, for the
   ;; reason section 2(b) gives and `:bed-status` already demonstrates:
   ;; the VALUES are not in this tree. `hapi-structures-v24` 2.6.0 ships
   ;; STRUCTURES, not HL7 tables, and tables 0038/0123/0085 appear in no
   ;; jar and no resource on any classpath here. Shipping them as
   ;; declared, overridable site-profile data is the honest form for a
   ;; vocabulary this repository cannot cite: a reader can see exactly
   ;; what was authored and replace it, and nothing anywhere asserts
   ;; these strings as an HL7 citation.
   [:order-status {:optional true} CodeTable]
   [:result-status {:optional true} CodeTable]
   [:observation-result-status {:optional true} CodeTable]
   ;; ARC 4 SWEEP 4 (ADR-0175 ruling B1): SCH-25's own table, and the
   ;; fourth vocabulary this tree ships as data for the same reason the
   ;; three above are -- `hapi-structures-v24` 2.6.0 carries STRUCTURES,
   ;; not HL7 tables, and Table 0278 appears in no jar and no resource
   ;; on any classpath here. Verified as a negative rather than assumed:
   ;; neither jar contains the string "rescheduling" or "did not
   ;; show" anywhere.
   [:appointment-status {:optional true} CodeTable]])

(def standard-patient-class-codes
  "HL7v2 Table 0004 (patient class), today's hard-coded value. This
  project only ever produces :inpatient (docs/patient-state-model.md's
  :class field is set unconditionally at admission, never :outpatient/
  :emergency/etc.), so \"I\" is the only entry this milestone's own
  rendering ever exercises -- the rest of the table is named here so a
  future site profile has somewhere documented to override against
  once other classes are produced."
  {:inpatient {:code "I"} :outpatient {:code "O"} :emergency {:code "E"}
   :preadmit {:code "P"} :recurring {:code "R"} :obstetrics {:code "B"}})

(def standard-discharge-disposition-codes
  "HL7v2 Table 0112 (discharge disposition). This project doesn't yet
  track a differentiated disposition outcome (docs/clinical-
  realities.md's post-mortem entry names codes 20/40-42 for a future
  milestone) -- every discharge today is the one standard case, \"01\"
  (discharged to home/self care)."
  {:discharged-to-home {:code "01"}})

(def standard-bed-status-codes
  "HL7v2 Table 0116 (bed status), NPU-2's own table -- arc 3b sweep 2
  (ADR-0174 ruling C, ADT^A20).

  THE MAPPING IS THIS REPOSITORY'S OWN READING of that table onto the
  engine's four bed states, and it is stated here so a site that reads
  it differently can override rather than fork:

    :occupied  -> \"O\"  Occupied
    :ready     -> \"U\"  Unoccupied -- the bed is turned and available
    :dirty     -> \"K\"  Contaminated -- vacated, not yet cleaned
    :cleaning  -> \"H\"  Housekeeping -- being cleaned right now

  `K` and `H` are the pair a reader is most likely to want moved: some
  sites report the whole vacated-to-available window as `H` and never
  distinguish the two legs. That site sets one override; it does not
  need a different cycle."
  {:occupied {:code "O"} :ready {:code "U"} :dirty {:code "K"} :cleaning {:code "H"}})

(def standard-order-status-codes
  "ORC-5 (Order Status), HL7v2 Table 0038 -- arc 4 sweep 3 (ADR-0175
  design (b)).

  NOT A CITATION. This repository has no copy of Table 0038 to check
  against: no jar on any classpath here carries HL7 tables, only v2.4
  structures. What is authored below is THIS PROJECT'S OWN READING of
  the table's conventional values onto a two-stage order ladder plus a
  terminal, and it is data precisely so a site that reads it
  differently overrides rather than forks
  (`notes/facts-register.md`'s own rule for a value this tree cannot
  verify: state the provenance, never assert the authority).

    :scheduled   -> \"SC\"  In process, scheduled
    :in-progress -> \"IP\"  In process, unspecified
    :final       -> \"CM\"  Completed

  `:final` is present but NOT rendered by anything today: the
  `:order-placed` ORM^O01 stays byte-frozen (author ruling Q3,
  2026-08-16) and the ladder's terminal message is the RESULT, whose
  status lives in OBR-25/OBX-11. It is authored anyway so an override
  of the order ladder does not have to invent a completion code the
  moment a later sweep sends a completion ORM."
  {:scheduled {:code "SC"} :in-progress {:code "IP"} :final {:code "CM"}})

(def standard-result-status-codes
  "OBR-25 (Result Status), HL7v2 Table 0123 -- arc 4 sweep 3 (ADR-0175
  design (b)). Same provenance caveat as `standard-order-status-codes`:
  authored, not cited.

    :preliminary -> \"P\"  Preliminary: a verified early result is
                          available, final not yet obtained
    :in-process  -> \"I\"  No results available; specimen received,
                          procedure incomplete
    :corrected   -> \"C\"  Record coming over is a correction
    :final       -> \"F\"  Final results

  THE DEFAULT LADDER USES `:preliminary`, NOT `:in-process`, and the
  choice is clinical rather than arbitrary. A rung message carries the
  order's OBX analytes -- this project's log holds ONE result per order,
  so a rung restates the values the final message will carry -- and
  \"I\" means precisely that no results are available. \"P\" is the code
  for a verified early value that may still change, which is what a rung
  actually is here. `:in-process` and `:corrected` are authored anyway,
  unused by the default ladder, so a site whose analyzer really does
  send an empty in-process report has a code to override to."
  {:preliminary {:code "P"} :in-process {:code "I"}
   :corrected {:code "C"} :final {:code "F"}})

(def standard-observation-result-status-codes
  "OBX-11 (Observation Result Status), HL7v2 Table 0085 -- arc 4 sweep 3
  (ADR-0175 design (b)). Authored, not cited, same as its two siblings.

  A SEPARATE TABLE FROM `standard-result-status-codes` EVEN THOUGH THE
  DEFAULT VALUES COINCIDE. 0123 and 0085 are two tables in the standard,
  they diverge beyond the four values below, and a site that overrides
  the report-level code has no reason to be forced into overriding the
  per-analyte one with it. Collapsing them would save three lines and
  make one override impossible."
  {:preliminary {:code "P"} :in-process {:code "I"}
   :corrected {:code "C"} :final {:code "F"}})

(def standard-appointment-status-codes
  "SCH-25 (Filler Status Code), HL7v2 Table 0278 -- arc 4 sweep 4
  (ADR-0175 ruling B1). Authored, not cited: same provenance caveat as
  `standard-order-status-codes`, and measured the same way (no jar on
  any classpath here carries HL7 tables).

    :booked    -> \"Booked\"     The appointment is on the schedule
    :cancelled -> \"Cancelled\"  Stopped before it started
    :no-show   -> \"Noshow\"     The slot arrived and the patient did not

  A RESCHEDULE RENDERS `:booked`, NOT A STATUS OF ITS OWN, and that is
  the standard's shape rather than a shortcut: a rescheduled appointment
  is still booked, at a different instant, and SCH-11 is where the move
  shows. The trigger -- SIU^S14 against SIU^S12 -- is what says a move
  happened; SCH-25 says what state the appointment is in afterwards.

  `:complete` and `:pending` are authored and unused by anything this
  project emits: no ground-truth event marks an appointment kept (an
  encounter opening against it is what does, and that is an ADT), so a
  site whose scheduler really does send a completion has a code to
  override to rather than a table to fork."
  {:booked {:code "Booked"} :cancelled {:code "Cancelled"} :no-show {:code "Noshow"}
   :complete {:code "Complete"} :pending {:code "Pending"}})

(defn code-for
  "Renders `state-value` (a keyword) as the ER7 field's component vector
  -- [code] or [code coding-system] -- from `site-profile`'s own
  override table (keyed under `table-key`, one of :patient-class /
  :discharge-disposition) when present, `standard-table`'s matching
  entry otherwise. This IS the code-table-overrides law made code: the
  underlying state value never changes, only which code string renders
  for it, per whichever site profile (if any) is in effect."
  [site-profile table-key standard-table state-value]
  (let [override (get-in site-profile [:code-tables table-key state-value])
        {:keys [code coding-system]} (or override (get standard-table state-value))]
    (if coding-system [code coding-system] [code])))

;; --- Naming: a facility-config transform, NOT an emit-time dialect --
;; see this namespace's own docstring for why :naming is the one
;; documented exception ------------------------------------------------

(def Naming
  [:map [:surge-format {:optional true} :string]])

(defn apply-naming
  "Overrides every ward's :surge-format with `site-profile`'s :naming
  :surge-format, when present -- the profile wins over each ward's own
  facility-level value (docs/site-profiles.md's documented precedence:
  facility-level surge-format is still honored when the profile carries
  no :naming key at all). Absent :naming (or an absent profile
  entirely) is the identity function on `facility` -- a caller who
  doesn't opt into this transform sees the exact same facility they
  always would. A caller applies this BEFORE
  ehrt.sim-engine.run/run, if at all: never automatically wired into
  `run` itself (this is a config-authoring convenience, not a stage the
  engine or emitter knows exists), and never inside decide/evolve."
  [site-profile facility]
  (if-let [fmt (get-in site-profile [:naming :surge-format])]
    (update facility :wards (fn [wards] (mapv #(assoc % :surge-format fmt) wards)))
    facility))

;; --- Z-segment templates (Task 3: the seam) -------------------------------

(def ZSegmentName
  "Z-prefixed, three characters total -- the standards-blessed shape of
  a site-invented segment id (docs/site-profiles.md: \"ZPI\", \"ZDS\",
  whatever a given site invented)."
  [:re #"^Z[A-Z0-9]{2}$"])

(def ZFieldBinding
  "One field of a Z-segment template: `:path` (a vector of keywords,
  looked up via get-in against a per-render context assembled by
  ehrt.sim-emit-hl7.emit-hl7 -- state/persona/event paths, e.g. [:persona
  :payer :type] or [:location :ward]) or `:literal` (a fixed string
  fallback). `:path` wins when both are present. An unbound path (nil
  at any step, or missing entirely) renders an EMPTY field, never
  throws -- docs/site-profiles.md Task 3's own requirement."
  [:map
   [:path {:optional true} [:vector :keyword]]
   [:literal {:optional true} :string]])

(def ZSegmentTemplate
  [:map
   [:segment ZSegmentName]
   [:trigger [:set :keyword]]
   [:fields [:vector ZFieldBinding]]])

(def SiteProfile
  [:map
   [:name {:optional true} :string]
   [:msh {:optional true} MshDialect]
   [:code-tables {:optional true} CodeTables]
   [:naming {:optional true} Naming]
   [:z-segments {:optional true} [:vector ZSegmentTemplate]]])

(defn valid-profile? [profile] (m/validate SiteProfile profile))
(defn explain-profile [profile] (m/explain SiteProfile profile))
