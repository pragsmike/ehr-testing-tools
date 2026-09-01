# 2026-09-01 — apply-unification landing: the last two pairs, one declared omission, and the P5 close

Archived under R-A. This is the prompt this session ran, verbatim.

---

SESSION: apply-unification landing — the last two pairs, one declared
omission, and the P5 close
Repo: pragsmike/ehr-testing-tools, tip (bf4616f or descendant).
RULINGS (2026-09-01, in force): A1(b) — enable :encounter-stamp at
sites 2 and 3, one commit each, WITH a co-landed identity gate: a
test pinning that re-stamping an already-stamped log is the identity,
co-landed because the inertness rests on stamp-encounter's contains?
guard and the whole-log-as-one-batch shape, both changeable by a
later session that doesn't know this pair depends on them. A2(b) —
:warm-up-mark at site 2 stays omitted, DECLARED PERMANENT: replay
has no source for the window, a declared 0 measurably destroys
marks, and the projection ends as a statement of fact. S1(a) holds.

READ FIRST
- The stage-2 record §4b-4d (the measurements these rulings rest on)
  and its §5; the census checklist; the P5 row.

STEPS (one gate each; full make test per push)
1. Confirm tip; re-verify the two pairs' enabling diffs against the
   stage-2 record's prepared shapes. Gate: recorded.
2. Land 2×A1: enabling commit with the identity gate co-landed in
   the SAME commit (red-first does not apply under S1(a); the gate
   is the co-landed invariant, green at birth). Commit: "feat:
   enable 2 x :encounter-stamp with its identity gate — A1(b)".
   Gate: suite green, delta explained (the new test's assertions);
   oracle IDENTICAL expected per the refuted prediction — if it
   moves, STOP: the measurement was wrong and the author must see.
3. Land 3×A1 the same way; site 3 reaches 13/13. Same gates.
4. A2(b) declaration commit (docs): the census checklist row and
   the site-2 projection's docstring both state the omission is
   PERMANENT and why, citing the record §4d; the projection test
   updated if it enumerates expected columns. Gate: suite green.
5. THE P5 CLOSE, one docs commit: the unification arc's narrative
   migrates to the roadmap Done section per rotation (one compact
   entry: 21 of 22 pairs at full product, one measured permanent
   omission, the refuted-prediction story, the record trail); the
   live P5 row RETIRES; the roadmap's head becomes P6
   [event-stream-mutation] — verify its row still reads: design ADR
   first, mutation moves to the ground-truth event stream, the
   unified apply path as injection point, file-level operators for
   lowering-layer faults only. Add one sentence to the P6 row: the
   injection point now exists (fold/apply-events, stage-1 landed).
   Gate: roadmap-lint green; headroom recorded; state-derived LAST.
6. Push; CI via gh; close marker. Record: final site matrix
   13/12/13 with the declared omission marked; oracle + bracket
   verdicts; the fence line: the apply arc is CLOSED.
FENCES: no interface.clj edits; no accumulator logic edited beyond
the two enabling set-literals; no draw-order changes; oracle
IDENTICAL or stop-and-show.
SELF-ARCHIVE: prompt and record in the final push.
