# emit_hl7 extraction 4 -- the er7 cluster

Session prompt, 2026-08-31. Archived verbatim as issued; the session's
own record is
`.agents/session-records/2026-08-31-emit-extraction-er7.md`.

[A] Adopt.

---

SESSION: emit_hl7 extraction 4 -- the er7 cluster
Repo: pragsmike/ehr-testing-tools, tip (04b1e9f or descendant).
Roadmap row P5, emit phase, order per census section 2a. Rulings: C1(a);
S1(a); constraint 5 as the weighted-pick precedent reads it.

READ FIRST
- Census section 2 `er7` (19 forms -- the census's span-line figures
  overcount; section 2a's form-line figures are the honest ones),
  section 2a, section 3b.
- The timelines session record: the found-not-caused stale-claim
  category and its disclose-and-backlog rule; the both-directions
  edge scan (bare names AND qualified/dotted symbols, since four
  clusters have now left the file); the in-clone baseline rule.

FIRST NON-LEAF: census and tree (per the timelines record) give er7
ONE outgoing edge -- context-for-event -> demographics-at, now in
ehrt.sim-emit-hl7.timelines. er7.clj requires the SIBLING extraction,
a first for the emit phase. Verify from the tree; any second edge is
a census correction, any symbol still resolving in emit_hl7.clj is
stop-and-report.

DERIVE FROM THE TREE: markers (5 of the last 12 clusters had census
rendering errors); spans; emit interface re-exports among movers;
test call sites; whether any mover's callers ALL travel (no widening
owed for those). er7 is the escape/encoding layer -- its movers are
prime docstring-citation targets; expect the sweep to be non-empty.

STEPS (one gate each; full make test before every push)
1. Derivations. Gate: recorded.
2. Constraint-6 sweep: shingles + hand-read of both charter
   registers + hand-owned-asset sources + prior banners +
   self-referential claims inside the moved text + residue prose +
   found-not-caused staleness (disclose-and-backlog, don't fix);
   dispositions committed before the move (or absence disclosed);
   predicted reds RED-FIRST with successor; state-derived regen
   LAST. Gate: hit list or absence first.
3. Extract to ehrt.sim-emit-hl7.er7: verbatim; widenings only where
   forced. Commit: "refactor: extract er7 namespace from
   emit_hl7.clj -- output-identical". Gate: suite delta explained to
   the assertion, in-clone baseline; bin/regression-oracle IDENTICAL
   (load-bearing), no declaration -- delta = defect, stop and report.
4. bin/ground-truth-bracket (near-vacuous; run, say so). Gate:
   IDENTICAL.
5. Push; CI via gh; close marker. Record: census corrections; P5
   row -> fourteen landings inside the 8-line headroom (compact
   emit-phase text only; engine doctrine is fenced); require set
   confirmed -- expect exactly (timelines).
FENCES: no interface.clj edits; emitted bytes identical; no var
renames; no engine-side edits; oracle IDENTICAL or stop.
SELF-ARCHIVE: prompt and record in the final push.
