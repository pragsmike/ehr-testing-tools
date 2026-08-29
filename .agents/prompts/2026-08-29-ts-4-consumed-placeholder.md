# Session prompt -- TS-4: a churn merge consumed an open placeholder (the last blocker, and the series' close)

Archived verbatim, 2026-08-29. Record:
`.agents/session-records/2026-08-29-ts-4-consumed-placeholder.md`.

---

Context. HEAD 23901f4. Row `[ts-4-placeholder-unresolved]` (roadmap
:62-84), CHARACTERISED: `PID-007500-e98926c1`, a placeholder John Doe
registered t=37017 (`:window-close-t 382617`), consumed at t=177420 as
the TARGET of an ORDINARY churn `:merge` (no `:cause`);
`every-placeholder-registration-is-resolved-or-still-open`
(`check.clj:1316`) counts only `(= :identification (:cause ev))` merges
as resolution, so the placeholder reads unresolved forever and, once
`:merged`, can never be filled or identification-merged. One violation
in EACH 10^5 cell, same patient, same instants. BLOCKS both cells,
alone. This is a semantics question, not a structure one; the law count
stays 1 whichever way it goes. Reproduce at the row's patient first;
verify the row's claims against the trace (the last two rows were
wrong in their particulars -- assume this one may be too, and say what
survives).

Step 1. Trace: the placeholder's full event family; the churn merge's
selection path (who was the survivor, what state the John Doe carried
in at merge time); whether the person's `:identity-resolution`
disposition still fires later against the now-`:merged` patient (a
second latent defect if it mints anything -- check both 10^5 logs and
the unit path); whether any SHIPPED corpus can produce the pattern
(placeholder + churn overlap counts per corpus -- predicts the bracket).

Step 2. Letter and proceed -- this one is small enough to fix on the
channel's recommendation IF the trace confirms the row; STOP only if
the trace overturns it or a lettered option changes semantics beyond
the pattern. Options: (A) CHECK-SIDE, recommended -- the invariant
becomes resolved-or-CONSUMED-or-still-open: a `:merge` naming the
placeholder as `:merged` closes its window whatever the cause,
because an erroneous merge eating a John Doe is a real MPI failure
shape and the corpus is telling the truth about it; the engine did
nothing wrong. Required riders: the invariant's docstring names the
failure shape and cites the row; a companion invariant asserts nothing
fills or identification-merges a `:merged` placeholder afterwards
(the second-latent-defect check made permanent); the witness counts
gain a `consumed-by-churn` column so the pattern is COUNTED, not just
tolerated. (B) ENGINE-SIDE -- churn's target selection skips patients
with an open identity window; draw analysis owed (does skipping
re-draw or shift the lottery? if it consumes differently, every churn
corpus reshuffles -- likely disqualifying); reject unless (A) is shown
unsound. (C) both. Channel prior, a hypothesis: (A) alone.

Step 3. RED at the row's cell (the one violation asserted, plus a
hand-built unit log: placeholder, ordinary merge inside the window,
invariant red on the old law); GREEN under the new law; the companion
invariant born with a firing mutation test. Brackets:
`bin/ground-truth-bracket 23901f4 HEAD` -- check-side only, so
IDENTICAL over all 38 is the prediction; any DIFFERS means the fix
leaked into the engine, STOP. `make test` + `make integration`.

Step 4. The cells, at last: v2 and nobed 10^5, warm-up + two timed,
full health record, self-check CLEAN required; plan appendix BLOCKED
-> MEASURED (dated, preamble); exponents and msg/event over the
completed 10^3-10^5 v2 series -- the program's number at scale, four
sessions owed; the 10^6 decision re-stated with the completed-series
arithmetic (take it only if heap permits; DECLINED keeps its label
and note). If a cell STILL fails self-check on some seventh thing,
it stays BLOCKED with the new reason and the new thing gets a row --
do not absorb it. Push; CI; no tag. Record: trace, the law as
landed with its riders, bracket line, cell tables, per-corpus
pattern counts; TS-4 closed by sha; roadmap before record.

Fences. No engine change under option (A) -- bracket-enforced. No
churn lottery change without the draw analysis and a STOP. A
disappearing violation is not a fixed one. F3 absolute. One session.
