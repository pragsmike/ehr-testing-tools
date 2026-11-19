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
