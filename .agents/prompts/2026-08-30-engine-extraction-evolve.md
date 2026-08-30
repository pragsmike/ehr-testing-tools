# Session prompt -- engine extraction 4 of N: the evolve cluster

Archived verbatim, per the session's own SELF-ARCHIVE instruction. The
record beside this file is
`.agents/session-records/2026-08-30-engine-extraction-evolve.md`.

---

SESSION: engine extraction 4 of N — the evolve cluster
Repo: pragsmike/ehr-testing-tools, tip (54551d7 or descendant).
Roadmap row P5. Rulings in force: C1(a) facade — moved PUBLIC vars get
delegating defs, private movers widen with none (constraint 5);
S1(a) equivalence proof.

READ FIRST
- Census §1 `evolve` (32 forms), §3a edges, §5 constraints. The census
  has twice mis-rendered privacy markers — DERIVE each mover's actual
  marker from the tree; do not trust §1's def/defn rendering.
- The encounters session record — its sweep shape, and the comment-
  block recipe (now hit twice; treat block tops as suspect).

WHAT MOVES (spans at 517a96d — re-derive; the two census blocks
2541-2586 and 2648-3029 may now be CONTIGUOUS since encounters left
from between them): defmulti evolve, its 28 defmethods, and the
private helpers fold-condition-annotation, fold-conditions,
resolve-appointment, keep-appointment. Expected edges: encounters,
state, streams only (the old back-edge broke at the state
extraction) — verify by whole-symbol scan; any symbol still resolving
in engine.clj is a stop-and-report, not a route-through.
DELEGATION NOTE: `evolve` is a defmulti — a delegating
`(def evolve evolve/evolve)` shares the one multifn, and defmethods
registered in the new ns are visible through it; verify live (resolve
+ dispatch one event through engine/evolve) rather than assume.

STEPS (one gate each; full `make test` before every push)
1. Confirm tip; re-derive spans and every mover's privacy marker.
   Gate: both recorded.
2. Constraint-6 sweep, both recipe levels, comment blocks included;
   dispositions committed before the move. Commit: "docs: the evolve
   cluster's pre-move citation sweep". Gate: hit list first.
3. Extract to ehrt.sim-engine.evolve: verbatim; privacy widenings per
   derived markers; delegating defs for public movers ONLY (expected:
   just the defmulti — correct me from the tree); qualified call
   sites for the rest. Commit: "refactor: extract evolve namespace
   from engine.clj — output-identical". Gate: suite delta zero or
   explained to the assertion (io-vocabulary +1-per-file-per-project
   is the known class); bin/regression-oracle IDENTICAL, no
   declaration — delta = defect, stop and report.
4. bin/ground-truth-bracket. Gate: IDENTICAL.
5. Push; verify CI via gh; CI green at tip is the close marker.
   Record: census corrections one sentence each; the P5 roadmap row
   updated to four landings in the same commit as the record; confirm
   evolve's require set against the expected three.
FENCES: no interface.clj edits; no behavior, dispatch-order, or
draw-order changes; no var renames; no emit_hl7.clj; oracle
IDENTICAL or stop.
SELF-ARCHIVE: prompt and record in the final push.
