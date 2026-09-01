# 2026-09-01 — application-path unification, stage 2: enabling the omitted pairs

Driving prompt, archived verbatim under R-A. Its session record is
[`../session-records/2026-09-01-apply-unification-stage-2.md`](../session-records/2026-09-01-apply-unification-stage-2.md).

---

SESSION: apply-unification stage 2 — enabling the omitted pairs
Repo: pragsmike/ehr-testing-tools, tip (e9c01b1 or descendant).
Roadmap row P5. RULINGS in force: end-state FULL PRODUCT at every
site; stage 2 enables omitted (site × accumulator) pairs ONE COMMIT
EACH; a delta is a FINDING, not a defect. C13 (2026-08-31): the
sim/docs internal citation convention is ruled acceptable; close its
row. C14 (2026-08-31): correct the two live deleted-gate surfaces
(docsgen.clj, help_voice_test.clj); historical artifacts stay.
Both ride this session's docs commit.

READ FIRST
- .agents/plans/apply-unification-census.md — the pair checklist:
  22 pairs, 19 predicted INERT, 3 predicted OUTPUT-MOVING (all
  decorations). Re-verify the checklist against the P5 row.
- The stage-1 record: the post-batch state-history subtlety; the
  vacuous-gate reading of the replay bed fold (no consumer waits on
  that pair — enable it for uniformity, not value).

PROOF SHAPE FOR THE INERT 19: enable in census order, one commit
each, suite once per push. Oracle economy: run
bin/regression-oracle over each pushed SPAN; IDENTICAL end-to-end
stands as the span's proof, with the caveat named in the record
that span-identity is not per-commit identity — on ANY delta,
BISECT to the enabling commit, and that pair moves to the
OUTPUT-MOVING protocol below. bin/ground-truth-bracket once per
span. Cone predictions: mark each pair CONFIRMED-INERT in the
checklist as its span proves out.

PROTOCOL FOR OUTPUT-MOVING PAIRS (the predicted 3, plus any INERT
prediction refuted by bisection): do NOT land. For each, prepare in
the record: the enabling diff, the delta's first divergent root and
byte span, the trace at that root's seed, the census cone row it
confirms or refutes, and lettered disposition options
(bug-in-current / load-bearing-keep-omitted-pending /
take-the-change-declared). STOP after the inert spans land and
report all three together — the author disposes, a later session
lands.

STEPS
1. Confirm tip; re-verify the checklist and census order. Gate:
   recorded.
2. Docs commit: C13 row closed, C14 live surfaces corrected.
   Gate: suite green (help_voice_test edit compiles); state-derived
   LAST.
3. INERT pairs, one commit each, pushed in 2-4 spans; span oracle +
   bracket per push as above. Gate per span: suite green with delta
   explained; oracle IDENTICAL or bisect.
4. OUTPUT-MOVING preparation per the protocol; no landing. Gate:
   all three write-ups complete with traces.
5. Push; CI via gh; close marker. Record: checklist fully marked
   (CONFIRMED-INERT / REFUTED-moved-to-protocol / PREPARED);
   P5 row updated; the fence line: projections remain for the
   three pending pairs only.
FENCES: no interface.clj edits; no draw-order changes; enable-only
diffs (no accumulator logic edited while enabling); if more than
two INERT predictions refute, stop the span work and report — the
census's cone method itself is then in question.
SELF-ARCHIVE: prompt and record in the final push.
