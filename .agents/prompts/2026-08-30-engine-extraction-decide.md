# Session prompt: engine extraction 8 of N -- the decide cluster

Archived verbatim, 2026-08-30. The session it drove is
`.agents/session-records/2026-08-30-engine-extraction-decide.md`, which
landed as the program's NINTH namespace (the prompt's own "8 of N"
counts sessions, not namespaces -- session 7 landed two).

---

SESSION: engine extraction 8 of N — the decide cluster
Repo: pragsmike/ehr-testing-tools, tip (ac27ee9 or descendant).
Roadmap row P5. Rulings: C1(a) — public movers get delegating defs;
private movers widen ONLY where a call site stays behind (the
weighted-pick precedent: both-callers-travel means no widening owed);
S1(a). This is the largest move of the program (census: 59 forms,
~1,613 lines, stale) — the sweep is the cost center, the move is
size-insensitive.

READ FIRST
- Census §1 `decide`, §3a (decide → log-index 11 edges, the biggest
  row — now OUTGOING to an extracted ns), §4a (decide at run's loop
  is the SOLE event producer — you are moving the producer; run's
  call site qualifies), §5.
- The config+assignment record: the weighted-pick precedent; poly
  check's parse blindness (back every check with a real -M:dev
  load); the tripwire's git-log-vs-worktree timing.

DERIVE FROM THE TREE: every mover's marker (census privacy renderings
have erred in 4 of 7 extractions); spans (seven stale; the cluster
is non-contiguous — the two cancel decides sat apart at census sha);
interface.clj re-exports among movers (channel expects
documented-step-rejection-reasons is ON the list — verify);
test-file call sites of every public mover; outgoing edges by
whole-symbol scan (expect log-index, encounters, state, streams;
evolve possible — correct from the tree); anything still resolving
in engine.clj is stop-and-report.
DELEGATION NOTE: decide is a defmulti — same one-MultiFn delegation
as evolve; verify live (identical? + dispatch one step through
engine/decide) rather than assume.
COVERAGE HONESTY: the oracle reaches no cancel decide; say plainly in
the record which movers neither bracket exercises and what covers
them (suite's cancel family, live checks) — the log-index record's
§2f is the model.

STEPS (one gate each; full make test before every push)
1. Confirm tip; re-derive everything above. Gate: recorded.
2. Constraint-6 sweep, all levels (docstring phrases, comment blocks,
   prior-extraction banners — seven sessions have written banners
   that may cite decide helpers — hand-owned-asset sources, charter
   registers, cross-brick prose attributions of private movers);
   dispositions committed before the move; predicted reds RED-FIRST
   with successor. Commit: "docs: the decide cluster's pre-move
   citation sweep". Gate: hit list first.
3. Extract to ehrt.sim-engine.decide, ONE commit: verbatim;
   widenings only where forced; delegating defs for public movers
   only. Commit: "refactor: extract decide namespace from
   engine.clj — output-identical". Gate: suite delta zero or
   explained to the assertion (io-vocabulary +1-per-file-per-project
   known benign); bin/regression-oracle IDENTICAL, no declaration —
   delta = defect, stop and report.
4. bin/ground-truth-bracket. Gate: IDENTICAL.
5. Push; verify CI via gh; CI green at tip is the close marker.
   Record: census corrections one sentence each; P5 row → nine
   landings; require set confirmed; and a short factual note on what
   remains in engine.clj (form count, public/private split) to
   inform the author's pending run/residual ruling.
FENCES: no interface.clj edits; no behavior, dispatch-order, or
draw-order changes; no var renames; no emit_hl7.clj; oracle
IDENTICAL or stop; if the sweep surfaces something structurally
unexpected, land nothing and report rather than force the move.
SELF-ARCHIVE: prompt and record in the final push.
