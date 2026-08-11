# 2026-08-11 — ehr-testing-tools: interpreter horizon/budget fix (ADR-0105)

## Context

Archived 2026-08-11. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `af2369c` (ADR-0104's own close) and closed at
the fix commit (`b0b030d`) plus this record's own close-phase commit.
Original prompt follows verbatim; a deviation record follows that.

## Original prompt (verbatim)

Session prompt -- interpreter: horizon-aware submodules, zero-advance
runaway budget (ADR-0105, B1 of the injuries arc)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session is B1 of the author-ruled injuries arc
(2026-08-11, author verbatim "yes" to the two-session plan): the
gmf-interpreter fix that ADR-0070's own revisit trigger names --
"a future session willing to extend gmf-interpreter's own
runaway-loop handling" -- prerequisite to B2 (the injuries vendoring
batch, NOT this session). HEAD at handoff: af2369c. This session's
ADR is ADR-0105.

THE DEFECT, two coupled halves (ADR-0070's diagnosis, extended by
design-channel arithmetic -- verify both against the tree):

1. `run-submodule` (gmf_interpreter.clj, the descend-run-return loop
   ~1459) is HORIZON-BLIND: it never receives `horizon-end-t`, while
   `run-module`'s own loop "re-checks `:t` against `horizon-end-t`
   before every `step` call" (that function's own docstring ~1523 --
   the mirror site). A time-advancing Delay loop inside a submodule
   therefore iterates past the horizon forever, tripping `max-steps`
   at ANY horizon -- ADR-0070's observed horizon-invariance
   (36500/18250/3650 all threw).
2. `max-steps` (10000, ~1419) counts EVERY step, but its own
   docstring defines its target as "a zero-time-advance transition
   cycle". A LEGAL upstream time-advancing loop (broken_jaw's 1-7-day
   dental wait) trips it within horizon at long horizons: at the
   50-year census horizon, mean ~4600 iterations, worst-case 18250 --
   tail seeds exceed 10000 with no bug present. VERIFY this
   arithmetic against the actual delay bounds and horizon before
   relying on it.

THE FIX, both halves in one commit:
- Thread `horizon-end-t` into `run-submodule` and mirror the
  top-level pre-step check exactly -- a walk crossing the horizon
  inside a submodule ends in the SAME truncation status the
  top-level Delay-overshoot path uses ("parking past the horizon
  ends the walk in the same status Delay uses" -- keep that
  contract, cite it).
- The runaway budget counts only ZERO-time-advance steps: any step
  that advances module time resets (or does not consume) the budget,
  so `max-steps` polices exactly its documented class. Update the
  docstring to say what the implementation now does; keep the
  exception's wording accurate.
- `max-call-depth` and all other guards unchanged.

ORACLE BRACKET -- genuinely sensitive, read carefully. Two movement
paths exist in principle:
(a) If any EXISTING vendored module's walks currently cross the
    horizon inside a submodule, the old code kept emitting
    post-horizon events from within that submodule; the fix truncates
    them -- digests for those engine roots would move LEGITIMATELY.
(b) The counting change alone alters no events (nothing currently
    vendored throws max-steps).
Before the bracket, ANALYZE path (a): identify which vendored
closures contain submodules with Delay/wait states, and reason about
horizon-crossing exposure. Expectation: pure identity IF no existing
walk crosses horizon inside a submodule; otherwise movement confined
to explainable post-horizon-submodule-event removal. EITHER WAY, any
digest movement is STOP-AND-REPORT with the per-root analysis -- a
declared-change ratification is the AUTHOR'S call, never a silent
baseline update. Do not proceed past a moved bracket.

## Read first

- notes/adr/0070-*.md -- the injuries deferral section IN FULL: the
  diagnosis, the direct-probe method (registration age 30, 50-year
  horizon, run-module called directly, 4/120 walks), the revisit
  trigger, the NOTICE dated section it names
- components/sim-trajectory/src/ehrt/sim_trajectory/
  gmf_interpreter.clj -- run-module's horizon loop, run-submodule,
  max-steps, the strict-> wellness-advance note (~277) whose
  loop-bounding property your change must not weaken
- components/sim-trajectory/docs/gmf-interpreter.md -- the design
  doc's sections on loops, horizon, and submodules (a dated addendum
  records this change; the doc is living, not frozen)
- components/sim/resources/sim/modules/NOTICE -- the injuries dated
  section (context only; NOTICE is untouched this session)
- bin/regression-oracle and the oracle digest layout -- for the
  bracket analysis
- .agents/plans/roadmap.md -- the ED row's B text (the rider below
  corrects it)
- .agents/rulings.md -- tag law AR-T-1, ASCII-first verification

## Author rulings, verbatim

- [A] 2026-08-11, author verbatim "yes" (to: B1 the interpreter fix
  -- horizon-aware submodule termination -- then B2 the batch). This
  session is B1 ONLY: no vendoring, no NOTICE rows, no oracle-root
  additions, no module content anywhere.
- [C] The zero-advance counting half, per the Context arithmetic:
  load-bearing, not optional -- horizon-awareness alone leaves
  within-horizon trips at the census's own 50-year parameters. If
  your verification of the arithmetic contradicts this,
  STOP-AND-REPORT with the numbers.
- [C] Roadmap correction rider (a design-channel error, owned): the
  ED row's B text calls the injuries batch "routine vendoring
  intake" while citing ADR-0070 -- whose own record deferred
  injuries WHOLE pending exactly this session. At the close, amend
  the row: B = B1 (this ADR) + B2 (the batch, unblocked once B1
  lands), and note the mis-characterization's correction with this
  ADR as its record.

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0104 landing at `af2369c` by fresh
   public clone. Tag `stable-20260810-ed-tuesday-scenario` at
   `af2369c`, push, verify the peeled ref. Remote moved =
   STOP-AND-REPORT.

2. **Red, reproduced hermetically.** A fixture module (test-local
   EDN/JSON, never under resources/sim/modules, never NOTICE'd)
   reproducing the attribute-gated time-advancing Delay-loop shape:
   set-attribute once, a Delay(1-7d) <-> conditional-check cycle
   gated on it, inside a CallSubmodule. Pre-fix: run-module (or the
   direct interpreter probe per ADR-0070's own method) throws
   `run-submodule exceeded max-steps` -- paste the red verbatim.
   A second hermetic red for the counting half: a shape that trips
   the budget WITHIN horizon purely by legal time-advancing
   iterations (tune delay bounds/horizon so iterations > 10000
   inside the horizon). If feasible, ALSO probe the real
   `injuries/broken_jaw.json` fetched from the recorded pin into a
   SCRATCH location (never committed): pre-fix throws on the seeds
   ADR-0070's method found, post-fix the same walks complete
   truncated. Scratch artifacts cleaned before any commit.

3. **Fix commit.** Both halves per Context, co-landed tests: the two
   reds flipped green; a zero-advance spin STILL throws (the
   backstop's documented job, non-vacuous -- use the retired
   create-now shape or a hand-built zero-delay cycle); the
   truncation status matches the top-level Delay-overshoot status
   exactly (assert equality against a top-level truncation, not a
   literal); existing interpreter tests pass unmodified. The design
   doc's dated addendum.
   Commit message (ASCII only):
   `fix: submodule walks respect horizon; runaway budget counts only zero-advance steps (ADR-0105)`

4. **Oracle bracket, with the pre-analysis** per Context. Pure
   identity confirms path-(a) non-exposure; ANY movement =
   STOP-AND-REPORT with per-root analysis, session pauses for the
   author.

5. **Full gate.** poly check, full local suite, CLI parse-guard
   lint, bin/verify-nist-lock.

6. **Close phase.** FIRST: self-archive this prompt. Then: ADR-0105
   (both diagnoses with the verified arithmetic, reds verbatim, the
   bracket analysis, deviations dated); roadmap: the B-row
   correction per the rider, B2 marked unblocked; .agents/rulings.md
   records the 2026-08-11 "yes" with the two-session plan it
   ratified; notes/ADRs.md index row; notes/adr/README.md count
   102 -> 103; session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- interpreter horizon/budget fix (ADR-0105)`

7. **Push and verify.** Push per R30 checkpoints. Post-push, ASCII
   check FIRST on every commit message, then CI confirmation.

## Fences

- Touch ONLY: components/sim-trajectory/{src,test},
  components/sim-trajectory/docs/gmf-interpreter.md (dated addendum),
  notes/adr/0105-*.md, notes/ADRs.md, notes/adr/README.md,
  .agents/plans/roadmap.md (the rider), .agents/* close-phase files.
  The sweep RULE governs over this list (ADR-0099 precedent).
- NO vendoring: resources/sim/modules and NOTICE untouched; no
  oracle-root additions; engine, emitters, sim, corpus, cli src all
  untouched.
- The strict-> wellness-advance property (~277) and max-call-depth
  are unchanged; the zero-advance backstop still throws (tested).
- Scratch upstream fetches never committed.
- No history rewrites; deviations dated; STOP-AND-REPORT over
  improvisation -- especially the oracle bracket.
- Channel claims (line numbers, arithmetic, the mirror-site contract
  wording) are verify-then-act.

## Deviations from the driving prompt

- No deviations from the driving prompt's own steps, fences, or
  rulings, and the oracle bracket never moved (pure identity, the
  predicted, non-exposure outcome) -- no STOP-AND-REPORT triggered.
- The real-content scratch probe's own observed failure rate (14/120)
  diverges from ADR-0070's own cited "4/120" -- disclosed in ADR-0105
  itself (Red section) rather than forced to match or silently
  omitted; the qualitative finding (same exception, same states,
  near-certain at population scale) agrees exactly.
- The zero-advance counting semantic ("does not consume the budget"
  rather than "resets it to zero on any advance") was a live choice
  between two options the driving prompt's own Context explicitly
  licensed either way -- recorded as a design decision in ADR-0105,
  not a deviation.
