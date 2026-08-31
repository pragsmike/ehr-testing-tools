# emit_hl7 extraction 3 -- the timelines cluster, plus the authorized P5 row compaction

Session prompt, 2026-08-31. Archived verbatim as issued; the session's
own record is
`.agents/session-records/2026-08-31-emit-extraction-timelines.md`.

[A] Adopt.

---

SESSION: emit_hl7 extraction 3 -- the timelines cluster, plus the
authorized P5 row compaction
Repo: pragsmike/ehr-testing-tools, tip (e3ce663 or descendant).
Roadmap row P5, emit phase, order per census section 2a. Rulings: C1(a);
S1(a); C6(a) -- the author authorizes compacting the P5 row's
ENGINE-phase closed narrative to a <=5-line summary whose pointer is
the nine engine session records; do this FIRST, as its own docs
commit, so the row has headroom before this session's own update.

READ FIRST
- Census section 2 `timelines` (5 forms), section 2a, section 3b.
- The registry session record: its already-located constraint-6
  site for THIS cluster (person-simulator/limitations_test.clj:152 --
  a TEST file, C1(a)-fenced: disclose stale-by-fence, do not edit);
  the in-clone baseline rule; the one-CI-run-per-push fact.

CHANNEL EXPECTATIONS TO CORRECT FROM THE TREE (the channel has
erred on edges twice): the registry record says all five movers are
private -- if the tree agrees, constraint 5 owes ZERO delegating defs
and every mover widens only where a caller stays behind (derive
which; some may have all callers travelling, the weighted-pick
shape). Census section 3b's timelines edges: derive fresh; expect
hl7-time possible, nothing else -- correct from the tree.

STEPS (one gate each; full make test before every push)
0. C6(a) compaction commit: "docs: compact the P5 row's engine-phase
   narrative -- author-ruled C6(a)". Gate: roadmap-lint green;
   :onboarding headroom recorded before and after.
1. Derivations (markers, spans, re-exports, test call sites, edges).
   Gate: recorded.
2. Constraint-6 sweep: shingles + hand-read of BOTH charter
   registers row by row (the shingle floor is proven blind to short
   pins) + hand-owned-asset sources + prior banners + self-
   referential claims inside the moved text + residue prose naming
   movers; dispositions committed before the move; predicted reds
   RED-FIRST with successor; state-derived regen LAST. Gate: hit
   list or disclosed absence first.
3. Extract to ehrt.sim-emit-hl7.timelines: verbatim; widenings only
   where forced. Commit: "refactor: extract timelines namespace from
   emit_hl7.clj -- output-identical". Gate: suite delta explained to
   the assertion, measured IN-CLONE; bin/regression-oracle IDENTICAL
   (load-bearing), no declaration -- delta = defect, stop and report.
4. bin/ground-truth-bracket (near-vacuous; run, say so). Gate:
   IDENTICAL.
5. Push; CI via gh; close marker. Record: census corrections; P5
   row -> thirteen landings within the recovered headroom; require
   set confirmed.
FENCES: no interface.clj edits; emitted bytes identical; no var
renames; no engine-side edits; oracle IDENTICAL or stop.
SELF-ARCHIVE: prompt and record in the final push.
