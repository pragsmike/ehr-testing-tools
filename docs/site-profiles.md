# Site profiles: the "simulate MY hospital" config layer

**Landed.** This is the answer to the first question any domain expert
asks on meeting this project: "how do I make it simulate *my*
hospital?" — and, as of the site-profiles milestone
(`.agents/plans/roadmap.md`), a working `--config`-file knob, not just
a design document. `clinical-realities.md`'s site-defined-codes
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

Four components (`ehrt.sim-emit-hl7.site-profile`), each extending an
existing idea rather than inventing a new mechanism:

1. **MSH dialect.** A site profile's `:msh` supplies MSH-12 (HL7
   version id), MSH-3/4/5/6 (sending/receiving app+facility), and
   MSH-11 (processing id — post-M6, ADR-0014's own Task 4 addition),
   defaulting field-by-field to today's hard-coded values when absent
   (`ehrt.sim-emit-hl7.site-profile/default-msh`). Version changes the
   MSH-12 literal only — this layer does not restructure segments per
   version (see the today/future table below for that honest scope
   line). SimHospital issue #17's own citation (a HL7v2.6 consumer who
   could see multiple generated schemas but no documented way to
   switch versions) is answered: version selection is a named,
   configured field, never a hard-coded emitter constant.

   **MSH-11 (`:processing-id`, HL7 Table 0103: "P" production, "T"
   training, "D" debugging) defaults to "P" and is configurable to "T"
   or "D".** A realism-vs-caution trade worth naming explicitly, since
   it differs from every other dialect knob on this page: MSH-3/4/5/6/12
   and the code-table/Z-segment knobs change how a fact is *said* with
   no bearing on whether a receiving system treats the message
   differently in kind, but MSH-11 is a field real interface engines
   and downstream systems are *built* to route on — a training or
   debugging processing id can (correctly, by HL7 convention) steer a
   message to a sandbox environment, suppress a production side effect,
   or otherwise change how the receiving system behaves. That is
   exactly the point of the field, and also exactly why changing it
   under test can silently defeat the test: a corpus generated with
   `:processing-id "T"` against a system that special-cases training
   traffic is no longer exercising the same code path production
   traffic would hit. Leave this at its "P" default when the goal is
   testing production-shaped behavior; set it deliberately, and record
   why, when the goal is exercising a receiving system's own
   training/debug handling.
2. **Code-table overrides.** A site profile supplies its own value set
   for a user-defined HL7 table — patient class (table 0004,
   `docs/patient-state-model.md`'s `:class` field, PV1-2) and
   discharge disposition (table 0112, PV1-36; this project's own
   standard default renders the one case it produces today,
   "discharged to home" — `clinical-realities.md`'s post-mortem
   disposition codes 20/40–42 stay future, since `:expired` isn't a
   landed status yet). Overriding a table is a **rendering-time
   substitution**: the underlying state value (`:class :inpatient`, a
   disposition concept) doesn't change: only which code string an
   emitter writes for it does, per the site profile in effect
   (`ehrt.sim-emit-hl7.site-profile/code-for`) — the site's own code
   plus an optional coding-system suffix, standard values otherwise.
   This is `:surge-format`'s pattern applied to code tables instead of
   bed-naming strings.
3. **Naming: `:surge-format` migrates to the profile.** A config-level
   compatibility shim (`ehrt.sim-emit-hl7.site-profile/apply-naming`), not
   an emit-time dialect like the other three — surge bed ids are
   already baked into ground truth at DECIDE time
   (`ehrt.sim.facility/surge-slot-ids`, pre-dating this layer),
   so a site profile's `:naming :surge-format` is a facility-config
   TRANSFORM a caller applies BEFORE `ehrt.sim.engine/run`, never
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
   (`ehrt.sim-emit-hl7.emit-hl7`'s own event map plus the patient's
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
the ground-truth log, following directly from `sim/ADR-0002`'s separation of
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

## Two knob classes: dialect vs. site config

The `:naming :surge-format` exception above is not merely one odd
component among four — landing it is what surfaced that this layer
actually has **two classes of knob**, not one, and every knob this
layer has shipped or will ever ship belongs to exactly one:

1. **Dialect knobs** — bound at emit time, inside `EmitHL7`'s own
   render call sites, and **truth-invariant**: changing one can only
   change how `hl7v2-stream` renders a fact, never what fact
   `ground-truth-log` records. MSH dialect, code-table overrides, and
   Z-segment templates are this layer's three dialect knobs. A dialect
   knob's surface is, by definition, exactly what the masking function
   (`emit-hl7-test/mask-dialect-surfaces`) strips before the weak-half
   invariance property compares two profiles' output — the masking
   function's own declared surface (MSH-3/4/5/6/11/12, PV1-2/PV1-36,
   Z-segment lines) **is** the enumeration of every dialect knob this
   layer has shipped to date — MSH-11 (`:processing-id`) joined this
   surface post-M6 (ADR-0014's own Task 4), the fourth dialect knob the
   paragraph below already anticipated; adding a fifth means
   extending that function in the same change, or the invariance
   property test stops actually covering it.
2. **Site config knobs** — bound before `Execute` runs, at
   facility-config (or other pre-run config) construction time, and
   **truth-affecting**: a site config knob changes what
   `ground-truth-log` itself contains, because the fact it touches was
   already decided at `decide`-time before this layer existed to
   configure it. `:naming :surge-format`'s migration to the profile
   (`ehrt.sim-emit-hl7.site-profile/apply-naming`) is this class's first
   citizen — surge bed ids are baked into ground truth at `decide`-time
   (`ehrt.sim.facility/surge-slot-ids`), so a naming knob for
   them cannot live at emit time the way the other three do without
   lying about when the fact it renders was actually decided.

**Every future site-profile knob must declare which class it belongs
to before it lands**, because the classes get materially different
treatment: only class 1 is covered by the invariance property
(`emit-hl7-test/site-profile-never-reaches-the-engine` and
`invariance-messages-agree-after-masking-dialect-surfaces`, both
proven above) — a class 1 knob that somehow perturbed
`ground-truth-log` would be a bug the property test exists to catch.
Class 2 knobs are **not** covered by that property, and must not be
expected to be: they run before `Execute`, so their whole purpose is
to affect ground truth, and holding them to a truth-invariance law
would be holding them to the wrong law. A knob proposal that can't
answer "which class" cleanly — bound at emit time yet touching a
`decide`-time fact, say — is a sign the knob is either mis-scoped or
belongs partly to each, which this layer has not yet needed to
support and should not silently assume.

## The invariance claim — proven

**Two site profiles over one seed produce the same ground truth in two
accents.** For a fixed `(config, seed)` pair, the ground-truth log is
byte-identical across any two site profiles — a site profile can only
change `hl7v2-stream`'s rendering (code strings, Z-segment content),
never `ground-truth-log`'s content or `check.clj`'s verdict on it. This
is the same "truth is invariant under dialect" argument
[`event-sourcing.md`](../components/sim/docs/event-sourcing.md) makes for emitter
coherence generally, specialized to site profiles as the
dialect-selection mechanism: two hospitals' interfaces can disagree
about what a disposition code looks like on the wire while agreeing,
provably, about what actually happened to the patient.

Checked two ways, not one: the **strong half** (ground truth)
structurally — `:site-profile` is not a member of
`ehrt.sim.engine/config-keys`, so it is structurally incapable
of reaching `Execute` at all, not merely untested against it
(`emit-hl7-test/site-profile-never-reaches-the-engine`). The **weak
half** (messages) as a property test over 100 random seeds/patient
counts (`emit-hl7-test/invariance-messages-agree-after-masking-
dialect-surfaces`), comparing a default and a deliberately gaudy second
profile after **masking** exactly the declared dialect surfaces
(MSH-3/4/5/6/11/12, PV1-2/PV1-36, and Z-segment lines stripped entirely).
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
templates), the `ehrt.sim-emit-hl7.site-profile/SiteProfile` schema
bundling them into one named, swappable `:site-profile` config value
(threaded through `ehrt.sim.run/run-command` via `--config`, no
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
stay future too. **A TEST-surname knob** (post-M6, ADR-0014's own Task
4) — an optional site-profile field forcing every persona's own
rendered surname to a fixed, obviously-synthetic marker (e.g. "TEST" or
"ZZTEST", the convention some real EHRs already reserve for exactly
this purpose) — is designed, not built: it would be a fifth dialect
knob (bound at `pid-segment`'s own render call, truth-invariant the
same way MSH/code-table/Z-segment/processing-id already are, since a
patient's *actual* sampled name never changes, only which string PID-5
renders) but isn't trivial to land in this same session — it needs its
own masking-function extension and its own invariance coverage, the
same discipline every other dialect knob here already carries, and
this session's own scope is the MSH-11 knob. Recorded here so a future
session has a named, reasoned starting point rather than reinventing
the idea from scratch.

## Where this lands

Landed between Milestone M4 and Milestone M5 in
`.agents/plans/roadmap.md`, per that document's own session
ratification — after M3, as this document originally reasoned: Z-segment
templates would have been thin content without order/result data to
bind them to (M3's `order-profiles` catalytic, `docs/sim-theory.edn`),
so landing before M3 would have exercised little beyond code-table
overrides.
