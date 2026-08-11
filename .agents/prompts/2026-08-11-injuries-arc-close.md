# 2026-08-11 — ehr-testing-tools: injuries arc close, auto-close on reopen (ADR-0107)

## Context

Archived 2026-08-11. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `fdb3984` (ADR-0106's own close) and closed at
this record's own close-phase commit. Original prompt follows
verbatim; a deviation record follows that.

## Original prompt (verbatim)

# Session prompt -- B3: auto-close on reopen, injuries batch lands (ADR-0107)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session is B3, closing the injuries arc under the
author's 2026-08-11 ruling: option (i) of ADR-0106's design options --
auto-close on reopen, matching upstream exactly -- then, ON ITS GREEN,
the injuries batch itself lands under the standing vendoring ceremony.
Two phases, ordered; if phase 1 cannot land green, STOP-AND-REPORT and
phase 2 never starts. HEAD at handoff: fdb3984. This session's ADR is
ADR-0107.

PHASE 1 -- the interpreter fix, per ADR-0106 option (i) verbatim
("The `:encounter` case's own assert becomes a conditional auto-close:
when `open-encounter-index` is non-nil, synthesize an implicit
`:encounter-end` for the stale one (referencing it, per this
project's own citation law) before emitting the new `:encounter`"):
- Upstream-faithful timing, source-verified at the pin
  (State.java Encounter.process ~984: `encounterEnd(time, ...)` at the
  NEW encounter's own time): the synthesized `:encounter-end`'s `:t`
  equals the new `:encounter`'s `:t`, ordered end-before-open in the
  trajectory.
- The synthesized end references the stale open per the citation law;
  it flows through emitters EXACTLY as a real end does -- that is the
  point (sequential ED-to-treatment conversion on the wire).
- The `:encounter-end`-with-nothing-open path
  (`:suppressed-encounter-ends`, ADR-0082 R2) is UNCHANGED. The
  zero-open assert semantics elsewhere unchanged. `mark-phase`'s
  single-open assumption is now SATISFIED by construction -- state
  that in the docstring, don't touch mark-phase.
- Consider (and record either way) whether the synthesized end is
  countable -- a `:synthesized-encounter-ends` counter mirroring the
  suppressed-ends precedent gives downstream tests a witness; if you
  add it, co-land its assertion; if not, say why in the ADR.

PHASE 1 red: the hermetic fixture shape (Encounter -> no EncounterEnd
-> Encounter) throws pre-fix; ADR-0106's two recorded failing seeds
against the pinned injuries closure (SCRATCH fetch) throw pre-fix,
complete post-fix with the synthesized end present and correctly
cited. The zero-open suppressed-end tests pass unmodified.
Commit message (ASCII only):
`fix: nested encounter open auto-closes the stale one, upstream-faithful (ADR-0107)`

PHASE 2 -- the batch, per AR-VB1-2/3/4 + AR-VB4 mechanics, ADR-0106's
CORRECTED disposition (5 already vendored / 3 new: injuries.json,
injuries/broken_jaw.json, snf/skilled_nursing_facility.json --
dme/wheelchair.json was already landed by a sibling batch;
re-verify all 5 byte-identical at the pin, no re-vendor, no new
NOTICE rows for them):
- Byte-verbatim copies of the 3 at the pin
  (7e08387c68a7f0e21d13076609a159fd473fc902), SHA-256 + pin NOTICE
  rows, the injuries dated section closed out (both legs: max-steps
  per ADR-0105, nested-encounter per phase 1 -- deferral LIFTED,
  citing ADR-0070's own trigger satisfied).
- Round-trip tests red-before-resource per landed module (mirror a
  batch-4 test's home and shape); the attribute-gate check recorded
  as handled-by-ADR-0105 (broken_jaw's dental_referral).
- New engine-layer oracle roots as FIRST BASELINES; modules
  AVAILABLE, not default.
- The 2/120 seeds that previously threw: at least one becomes a
  NAMED regression test (the walk completes, the synthesized end
  present) -- the arc's own closing witness.
Commit message (ASCII only):
`feat: injuries closure vendored -- both deferral legs closed, arc complete (ADR-0107)`

ORACLE BRACKET, with its reasoning (verify, then state): the
auto-close path fires only when a walk opens an encounter over a
stale open -- NO currently vendored module contains that shape
(ADR-0106's full-graph sweep: the sole hazard pair lives in
injuries.json, not vendored until phase 2). Expectation: existing 34
roots byte-identical across BOTH phases; phase 2's new roots recorded
as first baselines. Any other movement = STOP-AND-REPORT.

## Read first

- notes/adr/0106-*.md IN FULL -- option (i)'s text, the seeds, the
  corrected closure disposition, the engine-isolation finding (NOT
  this session's scope; its roadmap row stands)
- notes/adr/0105-*.md -- the probe method reused here
- notes/adr/0070-*.md and notes/adr/0090-*.md -- vendoring mechanics
  (AR-VB1-2/3/4, AR-VB4-0..5)
- components/sim-trajectory/src/ehrt/sim_trajectory/
  gmf_interpreter.clj -- the assert site, open-encounter-index,
  suppressed-encounter-ends, the citation law's reference shape
- components/sim/resources/sim/modules/NOTICE -- the injuries dated
  sections (both amendments)
- components/oracle/src/ehrt/oracle/digest.clj -- root mechanics
- a batch-4 vendored-module test -- the round-trip test shape/home
- .agents/rulings.md -- tag law AR-T-1, ASCII-first verification

## Author rulings, verbatim

- [A] 2026-08-11, author verbatim "Let's do (i)." -- option (i),
  auto-close on reopen. Phase 2 (the batch) is the arc's chartered
  completion per the B3 framing the author accepted.
- [A] 2026-08-11, NEW DESIGN DIRECTION, author verbatim, recorded at
  the close as chartering context for a FUTURE design pass (nothing
  executed this session): "Also, I want to make sure that the
  simulation faithfully simulates what happens in real life: lab
  results take time to come back, providers take time to log things
  in the EHR, etc. so it's possible that a downstream receiver of
  the HL7 traffic will have incomplete encounter records for some
  time. That's not our problem to solve, but in order to test that
  such downstream receivers handle it properly (whatever that might
  mean for them) we need to supply them with such cases." A roadmap
  Next row anchors it, marked awaiting-design-pass; the design
  channel frames it next.

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0106 landing at `fdb3984` by fresh
   public clone. Tag `stable-20260811-injuries-b2-assessment` at
   `fdb3984`, push, verify the peeled ref. Remote moved =
   STOP-AND-REPORT.

2. **Phase 1** per Context: reds, fix, co-landed tests, commit, push.

3. **Phase 1 gate before phase 2:** full interpreter test namespace
   green; the scratch injuries probe 0-throw across ADR-0105's 120
   well-mixed seeds (max-steps AND nested-encounter both clear);
   oracle bracket leg 1 (existing 34 identical).

4. **Phase 2** per Context: vendor, NOTICE, round-trip tests
   red-before-resource, first-baseline roots, the named-seed
   regression witness, commit, push.

5. **Oracle bracket leg 2 + full gate.** Existing 34 identical; new
   roots first baselines; poly check, full local suite, CLI
   parse-guard lint, bin/verify-nist-lock.

6. **Close phase.** FIRST: self-archive this prompt. Then: ADR-0107
   (both phases' evidence verbatim, the counter decision, the arc's
   closing narrative: ADR-0070 deferral -> ADR-0105 -> ADR-0106 ->
   here); roadmap: the injuries/B rows CLOSED, the arc dispositioned;
   the NEW latency-realism Next row per the ruling above;
   .agents/rulings.md records both 2026-08-11 rulings verbatim;
   notes/ADRs.md index row; notes/adr/README.md count 104 -> 105;
   session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- injuries arc closed (ADR-0107)`

7. **Push and verify.** Push per R30 checkpoints. Post-push, ASCII
   check FIRST on every commit message, then CI confirmation.

## Fences

- Phase 1: components/sim-trajectory/{src,test} and its design doc's
  dated addendum ONLY. mark-phase untouched. Emitters untouched (the
  synthesized end is an ordinary event to them -- if any emitter
  turns out to special-case it, STOP-AND-REPORT).
- Phase 2: components/sim/resources/sim/modules/ (the 3 + NOTICE),
  the vendored-test home, components/oracle/src/ehrt/oracle/
  digest.clj (first-baseline roots only).
- Close phase: the usual register files.
- The sweep RULE governs over these lists (ADR-0099 precedent).
- engine/run per-patient isolation is NOT this session (its row
  stands). No default-config changes. Scratch cleaned before commits.
- No history rewrites; deviations dated; STOP-AND-REPORT over
  improvisation.
- Channel claims (the upstream timing citation, the sweep's
  uniqueness, the 5/3 disposition) are verify-then-act.

## Deviations, disclosed (recorded at this session's own close)

- **`:module-horizon-days` for the injuries round-trip tests/oracle
  root is 18250 (50 years), not the 36500-day (100-year) convention
  most engine-layer roots use.** Found live: `engine/run` at 36500
  days throws `run-submodule exceeded max-steps` at
  `broken_jaw.json`'s own `Wait for Dental Visit` branch --
  ADR-0106's own dated finding [C] predicted exactly this (mean
  ~9124 cycles to cross 100 years, over the interpreter's 10000-step
  budget; mean ~4562 cycles at 50 years, safely under it). The
  prompt's own Context named "mirror a batch-4 test's home and
  shape" for the round-trip test's STRUCTURE, not its horizon value
  verbatim -- disclosed here as a deliberate, evidenced choice, not
  a silent substitution.
- **The `:synthesized-encounter-ends` counter was added** (the
  prompt's own Phase 1 instruction: "if you add it, co-land its
  assertion; if not, say why"). Added, mirroring
  `:suppressed-encounter-ends` exactly through every outcome-
  constructing site in `gmf_interpreter.clj`; asserted at the
  hermetic fixture test, the named regression test (interpreter
  layer), and a pinned population-scale count (engine layer, 4 across
  300 patients at seed 20260802).
- **`wellness-wait-step`'s own separate nesting assert (the "(wellness)"
  variant) was left UNCHANGED**, a disclosed, narrower scope decision:
  ADR-0106's own full-graph sweep found the hazard only in the
  ordinary `:encounter` case dispatch, and the design option named in
  ADR-0106 and ruled on by the author names that one case
  specifically.
- **`.agents/reading-sets.edn`'s own `:onboarding` budget re-baseline**
  was folded into this same close-phase step, not a separate session
  -- `reading-set-budget-test` went red once this session's own
  `roadmap.md`/`prompts/README.md`/`session-records/README.md` growth
  landed, the same "routine growth, re-baseline on the session that
  trips it" discipline every prior re-derivation comment in that file
  already establishes.
