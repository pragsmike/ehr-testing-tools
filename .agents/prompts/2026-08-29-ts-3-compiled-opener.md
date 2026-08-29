# Session prompt -- TS-3: a compiled opener never asks `encounter-openable?` (the last v2-10^5 blocker)

Archived verbatim, 2026-08-29. Record:
`.agents/session-records/2026-08-29-ts-3-compiled-opener.md`.

---

Context. HEAD 11765bb. Row `[ts-3-outpatient-opens-over-an-encounter]`
(roadmap :62-84, diagnosed 2026-08-29): module-cohort patient
`PID-000640-f57cb996`; module-compiled `:admission` t=240300; discharged
t=244620; a `:cancel-discharge` in the SAME batch legally reinstates
(its subject is `:discharged` -- the one status a cancel-discharge may
find, so the TS-5 guard rightly does not fire) and re-opens
`ENC-000640-00` with NOTHING left in the queue to close it; the module's
later compiled `:outpatient-visit` (t=100609860) then opens over the
still-open encounter, flips `:class` on a patient still holding an
inpatient bed, and produces ALL 33,950 surviving
`outpatient-patients-occupy-no-bed` violations at v2 10^5 -- the whole
residue, one patient. Mechanism: a COMPILED encounter opener is queued
directly (`engine.clj:990-1031` attach path) and is never routed
through `:repeat-arrival`, so `encounter-openable?` (:630, asked at
:1251 and :1295 on the arrival paths) is never asked. Read TS-5's
record section "TS-3, re-scoped" and both roadmap rows first; reproduce at
the row's patient before anything else.

Step 1. Establish the full mechanism to BOTH halves: (i) the re-open --
what does the reinstated encounter's remaining queue actually hold
(trace it; "nothing left to close it" is the record's claim, verify);
does the decided/churn path have the same exposure or does something
re-queue a discharge there (say which, with the trace); (ii) the
opener -- exactly which application path the compiled
`:outpatient-visit` takes and where `encounter-openable?` COULD be
asked on it without touching the arrival paths.

Step 2. LETTER THE RULING AND STOP -- this touches compiled-pathway
semantics and the author decides. Options to develop with the trace
(verify, refine, or replace): (A) gate at application -- a compiled
opener coming due on a patient with an open encounter routes through
the same `encounter-openable?` law as arrivals; state what happens to
the rejected opener AND to the module's subsequent steps for that
encounter (dangling-steps analysis, per step type); (B) close-at-
reopen -- a cancel-discharge that re-opens an encounter whose queue
holds no closer schedules a re-discharge (what t? what draws? if it
draws, from which stream, and the fixed-consumption analysis --
suspect this is draw-affecting and say so honestly); (C) compile-time
-- the walk never compiles an opener into a window another encounter
of the same patient can still occupy (likely unknowable at compile
time once churn exists; if so, say why and strike it). For each:
blast radius (which corpora move -- predict from pattern counts per
shipped corpus), draw analysis, which invariants change meaning.
Recommendation with the reason. Deliver the letter in the session
record and the row; make NO fix in this session unless the author
replies within it.

Step 3 (on the author's ruling, this session or its successor). RED at
PID-000640's cell (population: zero opens-over-open-encounter; the
patient's trace asserted clean) plus a hand-built unit log for the
reopen-with-empty-queue case. GREEN. Brackets: predict, then run --
shipped corpora that contain the pattern move ONCE, declared;
unreachable ones IDENTICAL, stated with counts. Then the two 10^5
cells (v2 and nobed), warm-up + two timed, full health record;
self-check CLEAN or BLOCKED with the new reason; plan appendix
entries convert; exponents and msg/event over the completed series;
the 10^6 decision re-stated. `make test` + `make integration`; push;
CI; no tag. Record: traces, the ruling as given, draw analysis,
bracket outcome, cell tables; rows closed by sha; TS-4 re-run and
closed only with its mechanism, else re-scoped. Roadmap before record.

Fences. No fix before the ruling. Rejected/deferred compiled events
must consume no draw the applied path would not (or the change is a
declared reshuffle, said plainly). A cell failing self-check is
BLOCKED, not tuned. F3 absolute. One declared sweep at most.
