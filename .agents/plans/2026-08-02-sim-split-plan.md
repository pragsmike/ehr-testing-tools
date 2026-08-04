# 2026-08-02 — sim split plan: staged extraction of components/sim

Status: PROPOSED (design channel, 2026-08-02). Precedent:
`.agents/plans/2026-08-01-migration-report.md` (a dated, sequenced plan
referenced from roadmap rows); method precedent: the three-stage tools
split (`notes/prompts/2026-07-31-ehr-testing-split-*`, characterize →
extract → verify → records, escalate once with edges named).

**Dated status note (2026-08-03, fix-forward, stale-header hygiene).**
S1–S3 are EXECUTED, not merely proposed: S1 (`sim-model`) and S2
(`sim-trajectory`) landed same session (`8d5c86c` for S1; S2 same
session, same commit lineage — `.agents/plans/roadmap.md`'s own "Done
(2026-08-02, sim split S1+S2)" entry has the full verification account,
poly check clean, deftest+defspec parity 403=403=403). S3
(`sim-emit-hl7`) landed front-run as GMF coverage Wave D's own stage D0
(`notes/ADRs.md` ADR-0029 R1; commits `7935b71`/`7a3dd58`, `ccce1fc`,
`e38e232` — roadmap's own "Done (2026-08-02, sim split S3 / GMF
coverage Wave D stage D0)" entry). **S4 (`sim-engine`) stays
DEFERRED**, its trigger unfired as of the 2026-08-03 rulings-capture
session (`notes/ADRs.md` ADR-0031 AR-6's own defect-fix sequencing
does not fire it either — neither defect fix adds a second `engine`
consumer) — see `.agents/plans/roadmap.md`'s own Deferred section for
the live trigger condition. This plan's own sequencing/rulings text
below is left as originally written, not rewritten, per this project's
own annotate-not-rewrite convention.

**Dated status note (2026-08-04, sim split B M1 session, AR-M1-5 /
plan AR-4, framing (b), author override plainly stated).** S4
(`sim-engine`) is SUPERSEDED-BY-CITATION here, not fired by its own
named trigger: `.agents/plans/2026-08-04-sim-split-b-plan.md` (RULED,
AR-1..AR-6) proceeds with the full decomposition ahead of a second
`engine` consumer actually appearing, as M2 (`sim-engine`) in a
four-stage sequence (M1 provenance → M2 sim-engine → M3 sim-emit-fhir
→ M4 sim-check). The trigger's own reasoning — don't design a boundary
with one consumer — is honored in substance, not overridden blindly:
M3 (`sim-emit-fhir`) is committed scope in the same sequence, so M2's
interfaces are designed against two known consumer surfaces even
though the second lands a session later. Not claimed: that the
trigger fired at M2's execution — the second consumer is promised,
not present, when M2 runs. `notes/ADRs.md` ADR-0043 (M1's own split
ADR) records this ruling verbatim; this plan's own text above stays as
written, annotated not rewritten, per this project's own
annotate-not-rewrite convention.

## Context

`components/sim` is the pre-merge simulator landed as one fat component
(ADR-0001 R5). Its `interface.clj` docstring explicitly defers narrowing
to "a future, author-ruled extraction session." That session's design
pass is this plan. The motivating goal is not tidiness: the GMF coverage
gap (CallSubmodule, condition-vocabulary gaps — `docs/gmf-interpreter.md`
§survey) lives entirely in the loader/interpreter/compile pipeline, and
extracting that pipeline first gives the coverage-expansion work a
bounded component with its own test surface before it grows.

Evidence base (require-graph audit of the public clone, 2026-08-02, this
plan's design session): sim's 20 namespaces layer cleanly with no cycles.
Zero-dep schema namespaces: `pathway`, `facility`, `persona`, `config`,
`site-profile`, `version`. Pure trajectory pipeline: `gmf` → `pathway`;
`gmf-interpreter` → `gmf`; `compile-trajectory` → `facility`. Engine
cluster: `engine` → {churn, compile-trajectory, config, facility,
gmf-interpreter, order-profiles, pathway, persona}. Emitters:
`emit-hl7` → {config, site-profile}; `v2-replay` → `emit-hl7`;
`emit-state` → `engine` (direct, non-interface — a seam S4 must design).
Orchestration: `run`, `check`, `identifiers`, `manifest`.

Load-bearing external fact: `ehrt.corpus.sim-adapter` calls
`ehrt.sim.interface/run-command` **in-process** (ADR-0012 edge
corpus → sim). The fat interface is therefore a live façade with an
external consumer, not just CLI plumbing. It survives every stage.

## The sequence

**S1 — `sim-model`** (forced prerequisite, pure move, zero logic).
Extract `pathway`, `facility`, `persona`, `config` as `ehrt.sim-model.*`
with a thin delegating interface. These are the schemas everything else
consumes; S2 cannot exist without them unless trajectory depends on
sim's fat interface, which inverts the intended direction. `site-profile`
does NOT move here — it is emitter vocabulary (MSH dialect, rendering
accents), and vocabulary is load-bearing: it belongs with the emitters
(S3), not the ground-truth model. `version` stays in the residual
(manifest/provenance concern).

**S2 — `sim-trajectory`** (the agreed target). Extract `gmf`,
`gmf-interpreter`, `compile-trajectory` as `ehrt.sim-trajectory.*`,
depending on {sim-model, kernel} only. Component docs move with it:
`gmf-interpreter.md`, `gmf-source-model.md`, `trajectory-computation.md`,
plus the vendored-module fixtures and their tests
(`gmf_test`, `gmf_interpreter_test`, `gmf_horizon_test`,
`compile_trajectory_test`, `vendored_*_test`). Interface derived from
grep evidence of live callers (`engine`, `run`), not judgment — the
same discipline R5 used for the fat interface, now applied per-child.
S1 and S2 may run as one session, two commits, S1 green before S2 starts.

**S3 — `sim-emit-hl7`**: `emit-hl7`, `v2-replay`, `site-profile`.
Motivated by the horizon, not hygiene: FHIR/CDA state-based emitters
land as sibling components (`sim-emit-fhir`, `sim-emit-cda`) rather than
re-fattening sim — formats are rendering accents over the ground-truth
log, and the component graph should say so. `emit-state` does NOT move
in S3 (it reads `engine` directly; its seam is S4's design problem).

**S4 — `sim-engine`** (last, highest coupling): `engine`, `churn`,
`order-profiles`. Requires designing two surfaces that don't exist yet:
what engine exposes to state-reading emitters (`emit-state`), and what
it exposes to acceptance (`check`). Trigger condition, not calendar:
run S4 when a second engine consumer appears (the FHIR emitter is the
likely one) or when engine work itself needs the boundary. Until then
engine stays in the residual and nothing is worse than today.

**Residual `sim`** after S1–S3: `run`, `check`, `engine`+`churn`+
`order-profiles` (until S4), `emit-state`, `identifiers`, `manifest`,
`version`, and the façade `ehrt.sim.interface` — unchanged surface,
internally repointed. Named holes, deliberately not in this plan:
`manifest` as its own micro-component (blocked on the sim↔corpus
manifest-interop ruling, roadmap Deferred); `config`'s final home
(model for now; revisit at S4 if it proves run-scoped rather than
model-scoped); renaming the residual.

## Author rulings needed

* **AR-1 Names.** Proposed: `sim-model`, `sim-trajectory`,
  `sim-emit-hl7`, `sim-engine` — the `sim-` prefix keeps the family
  visible in `poly info` and leaves bare `trajectory`/`model` free for
  any future non-sim meaning. Alternative considered and not
  recommended: `patient-trajectory` (longer, and the component compiles
  module content, not only patient state).
* **AR-2 S1 scope.** Ruled set for sim-model = {pathway, facility,
  persona, config}? The contested member is `config` (needed by
  emit-hl7 and check as well as engine; keeping it in the residual
  would force S3's emitter to depend on the residual sim, inverting
  direction — hence the proposal to move it). `site-profile` excluded
  per the emitter-vocabulary argument above.
* **AR-3 Façade permanence.** `ehrt.sim.interface` keeps its exact
  surface through all stages (corpus depends on it, ADR-0012). Whether
  it eventually thins to a true orchestration interface or corpus
  repoints to child interfaces is a post-S4 ruling, not this plan's.
* **AR-4 Same-session pairing.** S1+S2 as one session (two commits) or
  two sessions? Proposal: one session — S1 alone delivers nothing
  user-visible and the pairing keeps the rename churn in one window.

## Risks and mitigations

* **R-1 Behavior drift in a determinism-law codebase** (RNG consumption
  order, seeded walks). Mitigation: the sim's own byte-reproducibility
  is the oracle. Before S1: one fixed-seed golden run, record every
  output file's sha256 (the manifest already carries these). After each
  stage: identical run, byte-identical outputs required. This is
  stronger than test-passing and costs one command. Plus
  move-don't-improve: every moved namespace's body diffs only in its
  `ns` form; per-push lane prints the namespace diff list (stage-2/3
  baseline template).
* **R-2 Rename fan-out.** `ehrt.sim.pathway` → `ehrt.sim-model.pathway`
  touches every internal consumer. Mitigation: full caller map in the
  characterization step before any edit (stage-3 precedent); mechanical
  rename; `poly check` catches any missed non-interface require.
  External blast radius is zero by construction: corpus touches only
  the façade (verified 2026-08-02), and the CLI base composes
  interfaces.
* **R-3 Silent test loss.** tools.deps does not propagate `:test`
  extra-paths through `:local/root`; a moved test dir missing from root
  `deps.edn` vanishes from IDE and raw runs. Mitigation: deftest-count
  parity gate — sim holds 375 deftests today (counted this session);
  sum across {residual + new components} must equal the baseline ±
  explicitly listed additions. Both lanes green per stage.
* **R-4 Improvement creep.** The tools split allowed one sanctioned
  improvement per stage (the interface). Same rule here: interfaces
  are designed (from caller evidence), everything else is moved.
  CallSubmodule and all coverage expansion are explicitly OUT of every
  extraction session — they are the payoff milestone that starts only
  after S2 lands.
* **R-5 Docs and link rot.** Moving the three trajectory docs breaks
  relative links; current-tense prose elsewhere cites `ehrt.sim.gmf`.
  Mitigation: extend the stale-path tripwire with `ehrt\.sim\.gmf` /
  `ehrt\.sim\.compile-trajectory` (current-tense docs surfaces only —
  confirm the tripwire's existing archive exclusions cover
  `notes/sim/`, which stays frozen; fix-forward, never rewrite).
* **R-6 Mid-sequence stall.** Every stage ends green, committed, and
  self-sufficient; a half-split sim is valid Polylith indefinitely.
  Roadmap rows track per-stage state so a cold session knows where the
  sequence stands.
* **R-7 Concurrent-session collision.** Stages produce wide mechanical
  diffs. Rule: no other build session in flight during an extraction
  stage; author commits each stage before the next starts.
* **R-8 Workspace bookkeeping.** Each stage: root `deps.edn` `:dev` +
  `:test` entries, project `deps.edn`s, `workspace.edn`, `:necessary`
  re-derived (stage-3 AR-4 precedent), structure-currency red→green on
  each new component directory.

## Verification baselines (per stage)

1. Fixed-seed golden run byte-identical to the pre-S1 recording (R-1).
2. deftest-count parity vs the 375 baseline (R-3).
3. `poly check` clean; `poly test :all` green; both lanes green.
4. Per-push lane namespace diff list: renames only.
5. Seam CLI commands (`ehr sim run`, `ehr help`) byte-identical output.
6. Docs tripwire green after the R-5 pattern extension (red→green
   moment recorded).

## What lands where

- This file: `.agents/plans/2026-08-02-sim-split-plan.md`.
- Roadmap: one row in `Next` — "sim split S1+S2 (model, trajectory) —
  plan: .agents/plans/2026-08-02-sim-split-plan.md, awaiting AR-1..4";
  move to `Now` on approval. S3/S4 rows enter `Deferred` with their
  trigger conditions.
- Per-stage session prompts: authored in the design channel after the
  rulings, archived to `.agents/prompts/` per current convention
  (`notes/prompts/` is sealed).
- ADR: the executing session writes the workspace ADR for the split
  (dependency directions: `sim-trajectory → {sim-model, kernel}`,
  `sim → {sim-model, sim-trajectory, ...}`, forbidden-forever list),
  citing this plan.
