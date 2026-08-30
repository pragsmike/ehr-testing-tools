# Session prompt: engine extraction 9 of N -- the run cluster

Archived verbatim, 2026-08-30. The session it drove is
`.agents/session-records/2026-08-30-engine-extraction-run.md`, which
landed as the program's TENTH and LAST `engine.clj` namespace (the
prompt's own "9 of N" counts sessions, not namespaces -- session 7
landed two).

---

SESSION: engine extraction 9 of N — the run cluster; engine.clj
becomes a pure facade
Repo: pragsmike/ehr-testing-tools, tip (cd7302e or descendant).
Roadmap row P5. Rulings: C1(a) — public movers get delegating defs;
private movers widen only where a caller stays behind (weighted-pick
precedent); C4(b) — this extraction was ruled 2026-08-30; S1(a).

READ FIRST
- The decide session record: its residue measurement (6 forms:
  pop-min, placeholder-registration, select-person, prelude,
  person-plan, run — 1,327 code lines), its stale-test-citation
  backlog note, and §2g's recorded-not-fixed findings.
- Census §4b (run's in-loop fold IS apply site 1 — you are moving
  the program's main apply site verbatim; unification pays its
  divergence later, not here).

DERIVE FROM THE TREE: every mover's marker; interface.clj re-exports
among movers (channel expects run, person-plan, compile-patient,
valid-persons? resolve through engine — some may already be
delegating defs rather than movers; the mover set is exactly the six
REAL forms, derive it); test-file call sites; outgoing edges by
whole-symbol scan (expect decide, evolve, fold, log-index,
encounters, state, streams, config, assignment, churn, sim-model,
patient-simulator — the driver touches everything; correct from the
tree). Anything still resolving in engine.clj is stop-and-report.

FACADE RULE: engine.clj afterwards is ns + delegating defs + banners
ONLY. Do NOT remove any existing delegating def, even ones nothing in
src now calls (retirement is the ruled repoint pass's business —
count and name the candidates in the record instead). New delegating
defs for the public movers keep every test and interface reference
resolving.

STEPS (one gate each; full make test before every push)
1. Confirm tip; re-derive everything above. Gate: recorded.
2. Constraint-6 sweep, all levels (eight sessions of banners
   included; cross-brick prose; hand-owned-asset sources — the
   gt-emitters tripwire has fired four extractions running, predict
   it); dispositions committed before the move; predicted reds
   RED-FIRST with successor; run state-derived regeneration LAST
   before the gate. Commit: "docs: the run cluster's pre-move
   citation sweep". Gate: hit list first.
3. Extract to ehrt.sim-engine.run (collision-check the name against
   clojure.core/run! shadowing concerns and say what you chose):
   verbatim; widenings only where forced. Commit: "refactor: extract
   run namespace from engine.clj — engine is now a pure facade".
   Gate: suite delta zero or explained to the assertion;
   bin/regression-oracle IDENTICAL, no declaration — delta = defect,
   stop and report.
4. bin/ground-truth-bracket. Gate: IDENTICAL.
5. Push; verify CI via gh; CI green at tip is the close marker.
   Record: census corrections; P5 row → ten landings and PHASE NOTE
   that engine.clj extraction is COMPLETE, emit_hl7.clj is next;
   the facade's final form count and the retirement-candidate list
   for the repoint pass.
FENCES: no interface.clj edits; no behavior or draw-order changes; no
change to what the in-loop fold does; no var renames; no
emit_hl7.clj; oracle IDENTICAL or stop.
SELF-ARCHIVE: prompt and record in the final push.
