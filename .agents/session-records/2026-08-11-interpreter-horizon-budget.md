# 2026-08-11 — Interpreter horizon/budget fix: submodule walks respect the horizon, the runaway budget counts only zero-advance steps (ADR-0105)

## Scope

B1 of the author-ruled two-session injuries arc (2026-08-11, author
verbatim "yes"): the `gmf-interpreter` fix ADR-0070's own revisit
trigger named — "a future session willing to extend gmf-interpreter's
own runaway-loop handling" — prerequisite to B2 (the injuries
vendoring batch itself, NOT this session). Two commits: the fix
(`b0b030d`, interpreter + tests + design-doc addendum), and this
close-phase record/prompt-archive commit. This is ADR-0105.

## Red→green evidence highlights

Two coupled defects, both verified against the live tree before
relying on either. **Half 1:** `run-submodule` (the `CallSubmodule`
descend-run-return loop) never received `horizon-end-t` at all —
horizon-BLIND, so a time-advancing Delay loop inside a submodule
(`injuries/broken_jaw.json`'s own Dental Referral cycle, ADR-0070's
own bail-out finding) iterated past the horizon forever, tripping
`max-steps` at ANY horizon. **Half 2:** `max-steps` counted EVERY
step regardless of advance, so even a horizon-bounded LEGAL loop could
trip the 10000-step budget on volume alone — verified against the
census's own real 50-year horizon and `broken_jaw.json`'s own real
Delay bounds (byte-read from the pinned synthea checkout): mean cycles
to cross the horizon sit within ~9% of the ceiling at the exact mean,
so ordinary seed variance pushes real seeds over. Independent
corroboration already on record: `veteran_mdd.json`'s own NOTICE
finding (ADR-0090) trips the SAME backstop at the ROOT-level loop
(already horizon-aware), proving half 2 is a real, independent defect.

Two hermetic, test-local fixtures reproduced both halves red (`ERROR
in`, an uncaught exception escaping the intended `:status
:horizon-complete` assertion), both pasted verbatim in ADR-0105. A
real-content scratch probe (never committed) against the pinned
`injuries.json` closure dropped from 14/120 to 0/120 `run-submodule
exceeded max-steps` failures post-fix; the 2 remaining failures are a
SEPARATE, pre-existing `nested :encounter` gap — confirmed, by a
`git stash`/`git stash pop` seed-by-seed diff, to be the EXACT SAME
two seeds failing on the EXACT SAME assert before this fix too, not
newly exposed. Fix: `horizon-end-t` threads from `run-module` through
`step`/`call-submodule-step` into `run-submodule`, which now re-checks
`:t` before every step exactly as `run-module`'s own loop already
does, parking on `:status :horizon-complete` — the SAME status a
top-level Delay overshoot produces, asserted by equality against a
real top-level truncation, not a literal. A new shared helper,
`consume-step-budget`, makes all three `max-steps`-policed loops
(`walk-module`/`run-submodule`/`run-module`) count only zero-advance
steps. Three new non-regression tests confirm the backstop stays
non-vacuous (a genuine zero-advance spin still throws, at both call
sites the fix touches) and the mirror-site status-equality contract
holds. 203 tests / 527 assertions post-fix (200/523 pre-fix), every
pre-existing test unmodified.

**Oracle bracket.** Pre-analysis (required before running it, per the
driving prompt): enumerated all 33 submodule call-paths any vendored
root can reach, found four containing Delay states, read every one —
none loop, every bound is hours-to-6-weeks against 50-100-year
horizons. Expectation: pure identity. `bin/regression-oracle af2369c
b0b030d`: all 34 roots IDENTICAL, matching the prediction exactly — no
STOP-AND-REPORT triggered.

**Full gate:** `clojure -M:poly check` OK (twice); full local suite
(`clojure -M:poly test`, unredirected capture) 672 occurrences of "0
failures, 0 errors," ZERO `FAIL`/`ERROR` report lines, 6m11s;
`ehrt.cli.cli-parse-guard-lint-test` confirmed green within that run
(4 tests, 22 assertions); `bin/verify-nist-lock` OK, 6/6. Last five
`main` CI runs (checked at session start): all green.

## Judgment calls and their ratification status

- **The real-content scratch-probe's own failure rate (14/120)
  diverges from ADR-0070's own cited "4/120."** Disclosed honestly in
  ADR-0105 rather than forced to match or silently omitted — the
  qualitative finding (same exception, same states, near-certain at
  population scale) agrees exactly; this session makes no claim
  resting on the exact rate.
- **The zero-advance counting semantic** ("does not consume the
  budget" vs. "resets it to zero on any advance") — both explicitly
  licensed by the driving prompt's own Context. Chose "does not
  consume": matches `max-steps`'s own docstring most literally (a
  population count, not a consecutive-run count) and lands identically
  at all three call sites via one shared helper. Recorded as a design
  decision in ADR-0105, not a deviation.

## Findings and HEAD landed

**Two coupled, real defects found and fixed**, both named in ADR-0070's
own revisit trigger and closed here: `run-submodule`'s own horizon
blindness, and `max-steps`'s own every-step (not zero-advance-only)
counting. **One separate, pre-existing defect found and explicitly
NOT fixed** (out of this session's own B1 scope, disclosed for a
future B2 session): a `nested :encounter` assert failure on 2 of 120
real `injuries.json` walks, confirmed unaffected by this session's own
fix (same seeds, same failure, both before and after).

**Oracle bracket held pure identity**, matching the pre-analysis
exactly — no currently vendored closure's own submodule Delay states
were exposed to horizon-crossing, so no digest moved.

**Tag paid forward:** `stable-20260810-ed-tuesday-scenario` tagged at
`af2369c` (Step 1, this session — the design channel's own verified
ADR-0104 landing, tag law case (i)), peeled ref verified exact match,
remote unmoved.

**Roadmap corrected:** the ED row's own "B" text, which mis-
characterized the injuries batch as "routine vendoring intake" while
citing ADR-0070's own WHOLE deferral, is amended — B = B1 (this ADR,
landed) + B2 (the batch, now unblocked, still not scheduled).

**HEAD landed:** the close-phase commit (this record's own commit),
pushed.
