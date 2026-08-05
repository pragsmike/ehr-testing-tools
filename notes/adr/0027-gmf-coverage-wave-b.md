<!-- Attic file: notes/adr/0027-gmf-coverage-wave-b.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0027 — GMF coverage Wave B: `CallSubmodule` — three-compartment person record, root-scoped scratch, closure loading

**Status:** Accepted (author-ruled 2026-08-02, design channel, D1–D8 of
the Wave B session prompt; session executed same day).

### Context

Wave B (`.agents/plans/2026-08-02-gmf-coverage-plan.md`, roadmap's own
Wave B row) is the GMF coverage arc's structural lift after Wave A
(ADR-0026, condition vocabulary only): `CallSubmodule` — loader closure
resolution, interpreter call/return, root-scoped workflow attributes,
cross-boundary provenance — plus `type_of_care_transition`, a fifth
transition kind. Design was ruled 2026-08-02 in the design channel,
recorded verbatim below (D1–D7); this ADR is that record, not a
narrative retelling of it. D8, above (appended to ADR-0026), is this
same ruling session's retro-ratification rider for a Wave A deviation.

### Decision

**D1 — Three-compartment person record; root-scoped scratch.** The
interpreter ctx is the person record with three compartments of
distinct character: persona (fixed characteristics, immutable input),
clinical state (derived only by folding the trajectory — the event log
is ground truth; guards that need clinical facts read the log, as
`:observation` already does), and workflow attributes (module
control-flow scratch only). `CallSubmodule` shares the THIRD
compartment across a call tree by scoping it to the walk's ROOT
module: caller and callee resolve the same bare attribute name in the
root's namespace. Non-calling walks are byte-identical by construction
(root = self). Nothing is shared between separate top-level walks:
cross-storyline interaction, when it comes, goes through clinical
state, never through scratch. Implementation latitude on representation
(root-qualified keys vs. nested-by-root map) — the semantic contract
and the regression oracle are what's fixed.

**D2 — Provenance: every event emitted inside a submodule cites the
full call path, root-first.** Invariant, co-landed: every citation
path's head equals the walk's root module; single-module walks cite
the one-element path (representation may stay backward-compatible for
that case — same oracle applies).

**D3 — Loader closure: submodules resolve on the search path
`sim/modules/<call-path>.json`; the loader resolves the transitive
closure at load time; the all-or-nothing gate extends to the closure**
(a module is loadable iff every transitively-called submodule loads
clean and in-vocabulary). The static call graph must be acyclic — a
cyclic real-world closure is an ESCALATION with evidence, not a
relaxation. A defensive runtime call-depth invariant co-lands (limit
generous, violation is a bug signal, not a semantic).

**D4 — Determinism threading: one clock, one rng stream; consumption
order is descend-run-return**, documented in the interpreter ns
docstring's order contract; the whole-walk reproducibility property
extends over closures (property test: walk with closure, twice,
identical).

**D5 — `type_of_care_transition` semantics are characterized from
Synthea source at the pinned commit BEFORE implementation** — the
dispatch rule (how a care-setting path is selected, what it consumes
from the person record or rng) is recorded in this ADR's own
fix-forward note with the source citation, then implemented to match.
If selection consumes rng, its draw joins the documented order
contract.

> **D5 characterization note (filled Step 1, 2026-08-02):** real
> Synthea's own dispatch (`Transition.java`'s `TypeOfCareTransition`,
> same pinned commit) keys on the simulated calendar year (before/from
> `telemedicine_config.json`'s own `start_year: 2020`) AND the person's
> current insurance-payer name (`high_emergency_use_insurance_names`) —
> this project's persona carries no payer concept, the identical gap
> shape `Active Allergy`'s own documented simplification already
> established. Simplification: always the `typical_emergency_
> distribution` branch (never `high_emergency_distribution`), since no
> data exists to tell which synthetic patients would qualify; the
> year-gated half is NOT simplified away (`ctx`'s own `:t`, the same
> mechanism `:date` condition already uses, answers it honestly):
> `< 2020` -> `{:ambulatory 0.75 :emergency 0.25}`; `>= 2020` ->
> `{:ambulatory 0.56 :emergency 0.2 :telemedicine 0.24}` (both cited
> verbatim from `telemedicine_config.json`'s own
> `typical_emergency_distribution` rows). One `.nextDouble` draw, the
> same fixed-consumption weighted-pick `distributed_transition` already
> uses — implemented as a 5th TRANSITION kind (a `Simple` state's own
> field, not a new state type), joining the interpreter's own
> descend-run-return order contract as a zero-rng weight lookup
> followed by one weighted-pick draw. Full account, with source
> citations: `components/sim-trajectory/docs/gmf-interpreter.md` §9's
> own "D5 — `type_of_care_transition` dispatch-rule characterization."

**D6 — Curation per closure: ADR-0013 point 4's "modest deferred-type
surface" bar applies to the closure as a unit.** Each closure member
gets its own survey row. A dirty closure member (deferred types, or a
new gap) drops its whole root module from this wave's vendoring —
recorded as a finding with the evidence, payoff shrinks honestly.

**D7 — Hidden-import check (D1's falsifier):** for each candidate
closure, compute the set of attributes READ anywhere in the closure but
WRITTEN nowhere in it (excluding persona-backed builtins). Expected:
empty. Non-empty is an ESCALATION naming the attribute and its upstream
writer — do not restore a global channel to make it pass, and do not
seed it silently.

**D8 — Retro-ratification rider:** see ADR-0026's own deviation record,
above — this ADR's own D8 is that rider, not a Wave-B-scoped decision
of its own.

### Verification baselines

Fixed-seed walks of all three currently vendored modules (`sinusitis`,
`appendicitis`, `sore_throat` — 6 seeds × 2 sexes each, hashed
trajectory + `:status`) proven identical before and after every
commit — the D1 root-scoping restructure must be invisible to
non-calling walks by construction, and this oracle is what proves it.
`poly check` and `poly test :all skip:integration` clean at every
checkpoint, confirmed one final time at session close (Step 4) across
the FULL workspace, not just `sim-trajectory`.

### Execution record

Session executed same day as ruled, all eight checkpoints (Step 0
through Step 4) landed. Commits, in order: `a92254b` (Step 0, this ADR
+ ADR-0026's own D8 rider), `f596a37` (Step 1, closure survey + D5/D7
findings), `9a2f0cd` (Step 2a, D1 root-scoping refactor), `599fa47`
(Step 2b, D3 loader closure resolution — a real cycle-detection bug
found and fixed by its own red test), `cc9e0d6` (Step 2c, CallSubmodule
call/return, D1-D4), `13b924e` (Step 2e, encounter-class loader
normalizations — see Deviation record), `3adf974` (Step 2d,
`type_of_care_transition`, D5 — a real Java `Random` sequential-seed
clustering bug found and fixed in the test suite itself), `01eb56b`
(Step 3, `ear_infections.json` closure vendored — the real end-to-end
proof this Wave's own machinery works, not just synthetic unit-test
fixtures).

### Deviation record

Three disclosed deviations from the session prompt's own literal Step
2 checkpoint list (a-d), each the same "characterization surfaces a
real, in-spirit-authorized finding" shape ADR-0026's own deviation
record already established a precedent for:

1. **Step 2e (encounter-class loader normalizations) is an ADDITIONAL
   commit, not named in D1-D8.** Step 1's own characterization of
   `ear_infections.json`'s real closure found two more mandatory-path
   gaps beyond `CallSubmodule` itself: an unrecognized `encounter_class:
   "outpatient"` value on the module's own primary encounter, and the
   already-documented `wellness: true` boolean idiom (section 8's own
   M7 finding) confirmed mandatory here for the first time. Both are
   cheap, mechanical, narrowly-scoped v1.1 extensions in the same
   spirit as every prior wave's own emergent findings (`:symptom`-as-
   condition, Wave A) — landed rather than dropped, since dropping
   `ear_infections.json` over two loader normalizations this project's
   own established discipline already has a template for would have
   been the wrong trade.
2. **Two more mandatory-path findings folded into Step 2c rather than
   their own commits:** `MedicationOrder`'s own `assign_to_attribute` /
   `MedicationEnd`'s own `referenced_by_attribute`, and the `Attribute`
   condition's own `is nil`/`is not nil` operators. Both are tightly
   coupled to CallSubmodule's own cross-module reference shape (the
   whole reason they're load-bearing is that ear_infections.json's own
   closure crosses a call boundary) — splitting them into separate
   commits would have been an artificial cut through one coherent
   change, the same reasoning ADR-0026's own `:at-least`/`:or` combined
   commit already used.
3. **`lookup_table_transition` — a genuinely new, unplanned SIXTH
   transition kind, found on `urinary_tract_infections.json`'s own
   entry path, is named as a finding and NOT built.** Not an escalation
   this session blocked on: the outcome (UTI deferred, D6) does not
   change either way it's eventually resolved, and building it would
   need real design (an external lookup-table CSV mechanism this
   project has no analog for) outside D1-D8's own scope. Section 9 and
   §2 both carry the full account.

None of the three changes this ADR's own D1-D8 decisions — each is an
IMPLEMENTATION-level finding Step 1's own characterization surfaced,
resolved per that step's own "extend v1 with a documented reason, or
defer" standing option, not a design reopening.

### Fence

This ADR covers Wave B's design only. Its own characterization
(Step 1, D5/D6/D7's actual findings) and build record (Step 2–4) are
recorded in `components/sim-trajectory/docs/gmf-interpreter.md` and
this session's own session record, not restated here. Wave C
(`Death`) and Wave D (state types needing IR + emitter homes) are not
started — see `.agents/plans/2026-08-02-gmf-coverage-plan.md` and
`.agents/plans/roadmap.md`'s own Deferred rows.

---

