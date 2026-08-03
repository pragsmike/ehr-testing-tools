# 2026-08-02 — GMF parity plan: from Wave D to full Synthea module parity

Status: **APPROVED (2026-08-03, `notes/ADRs.md` ADR-0031).** Successor
to `.agents/plans/2026-08-02-gmf-coverage-plan.md`, whose Waves A–D are
complete (`297e337`…`7257775`) and whose H8 retrospective enumerates the
standing items this plan schedules.

**Dated note (2026-08-03, ADR-0031).** The gate paragraph below is
SATISFIED — J1 returned IDENTICAL SHA-256 digests on both spans
(`56c7cef`), no escalation. §6's own four open questions are RULED
(ADR-0031 AR-1 through AR-4); the approval act named in this header's
original text ("moves the census row to roadmap `Now`") is AMENDED
by ADR-0031 AR-7: AR-6 inserts two defect-fix sessions ahead of the
census, so the approval act is ADR-0031 landing + this status flip,
and the census row enters roadmap `Next` with its sequence position,
not `Now` — see `.agents/plans/roadmap.md`.

Prerequisite, now executed: the post-Wave-D cleanup session
(`.agents/session-records/2026-08-02-post-wave-d-cleanup.md`, J1–J5 —
byte-digest oracle verification, closure engine round-trips,
dual-clone guardrails). That session's J1 verdict was this plan's own
gate; it returned clean (above).

## 1. What parity means

**Parity = every upstream top-level GMF module in Synthea's catalog
loads clean and walks deterministically in this interpreter.**
Vendoring decisions are then made on curation grounds (ADR-0013 point
4) rather than capability grounds. Two consequences worth stating:

- Parity is a **pinned-commit claim**, not a permanent state — upstream
  adds modules. Every parity assertion names the pin it was measured at.
- **Walkable ≠ vendored.** A module may reach parity and still never be
  vendored; that is a curation judgment, made separately and recorded.

Catalog size at the time of writing: approximately 85 top-level module
JSONs (unpinned count from a GitHub contents listing, 2026-08-02 —
treat as indicative; the census tool in §3 produces the authoritative
number at the pin).

## 2. Ruled: the risk-attribute register (stroke's blocker, and the
Tier 3 precedent)

Ruled 2026-08-02 (design channel). Stroke's `stroke_risk` — and the
general class of attributes upstream modules read but delegate to
Synthea's hardcoded Java engine modules — is resolved by **curated
calibration content, not a ported calculation**.

Rationale: porting Framingham drags an input cascade (smoking, blood
pressure, cholesterol, diabetes — themselves engine- or
module-produced), which ends either in reimplementing Synthea's engine
inside a hospital simulator, or in a genuine formula computing over
invented inputs, which is less honest than an openly approximate table
because the machinery looks authoritative. The register keeps the
project's own doctrine: correctness is fitness-for-purpose, and
clinical realism comes from content provenance and calibration.

**Design.**

- A `risk-attributes.edn` register under `sim-trajectory` resources,
  under the vital-sign table's exact discipline: provenance header,
  sha256 in NOTICE, facts-register entry, growth-by-evidence.
- Each entry maps an engine-delegated attribute name to a **stratified
  rate**: coarse strata only — age band × sex, plus at most one
  comorbidity flag where the cited source makes it unavoidable.
- Interpreter resolution: an attribute-weighted distribution (D3 H3's
  mechanism, already built) reads the register when no module-written
  value exists; one documented formula converts declared annual
  incidence to per-timestep probability. The formula lives in the
  interpreter's order-contract docstring.
- **Default calibration target**: approximate incidence in a general
  population, as reported by a single authoritative surveillance
  source per attribute.

**Three properties keep the research cost bounded** (an explicit
author constraint):

1. **One provenance sweep per attribute, not per cell.** Cite one
   authoritative surveillance source (AHA/CDC-class) and adopt its
   stratification as published. If the source's bands do not match
   ours, adopt the source's bands rather than re-deriving.
2. **Coarse by design**, stated in the register's own header, so no
   later reader mistakes it for a risk model.
3. **A declared band, not a point.** Each entry carries an expected
   incidence range; a property test asserts realized incidence over N
   seeded patients falls inside it. This test IS the fitness-for-purpose
   criterion made executable — the knob must be *known*, not *right*.

**Named calibration item, not solved here:** population incidence ≠
presenting-population incidence at a regional hospital. The default is
documented as population-derived, with the presentation factor left as
a turnable knob and a named register item.

No figures appear in this plan by design — every rate is verified from
source at authoring time and lands in the facts register
(unearned-specificity discipline).

## 3. Do this first: the census tool

Every scope declaration in Waves A–D was produced by hand-reading
modules, and top-level survey rows were overturned twice (UTI's
"×3 closure" was 12 files; MI's was 27). D3 wrote a throwaway script
that mechanically loaded UTI's whole closure and reported gaps. That
script should become committed equipment.

**`gmf survey`** — **ruled (2026-08-03, ADR-0031 AR-1): a
`sim-trajectory` DEV ENTRY POINT, not a CLI verb** — promotable to a
CLI verb later as a curation decision, once the census verdict
vocabulary stabilizes, the same walkable-vs-vendored logic this plan
applies to modules. It walks the entire upstream catalog at the pin
and emits a computed census: per module and per closure, the
unrecognized state types, transition kinds, condition types,
unresolved attributes, closure file count, and load verdict. Output is
data (EDN), committed as a dated census artifact.

**Dated addition (2026-08-03, ADR-0031 AR-4): parity means WALKABLE,
and walkable means WALK-VERIFIED, not merely load-verified.** The
census performs a seeded interpreter-layer smoke walk per module (N
small) with digest recorded, not only a load verdict — the three
overturned survey rows this project's census work has found to date
(UTI's "×3 closure" was 12 files; MI's was 27; the `wellness: true`
"cheapest fix" claim, ADR-0031 AR-5) were all semantic gaps loading
alone would not catch. The verdict vocabulary keeps an
`:out-of-scope-by-ruling` category (populated even after AR-2 emptied
its largest bucket — the five wellness-cycle modules move to Wave G's
ledger rather than out of scope entirely). Boundary: census
walk-verification is INTERPRETER-LAYER — a capability claim about the
interpreter, surveying modules the engine will never see; engine round
trips remain per-vendored-root tests, ADR-0030 J3's established shape,
not something the census itself performs.

Value: converts the frontier from narrative into data; ranks
mechanisms by modules-unlocked-per-unit-work; and gives parity a
countable definition — **the census shows zero load failures and every
walked module's own smoke-walk digest recorded**. One session. Every
wave below takes its scope from the census rather than from a read
survey.

## 4. Projected waves

Sequencing is by leverage, not by module count. Session estimates are
estimates.

**Dated note (2026-08-03, ADR-0031 AR-6): two defect-fix sessions
precede the census**, inserted below as rows above Census.

**Dated note (2026-08-03, ADR-0035 AR-8): the census ranking read is
IN, resequencing the provisional E/F/G/H/I order below.** Ratified
order: **F0** (GAUSSIAN/EXPONENTIAL/TRIANGULAR distributions, ADR-0035
— DONE, see roadmap) → **F** (Counter/ImagingStudy/SupplyList, 24
modules, plus the `:race`/`:not` condition-type rider found by the
census's own `:walk-failed` mechanisms table, 4 more — **DONE, ADR-0036,
see roadmap**) → **G** (wellness — ledger is 19 tagged modules plus
now FOUR max-steps loop walk-failures, `med-rec`/`veteran-substance-
abuse-treatment`/`mend-program`/`metabolic-syndrome-care` (the last two
newly surfaced by Wave F's own census re-run), expected to resolve as
substitution artifacts once G lands) → **H** → **I**
(singleton tail: `AllergyOnset`, `VitalSign`, `Vaccine`, the
lookup-column `time` gap). **Wave E is RE-SCOPED**: `stroke.json`
already censuses `:ok-walked` (the `distributed_transition`
attribute-weighted mechanism, H3, already unblocks its own onset gate,
falling back to the JSON-declared `:default` in the absence of a real
`stroke_risk` source) — E is therefore calibration CONTENT (the
risk-attribute register, §2), not an unlock wave, and is scheduled on
demand rather than in the leverage queue below.

| Wave | Content | Sessions | Unlocks |
|---|---|---|---|
| **Defect fix 1** (ADR-0031 AR-6) | Procedure-duration fix — `resolve-time-advance`'s flat-map/nested-key mismatch, mechanical, semantics pinned from Synthea source before the fix commit. Full oracle-bracketed re-baseline (virtual time shifts for every root). | 1 | unblocks every vendored root's own Procedure timing; re-records the digest baseline once |
| **Defect fix 2** (ADR-0031 AR-6) | Engine closure-context fix — `engine.clj`'s `:registered` decide method threads a closure's own submodule registry and `initial-attributes` through to `run-module` (ADR-0030 J3's two gaps). Flips the three pinned round-trip tests. Oracle-bracketed: the five non-closure roots stay byte-identical; closure roots gain NEW engine-layer baselines. | 1 | the closure engine round trip, for real, for the first time |
| **Census** | `gmf survey` dev entry point + first census artifact (AR-1, AR-4: walk-verified, not load-verified) | 1 | scope for everything below |
| **E** *(RE-SCOPED 2026-08-03, ADR-0035 AR-8 — see dated note above; kept, annotated, not deleted)* | Risk-attribute register (§2) — calibration CONTENT now, not an unlock wave; `stroke` already censuses `:ok-walked` via H3's own attribute-weighted mechanism | on demand | none — dropped out of the leverage queue |
| **F0** (ADR-0035, ratified order) | GAUSSIAN/EXPONENTIAL/TRIANGULAR distributions — loader normalization + clean rejection, single-draw interpreter sampling (Delay/Procedure timing, Symptom severity), SetAttribute's own silent-nil fix. Oracle-bracketed: pure identity (no vendored root uses the new content). | 1 | 11 census-blocked modules resolve or surface their next blocker (2 `:ok-walked`, 3 `:walk-failed`, 6 stay `:load-failed` on an unrelated, earlier gap); structurally closes the loader-exception rejection class |
| **F** *(ratified next, was provisional)* | `Counter`, `SupplyList` (interpreter-only, small), then `ImagingStudy` (reverses ADR-0029 R5; full four-layer chain + emission ruling: radiology ORM/ORU or disclosed silence); the `:race`/`:not` condition-type rider (4 more modules, the census's own `:walk-failed` mechanisms table) | 2 | `myocardial_infarction` + a census-named cluster |
| **G** *(ratified next, was provisional)* | **Wellness cycle** — design session first. Ruled IN SCOPE (2026-08-03, ADR-0031 AR-2): `wellness: true` becomes a genuine WAIT state (parks the walk until the next scheduled cycle visit, then attaches downstream states), cadence ported from Synthea's own age-banded schedule (`EncounterModule.recommendedTimeBetweenWellnessVisits`) as provenance-cited content under the vital-sign table's discipline, cycle anchor is a seeded per-patient phase offset until Wave H's pre-roll supersedes it. Remaining design questions (schedule-state home, multi-module attachment/churn composition, chronic-meds cap) are the G design session's own scope. | 1 design + 1–2 impl | the chronic cluster; highest module count behind one decision — now NAMED: `epilepsy`, `med_rec`, `mTBI`, `atrial_fibrillation`, `osteoporosis` (ADR-0031 AR-5(a), the wellness-overturn finding) |
| **H** *(provisional order)* | **Pre-roll** — design session first. Walk modules deterministically from onset to registration; fold pre-window history into initial patient state; emit only in-window events. Open questions: what folds vs. what is discarded; concurrent-comorbidity interaction (ADR-0027 D1 already rules the channel: clinical state, never scratch); composition with churn and the horizon model. **Ruled (2026-08-03, ADR-0031 AR-3): emit-nothing REAFFIRMED — no backloaded-history mode in the sim; the backload need is a named TOOLS-SIDE future (a corpus construction over sim output), revisit trigger: a real consumer for pre-window messages appears.** **Dated pointer (2026-08-03, ADR-0033's own execution note, carried here by ADR-0034 AR-6 for H's own design session to read): UTI's mandatory Encounter (Care Pathways) straddles `engine.clj`'s own fixed registration-t anchor for most seeds — 8 of 10 sampled trip `check/check-all`'s `:clinical-content-only-when-admitted` invariant (opens pre-horizon, folded only into `:pre-horizon-facts`; closes post-horizon as a real, discrete `:outpatient-visit-end` event). The UTI engine round-trip test dodges this empirically (seed 777, chosen because it doesn't trip it) rather than resolving the boundary — that dodge is a standing, disclosed workaround, not a fix; it retires the moment H's own pre-horizon/post-horizon fold boundary design lands and actually resolves straddling encounters, at which point the round-trip test should be revisited too (does it still need a hand-picked seed, or does any seed now work).** | 1 design + 1–2 impl | the chronic/lifetime catalog; acute-episode → population-scale simulator |
| **I** *(provisional order)* | Bulk vendoring, batched by closure family; per-module cost by then is characterization + test | N (batched) | parity in the vendored sense |

Rough total: **10–14 sessions** (the original 8–12 plus the two
defect-fix sessions AR-6 inserts), front-loaded with two design
decisions (G and H) that dominate the outcome.

## 5. Cross-cutting expectations

- **S4 trigger.** The sim split's last deferred stage (extract
  `sim-engine`) fires when a second engine consumer appears — most
  likely `sim-emit-fhir`, plausibly during F or I. Watch for it;
  do not let engine work grow inside residual sim in the meantime.
- **Standing discipline, every wave**: characterization gates scope
  from *fetched closure* evidence (never a top-level survey row);
  co-landing (mechanism + invariants in one change); rng draws join
  the documented order contract; specify-vs-delegate governs every
  content gap (never override what the artifact specifies; freely
  supply what it delegates, disclosed); installed ≠ used (build what
  a vendored module exercises, name the rest); byte-digest regression
  oracle (per the cleanup session's J2 doctrine — digests, not counts).
- **Standing named items** carried from H8's retrospective and not
  scheduled above: `ImagingStudy`/CHF (now scheduled in F), the
  compound-guard forms not built (H4), closure engine round-trips
  (cleanup J3), and the `/mnt/c` clone question (cleanup J4).

## 6. Open questions for the author

**All four RULED 2026-08-03, `notes/ADRs.md` ADR-0031 — see AR-1
through AR-4 there for the full text; summarized below with the
pointer.**

1. Census tool home: a `gmf survey` CLI verb under the existing CLI
   base, or a `sim-trajectory` dev entry point not exposed as a
   product surface? **Ruled (ADR-0031 AR-1): dev entry point, not a
   CLI verb — promotable later as a curation decision.**
2. Wave G: is a synthesized wellness cycle in scope for a *hospital*
   traffic simulator at all, or are chronic-progression modules
   correctly out of scope, capping parity below 100% by design?
   **Ruled (ADR-0031 AR-2): IN SCOPE for Wave G — `wellness: true`
   becomes a genuine WAIT state on Synthea's own age-banded schedule,
   ported as provenance-cited content; interim per-patient phase-offset
   anchor until Wave H supersedes it.**
3. Wave H: does pre-roll emit nothing before the window (proposed), or
   should pre-window events be emitted as backloaded history for
   systems that ingest it? **Ruled (ADR-0031 AR-3): emit-nothing
   REAFFIRMED; the backload need is a named tools-side future, not a
   sim mode.**
4. Does parity mean *walkable* (this plan's definition) or *vendored*?
   The plan assumes walkable, with curation separate. **Ruled
   (ADR-0031 AR-4): walkable, and walkable means WALK-VERIFIED (a
   seeded interpreter-layer smoke walk with digest, not merely a load
   verdict) — the three overturned survey rows to date were all
   semantic gaps loading alone would not catch.**
