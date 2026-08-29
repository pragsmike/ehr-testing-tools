# Session prompt -- TS-1..TS-4: the defects the scale rerun found, fixed, and the blocked cells measured

Archived verbatim. Session record:
`.agents/session-records/2026-08-29-ts-defects-and-blocked-cells.md`.

---

Context. HEAD 2301acc. The traffic-scale close (record
`2026-08-29-traffic-scale-close.md`, findings at :458-540) found four
defects at 10^4-10^5 that no shipped corpus can reach, and its report
said "rowed" while the tree holds no rows -- the third instance of that
class. This session: rows FIRST, then diagnosis-gated fixes
red-before-green, then the two blocked v2 cells measured so the plan's
new series is complete. The record's diagnoses are hypotheses to
VERIFY, not premises -- TS-1 and TS-2 are probed to the event; TS-3 is
"almost certainly TS-2's other side" (unproven); TS-4 is uncharacterised.

Step 0, own commit, docs-only: four `## Next` rows `[ts-1-seventh-bed-
arc]` .. `[ts-4-placeholder-unresolved]`, each one line quoting the
record's location and BLOCKS status. The close report's rowing claim is
thereby made true late; the row text says so.

Step 1. TS-1 (record :458-493): a reinstating `:cancel-transfer` landing
during `:cleaning` produces `[:cleaning :occupied]`, which
`legal-bed-transitions` does not hold. The engine is behaving correctly
(the record shows `decide :bed-ready`'s guard then no-ops). This is the
sixth-arc pattern a third time: RATIFY a seventh arc `cleaning ->
occupied, reinstatement only` into the relation and ADR-0174 section 2(c)'s
table (dated), with a RED unit test first -- a hand-built log with a
cancel inside the turnaround window (2 events in 16,322 at 10^4: the
witness is authored, and its `pos?` count pinned). Check-side only; no
corpus moves. Verify the record's mechanism by reproducing at the
record's own seed before ratifying.

Step 2. TS-2 (:494-521) + TS-3 (:522-528). Reproduce first (`nobed`
config, the record's events). The mechanism claim to verify: nothing
gates the authored pathway walk on the encounter's class, so an
outpatient encounter's pathway can carry a `:transfer` that allocates a
licensed bed (`:from nil` is the tell; 24-25 patients per 10^5 cell).
This is an ENGINE defect against the skeleton's own semantics
(`outpatient-patients-occupy-no-bed` is the contract). Design question,
lettered in the record when you get there, but the channel's read to
test first: the walk's steps for an outpatient-class encounter must not
include bed-seeking step types -- filter at compile/queue time by
encounter class, deterministic, no draw consumed or skipped (the
fixed-consumption law: state what happens to the draws a dropped step
would have made -- if any step type draws, dropping it reshuffles, and
the fix must instead make the step decide to a non-allocating outcome).
RED: a population test at the record's seed asserting zero
outpatient-encounter licensed allocations, plus TS-3's one event
reproduced and shown same-root or not (say which, with the trace).
GREEN. This CAN move corpora: run `bin/ground-truth-bracket` -- if any
shipped corpus moves, it is a declared sweep (predict from the witness
counts first: shipped corpora may be unreachable, in which case the
brackets read IDENTICAL and only the dense/v2 series moves, unpinned).
TS-4 (:530-540): after TS-2's fix, re-run the failing cell; if the one
placeholder violation persists, characterise it to the event and row
its diagnosis; if it vanishes, say so with the trace -- do not close it
on disappearance alone without the mechanism.

Step 3. The blocked cells. With TS-1/TS-2 green: v2 at 10^4 and 10^5,
warm-up + two timed, full health record, same driver and preamble as
the close (its scratch is documented in the record); the plan appendix's
BLOCKED entries become MEASURED (dated, own preamble); messages-per-
event and exponents re-derived for the completed series. If a cell
still fails its self-check, it stays BLOCKED with the new reason.

Step 4. `make test` + `make integration`; both brackets bracketing the
whole session (declared only if step 2 moved shipped corpora); push;
CI; no tag. Record: the four diagnoses verified-or-corrected, the fix's
draw analysis, cell tables, rows updated (TS-1/TS-2 closed by sha,
TS-3/TS-4 closed or re-scoped). Roadmap reflects reality before the
record claims it -- the close's own lesson, quoted.

Fences. Rows before fixes. No fix without reproduction at the record's
seed. TS-2's fix must not consume or skip a draw differently for
non-outpatient patients (bracket-enforced). One declared sweep at most.
A disappearing defect is not a fixed one.
