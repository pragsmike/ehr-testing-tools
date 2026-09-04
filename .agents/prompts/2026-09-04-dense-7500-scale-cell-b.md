# Prompt: 2026-09-04 -- dense-7500 (b), skeleton corrected

**Repo** `ehr-testing-tools`, clone of record `~/src/ehr-testing-tools`
(ext4, WSL). **Base HEAD** `e1baf4d`. **Ceremony** R30 (commit and push
at each checkpoint, unattended), taken from the prompt. **Record**
`.agents/session-records/2026-09-04-dense-7500-scale-cell-b.md`.

## The prompt, verbatim

---

# Session: dense-7500 (b) -- skeleton corrected, cells re-measured, table rewritten (2026-09-04)

Record 2026-09-04-dense-7500-scale-cell.md section 7: config-bare.edn
stops on :capacity-exhausted at ~2,963 arrivals because without
:persons every arrival is a distinct patient holding a bed for its
pathway's full dwell. Ruled (b). This session shortens the dwells,
regenerates the three configs, re-measures all eight cells, and
rewrites the Scale table. No sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md; the
record above (sections 4, 5, 7); demos/scenarios/dense-7500/* and
bin/demo-exerciser-dense-7500 (note its witnessed-figures block reads
the README's table); docs/consuming-ground-truth.md :536-575; .agents/
plans/2026-09-01-event-mutation-population-ledger.md section 7;
.agents/reading-sets.edn.

Author rulings, verbatim and binding:
- R-b (2026-09-04): shorten dense-inpatient's delays; ward sizes are
  the 2026-08-24 record's facts and do not move. Regenerate all three,
  re-commit, re-measure all eight cells. Clinical realism of the
  resulting dwell is NOT a constraint -- this is a throughput cell,
  and R-values already declined retuning for realism.
- R-provenance (2026-09-04): R-shape's "config.edn's bytes are the
  provenance" is VOID on the authored path; bytes are amendable until
  the Scale table cites them. nobed/bare stay DERIVED (one key; prefix).
- R-measure (standing): warm-up plus two timed, one JVM per run, fresh
  spool target, seed 20260824, /usr/bin/time -v; means; process wall.
- R-edit (standing): edits touching backticked prose go through a
  script file, never an inline wrapper string.

Steps:
1. Derive the dwell bound from :arrival-gap (2 min) and the two binding
   wards -- Little's law, census = rate x mean dwell, per ward by the
   pathway's routing share -- and shorten dense-inpatient's delays to
   sit under capacity with margin; the arithmetic goes in the README.
   Re-derive nobed and bare mechanically; the README states both rules
   as commands. Gate: both derivation checks AND one config-bare run at
   7,500 exits 0 (it fails in ~42 s if not).
   Commit: demos: dense-7500 skeleton corrected -- the bare cell completes
2. Figures and warm-up. Run the exerciser once: it MUST fail at its
   witnessed-figures step (the README still carries the old cells) and
   nowhere else; take the counts it prints, write them into the
   README's 7,500 and 750 rows (events, messages only), re-run the
   exerciser to exit 0 -- that run is the warm-up. Then two timed runs
   per cell, strictly sequential. Gate: every timed run exits 0
   (nonzero = STOP). No commit.
3. Rewrite the Scale table's three rows to fresh figures citing
   demos/scenarios/dense-7500 (column "process wall"; :551-575 labeled
   as the 2026-08-29 programme's measurement on a configuration that
   no longer exists); one sentence where --patients is explained:
   without :persons every arrival is a distinct concurrent patient.
   README wall/RSS filled. Gate: full make test green.
   Commit: docs: Scale cells re-measured against the committed scenario
4. Record (per-run appendix; the dwell derivation; the matrix re-run
   over the corrected cell, probe re-authored from ledger section 7 and
   NOT promoted to bin/); roadmap row dense-scale-profile CREATED and
   CLOSED in one entry under measured :onboarding headroom, disclosed
   as created; indexes; archive prompt. Fences: no src; no new bin/
   script; ledger section 6 untouched.
   Commit: docs: dense-7500 (b) session record (archives prompt)
5. Push; verify CI yourself (gh run view); close-marker commit.

---

## Deviation record

Four, all disclosed in
[`../session-records/2026-09-04-dense-7500-scale-cell-b.md`](../session-records/2026-09-04-dense-7500-scale-cell-b.md)
at the section named beside each.

1. **Step 3's dictated sentence was NOT written** (record section 8).
   The prompt asked for "one sentence where `--patients` is explained:
   without `:persons` every arrival is a distinct concurrent patient".
   That mechanism is not this engine's: `sim_engine/run.clj`'s
   `owner-ordinal` resolves a repeat arrival to the FIRST arrival's
   ordinal, so `:persons` merges RECORDS and both arrivals still hold a
   bed. `:scheduling` is what spreads the census. The corrected
   mechanism was written instead, in both places the old one appeared.
   Fix-forward with disclosure rather than STOP-AND-REPORT, because only
   one reading survives reading the code
   (`rulings.md#R-stop-only-on-two-defensible-readings`).

2. **The warm-up run exited 1, not 0** (record section 3). Every check
   of substance passed -- three named derivations, three witnessed
   figures -- and the failure was the exerciser's own ADR-0005
   tree-clean postcondition firing on the uncommitted README edit the
   same step had just instructed. No ordering of step 2's own
   instructions could have satisfied it, since the step forbids a
   commit. The script was run a THIRD time after C2, with the tree
   clean, and exited 0; that run is section 7.

3. **Two edits beyond the five delays**, both in C1 and both because the
   published census arithmetic would otherwise sit beside a claim it
   contradicts: the `:wards` block's "the steady-state census never
   reaches the surge rungs" was narrowed (Surgery's 173.3 is above its
   160 licensed beds), and `bin/demo-exerciser-dense-7500`'s
   "what is not checked here" note was corrected in C2.

4. **The README's referential-matrix table was re-measured in C2**, not
   held back for the record's own commit. The prompt puts the matrix
   re-run in step 4; the table it feeds is one of the README's witnessed
   figures, and leaving it stale through the commit titled "cells
   re-measured" would have published a figure this session knew to be
   superseded.

**Fences held.** No `src` file touched; no new `bin/` script (both
instruments are scratch, reproduced in the record); ledger section 6
untouched.

