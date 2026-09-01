# 2026-09-01 — application-path unification: census and stage 1

Driving prompt, archived verbatim under R-A. Its session record is
[`../session-records/2026-09-01-apply-unification-stage-1.md`](../session-records/2026-09-01-apply-unification-stage-1.md).

---

SESSION: application-path unification — census and stage 1
Repo: pragsmike/ehr-testing-tools, tip (3e65ff9 or descendant).
Roadmap row P5 (now solely this arc). RULINGS (2026-08-30, verbatim
force): end-state is the FULL PRODUCT ALGEBRA at every apply site, no
per-site projections; the PATH is staged — stage 1 unifies the choke
point with each site's CURRENT accumulator stack as an explicit
projection (output-identical), stage 2 enables omitted
(site × accumulator) pairs ONE COMMIT EACH, where a delta is a
FINDING (the get-it-right stage: dispositioned bug/load-bearing with
a trace, no consumer ceremony). This session = census + stage 1 ONLY.

READ FIRST
- Census §4 (the three sites and the divergence, at pre-extraction
  homes — ALL THREE HAVE MOVED: site 1 in run.clj's in-loop fold,
  site 2 fold.clj/replay, site 3 log_index.clj/reinstated-state's
  fallback; re-derive everything at your sha).
- The extraction Done entry's coverage paragraph: neither bracket
  reaches a cancel decide — site 2/3 behavior is witnessed by the
  suite's cancel family and live checks, NOT the oracle; say so
  wherever you claim IDENTICAL.

STEPS (one gate each; full make test per push)
1. UNIFICATION CENSUS, committed as
   .agents/plans/apply-unification-census.md: (i) the accumulator
   inventory — every distinct concern the in-loop fold performs
   (expect ~10: evolve step, encounter stamp, warm-up mark, bed
   index, three log indexes, …) each as a named fold
   (acc, event, state') → acc'; (ii) the site × accumulator matrix,
   present/omitted per site, derived from the tree; (iii) for every
   OMITTED pair, the stage-2 cone prediction: what downstream of
   that site could observe the accumulator — predict INERT or
   OUTPUT-MOVING with the consuming path named, so stage-2 deltas
   arrive explained; (iv) the choke-point signature and its home
   (fold.clj is the channel's expectation; yours to confirm) with
   projections as explicit declared subsets. Commit: "docs: the
   apply-unification census — matrix, cones, choke point". Gate:
   every matrix cell cites file:line at your sha.
2. STAGE 1: implement the choke point; repoint all three sites to
   call it with their current stacks as projections; behavior
   change ZERO by construction. Red-before-green does NOT apply
   (S1(a)); but add the co-landed invariant: a test asserting the
   three sites' projections match the census matrix. Commit:
   "refactor: one apply choke point, three projected call sites —
   output-identical". Gate: suite delta explained in-clone;
   bin/regression-oracle IDENTICAL, no declaration;
   bin/ground-truth-bracket IDENTICAL; a real -M:dev drive of
   site 3 down BOTH branches (the log-index session's precedent).
3. Push; CI via gh; close marker. Record: census corrections to the
   channel's expectations; the P5 row gains the stage-2 pair
   checklist (one row per omitted pair, cone prediction alongside);
   C14's two live-surface corrections ride the docs commit if the
   author has so ruled.
FENCES: no accumulator enabled or disabled anywhere — stage 2 is
NOT this session; no interface.clj edits; no draw-order changes;
oracle IDENTICAL or stop; if the matrix disagrees with the census
§4 divergence in KIND (not just line numbers), stop and report
before implementing.
SELF-ARCHIVE: prompt and record in the final push.
