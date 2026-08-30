# Session prompt -- engine extraction 5 of N: the fold cluster

Archived verbatim, per the session's own SELF-ARCHIVE instruction. The
record beside this file is
`.agents/session-records/2026-08-30-engine-extraction-fold.md`.

---

SESSION: engine extraction 5 of N — the fold cluster
Repo: pragsmike/ehr-testing-tools, tip (5b6ab85 or descendant).
Roadmap row P5. Rulings in force: C1(a) facade — public movers get
delegating defs, private movers widen with none; S1(a) equivalence
proof.

READ FIRST
- Census §1 `fold` (3 forms: bed-correction-event-types, update-beds,
  replay), §3a edges, §4b-4d (replay is apply site 2 — you are moving
  an apply site; its behavior must not change one bit), §5.
- The evolve session record — the recipe's third firing: banners a
  PRIOR extraction wrote are mover candidates; sweep for them.

DERIVE FROM THE TREE (census renderings have erred three times):
each mover's actual privacy marker; spans (census's 3030-3139 at
517a96d is four extractions stale); whether `replay` is on
interface.clj's re-export list (channel expects YES — if so the
delegating def is what keeps that export resolving; verify, don't
trust this either).

NOTE ON WHAT THIS MOVE IS NOT: replay's divergence from run's in-loop
fold (six omitted concerns) is census-documented and RULED to be paid
at unification, not here. Move replay verbatim; do not add, remove,
or reorder anything it folds. Expected edges: evolve (replay →
evolve) and state (replay → initial-patient) — verify by whole-symbol
scan; anything still resolving in engine.clj is stop-and-report.

STEPS (one gate each; full `make test` before every push)
1. Confirm tip; re-derive spans + privacy markers + the interface
   re-export question. Gate: all three recorded.
2. Constraint-6 sweep, all levels (docstring phrases, adjacent
   comment blocks, prior-extraction banners, hand-owned-asset
   register, both charter registers); dispositions committed before
   the move; predicted reds named RED-FIRST with their successor.
   Commit: "docs: the fold cluster's pre-move citation sweep".
   Gate: hit list first.
3. Extract to ehrt.sim-engine.fold: verbatim; widenings per derived
   markers; delegating defs for public movers only. Commit:
   "refactor: extract fold namespace from engine.clj —
   output-identical". Gate: suite delta zero or explained to the
   assertion (io-vocabulary +1-per-file-per-project known benign);
   bin/regression-oracle IDENTICAL, no declaration — delta = defect,
   stop and report.
4. bin/ground-truth-bracket. Gate: IDENTICAL.
5. Push; verify CI via gh; CI green at tip is the close marker.
   Record: census corrections one sentence each; P5 row → five
   landings in the record commit; confirm fold's require set against
   the expected two.
FENCES: no interface.clj edits; no behavior or draw-order changes; no
change to what replay folds; no var renames; no emit_hl7.clj; oracle
IDENTICAL or stop.
SELF-ARCHIVE: prompt and record in the final push.
