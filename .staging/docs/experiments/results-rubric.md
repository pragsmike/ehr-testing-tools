# Results-File Rubric

<!-- The scoring half of the soft type in results-template.md (pattern
     nursery #12). Every EXP-*-results.md file self-scores against this
     rubric in its own "Rubric self-score" section. Binary criteria,
     each with required evidence -- no partial credit; either the
     criterion is met with a pointer to where, or it isn't. -->

| # | Criterion | What "met" means |
|---|---|---|
| 1 | Every finding is classified | Each row in every per-round findings table has a non-empty Classification of pin / control / canonicalize -- no divergence is left uncategorized |
| 2 | Environment record is complete | Every field in the Environment record table is filled in, not left blank or marked TBD |
| 3 | Amendments are justified | Every entry in Protocol amendments made states what changed, the date, and the evidence that motivated it -- or the section explicitly states "none" |
| 4 | Verdict is traceable to criteria | The Acceptance verdict section quotes the protocol's actual Acceptance and Stop Condition text, not a paraphrase, and states plainly whether each was met |
| 5 | No unexplained divergences | Every observed byte-level difference between compared runs appears in a per-round findings row -- none are silently dropped or left as "unknown noise" without an attempted classification |

A results file that fails any criterion is incomplete, not merely
imperfect -- finish the missing section rather than scoring around it.
