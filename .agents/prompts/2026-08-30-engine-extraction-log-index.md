# Session prompt -- engine extraction 6 of N: the log-index cluster

Archived verbatim, per the session's own SELF-ARCHIVE instruction. The
record beside this file is
`.agents/session-records/2026-08-30-engine-extraction-log-index.md`.

---

SESSION: engine extraction 6 of N — the log-index cluster
Repo: pragsmike/ehr-testing-tools, tip (c82436b or descendant).
Roadmap row P5. Rulings in force: C1(a) — public movers get delegating
defs, private movers none (widen, qualify call sites); S1(a).

READ FIRST
- Census §1 `log-index` (10 forms, FOUR non-contiguous regions at
  census sha — the most scattered cluster yet), §3a (decide →
  log-index is the biggest edge row, 11 crossings, all INCOMING —
  they stay in engine.clj and qualify), §4d (reinstated-state IS
  apply site 3 — you are moving an apply site verbatim; the ruled
  unification pays its divergence later, not here), §5.
- The fold session record — the private-mover prose class
  (check.clj:522/583/600 shape): sweep OTHER BRICKS' prose for
  attributions of private movers to engine; nothing forwards those.

DERIVE FROM THE TREE (census §1 privacy renderings have erred in 3
of 5 extractions): every mover's actual marker (census shows 4-5
private; expect few or zero delegating defs owed — count from the
tree); spans (census's are five extractions stale; the four regions
have drifted and may have merged or split); interface.clj re-export
check for every public mover; outgoing edges (expect fold and/or
evolve and state from reinstated-state's fallback; streams possible)
by whole-symbol scan — anything still resolving in engine.clj is
stop-and-report.

COVERAGE HONESTY (record it, don't fix it): the oracle's 41 roots
reach no cancel, so both brackets are blind to this cluster's actual
behavior; the suite and live resolution checks carry the load. Say
so in the record as the fold session did.

STEPS (one gate each; full make test before every push)
1. Confirm tip; re-derive spans, markers, re-exports, edges.
   Gate: all recorded.
2. Constraint-6 sweep, all levels (docstring phrases, comment blocks,
   prior-extraction banners, hand-owned-asset registry sources,
   charter registers, cross-brick prose attributions of private
   movers); dispositions committed before the move; predicted reds
   RED-FIRST with successor. Commit: "docs: the log-index cluster's
   pre-move citation sweep". Gate: hit list first.
3. Extract to ehrt.sim-engine.log-index: verbatim; widenings per
   derived markers; delegating defs for public movers only.
   Commit: "refactor: extract log-index namespace from engine.clj —
   output-identical". Gate: suite delta zero or explained to the
   assertion (io-vocabulary +1-per-file-per-project known benign);
   bin/regression-oracle IDENTICAL, no declaration — delta = defect,
   stop and report.
4. bin/ground-truth-bracket. Gate: IDENTICAL.
5. Push; verify CI via gh; CI green at tip is the close marker.
   Record: census corrections one sentence each; P5 row → six
   landings in the record commit; confirm the require set against
   step 1's derivation.
FENCES: no interface.clj edits; no behavior change — reinstated-state
folds exactly what it folded; no var renames; no emit_hl7.clj;
oracle IDENTICAL or stop.
SELF-ARCHIVE: prompt and record in the final push.
