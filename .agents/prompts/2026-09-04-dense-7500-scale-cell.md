# Session: dense-7500 -- the documented scale cell, committed (2026-09-04)

Archived verbatim, as issued. Record:
[`../session-records/2026-09-04-dense-7500-scale-cell.md`](../session-records/2026-09-04-dense-7500-scale-cell.md).

---

consuming-ground-truth.md's Scale table (:536-548) has three cells
nobody can re-run: "all nine opt-in keys" (171,864 events), "less
:bed-cycle" (129,415; TS-5's cell), "no opt-in key" (105,214; arc-0's
continuity series). Their configs are penny scratch plus a table in a
record; their in-process timings came from a var-rebinding driver that
P5's extraction retired. This session commits the configs as a
maintained scenario, exercises it like every scenario, re-measures the
cells with the instrument the tree can support, and rewrites the rows
to cite the committed artifact. No sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md;
.agents/session-records/2026-08-29-traffic-scale-close.md :75-104
(nine keys; additive prefix), :130-156 (what measured what); 2026-08-29-
ts-5-superseded-cancel.md :22-45; demos/scenarios/README.md and
ed-tuesday/*; bin/demo-exerciser-ed-tuesday (marker block); components/
docs-tooling/resources/docs-tooling/exercised-sources.edn; .agents/
plans/2026-09-01-event-mutation-population-ledger.md section 7;
docs/consuming-ground-truth.md :536-575; .agents/reading-sets.edn.

Author rulings, verbatim and binding:
- R-shape: demos/scenarios/dense-7500/ with config.edn (nine keys),
  config-nobed.edn (= config.edn minus :bed-cycle), config-bare.edn
  (= config.edn minus all nine keys). config.edn's bytes are the
  provenance; the other two are DERIVED from it. Scratch that
  disagrees with a derivation is disclosed, not adopted.
- R-values: frozen to the record's nine-key table, mismatches
  (scheduling, chatter) disclosed verbatim in the README. No retuning.
- R-exerciser: the exerciser runs the 7,500-arrival cell on config.edn.
- R-measure: warm-up plus two timed, one JVM per run, fresh spool
  target per run, seed 20260824, /usr/bin/time -v; figures are means.
  The table's "in-process wall" column becomes "process wall"; the
  phase prose (:551-575) stays as the 2026-08-29 record's, labeled so.

Steps:
1. config.edn: adopt penny's dense-7500-v2.edn if present (verify the
   record's stated byte counts), else author from the table with
   `:persons {:count 15000 :years 20}` per its rule. Derive the other
   two. Gate: `diff config config-nobed` = one key; config-bare is a
   prefix of config.edn (the record's additive claim). Commit: demos:
   dense-7500 configs, provenance
2. README (ed-tuesday shape; commands root-resolvable; --format
   ground-truth taught; disclosure; no figures yet), exerciser with
   marker block, registry row, marker pair. Gate: demo-exerciser-fresh
   + exercised-sources tests green. Commit: demos: dense-7500 page and
   exerciser, registered
3. Measure: the warm-up IS `bin/demo-exerciser-dense-7500` run once
   (exit 0 gated); then two timed runs per config at 7,500 plus the
   750 point on config.edn: events, messages, msg/event, process
   wall, peak RSS, exit. Gate: every run exits 0 (nonzero = STOP
   finding). No commit.
4. Referential matrix per ledger section 7 over config.edn's 7,500
   output: columns A, B1, B2, C, D counts. Report only. No commit.
5. Rewrite the three table rows to fresh figures citing the scenario;
   fill README figures. Gate: full make test green. Commit: docs:
   Scale cells re-measured against the committed scenario
6. Record (per-run appendix, matrix counts, scratch-or-authored);
   roadmap row `dense-scale-profile` CLOSED under measured :onboarding
   headroom, else record-only; indexes; archive prompt. Fences: no
   src; nothing under sim-check; no instrument authored.
   Commit: docs: dense-7500 session record (archives prompt)
7. Push; verify CI yourself (gh run view); close-marker commit.

---

## How it went

Steps 1, 2, 4 and 6 executed. **Step 3 gated red and the session
stopped there**: both `config-bare.edn` cells exit 2 on
`:capacity-exhausted`, so step 5's table rewrite and step 6's roadmap
closure were withheld. Step 1's premise did not hold either -- the
scratch this prompt hoped to adopt no longer exists on penny, so the
skeleton is re-authored from the 2026-08-24 record's prose. The record's
section 7 carries the ruling the channel owes.
