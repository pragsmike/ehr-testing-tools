# TS-3: a compiled opener never asks `encounter-openable?`

Session record, 2026-08-29. HEAD at start `11765bb`; ceremony R30
(commit and push at each checkpoint, unattended), taken from the prompt.
`bin/preflight` ran before git with two findings, both disclosed in
"Gates run" below.

The prompt's step 2 is "LETTER THE RULING AND STOP", and step 3 runs
only "on the author's ruling, this session or its successor". **The
letter was written, the ruling was asked for, and the author ruled (A')
within the session** -- so step 3 ran here.

## 0. What this session did

`roadmap.md#ts-3-outpatient-opens-over-an-encounter` was reproduced at
its own patient, its MECHANISM was measured to both halves the prompt
named, **the row's own mechanism was corrected in its particulars**, a
second and larger defect was found by the same measurement and rowed,
the design question was lettered with a recommendation, the author
ruled, and the ruling landed with five gates. **TS-3 is CLOSED by
`c156690`**; `bin/ground-truth-bracket` came back IDENTICAL over 38
roots, so nothing was re-pinned and no sweep was spent. Both 10^5 cells
were re-run and **both stay BLOCKED -- now on the same single row, TS-4,
at both.**

Four findings, in the order they matter:

1. **The row's reading of WHICH events collide is wrong, and its
   conclusion is right anyway.** The `:admission` is not
   module-compiled and the `:outpatient-visit` is not "the module's own
   later" opener -- `compile-trajectory` cannot emit two openers. The
   conclusion the row drew from that wrong reading -- a compiled opener
   is queued directly and never asks `encounter-openable?` -- is exact,
   and the trace confirms it at the decide.
2. **The patient holds TWO CONCURRENT QUEUE ENTRIES**, and that is the
   structural fact the first diagnosis had no way to see from the log.
   "Nothing left in the queue to close it" is true of the wrong queue.
3. **A legal `:cancel-discharge` re-opens an encounter that never
   closes -- 54 of 55 at v2 10^5, with no closer at all for the rest of
   the run, and every gate in the catalog green.** TS-3's patient is
   the ONE case where anything downstream cared. Rowed separately.
4. **The fix fires ONCE in a 424-span population and moves nothing
   else.** 423 compiled spans at `nobed` and 424 at `v2` go through the
   new wrapper; exactly one is refused, at TS-3's own patient, and the
   `nobed` corpus comes out at the identical 129,415 events it had
   before. That is the identity claim measured at population scale
   rather than argued.

## Fences honoured

- **No fix before the ruling.** Sections 1-3 were written, and the
  ruling asked for, with `src/` untouched. The letter's own options were
  developed against the trace, not against a fix already in hand.
- **Red before green under stash isolation.** The fix was written, then
  `git checkout`-ed away so the red run exercised exactly the unfixed
  code, then re-applied. The four failures are in section 5 with their
  own output.
- **Rejected/deferred compiled events consume no draw the applied path
  would not.** On the PASS branch, none: `decide :repeat-arrival` draws
  nothing and returns the identical vector at the identical `t`,
  measured as an unchanged 129,415-event `nobed` corpus over 423 spans.
  On the REFUSE branch the span's own draws are not taken, **which IS a
  declared reshuffle and is said plainly** -- it fires once, at `v2`, and
  the corpus moves by +29 events (section 7.4). No shipped corpus is
  reachable by it: the bracket is IDENTICAL over 38 roots.
- **A cell failing self-check is BLOCKED, not tuned.** Both cells fail;
  both stay BLOCKED; neither plan-appendix entry converts to MEASURED.
- **F3 absolute.** The 10^6 decision keeps its PROJECTED label and its
  note, and is re-stated rather than promoted (section 7.6).
- **One declared sweep at most.** None spent -- the bracket is
  IDENTICAL, so no declaration was owed and nothing was re-pinned.

## 1. The reproduction

`spike.probe-ts3`, a new scratch probe on the surviving traffic-scale
scratch, at `dense-7500-v2.edn`, seed 20260824, HEAD `11765bb`.
UNTIMED: it wraps the `decide` VAR and runs `check-all` a second time,
so its wall is not a measurement of anything and no health record is
taken for it.

    EVENTS 171835
    CENSUS-INVARIANTS {:admission-only-when-no-open-encounter 1,
                       :every-placeholder-registration-is-resolved-or-still-open 1,
                       :outpatient-patients-occupy-no-bed 33950}
      :admission-only-when-no-open-encounter   ("PID-000640-f57cb996")
      :outpatient-patients-occupy-no-bed       ("PID-000640-f57cb996")
      :every-placeholder-registration-...      ("PID-007500-e98926c1")

The row reproduces exactly: one opener violation, 33,950
`outpatient-patients-occupy-no-bed` rows, both entirely
`PID-000640-f57cb996`, and TS-4's single row at `PID-007500` beside
it. **171,835 events, which is the TS-5 session's own post-fix figure to
the event** (its section 7 table, and its `171,913 -> 171,835 = -78`
delta row). The reproduction is byte-for-byte the same corpus that
session measured; the 171,913 the TS-3 row still quotes is the
PRE-`c5e5f2b` count from the session before it, and is superseded.

## 2. The mechanism, to both halves

### 2.1 The compiled list, and what the row got wrong

`decide :registered` for `PID-000640` returns exactly these
`:prepend-steps` (module `bronchitis`):

    {:type :delay, :from 1676160, :to 1676160}
    {:type :outpatient-visit,   :citation {:module "bronchitis", :state :doctor-visit}}
    {:type :procedure,          :citation {:module "bronchitis", :state :lung-function-test}}
    {:type :delay, :from 27360, :to 27360}
    {:type :care-plan-start,    :citation {:module "bronchitis", :state :nonsmoker-careplan}}
    {:type :medication-order,   :citation {:module "bronchitis", :state :cough-suppressant}}
    {:type :outpatient-visit-end, :citation {:module "bronchitis", :state :end-doctor-visit}}

1,676,160 minutes is 100,569,600 seconds and the registration is at
t=40,260: **40,260 + 100,569,600 = 100,609,860**, the row's own
instant, to the second. So the offending `:outpatient-visit` is the
module's FIRST and ONLY compiled encounter, parked behind its own
compiled delay, not a "later" one.

It cannot be a later one. `compile-trajectory`'s loop short-circuits
on `encounter-closed?` as its cond's first clause
(`compile_trajectory.clj:493`), set by the first horizon-phase
`:encounter-end` -- this project's single-encounter horizon expressed
at compile time. **A compiled step list holds at most one encounter**,
so "the module's own LATER compiled `:outpatient-visit`" names a shape
the compiler cannot produce.

And the `:admission` at t=240,300 is not compiled either. It arrives
as the first step of the authored `dense_fast` pathway, carried by a
REPEAT ARRIVAL and correctly routed:

    t=240300  repeat-arrival  prepend=[:admission :delay :bed-swap :order
                                       :medication-order :delay :observation
                                       :medication-end :discharge :cancel-discharge]

The trailing `:cancel-discharge` is churn's, injected into that
pathway.

### 2.2 The re-open, traced (half (i))

    t=244620  discharge          closes ENC-000640-00; books APT-000640-01
                                 (follow-up, scheduled-t 6,897,420)
    t=244620  cancel-discharge   subject :discharged -> legal (the one status
                                 a cancel-discharge may find); RE-OPENS
                                 ENC-000640-00 with :location ED-139 restored

The row's claim that the queue then holds nothing to close it is
**CONFIRMED, and the reason is structural rather than incidental**:
`churn/applicable?` gates `:cancel-discharge` on
`:has-uncancelled-discharge?`, which the static oracle only sets after
the pathway's own `:discharge` step. That discharge is the LAST
authored step of `dense_fast`, so the only gap left for the insertion
is the end gap. A churned `:cancel-discharge` in these pathways is
necessarily terminal on its own entry.

**The decided/churn path has exactly the same exposure, and nothing
re-queues a discharge anywhere.** Measured over the whole population
rather than reasoned about:

| corpus | `:cancel-discharge` re-opens | of those with NO later closer |
| --- | --- | --- |
| v2 10^5 (`dense-7500-v2`) | 55 | **54** |
| seed-202-ed-tuesday | 1 | 1 |
| demo-ed-tuesday | 1 | 1 |
| seed-424242-clinic-decade | 0 | -- |
| seed-5-clinic-decade | 0 | -- |
| adhd-seed-45 | 0 | -- |
| demo-clinic-decade | 0 | -- |

The 55th, at v2, is `PID-000640` -- and its "closer" is the
illegitimate second encounter's own `:outpatient-visit-end` at
t=102,251,460, which is to say it has none either.

**Why the other 56 are green.** They stay `:class :inpatient` holding
the bed the reinstatement gave back, which is exactly what
`admitted-occupies-one-slot` requires, and
`every-encounter-is-opened-and-closed-or-still-open` says "or still
open" in so many words. The catalog permits a stay that never ends.
This is a real fidelity defect that no gate can see, and it is rowed
as `roadmap.md#cancel-discharge-reopens-an-encounter-that-never-closes`
rather than folded into TS-3, because it is not TS-3's cause: 56 of
its 57 instances produce nothing.

### 2.3 The two concurrent queue entries

This is the fact the first diagnosis could not have read off the log,
and it is why "nothing left in the queue to close it" is about the
wrong queue:

    t=40,260       registered      module steps prepended; the FIRST is a
                                   :delay of 100,569,600 s
    t=40,260       delay           this entry re-queues at t=100,609,860
                                   -- and is not touched again for 3.19 years
    t=180,960      appointment     a SECOND entry: an arrival ordinal bound
                                   to the same person
    t=240,300      repeat-arrival  a THIRD; prepends the whole dense_fast
                                   pathway (guard passed: encounter nil)
    t=240,300 .. 244,620           admission ... discharge ... cancel-discharge
                                   -- all on that third entry, which is then
                                   spent
    t=872,160      repeat-arrival  APT-000640-00 comes due; guard REFUSES
                                   (encounter open) -- prepends nothing
    t=6,897,420    repeat-arrival  APT-000640-01, the discharge's own
                                   follow-up; guard REFUSES -- prepends nothing
    t=100,609,860  outpatient-visit  the module entry finally comes due.
                                     NO WRAPPER. Opens ENC-000640-01 over the
                                     still-open ENC-000640-00, :class -> :outpatient
                                     with bed ED-153 still held.

**Both guards that exist ran, and both were right.** The defect is not
that `encounter-openable?` gave a wrong answer; it is that the one
path that needed it never asked.

### 2.4 The opener's application path (half (ii))

Measured, not inferred: there is no wrapper decide at t=100,609,860 in
the trace. The step reached the queue as `decide :registered`'s
`:prepend-steps` (`engine.clj:1047`, the attach path), was spliced onto
the front of that patient's own `remaining` by the run loop
(`engine.clj:4805`), and was popped straight into `decide
:outpatient-visit`, which asks nothing.

**Where the question COULD be put, without touching the arrival
paths.** Exactly one site: the head of `decide :admission` and `decide
:outpatient-visit`, the only two openers. For an arrival-borne opener
that is a redundant re-ask of what its wrapper answered at the same
`t`; for a compiled opener it is the only place the question can be
put at all, because the compiled list has no wrapper. It cannot be put
at the attach path itself: the legality question belongs to
t=100,609,860, and `decide :registered` runs at t=40,260.

### 2.5 The consequence, and why the mass is 33,950

`evolve :outpatient-visit` sets `:class :outpatient` and does not
touch `:location`; `evolve :outpatient-visit-end` sets `:status
:discharged` and does not touch it either. So from t=100,609,860 to
the end of the run the patient is an outpatient holding ED-153, and
`outpatient-patients-occupy-no-bed` re-reports the offender **on every
subsequent event in the log**, not once per event of this patient. The
33,950 is therefore a fact about the length of the log's tail, not
about the patient -- worth stating because it makes the residue look
like mass when it is one defect.

## 3. The letter

Compiled-pathway semantics; the author decides. Nothing below was
implemented.

### (A) Gate at application

Ask `encounter-openable?` at the head of `decide :admission` and
`decide :outpatient-visit`; a refused opener returns `rejected-outcome`
with a new reason (contract 1.8.0 -> 1.9.0).

**BARE (A) IS REFUTED by its own dangling-step analysis**, and this is
the section's main result. Rejecting the opener alone leaves the
compiled tail to run, per step type, at `PID-000640`:

| dangling step | what happens | what the catalog says |
| --- | --- | --- |
| `:procedure` | emits, stamped by `run` with the INPATIENT `ENC-000640-00` | `clinical-content-only-when-admitted` PASSES -- the patient is admitted |
| `:delay` (from = to) | advances the clock, draws nothing (`engine.clj:1728`'s arithmetically-dead branch) | nothing to say |
| `:care-plan-start` | emits, same mis-stamp | passes |
| `:medication-order` | emits, same mis-stamp | passes |
| `:outpatient-visit-end` | emits and **CLOSES ENC-000640-00**, the inpatient encounter | `discharge-closes-an-open-encounter` passes (one IS open); `every-encounter-is-opened-and-closed-or-still-open` passes (2 closers <= 1 opener + 1 reinstatement); `outpatient-patients-occupy-no-bed` passes (class stays `:inpatient`); `admitted-occupies-one-slot` passes (status is now `:discharged`) |

So bare (A) turns **33,950 red rows into zero red rows and a silently
false log**: bronchitis's cough suppressant and care plan attributed to
a laceration admission, and an "outpatient visit ended" for a visit
that never opened. For a tool whose product is trustworthy ground
truth that is a worse outcome than the defect.

### (A') Gate at application, span-wise -- THE RECOMMENDATION

Split the compiled list at the attach path into
`pre-opener ++ [{:type :repeat-arrival :steps span}] ++ post-closer`,
where `span` is opener-through-matching-closer. The EXISTING,
unchanged `decide :repeat-arrival` then owns the whole span.

Why this and not (A): it is ADR-0174's own law -- "the whole arrival is
prepended or none of it is", already stated verbatim in `decide
:person-encounter`, `decide :repeat-arrival` and `decide :appointment`
-- applied to the one producer of encounters that never got it. It adds
no decide, no rejection reason, no run-loop change, no new invariant
and no new predicate. The leading `:delay` stays OUTSIDE the wrapper,
which is what makes the guard fire at t=100,609,860 rather than at
t=40,260.

**Draw analysis, stated as a prediction and not as a measurement.**
`decide :repeat-arrival` consumes zero draws on both branches. On the
PASS branch the wrapper returns the identical step vector at the
identical `t` with `:advance 0`, so every downstream draw is unmoved.
The extra loop iteration advances `seq-no` by one more per compiled
encounter, and that is order-preserving rather than reshuffling: queue
keys are `[t seq-no]` with `seq-no` monotone, so an entry created later
always sorts after one created earlier at equal `t`, whatever the
increments in between. On the REFUSE branch the span's steps are never
decided and their draws are never taken -- and this IS draw-affecting,
because `decide :outpatient-visit` takes a `:facility` `uniform-choice`
for the attending, which is a SHARED stream. Said plainly rather than
claimed away: **(A') is draw-free exactly where it does not fire, and a
declared reshuffle exactly where it does.** The bracket is what settles
it, not this paragraph.

**Blast radius, measured this session** (`spike/ts3-census.sh`), where
a "compiled opener" is an `:admission`/`:outpatient-visit` decide whose
step carries a `:citation {:module ...}`, and "refused" is what
`encounter-openable?` would have answered had anything asked:

| corpus | events | compiled openers | refused |
| --- | --- | --- | --- |
| seed-202-ed-tuesday | 1,213 | 0 | 0 |
| seed-424242-clinic-decade | 1,774 | 33 | **0** |
| seed-5-clinic-decade | 1,412 | 32 | **0** |
| adhd-seed-45 | 97 | 0 | 0 |
| demo-ed-tuesday | 1,269 | 0 | 0 |
| demo-clinic-decade | 1,569 | 28 | **0** |
| v2 10^5 (scratch, not shipped) | 171,835 | -- | **1** |

Prediction to be tested rather than assumed: `bin/ground-truth-bracket`
**IDENTICAL over all 38 roots**, nothing re-pinned, no declaration
owed. The three clinic-decade corpora are the evidence that matters --
93 compiled openers between them exercise the wrapper's PASS branch, so
an IDENTICAL bracket there is a real proof of the byte-identity claim
and not a vacuous one.

**Which invariants change meaning.**
`admission-only-when-no-open-encounter` gains a second producer that
can no longer violate it by construction, which is the same thing arc
3b sweep 1 did for arrivals; nothing else in the catalog changes
meaning, and no row goes vacuous.

### (B) Close at re-open -- REJECTED

Have a `:cancel-discharge` that re-opens an encounter with no queued
closer schedule a re-discharge.

Three reasons, in order of force:

1. **It does not fix TS-3.** The compiled opener is 3.19 years out; any
   plausible re-discharge instant is before it, so the collision would
   vanish by TIMING and not by law. Move the module, the seed or the
   horizon so the opener falls first and TS-3 is back, unguarded. A fix
   whose correctness depends on which of two independent clocks wins is
   not a fix.
2. **The option's own predicate is unaskable.** `decide
   :cancel-discharge` sees `world`; the step queue is the run loop's
   local and is not in `world`. "Whose queue holds no closer" needs
   either queue introspection threaded into `decide` -- a new coupling
   between the scheduler and every decide -- or an UNCONDITIONAL
   re-discharge, which is wrong the moment a pathway does carry steps
   after its `:discharge`, and would then double-close.
3. **It is draw-affecting, and the prompt's suspicion is correct.** A
   re-discharge instant needs a stay-length draw from the `:patient`
   stream, and under the fixed-consumption law that draw must be taken
   on EVERY `:cancel-discharge` decide, applied or rejected, or
   consumption becomes a function of world state. Then `decide
   :discharge` itself takes its `vacate-bed` `:facility` draw and its
   two `:scheduling` follow-up draws. That reshuffles every corpus
   carrying a `:cancel-discharge` decide -- seed-202-ed-tuesday and
   demo-ed-tuesday today -- for a change that leaves TS-3 open.

The underlying gap is real and this session measured it for the first
time, so it is ROWED rather than dismissed:
`roadmap.md#cancel-discharge-reopens-an-encounter-that-never-closes`.

### (C) Compile-time -- STRUCK

"The walk never compiles an opener into a window another encounter of
the same patient can still occupy." Unknowable at compile time, and
the trace says why in four independent ways:

- the compile happens at RUN START (ADR-0173 ruling C1), before
  `init-world` exists, from the persona alone;
- the colliding encounter arrived on a DIFFERENT arrival ordinal, bound
  to the same person by the person layer, which the per-patient compile
  does not consult;
- that arrival's `:cancel-discharge` was injected by a STATIC oracle
  and accepted at runtime against the LIVE log by
  `last-uncancelled-index`;
- and the window it occupies is UNBOUNDED precisely because the cancel
  leaves the encounter open forever -- so even a perfect compile-time
  analysis would have to predict a runtime cancel's legality.

STRUCK, with the measurement as the reason rather than as an intuition.

### Recommendation

**(A').** It is the only option that closes the defect by law rather
than by timing; it introduces no new mechanism, reusing the guard and
the wrapper the repository already has; its dangling-step problem is
solved by construction rather than by a per-step-type argument; and
its blast radius is measured at zero refusals in every shipped corpus,
with 93 pass-branch exercises to prove the byte-identity claim is not
vacuous.

## 4. The ruling, and the fix it licensed

**THE AUTHOR RULED (A') WITHIN THE SESSION**, so step 3 ran here rather
than in a successor. The ruling was put as the four choices of section
3 with the recommendation named, and it was taken verbatim: wrap the
compiled encounter span at the attach path.

`c156690` is the whole of it, and it is 77 lines of engine.clj of which
most is the docstring:

    (defn- gate-compiled-encounters [steps] ...)

    ;; decide :registered, the attach path
    :prepend-steps (gate-compiled-encounters (:steps compiled))

Two sets are named beside `encounter-openable?` -- the opener and
closer STEP types -- and the function walks the compiled list once,
replacing each opener-through-closer run with one `:repeat-arrival`
step carrying it. Everything before an opener is copied through
untouched, which is what leaves the parking `:delay` outside the
wrapper; a span with no closer of its own is wrapped to the end of the
list rather than dropped; `nil` in is `nil` out and `[]` in is `[]`
out, because `decide :registered` attaches `(:steps compiled)` verbatim
for a patient with no closure and an existing gate reads that.

**The loop is a loop and not a find-the-one, deliberately.**
`compile-trajectory` emits at most one encounter per patient today, so
one span is all it ever finds; written as a find-the-one it would leave
a second span silently unguarded if that ever changed -- which is the
exact failure this row already had once, in the shape of an opener
nobody asked about.

## 5. Gates, red before green

Three new in `engine_test`, one new in `check_test`, one existing gate
restated.

**The red run, at `11765bb` with the fix stashed out** -- four failures,
and their own output rather than a summary of it:

    FAIL in (a-compiled-encounter-is-attached-behind-one-gated-step)
    the compiled list must attach as the parking delay plus ONE gated step
    expected: (= [:delay :repeat-arrival] (mapv :type attached))
      actual: (not (= [:delay :repeat-arrival]
                      [:delay :outpatient-visit :procedure :delay
                       :care-plan-start :medication-order :outpatient-visit-end]))

    FAIL ... the span is opener-through-closer, verbatim and in order
      actual: (not (= [{:type :outpatient-visit ...} ... ] nil))

    FAIL ... a span with no closer of its own is wrapped to the end of
             the list rather than dropped
      actual: (not (= [:delay :repeat-arrival] [:delay :outpatient-visit :procedure]))

    FAIL ... (the same span assertion, open-ended case)

    Ran 109 tests containing 707 assertions.  4 failures, 0 errors.

and green afterwards: `Ran 109 tests containing 707 assertions. 0
failures, 0 errors.`

**What each gate is for, and which were born green.**

| gate | red before? | what it holds |
| --- | --- | --- |
| `a-compiled-encounter-is-attached-behind-one-gated-step` | **RED, 4 failures** | the span is one step; the parking delay stays outside it; the re-bracket splices back to the compiled list exactly; an open-ended span is wrapped, not dropped |
| `a-compiled-list-with-no-encounter-attaches-unchanged` | born green | the identity half -- 19 of the 31 vendored modules emit no encounter at all, and their lists must reach the queue BYTE-identical. This is the gate that makes the IDENTICAL bracket a prediction rather than a hope, and `pr-str` equality is asserted beside `=` because the claim is bytes |
| `a-refused-compiled-encounter-takes-its-whole-tail-with-it` | born green | the composition: an open encounter swallows the WHOLE span, closer included, and the same span on a patient with no open encounter is prepended whole. The second half is the anti-vacuity clause |
| `check_test/...-detects-the-ts-3-reopen-shape` | born green | the prompt's hand-built reopen-with-empty-queue log, with its without-the-cancel control beside it so it cannot pass for the wrong reason |
| `compile-patient-is-what-registered-attaches` | amended | asserted raw equality of `(:steps compiled)` and `:prepend-steps`, which the re-bracket breaks by design. It now asserts the stronger thing the re-bracket owes: splicing every wrapper's own `:steps` back in recovers `compile-patient`'s output exactly. Stated as a splice rather than by calling the gate again, which would assert nothing |

Three of the five are born green and say so. The two that gate
behaviour the fix did not change -- the refusal composition and the
check-side invariant -- are characterisation gates, and calling them
red-before-green would have been a false claim about what moved.

## 6. The bracket

Predicted IDENTICAL in section 3, before it ran, on the measured
ground that no shipped corpus contains a refusable compiled opener.

    $ bin/ground-truth-bracket 11765bb c156690
    --- coverage: 38 roots carry :ground-truth and are digested;
        3 skipped (no such key): appendicitis.edn, ear-infections.edn, sore-throat.edn ---
    --- declared-digest-change: no (soundness: yes outside the leading docstring) ---
    IDENTICAL: every digested root's :ground-truth matches
               between 11765bb and c156690 (38 roots)

**IDENTICAL over 38 roots, exit 0. No declaration owed, nothing
re-pinned, no sweep spent.** And it is not a vacuous IDENTICAL: the
oracle's roots include every vendored module, so the wrapper's PASS
branch was exercised on every compiled encounter those 38 roots
contain, and the three clinic-decade corpora carry 93 compiled openers
between them (section 3's table). The prediction and the measurement
agree, and the reason they agree is the identity half of the gate, not
luck.

`make docsgen` moved nothing after the fix -- no generated file, no
fixture, no `demos/traces/**` byte -- which is the same claim from the
other side.

## 7. The two 10^5 cells

Same driver, same scratch, same protocol and same preamble as the TS-5
session (`spike/driver2.clj` via `cell2.sh`, seed 20260824, warm-up plus
two timed runs, one JVM per run, `/usr/bin/time -v` around each; figures
are the mean of the two). HEAD `c156690`.

### 7.1 Both cells STILL BLOCKED, and now on the SAME single row

| cell | events | persons | generate | check | emit | spool | wall | peak RSS | self-check |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `nobed` 10^5 | 129,415 | 39.79 s | 125.45 s | 12.71 s | -- | -- | 185.48 s | 1,478 MB | **BLOCKED (1 violation)** |
| `v2` 10^5 | 171,864 | 40.45 s | 165.51 s | 17.91 s | -- | -- | 231.62 s | 1,918 MB | **BLOCKED (1 violation)** |

Per-run walls: nobed 183.05 / 187.90 s; v2 229.44 / 233.80 s. Event
counts are recovered by the untimed probe, since a blocked run discards
its payload.

**Neither cell self-checks clean, so under the fence NEITHER converts to
MEASURED** and the plan appendix's BLOCKED entries stay BLOCKED. What
changed is which row does the blocking: it is now
`roadmap.md#ts-4-placeholder-unresolved` at BOTH cells, the same
patient (`PID-007500-e98926c1`) at the same instant (t=37017), and
nothing else.

### 7.2 The census, before and after

| invariant | nobed pre-fix | nobed post-fix | v2 pre-fix | v2 post-fix |
| --- | --- | --- | --- | --- |
| `admission-only-when-no-open-encounter` | 0 | 0 | 1 (TS-3) | **0** |
| `outpatient-patients-occupy-no-bed` | 0 | 0 | 33,950 (1 patient) | **0** |
| `every-placeholder-registration-is-resolved-or-still-open` | 1 (TS-4) | 1 (TS-4) | 1 (TS-4) | 1 (TS-4) |
| **total** | 1 | **1** | 33,952 | **1** |

TS-3 is closed at its own cell: one violation in 171,864 events, and it
is TS-4's.

### 7.3 The gate fired ONCE, in the whole population, and that is measured

The probe meters every compiled span the wrapper decides, split by what
`encounter-openable?` answered:

| cell | compiled spans | refused |
| --- | --- | --- |
| `nobed` 10^5 | 423 | **0** (the key is absent entirely) |
| `v2` 10^5 | 424 | **1** |

The decide counts say the same thing independently: `:outpatient-visit`
goes 2,255 -> 2,254 and `:outpatient-visit-end` 2,252 -> 2,251 at `v2`,
which is one span of exactly two openers-and-closers not run. **At
`nobed` the corpus is byte-for-byte the same size it was -- 129,415
events, identical to the TS-5 session's own post-fix figure** -- which
is the population-scale statement of the identity half: 423 spans went
through the new wrapper and not one event moved.

**THE FIRST VERSION OF THIS METER WAS WRONG and reported 28 refusals at
`nobed`.** It asked whether ANY step of the wrapped span carried a
`:citation {:module ...}`, which also matches an ORDINARY repeat
arrival, because this scratch config's AUTHORED `dense-inpatient`
pathway carries `:citation {:module "dense_inpatient"}` on a
medication-order step of its own. The discriminator is the FIRST step's
citation -- only a compiled span has one on its OPENER. The contaminated
run is kept beside the corrected one
(`spike/out/postfix-census-contaminated-meter.log`) rather than deleted,
and the tell that caught it was arithmetic: 28 refusals cannot coexist
with an unchanged event count.

### 7.4 Phase figures, and which of them are measurements

**`nobed` is the control and it is flat.** Generate 125.45 s against
129.12 s at the TS-5 session's cell (-2.8%), persons 39.79 against
39.14 (+1.7%), check 12.71 against 12.88 (-1.3%), wall 185.48 against
188.63 (-1.7%) -- and the event count identical. A corpus the gate never
refuses is unmoved, as a wrapper that consumes no draws and returns the
identical vector at the identical `t` should be.

**`v2` moves, and the movement is the check phase and nothing else.**
Generate 165.51 s against 162.68 s (+1.7%), persons 40.45 against 40.26
(+0.5%) -- both inside the noise of a two-run mean on this host. Wall
falls 282.02 -> 231.62 s (-17.9%), and 52.3 s of that 50.4 s is the
check: **17.91 s against a parenthesised (70.24 s)**. Those two are not
the same measurement. The old one materialised 33,952 violation maps;
this one materialises ONE. **The parenthesis is retired for this cell**,
on the same ground the TS-5 session retired it for `nobed`: a check
building a single violation is a check measurement, and 17.91 s against
`nobed`'s 12.71 s on a corpus 33% larger is the shape that says so.

**Event counts move at `v2` and not at `nobed`:**

| cell | pre-fix | post-fix | delta |
| --- | --- | --- | --- |
| `nobed` 10^5 | 129,415 | **129,415** | **0** |
| `v2` 10^5 | 171,835 | **171,864** | **+29** |

The refused span itself is -5 events (the opener, a procedure, a
care-plan-start, a medication-order and the closer), so the +29 is a net
+34 downstream and is a REAL corpus change, declared as such. The
mechanism is visible in the trace of section 2: with the second
encounter never opening, `PID-000640`'s reinstated inpatient encounter
stays open for the remaining 3.2 years of the run instead of being
closed by a closer that never belonged to it, and this config's
`:chatter {:restatement {:rate-per-patient-day 0.25}}` bills
restatements against OPEN-encounter patient-days. The gate does not
consume draws; it changes what the world looks like afterwards, which
is a different thing and is why the bracket -- not this delta -- is what
licenses the "nothing re-pinned" claim.

### 7.5 Exponents, and msg/event, over a series that is STILL not complete

The 10^4 column is the TS-defects session's
(`.agents/session-records/2026-08-29-ts-defects-and-blocked-cells.md`
section 3.1, HEAD `1b4e264`) and is NOT re-measured here, so these
slopes mix two HEADs three fixes apart. Printed with that caveat rather
than withheld, exactly as the two prior sessions printed theirs.

| series | phase | 10^4 -> 10^5 |
| --- | --- | --- |
| v2 | persons | 1.109 |
| v2 | generate | **1.663** |
| v2 | check | 0.941 |

Generate's second-decade exponent is **1.663** against the TS-5
session's 1.656, the TS-defects session's 1.620 and the close's 1.635 --
the same super-linear shape now across four independent measurements.
Persons stays near-linear at 1.109 against 1.107.

**The check exponent is new and is the first one this programme can
state at all.** Every previous v2 10^5 check figure was a failing check
materialising mass; 0.941 is sub-linear, which is what a check that no
longer builds a violation vector should look like -- but its 10^4 leg is
a startup-contaminated 2.052 s from another HEAD, so it is reported as
an observation and no claim is made from it.

**MSG/EVENT AT 10^5 IS STILL UNMEASURED ON ANY ADD-ON CORPUS, and this
session did not change that.** Both cells fail their self-check before
emission, so emit and spool never run and there is no message count to
divide. The gap the close named, and that both intervening sessions
named, is unchanged. Stated rather than extrapolated, for the fourth
time.

### 7.6 The 10^6 decision, re-stated

**STILL DECLINED, keeping its PROJECTED label and its note.** F3 is
absolute and nothing here touches it. The decline was on EMIT peak heap
and `:emit-peak-heap-mb` is 0.0 in all four timed runs, because the
phase was skipped -- so the arithmetic that would license 10^6 is
exactly as unmeasured as it was. What the new figures add, for whoever
gets to take it: generate peaks at 734.6 / 771.9 MB (`nobed` / `v2`)
against the 3.88 GB ceiling, and check now peaks at 1,010.1 / 1,460.2 MB
while building ONE violation each -- so the check-phase heap of a clean
10^6 run can now be projected off a figure that is not dominated by a
violation vector, for the first time. None of that reaches emit, which
is the gate.

## 8. Health record

    $ date -Is                        -> 2026-08-29T13:34:36-04:00 (before the cells)
    $ git log --oneline -1            -> c156690
    $ git rev-parse --abbrev-ref HEAD -> main

    $ uptime -s   -> 2026-08-24 15:10:46   -- the SAME boot the traffic-scale
                     close, the TS-defects session and the TS-5 session all ran on
    $ uptime      -> up 4 days 22:23, load average 0.78 1.21 1.39
    $ free -h     -> 15Gi total, 3.1Gi used, 12Gi avail
    $ df -hT .    -> /dev/sdd ext4 251G, 28% used   (NOT /mnt/c)
    $ nproc       -> 12

    $ java -version
    openjdk version "21.0.7" 2025-04-15
    OpenJDK Runtime Environment (build 21.0.7+6-Ubuntu-0ubuntu120.04)
    $ MaxHeapSize = 4162846720  ->  3.88 GB  {ergonomic}

    Windows side, 13:34 (immediately before the first cell):
      LoadPercentage -> 2

Same boot, same JVM, same heap ceiling and same disk as all three
prior sessions in this arc, so their figures and these are on the same
machine in the same state.

**Host at the cell boundaries.** Windows `LoadPercentage` **3, 12, 1**
at the three samples -- the series comparable to the close's own 0-17
in-flight range and to the TS-defects session's 4-15. No cell was
started above the ~20 threshold that session set after its 1.7x
instrument error, and none was discarded.

## 9. What landed

| commit | what |
| --- | --- |
| `c156690` | `gate-compiled-encounters` and the attach path through it; three new engine gates, one new check gate, one amended |
| the commit this record rides in | the roadmap (TS-3 CLOSED by sha under `## Done`, TS-4 re-scoped to block both cells, one new row, `corpus-player-slices` renumbered 9 -> 10), the plan appendix's post-TS-3 cell entry and its re-stated 10^6 decision, the regenerated INDEXes and `state-derived.md`, this record and its prompt archive |
| its successor | `make integration` green on the clean tree, and CI green at the pushed tip -- this repository's standing pattern, since neither can be named from inside the commit they judge (`7500c75`/`a4e8698` and `11765bb` are the two previous pairs) |

A docs commit cannot name its own sha, so that row names itself by
description. No tag was paid.

## What re-pinned, and nothing outside this list

**Nothing.** The bracket is IDENTICAL over 38 roots, `make test` is
green with no fixture, digest, count or roster pin touched, and `make
docsgen` moved no generated file at all -- not `docs/formats.md`, not
`event-examples.edn`, not the event-schema export, not
`demos/traces/**`. The event contract stays at 1.8.0: the fix adds no
event kind, no field and no rejection reason.

## Gates run, with exit codes

    clojure -M:poly check                       exit 0   OK
    clojure -M:poly test brick:sim-engine       4 failures (RED, fix stashed out)
    clojure -M:poly test brick:sim-engine       0 failures (GREEN, fix applied)
    make test          MAKE_EXIT=0    4,745 tests / 24,043 assertions,
                                      0 failures 0 errors, 15 min 43 s
    make docsgen       DOCSGEN_EXIT=0 no generated file moved
    bin/ground-truth-bracket 11765bb c156690    IDENTICAL (38 roots), exit 0
    bin/preflight      exit 1         two findings, both disclosed below
    make test (again)  MAKE_EXIT=0    after the final doc edits: same
                                      4,745 / 24,043, 15 min 41 s
    make docsgen (again) DOCSGEN_EXIT=0  still no generated file moved
    make integration   recorded by this record's own successor commit --
                       it needs a CLEAN tree, so it runs after the docs
                       commit and cannot be named from inside it
    gh run view        likewise, at the pushed tip

`bin/preflight`'s two findings, disclosed rather than summarised: the
working tree was NOT clean when it ran (the session's own work in
flight, listed file by file), and HEAD is not tagged `stable-*` -- which
is correct, since no tag is paid. Its exit code is 1 for the first of
those. The last five CI runs on `main` were all green, the repo root is
not under `/mnt/`, `core.fileMode` is true, and local HEAD matched
`origin/main` at `11765bb`.

## 10. Scratch

Copied from the TS-5 session's surviving scratch, which is copied in
turn from the traffic-scale close's (appendix C of
`.agents/session-records/2026-08-29-traffic-scale-close.md` regenerates
it from nothing). Added this session:

| file | what |
| --- | --- |
| `spike/src/spike/probe_ts3.clj` | wraps `engine/run` and the `decide` VAR: per-patient decide trace with pre-decide state, outcome events and `:prepend-steps`; the population opens-over-open-encounter and cancel-discharge-re-open censuses; and the compiled-opener blast-radius meter |
| `spike/ts3.sh` | classpath shim for it |
| `spike/ts3-census.sh` | the six shipped corpora, one line each |
| `spike/src/spike/sweep_ts3.clj`, `spike/sweep-ts3.sh` | the seed sweep of judgment call 3 -- one JVM, many runs |
| `spike/cells-ts3.sh` | the two 10^5 cells, health record either side |
| `spike/out/ts3-v2-1e5.log` | the trace of section 2 |
| `spike/out/ts3-census.txt` | the table of section 3 |
| `spike/out/sweep1.log` | the sweep that found no cheap witness |

## 11. Judgment calls

None is ratified; each is disclosed.

1. **The v2 10^5 diagnostic run of section 1 is UNTIMED.** It wraps a
   var and runs `check-all` a second time, so its wall is not a
   measurement; no health record was taken for it and no figure from it
   appears in any table but the event count and the violation census,
   which are shapes. The TIMED cells of section 7 are separate runs.
2. **The blast-radius meter identifies a compiled opener by its
   `:citation {:module ...}`.** That is a proxy: true today because
   compiled steps are the only opener steps carrying a module citation,
   and it would silently under-count if a hand-authored pathway ever
   carried one. Stated rather than relied on quietly.
3. **THERE IS NO POPULATION RED IN THE SUITE, and this is the
   session's weakest point.** The prompt asked for red "at
   PID-000640's cell", which is a five-minute 7,500-patient run and
   cannot be a gate. A seed sweep for a cheap population witness was
   run and FOUND NONE: 30 seeds at 40 patients on the clinic-decade
   config with `:churn-profile {:cancel-discharge 1.0}` produced 4-14
   compiled openers each and **zero** refusals
   (`spike/out/sweep1.log`). The reason is structural and worth
   recording: clinic-decade's pathways close with
   `:outpatient-visit-end`, and `churn/applicable?` gates
   `:cancel-discharge` on `:has-uncancelled-discharge?`, which only a
   `:discharge` STEP sets -- so that config can produce no
   cancel-discharge at all, and the long-lived open encounter the
   collision needs never appears. A witness would need a dense
   inpatient config with repeat arrivals, which is the scratch cell
   itself. What stands in its place is the hand-built attach-path gate,
   which exercises the same code path deterministically and at no
   seed's mercy -- and the cell, in section 7, as the population
   measurement. The counter-argument, stated fairly: a hand-built gate
   cannot catch a future change that stops the compiled list reaching
   `gate-compiled-encounters` at all.
4. **`corpus-player-slices` was renumbered 9 -> 10** to open a slot for
   the new row, because `roadmap-lint` requires unique ascending
   priorities and there was no gap. A one-token edit to a row this
   session otherwise did not touch.
5. **The `build-session` skill's step 13 asks for an index line in each
   of `.agents/session-records/README.md` and
   `.agents/prompts/README.md`.** Neither README carries per-file rows
   any more -- both say in so many words that `make state-derived`
   regenerates the two `INDEX.md` files -- so no README edit was owed
   and none was made. Disclosed rather than silently skipped.
6. **The second defect is rowed, not folded into TS-3.** It shares
   TS-3's neighbourhood and 56 of its 57 instances produce nothing, so
   folding it in would have made TS-3's row claim a mass it does not
   have -- the error this row had already made once.
