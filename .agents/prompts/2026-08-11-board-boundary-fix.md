# 2026-08-11 — ehr-testing-tools: board boundary catch-up fix (ADR-0103)

## Context

Archived 2026-08-11. Repo: `~/src/ehr-testing-tools` (ext4 clone).
Session opened at HEAD `0099f81` (ADR-0102's own close) and closed at
the fix commit (`ad69fdc`) plus this record's own close-phase commit.
Original prompt follows verbatim; a deviation record follows that.

## Original prompt (verbatim)

Session prompt -- board boundary catch-up (ADR-0103)

## Context

You are Claude Code executing under R30 ceremony in the ehr-testing-tools
workspace. This session fixes a live defect in `ehrt play --board`'s
snapshot cadence, author-reported 2026-08-10 (design channel; the
report is transcript-witnessed -- this session reproduces its own red,
never citing the transcript as evidence). HEAD at handoff: 0099f81.
This session's ADR is ADR-0103.

THE DEFECT (bases/cli/src/ehrt/cli/core.clj, the board sink's
`maybe-snapshot!`, ~line 1428): boundaries live on a grid anchored at
`first-ts` with span `board-minutes * 60000`, but on render the code
advances by exactly ONE span -- `(swap! next-boundary-ms +
boundary-span-ms)`. After any stream-time jump (the idle-skip case),
the boundary lags arbitrarily far behind the stream, so EVERY
subsequent message satisfies `>= boundary` and renders -- including a
second message inside the same board window, which prints a duplicate,
identical snapshot line. Author-observed shape: paired identical
`-- board snapshot: <same ts> --` lines after idle-skips.

THE INVARIANT the fix restores (state it in the code's own docstring):
boundaries are `first-ts + k*span`; at most ONE snapshot per grid
window that contains messages, rendered at the first message at or
after each crossed boundary; after rendering at `ts`, the next
boundary is the smallest grid point strictly greater than `ts`
(computed arithmetically, never by looping span-at-a-time across a
years-long gap). Empty windows inside a gap render nothing -- the
current only-on-message behavior is correct and unchanged.

KNOWN DOWNSTREAM of the fix: demos/scenarios/busy-tuesday/README.md's
"What to look for" block documents a witnessed run summary
(snapshot-count 68, "every one of the 68 messages rendered a
snapshot") produced UNDER the buggy cadence. Post-fix, re-run that
block's exact generate+play commands (its own seed) and update the
witnessed numbers if they move; if they don't move, say so in the ADR
(the 68-message run may genuinely have had no same-window pairs). This
is a live doc, not a frozen archive.

RIDER (flagged to the author twice, un-vetoed; ADR-0102's own residual
finding): docs/glossary.md's Baseline and Pack entries still read
"`notes/ADRs.md`" in visible prose while their footnotes correctly
target `notes/tools/ADRs.md` -- drop the stale path prefix from both
entries' prose (the footnote carries the reference), minimal
rewording, before/after in the ADR. The hardened gate doesn't catch
path-prefix text; no gate change chartered.

Oracle bracket, with its reasoning: pure identity on all 34 roots is
EXPECTED -- the footprint is one CLI sink fn, its tests, one demo
README, and two glossary prose lines; no oracle-path namespace
(`ehrt.oracle.digest` requires only sim-family interfaces) is
touched. Movement = STOP-AND-REPORT.

## Read first

- bases/cli/src/ehrt/cli/core.clj -- the board sink closure (~1418-
  1448) in full, including cue-fn/sink-fn wiring and the closing
  summary's `:snapshot-count`
- notes/adr/0067-player-board.md -- the board's landed contract (what
  "display-only" means; the summary envelope's fields)
- notes/adr/0014-corpus-player.md -- plan/execute seam and cue rule
  (the idle-skip machinery this cadence interacts with)
- demos/scenarios/busy-tuesday/README.md -- the witnessed block
- notes/adr/0102-*.md -- the glossary residual finding (rider
  provenance)
- .agents/rulings.md -- tag law AR-T-1, ASCII-first verification

## Author rulings, verbatim

- [A] 2026-08-10, author verbatim "c." (both: this bugfix now, the
  scenario redesign as its own next arc). This session is the bugfix
  HALF ONLY: no scenario/config/content changes, no ED-weighting work
  -- that design pass opens separately.
- [A] 2026-08-10, redesign direction, author verbatim: "Maybe weight
  the patient population toward immediate, emergent conditions like
  trauma/injuries? This would simulate an actual ED, which is where a
  lot of the activity and churn would happen." Recorded at the close
  as the redesign arc's chartering context -- NOT executed here.
- [C] The rider per Context. If the driving conversation vetoed it,
  skip and note.

## Steps

1. **Tag ceremony (tag law case i -- licensed here).** The design
   channel has verified the ADR-0102 landing at `0099f81` by fresh
   public clone. Tag `stable-20260810-marker-only-footnotes` at
   `0099f81`, push, verify the peeled ref. Remote moved =
   STOP-AND-REPORT.

2. **Red, reproduced.** A hermetic test at the sink level: a
   synthetic three-message sequence -- t0, then t0 + a multi-day gap,
   then thirty stream-seconds later (same board window at
   --board 60) -- asserting on the sink's rendered output. Pre-fix:
   the third message renders a duplicate snapshot (paste the red
   verbatim). Also reproduce live if convenient (a small generated
   corpus + play), but the hermetic red is the required witness.

3. **Fix commit.** The arithmetic catch-up per the invariant, in
   `maybe-snapshot!` only; docstring states the invariant; the
   summary envelope's shape unchanged (`:snapshot-count` now counts
   correctly by construction). Co-landed: the red test flipped
   green, plus a grid-invariant test (multiple windows, each with
   1-3 messages, exactly one snapshot per occupied window, rendered
   at each window's first message; a years-long gap costs O(1)
   boundary computation -- assert no per-span looping by
   construction, not by timing). Re-run the busy-tuesday README
   witnessed block per Context; update or attest. The glossary rider
   in this same commit or its own -- your call, disclosed.
   Commit message (ASCII only):
   `fix: board snapshot boundary catches up past stream jumps (ADR-0103)`

4. **Oracle bracket.** Expected pure identity per Context;
   movement = STOP-AND-REPORT.

5. **Full gate.** poly check, full local suite, CLI parse-guard
   lint, bin/verify-nist-lock.

6. **Close phase.** FIRST: self-archive this prompt. Then: ADR-0103
   (the defect's mechanism, the invariant, red/green verbatim, the
   README attestation, the rider's before/after, deviations dated);
   .agents/rulings.md records both 2026-08-10 rulings verbatim ("c."
   and the ED-direction quote, the latter marked as the redesign
   arc's chartering context, arc not yet opened); roadmap: a Next
   row for the busy-tuesday/ED scenario redesign arc, anchored to
   the ED-direction ruling and ADR-0103's findings, marked
   awaiting-design-pass (the design channel frames it next -- the
   row records the charter, not a plan); notes/ADRs.md index row;
   notes/adr/README.md count 100 -> 101; session record.
   Commit message (ASCII only):
   `docs: session record and prompt archive -- board boundary fix (ADR-0103)`

7. **Push and verify.** Push per R30 checkpoints. Post-push, ASCII
   check FIRST on every commit message, then CI confirmation.

## Fences

- Touch ONLY: bases/cli/{src,test} (the board sink and its tests),
  demos/scenarios/busy-tuesday/README.md (witnessed block only),
  docs/glossary.md (the two rider lines), notes/adr/0103-*.md,
  notes/ADRs.md, notes/adr/README.md, .agents/* close-phase files.
  The sweep RULE governs over this list (ADR-0099 precedent).
- components/corpus/board.clj is UNCHANGED (render-snapshot is pure
  and correct; the defect is the CLI executor's cadence).
- No scenario config, module, sim, or content changes -- the
  redesign arc is separate by ruling.
- No history rewrites; deviations dated; STOP-AND-REPORT over
  improvisation.
- Channel claims are verify-then-act.

## Deviations from the driving prompt

- No deviations from the driving prompt's own steps, fences, or
  rulings. The busy-tuesday README's witnessed numbers DID move
  post-fix (68 -> 48 snapshots); the prompt's own contingency for
  that case (update, don't merely attest) is what executed.
