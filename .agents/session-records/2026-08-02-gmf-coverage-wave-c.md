# 2026-08-02 — GMF coverage Wave C: `Death` — terminal contract, `:expired` status lands in code

## Scope

Third session of the GMF coverage-expansion arc (Wave A: ADR-0026;
Wave B: ADR-0027). `Death` — the interpreter learns Synthea's Death
state, the trajectory gains a terminal `:death` event, and death folds
through to the ground-truth patient state (`:expired`, docs-only until
this session). Design ruled in the design channel same day, recorded
verbatim in `notes/ADRs.md` ADR-0028 (C1–C7). Target payoff: `stroke.json`.
Full decision record: ADR-0028.

## Characterization (Step 1) findings

- **Death's own real forms, grounded against `State.java` at the pinned
  commit** — three time forms (immediate/exact/range, `range` costing
  exactly one rng draw, the SAME shape `Delay`/`Procedure` duration
  already uses) and three cause-of-death forms (`codes`,
  `condition-onset`, `referenced-by-attribute`). `stroke.json`'s own
  Death state uses exactly the `range` time form and the `codes` cause
  form; the other cause forms are named unbuilt (interpreter throws,
  the same disposition an unsupported condition type already gets).
  Real Synthea's own module CONTINUES past `Death` to its own declared
  transition — this project's own C2 ruling deliberately departs from
  that (the walk terminates AT `:death`), disclosed, not an oversight.
- **`stroke.json`'s own closure is trivial** — a single file, no
  `CallSubmodule`, three of the now-six known transition kinds used
  (`direct`/`distributed`/`conditional`), D7's hidden-import check
  vacuous by construction (a one-file closure has no cross-module
  reference to check).
- **The `:expired` gap table, checked against LIVE code, not docs'
  prose** — `:expired` existed nowhere in `components/sim/src` except
  three lines of comment in `check.clj`. `order-only-when-admitted`/
  `clinical-content-only-when-admitted` already generalize to cover
  `:expired` automatically once it's a real, distinct status value
  (zero code change for that half, confirmed by the green suite).
  Declared minimal coherent path: `:expired` joins the status enum;
  `:death` maps via `:discharge`'s own two new optional fields (no new
  IR step type); `:discharge`'s decide/evolve branch on `:disposition`
  and — a finding the docs' own gap table didn't name — suppress the
  existing bed-ready-transfer coupling (no bed is actually vacated by
  an expired-disposition discharge). No escalation triggered — the
  minimal path touches no pathway-IR step type and no `sim-model`
  schema beyond two optional fields.
- **A genuinely new, unplanned, escalation-worthy finding**:
  `Chance_of_Stroke`'s own `distributed_transition` gates onset on
  `{"attribute": "stroke_risk", "default": 0}` — real Synthea's own
  Framingham cardiovascular-risk score (`CardiovascularDiseaseModule`),
  a hard-coded ENGINE module this project has no source for. Honoring
  the JSON's own `default: 0` literally makes stroke onset — and the
  `Death` branch this wave exists to unlock — structurally unreachable,
  not merely rare, worse than the two prior attribute-sourced-data gaps
  this project already accepted (`type_of_care_transition`'s payer
  attribute, `Active Allergy`), both of which left a real default
  branch reachable. Escalated to the author mid-session (design
  channel) rather than silently resolved either way, since it directly
  threatened the wave's own stated payoff claim. **Ruled: `stroke.json`
  stays deferred this wave** (the same D6 treatment ADR-0027 gave a
  dirty closure) — `Death` built and proven in full regardless, against
  this project's own hand-authored `death-fixture.json`.

## Red→green evidence highlights

- Every new mechanism went RED first, for the right reason: the death-
  fixture's own first draft (a single near-birth event, no recurring
  onset gate) produced ZERO operational content in a 40-patient engine
  run — the SAME known limitation `fixture-clinic.json`'s own episode
  already has (`ehrt.sim.engine-test`'s own comment), rediscovered
  live. Fixed by giving the onset gate a recurring, sinusitis.json-
  shaped monthly-tick loop instead of a single episode.
- A second real, live-found interaction: the death-reaching seed a
  mixer-RNG search first landed on had EARLIER, survived encounter
  cycles ahead of the final fatal one — `compile-trajectory`'s own
  PRE-EXISTING `encounter-closed?` mechanism (unrelated to Wave C, the
  M7 survey's own "multi-encounter-per-episode" finding) silently
  dropped the LATER, fatal cycle once an earlier cycle's own
  `:encounter-end` had already compiled. Resolved by picking a seed
  that dies on its own first cycle for the compile-trajectory-level
  test specifically (interpreter-level and full-engine-level tests are
  unaffected — they don't depend on which cycle wins).
- `poly check`: clean at every checkpoint. `poly test project:dev`: 0
  failures/0 errors at every checkpoint, and one final full-workspace
  run at session close.
- Regression oracle (6 seeds × 2 sexes × the four currently-vendored
  roots — `sinusitis`/`appendicitis`/`sore_throat`/`ear_infections`)
  re-run after every commit: byte-identical across all Wave C
  checkpoints.
- The full engine/check round trip (200 patients, the death-fixture
  module configured) proves both outcomes present for a fixed seed (6
  died, 20 recovered of 26 who reached an in-window encounter), the
  full invariant catalog holds including the new
  `expired-patient-retains-location`, and at least one patient's
  folded state is genuinely `:status :expired`.

## Judgment calls and their ratification status

- The `stroke_risk` finding and its own escalation are the session's
  one real deviation from the prompt's own literal text — ratified
  live (design channel, mid-session), not merely disclosed after the
  fact. Recorded in ADR-0028's own Deviation record, the prompt
  archive's own closing section, and this document.
- Building `exact`/immediate Death time forms even though `stroke.json`
  uses only `range` — a scope judgment, not literally required by C1's
  own "only what's used" instruction, justified as zero-marginal-cost
  reuse of the existing `resolve-time-advance` helper (not new
  mechanism, ADR-0013 point 4's own curation discipline is about real
  complexity, not free generalization).
- The death-fixture's own recurring-loop shape (unlike stroke.json's
  real single-episode design) — a fixture-design judgment, not a
  ruling; disclosed in the fixture's own remarks and this session's own
  commit message, not silently chosen.

## Findings and HEAD landed

- This session ran under R30 (commit and push at each checkpoint,
  unattended), the ratified standing default, exactly as the prompt
  itself named. Every checkpoint's `git diff --cached --stat` matched
  that checkpoint's own file list before staging; `gitleaks git
  --staged -v` clean at every commit; every commit message written to
  the WSL scratch tree first (Write tool's own UNC path, never an
  inline heredoc through the wsl wrapper); `git commit -F`'d; every
  push verified against its message file (every diff was the expected
  trailing-newline formatting artifact, never a real mismatch).
- Reading-set budget self-caught once (`:onboarding`, 845→894 lines,
  this Step 4's own roadmap growth) — the same recurring pattern every
  prior GMF-wave session's own record already describes; fixed forward
  in this same close-out commit.
- HEAD at session end: this record's own companion commit (`docs: Wave
  C records (ADR, survey, roadmap; archives prompt)`) — see that
  commit for its own sha. Prior commits, in order: `7e4204b`, `ed4f7bd`,
  `a900f99`, `47d0f66`, `380a3e2`, `66005ae`.
