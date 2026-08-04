# 2026-08-04 — sim split B plan: full decomposition of components/sim

Status: RULED (author, 2026-08-04, design channel; rulings AR-1..AR-6
below, recorded verbatim). Supersedes-by-citation, not by edit:
`.agents/plans/2026-08-02-sim-split-plan.md` (S1–S3 EXECUTED, S4
scoping inherited here; that plan's text stands as written per the
annotate-not-rewrite convention — it receives a dated note, below).
Method precedent: the three-stage tools split and the S1–S3 executions
(characterize → extract → verify → records; escalate once with edges
named; move-don't-improve with one sanctioned improvement per stage —
the interface).

## Context

`components/sim` after S1–S3 still holds nine source namespaces plus
the façade. Survey evidence (public clone at `8f697f7`, 2026-08-04,
this plan's design session — require-graph read from the ns forms, not
from prior survey rows):

| ns | LOC | internal deps | external deps |
|---|---|---|---|
| `engine` | 1573 | churn, order-profiles | sim-model, sim-trajectory, malli |
| `check` | 571 | engine, order-profiles | kernel, sim-model |
| `run` | 345 | engine, check, churn, emit-state, manifest | kernel, sim-model, sim-trajectory, sim-emit-hl7 |
| `emit-state` | 267 | engine (only) | — |
| `churn` | 197 | — | sim-model, malli |
| `identifiers` | 136 | emit-state, engine, run | kernel, sim-emit-hl7 |
| `order-profiles` | 113 | — | sim-model, malli |
| `manifest` | 87 | version | malli |
| `version` | 63 | — | — |
| `interface` | 35 | check, identifiers, run, version | kernel |

No cycles. Layering: {version, churn, order-profiles} → engine →
{emit-state, check} → run → identifiers → façade; manifest is a
version-only side branch.

External consumers, load-bearing:

- `ehrt.corpus.sim-adapter` calls `ehrt.sim.interface` in-process
  (ADR-0012). The façade survives every stage byte-identical (08-02
  plan AR-3 permanence stands; post-split thinning remains a separate
  future ruling).
- `bases/cli` composes the façade.
- `components/sim-emit-hl7`'s six vendored/replay test files require
  `ehrt.sim.engine` and `ehrt.sim.check` directly (test-scope, legal
  Polylith; named rename fan-out for M2 and M4).

Manifest cycle proof (why a shared home is forced, not stylistic):
corpus → sim exists (`sim_adapter` requires the façade), therefore
sim → corpus for the schema would be a cycle. The only acyclic single
home for ManifestV1_1 is a component both depend on.

## Rulings (recorded verbatim, author, 2026-08-04)

- **AR-1 (scope).** Option B — full decomposition: `sim-engine`,
  `sim-emit-fhir`, `sim-check`, shared `provenance` component,
  orchestration residual `sim`. (Option A, engine-only, was the
  considered-and-declined fallback.)
- **AR-2 (shared schema home).** Component name `provenance`
  (`ehrt.provenance.interface`), holding ManifestV0/V1/V1_1 schemas +
  `valid?` predicates moved from `corpus/manifest.clj`. Builders stay
  producer-side: corpus keeps `build`/`build-v1-1`, sim keeps its
  `build` (validating against the shared schema). Name chosen over
  bare `manifest` to avoid three-things-called-manifest ambiguity
  during migration; leaves room for the schema family to grow (e.g.
  `corpus-io`'s operation manifest — noted, not proposed).
- **AR-3 (FHIR emitter).** Component `sim-emit-fhir`; namespace
  renamed `ehrt.sim-emit-fhir.emit-fhir` (the one sanctioned
  improvement, S3 precedent). Sibling to `sim-emit-hl7`; `sim-emit-cda`
  is the named-future third sibling per emit-state's own contract note.
  Sibling means peer rendering accent, not same input shape: hl7
  renders from the event log, fhir (and future cda) render from folded
  state — the two consume different sim-engine interface surfaces.
- **AR-4 (S4 trigger reconciliation — framing (b), author override,
  plainly stated).** The dated notes on the roadmap's S4 row and the
  08-02 plan read: author rules the split proceeds ahead of the
  recorded trigger (cleanup-arc scoping, 2026-08-04). The trigger's
  reasoning — don't design a boundary with one consumer — is honored
  in substance: M3 (`sim-emit-fhir`) is committed scope in the same
  sequence, so M2's boundary is designed against two known consumer
  surfaces even though the second lands a session later. Not claimed:
  that the trigger "fired" — at M2's execution the second consumer is
  promised, not present.
- **AR-5 (contract-test fate — option (b), convert).** The
  test-integration `sim-manifest-contract-test`'s drift-detection
  purpose retires with dated disclosure citing the mirror docstring's
  own lesson (a mirror validating its own output against its own
  schema copy agrees with its own mistakes); drift becomes impossible
  by construction once both sides read the same var. Its
  builder-validity purpose survives as a plain unit test next to sim's
  `build`: the built manifest validates against `provenance`'s
  ManifestV1_1. The M1 session record states explicitly which purpose
  died and which moved.
- **AR-6 (sequencing).** Approved as proposed: M1 → M2 → M3 → M4.
  M2-before-{M3,M4} is forced (both depend on interfaces M2 designs);
  M3 before M4 so AR-4's committed-second-consumer claim discharges
  early.

## The sequence

**M1 — `provenance` + vestige sweep** (small, independent, de-risks).
Create `components/provenance` with ManifestV0/V1/V1_1 + validators
moved from `corpus/manifest.clj`; corpus repoints. Retire
`sim/manifest.clj`'s `MirroredManifest` and its tripwire test (dated
disclosure cites the mirror docstring's M3-Task-0 lesson); thin
`sim/manifest.clj` to build-only, validating against `provenance`.
Convert the test-integration contract test per AR-5(b). Write the
intake-front-door doctrine down (sim runs enter `ehr corpus intake` as
if foreign — the discipline that caught real defects) in the split ADR.
Vestige sweep, per-file judgment, current-tense surfaces only (frozen
archives untouched): `sim_adapter.clj`, `sim_adapter_test.clj`,
`intake_test.clj`, `cli/core.clj`, `docs/dev/way-of-working.md`
cross-repo/pack-push language; the façade docstring's fat-component
disclosure updates as the split lands (M2–M4), not here. Place AR-4's
dated notes on the roadmap S4 row and the 08-02 plan.

**M2 — `sim-engine`** (highest risk: ~1,883 src lines move). Extract
{engine, churn, order-profiles} as `ehrt.sim-engine.*`. Design the two
interfaces from caller evidence gathered in the characterization step
(grep, not judgment — R5 discipline; the requires above are verified,
var-level lists are this session's job):

- state-reader surface — what emit-state and identifiers reach today
  (`replay`, `patient-id-for`, run output shapes);
- acceptance surface — what check reaches today (`replay`, event
  vocabulary, order-profiles defaults).

`run`'s and the façade's requires repoint; sim-emit-hl7's six test
files repoint (`ehrt.sim.engine` → the new interface or internals per
test-scope norms — characterize first). Oracle bracket: pure identity,
all ELEVEN batches (nine legacy + two history). Escalate on any digest
change.

**M3 — `sim-emit-fhir`.** Extract emit-state as
`ehrt.sim-emit-fhir.emit-fhir` (AR-3 rename), depending on
sim-engine's state-reader interface + whatever the characterization
finds (it currently requires engine only). `run` and `identifiers`
repoint. The `org.clojure/data.json` dep moves with it if the
characterization confirms it's emit-state's (today it appears in sim's
deps.edn; the serialization call sites found are CLI-side — verify,
don't assume). AR-4's second-consumer claim discharges here: record it
in the session record.

**M4 — `sim-check` + residual thinning.** Extract check as
`ehrt.sim-check.*` depending on sim-engine's acceptance surface +
kernel + sim-model. sim-emit-hl7's vendored tests repoint their check
requires. Residual `sim` = {run, identifiers, version, manifest-build,
façade} — pure orchestration; façade surface unchanged, its docstring's
fat-component disclosure finally retires with a dated note. Co-landing
note for the ADR: "every step type lands with its invariants in the
same change" is a commit discipline, unaffected by check living in a
sibling component.

## Risks and mitigations

Inherited unchanged from the 08-02 plan: R-1 (determinism drift — the
byte-reproducibility oracle brackets every stage; move-don't-improve,
ns-form-only diffs), R-2 (rename fan-out — full caller map before any
edit), R-3 (silent test loss — deftest-count parity; root `deps.edn`
`:test` paths per stage), R-4 (improvement creep — interfaces designed,
everything else moved; AR-3's namespace rename is the one sanctioned
improvement and it is M3's), R-5 (docs/link rot — stale-path tripwire
extended per stage for `ehrt\.sim\.engine`, `ehrt\.sim\.check`,
`ehrt\.sim\.emit-state`, `ehrt\.sim\.manifest` on current-tense
surfaces), R-6 (mid-sequence stall — every stage ends green, committed,
self-sufficient), R-7 (no concurrent sessions during extraction), R-8
(workspace bookkeeping per stage).

New to this plan:

- **R-9 (three manifests during M1).** Until M1 completes,
  `corpus/manifest.clj`, `sim/manifest.clj`, and `provenance` coexist.
  Mitigation: M1 is one session, single-topic; commit order inside it
  is create-provenance → repoint-corpus → retire-mirror, each green.
- **R-10 (test-scope repoints in a sibling component).** sim-emit-hl7's
  vendored tests are the only cross-component internal requires.
  Mitigation: they are test-scope (Polylith permits reaching
  implementation from test); repoint mechanically in M2/M4; deftest
  parity covers loss.
- **R-11 (oracle producer-shape coupling).** The regression oracle
  reads `digest.clj` from the current checkout (ADR-0030 J2; known
  incompatibility when producer call shapes change across the bracket).
  Extractions rename namespaces the digest producers require.
  Mitigation: each stage's bracket runs with the documented workaround
  if the baseline worktree cannot compile the current `digest.clj`;
  the expected-change set for every bracket is "none" (pure identity)
  and anything else escalates.

## Verification baselines (per stage)

1. `bin/regression-oracle <pre-stage-tip> <stage-landing>`: all eleven
   batches byte-identical (nine legacy + `ear-infections-history-engine`
   + `urinary-tract-infections-history-engine`). Any digest change
   escalates — no stage in this plan may change behavior.
2. deftest/defspec parity: sum across {residual + new components}
   equals the pre-M1 baseline (grep-count 229 in `components/sim/test`
   at `8f697f7` — the M1 characterization step counts properly and
   records the authoritative number) ± explicitly listed
   additions/retirements (the tripwire test retires in M1 with
   disclosure; the converted builder-validity test is its named
   replacement).
3. `clojure -M:poly check` clean; full suite green; both lanes green.
4. Per-push namespace diff list: renames only (plus the M1 schema move
   and AR-3's M3 rename, both disclosed).
5. Façade seam: `ehr sim run` / `ehr sim check` / `ehr help` output
   byte-identical across every stage.
6. Stale-path tripwire green after each stage's R-5 pattern extension
   (red→green moment recorded).

## What lands where

- This file: `.agents/plans/2026-08-04-sim-split-b-plan.md`.
- Roadmap: S4's Deferred row gets AR-4's dated note and the sequence
  enters `Now` as four rows (M1–M4), each pointing here.
- `.agents/plans/2026-08-02-sim-split-plan.md`: dated note (AR-4
  wording) — annotated, not rewritten.
- Session prompts: authored in the design channel per stage after this
  plan lands, archived per convention.
- ADR: M1's executing session writes the split ADR (dependency
  directions: `provenance` ← {corpus, sim}; `sim-engine` ←
  {sim-check, sim-emit-fhir, sim}; forbidden-forever list;
  intake-front-door doctrine; AR-1..AR-6 verbatim), citing this plan.
  Subsequent stages append dated execution records to it.

## Standing reminder

ADR-0042's ratification note (Step 3 finding, `history-phase?`
reference-chain extension) is a separate, pending author commit —
drafted in the design channel 2026-08-04, not part of this plan's
sessions.
