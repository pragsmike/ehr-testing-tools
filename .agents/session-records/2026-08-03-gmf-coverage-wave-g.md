# 2026-08-03 — GMF coverage Wave G: the wellness cycle (wait semantics, substitution retired)

## Scope

ADR-0031 AR-2 ruled the synthesized wellness cycle in scope for Wave G
and shaped its design; this session ratified the final design in chat
(design channel, 2026-08-03) and executed it in one pass per the
driving prompt's own AR-1 through AR-8 (`notes/ADRs.md` ADR-0037):
transcribed Synthea's own wellness-visit cadence table as pinned
content, built a pure zero-draw schedule function anchored at DOB,
replaced the Wave B create-now substitution with genuine wait
semantics at the loader and interpreter layers, oracle-bracketed the
change, retired the census tool's own `:wellness-timing` detector
(replacing it with an `:out-of-scope-by-ruling` classifier for
`Physiology`/`gallstones`), and re-ran the census. Deferred-by-ruling:
the chronic-meds cadence cap, a phase-offset knob, shared-visit
attachment, and all Wave H pre-roll mechanics — explicitly out of this
session's own fence.

## Red→green evidence

- **Step 1 (`d209267`, cadence table + schedule function).**
  `gmf-interpreter-test` gained 8 new tests (band/spot checks, an
  independently-re-derived full-sequence walk over 60 ticks, a tier-
  boundary check, an adult-band-boundary check) — all green on first
  run except one caught immediately: `next-wellness-tick-adult-band-
  boundaries` assumed age 39 itself was a real chain tick, which it
  isn't (38's own successor already overshoots it to 41) — fixed in
  the test, not the code, before committing.
- **Step 2 (`23974ba`, loader).** `gmf-test`'s existing wellness-idiom
  test updated to assert `:wellness-wait`, not a synthesized
  `:encounter-class`; full `sim-trajectory`/`sim-emit-hl7` brick suites
  run green with Steps 2+3 both staged (see Judgment calls, below).
- **Step 3 (`cbf5330`, interpreter + tests).** 396 assertions in
  `gmf-interpreter-test` (up from 380), 0 failures/0 errors, including
  six new `:wellness-wait` tests and a new `ear_infections` timing
  assertion. `sim-emit-hl7`'s own engine round-trip test re-confirmed
  green with a dated docstring note.
- **Live fix (`203ed9f`).** Re-ran the full `sim-trajectory`/
  `sim-emit-hl7` suites after the `next-wellness-tick` boundary
  refinement (below): 396/396 assertions, 0 failures/0 errors, both
  bricks. `clojure -M:poly check` OK throughout every checkpoint.
  `gitleaks git --staged -v` clean, every commit.
- **Step 5 (`8fc4b03`, census).** `development`'s own `census-test`
  namespace (not poly-tested, ADR-0034's disclosed gap) run twice via
  direct `clojure -M:dev:test` invocation: 27 assertions, 0 failures,
  both times.

## A real, live finding: `next-wellness-tick`'s own boundary semantics needed refining

Found running this session's own AR-8 census against the real, pinned
Synthea checkout (a shallow clone — the repo's current HEAD happened to
already equal the pin exactly, `7e08387c68a7f0e21d13076609a159fd473fc902`):
`med-rec`/`mend-program`/`metabolic-syndrome-care`/`veteran-substance-
abuse-treatment` were STILL `:walk-failed` at `max-steps`, contradicting
AR-8's own expectation that wait semantics alone would resolve them.
Traced directly (a standalone `interp/step` trace of `med_rec.json`,
40 iterations printed): its own wellness-wait loop has ZERO delay
anywhere in the loop body (`Wellness_Encounter` → ... →
`EncounterEnd` → `Initial` → `ConditionOnset` → `Wellness_Encounter`
again) — `:t` never moved from DOB across any iteration. Root cause:
`next-wellness-tick`'s originally-ruled inclusive `>=` semantics ("first
tick ≥ t") returns `t` unchanged on the very first call when `t` = DOB
(the recurrence's own tick0), and every re-entry at that same unchanged
`t` returns the same tick again — an infinite zero-advance spin. Fixed
same session, BEFORE re-running the oracle bracket or the census for
real: `next-wellness-tick` now returns the first tick STRICTLY AFTER
`t` (`>`, not `>=`), matching upstream's own real mechanism
(`timeSinceLastWellnessEncounter` resets to zero the instant an
encounter fires, so the next check always needs a genuinely new
interval). Re-verified directly before moving on: `med_rec.json` now
completes at `:horizon-complete` with 269 real events. The test suite
was updated to match (the tick0-anchoring test rewritten to assert
strict inequality; the wait-act-loop fixture's own artificial 1-day
nudge — added out of an earlier, correct worry about this exact
edge case — removed, since it's no longer needed and the fixture now
matches `med_rec.json`'s own real zero-delay shape directly, making it
the actual regression test for this fix). Recorded as a dated deviation
on ADR-0037's own AR-2, not a silent code change.

## Oracle bracket (`58fdd9c` → `203ed9f`, post-fix, evidence only)

`bin/regression-oracle 58fdd9c 203ed9f`, all 9 root batches:

| root | changed? |
|---|---|
| `appendicitis` | no |
| `death-fixture` | no |
| `ear-infections` | **yes, by design** |
| `ear-infections-engine` | **yes, by design** |
| `sepsis` | no |
| `sinusitis` | no |
| `sore-throat` | no |
| `total-joint-replacement-engine` | no |
| `urinary-tract-infections-engine` | no |

Exactly AR-6's own predicted bracket. Digests, old → new:
`ear-infections`
`6dcd3d2...` → `6ad02f8...`; `ear-infections-engine` `2294f78...` →
`5a63147...`. Ran the bracket TWICE — once right after Step 3 (before
the `next-wellness-tick` fix), once again after the fix — and confirmed
the ear-infections digests were IDENTICAL between both runs: the fix's
own boundary case never fires on ear_infections' real seeded walks, so
the disclosed re-baseline above is the fix-independent, Wave-G-design
consequence AR-6 anticipated, not an artifact of the live finding.

## Census re-run (`8fc4b03`)

Same header parameters as Wave F (pin, 3 seeds/module, mixer-seed
`20260803`, registration age 30, 50-year horizon, same persona-config
delta — no new header parameter this wave). Verdicts: `:ok-walked`
60→64, `:load-failed` 18→17, `:walk-failed` 7→3, `:out-of-scope-by-
ruling` 0→1, total 85→85. Full movement classified by direct EDN diff
against both artifacts (not eyeballed):

- **The four real upstream loop modules, ALL resolved fully:**
  `med-rec`, `mend-program`, `metabolic-syndrome-care`,
  `veteran-substance-abuse-treatment` → `:ok-walked`.
- **`gallstones` reclassifies** `:load-failed` → `:out-of-scope-by-
  ruling` (its sole gap, `Physiology`, the census's first ruled
  exclusion).
- **7 of the 19 formerly-`:wellness-timing`-tagged modules stay
  `:ok-walked` but change walk digest** (the wait now times the
  encounter differently): `asthma`, `bronchitis`, `dementia`,
  `ear-infections`, `osteoporosis`, `sleep-apnea`,
  `veteran-hyperlipidemia`.
- **8 show no observable difference** (still `:ok-walked`, byte-
  identical): `atrial-fibrillation`, `copd`, `epilepsy`,
  `hypertension`, `mtbi`, `stable-ischemic-heart-disease`,
  `veteran-prostate-cancer`, `wellness-encounters`.
- **Two modules stay `:walk-failed` for reasons wholly unrelated to
  this Wave**, byte-identical error to the Wave F census's own record
  for both: `anemia-unknown-etiology` (an observation-linkage gap),
  `wellness-encounters` (an unrecognized vital-sign name, `"Height"` —
  its own name is coincidental).
- **No module outside these 19 changed verdict or digest at all** —
  confirmed by a full per-module diff over all 85 entries, not a
  verdict-count comparison alone.

**Sanity anchors held.** All seven vendored roots stayed `:ok-walked`,
byte-identical across every census artifact to date, matching the
oracle bracket.

## Judgment calls and their ratification status

- **Steps 2 and 3 were committed separately (matching the driving
  prompt's own checkpoint breakdown) but verified together before
  either was pushed**, and pushed in the same `git push` call — the
  loader change alone (Step 2, in isolation) would leave any real walk
  reaching a `wellness: true` state throwing (no interpreter case yet
  exists for the new `:wellness-wait` type), a genuine regression if it
  ever reached `origin/main` on its own. Rather than accept that
  window, both steps' code was written and the FULL test suite run
  green once, THEN split into two commits along the file boundaries the
  prompt names, both pushed together immediately after. Disclosed here
  as a deliberate reading of "commit and push at each checkpoint" (R30)
  — checkpoint granularity in the commit history, not a guarantee that
  every individual commit's own tree independently passes the full
  suite if checked out alone. Not escalated; the alternative (merging
  Steps 2+3 into one commit) seemed a worse fit for the prompt's own
  explicit two-checkpoint structure.
- **The `next-wellness-tick` boundary refinement (above) was treated as
  a live "found while building, not merely anticipated" correction,
  fixed and disclosed rather than escalated as a STOP-AND-ESCALATE** —
  judged in-scope because it is a refinement of this session's OWN
  ruling (AR-2), discovered and resolved entirely within this session,
  before the census AR-8 depends on ever ran for real. Recorded as a
  dated deviation in ADR-0037 itself, not silently smoothed over.
- **`out-of-scope-by-ruling?` (census.clj) was made public and unit-
  tested directly against hand-built gap maps**, rather than only
  through `census-one`'s own integration path — `load-module`'s own
  short-circuiting `cond` (first bad state wins) makes a genuinely
  mixed real-world gap hard to construct honestly through the loader
  alone, so the "stays `:load-failed` on a mixed gap" property is
  proven directly against the predicate instead. Not escalated — the
  same public-for-testability precedent `wellness-substitution?`
  already set.

## Findings and HEAD landed

- The `next-wellness-tick` boundary finding (above) is the session's
  one real, unplanned finding — found live, fixed live, disclosed in
  both the ADR and here.
- This session ran under R30 (the standing default, ADR-0007/ADR-0023)
  — every checkpoint committed and pushed by this session itself, each
  verified against its own message file (every diff's only delta is
  `git log --format=%B`'s own trailing-newline artifact).
- Commits, in order: `d209267` (Step 1, cadence table + schedule
  function), `23974ba` (Step 2, loader), `cbf5330` (Step 3, interpreter
  + tests), `203ed9f` (live fix, `next-wellness-tick` strict `>`),
  `8fc4b03` (Step 5, census re-run), and this commit (Step 6 — ADR-0037,
  roadmap/parity-plan capture, this record and its paired prompt
  archive, both indexed).
- **Fence, explicit:** this session did NOT implement the chronic-meds
  cadence cap, a phase-offset knob, shared-visit attachment, or any
  Wave H pre-roll mechanics — exactly the driving prompt's own Fences
  section. The `:wellness-wait`-during-history-phase fold rule is named
  on Wave H's own row in the parity plan, not built.
