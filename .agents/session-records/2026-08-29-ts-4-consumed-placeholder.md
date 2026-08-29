# TS-4: a churn merge consumes an open placeholder

Session record, 2026-08-29. HEAD at start `23901f4`; ceremony R30
(commit and push at each checkpoint, unattended), taken from the prompt.

## 0. What this session did

`roadmap.md#ts-4-placeholder-unresolved` was reproduced at its own
patient in BOTH 10^5 add-on cells, its mechanism was measured to all
four halves the prompt named, the design question was lettered with a
recommendation, and **option (A) landed** -- check-side only, with all
three riders. **TS-4 is CLOSED**, and with it the last blocker on the
traffic-scale programme's two 10^5 cells.

Five findings, in the order they matter:

1. **The row is right in every particular this session could check.**
   Two prior rows were wrong in theirs; this one is not. Patient,
   instants, `:window-close-t`, the missing `:cause`, and the "can never
   be resolved afterwards" claim all reproduce exactly, at both cells.
2. **What churn destroyed is a FILL, not an identification merge.** The
   resolution seeded on this placeholder is `{:branch :fill,
   :survivor-patient-id nil}` -- `PERSON-014364` had no prior identified
   patient, so there was nothing to merge into. The row's phrasing
   ("can never be filled or identification-merged") is right; the thing
   actually lost is one `:identity-fill`.
3. **"Can never" is now MEASURED rather than reasoned.** 1,062
   resolution steps are seeded and 1,061 are decided, at both cells, and
   the one that never runs is this patient's. The run loop's `:merged`
   short-circuit swallows the queue entry, so nothing is minted after
   the merge -- **the second latent defect does not fire in either
   10^5 log.** It is real in the code all the same, and section 2.4 is
   where that is measured and section 5 is the gate that makes it
   permanent.
4. **THE SOURCE CARRIES AN ARGUMENT THAT THIS PATIENT REFUTES.**
   `decide :identification-merge`'s docstring says churn's lottery needs
   no change because "`decide :merge`'s own `never-mergeable?` excludes
   `:new`, and a placeholder patient who registered and was never
   admitted is exactly `:new`." True only of placeholders that are never
   admitted -- and the `:identity-unavailable` hook mints an
   unidentified ARRIVAL, which the same hook then admits. This one was
   admitted as "Unidentified patient" into ED-98 at its own registration
   instant and discharged 43,200 seconds later, so it carried
   `:status :discharged` into the lottery and `never-mergeable?` could
   not have excluded it. The class is structural, not a knife edge.
5. **Option (B) is disqualified by its own draw analysis, and NOT for
   the reason the letter predicted.** The prompt expected "every churn
   corpus reshuffles". Measured: **no shipped corpus reshuffles at all**
   -- in all 44 gated and bracketed corpora, zero churn merges have an
   open-window placeholder among their candidates. What (B) would move
   is the SCRATCH traffic-scale cells, at **694 of 747 churn merges**
   (`nobed`) and **697 of 750** (`v2`), because `uniform-choice` is
   `(.nextInt rng (count candidates))` and (B) changes that count. Four
   sessions of measured series would be invalidated to fix one violation
   the corpus is right to contain.

## Fences honoured

- **No engine change under option (A).** `git diff --stat` touches
  `components/sim-check/src` and three test files, and no file under
  `components/sim-engine/src`. Bracket-enforced in section 6.
- **No churn lottery change.** None was made; the draw analysis option
  (B) owed was run anyway (section 4.2), because a rejection is worth
  more with the number behind it.
- **A disappearing violation is not a fixed one.** The violation does
  not disappear -- it is RECLASSIFIED, into a column that is counted
  (`placeholder-dispositions`, section 5.3) and gated for non-vacuity.
  The red-before-green proof of section 5.4 runs the PRE-change law and
  the landed one over the same log, in one process.
- **F3 absolute.** The 10^6 decision keeps its label and its note;
  section 8.

## 1. The reproduction

`spike.probe-ts4`, a new scratch probe on the surviving traffic-scale
scratch (recovered whole from the TS-3 session's own scratchpad; the
`dense-7500*.edn` bytes are that session's, unmodified), at seed
20260824, HEAD `23901f4`. UNTIMED: it wraps `engine/run`, the private
`prelude` and the `decide` VAR and runs `check-all` a second time, so
its wall measures nothing and no health record is taken for it.

| | events | placeholders minted | with no `:window-close-t` | resolutions seeded | resolution decides that RAN | churn merges | consuming an OPEN placeholder | TS-4 violations |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `nobed` 10^5 | 129,415 | 1,063 | 1 | 1,062 (`{:fill 920 :merge 142}`) | **1,061** | 747 | **1** | **1** |
| `v2` 10^5 | 171,864 | 1,063 | 1 | 1,062 (`{:fill 920 :merge 142}`) | **1,061** | 750 | **1** | **1** |

Both event counts are the roadmap's own post-`c156690` figures to the
event, so the reproduction is byte-for-byte the corpora the last session
measured. `CENSUS-INVARIANTS` is `{:every-placeholder-registration-is-
resolved-or-still-open 1}` at each, naming `PID-007500-e98926c1` and
nothing else -- the row's "one violation in EACH cell, same patient,
same instants, blocks both cells alone" reproduces exactly.

## 2. The mechanism, to all four halves

### 2.1 The placeholder's full event family

Four events, and they are the whole of this patient's log (`nobed`
indices; the `v2` indices differ, the instants do not):

    2562  t=37017   :registered  :identity :placeholder
                                 :alias-name {:family "Doe" :given "Unknown"}
                                 :window-close-t 382617
                    before {:status :new,       :identity nil}
                    after  {:status :new,       :identity :placeholder}
    2563  t=37017   :admission   "Unidentified patient", Emergency, bed ED-98
                                 ENC-007500-00-ec4ca236
                                 :person-event-id "PERSON-014364#1"
                    after  {:status :admitted,  :identity :placeholder}
    6807  t=80217   :discharge   ENC-007500-00-ec4ca236
                    after  {:status :discharged, :identity :placeholder}
    15321 t=177420  :merge       [PID-002543-da5721e3 :survivor]
                                 [PID-007500-e98926c1 :merged]
                                 :surviving-mrn MRN002544 :merged-mrn MRN007501
                                 -- NO :cause
                    after  {:status :merged,    :identity :placeholder}

And the seeding pass behind them, read out of `prelude` rather than off
the log:

    placeholder {:patient-id "PID-007500-e98926c1" :ordinal 7500
                 :person-id "PERSON-014364"
                 :window {:event-id "PERSON-014364#1" :t 37017 :until-t 382617
                          :alias-name {:family "Doe" :given "Unknown"}
                          :branch :fill :resolution-t 382617
                          :resolution-event-id "PERSON-014364#2"}}
    resolution  {... :branch :fill :survivor-patient-id nil}

**The branch is `:fill`.** The row says the placeholder "can never be
filled or identification-merged", which is true; what was actually
seeded and then destroyed is one `:identity-fill` at t=382617. Finding
2 of section 0.

### 2.2 The churn merge's selection path

    {:t 177420 :survivor "PID-002543-da5721e3" :target "PID-007500-e98926c1"
     :with nil :eligible 666 :window-close-t 382617 :open? true :branch :fill
     :survivor-state {:status :admitted :class :inpatient
                      :location {:ward "Medicine A" :bed "MEDICINE-A-131"}
                      :active-mrn "MRN002544" :identity :known
                      :name {:family "Chen" :given "Richard"}}
     :target-state   {:status :discharged :class :inpatient :location nil
                      :active-mrn "MRN007501" :identity :placeholder
                      :name {:family "Doe" :given "Unknown"}}}

`:with` is nil, so the target was DRAWN, not named: one
`(.nextInt world-rng 666)` out of `decide :merge`'s `uniform-choice`.
The survivor is an ordinary admitted inpatient with a known identity.

**Why `never-mergeable?` did not exclude it.** That predicate is
`#{:new :merged}`, and the John Doe carried `:status :discharged` --
because the `:identity-unavailable` hook that minted the placeholder
minted an ARRIVAL, and `decide :person-encounter` admitted it at the
same instant. Finding 4 of section 0: the argument in
`decide :identification-merge`'s docstring holds for a placeholder that
registers and is never admitted, and every hook-minted placeholder is
admitted by construction.

### 2.3 Why the invariant reads it unresolved

`every-placeholder-registration-is-resolved-or-still-open` (at
`23901f4`, `check.clj:1316`) builds `resolved` from exactly two shapes
-- `:demographic-update` with `:cause :identity-fill`, and `:merge` with
`:cause :identification`. `decide :merge` emits neither: it writes
`:role :merged` on its participant and no `:cause` at all. So the
placeholder is not in `resolved`, its `:window-close-t` 382617 is well
inside the run, and it goes red -- correctly, on the law as written.

### 2.4 The second latent defect: it does NOT fire, and it IS real

**It does not fire.** 1,062 resolution steps are seeded and 1,061 are
decided, at both cells. Every one that ran carried `:status :discharged`
into its decide (`:resolution-decide-identity-fill-discharged 919`,
`:resolution-decide-identification-merge-discharged 142`, summing to
1,061). The missing one is `PID-007500`'s `:identity-fill` at t=382617,
and it is missing because `run`'s loop short-circuits a queue entry
whose patient is already `:merged` (`engine.clj:4735`) -- the entry is
popped and dropped without a decide. Nothing is minted after the merge,
in either 10^5 log.

**It is real all the same, on the unit path.** `identity-fill-outcome`
(`engine.clj:1506`) refuses only when the patient is missing or when
`(:identity (:demographics patient))` is no longer `:placeholder`, and
`evolve :merge` (`engine.clj:2851`) sets `:status :merged` while leaving
the demographics untouched -- the trace's own `after` line shows
`{:status :merged, :identity :placeholder, :name Doe/Unknown}`. So a
consumed placeholder still looks fillable to that decide, and
`decide :identification-merge` has the same gap from the other side: it
guards the SURVIVOR's status (`#{:merged :expired}`) and never the
placeholder's own. One `if` in the run loop is the whole defence, and
nothing asserted it from outside until section 5.2.

## 3. The pattern, per corpus -- can any SHIPPED corpus produce it?

**No.** Two censuses, both over finished logs
(`spike/src/spike/census_ts4.clj`, a pure log predicate needing no
instrumentation).

### 3.1 The bracket's own population -- 38 roots

`ehrt.oracle.digest` run whole into a scratch directory, then scanned.
Every root not listed below has zero placeholders, zero merges and zero
of everything else in this table; the three interpreter-layer batch
roots carry no `:ground-truth` and are skipped, exactly as
`bin/ground-truth-bracket` skips them.

| root | events | placeholders | churn merges | identification merges | consumed OPEN | TS-4 violations |
| --- | --- | --- | --- | --- | --- | --- |
| `chatter-charges` | 477 | 10 | 0 | 2 | **0** | 0 |
| `demographic-fold` | 671 | 10 | 0 | 2 | **0** | 0 |
| `encounter-horizon` | 170 | 0 | 2 | 0 | **0** | 0 |
| `scheduling` | 487 | 0 | 1 | 0 | **0** | 0 |
| (34 others) | -- | 0 | 0 | 0 | **0** | 0 |

**Not one bracketed root has both a placeholder and a churn merge.** The
pattern is not merely absent from them; it is unreachable in them.

### 3.2 The gated and demo corpora -- six

The four `gated-runs` of `components/sim/test/ehrt/sim/run_test.clj`
plus the two demo scenarios the exercisers drive.

| corpus | events | placeholders | churn merges | identification merges | consumed OPEN | TS-4 violations |
| --- | --- | --- | --- | --- | --- | --- |
| `seed-202-ed-tuesday` | 1,213 | 8 | 2 | 1 | **0** | 0 |
| `seed-424242-clinic-decade` | 1,774 | 24 | 0 | 5 | **0** | 0 |
| `seed-5-clinic-decade` | 1,412 | 33 | 0 | 4 | **0** | 0 |
| `adhd-seed-45` | 97 | 1 | 0 | 0 | **0** | 0 |
| `demo-ed-tuesday` | 1,269 | 15 | 1 | 0 | **0** | 0 |
| `demo-clinic-decade` | 1,569 | 28 | 0 | 7 | **0** | 0 |

Two of these -- both ed-tuesday corpora -- have BOTH a placeholder and a
churn merge, so the shape is reachable here in a way it is not in the
bracket's roots. It still never happens.

**What this predicts, and what it costs.** The bracket must be
IDENTICAL, which a check-side fix guarantees anyway; and the tolerated
column is VACUOUS on all 44 corpora, which is why
`R-empty-population-is-red` forces the hand-built gate of section 5.3
rather than a population one. The only place in this repository that
produces the shape is the scratch traffic-scale cell, once.

## 4. The letter, and why option (A)

### 4.1 What the trace settles before the options are weighed

- The engine emitted a legal merge. `decide :merge` drew a legal target
  out of a 666-strong eligible set, wrote both roles, and moved on.
  Nothing in the log is malformed and nothing in `world` is
  inconsistent.
- The corpus is describing a real failure of a real system. An
  unidentified record absorbed into another patient's before anybody
  establishes whose it was is one of the characteristic ways a master
  patient index fails -- a false-positive merge over a John Doe, which
  is the case an MPI test suite most wants to be handed.
- What the check was asserting is therefore too strong. It required
  every placeholder to reach a resolution or outlive the feed, and there
  is a third honest ending: the record stops existing as an independent
  identity.

### 4.2 The draw analysis option (B) owed

`decide :merge` picks its target with
`(nth candidates (.nextInt rng (count candidates)))`. Option (B) --
skipping patients with an open identity window -- removes elements from
`candidates`, so it changes the ARGUMENT to `.nextInt`. That re-indexes
the draw wherever the set changes at all, and `java.util.Random.nextInt`
consumes a variable number of `next(31)` calls for a non-power-of-two
bound, so it can also move the `:world` stream position for every draw
after it. Measured, per churn merge, as "how many candidates would (B)
have removed":

| corpus | churn merges | with >= 1 open-window placeholder among candidates | total such candidacies |
| --- | --- | --- | --- |
| `seed-202-ed-tuesday` | 2 | **0** | 0 |
| `demo-ed-tuesday` | 1 | **0** | 0 |
| `nobed` 10^5 (scratch) | 747 | **694** | 2,323 |
| `v2` 10^5 (scratch) | 750 | **697** | 2,331 |

**The prompt's own prediction is corrected by this.** (B) would NOT
reshuffle every churn corpus -- it would leave every shipped one
byte-identical, and `bin/ground-truth-bracket` would come back
IDENTICAL under (B) as well. What it would reshuffle is the traffic-
scale scratch cells, at 93% of their churn merges: every event count,
every timing figure, the exponents and the msg/event series that four
sessions have accumulated would be measurements of a different corpus.
That is the disqualifier, and it is a bigger one than the bracket.

The semantic argument disqualifies (B) independently, and matters more:
teaching churn to route around open placeholders would make this
simulator STRUCTURALLY INCAPABLE of emitting a John Doe eaten by a bad
merge. The pattern would not be fixed, it would be censored.

### 4.3 The options as lettered, and the ruling

- **(A) CHECK-SIDE -- TAKEN.** The invariant becomes
  resolved-or-CONSUMED-or-still-open. All three riders landed; section 5.
- **(B) ENGINE-SIDE -- REJECTED**, on 4.2, and not on the bracket
  argument the letter proposed.
- **(C) both -- REJECTED**, as (B) is.

Taken as a channel-prior ruling under the prompt's step 2 ("small enough
to fix on the channel's recommendation IF the trace confirms the row").
The trace confirms it. Not ratified; disclosed here.

### 4.4 One refinement inside option (A), disclosed

The letter's option (A) says a merge naming the placeholder as `:merged`
closes its window **whatever the cause**. Landed with a TIME bound as
well: the consuming merge must land at or before the window's due close.
That is narrower than the option as written and deliberately so -- a
merge AFTER the due instant does not retroactively excuse a placeholder
that was already dangling when identification came due, and a clause
without the bound would go quiet on a log that really does show an
unresolved window. It is cause-blindness the option asked for, not
merge-blindness. TS-4's own merge is at 177420 against a close of
382617, so the bound does not touch the witness. Judgment call, not
ratified.

## 5. The law as landed, with its riders

Check-side only. `git diff --stat`: `components/sim-check/src/.../check.clj`
and three test files. Nothing under `components/sim-engine/src`.

### 5.1 The invariant (`check.clj:1374`)

*A placeholder registration either gets its fill or its identification
merge, or is CONSUMED -- absorbed whole by a merge that never claimed to
have identified anybody -- or the run ENDED before its window was due to
close.*

Three helpers were factored out beside it because three readers now need
the same sets: `placeholder-registrations` (`:1316`), `consuming-merges`
(`:1329`, carrying a LOG INDEX beside the instant, because two events at
one `:t` are ordered by the log and by nothing else) and
`identity-resolutions` (`:1358`). The docstring names the failure shape
-- *an erroneous merge eats a John Doe* -- states that the engine did
nothing wrong, carries the witness with its instants so a later reader
can see the shape rather than trust the paragraph, and cites the row.

### 5.2 The companion (`check.clj:1436`)

`no-resolution-after-a-placeholder-is-consumed`: once a merge has
absorbed a placeholder record, nothing fills it and nothing
identification-merges it. This is what makes 5.1 safe -- a fill landing
on a record that was merged away would be the log claiming to have
identified somebody whose record no longer exists, and the clause above
would have gone quiet on exactly the run where that happened. It is
section 2.4's latent engine defect made permanently visible from
outside the run loop.

**DELIBERATELY OVERLAPPING, and disclosed rather than quietly
duplicated.** `no-events-after-merged-terminal` forbids ANY later event
naming a merged patient, so on today's catalog it subsumes this one.
The two separate the day a merged patient is allowed any trailing event
at all, and 5.1 depends on THIS law specifically rather than on the
general rule that happens to imply it now. Said in the docstring, where
a reader comparing the two will look.

### 5.3 The counted column (`check.clj:1489`)

`placeholder-dispositions` classifies every placeholder in a log into
six disjoint classes summing to the total: `resolved-by-fill`,
`resolved-by-identification-merge`, **`consumed-by-churn`**,
`unjudgeable`, `still-open`, `dangling`. `consumed-by-churn` is the
column the new clause tolerates, and a tolerated shape that nothing
counts is indistinguishable from one that never happens -- which is how
a clause added for one witness quietly becomes a clause covering a
hundred.

### 5.4 Red before green

**At the row's cell, before a line of `src` moved.** The probe of
section 1 ran at `23901f4` and reported
`{:every-placeholder-registration-is-resolved-or-still-open 1}` at each
cell, naming `PID-007500-e98926c1`. That is the RED the row asserts, and
it is the whole of each cell's violation census.

**At unit scale, both laws in one process** (`spike/red-proof.clj`, so
the comparison cannot be a comparison of two runs):

    RED   old law on the TS-4 log : 1  [{:invariant :every-placeholder-...,
                                         :patient-id "PID-000000-aaaaaaaa", :at 0}]
    GREEN new law on the TS-4 log : 0
          new law's census        : {:consumed-by-churn 1, :dangling 0, :total 1, ...}
          companion on that log   : 0
    RED   old law, merge past close: 1
    RED   new law, merge past close: 1

The last two lines are the narrowness proof of section 4.4: move the
consuming merge past the due close and the new law is red exactly where
the old one was.

**GREEN at the cell.** The same probe re-run against the landed law, at
the identical corpora, reports `CENSUS-INVARIANTS {}` at both cells --
129,415 and 171,864 events, unchanged. Section 7 has the timed cells.

### 5.5 The gates

| gate | what it holds |
| --- | --- |
| `every-placeholder-registration-is-resolved-or-still-open-test` | +3 cases: the consumed shape is clean; a merge past the due close still fires; a merge naming the placeholder as SURVIVOR consumes nothing and still fires |
| `no-resolution-after-a-placeholder-is-consumed-test` | born with 6 cases, 3 of them FIRING mutations -- a fill after the merge, an identification merge after it, and a fill at the same `:t` but later in the log |
| `placeholder-dispositions-counts-every-class-test` | every one of the six columns has a witness (`R-empty-population-is-red` applied per column), the classes are disjoint and sum, and `:dangling` equals what the invariant reports |
| `nothing-is-consumed-on-a-real-identification-run-test` | the shape is absent from the real fold fixture, pinned so the day a shipped corpus starts producing it is a finding |
| `the-person-family-is-registered-in-the-catalog-test` | the seventh is in `catalog`, so `check-all` runs it |
| `arc0-invariant-catalog` (`run_test.clj:983`) | re-pinned 43 -> 44, with the standard disclosure: no FINDING moved, the joined row is vacuous on all four corpora |

## 6. The bracket

    bin/ground-truth-bracket 23901f4 62dd9b3
    --- coverage: 38 roots carry :ground-truth and are digested;
        3 skipped (no such key): appendicitis.edn, ear-infections.edn, sore-throat.edn ---
    --- declared-digest-change: no (soundness: yes outside the leading docstring) ---
    IDENTICAL: every digested root's :ground-truth matches between
               23901f4 and 62dd9b3 (38 roots)

**IDENTICAL over all 38, as a check-side fix must be.** The prompt's own
fence -- "any DIFFERS means the fix leaked into the engine, STOP" --
holds. No declaration was owed, no sweep was spent, and nothing was
re-pinned. This is NOT a regression-oracle claim; the script says so on
every run and the distinction is `rulings.md#R-oracle-script-contract`'s.

## 7. The cells -- BOTH MEASURED, at last

Same driver, same scratch, same protocol and same preamble as the TS-3
session (`spike/driver2.clj` via `cell2.sh`, seed 20260824, warm-up plus
two timed runs, one JVM per run, `/usr/bin/time -v` around each; figures
are the mean of the two timed runs). HEAD `62dd9b3`. Host sampled at
every cell boundary: **1, 2, 30** -- the 30 is the CLOSING sample, taken
after the last run had already finished, so no figure sits inside it.

| cell | events | messages | msg/event | modules | persons | generate | check | emit | spool | other | in-process | wall | peak RSS | self-check |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `nobed` 10^5 | 129,415 | 165,946 | **1.2823** | 0.333 s | 42.81 s | 135.44 s | 13.60 s | 17.62 s | 11.16 s | 2.68 s | 212.48 s | **232.67 s** | 1,745 MB | **CLEAN** |
| `v2` 10^5 | 171,864 | 233,286 | **1.3574** | 0.333 s | 39.10 s | 164.58 s | 17.65 s | 21.57 s | 15.09 s | 2.78 s | 246.01 s | **270.37 s** | 1,935 MB | **CLEAN** |

**Emit and spool run at 10^5 on an add-on corpus for the first time in
this programme's history**, three sessions after the gap was named. Both
event counts are unchanged from the blocked runs -- 129,415 and 171,864
-- which is the identity claim a check-side fix owes and gets for free.
Fan-out spooled 23,197 `:adt-feed` + 8,097 `:bed-feed` beside `nobed`'s
165,946 base messages, and 23,714 + 49,935 beside `v2`'s 233,286.

**The `v2` check figure is a REAL measurement for the first time.** Every
`v2` 10^5 check wall this programme has quoted was parenthesised, being
the wall of a failing check materialising violation maps (TS-6). 17.65 s
is a passing check over 171,864 events.

### 7.1 The completed `v2` series, 10^3 -> 10^4 -> 10^5

| | events | messages | **msg/event** | persons | generate | check | emit | spool | wall | peak RSS |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| 75 patients | 1,488 | 1,562 | **1.0497** | 0.543 s | 0.432 s | 0.324 s | 0.445 s | 0.222 s | 8.86 s | 339 MB |
| 750 patients | 16,322 | 19,862 | **1.2169** | 3.149 s | 3.596 s | 2.052 s | 2.693 s | 1.642 s | 20.74 s | 713 MB |
| 7,500 patients | 171,864 | 233,286 | **1.3574** | 39.10 s | 164.58 s | 17.65 s | 21.57 s | 15.09 s | 270.37 s | 1,935 MB |

**THE PROGRAMME'S NUMBER AT SCALE, four sessions owed and now paid:
messages per event on the full add-on corpus climbs 1.050 -> 1.217 ->
1.357 across two decades, and is STILL CLIMBING.** It is not a settled
ratio and must not be quoted as one. Against the pre-arc-4 skeleton's
0.643, flat across its own decade, the arc-4 payload is worth **2.11x
the message volume per event at 10^5**, up from 1.63x at 10^3. The
`nobed` isolation series says the same with the bed cycle removed:
1.056 -> 1.214 -> **1.2823**.

### 7.2 The exponents over the completed series

Log-log slope over MEASURED event counts. First-decade slopes are
startup-contaminated (the 10^3 cell's phases are hundreds of
milliseconds inside a JIT-ing JVM) and are printed for completeness
only, exactly as the close printed them.

| phase | 10^3 -> 10^4 | 10^4 -> 10^5 | 10^3 -> 10^5 |
| --- | --- | --- | --- |
| persons | 0.734 | **1.070** | 0.901 |
| generate | 0.885 | **1.624** | 1.251 |
| check | 0.771 | **0.914** | 0.842 |
| emit | 0.752 | **0.884** | 0.817 |
| spool | 0.835 | **0.942** | 0.888 |
| process wall | 0.355 | 1.091 | 0.720 |

Three things this table says that no previous one could.

1. **Generate's second-decade exponent is 1.624, and the fix did not
   move it** -- 1.635 pre-fix on the same config. A check-side change
   should be invisible in generate and is. `roadmap.md#performance-
   residual-sites` still owns the super-linearity.
2. **CHECK IS SUB-LINEAR, 0.914, and this is the first honest check
   exponent on an add-on corpus.** The close reported a parenthesised
   (1.610) and said in so many words that it was the cost of
   materialising 0.9M violation maps rather than a property of the check
   phase. It was: over a clean log the same phase runs at 0.914.
   **The programme's own instrument finding (TS-6) is now confirmed by
   measurement rather than argued.**
3. **Emit and spool are sub-linear at 0.884 and 0.942**, extending the
   `nobed` 10^3->10^4 evidence a decade and confirming that the
   rendering and writing phases are not where 10^6 will hurt.

## 8. The 10^6 decision, re-stated on the completed series

**DECLINED, and it keeps its label and its note (F3: nothing
extrapolated is promoted).** The close declined it on the `old` series,
the only one that completed; the arithmetic can now be done on the `v2`
series' own measured exponents, and it declines harder.

| | projection at ~1.7M events (one decade on the v2 1e4->1e5 exponents) |
| --- | --- |
| persons | 39.10 s x 10^1.070 = 460 s |
| generate | 164.58 s x 10^1.624 = **6,927 s (1 h 55 min)** |
| check | 17.65 s x 10^0.914 = 145 s |
| emit | 21.57 s x 10^0.884 = 165 s |
| spool | 15.09 s x 10^0.942 = 132 s |
| **one run** | **~7,830 s = 2 h 10 min** |
| warm-up + two timed | **~6.5 h** |
| retained after generate | 157.3 MB x 10 = 1.57 GB (inside the 3.88 GB ceiling) |
| peak heap, emit | 1,489 MB x 10 = **14.5 GB -- 3.7x the 3.88 GB ceiling** |
| peak process RSS | 1,935 MB x 10 = **18.9 GB -- above the machine's 15 GiB** |

**The close's central memory finding survives contact with a completed
series and gets sharper.** Its projection was built on the `old` config,
which carries none of the payload; the add-on corpus emits 233,286
messages where the skeleton emitted 67,638, and the emit phase's peak
heap projects to 14.5 GB rather than 9.87. **The binding constraint at
10^6 is still the message vector and not the event log** -- retained
after generate projects to 1.57 GB, comfortably inside the ceiling --
and the margin is now 3.7x rather than 2.5x. Nothing here is promoted:
both PROJECTED entries stay PROJECTED and the DECLINED decision keeps
its label.

Two figures worth stating because a reader will otherwise infer them
wrongly. **Generate at 1.9 hours would dominate a 10^6 run so completely
that the other four phases together are 12% of it**; and **the heap wall
is reached in EMIT, a phase that has never been the suspect** -- it was
the phase the close could not measure at all on an add-on corpus, which
is why this arithmetic could not be done until today.

## 9. Gates run

| gate | result |
| --- | --- |
| `make test` (`clojure -M:poly check` + full suite, skip integration) | **green**, `poly check` OK, 0 failures / 0 errors across 816 result lines |
| `make integration` | **green** on the second run. The FIRST run failed its own ADR-0005 clean-tree postcondition and nothing else -- every integration test passed and the four modified files were this session's own, uncommitted. Re-run after the commit: green, "every command asserted, both pairs matched, tree clean". Disclosed rather than reported as one green run. |
| `bin/ground-truth-bracket 23901f4 62dd9b3` | **IDENTICAL over 38 roots** |
| `bin/preflight` | exit 1, ONE finding: working tree not clean (this session's own work in flight, listed file by file). Last five CI runs on `main` all green; repo root not under `/mnt/`; `core.fileMode` true; local HEAD matched `origin/main` at `23901f4`. DISCLOSED: HEAD not tagged `stable-*` -- correct, no tag is paid (the tag law is retired). |
| both 10^5 cells, self-check | **CLEAN**, both |
| CI at the tip (`b4bc4ce`) | **success**, run 33275738522. Both commits went up in one push, so CI covers the tip; `make test` and `make integration` were run locally at `62dd9b3` as well and are recorded above. |

## 10. Scratch

**The scratch did NOT survive on penny this time**, and that is worth
recording because two prior records say it did. `~/scratch` holds three
commit messages from 2026-08-01 and nothing else; there is no `spike/`
anywhere under `$HOME`. It was recovered whole from the TS-3 session's
own agent scratchpad under `/tmp/claude-1000/`, which is where it had
actually been living all along -- the "survived on penny" of the close
and the TS-3 record is true of that directory and not of a durable one.
A future session should look there first and should not assume the
directory outlives a reboot.

Added this session, on top of the recovered TS-3 scratch:

| file | what |
| --- | --- |
| `spike/src/spike/probe_ts4.clj` | wraps `engine/run`, the PRIVATE `prelude` (via `ns-resolve` + `with-redefs-fn`, since the compiler refuses `#'` on it) and the `decide` VAR: the watched patient's decide trace and event stream with folded state either side, the seeding pass's own placeholder/resolution records, every churn merge's selection path, the resolution-decide census of section 2.4, and option (B)'s draw meter |
| `spike/ts4.sh` | classpath shim for it |
| `spike/src/spike/census_ts4.clj`, `spike/census-ts4.sh` | the per-corpus pattern census, `scan` (oracle roots) and `run` (live corpus) modes |
| `spike/corpora-ts4.sh` | the six gated/demo corpora, one line each |
| `spike/cells-ts4.sh` | the two 10^5 cells, health record either side |
| `spike/red-proof.clj` | the pre-change law and the landed one over one log, in one process |
| `spike/out/ts4-{nobed,v2}-1e5.log` | the traces of section 2, at `23901f4` |
| `spike/out/ts4-{nobed,v2}-1e5-draws.log` | the same, re-run with the draw meter and against the landed law (`CENSUS-INVARIANTS {}`) |
| `spike/out/ts4-roots-census.txt`, `ts4-corpora-census.txt` | section 3's two tables |
| `spike/out/bracket.log`, `cells-ts4.log` | sections 6 and 7 |

## 11. Judgment calls

None is ratified; each is disclosed.

1. **The time bound inside option (A).** Section 4.4. The letter's
   option (A) said "whatever the cause"; the clause landed cause-blind
   but not merge-blind. Narrower than what was lettered.
2. **The companion invariant is landed despite being subsumed.**
   `no-events-after-merged-terminal` already forbids any later event
   naming a merged patient, so on today's catalog
   `no-resolution-after-a-placeholder-is-consumed` can find nothing the
   other would miss. It was a REQUIRED rider of the letter, it costs one
   cheap pass and no `engine/replay`, and section 5.2's argument for
   keeping it is in its own docstring rather than only here. The
   counter-argument, stated fairly: this is a second gate over one law,
   and `roadmap.md#performance-residual-sites` is a live row about the
   check phase's cost.
3. **The tolerated column is VACUOUS in every shipped corpus** (section
   3), so `placeholder-dispositions`'s `consumed-by-churn` is gated by a
   HAND-BUILT log rather than a population one. That is
   `R-empty-population-is-red` honoured rather than dodged, but it is
   the same weakness the TS-3 session disclosed in its own judgment call
   3: a hand-built gate cannot catch a future change that stops the
   corpus reaching the clause at all. What stands beside it is
   `nothing-is-consumed-on-a-real-identification-run-test`, which pins
   the ZERO on a real fold so that the day a shipped corpus starts
   producing the shape is a red rather than a silence.
4. **No ADR was written.** The de-scaffold ruling of 2026-08-25 reserves
   an ADR for a payload-behaviour or contract decision; this changes
   what an invariant MEANS, which is arguably a contract decision, and
   the two preceding TS closes (`c5e5f2b`, `c156690`) each landed a
   comparable semantics change with a session record and no ADR. Followed
   their precedent; disclosed because the case is genuinely arguable.
5. **The draw analysis was run for an option that was rejected.** It
   cost two 10^5 probe runs. The rejection is worth more with the number
   behind it, and the number CORRECTED the letter's own prediction
   (section 4.2) -- which is the second time in three sessions that a
   lettered option's stated rationale has been wrong while its verdict
   survived.
6. **`bin/regression-oracle` was NOT run.** This session moves no
   emission code and no engine code, so the oracle's `{:ground-truth
   :hl7}` pair cannot move; the bracket's IDENTICAL over the
   ground-truth half is the claim that was owed and is the one made.
   ADR-0175 E1's "an emission sweep owes both lines" does not apply --
   this is not an emission sweep.

## 12. Where the next session picks up

**The traffic-scale programme's measurement arc is complete.** Both
10^5 add-on cells are MEASURED, msg/event exists at every decade of the
`v2` series, and check/emit/spool have honest exponents on an add-on
corpus for the first time. What is left on the roadmap and what this
session did not touch:

- `roadmap.md#performance-residual-sites` (PRIORITY 1) now has a
  sharper target: generate's 1.624 second-decade exponent is the whole
  of the 10^6 wall-clock problem, and section 8 says the heap problem
  is in EMIT, which no row currently owns.
- `roadmap.md#cancel-discharge-reopens-an-encounter-that-never-closes`
  (PRIORITY 9) is untouched and still MEASURED-not-fixed: 54 of 55
  cancel-discharges at `v2` 10^5 re-open an encounter with no closer.
  It passes every gate in the catalog, which is why it blocks nothing.
- The `:identity-unavailable` hook's placeholders are ADMITTED by
  construction (section 2.2), which is what put them in churn's
  lottery. Nobody has asked whether that is the intended shape of an
  unidentified presentation; this session did not either, because
  option (A) makes it not matter for the check. It would matter to a
  session revisiting the person layer's own semantics.
