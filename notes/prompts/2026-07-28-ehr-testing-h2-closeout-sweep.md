# 2026-07-28 — H2 closeout sweep (superseded, never executed past step 0)

**This file is a placeholder, not an archived prompt body.** The
session this filename refers to — `2026-07-28-ehr-testing-h2-closeout-sweep`
— was never actually written into this tree. `notes/ADRs.md` ADR-0004's
own numbering note (written the same day) records why: that session
stopped at its own step 0 precondition (CI red) before writing anything,
including its own prompt file, and the reserved `ADR-0003` slot it would
have filled sat empty until this session. No commit in this repo's
history shows a prompt body at this path.

## Superseded by

`2026-07-28-ehr-testing-discipline-parity` (this session's own prompt,
self-archived at `notes/prompts/2026-07-28-ehr-testing-discipline-parity.md`
once written). That prompt's own opening line states this explicitly:
*"The closeout-sweep prompt... never executed past its step-0 stops; the
state has since changed under it. This prompt SUPERSEDES it: its items
are folded in below."* Every item this sweep would have covered —
pre-push hook doctrine (ADR-0003), `.claude/` gitignoring,
docsgen-regen restoration, ADR-0003's own reserved slot — landed under
the discipline-parity session's own step 6, not here.

## Why this file exists at all

So a future reader who finds `notes/ADRs.md` ADR-0004 citing this
filename by name has somewhere to land, rather than a dead reference to
a file that was never written. Created as part of the discipline-parity
session's own step 6e, per that session's fix-forward-with-disclosure
rule: the instruction to "archive the old sweep prompt with its dated
superseded-by note" presumed a prompt body that turned out not to exist
— recorded here rather than silently fabricated or silently skipped.
