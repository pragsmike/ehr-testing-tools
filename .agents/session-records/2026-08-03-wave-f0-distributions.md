# 2026-08-03 — Wave F0: GAUSSIAN/EXPONENTIAL/TRIANGULAR distributions land

## Scope

`ehrt.sim-trajectory.gmf`'s own `gmf-v2-timing->v1` had clauses for
UNIFORM/EXACT only — a real GAUSSIAN or EXPONENTIAL `gmf_version 2`
distribution threw a raw `IllegalArgumentException` at load time (found
live, ADR-0034's own census, 11 modules combined, tied for the largest
single-mechanism unlock). Separately, `:set-attribute` read only
`:value`/`:value-code`, so any upstream SetAttribute state whose only
value source was a distribution (e.g. `hypertension.json`'s own GAUSSIAN
onset age) silently wrote `nil` — invisible to the census, since a
module still loads and walks clean with `nil` attributes feeding its
guards. This session ported all five of `Distribution.java`'s own kinds
(GAUSSIAN/EXPONENTIAL/TRIANGULAR newly, UNIFORM/EXACT's existing
v1-collapse untouched) across three contexts — Delay timing, Procedure
duration, SetAttribute value — per the design channel's own AR-1
through AR-8 rulings (`notes/ADRs.md` ADR-0035), oracle-bracketed the
change, re-ran the census, and closed out records. `engine.clj`,
Counter/ImagingStudy/SupplyList (Wave F), and wellness (Wave G) were
untouched — explicitly out of this session's own fence.

## Red→green evidence

- **Step 1 (loader, `ced1c06`).** New tests proven RED first by
  stashing the loader-only change: 5 failures/5 errors, including the
  exact `IllegalArgumentException` ("No matching clause: WEIBULL") the
  fix exists to close. GREEN after: `gmf-test` 43 tests / 130 assertions
  (36/105 before this session), 0 failures/0 errors.
- **Step 2 (interpreter timing, `c5cde06`).** Proven RED by stashing
  the interpreter-only change: a compile error (`probit-approx` did not
  exist). GREEN after: `gmf-interpreter-test` 107 tests / 268 assertions
  (97/193 before). `probit-approx` spot-checked against known standard-
  normal quantiles (Phi(1.959964) ≈ 0.975, symmetric around 0) to
  1e-5 or better against a 6-decimal reference. `sample-distribution`'s
  EXPONENTIAL/TRIANGULAR branches pinned against fixed-seed golden
  values computed independently via the REPL before being hardcoded
  into the tests.
- **Step 3 (SetAttribute, `c9de204`).** Proven RED by stashing the
  interpreter-only change: 3 failures/2 errors, including the exact
  silent-nil behavior (`(= 7.0 nil)` for an EXACT-kind SetAttribute)
  this fix closes. GREEN after: `gmf-interpreter-test` 111 tests / 274
  assertions.
- `clojure -M:poly check` clean and `clojure -M:poly test :all
  skip:integration` 0 failures/0 errors at every checkpoint (Steps
  1/2/3/5), including once AFTER a real bug fix mid-Step-2 (below).
  `gitleaks git --staged -v` clean, every commit (5/5).

## A real bug found and fixed mid-Step-2, not merely a named future

`emit-and-advance` is the SHARED helper every trajectory-event-
producing state type calls (Encounter, Observation, ConditionOnset,
Procedure, ...) — not a Procedure-only function, a fact this session's
own "Read first" list did not surface (`resolve-time-advance`/
`rand-int-in`, both named, sit one layer below it). The full non-
integration suite caught it live: `clojure -M:poly test :all
skip:integration` failed with `IllegalArgumentException: No matching
clause: UNIFORM` inside `sample-distribution`, traced through
`ehrt.sim-emit-hl7.vendored-uti-test`. Root cause, found by loading the
real UTI closure directly and scanning every state for a still-raw
`:distribution`: `uti/ed_bundle.json`'s own O2-saturation Observation
states carry a `gmf_version 2` `:distribution` this loader has NEVER
normalized (Observation is not one of this session's three contexts) —
the new Procedure-duration check in `emit-and-advance` was ungated by
state TYPE, so it handed that raw, string-keyed leftover straight to
`sample-distribution`. Fixed by gating on `(= :procedure (:type
state))`; a regression test
(`emit-and-advance-ignores-a-stray-raw-distribution-on-a-non-procedure-
state`) pins the exact `uti/ed_bundle.json` shape so this class of bug
cannot silently reappear. Not put to the author as a question — a
straightforward correctness fix squarely inside Step 2's own scope, not
a scope expansion — proceeded and disclosed in the ADR/roadmap instead.

## Oracle bracket (Step 4, no commit — evidence only)

`bin/regression-oracle d9545c9 c9de204` (tip before Step 1 → the Step 3
tip) — `digest.clj` now covers NINE root batches (the six original plus
the three engine-layer closures ADR-0033 AR-4b added since the last
time this bracket ran):

| root | changed? |
|---|---|
| `appendicitis` | no |
| `death-fixture` | no |
| `ear-infections` | no |
| `ear-infections-engine` | no |
| `sepsis` | no |
| `sinusitis` | no |
| `sore-throat` | no |
| `total-joint-replacement-engine` | no |
| `urinary-tract-infections-engine` | no |

`IDENTICAL: every root's digest matches between d9545c9 and c9de204` —
AR-6's pure-identity claim holds, byte-verified by the real
`bin/regression-oracle` script's own output, not a count comparison.

## Census re-run (Step 5, `c80c5c5`)

Same header parameters as the original (pin, 3 seeds/module, mixer-seed
`20260803`, registration age 30, 50-year horizon, `{}` persona config).
Verdicts: `:ok-walked` 40→42, `:load-failed` 39→34, `:walk-failed` 6→9,
total 85→85. All 11 originally-`gmf_version-2`-loader-exception modules
traced individually against both artifacts (a script comparing
`:verdict`/`:gap`/`:walks` per module id, not eyeballed):

- **Resolved (2):** `copd`, `opioid-addiction` → `:ok-walked`.
- **Surfaced their next blocker (3):** `contraceptives`/`dementia` (an
  unsupported condition type), `wellness-encounters` (an unrecognized
  vital-sign name) → `:walk-failed`.
- **Stayed `:load-failed`, on a genuinely different, EARLIER gap (6):**
  `bone-marrow-transplant`/`colorectal-cancer`/`pregnancy` (`Counter`),
  `dental-and-oral-examination`/`metabolic-syndrome-care`
  (`SupplyList`), `acute-myeloid-leukemia` (an unrecognized lookup-table
  column, `race` — a new finding). The loader's own deterministic
  first-found short-circuit never reached these six modules' now-fixed
  distribution content at all — the fix did not regress them.

**Zero digest movement** among the 40 modules `:ok-walked` in both
runs — AR-7 anticipated SetAttribute-distribution digest changes;
empirically none occurred. Traced to source: `hypertension.json` (this
session's own cited example, `Black_Onset_Age`) stays `:load-failed` on
`Counter` in BOTH runs, blocked before its own GAUSSIAN SetAttribute
state is ever reached. The SetAttribute fix is real and directly tested
(Step 3's own `set-attribute-gaussian-*` tests) — this census's
85-module top-level scope simply doesn't currently walk far enough into
any module that exercises it. All seven vendored roots stayed
`:ok-walked`, byte-identical (matching the oracle bracket); nothing
outside the 11 traced modules moved. No STOP-AND-ESCALATE.

**Found live, worked around, disclosed:** the census tool's own
artifact filename (`<census-date>-synthea-<pin7>.edn`) has no
same-calendar-day disambiguation — the first re-run attempt silently
overwrote the ORIGINAL 2026-08-03 artifact (caught via `git status`
before staging, restored with `git restore`, no data lost). Re-ran into
a scratch directory and hand-appended `-wave-f0` to the copied-in
filename rather than the tool's own naming scheme, preserving AR-7's
"never overwrite" instruction without touching `ehrt.sim-trajectory.
census` (out of this session's own loader-plus-interpreter fence). Full
account in `docs/gmf-interpreter.md`'s own new §15 subsection.

## Judgment calls and their ratification status

- **The `emit-and-advance` scoping bug was fixed unilaterally, not
  escalated** — judged a straightforward, in-scope correctness fix
  (Step 2's own Procedure-duration wiring), not a design question or a
  scope expansion into Observation's own v2 distribution encoding
  (explicitly left alone, out of scope). Disclosed in the ADR/roadmap/
  this record rather than silently folded in without comment.
- **AR-1's "58" SetAttribute-distribution figure read as the
  GAUSSIAN-kind count specifically** (a full-catalog scan confirmed the
  exact match), not as an undercount of AR-4's own reach — the fix
  built handles all five kinds generically regardless. Not escalated;
  a straightforward reading, disclosed in the prompt's own deviation
  appendix.
- **Wave E kept in the parity plan's own §4 table, annotated
  RE-SCOPED, rather than deleted** — this repo's own standing "kept,
  annotated, not deleted" discipline (already established for §8's own
  superseded prioritization table) governed a choice AR-8's own text
  left open (drop from the table vs. annotate in place).
- Neither judgment call was escalated to the author via
  `AskUserQuestion` — none met the bar this repo's own precedent sets
  for that (a STOP-AND-ESCALATE-worthy identity break, e.g. the prior
  session's `death-fixture` finding); all three are named here and in
  the ADR for the author to overturn if the reading is wrong.

## Findings and HEAD landed

- Two real, unplanned findings this session: the `emit-and-advance`
  scoping bug (above, fixed) and the census tool's same-day filename
  collision (above, worked around, disclosed). Neither was anticipated
  by the driving prompt.
- This session ran under R30 (the standing default, ADR-0007/ADR-0023)
  — every checkpoint committed and pushed by this session itself, each
  verified against its own message file (every diff's only delta is
  `git log --format=%B`'s own trailing-newline artifact).
- Commits, in order: `ced1c06` (Step 1, loader), `c5cde06` (Step 2,
  interpreter timing sampling + the `emit-and-advance` scoping fix),
  `c9de204` (Step 3, SetAttribute), `c80c5c5` (Step 5, census re-run +
  doc delta note), and this commit (Step 6 — ADR-0035, roadmap/parity-
  plan AR-8 capture, this record and its paired prompt archive, both
  indexed). Step 4 (the oracle bracket) made no commit of its own —
  evidence only, folded into ADR-0035's own execution note.
- **Fence, explicit:** this session did NOT touch `engine.clj`; did NOT
  do any Counter/ImagingStudy/SupplyList work (Wave F) or the
  `:race`/`:not` condition-type rider; did NOT do any wellness work
  (Wave G); did NOT modify `ehrt.sim-trajectory.census` (the filename
  gap is named, not fixed) — exactly the prompt's own Fences section.
