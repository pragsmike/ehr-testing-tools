# Session prompt -- engine extraction 3 of N: the encounters cluster

Archived verbatim, per the session's own SELF-ARCHIVE instruction. The
record beside this file is
`.agents/session-records/2026-08-30-engine-extraction-encounters.md`.

---

SESSION: engine extraction 3 of N — the encounters cluster
Repo: pragsmike/ehr-testing-tools, tip (3e0b65a or descendant).
Roadmap row P5. Program rulings in force: C1(a) facade + delegating
defs for moved PUBLIC vars only; S1(a) equivalence proof.

READ FIRST
- .agents/plans/engine-extraction-census.md — §1 `encounters` list,
  §3a edges, §5 all constraints.
- The state-extraction session record — its constraint-6 sweep shape
  and the comment-block correction (census correction 2): sweep for
  positionally-cited comment blocks adjacent to moving forms too.

WHAT MOVES (census spans at 517a96d — re-derive; engine.clj is two
extractions smaller): encounter-openable?, compiled-encounter-openers,
compiled-encounter-closers, gate-compiled-encounters,
two-encounter-event-types, stamp-encounter, open-encounter,
close-encounter, cancel-open-encounter, reopen-encounter. Note the
cluster is NOT contiguous (630-771 and 2587-2647 at census sha).
Its one outgoing edge is into already-extracted streams
(open-encounter → next-encounter-ordinal) — require streams directly,
do not route through engine.

STEPS (one gate each; full `make test` before every push)
1. Confirm tip; re-derive spans. Gate: recorded.
2. Constraint-6 sweep, both recipe levels (docstring phrases AND
   adjacent positionally-cited comment blocks), dispositions committed
   before the move. Commit: "docs: the encounters cluster's pre-move
   citation sweep". Gate: hit list dispositioned in the record first.
3. Extract to ehrt.sim-engine.encounters: verbatim except
   dispositioned repoints; six of these are defn- — per constraint 5
   they go public with NO delegating defs, call sites qualify; the
   defs that are public get delegations. Commit: "refactor: extract
   encounters namespace from engine.clj — output-identical". Gate:
   suite delta zero or explained to the assertion (the io-vocabulary
   +1-per-file-per-project class is known benign);
   bin/regression-oracle IDENTICAL, no declaration — delta = defect,
   stop and report.
4. bin/ground-truth-bracket. Gate: IDENTICAL.
5. Push; verify CI via gh; CI green at tip is the close marker.
   Record: census corrections one sentence each; confirm encounters'
   only require beyond clojure/malli/sim-model is streams.
FENCES: no interface.clj edits; no behavior or draw-order changes; no
var renames; no emit_hl7.clj; oracle IDENTICAL or stop.
SELF-ARCHIVE: prompt and record in the final push.

---

## Note on step 3's premise

Step 3's "six of these are defn- ... the defs that are public get
delegations" does not hold against the live tree: SEVEN are `defn-` and
the other three are `def ^:private`, so all ten movers are private and
the extraction owed ZERO delegating defs. The error is inherited from
the census's own §1 list, which renders the three as bare `def`.
Dispositioned as fix-forward-with-disclosure under
`rulings.md#R-stop-only-on-two-defensible-readings` -- see the record's
§1 and §7.
