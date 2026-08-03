# 2026-08-02 — GMF parity plan: from Wave D to full Synthea module parity

Status: PROPOSED (design channel, 2026-08-02). Successor to
`.agents/plans/2026-08-02-gmf-coverage-plan.md`, whose Waves A–D are
complete (`297e337`…`7257775`) and whose H8 retrospective enumerates the
standing items this plan schedules. Becomes approved when the author
rules the open questions in §6 and moves the census row to roadmap `Now`.

Prerequisite, unscheduled here: the post-Wave-D cleanup session
(`.agents/prompts/2026-08-02-…-postwave-cleanup.md`, J1–J5 —
byte-digest oracle verification, closure engine round-trips,
dual-clone guardrails). That session's J1 verdict is a gate on this
plan: if the oracle verification escalates, resolve it before any
wave below starts.

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

**`gmf survey`** — a CLI verb (or `sim-trajectory` entry point) that
walks the entire upstream catalog at the pin and emits a computed
census: per module and per closure, the unrecognized state types,
transition kinds, condition types, unresolved attributes, closure
file count, and load verdict. Output is data (EDN), committed as a
dated census artifact.

Value: converts the frontier from narrative into data; ranks
mechanisms by modules-unlocked-per-unit-work; and gives parity a
countable definition — **the census shows zero load failures**. One
session. Every wave below takes its scope from the census rather than
from a read survey.

## 4. Projected waves

Sequencing is by leverage, not by module count. Session estimates are
estimates.

| Wave | Content | Sessions | Unlocks |
|---|---|---|---|
| **Census** | `gmf survey` tool + first census artifact | 1 | scope for everything below |
| **E** | Risk-attribute register (§2) + stroke as first consumer + incidence-band property test | 1 | `stroke`; sets the Tier 3 precedent |
| **F** | `Counter`, `SupplyList` (interpreter-only, small), then `ImagingStudy` (reverses ADR-0029 R5; full four-layer chain + emission ruling: radiology ORM/ORU or disclosed silence) | 2 | `myocardial_infarction` + a census-named cluster |
| **G** | **Wellness cycle** — design session first. Chronic modules hang progression on engine-generated wellness encounters the sim has no equivalent for. Synthesize a calibrated periodic outpatient cycle (the existing `outpatient-visit` machinery is the landing pad), or rule those modules out of scope. | 1 design + 1–2 impl | the chronic cluster; highest module count behind one decision |
| **H** | **Pre-roll** — design session first. Walk modules deterministically from onset to registration; fold pre-window history into initial patient state; emit only in-window events. Open questions: what folds vs. what is discarded; concurrent-comorbidity interaction (ADR-0027 D1 already rules the channel: clinical state, never scratch); composition with churn and the horizon model. | 1 design + 1–2 impl | the chronic/lifetime catalog; acute-episode → population-scale simulator |
| **I** | Bulk vendoring, batched by closure family; per-module cost by then is characterization + test | N (batched) | parity in the vendored sense |

Rough total: **8–12 sessions**, front-loaded with two design decisions
(G and H) that dominate the outcome.

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

1. Census tool home: a `gmf survey` CLI verb under the existing CLI
   base, or a `sim-trajectory` dev entry point not exposed as a
   product surface?
2. Wave G: is a synthesized wellness cycle in scope for a *hospital*
   traffic simulator at all, or are chronic-progression modules
   correctly out of scope, capping parity below 100% by design?
3. Wave H: does pre-roll emit nothing before the window (proposed), or
   should pre-window events be emitted as backloaded history for
   systems that ingest it?
4. Does parity mean *walkable* (this plan's definition) or *vendored*?
   The plan assumes walkable, with curation separate.
