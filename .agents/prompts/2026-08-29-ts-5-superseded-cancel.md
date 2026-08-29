# Session prompt -- TS-5: a cancel may not reinstate what a later event superseded (and the 10^5 cells)

Archived verbatim, 2026-08-29. Record:
`.agents/session-records/2026-08-29-ts-5-superseded-cancel.md`.

---

Context. HEAD a4e8698. Row `[cancel-transfer-reinstates-a-discharged-
patient]` (roadmap :62-76, probed to the event: PID-004302, `:discharge`
t=303660 nils the location, `:cancel-transfer` SAME t restores
SURGERY-91, an `:outpatient-visit` at t=3068460 inherits the bed; 13
distinct patients across the two 10^5 cells; the whole remaining root of
`outpatient-patients-occupy-no-bed` there). It BLOCKS both 10^5 v2
cells, alone. `decide`/`evolve :cancel-transfer` is reached by every
churn-carrying shipped corpus, so this is a CANDIDATE DECLARED SWEEP --
predict from the brackets, don't assume either way. The design question
the row refuses to answer is yours to letter for the author BEFORE
fixing (mechanism verified first; the TS-2 lesson -- two plausible
mechanisms were wrong before one trace was right -- is one session old).

Step 1. Reproduce at the row's own events (both PIDs); then establish
the mechanism to the ordering fact: is this the batch order (`[t
seq-no]`: the cancel decided after the discharge at equal t) or the
reinstate-index (the cancel reads `:before` state that predates the
discharge, correct in itself, applied without checking the subject's
CURRENT status)? The two mechanisms imply different fixes; trace, do
not infer. Also answer: why does `bed-reoccupied-by-someone-else?`
not catch it (presumably: the bed is empty, no one re-occupied -- the
patient is gone, not displaced).

Step 2. Letter the ruling, with the trace attached, and STOP for the
author IF the fix choice changes ground-truth semantics beyond the
defective pattern; otherwise proceed on the recommendation and record
it as channel-inferred [C]. Expected options (verify against the
trace): (A) engine guard -- `decide :cancel-transfer`/`:cancel-
discharge` reject (`:illegal-cancel-*` outcome, the existing rejected-
outcome shape, consuming exactly the draws the applied path consumes --
state the draw analysis) when the subject's status is `:discharged`/
`:expired`/`:merged` at decide time, i.e. a cancel may not reinstate
state a later-or-equal event superseded; (B) same-t only -- reject only
when the superseding event shares the batch (narrower; leaves a
later-t cancel free to resurrect a discharged patient, which is the
same defect slower); (C) check-side -- ratify resurrect-then-inherit
as legal and relax the invariant (rejected unless the trace shows
real-world ADT actually behaves this way -- cite or drop). Channel
prior, explicitly a hypothesis: (A).

Step 3. RED at the row's seed (a population test: zero discharged
patients holding a location, plus the two PIDs' traces asserted
clean), plus a hand-built unit log for the same-t batch case. GREEN.
Draw analysis in the commit body. Brackets: `bin/ground-truth-bracket
a4e8698 HEAD` -- IF any shipped corpus contains the pattern it moves
and this is ONE declared sweep (re-pin per the standing list,
witnesses re-derived and disclosed); if none reaches it, IDENTICAL and
no declaration -- either outcome stated with the per-corpus pattern
count that explains it. TS-3 (:77+) and TS-4 (:91+): re-run their
cells after the fix; each closes ONLY with its mechanism traced (TS-3
was already shown not-TS-2; if it is TS-5-rooted, show it), else
stays open re-scoped.

Step 4. The cells. v2 and nobed at 10^5, warm-up + two timed, full
health record; self-check must be CLEAN or the cell stays BLOCKED with
the new reason; the plan appendix's BLOCKED entries convert to
MEASURED (dated, preamble); exponents and msg/event re-derived over
the completed v2 series 10^3-10^5; the 10^6 decision re-stated with
the new arithmetic (take it only if the heap projection now permits;
a declined 10^6 keeps its PROJECTED label and note). `make test` +
`make integration`; push; CI; no tag. Record: the trace, the ruling
as taken, draw analysis, bracket outcome with counts, cell tables,
rows closed by sha. Roadmap before record.

Fences. No fix before the trace. Rejected-outcome draws identical to
applied-path draws or STOP (a consumption change reshuffles every
churn corpus). One declared sweep at most. A cell that fails
self-check is BLOCKED, not tuned. F3 absolute.
