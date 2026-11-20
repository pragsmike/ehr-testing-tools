# dense-7500 authors outpatient visits: three weighted pathways, half of pathway arrivals

2026-11-20. Base `77cc8f54`. HEAD matched origin/main and the tree was
clean. `bin/preflight` exit 0, no finding.

Prompt: `.agents/prompts/2026-11-20-dense-outpatient-pathways.md`.

## Step 1: the prediction, written before any edit

**The edit predicted.** Three pathways join `config.edn`'s weighted
pool at weights 50 / 20 / 30 beside the existing 45 / 35 / 20, so each
arrival that draws a weighted pathway takes a clinic visit with
probability one half:

- `dense-clinic-acute`: outpatient visit, delay, one observation, one
  medication order, delay, visit end.
- `dense-clinic-chronic`: the same with a long-term medication.
- `dense-clinic-review`: the same with no order.

**The baseline, counted from the logs the 2026-11-19 drill left**
(seed 20260824, `--churn`, at `b6fb4ab3`; top-level events only):

| | 750 | 7,500 | 28,024 |
|---|---|---|---|
| arrivals that draw a weighted pathway (arrivals less the walker ordinals) | 657 | 6,563 | 27,087 |
| admissions opened by the three pathways (abdominal / chest / appendicitis) | 547 (247 / 184 / 116) | 5,387 (2,466 / 1,851 / 1,070) | 22,113 (10,072 / 7,682 / 4,359) |
| the same per weighted arrival | 0.833 | 0.821 | 0.816 |
| outpatient visits already in the log | 720 | 2,328 | 8,098 |
| of them `Follow-up` visits the `:scheduling` key books at a discharge | 674 | 1,929 | 7,626 |
| of them vendored modules' | 46 | 399 | 472 |
| orders written in an outpatient visit; `:community` fills | 3; 3 | 48; 48 | 59; 59 |
| orders written in an admission; `:inpatient` dispenses | 493; 491 | 4,943; 4,912 | 20,168; 20,121 |
| events | 33,819 | 172,867 | 691,758 |
| subjects (distinct participant ids) | 2,797 | 8,522 | 32,081 |

Two things in that table the prompt does not say. Dense already holds
outpatient visits, thousands of them: the follow-ups `:scheduling`
books and the modules' own. And only about 82 in 100 weighted arrivals
open an encounter at all: `:scheduling` books 70% of arrivals, and 8%
of bookings are cancelled and 15% are no-shows.

**Assumptions.** A clinic arrival is realised at the rate an admission
arrival is today (0.82), since `:scheduling` books an arrival whatever
its opener. The three existing pathways each lose half their arrivals.
A follow-up is booked only at a `:discharge` (read in
`decide :discharge`), so follow-up visits fall with the discharges:
about 0.24 follow-up visits per pathway discharge lost. Every order
written in a visit is filled once, at the visit's close, except the few
whose patient is merged away between the order and the close.

| | 750 | 7,500 | 28,024 |
|---|---|---|---|
| clinic visits, the three new pathways | 274 | 2,694 | 11,057 |
| acute / chronic / review | 137 / 55 / 82 | 1,347 / 539 / 808 | 5,528 / 2,211 / 3,317 |
| outpatient visits, all | about 930 | about 4,380 | about 16,500 |
| orders written in an outpatient visit: acute pathway | 137 | 1,347 | 5,528 |
| chronic pathway | 55 | 539 | 2,211 |
| review pathway | 0 | 0 | 0 |
| vendored modules | about 3 | about 48 | about 59 |
| `:community` fills | 185 to 200 | 1,850 to 1,960 | 7,600 to 7,850 |
| orders written in an admission | about 248 | about 2,486 | about 10,105 |
| `:inpatient` dispenses | 240 to 252 | 2,430 to 2,500 | 9,950 to 10,150 |
| community fills per inpatient dispense | about 0.78 | about 0.78 | about 0.77 |
| events | about 29,000 | about 125,000 | about 496,000 |
| subjects | 2,797, give or take 1% | 8,522, give or take 1% | 32,081, give or take 1% |

**The events row, at 7,500.** Half of the pathway stays go, and with
them (rounded): 2,690 admissions and as many discharges; 3,700
transfers, 1,070 cancelled transfers and 1,100 bed swaps, taking churn
on a stay to scale with the stays; 17,000 bed status changes; 10,290
order and result events; 2,457 admission orders, their dispenses and
2,446 ends; 3,925 observations; 2,694 procedures; 1,225 diagnostic
reports; 3,530 care-plan events; and about 2,300 events of follow-up
bookings and visits. Coming in: 5,388 visit openers and closers, 2,694
observations, 1,886 orders and 1,886 fills. Net about -47,700. The
other two cells scale that by their pathway admissions (0.1015 and
4.105). This row is the weakest: the bed-status and churn terms are
proportions, not counts.

The prompt expects about 13,500 visits and 9,500 orders at 28,024.
This prediction is lower, 11,057 and 7,739, by the bookings that never
become a visit.

**Not predicted, flagged.**

- Whether `--churn` and a clinic visit get along. No shipped scenario
  runs churn over an authored outpatient pathway at this size. A
  self-check failure here would be a finding, not a count.
- What a repeat arrival does when its pathway is a clinic visit and the
  person is already in a bed.
- The mutation gate. Its dense populations change shape; an operator
  may lose or gain its sites.
- Bed occupancy. Half the stays go, so census should be about half the
  README's table. Not derived further.
