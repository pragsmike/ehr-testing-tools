## ADR-0105 — Interpreter horizon/budget fix: submodule walks respect the horizon, the runaway budget counts only zero-advance steps

**Status:** Accepted (author-directed, autonomous session per R30), 2026-08-11.

### Context

Prior: vendoring batch 1 (`notes/adr/0070-vendoring-batch-1.md`,
2026-08-07) assessed `injuries.json`'s own eight-file closure and
DEFERRED IT WHOLE — `engine/run` (the direct interpreter-layer probe,
`run-module` called at the census's own exact parameters: registration
age 30, three horizons tried — 36500/18250/3650 days) threw `run-
submodule exceeded max-steps` at `:check-for-dental-visit`, on 4 of
120 well-mixed-seed walks in ADR-0070's own sample, at EVERY horizon
tried. ADR-0070's own root-cause account: `injuries/broken_jaw.json`'s
own `Dental Referral` state sets the `dental_referral` attribute once
(no state anywhere in the closure ever clears it); `Check for Dental
Visit` loops with `Wait for Dental Visit` (a 1-7-day `Delay`) for as
long as the attribute stays set — permanently, once reached. ADR-0070's
own revisit trigger names exactly this session's own charter: "a
future session willing to extend `gmf-interpreter`'s own runaway-loop
handling."

This session (B1 of the author-ruled injuries arc, 2026-08-11, the
author's own verbatim "yes" to a two-session plan — `.agents/
rulings.md`, "From ADR-0105") is that extension. B2, the injuries
vendoring batch itself, is NOT this session's own scope — B1 only.

**THE DEFECT, two coupled halves — both verified against the live tree
before this session relied on either:**

1. **`run-submodule` is HORIZON-BLIND.** `ehrt.sim-trajectory.gmf-
   interpreter/run-submodule` (the `CallSubmodule` descend-run-return
   loop, gmf_interpreter.clj ~1467, pre-fix) never received `horizon-
   end-t` at all — `run-module`'s own loop "re-checks `:t` against
   `horizon-end-t` before every `step` call" (that function's own
   docstring, ~2057 pre-fix — the mirror site this fix keeps), but
   `run-submodule` had no equivalent check anywhere. A time-advancing
   Delay loop inside a submodule therefore iterated past the horizon
   forever, tripping `max-steps` at ANY horizon — exactly ADR-0070's
   own observed horizon-invariance (36500/18250/3650 all threw
   identically, because the horizon was never consulted at all — not
   because the loop genuinely needed that many iterations to cross any
   of the three).
2. **`max-steps` (10000) counted EVERY step, but its own docstring
   defined its target as "a zero-time-advance transition cycle."** A
   LEGAL upstream time-advancing loop can trip it within horizon at
   long horizons with no bug present. Verified this session against
   the census's own real 50-year default horizon
   (`ehrt.sim-trajectory.census/default-horizon-years`, 50 — `end-t =
   reg-t + 365*50` = 18250 days) and `broken_jaw.json`'s own real
   `Wait for Dental Visit` bounds (UNIFORM 1-7 days, byte-read from
   the pinned checkout, `/home/mg/synthea-checkout`, commit
   `7e08387c68a7f0e21d13076609a159fd473fc902`, matching `docs/gmf-
   interpreter.md`'s own pin): mean delay 4 days, so mean cycles to
   cross 18250 days ≈ 4562, at 2 steps/cycle (the Delay plus the
   zero-advance re-check) ≈ 9124 steps — already within ~9% of the
   10000-step ceiling at the EXACT MEAN, so ordinary per-seed variance
   (individual UNIFORM(1,7) draws running low) pushes a real fraction
   of seeds over budget with no bug present. This is load-bearing, not
   optional: fixing half 1 alone (horizon-awareness) would still leave
   real within-horizon trips at the census's own actual parameters.

**Independent corroboration, already on record, not sought out for
this session but confirmed relevant on read:** `components/sim/
resources/sim/modules/NOTICE`'s own vendoring-batch-4 dated section
(2026-08-08, ADR-0090) separately found `veteran_mdd.json` throwing
`run-module exceeded max-steps` (the ROOT-level loop, not a
submodule) at `:therapy-delay`/`:end-therapy-visit`, "reproduced at
every horizon tried... the module's own recurring therapy_delay/
Therapy_Visit/Therapy_Note/end therapy visit/MDD_Re_evaluation
Encounter cycle never exits before a multi-decade horizon exhausts the
interpreter's 10000-step runaway-loop backstop (a legitimate
long-running follow-up schedule the backstop cannot distinguish from a
true zero-advance spin)." This is defect half 2 tripping at the
ROOT-level loop (which is already horizon-aware) — direct evidence
that half 2 is a real, independent defect, not an artifact of half 1's
own horizon-blindness. `veteran_mdd.json` stays NOT vendored regardless
(deferred separately, out of this session's own B1 scope — no module
content, NOTICE row, or oracle root touched here).

### Decision

Author rulings, recorded verbatim (this session's own driving prompt,
2026-08-11). `[A]` author-ruled, `[C]` channel-inferred.

**[A] The two-session plan, author verbatim "yes"** (to: B1 — this
session, the interpreter fix — then B2, the injuries vendoring batch).
This session is B1 ONLY: no vendoring, no NOTICE rows, no oracle-root
additions, no module content anywhere.

**[C] The zero-advance counting half is load-bearing, not optional**
— per the arithmetic above, horizon-awareness alone leaves within-
horizon trips at the census's own 50-year parameters. The
verification confirmed the arithmetic; both halves land in the one
commit per the driving prompt's own instruction.

**[C] Roadmap correction rider** — the ED row's own prior "B" text
called the injuries batch "routine vendoring intake" while citing
ADR-0070, whose own record deferred injuries WHOLE pending exactly
this session. `.agents/plans/roadmap.md`'s Next row is amended: B = B1
(this ADR, landed) + B2 (the batch, now unblocked, still not
scheduled), with this ADR as the correction's own record.

### Tag ceremony

Design channel verified the ADR-0104 landing (`af2369c`) by fresh
public clone. `stable-20260810-ed-tuesday-scenario` tagged annotated
at `af2369c`, message "ed-tuesday scenario landed, design-channel-
verified 2026-08-11 (ADR-0104)"; pushed; peeled ref confirmed
`af2369c42ed6accf05796a9a11433f022b389792` — exact match; remote had
not moved (`origin/main` was already at `af2369c` at session start).

### Red, reproduced hermetically

Two test-local fixtures (hand-built Clojure module maps inside
`ehrt.sim-trajectory.gmf-interpreter-test`, never under `resources/
sim/modules`, never NOTICE'd), one per defect half, both asserting the
DESIRED post-fix behavior (`:status :horizon-complete`, not a thrown
exception) — failing red pre-fix with the bug's own exception
escaping as an uncaught error, per the build-session skill's own
red→green discipline.

**Half 1** (`dental-referral-caller-module`/`dental-referral-callee-
module`): mirrors `broken_jaw.json`'s own Dental Referral shape
exactly — `SetAttribute` once, a `Delay(1 day, exact)`↔conditional-
check cycle gated on the attribute staying set, inside a
`CallSubmodule`. A SMALL horizon (30 days) does not stop the loop
pre-fix. Red, verbatim:

```
ERROR in (run-submodule-respects-a-small-horizon-instead-of-running-to-max-steps) (gmf_interpreter.clj:1461)
Uncaught exception, not in assertion.
expected: nil
  actual: clojure.lang.ExceptionInfo: ehrt.sim-trajectory.gmf-interpreter: run-submodule exceeded max-steps -- likely a module authoring bug (a zero-time-advance transition cycle)
{:call-stack ["dental-caller" "dental-callee"], :current :wait}
 at ehrt.sim_trajectory.gmf_interpreter$run_submodule.invokeStatic (gmf_interpreter.clj:1461)
    ehrt.sim_trajectory.gmf_interpreter$call_submodule_step.invokeStatic (gmf_interpreter.clj:1498)
    ehrt.sim_trajectory.gmf_interpreter$step.invokeStatic (gmf_interpreter.clj:1827)
    ehrt.sim_trajectory.gmf_interpreter$step_safely.invokeStatic (gmf_interpreter.clj:1944)
    ehrt.sim_trajectory.gmf_interpreter$run_module.invokeStatic (gmf_interpreter.clj:2120)
    [... run-module arity chain, gmf_interpreter.clj:2043 ...]
```

**Half 2** (`perpetual-recheck-module`): a LEGAL, non-buggy, 1-day-
Delay/zero-advance-re-check loop that never terminates on its own —
only the horizon stops it, exactly the class `max-steps`'s own
docstring said it must not flag. A 6000-day horizon needs ~12000 total
steps to cross (2 steps/cycle), over the OLD every-step budget at
n=10000 (~5000 days elapsed, well short of the horizon) even though
the horizon check itself was already working correctly. Red, verbatim:

```
ERROR in (max-steps-counts-only-zero-advance-steps-a-legal-loop-reaches-horizon-complete) (gmf_interpreter.clj:2116)
Uncaught exception, not in assertion.
expected: nil
  actual: clojure.lang.ExceptionInfo: ehrt.sim-trajectory.gmf-interpreter: run-module exceeded max-steps -- likely a module authoring bug (a zero-time-advance transition cycle)
{:module "recheck-mod", :current :check}
 at ehrt.sim_trajectory.gmf_interpreter$run_module.invokeStatic (gmf_interpreter.clj:2116)
    [... run-module arity chain, gmf_interpreter.clj:2043 ...]
```

**Real-content scratch probe (feasible, run).** A SCRATCH-only script
(never committed, cleaned before this record's own commit) loaded
`injuries.json`'s real closure directly from the pinned synthea
checkout (`/home/mg/synthea-checkout`, resolve-fn/table-resolve-fn
mirroring `census.clj`'s own `make-resolve-fn`/`make-table-resolve-fn`
exactly) via `gmf/load-closure`, then ran `run-module` directly at the
census's own exact parameters (registration age 30, 50-year horizon,
`default-persona-config`, the SAME 120-seed mixer derivation
`census.clj`'s own `mixed-seeds` uses, mixer-seed 20260803) — no
engine, no CLI, ADR-0070's own direct-probe method repeated
faithfully.

- **Pre-fix:** 14 of 120 walks failed, all `run-submodule exceeded
  max-steps` at `:wait-for-dental-visit`/`:check-for-dental-visit` —
  the SAME exception ADR-0070 found (a different count than ADR-0070's
  own cited "4 of 120," disclosed honestly rather than forced to
  match: this session's own mixer/persona-config plumbing reproduces
  the qualitative finding exactly, at a materially higher observed
  rate, both consistent with "near-certain at population scale," ADR-
  0070's own conclusion either way — the exact rate was never this
  session's own claim to make).
- **Post-fix:** 0 of 120 walks throw `run-submodule exceeded max-
  steps`. 2 of 120 walks still fail — but on a SEPARATE, PRE-EXISTING,
  unrelated interpreter gap (`Assert failed:... nested :encounter --
  this project's GMF subset assumes encounters never nest`), and — the
  finding worth recording — these are the EXACT SAME TWO SEEDS that
  ALSO failed pre-fix, on the SAME assert, not newly exposed by this
  fix (confirmed by a full pre/post seed-by-seed diff, `git stash`/
  `git stash pop` around the source+test changes to compare cleanly).
  This fix therefore closes exactly the 12 seeds it targets and moves
  nothing else — no latent gap newly surfaced, none newly hidden.
  `injuries.json` remains NOT VENDORED regardless (still deferred
  whole; the nested-`:encounter` gap is a real, separate finding for a
  FUTURE session's own B2 attempt to characterize, not this session's
  own scope to fix).

### Fix commit

`b0b030d` — "fix: submodule walks respect horizon; runaway budget
counts only zero-advance steps (ADR-0105)."

**Half 1.** `horizon-end-t` threads from `run-module`'s own loop
through `step-safely` → `step` (new optional 6th argument, consulted
ONLY by the `:call-submodule` case — every other case and every
pre-ADR-0105 call site unaffected, `nil` by default) → `call-
submodule-step` → `run-submodule`, which now re-checks `:t` against
`horizon-end-t` before every step, mirroring `run-module`'s own
top-of-loop check exactly. Crossing the horizon inside a submodule now
parks with `:status :horizon-complete` — the SAME status a top-level
Delay overshoot produces ("parking past the horizon ends the walk in
the same status Delay uses," `wellness-wait-step`'s own docstring, the
mirror-site contract this fix keeps). `call-submodule-step` propagates
this up as a NEW outcome key, `:horizon-complete? true` (`:next nil` —
the caller's own post-call transition is never resolved against a ctx
whose clock already ran past the horizon), which `run-module`'s own
loop (the only caller that ever supplies a non-nil `horizon-end-t`)
recognizes and ends the WHOLE walk on. `walk-module` (never given a
`horizon-end-t`) is untouched by this half — the new outcome key never
appears for its own callers.

**Half 2.** A new shared helper, `consume-step-budget` (`n outcome ->
n`), used identically by all three `max-steps`-policed loops
(`walk-module`/`run-submodule`/`run-module`): increments `n` only when
`outcome`'s own `:advance` is zero, leaves it unchanged on any step
that genuinely advances module time. This is the "does not consume"
option the driving prompt's own Context named as one of two
acceptable semantics (the other, resetting `n` to 0 on any advance,
was also licensed) — chosen for matching `max-steps`'s own docstring
most literally ("counts only zero-time-advance steps," a population
count, not a consecutive-run count) and for landing identically at all
three call sites with one shared function. `max-steps`'s own docstring
is updated to state what the implementation now does, not merely
aspire to it.

**Co-landing tests** (`ehrt.sim-trajectory.gmf-interpreter-test`), all
green post-fix:

- The two reds above, flipped green.
- `run-module-zero-advance-spin-still-throws-max-steps` and
  `run-submodule-zero-advance-spin-still-throws-max-steps` — the
  backstop's documented job stays non-vacuous: a genuine zero-advance
  cycle (the existing `infinite-loop-module` shape, driven via
  `run-module` directly, and a NEW callee variant driven via
  `CallSubmodule`) still throws `max-steps` at both call sites the fix
  touches.
- `submodule-horizon-truncation-matches-a-top-level-truncations-own-
  status-exactly` — asserts EQUALITY between a real top-level
  truncation (`wellness-wait-then-encounter-module`, the pre-existing
  `wellness-wait-parks-past-the-horizon-the-same-way-delay-does`
  test's own module) and the new submodule-horizon truncation, not a
  literal keyword — the mirror-site contract proven, not merely
  asserted.
- Existing interpreter tests (203 tests, 527 assertions total post-
  fix, up from 200/523 pre-fix — the 3 new non-regression tests plus
  the 2 flipped reds account for the delta) pass unmodified — no
  existing test file or assertion edited, only appended to.
- `max-call-depth` and every other guard: unchanged, untouched by this
  fix.

**Design doc.** `components/sim-trajectory/docs/gmf-interpreter.md`
gains a dated addendum (§8, finding 5 — the site that already
discussed `run-module`/`horizon-end-t`/`max-steps` interaction)
recording both halves and pointing to this ADR.

### Oracle bracket

**Pre-analysis, per the driving prompt's own requirement, done BEFORE
running the bracket.** Enumerated every closure member any currently
vendored root can reach via `CallSubmodule` (`grep` over `resources/
sim/modules/**/*.json` for `"submodule"`, 33 distinct call-path
targets) and checked each for a `Delay` state. Four submodules carry
one: `anemia/anemia_sub.json` (two Delays, 1 hour and 1 day, both
`:exact`, neither loops), `metabolic_syndrome/amputations.json` (one
Delay, 6 weeks `:exact`, does not loop), `uti/ambulatory_path.json`
(two Delays, UNIFORM 0-48h/24-48h, neither loops), `uti/
telemed_path.json` (two Delays, UNIFORM 0-48h/24-48h, neither loops).
Every one of these eight Delay states transitions FORWARD (direct or
conditional) to a state that is never, by construction, itself a
predecessor of that same Delay — none loops, and every bound is small
(hours to 6 weeks) against the vendored corpus's own 50-100-year
horizons. **Expectation: pure identity** — no currently vendored
closure's own submodule walk plausibly straddles a horizon boundary,
so the horizon-awareness half changes no currently vendored behavior;
the counting half changes behavior only for a walk that currently
trips `max-steps`, and none currently do (no vendored root is
`:walk-failed` on `max-steps` today).

**Bracket result.** `bin/regression-oracle af2369c b0b030d`:
soundness check IDENTICAL outside `digest.clj`'s own `(ns ...)` form
(no digest-producer change this session touches `digest.clj` at all).
All 34 roots' SHA-256 digests IDENTICAL between baseline and target —
`IDENTICAL: every root's digest matches between af2369c and b0b030d`.
Matches the pre-analysis exactly. No STOP-AND-REPORT needed; no
digest moved.

### Full gate

`clojure -M:poly check`: OK (confirmed twice — before the fix commit
and again after every doc/close-phase edit landed). Full local suite
(`clojure -M:poly test`, unredirected capture): 672 occurrences of "0
failures, 0 errors" across the entire log, ZERO `FAIL`/`ERROR` report
lines anywhere, 6 minutes 11 seconds. `ehrt.cli.cli-parse-guard-lint-
test` confirmed green within that same run: 4 tests, 22 assertions, 0
failures/errors. `bin/verify-nist-lock`: OK, 6 hit-nexus-sourced
coordinates matched (`nist-hl7-v2-parser`, `nist-hl7-v2-profile`,
`nist-hl7-v2-validation`, `nist-xml-util`, `nist-hl7-v2-schemas`,
`nist-validation-report`).

**The last five `main` CI runs** (`gh run list --limit 5 --branch
main`, checked at session start, before this session's own pushes):
all `completed`/`success` — `af2369c` (ed-tuesday-scenario docs,
4m12s), `51f0e68` (ed-tuesday-scenario feat, 4m16s), `741b2f6`
(board-boundary-fix docs, 4m13s), the scheduled Integration run
(8m57s), `0099f81` (marker-only-footnotes/mllp-ruling docs, 4m15s) — no
red among the five, no probabilistic flake hiding behind a single
green.

### Fences

Touched exactly: `components/sim-trajectory/{src,test}`
(`gmf_interpreter.clj`, `gmf_interpreter_test.clj`), `components/
sim-trajectory/docs/gmf-interpreter.md` (the dated addendum),
`notes/adr/0105-*.md`, `notes/ADRs.md`, `notes/adr/README.md`,
`.agents/plans/roadmap.md` (the B-row rider), `.agents/rulings.md`,
`.agents/*` close-phase files. `resources/sim/modules` and `NOTICE`:
untouched — no vendoring, no new NOTICE row, no oracle-root addition.
Engine, emitters, sim, corpus, cli src: untouched. `max-call-depth`
and the `strict->` wellness-advance property (`next-wellness-tick`,
~289): unchanged, both re-confirmed live by the existing test suite
staying green unmodified. The zero-advance backstop still throws
(tested, non-vacuously, at both call sites the fix touches). Scratch
upstream fetches (the real `injuries.json`/`broken_jaw.json` probe):
never committed, cleaned before this record's own commit.

### Deviations, dated 2026-08-11

- **The real-content scratch-probe failure COUNT diverges from
  ADR-0070's own cited figure** (14/120 this session vs. "4 of 120"
  cited there) — disclosed above, in the red section, rather than
  forced to match or silently omitted. Both sessions' own qualitative
  finding (the SAME exception, at the SAME states, "near-certain at
  population scale") agrees exactly; only the precise rate differs,
  and this session makes no claim resting on the exact rate matching.
- **The zero-advance counting semantic choice** ("does not consume"
  vs. "resets on any advance," both licensed by the driving prompt's
  own Context) is a design decision recorded here, not a deviation —
  see Fix commit, half 2, above, for the reasoning.

### Consequence

A submodule's own time-advancing loop now respects the SAME horizon
bound its caller's root walk already did — `injuries.json`'s own
`broken_jaw.json` closure member no longer throws `max-steps` for a
reason that was never a real module-authoring bug, at any horizon.
The `max-steps` runaway-loop backstop now polices exactly the class
its own name and docstring already promised (a zero-time-advance
transition cycle) rather than merely aspiring to it, closing a SECOND,
independently-evidenced gap (`veteran_mdd.json`'s own NOTICE finding,
ADR-0090) at the SAME root cause. `injuries.json` itself is still NOT
vendored — a separate, real, pre-existing `nested :encounter` gap
remains (confirmed unaffected by this fix, named above for a future
session) — but the specific defect ADR-0070's own revisit trigger
named is closed, and `.agents/plans/roadmap.md`'s own B row records B2
(the injuries vendoring batch) as unblocked and ready for a future
session to schedule.

### Index line

```
- 2026-08-11 — interpreter-horizon-budget — ADR-0105
```

(appended to `.agents/plans/roadmap.md`'s own Done section.)

`notes/adr/README.md`'s own file count corrects 102→103, verified by
`ls notes/adr/*.md | grep -v README | wc -l`, not arithmetic.
