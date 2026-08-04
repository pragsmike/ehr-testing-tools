# 2026-08-04 — GMF coverage Wave I2: the last two — parity frontier CLOSES

## Scope

Post-I census (`2026-08-04-synthea-7e08387-wave-i.edn`, ADR-0040 AR-7):
82/85 walk, 1 out-of-scope, 2 blocked — both unmasked findings from
Wave I's own six-mechanism landing, neither fixed there. The driving
prompt ratified two author rulings (`notes/ADRs.md` ADR-0041, AR-1/
AR-2) plus two mechanical checkpoints (oracle bracket, census + parity
determination): Death's own `:condition-onset`/`:referenced-by-
attribute` cause-of-death forms (`congestive-heart-failure`'s four
Death states, all using `:referenced-by-attribute`), and the
`:active-careplan` condition type (`wellness-encounters`' own closure
member, `encounter/depression_screening.json`). Both mechanical,
pinned, additive ports — the closing unlock the parity plan named:
if both classified clean, the parity plan §1's countable definition
would be met. Executed in one pass.

## Red→green evidence

- **Step 1+2 (AR-1 + AR-2, one commit, `14e8dce`).** Landed together
  rather than the two checkpoints the driving prompt named — the two
  mechanisms touch the same two source files (`gmf.clj`,
  `gmf-interpreter.clj`) in adjacent, non-overlapping regions; a clean
  split would cost more than it buys, the same disclosed shape
  ADR-0040 AR-5 already took for the identical reason. Red evidence:
  the two retired tests
  (`death-throws-on-unbuilt-condition-onset-cause-form`/
  `death-throws-on-unbuilt-referenced-by-attribute-cause-form`)
  asserted throws the pre-fix `death-step` code actually produced
  (confirmed by direct read before editing); the post-I census's own
  `:walk-errors` show the identical `:active-careplan` "unsupported
  condition type" throw for real, pre-fix. 13 new tests (6 Death-cause
  forms + priority ordering, 1 `:assign-to-attribute` wiring, 6 Active
  CarePlan forms), net +11 against the 2 retired. Full
  sim-trajectory-adjacent suite green: 299 tests, 802 assertions, 0
  failures/errors (`gmf-interpreter-test`, `gmf-test`,
  `death-fixture-test`, `compile-trajectory-test`,
  `vendored-ear-infections-test`).
- **Step 3 (oracle bracket).** `bin/regression-oracle dd6a9d4 14e8dce`
  — all 9 vendored root batches IDENTICAL, byte-verified. A fresh
  recursive scan for both mechanisms across every vendored root, run
  before any edit: zero hits for either — the implementation is purely
  additive by construction, confirmed not merely intended.
- **Step 4 (census, `9f9b5be`).** `:ok-walked` 82→84,
  `:out-of-scope-by-ruling` unchanged at 1, `:walk-failed` 2→0,
  `:load-failed` unchanged at 0. Both `congestive-heart-failure` and
  `wellness-encounters` now `:ok-walked` with `:walk-errors []`; no
  other module's own verdict or digest shifted, confirmed by direct
  comparison against the post-I artifact.
- Throughout: `gitleaks git --staged -v`/`gitleaks git -v` clean on
  every commit; each push verified against its own message file;
  `clojure -M:poly check` green at every checkpoint.

## Judgment calls and their ratification status

Two premise refinements surfaced while reading the pin fresh, both
handled per fix-forward-with-disclosure rather than silently adopting
the driving prompt's own account verbatim — neither escalated, since
both are factual corrections against the live source, not design
decisions requiring an author's own call (the ADR's own AR-1 entry
states each correction directly, so this is a pointer, not a repeat):

- **The prompt's own AR-1 framing (nil-tolerant absence on both
  Death cause forms, attributed loosely to "upstream's own
  `hadPriorState` guard") did not match the real source when re-read.**
  `State.java`'s own `Death.process`: `:codes` is checked FIRST, not
  last (docs/gmf-interpreter.md section 10's own C1 paraphrase has the
  order backwards); `:condition-onset`'s absence-tolerance is real but
  incomplete (upstream has a SECOND fallback, reading a named state's
  own declared codes without having walked it, not ported here);
  `:referenced-by-attribute`'s absence-tolerance is a genuine,
  disclosed DEPARTURE from upstream, which throws a `RuntimeException`
  on a missing attribute, not a `hadPriorState`-style guard at all.
  Implemented per the corrected reading (real priority order, the
  narrower ported semantics, the disclosed departure named honestly),
  not per the prompt's own inaccurate citation.
- **AR-1's own "referenced-by-attribute reads the attribute's condition
  entry" phrase presupposes something writes that attribute — nothing
  did.** `congestive_heart_failure.json`'s own `CHF Condition Start`
  (a `ConditionOnset` state) authors `assign_to_attribute: "chf"`, but
  this project's `:condition-onset` `step` case never wired
  `:assign-to-attribute` (only `:medication-order` had it, since Wave
  B). Without porting that mechanism too, `chf` would never be written
  and the referenced-by-attribute form would always see an absent
  attribute on `congestive-heart-failure`'s own real, only exercised
  path — not a hypothetical gap, the actual mandatory path this wave
  exists to unlock. Ported (mirrors `:medication-order`'s own case
  verbatim), found live, not anticipated in the prompt's own text.
- **AR-1+AR-2 landed as ONE commit rather than the two checkpoints the
  driving prompt named.** Disclosed at the commit itself: the two
  mechanisms touch the same two source files in adjacent,
  non-overlapping regions, and the same precedent (ADR-0040 AR-5)
  already established that a clean split costs more than it buys for
  genuinely small, related changes. A pragmatic ceremony deviation,
  not escalated.

## Findings and HEAD landed

- **PARITY ACHIEVED, at pin `7e08387c68a7f0e21d13076609a159fd473fc902`,
  2026-08-04.** 84/85 top-level modules `:ok-walked`, 1
  `:out-of-scope-by-ruling` (`gallstones`), ZERO `:load-failed`, ZERO
  `:walk-failed` — the parity plan §1/§3's own countable definition,
  MET, exactly as ruled. The dated declaration lands on the parity
  plan's own Status header and its §4 dated-note trail, the interpreter
  doc's §15 own new census-re-run subsection (and a correction to its
  §10/§13-area dated notes), the roadmap (this row retired to Done,
  Wave H named the sole remaining wave), and `notes/ADRs.md` ADR-0041.
- This session ran under R30 (the standing default, ADR-0007/ADR-0023)
  — every checkpoint committed and pushed by this session itself.
- Commits, in order: `14e8dce` (Step 1+2), `9f9b5be` (Step 4 — Step
  3's oracle bracket needed no code change, folded into this record's
  own evidence instead), and this commit (Step 5 — ADR-0041, the
  parity plan's own dated note and Status-header amendment, the
  interpreter doc's own census-re-run subsection and section-10/13
  corrections, roadmap capture, this record and its paired prompt
  archive, both indexed).
- **Fence, explicit:** this session did NOT touch Wave H mechanics, the
  census tool's own overwrite bug (worked around by hand-copy again),
  or anything beyond the two named mechanisms — exactly the driving
  prompt's own Fences section. Two premise refinements against the
  prompt's own account (above) were disclosed at the point found, per
  fix-forward-with-disclosure, not silently absorbed or escalated —
  both were factual corrections against the live pin, not design
  decisions requiring an author's own call.
