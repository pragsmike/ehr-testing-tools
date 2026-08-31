# emit_hl7 extraction 5 -- the segments cluster

Session prompt, 2026-08-31. Archived verbatim as issued; the session's
own record is
`.agents/session-records/2026-08-31-emit-extraction-segments.md`.

[A] Adopt.

---

SESSION: emit_hl7 extraction 5 -- the segments cluster
Repo: pragsmike/ehr-testing-tools, tip (386e738 or descendant).
Roadmap row P5, emit phase, order per census section 2a. Rulings: C1(a)
with the C7 extension now RATIFIED -- a private var reached by #' from a
C1(a)-fenced test file gets a ^:private delegating def in the facade
(the er7 precedent, tn-field); constraint 5 as prohibition
(weighted-pick); S1(a).

READ FIRST
- Census section 2 `segments` (15 forms -- the largest emit cluster by
  lines), section 2a, section 3b (segments -> er7 is 18 pairs, the
  biggest emit edge row; expect requires on er7 and likely registry/
  hl7-time/timelines -- derive, don't trust).
- The er7 session record: the #' scan shape and its mis-scope
  correction; the two PREDICTED #' sites this cluster owes
  (msh-segment, pid-segment -- verify both and sweep for MORE, since
  the er7 scan was the first of its kind); the found-not-caused
  disclose-and-backlog rule; in-clone baselines.

DERIVE FROM THE TREE: markers; spans; emit interface re-exports
among movers; test call sites INCLUDING #'-quoted reaches across all
test files; edges both directions, bare and qualified (four clusters
have left the file); which private movers' callers all travel.

STEPS (one gate each; full make test before every push)
1. Derivations. Gate: recorded, #' census explicitly included.
2. Constraint-6 sweep: shingles + hand-read charter registers +
   hand-owned-asset sources + prior banners + self-referential
   claims in the moved text + residue prose + found-not-caused
   staleness (disclose-and-backlog); dispositions committed before
   the move or absence disclosed; predicted reds RED-FIRST with
   successor; state-derived regen LAST. Gate: hit list or absence
   first.
3. Extract to ehrt.sim-emit-hl7.segments: verbatim; widenings only
   where forced; ^:private delegating defs for #'-reached privates
   per C7. Commit: "refactor: extract segments namespace from
   emit_hl7.clj -- output-identical". Gate: suite delta explained to
   the assertion, in-clone baseline; bin/regression-oracle IDENTICAL
   (load-bearing), no declaration -- delta = defect, stop and report.
4. bin/ground-truth-bracket (near-vacuous; run, say so). Gate:
   IDENTICAL.
5. Push; CI via gh; close marker. Record: census corrections; P5
   row -> fifteen landings inside the 4-line headroom (compact
   emit-phase text only; if the row cannot fit, STOP and escalate
   rather than touch engine doctrine); require set confirmed.
FENCES: no interface.clj edits; emitted bytes identical; no var
renames; no engine-side edits; oracle IDENTICAL or stop.
SELF-ARCHIVE: prompt and record in the final push.
