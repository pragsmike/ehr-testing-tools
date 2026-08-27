# Session record -- arc 3b, sweep 2 of 3: the bed-status cycle, with ADT^A20

**Date:** 2026-08-27
**Prompt:** [`.agents/prompts/2026-08-27-arc-3b-bed-cycle.md`](../prompts/2026-08-27-arc-3b-bed-cycle.md)
**Base:** `9fc9df5` -- **Tip:** `cb81dff`, plus the two commits that can
only follow it (this record, and the CI marker)
**Mode:** payload session under the de-scaffold moratorium. R30 ceremony,
no tag, CI green at the tip as the close marker.

## Scope

ADR-0174 section 2(c) -- the BED-STATUS CYCLE -- under rulings C, D1 and
E1, landed dark and then turned on, alone, as ruling E1's three sweeps
require. Ruling C's ADT^A20 landed with it, which is the author's own
addition to what section 2(d) had recommended (nothing new on the wire).
Sweep 3 (scheduling) is untouched.

| commit | what |
|---|---|
| `95302f6` | the bed cycle, DARK behind `:bed-cycle` -- oracle IDENTICAL, 37 roots, no declaration |
| `cb81dff` | the cycle, TURNED ON in six corpora, plus `bed-cycle`, the 38th root |
| *(this one)* | this record, the prompt, the roadmap |

## The two oracle lines, verbatim

```
--- declared-digest-change: no (soundness: yes outside the leading docstring) ---
IDENTICAL: every root's digest matches between 9fc9df5 and HEAD
```

```
--- declared-digest-change: yes (soundness: no outside the leading docstring) ---
DIFFERS: digests diverge between 95302f6 and HEAD -- STOP, escalate (do not fix under this script)
+3433f3529ccbbf2d11e9c305ef0945a3557bd150579bd4bfd024930bbf6cc700  bed-cycle.edn
```

**37 baseline rows, 38 target rows, and the whole diff is that one added
line.** Not one pre-existing root moved a byte in either commit.

## The gates

| gate | result |
|---|---|
| `bin/preflight` | no findings, exit 0, at session start |
| `bin/regression-oracle 9fc9df5 95302f6` | **IDENTICAL**, exit 0, no declaration, **37 roots** each side |
| `make test` (dark, at `95302f6`) | **MAKE_EXIT=0**, 4,394 tests / 20,668 assertions, 0 failures, 0 errors, 1,202s |
| `make test` (on) | **MAKE_EXIT=0**, 4,394 tests / 20,668 assertions, 0 failures, 0 errors, 1,214s |
| `bin/regression-oracle 95302f6 cb81dff --declared-digest-change` | **DIFFERS by exactly ONE manifest line**, exit 1, 37 baseline + 38 target roots |
| `make integration` at the tip | **INT_EXIT=0**, 1,313s, zero `FAIL:` lines, 1,656 tests / 5,983 assertions; both demo exercisers "every command asserted, every named invariant held, tree clean", and all five use-case scripts clean |
| `clojure -M:poly check` | OK at both commits |

`make test` figures are the sum over BOTH project runs the polylith
runner performs, which double-counts bricks shared by two projects --
the same convention the previous four records use, kept so the numbers
are comparable to theirs. The test and assertion counts are IDENTICAL
across the dark and ON runs, which is the right answer: the ON commit
adds no test, it re-points existing ones at a corpus that moved.

## The witness table -- the coupling, moved

The headline is not the event count. It is that **every bed-ready
transfer this repository had was a zero-second re-occupancy, and none
is now.**

| corpus | events | `:bed-status-change` (dirty/cleaning/ready) | ADT^A20 | bed-ready transfers | 0-second bed-ready |
|---|---|---|---|---|---|
| `seed-202-ed-tuesday` | 711 -> **1,131** | 418 (140/139/139) | 418 | 10 -> 12 | **10 -> 0** |
| `seed-424242-clinic-decade` | 1,309 -> **1,660** | 351 (117/117/117) | 351 | 0 -> 0 | 0 -> 0 |
| `seed-5-clinic-decade` | 1,072 -> **1,342** | 270 (90/90/90) | 270 | 0 -> 0 | 0 -> 0 |
| `adhd-seed-45` | 68 -> **92** | 24 (8/8/8) | 24 | 0 -> 0 | 0 -> 0 |
| `ed-tuesday` (demo) | 745 -> **1,151** | 406 (136/135/135) | 406 | 1 -> 1 | **1 -> 0** |
| `clinic-decade` (demo) | 1,156 -> **1,456** | 300 (100/100/100) | 300 | 0 -> 0 | 0 -> 0 |

Wire side, at `ed-tuesday`: **333 -> 739 messages, of which 406 are
ADT^A20**, and PV1-19 now renders on **340 of 341** PV1 segments (it was
328 of 333 -- four of the five blanks moved inside an open encounter
when the run reshuffled; the one that remains is an ORU^R01 result
arriving after its patient's discharge). An A20 carries no PV1 at all,
which is why 739 messages hold only 341 PV1 segments between them.

All twelve of seed-202's bed-ready transfers now sit at their own bed's
READY event, asserted rather than inferred. The one demo boarder's wait
grew 6,000 s -> 8,580 s.

**FOUR OF THE SIX CORPORA BOARD NOBODY**, and that is stated in their
own configs rather than left to be inferred from an unchanged transfer
count. For them the opt-in buys the A20 stream and nothing else. Only
the two ED corpora have a discharge-to-relief coupling for the cycle to
move at all.

### The residual 0-second transitions, classified

The raw "vacate -> next occupy at 0 s" count at seed-202 is 28 -> 15,
not 28 -> 0, and the difference is entirely explanatory rather than a
shortfall. Measured by the pair of event kinds at each end:

| vacate -> occupy | OFF | ON | |
|---|---|---|---|
| `discharge -> bed-ready transfer` | 10 | **0** | the coupling the cycle moves |
| `discharge -> admission` | 1 | **0** | a fresh admit taking a just-vacated bed |
| `transfer -> admission` | 2 | **0** | the same, on a transfer's origin bed |
| `transfer -> cancel-transfer` | 8 | 8 | `:transfer-in-error`'s atomic pair -- a correction |
| `discharge -> cancel-discharge` | 1 | 1 | the reinstatement arc |
| `bed-swap -> bed-swap` | 6 | 6 | a swap vacates and re-occupies at one instant |

**Thirteen real zero-second re-occupancies went to zero.** The fifteen
that remain are exactly the three arcs the design puts OUTSIDE the
cycle, which is independent confirmation of the `:bed-swap` exclusion
and of both cancel arcs.

## The board -- the consumer R-mix-6 names, now fed

Before this sweep a bed with no patient in it was INVISIBLE on the
whiteboard: a room being turned over looked exactly like a room standing
free. `ehrt play --board` over `demos/scenarios/ed-tuesday` now renders
45 `(dirty)`/`(cleaning)` lines across its 128 snapshots:

```
-- board snapshot: 2026-08-11T05:00:00Z --

Emergency:
  ED-H02  Garcia, Michael  MRN MRN000013  inpatient  attending: 5761303028
  ED-H03  Wilson, Amanda  MRN MRN000016  inpatient  attending: 5761303028
  ED-H04  Miller, Deborah  MRN MRN000017  inpatient  attending: 5761303028
  ED-H07  Johnson, James  MRN MRN000012  inpatient  attending: 5761303028
  ED-H08  Gonzalez, Joshua  MRN MRN000010  inpatient  attending: 5761303028
  ED-H10  Gonzalez, Emily  MRN MRN000015  inpatient  attending: 5761303028
  ED-H15  Martinez, James  MRN MRN000007  inpatient  attending: 5761303028
  ED-H06  (dirty)
  ED-H13  (cleaning)

inpatients: 7  active outpatients: 0  discharged: 7  merged: 0
```

A `:ready` bed is still not listed: an available bed is the normal case,
and listing every one would bury the two states a charge nurse is
looking for. `bin/demo-exerciser-ed-tuesday` now asserts BOTH states
appear -- `(dirty)` alone would pass on a board that never advanced a
bed past the vacate, `(cleaning)` alone on one that never showed the
vacate at all. Its own line in the green run reads:

```
  bed board: 16 dirty and 29 cleaning bed lines rendered across the snapshots
```

## The filters, counted

**THREE, and the count stops at three.** `events-by-patient`,
`participant-ids-exist-in-run` and `participants-of` now scope to
participants that CARRY a `:patient-id`. `engine/replay` and the run
loop's own two folds (the `evolve` fold and the `state-history` fold)
filter at source for the same reason, which is why no fourth invariant
needed one.

## The defect the unit gate caught, and it was a VACUOUS PASS

check.clj's log-side bed fold first stored bare status keywords
(`{bed :ready}`) while `sim-model/free` reads `(:status (get beds id))`.
Every bed therefore read as not-ready, `earlier-rungs-exhausted?`
returned true for everything, and
`surge-only-when-earlier-rungs-exhausted` went SILENT on every cycle log
while looking perfectly clean. A population smoke run reported `:ok` and
was wrong.

What found it was the hand-built dirty-vs-ready PAIR -- the same
placement asserted legitimate over a `:dirty` rung-1 bed and a violation
over a `:ready` one. The negative half passed vacuously; only the
positive half could fail, and it did. The index shape is now documented
in the fold as part of the contract with `free` rather than as a private
detail of the fold.

## Judgment calls, and their ratification status

**1. Invariant 5 could not be "asserted UNCHANGED", and was re-read.**
The prompt lists invariants 4 and 5 together as unchanged. ADR-0174's
own item 5 says the opposite in as many words -- it *"changes meaning
and must be re-read"* -- and the tree agreed: unmodified, the row fired
twice on the first opted-in run, because a surge placement made while a
rung-1 bed sits empty-but-`:dirty` is legitimate under the cycle and a
violation without it. Its three `(remove board ...)` calls became three
`sim-model/free` calls -- the same predicate the ladder asks, which
ADR-0174 names this function specifically as owing. The CLAIM is
unchanged; the reading of "exhausted" is the ladder's own. Fix-forward
with disclosure under
`rulings.md#R-stop-only-on-two-defensible-readings`: a mechanical
conflict with one defensible reading, not two.

**2. A SIXTH transition arc the ADR does not name.** Section 2(c)
enumerates ready->occupied, occupied->dirty, dirty->cleaning,
cleaning->ready and the reinstatement's dirty->occupied, and says the
relation is enumerated *"so a new writer cannot invent a fifth"*. It did
not reach the two cancel classes that VACATE a bed. `:cancel-admit`, and
`:cancel-transfer`'s own erroneously-taken bed, return straight to
`:ready` with no event and no turnaround -- an occupancy a cancel
retracts leaves no dirt behind it. Without that arc the bed stays
`:occupied` for the rest of the run and its ward silently loses
capacity, which no reading of 2(c) intends. Named in three places
(`bed-correction-event-types`, `legal-bed-transitions`,
`operational-models.md`) rather than added quietly. **Unratified.**

**3. `:turnaround-minutes` is ONE key drawn TWICE.** ADR-0174 ruling D1
gives each `Ward` a `:turnaround-minutes` for *"the dirty->cleaning delay
and the cleaning->ready delay"* without saying whether that is one value
or two. It ships as a `[lo hi]` range that EACH LEG draws from
independently, so a ward's whole turnaround runs `[2*lo, 2*hi]`. Two
keys would have made a config author state a decomposition of
housekeeping that no site reports separately. `{:optional true}` with a
per-class fallback (`{:ed [5 15] :inpatient [15 30]}`), so a facility
config written before this sweep keeps validating. **Unratified.**

**4. Fixed draw consumption, deliberately unlike `decide :delay`.**
`turnaround-seconds` always draws, even where `lo` = `hi` makes the draw
arithmetically dead. `decide :delay` skips that draw, and paid for the
skip with a whole-corpus reshuffle in its own commit; drawing
unconditionally here means a site tuning one ward to a fixed turnaround
shifts no other ward's cycle and no other patient's stream.

## ADR premises the tree contradicted

**1. `:exhausted` is not a visible `:step-rejected`.** Section 2(c) item
4 argues the effective-capacity risk is acceptable because *"`allocate`
returns `{:exhausted true}` and the engine emits a `:step-rejected` with
a documented reason -- so the failure is VISIBLE."* It is not. The run
loop HALTS on `exhausted` and `run-command` surfaces
`:error :capacity-exhausted`; the corpus is not produced at all.
`:step-rejected` belongs to a different family entirely (illegal
cancel/bed-swap/merge). This was not theoretical: **three candidate
configurations for the new oracle root exhausted the ladder outright and
were rejected for it** before one was found that contends without dying.
The six shipped corpora were re-probed at the turnaround this sweep
ships and all six run `:ok`.

**2. The zero-second census figure is not comparable.** The prompt cites
ADR section 1(ii)'s *"7 of 102 vacate->occupy transitions are at 0 s"*.
That census predates sweep 1; at this HEAD the same measurement on
seed-202 is 28 of 137, and the 7 is not recoverable. What is comparable,
and is what the headline uses, is the count of bed-ready transfers
sharing their discharge's instant: 10, going to 0.

**3. `bin/regression-oracle` cannot be run before the commit.** It takes
two REFS and builds a worktree per side, so the dark proof is taken
after the dark commit rather than before it. Noted because the prompt's
step 1 reads as though the bracket precedes the commit.

## The event contract: 1.5.0 -> 1.6.0, OWED by twenty-three reasons

`classify-change` against the frozen 1.5.0 baseline returns
`:additive? false`, with one entry per PRE-EXISTING kind:

```
:admission: key changed: :participants (value schema changed)
... twenty-two more, one per kind, identical in shape ...
```

The NEW KIND is additive and correctly absent from that list.
`:participants` is reported on all twenty-three because `Participant`
became `[:or PatientParticipant BedParticipant]`, one level down inside
`[:vector Participant]` -- the same conservatism that produced 1.5.0's
single `:bed-swap` reason, applied twenty-three times.

A 1.5.0-era log validates unchanged: every participant such a log
carries goes through the first branch. **But the breaking direction is
REAL here in a way 1.5.0's was not**, and it is not papered over: a
consumer written against 1.5.0 may reasonably have assumed
`:patient-id` is present on every participant of every event -- the
schema said so -- and a 1.6.0 log carrying a `:bed-status-change` breaks
that on real data. The obligation is one line, PARTITION A LOG BY
PARTICIPANTS THAT CARRY A `:patient-id`, and this repository took it in
three places in the same change. Baseline re-frozen with
`make event-schema-freeze`.

## The wire, and the MSH-12 question answered rather than asserted

ADT^A20 renders `[MSH EVN NPU]` and nothing else. It is a SIBLING of
`single-subject-message`, not a branch inside it: that builder's
contract is a PID/PV1 pair per subject, and an A20 has no subject to
pair. NPU-1 is the same PL `location-field` PV1-3 already renders; NPU-2
goes through `site-profile`'s own code-table seam (HL7v2 Table 0116,
`:occupied` -> `O`, `:ready` -> `U`, `:dirty` -> `K`, `:cleaning` -> `H`),
so a site that reports the whole vacated-to-available window as `H`
overrides one table rather than forking the cycle.

**WHERE THE VERSION WAS CHECKED, and what could not be.** What is
checkable in this clone is A20 in **2.4**: `components/judge-v2-hapi/
deps.edn:9` pulls `ca.uhn.hapi/hapi-structures-v24` 2.6.0, and that jar
carries `ca/uhn/hl7v2/model/v24/message/ADT_A20.class` and
`ca/uhn/hl7v2/model/v24/segment/NPU.class`, with A20 absent from
`ca/uhn/hl7v2/parser/eventmap/2.4.properties` -- i.e. not aliased onto
another structure, it has its own. **There is NO 2.3 trigger table
anywhere in this clone**: only v2.4 structures resolve, `~/.m2` holds
`hapi-structures-v24` alone, and neither `hapi-base` nor any repository
resource carries a 2.3 eventmap. MSH-12 stays `"2.3"`
(`site_profile.clj`'s own default, untouched by this family) on the
author's ruling and this session's fence, recorded as a ruling in
`bed-status-message`'s own docstring rather than dressed up as a
verified fact. ADR-0174 section 2(d) asked for *"either a version
decision or an in-tree source for 2.3's trigger table"*; this is the
former, and the latter is still absent.

## The new oracle root, and why it is worth more than the cycle

`bed-cycle` is the 38th root: sixty arrivals at a 90-second gap over a
facility with four licensed beds per inpatient ward, `--churn` on,
`:bed-cycle` on. 527 events, 467 messages, 295 `:bed-status-change`
events over 26 distinct beds, 43 transfers of which 39 are bed-ready and
ZERO share their discharge's instant.

It moves three things sweep 1 named as unreachable:

* **RUNG 4 IS NO LONGER ZERO.** The ladder reaches all four rungs -- 45
  rung-1, 17 rung-2, 10 rung-3, **31 rung-4**. Sweep 1's own coverage
  note said *"Rung 4, `:forced` and `:exhausted` are still zero across
  all 37 ... the part arc 3b sweep 2 will have to [move]"*. It moved for
  a reason worth keeping: a `:ready` gate makes a small ward CONTEND
  where an ungated one did not.
* **`:cancel-discharge` leaves the unwitnessed set**, so the churn
  family is whole for the first time -- and **ADT^A13** arrives with it,
  which `witnessed-message-types`' own docstring named as emitted by no
  root.
* **ADT^A20** joins, unavoidably one root deep since no other root can
  emit one.

`witnessed-event-kinds` moves 19 of 23 -> **21 of 24**; what is left
unwitnessed is the order->result path alone (`:order-placed`,
`:result-available`, `:step-rejected`). `witnessed-message-types` moves
9 -> **11**. `:forced` and `:exhausted` are still zero, and `:exhausted`
is now named precisely in the coverage block: it is not a degraded
outcome a root could carry, it halts the run, so a root witnessing it
would be a root that produces no corpus.

## What re-pinned, and nothing outside this list

* the four gated fixtures and their four digests (`run_test`'s
  `arc0-pinned-digest`);
* the invariant catalog roster pin, **36 -> 39** (dark commit) -- all
  three added rows are VACUOUS on a log with no `:bed-status-change`,
  which every corpus that pin covers still was at the dark commit;
* the closed-vocabulary count, **23 -> 24**, in four places
  (`event_schema_test`, `event_log_doc_test`, docs-tooling's
  `oracle_coverage_test`, person-simulator's `limitations_test`) plus
  one prose mention in `emittable_events.clj`;
* the oracle root count, 37 -> 38, in docs-tooling's push-lane gate and
  in `.agents/state-derived.md` (generated);
* the integration tier's three: 37 -> 38 `.edn` files, 34 -> 35
  engine-layer roots, and the CAPACITY-WITNESS ROOT LIST,
  `["death-fixture" "encounter-horizon"]` ->
  `["bed-cycle" "death-fixture" "encounter-horizon"]`;
* both scenario READMEs, from freshly regenerated runs;
* `bin/demo-exerciser-ed-tuesday`'s batch count, **106 -> 135**, and its
  straddle assertion's own bucket, `batch-001` -> `batch-002` -- the bed
  cycle redrew that discharge's own sampled transmit delay and pushed it
  a whole bucket further out, which makes the README's straddle point
  harder rather than softer;
* `.agents/state-derived.md`, `docs/formats.md`, the event-schema export
  and its baseline, and `event-examples.edn`, all generated.

**CORRECTED while re-witnessing, and both predate this session:**
`demos/scenarios/ed-tuesday/README.md` claimed *"the same 375
ground-truth events either way"* in its second-clock section -- a
figure witnessed before ADR-0171's stream partition and one of the stale
tokens `roadmap.md#post-partition-narrative-refresh` counts in that
file. And its batching section's closing sentence named *"Smith's own
admission and discharge"* for a paragraph about Hernandez, Sandra; the
same wrong name was in `bin/demo-exerciser-ed-tuesday`'s own two failure
messages.

## What did NOT move, verified rather than assumed

* `demos/traces/` is byte-identical across a full `make docsgen` at the
  DARK commit -- all 24 tracked files, one aggregate sha256 taken before
  and after (`670ce67e...`);
* the four gated fixtures, both v2 conformance baselines and every
  generated doc were untouched by the dark commit except
  `docs/formats.md`, the event-schema export/baseline and
  `event-examples.edn`, which moved for the schema change alone;
* **`config-latency.edn` still produces byte-identical ground truth to
  `config.edn`** -- `729f07ad...` both sides, the second-clock
  zero-offset identity (ADR-0109) that the exerciser's own `diff`
  asserts. `:bed-cycle` is engine-facing, so it had to land in both
  files, exactly as `:encounters` did in sweep 1;
* the encounter figures at `ed-tuesday` -- 127 openers, 14 patients with
  more than one, 15 recovered, max 3 -- are unchanged by the cycle,
  which moves WHEN a bed changes hands and not who is admitted.

## Red before green

Every gate this session added was proved red first, by neutering the
code it checks and running the real suite over the two bricks that hold
these gates. Counts, not filtered:

* `no-assignment-to-a-non-ready-bed` returning nothing -- **3 failures**;
* `every-ready-follows-a-cleaning` returning nothing -- **2 failures**;
* `bed-cycle-transitions-are-legal` returning nothing -- **3 failures**;
* `sim-model/free`'s `:ready` branch removed (falling back to
  "not a key in board") -- **3 failures**, in three different
  namespaces: the predicate's own unit case, the ladder's
  dirty-bed case, and invariant 5's re-read;
* `:bed-cycle` removed from the fixture fleet's own run -- **1
  failure**, `every-declared-kind-is-actually-produced`, which is the
  gate that stops a schema kind from rotting unproduced.

The three invariants are ALSO each proved to fire on a MUTATED corpus
rather than merely to be non-empty -- drop the READY leg and the next
allocation is caught taking a `:cleaning` bed; drop the CLEANING leg and
the ready transition is caught following a `:dirty` one; rewrite a
transition's `:to` and the invented arc is named back.

## Performance, disclosed

`roadmap.md#post-partition-narrative-refresh`'s sibling row,
`roadmap.md#performance-residual-sites`, counts the independent
`engine/replay` calls in `check.clj` (~40% of the check phase). This
sweep adds **three**, not four: `bed-fold` returns the replay records it
already walked, so invariant 5 reads the bed index it needs without a
second replay, and invariant 3 folds once for both of its clauses. All
three new rows short-circuit on a cheap linear scan
(`bed-cycle-log?`) before replaying at all, so a corpus without the
cycle pays nothing.
