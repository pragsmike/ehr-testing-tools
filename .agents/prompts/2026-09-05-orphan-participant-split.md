# 2026-09-05 — orphan-participant split by log fact; Done rows capped

Archived verbatim, per `AGENTS.md`'s session-record ritual (R-A). The
record this prompt drove is
[`2026-09-05-orphan-participant-split.md`](../session-records/2026-09-05-orphan-participant-split.md).

---

# Session: orphan-participant split by log fact; Done rows capped (2026-09-05)

Row `orphan-participant-shape-gap` (PRIORITY 6). Ruled 2026-09-05:
narrow by predicate and split. `:orphan-participant` convicts a fifth
invariant exactly when the reattributed start is CLOSED by an end that
cites it (dense-7500 closes spans; the demo logs never do). Put that
log fact in the site predicate: the operator keeps its four-set on
starts no end references; two new operators site on closed starts, one
per span column, declaring the five-set. Q5(a) equality then holds per
operator on every population by construction; the provisional
`declared-shape-gaps` register empties. Step 0 lands the Done-row cap
that instruction failed to achieve twice. No sub-agents.

Read first: AGENTS.md; .agents/skills/build-session/SKILL.md; the row
and .agents/session-records/2026-09-05-q11c-catalog-wide-gate.md
(the exhaustive 48-site table); notes/adr/0176-*.md addendum (c);
components/corpus/src/ehrt/corpus/operators.clj :895-945; components/
corpus/test/ehrt/corpus/event_mutate_test.clj :173 (loop-rows),
:405-495 (the register and its divergence test); sim_check/check.clj
:1271-1340 (the two span-end invariants); components/docs-tooling/
test/ehrt/docs_tooling/roadmap_lint_test.clj; .agents/plans/roadmap
.md ## Done (19 rows over 400 chars; longest 6,269).

Rulings, binding:
- R-cap: roadmap_lint gains a Done-row cap of 480 characters. A
  compacted row keeps: status token, slug, ADR/sha, >= 1 record path
  that RESOLVES (the gate checks resolution), one clause of outcome.
  Nothing else is required to survive; it lives in the record.
- R-split: `:orphan-participant` sites only on therapeutic-intent
  events that no end event cites; `:orphan-closed-medication-order`
  and `:orphan-closed-care-plan-start` (ids per operators.clj's
  conventions) site only on starts an end cites, declaring the
  four-set plus their span invariant. Unsited on a population =
  reported by name, per the wide gate. Same phantom id, same draw
  discipline (Q3(a)).
- Q5(a), Q6(a), R-pins standing. The register must end EMPTY and its
  divergence test deleted -- an empty register with a live test is
  a scaffold.

Steps:
0. R-cap: the gate RED on the 19 rows; compact them; GREEN. Gate:
   roadmap_lint + stale-path + citation tests green.
   Commit: docs: Done rows are pointers -- a gate, and 19 rows
   compacted to comply
1. RED: loop-rows for the two new operators (dense population, five-
   set); `:orphan-participant`'s row unchanged; a test that no site of
   `:orphan-participant` is an end-cited start on any population; the
   register's divergence test still present and green. Gate: exactly
   the new tests red. Commit: test: orphan-participant split by log
   fact -- RED
2. GREEN: the predicate change and two registrations; register
   emptied; divergence test deleted; wide gate green with no shape-gap
   entry. `make docsgen` (operators.md; cli.md only via help.clj :doc
   if `sim mutate` enumerates ids). Gate: corpus brick green in every
   project, wide-gate wall within R-wide's rule.
   Commit: feat(corpus): orphan-participant narrowed by log fact; two
   closed-start operators (ADR-0176 addendum c)
3. ADR-0176 addendum (c) gains a dated line (the split, the fact it
   keys on); consuming-ground-truth's operator count and the
   fault-injection inventory updated. Full make test; pins per
   R-pins. Gate: full suite green.
   Commit: docs: catalog at twenty-eight; addendum (c) extended
4. Oracle + bracket vs 4cfa570: IDENTICAL (post-run stage). Record;
   row -> CLOSED under R-cap; indexes; archive. Fences: no sim-check
   src; no change to the other 25 operators.
   Commit: docs: session record (archives prompt)
5. Push; verify CI yourself (gh run view); close-marker commit.
