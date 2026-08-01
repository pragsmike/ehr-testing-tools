(ns ehrt.sim.site-profile
  "Site profiles (docs/site-profiles.md): the 'simulate MY hospital'
  config layer. A site profile changes HOW a fact is said at emit time
  (MSH dialect, code-table overrides, Z-segment content) -- never WHAT
  happened (sim/ADR-0002; docs/site-profiles.md's own applied-at-the-
  emitter argument: all three bind at ehrt.sim.emit-hl7's render
  call sites, never inside decide/evolve or the ground-truth log).

  Every key is optional; the default profile is the ABSENT profile
  (nil) -- ehrt.sim.emit-hl7 renders identically whether no
  profile arg is passed, an explicit nil is passed, or {} is passed.
  That three-way agreement is this milestone's own determinism anchor,
  property-tested in emit-hl7-test alongside the stronger two-profile
  invariance property (Task 4): ground truth never depends on which
  site profile, if any, is in effect, because :site-profile is not a
  member of ehrt.sim.engine/config-keys at all -- structurally
  incapable of reaching `engine/run`, not merely undertested.

  :naming :surge-format is the one documented exception to 'binds at
  emit time only'. Surge bed ids are baked into ground truth at DECIDE
  time (ehrt.sim.facility/surge-slot-ids, a decision that
  pre-dates this namespace -- docs/operational-models.md) -- so unlike
  the other three components, a site profile's naming override
  (`apply-naming`, below) is a FACILITY-CONFIG TRANSFORM a caller
  applies to `:facility` BEFORE ehrt.sim.engine/run, not
  something ehrt.sim.emit-hl7 ever reads. Calling it a
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
  "Today's hard-coded MSH values (ehrt.sim.emit-hl7, pre-site-
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
  in a config a consumer's own routing logic depends on."
  {:version "2.3"
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
   [:discharge-disposition {:optional true} CodeTable]])

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
  ehrt.sim.engine/run, if at all: never automatically wired into
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
  ehrt.sim.emit-hl7 -- state/persona/event paths, e.g. [:persona
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
