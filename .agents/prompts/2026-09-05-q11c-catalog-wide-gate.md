# 2026-09-05 — Q11 re-ruled (c): the finding vocabulary as a test-side law; the catalog-wide gate

Archived verbatim, per `AGENTS.md`'s session-record ritual (R-A). The
record this prompt drove is
[`2026-09-05-q11c-catalog-wide-gate.md`](../session-records/2026-09-05-q11c-catalog-wide-gate.md).

---

# Session: Q11 re-ruled (c) -- the finding vocabulary as a test-side law; the catalog-wide gate (2026-09-05)

Roadmap row `event-mutation-catalog-gate` (PRIORITY 6). Author ruling
(2026-09-05): Q11 re-ruled (c) -- `ehrt.sim-check.interface` stays one
var; the law that every operator's :expected-findings names an
invariant `check` can produce lands as a corpus-brick TEST reading
sim-check's four catalogs directly (test may reach any namespace).
Item (1), the catalog-wide gate, is built in the same session now that
a check-all costs 2.74 s. Rider: the build-session skill's close
ceremony gains the background-process postcondition. No sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md (close
step; it is in the :docs set at 0 headroom); the row; notes/adr/0176-
event-stream-mutation.md sections 1(d), 2(iv), and the Q11/Q14 text;
components/sim-check/src/ehrt/sim_check/check.clj :1882-1935 (the four
catalogs and how a violation names its :invariant); components/corpus/
src/ehrt/corpus/operators.clj (register!, catalog-gaps); event_mutate_
test.clj :87-135 (populations), :173, :303-400 (the loop tests);
.agents/reading-sets.edn.

Rulings, binding:
- R-c: no change to sim-check src or interface. The vocabulary is
  derived in test from the catalogs' vars (the keyword a violation
  carries), not hard-coded.
- R-wide: every (operator, population) pair with >= 1 candidate site
  runs the 2(iv) loop with Q5(a) equality; pairs with no site are
  reported by name, not skipped silently. Cost rule: measure the
  corpus brick's wall before/after; if the wide gate adds > 120 s
  per project, it moves under `make integration` and the record says
  so -- that is a measurement, not a STOP.
- R-rider: SKILL.md's close step gains, net-zero lines by compaction
  within the file: (1) enumerate and terminate every background
  process the session started, before the close marker; (2) never
  hand-roll an `until` waiter -- use the harness monitor or the job's
  completion notification. :docs stays at 785.
- R-pins: a moved count pin is its own commit.

Steps:
1. RED: `unknown-declared-findings` (test-side fn) and two tests --
   the real catalog yields none; a synthetic operator declaring
   `:no-such-invariant` is named. Gate: exactly these red.
   Commit: test: every declared finding is a checker invariant -- RED
2. GREEN for step 1. If the real catalog yields an offender, that is
   a STOP: a shipped operator promises a finding check cannot make.
   Gate: corpus brick green in every project.
   Commit: test: the finding vocabulary is a law (Q11(c))
3. Wide gate: the loop over every sited pair; the no-site report;
   wall measured per R-wide before and after. Gate: full make test
   green, wall recorded. Commit: test: the catalog-wide oracle loop
   (ADR-0176 2(iv)) -- or, per the cost rule, its integration home.
4. Rider per R-rider. Gate: reading-set table shows :docs 785, the
   skill's own gates green. Commit: docs: close ceremony -- background
   processes terminated, no hand-rolled waiters
5. ADR-0176 dated addendum: Q11 re-ruled (c) with the reasoning (a
   test may reach what an interface need not export); row -> CLOSED
   as a two-clause pointer; record with the wall numbers and the
   no-site pairs; indexes; archive. Fences: no sim-check src; no
   operators.clj change unless step 2 STOPs.
   Commit: docs: Q11(c) session record (archives prompt)
6. Push; verify CI yourself (gh run view); close-marker commit.
