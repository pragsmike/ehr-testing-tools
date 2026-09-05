# Session: validators built once -- the schema gate at its real cost (2026-09-05)

P7's record measured `valid-event?` (event_schema.clj:1057,
`(m/validate Event event)`) at 2.288 ms/event against 0.0063 ms for a
validator built once -- a data-form schema recompiled per call. Since
ADR-0178 put `every-event-is-schema-valid` first in check-all, that
line is ~42 s of a 45 s check-all on 18k events, ~6 min on the dense
cell, ~11 min on every CI run. Output-identical refactor: no schema
content, no finding, no byte changes. No sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md;
components/sim-engine/src/ehrt/sim_engine/event_schema.clj :565-580
and :1050-1060; components/sim-model/src/ehrt/sim_model/persona.clj
:130-140; sim_check/check.clj :936-975 and :1760-1770 (the two
per-event callers); .agents/session-records/2026-09-05-p7-*.md (the
measurement's method); bin/oracle-lib.sh.

Author rulings, binding:
- R-scope: `valid-event?`, `valid-ground-truth?` (sim-engine) and
  `valid-persona?` (sim-model) call a validator built ONCE at load
  (`m/validator` on the def'd form). `explain-event` and every other
  `m/validate` site in the tree are UNCHANGED; list them in the
  record as per-record or load-time, with the line for each.
- S1(a): output-identical; no red owed. The witness is MEASUREMENT,
  recorded: same log, same JVM, per-call cost before/after for each
  of the three, and check-all wall on the dense --patients 20
  population before/after. No timing assertion enters the suite.
- R-bracket: oracle and bracket vs f6eeeba, IDENTICAL expected; any
  delta is a STOP (a compiled validator disagreeing with the
  interpreted one is a malli finding, not a fix).

Steps:
1. Baseline: the three per-call costs and the check-all wall, per
   the P7 record's method, into the record. Gate: four numbers.
   No commit.
2. The change: a private def'd validator per schema, the predicate
   delegating; load order verified (validator after its schema).
   Gate: sim-engine, sim-model, sim-check bricks green in every
   project. Commit: perf(schema): validators built once at load
   (S1(a), output-identical)
3. After-measurement, same method: the three costs, check-all wall,
   and one full `make test` wall against the prior session's
   recorded 2,043 s. Gate: full suite green.
   Commit: none (numbers go in the record).
4. R-bracket. Gate: IDENTICAL on both.
5. Record (before/after table; the unchanged-sites list; the CI wall
   from step 6 filled in after); roadmap: `performance-residual-
   sites` gains a dated one-clause payment note, stays OPEN; indexes;
   archive. Fences: nothing but the three predicates and their defs.
   Commit: docs: validators-once session record (archives prompt)
6. Push; verify CI yourself (gh run view) and record its wall
   against the prior run's; close-marker commit.
