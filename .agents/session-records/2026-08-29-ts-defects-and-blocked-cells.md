# 2026-08-29 -- TS-1..TS-4: the defects the scale rerun found, fixed, and the blocked cells measured

The traffic-scale close of the same day (`2026-08-29-traffic-scale-close.md`,
findings at its section 9) found four invariant families red at 10^4-10^5 on
the arc-4 add-on configuration, reported them as "rowed", and left the tree
with no rows. This session rowed them first, then treated each of its four
diagnoses as a HYPOTHESIS and probed it. Base `2301acc`.

**One sentence.** Two defects are fixed and gated (TS-1's seventh bed arc,
TS-2's boarder predicate); **two of the close's four diagnoses turn out to be
wrong and one was never made at all**, so all four are now diagnosed to the
event; the v2 10^4 cell is UNBLOCKED and measured; the v2 10^5 cell is still
blocked, by a fifth defect this session found underneath TS-2 and rowed
rather than fixed.

## 1. Scope, and the fences it ran under

- **Rows before fixes.** Commit `fba3646` is docs-only and lands four `## Next`
  rows before a line of `src/` moved. The close's rowing claim is thereby made
  true late, and each row says so at the point of use rather than landing
  quietly -- this is the third instance of the class.
- **No fix without reproduction at the record's seed.** Every diagnosis below
  was reproduced at seed 20260824 on the close's own configs before anything
  changed. **The close's scratch survived on penny for the third arc running**,
  so `dense-750-v2.edn` and `dense-7500-{v2,nobed}.edn` are the same bytes the
  close measured, not a re-authoring. (An earlier reading of this session's own
  that the scratch was gone was wrong -- a truncated directory listing, not a
  missing directory. Recorded because the wrong reading nearly cost a
  reconstruction.)
- **One declared sweep at most.** None was needed: `bin/ground-truth-bracket`
  reads IDENTICAL across the whole session, so no shipped corpus reaches either
  fix.
- **A disappearing defect is not a fixed one.** TS-4 did not disappear and is
  characterised rather than closed; TS-3 did not disappear either.

## 2. The four diagnoses, verified or corrected

| | the close's diagnosis | verdict | the actual root |
| --- | --- | --- | --- |
| TS-1 | a reinstating cancel during `:cleaning` produces an arc the relation does not hold; the ENGINE is right | **CONFIRMED**, to the event | as stated; one detail added below |
| TS-2 | "nothing gates the authored pathway walk on the encounter's class" | **REFUTED** | `waiting-boarder` never asked whether a candidate was in a bed |
| TS-3 | "almost certainly the same root as TS-2 seen from the other side" | **REFUTED** | a `:cancel-discharge` re-opens an encounter nothing can close; a module-COMPILED opener then opens over it, unguarded |
| TS-4 | not characterised at all | **CHARACTERISED** | an ordinary churn `:merge` eats an open-window placeholder; the invariant counts only `:cause :identification` merges |

Two of the four readings the close carried forward were wrong. Both were
wrong in the same direction -- they named a plausible site one layer ABOVE
the real one -- and both were refuted by the close's OWN witness event,
which is the cheapest possible refutation and is why reproduction-before-fix
is the fence that earned its keep here.

### 2.1 TS-1 -- confirmed, and the seventh arc ratified (fixed, `19a4931`)

Reproduced at `dense-750-v2.edn`, seed 20260824: 16,322 events, 2 violations,
the same two beds at the same instants the close names (`ED-176` t=61920,
`SURGERY-34` t=402780). The SURGERY-34 window, verbatim:

    5563 t=401280 :transfer          PID-000473 OUT of SURGERY-34 (bed-ready)
    5564 t=401280 :bed-status-change SURGERY-34  :occupied -> :dirty
    5570 t=402540 :bed-status-change SURGERY-34  :dirty    -> :cleaning
    5571 t=402780 :cancel-transfer   reinstates PID-000473 into SURGERY-34
                                     -> the fold records [:cleaning :occupied]
    5573 t=402780 :discharge
    5574 t=402780 :bed-status-change SURGERY-34  :occupied -> :dirty

The close's reading of the mechanism holds exactly, including its claim about
`decide :bed-ready`'s guard: **no `:ready` event appears anywhere in the
window**, which is that guard no-opping on a bed that is no longer
`:cleaning`. The engine is correct and the check-side enumeration was
incomplete, so the fix is check-side and the brackets cannot move.

**One detail the close did not have.** At `ED-176` the reinstated patient is
NOT the bed's most recent occupant. `PID-000729` left `ED-176` at t=49800;
the bed then went through a full cycle and was handed to `PID-000730` by a
bed-ready transfer at t=51060; the cancel that fires at t=61920 reinstates
`PID-000729`, two occupancies later. **The arc is therefore not "the same
occupant returns"**, and the enumeration must not be re-narrowed to that on a
later reading. (Both witnesses at 750 patients are `:cancel-transfer`; a
`:cancel-discharge` can reach the same arc and the relation is stated for
both.)

Ratified as ADR-0174 section 2(c)'s fourth ratification, dated, and carried
in the same three places the sixth arc is. Ratification 1 had closed with
*"the enumeration stands against a seventh on the same terms"*; this is that
seventh, on those terms, and the wording now stands against an eighth.

### 2.2 TS-2 -- refuted, and the real root fixed (`1b4e264`)

**The close's own witness refutes the close's own diagnosis.** Log index
92836 of the `nobed` 10^5 cell, reproduced here byte for byte:

    92832 t=2842620 :outpatient-visit  ENC-001490-03, honouring APT-001490-01
    92836 t=2843280 :transfer  :from nil  -> Medicine A, MEDICINE-A-166,
                               :placement :licensed, :bed-ready TRUE
    92839 t=2843820 :outpatient-visit-end

`:bed-ready true` is a field ONLY `bed-ready-transfer-event` writes. No
authored pathway step produced this transfer, so "an outpatient encounter's
pathway can carry a `:transfer`" is not what happened. The reproducer built
for the red test settles it independently: its only pathway is
admission/delay/discharge, it contains no `:transfer` step of any kind, and
the defect fires anyway.

**The root, one layer down.** `waiting-boarder` asked three questions --
status is `:admitted`, `:home-ward` is this ward, the current location's ward
is not this ward -- and never asked whether the patient was in a bed at all.
An open outpatient encounter answers all three wrongly:

    evolve :outpatient-visit   sets :status :admitted, :class :outpatient,
                               and NO :location
    evolve :discharge          nils :location but LEAVES :home-ward

so a patient discharged from Medicine A who returns for a follow-up visit is,
for that visit's duration, `:admitted` with a nil location and a stale
Medicine A home ward. `(not= "Medicine A" nil)` answers yes. Their
`:admitted-at` is stale too, from the earlier inpatient stay, so `sort-by`
ranked them AHEAD of every genuine boarder.

The fix is one clause -- a boarder must hold a bed. `some?` rather than
`(not= :outpatient (:class p))` deliberately: over `:admitted` patients the
two select the same set (`admitted-occupies-one-slot` is what says so), and
this one says what a boarder IS rather than which class is today's exception.

**The draw analysis the fence asks for.** `waiting-boarder` consumes no
draws. The branch it now takes more often is the PRE-EXISTING zero-draw one:
both callers already emitted nothing and drew nothing on a nil boarder
(`decide :bed-ready`'s `{:events [ready-event]}`, `decide :discharge`'s own
nil `waiting-id`). So no draw is skipped differently -- a draw that should
never have been made is not made. For a genuine boarder nothing changes:
removing a non-boarder from a candidate set cannot reorder the rest. Where a
false boarder outranked a real one, the real one now gets the bed, which is
the correction itself rather than a side effect of it.

`engine_test.clj`'s own `boarding?` helper -- the SPECIFICATION copy of this
predicate, which the bed-ready property reads -- carried the identical hole
and gains the identical clause. Two copies of one predicate is a shape this
repo has been bitten by; they are kept in step and each says so.

### 2.3 The fifth defect, found underneath TS-2 and NOT fixed

TS-2's fix took the outpatient population from 24 patients to 12 at `nobed`
10^5, and from 25 to 13 at `v2` 10^5. **The remainder is a different root**,
and reporting TS-2 closed without saying so would have been the close's own
error repeated. Probed to the event, `PID-004302` at `nobed` 10^5:

    23030 t=264000   :admission        -> SURGERY-91, home-ward Surgery
    25384 t=288360   :transfer         -> MEDICINE-B-05, home-ward Medicine B
    26849 t=303660   :discharge        status :discharged, LOCATION NIL
    26851 t=303660   :appointment      the follow-up this discharge books
    26852 t=303660   :cancel-transfer  cancels 25384 -- and RESTORES
                                       :location SURGERY-91 and :home-ward
                                       Surgery onto a DISCHARGED patient
    93474 t=3068460  :outpatient-visit ENC-004302-01 opens; :class :outpatient
                                       with SURGERY-91 still held  -> RED

`PID-005562` is the same shape at t=363060. In both, the `:cancel-transfer`
lands in the SAME BATCH as the discharge.

**The question it raises is a design question: may a cancel reinstate state
that a LATER event has already superseded?** A `:cancel-transfer` says the
transfer did not happen; the discharge that followed it says the patient
left. Today the cancel wins and the bed is held for the rest of the run --
nothing ever vacates it, which is why 11 patients still produce 372,123
violations at `nobed` 10^5. Rowed as `roadmap.md#cancel-transfer-reinstates-a-discharged-patient`
and NOT fixed here, deliberately: `decide`/`evolve :cancel-transfer` is
reached by every shipped corpus carrying churn, so a fix is a candidate
declared sweep and owes its own session under the rows-before-fixes fence
this session opened by honouring.

### 2.4 TS-3 -- refuted, diagnosed, rowed

Not TS-2's other side. `PID-000640` is a MODULE-COHORT patient:

    27284 t=240300    :admission        ENC-000640-00 (module-compiled)
    27804 t=244620    :discharge        closes ENC-000640-00
    27806 t=244620    :appointment      a follow-up
    27807 t=244620    :cancel-discharge RE-OPENS ENC-000640-00 -- and the
                                        pathway's queue is already spent, so
                                        nothing will ever close it again
    ... 110,000 events of :bed-swap and bed-ready transfers ...
    137971 t=100609860 :outpatient-visit ENC-000640-01 opens OVER the open one

The guard that should have stopped this is `decide :repeat-arrival`'s
`encounter-openable?`. It never ran: **a module-COMPILED encounter opener is
queued directly and is not routed through `:repeat-arrival`**, unlike a
scheduled arrival or a follow-up, both of which `decide :appointment` wraps
for exactly this reason. So TS-3 belongs to the same FAMILY as section 2.3 --
a cancel reinstating state a later event superseded -- and should be weighed
with it, not with TS-2.

This patient is also one of the 13 residual `outpatient-patients-occupy-no-bed`
victims at `v2` 10^5, and legitimately so: their bed was allocated to them as
an inpatient, and the second opener merely flipped `:class` to `:outpatient`
underneath them.

### 2.5 TS-4 -- characterised for the first time

The close reported this un-probed rather than guessed at, which was right.
Probed now; one violation in EACH 10^5 cell, the SAME patient at the SAME
instants in both:

    t=37017   :registered  PID-007500, :identity :placeholder,
                           :alias-name Doe/Unknown, :window-close-t 382617
    t=37017   :admission   "Unidentified patient", Emergency
    t=80217   :discharge
    t=177420  :merge       surviving MRN002544, merged MRN007501
                           -- an ORDINARY CHURN MERGE, carrying no :cause

`every-placeholder-registration-is-resolved-or-still-open` counts as
resolution only `:demographic-update` with `:cause :identity-fill` and
`:merge` with `:cause :identification`. `decide :merge` (the M2b churn kind)
emits neither -- it writes `:role :merged` on its participant and no `:cause`
at all -- so the placeholder reads unresolved. And it can never become
resolved afterwards: once `:merged`, the patient is absorbing.

**Whether this is the engine's defect or the check's is a real design
question and is not answered here.** The engine reading is that churn must
not consume a placeholder whose identification window is still open, because
doing so destroys a flow the person layer set up. The check reading is that a
patient merged away by ANY merge has had their identity resolved in the only
sense a log can show. Rowed with both readings stated.

## 3. The blocked cells, re-run

Same driver, same scratch, same protocol and same preamble as the close
(`spike/driver2.clj`, six vars rebound around one real `run-command` call;
seed 20260824; warm-up plus two timed runs, one JVM per run,
`/usr/bin/time -v` around each; figures are the mean of the two). HEAD
`1b4e264`. Health record in section 4.

### 3.1 v2 10^4 -- UNBLOCKED, and MEASURED

| | events | messages | msg/event | modules | persons | generate | check | emit | spool | other | in-process | wall | peak RSS | self-check |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v2, 750 patients | 16,322 | 19,862 | **1.2169** | 0.317 s | 3.149 s | 3.596 s | 2.052 s | 2.693 s | 1.642 s | 0.959 s | 12.77 s | **20.74 s** | 713 MB | **CLEAN** |

Emit and spool run here for the first time on a v2 corpus, because the cell
had never completed. Fan-out spooled 2,218 `:adt-feed` and 4,657 `:bed-feed`
re-deliveries beside the 19,862 base messages.

**Against the same cell's blocked figures.** Persons 3.149 s against 3.156 s
and generate 3.596 s against 3.439 s: the fix is not measurable in generate.
Check reads 2.052 s against a parenthesised 2.005 s -- **and those are not the
same measurement.** The old one was a failing check materialising two
violations; this one is a passing check. That they nearly agree is a fact about
how cheap two violations are, not evidence that the phase is unchanged.

### 3.2 v2 10^5 -- STILL BLOCKED, with a new and much smaller reason

| | events | persons | generate | check | emit | spool | wall | peak RSS | self-check |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| v2, 7,500 patients | 171,913 | 39.93 s | 163.05 s | (84.84 s) | -- | -- | 300.9 s | 2,278 MB | **BLOCKED** |

**171,913 events against the close's 171,925 -- 12 fewer, which is exactly
the twelve erroneous bed-ready transfers the fix removes.** The census:

| invariant | close (2026-08-29, pre-fix) | post-fix |
| --- | --- | --- |
| `outpatient-patients-occupy-no-bed` | 897,579 | **495,205**, over 12 patients |
| `bed-cycle-transitions-are-legal` | 16 | **0** |
| `admission-only-when-no-open-encounter` | 1 | 1 (TS-3) |
| `every-placeholder-registration-is-resolved-or-still-open` | 1 | 1 (TS-4) |

Generate 163.05 s against 161.52 s pre-fix, +0.9% -- unmoved, as a predicate
change that consumes no draws should be. **The check figure is parenthesised
and is not a check measurement**: it is the wall of a FAILING check
materialising 495,205 violation maps, and it is below the pre-fix 88.81 s only
because there are fewer of them to build. The close's TS-6 says this and it is
repeated here rather than assumed read.

Distinct patients across the WHOLE violation set -- the statistic the close's
own probe reports -- falls **25 -> 13** at v2 and **24 -> 12** at nobed.

**The two cells' surviving populations are NOT nested, and the obvious
inference is wrong.** Asked for the ids rather than the counts, the
`outpatient-patients-occupy-no-bed` row flags **11 patients at nobed 10^5 and
12 at v2 10^5, sharing 10**: `PID-000442` is nobed-only, `PID-000640` and
`PID-002003` are v2-only. The tempting reading -- that v2 is nobed's set plus
TS-3's one patient -- is false in both directions, and it was checked rather
than reasoned to, which is the whole of why it is stated here.

**Emit and spool still never run, so no message figure exists at 10^5 on any
add-on-bearing corpus.** The gap the close named is unchanged.

### 3.3 Messages per event -- the v2 series gains its second decade

| corpus | events | messages | msg/event |
| --- | --- | --- | --- |
| old (no add-ons), 10^4 and 10^5 | 9,956 / 105,214 | 6,405 / 67,638 | 0.643 at both |
| v2 (nine keys), 10^3 | 1,488 | 1,562 | 1.050 |
| **v2 (nine keys), 10^4** | **16,322** | **19,862** | **1.2169** (new) |
| nobed (eight keys), 10^4 | 12,353 | 15,002 | 1.214 |

**The v2 series climbs 1.050 -> 1.217 across the decade and lands on top of
the `nobed` isolation series at the same scale.** That agreement is worth
stating rather than passing over: the bed cycle adds events and messages in
nearly the same proportion, so it barely moves the ratio -- which is also
what makes the close's decision to quote the isolation series' 1.214 as the
best available 10^4 figure look, in hindsight, sound.

**Still not measured, and still owed by nobody:** msg/event at 10^5 on any
add-on-bearing corpus. Both 10^5 add-on cells remain blocked, so 1.217 has
no second decade under it either. Stated rather than extrapolated -- the same
sentence the close wrote, still true, one decade further along.

### 3.4 Exponents

Only the two v2 phases that run at both 10^4 and 10^5 can be re-derived.
A first-decade slope is startup-contaminated and is printed for completeness
only, exactly as in the close.

| series | phase | 10^3 -> 10^4 | 10^4 -> 10^5 |
| --- | --- | --- | --- |
| v2 (post-fix) | persons | 0.734 | **1.079** |
| v2 (post-fix) | generate | 0.885 | **1.620** |
| v2 (post-fix) | check | 0.771 | (1.581) |

**Generate's second-decade exponent is 1.620 against the close's 1.635** --
the same super-linear shape, within the noise of a two-run mean, on a corpus
12 events smaller. Arc 0 removed the quadratics it named and did not make
generate linear; `roadmap.md#performance-residual-sites` still holds the
remainder. The person layer stays linear at 1.079. The check exponent is
parenthesised for section 3.2's reason and no claim is made about it.

## 4. Health record

Taken twice: once at session start, and once again after the first
measurement attempt was discarded (section 6, judgment call 5).

    $ date -Is                        -> 2026-08-29T07:48:18-04:00 / 08:03:16
    $ git log --oneline -1            -> 1b4e264 (both fixes in)
    $ git status --porcelain          -> empty
    $ git rev-parse --abbrev-ref HEAD -> main

    $ uptime -s   -> 2026-08-24 15:10:46   -- the SAME boot the close ran on,
                     and the same one ADR-0167's post-reboot baseline used
    $ uptime      -> up 4 days 16:52, load average 1.65 1.58 1.87
    $ free -h     -> 15Gi total, 2.4Gi used, 12Gi avail, 0B swap used
    $ df -hT .    -> /dev/sdd ext4 251G, 27% used   (NOT /mnt/c)
    $ nproc       -> 12

    $ java -version
    openjdk version "21.0.7" 2025-04-15
    OpenJDK Runtime Environment (build 21.0.7+6-Ubuntu-0ubuntu120.04)
    $ readlink -f $(which java) -> /usr/lib/jvm/java-21-openjdk-amd64/bin/java
    $ MaxHeapSize = 4162846720  ->  3.88 GB  {ergonomic}

    Windows side, 08:03 (before the kept cells):
      LoadPercentage             -> 0, 2, 3, 2, 9
      powercfg /getactivescheme  -> High performance
      Win32_Battery              -> BatteryStatus 2 (AC)

Same boot, same JVM, same heap ceiling and same disk as the close, so its
figures and these are on the same machine in the same state.

**Host during the kept cells.** Boundary samples, the series comparable to the
close's: **4, 5, 12, 15** -- inside the close's own 0-17 in-flight range. This
session also took an IN-FLIGHT series the close did not: 81 samples at ten
second intervals, **mean 9, max 51**, with 8 samples above 25. The sampler
spawns a `powershell.exe` each time and is therefore itself Windows-side load,
so those readings are an upper bound on the contention the cells actually saw
-- which is part of why the close sampled at boundaries only. Both series are
reported rather than the flattering one.

**The first attempt was discarded**, and this is the instrument finding of the
session. Its cells were started while the Windows side read **52-65**, against
the close's highest in-flight reading of 17: the author's desktop applications
were running. Its 10^4 wall came out **34-39 s** against **20.7-21.0 s** on the
quiet host -- a **1.7x instrument error**, comfortably large enough to have
been reported as a regression against the close's 15.41 s if it had been
believed. Both contaminated cells are kept in the scratch under a
`-CONTAMINATED` name rather than deleted, so the delta is checkable.

## 5. What landed

| commit | what |
| --- | --- |
| `fba3646` | docs-only: four `## Next` rows, TS-1..TS-4, before any `src/` moved |
| `19a4931` | TS-1: `legal-bed-transitions` gains `[:cleaning :occupied]`; ADR-0174 section 2(c) ratification 4; `operational-models.md`; the authored counted witness |
| `1b4e264` | TS-2: `waiting-boarder`'s `some?` clause, its specification copy in `engine_test.clj`, and the population red-test at the close's seed |
| `e932f9c` | the four rows re-scoped, two closed by sha and one new defect rowed; the plan appendix's blocked entries converted; this record and its prompt archive |

## 6. Judgment calls, and their ratification status

None is ratified; each is disclosed rather than folded in.

1. **The fifth defect (section 2.3) is rowed, not fixed.** It is the whole of
   what still blocks the 10^5 cells and fixing it was tempting at the point it
   was found. It was not fixed because it was not rowed when it was found, and
   because `:cancel-transfer` is reached by every corpus carrying churn -- so a
   fix is a candidate declared sweep, and this session had already spent its
   one-sweep budget's justification on a bracket that came back IDENTICAL. The
   counter-argument, stated fairly: a session that leaves the blocking defect
   in place leaves the cell blocked, and a later session pays the whole
   reproduction cost again. The scratch and the probes are preserved against
   exactly that.
2. **TS-1's witness is AUTHORED, not sampled.** The shape is zero-frequency in
   every shipped corpus, so there was nothing to sample. The mitigation is that
   the fixture's own `[:cleaning :occupied]` count is pinned `pos?` through
   `bed-transitions`, so a fold that recorded no transition cannot make the row
   pass silently.
3. **TS-2's red test is 150 patients and about 3 seconds of suite time.** A
   population test is slower than a hand-built log and was chosen anyway: the
   defect is an interaction between three opt-ins (`:encounters`,
   `:scheduling`'s follow-up, and the bed-ready coupling) and a hand-built log
   would have asserted the fix rather than the defect.
4. **The one-ward facility in that fixture is a deliberate over-constraint.** A
   facility with one ward can produce no genuine boarder at all, so every
   boarder it finds is a false one. That makes the test sharp and makes it
   unlike any shipped scenario; the fixture says so.
5. **The first measurement attempt was DISCARDED and re-run.** Its cells were
   started while the Windows side read 52-65 (`LoadPercentage`), against the
   close's own highest in-flight reading of 17, and its 10^4 wall came out
   34-39 s against 20.7-21.0 s on the quiet host -- a 1.7x instrument error
   that would have been reported as a regression. Both contaminated cells are
   kept in the scratch under a `-CONTAMINATED` name rather than deleted.
6. **The in-flight host sampler perturbs what it measures.** It spawns a
   `powershell.exe` every 10 seconds, and that process is itself Windows-side
   load. Its readings are therefore an UPPER BOUND that includes the sampler's
   own cost, which is a reason the close sampled only at cell boundaries. Both
   sets are reported.

## 7. Verification

**Both brackets, over the WHOLE session (`2301acc` -> `1b4e264`), both
IDENTICAL.**

    bin/ground-truth-bracket 2301acc HEAD
      -> IDENTICAL: every digested root's :ground-truth matches (38 roots)
         coverage: 38 digested, 3 skipped by name (appendicitis,
         ear-infections, sore-throat carry no :ground-truth key)
         declared-digest-change: no
    bin/regression-oracle 2301acc HEAD
      -> IDENTICAL: every root's digest matches

This confirms the close's own prediction rather than assuming it: **no shipped
corpus reaches either defect**, so no sweep is declared and the session's
one-sweep budget is unspent. The two scripts are run as a pair because
`rulings.md#R-oracle-script-contract` reserves the phrase "regression-oracle
claim" for the second -- the first excludes the `:hl7` half by construction and
could not make that claim alone.

The prediction was made from the witness counts BEFORE either script ran: the
close measured zero occurrences of TS-1's shape in every shipped corpus, and
TS-2 needs `:scheduling`'s follow-up producer and a bed freeing in a stale home
ward inside a twenty-minute visit. Both brackets agreeing is the confirmation,
not the discovery.

**`make test` -- EXIT 0.** 408 namespaces, **23,935 assertions, 0 failures, 0
errors**, `poly check` and `bin/verify-nist-lock` included. Run over the whole
tree rather than the touched bricks, per
`feedback_repo_gate_ordering`'s own lesson: hardcoded counts and the roadmap
lint's guards are invisible in a diff.

**`make integration` -- run TWICE, and the first run's failure is worth
recording.** Its first invocation exited 2 with **148 namespaces, 7,463
assertions, 0 failures, 0 errors** and every exerciser and use-case script
green through the MLLP one -- then failed its own last step:

    FAIL: tree not clean after a full run (ADR-0005 postcondition violated)

The tree was not clean because this record and the roadmap were still
uncommitted. **`make integration` postconditions on a clean worktree**, so it
cannot be run beside unfinished documentation. Noted here because the failure
reads like a test failure and is not one.

Re-run against the real tip `e932f9c`, tree clean: **EXIT 0**, 148
namespaces, **7,463 assertions, 0 failures, 0 errors**, both demo exercisers
and all five use-case scripts green, MLLP 273 sent / 273 acked with MSA-2
checked per pair.


## 8. Close

**What this session establishes.**

- **Two of the close's four diagnoses were wrong, and both were refuted by the
  close's own witness event.** TS-2's transfer carries `:bed-ready true`, which
  no authored pathway step can write; TS-3's patient is a module-cohort
  encounter re-opened by a `:cancel-discharge`, sharing no mechanism with
  TS-2 at all. The lesson is not that the close was careless -- it probed two
  of four to the event and said plainly that it had not probed the others --
  but that **a diagnosis and a probe are different artifacts, and a session
  that carries the first forward as the second inherits its errors.** This
  session's own prompt named the four as hypotheses, and that instruction is
  what caught both.
- **The relation is now seven arcs, and the enumeration stands against an
  eighth.** ADR-0174 section 2(c)'s ratification 1 wrote the sentence that made
  this cheap; it is worth writing again.
- **TS-4 is characterised**: an ordinary churn merge consumes an open-window
  placeholder, and the invariant's resolution set counts only
  `:cause :identification` merges.
- **A fifth defect was found underneath TS-2** and is what still blocks the
  10^5 cells. It was rowed, not fixed, and the reason is the fence.
- **`bin/ground-truth-bracket` reads IDENTICAL across the whole session** (38
  digested roots), so no shipped corpus reaches either fix and no sweep is
  declared. That is the close's own prediction, confirmed rather than assumed.

**What it does not establish.**

- Nothing about the `old` or `nobed` series is re-measured; only the two v2
  cells the close left BLOCKED were re-run.
- The v2 10^5 cell's check wall is still the wall of a FAILING check and is
  parenthesised wherever it appears, for exactly the reason the close's TS-6
  gives.
- No claim is made about whether the fifth defect, TS-3 and TS-4 are ONE
  design question or three. Sections 2.3 and 2.4 argue that two of them share
  a family; the third is independent; nobody has weighed them together yet.

**The finding worth more than the fixes.** `waiting-boarder`'s predicate had
been read by two reviews and copied verbatim into a property test that is the
specification of what it should do. Both copies had the same hole, and the
property could not see it because the property WAS it. A specification copied
from the implementation tests that the implementation agrees with itself --
the vacuous-gate shape this repository has now been bitten by three times.
What broke the tie here was volume: a population the copies were never run
against.
