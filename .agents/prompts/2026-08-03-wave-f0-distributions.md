# 2026-08-03 — Wave F0 distributions session prompt

Repo: `ehr-testing-tools`. Ran as a Claude Code session against the
native-Windows working directory
(`C:\Users\prags\Documents\ehr-testing-tools`), with all git/build
operations routed through `wsl -e bash -lc "cd ~/src/ehr-testing-tools && ..."`
against the WSL ext4 clone, per this repo's own WSL-only-git
convention; file edits routed through the UNC path
(`\\wsl.localhost\Ubuntu\home\mg\src\ehr-testing-tools\...`) so they
land on the same clone the git/build commands target
(`feedback-dual-clone-edit-hazard`). Found, at Step 0's own first Edit
attempt: writing through the native Windows path hit `EPERM` on rename
— the `/mnt/c` clone's own read-only guard (post-Wave-D cleanup
session, ADR-0030 J4) firing exactly as designed, not a tool bug;
switched to the UNC path for every subsequent edit, per this repo's own
dual-clone precedent. Preflight otherwise clean: ext4 clone at
`origin/main`'s own HEAD (`d9545c9`), no uncommitted changes; Synthea
checkout (`/home/mg/synthea-checkout`) confirmed at the pin
(`7e08387c68a7f0e21d13076609a159fd473fc902`) via `git rev-parse HEAD`.

## Prompt, verbatim

> 2026-08-03 — Build session: Wave F0 — distribution kinds (GAUSSIAN / EXPONENTIAL / TRIANGULAR)
>
> Context
>
> The first census (ADR-0034, artifact `2026-08-03-synthea-7e08387.edn`) ranked the frontier; the design channel's ranking read was ratified by the author (2026-08-03): a new small wave — F0, distributions — runs first (11 modules blocked by `EXPONENTIAL`/`GAUSSIAN` timing distributions, tied for the largest single-mechanism unlock, plus a real loader-robustness bug: unknown kinds THROW uncaught instead of rejecting cleanly, which the census tool itself had to defend against). The design channel additionally found a silent correctness gap the census cannot see: `:set-attribute` reads only `:value`/`:value-code`, so the 58 upstream SetAttribute states carrying distributions (e.g. `hypertension`'s GAUSSIAN onset ages) silently set `nil` today — modules census as `:ok-walked` while walking with nil attributes feeding their guards. This session owns all three contexts: Delay timing, Procedure duration, SetAttribute values. Semantics are pre-pinned from Synthea source at the pin (`7e08387c68a7f0e21d13076609a159fd473fc902`, `src/main/java/org/mitre/synthea/engine/Distribution.java`) — cite, don't re-derive. No vendored root uses the new kinds or SetAttribute distributions (design-channel survey), so the oracle claim is PURE IDENTITY.
>
> Read first
>
> 1. `AGENTS.md`; `.agents/skills/build-session/SKILL.md`
> 2. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf.clj` — `gmf-v2-timing->v1`, `apply-gmf-v2-timing`, `apply-gmf-v2-procedure-duration` (the `case kind` clauses that throw), the state schema (`:set-attribute`, `:delay`, `:procedure`)
> 3. `components/sim-trajectory/src/ehrt/sim_trajectory/gmf_interpreter.clj` — `resolve-time-advance`, `rand-int-in` (the draw-law home), the `:set-attribute` interpreter case
> 4. `notes/ADRs.md` — ADR-0032 AR-3 (fixed-consumption law), ADR-0034; next ADR expected 0035
> 5. `components/sim-trajectory/docs/census/2026-08-03-synthea-7e08387.edn` header (re-run parameters must match) and the census entry shape
> 6. `.agents/plans/2026-08-02-gmf-parity-plan.md` §4 (this session adds the ratified resequencing note, AR-8 below)
>
> Author rulings (design channel, 2026-08-03; record in ADR-0035)
>
> * AR-1 (semantics pin — port faithfully, cite verbatim). From `Distribution.java` at the pin: GAUSSIAN = `standardDeviation * gaussian() + mean`, then CLAMP to optional `min`/`max` parameters (clamping, not resampling). EXPONENTIAL = `1.0 + ln(1 - u) / (-1/mean)` — i.e. an exponential with the stated mean SHIFTED by +1; port the shift verbatim, it is upstream content truth. TRIANGULAR = the standard inverse-CDF over `min`/`mode`/`max` (port the two-branch formula verbatim). A `round: true` flag on any kind rounds the sampled value to the nearest integer. Required parameters per kind follow upstream `validate()`: EXACT `value`; UNIFORM `low`,`high`; GAUSSIAN `mean`,`standardDeviation`; EXPONENTIAL `mean`; TRIANGULAR `min`,`mode`,`max`.
> * AR-2 (coverage + robustness). Implement GAUSSIAN, EXPONENTIAL, and TRIANGULAR (TRIANGULAR has zero census-blocked modules at the pin but closes upstream's enum — one trivial single-draw formula) across all three contexts: Delay timing, Procedure duration, SetAttribute value. An UNKNOWN kind becomes a clean load-time rejection (Result value naming module/state/kind — the same rejection family the loader already uses), NEVER a thrown exception. This structurally closes the census's `:loader-exception` category.
> * AR-3 (draw law — fixed consumption, disclosed divergence). Every distribution sample consumes EXACTLY ONE uniform draw from the walk rng. GAUSSIAN samples via a single-draw inverse-CDF (a standard rational approximation of the probit function — e.g. Acklam's — cited in the code comment), which is NUMERICALLY DIVERGENT from `java.util.Random/nextGaussian` BY DESIGN: `nextGaussian` consumes a variable number of draws and caches state, both incompatible with the fixed-consumption law. Mean, sd, clamps, and rounding are preserved — fitness-for-purpose, not bit-parity with upstream. EXPONENTIAL and TRIANGULAR are already single-draw inverse-CDF upstream; port verbatim. Time-context samples: sample a double in unit space, convert through the SAME unit path the Range shape uses, with a deterministic rounding-to-granularity choice STATED in the ADR.
> * AR-4 (SetAttribute silent-nil fix + census blind-spot disclosure). `:set-attribute` gains distribution sampling per AR-1/AR-3 (honoring `round`); precedence `:value-code` > `:distribution` > `:value` is NOT the ruling — upstream precedence is: a `distribution` present means sample it (SetAttribute's value_code/value/distribution are mutually exclusive in practice; if a state carries several, record a load-time rejection rather than guessing). Record in the ADR that this gap was INVISIBLE to the census (loads and walks complete with nil attributes) — a named limitation of walk-verification: digests attest determinism, not value correctness.
> * AR-5 (internal shape). UNIFORM/EXACT keep their existing v1-collapse normalization untouched (no churn). The three new kinds survive loading as a normalized `:distribution {:kind :gaussian|:exponential|:triangular, ...}` with keyword kind and normalized parameter keys, schema-validated; the interpreter samples them where `:range`/`:exact` are read today and in `:set-attribute`.
> * AR-6 (oracle bracket — pure identity). No vendored root uses the new kinds or SetAttribute distributions, so EVERY oracle-covered root must come out byte-identical across the bracket. Any change is a STOP-AND-ESCALATE. (If the escalation turns out to be another incomplete design-channel survey, say so — that has happened once already, ADR-0032's execution note.)
> * AR-7 (census re-run). After the fix, re-run the census with the SAME header parameters (pin, seeds, persona, horizon) and commit the new dated artifact alongside the old (dated history, never overwrite) plus a delta note in the interpreter doc's §15: expected movement is the 11 `:loader-exception` modules resolving to `:ok-walked` or surfacing their NEXT blocker (either is a finding, record which), and previously-`:ok-walked` modules with SetAttribute distributions changing walk digests (now sampling real values — expected, disclosed). Anything else that moves: record it; a vendored root moving is a STOP-AND-ESCALATE.
> * AR-8 (capture the ratified resequencing — currently chat-only). Add a dated note to the parity plan §4 and matching roadmap rows: ratified order is F0 (this session) → F (Counter/ImagingStudy/SupplyList, 24 modules, with the `:race`/`:not` condition-type rider, 4 more) → G (wellness; ledger is 19 tagged modules plus the two max-steps loop walk-failures, `med-rec` and `veteran-substance-abuse-treatment`, which are substitution artifacts expected to resolve with G) → H → I (singleton tail: AllergyOnset, VitalSign, Vaccine, lookup-column `time`). Wave E is RE-SCOPED: `stroke` already censuses `:ok-walked`, so E is calibration content (the risk-attribute register), not an unlock wave — scheduled on demand, not in the leverage queue.
>
> Steps
>
> Step 0 — Preflight. Build-session preflight; confirm ADR-0034 at origin and next ADR is 0035; Synthea checkout at the pin available for Step 5's census re-run (AR-1 pin verification will refuse otherwise).
> Step 1 — Loader. New-kind pass-through normalization (AR-5), unknown-kind clean rejection (AR-2), mutually-exclusive-value rejection for SetAttribute (AR-4), schema extensions. Co-landing tests: each kind loads to the normalized shape; unknown kind rejects as a value with module/state/kind named; the census tool's defensive wrapper for loader exceptions can stay (defense in depth) but must no longer fire for this class. Commit: `feat(sim-trajectory): loader carries GAUSSIAN/EXPONENTIAL/TRIANGULAR distributions; unknown kinds reject cleanly (ADR-0035)`
> Step 2 — Interpreter sampling. Single-draw samplers per AR-1/AR-3 (probit approximation cited; clamp; round; unit conversion choice stated), wired into the timing path and Delay/Procedure contexts. Co-landing tests: one-draw consumption per sample (assert via draw counting on a seeded rng), clamp behavior at min/max, round flag, EXPONENTIAL's +1 shift (a fixed-seed value test), TRIANGULAR branch coverage. Commit: `feat(sim-trajectory): single-draw sampling for gaussian/exponential/triangular timing (ADR-0035 AR-3)`
> Step 3 — SetAttribute sampling. The `:set-attribute` case samples distributions per AR-4. Test: a GAUSSIAN SetAttribute sets a non-nil, clamped, rounded value; draw consumption counted. Commit: `fix(sim-trajectory): SetAttribute samples its distribution instead of silently setting nil (ADR-0035 AR-4)`
> Step 4 — Oracle bracket. `bin/regression-oracle` across `<tip-before-Step-1> -> <Step 3 tip>`. Pure identity per AR-6. Record the table. Any change: STOP-AND-ESCALATE.
> Step 5 — Census re-run. Per AR-7: new dated artifact + §15 delta note. Sanity anchors: vendored roots unmoved; the 11 loader-exception modules all moved somewhere; movement classified (resolved vs. next-blocker-surfaced vs. digest-changed-by-sampling). Commit: `docs(sim-trajectory): census re-run after F0 -- loader-exception class closed (ADR-0035)`
> Step 6 — Records. ADR-0035 (AR-1..AR-8 verbatim, attributed; execution note: oracle table, census delta classification). AR-8's plan/roadmap capture. Roadmap: F0 row → Done; F row (with rider) enters Next. Session record + prompt self-archive + budget check. Commit: `docs: wave F0 records -- distributions landed, resequencing captured (archives prompt)`
>
> Fences
>
> * No Counter/ImagingStudy/SupplyList work (Wave F), no condition-type work (F's rider), no wellness work (Wave G) — however small any looks from here.
> * `engine.clj` untouched; this wave is loader + interpreter.
> * AR-6's identity claim is absolute; AR-7's census movement is recorded, not judged — the ranking read on the new census is the design channel's move, not this session's.
> * Deviations: dated deviation-record appendix on the archived prompt.

## Deviation-record appendix (per the prompt's own Fences clause)

- **AR-1's own "58 upstream SetAttribute states carrying
  distributions" figure is the GAUSSIAN-kind count specifically, not
  the total.** A full-catalog scan (recursive, root modules + every
  submodule, `synthea-checkout` at the pin) found 113 SetAttribute
  states carrying a `:distribution` across all five kinds (GAUSSIAN 58
  — exactly matching the prompt's own number — UNIFORM 41, EXPONENTIAL
  9, EXACT 5). Read as scoping GAUSSIAN specifically (the kind the
  prompt's own example, `hypertension`'s onset age, uses), not as an
  undercount of the ruling's own reach: AR-4/AR-2's own text already
  requires the SetAttribute fix to handle "distributions" generically
  (not GAUSSIAN-only), and that is what was built — all five kinds
  normalize and sample through the same code path. Disclosed rather
  than silently reconciling the number.
- **A real, unanticipated bug found and fixed at Step 2, not merely a
  named future.** `emit-and-advance` is the shared helper every
  trajectory-event-producing state type calls (Encounter, Observation,
  ConditionOnset, Procedure, ...), not a Procedure-only function — a
  fact this session's own "Read first" list did not surface, since
  `resolve-time-advance`/`rand-int-in` (both named) sit one layer below
  it. The full non-integration suite caught it live:
  `uti/ed_bundle.json`'s own O2-saturation Observation states carry a
  `gmf_version 2` `:distribution` this loader has never normalized
  (Observation is not one of this session's three contexts) — an
  ungated `(:distribution state)` check in the new Procedure-duration
  branch handed that raw, string-keyed field straight to
  `sample-distribution`, crashing with `"No matching clause: UNIFORM"`.
  Fixed by gating the check on `(= :procedure (:type state))`; a
  regression test pins the exact shape. Not put to the author as a
  question — a straightforward correctness fix squarely inside Step
  2's own scope (Procedure duration), not a new context or a scope
  expansion, so proceeded and disclosed here rather than pausing the
  session for it.
- **The census tool's own artifact filename has no same-calendar-day
  disambiguation — found live at Step 5, the ORIGINAL artifact was
  briefly overwritten.** `ehrt.sim-trajectory.census/-main` names its
  output `<census-date>-synthea-<pin7>.edn`; re-running on the SAME
  calendar date as the original session collides on the identical path.
  The first re-run attempt silently overwrote
  `components/sim-trajectory/docs/census/2026-08-03-synthea-7e08387.edn`
  before this was caught via `git status` (a real diff against the
  tracked original) and restored with `git restore` before anything was
  staged — no data was actually lost. Worked around by re-running into
  a scratch directory and hand-appending a `-wave-f0` suffix to the
  copied-in filename, preserving AR-7's own "committed alongside the
  old, never overwrite" instruction without modifying the census tool
  itself (out of this session's own loader-plus-interpreter fence).
  Disclosed in `docs/gmf-interpreter.md`'s own new §15 subsection and
  `notes/ADRs.md` ADR-0035's own execution note; named, not fixed, for
  a future session that touches `ehrt.sim-trajectory.census`.
- **Wave E kept in the parity plan's own §4 table, annotated RE-SCOPED,
  rather than removed** — AR-8's own text reads as dropping E "out of
  the leverage queue," which could be executed either as deleting its
  row or as annotating it in place; this repo's own standing discipline
  ("kept, annotated, not deleted," `docs/gmf-interpreter.md` §8's own
  superseded-table precedent) governed the choice.
