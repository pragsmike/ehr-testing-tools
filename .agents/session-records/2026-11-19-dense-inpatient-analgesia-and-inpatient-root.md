# `dense-inpatient` writes its second medication order mid-stay; an oracle root for the inpatient dispense

2026-11-19. Base `3b6e836f`. HEAD matched origin/main and the tree was
clean. `bin/preflight` exit 0.

Prompt: `.agents/prompts/2026-11-19-dense-inpatient-analgesia-and-inpatient-root.md`.

**The pathway, read at the base.** The prompt's correction holds. After
the `:analgesia-start` order come an observation, both medication ends,
the care-plan end and the discharge, with no delay between them. There
is no later point for the end; the order moves earlier.

## Step 1: the prediction, written before any edit

The edit moves one step from index 15 to index 10 of 21. Read from
`churn.clj` before predicting: churn rolls once per churn type at every
gap whatever the neighbouring steps are, and a medication order changes
none of the state churn's legality table reads. So the same churn steps
land at the same gap indexes, and the five delays draw in the same
order.

**Will not move:** arrivals, subjects, and the count of every event
kind other than `:medication-order` and `:medication-dispense`. The
`:community` fills (48 at 7,500, 3 at 750).

**Will move:**

- Every log's bytes, `config-bare.edn`'s included: the analgesia order
  sits at a different instant and index in every stay.
- Orders written in admissions rise by a handful. A stay cut short by a
  merge between the old and new positions used to lose that order with
  the patient's other queued steps and now writes it first.
- Message counts, by a few either way, for the reason ADR-0189 gives.

| | 7,500 before | 7,500 predicted | 750 before | 750 predicted |
|---|---|---|---|---|
| orders written in admissions | 4,938 | 4,938 to 4,945 | 489 | 489 to 490 |
| `:inpatient` dispenses | 2,461 | 4,895 to 4,915 | 247 | 486 to 489 |
| undispensed: ended at the order's own instant | 2,444 | 0 | 240 | 0 |
| undispensed: the stay closed first | 31 | 31 to 40 | 2 | 2 to 3 |
| undispensed: merged away first | 2 | 2 to 6 | 0 | 0 to 1 |
| undispensed fraction | 50.2% | under 1% | 49.5% | under 1% |
| events | 170,224 | about 172,670 | 33,546 | about 33,786 |

Order to dispense on an inpatient ward: n rises from 51 to about 2,500
at 7,500, still inside 15 to 60 minutes, median near 37. The ED row is
unchanged in n and spread.

**Not predicted, flagged:** the mutation gate's dense population
(`event_mutate_test`). ADR-0189's site predicates give up every order a
dispense cites. Until now the undispensed analgesia orders were the
sites left; with nearly every order dispensed an operator may be left
with no site.

## Step 2: the edit

`b6fb4ab3`. `git diff` on `config.edn` is the one step moved: four
lines out, the same four lines in, six steps earlier. The three
siblings were re-derived by the README's commands. `sim check` ok over
the new 750 cell. Every cell's warm-up and two timed runs wrote one
byte-identical log. The tree has no dense case of the erasure test;
none was added.

## Step 3: measured, beside the prediction

Seed 20260824, `--churn`, at `b6fb4ab3`.

| | 7,500 predicted | 7,500 measured | 750 predicted | 750 measured |
|---|---|---|---|---|
| orders written in admissions | 4,938 to 4,945 | 4,943 | 489 to 490 | 493 |
| `:inpatient` dispenses | 4,895 to 4,915 | 4,912 | 486 to 489 | 491 |
| undispensed: ended at own instant | 0 | 0 | 0 | 0 |
| undispensed: the stay closed first | 31 to 40 | 29 | 2 to 3 | 2 |
| undispensed: merged away first | 2 to 6 | 2 | 0 to 1 | 0 |
| undispensed fraction | under 1% | 0.63% | under 1% | 0.41% |
| events | about 172,670 | 172,867 | about 33,786 | 33,819 |
| `:community` fills | 48 | 48 | 3 | 3 |

The full table, all cells:

| cell | admission orders | dispensed | undispensed (closed / merged) | ED lag n; min/med/max | inpatient lag n; min/med/max | community |
|---|---|---|---|---|---|---|
| `config.edn` 7,500 | 4,943 | 4,912 | 31 (29 / 2), 0.63% | 2,720; 5/15/25 | 2,192; 15/38/60 | 48 |
| `config-nobed.edn` 7,500 | 4,953 | 4,921 | 32 (30 / 2), 0.65% | 2,701; 5/15/25 | 2,220; 15/38/60 | 48 |
| `config-bare.edn` 7,500 | 5,915 | 0, no key | -- | -- | -- | 0 |
| `config.edn` 750 | 493 | 491 | 2 (2 / 0), 0.41% | 269; 5/15/25 | 222; 15/37/60 | 3 |
| `config-28024.edn` 28,024 | 20,168 | 20,121 | 47 (41 / 6), 0.23% | 10,949; 5/15/25 | 9,172; 15/37/60 | 59 |

No lag is outside its ward class's range. Every dispense carries its
order's `:encounter-id`. At 7,500 the dispensed are 2,459 antibiotic
and 2,453 analgesia; all 29 closed-first orders are vendored modules'.

**Where the prediction was wrong.**

1. **"Will not move: every other kind."** Wrong. At 7,500, 27 kinds
   moved, most by under ten (`:demographic-update` +91, `:merge` +8,
   `:admission` +3); at 750, 21 kinds. `config-bare.edn` moved too, by
   -51 events with no key, so the cause is the moved step and not the
   dispenses. Churn does roll the same types at the same gaps, as
   read. What the reading missed: the steps a stay cut short has
   already taken are different ones, so the log differs from there,
   and every later draw on a stream shared across patients follows.
   The mechanism was not isolated further than the keyless witness.
   Subjects did not move in the cells compared (distinct participant
   ids at both 7,500 cells and at 750; `describe`'s 32,080 at 28,024).
2. **750: 493 admission orders against 489 to 490.** Four stays that
   used to be cut between the two positions now write the order, not
   one.
3. **`config-nobed.edn` no longer has `config.edn`'s orders and
   dispense counts** (4,953 / 4,921 against 4,943 / 4,912). Not
   predicted either way.
4. **The stay-closed-first count fell at 7,500** (31 to 29) where a
   rise was predicted: those orders are modules', and the reshuffle
   moved them.

Events before and now: 170,224 to 172,867; 128,209 to 130,887; 101,120
to 101,069; 33,546 to 33,819; 681,741 to 691,758. Messages: 176,956 to
177,104; 134,895 to 135,033; 65,643 to 65,580; 35,920 to 35,947.

## Step 4: figures

Drill as ADR-0189's: warm-up and two timed runs per cell, Windows-side
load 1 to 15 at every start. Walls 127.35 / 105.20 / 52.73 / 55.45 s;
peak RSS 1,848 / 1,701 / 1,098 / 1,191 MiB. The 28,024 cell, one run,
`-Xmx8g`: 691,758 events, 32,080 subjects, 270,551,734 bytes, 373.04 s,
3,559 MiB, sha-256
`26e31b4f44df3c9cd52a0429f15f0262f3dde4a6dec7575f2ee8bea845b652b0`.
`ce0982de...` is kept under `:pre-order-move`.

`bin/demo-exerciser-dense-7500` rewrote `:asserted` to the drill's own
counts; its one FAIL was the tree-clean postcondition, with the docs
uncommitted. `describe` over the new 750 cell: inpatient row
configured, observed 491; community row configured, observed 3; report
1.3.0.

**What the suite showed moved: the mutation gate** (`8340c4e3`, its own
commit). The flagged risk landed. `:orphan-closed-medication-order`
and `:inverted-span-medication-end-order-event-id` had no site in any
population. Fixed with a fourth population, the dense config less its
`:dispense` key. `config-bare.edn` was tried first and fails three
orphan operators, which convict one invariant fewer without
`:encounters`. `brick:corpus` takes 17 minutes 17 seconds with it.

## Step 5: the root

`e0bbcdee`. `dispense-inpatient`: 72 events, 15 orders, 9 dispenses
(three each in the ED, on Renal at the class default, on Cardiology at
its own `[40 50]`), 6 undispensed (3 ended at once, 3 outrun by the
discharge). The engine test `the-inpatient-root-holds-each-case-by-name`
asserts those by name and self-checks. No `:transfer`: the list of
roots carrying one is pinned and this root does not move it.

`bin/regression-oracle b6fb4ab3 e0bbcdee --declared-digest-change` and
`bin/ground-truth-bracket` likewise: every existing root's digest
identical, one line added on the target side, `dispense-inpatient`.
Pins: 45 to 46 roots, 42 to 43 engine pairs.

## Step 6

A dated addendum to ADR-0189 closes its two items with the table above.
No ADR was written: the tree's rule asks for none for a scenario edit
or a root.

## What this prompt got wrong, and what the session found

1. **"The patient is then in Medicine A when it is written."** For
   about nine in ten. At 7,500, 2,720 dispensed orders were written
   with the patient placed in the ED against 2,459 antibiotic orders,
   so roughly 260 analgesia orders were too. They are dispensed all
   the same, on the ED's range.
2. **"Inpatient dispenses roughly double to about 4,900."** 4,912.
   Right.
3. **"About 1%."** 0.63% at 7,500, 0.23% at 28,024. Right, and lower.
4. **"The run may still move beyond the medication kinds. Not known."**
   It does, in every cell.
5. **Not in the prompt: the mutation gate.** Above.
6. **Not in the prompt: two stale literals in the scenario README**,
   left by the ADR-0189 session when the wards gained
   `:dispense-minutes`: the `diff` fence's line number (`2113d2112`,
   really `2117d2116`) and the bare prefix (`154,195` bytes,
   `a47c5b8c...`, really 154,382 and `66b2691e...`). Neither is gated:
   the exerciser re-derives both. Corrected here.
7. **Not in the prompt: the consumer doc's wall sentence** said the
   walls rose "again at this one" a session after they had fallen.
   Rewritten.
8. `bin/preflight` was clean; the prompt allowed for a red run that
   has aged out of its window.

## Downstream line

`dense-7500`'s admission-written orders are now dispensed at the
measured rate: 4,912 of 4,943 at 7,500, 20,121 of 20,168 at 28,024.
Every cell's ground truth moved, the keyless one included. Pin: the
tip of this session. Event schema 1.12.0, unchanged. New 28,024 log:
270,551,734 bytes, sha-256
`26e31b4f44df3c9cd52a0429f15f0262f3dde4a6dec7575f2ee8bea845b652b0`. A
retained corpus is a valid artifact of the earlier pin.

## Close

Appended by the close commits.
