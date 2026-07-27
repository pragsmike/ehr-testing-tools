# Site profiles: the "simulate MY hospital" config layer

**Landed.** This is the answer to the first question any domain expert
asks on meeting this project: "how do I make it simulate *my*
hospital?" — and, as of the site-profiles milestone
(`.agents/plans/roadmap.md`), a working `--config`-file knob, not just
a design document. `docs/clinical-realities.md`'s site-defined-codes
entry names the reality this layer exists to configure; this document
specifies the config layer itself.

## The reality this layer answers to

Every real hospital extends the standards it nominally conforms to,
and every hospital does it differently. HL7v2 sanctions this directly:
user-defined tables (patient class, table 0004; discharge disposition,
table 0112; location types) are explicitly sites' to extend, and
Z-segments (`ZPI`, `ZDS`, whatever a given site invented) are a
first-class, standards-blessed escape hatch that no universal parser
can know in advance. This isn't a standards failure to route around —
it's the mechanism by which one message grammar serves thousands of
institutions with genuinely different operational vocabularies. A
simulator that hard-codes one hospital's idiom produces traffic that
looks like the same fictional institution every time, which is exactly
the realism gap `docs/problem-statement.md`'s claim #5 (operational
realism) exists to close.

## What exists today

Site-specific configuration is not a new concept this document invents
— it has one citizen already shipped, and this layer is where that
citizen's pattern gets a name and a home for what comes next.

- **`:surge-format`** (`docs/operational-models.md`) is retroactively
  named this layer's **first citizen**: a format string
  (`"%s-H%02d"` by default) a config author supplies to control how
  overflow bed slots are named, because real hospitals name hallway
  beds, pseudo-rooms, and chair codes in wildly site-specific ways,
  and baking one convention into engine code would make every
  generated corpus look like the same institution. `:surge-format`
  already demonstrates the shape every future site-profile knob
  follows: a config value that changes *how something renders or
  names itself*, never *what happened* — the naming idiom is a
  dialect, not a fact.
- **The reserved `:attributes` map** (`docs/patient-state-model.md`) —
  an open `[:map-of :keyword :any]` on the patient accumulator,
  reserved for M5's GMF interpreter but available today as the
  **state-side carrier** a site profile writes to when a custom field
  needs to hold a fact standard accumulator fields don't have a place
  for (e.g., a site-specific ward designation, a locally tracked flag
  a Z-segment will later render). `:donor` (`docs/clinical-
  realities.md`'s post-mortem entry, `docs/patient-state-model.md`'s
  event-validity table) is the first concrete user of this map, ahead
  of any site-profile machinery existing to configure it — a preview
  of the pattern, not itself site-profile content.

## What this layer designs — landed

Four components (`ehr-testing-sim.site-profile`), each extending an
existing idea rather than inventing a new mechanism:

1. **MSH dialect.** A site profile's `:msh` supplies MSH-12 (HL7
   version id) and MSH-3/4/5/6 (sending/receiving app+facility),
   defaulting field-by-field to today's hard-coded values when absent
   (`ehr-testing-sim.site-profile/default-msh`). Version changes the
   MSH-12 literal only — this layer does not restructure segments per
   version (see the today/future table below for that honest scope
   line). SimHospital issue #17's own citation (a HL7v2.6 consumer who
   could see multiple generated schemas but no documented way to
   switch versions) is answered: version selection is a named,
   configured field, never a hard-coded emitter constant.
2. **Code-table overrides.** A site profile supplies its own value set
   for a user-defined HL7 table — patient class (table 0004,
   `docs/patient-state-model.md`'s `:class` field, PV1-2) and
   discharge disposition (table 0112, PV1-36; this project's own
   standard default renders the one case it produces today,
   "discharged to home" — `docs/clinical-realities.md`'s post-mortem
   disposition codes 20/40–42 stay future, since `:expired` isn't a
   landed status yet). Overriding a table is a **rendering-time
   substitution**: the underlying state value (`:class :inpatient`, a
   disposition concept) doesn't change: only which code string an
   emitter writes for it does, per the site profile in effect
   (`ehr-testing-sim.site-profile/code-for`) — the site's own code
   plus an optional coding-system suffix, standard values otherwise.
   This is `:surge-format`'s pattern applied to code tables instead of
   bed-naming strings.
3. **Naming: `:surge-format` migrates to the profile.** A config-level
   compatibility shim (`ehr-testing-sim.site-profile/apply-naming`), not
   an emit-time dialect like the other three — surge bed ids are
   already baked into ground truth at DECIDE time
   (`ehr-testing-sim.facility/surge-slot-ids`, pre-dating this layer),
   so a site profile's `:naming :surge-format` is a facility-config
   TRANSFORM a caller applies BEFORE `ehr-testing-sim.engine/run`, never
   auto-wired into it and never read by the emitter. Facility-level
   `:surge-format` is still honored when a profile carries no `:naming`
   key; the profile wins when both are present. Generalizing naming
   beyond bed slots (ward ids, provider id formats) stays future — see
   the today/future table.
4. **Z-segment templates**, binding a Z-segment's fields to
   state/persona/event paths (`get-in`-style, e.g. `[:persona :payer
   :type]`, `[:location :ward]`) — most naturally, paths into the
   reserved `:attributes` map once M5's interpreter writes to it, but
   not limited to it: any path into the per-render context
   (`ehr-testing-sim.emit-hl7`'s own event map plus the patient's
   persona) resolves today. A template is a declarative mapping
   (`:segment`, `:trigger` — the event types it attaches to — and
   `:fields`, each `:path` or a `:literal` fallback) the emitter reads
   at render time, rendered AFTER standard segments, escaped per ER7,
   an unbound path rendering an EMPTY field rather than throwing. This
   is where a site's fully custom fields (decedent-affairs tracking,
   ME-referral status, whatever a real site's Z-segments carry, or —
   landed as this layer's own worked example — a `ZPI` payer segment)
   become renderable without the engine needing to know what any
   particular site's Z-segment means — only that a template says where
   its values come from. SimHospital issue #21's own citation (a 2024
   user asking how to add GT1 and ZG1) is answered for the mechanism;
   GT1/ZG1 as SHIPPED templates stay future (see below).

**Applied at the emitter, because idiosyncrasy is a rendering
dialect** — with one documented exception. MSH dialect, code-table
overrides, and Z-segment templates all bind at emit time
(`docs/sim-theory.edn`'s `EmitHL7` stage — a real catalytic wire now,
not a declared-ahead-of-time one), never inside `decide`/`evolve` or
the ground-truth log, following directly from ADR-0002's separation of
ground truth from wire format: a site profile changes how a fact is
*said*, never what fact is true — property-tested as this layer's own
invariance claim, below. `:naming :surge-format`'s migration is the
one exception (component 3, above): it binds at facility-config
construction time, before `Execute`, because the fact it touches
(which bed-id string a surge slot gets) was already a decide-time fact
before this layer existed to configure it. The engine and the
invariant catalog (`check.clj`) never need to know which site profile
is active; only the emitter's render call sites (and, for naming, a
caller's own pre-run config-assembly step) do.

## The invariance claim — proven

**Two site profiles over one seed produce the same ground truth in two
accents.** For a fixed `(config, seed)` pair, the ground-truth log is
byte-identical across any two site profiles — a site profile can only
change `hl7v2-stream`'s rendering (code strings, Z-segment content),
never `ground-truth-log`'s content or `check.clj`'s verdict on it. This
is the same "truth is invariant under dialect" argument
[`docs/event-sourcing.md`](event-sourcing.md) makes for emitter
coherence generally, specialized to site profiles as the
dialect-selection mechanism: two hospitals' interfaces can disagree
about what a disposition code looks like on the wire while agreeing,
provably, about what actually happened to the patient.

Checked two ways, not one: the **strong half** (ground truth)
structurally — `:site-profile` is not a member of
`ehr-testing-sim.engine/config-keys`, so it is structurally incapable
of reaching `Execute` at all, not merely untested against it
(`emit-hl7-test/site-profile-never-reaches-the-engine`). The **weak
half** (messages) as a property test over 100 random seeds/patient
counts (`emit-hl7-test/invariance-messages-agree-after-masking-
dialect-surfaces`), comparing a default and a deliberately gaudy second
profile after **masking** exactly the declared dialect surfaces
(MSH-3/4/5/6/12, PV1-2/PV1-36, and Z-segment lines stripped entirely).
The masking function (`emit-hl7-test/mask-dialect-surfaces`) is itself
a deliverable of this claim, not merely test scaffolding: it is the
precise, executable enumeration of what a dialect may touch, and
nothing more — a vaguer masking (blanking whole segments, say) would
prove a weaker, less useful claim. A CLI-produced two-profile demo
(`docs/demos/site-profiles/`) shows the same event rendered under both
profiles side by side, ground-truth identity verified programmatically
when the demo was generated.

## Honest split: today vs. future

**Exists today:** all four landed components above (MSH dialect,
code-table overrides, the `:surge-format` naming migration, Z-segment
templates), the `ehr-testing-sim.site-profile/SiteProfile` schema
bundling them into one named, swappable `:site-profile` config value
(threaded through `ehr-testing-sim.run/run-command` via `--config`, no
CLI flag of its own — the same data-heavy-key passthrough
`:pathway`/`:order-profiles` use), and the invariance property proven
above. The reserved `:attributes` map is still a real, shipped
extension point written only by engine-internal state (`:donor`), not
yet by any site-profile mechanism — a Z-segment template CAN bind a
`:path` into it once something writes there, but nothing does yet.

**Designed, not built:** GT1/ZG1 as SHIPPED Z-segment templates (only
the mechanism landed; a config author writes their own `ZPI`-style
template today, the same way this layer's own worked example does —
no pre-built GT1/ZG1 template ships). MSH-version-driven segment
**restructuring** (a v2.6 profile still gets today's v1-shaped
segments with a different MSH-12 literal, never a genuinely different
segment set) stays future — HL7v2's own version-to-version segment
changes are a large surface this layer doesn't attempt. Generalized
naming idioms beyond `:surge-format` (ward ids, provider id formats)
stay future too.

## Where this lands

Landed between Milestone M4 and Milestone M5 in
`.agents/plans/roadmap.md`, per that document's own session
ratification — after M3, as this document originally reasoned: Z-segment
templates would have been thin content without order/result data to
bind them to (M3's `order-profiles` catalytic, `docs/sim-theory.edn`),
so landing before M3 would have exercised little beyond code-table
overrides.
