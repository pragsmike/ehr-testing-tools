# 2026-08-04 — GMF coverage Wave I: the singleton tail

## Scope

Post-VS census (`2026-08-04-synthea-7e08387-wave-vs.edn`, ADR-0039):
75/85 walk, 1 out-of-scope, 9 blocked across six small, fully-pinned
mechanisms. Named the CLOSING unlock wave: if the classification went
as ruled, the parity plan §1's countable definition would be met
(84/85 + `gallstones` out-of-scope) and only Wave H (architectural,
ruled last) would remain. The driving prompt ratified seven author
rulings (`notes/ADRs.md` ADR-0040, AR-1 through AR-7): NamedDistribution
in `complex_transition`, SetAttribute's own upstream precedence chain
(the F0 conflict rejection retired), Observation-condition absence
returning false, the vital-sign vocabulary completed, `AllergyOnset`/
`Vaccine` land, an oracle bracket, and a census re-run with a parity
determination. All semantics pinned at
`7e08387c68a7f0e21d13076609a159fd473fc902`.

Executed in one pass, red→green per step, ending in the ADR, this
record, and the paired prompt archive. Two premise mismatches surfaced
during the session — both handled per the fix-forward-with-disclosure
discipline, one escalated to the user before any code was written, one
found and fixed within the same wave.

## Red→green evidence

- **Step 1 (`d779cd6`, NamedDistribution + AR-1b encounter-class).**
  `gmf-test` + `gmf-interpreter-test` combined: 232/232, 637 assertions,
  0 failures (up from 58+178 pre-session) — 4 new tests for
  `complex_transition` NamedDistribution (loader + interpreter,
  attribute-present/default-fallback pair) and 3 new encounter-class
  tests (`hospice`, `home`/`urgentcare`/`snf`). Proven red first via
  `git stash` on the source files alone: 10 failures/10 errors without
  the fix.
- **Step 2 (`93de2c0`, SetAttribute precedence).** Combined: 241/241,
  670 assertions, 0 failures — 9 new tests (distribution-outranks-value,
  range draw bounds + one-draw consumption, range decimals rounding,
  range-outranks-distribution, value-attribute present/absent-falls-
  through, value-code-outranks-value-attribute). One test's own
  floating-point assertion was flaky (multiplying a HALF_UP-rounded
  value by 100 and comparing exact `==` against `Math/round` hit binary
  float representation error) — fixed to an epsilon comparison before
  landing, not silently left flaky. Red confirmed: 13 failures/2 errors
  without the fix.
- **Step 3 (`f99dff9`, Observation-condition absence).** Combined:
  241/241, 670 assertions, 0 failures (same count — one pre-existing
  throw-expecting test converted to false-expecting, not a net-new
  test). Red confirmed: 1 error without the fix.
- **Step 4 (`24f0184`, vital-sign vocabulary, first pass).** Combined:
  242/242, 675 assertions, 0 failures — 1 new test (5 names via
  `doseq`). Red confirmed: 1 error without the fix.
- **Step 4 follow-up (`d7f5003`, found live during Step 7, fixed same
  session).** Combined: 250/250, 704 assertions, 0 failures — the same
  test's own name list extended to 11. Red confirmed: 1 error without
  the fix. Oracle re-verified IDENTICAL after this fix too.
- **Step 5 (`959b0bc`, AllergyOnset + Vaccine).** Combined: 250/250,
  698 assertions, 0 failures (interim, before the Step 4 follow-up) —
  15 new tests across both namespaces. First attempt hit a real bug
  (found by this step's own red test, not anticipated): the loader's
  pre-existing, ungated `(:series state) (update :series #(mapv
  normalize-imaging-series %))` clause (ImagingStudy's own field)
  crashed trying to `mapv` over Vaccine's bare `:series` int — same key
  name, two different fields, no `kw-type` guard. Fixed. Also broke two
  pre-existing tests that used `AllergyOnset` as their own "still
  genuinely deferred" fixture placeholder (the same pattern already
  swapped five times before across prior waves); moved to `Physiology`.
  Red confirmed: 9 failures/1 error without the fix. Full 325-test
  sim-trajectory suite (all 12 test namespaces, not just the two
  touched) re-run green after the fix, given the shared-key-collision
  fix's own blast radius.
- **Step 6 (oracle bracket, evidence only).** `bin/regression-oracle
  3d85fa0 HEAD`, run twice (after `959b0bc` and again after `d7f5003`)
  — all 9 vendored root batches IDENTICAL both times, byte-verified. A
  fresh recursive scan for all six mechanisms across every vendored
  root, run BEFORE any edit: zero hits for five of six; the sixth
  (Observation conditions in `sore_throat.json`/`sepsis.json`) was
  provably unaffected by construction (the pre-existing oracle digests
  could only exist if no sampled walk had ever hit the throwing path).
- **Step 7 (`8ab71e7`, census).** `:ok-walked` 75→82 (not 84 — see
  Findings, below), `:out-of-scope-by-ruling` unchanged, `:walk-failed`
  9→2.
- Throughout: `gitleaks git --staged -v` clean on every commit; each
  push verified against its own message file; `clojure -M:poly check`
  green at every checkpoint.

## Judgment calls and their ratification status

- **AR-1's "4 modules" claim was checked against the live tree BEFORE
  any code was written, found wrong, and escalated to the user via
  AskUserQuestion rather than silently guessed at.** The census's own
  malli error payloads, cross-checked against the vendored module JSON
  directly, showed only `injuries.json` actually used NamedDistribution
  — the other three modules were blocked by unrecognized encounter-class
  values, unrelated to NamedDistribution and not named in any ruling.
  The user chose to fold the encounter-class fix into this session as
  AR-1b (a dated addendum, provenance-cited) rather than leaving 3 of
  the 4 named modules unfixed. Escalated, not improvised.
- **The AR-4 vocabulary gap found during Step 7 (Creatinine and five
  others, all `category: "laboratory"`) was fixed in-session rather
  than merely disclosed** — judged as a scope-completion bug in this
  session's OWN Step 4 work (the enumeration method AR-4 named,
  "Observation states with category vital-signs," was narrower than
  the mechanism it backs, which never gates on category), not a new
  design question requiring its own ruling. The two genuinely NEW
  findings from the same census re-run (Death's cause-of-death forms;
  `:active-careplan`) were NOT fixed — real, unrelated design/scope
  gaps, disclosed per AR-7's own instruction instead. The distinction
  drawn: "same mechanism, wider net" gets fixed in-session; "different
  mechanism entirely" gets disclosed, not improvised.
- **AllergyOnset/Vaccine landed as ONE commit rather than the two the
  session prompt named** — disclosed in the Step 5 commit message
  itself: the two states share the same `gmf-type->keyword` edit and
  `step` dispatch region, and a clean split (via `git add -p` or
  similar) would cost more than it buys for two genuinely small,
  related changes. Not escalated; a pragmatic ceremony deviation,
  disclosed at the point made.

## Findings and HEAD landed

- **AR-7's own "parity achieved" contingency did not hold, recorded
  honestly rather than silently adopted.** The census landed at
  82/85 walking + 1 out-of-scope + 2 walk-failed, not the 84/85 +
  zero-walk-failed the parity declaration required — no PARITY
  ACHIEVED note was written anywhere (not in the parity plan, not in
  the roadmap, not in this record). Two modules unmask NEW,
  UNRELATED, UNFIXED gaps once their own original blockers clear:
  `congestive-heart-failure` on Death's own unbuilt cause-of-death
  forms (a named Wave C limitation, ADR-0028, that finally has a real
  consumer), `wellness-encounters` on an unbuilt condition type,
  `:active-careplan`. Both disclosed in ADR-0040 AR-7, the parity plan's
  own dated note, and roadmap Next — not fixed here, real design/scope
  work for whichever session picks this up.
- This session ran under R30 (the standing default, ADR-0007/ADR-0023)
  — every checkpoint committed and pushed by this session itself.
- Commits, in order: `d779cd6` (Step 1), `93de2c0` (Step 2), `f99dff9`
  (Step 3), `24f0184` (Step 4), `d7f5003` (Step 4 follow-up), `959b0bc`
  (Step 5), `8ab71e7` (Step 7 — Step 6's oracle bracket needed no code
  change, folded into this record's own evidence instead), and this
  commit (Step 8 — ADR-0040, roadmap capture, parity plan dated note,
  this record and its paired prompt archive, both indexed).
- **Fence, explicit:** this session did NOT touch Wave H mechanics, Wave
  E content, the census tool's own overwrite bug (worked around by
  hand-copy again), or the lookup-column `time` gap — exactly the
  driving prompt's own Fences section. Deviation from the prompt's own
  framing recorded honestly in two places (AR-1's scope, AR-7's parity
  contingency) rather than silently adopted.
