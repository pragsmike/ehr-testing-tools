# TS-5: a cancel may not reinstate what a later event superseded

Session record, 2026-08-29. HEAD at start `a4e8698`; ceremony R30
(commit and push at each checkpoint, unattended), taken from the
prompt. `bin/preflight` ran first and printed **no findings, exit 0**,
with two disclosures: a CI run among the last five still in progress
(a4e8698's own, not awaited, not counted as red) and HEAD not tagged
`stable-*` (correct -- no tag is paid).

## 0. What this session did

`roadmap.md#cancel-transfer-reinstates-a-discharged-patient` was probed
to the event; its MECHANISM was measured rather than inferred; the
design question it refused to answer was lettered, decided, and the
decision taken as channel-inferred [C]; and the guard landed with five
gates, four hand-built and one population run at the row's own seed.

**The channel's own preferred option (A) was refuted by the measurement
it asked for**, and the correction -- an index-relative guard -- is what
landed and is this session's main finding.
`bin/ground-truth-bracket a4e8698 c5e5f2b` came back **IDENTICAL over 38
roots**, so no declaration was owed and nothing was re-pinned; the
per-corpus pattern count that explains that is in section 4. Both 10^5
add-on cells were re-run and **both stay BLOCKED**, on reasons that are
now single-instance rows rather than the row that owned the mass -- and
the closed row's own claim to block them alone is corrected.

## Fences honoured

- **No fix before the trace.** The probe ran at `a4e8698` on the close's
  own scratch configs; the guard was written only after the decide-time
  state at both of the row's patients was in hand.
- **Rejected-outcome draws identical to applied-path draws.** Zero on
  both, and asserted by a test rather than by a reading (section 4).
- **One declared sweep at most.** None was spent: the bracket is
  IDENTICAL, so no sweep was declared.
- **A cell that fails self-check is BLOCKED, not tuned.** Both cells
  fail; both stay BLOCKED; neither plan-appendix entry converts to
  MEASURED (section 7).
- **F3 absolute.** The 10^6 decision keeps its PROJECTED label and its
  note, and is re-stated rather than promoted (section 7.6).

## 1. The trace, and the mechanism it settles

Reproduced at `dense-7500-nobed.edn`, seed 20260824, HEAD `a4e8698`:
129,407 events, the row's own two patients at the row's own instants.
The probe (`spike.probe-ts5`) wraps the `decide` VAR rather than reading
the finished log, because the one measurement that separates the two
candidate mechanisms is the subject's `:status` in `world` at the
instant `decide` runs, and the log does not carry it.

    DECIDE cancel-transfer pid=PID-004302-fa1ab125 t=303660 log-len=26852
       world-state-at-decide-time {:status :discharged, :class :inpatient,
                                   :home-ward "Medicine B", :location nil,
                                   :admitted-at 264000}
       last-uncancelled-index     25384 {:t 288360, :event :transfer}
       reinstated-state           {:status :admitted,
                                   :location {:ward "Surgery", :bed "SURGERY-91",
                                              :placement :licensed},
                                   :home-ward "Surgery"}
       occupancy-board[ SURGERY-91 ] -> nil (reoccupied-by-someone-else? false)

    DECIDE cancel-transfer pid=PID-005562-03ed543c t=363060 log-len=32385
       world-state-at-decide-time {:status :discharged, :class :inpatient,
                                   :home-ward "Medicine A", :location nil,
                                   :admitted-at 337920}
       last-uncancelled-index     30529 {:t 343200, :event :transfer}
       reinstated-state           {:status :admitted,
                                   :location {:ward "Emergency", :bed "ED-31",
                                              :placement :licensed},
                                   :home-ward "Emergency"}
       occupancy-board[ ED-31 ] -> nil (reoccupied-by-someone-else? false)

and `PID-004302`'s own stream, replayed:

    23030 t=264000    admission        -> SURGERY-91, home-ward Surgery
    25384 t=288360    transfer         -> MEDICINE-B-05, home-ward Medicine B
    26844 t=303660    care-plan-end
    26849 t=303660    discharge        after: :status :discharged, :location nil
    26851 t=303660    appointment
    26852 t=303660    cancel-transfer  cancels 25384, restores SURGERY-91
    93474 t=3068460   outpatient-visit ENC-004302-01 opens, bed still held -> RED
    93476 t=3069660   outpatient-visit-end

### WHICH MECHANISM: the reinstate-index, not the batch order

The two readings the prompt puts side by side are distinguished by one
fact and the probe measures it directly: **at the instant
`decide :cancel-transfer` runs, `world` ALREADY carries the discharge**
-- `:status :discharged`, `:location` nil. It is not a batch-ordering
problem in which the cancel is decided against a pre-discharge world
that no decide-time test could see.

The run loop is what makes that so, and it is structural rather than
incidental: `run` pops ONE queue entry at a time and folds that decide's
events into `world` before the next entry is popped (`engine.clj`'s
`world''` binding). A batch is one decide's output, never a set of
simultaneous decides. The discharge (26849), the appointment (26851)
and the cancel (26852) are three consecutive steps of ONE patient's own
step list at one `t`; each sees the previous one's effect.

So the mechanism is the second reading: `reinstated-state` hands back
the state before log index 25384 -- correct in itself, and exactly what
an undo of THAT transfer means -- and `decide` applied it without ever
asking what had become of the subject since. The fix follows from the
mechanism: a decide-time status test, which is possible only because
the state is there to test.

### Why `bed-reoccupied-by-someone-else?` does not catch it

Presumption confirmed, at both witnesses: the occupancy board reads
**nil** for SURGERY-91 and for ED-31. Nobody re-occupied the bed,
because the patient who left it was discharged and the bed was
genuinely vacated. The guard asks "has someone else taken this bed?",
and the honest answer is no. The patient is gone, not displaced.

## 2. The ruling, lettered

### What the row refused to answer

May a cancel reinstate state that a LATER event has already superseded?

### The options, as the channel lettered them -- and one correction

(A) ENGINE GUARD. `decide :cancel-transfer`/`:cancel-discharge` reject
    when the subject's status is `:discharged`/`:expired`/`:merged` at
    decide time.

    **REFUTED AS LETTERED, by measurement.** A `:cancel-discharge`
    exists to find its subject `:discharged` -- that is the state it
    undoes. (A) applied literally rejects EVERY cancel-discharge in the
    repository: 55 of 55 at `nobed` 10^5 and 6 of 6 at `v2` 10^4 read
    `:discharged` at decide time, and all of them are legal.

(A') ENGINE GUARD, INDEX-RELATIVE. The same guard, with the superseded
    set taken RELATIVE to the status the cancelled event itself leaves
    behind: a `:transfer` leaves `:admitted`, a `:discharge` leaves
    `:discharged`. So a cancel-transfer is illegal against
    `:discharged`/`:expired`/`:merged`, and a cancel-discharge only
    against `:expired`/`:merged`.  **TAKEN.**

(B) SAME-T ONLY. Reject only when the superseding event shares the
    batch. **Rejected**: at `nobed` 10^5 the 52 applied superseded
    cancel-transfers are not all same-t, and a later-t cancel
    resurrecting a discharged patient is the same defect slower. The
    trace also shows there is no "batch" to test against -- the run
    loop folds each decide's events into `world` before the next decide
    runs, so same-t and later-t are the same case at decide time.

(C) CHECK-SIDE. Ratify resurrect-then-inherit and relax the invariant.
    **Rejected, and the trace is why**: nothing in ADT makes a cancelled
    transfer un-discharge a patient. The A12 says the transfer did not
    happen; it does not say the discharge did not. No citation offered,
    so per the channel's own instruction it is dropped rather than
    argued.

## 3. The census the trace bought

The same probe counts every cancel decide in the run, by kind and by the
subject's status at decide time. This is the table the ruling turns on,
and it is why option (A) as the channel lettered it could not be taken.

`nobed` 10^5 (129,407 events, seed 20260824):

| kind | subject's status at decide time | count |
| --- | --- | --- |
| `:cancel-transfer` | `:admitted` | 780 |
| `:cancel-transfer` | **`:discharged`** | **61** |
| `:cancel-transfer` | `:new` (after a `:cancel-admit`) | 2 |
| `:cancel-discharge` | `:discharged` | 55 |

`v2` 10^4 (16,322 events, same seed), for contrast -- the defect is
absent at a decade down and the legitimate shape is not:

| kind | subject's status at decide time | count |
| --- | --- | --- |
| `:cancel-transfer` | `:admitted` | 63 |
| `:cancel-discharge` | `:discharged` | 6 |

Of the 61 superseded-subject cancel-transfers at `nobed` 10^5, **52 were
APPLIED** (the bed they reinstate was empty, so `bed-reoccupied-by-
someone-else?` passed them) and 9 were already being rejected as
`-bed-reoccupied`. The 52 land on 52 distinct patients -- and **all 11
patients the `outpatient-patients-occupy-no-bed` row flags at this cell
are among them**, which is what makes this row the whole of that row's
remaining root rather than part of it.

`:expired` and `:merged` are ZERO-FREQUENCY here, in both cells. Both
arms of the guard are nonetheless present and both carry an AUTHORED
unit witness, for the reason TS-1's own authored witness was accepted a
day earlier: there is nothing to sample, and the alternative is a guard
whose stated law is narrower than the law it means.

## 4. The fix, and the draw analysis the fence asks for

`decide :cancel-transfer` and `decide :cancel-discharge` each gain ONE
guard, asked BEFORE the bed:

    (subject-superseded? patient :cancel-transfer)
    -> (rejected-outcome :illegal-cancel-transfer-subject-superseded ...)

`subject-superseded?` is two small tables and a comparison:

    statuses-that-supersede-a-reinstatement  #{:discharged :expired :merged}
    status-a-cancel-target-leaves            {:cancel-transfer  :admitted
                                              :cancel-discharge :discharged}

    superseded? = (and (superseding status) (not= status (target-leaves kind)))

The second table is the whole design. It is what keeps the guard from
rejecting the 55 legal cancel-discharges the census counts, and it is
derived from what the cancelled event DOES rather than hardcoded per
method: a `:transfer` leaves its subject `:admitted`, a `:discharge`
leaves them `:discharged`.

`:new` is deliberately outside the superseding set -- see section 11,
judgment call 2.

### DRAWS: zero on both paths, and the comparison is executable

Neither cancel decide draws at all. Both methods bind their stream map
as `_streams` and never touch it; `rejected-outcome`'s own docstring
records the same fact for the rejected path ("No RNG is drawn here").
So the rejected path consumes exactly the draws the applied path
consumes -- ZERO, the same zero -- and the fence is satisfied at the
step. The claim is not left as a reading of the source:
`a-superseded-cancel-consumes-exactly-the-draws-the-applied-path-consumes`
drives BOTH paths with a `java.util.Random` of a fixed seed and asserts
the next draw equals a pristine `Random`'s first draw in each case, so a
draw added to either path in future reddens it.

What the fix DOES change downstream is world state, not draw counts: a
bed that used to be silently held for the rest of the run is now free,
which other patients' decides can legitimately see. That is the
correction itself, and it is what a declared sweep declares.

### ORDERING, disclosed

The subject guard is asked before `bed-reoccupied-by-someone-else?`.
At `nobed` 10^5 that moves 9 of the 61 superseded-subject
cancel-transfers from `:illegal-cancel-transfer-bed-reoccupied` to the
new reason. Neither the world nor the draws differ for those 9 -- both
paths reject -- only the reason the log carries. The order is chosen so
that the recorded reason names why the step could never have happened
at all, rather than the second thing that would also have stopped it.

## 5. Red before green

The src half was `git stash`ed and the whole `sim-engine` brick run
against the unfixed engine with the new tests in place. **15 failures
across all five new gates**, counted and not filtered:

| test | failures |
| --- | --- |
| `a-cancel-transfer-may-not-reinstate-a-discharged-subject` | 5 |
| `a-cancel-transfer-may-not-reinstate-an-expired-subject` | 4 |
| `a-cancel-discharge-may-not-reinstate-an-expired-subject` | 4 |
| `a-superseded-cancel-consumes-exactly-the-draws-the-applied-path-consumes` | 1 |
| `a-cancel-may-not-reinstate-state-a-later-event-superseded` (population) | 1 |

The population gate's own red, at the row's own seed:

    FAIL in (a-cancel-may-not-reinstate-state-a-later-event-superseded)
    expected: (empty? holders)
      actual: (not (empty? ({:at 37920, :patient "PID-000012-57734faa", :bed "ED-18"}
                            {:at 38040, ...} ... )))

`a-cancel-transfer-against-a-cancel-admitted-subject-is-still-applied`
passed on BOTH sides, which is the point of it: it pins behaviour this
change deliberately leaves alone, so it must be green before and after.

Green after `stash pop`: **sim-engine brick, 0 failures, 0 errors**.

The fixture's own traffic, measured rather than assumed: 691 events, 77
cancel-transfers still APPLIED, 12 rejected `-subject-superseded`, 2
rejected `-bed-reoccupied`. A gate that rejected everything would be as
useless as one that rejected nothing, and those 77 are what says this
one does not.

## 6. The bracket, and the per-corpus count that explains it

    bin/ground-truth-bracket a4e8698 c5e5f2b

    --- coverage: 38 roots carry :ground-truth and are digested;
        3 skipped (no such key): appendicitis.edn, ear-infections.edn,
        sore-throat.edn ---
    --- declared-digest-change: no (soundness: yes outside the leading
        docstring) ---
    IDENTICAL: every digested root's :ground-truth matches between
    a4e8698 and HEAD (38 roots)

**IDENTICAL, so NO declaration is owed and NOTHING was re-pinned.** The
prompt asks for the per-corpus pattern count that explains that, and the
same probe took it -- every cancel DECIDE in every churn-carrying gated
corpus, by subject status:

| gated corpus | events | cancel decides | superseded-subject |
| --- | --- | --- | --- |
| `seed-202-ed-tuesday` | 1,213 | 1 (`:cancel-discharge`, subject `:discharged`) | 0 illegal |
| `seed-424242-clinic-decade` | 1,774 | 0 | 0 |
| `seed-5-clinic-decade` | 1,412 | 0 | 0 |
| `adhd-seed-45` | -- | 0 by construction: its opts carry no `:churn` key at all | 0 |

The one cancel decide in the whole gated population is a LEGAL
cancel-discharge -- its subject is `:discharged`, which is the status a
cancel-discharge exists to find -- and it is exactly the case option (A)
as lettered would have rejected.

**AND THE APPARENT CONTRADICTION IS RESOLVED, not left standing.**
`run_test.clj`'s own counted witness says seed-202 carries EIGHT
reinstating cancels, which sits oddly beside "one cancel decide". Both
are true and the fixture says why: of seed-202's 8, **7 are
`:cancel-transfer` events carrying `:in-error true`** -- counted
straight out of the committed baseline -- and those are products of
`decide :transfer-in-error`, which builds its own cancel off the live
patient and NEVER routes through `decide :cancel-transfer`. The 8th is
the `:cancel-discharge` above. So the guard has literally nothing to
touch in any shipped corpus, and `:transfer-in-error` needs no guard of
its own: it emits its transfer and that transfer's cancel at ONE
instant, so no later event can have superseded anything in between.

`make docsgen` is the independent second reading and agrees: every
generated artifact is byte-identical except `docs/formats.md`, which
moved for the two new reason keywords alone. `demos/traces/**` -- which
includes a `--churn` module-mix run -- did not move.

## 7. The cells

Same driver, same scratch, same protocol and same preamble as the close
and as the TS-1..TS-4 session (`spike/driver2.clj`, six vars rebound
around one real `run-command` call; seed 20260824; warm-up plus two
timed runs, one JVM per run, `/usr/bin/time -v` around each; figures are
the mean of the two timed runs). HEAD `c5e5f2b`, tree clean. Health
record in section 10.

### 7.1 Both cells stay BLOCKED, and the fence is why

Neither cell self-checks clean, so under the prompt's own fence NEITHER
converts to MEASURED and the plan appendix's BLOCKED entries STAY
BLOCKED -- with new, much smaller reasons, which is the honest report
and is not the same as progress being absent.

| cell | events | persons | generate | check | emit | spool | wall | peak RSS | self-check |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `nobed` 10^5 | 129,415 | 39.14 s | 129.12 s | 12.88 s | -- | -- | 188.63 s | 1,677 MB | **BLOCKED (1 violation)** |
| `v2` 10^5 | 171,835 | 40.26 s | 162.68 s | (70.24 s) | -- | -- | 282.02 s | 1,703 MB | **BLOCKED (33,952)** |

Event counts are recovered by the untimed probe, since a blocked run
discards its payload.

Per-run walls: nobed 186.74 / 190.52 s; v2 287.12 / 276.92 s.

### 7.2 What the fix did to the violation census

| invariant | nobed 10^5 pre-fix | nobed post-fix | v2 10^5 pre-fix | v2 post-fix |
| --- | --- | --- | --- | --- |
| `outpatient-patients-occupy-no-bed` | 372,123 (11 patients) | **0** | 495,205 (12 patients) | **33,950 (1 patient)** |
| `admission-only-when-no-open-encounter` | 0 | 0 | 1 (TS-3) | 1 (TS-3) |
| `every-placeholder-registration-is-resolved-or-still-open` | 1 (TS-4) | 1 (TS-4) | 1 (TS-4) | 1 (TS-4) |
| **total** | 372,124 | **1** | 495,207 | **33,952** |

**At `nobed` the row's invariant is GONE**, all 11 patients cleared, and
one violation remains in a 129,407-event run. **At `v2` eleven of the
twelve are cleared** and the entire 33,950-violation residue belongs to
ONE patient, `PID-000640-f57cb996` -- TS-3's, for TS-3's reason (section
8). The two witnesses the row names, `PID-004302-fa1ab125` and
`PID-005562-03ed543c`, appear in neither cell's violation list.

### 7.3 THE ROW'S OWN CLAIM, CORRECTED

`roadmap.md#cancel-transfer-reinstates-a-discharged-patient` said it
"BLOCKS both 10^5 v2 cells, alone". **It did not.** TS-4 blocks the
`nobed` cell on its own and would have with or without this fix, and
TS-3 blocks the `v2` cell on its own. The claim was a reasonable reading
when written -- TS-5 owned 99.99% of the violation MASS -- and mass is
not what blocks a cell. One violation blocks it exactly as hard as
372,123 do. Recorded here rather than quietly dropped, because it is the
third instance this arc of a count standing in for a condition.

### 7.4 Phase figures, and which of them are measurements

Generate at `v2` 10^5 is **162.68 s against 163.05 s** at the previous
session's blocked cell -- **-0.2%**, unmoved, which is what a guard that
consumes no draws and rejects 0.03% of steps should do. Persons is
40.26 s against 39.93 s.

**THE EVENT COUNTS MOVE, AND THIS SESSION'S FIRST DRAFT SAID THEY DID
NOT.** The reasoning behind that draft was that a rejected cancel emits
one `:step-rejected` in place of one `:cancel-transfer`, so the
substitution is 1:1 -- which is true, and which is exactly why it is
not the whole story. Measured:

| cell | pre-fix | post-fix | delta |
| --- | --- | --- | --- |
| `nobed` 10^5 | 129,407 | **129,415** | **+8** |
| `v2` 10^5 | 171,913 | **171,835** | **-78** |

The substitution accounts for NONE of that, and the two cells move in
OPPOSITE directions, which is the tell: the delta is downstream. A bed
that used to be held for the rest of the run by a resurrected patient is
now free, and a free bed gets allocated -- each allocation emitting its
own events, or failing to, depending on who is waiting. Not decomposed
further than that here, and named as undecomposed rather than explained
away.

The cancel-decide censuses show the same thing from the other side. At
`nobed` the census is IDENTICAL to pre-fix -- 780 `:admitted`, 61
`:discharged`, 2 `:new`, 55 cancel-discharges -- so the same steps were
decided against the same subjects at the same instants and only their
OUTCOME changed, which is the draw analysis confirmed at population
scale. At `v2` it moved: **783/60/2/55 against 780/61/2/55**. One
cancel-transfer that used to find its subject `:discharged` now finds
them `:admitted`, because the freed beds changed what happened upstream
of it. Churn insertion is static (per arrival, off the patient's own
stream), so the SET of inserted cancel steps cannot move; what moved is
the world those steps met. That is precisely the downstream effect a
declared sweep exists to cover -- and no shipped corpus reaches it,
which is why none was owed.

**The `nobed` check figure is a real measurement and the `v2` one is
not.** At `nobed`, `check-all` now materialises ONE violation, so 12.88 s
is the wall of an essentially-clean check at 129,407 events -- the first
such figure this arc has had at 10^5. At `v2` it materialises 33,950, so
70.24 s is parenthesised for the same reason the previous session
parenthesised 84.84 s and the close parenthesised 88.81 s. That the
number fell is a fact about how many maps there were to build.

**Emit and spool still never run at 10^5**, on either corpus, because
both cells fail their self-check before emission. So **msg/event at 10^5
remains unmeasured on any add-on-bearing corpus** -- the gap the close
named, still open, now two sessions on. The v2 series' msg/event stands
at 1.050 (10^3) and 1.2169 (10^4) and gains no third decade here; stated
rather than extrapolated.

### 7.5 Exponents, over the v2 series

Re-derived where the phases actually complete:

| series | phase | 10^3 -> 10^4 | 10^4 -> 10^5 |
| --- | --- | --- | --- |
| v2 | persons | 0.734 | **1.107** |
| v2 | generate | 0.885 | **1.656** |
| v2 | check | 0.771 | (1.534) |

The first-decade column is the previous session's and is
startup-contaminated, printed for continuity only; its source is
`.agents/session-records/2026-08-29-ts-defects-and-blocked-cells.md`
section 3.4, not re-measured here. **Generate's second-decade exponent
is 1.656 against 1.620 and the close's 1.635** -- the same super-linear
shape across three independent measurements now, on a corpus of
identical size. Persons stays near-linear at 1.107 against 1.079. The
check exponent is parenthesised for 7.4's reason and no claim is made
about it.

### 7.6 The 10^6 decision, re-stated with the new arithmetic

**STILL DECLINED, and it keeps its PROJECTED label and note.** The
close declined 10^6 on EMIT peak heap, not on the log, and this
session's cells cannot improve that projection for the simple reason
that **emit never ran**: `:emit-peak-heap-mb` is 0.0 in all four timed
runs because the phase was skipped. The arithmetic that would license
10^6 is therefore exactly as unmeasured as it was, and taking 10^6 on
the strength of a generate-phase heap that did shrink would be reasoning
from the half that was never the constraint.

What the new figures DO say, recorded for whoever gets to take it: the
generate phase's own peak heap is 703.5 MB (`nobed`) and 707.6 MB (`v2`)
against a 3.88 GB ceiling, and check peaks at 1,081.6 / 1,246.6 MB while
building 1 and 33,950 violations respectively. A 10^6 run whose
self-check is CLEAN would carry a check-phase heap far below the `v2`
figure here, since the violation vector is what dominates it. None of
that reaches emit, which is the gate.

## 8. TS-3 and TS-4, re-run after the fix

The prompt's condition: each closes ONLY with its mechanism traced,
else stays open re-scoped. **Neither closes. Both are re-scoped, and
both re-scopings are measurements rather than readings.**

### TS-3 is NOT TS-5-rooted, and the fix is what proves it

`roadmap.md#ts-3-outpatient-opens-over-an-encounter` was rowed as "same
FAMILY as TS-5 -- a cancel reinstating state a later event superseded --
and it should be weighed with it". Weighed, and the verdict is that the
family resemblance is real but the ROOT is not shared. The guard landed;
TS-3 reproduces UNCHANGED at v2 10^5 -- same patient
(`PID-000640-f57cb996`), same instant (t=100609860), same invariant.

Why it survives is the guard's own asymmetry, and it is worth stating
because it is the design decision meeting its consequence: TS-3's
reinstating event is a `:cancel-discharge` whose subject is
`:discharged`, which is the one status a cancel-discharge may legally
find. The guard is index-relative precisely so that such a cancel stays
legal. TS-3's defect is one layer away and where the prior session put
it: a module-COMPILED encounter opener is queued directly and never
routed through `:repeat-arrival`, so `encounter-openable?` is never
asked. Nothing this session did could reach that, and nothing it did
should have.

**RE-SCOPED, and upward.** TS-3 is no longer a one-violation curiosity
riding the v2 cell. It is now the LARGEST remaining blocker of that
cell: `PID-000640` alone produces all **33,950** surviving
`outpatient-patients-occupy-no-bed` violations, because the second
opener flips `:class` to `:outpatient` on a patient still holding the
bed their inpatient encounter was given. The prior session called those
violations "legitimate" for this patient and was right; what it could
not see is that after TS-5 they are the WHOLE of that invariant's
residue at v2.

### TS-4 unchanged, and now the sole blocker of the nobed cell

`PID-007500-e98926c1` at t=37017, in BOTH cells, exactly as
characterised. TS-5 does not touch it: the consuming event is a churn
`:merge`, not a cancel. Its design question -- engine's defect (churn
must not eat an open-window placeholder) or check's (any merge resolves
an identity) -- is untouched by this session and remains genuinely open.

**RE-SCOPED.** At `nobed` 10^5 it is now the ONLY violation in a
129,407-event run, and therefore the only thing standing between that
cell and a MEASURED entry.

## 9. What landed

| commit | what |
| --- | --- |
| `c5e5f2b` | the guard in both reinstating cancel decides, the two rejection reasons, five gates, contract 1.7.0 -> 1.8.0 with export and baseline re-frozen, `churn/applicable?`'s corrected docstring, the event-validity table row, `docs/formats.md` |
| the commit this record rides in | the roadmap (row closed by sha, TS-3 and TS-4 re-scoped), the plan appendix's post-TS-5 entry and re-stated 10^6 decision, the regenerated `state-derived.md` and both INDEXes, this record and its prompt archive |

A docs commit cannot name its own sha, so this row names itself by
description. `make integration` and CI green at the pushed tip are
recorded by this commit's own successors, which is this repository's
standing pattern (`7500c75` and `a4e8698` are the previous session's
pair).

## What re-pinned, and nothing outside this list

**Nothing.** The bracket is IDENTICAL, `make test` is green with no
fixture, digest, count or roster pin touched, and `make docsgen` moved
exactly one generated file -- `docs/formats.md` -- for the two new
reason keywords alone. `demos/traces/**` (which includes a `--churn`
run), `.agents/state-derived.md`, `event-examples.edn`, both v2
conformance baselines and all four gated fixtures are byte-identical.

## Gates run, with exit codes

    make test        MAKE_EXIT=0   0 failures, 15 min 33 s
    make docsgen     DOCSGEN_EXIT=0
    make integration recorded by this commit's successor (clean-tree rule)
    bin/ground-truth-bracket a4e8698 c5e5f2b   IDENTICAL (38 roots), exit 0
    clojure -M:poly check   OK

## 10. Health record

Taken at session start and again at each cell boundary.

    $ date -Is                        -> 2026-08-29T10:35:01-04:00
    $ git log --oneline -1            -> c5e5f2b
    $ git status --porcelain          -> empty
    $ git rev-parse --abbrev-ref HEAD -> main

    $ uptime -s   -> 2026-08-24 15:10:46   -- the SAME boot the close, the
                     TS-1..TS-4 session and ADR-0167's post-reboot baseline
                     all ran on
    $ uptime      -> up 4 days 19:24, load average 1.83 1.90 1.62
    $ free -h     -> 15Gi total, 2.6Gi used, 12Gi avail
    $ df -hT .    -> /dev/sdd ext4 251G, 28% used   (NOT /mnt/c)
    $ nproc       -> 12

    $ java -version
    openjdk version "21.0.7" 2025-04-15
    OpenJDK Runtime Environment (build 21.0.7+6-Ubuntu-0ubuntu120.04)
    $ readlink -f $(which java) -> /usr/lib/jvm/java-21-openjdk-amd64/bin/java
    $ MaxHeapSize = 4162846720  ->  3.88 GB  {ergonomic}

    Windows side, sampled BEFORE the first cell and at every boundary:
      LoadPercentage             -> 1, 4, 12, 0
      powercfg /getactivescheme  -> High performance
      Win32_Battery              -> BatteryStatus 2 (AC)

Same boot, same JVM, same heap ceiling and same disk as the close and as
the previous session, so all three sessions' figures are on the same
machine in the same state. The boundary series **1, 4, 12, 0** sits
inside the close's own 0-17 in-flight range, and the 1 was taken BEFORE
the first cell started -- the discipline the previous session's
discarded 1.7x measurement bought.

## 11. Judgment calls, and their ratification status

None is ratified; each is disclosed rather than folded in.

1. **The channel's option (A) was NOT taken as lettered, and the
   correction is the session's main finding.** (A) says reject when the
   subject is `:discharged`/`:expired`/`:merged`. Applied literally it
   rejects EVERY `:cancel-discharge` in the repository -- 55 of 55 at
   `nobed` 10^5, 6 of 6 at `v2` 10^4 -- because a cancel-discharge
   exists to find its subject `:discharged`. The landed guard is (A')
   -- the same rule taken relative to the status the cancelled event
   itself leaves behind. This is recorded as channel-inferred [C]
   rather than escalated because it does not change ground-truth
   semantics beyond the defective pattern: it narrows (A) to exactly
   the cases (A) meant. The counter-argument, stated fairly: the
   channel asked for a specific guard and got a different one, and a
   reader who trusts the letter of the prompt over its intent would
   call that a deviation owed a STOP.

2. **`:new` is left OUT of the superseding set, so 2 measured
   cancel-transfers against a cancel-admitted subject still apply.**
   The argument for including it is real -- a `:cancel-admit` has
   already removed the patient's location, so reinstating a transfer's
   location onto them is the same incoherence one step removed. It is
   excluded because doing otherwise re-rules a case a prior session
   decided deliberately (M6 Task 2, whose test asserts the
   cancel-admit-then-cancel-discharge sequence works) and because that
   re-ruling is not what this row asked for. Named here as an adjacent
   case rather than rowed, per the de-scaffold ruling.

3. **The two witness PIDs are asserted in this RECORD, not in the
   suite.** The prompt asks for their traces asserted clean. A 129,407
   -event run is the close's own measurement cell and cannot live in
   `make test`; what the suite carries is the population fixture at the
   same seed, plus four hand-built cases. The PIDs' own post-fix state
   is measured in section 5 instead.

4. **The guard's `:expired` and `:merged` arms are ZERO-FREQUENCY in
   every corpus measured**, and carry authored unit witnesses instead
   of sampled ones -- the same disposition TS-1's authored witness took
   one day earlier, for the same reason: there is nothing to sample,
   and the alternative is a guard whose stated law is narrower than the
   law it means. `:merged` additionally cannot be reached through
   `run`, which ends a merged patient's queue before their next step is
   decided; it is named for a hand-driven `decide` and for the reader
   who would otherwise have to prove the omission safe.

5. **`clojure -M:poly check` passed over a file that does not compile.**
   An unescaped quote inside a docstring in `churn.clj` produced a
   `defn-` that fails to macroexpand; `poly check` reported OK and the
   suite caught it one target later. This is the SECOND session running
   to record that finding (arc 4 sweep 5 recorded the first). It cost
   one full suite run.
