# Site profiles: the "simulate MY hospital" config layer

**Design capture only — no code lands with this document.** This is
the answer to the first question any domain expert asks on meeting
this project: "how do I make it simulate *my* hospital?"
`docs/clinical-realities.md`'s site-defined-codes entry names the
reality this layer exists to configure; this document specifies the
config layer itself.

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

## What this layer designs, for a future milestone

Three components, each extending an existing idea rather than
inventing a new mechanism:

1. **Code-table overrides.** A site profile supplies its own value set
   for a user-defined HL7 table — most concretely, patient class
   (table 0004, `docs/patient-state-model.md`'s `:class` field) and
   discharge disposition (table 0112, `docs/clinical-realities.md`'s
   post-mortem entry already names disposition codes 20/40–42 for
   died-here/died-at-home/place-unknown). Overriding a table is a
   **rendering-time substitution**: the underlying state value
   (`:class :inpatient`, a disposition concept) doesn't change: only
   which code string an emitter writes for it does, per the site
   profile in effect. This is `:surge-format`'s pattern applied to
   code tables instead of bed-naming strings.
2. **Naming idioms**, generalizing `:surge-format` beyond bed slots to
   any place this project's engine currently picks one baked-in
   naming convention — ward ids, provider id formats, whatever a
   future resource model needs named. Each is a config-supplied format
   or lookup a site profile overrides, never a hard-coded string in
   engine or emitter code.
3. **Z-segment templates**, binding a Z-segment's fields to state
   paths in the patient accumulator — most naturally, paths into the
   reserved `:attributes` map, since a Z-segment by definition carries
   facts standard segments don't. A template is a declarative mapping
   (field position → state path, plus a table/format for the value)
   that the emitter reads at render time. This is where a site's fully
   custom fields (decedent-affairs tracking, ME-referral status,
   whatever a real site's Z-segments carry) become renderable without
   the engine needing to know what any particular site's Z-segment
   means — only that a template says where its values come from.

**Applied at the emitter, because idiosyncrasy is a rendering
dialect.** All three components bind at emit time
(`docs/sim-theory.edn`'s `EmitHL7`/`EmitState` stages), never inside
`decide`/`evolve` or the ground-truth log. This follows directly from
ADR-0002's separation of ground truth from wire format: a site profile
changes how a fact is *said*, never what fact is true. The engine and
the invariant catalog (`check.clj`) never need to know which site
profile is active; only the emitter's render call sites do.

## The invariance claim

**Two site profiles over one seed produce the same ground truth in two
accents.** Stated as a **future property test**, once site profiles
are built: for a fixed `(config, seed)` pair, the ground-truth log is
byte-identical across any two site profiles — a site profile can only
change `hl7v2-stream`'s rendering (code strings, Z-segment content,
naming idioms), never `ground-truth-log`'s content or `check.clj`'s
verdict on it. This is the same "truth is invariant under dialect"
argument [`docs/event-sourcing.md`](event-sourcing.md) makes for
emitter coherence generally, specialized to site profiles as the
dialect-selection mechanism: two hospitals' interfaces can disagree
about what a disposition code looks like on the wire while agreeing,
provably, about what actually happened to the patient.

## Honest split: today vs. future

**Exists today:** `:surge-format` (a real, shipped config knob) and the
reserved `:attributes` map (a real, shipped extension point, currently
written only by engine-internal state like `:donor`, not yet by any
site-profile mechanism). **Designed, not built:** code-table overrides,
generalized naming idioms beyond bed slots, Z-segment templates, the
site-profile config schema that would bundle all of these into one
named, swappable unit, and the invariance property test above. No
`sim-config` key for "the active site profile" exists yet; this
document specifies what one would configure, not its wire format or
schema.

## Where this lands

`.agents/plans/roadmap.md` records a proposed milestone for this layer
(marked for author review — placement, not existence, is the open
question), reasoned as landing after M3: Z-segment templates are thin
content without order/result data to bind them to (M3's
`order-profiles` catalytic, `docs/sim-theory.edn`), so a site-profile
milestone that lands before M3 would have little beyond code-table
overrides to actually exercise.
