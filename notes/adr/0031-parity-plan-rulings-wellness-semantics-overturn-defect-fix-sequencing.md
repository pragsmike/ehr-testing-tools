<!-- Attic file: notes/adr/0031-parity-plan-rulings-wellness-semantics-overturn-defect-fix-sequencing.md -->
<!-- Moved verbatim from notes/ADRs.md by scaffolding compaction B (2026-08-05, notes/ADRs.md ADR-0046). notes/ADRs.md remains the citation index -- see there. -->

## ADR-0031 — Parity-plan rulings (Q1–Q4), wellness-semantics overturn, defect-fix sequencing

**Status:** Accepted (author-ruled 2026-08-03, design channel, AR-1
through AR-7 below; recorded verbatim, attributed, per `notes/ADRs.md`
ADR-0007's own provenance-tag convention — every ruling below is
`[A]`).

### Context

`.agents/plans/2026-08-02-gmf-parity-plan.md` (PROPOSED) named four
open questions in its own §6 and a gate on its own approval: J1's
byte-digest oracle verdict from the post-Wave-D cleanup session
(`notes/ADRs.md` ADR-0030). That gate is now SATISFIED — J1 returned
IDENTICAL SHA-256 digests on both spans (`56c7cef`). Separately, a live
probe against Synthea source at `docs/gmf-interpreter.md`'s own pin
(`7e08387c68a7f0e21d13076609a159fd473fc902`) overturned a Wave-B-era
survey claim about GMF's `wellness: true` encoding — the THIRD survey
row this project's own module census has overturned (after UTI's
"×3 closure was 12 files" and MI's "×1 was 27"), each time a semantic
gap loading alone would not have caught. ADR-0030's own J3 finding
(the closure engine round trip confirmed broken in two distinct ways,
pinned by three tests under `components/sim-emit-hl7/test/`) is the
other piece of evidence this session's rulings act on. This ADR
resolves the parity plan's own §6 questions, records the wellness
overturn that AR-2 rests on, and sequences the defect-fix work AR-6
inserts ahead of the census. No code behavior changes — this is a
capture session; the two defect-fix sessions AR-6 names are future
work, not executed here.

### Decision

Ruled 2026-08-03, design channel, recorded verbatim:

**AR-1 (parity plan §6 Q1).** The census tool is a `sim-trajectory`
DEV ENTRY POINT, not a CLI verb. Promotable to a CLI verb later as a
curation decision, once the census verdict vocabulary stabilizes —
same walkable-vs-vendored logic the plan applies to modules. §3's "CLI
verb (or `sim-trajectory` entry point)" hedge resolves to the latter.

**AR-2 (parity plan §6 Q2).** The synthesized wellness cycle is IN
SCOPE for Wave G. Design shape ruled: (a) `wellness: true` becomes a
genuine WAIT state — the interpreter parks the walk until the
patient's next cycle visit, then attaches the module's downstream
states to it — superseding the Wave B loader normalization that
rewrites it to a created-on-the-spot `:outpatient-visit`; (b) the
cycle's cadence is Synthea's own age-banded schedule
(`EncounterModule.recommendedTimeBetweenWellnessVisits`, pin
`7e08387c68a7f0e21d13076609a159fd473fc902`), ported as
provenance-cited CONTENT under the vital-sign table's exact discipline
(sha256, NOTICE, facts-register entry) — this is the register
pattern, not the Framingham anti-pattern: its inputs (age,
active-chronic-medications) exist in the sim with no input cascade;
(c) before Wave H (pre-roll) exists, the cycle anchor is a seeded
per-patient phase offset — an interim, disclosed answer superseded
when H lands; (d) Wave G's remaining design questions (schedule-state
home; multi-module attachment/churn composition — upstream, ALL
waiting modules attach to the SAME visit; chronic-meds cap in v1 or a
named register item) are the G design session's scope.

**AR-3 (parity plan §6 Q3).** Pre-roll REAFFIRMED as emit-nothing: the
history phase folds state effects and mints no operational events,
exactly as `docs/gmf-interpreter.md` §3 already ratifies. No
backloaded-history mode in the sim. The backload need (pre-window
messages for systems that ingest historical loads) is recorded as a
NAMED TOOLS-SIDE FUTURE — a corpus construction over sim output,
fault-injection's sibling — with revisit trigger: a real consumer for
pre-window messages appears.

**AR-4 (parity plan §6 Q4).** Parity means WALKABLE, and walkable
means WALK-VERIFIED: the census performs a seeded interpreter-layer
smoke walk per module (N small) with digest recorded, not merely a
load verdict — the three overturned survey rows were all semantic
gaps loading alone would not catch. The verdict vocabulary keeps an
`:out-of-scope-by-ruling` category even though AR-2 emptied its
largest bucket. Boundary: census walk-verification is
INTERPRETER-LAYER (a capability claim about the interpreter, and it
surveys modules the engine will never see); engine round-trips remain
per-vendored-root tests per ADR-0030 J3's established shape.

**AR-5 (wellness overturn — the finding AR-2 rests on).** The
prioritization-table claim that the `wellness: true` encoding gap is
"a loader normalization, not new interpreter machinery" is OVERTURNED
by fetched Synthea source at the doc's own pin
(`7e08387c68a7f0e21d13076609a159fd473fc902`): in
`src/main/java/org/mitre/synthea/engine/State.java` (the
`Encounter.process` method, wellness branch, ~lines 950–980),
`wellness: true` creates NOTHING — it BLOCKS until the engine's
hardcoded `EncounterModule` opens its next separately-scheduled
wellness encounter, then attaches to it. Consequences, all recorded:
(a) the five "would vendor immediately if fixed" modules (`epilepsy`,
`med_rec`, `mTBI`, `atrial_fibrillation`, `osteoporosis`) are
wellness-cycle modules and move into Wave G's unlock ledger — there is
no cheap loader-fix win separate from G; (b) the vendored
`ear_infections` root currently carries a live, previously-undisclosed
TIMING SUBSTITUTION: its `Next_Wellness_Encounter` (upstream
`wellness: true`, no `encounter_class` key) fires an immediate
outpatient visit where upstream resolves the infection at the next
SCHEDULED wellness visit, potentially months later on the cadence —
legal under specify-vs-delegate (the artifact delegates timing; the
sim supplied an answer) but documented as a vocabulary alias, which it
is not; fix-forward disclosure, no behavior change this session,
superseded by G-impl; (c) checked upstream at the same pin: NO
vendored root uses a class-string `"wellness"` (`sinusitis`/
`sore_throat` are `ambulatory`; `ear_infections`' other encounter is
`outpatient`) — §4's mapping-table row conflates two upstream
constructs sharing a word.

**AR-6 (sequencing).** Two DEFECT-FIX SESSIONS precede the census, in
this order: (1) Procedure-duration fix — mechanical
(`resolve-time-advance` destructures nested `:range`/`:exact` from a
flat map; semantics pinned from Synthea source per H1 discipline
before the fix commit), full oracle-bracketed re-baseline since
virtual time shifts for every root; (2) engine closure-context fix —
owns both J3 gaps (submodule-registry threading AND
`initial-attributes` plumbing, including the design ruling on who
supplies the seed at the engine layer), flips the three pinned
round-trip tests, oracle-bracketed (the five non-closure roots must
stay byte-identical; closure roots gain NEW engine-layer baselines).
Duration-first is deliberate: it re-records the existing digest set
once, and the engine fix then adds closure baselines on final timing
semantics rather than recording them twice. Then census, then E/F/G
with ordering left to the census ranking. G-impl follows the engine
fix. The two fixes are NOT combined: one is mechanical, one has a
design surface, and entangling them is churn.

**AR-7 (approval act).** The parity plan's own header names "census
row to roadmap Now" as the approval trigger; AR-6 inserts two sessions
before the census, so the approval act is AMENDED to: ADR-0031 landing
+ the plan's status flip (this session). The census row enters roadmap
`Next` with its sequence position, not `Now`.

### Fence

This ADR captures rulings only. No code behavior changes. The two
defect-fix sessions AR-6 names (procedure-duration, engine
closure-context) are NOT executed here, however small either looks —
they are future sessions of their own, each oracle-bracketed. The
census tool itself is not built here. `components/sim-trajectory/src/
ehrt/sim_trajectory/gmf.clj`'s wellness normalization clause is not
changed by this ADR — AR-5(b)'s timing-substitution disclosure lands
as a docstring/comment note only (this session's own Step 4), the code
form is untouched pending Wave G's wait-semantics implementation.

---

### Roadmap history (moved verbatim from roadmap.md by ADR-0144, 2026-08-17)

The `.agents/plans/roadmap.md` row this ADR owns, as it stood at `deb9a33` before the ADR-0144 row contract capped rows at six lines. The live row now states what remains and cites this ADR for the rest; this is the rest, verbatim.

- `ImagingStudy` (R5, CHF trigger) and the stroke-risk data source (R7)
  — GMF coverage Wave D closed 2026-08-02 (D0-D3, see Done below)
  without owning either; H3's own attribute-weighted `distributed_
  transition` mechanism landed D3 but is only half of stroke's own
  revisit trigger (`stroke.json` stays deferred). **Dated
  cross-reference (2026-08-03, ADR-0031):** the stroke-risk DATA-SOURCE
  question is RULED — `.agents/plans/2026-08-02-gmf-parity-plan.md` §2
  (the risk-attribute register, curated calibration content rather than
  a ported calculation). This row's remaining substance is Wave E
  scheduling (stroke as the register's first consumer), not an open
  design question.
