# Refactoring Plan: Judge/Gate Factorization + Palgebra Claim

**Companion:** [`docs/palgebra-design.md`](../../docs/palgebra-design.md) (decisions D1–D13, open O1–O5). Every rename below cites a decision.
**Operating rule:** everything from the design gets written down; **only the terminology gets built.** `lower`, `erase`, `emit`, passes, roundtrip tests are specified obligations (design §I.5–I.6), not work items here.
**Goal served:** a workable EHR testing workflow whose vocabulary matches its architecture — not a language implementation.

---

## Phase 0 — Documents land first

1. Commit `docs/palgebra-design.md`, decision register complete. Done — landed at `docs/palgebra-design.md`, no relocation needed.
2. **ADR-0009 — Judge/gate factorization** in `notes/ADRs.md` (next number after ADR-0008). Thesis sentence: *gate was an act-layer word on decide-layer components; each rename below is a factorization of one layer's concern out of another layer's name.* One-line technical justification: the old `:gate` kind's three outputs are derivable (`judge ⨟ route-by-verdict`), not primitive. Cites D1, D2, D11, D12.
3. Resolve **O1** (verdict value names) and **O2** (terminology-suppressed: `no-verdict` vs `ambiguous`) — both block Phase 3. Recommended: O1 = keep `:pass/:rejected` in v1 (serialization stability; revisit at signature-format v2); O2 = `no-verdict(:terminology-suppressed)` per design §II.5.

## Phase 1 — Mechanical renames (D12), one commit per bullet

Repo inventory (2026-07-25, shallow clone of `pragsmike/ehr-testing-tools@main`): "gate" appears in ~70 files; the live blast radius below excludes `.agents/prompts/archive/*` and experiment records, which **stay as history**.

- **Namespaces** (`git mv` + ns-form edits + require sites):
  - `src/ehr_testing_tools/gate/{fhir,v2,finding,report}.clj` → `src/ehr_testing_tools/judge/…`
  - matching `test/ehr_testing_tools/gate/*_test.clj` → `test/…/judge/…`
  - require sites: `cli.clj` (lines ~19–21), `check.clj`, `check/schemas.clj`, both `test-integration/` suites, `core.clj` if it aliases.
- **EDN data** (refactoring tools won't see these — string-level edits):
  - `docs/pipeline.edn`: `:kind :gate` → `:kind :judge`; the kind's laws split per design §II.7 (judge laws stay; route-by-verdict documented as the derived, policy-bearing construct — full notation treatment deferred to Phase 4/O3). 9 "gate" occurrences.
  - `docs/use-cases.edn`: 14 occurrences — stage ids/labels referencing gate.
  - `pipeline.clj`'s `stage-kinds` set: `:gate` → `:judge` (this is signature data hardcoded in code — flag for Phase 2 claim).
- **Docs**: `docs/gate-calibration.md` → `docs/judge-calibration.md` (title + inbound links from `pipeline.edn` law text, `README.md`, `docs/README.md`, `components.md`, `pipeline.md`, `use-cases.md`, `engine-onboarding.md`, `positioning.md`, `experiments.md`). README maturity table wording. `docs/notation.md`'s five-kinds table row.
- **CLI**: `ehr gate` verb **keeps its name** (D12). Internal fn names (`gate-command`, `gate-file-fn`…) may keep or move to judge-vocabulary at implementer's discretion — the *verb* is the contract. Add the explicit policy surface: `--fail-on`, `--treat-no-verdict-as` (joining the existing `--baseline`).
- **`:disposition` rename (D11)**: `gate.fhir` line ~211 finding key `:policy` → `:disposition`; `worst-of` call site (line ~226); `gate.report`'s echoes (lines ~97, ~119); any report fixtures. Do this **in Phase 1**, before any `policy` namespace exists.
- **Verify after each commit**: full test suite + both integration suites + `make pipeline` regenerates `pipeline.md` cleanly + tier-1 lint passes.

Grep discipline: search `gate` as a *string* (EDN keywords, doc prose, law text inside `pipeline.edn` — e.g. the format-dispatch law naming `gate.fhir`/`gate.v2` verbatim), not just as a symbol.

## Phase 2 — The palgebra claim sweep (D9). Claiming, not improving.

Placement test per file: *names a sort or stage → stays `ehr-testing-tools.*`; speaks only in wires/boxes/composition/laws → `palgebra.*`.*

**Claims (move + rename namespace only):**
- `.agents/skills/string-diagram/` — the skill doc, `resource_equations_to_mermaid.py`, and the cyberneutics example equation sets (lemon-pie, committee, deliberated-choice — already generic). Skill splits per design §I.7: notation-in-general (palgebra's teaching material, incl. the `⨟` decree) vs. this-repo's-diagrams guidance (EHR docs that *use* the skill). `notation.md` already draws this line in prose — make the file layout match it.
- `pipeline.clj`'s generic half: equation-EDN loading, `Stage`/`UnionResource` schema shapes, validation plumbing → `palgebra.signature` (or similar). The **entangled frontier** within it: the `stage-kinds` enum and any EHR-specific law prose are *signature data* — they move to EDN the palgebra loader reads, which is the D13 discipline arriving early and cheaply.
- `lint.clj`'s catalytic-resolution *mechanism* → palgebra; the four concrete targets and `catalytic-resource-targets` mapping stay EHR-side as data.

**Stays EHR-side:** everything in `judge/*`, `check*`, `corpus/*`, `diff`, `locator`, `canonical*`, `artifact`, `digest`, `invocation`, `usecases`, `result`, `lineage`, `cli`, `core`.

**Same-day infrastructure:**
- CI namespace-graph lint: `palgebra.*` requires no `ehr-testing-tools.*`, tests included.
- `palgebra/HISTORY.md`: cyberneutics → primitive palgebra → this design's embellishments; surface any upstream attribution/licensing expectations now. (`notation.md`'s verified upstream link is the seed.)
- Toy signature (two sorts, three stages) in palgebra's test tree — proves instantiation is data-authoring, exercises the loader without EHR freight.

**Explicitly out of scope:** rewriting claimed code toward the specified language (sorts, `⨟` combinators, lower/erase/emit). Claimed-but-primitive is the intended state; the design records the gap.

## Phase 3 — The one semantic change: the verdict split (D10)

Isolated from the renames so mechanical commits don't camouflage the migration.

- Add the fourth arm per O1/O2 resolutions: `no-verdict` with a cause, in a distinct position (recommended shape: `{:verdict :no-verdict :cause :terminology-suppressed}` vs. the three plain verdict keywords — or a tagged pair; decide with the Malli schema).
- `judge/finding.clj` (`worst-of`): fourth case in `verdict-rank`; decide `no-verdict`'s rank explicitly (recommended: above `:rejected` for aggregation visibility — a corpus you couldn't fully judge is not a corpus that passed). Empty-seq ⇒ `:pass` unchanged.
- `judge/fhir.clj`: terminology-suppressed classification emits the new form (O2). `judge/v2.clj` unchanged — documented as never producing the old `:indeterminate`; its docstring's claim becomes "never produces `:ambiguous` or `:no-verdict`."
- `judge/report.clj`: report schema and diff handle the fourth arm; the line-~119 comment about preserving `:indeterminate` resolves itself.
- CLI policy surface (`--treat-no-verdict-as`) consumes it — the policy-totality law enforced by the resulting match/spec failures, which is the law working as designed.
- Both integration suites updated; **contract-pairing suite is the polarity regression** — it must still read `:rejected` as success untouched, proving the split didn't disturb the judge/policy boundary.
- One commit (or one tightly-scoped PR), its own spec failures, its own test delta.

## Phase 4 — Deferred, recorded, not scheduled

- Signature format v1 as EDN (D13) beyond what Phase 2's data-extraction forces.
- Re-expressing the full pipeline in the new notation — **this is what adjudicates O3** (routing in the algebra vs. above it); do not decide O3 on paper.
- `lower`/`erase`/`emit`, passes, roundtrip + idempotence property tests, the fixed-point test (design §I.5 — first test *when* building begins).
- The payload-production signature sketch (the generality test, an afternoon when curiosity strikes).
- Repo extraction — triggered by a second instance or an external user, executed along this document's Phase 2 boundary.

## Order and effort

Phase 0 → 1 → 2 → 3 strictly (0 gates everything; 1 before 2 so moves carry final names; 3 last because it's the only change that can break behavior). Phases 0–2 are hours-scale and risk-free given the test suite; Phase 3 is the one to do with coffee and the integration suites open.
